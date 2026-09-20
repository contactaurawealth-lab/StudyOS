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
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.unit.dp
import com.studyos.app.core.ui.component.StudyOSButton
import com.studyos.app.core.ui.component.StudyOSConfirmationDialog
import com.studyos.app.core.ui.component.StudyOSDivider
import com.studyos.app.core.ui.component.StudyOSIconButton
import com.studyos.app.core.ui.component.StudyOSOutlinedButton
import com.studyos.app.core.util.DateTimeUtils
import com.studyos.app.features.today.viewmodel.StudySessionViewModel
import com.studyos.app.theme.StudyOSTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudySessionPlaceholderScreen(
    viewModel: StudySessionViewModel,
    onBack: () -> Unit,
    onOpenChapter: (chapterId: String) -> Unit,
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
            TopAppBar(
                title = {},
                navigationIcon = {
                    StudyOSIconButton(
                        onClick = onBack,
                        contentDescription = "Back to Today"
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = null,
                            tint = colors.primaryText,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                },
                actions = {
                    if (uiState.session != null) {
                        StudyOSIconButton(
                            onClick = { showDeleteConfirmation = true },
                            contentDescription = "Delete session"
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = null,
                                tint = colors.secondaryText,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.background
                )
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
                    Text(
                        text = "Session not found.",
                        style = typography.body,
                        color = colors.secondaryText
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 24.dp)
                        .verticalScroll(rememberScrollState())
                        .widthIn(max = 560.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "STUDY SESSION",
                        style = typography.caption,
                        color = colors.accent
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = session.title,
                        style = typography.screenTitle,
                        color = colors.primaryText
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    // Desk details card
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shapes.surface)
                            .background(colors.surface)
                            .border(1.dp, colors.border, shapes.surface)
                            .padding(20.dp)
                    ) {
                        if (uiState.subject != null) {
                            SessionDetailRow(
                                label = "Subject",
                                value = uiState.subject?.name ?: ""
                            )
                            StudyOSDivider(modifier = Modifier.padding(vertical = 12.dp))
                        }

                        if (uiState.chapter != null) {
                            SessionDetailRow(
                                label = "Chapter",
                                value = uiState.chapter?.name ?: ""
                            )
                            StudyOSDivider(modifier = Modifier.padding(vertical = 12.dp))
                        }

                        if (session.scheduledStart != null) {
                            SessionDetailRow(
                                label = "Scheduled time",
                                value = DateTimeUtils.formatTime(session.scheduledStart)
                            )
                            StudyOSDivider(modifier = Modifier.padding(vertical = 12.dp))
                        }

                        SessionDetailRow(
                            label = "Planned duration",
                            value = "${session.plannedMinutes} minutes"
                        )

                        StudyOSDivider(modifier = Modifier.padding(vertical = 12.dp))

                        SessionDetailRow(
                            label = "Status",
                            value = session.status.name.lowercase().replaceFirstChar { it.uppercase() }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Calm notice
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shapes.surface)
                            .background(colors.background)
                            .border(1.dp, colors.border, shapes.surface)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Study timer and active session tracking will be available in Phase 7.",
                            style = typography.secondary,
                            color = colors.secondaryText
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Chapter button if linked
                    if (uiState.chapter != null) {
                        StudyOSButton(
                            text = "Open Chapter",
                            onClick = { onOpenChapter(uiState.chapter!!.id) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionDetailRow(
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
        Text(
            text = label,
            style = typography.secondary,
            color = colors.secondaryText
        )
        Text(
            text = value,
            style = typography.bodyMedium,
            color = colors.primaryText
        )
    }
}
