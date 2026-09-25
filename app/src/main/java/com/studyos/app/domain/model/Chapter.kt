package com.studyos.app.domain.model

import java.util.UUID
import kotlin.math.roundToInt

enum class ChapterStatus {
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETED
}

data class Chapter(
    val id: String = UUID.randomUUID().toString(),
    val subjectId: String,
    val name: String,
    val description: String? = null,
    val orderIndex: Int = 0,
    val status: ChapterStatus = ChapterStatus.NOT_STARTED,
    val progress: Int = 0,
    val isImportant: Boolean = false,
    val confidence: Int = 3,
    val lastOpenedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    init {
        require(progress in 0..100) { "Progress must be between 0 and 100, got: $progress" }
        require(confidence in 1..5) { "Confidence must be between 1 and 5, got: $confidence" }
    }

    /**
     * Feature 3: WEAK CHAPTER DETECTOR
     * Automatically identifies chapters with:
     * - Low completion (< 50%)
     * - Low revision frequency (<= 1)
     * - Low confidence (<= 2)
     */
    fun isNeedsAttention(revisionCount: Int = 0): Boolean {
        return progress < 50 || revisionCount <= 1 || confidence <= 2
    }

    companion object {
        fun create(
            subjectId: String,
            name: String,
            description: String? = null,
            orderIndex: Int = 0,
            progress: Int = 0,
            isImportant: Boolean = false,
            confidence: Int = 3
        ): Chapter {
            val clamped = progress.coerceIn(0, 100)
            val derivedStatus = statusForProgress(clamped)
            return Chapter(
                subjectId = subjectId,
                name = name,
                description = description,
                orderIndex = orderIndex,
                status = derivedStatus,
                progress = clamped,
                isImportant = isImportant,
                confidence = confidence.coerceIn(1, 5)
            )
        }
    }
}

data class SubjectWithProgress(
    val subject: Subject,
    val chapterCount: Int,
    val completedChapterCount: Int,
    val inProgressChapterCount: Int = 0,
    val notStartedChapterCount: Int = 0,
    val progress: Int,
    val currentChapterName: String? = null,
    val strongCount: Int = 0,
    val weakCount: Int = 0,
    val dueCount: Int = 0,
    val readinessScore: Int = 0,
    val weakChapterId: String? = null,
    val weakChapterName: String? = null,
    val understandingPercentage: Int = 0,
    val recallPercentage: Int = 0,
    val practicePercentage: Int = 0,
    val unresolvedMistakesCount: Int = 0,
    val insight: String? = null
)

data class ChapterSearchResult(
    val chapter: Chapter,
    val subjectName: String
)

fun calculateSubjectProgress(chapters: List<Chapter>): Int {
    if (chapters.isEmpty()) return 0
    return ((chapters.sumOf { it.progress }.toFloat() / chapters.size.toFloat())).roundToInt().coerceIn(0, 100)
}

fun calculateOverallProgress(chapters: List<Chapter>): Int {
    if (chapters.isEmpty()) return 0
    return ((chapters.sumOf { it.progress }.toFloat() / chapters.size.toFloat())).roundToInt().coerceIn(0, 100)
}

fun statusForProgress(progress: Int): ChapterStatus {
    val clamped = progress.coerceIn(0, 100)
    return when {
        clamped == 100 -> ChapterStatus.COMPLETED
        clamped > 0 -> ChapterStatus.IN_PROGRESS
        else -> ChapterStatus.NOT_STARTED
    }
}

fun progressForStatus(status: ChapterStatus, currentProgress: Int = 0): Int {
    return when (status) {
        ChapterStatus.COMPLETED -> 100
        ChapterStatus.NOT_STARTED -> 0
        ChapterStatus.IN_PROGRESS -> if (currentProgress in 1..99) currentProgress else 50
    }
}
