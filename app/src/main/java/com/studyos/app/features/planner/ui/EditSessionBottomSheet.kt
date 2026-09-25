package com.studyos.app.features.planner.ui

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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.studyos.app.domain.model.StudySession
import com.studyos.app.domain.model.Subject
import com.studyos.app.theme.StudyOSTheme
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

private val DurationOptions = listOf(15, 25, 30, 45, 60, 90)

private val StandardTimes = listOf(
    Pair(9, 0) to "9:00 AM",
    Pair(11, 0) to "11:00 AM",
    Pair(14, 0) to "2:00 PM",
    Pair(16, 0) to "4:00 PM",
    Pair(18, 0) to "6:00 PM",
    Pair(20, 0) to "8:00 PM"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditSessionBottomSheet(
    session: StudySession?,
    initialDate: LocalDate,
    defaultDurationMinutes: Int,
    subjects: List<Subject>,
    onDismissRequest: () -> Unit,
    onSaveSession: (
        sessionId: String?,
        subjectId: String?,
        chapterId: String?,
        title: String?,
        scheduledStart: Long,
        plannedMinutes: Int
    ) -> Unit,
    onDeleteSession: ((sessionId: String) -> Unit)? = null,
    onCheckOverlap: suspend (startTime: Long, endTime: Long, excludeSessionId: String?) -> Boolean,
    onFetchChaptersForSubject: suspend (subjectId: String) -> List<Chapter>
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes
    val coroutineScope = rememberCoroutineScope()

    val zoneId = ZoneId.systemDefault()

    // Determine initial date & time
    val sessionDate = remember(session, initialDate) {
        if (session?.scheduledStart != null) {
            Instant.ofEpochMilli(session.scheduledStart).atZone(zoneId).toLocalDate()
        } else {
            initialDate
        }
    }

    val initialHourMinute = remember(session) {
        if (session?.scheduledStart != null) {
            val zdt = Instant.ofEpochMilli(session.scheduledStart).atZone(zoneId)
            Pair(zdt.hour, zdt.minute)
        } else {
            Pair(LocalTime.now().hour + 1, 0)
        }
    }

    var selectedDate by remember { mutableStateOf(sessionDate) }
    var selectedHourMinute by remember { mutableStateOf(initialHourMinute) }
    var selectedSubject by remember {
        mutableStateOf(
            if (session?.subjectId != null) subjects.find { it.id == session.subjectId }
            else subjects.firstOrNull()
        )
    }
    var chaptersForSubject by remember { mutableStateOf<List<Chapter>>(emptyList()) }
    var selectedChapterId by remember { mutableStateOf(session?.chapterId) }
    var plannedMinutes by remember { mutableIntStateOf(session?.plannedMinutes ?: defaultDurationMinutes) }
    var customTitle by remember { mutableStateOf(session?.title ?: "") }

    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showOverlapConfirmation by remember { mutableStateOf(false) }
    var pendingScheduledStart by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(selectedSubject?.id) {
        val subjectId = selectedSubject?.id
        if (subjectId != null) {
            chaptersForSubject = onFetchChaptersForSubject(subjectId)
        } else {
            chaptersForSubject = emptyList()
            selectedChapterId = null
        }
    }

    // Overlap Dialog
    if (showOverlapConfirmation) {
        StudyOSConfirmationDialog(
            title = "Overlapping session",
            message = "This overlaps another study session. Keep both?",
            confirmButtonText = "Keep both",
            dismissButtonText = "Cancel",
            onConfirm = {
                showOverlapConfirmation = false
                pendingScheduledStart?.let { start ->
                    onSaveSession(
                        session?.id,
                        selectedSubject?.id,
                        selectedChapterId,
                        customTitle.ifBlank { null },
                        start,
                        plannedMinutes
                    )
                }
                onDismissRequest()
            },
            onDismiss = { showOverlapConfirmation = false }
        )
    }

    // Delete Dialog
    if (showDeleteConfirmation && session != null && onDeleteSession != null) {
        StudyOSConfirmationDialog(
            title = "Delete study session?",
            message = "This study session will be permanently removed.",
            confirmButtonText = "Delete",
            dismissButtonText = "Cancel",
            onConfirm = {
                showDeleteConfirmation = false
                onDeleteSession(session.id)
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
                text = if (session != null) "Edit study session" else "New study session",
                style = typography.sectionTitle,
                color = colors.primaryText
            )

            Spacer(modifier = Modifier.height(18.dp))

            // 1. Subject
            Text(
                text = "Subject",
                style = typography.caption,
                color = colors.secondaryText,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (subjects.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 18.dp)
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
                            Text(
                                text = subject.name,
                                style = if (isSelected) typography.bodyMedium else typography.body,
                                color = textColor
                            )
                        }
                    }
                }
            }

            // 2. Chapter (optional)
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
                        .padding(bottom = 18.dp)
                ) {
                    val isNone = selectedChapterId == null
                    val noneBg = if (isNone) colors.primaryText else colors.surface
                    val noneBorder = if (isNone) colors.primaryText else colors.border
                    val noneTextColor = if (isNone) colors.background else colors.secondaryText

                    Box(
                        modifier = Modifier
                            .clip(shapes.button)
                            .background(noneBg)
                            .border(1.dp, noneBorder, shapes.button)
                            .clickable { selectedChapterId = null }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .semantics { this.role = Role.RadioButton },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Entire subject",
                            style = if (isNone) typography.bodyMedium else typography.body,
                            color = noneTextColor
                        )
                    }

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
                                .clickable { selectedChapterId = chapter.id }
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

            // 3. Date Selection
            Text(
                text = "Date: ${DateTimeUtils.formatDateHeader(selectedDate)}",
                style = typography.caption,
                color = colors.secondaryText,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 18.dp)
            ) {
                val today = LocalDate.now()
                listOf(
                    today to "Today",
                    today.plusDays(1) to "Tomorrow",
                    today.plusDays(2) to "In 2 days",
                    today.plusDays(3) to "In 3 days",
                    today.plusDays(7) to "Next week"
                ).forEach { (date, label) ->
                    val isSelected = selectedDate == date
                    val chipBg = if (isSelected) colors.surface else colors.background
                    val chipBorder = if (isSelected) colors.primaryText else colors.border
                    val textColor = if (isSelected) colors.primaryText else colors.secondaryText

                    Box(
                        modifier = Modifier
                            .clip(shapes.button)
                            .background(chipBg)
                            .border(1.dp, chipBorder, shapes.button)
                            .clickable { selectedDate = date }
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

            // 4. Start Time
            Text(
                text = "Start time",
                style = typography.caption,
                color = colors.secondaryText,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 18.dp)
            ) {
                StandardTimes.forEach { (timePair, label) ->
                    val isSelected = selectedHourMinute == timePair
                    val chipBg = if (isSelected) colors.surface else colors.background
                    val chipBorder = if (isSelected) colors.primaryText else colors.border
                    val textColor = if (isSelected) colors.primaryText else colors.secondaryText

                    Box(
                        modifier = Modifier
                            .clip(shapes.button)
                            .background(chipBg)
                            .border(1.dp, chipBorder, shapes.button)
                            .clickable { selectedHourMinute = timePair }
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

            // 5. Duration
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
                    .padding(bottom = 18.dp)
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

            // 6. Title
            Text(
                text = "Session title (optional)",
                style = typography.caption,
                color = colors.secondaryText,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            StudyOSTextField(
                value = customTitle,
                onValueChange = { customTitle = it },
                placeholder = "e.g. Revision or Chapter Review",
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (session != null && onDeleteSession != null) {
                    StudyOSOutlinedButton(
                        text = "Delete",
                        onClick = { showDeleteConfirmation = true }
                    )
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Row {
                    StudyOSOutlinedButton(
                        text = "Cancel",
                        onClick = onDismissRequest
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    StudyOSButton(
                        text = "Save",
                        onClick = {
                            val sessionStart = selectedDate.atTime(selectedHourMinute.first, selectedHourMinute.second)
                                .atZone(zoneId)
                                .toInstant()
                                .toEpochMilli()
                            val sessionEnd = sessionStart + plannedMinutes * 60_000L

                            coroutineScope.launch {
                                val hasConflict = onCheckOverlap(sessionStart, sessionEnd, session?.id)
                                if (hasConflict) {
                                    pendingScheduledStart = sessionStart
                                    showOverlapConfirmation = true
                                } else {
                                    onSaveSession(
                                        session?.id,
                                        selectedSubject?.id,
                                        selectedChapterId,
                                        customTitle.ifBlank { null },
                                        sessionStart,
                                        plannedMinutes
                                    )
                                    onDismissRequest()
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
