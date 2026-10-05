package com.studyos.app.core.csv

import com.studyos.app.core.database.StudyOSDatabase
import com.studyos.app.core.database.entity.ChapterEntity
import com.studyos.app.core.database.entity.MistakeEntity
import com.studyos.app.core.database.entity.QuestionBankEntity
import com.studyos.app.core.database.entity.RecallItemEntity
import com.studyos.app.core.database.entity.SubjectEntity
import com.studyos.app.core.database.entity.TopicEntity
import com.studyos.app.domain.model.ExamQuestionType
import com.studyos.app.domain.model.ExamRelevance
import com.studyos.app.domain.model.QuestionDifficulty
import com.studyos.app.domain.model.TopicMasteryState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.StringReader
import java.io.StringWriter
import java.util.UUID

enum class CsvEntityType(val displayName: String) {
    SYLLABUS("Syllabus (Subjects, Chapters & Topics)"),
    QUESTION_BANK("Question Bank"),
    RECALL_CARDS("Recall & Flashcards"),
    MISTAKES("Mistake Bank")
}

data class CsvRowError(
    val lineNumber: Int,
    val rowContent: String,
    val errorMessage: String
)

data class CsvValidationResult(
    val totalRows: Int,
    val validRowsCount: Int,
    val errors: List<CsvRowError>,
    val parsedData: List<Map<String, String>>
)

data class CsvImportResult(
    val success: Boolean,
    val importedCount: Int,
    val errors: List<CsvRowError>
)

