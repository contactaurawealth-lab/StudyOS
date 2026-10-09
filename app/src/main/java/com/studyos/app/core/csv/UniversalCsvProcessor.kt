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
import com.studyos.app.core.database.entity.FlashcardEntity
import java.io.BufferedReader
import java.io.StringReader
import java.io.StringWriter
import java.util.UUID

enum class CsvEntityType(val displayName: String) {
    SYLLABUS("Syllabus (Subjects, Chapters & Topics)"),
    QUESTION_BANK("Question Bank"),
    OBJECTIVE_QUESTIONS("Objective Questions (MCQ, FIB, T/F)"),
    RECALL_CARDS("Recall & Flashcards"),
    MISTAKES("Mistake Bank")
}

enum class QuestionImportTarget(val displayName: String) {
    BOTH("Both Question Bank & Flashcards (Default)"),
    QUESTION_BANK_ONLY("Question Bank Only"),
    FLASHCARDS_ONLY("Flashcards Only")
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

            CsvEntityType.OBJECTIVE_QUESTIONS ->
                getObjectiveQuestionsCsvTemplate()

            CsvEntityType.RECALL_CARDS ->
                "SubjectName,ChapterName,Prompt,ExpectedAnswer,Explanation\n" +
                "Physics,Mechanics,\"What is the SI unit of force?\",Newton,\"Named after Sir Isaac Newton, 1 N = 1 kg·m/s²\"\n"

