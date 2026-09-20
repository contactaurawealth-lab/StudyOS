package com.studyos.app.features.ai.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studyos.app.core.ui.component.StudyOSButton
import com.studyos.app.core.ui.component.StudyOSMarkdown
import com.studyos.app.core.ui.component.StudyOSOutlinedButton
import com.studyos.app.core.util.DateTimeUtils
import com.studyos.app.domain.model.AiMessage
import com.studyos.app.domain.model.AiMessageRole
import com.studyos.app.domain.model.AiMessageStatus
import com.studyos.app.theme.StudyOSTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AiMessageBubble(
    message: AiMessage,
    onRetry: () -> Unit,
    onRegenerate: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (message.role) {
        AiMessageRole.USER -> UserMessageBubble(message = message, modifier = modifier)
        AiMessageRole.ASSISTANT -> AssistantMessageView(
            message = message,
            onRetry = onRetry,
            onRegenerate = onRegenerate,
            onOpenSettings = onOpenSettings,
            modifier = modifier
        )
        AiMessageRole.SYSTEM -> {
            // System messages are omitted from visual chat history
        }
    }
}

@Composable
private fun UserMessageBubble(
    message: AiMessage,
    modifier: Modifier = Modifier
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.End
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(shapes.surface)
                .background(colors.surface)
                .border(1.dp, colors.border, shapes.surface)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Column {
                Text(
                    text = message.content,
                    style = typography.bodyMedium,
                    color = colors.primaryText
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = DateTimeUtils.formatTime(message.createdAt),
                    style = typography.caption.copy(fontSize = 11.sp),
                    color = colors.mutedText,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

@Composable
private fun AssistantMessageView(
    message: AiMessage,
    onRetry: () -> Unit,
    onRegenerate: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    var isCopied by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "STUDYOS AI",
                style = typography.caption,
                color = colors.accent
            )

            Text(
                text = DateTimeUtils.formatTime(message.createdAt),
                style = typography.caption.copy(fontSize = 11.sp),
                color = colors.mutedText
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Body
        when (message.status) {
            AiMessageStatus.ERROR -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shapes.surface)
                        .background(colors.surface)
                        .border(1.dp, colors.border, shapes.surface)
                        .padding(16.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.ErrorOutline,
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Unable to get response",
                                style = typography.bodyMedium,
                                color = colors.primaryText
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = message.errorMessage
                                ?: "An error occurred while connecting to the AI assistant.",
                            style = typography.secondary,
                            color = colors.secondaryText
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StudyOSButton(
                                text = "Retry",
                                onClick = onRetry
                            )

                            if (message.errorMessage?.contains("API key", ignoreCase = true) == true) {
                                StudyOSOutlinedButton(
                                    text = "Configure API Key",
                                    onClick = onOpenSettings
                                )
                            }
                        }
                    }
                }
            }

            AiMessageStatus.STREAMING -> {
                Column {
                    if (message.content.isNotBlank()) {
                        StudyOSMarkdown(content = message.content)
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    // Pulsing cursor
                    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0.2f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "cursorAlpha"
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(shapes.statusPill)
                                .background(colors.accent)
                                .alpha(alpha)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (message.content.isBlank()) "Thinking..." else "Writing...",
                            style = typography.caption,
                            color = colors.mutedText
                        )
                    }
                }
            }

            AiMessageStatus.SUCCESS, AiMessageStatus.SENT -> {
                StudyOSMarkdown(content = message.content)

                Spacer(modifier = Modifier.height(8.dp))

                // Action Bar (Copy & Regenerate)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Copy
                    Row(
                        modifier = Modifier
                            .clip(shapes.statusPill)
                            .clickable {
                                clipboardManager.setText(AnnotatedString(message.content))
                                isCopied = true
                                coroutineScope.launch {
                                    delay(2000)
                                    isCopied = false
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isCopied) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                            contentDescription = "Copy message",
                            tint = if (isCopied) colors.accent else colors.mutedText,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isCopied) "Copied" else "Copy",
                            style = typography.caption,
                            color = if (isCopied) colors.accent else colors.mutedText
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Regenerate
                    Row(
                        modifier = Modifier
                            .clip(shapes.statusPill)
                            .clickable { onRegenerate() }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Regenerate response",
                            tint = colors.mutedText,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Regenerate",
                            style = typography.caption,
                            color = colors.mutedText
                        )
                    }
                }
            }
        }
    }
}
