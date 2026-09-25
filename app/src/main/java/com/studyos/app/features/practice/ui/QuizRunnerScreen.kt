package com.studyos.app.features.practice.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.studyos.app.domain.model.QuestionType
import com.studyos.app.domain.model.QuizAttempt
import com.studyos.app.domain.model.QuizQuestion
import com.studyos.app.domain.usecase.checkQuizAnswersMatch
import com.studyos.app.features.practice.viewmodel.QuizRunnerViewModel
import com.studyos.app.theme.StudyOSTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizRunnerScreen(
    viewModel: QuizRunnerViewModel,
    onBack: () -> Unit,
    onOpenMistakes: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes
    val snackbarHostState = remember { SnackbarHostState() }
    var showExitDialog by remember { mutableStateOf(false) }
    var showTimerMenu by remember { mutableStateOf(false) }

    val isQuizActive = !uiState.isCompleted && !uiState.isLoading && uiState.questions.isNotEmpty()
    BackHandler(enabled = isQuizActive) {
        showExitDialog = true
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
        if (uiState.isLoading) {
            StudyOSLoadingState(message = "Preparing test...")
        } else if (uiState.questions.isEmpty()) {
            StudyOSEmptyState(
                title = "Quiz is empty",
                description = "No questions found for this quiz.",
                actionButtonText = "Go back",
                onActionClick = onBack
            )
        } else if (uiState.isCompleted && uiState.attemptResult != null) {
            // Quiz Results Breakdown View
            QuizResultsView(
                attempt = uiState.attemptResult!!,
                questions = uiState.questions,
                userAnswers = uiState.selectedAnswers,
                onRetake = { viewModel.retakeQuiz() },
                onReviewMistakes = onOpenMistakes,
                onFinish = onBack
            )
        } else {
            val question = uiState.currentQuestion
            if (question == null) {
                StudyOSLoadingState(message = "Loading question...")
                return@Box
            }
            val currentAnswer = uiState.selectedAnswers[question.id] ?: ""
            val isCurrentFlagged = uiState.flaggedQuestionIds.contains(question.id)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                // Top Bar: Back, Timer Pill with Countdown Mode, Question Counter & Palette Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StudyOSIconButton(
                        onClick = {
                            if (isQuizActive) {
                                showExitDialog = true
                            } else {
                                onBack()
                            }
                        },
                        contentDescription = "Exit Quiz"
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = null,
                            tint = colors.primaryText,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Interactive Timer Badge (Countdown / Elapsed)
                    Box {
                        val timerBgColor = when {
                            uiState.isTimeCritical -> Color(0xFFE53E3E).copy(alpha = 0.2f)
                            uiState.isTimeExpiringSoon -> colors.accent.copy(alpha = 0.15f)
                            else -> colors.surface
                        }
                        val timerBorderColor = when {
                            uiState.isTimeCritical -> Color(0xFFE53E3E)
                            uiState.isTimeExpiringSoon -> colors.accent
                            else -> colors.border
                        }
                        val timerTextColor = when {
                            uiState.isTimeCritical -> Color(0xFFFF6B6B)
                            uiState.isTimeExpiringSoon -> colors.accent
                            else -> colors.primaryText
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(shapes.button)
                                .background(timerBgColor)
                                .border(1.dp, timerBorderColor, shapes.button)
                                .clickable { showTimerMenu = true }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Timer,
                                contentDescription = null,
                                tint = timerTextColor,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))

                            if (uiState.isTimed) {
                                val rem = uiState.remainingSeconds
                                val minutes = rem / 60
                                val seconds = rem % 60
                                Text(
                                    text = String.format("%02d:%02d", minutes, seconds),
                                    style = typography.caption,
                                    fontWeight = FontWeight.Bold,
                                    color = timerTextColor
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (uiState.isTimeCritical) "LEFT!" else "left",
                                    style = typography.caption.copy(fontSize = 10.sp),
                                    color = timerTextColor.copy(alpha = 0.8f)
                                )
                            } else {
                                val minutes = uiState.elapsedSeconds / 60
                                val seconds = uiState.elapsedSeconds % 60
                                Text(
                                    text = String.format("%02d:%02d", minutes, seconds),
                                    style = typography.caption,
                                    fontWeight = FontWeight.Medium,
                                    color = timerTextColor
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showTimerMenu,
                            onDismissRequest = { showTimerMenu = false },
                            modifier = Modifier.background(colors.surface)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Mock Mode: 10 mins", style = typography.body, color = colors.primaryText) },
                                onClick = {
                                    viewModel.setCustomTimeLimit(10)
                                    showTimerMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Mock Mode: 15 mins", style = typography.body, color = colors.primaryText) },
                                onClick = {
                                    viewModel.setCustomTimeLimit(15)
                                    showTimerMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Mock Mode: 30 mins", style = typography.body, color = colors.primaryText) },
                                onClick = {
                                    viewModel.setCustomTimeLimit(30)
                                    showTimerMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Mock Mode: 60 mins", style = typography.body, color = colors.primaryText) },
                                onClick = {
                                    viewModel.setCustomTimeLimit(60)
                                    showTimerMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Untimed Practice", style = typography.body, color = colors.secondaryText) },
                                onClick = {
                                    viewModel.setCustomTimeLimit(null)
                                    showTimerMenu = false
                                }
                            )
                        }
                    }

                    // Question Count and Palette Launcher
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${uiState.currentQuestionIndex + 1}/${uiState.totalQuestions}",
                            style = typography.secondary,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.primaryText
                        )

                        StudyOSIconButton(
                            onClick = { viewModel.showPalette(true) },
                            contentDescription = "Question Palette"
                        ) {
                            Box(contentAlignment = Alignment.TopEnd) {
                                Icon(
                                    imageVector = Icons.Outlined.GridView,
                                    contentDescription = null,
                                    tint = if (uiState.flaggedCount > 0) colors.accent else colors.primaryText,
                                    modifier = Modifier.size(19.dp)
                                )
                                if (uiState.flaggedCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(colors.accent)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                StudyOSProgressBar(
                    progress = if (uiState.totalQuestions > 0) ((uiState.currentQuestionIndex + 1).toFloat() / uiState.totalQuestions * 100).toInt() else 0,
                    height = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Question Stepper Dots (Answered, Flagged, Current, Unanswered)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    uiState.questions.forEachIndexed { index, q ->
                        val isAnswered = uiState.selectedAnswers.containsKey(q.id)
                        val isCurrent = index == uiState.currentQuestionIndex
                        val isFlagged = uiState.flaggedQuestionIds.contains(q.id)

                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .size(if (isCurrent) 10.dp else 8.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(
                                    when {
                                        isCurrent -> colors.accent
                                        isFlagged -> Color(0xFFD39A3A)
                                        isAnswered -> colors.primaryText
                                        else -> colors.border
                                    }
                                )
                                .clickable { viewModel.jumpToQuestion(index) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Question Text Card with "Flag for Review" Bookmark
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
                            if (!question.topic.isNullOrBlank()) {
                                Text(
                                    text = question.topic.uppercase(),
                                    style = typography.caption,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.secondaryText,
                                    letterSpacing = 1.sp,
                                    modifier = Modifier.weight(1f, fill = false),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            } else {
                                Text(
                                    text = "QUESTION ${uiState.currentQuestionIndex + 1}",
                                    style = typography.caption,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.secondaryText,
                                    letterSpacing = 1.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Bookmark / Flag for Review Chip
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(shapes.button)
                                    .background(if (isCurrentFlagged) colors.accent.copy(alpha = 0.15f) else Color.Transparent)
                                    .border(
                                        0.8.dp,
                                        if (isCurrentFlagged) colors.accent else colors.border,
                                        shapes.button
                                    )
                                    .clickable { viewModel.toggleFlagCurrentQuestion() }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isCurrentFlagged) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                                    contentDescription = null,
                                    tint = if (isCurrentFlagged) colors.accent else colors.secondaryText,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isCurrentFlagged) "Flagged" else "Flag for Review",
                                    style = typography.caption.copy(fontSize = 11.sp),
                                    fontWeight = if (isCurrentFlagged) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isCurrentFlagged) colors.accent else colors.secondaryText
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = question.question,
                            style = typography.sectionTitle,
                            color = colors.primaryText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Answers Section
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when (question.type) {
                        QuestionType.MCQ -> {
                            question.options.forEach { option ->
                                val isSelected = currentAnswer == option
                                OptionItemRow(
                                    text = option,
                                    isSelected = isSelected,
                                    onClick = { viewModel.selectAnswer(question.id, option) }
                                )
                            }
                        }
                        QuestionType.TRUE_FALSE -> {
                            listOf("True", "False").forEach { option ->
                                val isSelected = currentAnswer.equals(option, ignoreCase = true)
                                OptionItemRow(
                                    text = option,
                                    isSelected = isSelected,
                                    onClick = { viewModel.selectAnswer(question.id, option) }
                                )
                            }
                        }
                        QuestionType.SHORT_ANSWER -> {
                            StudyOSTextField(
                                value = currentAnswer,
                                onValueChange = { viewModel.selectAnswer(question.id, it) },
                                label = "Your Answer",
                                placeholder = "Type your answer here...",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Navigation Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StudyOSOutlinedButton(
                        text = "Previous",
                        onClick = { viewModel.previousQuestion() },
                        enabled = uiState.currentQuestionIndex > 0,
                        modifier = Modifier.weight(1f)
                    )

                    StudyOSOutlinedButton(
                        text = "Palette",
                        onClick = { viewModel.showPalette(true) },
                        modifier = Modifier.weight(1f)
                    )

                    if (uiState.currentQuestionIndex < uiState.totalQuestions - 1) {
                        StudyOSButton(
                            text = "Next",
                            onClick = { viewModel.nextQuestion() },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        StudyOSButton(
                            text = if (uiState.isSubmitting) "Submitting..." else "Review & Submit",
                            onClick = { viewModel.showReviewDialog(true) },
                            enabled = !uiState.isSubmitting,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Question Palette Bottom Sheet
        if (uiState.showPaletteSheet) {
            QuestionPaletteBottomSheet(
                uiState = uiState,
                onSelectQuestion = { index -> viewModel.jumpToQuestion(index) },
                onDismiss = { viewModel.showPalette(false) },
                onSubmitRequest = {
                    viewModel.showPalette(false)
                    viewModel.showReviewDialog(true)
                }
            )
        }

        // Pre-Submission Review Dialog
        if (uiState.showReviewDialog) {
            PreSubmissionReviewDialog(
                uiState = uiState,
                onDismiss = { viewModel.showReviewDialog(false) },
                onConfirmSubmit = { viewModel.submitQuiz() },
                onReviewUnanswered = { viewModel.jumpToFirstUnanswered() },
                onReviewFlagged = { viewModel.jumpToFirstFlagged() }
            )
        }

        if (showExitDialog) {
            StudyOSConfirmationDialog(
                title = "Exit Quiz / Test?",
                message = "Your progress has been autosaved. You can resume this quiz anytime from the Chapter Practice Hub.",
                confirmButtonText = "Exit Quiz",
                onConfirm = {
                    showExitDialog = false
                    onBack()
                },
                onDismiss = { showExitDialog = false }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuestionPaletteBottomSheet(
    uiState: com.studyos.app.features.practice.viewmodel.QuizRunnerUiState,
    onSelectQuestion: (Int) -> Unit,
    onDismiss: () -> Unit,
    onSubmitRequest: () -> Unit
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedFilter by remember { mutableStateOf(PaletteFilter.ALL) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        scrimColor = Color.Black.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Question Palette",
                        style = typography.sectionTitle,
                        color = colors.primaryText
                    )
                    Text(
                        text = "Tap any question to jump directly to it",
                        style = typography.caption,
                        color = colors.secondaryText
                    )
                }

                StudyOSIconButton(
                    onClick = onDismiss,
                    contentDescription = "Close"
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = null,
                        tint = colors.secondaryText,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Stat Badges Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatBadge(
                    label = "Answered",
                    count = uiState.answeredCount,
                    color = colors.accent,
                    modifier = Modifier.weight(1f)
                )
                StatBadge(
                    label = "Flagged",
                    count = uiState.flaggedCount,
                    color = Color(0xFFD39A3A),
                    modifier = Modifier.weight(1f)
                )
                StatBadge(
                    label = "Unanswered",
                    count = uiState.unansweredCount,
                    color = colors.secondaryText,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Filter Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PaletteFilterChip(
                    label = "All (${uiState.totalQuestions})",
                    isSelected = selectedFilter == PaletteFilter.ALL,
                    onClick = { selectedFilter = PaletteFilter.ALL },
                    modifier = Modifier.weight(1f)
                )
                PaletteFilterChip(
                    label = "Flagged (${uiState.flaggedCount})",
                    isSelected = selectedFilter == PaletteFilter.FLAGGED,
                    onClick = { selectedFilter = PaletteFilter.FLAGGED },
                    modifier = Modifier.weight(1f)
                )
                PaletteFilterChip(
                    label = "Unanswered (${uiState.unansweredCount})",
                    isSelected = selectedFilter == PaletteFilter.UNANSWERED,
                    onClick = { selectedFilter = PaletteFilter.UNANSWERED },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Grid of Question Badges
            val filteredQuestionsWithIndex = uiState.questions.mapIndexed { index, q ->
                Triple(index, q, uiState.selectedAnswers.containsKey(q.id))
            }.filter { (index, q, isAnswered) ->
                when (selectedFilter) {
                    PaletteFilter.ALL -> true
                    PaletteFilter.FLAGGED -> uiState.flaggedQuestionIds.contains(q.id)
                    PaletteFilter.UNANSWERED -> !isAnswered
                }
            }

            if (filteredQuestionsWithIndex.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No questions match this filter.",
                        style = typography.body,
                        color = colors.secondaryText
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    itemsIndexed(filteredQuestionsWithIndex) { _, (origIdx, q, isAnswered) ->
                        val isCurrent = origIdx == uiState.currentQuestionIndex
                        val isFlagged = uiState.flaggedQuestionIds.contains(q.id)

                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(shapes.button)
                                .background(
                                    when {
                                        isCurrent -> colors.accent
                                        isFlagged -> Color(0xFFD39A3A).copy(alpha = 0.25f)
                                        isAnswered -> colors.cardBackground
                                        else -> colors.surface
                                    }
                                )
                                .border(
                                    width = if (isCurrent) 2.dp else 1.dp,
                                    color = when {
                                        isCurrent -> colors.accent
                                        isFlagged -> Color(0xFFD39A3A)
                                        isAnswered -> colors.accent.copy(alpha = 0.5f)
                                        else -> colors.border
                                    },
                                    shape = shapes.button
                                )
                                .clickable { onSelectQuestion(origIdx) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "${origIdx + 1}",
                                    style = typography.body,
                                    fontWeight = if (isCurrent || isFlagged) FontWeight.Bold else FontWeight.Medium,
                                    color = when {
                                        isCurrent -> colors.background
                                        isFlagged -> Color(0xFFD39A3A)
                                        isAnswered -> colors.primaryText
                                        else -> colors.secondaryText
                                    }
                                )
                                if (isFlagged && !isCurrent) {
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(Color(0xFFD39A3A))
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Actions: Resume vs Submit Test
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StudyOSOutlinedButton(
                    text = "Close Palette",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                )

                StudyOSButton(
                    text = "Finish & Submit",
                    onClick = onSubmitRequest,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private enum class PaletteFilter {
    ALL, FLAGGED, UNANSWERED
}

@Composable
private fun StatBadge(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    Box(
        modifier = modifier
            .clip(shapes.card)
            .background(color.copy(alpha = 0.12f))
            .border(0.5.dp, color.copy(alpha = 0.35f), shapes.card)
            .padding(vertical = 8.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$count",
                style = typography.sectionTitle,
                color = color
            )
            Text(
                text = label,
                style = typography.caption.copy(fontSize = 10.sp),
                color = colors.secondaryText
            )
        }
    }
}

@Composable
private fun PaletteFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    Box(
        modifier = modifier
            .clip(shapes.button)
            .background(if (isSelected) colors.cardBackground else Color.Transparent)
            .border(0.5.dp, if (isSelected) colors.accent else colors.border, shapes.button)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = typography.caption,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) colors.accent else colors.secondaryText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PreSubmissionReviewDialog(
    uiState: com.studyos.app.features.practice.viewmodel.QuizRunnerUiState,
    onDismiss: () -> Unit,
    onConfirmSubmit: () -> Unit,
    onReviewUnanswered: () -> Unit,
    onReviewFlagged: () -> Unit
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Submit Test & Save Score?",
                style = typography.sectionTitle,
                color = colors.primaryText
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Here is your response summary before final scoring:",
                    style = typography.body,
                    color = colors.secondaryText
                )

                // Stats summary
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shapes.card)
                        .background(colors.surface)
                        .border(1.dp, colors.border, shapes.card)
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Questions", style = typography.caption, color = colors.secondaryText)
                            Text("${uiState.totalQuestions}", style = typography.caption, fontWeight = FontWeight.SemiBold, color = colors.primaryText)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Answered", style = typography.caption, color = colors.secondaryText)
                            Text("${uiState.answeredCount}", style = typography.caption, fontWeight = FontWeight.SemiBold, color = colors.accent)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Unanswered", style = typography.caption, color = colors.secondaryText)
                            Text("${uiState.unansweredCount}", style = typography.caption, fontWeight = FontWeight.SemiBold, color = if (uiState.unansweredCount > 0) Color(0xFFFF6B6B) else colors.secondaryText)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Flagged for Review", style = typography.caption, color = colors.secondaryText)
                            Text("${uiState.flaggedCount}", style = typography.caption, fontWeight = FontWeight.SemiBold, color = Color(0xFFD39A3A))
                        }
                    }
                }

                if (uiState.unansweredCount > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shapes.card)
                            .background(Color(0xFFE53E3E).copy(alpha = 0.12f))
                            .border(0.5.dp, Color(0xFFE53E3E).copy(alpha = 0.4f), shapes.card)
                            .padding(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Warning,
                                contentDescription = null,
                                tint = Color(0xFFFF6B6B),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "You have ${uiState.unansweredCount} unanswered question(s). Unanswered questions will receive 0 marks.",
                                style = typography.caption,
                                color = Color(0xFFFF6B6B)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            StudyOSButton(
                text = "Confirm & Submit",
                onClick = onConfirmSubmit
            )
        },
        dismissButton = {
            if (uiState.unansweredCount > 0) {
                StudyOSOutlinedButton(
                    text = "Review Unanswered",
                    onClick = onReviewUnanswered
                )
            } else if (uiState.flaggedCount > 0) {
                StudyOSOutlinedButton(
                    text = "Review Flagged",
                    onClick = onReviewFlagged
                )
            } else {
                StudyOSOutlinedButton(
                    text = "Back",
                    onClick = onDismiss
                )
            }
        },
        containerColor = colors.background,
        shape = shapes.card
    )
}

@Composable
private fun OptionItemRow(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.button)
            .background(if (isSelected) colors.cardBackground else colors.surface)
            .border(1.dp, if (isSelected) colors.accent else colors.border, shapes.button)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.5.dp, if (isSelected) colors.accent else colors.secondaryText, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(colors.accent)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = text,
                style = typography.body,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = colors.primaryText
            )
        }
    }
}

@Composable
private fun QuizResultsView(
    attempt: QuizAttempt,
    questions: List<QuizQuestion>,
    userAnswers: Map<String, String>,
    onRetake: () -> Unit,
    onReviewMistakes: () -> Unit,
    onFinish: () -> Unit
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${attempt.accuracyPercentage}%",
                    style = typography.screenTitle.copy(fontSize = 44.sp),
                    color = colors.accent
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${attempt.score} of ${attempt.totalQuestions} questions correct",
                    style = typography.sectionTitle,
                    color = colors.primaryText
                )

                val minutes = attempt.timeSpentSeconds / 60
                val seconds = attempt.timeSpentSeconds % 60
                Text(
                    text = "Completed in ${minutes}m ${seconds}s",
                    style = typography.caption,
                    color = colors.secondaryText
                )
            }
        }

        // Mistake Bank Callout Banner
        val incorrectCount = attempt.totalQuestions - attempt.score
        if (incorrectCount > 0) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shapes.card)
                        .background(colors.cardBackground)
                        .border(1.dp, colors.border, shapes.card)
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "$incorrectCount Mistakes Recorded",
                                style = typography.secondary,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.primaryText
                            )
                            Text(
                                text = "Incorrect questions are saved to your Mistake Bank for revision and flashcard drill.",
                                style = typography.caption,
                                color = colors.secondaryText
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        StudyOSOutlinedButton(
                            text = "Mistake Bank",
                            onClick = onReviewMistakes
                        )
                    }
                }
            }
        }

        // Action Buttons
        item {
            val context = LocalContext.current
            val minutes = attempt.timeSpentSeconds / 60
            val seconds = attempt.timeSpentSeconds % 60

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StudyOSButton(
                    text = "Finish",
                    onClick = onFinish,
                    modifier = Modifier.weight(1f)
                )

                StudyOSOutlinedButton(
                    text = "Retake",
                    onClick = onRetake,
                    modifier = Modifier.weight(1f)
                )

                StudyOSOutlinedButton(
                    text = "Share",
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "🎯 I scored ${attempt.accuracyPercentage}% (${attempt.score}/${attempt.totalQuestions}) on my StudyOS Quiz in ${minutes}m ${seconds}s! 🚀 #StudyOS"
                            )
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Quiz Result"))
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Question Review",
                style = typography.sectionTitle,
                color = colors.primaryText
            )
        }

        // Question By Question Review List
        itemsIndexed(questions, key = { _, q -> q.id }) { index, question ->
            val userAns = userAnswers[question.id]?.trim() ?: ""
            val isCorrect = checkQuizAnswersMatch(userAns, question.correctAnswer, question.options)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shapes.card)
                    .background(colors.surface)
                    .border(1.dp, colors.border, shapes.card)
                    .padding(14.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Q${index + 1}",
                            style = typography.caption,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.secondaryText
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isCorrect) Icons.Outlined.Check else Icons.Outlined.Close,
                                contentDescription = null,
                                tint = if (isCorrect) colors.accent else colors.secondaryText,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isCorrect) "Correct" else "Incorrect",
                                style = typography.caption,
                                fontWeight = FontWeight.Medium,
                                color = if (isCorrect) colors.accent else colors.secondaryText
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = question.question,
                        style = typography.secondary,
                        fontWeight = FontWeight.Medium,
                        color = colors.primaryText
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Your Answer: ${if (userAns.isBlank()) "(Skipped)" else userAns}",
                        style = typography.caption,
                        color = if (isCorrect) colors.primaryText else colors.mutedText
                    )

                    if (!isCorrect) {
                        Text(
                            text = "Correct: ${question.correctAnswer}",
                            style = typography.caption,
                            fontWeight = FontWeight.Medium,
                            color = colors.primaryText
                        )
                    }

                    if (!question.explanation.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Explanation: ${question.explanation}",
                            style = typography.caption,
                            color = colors.secondaryText
                        )
                    }
                }
            }
        }
    }
}
