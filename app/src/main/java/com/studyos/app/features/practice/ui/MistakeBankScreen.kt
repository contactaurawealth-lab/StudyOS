package com.studyos.app.features.practice.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studyos.app.core.ui.component.StudyOSButton
import com.studyos.app.core.ui.component.StudyOSEmptyState
import com.studyos.app.core.ui.component.StudyOSIconButton
import com.studyos.app.core.ui.component.StudyOSLoadingState
import com.studyos.app.core.ui.component.StudyOSMarkdown
import com.studyos.app.core.ui.component.StudyOSOutlinedButton
import com.studyos.app.features.practice.viewmodel.MistakeBankViewModel
import com.studyos.app.features.practice.viewmodel.MistakeFilter
import com.studyos.app.theme.StudyOSTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MistakeBankScreen(
    viewModel: MistakeBankViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.infoMessage) {
        uiState.infoMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearInfoMessage()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        if (uiState.isLoading) {
            StudyOSLoadingState(message = "Loading mistake bank...")
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                // Top Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StudyOSIconButton(
                        onClick = onBack,
                        contentDescription = "Back"
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = null,
                            tint = colors.primaryText,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = "Mistake Bank",
                            style = typography.screenTitle,
                            color = colors.primaryText
                        )
                        Text(
                            text = "${uiState.unresolvedCount} questions need revision",
                            style = typography.caption,
                            color = colors.secondaryText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Search Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shapes.button)
                        .background(colors.surface)
                        .border(1.dp, colors.border, shapes.button)
                        .padding(horizontal = 12.dp, vertical = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = colors.secondaryText,
                            modifier = Modifier.size(18.dp)
                        )
                        TextField(
                            value = uiState.searchQuery,
                            onValueChange = viewModel::onSearchQueryChanged,
                            placeholder = {
                                Text(
                                    text = "Search questions or topics...",
                                    style = typography.caption,
                                    color = colors.mutedText
                                )
                            },
                            textStyle = typography.body.copy(color = colors.primaryText),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Filter Status Chips: Unresolved, All, Resolved
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MistakeFilter.values().forEach { filter ->
                        val isSelected = uiState.selectedFilter == filter
                        val count = when (filter) {
                            MistakeFilter.UNRESOLVED -> uiState.unresolvedCount
                            MistakeFilter.RESOLVED -> uiState.resolvedCount
                            MistakeFilter.ALL -> uiState.mistakes.size
                        }
                        Box(
                            modifier = Modifier
                                .clip(shapes.button)
                                .background(if (isSelected) colors.cardBackground else colors.surface)
                                .border(1.dp, if (isSelected) colors.accent else colors.border, shapes.button)
                                .clickable { viewModel.setFilter(filter) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${filter.label} ($count)",
                                style = typography.caption,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) colors.primaryText else colors.secondaryText
                            )
                        }
                    }
                }

                // Subject Filter Chips
                if (uiState.subjects.size > 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val isAllSelected = uiState.selectedSubjectId == null
                        Box(
                            modifier = Modifier
                                .clip(shapes.button)
                                .background(if (isAllSelected) colors.cardBackground else colors.surface)
                                .border(1.dp, if (isAllSelected) colors.accent else colors.border, shapes.button)
                                .clickable { viewModel.setSubjectFilter(null) }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "All Subjects",
                                style = typography.caption.copy(fontSize = 11.sp),
                                color = if (isAllSelected) colors.primaryText else colors.secondaryText
                            )
                        }

                        uiState.subjects.forEach { subject ->
                            val isSelected = uiState.selectedSubjectId == subject.id
                            Box(
                                modifier = Modifier
                                    .clip(shapes.button)
                                    .background(if (isSelected) colors.cardBackground else colors.surface)
                                    .border(1.dp, if (isSelected) colors.accent else colors.border, shapes.button)
                                    .clickable { viewModel.setSubjectFilter(subject.id) }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = subject.name,
                                    style = typography.caption.copy(fontSize = 11.sp),
                                    color = if (isSelected) colors.primaryText else colors.secondaryText
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Mistakes List
                val mistakes = uiState.filteredMistakes
                if (mistakes.isEmpty()) {
                    StudyOSEmptyState(
                        title = "No mistakes match criteria",
                        description = "Keep taking practice quizzes — when you miss a question, it is automatically captured here.",
                        actionButtonText = null,
                        onActionClick = {}
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(mistakes, key = { it.id }) { mistake ->
                            MistakeCard(
                                mistake = mistake,
                                onResolve = { viewModel.toggleResolve(mistake) },
                                onConvertToFlashcard = { viewModel.convertToFlashcard(mistake) },
                                onExplainWithAi = { viewModel.explainWithAi(mistake) }
                            )
                        }
                    }
                }
            }
        }

        // Mistake AI Explanation Bottom Sheet
        if (uiState.selectedMistakeForAi != null) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            val selected = uiState.selectedMistakeForAi!!

            ModalBottomSheet(
                onDismissRequest = { viewModel.closeAiExplanation() },
                sheetState = sheetState,
                containerColor = colors.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = "Concept Breakdown",
                        style = typography.screenTitle,
                        color = colors.primaryText
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = selected.question,
                        style = typography.secondary,
                        fontWeight = FontWeight.Medium,
                        color = colors.primaryText
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Your Answer: ${selected.studentAnswer}",
                        style = typography.caption,
                        color = colors.mutedText
                    )

                    Text(
                        text = "Correct Answer: ${selected.correctAnswer}",
                        style = typography.caption,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.primaryText
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shapes.card)
                            .background(colors.cardBackground)
                            .border(1.dp, colors.border, shapes.card)
                            .padding(14.dp)
                    ) {
                        if (uiState.isExplaining && uiState.aiExplanation.isNullOrBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = colors.accent,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Analyzing question and underlying concept...",
                                    style = typography.caption,
                                    color = colors.secondaryText
                                )
                            }
                        } else if (!uiState.aiExplanation.isNullOrBlank()) {
                            StudyOSMarkdown(content = uiState.aiExplanation!!)
                        } else {
                            Text(
                                text = "No explanation generated yet.",
                                style = typography.caption,
                                color = colors.secondaryText
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StudyOSOutlinedButton(
                            text = "Close",
                            onClick = { viewModel.closeAiExplanation() },
                            modifier = Modifier.weight(1f)
                        )

                        StudyOSButton(
                            text = "Convert to Flashcard",
                            onClick = {
                                viewModel.convertToFlashcard(selected)
                                viewModel.closeAiExplanation()
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
