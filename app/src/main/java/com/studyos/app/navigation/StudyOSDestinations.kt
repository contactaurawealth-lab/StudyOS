package com.studyos.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Today
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    // 5 Core Primary Destinations (Phase 5)
    object Today : Screen("today") // Home Dashboard
    object Subjects : Screen("subjects")
    object Planner : Screen("planner")
    object RevisionDashboard : Screen("revision")
    object Exams : Screen("exams")

    // Academic Hierarchy Sub-Routes
    object SubjectDetail : Screen("subject/{subjectId}") {
        fun createRoute(subjectId: String) = "subject/$subjectId"
    }
    object ChapterDetail : Screen("chapter/{chapterId}") {
        fun createRoute(chapterId: String) = "chapter/$chapterId"
    }
    object StudySession : Screen("study-session/{sessionId}") {
        fun createRoute(sessionId: String) = "study-session/$sessionId"
    }

    // Tasks & Progress Sub-Routes
    object Tasks : Screen("tasks")
    object Progress : Screen("progress")

    // Exam-Specific Modes (Phase 3 Features 6, 9, 10)
    object ExamDetail : Screen("exams/{examId}") {
        fun createRoute(examId: String) = "exams/$examId"
    }
    object ExamDayMode : Screen("exams/{examId}/exam-day") {
        fun createRoute(examId: String) = "exams/$examId" + "/exam-day"
    }
    object LastMinuteRevision : Screen("exams/{examId}/last-minute") {
        fun createRoute(examId: String) = "exams/$examId" + "/last-minute"
    }
    object MockTest : Screen("mock-test?subjectId={subjectId}") {
        fun createRoute(subjectId: String? = null): String {
            return if (!subjectId.isNullOrBlank()) "mock-test?subjectId=$subjectId" else "mock-test"
        }
    }

    // Practice, Notes & Revision Tools
    object ChapterPractice : Screen("practice/{chapterId}") {
        fun createRoute(chapterId: String) = "practice/$chapterId"
    }
    object NoteEditor : Screen("notes/edit?noteId={noteId}&subjectId={subjectId}&chapterId={chapterId}") {
        fun createRoute(noteId: String? = null, subjectId: String? = null, chapterId: String? = null): String {
            val params = mutableListOf<String>()
            if (!noteId.isNullOrBlank()) params.add("noteId=$noteId")
            if (!subjectId.isNullOrBlank()) params.add("subjectId=$subjectId")
            if (!chapterId.isNullOrBlank()) params.add("chapterId=$chapterId")
            return if (params.isNotEmpty()) "notes/edit?${params.joinToString("&")}" else "notes/edit"
        }
    }
    object FlashcardStudy : Screen("flashcards/study?chapterId={chapterId}&isDueOnly={isDueOnly}") {
        fun createRoute(chapterId: String? = null, isDueOnly: Boolean = false): String {
            val params = mutableListOf<String>()
            if (!chapterId.isNullOrBlank()) params.add("chapterId=$chapterId")
            if (isDueOnly) params.add("isDueOnly=true")
            return if (params.isNotEmpty()) "flashcards/study?${params.joinToString("&")}" else "flashcards/study"
        }
    }
    object MistakeBank : Screen("mistakes?subjectId={subjectId}") {
        fun createRoute(subjectId: String? = null): String {
            return if (!subjectId.isNullOrBlank()) "mistakes?subjectId=$subjectId" else "mistakes"
        }
    }
    object ActiveRecallRunner : Screen("recall/runner?sessionType={sessionType}&chapterId={chapterId}") {
        fun createRoute(sessionType: String = "DEEP_15", chapterId: String? = null): String {
            val params = mutableListOf("sessionType=$sessionType")
            if (!chapterId.isNullOrBlank()) params.add("chapterId=$chapterId")
            return "recall/runner?${params.joinToString("&")}"
        }
    }
    object QuizRunner : Screen("quiz/{quizId}") {
        fun createRoute(quizId: String) = "quiz/$quizId"
    }

    // Onboarding Sub-Routes
    object OnboardingWelcome : Screen("onboarding/welcome")
    object OnboardingProfile : Screen("onboarding/profile")
    object OnboardingSubjects : Screen("onboarding/subjects")
    object OnboardingPreferences : Screen("onboarding/preferences")
    object OnboardingReview : Screen("onboarding/review")

    // Settings Sub-Routes (accessible via top menu)
    object Settings : Screen("settings")
    object SettingsProfile : Screen("settings/profile")
    object SettingsSubjects : Screen("settings/subjects")
    object SettingsPreferences : Screen("settings/preferences")
}

data class TopLevelDestination(
    val screen: Screen,
    val title: String,
    val icon: ImageVector
)

// Clean 5-Item Exam-Focused Navigation
val MainNavigationItems = listOf(
    TopLevelDestination(Screen.Today, "Home", Icons.Outlined.Today),
    TopLevelDestination(Screen.Subjects, "Subjects", Icons.AutoMirrored.Outlined.MenuBook),
    TopLevelDestination(Screen.Planner, "Planner", Icons.Outlined.CalendarMonth),
    TopLevelDestination(Screen.RevisionDashboard, "Revision", Icons.Outlined.Refresh),
    TopLevelDestination(Screen.Exams, "Exams", Icons.Outlined.School)
)

val PhoneNavigationItems = MainNavigationItems
val TabletNavigationItems = MainNavigationItems
