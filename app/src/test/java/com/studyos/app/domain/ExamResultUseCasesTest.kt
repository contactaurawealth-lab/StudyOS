package com.studyos.app.domain

import com.studyos.app.core.database.dao.CategoryLostMarksTuple
import com.studyos.app.domain.model.Exam
import com.studyos.app.domain.model.ExamResult
import com.studyos.app.domain.model.LossReasonCategory
import com.studyos.app.domain.model.LostMark
import com.studyos.app.domain.model.Mistake
import com.studyos.app.domain.model.Paper
import com.studyos.app.domain.model.PaperQuestionItem
import com.studyos.app.domain.model.PaperStatus
import com.studyos.app.domain.repository.ExamRepository
import com.studyos.app.domain.repository.ExamResultRepository
import com.studyos.app.domain.repository.LostMarksRepository
import com.studyos.app.domain.repository.MistakeRepository
import com.studyos.app.domain.repository.PaperRepository
import com.studyos.app.domain.usecase.GetExamDiagnosticUseCase
import com.studyos.app.domain.usecase.LostMarkInput
import com.studyos.app.domain.usecase.RecordExamResultUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class TestExamResultRepository : ExamResultRepository {
    val results = mutableListOf<ExamResult>()
    private val resultsFlow = MutableStateFlow<List<ExamResult>>(emptyList())

    private fun sync() {
        resultsFlow.value = results.toList()
    }

    override suspend fun getResultForPaper(paperId: String): ExamResult? =
        results.find { it.paperId == paperId }

    override fun observeResultForPaper(paperId: String): Flow<ExamResult?> =
        resultsFlow.map { list -> list.find { it.paperId == paperId } }

    override fun getAllResults(): Flow<List<ExamResult>> = resultsFlow

    override suspend fun getResultById(id: String): ExamResult? =
        results.find { it.id == id }

    override suspend fun saveResult(result: ExamResult) {
        val index = results.indexOfFirst { it.id == result.id }
        if (index >= 0) results[index] = result else results.add(result)
        sync()
    }

    override suspend fun deleteResult(result: ExamResult) {
        results.removeAll { it.id == result.id }
        sync()
    }

    override fun getOverallAveragePercentage(): Flow<Double?> =
        resultsFlow.map { list -> if (list.isEmpty()) null else list.map { it.percentage }.average() }
}

private class TestLostMarksRepository : LostMarksRepository {
    val lostMarks = mutableListOf<LostMark>()
    private val flow = MutableStateFlow<List<LostMark>>(emptyList())

    private fun sync() {
        flow.value = lostMarks.toList()
    }

    override fun getLostMarksForResult(resultId: String): Flow<List<LostMark>> =
        flow.map { list -> list.filter { it.resultId == resultId } }

    override suspend fun getLostMarksForResultOnce(resultId: String): List<LostMark> =
        lostMarks.filter { it.resultId == resultId }

    override fun getLostMarksForTopic(topicId: String): Flow<List<LostMark>> =
        flow.map { list -> list.filter { it.topicId == topicId } }

    override fun getLostMarksByCategory(category: LossReasonCategory): Flow<List<LostMark>> =
        flow.map { list -> list.filter { it.category == category } }

    override fun getAllLostMarks(): Flow<List<LostMark>> = flow

    override fun getLostMarksCategoryBreakdown(): Flow<List<CategoryLostMarksTuple>> =
        flow.map { list ->
            list.groupBy { it.category.name }.map { (cat, marks) ->
                CategoryLostMarksTuple(category = cat, totalLost = marks.sumOf { it.marksLost })
            }
        }

    override suspend fun saveLostMark(lostMark: LostMark) {
        val index = lostMarks.indexOfFirst { it.id == lostMark.id }
        if (index >= 0) lostMarks[index] = lostMark else lostMarks.add(lostMark)
        sync()
    }

    override suspend fun saveLostMarks(lostMarks: List<LostMark>) {
        lostMarks.forEach { saveLostMark(it) }
    }

