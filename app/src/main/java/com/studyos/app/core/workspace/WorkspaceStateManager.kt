package com.studyos.app.core.workspace

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import kotlin.math.PI
import kotlin.math.roundToInt

/**
 * Central state engine for the Workspace Operating System (OS).
 * Synchronizes the 4 core subsystems: Time Management, 12-Widget Synthesizer,
 * Document Routing Bridge, and the JSON State Protocol.
 */
object WorkspaceStateManager {

    private val _state = MutableStateFlow(createPresetPayload(WorkspacePreset.ACADEMIC_STUDY))
    val state: StateFlow<WorkspaceOsPayload> = _state.asStateFlow()

    fun setPayload(payload: WorkspaceOsPayload) {
        _state.value = payload
    }

    fun applyPreset(preset: WorkspacePreset) {
        _state.value = createPresetPayload(preset)
    }

    /**
     * Calculates the SVG circle stroke-dashoffset for a given radius and duration.
     * Circumference = 2 * PI * 45 ≈ 282.743
     */
    fun calculateCircleOffset(remainingSeconds: Int, totalSeconds: Int, radius: Int = 45): String {
        if (totalSeconds <= 0) return "0.00"
        val circumference = 2 * PI * radius
        val progress = (totalSeconds - remainingSeconds).toDouble() / totalSeconds.toDouble()
        val clampedProgress = progress.coerceIn(0.0, 1.0)
        val offset = circumference * clampedProgress
        return String.format(java.util.Locale.US, "%.2f", offset)
    }

    /**
     * Routes an incoming document to its primary in-app viewer, desktop fallbacks, and instant AI actions.
     */
    fun routeDocument(fileName: String, mimeType: String? = null): DocumentRoutingConfig {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return when {
            extension == "pdf" || mimeType == "application/pdf" -> {
                DocumentRoutingConfig(
                    active = true,
                    fileName = fileName,
                    mimeType = "application/pdf",
                    targetViewer = "pdf_embed_pane",
                    desktopAlternatives = listOf("Apple Preview", "Adobe Acrobat", "Foxit Reader"),
                    availableActions = listOf("extract_summary", "generate_qa_cards", "highlight_key_takeaways")
                )
            }
            extension in listOf("md", "txt", "rtf") || mimeType?.startsWith("text/") == true -> {
                DocumentRoutingConfig(
                    active = true,
                    fileName = fileName,
                    mimeType = "text/markdown",
                    targetViewer = "monaco_markdown_editor",
                    desktopAlternatives = listOf("Obsidian", "VS Code", "Typora"),
                    availableActions = listOf("format_to_outline", "extract_action_items", "synthesize_to_scratchpad")
                )
            }
            extension in listOf("docx", "pptx", "xlsx", "csv") -> {
                DocumentRoutingConfig(
                    active = true,
                    fileName = fileName,
                    mimeType = "application/vnd.openxmlformats-officedocument",
                    targetViewer = "office_grid_embed_viewer",
                    desktopAlternatives = listOf("Microsoft 365", "LibreOffice", "Apple Keynote/Numbers"),
                    availableActions = listOf("parse_table_data", "summarize_slides", "audit_formulas")
                )
            }
            extension in listOf("png", "jpg", "jpeg", "webp", "svg") || mimeType?.startsWith("image/") == true -> {
                DocumentRoutingConfig(
                    active = true,
                    fileName = fileName,
                    mimeType = "image/png",
                    targetViewer = "lightbox_ocr_viewer",
                    desktopAlternatives = listOf("OS Default Image Viewer"),
                    availableActions = listOf("run_ocr_to_markdown", "explain_diagram", "isolate_handwriting")
                )
            }
            extension in listOf("py", "js", "ts", "html", "css", "json", "sql", "kt") -> {
                DocumentRoutingConfig(
                    active = true,
                    fileName = fileName,
                    mimeType = "text/plain",
                    targetViewer = "monaco_code_viewer",
                    desktopAlternatives = listOf("VS Code", "Cursor", "Android Studio"),
                    availableActions = listOf("explain_code", "find_bugs", "refactor_snippet")
                )
            }
            else -> {
                DocumentRoutingConfig(
                    active = true,
                    fileName = fileName,
                    mimeType = mimeType ?: "application/octet-stream",
                    targetViewer = "universal_preview_pane",
                    desktopAlternatives = listOf("System Default"),
                    availableActions = listOf("extract_summary", "view_metadata")
                )
            }
        }
    }

