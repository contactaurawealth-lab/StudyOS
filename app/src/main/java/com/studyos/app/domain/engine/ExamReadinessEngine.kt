package com.studyos.app.domain.engine

import com.studyos.app.domain.model.Chapter
import com.studyos.app.domain.model.ChapterIntelligence
import com.studyos.app.domain.model.Exam
import com.studyos.app.domain.model.IntelligentChapterStatus
import com.studyos.app.domain.model.Mistake
import com.studyos.app.domain.model.RecallItem
import com.studyos.app.domain.model.Subject
import kotlin.math.roundToInt

data class SubjectReadiness(
    val subjectId: String,
    val subjectName: String,
    val readinessPercentage: Int,
    val syllabusPercentage: Int,
    val understandingPercentage: Int,
    val recallPercentage: Int,
    val practicePercentage: Int,
    val insight: String,
    val daysUntilExam: Long? = null,
    val weakChapters: List<String> = emptyList(),
    val totalChapters: Int = 0,
    val strongChaptersCount: Int = 0,
    val weakChaptersCount: Int = 0,
    val dueChaptersCount: Int = 0
)

data class OverallReadiness(
    val overallPercentage: Int,
    val subjectsReadiness: List<SubjectReadiness>,
    val primaryInsight: String,
    val upcomingExamName: String? = null,
    val upcomingExamDaysLeft: Long? = null
)

/**
 * Intelligent Exam Readiness Engine.
 *
 * Considers:
 * - Syllabus completion
 * - Concept Understanding
 * - Active Recall retention
 * - Practice accuracy
 * - Unresolved mistakes
 * - Revision freshness
 * - Exam date proximity
 * - Weak chapters
 */
object ExamReadinessEngine {

    private const val ONE_DAY_MS = 86_400_000L

    fun calculateSubjectReadiness(
        subject: Subject,
        chapters: List<Chapter>,
        chapterIntelligences: List<ChapterIntelligence>,
        recallItems: List<RecallItem> = emptyList(),
        mistakes: List<Mistake> = emptyList(),
        exam: Exam? = null
    ): SubjectReadiness {
        if (chapters.isEmpty()) {
            return SubjectReadiness(
                subjectId = subject.id,
                subjectName = subject.name,
                readinessPercentage = 0,
                syllabusPercentage = 0,
                understandingPercentage = 0,
                recallPercentage = 0,
                practicePercentage = 0,
                insight = "Add chapters to calculate exam readiness."
            )
        }

        // 1. Syllabus Completion (Average of chapter progresses)
        val syllabusAvg = chapters.map { it.progress }.average().roundToInt().coerceIn(0, 100)

        // 2. Understanding Average
        val understandingAvg = if (chapterIntelligences.isNotEmpty()) {
            chapterIntelligences.map { it.understandingPercentage }.average().roundToInt().coerceIn(0, 100)
        } else {
            (syllabusAvg * 0.7).roundToInt()
        }

        // 3. Recall Average
        val recallAvg = if (chapterIntelligences.isNotEmpty()) {
            chapterIntelligences.map { it.recallPercentage }.average().roundToInt().coerceIn(0, 100)
        } else {
            (syllabusAvg * 0.5).roundToInt()
        }

        // 4. Practice Average
        val practiceAvg = if (chapterIntelligences.isNotEmpty()) {
            chapterIntelligences.map { it.practicePercentage }.average().roundToInt().coerceIn(0, 100)
        } else {
            (syllabusAvg * 0.6).roundToInt()
        }

        // 5. Unresolved mistakes deduction
        val subjectMistakes = mistakes.filter { it.subjectId == subject.id && !it.isResolved }
        val mistakePenalty = (subjectMistakes.size * 2).coerceAtMost(15)

        // 6. Chapter status counts
        val strongCount = chapterIntelligences.count {
            it.status == IntelligentChapterStatus.STRONG || it.status == IntelligentChapterStatus.MASTERED
        }
        val weakCount = chapterIntelligences.count {
            it.status == IntelligentChapterStatus.NEEDS_ATTENTION
        }
        val dueCount = chapterIntelligences.count {
            it.status == IntelligentChapterStatus.REVISION_DUE
        }

        // 7. Base Weighted Composite:
        // Syllabus 25% + Understanding 30% + Recall 25% + Practice 20% - Mistake Penalty
        val rawScore = (
            (syllabusAvg * 0.25) +
            (understandingAvg * 0.30) +
            (recallAvg * 0.25) +
            (practiceAvg * 0.20) - mistakePenalty
        ).roundToInt().coerceIn(0, 100)

        // Days until exam
        val now = System.currentTimeMillis()
        val daysUntil = exam?.let {
            ((it.targetDate - now) / ONE_DAY_MS).coerceAtLeast(0)
        }

        // Generate Short Explanatory Insight
        val insight = when {
            syllabusAvg < 50 -> "Syllabus coverage is low; prioritize learning new chapters."
            recallAvg < 50 && syllabusAvg >= 70 -> "Your syllabus is mostly complete, but recall needs revision."
            weakCount >= 2 -> "$weakCount chapters require attention before exam day."
            subjectMistakes.size >= 4 -> "${subjectMistakes.size} unreviewed mistakes are holding readiness back."
            rawScore >= 80 -> "High readiness! Continue maintaining recall freshness."
            else -> "Steady progress. Balance practice quizzes with flashcard recall."
        }

        val weakChapterNames = chapterIntelligences
            .filter { it.status == IntelligentChapterStatus.NEEDS_ATTENTION }
            .map { it.chapterName }

        return SubjectReadiness(
            subjectId = subject.id,
            subjectName = subject.name,
            readinessPercentage = rawScore,
            syllabusPercentage = syllabusAvg,
            understandingPercentage = understandingAvg,
            recallPercentage = recallAvg,
            practicePercentage = practiceAvg,
            insight = insight,
            daysUntilExam = daysUntil,
            weakChapters = weakChapterNames,
            totalChapters = chapters.size,
            strongChaptersCount = strongCount,
            weakChaptersCount = weakCount,
            dueChaptersCount = dueCount
        )
    }

