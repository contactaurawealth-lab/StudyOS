package com.studyos.app.features.exams.ui

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studyos.app.core.ui.component.StudyOSButton
import com.studyos.app.core.ui.component.StudyOSConfirmationDialog
import com.studyos.app.core.ui.component.StudyOSEmptyState
import com.studyos.app.core.ui.component.StudyOSIconButton
import com.studyos.app.core.ui.component.StudyOSLoadingState
import com.studyos.app.core.ui.component.StudyOSOutlinedButton
import com.studyos.app.core.ui.component.StudyOSProgressBar
import com.studyos.app.domain.model.ExamChapterRevisionItem
import com.studyos.app.domain.model.ExamRevisionCategory
import com.studyos.app.domain.model.WeakTopic
import com.studyos.app.domain.model.WeakTopicAction
import com.studyos.app.features.exams.viewmodel.ExamViewModel
import com.studyos.app.theme.StudyOSTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamDetailScreen(
    examId: String,
    viewModel: ExamViewModel,
    onBack: () -> Unit,
    onOpenChapterPractice: (chapterId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes
    val snackbarHostState = remember { SnackbarHostState() }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(examId) {
        viewModel.loadExamDetail(examId)
    }

    LaunchedEffect(uiState.infoMessage) {
        uiState.infoMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearInfoMessage()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        if (uiState.isLoadingDetail && uiState.examDashboard == null) {
            StudyOSLoadingState(message = "Calculating revision priorities...")
        } else if (uiState.examDashboard == null) {
            StudyOSEmptyState(
                title = "Exam not found",
                description = "This exam may have been deleted.",
                actionButtonText = "Go back",
                onActionClick = onBack
            )
        } else {
            val dashboard = uiState.examDashboard!!
            val exam = dashboard.exam

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                // Top App Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StudyOSIconButton(
                            onClick = onBack,
                            contentDescription = "Back"
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = null,
                                tint = colors.primaryText,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column {
                            Text(
                                text = exam.name,
                                style = typography.sectionTitle,
                                color = colors.primaryText
                            )
                            val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
                            Text(
                                text = dateFormat.format(Date(exam.targetDate)),
                                style = typography.caption,
                                color = colors.secondaryText
                            )
                        }
                    }

                    Row {
                        StudyOSIconButton(
                            onClick = { viewModel.openEditExamSheet(exam) },
                            contentDescription = "Edit exam"
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
                            contentDescription = "Delete exam"
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

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Countdown Hero Card
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(shapes.surface)
                                .background(colors.surface)
                                .border(1.dp, colors.border, shapes.surface)
                                .padding(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "COUNTDOWN",
                                        style = typography.caption,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.secondaryText,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = when (dashboard.daysRemaining) {
                                            0L -> "Exam Today!"
                                            1L -> "Exam Tomorrow"
                                            else -> "${dashboard.daysRemaining} Days Left"
                                        },
                                        style = typography.screenTitle.copy(fontSize = 28.sp),
                                        color = colors.accent
                                    )
                                }

                                if (exam.targetScore != null) {
                                    Box(
                                        modifier = Modifier
                                            .clip(shapes.card)
                                            .background(colors.cardBackground)
                                            .border(1.dp, colors.border, shapes.card)
                                            .padding(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "${exam.targetScore}%",
                                                style = typography.sectionTitle,
                                                color = colors.primaryText
                                            )
                                            Text(
                                                text = "Target Score",
                                                style = typography.caption,
                                                color = colors.secondaryText
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Key Exam Metrics
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            DashboardMetricCard(
                                title = "Remaining",
                                value = "${dashboard.chaptersRemaining} Ch",
                                modifier = Modifier.weight(1f)
                            )
                            DashboardMetricCard(
                                title = "Due Cards",
                                value = "${dashboard.flashcardsDue}",
                                modifier = Modifier.weight(1f)
                            )
                            DashboardMetricCard(
                                title = "Quiz Acc.",
                                value = if (dashboard.quizAccuracy != null) "${dashboard.quizAccuracy}%" else "—",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Subject Readiness Breakdown
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(shapes.surface)
                                .background(colors.surface)
                                .border(1.dp, colors.border, shapes.surface)
                                .padding(18.dp)
                        ) {
                            Column {
                                Text(
                                    text = "Curriculum Readiness",
                                    style = typography.sectionTitle,
                                    color = colors.primaryText
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                dashboard.subjectProgresses.forEach { subProg ->
                                    Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = subProg.subject.name,
                                                style = typography.secondary,
                                                fontWeight = FontWeight.Medium,
                                                color = colors.primaryText
                                            )
                                            Text(
                                                text = "${subProg.progressPercentage}% (${subProg.completedChaptersCount}/${subProg.chaptersCount})",
                                                style = typography.caption,
                                                color = colors.secondaryText
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        StudyOSProgressBar(
                                            progress = subProg.progressPercentage,
                                            height = 5.dp,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Weak Topics Identified
                    if (dashboard.weakTopics.isNotEmpty()) {
                        item {
                            Text(
                                text = "Exam Focus Topics",
                                style = typography.sectionTitle,
                                color = colors.primaryText
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                dashboard.weakTopics.take(3).forEach { weak ->
                                    WeakTopicExamRow(weak)
                                }
                            }
                        }
                    }

                    // Revision Priority Section
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Revision Priority",
                                style = typography.sectionTitle,
                                color = colors.primaryText
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Category Filter Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterCategoryChip(
                                label = "All (${uiState.revisionChapters.size})",
                                isSelected = uiState.selectedCategoryFilter == null,
                                onClick = { viewModel.filterRevisionCategory(null) }
                            )
                            FilterCategoryChip(
                                label = "Need Revision (${uiState.needRevisionCount})",
                                isSelected = uiState.selectedCategoryFilter == ExamRevisionCategory.NEED_REVISION,
                                onClick = { viewModel.filterRevisionCategory(ExamRevisionCategory.NEED_REVISION) }
                            )
                            FilterCategoryChip(
                                label = "Practice (${uiState.practiceCount})",
                                isSelected = uiState.selectedCategoryFilter == ExamRevisionCategory.PRACTICE,
                                onClick = { viewModel.filterRevisionCategory(ExamRevisionCategory.PRACTICE) }
                            )
                            FilterCategoryChip(
                                label = "Strong (${uiState.strongCount})",
                                isSelected = uiState.selectedCategoryFilter == ExamRevisionCategory.STRONG,
                                onClick = { viewModel.filterRevisionCategory(ExamRevisionCategory.STRONG) }
                            )
                        }
                    }

                    // Chapters Priority List
                    val chapters = uiState.filteredRevisionChapters
                    if (chapters.isEmpty()) {
                        item {
                            Text(
                                text = "No chapters in this category.",
                                style = typography.caption,
                                color = colors.secondaryText,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        }
                    } else {
                        items(chapters, key = { it.chapter.id }) { item ->
                            RevisionChapterItem(
                                item = item,
                                onClick = { onOpenChapterPractice(item.chapter.id) }
                            )
                        }
                    }
                }
            }
        }

        // Edit Exam Bottom Sheet
        if (uiState.isCreateExamSheetOpen) {
            CreateExamBottomSheet(
                subjects = uiState.subjects,
                editingExam = uiState.editingExam,
                onDismiss = { viewModel.closeExamSheet() },
                onSave = { name, targetDate, subjectIds, targetScore, notes ->
                    viewModel.saveExam(name, targetDate, subjectIds, targetScore, notes)
                }
            )
        }

        // Delete Confirmation Dialog
        if (showDeleteConfirmDialog) {
            StudyOSConfirmationDialog(
                title = "Delete Exam Plan?",
                message = "Are you sure you want to delete this exam? Your subject chapters and notes will remain intact.",
                confirmButtonText = "Delete",
                onConfirm = {
                    showDeleteConfirmDialog = false
                    viewModel.deleteExam(examId)
                    onBack()
                },
                onDismiss = { showDeleteConfirmDialog = false }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun DashboardMetricCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    Box(
        modifier = modifier
            .clip(shapes.card)
            .background(colors.surface)
            .border(1.dp, colors.border, shapes.card)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                style = typography.sectionTitle,
                color = colors.primaryText
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                style = typography.caption,
                color = colors.secondaryText
            )
        }
    }
}

@Composable
private fun WeakTopicExamRow(weak: WeakTopic) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.card)
            .background(colors.surface)
            .border(1.dp, colors.border, shapes.card)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = weak.topic,
                    style = typography.secondary,
                    fontWeight = FontWeight.Medium,
                    color = colors.primaryText
                )
                Text(
                    text = "Recommended: ${when (weak.recommendedAction) {
                        WeakTopicAction.REVISE -> "Revise concept"
                        WeakTopicAction.FLASHCARDS -> "Drill flashcards"
                        WeakTopicAction.PRACTICE_QUIZ -> "Practice quiz"
                    }}",
                    style = typography.caption,
                    color = colors.secondaryText
                )
            }

            Box(
                modifier = Modifier
                    .clip(shapes.button)
                    .background(colors.cardBackground)
                    .border(1.dp, colors.border, shapes.button)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${weak.accuracyPercentage}% Acc",
                    style = typography.caption,
                    color = colors.accent
                )
            }
        }
    }
}

