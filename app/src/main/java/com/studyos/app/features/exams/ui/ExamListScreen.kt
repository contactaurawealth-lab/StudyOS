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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.text.style.TextOverflow
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
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max

enum class TestListFilter(val label: String) {
    ALL("All"),
    MAJOR_EXAMS("Major Exams"),
    MOCK_TUITION("Mock Tests"),
    COMPLETED("Completed")
}

enum class ExamViewMode {
    LIST,
    CALENDAR
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamListScreen(
    viewModel: ExamViewModel,
    onBack: () -> Unit,
    onOpenExamDetail: (examId: String) -> Unit,
    onStartMockTest: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedFilter by remember { mutableStateOf(TestListFilter.ALL) }
    var viewMode by remember { mutableStateOf(ExamViewMode.LIST) }
    var menuExpanded by remember { mutableStateOf(false) }
    var createExamDefaultIsMock by remember { mutableStateOf(false) }
    var createExamInitialDate by remember { mutableStateOf<Long?>(null) }
    var calendarYearMonth by remember { mutableStateOf(java.time.YearMonth.now()) }
    var selectedCalendarDate by remember { mutableStateOf(java.time.LocalDate.now()) }

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
                    Row(
                        modifier = Modifier.weight(1f),
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

                        Column {
                            Text(
                                text = "Exams & Mock Tests",
                                style = typography.screenTitle,
                                color = colors.primaryText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (viewMode == ExamViewMode.CALENDAR) "Monthly interactive schedule" else "Targeted countdowns & preparation",
                                style = typography.caption,
                                color = colors.secondaryText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Toggle View Button (List <-> Calendar)
                        StudyOSIconButton(
                            onClick = {
                                viewMode = if (viewMode == ExamViewMode.LIST) ExamViewMode.CALENDAR else ExamViewMode.LIST
                            },
                            contentDescription = if (viewMode == ExamViewMode.LIST) "Calendar View" else "List View"
                        ) {
                            Icon(
                                imageVector = if (viewMode == ExamViewMode.LIST) Icons.Outlined.CalendarMonth else Icons.Outlined.FormatListBulleted,
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // 3-Dot Overflow Menu
                        Box {
                            StudyOSIconButton(
                                onClick = { menuExpanded = true },
                                contentDescription = "More Options"
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.MoreVert,
                                    contentDescription = null,
                                    tint = colors.primaryText,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false },
                                modifier = Modifier.background(colors.surface)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("+ Add Major Exam", style = typography.body, color = colors.primaryText) },
                                    onClick = {
                                        menuExpanded = false
                                        createExamDefaultIsMock = false
                                        createExamInitialDate = null
                                        viewModel.openCreateExamSheet()
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.School, null, tint = colors.accent, modifier = Modifier.size(18.dp))
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("+ Add Mock / Tuition Test", style = typography.body, color = colors.primaryText) },
                                    onClick = {
                                        menuExpanded = false
                                        createExamDefaultIsMock = true
                                        createExamInitialDate = null
                                        viewModel.openCreateExamSheet()
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.CalendarToday, null, tint = colors.accent, modifier = Modifier.size(18.dp))
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("⚡ Start Timed Mock Test", style = typography.body, color = colors.primaryText) },
                                    onClick = {
                                        menuExpanded = false
                                        onStartMockTest()
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.AccessTime, null, tint = colors.accent, modifier = Modifier.size(18.dp))
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (viewMode == ExamViewMode.LIST) "Switch to Calendar View" else "Switch to List View",
                                            style = typography.body,
                                            color = colors.primaryText
                                        )
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        viewMode = if (viewMode == ExamViewMode.LIST) ExamViewMode.CALENDAR else ExamViewMode.LIST
                                    },
                                    leadingIcon = {
                                        Icon(
                                            if (viewMode == ExamViewMode.LIST) Icons.Outlined.CalendarMonth else Icons.Outlined.FormatListBulleted,
                                            null,
                                            tint = colors.secondaryText,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        StudyOSButton(
                            text = "+ Schedule",
                            onClick = {
                                createExamDefaultIsMock = selectedFilter == TestListFilter.MOCK_TUITION
                                createExamInitialDate = null
                                viewModel.openCreateExamSheet()
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (viewMode == ExamViewMode.CALENDAR) {
                    // Full Interactive Month Calendar View
                    ExamCalendarView(
                        exams = uiState.exams,
                        subjects = uiState.subjects,
                        currentMonth = calendarYearMonth,
                        selectedDate = selectedCalendarDate,
                        onMonthChange = { calendarYearMonth = it },
                        onSelectDate = { selectedCalendarDate = it },
                        onOpenExamDetail = onOpenExamDetail,
                        onScheduleOnDate = { date ->
                            val millis = date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                            createExamInitialDate = millis
                            createExamDefaultIsMock = false
                            viewModel.openCreateExamSheet()
                        },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    // Segmented Filter Chips: All, Major Exams, Mock Tests, Completed
                    val completedExams = uiState.exams.filter { it.isCompleted || it.actualScore != null }
                    val activeExams = uiState.exams.filter { !it.isCompleted && it.actualScore == null }
                    val isMockExam: (com.studyos.app.domain.model.Exam) -> Boolean = { it.notes?.contains("[MOCK_TEST]") == true }
                    val mockCount = activeExams.count(isMockExam)
                    val majorCount = activeExams.size - mockCount
                    val completedCount = completedExams.size

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
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
                                    .wrapContentWidth()
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
                                    color = if (isSelected) colors.primaryText else colors.secondaryText,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val filteredExams = when (selectedFilter) {
                        TestListFilter.ALL -> activeExams
                        TestListFilter.MAJOR_EXAMS -> activeExams.filter { !isMockExam(it) }
                        TestListFilter.MOCK_TUITION -> activeExams.filter(isMockExam)
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
                            onActionClick = {
                                createExamDefaultIsMock = selectedFilter == TestListFilter.MOCK_TUITION
                                createExamInitialDate = null
                                viewModel.openCreateExamSheet()
                            }
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
        }

        // Create / Edit Exam Bottom Sheet
        if (uiState.isCreateExamSheetOpen) {
            CreateExamBottomSheet(
                subjects = uiState.subjects,
                editingExam = uiState.editingExam,
                defaultIsMock = if (uiState.editingExam != null) false else createExamDefaultIsMock,
                initialTargetDate = createExamInitialDate,
                onDismiss = {
                    viewModel.closeExamSheet()
                    createExamInitialDate = null
                },
                onSave = { name, targetDate, subjectIds, targetScore, notes ->
                    viewModel.saveExam(name, targetDate, subjectIds, targetScore, notes)
                    createExamInitialDate = null
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

    val isMock = exam.notes?.contains("[MOCK_TEST]") == true
    val now = System.currentTimeMillis()
    val daysRemaining = exam.getDaysRemaining(now)
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
                            color = colors.primaryText,
                            modifier = Modifier.weight(1f, fill = false),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Distinct Type Badge
                        Box(
                            modifier = Modifier
                                .wrapContentWidth()
                                .clip(shapes.surface)
                                .background(if (isMock) colors.accent.copy(alpha = 0.15f) else colors.cardBackground)
                                .border(0.5.dp, if (isMock) colors.accent.copy(alpha = 0.4f) else colors.border.copy(alpha = 0.3f), shapes.surface)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isMock) "Mock Test" else "Major Exam",
                                style = typography.caption.copy(fontSize = 10.sp),
                                fontWeight = FontWeight.SemiBold,
                                color = if (isMock) colors.accent else colors.secondaryText,
                                maxLines = 1,
                                softWrap = false
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
                                daysRemaining < 0L -> "PAST DUE"
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
    initialTargetDate: Long? = null,
    onDismiss: () -> Unit,
    onSave: (name: String, targetDate: Long, subjectIds: List<String>, targetScore: Int?, notes: String?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes
    val context = LocalContext.current

    val initialIsMock = editingExam?.let { it.notes?.contains("[MOCK_TEST]") == true } ?: defaultIsMock

    var isMockTest by remember { mutableStateOf(initialIsMock) }
    var name by remember { mutableStateOf(editingExam?.name ?: "") }
    var targetDate by remember {
        mutableStateOf(editingExam?.targetDate ?: initialTargetDate ?: (System.currentTimeMillis() + 7 * 86_400_000L))
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
        val localZone = remember { java.time.ZoneId.systemDefault() }
        val initialUtcMillis = remember(targetDate) {
            val localDate = java.time.Instant.ofEpochMilli(targetDate).atZone(localZone).toLocalDate()
            localDate.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
        }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialUtcMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selectedUtcMillis ->
                        val selectedLocalDate = java.time.Instant.ofEpochMilli(selectedUtcMillis)
                            .atZone(java.time.ZoneOffset.UTC)
                            .toLocalDate()
                        val currentLocal = java.time.Instant.ofEpochMilli(targetDate).atZone(localZone)
                        val updatedLocal = selectedLocalDate.atTime(currentLocal.toLocalTime()).atZone(localZone)
                        targetDate = updatedLocal.toInstant().toEpochMilli()
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

@Composable
private fun ExamCalendarView(
    exams: List<Exam>,
    subjects: List<Subject>,
    currentMonth: java.time.YearMonth,
    selectedDate: java.time.LocalDate,
    onMonthChange: (java.time.YearMonth) -> Unit,
    onSelectDate: (java.time.LocalDate) -> Unit,
    onOpenExamDetail: (examId: String) -> Unit,
    onScheduleOnDate: (java.time.LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes
    val zone = java.time.ZoneId.systemDefault()
    val today = java.time.LocalDate.now(zone)

    // Precompute exams by date
    val examsByDate = remember(exams) {
        exams.groupBy { exam ->
            java.time.Instant.ofEpochMilli(exam.targetDate).atZone(zone).toLocalDate()
        }
    }

    val selectedDateExams = remember(selectedDate, examsByDate) {
        examsByDate[selectedDate] ?: emptyList()
    }

    val monthFormatter = remember { java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()) }
    val selectedDateFormatter = remember { java.time.format.DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Month Navigation Header Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shapes.card)
                .background(colors.surface)
                .border(0.5.dp, colors.border.copy(alpha = 0.4f), shapes.card)
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = currentMonth.format(monthFormatter),
                            style = typography.sectionTitle.copy(fontSize = 18.sp),
                            color = colors.primaryText,
                            fontWeight = FontWeight.Bold
                        )
                        val totalInMonth = exams.count { exam ->
                            val dt = java.time.Instant.ofEpochMilli(exam.targetDate).atZone(zone).toLocalDate()
                            java.time.YearMonth.from(dt) == currentMonth
                        }
                        Text(
                            text = if (totalInMonth == 1) "1 scheduled exam event" else "$totalInMonth scheduled exam events",
                            style = typography.caption,
                            color = colors.secondaryText
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (currentMonth != java.time.YearMonth.from(today) || selectedDate != today) {
                            Box(
                                modifier = Modifier
                                    .clip(shapes.button)
                                    .background(colors.cardBackground)
                                    .border(1.dp, colors.accent.copy(alpha = 0.4f), shapes.button)
                                    .clickable {
                                        onMonthChange(java.time.YearMonth.from(today))
                                        onSelectDate(today)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Today",
                                    style = typography.caption,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.accent
                                )
                            }
                        }

                        StudyOSIconButton(
                            onClick = { onMonthChange(currentMonth.minusMonths(1)) },
                            contentDescription = "Previous Month"
                        ) {
                            Icon(Icons.Outlined.ChevronLeft, null, tint = colors.primaryText, modifier = Modifier.size(20.dp))
                        }

                        StudyOSIconButton(
                            onClick = { onMonthChange(currentMonth.plusMonths(1)) },
                            contentDescription = "Next Month"
                        ) {
                            Icon(Icons.Outlined.ChevronRight, null, tint = colors.primaryText, modifier = Modifier.size(20.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Days of week header (Mon - Sun)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val daysOfWeek = listOf(
                        java.time.DayOfWeek.MONDAY,
                        java.time.DayOfWeek.TUESDAY,
                        java.time.DayOfWeek.WEDNESDAY,
                        java.time.DayOfWeek.THURSDAY,
                        java.time.DayOfWeek.FRIDAY,
                        java.time.DayOfWeek.SATURDAY,
                        java.time.DayOfWeek.SUNDAY
                    )
                    daysOfWeek.forEach { dow ->
                        Text(
                            text = dow.getDisplayName(java.time.format.TextStyle.SHORT, Locale.getDefault()),
                            style = typography.caption.copy(fontSize = 11.sp),
                            fontWeight = FontWeight.SemiBold,
                            color = colors.mutedText,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Month Calendar Grid
                val firstDayOfMonth = currentMonth.atDay(1)
                val leadingBlanks = (firstDayOfMonth.dayOfWeek.value - 1) // 0 for Mon, 6 for Sun
                val daysInMonth = currentMonth.lengthOfMonth()
                val totalCells = ((leadingBlanks + daysInMonth + 6) / 7) * 7

                for (weekIndex in 0 until (totalCells / 7)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (colIndex in 0 until 7) {
                            val cellIndex = weekIndex * 7 + colIndex
                            val dayNumber = cellIndex - leadingBlanks + 1

                            if (dayNumber in 1..daysInMonth) {
                                val cellDate = currentMonth.atDay(dayNumber)
                                val isSelected = cellDate == selectedDate
                                val isCellToday = cellDate == today
                                val dayExamsList = examsByDate[cellDate] ?: emptyList()
                                val hasMajorExam = dayExamsList.any { it.notes?.contains("[MOCK_TEST]") != true && !it.isCompleted }
                                val hasMockTest = dayExamsList.any { it.notes?.contains("[MOCK_TEST]") == true && !it.isCompleted }
                                val hasCompleted = dayExamsList.any { it.isCompleted || it.actualScore != null }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clip(shapes.button)
                                        .background(
                                            when {
                                                isSelected -> colors.accent
                                                else -> androidx.compose.ui.graphics.Color.Transparent
                                            }
                                        )
                                        .border(
                                            width = if (isCellToday && !isSelected) 1.dp else 0.dp,
                                            color = if (isCellToday && !isSelected) colors.accent else androidx.compose.ui.graphics.Color.Transparent,
                                            shape = shapes.button
                                        )
                                        .clickable { onSelectDate(cellDate) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "$dayNumber",
                                            style = typography.body.copy(fontSize = 13.sp),
                                            fontWeight = if (isSelected || isCellToday) FontWeight.Bold else FontWeight.Normal,
                                            color = when {
                                                isSelected -> colors.background
                                                isCellToday -> colors.accent
                                                else -> colors.primaryText
                                            }
                                        )

                                        // Event Indicator Dots
                                        if (dayExamsList.isNotEmpty()) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(top = 2.dp)
                                            ) {
                                                if (hasMajorExam) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(4.dp)
                                                            .clip(androidx.compose.foundation.shape.CircleShape)
                                                            .background(if (isSelected) colors.background else colors.accent)
                                                    )
                                                }
                                                if (hasMockTest) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(4.dp)
                                                            .clip(androidx.compose.foundation.shape.CircleShape)
                                                            .background(if (isSelected) colors.background else androidx.compose.ui.graphics.Color(0xFFE5A038))
                                                    )
                                                }
                                                if (hasCompleted && !hasMajorExam && !hasMockTest) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(4.dp)
                                                            .clip(androidx.compose.foundation.shape.CircleShape)
                                                            .background(if (isSelected) colors.background else colors.mutedText)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Legend row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(6.dp).clip(androidx.compose.foundation.shape.CircleShape).background(colors.accent))
                        Text("Major Exam", style = typography.caption.copy(fontSize = 10.sp), color = colors.secondaryText)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(6.dp).clip(androidx.compose.foundation.shape.CircleShape).background(androidx.compose.ui.graphics.Color(0xFFE5A038)))
                        Text("Mock Test", style = typography.caption.copy(fontSize = 10.sp), color = colors.secondaryText)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(6.dp).clip(androidx.compose.foundation.shape.CircleShape).background(colors.mutedText))
                        Text("Completed", style = typography.caption.copy(fontSize = 10.sp), color = colors.secondaryText)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Selected Date Agenda Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = selectedDate.format(selectedDateFormatter),
                    style = typography.sectionTitle,
                    color = colors.primaryText,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (selectedDateExams.isEmpty()) "No scheduled tests on this date" else "${selectedDateExams.size} test(s) on this date",
                    style = typography.caption,
                    color = colors.secondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            StudyOSOutlinedButton(
                text = "+ Add Test",
                onClick = { onScheduleOnDate(selectedDate) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (selectedDateExams.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shapes.card)
                    .background(colors.surface)
                    .border(0.5.dp, colors.border.copy(alpha = 0.3f), shapes.card)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Clean Schedule",
                        style = typography.body,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.primaryText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "No tests or exams set for ${selectedDate.format(selectedDateFormatter)}.",
                        style = typography.caption,
                        color = colors.secondaryText
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    StudyOSButton(
                        text = "Schedule on This Date",
                        onClick = { onScheduleOnDate(selectedDate) }
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                selectedDateExams.forEach { exam ->
                    ExamCardItem(
                        exam = exam,
                        subjects = subjects.filter { exam.subjectIds.contains(it.id) },
                        onClick = { onOpenExamDetail(exam.id) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
