package com.studyos.app.features.exams.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.RoundedCornerShape
import com.studyos.app.domain.model.QuestionType
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studyos.app.core.csv.CsvRowError
import com.studyos.app.core.csv.QuestionImportTarget
import com.studyos.app.core.csv.UniversalCsvProcessor
import com.studyos.app.core.database.StudyOSDatabase
import com.studyos.app.core.database.entity.ChapterEntity
import com.studyos.app.core.database.entity.PaperEntity
import com.studyos.app.core.database.entity.QuestionBankEntity
import com.studyos.app.core.database.entity.SubjectEntity
import com.studyos.app.core.paper.ExamPaperMarks
import com.studyos.app.core.paper.QuestionPaperGenerator
import com.studyos.app.core.ui.component.GlassCard
import com.studyos.app.core.ui.component.GlassIconButton
import com.studyos.app.core.ui.component.GlassTopBar
import com.studyos.app.core.ui.component.StudyOSButton
import com.studyos.app.core.ui.component.StudyOSEmptyState
import com.studyos.app.core.ui.component.StudyOSOutlinedButton
import com.studyos.app.core.ui.component.StudyOSTextField
import com.studyos.app.theme.StudyOSTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionBankScreen(
    database: StudyOSDatabase,
    onBack: () -> Unit,
    onOpenPaper: (paperId: String) -> Unit,
    onStartQuiz: ((quizId: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    var subjects by remember { mutableStateOf<List<SubjectEntity>>(emptyList()) }
    var questions by remember { mutableStateOf<List<QuestionBankEntity>>(emptyList()) }
    var papers by remember { mutableStateOf<List<PaperEntity>>(emptyList()) }
    var selectedSubjectId by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedDifficultyFilter by remember { mutableStateOf<String?>(null) }
    var selectedMarksFilter by remember { mutableStateOf<Int?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    var showImportDialog by remember { mutableStateOf(false) }
    var showCreatePaperSheet by remember { mutableStateOf(false) }

    val filteredQuestions = remember(questions, searchQuery, selectedDifficultyFilter, selectedMarksFilter) {
        questions.filter { q ->
            val matchesQuery = searchQuery.isBlank() ||
                q.questionText.contains(searchQuery, ignoreCase = true) ||
                (q.markingScheme?.contains(searchQuery, ignoreCase = true) == true)
            val matchesDiff = selectedDifficultyFilter == null || q.difficulty.equals(selectedDifficultyFilter, ignoreCase = true)
            val matchesMarks = selectedMarksFilter == null || q.marks == selectedMarksFilter
            matchesQuery && matchesDiff && matchesMarks
        }
    }

    fun refreshData() {
        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                val subList = database.subjectDao().getAllSubjectsOnce()
                subjects = subList
                if (selectedSubjectId == null && subList.isNotEmpty()) {
                    selectedSubjectId = subList.first().id
                }
                questions = database.questionBankDao().getAllQuestionsOnce()
                papers = database.paperDao().getAllPapersOnce()
            }
            isLoading = false
        }
    }

    fun launchTopicQuiz() {
        if (filteredQuestions.isEmpty()) return
        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                val selectedSub = subjects.find { it.id == selectedSubjectId }
                val quizTitle = if (selectedSub != null) "${selectedSub.name} Practice Quiz" else "Question Bank Quiz"
                val result = com.studyos.app.core.quiz.QuestionBankQuizEngine.buildQuiz(
                    questions = filteredQuestions,
                    quizTitle = quizTitle,
                    subjectId = selectedSubjectId ?: filteredQuestions.first().subjectId,
                    chapterId = filteredQuestions.first().chapterId,
                    topicName = null,
                    targetQuestionCount = minOf(10, filteredQuestions.size)
                )
                if (result != null) {
                    val (quiz, quizQuestions) = result
                    with(com.studyos.app.core.quiz.QuestionBankQuizEngine) {
                        database.quizDao().insertQuiz(quiz.toEntity())
                        database.quizDao().insertQuestions(quizQuestions.map { it.toEntity() })
                    }
                    for (q in filteredQuestions) {
                        database.questionBankDao().incrementUsage(q.id)
                    }
                    withContext(Dispatchers.Main) {
                        onStartQuiz?.invoke(quiz.id)
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshData()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.background,
        topBar = {
            GlassTopBar(
                title = "Question Bank & Papers",
                subtitle = "${questions.size} Questions • ${papers.size} Papers",
                navigationIcon = {
                    GlassIconButton(onClick = onBack, contentDescription = "Back") {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = null,
                            tint = colors.primaryText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                actions = {
                    GlassIconButton(
                        onClick = { showImportDialog = true },
                        contentDescription = "Import CSV Questions"
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FileUpload,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = colors.accent)
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.TopCenter
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 680.dp)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Action Buttons Row
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StudyOSButton(
                                text = "📝 Form Paper",
                                onClick = { showCreatePaperSheet = true },
                                modifier = Modifier.weight(1f)
                            )

                            if (onStartQuiz != null && filteredQuestions.isNotEmpty()) {
                                StudyOSButton(
                                    text = "🎯 Quiz (${minOf(10, filteredQuestions.size)} Qs)",
                                    onClick = { launchTopicQuiz() },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            StudyOSOutlinedButton(
                                text = "📥 Import CSV",
                                onClick = { showImportDialog = true },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Generated Question Papers Section
                    if (papers.isNotEmpty()) {
                        item {
                            Text(
                                text = "GENERATED QUESTION PAPERS",
                                style = typography.caption.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                                color = colors.secondaryText
                            )
                        }

                        items(papers) { paperItem ->
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { onOpenPaper(paperItem.id) },
                                padding = 14.dp
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = paperItem.title,
                                            style = typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = colors.primaryText,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${paperItem.totalMarks} Marks • ${paperItem.durationMinutes} Minutes Time",
                                            style = typography.caption,
                                            color = colors.secondaryText
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(shapes.button)
                                            .background(colors.accent.copy(alpha = 0.15f))
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "View Paper",
                                            style = typography.caption,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.accent
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Question Bank Questions Section
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "QUESTION BANK (${filteredQuestions.size} of ${questions.size})",
                                style = typography.caption.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                                color = colors.secondaryText
                            )
                            if (questions.isEmpty()) {
                                Text(
                                    text = "Tap 'Import CSV' to add questions",
                                    style = typography.caption,
                                    color = colors.accent
                                )
                            }
                        }
                    }

                    if (questions.isNotEmpty()) {
                        item {
                            StudyOSTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = "Search questions or marking schemes...",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Difficulty Chips
                                listOf(null to "All Diff", "EASY" to "Easy", "MEDIUM" to "Med", "HARD" to "Hard").forEach { (diff, label) ->
                                    val isSelected = selectedDifficultyFilter == diff
                                    Box(
                                        modifier = Modifier
                                            .clip(shapes.button)
                                            .background(if (isSelected) colors.accent.copy(alpha = 0.2f) else colors.surface)
                                            .border(1.dp, if (isSelected) colors.accent else colors.border, shapes.button)
                                            .clickable { selectedDifficultyFilter = diff }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = label,
                                            style = typography.caption,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) colors.accent else colors.primaryText
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                // Marks Chips
                                listOf(null to "All Marks", 1 to "1M", 2 to "2M", 3 to "3M", 5 to "5M").forEach { (marks, label) ->
                                    val isSelected = selectedMarksFilter == marks
                                    Box(
                                        modifier = Modifier
                                            .clip(shapes.button)
                                            .background(if (isSelected) colors.accent.copy(alpha = 0.2f) else colors.surface)
                                            .border(1.dp, if (isSelected) colors.accent else colors.border, shapes.button)
                                            .clickable { selectedMarksFilter = marks }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = label,
                                            style = typography.caption,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) colors.accent else colors.primaryText
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (questions.isEmpty()) {
                        item {
                            StudyOSEmptyState(
                                title = "No Questions in Bank",
                                description = "Import questions via CSV into chapters or subjects. You can choose to store them as Question Bank items, Flashcards, or both!",
                                actionButtonText = "Import Questions CSV",
                                onActionClick = { showImportDialog = true }
                            )
                        }
                    } else if (filteredQuestions.isEmpty()) {
                        item {
                            StudyOSEmptyState(
                                title = "No Matching Questions",
                                description = "No questions match your current search or filter criteria. Try clearing filters.",
                                actionButtonText = "Clear Filters",
                                onActionClick = {
                                    searchQuery = ""
                                    selectedDifficultyFilter = null
                                    selectedMarksFilter = null
                                }
                            )
                        }
                    } else {
                        items(filteredQuestions) { q ->
                            QuestionBankCardItem(q = q)
                        }
                    }
                }
            }
        }

        // CSV Import Dialog
        if (showImportDialog) {
            CsvQuestionImportDialog(
                database = database,
                onDismiss = { showImportDialog = false },
                onImportSuccess = {
                    showImportDialog = false
                    refreshData()
                }
            )
        }

        // Form Question Paper Bottom Sheet
        if (showCreatePaperSheet) {
            FormQuestionPaperBottomSheet(
                database = database,
                subjects = subjects,
                onDismiss = { showCreatePaperSheet = false },
                onPaperGenerated = { createdPaperId ->
                    showCreatePaperSheet = false
                    refreshData()
                    onOpenPaper(createdPaperId)
                }
            )
        }
    }
}

@Composable
private fun QuestionBankCardItem(
    q: QuestionBankEntity,
    modifier: Modifier = Modifier
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    val parsed = remember(q) {
        com.studyos.app.core.quiz.QuestionBankQuizEngine.parseQuestionContent(
            q.questionText,
            q.markingScheme,
            q.questionType
        )
    }

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        padding = 16.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Badges Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Type Badge
                    val (typeLabel, typeBg, typeColor) = when (parsed.type) {
                        QuestionType.MCQ -> Triple("MCQ", Color(0xFF6366F1).copy(alpha = 0.18f), Color(0xFF818CF8))
                        QuestionType.FIB -> Triple("FILL BLANK", Color(0xFF10B981).copy(alpha = 0.18f), Color(0xFF34D399))
                        QuestionType.TRUE_FALSE -> Triple("TRUE / FALSE", Color(0xFF38BDF8).copy(alpha = 0.18f), Color(0xFF38BDF8))
                        QuestionType.SHORT_ANSWER -> Triple("SHORT ANSWER", colors.cardBackground, colors.secondaryText)
                        else -> Triple(q.questionType, colors.cardBackground, colors.secondaryText)
                    }

                    Box(
                        modifier = Modifier
                            .clip(shapes.button)
                            .background(typeBg)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = typeLabel,
                            style = typography.caption.copy(fontSize = 10.sp),
                            fontWeight = FontWeight.Bold,
                            color = typeColor
                        )
                    }

                    // Difficulty Badge with Colored Indicator Dot
                    val diffColor = when (q.difficulty.uppercase()) {
                        "EASY" -> Color(0xFF10B981)
                        "HARD" -> Color(0xFFF43F5E)
                        else -> Color(0xFFF59E0B)
                    }

                    Row(
                        modifier = Modifier
                            .clip(shapes.button)
                            .background(colors.surface)
                            .border(0.5.dp, colors.border, shapes.button)
                            .padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(diffColor)
                        )
                        Text(
                            text = q.difficulty.lowercase().replaceFirstChar { it.uppercase() },
                            style = typography.caption.copy(fontSize = 10.sp),
                            fontWeight = FontWeight.Medium,
                            color = colors.secondaryText
                        )
                    }

                    if (q.usageCount > 0) {
                        Box(
                            modifier = Modifier
                                .clip(shapes.button)
                                .background(colors.surface)
                                .border(0.5.dp, colors.border, shapes.button)
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "🎯 ${q.usageCount}x",
                                style = typography.caption.copy(fontSize = 9.sp),
                                color = colors.secondaryText
                            )
                        }
                    }
                }

                // Marks Badge
                Box(
                    modifier = Modifier
                        .clip(shapes.button)
                        .background(colors.accent.copy(alpha = 0.12f))
                        .border(0.5.dp, colors.accent.copy(alpha = 0.35f), shapes.button)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "★ ${q.marks} Mark${if (q.marks > 1) "s" else ""}",
                        style = typography.caption.copy(fontSize = 11.sp),
                        fontWeight = FontWeight.Bold,
                        color = colors.accent
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Question Stem
            Text(
                text = parsed.questionStem,
                style = typography.body.copy(fontSize = 14.sp, lineHeight = 21.sp),
                fontWeight = FontWeight.SemiBold,
                color = colors.primaryText
            )

            // Fill-in-the-blank (FIB) Answer Pill
            if (parsed.type == QuestionType.FIB) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shapes.button)
                        .background(Color(0xFF10B981).copy(alpha = 0.10f))
                        .border(1.dp, Color(0xFF10B981).copy(alpha = 0.35f), shapes.button)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🎯 Target Blank Answer:",
                        style = typography.caption,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "[ ${parsed.correctAnswer} ]",
                        style = typography.body.copy(fontSize = 13.sp),
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF34D399)
                    )
                }
            }

            // True / False Dual Pill Section
            if (parsed.type == QuestionType.TRUE_FALSE) {
                Spacer(modifier = Modifier.height(10.dp))
                val isTrueCorrect = parsed.correctAnswer.equals("True", ignoreCase = true)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // True Pill
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(shapes.button)
                            .background(if (isTrueCorrect) Color(0xFF10B981).copy(alpha = 0.15f) else colors.surface)
                            .border(
                                if (isTrueCorrect) 1.5.dp else 0.5.dp,
                                if (isTrueCorrect) Color(0xFF10B981) else colors.border,
                                shapes.button
                            )
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isTrueCorrect) {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = "TRUE",
                                style = typography.caption,
                                fontWeight = if (isTrueCorrect) FontWeight.Bold else FontWeight.Medium,
                                color = if (isTrueCorrect) Color(0xFF34D399) else colors.secondaryText
                            )
                        }
                    }

                    // False Pill
                    val isFalseCorrect = !isTrueCorrect
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(shapes.button)
                            .background(if (isFalseCorrect) Color(0xFFF43F5E).copy(alpha = 0.15f) else colors.surface)
                            .border(
                                if (isFalseCorrect) 1.5.dp else 0.5.dp,
                                if (isFalseCorrect) Color(0xFFF43F5E) else colors.border,
                                shapes.button
                            )
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isFalseCorrect) {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = null,
                                    tint = Color(0xFFF43F5E),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = "FALSE",
                                style = typography.caption,
                                fontWeight = if (isFalseCorrect) FontWeight.Bold else FontWeight.Medium,
                                color = if (isFalseCorrect) Color(0xFFFB7185) else colors.secondaryText
                            )
                        }
                    }
                }
            }

            // MCQ Options Display
            if (parsed.options.isNotEmpty() && parsed.type == QuestionType.MCQ) {
                Spacer(modifier = Modifier.height(10.dp))
                val optLetters = listOf("A", "B", "C", "D", "E", "F")
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    parsed.options.forEachIndexed { idx, opt ->
                        val letter = optLetters.getOrElse(idx) { "${idx + 1}" }
                        val cleanOpt = com.studyos.app.core.quiz.QuestionBankQuizEngine.cleanOptionText(opt)
                        val isCorrectOpt = opt.equals(parsed.correctAnswer, ignoreCase = true) ||
                            cleanOpt.equals(com.studyos.app.core.quiz.QuestionBankQuizEngine.cleanOptionText(parsed.correctAnswer), ignoreCase = true)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(shapes.button)
                                .background(
                                    if (isCorrectOpt) Color(0xFF10B981).copy(alpha = 0.10f)
                                    else colors.surface
                                )
                                .border(
                                    if (isCorrectOpt) 1.2.dp else 0.5.dp,
                                    if (isCorrectOpt) Color(0xFF10B981) else colors.border,
                                    shapes.button
                                )
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Letter squircle
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (isCorrectOpt) Color(0xFF10B981)
                                        else colors.cardBackground
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = letter,
                                    style = typography.caption.copy(fontSize = 11.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCorrectOpt) Color(0xFF101216) else colors.secondaryText
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Text(
                                text = cleanOpt,
                                style = typography.caption.copy(fontSize = 13.sp),
                                fontWeight = if (isCorrectOpt) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isCorrectOpt) colors.primaryText else colors.secondaryText,
                                modifier = Modifier.weight(1f)
                            )

                            if (isCorrectOpt) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(shapes.button)
                                        .background(Color(0xFF10B981).copy(alpha = 0.20f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Outlined.Check,
                                            contentDescription = "Correct Choice",
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "Correct",
                                            style = typography.caption.copy(fontSize = 10.sp),
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF34D399)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Marking Scheme / Explanation Drawer
            val explanationText = parsed.explanation ?: q.markingScheme
            if (!explanationText.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shapes.button)
                        .background(colors.cardBackground.copy(alpha = 0.6f))
                        .border(0.5.dp, colors.border, shapes.button)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = "💡 Solution Key & Rationale",
                            style = typography.caption.copy(fontSize = 10.sp),
                            fontWeight = FontWeight.Bold,
                            color = colors.accent
                        )
                        Text(
                            text = explanationText,
                            style = typography.caption.copy(fontSize = 11.sp, lineHeight = 16.sp),
                            color = colors.primaryText.copy(alpha = 0.85f),
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

enum class QuestionImportMode {
    AUTO_DETECT,
    OBJECTIVE,
    STANDARD
}

@Composable
fun FixQuestionRowDialog(
    errorItem: CsvRowError,
    onSave: (Map<String, String>) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    val initialData = errorItem.rowData ?: emptyMap()
    var questionText by remember { mutableStateOf(initialData["question"]?.ifBlank { errorItem.rowContent } ?: errorItem.rowContent) }
    var selectedType by remember {
        mutableStateOf(
            initialData["questiontype"]?.takeIf { it.isNotBlank() } ?: "MCQ"
        )
    }
    var optA by remember { mutableStateOf(initialData["optiona"] ?: "") }
    var optB by remember { mutableStateOf(initialData["optionb"] ?: "") }
    var optC by remember { mutableStateOf(initialData["optionc"] ?: "") }
    var optD by remember { mutableStateOf(initialData["optiond"] ?: "") }
    var answerText by remember { mutableStateOf(initialData["correctanswer"] ?: "") }
    var explanation by remember { mutableStateOf(initialData["explanation"] ?: "") }
    var marks by remember { mutableStateOf(initialData["marks"] ?: "1") }
    var localError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Fix Question (Line ${errorItem.lineNumber})",
                    style = typography.sectionTitle,
                    color = colors.primaryText
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = errorItem.errorMessage,
                    style = typography.caption.copy(fontSize = 11.sp),
                    color = Color(0xFFEF5350)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Question Type Selector
                Text(
                    text = "Question Type:",
                    style = typography.caption,
                    fontWeight = FontWeight.Bold,
                    color = colors.primaryText
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("MCQ", "FIB", "TRUE_FALSE", "SHORT_ANSWER").forEach { type ->
                        val isSelected = selectedType.equals(type, ignoreCase = true)
                        val label = when (type) {
                            "MCQ" -> "MCQ"
                            "FIB" -> "Fill Blank"
                            "TRUE_FALSE" -> "T / F"
                            else -> "Short"
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(shapes.button)
                                .background(if (isSelected) colors.accent else colors.cardBackground)
                                .border(1.dp, if (isSelected) colors.accent else colors.border, shapes.button)
                                .clickable {
                                    selectedType = type
                                    localError = null
                                    if (type == "TRUE_FALSE" && answerText.lowercase() !in listOf("true", "false")) {
                                        answerText = "True"
                                    }
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = typography.caption.copy(fontSize = 10.sp),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color(0xFF101216) else colors.primaryText
                            )
                        }
                    }
                }

                // Question Text
                StudyOSTextField(
                    value = questionText,
                    onValueChange = { questionText = it; localError = null },
                    label = "Question Prompt",
                    placeholder = "Enter question text...",
                    singleLine = false,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )

                // Type Specific Fields
                when (selectedType.uppercase()) {
                    "MCQ" -> {
                        Text(
                            text = "Options (Option A & B required):",
                            style = typography.caption,
                            fontWeight = FontWeight.Bold,
                            color = colors.primaryText
                        )
                        StudyOSTextField(
                            value = optA,
                            onValueChange = { optA = it; localError = null },
                            label = "Option A",
                            placeholder = "First option...",
                            modifier = Modifier.fillMaxWidth()
                        )
                        StudyOSTextField(
                            value = optB,
                            onValueChange = { optB = it; localError = null },
                            label = "Option B",
                            placeholder = "Second option...",
                            modifier = Modifier.fillMaxWidth()
                        )
                        StudyOSTextField(
                            value = optC,
                            onValueChange = { optC = it },
                            label = "Option C (Optional)",
                            placeholder = "Third option...",
                            modifier = Modifier.fillMaxWidth()
                        )
                        StudyOSTextField(
                            value = optD,
                            onValueChange = { optD = it },
                            label = "Option D (Optional)",
                            placeholder = "Fourth option...",
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = "Select Correct Option:",
                            style = typography.caption,
                            fontWeight = FontWeight.Bold,
                            color = colors.primaryText
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("A", "B", "C", "D").forEach { optKey ->
                                val isChosen = answerText.equals(optKey, ignoreCase = true) ||
                                        (optKey == "A" && optA.isNotBlank() && answerText.equals(optA, ignoreCase = true)) ||
                                        (optKey == "B" && optB.isNotBlank() && answerText.equals(optB, ignoreCase = true)) ||
                                        (optKey == "C" && optC.isNotBlank() && answerText.equals(optC, ignoreCase = true)) ||
                                        (optKey == "D" && optD.isNotBlank() && answerText.equals(optD, ignoreCase = true))

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(shapes.button)
                                        .background(if (isChosen) Color(0xFF10B981) else colors.cardBackground)
                                        .border(1.dp, if (isChosen) Color(0xFF10B981) else colors.border, shapes.button)
                                        .clickable {
                                            answerText = optKey
                                            localError = null
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Option $optKey",
                                        style = typography.caption,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isChosen) Color.White else colors.primaryText
                                    )
                                }
                            }
                        }
                    }

                    "TRUE_FALSE" -> {
                        Text(
                            text = "Select Correct Value:",
                            style = typography.caption,
                            fontWeight = FontWeight.Bold,
                            color = colors.primaryText
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("True", "False").forEach { boolVal ->
                                val isChosen = answerText.equals(boolVal, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(shapes.button)
                                        .background(if (isChosen) Color(0xFF10B981) else colors.cardBackground)
                                        .border(1.dp, if (isChosen) Color(0xFF10B981) else colors.border, shapes.button)
                                        .clickable {
                                            answerText = boolVal
                                            localError = null
                                        }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = boolVal,
                                        style = typography.caption,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isChosen) Color.White else colors.primaryText
                                    )
                                }
                            }
                        }
                    }

                    else -> {
                        StudyOSTextField(
                            value = answerText,
                            onValueChange = { answerText = it; localError = null },
                            label = "Correct Answer / Key",
                            placeholder = "Enter correct answer...",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Explanation
                StudyOSTextField(
                    value = explanation,
                    onValueChange = { explanation = it },
                    label = "Explanation (Optional)",
                    placeholder = "Why this is the answer...",
                    modifier = Modifier.fillMaxWidth()
                )

                if (localError != null) {
                    Text(
                        text = localError!!,
                        style = typography.caption,
                        color = Color(0xFFEF5350)
                    )
                }
            }
        },
        confirmButton = {
            StudyOSButton(
                text = "Save Question",
                onClick = {
                    if (questionText.isBlank()) {
                        localError = "Question prompt cannot be blank"
                        return@StudyOSButton
                    }
                    if (selectedType.equals("MCQ", ignoreCase = true)) {
                        if (optA.isBlank() || optB.isBlank()) {
                            localError = "Both Option A and Option B are required for MCQ"
                            return@StudyOSButton
                        }
                        if (answerText.isBlank()) {
                            localError = "Please select the correct option (A, B, C, or D)"
                            return@StudyOSButton
                        }
                    } else if (answerText.isBlank()) {
                        localError = "Answer cannot be blank"
                        return@StudyOSButton
                    }

                    val fixedMap = mutableMapOf<String, String>()
                    fixedMap.putAll(initialData)
                    fixedMap["subjectname"] = initialData["subjectname"]?.ifBlank { "General" } ?: "General"
                    fixedMap["chaptername"] = initialData["chaptername"]?.ifBlank { "General Practice" } ?: "General Practice"
                    fixedMap["topicname"] = initialData["topicname"] ?: ""
                    fixedMap["questiontype"] = selectedType.uppercase()
                    fixedMap["question"] = questionText.trim()
                    fixedMap["optiona"] = optA.trim()
                    fixedMap["optionb"] = optB.trim()
                    fixedMap["optionc"] = optC.trim()
                    fixedMap["optiond"] = optD.trim()
                    fixedMap["correctanswer"] = answerText.trim()
                    fixedMap["explanation"] = explanation.trim()
                    fixedMap["marks"] = marks.trim().ifBlank { "1" }
                    fixedMap["difficulty"] = initialData["difficulty"]?.ifBlank { "MEDIUM" } ?: "MEDIUM"

                    onSave(fixedMap)
                }
            )
        },
        dismissButton = {
            StudyOSOutlinedButton(
                text = "Cancel",
                onClick = onDismiss
            )
        },
        containerColor = colors.surface
    )
}

