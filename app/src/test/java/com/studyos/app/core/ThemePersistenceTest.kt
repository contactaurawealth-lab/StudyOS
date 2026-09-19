package com.studyos.app.core

import com.studyos.app.core.model.AppTheme
import com.studyos.app.domain.usecase.AddSubjectUseCase
import com.studyos.app.domain.usecase.DeleteSubjectUseCase
import com.studyos.app.domain.usecase.GetStudentUseCase
import com.studyos.app.domain.usecase.GetStudyPreferencesUseCase
import com.studyos.app.domain.usecase.GetSubjectsUseCase
import com.studyos.app.domain.usecase.RenameSubjectUseCase
import com.studyos.app.domain.usecase.SaveStudentUseCase
import com.studyos.app.domain.usecase.SaveStudyPreferencesUseCase
import com.studyos.app.features.FakePreferencesDataSource
import com.studyos.app.features.FakeStudentRepository
import com.studyos.app.features.FakeStudyPreferencesRepository
import com.studyos.app.features.settings.viewmodel.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ThemePersistenceTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var prefsDataSource: FakePreferencesDataSource
    private lateinit var studentRepo: FakeStudentRepository
    private lateinit var subjectRepo: com.studyos.app.domain.FakeSubjectRepository
    private lateinit var studyPrefsRepo: FakeStudyPreferencesRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        prefsDataSource = FakePreferencesDataSource()
        studentRepo = FakeStudentRepository()
        subjectRepo = com.studyos.app.domain.FakeSubjectRepository()
        studyPrefsRepo = FakeStudyPreferencesRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testThemeSwitchingAndPersistenceAcrossSessions() = runTest {
        // Session 1: Launch app
        val viewModelSession1 = SettingsViewModel(
            getStudentUseCase = GetStudentUseCase(studentRepo),
            saveStudentUseCase = SaveStudentUseCase(studentRepo),
            getSubjectsUseCase = GetSubjectsUseCase(subjectRepo),
            addSubjectUseCase = AddSubjectUseCase(subjectRepo),
            renameSubjectUseCase = RenameSubjectUseCase(subjectRepo),
            deleteSubjectUseCase = DeleteSubjectUseCase(subjectRepo),
            getStudyPreferencesUseCase = GetStudyPreferencesUseCase(studyPrefsRepo),
            saveStudyPreferencesUseCase = SaveStudyPreferencesUseCase(studyPrefsRepo),
            preferencesDataSource = prefsDataSource
        )

        advanceUntilIdle()
        // Default theme is SYSTEM
        assertEquals(AppTheme.SYSTEM, viewModelSession1.uiState.value.currentTheme)

        // Step 2: Change theme to DARK
        viewModelSession1.setTheme(AppTheme.DARK)
        advanceUntilIdle()

        // Verify state is DARK
        assertEquals(AppTheme.DARK, prefsDataSource.themeState.value)

        // Step 3 & 4: Simulate Process Kill & Relaunch by creating Session 2 with existing preferences
        val viewModelSession2 = SettingsViewModel(
            getStudentUseCase = GetStudentUseCase(studentRepo),
            saveStudentUseCase = SaveStudentUseCase(studentRepo),
            getSubjectsUseCase = GetSubjectsUseCase(subjectRepo),
            addSubjectUseCase = AddSubjectUseCase(subjectRepo),
            renameSubjectUseCase = RenameSubjectUseCase(subjectRepo),
            deleteSubjectUseCase = DeleteSubjectUseCase(subjectRepo),
            getStudyPreferencesUseCase = GetStudyPreferencesUseCase(studyPrefsRepo),
            saveStudyPreferencesUseCase = SaveStudyPreferencesUseCase(studyPrefsRepo),
            preferencesDataSource = prefsDataSource
        )

        advanceUntilIdle()

        // Step 5: Verify theme remains DARK in the new session
        assertEquals(AppTheme.DARK, viewModelSession2.uiState.value.currentTheme)
    }
}
