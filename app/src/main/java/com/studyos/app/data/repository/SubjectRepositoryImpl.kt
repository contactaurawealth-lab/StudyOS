package com.studyos.app.data.repository

import com.studyos.app.core.database.dao.SubjectDao
import com.studyos.app.core.database.entity.toDomainModel
import com.studyos.app.core.database.entity.toEntity
import com.studyos.app.domain.model.Subject
import com.studyos.app.domain.repository.SubjectRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SubjectRepositoryImpl(
    private val subjectDao: SubjectDao
) : SubjectRepository {

    override fun getAllSubjects(): Flow<List<Subject>> {
        return subjectDao.getAllSubjects().map { list ->
            list.map { it.toDomainModel() }
        }
    }

    override suspend fun getAllSubjectsOnce(): List<Subject> {
        return subjectDao.getAllSubjectsOnce().map { it.toDomainModel() }
    }

    override fun getSubjectById(id: String): Flow<Subject?> {
        return subjectDao.getSubjectById(id).map { it?.toDomainModel() }
    }

    override suspend fun findByName(name: String): Subject? {
        return subjectDao.findByName(name.trim())?.toDomainModel()
    }

    override suspend fun saveSubject(subject: Subject) {
        subjectDao.insert(subject.toEntity())
    }

    override suspend fun saveSubjects(subjects: List<Subject>) {
        subjectDao.insertAll(subjects.map { it.toEntity() })
    }

    override suspend fun updateSubject(subject: Subject) {
        subjectDao.update(subject.toEntity())
    }

    override suspend fun deleteSubject(subject: Subject) {
        subjectDao.delete(subject.toEntity())
    }

    override suspend fun deleteSubjectById(id: String) {
        subjectDao.deleteById(id)
    }
}
