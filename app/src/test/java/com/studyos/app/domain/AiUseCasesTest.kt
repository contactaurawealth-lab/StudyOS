package com.studyos.app.domain

import com.studyos.app.core.datastore.PreferencesDataSource
import com.studyos.app.core.model.AppState
import com.studyos.app.core.model.AppTheme
import com.studyos.app.domain.model.AiConfig
import com.studyos.app.domain.model.AiConversation
import com.studyos.app.domain.model.AiErrorType
import com.studyos.app.domain.model.AiMessage
import com.studyos.app.domain.model.AiMessageRole
import com.studyos.app.domain.model.AiMessageStatus
import com.studyos.app.domain.model.AiStreamChunk
import com.studyos.app.domain.model.Chapter
import com.studyos.app.domain.model.ChapterStatus
import com.studyos.app.domain.model.StudyContext
import com.studyos.app.domain.model.Subject
import com.studyos.app.domain.provider.AiProvider
import com.studyos.app.domain.repository.AiConversationRepository
import com.studyos.app.domain.repository.AiMessageRepository
import com.studyos.app.domain.usecase.CreateConversationUseCase
import com.studyos.app.domain.usecase.DeleteConversationUseCase
import com.studyos.app.domain.usecase.GetConversationUseCase
import com.studyos.app.domain.usecase.GetConversationsUseCase
import com.studyos.app.domain.usecase.GetMessagesUseCase
import com.studyos.app.domain.usecase.GetStudyContextUseCase
import com.studyos.app.domain.usecase.SendAiMessageUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeAiConversationRepository : AiConversationRepository {
    private val conversationsFlow = MutableStateFlow<List<AiConversation>>(emptyList())
    val items = mutableListOf<AiConversation>()

    private fun sync() {
        conversationsFlow.value = items.sortedByDescending { it.updatedAt }
    }

    override fun observeConversations(): Flow<List<AiConversation>> = conversationsFlow

    override fun getConversationById(id: String): Flow<AiConversation?> =
        conversationsFlow.map { list -> list.find { it.id == id } }

    override suspend fun getConversationByIdOnce(id: String): AiConversation? =
        items.find { it.id == id }

    override suspend fun createConversation(conversation: AiConversation): AiConversation {
        items.removeAll { it.id == conversation.id }
        items.add(conversation)
        sync()
        return conversation
    }

    override suspend fun updateConversation(conversation: AiConversation) {
        items.removeAll { it.id == conversation.id }
        items.add(conversation)
        sync()
    }

    override suspend fun deleteConversation(id: String) {
        items.removeAll { it.id == id }
        sync()
    }

    suspend fun clearAllConversations() {
        items.clear()
        sync()
    }
}

class FakeAiMessageRepository : AiMessageRepository {
    private val messagesFlow = MutableStateFlow<List<AiMessage>>(emptyList())
    val items = mutableListOf<AiMessage>()

    private fun sync() {
        messagesFlow.value = items.sortedBy { it.createdAt }
    }

    override fun observeMessagesForConversation(conversationId: String): Flow<List<AiMessage>> =
        messagesFlow.map { list -> list.filter { it.conversationId == conversationId }.sortedBy { it.createdAt } }

    override suspend fun getMessagesForConversationOnce(conversationId: String): List<AiMessage> =
        items.filter { it.conversationId == conversationId }.sortedBy { it.createdAt }

    override suspend fun getMessageByIdOnce(id: String): AiMessage? =
        items.find { it.id == id }

    override suspend fun createMessage(message: AiMessage): AiMessage {
        items.removeAll { it.id == message.id }
        items.add(message)
        sync()
        return message
    }

    override suspend fun updateMessage(message: AiMessage) {
        items.removeAll { it.id == message.id }
        items.add(message)
        sync()
    }

    override suspend fun deleteMessage(id: String) {
        items.removeAll { it.id == id }
        sync()
    }

    override suspend fun deleteMessagesForConversation(conversationId: String) {
        items.removeAll { it.conversationId == conversationId }
        sync()
    }
}

class FakeAiProvider : AiProvider {
    var chunksToEmit: List<AiStreamChunk> = emptyList()
    var shouldThrowException: Boolean = false
    var exceptionToThrow: Exception = RuntimeException("Network connection failed")

    override fun generateStream(
        messages: List<AiMessage>,
        context: StudyContext?,
        config: AiConfig
    ): Flow<AiStreamChunk> = flow {
        if (shouldThrowException) {
            throw exceptionToThrow
        }
        for (chunk in chunksToEmit) {
            emit(chunk)
        }
    }
}

class FakeTestPreferencesDataSource : PreferencesDataSource {
    val aiConfigState = MutableStateFlow(AiConfig())

