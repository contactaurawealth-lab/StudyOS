package com.studyos.app.domain.usecase

import com.studyos.app.domain.model.ActiveQuizState
import com.studyos.app.domain.model.AiConfig
import com.studyos.app.domain.model.AiMessage
import com.studyos.app.domain.model.AiMessageRole
import com.studyos.app.domain.model.AiMessageStatus
import com.studyos.app.domain.model.AiStreamChunk
import com.studyos.app.domain.model.Chapter
import com.studyos.app.domain.model.ChapterPracticeSummary
import com.studyos.app.domain.model.ChapterStatus
import com.studyos.app.domain.model.Exam
import com.studyos.app.domain.model.ExamChapterRevisionItem
import com.studyos.app.domain.model.ExamDashboardItem
import com.studyos.app.domain.model.ExamRevisionCategory
import com.studyos.app.domain.model.ExamSubjectProgress
import com.studyos.app.domain.model.Flashcard
import com.studyos.app.domain.model.FlashcardDifficulty
import com.studyos.app.domain.model.FlashcardRating
import com.studyos.app.domain.model.Mistake
import com.studyos.app.domain.model.Note
import com.studyos.app.domain.model.QuestionResult
import com.studyos.app.domain.model.QuestionType
import com.studyos.app.domain.model.Quiz
import com.studyos.app.domain.model.QuizAttempt
import com.studyos.app.domain.model.QuizDifficulty
import com.studyos.app.domain.model.QuizQuestion
import com.studyos.app.domain.model.StudyContext
import com.studyos.app.domain.model.WeakTopic
import com.studyos.app.domain.model.WeakTopicAction
import com.studyos.app.domain.provider.AiProvider
import com.studyos.app.domain.repository.ChapterRepository
import com.studyos.app.domain.repository.ExamRepository
import com.studyos.app.domain.repository.FlashcardRepository
import com.studyos.app.domain.repository.MistakeRepository
import com.studyos.app.domain.repository.NoteRepository
import com.studyos.app.domain.repository.QuizRepository
import com.studyos.app.domain.repository.SubjectRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

// ==========================================
// 1. NOTES USE CASES
// ==========================================

class GetNotesForChapterUseCase(private val noteRepository: NoteRepository) {
    operator fun invoke(chapterId: String): Flow<List<Note>> =
        noteRepository.observeNotesForChapter(chapterId)
}

class GetNoteUseCase(private val noteRepository: NoteRepository) {
    operator fun invoke(id: String): Flow<Note?> =
        noteRepository.getNoteById(id)

    suspend fun getOnce(id: String): Note? =
        noteRepository.getNoteByIdOnce(id)
}

class SaveNoteUseCase(private val noteRepository: NoteRepository) {
    suspend operator fun invoke(
        id: String? = null,
        title: String,
        content: String,
        subjectId: String? = null,
        chapterId: String? = null,
        isPinned: Boolean = false
    ): Note {
        val now = System.currentTimeMillis()
        val note = if (id != null) {
            val existing = noteRepository.getNoteByIdOnce(id)
            if (existing != null) {
                existing.copy(
                    title = title.trim(),
                    content = content.trim(),
                    isPinned = isPinned,
                    updatedAt = now
                )
            } else {
                Note(
                    id = id,
                    title = title.trim(),
                    content = content.trim(),
                    subjectId = subjectId,
                    chapterId = chapterId,
                    isPinned = isPinned,
                    createdAt = now,
                    updatedAt = now
                )
            }
        } else {
            Note(
                id = UUID.randomUUID().toString(),
                title = title.trim(),
                content = content.trim(),
                subjectId = subjectId,
                chapterId = chapterId,
                isPinned = isPinned,
                createdAt = now,
                updatedAt = now
            )
        }
        return noteRepository.saveNote(note)
    }
}

class DeleteNoteUseCase(private val noteRepository: NoteRepository) {
    suspend operator fun invoke(id: String) =
        noteRepository.deleteNote(id)
}

