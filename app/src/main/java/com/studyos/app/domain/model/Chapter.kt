package com.studyos.app.domain.model

import java.util.UUID

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
    val lastOpenedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    init {
        require(progress in 0..100) { "Progress must be between 0 and 100, got: $progress" }
    }

    companion object {
        fun create(
            subjectId: String,
            name: String,
            description: String? = null,
            orderIndex: Int = 0,
            progress: Int = 0
        ): Chapter {
            val clamped = progress.coerceIn(0, 100)
            val derivedStatus = statusForProgress(clamped)
            return Chapter(
                subjectId = subjectId,
                name = name,
                description = description,
                orderIndex = orderIndex,
                status = derivedStatus,
                progress = clamped
            )
        }
    }
}

data class SubjectWithProgress(
    val subject: Subject,
    val chapterCount: Int,
    val completedChapterCount: Int,
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
    return chapters.sumOf { it.progress } / chapters.size
}

fun calculateOverallProgress(chapters: List<Chapter>): Int {
    if (chapters.isEmpty()) return 0
    return chapters.sumOf { it.progress } / chapters.size
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
