package com.studyos.app.features.subjects.ui

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studyos.app.core.ui.component.GlassCard
import com.studyos.app.core.ui.component.GlassDialog
import com.studyos.app.core.ui.component.GlassIconButton
import com.studyos.app.core.ui.component.GlassTopBar
import com.studyos.app.core.ui.component.ShimmerPlaceholder
import com.studyos.app.core.ui.component.StudyOSButton
import com.studyos.app.core.ui.component.StudyOSConfirmationDialog
import com.studyos.app.core.ui.component.StudyOSDialog
import com.studyos.app.core.ui.component.StudyOSEmptyState
import com.studyos.app.core.ui.component.StudyOSIconButton
import com.studyos.app.core.ui.component.StudyOSLoadingState
import com.studyos.app.core.ui.component.StudyOSOutlinedButton
import com.studyos.app.core.ui.component.StudyOSProgressBar
import com.studyos.app.core.ui.component.StudyOSTextDialog
import com.studyos.app.core.ui.component.StudyOSTextField
import com.studyos.app.domain.model.Chapter
import com.studyos.app.domain.model.ChapterStatus
import com.studyos.app.domain.model.Note
import com.studyos.app.features.subjects.viewmodel.SubjectDetailViewModel
import com.studyos.app.theme.StudyOSTheme
import kotlinx.coroutines.launch