    /**
     * Instantiates preset layouts matching the 12-Widget library and layout matrices.
     */
    fun createPresetPayload(preset: WorkspacePreset): WorkspaceOsPayload {
        return when (preset) {
            WorkspacePreset.ACADEMIC_STUDY -> {
                WorkspaceOsPayload(
                    systemState = WorkspaceSystemState(activeMode = "focus", timestamp = Instant.now().toString()),
                    timer = WorkspaceTimerConfig(
                        enabled = true,
                        title = "Socratic Exam Prep Block",
                        durationSeconds = 1500,
                        remainingSeconds = 1500,
                        visualGeometry = VisualGeometry(colorActive = "#D39A3A", strokeDashoffset = "0.00"),
                        alertProfile = AlertProfile(sound = "gentle_bell", frequencyHz = 528),
                        notificationHooks = listOf(
                            NotificationHook("on_start", "Session started: 25 minutes of focused recall."),
                            NotificationHook("on_halfway", "Halfway checkpoint reached (750s). Stay focused."),
                            NotificationHook("on_one_minute_warning", "60 seconds remaining. Wrap up this flashcard."),
                            NotificationHook("on_complete", "Exam prep block complete! Transitioning to consolidation.")
                        )
                    ),
                    activeWidgets = listOf(
                        WorkspaceWidgetConfig(
                            id = "widget_focus_timer",
                            slot = 1,
                            title = "Focus Ring Timer",
                            data = mapOf("linked_to_timer" to true, "mode" to "focus")
                        ),
                        WorkspaceWidgetConfig(
                            id = "widget_doc_viewer",
                            slot = 2,
                            title = "Universal Document Viewer",
                            data = mapOf("file" to "Organic_Chemistry_Synthesis.pdf", "page" to 42)
                        ),
                        WorkspaceWidgetConfig(
                            id = "widget_metric_counter",
                            slot = 3,
                            title = "Exam Readiness Targets",
                            data = mapOf("cards_reviewed" to 28, "readiness_score" to 84)
                        ),
                        WorkspaceWidgetConfig(
                            id = "widget_flashcards",
                            slot = 4,
                            title = "Dynamic Flashcard Deck",
                            data = mapOf("deck" to "High-Yield Reactions", "card_count" to 50)
                        ),
                        WorkspaceWidgetConfig(
                            id = "widget_concept_graph",
                            slot = 5,
                            title = "Concept Graph & Outline",
                            data = mapOf("root" to "Carbonyl Chemistry", "nodes_count" to 8)
                        )
                    ),
                    documentRouting = routeDocument("Organic_Chemistry_Synthesis.pdf", "application/pdf")
                )
            }
            WorkspacePreset.DEEP_WORK -> {
                WorkspaceOsPayload(
                    systemState = WorkspaceSystemState(activeMode = "focus", timestamp = Instant.now().toString()),
                    timer = WorkspaceTimerConfig(
                        enabled = true,
                        title = "Deep Work & Architecture Sprint",
                        durationSeconds = 3000,
                        remainingSeconds = 3000,
                        visualGeometry = VisualGeometry(colorActive = "#3B82F6", strokeDashoffset = "0.00"),
                        alertProfile = AlertProfile(sound = "digital_pulse", frequencyHz = 880),
                        notificationHooks = listOf(
                            NotificationHook("on_start", "50-minute deep sprint started."),
                            NotificationHook("on_complete", "Sprint complete. Disconnect and rest.")
                        )
                    ),
                    activeWidgets = listOf(
                        WorkspaceWidgetConfig("widget_focus_timer", 1, "Focus Ring Timer", mapOf("linked_to_timer" to true)),
                        WorkspaceWidgetConfig("widget_task_list", 2, "Smart Task Checklist", mapOf("tasks_count" to 4)),
                        WorkspaceWidgetConfig("widget_scratchpad", 3, "Rapid Scratchpad", mapOf("auto_save" to true)),
                        WorkspaceWidgetConfig("widget_doc_viewer", 4, "Source Code Viewer", mapOf("file" to "WorkspaceStateManager.kt")),
                        WorkspaceWidgetConfig("widget_sound_player", 5, "Ambient Soundscape", mapOf("track" to "40Hz Gamma Binaural"))
                    ),
                    documentRouting = routeDocument("WorkspaceStateManager.kt", "text/plain")
                )
            }
            WorkspacePreset.PROJECT_MANAGEMENT -> {
                WorkspaceOsPayload(
                    systemState = WorkspaceSystemState(activeMode = "planning", timestamp = Instant.now().toString()),
                    timer = WorkspaceTimerConfig(
                        enabled = true,
                        title = "Sprint Planning & Triage",
                        durationSeconds = 1800,
                        remainingSeconds = 1800,
                        visualGeometry = VisualGeometry(colorActive = "#10B981", strokeDashoffset = "0.00"),
                        alertProfile = AlertProfile(sound = "chime_cascade", frequencyHz = 659)
                    ),
                    activeWidgets = listOf(
                        WorkspaceWidgetConfig("widget_task_list", 1, "Sprint Tasks", emptyMap()),
                        WorkspaceWidgetConfig("widget_agenda", 2, "Chronological Agenda", emptyMap()),
                        WorkspaceWidgetConfig("widget_habit_tracker", 3, "Team Habits & Streaks", emptyMap()),
                        WorkspaceWidgetConfig("widget_eisenhower_matrix", 4, "Eisenhower Priority Matrix", emptyMap()),
                        WorkspaceWidgetConfig("widget_metric_counter", 5, "Velocity & Metric Counter", emptyMap())
                    )
                )
            }
            WorkspacePreset.CREATIVE_WRITING -> {
                WorkspaceOsPayload(
                    systemState = WorkspaceSystemState(activeMode = "triage", timestamp = Instant.now().toString()),
                    timer = WorkspaceTimerConfig(
                        enabled = true,
                        title = "Creative Synthesis Flow",
                        durationSeconds = 2700,
                        remainingSeconds = 2700,
                        visualGeometry = VisualGeometry(colorActive = "#8B5CF6", strokeDashoffset = "0.00"),
                        alertProfile = AlertProfile(sound = "singing_bowl", frequencyHz = 432)
                    ),
                    activeWidgets = listOf(
                        WorkspaceWidgetConfig("widget_focus_timer", 1, "Focus Ring Timer", emptyMap()),
                        WorkspaceWidgetConfig("widget_scratchpad", 2, "Rapid Scratchpad", emptyMap()),
                        WorkspaceWidgetConfig("widget_doc_viewer", 3, "Manuscript Viewer", emptyMap()),
                        WorkspaceWidgetConfig("widget_sound_player", 4, "Ambient Rainfall", emptyMap()),
                        WorkspaceWidgetConfig("widget_concept_graph", 5, "Story Node Graph", emptyMap())
                    )
                )
            }
        }
    }

