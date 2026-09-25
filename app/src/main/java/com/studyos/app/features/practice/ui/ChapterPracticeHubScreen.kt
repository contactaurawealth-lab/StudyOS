package com.studyos.app.features.practice.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.platform.LocalContext
data class ParsedCard(val front: String, val back: String)

object FlashcardImportParser {
    fun parseText(text: String): List<ParsedCard> {
        return text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val delimiter = when {
                    "\t" in line -> "\t"
                    ";" in line -> ";"
                    "," in line -> ","
                    "-" in line -> "-"
                    else -> null
                }
                if (delimiter != null) {
                    val parts = line.split(delimiter, limit = 2)
                    if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                        ParsedCard(parts[0].trim(), parts[1].trim())
                    } else null
                } else null
            }.toList()
    }

    fun parseFromUri(context: android.content.Context, uri: android.net.Uri): List<ParsedCard> {
        return try {
            val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
            parseText(content)
        } catch (_: Exception) {
            emptyList()
        }
    }
}
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.studyos.app.core.ui.component.StudyOSButton
import com.studyos.app.core.ui.component.StudyOSEmptyState
import com.studyos.app.core.ui.component.StudyOSIconButton
import com.studyos.app.core.ui.component.StudyOSLoadingState
import com.studyos.app.core.ui.component.StudyOSMarkdown
import com.studyos.app.core.ui.component.StudyOSOutlinedButton
import com.studyos.app.core.ui.component.StudyOSProgressBar
import com.studyos.app.core.ui.component.StudyOSTextField
import com.studyos.app.domain.model.ChapterPracticeSummary
import com.studyos.app.domain.model.Flashcard
import com.studyos.app.domain.model.FlashcardCategory
import com.studyos.app.domain.model.FlashcardDifficulty
import com.studyos.app.domain.model.Mistake
import com.studyos.app.domain.model.Note
import com.studyos.app.domain.model.QuizDifficulty
import com.studyos.app.domain.model.WeakTopic
import com.studyos.app.domain.model.WeakTopicAction
import com.studyos.app.features.practice.viewmodel.ChapterPracticeHubViewModel
import com.studyos.app.features.practice.viewmodel.PracticeHubTab
import com.studyos.app.theme.StudyOSTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterPracticeHubScreen(
    viewModel: ChapterPracticeHubViewModel,
    onBack: () -> Unit,
    onOpenNote: (noteId: String?, chapterId: String) -> Unit,
    onStartFlashcards: (chapterId: String) -> Unit,
    onStartQuiz: (quizId: String) -> Unit,
    onOpenMistakeBank: () -> Unit,
    onStartAudioWalk: () -> Unit = {},
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

    LaunchedEffect(uiState.createdQuizId) {
        uiState.createdQuizId?.let { quizId ->
            viewModel.clearCreatedQuizId()
            onStartQuiz(quizId)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        if (uiState.isLoading) {
            StudyOSLoadingState(message = "Loading practice hub...")
        } else {
            val summary = uiState.summary
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                // Top App Bar
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

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = summary?.chapterName ?: "Practice Hub",
                            style = typography.screenTitle,
                            color = colors.primaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = summary?.subjectName ?: "Practice & Revision",
                            style = typography.caption,
                            color = colors.secondaryText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Hub Navigation Tabs
                val tabScrollState = rememberScrollState()

                LaunchedEffect(uiState.selectedTab) {
                    val tabIndex = PracticeHubTab.values().indexOf(uiState.selectedTab)
                    if (tabIndex > 0) {
                        tabScrollState.animateScrollTo(tabIndex * 180)
                    } else {
                        tabScrollState.animateScrollTo(0)
                    }
                }

                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shapes.button)
                            .background(colors.surface)
                            .border(1.dp, colors.border, shapes.button)
                            .padding(4.dp)
                            .horizontalScroll(tabScrollState),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        PracticeHubTab.values().forEach { tab ->
                            val isSelected = uiState.selectedTab == tab
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) colors.cardBackground else Color.Transparent)
                                    .border(
                                        width = if (isSelected) 1.dp else 0.dp,
                                        color = if (isSelected) colors.border.copy(alpha = 0.5f) else Color.Transparent,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .clickable { viewModel.selectTab(tab) }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tab.label,
                                    style = typography.caption,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) colors.primaryText else colors.secondaryText,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    if (tabScrollState.canScrollForward) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .width(28.dp)
                                .height(44.dp)
                                .clip(RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(Color.Transparent, colors.surface)
                                    )
                                )
                        )
                    }
                    if (tabScrollState.canScrollBackward) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .width(28.dp)
                                .height(44.dp)
                                .clip(RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(colors.surface, Color.Transparent)
                                    )
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Content according to selected tab
                Box(modifier = Modifier.weight(1f)) {
                    when (uiState.selectedTab) {
                        PracticeHubTab.OVERVIEW -> {
                            PracticeOverviewTab(
                                summary = summary,
                                onStudyFlashcards = { onStartFlashcards(uiState.chapterId) },
                                onTakeQuiz = { viewModel.openCreateQuizSheet() },
                                onCreateNote = { onOpenNote(null, uiState.chapterId) },
                                onSelectTab = { viewModel.selectTab(it) }
                            )
                        }
                        PracticeHubTab.NOTES -> {
                            PracticeNotesTab(
                                notes = uiState.notes,
                                onOpenNote = { noteId -> onOpenNote(noteId, uiState.chapterId) },
                                onNewNote = { onOpenNote(null, uiState.chapterId) },
                                onTogglePin = viewModel::toggleNotePin,
                                onDeleteNote = viewModel::deleteNote
                            )
                        }
                        PracticeHubTab.FLASHCARDS -> {
                            PracticeFlashcardsTab(
                                flashcards = uiState.flashcards,
                                onStartStudy = { onStartFlashcards(uiState.chapterId) },
                                onAddFlashcard = { viewModel.openCreateFlashcardSheet() },
                                onBulkImport = { viewModel.openBulkImportSheet() },
                                onDeleteCard = viewModel::deleteFlashcard
                            )
                        }
                        PracticeHubTab.QUIZ -> {
                            PracticeQuizTab(
                                summary = summary,
                                isGenerating = uiState.isGeneratingAiQuiz,
                                onOpenCreateQuiz = { viewModel.openCreateQuizSheet() }
                            )
                        }
                        PracticeHubTab.MISTAKES -> {
                            PracticeMistakesTab(
                                mistakes = uiState.mistakes,
                                onResolve = viewModel::resolveMistake,
                                onConvertToFlashcard = viewModel::convertMistakeToFlashcard,
                                onExplainWithAi = viewModel::explainMistakeWithAi,
                                onOpenGlobalMistakes = onOpenMistakeBank
                            )
                        }
                    }
                }
            }
        }

        // Bottom Sheet: Create Flashcard
        if (uiState.isCreateFlashcardSheetOpen) {
            CreateFlashcardBottomSheet(
                onDismiss = { viewModel.closeCreateFlashcardSheet() },
                onSave = { q, a, diff -> viewModel.createFlashcard(q, a, diff) }
            )
        }

        // Bottom Sheet: Bulk Import Flashcards
        if (uiState.isBulkImportSheetOpen) {
            BulkImportFlashcardsBottomSheet(
                onDismiss = { viewModel.closeBulkImportSheet() },
                onImport = { cards, diff -> viewModel.bulkImportFlashcards(cards, diff) }
            )
        }

        // Bottom Sheet: Create AI Quiz
        if (uiState.isCreateQuizSheetOpen) {
            CreateQuizBottomSheet(
                isGenerating = uiState.isGeneratingAiQuiz,
                onDismiss = { viewModel.closeCreateQuizSheet() },
                onGenerate = { count, diff -> viewModel.generateAiQuiz(count, diff) }
            )
        }

        // Bottom Sheet: AI Explanation for Mistake
        if (uiState.selectedMistakeForAi != null) {
            MistakeAiExplanationBottomSheet(
                mistake = uiState.selectedMistakeForAi!!,
                explanation = uiState.aiExplanation,
                isExplaining = uiState.isExplainingMistake,
                onDismiss = { viewModel.closeMistakeAiExplanation() },
                onConvertToFlashcard = {
                    viewModel.convertMistakeToFlashcard(uiState.selectedMistakeForAi!!)
                    viewModel.closeMistakeAiExplanation()
                }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun PracticeOverviewTab(
    summary: ChapterPracticeSummary?,
    onStudyFlashcards: () -> Unit,
    onTakeQuiz: () -> Unit,
    onCreateNote: () -> Unit,
    onSelectTab: (PracticeHubTab) -> Unit
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    if (summary == null) return

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Readiness & Progress Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shapes.surface)
                    .background(colors.surface)
                    .border(1.dp, colors.border, shapes.surface)
                    .padding(18.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Chapter Progress",
                            style = typography.sectionTitle,
                            color = colors.primaryText
                        )
                        Text(
                            text = "${summary.progress}%",
                            style = typography.sectionTitle,
                            color = colors.accent
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    StudyOSProgressBar(
                        progress = summary.progress,
                        height = 6.dp,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Metrics row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricBadge(
                            title = "Quiz Accuracy",
                            value = if (summary.quizAccuracy != null) "${summary.quizAccuracy}%" else "—",
                            modifier = Modifier.weight(1f)
                        )
                        MetricBadge(
                            title = "Due Cards",
                            value = summary.flashcardsDue.toString(),
                            modifier = Modifier.weight(1f)
                        )
                        MetricBadge(
                            title = "Mistakes",
                            value = summary.mistakesCount.toString(),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Quick Actions
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StudyOSButton(
                    text = if (summary.flashcardsDue > 0) "Study Due (${summary.flashcardsDue})" else "Study Flashcards",
                    onClick = onStudyFlashcards,
                    modifier = Modifier.weight(1.2f)
                )

                StudyOSOutlinedButton(
                    text = "Quiz",
                    onClick = onTakeQuiz,
                    modifier = Modifier.weight(0.8f)
                )

                StudyOSOutlinedButton(
                    text = "Note",
                    onClick = onCreateNote,
                    modifier = Modifier.weight(0.8f)
                )
            }
        }

        // Weak Topics Detected
        if (summary.weakTopics.isNotEmpty()) {
            item {
                Text(
                    text = "Areas Needing Attention",
                    style = typography.sectionTitle,
                    color = colors.primaryText
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    summary.weakTopics.forEach { weak ->
                        WeakTopicCard(weakTopic = weak)
                    }
                }
            }
        }

        // Connected Learning Loop Checklist
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shapes.surface)
                    .background(colors.surface)
                    .border(1.dp, colors.border, shapes.surface)
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "Connected Learning Loop",
                        style = typography.secondary,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.primaryText
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Follow the full retention loop: Notes → Flashcards → Practice Quiz → Analyze Mistakes.",
                        style = typography.caption,
                        color = colors.secondaryText
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    LearningLoopStepRow(
                        title = "1. Make Study Notes",
                        subtitle = "${summary.notesCount} notes created",
                        isCompleted = summary.notesCount > 0,
                        onClick = { onSelectTab(PracticeHubTab.NOTES) }
                    )
                    LearningLoopStepRow(
                        title = "2. Flashcards Review",
                        subtitle = "${summary.flashcardsDue} cards currently due",
                        isCompleted = summary.flashcardsDue == 0 && summary.progress > 0,
                        onClick = { onSelectTab(PracticeHubTab.FLASHCARDS) }
                    )
                    LearningLoopStepRow(
                        title = "3. Practice Quiz",
                        subtitle = if (summary.quizAccuracy != null) "Last accuracy: ${summary.quizAccuracy}%" else "No attempts yet",
                        isCompleted = summary.quizAccuracy != null && summary.quizAccuracy >= 70,
                        onClick = { onSelectTab(PracticeHubTab.QUIZ) }
                    )
                    LearningLoopStepRow(
                        title = "4. Review Mistakes",
                        subtitle = "${summary.mistakesCount} unresolved questions",
                        isCompleted = summary.mistakesCount == 0,
                        onClick = { onSelectTab(PracticeHubTab.MISTAKES) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricBadge(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    Box(
        modifier = modifier
            .clip(shapes.card)
            .background(colors.cardBackground)
            .border(1.dp, colors.border, shapes.card)
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                style = typography.sectionTitle,
                color = colors.primaryText
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                style = typography.caption,
                color = colors.secondaryText,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun WeakTopicCard(weakTopic: WeakTopic) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.card)
            .background(colors.surface)
            .border(1.dp, colors.border, shapes.card)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = weakTopic.topic,
                    style = typography.secondary,
                    fontWeight = FontWeight.Medium,
                    color = colors.primaryText
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Accuracy: ${weakTopic.accuracyPercentage}% (${weakTopic.questionsAttempted} attempts)",
                    style = typography.caption,
                    color = colors.secondaryText
                )
            }

            Box(
                modifier = Modifier
                    .clip(shapes.button)
                    .background(colors.cardBackground)
                    .border(1.dp, colors.border, shapes.button)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = when (weakTopic.recommendedAction) {
                        WeakTopicAction.REVISE -> "Revise Notes"
                        WeakTopicAction.FLASHCARDS -> "Drill Flashcards"
                        WeakTopicAction.PRACTICE_QUIZ -> "Practice Quiz"
                    },
                    style = typography.caption,
                    fontWeight = FontWeight.Medium,
                    color = colors.accent
                )
            }
        }
    }
}

