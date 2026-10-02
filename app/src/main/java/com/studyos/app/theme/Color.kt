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

// Coffee Latte Palette ("Coffee Time" Aesthetic: Warm Obsidian, Matte Slate, Frosted Smoke, Amber Latte)
val CoffeeLatteBackground = Color(0xFF0F1115)
val CoffeeLatteSurface = Color(0xFF161920)
val CoffeeLatteCardBackground = Color(0xFF1E222D)
val CoffeeLattePrimaryText = Color(0xFFF6EFEB)
val CoffeeLatteSecondaryText = Color(0xFFB8ADA3)
val CoffeeLatteMutedText = Color(0xFF7D7268)
val CoffeeLatteBorder = Color(0xFF28231E)
val CoffeeLatteAccent = Color(0xFFD4A373)

// Forest Sage Palette (Japanese Ma Calm: Deep Moss Obsidian, Pine Needle Slate, Fresh Fern)
val ForestSageBackground = Color(0xFF0C1410)
val ForestSageSurface = Color(0xFF131F19)
val ForestSageCardBackground = Color(0xFF1A2A22)
val ForestSagePrimaryText = Color(0xFFE8F5E9)
val ForestSageSecondaryText = Color(0xFFA3B899)
val ForestSageMutedText = Color(0xFF60725B)
val ForestSageBorder = Color(0xFF22362C)
val ForestSageAccent = Color(0xFF81C784)

// Ocean Cobalt Palette (Nordic Midnight: Abyssal Navy, Midnight Cobalt, Icy Cyan)
val OceanCobaltBackground = Color(0xFF0A0F1D)
val OceanCobaltSurface = Color(0xFF111827)
val OceanCobaltCardBackground = Color(0xFF1E293B)
val OceanCobaltPrimaryText = Color(0xFFF0F6FC)
val OceanCobaltSecondaryText = Color(0xFF94A3B8)
val OceanCobaltMutedText = Color(0xFF64748B)
val OceanCobaltBorder = Color(0xFF1E2D42)
val OceanCobaltAccent = Color(0xFF38BDF8)

// Sunset Amber Palette (Golden Hour: Charred Cocoa, Warm Clay, Radiant Sunset Amber)
val SunsetAmberBackground = Color(0xFF140F0D)
val SunsetAmberSurface = Color(0xFF1E1714)
val SunsetAmberCardBackground = Color(0xFF281E19)
val SunsetAmberPrimaryText = Color(0xFFFFF7ED)
val SunsetAmberSecondaryText = Color(0xFFD4A373)
val SunsetAmberMutedText = Color(0xFF785D48)
val SunsetAmberBorder = Color(0xFF382A22)
val SunsetAmberAccent = Color(0xFFFB923C)

// Cherry Blossom Palette (Sakura Nocturne: Plum Shadow, Velvet Violet, Sakura Rose)
val CherryBlossomBackground = Color(0xFF140D14)
val CherryBlossomSurface = Color(0xFF1F1420)
val CherryBlossomCardBackground = Color(0xFF2A1C2B)
val CherryBlossomPrimaryText = Color(0xFFFDF2F8)
val CherryBlossomSecondaryText = Color(0xFFF472B6)
val CherryBlossomMutedText = Color(0xFF835A74)
val CherryBlossomBorder = Color(0xFF3B2439)
val CherryBlossomAccent = Color(0xFFEC4899)

// Status Colors (restrained, non-vibrant)
val LightError = Color(0xFFB3261E)
val DarkError = Color(0xFFF2B8B5)
val LightSuccess = Color(0xFF386A20)
val DarkSuccess = Color(0xFFA6D388)

fun parseHexColor(hex: String?, fallback: Color = Color(0xFFD4A373)): Color {
    if (hex.isNullOrBlank()) return fallback
    return try {
        val clean = if (hex.startsWith("#")) hex else "#$hex"
        Color(android.graphics.Color.parseColor(clean))
    } catch (_: Exception) {
        fallback
    }
}

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
