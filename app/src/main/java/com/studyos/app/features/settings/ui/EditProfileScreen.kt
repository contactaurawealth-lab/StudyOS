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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.studyos.app.core.ui.component.StudyOSIconButton
import com.studyos.app.core.ui.component.StudyOSOutlinedButton
import com.studyos.app.core.ui.component.StudyOSTextField
import com.studyos.app.features.onboarding.viewmodel.GradeOptions
import com.studyos.app.features.settings.viewmodel.SettingsViewModel
import com.studyos.app.theme.StudyOSTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditProfileScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes
    val scrollState = rememberScrollState()

    var name by remember(uiState.student) { mutableStateOf(uiState.student?.name ?: "") }
    var classLevel by remember(uiState.student) { mutableStateOf(uiState.student?.classLevel ?: "Grade 9") }
    var division by remember(uiState.student) { mutableStateOf(uiState.student?.division ?: "") }
    var schoolName by remember(uiState.student) { mutableStateOf(uiState.student?.schoolName ?: "") }
    var nameError by remember { mutableStateOf<String?>(null) }

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
                    text = "Edit Profile",
                    style = typography.screenTitle,
                    color = colors.primaryText
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            StudyOSTextField(
                value = name,
                onValueChange = {
                    name = it
                    if (it.isNotBlank()) nameError = null
                },
                label = "Name *",
                placeholder = "Your name",
                errorMessage = nameError,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

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
                    val isSelected = classLevel == grade
                    val chipBackground = if (isSelected) colors.surface else colors.background
                    val chipBorder = if (isSelected) colors.primaryText else colors.border
                    val textColor = if (isSelected) colors.primaryText else colors.secondaryText

                    Box(
                        modifier = Modifier
                            .clip(shapes.button)
                            .background(chipBackground)
                            .border(1.dp, chipBorder, shapes.button)
                            .clickable { classLevel = grade }
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

            StudyOSTextField(
                value = division,
                onValueChange = { division = it },
                label = "Division (optional)",
                placeholder = "e.g. A, B",
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Next
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            StudyOSTextField(
                value = schoolName,
                onValueChange = { schoolName = it },
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
                        if (name.isBlank()) {
                            nameError = "Please enter your name."
                        } else {
                            viewModel.updateProfile(
                                name = name,
                                classLevel = classLevel,
                                division = division,
                                schoolName = schoolName,
                                onSuccess = onBack
                            )
                        }
                    }
                )
            }
        }
    }
}
