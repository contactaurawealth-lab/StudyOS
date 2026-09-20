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
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.ui.text.font.FontWeight
import com.studyos.app.core.ui.component.GlassCard
import com.studyos.app.core.ui.component.GlassDialog
import com.studyos.app.core.ui.component.GlassIconButton
import com.studyos.app.core.ui.component.GlassTopBar
import com.studyos.app.core.ui.component.ShimmerPlaceholder
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = StudyOSTheme.spacing.screenHorizontal, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                repeat(4) {
                    ShimmerPlaceholder(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        shape = shapes.medium
                    )
                }
            }
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
                    .widthIn(max = 600.dp)
            ) {
                // Glass Top Bar: ← SubjectName ⋮
                GlassTopBar(
                    title = subject.name,
                    subtitle = "${uiState.chapters.size} chapters • ${uiState.progress}% complete",
                    navigationIcon = {
                        GlassIconButton(
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
                    },
                    actions = {
                        GlassIconButton(
                            onClick = { showAddChapterSheet = true },
                            contentDescription = "Add chapter"
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = null,
                                tint = colors.primaryText,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Box {
                            GlassIconButton(
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
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = StudyOSTheme.spacing.screenHorizontal)
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Subject Progress Header Card
                    GlassCard(
                        backgroundColor = colors.glassSurface,
                        padding = 16.dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${uiState.progress}% complete",
                                style = typography.sectionTitle.copy(fontWeight = FontWeight.SemiBold),
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

                    Spacer(modifier = Modifier.height(20.dp))

                    // Chapters Section Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Chapters",
                            style = typography.sectionTitle.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.primaryText
                        )

                        StudyOSOutlinedButton(
                            text = "Add chapter",
                            onClick = { showAddChapterSheet = true }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Chapter List / Empty State
                    if (uiState.chapters.isEmpty()) {
                        Spacer(modifier = Modifier.height(20.dp))
                        StudyOSEmptyState(
                            title = "No chapters yet",
                            description = "Add chapters to start tracking your progress.",
                            actionButtonText = "Add Chapter",
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
                                    index = index,
                                    isFirst = index == 0,
                                    isLast = index == uiState.chapters.size - 1,
                                    onClick = { onChapterClick(chapter.id) },
                                    onMoveUp = { viewModel.moveChapter(chapter.id, moveUp = true) },
                                    onMoveDown = { viewModel.moveChapter(chapter.id, moveUp = false) },
                                    onDelete = { viewModel.deleteChapter(chapter.id) }
                                )
                            }
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
            GlassDialog(
                onDismissRequest = { showDeleteConfirmDialog = false },
                title = "Delete subject?",
                message = "This will remove ${uiState.subject?.name ?: "this subject"} and its chapters.",
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
    index: Int,
    isFirst: Boolean,
    isLast: Boolean,
    onClick: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    var menuExpanded by remember { mutableStateOf(false) }

    val isCompleted = chapter.status == ChapterStatus.COMPLETED || chapter.progress == 100
    val formattedIndex = String.format("%02d", index + 1)
    val stateText = when {
        isCompleted -> "Complete ✓"
        chapter.progress > 0 -> "${chapter.progress}%"
        else -> "Not started"
    }
    val stateColor = when {
        isCompleted -> colors.success
        chapter.progress > 0 -> colors.accent
        else -> colors.mutedText
    }

    GlassCard(
        onClick = onClick,
        backgroundColor = colors.glassSurface,
        padding = 14.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Chapter Number (01, 02, etc.)
            Text(
                text = formattedIndex,
                style = typography.caption.copy(fontWeight = FontWeight.SemiBold),
                color = colors.mutedText,
                modifier = Modifier.width(28.dp)
            )

            // Title & State
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = chapter.name,
                    style = typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = if (isCompleted) colors.secondaryText else colors.primaryText
                )
                Text(
                    text = stateText,
                    style = typography.caption.copy(fontWeight = FontWeight.Medium),
                    color = stateColor,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Overflow menu for chapter actions
            Box {
                GlassIconButton(
                    onClick = { menuExpanded = true },
                    contentDescription = "Chapter options",
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = null,
                        tint = colors.secondaryText,
                        modifier = Modifier.size(16.dp)
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(colors.surface)
                ) {
                    if (!isFirst) {
                        DropdownMenuItem(
                            text = { Text("Move up", style = typography.body, color = colors.primaryText) },
                            onClick = {
                                menuExpanded = false
                                onMoveUp()
                            }
                        )
                    }
                    if (!isLast) {
                        DropdownMenuItem(
                            text = { Text("Move down", style = typography.body, color = colors.primaryText) },
                            onClick = {
                                menuExpanded = false
                                onMoveDown()
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Delete chapter", style = typography.body, color = colors.accent) },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                contentDescription = null,
                tint = colors.mutedText,
                modifier = Modifier.size(14.dp)
            )
        }

        if (chapter.progress in 1..99) {
            Spacer(modifier = Modifier.height(8.dp))
            StudyOSProgressBar(
                progress = chapter.progress,
                height = 3.dp,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
