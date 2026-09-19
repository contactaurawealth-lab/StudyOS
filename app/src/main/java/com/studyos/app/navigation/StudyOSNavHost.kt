package com.studyos.app.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.studyos.app.core.StudyOSAppContainer
import com.studyos.app.core.ui.component.StudyOSDivider
import com.studyos.app.core.ui.component.StudyOSIconButton
import com.studyos.app.features.library.ui.LibraryScreen
import com.studyos.app.features.onboarding.ui.OnboardingScreen
import com.studyos.app.features.onboarding.viewmodel.OnboardingViewModel
import com.studyos.app.features.planner.ui.PlannerScreen
import com.studyos.app.features.progress.ui.ProgressScreen
import com.studyos.app.features.search.ui.SearchScreen
import com.studyos.app.features.search.viewmodel.SearchViewModel
import com.studyos.app.features.settings.ui.EditPreferencesScreen
import com.studyos.app.features.settings.ui.EditProfileScreen
import com.studyos.app.features.settings.ui.ManageSubjectsScreen
import com.studyos.app.features.settings.ui.MoreScreen
import com.studyos.app.features.settings.ui.SettingsScreen
import com.studyos.app.features.settings.viewmodel.SettingsViewModel
import com.studyos.app.features.subjects.ui.SubjectsScreen
import com.studyos.app.features.subjects.viewmodel.SubjectsViewModel
import com.studyos.app.features.today.ui.TodayScreen
import com.studyos.app.features.today.viewmodel.TodayViewModel
import com.studyos.app.theme.StudyOSTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyOSApp(
    container: StudyOSAppContainer,
    isOnboardingCompleted: Boolean,
    onExitApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography

    val startDestination = if (isOnboardingCompleted) {
        Screen.Today.route
    } else {
        Screen.OnboardingWelcome.route
    }

    val isOnboardingRoute = currentRoute?.startsWith("onboarding") == true
    val isSearchRoute = currentRoute == Screen.Search.route
    val isSubSettingsRoute = currentRoute?.startsWith("settings/") == true

    val showShell = !isOnboardingRoute && !isSearchRoute

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isExpandedScreen = maxWidth >= 600.dp

        if (isExpandedScreen && showShell) {
            // Tablet / Large Screen Layout with Navigation Rail
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.background)
            ) {
                NavigationRail(
                    containerColor = colors.surface,
                    contentColor = colors.primaryText,
                    modifier = Modifier.border(width = 1.dp, color = colors.border),
                    header = {
                        Text(
                            text = "StudyOS",
                            style = typography.sectionTitle,
                            color = colors.primaryText,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                ) {
                    TabletNavigationItems.forEach { destination ->
                        val selected = currentRoute == destination.screen.route
                        NavigationRailItem(
                            selected = selected,
                            onClick = {
                                navigateToTopLevel(navController, destination.screen.route)
                            },
                            icon = {
                                Icon(
                                    imageVector = destination.icon,
                                    contentDescription = destination.title,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = destination.title,
                                    style = typography.caption
                                )
                            },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = colors.accent,
                                selectedTextColor = colors.accent,
                                indicatorColor = colors.background,
                                unselectedIconColor = colors.secondaryText,
                                unselectedTextColor = colors.secondaryText
                            )
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Secondary buttons in rail: Search and Settings
                    StudyOSIconButton(
                        onClick = { navController.navigate(Screen.Search.route) },
                        contentDescription = "Search"
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = colors.secondaryText,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    StudyOSIconButton(
                        onClick = { navController.navigate(Screen.Settings.route) },
                        contentDescription = "Settings"
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = null,
                            tint = colors.secondaryText,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                Box(modifier = Modifier.weight(1f)) {
                    StudyOSNavGraph(
                        navController = navController,
                        container = container,
                        startDestination = startDestination,
                        onExitApp = onExitApp
                    )
                }
            }
        } else {
            // Phone / Compact Screen Layout with Bottom Navigation
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = colors.background,
                topBar = {
                    if (showShell && !isSubSettingsRoute) {
                        TopAppBar(
                            title = {
                                Text(
                                    text = "StudyOS",
                                    style = typography.subsectionTitle,
                                    color = colors.primaryText
                                )
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = colors.background,
                                titleContentColor = colors.primaryText
                            ),
                            actions = {
                                StudyOSIconButton(
                                    onClick = { navController.navigate(Screen.Search.route) },
                                    contentDescription = "Search"
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Search,
                                        contentDescription = null,
                                        tint = colors.secondaryText,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        )
                    }
                },
                bottomBar = {
                    if (showShell && !isSubSettingsRoute) {
                        Column {
                            StudyOSDivider()
                            NavigationBar(
                                containerColor = colors.surface,
                                contentColor = colors.primaryText,
                                tonalElevation = 0.dp
                            ) {
                                PhoneNavigationItems.forEach { item ->
                                    val selected = currentRoute == item.screen.route
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = {
                                            navigateToTopLevel(navController, item.screen.route)
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = item.icon,
                                                contentDescription = item.title,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = item.title,
                                                style = typography.caption
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = colors.accent,
                                            selectedTextColor = colors.accent,
                                            indicatorColor = colors.background,
                                            unselectedIconColor = colors.secondaryText,
                                            unselectedTextColor = colors.secondaryText
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    StudyOSNavGraph(
                        navController = navController,
                        container = container,
                        startDestination = startDestination,
                        onExitApp = onExitApp
                    )
                }
            }
        }
    }
}

private fun navigateToTopLevel(navController: NavHostController, route: String) {
    navController.navigate(route) {
        popUpTo(navController.graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun StudyOSNavGraph(
    navController: NavHostController,
    container: StudyOSAppContainer,
    startDestination: String,
    onExitApp: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { fadeIn(animationSpec = tween(200)) },
        exitTransition = { fadeOut(animationSpec = tween(180)) },
        popEnterTransition = { fadeIn(animationSpec = tween(200)) },
        popExitTransition = { fadeOut(animationSpec = tween(180)) }
    ) {
        // Onboarding
        composable(Screen.OnboardingWelcome.route) {
            val onboardingViewModel = rememberOnboardingViewModel(container)
            OnboardingScreen(
                viewModel = onboardingViewModel,
                onFinishOnboarding = {
                    navController.navigate(Screen.Today.route) {
                        popUpTo(Screen.OnboardingWelcome.route) { inclusive = true }
                    }
                },
                onExitApp = onExitApp
            )
        }

        // Top-Level Destinations
        composable(Screen.Today.route) {
            val todayViewModel = rememberTodayViewModel(container)
            TodayScreen(viewModel = todayViewModel)
        }

        composable(Screen.Subjects.route) {
            val subjectsViewModel = rememberSubjectsViewModel(container)
            SubjectsScreen(viewModel = subjectsViewModel)
        }

        composable(Screen.Planner.route) {
            PlannerScreen()
        }

        composable(Screen.Library.route) {
            LibraryScreen()
        }

        composable(Screen.Progress.route) {
            ProgressScreen()
        }

        composable(Screen.More.route) {
            MoreScreen(
                onNavigateToSearch = { navController.navigate(Screen.Search.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToProgress = { navController.navigate(Screen.Progress.route) }
            )
        }

        // Search
        composable(Screen.Search.route) {
            val searchViewModel = rememberSearchViewModel(container)
            SearchScreen(
                viewModel = searchViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        // Settings
        composable(Screen.Settings.route) {
            val settingsViewModel = rememberSettingsViewModel(container)
            SettingsScreen(
                viewModel = settingsViewModel,
                onNavigateToProfile = { navController.navigate(Screen.SettingsProfile.route) },
                onNavigateToSubjects = { navController.navigate(Screen.SettingsSubjects.route) },
                onNavigateToPreferences = { navController.navigate(Screen.SettingsPreferences.route) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.SettingsProfile.route) {
            val settingsViewModel = rememberSettingsViewModel(container)
            EditProfileScreen(
                viewModel = settingsViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.SettingsSubjects.route) {
            val settingsViewModel = rememberSettingsViewModel(container)
            ManageSubjectsScreen(
                viewModel = settingsViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.SettingsPreferences.route) {
            val settingsViewModel = rememberSettingsViewModel(container)
            EditPreferencesScreen(
                viewModel = settingsViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

// ViewModel creation helpers
@Composable
private fun rememberOnboardingViewModel(container: StudyOSAppContainer): OnboardingViewModel {
    return androidx.lifecycle.viewmodel.compose.viewModel {
        OnboardingViewModel(
            saveStudentUseCase = container.saveStudentUseCase,
            saveSubjectsUseCase = container.saveSubjectsUseCase,
            addSubjectUseCase = container.addSubjectUseCase,
            saveStudyPreferencesUseCase = container.saveStudyPreferencesUseCase,
            preferencesDataSource = container.preferencesDataSource
        )
    }
}

@Composable
private fun rememberTodayViewModel(container: StudyOSAppContainer): TodayViewModel {
    return androidx.lifecycle.viewmodel.compose.viewModel {
        TodayViewModel(
            getStudentUseCase = container.getStudentUseCase,
            getStudyPreferencesUseCase = container.getStudyPreferencesUseCase
        )
    }
}

@Composable
private fun rememberSubjectsViewModel(container: StudyOSAppContainer): SubjectsViewModel {
    return androidx.lifecycle.viewmodel.compose.viewModel {
        SubjectsViewModel(
            getSubjectsUseCase = container.getSubjectsUseCase,
            addSubjectUseCase = container.addSubjectUseCase,
            renameSubjectUseCase = container.renameSubjectUseCase,
            deleteSubjectUseCase = container.deleteSubjectUseCase
        )
    }
}

@Composable
private fun rememberSearchViewModel(container: StudyOSAppContainer): SearchViewModel {
    return androidx.lifecycle.viewmodel.compose.viewModel {
        SearchViewModel(
            getSubjectsUseCase = container.getSubjectsUseCase
        )
    }
}

@Composable
private fun rememberSettingsViewModel(container: StudyOSAppContainer): SettingsViewModel {
    return androidx.lifecycle.viewmodel.compose.viewModel {
        SettingsViewModel(
            getStudentUseCase = container.getStudentUseCase,
            saveStudentUseCase = container.saveStudentUseCase,
            getSubjectsUseCase = container.getSubjectsUseCase,
            addSubjectUseCase = container.addSubjectUseCase,
            renameSubjectUseCase = container.renameSubjectUseCase,
            deleteSubjectUseCase = container.deleteSubjectUseCase,
            getStudyPreferencesUseCase = container.getStudyPreferencesUseCase,
            saveStudyPreferencesUseCase = container.saveStudyPreferencesUseCase,
            preferencesDataSource = container.preferencesDataSource
        )
    }
}
