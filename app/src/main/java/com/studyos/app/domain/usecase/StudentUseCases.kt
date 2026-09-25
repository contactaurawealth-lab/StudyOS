package com.studyos.app.domain.usecase

import com.studyos.app.domain.model.Student
import com.studyos.app.domain.repository.StudentRepository
import kotlinx.coroutines.flow.Flow

class GetStudentUseCase(
    private val studentRepository: StudentRepository
) {
    operator fun invoke(): Flow<Student?> = studentRepository.getStudent()
    suspend fun getOnce(): Student? = studentRepository.getStudentOnce()
}

class SaveStudentUseCase(
    private val studentRepository: StudentRepository
) {
    suspend operator fun invoke(student: Student) {
        studentRepository.saveStudent(student)
    }
}
