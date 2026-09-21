package com.studyos.app.features.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.studyos.app.core.ui.component.StudyOSDivider
import com.studyos.app.core.ui.component.StudyOSListItem
import com.studyos.app.theme.StudyOSTheme

@Composable
fun MoreScreen(
    onNavigateToTasks: () -> Unit = {},
    onNavigateToSearch: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToProgress: () -> Unit,
    onNavigateToAi: () -> Unit = {},
    onNavigateToExams: () -> Unit = {},
    onNavigateToMistakes: () -> Unit = {},
    onNavigateToRevision: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 36.dp)
                .widthIn(max = 680.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "More",
                style = typography.screenTitle,
                color = colors.primaryText
            )

            Spacer(modifier = Modifier.height(32.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shapes.surface)
                    .background(colors.surface)
                    .border(1.dp, colors.border, shapes.surface)
            ) {
                Column {
                    StudyOSListItem(
                        title = "AI Assistant",
                        subtitle = "Study partner, explanations, practice quizzes",
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Outlined.Psychology,
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingContent = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                                contentDescription = null,
                                tint = colors.mutedText,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        onClick = onNavigateToAi
                    )

                    StudyOSDivider()

                    StudyOSListItem(
                        title = "Tasks",
                        subtitle = "To-dos, homework, and assignments",
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Outlined.Checklist,
                                contentDescription = null,
                                tint = colors.secondaryText,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingContent = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                                contentDescription = null,
                                tint = colors.mutedText,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        onClick = onNavigateToTasks
                    )

                    StudyOSDivider()

                    StudyOSListItem(
                        title = "Exams & Revision",
                        subtitle = "Exam countdowns, multi-subject readiness, revision plans",
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Outlined.School,
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingContent = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                                contentDescription = null,
                                tint = colors.mutedText,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        onClick = onNavigateToExams
                    )

                    StudyOSDivider()

                    StudyOSListItem(
                        title = "Revision & Active Recall",
                        subtitle = "Spaced repetition schedules, recall sessions & chapter mastery",
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Outlined.Psychology,
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingContent = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                                contentDescription = null,
                                tint = colors.mutedText,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        onClick = onNavigateToRevision
                    )

                    StudyOSDivider()

                    StudyOSListItem(
                        title = "Mistake Bank",
                        subtitle = "Review incorrect quiz questions & build flashcards",
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Outlined.ErrorOutline,
                                contentDescription = null,
                                tint = colors.secondaryText,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingContent = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                                contentDescription = null,
                                tint = colors.mutedText,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        onClick = onNavigateToMistakes
                    )

                    StudyOSDivider()

                    StudyOSListItem(
                        title = "Search",
                        subtitle = "Find subjects, chapters, notes",
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = null,
                                tint = colors.secondaryText,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingContent = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                                contentDescription = null,
                                tint = colors.mutedText,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        onClick = onNavigateToSearch
                    )

                    StudyOSDivider()

                    StudyOSListItem(
                        title = "Progress",
                        subtitle = "Review your academic study history",
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Outlined.ShowChart,
                                contentDescription = null,
                                tint = colors.secondaryText,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingContent = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                                contentDescription = null,
                                tint = colors.mutedText,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        onClick = onNavigateToProgress
                    )

                    StudyOSDivider()

                    StudyOSListItem(
                        title = "Settings",
                        subtitle = "Appearance, profile, preferences",
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = null,
                                tint = colors.secondaryText,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingContent = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                                contentDescription = null,
                                tint = colors.mutedText,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        onClick = onNavigateToSettings
                    )
                }
            }
        }
    }
}
