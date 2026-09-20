package com.studyos.app.core

import android.content.Context
import com.studyos.app.core.database.StudyOSDatabase
import com.studyos.app.core.datastore.PreferencesDataSource
import com.studyos.app.core.datastore.StudyOSPreferencesDataSource
import com.studyos.app.data.repository.ChapterRepositoryImpl
import com.studyos.app.data.repository.StudentRepositoryImpl
import com.studyos.app.data.repository.StudyPreferencesRepositoryImpl
import com.studyos.app.data.repository.SubjectRepositoryImpl
import com.studyos.app.domain.repository.ChapterRepository
import com.studyos.app.domain.repository.StudentRepository
import com.studyos.app.domain.repository.StudyPreferencesRepository
import com.studyos.app.domain.repository.SubjectRepository
import com.studyos.app.domain.usecase.AddChapterUseCase
import com.studyos.app.domain.usecase.AddSubjectUseCase
import com.studyos.app.domain.usecase.DeleteChapterUseCase
import com.studyos.app.domain.usecase.DeleteSubjectUseCase
import com.studyos.app.domain.usecase.GetAcademicProgressUseCase
import com.studyos.app.domain.usecase.GetChapterUseCase
import com.studyos.app.domain.usecase.GetChaptersForSubjectUseCase
import com.studyos.app.domain.usecase.GetStudentUseCase
import com.studyos.app.domain.usecase.GetStudyPreferencesUseCase
import com.studyos.app.domain.usecase.GetSubjectByIdUseCase
import com.studyos.app.domain.usecase.GetSubjectsUseCase
import com.studyos.app.domain.usecase.GetSubjectsWithProgressUseCase
import com.studyos.app.domain.usecase.LoadSampleDataUseCase
import com.studyos.app.domain.usecase.MoveChapterUseCase
import com.studyos.app.domain.usecase.RenameSubjectUseCase
import com.studyos.app.domain.usecase.ReorderChaptersUseCase
import com.studyos.app.domain.usecase.SaveStudentUseCase
import com.studyos.app.domain.usecase.SaveStudyPreferencesUseCase
import com.studyos.app.domain.usecase.SaveSubjectsUseCase
import com.studyos.app.domain.usecase.UpdateChapterProgressUseCase
import com.studyos.app.domain.usecase.UpdateChapterStatusUseCase
import com.studyos.app.domain.usecase.UpdateChapterUseCase

class StudyOSAppContainer(private val context: Context) {

    // Database & DataStore
    val database: StudyOSDatabase by lazy {
        StudyOSDatabase.getDatabase(context)
    }

    val preferencesDataSource: PreferencesDataSource by lazy {
        StudyOSPreferencesDataSource(context)
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
}
