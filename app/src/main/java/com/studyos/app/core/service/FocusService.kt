package com.studyos.app.core.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import com.studyos.app.StudyOSApplication
import com.studyos.app.core.notification.StudyOSNotificationManager
import com.studyos.app.core.widget.DailyPlanWidgetProvider
import com.studyos.app.core.widget.FocusTimerWidgetProvider
import com.studyos.app.domain.model.StudySession
import com.studyos.app.domain.model.StudySessionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

/**
 * Hardened Foreground Service for Study Focus Timer with:
 * - Air-gapped offline-first execution
 * - Periodic 60-second SQLite checkpointing to prevent lost study minutes
 * - onTaskRemoved hook to persist sessions on swipe-to-kill
 * - Foreground notification sync
 */
class FocusService : Service() {

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var timerJob: Job? = null
    private var checkpointJob: Job? = null

    inner class LocalBinder : Binder() {
        fun getService(): FocusService = this@FocusService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    companion object {
        const val ACTION_START = "com.studyos.app.service.focus.START"
        const val ACTION_PAUSE = "com.studyos.app.service.focus.PAUSE"
        const val ACTION_RESUME = "com.studyos.app.service.focus.RESUME"
        const val ACTION_STOP = "com.studyos.app.service.focus.STOP"

        const val EXTRA_SUBJECT_ID = "extra_subject_id"
        const val EXTRA_CHAPTER_ID = "extra_chapter_id"
        const val EXTRA_SESSION_TITLE = "extra_session_title"
        const val EXTRA_TOTAL_SECONDS = "extra_total_seconds"
        const val EXTRA_REMAINING_SECONDS = "extra_remaining_seconds"
        const val EXTRA_ELAPSED_SECONDS = "extra_elapsed_seconds"
        const val EXTRA_IS_COUNT_UP = "extra_is_count_up"

        private val _activeSessionState = MutableStateFlow<FocusSessionData?>(null)
        val activeSessionState: StateFlow<FocusSessionData?> = _activeSessionState.asStateFlow()

        fun startFocusService(
            context: Context,
            subjectId: String?,
            chapterId: String?,
            sessionTitle: String,
            totalSeconds: Int,
            remainingSeconds: Int,
            elapsedSeconds: Int,
            isCountUp: Boolean = false
        ) {
            val intent = Intent(context, FocusService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_SUBJECT_ID, subjectId)
                putExtra(EXTRA_CHAPTER_ID, chapterId)
                putExtra(EXTRA_SESSION_TITLE, sessionTitle)
                putExtra(EXTRA_TOTAL_SECONDS, totalSeconds)
                putExtra(EXTRA_REMAINING_SECONDS, remainingSeconds)
                putExtra(EXTRA_ELAPSED_SECONDS, elapsedSeconds)
                putExtra(EXTRA_IS_COUNT_UP, isCountUp)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun pauseFocusService(context: Context) {
            val intent = Intent(context, FocusService::class.java).apply { action = ACTION_PAUSE }
            context.startService(intent)
        }

        fun resumeFocusService(context: Context) {
            val intent = Intent(context, FocusService::class.java).apply { action = ACTION_RESUME }
            context.startService(intent)
        }

        fun stopFocusService(context: Context) {
            val intent = Intent(context, FocusService::class.java).apply { action = ACTION_STOP }
            context.startService(intent)
        }
    }

    data class FocusSessionData(
        val sessionId: String,
        val subjectId: String?,
        val chapterId: String?,
        val title: String,
        val totalSeconds: Int,
        val remainingSeconds: Int,
        val elapsedSeconds: Int,
        val isRunning: Boolean,
        val isCountUp: Boolean,
        val startTimestamp: Long,
        val lastCheckpointTimestamp: Long
    )

    private var currentData: FocusSessionData? = null
    private var targetEndRealtime: Long = 0L
    private var startRealtime: Long = 0L

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> handleStart(intent)
            ACTION_PAUSE -> handlePause()
            ACTION_RESUME -> handleResume()
            ACTION_STOP -> handleStop()
        }
        return START_STICKY
    }

