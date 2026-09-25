package com.studyos.app.features.practice.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Title
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studyos.app.core.ui.component.StudyOSButton
import com.studyos.app.core.ui.component.StudyOSIconButton
import com.studyos.app.core.ui.component.StudyOSLoadingState
import com.studyos.app.core.ui.component.StudyOSMarkdown
import com.studyos.app.core.ui.component.StudyOSOutlinedButton
import com.studyos.app.core.util.DocumentTextExtractor
import com.studyos.app.features.practice.viewmodel.NoteEditorViewModel
import com.studyos.app.theme.StudyOSTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    viewModel: NoteEditorViewModel,
    onBack: () -> Unit,
    onOpenQuiz: (quizId: String) -> Unit = {},
    onOpenDocumentViewer: (noteId: String?, title: String?) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var isPreviewMode by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // File picker launcher for importing documents directly into note (PDF, DOCX, TXT, MD)
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val extracted = DocumentTextExtractor.extract(context, uri)
                    if (extracted.content.isNotBlank()) {
                        if (uiState.title.isBlank()) {
                            viewModel.onTitleChange(extracted.title)
                        }
                        val newContent = if (uiState.content.isBlank()) {
                            extracted.content
                        } else {
                            "${uiState.content}\n\n${extracted.content}"
                        }
                        viewModel.onContentChange(newContent)
                        snackbarHostState.showSnackbar("Imported ${extracted.title} (${extracted.wordCount} words, ${extracted.fileType})")
                    } else {
                        snackbarHostState.showSnackbar("Could not extract readable text from document.")
                    }
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Failed to import document: ${e.message}")
                }
            }
        }
    }

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

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Text(
                    text = "Delete Note",
                    style = typography.sectionTitle,
                    color = colors.primaryText
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete this note? This action cannot be undone.",
                    style = typography.body,
                    color = colors.secondaryText
                )
            },
            confirmButton = {
                StudyOSButton(
                    text = "Delete",
                    onClick = {
                        showDeleteConfirmDialog = false
                        viewModel.deleteNote()
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel", color = colors.secondaryText)
                }
            },
            containerColor = colors.surface
        )
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
                    .imePadding()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                // Top App Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f, fill = false),
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

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = if (uiState.noteId == null) "New Note" else "Edit Note",
                            style = typography.secondary,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.primaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
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

                        Box {
                            StudyOSIconButton(
                                onClick = { showMoreMenu = true },
                                contentDescription = "More Options"
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.MoreVert,
                                    contentDescription = null,
                                    tint = colors.secondaryText,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showMoreMenu,
                                onDismissRequest = { showMoreMenu = false },
                                modifier = Modifier.background(colors.surface)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Import Document (PDF, Word, MD, TXT)", style = typography.body, color = colors.primaryText) },
                                    onClick = {
                                        showMoreMenu = false
                                        filePickerLauncher.launch("*/*")
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.FileUpload, null, tint = colors.accent, modifier = Modifier.size(18.dp))
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text("Google Docs Preview", style = typography.body, color = colors.primaryText) },
                                    onClick = {
                                        showMoreMenu = false
                                        onOpenDocumentViewer(uiState.noteId, uiState.title)
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.Visibility, null, tint = colors.accent, modifier = Modifier.size(18.dp))
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text("Share Note", style = typography.body, color = colors.primaryText) },
                                    onClick = {
                                        showMoreMenu = false
                                        val shareText = "${uiState.title.ifBlank { "Untitled Note" }}\n\n${uiState.content}"
                                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_SUBJECT, uiState.title)
                                            putExtra(Intent.EXTRA_TEXT, shareText)
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, "Share Note"))
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.Share, null, tint = colors.accent, modifier = Modifier.size(18.dp))
                                    }
                                )

                                if (uiState.noteId != null) {
                                    DropdownMenuItem(
                                        text = { Text("Delete Note", style = typography.body, color = Color(0xFFE53E3E)) },
                                        onClick = {
                                            showMoreMenu = false
                                            showDeleteConfirmDialog = true
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Outlined.DeleteOutline, null, tint = Color(0xFFE53E3E), modifier = Modifier.size(18.dp))
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(4.dp))

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
                    ToolbarButton(
                        icon = Icons.AutoMirrored.Outlined.Undo,
                        label = "Undo",
                        enabled = uiState.canUndo
                    ) {
                        viewModel.undo()
                    }
                    ToolbarButton(
                        icon = Icons.AutoMirrored.Outlined.Redo,
                        label = "Redo",
                        enabled = uiState.canRedo
                    ) {
                        viewModel.redo()
                    }

                    Box(
                        modifier = Modifier
                            .height(20.dp)
                            .width(1.dp)
                            .background(colors.border)
                            .padding(horizontal = 4.dp)
                    )

                    ToolbarButton(icon = Icons.Outlined.FormatBold, label = "Bold") {
                        viewModel.insertMarkdown("**", "**")
                    }
                    ToolbarButton(icon = Icons.Outlined.FormatItalic, label = "Italic") {
                        viewModel.insertMarkdown("*", "*")
                    }
                    ToolbarButton(icon = Icons.Outlined.Title, label = "H1") {
                        viewModel.insertMarkdown("# ")
                    }
                    ToolbarButton(icon = Icons.AutoMirrored.Outlined.FormatListBulleted, label = "List") {
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
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val colors = StudyOSTheme.colors
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (enabled) colors.primaryText else colors.mutedText.copy(alpha = 0.35f),
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
