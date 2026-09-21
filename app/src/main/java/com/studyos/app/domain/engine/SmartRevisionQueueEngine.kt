package com.studyos.app.domain.engine

import com.studyos.app.domain.model.Chapter
import com.studyos.app.domain.model.ChapterIntelligence
import com.studyos.app.domain.model.Exam
import com.studyos.app.domain.model.IntelligentChapterStatus
import com.studyos.app.domain.model.Mistake
import com.studyos.app.domain.model.RecallItem
import com.studyos.app.domain.model.Subject

enum class RevisionQueueReasonType {
    RECALL_OVERDUE,
    HIGH_MISTAKE_RATE,
    EXAM_APPROACHING,
    WEAK_CONCEPT_GAP,
    NEGLECTED_TOPIC
}

data class PriorityQueueItem(
    val id: String,
    val title: String,
    val subjectName: String,
    val reason: String,
    val reasonType: RevisionQueueReasonType,
    val priorityScore: Int,
    val subjectId: String,
    val chapterId: String? = null
)

object SmartRevisionQueueEngine {

    private const val ONE_DAY_MS = 86_400_000L

    fun buildQueue(
        chapters: List<Chapter>,
        subjects: List<Subject>,
        intelligences: List<ChapterIntelligence>,
        exams: List<Exam> = emptyList(),
        recallItems: List<RecallItem> = emptyList(),
        mistakes: List<Mistake> = emptyList()
    ): List<PriorityQueueItem> {
        val subjectMap = subjects.associateBy { it.id }
        val intelByChapter = intelligences.associateBy { it.chapterId }
        val mistakesByChapter = mistakes.filter { !it.isResolved }.groupBy { it.chapterId }
        val now = System.currentTimeMillis()

        val items = mutableListOf<PriorityQueueItem>()

        chapters.forEach { chapter ->
            val subject = subjectMap[chapter.subjectId] ?: return@forEach
            val intel = intelByChapter[chapter.id]
            val chMistakes = mistakesByChapter[chapter.id] ?: emptyList()
            val chRecallDue = recallItems.filter { it.chapterId == chapter.id && it.isDue }

            // 1. Check Exam Approaching
            val relevantExam = exams.firstOrNull { exam ->
                exam.subjectIds.contains(subject.id) || exam.subjectIds.isEmpty()
            }
            val daysToExam = relevantExam?.let { ((it.targetDate - now) / ONE_DAY_MS).coerceAtLeast(0) }

            when {
                // Exam within 14 days and chapter not mastered
                daysToExam != null && daysToExam <= 14 && (intel == null || intel.overallPercentage < 85) -> {
                    items.add(
                        PriorityQueueItem(
                            id = "exam_${chapter.id}",
                            title = chapter.name,
                            subjectName = subject.name,
                            reason = "Exam approaching (${daysToExam}d left)",
                            reasonType = RevisionQueueReasonType.EXAM_APPROACHING,
                            priorityScore = 95 - daysToExam.toInt() * 2,
                            subjectId = subject.id,
                            chapterId = chapter.id
                        )
                    )
                }

                // Recall Overdue
                chRecallDue.isNotEmpty() -> {
                    items.add(
                        PriorityQueueItem(
                            id = "recall_${chapter.id}",
                            title = chapter.name,
                            subjectName = subject.name,
                            reason = "Recall overdue (${chRecallDue.size} prompts due)",
                            reasonType = RevisionQueueReasonType.RECALL_OVERDUE,
                            priorityScore = 88 + chRecallDue.size.coerceAtMost(10),
                            subjectId = subject.id,
                            chapterId = chapter.id
                        )
                    )
                }

                // High Mistake Rate
                chMistakes.size >= 2 -> {
                    items.add(
                        PriorityQueueItem(
                            id = "mistake_${chapter.id}",
                            title = chapter.name,
                            subjectName = subject.name,
                            reason = "High mistake rate (${chMistakes.size} unreviewed)",
                            reasonType = RevisionQueueReasonType.HIGH_MISTAKE_RATE,
                            priorityScore = 82 + chMistakes.size * 2,
                            subjectId = subject.id,
                            chapterId = chapter.id
                        )
                    )
                }

                // Weak Concept Gap
                intel?.status == IntelligentChapterStatus.NEEDS_ATTENTION -> {
                    items.add(
                        PriorityQueueItem(
                            id = "weak_${chapter.id}",
                            title = chapter.name,
                            subjectName = subject.name,
                            reason = "Weak concept gap (${intel.overallPercentage}% mastery)",
                            reasonType = RevisionQueueReasonType.WEAK_CONCEPT_GAP,
                            priorityScore = 78,
                            subjectId = subject.id,
                            chapterId = chapter.id
                        )
                    )
                }

                // Neglected Topic (has progress but haven't studied in over 7 days)
                chapter.progress in 1..99 && (now - chapter.updatedAt) > 7 * ONE_DAY_MS -> {
                    val daysInactive = ((now - chapter.updatedAt) / ONE_DAY_MS).toInt()
                    items.add(
                        PriorityQueueItem(
                            id = "neglected_${chapter.id}",
                            title = chapter.name,
                            subjectName = subject.name,
                            reason = "Neglected chapter ($daysInactive days inactive)",
                            reasonType = RevisionQueueReasonType.NEGLECTED_TOPIC,
                            priorityScore = 65 + daysInactive.coerceAtMost(15),
                            subjectId = subject.id,
                            chapterId = chapter.id
                        )
                    )
                }
            }
        }

        return items.sortedByDescending { it.priorityScore }
    }
}