class ToggleNotePinUseCase(private val noteRepository: NoteRepository) {
    suspend operator fun invoke(id: String, isPinned: Boolean) =
        noteRepository.togglePin(id, isPinned)
}

// ==========================================
// 2. FLASHCARDS USE CASES
// ==========================================

class GetFlashcardsForChapterUseCase(private val flashcardRepository: FlashcardRepository) {
    operator fun invoke(chapterId: String): Flow<List<Flashcard>> =
        flashcardRepository.observeFlashcardsForChapter(chapterId)
}

class GetDueFlashcardsUseCase(private val flashcardRepository: FlashcardRepository) {
    operator fun invoke(currentTime: Long = System.currentTimeMillis()): Flow<List<Flashcard>> =
        flashcardRepository.observeDueFlashcards(currentTime)
}

class SaveFlashcardUseCase(private val flashcardRepository: FlashcardRepository) {
    suspend operator fun invoke(
        id: String? = null,
        question: String,
        answer: String,
        subjectId: String,
        chapterId: String? = null,
        difficulty: FlashcardDifficulty = FlashcardDifficulty.MEDIUM
    ): Flashcard {
        val card = Flashcard(
            id = id ?: UUID.randomUUID().toString(),
            question = question.trim(),
            answer = answer.trim(),
            subjectId = subjectId,
            chapterId = chapterId,
            difficulty = difficulty
        )
        return flashcardRepository.saveFlashcard(card)
    }

    suspend fun saveBatch(flashcards: List<Flashcard>) {
        flashcardRepository.saveFlashcards(flashcards)
    }
}

class ReviewFlashcardUseCase(private val flashcardRepository: FlashcardRepository) {
    suspend operator fun invoke(cardId: String, rating: FlashcardRating): Flashcard =
        flashcardRepository.recordReview(cardId, rating)
}

class DeleteFlashcardUseCase(private val flashcardRepository: FlashcardRepository) {
    suspend operator fun invoke(id: String) =
        flashcardRepository.deleteFlashcard(id)
}

// ==========================================
// 3. QUIZ & ATTEMPTS USE CASES
// ==========================================

class SaveQuizUseCase(private val quizRepository: QuizRepository) {
    suspend operator fun invoke(quiz: Quiz, questions: List<QuizQuestion>): Quiz =
        quizRepository.saveQuiz(quiz, questions)
}

class GetQuizWithQuestionsUseCase(private val quizRepository: QuizRepository) {
    suspend operator fun invoke(quizId: String): Pair<Quiz?, List<QuizQuestion>> {
        val quiz = quizRepository.getQuizById(quizId)
        val questions = quizRepository.getQuestionsForQuiz(quizId)
        return Pair(quiz, questions)
    }
}

class SubmitQuizAttemptUseCase(
    private val quizRepository: QuizRepository,
    private val mistakeRepository: MistakeRepository
) {
    suspend operator fun invoke(
        quizId: String,
        subjectId: String,
        chapterId: String?,
        startedAt: Long,
        timeSpentSeconds: Int,
        answers: Map<String, String>, // questionId -> studentAnswer
        questions: List<QuizQuestion>
    ): QuizAttempt {
        val completedAt = System.currentTimeMillis()
        var correctCount = 0
        val questionResults = mutableListOf<QuestionResult>()
        val attemptId = UUID.randomUUID().toString()

        for (q in questions) {
            val studentAns = answers[q.id]?.trim() ?: ""
            val isCorrect = studentAns.equals(q.correctAnswer.trim(), ignoreCase = true)
            if (isCorrect) correctCount++

            questionResults.add(
                QuestionResult(
                    id = UUID.randomUUID().toString(),
                    attemptId = attemptId,
                    questionId = q.id,
                    studentAnswer = studentAns,
                    isCorrect = isCorrect,
                    topic = q.topic
                )
            )

            // Auto-record incorrect answers into Mistake Bank!
            if (!isCorrect) {
                mistakeRepository.recordMistake(
                    Mistake(
                        question = q.question,
                        studentAnswer = if (studentAns.isBlank()) "(Skipped)" else studentAns,
                        correctAnswer = q.correctAnswer,
                        explanation = q.explanation,
                        subjectId = subjectId,
                        chapterId = chapterId,
                        topic = q.topic
                    )
                )
            }
        }

        val totalQuestions = questions.size
        val accuracy = if (totalQuestions > 0) ((correctCount.toFloat() / totalQuestions) * 100).roundToInt() else 0

        val attempt = QuizAttempt(
            id = attemptId,
            quizId = quizId,
            subjectId = subjectId,
            chapterId = chapterId,
            startedAt = startedAt,
            completedAt = completedAt,
            score = correctCount,
            totalQuestions = totalQuestions,
            accuracyPercentage = accuracy,
            timeSpentSeconds = timeSpentSeconds,
            isCompleted = true
        )

        quizRepository.recordAttempt(attempt, questionResults)
        quizRepository.clearActiveState(quizId)
        return attempt
    }
}

