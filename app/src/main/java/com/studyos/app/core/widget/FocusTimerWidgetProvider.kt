package com.studyos.app.core.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.studyos.app.MainActivity
import com.studyos.app.R
import com.studyos.app.core.database.StudyOSDatabase
import com.studyos.app.core.service.FocusService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class FocusTimerWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updateWidgets(context, appWidgetManager, appWidgetIds)
    }

    companion object {
        fun triggerUpdate(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
            val component = ComponentName(context, FocusTimerWidgetProvider::class.java)
            val ids = appWidgetManager.getAppWidgetIds(component)
            if (ids.isNotEmpty()) {
                updateWidgets(context, appWidgetManager, ids)
            }
        }

        private fun updateWidgets(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
            CoroutineScope(Dispatchers.IO).launch {
                val activeSession = FocusService.activeSessionState.value

                var timeText = "25:00"
                var stateLabel = "READY"
                var subjectText = "Deep Focus Session"
                var quoteText = "One concept at a time."
                var actionButtonText = "Start"
                var isRunning = false

                if (activeSession != null) {
                    val displaySec = if (activeSession.isCountUp) activeSession.elapsedSeconds else activeSession.remainingSeconds
                    val m = displaySec / 60
                    val s = displaySec % 60
                    timeText = String.format(Locale.US, "%02d:%02d", m, s)
                    subjectText = activeSession.title
                    isRunning = activeSession.isRunning

                    if (isRunning) {
                        stateLabel = "ACTIVE"
                        quoteText = "In the flow — let the world wait."
                        actionButtonText = "Pause"
                    } else {
                        stateLabel = "PAUSED"
                        quoteText = "Resting. Take a gentle breath."
                        actionButtonText = "Resume"
                    }
                } else {
                    // Try to read nearest planned session or first subject
                    try {
                        val db = StudyOSDatabase.getDatabase(context)
                        val subjects = db.subjectDao().getAllSubjectsOnce()
                        if (subjects.isNotEmpty()) {
                            subjectText = subjects.first().name
                        }
                    } catch (_: Exception) {}
                }

                // Open timer screen
                val openTimerIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(MainActivity.EXTRA_ROUTE, "timer")
                }
                val pendingOpen = PendingIntent.getActivity(
                    context,
                    101,
                    openTimerIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                // Interactive action intent
                val actionPendingIntent: PendingIntent = if (activeSession != null) {
                    if (isRunning) {
                        val pauseIntent = Intent(context, FocusService::class.java).apply {
                            action = FocusService.ACTION_PAUSE
                        }
                        PendingIntent.getService(
                            context,
                            102,
                            pauseIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                    } else {
                        val resumeIntent = Intent(context, FocusService::class.java).apply {
                            action = FocusService.ACTION_RESUME
                        }
                        PendingIntent.getService(
                            context,
                            103,
                            resumeIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                    }
                } else {
                    val startIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra(MainActivity.EXTRA_ROUTE, "timer")
                        putExtra(MainActivity.EXTRA_AUTO_START_TIMER, true)
                    }
                    PendingIntent.getActivity(
                        context,
                        104,
                        startIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                }

                for (appWidgetId in appWidgetIds) {
                    try {
                        val views = RemoteViews(context.packageName, R.layout.widget_focus_timer).apply {
                            setTextViewText(R.id.widget_timer_time, timeText)
                            setTextViewText(R.id.widget_timer_state_label, stateLabel)
                            setTextViewText(R.id.widget_timer_subject, subjectText)
                            setTextViewText(R.id.widget_timer_quote, quoteText)
                            setTextViewText(R.id.widget_timer_action_button, actionButtonText)

                            setOnClickPendingIntent(R.id.widget_timer_root, pendingOpen)
                            setOnClickPendingIntent(R.id.widget_timer_action_button, actionPendingIntent)
                        }
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    } catch (t: Throwable) {
                        android.util.Log.e("FocusTimerWidget", "Error updating widget $appWidgetId", t)
                    }
                }
            }
        }
    }
}
