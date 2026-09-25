package com.studyos.app.features.exams.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studyos.app.core.ui.component.StudyOSProgressBar
import com.studyos.app.domain.model.Exam
import com.studyos.app.domain.model.Note
import com.studyos.app.theme.StudyOSTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Feature 9: EXAM DAY MODE
 * Focused, distraction-free screen containing:
 * - Exam name
 * - Countdown
 * - Syllabus completion
 * - Final revision checklist
 * - Important notes
 * - Practice status
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamDayModeScreen(
    exam: Exam,
    syllabusCompletionPercentage: Int,
    importantNotes: List<Note>,
    questionsPracticedCount: Int,
    practiceAccuracyPercentage: Int,
    mockTestsCount: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    val daysLeft = exam.getDaysRemaining()
    val isToday = daysLeft == 0L
    val isPast = daysLeft < 0L

    val countdownDisplay = when {
        isToday -> "TODAY"
        daysLeft == 1L -> "1 DAY LEFT"
        daysLeft > 1L -> "$daysLeft DAYS LEFT"
        else -> "EXAM ENDED"
    }

    val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(exam.targetDate))

    // Checklist state
    val checklistItems = remember {
        mutableStateListOf(
            "Review key formula sheet and core constants" to false,
            "Re-read high-yield definitions and theorems" to false,
            "Quick review of unresolved items in Mistake Book" to false,
            "Stationery, calculator, pens & student ID prepared" to false,
            "Hydrate, rest well, and approach exam calmly" to false
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Exam Day Mode",
                            style = typography.caption.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                            color = colors.accent
                        )
                        Text(
                            text = exam.name,
                            style = typography.sectionTitle.copy(fontSize = 17.sp),
                            color = colors.primaryText
                        )
                    }
                },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Exit Exam Day Mode",
                            tint = colors.primaryText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Big Calm Countdown Hero
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shapes.surface)
                    .background(colors.surface)
                    .border(1.5.dp, colors.accent, shapes.surface)
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = countdownDisplay,
                        style = typography.screenTitle.copy(fontSize = 32.sp, fontWeight = FontWeight.Bold),
                        color = colors.accent
                    )
                    Text(
                        text = formattedDate,
                        style = typography.caption.copy(fontSize = 13.sp),
                        color = colors.secondaryText
                    )
                }
            }

            // 2. Syllabus Completion
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shapes.surface)
                    .background(colors.surface)
                    .border(1.dp, colors.border, shapes.surface)
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Syllabus Coverage",
                            style = typography.caption.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                            color = colors.primaryText
                        )
                        Text(
                            text = "$syllabusCompletionPercentage%",
                            style = typography.caption.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                            color = colors.accent
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    StudyOSProgressBar(progress = syllabusCompletionPercentage, modifier = Modifier.fillMaxWidth())
                }
            }

            // 3. Final Revision Checklist
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shapes.surface)
                    .background(colors.surface)
                    .border(1.dp, colors.border, shapes.surface)
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Final Revision Checklist",
                        style = typography.caption.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                        color = colors.primaryText
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    checklistItems.forEachIndexed { index, (itemText, isChecked) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    checklistItems[index] = itemText to checked
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = colors.success,
                                    uncheckedColor = colors.mutedText
                                )
                            )
                            Text(
                                text = itemText,
                                style = typography.caption.copy(
                                    fontSize = 13.sp,
                                    color = if (isChecked) colors.mutedText else colors.primaryText
                                )
                            )
                        }
                    }
                }
            }

            // 4. Practice Status
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shapes.surface)
                    .background(colors.surface)
                    .border(1.dp, colors.border, shapes.surface)
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Practice Status",
                        style = typography.caption.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                        color = colors.primaryText
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "$questionsPracticedCount", style = typography.caption.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp), color = colors.primaryText)
                            Text(text = "Questions Practiced", style = typography.caption.copy(fontSize = 11.sp), color = colors.mutedText)
                        }
                        Column {
                            Text(text = "$practiceAccuracyPercentage%", style = typography.caption.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp), color = colors.success)
                            Text(text = "Practice Accuracy", style = typography.caption.copy(fontSize = 11.sp), color = colors.mutedText)
                        }
                        Column {
                            Text(text = "$mockTestsCount", style = typography.caption.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp), color = colors.accent)
                            Text(text = "Mocks Completed", style = typography.caption.copy(fontSize = 11.sp), color = colors.mutedText)
                        }
                    }
                }
            }

            // 5. Important Key Revision Notes
            if (importantNotes.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shapes.surface)
                        .background(colors.surface)
                        .border(1.dp, colors.border, shapes.surface)
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Important Notes / Key Formulas",
                            style = typography.caption.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                            color = colors.primaryText
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        importantNotes.take(4).forEach { note ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(shapes.small)
                                    .background(colors.cardBackground)
                                    .padding(10.dp)
                                    .padding(bottom = 6.dp)
                            ) {
                                Column {
                                    Text(
                                        text = note.title,
                                        style = typography.caption.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                                        color = colors.primaryText
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = note.content,
                                        style = typography.caption.copy(fontSize = 12.sp, lineHeight = 16.sp),
                                        color = colors.secondaryText,
                                        maxLines = 3
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