    override suspend fun updateRemediation(id: String, remediated: Boolean) {
        val index = lostMarks.indexOfFirst { it.id == id }
        if (index >= 0) {
            lostMarks[index] = lostMarks[index].copy(isRemediated = remediated)
            sync()
        }
    }

    override suspend fun deleteLostMark(lostMark: LostMark) {
        lostMarks.removeAll { it.id == lostMark.id }
        sync()
    }
}

private class TestMistakeRepository : MistakeRepository {
    val mistakes = mutableListOf<Mistake>()
    private val flow = MutableStateFlow<List<Mistake>>(emptyList())

    private fun sync() {
        flow.value = mistakes.toList()
    }

    override fun observeMistakesForChapter(chapterId: String): Flow<List<Mistake>> =
        flow.map { list -> list.filter { it.chapterId == chapterId } }

    override fun observeMistakesForSubject(subjectId: String): Flow<List<Mistake>> =
        flow.map { list -> list.filter { it.subjectId == subjectId } }

    override fun observeAllMistakes(): Flow<List<Mistake>> = flow

    override suspend fun recordMistake(mistake: Mistake): Mistake {
        val index = mistakes.indexOfFirst { it.id == mistake.id }
        if (index >= 0) mistakes[index] = mistake else mistakes.add(mistake)
        sync()
        return mistake
    }

    override suspend fun resolveMistake(id: String) {
        val index = mistakes.indexOfFirst { it.id == id }
        if (index >= 0) {
            mistakes[index] = mistakes[index].copy(isResolved = true)
            sync()
        }
    }

    override suspend fun deleteMistake(id: String) {
        mistakes.removeAll { it.id == id }
        sync()
    }

    override fun searchMistakes(query: String): Flow<List<Mistake>> =
        flow.map { list -> list.filter { it.question.contains(query, ignoreCase = true) } }
}

private class TestExamRepository : ExamRepository {
    val exams = mutableListOf<Exam>()
    var updatedScoreId: String? = null
    var updatedScoreValue: Int? = null
    var updatedScoreCompleted: Boolean? = null

    override fun observeAllExams(): Flow<List<Exam>> = MutableStateFlow(exams.toList())
    override fun getExamById(id: String): Flow<Exam?> = MutableStateFlow(exams.find { it.id == id })
    override suspend fun getExamByIdOnce(id: String): Exam? = exams.find { it.id == id }
    override suspend fun saveExam(exam: Exam, subjectIds: List<String>): Exam {
        exams.add(exam)
        return exam
    }
    override suspend fun deleteExam(id: String) {
        exams.removeAll { it.id == id }
    }
    override suspend fun updateExamScore(id: String, actualScore: Int?, isCompleted: Boolean) {
        updatedScoreId = id
        updatedScoreValue = actualScore
        updatedScoreCompleted = isCompleted
        val index = exams.indexOfFirst { it.id == id }
        if (index >= 0) {
            exams[index] = exams[index].copy(actualScore = actualScore, isCompleted = isCompleted)
        }
    }
}

private class TestPaperRepository : PaperRepository {
    val papers = mutableListOf<Paper>()
    var updatedPaperStatus: Pair<String, PaperStatus>? = null

    override fun getPapersForSubject(subjectId: String): Flow<List<Paper>> = MutableStateFlow(emptyList())
    override fun getAllPapers(): Flow<List<Paper>> = MutableStateFlow(papers.toList())
    override suspend fun getPaperById(id: String): Paper? = papers.find { it.id == id }
    override fun observePaperById(id: String): Flow<Paper?> = MutableStateFlow(papers.find { it.id == id })
    override suspend fun savePaper(paper: Paper, questions: List<PaperQuestionItem>) {
        papers.add(paper)
    }
    override suspend fun getQuestionsForPaper(paperId: String): List<PaperQuestionItem> = emptyList()
    override fun observeQuestionsForPaper(paperId: String): Flow<List<PaperQuestionItem>> = MutableStateFlow(emptyList())
    override suspend fun updatePaperStatus(id: String, status: PaperStatus) {
        updatedPaperStatus = Pair(id, status)
        val index = papers.indexOfFirst { it.id == id }
        if (index >= 0) {
            papers[index] = papers[index].copy(status = status)
        }
    }
    override suspend fun updatePaperPdfUri(id: String, pdfUri: String) {}
    override suspend fun deletePaper(paper: Paper) {
        papers.removeAll { it.id == paper.id }
    }
}

