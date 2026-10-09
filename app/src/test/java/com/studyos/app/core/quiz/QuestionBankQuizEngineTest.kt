package com.studyos.app.core.quiz

import com.studyos.app.core.database.entity.QuestionBankEntity
import com.studyos.app.domain.model.QuestionType
import com.studyos.app.domain.model.QuizDifficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestionBankQuizEngineTest {

    @Test
    fun parseQuestionContent_parsesInlineOptionsCorrectly() {
        val qText = "What is the unit of electric current? (A) Volt (B) Ampere (C) Ohm (D) Watt"
        val scheme = "B"

        val parsed = QuestionBankQuizEngine.parseQuestionContent(
            questionText = qText,
            markingScheme = scheme,
            questionTypeHint = "MCQ"
        )

        assertEquals("What is the unit of electric current?", parsed.questionStem)
        assertEquals(4, parsed.options.size)
        assertEquals("Volt", parsed.options[0])
        assertEquals("Ampere", parsed.options[1])
        assertEquals("Ohm", parsed.options[2])
        assertEquals("Watt", parsed.options[3])
        assertEquals(QuestionType.MCQ, parsed.type)
        assertEquals("Ampere", parsed.correctAnswer)
    }

    @Test
    fun parseQuestionContent_parsesMultilineOptionsCorrectly() {
        val qText = """
            Which gas is most abundant in Earth's atmosphere?
            A) Oxygen
            B) Nitrogen
            C) Carbon Dioxide
            D) Argon
        """.trimIndent()
        val scheme = "Option B: Nitrogen"

        val parsed = QuestionBankQuizEngine.parseQuestionContent(
            questionText = qText,
            markingScheme = scheme,
            questionTypeHint = "MCQ"
        )

        assertEquals("Which gas is most abundant in Earth's atmosphere?", parsed.questionStem)
        assertEquals(4, parsed.options.size)
        assertEquals("Oxygen", parsed.options[0])
        assertEquals("Nitrogen", parsed.options[1])
        assertEquals(QuestionType.MCQ, parsed.type)
        assertEquals("Nitrogen", parsed.correctAnswer)
    }

    @Test
    fun parseQuestionContent_detectsTrueFalse() {
        val qText = "Sound travels faster in solids than in air. True or False?"
        val scheme = "True"

        val parsed = QuestionBankQuizEngine.parseQuestionContent(
            questionText = qText,
            markingScheme = scheme,
            questionTypeHint = "TRUE_FALSE"
        )

        assertEquals(QuestionType.TRUE_FALSE, parsed.type)
        assertEquals(listOf("True", "False"), parsed.options)
        assertEquals("True", parsed.correctAnswer)
    }

    @Test
    fun parseQuestionContent_handlesShortAnswerAndDerivations() {
        val qText = "State Faraday's First Law of Electromagnetic Induction."
        val scheme = "Whenever magnetic flux linked with a circuit changes, an emf is induced in it."

        val parsed = QuestionBankQuizEngine.parseQuestionContent(
            questionText = qText,
            markingScheme = scheme,
            questionTypeHint = "SHORT_ANSWER"
        )

        assertEquals(QuestionType.SHORT_ANSWER, parsed.type)
        assertTrue(parsed.options.isEmpty())
        assertEquals(scheme, parsed.correctAnswer)
    }

    @Test
    fun parseQuestionContent_synthesizesDistractorsFromSiblingsWhenMcqHasNoInlineOptions() {
        val qText = "What is the powerhouse of the cell?"
        val scheme = "Mitochondria"
        val siblings = listOf("Ribosome", "Nucleus", "Chloroplast", "Golgi Body")

        val parsed = QuestionBankQuizEngine.parseQuestionContent(
            questionText = qText,
            markingScheme = scheme,
            questionTypeHint = "MCQ",
            siblingAnswers = siblings
        )

        assertEquals(QuestionType.MCQ, parsed.type)
        assertEquals(4, parsed.options.size)
        assertTrue(parsed.options.contains("Mitochondria"))
        assertEquals("Mitochondria", parsed.correctAnswer)
    }

    @Test
    fun buildQuiz_constructsQuizExclusivelyFromQuestionBankQuestions() {
        val questions = listOf(
            QuestionBankEntity(
                id = "q1",
                subjectId = "sub1",
                chapterId = "chap1",
                topicId = "topic1",
                questionText = "What is SI unit of force? (A) Dyne (B) Newton (C) Pascal (D) Joule",
                markingScheme = "Newton",
                marks = 1,
                questionType = "MCQ",
                difficulty = "EASY"
            ),
            QuestionBankEntity(
                id = "q2",
                subjectId = "sub1",
                chapterId = "chap1",
                topicId = "topic1",
                questionText = "State Newton's second law.",
                markingScheme = "F = dp/dt",
                marks = 2,
                questionType = "SHORT_ANSWER",
                difficulty = "MEDIUM"
            )
        )

        val result = QuestionBankQuizEngine.buildQuiz(
            questions = questions,
            quizTitle = "Laws of Motion Topic Quiz",
            subjectId = "sub1",
            chapterId = "chap1",
            topicName = "Laws of Motion",
            targetQuestionCount = 5,
            targetDifficulty = QuizDifficulty.MEDIUM
        )

        assertNotNull(result)
        val (quiz, quizQuestions) = result!!
        assertEquals(2, quiz.questionCount)
        assertEquals(2, quizQuestions.size)
        assertEquals("Laws of Motion Topic Quiz", quiz.title)
        assertEquals("sub1", quiz.subjectId)
        assertEquals("chap1", quiz.chapterId)
        assertEquals("Laws of Motion", quizQuestions[0].topic)
    }

    @Test
    fun parseQuestionContent_detectsFibQuestion() {
        val qText = "The light-absorbing green pigment found inside chloroplasts is called ________."
        val scheme = "Chlorophyll • Explanation: Chlorophyll a and b absorb light."

        val parsed = QuestionBankQuizEngine.parseQuestionContent(
            questionText = qText,
            markingScheme = scheme,
            questionTypeHint = "FIB"
        )

        assertEquals(QuestionType.FIB, parsed.type)
        assertEquals(qText, parsed.questionStem)
        assertTrue(parsed.options.isEmpty())
        assertEquals("Chlorophyll", parsed.correctAnswer)
    }

    @Test
    fun checkQuizAnswersMatch_forgivingFibMatching() {
        val check = { s: String, c: String, opts: List<String> ->
            com.studyos.app.domain.usecase.checkQuizAnswersMatch(s, c, opts)
        }

        // Exact match
        assertTrue(check("Chlorophyll", "Chlorophyll", emptyList()))
        // Case-insensitive
        assertTrue(check("chlorophyll", "Chlorophyll", emptyList()))
        // Punctuation forgiving
        assertTrue(check("Chlorophyll.", "Chlorophyll", emptyList()))
        assertTrue(check("\"Chlorophyll\"", "Chlorophyll", emptyList()))
        // Leading English article forgiving
        assertTrue(check("the chlorophyll", "Chlorophyll", emptyList()))
        assertTrue(check("chlorophyll", "the chlorophyll", emptyList()))
        // Slash alternative forgiving
        assertTrue(check("chlorophyll", "chlorophyll / chloroplast pigments", emptyList()))
        assertTrue(check("chloroplast pigments", "chlorophyll / chloroplast pigments", emptyList()))
    }
}
