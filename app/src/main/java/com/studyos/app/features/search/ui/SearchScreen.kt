package com.studyos.app.features.search.ui

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.studyos.app.core.ui.component.StudyOSEmptyState
import com.studyos.app.core.ui.component.StudyOSIconButton
import com.studyos.app.core.ui.component.StudyOSProgressBar
import com.studyos.app.core.ui.component.StudyOSSearchField
import com.studyos.app.features.search.viewmodel.SearchViewModel
import com.studyos.app.theme.StudyOSTheme

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onBack: () -> Unit,
    onSubjectClick: (String) -> Unit = {},
    onChapterClick: (String) -> Unit = {},
    onNoteClick: (noteId: String, subjectId: String, chapterId: String) -> Unit = { _, _, _ -> },
    onFlashcardClick: (chapterId: String) -> Unit = {},
    onMistakeClick: (mistakeId: String) -> Unit = {},
    onExamClick: (examId: String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .widthIn(max = 680.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StudyOSIconButton(
                    onClick = onBack,
                    contentDescription = "Navigate back"
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = null,
                        tint = colors.primaryText,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                StudyOSSearchField(
                    query = uiState.query,
                    onQueryChange = viewModel::onQueryChanged,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (!uiState.hasSearched) {
                Text(
                    text = "Search across subjects, chapters, notes, flashcards, mistakes, and recall prompts.",
                    style = typography.secondary,
                    color = colors.mutedText,
                    modifier = Modifier.padding(start = 8.dp)
                )
            } else if (uiState.totalResultsCount == 0) {
                StudyOSEmptyState(
                    title = "No results found",
                    description = "Nothing matched \"${uiState.query}\"."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1. Subjects
                    if (uiState.subjectResults.isNotEmpty()) {
                        item {
                            Text(
                                text = "Subjects (${uiState.subjectResults.size})",
                                style = typography.secondary,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.secondaryText,
                                modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 4.dp)
                            )
                        }

                        items(uiState.subjectResults, key = { "sub_${it.id}" }) { subject ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(shapes.surface)
                                    .background(colors.surface)
                                    .border(1.dp, colors.border, shapes.surface)
                                    .clickable { onSubjectClick(subject.id) }
                                    .semantics { role = Role.Button }
                                    .padding(horizontal = 16.dp, vertical = 14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = colors.accent
                                    )
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Text(
                                        text = subject.name,
                                        style = typography.bodyMedium,
                                        color = colors.primaryText
                                    )
                                }
                            }
                        }
                    }

                    // 2. Chapters
                    if (uiState.chapterResults.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Chapters (${uiState.chapterResults.size})",
                                style = typography.secondary,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.secondaryText,
                                modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 4.dp)
                            )
                        }

                        items(uiState.chapterResults, key = { "chap_${it.chapter.id}" }) { result ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(shapes.surface)
                                    .background(colors.surface)
                                    .border(1.dp, colors.border, shapes.surface)
                                    .clickable { onChapterClick(result.chapter.id) }
                                    .semantics { role = Role.Button }
                                    .padding(horizontal = 16.dp, vertical = 14.dp)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.BookmarkBorder,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = colors.accent
                                        )
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = result.chapter.name,
                                                style = typography.bodyMedium,
                                                color = colors.primaryText
                                            )
                                            Text(
                                                text = result.subjectName,
                                                style = typography.caption,
                                                color = colors.mutedText,
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                        }

                                        Text(
                                            text = "${result.chapter.progress}%",
                                            style = typography.caption,
                                            color = colors.secondaryText
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    StudyOSProgressBar(
                                        progress = result.chapter.progress,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    // 3. Notes
                    if (uiState.noteResults.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Notes (${uiState.noteResults.size})",
                                style = typography.secondary,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.secondaryText,
                                modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 4.dp)
                            )
                        }

                        items(uiState.noteResults, key = { "note_${it.id}" }) { note ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(shapes.surface)
                                    .background(colors.surface)
                                    .border(1.dp, colors.border, shapes.surface)
                                    .clickable { onNoteClick(note.id, note.subjectId ?: "", note.chapterId ?: "") }
                                    .semantics { role = Role.Button }
                                    .padding(horizontal = 16.dp, vertical = 14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Description,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp).padding(top = 2.dp),
                                        tint = colors.accent
                                    )
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = note.title,
                                            style = typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = colors.primaryText
                                        )
                                        if (note.content.isNotBlank()) {
                                            Text(
                                                text = note.content.take(120),
                                                style = typography.caption,
                                                color = colors.secondaryText,
                                                maxLines = 2,
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 4. Flashcards
                    if (uiState.flashcardResults.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Flashcards (${uiState.flashcardResults.size})",
                                style = typography.secondary,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.secondaryText,
                                modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 4.dp)
                            )
                        }

                        items(uiState.flashcardResults, key = { "fc_${it.id}" }) { card ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(shapes.surface)
                                    .background(colors.surface)
                                    .border(1.dp, colors.border, shapes.surface)
                                    .clickable { onFlashcardClick(card.chapterId ?: "") }
                                    .semantics { role = Role.Button }
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Style,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp).padding(top = 2.dp),
                                        tint = colors.accent
                                    )
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = card.question,
                                            style = typography.bodyMedium,
                                            color = colors.primaryText
                                        )
                                        Text(
                                            text = card.answer,
                                            style = typography.caption,
                                            color = colors.mutedText,
                                            maxLines = 1,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 5. Mistakes
                    if (uiState.mistakeResults.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Mistakes (${uiState.mistakeResults.size})",
                                style = typography.secondary,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.secondaryText,
                                modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 4.dp)
                            )
                        }

                        items(uiState.mistakeResults, key = { "mst_${it.id}" }) { mistake ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(shapes.surface)
                                    .background(colors.surface)
                                    .border(1.dp, colors.border, shapes.surface)
                                    .clickable { onMistakeClick(mistake.id) }
                                    .semantics { role = Role.Button }
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ErrorOutline,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp).padding(top = 2.dp),
                                        tint = colors.critical
                                    )
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = mistake.question,
                                            style = typography.bodyMedium,
                                            color = colors.primaryText
                                        )
                                        Row(
                                            modifier = Modifier.padding(top = 4.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            if (!mistake.topic.isNullOrBlank()) {
                                                Text(
                                                    text = mistake.topic,
                                                    style = typography.caption,
                                                    color = colors.accent
                                                )
                                            }
                                            Text(
                                                text = if (mistake.isResolved) "Resolved" else "Needs Review",
                                                style = typography.caption,
                                                color = if (mistake.isResolved) colors.success else colors.critical
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 6. Active Recall Items
                    if (uiState.recallResults.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Recall Prompts (${uiState.recallResults.size})",
                                style = typography.secondary,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.secondaryText,
                                modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 4.dp)
                            )
                        }

                        items(uiState.recallResults, key = { "rec_${it.id}" }) { recall ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(shapes.surface)
                                    .background(colors.surface)
                                    .border(1.dp, colors.border, shapes.surface)
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Psychology,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp).padding(top = 2.dp),
                                        tint = colors.accent
                                    )
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = recall.prompt,
                                            style = typography.bodyMedium,
                                            color = colors.primaryText
                                        )
                                        Text(
                                            text = "Accuracy: ${recall.recallAccuracy}% • Next review in ${recall.intervalDays}d",
                                            style = typography.caption,
                                            color = colors.mutedText,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 7. Exams & Mock Tests
                    if (uiState.examResults.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Exams & Mock Tests (${uiState.examResults.size})",
                                style = typography.secondary,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.secondaryText,
                                modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 4.dp)
                            )
                        }

                        items(uiState.examResults, key = { "exam_${it.id}" }) { exam ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(shapes.surface)
                                    .background(colors.surface)
                                    .border(1.dp, colors.border, shapes.surface)
                                    .clickable { onExamClick(exam.id) }
                                    .semantics { role = Role.Button }
                                    .padding(horizontal = 16.dp, vertical = 14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val isMock = exam.notes?.contains("[MOCK_TEST]") == true
                                    Icon(
                                        imageVector = if (isMock) Icons.AutoMirrored.Outlined.Assignment else Icons.Outlined.School,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = colors.accent
                                    )
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = exam.name,
                                            style = typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = colors.primaryText
                                        )
                                        val examTypeLabel = if (isMock) "Mock Test" else "Major Exam"
                                        val targetLabel = if (exam.targetScore != null) " • Target: ${exam.targetScore}%" else ""
                                        Text(
                                            text = "$examTypeLabel$targetLabel",
                                            style = typography.caption,
                                            color = colors.mutedText,
                                            maxLines = 1,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
