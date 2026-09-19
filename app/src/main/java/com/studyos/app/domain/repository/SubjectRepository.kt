package com.studyos.app.domain.repository

import com.studyos.app.domain.model.Subject
import kotlinx.coroutines.flow.Flow

interface SubjectRepository {
    fun getAllSubjects(): Flow<List<Subject>>
    suspend fun getAllSubjectsOnce(): List<Subject>
    fun getSubjectById(id: String): Flow<Subject?>
    suspend fun findByName(name: String): Subject?
    suspend fun saveSubject(subject: Subject)
    suspend fun saveSubjects(subjects: List<Subject>)
    suspend fun updateSubject(subject: Subject)
    suspend fun deleteSubject(subject: Subject)
    suspend fun deleteSubjectById(id: String)
}
