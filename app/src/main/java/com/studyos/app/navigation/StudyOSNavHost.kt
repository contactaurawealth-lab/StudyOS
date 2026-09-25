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
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.studyos.app.features.ai.ui.AiAssistantScreen
import com.studyos.app.features.ai.viewmodel.AiAssistantViewModel
import com.studyos.app.features.timer.ui.StudyTimerScreen
import com.studyos.app.features.timer.viewmodel.StudyTimerViewModel
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.studyos.app.core.ui.component.DrawerNavigationItem
import com.studyos.app.core.ui.component.GlassBottomBar
import com.studyos.app.core.ui.component.GlassDialog
import com.studyos.app.core.ui.component.GlassNavigationItem
import com.studyos.app.core.ui.component.StudyOSNavigationDrawer
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
import com.studyos.app.features.planner.viewmodel.PlannerViewModel
import com.studyos.app.features.progress.ui.ProgressScreen
import com.studyos.app.features.progress.viewmodel.ProgressViewModel
import com.studyos.app.features.search.ui.SearchScreen
import com.studyos.app.features.search.viewmodel.SearchViewModel
import com.studyos.app.features.tasks.ui.TasksScreen
import com.studyos.app.features.tasks.viewmodel.TasksViewModel
import com.studyos.app.features.settings.ui.EditPreferencesScreen
import com.studyos.app.features.settings.ui.EditProfileScreen
import com.studyos.app.features.settings.ui.ManageSubjectsScreen
import com.studyos.app.features.settings.ui.MoreScreen
import com.studyos.app.features.settings.ui.SettingsScreen
import com.studyos.app.features.settings.viewmodel.SettingsViewModel
import com.studyos.app.features.subjects.ui.ChapterDetailScreen
import com.studyos.app.features.subjects.ui.SubjectDetailScreen
import com.studyos.app.features.subjects.ui.SubjectsScreen
import com.studyos.app.features.subjects.viewmodel.ChapterViewModel
import com.studyos.app.features.subjects.viewmodel.SubjectDetailViewModel
import com.studyos.app.features.subjects.viewmodel.SubjectsViewModel
import com.studyos.app.features.today.ui.StudySessionPlaceholderScreen
import com.studyos.app.features.today.ui.TodayScreen
import com.studyos.app.features.today.viewmodel.StudySessionViewModel
import com.studyos.app.features.today.viewmodel.TodayViewModel
import com.studyos.app.features.practice.ui.ChapterPracticeHubScreen
import com.studyos.app.features.practice.ui.FlashcardStudyScreen
import com.studyos.app.features.practice.ui.MistakeBankScreen
import com.studyos.app.features.practice.ui.NoteEditorScreen
import com.studyos.app.features.practice.ui.QuizRunnerScreen
import com.studyos.app.features.practice.viewmodel.ChapterPracticeHubViewModel
import com.studyos.app.features.practice.viewmodel.FlashcardStudyViewModel
import com.studyos.app.features.practice.viewmodel.MistakeBankViewModel
import com.studyos.app.features.practice.viewmodel.NoteEditorViewModel
import com.studyos.app.features.practice.viewmodel.QuizRunnerViewModel
import com.studyos.app.features.exams.ui.ExamDetailScreen
import com.studyos.app.features.exams.ui.ExamListScreen
import com.studyos.app.features.exams.viewmodel.ExamViewModel
import com.studyos.app.features.practice.ui.RevisionDashboardScreen
import com.studyos.app.features.practice.ui.ActiveRecallRunnerScreen
import com.studyos.app.features.practice.viewmodel.RevisionDashboardViewModel
import com.studyos.app.features.practice.viewmodel.ActiveRecallRunnerViewModel
import com.studyos.app.features.practice.audiowalk.FeynmanAudioWalkScreen
import com.studyos.app.features.practice.audiowalk.FeynmanAudioWalkViewModel
import com.studyos.app.domain.model.ActiveRecallSessionType
import com.studyos.app.features.document.ui.DocumentViewerScreen
import com.studyos.app.features.document.viewmodel.DocumentViewerViewModel
import com.studyos.app.theme.StudyOSTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyOSApp(
    container: StudyOSAppContainer,
    isOnboardingCompleted: Boolean,
    onExitApp: () -> Unit,
    initialRoute: String? = null,
    onRouteConsumed: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    var isDrawerOpen by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(initialRoute) {
        if (!initialRoute.isNullOrBlank()) {
            try {
                navController.navigate(initialRoute) {
                    launchSingleTop = true
                }
            } catch (e: Exception) {
                // If route is invalid, stay on current destination
            } finally {
                onRouteConsumed()
            }
        }
    }

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
    val isSubjectDetailRoute = currentRoute?.startsWith("subject/") == true
    val isChapterDetailRoute = currentRoute?.startsWith("chapter/") == true
    val isStudySessionRoute = currentRoute?.startsWith("study-session/") == true
    val isTasksRoute = currentRoute == Screen.Tasks.route
    val isAiRoute = currentRoute?.startsWith("ai") == true
    val isPracticeRoute = currentRoute?.startsWith("practice/") == true ||
        currentRoute?.startsWith("notes/") == true ||
        currentRoute?.startsWith("flashcards/") == true ||
        currentRoute?.startsWith("quiz/") == true ||
        currentRoute?.startsWith("recall/") == true ||
        currentRoute == Screen.Exams.route ||
        currentRoute?.startsWith("exams/") == true
    val isSubRoute = isSubSettingsRoute || isSubjectDetailRoute || isChapterDetailRoute ||
        isStudySessionRoute || isTasksRoute || isAiRoute || isPracticeRoute

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

                    // Secondary buttons in rail: AI Assistant, Search and Settings
                    StudyOSIconButton(
                        onClick = { navController.navigate(Screen.Ai.createRoute()) },
                        contentDescription = "AI Assistant"
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Psychology,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(20.dp)
                        )
                    }

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
                        onExitApp = onExitApp,
                        onOpenDrawer = { isDrawerOpen = true }
                    )
                }
            }
        } else {
            // Phone / Compact Screen Layout with Glass Bottom Navigation
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = colors.background,
                bottomBar = {
                    if (showShell && !isSubRoute) {
                        GlassBottomBar(
                            items = PhoneBottomNavItems,
                            currentRoute = currentRoute,
                            onNavigate = { route ->
                                navigateToTopLevel(navController, route)
                            }
                        )
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
                        onExitApp = onExitApp,
                        onOpenDrawer = { isDrawerOpen = true }
                    )
                }
            }
        }

        if (showAboutDialog) {
            GlassDialog(
                onDismissRequest = { showAboutDialog = false },
                title = "StudyOS",
                message = "StudyOS (Glassmorphic Edition)\n\nA minimal, offline-first study workspace designed to help you plan, learn, revise, and excel.",
                confirmButtonText = "Close",
                onConfirm = { showAboutDialog = false },
                dismissButtonText = null
            )
        }

        StudyOSNavigationDrawer(
            isOpen = isDrawerOpen,
            onClose = { isDrawerOpen = false },
            currentRoute = currentRoute,
            onNavigate = { route ->
                navigateToTopLevel(navController, route)
            },
            primaryItems = PrimaryDrawerItems,
            secondaryItems = SecondaryDrawerItems,
            onAboutClick = {
                isDrawerOpen = false
                showAboutDialog = true
            }
        )
    }
}

