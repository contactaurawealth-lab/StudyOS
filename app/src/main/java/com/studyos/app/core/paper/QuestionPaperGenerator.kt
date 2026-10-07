package com.studyos.app.core.paper

import com.studyos.app.core.database.StudyOSDatabase
import com.studyos.app.core.database.entity.ChapterEntity
import com.studyos.app.core.database.entity.ExamEntity
import com.studyos.app.core.database.entity.ExamSubjectCrossRefEntity
import com.studyos.app.core.database.entity.PaperEntity
import com.studyos.app.core.database.entity.PaperQuestionCrossRefEntity
import com.studyos.app.core.database.entity.QuestionBankEntity
import com.studyos.app.domain.model.ExamQuestionType
import com.studyos.app.domain.model.PaperStatus
import com.studyos.app.domain.model.QuestionDifficulty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class ExamPaperMarks(
    val marks: Int,
    val durationMinutes: Int,
    val displayName: String
) {
    MARKS_20(20, 45, "20 Marks (45 mins)"),
    MARKS_40(40, 90, "40 Marks (1.5 hrs)"),
    MARKS_80(80, 150, "80 Marks (2.5 hrs)"),
    MARKS_100(100, 180, "100 Marks (3.0 hrs)")
}

data class GeneratedPaperResult(
    val paper: PaperEntity,
    val exam: ExamEntity,
    val questions: List<QuestionBankEntity>,
    val crossRefs: List<PaperQuestionCrossRefEntity>
)

