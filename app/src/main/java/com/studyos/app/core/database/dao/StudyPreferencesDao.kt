package com.studyos.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.studyos.app.core.database.entity.StudyPreferencesEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyPreferencesDao {

    @Query("SELECT * FROM study_preferences LIMIT 1")
    fun getPreferences(): Flow<StudyPreferencesEntity?>

    @Query("SELECT * FROM study_preferences LIMIT 1")
    suspend fun getPreferencesOnce(): StudyPreferencesEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(preferences: StudyPreferencesEntity)

    @Update
    suspend fun update(preferences: StudyPreferencesEntity)

    @Query("DELETE FROM study_preferences")
    suspend fun deleteAll()
}
