package com.studyos.app.features.search.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.studyos.app.core.ui.component.StudyOSEmptyState
import com.studyos.app.core.ui.component.StudyOSIconButton
import com.studyos.app.core.ui.component.StudyOSSearchField
import com.studyos.app.features.search.viewmodel.SearchViewModel
import com.studyos.app.theme.StudyOSTheme

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onBack: () -> Unit,
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
                    text = "Search subjects, notes, and study resources.",
                    style = typography.secondary,
                    color = colors.mutedText,
                    modifier = Modifier.padding(start = 8.dp)
                )
            } else if (uiState.subjectResults.isEmpty()) {
                StudyOSEmptyState(
                    title = "No results",
                    description = "Nothing matched \"${uiState.query}\"."
                )
            } else {
                Text(
                    text = "Subjects (${uiState.subjectResults.size})",
                    style = typography.secondary,
                    color = colors.secondaryText,
                    modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
                )

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.subjectResults, key = { it.id }) { subject ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(shapes.surface)
                                .background(colors.surface)
                                .border(1.dp, colors.border, shapes.surface)
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
            }
        }
    }
}
