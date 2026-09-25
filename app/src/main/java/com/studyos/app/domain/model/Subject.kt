package com.studyos.app.domain.model

import java.util.UUID

data class Subject(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val isCustom: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
