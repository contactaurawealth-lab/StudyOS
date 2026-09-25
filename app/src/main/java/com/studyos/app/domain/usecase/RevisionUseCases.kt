package com.studyos.app.domain.usecase

import com.studyos.app.core.datastore.PreferencesDataSource
import com.studyos.app.domain.model.ActiveRecallItem
import com.studyos.app.domain.model.ActiveRecallItemType
import com.studyos.app.domain.model.ActiveRecallSessionSummary
import com.studyos.app.domain.model.ActiveRecallSessionType
import com.studyos.app.domain.model.AiConfig
import com.studyos.app.domain.model.AiMessage
import com.studyos.app.domain.model.AiMessageRole
import com.studyos.app.domain.model.AiStreamChunk
import com.studyos.app.domain.model.AiTutorMode
import com.studyos.app.domain.model.ChapterAiContext
import com.studyos.app.domain.model.ChapterMastery
import com.studyos.app.domain.model.ChapterMasteryLevel
import com.studyos.app.domain.model.DueRevisionDashboardData
import com.studyos.app.domain.model.FlashcardRating
import com.studyos.app.domain.model.PracticeDifficulty
import com.studyos.app.domain.model.RevisionRecommendation
import com.studyos.app.domain.model.StudyContext
import com.studyos.app.domain.provider.AiProvider
import com.studyos.app.domain.repository.ChapterRepository
import com.studyos.app.domain.repository.FlashcardRepository
import com.studyos.app.domain.repository.MistakeRepository
import com.studyos.app.domain.repository.NoteRepository
import com.studyos.app.domain.repository.QuizRepository
import com.studyos.app.domain.repository.RevisionRepository
import com.studyos.app.domain.repository.SubjectRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import java.util.UUID
import kotlin.math.roundToInt

// ==========================================
// 1. CHAPTER MASTERY USE CASE
// ==========================================

class GetChapterMasteryUseCase(
    private val chapterRepository: ChapterRepository,
    private val subjectRepository: SubjectRepository,
    private val quizRepository: QuizRepository,
    private val flashcardRepository: FlashcardRepository,
    private val mistakeRepository: MistakeRepository,
    private val revisionRepository: RevisionRepository
) {
    suspend fun getForChapter(chapterId: String): ChapterMastery? {
        val chapter = chapterRepository.getChapterById(chapterId) ?: return null
        val subject = subjectRepository.getSubjectByIdOnce(chapter.subjectId)

        val attempts = quizRepository.getAttemptsForChapter(chapterId).first()
        val quizAccuracy = if (attempts.isNotEmpty()) {
            attempts.map { it.accuracyPercentage }.average().roundToInt()
        } else null

        val flashcards = flashcardRepository.observeFlashcardsForChapter(chapterId).first()
        val flashcardsTotal = flashcards.size
        val flashcardsMastered = flashcards.count { it.reviewCount >= 2 && it.easeFactor >= 2.2f }

        val mistakes = mistakeRepository.observeMistakesForChapter(chapterId).first()
        val mistakesCount = mistakes.size
        val resolvedMistakesCount = mistakes.count { it.isResolved }

        val schedule = revisionRepository.getScheduleForChapter(chapterId)

        val pWeight = 0.20
        val qWeight = 0.35
        val fWeight = 0.25
        val mWeight = 0.20

        val progressPart = chapter.progress * pWeight
        val quizPart = (quizAccuracy ?: chapter.progress) * qWeight
        val flashPart = if (flashcardsTotal > 0) {
            (flashcardsMastered.toDouble() / flashcardsTotal * 100.0) * fWeight
        } else {
            chapter.progress * fWeight
        }
        val mistakePart = if (mistakesCount > 0) {
            (resolvedMistakesCount.toDouble() / mistakesCount * 100.0) * mWeight
        } else {
            chapter.progress * mWeight
        }

        val masteryPercentage = (progressPart + quizPart + flashPart + mistakePart).roundToInt().coerceIn(0, 100)

        val level = when {
            masteryPercentage >= 90 -> ChapterMasteryLevel.MASTERED
            masteryPercentage >= 70 -> ChapterMasteryLevel.PROFICIENT
            masteryPercentage >= 40 -> ChapterMasteryLevel.LEARNING
            else -> ChapterMasteryLevel.NOVICE
        }

        val now = System.currentTimeMillis()
        val isDue = when {
            schedule != null -> schedule.nextRevisionDue <= now
            chapter.progress > 0 -> true
            else -> false
        }

        return ChapterMastery(
            chapterId = chapter.id,
            chapterName = chapter.name,
            subjectId = chapter.subjectId,
            subjectName = subject?.name ?: "Subject",
            masteryPercentage = masteryPercentage,
            level = level,
            progress = chapter.progress,
            quizAccuracy = quizAccuracy,
            flashcardsMastered = flashcardsMastered,
            flashcardsTotal = flashcardsTotal,
            mistakesCount = mistakesCount,
            resolvedMistakesCount = resolvedMistakesCount,
            lastRevisedAt = schedule?.lastRevisedAt,
            nextRevisionDue = schedule?.nextRevisionDue,
            isDueForRevision = isDue
        )
    }

    suspend fun getAllMasteries(): List<ChapterMastery> {
        val chapters = chapterRepository.getAllChaptersOnce()
        return chapters.mapNotNull { getForChapter(it.id) }
    }
}

