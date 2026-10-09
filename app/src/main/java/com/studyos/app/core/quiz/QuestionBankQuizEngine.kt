package com.studyos.app.core.quiz

import com.studyos.app.core.database.entity.QuestionBankEntity
import com.studyos.app.domain.model.QuestionType
import com.studyos.app.domain.model.Quiz
import com.studyos.app.domain.model.QuizDifficulty
import com.studyos.app.domain.model.QuizQuestion
import java.util.UUID

object QuestionBankQuizEngine {

    data class ParsedQuestionContent(
        val questionStem: String,
        val options: List<String>,
        val type: QuestionType,
        val correctAnswer: String
    )

    private val INLINE_OPTION_REGEX = Regex(
        """(?i)(?:^|\s)(?:\(([A-D1-4])\)|\[([A-D1-4])\]|([A-D1-4])[\.\)])\s+"""
    )

    /**
     * Parses a QuestionBankEntity into clean question stem, options, detected type, and resolved correct answer.
     * 100% offline and deterministic.
     */
    fun parseQuestionContent(
        questionText: String,
        markingScheme: String?,
        questionTypeHint: String?,
        siblingAnswers: List<String> = emptyList()
    ): ParsedQuestionContent {
        val rawText = questionText.trim()
        val rawScheme = (markingScheme ?: "").trim()

        // 1. Check for True / False
        if (questionTypeHint?.equals("TRUE_FALSE", ignoreCase = true) == true ||
            rawScheme.equals("true", ignoreCase = true) || rawScheme.equals("false", ignoreCase = true) ||
            rawScheme.startsWith("true •", ignoreCase = true) || rawScheme.startsWith("false •", ignoreCase = true) ||
            rawText.contains("True or False", ignoreCase = true) || rawText.contains("True/False", ignoreCase = true)
        ) {
            val resolvedCorrect = if (rawScheme.startsWith("false", ignoreCase = true)) "False" else "True"
            return ParsedQuestionContent(
                questionStem = rawText,
                options = listOf("True", "False"),
                type = QuestionType.TRUE_FALSE,
                correctAnswer = resolvedCorrect
            )
        }

        // 2. Try parsing multiline options: e.g. A) opt1 \n B) opt2 ...
        val multilineResult = parseMultilineOptions(rawText, rawScheme)
        if (multilineResult != null && multilineResult.options.size >= 2) {
            return multilineResult
        }

        // 3. Try parsing inline options: e.g. (A) opt1 (B) opt2 (C) opt3 (D) opt4
        val inlineResult = parseInlineOptions(rawText, rawScheme)
        if (inlineResult != null && inlineResult.options.size >= 2) {
            return inlineResult
        }

        // 4. Check for Fill in the Blanks (FIB)
        val isFib = questionTypeHint?.equals("FIB", ignoreCase = true) == true ||
            rawText.contains("____") || rawText.contains("[blank]", ignoreCase = true)
        if (isFib) {
            val resolvedScheme = rawScheme.substringBefore(" •").substringBefore(" -").trim().ifBlank { rawScheme }
            return ParsedQuestionContent(
                questionStem = rawText,
                options = emptyList(),
                type = QuestionType.FIB,
                correctAnswer = if (resolvedScheme.isNotBlank()) resolvedScheme else "Answer"
            )
        }

        // 4. If tagged as MCQ but no options found in text, generate distractors from sibling pool
        val isMcq = questionTypeHint?.equals("MCQ", ignoreCase = true) == true
        if (isMcq && rawScheme.isNotBlank()) {
            val distractors = siblingAnswers
                .filter { it.isNotBlank() && !it.equals(rawScheme, ignoreCase = true) && it.length <= 80 }
                .shuffled()
                .distinct()
                .take(3)

            if (distractors.size >= 2) {
                val allOptions = (distractors + rawScheme).shuffled()
                return ParsedQuestionContent(
                    questionStem = rawText,
                    options = allOptions,
                    type = QuestionType.MCQ,
                    correctAnswer = rawScheme
                )
            }
        }

        // 5. Short answer fallback (works natively in QuizRunnerScreen with StudyOSTextField)
        return ParsedQuestionContent(
            questionStem = rawText,
            options = emptyList(),
            type = QuestionType.SHORT_ANSWER,
            correctAnswer = if (rawScheme.isNotBlank()) rawScheme else "Answer"
        )
    }

