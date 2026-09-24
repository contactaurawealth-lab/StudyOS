package com.studyos.app.features.ai.ui

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studyos.app.core.ui.component.GlassCard
import com.studyos.app.core.ui.component.GlassIconButton
import com.studyos.app.core.ui.component.StudyOSButton
import com.studyos.app.core.ui.component.StudyOSDivider
import com.studyos.app.core.ui.component.StudyOSOutlinedButton
import com.studyos.app.core.ui.component.StudyOSTextField
import com.studyos.app.domain.model.AiConfig
import com.studyos.app.domain.model.DefaultAiModels
import com.studyos.app.domain.model.NamedApiKey
import com.studyos.app.theme.StudyOSTheme
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSettingsBottomSheet(
    currentConfig: AiConfig,
    onSaveConfig: (AiConfig) -> Unit,
    onDismissRequest: () -> Unit
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var apiKey by remember(currentConfig) { mutableStateOf(currentConfig.apiKey) }
    var baseUrl by remember(currentConfig) { mutableStateOf(currentConfig.baseUrl) }
    var model by remember(currentConfig) { mutableStateOf(currentConfig.model) }
    var customPrompt by remember(currentConfig) { mutableStateOf(currentConfig.customSystemPrompt ?: "") }
    var savedKeys by remember(currentConfig) { mutableStateOf(currentConfig.savedApiKeys) }
    var savedModels by remember(currentConfig) { mutableStateOf(currentConfig.savedModels) }

    var isApiKeyVisible by remember { mutableStateOf(false) }
    var showAddKeySection by remember { mutableStateOf(false) }
    var newKeyName by remember { mutableStateOf("") }
    var newKeyValue by remember { mutableStateOf("") }
    var isNewKeyVisible by remember { mutableStateOf(false) }

    var showAddModelSection by remember { mutableStateOf(false) }
    var newModelInput by remember { mutableStateOf("") }

    val providers = listOf(
        "OpenAI" to "https://api.openai.com/v1/",
        "OpenRouter" to "https://openrouter.ai/api/v1/",
        "Groq" to "https://api.groq.com/openai/v1/",
        "DeepSeek" to "https://api.deepseek.com/v1/",
        "Ollama (Local)" to "http://localhost:11434/v1/"
    )

    val allModels = remember(savedModels) {
        (DefaultAiModels + savedModels).distinct()
    }

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
                text = "Manage multiple API keys and model IDs. Switch providers instantly. Credentials are stored securely and strictly on-device.",
                style = typography.caption,
                color = colors.secondaryText
            )

            Spacer(modifier = Modifier.height(18.dp))
            StudyOSDivider()
            Spacer(modifier = Modifier.height(18.dp))

            // ==========================================
            // 1. MULTIPLE API KEYS
            // ==========================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Saved API Keys (${savedKeys.size})",
                    style = typography.caption.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.primaryText
                )

                StudyOSOutlinedButton(
                    text = if (showAddKeySection) "Cancel" else "+ Add Key",
                    onClick = { showAddKeySection = !showAddKeySection }
                )
            }

            // Inline Add Key Form
            if (showAddKeySection) {
                Spacer(modifier = Modifier.height(10.dp))
                GlassCard(
                    backgroundColor = colors.cardBackground,
                    padding = 12.dp
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Add New Key",
                            style = typography.caption.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.primaryText
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        StudyOSTextField(
                            value = newKeyName,
                            onValueChange = { newKeyName = it },
                            placeholder = "Key Name (e.g., Groq Fast, OpenAI Work)",
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        StudyOSTextField(
                            value = newKeyValue,
                            onValueChange = { newKeyValue = it },
                            placeholder = "sk-...",
                            visualTransformation = if (isNewKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                Icon(
                                    imageVector = if (isNewKeyVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    contentDescription = null,
                                    tint = colors.mutedText,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clickable { isNewKeyVisible = !isNewKeyVisible }
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            StudyOSButton(
                                text = "Save & Use Key",
                                onClick = {
                                    if (newKeyValue.isNotBlank()) {
                                        val newKey = NamedApiKey(
                                            id = UUID.randomUUID().toString(),
                                            name = newKeyName.trim().ifBlank { "API Key ${savedKeys.size + 1}" },
                                            key = newKeyValue.trim()
                                        )
                                        savedKeys = savedKeys + newKey
                                        apiKey = newKeyValue.trim()
                                        newKeyName = ""
                                        newKeyValue = ""
                                        showAddKeySection = false
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Saved Keys List
            if (savedKeys.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    savedKeys.forEach { savedKey ->
                        val isActive = apiKey == savedKey.key
                        val maskedKey = if (savedKey.key.length > 8) {
                            "${savedKey.key.take(4)}••••${savedKey.key.takeLast(4)}"
                        } else "••••••••"

                        GlassCard(
                            onClick = { apiKey = savedKey.key },
                            backgroundColor = if (isActive) colors.glassSurface else colors.cardBackground,
                            borderColor = if (isActive) colors.accent else colors.glassBorder,
                            padding = 10.dp
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .border(
                                                width = if (isActive) 2.dp else 1.dp,
                                                color = if (isActive) colors.accent else colors.mutedText,
                                                shape = CircleShape
                                            )
                                            .background(if (isActive) colors.accent else Color.Transparent),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isActive) {
                                            Icon(
                                                imageVector = Icons.Outlined.Check,
                                                contentDescription = null,
                                                tint = colors.background,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column {
                                        Text(
                                            text = savedKey.name,
                                            style = typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = colors.primaryText
                                        )
                                        Text(
                                            text = maskedKey,
                                            style = typography.caption.copy(fontSize = 11.sp),
                                            color = colors.mutedText
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isActive) {
                                        Box(
                                            modifier = Modifier
                                                .clip(shapes.small)
                                                .background(colors.accent.copy(alpha = 0.15f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "ACTIVE",
                                                style = typography.caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                                color = colors.accent
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }

                                    GlassIconButton(
                                        onClick = {
                                            savedKeys = savedKeys.filter { it.id != savedKey.id }
                                            if (apiKey == savedKey.key) {
                                                apiKey = savedKeys.firstOrNull()?.key ?: ""
                                            }
                                        },
                                        contentDescription = "Delete key",
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.DeleteOutline,
                                            contentDescription = null,
                                            tint = colors.mutedText,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Direct Active API Key input
            Text(
                text = "Active API Key",
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

            Spacer(modifier = Modifier.height(20.dp))
            StudyOSDivider()
            Spacer(modifier = Modifier.height(20.dp))

            // ==========================================
            // 2. MULTIPLE MODEL IDS
            // ==========================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Model ID Selection",
                    style = typography.caption.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.primaryText
                )

                StudyOSOutlinedButton(
                    text = if (showAddModelSection) "Cancel" else "+ Custom Model",
                    onClick = { showAddModelSection = !showAddModelSection }
                )
            }

            // Inline Add Custom Model Form
            if (showAddModelSection) {
                Spacer(modifier = Modifier.height(10.dp))
                GlassCard(
                    backgroundColor = colors.cardBackground,
                    padding = 12.dp
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Add Custom Model ID",
                            style = typography.caption.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.primaryText
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Enter any model supported by your provider (e.g., deepseek-ai/DeepSeek-V3, meta-llama/llama-3.3-70b-instruct).",
                            style = typography.caption.copy(fontSize = 11.sp),
                            color = colors.secondaryText
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                StudyOSTextField(
                                    value = newModelInput,
                                    onValueChange = { newModelInput = it },
                                    placeholder = "e.g., mistralai/mixtral-8x7b-instruct",
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            StudyOSButton(
                                text = "Add",
                                onClick = {
                                    val trimmed = newModelInput.trim()
                                    if (trimmed.isNotBlank()) {
                                        if (!savedModels.contains(trimmed)) {
                                            savedModels = savedModels + trimmed
                                        }
                                        model = trimmed
                                        newModelInput = ""
                                        showAddModelSection = false
                                    }
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Model Selection Chips (Horizontal Scroll)
            Text(
                text = "Preset & Saved Models (tap to select)",
                style = typography.caption,
                color = colors.secondaryText
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                allModels.forEach { modelOption ->
                    val isSelected = model == modelOption
                    val isCustom = savedModels.contains(modelOption) && !DefaultAiModels.contains(modelOption)

                    Box(
                        modifier = Modifier
                            .clip(shapes.small)
                            .background(if (isSelected) colors.accent.copy(alpha = 0.18f) else colors.cardBackground)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) colors.accent else colors.glassBorder,
                                shape = shapes.small
                            )
                            .clickable { model = modelOption }
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = null,
                                    tint = colors.accent,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                            Text(
                                text = modelOption,
                                style = typography.caption.copy(
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    fontSize = 12.sp
                                ),
                                color = if (isSelected) colors.accent else colors.primaryText,
                                softWrap = false
                            )

                            if (isCustom) {
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Remove custom model",
                                    tint = colors.mutedText,
                                    modifier = Modifier
                                        .size(13.dp)
                                        .clickable {
                                            savedModels = savedModels.filter { it != modelOption }
                                            if (model == modelOption) {
                                                model = DefaultAiModels.first()
                                            }
                                        }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Direct Model ID input
            Text(
                text = "Active Model ID",
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

            Spacer(modifier = Modifier.height(20.dp))
            StudyOSDivider()
            Spacer(modifier = Modifier.height(20.dp))

            // ==========================================
            // 3. BASE URL & PROVIDER PRESETS
            // ==========================================
            Text(
                text = "API Endpoint (Base URL)",
                style = typography.caption.copy(fontWeight = FontWeight.SemiBold),
                color = colors.primaryText
            )
            Spacer(modifier = Modifier.height(6.dp))

            // Quick Provider Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                providers.forEach { (provName, provUrl) ->
                    val isProvSelected = baseUrl.trim().removeSuffix("/") == provUrl.trim().removeSuffix("/")
                    Box(
                        modifier = Modifier
                            .clip(shapes.small)
                            .background(if (isProvSelected) colors.accent.copy(alpha = 0.15f) else colors.cardBackground)
                            .border(
                                width = 1.dp,
                                color = if (isProvSelected) colors.accent else colors.glassBorder,
                                shape = shapes.small
                            )
                            .clickable { baseUrl = provUrl }
                            .padding(horizontal = 9.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = provName,
                            style = typography.caption.copy(
                                fontSize = 11.sp,
                                fontWeight = if (isProvSelected) FontWeight.SemiBold else FontWeight.Normal
                            ),
                            color = if (isProvSelected) colors.accent else colors.secondaryText,
                            softWrap = false
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            StudyOSTextField(
                value = baseUrl,
                onValueChange = { baseUrl = it },
                placeholder = "https://api.openai.com/v1/",
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(18.dp))

            // ==========================================
            // 4. CUSTOM TUTOR INSTRUCTIONS
            // ==========================================
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
                singleLine = false,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(28.dp))

            // ==========================================
            // 5. ACTIONS
            // ==========================================
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
                        // Ensure the active key is in saved keys if nameable
                        val updatedSavedKeys = if (apiKey.isNotBlank() && savedKeys.none { it.key == apiKey.trim() }) {
                            savedKeys + NamedApiKey(
                                name = "Key ${savedKeys.size + 1}",
                                key = apiKey.trim()
                            )
                        } else {
                            savedKeys
                        }

                        // Ensure active model is in saved models if custom
                        val updatedSavedModels = if (model.isNotBlank() && !DefaultAiModels.contains(model.trim()) && !savedModels.contains(model.trim())) {
                            savedModels + model.trim()
                        } else {
                            savedModels
                        }

                        onSaveConfig(
                            AiConfig(
                                apiKey = apiKey.trim(),
                                baseUrl = baseUrl.trim().ifBlank { "https://api.openai.com/v1/" },
                                model = model.trim().ifBlank { "gpt-4o-mini" },
                                customSystemPrompt = customPrompt.trim().ifBlank { null },
                                savedApiKeys = updatedSavedKeys,
                                savedModels = updatedSavedModels
                            )
                        )
                        onDismissRequest()
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
