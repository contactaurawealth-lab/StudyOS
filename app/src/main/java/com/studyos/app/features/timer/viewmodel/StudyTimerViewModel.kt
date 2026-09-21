package com.studyos.app.features.timer.viewmodel

import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyos.app.core.notification.StudyOSNotificationManager
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
import java.util.Calendar
import java.util.Locale
import java.util.UUID

enum class TimerMode {
    COUNTDOWN,
    COUNT_UP,
    POMODORO
}

enum class PomodoroPhase(val label: String, val durationMinutes: Int) {
    FOCUS("Focus Session", 25),
    SHORT_BREAK("Short Break", 5),
    LONG_BREAK("Long Break", 15)
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
    val pomodoroPhase: PomodoroPhase = PomodoroPhase.FOCUS,
    val completedPomodoros: Int = 0,
    val autoStartPomodoro: Boolean = false,
    val keepScreenAwake: Boolean = true,
    val showCustomDurationDialog: Boolean = false,
    val todaySubjectMinutes: Int = 0,
    val subjects: List<Subject> = emptyList(),
    val chapters: List<Chapter> = emptyList(),
    val selectedSubject: Subject? = null,
    val selectedChapter: Chapter? = null,
    val sessionTitle: String = "Focused Study",
    val isSessionSaved: Boolean = false,
    val savedSessionMinutes: Int = 0
)

