package com.studyos.app.features.ai.ui

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.studyos.app.core.ui.component.StudyOSButton
import com.studyos.app.core.ui.component.StudyOSConfirmationDialog
import com.studyos.app.core.ui.component.StudyOSDivider
import com.studyos.app.core.ui.component.StudyOSIconButton
import com.studyos.app.core.util.DateTimeUtils
import com.studyos.app.domain.model.AiConversationItem
import com.studyos.app.theme.StudyOSTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationHistorySheet(
    conversations: List<AiConversationItem>,
    currentConversationId: String?,
    onSelectConversation: (String) -> Unit,
    onNewConversation: () -> Unit,
    onDeleteConversation: (String) -> Unit,
    onDismissRequest: () -> Unit
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var conversationToDelete by remember { mutableStateOf<AiConversationItem?>(null) }

    if (conversationToDelete != null) {
        StudyOSConfirmationDialog(
            title = "Delete Conversation",
            message = "Are you sure you want to delete this conversation? Its messages will be permanently removed.",
            confirmButtonText = "Delete",
            dismissButtonText = "Cancel",
            onConfirm = {
                conversationToDelete?.let { onDeleteConversation(it.conversation.id) }
                conversationToDelete = null
            },
            onDismiss = { conversationToDelete = null }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = colors.surface,
        contentColor = colors.primaryText,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Conversations",
                    style = typography.sectionTitle,
                    color = colors.primaryText
                )

                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Close",
                    tint = colors.secondaryText,
                    modifier = Modifier
                        .size(22.dp)
                        .clickable { onDismissRequest() }
                        .semantics { this.role = Role.Button }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // New Chat Button
            StudyOSButton(
                text = "+ New conversation",
                onClick = {
                    onNewConversation()
                    onDismissRequest()
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))
            StudyOSDivider()
            Spacer(modifier = Modifier.height(12.dp))

            // Conversations List
            if (conversations.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No previous conversations.",
                        style = typography.body,
                        color = colors.mutedText
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    itemsIndexed(conversations, key = { _, item -> item.conversation.id }) { index, item ->
                        val isSelected = item.conversation.id == currentConversationId

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(shapes.surface)
                                .background(if (isSelected) colors.background else colors.surface)
                                .border(
                                    width = if (isSelected) 1.dp else 0.dp,
                                    color = if (isSelected) colors.accent else colors.border.copy(alpha = 0f),
                                    shape = shapes.surface
                                )
                                .clickable {
                                    onSelectConversation(item.conversation.id)
                                    onDismissRequest()
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ChatBubbleOutline,
                                    contentDescription = null,
                                    tint = if (isSelected) colors.accent else colors.mutedText,
                                    modifier = Modifier.size(18.dp)
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.conversation.title,
                                        style = typography.bodyMedium,
                                        color = colors.primaryText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    val subtitle = buildString {
                                        if (!item.subjectName.isNullOrBlank()) {
                                            append(item.subjectName)
                                            append(" • ")
                                        }
                                        append(DateTimeUtils.formatDateShort(item.conversation.updatedAt))
                                    }

                                    Text(
                                        text = subtitle,
                                        style = typography.caption,
                                        color = colors.secondaryText
                                    )
                                }
                            }

                            // Delete conversation button
                            StudyOSIconButton(
                                onClick = { conversationToDelete = item },
                                contentDescription = "Delete conversation"
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteOutline,
                                    contentDescription = null,
                                    tint = colors.mutedText,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        if (index < conversations.lastIndex) {
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }
        }
    }
}
