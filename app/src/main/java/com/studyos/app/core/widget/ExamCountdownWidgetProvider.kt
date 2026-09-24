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
import com.studyos.app.navigation.Screen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExamCountdownWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updateWidgets(context, appWidgetManager, appWidgetIds)
    }

    companion object {
        fun triggerUpdate(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
            val component = ComponentName(context, ExamCountdownWidgetProvider::class.java)
            val ids = appWidgetManager.getAppWidgetIds(component)
            if (ids.isNotEmpty()) {
                updateWidgets(context, appWidgetManager, ids)
            }
        }

        private fun updateWidgets(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
            CoroutineScope(Dispatchers.IO).launch {
                var examTitle = "Exam Radar"
                var examSubtitle = "No upcoming tests scheduled"
                var countdownBadge = "Tap to set an exam target"
                var targetRoute = Screen.Exams.route

                try {
                    val db = StudyOSDatabase.getDatabase(context)
                    val now = System.currentTimeMillis()
                    val upcoming = db.examDao().getUpcomingExamsOnce(now)

                    if (upcoming.isNotEmpty()) {
                        val exam = upcoming.first()
                        examTitle = exam.name
                        targetRoute = Screen.ExamDetail.createRoute(exam.id)

                        val daysRemaining = com.studyos.app.domain.model.calculateDaysRemaining(exam.date, now)
                        val diff = exam.date - now
                        val hours = (diff / (1000 * 60 * 60)).coerceAtLeast(0)

                        countdownBadge = when {
                            daysRemaining > 1L -> "🔥 $daysRemaining DAYS REMAINING"
                            daysRemaining == 1L -> "⚡ TOMORROW ($hours hrs left)"
                            daysRemaining == 0L && diff > 0 -> "⏱️ TODAY ($hours hrs left)"
                            daysRemaining == 0L -> "🚨 TODAY (IN PROGRESS)"
                            else -> "EXAM PASSED"
                        }

                        val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                        val target = if (exam.targetScore != null) " • Target ${exam.targetScore}%" else ""
                        examSubtitle = "${dateFormat.format(Date(exam.date))}$target"
                    }
                } catch (e: Exception) {
                    // Fallback to default copy
                }

                val openAppIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(MainActivity.EXTRA_ROUTE, targetRoute)
                }
                val pendingOpen = PendingIntent.getActivity(
                    context,
                    101,
                    openAppIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                for (appWidgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_exam_countdown).apply {
                        setTextViewText(R.id.widget_exam_title, examTitle)
                        setTextViewText(R.id.widget_exam_subtitle, examSubtitle)
                        setTextViewText(R.id.widget_exam_countdown_text, countdownBadge)
                        setOnClickPendingIntent(R.id.widget_exam_root, pendingOpen)
                        setOnClickPendingIntent(R.id.widget_exam_action_button, pendingOpen)
                    }
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }
        }
    }
}
