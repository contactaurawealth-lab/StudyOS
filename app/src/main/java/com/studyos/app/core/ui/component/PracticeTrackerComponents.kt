package com.studyos.app.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.studyos.app.theme.StudyOSTheme

data class PracticeStatsData(
    val subjectId: String,
    val subjectName: String,
    val questionsAttempted: Int = 0,
    val correct: Int = 0,
    val incorrect: Int = (questionsAttempted - correct).coerceAtLeast(0),
    val accuracy: Int = if (questionsAttempted > 0) ((correct * 100) / questionsAttempted) else 0,
    val sessionsCount: Int = 0
)

/**
 * Feature 5: PAST PAPER / QUESTION PRACTICE TRACKER CARD
 */
@Composable
fun PracticeTrackerCard(
    stats: PracticeStatsData,
    onLogPracticeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    Box(
        modifier = modifier
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
                Column {
                    Text(
                        text = "Question Practice & Past Papers",
                        style = typography.caption.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                        color = colors.primaryText
                    )
                    Text(
                        text = "${stats.sessionsCount} sessions recorded",
                        style = typography.caption.copy(fontSize = 11.sp),
                        color = colors.mutedText
                    )
                }

                StudyOSOutlinedButton(
                    text = "+ Log Practice",
                    onClick = onLogPracticeClick,
                    modifier = Modifier.height(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PracticeStatBox(
                    label = "Attempted",
                    value = "${stats.questionsAttempted}",
                    color = colors.primaryText
                )
                PracticeStatBox(
                    label = "Correct",
                    value = "${stats.correct}",
                    color = colors.success
                )
                PracticeStatBox(
                    label = "Incorrect",
                    value = "${stats.incorrect}",
                    color = colors.error
                )
                PracticeStatBox(
                    label = "Accuracy",
                    value = "${stats.accuracy}%",
                    color = Color(0xFF2563EB)
                )
            }
        }
    }
}

@Composable
private fun PracticeStatBox(
    label: String,
    value: String,
    color: Color
) {
    val typography = StudyOSTheme.typography
    val colors = StudyOSTheme.colors

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = typography.caption.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
            color = color
        )
        Text(
            text = label,
            style = typography.caption.copy(fontSize = 11.sp),
            color = colors.mutedText
        )
    }
}

/**
 * Dialog to log a Past Paper / Question Practice session.
 */
@Composable
fun LogPracticeSessionDialog(
    subjectName: String,
    onDismissRequest: () -> Unit,
    onSave: (attempted: Int, correct: Int, paperName: String) -> Unit
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography

    var paperName by remember { mutableStateOf("") }
    var attemptedText by remember { mutableStateOf("") }
    var correctText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val attempted = attemptedText.toIntOrNull() ?: 0
    val correct = correctText.toIntOrNull() ?: 0
    val incorrect = (attempted - correct).coerceAtLeast(0)
    val accuracy = if (attempted > 0) ((correct * 100) / attempted).coerceIn(0, 100) else 0

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = colors.surface,
        title = {
            Text(
                text = "Log Question Practice ($subjectName)",
                style = typography.sectionTitle.copy(fontSize = 16.sp),
                color = colors.primaryText
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                StudyOSTextField(
                    value = paperName,
                    onValueChange = { paperName = it },
                    placeholder = "Session / Paper Title (e.g. 2024 Past Paper 1)",
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StudyOSTextField(
                        value = attemptedText,
                        onValueChange = { attemptedText = it },
                        placeholder = "Attempted",
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    StudyOSTextField(
                        value = correctText,
                        onValueChange = { correctText = it },
                        placeholder = "Correct",
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                if (attempted > 0) {
                    Text(
                        text = "Calculated: $incorrect incorrect • $accuracy% accuracy",
                        style = typography.caption.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
                        color = colors.accent
                    )
                }

                errorMessage?.let {
                    Text(
                        text = it,
                        style = typography.caption.copy(color = colors.error, fontSize = 11.sp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (attempted <= 0) {
                        errorMessage = "Questions attempted must be greater than 0"
                        return@TextButton
                    }
                    if (correct > attempted) {
                        errorMessage = "Correct cannot exceed attempted"
                        return@TextButton
                    }
                    onSave(attempted, correct, paperName.ifBlank { "Practice Session" })
                    onDismissRequest()
                }
            ) {
                Text("Save Session", color = colors.accent, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel", color = colors.secondaryText)
            }
        }
    )
}
