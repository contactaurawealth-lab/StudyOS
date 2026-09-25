package com.studyos.app.features.exams.ui

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studyos.app.core.ui.component.StudyOSEmptyState
import com.studyos.app.domain.model.Chapter
import com.studyos.app.domain.model.Mistake
import com.studyos.app.domain.model.Note
import com.studyos.app.theme.StudyOSTheme

enum class LastMinuteFilter(val label: String) {
    ALL("All Rapid Items"),
    IMPORTANT_CHAPTERS("Important Chapters"),
    WEAK_CHAPTERS("Weak Chapters"),
    MISTAKES("Mistakes"),
    SHORT_NOTES("Short Notes"),
    PENDING_REVISION("Pending Revision")
}

/**
 * Feature 10: LAST-MINUTE REVISION MODE
 * Filter and view ONLY:
 * - Important chapters
 * - Weak chapters ("Needs Attention")
 * - Mistakes
 * - Short notes
 * - Pending revision
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LastMinuteRevisionScreen(
    examName: String,
    importantChapters: List<Chapter>,
    weakChapters: List<Chapter>,
    unresolvedMistakes: List<Mistake>,
    shortNotes: List<Note>,
    pendingRevisions: List<Chapter>,
    onResolveMistake: (String) -> Unit = {},
    onOpenChapter: (String) -> Unit = {},
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    var selectedFilter by remember { mutableStateOf(LastMinuteFilter.ALL) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Last-Minute Rapid Revision",
                            style = typography.caption.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                            color = colors.accent
                        )
                        Text(
                            text = examName,
                            style = typography.sectionTitle.copy(fontSize = 17.sp),
                            color = colors.primaryText
                        )
                    }
                },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Exit Last-Minute Revision",
                            tint = colors.primaryText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Horizontal Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LastMinuteFilter.entries.forEach { filter ->
                    val isSelected = selectedFilter == filter
                    val count = when (filter) {
                        LastMinuteFilter.ALL -> importantChapters.size + weakChapters.size + unresolvedMistakes.size + shortNotes.size + pendingRevisions.size
                        LastMinuteFilter.IMPORTANT_CHAPTERS -> importantChapters.size
                        LastMinuteFilter.WEAK_CHAPTERS -> weakChapters.size
                        LastMinuteFilter.MISTAKES -> unresolvedMistakes.size
                        LastMinuteFilter.SHORT_NOTES -> shortNotes.size
                        LastMinuteFilter.PENDING_REVISION -> pendingRevisions.size
                    }

                    Box(
                        modifier = Modifier
                            .clip(shapes.pill)
                            .background(if (isSelected) colors.accent.copy(alpha = 0.16f) else colors.surface)
                            .border(1.dp, if (isSelected) colors.accent else colors.border, shapes.pill)
                            .clickable { selectedFilter = filter }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${filter.label} ($count)",
                            style = typography.caption.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            ),
                            color = if (isSelected) colors.accent else colors.secondaryText
                        )
                    }
                }
            }

            // Main Content Feed
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Important Chapters Section
                if (selectedFilter == LastMinuteFilter.ALL || selectedFilter == LastMinuteFilter.IMPORTANT_CHAPTERS) {
                    if (importantChapters.isNotEmpty()) {
                        item {
                            SectionLabel(title = "⭐ Important Chapters to Retain", count = importantChapters.size)
                        }
                        items(importantChapters, key = { "imp_${it.id}" }) { chapter ->
                            RapidChapterCard(
                                chapter = chapter,
                                tag = "IMPORTANT",
                                tagColor = colors.accent,
                                onClick = { onOpenChapter(chapter.id) }
                            )
                        }
                    }
                }

                // Weak Chapters ("Needs Attention") Section
                if (selectedFilter == LastMinuteFilter.ALL || selectedFilter == LastMinuteFilter.WEAK_CHAPTERS) {
                    if (weakChapters.isNotEmpty()) {
                        item {
                            SectionLabel(title = "⚠️ Weak Chapters (Needs Attention)", count = weakChapters.size)
                        }
                        items(weakChapters, key = { "weak_${it.id}" }) { chapter ->
                            RapidChapterCard(
                                chapter = chapter,
                                tag = "NEEDS ATTENTION",
                                tagColor = colors.warning,
                                onClick = { onOpenChapter(chapter.id) }
                            )
                        }
                    }
                }

                // Mistakes Book Section
                if (selectedFilter == LastMinuteFilter.ALL || selectedFilter == LastMinuteFilter.MISTAKES) {
                    if (unresolvedMistakes.isNotEmpty()) {
                        item {
                            SectionLabel(title = "❌ Unresolved Mistakes", count = unresolvedMistakes.size)
                        }
                        items(unresolvedMistakes, key = { "mis_${it.id}" }) { mistake ->
                            RapidMistakeCard(
                                mistake = mistake,
                                onResolve = { onResolveMistake(mistake.id) }
                            )
                        }
                    }
                }

                // Short Key Notes Section
                if (selectedFilter == LastMinuteFilter.ALL || selectedFilter == LastMinuteFilter.SHORT_NOTES) {
                    if (shortNotes.isNotEmpty()) {
                        item {
                            SectionLabel(title = "📝 Short Revision Notes", count = shortNotes.size)
                        }
                        items(shortNotes, key = { "note_${it.id}" }) { note ->
                            RapidNoteCard(note = note)
                        }
                    }
                }

                // Pending Revision Section
                if (selectedFilter == LastMinuteFilter.ALL || selectedFilter == LastMinuteFilter.PENDING_REVISION) {
                    if (pendingRevisions.isNotEmpty()) {
                        item {
                            SectionLabel(title = "🔄 Pending Revision Due", count = pendingRevisions.size)
                        }
                        items(pendingRevisions, key = { "rev_${it.id}" }) { chapter ->
                            RapidChapterCard(
                                chapter = chapter,
                                tag = "REVISION DUE",
                                tagColor = colors.success,
                                onClick = { onOpenChapter(chapter.id) }
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(title: String, count: Int) {
    val typography = StudyOSTheme.typography
    val colors = StudyOSTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = typography.caption.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
            color = colors.primaryText
        )
    }
}

@Composable
private fun RapidChapterCard(
    chapter: Chapter,
    tag: String,
    tagColor: Color,
    onClick: () -> Unit
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.small)
            .background(colors.surface)
            .border(1.dp, colors.border, shapes.small)
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = chapter.name,
                    style = typography.caption.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                    color = colors.primaryText
                )
                if (!chapter.description.isNullOrBlank()) {
                    Text(
                        text = chapter.description,
                        style = typography.caption.copy(fontSize = 11.sp),
                        color = colors.secondaryText,
                        maxLines = 1
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(shapes.pill)
                    .background(tagColor.copy(alpha = 0.12f))
                    .border(1.dp, tagColor.copy(alpha = 0.4f), shapes.pill)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = tag,
                    style = typography.caption.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                    color = tagColor
                )
            }
        }
    }
}

@Composable
private fun RapidMistakeCard(
    mistake: Mistake,
    onResolve: () -> Unit
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.small)
            .background(colors.surface)
            .border(1.dp, colors.border, shapes.small)
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = mistake.question,
                    style = typography.caption.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                    color = colors.primaryText,
                    modifier = Modifier.weight(1f)
                )

                TextButton(onClick = onResolve) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = "Mark as Mastered",
                        tint = colors.success,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(text = "Mastered", style = typography.caption.copy(fontSize = 11.sp), color = colors.success)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // What was wrong
            Text(
                text = "Wrong concept: ${mistake.studentAnswer}",
                style = typography.caption.copy(fontSize = 11.sp),
                color = colors.error
            )

            // Correct concept
            Text(
                text = "Correct concept: ${mistake.correctAnswer}",
                style = typography.caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                color = colors.success
            )
        }
    }
}

@Composable
private fun RapidNoteCard(note: Note) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.small)
            .background(colors.surface)
            .border(1.dp, colors.border, shapes.small)
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = note.title,
                style = typography.caption.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                color = colors.primaryText
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = note.content,
                style = typography.caption.copy(fontSize = 12.sp, lineHeight = 16.sp),
                color = colors.secondaryText
            )
        }
    }
}
