package com.studyos.app.domain.usecase

import com.studyos.app.domain.model.ExamResult
import com.studyos.app.domain.model.LossReasonCategory
import com.studyos.app.domain.model.LostMark
import com.studyos.app.domain.model.Mistake
import com.studyos.app.domain.model.PaperStatus
import com.studyos.app.domain.repository.ExamRepository
import com.studyos.app.domain.repository.ExamResultRepository
import com.studyos.app.domain.repository.LostMarksRepository
import com.studyos.app.domain.repository.MistakeRepository
import com.studyos.app.domain.repository.PaperRepository
import kotlinx.coroutines.flow.firstOrNull
import java.util.UUID
import kotlin.math.roundToInt

data class LostMarkInput(
    val questionId: String? = null,
    val topicId: String? = null,
    val chapterId: String? = null,
    val subjectId: String? = null,
    val marksLost: Double,
    val category: LossReasonCategory = LossReasonCategory.CONCEPT_ERROR,
    val reflection: String? = null,
    val questionText: String? = null,
    val studentAnswer: String? = null,
    val correctAnswer: String? = null,
    val explanation: String? = null
)

data class RecordExamResultOutput(
    val result: ExamResult,
    val savedLostMarks: List<LostMark>,
    val mistakesCreated: Int
)

class RecordExamResultUseCase(
    private val examResultRepository: ExamResultRepository,
    private val lostMarksRepository: LostMarksRepository,
    private val mistakeRepository: MistakeRepository,
    private val examRepository: ExamRepository? = null,
    private val paperRepository: PaperRepository? = null
) {
    suspend operator fun invoke(
        paperId: String,
        marksObtained: Double,
        totalMarks: Double,
        timeTakenMinutes: Int,
        notesOrFeedback: String? = null,
        lostMarks: List<LostMarkInput> = emptyList(),
        examId: String? = null
    ): RecordExamResultOutput {
        val safeTotal = if (totalMarks <= 0.0) 100.0 else totalMarks
        val safeObtained = marksObtained.coerceIn(0.0, safeTotal)
        val percentage = (safeObtained / safeTotal) * 100.0

        val resultId = UUID.randomUUID().toString()
        val examResult = ExamResult(
            id = resultId,
            paperId = paperId,
            marksObtained = safeObtained,
            totalMarks = safeTotal,
            percentage = percentage,
            timeTakenMinutes = timeTakenMinutes,
            notesOrFeedback = notesOrFeedback,
            examDate = System.currentTimeMillis()
        )
        examResultRepository.saveResult(examResult)

        // Update Paper status if available
        paperRepository?.updatePaperStatus(paperId, PaperStatus.EVALUATED)

        // Save lost marks and feed to Mistake Bank
        val lostMarkEntities = mutableListOf<LostMark>()
        var mistakesCount = 0

        for (input in lostMarks) {
            val lostMark = LostMark(
                id = UUID.randomUUID().toString(),
                resultId = resultId,
                questionId = input.questionId,
                topicId = input.topicId,
                marksLost = input.marksLost,
                category = input.category,
                reflection = input.reflection,
                isRemediated = false,
                createdAt = System.currentTimeMillis()
            )
            lostMarkEntities.add(lostMark)

            // Connect to Mistake Bank if question text and subject are present
            if (!input.questionText.isNullOrBlank() && !input.subjectId.isNullOrBlank()) {
                val mistake = Mistake(
                    id = UUID.randomUUID().toString(),
                    question = input.questionText,
                    studentAnswer = input.studentAnswer ?: "Marks lost: ${input.marksLost} (${input.category.displayName})",
                    correctAnswer = input.correctAnswer ?: "See model marking scheme",
                    explanation = input.explanation ?: input.reflection,
                    subjectId = input.subjectId,
                    chapterId = input.chapterId,
                    topic = input.topicId,
                    missedCount = 1,
                    lastMissedAt = System.currentTimeMillis(),
                    isResolved = false
                )
                try {
                    mistakeRepository.recordMistake(mistake)
                    mistakesCount++
                } catch (_: Exception) {}
            }
        }

        if (lostMarkEntities.isNotEmpty()) {
            lostMarksRepository.saveLostMarks(lostMarkEntities)
        }

        // Link with ExamEntity if provided or found by tag
        val finalScoreInt = percentage.roundToInt()
        if (!examId.isNullOrBlank()) {
            examRepository?.updateExamScore(examId, finalScoreInt, isCompleted = true)
        } else {
            val allExams = examRepository?.observeAllExams()?.firstOrNull() ?: emptyList()
            val matched = allExams.firstOrNull { it.notes?.contains("[PAPER_ID:$paperId]") == true }
            matched?.let {
                examRepository?.updateExamScore(it.id, finalScoreInt, isCompleted = true)
            }
        }

        return RecordExamResultOutput(
            result = examResult,
            savedLostMarks = lostMarkEntities,
            mistakesCreated = mistakesCount
        )
    }
}
