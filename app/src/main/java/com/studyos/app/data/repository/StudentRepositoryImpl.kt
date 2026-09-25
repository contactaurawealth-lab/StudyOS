package com.studyos.app.data.repository

import com.studyos.app.core.database.dao.StudentDao
import com.studyos.app.core.database.entity.toDomainModel
import com.studyos.app.core.database.entity.toEntity
import com.studyos.app.domain.model.Student
import com.studyos.app.domain.repository.StudentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class StudentRepositoryImpl(
    private val studentDao: StudentDao
) : StudentRepository {

    override fun getStudent(): Flow<Student?> {
        return studentDao.getStudent().map { it?.toDomainModel() }
    }

    override suspend fun getStudentOnce(): Student? {
        return studentDao.getStudentOnce()?.toDomainModel()
    }

    override suspend fun saveStudent(student: Student) {
        studentDao.insertOrUpdate(student.toEntity())
    }

    override suspend fun deleteStudent(student: Student) {
        studentDao.delete(student.toEntity())
    }
}
