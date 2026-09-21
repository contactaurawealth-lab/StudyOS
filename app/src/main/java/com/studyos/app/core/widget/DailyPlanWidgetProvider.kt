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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DailyPlanWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updateWidgets(context, appWidgetManager, appWidgetIds)
    }

    companion object {
        fun triggerUpdate(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
            val component = ComponentName(context, DailyPlanWidgetProvider::class.java)
            val ids = appWidgetManager.getAppWidgetIds(component)
            if (ids.isNotEmpty()) {
                updateWidgets(context, appWidgetManager, ids)
            }
        }

        private fun updateWidgets(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
            CoroutineScope(Dispatchers.IO).launch {
                var sessionTitle = "Today's Study Plan"
                var sessionSubtitle = "Tap to review your active goals"
                var streakText = "Daily Goal Active"

                try {
                    val db = StudyOSDatabase.getDatabase(context)
                    val now = System.currentTimeMillis()
                    val upcoming = db.studySessionDao().getUpcomingSessionsOnce(now)
                    if (upcoming.isNotEmpty()) {
                        val session = upcoming.first()
                        sessionTitle = session.title
                        sessionSubtitle = "${session.plannedMinutes} mins planned"
                    } else {
                        val subjects = db.subjectDao().getAllSubjectsOnce()
                        if (subjects.isNotEmpty()) {
                            val sub = subjects.first()
                            sessionTitle = "Focus: ${sub.name}"
                            sessionSubtitle = "Ready for focused session"
                        }
                    }
                } catch (e: Exception) {
                    // Fallback to default copy
                }

                val openAppIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(MainActivity.EXTRA_ROUTE, "today")
                }
                val pendingOpen = PendingIntent.getActivity(
                    context,
                    0,
                    openAppIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val startTimerIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(MainActivity.EXTRA_ROUTE, "timer")
                }
                val pendingTimer = PendingIntent.getActivity(
                    context,
                    1,
                    startTimerIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                for (appWidgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_daily_plan).apply {
                        setTextViewText(R.id.widget_title, sessionTitle)
                        setTextViewText(R.id.widget_subtitle, sessionSubtitle)
                        setTextViewText(R.id.widget_streak_or_time, streakText)
                        setOnClickPendingIntent(R.id.widget_root, pendingOpen)
                        setOnClickPendingIntent(R.id.widget_action_button, pendingTimer)
                    }
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }
        }
    }
}
