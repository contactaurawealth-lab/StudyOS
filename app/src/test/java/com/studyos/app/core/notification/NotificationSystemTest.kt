package com.studyos.app.core.notification

import com.studyos.app.domain.model.StudySession
import com.studyos.app.domain.model.StudySessionStatus
import com.studyos.app.navigation.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationSystemTest {

    @Test
    fun testDeepLinkRoutes() {
        val chapterRoute = Screen.ChapterDetail.createRoute("chap_123")
        assertEquals("chapter/chap_123", chapterRoute)

        val subjectRoute = Screen.SubjectDetail.createRoute("sub_456")
        assertEquals("subject/sub_456", subjectRoute)

        val sessionRoute = Screen.StudySession.createRoute("sess_789")
        assertEquals("study-session/sess_789", sessionRoute)

        val homeRoute = Screen.Today.route
        assertEquals("today", homeRoute)
    }

    @Test
    fun testStudySessionCreation() {
        val session = StudySession(
            id = "test_session_1",
            subjectId = "sub_math",
            chapterId = "chap_algebra",
            title = "Algebra Study",
            scheduledStart = System.currentTimeMillis() + 3600_000L,
            plannedMinutes = 45,
            status = StudySessionStatus.PLANNED
        )

        assertEquals("test_session_1", session.id)
        assertEquals(45, session.plannedMinutes)
        assertEquals(StudySessionStatus.PLANNED, session.status)
        assertTrue(session.scheduledStart!! > System.currentTimeMillis())
    }

    @Test
    fun testNotificationChannelConstants() {
        assertEquals("study_reminders", StudyOSNotificationManager.CHANNEL_ID)
        assertEquals("Study reminders", StudyOSNotificationManager.CHANNEL_NAME)
        assertEquals("StudyOS reminders and study notifications.", StudyOSNotificationManager.CHANNEL_DESCRIPTION)
    }

    @Test
    fun testAlarmSchedulerConstants() {
        assertEquals("com.studyos.app.ACTION_STUDY_REMINDER", AlarmScheduler.ACTION_STUDY_REMINDER)
        assertEquals(999_999, AlarmScheduler.DAILY_REMINDER_REQUEST_CODE)
    }
}