    private fun parseMultilineOptions(rawText: String, rawScheme: String): ParsedQuestionContent? {
        val lines = rawText.lines()
        val optionPrefixRegex = Regex("""^\s*(?:\(([A-Da-d1-4])\)|\[([A-Da-d1-4])\]|([A-Da-d1-4])[\.\)])\s*(.*)$""")

        val stemLines = mutableListOf<String>()
        val parsedOptions = mutableListOf<String>()
        var startedOptions = false

        for (line in lines) {
            val match = optionPrefixRegex.find(line)
            if (match != null) {
                startedOptions = true
                val optText = match.groupValues[4].trim()
                if (optText.isNotBlank()) {
                    parsedOptions.add(optText)
                }
            } else {
                if (!startedOptions) {
                    stemLines.add(line)
                } else if (line.isNotBlank() && parsedOptions.isNotEmpty()) {
                    // continuation of last option
                    val lastIdx = parsedOptions.lastIndex
                    parsedOptions[lastIdx] = parsedOptions[lastIdx] + " " + line.trim()
                }
            }
        }

        if (parsedOptions.size >= 2) {
            val stem = stemLines.joinToString("\n").trim().ifBlank { rawText }
            val resolvedCorrect = resolveCorrectAnswer(rawScheme, parsedOptions)
            return ParsedQuestionContent(
                questionStem = stem,
                options = parsedOptions,
                type = QuestionType.MCQ,
                correctAnswer = resolvedCorrect
            )
        }
        return null
    }

    private fun parseInlineOptions(rawText: String, rawScheme: String): ParsedQuestionContent? {
        val matches = INLINE_OPTION_REGEX.findAll(rawText).toList()
        if (matches.size < 2) return null

        val stem = rawText.substring(0, matches.first().range.first).trim()
        val options = mutableListOf<String>()

        for (i in matches.indices) {
            val startIdx = matches[i].range.last + 1
            val endIdx = if (i + 1 < matches.size) matches[i + 1].range.first else rawText.length
            val optText = rawText.substring(startIdx, endIdx).trim()
            if (optText.isNotBlank()) {
                options.add(optText)
            }
        }

        if (options.size >= 2) {
            val resolvedCorrect = resolveCorrectAnswer(rawScheme, options)
            return ParsedQuestionContent(
                questionStem = stem.ifBlank { rawText },
                options = options,
                type = QuestionType.MCQ,
                correctAnswer = resolvedCorrect
            )
        }
        return null
    }

    /**
     * Resolves marking scheme (e.g. "A", "Option B", or exact text) to matching option choice.
     */
    fun resolveCorrectAnswer(markingScheme: String, options: List<String>): String {
        val scheme = markingScheme.trim()
        if (scheme.isBlank() && options.isNotEmpty()) return options.first()

        // 1. Direct equality with an option
        for (opt in options) {
            if (opt.equals(scheme, ignoreCase = true)) return opt
        }

        // 2. Letter index matching (A -> 0, B -> 1, C -> 2, D -> 3)
        val cleanLetter = scheme
            .removePrefix("Option ")
            .removePrefix("option ")
            .removePrefix("Ans:")
            .removePrefix("Answer:")
            .removeSurrounding("(", ")")
            .removeSurrounding("[", "]")
            .removeSuffix(".")
            .removeSuffix(")")
            .trim()

        val letterMap = mapOf(
            "A" to 0, "a" to 0, "1" to 0,
            "B" to 1, "b" to 1, "2" to 1,
            "C" to 2, "c" to 2, "3" to 2,
            "D" to 3, "d" to 3, "4" to 3
        )

        letterMap[cleanLetter]?.let { idx ->
            if (idx in options.indices) return options[idx]
        }

        // 3. Partial match
        for (opt in options) {
            if (opt.contains(scheme, ignoreCase = true) || scheme.contains(opt, ignoreCase = true)) {
                return opt
            }
        }

        return options.firstOrNull() ?: scheme
    }

