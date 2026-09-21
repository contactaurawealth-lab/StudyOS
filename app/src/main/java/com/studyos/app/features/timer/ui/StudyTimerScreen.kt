package com.studyos.app.features.timer.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
                        label = "Stopwatch",
                        selected = uiState.mode == TimerMode.COUNT_UP,
                        enabled = uiState.status != TimerStatus.RUNNING,
                        onClick = {
                            viewModel.setTimerMode(TimerMode.COUNT_UP)
                        }
                    )
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
                                        imageVector = Icons.Outlined.MenuBook,
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
                        val sweep = if (uiState.mode == TimerMode.COUNT_UP) {
                            // Rotate a small active dot / arc for stopwatch
                            val secAngle = ((uiState.elapsedSeconds % 60) / 60f) * 360f
                            drawArc(
                                color = accentColor,
                                startAngle = -90f + secAngle,
                                sweepAngle = 40f,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                            0f
                        } else {
                            val sweepAngle = 360f * animatedProgress
                            drawArc(
                                color = accentColor,
                                startAngle = -90f,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                            sweepAngle
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
                            TimerStatus.RUNNING -> "FOCUSING"
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

                        // Large Digital Clock Time
                        val displaySec = if (uiState.mode == TimerMode.COUNTDOWN) {
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
                            TimerStatus.RUNNING -> "Stay in the zone"
                            TimerStatus.PAUSED -> "Take a quick breath"
                            TimerStatus.COMPLETED -> "Well done! Session logged"
                            TimerStatus.IDLE -> if (uiState.mode == TimerMode.COUNTDOWN) "${uiState.totalDurationSeconds / 60}m session" else "Open session"
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

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
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
