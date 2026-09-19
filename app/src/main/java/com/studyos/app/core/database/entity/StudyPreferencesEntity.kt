package com.studyos.app.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.studyos.app.domain.model.StudyPreferences
import java.util.UUID

@Entity(tableName = "study_preferences")
data class StudyPreferencesEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val dailyStudyGoalMinutes: Int = 60,
    val defaultSessionMinutes: Int = 45,
    val preferredStartMinutes: Int? = null,
    val preferredEndMinutes: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

fun StudyPreferencesEntity.toDomainModel(): StudyPreferences = StudyPreferences(
    id = id,
    dailyStudyGoalMinutes = dailyStudyGoalMinutes,
    defaultSessionMinutes = defaultSessionMinutes,
    preferredStartMinutes = preferredStartMinutes,
    preferredEndMinutes = preferredEndMinutes,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun StudyPreferences.toEntity(): StudyPreferencesEntity = StudyPreferencesEntity(
    id = id,
    dailyStudyGoalMinutes = dailyStudyGoalMinutes,
    defaultSessionMinutes = defaultSessionMinutes,
    preferredStartMinutes = preferredStartMinutes,
    preferredEndMinutes = preferredEndMinutes,
    createdAt = createdAt,
    updatedAt = updatedAt
)
