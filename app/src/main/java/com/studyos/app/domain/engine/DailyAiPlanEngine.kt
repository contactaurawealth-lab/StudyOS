package com.studyos.app.domain.engine

import com.studyos.app.domain.model.Chapter
import com.studyos.app.domain.model.ChapterIntelligence
import com.studyos.app.domain.model.IntelligentChapterStatus
import com.studyos.app.domain.model.Mistake
import com.studyos.app.domain.model.RecallItem
import com.studyos.app.domain.model.Subject

enum class DailyBlockType {
    STUDY_CHAPTER,
    ACTIVE_RECALL,
    MISTAKE_REVIEW,
    PRACTICE_QUIZ
}

data class DailyPlanBlock(
    val id: String,
    val title: String,
    val subtitle: String,
    val durationMinutes: Int,
    val type: DailyBlockType,
    val subjectId: String? = null,
    val chapterId: String? = null,
    val isCompleted: Boolean = false
)

data class DailyAiPlan(
    val totalAllocatedMinutes: Int,
    val targetTimeMinutes: Int,
    val blocks: List<DailyPlanBlock>
)

object DailyAiPlanEngine {

    fun generatePlan(
        availableTimeMinutes: Int,
        subjects: List<Subject>,
        chaptersBySubject: Map<String, List<Chapter>>,
        intelligences: List<ChapterIntelligence>,
        recallDueItems: List<RecallItem>,
        unresolvedMistakes: List<Mistake>
    ): DailyAiPlan {
        val targetMinutes = availableTimeMinutes.coerceIn(15, 360)
        var remainingMinutes = targetMinutes
        val blocks = mutableListOf<DailyPlanBlock>()
        val subjectMap = subjects.associateBy { it.id }

        // 1. High priority: Active Recall if items are due
        if (recallDueItems.isNotEmpty() && remainingMinutes >= 15) {
            val recallTime = if (remainingMinutes >= 45) 15 else 10
            val primaryChapter = intelligences.firstOrNull { it.status == IntelligentChapterStatus.REVISION_DUE }
            val subjectName = primaryChapter?.let { subjectMap[it.subjectId]?.name } ?: "Core"
            blocks.add(
                DailyPlanBlock(
                    id = "recall_block",
                    title = "$subjectName — Active Recall",
                    subtitle = "${recallDueItems.size} key concepts scheduled for retrieval",
                    durationMinutes = recallTime,
                    type = DailyBlockType.ACTIVE_RECALL,
                    subjectId = primaryChapter?.subjectId,
                    chapterId = primaryChapter?.chapterId
                )
            )
            remainingMinutes -= recallTime
        }

        // 2. High priority: Weakest / Most urgent chapter to study or revise
        val priorityChapter = intelligences
            .sortedWith(
                compareBy(
                    { it.status != IntelligentChapterStatus.NEEDS_ATTENTION },
                    { it.status != IntelligentChapterStatus.REVISION_DUE },
                    { it.overallPercentage }
                )
            ).firstOrNull()

        if (priorityChapter != null && remainingMinutes >= 20) {
            val studyDuration = when {
                remainingMinutes >= 45 -> 25
                remainingMinutes >= 30 -> 20
                else -> remainingMinutes
            }
            val subject = subjectMap[priorityChapter.subjectId]
            blocks.add(
                DailyPlanBlock(
                    id = "chapter_block_1",
                    title = "${subject?.name ?: "Study"} — ${priorityChapter.chapterName}",
                    subtitle = if (priorityChapter.status == IntelligentChapterStatus.NEEDS_ATTENTION) "Targeting weak concept gap" else "Progress & concept mastery",
                    durationMinutes = studyDuration,
                    type = DailyBlockType.STUDY_CHAPTER,
                    subjectId = priorityChapter.subjectId,
                    chapterId = priorityChapter.chapterId
                )
            )
            remainingMinutes -= studyDuration
        }

        // 3. Second chapter if there is still at least 20 minutes left
        val secondChapter = intelligences
            .filter { it.chapterId != priorityChapter?.chapterId }
            .firstOrNull { it.status == IntelligentChapterStatus.LEARNING || it.status == IntelligentChapterStatus.NOT_STARTED }

        if (secondChapter != null && remainingMinutes >= 20) {
            val studyDuration = if (remainingMinutes >= 30) 20 else remainingMinutes
            val subject = subjectMap[secondChapter.subjectId]
            blocks.add(
                DailyPlanBlock(
                    id = "chapter_block_2",
                    title = "${subject?.name ?: "Study"} — ${secondChapter.chapterName}",
                    subtitle = "Syllabus progression",
                    durationMinutes = studyDuration,
                    type = DailyBlockType.STUDY_CHAPTER,
                    subjectId = secondChapter.subjectId,
                    chapterId = secondChapter.chapterId
                )
            )
            remainingMinutes -= studyDuration
        }

        // 4. Mistake Review if mistakes exist and we have remaining time (or add 10 min)
        if (unresolvedMistakes.isNotEmpty() && remainingMinutes >= 10) {
            val mistakeTime = remainingMinutes.coerceAtMost(15)
            blocks.add(
                DailyPlanBlock(
                    id = "mistake_block",
                    title = "Mistake Bank Review",
                    subtitle = "Reviewing ${unresolvedMistakes.size.coerceAtMost(5)} previously missed items",
                    durationMinutes = mistakeTime,
                    type = DailyBlockType.MISTAKE_REVIEW
                )
            )
            remainingMinutes -= mistakeTime
        }

        // Fallback: If no blocks generated (brand new user), create initial planning block
        if (blocks.isEmpty()) {
            blocks.add(
                DailyPlanBlock(
                    id = "init_block",
                    title = "Setup & First Chapter Study",
                    subtitle = "Begin your syllabus coverage",
                    durationMinutes = targetMinutes,
                    type = DailyBlockType.STUDY_CHAPTER
                )
            )
        }

        val totalAllocated = blocks.sumOf { it.durationMinutes }
        return DailyAiPlan(
            totalAllocatedMinutes = totalAllocated,
            targetTimeMinutes = targetMinutes,
            blocks = blocks
        )
    }
}
