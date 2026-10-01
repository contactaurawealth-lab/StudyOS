package com.studyos.app.core.assistant

import android.os.Bundle
import android.service.voice.VoiceInteractionService
import android.speech.SpeechRecognizer
import com.studyos.app.StudyOSApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class StudyOSVoiceInteractionService : VoiceInteractionService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onReady() {
        super.onReady()

        // Warm-start optimization: pre-warm AI config and speech recognition availability
        serviceScope.launch {
            try {
                val app = applicationContext as? StudyOSApplication
                app?.container?.preferencesDataSource?.aiConfig?.firstOrNull()
                // Warm speech engine availability check
                SpeechRecognizer.isRecognitionAvailable(applicationContext)
            } catch (ignored: Exception) {
            }
        }
    }

    override fun onShutdown() {
        super.onShutdown()
        StudyOSAssistantManager.notifyOverlayClosed()
    }
}
