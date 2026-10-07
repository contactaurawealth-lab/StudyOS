package com.studyos.app.features.exams.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studyos.app.core.database.StudyOSDatabase
import com.studyos.app.core.database.entity.PaperEntity
import com.studyos.app.core.database.entity.QuestionBankEntity
import com.studyos.app.core.paper.PdfQuestionPaperGenerator
import com.studyos.app.core.ui.component.GlassCard
import com.studyos.app.core.ui.component.GlassIconButton
import com.studyos.app.core.ui.component.GlassTopBar
import com.studyos.app.core.ui.component.StudyOSButton
import com.studyos.app.core.ui.component.StudyOSOutlinedButton
import com.studyos.app.core.ui.component.StudyOSTextField
import com.studyos.app.core.util.DocumentOpener
import com.studyos.app.theme.StudyOSTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionPaperViewerScreen(
    paperId: String,
    database: StudyOSDatabase,
    onBack: () -> Unit,
    onOpenExams: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    var paper by remember { mutableStateOf<PaperEntity?>(null) }
    var questions by remember { mutableStateOf<List<QuestionBankEntity>>(emptyList()) }
    var subjectName by remember { mutableStateOf("Subject") }
    var chapterName by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showSolutions by remember { mutableStateOf(false) }
    val expandedSolutions = remember { mutableStateMapOf<String, Boolean>() }
    var marksInputText by remember { mutableStateOf("") }
    var recordedScore by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(paperId) {
        withContext(Dispatchers.IO) {
            val p = database.paperDao().getPaperById(paperId)
            if (p != null) {
                paper = p
                val sub = database.subjectDao().getSubject(p.subjectId)
                subjectName = sub?.name ?: "Subject"

                val crossRefs = database.paperDao().getQuestionsForPaper(paperId)
                val qList = mutableListOf<QuestionBankEntity>()
                for (ref in crossRefs) {
                    database.questionBankDao().getQuestionById(ref.questionId)?.let {
                        qList.add(it)
                    }
                }
                questions = qList
                if (qList.isNotEmpty()) {
                    val firstChap = qList.firstNotNullOfOrNull { it.chapterId }
                    if (firstChap != null) {
                        chapterName = database.chapterDao().getChapterByIdOnce(firstChap)?.name
                    }
                }

                // Check if linked exam already has recorded score
                val exams = database.examDao().getUpcomingExamsOnce(0)
                val linkedExam = exams.find { it.notes?.contains("[PAPER_ID:$paperId]") == true }
                recordedScore = linkedExam?.actualScore
            }
            isLoading = false
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.background,
        topBar = {
            GlassTopBar(
                title = "Question Paper",
                subtitle = "${paper?.totalMarks ?: 0} Marks • ${paper?.durationMinutes ?: 0} mins",
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
                    // Open in Native External PDF Reader
                    GlassIconButton(
                        onClick = {
                            val currentPaper = paper ?: return@GlassIconButton
                            coroutineScope.launch {
                                val generator = PdfQuestionPaperGenerator()
                                val file = withContext(Dispatchers.IO) {
                                    generator.generatePdf(context, currentPaper, subjectName, chapterName, questions)
                                }
                                DocumentOpener.openPdfInExternalApp(context, file.absolutePath, currentPaper.title)
                            }
                        },
                        contentDescription = "Open in PDF App"
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PictureAsPdf,
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
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = colors.accent)
            }
        } else if (paper == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Question paper not found.", color = colors.secondaryText)
            }
        } else {
            val curPaper = paper!!
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.TopCenter
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 680.dp)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. Direct Marks Input Box Card
                    item {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            padding = 16.dp,
                            backgroundColor = colors.cardBackground
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Marks Obtained in this Paper",
                                        style = typography.sectionTitle,
                                        color = colors.primaryText
                                    )
                                    if (recordedScore != null) {
                                        Box(
                                            modifier = Modifier
                                                .clip(shapes.button)
                                                .background(colors.accent.copy(alpha = 0.15f))
                                                .border(0.5.dp, colors.accent, shapes.button)
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "Score: $recordedScore / ${curPaper.totalMarks}",
                                                style = typography.caption,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.accent
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    StudyOSTextField(
                                        value = marksInputText,
                                        onValueChange = { marksInputText = it },
                                        placeholder = "Enter marks scored (e.g. ${curPaper.totalMarks * 8 / 10})",
                                        modifier = Modifier.weight(1f)
                                    )

                                    StudyOSButton(
                                        text = if (recordedScore != null) "Update" else "Save Marks",
                                        onClick = {
                                            val score = marksInputText.trim().toIntOrNull()
                                            if (score != null) {
                                                coroutineScope.launch {
                                                    withContext(Dispatchers.IO) {
                                                        val allExams = database.examDao().getUpcomingExamsOnce(0)
                                                        val linkedExam = allExams.find { it.notes?.contains("[PAPER_ID:$paperId]") == true }
                                                        if (linkedExam != null) {
                                                            database.examDao().updateExamScore(linkedExam.id, score, isCompleted = true)
                                                        }
                                                    }
                                                    recordedScore = score
                                                    Toast.makeText(context, "Marks saved ($score/${curPaper.totalMarks})! Updated in Exams.", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // 2. Action Bar: Open in PDF App, Marking Scheme PDF & Toggle Solutions
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                StudyOSButton(
                                    text = "📄 Open Paper PDF",
                                    onClick = {
                                        coroutineScope.launch {
                                            val generator = PdfQuestionPaperGenerator()
                                            val file = withContext(Dispatchers.IO) {
                                                generator.generatePdf(context, curPaper, subjectName, chapterName, questions, includeMarkingScheme = false)
                                            }
                                            DocumentOpener.openPdfInExternalApp(context, file.absolutePath, curPaper.title)
                                        }
                                    },
                                    modifier = Modifier.weight(1.2f)
                                )

                                StudyOSOutlinedButton(
                                    text = if (showSolutions) "Hide Answers" else "Show Answers",
                                    onClick = { showSolutions = !showSolutions },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            StudyOSOutlinedButton(
                                text = "💡 Export Marking Scheme & Solutions PDF",
                                onClick = {
                                    coroutineScope.launch {
                                        val generator = PdfQuestionPaperGenerator()
                                        val file = withContext(Dispatchers.IO) {
                                            generator.generatePdf(context, curPaper, subjectName, chapterName, questions, includeMarkingScheme = true)
                                        }
                                        DocumentOpener.openPdfInExternalApp(context, file.absolutePath, "${curPaper.title} - Marking Scheme")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // 3. Official Printable Paper View
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(shapes.surface)
                                .background(colors.surface)
                                .border(1.dp, colors.border, shapes.surface)
                                .padding(20.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // Institutional Header
                                Text(
                                    text = "STUDYOS EXAMINATION BOARD",
                                    style = typography.caption.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.5.sp
                                    ),
                                    textAlign = TextAlign.Center,
                                    color = colors.secondaryText,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = curPaper.title.uppercase(),
                                    style = typography.sectionTitle.copy(
                                        fontFamily = FontFamily.Serif,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    textAlign = TextAlign.Center,
                                    color = colors.primaryText,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(thickness = 1.dp, color = colors.border)
                                Spacer(modifier = Modifier.height(8.dp))

                                // Metadata Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Subject: $subjectName", style = typography.caption, fontWeight = FontWeight.Bold, color = colors.primaryText)
                                        if (chapterName != null) {
                                            Text("Chapter: $chapterName", style = typography.caption, color = colors.secondaryText)
                                        } else {
                                            Text("Scope: Complete Syllabus", style = typography.caption, color = colors.secondaryText)
                                        }
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Max Marks: ${curPaper.totalMarks}", style = typography.caption, fontWeight = FontWeight.Bold, color = colors.primaryText)
                                        Text("Time: ${curPaper.durationMinutes} Minutes", style = typography.caption, color = colors.secondaryText)
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(thickness = 1.dp, color = colors.border)
                                Spacer(modifier = Modifier.height(10.dp))

                                // Instructions
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(shapes.card)
                                        .background(colors.cardBackground)
                                        .padding(10.dp)
                                ) {
                                    Column {
                                        Text("General Instructions:", style = typography.caption, fontWeight = FontWeight.Bold, color = colors.primaryText)
                                        Text("1. All questions are compulsory.", style = typography.caption.copy(fontSize = 11.sp), color = colors.secondaryText)
                                        Text("2. Marks are indicated on the right margin against each question.", style = typography.caption.copy(fontSize = 11.sp), color = colors.secondaryText)
                                        Text("3. Show step-by-step working and formulas where applicable.", style = typography.caption.copy(fontSize = 11.sp), color = colors.secondaryText)
                                    }
                                }
                            }
                        }
                    }

                    // 4. Questions Rendered
                    val groupedSecA = questions.filter { it.marks == 1 }
                    val groupedSecB = questions.filter { it.marks in 2..3 }
                    val groupedSecC = questions.filter { it.marks >= 4 }

                    fun renderSectionItems(secTitle: String, secQuestions: List<QuestionBankEntity>, startNumber: Int): Int {
                        if (secQuestions.isEmpty()) return startNumber
                        item {
                            Text(
                                text = secTitle.uppercase(),
                                style = typography.caption.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                                color = colors.accent,
                                modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                            )
                        }
                        var qNum = startNumber
                        itemsIndexed(secQuestions) { _, q ->
                            val isExpanded = expandedSolutions[q.id] ?: showSolutions

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(shapes.card)
                                    .background(colors.surface)
                                    .border(0.5.dp, colors.border.copy(alpha = 0.5f), shapes.card)
                                    .padding(14.dp)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text(
                                            text = "Q$qNum.",
                                            style = typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.primaryText,
                                            modifier = Modifier.width(36.dp)
                                        )

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

                                    // Answer / Marking Scheme Accordion
                                    if (q.markingScheme.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier
                                                .clip(shapes.button)
                                                .clickable {
                                                    expandedSolutions[q.id] = !(expandedSolutions[q.id] ?: showSolutions)
                                                }
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (isExpanded) "Hide Model Solution" else "View Model Solution",
                                                style = typography.caption,
                                                fontWeight = FontWeight.SemiBold,
                                                color = colors.accent
                                            )
                                            Icon(
                                                imageVector = if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                                                contentDescription = null,
                                                tint = colors.accent,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        if (isExpanded) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(shapes.button)
                                                    .background(colors.accent.copy(alpha = 0.08f))
                                                    .border(0.5.dp, colors.accent.copy(alpha = 0.3f), shapes.button)
                                                    .padding(10.dp)
                                            ) {
                                                Column {
                                                    Text(
                                                        text = "Marking Scheme / Expected Answer:",
                                                        style = typography.caption.copy(fontWeight = FontWeight.Bold),
                                                        color = colors.accent
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = q.markingScheme,
                                                        style = typography.secondary,
                                                        color = colors.primaryText
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            qNum++
                        }
                        return qNum
                    }

                    var currQ = 1
                    currQ = renderSectionItems("Section A: Objective & Conceptual (1 Mark Each)", groupedSecA, currQ)
                    currQ = renderSectionItems("Section B: Short Answer Questions (2 - 3 Marks Each)", groupedSecB, currQ)
                    renderSectionItems("Section C: Long Answer & Problem Solving (4+ Marks Each)", groupedSecC, currQ)

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}
