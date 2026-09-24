package com.studyos.app.core

import android.content.Context
import com.studyos.app.core.database.StudyOSDatabase
import com.studyos.app.core.datastore.PreferencesDataSource
import com.studyos.app.core.datastore.StudyOSPreferencesDataSource
import com.studyos.app.data.repository.ChapterRepositoryImpl
import com.studyos.app.data.repository.PlannerRepositoryImpl
import com.studyos.app.data.repository.StudentRepositoryImpl
import com.studyos.app.data.repository.StudyPreferencesRepositoryImpl
import com.studyos.app.data.repository.StudySessionRepositoryImpl
import com.studyos.app.data.repository.SubjectRepositoryImpl
import com.studyos.app.data.repository.TaskRepositoryImpl
import com.studyos.app.domain.repository.ChapterRepository
import com.studyos.app.domain.repository.PlannerRepository
import com.studyos.app.domain.repository.StudentRepository
import com.studyos.app.domain.repository.StudyPreferencesRepository
import com.studyos.app.domain.repository.StudySessionRepository
import com.studyos.app.domain.repository.SubjectRepository
import com.studyos.app.domain.repository.TaskRepository
import com.studyos.app.domain.usecase.AddChapterUseCase
import com.studyos.app.domain.usecase.AddSubjectUseCase
import com.studyos.app.domain.usecase.CheckSessionOverlapUseCase
import com.studyos.app.domain.usecase.CreateTaskUseCase
import com.studyos.app.domain.usecase.DeleteChapterUseCase
import com.studyos.app.domain.usecase.DeletePlannerSessionUseCase
import com.studyos.app.domain.usecase.DeleteSessionUseCase
import com.studyos.app.domain.usecase.DeleteSubjectUseCase
import com.studyos.app.domain.usecase.DeleteTaskUseCase
import com.studyos.app.domain.usecase.GetAcademicProgressUseCase
import com.studyos.app.domain.usecase.GetChapterUseCase
import com.studyos.app.domain.usecase.GetChaptersForSubjectUseCase
import com.studyos.app.domain.usecase.GetStudentUseCase
import com.studyos.app.domain.usecase.GetStudyPreferencesUseCase
import com.studyos.app.domain.usecase.GetSubjectByIdUseCase
import com.studyos.app.domain.usecase.GetSubjectsUseCase
import com.studyos.app.domain.usecase.GetSubjectsWithProgressUseCase
import com.studyos.app.domain.usecase.GetTasksUseCase
import com.studyos.app.domain.usecase.GetTodayDataUseCase
import com.studyos.app.domain.usecase.GetTodayTasksUseCase
import com.studyos.app.domain.usecase.GetUpcomingScheduleUseCase
import com.studyos.app.domain.usecase.GetWeekScheduleUseCase
import com.studyos.app.domain.usecase.LoadSampleDataUseCase
import com.studyos.app.domain.usecase.MoveChapterUseCase
import com.studyos.app.domain.usecase.MoveSessionUseCase
import com.studyos.app.domain.usecase.PlanSessionUseCase
import com.studyos.app.domain.usecase.RecordChapterOpenedUseCase
import com.studyos.app.domain.usecase.RenameSubjectUseCase
import com.studyos.app.domain.usecase.ReorderChaptersUseCase
import com.studyos.app.data.provider.OpenAiCompatibleProvider
import com.studyos.app.data.repository.AiConversationRepositoryImpl
import com.studyos.app.data.repository.AiMessageRepositoryImpl
import com.studyos.app.domain.provider.AiProvider
import com.studyos.app.domain.repository.AiConversationRepository
import com.studyos.app.domain.repository.AiMessageRepository
import com.studyos.app.domain.usecase.CreateConversationUseCase
import com.studyos.app.domain.usecase.DeleteConversationUseCase
import com.studyos.app.domain.usecase.GetAiConfigUseCase
import com.studyos.app.domain.usecase.GetConversationUseCase
import com.studyos.app.domain.usecase.GetConversationsUseCase
import com.studyos.app.domain.usecase.GetMessagesUseCase
import com.studyos.app.domain.usecase.GetStudyContextUseCase
import com.studyos.app.domain.usecase.SaveAiConfigUseCase
import com.studyos.app.domain.usecase.SavePlannerSessionUseCase
import com.studyos.app.domain.usecase.SaveStudentUseCase
import com.studyos.app.domain.usecase.SaveStudyPreferencesUseCase
import com.studyos.app.domain.usecase.SaveSubjectsUseCase
import com.studyos.app.domain.usecase.SendAiMessageUseCase
import com.studyos.app.domain.usecase.ToggleTaskCompletionUseCase
import com.studyos.app.domain.usecase.UpdateChapterProgressUseCase
import com.studyos.app.domain.usecase.UpdateChapterStatusUseCase
import com.studyos.app.domain.usecase.UpdateChapterUseCase
import com.studyos.app.domain.usecase.UpdateSessionStatusUseCase
import com.studyos.app.domain.usecase.UpdateTaskUseCase

