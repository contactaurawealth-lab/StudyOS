package com.studyos.app.domain.usecase

import com.studyos.app.domain.model.ExamResult
import com.studyos.app.domain.model.LossReasonCategory
import com.studyos.app.domain.model.LostMark
import com.studyos.app.domain.repository.ExamResultRepository
import com.studyos.app.domain.repository.LostMarksRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class ExamDiagnostic(
    val result: ExamResult?,
    val totalMarksLost: Double,
    val categoryBreakdown: Map<LossReasonCategory, Double>,
    val topLossCategory: LossReasonCategory?,
    val lostMarksList: List<LostMark>,
    val recoveryRecommendation: String
)

class GetExamDiagnosticUseCase(
    private val examResultRepository: ExamResultRepository,
    private val lostMarksRepository: LostMarksRepository
) {
    suspend fun getDiagnosticForResult(resultId: String): ExamDiagnostic? {
        val result = examResultRepository.getResultById(resultId) ?: return null
        val lostMarks = lostMarksRepository.getLostMarksForResultOnce(resultId)
        return buildDiagnostic(result, lostMarks)
    }

    suspend fun getDiagnosticForPaper(paperId: String): ExamDiagnostic? {
        val result = examResultRepository.getResultForPaper(paperId) ?: return null
        val lostMarks = lostMarksRepository.getLostMarksForResultOnce(result.id)
        return buildDiagnostic(result, lostMarks)
    }

    fun observeDiagnosticForPaper(paperId: String): Flow<ExamDiagnostic?> {
        return examResultRepository.observeResultForPaper(paperId).map { result ->
            if (result == null) null
            else {
                val lostMarks = lostMarksRepository.getLostMarksForResultOnce(result.id)
                buildDiagnostic(result, lostMarks)
            }
        }
    }

    private fun buildDiagnostic(result: ExamResult, lostMarks: List<LostMark>): ExamDiagnostic {
        val breakdown = mutableMapOf<LossReasonCategory, Double>()
        LossReasonCategory.values().forEach { breakdown[it] = 0.0 }

        var totalLost = 0.0
        for (mark in lostMarks) {
            totalLost += mark.marksLost
            breakdown[mark.category] = (breakdown[mark.category] ?: 0.0) + mark.marksLost
        }

        // If no explicit lost marks logged, calculate difference between total and obtained
        if (totalLost == 0.0 && result.totalMarks > result.marksObtained) {
            totalLost = result.totalMarks - result.marksObtained
        }

        val topCategory = breakdown.filter { it.value > 0.0 }
            .maxByOrNull { it.value }?.key

        val recommendation = when (topCategory) {
            LossReasonCategory.CONCEPT_ERROR ->
                "Core conceptual gaps detected. These questions have been bridged into your Mistake Bank for active recall."
            LossReasonCategory.CALCULATION_ERROR ->
                "Arithmetic / calculation slips cost you the most marks. Reserve 5 minutes at the end of every test strictly for re-verifying arithmetic."
            LossReasonCategory.DIDNT_KNOW, LossReasonCategory.FORGOT ->
                "Memory retrieval failure. Target these specific topics with high-frequency active recall drills."
            LossReasonCategory.TIME_SHORTAGE ->
                "Time pressure degraded your accuracy. Practice section-wise timing (e.g. 1 mark = 1.2 minutes)."
            LossReasonCategory.CARELESS_MISTAKE, LossReasonCategory.MISREAD ->
                "Avoidable slips. Circle or underline keywords (e.g., 'NOT', 'EXCEPT', units) in the prompt before writing answers."
            LossReasonCategory.POOR_PRESENTATION, LossReasonCategory.INCOMPLETE_ANSWER ->
                "Structured answering required. Format answers with bullet points and clear intermediate steps."
            else ->
                "Review the model marking scheme and re-attempt the missed questions during your next revision session."
        }

        return ExamDiagnostic(
            result = result,
            totalMarksLost = totalLost,
            categoryBreakdown = breakdown,
            topLossCategory = topCategory,
            lostMarksList = lostMarks,
            recoveryRecommendation = recommendation
        )
    }
}
