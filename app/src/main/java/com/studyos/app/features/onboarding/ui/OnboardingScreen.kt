package com.studyos.app.features.onboarding.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.studyos.app.features.onboarding.viewmodel.OnboardingViewModel

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onFinishOnboarding: () -> Unit,
    onExitApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var currentStep by rememberSaveable { mutableIntStateOf(0) }

    // Android back navigation handling
    BackHandler(enabled = true) {
        if (currentStep > 0) {
            currentStep -= 1
        } else {
            onExitApp()
        }
    }

    AnimatedContent(
        targetState = currentStep,
        transitionSpec = {
            fadeIn(animationSpec = tween(180)) togetherWith fadeOut(animationSpec = tween(140))
        },
        label = "OnboardingTransition",
        modifier = modifier.fillMaxSize()
    ) { step ->
        when (step) {
            0 -> WelcomeStep(
                onGetStarted = { currentStep = 1 }
            )

            1 -> ProfileStep(
                uiState = uiState,
                onNameChanged = viewModel::onNameChanged,
                onClassLevelChanged = viewModel::onClassLevelChanged,
                onDivisionChanged = viewModel::onDivisionChanged,
                onSchoolNameChanged = viewModel::onSchoolNameChanged,
                onContinue = {
                    if (viewModel.validateProfile()) {
                        currentStep = 2
                    }
                },
                onBack = { currentStep = 0 }
            )

            2 -> SubjectsStep(
                uiState = uiState,
                onSubjectToggled = viewModel::onSubjectToggled,
                onAddCustomSubject = viewModel::addCustomSubject,
                onClearCustomError = viewModel::clearCustomSubjectError,
                onContinue = {
                    if (viewModel.validateSubjects()) {
                        currentStep = 3
                    }
                },
                onBack = { currentStep = 1 }
            )

            3 -> PreferencesStep(
                uiState = uiState,
                onDailyGoalChanged = viewModel::onDailyGoalChanged,
                onDefaultSessionChanged = viewModel::onDefaultSessionChanged,
                onContinue = { currentStep = 4 },
                onBack = { currentStep = 2 }
            )

            4 -> ReviewStep(
                uiState = uiState,
                onFinishSetup = {
                    viewModel.finishSetup(onSuccess = onFinishOnboarding)
                },
                onBack = { currentStep = 3 }
            )
        }
    }
}
