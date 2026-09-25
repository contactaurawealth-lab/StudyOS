package com.studyos.app.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Immutable
data class StudyOSShapes(
    // Global Radius Tokens
    val small: Shape = RoundedCornerShape(12.dp),
    val medium: Shape = RoundedCornerShape(16.dp),
    val large: Shape = RoundedCornerShape(22.dp),
    val modal: Shape = RoundedCornerShape(28.dp),
    val pill: Shape = RoundedCornerShape(100.dp),

    // Semantic Component Shapes
    val button: Shape = RoundedCornerShape(12.dp),
    val input: Shape = RoundedCornerShape(12.dp),
    val surface: Shape = RoundedCornerShape(16.dp),
    val card: Shape = RoundedCornerShape(16.dp),
    val dialog: Shape = RoundedCornerShape(28.dp),
    val bottomSheet: Shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    val statusPill: Shape = RoundedCornerShape(6.dp)
)

val LocalStudyOSShapes = staticCompositionLocalOf { StudyOSShapes() }
