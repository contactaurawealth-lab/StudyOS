package com.studyos.app.domain.model

import java.util.UUID

enum class StudySessionStatus {
    PLANNED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}

data class StudySession(
    val id: String = UUID.randomUUID().toString(),
    val subjectId: String? = null,
    val chapterId: String? = null,
    val title: String,
    val scheduledStart: Long? = null,
    val scheduledEnd: Long? = null,
    val plannedMinutes: Int,
    val actualMinutes: Int = 0,
    val status: StudySessionStatus = StudySessionStatus.PLANNED,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    init {
        require(plannedMinutes > 0) { "Planned minutes must be greater than 0, got: $plannedMinutes" }
        require(actualMinutes >= 0) { "Actual minutes cannot be negative, got: $actualMinutes" }
    }
}

sealed class FocusItem {
    data class SessionFocus(
        val session: StudySession,
        val subjectName: String,
        val chapterName: String? = null
    ) : FocusItem()

    data class ChapterFocus(
        val chapter: Chapter,
        val subjectName: String,
        val isLowestProgress: Boolean = false
    ) : FocusItem()
}

data class StudySessionItem(
    val session: StudySession,
    val subjectName: String,
    val chapterName: String? = null,
    val formattedTime: String
)

data class RecentChapterItem(
    val chapter: Chapter,
    val subjectName: String
)
