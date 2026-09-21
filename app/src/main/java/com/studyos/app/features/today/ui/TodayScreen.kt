package com.studyos.app.features.today.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import com.studyos.app.core.ui.component.StudyOSLoadingState
import com.studyos.app.domain.model.RecallDashboardSummary
import com.studyos.app.domain.model.SmartStudyRecommendation
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.studyos.app.core.ui.component.GlassCard
import com.studyos.app.core.ui.component.GlassIconButton
import com.studyos.app.core.ui.component.GlassTopBar
import com.studyos.app.core.ui.component.StudyOSButton
import com.studyos.app.core.ui.component.StudyOSDivider
import com.studyos.app.core.ui.component.StudyOSOutlinedButton
import com.studyos.app.core.ui.component.StudyOSProgressBar
import com.studyos.app.core.ui.component.StudyOSSectionHeader
import com.studyos.app.core.util.DateTimeUtils
import com.studyos.app.domain.model.FocusItem
import com.studyos.app.domain.model.RecentChapterItem
import com.studyos.app.domain.model.StudySessionItem
import com.studyos.app.domain.model.TaskItem
import com.studyos.app.features.tasks.ui.TaskItemRow
import com.studyos.app.features.today.viewmodel.TodayViewModel
import com.studyos.app.theme.StudyOSTheme

