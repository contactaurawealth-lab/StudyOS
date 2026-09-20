package com.studyos.app.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.studyos.app.theme.StudyOSTheme

@Composable
fun StudyOSDialog(
    onDismissRequest: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    text: String? = null,
    confirmButtonText: String = "Confirm",
    onConfirm: () -> Unit,
    dismissButtonText: String? = "Cancel",
    onDismiss: (() -> Unit)? = onDismissRequest,
    isDestructive: Boolean = false,
    content: @Composable (() -> Unit)? = null
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth(0.9f)
                .clip(shapes.dialog)
                .background(colors.surface)
                .border(1.dp, colors.border, shapes.dialog)
                .padding(24.dp)
        ) {
            Column {
                Text(
                    text = title,
                    style = typography.sectionTitle,
                    color = colors.primaryText
                )

                if (!text.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = text,
                        style = typography.body,
                        color = colors.secondaryText
                    )
                }

                if (content != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    content()
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    if (dismissButtonText != null && onDismiss != null) {
                        StudyOSOutlinedButton(
                            text = dismissButtonText,
                            onClick = onDismiss
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                    StudyOSButton(
                        text = confirmButtonText,
                        onClick = onConfirm
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyOSBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = StudyOSTheme.colors
    val shapes = StudyOSTheme.shapes

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        shape = shapes.bottomSheet,
        containerColor = colors.surface,
        contentColor = colors.primaryText,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(shapes.statusPill)
                    .background(colors.border)
            )
        },
        content = content
    )
}

@Composable
fun StudyOSConfirmationDialog(
    title: String,
    message: String,
    confirmButtonText: String = "Confirm",
    dismissButtonText: String = "Cancel",
    isDestructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    StudyOSDialog(
        onDismissRequest = onDismiss,
        title = title,
        text = message,
        confirmButtonText = confirmButtonText,
        onConfirm = onConfirm,
        dismissButtonText = dismissButtonText,
        onDismiss = onDismiss,
        isDestructive = isDestructive
    )
}

@Composable
fun StudyOSTextDialog(
    title: String,
    initialValue: String = "",
    placeholder: String = "",
    confirmButtonText: String = "Save",
    dismissButtonText: String = "Cancel",
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    errorMessage: String? = null
) {
    var textValue by androidx.compose.runtime.remember(initialValue) {
        androidx.compose.runtime.mutableStateOf(initialValue)
    }

    StudyOSDialog(
        onDismissRequest = onDismiss,
        title = title,
        confirmButtonText = confirmButtonText,
        onConfirm = {
            if (textValue.isNotBlank()) {
                onConfirm(textValue)
            }
        },
        dismissButtonText = dismissButtonText,
        onDismiss = onDismiss,
        content = {
            StudyOSTextField(
                value = textValue,
                onValueChange = { textValue = it },
                placeholder = placeholder,
                errorMessage = errorMessage,
                modifier = Modifier.fillMaxWidth()
            )
        }
    )
}
