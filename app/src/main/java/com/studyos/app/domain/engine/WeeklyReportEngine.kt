package com.studyos.app.domain.engine

import com.studyos.app.domain.model.Mistake
import com.studyos.app.domain.model.StudySession
import com.studyos.app.domain.model.StudySessionStatus
import com.studyos.app.domain.model.Subject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class WeeklyStudyReport(
    val totalMinutes: Int,
    val activeDaysCount: Int,
    val completedSessionsCount: Int,
    val subjectBreakdown: List<Pair<String, Int>>,
    val mistakesResolved: Int,
    val headline: String,
    val shareableText: String
)

object WeeklyReportEngine {

    fun generateReport(
        sessions: List<StudySession>,
        subjects: List<Subject>,
        mistakes: List<Mistake>,
        now: Long = System.currentTimeMillis()
    ): WeeklyStudyReport {
        val sevenDaysAgo = now - (7L * 24 * 60 * 60 * 1000)
        val subjectMap = subjects.associateBy { it.id }
        val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val weekSessions = sessions.filter { session ->
            val start = session.scheduledStart ?: session.createdAt
            start in sevenDaysAgo..now && session.status == StudySessionStatus.COMPLETED
        }

        val totalMinutes = weekSessions.sumOf { it.actualMinutes }
        val activeDays = weekSessions.map {
            val start = it.scheduledStart ?: it.createdAt
            dayFormat.format(Date(start))
        }.distinct().size

        val subjectMinutesMap = mutableMapOf<String, Int>()
        weekSessions.forEach { s ->
            val subName = s.subjectId?.let { subjectMap[it]?.name } ?: "General Study"
            subjectMinutesMap[subName] = (subjectMinutesMap[subName] ?: 0) + s.actualMinutes
        }

        val subjectBreakdown = subjectMinutesMap.toList().sortedByDescending { it.second }

        val weekMistakesResolved = mistakes.count { m ->
            m.isResolved && m.lastMissedAt in sevenDaysAgo..now
        }

        val hours = totalMinutes / 60
        val remainingMins = totalMinutes % 60
        val timeDisplay = if (hours > 0) "${hours}h ${remainingMins}m" else "${remainingMins}m"

        val headline = when {
            totalMinutes >= 600 -> "Outstanding study week! ${timeDisplay} across $activeDays days."
            totalMinutes >= 180 -> "Solid momentum! ${timeDisplay} logged this week."
            totalMinutes > 0 -> "${timeDisplay} studied this week. Keep showing up!"
            else -> "Start your first study session of the week."
        }

        val topSubjectsText = if (subjectBreakdown.isNotEmpty()) {
            subjectBreakdown.take(3).joinToString(", ") { "${it.first} (${it.second / 60}h)" }
        } else {
            "No subjects recorded yet"
        }

        val shareableText = buildString {
            append("📊 StudyOS Weekly Report\n")
            append("⏱️ $timeDisplay focused study across $activeDays active days\n")
            if (subjectBreakdown.isNotEmpty()) {
                append("📚 Top subjects: $topSubjectsText\n")
            }
            if (weekMistakesResolved > 0) {
                append("🎯 $weekMistakesResolved mistakes reviewed & resolved\n")
            }
            append("🚀 Built offline-first with StudyOS!")
        }

        return WeeklyStudyReport(
            totalMinutes = totalMinutes,
            activeDaysCount = activeDays,
            completedSessionsCount = weekSessions.size,
            subjectBreakdown = subjectBreakdown,
            mistakesResolved = weekMistakesResolved,
            headline = headline,
            shareableText = shareableText
        )
    }
}
