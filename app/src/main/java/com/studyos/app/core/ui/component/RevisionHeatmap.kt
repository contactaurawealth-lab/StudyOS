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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studyos.app.theme.StudyOSTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Activity log entry for a specific day.
 */
data class DayActivity(
    val date: LocalDate,
    val hasStudy: Boolean = false,
    val hasRevision: Boolean = false,
    val hasPractice: Boolean = false
)

/**
 * Feature 8: REVISION HEATMAP
 * Minimal calendar heatmap showing:
 * - Study days (Amber)
 * - Revision days (Green)
 * - Practice days (Blue)
 */
@Composable
fun RevisionHeatmap(
    modifier: Modifier = Modifier,
    activityMap: Map<LocalDate, DayActivity> = emptyMap(),
    daysCount: Int = 28
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    val today = LocalDate.now()
    // Align to start of week (Monday)
    val startDate = today.minusDays((daysCount - 1).toLong())

    // Group dates into weeks (7 days each)
    val days = (0 until daysCount).map { startDate.plusDays(it.toLong()) }
    val weeks = days.chunked(7)
    val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")

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
                Text(
                    text = "Activity & Revision Heatmap",
                    style = typography.caption.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                    color = colors.primaryText
                )
                Text(
                    text = "Last $daysCount days",
                    style = typography.caption.copy(fontSize = 11.sp),
                    color = colors.mutedText
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Day headers (M T W T F S S)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                dayLabels.forEach { label ->
                    Text(
                        text = label,
                        modifier = Modifier.weight(1f),
                        style = typography.caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                        color = colors.mutedText,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Grid of days
            weeks.forEach { week ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    week.forEach { date ->
                        val isToday = date == today
                        val activity = activityMap[date] ?: DayActivity(date)
                        val hasAny = activity.hasStudy || activity.hasRevision || activity.hasPractice

                        val cellBg = when {
                            isToday -> colors.cardBackground
                            hasAny -> colors.background
                            else -> colors.surface
                        }

                        val cellBorder = when {
                            isToday -> colors.accent
                            else -> colors.border.copy(alpha = 0.6f)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .padding(horizontal = 2.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(cellBg)
                                .border(1.dp, cellBorder, RoundedCornerShape(6.dp))
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = date.dayOfMonth.toString(),
                                    style = typography.caption.copy(
                                        fontSize = 10.sp,
                                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (isToday) colors.accent else colors.secondaryText
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (activity.hasStudy) {
                                        Box(
                                            modifier = Modifier
                                                .size(4.dp)
                                                .clip(CircleShape)
                                                .background(colors.accent)
                                        )
                                    }
                                    if (activity.hasRevision) {
                                        Box(
                                            modifier = Modifier
                                                .size(4.dp)
                                                .clip(CircleShape)
                                                .background(colors.success)
                                        )
                                    }
                                    if (activity.hasPractice) {
                                        Box(
                                            modifier = Modifier
                                                .size(4.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF2563EB))
                                        )
                                    }
                                    if (!hasAny) {
                                        Box(
                                            modifier = Modifier
                                                .size(4.dp)
                                                .clip(CircleShape)
                                                .background(Color.Transparent)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Minimal Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                HeatmapLegendItem(color = colors.accent, label = "Study")
                Spacer(modifier = Modifier.size(16.dp))
                HeatmapLegendItem(color = colors.success, label = "Revision")
                Spacer(modifier = Modifier.size(16.dp))
                HeatmapLegendItem(color = Color(0xFF2563EB), label = "Practice")
            }
        }
    }
}

@Composable
private fun HeatmapLegendItem(color: Color, label: String) {
    val typography = StudyOSTheme.typography
    val colors = StudyOSTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = typography.caption.copy(fontSize = 10.sp),
            color = colors.mutedText
        )
    }
}

/**
 * Helper to calculate activity map from timestamps.
 */
fun buildActivityMap(
    studyTimestamps: List<Long>,
    revisionTimestamps: List<Long>,
    practiceTimestamps: List<Long>,
    zoneId: ZoneId = ZoneId.systemDefault()
): Map<LocalDate, DayActivity> {
    val map = mutableMapOf<LocalDate, DayActivity>()

    fun toDate(ts: Long): LocalDate =
        Instant.ofEpochMilli(ts).atZone(zoneId).toLocalDate()

    studyTimestamps.forEach { ts ->
        val date = toDate(ts)
        val existing = map[date] ?: DayActivity(date)
        map[date] = existing.copy(hasStudy = true)
    }

    revisionTimestamps.forEach { ts ->
        val date = toDate(ts)
        val existing = map[date] ?: DayActivity(date)
        map[date] = existing.copy(hasRevision = true)
    }

    practiceTimestamps.forEach { ts ->
        val date = toDate(ts)
        val existing = map[date] ?: DayActivity(date)
        map[date] = existing.copy(hasPractice = true)
    }

    return map
}