class StudyOSAppContainer(private val context: Context) {

    // Database & DataStore
    val database: StudyOSDatabase by lazy {
        StudyOSDatabase.getDatabase(context)
    }

    val preferencesDataSource: PreferencesDataSource by lazy {
        StudyOSPreferencesDataSource(context)
    }

    val alarmScheduler: com.studyos.app.core.notification.AlarmScheduler by lazy {
        com.studyos.app.core.notification.AlarmScheduler(context)
    }

    // Repositories
    val studentRepository: StudentRepository by lazy {
        StudentRepositoryImpl(database.studentDao())
    }

    val chapterRepository: ChapterRepository by lazy {
        ChapterRepositoryImpl(database.chapterDao())
    }

    val subjectRepository: SubjectRepository by lazy {
        SubjectRepositoryImpl(database.subjectDao(), database.chapterDao())
    }

    val studyPreferencesRepository: StudyPreferencesRepository by lazy {
        StudyPreferencesRepositoryImpl(database.studyPreferencesDao())
    }

    val studySessionRepository: StudySessionRepository by lazy {
        StudySessionRepositoryImpl(database.studySessionDao())
    }

    val taskRepository: TaskRepository by lazy {
        TaskRepositoryImpl(database.taskDao())
    }

    val plannerRepository: PlannerRepository by lazy {
        PlannerRepositoryImpl(database.studySessionDao())
    }

    val studyTimerViewModel: com.studyos.app.features.timer.viewmodel.StudyTimerViewModel by lazy {
        com.studyos.app.features.timer.viewmodel.StudyTimerViewModel(
            context = context.applicationContext,
            studySessionRepository = studySessionRepository,
            subjectRepository = subjectRepository,
            chapterRepository = chapterRepository
        )
    }

    // Use Cases
    val getStudentUseCase: GetStudentUseCase by lazy {
        GetStudentUseCase(studentRepository)
    }

    val saveStudentUseCase: SaveStudentUseCase by lazy {
        SaveStudentUseCase(studentRepository)
    }

    val getSubjectsUseCase: GetSubjectsUseCase by lazy {
        GetSubjectsUseCase(subjectRepository)
    }

    val getSubjectsWithProgressUseCase: GetSubjectsWithProgressUseCase by lazy {
        GetSubjectsWithProgressUseCase(subjectRepository)
    }

    val getSubjectByIdUseCase: GetSubjectByIdUseCase by lazy {
        GetSubjectByIdUseCase(subjectRepository)
    }

    val saveSubjectsUseCase: SaveSubjectsUseCase by lazy {
        SaveSubjectsUseCase(subjectRepository)
    }

    val addSubjectUseCase: AddSubjectUseCase by lazy {
        AddSubjectUseCase(subjectRepository)
    }

    val renameSubjectUseCase: RenameSubjectUseCase by lazy {
        RenameSubjectUseCase(subjectRepository)
    }

    val deleteSubjectUseCase: DeleteSubjectUseCase by lazy {
        DeleteSubjectUseCase(subjectRepository)
    }

