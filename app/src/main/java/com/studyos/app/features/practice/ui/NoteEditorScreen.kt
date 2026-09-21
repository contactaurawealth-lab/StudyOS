package com.studyos.app.features.practice.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Title
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studyos.app.core.ui.component.StudyOSButton
import com.studyos.app.core.ui.component.StudyOSIconButton
import com.studyos.app.core.ui.component.StudyOSLoadingState
import com.studyos.app.core.ui.component.StudyOSMarkdown
import com.studyos.app.core.ui.component.StudyOSOutlinedButton
import com.studyos.app.features.practice.viewmodel.NoteEditorViewModel
import com.studyos.app.theme.StudyOSTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    viewModel: NoteEditorViewModel,
    onBack: () -> Unit,
    onOpenQuiz: (quizId: String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var isPreviewMode by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) {
            onBack()
        }
    }

    LaunchedEffect(uiState.infoMessage) {
        uiState.infoMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearInfoMessage()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        if (uiState.isLoading) {
            StudyOSLoadingState(message = "Loading note...")
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                // Top App Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
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

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = if (uiState.noteId == null) "New Note" else "Edit Note",
                            style = typography.secondary,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.primaryText
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Native Share Action
                        StudyOSIconButton(
                            onClick = {
                                val shareText = "${uiState.title.ifBlank { "Untitled Note" }}\n\n${uiState.content}"
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, uiState.title)
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share Note"))
                            },
                            contentDescription = "Share Note"
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Share,
                                contentDescription = null,
                                tint = colors.secondaryText,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        StudyOSIconButton(
                            onClick = { viewModel.togglePin() },
                            contentDescription = "Toggle Pin"
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PushPin,
                                contentDescription = null,
                                tint = if (uiState.isPinned) colors.accent else colors.secondaryText,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        if (uiState.noteId != null) {
                            StudyOSIconButton(
                                onClick = { viewModel.deleteNote() },
                                contentDescription = "Delete Note"
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteOutline,
                                    contentDescription = null,
                                    tint = colors.secondaryText,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        StudyOSButton(
                            text = "Save",
                            onClick = { viewModel.saveNote() }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Title Input & Edit/Preview Switcher Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = uiState.title,
                        onValueChange = viewModel::onTitleChange,
                        placeholder = {
                            Text(
                                text = "Note Title...",
                                style = typography.screenTitle.copy(color = colors.mutedText)
                            )
                        },
                        textStyle = typography.screenTitle.copy(color = colors.primaryText),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Edit / Preview Pill Toggle
                    Row(
                        modifier = Modifier
                            .clip(shapes.button)
                            .background(colors.cardBackground)
                            .border(1.dp, colors.border, shapes.button)
                            .padding(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (!isPreviewMode) colors.accent else Color.Transparent)
                                .clickable { isPreviewMode = false }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Edit",
                                style = typography.caption.copy(fontSize = 11.sp),
                                fontWeight = if (!isPreviewMode) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (!isPreviewMode) colors.background else colors.secondaryText
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isPreviewMode) colors.accent else Color.Transparent)
                                .clickable { isPreviewMode = true }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Preview",
                                style = typography.caption.copy(fontSize = 11.sp),
                                fontWeight = if (isPreviewMode) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isPreviewMode) colors.background else colors.secondaryText
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Formatting & AI Toolbar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shapes.button)
                        .background(colors.surface)
                        .border(1.dp, colors.border, shapes.button)
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ToolbarButton(icon = Icons.Outlined.FormatBold, label = "Bold") {
                        viewModel.insertMarkdown("**", "**")
                    }
                    ToolbarButton(icon = Icons.Outlined.FormatItalic, label = "Italic") {
                        viewModel.insertMarkdown("*", "*")
                    }
                    ToolbarButton(icon = Icons.Outlined.Title, label = "H1") {
                        viewModel.insertMarkdown("# ")
                    }
                    ToolbarButton(icon = Icons.Outlined.FormatListBulleted, label = "List") {
                        viewModel.insertMarkdown("- ")
                    }
                    ToolbarButton(icon = Icons.Outlined.FormatListNumbered, label = "Num") {
                        viewModel.insertMarkdown("1. ")
                    }
                    ToolbarTextButton(label = "[ ]") {
                        viewModel.insertMarkdown("- [ ] ")
                    }
                    ToolbarTextButton(label = "fx") {
                        viewModel.insertMarkdown("$$", "$$")
                    }
                    ToolbarTextButton(label = "</>") {
                        viewModel.insertMarkdown("```\n", "\n```")
                    }
                    ToolbarTextButton(label = ">") {
                        viewModel.insertMarkdown("> ")
                    }

                    Box(
                        modifier = Modifier
                            .height(20.dp)
                            .width(1.dp)
                            .background(colors.border)
                            .padding(horizontal = 4.dp)
                    )

                    // AI Quick Tools
                    AiActionButton(text = "Summarize", isWorking = uiState.isAiWorking) {
                        viewModel.summarizeNote()
                    }
                    AiActionButton(text = "Explain", isWorking = uiState.isAiWorking) {
                        viewModel.explainNote()
                    }
                    AiActionButton(text = "To Flashcards", isWorking = uiState.isAiWorking) {
                        viewModel.convertToFlashcards()
                    }
                    AiActionButton(text = "Generate Quiz", isWorking = uiState.isAiWorking) {
                        viewModel.generateQuizFromNote()
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Content Markdown Editor / Live Preview Area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(shapes.surface)
                        .background(colors.surface)
                        .border(1.dp, colors.border, shapes.surface)
                        .padding(12.dp)
                ) {
                    if (isPreviewMode) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            if (uiState.content.isBlank()) {
                                Text(
                                    text = "Nothing to preview yet. Switch to Edit mode to begin typing notes.",
                                    style = typography.body,
                                    color = colors.mutedText
                                )
                            } else {
                                StudyOSMarkdown(content = uiState.content)
                            }
                        }
                    } else {
                        TextField(
                            value = uiState.content,
                            onValueChange = viewModel::onContentChange,
                            placeholder = {
                                Text(
                                    text = "Start writing notes in markdown...\n\nUse # for headings\n- for bullet points\n- [ ] for checklists\n$$ for math equations\n``` for code blocks\n**bold** for emphasis",
                                    style = typography.body.copy(color = colors.mutedText)
                                )
                            },
                            textStyle = typography.body.copy(color = colors.primaryText),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Word count and Reading time status bar
                val wordCount = remember(uiState.content) {
                    if (uiState.content.isBlank()) 0
                    else uiState.content.trim().split("\\s+".toRegex()).count { it.isNotEmpty() }
                }
                val charCount = uiState.content.length
                val readingTimeMinutes = kotlin.math.max(1, (wordCount + 199) / 200)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, start = 4.dp, end = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$wordCount words • $charCount characters • ~$readingTimeMinutes min read",
                        style = typography.caption.copy(fontSize = 11.sp),
                        color = colors.mutedText
                    )
                    Text(
                        text = if (isPreviewMode) "Live Markdown Preview" else "Markdown Editor",
                        style = typography.caption.copy(fontSize = 10.sp),
                        color = colors.secondaryText
                    )
                }
            }
        }

        // AI Result Bottom Sheet
        if (uiState.aiResultContent != null || uiState.isAiWorking) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { viewModel.dismissAiResult() },
                sheetState = sheetState,
                containerColor = colors.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Psychology,
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = uiState.aiResultTitle ?: "AI Study Assistance",
                                style = typography.secondary,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.primaryText
                            )
                        }

                        if (uiState.isAiWorking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = colors.accent,
                                strokeWidth = 2.dp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shapes.card)
                            .background(colors.cardBackground)
                            .border(1.dp, colors.border, shapes.card)
                            .padding(16.dp)
                    ) {
                        if (!uiState.aiResultContent.isNullOrBlank()) {
                            StudyOSMarkdown(content = uiState.aiResultContent!!)
                        } else if (uiState.isAiWorking) {
                            Text(
                                text = "Analyzing note concepts...",
                                style = typography.caption,
                                color = colors.secondaryText
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    StudyOSButton(
                        text = "Done",
                        onClick = { viewModel.dismissAiResult() },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // Quiz Generated Prompt Dialog / Toast action
        if (uiState.generatedQuizId != null) {
            val generatedId = uiState.generatedQuizId!!
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(20.dp)
                    .clip(shapes.surface)
                    .background(colors.surface)
                    .border(1.dp, colors.accent, shapes.surface)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Quiz ready to play!",
                            style = typography.secondary,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.primaryText
                        )
                        Text(
                            text = "Created from your study note",
                            style = typography.caption,
                            color = colors.secondaryText
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    StudyOSButton(
                        text = "Start Quiz",
                        onClick = {
                            viewModel.dismissAiResult()
                            onOpenQuiz(generatedId)
                        }
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun ToolbarButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val colors = StudyOSTheme.colors
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = colors.primaryText,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun ToolbarTextButton(
    label: String,
    onClick: () -> Unit
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = typography.caption.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
            color = colors.primaryText
        )
    }
}

@Composable
private fun AiActionButton(
    text: String,
    isWorking: Boolean,
    onClick: () -> Unit
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography

    Box(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(colors.cardBackground)
            .border(1.dp, colors.border, RoundedCornerShape(6.dp))
            .clickable(enabled = !isWorking, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = typography.caption.copy(fontSize = 11.sp),
            fontWeight = FontWeight.Medium,
            color = colors.accent
        )
    }
}