            CsvEntityType.MISTAKES ->
                "SubjectName,ChapterName,Question,StudentAnswer,CorrectAnswer,LossCategory,MarksLost\n" +
                "Mathematics,Calculus,\"Integrate sin(x)\",\"cos(x)\",\"-cos(x) + C\",CARELESS_MISTAKE,1.0\n"
        }
    }

    fun parseAndValidate(type: CsvEntityType, csvContent: String): CsvValidationResult {
        if (type == CsvEntityType.OBJECTIVE_QUESTIONS) {
            return parseAndValidateObjectiveQuestions(csvContent)
        }

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
            CsvEntityType.OBJECTIVE_QUESTIONS -> listOf("subjectname", "chaptername", "question", "correctanswer")
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
                CsvEntityType.OBJECTIVE_QUESTIONS -> {
                    // Handled in parseAndValidateObjectiveQuestions
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
        if (type == CsvEntityType.OBJECTIVE_QUESTIONS) {
            return@withContext importObjectiveQuestions(parsedData)
        }
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

                    CsvEntityType.OBJECTIVE_QUESTIONS -> {
                        // Handled in importObjectiveQuestions
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

    fun getQuestionBankCsvTemplate(): String {
        return "SubjectName,ChapterName,TopicName,Question,Answer,Marks,Difficulty\n" +
            "Physics,Rotational Dynamics,Moment of Inertia,\"State the parallel axis theorem for moment of inertia.\",\"Statement: I = Ic + Mh^2 with all terms defined.\",2,EASY\n" +
            "Physics,Rotational Dynamics,Torque,\"Derive the relation between torque and angular acceleration for a rigid body.\",\"Derivation steps with formula τ = I · α and diagram reference.\",4,MEDIUM\n" +
            "Mathematics,Calculus,Integrals,\"Evaluate the indefinite integral of sec(x) dx.\",\"ln|sec(x) + tan(x)| + C\",2,EASY\n" +
            "Mathematics,Calculus,Derivatives,\"State and prove Rolle's Theorem with required conditions.\",\"Proof using Extreme Value Theorem to show f'(c) = 0 for some c in (a, b).\",5,HARD\n" +
            "Chemistry,Thermodynamics,First Law,\"State the first law of thermodynamics in mathematical form.\",\"ΔU = q + w with IUPAC sign conventions.\",3,MEDIUM\n"
    }

    fun parseAndValidateQuestions(csvContent: String): CsvValidationResult {
        val lines = parseCsvLines(csvContent)
        if (lines.isEmpty()) {
            return CsvValidationResult(0, 0, listOf(CsvRowError(0, "", "CSV content is empty")), emptyList())
        }

        val header = lines.first().map { it.trim().lowercase() }
        val errors = mutableListOf<CsvRowError>()
        val parsed = mutableListOf<Map<String, String>>()

        val subjectIdx = header.indexOfFirst { it == "subjectname" || it == "subject" }
        val chapterIdx = header.indexOfFirst { it == "chaptername" || it == "chapter" }
        val topicIdx = header.indexOfFirst { it == "topicname" || it == "topic" }
        val questionIdx = header.indexOfFirst { it == "question" || it == "questiontext" || it == "prompt" }
        val answerIdx = header.indexOfFirst { it == "answer" || it == "expectedanswer" || it == "markingscheme" }
        val marksIdx = header.indexOfFirst { it == "marks" || it == "mark" }
        val difficultyIdx = header.indexOfFirst { it == "difficulty" }

        val missing = mutableListOf<String>()
        if (subjectIdx == -1) missing.add("SubjectName")
        if (chapterIdx == -1) missing.add("ChapterName")
        if (questionIdx == -1) missing.add("Question")
        if (answerIdx == -1) missing.add("Answer")

        if (missing.isNotEmpty()) {
            return CsvValidationResult(
                totalRows = 0,
                validRowsCount = 0,
                errors = listOf(CsvRowError(1, lines.first().joinToString(","), "Missing required columns: ${missing.joinToString(", ")}")),
                parsedData = emptyList()
            )
        }

        for (i in 1 until lines.size) {
            val line = lines[i]
            if (line.isEmpty() || (line.size == 1 && line[0].isBlank())) continue

            fun getVal(idx: Int): String = if (idx in line.indices) line[idx].trim() else ""

            val sub = getVal(subjectIdx)
            val chap = getVal(chapterIdx)
            val q = getVal(questionIdx)
            val ans = getVal(answerIdx)
            val top = if (topicIdx != -1) getVal(topicIdx) else ""
            val marksStr = if (marksIdx != -1) getVal(marksIdx) else "1"
            val diffStr = if (difficultyIdx != -1) getVal(difficultyIdx) else "MEDIUM"

            var err: String? = null
            if (sub.isBlank()) err = "SubjectName cannot be blank"
            else if (chap.isBlank()) err = "ChapterName cannot be blank"
            else if (q.isBlank()) err = "Question cannot be blank"
            else if (ans.isBlank()) err = "Answer cannot be blank"

            if (err != null) {
                errors.add(CsvRowError(i + 1, line.joinToString(","), err))
            } else {
                val rowMap = mapOf(
                    "subjectname" to sub,
                    "chaptername" to chap,
                    "topicname" to top,
                    "question" to q,
                    "answer" to ans,
                    "marks" to (marksStr.toIntOrNull()?.coerceAtLeast(1)?.toString() ?: "1"),
                    "difficulty" to (if (diffStr.isNotBlank()) diffStr.uppercase() else "MEDIUM")
                )
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

    suspend fun importQuestions(
        parsedData: List<Map<String, String>>,
        target: QuestionImportTarget = QuestionImportTarget.BOTH
    ): CsvImportResult = withContext(Dispatchers.IO) {
        val db = database ?: throw IllegalStateException("StudyOSDatabase is required to import questions")
        var importedCount = 0
        val errors = mutableListOf<CsvRowError>()
        val subjectDao = db.subjectDao()
        val chapterDao = db.chapterDao()
        val topicDao = db.topicDao()
        val questionBankDao = db.questionBankDao()
        val flashcardDao = db.flashcardDao()

        suspend fun getOrCreateSubject(name: String): String {
            val existing = subjectDao.getAllSubjectsOnce().find { it.name.equals(name, ignoreCase = true) }
            if (existing != null) return existing.id
            val id = UUID.randomUUID().toString()
            subjectDao.insertSubject(SubjectEntity(id = id, name = name, isCustom = true))
            return id
        }

        suspend fun getOrCreateChapter(subjectId: String, name: String): String {
            val existing = chapterDao.getChaptersForSubjectOnce(subjectId).find { it.name.equals(name, ignoreCase = true) }
            if (existing != null) return existing.id
            val id = UUID.randomUUID().toString()
            chapterDao.insertChapter(ChapterEntity(id = id, subjectId = subjectId, name = name))
            return id
        }

        suspend fun getOrCreateTopic(chapterId: String, name: String): String {
            val existing = topicDao.getTopicsForChapterOnce(chapterId).find { it.name.equals(name, ignoreCase = true) }
            if (existing != null) return existing.id
            val id = UUID.randomUUID().toString()
            topicDao.insertTopic(TopicEntity(id = id, chapterId = chapterId, name = name, examRelevance = "MEDIUM"))
            return id
        }

        for ((index, row) in parsedData.withIndex()) {
            val rowNum = index + 2
            try {
                val subName = row["subjectname"] ?: throw IllegalArgumentException("Missing subject name")
                val chapName = row["chaptername"] ?: throw IllegalArgumentException("Missing chapter name")
                val qText = row["question"] ?: throw IllegalArgumentException("Missing question")
                val answerText = row["answer"] ?: throw IllegalArgumentException("Missing answer")
                val topicName = row["topicname"]
                val marks = row["marks"]?.toIntOrNull() ?: 1
                val diff = row["difficulty"]?.uppercase() ?: "MEDIUM"

                val subId = getOrCreateSubject(subName)
                val chapId = getOrCreateChapter(subId, chapName)
                val topId = if (!topicName.isNullOrBlank()) getOrCreateTopic(chapId, topicName) else null

                // 1. Insert into Question Bank
                if (target == QuestionImportTarget.BOTH || target == QuestionImportTarget.QUESTION_BANK_ONLY) {
                    val qType = when {
                        marks <= 1 -> ExamQuestionType.SHORT_ANSWER.name
                        marks in 2..3 -> ExamQuestionType.SHORT_ANSWER.name
                        else -> ExamQuestionType.LONG_ANSWER.name
                    }
                    questionBankDao.insertQuestion(
                        QuestionBankEntity(
                            subjectId = subId,
                            chapterId = chapId,
                            topicId = topId,
                            questionText = qText,
                            markingScheme = answerText,
                            marks = marks,
                            questionType = qType,
                            difficulty = diff
                        )
                    )
                }

                // 2. Insert into Flashcards
                if (target == QuestionImportTarget.BOTH || target == QuestionImportTarget.FLASHCARDS_ONLY) {
                    flashcardDao.insert(
                        FlashcardEntity(
                            subjectId = subId,
                            chapterId = chapId,
                            question = qText,
                            answer = answerText,
                            difficulty = diff
                        )
                    )
                }

                importedCount++
            } catch (e: Exception) {
                errors.add(
                    CsvRowError(
                        lineNumber = rowNum,
                        rowContent = row.values.joinToString(","),
                        errorMessage = e.message ?: "Failed to import question row"
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

    fun exportQuestionBankTemplateFile(context: android.content.Context): java.io.File {
        val folder = java.io.File(context.cacheDir, "shared_preview").apply { if (!exists()) mkdirs() }
        val file = java.io.File(folder, "studyos_question_bank_template.csv")
        file.writeText(getQuestionBankCsvTemplate())
        return file
    }

    fun getObjectiveQuestionsCsvTemplate(): String {
        return "SubjectName,ChapterName,TopicName,QuestionType,Question,OptionA,OptionB,OptionC,OptionD,CorrectAnswer,Explanation,Difficulty,Marks\n" +
            "Physics,Current Electricity,Ohm's Law,MCQ,\"Which particle is the primary carrier of electric current in metallic conductors?\",Protons,Free Electrons,Neutrons,Positrons,B,\"In metallic conductors, valence electrons are delocalized and drift under an applied electric field.\",EASY,1\n" +
            "Biology,Photosynthesis,Light Reaction,FIB,\"The light-absorbing green pigment found inside chloroplasts is called ________.\",,,,,Chlorophyll,\"Chlorophyll a and b absorb blue and red light while reflecting green light.\",EASY,1\n" +
            "Chemistry,Thermodynamics,Enthalpy,TRUE_FALSE,\"An exothermic reaction absorbs thermal energy from its surroundings.\",,,,,False,\"Exothermic reactions release heat (ΔH < 0), warming their surroundings.\",MEDIUM,1\n" +
            "Mathematics,Calculus,Derivatives,MCQ,\"What is the derivative of sin(2x) with respect to x?\",cos(2x),2cos(2x),-2cos(2x),0.5cos(2x),Option B,\"By the chain rule: d/dx[sin(2x)] = cos(2x) * 2 = 2cos(2x).\",MEDIUM,1\n" +
            "Physics,Mechanics,Newton's Laws,FIB,\"The rate of change of momentum of a body is directly proportional to the applied ________.\",,,,,Force,\"Newton's second law states that F = dp/dt = m*a.\",MEDIUM,1\n" +
            "Chemistry,Chemical Bonding,Covalent,TRUE_FALSE,\"A triple covalent bond involves the sharing of six valence electrons.\",,,,,True,\"Each covalent bond consists of 2 shared electrons; 3 covalent bonds share 6 electrons.\",EASY,1\n"
    }

    fun exportObjectiveQuestionsTemplateFile(context: android.content.Context): java.io.File {
        val folder = java.io.File(context.cacheDir, "shared_preview").apply { if (!exists()) mkdirs() }
        val file = java.io.File(folder, "studyos_objective_questions_template.csv")
        file.writeText(getObjectiveQuestionsCsvTemplate())
        return file
    }

    fun parseAndValidateObjectiveQuestions(csvContent: String): CsvValidationResult {
        val lines = parseCsvLines(csvContent)
        if (lines.isEmpty()) {
            return CsvValidationResult(0, 0, listOf(CsvRowError(0, "", "CSV content is empty")), emptyList())
        }

        val header = lines.first().map { it.trim().lowercase() }
        val errors = mutableListOf<CsvRowError>()
        val parsed = mutableListOf<Map<String, String>>()

        val subjectIdx = header.indexOfFirst { it in listOf("subjectname", "subject", "sub") }
        val chapterIdx = header.indexOfFirst { it in listOf("chaptername", "chapter", "chap") }
        val topicIdx = header.indexOfFirst { it in listOf("topicname", "topic") }
        val typeIdx = header.indexOfFirst { it in listOf("questiontype", "type", "qtype") }
        val questionIdx = header.indexOfFirst { it in listOf("question", "questiontext", "prompt", "stem") }
        val optAIdx = header.indexOfFirst { it in listOf("optiona", "opta", "a", "opt1", "option1") }
        val optBIdx = header.indexOfFirst { it in listOf("optionb", "optb", "b", "opt2", "option2") }
        val optCIdx = header.indexOfFirst { it in listOf("optionc", "optc", "c", "opt3", "option3") }
        val optDIdx = header.indexOfFirst { it in listOf("optiond", "optd", "d", "opt4", "option4") }
        val answerIdx = header.indexOfFirst { it in listOf("correctanswer", "answer", "key", "correct", "expectedanswer") }
        val explanationIdx = header.indexOfFirst { it in listOf("explanation", "markingscheme", "solution", "rationale", "notes") }
        val diffIdx = header.indexOfFirst { it in listOf("difficulty", "diff") }
        val marksIdx = header.indexOfFirst { it in listOf("marks", "mark") }

        val missing = mutableListOf<String>()
        if (subjectIdx == -1) missing.add("SubjectName")
        if (chapterIdx == -1) missing.add("ChapterName")
        if (questionIdx == -1) missing.add("Question")
        if (answerIdx == -1) missing.add("CorrectAnswer")

        if (missing.isNotEmpty()) {
            return CsvValidationResult(
                totalRows = 0,
                validRowsCount = 0,
                errors = listOf(CsvRowError(1, lines.first().joinToString(","), "Missing required columns: ${missing.joinToString(", ")}")),
                parsedData = emptyList()
            )
        }

        for (i in 1 until lines.size) {
            val line = lines[i]
            if (line.isEmpty() || (line.size == 1 && line[0].isBlank())) continue

            fun getVal(idx: Int): String = if (idx in line.indices) line[idx].trim() else ""

            val sub = getVal(subjectIdx)
            val chap = getVal(chapterIdx)
            val top = if (topicIdx != -1) getVal(topicIdx) else ""
            val rawType = if (typeIdx != -1) getVal(typeIdx) else ""
            val q = getVal(questionIdx)
            val optA = if (optAIdx != -1) getVal(optAIdx) else ""
            val optB = if (optBIdx != -1) getVal(optBIdx) else ""
            val optC = if (optCIdx != -1) getVal(optCIdx) else ""
            val optD = if (optDIdx != -1) getVal(optDIdx) else ""
            val ans = getVal(answerIdx)
            val explanation = if (explanationIdx != -1) getVal(explanationIdx) else ""
            val diffStr = if (diffIdx != -1) getVal(diffIdx) else "MEDIUM"
            val marksStr = if (marksIdx != -1) getVal(marksIdx) else "1"

            var err: String? = null
            if (sub.isBlank()) {
                err = "SubjectName cannot be blank"
            } else if (chap.isBlank()) {
                err = "ChapterName cannot be blank"
            } else if (q.isBlank()) {
                err = "Question cannot be blank"
            } else if (ans.isBlank()) {
                err = "CorrectAnswer cannot be blank"
            } else {
                val inferredType = when {
                    rawType.equals("FIB", ignoreCase = true) || rawType.contains("fill", ignoreCase = true) || rawType.contains("blank", ignoreCase = true) -> "FIB"
                    rawType.equals("TRUE_FALSE", ignoreCase = true) || rawType.equals("TF", ignoreCase = true) || rawType.equals("T/F", ignoreCase = true) || rawType.contains("true", ignoreCase = true) -> "TRUE_FALSE"
                    rawType.equals("MCQ", ignoreCase = true) || rawType.contains("choice", ignoreCase = true) || rawType.contains("multiple", ignoreCase = true) -> "MCQ"
                    optA.isNotBlank() && optB.isNotBlank() -> "MCQ"
                    q.contains("____") || q.contains("[blank]", ignoreCase = true) -> "FIB"
                    ans.equals("true", ignoreCase = true) || ans.equals("false", ignoreCase = true) -> "TRUE_FALSE"
                    else -> "MCQ"
                }

                if (inferredType == "MCQ" && (optA.isBlank() || optB.isBlank())) {
                    err = "MCQ question requires at least OptionA and OptionB"
                } else if (inferredType == "TRUE_FALSE" && ans.lowercase() !in listOf("true", "false", "t", "f", "yes", "no", "1", "0")) {
                    err = "True/False answer must be True or False (got: '$ans')"
                }
            }

            if (err != null) {
                errors.add(CsvRowError(i + 1, line.joinToString(","), err))
            } else {
                val inferredType = when {
                    rawType.equals("FIB", ignoreCase = true) || rawType.contains("fill", ignoreCase = true) || rawType.contains("blank", ignoreCase = true) -> "FIB"
                    rawType.equals("TRUE_FALSE", ignoreCase = true) || rawType.equals("TF", ignoreCase = true) || rawType.equals("T/F", ignoreCase = true) || rawType.contains("true", ignoreCase = true) -> "TRUE_FALSE"
                    rawType.equals("MCQ", ignoreCase = true) || rawType.contains("choice", ignoreCase = true) || rawType.contains("multiple", ignoreCase = true) -> "MCQ"
                    optA.isNotBlank() && optB.isNotBlank() -> "MCQ"
                    q.contains("____") || q.contains("[blank]", ignoreCase = true) -> "FIB"
                    ans.equals("true", ignoreCase = true) || ans.equals("false", ignoreCase = true) -> "TRUE_FALSE"
                    else -> "MCQ"
                }

                val rowMap = mapOf(
                    "subjectname" to sub,
                    "chaptername" to chap,
                    "topicname" to top,
                    "questiontype" to inferredType,
                    "question" to q,
                    "optiona" to optA,
                    "optionb" to optB,
                    "optionc" to optC,
                    "optiond" to optD,
                    "correctanswer" to ans,
                    "explanation" to explanation,
                    "difficulty" to (if (diffStr.isNotBlank()) diffStr.uppercase() else "MEDIUM"),
                    "marks" to (marksStr.toIntOrNull()?.coerceAtLeast(1)?.toString() ?: "1")
                )
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

    suspend fun importObjectiveQuestions(
        parsedData: List<Map<String, String>>,
        target: QuestionImportTarget = QuestionImportTarget.BOTH
    ): CsvImportResult = withContext(Dispatchers.IO) {
        val db = database ?: throw IllegalStateException("StudyOSDatabase is required to import objective questions")
        var importedCount = 0
        val errors = mutableListOf<CsvRowError>()
        val subjectDao = db.subjectDao()
        val chapterDao = db.chapterDao()
        val topicDao = db.topicDao()
        val questionBankDao = db.questionBankDao()
        val flashcardDao = db.flashcardDao()

        suspend fun getOrCreateSubject(name: String): String {
            val existing = subjectDao.getAllSubjectsOnce().find { it.name.equals(name, ignoreCase = true) }
            if (existing != null) return existing.id
            val id = UUID.randomUUID().toString()
            subjectDao.insertSubject(SubjectEntity(id = id, name = name, isCustom = true))
            return id
        }

        suspend fun getOrCreateChapter(subjectId: String, name: String): String {
            val existing = chapterDao.getChaptersForSubjectOnce(subjectId).find { it.name.equals(name, ignoreCase = true) }
            if (existing != null) return existing.id
            val id = UUID.randomUUID().toString()
            chapterDao.insertChapter(ChapterEntity(id = id, subjectId = subjectId, name = name))
            return id
        }

        suspend fun getOrCreateTopic(chapterId: String, name: String): String {
            val existing = topicDao.getTopicsForChapterOnce(chapterId).find { it.name.equals(name, ignoreCase = true) }
            if (existing != null) return existing.id
            val id = UUID.randomUUID().toString()
            topicDao.insertTopic(TopicEntity(id = id, chapterId = chapterId, name = name, examRelevance = "HIGH"))
            return id
        }

        for ((index, row) in parsedData.withIndex()) {
            val rowNum = index + 2
            try {
                val subName = row["subjectname"] ?: throw IllegalArgumentException("Missing subject name")
                val chapName = row["chaptername"] ?: throw IllegalArgumentException("Missing chapter name")
                val qTypeStr = row["questiontype"] ?: "MCQ"
                val rawQuestion = row["question"] ?: throw IllegalArgumentException("Missing question")
                val optA = row["optiona"] ?: ""
                val optB = row["optionb"] ?: ""
                val optC = row["optionc"] ?: ""
                val optD = row["optiond"] ?: ""
                val rawAns = row["correctanswer"] ?: throw IllegalArgumentException("Missing correct answer")
                val explanation = row["explanation"] ?: ""
                val topicName = row["topicname"]
                val marks = row["marks"]?.toIntOrNull() ?: 1
                val diff = row["difficulty"]?.uppercase() ?: "MEDIUM"

                val subId = getOrCreateSubject(subName)
                val chapId = getOrCreateChapter(subId, chapName)
                val topId = if (!topicName.isNullOrBlank()) getOrCreateTopic(chapId, topicName) else null

                val formattedQuestionText: String
                val resolvedMarkingScheme: String
                val effectiveQType: ExamQuestionType

                when (qTypeStr.uppercase()) {
                    "FIB" -> {
                        effectiveQType = ExamQuestionType.FIB
                        formattedQuestionText = if (rawQuestion.contains("___") || rawQuestion.contains("[blank]", ignoreCase = true)) {
                            rawQuestion
                        } else {
                            "$rawQuestion ________"
                        }
                        resolvedMarkingScheme = if (explanation.isNotBlank()) {
                            "$rawAns • Explanation: $explanation"
                        } else {
                            rawAns
                        }
                    }
                    "TRUE_FALSE" -> {
                        effectiveQType = ExamQuestionType.TRUE_FALSE
                        val normBool = when (rawAns.trim().lowercase()) {
                            "true", "t", "yes", "1" -> "True"
                            else -> "False"
                        }
                        formattedQuestionText = rawQuestion
                        resolvedMarkingScheme = if (explanation.isNotBlank()) {
                            "$normBool • Explanation: $explanation"
                        } else {
                            normBool
                        }
                    }
                    else -> { // MCQ
                        effectiveQType = ExamQuestionType.MCQ
                        val optionsSb = StringBuilder(rawQuestion)
                        val opts = listOf("A" to optA, "B" to optB, "C" to optC, "D" to optD)
                        for ((letter, text) in opts) {
                            if (text.isNotBlank()) {
                                optionsSb.append("\n($letter) $text")
                            }
                        }
                        formattedQuestionText = optionsSb.toString()

                        val cleanLetter = rawAns.removePrefix("Option ").removePrefix("option ").trim().removeSurrounding("(", ")").removeSuffix(")").removeSuffix(".").trim()
                        val resolvedText = when {
                            cleanLetter.equals("A", ignoreCase = true) && optA.isNotBlank() -> "Option A: $optA"
                            cleanLetter.equals("B", ignoreCase = true) && optB.isNotBlank() -> "Option B: $optB"
                            cleanLetter.equals("C", ignoreCase = true) && optC.isNotBlank() -> "Option C: $optC"
                            cleanLetter.equals("D", ignoreCase = true) && optD.isNotBlank() -> "Option D: $optD"
                            rawAns.equals(optA, ignoreCase = true) -> "Option A: $optA"
                            rawAns.equals(optB, ignoreCase = true) -> "Option B: $optB"
                            rawAns.equals(optC, ignoreCase = true) -> "Option C: $optC"
                            rawAns.equals(optD, ignoreCase = true) -> "Option D: $optD"
                            else -> rawAns
                        }

                        resolvedMarkingScheme = if (explanation.isNotBlank()) {
                            "$resolvedText • Explanation: $explanation"
                        } else {
                            resolvedText
                        }
                    }
                }

                // 1. Insert into Question Bank
                if (target == QuestionImportTarget.BOTH || target == QuestionImportTarget.QUESTION_BANK_ONLY) {
                    questionBankDao.insertQuestion(
                        QuestionBankEntity(
                            subjectId = subId,
                            chapterId = chapId,
                            topicId = topId,
                            questionText = formattedQuestionText,
                            markingScheme = resolvedMarkingScheme,
                            marks = marks,
                            questionType = effectiveQType.name,
                            difficulty = diff
                        )
                    )
                }

                // 2. Insert into Flashcards
                if (target == QuestionImportTarget.BOTH || target == QuestionImportTarget.FLASHCARDS_ONLY) {
                    val flashcardQuestion = when (effectiveQType) {
                        ExamQuestionType.TRUE_FALSE -> "$rawQuestion (True or False?)"
                        else -> formattedQuestionText
                    }
                    val flashcardAnswer = when (effectiveQType) {
                        ExamQuestionType.FIB -> if (explanation.isNotBlank()) "$rawAns\n\nExplanation: $explanation" else rawAns
                        ExamQuestionType.TRUE_FALSE -> resolvedMarkingScheme
                        ExamQuestionType.MCQ -> resolvedMarkingScheme
                        else -> resolvedMarkingScheme
                    }

                    flashcardDao.insert(
                        FlashcardEntity(
                            subjectId = subId,
                            chapterId = chapId,
                            question = flashcardQuestion,
                            answer = flashcardAnswer,
                            difficulty = diff
                        )
                    )
                }

                importedCount++
            } catch (e: Exception) {
                errors.add(
                    CsvRowError(
                        lineNumber = rowNum,
                        rowContent = row.values.joinToString(","),
                        errorMessage = e.message ?: "Failed to import objective question row"
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

            CsvEntityType.OBJECTIVE_QUESTIONS -> {
                writer.append("SubjectName,ChapterName,TopicName,QuestionType,Question,OptionA,OptionB,OptionC,OptionD,CorrectAnswer,Explanation,Difficulty,Marks\n")
                val subjects = db.subjectDao().getAllSubjectsOnce().associateBy { it.id }
                val chapters = db.chapterDao().getAllChaptersOnce().associateBy { it.id }
                val questions = db.questionBankDao().getAllQuestionsOnce()
                    .filter { it.questionType in listOf("MCQ", "FIB", "TRUE_FALSE") }
                for (q in questions) {
                    val topicName = q.topicId?.let { db.topicDao().getTopicById(it)?.name } ?: ""
                    val parsed = com.studyos.app.core.quiz.QuestionBankQuizEngine.parseQuestionContent(q.questionText, q.markingScheme, q.questionType)
                    val optA = parsed.options.getOrNull(0) ?: ""
                    val optB = parsed.options.getOrNull(1) ?: ""
                    val optC = parsed.options.getOrNull(2) ?: ""
                    val optD = parsed.options.getOrNull(3) ?: ""
                    writer.append(escapeCsv(subjects[q.subjectId]?.name ?: "")).append(",")
                    writer.append(escapeCsv(chapters[q.chapterId]?.name ?: "")).append(",")
                    writer.append(escapeCsv(topicName)).append(",")
                    writer.append(q.questionType).append(",")
                    writer.append(escapeCsv(parsed.questionStem)).append(",")
                    writer.append(escapeCsv(optA)).append(",")
                    writer.append(escapeCsv(optB)).append(",")
                    writer.append(escapeCsv(optC)).append(",")
                    writer.append(escapeCsv(optD)).append(",")
                    writer.append(escapeCsv(parsed.correctAnswer)).append(",")
                    writer.append(escapeCsv(q.markingScheme ?: "")).append(",")
                    writer.append(q.difficulty).append(",")
                    writer.append(q.marks.toString()).append("\n")
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
