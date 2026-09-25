package com.studyos.app.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.studyos.app.domain.model.Subject
import java.util.UUID

@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val isCustom: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

fun SubjectEntity.toDomainModel(): Subject = Subject(
    id = id,
    name = name,
    isCustom = isCustom,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Subject.toEntity(): SubjectEntity = SubjectEntity(
    id = id,
    name = name,
    isCustom = isCustom,
    createdAt = createdAt,
    updatedAt = updatedAt
)
