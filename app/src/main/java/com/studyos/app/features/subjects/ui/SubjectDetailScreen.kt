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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.studyos.app.core.ui.component.StudyOSConfirmationDialog
import com.studyos.app.core.ui.component.StudyOSEmptyState
import com.studyos.app.core.ui.component.StudyOSIconButton
import com.studyos.app.core.ui.component.StudyOSLoadingState
import com.studyos.app.core.ui.component.StudyOSOutlinedButton
import com.studyos.app.core.ui.component.StudyOSProgressBar
import com.studyos.app.core.ui.component.StudyOSTextDialog
import com.studyos.app.domain.model.Chapter
import com.studyos.app.domain.model.ChapterStatus
import com.studyos.app.features.subjects.viewmodel.SubjectDetailViewModel
import com.studyos.app.theme.StudyOSTheme
import kotlinx.coroutines.launch

@Composable
fun SubjectDetailScreen(
    viewModel: SubjectDetailViewModel,
    onChapterClick: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes
    val coroutineScope = rememberCoroutineScope()

    var showAddChapterSheet by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
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
            StudyOSLoadingState(message = "Loading subject...")
        } else if (uiState.subject == null) {
            StudyOSEmptyState(
                title = "Subject not found",
                description = "This subject may have been deleted.",
                actionButtonText = "Go back",
                onActionClick = onBack
            )
        } else {
            val subject = uiState.subject!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 24.dp)
                    .widthIn(max = 600.dp)
            ) {
                // Top Navigation Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StudyOSIconButton(
                            onClick = onBack,
                            contentDescription = "Back to subjects"
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
                            text = subject.name,
                            style = typography.subsectionTitle,
                            color = colors.primaryText
                        )
                    }

                    Box {
                        StudyOSIconButton(
                            onClick = { menuExpanded = true },
                            contentDescription = "Subject options"
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
                                text = { Text("Edit subject", style = typography.body, color = colors.primaryText) },
                                onClick = {
                                    menuExpanded = false
                                    showRenameDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete subject", style = typography.body, color = colors.accent) },
                                onClick = {
                                    menuExpanded = false
                                    showDeleteConfirmDialog = true
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Subject Progress Header Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shapes.surface)
                        .background(colors.surface)
                        .border(1.dp, colors.border, shapes.surface)
                        .padding(18.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${uiState.progress}% complete",
                                style = typography.sectionTitle,
                                color = colors.primaryText
                            )

                            val chapterSummary = when (uiState.chapters.size) {
                                0 -> "0 chapters"
                                1 -> "1 chapter"
                                else -> "${uiState.chapters.size} chapters"
                            }
                            Text(
                                text = chapterSummary,
                                style = typography.secondary,
                                color = colors.secondaryText
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        StudyOSProgressBar(
                            progress = uiState.progress,
                            height = 5.dp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Chapters Section Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Chapters",
                        style = typography.sectionTitle,
                        color = colors.primaryText
                    )

                    StudyOSOutlinedButton(
                        text = "Add chapter",
                        onClick = { showAddChapterSheet = true }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Chapter List / Empty State
                if (uiState.chapters.isEmpty()) {
                    Spacer(modifier = Modifier.height(20.dp))
                    StudyOSEmptyState(
                        title = "No chapters yet.",
                        description = "Add chapters to start tracking this subject.",
                        actionButtonText = "Add chapter",
                        onActionClick = { showAddChapterSheet = true }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(uiState.chapters, key = { _, chapter -> chapter.id }) { index, chapter ->
                            ChapterListItemRow(
                                chapter = chapter,
                                isFirst = index == 0,
                                isLast = index == uiState.chapters.size - 1,
                                onClick = { onChapterClick(chapter.id) },
                                onMoveUp = { viewModel.moveChapter(chapter.id, moveUp = true) },
                                onMoveDown = { viewModel.moveChapter(chapter.id, moveUp = false) }
                            )
                        }
                    }
                }
            }
        }

        // Add Chapter Bottom Sheet
        if (showAddChapterSheet) {
            AddChapterBottomSheet(
                onDismissRequest = {
                    showAddChapterSheet = false
                    viewModel.clearMessages()
                },
                onAddChapter = { name, desc ->
                    val success = viewModel.addChapter(name, desc)
                    if (success) {
                        showAddChapterSheet = false
                    }
                    success
                },
                errorMessage = uiState.errorMessage,
                onClearError = viewModel::clearMessages
            )
        }

        // Rename Subject Dialog
        if (showRenameDialog && uiState.subject != null) {
            val currentSubject = uiState.subject!!
            StudyOSTextDialog(
                title = "Edit subject",
                initialValue = currentSubject.name,
                placeholder = "Subject name",
                confirmButtonText = "Save",
                onConfirm = { newName: String ->
                    coroutineScope.launch {
                        val success = viewModel.renameSubject(newName)
                        if (success) {
                            showRenameDialog = false
                        }
                    }
                },
                onDismiss = {
                    showRenameDialog = false
                    viewModel.clearMessages()
                },
                errorMessage = uiState.errorMessage
            )
        }

        // Delete Subject Confirmation Dialog
        if (showDeleteConfirmDialog) {
            StudyOSConfirmationDialog(
                title = "Delete subject?",
                message = "This will also remove its chapters.",
                confirmButtonText = "Delete",
                dismissButtonText = "Cancel",
                isDestructive = true,
                onConfirm = {
                    viewModel.deleteSubject()
                    showDeleteConfirmDialog = false
                },
                onDismiss = { showDeleteConfirmDialog = false }
            )
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
private fun ChapterListItemRow(
    chapter: Chapter,
    isFirst: Boolean,
    isLast: Boolean,
    onClick: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes
    val isCompleted = chapter.status == ChapterStatus.COMPLETED || chapter.progress == 100

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.surface)
            .background(colors.surface)
            .border(1.dp, colors.border, shapes.surface)
            .clickable(onClick = onClick)
            .semantics { role = Role.Button }
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = "Completed",
                        tint = colors.secondaryText,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = chapter.name,
                        style = typography.bodyMedium,
                        color = if (isCompleted) colors.secondaryText else colors.primaryText
                    )
                    if (!chapter.description.isNullOrBlank()) {
                        Text(
                            text = chapter.description,
                            style = typography.secondary,
                            color = colors.mutedText,
                            maxLines = 1,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "${chapter.progress}%",
                    style = typography.caption,
                    color = if (isCompleted) colors.secondaryText else colors.primaryText
                )

                Spacer(modifier = Modifier.width(4.dp))

                // Reorder controls
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StudyOSIconButton(
                        onClick = onMoveUp,
                        enabled = !isFirst,
                        contentDescription = "Move chapter up"
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ArrowUpward,
                            contentDescription = null,
                            tint = if (!isFirst) colors.secondaryText else colors.mutedText.copy(alpha = 0.4f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    StudyOSIconButton(
                        onClick = onMoveDown,
                        enabled = !isLast,
                        contentDescription = "Move chapter down"
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ArrowDownward,
                            contentDescription = null,
                            tint = if (!isLast) colors.secondaryText else colors.mutedText.copy(alpha = 0.4f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            StudyOSProgressBar(
                progress = chapter.progress,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
