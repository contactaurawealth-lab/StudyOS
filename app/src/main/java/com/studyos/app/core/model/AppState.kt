package com.studyos.app.core.model

data class AppState(
    val isFirstLaunch: Boolean = true,
    val isOnboardingCompleted: Boolean = false,
    val theme: AppTheme = AppTheme.SYSTEM,
    val isOffline: Boolean = true,
    val isLoading: Boolean = true
)
