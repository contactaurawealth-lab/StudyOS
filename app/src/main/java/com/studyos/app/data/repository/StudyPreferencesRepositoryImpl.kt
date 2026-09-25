package com.studyos.app.data.repository

import com.studyos.app.core.database.dao.StudyPreferencesDao
import com.studyos.app.core.database.entity.toDomainModel
import com.studyos.app.core.database.entity.toEntity
import com.studyos.app.domain.model.StudyPreferences
import com.studyos.app.domain.repository.StudyPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class StudyPreferencesRepositoryImpl(
    private val studyPreferencesDao: StudyPreferencesDao
) : StudyPreferencesRepository {

    override fun getPreferences(): Flow<StudyPreferences?> {
        return studyPreferencesDao.getPreferences().map { it?.toDomainModel() }
    }

    override suspend fun getPreferencesOnce(): StudyPreferences? {
        return studyPreferencesDao.getPreferencesOnce()?.toDomainModel()
    }

    override suspend fun savePreferences(preferences: StudyPreferences) {
        studyPreferencesDao.insertOrUpdate(preferences.toEntity())
    }
}
