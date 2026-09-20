package com.studyos.app.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.studyos.app.theme.StudyOSTheme

@Composable
fun StudyOSProgressBar(
    progress: Int,
    modifier: Modifier = Modifier,
    height: Dp = 4.dp,
    trackColor: Color = StudyOSTheme.colors.border,
    progressColor: Color = StudyOSTheme.colors.accent
) {
    val clampedProgress = (progress.coerceIn(0, 100)) / 100f
    val shapes = StudyOSTheme.shapes

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(shapes.pill)
            .background(trackColor)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(clampedProgress, 0f..1f)
            }
    ) {
        if (clampedProgress > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(clampedProgress)
                    .clip(shapes.pill)
                    .background(progressColor)
            )
        }
    }
}

@Composable
fun StudyOSListItem(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    isSelected: Boolean = false,
    enabled: Boolean = true
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    val backgroundColor = when {
        isSelected -> colors.surface
        else -> Color.Transparent
    }

    val clickableModifier = if (onClick != null && enabled) {
        Modifier
            .clip(shapes.surface)
            .clickable(onClick = onClick)
            .semantics { this.role = Role.Button }
    } else {
        Modifier.clip(shapes.surface)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .then(clickableModifier)
            .background(backgroundColor)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingContent != null) {
            leadingContent()
            Spacer(modifier = Modifier.width(14.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = if (isSelected) typography.bodyMedium else typography.body,
                color = if (enabled) colors.primaryText else colors.mutedText
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = typography.secondary,
                    color = colors.secondaryText,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        if (trailingContent != null) {
            Spacer(modifier = Modifier.width(12.dp))
            trailingContent()
        }
    }
}

@Composable
fun StudyOSSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography

    Column(modifier = modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            text = title,
            style = typography.sectionTitle,
            color = colors.primaryText
        )
        if (!description.isNullOrBlank()) {
            Text(
                text = description,
                style = typography.secondary,
                color = colors.secondaryText,
                modifier = Modifier.padding(top = 3.dp)
            )
        }
    }
}

@Composable
fun StudyOSDivider(
    modifier: Modifier = Modifier,
    color: Color = StudyOSTheme.colors.border,
    thickness: Float = 1f
) {
    HorizontalDivider(
        modifier = modifier.fillMaxWidth(),
        thickness = thickness.dp,
        color = color
    )
}
