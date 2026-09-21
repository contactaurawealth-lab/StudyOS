package com.studyos.app.features.subjects.ui

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.studyos.app.core.ui.component.StudyOSButton
import com.studyos.app.core.ui.component.StudyOSEmptyState
import com.studyos.app.core.ui.component.StudyOSOutlinedButton
import com.studyos.app.core.ui.component.StudyOSProgressBar
import com.studyos.app.core.ui.component.StudyOSTextButton
import com.studyos.app.core.ui.component.StudyOSTextDialog
import com.studyos.app.domain.model.SubjectWithProgress
import com.studyos.app.features.onboarding.ui.AddSubjectBottomSheet
import com.studyos.app.features.subjects.viewmodel.SubjectFilter
import com.studyos.app.features.subjects.viewmodel.SubjectsViewModel
import com.studyos.app.theme.StudyOSTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectsScreen(
    viewModel: SubjectsViewModel,
    onSubjectClick: (String) -> Unit,
    onStartAiSession: (subjectId: String?, chapterId: String?) -> Unit = { _, _ -> },
    onStartRecall: (subjectId: String?) -> Unit = {},
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
                subtitle = if (uiState.subjects.isNotEmpty()) {
                    val count = uiState.subjects.size
                    val needsAttention = uiState.subjects.count { it.weakCount > 0 || it.readinessScore < 70 }
                    if (needsAttention > 0) {
                        "$count subjects • $needsAttention need attention"
                    } else {
                        "$count ${if (count == 1) "subject" else "subjects"}"
                    }
                } else null,
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
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 680.dp)
                            .padding(horizontal = StudyOSTheme.spacing.screenHorizontal)
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))

                        // Filter Chips Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SubjectFilterChip(
                                title = "All (${uiState.subjects.size})",
                                isSelected = uiState.selectedFilter == SubjectFilter.ALL,
                                onClick = { viewModel.setFilter(SubjectFilter.ALL) }
                            )

                            val attentionCount = uiState.subjects.count { it.weakCount > 0 || it.readinessScore < 70 }
                            SubjectFilterChip(
                                title = "⚠️ Needs Attention ($attentionCount)",
                                isSelected = uiState.selectedFilter == SubjectFilter.NEEDS_ATTENTION,
                                onClick = { viewModel.setFilter(SubjectFilter.NEEDS_ATTENTION) }
                            )

                            val dueCount = uiState.subjects.count { it.dueCount > 0 }
                            SubjectFilterChip(
                                title = "⚡ Due ($dueCount)",
                                isSelected = uiState.selectedFilter == SubjectFilter.DUE_FOR_REVIEW,
                                onClick = { viewModel.setFilter(SubjectFilter.DUE_FOR_REVIEW) }
                            )

                            val completedCount = uiState.subjects.count { it.progress == 100 }
                            SubjectFilterChip(
                                title = "✓ Completed ($completedCount)",
                                isSelected = uiState.selectedFilter == SubjectFilter.COMPLETED,
                                onClick = { viewModel.setFilter(SubjectFilter.COMPLETED) }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val displayList = uiState.filteredSubjects
                        if (displayList.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No subjects match this filter.",
                                    style = typography.body,
                                    color = colors.secondaryText
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(displayList, key = { it.subject.id }) { item ->
                                    SubjectRowItem(
                                        item = item,
                                        onClick = { onSubjectClick(item.subject.id) },
                                        onOpenDiagnostic = { viewModel.showDiagnostic(item) },
                                        onStartAiSession = { onStartAiSession(item.subject.id, item.weakChapterId) },
                                        onStartRecall = { onStartRecall(item.subject.id) },
                                        onRename = { subjectToRename = item },
                                        onDelete = { subjectToDelete = item }
                                    )
                                }
                            }
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

        // Subject Diagnostic Bottom Sheet
        uiState.selectedSubjectForDiagnostic?.let { diagnosticSubject ->
            SubjectDiagnosticBottomSheet(
                subject = diagnosticSubject,
                onDismissRequest = viewModel::dismissDiagnostic,
                onStartAiSession = { subId, chapId ->
                    viewModel.dismissDiagnostic()
                    onStartAiSession(subId, chapId)
                },
                onStartRecall = { subId ->
                    viewModel.dismissDiagnostic()
                    onStartRecall(subId)
                }
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
                message = "This will remove ${item.subject.name} and all its chapters from your study plan.",
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
private fun SubjectFilterChip(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    val backgroundColor = if (isSelected) colors.accent.copy(alpha = 0.16f) else colors.surface.copy(alpha = 0.6f)
    val borderColor = if (isSelected) colors.accent else colors.border
    val textColor = if (isSelected) colors.accent else colors.secondaryText

    Box(
        modifier = Modifier
            .clip(shapes.pill)
            .background(backgroundColor)
            .border(1.dp, borderColor, shapes.pill)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = typography.caption.copy(fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium),
            color = textColor
        )
    }
}

@Composable
private fun SubjectRowItem(
    item: SubjectWithProgress,
    onClick: () -> Unit,
    onOpenDiagnostic: () -> Unit,
    onStartAiSession: () -> Unit,
    onStartRecall: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes
    var menuExpanded by remember { mutableStateOf(false) }

    GlassCard(
        onClick = onClick,
        backgroundColor = colors.glassSurface,
        padding = 16.dp
    ) {
        // 1. Top row: Name + Exam Readiness Pill + More Menu
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

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Exam Readiness Pill
                Box(
                    modifier = Modifier
                        .clip(shapes.surface)
                        .background(colors.accent.copy(alpha = 0.12f))
                        .border(1.dp, colors.accent.copy(alpha = 0.3f), shapes.surface)
                        .clickable(onClick = onOpenDiagnostic)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "${item.readinessScore}% Ready",
                        style = typography.caption.copy(fontSize = 11.sp),
                        fontWeight = FontWeight.SemiBold,
                        color = colors.accent
                    )
                }

                // Diagnostic Analytics Icon
                GlassIconButton(
                    onClick = onOpenDiagnostic,
                    contentDescription = "Subject Diagnostic",
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Insights,
                        contentDescription = null,
                        tint = colors.secondaryText,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Box {
                    GlassIconButton(
                        onClick = { menuExpanded = true },
                        contentDescription = "Subject options",
                        modifier = Modifier.size(32.dp)
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
                        DropdownMenuItem(
                            text = { Text("View Intelligence", style = typography.body, color = colors.primaryText) },
                            onClick = {
                                menuExpanded = false
                                onOpenDiagnostic()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Edit subject", style = typography.body, color = colors.primaryText) },
                            onClick = {
                                menuExpanded = false
                                onRename()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete subject", style = typography.body, color = colors.critical) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 2. Syllabus progress label
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
                text = "$chapterCountText • Syllabus ${item.progress}%",
                style = typography.caption,
                color = colors.secondaryText
            )

            if (!item.insight.isNullOrBlank()) {
                Text(
                    text = item.insight,
                    style = typography.caption.copy(fontSize = 11.sp),
                    color = colors.mutedText,
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 3. Progress bar
        StudyOSProgressBar(
            progress = item.progress,
            height = 4.dp,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 4. Chapter Health Badges & 1-Tap Action Pills
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (item.strongCount > 0) {
                    Box(
                        modifier = Modifier
                            .clip(shapes.surface)
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
                            .clip(shapes.surface)
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
                            .clip(shapes.surface)
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
            }

            // Direct 1-Tap Action Pill
            when {
                item.dueCount > 0 -> {
                    Box(
                        modifier = Modifier
                            .clip(shapes.pill)
                            .background(colors.accent.copy(alpha = 0.15f))
                            .border(1.dp, colors.accent.copy(alpha = 0.4f), shapes.pill)
                            .clickable(onClick = onStartRecall)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Outlined.Bolt, null, tint = colors.accent, modifier = Modifier.size(12.dp))
                            Text(
                                text = "Recall Due",
                                style = typography.caption.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                                color = colors.accent
                            )
                        }
                    }
                }
                item.weakCount > 0 -> {
                    Box(
                        modifier = Modifier
                            .clip(shapes.pill)
                            .background(colors.surface.copy(alpha = 0.8f))
                            .border(1.dp, colors.border, shapes.pill)
                            .clickable(onClick = onStartAiSession)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Outlined.AutoAwesome, null, tint = colors.accent, modifier = Modifier.size(12.dp))
                            Text(
                                text = "Practice Weak",
                                style = typography.caption.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                                color = colors.primaryText
                            )
                        }
                    }
                }
                !item.currentChapterName.isNullOrBlank() -> {
                    Text(
                        text = "${item.currentChapterName} →",
                        style = typography.caption.copy(fontWeight = FontWeight.Medium),
                        color = colors.accent,
                        modifier = Modifier.clickable(onClick = onClick)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubjectDiagnosticBottomSheet(
    subject: SubjectWithProgress,
    onDismissRequest: () -> Unit,
    onStartAiSession: (subjectId: String?, chapterId: String?) -> Unit,
    onStartRecall: (subjectId: String?) -> Unit
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = colors.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = subject.subject.name,
                        style = typography.sectionTitle,
                        color = colors.primaryText
                    )
                    Text(
                        text = "Subject Intelligence Diagnostic",
                        style = typography.caption,
                        color = colors.secondaryText
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(shapes.surface)
                        .background(colors.accent.copy(alpha = 0.16f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${subject.readinessScore}% Ready",
                        style = typography.secondary,
                        fontWeight = FontWeight.Bold,
                        color = colors.accent
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // AI Insight box
            if (!subject.insight.isNullOrBlank()) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = colors.glassSurface,
                    padding = 12.dp
                ) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = subject.insight,
                            style = typography.caption,
                            color = colors.primaryText
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 4-Quadrant Intelligence Matrix
            Text(
                text = "Intelligence Matrix",
                style = typography.caption,
                fontWeight = FontWeight.SemiBold,
                color = colors.secondaryText
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DiagnosticMetricCard(
                    title = "Concept Understanding",
                    percentage = subject.understandingPercentage,
                    modifier = Modifier.weight(1f)
                )
                DiagnosticMetricCard(
                    title = "Active Recall Retention",
                    percentage = subject.recallPercentage,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DiagnosticMetricCard(
                    title = "Practice Quiz Accuracy",
                    percentage = subject.practicePercentage,
                    modifier = Modifier.weight(1f)
                )
                DiagnosticMetricCard(
                    title = "Syllabus Coverage",
                    percentage = subject.progress,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (subject.dueCount > 0) {
                    StudyOSOutlinedButton(
                        text = "Recall Due (${subject.dueCount})",
                        onClick = { onStartRecall(subject.subject.id) },
                        modifier = Modifier.weight(1f)
                    )
                }

                StudyOSButton(
                    text = "Start AI Study Session",
                    onClick = { onStartAiSession(subject.subject.id, subject.weakChapterId) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun DiagnosticMetricCard(
    title: String,
    percentage: Int,
    modifier: Modifier = Modifier
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography

    GlassCard(
        modifier = modifier,
        backgroundColor = colors.glassSurface,
        padding = 12.dp
    ) {
        Column {
            Text(
                text = title,
                style = typography.caption.copy(fontSize = 11.sp),
                color = colors.secondaryText,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$percentage%",
                    style = typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.primaryText
                )
                StudyOSProgressBar(
                    progress = percentage,
                    height = 4.dp,
                    modifier = Modifier.width(60.dp)
                )
            }
        }
    }
}
