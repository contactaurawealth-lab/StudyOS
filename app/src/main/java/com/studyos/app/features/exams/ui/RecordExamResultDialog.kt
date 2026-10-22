package com.studyos.app.features.exams.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studyos.app.core.database.entity.QuestionBankEntity
import com.studyos.app.core.ui.component.GlassCard
import com.studyos.app.core.ui.component.GlassIconButton
import com.studyos.app.core.ui.component.StudyOSButton
import com.studyos.app.core.ui.component.StudyOSOutlinedButton
import com.studyos.app.core.ui.component.StudyOSTextField
import com.studyos.app.domain.model.LossReasonCategory
import com.studyos.app.domain.usecase.LostMarkInput
import com.studyos.app.theme.StudyOSTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordExamResultDialog(
    paperTitle: String,
    totalMarks: Double,
    durationMinutes: Int,
    questions: List<QuestionBankEntity>,
    subjectId: String,
    onDismissRequest: () -> Unit,
    onSaveResult: (
        marksObtained: Double,
        timeTakenMinutes: Int,
        notes: String?,
        lostMarks: List<LostMarkInput>
    ) -> Unit
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var marksObtainedText by remember { mutableStateOf("") }
    var timeTakenText by remember { mutableStateOf(durationMinutes.toString()) }
    var notesText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Map of questionId -> Pair(marksLost: Double, category: LossReasonCategory)
    val questionLossMap = remember {
        mutableStateMapOf<String, Pair<Double, LossReasonCategory>>()
    }

    val obtained = marksObtainedText.toDoubleOrNull() ?: 0.0
    val percentage = if (totalMarks > 0) ((obtained / totalMarks) * 100.0).coerceIn(0.0, 100.0) else 0.0

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = colors.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(colors.accent.copy(alpha = 0.15f))
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Insights,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Record Exam Result",
                            style = typography.sectionTitle.copy(fontWeight = FontWeight.Bold),
                            color = colors.primaryText
                        )
                        Text(
                            text = paperTitle,
                            style = typography.caption,
                            color = colors.secondaryText
                        )
                    }
                }

                GlassIconButton(onClick = onDismissRequest, contentDescription = "Close") {
                    Icon(Icons.Outlined.Close, null, tint = colors.secondaryText, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Score Input & Live Percentage Card
            GlassCard(
                backgroundColor = colors.cardBackground,
                padding = 16.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Marks Obtained", style = typography.caption, color = colors.secondaryText)
                        Spacer(modifier = Modifier.height(4.dp))
                        StudyOSTextField(
                            value = marksObtainedText,
                            onValueChange = {
                                marksObtainedText = it
                                errorMessage = null
                            },
                            placeholder = "Max: ${totalMarks.toInt()}",
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text("Time Taken (mins)", style = typography.caption, color = colors.secondaryText)
                        Spacer(modifier = Modifier.height(4.dp))
                        StudyOSTextField(
                            value = timeTakenText,
                            onValueChange = { timeTakenText = it },
                            placeholder = "Minutes",
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(shapes.card)
                            .background(if (percentage >= 90) colors.success.copy(alpha = 0.15f) else colors.accent.copy(alpha = 0.15f))
                            .border(1.dp, if (percentage >= 90) colors.success.copy(alpha = 0.3f) else colors.accent.copy(alpha = 0.3f), shapes.card)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "%.1f%%".format(percentage),
                                style = typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
                                color = if (percentage >= 90) colors.success else colors.accent
                            )
                            Text(
                                text = "Score",
                                style = typography.caption.copy(fontSize = 10.sp),
                                color = colors.mutedText
                            )
                        }
                    }
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Outlined.WarningAmber, null, tint = colors.critical, modifier = Modifier.size(16.dp))
                    Text(errorMessage!!, style = typography.caption, color = colors.critical)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Diagnostic Section Header
            Text(
                text = "QUESTION DIAGNOSTICS & LOST MARKS",
                style = typography.caption.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                color = colors.secondaryText
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Tag questions where you lost marks with one of the 10 diagnostic categories. These automatically sync to your Mistake Bank for active recall.",
                style = typography.caption.copy(fontSize = 11.sp),
                color = colors.mutedText,
                lineHeight = 15.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Question List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .height(260.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(questions, key = { _, q -> q.id }) { index, question ->
                    val lossEntry = questionLossMap[question.id]
                    val isMarkedLost = lossEntry != null

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shapes.card)
                            .background(if (isMarkedLost) colors.surface.copy(alpha = 0.8f) else colors.cardBackground.copy(alpha = 0.5f))
                            .border(
                                width = 1.dp,
                                color = if (isMarkedLost) colors.accent.copy(alpha = 0.5f) else colors.border.copy(alpha = 0.4f),
                                shape = shapes.card
                            )
                            .padding(10.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Q${index + 1}.",
                                        style = typography.caption.copy(fontWeight = FontWeight.Bold),
                                        color = colors.primaryText
                                    )
                                    Text(
                                        text = question.questionText.take(50) + if (question.questionText.length > 50) "..." else "",
                                        style = typography.caption,
                                        color = colors.primaryText,
                                        maxLines = 1
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("${question.marks}M", style = typography.caption, color = colors.mutedText)
                                    Box(
                                        modifier = Modifier
                                            .clip(shapes.surface)
                                            .background(if (isMarkedLost) colors.critical.copy(alpha = 0.15f) else colors.surface)
                                            .clickable {
                                                if (isMarkedLost) {
                                                    questionLossMap.remove(question.id)
                                                } else {
                                                    questionLossMap[question.id] = Pair(question.marks.toDouble(), LossReasonCategory.CONCEPT_ERROR)
                                                }
                                            }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = if (isMarkedLost) "Lost Marks" else "+ Log Loss",
                                            style = typography.caption.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                                            color = if (isMarkedLost) colors.critical else colors.accent
                                        )
                                    }
                                }
                            }

                            // If marked lost, show category picker
                            if (isMarkedLost) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Reason for losing marks:", style = typography.caption.copy(fontSize = 10.sp), color = colors.secondaryText)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    LossReasonCategory.values().forEach { cat ->
                                        val isSelected = lossEntry?.second == cat
                                        Box(
                                            modifier = Modifier
                                                .clip(shapes.surface)
                                                .background(if (isSelected) colors.accent.copy(alpha = 0.2f) else colors.surface)
                                                .border(
                                                    width = 1.dp,
                                                    color = if (isSelected) colors.accent else colors.border,
                                                    shape = shapes.surface
                                                )
                                                .clickable {
                                                    val currentMarks = lossEntry?.first ?: question.marks.toDouble()
                                                    questionLossMap[question.id] = Pair(currentMarks, cat)
                                                }
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = cat.displayName,
                                                style = typography.caption.copy(fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                                                color = if (isSelected) colors.accent else colors.secondaryText
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StudyOSOutlinedButton(
                    text = "Cancel",
                    onClick = onDismissRequest,
                    modifier = Modifier.weight(1f)
                )

                StudyOSButton(
                    text = "Save & Run Diagnostic",
                    onClick = {
                        val parsedMarks = marksObtainedText.toDoubleOrNull()
                        if (parsedMarks == null || parsedMarks < 0 || parsedMarks > totalMarks) {
                            errorMessage = "Please enter valid marks obtained (0 to ${totalMarks.toInt()})."
                            return@StudyOSButton
                        }
                        val time = timeTakenText.toIntOrNull() ?: durationMinutes

                        val lostList = questionLossMap.map { (qId, pair) ->
                            val q = questions.find { it.id == qId }
                            LostMarkInput(
                                questionId = qId,
                                topicId = q?.topicId,
                                chapterId = q?.chapterId,
                                subjectId = subjectId,
                                marksLost = pair.first,
                                category = pair.second,
                                questionText = q?.questionText,
                                studentAnswer = null,
                                correctAnswer = q?.markingScheme
                            )
                        }

                        onSaveResult(parsedMarks, time, notesText.ifBlank { null }, lostList)
                        onDismissRequest()
                    },
                    modifier = Modifier.weight(1.5f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