class SaveActiveQuizStateUseCase(private val quizRepository: QuizRepository) {
    suspend operator fun invoke(state: ActiveQuizState) =
        quizRepository.saveActiveState(state)
}

class GetActiveQuizStateUseCase(private val quizRepository: QuizRepository) {
    suspend operator fun invoke(quizId: String): ActiveQuizState? =
        quizRepository.getActiveState(quizId)
}

// ==========================================
// 4. MISTAKE BANK USE CASES
// ==========================================

class GetMistakesForChapterUseCase(private val mistakeRepository: MistakeRepository) {
    operator fun invoke(chapterId: String): Flow<List<Mistake>> =
        mistakeRepository.observeMistakesForChapter(chapterId)
}

class GetAllMistakesUseCase(private val mistakeRepository: MistakeRepository) {
    operator fun invoke(): Flow<List<Mistake>> =
        mistakeRepository.observeAllMistakes()
}

class ResolveMistakeUseCase(private val mistakeRepository: MistakeRepository) {
    suspend operator fun invoke(id: String) =
        mistakeRepository.resolveMistake(id)
}

class ConvertMistakeToFlashcardUseCase(
    private val mistakeRepository: MistakeRepository,
    private val flashcardRepository: FlashcardRepository
) {
    suspend operator fun invoke(mistake: Mistake): Flashcard {
        val answerText = if (!mistake.explanation.isNullOrBlank()) {
            "${mistake.correctAnswer}\n\nExplanation: ${mistake.explanation}"
        } else {
            mistake.correctAnswer
        }

        val flashcard = Flashcard(
            id = UUID.randomUUID().toString(),
            question = mistake.question,
            answer = answerText,
            subjectId = mistake.subjectId,
            chapterId = mistake.chapterId,
            difficulty = FlashcardDifficulty.HARD
        )
        flashcardRepository.saveFlashcard(flashcard)
        mistakeRepository.resolveMistake(mistake.id)
        return flashcard
    }
}

// ==========================================
// 5. CHAPTER PERFORMANCE & REVISION ENGINE
// ==========================================

