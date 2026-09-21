package com.studyos.app.features.ai.ui

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Psychology
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.studyos.app.core.ui.component.StudyOSDivider
import com.studyos.app.domain.model.AiTutorMode
import com.studyos.app.domain.model.ChapterAiContext
import com.studyos.app.domain.model.PracticeDifficulty
import com.studyos.app.theme.StudyOSTheme

@Composable
fun AiTutorHeader(
    selectedMode: AiTutorMode,
    onSelectMode: (AiTutorMode) -> Unit,
    selectedDifficulty: PracticeDifficulty,
    onSelectDifficulty: (PracticeDifficulty) -> Unit,
    chapterAiContext: ChapterAiContext?,
    isContextVisible: Boolean,
    onToggleContextVisibility: () -> Unit,
    onQuickActionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.background)
    ) {
        // Mode Selector Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AiTutorMode.values().forEach { mode ->
                val isSelected = mode == selectedMode
                Box(
                    modifier = Modifier
                        .clip(shapes.statusPill)
                        .background(if (isSelected) colors.primaryText else colors.surface)
                        .border(1.dp, if (isSelected) colors.primaryText else colors.border, shapes.statusPill)
                        .clickable { onSelectMode(mode) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .semantics { this.role = Role.Button },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = mode.title,
                        style = typography.caption.copy(
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        ),
                        color = if (isSelected) colors.buttonText else colors.primaryText
                    )
                }
            }
        }

        // Practice Drill Difficulty Bar
        AnimatedVisibility(visible = selectedMode == AiTutorMode.PRACTICE_DRILL) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Difficulty:",
                    style = typography.caption,
                    color = colors.secondaryText
                )
                PracticeDifficulty.values().forEach { diff ->
                    val isSelected = diff == selectedDifficulty
                    Box(
                        modifier = Modifier
                            .clip(shapes.statusPill)
                            .background(if (isSelected) colors.cardBackground else colors.background)
                            .border(1.dp, if (isSelected) colors.accent else colors.border, shapes.statusPill)
                            .clickable { onSelectDifficulty(diff) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .semantics { this.role = Role.Button },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = diff.label,
                            style = typography.caption.copy(
                                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                            ),
                            color = if (isSelected) colors.primaryText else colors.mutedText
                        )
                    }
                }
            }
        }

        // Chapter Context Card
        if (chapterAiContext != null && isContextVisible) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(shapes.card)
                    .background(colors.cardBackground)
                    .border(1.dp, colors.border, shapes.card)
                    .padding(12.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.School,
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${chapterAiContext.chapterName} • ${chapterAiContext.subjectName}",
                                style = typography.body.copy(fontWeight = FontWeight.Medium),
                                color = colors.primaryText,
                                maxLines = 1
                            )
                        }

                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Dismiss context",
                            tint = colors.mutedText,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { onToggleContextVisibility() }
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Mastery: ${chapterAiContext.masteryPercentage}%",
                            style = typography.caption,
                            color = colors.accent
                        )
                        Text(
                            text = "Progress: ${chapterAiContext.progress}%",
                            style = typography.caption,
                            color = colors.secondaryText
                        )
                        if (chapterAiContext.quizAccuracy != null) {
                            Text(
                                text = "Quiz: ${chapterAiContext.quizAccuracy}%",
                                style = typography.caption,
                                color = colors.secondaryText
                            )
                        }
                    }

                    if (chapterAiContext.weakTopics.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Weak topics: ${chapterAiContext.weakTopics.joinToString(", ")}",
                            style = typography.caption,
                            color = colors.accent
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Context Quick Actions
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val actions = listOf(
                            "Explain" to "EXPLAIN",
                            "Teach me" to "TEACH_ME",
                            "Doubt" to "DOUBT",
                            "Example" to "EXAMPLE",
                            "Quiz me" to "QUIZ_ME",
                            "Simplify" to "SIMPLIFY"
                        )
                        actions.forEach { (label, code) ->
                            Box(
                                modifier = Modifier
                                    .clip(shapes.statusPill)
                                    .background(colors.surface)
                                    .border(1.dp, colors.border, shapes.statusPill)
                                    .clickable { onQuickActionClick(code) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .semantics { this.role = Role.Button }
                            ) {
                                Text(
                                    text = label,
                                    style = typography.caption,
                                    color = colors.primaryText
                                )
                            }
                        }
                    }
                }
            }
        }

        StudyOSDivider()
    }
}
