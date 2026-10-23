package com.studyos.app.domain.model

enum class LeverageQuadrant(val label: String, val description: String) {
    HIGH_IMPACT("High Impact", "High exam weight & low mastery. Immediate 95% target booster."),
    SECURE("Secure Foundation", "High exam weight & high mastery. Maintain to prevent leakage."),
    QUICK_WINS("Quick Wins", "Low exam weight & low mastery. Easy mark pickups."),
    OVERSTUDIED("Diminishing Returns", "Low exam weight & high mastery. Diminishing score yield.")
}

data class LeverageTopicItem(
    val topicId: String,
    val topicName: String,
    val chapterId: String,
    val chapterName: String,
    val subjectId: String,
    val subjectName: String,
    val examRelevance: ExamRelevance,
    val masteryPercentage: Int,
    val weaknessScore: Float,
    val leverageScore: Double,
    val estimatedHoursToMaster: Double,
    val marksYieldPotential: Double,
    val quadrant: LeverageQuadrant
)

data class LeverageMatrixReport(
    val items: List<LeverageTopicItem> = emptyList(),
    val highImpactCount: Int = 0,
    val secureCount: Int = 0,
    val quickWinsCount: Int = 0,
    val overstudiedCount: Int = 0,
    val topLeverageTopic: LeverageTopicItem? = null,
    val estimatedTotalMarksGainTo95: Double = 0.0,
    val strategicSummary: String = ""
)
