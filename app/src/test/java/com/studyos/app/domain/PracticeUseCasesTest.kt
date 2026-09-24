package com.studyos.app.domain

import com.studyos.app.domain.model.ActiveQuizState
import com.studyos.app.domain.model.Chapter
import com.studyos.app.domain.model.ChapterStatus
import com.studyos.app.domain.model.Exam
import com.studyos.app.domain.model.ExamRevisionCategory
import com.studyos.app.domain.model.Flashcard
import com.studyos.app.domain.model.FlashcardCategory
import com.studyos.app.domain.model.FlashcardDifficulty
import com.studyos.app.domain.model.FlashcardRating
import com.studyos.app.domain.model.FlashcardReview
import com.studyos.app.domain.model.Mistake
import com.studyos.app.domain.model.Note
import com.studyos.app.domain.model.QuestionResult
import com.studyos.app.domain.model.Quiz
import com.studyos.app.domain.model.QuizAttempt
import com.studyos.app.domain.model.QuizQuestion
import com.studyos.app.domain.model.SpacedRepetitionAlgorithm
import com.studyos.app.domain.model.Subject
import com.studyos.app.domain.model.SubjectWithProgress
import com.studyos.app.domain.repository.ChapterRepository
import com.studyos.app.domain.repository.ExamRepository
import com.studyos.app.domain.repository.FlashcardRepository
import com.studyos.app.domain.repository.MistakeRepository
import com.studyos.app.domain.repository.NoteRepository
import com.studyos.app.domain.repository.QuizRepository
import com.studyos.app.domain.repository.SubjectRepository
import com.studyos.app.domain.usecase.ConvertMistakeToFlashcardUseCase
import com.studyos.app.domain.usecase.DeleteFlashcardUseCase
import com.studyos.app.domain.usecase.DeleteNoteUseCase
import com.studyos.app.domain.usecase.GetExamDashboardUseCase
import com.studyos.app.domain.usecase.GetNotesForChapterUseCase
import com.studyos.app.domain.usecase.ReviewFlashcardUseCase
import com.studyos.app.domain.usecase.SaveFlashcardUseCase
import com.studyos.app.domain.usecase.SaveNoteUseCase
import com.studyos.app.domain.usecase.SubmitQuizAttemptUseCase
import com.studyos.app.domain.usecase.ToggleNotePinUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

class FakeNoteRepository : NoteRepository {
    val notes = mutableListOf<Note>()
    private val notesFlow = MutableStateFlow<List<Note>>(emptyList())

    private fun sync() {
        notesFlow.value = notes.sortedWith(compareByDescending<Note> { it.isPinned }.thenByDescending { it.updatedAt })
    }

    override fun observeNotesForChapter(chapterId: String): Flow<List<Note>> =
        notesFlow.map { list -> list.filter { it.chapterId == chapterId } }

    override fun observeNotesForSubject(subjectId: String): Flow<List<Note>> =
        notesFlow.map { list -> list.filter { it.subjectId == subjectId } }

    override fun observeAllNotes(): Flow<List<Note>> = notesFlow

    override fun getNoteById(id: String): Flow<Note?> =
        notesFlow.map { it.find { n -> n.id == id } }

    override suspend fun getNoteByIdOnce(id: String): Note? = notes.find { it.id == id }

    override suspend fun saveNote(note: Note): Note {
        notes.removeAll { it.id == note.id }
        notes.add(note)
        sync()
        return note
    }

    override suspend fun updateNote(note: Note) {
        saveNote(note)
    }

    override suspend fun deleteNote(id: String) {
        notes.removeAll { it.id == id }
        sync()
    }

    override suspend fun togglePin(id: String, isPinned: Boolean) {
        val idx = notes.indexOfFirst { it.id == id }
        if (idx != -1) {
            notes[idx] = notes[idx].copy(isPinned = isPinned)
            sync()
        }
    }

    override fun searchNotes(query: String): Flow<List<Note>> =
        notesFlow.map { list -> list.filter { it.title.contains(query, true) || it.content.contains(query, true) } }
}

class FakeFlashcardRepository : FlashcardRepository {
    val cards = mutableListOf<Flashcard>()
    val reviews = mutableListOf<FlashcardReview>()
    private val cardsFlow = MutableStateFlow<List<Flashcard>>(emptyList())

    private fun sync() {
        cardsFlow.value = cards.toList()
    }

    override fun observeFlashcardsForChapter(chapterId: String): Flow<List<Flashcard>> =
        cardsFlow.map { list -> list.filter { it.chapterId == chapterId } }

