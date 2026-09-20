package com.studyos.app.features

import com.studyos.app.domain.FakeAiConversationRepository
import com.studyos.app.domain.FakeAiMessageRepository
import com.studyos.app.domain.FakeAiProvider
import com.studyos.app.domain.FakeChapterRepository
import com.studyos.app.domain.FakeSubjectRepository
import com.studyos.app.domain.FakeTestPreferencesDataSource
import com.studyos.app.domain.model.AiConfig
import com.studyos.app.domain.model.AiMessageRole
import com.studyos.app.domain.model.AiMessageStatus
import com.studyos.app.domain.model.AiStreamChunk
import com.studyos.app.domain.model.Chapter
import com.studyos.app.domain.model.QuickAction
import com.studyos.app.domain.model.Subject
import com.studyos.app.domain.usecase.CreateConversationUseCase
import com.studyos.app.domain.usecase.DeleteConversationUseCase
import com.studyos.app.domain.usecase.GetAiConfigUseCase
import com.studyos.app.domain.usecase.GetConversationUseCase
import com.studyos.app.domain.usecase.GetConversationsUseCase
import com.studyos.app.domain.usecase.GetMessagesUseCase
import com.studyos.app.domain.usecase.GetStudyContextUseCase
import com.studyos.app.domain.usecase.SaveAiConfigUseCase
import com.studyos.app.domain.usecase.SendAiMessageUseCase
import com.studyos.app.features.ai.viewmodel.AiAssistantViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AiAssistantViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
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
    private lateinit var getAiConfigUseCase: GetAiConfigUseCase
    private lateinit var saveAiConfigUseCase: SaveAiConfigUseCase

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
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
        getAiConfigUseCase = GetAiConfigUseCase(preferencesDataSource)
        saveAiConfigUseCase = SaveAiConfigUseCase(preferencesDataSource)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(
        initialConversationId: String? = null,
        initialSubjectId: String? = null,
        initialChapterId: String? = null
    ): AiAssistantViewModel {
        return AiAssistantViewModel(
            getConversationsUseCase = getConversationsUseCase,
            getConversationUseCase = getConversationUseCase,
            getMessagesUseCase = getMessagesUseCase,
            createConversationUseCase = createConversationUseCase,
            deleteConversationUseCase = deleteConversationUseCase,
            getStudyContextUseCase = getStudyContextUseCase,
            sendAiMessageUseCase = sendAiMessageUseCase,
            getAiConfigUseCase = getAiConfigUseCase,
            saveAiConfigUseCase = saveAiConfigUseCase,
            initialConversationId = initialConversationId,
            initialSubjectId = initialSubjectId,
            initialChapterId = initialChapterId
        )
    }

    @Test
    fun testInitialState_withoutContext_createsOrLoadsConversation() = runTest {
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.currentConversation)
        assertEquals("New Conversation", state.currentConversation?.title)
    }

    @Test
    fun testInitialState_withChapterContext_loadsContext() = runTest {
        val sub = Subject(id = "sub-1", name = "Physics")
        val chap = Chapter(id = "chap-1", subjectId = "sub-1", name = "Electromagnetism", progress = 60, orderIndex = 1)
        subjectRepo.saveSubject(sub)
        chapterRepo.items.add(chap)

        val viewModel = createViewModel(
            initialSubjectId = "sub-1",
            initialChapterId = "chap-1"
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("sub-1", state.studyContext.subjectId)
        assertEquals("chap-1", state.studyContext.chapterId)
        assertEquals("Physics", state.studyContext.subjectName)
        assertEquals("Electromagnetism", state.studyContext.chapterName)
        assertEquals(60, state.studyContext.chapterProgress)
    }

    @Test
    fun testInputText_changed() = runTest {
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        viewModel.onInputTextChanged("Explain Bernoulli's principle")
        assertEquals("Explain Bernoulli's principle", viewModel.uiState.value.inputText)
    }

    @Test
    fun testQuickAction_explainThis_generatesPromptWithContext() = runTest {
        val sub = Subject(id = "sub-1", name = "Chemistry")
        val chap = Chapter(id = "chap-1", subjectId = "sub-1", name = "Stoichiometry", orderIndex = 1)
        subjectRepo.saveSubject(sub)
        chapterRepo.items.add(chap)

        val viewModel = createViewModel(
            initialSubjectId = "sub-1",
            initialChapterId = "chap-1"
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        viewModel.executeQuickAction(QuickAction.EXPLAIN)
        val input = viewModel.uiState.value.inputText

        assertTrue(input.startsWith(QuickAction.EXPLAIN.promptPrefix))
        assertTrue(input.contains("Stoichiometry"))
    }

    @Test
    fun testSendMessage_savesUserMessageAndTriggersStream() = runTest {
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        aiProvider.chunksToEmit = listOf(
            AiStreamChunk.Content("Inertia is the resistance of any physical object "),
            AiStreamChunk.Content("to any change in its velocity."),
            AiStreamChunk.Done
        )

        viewModel.sendMessage("What is inertia?")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isGenerating)
        assertEquals(2, state.messages.size)

        val userMessage = state.messages[0]
        assertEquals(AiMessageRole.USER, userMessage.role)
        assertEquals("What is inertia?", userMessage.content)

        val assistantMessage = state.messages[1]
        assertEquals(AiMessageRole.ASSISTANT, assistantMessage.role)
        assertEquals("Inertia is the resistance of any physical object to any change in its velocity.", assistantMessage.content)
        assertEquals(AiMessageStatus.SUCCESS, assistantMessage.status)
    }

    @Test
    fun testStopGeneration_stopsGenerating() = runTest {
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        viewModel.stopGeneration()
        assertFalse(viewModel.uiState.value.isGenerating)
    }

    @Test
    fun testSaveAiConfig_updatesState() = runTest {
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        val newConfig = AiConfig(
            apiKey = "sk-test-key-12345",
            baseUrl = "https://api.openai.com/v1",
            model = "gpt-4o"
        )
        viewModel.saveAiConfig(newConfig)
        advanceUntilIdle()

        assertEquals("sk-test-key-12345", viewModel.uiState.value.aiConfig.apiKey)
        assertEquals("gpt-4o", viewModel.uiState.value.aiConfig.model)
        assertEquals("AI configuration saved.", viewModel.uiState.value.infoMessage)
    }

    @Test
    fun testClearContext_removesAttachedStudyContext() = runTest {
        val sub = Subject(id = "sub-1", name = "History")
        val chap = Chapter(id = "chap-1", subjectId = "sub-1", name = "Industrial Revolution", orderIndex = 1)
        subjectRepo.saveSubject(sub)
        chapterRepo.items.add(chap)

        val viewModel = createViewModel(
            initialSubjectId = "sub-1",
            initialChapterId = "chap-1"
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.studyContext.hasContext)

        viewModel.clearContext()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.studyContext.hasContext)
    }
}