    private fun handleStart(intent: Intent) {
        val subjectId = intent.getStringExtra(EXTRA_SUBJECT_ID)
        val chapterId = intent.getStringExtra(EXTRA_CHAPTER_ID)
        val title = intent.getStringExtra(EXTRA_SESSION_TITLE) ?: "Focused Study"
        val totalSeconds = intent.getIntExtra(EXTRA_TOTAL_SECONDS, 25 * 60)
        val remaining = intent.getIntExtra(EXTRA_REMAINING_SECONDS, totalSeconds)
        val elapsed = intent.getIntExtra(EXTRA_ELAPSED_SECONDS, 0)
        val isCountUp = intent.getBooleanExtra(EXTRA_IS_COUNT_UP, false)

        val now = System.currentTimeMillis()
        val data = FocusSessionData(
            sessionId = currentData?.sessionId ?: UUID.randomUUID().toString(),
            subjectId = subjectId,
            chapterId = chapterId,
            title = title,
            totalSeconds = totalSeconds,
            remainingSeconds = remaining,
            elapsedSeconds = elapsed,
            isRunning = true,
            isCountUp = isCountUp,
            startTimestamp = currentData?.startTimestamp ?: (now - elapsed * 1000L),
            lastCheckpointTimestamp = now
        )
        currentData = data
        _activeSessionState.value = data

        if (isCountUp) {
            startRealtime = SystemClock.elapsedRealtime() - (elapsed * 1000L)
        } else {
            targetEndRealtime = SystemClock.elapsedRealtime() + (remaining * 1000L)
        }

        promoteToForeground()
        startTimerLoop()
        startPeriodicCheckpointing()
    }

    private fun handlePause() {
        timerJob?.cancel()
        timerJob = null
        val data = currentData?.copy(isRunning = false) ?: return
        currentData = data
        _activeSessionState.value = data
        updateNotification(isPaused = true)
        persistCheckpoint(data)
    }

    private fun handleResume() {
        val data = currentData ?: return
        if (data.isCountUp) {
            startRealtime = SystemClock.elapsedRealtime() - (data.elapsedSeconds * 1000L)
        } else {
            targetEndRealtime = SystemClock.elapsedRealtime() + (data.remainingSeconds * 1000L)
        }
        val runningData = data.copy(isRunning = true)
        currentData = runningData
        _activeSessionState.value = runningData
        promoteToForeground()
        startTimerLoop()
    }

    private fun handleStop() {
        currentData?.let { data ->
            persistFinalSession(data)
        }
        cleanupAndStop()
    }

    private fun promoteToForeground() {
        val notif = StudyOSNotificationManager.buildOngoingTimerNotification(
            context = this,
            title = currentData?.title ?: "⏱️ Focus Session",
            timeFormatted = formatTime(currentData?.remainingSeconds ?: 0),
            isPaused = false
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                StudyOSNotificationManager.TIMER_NOTIFICATION_ID,
                notif,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(StudyOSNotificationManager.TIMER_NOTIFICATION_ID, notif)
        }
    }

    private fun updateNotification(isPaused: Boolean) {
        val data = currentData ?: return
        val displaySec = if (data.isCountUp) data.elapsedSeconds else data.remainingSeconds
        StudyOSNotificationManager.showOngoingTimerNotification(
            context = this,
            title = data.title,
            timeFormatted = formatTime(displaySec),
            isPaused = isPaused
        )
        // Refresh home screen widgets
        DailyPlanWidgetProvider.triggerUpdate(this)
        FocusTimerWidgetProvider.triggerUpdate(this)
    }