class GetChapterPracticeSummaryUseCase(
    private val chapterRepository: ChapterRepository,
    private val subjectRepository: SubjectRepository,
    private val noteRepository: NoteRepository,
    private val flashcardRepository: FlashcardRepository,
    private val quizRepository: QuizRepository,
    private val mistakeRepository: MistakeRepository
) {
    operator fun invoke(chapterId: String): Flow<ChapterPracticeSummary?> = flow {
        val chapter = chapterRepository.getChapterById(chapterId) ?: run {
            emit(null)
            return@flow
        }
        val subject = subjectRepository.getSubjectByIdOnce(chapter.subjectId)

        combine(
            noteRepository.observeNotesForChapter(chapterId),
            flashcardRepository.observeFlashcardsForChapter(chapterId),
            quizRepository.getAttemptsForChapter(chapterId),
            mistakeRepository.observeMistakesForChapter(chapterId)
        ) { notes, cards, attempts, mistakes ->
            val dueCount = cards.count { it.getCategory() == com.studyos.app.domain.model.FlashcardCategory.DUE }
            val avgAccuracy = if (attempts.isNotEmpty()) {
                attempts.map { it.accuracyPercentage }.average().roundToInt()
            } else null

            // Analyze weak topics from attempts and unresolved mistakes
            val topicStats = mutableMapOf<String, Pair<Int, Int>>() // topic -> (correct, total)
            mistakes.filter { !it.isResolved }.forEach { m ->
                val topicName = m.topic ?: chapter.name
                val current = topicStats.getOrDefault(topicName, Pair(0, 0))
                topicStats[topicName] = Pair(current.first, current.second + m.missedCount)
            }

            val weakTopics = topicStats.map { (topic, stats) ->
                val total = stats.second
                val correct = stats.first
                val acc = if (total > 0) ((correct.toFloat() / total) * 100).roundToInt() else 0
                val action = when {
                    acc < 40 -> WeakTopicAction.REVISE
                    acc < 65 -> WeakTopicAction.FLASHCARDS
                    else -> WeakTopicAction.PRACTICE_QUIZ
                }
                WeakTopic(
                    topic = topic,
                    accuracyPercentage = acc,
                    questionsAttempted = total,
                    subjectId = chapter.subjectId,
                    chapterId = chapterId,
                    recommendedAction = action
                )
            }.sortedBy { it.accuracyPercentage }

            ChapterPracticeSummary(
                chapterId = chapter.id,
                chapterName = chapter.name,
                subjectName = subject?.name ?: "Subject",
                progress = chapter.progress,
                quizAccuracy = avgAccuracy,
                flashcardsDue = dueCount,
                notesCount = notes.size,
                mistakesCount = mistakes.count { !it.isResolved },
                weakTopics = weakTopics
            )
        }.collect { emit(it) }
    }
}

// ==========================================
// 6. EXAM USE CASES & REVISION ENGINE
// ==========================================

class GetExamsUseCase(private val examRepository: ExamRepository) {
    operator fun invoke(): Flow<List<Exam>> = examRepository.observeAllExams()
}

class SaveExamUseCase(private val examRepository: ExamRepository) {
    suspend operator fun invoke(
        id: String? = null,
        name: String,
        targetDate: Long,
        subjectIds: List<String>,
        targetScore: Int? = null,
        notes: String? = null
    ): Exam {
        val exam = Exam(
            id = id ?: UUID.randomUUID().toString(),
            name = name.trim(),
            targetDate = targetDate,
            subjectIds = subjectIds,
            targetScore = targetScore,
            notes = notes?.trim()?.ifEmpty { null }
        )
        return examRepository.saveExam(exam, subjectIds)
    }
}

class DeleteExamUseCase(private val examRepository: ExamRepository) {
    suspend operator fun invoke(id: String) = examRepository.deleteExam(id)
}

