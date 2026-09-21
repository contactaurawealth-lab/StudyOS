package com.studyos.app.features.library.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Functions
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studyos.app.core.ui.component.GlassCard
import com.studyos.app.core.ui.component.GlassIconButton
import com.studyos.app.core.ui.component.GlassTopBar
import com.studyos.app.core.ui.component.StudyOSButton
import com.studyos.app.core.ui.component.StudyOSEmptyState
import com.studyos.app.core.ui.component.StudyOSLoadingState
import com.studyos.app.core.ui.component.StudyOSMarkdown
import com.studyos.app.core.ui.component.StudyOSOutlinedButton
import com.studyos.app.core.ui.component.StudyOSProgressBar
import com.studyos.app.core.util.DateTimeUtils
import com.studyos.app.features.library.viewmodel.LibraryDeckItem
import com.studyos.app.features.library.viewmodel.LibraryFormulaItem
import com.studyos.app.features.library.viewmodel.LibraryNoteItem
import com.studyos.app.features.library.viewmodel.LibraryResourceItem
import com.studyos.app.features.library.viewmodel.LibraryTab
import com.studyos.app.features.library.viewmodel.LibraryViewModel
import com.studyos.app.theme.StudyOSTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onOpenNote: (noteId: String, subjectId: String?, chapterId: String?) -> Unit = { _, _, _ -> },
    onStudyDeck: (chapterId: String) -> Unit = {},
    onOpenSubject: (subjectId: String) -> Unit = {},
    onOpenDrawer: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    if (uiState.isAddResourceDialogOpen) {
        AddResourceDialog(
            subjects = uiState.subjects,
            onDismiss = viewModel::closeAddResourceDialog,
            onAdd = viewModel::addResource
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Frosted Top Bar
            GlassTopBar(
                title = "Knowledge Library",
                subtitle = "${uiState.notes.size} notes • ${uiState.decks.size} flashcard decks",
                navigationIcon = {
                    GlassIconButton(
                        onClick = onOpenDrawer,
                        contentDescription = "Open Navigation Menu"
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = null,
                            tint = colors.primaryText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                actions = {
                    if (uiState.currentTab == LibraryTab.RESOURCES) {
                        GlassIconButton(
                            onClick = viewModel::openAddResourceDialog,
                            contentDescription = "Add Study Resource"
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 680.dp)
                ) {
                    // Search Bar
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = viewModel::setSearchQuery,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text(
                                    "Search notes, decks, formulas...",
                                    style = typography.body,
                                    color = colors.mutedText
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Search,
                                    contentDescription = null,
                                    tint = colors.secondaryText,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = colors.surface,
                                unfocusedContainerColor = colors.surface,
                                focusedTextColor = colors.primaryText,
                                unfocusedTextColor = colors.primaryText,
                                focusedIndicatorColor = colors.accent,
                                unfocusedIndicatorColor = colors.border
                            )
                        )
                    }

                    // Tab Selector Pills
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LibraryTabPill(
                            label = "Notes (${uiState.notes.size})",
                            isSelected = uiState.currentTab == LibraryTab.NOTES,
                            onClick = { viewModel.setTab(LibraryTab.NOTES) }
                        )
                        LibraryTabPill(
                            label = "Flashcard Decks (${uiState.decks.size})",
                            isSelected = uiState.currentTab == LibraryTab.FLASHCARDS,
                            onClick = { viewModel.setTab(LibraryTab.FLASHCARDS) }
                        )
                        LibraryTabPill(
                            label = "Formulas & Rules (${uiState.formulas.size})",
                            isSelected = uiState.currentTab == LibraryTab.FORMULAS,
                            onClick = { viewModel.setTab(LibraryTab.FORMULAS) }
                        )
                        LibraryTabPill(
                            label = "Resources (${uiState.resources.size})",
                            isSelected = uiState.currentTab == LibraryTab.RESOURCES,
                            onClick = { viewModel.setTab(LibraryTab.RESOURCES) }
                        )
                    }

                    // Subject Filter Chips
                    if (uiState.subjects.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            SubjectFilterChip(
                                name = "All Subjects",
                                isSelected = uiState.selectedSubjectId == null,
                                onClick = { viewModel.selectSubjectFilter(null) }
                            )
                            uiState.subjects.forEach { subject ->
                                SubjectFilterChip(
                                    name = subject.name,
                                    isSelected = uiState.selectedSubjectId == subject.id,
                                    onClick = { viewModel.selectSubjectFilter(subject.id) }
                                )
                            }
                        }
                    }

                    // Content Area
                    if (uiState.isLoading) {
                        StudyOSLoadingState(message = "Loading knowledge library...")
                    } else {
                        when (uiState.currentTab) {
                            LibraryTab.NOTES -> NotesListSection(
                                notes = uiState.notes,
                                onOpenNote = onOpenNote,
                                onTogglePin = viewModel::togglePinNote,
                                onDelete = viewModel::deleteNote
                            )
                            LibraryTab.FLASHCARDS -> DecksListSection(
                                decks = uiState.decks,
                                onStudyDeck = onStudyDeck
                            )
                            LibraryTab.FORMULAS -> FormulasListSection(
                                formulas = uiState.formulas,
                                onOpenNote = onOpenNote
                            )
                            LibraryTab.RESOURCES -> ResourcesListSection(
                                resources = uiState.resources,
                                onAddResource = viewModel::openAddResourceDialog,
                                onDeleteResource = viewModel::deleteResource
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryTabPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) colors.accent else colors.surface)
            .border(1.dp, if (isSelected) colors.accent else colors.border, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(
            text = label,
            style = typography.caption.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium),
            color = if (isSelected) colors.background else colors.secondaryText
        )
    }
}

@Composable
private fun SubjectFilterChip(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) colors.accent.copy(alpha = 0.15f) else colors.surface.copy(alpha = 0.5f))
            .border(1.dp, if (isSelected) colors.accent else colors.border.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = name,
            style = typography.caption.copy(fontSize = 11.sp),
            color = if (isSelected) colors.accent else colors.mutedText
        )
    }
}

