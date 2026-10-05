package com.studyos.app.domain.model

data class ExamResult(
    val id: String,
    val paperId: String,
    val marksObtained: Double,
    val totalMarks: Double,
    val percentage: Double,
    val timeTakenMinutes: Int,
    val notesOrFeedback: String? = null,
    val examDate: Long = System.currentTimeMillis()
)