class ExamResultUseCasesTest {

    private lateinit var examResultRepo: TestExamResultRepository
    private lateinit var lostMarksRepo: TestLostMarksRepository
    private lateinit var mistakeRepo: TestMistakeRepository
    private lateinit var examRepo: TestExamRepository
    private lateinit var paperRepo: TestPaperRepository

    private lateinit var recordExamResultUseCase: RecordExamResultUseCase
    private lateinit var getExamDiagnosticUseCase: GetExamDiagnosticUseCase

    @Before
    fun setUp() {
        examResultRepo = TestExamResultRepository()
        lostMarksRepo = TestLostMarksRepository()
        mistakeRepo = TestMistakeRepository()
        examRepo = TestExamRepository()
        paperRepo = TestPaperRepository()

        recordExamResultUseCase = RecordExamResultUseCase(
            examResultRepository = examResultRepo,
            lostMarksRepository = lostMarksRepo,
            mistakeRepository = mistakeRepo,
            examRepository = examRepo,
            paperRepository = paperRepo
        )

        getExamDiagnosticUseCase = GetExamDiagnosticUseCase(
            examResultRepository = examResultRepo,
            lostMarksRepository = lostMarksRepo
        )
    }

    @Test
    fun testRecordExamResult_basicSuccess() = runTest {
        val output = recordExamResultUseCase(
            paperId = "paper-1",
            marksObtained = 42.0,
            totalMarks = 50.0,
            timeTakenMinutes = 60,
            notesOrFeedback = "Good time management"
        )

        assertEquals("paper-1", output.result.paperId)
        assertEquals(42.0, output.result.marksObtained, 0.001)
        assertEquals(50.0, output.result.totalMarks, 0.001)
        assertEquals(84.0, output.result.percentage, 0.001)
        assertEquals(60, output.result.timeTakenMinutes)
        assertEquals("Good time management", output.result.notesOrFeedback)

        assertEquals(1, examResultRepo.results.size)
        assertEquals(output.result.id, examResultRepo.results[0].id)
        assertEquals(Pair("paper-1", PaperStatus.EVALUATED), paperRepo.updatedPaperStatus)
    }

    @Test
    fun testRecordExamResult_withLostMarksAndMistakeBankLoop() = runTest {
        val lostMarkInputs = listOf(
            LostMarkInput(
                questionId = "q-101",
                topicId = "topic-lenz",
                chapterId = "chap-emi",
                subjectId = "sub-physics",
                marksLost = 3.0,
                category = LossReasonCategory.CONCEPT_ERROR,
                reflection = "Confused induced emf direction with magnetic field direction",
                questionText = "State Lenz's law and explain its conservation of energy basis.",
                studentAnswer = "Current opposes field directly.",
                correctAnswer = "Current induces a magnetic field opposing change in flux."
            ),
            LostMarkInput(
                questionId = "q-102",
                topicId = "topic-calc",
                chapterId = "chap-emi",
                subjectId = "sub-physics",
                marksLost = 2.0,
                category = LossReasonCategory.CALCULATION_ERROR,
                reflection = "Forgot factor of 10^-3 when converting mA to A",
                questionText = "Calculate the induced current when R = 50 ohms.",
                studentAnswer = "0.4 A",
                correctAnswer = "0.0004 A"
            )
        )

        val output = recordExamResultUseCase(
            paperId = "paper-2",
            marksObtained = 45.0,
            totalMarks = 50.0,
            timeTakenMinutes = 55,
            lostMarks = lostMarkInputs
        )

        assertEquals(2, output.savedLostMarks.size)
        assertEquals(2, output.mistakesCreated)
        assertEquals(2, lostMarksRepo.lostMarks.size)
        assertEquals(2, mistakeRepo.mistakes.size)

        val conceptMistake = mistakeRepo.mistakes.find { it.question.contains("Lenz's law") }
        assertNotNull(conceptMistake)
        assertEquals("sub-physics", conceptMistake!!.subjectId)
        assertEquals("chap-emi", conceptMistake.chapterId)
        assertEquals("topic-lenz", conceptMistake.topic)
        assertEquals(1, conceptMistake.missedCount)
        assertEquals(false, conceptMistake.isResolved)
    }

