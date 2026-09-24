package com.studyos.app.features

import com.studyos.app.core.datastore.PreferencesDataSource
import com.studyos.app.core.model.AppState
import com.studyos.app.core.model.AppTheme
import com.studyos.app.domain.model.AiConfig
import com.studyos.app.domain.model.Student
import com.studyos.app.domain.model.StudyPreferences
import com.studyos.app.domain.model.Subject
import com.studyos.app.domain.repository.StudentRepository
import com.studyos.app.domain.repository.StudyPreferencesRepository
import com.studyos.app.domain.usecase.AddSubjectUseCase
import com.studyos.app.domain.usecase.SaveStudentUseCase
import com.studyos.app.domain.usecase.SaveStudyPreferencesUseCase
import com.studyos.app.domain.usecase.SaveSubjectsUseCase
import com.studyos.app.features.onboarding.viewmodel.OnboardingViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeStudentRepository : StudentRepository {
    var savedStudent: Student? = null
    override fun getStudent(): Flow<Student?> = flowOf(savedStudent)
    override suspend fun getStudentOnce(): Student? = savedStudent
    override suspend fun saveStudent(student: Student) {
        savedStudent = student
    }
    override suspend fun deleteStudent(student: Student) {
        savedStudent = null
    }
}

class FakeStudyPreferencesRepository : StudyPreferencesRepository {
    var savedPreferences: StudyPreferences? = null
    override fun getPreferences(): Flow<StudyPreferences?> = flowOf(savedPreferences)
    override suspend fun getPreferencesOnce(): StudyPreferences? = savedPreferences
    override suspend fun savePreferences(preferences: StudyPreferences) {
        savedPreferences = preferences
    }
}

class FakePreferencesDataSource : PreferencesDataSource {
    val themeState = MutableStateFlow(AppTheme.SYSTEM)
    val onboardingCompletedState = MutableStateFlow(false)
    val appStateFlow = MutableStateFlow(AppState())
    val aiConfigState = MutableStateFlow(AiConfig())

    override val appState: Flow<AppState> = appStateFlow
    override val themePreference: Flow<AppTheme> = themeState
    override val isOnboardingCompleted: Flow<Boolean> = onboardingCompletedState
    override val aiConfig: Flow<AiConfig> = aiConfigState
    override val revisionStreakDays: Flow<Int> = flowOf(0)
    override val dailyRevisionTargetMinutes: Flow<Int> = flowOf(15)
    override val lastRevisionEpochDay: Flow<Long> = flowOf(0L)
    override val studyRemindersEnabled: Flow<Boolean> = flowOf(true)
    override val dailyReminderEnabled: Flow<Boolean> = flowOf(true)
    override val dailyReminderTime: Flow<String> = flowOf("19:00")
    override val revisionRemindersEnabled: Flow<Boolean> = flowOf(true)

    override suspend fun setThemePreference(theme: AppTheme) {
        themeState.value = theme
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        onboardingCompletedState.value = completed
    }

    override suspend fun resetOnboarding() {
        onboardingCompletedState.value = false
    }

    override suspend fun saveAiConfig(config: AiConfig) {
        aiConfigState.value = config
    }

    override suspend fun updateRevisionStreak(todayEpochDay: Long): Int = 1

    override suspend fun setDailyRevisionTargetMinutes(minutes: Int) {}
    override suspend fun setStudyRemindersEnabled(enabled: Boolean) {}
    override suspend fun setDailyReminderEnabled(enabled: Boolean) {}
    override suspend fun setDailyReminderTime(time: String) {}
    override suspend fun setRevisionRemindersEnabled(enabled: Boolean) {}
    override suspend fun resetAll() {}
}

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var studentRepo: FakeStudentRepository
    private lateinit var subjectRepo: com.studyos.app.domain.FakeSubjectRepository
    private lateinit var prefsRepo: FakeStudyPreferencesRepository
    private lateinit var prefsDataSource: FakePreferencesDataSource

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        studentRepo = FakeStudentRepository()
        subjectRepo = com.studyos.app.domain.FakeSubjectRepository()
        prefsRepo = FakeStudyPreferencesRepository()
        prefsDataSource = FakePreferencesDataSource()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testValidateProfile_emptyNameFails() {
        val vm = OnboardingViewModel(
            saveStudentUseCase = SaveStudentUseCase(studentRepo),
            saveSubjectsUseCase = SaveSubjectsUseCase(subjectRepo),
            addSubjectUseCase = AddSubjectUseCase(subjectRepo),
            saveStudyPreferencesUseCase = SaveStudyPreferencesUseCase(prefsRepo),
            preferencesDataSource = prefsDataSource
        )

        vm.onNameChanged("")
        val valid = vm.validateProfile()
        assertFalse(valid)
        assertNotNull(vm.uiState.value.nameError)

        vm.onNameChanged("Aarav Patel")
        val validAfter = vm.validateProfile()
        assertTrue(validAfter)
        assertEquals(null, vm.uiState.value.nameError)
    }

    @Test
    fun testAddCustomSubject_duplicateFails() {
        val vm = OnboardingViewModel(
            saveStudentUseCase = SaveStudentUseCase(studentRepo),
            saveSubjectsUseCase = SaveSubjectsUseCase(subjectRepo),
            addSubjectUseCase = AddSubjectUseCase(subjectRepo),
            saveStudyPreferencesUseCase = SaveStudyPreferencesUseCase(prefsRepo),
            preferencesDataSource = prefsDataSource
        )

        // Mathematics already in default suggested list
        val addedDuplicate = vm.addCustomSubject("mathematics")
        assertFalse(addedDuplicate)
        assertEquals("That subject already exists.", vm.uiState.value.customSubjectError)

        // Adding a new custom subject succeeds
        val addedNew = vm.addCustomSubject("Economics")
        assertTrue(addedNew)
        assertTrue(vm.uiState.value.selectedSubjects.contains("Economics"))
    }

    @Test
    fun testFinishSetup_persistsEverythingAndCompletesOnboarding() = runTest {
        val vm = OnboardingViewModel(
            saveStudentUseCase = SaveStudentUseCase(studentRepo),
            saveSubjectsUseCase = SaveSubjectsUseCase(subjectRepo),
            addSubjectUseCase = AddSubjectUseCase(subjectRepo),
            saveStudyPreferencesUseCase = SaveStudyPreferencesUseCase(prefsRepo),
            preferencesDataSource = prefsDataSource
        )

        vm.onNameChanged("Rohan Sharma")
        vm.onClassLevelChanged("Grade 10")
        vm.onDivisionChanged("B")
        vm.onSchoolNameChanged("Delhi Public School")
        vm.onDailyGoalChanged(90)
        vm.onDefaultSessionChanged(60)

        var completed = false
        vm.finishSetup {
            completed = true
        }

        advanceUntilIdle()

        assertTrue(completed)
        assertEquals("Rohan Sharma", studentRepo.savedStudent?.name)
        assertEquals("Grade 10", studentRepo.savedStudent?.classLevel)
        assertEquals("B", studentRepo.savedStudent?.division)
        assertEquals(90, prefsRepo.savedPreferences?.dailyStudyGoalMinutes)
        assertEquals(60, prefsRepo.savedPreferences?.defaultSessionMinutes)
        assertTrue(prefsDataSource.onboardingCompletedState.value)
    }
}
