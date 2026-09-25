package com.studyos.app.features.onboarding.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.studyos.app.core.ui.component.StudyOSBottomSheet
import com.studyos.app.core.ui.component.StudyOSButton
import com.studyos.app.core.ui.component.StudyOSOutlinedButton
import com.studyos.app.core.ui.component.StudyOSTextField
import com.studyos.app.theme.StudyOSTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSubjectBottomSheet(
    onDismissRequest: () -> Unit,
    onAddSubject: (String) -> Boolean,
    errorMessage: String? = null,
    onClearError: () -> Unit = {}
) {
    var subjectName by remember { mutableStateOf("") }
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography

    StudyOSBottomSheet(
        onDismissRequest = onDismissRequest
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Add subject",
                style = typography.sectionTitle,
                color = colors.primaryText
            )

            Spacer(modifier = Modifier.height(16.dp))

            StudyOSTextField(
                value = subjectName,
                onValueChange = {
                    subjectName = it
                    if (errorMessage != null) onClearError()
                },
                label = "Subject name",
                placeholder = "e.g. Economics, Psychology",
                errorMessage = errorMessage,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (onAddSubject(subjectName)) {
                            onDismissRequest()
                        }
                    }
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.weight(1f))
                StudyOSOutlinedButton(
                    text = "Cancel",
                    onClick = onDismissRequest
                )
                Spacer(modifier = Modifier.width(10.dp))
                StudyOSButton(
                    text = "Add",
                    onClick = {
                        if (onAddSubject(subjectName)) {
                            onDismissRequest()
                        }
                    }
                )
            }
        }
    }
}
