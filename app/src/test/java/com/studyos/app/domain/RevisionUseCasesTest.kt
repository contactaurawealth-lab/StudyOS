package com.studyos.app.domain

import com.studyos.app.core.datastore.PreferencesDataSource
import com.studyos.app.core.model.AppState
import com.studyos.app.core.model.AppTheme
import com.studyos.app.domain.model.ActiveQuizState
import com.studyos.app.domain.model.ActiveRecallItem
import com.studyos.app.domain.model.ActiveRecallItemType
import com.studyos.app.domain.model.ActiveRecallSessionSummary
import com.studyos.app.domain.model.ActiveRecallSessionType
import com.studyos.app.domain.model.AiConfig
import com.studyos.app.domain.model.AiMessage
import com.studyos.app.domain.model.AiStreamChunk
import com.studyos.app.domain.model.AiTutorMode
import com.studyos.app.domain.model.Chapter
import com.studyos.app.domain.model.ChapterAiContext
import com.studyos.app.domain.model.ChapterMasteryLevel
import com.studyos.app.domain.model.ChapterStatus
import com.studyos.app.domain.model.Flashcard
import com.studyos.app.domain.model.FlashcardDifficulty
import com.studyos.app.domain.model.FlashcardRating
import com.studyos.app.domain.model.Mistake
import com.studyos.app.domain.model.Note
import com.studyos.app.domain.model.PracticeDifficulty
import com.studyos.app.domain.model.Quiz
import com.studyos.app.domain.model.QuizAttempt
import com.studyos.app.domain.model.QuizDifficulty
import com.studyos.app.domain.model.QuizQuestion
import com.studyos.app.domain.model.RevisionSchedule
import com.studyos.app.domain.model.StudyContext
import com.studyos.app.domain.model.Subject
import com.studyos.app.domain.model.SubjectWithProgress
import com.studyos.app.domain.provider.AiProvider
import com.studyos.app.domain.repository.ChapterRepository
import com.studyos.app.domain.repository.FlashcardRepository
import com.studyos.app.domain.repository.MistakeRepository
import com.studyos.app.domain.repository.NoteRepository
import com.studyos.app.domain.repository.QuizRepository
import com.studyos.app.domain.repository.RevisionRepository
import com.studyos.app.domain.repository.SubjectRepository
import com.studyos.app.domain.usecase.AiStudyEngineUseCase
import com.studyos.app.domain.usecase.CompleteActiveRecallSessionUseCase
import com.studyos.app.domain.usecase.GenerateRevisionRecommendationsUseCase
import com.studyos.app.domain.usecase.GetChapterAiContextUseCase
import com.studyos.app.domain.usecase.GetChapterMasteryUseCase
import com.studyos.app.domain.usecase.GetDueRevisionDashboardUseCase
import com.studyos.app.domain.usecase.StartActiveRecallSessionUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

// FAKE IMPLEMENTATIONS FOR REVISION TESTS

class FakeRevisionRepository : RevisionRepository {
    val schedules = mutableMapOf<String, RevisionSchedule>()
    val recallLogs = mutableListOf<ActiveRecallSessionSummary>()
    var streak = 3
    var targetMinutes = 15
    var minutesToday = 10

    override fun observeDueSchedules(currentTime: Long): Flow<List<RevisionSchedule>> {
        return flowOf(schedules.values.filter { it.nextRevisionDue <= currentTime })
    }

    override fun observeAllSchedules(): Flow<List<RevisionSchedule>> {
        return flowOf(schedules.values.toList())
    }

    override fun observeScheduleForChapter(chapterId: String): Flow<RevisionSchedule?> {
        return flowOf(schedules[chapterId])
    }

    override suspend fun getScheduleForChapter(chapterId: String): RevisionSchedule? {
        return schedules[chapterId]
    }

    override suspend fun saveSchedule(schedule: RevisionSchedule) {
        schedules[schedule.chapterId] = schedule
    }

