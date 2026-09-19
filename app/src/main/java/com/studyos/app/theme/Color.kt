package com.studyos.app.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// StudyOS Light Palette
val LightBackground = Color(0xFFF7F7F4)
val LightSurface = Color(0xFFFFFFFF)
val LightPrimaryText = Color(0xFF191918)
val LightSecondaryText = Color(0xFF686862)
val LightMutedText = Color(0xFF999991)
val LightBorder = Color(0xFFDEDED7)
val LightAccent = Color(0xFFB7791F)

// StudyOS Dark Palette
val DarkBackground = Color(0xFF181817)
val DarkSurface = Color(0xFF222220)
val DarkPrimaryText = Color(0xFFF1F1EC)
val DarkSecondaryText = Color(0xFFB5B5AD)
val DarkMutedText = Color(0xFF85857E)
val DarkBorder = Color(0xFF393934)
val DarkAccent = Color(0xFFD39A3A)

// Status Colors (restrained, non-vibrant)
val LightError = Color(0xFFB3261E)
val DarkError = Color(0xFFF2B8B5)
val LightSuccess = Color(0xFF386A20)
val DarkSuccess = Color(0xFFA6D388)

@Immutable
data class StudyOSColors(
    val background: Color,
    val surface: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val mutedText: Color,
    val border: Color,
    val accent: Color,
    val error: Color,
    val success: Color,
    val isDark: Boolean
)

val LocalStudyOSColors = staticCompositionLocalOf {
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
