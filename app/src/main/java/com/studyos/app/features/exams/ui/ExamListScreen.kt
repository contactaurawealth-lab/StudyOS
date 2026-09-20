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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.studyos.app.core.ui.component.StudyOSEmptyState
import com.studyos.app.core.ui.component.StudyOSIconButton
import com.studyos.app.core.ui.component.StudyOSLoadingState
import com.studyos.app.core.ui.component.StudyOSOutlinedButton
import com.studyos.app.core.ui.component.StudyOSTextField
import com.studyos.app.domain.model.Exam
import com.studyos.app.domain.model.Subject
import com.studyos.app.features.exams.viewmodel.ExamViewModel
import com.studyos.app.theme.StudyOSTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamListScreen(
    viewModel: ExamViewModel,
    onBack: () -> Unit,
    onOpenExamDetail: (examId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes
    val snackbarHostState = remember { SnackbarHostState() }

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
        if (uiState.isLoading) {
            StudyOSLoadingState(message = "Loading exams...")
        } else {
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

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = "Exam Preparation",
                                style = typography.screenTitle,
                                color = colors.primaryText
                            )
                            Text(
                                text = "Targeted countdowns & data-driven revision",
                                style = typography.caption,
                                color = colors.secondaryText
                            )
                        }
                    }

                    StudyOSButton(
                        text = "+ Add Exam",
                        onClick = { viewModel.openCreateExamSheet() }
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                if (uiState.exams.isEmpty()) {
                    StudyOSEmptyState(
                        title = "No upcoming exams",
                        description = "Add an upcoming exam to generate targeted countdowns, weak topic analysis, and revision priorities.",
                        actionButtonText = "Create Exam Plan",
                        onActionClick = { viewModel.openCreateExamSheet() }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(uiState.exams, key = { it.id }) { exam ->
                            ExamCardItem(
                                exam = exam,
                                subjects = uiState.subjects.filter { exam.subjectIds.contains(it.id) },
                                onClick = { onOpenExamDetail(exam.id) }
                            )
                        }
                    }
                }
            }
        }

        // Create / Edit Exam Bottom Sheet
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

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun ExamCardItem(
    exam: Exam,
    subjects: List<Subject>,
    onClick: () -> Unit
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    val now = System.currentTimeMillis()
    val daysRemaining = max(0L, (exam.targetDate - now) / 86_400_000L)
    val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.surface)
            .background(colors.surface)
            .border(1.dp, colors.border, shapes.surface)
            .clickable(onClick = onClick)
            .padding(18.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exam.name,
                        style = typography.sectionTitle,
                        color = colors.primaryText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = dateFormat.format(Date(exam.targetDate)),
                        style = typography.caption,
                        color = colors.secondaryText
                    )
                }

                // Countdown Badge
                Box(
                    modifier = Modifier
                        .clip(shapes.button)
                        .background(colors.cardBackground)
                        .border(1.dp, colors.border, shapes.button)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = when {
                            daysRemaining == 0L -> "TODAY"
                            daysRemaining == 1L -> "TOMORROW"
                            else -> "IN $daysRemaining DAYS"
                        },
                        style = typography.caption,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.accent
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Subjects tags
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    subjects.take(3).forEach { subject ->
                        Box(
                            modifier = Modifier
                                .clip(shapes.button)
                                .background(colors.background)
                                .border(1.dp, colors.border, shapes.button)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = subject.name,
                                style = typography.caption.copy(fontSize = 11.sp),
                                color = colors.secondaryText
                            )
                        }
                    }
                    if (subjects.size > 3) {
                        Text(
                            text = "+${subjects.size - 3} more",
                            style = typography.caption,
                            color = colors.mutedText,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateExamBottomSheet(
    subjects: List<Subject>,
    editingExam: Exam?,
    onDismiss: () -> Unit,
    onSave: (name: String, targetDate: Long, subjectIds: List<String>, targetScore: Int?, notes: String?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    var name by remember { mutableStateOf(editingExam?.name ?: "") }
    var targetDate by remember {
        mutableStateOf(editingExam?.targetDate ?: (System.currentTimeMillis() + 14 * 86_400_000L))
    }
    var targetScoreText by remember { mutableStateOf(editingExam?.targetScore?.toString() ?: "") }
    var notes by remember { mutableStateOf(editingExam?.notes ?: "") }
    val selectedSubjectIds = remember {
        mutableStateListOf<String>().apply {
            if (editingExam != null) {
                addAll(editingExam.subjectIds)
            } else if (subjects.isNotEmpty()) {
                addAll(subjects.map { it.id })
            }
        }
    }

    var showDatePicker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = if (editingExam == null) "Schedule Exam" else "Edit Exam",
                style = typography.screenTitle,
                color = colors.primaryText
            )

            Spacer(modifier = Modifier.height(16.dp))

            StudyOSTextField(
                value = name,
                onValueChange = { name = it },
                label = "Exam Title",
                placeholder = "Midterm Examination, AP Biology...",
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Target Date Selector
            val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shapes.button)
                    .background(colors.cardBackground)
                    .border(1.dp, colors.border, shapes.button)
                    .clickable { showDatePicker = true }
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Exam Date",
                            style = typography.caption,
                            color = colors.secondaryText
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = dateFormat.format(Date(targetDate)),
                            style = typography.secondary,
                            fontWeight = FontWeight.Medium,
                            color = colors.primaryText
                        )
                    }

                    Icon(
                        imageVector = Icons.Outlined.CalendarToday,
                        contentDescription = null,
                        tint = colors.secondaryText,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Subjects Included
            Text(
                text = "Subjects to Include in Exam:",
                style = typography.secondary,
                fontWeight = FontWeight.SemiBold,
                color = colors.primaryText
            )

            Spacer(modifier = Modifier.height(6.dp))

            subjects.forEach { subject ->
                val isChecked = selectedSubjectIds.contains(subject.id)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shapes.button)
                        .clickable {
                            if (isChecked) {
                                selectedSubjectIds.remove(subject.id)
                            } else {
                                selectedSubjectIds.add(subject.id)
                            }
                        }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isChecked,
                        onCheckedChange = { checked ->
                            if (checked) selectedSubjectIds.add(subject.id) else selectedSubjectIds.remove(subject.id)
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = colors.accent,
                            uncheckedColor = colors.border,
                            checkmarkColor = colors.buttonText
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = subject.name,
                        style = typography.body,
                        color = colors.primaryText
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            StudyOSTextField(
                value = targetScoreText,
                onValueChange = { targetScoreText = it },
                label = "Target Score % (Optional)",
                placeholder = "e.g. 90",
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            StudyOSTextField(
                value = notes,
                onValueChange = { notes = it },
                label = "Notes / Syllabus Topics (Optional)",
                placeholder = "Chapters 1-5, focus on cellular respiration...",
                singleLine = false,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            StudyOSButton(
                text = "Save Exam",
                onClick = {
                    val score = targetScoreText.toIntOrNull()
                    onSave(name, targetDate, selectedSubjectIds.toList(), score, notes)
                },
                enabled = name.isNotBlank() && selectedSubjectIds.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = targetDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        targetDate = it
                    }
                    showDatePicker = false
                }) {
                    Text("Select", color = colors.accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = colors.secondaryText)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
