package com.studyos.app.features.practice.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studyos.app.core.ui.component.GlassCard
import com.studyos.app.core.ui.component.GlassIconButton
import com.studyos.app.core.ui.component.StudyOSButton
import com.studyos.app.core.ui.component.StudyOSOutlinedButton
import com.studyos.app.core.ui.component.StudyOSProgressBar
import com.studyos.app.features.practice.viewmodel.AiStudySessionViewModel
import com.studyos.app.features.practice.viewmodel.AiStudyStep
import com.studyos.app.theme.StudyOSTheme
import java.util.Locale

@Composable
fun AiStudySessionScreen(
    viewModel: AiStudySessionViewModel,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Distraction-Free Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = (uiState.subject?.name ?: "Study").uppercase(),
                        style = typography.caption,
                        fontWeight = FontWeight.Bold,
                        color = colors.accent,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = uiState.chapter?.name ?: "Deep Work Session",
                        style = typography.sectionTitle,
                        color = colors.primaryText,
                        maxLines = 1
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Timer pill
                    val min = uiState.elapsedSeconds / 60
                    val sec = uiState.elapsedSeconds % 60
                    val timerStr = String.format(Locale.US, "%02d:%02d", min, sec)

                    Row(
                        modifier = Modifier
                            .clip(shapes.small)
                            .background(colors.surface)
                            .border(1.dp, colors.border, shapes.small)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Timer,
                            contentDescription = null,
                            tint = if (uiState.isPaused) colors.secondaryText else colors.accent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = timerStr,
                            style = typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                            color = colors.primaryText
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    GlassIconButton(
                        onClick = viewModel::togglePause,
                        contentDescription = if (uiState.isPaused) "Resume" else "Pause"
                    ) {
                        Icon(
                            imageVector = if (uiState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = null,
                            tint = colors.primaryText,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    GlassIconButton(
                        onClick = onFinish,
                        contentDescription = "Exit"
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = colors.secondaryText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Step Progress Bar
            val stepFraction = when (uiState.step) {
                AiStudyStep.LEARN -> 0.25f
                AiStudyStep.RECALL -> 0.50f
                AiStudyStep.PRACTICE -> 0.75f
                AiStudyStep.EVALUATE -> 0.90f
                AiStudyStep.COMPLETE -> 1.0f
            }
            StudyOSProgressBar(
                progress = stepFraction,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = uiState.step.title,
                    style = typography.caption,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.accent
                )
                Text(
                    text = if (uiState.isPaused) "PAUSED" else "IN FOCUS",
                    style = typography.caption,
                    color = if (uiState.isPaused) colors.secondaryText else colors.success
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Main Content Area based on Step
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                when (uiState.step) {
                    AiStudyStep.LEARN -> {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(24.dp)) {
                                Text(
                                    text = "Phase 1: Concept Absorption",
                                    style = typography.sectionTitle,
                                    color = colors.primaryText
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Read through your chapter material, primary notes, and core formulas. Spend this focused block absorbing key principles before active testing.",
                                    style = typography.body,
                                    color = colors.secondaryText
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                Text(
                                    text = "Target Chapter: ${uiState.chapter?.name ?: "All Topics"}",
                                    style = typography.bodyMedium,
                                    color = colors.primaryText
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        StudyOSButton(
                            text = "Proceed to Active Recall →",
                            onClick = viewModel::nextStep,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    AiStudyStep.RECALL -> {
                        val items = uiState.recallItems
                        val currentItem = items.getOrNull(uiState.currentRecallIndex)

                        if (currentItem == null) {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Recall Prompts Ready",
                                        style = typography.sectionTitle,
                                        color = colors.primaryText
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Can you define the governing formula and primary mechanism of ${uiState.chapter?.name} from memory?",
                                        style = typography.body,
                                        color = colors.secondaryText,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                            StudyOSButton(
                                text = "Proceed to Practice →",
                                onClick = viewModel::nextStep,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(24.dp)) {
                                    Text(
                                        text = "RECALL PROMPT (${uiState.currentRecallIndex + 1}/${items.size})",
                                        style = typography.caption,
                                        color = colors.accent
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = currentItem.prompt,
                                        style = typography.sectionTitle,
                                        color = colors.primaryText
                                    )

                                    if (uiState.isRecallAnswerRevealed) {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(shapes.surface)
                                                .background(colors.background)
                                                .border(1.dp, colors.border, shapes.surface)
                                                .padding(14.dp)
                                        ) {
                                            Column {
                                                Text(
                                                    text = "EXPECTED ANSWER",
                                                    style = typography.caption,
                                                    color = colors.accent
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = currentItem.expectedAnswer,
                                                    style = typography.bodyMedium,
                                                    color = colors.primaryText
                                                )
                                                if (currentItem.explanation.isNotBlank()) {
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    Text(
                                                        text = currentItem.explanation,
                                                        style = typography.caption,
                                                        color = colors.secondaryText
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            if (!uiState.isRecallAnswerRevealed) {
                                StudyOSButton(
                                    text = "Reveal Answer",
                                    onClick = viewModel::revealRecallAnswer,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    StudyOSOutlinedButton(
                                        text = "Didn't Remember",
                                        onClick = { viewModel.recordRecallAttempt(false) },
                                        modifier = Modifier.weight(1f)
                                    )
                                    StudyOSButton(
                                        text = "Recalled Correctly",
                                        onClick = { viewModel.recordRecallAttempt(true) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    AiStudyStep.PRACTICE -> {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(24.dp)) {
                                Text(
                                    text = "Phase 3: Rapid Practice Drill",
                                    style = typography.sectionTitle,
                                    color = colors.primaryText
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Solve 5 quick practice problems or review past exam questions for ${uiState.chapter?.name}.",
                                    style = typography.body,
                                    color = colors.secondaryText
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                Text(
                                    text = "How many problems did you solve correctly?",
                                    style = typography.bodyMedium,
                                    color = colors.primaryText
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            (3..5).forEach { score ->
                                StudyOSButton(
                                    text = "$score / 5",
                                    onClick = { viewModel.recordPracticeResult(score, 5) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    AiStudyStep.EVALUATE -> {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(24.dp)) {
                                Text(
                                    text = "Phase 4: Synthesis & Feedback",
                                    style = typography.sectionTitle,
                                    color = colors.primaryText
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "StudyOS is integrating your retention metrics, updating your Chapter Intelligence 4-quadrant breakdown, and scheduling tomorrow's review.",
                                    style = typography.body,
                                    color = colors.secondaryText
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        StudyOSButton(
                            text = "Complete Session & Save →",
                            onClick = viewModel::nextStep,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    AiStudyStep.COMPLETE -> {
                        // Session Complete Card
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = colors.accent,
                                    modifier = Modifier.size(52.dp)
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = "Session Complete",
                                    style = typography.screenTitle,
                                    color = colors.primaryText
                                )

                                val minutes = (uiState.elapsedSeconds / 60).coerceAtLeast(1)
                                Text(
                                    text = "$minutes min studied",
                                    style = typography.secondary,
                                    color = colors.secondaryText
                                )

                                Spacer(modifier = Modifier.height(24.dp))

                                // Metrics Breakdown Table
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(text = "Recall", style = typography.caption, color = colors.secondaryText)
                                        Text(
                                            text = "${uiState.recallCorrectCount}/${uiState.recallTotalAttempted.coerceAtLeast(uiState.recallCorrectCount).coerceAtLeast(1)}",
                                            style = typography.sectionTitle,
                                            color = colors.primaryText
                                        )
                                        Text(text = "Recall ↑", style = typography.caption, color = colors.success)
                                    }

                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(text = "Practice", style = typography.caption, color = colors.secondaryText)
                                        Text(
                                            text = "${uiState.practiceScore}/${uiState.practiceTotal}",
                                            style = typography.sectionTitle,
                                            color = colors.primaryText
                                        )
                                        Text(text = "Understanding ↑", style = typography.caption, color = colors.success)
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(shapes.surface)
                                        .background(colors.background)
                                        .border(1.dp, colors.border, shapes.surface)
                                        .padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Next review: ${uiState.nextReviewDate}",
                                        style = typography.bodyMedium,
                                        color = colors.accent
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        StudyOSButton(
                            text = "Return to Desk",
                            onClick = onFinish,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
