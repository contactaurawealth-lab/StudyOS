package com.studyos.app.data.repository

import com.studyos.app.core.database.dao.ChapterDao
import com.studyos.app.core.database.dao.SubjectDao
import com.studyos.app.core.database.entity.toDomain
import com.studyos.app.core.database.entity.toDomainModel
import com.studyos.app.core.database.entity.toEntity
import com.studyos.app.domain.model.ChapterStatus
import com.studyos.app.domain.model.Subject
import com.studyos.app.domain.model.SubjectWithProgress
import com.studyos.app.domain.model.calculateSubjectProgress
import com.studyos.app.domain.repository.SubjectRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class SubjectRepositoryImpl(
    private val subjectDao: SubjectDao,
    private val chapterDao: ChapterDao
) : SubjectRepository {

    override fun getAllSubjects(): Flow<List<Subject>> {
        return subjectDao.getAllSubjects().map { list ->
            list.map { it.toDomainModel() }
        }
    }

    override fun observeSubjects(): Flow<List<Subject>> = getAllSubjects()

    override fun observeSubjectsWithProgress(): Flow<List<SubjectWithProgress>> {
        return combine(
            subjectDao.getAllSubjects(),
            chapterDao.observeAllChapters()
        ) { subjectEntities, chapterEntities ->
            val domainChapters = chapterEntities.map { it.toDomain() }
            val chaptersBySubject = domainChapters.groupBy { it.subjectId }

            subjectEntities.map { subjectEntity ->
                val subject = subjectEntity.toDomainModel()
                val chapters = chaptersBySubject[subject.id] ?: emptyList()
                val progress = calculateSubjectProgress(chapters)
                val completedCount = chapters.count { it.status == ChapterStatus.COMPLETED || it.progress == 100 }
                SubjectWithProgress(
                    subject = subject,
                    chapterCount = chapters.size,
                    completedChapterCount = completedCount,
                    progress = progress
                )
            }
        }
    }

    override suspend fun getAllSubjectsOnce(): List<Subject> {
        return subjectDao.getAllSubjectsOnce().map { it.toDomainModel() }
    }

    override fun getSubjectById(id: String): Flow<Subject?> {
        return subjectDao.getSubjectById(id).map { it?.toDomainModel() }
    }

    override suspend fun getSubjectByIdOnce(id: String): Subject? {
        return subjectDao.getSubject(id)?.toDomainModel()
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
        chapterDao.deleteForSubject(subject.id)
        subjectDao.delete(subject.toEntity())
    }

    override suspend fun deleteSubjectById(id: String) {
        chapterDao.deleteForSubject(id)
        subjectDao.deleteById(id)
    }
}
