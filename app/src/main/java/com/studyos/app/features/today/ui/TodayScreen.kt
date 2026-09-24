package com.studyos.app.features.today.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalContext
import com.studyos.app.core.ui.component.StudyOSDialog
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
import androidx.compose.ui.unit.sp
import com.studyos.app.core.ui.component.GlassCard
import com.studyos.app.core.ui.component.GlassIconButton
import com.studyos.app.core.ui.component.GlassTopBar
import com.studyos.app.core.ui.component.StudyOSButton
import com.studyos.app.core.ui.component.StudyOSDivider
import com.studyos.app.core.ui.component.StudyOSLoadingState
import com.studyos.app.core.ui.component.StudyOSOutlinedButton
import com.studyos.app.core.ui.component.StudyOSProgressBar
import com.studyos.app.core.ui.component.StudyOSSectionHeader
import com.studyos.app.core.util.DateTimeUtils
import com.studyos.app.domain.engine.DailyBlockType
import com.studyos.app.domain.engine.DailyPlanBlock
import com.studyos.app.domain.model.FocusItem
import com.studyos.app.domain.model.SmartStudyRecommendation
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
    onOpenNotifications: () -> Unit = {},
    onOpenSubjects: () -> Unit = {},
    onResetComplete: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes
    val snackbarHostState = remember { SnackbarHostState() }
    var menuExpanded by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

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
            // Modern Top Bar
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
                    // Mini Daily Goal Habit Ring
                    val habitProgress = (uiState.completedMinutesToday.toFloat() / uiState.dailyGoalMinutes.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(colors.cardBackground.copy(alpha = 0.5f))
                            .border(0.5.dp, colors.border.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = { habitProgress },
                            modifier = Modifier.size(24.dp),
                            color = colors.accent,
                            trackColor = colors.border.copy(alpha = 0.2f),
                            strokeWidth = 2.dp
                        )
                        if (uiState.isDailyGoalReached) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = "Daily goal reached",
                                tint = colors.accent,
                                modifier = Modifier.size(12.dp)
                            )
                        } else {
                            Text(
                                text = "${uiState.completedMinutesToday}",
                                style = typography.caption.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                color = colors.primaryText
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    GlassIconButton(
                        onClick = onOpenNotifications,
                        contentDescription = "Notifications"
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = null,
                                tint = colors.secondaryText,
                                modifier = Modifier.size(20.dp)
                            )
                            if (uiState.unreadNotificationsCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .align(Alignment.TopEnd)
                                        .offset(x = 2.dp, y = (-2).dp)
                                        .background(colors.accent, shape = CircleShape)
                                )
                            }
                        }
                    }

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
                                text = { Text("Notifications", style = typography.body, color = colors.primaryText) },
                                onClick = { menuExpanded = false; onOpenNotifications() },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Notifications, null, tint = colors.accent, modifier = Modifier.size(18.dp))
                                }
                            )
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
                                text = { Text("Exams & Mock Tests", style = typography.body, color = colors.primaryText) },
                                onClick = { menuExpanded = false; onOpenExams() },
                                leadingIcon = {
                                    Icon(Icons.Outlined.School, null, tint = colors.accent, modifier = Modifier.size(18.dp))
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
                                text = { Text("Revision & Recall", style = typography.body, color = colors.primaryText) },
                                onClick = { menuExpanded = false; onOpenRevision() },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Bolt, null, tint = colors.accent, modifier = Modifier.size(18.dp))
                                }
                            )
                            StudyOSDivider()
                            DropdownMenuItem(
                                text = { Text("Backup & Reset App", style = typography.body, color = colors.accent) },
                                onClick = {
                                    menuExpanded = false
                                    showResetConfirmDialog = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Outlined.DeleteSweep, null, tint = colors.accent, modifier = Modifier.size(18.dp))
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
                    StudyOSLoadingState(message = "Personalizing StudyOS...")
                } else {
                    BoxWithConstraints(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        val isWide = maxWidth >= 600.dp
                        val contentPadding = if (isWide) 24.dp else StudyOSTheme.spacing.screenHorizontal
                        val isCleanSlate = uiState.subjects.isEmpty() && uiState.focusItem == null

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = 680.dp)
                                .padding(horizontal = contentPadding)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.Top,
                            horizontalAlignment = Alignment.Start
                        ) {
                            Spacer(modifier = Modifier.height(14.dp))

                            if (isCleanSlate) {
                                ZenCleanSlateView(
                                    onOpenSubjects = onOpenSubjects,
                                    onPlanSession = viewModel::openPlanSessionSheet,
                                    onOpenTimer = onOpenTimer,
                                    onOpenAi = onOpenAi
                                )
                            } else {
                                // 1. Actionable Exam Readiness Alert (if upcoming exam exists or readiness calculated)
                                if (uiState.upcomingExam != null || uiState.overallReadiness?.upcomingExamDaysLeft != null) {
                                    val now = System.currentTimeMillis()
                                    val isMock = uiState.upcomingExam?.notes?.contains("[MOCK_TEST]") == true
                                    val examName = uiState.upcomingExam?.name
                                        ?: uiState.overallReadiness?.upcomingExamName
                                        ?: "Final Exam"
                                    val daysLeft = uiState.upcomingExam?.getDaysRemaining(now)
                                        ?: uiState.overallReadiness?.upcomingExamDaysLeft
                                        ?: 0L
                                    val readinessPct = uiState.overallReadiness?.overallPercentage ?: 70

                                    GlassCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = onOpenExams,
                                        padding = 12.dp
                                    ) {
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    modifier = Modifier.weight(1f, fill = false),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        text = when {
                                                            daysLeft < 0L -> if (isMock) "MOCK PAST DUE" else "EXAM PAST DUE"
                                                            daysLeft == 0L -> if (isMock) "MOCK TODAY" else "EXAM TODAY"
                                                            daysLeft == 1L -> if (isMock) "MOCK TOMORROW" else "EXAM TOMORROW"
                                                            else -> if (isMock) "MOCK IN $daysLeft DAYS" else "EXAM IN $daysLeft DAYS"
                                                        },
                                                        style = typography.caption,
                                                        fontWeight = FontWeight.Bold,
                                                        color = colors.accent,
                                                        maxLines = 1,
                                                        softWrap = false
                                                    )
                                                    Text(
                                                        text = "•",
                                                        style = typography.caption,
                                                        color = colors.mutedText
                                                    )
                                                    Text(
                                                        text = examName,
                                                        style = typography.caption,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = colors.primaryText,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }

                                                Spacer(modifier = Modifier.width(8.dp))

                                                Box(
                                                    modifier = Modifier
                                                        .clip(shapes.surface)
                                                        .background(colors.accent.copy(alpha = 0.14f))
                                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "$readinessPct% Ready",
                                                        style = typography.caption.copy(fontSize = 11.sp),
                                                        fontWeight = FontWeight.Bold,
                                                        color = colors.accent,
                                                        maxLines = 1,
                                                        softWrap = false
                                                    )
                                                }
                                            }

                                            val insight = uiState.overallReadiness?.primaryInsight
                                            if (!insight.isNullOrBlank()) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = insight,
                                                    style = typography.caption.copy(fontSize = 12.sp),
                                                    color = colors.secondaryText
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(14.dp))
                                }

                                // 2. Interactive Quick Stat Strip
                                val dueCount = uiState.recallDashboardSummary?.dueCount ?: uiState.dueFlashcardsCount
                                val weakCount = uiState.recallDashboardSummary?.weakCount ?: 0
                                val hasActiveStats = dueCount > 0 || weakCount > 0 || uiState.dueFlashcardsCount > 0

                                if (hasActiveStats) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        StatPill(
                                            label = "$dueCount Due",
                                            icon = Icons.Outlined.Bolt,
                                            isHighlighted = dueCount > 0,
                                            onClick = onOpenRevision,
                                            modifier = Modifier.weight(1f)
                                        )
                                        StatPill(
                                            label = "$weakCount Weak",
                                            icon = Icons.Outlined.AutoAwesome,
                                            isHighlighted = weakCount > 0,
                                            onClick = {
                                                val firstWeakSub = uiState.subjects.firstOrNull { it.name.isNotBlank() }
                                                onStartAiSession(firstWeakSub?.id, null)
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                        StatPill(
                                            label = "${uiState.dueFlashcardsCount} Flashcards",
                                            icon = Icons.Outlined.Psychology,
                                            isHighlighted = false,
                                            onClick = onOpenFlashcards,
                                            modifier = Modifier.weight(1.2f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                }

                                // 3. Hero Focus Card (The North Star)
                                StudyOSSectionHeader(
                                    title = "Focus now"
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                UnifiedHeroFocusCard(
                                    recommendation = uiState.smartRecommendation,
                                    focusItem = uiState.focusItem,
                                    onStartAiSession = { subId, chapId -> onStartAiSession(subId, chapId) },
                                    onOpenTimer = onOpenTimer,
                                    onOpenChapter = onOpenChapter,
                                    onOpenSession = onOpenSession,
                                    onPlanSession = viewModel::openPlanSessionSheet
                                )

                                // 4. Daily AI Plan (Only rendered if blocks are present!)
                                val plan = uiState.dailyAiPlan
                                if (plan != null && plan.blocks.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(20.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        StudyOSSectionHeader(
                                            title = "Daily study blocks",
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = "${plan.totalAllocatedMinutes} min planned",
                                            style = typography.caption,
                                            color = colors.accent
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Time budget chips: 15m | 30m | 45m | 60m
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        listOf(15, 30, 45, 60).forEach { minutes ->
                                            TimeBudgetChip(
                                                minutes = minutes,
                                                isSelected = uiState.selectedTimeBudgetMinutes == minutes,
                                                onClick = { viewModel.setTimeBudget(minutes) }
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    GlassCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        backgroundColor = colors.glassSurface,
                                        padding = 12.dp
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            plan.blocks.forEachIndexed { index, block ->
                                                DailyPlanBlockRow(
                                                    index = index + 1,
                                                    block = block,
                                                    onStartBlock = {
                                                        when (block.type) {
                                                            DailyBlockType.ACTIVE_RECALL -> onOpenRevision()
                                                            DailyBlockType.MISTAKE_REVIEW -> onOpenFlashcards()
                                                            DailyBlockType.STUDY_CHAPTER, DailyBlockType.PRACTICE_QUIZ -> {
                                                                onStartAiSession(block.subjectId, block.chapterId)
                                                            }
                                                        }
                                                    }
                                                )
                                                if (index < plan.blocks.lastIndex) {
                                                    StudyOSDivider()
                                                }
                                            }
                                        }
                                    }
                                }

                                // 5. Next up sessions (if any)
                                if (uiState.upcomingSessions.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(20.dp))
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
                                            modifier = Modifier.height(34.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    NextUpSection(
                                        sessions = uiState.upcomingSessions,
                                        onOpenSession = onOpenSession
                                    )
                                }

                                // 6. Tasks (if any)
                                if (uiState.todayTasks.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(20.dp))
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
                                            modifier = Modifier.height(34.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    TodayTasksSection(
                                        tasks = uiState.todayTasks.take(4),
                                        onToggleTask = viewModel::toggleTaskCompletion,
                                        onOpenTasks = onOpenTasks
                                    )
                                }

                                // 7. AI Assistant Quick Card
                                Spacer(modifier = Modifier.height(20.dp))
                                AiAssistantTodayCard(onOpenAi = onOpenAi)

                                Spacer(modifier = Modifier.height(40.dp))
                            }
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 88.dp)
        )

        if (showResetConfirmDialog) {
            StudyOSDialog(
                onDismissRequest = { showResetConfirmDialog = false },
                title = "Export & Reset StudyOS?",
                text = "Your current data will be exported first as an offline JSON backup so you can keep a copy in your files. Then, all local database tables, subjects, chapters, notes, flashcards, exams, and settings will be permanently wiped.\n\nStudyOS will reset completely to a clean slate.",
                confirmButtonText = "Export & Reset",
                onConfirm = {
                    showResetConfirmDialog = false
                    viewModel.exportAndResetApp(
                        context = context,
                        onReadyToShare = { file ->
                            if (file != null) {
                                try {
                                    val fileUri = androidx.core.content.FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        file
                                    )
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/json"
                                        putExtra(Intent.EXTRA_STREAM, fileUri)
                                        putExtra(Intent.EXTRA_SUBJECT, "StudyOS Data Backup")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Save or Share StudyOS Backup"))
                                } catch (e: Exception) {
                                    // Handled gracefully
                                }
                            }
                        },
                        onResetComplete = onResetComplete
                    )
                },
                dismissButtonText = "Cancel",
                onDismiss = { showResetConfirmDialog = false }
            )
        }
    }
}

