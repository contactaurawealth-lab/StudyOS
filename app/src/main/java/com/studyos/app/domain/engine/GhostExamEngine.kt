package com.studyos.app.domain.engine

import com.studyos.app.domain.model.ExamQuestionType
import com.studyos.app.domain.model.LossReasonCategory
import com.studyos.app.domain.model.LostMark
import com.studyos.app.domain.model.Mistake
import com.studyos.app.domain.model.QuestionBankItem
import com.studyos.app.domain.model.QuestionDifficulty
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import kotlin.math.roundToInt

data class GhostExamQuestionItem(
    val question: QuestionBankItem,
    val sectionName: String,
    val questionNumber: Int,
    val marksAllocated: Int,
    val sourceLostMarkId: String? = null,
    val sourceMistakeId: String? = null
)

data class GhostExamPlan(
    val title: String,
    val subjectId: String,
    val totalMarks: Int,
    val durationMinutes: Int,
    val sectionsJson: String,
    val questionItems: List<GhostExamQuestionItem>,
    val remediatedLostMarkIds: List<String>,
    val remediatedMistakeIds: List<String>
)

object GhostExamEngine {

    fun generatePlan(
        subjectId: String,
        subjectName: String,
        fallbackChapterId: String,
        unremediatedLostMarks: List<LostMark>,
        unresolvedMistakes: List<Mistake>,
        existingQuestions: List<QuestionBankItem> = emptyList()
    ): GhostExamPlan {
        val existingQuestionsById = existingQuestions.associateBy { it.id }
        val questionItems = mutableListOf<GhostExamQuestionItem>()
        val remediatedLostMarkIds = mutableListOf<String>()
        val remediatedMistakeIds = mutableListOf<String>()

        var questionNumber = 1

        // 1. Process Lost Marks that link to questions or create remedial questions
        val executionSlips = unremediatedLostMarks.filter {
            it.category in setOf(
                LossReasonCategory.CARELESS_MISTAKE,
                LossReasonCategory.CALCULATION_ERROR,
                LossReasonCategory.MISREAD,
                LossReasonCategory.TIME_SHORTAGE
            )
        }

        val conceptGaps = unremediatedLostMarks.filter { it !in executionSlips }

        // Section A: Execution Slips
        executionSlips.forEach { lostMark ->
            val q = lostMark.questionId?.let { existingQuestionsById[it] } ?: run {
                QuestionBankItem(
                    id = UUID.randomUUID().toString(),
                    subjectId = subjectId,
                    chapterId = fallbackChapterId,
                    topicId = lostMark.topicId,
                    questionText = lostMark.reflection?.takeIf { it.isNotBlank() }
                        ?: "Remediation Question: Verify calculation accuracy and error-checking steps for this problem.",
                    markingScheme = "Verify full step working, unit consistency, and final answer sanity check.",
                    marks = lostMark.marksLost.roundToInt().coerceAtLeast(1),
                    questionType = ExamQuestionType.SHORT_ANSWER,
                    difficulty = QuestionDifficulty.MEDIUM
                )
            }
            questionItems.add(
                GhostExamQuestionItem(
                    question = q,
                    sectionName = "Section A: Execution & Calculation Verification",
                    questionNumber = questionNumber++,
                    marksAllocated = q.marks,
                    sourceLostMarkId = lostMark.id
                )
            )
            remediatedLostMarkIds.add(lostMark.id)
        }

        // Section B: Concept Gaps
        conceptGaps.forEach { lostMark ->
            val q = lostMark.questionId?.let { existingQuestionsById[it] } ?: run {
                QuestionBankItem(
                    id = UUID.randomUUID().toString(),
                    subjectId = subjectId,
                    chapterId = fallbackChapterId,
                    topicId = lostMark.topicId,
                    questionText = lostMark.reflection?.takeIf { it.isNotBlank() }
                        ?: "Remediation Question: Re-derive and explain the core principle behind this question.",
                    markingScheme = "State definition, formula, underlying assumptions, and full derivation.",
                    marks = lostMark.marksLost.roundToInt().coerceAtLeast(1),
                    questionType = ExamQuestionType.SHORT_ANSWER,
                    difficulty = QuestionDifficulty.HARD
                )
            }
            questionItems.add(
                GhostExamQuestionItem(
                    question = q,
                    sectionName = "Section B: Conceptual Mastery Remediation",
                    questionNumber = questionNumber++,
                    marksAllocated = q.marks,
                    sourceLostMarkId = lostMark.id
                )
            )
            remediatedLostMarkIds.add(lostMark.id)
        }

        // Also incorporate Unresolved Mistakes if not already covered
        unresolvedMistakes.forEach { mistake ->
            val marks = mistake.marksLost.roundToInt().coerceAtLeast(1)
            val q = QuestionBankItem(
                id = UUID.randomUUID().toString(),
                subjectId = subjectId,
                chapterId = mistake.chapterId ?: fallbackChapterId,
                topicId = null,
                questionText = mistake.question,
                markingScheme = "Expected: ${mistake.correctAnswer}. Rationale: ${mistake.explanation ?: "Clear student misconception: was ${mistake.studentAnswer}"}",
                marks = marks,
                questionType = ExamQuestionType.SHORT_ANSWER,
                difficulty = QuestionDifficulty.MEDIUM
            )
            val section = if (mistake.lossCategory.contains("CARELESS") || mistake.lossCategory.contains("CALCULATION")) {
                "Section A: Execution & Calculation Verification"
            } else {
                "Section B: Conceptual Mastery Remediation"
            }
            questionItems.add(
                GhostExamQuestionItem(
                    question = q,
                    sectionName = section,
                    questionNumber = questionNumber++,
                    marksAllocated = marks,
                    sourceMistakeId = mistake.id
                )
            )
            remediatedMistakeIds.add(mistake.id)
        }

        val totalMarks = questionItems.sumOf { it.marksAllocated }.coerceAtLeast(10)
        val durationMinutes = (totalMarks * 1.5).roundToInt().coerceAtLeast(15)

        // Group into sections JSON
        val sectionsArray = JSONArray()
        val groupedSections = questionItems.groupBy { it.sectionName }
        groupedSections.forEach { (secName, items) ->
            val secObj = JSONObject().apply {
                put("name", secName)
                put("totalMarks", items.sumOf { it.marksAllocated })
                val questionsArray = JSONArray()
                items.forEach { item ->
                    questionsArray.put(JSONObject().apply {
                        put("number", item.questionNumber)
                        put("marks", item.marksAllocated)
                        put("questionText", item.question.questionText)
                        put("markingScheme", item.question.markingScheme)
                    })
                }
                put("questions", questionsArray)
            }
            sectionsArray.put(secObj)
        }

        return GhostExamPlan(
            title = "Ghost Exam • $subjectName Remediation",
            subjectId = subjectId,
            totalMarks = totalMarks,
            durationMinutes = durationMinutes,
            sectionsJson = sectionsArray.toString(),
            questionItems = questionItems,
            remediatedLostMarkIds = remediatedLostMarkIds,
            remediatedMistakeIds = remediatedMistakeIds
        )
    }
}
