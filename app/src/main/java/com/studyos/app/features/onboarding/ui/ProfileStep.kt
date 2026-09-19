package com.studyos.app.features.onboarding.ui

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.studyos.app.core.ui.component.StudyOSButton
import com.studyos.app.core.ui.component.StudyOSOutlinedButton
import com.studyos.app.core.ui.component.StudyOSTextField
import com.studyos.app.features.onboarding.viewmodel.GradeOptions
import com.studyos.app.features.onboarding.viewmodel.OnboardingUiState
import com.studyos.app.theme.StudyOSTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileStep(
    uiState: OnboardingUiState,
    onNameChanged: (String) -> Unit,
    onClassLevelChanged: (String) -> Unit,
    onDivisionChanged: (String) -> Unit,
    onSchoolNameChanged: (String) -> Unit,
    onContinue: () -> Unit,
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
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 32.dp)
                .widthIn(max = 520.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = "Tell us a little about you",
                style = typography.screenTitle,
                color = colors.primaryText
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Name (Required)
            StudyOSTextField(
                value = uiState.name,
                onValueChange = onNameChanged,
                label = "Name *",
                placeholder = "Your name",
                errorMessage = uiState.nameError,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Class / Grade (Required)
            Text(
                text = "Class / Grade *",
                style = typography.secondary,
                color = colors.secondaryText,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GradeOptions.forEach { grade ->
                    val isSelected = uiState.classLevel == grade
                    val chipBackground = if (isSelected) colors.surface else colors.background
                    val chipBorder = if (isSelected) colors.primaryText else colors.border
                    val textColor = if (isSelected) colors.primaryText else colors.secondaryText

                    Box(
                        modifier = Modifier
                            .clip(shapes.button)
                            .background(chipBackground)
                            .border(1.dp, chipBorder, shapes.button)
                            .clickable { onClassLevelChanged(grade) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .semantics { this.role = Role.RadioButton },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = grade,
                            style = if (isSelected) typography.bodyMedium else typography.body,
                            color = textColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Division (Optional)
            StudyOSTextField(
                value = uiState.division,
                onValueChange = onDivisionChanged,
                label = "Division (optional)",
                placeholder = "e.g. A, B",
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Next
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            // School (Optional)
            StudyOSTextField(
                value = uiState.schoolName,
                onValueChange = onSchoolNameChanged,
                label = "School (optional)",
                placeholder = "Your school name",
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done
                )
            )

            Spacer(modifier = Modifier.height(36.dp))

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
                    onClick = onContinue
                )
            }
        }
    }
}