    val getStudyPreferencesUseCase: GetStudyPreferencesUseCase by lazy {
        GetStudyPreferencesUseCase(studyPreferencesRepository)
    }

    val saveStudyPreferencesUseCase: SaveStudyPreferencesUseCase by lazy {
        SaveStudyPreferencesUseCase(studyPreferencesRepository)
    }

    // Chapter Use Cases
    val getChaptersForSubjectUseCase: GetChaptersForSubjectUseCase by lazy {
        GetChaptersForSubjectUseCase(chapterRepository)
    }

    val getChapterUseCase: GetChapterUseCase by lazy {
        GetChapterUseCase(chapterRepository)
    }

    val addChapterUseCase: AddChapterUseCase by lazy {
        AddChapterUseCase(chapterRepository)
    }

    val updateChapterUseCase: UpdateChapterUseCase by lazy {
        UpdateChapterUseCase(chapterRepository)
    }

    val updateChapterProgressUseCase: UpdateChapterProgressUseCase by lazy {
        UpdateChapterProgressUseCase(chapterRepository)
    }

    val updateChapterStatusUseCase: UpdateChapterStatusUseCase by lazy {
        UpdateChapterStatusUseCase(chapterRepository)
    }

    val deleteChapterUseCase: DeleteChapterUseCase by lazy {
        DeleteChapterUseCase(chapterRepository)
    }

    val reorderChaptersUseCase: ReorderChaptersUseCase by lazy {
        ReorderChaptersUseCase(chapterRepository)
    }

    val moveChapterUseCase: MoveChapterUseCase by lazy {
        MoveChapterUseCase(chapterRepository)
    }

    val loadSampleDataUseCase: LoadSampleDataUseCase by lazy {
        LoadSampleDataUseCase(subjectRepository, chapterRepository)
    }

    val getAcademicProgressUseCase: GetAcademicProgressUseCase by lazy {
        GetAcademicProgressUseCase(subjectRepository, chapterRepository)
    }

    val recordChapterOpenedUseCase: RecordChapterOpenedUseCase by lazy {
        RecordChapterOpenedUseCase(chapterRepository)
    }

    // Today / Session Use Cases
    val getTodayDataUseCase: GetTodayDataUseCase by lazy {
        GetTodayDataUseCase(
            studentRepository = studentRepository,
            studyPreferencesRepository = studyPreferencesRepository,
            subjectRepository = subjectRepository,
            chapterRepository = chapterRepository,
            studySessionRepository = studySessionRepository
        )
    }

    val planSessionUseCase: PlanSessionUseCase by lazy {
        PlanSessionUseCase(
            studySessionRepository = studySessionRepository,
            subjectRepository = subjectRepository,
            chapterRepository = chapterRepository
        )
    }

    val deleteSessionUseCase: DeleteSessionUseCase by lazy {
        DeleteSessionUseCase(studySessionRepository)
    }

    val updateSessionStatusUseCase: UpdateSessionStatusUseCase by lazy {
        UpdateSessionStatusUseCase(studySessionRepository)
    }

    // Task Use Cases
    val getTasksUseCase: GetTasksUseCase by lazy {
        GetTasksUseCase(taskRepository, subjectRepository, chapterRepository)
    }

    val getTodayTasksUseCase: GetTodayTasksUseCase by lazy {
        GetTodayTasksUseCase(taskRepository, subjectRepository, chapterRepository)
    }

    val createTaskUseCase: CreateTaskUseCase by lazy {
        CreateTaskUseCase(taskRepository)
    }

    val updateTaskUseCase: UpdateTaskUseCase by lazy {
        UpdateTaskUseCase(taskRepository)
    }

    val toggleTaskCompletionUseCase: ToggleTaskCompletionUseCase by lazy {
        ToggleTaskCompletionUseCase(taskRepository)
    }

    val deleteTaskUseCase: DeleteTaskUseCase by lazy {
        DeleteTaskUseCase(taskRepository)
    }

