package com.studyos.app.data.repository

import com.studyos.app.core.database.dao.PaperDao
import com.studyos.app.core.database.entity.PaperQuestionCrossRefEntity
import com.studyos.app.core.database.entity.toDomain
import com.studyos.app.core.database.entity.toEntity
import com.studyos.app.domain.model.Paper
import com.studyos.app.domain.model.PaperQuestionItem
import com.studyos.app.domain.model.PaperStatus
import com.studyos.app.domain.repository.PaperRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OfflinePaperRepository(
    private val paperDao: PaperDao
) : PaperRepository {

    override fun getPapersForSubject(subjectId: String): Flow<List<Paper>> =
        paperDao.getPapersForSubject(subjectId).map { list -> list.map { it.toDomain() } }

    override fun getAllPapers(): Flow<List<Paper>> =
        paperDao.getAllPapers().map { list -> list.map { it.toDomain() } }

    override suspend fun getPaperById(id: String): Paper? =
        paperDao.getPaperById(id)?.toDomain()

    override fun observePaperById(id: String): Flow<Paper?> =
        paperDao.observePaperById(id).map { it?.toDomain() }

    override suspend fun savePaper(paper: Paper, questions: List<PaperQuestionItem>) {
        paperDao.insertPaper(paper.toEntity())
        paperDao.deletePaperQuestions(paper.id)
        val crossRefs = questions.map { q ->
            PaperQuestionCrossRefEntity(
                paperId = paper.id,
                questionId = q.questionId,
                sectionName = q.sectionName,
                questionNumber = q.questionNumber,
                marksAllocated = q.marksAllocated
            )
        }
        paperDao.insertPaperQuestions(crossRefs)
    }

    override suspend fun getQuestionsForPaper(paperId: String): List<PaperQuestionItem> =
        paperDao.getQuestionsForPaper(paperId).map { crossRef ->
            PaperQuestionItem(
                paperId = crossRef.paperId,
                questionId = crossRef.questionId,
                sectionName = crossRef.sectionName,
                questionNumber = crossRef.questionNumber,
                marksAllocated = crossRef.marksAllocated
            )
        }

    override fun observeQuestionsForPaper(paperId: String): Flow<List<PaperQuestionItem>> =
        paperDao.observeQuestionsForPaper(paperId).map { list ->
            list.map { crossRef ->
                PaperQuestionItem(
                    paperId = crossRef.paperId,
                    questionId = crossRef.questionId,
                    sectionName = crossRef.sectionName,
                    questionNumber = crossRef.questionNumber,
                    marksAllocated = crossRef.marksAllocated
                )
            }
        }

    override suspend fun updatePaperStatus(id: String, status: PaperStatus) {
        paperDao.updatePaperStatus(id, status.name)
    }

    override suspend fun updatePaperPdfUri(id: String, pdfUri: String) {
        paperDao.updatePaperPdfUri(id, pdfUri)
    }

    override suspend fun deletePaper(paper: Paper) {
        paperDao.deletePaper(paper.toEntity())
        paperDao.deletePaperQuestions(paper.id)
    }
}
