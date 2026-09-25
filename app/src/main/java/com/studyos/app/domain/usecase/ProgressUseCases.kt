package com.studyos.app.domain.usecase

import com.studyos.app.domain.model.ChapterStatus
import com.studyos.app.domain.model.calculateOverallProgress
import com.studyos.app.domain.model.calculateSubjectProgress
import com.studyos.app.domain.repository.ChapterRepository
import com.studyos.app.domain.repository.SubjectRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class AcademicProgress(
    val overallProgress: Int = 0,
    val totalChapters: Int = 0,
    val completedChapters: Int = 0,
    val subjectBreakdowns: List<SubjectProgressBreakdown> = emptyList()
)

data class SubjectProgressBreakdown(
    val subjectId: String,
    val subjectName: String,
    val totalChapters: Int,
    val completedChapters: Int,
    val progress: Int
)

class GetAcademicProgressUseCase(
    private val subjectRepository: SubjectRepository,
    private val chapterRepository: ChapterRepository
) {
    operator fun invoke(): Flow<AcademicProgress> {
        return combine(
            subjectRepository.getAllSubjects(),
            chapterRepository.observeAllChapters()
        ) { subjects, chapters ->
            if (chapters.isEmpty()) {
                val emptyBreakdowns = subjects.map { subject ->
                    SubjectProgressBreakdown(
                        subjectId = subject.id,
                        subjectName = subject.name,
                        totalChapters = 0,
                        completedChapters = 0,
                        progress = 0
                    )
                }
                return@combine AcademicProgress(
                    overallProgress = 0,
                    totalChapters = 0,
                    completedChapters = 0,
                    subjectBreakdowns = emptyBreakdowns
                )
            }

            val chaptersBySubject = chapters.groupBy { it.subjectId }
            val completedCount = chapters.count { it.status == ChapterStatus.COMPLETED || it.progress == 100 }
            val overall = calculateOverallProgress(chapters)

            val breakdowns = subjects.map { subject ->
                val subjectChapters = chaptersBySubject[subject.id] ?: emptyList()
                val subjectCompleted = subjectChapters.count { it.status == ChapterStatus.COMPLETED || it.progress == 100 }
                SubjectProgressBreakdown(
                    subjectId = subject.id,
                    subjectName = subject.name,
                    totalChapters = subjectChapters.size,
                    completedChapters = subjectCompleted,
                    progress = calculateSubjectProgress(subjectChapters)
                )
            }

            AcademicProgress(
                overallProgress = overall,
                totalChapters = chapters.size,
                completedChapters = completedCount,
                subjectBreakdowns = breakdowns
            )
        }
    }
}