class StudyTimerViewModel(
    private val context: Context,
    private val studySessionRepository: StudySessionRepository,
    private val subjectRepository: SubjectRepository,
    private val chapterRepository: ChapterRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudyTimerUiState())
    val uiState: StateFlow<StudyTimerUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var sessionStartTime: Long? = null
    private var targetEndRealtime: Long = 0L
    private var startRealtime: Long = 0L
    private var subjectMinutesJob: Job? = null

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
        refreshTodaySubjectMinutes(subject?.id)
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

    private fun refreshTodaySubjectMinutes(subjectId: String?) {
        subjectMinutesJob?.cancel()
        if (subjectId == null) {
            _uiState.update { it.copy(todaySubjectMinutes = 0) }
            return
        }
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val endOfDay = calendar.timeInMillis

        subjectMinutesJob = viewModelScope.launch {
            studySessionRepository.observeSessionsForDay(startOfDay, endOfDay).collect { sessions ->
                val totalMins = sessions.filter { it.subjectId == subjectId && it.status == StudySessionStatus.COMPLETED }
                    .sumOf { it.actualMinutes }
                _uiState.update { it.copy(todaySubjectMinutes = totalMins) }
            }
        }
    }

    fun setTimerMode(mode: TimerMode) {
        if (_uiState.value.status == TimerStatus.RUNNING) return
        resetTimer()
        val defaultSec = if (mode == TimerMode.POMODORO) 25 * 60 else _uiState.value.totalDurationSeconds
        _uiState.update {
            it.copy(
                mode = mode,
                pomodoroPhase = PomodoroPhase.FOCUS,
                totalDurationSeconds = defaultSec,
                remainingSeconds = if (mode == TimerMode.COUNT_UP) 0 else defaultSec
            )
        }
    }

    fun setPomodoroPhase(phase: PomodoroPhase) {
        if (_uiState.value.status == TimerStatus.RUNNING) return
        val totalSec = phase.durationMinutes * 60
        _uiState.update {
            it.copy(
                pomodoroPhase = phase,
                totalDurationSeconds = totalSec,
                remainingSeconds = totalSec,
                elapsedSeconds = 0,
                status = TimerStatus.IDLE
            )
        }
    }

    fun skipPomodoroPhase() {
        timerJob?.cancel()
        timerJob = null
        targetEndRealtime = 0L
        StudyOSNotificationManager.cancelTimerNotification(context)
        val current = _uiState.value
        val nextPhase = when (current.pomodoroPhase) {
            PomodoroPhase.FOCUS -> {
                val nextCount = current.completedPomodoros + 1
                if (nextCount % 4 == 0) PomodoroPhase.LONG_BREAK else PomodoroPhase.SHORT_BREAK
            }
            PomodoroPhase.SHORT_BREAK, PomodoroPhase.LONG_BREAK -> PomodoroPhase.FOCUS
        }
        val dur = nextPhase.durationMinutes * 60
        _uiState.update {
            it.copy(
                pomodoroPhase = nextPhase,
                totalDurationSeconds = dur,
                remainingSeconds = dur,
                elapsedSeconds = 0,
                status = TimerStatus.IDLE
            )
        }
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

    fun toggleAutoStartPomodoro() {
        _uiState.update { it.copy(autoStartPomodoro = !it.autoStartPomodoro) }
    }

    fun toggleKeepScreenAwake() {
        _uiState.update { it.copy(keepScreenAwake = !it.keepScreenAwake) }
    }

    fun showCustomDurationDialog(show: Boolean) {
        _uiState.update { it.copy(showCustomDurationDialog = show) }
    }

    fun setCustomDurationMinutes(minutes: Int) {
        if (_uiState.value.status == TimerStatus.RUNNING) return
        val clamped = minutes.coerceIn(1, 360)
        val totalSec = clamped * 60
        _uiState.update {
            it.copy(
                mode = TimerMode.COUNTDOWN,
                totalDurationSeconds = totalSec,
                remainingSeconds = totalSec,
                elapsedSeconds = 0,
                status = TimerStatus.IDLE,
                isSessionSaved = false,
                showCustomDurationDialog = false
            )
        }
    }

    fun startTimer() {
        if (_uiState.value.status == TimerStatus.RUNNING) return
        if (sessionStartTime == null) {
            sessionStartTime = System.currentTimeMillis()
        }

        val current = _uiState.value
        if (current.mode == TimerMode.COUNTDOWN || current.mode == TimerMode.POMODORO) {
            targetEndRealtime = SystemClock.elapsedRealtime() + (current.remainingSeconds * 1000L)
        } else {
            startRealtime = SystemClock.elapsedRealtime() - (current.elapsedSeconds * 1000L)
        }

        _uiState.update { it.copy(status = TimerStatus.RUNNING, isSessionSaved = false) }
        updateTimerNotification(isPaused = false)

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(500L)
                val state = _uiState.value
                if (state.status != TimerStatus.RUNNING) break

                if (state.mode == TimerMode.COUNTDOWN || state.mode == TimerMode.POMODORO) {
                    val nowRealtime = SystemClock.elapsedRealtime()
                    val remainingMillis = targetEndRealtime - nowRealtime
                    val newRemaining = ((remainingMillis + 999) / 1000).toInt().coerceAtLeast(0)
                    val totalSec = state.totalDurationSeconds
                    val newElapsed = (totalSec - newRemaining).coerceAtLeast(0)

                    if (newRemaining <= 0) {
                        handleTimerFinished()
                        break
                    } else if (newRemaining != state.remainingSeconds) {
                        _uiState.update {
                            it.copy(remainingSeconds = newRemaining, elapsedSeconds = newElapsed)
                        }
                        updateTimerNotification(isPaused = false)
                    }
                } else {
                    // COUNT_UP
                    val nowRealtime = SystemClock.elapsedRealtime()
                    val elapsedMillis = nowRealtime - startRealtime
                    val newElapsed = (elapsedMillis / 1000).toInt().coerceAtLeast(0)
                    if (newElapsed != state.elapsedSeconds) {
                        _uiState.update {
                            it.copy(elapsedSeconds = newElapsed, remainingSeconds = newElapsed)
                        }
                        updateTimerNotification(isPaused = false)
                    }
                }
            }
        }
    }

    private fun handleTimerFinished() {
        val current = _uiState.value
        StudyOSNotificationManager.playCompletionChimeAndVibrate(context)

        if (current.mode == TimerMode.POMODORO) {
            if (current.pomodoroPhase == PomodoroPhase.FOCUS) {
                autoSaveSession()
                val nextCount = current.completedPomodoros + 1
                val nextPhase = if (nextCount % 4 == 0) PomodoroPhase.LONG_BREAK else PomodoroPhase.SHORT_BREAK
                val dur = nextPhase.durationMinutes * 60

                StudyOSNotificationManager.showTimerCompletedNotification(
                    context,
                    "🍅 Pomodoro Focus Complete!",
                    "Great work! Time for a ${nextPhase.label} (${nextPhase.durationMinutes}m). Remember to hydrate and rest your eyes."
                )

                _uiState.update {
                    it.copy(
                        remainingSeconds = dur,
                        totalDurationSeconds = dur,
                        elapsedSeconds = 0,
                        pomodoroPhase = nextPhase,
                        completedPomodoros = nextCount,
                        status = TimerStatus.IDLE
                    )
                }

                if (current.autoStartPomodoro) {
                    startTimer()
                } else {
                    StudyOSNotificationManager.cancelTimerNotification(context)
                }
            } else {
                val nextPhase = PomodoroPhase.FOCUS
                val dur = nextPhase.durationMinutes * 60

                StudyOSNotificationManager.showTimerCompletedNotification(
                    context,
                    "🔔 Break Finished!",
                    "Ready to jump into your next focus block? Let's get to work!"
                )

                _uiState.update {
                    it.copy(
                        remainingSeconds = dur,
                        totalDurationSeconds = dur,
                        elapsedSeconds = 0,
                        pomodoroPhase = nextPhase,
                        status = TimerStatus.IDLE
                    )
                }

                if (current.autoStartPomodoro) {
                    startTimer()
                } else {
                    StudyOSNotificationManager.cancelTimerNotification(context)
                }
            }
        } else {
            // Standard COUNTDOWN completed
            _uiState.update {
                it.copy(
                    remainingSeconds = 0,
                    elapsedSeconds = it.totalDurationSeconds,
                    status = TimerStatus.COMPLETED
                )
            }
            autoSaveSession()
            StudyOSNotificationManager.showTimerCompletedNotification(
                context,
                "🎉 Study Session Complete!",
                "Fantastic focus session! ${current.totalDurationSeconds / 60} minutes recorded to your desk."
            )
            StudyOSNotificationManager.cancelTimerNotification(context)
        }
    }

    fun pauseTimer() {
        timerJob?.cancel()
        timerJob = null
        _uiState.update { it.copy(status = TimerStatus.PAUSED) }
        updateTimerNotification(isPaused = true)
    }

    fun resumeTimer() {
        startTimer()
    }

    fun resetTimer() {
        timerJob?.cancel()
        timerJob = null
        sessionStartTime = null
        targetEndRealtime = 0L
        startRealtime = 0L
        val duration = _uiState.value.totalDurationSeconds
        _uiState.update {
            it.copy(
                status = TimerStatus.IDLE,
                remainingSeconds = if (it.mode == TimerMode.COUNT_UP) 0 else duration,
                elapsedSeconds = 0,
                isSessionSaved = false
            )
        }
        StudyOSNotificationManager.cancelTimerNotification(context)
    }

    fun stopAndLogSession() {
        timerJob?.cancel()
        timerJob = null
        targetEndRealtime = 0L
        startRealtime = 0L
        val elapsed = _uiState.value.elapsedSeconds
        _uiState.update { it.copy(status = TimerStatus.COMPLETED) }
        StudyOSNotificationManager.cancelTimerNotification(context)
        saveSessionToDatabase(elapsed)
    }

    private fun updateTimerNotification(isPaused: Boolean = false) {
        val state = _uiState.value
        val displaySec = if (state.mode == TimerMode.COUNTDOWN || state.mode == TimerMode.POMODORO) {
            state.remainingSeconds
        } else {
            state.elapsedSeconds
        }
        val h = displaySec / 3600
        val m = (displaySec % 3600) / 60
        val s = displaySec % 60
        val timeFormatted = if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s)
            else String.format(Locale.US, "%02d:%02d", m, s)

        val title = when (state.mode) {
            TimerMode.POMODORO -> "🍅 ${state.pomodoroPhase.label}" + (state.selectedSubject?.let { " • ${it.name}" } ?: "")
            TimerMode.COUNTDOWN -> "⏱️ Focus Timer" + (state.selectedSubject?.let { " • ${it.name}" } ?: "")
            TimerMode.COUNT_UP -> "⏱️ Open Study" + (state.selectedSubject?.let { " • ${it.name}" } ?: "")
        }

        StudyOSNotificationManager.showOngoingTimerNotification(
            context = context,
            title = title,
            timeFormatted = timeFormatted,
            isPaused = isPaused
        )
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
                subject?.id?.let { refreshTodaySubjectMinutes(it) }
            } catch (e: Exception) {
                // Log failed but session state remains
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        StudyOSNotificationManager.cancelTimerNotification(context)
    }
}
