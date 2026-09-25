package com.studyos.app.features.tasks.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import com.studyos.app.core.ui.component.StudyOSButton
import com.studyos.app.core.ui.component.StudyOSConfirmationDialog
import com.studyos.app.core.ui.component.StudyOSOutlinedButton
import com.studyos.app.core.ui.component.StudyOSTextField
import com.studyos.app.core.util.DateTimeUtils
import com.studyos.app.domain.model.Chapter
import com.studyos.app.domain.model.Subject
import com.studyos.app.domain.model.Task
import com.studyos.app.domain.model.TaskPriority
import com.studyos.app.theme.StudyOSTheme
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditTaskBottomSheet(
    task: Task,
    subjects: List<Subject>,
    onDismissRequest: () -> Unit,
    onSaveTask: (updatedTask: Task) -> Unit,
    onDeleteTask: (taskId: String) -> Unit,
    onFetchChaptersForSubject: suspend (subjectId: String) -> List<Chapter>
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    var title by remember(task) { mutableStateOf(task.title) }
    var description by remember(task) { mutableStateOf(task.description ?: "") }
    var selectedSubject by remember(task) { mutableStateOf(subjects.find { it.id == task.subjectId }) }
    var chaptersForSubject by remember { mutableStateOf<List<Chapter>>(emptyList()) }
    var selectedChapterId by remember(task) { mutableStateOf(task.chapterId) }
    var priority by remember(task) { mutableStateOf(task.priority) }

    val initialDueDateOption = remember(task) {
        val dueAt = task.dueAt
        val today = LocalDate.now()
        val startOfToday = DateTimeUtils.getStartOfDay(today)
        val endOfToday = DateTimeUtils.getEndOfDay(today)
        val endOfTomorrow = DateTimeUtils.getEndOfDay(today.plusDays(1))
        val endOfInTwoDays = DateTimeUtils.getEndOfDay(today.plusDays(2))

        when {
            dueAt == null -> DueDateOption.NO_DUE_DATE
            dueAt in startOfToday..endOfToday -> DueDateOption.TODAY
            dueAt in (endOfToday + 1)..endOfTomorrow -> DueDateOption.TOMORROW
            dueAt in (endOfTomorrow + 1)..endOfInTwoDays -> DueDateOption.IN_TWO_DAYS
            else -> DueDateOption.NEXT_WEEK
        }
    }
    var dueDateOption by remember(task) { mutableStateOf(initialDueDateOption) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    LaunchedEffect(selectedSubject?.id) {
        val subjectId = selectedSubject?.id
        if (subjectId != null) {
            chaptersForSubject = onFetchChaptersForSubject(subjectId)
        } else {
            chaptersForSubject = emptyList()
            selectedChapterId = null
        }
    }

    if (showDeleteConfirmation) {
        StudyOSConfirmationDialog(
            title = "Delete this task?",
            message = "This task will be permanently removed.",
            confirmButtonText = "Delete",
            dismissButtonText = "Cancel",
            onConfirm = {
                showDeleteConfirmation = false
                onDeleteTask(task.id)
                onDismissRequest()
            },
            onDismiss = { showDeleteConfirmation = false },
            isDestructive = true
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = colors.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp)
                .verticalScroll(rememberScrollState())
                .imePadding(),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Edit task",
                style = typography.sectionTitle,
                color = colors.primaryText
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Title
            StudyOSTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = "Task title",
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Description
            StudyOSTextField(
                value = description,
                onValueChange = { description = it },
                placeholder = "Notes or instructions (optional)",
                singleLine = false,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Priority
            Text(
                text = "Priority",
                style = typography.caption,
                color = colors.secondaryText,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 20.dp)
            ) {
                TaskPriority.values().forEach { prio ->
                    val isSelected = priority == prio
                    val chipBg = if (isSelected) colors.primaryText else colors.surface
                    val chipBorder = if (isSelected) colors.primaryText else colors.border
                    val textColor = if (isSelected) colors.background else colors.primaryText

                    Box(
                        modifier = Modifier
                            .clip(shapes.button)
                            .background(chipBg)
                            .border(1.dp, chipBorder, shapes.button)
                            .clickable { priority = prio }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .semantics { this.role = Role.RadioButton },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = prio.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = if (isSelected) typography.bodyMedium else typography.body,
                            color = textColor
                        )
                    }
                }
            }

            // Due Date
            Text(
                text = "Due date",
                style = typography.caption,
                color = colors.secondaryText,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 20.dp)
            ) {
                DueDateOption.values().forEach { opt ->
                    val isSelected = dueDateOption == opt
                    val chipBg = if (isSelected) colors.surface else colors.background
                    val chipBorder = if (isSelected) colors.primaryText else colors.border
                    val textColor = if (isSelected) colors.primaryText else colors.secondaryText

                    Box(
                        modifier = Modifier
                            .clip(shapes.button)
                            .background(chipBg)
                            .border(1.dp, chipBorder, shapes.button)
                            .clickable { dueDateOption = opt }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .semantics { this.role = Role.RadioButton },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = opt.label,
                            style = if (isSelected) typography.bodyMedium else typography.body,
                            color = textColor
                        )
                    }
                }
            }

            // Subject
            if (subjects.isNotEmpty()) {
                Text(
                    text = "Subject (optional)",
                    style = typography.caption,
                    color = colors.secondaryText,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    val isNone = selectedSubject == null
                    val noneBg = if (isNone) colors.primaryText else colors.surface
                    val noneBorder = if (isNone) colors.primaryText else colors.border
                    val noneTextColor = if (isNone) colors.background else colors.secondaryText

                    Box(
                        modifier = Modifier
                            .clip(shapes.button)
                            .background(noneBg)
                            .border(1.dp, noneBorder, shapes.button)
                            .clickable { selectedSubject = null }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .semantics { this.role = Role.RadioButton },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "None",
                            style = if (isNone) typography.bodyMedium else typography.body,
                            color = noneTextColor
                        )
                    }

                    subjects.forEach { subject ->
                        val isSelected = selectedSubject?.id == subject.id
                        val chipBg = if (isSelected) colors.primaryText else colors.surface
                        val chipBorder = if (isSelected) colors.primaryText else colors.border
                        val textColor = if (isSelected) colors.background else colors.primaryText

                        Box(
                            modifier = Modifier
                                .clip(shapes.button)
                                .background(chipBg)
                                .border(1.dp, chipBorder, shapes.button)
                                .clickable { selectedSubject = subject }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .semantics { this.role = Role.RadioButton },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = subject.name,
                                style = if (isSelected) typography.bodyMedium else typography.body,
                                color = textColor
                            )
                        }
                    }
                }
            }

            // Chapter
            if (chaptersForSubject.isNotEmpty()) {
                Text(
                    text = "Chapter (optional)",
                    style = typography.caption,
                    color = colors.secondaryText,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    chaptersForSubject.forEach { chapter ->
                        val isSelected = selectedChapterId == chapter.id
                        val chipBg = if (isSelected) colors.primaryText else colors.surface
                        val chipBorder = if (isSelected) colors.primaryText else colors.border
                        val textColor = if (isSelected) colors.background else colors.primaryText

                        Box(
                            modifier = Modifier
                                .clip(shapes.button)
                                .background(chipBg)
                                .border(1.dp, chipBorder, shapes.button)
                                .clickable {
                                    selectedChapterId = if (isSelected) null else chapter.id
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .semantics { this.role = Role.RadioButton },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = chapter.name,
                                style = if (isSelected) typography.bodyMedium else typography.body,
                                color = textColor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StudyOSOutlinedButton(
                    text = "Delete",
                    onClick = { showDeleteConfirmation = true }
                )

                Row {
                    StudyOSOutlinedButton(
                        text = "Cancel",
                        onClick = onDismissRequest
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    StudyOSButton(
                        text = "Save",
                        onClick = {
                            val dueAtMillis = when (dueDateOption) {
                                DueDateOption.NO_DUE_DATE -> null
                                DueDateOption.TODAY -> DateTimeUtils.getEndOfDay(LocalDate.now())
                                DueDateOption.TOMORROW -> DateTimeUtils.getEndOfDay(LocalDate.now().plusDays(1))
                                DueDateOption.IN_TWO_DAYS -> DateTimeUtils.getEndOfDay(LocalDate.now().plusDays(2))
                                DueDateOption.NEXT_WEEK -> DateTimeUtils.getEndOfDay(LocalDate.now().plusWeeks(1))
                            }
                            val updated = task.copy(
                                title = title.trim(),
                                description = description.ifBlank { null },
                                subjectId = selectedSubject?.id,
                                chapterId = selectedChapterId,
                                dueAt = dueAtMillis,
                                priority = priority,
                                updatedAt = System.currentTimeMillis()
                            )
                            onSaveTask(updated)
                            onDismissRequest()
                        },
                        enabled = title.isNotBlank()
                    )
                }
            }
        }
    }
}