// ==========================================
// 2. RECOMMENDATIONS USE CASE
// ==========================================

class GenerateRevisionRecommendationsUseCase(
    private val chapterRepository: ChapterRepository,
    private val subjectRepository: SubjectRepository,
    private val mistakeRepository: MistakeRepository,
    private val quizRepository: QuizRepository,
    private val revisionRepository: RevisionRepository
) {
    suspend operator fun invoke(): List<RevisionRecommendation> {
        val chapters = chapterRepository.getAllChaptersOnce()
        val subjects = subjectRepository.getAllSubjectsOnce().associateBy { it.id }
        val recommendations = mutableListOf<RevisionRecommendation>()
        val now = System.currentTimeMillis()

        for (chapter in chapters) {
            val mistakes = mistakeRepository.observeMistakesForChapter(chapter.id).first()
            val unresolved = mistakes.filter { !it.isResolved }
            val schedule = revisionRepository.getScheduleForChapter(chapter.id)
            val subjectName = subjects[chapter.subjectId]?.name ?: "Subject"

            if (unresolved.size >= 3) {
                recommendations.add(
                    RevisionRecommendation(
                        chapterId = chapter.id,
                        chapterName = chapter.name,
                        subjectName = subjectName,
                        reason = "${unresolved.size} unresolved mistakes in this chapter",
                        recommendedAction = "Run 10-Min Mistake Drill",
                        priority = 1
                    )
                )
            } else if (schedule != null && schedule.nextRevisionDue <= now) {
                val daysOverdue = ((now - schedule.nextRevisionDue) / 86_400_000L).coerceAtLeast(0)
                recommendations.add(
                    RevisionRecommendation(
                        chapterId = chapter.id,
                        chapterName = chapter.name,
                        subjectName = subjectName,
                        reason = if (daysOverdue > 0) "Spaced revision overdue by $daysOverdue days" else "Spaced revision due today",
                        recommendedAction = "Complete 15-Min Active Recall",
                        priority = 2
                    )
                )
            } else if (chapter.progress in 30..90 && schedule == null) {
                recommendations.add(
                    RevisionRecommendation(
                        chapterId = chapter.id,
                        chapterName = chapter.name,
                        subjectName = subjectName,
                        reason = "In-progress chapter has no revision schedule",
                        recommendedAction = "Start initial recall session",
                        priority = 3
                    )
                )
            }
        }

        return recommendations.sortedBy { it.priority }.take(5)
    }
}

// ==========================================
// 3. DUE REVISION DASHBOARD USE CASE
// ==========================================

