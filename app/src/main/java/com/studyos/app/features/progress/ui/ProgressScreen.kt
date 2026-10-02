package com.studyos.app.features.progress.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.Color
import com.studyos.app.core.ui.component.GlassCard
import com.studyos.app.core.ui.component.GlassTopBar
import com.studyos.app.core.ui.component.ShimmerPlaceholder
import com.studyos.app.core.ui.component.StudyOSButton
import com.studyos.app.core.ui.component.StudyOSEmptyState
import com.studyos.app.core.ui.component.StudyOSProgressBar
import com.studyos.app.domain.usecase.SubjectProgressBreakdown
import com.studyos.app.features.progress.viewmodel.CognitiveReadiness
import com.studyos.app.features.progress.viewmodel.ProgressTimeWindow
import com.studyos.app.features.progress.viewmodel.ProgressViewModel
import com.studyos.app.theme.StudyOSTheme
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ProgressScreen(
    viewModel: ProgressViewModel,
    onSubjectClick: (String) -> Unit = {},
    onStartMicroSprint: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes
    val spacing = StudyOSTheme.spacing

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            GlassTopBar(
                title = "Progress",
                subtitle = if (uiState.academicProgress.totalChapters > 0) {
                    "${uiState.academicProgress.completedChapters} of ${uiState.academicProgress.totalChapters} completed"
                } else null
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                if (uiState.isLoading) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 680.dp)
                            .padding(horizontal = spacing.screenHorizontal, vertical = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        ShimmerPlaceholder(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp),
                            shape = shapes.medium
                        )
                        repeat(3) {
                            ShimmerPlaceholder(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(80.dp),
                                shape = shapes.medium
                            )
                        }
                    }
                } else {
                    val progress = uiState.academicProgress
                    val animatedProgress by animateIntAsState(
                        targetValue = progress.overallProgress,
                        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
                        label = "overallProgressAnimation"
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 680.dp)
                            .padding(horizontal = spacing.screenHorizontal),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(top = 14.dp, bottom = 32.dp)
                    ) {
                        // 1. Time Window Filter Chips
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ProgressTimeWindow.values().forEach { window ->
                                    val isSelected = uiState.selectedTimeWindow == window
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(shapes.statusPill)
                                            .background(if (isSelected) colors.primaryText else colors.surface)
                                            .border(0.5.dp, if (isSelected) colors.primaryText else colors.border.copy(alpha = 0.3f), shapes.statusPill)
                                            .clickable { viewModel.setTimeWindowFilter(window) }
                                            .padding(vertical = 6.dp)
                                            .semantics { this.role = Role.Tab },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = window.label,
                                            style = typography.caption.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 12.sp
                                            ),
                                            color = if (isSelected) colors.buttonText else colors.secondaryText
                                        )
                                    }
                                }
                            }
                        }

                        if (progress.totalChapters == 0) {
                            item {
                                Spacer(modifier = Modifier.height(32.dp))
                                StudyOSEmptyState(
                                    title = "No progress yet",
                                    description = "Add chapters and update their progress to see your study coverage."
                                )
                            }
                        } else {
                            // 2. Cognitive Readiness Index Card
                            val readiness = uiState.cognitiveReadiness
                            item {
                                GlassCard(
                                    backgroundColor = colors.glassSurface,
                                    padding = 18.dp
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "COGNITIVE READINESS",
                                                style = typography.caption.copy(fontWeight = FontWeight.Bold),
                                                color = colors.accent,
                                                letterSpacing = 1.sp
                                            )

                                            Box(
                                                modifier = Modifier
                                                    .clip(shapes.statusPill)
                                                    .background(colors.accent.copy(alpha = 0.15f))
                                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = readiness.readinessTier,
                                                    style = typography.caption.copy(fontWeight = FontWeight.Bold),
                                                    color = colors.accent
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.Bottom
                                        ) {
                                            Text(
                                                text = "${readiness.overallIndex}%",
                                                style = typography.screenTitle,
                                                color = colors.primaryText
                                            )

                                            val hours = uiState.filteredStudyMinutes / 60
                                            val mins = uiState.filteredStudyMinutes % 60
                                            val timeStr = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
                                            Text(
                                                text = "$timeStr • ${uiState.activeDaysCount} active days",
                                                style = typography.caption,
                                                color = colors.secondaryText
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        StudyOSProgressBar(
                                            progress = readiness.overallIndex,
                                            height = 6.dp,
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Spacer(modifier = Modifier.height(14.dp))

                                        // 5-Axis Multi-Dimensional Readiness Radar Chart
                                        CognitiveReadinessRadarChart(
                                            readiness = readiness,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(180.dp)
                                        )

                                        Spacer(modifier = Modifier.height(14.dp))

                                        // 4 Breakdown Quadrants in responsive 2x2 Grid
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                ReadinessMetricTile(
                                                    label = "Syllabus",
                                                    value = "${readiness.syllabusCompletionPct}%",
                                                    weight = "35%",
                                                    modifier = Modifier.weight(1f)
                                                )
                                                ReadinessMetricTile(
                                                    label = "Retention",
                                                    value = "${readiness.recallRetentionPct}%",
                                                    weight = "25%",
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                ReadinessMetricTile(
                                                    label = "Accuracy",
                                                    value = "${readiness.quizAccuracyPct}%",
                                                    weight = "20%",
                                                    modifier = Modifier.weight(1f)
                                                )
                                                ReadinessMetricTile(
                                                    label = "Habit",
                                                    value = "${readiness.consistencyPct}%",
                                                    weight = "20%",
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Weak Spot Diagnoser & 10-Minute Micro-Sprint Card
                            if (uiState.diagnosedWeakSpots.isNotEmpty()) {
                                item {
                                    GlassCard(
                                        backgroundColor = colors.glassSurface,
                                        padding = 16.dp
                                    ) {
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "⚡ WEAK SPOT DIAGNOSER",
                                                    style = typography.caption.copy(fontWeight = FontWeight.Bold),
                                                    color = Color(0xFFD4A373),
                                                    letterSpacing = 1.sp
                                                )
                                                Text(
                                                    text = "${uiState.diagnosedWeakSpots.size} areas to reinforce",
                                                    style = typography.caption,
                                                    color = colors.secondaryText
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(10.dp))

                                            uiState.diagnosedWeakSpots.forEach { spot ->
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(shapes.medium)
                                                        .background(colors.surface.copy(alpha = 0.5f))
                                                        .border(0.5.dp, colors.border.copy(alpha = 0.3f), shapes.medium)
                                                        .padding(12.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = spot.subjectName,
                                                            style = typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                            color = colors.primaryText
                                                        )
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(shapes.statusPill)
                                                                .background(Color(0xFFE53E3E).copy(alpha = 0.15f))
                                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                                        ) {
                                                            Text(
                                                                text = "${spot.accuracyPercentage}% retention",
                                                                style = typography.caption.copy(fontWeight = FontWeight.Bold),
                                                                color = Color(0xFFFC8181)
                                                            )
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = spot.recommendation,
                                                        style = typography.caption,
                                                        color = colors.secondaryText
                                                    )
                                                    Spacer(modifier = Modifier.height(10.dp))
                                                    StudyOSButton(
                                                        text = "⚡ Start 10-Min Micro-Sprint",
                                                        onClick = { onStartMicroSprint(spot.subjectId) },
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(8.dp))
                                            }
                                        }
                                    }
                                }
                            }

                            // 3. Overall Progress Card
                            item {
                                GlassCard(
                                    backgroundColor = colors.glassSurface,
                                    padding = 18.dp
                                ) {
                                    Text(
                                        text = "Overall progress",
                                        style = typography.caption.copy(fontWeight = FontWeight.SemiBold),
                                        color = colors.secondaryText
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Bottom
                                    ) {
                                        Text(
                                            text = "$animatedProgress%",
                                            style = typography.screenTitle,
                                            color = colors.primaryText
                                        )

                                        Text(
                                            text = "${progress.completedChapters} of ${progress.totalChapters} chapters",
                                            style = typography.caption,
                                            color = colors.secondaryText
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    StudyOSProgressBar(
                                        progress = progress.overallProgress,
                                        height = 6.dp,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            // 4. Consistency & Retention Momentum Card
                            item {
                                GlassCard(
                                    backgroundColor = colors.glassSurface,
                                    padding = 16.dp
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            text = "Consistency & Momentum",
                                            style = typography.caption.copy(fontWeight = FontWeight.SemiBold),
                                            color = colors.accent
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (progress.completedChapters > 0) "Active Learning Pace" else "Start Your Habit",
                                            style = typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = colors.primaryText
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Non-punitive consistency: daily retrieval and incremental progress compound into long-term recall without penalty for missed days.",
                                            style = typography.caption,
                                            color = colors.secondaryText
                                        )
                                    }
                                }
                            }

                            // 5. Weekly Study Report Card
                            val report = uiState.weeklyReport
                            if (report != null && report.totalMinutes > 0) {
                                item {
                                    val context = androidx.compose.ui.platform.LocalContext.current
                                    GlassCard(
                                        backgroundColor = colors.glassSurface,
                                        padding = 16.dp
                                    ) {
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "WEEKLY STUDY REPORT",
                                                    style = typography.caption.copy(fontWeight = FontWeight.Bold),
                                                    color = colors.accent,
                                                    letterSpacing = 1.sp
                                                )

                                                Text(
                                                    text = "Share",
                                                    style = typography.caption.copy(fontWeight = FontWeight.SemiBold),
                                                    color = colors.accent,
                                                    modifier = Modifier.clickable {
                                                        val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                            type = "text/plain"
                                                            putExtra(android.content.Intent.EXTRA_TEXT, report.shareableText)
                                                        }
                                                        context.startActivity(android.content.Intent.createChooser(sendIntent, "Share Weekly Report"))
                                                    }
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(6.dp))

                                            Text(
                                                text = report.headline,
                                                style = typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = colors.primaryText
                                            )

                                            Spacer(modifier = Modifier.height(8.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    val hours = report.totalMinutes / 60
                                                    val mins = report.totalMinutes % 60
                                                    Text(
                                                        text = if (hours > 0) "${hours}h ${mins}m" else "${mins}m",
                                                        style = typography.sectionTitle,
                                                        color = colors.primaryText
                                                    )
                                                    Text(
                                                        text = "Focused Study",
                                                        style = typography.caption,
                                                        color = colors.secondaryText
                                                    )
                                                }

                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "${report.activeDaysCount}/7 Days",
                                                        style = typography.sectionTitle,
                                                        color = colors.primaryText
                                                    )
                                                    Text(
                                                        text = "Consistency",
                                                        style = typography.caption,
                                                        color = colors.secondaryText
                                                    )
                                                }

                                                if (report.mistakesResolved > 0) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = "${report.mistakesResolved}",
                                                            style = typography.sectionTitle,
                                                            color = colors.accent
                                                        )
                                                        Text(
                                                            text = "Mistakes Fixed",
                                                            style = typography.caption,
                                                            color = colors.secondaryText
                                                        )
                                                    }
                                                }
                                            }

                                            if (report.subjectBreakdown.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(10.dp))
                                                Text(
                                                    text = "Top Subjects: " + report.subjectBreakdown.take(3).joinToString(", ") { "${it.first} (${it.second / 60}h)" },
                                                    style = typography.caption,
                                                    color = colors.mutedText
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // 6. Subjects Breakdown Section Header
                            item {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Subjects",
                                    style = typography.sectionTitle.copy(fontWeight = FontWeight.SemiBold),
                                    color = colors.primaryText
                                )
                            }

                            // 7. Subjects Breakdown Rows
                            items(progress.subjectBreakdowns, key = { it.subjectId }) { breakdown ->
                                SubjectProgressRow(
                                    breakdown = breakdown,
                                    onClick = { onSubjectClick(breakdown.subjectId) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubjectProgressRow(
    breakdown: SubjectProgressBreakdown,
    onClick: () -> Unit
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography

    GlassCard(
        onClick = onClick,
        backgroundColor = colors.glassSurface,
        padding = 14.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = breakdown.subjectName,
                style = typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = colors.primaryText
            )

            Text(
                text = "${breakdown.progress}%",
                style = typography.caption.copy(fontWeight = FontWeight.SemiBold),
                color = if (breakdown.progress == 100) colors.success else colors.accent
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "${breakdown.completedChapters} of ${breakdown.totalChapters} chapters completed",
            style = typography.caption,
            color = colors.secondaryText
        )

        Spacer(modifier = Modifier.height(8.dp))

        StudyOSProgressBar(
            progress = breakdown.progress,
            height = 4.dp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ReadinessMetricTile(
    label: String,
    value: String,
    weight: String,
    modifier: Modifier = Modifier
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    Box(
        modifier = modifier
            .clip(shapes.small)
            .background(colors.surface.copy(alpha = 0.6f))
            .padding(vertical = 8.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                style = typography.caption.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                color = colors.primaryText
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = typography.caption.copy(fontSize = 10.sp),
                color = colors.secondaryText
            )
            Text(
                text = weight,
                style = typography.caption.copy(fontSize = 9.sp),
                color = colors.mutedText
            )
        }
    }
}

@Composable
private fun CognitiveReadinessRadarChart(
    readiness: CognitiveReadiness,
    modifier: Modifier = Modifier
) {
    val colors = StudyOSTheme.colors

    val labels = listOf("Syllabus", "Retention", "Accuracy", "Habit", "Overall")
    val rawValues = listOf(
        readiness.syllabusCompletionPct,
        readiness.recallRetentionPct,
        readiness.quizAccuracyPct,
        readiness.consistencyPct,
        readiness.overallIndex
    )

    Canvas(modifier = modifier) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val radius = (minOf(size.width, size.height) / 2f) - 24.dp.toPx()
        val numAxes = 5
        val angleStep = (2 * Math.PI / numAxes).toFloat()

        // 1. Draw Concentric Web Guide Pentagons (25%, 50%, 75%, 100%)
        val webLevels = listOf(0.25f, 0.50f, 0.75f, 1.0f)
        for (level in webLevels) {
            val levelRadius = radius * level
            val webPath = Path()
            for (i in 0 until numAxes) {
                val angle = i * angleStep - (Math.PI / 2f).toFloat()
                val x = centerX + levelRadius * cos(angle)
                val y = centerY + levelRadius * sin(angle)
                if (i == 0) webPath.moveTo(x, y) else webPath.lineTo(x, y)
            }
            webPath.close()
            drawPath(
                path = webPath,
                color = colors.border.copy(alpha = if (level == 1.0f) 0.35f else 0.18f),
                style = Stroke(width = if (level == 1.0f) 1.5.dp.toPx() else 1.dp.toPx())
            )
        }

        // 2. Draw Spokes from center
        for (i in 0 until numAxes) {
            val angle = i * angleStep - (Math.PI / 2f).toFloat()
            val endX = centerX + radius * cos(angle)
            val endY = centerY + radius * sin(angle)
            drawLine(
                color = colors.border.copy(alpha = 0.25f),
                start = Offset(centerX, centerY),
                end = Offset(endX, endY),
                strokeWidth = 1.dp.toPx()
            )
        }

        // 3. Draw Data Polygon
        val dataPath = Path()
        val points = mutableListOf<Offset>()
        for (i in 0 until numAxes) {
            val valuePct = (rawValues[i].coerceIn(5, 100).toFloat() / 100f)
            val pointRadius = radius * valuePct
            val angle = i * angleStep - (Math.PI / 2f).toFloat()
            val x = centerX + pointRadius * cos(angle)
            val y = centerY + pointRadius * sin(angle)
            val pt = Offset(x, y)
            points.add(pt)
            if (i == 0) dataPath.moveTo(x, y) else dataPath.lineTo(x, y)
        }
        dataPath.close()

        // Fill radar shape with serene translucent accent latte
        drawPath(
            path = dataPath,
            color = colors.accent.copy(alpha = 0.22f)
        )

        // Outline radar shape
        drawPath(
            path = dataPath,
            color = colors.accent,
            style = Stroke(width = 2.dp.toPx())
        )

        // Draw points on vertices
        for (pt in points) {
            drawCircle(
                color = colors.background,
                radius = 4.dp.toPx(),
                center = pt
            )
            drawCircle(
                color = colors.accent,
                radius = 3.dp.toPx(),
                center = pt
            )
        }

        // 4. Axis Labels
        val textPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(190, 200, 200, 200)
            textSize = 10.dp.toPx()
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }

        for (i in 0 until numAxes) {
            val angle = i * angleStep - (Math.PI / 2f).toFloat()
            val labelRadius = radius + 14.dp.toPx()
            val labelX = centerX + labelRadius * cos(angle)
            val labelY = centerY + labelRadius * sin(angle) + 3.dp.toPx()
            drawContext.canvas.nativeCanvas.drawText(
                labels[i],
                labelX,
                labelY,
                textPaint
            )
        }
    }
}

