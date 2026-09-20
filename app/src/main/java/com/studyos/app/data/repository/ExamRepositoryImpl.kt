package com.studyos.app.data.repository

import com.studyos.app.core.database.dao.ExamDao
import com.studyos.app.core.database.entity.ExamEntity
import com.studyos.app.core.database.entity.ExamSubjectCrossRefEntity
import com.studyos.app.domain.model.Exam
import com.studyos.app.domain.repository.ExamRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ExamRepositoryImpl(
    private val examDao: ExamDao
) : ExamRepository {

    override fun observeAllExams(): Flow<List<Exam>> =
        examDao.observeAllExams().map { entities ->
            entities.map { entity ->
                val subjectIds = examDao.getSubjectIdsForExam(entity.id)
                entity.toDomain(subjectIds)
            }
        }

    override fun getExamById(id: String): Flow<Exam?> =
        examDao.observeExamById(id).map { entity ->
            entity?.let {
                val subjectIds = examDao.getSubjectIdsForExam(it.id)
                it.toDomain(subjectIds)
            }
        }

    override suspend fun getExamByIdOnce(id: String): Exam? {
        val entity = examDao.getExamByIdOnce(id) ?: return null
        val subjectIds = examDao.getSubjectIdsForExam(entity.id)
        return entity.toDomain(subjectIds)
    }

    override suspend fun saveExam(exam: Exam, subjectIds: List<String>): Exam {
        examDao.insertExam(exam.toEntity())
        examDao.deleteExamSubjects(exam.id)
        val crossRefs = subjectIds.map { ExamSubjectCrossRefEntity(exam.id, it) }
        examDao.insertExamSubjects(crossRefs)
        return exam.copy(subjectIds = subjectIds)
    }

    override suspend fun deleteExam(id: String) {
        examDao.deleteExamById(id)
    }

    private fun ExamEntity.toDomain(subjectIds: List<String>): Exam = Exam(
        id = id,
        name = name,
        targetDate = date,
        targetScore = targetScore,
        notes = notes,
        subjectIds = subjectIds,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun Exam.toEntity(): ExamEntity = ExamEntity(
        id = id,
        name = name,
        date = targetDate,
        targetScore = targetScore,
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