@Composable
private fun NotesListSection(
    notes: List<LibraryNoteItem>,
    onOpenNote: (noteId: String, subjectId: String?, chapterId: String?) -> Unit,
    onTogglePin: (noteId: String, currentPinned: Boolean) -> Unit,
    onDelete: (noteId: String) -> Unit
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography

    if (notes.isEmpty()) {
        StudyOSEmptyState(
            title = "No notes found",
            description = "Create notes inside any chapter or import Markdown summaries to see them in your unified library."
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(notes, key = { it.note.id }) { item ->
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenNote(item.note.id, item.note.subjectId, item.note.chapterId) }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (item.note.isPinned) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(colors.accent.copy(alpha = 0.2f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "PINNED",
                                            style = typography.caption.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                            color = colors.accent
                                        )
                                    }
                                }

                                Text(
                                    text = item.note.title.ifBlank { "Untitled Note" },
                                    style = typography.subsectionTitle,
                                    color = colors.primaryText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                GlassIconButton(
                                    onClick = { onTogglePin(item.note.id, item.note.isPinned) },
                                    contentDescription = "Pin Note"
                                ) {
                                    Icon(
                                        imageVector = if (item.note.isPinned) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                                        contentDescription = null,
                                        tint = if (item.note.isPinned) colors.accent else colors.mutedText,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                GlassIconButton(
                                    onClick = { onDelete(item.note.id) },
                                    contentDescription = "Delete Note"
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Delete,
                                        contentDescription = null,
                                        tint = colors.mutedText,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Preview content
                        val previewText = item.note.content
                            .replace(Regex("^[#*`>\\-\\s]+", RegexOption.MULTILINE), "")
                            .take(160)
                            .trim()

                        if (previewText.isNotBlank()) {
                            Text(
                                text = previewText,
                                style = typography.secondary,
                                color = colors.secondaryText,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Tags & Date Footer
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(colors.surface)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = item.subjectName,
                                        style = typography.caption.copy(fontSize = 10.sp),
                                        color = colors.accent
                                    )
                                }

                                Text(
                                    text = "•",
                                    style = typography.caption,
                                    color = colors.mutedText
                                )

                                Text(
                                    text = item.chapterName,
                                    style = typography.caption.copy(fontSize = 10.sp),
                                    color = colors.mutedText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Text(
                                text = DateTimeUtils.formatDateShort(item.note.updatedAt),
                                style = typography.caption.copy(fontSize = 10.sp),
                                color = colors.mutedText
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DecksListSection(
    decks: List<LibraryDeckItem>,
    onStudyDeck: (chapterId: String) -> Unit
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography

    if (decks.isEmpty()) {
        StudyOSEmptyState(
            title = "No flashcard decks yet",
            description = "Generate flashcards inside chapters or practice hubs to build your spaced repetition library."
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(decks, key = { it.chapterId }) { deck ->
                GlassCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = deck.chapterName,
                                    style = typography.subsectionTitle,
                                    color = colors.primaryText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = deck.subjectName,
                                    style = typography.caption,
                                    color = colors.accent
                                )
                            }

                            if (deck.dueCards > 0) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(colors.accent.copy(alpha = 0.2f))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${deck.dueCards} DUE",
                                        style = typography.caption.copy(fontWeight = FontWeight.Bold, color = colors.accent)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Stats bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${deck.totalCards} cards in deck",
                                style = typography.secondary,
                                color = colors.secondaryText
                            )
                            Text(
                                text = "${deck.masteryPercentage}% mastered",
                                style = typography.caption,
                                color = if (deck.masteryPercentage >= 75) colors.accent else colors.mutedText
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        StudyOSProgressBar(
                            progress = deck.masteryPercentage / 100f,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        StudyOSButton(
                            text = if (deck.dueCards > 0) "Study Due Cards (${deck.dueCards})" else "Practice All Cards",
                            onClick = { onStudyDeck(deck.chapterId) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FormulasListSection(
    formulas: List<LibraryFormulaItem>,
    onOpenNote: (noteId: String, subjectId: String?, chapterId: String?) -> Unit
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val clipboardManager = LocalClipboardManager.current
    var copiedId by remember { mutableStateOf<String?>(null) }

    if (formulas.isEmpty()) {
        StudyOSEmptyState(
            title = "No math or formulas detected",
            description = "Use $$ equation $$ or $ inline $ in your notes to automatically collect formulas into this quick reference sheet."
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(formulas, key = { it.id }) { item ->
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(colors.accent.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = item.subjectName,
                                        style = typography.caption.copy(fontSize = 10.sp, color = colors.accent)
                                    )
                                }

                                Text(
                                    text = item.title,
                                    style = typography.bodyMedium,
                                    color = colors.primaryText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Row {
                                GlassIconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(item.formulaContent))
                                        copiedId = item.id
                                    },
                                    contentDescription = "Copy formula"
                                ) {
                                    Icon(
                                        imageVector = if (copiedId == item.id) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                                        contentDescription = null,
                                        tint = if (copiedId == item.id) colors.accent else colors.mutedText,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                GlassIconButton(
                                    onClick = { onOpenNote(item.noteId, item.subjectId, item.chapterId) },
                                    contentDescription = "Open note"
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.OpenInNew,
                                        contentDescription = null,
                                        tint = colors.mutedText,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.surface)
                                .border(1.dp, colors.border, RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            StudyOSMarkdown(content = item.formulaContent)
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "From: ${item.chapterName}",
                            style = typography.caption.copy(fontSize = 10.sp),
                            color = colors.mutedText
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResourcesListSection(
    resources: List<LibraryResourceItem>,
    onAddResource: () -> Unit,
    onDeleteResource: (com.studyos.app.core.database.entity.ResourceEntity) -> Unit
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val clipboardManager = LocalClipboardManager.current
    var copiedResId by remember { mutableStateOf<String?>(null) }

    if (resources.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            StudyOSEmptyState(
                title = "No study resources saved",
                description = "Save textbook links, lecture slides, and PDF references to keep all coursework organized offline."
            )
            Spacer(modifier = Modifier.height(16.dp))
            StudyOSButton(
                text = "+ Add Study Resource",
                onClick = onAddResource
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(resources, key = { it.resource.id }) { item ->
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val icon = when (item.resource.type.uppercase()) {
                            "PDF" -> Icons.Outlined.PictureAsPdf
                            "LINK" -> Icons.Outlined.Link
                            else -> Icons.Outlined.Description
                        }

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.accent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.resource.title,
                                style = typography.subsectionTitle,
                                color = colors.primaryText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${item.resource.type} • ${item.subjectName ?: "General"}",
                                style = typography.caption,
                                color = colors.mutedText
                            )
                        }

                        Row {
                            GlassIconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(item.resource.uriOrPath))
                                    copiedResId = item.resource.id
                                },
                                contentDescription = "Copy Path/URL"
                            ) {
                                Icon(
                                    imageVector = if (copiedResId == item.resource.id) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                                    contentDescription = null,
                                    tint = if (copiedResId == item.resource.id) colors.accent else colors.mutedText,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            GlassIconButton(
                                onClick = { onDeleteResource(item.resource) },
                                contentDescription = "Delete Resource"
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = null,
                                    tint = colors.mutedText,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddResourceDialog(
    subjects: List<com.studyos.app.core.database.entity.SubjectEntity>,
    onDismiss: () -> Unit,
    onAdd: (title: String, type: String, uriOrPath: String, subjectId: String?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var uriOrPath by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("LINK") }
    var selectedSubjectId by remember { mutableStateOf<String?>(subjects.firstOrNull()?.id) }

    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Study Resource", style = typography.subsectionTitle, color = colors.primaryText) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title (e.g. Calculus Reference)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = uriOrPath,
                    onValueChange = { uriOrPath = it },
                    label = { Text("URL or Local Path") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Resource Type", style = typography.caption, color = colors.mutedText)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("LINK", "PDF", "DOCUMENT").forEach { type ->
                        val isSelected = selectedType == type
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) colors.accent else colors.surface)
                                .clickable { selectedType = type }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = type,
                                style = typography.caption,
                                color = if (isSelected) colors.background else colors.secondaryText
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            StudyOSButton(
                text = "Add",
                enabled = title.isNotBlank() && uriOrPath.isNotBlank(),
                onClick = {
                    onAdd(title, selectedType, uriOrPath, selectedSubjectId)
                }
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.secondaryText)
            }
        },
        containerColor = colors.surface
    )
}