class UniversalCsvProcessor(
    private val database: StudyOSDatabase? = null
) {

    fun getTemplate(type: CsvEntityType): String {
        return when (type) {
            CsvEntityType.SYLLABUS ->
                "SubjectName,ChapterName,TopicName,MasteryState,ExamRelevance\n" +
                "Mathematics,Calculus,Derivatives,LEARNING,HIGH\n" +
                "Physics,Mechanics,Newton's Laws,MASTERED,HIGH\n"

            CsvEntityType.QUESTION_BANK ->
                "SubjectName,ChapterName,TopicName,QuestionText,MarkingScheme,Marks,QuestionType,Difficulty\n" +
                "Physics,Mechanics,Newton's Laws,\"State Newton's second law.\",\"Statement (1m) + Formula (1m)\",2,SHORT_ANSWER,MEDIUM\n"

            CsvEntityType.RECALL_CARDS ->
                "SubjectName,ChapterName,Prompt,ExpectedAnswer,Explanation\n" +
                "Physics,Mechanics,\"What is the SI unit of force?\",Newton,\"Named after Sir Isaac Newton, 1 N = 1 kg·m/s²\"\n"

            CsvEntityType.MISTAKES ->
                "SubjectName,ChapterName,Question,StudentAnswer,CorrectAnswer,LossCategory,MarksLost\n" +
                "Mathematics,Calculus,\"Integrate sin(x)\",\"cos(x)\",\"-cos(x) + C\",CARELESS_MISTAKE,1.0\n"
        }
    }

    fun parseAndValidate(type: CsvEntityType, csvContent: String): CsvValidationResult {
        val lines = parseCsvLines(csvContent)
        if (lines.isEmpty()) {
            return CsvValidationResult(0, 0, listOf(CsvRowError(0, "", "CSV content is empty")), emptyList())
        }

        val header = lines.first().map { it.trim().lowercase() }
        val errors = mutableListOf<CsvRowError>()
        val parsed = mutableListOf<Map<String, String>>()

        val requiredHeaders = when (type) {
            CsvEntityType.SYLLABUS -> listOf("subjectname", "chaptername", "topicname")
            CsvEntityType.QUESTION_BANK -> listOf("subjectname", "chaptername", "questiontext", "marks")
            CsvEntityType.RECALL_CARDS -> listOf("subjectname", "chaptername", "prompt", "expectedanswer")
            CsvEntityType.MISTAKES -> listOf("subjectname", "chaptername", "question", "correctanswer")
        }

        val missingHeaders = requiredHeaders.filter { it !in header }
        if (missingHeaders.isNotEmpty()) {
            return CsvValidationResult(
                totalRows = 0,
                validRowsCount = 0,
                errors = listOf(CsvRowError(1, lines.first().joinToString(","), "Missing required columns: ${missingHeaders.joinToString(", ")}")),
                parsedData = emptyList()
            )
        }

        for (i in 1 until lines.size) {
            val line = lines[i]
            if (line.isEmpty() || (line.size == 1 && line[0].isBlank())) continue

            val rowMap = mutableMapOf<String, String>()
            for (colIdx in 0 until minOf(header.size, line.size)) {
                rowMap[header[colIdx]] = line[colIdx].trim()
            }

            var rowError: String? = null
            when (type) {
                CsvEntityType.SYLLABUS -> {
                    if (rowMap["subjectname"].isNullOrBlank()) rowError = "SubjectName cannot be blank"
                    else if (rowMap["chaptername"].isNullOrBlank()) rowError = "ChapterName cannot be blank"
                    else if (rowMap["topicname"].isNullOrBlank()) rowError = "TopicName cannot be blank"
                }
                CsvEntityType.QUESTION_BANK -> {
                    if (rowMap["subjectname"].isNullOrBlank()) rowError = "SubjectName cannot be blank"
                    else if (rowMap["chaptername"].isNullOrBlank()) rowError = "ChapterName cannot be blank"
                    else if (rowMap["questiontext"].isNullOrBlank()) rowError = "QuestionText cannot be blank"
                    else if (rowMap["marks"]?.toIntOrNull() == null || rowMap["marks"]!!.toInt() <= 0) {
                        rowError = "Marks must be a positive integer"
                    }
                }
                CsvEntityType.RECALL_CARDS -> {
                    if (rowMap["subjectname"].isNullOrBlank()) rowError = "SubjectName cannot be blank"
                    else if (rowMap["chaptername"].isNullOrBlank()) rowError = "ChapterName cannot be blank"
                    else if (rowMap["prompt"].isNullOrBlank()) rowError = "Prompt cannot be blank"
                    else if (rowMap["expectedanswer"].isNullOrBlank()) rowError = "ExpectedAnswer cannot be blank"
                }
                CsvEntityType.MISTAKES -> {
                    if (rowMap["subjectname"].isNullOrBlank()) rowError = "SubjectName cannot be blank"
                    else if (rowMap["question"].isNullOrBlank()) rowError = "Question cannot be blank"
                    else if (rowMap["correctanswer"].isNullOrBlank()) rowError = "CorrectAnswer cannot be blank"
                }
            }

            if (rowError != null) {
                errors.add(CsvRowError(i + 1, line.joinToString(","), rowError))
            } else {
                parsed.add(rowMap)
            }
        }

        return CsvValidationResult(
            totalRows = lines.size - 1,
            validRowsCount = parsed.size,
            errors = errors,
            parsedData = parsed
        )
    }

    suspend fun importData(type: CsvEntityType, parsedData: List<Map<String, String>>): CsvImportResult = withContext(Dispatchers.IO) {
        val db = database ?: throw IllegalStateException("StudyOSDatabase is required to import data")
        var importedCount = 0
        val subjectDao = db.subjectDao()
        val chapterDao = db.chapterDao()
        val topicDao = db.topicDao()
        val questionBankDao = db.questionBankDao()
        val recallDao = db.recallDao()
        val mistakeDao = db.mistakeDao()

        // Helper to get or create subject
        suspend fun getOrCreateSubject(name: String): String {
            val existing = subjectDao.getAllSubjectsOnce().find { it.name.equals(name, ignoreCase = true) }
            if (existing != null) return existing.id
            val id = UUID.randomUUID().toString()
            subjectDao.insertSubject(SubjectEntity(id = id, name = name, isCustom = true))
            return id
        }

        // Helper to get or create chapter
        suspend fun getOrCreateChapter(subjectId: String, name: String): String {
            val existing = chapterDao.getChaptersForSubjectOnce(subjectId).find { it.name.equals(name, ignoreCase = true) }
            if (existing != null) return existing.id
            val id = UUID.randomUUID().toString()
            chapterDao.insertChapter(ChapterEntity(id = id, subjectId = subjectId, name = name))
            return id
        }

        // Helper to get or create topic
        suspend fun getOrCreateTopic(chapterId: String, name: String, relevance: String = "MEDIUM"): String {
            val existing = topicDao.getTopicsForChapterOnce(chapterId).find { it.name.equals(name, ignoreCase = true) }
            if (existing != null) return existing.id
            val id = UUID.randomUUID().toString()
            topicDao.insertTopic(TopicEntity(id = id, chapterId = chapterId, name = name, examRelevance = relevance))
            return id
        }

        val errors = mutableListOf<CsvRowError>()

        for ((index, row) in parsedData.withIndex()) {
            val rowNum = index + 2 // Row 1 is header
            try {
                when (type) {
                    CsvEntityType.SYLLABUS -> {
                        val subName = row["subjectname"] ?: throw IllegalArgumentException("Missing subject name")
                        val chapName = row["chaptername"] ?: throw IllegalArgumentException("Missing chapter name")
                        val topicName = row["topicname"] ?: throw IllegalArgumentException("Missing topic name")
                        val subId = getOrCreateSubject(subName)
                        val chapId = getOrCreateChapter(subId, chapName)
                        val mastery = row["masterystate"]?.uppercase()?.let {
                            try { TopicMasteryState.valueOf(it).name } catch (_: Exception) { "NOT_STARTED" }
                        } ?: "NOT_STARTED"
                        val relevance = row["examrelevance"]?.uppercase()?.let {
                            try { ExamRelevance.valueOf(it).name } catch (_: Exception) { "MEDIUM" }
                        } ?: "MEDIUM"

                        val topicId = getOrCreateTopic(chapId, topicName, relevance)
                        topicDao.updateMasteryState(topicId, mastery)
                        importedCount++
                    }

                    CsvEntityType.QUESTION_BANK -> {
                        val subName = row["subjectname"] ?: throw IllegalArgumentException("Missing subject name")
                        val chapName = row["chaptername"] ?: throw IllegalArgumentException("Missing chapter name")
                        val qText = row["questiontext"] ?: throw IllegalArgumentException("Missing question text")
                        val subId = getOrCreateSubject(subName)
                        val chapId = getOrCreateChapter(subId, chapName)
                        val topicId = row["topicname"]?.let { if (it.isNotBlank()) getOrCreateTopic(chapId, it) else null }

                        val scheme = row["markingscheme"] ?: ""
                        val marks = row["marks"]?.toIntOrNull() ?: 1
                        val qType = row["questiontype"]?.uppercase()?.let {
                            try { ExamQuestionType.valueOf(it).name } catch (_: Exception) { "SHORT_ANSWER" }
                        } ?: "SHORT_ANSWER"
                        val diff = row["difficulty"]?.uppercase()?.let {
                            try { QuestionDifficulty.valueOf(it).name } catch (_: Exception) { "MEDIUM" }
                        } ?: "MEDIUM"

                        questionBankDao.insertQuestion(
                            QuestionBankEntity(
                                subjectId = subId,
                                chapterId = chapId,
                                topicId = topicId,
                                questionText = qText,
                                markingScheme = scheme,
                                marks = marks,
                                questionType = qType,
                                difficulty = diff
                            )
                        )
                        importedCount++
                    }

                    CsvEntityType.RECALL_CARDS -> {
                        val subName = row["subjectname"] ?: throw IllegalArgumentException("Missing subject name")
                        val chapName = row["chaptername"] ?: throw IllegalArgumentException("Missing chapter name")
                        val prompt = row["prompt"] ?: throw IllegalArgumentException("Missing prompt")
                        val answer = row["expectedanswer"] ?: throw IllegalArgumentException("Missing expected answer")
                        val subId = getOrCreateSubject(subName)
                        val chapId = getOrCreateChapter(subId, chapName)
                        val explanation = row["explanation"] ?: ""

                        recallDao.insertOrUpdate(
                            RecallItemEntity(
                                subjectId = subId,
                                chapterId = chapId,
                                prompt = prompt,
                                expectedAnswer = answer,
                                explanation = explanation
                            )
                        )
                        importedCount++
                    }

                    CsvEntityType.MISTAKES -> {
                        val subName = row["subjectname"] ?: throw IllegalArgumentException("Missing subject name")
                        val question = row["question"] ?: throw IllegalArgumentException("Missing question")
                        val correctAns = row["correctanswer"] ?: throw IllegalArgumentException("Missing correct answer")
                        val subId = getOrCreateSubject(subName)
                        val chapId = row["chaptername"]?.let { if (it.isNotBlank()) getOrCreateChapter(subId, it) else null }
                        val studentAns = row["studentanswer"] ?: ""
                        val category = row["losscategory"] ?: "CONCEPT_ERROR"
                        val marksLost = row["markslost"]?.toDoubleOrNull() ?: 1.0

                        mistakeDao.insert(
                            MistakeEntity(
                                subjectId = subId,
                                chapterId = chapId,
                                question = question,
                                studentAnswer = studentAns,
                                correctAnswer = correctAns,
                                lossCategory = category,
                                marksLost = marksLost
                            )
                        )
                        importedCount++
                    }
                }
            } catch (e: Exception) {
                errors.add(
                    CsvRowError(
                        lineNumber = rowNum,
                        rowContent = row.values.joinToString(","),
                        errorMessage = e.message ?: "Failed to import row"
                    )
                )
            }
        }

        CsvImportResult(
            success = errors.isEmpty(),
            importedCount = importedCount,
            errors = errors
        )
    }

    suspend fun exportData(type: CsvEntityType): String = withContext(Dispatchers.IO) {
        val db = database ?: throw IllegalStateException("StudyOSDatabase is required to export data")
        val writer = StringWriter()
        when (type) {
            CsvEntityType.SYLLABUS -> {
                writer.append("SubjectName,ChapterName,TopicName,MasteryState,ExamRelevance\n")
                val subjects = db.subjectDao().getAllSubjectsOnce()
                for (s in subjects) {
                    val chapters = db.chapterDao().getChaptersForSubjectOnce(s.id)
                    for (c in chapters) {
                        val topics = db.topicDao().getTopicsForChapterOnce(c.id)
                        for (t in topics) {
                            writer.append(escapeCsv(s.name)).append(",")
                            writer.append(escapeCsv(c.name)).append(",")
                            writer.append(escapeCsv(t.name)).append(",")
                            writer.append(t.masteryState).append(",")
                            writer.append(t.examRelevance).append("\n")
                        }
                    }
                }
            }

            CsvEntityType.QUESTION_BANK -> {
                writer.append("SubjectName,ChapterName,TopicName,QuestionText,MarkingScheme,Marks,QuestionType,Difficulty\n")
                val subjects = db.subjectDao().getAllSubjectsOnce().associateBy { it.id }
                val chapters = db.chapterDao().getAllChaptersOnce().associateBy { it.id }
                val questions = db.questionBankDao().getAllQuestionsOnce()
                for (q in questions) {
                    val topicName = q.topicId?.let { db.topicDao().getTopicById(it)?.name } ?: ""
                    writer.append(escapeCsv(subjects[q.subjectId]?.name ?: "")).append(",")
                    writer.append(escapeCsv(chapters[q.chapterId]?.name ?: "")).append(",")
                    writer.append(escapeCsv(topicName)).append(",")
                    writer.append(escapeCsv(q.questionText)).append(",")
                    writer.append(escapeCsv(q.markingScheme)).append(",")
                    writer.append(q.marks.toString()).append(",")
                    writer.append(q.questionType).append(",")
                    writer.append(q.difficulty).append("\n")
                }
            }

            CsvEntityType.RECALL_CARDS -> {
                writer.append("SubjectName,ChapterName,Prompt,ExpectedAnswer,Explanation\n")
                val subjects = db.subjectDao().getAllSubjectsOnce().associateBy { it.id }
                val chapters = db.chapterDao().getAllChaptersOnce().associateBy { it.id }
                val items = db.recallDao().getAllRecallItemsOnce()
                for (item in items) {
                    writer.append(escapeCsv(subjects[item.subjectId]?.name ?: "")).append(",")
                    writer.append(escapeCsv(chapters[item.chapterId]?.name ?: "")).append(",")
                    writer.append(escapeCsv(item.prompt)).append(",")
                    writer.append(escapeCsv(item.expectedAnswer)).append(",")
                    writer.append(escapeCsv(item.explanation)).append("\n")
                }
            }

            CsvEntityType.MISTAKES -> {
                writer.append("SubjectName,ChapterName,Question,StudentAnswer,CorrectAnswer,LossCategory,MarksLost\n")
                val subjects = db.subjectDao().getAllSubjectsOnce().associateBy { it.id }
                val chapters = db.chapterDao().getAllChaptersOnce().associateBy { it.id }
                val mistakes = db.mistakeDao().getAllMistakesOnce()
                for (m in mistakes) {
                    writer.append(escapeCsv(subjects[m.subjectId]?.name ?: "")).append(",")
                    writer.append(escapeCsv(m.chapterId?.let { chapters[it]?.name } ?: "")).append(",")
                    writer.append(escapeCsv(m.question)).append(",")
                    writer.append(escapeCsv(m.studentAnswer)).append(",")
                    writer.append(escapeCsv(m.correctAnswer)).append(",")
                    writer.append(m.lossCategory).append(",")
                    writer.append(m.marksLost.toString()).append("\n")
                }
            }
        }
        writer.toString()
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

    private fun parseCsvLines(csv: String): List<List<String>> {
        val result = mutableListOf<List<String>>()
        val reader = BufferedReader(StringReader(csv))
        var line = reader.readLine()
        while (line != null) {
            if (line.isNotBlank()) {
                val row = mutableListOf<String>()
                val sb = StringBuilder()
                var inQuotes = false
                var i = 0
                while (i < line.length) {
                    val c = line[i]
                    if (c == '\"') {
                        if (inQuotes && i + 1 < line.length && line[i + 1] == '\"') {
                            sb.append('\"')
                            i++
                        } else {
                            inQuotes = !inQuotes
                        }
                    } else if (c == ',' && !inQuotes) {
                        row.add(sb.toString())
                        sb.setLength(0)
                    } else {
                        sb.append(c)
                    }
                    i++
                }
                row.add(sb.toString())
                result.add(row)
            }
            line = reader.readLine()
        }
        return result
    }
}
