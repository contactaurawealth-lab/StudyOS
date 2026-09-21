package com.studyos.app.features.tasks.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.studyos.app.core.util.DateTimeUtils
import com.studyos.app.domain.model.TaskItem
import com.studyos.app.domain.model.TaskPriority
import com.studyos.app.domain.model.TaskStatus
import com.studyos.app.theme.StudyOSTheme

@Composable
fun TaskItemRow(
    taskItem: TaskItem,
    onToggleCompletion: (taskId: String) -> Unit,
    onClick: (taskId: String) -> Unit,
    onStartTimer: ((subjectId: String?, title: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val task = taskItem.task
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes
    val isCompleted = task.status == TaskStatus.COMPLETED

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick(task.id) }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Checkbox with accessible 44dp touch target
        Box(
            modifier = Modifier
                .size(44.dp)
                .clickable { onToggleCompletion(task.id) }
                .semantics {
                    this.role = Role.Checkbox
                    this.stateDescription = if (isCompleted) "Completed" else "Not completed"
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(shapes.statusPill)
                    .background(if (isCompleted) colors.accent else colors.surface)
                    .border(
                        width = 1.dp,
                        color = if (isCompleted) colors.accent else colors.border,
                        shape = shapes.statusPill
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = null,
                        tint = colors.background,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Content
        Column(modifier = Modifier.weight(1f).padding(vertical = 6.dp)) {
            Text(
                text = task.title,
                style = if (isCompleted) typography.body else typography.bodyMedium,
                color = if (isCompleted) colors.mutedText else colors.primaryText,
                textDecoration = if (isCompleted) TextDecoration.LineThrough else null,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )

            // Subtitle metadata row
            val metaParts = mutableListOf<String>()
            taskItem.subjectName?.let { metaParts.add(it) }
            taskItem.chapterName?.let { metaParts.add(it) }

            Row(
                modifier = Modifier.padding(top = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (metaParts.isNotEmpty()) {
                    Text(
                        text = metaParts.joinToString(" • "),
                        style = typography.secondary,
                        color = colors.secondaryText,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                // Due date / Overdue tag
                when {
                    isCompleted -> {
                        Text(
                            text = "Completed",
                            style = typography.caption,
                            color = colors.mutedText
                        )
                    }
                    taskItem.isOverdue -> {
                        Text(
                            text = "Overdue",
                            style = typography.caption,
                            color = colors.accent
                        )
                    }
                    taskItem.isDueToday -> {
                        Text(
                            text = "Due today",
                            style = typography.caption,
                            color = colors.secondaryText
                        )
                    }
                    task.dueAt != null -> {
                        Text(
                            text = "Due ${DateTimeUtils.formatDateShort(task.dueAt)}",
                            style = typography.caption,
                            color = colors.secondaryText
                        )
                    }
                }

                // High priority tag
                if (!isCompleted && task.priority == TaskPriority.HIGH) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "HIGH",
                        style = typography.caption,
                        color = colors.accent
                    )
                }
            }
        }

        if (!isCompleted && onStartTimer != null) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(shapes.statusPill)
                    .clickable { onStartTimer(task.subjectId, task.title) }
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Timer,
                    contentDescription = "Start Timer for this task",
                    tint = colors.accent,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