    /**
     * Serializes a WorkspaceOsPayload into the exact JSON specification defined in Section 4.
     */
    fun toJson(payload: WorkspaceOsPayload): String {
        val root = JSONObject()

        val sysStateObj = JSONObject().apply {
            put("active_mode", payload.systemState.activeMode)
            put("timestamp", payload.systemState.timestamp)
        }
        root.put("system_state", sysStateObj)

        val timerObj = JSONObject().apply {
            put("enabled", payload.timer.enabled)
            put("title", payload.timer.title)
            put("duration_seconds", payload.timer.durationSeconds)

            val geomObj = JSONObject().apply {
                put("type", payload.timer.visualGeometry.type)
                put("radius", payload.timer.visualGeometry.radius)
                put("stroke_width", payload.timer.visualGeometry.strokeWidth)
                put("stroke_dasharray", payload.timer.visualGeometry.strokeDasharray)
                put("stroke_dashoffset", payload.timer.visualGeometry.strokeDashoffset)
                put("color_active", payload.timer.visualGeometry.colorActive)
                put("color_trail", payload.timer.visualGeometry.colorTrail)
            }
            put("visual_geometry", geomObj)

            val alertObj = JSONObject().apply {
                put("sound", payload.timer.alertProfile.sound)
                put("volume", payload.timer.alertProfile.volume.toDouble())
                payload.timer.alertProfile.frequencyHz?.let { put("frequency_hz", it) }
            }
            put("alert_profile", alertObj)

            val hooksArray = JSONArray()
            payload.timer.notificationHooks.forEach { hook ->
                hooksArray.put(JSONObject().apply {
                    put("trigger", hook.trigger)
                    put("message", hook.message)
                })
            }
            put("notification_hooks", hooksArray)

            val controlsArray = JSONArray()
            payload.timer.controls.forEach { controlsArray.put(it) }
            put("controls", controlsArray)
        }
        root.put("timer", timerObj)

        val widgetsArray = JSONArray()
        payload.activeWidgets.forEach { widget ->
            val wObj = JSONObject().apply {
                put("id", widget.id)
                put("slot", widget.slot)
                put("title", widget.title)
                val dataObj = JSONObject()
                widget.data.forEach { (k, v) -> dataObj.put(k, v) }
                put("data", dataObj)
            }
            widgetsArray.put(wObj)
        }
        root.put("active_widgets", widgetsArray)

        val docObj = JSONObject().apply {
            put("active", payload.documentRouting.active)
            put("file_name", payload.documentRouting.fileName ?: JSONObject.NULL)
            put("mime_type", payload.documentRouting.mimeType ?: JSONObject.NULL)
            put("target_viewer", payload.documentRouting.targetViewer ?: JSONObject.NULL)

            val altArray = JSONArray()
            payload.documentRouting.desktopAlternatives.forEach { altArray.put(it) }
            put("desktop_alternatives", altArray)

            val actionsArray = JSONArray()
            payload.documentRouting.availableActions.forEach { actionsArray.put(it) }
            put("available_actions", actionsArray)
        }
        root.put("document_routing", docObj)

        return root.toString(2)
    }

