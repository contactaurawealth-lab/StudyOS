package com.studyos.app.features.progress.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studyos.app.core.ui.component.GlassCard
import com.studyos.app.core.ui.component.GlassTopBar
import com.studyos.app.core.ui.component.ShimmerPlaceholder
import com.studyos.app.core.ui.component.StudyOSButton
import com.studyos.app.core.ui.component.StudyOSEmptyState
import com.studyos.app.core.ui.component.StudyOSOutlinedButton
import com.studyos.app.core.ui.component.StudyOSProgressBar
import com.studyos.app.core.ui.component.bounceClick
import com.studyos.app.domain.model.ExamRelevance
import com.studyos.app.domain.model.LeverageQuadrant
import com.studyos.app.domain.model.LeverageTopicItem
import com.studyos.app.domain.model.LossReasonCategory
import com.studyos.app.domain.model.MarkLeakageReport
import com.studyos.app.domain.usecase.SubjectProgressBreakdown
import com.studyos.app.features.progress.viewmodel.CognitiveReadiness
import com.studyos.app.features.progress.viewmodel.ProgressTab
import com.studyos.app.features.progress.viewmodel.ProgressTimeWindow
import com.studyos.app.features.progress.viewmodel.ProgressViewModel
import com.studyos.app.theme.StudyOSTheme
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ProgressScreen(
    viewModel: ProgressViewModel,
    onSubjectClick: (String) -> Unit = {},
    onStartMicroSprint: (String) -> Unit = {},
    onOpenPaper: (String) -> Unit = {},
    onGeneratePreExamPrimer: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes
    val spacing = StudyOSTheme.spacing
    val haptic = LocalHapticFeedback.current

    var selectedQuadrantFilter by remember { mutableStateOf<LeverageQuadrant?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            GlassTopBar(
                title = "Progress & Intelligence",
                subtitle = when (uiState.selectedTab) {
                    ProgressTab.OVERVIEW -> if (uiState.academicProgress.totalChapters > 0) {
                        "${uiState.academicProgress.completedChapters} of ${uiState.academicProgress.totalChapters} completed"
                    } else null
                    ProgressTab.LEAKAGE_RADAR -> "Step 11 Diagnostic • Bleed Rate & Recovery"
                    ProgressTab.LEVERAGE_MATRIX -> "Step 13 Prioritizer • Deterministic ROI"
                }
            )

            // Segmented Top Navigation Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.screenHorizontal, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProgressTab.values().forEach { tab ->
                    val isSelected = uiState.selectedTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(shapes.statusPill)
                            .background(if (isSelected) colors.primaryText else colors.surface)
                            .border(
                                0.5.dp,
                                if (isSelected) colors.primaryText else colors.border.copy(alpha = 0.3f),
                                shapes.statusPill
                            )
                            .bounceClick {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.setProgressTab(tab)
                            }
                            .padding(vertical = 8.dp)
                            .semantics { this.role = Role.Tab },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = tab.label,
                                style = typography.caption.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 11.5.sp
                                ),
                                color = if (isSelected) colors.buttonText else colors.secondaryText
                            )

                            // Badges for recoverable marks or high leverage items
                            if (tab == ProgressTab.LEAKAGE_RADAR && uiState.markLeakageReport.recoverableScorePotential > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(colors.accent)
                                )
                            } else if (tab == ProgressTab.LEVERAGE_MATRIX && uiState.leverageMatrix.highImpactCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(colors.error)
                                )
                            }
                        }
                    }
                }
            }

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
                        ShimmerPlaceholder(modifier = Modifier.fillMaxWidth().height(140.dp), shape = shapes.medium)
                        repeat(3) {
                            ShimmerPlaceholder(modifier = Modifier.fillMaxWidth().height(80.dp), shape = shapes.medium)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 680.dp)
                            .padding(horizontal = spacing.screenHorizontal),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)
                    ) {
                        when (uiState.selectedTab) {
                            // TAB 1: READINESS OVERVIEW
                            ProgressTab.OVERVIEW -> {
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
                                                    .background(if (isSelected) colors.primaryText.copy(alpha = 0.9f) else colors.surface)
                                                    .border(0.5.dp, if (isSelected) colors.primaryText else colors.border.copy(alpha = 0.3f), shapes.statusPill)
                                                .clickable { viewModel.setTimeWindowFilter(window) }
                                                .padding(vertical = 6.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = window.label,
                                                    style = typography.caption.copy(
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                        fontSize = 11.5.sp
                                                    ),
                                                    color = if (isSelected) colors.buttonText else colors.secondaryText
                                                )
                                            }
                                        }
                                    }
                                }

                                if (uiState.academicProgress.totalChapters == 0) {
                                    item {
                                        Spacer(modifier = Modifier.height(32.dp))
                                        StudyOSEmptyState(
                                            title = "No progress recorded yet",
                                            description = "Add chapters and topics to track your cognitive exam readiness."
                                        )
                                    }
                                } else {
                                    val readiness = uiState.cognitiveReadiness
                                    item {
                                        GlassCard(backgroundColor = colors.glassSurface, padding = 18.dp) {
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
                                                CognitiveReadinessRadarChart(
                                                    readiness = readiness,
                                                    modifier = Modifier.fillMaxWidth().height(180.dp)
                                                )

                                                Spacer(modifier = Modifier.height(14.dp))
                                                Column(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        ReadinessMetricTile(label = "Syllabus Coverage", value = "${readiness.syllabusCompletionPct}%", weight = "35% weight", modifier = Modifier.weight(1f))
                                                        ReadinessMetricTile(label = "Active Recall", value = "${readiness.recallRetentionPct}%", weight = "25% weight", modifier = Modifier.weight(1f))
                                                    }
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        ReadinessMetricTile(label = "Quiz Accuracy", value = "${readiness.quizAccuracyPct}%", weight = "20% weight", modifier = Modifier.weight(1f))
                                                        ReadinessMetricTile(label = "Consistency", value = "${readiness.consistencyPct}%", weight = "20% weight", modifier = Modifier.weight(1f))
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Weak Spot Diagnoser
                                    if (uiState.diagnosedWeakSpots.isNotEmpty()) {
                                        item {
                                            Text(
                                                text = "Diagnosed Weak Spots",
                                                style = typography.sectionTitle.copy(fontWeight = FontWeight.SemiBold),
                                                color = colors.primaryText
                                            )
                                        }
                                        items(uiState.diagnosedWeakSpots, key = { it.subjectId }) { weakSpot ->
                                            GlassCard(backgroundColor = colors.glassSurface, padding = 14.dp) {
                                                Column(modifier = Modifier.fillMaxWidth()) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(text = weakSpot.subjectName, style = typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = colors.primaryText)
                                                        Text(text = "${weakSpot.accuracyPercentage}% Accuracy", style = typography.caption.copy(fontWeight = FontWeight.Bold), color = colors.error)
                                                    }
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(text = weakSpot.recommendation, style = typography.caption, color = colors.secondaryText)
                                                    Spacer(modifier = Modifier.height(10.dp))
                                                    StudyOSButton(
                                                        text = "Start 10-Min Micro Sprint",
                                                        onClick = { onStartMicroSprint(weakSpot.subjectId) },
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Subjects Breakdown
                                    item {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(text = "Subjects Coverage", style = typography.sectionTitle.copy(fontWeight = FontWeight.SemiBold), color = colors.primaryText)
                                    }
                                    items(uiState.academicProgress.subjectBreakdowns, key = { it.subjectId }) { breakdown ->
                                        SubjectProgressRow(breakdown = breakdown, onClick = { onSubjectClick(breakdown.subjectId) })
                                    }
                                }
                            }

                            // TAB 2: MARK-LEAKAGE RADAR & BLEED RATE ANALYTICS
                            ProgressTab.LEAKAGE_RADAR -> {
                                val report = uiState.markLeakageReport

                                item {
                                    GlassCard(backgroundColor = colors.glassSurface, padding = 18.dp) {
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "MARK-LEAKAGE RADAR",
                                                    style = typography.caption.copy(fontWeight = FontWeight.Bold),
                                                    color = colors.error,
                                                    letterSpacing = 1.sp
                                                )
                                                val bleedRateStr = String.format(Locale.US, "%.1f", report.bleedRatePer100Marks)
                                                Box(
                                                    modifier = Modifier
                                                        .clip(shapes.statusPill)
                                                        .background(colors.error.copy(alpha = 0.15f))
                                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "Bleed Rate: $bleedRateStr / 100",
                                                        style = typography.caption.copy(fontWeight = FontWeight.Bold),
                                                        color = colors.error
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(12.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column {
                                                    val totalLostStr = String.format(Locale.US, "%.1f", report.totalMarksLost)
                                                    Text(text = totalLostStr, style = typography.screenTitle, color = colors.primaryText)
                                                    Text(text = "Total Marks Leaked", style = typography.caption, color = colors.secondaryText)
                                                }

                                                Column(horizontalAlignment = Alignment.End) {
                                                    val recoverableStr = String.format(Locale.US, "%.1f", report.recoverableScorePotential)
                                                    Text(text = "+$recoverableStr", style = typography.screenTitle, color = colors.accent)
                                                    Text(text = "Recoverable Score Slip", style = typography.caption, color = colors.accent)
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(14.dp))

                                            // Canvas Mark Leakage Radar Chart
                                            MarkLeakageRadarChart(
                                                report = report,
                                                modifier = Modifier.fillMaxWidth().height(180.dp)
                                            )

                                            Spacer(modifier = Modifier.height(12.dp))

                                            // Actionable Diagnostic Box
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(shapes.medium)
                                                    .background(colors.surface.copy(alpha = 0.5f))
                                                    .border(0.5.dp, colors.border.copy(alpha = 0.3f), shapes.medium)
                                                    .padding(12.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.Top,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Text("💡", fontSize = 16.sp)
                                                    Text(
                                                        text = report.actionableAdvice,
                                                        style = typography.caption,
                                                        color = colors.primaryText
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // 1-Tap Ghost Exam Hero Launcher Card
                                item {
                                    GlassCard(
                                        backgroundColor = colors.cardBackground,
                                        padding = 16.dp,
                                        modifier = Modifier.border(1.dp, colors.accent.copy(alpha = 0.35f), shapes.card)
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text("👻", fontSize = 22.sp)
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "Auto-Remediation Ghost Exam",
                                                        style = typography.subsectionTitle.copy(fontWeight = FontWeight.Bold),
                                                        color = colors.primaryText
                                                    )
                                                    Text(
                                                        text = "Generate a custom verification exam strictly from questions where you leaked marks. Passing this exam marks them remediated and closes the Connected Mistake Loop.",
                                                        style = typography.caption,
                                                        color = colors.secondaryText
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(4.dp))

                                            StudyOSButton(
                                                text = if (uiState.isGeneratingGhostExam) "Generating Ghost Exam..." else "⚡ Launch Ghost Remediation Exam",
                                                enabled = !uiState.isGeneratingGhostExam,
                                                onClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    viewModel.generateGhostExam { paperId ->
                                                        onOpenPaper(paperId)
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }

                                // Category Breakdown Section
                                item {
                                    Text(
                                        text = "Leakage Categories (${report.categoryBreakdown.size})",
                                        style = typography.sectionTitle.copy(fontWeight = FontWeight.SemiBold),
                                        color = colors.primaryText
                                    )
                                }

                                items(report.categoryBreakdown, key = { it.category.name }) { summary ->
                                    if (summary.totalLostMarks > 0) {
                                        GlassCard(backgroundColor = colors.glassSurface, padding = 12.dp) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Text(
                                                            text = summary.category.displayName,
                                                            style = typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                            color = colors.primaryText
                                                        )
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(shapes.statusPill)
                                                                .background(if (summary.isExecutionError) colors.accent.copy(alpha = 0.15f) else colors.surface)
                                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                                        ) {
                                                            Text(
                                                                text = if (summary.isExecutionError) "PREVENTABLE SLIP" else "CONCEPT GAP",
                                                                style = typography.caption.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                                                color = if (summary.isExecutionError) colors.accent else colors.secondaryText
                                                            )
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    val pctStr = String.format(Locale.US, "%.1f", summary.percentageOfTotal)
                                                    Text(text = "$pctStr% of total leaked marks", style = typography.caption, color = colors.secondaryText)
                                                }

                                                val marksStr = String.format(Locale.US, "%.1f", summary.totalLostMarks)
                                                Text(
                                                    text = "-$marksStr",
                                                    style = typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = colors.error
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // TAB 3: 95% TARGET LEVERAGE MATRIX
                            ProgressTab.LEVERAGE_MATRIX -> {
                                val matrix = uiState.leverageMatrix

                                item {
                                    GlassCard(backgroundColor = colors.glassSurface, padding = 18.dp) {
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "95% LEVERAGE MATRIX",
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
                                                        text = "Deterministic ROI Engine",
                                                        style = typography.caption.copy(fontWeight = FontWeight.Bold),
                                                        color = colors.accent
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(10.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column {
                                                    Text(text = "${matrix.highImpactCount} Topics", style = typography.screenTitle, color = colors.primaryText)
                                                    Text(text = "High Impact Boosters", style = typography.caption, color = colors.secondaryText)
                                                }

                                                Column(horizontalAlignment = Alignment.End) {
                                                    val gainStr = String.format(Locale.US, "%.1f", matrix.estimatedTotalMarksGainTo95)
                                                    Text(text = "+$gainStr", style = typography.screenTitle, color = colors.accent)
                                                    Text(text = "Recoverable Marks Potential", style = typography.caption, color = colors.accent)
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(12.dp))

                                            Text(
                                                text = matrix.strategicSummary,
                                                style = typography.caption,
                                                color = colors.secondaryText
                                            )

                                            Spacer(modifier = Modifier.height(12.dp))

                                            // 2x2 Quadrant Summary Grid
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                LeverageQuadrantTile(
                                                    title = "High Impact",
                                                    count = matrix.highImpactCount,
                                                    badgeColor = colors.error,
                                                    isSelected = selectedQuadrantFilter == LeverageQuadrant.HIGH_IMPACT,
                                                    onClick = {
                                                        selectedQuadrantFilter = if (selectedQuadrantFilter == LeverageQuadrant.HIGH_IMPACT) null else LeverageQuadrant.HIGH_IMPACT
                                                    },
                                                    modifier = Modifier.weight(1f)
                                                )
                                                LeverageQuadrantTile(
                                                    title = "Secure",
                                                    count = matrix.secureCount,
                                                    badgeColor = colors.success,
                                                    isSelected = selectedQuadrantFilter == LeverageQuadrant.SECURE,
                                                    onClick = {
                                                        selectedQuadrantFilter = if (selectedQuadrantFilter == LeverageQuadrant.SECURE) null else LeverageQuadrant.SECURE
                                                    },
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                LeverageQuadrantTile(
                                                    title = "Quick Wins",
                                                    count = matrix.quickWinsCount,
                                                    badgeColor = colors.accent,
                                                    isSelected = selectedQuadrantFilter == LeverageQuadrant.QUICK_WINS,
                                                    onClick = {
                                                        selectedQuadrantFilter = if (selectedQuadrantFilter == LeverageQuadrant.QUICK_WINS) null else LeverageQuadrant.QUICK_WINS
                                                    },
                                                    modifier = Modifier.weight(1f)
                                                )
                                                LeverageQuadrantTile(
                                                    title = "Overstudied",
                                                    count = matrix.overstudiedCount,
                                                    badgeColor = colors.mutedText,
                                                    isSelected = selectedQuadrantFilter == LeverageQuadrant.OVERSTUDIED,
                                                    onClick = {
                                                        selectedQuadrantFilter = if (selectedQuadrantFilter == LeverageQuadrant.OVERSTUDIED) null else LeverageQuadrant.OVERSTUDIED
                                                    },
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Printable Pre-Exam Primer PDF Action Card
                                item {
                                    GlassCard(
                                        backgroundColor = colors.cardBackground,
                                        padding = 16.dp,
                                        modifier = Modifier.border(1.dp, colors.accent.copy(alpha = 0.35f), shapes.card)
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text("📄", fontSize = 22.sp)
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "Printable Pre-Exam Rapid Primer",
                                                        style = typography.subsectionTitle.copy(fontWeight = FontWeight.Bold),
                                                        color = colors.primaryText
                                                    )
                                                    Text(
                                                        text = "Compile an ultra-clean 1-page vector PDF with core formulas, personal careless trap warnings, and exam pacing protocols for revision right before the test.",
                                                        style = typography.caption,
                                                        color = colors.secondaryText
                                                    )
                                                }
                                            }

                                            val firstSubjectId = uiState.academicProgress.subjectBreakdowns.firstOrNull()?.subjectId
                                            if (firstSubjectId != null) {
                                                StudyOSOutlinedButton(
                                                    text = "Print Pre-Exam Primer PDF",
                                                    onClick = {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        onGeneratePreExamPrimer(firstSubjectId)
                                                    },
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                        }
                                    }
                                }

                                // Filtered Topics List
                                val filteredItems = if (selectedQuadrantFilter != null) {
                                    matrix.items.filter { it.quadrant == selectedQuadrantFilter }
                                } else {
                                    matrix.items
                                }

                                item {
                                    Text(
                                        text = if (selectedQuadrantFilter != null) {
                                            "${selectedQuadrantFilter?.label} Topics (${filteredItems.size})"
                                        } else {
                                            "Prioritized Topics (${filteredItems.size})"
                                        },
                                        style = typography.sectionTitle.copy(fontWeight = FontWeight.SemiBold),
                                        color = colors.primaryText
                                    )
                                }

                                if (filteredItems.isEmpty()) {
                                    item {
                                        StudyOSEmptyState(
                                            title = "No topics in this quadrant",
                                            description = "Add or review syllabus topics to balance your 95% Leverage Matrix."
                                        )
                                    }
                                } else {
                                    items(filteredItems, key = { it.topicId }) { topicItem ->
                                        LeverageTopicCard(
                                            item = topicItem,
                                            onStudyClick = { onStartMicroSprint(topicItem.subjectId) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LeverageQuadrantTile(
    title: String,
    count: Int,
    badgeColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    Box(
        modifier = modifier
            .clip(shapes.medium)
            .background(if (isSelected) badgeColor.copy(alpha = 0.2f) else colors.surface)
            .border(
                1.dp,
                if (isSelected) badgeColor else colors.border.copy(alpha = 0.3f),
                shapes.medium
            )
            .bounceClick { onClick() }
            .padding(10.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = typography.caption.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                    color = colors.primaryText
                )
                Box(
                    modifier = Modifier
                        .clip(shapes.statusPill)
                        .background(badgeColor.copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "$count",
                        style = typography.caption.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                        color = badgeColor
                    )
                }
            }
        }
    }
}

@Composable
private fun LeverageTopicCard(
    item: LeverageTopicItem,
    onStudyClick: () -> Unit
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    GlassCard(
        backgroundColor = colors.glassSurface,
        padding = 14.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.topicName,
                    style = typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = colors.primaryText
                )

                val badgeColor = when (item.quadrant) {
                    LeverageQuadrant.HIGH_IMPACT -> colors.error
                    LeverageQuadrant.SECURE -> colors.success
                    LeverageQuadrant.QUICK_WINS -> colors.accent
                    LeverageQuadrant.OVERSTUDIED -> colors.mutedText
                }

                Box(
                    modifier = Modifier
                        .clip(shapes.statusPill)
                        .background(badgeColor.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = item.quadrant.label,
                        style = typography.caption.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                        color = badgeColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${item.subjectName} • ${item.chapterName}",
                style = typography.caption,
                color = colors.secondaryText
            )

            Spacer(modifier = Modifier.height(8.dp))
            StudyOSProgressBar(
                progress = item.masteryPercentage,
                height = 5.dp,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val yieldStr = String.format(Locale.US, "%.1f", item.marksYieldPotential)
                val hoursStr = String.format(Locale.US, "%.1f", item.estimatedHoursToMaster)
                Text(
                    text = "+$yieldStr Marks yield • Est. ${hoursStr}h",
                    style = typography.caption.copy(fontWeight = FontWeight.Bold),
                    color = colors.accent
                )

                StudyOSButton(
                    text = "Study Now",
                    onClick = onStudyClick,
                    modifier = Modifier.height(30.dp)
                )
            }
        }
    }
}

@Composable
private fun MarkLeakageRadarChart(
    report: MarkLeakageReport,
    modifier: Modifier = Modifier
) {
    val colors = StudyOSTheme.colors

    val defaultCategories = listOf(
        LossReasonCategory.CARELESS_MISTAKE,
        LossReasonCategory.CALCULATION_ERROR,
        LossReasonCategory.MISREAD,
        LossReasonCategory.TIME_SHORTAGE,
        LossReasonCategory.CONCEPT_ERROR,
        LossReasonCategory.FORGOT
    )

    val maxLoss = report.categoryBreakdown.maxOfOrNull { it.totalLostMarks }?.coerceAtLeast(5.0) ?: 10.0

    Canvas(modifier = modifier) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val radius = (minOf(size.width, size.height) / 2f) - 24.dp.toPx()
        val numAxes = 6
        val angleStep = (2 * Math.PI / numAxes).toFloat()

        // 1. Concentric Guide Circles
        val levels = listOf(0.33f, 0.66f, 1.0f)
        for (level in levels) {
            val r = radius * level
            drawCircle(
                color = colors.border.copy(alpha = if (level == 1.0f) 0.3f else 0.15f),
                radius = r,
                center = Offset(centerX, centerY),
                style = Stroke(width = 1.dp.toPx())
            )
        }

        // 2. Spoke lines
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

        // 3. Data Polygon
        val dataPath = Path()
        val points = mutableListOf<Offset>()
        for (i in 0 until numAxes) {
            val cat = defaultCategories[i]
            val catLost = report.categoryBreakdown.find { it.category == cat }?.totalLostMarks ?: 0.0
            val valuePct = (catLost / maxLoss).toFloat().coerceIn(0.1f, 1.0f)
            val pointRadius = radius * valuePct
            val angle = i * angleStep - (Math.PI / 2f).toFloat()
            val x = centerX + pointRadius * cos(angle)
            val y = centerY + pointRadius * sin(angle)
            val pt = Offset(x, y)
            points.add(pt)
            if (i == 0) dataPath.moveTo(x, y) else dataPath.lineTo(x, y)
        }
        dataPath.close()

        drawPath(
            path = dataPath,
            color = colors.error.copy(alpha = 0.2f)
        )
        drawPath(
            path = dataPath,
            color = colors.error,
            style = Stroke(width = 2.dp.toPx())
        )

        for (pt in points) {
            drawCircle(color = colors.background, radius = 4.dp.toPx(), center = pt)
            drawCircle(color = colors.error, radius = 3.dp.toPx(), center = pt)
        }
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

        drawPath(path = dataPath, color = colors.accent.copy(alpha = 0.22f))
        drawPath(path = dataPath, color = colors.accent, style = Stroke(width = 2.dp.toPx()))

        for (pt in points) {
            drawCircle(color = colors.background, radius = 4.dp.toPx(), center = pt)
            drawCircle(color = colors.accent, radius = 3.dp.toPx(), center = pt)
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

        Spacer(modifier = Modifier.height(8.dp))

        StudyOSProgressBar(
            progress = breakdown.progress,
            height = 5.dp,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "${breakdown.completedChapters} of ${breakdown.totalChapters} chapters completed",
            style = typography.caption,
            color = colors.secondaryText
        )
    }
}