@Composable
private fun LearningLoopStepRow(
    title: String,
    subtitle: String,
    isCompleted: Boolean,
    onClick: () -> Unit
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isCompleted) colors.accent else colors.cardBackground)
                    .border(1.dp, if (isCompleted) colors.accent else colors.border, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = null,
                        tint = colors.buttonText,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = title,
                    style = typography.secondary,
                    fontWeight = FontWeight.Medium,
                    color = colors.primaryText
                )
                Text(
                    text = subtitle,
                    style = typography.caption,
                    color = colors.secondaryText
                )
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
            contentDescription = null,
            tint = colors.secondaryText,
            modifier = Modifier.size(16.dp)
        )
    }
}

// ----------------------------------------------------
// NOTES TAB
// ----------------------------------------------------

@Composable
private fun PracticeNotesTab(
    notes: List<Note>,
    onOpenNote: (noteId: String?) -> Unit,
    onNewNote: () -> Unit,
    onTogglePin: (String, Boolean) -> Unit,
    onDeleteNote: (String) -> Unit
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${notes.size} Study Notes",
                style = typography.secondary,
                color = colors.secondaryText
            )

            StudyOSButton(
                text = "+ New Note",
                onClick = onNewNote
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (notes.isEmpty()) {
            StudyOSEmptyState(
                title = "No notes yet",
                description = "Summarize concepts, write markdown notes, or generate notes with AI.",
                actionButtonText = "Create Note",
                onActionClick = onNewNote
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(notes, key = { it.id }) { note ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shapes.card)
                            .background(colors.surface)
                            .border(1.dp, colors.border, shapes.card)
                            .clickable { onOpenNote(note.id) }
                            .padding(16.dp)
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
                                    if (note.isPinned) {
                                        Icon(
                                            imageVector = Icons.Outlined.PushPin,
                                            contentDescription = "Pinned",
                                            tint = colors.accent,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Text(
                                        text = note.title,
                                        style = typography.secondary,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.primaryText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Row {
                                    StudyOSIconButton(
                                        onClick = { onTogglePin(note.id, note.isPinned) },
                                        contentDescription = "Pin"
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.PushPin,
                                            contentDescription = null,
                                            tint = if (note.isPinned) colors.accent else colors.secondaryText,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    StudyOSIconButton(
                                        onClick = { onDeleteNote(note.id) },
                                        contentDescription = "Delete"
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.DeleteOutline,
                                            contentDescription = null,
                                            tint = colors.secondaryText,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = note.content,
                                style = typography.body,
                                color = colors.secondaryText,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                            Text(
                                text = dateFormat.format(Date(note.updatedAt)),
                                style = typography.caption,
                                color = colors.mutedText
                            )
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// FLASHCARDS TAB
// ----------------------------------------------------

@Composable
private fun PracticeFlashcardsTab(
    flashcards: List<Flashcard>,
    onStartStudy: () -> Unit,
    onAddFlashcard: () -> Unit,
    onBulkImport: () -> Unit,
    onDeleteCard: (String) -> Unit
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes
    val now = System.currentTimeMillis()

    val dueCards = flashcards.filter { it.getCategory(now) == FlashcardCategory.DUE }
    val masteredCards = flashcards.filter { it.getCategory(now) == FlashcardCategory.MASTERED }
    val learningCards = flashcards.filter { it.getCategory(now) == FlashcardCategory.LEARNING || it.getCategory(now) == FlashcardCategory.NEW }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StudyOSButton(
                text = if (dueCards.isNotEmpty()) "Study Due (${dueCards.size})" else "Study All (${flashcards.size})",
                onClick = onStartStudy,
                enabled = flashcards.isNotEmpty(),
                modifier = Modifier.weight(1.2f)
            )

            StudyOSOutlinedButton(
                text = "+ Add",
                onClick = onAddFlashcard,
                modifier = Modifier.weight(0.8f)
            )

            StudyOSOutlinedButton(
                text = "📥 Import",
                onClick = onBulkImport,
                modifier = Modifier.weight(1.0f)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Distribution stats
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shapes.card)
                .background(colors.surface)
                .border(1.dp, colors.border, shapes.card)
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            FlashcardStatItem(label = "Due", count = dueCards.size)
            FlashcardStatItem(label = "Learning", count = learningCards.size)
            FlashcardStatItem(label = "Mastered", count = masteredCards.size)
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (flashcards.isEmpty()) {
            StudyOSEmptyState(
                title = "No flashcards yet",
                description = "Add flashcards manually or generate them instantly from study notes with AI.",
                actionButtonText = "Create Flashcard",
                onActionClick = onAddFlashcard
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(flashcards, key = { it.id }) { card ->
                    val category = card.getCategory(now)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shapes.card)
                            .background(colors.surface)
                            .border(1.dp, colors.border, shapes.card)
                            .padding(14.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(shapes.button)
                                        .background(colors.cardBackground)
                                        .border(1.dp, colors.border, shapes.button)
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = category.name,
                                        style = typography.caption,
                                        color = if (category == FlashcardCategory.DUE) colors.accent else colors.secondaryText
                                    )
                                }

                                StudyOSIconButton(
                                    onClick = { onDeleteCard(card.id) },
                                    contentDescription = "Delete card"
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.DeleteOutline,
                                        contentDescription = null,
                                        tint = colors.secondaryText,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Q: ${card.question}",
                                style = typography.secondary,
                                fontWeight = FontWeight.Medium,
                                color = colors.primaryText
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "A: ${card.answer}",
                                style = typography.body,
                                color = colors.secondaryText,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FlashcardStatItem(label: String, count: Int) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
            style = typography.sectionTitle,
            color = colors.primaryText
        )
        Text(
            text = label,
            style = typography.caption,
            color = colors.secondaryText
        )
    }
}

// ----------------------------------------------------
// QUIZ TAB
// ----------------------------------------------------

@Composable
private fun PracticeQuizTab(
    summary: ChapterPracticeSummary?,
    isGenerating: Boolean,
    onOpenCreateQuiz: () -> Unit
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shapes.surface)
                .background(colors.surface)
                .border(1.dp, colors.border, shapes.surface)
                .padding(20.dp)
        ) {
            Column {
                Text(
                    text = "Adaptive Practice Quiz",
                    style = typography.sectionTitle,
                    color = colors.primaryText
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Quizzes test your recall across Multiple Choice, True/False, and conceptual questions. Incorrect questions are automatically saved to your Mistake Bank for revision.",
                    style = typography.body,
                    color = colors.secondaryText
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (summary?.quizAccuracy != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Average Quiz Accuracy",
                            style = typography.secondary,
                            color = colors.primaryText
                        )
                        Text(
                            text = "${summary.quizAccuracy}%",
                            style = typography.sectionTitle,
                            color = colors.accent
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    StudyOSProgressBar(
                        progress = summary.quizAccuracy,
                        height = 6.dp,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }

                StudyOSButton(
                    text = if (isGenerating) "Generating Quiz..." else "Generate AI Practice Quiz",
                    onClick = onOpenCreateQuiz,
                    enabled = !isGenerating,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// ----------------------------------------------------
// MISTAKES TAB
// ----------------------------------------------------

@Composable
private fun PracticeMistakesTab(
    mistakes: List<Mistake>,
    onResolve: (String) -> Unit,
    onConvertToFlashcard: (Mistake) -> Unit,
    onExplainWithAi: (Mistake) -> Unit,
    onOpenGlobalMistakes: () -> Unit
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    val unresolved = mistakes.filter { !it.isResolved }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${unresolved.size} Unresolved Mistakes",
                style = typography.secondary,
                color = colors.secondaryText
            )

            StudyOSOutlinedButton(
                text = "All Mistakes",
                onClick = onOpenGlobalMistakes
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (unresolved.isEmpty()) {
            StudyOSEmptyState(
                title = "No mistakes recorded!",
                description = "Mistakes made during quizzes automatically appear here with explanation tools and 1-tap conversion to flashcards.",
                actionButtonText = "View All Mistakes",
                onActionClick = onOpenGlobalMistakes
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(unresolved, key = { it.id }) { mistake ->
                    MistakeCard(
                        mistake = mistake,
                        onResolve = { onResolve(mistake.id) },
                        onConvertToFlashcard = { onConvertToFlashcard(mistake) },
                        onExplainWithAi = { onExplainWithAi(mistake) }
                    )
                }
            }
        }
    }
}

@Composable
fun MistakeCard(
    mistake: Mistake,
    onResolve: () -> Unit,
    onConvertToFlashcard: () -> Unit,
    onExplainWithAi: () -> Unit
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.card)
            .background(colors.surface)
            .border(1.dp, colors.border, shapes.card)
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (mistake.missedCount > 1) {
                    Box(
                        modifier = Modifier
                            .clip(shapes.button)
                            .background(colors.cardBackground)
                            .border(1.dp, colors.border, shapes.button)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Missed ${mistake.missedCount}x",
                            style = typography.caption,
                            color = colors.accent
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                StudyOSIconButton(
                    onClick = onResolve,
                    contentDescription = "Resolve mistake"
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = null,
                        tint = colors.secondaryText,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = mistake.question,
                style = typography.secondary,
                fontWeight = FontWeight.SemiBold,
                color = colors.primaryText
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Your Answer: ${mistake.studentAnswer}",
                style = typography.caption,
                color = colors.mutedText
            )

            Text(
                text = "Correct: ${mistake.correctAnswer}",
                style = typography.caption,
                fontWeight = FontWeight.Medium,
                color = colors.primaryText
            )

            if (!mistake.explanation.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = mistake.explanation,
                    style = typography.caption,
                    color = colors.secondaryText
                )
            }

            if (!mistake.photoUri.isNullOrBlank()) {
                val photoBitmap = androidx.compose.runtime.remember(mistake.photoUri) {
                    try {
                        val file = java.io.File(mistake.photoUri)
                        if (file.exists()) android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                        else null
                    } catch (e: Exception) {
                        null
                    }
                }

                if (photoBitmap != null) {
                    var showFullDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                    Spacer(modifier = Modifier.height(8.dp))
                    Image(
                        bitmap = photoBitmap.asImageBitmap(),
                        contentDescription = "Question Photo",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 180.dp)
                            .clip(shapes.button)
                            .clickable { showFullDialog = true },
                        contentScale = ContentScale.Crop
                    )

                    if (showFullDialog) {
                        com.studyos.app.core.ui.component.StudyOSDialog(
                            onDismissRequest = { showFullDialog = false },
                            title = "Question Photo",
                            content = {
                                Image(
                                    bitmap = photoBitmap.asImageBitmap(),
                                    contentDescription = "Full photo",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 350.dp),
                                    contentScale = ContentScale.Fit
                                )
                            },
                            confirmButtonText = "Close",
                            onConfirm = { showFullDialog = false },
                            dismissButtonText = null
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StudyOSOutlinedButton(
                    text = "Explain with AI",
                    onClick = onExplainWithAi,
                    modifier = Modifier.weight(1f)
                )

                StudyOSButton(
                    text = "Make Flashcard",
                    onClick = onConvertToFlashcard,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// ----------------------------------------------------
// BOTTOM SHEETS
// ----------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateFlashcardBottomSheet(
    onDismiss: () -> Unit,
    onSave: (question: String, answer: String, difficulty: FlashcardDifficulty) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography

    var question by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }
    var difficulty by remember { mutableStateOf(FlashcardDifficulty.MEDIUM) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Create Flashcard",
                style = typography.screenTitle,
                color = colors.primaryText
            )

            Spacer(modifier = Modifier.height(16.dp))

            StudyOSTextField(
                value = question,
                onValueChange = { question = it },
                label = "Front (Question / Prompt)",
                placeholder = "What is the mitochondria?",
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            StudyOSTextField(
                value = answer,
                onValueChange = { answer = it },
                label = "Back (Answer)",
                placeholder = "The powerhouse of the cell.",
                singleLine = false,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Difficulty",
                style = typography.secondary,
                color = colors.primaryText
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FlashcardDifficulty.values().forEach { diff ->
                    val isSelected = difficulty == diff
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(StudyOSTheme.shapes.button)
                            .background(if (isSelected) colors.cardBackground else colors.surface)
                            .border(1.dp, if (isSelected) colors.accent else colors.border, StudyOSTheme.shapes.button)
                            .clickable { difficulty = diff }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = diff.name,
                            style = typography.caption,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = colors.primaryText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            StudyOSButton(
                text = "Save Flashcard",
                onClick = { onSave(question, answer, difficulty) },
                enabled = question.isNotBlank() && answer.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BulkImportFlashcardsBottomSheet(
    onDismiss: () -> Unit,
    onImport: (List<ParsedCard>, FlashcardDifficulty) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val context = LocalContext.current

    var selectedMode by remember { mutableIntStateOf(0) } // 0 = File, 1 = Paste
    var pastedText by remember { mutableStateOf("") }
    var parsedCards by remember { mutableStateOf<List<ParsedCard>>(emptyList()) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var difficulty by remember { mutableStateOf(FlashcardDifficulty.MEDIUM) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "imported_file"
            selectedFileName = fileName
            val cards = FlashcardImportParser.parseFromUri(context, uri)
            parsedCards = cards
        }
    }

    LaunchedEffect(pastedText, selectedMode) {
        if (selectedMode == 1) {
            parsedCards = FlashcardImportParser.parseText(pastedText)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Bulk Import Flashcards",
                style = typography.screenTitle,
                color = colors.primaryText
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Import from Anki (.tsv, .txt), Quizlet, or CSV spreadsheet. Format: Question [TAB / COMMA] Answer",
                style = typography.caption,
                color = colors.secondaryText
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Mode Selector: Upload File vs Paste Text
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(StudyOSTheme.shapes.button)
                    .background(colors.cardBackground)
                    .border(1.dp, colors.border, StudyOSTheme.shapes.button)
                    .padding(3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(StudyOSTheme.shapes.button)
                        .background(if (selectedMode == 0) colors.accent else androidx.compose.ui.graphics.Color.Transparent)
                        .clickable { selectedMode = 0 }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "📁 Upload File",
                        style = typography.caption,
                        fontWeight = if (selectedMode == 0) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedMode == 0) colors.background else colors.secondaryText
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(StudyOSTheme.shapes.button)
                        .background(if (selectedMode == 1) colors.accent else androidx.compose.ui.graphics.Color.Transparent)
                        .clickable { selectedMode = 1 }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✏️ Paste Text",
                        style = typography.caption,
                        fontWeight = if (selectedMode == 1) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedMode == 1) colors.background else colors.secondaryText
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (selectedMode == 0) {
                // Upload File Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(StudyOSTheme.shapes.card)
                        .background(colors.cardBackground)
                        .border(1.dp, colors.border, StudyOSTheme.shapes.card)
                        .clickable {
                            filePickerLauncher.launch(arrayOf("text/*", "*/*"))
                        }
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.FileUpload,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (selectedFileName != null) selectedFileName!! else "Select CSV, TSV or Anki export file",
                            style = typography.secondary,
                            fontWeight = FontWeight.Medium,
                            color = colors.primaryText
                        )
                        Text(
                            text = "Tap to open device file picker",
                            style = typography.caption,
                            color = colors.secondaryText
                        )
                    }
                }
            } else {
                // Paste Text
                StudyOSTextField(
                    value = pastedText,
                    onValueChange = { pastedText = it },
                    label = "Paste Flashcard Text",
                    placeholder = "What is mitochondria?\tPowerhouse of the cell\nWhat is ATP?\tEnergy currency of cell",
                    singleLine = false,
                    maxLines = 6,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp, max = 150.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Status & Preview
            if (parsedCards.isNotEmpty()) {
                Text(
                    text = "Detected ${parsedCards.size} flashcards:",
                    style = typography.caption,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.accent
                )
                Spacer(modifier = Modifier.height(6.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(StudyOSTheme.shapes.card)
                        .background(colors.cardBackground)
                        .border(1.dp, colors.border, StudyOSTheme.shapes.card)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    parsedCards.take(3).forEachIndexed { idx, card ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "#${idx + 1}",
                                style = typography.caption,
                                color = colors.mutedText
                            )
                            Text(
                                text = card.question,
                                style = typography.caption,
                                fontWeight = FontWeight.Medium,
                                color = colors.primaryText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "→",
                                style = typography.caption,
                                color = colors.mutedText
                            )
                            Text(
                                text = card.answer,
                                style = typography.caption,
                                color = colors.secondaryText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    if (parsedCards.size > 3) {
                        Text(
                            text = "+ ${parsedCards.size - 3} more cards",
                            style = typography.caption,
                            color = colors.mutedText
                        )
                    }
                }
            } else if (selectedFileName != null || pastedText.isNotBlank()) {
                Text(
                    text = "No cards detected. Ensure cards are separated by tab, comma, or semicolon on each line.",
                    style = typography.caption,
                    color = colors.accent
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StudyOSOutlinedButton(
                    text = "Cancel",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                )
                StudyOSButton(
                    text = if (parsedCards.isNotEmpty()) "Import ${parsedCards.size} Cards" else "Import Cards",
                    enabled = parsedCards.isNotEmpty(),
                    onClick = {
                        onImport(parsedCards, difficulty)
                    },
                    modifier = Modifier.weight(1.5f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateQuizBottomSheet(
    isGenerating: Boolean,
    onDismiss: () -> Unit,
    onGenerate: (count: Int, difficulty: QuizDifficulty) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography

    var questionCount by remember { mutableIntStateOf(5) }
    var difficulty by remember { mutableStateOf(QuizDifficulty.MEDIUM) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Generate Practice Quiz",
                style = typography.screenTitle,
                color = colors.primaryText
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "AI will synthesize questions based on your study notes and key topics in this chapter.",
                style = typography.body,
                color = colors.secondaryText
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Number of Questions: $questionCount",
                style = typography.secondary,
                color = colors.primaryText
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(3, 5, 10).forEach { count ->
                    val isSelected = questionCount == count
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(StudyOSTheme.shapes.button)
                            .background(if (isSelected) colors.cardBackground else colors.surface)
                            .border(1.dp, if (isSelected) colors.accent else colors.border, StudyOSTheme.shapes.button)
                            .clickable { questionCount = count }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$count questions",
                            style = typography.caption,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = colors.primaryText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Difficulty",
                style = typography.secondary,
                color = colors.primaryText
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuizDifficulty.values().forEach { diff ->
                    val isSelected = difficulty == diff
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(StudyOSTheme.shapes.button)
                            .background(if (isSelected) colors.cardBackground else colors.surface)
                            .border(1.dp, if (isSelected) colors.accent else colors.border, StudyOSTheme.shapes.button)
                            .clickable { difficulty = diff }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = diff.name,
                            style = typography.caption,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = colors.primaryText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            StudyOSButton(
                text = if (isGenerating) "Synthesizing Quiz..." else "Generate Quiz",
                onClick = { onGenerate(questionCount, difficulty) },
                enabled = !isGenerating,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MistakeAiExplanationBottomSheet(
    mistake: Mistake,
    explanation: String?,
    isExplaining: Boolean,
    onDismiss: () -> Unit,
    onConvertToFlashcard: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography

    ModalBottomSheet(
        onDismissRequest = onDismiss,
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
                text = "Question: ${mistake.question}",
                style = typography.secondary,
                fontWeight = FontWeight.Medium,
                color = colors.primaryText
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Your answer: ${mistake.studentAnswer} (Incorrect)",
                style = typography.caption,
                color = colors.mutedText
            )
            Text(
                text = "Correct answer: ${mistake.correctAnswer}",
                style = typography.caption,
                fontWeight = FontWeight.SemiBold,
                color = colors.primaryText
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(StudyOSTheme.shapes.card)
                    .background(colors.cardBackground)
                    .border(1.dp, colors.border, StudyOSTheme.shapes.card)
                    .padding(14.dp)
            ) {
                if (isExplaining && explanation.isNullOrBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = colors.accent,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Analyzing concept and mistake pattern...",
                            style = typography.caption,
                            color = colors.secondaryText
                        )
                    }
                } else if (!explanation.isNullOrBlank()) {
                    StudyOSMarkdown(content = explanation)
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
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                )

                StudyOSButton(
                    text = "Convert to Flashcard",
                    onClick = onConvertToFlashcard,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
