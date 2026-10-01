package com.studyos.app.features.assistant

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyos.app.core.assistant.AssistantCustomization
import com.studyos.app.core.assistant.StudyOSAssistantManager
import com.studyos.app.core.database.dao.FlashcardDao
import com.studyos.app.core.database.dao.NoteDao
import com.studyos.app.core.database.entity.FlashcardEntity
import com.studyos.app.core.database.entity.NoteEntity
import com.studyos.app.core.datastore.PreferencesDataSource
import com.studyos.app.domain.model.AiMessage
import com.studyos.app.domain.model.AiMessageRole
import com.studyos.app.domain.model.AiMessageStatus
import com.studyos.app.domain.model.StudyContext
import com.studyos.app.domain.model.Subject
import com.studyos.app.domain.repository.SubjectRepository
import com.studyos.app.domain.usecase.CreateConversationUseCase
import com.studyos.app.domain.usecase.SendAiMessageUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

data class AssistantOverlayUiState(
    val queryText: String = "",
    val isListening: Boolean = false,
    val isGenerating: Boolean = false,
    val responseText: String = "",
    val screenContextText: String? = null,
    val currentConversationId: String? = null,
    val customization: AssistantCustomization = AssistantCustomization(),
    val isSavedAsNote: Boolean = false,
    val isSavedAsFlashcard: Boolean = false,
    val statusMessage: String? = null,
    val showCustomizationSheet: Boolean = false
)