    override fun observeFlashcardsForSubject(subjectId: String): Flow<List<Flashcard>> =
        cardsFlow.map { list -> list.filter { it.subjectId == subjectId } }

    override fun observeDueFlashcards(currentTime: Long): Flow<List<Flashcard>> =
        cardsFlow.map { list -> list.filter { it.getCategory(currentTime) == FlashcardCategory.DUE } }

    override fun observeAllFlashcards(): Flow<List<Flashcard>> = cardsFlow

    override fun getFlashcardById(id: String): Flow<Flashcard?> =
        cardsFlow.map { it.find { c -> c.id == id } }

    override suspend fun getFlashcardByIdOnce(id: String): Flashcard? = cards.find { it.id == id }

    override suspend fun saveFlashcard(flashcard: Flashcard): Flashcard {
        cards.removeAll { it.id == flashcard.id }
        cards.add(flashcard)
        sync()
        return flashcard
    }

    override suspend fun saveFlashcards(flashcards: List<Flashcard>) {
        cards.removeAll { c -> flashcards.any { it.id == c.id } }
        cards.addAll(flashcards)
        sync()
    }

    override suspend fun recordReview(cardId: String, rating: FlashcardRating): Flashcard {
        val card = cards.find { it.id == cardId } ?: throw IllegalArgumentException("Card not found")
        val updated = SpacedRepetitionAlgorithm.calculateNextReview(card, rating)
        cards.removeAll { it.id == cardId }
        cards.add(updated)
        reviews.add(
            FlashcardReview(
                flashcardId = cardId,
                rating = rating,
                intervalAfterDays = updated.intervalDays
            )
        )
        sync()
        return updated
    }

    override suspend fun deleteFlashcard(id: String) {
        cards.removeAll { it.id == id }
        sync()
    }

    override fun searchFlashcards(query: String): Flow<List<Flashcard>> =
        cardsFlow.map { list -> list.filter { it.question.contains(query, true) || it.answer.contains(query, true) } }
}

class FakeQuizRepository : QuizRepository {
    val quizzes = mutableListOf<Quiz>()
    val questions = mutableListOf<QuizQuestion>()
    val attempts = mutableListOf<QuizAttempt>()
    val activeStates = mutableMapOf<String, ActiveQuizState>()
    private val attemptsFlow = MutableStateFlow<List<QuizAttempt>>(emptyList())

    override fun observeQuizzesForChapter(chapterId: String): Flow<List<Quiz>> =
        flowOf(quizzes.filter { it.chapterId == chapterId })

    override fun observeQuizzesForSubject(subjectId: String): Flow<List<Quiz>> =
        flowOf(quizzes.filter { it.subjectId == subjectId })

    override suspend fun saveQuiz(quiz: Quiz, questions: List<QuizQuestion>): Quiz {
        quizzes.removeAll { it.id == quiz.id }
        quizzes.add(quiz)
        this.questions.removeAll { it.quizId == quiz.id }
        this.questions.addAll(questions)
        return quiz
    }

    override suspend fun getQuizById(id: String): Quiz? = quizzes.find { it.id == id }

    override suspend fun getQuestionsForQuiz(quizId: String): List<QuizQuestion> =
        questions.filter { it.quizId == quizId }

    override suspend fun deleteQuiz(quizId: String) {
        quizzes.removeAll { it.id == quizId }
    }

    override suspend fun recordAttempt(attempt: QuizAttempt, results: List<QuestionResult>): QuizAttempt {
        attempts.add(attempt)
        attemptsFlow.value = attempts.toList()
        return attempt
    }

    override fun getAttemptsForQuiz(quizId: String): Flow<List<QuizAttempt>> =
        attemptsFlow.map { list -> list.filter { it.quizId == quizId } }

    override fun getAttemptsForChapter(chapterId: String): Flow<List<QuizAttempt>> =
        attemptsFlow.map { list -> list.filter { it.chapterId == chapterId } }

    override fun getAllAttempts(): Flow<List<QuizAttempt>> = attemptsFlow

    override suspend fun getAttemptResults(attemptId: String): List<QuestionResult> = emptyList()

    override suspend fun saveActiveState(state: ActiveQuizState) {
        activeStates[state.quizId] = state
    }

    override suspend fun getActiveState(quizId: String): ActiveQuizState? = activeStates[quizId]

    override suspend fun clearActiveState(quizId: String) {
        activeStates.remove(quizId)
    }
}

class FakeMistakeRepository : MistakeRepository {
    val mistakes = mutableListOf<Mistake>()
    private val mistakesFlow = MutableStateFlow<List<Mistake>>(emptyList())