class GetExamDashboardUseCase(
    private val examRepository: ExamRepository,
    private val subjectRepository: SubjectRepository,
    private val chapterRepository: ChapterRepository,
    private val flashcardRepository: FlashcardRepository,
    private val noteRepository: NoteRepository,
    private val quizRepository: QuizRepository,
    private val mistakeRepository: MistakeRepository
) {
    suspend operator fun invoke(examId: String): ExamDashboardItem? {
        val exam = examRepository.getExamByIdOnce(examId) ?: return null
        val now = System.currentTimeMillis()
        val daysRemaining = max(0L, (exam.targetDate - now) / 86_400_000L)

        val allSubjects = subjectRepository.getAllSubjectsOnce()
        val examSubjects = allSubjects.filter { exam.subjectIds.contains(it.id) }

        val subjectProgresses = mutableListOf<ExamSubjectProgress>()
        var totalChaptersRemaining = 0
        val examChapterRevisionItems = mutableListOf<ExamChapterRevisionItem>()

        for (subject in examSubjects) {
            val chapters = chapterRepository.getChaptersForSubjectOnce(subject.id)
            val completedCount = chapters.count { it.status == ChapterStatus.COMPLETED || it.progress == 100 }
            val remainingCount = chapters.size - completedCount
            totalChaptersRemaining += remainingCount

            val avgProgress = if (chapters.isNotEmpty()) {
                (chapters.sumOf { it.progress }.toFloat() / chapters.size).roundToInt()
            } else 0

            subjectProgresses.add(
                ExamSubjectProgress(
                    subject = subject,
                    progressPercentage = avgProgress,
                    chaptersCount = chapters.size,
                    completedChaptersCount = completedCount
                )
            )
        }

        // Global metrics across exam subjects
        val allNotes = noteRepository.observeAllNotes().first()
        val examNotesCount = allNotes.count { it.subjectId != null && exam.subjectIds.contains(it.subjectId) }

        val allCards = flashcardRepository.observeAllFlashcards().first()
        val examCardsDue = allCards.count {
            exam.subjectIds.contains(it.subjectId) && it.getCategory(now) == com.studyos.app.domain.model.FlashcardCategory.DUE
        }

        val allAttempts = quizRepository.getAllAttempts().first()
        val examAttempts = allAttempts.filter { exam.subjectIds.contains(it.subjectId) }
        val avgQuizAcc = if (examAttempts.isNotEmpty()) {
            examAttempts.map { it.accuracyPercentage }.average().roundToInt()
        } else null

        val allMistakes = mistakeRepository.observeAllMistakes().first()
        val examMistakes = allMistakes.filter { exam.subjectIds.contains(it.subjectId) && !it.isResolved }

        // Weak topics for exam
        val weakTopics = examMistakes.groupBy { it.topic ?: "General" }.map { (topic, list) ->
            WeakTopic(
                topic = topic,
                accuracyPercentage = max(10, 100 - (list.size * 20)),
                questionsAttempted = list.size,
                recommendedAction = if (list.size > 2) WeakTopicAction.REVISE else WeakTopicAction.FLASHCARDS
            )
        }.sortedBy { it.accuracyPercentage }

        return ExamDashboardItem(
            exam = exam,
            daysRemaining = daysRemaining,
            subjectProgresses = subjectProgresses,
            chaptersRemaining = totalChaptersRemaining,
            notesAvailable = examNotesCount,
            flashcardsDue = examCardsDue,
            quizAccuracy = avgQuizAcc,
            weakTopics = weakTopics
        )
    }

    suspend fun getRevisionChapters(examId: String): List<ExamChapterRevisionItem> {
        val exam = examRepository.getExamByIdOnce(examId) ?: return emptyList()
        val allSubjects = subjectRepository.getAllSubjectsOnce().associateBy { it.id }
        val now = System.currentTimeMillis()

        val allCards = flashcardRepository.observeAllFlashcards().first()
        val allAttempts = quizRepository.getAllAttempts().first()
        val allMistakes = mistakeRepository.observeAllMistakes().first()

        val items = mutableListOf<ExamChapterRevisionItem>()

        for (subjectId in exam.subjectIds) {
            val subject = allSubjects[subjectId] ?: continue
            val chapters = chapterRepository.getChaptersForSubjectOnce(subjectId)

            for (chapter in chapters) {
                val chapterAttempts = allAttempts.filter { it.chapterId == chapter.id }
                val chapterMistakes = allMistakes.filter { it.chapterId == chapter.id && !it.isResolved }
                val chapterDueCards = allCards.count { it.chapterId == chapter.id && it.getCategory(now) == com.studyos.app.domain.model.FlashcardCategory.DUE }

                val accuracy = if (chapterAttempts.isNotEmpty()) {
                    chapterAttempts.map { it.accuracyPercentage }.average().roundToInt()
                } else null

                val category = when {
                    chapterMistakes.isNotEmpty() || (accuracy != null && accuracy < 60) || chapter.progress < 40 ->
                        ExamRevisionCategory.NEED_REVISION
                    (accuracy != null && accuracy < 80) || chapter.progress < 85 ->
                        ExamRevisionCategory.PRACTICE
                    else ->
                        ExamRevisionCategory.STRONG
                }

                items.add(
                    ExamChapterRevisionItem(
                        chapter = chapter,
                        subject = subject,
                        category = category,
                        accuracyPercentage = accuracy,
                        mistakesCount = chapterMistakes.size,
                        flashcardsDueCount = chapterDueCards
                    )
                )
            }
        }

        return items.sortedWith(
            compareBy<ExamChapterRevisionItem> {
                when (it.category) {
                    ExamRevisionCategory.NEED_REVISION -> 0
                    ExamRevisionCategory.PRACTICE -> 1
                    ExamRevisionCategory.STRONG -> 2
                }
            }.thenBy { it.accuracyPercentage ?: 0 }
        )
    }
}

