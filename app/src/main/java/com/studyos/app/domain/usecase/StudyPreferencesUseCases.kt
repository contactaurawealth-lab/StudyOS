package com.studyos.app.domain.usecase

import com.studyos.app.domain.model.StudyPreferences
import com.studyos.app.domain.repository.StudyPreferencesRepository
import kotlinx.coroutines.flow.Flow

class GetStudyPreferencesUseCase(
    private val studyPreferencesRepository: StudyPreferencesRepository
) {
    operator fun invoke(): Flow<StudyPreferences?> = studyPreferencesRepository.getPreferences()
    suspend fun getOnce(): StudyPreferences? = studyPreferencesRepository.getPreferencesOnce()
}

class SaveStudyPreferencesUseCase(
    private val studyPreferencesRepository: StudyPreferencesRepository
) {
    suspend operator fun invoke(preferences: StudyPreferences) {
        studyPreferencesRepository.savePreferences(preferences)
    }
}
