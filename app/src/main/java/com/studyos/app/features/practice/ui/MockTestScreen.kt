package com.studyos.app.features.practice.ui

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studyos.app.core.ui.component.StudyOSButton
import com.studyos.app.core.ui.component.StudyOSOutlinedButton
import com.studyos.app.core.ui.component.StudyOSTextField
import com.studyos.app.domain.model.Subject
import com.studyos.app.theme.StudyOSTheme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class MockTestState {
    CONFIG,
    RUNNING,
    RESULTS
}

data class MockTestResult(
    val title: String,
    val subjectName: String,
    val durationMinutes: Int,
    val timeSpentSeconds: Int,
    val totalQuestions: Int,
    val score: Int,
    val accuracyPercentage: Int,
    val dateMillis: Long = System.currentTimeMillis()
)

/**
 * Feature 6: MOCK TEST MODE
 * Allows students to configure and run timed mock tests, tracking:
 * - Subject
 * - Duration
 * - Questions
 * - Score
 * - Accuracy
 * - Date
 * Shows detailed results afterward.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MockTestScreen(
    subjects: List<Subject>,
    onBack: () -> Unit,
    onSaveResult: (MockTestResult) -> Unit = {},
    onAddMistakeClick: ((subjectId: String?, question: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    var testState by remember { mutableStateOf(MockTestState.CONFIG) }

    // Configuration
    var selectedSubject by remember { mutableStateOf(subjects.firstOrNull()) }
    var testTitle by remember { mutableStateOf("Mock Test 1") }
    var selectedDurationMinutes by remember { mutableIntStateOf(45) }
    var totalQuestionsInput by remember { mutableStateOf("30") }

    // Running state
    var remainingSeconds by remember { mutableIntStateOf(45 * 60) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var isTimerRunning by remember { mutableStateOf(false) }
    val answersList = remember { mutableStateListOf<Boolean?>() } // true = correct, false = incorrect, null = unanswered
    var showFinishConfirmDialog by remember { mutableStateOf(false) }

    // Results state
    var testResult by remember { mutableStateOf<MockTestResult?>(null) }

    // Countdown Timer logic
    LaunchedEffect(isTimerRunning, testState) {
        if (testState == MockTestState.RUNNING && isTimerRunning) {
            while (remainingSeconds > 0 && isTimerRunning) {
                delay(1000L)
                remainingSeconds -= 1
                elapsedSeconds += 1
            }
            if (remainingSeconds <= 0 && testState == MockTestState.RUNNING) {
                // Auto finish on time expiration
                val qCount = totalQuestionsInput.toIntOrNull() ?: 30
                val correctCount = answersList.count { it == true }
                val accuracy = if (qCount > 0) ((correctCount * 100) / qCount) else 0
                val result = MockTestResult(
                    title = testTitle.ifBlank { "Mock Test" },
                    subjectName = selectedSubject?.name ?: "General",
                    durationMinutes = selectedDurationMinutes,
                    timeSpentSeconds = elapsedSeconds,
                    totalQuestions = qCount,
                    score = correctCount,
                    accuracyPercentage = accuracy
                )
                testResult = result
                onSaveResult(result)
                testState = MockTestState.RESULTS
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (testState) {
                            MockTestState.CONFIG -> "Mock Test Setup"
                            MockTestState.RUNNING -> "Mock Test in Progress"
                            MockTestState.RESULTS -> "Mock Test Results"
                        },
                        style = typography.sectionTitle.copy(fontSize = 17.sp),
                        color = colors.primaryText
                    )
                },
                navigationIcon = {
                    if (testState == MockTestState.CONFIG || testState == MockTestState.RESULTS) {
                        TextButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "Back",
                                tint = colors.primaryText,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.surface)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(colors.background)
        ) {
            when (testState) {
                MockTestState.CONFIG -> {
                    MockTestConfigView(
                        subjects = subjects,
                        selectedSubject = selectedSubject,
                        onSelectSubject = { selectedSubject = it },
                        title = testTitle,
                        onTitleChange = { testTitle = it },
                        selectedDuration = selectedDurationMinutes,
                        onSelectDuration = { selectedDurationMinutes = it },
                        totalQuestionsText = totalQuestionsInput,
                        onQuestionsChange = { totalQuestionsInput = it },
                        onStartTest = {
                            val qCount = totalQuestionsInput.toIntOrNull()?.coerceAtLeast(1) ?: 30
                            answersList.clear()
                            repeat(qCount) { answersList.add(null) }
                            remainingSeconds = selectedDurationMinutes * 60
                            elapsedSeconds = 0
                            isTimerRunning = true
                            testState = MockTestState.RUNNING
                        }
                    )
                }
                MockTestState.RUNNING -> {
                    MockTestRunningView(
                        title = testTitle,
                        subjectName = selectedSubject?.name ?: "General",
                        remainingSeconds = remainingSeconds,
                        answersList = answersList,
                        onAnswerQuestion = { index, isCorrect ->
                            answersList[index] = isCorrect
                        },
                        onRequestFinish = { showFinishConfirmDialog = true }
                    )
                }
                MockTestState.RESULTS -> {
                    testResult?.let { result ->
                        MockTestResultsView(
                            result = result,
                            onDone = onBack,
                            onAddMistake = {
                                onAddMistakeClick?.invoke(selectedSubject?.id, "Missed during ${result.title}")
                            }
                        )
                    }
                }
            }
        }

        if (showFinishConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showFinishConfirmDialog = false },
                containerColor = colors.surface,
                title = {
                    Text(
                        text = "Submit Mock Test?",
                        style = typography.sectionTitle.copy(fontSize = 16.sp),
                        color = colors.primaryText
                    )
                },
                text = {
                    val answered = answersList.count { it != null }
                    val remaining = answersList.size - answered
                    Text(
                        text = "You have answered $answered of ${answersList.size} questions ($remaining unanswered).\n\nAre you sure you want to finish and calculate your score?",
                        style = typography.caption.copy(fontSize = 13.sp),
                        color = colors.secondaryText
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showFinishConfirmDialog = false
                            isTimerRunning = false
                            val qCount = answersList.size
                            val correctCount = answersList.count { it == true }
                            val accuracy = if (qCount > 0) ((correctCount * 100) / qCount) else 0
                            val result = MockTestResult(
                                title = testTitle.ifBlank { "Mock Test" },
                                subjectName = selectedSubject?.name ?: "General",
                                durationMinutes = selectedDurationMinutes,
                                timeSpentSeconds = elapsedSeconds,
                                totalQuestions = qCount,
                                score = correctCount,
                                accuracyPercentage = accuracy
                            )
                            testResult = result
                            onSaveResult(result)
                            testState = MockTestState.RESULTS
                        }
                    ) {
                        Text("Finish Test", color = colors.accent, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showFinishConfirmDialog = false }) {
                        Text("Continue Test", color = colors.secondaryText)
                    }
                }
            )
        }
    }
}

@Composable
private fun MockTestConfigView(
    subjects: List<Subject>,
    selectedSubject: Subject?,
    onSelectSubject: (Subject) -> Unit,
    title: String,
    onTitleChange: (String) -> Unit,
    selectedDuration: Int,
    onSelectDuration: (Int) -> Unit,
    totalQuestionsText: String,
    onQuestionsChange: (String) -> Unit,
    onStartTest: () -> Unit
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes
    val durations = listOf(15, 30, 45, 60, 90, 120)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Create Mock-Test Session",
            style = typography.sectionTitle.copy(fontSize = 18.sp),
            color = colors.primaryText
        )
        Text(
            text = "Simulate real exam pressure with strict timing and score tracking.",
            style = typography.caption,
            color = colors.secondaryText
        )

        // Title
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = "Test Name", style = typography.caption.copy(fontWeight = FontWeight.Medium), color = colors.primaryText)
            StudyOSTextField(
                value = title,
                onValueChange = onTitleChange,
                placeholder = "e.g. Physics Mid-Term Mock 1",
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Subject Selection
        if (subjects.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = "Subject", style = typography.caption.copy(fontWeight = FontWeight.Medium), color = colors.primaryText)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    subjects.take(4).forEach { subject ->
                        val isSelected = selectedSubject?.id == subject.id
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(shapes.pill)
                                .background(if (isSelected) colors.accent.copy(alpha = 0.16f) else colors.surface)
                                .border(1.dp, if (isSelected) colors.accent else colors.border, shapes.pill)
                                .clickable { onSelectSubject(subject) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = subject.name,
                                style = typography.caption.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                                color = if (isSelected) colors.accent else colors.primaryText,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // Duration Selection
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = "Duration", style = typography.caption.copy(fontWeight = FontWeight.Medium), color = colors.primaryText)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                durations.forEach { dur ->
                    val isSelected = selectedDuration == dur
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(shapes.pill)
                            .background(if (isSelected) colors.accent.copy(alpha = 0.16f) else colors.surface)
                            .border(1.dp, if (isSelected) colors.accent else colors.border, shapes.pill)
                            .clickable { onSelectDuration(dur) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${dur}m",
                            style = typography.caption.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                            color = if (isSelected) colors.accent else colors.primaryText
                        )
                    }
                }
            }
        }

        // Question count
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = "Number of Questions", style = typography.caption.copy(fontWeight = FontWeight.Medium), color = colors.primaryText)
            StudyOSTextField(
                value = totalQuestionsText,
                onValueChange = onQuestionsChange,
                placeholder = "e.g. 30",
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        StudyOSButton(
            text = "Start Mock Test",
            onClick = onStartTest,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun MockTestRunningView(
    title: String,
    subjectName: String,
    remainingSeconds: Int,
    answersList: List<Boolean?>,
    onAnswerQuestion: (Int, Boolean) -> Unit,
    onRequestFinish: () -> Unit
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    val isCritical = remainingSeconds <= 300 // under 5 mins

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = typography.sectionTitle.copy(fontSize = 18.sp),
            color = colors.primaryText
        )
        Text(
            text = subjectName,
            style = typography.caption,
            color = colors.secondaryText
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Large Countdown Clock
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shapes.surface)
                .background(colors.surface)
                .border(
                    width = if (isCritical) 2.dp else 1.dp,
                    color = if (isCritical) colors.error else colors.border,
                    shape = shapes.surface
                )
                .padding(vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = timeFormatted,
                    style = typography.screenTitle.copy(
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = if (isCritical) colors.error else colors.primaryText
                )
                Text(
                    text = if (isCritical) "Time expiring soon" else "Time Remaining",
                    style = typography.caption.copy(fontSize = 11.sp),
                    color = if (isCritical) colors.error else colors.mutedText
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Questions checklist/scoring grid
        Text(
            text = "Record Question Status as you practice:",
            style = typography.caption.copy(fontWeight = FontWeight.SemiBold),
            color = colors.primaryText,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            answersList.forEachIndexed { index, status ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shapes.small)
                        .background(colors.surface)
                        .border(1.dp, colors.border, shapes.small)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Q${index + 1}",
                        style = typography.caption.copy(fontWeight = FontWeight.Bold),
                        color = colors.primaryText
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Correct button
                        Box(
                            modifier = Modifier
                                .clip(shapes.pill)
                                .background(if (status == true) colors.success.copy(alpha = 0.2f) else colors.cardBackground)
                                .border(1.dp, if (status == true) colors.success else colors.border, shapes.pill)
                                .clickable { onAnswerQuestion(index, true) }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Correct",
                                style = typography.caption.copy(fontSize = 11.sp, fontWeight = if (status == true) FontWeight.Bold else FontWeight.Normal),
                                color = if (status == true) colors.success else colors.secondaryText
                            )
                        }

                        // Incorrect button
                        Box(
                            modifier = Modifier
                                .clip(shapes.pill)
                                .background(if (status == false) colors.error.copy(alpha = 0.2f) else colors.cardBackground)
                                .border(1.dp, if (status == false) colors.error else colors.border, shapes.pill)
                                .clickable { onAnswerQuestion(index, false) }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Incorrect",
                                style = typography.caption.copy(fontSize = 11.sp, fontWeight = if (status == false) FontWeight.Bold else FontWeight.Normal),
                                color = if (status == false) colors.error else colors.secondaryText
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        StudyOSButton(
            text = "Submit & Finish Test",
            onClick = onRequestFinish,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun MockTestResultsView(
    result: MockTestResult,
    onDone: () -> Unit,
    onAddMistake: () -> Unit
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(result.dateMillis))
    val minutesSpent = result.timeSpentSeconds / 60
    val secondsSpent = result.timeSpentSeconds % 60

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = result.title,
            style = typography.sectionTitle.copy(fontSize = 20.sp),
            color = colors.primaryText
        )
        Text(
            text = "${result.subjectName} • $formattedDate",
            style = typography.caption,
            color = colors.secondaryText
        )

        // Hero Score Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shapes.surface)
                .background(colors.surface)
                .border(1.dp, colors.border, shapes.surface)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${result.score} / ${result.totalQuestions}",
                    style = typography.screenTitle.copy(fontSize = 44.sp, fontWeight = FontWeight.Bold),
                    color = colors.primaryText
                )
                Text(
                    text = "${result.accuracyPercentage}% Accuracy",
                    style = typography.sectionTitle.copy(fontSize = 18.sp, color = colors.accent)
                )
            }
        }

        // Stats grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(shapes.small)
                    .background(colors.surface)
                    .border(1.dp, colors.border, shapes.small)
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Time Spent", style = typography.caption, color = colors.mutedText)
                    Text(
                        text = "${minutesSpent}m ${secondsSpent}s",
                        style = typography.caption.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                        color = colors.primaryText
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(shapes.small)
                    .background(colors.surface)
                    .border(1.dp, colors.border, shapes.small)
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Missed", style = typography.caption, color = colors.mutedText)
                    Text(
                        text = "${result.totalQuestions - result.score}",
                        style = typography.caption.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                        color = colors.error
                    )
                }
            }
        }

        if (result.totalQuestions - result.score > 0) {
            StudyOSOutlinedButton(
                text = "Record Missed in Mistake Book",
                onClick = onAddMistake,
                modifier = Modifier.fillMaxWidth()
            )
        }

        StudyOSButton(
            text = "Done",
            onClick = onDone,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