@Composable
private fun FilterCategoryChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    Box(
        modifier = Modifier
            .clip(shapes.button)
            .background(if (isSelected) colors.cardBackground else colors.surface)
            .border(1.dp, if (isSelected) colors.accent else colors.border, shapes.button)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = typography.caption.copy(fontSize = 11.sp),
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) colors.primaryText else colors.secondaryText
        )
    }
}

@Composable
private fun RevisionChapterItem(
    item: ExamChapterRevisionItem,
    onClick: () -> Unit
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    val categoryBadgeText = when (item.category) {
        ExamRevisionCategory.NEED_REVISION -> "Need Revision"
        ExamRevisionCategory.PRACTICE -> "Practice"
        ExamRevisionCategory.STRONG -> "Strong"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.card)
            .background(colors.surface)
            .border(1.dp, colors.border, shapes.card)
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(shapes.button)
                            .background(colors.cardBackground)
                            .border(1.dp, colors.border, shapes.button)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = categoryBadgeText,
                            style = typography.caption.copy(fontSize = 10.sp),
                            fontWeight = FontWeight.SemiBold,
                            color = if (item.category == ExamRevisionCategory.NEED_REVISION) colors.accent else colors.secondaryText
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = item.subject.name,
                        style = typography.caption,
                        color = colors.secondaryText
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.chapter.name,
                    style = typography.secondary,
                    fontWeight = FontWeight.Medium,
                    color = colors.primaryText
                )

                Spacer(modifier = Modifier.height(2.dp))

                val metrics = mutableListOf<String>()
                metrics.add("Progress ${item.chapter.progress}%")
                if (item.accuracyPercentage != null) metrics.add("Acc ${item.accuracyPercentage}%")
                if (item.mistakesCount > 0) metrics.add("${item.mistakesCount} mistakes")
                if (item.flashcardsDueCount > 0) metrics.add("${item.flashcardsDueCount} due")

                Text(
                    text = metrics.joinToString(" • "),
                    style = typography.caption,
                    color = colors.mutedText
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = null,
                tint = colors.secondaryText,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