class GetDueRevisionDashboardUseCase(
    private val getChapterMasteryUseCase: GetChapterMasteryUseCase,
    private val identifyWeakTopicsUseCase: IdentifyWeakTopicsUseCase,
    private val generateRecommendationsUseCase: GenerateRevisionRecommendationsUseCase,
    private val flashcardRepository: FlashcardRepository,
    private val revisionRepository: RevisionRepository
) {
    operator fun invoke(): Flow<DueRevisionDashboardData> {
        return combine(
            revisionRepository.revisionStreakDays,
            revisionRepository.dailyRevisionTargetMinutes,
            revisionRepository.observeMinutesRevisedToday()
        ) { streak, targetMinutes, minutesToday ->
            val allMasteries = getChapterMasteryUseCase.getAllMasteries()
            val dueChapters = allMasteries.filter { it.isDueForRevision }
            val dueFlashcards = flashcardRepository.observeDueFlashcards(System.currentTimeMillis()).first()
            val weakTopics = identifyWeakTopicsUseCase().first()
            val recommendations = generateRecommendationsUseCase()

            val recommendedSession = when {
                dueFlashcards.size >= 8 && weakTopics.isEmpty() -> ActiveRecallSessionType.QUICK_5
                weakTopics.isNotEmpty() && weakTopics.size >= 3 -> ActiveRecallSessionType.FOCUSED_10
                else -> ActiveRecallSessionType.DEEP_15
            }

            DueRevisionDashboardData(
                dueChaptersCount = dueChapters.size,
                dueChapters = dueChapters,
                dueFlashcardsCount = dueFlashcards.size,
                unresolvedWeakTopicsCount = weakTopics.size,
                weakTopics = weakTopics,
                recommendedSession = recommendedSession,
                revisionStreakDays = streak,
                dailyRevisionTargetMinutes = targetMinutes,
                completedRevisionMinutesToday = minutesToday,
                recommendations = recommendations
            )
        }
    }
}

// ==========================================
// 4. ACTIVE RECALL RUNNER USE CASES
// ==========================================

