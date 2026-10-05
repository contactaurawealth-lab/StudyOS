package com.studyos.app.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.studyos.app.core.database.entity.PaperEntity
import com.studyos.app.core.database.entity.PaperQuestionCrossRefEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaperDao {

    @Query("SELECT * FROM papers WHERE subjectId = :subjectId ORDER BY createdAt DESC")
    fun getPapersForSubject(subjectId: String): Flow<List<PaperEntity>>

    @Query("SELECT * FROM papers ORDER BY createdAt DESC")
    fun getAllPapers(): Flow<List<PaperEntity>>

    @Query("SELECT * FROM papers WHERE id = :id")
    suspend fun getPaperById(id: String): PaperEntity?

    @Query("SELECT * FROM papers WHERE id = :id")
    fun observePaperById(id: String): Flow<PaperEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaper(paper: PaperEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaperQuestions(questions: List<PaperQuestionCrossRefEntity>)

    @Query("SELECT * FROM paper_questions WHERE paperId = :paperId ORDER BY questionNumber ASC")
    suspend fun getQuestionsForPaper(paperId: String): List<PaperQuestionCrossRefEntity>

    @Query("SELECT * FROM paper_questions WHERE paperId = :paperId ORDER BY questionNumber ASC")
    fun observeQuestionsForPaper(paperId: String): Flow<List<PaperQuestionCrossRefEntity>>

    @Update
    suspend fun updatePaper(paper: PaperEntity)

    @Query("UPDATE papers SET status = :status WHERE id = :id")
    suspend fun updatePaperStatus(id: String, status: String)

    @Query("UPDATE papers SET pdfUri = :pdfUri WHERE id = :id")
    suspend fun updatePaperPdfUri(id: String, pdfUri: String)

    @Delete
    suspend fun deletePaper(paper: PaperEntity)

    @Query("DELETE FROM paper_questions WHERE paperId = :paperId")
    suspend fun deletePaperQuestions(paperId: String)
}