@Composable
fun TodayScreen(
    viewModel: TodayViewModel,
    onOpenSession: (String) -> Unit = {},
    onOpenChapter: (String) -> Unit = {},
    onOpenTasks: () -> Unit = {},
    onOpenAi: () -> Unit = {},
    onOpenExams: () -> Unit = {},
    onOpenFlashcards: () -> Unit = {},
    onOpenRevision: () -> Unit = {},
    onOpenTimer: () -> Unit = {},
    onStartAiSession: (subjectId: String?, chapterId: String?) -> Unit = { _, _ -> },
    onOpenDrawer: () -> Unit = {},
    onOpenSearch: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes
    val snackbarHostState = remember { SnackbarHostState() }
    var menuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.infoMessage) {
        uiState.infoMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearInfoMessage()
        }
    }

    if (uiState.isPlanSessionSheetOpen) {
        PlanSessionBottomSheet(
            subjects = uiState.subjects,
            defaultDurationMinutes = uiState.sessionLengthMinutes,
            onDismissRequest = viewModel::closePlanSessionSheet,
            onPlanSession = { subjectId, chapterId, title, scheduledStart, plannedMinutes ->
                viewModel.planSession(subjectId, chapterId, title, scheduledStart, plannedMinutes)
            },
            onFetchChaptersForSubject = { subjectId ->
                viewModel.getChaptersForSubject(subjectId)
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Compact Modern Top Bar: ☰ Good morning, {name} ⋮
            GlassTopBar(
            title = uiState.greeting.removeSuffix("."),
            subtitle = uiState.dateHeader,
            navigationIcon = {
                GlassIconButton(
                    onClick = onOpenDrawer,
                    contentDescription = "Open Navigation Menu"
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = null,
                        tint = colors.primaryText,
                        modifier = Modifier.size(20.dp)
                    )
                }
            },
            actions = {
                Box {
                    GlassIconButton(
                        onClick = { menuExpanded = true },
                        contentDescription = "More Options"
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = null,
                            tint = colors.secondaryText,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier.background(colors.surface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("AI Assistant", style = typography.body, color = colors.primaryText) },
                            onClick = { menuExpanded = false; onOpenAi() },
                            leadingIcon = {
                                Icon(Icons.Outlined.Psychology, null, tint = colors.accent, modifier = Modifier.size(18.dp))
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Search", style = typography.body, color = colors.primaryText) },
                            onClick = { menuExpanded = false; onOpenSearch() },
                            leadingIcon = {
                                Icon(Icons.Outlined.Search, null, tint = colors.secondaryText, modifier = Modifier.size(18.dp))
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Plan Study Session", style = typography.body, color = colors.primaryText) },
                            onClick = { menuExpanded = false; viewModel.openPlanSessionSheet() },
                            leadingIcon = {
                                Icon(Icons.Outlined.Add, null, tint = colors.secondaryText, modifier = Modifier.size(18.dp))
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Study Timer", style = typography.body, color = colors.primaryText) },
                            onClick = { menuExpanded = false; onOpenTimer() },
                            leadingIcon = {
                                Icon(Icons.Outlined.Timer, null, tint = colors.accent, modifier = Modifier.size(18.dp))
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Revision & Active Recall", style = typography.body, color = colors.primaryText) },
                            onClick = { menuExpanded = false; onOpenRevision() },
                            leadingIcon = {
                                Icon(Icons.Outlined.Psychology, null, tint = colors.accent, modifier = Modifier.size(18.dp))
                            }
                        )
                    }
                }
            }
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            if (uiState.isLoading) {
                StudyOSLoadingState(message = "Loading StudyOS...")
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 680.dp)
                        .padding(horizontal = StudyOSTheme.spacing.screenHorizontal)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.Start
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Upcoming Exam or Flashcards Due Alerts
                    if (uiState.upcomingExam != null || uiState.dueFlashcardsCount > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (uiState.upcomingExam != null) {
                                val exam = uiState.upcomingExam!!
                                val days = kotlin.math.max(0L, (exam.targetDate - System.currentTimeMillis()) / 86_400_000L)
                                GlassCard(
                                    modifier = Modifier.weight(1f),
                                    onClick = { onOpenExams() },
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = if (days == 0L) "EXAM TODAY" else "EXAM IN $days DAYS",
                                            style = typography.caption,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colors.accent
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = exam.name,
                                            style = typography.secondary,
                                            fontWeight = FontWeight.Medium,
                                            color = colors.primaryText,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }

                            if (uiState.dueFlashcardsCount > 0) {
                                GlassCard(
                                    modifier = Modifier.weight(1f),
                                    onClick = { onOpenFlashcards() },
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = "FLASHCARDS",
                                            style = typography.caption,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colors.accent
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${uiState.dueFlashcardsCount} cards due",
                                            style = typography.secondary,
                                            fontWeight = FontWeight.Medium,
                                            color = colors.primaryText,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // 1. "What should I study now?" Smart Recommendation Card
                    uiState.smartRecommendation?.let { recommendation ->
                        SmartStudyRecommendationCard(
                            recommendation = recommendation,
                            onStartSession = { onStartAiSession(recommendation.subjectId, recommendation.chapterId) },
                            onOpenChapter = onOpenChapter
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // 2. AI Recall Engine Dashboard Widget (Spaced Repetition & Status Strip)
                    RecallDashboardWidget(
                        summary = uiState.recallDashboardSummary,
                        onStartRecall = onOpenRevision
                    )

                    Spacer(modifier = Modifier.height(20.dp))

            // 2. Today's Focus
            StudyOSSectionHeader(
                title = "Today's focus"
            )

            Spacer(modifier = Modifier.height(8.dp))

            TodayFocusCard(
                focusItem = uiState.focusItem,
                onOpenSession = onOpenSession,
                onOpenChapter = onOpenChapter,
                onPlanSession = viewModel::openPlanSessionSheet
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 3. Next Up
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StudyOSSectionHeader(
                    title = "Next up",
                    modifier = Modifier.weight(1f)
                )

                StudyOSOutlinedButton(
                    text = "Plan",
                    onClick = viewModel::openPlanSessionSheet,
                    modifier = Modifier.height(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            NextUpSection(
                sessions = uiState.upcomingSessions,
                onOpenSession = onOpenSession
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 4. Tasks
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StudyOSSectionHeader(
                    title = "Tasks",
                    modifier = Modifier.weight(1f)
                )

                StudyOSOutlinedButton(
                    text = "View all",
                    onClick = onOpenTasks,
                    modifier = Modifier.height(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            TodayTasksSection(
                tasks = uiState.todayTasks.take(4),
                onToggleTask = viewModel::toggleTaskCompletion,
                onOpenTasks = onOpenTasks
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 5. Today's Progress
            StudyOSSectionHeader(
                title = "Today's progress"
            )

            Spacer(modifier = Modifier.height(8.dp))

            TodayProgressCard(
                completedMinutes = uiState.completedMinutesToday,
                goalMinutes = uiState.dailyGoalMinutes,
                progressPercentage = uiState.dailyProgressPercentage,
                isGoalReached = uiState.isDailyGoalReached
            )

            // 5. Continue Studying (Only if a recent chapter exists)
            uiState.recentChapter?.let { recentChapter ->
                Spacer(modifier = Modifier.height(32.dp))

                StudyOSSectionHeader(
                    title = "Continue studying"
                )

                Spacer(modifier = Modifier.height(8.dp))

                ContinueStudyingCard(
                    recentChapter = recentChapter,
                    onOpenChapter = onOpenChapter
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 7. AI Study Assistant
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StudyOSSectionHeader(
                    title = "AI Assistant",
                    modifier = Modifier.weight(1f)
                )

                StudyOSOutlinedButton(
                    text = "Open chat",
                    onClick = onOpenAi,
                    modifier = Modifier.height(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            AiAssistantTodayCard(
                onOpenAi = onOpenAi
            )

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
    }
    }

    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 16.dp)
    )
}
}

@Composable
private fun AiAssistantTodayCard(
    onOpenAi: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onOpenAi,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Psychology,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "AI Study Assistant",
                    style = typography.sectionTitle,
                    color = colors.primaryText
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Ask questions, simplify complex topics, generate practice quizzes, or review notes.",
                style = typography.body,
                color = colors.secondaryText
            )
        }
    }
}

@Composable
private fun TodayFocusCard(
    focusItem: FocusItem?,
    onOpenSession: (sessionId: String) -> Unit,
    onOpenChapter: (chapterId: String) -> Unit,
    onPlanSession: () -> Unit
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp)
    ) {
        when (focusItem) {
            is FocusItem.SessionFocus -> {
                val session = focusItem.session
                val now = System.currentTimeMillis()
                val isNow = session.scheduledStart != null && now >= session.scheduledStart &&
                    (session.scheduledEnd == null || now <= session.scheduledEnd)

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isNow) "NOW" else "SCHEDULED TODAY",
                            style = typography.caption,
                            color = colors.accent
                        )

                        session.scheduledStart?.let {
                            Text(
                                text = DateTimeUtils.formatTime(it),
                                style = typography.caption,
                                color = colors.secondaryText
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = focusItem.subjectName,
                        style = typography.subsectionTitle,
                        color = colors.primaryText
                    )

                    if (!focusItem.chapterName.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = focusItem.chapterName,
                            style = typography.body,
                            color = colors.secondaryText
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "${session.plannedMinutes} min planned",
                        style = typography.caption,
                        color = colors.mutedText
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StudyOSButton(
                            text = "Start studying",
                            onClick = { onOpenSession(session.id) }
                        )

                        if (session.chapterId != null) {
                            Spacer(modifier = Modifier.width(10.dp))
                            StudyOSOutlinedButton(
                                text = "Open chapter",
                                onClick = { onOpenChapter(session.chapterId) }
                            )
                        }
                    }
                }
            }

            is FocusItem.ChapterFocus -> {
                val chapter = focusItem.chapter
                val tagText = if (focusItem.isLowestProgress) "RECOMMENDED FOCUS" else "IN PROGRESS"

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = tagText,
                        style = typography.caption,
                        color = colors.accent
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = focusItem.subjectName,
                        style = typography.secondary,
                        color = colors.secondaryText
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = chapter.name,
                        style = typography.subsectionTitle,
                        color = colors.primaryText
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    StudyOSProgressBar(progress = chapter.progress)

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "${chapter.progress}% completed",
                        style = typography.caption,
                        color = colors.mutedText
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    StudyOSButton(
                        text = "Open chapter",
                        onClick = { onOpenChapter(chapter.id) }
                    )
                }
            }

            null -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "Nothing planned yet.",
                        style = typography.body,
                        color = colors.secondaryText
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    StudyOSButton(
                        text = "Plan a session",
                        onClick = onPlanSession
                    )
                }
            }
        }
    }
}

@Composable
private fun NextUpSection(
    sessions: List<StudySessionItem>,
    onOpenSession: (sessionId: String) -> Unit
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography

    if (sessions.isEmpty()) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = "No more sessions scheduled for today.",
                style = typography.body,
                color = colors.mutedText
            )
        }
    } else {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
        ) {
            sessions.forEachIndexed { index, sessionItem ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenSession(sessionItem.session.id) }
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                        .semantics { this.role = Role.Button },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = sessionItem.formattedTime,
                                style = typography.caption,
                                color = colors.accent
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = "•  ${sessionItem.session.plannedMinutes} min",
                                style = typography.caption,
                                color = colors.mutedText
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = sessionItem.subjectName,
                            style = typography.bodyMedium,
                            color = colors.primaryText
                        )

                        if (!sessionItem.chapterName.isNullOrBlank()) {
                            Text(
                                text = sessionItem.chapterName,
                                style = typography.secondary,
                                color = colors.secondaryText
                            )
                        }
                    }
                }

                if (index < sessions.lastIndex) {
                    StudyOSDivider()
                }
            }
        }
    }
}

