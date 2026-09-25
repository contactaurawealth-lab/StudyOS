package com.studyos.app.domain.model

import java.util.UUID

data class StudyPreferences(
    val id: String = UUID.randomUUID().toString(),
    val dailyStudyGoalMinutes: Int = 60,
    val defaultSessionMinutes: Int = 45,
    val preferredStartMinutes: Int? = null,
    val preferredEndMinutes: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
