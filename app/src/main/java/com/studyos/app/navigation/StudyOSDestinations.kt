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

    // Mobile "More" screen
    object More : Screen("more")

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
