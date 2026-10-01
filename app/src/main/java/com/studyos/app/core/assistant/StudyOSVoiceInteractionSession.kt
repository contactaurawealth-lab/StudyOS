package com.studyos.app.core.assistant

import android.app.assist.AssistContent
import android.app.assist.AssistStructure
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import com.studyos.app.features.assistant.AssistantOverlayActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StudyOSVoiceInteractionSession(context: Context) : VoiceInteractionSession(context) {

    private val sessionScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        StudyOSAssistantManager.registerSessionFinisher {
            finish()
        }
    }

    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)

        // Launch the floating translucent assistant overlay activity
        val overlayIntent = Intent(context, AssistantOverlayActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtras(args ?: Bundle())
        }
        startAssistantActivity(overlayIntent)
    }

    @Suppress("DEPRECATION")
    override fun onHandleAssist(
        data: Bundle?,
        structure: AssistStructure?,
        content: AssistContent?
    ) {
        super.onHandleAssist(data, structure, content)

        if (structure == null) return

        // Dispatch AssistStructure traversal to Dispatchers.Default (off main thread)
        sessionScope.launch {
            val extractedText = withContext(Dispatchers.Default) {
                parseAssistStructure(structure)
            }

            if (extractedText.isNotBlank()) {
                val assistContext = AssistScreenContext(
                    textContent = extractedText,
                    sourcePackage = structure.activityComponent?.packageName,
                    timestamp = System.currentTimeMillis()
                )
                StudyOSAssistantManager.updateAssistContext(assistContext)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        StudyOSAssistantManager.unregisterSessionFinisher()
        sessionScope.cancel()
    }

    /**
     * Traverses AssistStructure.ViewNode safely on Dispatchers.Default with depth and size caps.
     */
    private fun parseAssistStructure(structure: AssistStructure): String {
        val buffer = StringBuilder()
        val maxChars = 8000
        val maxDepth = 20

        try {
            val windowCount = structure.windowNodeCount
            for (w in 0 until windowCount) {
                val windowNode = structure.getWindowNodeAt(w)
                val rootNode = windowNode.rootViewNode ?: continue
                traverseNode(rootNode, buffer, 0, maxDepth, maxChars)
                if (buffer.length >= maxChars) break
            }
        } catch (e: Exception) {
            // Safe fallback if structure traversal is interrupted
        }

        return buffer.toString().trim()
    }

    private fun traverseNode(
        node: AssistStructure.ViewNode,
        buffer: StringBuilder,
        currentDepth: Int,
        maxDepth: Int,
        maxChars: Int
    ) {
        if (currentDepth > maxDepth || buffer.length >= maxChars) return

        val text = node.text?.toString()?.trim()
        val desc = node.contentDescription?.toString()?.trim()

        if (!text.isNullOrBlank()) {
            if (buffer.isNotEmpty() && !buffer.endsWith("\n")) {
                buffer.append("\n")
            }
            buffer.append(text)
        } else if (!desc.isNullOrBlank()) {
            if (buffer.isNotEmpty() && !buffer.endsWith("\n")) {
                buffer.append("\n")
            }
            buffer.append(desc)
        }

        val childCount = node.childCount
        for (i in 0 until childCount) {
            if (buffer.length >= maxChars) break
            val child = node.getChildAt(i) ?: continue
            traverseNode(child, buffer, currentDepth + 1, maxDepth, maxChars)
        }
    }
}
