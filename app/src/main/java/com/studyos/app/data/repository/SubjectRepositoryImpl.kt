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
import com.studyos.app.core.database.dao.ExamDao
import kotlinx.coroutines.flow.map

class SubjectRepositoryImpl(
    private val subjectDao: SubjectDao,
    private val chapterDao: ChapterDao,
    private val examDao: ExamDao? = null
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
                val inProgressCount = chapters.count { (it.status == ChapterStatus.IN_PROGRESS || it.progress in 1..99) && it.status != ChapterStatus.COMPLETED && it.progress != 100 }
                val notStartedCount = (chapters.size - completedCount - inProgressCount).coerceAtLeast(0)
                val currentChapter = chapters.find { it.status == ChapterStatus.IN_PROGRESS }
                    ?: chapters.firstOrNull { it.progress < 100 }
                    ?: chapters.firstOrNull()

                val strongCount = chapters.count { it.progress >= 75 || it.status == ChapterStatus.COMPLETED }
                val weakCount = chapters.count { it.progress in 1..49 }
                val now = System.currentTimeMillis()
                val dueCount = chapters.count { it.status != ChapterStatus.COMPLETED && (now - (it.lastOpenedAt ?: 0L) > 3 * 86400000L) }
                val weakChapter = chapters.filter { it.progress in 1..49 || it.status == ChapterStatus.IN_PROGRESS }.minByOrNull { it.progress }

                SubjectWithProgress(
                    subject = subject,
                    chapterCount = chapters.size,
                    completedChapterCount = completedCount,
                    inProgressChapterCount = inProgressCount,
                    notStartedChapterCount = notStartedCount,
                    progress = progress,
                    currentChapterName = currentChapter?.name,
                    strongCount = strongCount,
                    weakCount = weakCount,
                    dueCount = dueCount,
                    readinessScore = progress,
                    weakChapterId = weakChapter?.id ?: currentChapter?.id,
                    weakChapterName = weakChapter?.name ?: currentChapter?.name,
                    understandingPercentage = (progress * 0.85).toInt().coerceIn(0, 100),
                    recallPercentage = (progress * 0.70).toInt().coerceIn(0, 100),
                    practicePercentage = (progress * 0.75).toInt().coerceIn(0, 100),
                    unresolvedMistakesCount = 0,
                    insight = if (weakCount > 0) "$weakCount chapters require concept reinforcement." else "Syllabus on track."
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
        deleteSubjectById(subject.id)
    }

    override suspend fun deleteSubjectById(id: String) {
        examDao?.deleteExamSubjectsBySubject(id)
        chapterDao.deleteForSubject(id)
        subjectDao.deleteById(id)
    }
}
