package com.studyos.app.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// StudyOS Light Palette (Warm paper, restrained neutrals, subtle amber accents)
val LightBackground = Color(0xFFF7F7F4)
val LightSurface = Color(0xFFFFFFFF)
val LightCardBackground = Color(0xFFEFEFE9)
val LightPrimaryText = Color(0xFF191918)
val LightSecondaryText = Color(0xFF686862)
val LightMutedText = Color(0xFF999991)
val LightBorder = Color(0xFFDEDED7)
val LightAccent = Color(0xFFB7791F)

// Light Glass Tokens
val LightGlassSurface = Color(0xF2FFFFFF) // 95% translucent white
val LightGlassSurfaceSubtle = Color(0x99FFFFFF) // 60% translucent
val LightGlassBorder = Color(0x14000000) // 8% subtle black border
val LightGlassHighlight = Color(0x33FFFFFF)

// StudyOS Dark Palette (Near-black, warm graphite, restrained amber)
val DarkBackground = Color(0xFF141413)
val DarkSurface = Color(0xFF1E1E1C)
val DarkCardBackground = Color(0xFF242422)
val DarkPrimaryText = Color(0xFFF1F1EC)
val DarkSecondaryText = Color(0xFFB5B5AD)
val DarkMutedText = Color(0xFF85857E)
val DarkBorder = Color(0xFF2E2E2A)
val DarkAccent = Color(0xFFD39A3A)

// Dark Glass Tokens
val DarkGlassSurface = Color(0xD9222220) // 85% translucent graphite
val DarkGlassSurfaceSubtle = Color(0x80222220) // 50% translucent
val DarkGlassBorder = Color(0x24FFFFFF) // 14% subtle white border
val DarkGlassHighlight = Color(0x14FFFFFF)

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
    val isDark: Boolean,
    val warning: Color = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706),
    val glassSurface: Color = if (isDark) DarkGlassSurface else LightGlassSurface,
    val glassSurfaceSubtle: Color = if (isDark) DarkGlassSurfaceSubtle else LightGlassSurfaceSubtle,
    val glassBorder: Color = if (isDark) DarkGlassBorder else LightGlassBorder,
    val glassHighlight: Color = if (isDark) DarkGlassHighlight else LightGlassHighlight
) {
    val critical: Color get() = error
    val cardBackground: Color get() = if (isDark) DarkCardBackground else LightCardBackground
    val buttonText: Color get() = if (isDark) Color(0xFF141413) else Color(0xFFFFFFFF)
}

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
