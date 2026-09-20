package com.studyos.app.features.progress.ui

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.studyos.app.core.ui.component.StudyOSLoadingState
import com.studyos.app.core.ui.component.StudyOSProgressBar
import com.studyos.app.domain.usecase.SubjectProgressBreakdown
import com.studyos.app.features.progress.viewmodel.ProgressViewModel
import com.studyos.app.theme.StudyOSTheme

@Composable
fun ProgressScreen(
    viewModel: ProgressViewModel,
    onSubjectClick: (String) -> Unit = {},
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
        if (uiState.isLoading) {
            StudyOSLoadingState(message = "Calculating progress...")
        } else {
            val progress = uiState.academicProgress

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 36.dp)
                    .widthIn(max = 560.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Progress",
                    style = typography.screenTitle,
                    color = colors.primaryText
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (progress.totalChapters == 0) {
                    Spacer(modifier = Modifier.height(32.dp))
                    StudyOSEmptyState(
                        title = "No progress yet.",
                        description = "Add chapters and update their progress to see your study coverage."
                    )
                } else {
                    // Overall Progress Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shapes.surface)
                            .background(colors.surface)
                            .border(1.dp, colors.border, shapes.surface)
                            .padding(20.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Overall progress",
                                style = typography.secondary,
                                color = colors.secondaryText
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Text(
                                    text = "${progress.overallProgress}%",
                                    style = typography.screenTitle,
                                    color = colors.primaryText
                                )

                                Text(
                                    text = "${progress.completedChapters} of ${progress.totalChapters} chapters completed",
                                    style = typography.caption,
                                    color = colors.secondaryText
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            StudyOSProgressBar(
                                progress = progress.overallProgress,
                                height = 6.dp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Subjects Breakdown
                    Text(
                        text = "Subjects",
                        style = typography.sectionTitle,
                        color = colors.primaryText
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(progress.subjectBreakdowns, key = { it.subjectId }) { breakdown ->
                            SubjectProgressRow(
                                breakdown = breakdown,
                                onClick = { onSubjectClick(breakdown.subjectId) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubjectProgressRow(
    breakdown: SubjectProgressBreakdown,
    onClick: () -> Unit
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.surface)
            .background(colors.surface)
            .border(1.dp, colors.border, shapes.surface)
            .clickable(onClick = onClick)
            .semantics { role = Role.Button }
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = breakdown.subjectName,
                    style = typography.bodyMedium,
                    color = colors.primaryText
                )

                Text(
                    text = "${breakdown.progress}%",
                    style = typography.bodyMedium,
                    color = colors.primaryText
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${breakdown.completedChapters} / ${breakdown.totalChapters} chapters completed",
                style = typography.caption,
                color = colors.secondaryText
            )

            Spacer(modifier = Modifier.height(8.dp))

            StudyOSProgressBar(
                progress = breakdown.progress,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
