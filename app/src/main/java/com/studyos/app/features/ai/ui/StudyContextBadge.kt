package com.studyos.app.features.ai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.studyos.app.domain.model.StudyContext
import com.studyos.app.theme.StudyOSTheme

@Composable
fun StudyContextBadge(
    context: StudyContext,
    onOpenChapter: ((chapterId: String) -> Unit)? = null,
    onClearContext: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (!context.hasContext) return

    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    val labelText = buildString {
        if (!context.subjectName.isNullOrBlank()) {
            append(context.subjectName)
        }
        if (!context.chapterName.isNullOrBlank()) {
            if (isNotEmpty()) append(" • ")
            append(context.chapterName)
        }
        if (context.chapterProgress != null) {
            append(" (${context.chapterProgress}%)")
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shapes.surface)
            .background(colors.surface)
            .border(1.dp, colors.border, shapes.surface)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (onOpenChapter != null && context.chapterId != null) {
                            Modifier.clickable { onOpenChapter(context.chapterId) }
                        } else Modifier
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.School,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(16.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = labelText,
                    style = typography.caption,
                    color = colors.primaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                if (onOpenChapter != null && context.chapterId != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = "View chapter",
                        tint = colors.mutedText,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            if (onClearContext != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(shapes.statusPill)
                        .clickable { onClearContext() }
                        .semantics { this.role = Role.Button },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Remove chapter context",
                        tint = colors.mutedText,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
