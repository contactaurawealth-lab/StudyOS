package com.studyos.app.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studyos.app.domain.model.Exam
import com.studyos.app.theme.StudyOSTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Feature 1: EXAM COUNTDOWN
 * Displays "23 days until Mathematics"
 * Allows multiple exams with clean styling.
 */
@Composable
fun ExamCountdownCard(
    exam: Exam,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenExamDayMode: (() -> Unit)? = null
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    val daysLeft = exam.getDaysRemaining()
    val isUrgent = daysLeft in 0..7
    val isToday = daysLeft == 0L
    val isPast = daysLeft < 0L

    val headlineText = when {
        isToday -> "Today is ${exam.name}"
        daysLeft == 1L -> "1 day until ${exam.name}"
        daysLeft > 1L -> "$daysLeft days until ${exam.name}"
        else -> "${exam.name} completed"
    }

    val dateFormat = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(exam.targetDate))

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shapes.surface)
            .background(colors.surface)
            .border(
                width = if (isUrgent && !isPast) 1.5.dp else 1.dp,
                color = if (isUrgent && !isPast) colors.accent else colors.border,
                shape = shapes.surface
            )
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Countdown Badge
                Box(
                    modifier = Modifier
                        .clip(shapes.pill)
                        .background(
                            when {
                                isPast -> colors.cardBackground
                                isUrgent -> colors.accent.copy(alpha = 0.16f)
                                else -> colors.surface
                            }
                        )
                        .border(
                            1.dp,
                            when {
                                isPast -> colors.border
                                isUrgent -> colors.accent
                                else -> colors.border
                            },
                            shapes.pill
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = when {
                            isToday -> "TODAY"
                            daysLeft == 1L -> "1 DAY"
                            daysLeft > 1L -> "$daysLeft DAYS"
                            else -> "COMPLETED"
                        },
                        style = typography.caption.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = when {
                            isPast -> colors.mutedText
                            isUrgent -> colors.accent
                            else -> colors.primaryText
                        }
                    )
                }

                if (exam.targetScore != null) {
                    Text(
                        text = "Target: ${exam.targetScore}%",
                        style = typography.caption.copy(fontSize = 11.sp),
                        color = colors.mutedText
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main headline: "23 days until Mathematics"
            Text(
                text = headlineText,
                style = typography.sectionTitle.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
                color = colors.primaryText
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarToday,
                        contentDescription = null,
                        tint = colors.mutedText,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = formattedDate,
                        style = typography.caption.copy(fontSize = 12.sp),
                        color = colors.secondaryText
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (onOpenExamDayMode != null && daysLeft in 0..14) {
                        Box(
                            modifier = Modifier
                                .clip(shapes.pill)
                                .background(colors.accent.copy(alpha = 0.12f))
                                .border(1.dp, colors.accent.copy(alpha = 0.4f), shapes.pill)
                                .clickable(onClick = onOpenExamDayMode)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Exam Day Mode",
                                style = typography.caption.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                                color = colors.accent
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = "View Exam",
                        tint = colors.mutedText,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
