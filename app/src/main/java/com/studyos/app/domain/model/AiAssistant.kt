package com.studyos.app.domain.model

import java.util.UUID

enum class AiMessageRole {
    USER,
    ASSISTANT,
    SYSTEM
}

enum class AiMessageStatus {
    SENT,
    STREAMING,
    SUCCESS,
    ERROR
}

data class AiMessage(
    val id: String = UUID.randomUUID().toString(),
    val conversationId: String = "temp",
    val role: AiMessageRole,
    val content: String,
    val status: AiMessageStatus = AiMessageStatus.SUCCESS,
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class AiConversation(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "New Conversation",
    val subjectId: String? = null,
    val chapterId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class AiConversationItem(
    val conversation: AiConversation,
    val lastMessagePreview: String? = null,
    val messageCount: Int = 0,
    val subjectName: String? = null,
    val chapterName: String? = null
)

data class StudyContext(
    val subjectId: String? = null,
    val chapterId: String? = null,
    val subjectName: String? = null,
    val chapterName: String? = null,
    val chapterProgress: Int? = null,
    val weakTopics: List<String> = emptyList(),
    val recentActivity: String? = null,
    val upcomingExams: List<String> = emptyList()
) {
    val hasContext: Boolean
        get() = !subjectName.isNullOrBlank() || !chapterName.isNullOrBlank() || weakTopics.isNotEmpty()

    fun toPromptContext(): String {
        val parts = mutableListOf<String>()
        if (!subjectName.isNullOrBlank()) {
            parts.add("Subject: $subjectName")
        }
        if (!chapterName.isNullOrBlank()) {
            val progressStr = if (chapterProgress != null) " (Progress: $chapterProgress%)" else ""
            parts.add("Chapter: $chapterName$progressStr")
        }
        if (weakTopics.isNotEmpty()) {
            parts.add("Student's weak areas in this topic: ${weakTopics.joinToString(", ")}")
        }
        if (!recentActivity.isNullOrBlank()) {
            parts.add("Recent study activity: $recentActivity")
        }
        if (upcomingExams.isNotEmpty()) {
            parts.add("Upcoming exams: ${upcomingExams.joinToString(", ")}")
        }
        return if (parts.isNotEmpty()) {
            "STUDY CONTEXT:\n" + parts.joinToString("\n")
        } else {
            ""
        }
    }
}

enum class QuickAction(val label: String, val promptPrefix: String) {
    EXPLAIN("Explain this", "Please explain the following concept clearly with an intuitive breakdown:"),
    SIMPLIFY("Simplify", "Please simplify this concept in plain, simple terms as if explaining to a beginner:"),
    GIVE_EXAMPLE("Give example", "Please provide 2-3 practical, concrete examples to illustrate:"),
    QUIZ_ME("Quiz me", "Please create a 3-question conceptual quiz (with answers and explanations at the end) on:"),
    MAKE_NOTES("Make notes", "Please create structured, concise study notes (summary, key definitions, important formulas/points) for:"),
    FLASHCARDS("Create flashcards", "Please create 5 high-yield Question / Answer flashcards for revision on:"),
    WEAK_AREAS("Find my weak areas", "Based on our study context, what are the most common misconceptions and weak areas students encounter in:")
}

data class AiConfig(
    val apiKey: String = "",
    val baseUrl: String = "https://api.openai.com/v1/",
    val model: String = "gpt-4o-mini",
    val customSystemPrompt: String? = null
) {
    val isConfigured: Boolean get() = apiKey.isNotBlank()
}

enum class AiErrorType {
    NO_INTERNET,
    API_KEY_MISSING,
    RATE_LIMIT,
    TIMEOUT,
    SERVER_ERROR,
    EMPTY_RESPONSE,
    UNKNOWN
}

sealed class AiStreamChunk {
    data class Content(val delta: String) : AiStreamChunk()
    data class Error(val errorType: AiErrorType, val message: String) : AiStreamChunk()
    object Done : AiStreamChunk()
}
