package com.studyos.app.features.assistant.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ScreenShare
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studyos.app.core.assistant.AssistantCustomization
import com.studyos.app.core.assistant.AssistantPersona
import com.studyos.app.core.assistant.AssistantThemeGlow
import com.studyos.app.core.assistant.StudyOSAssistantManager
import com.studyos.app.core.ui.component.GlassCard
import com.studyos.app.core.ui.component.StudyOSButton
import com.studyos.app.core.ui.component.StudyOSDivider
import com.studyos.app.core.ui.component.StudyOSIconButton
import com.studyos.app.core.ui.component.StudyOSOutlinedButton
import com.studyos.app.features.assistant.AssistantOverlayViewModel
import com.studyos.app.theme.StudyOSTheme

@Composable
fun AssistantOverlaySheet(
    viewModel: AssistantOverlayViewModel,
    onDismiss: () -> Unit,
    onOpenFullChat: (conversationId: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val theme = state.customization.themeGlow
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val context = LocalContext.current

    // Pulsing gradient glow animation around the sheet
    val infiniteTransition = rememberInfiniteTransition(label = "GeminiGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowAlpha"
    )

    val micPulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "MicPulse"
    )

    val glowBrush = Brush.linearGradient(
        colors = listOf(
            theme.startColor.copy(alpha = glowAlpha),
            theme.accentColor.copy(alpha = glowAlpha),
            theme.endColor.copy(alpha = glowAlpha)
        )
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .imePadding()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(Color(0xF012131A))
            .border(
                width = 1.5.dp,
                brush = glowBrush,
                shape = RoundedCornerShape(26.dp)
            )
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Drag Handle Indicator
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(38.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.25f))
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Header Row: AI Sparkle, Title, Persona Chip, Settings, Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Brush.radialGradient(listOf(theme.accentColor.copy(alpha = 0.35f), Color.Transparent))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            tint = theme.accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Text(
                            text = "StudyOS Assistant",
                            style = typography.body.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
                            color = Color.White
                        )
                        Text(
                            text = state.customization.persona.title,
                            style = typography.caption.copy(fontSize = 11.sp),
                            color = theme.accentColor
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    StudyOSIconButton(
                        onClick = { viewModel.toggleCustomizationSheet(!state.showCustomizationSheet) },
                        contentDescription = "Customization"
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = null,
                            tint = if (state.showCustomizationSheet) theme.accentColor else Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    StudyOSIconButton(
                        onClick = onDismiss,
                        contentDescription = "Close"
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }

            // Customization In-Sheet View
            AnimatedVisibility(
                visible = state.showCustomizationSheet,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                AssistantCustomizationPanel(
                    customization = state.customization,
                    onUpdateCustomization = viewModel::updateCustomization,
                    onOpenSystemSettings = { StudyOSAssistantManager.openAssistantSettings(context) }
                )
            }

            if (!state.showCustomizationSheet) {
                Spacer(modifier = Modifier.height(10.dp))

                // Screen Context Pill Chip (Visible only if screen context text exists)
                if (state.customization.autoCaptureScreen && !state.screenContextText.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(theme.startColor.copy(alpha = 0.12f))
                            .border(1.dp, theme.startColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .clickable { viewModel.askAboutScreen() }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ScreenShare,
                                contentDescription = null,
                                tint = theme.startColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Ask about this screen (${state.screenContextText?.take(35)}...)",
                                style = typography.caption.copy(fontWeight = FontWeight.Medium),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Quick Suggestion Chips Row
                if (state.responseText.isBlank() && !state.isGenerating) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickAssistantChip("💡 Explain concept", theme.accentColor) {
                            viewModel.submitQuery("Explain the core concept in simple terms with an example.")
                        }
                        QuickAssistantChip("📝 Summarize", theme.accentColor) {
                            viewModel.submitQuery("Summarize key study points in 3 clear bullet points.")
                        }
                        QuickAssistantChip("🎯 Quiz me", theme.accentColor) {
                            viewModel.submitQuery("Quiz me on this topic with 1 multiple choice question.")
                        }
                        QuickAssistantChip("📐 Key Formulas", theme.accentColor) {
                            viewModel.submitQuery("List the essential formulas and definitions.")
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Interactive Content Area (Response or Greeting)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (state.responseText.isNotBlank() || state.isGenerating) {
                        // User Query Header
                        if (state.queryText.isNotBlank()) {
                            Text(
                                text = "Q: ${state.queryText}",
                                style = typography.caption.copy(fontWeight = FontWeight.SemiBold),
                                color = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }

                        // Response Card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = state.responseText.ifBlank { "Thinking..." },
                                    style = typography.body.copy(fontSize = 13.5.sp, lineHeight = 20.sp),
                                    color = Color.White
                                )

                                if (state.isGenerating) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            color = theme.accentColor,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Streaming answer...",
                                            style = typography.caption.copy(fontSize = 11.sp),
                                            color = Color.White.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                        }

                        // Post-Response Action Row
                        if (state.responseText.isNotBlank() && !state.isGenerating) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                StudyOSOutlinedButton(
                                    text = if (state.isSavedAsNote) "✓ Saved Note" else "Save Note",
                                    onClick = { viewModel.saveCurrentResponseAsNote() },
                                    modifier = Modifier.weight(1f)
                                )
                                StudyOSOutlinedButton(
                                    text = if (state.isSavedAsFlashcard) "✓ Card Added" else "Flashcard",
                                    onClick = { viewModel.saveCurrentResponseAsFlashcard() },
                                    modifier = Modifier.weight(1f)
                                )
                                StudyOSOutlinedButton(
                                    text = "Full Chat",
                                    onClick = { onOpenFullChat(state.currentConversationId) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    } else {
                        // Empty State / Welcome
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Speak or ask any study question...",
                                style = typography.body.copy(fontSize = 13.sp),
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Input Bar with Voice Mic & Send
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White.copy(alpha = 0.07f))
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pulsing Microphone Button
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .scale(if (state.isListening) micPulseScale else 1f)
                            .clip(CircleShape)
                            .background(
                                if (state.isListening) theme.startColor.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.1f)
                            )
                            .clickable { viewModel.startListening() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (state.isListening) Icons.Outlined.GraphicEq else Icons.Outlined.Mic,
                            contentDescription = "Voice Input",
                            tint = if (state.isListening) Color.White else theme.accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedTextField(
                        value = state.queryText,
                        onValueChange = viewModel::onQueryChanged,
                        placeholder = {
                            Text(
                                text = if (state.isListening) "Listening..." else "Ask StudyOS...",
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 13.sp
                            )
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    // Send Button
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (state.queryText.isNotBlank()) theme.accentColor else Color.Transparent
                            )
                            .clickable(enabled = state.queryText.isNotBlank() && !state.isGenerating) {
                                viewModel.submitQuery()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Send,
                            contentDescription = "Send",
                            tint = if (state.queryText.isNotBlank()) Color.Black else Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickAssistantChip(
    text: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun AssistantCustomizationPanel(
    customization: AssistantCustomization,
    onUpdateCustomization: (AssistantCustomization) -> Unit,
    onOpenSystemSettings: () -> Unit
) {
    val typography = StudyOSTheme.typography

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = "Assistant Customization",
            style = typography.subsectionTitle.copy(fontSize = 14.sp),
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Theme Glow Selector
        Text(
            text = "GLOW ACCENT THEME",
            style = typography.caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
            color = Color.White.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AssistantThemeGlow.entries.forEach { glow ->
                val isSelected = customization.themeGlow == glow
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) glow.accentColor.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.05f))
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) glow.accentColor else Color.White.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onUpdateCustomization(customization.copy(themeGlow = glow)) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(glow.startColor, glow.endColor)))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = glow.title,
                            style = typography.caption.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Persona Selector
        Text(
            text = "AI TUTOR PERSONA",
            style = typography.caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
            color = Color.White.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AssistantPersona.entries.forEach { persona ->
                val isSelected = customization.persona == persona
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) Color.White.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f))
                        .border(
                            width = if (isSelected) 1.dp else 0.5.dp,
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onUpdateCustomization(customization.copy(persona = persona)) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = persona.title,
                        style = typography.caption,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Toggles: Auto-Capture Screen & Haptics
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Screen Context Awareness",
                style = typography.caption,
                color = Color.White
            )
            Switch(
                checked = customization.autoCaptureScreen,
                onCheckedChange = { onUpdateCustomization(customization.copy(autoCaptureScreen = it)) },
                colors = SwitchDefaults.colors(checkedThumbColor = Color.White)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Haptic Vibration Feedback",
                style = typography.caption,
                color = Color.White
            )
            Switch(
                checked = customization.hapticFeedback,
                onCheckedChange = { onUpdateCustomization(customization.copy(hapticFeedback = it)) },
                colors = SwitchDefaults.colors(checkedThumbColor = Color.White)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        StudyOSButton(
            text = "Configure Android Assistant Settings",
            onClick = onOpenSystemSettings,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