class IdentifyWeakTopicsUseCase(
    private val mistakeRepository: MistakeRepository
) {
    operator fun invoke(subjectId: String? = null, chapterId: String? = null): Flow<List<WeakTopic>> {
        val mistakesFlow: Flow<List<Mistake>> = when {
            chapterId != null -> mistakeRepository.observeMistakesForChapter(chapterId)
            subjectId != null -> mistakeRepository.observeMistakesForSubject(subjectId)
            else -> mistakeRepository.observeAllMistakes()
        }

        return mistakesFlow.map { list: List<Mistake> ->
            val unresolved = list.filter { m -> !m.isResolved }
            val grouped: Map<String, List<Mistake>> = unresolved.groupBy { it.topic ?: "General" }
            grouped.map { entry ->
                val topic = entry.key
                val mList = entry.value
                val acc = max(10, 100 - (mList.size * 20))
                WeakTopic(
                    topic = topic,
                    accuracyPercentage = acc,
                    questionsAttempted = mList.size,
                    recommendedAction = if (mList.size > 2) WeakTopicAction.REVISE else WeakTopicAction.FLASHCARDS
                )
            }.sortedBy { it.accuracyPercentage }
        }
    }
}

// ==========================================
// 7. AI PRACTICE TOOLS USE CASE
// ==========================================

