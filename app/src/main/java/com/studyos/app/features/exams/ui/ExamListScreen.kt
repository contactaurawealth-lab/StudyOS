package com.studyos.app.features.exams.ui

import android.app.TimePickerDialog
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
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarToday
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studyos.app.core.ui.component.StudyOSButton
import com.studyos.app.core.ui.component.StudyOSEmptyState
import com.studyos.app.core.ui.component.StudyOSIconButton
import com.studyos.app.core.ui.component.StudyOSLoadingState
import com.studyos.app.core.ui.component.StudyOSTextField
import com.studyos.app.domain.model.Exam
import com.studyos.app.domain.model.Subject
import com.studyos.app.features.exams.viewmodel.ExamViewModel
import com.studyos.app.theme.StudyOSTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max

enum class TestListFilter(val label: String) {
    ALL("All"),
    MAJOR_EXAMS("Major Exams"),
    MOCK_TUITION("Mock & Tuition"),
    COMPLETED("Completed")
}

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

    var selectedFilter by remember { mutableStateOf(TestListFilter.ALL) }

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
            StudyOSLoadingState(message = "Loading test & exam schedules...")
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
                                text = "Exams & Mock Tests",
                                style = typography.screenTitle,
                                color = colors.primaryText
                            )
                            Text(
                                text = "Targeted countdowns & mock test preparation",
                                style = typography.caption,
                                color = colors.secondaryText
                            )
                        }
                    }

                    StudyOSButton(
                        text = "+ Schedule",
                        onClick = { viewModel.openCreateExamSheet() }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Segmented Filter Chips: All, Major Exams, Mock & Tuition Tests, Completed
                val completedExams = uiState.exams.filter { it.isCompleted || it.actualScore != null }
                val activeExams = uiState.exams.filter { !it.isCompleted && it.actualScore == null }
                val mockCount = activeExams.count { it.notes?.contains("[MOCK_TEST]") == true || it.name.contains("Mock", true) || it.name.contains("Tuition", true) || it.name.contains("Test", true) }
                val majorCount = activeExams.size - mockCount
                val completedCount = completedExams.size

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TestListFilter.values().forEach { filter ->
                        val isSelected = selectedFilter == filter
                        val count = when (filter) {
                            TestListFilter.ALL -> activeExams.size
                            TestListFilter.MAJOR_EXAMS -> majorCount
                            TestListFilter.MOCK_TUITION -> mockCount
                            TestListFilter.COMPLETED -> completedCount
                        }
                        Box(
                            modifier = Modifier
                                .clip(shapes.button)
                                .background(if (isSelected) colors.cardBackground else colors.surface)
                                .border(1.dp, if (isSelected) colors.accent else colors.border, shapes.button)
                                .clickable { selectedFilter = filter }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${filter.label} ($count)",
                                style = typography.caption,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) colors.primaryText else colors.secondaryText
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                val filteredExams = when (selectedFilter) {
                    TestListFilter.ALL -> activeExams
                    TestListFilter.MAJOR_EXAMS -> activeExams.filter { !(it.notes?.contains("[MOCK_TEST]") == true || it.name.contains("Mock", true) || it.name.contains("Tuition", true) || it.name.contains("Test", true)) }
                    TestListFilter.MOCK_TUITION -> activeExams.filter { it.notes?.contains("[MOCK_TEST]") == true || it.name.contains("Mock", true) || it.name.contains("Tuition", true) || it.name.contains("Test", true) }
                    TestListFilter.COMPLETED -> completedExams
                }

                if (filteredExams.isEmpty()) {
                    StudyOSEmptyState(
                        title = when (selectedFilter) {
                            TestListFilter.MOCK_TUITION -> "No mock / tuition tests scheduled"
                            TestListFilter.MAJOR_EXAMS -> "No major exams scheduled"
                            TestListFilter.COMPLETED -> "No completed tests or exams yet"
                            TestListFilter.ALL -> "No upcoming tests or exams"
                        },
                        description = "Schedule upcoming tuition quizzes, coaching mock tests, or major exams with subject selection and date/time.",
                        actionButtonText = "+ Schedule Test / Exam",
                        onActionClick = { viewModel.openCreateExamSheet() }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(filteredExams, key = { it.id }) { exam ->
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
                defaultIsMock = selectedFilter == TestListFilter.MOCK_TUITION,
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

    val isMock = exam.notes?.contains("[MOCK_TEST]") == true || exam.name.contains("Mock", true) || exam.name.contains("Tuition", true) || exam.name.contains("Test", true)
    val now = System.currentTimeMillis()
    val daysRemaining = max(0L, (exam.targetDate - now) / 86_400_000L)
    val dateTimeFormat = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.surface)
            .background(colors.surface)
            .border(0.5.dp, colors.border.copy(alpha = 0.3f), shapes.surface)
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = exam.name,
                            style = typography.sectionTitle,
                            color = colors.primaryText
                        )

                        // Distinct Type Badge
                        Box(
                            modifier = Modifier
                                .clip(shapes.surface)
                                .background(if (isMock) colors.accent.copy(alpha = 0.15f) else colors.cardBackground)
                                .border(0.5.dp, if (isMock) colors.accent.copy(alpha = 0.4f) else colors.border.copy(alpha = 0.3f), shapes.surface)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isMock) "Mock / Tuition" else "Major Exam",
                                style = typography.caption.copy(fontSize = 10.sp),
                                fontWeight = FontWeight.SemiBold,
                                color = if (isMock) colors.accent else colors.secondaryText
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = dateTimeFormat.format(Date(exam.targetDate)),
                        style = typography.caption,
                        color = colors.secondaryText
                    )
                }

                // Status Badge: Score/Completed or Countdown
                if (exam.isCompleted || exam.actualScore != null) {
                    Box(
                        modifier = Modifier
                            .clip(shapes.button)
                            .background(colors.cardBackground)
                            .border(0.5.dp, colors.accent, shapes.button)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (exam.actualScore != null) "SCORE: ${exam.actualScore}%" else "COMPLETED",
                            style = typography.caption,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.accent
                        )
                    }
                } else {
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

            if (exam.targetScore != null || exam.actualScore != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (exam.targetScore != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Target: ",
                                style = typography.caption,
                                color = colors.mutedText
                            )
                            Text(
                                text = "${exam.targetScore}%",
                                style = typography.caption,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.primaryText
                            )
                        }
                    }

                    if (exam.actualScore != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Actual: ",
                                style = typography.caption,
                                color = colors.mutedText
                            )
                            Text(
                                text = "${exam.actualScore}%",
                                style = typography.caption,
                                fontWeight = FontWeight.Bold,
                                color = colors.accent
                            )
                            if (exam.targetScore != null) {
                                val delta = exam.actualScore!! - exam.targetScore!!
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (delta >= 0) "(+${delta}%)" else "(${delta}%)",
                                    style = typography.caption.copy(fontSize = 11.sp),
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (delta >= 0) colors.accent else colors.secondaryText
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateExamBottomSheet(
    subjects: List<Subject>,
    editingExam: Exam?,
    defaultIsMock: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (name: String, targetDate: Long, subjectIds: List<String>, targetScore: Int?, notes: String?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes
    val context = LocalContext.current

    val initialIsMock = editingExam?.let { it.notes?.contains("[MOCK_TEST]") == true || it.name.contains("Mock", true) || it.name.contains("Tuition", true) || it.name.contains("Test", true) } ?: defaultIsMock

    var isMockTest by remember { mutableStateOf(initialIsMock) }
    var name by remember { mutableStateOf(editingExam?.name ?: "") }
    var targetDate by remember {
        mutableStateOf(editingExam?.targetDate ?: (System.currentTimeMillis() + 7 * 86_400_000L))
    }
    var targetScoreText by remember { mutableStateOf(editingExam?.targetScore?.toString() ?: "") }
    var rawNotes by remember {
        mutableStateOf(editingExam?.notes?.replace("[MOCK_TEST]", "")?.trim() ?: "")
    }
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
                text = if (editingExam == null) (if (isMockTest) "Schedule Mock / Tuition Test" else "Schedule Major Exam") else "Edit Schedule",
                style = typography.screenTitle,
                color = colors.primaryText
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Type Toggle Selector: Major Exam vs Mock/Tuition Test
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(shapes.button)
                        .background(if (!isMockTest) colors.accent.copy(alpha = 0.15f) else colors.cardBackground)
                        .border(1.dp, if (!isMockTest) colors.accent else colors.border, shapes.button)
                        .clickable { isMockTest = false }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Major Exam",
                        style = typography.caption,
                        fontWeight = if (!isMockTest) FontWeight.Bold else FontWeight.Normal,
                        color = if (!isMockTest) colors.accent else colors.secondaryText
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(shapes.button)
                        .background(if (isMockTest) colors.accent.copy(alpha = 0.15f) else colors.cardBackground)
                        .border(1.dp, if (isMockTest) colors.accent else colors.border, shapes.button)
                        .clickable { isMockTest = true }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Mock / Tuition Test",
                        style = typography.caption,
                        fontWeight = if (isMockTest) FontWeight.Bold else FontWeight.Normal,
                        color = if (isMockTest) colors.accent else colors.secondaryText
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            StudyOSTextField(
                value = name,
                onValueChange = { name = it },
                label = if (isMockTest) "Test Title / Coaching Name" else "Exam Title",
                placeholder = if (isMockTest) "e.g. Physics Weekly Tuition Test, Allen Mock Test" else "Midterm Examination, AP Biology...",
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Target Date & Time Selectors
            val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
            val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Date Selector Card
                Box(
                    modifier = Modifier
                        .weight(1.3f)
                        .clip(shapes.button)
                        .background(colors.cardBackground)
                        .border(1.dp, colors.border, shapes.button)
                        .clickable { showDatePicker = true }
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Date",
                                style = typography.caption,
                                color = colors.secondaryText
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = dateFormat.format(Date(targetDate)),
                                style = typography.secondary,
                                fontWeight = FontWeight.Medium,
                                color = colors.primaryText,
                                maxLines = 1
                            )
                        }

                        Icon(
                            imageVector = Icons.Outlined.CalendarToday,
                            contentDescription = null,
                            tint = colors.secondaryText,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Time Selector Card
                Box(
                    modifier = Modifier
                        .weight(0.9f)
                        .clip(shapes.button)
                        .background(colors.cardBackground)
                        .border(1.dp, colors.border, shapes.button)
                        .clickable {
                            val cal = Calendar.getInstance().apply { timeInMillis = targetDate }
                            TimePickerDialog(
                                context,
                                { _, hourOfDay, minute ->
                                    cal.set(Calendar.HOUR_OF_DAY, hourOfDay)
                                    cal.set(Calendar.MINUTE, minute)
                                    targetDate = cal.timeInMillis
                                },
                                cal.get(Calendar.HOUR_OF_DAY),
                                cal.get(Calendar.MINUTE),
                                false
                            ).show()
                        }
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Time",
                                style = typography.caption,
                                color = colors.secondaryText
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = timeFormat.format(Date(targetDate)),
                                style = typography.secondary,
                                fontWeight = FontWeight.Medium,
                                color = colors.primaryText
                            )
                        }

                        Icon(
                            imageVector = Icons.Outlined.AccessTime,
                            contentDescription = null,
                            tint = colors.secondaryText,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Subjects Included
            Text(
                text = "Subject(s) Covered:",
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
                label = "Target Score / Marks % (Optional)",
                placeholder = "e.g. 85",
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            StudyOSTextField(
                value = rawNotes,
                onValueChange = { rawNotes = it },
                label = "Notes / Syllabus Topics (Optional)",
                placeholder = "Chapters 1-3, formula derivations, numericals...",
                singleLine = false,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            StudyOSButton(
                text = if (isMockTest) "Schedule Mock / Tuition Test" else "Save Exam",
                onClick = {
                    val score = targetScoreText.toIntOrNull()
                    val finalNotes = if (isMockTest) {
                        "[MOCK_TEST] ${rawNotes.trim()}".trim()
                    } else {
                        rawNotes.trim()
                    }
                    onSave(name, targetDate, selectedSubjectIds.toList(), score, finalNotes)
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
                    datePickerState.selectedDateMillis?.let { selectedMillis ->
                        val calOld = Calendar.getInstance().apply { timeInMillis = targetDate }
                        val calNew = Calendar.getInstance().apply { timeInMillis = selectedMillis }
                        calNew.set(Calendar.HOUR_OF_DAY, calOld.get(Calendar.HOUR_OF_DAY))
                        calNew.set(Calendar.MINUTE, calOld.get(Calendar.MINUTE))
                        targetDate = calNew.timeInMillis
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
