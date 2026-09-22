package com.studyos.app.features.ai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.studyos.app.domain.model.QuickAction
import com.studyos.app.theme.StudyOSTheme

@Composable
fun QuickActionChips(
    onActionClick: (QuickAction) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        QuickAction.values().forEach { action ->
            Box(
                modifier = Modifier
                    .clip(shapes.statusPill)
                    .background(colors.cardBackground.copy(alpha = 0.6f))
                    .border(0.5.dp, colors.border.copy(alpha = 0.3f), shapes.statusPill)
                    .clickable(enabled = enabled) { onActionClick(action) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .semantics { this.role = Role.Button },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = action.label,
                    style = typography.caption,
                    color = if (enabled) colors.primaryText else colors.mutedText
                )
            }
        }
    }
}