private val PhoneBottomNavItems = listOf(
    GlassNavigationItem(Screen.Today.route, "Today", Icons.Outlined.Today),
    GlassNavigationItem(Screen.Subjects.route, "Subjects", Icons.AutoMirrored.Outlined.MenuBook),
    GlassNavigationItem(Screen.Progress.route, "Progress", Icons.AutoMirrored.Outlined.ShowChart),
    GlassNavigationItem(Screen.Planner.route, "Calendar", Icons.Outlined.CalendarMonth)
)

private val PrimaryDrawerItems = listOf(
    DrawerNavigationItem("Today", Screen.Today.route, Icons.Outlined.Today),
    DrawerNavigationItem("Subjects", Screen.Subjects.route, Icons.AutoMirrored.Outlined.MenuBook),
    DrawerNavigationItem("Library", Screen.Library.route, Icons.Outlined.Folder),
    DrawerNavigationItem("Study Timer", Screen.StudyTimer.route, Icons.Outlined.Timer),
    DrawerNavigationItem("Progress", Screen.Progress.route, Icons.AutoMirrored.Outlined.ShowChart),
    DrawerNavigationItem("Calendar", Screen.Planner.route, Icons.Outlined.CalendarMonth),
    DrawerNavigationItem("Revision", Screen.RevisionDashboard.route, Icons.Outlined.Psychology)
)

