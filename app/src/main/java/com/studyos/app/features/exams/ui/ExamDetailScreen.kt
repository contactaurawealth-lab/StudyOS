package com.studyos.app.features.exams.ui

import android.content.Intent
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Grade
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studyos.app.core.ui.component.StudyOSButton
import com.studyos.app.core.ui.component.StudyOSConfirmationDialog
import com.studyos.app.core.ui.component.StudyOSEmptyState
import com.studyos.app.core.ui.component.StudyOSIconButton
import com.studyos.app.core.ui.component.StudyOSLoadingState
import com.studyos.app.core.ui.component.StudyOSOutlinedButton
import com.studyos.app.core.ui.component.StudyOSProgressBar
import com.studyos.app.core.ui.component.StudyOSTextField
import com.studyos.app.domain.model.Exam
import com.studyos.app.domain.model.ExamChapterRevisionItem
import com.studyos.app.domain.model.ExamDashboardItem
import com.studyos.app.domain.model.ExamRevisionCategory
import com.studyos.app.domain.model.QuizAttempt
import com.studyos.app.domain.model.WeakTopic
import com.studyos.app.domain.model.WeakTopicAction
import com.studyos.app.features.exams.viewmodel.ExamViewModel
import com.studyos.app.theme.StudyOSTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

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
    var showScoreDialog by remember { mutableStateOf(false) }
    var scoreInputText by remember { mutableStateOf("") }
    var isCompletedChecked by remember { mutableStateOf(false) }

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
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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

                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = exam.name,
                                style = typography.sectionTitle,
                                color = colors.primaryText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
                            Text(
                                text = dateFormat.format(Date(exam.targetDate)),
                                style = typography.caption,
                                color = colors.secondaryText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        val context = LocalContext.current
                        val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())

                        StudyOSIconButton(
                            onClick = {
                                val avgProgress = if (dashboard.subjectProgresses.isNotEmpty()) dashboard.subjectProgresses.map { it.progressPercentage }.average().toInt() else 0
                                val shareText = "📊 My ${exam.name} Exam Readiness is at ${avgProgress}% completion on StudyOS! Days left: ${dashboard.daysRemaining}. Target Date: ${dateFormat.format(Date(exam.targetDate))}. 📚 #StudyOS"
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share Exam Readiness"))
                            },
                            contentDescription = "Share exam readiness"
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Share,
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(18.dp)
                            )
                        }

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
                                        text = when {
                                            dashboard.daysRemaining < 0L -> "Exam Past Due"
                                            dashboard.daysRemaining == 0L -> "Exam Today!"
                                            dashboard.daysRemaining == 1L -> "Exam Tomorrow"
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

                    // Multi-Dimension Readiness Radar / Index Card
                    item {
                        ReadinessRadarCard(
                            dashboard = dashboard,
                            revisionChapters = uiState.revisionChapters,
                            recentMockAttempts = uiState.recentMockAttempts
                        )
                    }

                    // Mock Test Performance & Score Trendline Card
                    item {
                        MockScoreTrendlineCard(
                            exam = exam,
                            recentAttempts = uiState.recentMockAttempts,
                            onLogScore = {
                                scoreInputText = exam.actualScore?.toString() ?: ""
                                isCompletedChecked = exam.isCompleted
                                showScoreDialog = true
                            }
                        )
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

                    // Interactive Syllabus Revision Checklist Section
                    item {
                        val totalCh = uiState.revisionChapters.size
                        val revCh = uiState.revisedChaptersCount
                        val revPct = if (totalCh > 0) ((revCh * 100f) / totalCh.toFloat()).roundToInt().coerceIn(0, 100) else 0

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Syllabus Revision Checklist",
                                    style = typography.sectionTitle,
                                    color = colors.primaryText
                                )

                                Text(
                                    text = "$revCh of $totalCh Mastered ($revPct%)",
                                    style = typography.caption,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (revPct == 100) colors.accent else colors.secondaryText
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            StudyOSProgressBar(
                                progress = revPct,
                                height = 5.dp,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(12.dp))

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
                                onToggleRevised = { viewModel.toggleChapterRevised(item.chapter.id, item.chapter.progress) },
                                onClick = { onOpenChapterPractice(item.chapter.id) }
                            )
                        }
                    }

                    // Exam Day Kit Checklist Card
                    item {
                        var admitCardChecked by rememberSaveable(exam.id) { mutableStateOf(false) }
                        var stationeryChecked by rememberSaveable(exam.id) { mutableStateOf(false) }
                        var toolsChecked by rememberSaveable(exam.id) { mutableStateOf(false) }
                        var cheatSheetChecked by rememberSaveable(exam.id) { mutableStateOf(false) }
                        var waterWatchChecked by rememberSaveable(exam.id) { mutableStateOf(false) }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(shapes.surface)
                                .background(colors.surface)
                                .border(1.dp, colors.border, shapes.surface)
                                .padding(18.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Outlined.ListAlt,
                                            contentDescription = null,
                                            tint = colors.accent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Exam Day Kit Checklist",
                                            style = typography.sectionTitle,
                                            color = colors.primaryText
                                        )
                                    }

                                    val kitDoneCount = listOf(admitCardChecked, stationeryChecked, toolsChecked, cheatSheetChecked, waterWatchChecked).count { it }
                                    Text(
                                        text = "$kitDoneCount / 5 Ready",
                                        style = typography.caption,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (kitDoneCount == 5) colors.accent else colors.secondaryText
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                KitItemRow(
                                    title = "Admit Card / Hall Ticket / Photo ID",
                                    checked = admitCardChecked,
                                    onCheckedChange = { admitCardChecked = it }
                                )
                                KitItemRow(
                                    title = "Pens (Blue/Black ballpoint), Pencils & Eraser",
                                    checked = stationeryChecked,
                                    onCheckedChange = { stationeryChecked = it }
                                )
                                KitItemRow(
                                    title = "Approved Calculator / Geometry Box (if required)",
                                    checked = toolsChecked,
                                    onCheckedChange = { toolsChecked = it }
                                )
                                KitItemRow(
                                    title = "Quick Formula Sheet & Summary Notes",
                                    checked = cheatSheetChecked,
                                    onCheckedChange = { cheatSheetChecked = it }
                                )
                                KitItemRow(
                                    title = "Transparent Water Bottle & Analog Watch",
                                    checked = waterWatchChecked,
                                    onCheckedChange = { waterWatchChecked = it }
                                )
                            }
                        }
                    }

                    // 7-Day Final Revision Roadmap Card
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.CalendarMonth,
                                        contentDescription = null,
                                        tint = colors.accent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "7-Day Final Sprint Roadmap",
                                        style = typography.sectionTitle,
                                        color = colors.primaryText
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                RoadmapDayItem(
                                    dayRange = "Days 7 – 5",
                                    task = "High-Priority Revision",
                                    detail = "Deep-dive into '${uiState.revisionChapters.firstOrNull { it.category == ExamRevisionCategory.NEED_REVISION }?.chapter?.name ?: "Need Revision chapters"}' and solve fundamental examples."
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                RoadmapDayItem(
                                    dayRange = "Days 4 – 3",
                                    task = "Mistakes & Weak Topics Drill",
                                    detail = "Drill identified weak topics (${dashboard.weakTopics.size} flagged) and review your Mistake Notebook."
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                RoadmapDayItem(
                                    dayRange = "Day 2",
                                    task = "Timed Mock Quiz Simulation",
                                    detail = "Simulate actual test timing and review rapid flashcard recall across all subjects."
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                RoadmapDayItem(
                                    dayRange = "Day 1 (Eve)",
                                    task = "Light Summary & Kit Prep",
                                    detail = "Scan key formulas, organize Exam Day Kit, and get 8 hours of solid sleep."
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                RoadmapDayItem(
                                    dayRange = "Day 0",
                                    task = "Exam Day Execution",
                                    detail = "Stay calm, review cheat sheet before entering, and pace time carefully."
                                )
                            }
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

        // Record Exam Score Dialog
        if (showScoreDialog) {
            AlertDialog(
                onDismissRequest = { showScoreDialog = false },
                title = {
                    Text(
                        text = "Record Exam Score",
                        style = typography.sectionTitle,
                        color = colors.primaryText
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "Enter your achieved percentage or score (0 - 100):",
                            style = typography.caption,
                            color = colors.secondaryText
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        StudyOSTextField(
                            value = scoreInputText,
                            onValueChange = { scoreInputText = it },
                            placeholder = "e.g. 85"
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { isCompletedChecked = !isCompletedChecked }
                        ) {
                            Checkbox(
                                checked = isCompletedChecked,
                                onCheckedChange = { isCompletedChecked = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = colors.accent,
                                    uncheckedColor = colors.secondaryText,
                                    checkmarkColor = colors.background
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Mark exam as completed",
                                style = typography.secondary,
                                color = colors.primaryText
                            )
                        }
                    }
                },
                confirmButton = {
                    StudyOSButton(
                        text = "Save",
                        onClick = {
                            val parsed = scoreInputText.trim().toIntOrNull()
                            viewModel.saveExamResult(examId, parsed, isCompletedChecked)
                            showScoreDialog = false
                        }
                    )
                },
                dismissButton = {
                    StudyOSOutlinedButton(
                        text = "Cancel",
                        onClick = { showScoreDialog = false }
                    )
                },
                containerColor = colors.surface
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun ReadinessRadarCard(
    dashboard: ExamDashboardItem,
    revisionChapters: List<ExamChapterRevisionItem>,
    recentMockAttempts: List<QuizAttempt>
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    val rawTotalChapters = dashboard.subjectProgresses.sumOf { it.chaptersCount }
    val totalChapters = rawTotalChapters.coerceAtLeast(1)
    val completedChapters = dashboard.subjectProgresses.sumOf { it.completedChaptersCount }
    val syllabusPct = if (rawTotalChapters > 0) {
        ((completedChapters * 100f) / totalChapters.toFloat()).roundToInt().coerceIn(0, 100)
    } else 0

    val accuracyPct = dashboard.quizAccuracy
        ?: (if (recentMockAttempts.isNotEmpty()) recentMockAttempts.map { it.accuracyPercentage.toDouble() }.average().roundToInt().coerceIn(0, 100) else 0)

    val dueFlashcards = dashboard.flashcardsDue
    val retentionPct = (100 - (dueFlashcards * 4)).coerceIn(15, 100)

    val weakCount = dashboard.weakTopics.size
    val masteryPct = (100 - (weakCount * 15)).coerceIn(20, 100)

    val readinessScore = ((syllabusPct * 0.40f) + (accuracyPct * 0.30f) + (retentionPct * 0.15f) + (masteryPct * 0.15f)).roundToInt().coerceIn(0, 100)

    val tierLabel = when {
        readinessScore >= 80 -> "EXAM READY 🎯"
        readinessScore >= 60 -> "ON TRACK 📈"
        readinessScore >= 40 -> "NEEDS FOCUS ⚡"
        else -> "CRITICAL REVISION ⚠️"
    }

    val tierColor = when {
        readinessScore >= 80 -> colors.accent
        readinessScore >= 60 -> colors.accent
        readinessScore >= 40 -> Color(0xFFD39A3A)
        else -> Color(0xFFFF6B6B)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.surface)
            .background(colors.surface)
            .border(1.dp, colors.border, shapes.surface)
            .padding(18.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ShowChart,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Exam Readiness Index",
                        style = typography.sectionTitle,
                        color = colors.primaryText
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(shapes.button)
                        .background(tierColor.copy(alpha = 0.15f))
                        .border(0.5.dp, tierColor.copy(alpha = 0.4f), shapes.button)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = tierLabel,
                        style = typography.caption.copy(fontSize = 10.sp),
                        fontWeight = FontWeight.Bold,
                        color = tierColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Score Banner
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "$readinessScore",
                            style = typography.screenTitle.copy(fontSize = 36.sp),
                            fontWeight = FontWeight.Bold,
                            color = tierColor
                        )
                        Text(
                            text = "/100",
                            style = typography.body,
                            color = colors.secondaryText,
                            modifier = Modifier.padding(bottom = 6.dp, start = 2.dp)
                        )
                    }
                    Text(
                        text = "Composite weighted score based on 4 prep dimensions",
                        style = typography.caption,
                        color = colors.secondaryText
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4 Dimension Progress Meters
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DimensionMeterRow(title = "Syllabus Covered", percentage = syllabusPct, detail = "$completedChapters of $totalChapters Ch")
                DimensionMeterRow(title = "Mock / Quiz Accuracy", percentage = accuracyPct, detail = if (accuracyPct > 0) "$accuracyPct% avg" else "No tests yet")
                DimensionMeterRow(title = "Active Recall Retention", percentage = retentionPct, detail = if (dueFlashcards == 0) "All cards clear" else "$dueFlashcards cards due")
                DimensionMeterRow(title = "Weak Topic Mastery", percentage = masteryPct, detail = if (weakCount == 0) "Zero weak spots" else "$weakCount flagged topics")
            }
        }
    }
}

@Composable
private fun DimensionMeterRow(
    title: String,
    percentage: Int,
    detail: String
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = typography.caption,
                fontWeight = FontWeight.Medium,
                color = colors.primaryText
            )
            Text(
                text = "$percentage% ($detail)",
                style = typography.caption.copy(fontSize = 11.sp),
                color = colors.secondaryText
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        StudyOSProgressBar(
            progress = percentage,
            height = 4.dp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun MockScoreTrendlineCard(
    exam: Exam,
    recentAttempts: List<QuizAttempt>,
    onLogScore: () -> Unit
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    val bestAttemptScore = recentAttempts.maxOfOrNull { it.accuracyPercentage }
    val latestAttemptScore = recentAttempts.firstOrNull()?.accuracyPercentage

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.surface)
            .background(colors.surface)
            .border(1.dp, if (exam.actualScore != null || recentAttempts.isNotEmpty()) colors.accent.copy(alpha = 0.4f) else colors.border, shapes.surface)
            .padding(18.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Grade,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Mock Tests & Performance Trend",
                        style = typography.sectionTitle,
                        color = colors.primaryText
                    )
                }

                StudyOSButton(
                    text = if (exam.actualScore != null) "Edit Score" else "Log Score",
                    onClick = onLogScore
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Score Metrics Summary Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(shapes.card)
                        .background(colors.cardBackground)
                        .border(1.dp, colors.border, shapes.card)
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (latestAttemptScore != null) "$latestAttemptScore%" else if (exam.actualScore != null) "${exam.actualScore}%" else "—",
                            style = typography.sectionTitle,
                            color = colors.primaryText
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Latest Mock",
                            style = typography.caption.copy(fontSize = 10.sp),
                            color = colors.secondaryText
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(shapes.card)
                        .background(colors.cardBackground)
                        .border(1.dp, colors.border, shapes.card)
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (bestAttemptScore != null) "$bestAttemptScore%" else if (exam.actualScore != null) "${exam.actualScore}%" else "—",
                            style = typography.sectionTitle,
                            color = colors.accent
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Best Score",
                            style = typography.caption.copy(fontSize = 10.sp),
                            color = colors.secondaryText
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(shapes.card)
                        .background(colors.cardBackground)
                        .border(1.dp, colors.border, shapes.card)
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (exam.targetScore != null) "${exam.targetScore}%" else "—",
                            style = typography.sectionTitle,
                            color = colors.primaryText
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Target Score",
                            style = typography.caption.copy(fontSize = 10.sp),
                            color = colors.secondaryText
                        )
                    }
                }

                if (exam.targetScore != null && (latestAttemptScore != null || exam.actualScore != null)) {
                    val currentVal = latestAttemptScore ?: exam.actualScore!!
                    val delta = currentVal - exam.targetScore
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(shapes.card)
                            .background(colors.cardBackground)
                            .border(1.dp, colors.border, shapes.card)
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (delta >= 0) "+$delta%" else "$delta%",
                                style = typography.sectionTitle,
                                color = if (delta >= 0) colors.accent else Color(0xFFFF6B6B)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Delta",
                                style = typography.caption.copy(fontSize = 10.sp),
                                color = colors.secondaryText
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Canvas Score Trendline
            MockScoreTrendlineView(
                attempts = recentAttempts,
                targetScore = exam.targetScore,
                actualScore = exam.actualScore
            )
        }
    }
}

