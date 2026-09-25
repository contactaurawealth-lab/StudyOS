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
import com.studyos.app.domain.model.StudySession
import com.studyos.app.domain.usecase.UpcomingScheduleGroup
import com.studyos.app.theme.StudyOSTheme

@Composable
fun ListScheduleView(
    groups: List<UpcomingScheduleGroup>,
    onAddSession: () -> Unit,
    onEditSession: (StudySession) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    if (groups.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(shapes.surface)
                .background(colors.surface)
                .border(1.dp, colors.border, shapes.surface)
                .padding(vertical = 40.dp, horizontal = 24.dp),
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
                    text = "Add a study session to organize your upcoming days.",
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
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            groups.forEach { group ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = group.title,
                        style = typography.subsectionTitle,
                        color = colors.primaryText,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shapes.surface)
                            .background(colors.surface)
                            .border(1.dp, colors.border, shapes.surface)
                    ) {
                        group.sessions.forEachIndexed { index, sessionItem ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onEditSession(sessionItem.session) }
                                    .padding(horizontal = 20.dp, vertical = 14.dp)
                                    .semantics { this.role = Role.Button },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
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

                                Box(
                                    modifier = Modifier
                                        .height(36.dp)
                                        .width(1.dp)
                                        .background(colors.border)
                                )

                                Spacer(modifier = Modifier.width(16.dp))

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

                            if (index < group.sessions.lastIndex) {
                                StudyOSDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}
