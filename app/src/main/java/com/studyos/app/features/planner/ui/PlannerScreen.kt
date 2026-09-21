package com.studyos.app.features.planner.ui

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.studyos.app.domain.model.PlannerViewMode
import com.studyos.app.features.planner.viewmodel.PlannerViewModel
import com.studyos.app.theme.StudyOSTheme

@Composable
fun PlannerScreen(
    viewModel: PlannerViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbarMessage()
        }
    }

    // Create Session Sheet
    if (uiState.isCreateSessionSheetOpen) {
        EditSessionBottomSheet(
            session = null,
            initialDate = uiState.selectedDate,
            defaultDurationMinutes = uiState.defaultSessionMinutes,
            subjects = uiState.subjects,
            onDismissRequest = viewModel::closeCreateSessionSheet,
            onSaveSession = { sessionId, subjectId, chapterId, title, scheduledStart, plannedMinutes ->
                viewModel.saveSession(sessionId, subjectId, chapterId, title, scheduledStart, plannedMinutes)
            },
            onCheckOverlap = { start, end, excludeId ->
                viewModel.checkOverlap(start, end, excludeId)
            },
            onFetchChaptersForSubject = { subjectId ->
                viewModel.getChaptersForSubject(subjectId)
            }
        )
    }

    // Edit Session Sheet
    uiState.editingSession?.let { session ->
        EditSessionBottomSheet(
            session = session,
            initialDate = uiState.selectedDate,
            defaultDurationMinutes = uiState.defaultSessionMinutes,
            subjects = uiState.subjects,
            onDismissRequest = viewModel::closeEditSession,
            onSaveSession = { sessionId, subjectId, chapterId, title, scheduledStart, plannedMinutes ->
                viewModel.saveSession(sessionId, subjectId, chapterId, title, scheduledStart, plannedMinutes)
            },
            onDeleteSession = { sessionId ->
                viewModel.deleteSession(sessionId)
            },
            onCheckOverlap = { start, end, excludeId ->
                viewModel.checkOverlap(start, end, excludeId)
            },
            onFetchChaptersForSubject = { subjectId ->
                viewModel.getChaptersForSubject(subjectId)
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.background,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.Start
            ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Screen Header & View Mode Switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Planner",
                    style = typography.screenTitle,
                    color = colors.primaryText
                )

                // Controls: Week | List
                Row(
                    modifier = Modifier
                        .clip(shapes.button)
                        .background(colors.surface)
                        .border(1.dp, colors.border, shapes.button)
                        .padding(2.dp)
                ) {
                    val isWeek = uiState.viewMode == PlannerViewMode.WEEK
                    Box(
                        modifier = Modifier
                            .clip(shapes.button)
                            .background(if (isWeek) colors.primaryText else colors.surface)
                            .clickable { viewModel.setViewMode(PlannerViewMode.WEEK) }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                            .semantics { this.role = Role.Tab },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Week",
                            style = if (isWeek) typography.bodyMedium else typography.body,
                            color = if (isWeek) colors.background else colors.secondaryText
                        )
                    }

                    val isList = uiState.viewMode == PlannerViewMode.LIST
                    Box(
                        modifier = Modifier
                            .clip(shapes.button)
                            .background(if (isList) colors.primaryText else colors.surface)
                            .clickable { viewModel.setViewMode(PlannerViewMode.LIST) }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                            .semantics { this.role = Role.Tab },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "List",
                            style = if (isList) typography.bodyMedium else typography.body,
                            color = if (isList) colors.background else colors.secondaryText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Auto-Schedule Reviews & iCal Export Action Bar
            val context = androidx.compose.ui.platform.LocalContext.current
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Auto-schedule button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(shapes.button)
                        .background(colors.accent.copy(alpha = 0.12f))
                        .border(1.dp, colors.accent.copy(alpha = 0.4f), shapes.button)
                        .clickable { viewModel.autoScheduleDueReviews() }
                        .padding(vertical = 8.dp, horizontal = 10.dp)
                        .semantics { this.role = Role.Button },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "⚡ Auto-Schedule Due",
                        style = typography.caption.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
                        color = colors.accent
                    )
                }

                // Export iCal (.ics) button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(shapes.button)
                        .background(colors.surface)
                        .border(1.dp, colors.border, shapes.button)
                        .clickable {
                            val icsData = viewModel.exportIcsCalendar()
                            val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/calendar"
                                putExtra(android.content.Intent.EXTRA_TITLE, "studyos_schedule.ics")
                                putExtra(android.content.Intent.EXTRA_SUBJECT, "StudyOS Schedule Export")
                                putExtra(android.content.Intent.EXTRA_TEXT, icsData)
                            }
                            context.startActivity(android.content.Intent.createChooser(sendIntent, "Export Calendar (.ics)"))
                        }
                        .padding(vertical = 8.dp, horizontal = 10.dp)
                        .semantics { this.role = Role.Button },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "📅 Export .ics",
                        style = typography.caption.copy(fontWeight = FontWeight.Medium, fontSize = 12.sp),
                        color = colors.primaryText
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (uiState.viewMode) {
                PlannerViewMode.WEEK, PlannerViewMode.DAY -> {
                    uiState.weekSchedule?.let { schedule ->
                        WeekCalendarRow(
                            selectedDate = schedule.selectedDate,
                            weekDays = schedule.weekDays,
                            onSelectDate = viewModel::selectDate,
                            onPreviousWeek = viewModel::previousWeek,
                            onNextWeek = viewModel::nextWeek,
                            onTodayClick = viewModel::goToToday
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        DayScheduleTimeline(
                            selectedDate = schedule.selectedDate,
                            sessions = schedule.sessionsForSelectedDate,
                            onAddSession = viewModel::openCreateSessionSheet,
                            onEditSession = viewModel::openEditSession
                        )
                    }
                }

                PlannerViewMode.LIST -> {
                    ListScheduleView(
                        groups = uiState.upcomingGroups,
                        onAddSession = viewModel::openCreateSessionSheet,
                        onEditSession = viewModel::openEditSession
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}
}