    /**
     * Builds a complete Quiz and QuizQuestion set from questions in QuestionBankEntity.
     * Guaranteed 100% offline with zero cloud dependency.
     */
    fun buildQuiz(
        questions: List<QuestionBankEntity>,
        quizTitle: String,
        subjectId: String,
        chapterId: String?,
        topicName: String?,
        targetQuestionCount: Int = 5,
        targetDifficulty: QuizDifficulty? = null
    ): Pair<Quiz, List<QuizQuestion>>? {
        if (questions.isEmpty()) return null

        // Filter / prioritize difficulty
        val selectedEntities = if (targetDifficulty != null) {
            val matching = questions.filter { it.difficulty.equals(targetDifficulty.name, ignoreCase = true) }
            if (matching.isNotEmpty()) {
                val nonMatching = questions.filterNot { it.difficulty.equals(targetDifficulty.name, ignoreCase = true) }.shuffled()
                (matching.shuffled() + nonMatching).take(targetQuestionCount)
            } else {
                questions.shuffled().take(targetQuestionCount)
            }
        } else {
            questions.shuffled().take(targetQuestionCount)
        }

        val poolAnswers = questions.map { it.markingScheme.trim() }.filter { it.isNotBlank() }.distinct()
        val quizId = UUID.randomUUID().toString()

        val quizQuestions = selectedEntities.mapIndexed { index, qb ->
            val parsed = parseQuestionContent(
                questionText = qb.questionText,
                markingScheme = qb.markingScheme,
                questionTypeHint = qb.questionType,
                siblingAnswers = poolAnswers
            )

            QuizQuestion(
                id = UUID.randomUUID().toString(),
                quizId = quizId,
                question = parsed.questionStem,
                options = parsed.options,
                correctAnswer = parsed.correctAnswer,
                explanation = if (qb.markingScheme.isNotBlank()) qb.markingScheme else null,
                type = parsed.type,
                topic = topicName ?: qb.topicId,
                orderIndex = index
            )
        }

        val effectiveDifficulty = targetDifficulty ?: when {
            selectedEntities.any { it.difficulty.equals("HARD", ignoreCase = true) } -> QuizDifficulty.HARD
            selectedEntities.any { it.difficulty.equals("EASY", ignoreCase = true) } -> QuizDifficulty.EASY
            else -> QuizDifficulty.MEDIUM
        }

        val quiz = Quiz(
            id = quizId,
            title = quizTitle,
            subjectId = subjectId,
            chapterId = chapterId,
            questionCount = quizQuestions.size,
            difficulty = effectiveDifficulty,
            createdAt = System.currentTimeMillis()
        )

        return Pair(quiz, quizQuestions)
    }

    fun Quiz.toEntity(): com.studyos.app.core.database.entity.QuizEntity {
        return com.studyos.app.core.database.entity.QuizEntity(
            id = id,
            title = title,
            subjectId = subjectId,
            chapterId = chapterId,
            questionCount = questionCount,
            difficulty = difficulty.name,
            timeLimitMinutes = timeLimitMinutes,
            createdAt = createdAt
        )
    }

    fun QuizQuestion.toEntity(): com.studyos.app.core.database.entity.QuizQuestionEntity {
        val jsonArray = org.json.JSONArray()
        options.forEach { jsonArray.put(it) }
        return com.studyos.app.core.database.entity.QuizQuestionEntity(
            id = id,
            quizId = quizId,
            question = question,
            optionsJson = jsonArray.toString(),
            correctAnswer = correctAnswer,
            explanation = explanation,
            questionType = type.name,
            topic = topic,
            orderIndex = orderIndex
        )
    }
}
