package com.studyos.app.features.timer.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studyos.app.core.ui.component.GlassCard
import com.studyos.app.core.ui.component.GlassIconButton
import com.studyos.app.core.ui.component.GlassTopBar
import com.studyos.app.core.ui.component.StudyOSButton
import com.studyos.app.core.ui.component.StudyOSOutlinedButton
import com.studyos.app.core.ui.component.StudyOSTextField
import com.studyos.app.features.timer.viewmodel.PomodoroPhase
import com.studyos.app.features.timer.viewmodel.StudyTimerViewModel
import com.studyos.app.features.timer.viewmodel.TimerMode
import com.studyos.app.features.timer.viewmodel.TimerStatus
import com.studyos.app.theme.StudyOSTheme
import java.util.Locale

@Composable
fun StudyTimerScreen(
    viewModel: StudyTimerViewModel,
    onBack: () -> Unit = {},
    onOpenDrawer: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    var subjectMenuExpanded by remember { mutableStateOf(false) }
    var chapterMenuExpanded by remember { mutableStateOf(false) }

    // Keep screen awake (Desk Mode) when timer is running
    val view = LocalView.current
    DisposableEffect(uiState.keepScreenAwake, uiState.status) {
        view.keepScreenOn = uiState.keepScreenAwake && uiState.status == TimerStatus.RUNNING
        onDispose {
            view.keepScreenOn = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
        ) {
            // Header Top Bar
            GlassTopBar(
                title = "Study Timer",
                subtitle = when (uiState.mode) {
                    TimerMode.COUNTDOWN -> "Deep Work Focus Block"
                    TimerMode.COUNT_UP -> "Open Study Stopwatch"
                    TimerMode.POMODORO -> "Pomodoro Technique • ${uiState.pomodoroPhase.label}"
                },
                navigationIcon = {
                    GlassIconButton(
                        onClick = onOpenDrawer,
                        contentDescription = "Open Drawer"
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
                    GlassIconButton(
                        onClick = onBack,
                        contentDescription = "Back"
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = null,
                            tint = colors.secondaryText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Preset Duration & Mode Selector Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TimerPresetChip(
                        label = "25m Focus",
                        selected = uiState.mode == TimerMode.COUNTDOWN && uiState.totalDurationSeconds == 25 * 60,
                        enabled = uiState.status != TimerStatus.RUNNING,
                        onClick = {
                            viewModel.setTimerMode(TimerMode.COUNTDOWN)
                            viewModel.setPresetMinutes(25)
                        }
                    )
                    TimerPresetChip(
                        label = "45m Deep",
                        selected = uiState.mode == TimerMode.COUNTDOWN && uiState.totalDurationSeconds == 45 * 60,
                        enabled = uiState.status != TimerStatus.RUNNING,
                        onClick = {
                            viewModel.setTimerMode(TimerMode.COUNTDOWN)
                            viewModel.setPresetMinutes(45)
                        }
                    )
                    TimerPresetChip(
                        label = "60m Block",
                        selected = uiState.mode == TimerMode.COUNTDOWN && uiState.totalDurationSeconds == 60 * 60,
                        enabled = uiState.status != TimerStatus.RUNNING,
                        onClick = {
                            viewModel.setTimerMode(TimerMode.COUNTDOWN)
                            viewModel.setPresetMinutes(60)
                        }
                    )
                    TimerPresetChip(
                        label = "90m Exam",
                        selected = uiState.mode == TimerMode.COUNTDOWN && uiState.totalDurationSeconds == 90 * 60,
                        enabled = uiState.status != TimerStatus.RUNNING,
                        onClick = {
                            viewModel.setTimerMode(TimerMode.COUNTDOWN)
                            viewModel.setPresetMinutes(90)
                        }
                    )
                    TimerPresetChip(
                        label = "+ Custom",
                        selected = uiState.mode == TimerMode.COUNTDOWN && !listOf(25, 45, 60, 90).contains(uiState.totalDurationSeconds / 60),
                        enabled = uiState.status != TimerStatus.RUNNING,
                        onClick = {
                            viewModel.showCustomDurationDialog(true)
                        }
                    )
                    TimerPresetChip(
                        label = "Stopwatch",
                        selected = uiState.mode == TimerMode.COUNT_UP,
                        enabled = uiState.status != TimerStatus.RUNNING,
                        onClick = {
                            viewModel.setTimerMode(TimerMode.COUNT_UP)
                        }
                    )
                    TimerPresetChip(
                        label = "🍅 Pomodoro",
                        selected = uiState.mode == TimerMode.POMODORO,
                        enabled = uiState.status != TimerStatus.RUNNING,
                        onClick = {
                            viewModel.setTimerMode(TimerMode.POMODORO)
                        }
                    )
                }

                if (uiState.mode == TimerMode.POMODORO) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shapes.card)
                            .background(colors.cardBackground)
                            .border(1.dp, colors.accent.copy(alpha = 0.4f), shapes.card)
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "POMODORO CYCLE • ${uiState.completedPomodoros} DONE",
                                        style = typography.caption,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.accent,
                                        letterSpacing = 1.sp
                                    )

                                    // 4-step cycle dots
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        val cycleStep = uiState.completedPomodoros % 4
                                        (0..3).forEach { dotIdx ->
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (dotIdx < cycleStep) colors.accent else colors.border
                                                    )
                                            )
                                        }
                                    }
                                }

                                if (uiState.status != TimerStatus.RUNNING) {
                                    Text(
                                        text = "Skip Phase",
                                        style = typography.caption,
                                        color = colors.mutedText,
                                        modifier = Modifier.clickable { viewModel.skipPomodoroPhase() }
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                PomodoroPhase.values().forEach { phase ->
                                    val isCurrent = uiState.pomodoroPhase == phase
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(shapes.button)
                                            .background(if (isCurrent) colors.accent.copy(alpha = 0.2f) else colors.surface)
                                            .border(1.dp, if (isCurrent) colors.accent else colors.border, shapes.button)
                                            .clickable(enabled = uiState.status != TimerStatus.RUNNING) {
                                                viewModel.setPomodoroPhase(phase)
                                            }
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = phase.label,
                                            style = typography.caption,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isCurrent) colors.accent else colors.secondaryText
                                        )
                                    }
                                }
                            }

                            // Auto-start toggle row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.toggleAutoStartPomodoro() },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Auto-transition to next phase",
                                    style = typography.caption,
                                    color = colors.secondaryText
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(shapes.button)
                                        .background(if (uiState.autoStartPomodoro) colors.accent.copy(alpha = 0.2f) else colors.surface)
                                        .border(1.dp, if (uiState.autoStartPomodoro) colors.accent else colors.border, shapes.button)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (uiState.autoStartPomodoro) "AUTO ON" else "MANUAL",
                                        style = typography.caption.copy(fontSize = 10.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = if (uiState.autoStartPomodoro) colors.accent else colors.mutedText
                                    )
                                }
                            }
                        }
                    }

                    // Restoration break guidance card
                    if (uiState.pomodoroPhase != PomodoroPhase.FOCUS) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(shapes.card)
                                .background(colors.cardBackground)
                                .border(1.dp, colors.accent.copy(alpha = 0.3f), shapes.card)
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "☕ ACTIVE RESTORATION BREAK",
                                    style = typography.caption,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.accent,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "Give your brain true recovery. Avoid social media & short-form video during breaks.",
                                    style = typography.caption,
                                    color = colors.secondaryText
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    BreakPromptItem("💧 Hydrate", "Drink water")
                                    BreakPromptItem("👁️ 20-20-20", "Rest eyes")
                                    BreakPromptItem("🧘 Stretch", "Roll shoulders")
                                    BreakPromptItem("🚶 Move", "Stand up")
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Subject & Chapter Pill Selector
                GlassCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "STUDY TARGET",
                                style = typography.caption,
                                color = colors.secondaryText,
                                letterSpacing = 1.sp
                            )
                            if (uiState.selectedSubject != null) {
                                Text(
                                    text = "Change",
                                    style = typography.caption,
                                    color = colors.accent,
                                    modifier = Modifier.clickable { subjectMenuExpanded = true }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Subject Selector Box
                            Box(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(shapes.small)
                                        .background(colors.background)
                                        .border(1.dp, colors.border, shapes.small)
                                        .clickable(enabled = uiState.status != TimerStatus.RUNNING) {
                                            subjectMenuExpanded = true
                                        }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                                        contentDescription = null,
                                        tint = colors.accent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = uiState.selectedSubject?.name ?: "All Subjects",
                                        style = typography.bodyMedium,
                                        color = colors.primaryText,
                                        maxLines = 1
                                    )
                                }

                                DropdownMenu(
                                    expanded = subjectMenuExpanded,
                                    onDismissRequest = { subjectMenuExpanded = false },
                                    modifier = Modifier.background(colors.surface)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("General Study (No Subject)", color = colors.primaryText) },
                                        onClick = {
                                            viewModel.selectSubject(null)
                                            subjectMenuExpanded = false
                                        }
                                    )
                                    uiState.subjects.forEach { subj ->
                                        DropdownMenuItem(
                                            text = { Text(subj.name, color = colors.primaryText) },
                                            onClick = {
                                                viewModel.selectSubject(subj)
                                                subjectMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Chapter Selector (if subject selected)
                            if (uiState.selectedSubject != null && uiState.chapters.isNotEmpty()) {
                                Box(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(shapes.small)
                                            .background(colors.background)
                                            .border(1.dp, colors.border, shapes.small)
                                            .clickable(enabled = uiState.status != TimerStatus.RUNNING) {
                                                chapterMenuExpanded = true
                                            }
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Book,
                                            contentDescription = null,
                                            tint = colors.secondaryText,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = uiState.selectedChapter?.name ?: "Select Chapter",
                                            style = typography.bodyMedium,
                                            color = if (uiState.selectedChapter != null) colors.primaryText else colors.secondaryText,
                                            maxLines = 1
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = chapterMenuExpanded,
                                        onDismissRequest = { chapterMenuExpanded = false },
                                        modifier = Modifier.background(colors.surface)
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Entire Subject", color = colors.primaryText) },
                                            onClick = {
                                                viewModel.selectChapter(null)
                                                chapterMenuExpanded = false
                                            }
                                        )
                                        uiState.chapters.forEach { chap ->
                                            DropdownMenuItem(
                                                text = { Text(chap.name, color = colors.primaryText) },
                                                onClick = {
                                                    viewModel.selectChapter(chap)
                                                    chapterMenuExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Today's studied minutes for this subject
                        if (uiState.selectedSubject != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.Timer,
                                    contentDescription = null,
                                    tint = colors.accent,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Today: ${uiState.todaySubjectMinutes} min studied for ${uiState.selectedSubject!!.name}",
                                    style = typography.caption,
                                    color = colors.accent
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Big Circular Timer Display
                val progress = when {
                    uiState.mode == TimerMode.COUNT_UP -> 1.0f
                    uiState.totalDurationSeconds > 0 -> {
                        val fraction = uiState.remainingSeconds.toFloat() / uiState.totalDurationSeconds.toFloat()
                        fraction.coerceIn(0f, 1f)
                    }
                    else -> 0f
                }

                val animatedProgress by animateFloatAsState(
                    targetValue = progress,
                    animationSpec = tween(durationMillis = 350),
                    label = "timerProgress"
                )

                val accentColor = colors.accent
                val trackColor = colors.surface

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(260.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                        val strokeWidth = 14.dp.toPx()
                        // Track ring
                        drawCircle(
                            color = trackColor,
                            style = Stroke(width = strokeWidth)
                        )
                        // Progress Arc
                        if (uiState.mode == TimerMode.COUNT_UP) {
                            val secAngle = ((uiState.elapsedSeconds % 60) / 60f) * 360f
                            drawArc(
                                color = accentColor,
                                startAngle = -90f + secAngle,
                                sweepAngle = 40f,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                        } else {
                            val sweepAngle = 360f * animatedProgress
                            drawArc(
                                color = accentColor,
                                startAngle = -90f,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                        }
                    }

                    // Inner Time readout
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Status Badge
                        val badgeText = when (uiState.status) {
                            TimerStatus.IDLE -> "READY"
                            TimerStatus.RUNNING -> if (uiState.mode == TimerMode.POMODORO) uiState.pomodoroPhase.label.uppercase() else "FOCUSING"
                            TimerStatus.PAUSED -> "PAUSED"
                            TimerStatus.COMPLETED -> "COMPLETED"
                        }
                        val badgeColor = when (uiState.status) {
                            TimerStatus.RUNNING -> colors.accent
                            TimerStatus.PAUSED -> colors.secondaryText
                            TimerStatus.COMPLETED -> colors.success
                            TimerStatus.IDLE -> colors.secondaryText
                        }

                        Text(
                            text = badgeText,
                            style = typography.caption,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor,
                            letterSpacing = 2.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Large Digital Clock Time (Fixed Pomodoro Bug!)
                        val displaySec = if (uiState.mode == TimerMode.COUNTDOWN || uiState.mode == TimerMode.POMODORO) {
                            uiState.remainingSeconds
                        } else {
                            uiState.elapsedSeconds
                        }
                        val hours = displaySec / 3600
                        val minutes = (displaySec % 3600) / 60
                        val seconds = displaySec % 60

                        val timeString = if (hours > 0) {
                            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
                        } else {
                            String.format(Locale.US, "%02d:%02d", minutes, seconds)
                        }

                        Text(
                            text = timeString,
                            style = typography.screenTitle.copy(
                                fontSize = if (hours > 0) 40.sp else 50.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = colors.primaryText,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        val subtitleText = when (uiState.status) {
                            TimerStatus.RUNNING -> if (uiState.mode == TimerMode.POMODORO) uiState.pomodoroPhase.label else "Stay in the zone"
                            TimerStatus.PAUSED -> "Take a quick breath"
                            TimerStatus.COMPLETED -> "Well done! Session logged"
                            TimerStatus.IDLE -> when (uiState.mode) {
                                TimerMode.POMODORO -> "${uiState.pomodoroPhase.durationMinutes}m ${uiState.pomodoroPhase.label}"
                                TimerMode.COUNTDOWN -> "${uiState.totalDurationSeconds / 60}m session"
                                TimerMode.COUNT_UP -> "Open session"
                            }
                        }

                        Text(
                            text = subtitleText,
                            style = typography.caption,
                            color = colors.secondaryText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Session logged banner
                AnimatedVisibility(
                    visible = uiState.isSessionSaved,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shapes.surface)
                            .background(colors.accent.copy(alpha = 0.12f))
                            .border(1.dp, colors.accent.copy(alpha = 0.35f), shapes.surface)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${uiState.savedSessionMinutes} min recorded to your StudyOS desk!",
                            style = typography.bodyMedium,
                            color = colors.primaryText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Control Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (uiState.status) {
                        TimerStatus.IDLE -> {
                            StudyOSButton(
                                text = "Start Session",
                                onClick = viewModel::startTimer,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        TimerStatus.RUNNING -> {
                            StudyOSOutlinedButton(
                                text = "Pause",
                                onClick = viewModel::pauseTimer,
                                modifier = Modifier.weight(1f)
                            )
                            StudyOSButton(
                                text = "End & Log",
                                onClick = viewModel::stopAndLogSession,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        TimerStatus.PAUSED -> {
                            StudyOSOutlinedButton(
                                text = "Reset",
                                onClick = viewModel::resetTimer,
                                modifier = Modifier.weight(1f)
                            )
                            StudyOSButton(
                                text = "Resume",
                                onClick = viewModel::resumeTimer,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        TimerStatus.COMPLETED -> {
                            StudyOSOutlinedButton(
                                text = "Reset",
                                onClick = viewModel::resetTimer,
                                modifier = Modifier.weight(1f)
                            )
                            StudyOSButton(
                                text = "Start New",
                                onClick = {
                                    viewModel.resetTimer()
                                    viewModel.startTimer()
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Desk Mode (Keep Screen On) Card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shapes.card)
                        .background(colors.cardBackground)
                        .border(1.dp, colors.border, shapes.card)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { viewModel.toggleKeepScreenAwake() }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Timer,
                            contentDescription = null,
                            tint = if (uiState.keepScreenAwake) colors.accent else colors.secondaryText,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Desk Mode (Keep Screen Awake)",
                            style = typography.caption,
                            color = if (uiState.keepScreenAwake) colors.primaryText else colors.secondaryText
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(shapes.button)
                            .background(if (uiState.keepScreenAwake) colors.accent.copy(alpha = 0.2f) else colors.surface)
                            .border(1.dp, if (uiState.keepScreenAwake) colors.accent else colors.border, shapes.button)
                            .clickable { viewModel.toggleKeepScreenAwake() }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (uiState.keepScreenAwake) "ENABLED" else "DISABLED",
                            style = typography.caption.copy(fontSize = 10.sp),
                            fontWeight = FontWeight.Bold,
                            color = if (uiState.keepScreenAwake) colors.accent else colors.mutedText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Custom Duration Dialog
    if (uiState.showCustomDurationDialog) {
        CustomDurationDialog(
            initialMinutes = uiState.totalDurationSeconds / 60,
            onDismiss = { viewModel.showCustomDurationDialog(false) },
            onConfirm = { mins -> viewModel.setCustomDurationMinutes(mins) }
        )
    }
}

@Composable
private fun BreakPromptItem(title: String, subtitle: String) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = title, style = typography.caption, fontWeight = FontWeight.SemiBold, color = colors.primaryText)
        Text(text = subtitle, style = typography.caption.copy(fontSize = 10.sp), color = colors.mutedText)
    }
}

@Composable
private fun CustomDurationDialog(
    initialMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var textValue by remember { mutableStateOf(initialMinutes.toString()) }
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    val quickPresets = listOf(10, 15, 20, 30, 45, 75, 120)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = {
            Text("Set Custom Timer Duration", style = typography.sectionTitle, color = colors.primaryText)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "Quick Presets",
                    style = typography.caption,
                    color = colors.secondaryText
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickPresets.forEach { mins ->
                        val isSelected = textValue == mins.toString()
                        Box(
                            modifier = Modifier
                                .clip(shapes.button)
                                .background(if (isSelected) colors.accent.copy(alpha = 0.2f) else colors.cardBackground)
                                .border(1.dp, if (isSelected) colors.accent else colors.border, shapes.button)
                                .clickable { textValue = mins.toString() }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${mins}m",
                                style = typography.caption,
                                color = if (isSelected) colors.accent else colors.primaryText
                            )
                        }
                    }
                }

                StudyOSTextField(
                    value = textValue,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() } && input.length <= 3) {
                            textValue = input
                        }
                    },
                    label = "Minutes (1 - 360)",
                    placeholder = "e.g. 30",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            StudyOSButton(
                text = "Apply",
                onClick = {
                    val mins = textValue.toIntOrNull()?.coerceIn(1, 360) ?: 25
                    onConfirm(mins)
                }
            )
        },
        dismissButton = {
            StudyOSOutlinedButton(
                text = "Cancel",
                onClick = onDismiss
            )
        }
    )
}

@Composable
private fun TimerPresetChip(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    val bg = if (selected) colors.accent.copy(alpha = 0.2f) else colors.surface
    val borderCol = if (selected) colors.accent else colors.border
    val textCol = if (selected) colors.accent else colors.secondaryText

    Box(
        modifier = Modifier
            .clip(shapes.small)
            .background(bg)
            .border(1.dp, borderCol, shapes.small)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = typography.caption,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = textCol
        )
    }
}