    @Test
    fun testRecordExamResult_updatesLinkedExamScore() = runTest {
        val exam = Exam(
            id = "exam-cbse-midterm",
            name = "Physics Mid-Term",
            targetDate = System.currentTimeMillis() + 86400000L,
            targetScore = 95,
            isCompleted = false
        )
        examRepo.exams.add(exam)

        recordExamResultUseCase(
            paperId = "paper-cbse",
            marksObtained = 88.0,
            totalMarks = 100.0,
            timeTakenMinutes = 180,
            examId = "exam-cbse-midterm"
        )

        assertEquals("exam-cbse-midterm", examRepo.updatedScoreId)
        assertEquals(88, examRepo.updatedScoreValue)
        assertEquals(true, examRepo.updatedScoreCompleted)
    }

    @Test
    fun testRecordExamResult_clampingAndEdgeCases() = runTest {
        // Marks over total clamped
        val overResult = recordExamResultUseCase(
            paperId = "p-clamp",
            marksObtained = 110.0,
            totalMarks = 100.0,
            timeTakenMinutes = 45
        )
        assertEquals(100.0, overResult.result.marksObtained, 0.001)
        assertEquals(100.0, overResult.result.percentage, 0.001)

        // Negative marks clamped to 0
        val negResult = recordExamResultUseCase(
            paperId = "p-neg",
            marksObtained = -10.0,
            totalMarks = 50.0,
            timeTakenMinutes = 30
        )
        assertEquals(0.0, negResult.result.marksObtained, 0.001)
        assertEquals(0.0, negResult.result.percentage, 0.001)

        // Zero total marks fallback prevents NaN
        val zeroTotalResult = recordExamResultUseCase(
            paperId = "p-zero",
            marksObtained = 50.0,
            totalMarks = 0.0,
            timeTakenMinutes = 20
        )
        assertEquals(50.0, zeroTotalResult.result.percentage, 0.001)
    }

    @Test
    fun testGetExamDiagnostic_identifiesTopLossCategory() = runTest {
        val output = recordExamResultUseCase(
            paperId = "paper-diag",
            marksObtained = 80.0,
            totalMarks = 100.0,
            timeTakenMinutes = 90,
            lostMarks = listOf(
                LostMarkInput(marksLost = 12.0, category = LossReasonCategory.CONCEPT_ERROR),
                LostMarkInput(marksLost = 5.0, category = LossReasonCategory.CARELESS_MISTAKE),
                LostMarkInput(marksLost = 3.0, category = LossReasonCategory.TIME_SHORTAGE)
            )
        )

        val diagnostic = getExamDiagnosticUseCase.getDiagnosticForResult(output.result.id)
        assertNotNull(diagnostic)
        assertEquals(20.0, diagnostic!!.totalMarksLost, 0.001)
        assertEquals(LossReasonCategory.CONCEPT_ERROR, diagnostic.topLossCategory)
        assertTrue(diagnostic.recoveryRecommendation.contains("conceptual gaps detected"))
        assertEquals(3, diagnostic.lostMarksList.size)
    }

    @Test
    fun testGetExamDiagnostic_fallbackWhenNoExplicitLostMarks() = runTest {
        val output = recordExamResultUseCase(
            paperId = "paper-no-explicit",
            marksObtained = 72.0,
            totalMarks = 100.0,
            timeTakenMinutes = 90,
            lostMarks = emptyList()
        )

        val diagnostic = getExamDiagnosticUseCase.getDiagnosticForPaper("paper-no-explicit")
        assertNotNull(diagnostic)
        assertEquals(28.0, diagnostic!!.totalMarksLost, 0.001)
        assertNull(diagnostic.topLossCategory)
        assertTrue(diagnostic.recoveryRecommendation.contains("Review the model marking scheme"))
    }
}
