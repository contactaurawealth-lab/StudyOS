package com.studyos.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.studyos.app.core.model.AppState
import com.studyos.app.core.notification.StudyOSNotificationManager
import com.studyos.app.navigation.Screen
import com.studyos.app.navigation.StudyOSApp
import com.studyos.app.theme.StudyOSTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private val pendingRoute = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Create default notification channel
        StudyOSNotificationManager.createNotificationChannel(this)

        handleIncomingIntent(intent)

        val appContainer = (application as StudyOSApplication).container
        checkAutoStartTimer(intent, appContainer)

        lifecycleScope.launch {
            com.studyos.app.core.util.KeyboardShortcutManager.actions.collect { action ->
                when (action) {
                    com.studyos.app.core.util.KeyboardAction.NewNote -> {
                        pendingRoute.value = Screen.NoteEditor.createRoute()
                    }
                    com.studyos.app.core.util.KeyboardAction.OpenAiAssistant -> {
                        pendingRoute.value = Screen.Ai.createRoute()
                    }
                    else -> {}
                }
            }
        }

        setContent {
            val appState by appContainer.preferencesDataSource.appState.collectAsState(
                initial = AppState(isLoading = true)
            )

            StudyOSTheme(
                appTheme = appState.theme,
                customAccentHex = appState.customAccentHex
            ) {
                if (!appState.isLoading) {
                    StudyOSApp(
                        container = appContainer,
                        isOnboardingCompleted = appState.isOnboardingCompleted,
                        initialRoute = pendingRoute.value,
                        onRouteConsumed = { pendingRoute.value = null },
                        onExitApp = { finish() }
                    )
                } else {
                    // Blank quiet surface during initial datastore read to avoid flicker
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(StudyOSTheme.colors.background)
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val appContainer = (application as StudyOSApplication).container
        checkAutoStartTimer(intent, appContainer)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        extractRouteFromIntent(intent)?.let {
            pendingRoute.value = it
            return
        }

        if (intent.getBooleanExtra("OPEN_AI_CHAT", false)) {
            val convId = intent.getStringExtra("CONVERSATION_ID")
            pendingRoute.value = Screen.Ai.createRoute(conversationId = convId)
            return
        }

        // Handle shared document / file via ACTION_SEND or ACTION_VIEW
        val uri: android.net.Uri? = when (intent.action) {
            Intent.ACTION_SEND -> {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, android.net.Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_STREAM)
                }
            }
            Intent.ACTION_VIEW -> intent.data
            else -> null
        }

        if (uri != null) {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val stored = com.studyos.app.core.util.DocumentStorageManager.saveDocumentLocally(this@MainActivity, uri)
                    withContext(Dispatchers.Main) {
                        pendingRoute.value = Screen.DocumentViewer.createRoute(
                            documentUri = stored.persistentPath,
                            title = stored.cleanTitle
                        )
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        pendingRoute.value = Screen.DocumentViewer.createRoute(
                            documentUri = uri.toString(),
                            title = "Shared Document"
                        )
                    }
                }
            }
            return
        }

        // Handle shared plain text via ACTION_SEND
        if (intent.action == Intent.ACTION_SEND) {
            val text = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!text.isNullOrBlank()) {
                val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT) ?: "Shared Study Note"
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val stored = com.studyos.app.core.util.DocumentStorageManager.saveTextLocally(
                            this@MainActivity,
                            text,
                            subject
                        )
                        withContext(Dispatchers.Main) {
                            pendingRoute.value = Screen.DocumentViewer.createRoute(
                                documentUri = stored.persistentPath,
                                title = stored.cleanTitle
                            )
                        }
                    } catch (_: Exception) {}
                }
            }
        }
    }

    private fun checkAutoStartTimer(intent: Intent?, container: com.studyos.app.core.StudyOSAppContainer) {
        if (intent?.getBooleanExtra(EXTRA_AUTO_START_TIMER, false) == true) {
            val subjectId = intent.getStringExtra(EXTRA_SUBJECT_ID)
            if (!subjectId.isNullOrBlank()) {
                lifecycleScope.launch(Dispatchers.IO) {
                    val subject = container.subjectRepository.getSubjectByIdOnce(subjectId)
                    withContext(Dispatchers.Main) {
                        if (subject != null) {
                            container.studyTimerViewModel.selectSubject(subject)
                        }
                        container.studyTimerViewModel.startTimer()
                    }
                }
            } else {
                container.studyTimerViewModel.startTimer()
            }
        }
    }

    private fun extractRouteFromIntent(intent: Intent?): String? {
        if (intent == null) return null
        val directRoute = intent.getStringExtra(EXTRA_ROUTE) ?: intent.getStringExtra("route")
        if (!directRoute.isNullOrBlank()) return directRoute

        val chapterId = intent.getStringExtra(EXTRA_CHAPTER_ID) ?: intent.getStringExtra("chapterId")
        if (!chapterId.isNullOrBlank()) return Screen.ChapterDetail.createRoute(chapterId)

        val subjectId = intent.getStringExtra(EXTRA_SUBJECT_ID) ?: intent.getStringExtra("subjectId")
        if (!subjectId.isNullOrBlank()) return Screen.SubjectDetail.createRoute(subjectId)

        val sessionId = intent.getStringExtra(EXTRA_SESSION_ID) ?: intent.getStringExtra("sessionId")
        if (!sessionId.isNullOrBlank()) return Screen.StudySession.createRoute(sessionId)

        return null
    }

    override fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean {
        val handled = com.studyos.app.core.util.KeyboardShortcutManager.handleKeyEvent(event)
        if (handled) return true
        return super.dispatchKeyEvent(event)
    }

    companion object {
        const val EXTRA_ROUTE = "com.studyos.app.EXTRA_ROUTE"
        const val EXTRA_SUBJECT_ID = "com.studyos.app.EXTRA_SUBJECT_ID"
        const val EXTRA_CHAPTER_ID = "com.studyos.app.EXTRA_CHAPTER_ID"
        const val EXTRA_SESSION_ID = "com.studyos.app.EXTRA_SESSION_ID"
        const val EXTRA_AUTO_START_TIMER = "com.studyos.app.EXTRA_AUTO_START_TIMER"
    }
}
