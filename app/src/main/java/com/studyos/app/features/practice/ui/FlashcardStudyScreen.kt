package com.studyos.app.features.practice.ui

import android.speech.tts.TextToSpeech
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
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
import com.studyos.app.domain.model.FlashcardRating
import com.studyos.app.features.practice.viewmodel.FlashcardStudyViewModel
import com.studyos.app.theme.StudyOSTheme

@Composable
fun FlashcardStudyScreen(
    viewModel: FlashcardStudyViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes
    val context = LocalContext.current

    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var isTtsReady by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        val speechEngine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsReady = true
            }
        }
        tts = speechEngine
        onDispose {
            speechEngine.stop()
            speechEngine.shutdown()
        }
    }

    val speakText = { text: String ->
        if (isTtsReady) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "FLASHCARD_TTS")
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        if (uiState.isLoading) {
            StudyOSLoadingState(message = "Loading flashcards...")
        } else if (uiState.cards.isEmpty()) {
            StudyOSEmptyState(
                title = "No flashcards to review",
                description = "All cards are up to date, or none have been added for this chapter yet.",
                actionButtonText = "Go Back",
                onActionClick = onBack
            )
        } else if (uiState.isFinished) {
            // Finished Deck Summary
            DeckFinishedView(
                reviewedCount = uiState.reviewedCount,
                againCount = uiState.againCount,
                hardCount = uiState.hardCount,
                goodCount = uiState.goodCount,
                easyCount = uiState.easyCount,
                onRestart = { viewModel.restartDeck() },
                onFinish = onBack
            )
        } else {
            val card = uiState.currentCard ?: return
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                // Top Navigation Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
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

                    Text(
                        text = "${uiState.currentIndex + 1} of ${uiState.cards.size}",
                        style = typography.secondary,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.primaryText
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StudyOSIconButton(
                            onClick = { viewModel.shuffleDeck() },
                            contentDescription = "Shuffle Deck"
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Shuffle,
                                contentDescription = null,
                                tint = colors.secondaryText,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                StudyOSProgressBar(
                    progress = ((uiState.currentIndex + 1).toFloat() / uiState.cards.size * 100).toInt(),
                    height = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Flashcard Interactive Container
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(shapes.surface)
                        .background(colors.surface)
                        .border(1.dp, colors.border, shapes.surface)
                        .clickable(enabled = !uiState.isAnswerRevealed) { viewModel.revealAnswer() }
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "QUESTION",
                                style = typography.caption,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.secondaryText,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            StudyOSIconButton(
                                onClick = { speakText(card.question) },
                                contentDescription = "Pronounce question"
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.VolumeUp,
                                    contentDescription = null,
                                    tint = colors.accent,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = card.question,
                            style = typography.sectionTitle,
                            color = colors.primaryText,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        AnimatedVisibility(
                            visible = uiState.isAnswerRevealed,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(colors.border)
                                )

                                Spacer(modifier = Modifier.height(24.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "ANSWER",
                                        style = typography.caption,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.secondaryText,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    StudyOSIconButton(
                                        onClick = { speakText(card.answer) },
                                        contentDescription = "Pronounce answer"
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.VolumeUp,
                                            contentDescription = null,
                                            tint = colors.accent,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = card.answer,
                                    style = typography.body,
                                    fontSize = 16.sp,
                                    color = colors.primaryText,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        if (!uiState.isAnswerRevealed) {
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = "Tap anywhere to reveal answer",
                                style = typography.caption,
                                color = colors.mutedText
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Controls: Tap to Reveal or SM-2 Rating Row
                if (!uiState.isAnswerRevealed) {
                    StudyOSButton(
                        text = "Reveal Answer",
                        onClick = { viewModel.revealAnswer() },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Column {
                        Text(
                            text = "Rate your recall difficulty:",
                            style = typography.caption,
                            color = colors.secondaryText,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RatingButton(
                                label = "Again",
                                sublabel = "< 1d",
                                onClick = { viewModel.rateCard(FlashcardRating.AGAIN) },
                                modifier = Modifier.weight(1f)
                            )

                            RatingButton(
                                label = "Hard",
                                sublabel = "2d",
                                onClick = { viewModel.rateCard(FlashcardRating.HARD) },
                                modifier = Modifier.weight(1f)
                            )

                            RatingButton(
                                label = "Good",
                                sublabel = "4d",
                                onClick = { viewModel.rateCard(FlashcardRating.GOOD) },
                                modifier = Modifier.weight(1f)
                            )

                            RatingButton(
                                label = "Easy",
                                sublabel = "7d",
                                onClick = { viewModel.rateCard(FlashcardRating.EASY) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun RatingButton(
    label: String,
    sublabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    Box(
        modifier = modifier
            .clip(shapes.button)
            .background(colors.surface)
            .border(1.dp, colors.border, shapes.button)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                style = typography.secondary,
                fontWeight = FontWeight.SemiBold,
                color = colors.primaryText
            )
            Text(
                text = sublabel,
                style = typography.caption.copy(fontSize = 11.sp),
                color = colors.secondaryText
            )
        }
    }
}

@Composable
private fun DeckFinishedView(
    reviewedCount: Int,
    againCount: Int,
    hardCount: Int,
    goodCount: Int,
    easyCount: Int,
    onRestart: () -> Unit,
    onFinish: () -> Unit
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Outlined.CheckCircle,
            contentDescription = null,
            tint = colors.accent,
            modifier = Modifier.size(54.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Review Completed!",
            style = typography.screenTitle,
            color = colors.primaryText
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "You reviewed $reviewedCount cards. Next intervals updated automatically with spaced repetition.",
            style = typography.body,
            color = colors.secondaryText,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Breakdown Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shapes.card)
                .background(colors.surface)
                .border(1.dp, colors.border, shapes.card)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                ResultMetric(label = "Again", count = againCount)
                ResultMetric(label = "Hard", count = hardCount)
                ResultMetric(label = "Good", count = goodCount)
                ResultMetric(label = "Easy", count = easyCount)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        StudyOSButton(
            text = "Finish Session",
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        StudyOSOutlinedButton(
            text = "Study Again",
            onClick = onRestart,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ResultMetric(label: String, count: Int) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
            style = typography.sectionTitle,
            color = colors.primaryText
        )
        Text(
            text = label,
            style = typography.caption,
            color = colors.secondaryText
        )
    }
}
