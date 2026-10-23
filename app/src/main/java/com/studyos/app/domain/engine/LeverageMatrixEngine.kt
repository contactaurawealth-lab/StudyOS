package com.studyos.app.domain.engine

import com.studyos.app.domain.model.Chapter
import com.studyos.app.domain.model.ExamRelevance
import com.studyos.app.domain.model.LeverageMatrixReport
import com.studyos.app.domain.model.LeverageQuadrant
import com.studyos.app.domain.model.LeverageTopicItem
import com.studyos.app.domain.model.Subject
import com.studyos.app.domain.model.Topic
import com.studyos.app.domain.model.TopicMasteryState
import java.util.Locale

object LeverageMatrixEngine {

    fun buildMatrix(
        topics: List<Topic>,
        chapters: List<Chapter>,
        subjects: List<Subject>
    ): LeverageMatrixReport {
        if (topics.isEmpty()) {
            return LeverageMatrixReport(
                strategicSummary = "Add topics to your syllabus to unlock the 95% Leverage Matrix and prioritize study hours."
            )
        }

        val chapterMap = chapters.associateBy { it.id }
        val subjectMap = subjects.associateBy { it.id }

        val items = topics.map { topic ->
            val chapter = chapterMap[topic.chapterId]
            val chapterName = chapter?.name ?: "General Chapter"
            val subject = chapter?.let { subjectMap[it.subjectId] }
            val subjectId = subject?.id ?: ""
            val subjectName = subject?.name ?: "General"

            val weightFactor = when (topic.examRelevance) {
                ExamRelevance.HIGH -> 3.0
                ExamRelevance.MEDIUM -> 2.0
                ExamRelevance.LOW -> 1.0
            }

            val baseMastery = when (topic.masteryState) {
                TopicMasteryState.NOT_STARTED -> 10
                TopicMasteryState.LEARNING -> 40
                TopicMasteryState.REVISED -> 75
                TopicMasteryState.MASTERED -> 95
            }
            // Penalize for weakness score
            val masteryPct = (baseMastery - (topic.weaknessScore * 25).toInt()).coerceIn(0, 100)
            val gapPct = 100 - masteryPct

            val effortHours = (0.75 + (topic.weaknessScore * 1.25)).coerceIn(0.5, 3.0)
            val marksYield = (weightFactor * 2.5 * (gapPct / 100.0))
            val leverageScore = (weightFactor * gapPct) / effortHours

            // Quadrant evaluation:
            // High Weight: HIGH or MEDIUM
            // High Mastery: >= 70%
            val isHighWeight = topic.examRelevance != ExamRelevance.LOW
            val isHighMastery = masteryPct >= 70

            val quadrant = when {
                isHighWeight && !isHighMastery -> LeverageQuadrant.HIGH_IMPACT
                isHighWeight && isHighMastery -> LeverageQuadrant.SECURE
                !isHighWeight && !isHighMastery -> LeverageQuadrant.QUICK_WINS
                else -> LeverageQuadrant.OVERSTUDIED
            }

            LeverageTopicItem(
                topicId = topic.id,
                topicName = topic.name,
                chapterId = topic.chapterId,
                chapterName = chapterName,
                subjectId = subjectId,
                subjectName = subjectName,
                examRelevance = topic.examRelevance,
                masteryPercentage = masteryPct,
                weaknessScore = topic.weaknessScore,
                leverageScore = leverageScore,
                estimatedHoursToMaster = effortHours,
                marksYieldPotential = marksYield,
                quadrant = quadrant
            )
        }.sortedByDescending { it.leverageScore }

        val highImpact = items.count { it.quadrant == LeverageQuadrant.HIGH_IMPACT }
        val secure = items.count { it.quadrant == LeverageQuadrant.SECURE }
        val quickWins = items.count { it.quadrant == LeverageQuadrant.QUICK_WINS }
        val overstudied = items.count { it.quadrant == LeverageQuadrant.OVERSTUDIED }

        val totalMarksGain = items
            .filter { it.quadrant == LeverageQuadrant.HIGH_IMPACT || it.quadrant == LeverageQuadrant.QUICK_WINS }
            .sumOf { it.marksYieldPotential }

        val topTopic = items.firstOrNull { it.quadrant == LeverageQuadrant.HIGH_IMPACT } ?: items.firstOrNull()

        val summary = if (highImpact > 0) {
            val formattedGain = String.format(Locale.US, "%.1f", totalMarksGain)
            "Found $highImpact High-Impact topics unlocking +$formattedGain marks. Focus on '${topTopic?.topicName}' first for maximum ROI per hour."
        } else {
            "Foundation is rock solid ($secure secure topics). Complete remaining quick wins to lock in 95%+."
        }

        return LeverageMatrixReport(
            items = items,
            highImpactCount = highImpact,
            secureCount = secure,
            quickWinsCount = quickWins,
            overstudiedCount = overstudied,
            topLeverageTopic = topTopic,
            estimatedTotalMarksGainTo95 = totalMarksGain,
            strategicSummary = summary
        )
    }
}