    override suspend fun recordChapterRevisionCompleted(chapterId: String, subjectId: String, qualityScore: Int) {
        val existing = schedules[chapterId]
        val now = System.currentTimeMillis()
        if (existing != null) {
            val nextInterval = if (qualityScore >= 3) existing.intervalDays * 2 else 1
            schedules[chapterId] = existing.copy(
                intervalDays = nextInterval,
                revisionCount = existing.revisionCount + 1,
                lastRevisedAt = now,
                nextRevisionDue = now + nextInterval * 86_400_000L
            )
        } else {
            schedules[chapterId] = RevisionSchedule(
                chapterId = chapterId,
                subjectId = subjectId,
                intervalDays = 3,
                revisionCount = 1,
                lastRevisedAt = now,
                nextRevisionDue = now + 3 * 86_400_000L
            )
        }
    }

    override suspend fun recordRecallSession(summary: ActiveRecallSessionSummary): Int {
        recallLogs.add(summary)
        streak += 1
        return streak
    }

    override fun observeMinutesRevisedToday(): Flow<Int> = flowOf(minutesToday)
    override val revisionStreakDays: Flow<Int> = flowOf(streak)
    override val dailyRevisionTargetMinutes: Flow<Int> = flowOf(targetMinutes)

    override suspend fun setDailyRevisionTargetMinutes(minutes: Int) {
        targetMinutes = minutes
    }
}

private class FakeRevisionPreferencesDataSource : PreferencesDataSource {
    override val appState: Flow<AppState> = flowOf(AppState())
    override val themePreference: Flow<AppTheme> = flowOf(AppTheme.SYSTEM)
    override val isOnboardingCompleted: Flow<Boolean> = flowOf(true)
    override val aiConfig: Flow<AiConfig> = flowOf(AiConfig())
    override val revisionStreakDays: Flow<Int> = flowOf(5)
    override val dailyRevisionTargetMinutes: Flow<Int> = flowOf(15)
    override val lastRevisionEpochDay: Flow<Long> = flowOf(100L)
    override val studyRemindersEnabled: Flow<Boolean> = flowOf(true)
    override val dailyReminderEnabled: Flow<Boolean> = flowOf(true)
    override val dailyReminderTime: Flow<String> = flowOf("19:00")
    override val revisionRemindersEnabled: Flow<Boolean> = flowOf(true)
    override suspend fun setThemePreference(theme: AppTheme) {}
    override suspend fun setOnboardingCompleted(completed: Boolean) {}
    override suspend fun resetOnboarding() {}
    override suspend fun saveAiConfig(config: AiConfig) {}
    override suspend fun updateRevisionStreak(todayEpochDay: Long): Int = 6
    override suspend fun setDailyRevisionTargetMinutes(minutes: Int) {}
    override suspend fun setStudyRemindersEnabled(enabled: Boolean) {}
    override suspend fun setDailyReminderEnabled(enabled: Boolean) {}
    override suspend fun setDailyReminderTime(time: String) {}
    override suspend fun setRevisionRemindersEnabled(enabled: Boolean) {}
}

private class FakeRevisionAiProvider : AiProvider {
    override fun generateStream(
        messages: List<AiMessage>,
        studyContext: StudyContext?,
        config: AiConfig
    ): Flow<AiStreamChunk> = flowOf(
        AiStreamChunk.Content("Test response delta"),
        AiStreamChunk.Done
    )
}

class RevisionUseCasesTest {

    private lateinit var fakeChapterRepo: FakeChapterRepositoryForPractice
    private lateinit var fakeSubjectRepo: FakeSubjectRepositoryForPractice
    private lateinit var fakeQuizRepo: FakeQuizRepository
    private lateinit var fakeFlashcardRepo: FakeFlashcardRepository
    private lateinit var fakeMistakeRepo: FakeMistakeRepository
    private lateinit var fakeNoteRepo: FakeNoteRepository
    private lateinit var fakeRevisionRepo: FakeRevisionRepository
    private lateinit var fakePreferences: FakeRevisionPreferencesDataSource
    private lateinit var fakeAiProvider: FakeRevisionAiProvider

