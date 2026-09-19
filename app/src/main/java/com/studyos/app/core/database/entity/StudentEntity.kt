package com.studyos.app.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.studyos.app.domain.model.Student
import java.util.UUID

@Entity(tableName = "students")
data class StudentEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val classLevel: String?,
    val division: String?,
    val schoolName: String?,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

fun StudentEntity.toDomainModel(): Student = Student(
    id = id,
    name = name,
    classLevel = classLevel,
    division = division,
    schoolName = schoolName,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Student.toEntity(): StudentEntity = StudentEntity(
    id = id,
    name = name,
    classLevel = classLevel,
    division = division,
    schoolName = schoolName,
    createdAt = createdAt,
    updatedAt = updatedAt
)
