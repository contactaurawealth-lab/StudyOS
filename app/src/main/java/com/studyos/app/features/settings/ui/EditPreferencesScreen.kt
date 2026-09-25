package com.studyos.app.features.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.studyos.app.core.ui.component.StudyOSButton
import com.studyos.app.core.ui.component.StudyOSIconButton
import com.studyos.app.core.ui.component.StudyOSOutlinedButton
import com.studyos.app.features.onboarding.viewmodel.DailyGoalOptions
import com.studyos.app.features.onboarding.viewmodel.SessionLengthOptions
import com.studyos.app.features.settings.viewmodel.SettingsViewModel
import com.studyos.app.theme.StudyOSTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditPreferencesScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes
    val scrollState = rememberScrollState()

    var dailyGoal by remember(uiState.preferences) {
        mutableIntStateOf(uiState.preferences.dailyStudyGoalMinutes)
    }
    var sessionLen by remember(uiState.preferences) {
        mutableIntStateOf(uiState.preferences.defaultSessionMinutes)
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
                .widthIn(max = 560.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                Text(
                    text = "Study preferences",
                    style = typography.screenTitle,
                    color = colors.primaryText
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Daily study goal",
                style = typography.sectionTitle,
                color = colors.primaryText
            )
            Text(
                text = "Daily focus target in minutes.",
                style = typography.secondary,
                color = colors.secondaryText,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DailyGoalOptions.forEach { minutes ->
                    val isSelected = dailyGoal == minutes
                    val chipBackground = if (isSelected) colors.surface else colors.background
                    val chipBorder = if (isSelected) colors.primaryText else colors.border
                    val textColor = if (isSelected) colors.primaryText else colors.secondaryText

                    Box(
                        modifier = Modifier
                            .clip(shapes.button)
                            .background(chipBackground)
                            .border(1.dp, chipBorder, shapes.button)
                            .clickable { dailyGoal = minutes }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .semantics { this.role = Role.RadioButton },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$minutes min",
                            style = if (isSelected) typography.bodyMedium else typography.body,
                            color = textColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Default session length",
                style = typography.sectionTitle,
                color = colors.primaryText
            )
            Text(
                text = "Length of each single study block.",
                style = typography.secondary,
                color = colors.secondaryText,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SessionLengthOptions.forEach { minutes ->
                    val isSelected = sessionLen == minutes
                    val chipBackground = if (isSelected) colors.surface else colors.background
                    val chipBorder = if (isSelected) colors.primaryText else colors.border
                    val textColor = if (isSelected) colors.primaryText else colors.secondaryText

                    Box(
                        modifier = Modifier
                            .clip(shapes.button)
                            .background(chipBackground)
                            .border(1.dp, chipBorder, shapes.button)
                            .clickable { sessionLen = minutes }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .semantics { this.role = Role.RadioButton },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$minutes min",
                            style = if (isSelected) typography.bodyMedium else typography.body,
                            color = textColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StudyOSOutlinedButton(
                    text = "Cancel",
                    onClick = onBack
                )
                Spacer(modifier = Modifier.width(12.dp))
                StudyOSButton(
                    text = "Save changes",
                    onClick = {
                        viewModel.updatePreferences(
                            dailyGoalMinutes = dailyGoal,
                            defaultSessionMinutes = sessionLen,
                            onSuccess = onBack
                        )
                    }
                )
            }
        }
    }
}
