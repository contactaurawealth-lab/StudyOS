package com.studyos.app.features.ai.ui

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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.studyos.app.domain.model.QuickAction
import com.studyos.app.theme.StudyOSTheme

@Composable
fun AiChatInputBar(
    text: String,
    onTextChanged: (String) -> Unit,
    onSendMessage: () -> Unit,
    onStopGeneration: () -> Unit,
    onQuickActionClick: (QuickAction) -> Unit,
    isGenerating: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.background)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Quick Action Chips
        QuickActionChips(
            onActionClick = onQuickActionClick,
            enabled = !isGenerating,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Input row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shapes.surface)
                .background(colors.cardBackground.copy(alpha = 0.7f))
                .border(0.5.dp, colors.border.copy(alpha = 0.3f), shapes.surface)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Multiline text input
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                if (text.isEmpty()) {
                    Text(
                        text = "Ask anything about your studies...",
                        style = typography.body,
                        color = colors.mutedText
                    )
                }

                BasicTextField(
                    value = text,
                    onValueChange = onTextChanged,
                    textStyle = typography.body.copy(color = colors.primaryText),
                    cursorBrush = SolidColor(colors.accent),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (text.isNotBlank() && !isGenerating) {
                                onSendMessage()
                            }
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Stop / Send Button
            if (isGenerating) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(shapes.statusPill)
                        .background(colors.accent)
                        .clickable { onStopGeneration() }
                        .semantics { this.role = Role.Button },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Stop,
                        contentDescription = "Stop generation",
                        tint = colors.background,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                val canSend = text.isNotBlank()
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(shapes.statusPill)
                        .background(if (canSend) colors.accent else colors.border.copy(alpha = 0.5f))
                        .clickable(enabled = canSend) { onSendMessage() }
                        .semantics { this.role = Role.Button },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Send,
                        contentDescription = "Send message",
                        tint = if (canSend) colors.background else colors.mutedText,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
