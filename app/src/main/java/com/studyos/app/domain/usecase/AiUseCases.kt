package com.studyos.app.domain.usecase

import com.studyos.app.core.datastore.PreferencesDataSource
import com.studyos.app.domain.model.AiConfig
import com.studyos.app.domain.model.AiConversation
import com.studyos.app.domain.model.AiConversationItem
import com.studyos.app.domain.model.AiMessage
import com.studyos.app.domain.model.AiMessageRole
import com.studyos.app.domain.model.AiMessageStatus
import com.studyos.app.domain.model.AiStreamChunk
import com.studyos.app.domain.model.StudyContext
import com.studyos.app.domain.provider.AiProvider
import com.studyos.app.domain.repository.AiConversationRepository
import com.studyos.app.domain.repository.AiMessageRepository
import com.studyos.app.domain.repository.ChapterRepository
import com.studyos.app.domain.repository.SubjectRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import java.util.UUID

class GetConversationsUseCase(
    private val conversationRepository: AiConversationRepository,
    private val subjectRepository: SubjectRepository,
    private val chapterRepository: ChapterRepository,
    private val messageRepository: AiMessageRepository
) {
    operator fun invoke(): Flow<List<AiConversationItem>> {
        return combine(
            conversationRepository.observeConversations(),
            subjectRepository.getAllSubjects(),
            chapterRepository.observeAllChapters()
        ) { conversations, subjects, chapters ->
            val subjectMap = subjects.associateBy { it.id }
            val chapterMap = chapters.associateBy { it.id }

            conversations.map { conv ->
                val lastMsg = messageRepository.getMessagesForConversationOnce(conv.id).lastOrNull()
                val count = messageRepository.getMessagesForConversationOnce(conv.id).size
                AiConversationItem(
                    conversation = conv,
                    lastMessagePreview = lastMsg?.content?.take(80),
                    messageCount = count,
                    subjectName = conv.subjectId?.let { subjectMap[it]?.name },
                    chapterName = conv.chapterId?.let { chapterMap[it]?.name }
                )
            }
        }
    }
}

class GetConversationUseCase(
    private val conversationRepository: AiConversationRepository
) {
    operator fun invoke(id: String): Flow<AiConversation?> =
        conversationRepository.getConversationById(id)

    suspend fun getOnce(id: String): AiConversation? =
        conversationRepository.getConversationByIdOnce(id)
}

class GetMessagesUseCase(
    private val messageRepository: AiMessageRepository
) {
    operator fun invoke(conversationId: String): Flow<List<AiMessage>> =
        messageRepository.observeMessagesForConversation(conversationId)
}