    private fun formatTime(seconds: Int): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s)
        else String.format(Locale.US, "%02d:%02d", m, s)
    }

    private fun startTimerLoop() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (true) {
                delay(1000L)
                val data = currentData ?: break
                if (!data.isRunning) break

                if (data.isCountUp) {
                    val nowRealtime = SystemClock.elapsedRealtime()
                    val newElapsed = ((nowRealtime - startRealtime) / 1000).toInt().coerceAtLeast(0)
                    val updated = data.copy(elapsedSeconds = newElapsed, remainingSeconds = newElapsed)
                    currentData = updated
                    _activeSessionState.value = updated
                    updateNotification(isPaused = false)
                } else {
                    val nowRealtime = SystemClock.elapsedRealtime()
                    val remainingMillis = targetEndRealtime - nowRealtime
                    val newRemaining = ((remainingMillis + 999) / 1000).toInt().coerceAtLeast(0)
                    val newElapsed = (data.totalSeconds - newRemaining).coerceAtLeast(0)

                    if (newRemaining <= 0) {
                        val completed = data.copy(remainingSeconds = 0, elapsedSeconds = data.totalSeconds, isRunning = false)
                        currentData = completed
                        _activeSessionState.value = completed
                        persistFinalSession(completed)
                        StudyOSNotificationManager.playCompletionChimeAndVibrate(this@FocusService)
                        StudyOSNotificationManager.showTimerCompletedNotification(
                            this@FocusService,
                            "🎉 Study Session Complete!",
                            "Great work! Recorded ${data.totalSeconds / 60} minutes to your desk."
                        )
                        cleanupAndStop()
                        break
                    } else {
                        val updated = data.copy(remainingSeconds = newRemaining, elapsedSeconds = newElapsed)
                        currentData = updated
                        _activeSessionState.value = updated
                        updateNotification(isPaused = false)
                    }
                }
            }
        }
    }

    /**
     * Periodic 60-second checkpoint to SQLite to prevent loss of study minutes
     * if the OS terminates the application.
     */
    private fun startPeriodicCheckpointing() {
        checkpointJob?.cancel()
        checkpointJob = serviceScope.launch {
            while (true) {
                delay(60_000L)
                currentData?.let { data ->
                    if (data.isRunning && data.elapsedSeconds >= 60) {
                        persistCheckpoint(data)
                    }
                }
            }
        }
    }

    private fun persistCheckpoint(data: FocusSessionData) {
        val app = application as? StudyOSApplication ?: return
        val db = app.container.database
        val elapsedMins = (data.elapsedSeconds / 60).coerceAtLeast(1)
        val now = System.currentTimeMillis()

        serviceScope.launch {
            try {
                val session = StudySession(
                    id = data.sessionId,
                    subjectId = data.subjectId,
                    chapterId = data.chapterId,
                    title = data.title,
                    scheduledStart = data.startTimestamp,
                    scheduledEnd = now,
                    plannedMinutes = (data.totalSeconds / 60).coerceAtLeast(1),
                    actualMinutes = elapsedMins,
                    status = StudySessionStatus.IN_PROGRESS,
                    createdAt = data.startTimestamp,
                    updatedAt = now
                )
                db.studySessionDao().insert(
                    com.studyos.app.core.database.entity.StudySessionEntity(
                        id = session.id,
                        subjectId = session.subjectId,
                        chapterId = session.chapterId,
                        title = session.title,
                        scheduledStart = session.scheduledStart,
                        scheduledEnd = session.scheduledEnd,
                        plannedMinutes = session.plannedMinutes,
                        actualMinutes = session.actualMinutes,
                        status = session.status.name,
                        createdAt = session.createdAt,
                        updatedAt = session.updatedAt
                    )
                )
            } catch (_: Exception) {}
        }
    }

    private fun persistFinalSession(data: FocusSessionData) {
        val app = application as? StudyOSApplication ?: return
        val db = app.container.database
        val elapsedMins = (data.elapsedSeconds / 60).coerceAtLeast(1)
        val now = System.currentTimeMillis()

        serviceScope.launch {
            try {
                db.studySessionDao().insert(
                    com.studyos.app.core.database.entity.StudySessionEntity(
                        id = data.sessionId,
                        subjectId = data.subjectId,
                        chapterId = data.chapterId,
                        title = data.title,
                        scheduledStart = data.startTimestamp,
                        scheduledEnd = now,
                        plannedMinutes = (data.totalSeconds / 60).coerceAtLeast(1),
                        actualMinutes = elapsedMins,
                        status = StudySessionStatus.COMPLETED.name,
                        createdAt = data.startTimestamp,
                        updatedAt = now
                    )
                )
                DailyPlanWidgetProvider.triggerUpdate(this@FocusService)
                FocusTimerWidgetProvider.triggerUpdate(this@FocusService)
            } catch (_: Exception) {}
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // Hardened resilience hook: When app is swiped away from Recents, finalize active progress!
        currentData?.let { data ->
            if (data.elapsedSeconds >= 30) {
                persistFinalSession(data)
            }
        }
        cleanupAndStop()
    }

    private fun cleanupAndStop() {
        timerJob?.cancel()
        timerJob = null
        checkpointJob?.cancel()
        checkpointJob = null
        currentData = null
        _activeSessionState.value = null
        StudyOSNotificationManager.cancelTimerNotification(this)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        cleanupAndStop()
    }
}