    // Planner Use Cases
    val getWeekScheduleUseCase: GetWeekScheduleUseCase by lazy {
        GetWeekScheduleUseCase(plannerRepository, subjectRepository, chapterRepository)
    }

    val getUpcomingScheduleUseCase: GetUpcomingScheduleUseCase by lazy {
        GetUpcomingScheduleUseCase(plannerRepository, subjectRepository, chapterRepository)
    }

    val checkSessionOverlapUseCase: CheckSessionOverlapUseCase by lazy {
        CheckSessionOverlapUseCase(plannerRepository)
    }

    val moveSessionUseCase: MoveSessionUseCase by lazy {
        MoveSessionUseCase(plannerRepository)
    }

    val savePlannerSessionUseCase: SavePlannerSessionUseCase by lazy {
        SavePlannerSessionUseCase(plannerRepository, subjectRepository, chapterRepository)
    }

    val deletePlannerSessionUseCase: DeletePlannerSessionUseCase by lazy {
        DeletePlannerSessionUseCase(plannerRepository)
    }

    // AI Repositories & Provider
    val aiConversationRepository: AiConversationRepository by lazy {
        AiConversationRepositoryImpl(database.aiConversationDao())
    }

    val aiMessageRepository: AiMessageRepository by lazy {
        AiMessageRepositoryImpl(database.aiMessageDao())
    }

    val aiProvider: AiProvider by lazy {
        OpenAiCompatibleProvider()
    }

    // AI Use Cases
    val getConversationsUseCase: GetConversationsUseCase by lazy {
        GetConversationsUseCase(aiConversationRepository, subjectRepository, chapterRepository, aiMessageRepository)
    }

    val getConversationUseCase: GetConversationUseCase by lazy {
        GetConversationUseCase(aiConversationRepository)
    }

    val getMessagesUseCase: GetMessagesUseCase by lazy {
        GetMessagesUseCase(aiMessageRepository)
    }

    val createConversationUseCase: CreateConversationUseCase by lazy {
        CreateConversationUseCase(aiConversationRepository, subjectRepository, chapterRepository)
    }

    val deleteConversationUseCase: DeleteConversationUseCase by lazy {
        DeleteConversationUseCase(aiConversationRepository, aiMessageRepository)
    }

    val getStudyContextUseCase: GetStudyContextUseCase by lazy {
        GetStudyContextUseCase(subjectRepository, chapterRepository, noteRepository)
    }

    val sendAiMessageUseCase: SendAiMessageUseCase by lazy {
        SendAiMessageUseCase(aiMessageRepository, aiConversationRepository, aiProvider, preferencesDataSource)
    }

    val saveAiConfigUseCase: SaveAiConfigUseCase by lazy {
        SaveAiConfigUseCase(preferencesDataSource)
    }

    val getAiConfigUseCase: GetAiConfigUseCase by lazy {
        GetAiConfigUseCase(preferencesDataSource)
    }

    // Practice, Revision & Exam Prep Repositories
    val noteRepository: com.studyos.app.domain.repository.NoteRepository by lazy {
        com.studyos.app.data.repository.NoteRepositoryImpl(database.noteDao())
    }

    val flashcardRepository: com.studyos.app.domain.repository.FlashcardRepository by lazy {
        com.studyos.app.data.repository.FlashcardRepositoryImpl(database.flashcardDao())
    }

    val quizRepository: com.studyos.app.domain.repository.QuizRepository by lazy {
        com.studyos.app.data.repository.QuizRepositoryImpl(database.quizDao())
    }

    val mistakeRepository: com.studyos.app.domain.repository.MistakeRepository by lazy {
        com.studyos.app.data.repository.MistakeRepositoryImpl(database.mistakeDao())
    }

    val examRepository: com.studyos.app.domain.repository.ExamRepository by lazy {
        com.studyos.app.data.repository.ExamRepositoryImpl(database.examDao())
    }

    // Practice & Revision Use Cases
    val getNotesForChapterUseCase: com.studyos.app.domain.usecase.GetNotesForChapterUseCase by lazy {
        com.studyos.app.domain.usecase.GetNotesForChapterUseCase(noteRepository)
    }

