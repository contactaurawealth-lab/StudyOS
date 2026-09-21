package com.studyos.app.features.practice.ui

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studyos.app.core.ui.component.StudyOSButton
import com.studyos.app.core.ui.component.StudyOSEmptyState
import com.studyos.app.core.ui.component.StudyOSIconButton
import com.studyos.app.core.ui.component.StudyOSLoadingState
import com.studyos.app.core.ui.component.StudyOSOutlinedButton
import com.studyos.app.core.ui.component.StudyOSProgressBar
import com.studyos.app.core.ui.component.StudyOSTextField
import com.studyos.app.domain.model.QuestionType
import com.studyos.app.domain.model.QuizAttempt
import com.studyos.app.domain.model.QuizQuestion
import com.studyos.app.features.practice.viewmodel.QuizRunnerViewModel
import com.studyos.app.theme.StudyOSTheme

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
            StudyOSLoadingState(message = "Preparing quiz...")
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
            val question = uiState.currentQuestion ?: return
            val currentAnswer = uiState.selectedAnswers[question.id] ?: ""

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                // Top Bar: Back, Timer, Question Counter
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StudyOSIconButton(
                        onClick = onBack,
                        contentDescription = "Exit Quiz"
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = null,
                            tint = colors.primaryText,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Timer Display
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Timer,
                            contentDescription = null,
                            tint = colors.secondaryText,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        val minutes = uiState.elapsedSeconds / 60
                        val seconds = uiState.elapsedSeconds % 60
                        Text(
                            text = String.format("%02d:%02d", minutes, seconds),
                            style = typography.secondary,
                            fontWeight = FontWeight.Medium,
                            color = colors.primaryText
                        )
                    }

                    Text(
                        text = "${uiState.currentQuestionIndex + 1}/${uiState.totalQuestions}",
                        style = typography.secondary,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.primaryText
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                StudyOSProgressBar(
                    progress = ((uiState.currentQuestionIndex + 1).toFloat() / uiState.totalQuestions * 100).toInt(),
                    height = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Question Stepper Dots
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    uiState.questions.forEachIndexed { index, q ->
                        val isAnswered = uiState.selectedAnswers.containsKey(q.id)
                        val isCurrent = index == uiState.currentQuestionIndex

                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .size(if (isCurrent) 10.dp else 8.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(
                                    when {
                                        isCurrent -> colors.accent
                                        isAnswered -> colors.primaryText
                                        else -> colors.border
                                    }
                                )
                                .clickable { viewModel.jumpToQuestion(index) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Question Text Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shapes.surface)
                        .background(colors.surface)
                        .border(1.dp, colors.border, shapes.surface)
                        .padding(20.dp)
                ) {
                    Column {
                        if (!question.topic.isNullOrBlank()) {
                            Text(
                                text = question.topic.uppercase(),
                                style = typography.caption,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.secondaryText,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        Text(
                            text = question.question,
                            style = typography.sectionTitle,
                            color = colors.primaryText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

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
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StudyOSOutlinedButton(
                        text = "Previous",
                        onClick = { viewModel.previousQuestion() },
                        enabled = uiState.currentQuestionIndex > 0,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    if (uiState.currentQuestionIndex < uiState.totalQuestions - 1) {
                        StudyOSButton(
                            text = "Next",
                            onClick = { viewModel.nextQuestion() },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        StudyOSButton(
                            text = if (uiState.isSubmitting) "Submitting..." else "Submit Quiz",
                            onClick = { viewModel.submitQuiz() },
                            enabled = !uiState.isSubmitting,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
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
            val context = androidx.compose.ui.platform.LocalContext.current
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
                        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                android.content.Intent.EXTRA_TEXT,
                                "🎯 I scored ${attempt.accuracyPercentage}% (${attempt.score}/${attempt.totalQuestions}) on my StudyOS Quiz in ${minutes}m ${seconds}s! 🚀 #StudyOS"
                            )
                        }
                        context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Quiz Result"))
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
            val isCorrect = userAns.equals(question.correctAnswer.trim(), ignoreCase = true)

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
