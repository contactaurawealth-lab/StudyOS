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

        extractRouteFromIntent(intent)?.let {
            pendingRoute.value = it
        }

        val appContainer = (application as StudyOSApplication).container
        checkAutoStartTimer(intent, appContainer)

        setContent {
            val appState by appContainer.preferencesDataSource.appState.collectAsState(
                initial = AppState(isLoading = true)
            )

            StudyOSTheme(appTheme = appState.theme) {
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
        extractRouteFromIntent(intent)?.let {
            pendingRoute.value = it
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

    companion object {
        const val EXTRA_ROUTE = "com.studyos.app.EXTRA_ROUTE"
        const val EXTRA_SUBJECT_ID = "com.studyos.app.EXTRA_SUBJECT_ID"
        const val EXTRA_CHAPTER_ID = "com.studyos.app.EXTRA_CHAPTER_ID"
        const val EXTRA_SESSION_ID = "com.studyos.app.EXTRA_SESSION_ID"
        const val EXTRA_AUTO_START_TIMER = "com.studyos.app.EXTRA_AUTO_START_TIMER"
    }
}
