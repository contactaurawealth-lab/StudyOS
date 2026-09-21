package com.studyos.app.features.timer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyos.app.domain.model.Chapter
import com.studyos.app.domain.model.StudySession
import com.studyos.app.domain.model.StudySessionStatus
import com.studyos.app.domain.model.Subject
import com.studyos.app.domain.repository.ChapterRepository
import com.studyos.app.domain.repository.StudySessionRepository
import com.studyos.app.domain.repository.SubjectRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

enum class TimerMode {
    COUNTDOWN,
    COUNT_UP
}

enum class TimerStatus {
    IDLE,
    RUNNING,
    PAUSED,
    COMPLETED
}

data class StudyTimerUiState(
    val mode: TimerMode = TimerMode.COUNTDOWN,
    val status: TimerStatus = TimerStatus.IDLE,
    val totalDurationSeconds: Int = 25 * 60, // 25 min default Pomodoro
    val remainingSeconds: Int = 25 * 60,
    val elapsedSeconds: Int = 0,
    val subjects: List<Subject> = emptyList(),
    val chapters: List<Chapter> = emptyList(),
    val selectedSubject: Subject? = null,
    val selectedChapter: Chapter? = null,
    val sessionTitle: String = "Focused Study",
    val isSessionSaved: Boolean = false,
    val savedSessionMinutes: Int = 0
)

class StudyTimerViewModel(
    private val studySessionRepository: StudySessionRepository,
    private val subjectRepository: SubjectRepository,
    private val chapterRepository: ChapterRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudyTimerUiState())
    val uiState: StateFlow<StudyTimerUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var sessionStartTime: Long? = null

    init {
        loadSubjects()
    }

    private fun loadSubjects() {
        viewModelScope.launch {
            subjectRepository.observeSubjects().collect { subjects ->
                _uiState.update { it.copy(subjects = subjects) }
            }
        }
    }

    fun selectSubject(subject: Subject?) {
        _uiState.update { it.copy(selectedSubject = subject, selectedChapter = null, chapters = emptyList()) }
        if (subject != null) {
            viewModelScope.launch {
                chapterRepository.observeChaptersForSubject(subject.id).collect { chapters ->
                    _uiState.update { it.copy(chapters = chapters) }
                }
            }
        }
    }

    fun selectChapter(chapter: Chapter?) {
        _uiState.update { it.copy(selectedChapter = chapter) }
    }

    fun setTimerMode(mode: TimerMode) {
        if (_uiState.value.status == TimerStatus.RUNNING) return
        resetTimer()
        _uiState.update { it.copy(mode = mode) }
    }

    fun setPresetMinutes(minutes: Int) {
        if (_uiState.value.status == TimerStatus.RUNNING) return
        val totalSec = minutes * 60
        _uiState.update {
            it.copy(
                totalDurationSeconds = totalSec,
                remainingSeconds = totalSec,
                elapsedSeconds = 0,
                status = TimerStatus.IDLE,
                isSessionSaved = false
            )
        }
    }

    fun startTimer() {
        if (_uiState.value.status == TimerStatus.RUNNING) return
        if (sessionStartTime == null) {
            sessionStartTime = System.currentTimeMillis()
        }

        _uiState.update { it.copy(status = TimerStatus.RUNNING, isSessionSaved = false) }

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                val current = _uiState.value
                if (current.status != TimerStatus.RUNNING) break

                if (current.mode == TimerMode.COUNTDOWN) {
                    val newRemaining = (current.remainingSeconds - 1).coerceAtLeast(0)
                    val newElapsed = current.elapsedSeconds + 1
                    if (newRemaining <= 0) {
                        _uiState.update {
                            it.copy(
                                remainingSeconds = 0,
                                elapsedSeconds = newElapsed,
                                status = TimerStatus.COMPLETED
                            )
                        }
                        autoSaveSession()
                        break
                    } else {
                        _uiState.update {
                            it.copy(remainingSeconds = newRemaining, elapsedSeconds = newElapsed)
                        }
                    }
                } else {
                    // COUNT_UP
                    val newElapsed = current.elapsedSeconds + 1
                    _uiState.update {
                        it.copy(elapsedSeconds = newElapsed, remainingSeconds = newElapsed)
                    }
                }
            }
        }
    }

    fun pauseTimer() {
        timerJob?.cancel()
        timerJob = null
        _uiState.update { it.copy(status = TimerStatus.PAUSED) }
    }

    fun resumeTimer() {
        startTimer()
    }

    fun resetTimer() {
        timerJob?.cancel()
        timerJob = null
        sessionStartTime = null
        val duration = _uiState.value.totalDurationSeconds
        _uiState.update {
            it.copy(
                status = TimerStatus.IDLE,
                remainingSeconds = if (it.mode == TimerMode.COUNTDOWN) duration else 0,
                elapsedSeconds = 0,
                isSessionSaved = false
            )
        }
    }

    fun stopAndLogSession() {
        timerJob?.cancel()
        timerJob = null
        val elapsed = _uiState.value.elapsedSeconds
        _uiState.update { it.copy(status = TimerStatus.COMPLETED) }
        saveSessionToDatabase(elapsed)
    }

    private fun autoSaveSession() {
        val elapsed = _uiState.value.elapsedSeconds
        saveSessionToDatabase(elapsed)
    }

    private fun saveSessionToDatabase(elapsedSec: Int) {
        if (_uiState.value.isSessionSaved) return
        val minutes = (elapsedSec / 60).coerceAtLeast(1)
        val now = System.currentTimeMillis()
        val start = sessionStartTime ?: (now - elapsedSec * 1000L)
        val subject = _uiState.value.selectedSubject
        val chapter = _uiState.value.selectedChapter

        val title = when {
            chapter != null -> "Timer: ${chapter.name}"
            subject != null -> "Timer: ${subject.name}"
            else -> "Study Timer Session"
        }

        viewModelScope.launch {
            try {
                val session = StudySession(
                    id = UUID.randomUUID().toString(),
                    subjectId = subject?.id,
                    chapterId = chapter?.id,
                    title = title,
                    scheduledStart = start,
                    scheduledEnd = now,
                    plannedMinutes = (_uiState.value.totalDurationSeconds / 60).coerceAtLeast(1),
                    actualMinutes = minutes,
                    status = StudySessionStatus.COMPLETED,
                    createdAt = start,
                    updatedAt = now
                )
                studySessionRepository.createSession(session)
                _uiState.update {
                    it.copy(isSessionSaved = true, savedSessionMinutes = minutes)
                }
            } catch (e: Exception) {
                // Log failed but session state remains
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
