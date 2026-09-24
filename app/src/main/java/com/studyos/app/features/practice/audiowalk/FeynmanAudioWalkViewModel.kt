package com.studyos.app.features.practice.audiowalk

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyos.app.domain.provider.AiProvider
import com.studyos.app.domain.model.AiMessage
import com.studyos.app.domain.model.AiMessageRole
import com.studyos.app.domain.model.AiStreamChunk
import com.studyos.app.domain.model.Mistake
import com.studyos.app.domain.model.Note
import com.studyos.app.domain.repository.ChapterRepository
import com.studyos.app.domain.repository.MistakeRepository
import com.studyos.app.domain.repository.NoteRepository
import com.studyos.app.domain.repository.SubjectRepository
import com.studyos.app.domain.usecase.GetAiConfigUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.util.UUID

data class SocraticQuestion(
    val id: String = UUID.randomUUID().toString(),
    val question: String,
    val expectedConcept: String,
    val sourceContext: String,
    val studentResponse: String? = null,
    val feedback: String? = null,
    val isEvaluated: Boolean = false
)

enum class WalkStatus {
    IDLE,
    SPEAKING_QUESTION,
    LISTENING_ANSWER,
    EVALUATING,
    SPEAKING_FEEDBACK,
    FINISHED
}

data class FeynmanAudioWalkUiState(
    val title: String = "Feynman Audio Walk",
    val subtitle: String = "Hands-Free Socratic Revision",
    val subjectId: String? = null,
    val chapterId: String? = null,
    val subjectName: String = "",
    val chapterName: String? = null,
    val questions: List<SocraticQuestion> = emptyList(),
    val currentIndex: Int = 0,
    val walkStatus: WalkStatus = WalkStatus.IDLE,
    val targetDurationMinutes: Int = 15,
    val remainingSeconds: Int = 15 * 60,
    val isTimerActive: Boolean = false,
    val spokenTranscript: String = "",
    val lastEvaluationFeedback: String = "",
    val uploadedAudioUri: Uri? = null,
    val uploadedAudioFileName: String? = null,
    val isAudioRevisionPlaying: Boolean = false,
    val currentAudioPositionMs: Int = 0,
    val totalAudioDurationMs: Int = 0,
    val playbackSpeed: Float = 1.0f,
    val notesCount: Int = 0,
    val mistakesCount: Int = 0,
    val isLoading: Boolean = true,
    val activeTab: Int = 0 // 0: Socratic Walk, 1: Audio Revision Player
)