    fun calculateOverallReadiness(
        subjects: List<Subject>,
        chaptersBySubject: Map<String, List<Chapter>>,
        intelligencesBySubject: Map<String, List<ChapterIntelligence>>,
        recallItems: List<RecallItem> = emptyList(),
        mistakes: List<Mistake> = emptyList(),
        upcomingExams: List<Exam> = emptyList()
    ): OverallReadiness {
        if (subjects.isEmpty()) {
            return OverallReadiness(
                overallPercentage = 0,
                subjectsReadiness = emptyList(),
                primaryInsight = "Add your subjects to start tracking exam readiness."
            )
        }

        val now = System.currentTimeMillis()
        val activeUpcomingExams = upcomingExams.filter { !it.isCompleted && it.getDaysRemaining(now) >= 0L }
        val examsBySubject = mutableMapOf<String, Exam>()
        activeUpcomingExams.forEach { exam ->
            exam.subjectIds.forEach { subId ->
                if (!examsBySubject.containsKey(subId) || exam.targetDate < examsBySubject[subId]!!.targetDate) {
                    examsBySubject[subId] = exam
                }
            }
        }
        val nearestExam = activeUpcomingExams.minByOrNull { it.targetDate }
        val examDays = nearestExam?.getDaysRemaining(now)

        val subjectList = subjects.map { subj ->
            calculateSubjectReadiness(
                subject = subj,
                chapters = chaptersBySubject[subj.id] ?: emptyList(),
                chapterIntelligences = intelligencesBySubject[subj.id] ?: emptyList(),
                recallItems = recallItems,
                mistakes = mistakes,
                exam = examsBySubject[subj.id] ?: nearestExam
            )
        }

        val avgReadiness = if (subjectList.isNotEmpty()) {
            subjectList.map { it.readinessPercentage }.average().roundToInt().coerceIn(0, 100)
        } else 0

        val lowestSubject = subjectList.minByOrNull { it.readinessPercentage }
        val primaryInsight = when {
            nearestExam != null && examDays != null && examDays <= 7 ->
                "${nearestExam.name} in $examDays days: focus on ${lowestSubject?.subjectName ?: "weak topics"}."
            lowestSubject != null && lowestSubject.readinessPercentage < 60 ->
                "${lowestSubject.subjectName} needs review (${lowestSubject.readinessPercentage}% readiness)."
            avgReadiness >= 75 -> "Strong multi-subject readiness across active courses."
            else -> "Regular recall sessions will steadily boost readiness."
        }

        return OverallReadiness(
            overallPercentage = avgReadiness,
            subjectsReadiness = subjectList,
            primaryInsight = primaryInsight,
            upcomingExamName = nearestExam?.name,
            upcomingExamDaysLeft = examDays
        )
    }
}