    private fun sync() {
        mistakesFlow.value = mistakes.toList()
    }

    override fun observeMistakesForChapter(chapterId: String): Flow<List<Mistake>> =
        mistakesFlow.map { list -> list.filter { it.chapterId == chapterId } }

    override fun observeMistakesForSubject(subjectId: String): Flow<List<Mistake>> =
        mistakesFlow.map { list -> list.filter { it.subjectId == subjectId } }

    override fun observeAllMistakes(): Flow<List<Mistake>> = mistakesFlow

    override suspend fun recordMistake(mistake: Mistake): Mistake {
        val existing = mistakes.find { it.question == mistake.question && it.chapterId == mistake.chapterId }
        if (existing != null) {
            val updated = existing.copy(
                missedCount = existing.missedCount + 1,
                lastMissedAt = System.currentTimeMillis(),
                isResolved = false
            )
            mistakes.removeAll { it.id == existing.id }
            mistakes.add(updated)
            sync()
            return updated
        } else {
            mistakes.add(mistake)
            sync()
            return mistake
        }
    }

    override suspend fun resolveMistake(id: String) {
        val idx = mistakes.indexOfFirst { it.id == id }
        if (idx != -1) {
            mistakes[idx] = mistakes[idx].copy(isResolved = true)
            sync()
        }
    }

    override suspend fun deleteMistake(id: String) {
        mistakes.removeAll { it.id == id }
        sync()
    }

    override fun searchMistakes(query: String): Flow<List<Mistake>> =
        mistakesFlow.map { list -> list.filter { it.question.contains(query, true) } }
}

class FakeExamRepository : ExamRepository {
    val exams = mutableListOf<Exam>()
    private val examsFlow = MutableStateFlow<List<Exam>>(emptyList())

    private fun sync() {
        examsFlow.value = exams.sortedBy { it.targetDate }
    }

    override fun observeAllExams(): Flow<List<Exam>> = examsFlow

    override fun getExamById(id: String): Flow<Exam?> =
        examsFlow.map { it.find { e -> e.id == id } }

    override suspend fun getExamByIdOnce(id: String): Exam? = exams.find { it.id == id }

    override suspend fun saveExam(exam: Exam, subjectIds: List<String>): Exam {
        exams.removeAll { it.id == exam.id }
        exams.add(exam.copy(subjectIds = subjectIds))
        sync()
        return exam
    }

    override suspend fun deleteExam(id: String) {
        exams.removeAll { it.id == id }
        sync()
    }

    override suspend fun updateExamScore(id: String, actualScore: Int?, isCompleted: Boolean) {
        val index = exams.indexOfFirst { it.id == id }
        if (index != -1) {
            exams[index] = exams[index].copy(actualScore = actualScore, isCompleted = isCompleted)
            sync()
        }
    }
}

class FakeSubjectRepositoryForPractice : SubjectRepository {
    val subjects = mutableListOf<Subject>()
    private val flow = MutableStateFlow<List<Subject>>(emptyList())

    override fun getAllSubjects(): Flow<List<Subject>> = flow
    override fun observeSubjects(): Flow<List<Subject>> = flow
    override fun observeSubjectsWithProgress(): Flow<List<SubjectWithProgress>> = flowOf(emptyList())
    override suspend fun getAllSubjectsOnce(): List<Subject> = subjects.toList()
    override fun getSubjectById(id: String): Flow<Subject?> = flow.map { it.find { s -> s.id == id } }
    override suspend fun getSubjectByIdOnce(id: String): Subject? = subjects.find { it.id == id }
    override suspend fun findByName(name: String): Subject? = subjects.find { it.name == name }
    override suspend fun saveSubject(subject: Subject) { subjects.add(subject); flow.value = subjects.toList() }
    override suspend fun saveSubjects(subjects: List<Subject>) { this.subjects.addAll(subjects); flow.value = this.subjects.toList() }
    override suspend fun updateSubject(subject: Subject) {}
    override suspend fun deleteSubject(subject: Subject) {}
    override suspend fun deleteSubjectById(id: String) { subjects.removeAll { it.id == id }; flow.value = subjects.toList() }
}

class FakeChapterRepositoryForPractice : ChapterRepository {
    val chapters = mutableListOf<Chapter>()
    private val flow = MutableStateFlow<List<Chapter>>(emptyList())

