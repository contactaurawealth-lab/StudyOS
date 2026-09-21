package com.studyos.app.features.ai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyos.app.domain.model.AiConfig
import com.studyos.app.domain.model.AiConversation
import com.studyos.app.domain.model.AiConversationItem
import com.studyos.app.domain.model.AiMessage
import com.studyos.app.domain.model.AiMessageRole
import com.studyos.app.domain.model.QuickAction
import com.studyos.app.domain.model.StudyContext
import com.studyos.app.domain.usecase.CreateConversationUseCase
import com.studyos.app.domain.usecase.DeleteConversationUseCase
import com.studyos.app.domain.usecase.GetAiConfigUseCase
import com.studyos.app.domain.usecase.GetConversationUseCase
import com.studyos.app.domain.usecase.GetConversationsUseCase
import com.studyos.app.domain.usecase.GetMessagesUseCase
import com.studyos.app.domain.usecase.GetStudyContextUseCase
import com.studyos.app.domain.usecase.SaveAiConfigUseCase
import com.studyos.app.domain.usecase.SendAiMessageUseCase
import com.studyos.app.domain.model.AiTutorMode
import com.studyos.app.domain.model.ChapterAiContext
import com.studyos.app.domain.model.PracticeDifficulty
import com.studyos.app.domain.usecase.AiStudyEngineUseCase
import com.studyos.app.domain.usecase.GetChapterAiContextUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AiAssistantUiState(
    val currentConversation: AiConversation? = null,
    val conversations: List<AiConversationItem> = emptyList(),
    val messages: List<AiMessage> = emptyList(),
    val studyContext: StudyContext = StudyContext(),
    val chapterAiContext: ChapterAiContext? = null,
    val selectedMode: AiTutorMode = AiTutorMode.STUDY_PARTNER,
    val selectedDifficulty: PracticeDifficulty = PracticeDifficulty.MEDIUM,
    val isChapterContextVisible: Boolean = true,
    val aiConfig: AiConfig = AiConfig(),
    val inputText: String = "",
    val isGenerating: Boolean = false,
    val isLoading: Boolean = true,
    val isHistorySheetOpen: Boolean = false,
    val isSettingsSheetOpen: Boolean = false,
    val infoMessage: String? = null
)

