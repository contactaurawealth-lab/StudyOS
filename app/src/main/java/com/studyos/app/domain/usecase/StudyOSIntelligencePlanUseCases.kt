package com.studyos.app.domain.usecase

import com.studyos.app.domain.engine.DailyAiPlan
import com.studyos.app.domain.engine.DailyAiPlanEngine
import com.studyos.app.domain.engine.ExamReadinessEngine
import com.studyos.app.domain.engine.OverallReadiness
import com.studyos.app.domain.engine.SubjectReadiness
import com.studyos.app.domain.model.ChapterIntelligence
import com.studyos.app.domain.model.Exam
import com.studyos.app.domain.repository.ChapterRepository
import com.studyos.app.domain.repository.ExamRepository
import com.studyos.app.domain.repository.MistakeRepository
import com.studyos.app.domain.repository.RecallRepository
import com.studyos.app.domain.repository.SubjectRepository
import kotlinx.coroutines.flow.firstOrNull

/**
 * Generates an optimized, distraction-free daily study plan based on available minutes.
 */
class GetDailyAiPlanUseCase(
    private val subjectRepository: SubjectRepository,
    private val chapterRepository: ChapterRepository,
    private val recallRepository: RecallRepository,
    private val mistakeRepository: MistakeRepository,
    private val getChapterIntelligenceUseCase: GetChapterIntelligenceUseCase
) {
    suspend operator fun invoke(availableTimeMinutes: Int): DailyAiPlan {
        val subjects = subjectRepository.getAllSubjectsOnce()
        val allChapters = chapterRepository.getAllChaptersOnce()
        val chaptersBySubject = allChapters.groupBy { it.subjectId }
        val dueRecallItems = recallRepository.getDueRecallItems()
        val allMistakes = mistakeRepository.observeAllMistakes().firstOrNull() ?: emptyList()
        val unresolvedMistakes = allMistakes.filter { !it.isResolved }

        val intelligences = allChapters.mapNotNull { chapter ->
            getChapterIntelligenceUseCase(chapter.id)
        }

        return DailyAiPlanEngine.generatePlan(
            availableTimeMinutes = availableTimeMinutes,
            subjects = subjects,
            chaptersBySubject = chaptersBySubject,
            intelligences = intelligences,
            recallDueItems = dueRecallItems,
            unresolvedMistakes = unresolvedMistakes
        )
    }
}

/**
 * Calculates overall exam readiness across all enrolled subjects.
 */
class GetOverallExamReadinessUseCase(
    private val subjectRepository: SubjectRepository,
    private val chapterRepository: ChapterRepository,
    private val examRepository: ExamRepository,
    private val recallRepository: RecallRepository,
    private val mistakeRepository: MistakeRepository,
    private val getChapterIntelligenceUseCase: GetChapterIntelligenceUseCase
) {
    suspend operator fun invoke(): OverallReadiness {
        val subjects = subjectRepository.getAllSubjectsOnce()
        val allChapters = chapterRepository.getAllChaptersOnce()
        val chaptersBySubject = allChapters.groupBy { it.subjectId }
        val dueRecallItems = recallRepository.getDueRecallItems()
        val allMistakes = mistakeRepository.observeAllMistakes().firstOrNull() ?: emptyList()
        val upcomingExams = examRepository.observeAllExams().firstOrNull() ?: emptyList()

        val intelligences = allChapters.mapNotNull { chapter ->
            getChapterIntelligenceUseCase(chapter.id)
        }
        val intelligencesBySubject = intelligences.groupBy { it.subjectId }

        return ExamReadinessEngine.calculateOverallReadiness(
            subjects = subjects,
            chaptersBySubject = chaptersBySubject,
            intelligencesBySubject = intelligencesBySubject,
            recallItems = dueRecallItems,
            mistakes = allMistakes,
            upcomingExams = upcomingExams
        )
    }
}

/**
 * Calculates deep exam readiness and 4-quadrant diagnostic for a specific subject.
 */
class GetSubjectReadinessUseCase(
    private val subjectRepository: SubjectRepository,
    private val chapterRepository: ChapterRepository,
    private val examRepository: ExamRepository,
    private val recallRepository: RecallRepository,
    private val mistakeRepository: MistakeRepository,
    private val getChapterIntelligenceUseCase: GetChapterIntelligenceUseCase
) {
    suspend operator fun invoke(subjectId: String): SubjectReadiness? {
        val subject = subjectRepository.getSubjectByIdOnce(subjectId) ?: return null
        val chapters = chapterRepository.getChaptersForSubjectOnce(subjectId)
        val intelligences = chapters.mapNotNull { getChapterIntelligenceUseCase(it.id) }
        val dueRecallItems = recallRepository.getDueRecallItems().filter { it.subjectId == subjectId }
        val allMistakes = mistakeRepository.observeAllMistakes().firstOrNull() ?: emptyList()
        val subjectMistakes = allMistakes.filter { it.subjectId == subjectId }
        val now = System.currentTimeMillis()
        val exams = examRepository.observeAllExams().firstOrNull() ?: emptyList()
        val activeExams = exams.filter { !it.isCompleted && it.getDaysRemaining(now) >= 0L }
        val exam = activeExams.firstOrNull { it.subjectIds.contains(subjectId) }
            ?: activeExams.minByOrNull { it.targetDate }

        return ExamReadinessEngine.calculateSubjectReadiness(
            subject = subject,
            chapters = chapters,
            chapterIntelligences = intelligences,
            recallItems = dueRecallItems,
            mistakes = subjectMistakes,
            exam = exam
        )
    }
}
