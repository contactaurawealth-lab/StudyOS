package com.studyos.app.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.studyos.app.core.model.AppTheme

private val LightColorScheme = lightColorScheme(
    primary = LightAccent,
    onPrimary = LightSurface,
    background = LightBackground,
    onBackground = LightPrimaryText,
    surface = LightSurface,
    onSurface = LightPrimaryText,
    surfaceVariant = LightBackground,
    onSurfaceVariant = LightSecondaryText,
    outline = LightBorder,
    outlineVariant = LightBorder,
    error = LightError,
    onError = LightSurface
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkAccent,
    onPrimary = DarkBackground,
    background = DarkBackground,
    onBackground = DarkPrimaryText,
    surface = DarkSurface,
    onSurface = DarkPrimaryText,
    surfaceVariant = DarkBackground,
    onSurfaceVariant = DarkSecondaryText,
    outline = DarkBorder,
    outlineVariant = DarkBorder,
    error = DarkError,
    onError = DarkBackground
)

object StudyOSTheme {
    val colors: StudyOSColors
        @Composable
        @ReadOnlyComposable
        get() = LocalStudyOSColors.current

    val typography: StudyOSTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalStudyOSTypography.current

    val shapes: StudyOSShapes
        @Composable
        @ReadOnlyComposable
        get() = LocalStudyOSShapes.current

    val spacing: StudyOSSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalStudyOSSpacing.current
}

@Composable
fun StudyOSTheme(
    appTheme: AppTheme = AppTheme.SYSTEM,
    content: @Composable () -> Unit
) {
    val isDark = when (appTheme) {
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
        AppTheme.SYSTEM -> isSystemInDarkTheme()
    }

    val colors = if (isDark) {
        StudyOSColors(
            background = DarkBackground,
            surface = DarkSurface,
            primaryText = DarkPrimaryText,
            secondaryText = DarkSecondaryText,
            mutedText = DarkMutedText,
            border = DarkBorder,
            accent = DarkAccent,
            error = DarkError,
            success = DarkSuccess,
            isDark = true
        )
    } else {
        StudyOSColors(
            background = LightBackground,
            surface = LightSurface,
            primaryText = LightPrimaryText,
            secondaryText = LightSecondaryText,
            mutedText = LightMutedText,
            border = LightBorder,
            accent = LightAccent,
            error = LightError,
            success = LightSuccess,
            isDark = false
        )
    }

    val materialColorScheme = if (isDark) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colors.background.toArgb()
                window.navigationBarColor = colors.background.toArgb()
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !isDark
                insetsController.isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    CompositionLocalProvider(
        LocalStudyOSColors provides colors,
        LocalStudyOSTypography provides StudyOSTypography(),
        LocalStudyOSShapes provides StudyOSShapes(),
        LocalStudyOSSpacing provides StudyOSSpacing()
    ) {
        MaterialTheme(
            colorScheme = materialColorScheme,
            content = content
        )
    }
}
