package com.studyos.app.features.ai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.studyos.app.core.ui.component.StudyOSButton
import com.studyos.app.core.ui.component.StudyOSDivider
import com.studyos.app.core.ui.component.StudyOSOutlinedButton
import com.studyos.app.core.ui.component.StudyOSTextField
import com.studyos.app.domain.model.AiConfig
import com.studyos.app.theme.StudyOSTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSettingsBottomSheet(
    currentConfig: AiConfig,
    onSaveConfig: (AiConfig) -> Unit,
    onDismissRequest: () -> Unit
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var apiKey by remember(currentConfig) { mutableStateOf(currentConfig.apiKey) }
    var baseUrl by remember(currentConfig) { mutableStateOf(currentConfig.baseUrl) }
    var model by remember(currentConfig) { mutableStateOf(currentConfig.model) }
    var customPrompt by remember(currentConfig) { mutableStateOf(currentConfig.customSystemPrompt ?: "") }
    var isApiKeyVisible by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = colors.surface,
        contentColor = colors.primaryText,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AI Configuration",
                    style = typography.sectionTitle,
                    color = colors.primaryText
                )

                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Close",
                    tint = colors.secondaryText,
                    modifier = Modifier
                        .size(22.dp)
                        .clickable { onDismissRequest() }
                        .semantics { this.role = Role.Button }
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Configure your preferred OpenAI-compatible provider (OpenAI, OpenRouter, Groq, Ollama, etc.). Credentials are stored strictly on your device.",
                style = typography.caption,
                color = colors.secondaryText
            )

            Spacer(modifier = Modifier.height(20.dp))
            StudyOSDivider()
            Spacer(modifier = Modifier.height(20.dp))

            // API Key
            Text(
                text = "API Key",
                style = typography.caption,
                color = colors.secondaryText
            )
            Spacer(modifier = Modifier.height(4.dp))
            StudyOSTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                placeholder = "sk-...",
                visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    Icon(
                        imageVector = if (isApiKeyVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (isApiKeyVisible) "Hide key" else "Show key",
                        tint = colors.mutedText,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { isApiKeyVisible = !isApiKeyVisible }
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Base URL
            Text(
                text = "API Endpoint (Base URL)",
                style = typography.caption,
                color = colors.secondaryText
            )
            Spacer(modifier = Modifier.height(4.dp))
            StudyOSTextField(
                value = baseUrl,
                onValueChange = { baseUrl = it },
                placeholder = "https://api.openai.com/v1/",
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Model
            Text(
                text = "Model",
                style = typography.caption,
                color = colors.secondaryText
            )
            Spacer(modifier = Modifier.height(4.dp))
            StudyOSTextField(
                value = model,
                onValueChange = { model = it },
                placeholder = "gpt-4o-mini",
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Custom Instructions
            Text(
                text = "Custom Tutor Instructions (Optional)",
                style = typography.caption,
                color = colors.secondaryText
            )
            Spacer(modifier = Modifier.height(4.dp))
            StudyOSTextField(
                value = customPrompt,
                onValueChange = { customPrompt = it },
                placeholder = "e.g., Focus on AP Calculus BC prep, use Socratic questioning...",
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StudyOSOutlinedButton(
                    text = "Cancel",
                    onClick = onDismissRequest
                )

                Spacer(modifier = Modifier.width(12.dp))

                StudyOSButton(
                    text = "Save configuration",
                    onClick = {
                        onSaveConfig(
                            AiConfig(
                                apiKey = apiKey.trim(),
                                baseUrl = baseUrl.trim().ifBlank { "https://api.openai.com/v1/" },
                                model = model.trim().ifBlank { "gpt-4o-mini" },
                                customSystemPrompt = customPrompt.trim().ifBlank { null }
                            )
                        )
                        onDismissRequest()
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