private val SecondaryDrawerItems = listOf(
    DrawerNavigationItem("Notifications", Screen.NotificationCenter.route, Icons.Outlined.Notifications),
    DrawerNavigationItem("AI Assistant", Screen.Ai.createRoute(), Icons.Outlined.Psychology),
    DrawerNavigationItem("Tasks", Screen.Tasks.route, Icons.Outlined.CheckCircle),
    DrawerNavigationItem("Exams", Screen.Exams.route, Icons.Outlined.School),
    DrawerNavigationItem("Settings", Screen.Settings.route, Icons.Outlined.Settings)
)

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
    onExitApp: () -> Unit,
    onOpenDrawer: () -> Unit = {}
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
            TodayScreen(
                viewModel = todayViewModel,
                onOpenSession = { sessionId ->
                    navController.navigate(Screen.StudySession.createRoute(sessionId))
                },
                onOpenChapter = { chapterId ->
                    navController.navigate(Screen.ChapterDetail.createRoute(chapterId))
                },
                onOpenTasks = {
                    navController.navigate(Screen.Tasks.route)
                },
                onOpenAi = {
                    navController.navigate(Screen.Ai.createRoute())
                },
                onOpenExams = {
                    navController.navigate(Screen.Exams.route)
                },
                onOpenFlashcards = {
                    navController.navigate(Screen.FlashcardStudy.createRoute(isDueOnly = true))
                },
                onOpenRevision = {
                    navController.navigate(Screen.RevisionDashboard.route)
                },
                onOpenTimer = {
                    navController.navigate(Screen.StudyTimer.route)
                },
                onStartAiSession = { subjectId, chapterId ->
                    navController.navigate(Screen.AiStudySession.createRoute(subjectId, chapterId))
                },
                onOpenDrawer = onOpenDrawer,
                onOpenSearch = {
                    navController.navigate(Screen.Search.route)
                },
                onOpenNotifications = {
                    navController.navigate(Screen.NotificationCenter.route)
                },
                onOpenSubjects = {
                    navController.navigate(Screen.Subjects.route)
                },
                onResetComplete = {
                    navController.navigate(Screen.OnboardingWelcome.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.NotificationCenter.route) {
            val notificationVm = rememberNotificationCenterViewModel(container)
            com.studyos.app.features.notifications.ui.NotificationCenterScreen(
                viewModel = notificationVm,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.AiStudySession.route) { backStackEntry ->
            val subjectId = backStackEntry.arguments?.getString("subjectId")
            val chapterId = backStackEntry.arguments?.getString("chapterId")
            val sessionVm = androidx.compose.runtime.remember(subjectId, chapterId) {
                com.studyos.app.features.practice.viewmodel.AiStudySessionViewModel(
                    initialSubjectId = subjectId,
                    initialChapterId = chapterId,
                    subjectRepository = container.subjectRepository,
                    chapterRepository = container.chapterRepository,
                    recallRepository = container.recallRepository,
                    studySessionRepository = container.studySessionRepository
                )
            }
            com.studyos.app.features.practice.ui.AiStudySessionScreen(
                viewModel = sessionVm,
                onFinish = { navController.popBackStack() }
            )
        }

        composable(Screen.StudyTimer.route) {
            StudyTimerScreen(
                viewModel = container.studyTimerViewModel,
                onBack = { navController.popBackStack() },
                onOpenDrawer = onOpenDrawer
            )
        }

        composable(Screen.StudySession.route) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
            val sessionViewModel = rememberStudySessionViewModel(container, sessionId)
            StudySessionPlaceholderScreen(
                viewModel = sessionViewModel,
                onBack = { navController.popBackStack() },
                onOpenChapter = { chapterId ->
                    navController.navigate(Screen.ChapterDetail.createRoute(chapterId))
                },
                onOpenTimer = {
                    navController.navigate(Screen.StudyTimer.route)
                }
            )
        }

        composable(Screen.Subjects.route) {
            val subjectsViewModel = rememberSubjectsViewModel(container)
            SubjectsScreen(
                viewModel = subjectsViewModel,
                onSubjectClick = { subjectId ->
                    navController.navigate(Screen.SubjectDetail.createRoute(subjectId))
                },
                onStartAiSession = { subjectId, chapterId ->
                    navController.navigate(Screen.AiStudySession.createRoute(subjectId, chapterId))
                },
                onStartRecall = { subjectId ->
                    navController.navigate(Screen.RevisionDashboard.route)
                }
            )
        }

        composable(Screen.SubjectDetail.route) { backStackEntry ->
            val subjectId = backStackEntry.arguments?.getString("subjectId") ?: ""
            val subjectDetailViewModel = rememberSubjectDetailViewModel(container, subjectId)
            SubjectDetailScreen(
                viewModel = subjectDetailViewModel,
                onChapterClick = { chapterId ->
                    navController.navigate(Screen.ChapterDetail.createRoute(chapterId))
                },
                onNoteClick = { noteId ->
                    navController.navigate(Screen.NoteEditor.createRoute(noteId = noteId, subjectId = subjectId))
                },
                onCreateNote = {
                    navController.navigate(Screen.NoteEditor.createRoute(subjectId = subjectId))
                },
                onStartAudioWalk = {
                    navController.navigate(Screen.AudioWalk.createRoute(subjectId = subjectId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.ChapterDetail.route) { backStackEntry ->
            val chapterId = backStackEntry.arguments?.getString("chapterId") ?: ""
            val chapterViewModel = rememberChapterViewModel(container, chapterId)
            ChapterDetailScreen(
                viewModel = chapterViewModel,
                onBack = { navController.popBackStack() },
                onAskAi = { targetChapterId ->
                    navController.navigate(Screen.Ai.createRoute(chapterId = targetChapterId))
                },
                onOpenPractice = { targetChapterId ->
                    navController.navigate(Screen.ChapterPractice.createRoute(targetChapterId))
                }
            )
        }

        composable(Screen.Planner.route) {
            val plannerViewModel = rememberPlannerViewModel(container)
            PlannerScreen(
                viewModel = plannerViewModel
            )
        }

        composable(Screen.Tasks.route) {
            val tasksViewModel = rememberTasksViewModel(container)
            TasksScreen(
                viewModel = tasksViewModel,
                onBack = { navController.popBackStack() },
                onStartTimer = { _, _ ->
                    navController.navigate(Screen.StudyTimer.route)
                }
            )
        }

        composable(Screen.Library.route) {
            val libraryViewModel = rememberLibraryViewModel(container)
            LibraryScreen(
                viewModel = libraryViewModel,
                onOpenNote = { noteId, subjectId, chapterId ->
                    navController.navigate(Screen.NoteEditor.createRoute(noteId, subjectId, chapterId))
                },
                onStudyDeck = { chapterId ->
                    navController.navigate(Screen.FlashcardStudy.createRoute(chapterId))
                },
                onOpenSubject = { subjectId ->
                    navController.navigate(Screen.SubjectDetail.createRoute(subjectId))
                },
                onOpenDrawer = onOpenDrawer,
                onOpenDocumentViewer = { uri, noteId, title ->
                    navController.navigate(Screen.DocumentViewer.createRoute(documentUri = uri, noteId = noteId, title = title))
                }
            )
        }

        composable(Screen.Progress.route) {
            val progressViewModel = rememberProgressViewModel(container)
            ProgressScreen(
                viewModel = progressViewModel,
                onSubjectClick = { subjectId ->
                    navController.navigate(Screen.SubjectDetail.createRoute(subjectId))
                }
            )
        }

        composable(Screen.More.route) {
            MoreScreen(
                onNavigateToTasks = { navController.navigate(Screen.Tasks.route) },
                onNavigateToSearch = { navController.navigate(Screen.Search.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToProgress = { navController.navigate(Screen.Progress.route) },
                onNavigateToAi = { navController.navigate(Screen.Ai.createRoute()) },
                onNavigateToExams = { navController.navigate(Screen.Exams.route) },
                onNavigateToMistakes = { navController.navigate(Screen.MistakeBank.route) },
                onNavigateToRevision = { navController.navigate(Screen.RevisionDashboard.route) }
            )
        }

        // AI Assistant
        composable(
            route = Screen.Ai.route,
            arguments = listOf(
                navArgument("conversationId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("chapterId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("subjectId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getString("conversationId")
            val chapterId = backStackEntry.arguments?.getString("chapterId")
            val subjectId = backStackEntry.arguments?.getString("subjectId")
            val aiViewModel = rememberAiAssistantViewModel(
                container = container,
                initialConversationId = conversationId,
                initialChapterId = chapterId,
                initialSubjectId = subjectId
            )
            AiAssistantScreen(
                viewModel = aiViewModel,
                onBack = { navController.popBackStack() },
                onOpenChapter = { targetChapterId ->
                    navController.navigate(Screen.ChapterDetail.createRoute(targetChapterId))
                }
            )
        }

        // Search
        composable(Screen.Search.route) {
            val searchViewModel = rememberSearchViewModel(container)
            SearchScreen(
                viewModel = searchViewModel,
                onBack = { navController.popBackStack() },
                onSubjectClick = { subjectId ->
                    navController.navigate(Screen.SubjectDetail.createRoute(subjectId))
                },
                onChapterClick = { chapterId ->
                    navController.navigate(Screen.ChapterDetail.createRoute(chapterId))
                },
                onNoteClick = { noteId, subjectId, chapterId ->
                    navController.navigate(
                        Screen.NoteEditor.createRoute(
                            noteId = noteId,
                            subjectId = subjectId.ifBlank { null },
                            chapterId = chapterId.ifBlank { null }
                        )
                    )
                },
                onFlashcardClick = { chapterId ->
                    if (chapterId.isNotBlank()) {
                        navController.navigate(Screen.FlashcardStudy.createRoute(chapterId))
                    }
                },
                onMistakeClick = {
                    navController.navigate(Screen.MistakeBank.route)
                },
                onExamClick = { examId ->
                    navController.navigate(Screen.ExamDetail.createRoute(examId))
                }
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
                onBack = { navController.popBackStack() },
                onResetComplete = {
                    navController.navigate(Screen.OnboardingWelcome.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
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

        // Practice, Revision & Exam Prep Routes
        composable(Screen.ChapterPractice.route) { backStackEntry ->
            val chapterId = backStackEntry.arguments?.getString("chapterId") ?: ""
            val practiceViewModel = rememberChapterPracticeHubViewModel(container, chapterId)
            ChapterPracticeHubScreen(
                viewModel = practiceViewModel,
                onBack = { navController.popBackStack() },
                onOpenNote = { noteId, chapId ->
                    navController.navigate(Screen.NoteEditor.createRoute(noteId = noteId, chapterId = chapId))
                },
                onStartFlashcards = { chapId ->
                    navController.navigate(Screen.FlashcardStudy.createRoute(chapterId = chapId))
                },
                onStartQuiz = { quizId ->
                    navController.navigate(Screen.QuizRunner.createRoute(quizId))
                },
                onOpenMistakeBank = {
                    navController.navigate(Screen.MistakeBank.route)
                },
                onStartAudioWalk = {
                    navController.navigate(Screen.AudioWalk.createRoute(chapterId = chapterId))
                }
            )
        }

        composable(
            route = Screen.NoteEditor.route,
            arguments = listOf(
                navArgument("noteId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("subjectId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("chapterId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getString("noteId")
            val subjectId = backStackEntry.arguments?.getString("subjectId")
            val chapterId = backStackEntry.arguments?.getString("chapterId")
            val noteViewModel = rememberNoteEditorViewModel(container, noteId, subjectId, chapterId)
            NoteEditorScreen(
                viewModel = noteViewModel,
                onBack = { navController.popBackStack() },
                onOpenQuiz = { quizId ->
                    navController.navigate(Screen.QuizRunner.createRoute(quizId))
                },
                onOpenDocumentViewer = { nId, title ->
                    navController.navigate(Screen.DocumentViewer.createRoute(noteId = nId, title = title))
                }
            )
        }

        composable(
            route = Screen.DocumentViewer.route,
            arguments = listOf(
                navArgument("documentUri") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("noteId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("title") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val documentUriEncoded = backStackEntry.arguments?.getString("documentUri")
            val documentUri = documentUriEncoded?.let {
                try {
                    java.net.URLDecoder.decode(it, "UTF-8")
                } catch (_: Exception) {
                    it
                }
            }
            val noteId = backStackEntry.arguments?.getString("noteId")
            val titleEncoded = backStackEntry.arguments?.getString("title")
            val title = titleEncoded?.let {
                try {
                    java.net.URLDecoder.decode(it, "UTF-8")
                } catch (_: Exception) {
                    it
                }
            }

            val docViewModel = rememberDocumentViewerViewModel(
                container = container,
                documentUri = documentUri,
                noteId = noteId,
                title = title
            )

            DocumentViewerScreen(
                viewModel = docViewModel,
                onBack = { navController.popBackStack() },
                onOpenNoteEditor = { newNoteId ->
                    navController.navigate(Screen.NoteEditor.createRoute(noteId = newNoteId))
                },
                onOpenFlashcardStudy = { chapterId ->
                    navController.navigate(Screen.FlashcardStudy.createRoute(chapterId = chapterId))
                },
                onOpenQuiz = { quizId ->
                    navController.navigate(Screen.QuizRunner.createRoute(quizId))
                }
            )
        }

        composable(
            route = Screen.FlashcardStudy.route,
            arguments = listOf(
                navArgument("chapterId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("isDueOnly") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->
            val chapterId = backStackEntry.arguments?.getString("chapterId")
            val isDueOnly = backStackEntry.arguments?.getBoolean("isDueOnly") ?: false
            val flashcardViewModel = rememberFlashcardStudyViewModel(container, chapterId, isDueOnly)
            FlashcardStudyScreen(
                viewModel = flashcardViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.QuizRunner.route) { backStackEntry ->
            val quizId = backStackEntry.arguments?.getString("quizId") ?: ""
            val quizViewModel = rememberQuizRunnerViewModel(container, quizId)
            QuizRunnerScreen(
                viewModel = quizViewModel,
                onBack = { navController.popBackStack() },
                onOpenMistakes = {
                    navController.navigate(Screen.MistakeBank.route)
                }
            )
        }

        composable(Screen.MistakeBank.route) {
            val mistakeViewModel = rememberMistakeBankViewModel(container)
            MistakeBankScreen(
                viewModel = mistakeViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Exams.route) {
            val examViewModel = rememberExamViewModel(container)
            ExamListScreen(
                viewModel = examViewModel,
                onBack = { navController.popBackStack() },
                onOpenExamDetail = { examId ->
                    navController.navigate(Screen.ExamDetail.createRoute(examId))
                }
            )
        }

        composable(Screen.ExamDetail.route) { backStackEntry ->
            val examId = backStackEntry.arguments?.getString("examId") ?: ""
            // Share the same ExamViewModel with the ExamListScreen to keep data in sync
            val parentEntry = try {
                navController.getBackStackEntry(Screen.Exams.route)
            } catch (_: Exception) {
                backStackEntry
            }
            val examViewModel = rememberExamViewModel(container, parentEntry)
            ExamDetailScreen(
                examId = examId,
                viewModel = examViewModel,
                onBack = { navController.popBackStack() },
                onOpenChapterPractice = { chapterId ->
                    navController.navigate(Screen.ChapterPractice.createRoute(chapterId))
                }
            )
        }

        composable(Screen.RevisionDashboard.route) {
            val revViewModel = rememberRevisionDashboardViewModel(container)
            RevisionDashboardScreen(
                viewModel = revViewModel,
                onBack = { navController.popBackStack() },
                onStartRecallSession = { sessionType, chapterId ->
                    navController.navigate(Screen.ActiveRecallRunner.createRoute(sessionType.name, chapterId))
                },
                onOpenChapter = { chapterId ->
                    navController.navigate(Screen.ChapterDetail.createRoute(chapterId))
                }
            )
        }

        composable(
            route = Screen.ActiveRecallRunner.route,
            arguments = listOf(
                navArgument("sessionType") {
                    type = NavType.StringType
                    defaultValue = "DEEP_15"
                },
                navArgument("chapterId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val sessionTypeStr = backStackEntry.arguments?.getString("sessionType") ?: "DEEP_15"
            val chapterId = backStackEntry.arguments?.getString("chapterId")
            val recallViewModel = rememberActiveRecallRunnerViewModel(container, sessionTypeStr, chapterId)
            ActiveRecallRunnerScreen(
                viewModel = recallViewModel,
                onFinish = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.AudioWalk.route,
            arguments = listOf(
                navArgument("subjectId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("chapterId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val subjectId = backStackEntry.arguments?.getString("subjectId")
            val chapterId = backStackEntry.arguments?.getString("chapterId")
            val audioWalkViewModel = rememberFeynmanAudioWalkViewModel(container, subjectId, chapterId)
            FeynmanAudioWalkScreen(
                viewModel = audioWalkViewModel,
                onNavigateBack = { navController.popBackStack() }
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
            getTodayDataUseCase = container.getTodayDataUseCase,
            planSessionUseCase = container.planSessionUseCase,
            deleteSessionUseCase = container.deleteSessionUseCase,
            updateSessionStatusUseCase = container.updateSessionStatusUseCase,
            getSubjectsUseCase = container.getSubjectsUseCase,
            getChaptersForSubjectUseCase = container.getChaptersForSubjectUseCase,
            getTodayTasksUseCase = container.getTodayTasksUseCase,
            toggleTaskCompletionUseCase = container.toggleTaskCompletionUseCase,
            getDueFlashcardsUseCase = container.getDueFlashcardsUseCase,
            getExamsUseCase = container.getExamsUseCase,
            getSmartStudyRecommendationUseCase = container.getSmartStudyRecommendationUseCase,
            getRecallDashboardUseCase = container.getRecallDashboardUseCase,
            getDailyAiPlanUseCase = container.getDailyAiPlanUseCase,
            getOverallExamReadinessUseCase = container.getOverallExamReadinessUseCase,
            alarmScheduler = container.alarmScheduler,
            preferencesDataSource = container.preferencesDataSource,
            notificationDao = container.database.notificationDao(),
            database = container.database
        )
    }
}

@Composable
private fun rememberNotificationCenterViewModel(
    container: StudyOSAppContainer
): com.studyos.app.features.notifications.viewmodel.NotificationCenterViewModel {
    return androidx.lifecycle.viewmodel.compose.viewModel {
        com.studyos.app.features.notifications.viewmodel.NotificationCenterViewModel(
            notificationDao = container.database.notificationDao()
        )
    }
}

@Composable
private fun rememberLibraryViewModel(
    container: StudyOSAppContainer
): com.studyos.app.features.library.viewmodel.LibraryViewModel {
    return androidx.lifecycle.viewmodel.compose.viewModel {
        com.studyos.app.features.library.viewmodel.LibraryViewModel(
            noteDao = container.database.noteDao(),
            flashcardDao = container.database.flashcardDao(),
            resourceDao = container.database.resourceDao(),
            subjectDao = container.database.subjectDao(),
            chapterDao = container.database.chapterDao()
        )
    }
}

@Composable
private fun rememberPlannerViewModel(container: StudyOSAppContainer): PlannerViewModel {
    return androidx.lifecycle.viewmodel.compose.viewModel {
        PlannerViewModel(
            getWeekScheduleUseCase = container.getWeekScheduleUseCase,
            getUpcomingScheduleUseCase = container.getUpcomingScheduleUseCase,
            checkSessionOverlapUseCase = container.checkSessionOverlapUseCase,
            moveSessionUseCase = container.moveSessionUseCase,
            savePlannerSessionUseCase = container.savePlannerSessionUseCase,
            deletePlannerSessionUseCase = container.deletePlannerSessionUseCase,
            getSubjectsUseCase = container.getSubjectsUseCase,
            getChaptersForSubjectUseCase = container.getChaptersForSubjectUseCase,
            getStudyPreferencesUseCase = container.getStudyPreferencesUseCase,
            alarmScheduler = container.alarmScheduler,
            preferencesDataSource = container.preferencesDataSource,
            chapterRepository = container.chapterRepository
        )
    }
}

@Composable
private fun rememberTasksViewModel(container: StudyOSAppContainer): TasksViewModel {
    return androidx.lifecycle.viewmodel.compose.viewModel {
        TasksViewModel(
            getTasksUseCase = container.getTasksUseCase,
            createTaskUseCase = container.createTaskUseCase,
            updateTaskUseCase = container.updateTaskUseCase,
            toggleTaskCompletionUseCase = container.toggleTaskCompletionUseCase,
            deleteTaskUseCase = container.deleteTaskUseCase,
            getSubjectsUseCase = container.getSubjectsUseCase,
            getChaptersForSubjectUseCase = container.getChaptersForSubjectUseCase,
            alarmScheduler = container.alarmScheduler
        )
    }
}

@Composable
private fun rememberStudySessionViewModel(container: StudyOSAppContainer, sessionId: String): StudySessionViewModel {
    return androidx.lifecycle.viewmodel.compose.viewModel(
        key = "StudySessionViewModel_$sessionId"
    ) {
        StudySessionViewModel(
            sessionId = sessionId,
            studySessionRepository = container.studySessionRepository,
            subjectRepository = container.subjectRepository,
            chapterRepository = container.chapterRepository,
            deleteSessionUseCase = container.deleteSessionUseCase
        )
    }
}

@Composable
private fun rememberSubjectsViewModel(container: StudyOSAppContainer): SubjectsViewModel {
    return androidx.lifecycle.viewmodel.compose.viewModel {
        SubjectsViewModel(
            getSubjectsWithProgressUseCase = container.getSubjectsWithProgressUseCase,
            addSubjectUseCase = container.addSubjectUseCase,
            renameSubjectUseCase = container.renameSubjectUseCase,
            deleteSubjectUseCase = container.deleteSubjectUseCase,
            loadSampleDataUseCase = container.loadSampleDataUseCase,
            getSubjectReadinessUseCase = container.getSubjectReadinessUseCase
        )
    }
}

@Composable
private fun rememberSubjectDetailViewModel(container: StudyOSAppContainer, subjectId: String): SubjectDetailViewModel {
    return androidx.lifecycle.viewmodel.compose.viewModel(
        key = "SubjectDetailViewModel_$subjectId"
    ) {
        SubjectDetailViewModel(
            subjectId = subjectId,
            getSubjectByIdUseCase = container.getSubjectByIdUseCase,
            getChaptersForSubjectUseCase = container.getChaptersForSubjectUseCase,
            addChapterUseCase = container.addChapterUseCase,
            deleteChapterUseCase = container.deleteChapterUseCase,
            moveChapterUseCase = container.moveChapterUseCase,
            renameSubjectUseCase = container.renameSubjectUseCase,
            deleteSubjectUseCase = container.deleteSubjectUseCase,
            noteRepository = container.noteRepository
        )
    }
}

@Composable
private fun rememberChapterViewModel(container: StudyOSAppContainer, chapterId: String): ChapterViewModel {
    return androidx.lifecycle.viewmodel.compose.viewModel(
        key = "ChapterViewModel_$chapterId"
    ) {
        ChapterViewModel(
            chapterId = chapterId,
            getChapterUseCase = container.getChapterUseCase,
            getSubjectByIdUseCase = container.getSubjectByIdUseCase,
            updateChapterUseCase = container.updateChapterUseCase,
            updateChapterProgressUseCase = container.updateChapterProgressUseCase,
            updateChapterStatusUseCase = container.updateChapterStatusUseCase,
            deleteChapterUseCase = container.deleteChapterUseCase,
            recordChapterOpenedUseCase = container.recordChapterOpenedUseCase,
            getChapterIntelligenceUseCase = container.getChapterIntelligenceUseCase
        )
    }
}

@Composable
private fun rememberProgressViewModel(container: StudyOSAppContainer): ProgressViewModel {
    return androidx.lifecycle.viewmodel.compose.viewModel {
        ProgressViewModel(
            getAcademicProgressUseCase = container.getAcademicProgressUseCase,
            studySessionRepository = container.studySessionRepository,
            subjectRepository = container.subjectRepository,
            mistakeRepository = container.mistakeRepository,
            recallRepository = container.recallRepository,
            quizRepository = container.quizRepository
        )
    }
}

@Composable
private fun rememberSearchViewModel(container: StudyOSAppContainer): SearchViewModel {
    return androidx.lifecycle.viewmodel.compose.viewModel {
        SearchViewModel(
            getSubjectsUseCase = container.getSubjectsUseCase,
            chapterRepository = container.chapterRepository,
            noteRepository = container.noteRepository,
            flashcardRepository = container.flashcardRepository,
            mistakeRepository = container.mistakeRepository,
            recallRepository = container.recallRepository,
            examRepository = container.examRepository
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
            preferencesDataSource = container.preferencesDataSource,
            alarmScheduler = container.alarmScheduler,
            database = container.database
        )
    }
}

@Composable
private fun rememberAiAssistantViewModel(
    container: StudyOSAppContainer,
    initialConversationId: String?,
    initialChapterId: String?,
    initialSubjectId: String?
): AiAssistantViewModel {
    val key = "AiAssistantViewModel_${initialConversationId}_${initialChapterId}_${initialSubjectId}"
    return androidx.lifecycle.viewmodel.compose.viewModel(key = key) {
        AiAssistantViewModel(
            initialConversationId = initialConversationId,
            initialChapterId = initialChapterId,
            initialSubjectId = initialSubjectId,
            getConversationsUseCase = container.getConversationsUseCase,
            getConversationUseCase = container.getConversationUseCase,
            getMessagesUseCase = container.getMessagesUseCase,
            createConversationUseCase = container.createConversationUseCase,
            deleteConversationUseCase = container.deleteConversationUseCase,
            getStudyContextUseCase = container.getStudyContextUseCase,
            sendAiMessageUseCase = container.sendAiMessageUseCase,
            saveAiConfigUseCase = container.saveAiConfigUseCase,
            getAiConfigUseCase = container.getAiConfigUseCase,
            getChapterAiContextUseCase = container.getChapterAiContextUseCase,
            aiStudyEngineUseCase = container.aiStudyEngineUseCase,
            noteDao = container.database.noteDao(),
            flashcardDao = container.database.flashcardDao()
        )
    }
}

@Composable
private fun rememberChapterPracticeHubViewModel(
    container: StudyOSAppContainer,
    chapterId: String
): ChapterPracticeHubViewModel {
    val key = "ChapterPracticeHubViewModel_$chapterId"
    return androidx.lifecycle.viewmodel.compose.viewModel(key = key) {
        ChapterPracticeHubViewModel(
            chapterId = chapterId,
            getChapterPracticeSummaryUseCase = container.getChapterPracticeSummaryUseCase,
            getNotesForChapterUseCase = container.getNotesForChapterUseCase,
            getFlashcardsForChapterUseCase = container.getFlashcardsForChapterUseCase,
            getMistakesForChapterUseCase = container.getMistakesForChapterUseCase,
            saveFlashcardUseCase = container.saveFlashcardUseCase,
            deleteFlashcardUseCase = container.deleteFlashcardUseCase,
            deleteNoteUseCase = container.deleteNoteUseCase,
            toggleNotePinUseCase = container.toggleNotePinUseCase,
            resolveMistakeUseCase = container.resolveMistakeUseCase,
            convertMistakeToFlashcardUseCase = container.convertMistakeToFlashcardUseCase,
            saveQuizUseCase = container.saveQuizUseCase,
            aiPracticeToolsUseCase = container.aiPracticeToolsUseCase,
            getAiConfigUseCase = container.getAiConfigUseCase
        )
    }
}

@Composable
private fun rememberNoteEditorViewModel(
    container: StudyOSAppContainer,
    noteId: String?,
    subjectId: String?,
    chapterId: String?
): NoteEditorViewModel {
    val key = "NoteEditorViewModel_${noteId}_${subjectId}_${chapterId}"
    return androidx.lifecycle.viewmodel.compose.viewModel(key = key) {
        NoteEditorViewModel(
            noteId = noteId,
            initialSubjectId = subjectId,
            initialChapterId = chapterId,
            getNoteUseCase = container.getNoteUseCase,
            saveNoteUseCase = container.saveNoteUseCase,
            deleteNoteUseCase = container.deleteNoteUseCase,
            saveFlashcardUseCase = container.saveFlashcardUseCase,
            saveQuizUseCase = container.saveQuizUseCase,
            aiPracticeToolsUseCase = container.aiPracticeToolsUseCase,
            getAiConfigUseCase = container.getAiConfigUseCase,
            subjectRepository = container.subjectRepository
        )
    }
}

@Composable
private fun rememberFlashcardStudyViewModel(
    container: StudyOSAppContainer,
    chapterId: String?,
    isDueOnly: Boolean
): FlashcardStudyViewModel {
    val key = "FlashcardStudyViewModel_${chapterId}_${isDueOnly}"
    return androidx.lifecycle.viewmodel.compose.viewModel(key = key) {
        FlashcardStudyViewModel(
            chapterId = chapterId,
            isDueOnly = isDueOnly,
            getFlashcardsForChapterUseCase = container.getFlashcardsForChapterUseCase,
            getDueFlashcardsUseCase = container.getDueFlashcardsUseCase,
            reviewFlashcardUseCase = container.reviewFlashcardUseCase
        )
    }
}

@Composable
private fun rememberQuizRunnerViewModel(
    container: StudyOSAppContainer,
    quizId: String
): QuizRunnerViewModel {
    val key = "QuizRunnerViewModel_$quizId"
    return androidx.lifecycle.viewmodel.compose.viewModel(key = key) {
        QuizRunnerViewModel(
            quizId = quizId,
            getQuizWithQuestionsUseCase = container.getQuizWithQuestionsUseCase,
            submitQuizAttemptUseCase = container.submitQuizAttemptUseCase,
            saveActiveQuizStateUseCase = container.saveActiveQuizStateUseCase,
            getActiveQuizStateUseCase = container.getActiveQuizStateUseCase
        )
    }
}

@Composable
private fun rememberMistakeBankViewModel(
    container: StudyOSAppContainer
): MistakeBankViewModel {
    return androidx.lifecycle.viewmodel.compose.viewModel {
        MistakeBankViewModel(
            getAllMistakesUseCase = container.getAllMistakesUseCase,
            resolveMistakeUseCase = container.resolveMistakeUseCase,
            convertMistakeToFlashcardUseCase = container.convertMistakeToFlashcardUseCase,
            aiPracticeToolsUseCase = container.aiPracticeToolsUseCase,
            getAiConfigUseCase = container.getAiConfigUseCase,
            subjectRepository = container.subjectRepository,
            mistakeRepository = container.mistakeRepository
        )
    }
}

@Composable
private fun rememberExamViewModel(
    container: StudyOSAppContainer,
    viewModelStoreOwner: androidx.lifecycle.ViewModelStoreOwner? = null
): ExamViewModel {
    val context = androidx.compose.ui.platform.LocalContext.current.applicationContext
    val factory = remember(container) {
        object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return ExamViewModel(
                    getExamsUseCase = container.getExamsUseCase,
                    saveExamUseCase = container.saveExamUseCase,
                    deleteExamUseCase = container.deleteExamUseCase,
                    getExamDashboardUseCase = container.getExamDashboardUseCase,
                    subjectRepository = container.subjectRepository,
                    updateExamScoreUseCase = container.updateExamScoreUseCase,
                    updateChapterProgressUseCase = container.updateChapterProgressUseCase,
                    quizRepository = container.quizRepository,
                    alarmScheduler = container.alarmScheduler,
                    context = context
                ) as T
            }
        }
    }
    return if (viewModelStoreOwner != null) {
        androidx.lifecycle.viewmodel.compose.viewModel(
            viewModelStoreOwner = viewModelStoreOwner,
            factory = factory
        )
    } else {
        androidx.lifecycle.viewmodel.compose.viewModel(factory = factory)
    }
}

@Composable
private fun rememberRevisionDashboardViewModel(
    container: StudyOSAppContainer
): RevisionDashboardViewModel {
    return androidx.lifecycle.viewmodel.compose.viewModel {
        RevisionDashboardViewModel(
            getDueRevisionDashboardUseCase = container.getDueRevisionDashboardUseCase,
            revisionRepository = container.revisionRepository,
            chapterRepository = container.chapterRepository,
            subjectRepository = container.subjectRepository,
            mistakeRepository = container.mistakeRepository,
            recallRepository = container.recallRepository,
            examRepository = container.examRepository,
            flashcardRepository = container.flashcardRepository
        )
    }
}

@Composable
private fun rememberActiveRecallRunnerViewModel(
    container: StudyOSAppContainer,
    sessionTypeStr: String,
    chapterId: String?
): ActiveRecallRunnerViewModel {
    val sessionType = try {
        ActiveRecallSessionType.valueOf(sessionTypeStr)
    } catch (_: Exception) {
        ActiveRecallSessionType.DEEP_15
    }
    val key = "ActiveRecallRunnerViewModel_${sessionType.name}_$chapterId"
    return androidx.lifecycle.viewmodel.compose.viewModel(key = key) {
        ActiveRecallRunnerViewModel(
            startActiveRecallSessionUseCase = container.startActiveRecallSessionUseCase,
            completeActiveRecallSessionUseCase = container.completeActiveRecallSessionUseCase,
            initialSessionType = sessionType,
            chapterId = chapterId
        )
    }
}

@Composable
private fun rememberFeynmanAudioWalkViewModel(
    container: StudyOSAppContainer,
    subjectId: String?,
    chapterId: String?
): FeynmanAudioWalkViewModel {
    val key = "FeynmanAudioWalkViewModel_${subjectId}_${chapterId}"
    return androidx.lifecycle.viewmodel.compose.viewModel(key = key) {
        FeynmanAudioWalkViewModel(
            subjectId = subjectId,
            chapterId = chapterId,
            noteRepository = container.noteRepository,
            mistakeRepository = container.mistakeRepository,
            subjectRepository = container.subjectRepository,
            chapterRepository = container.chapterRepository,
            aiProvider = container.aiProvider,
            getAiConfigUseCase = container.getAiConfigUseCase
        )
    }
}

@Composable
private fun rememberDocumentViewerViewModel(
    container: StudyOSAppContainer,
    documentUri: String?,
    noteId: String?,
    title: String?
): DocumentViewerViewModel {
    val key = "DocumentViewerViewModel_${documentUri}_${noteId}_${title}"
    return androidx.lifecycle.viewmodel.compose.viewModel(key = key) {
        DocumentViewerViewModel(
            context = container.appContext,
            initialDocumentUri = documentUri,
            initialNoteId = noteId,
            initialTitle = title,
            noteRepository = container.noteRepository,
            subjectRepository = container.subjectRepository,
            chapterRepository = container.chapterRepository,
            saveFlashcardUseCase = container.saveFlashcardUseCase,
            saveQuizUseCase = container.saveQuizUseCase,
            aiPracticeToolsUseCase = container.aiPracticeToolsUseCase,
            getAiConfigUseCase = container.getAiConfigUseCase,
            getNoteUseCase = container.getNoteUseCase
        )
    }
}