class StartActiveRecallSessionUseCase(
    private val flashcardRepository: FlashcardRepository,
    private val mistakeRepository: MistakeRepository,
    private val quizRepository: QuizRepository,
    private val chapterRepository: ChapterRepository
) {
    suspend operator fun invoke(
        sessionType: ActiveRecallSessionType,
        chapterId: String? = null
    ): List<ActiveRecallItem> {
        val items = mutableListOf<ActiveRecallItem>()
        val now = System.currentTimeMillis()

        when (sessionType) {
            ActiveRecallSessionType.QUICK_5 -> {
                // Focus: Due flashcards
                val cards = if (chapterId != null) {
                    flashcardRepository.observeFlashcardsForChapter(chapterId).first()
                } else {
                    flashcardRepository.observeDueFlashcards(now).first()
                }
                val selectedCards = if (cards.isEmpty() && chapterId == null) {
                    flashcardRepository.observeAllFlashcards().first().take(10)
                } else cards.take(10)

                for (card in selectedCards) {
                    items.add(
                        ActiveRecallItem(
                            id = UUID.randomUUID().toString(),
                            type = ActiveRecallItemType.FLASHCARD,
                            title = "Flashcard Recall",
                            prompt = card.question,
                            answer = card.answer,
                            options = emptyList(),
                            subjectId = card.subjectId,
                            chapterId = card.chapterId,
                            sourceReferenceId = card.id
                        )
                    )
                }
            }

            ActiveRecallSessionType.FOCUSED_10 -> {
                // Focus: Mistakes Drill
                val mistakes = if (chapterId != null) {
                    mistakeRepository.observeMistakesForChapter(chapterId).first().filter { !it.isResolved }
                } else {
                    mistakeRepository.observeAllMistakes().first().filter { !it.isResolved }
                }

                for (m in mistakes.take(8)) {
                    items.add(
                        ActiveRecallItem(
                            id = UUID.randomUUID().toString(),
                            type = ActiveRecallItemType.MISTAKE_RETRY,
                            title = "Mistake Retry: ${m.topic ?: "Concept"}",
                            prompt = m.question,
                            answer = m.correctAnswer,
                            options = emptyList(),
                            explanation = m.explanation,
                            subjectId = m.subjectId,
                            chapterId = m.chapterId,
                            sourceReferenceId = m.id
                        )
                    )
                }

                // If not enough mistakes, add cards
                if (items.size < 5) {
                    val cards = flashcardRepository.observeDueFlashcards(now).first().take(5 - items.size)
                    for (card in cards) {
                        items.add(
                            ActiveRecallItem(
                                id = UUID.randomUUID().toString(),
                                type = ActiveRecallItemType.FLASHCARD,
                                title = "Supplementary Flashcard",
                                prompt = card.question,
                                answer = card.answer,
                                subjectId = card.subjectId,
                                chapterId = card.chapterId,
                                sourceReferenceId = card.id
                            )
                        )
                    }
                }
            }

            ActiveRecallSessionType.DEEP_15 -> {
                // Focus: Comprehensive consolidation
                // 1. Flashcards (up to 6)
                val cards = if (chapterId != null) {
                    flashcardRepository.observeFlashcardsForChapter(chapterId).first().take(6)
                } else {
                    flashcardRepository.observeDueFlashcards(now).first().take(6)
                }
                for (card in cards) {
                    items.add(
                        ActiveRecallItem(
                            id = UUID.randomUUID().toString(),
                            type = ActiveRecallItemType.FLASHCARD,
                            title = "Flashcard Recall",
                            prompt = card.question,
                            answer = card.answer,
                            subjectId = card.subjectId,
                            chapterId = card.chapterId,
                            sourceReferenceId = card.id
                        )
                    )
                }

                // 2. Mistakes (up to 4)
                val mistakes = if (chapterId != null) {
                    mistakeRepository.observeMistakesForChapter(chapterId).first().filter { !it.isResolved }.take(4)
                } else {
                    mistakeRepository.observeAllMistakes().first().filter { !it.isResolved }.take(4)
                }
                for (m in mistakes) {
                    items.add(
                        ActiveRecallItem(
                            id = UUID.randomUUID().toString(),
                            type = ActiveRecallItemType.MISTAKE_RETRY,
                            title = "Mistake Retry: ${m.topic ?: "Concept"}",
                            prompt = m.question,
                            answer = m.correctAnswer,
                            explanation = m.explanation,
                            subjectId = m.subjectId,
                            chapterId = m.chapterId,
                            sourceReferenceId = m.id
                        )
                    )
                }

                // 3. Quiz Questions if available (up to 4)
                val quizzes = if (chapterId != null) {
                    quizRepository.observeQuizzesForChapter(chapterId).first()
                } else {
                    emptyList()
                }
                for (q in quizzes) {
                    if (items.size >= 14) break
                    val questions = quizRepository.getQuestionsForQuiz(q.id)
                    for (question in questions.take(2)) {
                        items.add(
                            ActiveRecallItem(
                                id = UUID.randomUUID().toString(),
                                type = ActiveRecallItemType.CONCEPT_QUIZ,
                                title = "Concept Check: ${q.title}",
                                prompt = question.question,
                                answer = question.correctAnswer,
                                options = question.options,
                                explanation = question.explanation,
                                subjectId = q.subjectId,
                                chapterId = q.chapterId,
                                sourceReferenceId = question.id
                            )
                        )
                    }
                }
            }
        }

        return items
    }
}

