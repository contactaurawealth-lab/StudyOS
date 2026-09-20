package com.studyos.app.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class StudyOSSpacing(
    val extraSmall: Dp = 4.dp,
    val small: Dp = 8.dp,
    val medium: Dp = 12.dp,
    val standard: Dp = 16.dp,
    val large: Dp = 20.dp,
    val extraLarge: Dp = 24.dp,
    val huge: Dp = 32.dp,
    val screenHorizontal: Dp = 20.dp,
    val screenVertical: Dp = 20.dp,
    val cardPadding: Dp = 16.dp
)

val LocalStudyOSSpacing = staticCompositionLocalOf { StudyOSSpacing() }
