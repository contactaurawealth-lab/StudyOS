package com.studyos.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.studyos.app.core.database.entity.FlashcardEntity
import com.studyos.app.core.database.entity.FlashcardReviewEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FlashcardDao {
    @Query("SELECT * FROM flashcards WHERE chapterId = :chapterId ORDER BY nextReview ASC, createdAt ASC")
    fun observeFlashcardsForChapter(chapterId: String): Flow<List<FlashcardEntity>>

    @Query("SELECT * FROM flashcards WHERE subjectId = :subjectId ORDER BY nextReview ASC, createdAt ASC")
    fun observeFlashcardsForSubject(subjectId: String): Flow<List<FlashcardEntity>>

    @Query("SELECT * FROM flashcards WHERE nextReview IS NOT NULL AND nextReview <= :currentTime ORDER BY nextReview ASC")
    fun observeDueFlashcards(currentTime: Long): Flow<List<FlashcardEntity>>

    @Query("SELECT * FROM flashcards ORDER BY createdAt DESC")
    fun observeAllFlashcards(): Flow<List<FlashcardEntity>>

    @Query("SELECT * FROM flashcards WHERE id = :id LIMIT 1")
    fun observeFlashcardById(id: String): Flow<FlashcardEntity?>

    @Query("SELECT * FROM flashcards WHERE id = :id LIMIT 1")
    suspend fun getFlashcardByIdOnce(id: String): FlashcardEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(flashcard: FlashcardEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(flashcards: List<FlashcardEntity>)

    @Update
    suspend fun update(flashcard: FlashcardEntity)

    @Query("DELETE FROM flashcards WHERE id = :id")
    suspend fun deleteById(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: FlashcardReviewEntity)

    @Query("SELECT * FROM flashcard_reviews WHERE flashcardId = :cardId ORDER BY reviewedAt DESC")
    fun observeReviewsForCard(cardId: String): Flow<List<FlashcardReviewEntity>>

    @Query("SELECT * FROM flashcards WHERE question LIKE '%' || :query || '%' OR answer LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchFlashcards(query: String): Flow<List<FlashcardEntity>>
}
