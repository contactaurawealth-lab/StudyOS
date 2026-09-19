package com.studyos.app.features.subjects.ui

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.studyos.app.core.ui.component.StudyOSButton
import com.studyos.app.core.ui.component.StudyOSEmptyState
import com.studyos.app.core.ui.component.StudyOSLoadingState
import com.studyos.app.core.ui.component.StudyOSOutlinedButton
import com.studyos.app.features.onboarding.ui.AddSubjectBottomSheet
import com.studyos.app.features.subjects.viewmodel.SubjectsViewModel
import com.studyos.app.theme.StudyOSTheme

@Composable
fun SubjectsScreen(
    viewModel: SubjectsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes
    var showAddSheet by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.actionMessage) {
        uiState.actionMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        if (uiState.isLoading) {
            StudyOSLoadingState(message = "Loading subjects...")
        } else if (uiState.subjects.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 36.dp)
                    .widthIn(max = 560.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Subjects",
                    style = typography.screenTitle,
                    color = colors.primaryText
                )

                Spacer(modifier = Modifier.height(48.dp))

                StudyOSEmptyState(
                    title = "No subjects yet.",
                    description = "Add your subjects to start organizing your studies.",
                    actionButtonText = "Add subject",
                    onActionClick = { showAddSheet = true }
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 36.dp)
                    .widthIn(max = 560.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Subjects",
                            style = typography.screenTitle,
                            color = colors.primaryText
                        )
                        Text(
                            text = "${uiState.subjects.size} active subjects",
                            style = typography.secondary,
                            color = colors.secondaryText,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    StudyOSOutlinedButton(
                        text = "Add",
                        onClick = { showAddSheet = true }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.subjects, key = { it.id }) { subject ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(shapes.surface)
                                .background(colors.surface)
                                .border(1.dp, colors.border, shapes.surface)
                                .padding(horizontal = 16.dp, vertical = 14.dp)
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
                                        color = colors.mutedText
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showAddSheet) {
            AddSubjectBottomSheet(
                onDismissRequest = {
                    showAddSheet = false
                    viewModel.clearMessages()
                },
                onAddSubject = { name ->
                    val success = viewModel.addSubject(name)
                    if (success) {
                        showAddSheet = false
                    }
                    success
                },
                errorMessage = uiState.errorMessage,
                onClearError = viewModel::clearMessages
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }
}