    override val appState: Flow<AppState> = flowOf(AppState())
    override val themePreference: Flow<AppTheme> = flowOf(AppTheme.SYSTEM)
    override val isOnboardingCompleted: Flow<Boolean> = flowOf(true)
    override val aiConfig: Flow<AiConfig> = aiConfigState
    override val revisionStreakDays: Flow<Int> = flowOf(0)
    override val dailyRevisionTargetMinutes: Flow<Int> = flowOf(15)
    override val lastRevisionEpochDay: Flow<Long> = flowOf(0L)
    override val studyRemindersEnabled: Flow<Boolean> = flowOf(true)
    override val dailyReminderEnabled: Flow<Boolean> = flowOf(true)
    override val dailyReminderTime: Flow<String> = flowOf("19:00")
    override val revisionRemindersEnabled: Flow<Boolean> = flowOf(true)

    override suspend fun setThemePreference(theme: AppTheme) {}
    override suspend fun setOnboardingCompleted(completed: Boolean) {}
    override suspend fun resetOnboarding() {}
    override suspend fun saveAiConfig(config: AiConfig) {
        aiConfigState.value = config
    }
    override suspend fun updateRevisionStreak(todayEpochDay: Long): Int = 1
    override suspend fun setDailyRevisionTargetMinutes(minutes: Int) {}
    override suspend fun setStudyRemindersEnabled(enabled: Boolean) {}
    override suspend fun setDailyReminderEnabled(enabled: Boolean) {}
    override suspend fun setDailyReminderTime(time: String) {}
    override suspend fun setRevisionRemindersEnabled(enabled: Boolean) {}
    override suspend fun resetAll() {}
}

class AiUseCasesTest {

    private lateinit var conversationRepo: FakeAiConversationRepository
    private lateinit var messageRepo: FakeAiMessageRepository
    private lateinit var subjectRepo: FakeSubjectRepository
    private lateinit var chapterRepo: FakeChapterRepository
    private lateinit var aiProvider: FakeAiProvider
    private lateinit var preferencesDataSource: FakeTestPreferencesDataSource

    private lateinit var getConversationsUseCase: GetConversationsUseCase
    private lateinit var getConversationUseCase: GetConversationUseCase
    private lateinit var getMessagesUseCase: GetMessagesUseCase
    private lateinit var createConversationUseCase: CreateConversationUseCase
    private lateinit var deleteConversationUseCase: DeleteConversationUseCase
    private lateinit var getStudyContextUseCase: GetStudyContextUseCase
    private lateinit var sendAiMessageUseCase: SendAiMessageUseCase

    @Before
    fun setup() {
        conversationRepo = FakeAiConversationRepository()
        messageRepo = FakeAiMessageRepository()
        subjectRepo = FakeSubjectRepository()
        chapterRepo = FakeChapterRepository()
        aiProvider = FakeAiProvider()
        preferencesDataSource = FakeTestPreferencesDataSource()

        getConversationsUseCase = GetConversationsUseCase(conversationRepo, subjectRepo, chapterRepo, messageRepo)
        getConversationUseCase = GetConversationUseCase(conversationRepo)
        getMessagesUseCase = GetMessagesUseCase(messageRepo)
        createConversationUseCase = CreateConversationUseCase(conversationRepo, subjectRepo, chapterRepo)
        deleteConversationUseCase = DeleteConversationUseCase(conversationRepo, messageRepo)
        getStudyContextUseCase = GetStudyContextUseCase(subjectRepo, chapterRepo)
        sendAiMessageUseCase = SendAiMessageUseCase(messageRepo, conversationRepo, aiProvider, preferencesDataSource)
    }

    @Test
    fun testCreateConversation_withoutContext_createsDefaultTitle() = runTest {
        val conv = createConversationUseCase()
        assertNotNull(conv)
        assertEquals("New Conversation", conv.title)
        assertNull(conv.subjectId)
        assertNull(conv.chapterId)
    }

    @Test
    fun testCreateConversation_withChapterContext_setsChapterTitle() = runTest {
        val sub = Subject(id = "sub-1", name = "Physics")
        val chap = Chapter(id = "chap-1", subjectId = "sub-1", name = "Electromagnetism", orderIndex = 1)
        subjectRepo.saveSubject(sub)
        chapterRepo.items.add(chap)

        val conv = createConversationUseCase(subjectId = "sub-1", chapterId = "chap-1")

        assertNotNull(conv)
        assertEquals("Physics • Electromagnetism", conv.title)
        assertEquals("sub-1", conv.subjectId)
        assertEquals("chap-1", conv.chapterId)
    }

