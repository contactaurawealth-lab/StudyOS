package com.studyos.app.domain.repository

import com.studyos.app.domain.model.Student
import kotlinx.coroutines.flow.Flow

interface StudentRepository {
    fun getStudent(): Flow<Student?>
    suspend fun getStudentOnce(): Student?
    suspend fun saveStudent(student: Student)
    suspend fun deleteStudent(student: Student)
}
