package com.studyos.app.core

import android.content.Context
import com.studyos.app.core.database.StudyOSDatabase
import com.studyos.app.core.datastore.PreferencesDataSource
import com.studyos.app.core.datastore.StudyOSPreferencesDataSource
import com.studyos.app.data.repository.StudentRepositoryImpl
import com.studyos.app.data.repository.StudyPreferencesRepositoryImpl
import com.studyos.app.data.repository.SubjectRepositoryImpl
import com.studyos.app.domain.repository.StudentRepository
import com.studyos.app.domain.repository.StudyPreferencesRepository
import com.studyos.app.domain.repository.SubjectRepository
import com.studyos.app.domain.usecase.AddSubjectUseCase
import com.studyos.app.domain.usecase.DeleteSubjectUseCase
import com.studyos.app.domain.usecase.GetStudentUseCase
import com.studyos.app.domain.usecase.GetStudyPreferencesUseCase
import com.studyos.app.domain.usecase.GetSubjectsUseCase
import com.studyos.app.domain.usecase.RenameSubjectUseCase
import com.studyos.app.domain.usecase.SaveStudentUseCase
import com.studyos.app.domain.usecase.SaveStudyPreferencesUseCase
import com.studyos.app.domain.usecase.SaveSubjectsUseCase

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

    val subjectRepository: SubjectRepository by lazy {
        SubjectRepositoryImpl(database.subjectDao())
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
}
