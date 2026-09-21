package com.studyos.app.features.ai.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.studyos.app.core.ui.component.StudyOSButton
import com.studyos.app.core.ui.component.StudyOSDivider
import com.studyos.app.core.ui.component.StudyOSIconButton
import com.studyos.app.core.ui.component.StudyOSLoadingState
import com.studyos.app.domain.model.QuickAction
import com.studyos.app.features.ai.viewmodel.AiAssistantViewModel
import com.studyos.app.theme.StudyOSTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantScreen(
    viewModel: AiAssistantViewModel,
    onBack: (() -> Unit)? = null,
    onOpenChapter: ((chapterId: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    var flashcardPromptContent by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState.infoMessage) {
        uiState.infoMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearInfoMessage()
        }
    }

    // Auto-scroll to bottom when new messages arrive or when streaming
    LaunchedEffect(uiState.messages.size, uiState.isGenerating) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size)
        }
    }

    // History Sheet
    if (uiState.isHistorySheetOpen) {
        ConversationHistorySheet(
            conversations = uiState.conversations,
            currentConversationId = uiState.currentConversation?.id,
            onSelectConversation = viewModel::selectConversation,
            onNewConversation = { viewModel.startNewConversation() },
            onDeleteConversation = viewModel::deleteConversation,
            onDismissRequest = viewModel::closeHistorySheet
        )
    }

    // Settings Sheet
    if (uiState.isSettingsSheetOpen) {
        AiSettingsBottomSheet(
            currentConfig = uiState.aiConfig,
            onSaveConfig = viewModel::saveAiConfig,
            onDismissRequest = viewModel::closeSettingsSheet
        )
    }

    // Quick Flashcard Creation Dialog
    flashcardPromptContent?.let { raw ->
        val firstLine = raw.lines().firstOrNull { it.isNotBlank() }?.trim()?.removePrefix("#")?.trim() ?: "Key Concept"
        val remaining = raw.lines().drop(1).joinToString("\n").trim().ifBlank { raw }
        var questionText by remember(raw) { mutableStateOf(firstLine) }
        var answerText by remember(raw) { mutableStateOf(remaining) }

        AlertDialog(
            onDismissRequest = { flashcardPromptContent = null },
            title = { Text("Create Flashcard from AI Insight", style = typography.subsectionTitle, color = colors.primaryText) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = questionText,
                        onValueChange = { questionText = it },
                        label = { Text("Question / Concept") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = answerText,
                        onValueChange = { answerText = it },
                        label = { Text("Answer / Explanation") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 6
                    )
                }
            },
            confirmButton = {
                StudyOSButton(
                    text = "Save Flashcard",
                    enabled = questionText.isNotBlank() && answerText.isNotBlank(),
                    onClick = {
                        viewModel.saveMessageAsFlashcard(questionText, answerText)
                        flashcardPromptContent = null
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { flashcardPromptContent = null }) {
                    Text("Cancel", color = colors.secondaryText)
                }
            },
            containerColor = colors.surface
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Column(
                        modifier = Modifier
                            .clickable { viewModel.openHistorySheet() }
                            .semantics { this.role = Role.Button }
                    ) {
                        Text(
                            text = uiState.currentConversation?.title ?: "AI Assistant",
                            style = typography.subsectionTitle,
                            color = colors.primaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = if (uiState.isGenerating) "Generating response..." else "StudyOS Assistant",
                            style = typography.caption,
                            color = if (uiState.isGenerating) colors.accent else colors.mutedText
                        )
                    }
                },
                navigationIcon = {
                    if (onBack != null) {
                        StudyOSIconButton(
                            onClick = onBack,
                            contentDescription = "Back"
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = null,
                                tint = colors.secondaryText,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                actions = {
                    // History
                    StudyOSIconButton(
                        onClick = viewModel::openHistorySheet,
                        contentDescription = "Conversation history"
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.History,
                            contentDescription = null,
                            tint = colors.secondaryText,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Settings
                    StudyOSIconButton(
                        onClick = viewModel::openSettingsSheet,
                        contentDescription = "AI Settings"
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = null,
                            tint = colors.secondaryText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.background,
                    titleContentColor = colors.primaryText
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                StudyOSDivider()
                AiChatInputBar(
                    text = uiState.inputText,
                    onTextChanged = viewModel::onInputTextChanged,
                    onSendMessage = { viewModel.sendMessage() },
                    onStopGeneration = viewModel::stopGeneration,
                    onQuickActionClick = viewModel::executeQuickAction,
                    isGenerating = uiState.isGenerating,
                    modifier = Modifier.widthIn(max = 720.dp)
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 720.dp)
            ) {
            // AI Tutor Header & Mode Selector
            AiTutorHeader(
                selectedMode = uiState.selectedMode,
                onSelectMode = viewModel::setTutorMode,
                selectedDifficulty = uiState.selectedDifficulty,
                onSelectDifficulty = viewModel::setPracticeDifficulty,
                chapterAiContext = uiState.chapterAiContext,
                isContextVisible = uiState.isChapterContextVisible,
                onToggleContextVisibility = viewModel::toggleChapterContextVisibility,
                onQuickActionClick = viewModel::triggerQuickStudyAction
            )

            // Attached Context Badge (if generic subject/chapter context without full chapter details)
            if (uiState.studyContext.hasContext && uiState.chapterAiContext == null) {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    StudyContextBadge(
                        context = uiState.studyContext,
                        onOpenChapter = onOpenChapter,
                        onClearContext = viewModel::clearContext
                    )
                }
            }

            // Message Area
            if (uiState.isLoading && uiState.messages.isEmpty()) {
                StudyOSLoadingState(message = "Loading conversation...")
            } else if (uiState.messages.isEmpty()) {
                // Empty conversation state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.widthIn(max = 440.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(colors.surface, shapes.surface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Psychology,
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "StudyOS AI Assistant",
                            style = typography.subsectionTitle,
                            color = colors.primaryText
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = if (uiState.studyContext.hasContext) {
                                "Ask questions, explore concepts, or get quizzed on ${uiState.studyContext.chapterName ?: uiState.studyContext.subjectName}."
                            } else {
                                "Ask questions, simplify difficult concepts, generate flashcards, or quiz your knowledge."
                            },
                            style = typography.secondary,
                            color = colors.secondaryText,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        QuickActionChips(
                            onActionClick = viewModel::executeQuickAction,
                            enabled = !uiState.isGenerating
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.Top
                ) {
                    itemsIndexed(
                        items = uiState.messages,
                        key = { _, msg -> msg.id }
                    ) { index, message ->
                        AiMessageBubble(
                            message = message,
                            onRetry = { viewModel.retryMessage(message.id) },
                            onRegenerate = { viewModel.regenerateMessage(message.id) },
                            onOpenSettings = viewModel::openSettingsSheet,
                            onSaveAsNote = { content -> viewModel.saveMessageAsNote(content) },
                            onCreateFlashcard = { content -> flashcardPromptContent = content }
                        )

                        if (index < uiState.messages.lastIndex) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}
}