class AiPracticeToolsUseCase(
    private val aiProvider: AiProvider
) {
    fun summarizeNote(noteTitle: String, noteContent: String, config: AiConfig): Flow<String> = flow {
        val prompt = "Please provide a concise, high-yield study summary of this note (\"$noteTitle\"):\n\n$noteContent"
        val messages = listOf(AiMessage(conversationId = "practice", role = AiMessageRole.USER, content = prompt))
        val buffer = StringBuilder()

        aiProvider.generateStream(messages, null, config).collect { chunk ->
            if (chunk is AiStreamChunk.Content) {
                buffer.append(chunk.delta)
                emit(buffer.toString())
            }
        }
    }

    fun explainNote(noteTitle: String, noteContent: String, config: AiConfig): Flow<String> = flow {
        val prompt = "Explain the concepts in this note (\"$noteTitle\") in an easy-to-understand, beginner-friendly way with a practical example:\n\n$noteContent"
        val messages = listOf(AiMessage(conversationId = "practice", role = AiMessageRole.USER, content = prompt))
        val buffer = StringBuilder()

        aiProvider.generateStream(messages, null, config).collect { chunk ->
            if (chunk is AiStreamChunk.Content) {
                buffer.append(chunk.delta)
                emit(buffer.toString())
            }
        }
    }

    fun explainMistake(
        question: String,
        studentAnswer: String,
        correctAnswer: String,
        config: AiConfig
    ): Flow<String> = flow {
        val prompt = """
            A student made a mistake on this question:
            Question: $question
            Student's answer: $studentAnswer
            Correct answer: $correctAnswer

            Please explain:
            1. Why the student's answer is incorrect or where the misconception lies.
            2. Why the correct answer is right.
            3. A quick memory tip to avoid this mistake in the future.
        """.trimIndent()
        val messages = listOf(AiMessage(conversationId = "practice", role = AiMessageRole.USER, content = prompt))
        val buffer = StringBuilder()

        aiProvider.generateStream(messages, null, config).collect { chunk ->
            if (chunk is AiStreamChunk.Content) {
                buffer.append(chunk.delta)
                emit(buffer.toString())
            }
        }
    }

    suspend fun generateFlashcardsFromNote(
        noteTitle: String,
        noteContent: String,
        subjectId: String,
        chapterId: String?,
        config: AiConfig
    ): List<Flashcard> {
        val prompt = """
            Generate 5 high-yield flashcards from this study note ($noteTitle).
            Respond ONLY with a JSON array where each object has "question" and "answer" strings.
            Example: [{"question": "What is ...?", "answer": "It is ..."}]

            Note content:
            $noteContent
        """.trimIndent()

        val messages = listOf(AiMessage(conversationId = "practice", role = AiMessageRole.USER, content = prompt))
        val buffer = StringBuilder()

        aiProvider.generateStream(messages, null, config).collect { chunk ->
            if (chunk is AiStreamChunk.Content) {
                buffer.append(chunk.delta)
            }
        }

        val cards = mutableListOf<Flashcard>()
        try {
            val raw = buffer.toString()
            val startIdx = raw.indexOf('[')
            val endIdx = raw.lastIndexOf(']')
            if (startIdx != -1 && endIdx != -1 && endIdx > startIdx) {
                val jsonArr = JSONArray(raw.substring(startIdx, endIdx + 1))
                for (i in 0 until jsonArr.length()) {
                    val obj = jsonArr.getJSONObject(i)
                    cards.add(
                        Flashcard(
                            question = obj.getString("question"),
                            answer = obj.getString("answer"),
                            subjectId = subjectId,
                            chapterId = chapterId,
                            difficulty = FlashcardDifficulty.MEDIUM
                        )
                    )
                }
            }
        } catch (_: Exception) {}

        return cards
    }

    suspend fun generateQuizFromContext(
        title: String,
        topic: String,
        content: String,
        questionCount: Int,
        difficulty: QuizDifficulty,
        subjectId: String,
        chapterId: String?,
        config: AiConfig
    ): Pair<Quiz, List<QuizQuestion>> {
        val prompt = """
            Create a $questionCount-question multiple choice quiz on "$topic" with difficulty $difficulty.
            Respond ONLY with a JSON array where each object has:
            - "question": string
            - "options": array of 4 string choices
            - "correctAnswer": string (exact match to one of the options)
            - "explanation": string
            - "topic": string

            Reference material:
            $content
        """.trimIndent()

        val messages = listOf(AiMessage(conversationId = "practice", role = AiMessageRole.USER, content = prompt))
        val buffer = StringBuilder()

        aiProvider.generateStream(messages, null, config).collect { chunk ->
            if (chunk is AiStreamChunk.Content) {
                buffer.append(chunk.delta)
            }
        }

        val quizId = UUID.randomUUID().toString()
        val quiz = Quiz(
            id = quizId,
            title = title,
            subjectId = subjectId,
            chapterId = chapterId,
            questionCount = questionCount,
            difficulty = difficulty
        )

        val questions = mutableListOf<QuizQuestion>()
        try {
            val raw = buffer.toString()
            val startIdx = raw.indexOf('[')
            val endIdx = raw.lastIndexOf(']')
            if (startIdx != -1 && endIdx != -1 && endIdx > startIdx) {
                val jsonArr = JSONArray(raw.substring(startIdx, endIdx + 1))
                for (i in 0 until jsonArr.length()) {
                    val obj = jsonArr.getJSONObject(i)
                    val opts = mutableListOf<String>()
                    val optsArr = obj.getJSONArray("options")
                    for (j in 0 until optsArr.length()) {
                        opts.add(optsArr.getString(j))
                    }
                    questions.add(
                        QuizQuestion(
                            quizId = quizId,
                            question = obj.getString("question"),
                            options = opts,
                            correctAnswer = obj.getString("correctAnswer"),
                            explanation = obj.optString("explanation", null),
                            type = QuestionType.MCQ,
                            topic = obj.optString("topic", topic),
                            orderIndex = i
                        )
                    )
                }
            }
        } catch (_: Exception) {}

        return Pair(quiz, questions)
    }
}