    @Test
    fun testCreateConversation_withSubjectContextOnly_setsSubjectTitle() = runTest {
        val sub = Subject(id = "sub-2", name = "Calculus")
        subjectRepo.saveSubject(sub)

        val conv = createConversationUseCase(subjectId = "sub-2", chapterId = null)

        assertNotNull(conv)
        assertEquals("Calculus Study", conv.title)
        assertEquals("sub-2", conv.subjectId)
        assertNull(conv.chapterId)
    }

    @Test
    fun testDeleteConversation_deletesConversationAndMessages() = runTest {
        val conv = createConversationUseCase()
        sendAiMessageUseCase.saveUserMessage(conv.id, "Hello")
        sendAiMessageUseCase.saveUserMessage(conv.id, "Explain integrals")

        assertEquals(1, conversationRepo.items.size)
        assertEquals(2, messageRepo.items.size)

        deleteConversationUseCase(conv.id)

        assertEquals(0, conversationRepo.items.size)
        assertEquals(0, messageRepo.items.size)
    }

    @Test
    fun testGetStudyContext_populatesSubjectAndChapterDetails() = runTest {
        val sub = Subject(id = "sub-10", name = "Biology")
        val chap = Chapter(id = "chap-10", subjectId = "sub-10", name = "Cell Division", progress = 45, orderIndex = 1)
        subjectRepo.saveSubject(sub)
        chapterRepo.items.add(chap)

        val context = getStudyContextUseCase("sub-10", "chap-10")

        assertEquals("sub-10", context.subjectId)
        assertEquals("chap-10", context.chapterId)
        assertEquals("Biology", context.subjectName)
        assertEquals("Cell Division", context.chapterName)
        assertEquals(45, context.chapterProgress)
    }

    @Test
    fun testSendAiMessage_saveUserMessage_updatesConversationTitle() = runTest {
        val conv = createConversationUseCase()
        val savedMessage = sendAiMessageUseCase.saveUserMessage(conv.id, "What is Newton's third law?")

        assertEquals(AiMessageRole.USER, savedMessage.role)
        assertEquals(AiMessageStatus.SENT, savedMessage.status)
        assertEquals("What is Newton's third law?", savedMessage.content)

        val updatedConv = conversationRepo.getConversationByIdOnce(conv.id)
        assertEquals("What is Newton's third law?", updatedConv?.title)
    }

    @Test
    fun testSendAiMessage_streamAssistantResponse_success() = runTest {
        val conv = createConversationUseCase()
        sendAiMessageUseCase.saveUserMessage(conv.id, "Explain gravity")

        val placeholder = sendAiMessageUseCase.createAssistantPlaceholder(conv.id)
        assertEquals(AiMessageStatus.STREAMING, placeholder.status)

        aiProvider.chunksToEmit = listOf(
            AiStreamChunk.Content("Gravity is "),
            AiStreamChunk.Content("a fundamental force "),
            AiStreamChunk.Content("of attraction."),
            AiStreamChunk.Done
        )

        val emissions = sendAiMessageUseCase.streamAssistantResponse(
            assistantMessageId = placeholder.id,
            conversationId = conv.id,
            context = null
        ).toList()

        assertEquals(3, emissions.size)
        assertEquals("Gravity is a fundamental force of attraction.", emissions.last())

        val finalizedMessage = messageRepo.getMessageByIdOnce(placeholder.id)
        assertNotNull(finalizedMessage)
        assertEquals(AiMessageStatus.SUCCESS, finalizedMessage?.status)
        assertEquals("Gravity is a fundamental force of attraction.", finalizedMessage?.content)
    }

    @Test
    fun testSendAiMessage_streamAssistantResponse_errorHandling() = runTest {
        val conv = createConversationUseCase()
        sendAiMessageUseCase.saveUserMessage(conv.id, "Explain thermodynamics")

        val placeholder = sendAiMessageUseCase.createAssistantPlaceholder(conv.id)

        aiProvider.chunksToEmit = listOf(
            AiStreamChunk.Content("Thermodynamics "),
            AiStreamChunk.Error(AiErrorType.RATE_LIMIT, "Rate limit reached. Please wait a moment.")
        )

        val emissions = sendAiMessageUseCase.streamAssistantResponse(
            assistantMessageId = placeholder.id,
            conversationId = conv.id,
            context = null
        ).toList()

        assertEquals(1, emissions.size)
        assertEquals("Thermodynamics ", emissions[0])

        val finalizedMessage = messageRepo.getMessageByIdOnce(placeholder.id)
        assertNotNull(finalizedMessage)
        assertEquals(AiMessageStatus.ERROR, finalizedMessage?.status)
        assertEquals("Rate limit reached. Please wait a moment.", finalizedMessage?.errorMessage)
    }
}