@Composable
fun SubjectDetailScreen(
    viewModel: SubjectDetailViewModel,
    onChapterClick: (String) -> Unit,
    onNoteClick: (String) -> Unit = {},
    onCreateNote: () -> Unit = {},
    onStartAudioWalk: () -> Unit = {},
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Chapters, 1: Notes & AI Context
    var showAddChapterSheet by remember { mutableStateOf(false) }
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var noteToDelete by remember { mutableStateOf<Note?>(null) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // File picker launcher for uploading notes/documents (PDF, DOCX, TXT, MD)
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    // 1. Copy document to persistent internal storage to prevent permission expiration
                    val stored = com.studyos.app.core.util.DocumentStorageManager.saveDocumentLocally(context, uri)

                    // 2. Extract text from the persistent local copy
                    val extracted = com.studyos.app.core.util.DocumentTextExtractor.extract(
                        context,
                        Uri.fromFile(stored.file),
                        stored.cleanTitle
                    )

                    // 3. Format note with rich metadata header
                    val noteHeader = buildString {
                        append("> [!NOTE] **Source Document:** ${stored.originalFileName} (${stored.formattedSize} • ${stored.extension.uppercase()})\n")
                        append("> 📂 *Saved offline. Open original file in Google Drive, Docs, Word, or PDF Reader from Knowledge Library Resources.*\n\n")
                    }
                    val bodyContent = if (extracted.content.isNotBlank()) {
                        extracted.content
                    } else {
                        "*(Document contains scanned pages or binary formatting. Open in external viewer via Resources to read full document.)*"
                    }
                    val fullContent = noteHeader + bodyContent

                    // 4. Save to Subject Notes for AI context & revision
                    viewModel.addOrUploadNote(title = stored.cleanTitle, content = fullContent)

                    // 5. Index in Subject Resources so original file can be opened in external apps
                    viewModel.addResource(
                        title = stored.originalFileName,
                        type = when (stored.extension.lowercase()) {
                            "pdf" -> "PDF"
                            "md", "markdown" -> "MARKDOWN"
                            else -> "DOCUMENT"
                        },
                        uriOrPath = stored.persistentPath
                    )

                    snackbarHostState.showSnackbar("Uploaded ${stored.originalFileName} (${stored.formattedSize}) to Notes & Resources!")
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Failed to process document: ${e.message}")
                }
            }
        }
    }

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) {
            onBack()
        }
    }

    LaunchedEffect(uiState.actionMessage) {
        uiState.actionMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background),
        contentAlignment = Alignment.TopCenter
    ) {
        if (uiState.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp)
                    .padding(horizontal = StudyOSTheme.spacing.screenHorizontal, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                repeat(4) {
                    ShimmerPlaceholder(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        shape = shapes.medium
                    )
                }
            }
        } else if (uiState.subject == null) {
            StudyOSEmptyState(
                title = "Subject not found",
                description = "This subject may have been deleted.",
                actionButtonText = "Go back",
                onActionClick = onBack
            )
        } else {
            val subject = uiState.subject!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp)
            ) {
                // Glass Top Bar: ← SubjectName ⋮
                GlassTopBar(
                    title = subject.name,
                    subtitle = "${uiState.chapters.size} chapters • ${uiState.notes.size} notes • ${uiState.progress}% complete",
                    navigationIcon = {
                        GlassIconButton(
                            onClick = onBack,
                            contentDescription = "Back to subjects"
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = null,
                                tint = colors.primaryText,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    actions = {
                        GlassIconButton(
                            onClick = onStartAudioWalk,
                            contentDescription = "Feynman Audio Walk"
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Headphones,
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        GlassIconButton(
                            onClick = {
                                if (selectedTab == 0) {
                                    showAddChapterSheet = true
                                } else {
                                    showAddNoteDialog = true
                                }
                            },
                            contentDescription = if (selectedTab == 0) "Add chapter" else "Add note"
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = null,
                                tint = colors.primaryText,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Box {
                            GlassIconButton(
                                onClick = { menuExpanded = true },
                                contentDescription = "Subject options"
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.MoreVert,
                                    contentDescription = null,
                                    tint = colors.secondaryText,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false },
                                modifier = Modifier.background(colors.surface)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Upload note from file", style = typography.body, color = colors.primaryText) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.UploadFile,
                                            contentDescription = null,
                                            tint = colors.accent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        filePickerLauncher.launch(
                                            arrayOf(
                                                "application/pdf",
                                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                                "application/msword",
                                                "text/*"
                                            )
                                        )
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Create new note", style = typography.body, color = colors.primaryText) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.EditNote,
                                            contentDescription = null,
                                            tint = colors.primaryText,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        showAddNoteDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Edit subject", style = typography.body, color = colors.primaryText) },
                                    onClick = {
                                        menuExpanded = false
                                        showRenameDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete subject", style = typography.body, color = colors.accent) },
                                    onClick = {
                                        menuExpanded = false
                                        showDeleteConfirmDialog = true
                                    }
                                )
                            }
                        }
                    }
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = StudyOSTheme.spacing.screenHorizontal)
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Subject Progress Header Card
                    GlassCard(
                        backgroundColor = colors.glassSurface,
                        padding = 16.dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${uiState.progress}% complete",
                                style = typography.sectionTitle.copy(fontWeight = FontWeight.SemiBold),
                                color = colors.primaryText
                            )

                            val chapterSummary = when (uiState.chapters.size) {
                                0 -> "0 chapters"
                                1 -> "1 chapter"
                                else -> "${uiState.chapters.size} chapters"
                            }
                            Text(
                                text = "$chapterSummary • ${uiState.notes.size} notes",
                                style = typography.secondary,
                                color = colors.secondaryText
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        StudyOSProgressBar(
                            progress = uiState.progress,
                            height = 5.dp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Tab Selector: Chapters | Notes & AI Context
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shapes.medium)
                            .background(colors.cardBackground)
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(shapes.small)
                                .background(if (selectedTab == 0) colors.glassSurface else Color.Transparent)
                                .border(
                                    width = if (selectedTab == 0) 1.dp else 0.dp,
                                    color = if (selectedTab == 0) colors.glassBorder else Color.Transparent,
                                    shape = shapes.small
                                )
                                .clickable { selectedTab = 0 }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Chapters",
                                    style = typography.caption.copy(fontWeight = if (selectedTab == 0) FontWeight.SemiBold else FontWeight.Medium),
                                    color = if (selectedTab == 0) colors.primaryText else colors.secondaryText
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(if (selectedTab == 0) colors.accent.copy(alpha = 0.2f) else colors.surface)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${uiState.chapters.size}",
                                        style = typography.caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                        color = if (selectedTab == 0) colors.accent else colors.mutedText
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(shapes.small)
                                .background(if (selectedTab == 1) colors.glassSurface else Color.Transparent)
                                .border(
                                    width = if (selectedTab == 1) 1.dp else 0.dp,
                                    color = if (selectedTab == 1) colors.glassBorder else Color.Transparent,
                                    shape = shapes.small
                                )
                                .clickable { selectedTab = 1 }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AutoAwesome,
                                    contentDescription = null,
                                    tint = if (selectedTab == 1) colors.accent else colors.mutedText,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "Notes & AI Context",
                                    style = typography.caption.copy(fontWeight = if (selectedTab == 1) FontWeight.SemiBold else FontWeight.Medium),
                                    color = if (selectedTab == 1) colors.primaryText else colors.secondaryText
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(if (selectedTab == 1) colors.accent.copy(alpha = 0.2f) else colors.surface)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${uiState.notes.size}",
                                        style = typography.caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                        color = if (selectedTab == 1) colors.accent else colors.mutedText
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (selectedTab == 0) {
                        // Chapters Section Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Chapters",
                                style = typography.sectionTitle.copy(fontWeight = FontWeight.SemiBold),
                                color = colors.primaryText
                            )

                            StudyOSOutlinedButton(
                                text = "Add chapter",
                                onClick = { showAddChapterSheet = true }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Chapter List / Empty State
                        if (uiState.chapters.isEmpty()) {
                            Spacer(modifier = Modifier.height(20.dp))
                            StudyOSEmptyState(
                                title = "No chapters yet",
                                description = "Add chapters to start tracking your progress.",
                                actionButtonText = "Add Chapter",
                                onActionClick = { showAddChapterSheet = true }
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 88.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                itemsIndexed(uiState.chapters, key = { _, chapter -> chapter.id }) { index, chapter ->
                                    ChapterListItemRow(
                                        chapter = chapter,
                                        index = index,
                                        isFirst = index == 0,
                                        isLast = index == uiState.chapters.size - 1,
                                        onClick = { onChapterClick(chapter.id) },
                                        onMoveUp = { viewModel.moveChapter(chapter.id, moveUp = true) },
                                        onMoveDown = { viewModel.moveChapter(chapter.id, moveUp = false) },
                                        onDelete = { viewModel.deleteChapter(chapter.id) }
                                    )
                                }
                            }
                        }
                    } else {
                        // Notes & AI Context View
                        // Banner: Explaining AI Context Reading
                        GlassCard(
                            backgroundColor = colors.accent.copy(alpha = 0.08f),
                            borderColor = colors.accent.copy(alpha = 0.25f),
                            padding = 12.dp
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AutoAwesome,
                                    contentDescription = null,
                                    tint = colors.accent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "AI Study Assistant automatically reads and references all notes here during chat, quizzes, and revision.",
                                    style = typography.caption,
                                    color = colors.primaryText,
                                    lineHeight = 16.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Uploaded Notes (${uiState.notes.size})",
                                style = typography.sectionTitle.copy(fontWeight = FontWeight.SemiBold),
                                color = colors.primaryText
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                StudyOSOutlinedButton(
                                    text = "Upload file",
                                    onClick = {
                                        filePickerLauncher.launch(
                                            arrayOf(
                                                "application/pdf",
                                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                                "application/msword",
                                                "text/*"
                                            )
                                        )
                                    }
                                )
                                StudyOSButton(
                                    text = "+ Add Note",
                                    onClick = { showAddNoteDialog = true }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (uiState.notes.isEmpty()) {
                            Spacer(modifier = Modifier.height(20.dp))
                            StudyOSEmptyState(
                                title = "No notes uploaded yet",
                                description = "Upload lecture slides, markdown guides, or text notes to allow your AI tutor to learn from your actual class materials.",
                                actionButtonText = "Upload Note / Document",
                                onActionClick = {
                                    filePickerLauncher.launch(
                                        arrayOf(
                                            "application/pdf",
                                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                            "application/msword",
                                            "text/*",
                                            "application/vnd.ms-powerpoint",
                                            "application/vnd.openxmlformats-officedocument.presentationml.presentation"
                                        )
                                    )
                                }
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 88.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(uiState.notes, key = { it.id }) { note ->
                                    SubjectNoteItemCard(
                                        note = note,
                                        onClick = { onNoteClick(note.id) },
                                        onDelete = { noteToDelete = note }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Add Chapter Bottom Sheet
        if (showAddChapterSheet) {
            AddChapterBottomSheet(
                onDismissRequest = {
                    showAddChapterSheet = false
                    viewModel.clearMessages()
                },
                onAddChapter = { name, desc ->
                    val success = viewModel.addChapter(name, desc)
                    if (success) {
                        showAddChapterSheet = false
                    }
                    success
                },
                errorMessage = uiState.errorMessage,
                onClearError = viewModel::clearMessages
            )
        }

        // Quick Add Note Dialog
        if (showAddNoteDialog) {
            SubjectNoteUploadDialog(
                onDismiss = {
                    showAddNoteDialog = false
                    viewModel.clearMessages()
                },
                onUploadFile = {
                    showAddNoteDialog = false
                    filePickerLauncher.launch(
                        arrayOf(
                            "application/pdf",
                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                            "application/msword",
                            "text/*"
                        )
                    )
                },
                onSaveNote = { title, content ->
                    viewModel.addOrUploadNote(title = title, content = content)
                    showAddNoteDialog = false
                },
                onOpenFullEditor = {
                    showAddNoteDialog = false
                    onCreateNote()
                },
                errorMessage = uiState.errorMessage
            )
        }

        // Delete Note Confirmation Dialog
        if (noteToDelete != null) {
            val note = noteToDelete!!
            GlassDialog(
                onDismissRequest = { noteToDelete = null },
                title = "Delete note?",
                message = "Are you sure you want to delete \"${note.title.ifBlank { "Untitled Note" }}\"? It will also be removed from AI tutor memory.",
                confirmButtonText = "Delete",
                dismissButtonText = "Cancel",
                isDestructive = true,
                onConfirm = {
                    viewModel.deleteNote(note.id)
                    noteToDelete = null
                },
                onDismiss = { noteToDelete = null }
            )
        }

        // Rename Subject Dialog
        if (showRenameDialog && uiState.subject != null) {
            val currentSubject = uiState.subject!!
            StudyOSTextDialog(
                title = "Edit subject",
                initialValue = currentSubject.name,
                placeholder = "Subject name",
                confirmButtonText = "Save",
                onConfirm = { newName: String ->
                    coroutineScope.launch {
                        val success = viewModel.renameSubject(newName)
                        if (success) {
                            showRenameDialog = false
                        }
                    }
                },
                onDismiss = {
                    showRenameDialog = false
                    viewModel.clearMessages()
                },
                errorMessage = uiState.errorMessage
            )
        }

        // Delete Subject Confirmation Dialog
        if (showDeleteConfirmDialog) {
            GlassDialog(
                onDismissRequest = { showDeleteConfirmDialog = false },
                title = "Delete subject?",
                message = "This will remove ${uiState.subject?.name ?: "this subject"} and its chapters.",
                confirmButtonText = "Delete",
                dismissButtonText = "Cancel",
                isDestructive = true,
                onConfirm = {
                    viewModel.deleteSubject()
                    showDeleteConfirmDialog = false
                },
                onDismiss = { showDeleteConfirmDialog = false }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }
}

@Composable
private fun ChapterListItemRow(
    chapter: Chapter,
    index: Int,
    isFirst: Boolean,
    isLast: Boolean,
    onClick: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    var menuExpanded by remember { mutableStateOf(false) }

    val isCompleted = chapter.status == ChapterStatus.COMPLETED || chapter.progress == 100
    val formattedIndex = String.format("%02d", index + 1)
    val stateText = when {
        isCompleted -> "Complete ✓"
        chapter.progress > 0 -> "${chapter.progress}%"
        else -> "Not started"
    }
    val stateColor = when {
        isCompleted -> colors.success
        chapter.progress > 0 -> colors.accent
        else -> colors.mutedText
    }

    GlassCard(
        onClick = onClick,
        backgroundColor = colors.glassSurface,
        padding = 14.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Chapter Number (01, 02, etc.)
            Text(
                text = formattedIndex,
                style = typography.caption.copy(fontWeight = FontWeight.SemiBold),
                color = colors.mutedText,
                modifier = Modifier.width(28.dp)
            )

            // Title & State
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = chapter.name,
                    style = typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = if (isCompleted) colors.secondaryText else colors.primaryText
                )
                Text(
                    text = stateText,
                    style = typography.caption.copy(fontWeight = FontWeight.Medium),
                    color = stateColor,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Overflow menu for chapter actions
            Box {
                GlassIconButton(
                    onClick = { menuExpanded = true },
                    contentDescription = "Chapter options",
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = null,
                        tint = colors.secondaryText,
                        modifier = Modifier.size(16.dp)
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(colors.surface)
                ) {
                    if (!isFirst) {
                        DropdownMenuItem(
                            text = { Text("Move up", style = typography.body, color = colors.primaryText) },
                            onClick = {
                                menuExpanded = false
                                onMoveUp()
                            }
                        )
                    }
                    if (!isLast) {
                        DropdownMenuItem(
                            text = { Text("Move down", style = typography.body, color = colors.primaryText) },
                            onClick = {
                                menuExpanded = false
                                onMoveDown()
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Delete chapter", style = typography.body, color = colors.accent) },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                contentDescription = null,
                tint = colors.mutedText,
                modifier = Modifier.size(14.dp)
            )
        }

        if (chapter.progress in 1..99) {
            Spacer(modifier = Modifier.height(8.dp))
            StudyOSProgressBar(
                progress = chapter.progress,
                height = 3.dp,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SubjectNoteItemCard(
    note: Note,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    val wordCount = remember(note.content) {
        if (note.content.isBlank()) 0
        else note.content.split("\\s+".toRegex()).count { it.isNotBlank() }
    }

    GlassCard(
        onClick = onClick,
        backgroundColor = colors.glassSurface,
        padding = 14.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.EditNote,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = note.title.ifBlank { "Untitled Note" },
                        style = typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.primaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                GlassIconButton(
                    onClick = onDelete,
                    contentDescription = "Delete note",
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = null,
                        tint = colors.mutedText,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            if (note.content.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = note.content.trim(),
                    style = typography.caption,
                    color = colors.secondaryText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(shapes.small)
                            .background(colors.accent.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.AutoAwesome,
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "In AI Context",
                                style = typography.caption.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                                color = colors.accent
                            )
                        }
                    }

                    Text(
                        text = "$wordCount words",
                        style = typography.caption.copy(fontSize = 11.sp),
                        color = colors.mutedText
                    )
                }

                Text(
                    text = "Edit note →",
                    style = typography.caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                    color = colors.secondaryText
                )
            }
        }
    }
}

@Composable
private fun SubjectNoteUploadDialog(
    onDismiss: () -> Unit,
    onUploadFile: () -> Unit,
    onSaveNote: (title: String, content: String) -> Unit,
    onOpenFullEditor: () -> Unit,
    errorMessage: String? = null
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    StudyOSDialog(
        onDismissRequest = onDismiss,
        title = "Add Subject Note",
        confirmButtonText = "Save & Ingest",
        onConfirm = {
            if (title.isBlank() && content.isBlank()) {
                localError = "Please enter a title or note content."
            } else {
                onSaveNote(title, content)
            }
        },
        dismissButtonText = "Cancel",
        onDismiss = onDismiss
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Notes are saved to this subject and ingested by the AI Assistant as study context.",
                style = typography.caption,
                color = colors.secondaryText
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Action to upload file directly
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StudyOSOutlinedButton(
                    text = "Import document file",
                    onClick = onUploadFile
                )
                Text(
                    text = "Open editor ↗",
                    style = typography.caption.copy(fontWeight = FontWeight.Medium),
                    color = colors.accent,
                    modifier = Modifier
                        .clickable { onOpenFullEditor() }
                        .padding(4.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Note Title",
                style = typography.caption,
                color = colors.secondaryText
            )
            Spacer(modifier = Modifier.height(4.dp))
            StudyOSTextField(
                value = title,
                onValueChange = {
                    title = it
                    localError = null
                },
                placeholder = "e.g., Week 3 Lecture Notes, Summary...",
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Note Content / Text",
                style = typography.caption,
                color = colors.secondaryText
            )
            Spacer(modifier = Modifier.height(4.dp))
            StudyOSTextField(
                value = content,
                onValueChange = {
                    content = it
                    localError = null
                },
                placeholder = "Paste or type study notes, key formulas, or concept explanations here...",
                singleLine = false,
                maxLines = 8,
                modifier = Modifier.fillMaxWidth()
            )

            val displayError = localError ?: errorMessage
            if (!displayError.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = displayError,
                    style = typography.caption,
                    color = colors.accent
                )
            }
        }
    }
}

