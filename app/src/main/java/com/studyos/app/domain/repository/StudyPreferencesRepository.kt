package com.studyos.app.domain.repository

import com.studyos.app.domain.model.StudyPreferences
import kotlinx.coroutines.flow.Flow

interface StudyPreferencesRepository {
    fun getPreferences(): Flow<StudyPreferences?>
    suspend fun getPreferencesOnce(): StudyPreferences?
    suspend fun savePreferences(preferences: StudyPreferences)
}