    private lateinit var getChapterMasteryUseCase: GetChapterMasteryUseCase
    private lateinit var generateRevisionRecommendationsUseCase: GenerateRevisionRecommendationsUseCase
    private lateinit var startActiveRecallSessionUseCase: StartActiveRecallSessionUseCase
    private lateinit var completeActiveRecallSessionUseCase: CompleteActiveRecallSessionUseCase
    private lateinit var getChapterAiContextUseCase: GetChapterAiContextUseCase
    private lateinit var aiStudyEngineUseCase: AiStudyEngineUseCase

    @Before
    fun setUp() {
        fakeChapterRepo = FakeChapterRepositoryForPractice()
        fakeSubjectRepo = FakeSubjectRepositoryForPractice()
        fakeQuizRepo = FakeQuizRepository()
        fakeFlashcardRepo = FakeFlashcardRepository()
        fakeMistakeRepo = FakeMistakeRepository()
        fakeNoteRepo = FakeNoteRepository()
        fakeRevisionRepo = FakeRevisionRepository()
        fakePreferences = FakeRevisionPreferencesDataSource()
        fakeAiProvider = FakeRevisionAiProvider()

        getChapterMasteryUseCase = GetChapterMasteryUseCase(
            fakeChapterRepo,
            fakeSubjectRepo,
            fakeQuizRepo,
            fakeFlashcardRepo,
            fakeMistakeRepo,
            fakeRevisionRepo
        )

        generateRevisionRecommendationsUseCase = GenerateRevisionRecommendationsUseCase(
            fakeChapterRepo,
            fakeSubjectRepo,
            fakeMistakeRepo,
            fakeQuizRepo,
            fakeRevisionRepo
        )

        startActiveRecallSessionUseCase = StartActiveRecallSessionUseCase(
            fakeFlashcardRepo,
            fakeMistakeRepo,
            fakeQuizRepo,
            fakeChapterRepo
        )

        completeActiveRecallSessionUseCase = CompleteActiveRecallSessionUseCase(
            fakeRevisionRepo,
            fakeFlashcardRepo,
            fakeMistakeRepo
        )

        getChapterAiContextUseCase = GetChapterAiContextUseCase(
            fakeChapterRepo,
            fakeSubjectRepo,
            fakeNoteRepo,
            fakeMistakeRepo,
            fakeQuizRepo,
            getChapterMasteryUseCase
        )

        aiStudyEngineUseCase = AiStudyEngineUseCase(
            fakeAiProvider,
            fakePreferences
        )
    }

    @Test
    fun chapterMasteryCalculation_calculatesCorrectLevelAndScore() = runTest {
        val subject = Subject(id = "sub-1", name = "Physics")
        fakeSubjectRepo.subjects.add(subject)

        val chapter = Chapter(
            id = "ch-1",
            subjectId = "sub-1",
            name = "Kinematics",
            orderIndex = 0,
            progress = 100
        )
        fakeChapterRepo.chapters.add(chapter)

        // Perfect quiz attempt
        fakeQuizRepo.recordAttempt(
            QuizAttempt(
                id = "att-1",
                quizId = "q-1",
                subjectId = "sub-1",
                chapterId = "ch-1",
                score = 5,
                totalQuestions = 5,
                accuracyPercentage = 100,
                isCompleted = true
            ),
            emptyList()
        )

        // Mastered flashcard
        fakeFlashcardRepo.saveFlashcard(
            Flashcard(
                id = "fc-1",
                subjectId = "sub-1",
                chapterId = "ch-1",
                question = "What is velocity?",
                answer = "Displacement per time",
                reviewCount = 3,
                easeFactor = 2.5f
            )
        )

        // Resolved mistake
        fakeMistakeRepo.recordMistake(
            Mistake(
                id = "mis-1",
                question = "Scalar vs vector",
                studentAnswer = "Scalar",
                correctAnswer = "Vector",
                subjectId = "sub-1",
                chapterId = "ch-1",
                isResolved = false
            )
        )
        fakeMistakeRepo.resolveMistake("mis-1")

        val mastery = getChapterMasteryUseCase.getForChapter("ch-1")
        assertNotNull(mastery)
        assertEquals(100, mastery!!.masteryPercentage)
        assertEquals(ChapterMasteryLevel.MASTERED, mastery.level)
    }

