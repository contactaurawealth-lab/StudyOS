package com.studyos.app.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Immutable
data class StudyOSShapes(
    val button: Shape = RoundedCornerShape(7.dp),
    val input: Shape = RoundedCornerShape(8.dp),
    val surface: Shape = RoundedCornerShape(8.dp),
    val dialog: Shape = RoundedCornerShape(12.dp),
    val bottomSheet: Shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
    val statusPill: Shape = RoundedCornerShape(4.dp)
)

val LocalStudyOSShapes = staticCompositionLocalOf { StudyOSShapes() }