    override fun observeChaptersForSubject(subjectId: String): Flow<List<Chapter>> =
        flow.map { list -> list.filter { it.subjectId == subjectId } }
    override suspend fun getChaptersForSubjectOnce(subjectId: String): List<Chapter> =
        chapters.filter { it.subjectId == subjectId }
    override fun observeAllChapters(): Flow<List<Chapter>> = flow
    override suspend fun getAllChaptersOnce(): List<Chapter> = chapters.toList()
    override fun getChapter(id: String): Flow<Chapter?> = flow.map { it.find { c -> c.id == id } }
    override suspend fun getChapterById(id: String): Chapter? = chapters.find { it.id == id }
    override suspend fun addChapter(subjectId: String, name: String, description: String?): Chapter {
        val chap = Chapter(subjectId = subjectId, name = name, description = description)
        chapters.add(chap)
        flow.value = chapters.toList()
        return chap
    }
    override suspend fun updateChapter(chapter: Chapter) {}
    override suspend fun updateChapterProgress(id: String, progress: Int) {}
    override suspend fun updateChapterStatus(id: String, status: ChapterStatus) {}
    override suspend fun deleteChapter(id: String) {}
    override suspend fun reorderChapters(subjectId: String, chapters: List<Chapter>) {}
    override suspend fun moveChapter(subjectId: String, chapterId: String, moveUp: Boolean) {}
    override fun observeMostRecentChapter(): Flow<Chapter?> = flow.map { it.firstOrNull() }
    override suspend fun recordChapterOpened(id: String) {}
}

class PracticeUseCasesTest {

    private lateinit var noteRepo: FakeNoteRepository
    private lateinit var flashcardRepo: FakeFlashcardRepository
    private lateinit var quizRepo: FakeQuizRepository
    private lateinit var mistakeRepo: FakeMistakeRepository
    private lateinit var examRepo: FakeExamRepository
    private lateinit var subjectRepo: FakeSubjectRepositoryForPractice
    private lateinit var chapterRepo: FakeChapterRepositoryForPractice

    @Before
    fun setup() {
        noteRepo = FakeNoteRepository()
        flashcardRepo = FakeFlashcardRepository()
        quizRepo = FakeQuizRepository()
        mistakeRepo = FakeMistakeRepository()
        examRepo = FakeExamRepository()
        subjectRepo = FakeSubjectRepositoryForPractice()
        chapterRepo = FakeChapterRepositoryForPractice()
    }

    @Test
    fun `SpacedRepetitionAlgorithm calculates intervals and ease factor correctly`() {
        val card = Flashcard(
            question = "What is DNA?",
            answer = "Genetic material",
            subjectId = "sub1",
            intervalDays = 0,
            easeFactor = 2.5f
        )

        // Rating AGAIN resets interval to 1 and drops ease
        val afterAgain = SpacedRepetitionAlgorithm.calculateNextReview(card, FlashcardRating.AGAIN)
        assertEquals(1, afterAgain.intervalDays)
        assertEquals(2.3f, afterAgain.easeFactor, 0.01f)

        // Rating GOOD starts standard progression: 0 -> 1 -> 6 -> interval * ease
        val afterGood1 = SpacedRepetitionAlgorithm.calculateNextReview(card, FlashcardRating.GOOD)
        assertEquals(1, afterGood1.intervalDays)

        val card1Day = card.copy(intervalDays = 1)
        val afterGood2 = SpacedRepetitionAlgorithm.calculateNextReview(card1Day, FlashcardRating.GOOD)
        assertEquals(6, afterGood2.intervalDays)

        // Rating EASY gives bonus
        val card6Days = card.copy(intervalDays = 6, easeFactor = 2.5f)
        val afterEasy = SpacedRepetitionAlgorithm.calculateNextReview(card6Days, FlashcardRating.EASY)
        assertTrue(afterEasy.intervalDays > 6 * 2.5f)
        assertTrue(afterEasy.easeFactor > 2.5f)
    }

    @Test
    fun `Note CRUD and pinning work correctly`() = runTest {
        val saveNote = SaveNoteUseCase(noteRepo)
        val getNotes = GetNotesForChapterUseCase(noteRepo)
        val togglePin = ToggleNotePinUseCase(noteRepo)
        val deleteNote = DeleteNoteUseCase(noteRepo)

        val note = saveNote(
            id = null,
            title = "Photosynthesis",
            content = "Light-dependent reactions",
            subjectId = "sub1",
            chapterId = "chap1",
            isPinned = false
        )

        var list = getNotes("chap1").first()
        assertEquals(1, list.size)
        assertEquals("Photosynthesis", list[0].title)
        assertFalse(list[0].isPinned)

        // Pin note
        togglePin(note.id, true)
        list = getNotes("chap1").first()
        assertTrue(list[0].isPinned)

        // Delete note
        deleteNote(note.id)
        list = getNotes("chap1").first()
        assertTrue(list.isEmpty())
    }