@Composable
private fun TodayProgressCard(
    completedMinutes: Int,
    goalMinutes: Int,
    progressPercentage: Int,
    isGoalReached: Boolean
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$completedMinutes / $goalMinutes min studied",
                    style = typography.bodyMedium,
                    color = colors.primaryText
                )

                if (isGoalReached) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Goal reached",
                            style = typography.caption,
                            color = colors.accent
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            StudyOSProgressBar(progress = progressPercentage)
        }
    }
}

@Composable
private fun ContinueStudyingCard(
    recentChapter: RecentChapterItem,
    onOpenChapter: (chapterId: String) -> Unit
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (recentChapter.subjectName.isNotBlank()) {
                Text(
                    text = recentChapter.subjectName,
                    style = typography.secondary,
                    color = colors.secondaryText
                )
                Spacer(modifier = Modifier.height(2.dp))
            }

            Text(
                text = recentChapter.chapter.name,
                style = typography.subsectionTitle,
                color = colors.primaryText
            )

            Spacer(modifier = Modifier.height(12.dp))

            StudyOSProgressBar(progress = recentChapter.chapter.progress)

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "${recentChapter.chapter.progress}% completed",
                style = typography.caption,
                color = colors.mutedText
            )

            Spacer(modifier = Modifier.height(16.dp))

            StudyOSButton(
                text = "Continue",
                onClick = { onOpenChapter(recentChapter.chapter.id) }
            )
        }
    }
}