    /**
     * Parses a JSON string following the Section 4 schema into a WorkspaceOsPayload.
     */
    fun fromJson(jsonStr: String): WorkspaceOsPayload {
        val root = JSONObject(jsonStr)

        val sysObj = root.optJSONObject("system_state")
        val sysState = WorkspaceSystemState(
            activeMode = sysObj?.optString("active_mode", "focus") ?: "focus",
            timestamp = sysObj?.optString("timestamp", Instant.now().toString()) ?: Instant.now().toString()
        )

        val timerObj = root.optJSONObject("timer")
        val geomObj = timerObj?.optJSONObject("visual_geometry")
        val visualGeometry = VisualGeometry(
            type = geomObj?.optString("type", "svg_circle") ?: "svg_circle",
            radius = geomObj?.optInt("radius", 45) ?: 45,
            strokeWidth = geomObj?.optInt("stroke_width", 6) ?: 6,
            strokeDasharray = geomObj?.optString("stroke_dasharray", "282.74") ?: "282.74",
            strokeDashoffset = geomObj?.optString("stroke_dashoffset", "0.00") ?: "0.00",
            colorActive = geomObj?.optString("color_active", "#D39A3A") ?: "#D39A3A",
            colorTrail = geomObj?.optString("color_trail", "#181C27") ?: "#181C27"
        )

        val alertObj = timerObj?.optJSONObject("alert_profile")
        val alertProfile = AlertProfile(
            sound = alertObj?.optString("sound", "gentle_bell") ?: "gentle_bell",
            volume = alertObj?.optDouble("volume", 0.8)?.toFloat() ?: 0.8f,
            frequencyHz = if (alertObj?.has("frequency_hz") == true) alertObj.optInt("frequency_hz") else null
        )

        val hooksList = mutableListOf<NotificationHook>()
        val hooksArray = timerObj?.optJSONArray("notification_hooks")
        if (hooksArray != null) {
            for (i in 0 until hooksArray.length()) {
                val h = hooksArray.getJSONObject(i)
                hooksList.add(
                    NotificationHook(
                        trigger = h.optString("trigger", "on_complete"),
                        message = h.optString("message", "")
                    )
                )
            }
        }

        val controlsList = mutableListOf<String>()
        val controlsArray = timerObj?.optJSONArray("controls")
        if (controlsArray != null) {
            for (i in 0 until controlsArray.length()) {
                controlsList.add(controlsArray.getString(i))
            }
        } else {
            controlsList.addAll(listOf("start", "pause", "resume", "reset", "add_5_mins", "toggle_chaining"))
        }

        val timerConfig = WorkspaceTimerConfig(
            enabled = timerObj?.optBoolean("enabled", true) ?: true,
            title = timerObj?.optString("title", "Focus Session") ?: "Focus Session",
            durationSeconds = timerObj?.optInt("duration_seconds", 1500) ?: 1500,
            remainingSeconds = timerObj?.optInt("remaining_seconds", 1500) ?: 1500,
            visualGeometry = visualGeometry,
            alertProfile = alertProfile,
            notificationHooks = hooksList,
            controls = controlsList
        )

        val widgetsList = mutableListOf<WorkspaceWidgetConfig>()
        val widgetsArray = root.optJSONArray("active_widgets")
        if (widgetsArray != null) {
            for (i in 0 until widgetsArray.length()) {
                val w = widgetsArray.getJSONObject(i)
                val id = w.optString("id", "")
                val slot = w.optInt("slot", i + 1)
                val title = w.optString("title", "")
                val dataMap = mutableMapOf<String, Any?>()
                val dataObj = w.optJSONObject("data")
                if (dataObj != null) {
                    val keys = dataObj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        dataMap[key] = dataObj.opt(key)
                    }
                }
                widgetsList.add(WorkspaceWidgetConfig(id = id, slot = slot, title = title, data = dataMap))
            }
        }

        val docObj = root.optJSONObject("document_routing")
        val altList = mutableListOf<String>()
        val actionsList = mutableListOf<String>()
        docObj?.optJSONArray("desktop_alternatives")?.let {
            for (i in 0 until it.length()) altList.add(it.getString(i))
        }
        docObj?.optJSONArray("available_actions")?.let {
            for (i in 0 until it.length()) actionsList.add(it.getString(i))
        }

        val docConfig = DocumentRoutingConfig(
            active = docObj?.optBoolean("active", false) ?: false,
            fileName = if (docObj?.has("file_name") == true && !docObj.isNull("file_name")) docObj.optString("file_name") else null,
            mimeType = if (docObj?.has("mime_type") == true && !docObj.isNull("mime_type")) docObj.optString("mime_type") else null,
            targetViewer = if (docObj?.has("target_viewer") == true && !docObj.isNull("target_viewer")) docObj.optString("target_viewer") else null,
            desktopAlternatives = altList,
            availableActions = actionsList
        )

        return WorkspaceOsPayload(
            systemState = sysState,
            timer = timerConfig,
            activeWidgets = widgetsList,
            documentRouting = docConfig
        )
    }
}
