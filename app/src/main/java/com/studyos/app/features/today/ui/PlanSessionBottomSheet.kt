package com.studyos.app.features.today.ui

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
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.mutableIntStateOf
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
import com.studyos.app.domain.model.Chapter
import com.studyos.app.domain.model.Subject
import com.studyos.app.theme.StudyOSTheme

private val DurationOptions = listOf(15, 25, 30, 45, 60, 90)
private val TimeOffsetOptions = listOf(
    0 to "Now",
    15 to "In 15m",
    30 to "In 30m",
    60 to "In 1h",
    120 to "In 2h"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PlanSessionBottomSheet(
    subjects: List<Subject>,
    defaultDurationMinutes: Int,
    onDismissRequest: () -> Unit,
    onPlanSession: (
        subjectId: String?,
        chapterId: String?,
        title: String?,
        scheduledStart: Long,
        plannedMinutes: Int
    ) -> Unit,
    onFetchChaptersForSubject: suspend (subjectId: String) -> List<Chapter>
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    var selectedSubject by remember { mutableStateOf(subjects.firstOrNull()) }
    var chaptersForSubject by remember { mutableStateOf<List<Chapter>>(emptyList()) }
    var selectedChapter by remember { mutableStateOf<Chapter?>(null) }
    var plannedMinutes by remember { mutableIntStateOf(defaultDurationMinutes) }
    var timeOffsetMinutes by remember { mutableIntStateOf(0) }
    var customTitle by remember { mutableStateOf("") }

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

    val placeholderTitle = when {
        selectedSubject != null && selectedChapter != null ->
            "${selectedSubject?.name} • ${selectedChapter?.name}"
        selectedSubject != null ->
            "${selectedSubject?.name} Study"
        else -> "Study Session"
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
                text = "Plan a session",
                style = typography.sectionTitle,
                color = colors.primaryText
            )
            Text(
                text = "Schedule dedicated study time for today.",
                style = typography.secondary,
                color = colors.secondaryText,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
            )

            // 1. Select Subject
            Text(
                text = "Subject",
                style = typography.caption,
                color = colors.secondaryText,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (subjects.isEmpty()) {
                Text(
                    text = "No subjects added yet. You can still plan a general study session.",
                    style = typography.secondary,
                    color = colors.mutedText,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                ) {
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
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                .semantics { this.role = Role.RadioButton },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = null,
                                        tint = colors.background,
                                        modifier = Modifier
                                            .size(14.dp)
                                            .padding(end = 4.dp)
                                    )
                                }
                                Text(
                                    text = subject.name,
                                    style = if (isSelected) typography.bodyMedium else typography.body,
                                    color = textColor
                                )
                            }
                        }
                    }
                }
            }

            // 2. Select Chapter (optional)
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
                        .padding(bottom = 20.dp)
                ) {
                    // Option for "Entire subject"
                    val isAllSelected = selectedChapter == null
                    val allBg = if (isAllSelected) colors.primaryText else colors.surface
                    val allBorder = if (isAllSelected) colors.primaryText else colors.border
                    val allTextColor = if (isAllSelected) colors.background else colors.secondaryText

                    Box(
                        modifier = Modifier
                            .clip(shapes.button)
                            .background(allBg)
                            .border(1.dp, allBorder, shapes.button)
                            .clickable { selectedChapter = null }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .semantics { this.role = Role.RadioButton },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Entire subject",
                            style = if (isAllSelected) typography.bodyMedium else typography.body,
                            color = allTextColor
                        )
                    }

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
                                .clickable { selectedChapter = chapter }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
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

            // 3. Start time
            Text(
                text = "When",
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
                TimeOffsetOptions.forEach { (offset, label) ->
                    val isSelected = timeOffsetMinutes == offset
                    val chipBg = if (isSelected) colors.surface else colors.background
                    val chipBorder = if (isSelected) colors.primaryText else colors.border
                    val textColor = if (isSelected) colors.primaryText else colors.secondaryText

                    Box(
                        modifier = Modifier
                            .clip(shapes.button)
                            .background(chipBg)
                            .border(1.dp, chipBorder, shapes.button)
                            .clickable { timeOffsetMinutes = offset }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .semantics { this.role = Role.RadioButton },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = if (isSelected) typography.bodyMedium else typography.body,
                            color = textColor
                        )
                    }
                }
            }

            // 4. Duration
            Text(
                text = "Duration",
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
                DurationOptions.forEach { minutes ->
                    val isSelected = plannedMinutes == minutes
                    val chipBg = if (isSelected) colors.surface else colors.background
                    val chipBorder = if (isSelected) colors.primaryText else colors.border
                    val textColor = if (isSelected) colors.primaryText else colors.secondaryText

                    Box(
                        modifier = Modifier
                            .clip(shapes.button)
                            .background(chipBg)
                            .border(1.dp, chipBorder, shapes.button)
                            .clickable { plannedMinutes = minutes }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .semantics { this.role = Role.RadioButton },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$minutes min",
                            style = if (isSelected) typography.bodyMedium else typography.body,
                            color = textColor
                        )
                    }
                }
            }

            // 5. Title (optional)
            Text(
                text = "Session title (optional)",
                style = typography.caption,
                color = colors.secondaryText,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            StudyOSTextField(
                value = customTitle,
                onValueChange = { customTitle = it },
                placeholder = placeholderTitle,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Buttons
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
                    text = "Plan Session",
                    onClick = {
                        val scheduledStart = System.currentTimeMillis() + (timeOffsetMinutes * 60_000L)
                        onPlanSession(
                            selectedSubject?.id,
                            selectedChapter?.id,
                            customTitle.ifBlank { placeholderTitle },
                            scheduledStart,
                            plannedMinutes
                        )
                    }
                )
            }
        }
    }
}
