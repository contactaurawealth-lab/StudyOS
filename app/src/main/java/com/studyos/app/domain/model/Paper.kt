package com.studyos.app.domain.model

enum class PaperStatus {
    GENERATED,
    IN_PROGRESS,
    COMPLETED,
    EVALUATED
}

data class PaperSection(
    val name: String,
    val instructions: String,
    val totalQuestions: Int,
    val totalMarks: Int
)

data class Paper(
    val id: String,
    val subjectId: String,
    val title: String,
    val totalMarks: Int,
    val durationMinutes: Int,
    val sectionsJson: String = "[]",
    val pdfUri: String? = null,
    val status: PaperStatus = PaperStatus.GENERATED,
    val createdAt: Long = System.currentTimeMillis()
)

data class PaperQuestionItem(
    val paperId: String,
    val questionId: String,
    val sectionName: String,
    val questionNumber: Int,
    val marksAllocated: Int,
    val question: QuestionBankItem? = null
)
