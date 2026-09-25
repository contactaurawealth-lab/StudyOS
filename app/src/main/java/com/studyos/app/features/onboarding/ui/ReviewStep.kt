package com.studyos.app.features.onboarding.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.studyos.app.core.ui.component.StudyOSButton
import com.studyos.app.core.ui.component.StudyOSDivider
import com.studyos.app.core.ui.component.StudyOSOutlinedButton
import com.studyos.app.features.onboarding.viewmodel.OnboardingUiState
import com.studyos.app.theme.StudyOSTheme

@Composable
fun ReviewStep(
    uiState: OnboardingUiState,
    onFinishSetup: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 32.dp)
        ) {
            Text(
                text = "Ready to study",
                style = typography.screenTitle,
                color = colors.primaryText
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Clean quiet summary container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shapes.surface)
                    .background(colors.surface)
                    .border(1.dp, colors.border, shapes.surface)
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Name & Class
                    Column {
                        Text(
                            text = uiState.name.ifBlank { "Student" },
                            style = typography.sectionTitle,
                            color = colors.primaryText
                        )
                        val classDivision = buildString {
                            append(uiState.classLevel)
                            if (uiState.division.isNotBlank()) {
                                append(" • Division ${uiState.division}")
                            }
                        }
                        Text(
                            text = classDivision,
                            style = typography.secondary,
                            color = colors.secondaryText,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                        if (uiState.schoolName.isNotBlank()) {
                            Text(
                                text = uiState.schoolName,
                                style = typography.secondary,
                                color = colors.mutedText,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }

                    StudyOSDivider()

                    // Subjects
                    Column {
                        Text(
                            text = "${uiState.selectedSubjects.size} subjects",
                            style = typography.bodyMedium,
                            color = colors.primaryText
                        )
                        Text(
                            text = uiState.selectedSubjects.joinToString(", "),
                            style = typography.secondary,
                            color = colors.secondaryText,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    StudyOSDivider()

                    // Preferences
                    Column {
                        Text(
                            text = "${uiState.dailyStudyGoalMinutes} min daily goal",
                            style = typography.bodyMedium,
                            color = colors.primaryText
                        )
                        Text(
                            text = "${uiState.defaultSessionMinutes} min sessions",
                            style = typography.secondary,
                            color = colors.secondaryText,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            if (uiState.saveError != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = uiState.saveError,
                    style = typography.caption,
                    color = colors.error
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StudyOSOutlinedButton(
                    text = "Back",
                    onClick = onBack,
                    enabled = !uiState.isLoading
                )
                Spacer(modifier = Modifier.width(12.dp))
                StudyOSButton(
                    text = "Finish setup",
                    onClick = onFinishSetup,
                    isLoading = uiState.isLoading
                )
            }
        }
    }
}
