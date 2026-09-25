package com.studyos.app.domain.repository

import com.studyos.app.domain.model.ActiveQuizState
import com.studyos.app.domain.model.Exam
import com.studyos.app.domain.model.Flashcard
import com.studyos.app.domain.model.FlashcardRating
import com.studyos.app.domain.model.Mistake
import com.studyos.app.domain.model.Note
import com.studyos.app.domain.model.QuestionResult
import com.studyos.app.domain.model.Quiz
import com.studyos.app.domain.model.QuizAttempt
import com.studyos.app.domain.model.QuizQuestion
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun observeNotesForChapter(chapterId: String): Flow<List<Note>>
    fun observeNotesForSubject(subjectId: String): Flow<List<Note>>
    fun observeAllNotes(): Flow<List<Note>>
    fun getNoteById(id: String): Flow<Note?>
    suspend fun getNoteByIdOnce(id: String): Note?
    suspend fun saveNote(note: Note): Note
    suspend fun updateNote(note: Note)
    suspend fun deleteNote(id: String)
    suspend fun togglePin(id: String, isPinned: Boolean)
    fun searchNotes(query: String): Flow<List<Note>>
}

interface FlashcardRepository {
    fun observeFlashcardsForChapter(chapterId: String): Flow<List<Flashcard>>
    fun observeFlashcardsForSubject(subjectId: String): Flow<List<Flashcard>>
    fun observeDueFlashcards(currentTime: Long = System.currentTimeMillis()): Flow<List<Flashcard>>
    fun observeAllFlashcards(): Flow<List<Flashcard>>
    fun getFlashcardById(id: String): Flow<Flashcard?>
    suspend fun getFlashcardByIdOnce(id: String): Flashcard?
    suspend fun saveFlashcard(flashcard: Flashcard): Flashcard
    suspend fun saveFlashcards(flashcards: List<Flashcard>)
    suspend fun deleteFlashcard(id: String)
    suspend fun recordReview(cardId: String, rating: FlashcardRating): Flashcard
    fun searchFlashcards(query: String): Flow<List<Flashcard>>
}

interface QuizRepository {
    fun observeQuizzesForChapter(chapterId: String): Flow<List<Quiz>>
    fun observeQuizzesForSubject(subjectId: String): Flow<List<Quiz>>
    suspend fun getQuizById(id: String): Quiz?
    suspend fun getQuestionsForQuiz(quizId: String): List<QuizQuestion>
    suspend fun saveQuiz(quiz: Quiz, questions: List<QuizQuestion>): Quiz
    suspend fun deleteQuiz(quizId: String)
    suspend fun saveActiveState(state: ActiveQuizState)
    suspend fun getActiveState(quizId: String): ActiveQuizState?
    suspend fun clearActiveState(quizId: String)
    suspend fun recordAttempt(attempt: QuizAttempt, results: List<QuestionResult>): QuizAttempt
    fun getAttemptsForQuiz(quizId: String): Flow<List<QuizAttempt>>
    fun getAttemptsForChapter(chapterId: String): Flow<List<QuizAttempt>>
    fun getAllAttempts(): Flow<List<QuizAttempt>>
    suspend fun getAttemptResults(attemptId: String): List<QuestionResult>
}

interface MistakeRepository {
    fun observeMistakesForChapter(chapterId: String): Flow<List<Mistake>>
    fun observeMistakesForSubject(subjectId: String): Flow<List<Mistake>>
    fun observeAllMistakes(): Flow<List<Mistake>>
    suspend fun recordMistake(mistake: Mistake): Mistake
    suspend fun resolveMistake(id: String)
    suspend fun deleteMistake(id: String)
    fun searchMistakes(query: String): Flow<List<Mistake>>
}

interface ExamRepository {
    fun observeAllExams(): Flow<List<Exam>>
    fun getExamById(id: String): Flow<Exam?>
    suspend fun getExamByIdOnce(id: String): Exam?
    suspend fun saveExam(exam: Exam, subjectIds: List<String>): Exam
    suspend fun deleteExam(id: String)
    suspend fun updateExamScore(id: String, actualScore: Int?, isCompleted: Boolean)
}
