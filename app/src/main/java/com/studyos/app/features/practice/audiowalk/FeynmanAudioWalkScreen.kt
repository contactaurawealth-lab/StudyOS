package com.studyos.app.features.practice.audiowalk

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.FastForward
import androidx.compose.material.icons.outlined.FastRewind
import androidx.compose.material.icons.outlined.Forward10
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MicNone
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Replay10
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.studyos.app.core.ui.component.StudyOSButton
import com.studyos.app.core.ui.component.StudyOSIconButton
import com.studyos.app.core.ui.component.StudyOSOutlinedButton
import com.studyos.app.core.ui.component.StudyOSTextField
import com.studyos.app.theme.StudyOSTheme
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun FeynmanAudioWalkScreen(
    viewModel: FeynmanAudioWalkViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes
    val context = LocalContext.current

    // Speech & Audio engines
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var isTtsReady by remember { mutableStateOf(false) }
    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }
    var isSpeechRecognizerListening by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    // Audio file picker
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "audio_revision.mp3"
            viewModel.setUploadedAudio(uri, fileName)
        }
    }

    // Microphone permission launcher
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
    }

    // Initialize TTS
    DisposableEffect(context) {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                engine?.language = Locale.getDefault()
                isTtsReady = true
            }
        }
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                when (utteranceId) {
                    "QUESTION_TTS" -> viewModel.onQuestionTtsFinished()
                    "FEEDBACK_TTS" -> viewModel.onFeedbackTtsFinished()
                }
            }
            override fun onError(utteranceId: String?) {}
        })
        tts = engine

        onDispose {
            engine.stop()
            engine.shutdown()
        }
    }

    // Initialize Speech Recognizer
    DisposableEffect(context) {
        var recognizer: SpeechRecognizer? = null
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        isSpeechRecognizerListening = true
                    }
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {
                        isSpeechRecognizerListening = false
                    }
                    override fun onError(error: Int) {
                        isSpeechRecognizerListening = false
                    }
                    override fun onResults(results: Bundle?) {
                        isSpeechRecognizerListening = false
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        if (text.isNotBlank()) {
                            viewModel.onUserAnswerSpoken(text)
                        }
                    }
                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
            speechRecognizer = recognizer
        }

        onDispose {
            recognizer?.destroy()
        }
    }

    // Manage Media Player for uploaded audio
    DisposableEffect(uiState.uploadedAudioUri) {
        val uri = uiState.uploadedAudioUri
        if (uri != null) {
            try {
                mediaPlayer?.release()
                val player = MediaPlayer().apply {
                    setDataSource(context, uri)
                    prepareAsync()
                    setOnPreparedListener { mp ->
                        viewModel.updateAudioPosition(0, mp.duration)
                    }
                    setOnCompletionListener {
                        viewModel.setAudioPlaying(false)
                    }
                }
                mediaPlayer = player
            } catch (e: Exception) {
                // Ignore player setup errors
            }
        }
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    // Media Player progress polling
    LaunchedEffect(uiState.isAudioRevisionPlaying) {
        while (uiState.isAudioRevisionPlaying) {
            mediaPlayer?.let { mp ->
                if (mp.isPlaying) {
                    viewModel.updateAudioPosition(mp.currentPosition, mp.duration)
                }
            }
            delay(500L)
        }
    }

    // Trigger TTS for Question
    val currentQuestion = uiState.questions.getOrNull(uiState.currentIndex)
    LaunchedEffect(uiState.walkStatus, currentQuestion, isTtsReady) {
        if (isTtsReady && uiState.walkStatus == WalkStatus.SPEAKING_QUESTION && currentQuestion != null) {
            tts?.speak(currentQuestion.question, TextToSpeech.QUEUE_FLUSH, null, "QUESTION_TTS")
        }
    }

    // Trigger TTS for Socratic Feedback
    LaunchedEffect(uiState.walkStatus, uiState.lastEvaluationFeedback, isTtsReady) {
        if (isTtsReady && uiState.walkStatus == WalkStatus.SPEAKING_FEEDBACK && uiState.lastEvaluationFeedback.isNotBlank()) {
            tts?.speak(uiState.lastEvaluationFeedback, TextToSpeech.QUEUE_FLUSH, null, "FEEDBACK_TTS")
        }
    }

    // Trigger STT when Listening
    LaunchedEffect(uiState.walkStatus, hasMicPermission) {
        if (uiState.walkStatus == WalkStatus.LISTENING_ANSWER) {
            if (!hasMicPermission) {
                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            } else if (speechRecognizer != null) {
                try {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    }
                    speechRecognizer?.startListening(intent)
                    isSpeechRecognizerListening = true
                } catch (e: Exception) {
                    // Ignore mic error
                }
            }
        } else {
            speechRecognizer?.stopListening()
            isSpeechRecognizerListening = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StudyOSIconButton(
                    onClick = {
                        tts?.stop()
                        speechRecognizer?.stopListening()
                        mediaPlayer?.pause()
                        onNavigateBack()
                    },
                    contentDescription = "Back"
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = colors.primaryText,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        text = "Feynman Audio Walk",
                        style = typography.secondary,
                        fontWeight = FontWeight.Bold,
                        color = colors.primaryText
                    )
                    Text(
                        text = "${uiState.subjectName}${if (uiState.chapterName != null) " • " + uiState.chapterName else ""}",
                        style = typography.caption,
                        color = colors.secondaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Walk Mode Badge
            Box(
                modifier = Modifier
                    .clip(shapes.button)
                    .background(colors.cardBackground)
                    .border(1.dp, colors.accent.copy(alpha = 0.5f), shapes.button)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Headphones,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Hands-Free",
                        style = typography.caption,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.accent
                    )
                }
            }
        }

        // Tab Row: Socratic Walk vs Audio Revision
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .clip(shapes.button)
                .background(colors.cardBackground)
                .border(1.dp, colors.border, shapes.button)
                .padding(3.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(shapes.button)
                    .background(if (uiState.activeTab == 0) colors.accent else Color.Transparent)
                    .clickable { viewModel.selectTab(0) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.RecordVoiceOver,
                        contentDescription = null,
                        tint = if (uiState.activeTab == 0) colors.background else colors.secondaryText,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Socratic Walk",
                        style = typography.caption,
                        fontWeight = if (uiState.activeTab == 0) FontWeight.Bold else FontWeight.Normal,
                        color = if (uiState.activeTab == 0) colors.background else colors.secondaryText
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(shapes.button)
                    .background(if (uiState.activeTab == 1) colors.accent else Color.Transparent)
                    .clickable { viewModel.selectTab(1) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Audiotrack,
                        contentDescription = null,
                        tint = if (uiState.activeTab == 1) colors.background else colors.secondaryText,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Audio Player",
                        style = typography.caption,
                        fontWeight = if (uiState.activeTab == 1) FontWeight.Bold else FontWeight.Normal,
                        color = if (uiState.activeTab == 1) colors.background else colors.secondaryText
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (uiState.activeTab == 0) {
            // Socratic Walk Tab Content
            SocraticWalkContent(
                uiState = uiState,
                isSpeechListening = isSpeechRecognizerListening,
                onStartWalk = { duration -> viewModel.startWalk(duration) },
                onPauseWalk = { viewModel.pauseWalk() },
                onResumeWalk = { viewModel.resumeWalk() },
                onSkip = { viewModel.nextQuestion() },
                onRepeat = { viewModel.repeatCurrentQuestion() },
                onExplain = { viewModel.explainCurrentConcept() },
                onManualAnswer = { transcript -> viewModel.onUserAnswerSpoken(transcript) }
            )
        } else {
            // Audio Revision Player Tab Content
            AudioRevisionPlayerContent(
                uiState = uiState,
                onSelectFile = { audioPickerLauncher.launch(arrayOf("audio/*")) },
                onTogglePlay = {
                    mediaPlayer?.let { mp ->
                        if (mp.isPlaying) {
                            mp.pause()
                            viewModel.setAudioPlaying(false)
                        } else {
                            mp.start()
                            viewModel.setAudioPlaying(true)
                        }
                    }
                },
                onSeek = { positionMs ->
                    mediaPlayer?.seekTo(positionMs)
                    viewModel.updateAudioPosition(positionMs, mediaPlayer?.duration ?: 0)
                },
                onRewind10 = {
                    mediaPlayer?.let { mp ->
                        val newPos = (mp.currentPosition - 10000).coerceAtLeast(0)
                        mp.seekTo(newPos)
                        viewModel.updateAudioPosition(newPos, mp.duration)
                    }
                },
                onForward10 = {
                    mediaPlayer?.let { mp ->
                        val newPos = (mp.currentPosition + 10000).coerceAtMost(mp.duration)
                        mp.seekTo(newPos)
                        viewModel.updateAudioPosition(newPos, mp.duration)
                    }
                },
                onSetSpeed = { speed ->
                    mediaPlayer?.let { mp ->
                        try {
                            mp.playbackParams = mp.playbackParams.setSpeed(speed)
                            viewModel.setPlaybackSpeed(speed)
                        } catch (e: Exception) {
                            // Ignore speed error
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun SocraticWalkContent(
    uiState: FeynmanAudioWalkUiState,
    isSpeechListening: Boolean,
    onStartWalk: (Int) -> Unit,
    onPauseWalk: () -> Unit,
    onResumeWalk: () -> Unit,
    onSkip: () -> Unit,
    onRepeat: () -> Unit,
    onExplain: () -> Unit,
    onManualAnswer: (String) -> Unit
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    val currentQ = uiState.questions.getOrNull(uiState.currentIndex)
    var textAnswerFallback by remember { mutableStateOf("") }
    var isManualTextOpen by remember { mutableStateOf(false) }

    // Pulsing animation for mic orb
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.22f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Walk Timer & Session Ring Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shapes.card)
                    .background(colors.cardBackground)
                    .border(1.dp, colors.border, shapes.card)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        val minutes = uiState.remainingSeconds / 60
                        val seconds = uiState.remainingSeconds % 60
                        val timeStr = "%02d:%02d".format(minutes, seconds)

                        Text(
                            text = timeStr,
                            style = typography.screenTitle.copy(fontSize = 32.sp, fontWeight = FontWeight.Bold),
                            color = if (uiState.isTimerActive) colors.accent else colors.secondaryText
                        )
                        Text(
                            text = if (uiState.walkStatus == WalkStatus.IDLE) "15-Min Walk Session" else "Walk Active • Earphones In",
                            style = typography.caption,
                            color = colors.mutedText
                        )
                    }

                    // Progress / Status indicator
                    val progress = if (uiState.targetDurationMinutes > 0) {
                        (uiState.remainingSeconds.toFloat() / (uiState.targetDurationMinutes * 60f)).coerceIn(0f, 1f)
                    } else 0f

                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.size(52.dp),
                            color = colors.accent,
                            trackColor = colors.border,
                            strokeWidth = 4.dp
                        )
                        Icon(
                            imageVector = when (uiState.walkStatus) {
                                WalkStatus.SPEAKING_QUESTION -> Icons.Outlined.VolumeUp
                                WalkStatus.LISTENING_ANSWER -> Icons.Outlined.Mic
                                WalkStatus.EVALUATING -> Icons.Outlined.RecordVoiceOver
                                WalkStatus.SPEAKING_FEEDBACK -> Icons.Outlined.VolumeUp
                                WalkStatus.FINISHED -> Icons.Outlined.Check
                                else -> Icons.Outlined.Headphones
                            },
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Idle State: Start Session
        if (uiState.walkStatus == WalkStatus.IDLE) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shapes.card)
                        .background(colors.surface)
                        .border(1.dp, colors.border, shapes.card)
                        .padding(18.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "🎧 Ready for your Socratic Walk?",
                            style = typography.sectionTitle,
                            color = colors.primaryText,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Synthesized from ${uiState.notesCount} lecture notes and ${uiState.mistakesCount} mistake bank items. Put on your earphones, hit start, and explain concepts out loud.",
                            style = typography.caption,
                            color = colors.secondaryText,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Duration pickers
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            listOf(10, 15, 25).forEach { mins ->
                                val isSelected = uiState.targetDurationMinutes == mins
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(shapes.button)
                                        .background(if (isSelected) colors.accent else colors.cardBackground)
                                        .border(1.dp, if (isSelected) colors.accent else colors.border, shapes.button)
                                        .clickable { onStartWalk(mins) }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$mins Min",
                                        style = typography.caption,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) colors.background else colors.primaryText
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        StudyOSButton(
                            text = "Start ${uiState.targetDurationMinutes}-Min Audio Walk",
                            onClick = { onStartWalk(uiState.targetDurationMinutes) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        } else if (uiState.walkStatus == WalkStatus.FINISHED) {
            // Finished State
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shapes.card)
                        .background(colors.surface)
                        .border(1.dp, colors.accent, shapes.card)
                        .padding(20.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Walk Session Completed! 🎉",
                            style = typography.screenTitle,
                            color = colors.primaryText,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "You completed active verbal recall across ${uiState.questions.size} Socratic prompts. Great job articulating ideas out loud!",
                            style = typography.caption,
                            color = colors.secondaryText,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        StudyOSButton(
                            text = "Start Another Walk Review",
                            onClick = { onStartWalk(15) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        } else {
            // Active Walk: Question Card
            if (currentQ != null) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shapes.card)
                            .background(colors.surface)
                            .border(1.dp, colors.border, shapes.card)
                            .padding(18.dp)
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
                                        text = currentQ.sourceContext,
                                        style = typography.caption,
                                        color = colors.accent
                                    )
                                }

                                Text(
                                    text = "Prompt ${uiState.currentIndex + 1} of ${uiState.questions.size}",
                                    style = typography.caption,
                                    color = colors.mutedText
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = currentQ.question,
                                style = typography.sectionTitle.copy(fontSize = 19.sp, lineHeight = 26.sp),
                                fontWeight = FontWeight.SemiBold,
                                color = colors.primaryText
                            )

                            if (uiState.walkStatus == WalkStatus.SPEAKING_QUESTION) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.VolumeUp,
                                        contentDescription = null,
                                        tint = colors.accent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "AI speaking prompt aloud...",
                                        style = typography.caption,
                                        color = colors.accent
                                    )
                                }
                            }
                        }
                    }
                }

                // Socratic Response / Mic Card
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shapes.card)
                            .background(colors.cardBackground)
                            .border(
                                1.dp,
                                if (uiState.walkStatus == WalkStatus.LISTENING_ANSWER) colors.accent else colors.border,
                                shapes.card
                            )
                            .padding(18.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = when (uiState.walkStatus) {
                                    WalkStatus.LISTENING_ANSWER -> "Listening to your spoken answer..."
                                    WalkStatus.EVALUATING -> "Evaluating your explanation..."
                                    WalkStatus.SPEAKING_FEEDBACK -> "Socratic Feedback"
                                    else -> "Your Spoken Response"
                                },
                                style = typography.secondary,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.primaryText
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Glowing Mic Orb
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .scale(if (uiState.walkStatus == WalkStatus.LISTENING_ANSWER) pulseScale else 1f)
                                    .clip(CircleShape)
                                    .background(
                                        if (uiState.walkStatus == WalkStatus.LISTENING_ANSWER) {
                                            Brush.radialGradient(listOf(colors.accent, colors.accent.copy(alpha = 0.4f)))
                                        } else {
                                            Brush.linearGradient(listOf(colors.surface, colors.cardBackground))
                                        }
                                    )
                                    .border(2.dp, colors.accent, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (uiState.walkStatus == WalkStatus.LISTENING_ANSWER) Icons.Outlined.Mic else Icons.Outlined.MicNone,
                                    contentDescription = "Microphone",
                                    tint = if (uiState.walkStatus == WalkStatus.LISTENING_ANSWER) colors.background else colors.accent,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Spoken transcript display
                            if (uiState.spokenTranscript.isNotBlank()) {
                                Text(
                                    text = "\"${uiState.spokenTranscript}\"",
                                    style = typography.caption.copy(fontSize = 13.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                                    color = colors.primaryText,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                            }

                            // Socratic Feedback Bubble
                            if (uiState.lastEvaluationFeedback.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(shapes.button)
                                        .background(colors.surface)
                                        .border(1.dp, colors.accent.copy(alpha = 0.5f), shapes.button)
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = uiState.lastEvaluationFeedback,
                                        style = typography.caption.copy(fontSize = 13.sp),
                                        color = colors.primaryText
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            // Manual text fallback toggle for noisy outdoor environments
                            AnimatedVisibility(visible = isManualTextOpen) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    StudyOSTextField(
                                        value = textAnswerFallback,
                                        onValueChange = { textAnswerFallback = it },
                                        placeholder = "Type your explanation if in noisy area...",
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    StudyOSButton(
                                        text = "Submit Written Answer",
                                        onClick = {
                                            if (textAnswerFallback.isNotBlank()) {
                                                onManualAnswer(textAnswerFallback)
                                                textAnswerFallback = ""
                                                isManualTextOpen = false
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                }
                            }

                            // Control buttons row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StudyOSIconButton(
                                    onClick = onRepeat,
                                    contentDescription = "Repeat Question"
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Replay,
                                        contentDescription = "Repeat",
                                        tint = colors.secondaryText,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                StudyOSOutlinedButton(
                                    text = if (isManualTextOpen) "Hide Keyboard" else "⌨️ Type Answer",
                                    onClick = { isManualTextOpen = !isManualTextOpen }
                                )

                                StudyOSOutlinedButton(
                                    text = "💡 Explain",
                                    onClick = onExplain
                                )

                                StudyOSIconButton(
                                    onClick = onSkip,
                                    contentDescription = "Next Question"
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.SkipNext,
                                        contentDescription = "Next",
                                        tint = colors.accent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioRevisionPlayerContent(
    uiState: FeynmanAudioWalkUiState,
    onSelectFile: () -> Unit,
    onTogglePlay: () -> Unit,
    onSeek: (Int) -> Unit,
    onRewind10: () -> Unit,
    onForward10: () -> Unit,
    onSetSpeed: (Float) -> Unit
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Upload / Pick Audio File Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shapes.card)
                    .background(colors.cardBackground)
                    .border(1.dp, colors.border, shapes.card)
                    .clickable { onSelectFile() }
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.UploadFile,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (uiState.uploadedAudioFileName != null) {
                            uiState.uploadedAudioFileName
                        } else {
                            "Select Audio Revision (.mp3, .wav, .m4a)"
                        },
                        style = typography.secondary,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.primaryText,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Listen to your recorded lecture or voice note while walking",
                        style = typography.caption,
                        color = colors.secondaryText,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        if (uiState.uploadedAudioUri != null) {
            item {
                // Integrated Audio Player Controls
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shapes.card)
                        .background(colors.surface)
                        .border(1.dp, colors.border, shapes.card)
                        .padding(20.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = uiState.uploadedAudioFileName ?: "Audio Revision Track",
                            style = typography.sectionTitle,
                            color = colors.primaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Seek Slider
                        val currSec = uiState.currentAudioPositionMs / 1000
                        val totalSec = (uiState.totalAudioDurationMs / 1000).coerceAtLeast(1)
                        val currFormatted = "%02d:%02d".format(currSec / 60, currSec % 60)
                        val totalFormatted = "%02d:%02d".format(totalSec / 60, totalSec % 60)

                        Slider(
                            value = (uiState.currentAudioPositionMs.toFloat() / uiState.totalAudioDurationMs.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f),
                            onValueChange = { frac ->
                                val target = (frac * uiState.totalAudioDurationMs).toInt()
                                onSeek(target)
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = colors.accent,
                                activeTrackColor = colors.accent,
                                inactiveTrackColor = colors.border
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = currFormatted, style = typography.caption, color = colors.secondaryText)
                            Text(text = totalFormatted, style = typography.caption, color = colors.secondaryText)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Controls: Rewind 10, Play/Pause, Forward 10
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StudyOSIconButton(
                                onClick = onRewind10,
                                contentDescription = "Rewind 10s"
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Replay10,
                                    contentDescription = null,
                                    tint = colors.primaryText,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(20.dp))

                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(colors.accent)
                                    .clickable { onTogglePlay() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (uiState.isAudioRevisionPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                                    contentDescription = if (uiState.isAudioRevisionPlaying) "Pause" else "Play",
                                    tint = colors.background,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(20.dp))

                            StudyOSIconButton(
                                onClick = onForward10,
                                contentDescription = "Forward 10s"
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Forward10,
                                    contentDescription = null,
                                    tint = colors.primaryText,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Speed selector
                        Text(
                            text = "Playback Speed",
                            style = typography.caption,
                            color = colors.secondaryText
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { spd ->
                                val isSelected = uiState.playbackSpeed == spd
                                Box(
                                    modifier = Modifier
                                        .clip(shapes.button)
                                        .background(if (isSelected) colors.accent else colors.cardBackground)
                                        .border(1.dp, if (isSelected) colors.accent else colors.border, shapes.button)
                                        .clickable { onSetSpeed(spd) }
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = "${spd}x",
                                        style = typography.caption,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) colors.background else colors.primaryText
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
