package com.studyos.app.features.document.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FormatColorFill
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.NoteAdd
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material.icons.outlined.ViewDay
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studyos.app.core.ui.component.StudyOSButton
import com.studyos.app.core.ui.component.StudyOSEmptyState
import com.studyos.app.core.ui.component.StudyOSIconButton
import com.studyos.app.core.ui.component.StudyOSLoadingState
import com.studyos.app.core.ui.component.StudyOSOutlinedButton
import com.studyos.app.features.document.viewmodel.DocumentFontSize
import com.studyos.app.features.document.viewmodel.DocumentReadingTheme
import com.studyos.app.features.document.viewmodel.DocumentViewMode
import com.studyos.app.features.document.viewmodel.DocumentViewerViewModel
import com.studyos.app.theme.StudyOSTheme
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentViewerScreen(
    viewModel: DocumentViewerViewModel,
    onBack: () -> Unit,
    onOpenNoteEditor: (noteId: String) -> Unit = {},
    onOpenFlashcardStudy: (chapterId: String) -> Unit = {},
    onOpenQuiz: (quizId: String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    var showOptionsSheet by remember { mutableStateOf(false) }

    // Text-to-Speech Engine
    var ttsEngine by remember { mutableStateOf<TextToSpeech?>(null) }
    var isTtsReady by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        val tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsReady = true
                ttsEngine?.language = Locale.getDefault()
            }
        }
        ttsEngine = tts
        onDispose {
            tts.stop()
            tts.shutdown()
        }
    }

    // File picker launcher for opening any document
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.loadUri(uri)
        }
    }

    LaunchedEffect(uiState.infoMessage) {
        uiState.infoMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearInfoMessage()
        }
    }

    // Scroll to current search match
    LaunchedEffect(uiState.currentMatchIndex, uiState.searchResults) {
        if (uiState.searchResults.isNotEmpty() && uiState.currentMatchIndex in uiState.searchResults.indices) {
            val match = uiState.searchResults[uiState.currentMatchIndex]
            if (uiState.viewMode == DocumentViewMode.VISUAL_CANVAS && uiState.pdfPages.isNotEmpty()) {
                val targetPage = (match.pageNumber - 1).coerceIn(0, uiState.pdfPages.lastIndex)
                listState.animateScrollToItem(targetPage)
            }
        }
    }

    val currentTheme = uiState.readingTheme

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            GoogleDocsTopBar(
                title = uiState.documentTitle,
                fileType = uiState.fileType,
                hasPdfVisuals = uiState.pdfPages.isNotEmpty(),
                viewMode = uiState.viewMode,
                isSearchActive = uiState.isSearchActive,
                isSpeaking = uiState.isTtsSpeaking,
                onBack = onBack,
                onToggleViewMode = {
                    val nextMode = if (uiState.viewMode == DocumentViewMode.VISUAL_CANVAS) {
                        DocumentViewMode.DOCS_READING
                    } else {
                        DocumentViewMode.VISUAL_CANVAS
                    }
                    viewModel.setViewMode(nextMode)
                },
                onToggleSearch = { viewModel.toggleSearch(!uiState.isSearchActive) },
                onOptionsClick = { showOptionsSheet = true },
                onToggleTts = {
                    if (uiState.isTtsSpeaking) {
                        ttsEngine?.stop()
                        viewModel.setTtsSpeaking(false)
                    } else {
                        if (isTtsReady && uiState.fullTextContent.isNotBlank()) {
                            ttsEngine?.setSpeechRate(uiState.ttsSpeechRate)
                            ttsEngine?.speak(
                                uiState.fullTextContent.take(4000),
                                TextToSpeech.QUEUE_FLUSH,
                                null,
                                "DOCUMENT_PREVIEW_TTS"
                            )
                            viewModel.setTtsSpeaking(true)
                        } else {
                            scope.launch {
                                snackbarHostState.showSnackbar("No readable text available to read aloud.")
                            }
                        }
                    }
                },
                onShare = {
                    val shareText = "${uiState.documentTitle}\n\n${uiState.fullTextContent}"
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, uiState.documentTitle)
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Share Document"))
                }
            )
        },
        bottomBar = {
            GoogleDocsBottomActionHub(
                isAiWorking = uiState.isAiWorking,
                onConvertToNote = {
                    viewModel.saveAsNote { noteId ->
                        onOpenNoteEditor(noteId)
                    }
                },
                onGenerateFlashcards = {
                    viewModel.generateFlashcards { chapterId ->
                        onOpenFlashcardStudy(chapterId)
                    }
                },
                onGenerateQuiz = {
                    viewModel.generateQuiz { quizId ->
                        onOpenQuiz(quizId)
                    }
                },
                onOpenOtherDocument = {
                    documentPickerLauncher.launch(
                        arrayOf(
                            "application/pdf",
                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                            "application/msword",
                            "text/*"
                        )
                    )
                }
            )
        },
        containerColor = currentTheme.canvasColor,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Find in Document / Search Bar
            AnimatedVisibility(
                visible = uiState.isSearchActive,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                GoogleDocsSearchBar(
                    searchQuery = uiState.searchQuery,
                    matchCount = uiState.searchResults.size,
                    currentMatchIndex = if (uiState.searchResults.isNotEmpty()) uiState.currentMatchIndex + 1 else 0,
                    onQueryChange = viewModel::onSearchQueryChange,
                    onNextMatch = viewModel::nextSearchMatch,
                    onPreviousMatch = viewModel::previousSearchMatch,
                    onCloseSearch = { viewModel.toggleSearch(false) },
                    theme = currentTheme
                )
            }

            // Text-To-Speech Floating Active Banner
            AnimatedVisibility(
                visible = uiState.isTtsSpeaking,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                TtsActiveAudioPlayerBanner(
                    speechRate = uiState.ttsSpeechRate,
                    onChangeRate = { rate ->
                        viewModel.setTtsSpeechRate(rate)
                        ttsEngine?.setSpeechRate(rate)
                    },
                    onStop = {
                        ttsEngine?.stop()
                        viewModel.setTtsSpeaking(false)
                    },
                    theme = currentTheme
                )
            }

            // Main Paper Canvas Body
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        StudyOSLoadingState(message = "Opening document in Google Docs viewer...")
                    }
                } else if (uiState.errorMessage != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            StudyOSEmptyState(
                                title = "Could not preview document",
                                description = uiState.errorMessage ?: "Format not supported"
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            StudyOSButton(
                                text = "Open Another File",
                                onClick = {
                                    documentPickerLauncher.launch(
                                        arrayOf("application/pdf", "text/*", "application/msword")
                                    )
                                }
                            )
                        }
                    }
                } else {
                    // Document Content Canvas
                    when (uiState.viewMode) {
                        DocumentViewMode.VISUAL_CANVAS -> {
                            PdfVisualCanvas(
                                pages = uiState.pdfPages,
                                theme = currentTheme,
                                listState = listState,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        DocumentViewMode.DOCS_READING -> {
                            GoogleDocsPaperCanvas(
                                title = uiState.documentTitle,
                                content = uiState.fullTextContent,
                                fileType = uiState.fileType,
                                wordCount = uiState.wordCount,
                                characterCount = uiState.characterCount,
                                theme = currentTheme,
                                fontSize = uiState.fontSize,
                                searchQuery = uiState.searchQuery,
                                activeMatchOffset = uiState.searchResults.getOrNull(uiState.currentMatchIndex)?.startOffset,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }

    // Google Docs Formatting & Reading Theme Bottom Sheet
    if (showOptionsSheet) {
        GoogleDocsDisplayOptionsSheet(
            currentTheme = currentTheme,
            currentFontSize = uiState.fontSize,
            hasPdfPages = uiState.pdfPages.isNotEmpty(),
            viewMode = uiState.viewMode,
            wordCount = uiState.wordCount,
            charCount = uiState.characterCount,
            totalPages = uiState.totalPages,
            onSelectTheme = { viewModel.setReadingTheme(it) },
            onSelectFontSize = { viewModel.setFontSize(it) },
            onToggleViewMode = { viewModel.setViewMode(it) },
            onDismiss = { showOptionsSheet = false }
        )
    }
}

// -------------------------------------------------------------------------
// TOP APP BAR: Google Docs / Drive Style
// -------------------------------------------------------------------------
@Composable
private fun GoogleDocsTopBar(
    title: String,
    fileType: String,
    hasPdfVisuals: Boolean,
    viewMode: DocumentViewMode,
    isSearchActive: Boolean,
    isSpeaking: Boolean,
    onBack: () -> Unit,
    onToggleViewMode: () -> Unit,
    onToggleSearch: () -> Unit,
    onOptionsClick: () -> Unit,
    onToggleTts: () -> Unit,
    onShare: () -> Unit
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography

    Surface(
        color = colors.surface.copy(alpha = 0.95f),
        shadowElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Back button & Title with File Type Badge
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.primaryText,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    FileTypeBadge(fileType = fileType)

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = title,
                        style = typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.primaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Right: Action Icons (Search, Toggle View, TTS, Options, Share)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (hasPdfVisuals) {
                        IconButton(
                            onClick = onToggleViewMode,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = if (viewMode == DocumentViewMode.VISUAL_CANVAS) {
                                    Icons.Outlined.Description
                                } else {
                                    Icons.Outlined.PictureAsPdf
                                },
                                contentDescription = "Toggle Reading Mode",
                                tint = colors.accent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onToggleSearch,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "Find in Document",
                            tint = if (isSearchActive) colors.accent else colors.secondaryText,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onToggleTts,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = if (isSpeaking) Icons.Outlined.Stop else Icons.AutoMirrored.Outlined.VolumeUp,
                            contentDescription = "Read Aloud",
                            tint = if (isSpeaking) Color(0xFFEF4444) else colors.secondaryText,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onOptionsClick,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Tune,
                            contentDescription = "Formatting & Display Options",
                            tint = colors.secondaryText,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onShare,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = "Share Document",
                            tint = colors.secondaryText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// FILE TYPE BADGE: Google Drive Color Identity
// -------------------------------------------------------------------------
@Composable
private fun FileTypeBadge(fileType: String) {
    val upper = fileType.uppercase()
    val (badgeBg, badgeText) = when {
        upper.contains("PDF") -> Color(0xFFDC2626) to Color.White
        upper.contains("DOC") -> Color(0xFF2563EB) to Color.White
        upper.contains("MD") -> Color(0xFF0D9488) to Color.White
        upper.contains("TXT") -> Color(0xFFD97706) to Color.White
        else -> Color(0xFF7C3AED) to Color.White
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(badgeBg)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = upper.take(4),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = badgeText,
            letterSpacing = 0.5.sp
        )
    }
}

// -------------------------------------------------------------------------
// SEARCH BAR: Google Docs Find in Document (Ctrl + F)
// -------------------------------------------------------------------------
@Composable
private fun GoogleDocsSearchBar(
    searchQuery: String,
    matchCount: Int,
    currentMatchIndex: Int,
    onQueryChange: (String) -> Unit,
    onNextMatch: () -> Unit,
    onPreviousMatch: () -> Unit,
    onCloseSearch: () -> Unit,
    theme: DocumentReadingTheme
) {
    Surface(
        color = theme.paperColor,
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = theme.secondaryTextColor,
                modifier = Modifier.size(18.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            BasicTextField(
                value = searchQuery,
                onValueChange = onQueryChange,
                textStyle = TextStyle(
                    color = theme.textColor,
                    fontSize = 14.sp
                ),
                cursorBrush = SolidColor(theme.accentColor),
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp),
                decorationBox = { innerTextField ->
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "Find in document...",
                            color = theme.secondaryTextColor.copy(alpha = 0.6f),
                            fontSize = 14.sp
                        )
                    }
                    innerTextField()
                }
            )

            if (searchQuery.isNotBlank()) {
                Text(
                    text = if (matchCount > 0) "$currentMatchIndex of $matchCount" else "0 matches",
                    fontSize = 12.sp,
                    color = if (matchCount > 0) theme.secondaryTextColor else Color(0xFFEF4444),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )

                IconButton(
                    onClick = onPreviousMatch,
                    enabled = matchCount > 0,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.KeyboardArrowUp,
                        contentDescription = "Previous Match",
                        tint = if (matchCount > 0) theme.textColor else theme.secondaryTextColor.copy(alpha = 0.4f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onNextMatch,
                    enabled = matchCount > 0,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.KeyboardArrowDown,
                        contentDescription = "Next Match",
                        tint = if (matchCount > 0) theme.textColor else theme.secondaryTextColor.copy(alpha = 0.4f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            IconButton(
                onClick = onCloseSearch,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Close Find",
                    tint = theme.secondaryTextColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// -------------------------------------------------------------------------
// TTS FLOATING AUDIO PLAYER BANNER
// -------------------------------------------------------------------------
@Composable
private fun TtsActiveAudioPlayerBanner(
    speechRate: Float,
    onChangeRate: (Float) -> Unit,
    onStop: () -> Unit,
    theme: DocumentReadingTheme
) {
    Surface(
        color = theme.accentColor,
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.VolumeUp,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Reading document aloud...",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Speed pills (0.75x, 1.0x, 1.25x, 1.5x)
                val rates = listOf(0.75f, 1.0f, 1.25f, 1.5f)
                val nextRate = rates[(rates.indexOf(speechRate) + 1).coerceAtLeast(0) % rates.size]

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                        .clickable { onChangeRate(nextRate) }
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "${speechRate}x",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onStop,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Stop,
                        contentDescription = "Stop Reading",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// PDF VISUAL CANVAS: High-DPI Rendered Bitmap Pages
// -------------------------------------------------------------------------
@Composable
private fun PdfVisualCanvas(
    pages: List<com.studyos.app.core.util.RenderedPdfPage>,
    theme: DocumentReadingTheme,
    listState: androidx.compose.foundation.lazy.LazyListState,
    modifier: Modifier = Modifier
) {
    if (pages.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No PDF pages rendered.",
                color = theme.secondaryTextColor,
                fontSize = 14.sp
            )
        }
        return
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxSize()
    ) {
        itemsIndexed(pages) { index, page ->
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Paper Sheet Card
                Card(
                    shape = RoundedCornerShape(4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Image(
                        bitmap = page.bitmap.asImageBitmap(),
                        contentDescription = "Page ${page.pageNumber}",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Page Number Tag
                Text(
                    text = "Page ${page.pageNumber} of ${page.totalPages}",
                    color = theme.secondaryTextColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// -------------------------------------------------------------------------
// GOOGLE DOCS PAPER CANVAS: Formatted Paper Sheets with Margins & Typography
// -------------------------------------------------------------------------
@Composable
private fun GoogleDocsPaperCanvas(
    title: String,
    content: String,
    fileType: String,
    wordCount: Int,
    characterCount: Int,
    theme: DocumentReadingTheme,
    fontSize: DocumentFontSize,
    searchQuery: String,
    activeMatchOffset: Int?,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        // Continuous Google Docs Paper Sheet
        Card(
            shape = RoundedCornerShape(4.dp),
            colors = CardDefaults.cardColors(containerColor = theme.paperColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, theme.borderColor, RoundedCornerShape(4.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 28.dp)
            ) {
                // Google Docs Document Header & Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        fontSize = (fontSize.sizeSp + 6).sp,
                        fontWeight = FontWeight.Bold,
                        color = theme.textColor,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$fileType • $wordCount words • $characterCount characters",
                        fontSize = 11.sp,
                        color = theme.secondaryTextColor
                    )
                    Text(
                        text = "Google Docs View",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = theme.accentColor
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Page Rule Divider
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(theme.borderColor)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Selectable, Formatted Document Body with Search Highlighting
                SelectionContainer {
                    val annotatedContent = remember(content, searchQuery, activeMatchOffset, theme) {
                        buildHighlightedDocumentText(
                            text = content,
                            searchQuery = searchQuery,
                            activeMatchOffset = activeMatchOffset,
                            theme = theme
                        )
                    }

                    Text(
                        text = annotatedContent,
                        color = theme.textColor,
                        fontSize = fontSize.sizeSp.sp,
                        lineHeight = fontSize.lineHeightSp.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Document Footer
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(theme.borderColor.copy(alpha = 0.5f))
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "End of document • StudyOS Document Previewer",
                    fontSize = 11.sp,
                    color = theme.secondaryTextColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// -------------------------------------------------------------------------
// ANNOTATED TEXT WITH SEARCH HIGHLIGHTING
// -------------------------------------------------------------------------
private fun buildHighlightedDocumentText(
    text: String,
    searchQuery: String,
    activeMatchOffset: Int?,
    theme: DocumentReadingTheme
): AnnotatedString {
    val query = searchQuery.trim()
    if (query.isBlank()) {
        return AnnotatedString(text)
    }

    return buildAnnotatedString {
        var currentIndex = 0
        while (currentIndex < text.length) {
            val foundAt = text.indexOf(query, startIndex = currentIndex, ignoreCase = true)
            if (foundAt == -1) {
                append(text.substring(currentIndex))
                break
            }

            // Append leading text
            append(text.substring(currentIndex, foundAt))

            // Highlight match
            val isActive = activeMatchOffset != null && foundAt == activeMatchOffset
            val matchEnd = foundAt + query.length

            val highlightStyle = if (isActive) {
                SpanStyle(
                    background = Color(0xFFF97316),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            } else {
                SpanStyle(
                    background = Color(0xFFFEF08A),
                    color = Color(0xFF854D0E),
                    fontWeight = FontWeight.SemiBold
                )
            }

            pushStyle(highlightStyle)
            append(text.substring(foundAt, matchEnd))
            pop()

            currentIndex = matchEnd
        }
    }
}

// -------------------------------------------------------------------------
// BOTTOM ACTION HUB: Convert to Note, AI Flashcards, AI Quiz, Open Other File
// -------------------------------------------------------------------------
@Composable
private fun GoogleDocsBottomActionHub(
    isAiWorking: Boolean,
    onConvertToNote: () -> Unit,
    onGenerateFlashcards: () -> Unit,
    onGenerateQuiz: () -> Unit,
    onOpenOtherDocument: () -> Unit
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography

    Surface(
        color = colors.surface.copy(alpha = 0.96f),
        shadowElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            if (isAiWorking) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        color = colors.accent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AI is studying document & generating practice content...",
                        style = typography.caption,
                        color = colors.accent
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Convert to Note
                StudyOSButton(
                    text = "Convert to Note",
                    onClick = onConvertToNote,
                    modifier = Modifier.height(38.dp)
                )

                // AI Flashcards
                StudyOSOutlinedButton(
                    text = "AI Flashcards",
                    onClick = onGenerateFlashcards,
                    modifier = Modifier.height(38.dp)
                )

                // AI Quiz
                StudyOSOutlinedButton(
                    text = "AI Quiz",
                    onClick = onGenerateQuiz,
                    modifier = Modifier.height(38.dp)
                )

                // Open Other File
                StudyOSOutlinedButton(
                    text = "Open File",
                    onClick = onOpenOtherDocument,
                    modifier = Modifier.height(38.dp)
                )
            }
        }
    }
}

// -------------------------------------------------------------------------
// DISPLAY OPTIONS SHEET: Themes, Font Size, Layout
// -------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoogleDocsDisplayOptionsSheet(
    currentTheme: DocumentReadingTheme,
    currentFontSize: DocumentFontSize,
    hasPdfPages: Boolean,
    viewMode: DocumentViewMode,
    wordCount: Int,
    charCount: Int,
    totalPages: Int,
    onSelectTheme: (DocumentReadingTheme) -> Unit,
    onSelectFontSize: (DocumentFontSize) -> Unit,
    onToggleViewMode: (DocumentViewMode) -> Unit,
    onDismiss: () -> Unit
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
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Document View & Theme",
                style = typography.sectionTitle,
                color = colors.primaryText
            )
            Text(
                text = "$wordCount words • $charCount chars • $totalPages pages",
                style = typography.caption,
                color = colors.secondaryText
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Reading Themes
            Text(
                text = "READING THEME",
                style = typography.caption.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                color = colors.mutedText
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DocumentReadingTheme.values().forEach { theme ->
                    val isSelected = theme == currentTheme
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(theme.paperColor)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) colors.accent else colors.border,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onSelectTheme(theme) }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = theme.label,
                            color = theme.textColor,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Font Sizes
            Text(
                text = "FONT SIZE",
                style = typography.caption.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                color = colors.mutedText
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DocumentFontSize.values().forEach { size ->
                    val isSelected = size == currentFontSize
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) colors.accent.copy(alpha = 0.15f) else colors.surface.copy(alpha = 0.5f))
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) colors.accent else colors.border,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onSelectFontSize(size) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = size.label,
                            color = if (isSelected) colors.accent else colors.primaryText,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            if (hasPdfPages) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "PDF RENDERING MODE",
                    style = typography.caption.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = colors.mutedText
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isVisual = viewMode == DocumentViewMode.VISUAL_CANVAS
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isVisual) colors.accent.copy(alpha = 0.15f) else colors.surface.copy(alpha = 0.5f))
                            .border(
                                width = if (isVisual) 2.dp else 1.dp,
                                color = if (isVisual) colors.accent else colors.border,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onToggleViewMode(DocumentViewMode.VISUAL_CANVAS) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Visual Canvas",
                            color = if (isVisual) colors.accent else colors.primaryText,
                            fontSize = 12.sp,
                            fontWeight = if (isVisual) FontWeight.Bold else FontWeight.Normal
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (!isVisual) colors.accent.copy(alpha = 0.15f) else colors.surface.copy(alpha = 0.5f))
                            .border(
                                width = if (!isVisual) 2.dp else 1.dp,
                                color = if (!isVisual) colors.accent else colors.border,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onToggleViewMode(DocumentViewMode.DOCS_READING) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Google Docs View",
                            color = if (!isVisual) colors.accent else colors.primaryText,
                            fontSize = 12.sp,
                            fontWeight = if (!isVisual) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}
