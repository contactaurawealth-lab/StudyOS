package com.studyos.app.features.planner.ui

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.studyos.app.core.ui.component.StudyOSIconButton
import com.studyos.app.core.ui.component.StudyOSOutlinedButton
import com.studyos.app.core.util.DateTimeUtils
import com.studyos.app.domain.model.CalendarDay
import com.studyos.app.theme.StudyOSTheme
import java.time.LocalDate

@Composable
fun WeekCalendarRow(
    selectedDate: LocalDate,
    weekDays: List<CalendarDay>,
    onSelectDate: (LocalDate) -> Unit,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onTodayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shapes.surface)
            .background(colors.surface)
            .border(1.dp, colors.border, shapes.surface)
            .padding(16.dp)
    ) {
        // Month/Year header and navigation
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = DateTimeUtils.formatMonthYear(selectedDate),
                style = typography.subsectionTitle,
                color = colors.primaryText
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                StudyOSOutlinedButton(
                    text = "Today",
                    onClick = onTodayClick,
                    modifier = Modifier.height(34.dp)
                )

                Spacer(modifier = Modifier.width(6.dp))

                StudyOSIconButton(
                    onClick = onPreviousWeek,
                    contentDescription = "Previous week",
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = null,
                        tint = colors.primaryText,
                        modifier = Modifier.size(18.dp)
                    )
                }

                StudyOSIconButton(
                    onClick = onNextWeek,
                    contentDescription = "Next week",
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = null,
                        tint = colors.primaryText,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Weekday pills row (Mon .. Sun)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            weekDays.forEach { calendarDay ->
                val isSelected = calendarDay.isSelected
                val isToday = calendarDay.isToday

                val itemBg = when {
                    isSelected -> colors.primaryText
                    else -> colors.surface
                }
                val itemBorder = when {
                    isSelected -> colors.primaryText
                    isToday -> colors.accent
                    else -> colors.border
                }
                val textColor = when {
                    isSelected -> colors.background
                    isToday -> colors.accent
                    else -> colors.primaryText
                }
                val subTextColor = when {
                    isSelected -> colors.background.copy(alpha = 0.8f)
                    isToday -> colors.accent
                    else -> colors.secondaryText
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 2.dp)
                        .clip(shapes.button)
                        .background(itemBg)
                        .border(1.dp, itemBorder, shapes.button)
                        .clickable { onSelectDate(calendarDay.date) }
                        .padding(vertical = 8.dp)
                        .semantics { this.role = Role.Button },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = DateTimeUtils.formatDayOfWeekShort(calendarDay.date),
                        style = typography.caption,
                        color = subTextColor
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = DateTimeUtils.formatDayNumber(calendarDay.date),
                        style = if (isSelected) typography.bodyMedium else typography.body,
                        color = textColor
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Session dot indicator
                    if (calendarDay.sessionCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(shapes.pill)
                                .background(if (isSelected) colors.background else colors.accent)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}
