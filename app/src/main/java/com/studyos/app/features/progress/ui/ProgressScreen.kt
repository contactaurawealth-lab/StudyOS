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
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.studyos.app.core.ui.component.GlassCard
import com.studyos.app.core.ui.component.GlassTopBar
import com.studyos.app.core.ui.component.ShimmerPlaceholder
import com.studyos.app.core.ui.component.StudyOSEmptyState
import com.studyos.app.core.ui.component.StudyOSProgressBar
import com.studyos.app.domain.usecase.SubjectProgressBreakdown
import com.studyos.app.features.progress.viewmodel.ProgressTimeWindow
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

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                if (uiState.isLoading) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 680.dp)
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
                            .widthIn(max = 680.dp)
                            .padding(horizontal = spacing.screenHorizontal),
                        verticalArrangement = Arrangement.Top,
                        horizontalAlignment = Alignment.Start
                    ) {
                        Spacer(modifier = Modifier.height(14.dp))

                        // 1. Time Window Filter Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ProgressTimeWindow.values().forEach { window ->
                                val isSelected = uiState.selectedTimeWindow == window
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(shapes.statusPill)
                                        .background(if (isSelected) colors.primaryText else colors.surface)
                                        .border(1.dp, if (isSelected) colors.primaryText else colors.border, shapes.statusPill)
                                        .clickable { viewModel.setTimeWindowFilter(window) }
                                        .padding(vertical = 6.dp)
                                        .semantics { this.role = Role.Tab },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = window.label,
                                        style = typography.caption.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 12.sp
                                        ),
                                        color = if (isSelected) colors.buttonText else colors.secondaryText
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        if (progress.totalChapters == 0) {
                            Spacer(modifier = Modifier.height(32.dp))
                            StudyOSEmptyState(
                                title = "No progress yet",
                                description = "Add chapters and update their progress to see your study coverage."
                            )
                        } else {
                            // 2. Cognitive Readiness Index Card
                            val readiness = uiState.cognitiveReadiness
                            GlassCard(
                                backgroundColor = colors.glassSurface,
                                padding = 18.dp
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "COGNITIVE READINESS",
                                            style = typography.caption.copy(fontWeight = FontWeight.Bold),
                                            color = colors.accent,
                                            letterSpacing = 1.sp
                                        )

                                        Box(
                                            modifier = Modifier
                                                .clip(shapes.statusPill)
                                                .background(colors.accent.copy(alpha = 0.15f))
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = readiness.readinessTier,
                                                style = typography.caption.copy(fontWeight = FontWeight.Bold),
                                                color = colors.accent
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Bottom
                                    ) {
                                        Text(
                                            text = "${readiness.overallIndex}%",
                                            style = typography.screenTitle,
                                            color = colors.primaryText
                                        )

                                        val hours = uiState.filteredStudyMinutes / 60
                                        val mins = uiState.filteredStudyMinutes % 60
                                        val timeStr = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
                                        Text(
                                            text = "$timeStr • ${uiState.activeDaysCount} active days",
                                            style = typography.caption,
                                            color = colors.secondaryText
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    StudyOSProgressBar(
                                        progress = readiness.overallIndex,
                                        height = 6.dp,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // 4 Breakdown Quadrants
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        ReadinessMetricTile(
                                            label = "Syllabus",
                                            value = "${readiness.syllabusCompletionPct}%",
                                            weight = "35%",
                                            modifier = Modifier.weight(1f)
                                        )
                                        ReadinessMetricTile(
                                            label = "Retention",
                                            value = "${readiness.recallRetentionPct}%",
                                            weight = "25%",
                                            modifier = Modifier.weight(1f)
                                        )
                                        ReadinessMetricTile(
                                            label = "Accuracy",
                                            value = "${readiness.quizAccuracyPct}%",
                                            weight = "20%",
                                            modifier = Modifier.weight(1f)
                                        )
                                        ReadinessMetricTile(
                                            label = "Habit",
                                            value = "${readiness.consistencyPct}%",
                                            weight = "20%",
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

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

                            Spacer(modifier = Modifier.height(12.dp))

                            // Consistency & Retention Momentum Card
                            GlassCard(
                                backgroundColor = colors.glassSurface,
                                padding = 16.dp
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "Consistency & Momentum",
                                        style = typography.caption.copy(fontWeight = FontWeight.SemiBold),
                                        color = colors.accent
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (progress.completedChapters > 0) "Active Learning Pace" else "Start Your Habit",
                                        style = typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = colors.primaryText
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Non-punitive consistency: daily retrieval and incremental progress compound into long-term recall without penalty for missed days.",
                                        style = typography.caption,
                                        color = colors.secondaryText
                                    )
                                }
                            }

                            // Weekly Study Report Card
                            val report = uiState.weeklyReport
                            if (report != null && report.totalMinutes > 0) {
                                val context = androidx.compose.ui.platform.LocalContext.current
                                Spacer(modifier = Modifier.height(12.dp))
                                GlassCard(
                                    backgroundColor = colors.glassSurface,
                                    padding = 16.dp
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "WEEKLY STUDY REPORT",
                                                style = typography.caption.copy(fontWeight = FontWeight.Bold),
                                                color = colors.accent,
                                                letterSpacing = 1.sp
                                            )

                                            Text(
                                                text = "Share",
                                                style = typography.caption.copy(fontWeight = FontWeight.SemiBold),
                                                color = colors.accent,
                                                modifier = Modifier.clickable {
                                                    val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                        type = "text/plain"
                                                        putExtra(android.content.Intent.EXTRA_TEXT, report.shareableText)
                                                    }
                                                    context.startActivity(android.content.Intent.createChooser(sendIntent, "Share Weekly Report"))
                                                }
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Text(
                                            text = report.headline,
                                            style = typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = colors.primaryText
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                val hours = report.totalMinutes / 60
                                                val mins = report.totalMinutes % 60
                                                Text(
                                                    text = if (hours > 0) "${hours}h ${mins}m" else "${mins}m",
                                                    style = typography.sectionTitle,
                                                    color = colors.primaryText
                                                )
                                                Text(
                                                    text = "Focused Study",
                                                    style = typography.caption,
                                                    color = colors.secondaryText
                                                )
                                            }

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "${report.activeDaysCount}/7 Days",
                                                    style = typography.sectionTitle,
                                                    color = colors.primaryText
                                                )
                                                Text(
                                                    text = "Consistency",
                                                    style = typography.caption,
                                                    color = colors.secondaryText
                                                )
                                            }

                                            if (report.mistakesResolved > 0) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "${report.mistakesResolved}",
                                                        style = typography.sectionTitle,
                                                        color = colors.accent
                                                    )
                                                    Text(
                                                        text = "Mistakes Fixed",
                                                        style = typography.caption,
                                                        color = colors.secondaryText
                                                    )
                                                }
                                            }
                                        }

                                        if (report.subjectBreakdown.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text(
                                                text = "Top Subjects: " + report.subjectBreakdown.take(3).joinToString(", ") { "${it.first} (${it.second / 60}h)" },
                                                style = typography.caption,
                                                color = colors.mutedText
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

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

@Composable
private fun ReadinessMetricTile(
    label: String,
    value: String,
    weight: String,
    modifier: Modifier = Modifier
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    Box(
        modifier = modifier
            .clip(shapes.small)
            .background(colors.surface.copy(alpha = 0.6f))
            .padding(vertical = 8.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                style = typography.caption.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                color = colors.primaryText
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = typography.caption.copy(fontSize = 10.sp),
                color = colors.secondaryText
            )
            Text(
                text = weight,
                style = typography.caption.copy(fontSize = 9.sp),
                color = colors.mutedText
            )
        }
    }
}
