package com.studyos.app.features.onboarding.ui

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.studyos.app.core.ui.component.StudyOSButton
import com.studyos.app.core.ui.component.StudyOSOutlinedButton
import com.studyos.app.core.ui.component.StudyOSTextField
import com.studyos.app.features.onboarding.viewmodel.OnboardingUiState
import com.studyos.app.theme.StudyOSTheme

@Composable
fun StudyGoalStep(
    uiState: OnboardingUiState,
    onDailyGoalPresetChanged: (Int) -> Unit,
    onCustomGoalInputChanged: (String) -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes
    val scrollState = rememberScrollState()

    val options = listOf(
        Pair("30 min", 30),
        Pair("1 hour", 60),
        Pair("2 hours", 120),
        Pair("3+ hours", 180)
    )

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
                text = "Study Goal",
                style = typography.screenTitle,
                color = colors.primaryText
            )

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "How much do you want to study each day?",
                style = typography.sectionTitle,
                color = colors.primaryText
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                options.forEach { (label, minutes) ->
                    val isSelected = !uiState.isCustomGoal && uiState.dailyStudyGoalMinutes == minutes
                    val chipBackground = if (isSelected) colors.surface else colors.background
                    val chipBorder = if (isSelected) colors.primaryText else colors.border
                    val textColor = if (isSelected) colors.primaryText else colors.secondaryText

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shapes.button)
                            .background(chipBackground)
                            .border(1.dp, chipBorder, shapes.button)
                            .clickable { onDailyGoalPresetChanged(minutes) }
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                            .semantics { this.role = Role.RadioButton },
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = label,
                            style = if (isSelected) typography.bodyMedium else typography.body,
                            color = textColor
                        )
                    }
                }

                // Custom Option
                val isCustomSelected = uiState.isCustomGoal
                val customBackground = if (isCustomSelected) colors.surface else colors.background
                val customBorder = if (isCustomSelected) colors.primaryText else colors.border
                val customTextColor = if (isCustomSelected) colors.primaryText else colors.secondaryText

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shapes.button)
                        .background(customBackground)
                        .border(1.dp, customBorder, shapes.button)
                        .clickable { onCustomGoalInputChanged(uiState.customGoalInput) }
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                        .semantics { this.role = Role.RadioButton },
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = "Custom",
                        style = if (isCustomSelected) typography.bodyMedium else typography.body,
                        color = customTextColor
                    )
                }

                if (isCustomSelected) {
                    Spacer(modifier = Modifier.height(8.dp))
                    StudyOSTextField(
                        value = uiState.customGoalInput,
                        onValueChange = onCustomGoalInputChanged,
                        label = "Minutes per day",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StudyOSOutlinedButton(
                    text = "Back",
                    onClick = onBack
                )
                Spacer(modifier = Modifier.width(12.dp))
                StudyOSButton(
                    text = "Continue",
                    onClick = onContinue,
                    enabled = uiState.dailyStudyGoalMinutes > 0
                )
            }
        }
    }
}
