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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
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
import com.studyos.app.core.ui.component.GlassDialog
import com.studyos.app.core.ui.component.GlassIconButton
import com.studyos.app.core.ui.component.GlassTopBar
import com.studyos.app.core.ui.component.ShimmerPlaceholder
import com.studyos.app.core.ui.component.StudyOSEmptyState
import com.studyos.app.core.ui.component.StudyOSIconButton
import com.studyos.app.core.ui.component.StudyOSLoadingState
import com.studyos.app.core.ui.component.StudyOSOutlinedButton
import com.studyos.app.core.ui.component.StudyOSProgressBar
import com.studyos.app.core.ui.component.StudyOSTextButton
import com.studyos.app.core.ui.component.StudyOSTextDialog
import com.studyos.app.domain.model.SubjectWithProgress
import com.studyos.app.features.onboarding.ui.AddSubjectBottomSheet
import com.studyos.app.features.subjects.viewmodel.SubjectsViewModel
import com.studyos.app.theme.StudyOSTheme

@Composable
fun SubjectsScreen(
    viewModel: SubjectsViewModel,
    onSubjectClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    var showAddSheet by remember { mutableStateOf(false) }
    var subjectToRename by remember { mutableStateOf<SubjectWithProgress?>(null) }
    var subjectToDelete by remember { mutableStateOf<SubjectWithProgress?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

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
        Column(modifier = Modifier.fillMaxSize()) {
            GlassTopBar(
                title = "Subjects",
                subtitle = if (uiState.subjects.isNotEmpty()) "${uiState.subjects.size} ${if (uiState.subjects.size == 1) "subject" else "subjects"}" else null,
                actions = {
                    GlassIconButton(
                        onClick = { showAddSheet = true },
                        contentDescription = "Add Subject"
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = null,
                            tint = colors.primaryText,
                            modifier = Modifier.size(20.dp)
                        )
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
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 680.dp)
                            .padding(horizontal = StudyOSTheme.spacing.screenHorizontal, vertical = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        repeat(4) {
                            ShimmerPlaceholder(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(96.dp),
                                shape = shapes.medium
                            )
                        }
                    }
                } else if (uiState.subjects.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 680.dp)
                            .padding(horizontal = StudyOSTheme.spacing.screenHorizontal, vertical = 28.dp),
                        verticalArrangement = Arrangement.Top,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        StudyOSEmptyState(
                            title = "No subjects yet",
                            description = "Add your first subject to start building your study plan.",
                            actionButtonText = "Add Subject",
                            onActionClick = { showAddSheet = true }
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            StudyOSTextButton(
                                text = "Load sample subjects & chapters",
                                onClick = { viewModel.loadSampleData() }
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 680.dp)
                            .padding(horizontal = StudyOSTheme.spacing.screenHorizontal),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(uiState.subjects, key = { it.subject.id }) { item ->
                            SubjectRowItem(
                                item = item,
                                onClick = { onSubjectClick(item.subject.id) },
                                onRename = { subjectToRename = item },
                                onDelete = { subjectToDelete = item }
                            )
                        }
                    }
                }
            }
        }

        // Add Subject Sheet
        if (showAddSheet) {
            AddSubjectBottomSheet(
                onDismissRequest = {
                    showAddSheet = false
                    viewModel.clearMessages()
                },
                onAddSubject = { name ->
                    val success = viewModel.addSubject(name)
                    if (success) {
                        showAddSheet = false
                    }
                    success
                },
                errorMessage = uiState.errorMessage,
                onClearError = viewModel::clearMessages
            )
        }

        // Rename Subject Dialog
        subjectToRename?.let { item: SubjectWithProgress ->
            StudyOSTextDialog(
                title = "Edit subject",
                initialValue = item.subject.name,
                placeholder = "Subject name",
                confirmButtonText = "Save",
                onConfirm = { newName: String ->
                    val success = viewModel.renameSubject(item.subject.id, newName)
                    if (success) {
                        subjectToRename = null
                    }
                },
                onDismiss = {
                    subjectToRename = null
                    viewModel.clearMessages()
                },
                errorMessage = uiState.errorMessage
            )
        }

        // Delete Subject Confirmation Dialog
        subjectToDelete?.let { item: SubjectWithProgress ->
            GlassDialog(
                onDismissRequest = { subjectToDelete = null },
                title = "Delete subject?",
                message = "This will remove ${item.subject.name} and its chapters from your study plan.",
                confirmButtonText = "Delete",
                dismissButtonText = "Cancel",
                isDestructive = true,
                onConfirm = {
                    viewModel.deleteSubject(item.subject.id)
                    subjectToDelete = null
                },
                onDismiss = { subjectToDelete = null }
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
private fun SubjectRowItem(
    item: SubjectWithProgress,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    var menuExpanded by remember { mutableStateOf(false) }

    GlassCard(
        onClick = onClick,
        backgroundColor = colors.glassSurface,
        padding = 16.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.subject.name,
                style = typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = colors.primaryText,
                modifier = Modifier.weight(1f)
            )

            Box {
                GlassIconButton(
                    onClick = { menuExpanded = true },
                    contentDescription = "Subject options",
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = null,
                        tint = colors.secondaryText,
                        modifier = Modifier.size(18.dp)
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
                            onRename()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete subject", style = typography.body, color = colors.accent) },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val chapterCountText = when (item.chapterCount) {
                0 -> "0 chapters"
                1 -> "1 chapter"
                else -> "${item.chapterCount} chapters"
            }

            Text(
                text = chapterCountText,
                style = typography.caption,
                color = colors.secondaryText
            )

            Text(
                text = "${item.progress}%",
                style = typography.caption.copy(fontWeight = FontWeight.Medium),
                color = colors.secondaryText
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        StudyOSProgressBar(
            progress = item.progress,
            height = 4.dp,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Chapter Health Badges
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (item.strongCount > 0) {
                Box(
                    modifier = Modifier
                        .clip(StudyOSTheme.shapes.surface)
                        .background(colors.success.copy(alpha = 0.12f))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${item.strongCount} Strong",
                        style = typography.caption.copy(fontSize = 11.sp),
                        color = colors.success,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (item.weakCount > 0) {
                Box(
                    modifier = Modifier
                        .clip(StudyOSTheme.shapes.surface)
                        .background(colors.critical.copy(alpha = 0.12f))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${item.weakCount} Weak",
                        style = typography.caption.copy(fontSize = 11.sp),
                        color = colors.critical,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (item.dueCount > 0) {
                Box(
                    modifier = Modifier
                        .clip(StudyOSTheme.shapes.surface)
                        .background(colors.warning.copy(alpha = 0.12f))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${item.dueCount} Due",
                        style = typography.caption.copy(fontSize = 11.sp),
                        color = colors.warning,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            if (!item.currentChapterName.isNullOrBlank()) {
                Text(
                    text = "${item.currentChapterName} →",
                    style = typography.caption.copy(fontWeight = FontWeight.Medium),
                    color = colors.accent
                )
            }
        }
    }
}
