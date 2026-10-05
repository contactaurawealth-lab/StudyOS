package com.studyos.app.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.studyos.app.core.database.entity.LostMarksEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LostMarksDao {

    @Query("SELECT * FROM lost_marks WHERE resultId = :resultId")
    fun getLostMarksForResult(resultId: String): Flow<List<LostMarksEntity>>

    @Query("SELECT * FROM lost_marks WHERE resultId = :resultId")
    suspend fun getLostMarksForResultOnce(resultId: String): List<LostMarksEntity>

    @Query("SELECT * FROM lost_marks WHERE topicId = :topicId")
    fun getLostMarksForTopic(topicId: String): Flow<List<LostMarksEntity>>

    @Query("SELECT * FROM lost_marks WHERE category = :category")
    fun getLostMarksByCategory(category: String): Flow<List<LostMarksEntity>>

    @Query("SELECT * FROM lost_marks ORDER BY createdAt DESC")
    fun getAllLostMarks(): Flow<List<LostMarksEntity>>

    @Query("SELECT category, SUM(marksLost) as totalLost FROM lost_marks GROUP BY category")
    fun getLostMarksCategoryBreakdown(): Flow<List<CategoryLostMarksTuple>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLostMark(lostMark: LostMarksEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLostMarks(lostMarks: List<LostMarksEntity>)

    @Update
    suspend fun updateLostMark(lostMark: LostMarksEntity)

    @Query("UPDATE lost_marks SET isRemediated = :remediated WHERE id = :id")
    suspend fun updateRemediation(id: String, remediated: Boolean)

    @Delete
    suspend fun deleteLostMark(lostMark: LostMarksEntity)
}

data class CategoryLostMarksTuple(
    val category: String,
    val totalLost: Double
)
