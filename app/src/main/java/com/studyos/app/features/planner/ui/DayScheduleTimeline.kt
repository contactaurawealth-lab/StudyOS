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
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
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
import com.studyos.app.core.ui.component.StudyOSButton
import com.studyos.app.core.ui.component.StudyOSDivider
import com.studyos.app.core.ui.component.StudyOSIconButton
import com.studyos.app.core.util.DateTimeUtils
import com.studyos.app.domain.model.StudySession
import com.studyos.app.domain.model.StudySessionItem
import com.studyos.app.theme.StudyOSTheme
import java.time.LocalDate

@Composable
fun DayScheduleTimeline(
    selectedDate: LocalDate,
    sessions: List<StudySessionItem>,
    onAddSession: () -> Unit,
    onEditSession: (StudySession) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    Column(modifier = modifier.fillMaxWidth()) {
        // Date Title and Add action
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = DateTimeUtils.formatDateHeader(selectedDate),
                    style = typography.sectionTitle,
                    color = colors.primaryText
                )
                Text(
                    text = if (sessions.isNotEmpty()) "${sessions.size} scheduled" else "No sessions",
                    style = typography.secondary,
                    color = colors.secondaryText,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            StudyOSIconButton(
                onClick = onAddSession,
                contentDescription = "Add study session"
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null,
                    tint = colors.primaryText
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (sessions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shapes.surface)
                    .background(colors.surface)
                    .border(1.dp, colors.border, shapes.surface)
                    .padding(vertical = 36.dp, horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Nothing planned.",
                        style = typography.subsectionTitle,
                        color = colors.primaryText
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Add a study session to organize your day.",
                        style = typography.body,
                        color = colors.secondaryText
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    StudyOSButton(
                        text = "Add session",
                        onClick = onAddSession
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shapes.surface)
                    .background(colors.surface)
                    .border(1.dp, colors.border, shapes.surface)
            ) {
                sessions.forEachIndexed { index, sessionItem ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEditSession(sessionItem.session) }
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                            .semantics { this.role = Role.Button },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left time column
                        Column(
                            modifier = Modifier.width(72.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = sessionItem.formattedTime,
                                style = typography.bodyMedium,
                                color = colors.primaryText
                            )
                            Text(
                                text = "${sessionItem.session.plannedMinutes} min",
                                style = typography.caption,
                                color = colors.mutedText,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        // Divider line
                        Box(
                            modifier = Modifier
                                .height(36.dp)
                                .width(1.dp)
                                .background(colors.border)
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        // Subject and Chapter
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = sessionItem.subjectName,
                                style = typography.bodyMedium,
                                color = colors.primaryText
                            )

                            if (!sessionItem.chapterName.isNullOrBlank()) {
                                Text(
                                    text = sessionItem.chapterName,
                                    style = typography.secondary,
                                    color = colors.secondaryText,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }

                    if (index < sessions.lastIndex) {
                        StudyOSDivider()
                    }
                }
            }
        }
    }
}