@Composable
private fun TodayTasksSection(
    tasks: List<TaskItem>,
    onToggleTask: (taskId: String) -> Unit,
    onOpenTasks: () -> Unit
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography

    if (tasks.isEmpty()) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = "No tasks scheduled for today.",
                style = typography.body,
                color = colors.mutedText
            )
        }
    } else {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
        ) {
            tasks.forEachIndexed { index, taskItem ->
                TaskItemRow(
                    taskItem = taskItem,
                    onToggleCompletion = onToggleTask,
                    onClick = { onOpenTasks() }
                )

                if (index < tasks.lastIndex) {
                    StudyOSDivider()
                }
            }
        }
    }
}

@Composable
private fun SmartStudyRecommendationCard(
    recommendation: SmartStudyRecommendation,
    onStartSession: (chapterId: String) -> Unit,
    onOpenChapter: (chapterId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header tag + Match score
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Bolt,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "WHAT SHOULD I STUDY NOW?",
                        style = typography.caption,
                        fontWeight = FontWeight.Bold,
                        color = colors.accent
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(shapes.surface)
                        .background(colors.accent.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "${recommendation.priorityScore}% Match",
                        style = typography.caption,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.accent
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Subject name
            Text(
                text = recommendation.subjectName,
                style = typography.secondary,
                color = colors.secondaryText
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Chapter name
            Text(
                text = recommendation.chapterName,
                style = typography.subsectionTitle,
                color = colors.primaryText
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Why list
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shapes.surface)
                    .background(colors.surface.copy(alpha = 0.5f))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Why this chapter?",
                    style = typography.caption,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.secondaryText
                )
                recommendation.whyReasons.forEach { reason ->
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "•",
                            style = typography.caption,
                            color = colors.accent
                        )
                        Text(
                            text = reason,
                            style = typography.caption,
                            color = colors.primaryText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action row & Duration
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Timer,
                        contentDescription = null,
                        tint = colors.mutedText,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "${recommendation.recommendedDurationMinutes} min recommended",
                        style = typography.caption,
                        color = colors.mutedText
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StudyOSOutlinedButton(
                        text = "View",
                        onClick = { onOpenChapter(recommendation.chapterId) },
                        modifier = Modifier.height(36.dp)
                    )
                    StudyOSButton(
                        text = "Start Session",
                        onClick = { onStartSession(recommendation.chapterId) },
                        modifier = Modifier.height(36.dp)
                    )
                }
            }

            // Next chapter preview
            val nextChapter = recommendation.nextChapterPreview
            if (nextChapter != null) {
                Spacer(modifier = Modifier.height(10.dp))
                StudyOSDivider()
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Up next:",
                        style = typography.caption,
                        color = colors.mutedText
                    )
                    Text(
                        text = nextChapter,
                        style = typography.caption,
                        fontWeight = FontWeight.Medium,
                        color = colors.secondaryText
                    )
                }
            }
        }
    }
}