class QuestionPaperGenerator(
    private val database: StudyOSDatabase
) {
    suspend fun generatePaper(
        subjectId: String,
        chapterId: String? = null,
        examMarks: ExamPaperMarks,
        customTitle: String? = null
    ): GeneratedPaperResult = withContext(Dispatchers.IO) {
        val subjectDao = database.subjectDao()
        val chapterDao = database.chapterDao()
        val questionBankDao = database.questionBankDao()
        val paperDao = database.paperDao()
        val examDao = database.examDao()

        val subject = subjectDao.getSubject(subjectId)
            ?: throw IllegalArgumentException("Subject with ID $subjectId not found")
        val chapter = if (chapterId != null) chapterDao.getChapterByIdOnce(chapterId) else null

        val title = customTitle?.trim()?.ifBlank { null }
            ?: "${subject.name}${if (chapter != null) " • ${chapter.name}" else ""} - ${examMarks.marks} Marks Test"

        // 1. Fetch existing questions from Question Bank
        val availableQuestions = if (chapterId != null) {
            questionBankDao.getQuestionsForChapterOnce(chapterId)
        } else {
            questionBankDao.getQuestionsForSubjectOnce(subjectId)
        }.toMutableList()

        // 2. Target marks and question distribution
        val targetTotal = examMarks.marks
        val selectedQuestions = mutableListOf<QuestionBankEntity>()
        var accumulatedMarks = 0

        // Use available questions from bank first
        availableQuestions.shuffle()
        for (q in availableQuestions) {
            if (accumulatedMarks + q.marks <= targetTotal) {
                selectedQuestions.add(q)
                accumulatedMarks += q.marks
            }
        }

        // If question bank has fewer questions than targetTotal, generate balanced conceptual questions
        val fallbackChapterId = chapterId ?: chapterDao.getChaptersForSubjectOnce(subjectId).firstOrNull()?.id
            ?: run {
                val newChapId = UUID.randomUUID().toString()
                chapterDao.insertChapter(ChapterEntity(id = newChapId, subjectId = subjectId, name = "General Syllabus"))
                newChapId
            }

        var genIndex = 1
        while (accumulatedMarks < targetTotal) {
            val remaining = targetTotal - accumulatedMarks
            val qMarks = when {
                remaining >= 5 && (accumulatedMarks % 5 == 0) -> 5
                remaining >= 4 && (accumulatedMarks % 4 == 0) -> 4
                remaining >= 3 && (accumulatedMarks % 3 == 0) -> 3
                remaining >= 2 -> 2
                else -> 1
            }

            val qType = when (qMarks) {
                1 -> ExamQuestionType.SHORT_ANSWER.name
                2, 3 -> ExamQuestionType.SHORT_ANSWER.name
                else -> ExamQuestionType.LONG_ANSWER.name
            }

            val scopeName = chapter?.name ?: subject.name
            val syntheticQ = QuestionBankEntity(
                id = UUID.randomUUID().toString(),
                subjectId = subjectId,
                chapterId = fallbackChapterId,
                questionText = "$scopeName Concept $genIndex: Explain the fundamental principles, key governing formulas, and practical applications in detail.",
                markingScheme = "Accurate definition & principles (${qMarks / 2}m) + Formula derivation & analytical steps (${qMarks - (qMarks / 2)}m).",
                marks = qMarks,
                questionType = qType,
                difficulty = if (qMarks >= 4) QuestionDifficulty.HARD.name else QuestionDifficulty.MEDIUM.name
            )
            questionBankDao.insertQuestion(syntheticQ)
            selectedQuestions.add(syntheticQ)
            accumulatedMarks += qMarks
            genIndex++
        }

        // 3. Group questions into formal exam sections
        val paperId = UUID.randomUUID().toString()
        val crossRefs = mutableListOf<PaperQuestionCrossRefEntity>()
        val sectionsArray = JSONArray()

        // Sort questions by marks (1m first in Section A, then 2m/3m in Section B, then 4m/5m in Section C)
        selectedQuestions.sortBy { it.marks }

        val secA = selectedQuestions.filter { it.marks == 1 }
        val secB = selectedQuestions.filter { it.marks in 2..3 }
        val secC = selectedQuestions.filter { it.marks >= 4 }

        var qNumber = 1

        fun processSection(secName: String, secDesc: String, questions: List<QuestionBankEntity>) {
            if (questions.isEmpty()) return
            val secObj = JSONObject()
            secObj.put("name", secName)
            secObj.put("description", secDesc)
            val qList = JSONArray()
            for (q in questions) {
                qList.put(q.id)
                crossRefs.add(
                    PaperQuestionCrossRefEntity(
                        paperId = paperId,
                        questionId = q.id,
                        sectionName = secName,
                        questionNumber = qNumber,
                        marksAllocated = q.marks
                    )
                )
                qNumber++
            }
            secObj.put("questionIds", qList)
            sectionsArray.put(secObj)
        }

        processSection("Section A", "Objective & Conceptual Questions (1 Mark Each)", secA)
        processSection("Section B", "Short Answer Questions (2 - 3 Marks Each)", secB)
        processSection("Section C", "Long Answer & Analytical Problems (4 - 5 Marks Each)", secC)

        // 4. Save PaperEntity
        val paper = PaperEntity(
            id = paperId,
            subjectId = subjectId,
            title = title,
            totalMarks = targetTotal,
            durationMinutes = examMarks.durationMinutes,
            sectionsJson = sectionsArray.toString(),
            pdfUri = null,
            status = PaperStatus.GENERATED.name,
            createdAt = System.currentTimeMillis()
        )
        paperDao.insertPaper(paper)
        paperDao.insertPaperQuestions(crossRefs)

        // 5. Automatically create/link an ExamEntity so it appears in Exams and pins to the Home Screen!
        val exam = ExamEntity(
            id = UUID.randomUUID().toString(),
            name = title,
            date = System.currentTimeMillis() + (examMarks.durationMinutes * 60_000L),
            targetScore = targetTotal,
            actualScore = null,
            isCompleted = false,
            notes = "[MOCK_TEST] [PAPER_ID:$paperId] MaxMarks:$targetTotal Duration:${examMarks.durationMinutes}m"
        )
        examDao.insertExam(exam)
        examDao.insertExamSubjects(listOf(ExamSubjectCrossRefEntity(exam.id, subjectId)))

        GeneratedPaperResult(
            paper = paper,
            exam = exam,
            questions = selectedQuestions,
            crossRefs = crossRefs
        )
    }
}