    val getNoteUseCase: com.studyos.app.domain.usecase.GetNoteUseCase by lazy {
        com.studyos.app.domain.usecase.GetNoteUseCase(noteRepository)
    }

    val saveNoteUseCase: com.studyos.app.domain.usecase.SaveNoteUseCase by lazy {
        com.studyos.app.domain.usecase.SaveNoteUseCase(noteRepository)
    }

    val deleteNoteUseCase: com.studyos.app.domain.usecase.DeleteNoteUseCase by lazy {
        com.studyos.app.domain.usecase.DeleteNoteUseCase(noteRepository)
    }

    val toggleNotePinUseCase: com.studyos.app.domain.usecase.ToggleNotePinUseCase by lazy {
        com.studyos.app.domain.usecase.ToggleNotePinUseCase(noteRepository)
    }

    val getFlashcardsForChapterUseCase: com.studyos.app.domain.usecase.GetFlashcardsForChapterUseCase by lazy {
        com.studyos.app.domain.usecase.GetFlashcardsForChapterUseCase(flashcardRepository)
    }

    val getDueFlashcardsUseCase: com.studyos.app.domain.usecase.GetDueFlashcardsUseCase by lazy {
        com.studyos.app.domain.usecase.GetDueFlashcardsUseCase(flashcardRepository)
    }

    val saveFlashcardUseCase: com.studyos.app.domain.usecase.SaveFlashcardUseCase by lazy {
        com.studyos.app.domain.usecase.SaveFlashcardUseCase(flashcardRepository)
    }

    val reviewFlashcardUseCase: com.studyos.app.domain.usecase.ReviewFlashcardUseCase by lazy {
        com.studyos.app.domain.usecase.ReviewFlashcardUseCase(flashcardRepository)
    }

    val deleteFlashcardUseCase: com.studyos.app.domain.usecase.DeleteFlashcardUseCase by lazy {
        com.studyos.app.domain.usecase.DeleteFlashcardUseCase(flashcardRepository)
    }

    val saveQuizUseCase: com.studyos.app.domain.usecase.SaveQuizUseCase by lazy {
        com.studyos.app.domain.usecase.SaveQuizUseCase(quizRepository)
    }

    val getQuizWithQuestionsUseCase: com.studyos.app.domain.usecase.GetQuizWithQuestionsUseCase by lazy {
        com.studyos.app.domain.usecase.GetQuizWithQuestionsUseCase(quizRepository)
    }

    val submitQuizAttemptUseCase: com.studyos.app.domain.usecase.SubmitQuizAttemptUseCase by lazy {
        com.studyos.app.domain.usecase.SubmitQuizAttemptUseCase(quizRepository, mistakeRepository)
    }

    val saveActiveQuizStateUseCase: com.studyos.app.domain.usecase.SaveActiveQuizStateUseCase by lazy {
        com.studyos.app.domain.usecase.SaveActiveQuizStateUseCase(quizRepository)
    }

    val getActiveQuizStateUseCase: com.studyos.app.domain.usecase.GetActiveQuizStateUseCase by lazy {
        com.studyos.app.domain.usecase.GetActiveQuizStateUseCase(quizRepository)
    }

    val getMistakesForChapterUseCase: com.studyos.app.domain.usecase.GetMistakesForChapterUseCase by lazy {
        com.studyos.app.domain.usecase.GetMistakesForChapterUseCase(mistakeRepository)
    }

    val getAllMistakesUseCase: com.studyos.app.domain.usecase.GetAllMistakesUseCase by lazy {
        com.studyos.app.domain.usecase.GetAllMistakesUseCase(mistakeRepository)
    }

    val resolveMistakeUseCase: com.studyos.app.domain.usecase.ResolveMistakeUseCase by lazy {
        com.studyos.app.domain.usecase.ResolveMistakeUseCase(mistakeRepository)
    }

    val convertMistakeToFlashcardUseCase: com.studyos.app.domain.usecase.ConvertMistakeToFlashcardUseCase by lazy {
        com.studyos.app.domain.usecase.ConvertMistakeToFlashcardUseCase(mistakeRepository, flashcardRepository)
    }

