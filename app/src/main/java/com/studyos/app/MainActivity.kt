package com.studyos.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.studyos.app.core.model.AppState
import com.studyos.app.navigation.StudyOSApp
import com.studyos.app.theme.StudyOSTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContainer = (application as StudyOSApplication).container

        setContent {
            val appState by appContainer.preferencesDataSource.appState.collectAsState(
                initial = AppState(isLoading = true)
            )

            StudyOSTheme(appTheme = appState.theme) {
                if (!appState.isLoading) {
                    StudyOSApp(
                        container = appContainer,
                        isOnboardingCompleted = appState.isOnboardingCompleted,
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
}
