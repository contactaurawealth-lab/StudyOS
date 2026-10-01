package com.studyos.app.core.workspace

import java.time.Instant

/**
 * Core models representing the Workspace Operating System (OS) Agent schema.
 */
data class WorkspaceSystemState(
    val activeMode: String = "focus", // focus | review | planning | triage
    val timestamp: String = Instant.now().toString()
)

data class VisualGeometry(
    val type: String = "svg_circle",
    val radius: Int = 45,
    val strokeWidth: Int = 6,
    val strokeDasharray: String = "282.74",
    val strokeDashoffset: String = "0.00",
    val colorActive: String = "#D39A3A",
    val colorTrail: String = "#181C27"
)

data class AlertProfile(
    val sound: String = "gentle_bell", // gentle_bell | digital_pulse | singing_bowl | chime_cascade
    val volume: Float = 0.8f,
    val frequencyHz: Int? = 528
)

data class NotificationHook(
    val trigger: String, // on_start | on_halfway | on_one_minute_warning | on_complete
    val message: String
)

data class WorkspaceTimerConfig(
    val enabled: Boolean = true,
    val title: String = "Deep Work Block",
    val durationSeconds: Int = 1500,
    val remainingSeconds: Int = 1500,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val visualGeometry: VisualGeometry = VisualGeometry(),
    val alertProfile: AlertProfile = AlertProfile(),
    val notificationHooks: List<NotificationHook> = emptyList(),
    val controls: List<String> = listOf("start", "pause", "resume", "reset", "add_5_mins", "toggle_chaining")
)

data class WorkspaceWidgetConfig(
    val id: String,
    val slot: Int,
    val title: String,
    val data: Map<String, Any?> = emptyMap()
)

data class DocumentRoutingConfig(
    val active: Boolean = false,
    val fileName: String? = null,
    val mimeType: String? = null,
    val targetViewer: String? = null,
    val desktopAlternatives: List<String> = emptyList(),
    val availableActions: List<String> = emptyList()
)

data class WorkspaceOsPayload(
    val systemState: WorkspaceSystemState = WorkspaceSystemState(),
    val timer: WorkspaceTimerConfig = WorkspaceTimerConfig(),
    val activeWidgets: List<WorkspaceWidgetConfig> = emptyList(),
    val documentRouting: DocumentRoutingConfig = DocumentRoutingConfig()
)

enum class WorkspacePreset(val displayName: String, val description: String) {
    ACADEMIC_STUDY(
        "Academic Study & Exam Prep",
        "Optimized for high-yield recall, spaced repetition, concept maps, and document review."
    ),
    DEEP_WORK(
        "Deep Work & Coding",
        "Focused layout with active timer, priority task queue, rapid scratchpad, and ambient sound."
    ),
    PROJECT_MANAGEMENT(
        "Project & Team Management",
        "Structured triage layout with Eisenhower matrix, chronological agenda, and habit trackers."
    ),
    CREATIVE_WRITING(
        "Creative Writing & Brainstorming",
        "Distraction-free environment with ambient soundscapes, concept graphs, and document synthesis."
    )
}
