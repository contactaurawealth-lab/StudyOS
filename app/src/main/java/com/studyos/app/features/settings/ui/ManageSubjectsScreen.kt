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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.studyos.app.core.ui.component.StudyOSBottomSheet
import com.studyos.app.core.ui.component.StudyOSButton
import com.studyos.app.core.ui.component.StudyOSDialog
import com.studyos.app.core.ui.component.StudyOSEmptyState
import com.studyos.app.core.ui.component.StudyOSIconButton
import com.studyos.app.core.ui.component.StudyOSOutlinedButton
import com.studyos.app.core.ui.component.StudyOSTextField
import com.studyos.app.domain.model.Subject
import com.studyos.app.features.onboarding.ui.AddSubjectBottomSheet
import com.studyos.app.features.settings.viewmodel.SettingsViewModel
import com.studyos.app.theme.StudyOSTheme

@Composable
fun ManageSubjectsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    var showAddSheet by remember { mutableStateOf(false) }
    var addError by remember { mutableStateOf<String?>(null) }

    var subjectToRename by remember { mutableStateOf<Subject?>(null) }
    var renameValue by remember { mutableStateOf("") }
    var renameError by remember { mutableStateOf<String?>(null) }

    var subjectToDelete by remember { mutableStateOf<Subject?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 28.dp)
                .widthIn(max = 600.dp)
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Subjects",
                        style = typography.screenTitle,
                        color = colors.primaryText
                    )
                }
                StudyOSOutlinedButton(
                    text = "Add",
                    onClick = { showAddSheet = true }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (uiState.subjects.isEmpty()) {
                StudyOSEmptyState(
                    title = "No subjects configured.",
                    description = "Add subjects to organize your study time.",
                    actionButtonText = "Add subject",
                    onActionClick = { showAddSheet = true }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.subjects, key = { it.id }) { subject ->
                        SubjectManageRow(
                            subject = subject,
                            onRename = {
                                subjectToRename = subject
                                renameValue = subject.name
                                renameError = null
                            },
                            onDelete = { subjectToDelete = subject }
                        )
                    }
                }
            }
        }

        // Add Subject Bottom Sheet
        if (showAddSheet) {
            AddSubjectBottomSheet(
                onDismissRequest = {
                    showAddSheet = false
                    addError = null
                },
                onAddSubject = { name ->
                    var success = false
                    viewModel.addSubject(name) { ok, err ->
                        success = ok
                        addError = err
                    }
                    success
                },
                errorMessage = addError,
                onClearError = { addError = null }
            )
        }

        // Rename Subject Dialog
        if (subjectToRename != null) {
            StudyOSDialog(
                onDismissRequest = {
                    subjectToRename = null
                    renameError = null
                },
                title = "Rename subject",
                confirmButtonText = "Save",
                onConfirm = {
                    val target = subjectToRename ?: return@StudyOSDialog
                    viewModel.renameSubject(target.id, renameValue) { ok, err ->
                        if (ok) {
                            subjectToRename = null
                            renameError = null
                        } else {
                            renameError = err
                        }
                    }
                },
                content = {
                    StudyOSTextField(
                        value = renameValue,
                        onValueChange = {
                            renameValue = it
                            renameError = null
                        },
                        placeholder = "Subject name",
                        errorMessage = renameError,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Done
                        )
                    )
                }
            )
        }

        // Delete Subject Confirmation Dialog (Section 18)
        if (subjectToDelete != null) {
            StudyOSDialog(
                onDismissRequest = { subjectToDelete = null },
                title = "Delete subject?",
                text = "This will remove ${subjectToDelete?.name} from your StudyOS setup.",
                confirmButtonText = "Delete",
                onConfirm = {
                    subjectToDelete?.let { viewModel.deleteSubject(it.id) }
                    subjectToDelete = null
                },
                dismissButtonText = "Cancel",
                isDestructive = true
            )
        }
    }
}

@Composable
private fun SubjectManageRow(
    subject: Subject,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes
    var menuExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shapes.surface)
            .background(colors.surface)
            .border(1.dp, colors.border, shapes.surface)
            .padding(start = 16.dp, end = 6.dp, top = 8.dp, bottom = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.MenuBook,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = colors.secondaryText
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = subject.name,
                style = typography.bodyMedium,
                color = colors.primaryText,
                modifier = Modifier.weight(1f)
            )

            if (subject.isCustom) {
                Text(
                    text = "Custom",
                    style = typography.caption,
                    color = colors.mutedText,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }

            Box {
                StudyOSIconButton(
                    onClick = { menuExpanded = true },
                    contentDescription = "Options for ${subject.name}"
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = colors.secondaryText
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(colors.surface)
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "Rename",
                                style = typography.body,
                                color = colors.primaryText
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = colors.secondaryText
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onRename()
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "Delete",
                                style = typography.body,
                                color = colors.error
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = colors.error
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}
