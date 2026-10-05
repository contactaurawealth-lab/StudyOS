package com.studyos.app.domain.repository

import com.studyos.app.domain.model.Paper
import com.studyos.app.domain.model.PaperQuestionItem
import com.studyos.app.domain.model.PaperStatus
import kotlinx.coroutines.flow.Flow

interface PaperRepository {
    fun getPapersForSubject(subjectId: String): Flow<List<Paper>>
    fun getAllPapers(): Flow<List<Paper>>
    suspend fun getPaperById(id: String): Paper?
    fun observePaperById(id: String): Flow<Paper?>
    suspend fun savePaper(paper: Paper, questions: List<PaperQuestionItem>)
    suspend fun getQuestionsForPaper(paperId: String): List<PaperQuestionItem>
    fun observeQuestionsForPaper(paperId: String): Flow<List<PaperQuestionItem>>
    suspend fun updatePaperStatus(id: String, status: PaperStatus)
    suspend fun updatePaperPdfUri(id: String, pdfUri: String)
    suspend fun deletePaper(paper: Paper)
}
