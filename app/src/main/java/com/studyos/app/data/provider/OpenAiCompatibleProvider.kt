package com.studyos.app.data.provider

import com.studyos.app.domain.model.AiConfig
import com.studyos.app.domain.model.AiErrorType
import com.studyos.app.domain.model.AiMessage
import com.studyos.app.domain.model.AiMessageRole
import com.studyos.app.domain.model.AiMessageStatus
import com.studyos.app.domain.model.AiStreamChunk
import com.studyos.app.domain.model.StudyContext
import com.studyos.app.domain.provider.AiProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException

class OpenAiCompatibleProvider : AiProvider {

    override fun generateStream(
        messages: List<AiMessage>,
        context: StudyContext?,
        config: AiConfig
    ): Flow<AiStreamChunk> = flow {
        if (!config.isConfigured) {
            emit(
                AiStreamChunk.Error(
                    AiErrorType.API_KEY_MISSING,
                    "API key is not configured. Please set your API key in AI Settings to chat with the assistant."
                )
            )
            return@flow
        }

        val endpoint = if (config.baseUrl.endsWith("/")) {
            "${config.baseUrl}chat/completions"
        } else {
            "${config.baseUrl}/chat/completions"
        }

        var connection: HttpURLConnection? = null
        var reader: BufferedReader? = null

        try {
            val url = URL(endpoint)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 45_000
                doOutput = true
                doInput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "text/event-stream")
                setRequestProperty("Authorization", "Bearer ${config.apiKey.trim()}")
            }

            // Build payload
            val rootJson = JSONObject()
            val modelName = config.model.trim().ifBlank { "gpt-4o-mini" }
            rootJson.put("model", modelName)
            rootJson.put("stream", true)
            rootJson.put("temperature", 0.7)

            val messagesJsonArray = JSONArray()

            // 1. System prompt
            val baseSystemPrompt = buildString {
                append("You are the StudyOS AI Study Assistant, a calm, precise, and encouraging academic tutor.")
                append("\nYour goal is to help the student deeply understand concepts, prepare for exams, and master their coursework.")
                append("\n\nPedagogical guidelines:")
                append("\n- Be clear, structured, and helpful. Avoid robotic fluff or generic praise.")
                append("\n- Break down complex topics into clear steps using first principles.")
                append("\n- Format your responses using clean GitHub Markdown (headings, bullet points, bold key terms).")
                append("\n- For formulas and equations, format them clearly using standard notation.")
                append("\n- For code, always use fenced code blocks with the language specified.")
                append("\n- If the student asks for a quiz, present 3-4 distinct questions, then provide answers with explanations.")
                append("\n- If the student asks for notes, provide concise, high-yield revision notes.")
                if (context?.hasContext == true) {
                    append("\n\n${context.toPromptContext()}")
                }
                if (!config.customSystemPrompt.isNullOrBlank()) {
                    append("\n\nCustom Instructions:\n${config.customSystemPrompt.trim()}")
                }
            }

            val systemMsg = JSONObject().apply {
                put("role", "system")
                put("content", baseSystemPrompt)
            }
            messagesJsonArray.put(systemMsg)

            // 2. Chat history
            messages.forEach { msg ->
                if (msg.content.isNotBlank() && msg.status != AiMessageStatus.ERROR) {
                    val roleStr = when (msg.role) {
                        AiMessageRole.USER -> "user"
                        AiMessageRole.ASSISTANT -> "assistant"
                        AiMessageRole.SYSTEM -> "system"
                    }
                    val msgObj = JSONObject().apply {
                        put("role", roleStr)
                        put("content", msg.content.trim())
                    }
                    messagesJsonArray.put(msgObj)
                }
            }

            rootJson.put("messages", messagesJsonArray)

