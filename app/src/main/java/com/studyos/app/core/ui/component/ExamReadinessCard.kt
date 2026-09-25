package com.studyos.app.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
 * Transparent calculation breakdown for Exam Readiness.
 */
data class ExamReadinessData(
    val overallScore: Int,
    val syllabusPercentage: Int,
    val revisionPercentage: Int,
    val practicePercentage: Int,
    val confidencePercentage: Int,
    val explanatoryNote: String? = null
)

/**
 * Feature 4: EXAM READINESS INDICATOR
 * Based transparently on:
 * - Syllabus completion (35%)
 * - Revision completion (30%)
 * - Practice completion (25%)
 * - Self-confidence (10%)
 */
@Composable
fun ExamReadinessCard(
    data: ExamReadinessData,
    modifier: Modifier = Modifier
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    val scoreColor = when {
        data.overallScore >= 75 -> colors.success
        data.overallScore >= 50 -> colors.accent
        else -> colors.secondaryText
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shapes.surface)
            .background(colors.surface)
            .border(1.dp, colors.border, shapes.surface)
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Exam Readiness Indicator",
                        style = typography.caption.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                        color = colors.primaryText
                    )
                    Text(
                        text = "Calculated from 4 measurable components",
                        style = typography.caption.copy(fontSize = 11.sp),
                        color = colors.mutedText
                    )
                }

                // Big Score Pill
                Box(
                    modifier = Modifier
                        .clip(shapes.pill)
                        .background(scoreColor.copy(alpha = 0.12f))
                        .border(1.dp, scoreColor.copy(alpha = 0.4f), shapes.pill)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${data.overallScore}%",
                        style = typography.caption.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                        color = scoreColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4 Components Breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ReadinessPillar(
                    title = "Syllabus",
                    percentage = data.syllabusPercentage,
                    weight = "35%",
                    color = colors.accent
                )
                ReadinessPillar(
                    title = "Revision",
                    percentage = data.revisionPercentage,
                    weight = "30%",
                    color = colors.success
                )
                ReadinessPillar(
                    title = "Practice",
                    percentage = data.practicePercentage,
                    weight = "25%",
                    color = Color(0xFF2563EB)
                )
                ReadinessPillar(
                    title = "Confidence",
                    percentage = data.confidencePercentage,
                    weight = "10%",
                    color = colors.secondaryText
                )
            }

            if (!data.explanatoryNote.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shapes.small)
                        .background(colors.cardBackground)
                        .padding(10.dp)
                ) {
                    Text(
                        text = data.explanatoryNote,
                        style = typography.caption.copy(fontSize = 11.sp, lineHeight = 16.sp),
                        color = colors.secondaryText
                    )
                }
            }
        }
    }
}

@Composable
private fun ReadinessPillar(
    title: String,
    percentage: Int,
    weight: String,
    color: Color
) {
    val typography = StudyOSTheme.typography
    val colors = StudyOSTheme.colors

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Text(
            text = "$percentage%",
            style = typography.caption.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
            color = color
        )
        Text(
            text = title,
            style = typography.caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
            color = colors.primaryText
        )
        Text(
            text = weight,
            style = typography.caption.copy(fontSize = 9.sp),
            color = colors.mutedText
        )
    }
}
