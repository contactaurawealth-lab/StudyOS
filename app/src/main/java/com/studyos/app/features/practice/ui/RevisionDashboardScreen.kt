package com.studyos.app.features.practice.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ElectricBolt
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studyos.app.core.ui.component.StudyOSButton
import com.studyos.app.core.ui.component.StudyOSDivider
import com.studyos.app.core.ui.component.StudyOSEmptyState
import com.studyos.app.core.ui.component.StudyOSIconButton
import com.studyos.app.core.ui.component.StudyOSLoadingState
import com.studyos.app.core.ui.component.StudyOSOutlinedButton
import com.studyos.app.core.ui.component.StudyOSProgressBar
import com.studyos.app.domain.model.ActiveRecallSessionType
import com.studyos.app.domain.model.ChapterMastery
import com.studyos.app.domain.model.ChapterMasteryLevel
import com.studyos.app.domain.model.RevisionRecommendation
import com.studyos.app.features.practice.viewmodel.RevisionDashboardViewModel
import com.studyos.app.theme.StudyOSTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RevisionDashboardScreen(
    viewModel: RevisionDashboardViewModel,
    onBack: (() -> Unit)? = null,
    onStartRecallSession: (ActiveRecallSessionType, chapterId: String?) -> Unit,
    onOpenChapter: (chapterId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Revision & Recall",
                        style = typography.screenTitle,
                        color = colors.primaryText
                    )
                },
                navigationIcon = {
                    if (onBack != null) {
                        StudyOSIconButton(
                            onClick = onBack,
                            contentDescription = "Back"
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = null,
                                tint = colors.secondaryText,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.background,
                    titleContentColor = colors.primaryText
                )
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            StudyOSLoadingState(message = "Evaluating recall schedules & mastery...")
        } else {
            val data = uiState.dashboardData

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Streak & Daily Target Progress Card
                item {
                    DailyRevisionStreakCard(
                        streakDays = data.revisionStreakDays,
                        dailyTargetMinutes = data.dailyRevisionTargetMinutes,
                        completedMinutesToday = data.completedRevisionMinutesToday,
                        isTargetCompleted = data.isDailyTargetCompleted,
                        onSetTargetMinutes = viewModel::setDailyTargetMinutes
                    )
                }

                // 2. Active Recall Quick Launchers
                item {
                    Text(
                        text = "Active Recall Sessions",
                        style = typography.subsectionTitle,
                        color = colors.primaryText
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        RecallSessionCard(
                            sessionType = ActiveRecallSessionType.QUICK_5,
                            isRecommended = data.recommendedSession == ActiveRecallSessionType.QUICK_5,
                            onClick = { onStartRecallSession(ActiveRecallSessionType.QUICK_5, null) }
                        )
                        RecallSessionCard(
                            sessionType = ActiveRecallSessionType.FOCUSED_10,
                            isRecommended = data.recommendedSession == ActiveRecallSessionType.FOCUSED_10,
                            onClick = { onStartRecallSession(ActiveRecallSessionType.FOCUSED_10, null) }
                        )
                        RecallSessionCard(
                            sessionType = ActiveRecallSessionType.DEEP_15,
                            isRecommended = data.recommendedSession == ActiveRecallSessionType.DEEP_15,
                            onClick = { onStartRecallSession(ActiveRecallSessionType.DEEP_15, null) }
                        )
                    }
                }

                // 3. Due For Revision Dashboard Hierarchy View
                item {
                    DueHierarchyCard(
                        dueChaptersCount = data.dueChaptersCount,
                        dueFlashcardsCount = data.dueFlashcardsCount,
                        weakTopicsCount = data.unresolvedWeakTopicsCount,
                        recommendedSession = data.recommendedSession,
                        onLaunchRecommended = { onStartRecallSession(data.recommendedSession, null) }
                    )
                }

                // 4. Personalized Revision Recommendations
                if (data.recommendations.isNotEmpty()) {
                    item {
                        Text(
                            text = "AI Revision Recommendations",
                            style = typography.subsectionTitle,
                            color = colors.primaryText
                        )
                    }

                    items(data.recommendations, key = { it.id }) { rec ->
                        RecommendationItemCard(
                            recommendation = rec,
                            onAction = {
                                onStartRecallSession(ActiveRecallSessionType.DEEP_15, rec.chapterId)
                            }
                        )
                    }
                }

                // 5. Chapters Due for Revision
                item {
                    Text(
                        text = "Chapters Due for Revision (${data.dueChapters.size})",
                        style = typography.subsectionTitle,
                        color = colors.primaryText
                    )
                }

                if (data.dueChapters.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(shapes.card)
                                .background(colors.cardBackground)
                                .border(1.dp, colors.border, shapes.card)
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircle,
                                    contentDescription = null,
                                    tint = colors.success,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "All chapters are up to date!",
                                    style = typography.body.copy(fontWeight = FontWeight.Medium),
                                    color = colors.primaryText
                                )
                                Text(
                                    text = "No spaced repetition reviews due right now.",
                                    style = typography.caption,
                                    color = colors.secondaryText
                                )
                            }
                        }
                    }
                } else {
                    items(data.dueChapters, key = { it.chapterId }) { chMastery ->
                        ChapterMasteryCard(
                            mastery = chMastery,
                            onRevise = { onStartRecallSession(ActiveRecallSessionType.DEEP_15, chMastery.chapterId) },
                            onOpenChapter = { onOpenChapter(chMastery.chapterId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyRevisionStreakCard(
    streakDays: Int,
    dailyTargetMinutes: Int,
    completedMinutesToday: Int,
    isTargetCompleted: Boolean,
    onSetTargetMinutes: (Int) -> Unit
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    val progress = if (dailyTargetMinutes > 0) {
        (completedMinutesToday.toFloat() / dailyTargetMinutes.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.card)
            .background(colors.cardBackground)
            .border(1.dp, colors.border, shapes.card)
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.ElectricBolt,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (streakDays > 0) "$streakDays Day Revision Streak" else "Start Your Revision Streak",
                        style = typography.body.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.primaryText
                    )
                }

                if (isTargetCompleted) {
                    Box(
                        modifier = Modifier
                            .clip(shapes.statusPill)
                            .background(colors.success.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Target Met",
                            style = typography.caption.copy(fontWeight = FontWeight.Medium),
                            color = colors.success
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Daily Target: $completedMinutesToday / $dailyTargetMinutes min",
                    style = typography.caption,
                    color = colors.secondaryText
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = typography.caption.copy(fontWeight = FontWeight.Medium),
                    color = colors.primaryText
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            StudyOSProgressBar(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Target selector row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Adjust Goal:",
                    style = typography.caption,
                    color = colors.mutedText
                )
                listOf(5, 10, 15, 20).forEach { mins ->
                    val isSelected = dailyTargetMinutes == mins
                    Box(
                        modifier = Modifier
                            .clip(shapes.statusPill)
                            .background(if (isSelected) colors.primaryText else colors.surface)
                            .border(1.dp, if (isSelected) colors.primaryText else colors.border, shapes.statusPill)
                            .clickable { onSetTargetMinutes(mins) }
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                            .semantics { this.role = Role.Button }
                    ) {
                        Text(
                            text = "${mins}m",
                            style = typography.caption.copy(
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            ),
                            color = if (isSelected) colors.buttonText else colors.primaryText
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecallSessionCard(
    sessionType: ActiveRecallSessionType,
    isRecommended: Boolean,
    onClick: () -> Unit
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    Box(
        modifier = Modifier
            .width(200.dp)
            .clip(shapes.card)
            .background(colors.cardBackground)
            .border(
                1.dp,
                if (isRecommended) colors.accent else colors.border,
                shapes.card
            )
            .clickable(onClick = onClick)
            .padding(14.dp)
            .semantics { this.role = Role.Button }
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${sessionType.targetMinutes} MIN",
                    style = typography.caption.copy(fontWeight = FontWeight.Bold),
                    color = colors.accent
                )
                if (isRecommended) {
                    Box(
                        modifier = Modifier
                            .clip(shapes.statusPill)
                            .background(colors.accent.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Best Fit",
                            style = typography.caption.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                            color = colors.accent
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = sessionType.title,
                style = typography.body.copy(fontWeight = FontWeight.SemiBold),
                color = colors.primaryText,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = sessionType.description,
                style = typography.caption,
                color = colors.secondaryText,
                maxLines = 2,
                minLines = 2
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.PlayArrow,
                    contentDescription = null,
                    tint = colors.primaryText,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Start Session",
                    style = typography.caption.copy(fontWeight = FontWeight.Medium),
                    color = colors.primaryText
                )
            }
        }
    }
}

@Composable
private fun DueHierarchyCard(
    dueChaptersCount: Int,
    dueFlashcardsCount: Int,
    weakTopicsCount: Int,
    recommendedSession: ActiveRecallSessionType,
    onLaunchRecommended: () -> Unit
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.card)
            .background(colors.surface)
            .border(1.dp, colors.border, shapes.card)
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = "Today's Recall Agenda",
                style = typography.body.copy(fontWeight = FontWeight.SemiBold),
                color = colors.primaryText
            )
            Spacer(modifier = Modifier.height(8.dp))

            val treeLines = listOf(
                "├── $dueChaptersCount ${if (dueChaptersCount == 1) "chapter" else "chapters"} to revise",
                "├── $dueFlashcardsCount ${if (dueFlashcardsCount == 1) "flashcard" else "flashcards"} due",
                "├── $weakTopicsCount ${if (weakTopicsCount == 1) "weak topic" else "weak topics"} to reinforce",
                "└── Recommended: ${recommendedSession.title}"
            )

            treeLines.forEach { line ->
                Text(
                    text = line,
                    style = typography.caption.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                    color = colors.secondaryText,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            StudyOSButton(
                text = "Launch ${recommendedSession.title}",
                onClick = onLaunchRecommended,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun RecommendationItemCard(
    recommendation: RevisionRecommendation,
    onAction: () -> Unit
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.card)
            .background(colors.cardBackground)
            .border(1.dp, colors.border, shapes.card)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${recommendation.chapterName} • ${recommendation.subjectName}",
                    style = typography.body.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.primaryText
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = recommendation.reason,
                    style = typography.caption,
                    color = if (recommendation.priority == 1) colors.error else colors.secondaryText
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            StudyOSOutlinedButton(
                text = recommendation.recommendedAction,
                onClick = onAction
            )
        }
    }
}

@Composable
private fun ChapterMasteryCard(
    mastery: ChapterMastery,
    onRevise: () -> Unit,
    onOpenChapter: () -> Unit
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.card)
            .background(colors.cardBackground)
            .border(1.dp, colors.border, shapes.card)
            .clickable(onClick = onOpenChapter)
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = mastery.chapterName,
                        style = typography.body.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.primaryText
                    )
                    Text(
                        text = mastery.subjectName,
                        style = typography.caption,
                        color = colors.secondaryText
                    )
                }

                // Mastery badge
                Box(
                    modifier = Modifier
                        .clip(shapes.statusPill)
                        .background(colors.surface)
                        .border(1.dp, colors.border, shapes.statusPill)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${mastery.masteryPercentage}% • ${mastery.level.label}",
                        style = typography.caption.copy(fontWeight = FontWeight.Medium),
                        color = when (mastery.level) {
                            ChapterMasteryLevel.MASTERED -> colors.success
                            ChapterMasteryLevel.PROFICIENT -> colors.accent
                            ChapterMasteryLevel.LEARNING -> colors.primaryText
                            ChapterMasteryLevel.NOVICE -> colors.mutedText
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Mastery bar
            StudyOSProgressBar(
                progress = mastery.masteryPercentage / 100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cards: ${mastery.flashcardsMastered}/${mastery.flashcardsTotal}",
                        style = typography.caption,
                        color = colors.secondaryText
                    )
                    if (mastery.mistakesCount > 0) {
                        Text(
                            text = "Mistakes: ${mastery.resolvedMistakesCount}/${mastery.mistakesCount}",
                            style = typography.caption,
                            color = colors.secondaryText
                        )
                    }
                }

                StudyOSButton(
                    text = "Recall",
                    onClick = onRevise
                )
            }
        }
    }
}
