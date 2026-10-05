package com.studyos.app.features.subjects.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studyos.app.core.ui.component.GlassCard
import com.studyos.app.core.ui.component.GlassDialog
import com.studyos.app.core.ui.component.StudyOSButton
import com.studyos.app.core.ui.component.StudyOSOutlinedButton
import com.studyos.app.core.ui.component.StudyOSTextField
import com.studyos.app.domain.model.ExamRelevance
import com.studyos.app.domain.model.Topic
import com.studyos.app.domain.model.TopicMasteryState
import com.studyos.app.theme.StudyOSTheme

@Composable
fun SyllabusTopicsSection(
    topics: List<Topic>,
    topicError: String?,
    onAddTopic: (name: String, relevance: ExamRelevance) -> Unit,
    onUpdateMastery: (topicId: String, state: TopicMasteryState) -> Unit,
    onDeleteTopic: (topicId: String) -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    var showAddDialog by remember { mutableStateOf(false) }

    val totalTopics = topics.size
    val mastered = topics.count { it.masteryState == TopicMasteryState.MASTERED }
    val revised = topics.count { it.masteryState == TopicMasteryState.REVISED }
    val learning = topics.count { it.masteryState == TopicMasteryState.LEARNING }

    Column(modifier = modifier.fillMaxWidth()) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Syllabus Topics",
                    style = typography.sectionTitle.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.primaryText
                )
                Text(
                    text = if (totalTopics > 0) {
                        "$totalTopics topics • $mastered Mastered, $revised Revised, $learning Learning"
                    } else {
                        "Track granular topic mastery"
                    },
                    style = typography.caption,
                    color = colors.secondaryText
                )
            }

            IconButton(
                onClick = {
                    onClearError()
                    showAddDialog = true
                }
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = "Add Topic",
                    tint = colors.accent
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (topics.isEmpty()) {
            GlassCard(
                backgroundColor = colors.glassSurface,
                padding = 16.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No topics added yet",
                        style = typography.bodyMedium,
                        color = colors.primaryText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Break this chapter down into topics to track 95% target readiness.",
                        style = typography.caption,
                        color = colors.secondaryText,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    StudyOSOutlinedButton(
                        text = "+ Add First Topic",
                        onClick = {
                            onClearError()
                            showAddDialog = true
                        }
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                topics.forEach { topic ->
                    TopicCard(
                        topic = topic,
                        onUpdateMastery = { state -> onUpdateMastery(topic.id, state) },
                        onDeleteTopic = { onDeleteTopic(topic.id) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddTopicDialog(
            errorMessage = topicError,
            onDismiss = {
                showAddDialog = false
                onClearError()
            },
            onConfirm = { name, relevance ->
                onAddTopic(name, relevance)
                if (topicError == null) {
                    showAddDialog = false
                }
            }
        )
    }
}

@Composable
private fun TopicCard(
    topic: Topic,
    onUpdateMastery: (TopicMasteryState) -> Unit,
    onDeleteTopic: () -> Unit
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography

    GlassCard(
        backgroundColor = colors.glassSurface,
        padding = 12.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = topic.name,
                        style = typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = colors.primaryText,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Exam Relevance Badge
                    val relevanceColor = when (topic.examRelevance) {
                        ExamRelevance.HIGH -> colors.accent
                        ExamRelevance.MEDIUM -> colors.secondaryText
                        ExamRelevance.LOW -> colors.mutedText
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(relevanceColor.copy(alpha = 0.12f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${topic.examRelevance.name} PRIORITY",
                            style = typography.caption.copy(fontSize = 9.sp, fontWeight = FontWeight.SemiBold),
                            color = relevanceColor
                        )
                    }
                }

                IconButton(
                    onClick = onDeleteTopic,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = "Delete topic",
                        tint = colors.mutedText,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Mastery State Selector Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MasteryChip(
                    label = "Not Started",
                    isSelected = topic.masteryState == TopicMasteryState.NOT_STARTED,
                    activeColor = colors.secondaryText,
                    onClick = { onUpdateMastery(TopicMasteryState.NOT_STARTED) },
                    modifier = Modifier.weight(1f)
                )
                MasteryChip(
                    label = "Learning",
                    isSelected = topic.masteryState == TopicMasteryState.LEARNING,
                    activeColor = colors.accent,
                    onClick = { onUpdateMastery(TopicMasteryState.LEARNING) },
                    modifier = Modifier.weight(1f)
                )
                MasteryChip(
                    label = "Revised",
                    isSelected = topic.masteryState == TopicMasteryState.REVISED,
                    activeColor = colors.primaryText,
                    onClick = { onUpdateMastery(TopicMasteryState.REVISED) },
                    modifier = Modifier.weight(1f)
                )
                MasteryChip(
                    label = "Mastered",
                    isSelected = topic.masteryState == TopicMasteryState.MASTERED,
                    activeColor = colors.success,
                    onClick = { onUpdateMastery(TopicMasteryState.MASTERED) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MasteryChip(
    label: String,
    isSelected: Boolean,
    activeColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shape = RoundedCornerShape(6.dp)

    Box(
        modifier = modifier
            .clip(shape)
            .background(if (isSelected) activeColor.copy(alpha = 0.16f) else colors.background)
            .border(
                width = 1.dp,
                color = if (isSelected) activeColor else colors.border.copy(alpha = 0.4f),
                shape = shape
            )
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = typography.caption.copy(fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal),
            color = if (isSelected) activeColor else colors.secondaryText
        )
    }
}

@Composable
private fun AddTopicDialog(
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, relevance: ExamRelevance) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var relevance by remember { mutableStateOf(ExamRelevance.MEDIUM) }
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography

    GlassDialog(
        title = "Add Syllabus Topic",
        onDismissRequest = onDismiss,
        confirmButtonText = "Add Topic",
        onConfirm = {
            if (name.isNotBlank()) {
                onConfirm(name, relevance)
            }
        },
        dismissButtonText = "Cancel",
        onDismiss = onDismiss
    ) {
        StudyOSTextField(
            value = name,
            onValueChange = { name = it },
            label = "Topic Name",
            placeholder = "e.g. Newton's Second Law",
            errorMessage = errorMessage,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Exam Relevance",
            style = typography.caption,
            color = colors.secondaryText
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ExamRelevance.entries.forEach { option ->
                val selected = relevance == option
                MasteryChip(
                    label = option.name,
                    isSelected = selected,
                    activeColor = colors.accent,
                    onClick = { relevance = option },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