@Composable
private fun MockScoreTrendlineView(
    attempts: List<QuizAttempt>,
    targetScore: Int?,
    actualScore: Int?,
    modifier: Modifier = Modifier
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    val scores = remember(attempts, actualScore) {
        val list = attempts.sortedBy { it.completedAt ?: it.startedAt }.map { it.accuracyPercentage }
        if (list.isEmpty() && actualScore != null) {
            listOf(actualScore)
        } else {
            list
        }
    }

    if (scores.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(shapes.card)
                .background(colors.cardBackground)
                .border(0.5.dp, colors.border.copy(alpha = 0.4f), shapes.card)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No mock tests recorded yet. Practice tests to see your performance trendline.",
                style = typography.caption,
                color = colors.secondaryText
            )
        }
        return
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(shapes.card)
                .background(colors.cardBackground)
                .border(0.5.dp, colors.border.copy(alpha = 0.4f), shapes.card)
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val maxScore = 100f

                // Draw target score dashed line
                if (targetScore != null && targetScore in 0..100) {
                    val targetY = height - (targetScore / maxScore * height)
                    drawLine(
                        color = colors.accent.copy(alpha = 0.5f),
                        start = Offset(0f, targetY),
                        end = Offset(width, targetY),
                        strokeWidth = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
                    )
                }

                if (scores.size == 1) {
                    val y = height - (scores[0].coerceIn(0, 100) / maxScore * height)
                    drawCircle(
                        color = colors.accent,
                        radius = 6.dp.toPx(),
                        center = Offset(width / 2f, y)
                    )
                } else {
                    val stepX = width / (scores.size - 1)
                    val points = scores.mapIndexed { idx, s ->
                        val x = idx * stepX
                        val y = height - (s.coerceIn(0, 100) / maxScore * height)
                        Offset(x, y)
                    }

                    val path = Path()
                    path.moveTo(points.first().x, points.first().y)
                    for (i in 1 until points.size) {
                        val prev = points[i - 1]
                        val curr = points[i]
                        val cx = (prev.x + curr.x) / 2f
                        path.cubicTo(cx, prev.y, cx, curr.y, curr.x, curr.y)
                    }

                    drawPath(
                        path = path,
                        color = colors.accent,
                        style = Stroke(
                            width = 3.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    )

                    points.forEach { pt ->
                        drawCircle(
                            color = colors.surface,
                            radius = 5.dp.toPx(),
                            center = pt
                        )
                        drawCircle(
                            color = colors.accent,
                            radius = 3.5.dp.toPx(),
                            center = pt
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val latest = scores.lastOrNull()
            val initial = scores.firstOrNull()
            val gain = if (latest != null && initial != null) latest - initial else 0

            Text(
                text = "${scores.size} Attempt${if (scores.size > 1) "s" else ""} Logged",
                style = typography.caption,
                color = colors.secondaryText
            )

            if (scores.size > 1) {
                Text(
                    text = if (gain >= 0) "Growth: +$gain% 🚀" else "Trend: $gain%",
                    style = typography.caption,
                    fontWeight = FontWeight.SemiBold,
                    color = if (gain >= 0) colors.accent else colors.secondaryText
                )
            }
        }
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
    onToggleRevised: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes
    val isMastered = item.chapter.progress >= 100

    val categoryBadgeText = when {
        isMastered -> "Mastered ✓"
        item.category == ExamRevisionCategory.NEED_REVISION -> "Need Revision"
        item.category == ExamRevisionCategory.PRACTICE -> "Practice"
        else -> "Strong"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.card)
            .background(colors.surface)
            .border(1.dp, if (isMastered) colors.accent.copy(alpha = 0.35f) else colors.border, shapes.card)
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isMastered,
                    onCheckedChange = onToggleRevised,
                    colors = CheckboxDefaults.colors(
                        checkedColor = colors.accent,
                        uncheckedColor = colors.secondaryText,
                        checkmarkColor = colors.background
                    )
                )

                Spacer(modifier = Modifier.width(6.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(shapes.button)
                                .background(if (isMastered) colors.accent.copy(alpha = 0.15f) else colors.cardBackground)
                                .border(
                                    0.5.dp,
                                    if (isMastered) colors.accent else colors.border,
                                    shapes.button
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = categoryBadgeText,
                                style = typography.caption.copy(fontSize = 10.sp),
                                fontWeight = FontWeight.SemiBold,
                                color = when {
                                    isMastered -> colors.accent
                                    item.category == ExamRevisionCategory.NEED_REVISION -> Color(0xFFD39A3A)
                                    else -> colors.secondaryText
                                }
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = item.subject.name,
                            style = typography.caption,
                            color = colors.secondaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = item.chapter.name,
                        style = typography.secondary,
                        fontWeight = FontWeight.Medium,
                        color = if (isMastered) colors.secondaryText else colors.primaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    val metrics = mutableListOf<String>()
                    metrics.add("Progress ${item.chapter.progress}%")
                    if (item.accuracyPercentage != null) metrics.add("${item.accuracyPercentage}% Acc")
                    if (item.mistakesCount > 0) metrics.add("${item.mistakesCount} mistakes")
                    if (item.flashcardsDueCount > 0) metrics.add("${item.flashcardsDueCount} due")

                    Text(
                        text = metrics.joinToString(" • "),
                        style = typography.caption,
                        color = colors.mutedText
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = "Open Chapter Practice",
                tint = colors.secondaryText,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun KitItemRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = colors.accent,
                uncheckedColor = colors.secondaryText,
                checkmarkColor = colors.background
            )
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            style = typography.caption.copy(fontSize = 13.sp),
            color = if (checked) colors.mutedText else colors.primaryText,
            fontWeight = if (checked) FontWeight.Normal else FontWeight.Medium
        )
    }
}

@Composable
private fun RoadmapDayItem(
    dayRange: String,
    task: String,
    detail: String
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.card)
            .background(colors.cardBackground)
            .border(1.dp, colors.border, shapes.card)
            .padding(10.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dayRange,
                    style = typography.caption.copy(fontSize = 11.sp),
                    fontWeight = FontWeight.Bold,
                    color = colors.accent
                )
                Text(
                    text = task,
                    style = typography.secondary,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.primaryText
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = detail,
                style = typography.caption,
                color = colors.secondaryText
            )
        }
    }
}