class FeynmanAudioWalkViewModel(
    private val subjectId: String?,
    private val chapterId: String?,
    private val noteRepository: NoteRepository,
    private val mistakeRepository: MistakeRepository,
    private val subjectRepository: SubjectRepository,
    private val chapterRepository: ChapterRepository,
    private val aiProvider: AiProvider,
    private val getAiConfigUseCase: GetAiConfigUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        FeynmanAudioWalkUiState(
            subjectId = subjectId,
            chapterId = chapterId
        )
    )
    val uiState: StateFlow<FeynmanAudioWalkUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var cachedNotes: List<Note> = emptyList()
    private var cachedMistakes: List<Mistake> = emptyList()

    init {
        loadContext()
    }

    private fun loadContext() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            var sName = "All Subjects"
            if (!subjectId.isNullOrBlank()) {
                val s = subjectRepository.getSubjectByIdOnce(subjectId)
                if (s != null) {
                    sName = s.name
                }
            }

            var cName: String? = null
            if (!chapterId.isNullOrBlank()) {
                val c = chapterRepository.getChapterById(chapterId)
                if (c != null) {
                    cName = c.name
                    if (sName == "All Subjects") {
                        val parentSubject = subjectRepository.getSubjectByIdOnce(c.subjectId)
                        if (parentSubject != null) {
                            sName = parentSubject.name
                        }
                    }
                }
            }

            // Load Notes
            val notes = when {
                !chapterId.isNullOrBlank() -> noteRepository.observeNotesForChapter(chapterId).firstOrNull() ?: emptyList()
                !subjectId.isNullOrBlank() -> noteRepository.observeNotesForSubject(subjectId).firstOrNull() ?: emptyList()
                else -> noteRepository.observeAllNotes().firstOrNull() ?: emptyList()
            }
            cachedNotes = notes

            // Load Mistakes
            val mistakes = when {
                !chapterId.isNullOrBlank() -> mistakeRepository.observeMistakesForChapter(chapterId).firstOrNull() ?: emptyList()
                !subjectId.isNullOrBlank() -> mistakeRepository.observeMistakesForSubject(subjectId).firstOrNull() ?: emptyList()
                else -> mistakeRepository.observeAllMistakes().firstOrNull() ?: emptyList()
            }
            cachedMistakes = mistakes

            _uiState.update {
                it.copy(
                    subjectName = sName,
                    chapterName = cName,
                    notesCount = notes.size,
                    mistakesCount = mistakes.size,
                    isLoading = false
                )
            }

            generateSocraticQuestions(notes, mistakes, sName, cName)
        }
    }

    private suspend fun generateSocraticQuestions(
        notes: List<Note>,
        mistakes: List<Mistake>,
        subjectName: String,
        chapterName: String?
    ) {
        val config = getAiConfigUseCase().firstOrNull()
        val apiKey = config?.apiKey?.trim()

        if (!apiKey.isNullOrBlank()) {
            try {
                val promptBuilder = StringBuilder()
                promptBuilder.append("You are a Socratic tutor guiding a student through a hands-free walk revision on $subjectName")
                if (!chapterName.isNullOrBlank()) {
                    promptBuilder.append(" ($chapterName)")
                }
                promptBuilder.append(".\n\n")

                if (notes.isNotEmpty()) {
                    promptBuilder.append("LECTURE NOTES & UPLOADED MATERIALS:\n")
                    notes.take(4).forEach { note ->
                        promptBuilder.append("- Title: ${note.title}\nContent snippet: ${note.content.take(300)}\n")
                    }
                    promptBuilder.append("\n")
                }

                if (mistakes.isNotEmpty()) {
                    promptBuilder.append("MISTAKE BANK (WEAK AREAS):\n")
                    mistakes.take(4).forEach { m ->
                        promptBuilder.append("- Question: ${m.question}\nCorrect Answer: ${m.correctAnswer}\nStudent Missed: ${m.studentAnswer}\n")
                    }
                    promptBuilder.append("\n")
                }

                promptBuilder.append("Create 5 oral Socratic questions using the Feynman technique. Ask the student to explain the 'why' or 'how' out loud as if explaining to a beginner. Format strictly as JSON array of objects with keys: \"question\", \"expectedConcept\", \"sourceContext\". No markdown formatting around the JSON.")

                val messages = listOf(
                    AiMessage(
                        conversationId = "audio_walk",
                        role = AiMessageRole.USER,
                        content = promptBuilder.toString()
                    )
                )

                val responseBuilder = StringBuilder()
                aiProvider.generateStream(messages, null, config).collect { chunk ->
                    if (chunk is AiStreamChunk.Content) {
                        responseBuilder.append(chunk.delta)
                    }
                }
                val response = responseBuilder.toString()
                val jsonStr = response.trim().removeSurrounding("```json", "```").removeSurrounding("```", "```").trim()
                val jsonArray = JSONArray(jsonStr)

                val questionsList = mutableListOf<SocraticQuestion>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    questionsList.add(
                        SocraticQuestion(
                            question = obj.getString("question"),
                            expectedConcept = obj.optString("expectedConcept", "Key conceptual understanding"),
                            sourceContext = obj.optString("sourceContext", "Study Notes")
                        )
                    )
                }

                if (questionsList.isNotEmpty()) {
                    _uiState.update { it.copy(questions = questionsList) }
                    return
                }
            } catch (e: Exception) {
                // Fallback to intelligent offline generation
            }
        }

        // Offline Rule-based Socratic Synthesis
        val fallbackList = mutableListOf<SocraticQuestion>()

        mistakes.take(3).forEach { m ->
            fallbackList.add(
                SocraticQuestion(
                    question = "You recently encountered: '${m.question}'. In your own words, explain why '${m.correctAnswer}' is the correct reasoning.",
                    expectedConcept = m.correctAnswer,
                    sourceContext = "Mistake Bank (${m.topic ?: subjectName})"
                )
            )
        }

        notes.take(4).forEach { n ->
            val firstLine = n.content.lines().firstOrNull { it.isNotBlank() }?.take(100) ?: n.title
            fallbackList.add(
                SocraticQuestion(
                    question = "In your notes on '${n.title}', explain the core idea of this concept as if you were teaching a peer: $firstLine",
                    expectedConcept = n.title,
                    sourceContext = "Lecture Note: ${n.title}"
                )
            )
        }

        if (fallbackList.isEmpty()) {
            fallbackList.add(
                SocraticQuestion(
                    question = "What is the single most important concept in $subjectName that you want to master during this walk? Explain how it works step-by-step.",
                    expectedConcept = "Core subject framework",
                    sourceContext = "Active Synthesis"
                )
            )
            fallbackList.add(
                SocraticQuestion(
                    question = "Think of a common misconception or pitfall in $subjectName. How would you explain it to avoid making that mistake?",
                    expectedConcept = "Conceptual distinction",
                    sourceContext = "Feynman Challenge"
                )
            )
        }

        _uiState.update { it.copy(questions = fallbackList) }
    }

    fun startWalk(durationMinutes: Int = 15) {
        timerJob?.cancel()
        _uiState.update {
            it.copy(
                targetDurationMinutes = durationMinutes,
                remainingSeconds = durationMinutes * 60,
                isTimerActive = true,
                currentIndex = 0,
                walkStatus = WalkStatus.SPEAKING_QUESTION,
                spokenTranscript = "",
                lastEvaluationFeedback = ""
            )
        }

        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                val curr = _uiState.value.remainingSeconds
                if (curr <= 1) {
                    _uiState.update {
                        it.copy(
                            remainingSeconds = 0,
                            isTimerActive = false,
                            walkStatus = WalkStatus.FINISHED
                        )
                    }
                    break
                } else {
                    _uiState.update { it.copy(remainingSeconds = curr - 1) }
                }
            }
        }
    }

    fun pauseWalk() {
        timerJob?.cancel()
        _uiState.update { it.copy(isTimerActive = false) }
    }

    fun resumeWalk() {
        if (_uiState.value.remainingSeconds <= 0) return
        _uiState.update { it.copy(isTimerActive = true) }
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                val curr = _uiState.value.remainingSeconds
                if (curr <= 1) {
                    _uiState.update {
                        it.copy(
                            remainingSeconds = 0,
                            isTimerActive = false,
                            walkStatus = WalkStatus.FINISHED
                        )
                    }
                    break
                } else {
                    _uiState.update { it.copy(remainingSeconds = curr - 1) }
                }
            }
        }
    }

    fun onQuestionTtsFinished() {
        if (_uiState.value.walkStatus == WalkStatus.SPEAKING_QUESTION) {
            _uiState.update { it.copy(walkStatus = WalkStatus.LISTENING_ANSWER) }
        }
    }

    fun onUserAnswerSpoken(transcript: String) {
        val trimmed = transcript.trim()
        if (trimmed.isBlank()) return

        val state = _uiState.value
        val currentQ = state.questions.getOrNull(state.currentIndex) ?: return

        _uiState.update {
            it.copy(
                spokenTranscript = trimmed,
                walkStatus = WalkStatus.EVALUATING
            )
        }

        viewModelScope.launch {
            evaluateAnswer(currentQ, trimmed)
        }
    }

    private suspend fun evaluateAnswer(question: SocraticQuestion, studentAnswer: String) {
        val config = getAiConfigUseCase().firstOrNull()
        val apiKey = config?.apiKey?.trim()

        var feedback = ""
        if (!apiKey.isNullOrBlank()) {
            try {
                val evalPrompt = """
                    You are an encouraging Socratic oral examiner on an audio walk review.
                    Question asked: "${question.question}"
                    Target Concept: "${question.expectedConcept}"
                    Student's spoken answer: "$studentAnswer"

                    Give a concise 1-2 sentence spoken reaction directly to the student. If they got it right, praise their insight. If they missed something, highlight the key missing link gently. Keep it conversational for audio listening.
                """.trimIndent()

                val messages = listOf(
                    AiMessage(
                        conversationId = "audio_eval",
                        role = AiMessageRole.USER,
                        content = evalPrompt
                    )
                )

                val feedbackBuilder = StringBuilder()
                aiProvider.generateStream(messages, null, config).collect { chunk ->
                    if (chunk is AiStreamChunk.Content) {
                        feedbackBuilder.append(chunk.delta)
                    }
                }
                feedback = feedbackBuilder.toString().trim()
            } catch (e: Exception) {
                feedback = ""
            }
        }

        if (feedback.isBlank()) {
            feedback = if (studentAnswer.length > 20) {
                "Spot on! Great articulation of the core principles. That's a solid explanation."
            } else {
                "Good start! Remember to connect this directly back to the underlying mechanism and why it matters."
            }
        }

        val updatedQuestions = _uiState.value.questions.toMutableList()
        val idx = _uiState.value.currentIndex
        if (idx in updatedQuestions.indices) {
            updatedQuestions[idx] = updatedQuestions[idx].copy(
                studentResponse = studentAnswer,
                feedback = feedback,
                isEvaluated = true
            )
        }

        _uiState.update {
            it.copy(
                questions = updatedQuestions,
                lastEvaluationFeedback = feedback,
                walkStatus = WalkStatus.SPEAKING_FEEDBACK
            )
        }
    }

    fun onFeedbackTtsFinished() {
        viewModelScope.launch {
            delay(2500L) // Brief pause after feedback before next question
            nextQuestion()
        }
    }

    fun nextQuestion() {
        val state = _uiState.value
        val nextIdx = state.currentIndex + 1
        if (nextIdx < state.questions.size) {
            _uiState.update {
                it.copy(
                    currentIndex = nextIdx,
                    walkStatus = WalkStatus.SPEAKING_QUESTION,
                    spokenTranscript = "",
                    lastEvaluationFeedback = ""
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    walkStatus = WalkStatus.FINISHED,
                    isTimerActive = false
                )
            }
            timerJob?.cancel()
        }
    }

    fun repeatCurrentQuestion() {
        _uiState.update {
            it.copy(
                walkStatus = WalkStatus.SPEAKING_QUESTION,
                spokenTranscript = ""
            )
        }
    }

    fun explainCurrentConcept() {
        val state = _uiState.value
        val currentQ = state.questions.getOrNull(state.currentIndex) ?: return
        val explanation = "Here is the key breakdown: ${currentQ.expectedConcept}. When tackling this, focus on how each piece connects to the whole."

        _uiState.update {
            it.copy(
                lastEvaluationFeedback = explanation,
                walkStatus = WalkStatus.SPEAKING_FEEDBACK
            )
        }
    }

    fun selectTab(tab: Int) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun setUploadedAudio(uri: Uri, fileName: String) {
        _uiState.update {
            it.copy(
                uploadedAudioUri = uri,
                uploadedAudioFileName = fileName,
                activeTab = 1
            )
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        _uiState.update { it.copy(playbackSpeed = speed) }
    }

    fun updateAudioPosition(pos: Int, duration: Int) {
        _uiState.update {
            it.copy(
                currentAudioPositionMs = pos,
                totalAudioDurationMs = duration
            )
        }
    }

    fun setAudioPlaying(playing: Boolean) {
        _uiState.update { it.copy(isAudioRevisionPlaying = playing) }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