class CreateConversationUseCase(
    private val conversationRepository: AiConversationRepository,
    private val subjectRepository: SubjectRepository,
    private val chapterRepository: ChapterRepository
) {
    suspend operator fun invoke(
        title: String? = null,
        subjectId: String? = null,
        chapterId: String? = null
    ): AiConversation {
        val cleanSubjectId = subjectId?.trim()?.takeIf { it.isNotBlank() && !it.startsWith("{") }
        val cleanChapterId = chapterId?.trim()?.takeIf { it.isNotBlank() && !it.startsWith("{") }

        val validSubject = cleanSubjectId?.let { subjectRepository.getSubjectByIdOnce(it) }
        val validChapter = cleanChapterId?.let { chapterRepository.getChapterById(it) }

        val verifiedSubjectId = validSubject?.id
        val verifiedChapterId = validChapter?.id

        val resolvedTitle = when {
            !title.isNullOrBlank() -> title.trim()
            validChapter != null -> {
                if (validSubject != null) {
                    "${validSubject.name} • ${validChapter.name}"
                } else {
                    validChapter.name
                }
            }
            validSubject != null -> "${validSubject.name} Study"
            else -> "New Conversation"
        }

        val conversation = AiConversation(
            id = UUID.randomUUID().toString(),
            title = resolvedTitle,
            subjectId = verifiedSubjectId,
            chapterId = verifiedChapterId,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        return conversationRepository.createConversation(conversation)
    }
}

class DeleteConversationUseCase(
    private val conversationRepository: AiConversationRepository,
    private val messageRepository: AiMessageRepository
) {
    suspend operator fun invoke(conversationId: String) {
        messageRepository.deleteMessagesForConversation(conversationId)
        conversationRepository.deleteConversation(conversationId)
    }
}

class GetStudyContextUseCase(
    private val subjectRepository: SubjectRepository,
    private val chapterRepository: ChapterRepository
) {
    suspend operator fun invoke(subjectId: String?, chapterId: String?): StudyContext {
        val subject = subjectId?.let { subjectRepository.getSubjectByIdOnce(it) }
        val chapter = chapterId?.let { chapterRepository.getChapterById(it) }

        return StudyContext(
            subjectId = subjectId,
            chapterId = chapterId,
            subjectName = subject?.name,
            chapterName = chapter?.name,
            chapterProgress = chapter?.progress,
            weakTopics = emptyList(),
            recentActivity = null,
            upcomingExams = emptyList()
        )
    }
}

class SendAiMessageUseCase(
    private val messageRepository: AiMessageRepository,
    private val conversationRepository: AiConversationRepository,
    private val aiProvider: AiProvider,
    private val preferencesDataSource: PreferencesDataSource
) {
    suspend fun saveUserMessage(
        conversationId: String,
        content: String
    ): AiMessage {
        val userMessage = AiMessage(
            id = UUID.randomUUID().toString(),
            conversationId = conversationId,
            role = AiMessageRole.USER,
            content = content.trim(),
            status = AiMessageStatus.SENT,
            createdAt = System.currentTimeMillis()
        )
        messageRepository.createMessage(userMessage)

        // Update conversation title if this is the first user message and title was default
        val conversation = conversationRepository.getConversationByIdOnce(conversationId)
        if (conversation != null) {
            val messages = messageRepository.getMessagesForConversationOnce(conversationId)
            val updatedTitle = if (conversation.title == "New Conversation" || conversation.title.isBlank()) {
                val preview = content.trim().take(40)
                if (content.length > 40) "$preview..." else preview
            } else {
                conversation.title
            }
            conversationRepository.updateConversation(
                conversation.copy(
                    title = updatedTitle,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }

        return userMessage
    }

    suspend fun createAssistantPlaceholder(conversationId: String): AiMessage {
        val placeholder = AiMessage(
            id = UUID.randomUUID().toString(),
            conversationId = conversationId,
            role = AiMessageRole.ASSISTANT,
            content = "",
            status = AiMessageStatus.STREAMING,
            createdAt = System.currentTimeMillis()
        )
        messageRepository.createMessage(placeholder)
        return placeholder
    }

    fun streamAssistantResponse(
        assistantMessageId: String,
        conversationId: String,
        context: StudyContext?
    ): Flow<String> = flow {
        val config = preferencesDataSource.aiConfig.firstOrNull() ?: AiConfig()
        val allMessages = messageRepository.getMessagesForConversationOnce(conversationId)
            .filter { it.id != assistantMessageId }

        val contentBuffer = StringBuilder()
        var hasError = false
        var errorDesc: String? = null

        try {
            aiProvider.generateStream(allMessages, context, config).collect { chunk ->
                when (chunk) {
                    is AiStreamChunk.Content -> {
                        contentBuffer.append(chunk.delta)
                        emit(contentBuffer.toString())
                    }
                    is AiStreamChunk.Error -> {
                        hasError = true
                        errorDesc = chunk.message
                    }
                    is AiStreamChunk.Done -> {
                        // Stream finished
                    }
                }
            }
        } catch (e: Exception) {
            hasError = true
            errorDesc = e.message ?: "Streaming interrupted."
        } finally {
            val finalContent = contentBuffer.toString()
            val finalStatus = if (hasError) {
                AiMessageStatus.ERROR
            } else {
                AiMessageStatus.SUCCESS
            }
            val existing = messageRepository.getMessageByIdOnce(assistantMessageId)
            if (existing != null) {
                messageRepository.updateMessage(
                    existing.copy(
                        content = finalContent,
                        status = finalStatus,
                        errorMessage = errorDesc
                    )
                )
            }
        }
    }

    private fun generateLocalOfflineResponse(
        lastUserMessage: String?,
        context: StudyContext?
    ): String {
        val query = lastUserMessage?.lowercase() ?: ""
        return when {
            query.contains("what should i study") || query.contains("what to study") || query.contains("recommend") || query.contains("next topic") -> {
                val subjectName = context?.subjectName ?: "Core Subject"
                val chapterName = context?.chapterName ?: "Key Chapter"
                val progress = context?.chapterProgress ?: 0
                """### 🎯 Recommended Study Focus
Based on your StudyOS learning intelligence:
- **Target Subject:** $subjectName
- **Priority Chapter:** $chapterName
- **Current Progress:** $progress%

**Action Plan:**
1. **Learn:** Spend 15 minutes reviewing key theorems and definitions.
2. **Recall:** Perform a quick 5-item active recall session to test retrieval.
3. **Practice:** Solve 3-5 rapid problems and capture any errors in the Mistake Bank.
"""
            }
            query.contains("weak") || query.contains("struggl") -> {
                val weak = context?.weakTopics ?: emptyList()
                """### 🔍 Weak Areas & Knowledge Gaps
Based on your practice quizzes and active recall stability:
${if (weak.isNotEmpty()) {
    weak.joinToString("\n") { "- **$it**: Prioritize concept reconstruction and retrieval." }
} else {
    "- Topics with Active Recall retention under 60% are flagged in your **Smart Revision Queue**."
}}

**Recommended Intervention:**
- Review the core formulas from first principles.
- Use the **Mistake Bank** to retry past missed questions.
"""
            }
            query.contains("plan") || query.contains("30-minute") || query.contains("schedule") -> {
                val topic = context?.chapterName ?: context?.subjectName ?: "Focused Subject"
                """### ⏱️ Focused 30-Minute Study Plan
Here is your structured, high-yield study cycle for **$topic**:

1. **Minutes 0–15:** Concept Learning & Deep Reading
2. **Minutes 15–22:** Active Recall Retrieval (test memory without notes)
3. **Minutes 22–27:** Rapid Practice Quiz & Mistake Logging
4. **Minutes 27–30:** Reflection & Daily Intelligence Update

*Tip: Use the **Study Timer** in the sidebar to run this session with zero distractions!*
"""
            }
            query.contains("exam") || query.contains("readiness") -> {
                """### 📊 Exam Readiness Intelligence
Your readiness score is evaluated using 7 key dimensions:
- **Syllabus Coverage:** Progression through chapters
- **Understanding Score:** Concept mastery
- **Active Recall Stability:** Spaced repetition retention
- **Practice Accuracy:** Quiz scores
- **Mistake Clearance:** Unreviewed error reduction
- **Freshness:** Knowledge decay since last study session
- **Exam Proximity:** Days remaining countdown

Head to the **Exams** tab in the sidebar to view your full countdown dashboard and high-yield focus topics!
"""
            }
            else -> {
                val topic = context?.chapterName ?: context?.subjectName ?: "this concept"
                """### 💡 Academic Tutor (Local Intelligence)
*Offline local intelligence active. Configure an API key in AI Settings to enable cloud models.*

**Key Principles for Mastering $topic:**
- **First Principles:** Break the concept down into its fundamental definitions and axioms.
- **Active Retrieval:** Do not passively re-read. Formulate self-test questions and explain the concept aloud.
- **Error Analysis:** When a problem is missed, identify whether it was a *Concept Gap*, *Calculation Error*, or *Memory Gap* in the **Mistake Bank**.
"""
            }
        }
    }

    suspend fun cancelStreaming(assistantMessageId: String) {
        val existing = messageRepository.getMessageByIdOnce(assistantMessageId)
        if (existing != null && existing.status == AiMessageStatus.STREAMING) {
            val content = existing.content.ifBlank { "Generation stopped." }
            messageRepository.updateMessage(
                existing.copy(
                    content = content,
                    status = AiMessageStatus.SUCCESS
                )
            )
        }
    }
}

class SaveAiConfigUseCase(
    private val preferencesDataSource: PreferencesDataSource
) {
    suspend operator fun invoke(config: AiConfig) {
        preferencesDataSource.saveAiConfig(config)
    }
}

class GetAiConfigUseCase(
    private val preferencesDataSource: PreferencesDataSource
) {
    operator fun invoke(): Flow<AiConfig> = preferencesDataSource.aiConfig
}
