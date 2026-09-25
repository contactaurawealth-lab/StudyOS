package com.studyos.app.features.today.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studyos.app.core.ui.component.GlassCard
import com.studyos.app.core.ui.component.GlassIconButton
import com.studyos.app.core.ui.component.GlassTopBar
import com.studyos.app.core.ui.component.StudyOSButton
import com.studyos.app.core.ui.component.StudyOSConfirmationDialog
import com.studyos.app.core.ui.component.StudyOSDivider
import com.studyos.app.core.ui.component.StudyOSEmptyState
import com.studyos.app.core.ui.component.StudyOSOutlinedButton
import com.studyos.app.core.util.DateTimeUtils
import com.studyos.app.domain.model.StudySessionStatus
import com.studyos.app.features.today.viewmodel.StudySessionViewModel
import com.studyos.app.theme.StudyOSTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudySessionPlaceholderScreen(
    viewModel: StudySessionViewModel,
    onBack: () -> Unit,
    onOpenChapter: (chapterId: String) -> Unit,
    onOpenTimer: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) {
            onBack()
        }
    }

    if (showDeleteConfirmation) {
        StudyOSConfirmationDialog(
            title = "Delete study session?",
            message = "This study session will be permanently removed.",
            confirmButtonText = "Delete",
            dismissButtonText = "Cancel",
            onConfirm = {
                showDeleteConfirmation = false
                viewModel.deleteSession()
            },
            onDismiss = { showDeleteConfirmation = false },
            isDestructive = true
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.background,
        topBar = {
            GlassTopBar(
                title = "Study Session",
                subtitle = uiState.subject?.name ?: "Deep Work",
                navigationIcon = {
                    GlassIconButton(
                        onClick = onBack,
                        contentDescription = "Back to Today"
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = null,
                            tint = colors.primaryText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                actions = {
                    if (uiState.session != null) {
                        GlassIconButton(
                            onClick = { showDeleteConfirmation = true },
                            contentDescription = "Delete session"
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = null,
                                tint = colors.secondaryText,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = colors.accent,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(32.dp)
                )
            }
        } else {
            val session = uiState.session
            if (session == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    StudyOSEmptyState(
                        title = "Session not found",
                        description = "This study session may have already been completed or removed.",
                        actionButtonText = "Back to Today",
                        onActionClick = onBack
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 600.dp)
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.Start
                    ) {
                        // Hero Session Card
                        GlassCard(
                            backgroundColor = colors.glassSurface,
                            padding = 20.dp
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // Subject badge
                                if (uiState.subject != null) {
                                    Box(
                                        modifier = Modifier
                                            .clip(shapes.statusPill)
                                            .background(colors.accent.copy(alpha = 0.15f))
                                            .border(0.5.dp, colors.accent.copy(alpha = 0.3f), shapes.statusPill)
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = uiState.subject!!.name.uppercase(),
                                            style = typography.caption.copy(
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 1.sp
                                            ),
                                            color = colors.accent
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                }

                                Text(
                                    text = session.title,
                                    style = typography.screenTitle,
                                    color = colors.primaryText
                                )

                                if (uiState.chapter != null) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                                            contentDescription = null,
                                            tint = colors.secondaryText,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = uiState.chapter!!.name,
                                            style = typography.bodyMedium,
                                            color = colors.secondaryText
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(18.dp))
                                StudyOSDivider()
                                Spacer(modifier = Modifier.height(16.dp))

                                // Session Details Rows
                                if (session.scheduledStart != null) {
                                    SessionDetailRow(
                                        icon = Icons.Outlined.CalendarToday,
                                        label = "Scheduled time",
                                        value = DateTimeUtils.formatTime(session.scheduledStart)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                }

                                SessionDetailRow(
                                    icon = Icons.Outlined.AccessTime,
                                    label = "Planned duration",
                                    value = "${session.plannedMinutes} minutes"
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                SessionDetailRow(
                                    icon = Icons.Outlined.CheckCircle,
                                    label = "Status",
                                    value = when (session.status) {
                                        StudySessionStatus.COMPLETED -> "Completed"
                                        StudySessionStatus.IN_PROGRESS -> "In Progress"
                                        StudySessionStatus.CANCELLED -> "Cancelled"
                                        StudySessionStatus.PLANNED -> "Planned"
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Primary Action: Start Timer
                        StudyOSButton(
                            text = "Start Focus Timer",
                            onClick = onOpenTimer,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Secondary Action: Open Chapter if linked
                        if (uiState.chapter != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            StudyOSOutlinedButton(
                                text = "Open Chapter",
                                onClick = { onOpenChapter(uiState.chapter!!.id) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.secondaryText,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = typography.secondary,
                color = colors.secondaryText
            )
        }
        Text(
            text = value,
            style = typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = colors.primaryText
        )
    }
}