@Composable
private fun StatPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isHighlighted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    val bg = if (isHighlighted) colors.accent.copy(alpha = 0.14f) else colors.surface.copy(alpha = 0.6f)
    val border = if (isHighlighted) colors.accent.copy(alpha = 0.4f) else colors.border
    val tint = if (isHighlighted) colors.accent else colors.secondaryText

    Box(
        modifier = modifier
            .clip(shapes.pill)
            .background(bg)
            .border(1.dp, border, shapes.pill)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(13.dp))
            Text(
                text = label,
                style = typography.caption.copy(fontSize = 11.sp, fontWeight = if (isHighlighted) FontWeight.SemiBold else FontWeight.Medium),
                color = tint,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun TimeBudgetChip(
    minutes: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    val bg = if (isSelected) colors.accent.copy(alpha = 0.16f) else colors.surface.copy(alpha = 0.5f)
    val border = if (isSelected) colors.accent else colors.border
    val text = if (isSelected) colors.accent else colors.secondaryText

    Box(
        modifier = Modifier
            .clip(shapes.pill)
            .background(bg)
            .border(1.dp, border, shapes.pill)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$minutes min",
            style = typography.caption.copy(fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium),
            color = text
        )
    }
}

@Composable
private fun DailyPlanBlockRow(
    index: Int,
    block: DailyPlanBlock,
    onStartBlock: () -> Unit
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onStartBlock)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(shapes.pill)
                    .background(colors.accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$index",
                    style = typography.caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                    color = colors.accent
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = block.title,
                    style = typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = colors.primaryText,
                    maxLines = 1
                )
                Text(
                    text = "${block.durationMinutes} min • ${block.subtitle}",
                    style = typography.caption.copy(fontSize = 11.sp),
                    color = colors.secondaryText,
                    maxLines = 1
                )
            }
        }

        Box(
            modifier = Modifier
                .clip(shapes.pill)
                .background(colors.surface.copy(alpha = 0.8f))
                .border(1.dp, colors.border, shapes.pill)
                .clickable(onClick = onStartBlock)
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = "Start",
                style = typography.caption.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                color = colors.accent
            )
        }
    }
}

