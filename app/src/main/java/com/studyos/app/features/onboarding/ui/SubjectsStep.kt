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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.studyos.app.core.ui.component.StudyOSButton
import com.studyos.app.core.ui.component.StudyOSOutlinedButton
import com.studyos.app.features.onboarding.viewmodel.OnboardingUiState
import com.studyos.app.theme.StudyOSTheme

@Composable
fun SubjectsStep(
    uiState: OnboardingUiState,
    onSubjectToggled: (String) -> Unit,
    onAddCustomSubject: (String) -> Boolean,
    onClearCustomError: () -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddSheet by remember { mutableStateOf(false) }
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
                .padding(horizontal = 24.dp, vertical = 32.dp)
        ) {
            Text(
                text = "Your subjects",
                style = typography.screenTitle,
                color = colors.primaryText
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Choose the subjects you study.",
                style = typography.body,
                color = colors.secondaryText
            )

            if (uiState.subjectsError != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = uiState.subjectsError,
                    style = typography.caption,
                    color = colors.error
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(uiState.allAvailableSubjects) { subject ->
                    val isSelected = uiState.selectedSubjects.contains(subject)
                    val isCustom = uiState.customSubjects.contains(subject)

                    SubjectRow(
                        name = subject,
                        isSelected = isSelected,
                        isCustom = isCustom,
                        onClick = { onSubjectToggled(subject) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shapes.button)
                            .clickable { showAddSheet = true }
                            .padding(vertical = 12.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Add another subject",
                            style = typography.bodyMedium,
                            color = colors.accent
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

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

        if (showAddSheet) {
            AddSubjectBottomSheet(
                onDismissRequest = {
                    showAddSheet = false
                    onClearCustomError()
                },
                onAddSubject = { name ->
                    val success = onAddCustomSubject(name)
                    if (success) {
                        showAddSheet = false
                    }
                    success
                },
                errorMessage = uiState.customSubjectError,
                onClearError = onClearCustomError
            )
        }
    }
}

@Composable
private fun SubjectRow(
    name: String,
    isSelected: Boolean,
    isCustom: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    val backgroundColor = if (isSelected) colors.surface else Color.Transparent
    val borderColor = if (isSelected) colors.border else Color.Transparent

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shapes.surface)
            .background(backgroundColor)
            .border(1.dp, borderColor, shapes.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .semantics {
                this.role = Role.Checkbox
                this.selected = isSelected
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Selection indicator (subtle check / outline circle)
        Box(
            modifier = Modifier
                .size(20.dp)
                .border(
                    width = 1.2.dp,
                    color = if (isSelected) colors.accent else colors.mutedText,
                    shape = shapes.statusPill
                )
                .background(
                    color = if (isSelected) colors.accent else Color.Transparent,
                    shape = shapes.statusPill
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = colors.surface
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Text(
            text = name,
            style = if (isSelected) typography.bodyMedium else typography.body,
            color = if (isSelected) colors.primaryText else colors.secondaryText,
            modifier = Modifier.weight(1f)
        )

        if (isCustom) {
            Text(
                text = "Custom",
                style = typography.caption,
                color = colors.mutedText,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}