    @Test
    fun activeRecallSession_generatesCorrectItemQueues() = runTest {
        val sub = Subject(id = "sub-1", name = "Biology")
        fakeSubjectRepo.subjects.add(sub)

        fakeFlashcardRepo.saveFlashcard(
            Flashcard(
                id = "fc-bio-1",
                subjectId = "sub-1",
                chapterId = "ch-cell",
                question = "Mitochondria role?",
                answer = "Powerhouse of cell"
            )
        )

        fakeMistakeRepo.recordMistake(
            Mistake(
                id = "mis-bio-1",
                question = "Osmosis direction?",
                studentAnswer = "Against gradient",
                correctAnswer = "Along gradient",
                subjectId = "sub-1",
                chapterId = "ch-cell",
                isResolved = false
            )
        )

        // 1. Quick 5-min session tests flashcards
        val quickItems = startActiveRecallSessionUseCase(ActiveRecallSessionType.QUICK_5)
        assertTrue(quickItems.any { it.type == ActiveRecallItemType.FLASHCARD })

        // 2. Focused 10-min session tests mistakes
        val focusedItems = startActiveRecallSessionUseCase(ActiveRecallSessionType.FOCUSED_10)
        assertTrue(focusedItems.any { it.type == ActiveRecallItemType.MISTAKE_RETRY })

        // 3. Deep 15-min session tests combined items
        val deepItems = startActiveRecallSessionUseCase(ActiveRecallSessionType.DEEP_15)
        assertTrue(deepItems.isNotEmpty())
    }

    @Test
    fun completeActiveRecallSession_updatesStreakAndSchedules() = runTest {
        val item = ActiveRecallItem(
            id = "test-item-1",
            type = ActiveRecallItemType.FLASHCARD,
            title = "Test Card",
            prompt = "What is photosynthesis?",
            answer = "Light to energy",
            subjectId = "sub-bio",
            chapterId = "ch-bio-1",
            sourceReferenceId = "card-1"
        )

        fakeFlashcardRepo.saveFlashcard(
            Flashcard(
                id = "card-1",
                subjectId = "sub-bio",
                chapterId = "ch-bio-1",
                question = "What is photosynthesis?",
                answer = "Light to energy"
            )
        )

        val summary = completeActiveRecallSessionUseCase(
            sessionType = ActiveRecallSessionType.QUICK_5,
            durationSeconds = 120,
            reviewedItems = listOf(item to true)
        )

        assertEquals(100, summary.accuracyPercentage)
        assertEquals(1, summary.itemsReviewedCount)
        assertEquals(1, summary.correctCount)
        assertTrue(summary.streakDays > 0)
        assertNotNull(fakeRevisionRepo.schedules["ch-bio-1"])
    }

    @Test
    fun aiStudyEngine_buildsModeSpecificSystemPrompts() {
        val context = ChapterAiContext(
            subjectId = "sub-1",
            subjectName = "Chemistry",
            chapterId = "ch-1",
            chapterName = "Periodic Table",
            progress = 75,
            masteryPercentage = 80,
            weakTopics = listOf("Electronegativity"),
            notesSummary = "Electronegativity increases across a period."
        )

        val socraticPrompt = aiStudyEngineUseCase.buildSystemPrompt(AiTutorMode.TEACH_ME, context)
        assertTrue(socraticPrompt.contains("Socratic teacher"))
        assertTrue(socraticPrompt.contains("Periodic Table"))
        assertTrue(socraticPrompt.contains("Electronegativity"))

        val doubtPrompt = aiStudyEngineUseCase.buildSystemPrompt(AiTutorMode.DOUBT_SOLVER, context)
        assertTrue(doubtPrompt.contains("doubt solver"))

        val drillPrompt = aiStudyEngineUseCase.buildSystemPrompt(AiTutorMode.PRACTICE_DRILL, context)
        assertTrue(drillPrompt.contains("quiz tutor"))
    }
}
