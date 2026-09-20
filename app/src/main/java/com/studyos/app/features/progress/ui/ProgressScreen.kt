package com.studyos.app.features.progress.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.studyos.app.core.ui.component.GlassCard
import com.studyos.app.core.ui.component.GlassTopBar
import com.studyos.app.core.ui.component.ShimmerPlaceholder
import com.studyos.app.core.ui.component.StudyOSEmptyState
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
    val spacing = StudyOSTheme.spacing

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            GlassTopBar(
                title = "Progress",
                subtitle = if (uiState.academicProgress.totalChapters > 0) {
                    "${uiState.academicProgress.completedChapters} of ${uiState.academicProgress.totalChapters} completed"
                } else null
            )

            if (uiState.isLoading) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = spacing.screenHorizontal, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    ShimmerPlaceholder(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        shape = shapes.medium
                    )
                    repeat(3) {
                        ShimmerPlaceholder(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp),
                            shape = shapes.medium
                        )
                    }
                }
            } else {
                val progress = uiState.academicProgress

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = spacing.screenHorizontal)
                        .widthIn(max = 560.dp),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.Start
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    if (progress.totalChapters == 0) {
                        Spacer(modifier = Modifier.height(32.dp))
                        StudyOSEmptyState(
                            title = "No progress yet",
                            description = "Add chapters and update their progress to see your study coverage."
                        )
                    } else {
                        // Animated overall progress percentage (400-600ms)
                        val animatedProgress by animateIntAsState(
                            targetValue = progress.overallProgress,
                            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
                            label = "overallProgressAnimation"
                        )

                        // Overall Progress Card
                        GlassCard(
                            backgroundColor = colors.glassSurface,
                            padding = 18.dp
                        ) {
                            Text(
                                text = "Overall progress",
                                style = typography.caption.copy(fontWeight = FontWeight.SemiBold),
                                color = colors.secondaryText
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Text(
                                    text = "$animatedProgress%",
                                    style = typography.screenTitle,
                                    color = colors.primaryText
                                )

                                Text(
                                    text = "${progress.completedChapters} of ${progress.totalChapters} chapters",
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

                        Spacer(modifier = Modifier.height(24.dp))

                        // Subjects Breakdown Section Header
                        Text(
                            text = "Subjects",
                            style = typography.sectionTitle.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.primaryText
                        )

                        Spacer(modifier = Modifier.height(12.dp))

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
}

@Composable
private fun SubjectProgressRow(
    breakdown: SubjectProgressBreakdown,
    onClick: () -> Unit
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography

    GlassCard(
        onClick = onClick,
        backgroundColor = colors.glassSurface,
        padding = 14.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = breakdown.subjectName,
                style = typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = colors.primaryText
            )

            Text(
                text = "${breakdown.progress}%",
                style = typography.caption.copy(fontWeight = FontWeight.SemiBold),
                color = if (breakdown.progress == 100) colors.success else colors.accent
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "${breakdown.completedChapters} of ${breakdown.totalChapters} chapters completed",
            style = typography.caption,
            color = colors.secondaryText
        )

        Spacer(modifier = Modifier.height(8.dp))

        StudyOSProgressBar(
            progress = breakdown.progress,
            height = 4.dp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
