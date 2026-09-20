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
        val resolvedTitle = when {
            !title.isNullOrBlank() -> title.trim()
            chapterId != null -> {
                val chapter = chapterRepository.getChapterById(chapterId)
                val subject = subjectId?.let { subjectRepository.getSubjectByIdOnce(it) }
                if (subject != null && chapter != null) {
                    "${subject.name} • ${chapter.name}"
                } else {
                    chapter?.name ?: "Chapter Study"
                }
            }
            subjectId != null -> {
                val subject = subjectRepository.getSubjectByIdOnce(subjectId)
                subject?.name?.let { "$it Study" } ?: "Subject Study"
            }
            else -> "New Conversation"
        }

        val conversation = AiConversation(
            id = UUID.randomUUID().toString(),
            title = resolvedTitle,
            subjectId = subjectId,
            chapterId = chapterId,
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