@Composable
fun CsvQuestionImportDialog(
    database: StudyOSDatabase,
    onDismiss: () -> Unit,
    onImportSuccess: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    val processor = remember { UniversalCsvProcessor(database) }
    var importMode by remember { mutableStateOf(QuestionImportMode.AUTO_DETECT) }
    var csvText by remember { mutableStateOf("") }
    var selectedTarget by remember { mutableStateOf(QuestionImportTarget.BOTH) }
    var isImporting by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    var fixedRows by remember(csvText, importMode) { mutableStateOf<Map<Int, Map<String, String>>>(emptyMap()) }
    var dismissedErrorLineNumbers by remember(csvText, importMode) { mutableStateOf<Set<Int>>(emptySet()) }
    var editingErrorItem by remember { mutableStateOf<CsvRowError?>(null) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val content = stream.bufferedReader(Charsets.UTF_8).readText()
                    csvText = content
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to read CSV: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Dynamic Live Preview
    val previewInfo = remember(csvText, importMode) {
        if (csvText.isBlank()) null
        else {
            val res = when (importMode) {
                QuestionImportMode.AUTO_DETECT -> {
                    val obj = processor.parseAndValidateObjectiveQuestions(csvText)
                    if (obj.validRowsCount > 0 || obj.errors.any { it.canFixInUi }) obj else processor.parseAndValidateQuestions(csvText)
                }
                QuestionImportMode.OBJECTIVE -> processor.parseAndValidateObjectiveQuestions(csvText)
                QuestionImportMode.STANDARD -> processor.parseAndValidateQuestions(csvText)
            }
            res
        }
    }

    val activeErrors = remember(previewInfo, fixedRows, dismissedErrorLineNumbers) {
        previewInfo?.errors?.filter { it.lineNumber !in dismissedErrorLineNumbers && it.lineNumber !in fixedRows.keys } ?: emptyList()
    }

    val combinedValidData = remember(previewInfo, fixedRows) {
        val base = previewInfo?.parsedData ?: emptyList()
        base + fixedRows.values.toList()
    }

    if (editingErrorItem != null) {
        FixQuestionRowDialog(
            errorItem = editingErrorItem!!,
            onSave = { fixedMap ->
                val line = editingErrorItem!!.lineNumber
                fixedRows = fixedRows + (line to fixedMap)
                editingErrorItem = null
                Toast.makeText(context, "Row $line fixed and added to import!", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { editingErrorItem = null }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Import Questions via CSV", style = typography.sectionTitle, color = colors.primaryText)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Format Mode Selector (3 options: Auto-Detect, Objective, Standard)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shapes.button)
                        .background(colors.cardBackground)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(shapes.button)
                            .background(if (importMode == QuestionImportMode.AUTO_DETECT) colors.accent else Color.Transparent)
                            .clickable { importMode = QuestionImportMode.AUTO_DETECT; errorMsg = null }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "⚡ Auto",
                            style = typography.caption,
                            fontWeight = FontWeight.Bold,
                            color = if (importMode == QuestionImportMode.AUTO_DETECT) Color(0xFF101216) else colors.secondaryText
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1.2f)
                            .clip(shapes.button)
                            .background(if (importMode == QuestionImportMode.OBJECTIVE) colors.accent else Color.Transparent)
                            .clickable { importMode = QuestionImportMode.OBJECTIVE; errorMsg = null }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Objective",
                            style = typography.caption,
                            fontWeight = FontWeight.Bold,
                            color = if (importMode == QuestionImportMode.OBJECTIVE) Color(0xFF101216) else colors.secondaryText
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1.2f)
                            .clip(shapes.button)
                            .background(if (importMode == QuestionImportMode.STANDARD) colors.accent else Color.Transparent)
                            .clickable { importMode = QuestionImportMode.STANDARD; errorMsg = null }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Standard",
                            style = typography.caption,
                            fontWeight = FontWeight.Bold,
                            color = if (importMode == QuestionImportMode.STANDARD) Color(0xFF101216) else colors.secondaryText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = when (importMode) {
                        QuestionImportMode.AUTO_DETECT -> "Smart detection: Automatically reads MCQs, Fill in the Blanks, True/False, or Standard questions."
                        QuestionImportMode.OBJECTIVE -> "Objective format: Questions with option choices (A-D), sentence blanks, or True/False."
                        QuestionImportMode.STANDARD -> "Standard format: Conceptual theory, short answers, and long numerical questions."
                    },
                    style = typography.caption,
                    color = colors.secondaryText
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Copy & Share Template Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StudyOSOutlinedButton(
                        text = "📋 Copy Template",
                        onClick = {
                            val template = if (importMode == QuestionImportMode.OBJECTIVE) {
                                processor.getObjectiveQuestionsCsvTemplate()
                            } else {
                                processor.getQuestionBankCsvTemplate()
                            }
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Question Bank Template", template))
                            Toast.makeText(context, "Template copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f)
                    )

                    StudyOSOutlinedButton(
                        text = "📤 Share Template",
                        onClick = {
                            try {
                                val templateFile = if (importMode == QuestionImportMode.OBJECTIVE) {
                                    processor.exportObjectiveQuestionsTemplateFile(context)
                                } else {
                                    processor.exportQuestionBankTemplateFile(context)
                                }
                                val shareUri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    templateFile
                                )
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/csv"
                                    putExtra(Intent.EXTRA_STREAM, shareUri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share Question Template"))
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error sharing template: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Destination Option
                Text(
                    text = "Import Destination:",
                    style = typography.caption,
                    fontWeight = FontWeight.Bold,
                    color = colors.primaryText
                )

                Spacer(modifier = Modifier.height(4.dp))

                QuestionImportTarget.values().forEach { target ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shapes.button)
                            .clickable { selectedTarget = target }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedTarget == target,
                            onClick = { selectedTarget = target },
                            colors = RadioButtonDefaults.colors(selectedColor = colors.accent)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = target.displayName,
                            style = typography.caption,
                            color = colors.primaryText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // File Picker / Paste
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CSV Content:",
                        style = typography.caption,
                        fontWeight = FontWeight.Bold,
                        color = colors.primaryText
                    )
                    Text(
                        text = "Pick .csv File",
                        style = typography.caption,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.accent,
                        modifier = Modifier.clickable {
                            filePicker.launch(arrayOf("text/*", "application/*", "*/*"))
                        }
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                StudyOSTextField(
                    value = csvText,
                    onValueChange = { csvText = it; errorMsg = null },
                    placeholder = "Paste CSV content here or pick a .csv file...",
                    modifier = Modifier.fillMaxWidth()
                )

                // Status Bar & Issue Resolver
                val totalValidCount = combinedValidData.size
                if (totalValidCount > 0 || activeErrors.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "✓ $totalValidCount valid question(s)" +
                                    (if (activeErrors.isNotEmpty()) " • ⚠️ ${activeErrors.size} issue(s)" else ""),
                            style = typography.caption,
                            fontWeight = FontWeight.SemiBold,
                            color = if (totalValidCount > 0) Color(0xFF10B981) else Color(0xFFEF5350)
                        )
                        if (activeErrors.size > 1) {
                            Text(
                                text = "⚡ Convert All to Short Answer",
                                style = typography.caption.copy(fontSize = 11.sp),
                                fontWeight = FontWeight.Bold,
                                color = colors.accent,
                                modifier = Modifier
                                    .clip(shapes.button)
                                    .clickable {
                                        val newFixes = fixedRows.toMutableMap()
                                        for (err in activeErrors) {
                                            val data = err.rowData?.toMutableMap() ?: mutableMapOf(
                                                "question" to err.rowContent,
                                                "correctanswer" to "Answer",
                                                "questiontype" to "SHORT_ANSWER",
                                                "subjectname" to "General",
                                                "chaptername" to "General Practice"
                                            )
                                            data["questiontype"] = "SHORT_ANSWER"
                                            if (data["correctanswer"].isNullOrBlank()) {
                                                data["correctanswer"] = "Answer"
                                            }
                                            newFixes[err.lineNumber] = data
                                        }
                                        fixedRows = newFixes
                                        Toast.makeText(context, "Rescued all issues as Short Answer questions!", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Issues list (cards with 1-tap rescue buttons)
                if (activeErrors.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "ISSUES FOUND: FIX DIRECTLY OR RESCUE IN-APP",
                        style = typography.caption.copy(fontSize = 10.sp),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEF5350)
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    activeErrors.take(15).forEach { err ->
                        val promptPreview = err.rowData?.get("question")?.ifBlank { null }
                            ?: err.rowContent.take(70).ifBlank { "Empty row" }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(shapes.card)
                                .background(colors.cardBackground)
                                .border(1.dp, Color(0xFFEF5350).copy(alpha = 0.4f), shapes.card)
                                .padding(10.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Row ${err.lineNumber}: ${err.errorMessage}",
                                        style = typography.caption.copy(fontSize = 11.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFEF5350),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "✕ Skip",
                                        style = typography.caption.copy(fontSize = 10.sp),
                                        color = colors.secondaryText,
                                        modifier = Modifier
                                            .clip(shapes.button)
                                            .clickable {
                                                dismissedErrorLineNumbers = dismissedErrorLineNumbers + err.lineNumber
                                            }
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "\"$promptPreview\"",
                                    style = typography.caption.copy(fontSize = 11.sp),
                                    color = colors.primaryText,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // 1-tap Convert to Short Answer
                                    Box(
                                        modifier = Modifier
                                            .weight(1.2f)
                                            .clip(shapes.button)
                                            .background(colors.accent.copy(alpha = 0.15f))
                                            .border(1.dp, colors.accent.copy(alpha = 0.5f), shapes.button)
                                        .clickable {
                                            val data = err.rowData?.toMutableMap() ?: mutableMapOf(
                                                "question" to promptPreview,
                                                "correctanswer" to "Answer",
                                                "questiontype" to "SHORT_ANSWER",
                                                "subjectname" to "General",
                                                "chaptername" to "General Practice"
                                            )
                                            data["questiontype"] = "SHORT_ANSWER"
                                            if (data["correctanswer"].isNullOrBlank()) {
                                                data["correctanswer"] = "Answer"
                                            }
                                            fixedRows = fixedRows + (err.lineNumber to data)
                                            Toast.makeText(context, "Rescued Row ${err.lineNumber} as Short Answer!", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "⚡ Convert to Short",
                                            style = typography.caption.copy(fontSize = 10.sp),
                                            fontWeight = FontWeight.Bold,
                                            color = colors.primaryText
                                        )
                                    }

                                    // Fix & Add Options
                                    Box(
                                        modifier = Modifier
                                            .weight(1.2f)
                                            .clip(shapes.button)
                                            .background(colors.accent)
                                            .clickable {
                                                editingErrorItem = err
                                            }
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "✏️ Fix Options",
                                            style = typography.caption.copy(fontSize = 10.sp),
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF101216)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (activeErrors.size > 15) {
                        Text(
                            text = "+ ${activeErrors.size - 15} more issue(s) will be skipped or can be fixed above",
                            style = typography.caption.copy(fontSize = 10.sp),
                            color = colors.secondaryText,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                // Live Preview of Valid / Rescued Question
                val previewRow = combinedValidData.firstOrNull()
                if (previewRow != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "PREVIEW: HOW QUESTION WILL LOOK IN STUDYOS",
                        style = typography.caption.copy(fontSize = 10.sp),
                        fontWeight = FontWeight.Bold,
                        color = colors.secondaryText
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    val previewEntity = remember(previewRow) {
                        val qText = previewRow["question"] ?: previewRow["questiontext"] ?: ""
                        val qType = previewRow["questiontype"] ?: "MCQ"
                        val ans = previewRow["correctanswer"] ?: previewRow["answer"] ?: ""
                        val exp = previewRow["explanation"] ?: ""
                        val fullScheme = if (exp.isNotBlank()) "$ans • Explanation: $exp" else ans
                        val formattedQ = if (qType == "MCQ" && !qText.contains("\n(A)")) {
                            val opts = listOfNotNull(
                                previewRow["optiona"]?.let { "A" to it },
                                previewRow["optionb"]?.let { "B" to it },
                                previewRow["optionc"]?.let { "C" to it },
                                previewRow["optiond"]?.let { "D" to it }
                            ).filter { it.second.isNotBlank() }
                            val sb = StringBuilder(com.studyos.app.core.quiz.QuestionBankQuizEngine.cleanStem(qText))
                            for ((letter, text) in opts) {
                                sb.append("\n($letter) ${com.studyos.app.core.quiz.QuestionBankQuizEngine.cleanOptionText(text)}")
                            }
                            sb.toString()
                        } else qText

                        QuestionBankEntity(
                            id = "preview",
                            subjectId = "sub",
                            chapterId = "chap",
                            questionText = formattedQ,
                            markingScheme = fullScheme,
                            marks = previewRow["marks"]?.toIntOrNull() ?: 1,
                            questionType = qType,
                            difficulty = previewRow["difficulty"]?.uppercase() ?: "MEDIUM"
                        )
                    }

                    QuestionBankCardItem(q = previewEntity)
                }

                if (errorMsg != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMsg!!,
                        style = typography.caption,
                        color = Color(0xFFEF5350)
                    )
                }
            }
        },
        confirmButton = {
            StudyOSButton(
                text = if (isImporting) "Importing..." else if (combinedValidData.isNotEmpty()) "Import (${combinedValidData.size})" else "Import",
                onClick = {
                    if (csvText.isBlank()) {
                        errorMsg = "Please enter or select CSV content"
                        return@StudyOSButton
                    }
                    if (combinedValidData.isEmpty()) {
                        errorMsg = if (activeErrors.isNotEmpty()) {
                            "Line ${activeErrors.first().lineNumber}: " + activeErrors.first().errorMessage + " — Please fix or convert above."
                        } else {
                            "No valid questions found in CSV content"
                        }
                        return@StudyOSButton
                    }

                    isImporting = true
                    coroutineScope.launch {
                        val isObjectiveData = combinedValidData.any {
                            it["questiontype"] in listOf("MCQ", "FIB", "TRUE_FALSE") || !it["optiona"].isNullOrBlank()
                        }
                        val result = if (isObjectiveData) {
                            processor.importObjectiveQuestions(combinedValidData, selectedTarget)
                        } else {
                            processor.importQuestions(combinedValidData, selectedTarget)
                        }
                        isImporting = false
                        if (result.importedCount > 0) {
                            val skipMsg = if (activeErrors.isNotEmpty()) " (${activeErrors.size} issue(s) skipped)" else ""
                            Toast.makeText(context, "Successfully imported ${result.importedCount} questions!$skipMsg", Toast.LENGTH_LONG).show()
                            onImportSuccess()
                        } else {
                            errorMsg = result.errors.firstOrNull()?.errorMessage ?: "Failed to import questions"
                        }
                    }
                }
            )
        },
        dismissButton = {
            StudyOSOutlinedButton(
                text = "Cancel",
                onClick = onDismiss
            )
        },
        containerColor = colors.surface
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormQuestionPaperBottomSheet(
    database: StudyOSDatabase,
    subjects: List<SubjectEntity>,
    onDismiss: () -> Unit,
    onPaperGenerated: (paperId: String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    var selectedSubject by remember { mutableStateOf<SubjectEntity?>(subjects.firstOrNull()) }
    var chapters by remember { mutableStateOf<List<ChapterEntity>>(emptyList()) }
    var selectedChapter by remember { mutableStateOf<ChapterEntity?>(null) } // null = Whole Subject
    var selectedMarks by remember { mutableStateOf(ExamPaperMarks.MARKS_40) }
    var customTitle by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }

    LaunchedEffect(selectedSubject?.id) {
        val sId = selectedSubject?.id ?: return@LaunchedEffect
        withContext(Dispatchers.IO) {
            chapters = database.chapterDao().getChaptersForSubjectOnce(sId)
        }
        selectedChapter = null
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
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Form Question Paper",
                style = typography.screenTitle,
                color = colors.primaryText
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Assemble a balanced model exam paper from your Question Bank with timed sections.",
                style = typography.caption,
                color = colors.secondaryText
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Choose Subject
            Text("Select Subject:", style = typography.caption, fontWeight = FontWeight.Bold, color = colors.primaryText)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                subjects.forEach { sub ->
                    val isSelected = selectedSubject?.id == sub.id
                    Box(
                        modifier = Modifier
                            .clip(shapes.button)
                            .background(if (isSelected) colors.accent.copy(alpha = 0.15f) else colors.cardBackground)
                            .border(1.dp, if (isSelected) colors.accent else colors.border, shapes.button)
                            .clickable { selectedSubject = sub }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = sub.name,
                            style = typography.caption,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) colors.accent else colors.secondaryText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Scope: Specific Chapter vs Whole Subject
            Text("Syllabus Scope:", style = typography.caption, fontWeight = FontWeight.Bold, color = colors.primaryText)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val isWholeSubject = selectedChapter == null
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(shapes.button)
                        .background(if (isWholeSubject) colors.accent.copy(alpha = 0.15f) else colors.cardBackground)
                        .border(1.dp, if (isWholeSubject) colors.accent else colors.border, shapes.button)
                        .clickable { selectedChapter = null }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Entire Subject",
                        style = typography.caption,
                        fontWeight = if (isWholeSubject) FontWeight.Bold else FontWeight.Normal,
                        color = if (isWholeSubject) colors.accent else colors.secondaryText
                    )
                }

                if (chapters.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(shapes.button)
                            .background(if (!isWholeSubject) colors.accent.copy(alpha = 0.15f) else colors.cardBackground)
                            .border(1.dp, if (!isWholeSubject) colors.accent else colors.border, shapes.button)
                            .clickable { selectedChapter = chapters.first() }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Specific Chapter",
                            style = typography.caption,
                            fontWeight = if (!isWholeSubject) FontWeight.Bold else FontWeight.Normal,
                            color = if (!isWholeSubject) colors.accent else colors.secondaryText
                        )
                    }
                }
            }

            if (selectedChapter != null && chapters.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    chapters.forEach { chap ->
                        val isChapSelected = selectedChapter?.id == chap.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(shapes.button)
                                .clickable { selectedChapter = chap }
                                .padding(vertical = 4.dp, horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isChapSelected,
                                onClick = { selectedChapter = chap },
                                colors = RadioButtonDefaults.colors(selectedColor = colors.accent)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(chap.name, style = typography.caption, color = colors.primaryText)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Exam Marks & Duration Options
            Text("Total Marks & Duration:", style = typography.caption, fontWeight = FontWeight.Bold, color = colors.primaryText)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ExamPaperMarks.values().forEach { option ->
                    val isOptSelected = selectedMarks == option
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(shapes.card)
                            .background(if (isOptSelected) colors.accent.copy(alpha = 0.15f) else colors.cardBackground)
                            .border(1.dp, if (isOptSelected) colors.accent else colors.border, shapes.card)
                            .clickable { selectedMarks = option }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${option.marks}M",
                                style = typography.sectionTitle,
                                color = if (isOptSelected) colors.accent else colors.primaryText
                            )
                            Text(
                                text = "${option.durationMinutes}m",
                                style = typography.caption.copy(fontSize = 11.sp),
                                color = colors.secondaryText
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            StudyOSTextField(
                value = customTitle,
                onValueChange = { customTitle = it },
                label = "Paper Title (Optional)",
                placeholder = "e.g. Unit Test 1, Pre-Board Examination",
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            StudyOSButton(
                text = if (isGenerating) "Assembling Paper..." else "Generate & View Paper",
                onClick = {
                    val sub = selectedSubject
                    if (sub == null) {
                        Toast.makeText(context, "Please select a subject", Toast.LENGTH_SHORT).show()
                        return@StudyOSButton
                    }
                    isGenerating = true
                    coroutineScope.launch {
                        val generator = QuestionPaperGenerator(database)
                        val result = generator.generatePaper(
                            subjectId = sub.id,
                            chapterId = selectedChapter?.id,
                            examMarks = selectedMarks,
                            customTitle = customTitle.ifBlank { null }
                        )
                        isGenerating = false
                        Toast.makeText(context, "Question paper created & pinned to Home screen!", Toast.LENGTH_SHORT).show()
                        onPaperGenerated(result.paper.id)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
