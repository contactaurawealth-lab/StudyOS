package com.studyos.app.features.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShowChart
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
    onNavigateToSearch: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToProgress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 36.dp)
                .widthIn(max = 560.dp),
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
