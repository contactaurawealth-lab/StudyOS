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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import com.studyos.app.core.ui.component.StudyOSOutlinedButton
import com.studyos.app.core.ui.component.StudyOSTextField
import com.studyos.app.core.util.DateTimeUtils
import com.studyos.app.domain.model.Chapter
import com.studyos.app.domain.model.Subject
import com.studyos.app.domain.model.TaskPriority
import com.studyos.app.theme.StudyOSTheme
import java.time.LocalDate

enum class DueDateOption(val label: String) {
    NO_DUE_DATE("No due date"),
    TODAY("Today"),
    TOMORROW("Tomorrow"),
    IN_TWO_DAYS("In 2 days"),
    NEXT_WEEK("Next week")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddTaskBottomSheet(
    subjects: List<Subject>,
    onDismissRequest: () -> Unit,
    onAddTask: (
        title: String,
        description: String?,
        subjectId: String?,
        chapterId: String?,
        dueAt: Long?,
        priority: TaskPriority
    ) -> Unit,
    onFetchChaptersForSubject: suspend (subjectId: String) -> List<Chapter>
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedSubject by remember { mutableStateOf<Subject?>(null) }
    var chaptersForSubject by remember { mutableStateOf<List<Chapter>>(emptyList()) }
    var selectedChapter by remember { mutableStateOf<Chapter?>(null) }
    var dueDateOption by remember { mutableStateOf(DueDateOption.TODAY) }
    var priority by remember { mutableStateOf(TaskPriority.MEDIUM) }

    LaunchedEffect(selectedSubject?.id) {
        val subjectId = selectedSubject?.id
        if (subjectId != null) {
            chaptersForSubject = onFetchChaptersForSubject(subjectId)
            selectedChapter = null
        } else {
            chaptersForSubject = emptyList()
            selectedChapter = null
        }
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
                text = "Add task",
                style = typography.sectionTitle,
                color = colors.primaryText
            )
            Text(
                text = "Record work that needs to be completed.",
                style = typography.secondary,
                color = colors.secondaryText,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
            )

            // Title
            StudyOSTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = "Task title",
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Description (optional)
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

            // Subject (optional)
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
                    // Option for None
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

            // Chapter (optional)
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
                        val isSelected = selectedChapter?.id == chapter.id
                        val chipBg = if (isSelected) colors.primaryText else colors.surface
                        val chipBorder = if (isSelected) colors.primaryText else colors.border
                        val textColor = if (isSelected) colors.background else colors.primaryText

                        Box(
                            modifier = Modifier
                                .clip(shapes.button)
                                .background(chipBg)
                                .border(1.dp, chipBorder, shapes.button)
                                .clickable {
                                    selectedChapter = if (isSelected) null else chapter
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
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StudyOSOutlinedButton(
                    text = "Cancel",
                    onClick = onDismissRequest
                )

                Spacer(modifier = Modifier.width(12.dp))

                StudyOSButton(
                    text = "Add Task",
                    onClick = {
                        val dueAtMillis = when (dueDateOption) {
                            DueDateOption.NO_DUE_DATE -> null
                            DueDateOption.TODAY -> DateTimeUtils.getEndOfDay(LocalDate.now())
                            DueDateOption.TOMORROW -> DateTimeUtils.getEndOfDay(LocalDate.now().plusDays(1))
                            DueDateOption.IN_TWO_DAYS -> DateTimeUtils.getEndOfDay(LocalDate.now().plusDays(2))
                            DueDateOption.NEXT_WEEK -> DateTimeUtils.getEndOfDay(LocalDate.now().plusWeeks(1))
                        }
                        onAddTask(
                            title.trim(),
                            description.ifBlank { null },
                            selectedSubject?.id,
                            selectedChapter?.id,
                            dueAtMillis,
                            priority
                        )
                    },
                    enabled = title.isNotBlank()
                )
            }
        }
    }
}
