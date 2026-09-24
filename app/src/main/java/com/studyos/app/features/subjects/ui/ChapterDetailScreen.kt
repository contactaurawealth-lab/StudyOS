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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Psychology
import com.studyos.app.core.ui.component.StudyOSDivider
import com.studyos.app.domain.model.ActionableWeakConcept
import com.studyos.app.domain.model.ChapterIntelligence
import com.studyos.app.domain.model.IntelligentChapterStatus
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import com.studyos.app.core.ui.component.GlassCard
import com.studyos.app.core.ui.component.GlassChip
import com.studyos.app.core.ui.component.GlassDialog
import com.studyos.app.core.ui.component.GlassIconButton
import com.studyos.app.core.ui.component.GlassTopBar
import com.studyos.app.core.ui.component.ShimmerPlaceholder
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
    onAskAi: (chapterId: String) -> Unit = {},
    onOpenPractice: (chapterId: String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    var showEditSheet by remember { mutableStateOf(false) }
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
            .background(colors.background),
        contentAlignment = Alignment.TopCenter
    ) {
        if (uiState.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp)
                    .padding(horizontal = StudyOSTheme.spacing.screenHorizontal, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                repeat(3) {
                    ShimmerPlaceholder(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        shape = shapes.medium
                    )
                }
            }
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
                    .widthIn(max = 680.dp)
            ) {
                // Glass Top Bar: ← SubjectName ⋮
                GlassTopBar(
                    title = subjectName,
                    subtitle = chapter.name,
                    navigationIcon = {
                        GlassIconButton(
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
                    },
                    actions = {
                        Box {
                            GlassIconButton(
                                onClick = { menuExpanded = true },
                                contentDescription = "Chapter options"
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
                                val isCompleted = chapter.status == ChapterStatus.COMPLETED || chapter.progress == 100
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (isCompleted) "Mark in progress" else "Mark complete",
                                            style = typography.body,
                                            color = colors.primaryText
                                        )
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        if (isCompleted) {
                                            viewModel.updateStatus(ChapterStatus.IN_PROGRESS)
                                        } else {
                                            viewModel.updateStatus(ChapterStatus.COMPLETED)
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Reset progress", style = typography.body, color = colors.primaryText) },
                                    onClick = {
                                        menuExpanded = false
                                        viewModel.updateProgress(0)
                                        viewModel.updateStatus(ChapterStatus.NOT_STARTED)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Practice & Revision", style = typography.body, color = colors.primaryText) },
                                    onClick = {
                                        menuExpanded = false
                                        onOpenPractice(chapter.id)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Ask AI Assistant", style = typography.body, color = colors.primaryText) },
                                    onClick = {
                                        menuExpanded = false
                                        onAskAi(chapter.id)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Edit chapter", style = typography.body, color = colors.primaryText) },
                                    onClick = {
                                        menuExpanded = false
                                        showEditSheet = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete chapter", style = typography.body, color = colors.accent) },
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
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Chapter Title & Description Card
                    GlassCard(
                        backgroundColor = colors.glassSurface,
                        padding = 18.dp
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (chapter.status == ChapterStatus.COMPLETED || chapter.progress == 100) {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = "Completed",
                                    tint = colors.success,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = chapter.name,
                                style = typography.sectionTitle.copy(fontWeight = FontWeight.SemiBold),
                                color = colors.primaryText
                            )
                        }

                        if (!chapter.description.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = chapter.description,
                                style = typography.body,
                                color = colors.secondaryText
                            )
                        }
                    }

                    // Chapter Intelligence: 4 Dimensions, Status Badge, Weak Concepts, Timeline
                    uiState.intelligence?.let { intel ->
                        Spacer(modifier = Modifier.height(16.dp))
                        ChapterIntelligenceCard(
                            intelligence = intel,
                            onRecallConcept = { onOpenPractice(chapter.id) },
                            onReviewConcept = { onAskAi(chapter.id) },
                            onPracticeConcept = { onOpenPractice(chapter.id) }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Progress Control Card
                    GlassCard(
                        backgroundColor = colors.glassSurface,
                        padding = 18.dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Progress",
                                style = typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = colors.primaryText
                            )

                            Text(
                                text = "${chapter.progress}%",
                                style = typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (chapter.progress == 100) colors.success else colors.accent
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

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Status",
                            style = typography.caption,
                            color = colors.mutedText
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Quick Status Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            GlassChip(
                                text = "Not started",
                                selected = chapter.status == ChapterStatus.NOT_STARTED && chapter.progress == 0,
                                onClick = { viewModel.updateStatus(ChapterStatus.NOT_STARTED) },
                                modifier = Modifier.weight(1f)
                            )

                            GlassChip(
                                text = "In progress",
                                selected = chapter.status == ChapterStatus.IN_PROGRESS && chapter.progress in 1..99,
                                onClick = { viewModel.updateStatus(ChapterStatus.IN_PROGRESS) },
                                modifier = Modifier.weight(1f)
                            )

                            GlassChip(
                                text = "Completed",
                                selected = chapter.status == ChapterStatus.COMPLETED || chapter.progress == 100,
                                onClick = { viewModel.updateStatus(ChapterStatus.COMPLETED) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Practice & Revision Hub Button
                    StudyOSButton(
                        text = "Practice & Revision Hub",
                        onClick = { onOpenPractice(chapter.id) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Ask AI Button
                    StudyOSOutlinedButton(
                        text = "Ask AI about this chapter",
                        onClick = { onAskAi(chapter.id) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(48.dp))
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
                GlassDialog(
                    onDismissRequest = { showDeleteConfirmDialog = false },
                    title = "Delete chapter?",
                    message = "This will remove ${chapter.name} and its progress.",
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

@Composable
private fun ChapterIntelligenceCard(
    intelligence: ChapterIntelligence,
    onRecallConcept: (concept: String) -> Unit,
    onReviewConcept: (concept: String) -> Unit,
    onPracticeConcept: (concept: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    val statusColor = when (intelligence.status) {
        IntelligentChapterStatus.MASTERED -> colors.success
        IntelligentChapterStatus.STRONG -> colors.accent
        IntelligentChapterStatus.NEEDS_ATTENTION -> colors.critical
        IntelligentChapterStatus.REVISION_DUE -> colors.warning
        IntelligentChapterStatus.LEARNING -> colors.accent
        IntelligentChapterStatus.NOT_STARTED -> colors.mutedText
    }

    GlassCard(
        backgroundColor = colors.glassSurface,
        padding = 18.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header with Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Psychology,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "CHAPTER INTELLIGENCE",
                        style = typography.caption,
                        fontWeight = FontWeight.Bold,
                        color = colors.accent
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(shapes.surface)
                        .background(statusColor.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = intelligence.status.label,
                        style = typography.caption,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4 Quadrant Dimensions Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Understanding
                DimensionMetricItem(
                    label = "Understanding",
                    percentage = intelligence.understandingPercentage,
                    color = colors.accent,
                    modifier = Modifier.weight(1f)
                )
                // Recall
                DimensionMetricItem(
                    label = "Recall",
                    percentage = intelligence.recallPercentage,
                    color = if (intelligence.recallPercentage < 60 && intelligence.completionPercentage > 60) colors.critical else colors.accent,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Practice
                DimensionMetricItem(
                    label = "Practice",
                    percentage = intelligence.practicePercentage,
                    color = colors.accent,
                    modifier = Modifier.weight(1f)
                )
                // Completion
                DimensionMetricItem(
                    label = "Completion",
                    percentage = intelligence.completionPercentage,
                    color = if (intelligence.completionPercentage == 100) colors.success else colors.accent,
                    modifier = Modifier.weight(1f)
                )
            }

            // Disparity Warning
            if (intelligence.status == IntelligentChapterStatus.NEEDS_ATTENTION) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shapes.surface)
                        .background(colors.critical.copy(alpha = 0.10f))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "Attention: Syllabus completion is high, but recall accuracy is below 60%. Prioritize active recall to prevent memory decay.",
                        style = typography.caption,
                        color = colors.critical
                    )
                }
            }

            // Timeline
            Spacer(modifier = Modifier.height(14.dp))
            StudyOSDivider()
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Last studied: ${intelligence.lastStudiedText}",
                        style = typography.caption,
                        color = colors.secondaryText
                    )
                    Text(
                        text = "Last recalled: ${intelligence.lastRecalledText}",
                        style = typography.caption,
                        color = colors.secondaryText
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(shapes.surface)
                        .background(colors.surface)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Review: ${intelligence.nextReviewText}",
                        style = typography.caption,
                        fontWeight = FontWeight.Medium,
                        color = colors.accent
                    )
                }
            }

            // Actionable Weak Concepts
            if (intelligence.actionableWeakConcepts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                StudyOSDivider()
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Weak Concepts Identified",
                    style = typography.caption,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.secondaryText
                )

                Spacer(modifier = Modifier.height(6.dp))

                for (conceptItem in intelligence.actionableWeakConcepts) {
                    WeakConceptRow(
                        item = conceptItem,
                        onRecall = { onRecallConcept(conceptItem.concept) },
                        onReview = { onReviewConcept(conceptItem.concept) },
                        onPractice = { onPracticeConcept(conceptItem.concept) }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun DimensionMetricItem(
    label: String,
    percentage: Int,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    Box(
        modifier = modifier
            .clip(shapes.surface)
            .background(colors.surface)
            .padding(10.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = typography.caption,
                    color = colors.secondaryText
                )
                Text(
                    text = "$percentage%",
                    style = typography.caption,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            StudyOSProgressBar(
                progress = percentage,
                height = 4.dp,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun WeakConceptRow(
    item: ActionableWeakConcept,
    onRecall: () -> Unit,
    onReview: () -> Unit,
    onPractice: () -> Unit
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.surface)
            .background(colors.surface.copy(alpha = 0.6f))
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.concept,
                    style = typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = colors.primaryText,
                    maxLines = 1
                )
                Text(
                    text = "${item.missCount} mistakes logged",
                    style = typography.caption,
                    color = colors.critical
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier
                        .clip(shapes.surface)
                        .background(colors.accent.copy(alpha = 0.12f))
                        .clickable(onClick = onRecall)
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text("Recall", style = typography.caption, color = colors.accent, fontWeight = FontWeight.SemiBold)
                }
                Box(
                    modifier = Modifier
                        .clip(shapes.surface)
                        .background(colors.surface)
                        .clickable(onClick = onReview)
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text("Review", style = typography.caption, color = colors.secondaryText, fontWeight = FontWeight.SemiBold)
                }
                Box(
                    modifier = Modifier
                        .clip(shapes.surface)
                        .background(colors.surface)
                        .clickable(onClick = onPractice)
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text("Practice", style = typography.caption, color = colors.secondaryText, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
