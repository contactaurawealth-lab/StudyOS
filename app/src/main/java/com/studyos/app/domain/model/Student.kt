package com.studyos.app.domain.model

import java.util.UUID

data class Student(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val classLevel: String? = null,
    val division: String? = null,
    val schoolName: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
