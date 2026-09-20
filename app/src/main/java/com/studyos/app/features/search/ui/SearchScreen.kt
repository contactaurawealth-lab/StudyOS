package com.studyos.app.features.search.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.MenuBook
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
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .widthIn(max = 600.dp)
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

            Spacer(modifier = Modifier.height(24.dp))

            if (!uiState.hasSearched) {
                Text(
                    text = "Search subjects and chapters.",
                    style = typography.secondary,
                    color = colors.mutedText,
                    modifier = Modifier.padding(start = 8.dp)
                )
            } else if (uiState.subjectResults.isEmpty() && uiState.chapterResults.isEmpty()) {
                StudyOSEmptyState(
                    title = "No results",
                    description = "Nothing matched \"${uiState.query}\"."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (uiState.subjectResults.isNotEmpty()) {
                        item {
                            Text(
                                text = "Subjects (${uiState.subjectResults.size})",
                                style = typography.secondary,
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
                                        imageVector = Icons.Outlined.MenuBook,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = colors.secondaryText
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

                    if (uiState.chapterResults.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Chapters (${uiState.chapterResults.size})",
                                style = typography.secondary,
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
                                            tint = colors.secondaryText
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
                }
            }
        }
    }
}
