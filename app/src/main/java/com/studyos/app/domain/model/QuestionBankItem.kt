package com.studyos.app.domain.model

enum class ExamQuestionType {
    MCQ,
    SHORT_ANSWER,
    LONG_ANSWER,
    NUMERICAL
}

enum class QuestionDifficulty {
    EASY,
    MEDIUM,
    HARD
}

data class QuestionBankItem(
    val id: String,
    val subjectId: String,
    val chapterId: String,
    val topicId: String? = null,
    val questionText: String,
    val markingScheme: String,
    val marks: Int,
    val questionType: ExamQuestionType = ExamQuestionType.SHORT_ANSWER,
    val difficulty: QuestionDifficulty = QuestionDifficulty.MEDIUM,
    val usageCount: Int = 0,
    val lastTestedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
