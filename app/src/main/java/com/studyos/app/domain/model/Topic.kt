package com.studyos.app.domain.model

enum class TopicMasteryState {
    NOT_STARTED,
    LEARNING,
    REVISED,
    MASTERED
}

enum class ExamRelevance {
    LOW,
    MEDIUM,
    HIGH
}

data class Topic(
    val id: String,
    val chapterId: String,
    val name: String,
    val masteryState: TopicMasteryState = TopicMasteryState.NOT_STARTED,
    val examRelevance: ExamRelevance = ExamRelevance.MEDIUM,
    val weaknessScore: Float = 0.0f,
    val lastRevisedAt: Long? = null,
    val orderIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
