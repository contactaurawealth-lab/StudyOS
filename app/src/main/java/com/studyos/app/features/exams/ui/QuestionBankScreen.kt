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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                padding = 12.dp
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text(
                                            text = q.questionText,
                                            style = typography.body,
                                            color = colors.primaryText,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(shapes.button)
                                                .background(colors.accent.copy(alpha = 0.12f))
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "${q.marks}M",
                                                style = typography.caption,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.accent
                                            )
                                        }
                                    }

                                    if (!q.markingScheme.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Scheme: ${q.markingScheme}",
                                            style = typography.caption,
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
    var csvText by remember { mutableStateOf("") }
    var selectedTarget by remember { mutableStateOf(QuestionImportTarget.BOTH) }
    var isImporting by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val content = stream.bufferedReader().readText()
                    csvText = content
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to read CSV: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
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
                Text(
                    text = "Import questions into chapters for your question bank & flashcard decks.",
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
                            val template = processor.getQuestionBankCsvTemplate()
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Question Bank CSV Template", template))
                            Toast.makeText(context, "Template copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f)
                    )

                    StudyOSOutlinedButton(
                        text = "📤 Share Template",
                        onClick = {
                            try {
                                val templateFile = processor.exportQuestionBankTemplateFile(context)
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
                                context.startActivity(Intent.createChooser(sendIntent, "Share Question Bank Template"))
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

                if (errorMsg != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMsg!!,
                        style = typography.caption,
                        color = colors.accent
                    )
                }
            }
        },
        confirmButton = {
            StudyOSButton(
                text = if (isImporting) "Importing..." else "Import",
                onClick = {
                    if (csvText.isBlank()) {
                        errorMsg = "Please enter or select CSV content"
                        return@StudyOSButton
                    }
                    val validation = processor.parseAndValidateQuestions(csvText)
                    if (validation.errors.isNotEmpty()) {
                        errorMsg = "Error: " + validation.errors.first().errorMessage
                        return@StudyOSButton
                    }

                    isImporting = true
                    coroutineScope.launch {
                        val result = processor.importQuestions(validation.parsedData, selectedTarget)
                        isImporting = false
                        if (result.success) {
                            Toast.makeText(context, "Successfully imported ${result.importedCount} questions!", Toast.LENGTH_SHORT).show()
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