@Composable
private fun UnifiedHeroFocusCard(
    recommendation: SmartStudyRecommendation?,
    focusItem: FocusItem?,
    onStartAiSession: (subjectId: String?, chapterId: String?) -> Unit,
    onOpenTimer: () -> Unit,
    onOpenChapter: (chapterId: String) -> Unit,
    onOpenSession: (sessionId: String) -> Unit,
    onPlanSession: () -> Unit
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = colors.glassSurface,
        padding = 16.dp
    ) {
        if (recommendation != null) {
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
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "RECOMMENDED FOCUS",
                            style = typography.caption,
                            fontWeight = FontWeight.Bold,
                            color = colors.accent
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(shapes.surface)
                            .background(colors.accent.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${recommendation.priorityScore}% Match",
                            style = typography.caption.copy(fontSize = 11.sp),
                            fontWeight = FontWeight.SemiBold,
                            color = colors.accent
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = recommendation.subjectName,
                    style = typography.secondary,
                    color = colors.secondaryText
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = recommendation.chapterName,
                    style = typography.subsectionTitle,
                    color = colors.primaryText
                )

                val primaryReason = recommendation.whyReasons.firstOrNull()
                if (!primaryReason.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• $primaryReason",
                        style = typography.caption,
                        color = colors.secondaryText,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Actions: Start AI Session + Timer + Details
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StudyOSButton(
                        text = "Start AI Session",
                        onClick = { onStartAiSession(recommendation.subjectId, recommendation.chapterId) },
                        modifier = Modifier.weight(1f).height(38.dp)
                    )

                    GlassIconButton(
                        onClick = onOpenTimer,
                        contentDescription = "Study Timer",
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Timer,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    StudyOSOutlinedButton(
                        text = "View",
                        onClick = { onOpenChapter(recommendation.chapterId) },
                        modifier = Modifier.height(38.dp)
                    )
                }
            }
        } else if (focusItem is FocusItem.ChapterFocus) {
            val chapter = focusItem.chapter
            Column(modifier = Modifier.fillMaxWidth()) {
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
                            imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "CONTINUE STUDYING",
                            style = typography.caption,
                            fontWeight = FontWeight.Bold,
                            color = colors.accent
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(shapes.surface)
                            .background(colors.accent.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${chapter.progress}% Done",
                            style = typography.caption.copy(fontSize = 11.sp),
                            fontWeight = FontWeight.SemiBold,
                            color = colors.accent
                        )
                    }
                }

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

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StudyOSButton(
                        text = "Continue Chapter",
                        onClick = { onOpenChapter(chapter.id) },
                        modifier = Modifier.weight(1f).height(38.dp)
                    )

                    GlassIconButton(
                        onClick = onOpenTimer,
                        contentDescription = "Study Timer",
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Timer,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        } else if (focusItem is FocusItem.SessionFocus) {
            val session = focusItem.session
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Timer,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "SCHEDULED SESSION",
                        style = typography.caption,
                        fontWeight = FontWeight.Bold,
                        color = colors.accent
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = focusItem.subjectName,
                    style = typography.subsectionTitle,
                    color = colors.primaryText
                )
                if (!focusItem.chapterName.isNullOrBlank()) {
                    Text(
                        text = focusItem.chapterName,
                        style = typography.body,
                        color = colors.secondaryText
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StudyOSButton(
                        text = "Start studying",
                        onClick = { onOpenSession(session.id) },
                        modifier = Modifier.height(38.dp)
                    )
                    if (session.chapterId != null) {
                        StudyOSOutlinedButton(
                            text = "Open chapter",
                            onClick = { onOpenChapter(session.chapterId) },
                            modifier = Modifier.height(38.dp)
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "No focus block scheduled for today.",
                    style = typography.body,
                    color = colors.secondaryText
                )
                Spacer(modifier = Modifier.height(10.dp))
                StudyOSButton(
                    text = "Plan a session",
                    onClick = onPlanSession,
                    modifier = Modifier.height(36.dp)
                )
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

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = colors.glassSurface,
        padding = 0.dp
    ) {
        sessions.forEachIndexed { index, sessionItem ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenSession(sessionItem.session.id) }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
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
                            text = "• ${sessionItem.session.plannedMinutes} min",
                            style = typography.caption,
                            color = colors.mutedText
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
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

@Composable
private fun TodayTasksSection(
    tasks: List<TaskItem>,
    onToggleTask: (taskId: String) -> Unit,
    onOpenTasks: () -> Unit
) {
    val colors = StudyOSTheme.colors
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = colors.glassSurface,
        padding = 0.dp
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
        backgroundColor = colors.glassSurface,
        padding = 16.dp
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

            Spacer(modifier = Modifier.height(10.dp))
            StudyOSProgressBar(progress = progressPercentage)
        }
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
        backgroundColor = colors.glassSurface,
        padding = 14.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(colors.accent.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Psychology,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    Text(
                        text = "Socratic AI Tutor",
                        style = typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.primaryText
                    )
                    Text(
                        text = "Ask questions, explore concepts, or create flashcards",
                        style = typography.caption,
                        color = colors.mutedText,
                        maxLines = 1
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                contentDescription = null,
                tint = colors.mutedText,
                modifier = Modifier.size(13.dp)
            )
        }
    }
}

@Composable
private fun ZenCleanSlateView(
    onOpenSubjects: () -> Unit,
    onPlanSession: () -> Unit,
    onOpenTimer: () -> Unit,
    onOpenAi: () -> Unit
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 28.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Glowing Icon Badge
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(colors.accent.copy(alpha = 0.12f))
                .border(0.5.dp, colors.accent.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.LightMode,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(34.dp)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Clean Slate",
                style = typography.sectionTitle.copy(fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
                color = colors.primaryText
            )
            Text(
                text = "No clutter, no distractions. Add your subjects or start a quiet focus timer.",
                style = typography.secondary.copy(fontSize = 14.sp),
                color = colors.secondaryText,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StudyOSButton(
                text = "+ Add Subjects",
                onClick = onOpenSubjects,
                modifier = Modifier.weight(1f).height(42.dp)
            )

            StudyOSOutlinedButton(
                text = "Plan Session",
                onClick = onPlanSession,
                modifier = Modifier.weight(1f).height(42.dp)
            )
        }

        // Quick Zen Timer Card
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = onOpenTimer,
            padding = 14.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(colors.accent.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Timer,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Distraction-Free Timer",
                            style = typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = colors.primaryText
                        )
                        Text(
                            text = "Tap to start a 25m focus block anytime",
                            style = typography.caption,
                            color = colors.mutedText
                        )
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                    contentDescription = null,
                    tint = colors.mutedText,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        // Minimal AI Input Banner
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = onOpenAi,
            padding = 14.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(colors.accent.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Psychology,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "StudyOS AI Assistant",
                            style = typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = colors.primaryText
                        )
                        Text(
                            text = "Ask questions, explore concepts, or create flashcards",
                            style = typography.caption,
                            color = colors.mutedText
                        )
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                    contentDescription = null,
                    tint = colors.mutedText,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
