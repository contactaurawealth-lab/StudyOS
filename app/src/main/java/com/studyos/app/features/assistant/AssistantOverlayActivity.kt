package com.studyos.app.features.assistant

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.studyos.app.MainActivity
import com.studyos.app.StudyOSApplication
import com.studyos.app.core.assistant.StudyOSAssistantManager
import com.studyos.app.features.assistant.ui.AssistantOverlaySheet
import com.studyos.app.theme.StudyOSTheme

class AssistantOverlayActivity : ComponentActivity() {

    private lateinit var viewModel: AssistantOverlayViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as StudyOSApplication
        val container = app.container

        viewModel = ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AssistantOverlayViewModel(
                        context = applicationContext,
                        preferencesDataSource = container.preferencesDataSource,
                        createConversationUseCase = container.createConversationUseCase,
                        sendAiMessageUseCase = container.sendAiMessageUseCase,
                        noteDao = container.database.noteDao(),
                        flashcardDao = container.database.flashcardDao(),
                        subjectRepository = container.subjectRepository
                    ) as T
                }
            }
        )[AssistantOverlayViewModel::class.java]

        setContent {
            StudyOSTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.52f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            finish()
                        },
                    contentAlignment = Alignment.BottomCenter
                ) {
                    AssistantOverlaySheet(
                        viewModel = viewModel,
                        onDismiss = { finish() },
                        onOpenFullChat = { conversationId ->
                            openFullChatInApp(conversationId)
                        },
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            // Catch clicks on the sheet so it doesn't dismiss the scrim
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun openFullChatInApp(conversationId: String?) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("OPEN_AI_CHAT", true)
            if (!conversationId.isNullOrBlank()) {
                putExtra("CONVERSATION_ID", conversationId)
            }
        }
        startActivity(intent)
        finish()
    }

    override fun finish() {
        super.finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, android.R.anim.fade_out)
    }

    override fun onDestroy() {
        super.onDestroy()
        StudyOSAssistantManager.notifyOverlayClosed()
    }
}