    val getChapterPracticeSummaryUseCase: com.studyos.app.domain.usecase.GetChapterPracticeSummaryUseCase by lazy {
        com.studyos.app.domain.usecase.GetChapterPracticeSummaryUseCase(
            chapterRepository,
            subjectRepository,
            noteRepository,
            flashcardRepository,
            quizRepository,
            mistakeRepository
        )
    }

    val getExamsUseCase: com.studyos.app.domain.usecase.GetExamsUseCase by lazy {
        com.studyos.app.domain.usecase.GetExamsUseCase(examRepository)
    }

    val saveExamUseCase: com.studyos.app.domain.usecase.SaveExamUseCase by lazy {
        com.studyos.app.domain.usecase.SaveExamUseCase(examRepository)
    }

    val deleteExamUseCase: com.studyos.app.domain.usecase.DeleteExamUseCase by lazy {
        com.studyos.app.domain.usecase.DeleteExamUseCase(examRepository)
    }

    val updateExamScoreUseCase: com.studyos.app.domain.usecase.UpdateExamScoreUseCase by lazy {
        com.studyos.app.domain.usecase.UpdateExamScoreUseCase(examRepository)
    }

    val getExamDashboardUseCase: com.studyos.app.domain.usecase.GetExamDashboardUseCase by lazy {
        com.studyos.app.domain.usecase.GetExamDashboardUseCase(
            examRepository,
            subjectRepository,
            chapterRepository,
            flashcardRepository,
            noteRepository,
            quizRepository,
            mistakeRepository
        )
    }

    val aiPracticeToolsUseCase: com.studyos.app.domain.usecase.AiPracticeToolsUseCase by lazy {
        com.studyos.app.domain.usecase.AiPracticeToolsUseCase(aiProvider)
    }

    // Phase 10 & 11: Revision & AI Engine
    val revisionRepository: com.studyos.app.domain.repository.RevisionRepository by lazy {
        com.studyos.app.data.repository.RevisionRepositoryImpl(database.revisionDao(), preferencesDataSource)
    }

    val identifyWeakTopicsUseCase: com.studyos.app.domain.usecase.IdentifyWeakTopicsUseCase by lazy {
        com.studyos.app.domain.usecase.IdentifyWeakTopicsUseCase(mistakeRepository)
    }

    val getChapterMasteryUseCase: com.studyos.app.domain.usecase.GetChapterMasteryUseCase by lazy {
        com.studyos.app.domain.usecase.GetChapterMasteryUseCase(
            chapterRepository,
            subjectRepository,
            quizRepository,
            flashcardRepository,
            mistakeRepository,
            revisionRepository
        )
    }

    val generateRevisionRecommendationsUseCase: com.studyos.app.domain.usecase.GenerateRevisionRecommendationsUseCase by lazy {
        com.studyos.app.domain.usecase.GenerateRevisionRecommendationsUseCase(
            chapterRepository,
            subjectRepository,
            mistakeRepository,
            quizRepository,
            revisionRepository
        )
    }

    val getDueRevisionDashboardUseCase: com.studyos.app.domain.usecase.GetDueRevisionDashboardUseCase by lazy {
        com.studyos.app.domain.usecase.GetDueRevisionDashboardUseCase(
            getChapterMasteryUseCase,
            identifyWeakTopicsUseCase,
            generateRevisionRecommendationsUseCase,
            flashcardRepository,
            revisionRepository
        )
    }

    val startActiveRecallSessionUseCase: com.studyos.app.domain.usecase.StartActiveRecallSessionUseCase by lazy {
        com.studyos.app.domain.usecase.StartActiveRecallSessionUseCase(
            flashcardRepository,
            mistakeRepository,
            quizRepository,
            chapterRepository
        )
    }

    val completeActiveRecallSessionUseCase: com.studyos.app.domain.usecase.CompleteActiveRecallSessionUseCase by lazy {
        com.studyos.app.domain.usecase.CompleteActiveRecallSessionUseCase(
            revisionRepository,
            flashcardRepository,
            mistakeRepository
        )
    }