@Composable
private fun RecallDashboardWidget(
    summary: RecallDashboardSummary?,
    onStartRecall: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    val dueCount = summary?.dueCount ?: 0
    val weakCount = summary?.weakCount ?: 0
    val masteredCount = summary?.masteredCount ?: 0

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onStartRecall,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Psychology,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "AI Recall Engine",
                        style = typography.body.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.primaryText
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(shapes.surface)
                        .background(colors.accent.copy(alpha = 0.10f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Spaced Recall",
                        style = typography.caption,
                        color = colors.accent
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3 Stat Pills: Due, Weak, Mastered
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Due
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(shapes.surface)
                        .background(if (dueCount > 0) colors.accent.copy(alpha = 0.12f) else colors.surface)
                        .padding(vertical = 8.dp, horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$dueCount",
                            style = typography.sectionTitle.copy(fontWeight = FontWeight.Bold),
                            color = if (dueCount > 0) colors.accent else colors.secondaryText
                        )
                        Text(
                            text = "Due",
                            style = typography.caption,
                            color = colors.secondaryText
                        )
                    }
                }

                // Weak
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(shapes.surface)
                        .background(if (weakCount > 0) colors.critical.copy(alpha = 0.12f) else colors.surface)
                        .padding(vertical = 8.dp, horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$weakCount",
                            style = typography.sectionTitle.copy(fontWeight = FontWeight.Bold),
                            color = if (weakCount > 0) colors.critical else colors.secondaryText
                        )
                        Text(
                            text = "Weak",
                            style = typography.caption,
                            color = colors.secondaryText
                        )
                    }
                }

                // Mastered
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(shapes.surface)
                        .background(if (masteredCount > 0) colors.success.copy(alpha = 0.12f) else colors.surface)
                        .padding(vertical = 8.dp, horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$masteredCount",
                            style = typography.sectionTitle.copy(fontWeight = FontWeight.Bold),
                            color = if (masteredCount > 0) colors.success else colors.secondaryText
                        )
                        Text(
                            text = "Mastered",
                            style = typography.caption,
                            color = colors.secondaryText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            StudyOSButton(
                text = if (dueCount > 0) "Start Recall ($dueCount Due)" else "Start Recall Drill",
                onClick = onStartRecall,
                modifier = Modifier.fillMaxWidth().height(38.dp)
            )
        }
    }
}
