package com.studyos.app.features.subjects.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.studyos.app.core.ui.component.StudyOSButton
import com.studyos.app.core.ui.component.StudyOSOutlinedButton
import com.studyos.app.core.ui.component.StudyOSTextField
import com.studyos.app.domain.model.Chapter
import com.studyos.app.theme.StudyOSTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditChapterBottomSheet(
    chapter: Chapter,
    onDismissRequest: () -> Unit,
    onSaveChapter: suspend (name: String, description: String?, progress: Int) -> Boolean,
    errorMessage: String? = null,
    onClearError: () -> Unit = {}
) {
    var name by remember { mutableStateOf(chapter.name) }
    var description by remember { mutableStateOf(chapter.description ?: "") }
    var progressValue by remember { mutableFloatStateOf(chapter.progress.toFloat()) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val coroutineScope = rememberCoroutineScope()

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
                .imePadding(),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Edit chapter",
                style = typography.sectionTitle,
                color = colors.primaryText
            )

            Spacer(modifier = Modifier.height(18.dp))

            StudyOSTextField(
                value = name,
                onValueChange = {
                    name = it
                    if (errorMessage != null) onClearError()
                },
                placeholder = "Chapter name",
                errorMessage = errorMessage,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            StudyOSTextField(
                value = description,
                onValueChange = { description = it },
                placeholder = "Description (optional)",
                singleLine = false,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Progress",
                    style = typography.bodyMedium,
                    color = colors.primaryText
                )
                Text(
                    text = "${progressValue.roundToInt()}%",
                    style = typography.bodyMedium,
                    color = colors.accent
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Slider(
                value = progressValue,
                onValueChange = { progressValue = it },
                valueRange = 0f..100f,
                steps = 99,
                colors = SliderDefaults.colors(
                    thumbColor = colors.accent,
                    activeTrackColor = colors.accent,
                    inactiveTrackColor = colors.border
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                StudyOSOutlinedButton(
                    text = "Cancel",
                    onClick = onDismissRequest
                )

                Spacer(modifier = Modifier.padding(start = 12.dp))

                StudyOSButton(
                    text = "Save",
                    onClick = {
                        coroutineScope.launch {
                            val success = onSaveChapter(
                                name,
                                description.ifBlank { null },
                                progressValue.roundToInt()
                            )
                            if (success) {
                                onDismissRequest()
                            }
                        }
                    },
                    enabled = name.isNotBlank()
                )
            }
        }
    }
}
