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
import androidx.compose.ui.graphics.Color
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
    customAccentHex: String? = null,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (appTheme) {
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
        AppTheme.SYSTEM -> isSystemDark
        AppTheme.COFFEE_LATTE,
        AppTheme.FOREST_SAGE,
        AppTheme.OCEAN_COBALT,
        AppTheme.SUNSET_AMBER,
        AppTheme.CHERRY_BLOSSOM,
        AppTheme.CUSTOM_ACCENT -> true
    }

    val customAccent = parseHexColor(customAccentHex, DarkAccent)

    val colors = when (appTheme) {
        AppTheme.LIGHT -> StudyOSColors(
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
        AppTheme.DARK -> StudyOSColors(
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
        AppTheme.SYSTEM -> if (isSystemDark) {
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
        AppTheme.COFFEE_LATTE -> StudyOSColors(
            background = CoffeeLatteBackground,
            surface = CoffeeLatteSurface,
            primaryText = CoffeeLattePrimaryText,
            secondaryText = CoffeeLatteSecondaryText,
            mutedText = CoffeeLatteMutedText,
            border = CoffeeLatteBorder,
            accent = CoffeeLatteAccent,
            error = DarkError,
            success = CoffeeLatteAccent,
            isDark = true
        )
        AppTheme.FOREST_SAGE -> StudyOSColors(
            background = ForestSageBackground,
            surface = ForestSageSurface,
            primaryText = ForestSagePrimaryText,
            secondaryText = ForestSageSecondaryText,
            mutedText = ForestSageMutedText,
            border = ForestSageBorder,
            accent = ForestSageAccent,
            error = DarkError,
            success = ForestSageAccent,
            isDark = true
        )
        AppTheme.OCEAN_COBALT -> StudyOSColors(
            background = OceanCobaltBackground,
            surface = OceanCobaltSurface,
            primaryText = OceanCobaltPrimaryText,
            secondaryText = OceanCobaltSecondaryText,
            mutedText = OceanCobaltMutedText,
            border = OceanCobaltBorder,
            accent = OceanCobaltAccent,
            error = DarkError,
            success = Color(0xFF34D399),
            isDark = true
        )
        AppTheme.SUNSET_AMBER -> StudyOSColors(
            background = SunsetAmberBackground,
            surface = SunsetAmberSurface,
            primaryText = SunsetAmberPrimaryText,
            secondaryText = SunsetAmberSecondaryText,
            mutedText = SunsetAmberMutedText,
            border = SunsetAmberBorder,
            accent = SunsetAmberAccent,
            error = DarkError,
            success = Color(0xFF4ADE80),
            isDark = true
        )
        AppTheme.CHERRY_BLOSSOM -> StudyOSColors(
            background = CherryBlossomBackground,
            surface = CherryBlossomSurface,
            primaryText = CherryBlossomPrimaryText,
            secondaryText = CherryBlossomSecondaryText,
            mutedText = CherryBlossomMutedText,
            border = CherryBlossomBorder,
            accent = CherryBlossomAccent,
            error = DarkError,
            success = Color(0xFF4ADE80),
            isDark = true
        )
        AppTheme.CUSTOM_ACCENT -> StudyOSColors(
            background = DarkBackground,
            surface = DarkSurface,
            primaryText = DarkPrimaryText,
            secondaryText = DarkSecondaryText,
            mutedText = DarkMutedText,
            border = DarkBorder,
            accent = customAccent,
            error = DarkError,
            success = DarkSuccess,
            isDark = true
        )
    }

    val materialColorScheme = if (isDark) {
        DarkColorScheme.copy(
            primary = colors.accent,
            background = colors.background,
            surface = colors.surface
        )
    } else {
        LightColorScheme.copy(
            primary = colors.accent,
            background = colors.background,
            surface = colors.surface
        )
    }

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
