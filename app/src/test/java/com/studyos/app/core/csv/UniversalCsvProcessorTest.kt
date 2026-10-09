package com.studyos.app.core.csv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UniversalCsvProcessorTest {

    // No database needed for parsing and validation tests
    private val processor = UniversalCsvProcessor()

    @Test
    fun testTemplatesGeneration() {
        for (type in CsvEntityType.values()) {
            val template = processor.getTemplate(type)
            assertTrue("Template for $type must not be empty", template.isNotBlank())
            val validation = processor.parseAndValidate(type, template)
            assertEquals("Template for $type should have zero validation errors", 0, validation.errors.size)
            assertTrue("Template for $type should have at least 1 parsed row", validation.parsedData.isNotEmpty())
        }
    }

    @Test
    fun testSyllabusCsvParsingAndEscaping() {
        val csv = "SubjectName,ChapterName,TopicName,MasteryState,ExamRelevance\n" +
            "\"Advanced Mathematics\",\"Differential Calculus\",\"Limits, Continuity & Differentiability\",LEARNING,HIGH\n" +
            "\"Physics, Mechanics\",\"Rotational Motion\",\"Moment of Inertia \"\"Rigid Bodies\"\"\",MASTERED,HIGH\n"

        val validation = processor.parseAndValidate(CsvEntityType.SYLLABUS, csv)
        assertEquals(0, validation.errors.size)
        assertEquals(2, validation.validRowsCount)

        val row0 = validation.parsedData[0]
        assertEquals("Advanced Mathematics", row0["subjectname"])
        assertEquals("Differential Calculus", row0["chaptername"])
        assertEquals("Limits, Continuity & Differentiability", row0["topicname"])
        assertEquals("LEARNING", row0["masterystate"])
        assertEquals("HIGH", row0["examrelevance"])

        val row1 = validation.parsedData[1]
        assertEquals("Physics, Mechanics", row1["subjectname"])
        assertEquals("Rotational Motion", row1["chaptername"])
        assertEquals("Moment of Inertia \"Rigid Bodies\"", row1["topicname"])
    }

    @Test
    fun testMissingRequiredHeaders() {
        val badCsv = """
            SomeRandomColumn,AnotherColumn
            Value1,Value2
        """.trimIndent()

        val validation = processor.parseAndValidate(CsvEntityType.SYLLABUS, badCsv)
        assertEquals(0, validation.validRowsCount)
        assertTrue(validation.errors.isNotEmpty())
        assertTrue(validation.errors[0].errorMessage.contains("Missing required columns"))
    }

    @Test
    fun testQuestionBankValidation() {
        val validCsv = """
            SubjectName,ChapterName,TopicName,QuestionText,MarkingScheme,Marks,QuestionType,Difficulty
            Chemistry,Electrochemistry,Nernst Equation,"Calculate EMF of cell","Formula (1m) + Calculation (2m)",3,LONG_ANSWER,HARD
        """.trimIndent()

        val validationValid = processor.parseAndValidate(CsvEntityType.QUESTION_BANK, validCsv)
        assertEquals(0, validationValid.errors.size)
        assertEquals(1, validationValid.validRowsCount)

        val invalidCsv = """
            SubjectName,ChapterName,TopicName,QuestionText,MarkingScheme,Marks,QuestionType,Difficulty
            Chemistry,Electrochemistry,Nernst Equation,"Calculate EMF of cell","Formula (1m)",INVALID_MARKS,LONG_ANSWER,HARD
            Chemistry,Electrochemistry,Nernst Equation,"",No Question,2,LONG_ANSWER,HARD
        """.trimIndent()

        val validationInvalid = processor.parseAndValidate(CsvEntityType.QUESTION_BANK, invalidCsv)
        assertEquals(2, validationInvalid.errors.size)
        assertEquals(0, validationInvalid.validRowsCount)
        assertTrue(validationInvalid.errors[0].errorMessage.contains("Marks must be a positive integer"))
        assertTrue(validationInvalid.errors[1].errorMessage.contains("QuestionText cannot be blank"))
    }

    @Test
    fun testEmptyCsv() {
        val validation = processor.parseAndValidate(CsvEntityType.RECALL_CARDS, "")
        assertEquals(1, validation.errors.size)
        assertEquals(0, validation.validRowsCount)
        assertTrue(validation.errors[0].errorMessage.contains("CSV content is empty"))
    }

    @Test
    fun testObjectiveQuestionsCsvParsingAndValidation() {
        val template = processor.getObjectiveQuestionsCsvTemplate()
        val result = processor.parseAndValidateObjectiveQuestions(template)
        assertEquals("Template should have 0 errors", 0, result.errors.size)
        assertTrue("Template should have parsed rows", result.validRowsCount >= 6)

        val row0 = result.parsedData[0]
        assertEquals("Physics", row0["subjectname"])
        assertEquals("Current Electricity", row0["chaptername"])
        assertEquals("MCQ", row0["questiontype"])
        assertEquals("B", row0["correctanswer"])
        assertEquals("Free Electrons", row0["optionb"])

        val row1 = result.parsedData[1]
        assertEquals("Biology", row1["subjectname"])
        assertEquals("FIB", row1["questiontype"])
        assertEquals("Chlorophyll", row1["correctanswer"])

        val row2 = result.parsedData[2]
        assertEquals("Chemistry", row2["subjectname"])
        assertEquals("TRUE_FALSE", row2["questiontype"])
        assertEquals("False", row2["correctanswer"])
    }

    @Test
    fun testObjectiveQuestionsValidationErrors() {
        val invalidCsv = """
            SubjectName,ChapterName,TopicName,QuestionType,Question,OptionA,OptionB,OptionC,OptionD,CorrectAnswer,Explanation,Difficulty,Marks
            Physics,Mechanics,Kinematics,MCQ,"Which vector?",OnlyOneOption,,,,A,Exp,EASY,1
            Physics,Mechanics,Kinematics,TRUE_FALSE,"Gravity is repulsive",,,,,Maybe,Exp,EASY,1
            Physics,Mechanics,Kinematics,FIB,"",,,,,MissingStem,Exp,EASY,1
        """.trimIndent()

        val result = processor.parseAndValidateObjectiveQuestions(invalidCsv)
        assertEquals(3, result.errors.size)
        assertTrue(result.errors[0].errorMessage.contains("requires at least OptionA and OptionB"))
        assertTrue(result.errors[1].errorMessage.contains("True/False answer must be True or False"))
        assertTrue(result.errors[2].errorMessage.contains("Question cannot be blank"))
    }
}
