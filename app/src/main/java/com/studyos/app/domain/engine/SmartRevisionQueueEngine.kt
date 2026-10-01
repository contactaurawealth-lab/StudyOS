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
            val relevantExam = exams.filter { !it.isCompleted && it.getDaysRemaining(now) >= 0L }
                .firstOrNull { exam -> exam.subjectIds.contains(subject.id) || exam.subjectIds.isEmpty() }
            val daysToExam = relevantExam?.getDaysRemaining(now)

            // Multi-factor cognitive risk evaluation
            val activeFactors = mutableListOf<Pair<RevisionQueueReasonType, Pair<Int, String>>>()

            // 1. Check Exam Approaching
            if (daysToExam != null && daysToExam <= 14 && (intel == null || intel.overallPercentage < 85)) {
                val score = 95 - daysToExam.toInt() * 2
                val reason = if (daysToExam == 0L) "Exam TODAY!" else "Exam approaching (${daysToExam}d left)"
                activeFactors.add(RevisionQueueReasonType.EXAM_APPROACHING to (score to reason))
            }

            // 2. Recall Overdue
            if (chRecallDue.isNotEmpty()) {
                val score = 88 + chRecallDue.size.coerceAtMost(10)
                val reason = "Recall overdue (${chRecallDue.size} prompts due)"
                activeFactors.add(RevisionQueueReasonType.RECALL_OVERDUE to (score to reason))
            }

            // 3. High Mistake Rate
            if (chMistakes.size >= 2) {
                val score = 82 + chMistakes.size * 2
                val reason = "High mistake rate (${chMistakes.size} unreviewed)"
                activeFactors.add(RevisionQueueReasonType.HIGH_MISTAKE_RATE to (score to reason))
            }

            // 4. Weak Concept Gap
            if (intel?.status == IntelligentChapterStatus.NEEDS_ATTENTION) {
                val score = 78
                val reason = "Weak concept gap (${intel.overallPercentage}% mastery)"
                activeFactors.add(RevisionQueueReasonType.WEAK_CONCEPT_GAP to (score to reason))
            }

            // 5. Neglected Topic (has progress but haven't studied in over 7 days)
            if (chapter.progress in 1..99 && (now - chapter.updatedAt) > 7 * ONE_DAY_MS) {
                val daysInactive = ((now - chapter.updatedAt) / ONE_DAY_MS).toInt()
                val score = 65 + daysInactive.coerceAtMost(15)
                val reason = "Neglected chapter ($daysInactive days inactive)"
                activeFactors.add(RevisionQueueReasonType.NEGLECTED_TOPIC to (score to reason))
            }

            if (activeFactors.isNotEmpty()) {
                // Sort factors by score descending
                activeFactors.sortByDescending { it.second.first }
                val primary = activeFactors.first()
                val primaryType = primary.first
                val baseScore = primary.second.first

                // Composite score bonus for multiple co-occurring risk factors
                val secondaryBonus = (activeFactors.size - 1) * 3
                val compositeScore = (baseScore + secondaryBonus).coerceIn(0, 100)

                // Combined descriptive reason if secondary cognitive risks exist
                val combinedReason = if (activeFactors.size > 1) {
                    val secondaryNotes = activeFactors.drop(1).joinToString(" • ") { it.second.second }
                    "${primary.second.second} • $secondaryNotes"
                } else {
                    primary.second.second
                }

                items.add(
                    PriorityQueueItem(
                        id = "rev_${chapter.id}",
                        title = chapter.name,
                        subjectName = subject.name,
                        reason = combinedReason,
                        reasonType = primaryType,
                        priorityScore = compositeScore,
                        subjectId = subject.id,
                        chapterId = chapter.id
                    )
                )
            }
        }

        return items.sortedByDescending { it.priorityScore }
    }
}
