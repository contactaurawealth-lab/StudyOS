package com.studyos.app.core.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.studyos.app.theme.StudyOSTheme

@Composable
fun StudyOSButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    contentDescription: String? = null
) {
    val colors = StudyOSTheme.colors
    val shapes = StudyOSTheme.shapes
    val typography = StudyOSTheme.typography

    Button(
        onClick = onClick,
        modifier = modifier
            .defaultMinSize(minWidth = 88.dp, minHeight = 44.dp)
            .height(46.dp)
            .semantics {
                this.role = Role.Button
                if (contentDescription != null) {
                    this.contentDescription = contentDescription
                }
            },
        enabled = enabled && !isLoading,
        shape = shapes.button,
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.primaryText,
            contentColor = colors.background,
            disabledContainerColor = colors.border,
            disabledContentColor = colors.mutedText
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
        elevation = null
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = colors.background
            )
        } else {
            Text(
                text = text,
                style = typography.button,
                color = if (enabled) colors.background else colors.mutedText
            )
        }
    }
}

@Composable
fun StudyOSOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    contentDescription: String? = null
) {
    val colors = StudyOSTheme.colors
    val shapes = StudyOSTheme.shapes
    val typography = StudyOSTheme.typography

    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .defaultMinSize(minWidth = 88.dp, minHeight = 44.dp)
            .height(46.dp)
            .semantics {
                this.role = Role.Button
                if (contentDescription != null) {
                    this.contentDescription = contentDescription
                }
            },
        enabled = enabled && !isLoading,
        shape = shapes.button,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Transparent,
            contentColor = colors.primaryText,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = colors.mutedText
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (enabled) colors.border else colors.border.copy(alpha = 0.5f)
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
        elevation = null
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = colors.primaryText
            )
        } else {
            Text(
                text = text,
                style = typography.button,
                color = if (enabled) colors.primaryText else colors.mutedText
            )
        }
    }
}

@Composable
fun StudyOSIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String? = null,
    content: @Composable () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(44.dp)
            .semantics {
                this.role = Role.Button
                if (contentDescription != null) {
                    this.contentDescription = contentDescription
                }
            },
        enabled = enabled
    ) {
        Box(contentAlignment = Alignment.Center) {
            content()
        }
    }
}

@Composable
fun StudyOSTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String? = null
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography

    androidx.compose.material3.TextButton(
        onClick = onClick,
        modifier = modifier
            .defaultMinSize(minHeight = 44.dp)
            .semantics {
                this.role = Role.Button
                if (contentDescription != null) {
                    this.contentDescription = contentDescription
                }
            },
        enabled = enabled
    ) {
        Text(
            text = text,
            style = typography.button,
            color = if (enabled) colors.accent else colors.mutedText
        )
    }
}
