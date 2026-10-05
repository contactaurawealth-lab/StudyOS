package com.studyos.app.domain.model

enum class LossReasonCategory(val displayName: String) {
    DIDNT_KNOW("Didn't know"),
    FORGOT("Forgot"),
    CONCEPT_ERROR("Concept error"),
    CALCULATION_ERROR("Calculation error"),
    MISREAD("Misread"),
    CARELESS_MISTAKE("Careless mistake"),
    POOR_PRESENTATION("Poor presentation"),
    TIME_SHORTAGE("Time shortage"),
    INCOMPLETE_ANSWER("Incomplete answer"),
    OTHER("Other")
}

data class LostMark(
    val id: String,
    val resultId: String,
    val questionId: String? = null,
    val topicId: String? = null,
    val marksLost: Double,
    val category: LossReasonCategory = LossReasonCategory.CONCEPT_ERROR,
    val reflection: String? = null,
    val isRemediated: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