class CompleteActiveRecallSessionUseCase(
    private val revisionRepository: RevisionRepository,
    private val flashcardRepository: FlashcardRepository,
    private val mistakeRepository: MistakeRepository
) {
    suspend operator fun invoke(
        sessionType: ActiveRecallSessionType,
        durationSeconds: Int,
        reviewedItems: List<Pair<ActiveRecallItem, Boolean>> // Item and whether student got it right
    ): ActiveRecallSessionSummary {
        val total = reviewedItems.size
        val correct = reviewedItems.count { it.second }
        val accuracy = if (total > 0) (correct * 100) / total else 0

        // Update items in database
        val touchedChapterIds = mutableSetOf<Pair<String, String>>() // (chapterId, subjectId)

        for ((item, wasCorrect) in reviewedItems) {
            item.chapterId?.let { chId ->
                touchedChapterIds.add(chId to item.subjectId)
            }

            when (item.type) {
                ActiveRecallItemType.FLASHCARD -> {
                    val rating = if (wasCorrect) FlashcardRating.GOOD else FlashcardRating.AGAIN
                    flashcardRepository.recordReview(item.sourceReferenceId, rating)
                }
                ActiveRecallItemType.MISTAKE_RETRY -> {
                    if (wasCorrect) {
                        mistakeRepository.resolveMistake(item.sourceReferenceId)
                    }
                }
                ActiveRecallItemType.CONCEPT_QUIZ -> {
                    // Handled as part of chapter revision schedule update
                }
            }
        }

        // Update revision schedule for chapters touched
        for ((chId, subId) in touchedChapterIds) {
            val qualityScore = if (accuracy >= 80) 5 else if (accuracy >= 60) 3 else 1
            revisionRepository.recordChapterRevisionCompleted(chId, subId, qualityScore)
        }

        val tempSummary = ActiveRecallSessionSummary(
            sessionType = sessionType,
            durationSeconds = durationSeconds,
            itemsReviewedCount = total,
            correctCount = correct,
            accuracyPercentage = accuracy,
            streakDays = 0,
            streakMaintained = true,
            masteryGainedText = "+${(accuracy * 0.15).roundToInt()}% Mastery Boost"
        )

        val updatedStreak = revisionRepository.recordRecallSession(tempSummary)

        return tempSummary.copy(
            streakDays = updatedStreak,
            streakMaintained = true
        )
    }
}

// ==========================================
// 5. CHAPTER AI CONTEXT USE CASE
// ==========================================

class GetChapterAiContextUseCase(
    private val chapterRepository: ChapterRepository,
    private val subjectRepository: SubjectRepository,
    private val noteRepository: NoteRepository,
    private val mistakeRepository: MistakeRepository,
    private val quizRepository: QuizRepository,
    private val getChapterMasteryUseCase: GetChapterMasteryUseCase
) {
    suspend operator fun invoke(chapterId: String): ChapterAiContext? {
        val chapter = chapterRepository.getChapterById(chapterId) ?: return null
        val subject = subjectRepository.getSubjectByIdOnce(chapter.subjectId)
        val mastery = getChapterMasteryUseCase.getForChapter(chapterId)

        val notes = noteRepository.observeNotesForChapter(chapterId).first()
        val notesSummary = notes.joinToString("\n\n") { "${it.title}:\n${it.content}" }.take(800)

        val mistakes = mistakeRepository.observeMistakesForChapter(chapterId).first()
        val recentMistakes = mistakes.filter { !it.isResolved }.map { it.question }.take(5)

        val attempts = quizRepository.getAttemptsForChapter(chapterId).first()
        val quizAcc = if (attempts.isNotEmpty()) attempts.map { it.accuracyPercentage }.average().roundToInt() else null

        val weakTopics = mistakes.mapNotNull { it.topic }.distinct().take(4)

        return ChapterAiContext(
            subjectId = chapter.subjectId,
            subjectName = subject?.name ?: "Subject",
            chapterId = chapter.id,
            chapterName = chapter.name,
            progress = chapter.progress,
            masteryPercentage = mastery?.masteryPercentage ?: chapter.progress,
            weakTopics = weakTopics,
            notesSummary = notesSummary,
            recentMistakes = recentMistakes,
            quizAccuracy = quizAcc
        )
    }
}

// ==========================================
// 6. AI STUDY ENGINE USE CASE
// ==========================================

