package com.studyos.app.features.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.studyos.app.core.model.AppTheme
import com.studyos.app.core.ui.component.StudyOSDivider
import com.studyos.app.core.ui.component.StudyOSIconButton
import com.studyos.app.core.ui.component.StudyOSListItem
import com.studyos.app.core.ui.component.StudyOSSectionHeader
import com.studyos.app.features.settings.viewmodel.SettingsViewModel
import com.studyos.app.theme.StudyOSTheme

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToProfile: () -> Unit,
    onNavigateToSubjects: () -> Unit,
    onNavigateToPreferences: () -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.notificationMessage) {
        uiState.notificationMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearNotificationMessage()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 28.dp)
                .widthIn(max = 600.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onBack != null) {
                    StudyOSIconButton(
                        onClick = onBack,
                        contentDescription = "Back"
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = null,
                            tint = colors.primaryText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = "Settings",
                    style = typography.screenTitle,
                    color = colors.primaryText
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Academic Configuration Section
            StudyOSSectionHeader(
                title = "Academic setup"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shapes.surface)
                    .background(colors.surface)
                    .border(1.dp, colors.border, shapes.surface)
            ) {
                Column {
                    val studentName = uiState.student?.name ?: "Configure student"
                    val studentGrade = uiState.student?.classLevel ?: "Not set"
                    StudyOSListItem(
                        title = "Profile",
                        subtitle = "$studentName • $studentGrade",
                        onClick = onNavigateToProfile,
                        trailingContent = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                                contentDescription = null,
                                tint = colors.mutedText,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    )

                    StudyOSDivider()

                    StudyOSListItem(
                        title = "Subjects",
                        subtitle = "${uiState.subjects.size} subjects organized",
                        onClick = onNavigateToSubjects,
                        trailingContent = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                                contentDescription = null,
                                tint = colors.mutedText,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    )

                    StudyOSDivider()

                    val dailyGoal = uiState.preferences.dailyStudyGoalMinutes
                    val sessionLen = uiState.preferences.defaultSessionMinutes
                    StudyOSListItem(
                        title = "Study preferences",
                        subtitle = "$dailyGoal min daily goal • $sessionLen min sessions",
                        onClick = onNavigateToPreferences,
                        trailingContent = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                                contentDescription = null,
                                tint = colors.mutedText,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Appearance Section
            StudyOSSectionHeader(
                title = "Appearance",
                description = "Choose your reading preference."
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shapes.surface)
                    .background(colors.surface)
                    .border(1.dp, colors.border, shapes.surface)
            ) {
                Column {
                    ThemeOptionRow(
                        title = "Light",
                        isSelected = uiState.currentTheme == AppTheme.LIGHT,
                        onClick = { viewModel.setTheme(AppTheme.LIGHT) }
                    )
                    StudyOSDivider()
                    ThemeOptionRow(
                        title = "Dark",
                        isSelected = uiState.currentTheme == AppTheme.DARK,
                        onClick = { viewModel.setTheme(AppTheme.DARK) }
                    )
                    StudyOSDivider()
                    ThemeOptionRow(
                        title = "System",
                        isSelected = uiState.currentTheme == AppTheme.SYSTEM,
                        onClick = { viewModel.setTheme(AppTheme.SYSTEM) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // About Section
            StudyOSSectionHeader(
                title = "About"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shapes.surface)
                    .background(colors.surface)
                    .border(1.dp, colors.border, shapes.surface)
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "StudyOS",
                        style = typography.bodyMedium,
                        color = colors.primaryText
                    )
                    Text(
                        text = "Version 1.0.0",
                        style = typography.secondary,
                        color = colors.secondaryText
                    )
                    Text(
                        text = "Local-first study app",
                        style = typography.caption,
                        color = colors.mutedText
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }
}

@Composable
private fun ThemeOptionRow(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .semantics {
                this.role = Role.RadioButton
                this.selected = isSelected
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = if (isSelected) typography.bodyMedium else typography.body,
            color = colors.primaryText,
            modifier = Modifier.weight(1f)
        )

        if (isSelected) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
