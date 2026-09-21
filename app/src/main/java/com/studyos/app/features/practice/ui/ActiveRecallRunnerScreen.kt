package com.studyos.app.features.practice.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ElectricBolt
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studyos.app.core.ui.component.StudyOSButton
import com.studyos.app.core.ui.component.StudyOSDivider
import com.studyos.app.core.ui.component.StudyOSEmptyState
import com.studyos.app.core.ui.component.StudyOSIconButton
import com.studyos.app.core.ui.component.StudyOSLoadingState
import com.studyos.app.core.ui.component.StudyOSOutlinedButton
import com.studyos.app.core.ui.component.StudyOSProgressBar
import com.studyos.app.core.ui.component.StudyOSTextField
import com.studyos.app.domain.model.ActiveRecallItemType
import com.studyos.app.domain.model.ActiveRecallSessionSummary
import com.studyos.app.domain.model.RecallEvaluationStatus
import com.studyos.app.features.practice.viewmodel.ActiveRecallRunnerViewModel
import com.studyos.app.theme.StudyOSTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveRecallRunnerScreen(
    viewModel: ActiveRecallRunnerViewModel,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    val minutes = uiState.elapsedSeconds / 60
    val seconds = uiState.elapsedSeconds % 60
    val timerString = String.format("%02d:%02d", minutes, seconds)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = uiState.sessionType.title,
                            style = typography.subsectionTitle,
                            color = colors.primaryText
                        )
                        Text(
                            text = "Elapsed: $timerString",
                            style = typography.caption,
                            color = colors.secondaryText
                        )
                    }
                },
                navigationIcon = {
                    StudyOSIconButton(
                        onClick = onFinish,
                        contentDescription = "Exit session"
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = null,
                            tint = colors.secondaryText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clip(shapes.statusPill)
                            .background(if (uiState.isFeynmanMode) colors.accent.copy(alpha = 0.18f) else colors.surface)
                            .border(1.dp, if (uiState.isFeynmanMode) colors.accent else colors.border, shapes.statusPill)
                            .clickable { viewModel.toggleFeynmanMode() }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                            .semantics { this.role = Role.Button }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Psychology,
                                contentDescription = null,
                                tint = if (uiState.isFeynmanMode) colors.accent else colors.secondaryText,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (uiState.isFeynmanMode) "Feynman 3m" else "Feynman Mode",
                                style = typography.caption.copy(
                                    fontSize = 11.sp,
                                    fontWeight = if (uiState.isFeynmanMode) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (uiState.isFeynmanMode) colors.accent else colors.secondaryText
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.background,
                    titleContentColor = colors.primaryText
                )
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            StudyOSLoadingState(message = "Preparing recall session queue...")
        } else if (uiState.isFinished && uiState.summary != null) {
            // Session Completion Screen
            ActiveRecallSummaryView(
                summary = uiState.summary!!,
                onDone = onFinish,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else if (uiState.items.isEmpty()) {
            StudyOSEmptyState(
                title = "No Recall Items Found",
                description = "There are currently no items due for recall in this category.",
                actionButtonText = "Return",
                onActionClick = onFinish,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
            val currentItem = uiState.currentItem
            if (currentItem == null) {
                StudyOSEmptyState(
                    title = "Session Completed",
                    description = "You have reached the end of the recall queue.",
                    actionButtonText = "Done",
                    onActionClick = onFinish,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Header progress bar
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Item ${uiState.currentIndex + 1} of ${uiState.items.size}",
                                style = typography.caption.copy(fontWeight = FontWeight.Medium),
                                color = colors.secondaryText
                            )

                            // Type badge
                            val badgeLabel = when (currentItem.type) {
                                ActiveRecallItemType.FLASHCARD -> "Flashcard Recall"
                                ActiveRecallItemType.MISTAKE_RETRY -> "Mistake Drill"
                                ActiveRecallItemType.CONCEPT_QUIZ -> "Concept Quiz"
                            }
                            Box(
                                modifier = Modifier
                                    .clip(shapes.statusPill)
                                    .background(colors.surface)
                                    .border(1.dp, colors.border, shapes.statusPill)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = badgeLabel,
                                    style = typography.caption.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
                                    color = colors.accent
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        StudyOSProgressBar(
                            progress = uiState.progressFraction,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Recall Card Content
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(shapes.card)
                            .background(colors.cardBackground)
                            .border(1.dp, colors.border, shapes.card)
                            .padding(20.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Column {
                            Text(
                                text = currentItem.title,
                                style = typography.caption,
                                color = colors.secondaryText
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = currentItem.prompt,
                                style = typography.body.copy(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 26.sp
                                ),
                                color = colors.primaryText
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // Quiz Options if MCQ
                            if (currentItem.type == ActiveRecallItemType.CONCEPT_QUIZ && currentItem.options.isNotEmpty()) {
                                currentItem.options.forEach { option ->
                                    val isSelected = uiState.selectedOption == option
                                    val isCorrect = option.equals(currentItem.answer, ignoreCase = true)
                                    val showResult = uiState.isAnswerRevealed

                                    val optionBorderColor = when {
                                        showResult && isCorrect -> colors.success
                                        showResult && isSelected && !isCorrect -> colors.error
                                        isSelected -> colors.accent
                                        else -> colors.border
                                    }

                                    val optionBgColor = when {
                                        showResult && isCorrect -> colors.success.copy(alpha = 0.1f)
                                        showResult && isSelected && !isCorrect -> colors.error.copy(alpha = 0.1f)
                                        else -> colors.surface
                                    }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clip(shapes.card)
                                            .background(optionBgColor)
                                            .border(1.dp, optionBorderColor, shapes.card)
                                            .clickable(enabled = !uiState.isAnswerRevealed) {
                                                viewModel.selectOption(option)
                                            }
                                            .padding(14.dp)
                                            .semantics { this.role = Role.Button }
                                    ) {
                                        Text(
                                            text = option,
                                            style = typography.body,
                                            color = colors.primaryText
                                        )
                                    }
                                }
                            }

                            if (uiState.isFeynmanMode) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(shapes.card)
                                        .background(colors.accent.copy(alpha = 0.08f))
                                        .border(1.dp, colors.accent.copy(alpha = 0.3f), shapes.card)
                                        .padding(12.dp)
                                ) {
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Feynman Free-Recall (3 min)",
                                                style = typography.caption.copy(fontWeight = FontWeight.Bold),
                                                color = colors.accent
                                            )
                                            val timerColor = if (uiState.feynmanRemainingSeconds <= 30) colors.critical else colors.accent
                                            Text(
                                                text = uiState.feynmanTimerString,
                                                style = typography.caption.copy(fontWeight = FontWeight.Bold),
                                                color = timerColor
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Explain this concept from scratch in plain English. No memorized jargon without simple analogies. Stumbling highlights your exact knowledge gap.",
                                            style = typography.caption,
                                            color = colors.secondaryText
                                        )
                                    }
                                }
                            }

                            // Optional active typing before reveal
                            if (!uiState.isAnswerRevealed && currentItem.type != ActiveRecallItemType.CONCEPT_QUIZ) {
                                Spacer(modifier = Modifier.height(14.dp))
                                StudyOSTextField(
                                    value = uiState.typedAnswer,
                                    onValueChange = viewModel::updateTypedAnswer,
                                    placeholder = if (uiState.isFeynmanMode) {
                                        "Write your plain-English breakdown here... What happens? Why? Step by step..."
                                    } else {
                                        "Type your answer to test active recall..."
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            // Answer reveal block & Evaluation Feedback
                            AnimatedVisibility(
                                visible = uiState.isAnswerRevealed,
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    StudyOSDivider(modifier = Modifier.padding(vertical = 14.dp))

                                    val eval = uiState.evaluationResult
                                    val resultLabel = when {
                                        uiState.isFeynmanMode -> "Feynman Comparative Analysis"
                                        eval?.status == RecallEvaluationStatus.CORRECT -> "Correct"
                                        eval?.status == RecallEvaluationStatus.PARTIALLY_CORRECT -> "Partially Correct"
                                        eval?.status == RecallEvaluationStatus.INCORRECT -> "Needs Review"
                                        else -> if (uiState.selectedOption.equals(currentItem.answer, ignoreCase = true)) "Correct" else "Revealed"
                                    }
                                    val resultColor = when {
                                        uiState.isFeynmanMode -> colors.accent
                                        eval?.status == RecallEvaluationStatus.CORRECT -> colors.success
                                        eval?.status == RecallEvaluationStatus.PARTIALLY_CORRECT -> colors.warning
                                        eval?.status == RecallEvaluationStatus.INCORRECT -> colors.critical
                                        else -> colors.accent
                                    }

                                    // Evaluation Feedback Card
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(shapes.card)
                                            .background(colors.surface.copy(alpha = 0.5f))
                                            .border(1.dp, resultColor.copy(alpha = 0.4f), shapes.card)
                                            .padding(14.dp)
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            // Result Header
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = if (uiState.isFeynmanMode) "FEYNMAN SIDE-BY-SIDE" else "RECALL EVALUATION",
                                                    style = typography.caption,
                                                    fontWeight = FontWeight.Bold,
                                                    color = colors.secondaryText
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .clip(shapes.surface)
                                                        .background(resultColor.copy(alpha = 0.14f))
                                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = resultLabel,
                                                        style = typography.caption,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = resultColor
                                                    )
                                                }
                                            }

                                            // Your Answer (if typed)
                                            val studentAnswer = uiState.typedAnswer.ifBlank { uiState.selectedOption }
                                            if (!studentAnswer.isNullOrBlank()) {
                                                Column {
                                                    Text(
                                                        text = if (uiState.isFeynmanMode) "Your Free-Recall Explanation:" else "Your answer:",
                                                        style = typography.caption,
                                                        color = colors.secondaryText
                                                    )
                                                    Text(
                                                        text = studentAnswer,
                                                        style = typography.body,
                                                        fontWeight = FontWeight.Medium,
                                                        color = colors.primaryText
                                                    )
                                                }
                                            }

                                            // Expected Answer
                                            Column {
                                                Text(
                                                    text = if (uiState.isFeynmanMode) "Reference Answer & Mechanism:" else "Expected answer:",
                                                    style = typography.caption,
                                                    color = colors.success
                                                )
                                                Text(
                                                    text = currentItem.answer,
                                                    style = typography.body,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = colors.primaryText
                                                )
                                            }

                                            // Why / Reference Notes
                                            val whyText = currentItem.explanation?.ifBlank { null }
                                                ?: eval?.whyExplanation?.ifBlank { null }
                                                ?: "Core fundamental principle of this concept."
                                            Column {
                                                Text(
                                                    text = if (uiState.isFeynmanMode) "Reference Concept / Notes:" else "Why:",
                                                    style = typography.caption,
                                                    color = colors.accent
                                                )
                                                Text(
                                                    text = whyText,
                                                    style = typography.caption,
                                                    color = colors.secondaryText
                                                )
                                            }

                                            // Feynman Self-Scoring Rubric (when in Feynman Mode)
                                            if (uiState.isFeynmanMode) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                StudyOSDivider()
                                                Spacer(modifier = Modifier.height(4.dp))

                                                Text(
                                                    text = "Feynman Self-Scoring Rubric:",
                                                    style = typography.caption.copy(fontWeight = FontWeight.Bold),
                                                    color = colors.accent
                                                )

                                                listOf(
                                                    Triple("simple", "Plain English (no unexplained jargon)", uiState.feynmanRubricSimple),
                                                    Triple("accurate", "Mechanism Accurate (cause & effect sound)", uiState.feynmanRubricAccurate),
                                                    Triple("gaps", "Gaps Identified (spotted fuzzy points)", uiState.feynmanRubricGapsIdentified)
                                                ).forEach { (id, label, isChecked) ->
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clickable { viewModel.toggleFeynmanRubric(id) }
                                                            .padding(vertical = 3.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(16.dp)
                                                                .clip(shapes.small)
                                                                .background(if (isChecked) colors.success else colors.surface)
                                                                .border(1.dp, if (isChecked) colors.success else colors.border, shapes.small),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            if (isChecked) {
                                                                Icon(
                                                                    imageVector = Icons.Outlined.CheckCircle,
                                                                    contentDescription = null,
                                                                    tint = colors.cardBackground,
                                                                    modifier = Modifier.size(12.dp)
                                                                )
                                                            }
                                                        }
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(
                                                            text = label,
                                                            style = typography.caption,
                                                            color = if (isChecked) colors.primaryText else colors.secondaryText
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Bottom Action Controls
                    if (!uiState.isAnswerRevealed) {
                        if (currentItem.type != ActiveRecallItemType.CONCEPT_QUIZ) {
                            StudyOSButton(
                                text = if (uiState.isFeynmanMode) "Compare & Self-Score (Feynman)" else "Reveal Answer",
                                onClick = viewModel::revealAnswer,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            StudyOSOutlinedButton(
                                text = "Skip Item",
                                onClick = viewModel::skipItem,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        // After reveal: Rate recall
                        if (currentItem.type == ActiveRecallItemType.CONCEPT_QUIZ) {
                            val isCorrect = uiState.selectedOption.equals(currentItem.answer, ignoreCase = true)
                            StudyOSButton(
                                text = "Next Question",
                                onClick = { viewModel.recordResult(wasCorrect = isCorrect) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                StudyOSOutlinedButton(
                                    text = if (uiState.isFeynmanMode) "Needs Work (Gap)" else "Need Review",
                                    onClick = { viewModel.recordResult(wasCorrect = false) },
                                    modifier = Modifier.weight(1f)
                                )

                                StudyOSButton(
                                    text = if (uiState.isFeynmanMode) "Mastered (Clear)" else "Got It!",
                                    onClick = { viewModel.recordResult(wasCorrect = true) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveRecallSummaryView(
    summary: ActiveRecallSessionSummary,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    val minutes = summary.durationSeconds / 60
    val seconds = summary.durationSeconds % 60

    Column(
        modifier = modifier
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(shapes.surface)
                .background(colors.cardBackground)
                .border(1.dp, colors.border, shapes.surface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = colors.success,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Active Recall Complete!",
            style = typography.screenTitle,
            color = colors.primaryText,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Memory pathways reinforced for ${summary.itemsReviewedCount} items.",
            style = typography.body,
            color = colors.secondaryText,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Stats grid
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shapes.card)
                .background(colors.cardBackground)
                .border(1.dp, colors.border, shapes.card)
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Accuracy",
                        style = typography.body,
                        color = colors.secondaryText
                    )
                    Text(
                        text = "${summary.accuracyPercentage}%",
                        style = typography.body.copy(fontWeight = FontWeight.Bold),
                        color = colors.primaryText
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Items Mastered",
                        style = typography.body,
                        color = colors.secondaryText
                    )
                    Text(
                        text = "${summary.correctCount} / ${summary.itemsReviewedCount}",
                        style = typography.body.copy(fontWeight = FontWeight.Bold),
                        color = colors.primaryText
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Duration",
                        style = typography.body,
                        color = colors.secondaryText
                    )
                    Text(
                        text = String.format("%02d:%02d", minutes, seconds),
                        style = typography.body.copy(fontWeight = FontWeight.Bold),
                        color = colors.primaryText
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Revision Streak",
                        style = typography.body,
                        color = colors.secondaryText
                    )
                    Text(
                        text = "${summary.streakDays} Days 🔥",
                        style = typography.body.copy(fontWeight = FontWeight.Bold),
                        color = colors.accent
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Mastery Gain",
                        style = typography.body,
                        color = colors.secondaryText
                    )
                    Text(
                        text = summary.masteryGainedText,
                        style = typography.body.copy(fontWeight = FontWeight.Bold),
                        color = colors.success
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        StudyOSButton(
            text = "Back to Revision Desk",
            onClick = onDone,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
