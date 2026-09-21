package com.studyos.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material.icons.outlined.Today
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    // Main primary destinations
    object Today : Screen("today")
    object Subjects : Screen("subjects")
    object Planner : Screen("planner")
    object Library : Screen("library")
    object Progress : Screen("progress")
    object Tasks : Screen("tasks")
    object Ai : Screen("ai?conversationId={conversationId}&chapterId={chapterId}&subjectId={subjectId}") {
        fun createRoute(conversationId: String? = null, chapterId: String? = null, subjectId: String? = null): String {
            val params = mutableListOf<String>()
            if (!conversationId.isNullOrBlank()) params.add("conversationId=$conversationId")
            if (!chapterId.isNullOrBlank()) params.add("chapterId=$chapterId")
            if (!subjectId.isNullOrBlank()) params.add("subjectId=$subjectId")
            return if (params.isNotEmpty()) "ai?${params.joinToString("&")}" else "ai"
        }
    }

    // Mobile "More" screen
    object More : Screen("more")

    // Academic hierarchy sub-routes
    object SubjectDetail : Screen("subject/{subjectId}") {
        fun createRoute(subjectId: String) = "subject/$subjectId"
    }
    object ChapterDetail : Screen("chapter/{chapterId}") {
        fun createRoute(chapterId: String) = "chapter/$chapterId"
    }
    object StudySession : Screen("study-session/{sessionId}") {
        fun createRoute(sessionId: String) = "study-session/$sessionId"
    }

    // Global Search
    object Search : Screen("search")

    // Onboarding sub-routes
    object OnboardingWelcome : Screen("onboarding/welcome")
    object OnboardingProfile : Screen("onboarding/profile")
    object OnboardingSubjects : Screen("onboarding/subjects")
    object OnboardingPreferences : Screen("onboarding/preferences")
    object OnboardingReview : Screen("onboarding/review")

    // Settings sub-routes
    object Settings : Screen("settings")
    object SettingsProfile : Screen("settings/profile")
    object SettingsSubjects : Screen("settings/subjects")
    object SettingsPreferences : Screen("settings/preferences")

    // Practice, Revision & Exam Prep destinations
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

    object QuizRunner : Screen("quiz/{quizId}") {
        fun createRoute(quizId: String) = "quiz/$quizId"
    }

    object MistakeBank : Screen("mistakes")

    object Exams : Screen("exams")

    object ExamDetail : Screen("exams/{examId}") {
        fun createRoute(examId: String) = "exams/$examId"
    }

    // Phase 11: Revision & Active Recall
    object RevisionDashboard : Screen("revision")

    object ActiveRecallRunner : Screen("recall/runner?sessionType={sessionType}&chapterId={chapterId}") {
        fun createRoute(sessionType: String = "DEEP_15", chapterId: String? = null): String {
            val params = mutableListOf("sessionType=$sessionType")
            if (!chapterId.isNullOrBlank()) params.add("chapterId=$chapterId")
            return "recall/runner?${params.joinToString("&")}"
        }
    }

    // Study Timer
    object StudyTimer : Screen("timer")

    // AI Study Session
    object AiStudySession : Screen("ai-study-session?subjectId={subjectId}&chapterId={chapterId}") {
        fun createRoute(subjectId: String? = null, chapterId: String? = null): String {
            val params = mutableListOf<String>()
            if (!subjectId.isNullOrBlank()) params.add("subjectId=$subjectId")
            if (!chapterId.isNullOrBlank()) params.add("chapterId=$chapterId")
            return if (params.isEmpty()) "ai-study-session" else "ai-study-session?${params.joinToString("&")}"
        }
    }

    // Notification Center
    object NotificationCenter : Screen("notifications")
}

data class TopLevelDestination(
    val screen: Screen,
    val title: String,
    val icon: ImageVector
)

val PhoneNavigationItems = listOf(
    TopLevelDestination(Screen.Today, "Today", Icons.Outlined.Today),
    TopLevelDestination(Screen.Subjects, "Subjects", Icons.Outlined.MenuBook),
    TopLevelDestination(Screen.Planner, "Planner", Icons.Outlined.CalendarMonth),
    TopLevelDestination(Screen.Library, "Library", Icons.Outlined.Folder),
    TopLevelDestination(Screen.More, "More", Icons.Outlined.MoreHoriz)
)

val TabletNavigationItems = listOf(
    TopLevelDestination(Screen.Today, "Today", Icons.Outlined.Today),
    TopLevelDestination(Screen.Subjects, "Subjects", Icons.Outlined.MenuBook),
    TopLevelDestination(Screen.Planner, "Planner", Icons.Outlined.CalendarMonth),
    TopLevelDestination(Screen.Library, "Library", Icons.Outlined.Folder),
    TopLevelDestination(Screen.Progress, "Progress", Icons.Outlined.ShowChart)
)