class AiAssistantViewModel(
    private val getConversationsUseCase: GetConversationsUseCase,
    private val getConversationUseCase: GetConversationUseCase,
    private val getMessagesUseCase: GetMessagesUseCase,
    private val createConversationUseCase: CreateConversationUseCase,
    private val deleteConversationUseCase: DeleteConversationUseCase,
    private val getStudyContextUseCase: GetStudyContextUseCase,
    private val sendAiMessageUseCase: SendAiMessageUseCase,
    private val getAiConfigUseCase: GetAiConfigUseCase,
    private val saveAiConfigUseCase: SaveAiConfigUseCase,
    private val getChapterAiContextUseCase: GetChapterAiContextUseCase? = null,
    private val aiStudyEngineUseCase: AiStudyEngineUseCase? = null,
    private val initialConversationId: String? = null,
    private val initialSubjectId: String? = null,
    private val initialChapterId: String? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiAssistantUiState())
    val uiState: StateFlow<AiAssistantUiState> = _uiState.asStateFlow()

    private var messagesJob: Job? = null
    private var streamJob: Job? = null
    private var currentAssistantMessageId: String? = null

    init {
        observeConversations()
        observeAiConfig()
        initializeSession(initialConversationId, initialSubjectId, initialChapterId)
    }

    private fun observeConversations() {
        viewModelScope.launch {
            getConversationsUseCase().collect { conversationsList ->
                _uiState.update { it.copy(conversations = conversationsList) }
            }
        }
    }

    private fun observeAiConfig() {
        viewModelScope.launch {
            getAiConfigUseCase().collect { config ->
                _uiState.update { it.copy(aiConfig = config) }
            }
        }
    }

    private fun sanitizeParam(param: String?): String? {
        if (param.isNullOrBlank()) return null
        val trimmed = param.trim()
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) return null
        return trimmed
    }

    private fun initializeSession(rawConvId: String?, rawSubjectId: String?, rawChapterId: String?) {
        val convId = sanitizeParam(rawConvId)
        val subjectId = sanitizeParam(rawSubjectId)
        val chapterId = sanitizeParam(rawChapterId)

        viewModelScope.launch {
            try {
                if (!convId.isNullOrBlank()) {
                    val conversation = getConversationUseCase.getOnce(convId)
                    if (conversation != null) {
                        loadConversation(conversation)
                        return@launch
                    }
                }

                // If chapter or subject specified, start conversation with context attached
                if (chapterId != null || subjectId != null) {
                    val newConv = createConversationUseCase(
                        subjectId = subjectId,
                        chapterId = chapterId
                    )
                    loadConversation(newConv)
                    return@launch
                }

                // Otherwise, get most recent conversation or create a new one
                val existingList = _uiState.value.conversations
                if (existingList.isNotEmpty()) {
                    val recent = existingList.first().conversation
                    loadConversation(recent)
                } else {
                    val newConv = createConversationUseCase()
                    loadConversation(newConv)
                }
            } catch (e: Exception) {
                // Fallback gracefully without shutting down the app
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        messages = emptyList()
                    )
                }
            }
        }
    }

    fun selectConversation(conversationId: String) {
        viewModelScope.launch {
            stopGeneration()
            val conv = getConversationUseCase.getOnce(conversationId)
            if (conv != null) {
                loadConversation(conv)
            }
        }
    }

    private suspend fun loadConversation(conversation: AiConversation) {
        val context = getStudyContextUseCase(conversation.subjectId, conversation.chapterId)
        val chapterAiCtx = if (!conversation.chapterId.isNullOrBlank()) {
            getChapterAiContextUseCase?.invoke(conversation.chapterId)
        } else null

        _uiState.update {
            it.copy(
                currentConversation = conversation,
                studyContext = context,
                chapterAiContext = chapterAiCtx,
                isLoading = false
            )
        }

        observeMessages(conversation.id)
    }

    private fun observeMessages(conversationId: String) {
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            getMessagesUseCase(conversationId).collect { msgs ->
                _uiState.update { it.copy(messages = msgs) }
            }
        }
    }

    fun setTutorMode(mode: AiTutorMode) {
        _uiState.update { it.copy(selectedMode = mode) }
    }

    fun setPracticeDifficulty(difficulty: PracticeDifficulty) {
        _uiState.update { it.copy(selectedDifficulty = difficulty) }
    }

    fun toggleChapterContextVisibility() {
        _uiState.update { it.copy(isChapterContextVisible = !it.isChapterContextVisible) }
    }

    fun triggerQuickStudyAction(actionCode: String) {
        val ctx = _uiState.value.chapterAiContext
        val topic = ctx?.chapterName ?: _uiState.value.studyContext.chapterName ?: "the current topic"

        when (actionCode) {
            "EXPLAIN" -> {
                sendMessage("Explain $topic simply with clear key points and an everyday analogy.")
            }
            "TEACH_ME" -> {
                setTutorMode(AiTutorMode.TEACH_ME)
                sendMessage("Teach me $topic step-by-step from the beginning. Give me the first concept in 2-3 sentences, then ask me a check question.")
            }
            "DOUBT" -> {
                setTutorMode(AiTutorMode.DOUBT_SOLVER)
                _uiState.update { it.copy(inputText = "I'm having trouble understanding ") }
            }
            "EXAMPLE" -> {
                sendMessage("Give me 2 clear, practical real-world examples of $topic in action.")
            }
            "QUIZ_ME" -> {
                setTutorMode(AiTutorMode.PRACTICE_DRILL)
                sendMessage("Test my knowledge on $topic with 1 ${_uiState.value.selectedDifficulty.label}-level conceptual question and 4 choices.")
            }
            "SIMPLIFY" -> {
                sendMessage("Simplify $topic as if I am 12 years old (ELI5). Avoid heavy jargon.")
            }
        }
    }

    fun onInputTextChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendMessage(customPrompt: String? = null) {
        val promptToSend = customPrompt ?: _uiState.value.inputText.trim()
        if (promptToSend.isBlank() || _uiState.value.isGenerating) return

        val conversation = _uiState.value.currentConversation ?: return

        _uiState.update { it.copy(inputText = "", isGenerating = true) }

        viewModelScope.launch {
            try {
                // 1. Save user message
                sendAiMessageUseCase.saveUserMessage(conversation.id, promptToSend)

                // 2. Create placeholder assistant message
                val assistantMsg = sendAiMessageUseCase.createAssistantPlaceholder(conversation.id)
                currentAssistantMessageId = assistantMsg.id

                // 3. Start streaming
                streamResponse(assistantMsg.id, conversation.id)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        infoMessage = e.message ?: "Failed to send message."
                    )
                }
            }
        }
    }

    private fun streamResponse(assistantMessageId: String, conversationId: String) {
        streamJob?.cancel()
        streamJob = viewModelScope.launch {
            try {
                val studyCtx = _uiState.value.studyContext
                val chCtx = _uiState.value.chapterAiContext
                val effectiveContext = if (chCtx != null) {
                    studyCtx.copy(
                        chapterName = chCtx.chapterName,
                        subjectName = chCtx.subjectName,
                        chapterProgress = chCtx.progress,
                        weakTopics = chCtx.weakTopics
                    )
                } else if (studyCtx.hasContext) studyCtx else null

                sendAiMessageUseCase.streamAssistantResponse(
                    assistantMessageId = assistantMessageId,
                    conversationId = conversationId,
                    context = effectiveContext
                ).collect {
                    // Reactive Room collection automatically updates uiState.messages
                }
            } finally {
                _uiState.update { it.copy(isGenerating = false) }
                currentAssistantMessageId = null
            }
        }
    }

    fun executeQuickAction(action: QuickAction) {
        val context = _uiState.value.studyContext
        val targetTopic = buildString {
            if (!context.chapterName.isNullOrBlank()) {
                append(context.chapterName)
                if (!context.subjectName.isNullOrBlank()) append(" (${context.subjectName})")
            } else if (!context.subjectName.isNullOrBlank()) {
                append(context.subjectName)
            }
        }

        val prompt = if (targetTopic.isNotBlank()) {
            "${action.promptPrefix} $targetTopic"
        } else {
            action.promptPrefix
        }

        _uiState.update { it.copy(inputText = prompt) }
    }

    fun stopGeneration() {
        streamJob?.cancel()
        streamJob = null
        currentAssistantMessageId?.let { msgId ->
            viewModelScope.launch {
                sendAiMessageUseCase.cancelStreaming(msgId)
                currentAssistantMessageId = null
            }
        }
        _uiState.update { it.copy(isGenerating = false) }
    }

    fun retryMessage(messageId: String) {
        if (_uiState.value.isGenerating) return

        val messages = _uiState.value.messages
        val messageIndex = messages.indexOfFirst { it.id == messageId }
        if (messageIndex == -1) return

        val conversation = _uiState.value.currentConversation ?: return

        // Find the user query preceding this assistant message
        val userQuery = messages.take(messageIndex)
            .lastOrNull { it.role == AiMessageRole.USER }?.content

        if (!userQuery.isNullOrBlank()) {
            viewModelScope.launch {
                stopGeneration()
                val assistantMsg = sendAiMessageUseCase.createAssistantPlaceholder(conversation.id)
                currentAssistantMessageId = assistantMsg.id
                _uiState.update { it.copy(isGenerating = true) }
                streamResponse(assistantMsg.id, conversation.id)
            }
        }
    }

    fun regenerateMessage(messageId: String) {
        retryMessage(messageId)
    }

    fun startNewConversation(subjectId: String? = null, chapterId: String? = null) {
        viewModelScope.launch {
            stopGeneration()
            val newConv = createConversationUseCase(
                subjectId = subjectId,
                chapterId = chapterId
            )
            loadConversation(newConv)
            _uiState.update { it.copy(isHistorySheetOpen = false) }
        }
    }

    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            deleteConversationUseCase(conversationId)
            if (_uiState.value.currentConversation?.id == conversationId) {
                val remaining = _uiState.value.conversations.filter { it.conversation.id != conversationId }
                if (remaining.isNotEmpty()) {
                    loadConversation(remaining.first().conversation)
                } else {
                    val newConv = createConversationUseCase()
                    loadConversation(newConv)
                }
            }
        }
    }

    fun saveAiConfig(config: AiConfig) {
        viewModelScope.launch {
            saveAiConfigUseCase(config)
            _uiState.update {
                it.copy(
                    aiConfig = config,
                    infoMessage = "AI configuration saved."
                )
            }
        }
    }

    fun clearContext() {
        val currentConv = _uiState.value.currentConversation ?: return
        viewModelScope.launch {
            _uiState.update {
                it.copy(studyContext = StudyContext())
            }
        }
    }

    fun openHistorySheet() {
        _uiState.update { it.copy(isHistorySheetOpen = true) }
    }

    fun closeHistorySheet() {
        _uiState.update { it.copy(isHistorySheetOpen = false) }
    }

    fun openSettingsSheet() {
        _uiState.update { it.copy(isSettingsSheetOpen = true) }
    }

    fun closeSettingsSheet() {
        _uiState.update { it.copy(isSettingsSheetOpen = false) }
    }

    fun clearInfoMessage() {
        _uiState.update { it.copy(infoMessage = null) }
    }
}