            // Write body
            OutputStreamWriter(connection.outputStream, "UTF-8").use { writer ->
                writer.write(rootJson.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode

            if (responseCode == 401 || responseCode == 403) {
                emit(
                    AiStreamChunk.Error(
                        AiErrorType.API_KEY_MISSING,
                        "Invalid or unauthorized API key (HTTP $responseCode). Please verify your API key in AI Settings."
                    )
                )
                return@flow
            }

            if (responseCode == 429) {
                emit(
                    AiStreamChunk.Error(
                        AiErrorType.RATE_LIMIT,
                        "Rate limit reached (HTTP 429). Please wait a moment before sending another message."
                    )
                )
                return@flow
            }

            if (responseCode !in 200..299) {
                val errorBody = try {
                    connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                } catch (e: Exception) {
                    ""
                }
                val parsedMessage = try {
                    val errJson = JSONObject(errorBody)
                    errJson.optJSONObject("error")?.optString("message") ?: errorBody
                } catch (e: Exception) {
                    errorBody.ifBlank { "API request failed with HTTP $responseCode." }
                }

                emit(
                    AiStreamChunk.Error(
                        AiErrorType.SERVER_ERROR,
                        "API request failed (HTTP $responseCode): $parsedMessage"
                    )
                )
                return@flow
            }

            // Stream response
            reader = BufferedReader(InputStreamReader(connection.inputStream, "UTF-8"))
            var hasReceivedContent = false
            var line: String?
            val sseDataBuffer = StringBuilder()

            while (reader.readLine().also { line = it } != null) {
                val currentLine = line ?: continue
                val trimmedLine = currentLine.trim()
                if (trimmedLine.isEmpty() || trimmedLine.startsWith(":")) {
                    continue // SSE comment or empty keep-alive line
                }

                if (trimmedLine.startsWith("data:")) {
                    val rawData = trimmedLine.removePrefix("data:").trim()
                    if (rawData == "[DONE]") {
                        break
                    }
                    sseDataBuffer.append(rawData)
                    val jsonStr = sseDataBuffer.toString()
                    val deltaContent = extractDeltaContent(jsonStr)
                    if (!deltaContent.isNullOrEmpty()) {
                        hasReceivedContent = true
                        emit(AiStreamChunk.Content(deltaContent))
                        sseDataBuffer.clear()
                    } else if (jsonStr.startsWith("{") && jsonStr.endsWith("}")) {
                        // Complete JSON was parsed but produced no text, clear buffer
                        sseDataBuffer.clear()
                    }
                } else if (trimmedLine.startsWith("{") && !hasReceivedContent) {
                    val fallbackContent = extractDeltaContent(trimmedLine)
                    if (!fallbackContent.isNullOrEmpty()) {
                        hasReceivedContent = true
                        emit(AiStreamChunk.Content(fallbackContent))
                    }
                }
            }

            if (!hasReceivedContent) {
                emit(
                    AiStreamChunk.Error(
                        AiErrorType.EMPTY_RESPONSE,
                        "Received an empty response from the AI provider."
                    )
                )
            } else {
                emit(AiStreamChunk.Done)
            }

        } catch (e: UnknownHostException) {
            emit(
                AiStreamChunk.Error(
                    AiErrorType.NO_INTERNET,
                    "No internet connection. Please check your Wi-Fi or mobile data and try again."
                )
            )
        } catch (e: ConnectException) {
            emit(
                AiStreamChunk.Error(
                    AiErrorType.NO_INTERNET,
                    "Could not connect to the AI endpoint. Please verify your connection or Base URL."
                )
            )
        } catch (e: SocketTimeoutException) {
            emit(
                AiStreamChunk.Error(
                    AiErrorType.TIMEOUT,
                    "The connection to the AI provider timed out. Please try again."
                )
            )
        } catch (e: java.io.IOException) {
            emit(
                AiStreamChunk.Error(
                    AiErrorType.NO_INTERNET,
                    "Network error: ${e.localizedMessage ?: "Unable to access the internet. Please check your network connection."}"
                )
            )
        } catch (e: CancellationException) {
            // User requested stop generation - rethrow without emitting to respect cancellation
            throw e
        } catch (e: Exception) {
            emit(
                AiStreamChunk.Error(
                    AiErrorType.UNKNOWN,
                    e.message ?: "An unexpected error occurred while communicating with the AI."
                )
            )
        } finally {
            try {
                reader?.close()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun extractDeltaContent(jsonString: String): String? {
        return try {
            val json = JSONObject(jsonString)

            // 1. OpenAI format choices
            val choices = json.optJSONArray("choices")
            if (choices != null && choices.length() > 0) {
                val choice = choices.getJSONObject(0)
                val delta = choice.optJSONObject("delta")
                if (delta != null) {
                    if (delta.has("content") && !delta.isNull("content")) {
                        return delta.getString("content")
                    }
                    if (delta.has("reasoning_content") && !delta.isNull("reasoning_content")) {
                        return delta.getString("reasoning_content")
                    }
                }
                val message = choice.optJSONObject("message")
                if (message != null && message.has("content") && !message.isNull("content")) {
                    return message.getString("content")
                }
                val text = choice.optString("text")
                if (text.isNotBlank()) {
                    return text
                }
            }

            // 2. Gemini native candidates format
            val candidates = json.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val candidate = candidates.getJSONObject(0)
                val contentObj = candidate.optJSONObject("content")
                val parts = contentObj?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    val partText = parts.getJSONObject(0).optString("text")
                    if (partText.isNotBlank()) {
                        return partText
                    }
                }
            }

            null
        } catch (e: Exception) {
            null
        }
    }
}
