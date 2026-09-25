package com.studyos.app.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studyos.app.theme.StudyOSTheme

/**
 * Syllabus breakdown statistics for a subject.
 */
data class SubjectSyllabusStats(
    val subjectId: String,
    val subjectName: String,
    val totalChapters: Int,
    val completedCount: Int,
    val inProgressCount: Int,
    val notStartedCount: Int,
    val completionPercentage: Int
)

/**
 * Feature 2: SYLLABUS COMPLETION TRACKER
 * Shows:
 * - Total chapters
 * - Completed
 * - In progress
 * - Not started
 * - Percentage completion
 */
@Composable
fun SyllabusTrackerCard(
    stats: SubjectSyllabusStats,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shapes.surface)
            .background(colors.surface)
            .border(1.dp, colors.border, shapes.surface)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stats.subjectName,
                    style = typography.sectionTitle.copy(fontSize = 16.sp),
                    color = colors.primaryText
                )
                Text(
                    text = "${stats.completionPercentage}% Completed",
                    style = typography.caption.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                    color = if (stats.completionPercentage >= 80) colors.success else colors.accent
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Multi-segment syllabus progress bar
            SyllabusProgressBar(
                total = stats.totalChapters,
                completed = stats.completedCount,
                inProgress = stats.inProgressCount,
                notStarted = stats.notStartedCount
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Metrics row: Total chapters, Completed, In progress, Not started
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SyllabusMetricChip(label = "Total", value = "${stats.totalChapters}")
                SyllabusMetricChip(label = "Completed", value = "${stats.completedCount}", tint = colors.success)
                SyllabusMetricChip(label = "In Progress", value = "${stats.inProgressCount}", tint = colors.accent)
                SyllabusMetricChip(label = "Not Started", value = "${stats.notStartedCount}", tint = colors.mutedText)
            }
        }
    }
}

@Composable
fun SyllabusProgressBar(
    total: Int,
    completed: Int,
    inProgress: Int,
    notStarted: Int,
    modifier: Modifier = Modifier
) {
    val colors = StudyOSTheme.colors
    val completedWeight = if (total > 0) completed.toFloat() / total else 0f
    val inProgressWeight = if (total > 0) inProgress.toFloat() / total else 0f
    val notStartedWeight = if (total > 0) notStarted.toFloat() / total else 1f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(colors.cardBackground)
    ) {
        Row(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
            if (completedWeight > 0f) {
                Box(
                    modifier = Modifier
                        .weight(completedWeight)
                        .fillMaxHeight()
                        .background(colors.success)
                )
            }
            if (inProgressWeight > 0f) {
                Box(
                    modifier = Modifier
                        .weight(inProgressWeight)
                        .fillMaxHeight()
                        .background(colors.accent)
                )
            }
            if (notStartedWeight > 0f) {
                Box(
                    modifier = Modifier
                        .weight(notStartedWeight)
                        .fillMaxHeight()
                        .background(colors.border)
                )
            }
        }
    }
}

@Composable
private fun SyllabusMetricChip(
    label: String,
    value: String,
    tint: Color = StudyOSTheme.colors.primaryText
) {
    val typography = StudyOSTheme.typography
    val colors = StudyOSTheme.colors

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = typography.caption.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
            color = tint
        )
        Text(
            text = label,
            style = typography.caption.copy(fontSize = 10.sp),
            color = colors.mutedText
        )
    }
}