    @Test
    fun `SubmitQuizAttempt records quiz attempt and automatically saves mistakes`() = runTest {
        val submitQuiz = SubmitQuizAttemptUseCase(quizRepo, mistakeRepo)

        val questions = listOf(
            QuizQuestion(
                id = "q1",
                quizId = "quiz1",
                question = "What is H2O?",
                options = listOf("Water", "Carbon", "Oxygen"),
                correctAnswer = "Water"
            ),
            QuizQuestion(
                id = "q2",
                quizId = "quiz1",
                question = "What is the speed of light?",
                options = listOf("3x10^8 m/s", "100 m/s"),
                correctAnswer = "3x10^8 m/s"
            )
        )

        val answers = mapOf(
            "q1" to "Water",       // correct
            "q2" to "100 m/s"      // incorrect!
        )

        val attempt = submitQuiz(
            quizId = "quiz1",
            subjectId = "sub1",
            chapterId = "chap1",
            startedAt = System.currentTimeMillis() - 60_000,
            timeSpentSeconds = 60,
            answers = answers,
            questions = questions
        )

        assertEquals(1, attempt.score)
        assertEquals(2, attempt.totalQuestions)
        assertEquals(50, attempt.accuracyPercentage)

        // Verify mistake was automatically recorded into Mistake Bank!
        val mistakes = mistakeRepo.observeAllMistakes().first()
        assertEquals(1, mistakes.size)
        assertEquals("What is the speed of light?", mistakes[0].question)
        assertEquals("100 m/s", mistakes[0].studentAnswer)
        assertEquals("3x10^8 m/s", mistakes[0].correctAnswer)
        assertFalse(mistakes[0].isResolved)
    }

    @Test
    fun `ConvertMistakeToFlashcard creates flashcard and marks mistake resolved`() = runTest {
        val convertMistake = ConvertMistakeToFlashcardUseCase(mistakeRepo, flashcardRepo)

        val mistake = mistakeRepo.recordMistake(
            Mistake(
                question = "What is ATP?",
                studentAnswer = "Protein",
                correctAnswer = "Adenosine triphosphate, energy carrier",
                explanation = "ATP stores energy for cellular work",
                subjectId = "sub1",
                chapterId = "chap1"
            )
        )

        val flashcard = convertMistake(mistake)

        assertNotNull(flashcard)
        assertEquals("What is ATP?", flashcard.question)
        assertTrue(flashcard.answer.contains("Adenosine triphosphate"))

        // Mistake must now be resolved
        val mistakes = mistakeRepo.observeAllMistakes().first()
        assertTrue(mistakes[0].isResolved)
    }

    @Test
    fun `Exam Dashboard categorizes chapters by revision priority`() = runTest {
        val subject = Subject(id = "sub1", name = "Biology")
        subjectRepo.saveSubject(subject)

        val chapNeedRevision = Chapter(id = "c1", subjectId = "sub1", name = "Cells", progress = 30)
        val chapStrong = Chapter(id = "c2", subjectId = "sub1", name = "Genetics", progress = 95)
        chapterRepo.chapters.addAll(listOf(chapNeedRevision, chapStrong))

        val exam = Exam(
            id = "exam1",
            name = "Biology Final",
            targetDate = System.currentTimeMillis() + (7 * 86_400_000L),
            subjectIds = listOf("sub1")
        )
        examRepo.saveExam(exam, listOf("sub1"))

        val dashboardUseCase = GetExamDashboardUseCase(
            examRepository = examRepo,
            subjectRepository = subjectRepo,
            chapterRepository = chapterRepo,
            flashcardRepository = flashcardRepo,
            noteRepository = noteRepo,
            quizRepository = quizRepo,
            mistakeRepository = mistakeRepo
        )

        val dashboard = dashboardUseCase("exam1")
        assertNotNull(dashboard)
        assertTrue(dashboard!!.daysRemaining in 6L..7L)

        val revisionChapters = dashboardUseCase.getRevisionChapters("exam1")
        assertEquals(2, revisionChapters.size)

        val c1Item = revisionChapters.find { it.chapter.id == "c1" }
        assertEquals(ExamRevisionCategory.NEED_REVISION, c1Item?.category)

        val c2Item = revisionChapters.find { it.chapter.id == "c2" }
        assertEquals(ExamRevisionCategory.STRONG, c2Item?.category)
    }
}
