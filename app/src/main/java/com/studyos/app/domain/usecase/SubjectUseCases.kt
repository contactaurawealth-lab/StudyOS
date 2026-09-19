package com.studyos.app.domain.usecase

import com.studyos.app.domain.model.Subject
import com.studyos.app.domain.repository.SubjectRepository
import kotlinx.coroutines.flow.Flow

class GetSubjectsUseCase(
    private val subjectRepository: SubjectRepository
) {
    operator fun invoke(): Flow<List<Subject>> = subjectRepository.getAllSubjects()
    suspend fun getOnce(): List<Subject> = subjectRepository.getAllSubjectsOnce()
}

class SaveSubjectsUseCase(
    private val subjectRepository: SubjectRepository
) {
    suspend operator fun invoke(subjects: List<Subject>) {
        subjectRepository.saveSubjects(subjects)
    }
}

sealed class AddSubjectResult {
    data class Success(val subject: Subject) : AddSubjectResult()
    object EmptyName : AddSubjectResult()
    object DuplicateName : AddSubjectResult()
    data class Error(val message: String) : AddSubjectResult()
}

class AddSubjectUseCase(
    private val subjectRepository: SubjectRepository
) {
    suspend operator fun invoke(name: String, isCustom: Boolean = true): AddSubjectResult {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            return AddSubjectResult.EmptyName
        }

        val existing = subjectRepository.findByName(trimmedName)
        if (existing != null) {
            return AddSubjectResult.DuplicateName
        }

        val newSubject = Subject(
            name = trimmedName,
            isCustom = isCustom
        )
        return try {
            subjectRepository.saveSubject(newSubject)
            AddSubjectResult.Success(newSubject)
        } catch (e: Exception) {
            AddSubjectResult.Error(e.message ?: "Could not save subject")
        }
    }
}

class RenameSubjectUseCase(
    private val subjectRepository: SubjectRepository
) {
    suspend operator fun invoke(subjectId: String, newName: String): AddSubjectResult {
        val trimmedName = newName.trim()
        if (trimmedName.isEmpty()) {
            return AddSubjectResult.EmptyName
        }

        val existing = subjectRepository.findByName(trimmedName)
        if (existing != null && existing.id != subjectId) {
            return AddSubjectResult.DuplicateName
        }

        val subject = subjectRepository.getAllSubjectsOnce().find { it.id == subjectId }
            ?: return AddSubjectResult.Error("Subject not found")

        val updated = subject.copy(name = trimmedName, updatedAt = System.currentTimeMillis())
        return try {
            subjectRepository.updateSubject(updated)
            AddSubjectResult.Success(updated)
        } catch (e: Exception) {
            AddSubjectResult.Error(e.message ?: "Could not update subject")
        }
    }
}

class DeleteSubjectUseCase(
    private val subjectRepository: SubjectRepository
) {
    suspend operator fun invoke(subjectId: String) {
        subjectRepository.deleteSubjectById(subjectId)
    }
}
