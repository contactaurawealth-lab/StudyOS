package com.studyos.app.domain.usecase

import com.studyos.app.domain.engine.MarkLeakageAnalyticsEngine
import com.studyos.app.domain.model.MarkLeakageReport
import com.studyos.app.domain.repository.ExamResultRepository
import com.studyos.app.domain.repository.LostMarksRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class CalculateMarkLeakageUseCase(
    private val lostMarksRepository: LostMarksRepository,
    private val examResultRepository: ExamResultRepository
) {
    operator fun invoke(): Flow<MarkLeakageReport> {
        return combine(
            lostMarksRepository.getAllLostMarks(),
            examResultRepository.getAllResults()
        ) { lostMarks, results ->
            val completedExams = results.size
            val totalMarksAttempted = results.sumOf { it.totalMarks.toDouble() }
            MarkLeakageAnalyticsEngine.analyze(
                lostMarks = lostMarks,
                completedExamsCount = completedExams,
                totalMarksAttempted = totalMarksAttempted
            )
        }
    }
}