class AiStudyEngineUseCase(
    private val aiProvider: AiProvider,
    private val preferencesDataSource: PreferencesDataSource
) {
    fun buildSystemPrompt(
        mode: AiTutorMode,
        context: ChapterAiContext?
    ): String {
        val base = when (mode) {
            AiTutorMode.STUDY_PARTNER ->
                "You are an encouraging academic study partner. Give clear, concise, and structured explanations. Break down difficult concepts into simple terms."
            AiTutorMode.TEACH_ME ->
                "You are a master Socratic teacher. Teach the student step-by-step. Explain one core concept at a time in 2-3 sentences, then ask ONE quick check question to test understanding before moving forward."
            AiTutorMode.DOUBT_SOLVER ->
                "You are an empathetic doubt solver. Identify the exact misconception, explain why the concept works the way it does using intuitive analogies, and summarize the key rule clearly."
            AiTutorMode.PRACTICE_DRILL ->
                "You are a rigorous quiz tutor. Present a conceptual practice question with 4 multiple choice options. Wait for the user's response, or explain the answer if they ask."
        }

        return if (context != null) {
            base + context.toSystemPromptAddendum()
        } else {
            base
        }
    }

    fun executeQuickAction(
        actionType: String,
        context: ChapterAiContext?,
        topicOrQuestion: String
    ): Pair<String, String> { // Returns (Display prompt, System instruction)
        return when (actionType) {
            "EXPLAIN" -> {
                val prompt = "Explain $topicOrQuestion in simple, clear language with an intuitive example."
                val sys = "Explain the topic with crystal clarity. Use bullet points and bold key terms."
                prompt to sys
            }
            "TEACH_ME" -> {
                val prompt = "Teach me $topicOrQuestion from scratch using step-by-step Socratic teaching."
                val sys = "Teach step-by-step. Conclude with a single check question for the student."
                prompt to sys
            }
            "DOUBT" -> {
                val prompt = "I'm confused about: $topicOrQuestion. Can you clarify this for me?"
                val sys = "Clear up the doubt with an everyday analogy and common pitfalls to avoid."
                prompt to sys
            }
            "EXAMPLE" -> {
                val prompt = "Give me 2 real-world, practical examples of $topicOrQuestion."
                val sys = "Provide 2 concrete, vivid real-world applications showing how the concept works in practice."
                prompt to sys
            }
            "QUIZ_ME" -> {
                val prompt = "Test my knowledge on $topicOrQuestion with 1 conceptual question and 4 choices."
                val sys = "Provide 1 conceptual question with choices A, B, C, D and ask the student to select the answer."
                prompt to sys
            }
            "SIMPLIFY" -> {
                val prompt = "Simplify $topicOrQuestion as if I am 12 years old (ELI5)."
                val sys = "Explain with maximum simplicity, no jargon, and an relatable analogy."
                prompt to sys
            }
            else -> {
                topicOrQuestion to "Be a helpful academic study assistant."
            }
        }
    }

    fun generateDrillQuestion(
        difficulty: PracticeDifficulty,
        context: ChapterAiContext?,
        topic: String?
    ): Flow<String> = flow {
        val topicName = topic ?: context?.chapterName ?: "Key Concepts"
        val prompt = """
            Generate ONE ${difficulty.label} practice question testing $topicName.
            Include 4 options: A), B), C), D).
            Do NOT reveal the answer immediately; ask the student to answer.
        """.trimIndent()

        val messages = listOf(AiMessage(conversationId = "drill", role = AiMessageRole.USER, content = prompt))
        val config = preferencesDataSource.aiConfig.firstOrNull() ?: AiConfig()
        val studyContext = StudyContext(
            subjectName = context?.subjectName,
            chapterName = context?.chapterName,
            chapterProgress = context?.progress
        )

        aiProvider.generateStream(messages, studyContext, config).collect { chunk ->
            if (chunk is AiStreamChunk.Content) {
                emit(chunk.delta)
            }
        }
    }
}
