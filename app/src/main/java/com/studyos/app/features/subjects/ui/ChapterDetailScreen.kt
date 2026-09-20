package com.studyos.app.features.subjects.ui

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.studyos.app.core.ui.component.StudyOSButton
import com.studyos.app.core.ui.component.StudyOSConfirmationDialog
import com.studyos.app.core.ui.component.StudyOSEmptyState
import com.studyos.app.core.ui.component.StudyOSIconButton
import com.studyos.app.core.ui.component.StudyOSLoadingState
import com.studyos.app.core.ui.component.StudyOSOutlinedButton
import com.studyos.app.core.ui.component.StudyOSProgressBar
import com.studyos.app.domain.model.ChapterStatus
import com.studyos.app.features.subjects.viewmodel.ChapterViewModel
import com.studyos.app.theme.StudyOSTheme
import kotlin.math.roundToInt

@Composable
fun ChapterDetailScreen(
    viewModel: ChapterViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    var showEditSheet by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) {
            onBack()
        }
    }

    LaunchedEffect(uiState.actionMessage) {
        uiState.actionMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        if (uiState.isLoading) {
            StudyOSLoadingState(message = "Loading chapter...")
        } else if (uiState.chapter == null) {
            StudyOSEmptyState(
                title = "Chapter not found",
                description = "This chapter may have been deleted.",
                actionButtonText = "Go back",
                onActionClick = onBack
            )
        } else {
            val chapter = uiState.chapter!!
            val subjectName = uiState.subject?.name ?: "Subject"

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 24.dp)
                    .widthIn(max = 600.dp)
            ) {
                // Top Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StudyOSIconButton(
                            onClick = onBack,
                            contentDescription = "Back to subject"
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = null,
                                tint = colors.primaryText,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = subjectName,
                            style = typography.secondary,
                            color = colors.secondaryText
                        )
                    }

                    Row {
                        StudyOSIconButton(
                            onClick = { showEditSheet = true },
                            contentDescription = "Edit chapter"
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = null,
                                tint = colors.secondaryText,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        StudyOSIconButton(
                            onClick = { showDeleteConfirmDialog = true },
                            contentDescription = "Delete chapter"
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

                Spacer(modifier = Modifier.height(20.dp))

                // Chapter Title & Description Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shapes.surface)
                        .background(colors.surface)
                        .border(1.dp, colors.border, shapes.surface)
                        .padding(20.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (chapter.status == ChapterStatus.COMPLETED || chapter.progress == 100) {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = "Completed",
                                    tint = colors.secondaryText,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = chapter.name,
                                style = typography.screenTitle,
                                color = colors.primaryText
                            )
                        }

                        if (!chapter.description.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = chapter.description,
                                style = typography.body,
                                color = colors.secondaryText
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Progress Control Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shapes.surface)
                        .background(colors.surface)
                        .border(1.dp, colors.border, shapes.surface)
                        .padding(20.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Progress",
                                style = typography.sectionTitle,
                                color = colors.primaryText
                            )

                            Text(
                                text = "${chapter.progress}%",
                                style = typography.sectionTitle,
                                color = colors.accent
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        StudyOSProgressBar(
                            progress = chapter.progress,
                            height = 6.dp,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        var sliderVal by remember(chapter.progress) {
                            mutableFloatStateOf(chapter.progress.toFloat())
                        }

                        Slider(
                            value = sliderVal,
                            onValueChange = { sliderVal = it },
                            onValueChangeFinished = {
                                viewModel.updateProgress(sliderVal.roundToInt())
                            },
                            valueRange = 0f..100f,
                            steps = 99,
                            colors = SliderDefaults.colors(
                                thumbColor = colors.accent,
                                activeTrackColor = colors.accent,
                                inactiveTrackColor = colors.border
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = "Status",
                            style = typography.caption,
                            color = colors.mutedText
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Quick Status Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatusOptionChip(
                                label = "Not started",
                                isSelected = chapter.status == ChapterStatus.NOT_STARTED && chapter.progress == 0,
                                onClick = {
                                    viewModel.updateStatus(ChapterStatus.NOT_STARTED)
                                },
                                modifier = Modifier.weight(1f)
                            )

                            StatusOptionChip(
                                label = "In progress",
                                isSelected = chapter.status == ChapterStatus.IN_PROGRESS && chapter.progress in 1..99,
                                onClick = {
                                    viewModel.updateStatus(ChapterStatus.IN_PROGRESS)
                                },
                                modifier = Modifier.weight(1f)
                            )

                            StatusOptionChip(
                                label = "Completed",
                                isSelected = chapter.status == ChapterStatus.COMPLETED || chapter.progress == 100,
                                onClick = {
                                    viewModel.updateStatus(ChapterStatus.COMPLETED)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Edit Chapter Bottom Sheet
            if (showEditSheet) {
                EditChapterBottomSheet(
                    chapter = chapter,
                    onDismissRequest = {
                        showEditSheet = false
                        viewModel.clearMessages()
                    },
                    onSaveChapter = { name, desc, progress ->
                        val success = viewModel.editChapter(name, desc, progress)
                        if (success) {
                            showEditSheet = false
                        }
                        success
                    },
                    errorMessage = uiState.errorMessage,
                    onClearError = viewModel::clearMessages
                )
            }

            // Delete Confirmation Dialog
            if (showDeleteConfirmDialog) {
                StudyOSConfirmationDialog(
                    title = "Delete chapter?",
                    message = "Are you sure you want to remove \"${chapter.name}\"? This action cannot be undone.",
                    confirmButtonText = "Delete",
                    dismissButtonText = "Cancel",
                    isDestructive = true,
                    onConfirm = {
                        viewModel.deleteChapter()
                        showDeleteConfirmDialog = false
                    },
                    onDismiss = { showDeleteConfirmDialog = false }
                )
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
private fun StatusOptionChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    val bg = if (isSelected) colors.accent.copy(alpha = 0.12f) else colors.surface
    val border = if (isSelected) colors.accent else colors.border
    val textCol = if (isSelected) colors.accent else colors.secondaryText

    Box(
        modifier = modifier
            .clip(shapes.surface)
            .background(bg)
            .border(1.dp, border, shapes.surface)
            .clickable(onClick = onClick)
            .semantics { role = Role.Button }
            .padding(vertical = 10.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = typography.caption,
            color = textCol
        )
    }
}