class AssistantOverlayViewModel(
    private val context: Context,
    private val preferencesDataSource: PreferencesDataSource,
    private val createConversationUseCase: CreateConversationUseCase,
    private val sendAiMessageUseCase: SendAiMessageUseCase,
    private val noteDao: NoteDao,
    private val flashcardDao: FlashcardDao,
    private val subjectRepository: SubjectRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AssistantOverlayUiState())
    val uiState: StateFlow<AssistantOverlayUiState> = _uiState.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null
    private var streamJob: Job? = null
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null

    init {
        observeCustomization()
        observeScreenContext()
    }

    private fun observeCustomization() {
        viewModelScope.launch {
            preferencesDataSource.assistantCustomization.collect { customization ->
                _uiState.update { it.copy(customization = customization) }
            }
        }
    }

    private fun observeScreenContext() {
        viewModelScope.launch {
            StudyOSAssistantManager.assistContext.collect { assistCtx ->
                val text = assistCtx?.textContent?.takeIf { it.isNotBlank() }
                _uiState.update { it.copy(screenContextText = text) }
            }
        }
    }

    fun onQueryChanged(newQuery: String) {
        _uiState.update { it.copy(queryText = newQuery) }
    }

    fun toggleCustomizationSheet(show: Boolean) {
        _uiState.update { it.copy(showCustomizationSheet = show) }
    }

    fun updateCustomization(customization: AssistantCustomization) {
        viewModelScope.launch {
            preferencesDataSource.updateAssistantCustomization(customization)
        }
    }

    fun submitQuery(userQuery: String? = null) {
        val query = (userQuery ?: _uiState.value.queryText).trim()
        if (query.isBlank() || _uiState.value.isGenerating) return

        stopListening()
        triggerHaptic()

        _uiState.update {
            it.copy(
                isGenerating = true,
                responseText = "",
                isSavedAsNote = false,
                isSavedAsFlashcard = false,
                statusMessage = null,
                queryText = query
            )
        }

        streamJob?.cancel()
        streamJob = viewModelScope.launch {
            try {
                // Ensure conversation exists
                var convId = _uiState.value.currentConversationId
                if (convId == null) {
                    val personaTitle = _uiState.value.customization.persona.title
                    val conv = createConversationUseCase(title = "Assistant: $personaTitle")
                    convId = conv.id
                    _uiState.update { it.copy(currentConversationId = convId) }
                }

                // Append persona instruction context
                val personaPrompt = _uiState.value.customization.persona.promptInstruction
                val fullQuery = "$personaPrompt\n\nStudent Query: $query"

                // Create user message
                sendAiMessageUseCase.saveUserMessage(convId, fullQuery)

                // Placeholder for assistant streaming
                val assistantMsg = sendAiMessageUseCase.createAssistantPlaceholder(convId)

                // Stream response
                val studyContext = StudyContext(subjectName = "General Study")
                sendAiMessageUseCase.streamAssistantResponse(
                    assistantMessageId = assistantMsg.id,
                    conversationId = convId,
                    context = studyContext
                ).collect { content ->
                    _uiState.update {
                        it.copy(responseText = content)
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        responseText = if (it.responseText.isNotBlank()) it.responseText else "Unable to generate response: ${e.message}",
                        statusMessage = "Error occurred during generation"
                    )
                }
            } finally {
                _uiState.update { it.copy(isGenerating = false) }
                triggerHaptic()
            }
        }
    }

    fun askAboutScreen() {
        val screenText = _uiState.value.screenContextText ?: return
        val prompt = "Explain what is on my screen and summarize the core study concepts and takeaways:\n\n```\n$screenText\n```"
        submitQuery(prompt)
    }

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _uiState.update { it.copy(statusMessage = "Speech recognition unavailable on this device") }
            return
        }

        if (_uiState.value.isListening) {
            stopListening()
            return
        }

        requestAudioFocus()
        triggerHaptic()

        try {
            if (speechRecognizer == null) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(createRecognitionListener())
                }
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }

            speechRecognizer?.startListening(intent)
            _uiState.update { it.copy(isListening = true, statusMessage = "Listening...") }
        } catch (e: Exception) {
            stopListening()
            _uiState.update { it.copy(statusMessage = "Could not activate microphone") }
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (ignored: Exception) {
        }
        abandonAudioFocus()
        _uiState.update { it.copy(isListening = false) }
    }

    fun saveCurrentResponseAsNote() {
        val text = _uiState.value.responseText
        if (text.isBlank()) return

        viewModelScope.launch {
            try {
                val queryPreview = _uiState.value.queryText.take(40).ifBlank { "Study Concept" }
                val title = "Assistant Note: $queryPreview"
                val note = NoteEntity(
                    id = UUID.randomUUID().toString(),
                    title = title,
                    content = text,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                withContext(Dispatchers.IO) {
                    noteDao.insert(note)
                }
                _uiState.update { it.copy(isSavedAsNote = true, statusMessage = "Saved to Notes!") }
                triggerHaptic()
            } catch (e: Exception) {
                _uiState.update { it.copy(statusMessage = "Failed to save note") }
            }
        }
    }

    fun saveCurrentResponseAsFlashcard() {
        val text = _uiState.value.responseText
        val query = _uiState.value.queryText
        if (text.isBlank()) return

        viewModelScope.launch {
            try {
                val subjects = withContext(Dispatchers.IO) {
                    subjectRepository.getAllSubjectsOnce()
                }
                val subjectId = if (subjects.isNotEmpty()) {
                    subjects.first().id
                } else {
                    val defaultSubject = Subject(id = UUID.randomUUID().toString(), name = "General Studies")
                    withContext(Dispatchers.IO) {
                        subjectRepository.saveSubject(defaultSubject)
                    }
                    defaultSubject.id
                }

                val card = FlashcardEntity(
                    id = UUID.randomUUID().toString(),
                    subjectId = subjectId,
                    question = query.ifBlank { "Core Concept" },
                    answer = text.take(500),
                    difficulty = "MEDIUM",
                    createdAt = System.currentTimeMillis()
                )
                withContext(Dispatchers.IO) {
                    flashcardDao.insert(card)
                }
                _uiState.update { it.copy(isSavedAsFlashcard = true, statusMessage = "Saved to Flashcards!") }
                triggerHaptic()
            } catch (e: Exception) {
                _uiState.update { it.copy(statusMessage = "Failed to save flashcard") }
            }
        }
    }

    private fun createRecognitionListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _uiState.update { it.copy(isListening = true) }
        }

        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {
            _uiState.update { it.copy(isListening = false) }
        }

        override fun onError(error: Int) {
            abandonAudioFocus()
            _uiState.update { it.copy(isListening = false, statusMessage = null) }
        }

        override fun onResults(results: Bundle?) {
            abandonAudioFocus()
            _uiState.update { it.copy(isListening = false) }
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val recognizedText = matches?.firstOrNull()?.trim()
            if (!recognizedText.isNullOrBlank()) {
                _uiState.update { it.copy(queryText = recognizedText) }
                submitQuery(recognizedText)
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val partial = matches?.firstOrNull()?.trim()
            if (!partial.isNullOrBlank()) {
                _uiState.update { it.copy(queryText = partial) }
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun requestAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val playbackAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                    .setAudioAttributes(playbackAttributes)
                    .setAcceptsDelayedFocusGain(false)
                    .setOnAudioFocusChangeListener { /* No-op */ }
                    .build()
                audioFocusRequest?.let { audioManager?.requestAudioFocus(it) }
            } else {
                @Suppress("DEPRECATION")
                audioManager?.requestAudioFocus(
                    null,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE
                )
            }
        } catch (ignored: Exception) {
        }
    }

    private fun abandonAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                audioManager?.abandonAudioFocus(null)
            }
        } catch (ignored: Exception) {
        }
    }

    private fun triggerHaptic() {
        if (!_uiState.value.customization.hapticFeedback) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(25)
                }
            }
        } catch (ignored: Exception) {
        }
    }

    override fun onCleared() {
        super.onCleared()
        streamJob?.cancel()
        abandonAudioFocus()
        try {
            speechRecognizer?.destroy()
        } catch (ignored: Exception) {
        }
        speechRecognizer = null
    }
}
