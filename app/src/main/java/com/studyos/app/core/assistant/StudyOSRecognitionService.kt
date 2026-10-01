package com.studyos.app.core.assistant

import android.content.Intent
import android.speech.RecognitionService

class StudyOSRecognitionService : RecognitionService() {

    override fun onStartListening(recognizerIntent: Intent?, listener: Callback?) {
        // Recognition requests within the app use standard SpeechRecognizer instances.
        // This service satisfies the system voice-interaction recognition contract.
    }

    override fun onStopListening(listener: Callback?) {
    }

    override fun onCancel(listener: Callback?) {
    }
}
