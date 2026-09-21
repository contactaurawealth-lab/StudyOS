package com.studyos.app.features.tasks.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.studyos.app.core.ui.component.StudyOSButton
import com.studyos.app.core.ui.component.StudyOSDivider
import com.studyos.app.core.ui.component.StudyOSIconButton
import com.studyos.app.core.ui.component.StudyOSTextField
import com.studyos.app.domain.model.TaskFilter
import com.studyos.app.features.tasks.viewmodel.TasksViewModel
import com.studyos.app.theme.StudyOSTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    viewModel: TasksViewModel,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes
    val snackbarHostState = remember { SnackbarHostState() }

    var quickTaskTitle by remember { mutableStateOf("") }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    // Add Task Sheet
    if (uiState.isAddTaskSheetOpen) {
        AddTaskBottomSheet(
            subjects = uiState.subjects,
            onDismissRequest = viewModel::closeAddTaskSheet,
            onAddTask = { title, description, subjectId, chapterId, dueAt, priority ->
                viewModel.addTask(title, description, subjectId, chapterId, dueAt, priority)
            },
            onFetchChaptersForSubject = { subjectId ->
                viewModel.getChaptersForSubject(subjectId)
            }
        )
    }

    // Edit Task Sheet
    uiState.editingTask?.let { task ->
        EditTaskBottomSheet(
            task = task,
            subjects = uiState.subjects,
            onDismissRequest = viewModel::closeEditTask,
            onSaveTask = viewModel::updateTask,
            onDeleteTask = viewModel::deleteTask,
            onFetchChaptersForSubject = { subjectId ->
                viewModel.getChaptersForSubject(subjectId)
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Tasks",
                        style = typography.subsectionTitle,
                        color = colors.primaryText
                    )
                },
                navigationIcon = {
                    if (onBack != null) {
                        StudyOSIconButton(
                            onClick = onBack,
                            contentDescription = "Back"
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = null,
                                tint = colors.primaryText,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                },
                actions = {
                    StudyOSIconButton(
                        onClick = viewModel::openAddTaskSheet,
                        contentDescription = "Add task"
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = null,
                            tint = colors.primaryText,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.background
                )
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp)
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.Start
            ) {
                // Filter tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                TaskFilter.values().forEach { filter ->
                    val isSelected = uiState.filter == filter
                    val chipBg = if (isSelected) colors.primaryText else colors.surface
                    val chipBorder = if (isSelected) colors.primaryText else colors.border
                    val textColor = if (isSelected) colors.background else colors.secondaryText

                    Box(
                        modifier = Modifier
                            .clip(shapes.button)
                            .background(chipBg)
                            .border(1.dp, chipBorder, shapes.button)
                            .clickable { viewModel.setFilter(filter) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .semantics { this.role = Role.Tab },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = filter.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = if (isSelected) typography.bodyMedium else typography.body,
                            color = textColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quick task creation field
            StudyOSTextField(
                value = quickTaskTitle,
                onValueChange = { quickTaskTitle = it },
                placeholder = "+ Add a task",
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (quickTaskTitle.isNotBlank()) {
                            viewModel.quickAddTask(quickTaskTitle.trim())
                            quickTaskTitle = ""
                        }
                    }
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Task list
            if (uiState.tasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (uiState.filter == TaskFilter.COMPLETED) {
                            Text(
                                text = "No completed tasks yet.",
                                style = typography.body,
                                color = colors.secondaryText
                            )
                        } else {
                            Text(
                                text = "No tasks yet.",
                                style = typography.sectionTitle,
                                color = colors.primaryText
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Add something you need to finish.",
                                style = typography.body,
                                color = colors.secondaryText
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                            StudyOSButton(
                                text = "Add task",
                                onClick = viewModel::openAddTaskSheet
                            )
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(shapes.surface)
                        .background(colors.surface)
                        .border(1.dp, colors.border, shapes.surface)
                ) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        itemsIndexed(uiState.tasks, key = { _, item -> item.task.id }) { index, taskItem ->
                            TaskItemRow(
                                taskItem = taskItem,
                                onToggleCompletion = viewModel::toggleTaskCompletion,
                                onClick = {
                                    viewModel.openEditTask(taskItem.task)
                                }
                            )

                            if (index < uiState.tasks.lastIndex) {
                                StudyOSDivider()
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
}