    val getChapterAiContextUseCase: com.studyos.app.domain.usecase.GetChapterAiContextUseCase by lazy {
        com.studyos.app.domain.usecase.GetChapterAiContextUseCase(
            chapterRepository,
            subjectRepository,
            noteRepository,
            mistakeRepository,
            quizRepository,
            getChapterMasteryUseCase
        )
    }

    val aiStudyEngineUseCase: com.studyos.app.domain.usecase.AiStudyEngineUseCase by lazy {
        com.studyos.app.domain.usecase.AiStudyEngineUseCase(aiProvider, preferencesDataSource)
    }

    // AI Recall Engine & Intelligence Upgrade
    val recallRepository: com.studyos.app.domain.repository.RecallRepository by lazy {
        com.studyos.app.data.repository.RecallRepositoryImpl(
            recallDao = database.recallDao(),
            flashcardDao = database.flashcardDao(),
            mistakeDao = database.mistakeDao()
        )
    }

    val getSmartStudyRecommendationUseCase: com.studyos.app.domain.usecase.GetSmartStudyRecommendationUseCase by lazy {
        com.studyos.app.domain.usecase.GetSmartStudyRecommendationUseCase(
            subjectRepository = subjectRepository,
            chapterRepository = chapterRepository,
            examRepository = examRepository,
            recallRepository = recallRepository,
            mistakeRepository = mistakeRepository,
            quizRepository = quizRepository,
            studyPreferencesRepository = studyPreferencesRepository
        )
    }

    val getChapterIntelligenceUseCase: com.studyos.app.domain.usecase.GetChapterIntelligenceUseCase by lazy {
        com.studyos.app.domain.usecase.GetChapterIntelligenceUseCase(
            chapterRepository = chapterRepository,
            subjectRepository = subjectRepository,
            noteRepository = noteRepository,
            flashcardRepository = flashcardRepository,
            quizRepository = quizRepository,
            mistakeRepository = mistakeRepository,
            recallRepository = recallRepository
        )
    }

    val getRecallDashboardUseCase: com.studyos.app.domain.usecase.GetRecallDashboardUseCase by lazy {
        com.studyos.app.domain.usecase.GetRecallDashboardUseCase(recallRepository)
    }

    val submitRecallAnswerUseCase: com.studyos.app.domain.usecase.SubmitRecallAnswerUseCase by lazy {
        com.studyos.app.domain.usecase.SubmitRecallAnswerUseCase(recallRepository)
    }

    val getDailyAiPlanUseCase: com.studyos.app.domain.usecase.GetDailyAiPlanUseCase by lazy {
        com.studyos.app.domain.usecase.GetDailyAiPlanUseCase(
            subjectRepository = subjectRepository,
            chapterRepository = chapterRepository,
            recallRepository = recallRepository,
            mistakeRepository = mistakeRepository,
            getChapterIntelligenceUseCase = getChapterIntelligenceUseCase
        )
    }

    val getOverallExamReadinessUseCase: com.studyos.app.domain.usecase.GetOverallExamReadinessUseCase by lazy {
        com.studyos.app.domain.usecase.GetOverallExamReadinessUseCase(
            subjectRepository = subjectRepository,
            chapterRepository = chapterRepository,
            examRepository = examRepository,
            recallRepository = recallRepository,
            mistakeRepository = mistakeRepository,
            getChapterIntelligenceUseCase = getChapterIntelligenceUseCase
        )
    }

    val getSubjectReadinessUseCase: com.studyos.app.domain.usecase.GetSubjectReadinessUseCase by lazy {
        com.studyos.app.domain.usecase.GetSubjectReadinessUseCase(
            subjectRepository = subjectRepository,
            chapterRepository = chapterRepository,
            examRepository = examRepository,
            recallRepository = recallRepository,
            mistakeRepository = mistakeRepository,
            getChapterIntelligenceUseCase = getChapterIntelligenceUseCase
        )
    }
}

