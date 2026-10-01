package com.studyos.app.features.timer.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

private val WarmObsidian = Color(0xFF0F1115)
private val MatteSlate = Color(0xFF161920)
private val FrostedSmoke = Color(0xFF1E222D)
private val WarmAmberLatte = Color(0xFFD4A373)
private val SoftChamomile = Color(0xFFE9D8A6)
private val MutedSage = Color(0xFFCCD5AE)

@Composable
fun CoffeeTimeOverlay(
    visible: Boolean,
    onStartBreak: (durationMinutes: Int) -> Unit,
    onFinishForNow: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "CoffeeBreathingGlow")
        val pulseScale by infiniteTransition.animateFloat(
            initialValue = 0.94f,
            targetValue = 1.06f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 4000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseScale"
        )
        val pulseAlpha by infiniteTransition.animateFloat(
            initialValue = 0.25f,
            targetValue = 0.50f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 4000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseAlpha"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f)),
            contentAlignment = Alignment.Center
        ) {
            // Ambient warm amber haze orb in background
            Box(
                modifier = Modifier
                    .size(320.dp)
                    .scale(pulseScale)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                WarmAmberLatte.copy(alpha = pulseAlpha),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )

            // Calm Sanctuary Card
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(28.dp))
                    .background(MatteSlate.copy(alpha = 0.95f))
                    .border(1.dp, WarmAmberLatte.copy(alpha = 0.25f), RoundedCornerShape(28.dp))
                    .padding(horizontal = 28.dp, vertical = 34.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Steaming Mug Iconography with Breathing Ring
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .scale(pulseScale)
                        .background(FrostedSmoke, CircleShape)
                        .border(1.5.dp, WarmAmberLatte.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "☕",
                        fontSize = 38.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Coffee Time",
                    color = SoftChamomile,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Step away from the screen. Pour a warm cup, stretch your shoulders, and let your mind naturally consolidate what you just learned.",
                    color = Color(0xFF9BA3AF),
                    fontSize = 14.5.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Restorative Action Presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BreakOptionButton(
                        icon = "☕",
                        title = "Brew Coffee",
                        time = "5 min",
                        modifier = Modifier.weight(1f),
                        onClick = { onStartBreak(5) }
                    )
                    BreakOptionButton(
                        icon = "🚶",
                        title = "Gentle Walk",
                        time = "15 min",
                        modifier = Modifier.weight(1f),
                        onClick = { onStartBreak(15) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Honorable effort dismiss button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(WarmObsidian)
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                        .clickable { onFinishForNow() }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Honorable effort. That’s enough for now.",
                        color = MutedSage,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun BreakOptionButton(
    icon: String,
    title: String,
    time: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(FrostedSmoke)
            .border(1.dp, WarmAmberLatte.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = icon, fontSize = 22.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            color = Color(0xFFF0F2F5),
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = time,
            color = WarmAmberLatte,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
