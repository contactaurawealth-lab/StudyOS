package com.studyos.app.features.practice.ui

import android.speech.tts.TextToSpeech
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.abs
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
                wrongCount = uiState.wrongCount,
                rightCount = uiState.rightCount,
                accuracy = uiState.accuracyPercentage,
                onRestart = { viewModel.restartDeck() },
                onFinish = onBack
            )
        } else {
            val card = uiState.currentCard ?: return
            val coroutineScope = rememberCoroutineScope()
            val offsetX = remember { Animatable(0f) }
            val swipeThreshold = 260f

            LaunchedEffect(card.id) {
                offsetX.snapTo(0f)
            }

            val dragOffset = offsetX.value
            val isSwipingLeft = dragOffset < 0
            val isSwipingRight = dragOffset > 0
            val swipeRatio = (abs(dragOffset) / swipeThreshold).coerceIn(0f, 1f)

            val dynamicBorderColor = when {
                isSwipingLeft -> Color(0xFFE53E3E).copy(alpha = 0.35f + swipeRatio * 0.65f)
                isSwipingRight -> Color(0xFF38A169).copy(alpha = 0.35f + swipeRatio * 0.65f)
                else -> colors.border
            }

            val dynamicBgColor = when {
                isSwipingLeft -> Color(0xFFE53E3E).copy(alpha = swipeRatio * 0.15f)
                isSwipingRight -> Color(0xFF38A169).copy(alpha = swipeRatio * 0.15f)
                else -> Color.Transparent
            }

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

                // Flashcard Interactive Container with Swipe Physics
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .graphicsLayer {
                            translationX = offsetX.value
                            rotationZ = (offsetX.value / 25f).coerceIn(-15f, 15f)
                        }
                        .pointerInput(card.id) {
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    if (offsetX.value < -swipeThreshold) {
                                        coroutineScope.launch {
                                            offsetX.animateTo(-1200f, tween(180))
                                            viewModel.rateWrong()
                                            offsetX.snapTo(0f)
                                        }
                                    } else if (offsetX.value > swipeThreshold) {
                                        coroutineScope.launch {
                                            offsetX.animateTo(1200f, tween(180))
                                            viewModel.rateRight()
                                            offsetX.snapTo(0f)
                                        }
                                    } else {
                                        coroutineScope.launch {
                                            offsetX.animateTo(
                                                0f,
                                                spring(
                                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                                    stiffness = Spring.StiffnessLow
                                                )
                                            )
                                        }
                                    }
                                },
                                onDragCancel = {
                                    coroutineScope.launch {
                                        offsetX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                                    }
                                },
                                onHorizontalDrag = { _, dragAmount ->
                                    coroutineScope.launch {
                                        offsetX.snapTo(offsetX.value + dragAmount)
                                    }
                                }
                            )
                        }
                        .clip(shapes.surface)
                        .background(colors.surface)
                        .background(dynamicBgColor)
                        .border(
                            width = if (swipeRatio > 0.05f) 2.dp else 1.dp,
                            color = dynamicBorderColor,
                            shape = shapes.surface
                        )
                        .clickable(enabled = !uiState.isAnswerRevealed) { viewModel.revealAnswer() }
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Swipe feedback badges
                    if (isSwipingLeft && swipeRatio > 0.08f) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .graphicsLayer { alpha = swipeRatio }
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFE53E3E).copy(alpha = 0.92f))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "WRONG",
                                    style = typography.caption.copy(fontWeight = FontWeight.Bold, color = Color.White, fontSize = 11.sp)
                                )
                            }
                        }
                    }

                    if (isSwipingRight && swipeRatio > 0.08f) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .graphicsLayer { alpha = swipeRatio }
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF38A169).copy(alpha = 0.92f))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "RIGHT",
                                    style = typography.caption.copy(fontWeight = FontWeight.Bold, color = Color.White, fontSize = 11.sp)
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
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
                                    imageVector = Icons.AutoMirrored.Outlined.VolumeUp,
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
                                            imageVector = Icons.AutoMirrored.Outlined.VolumeUp,
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

                // Action Controls: Tap to Reveal or Wrong (Swipe Left) / Right (Swipe Right)
                if (!uiState.isAnswerRevealed) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        StudyOSButton(
                            text = "Reveal Answer",
                            onClick = { viewModel.revealAnswer() },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Swipe card left if wrong (❌) • Swipe right if right (✅)",
                            style = typography.caption.copy(fontSize = 11.sp),
                            color = colors.mutedText,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Wrong (Swipe Left) Button
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(shapes.button)
                                    .background(Color(0xFFE53E3E).copy(alpha = 0.12f))
                                    .border(1.5.dp, Color(0xFFE53E3E).copy(alpha = 0.7f), shapes.button)
                                    .clickable {
                                        coroutineScope.launch {
                                            offsetX.animateTo(-1200f, tween(180))
                                            viewModel.rateWrong()
                                            offsetX.snapTo(0f)
                                        }
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = null,
                                        tint = Color(0xFFE53E3E),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = "Wrong",
                                            style = typography.secondary.copy(fontWeight = FontWeight.Bold),
                                            color = Color(0xFFE53E3E)
                                        )
                                        Text(
                                            text = "Swipe Left",
                                            style = typography.caption.copy(fontSize = 10.sp),
                                            color = Color(0xFFE53E3E).copy(alpha = 0.85f)
                                        )
                                    }
                                }
                            }

                            // Right (Swipe Right) Button
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(shapes.button)
                                    .background(Color(0xFF38A169).copy(alpha = 0.12f))
                                    .border(1.5.dp, Color(0xFF38A169).copy(alpha = 0.7f), shapes.button)
                                    .clickable {
                                        coroutineScope.launch {
                                            offsetX.animateTo(1200f, tween(180))
                                            viewModel.rateRight()
                                            offsetX.snapTo(0f)
                                        }
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = null,
                                        tint = Color(0xFF38A169),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = "Right",
                                            style = typography.secondary.copy(fontWeight = FontWeight.Bold),
                                            color = Color(0xFF38A169)
                                        )
                                        Text(
                                            text = "Swipe Right",
                                            style = typography.caption.copy(fontSize = 10.sp),
                                            color = Color(0xFF38A169).copy(alpha = 0.85f)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Swipe card left if wrong (❌) • Swipe right if right (✅)",
                            style = typography.caption.copy(fontSize = 11.sp),
                            color = colors.mutedText,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun DeckFinishedView(
    reviewedCount: Int,
    wrongCount: Int,
    rightCount: Int,
    accuracy: Int,
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
            tint = if (accuracy >= 70) Color(0xFF38A169) else colors.accent,
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
            text = "You reviewed $reviewedCount cards with $accuracy% accuracy.",
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
                ResultMetric(label = "Total Cards", count = reviewedCount.toString(), color = colors.primaryText)
                ResultMetric(label = "Right", count = rightCount.toString(), color = Color(0xFF38A169))
                ResultMetric(label = "Wrong", count = wrongCount.toString(), color = Color(0xFFE53E3E))
                ResultMetric(label = "Accuracy", count = "$accuracy%", color = colors.accent)
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
private fun ResultMetric(label: String, count: String, color: Color = Color.Unspecified) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count,
            style = typography.sectionTitle,
            color = if (color != Color.Unspecified) color else colors.primaryText
        )
        Text(
            text = label,
            style = typography.caption,
            color = colors.secondaryText
        )
    }
}
