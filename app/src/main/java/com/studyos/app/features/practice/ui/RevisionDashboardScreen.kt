package com.studyos.app.features.practice.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Timer
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studyos.app.core.ui.component.GlassCard
import com.studyos.app.core.ui.component.StudyOSButton
import com.studyos.app.core.ui.component.StudyOSDivider
import com.studyos.app.core.ui.component.StudyOSEmptyState
import com.studyos.app.core.ui.component.StudyOSIconButton
import com.studyos.app.core.ui.component.StudyOSLoadingState
import com.studyos.app.core.ui.component.StudyOSProgressBar
import com.studyos.app.domain.engine.PriorityQueueItem
import com.studyos.app.domain.model.ActiveRecallSessionType
import com.studyos.app.domain.model.ChapterMastery
import com.studyos.app.domain.model.ChapterMasteryLevel
import com.studyos.app.features.practice.viewmodel.LeitnerBoxState
import com.studyos.app.features.practice.viewmodel.RevisionDashboardViewModel
import com.studyos.app.features.practice.viewmodel.RevisionFilter
import com.studyos.app.features.practice.viewmodel.RevisionTab
import com.studyos.app.theme.StudyOSTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RevisionDashboardScreen(
    viewModel: RevisionDashboardViewModel,
    onStartRecallSession: (ActiveRecallSessionType, String?) -> Unit,
    onOpenChapter: (String) -> Unit,
    onOpenMistakes: () -> Unit = {},
    onBack: (() -> Unit)? = null,
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
            val totalDueCount = data.dueChaptersCount + data.dueFlashcardsCount + data.unresolvedWeakTopicsCount

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 680.dp)
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(4.dp))

                    // 1. Sleek 2-Tab Segmented Switch (Due Queue vs Practice Lab)
                    RevisionTabSelector(
                        selectedTab = uiState.selectedTab,
                        dueBadgeCount = totalDueCount,
                        onTabSelected = viewModel::selectTab
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 2. Tab Content
                    AnimatedVisibility(
                        visible = uiState.selectedTab == RevisionTab.DUE_QUEUE,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        DueQueueTabContent(
                            data = data,
                            leitnerBoxes = uiState.leitnerBoxes,
                            selectedBoxIndex = uiState.selectedBoxIndex,
                            selectedFilter = uiState.selectedFilter,
                            priorityQueue = uiState.priorityQueue,
                            onSelectBox = viewModel::selectLeitnerBox,
                            onSelectFilter = viewModel::selectFilter,
                            onStartRecallSession = onStartRecallSession,
                            onOpenChapter = onOpenChapter
                        )
                    }

                    AnimatedVisibility(
                        visible = uiState.selectedTab == RevisionTab.PRACTICE_LAB,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        PracticeLabTabContent(
                            data = data,
                            onStartRecallSession = onStartRecallSession,
                            onOpenMistakes = onOpenMistakes,
                            onOpenChapter = onOpenChapter
                        )
                    }
                }
            }
        }
    }
}

/**
 * Clean, lightweight 2-Tab Pill Switcher with whisper border
 */
@Composable
private fun RevisionTabSelector(
    selectedTab: RevisionTab,
    dueBadgeCount: Int,
    onTabSelected: (RevisionTab) -> Unit
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.statusPill)
            .background(colors.cardBackground.copy(alpha = 0.6f))
            .border(0.5.dp, colors.border.copy(alpha = 0.2f), shapes.statusPill)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val isDueSelected = selectedTab == RevisionTab.DUE_QUEUE
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(shapes.statusPill)
                .background(if (isDueSelected) colors.surface else Color.Transparent)
                .border(
                    width = if (isDueSelected) 0.5.dp else 0.dp,
                    color = if (isDueSelected) colors.border.copy(alpha = 0.3f) else Color.Transparent,
                    shape = shapes.statusPill
                )
                .clickable { onTabSelected(RevisionTab.DUE_QUEUE) }
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Due Queue",
                    style = typography.caption.copy(fontWeight = if (isDueSelected) FontWeight.SemiBold else FontWeight.Normal),
                    color = if (isDueSelected) colors.primaryText else colors.secondaryText
                )
                if (dueBadgeCount > 0) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isDueSelected) colors.accent else colors.mutedText.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "$dueBadgeCount",
                            style = typography.caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                            color = if (isDueSelected) colors.background else colors.primaryText
                        )
                    }
                }
            }
        }

        val isLabSelected = selectedTab == RevisionTab.PRACTICE_LAB
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(shapes.statusPill)
                .background(if (isLabSelected) colors.surface else Color.Transparent)
                .border(
                    width = if (isLabSelected) 0.5.dp else 0.dp,
                    color = if (isLabSelected) colors.border.copy(alpha = 0.3f) else Color.Transparent,
                    shape = shapes.statusPill
                )
                .clickable { onTabSelected(RevisionTab.PRACTICE_LAB) }
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Practice Lab",
                style = typography.caption.copy(fontWeight = if (isLabSelected) FontWeight.SemiBold else FontWeight.Normal),
                color = if (isLabSelected) colors.primaryText else colors.secondaryText
            )
        }
    }
}

/**
 * Tab 1 Content: The Zen Due Queue
 */
@Composable
private fun DueQueueTabContent(
    data: com.studyos.app.domain.model.DueRevisionDashboardData,
    leitnerBoxes: List<LeitnerBoxState>,
    selectedBoxIndex: Int?,
    selectedFilter: RevisionFilter,
    priorityQueue: List<PriorityQueueItem>,
    onSelectBox: (Int?) -> Unit,
    onSelectFilter: (RevisionFilter) -> Unit,
    onStartRecallSession: (ActiveRecallSessionType, String?) -> Unit,
    onOpenChapter: (String) -> Unit
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    val totalItemsDue = data.dueChaptersCount + data.dueFlashcardsCount + data.unresolvedWeakTopicsCount

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. One-Tap Zen Revise Hero Card
        item {
            OneTapReviseHeroCard(
                totalDue = totalItemsDue,
                recommendedSession = data.recommendedSession,
                streakDays = data.revisionStreakDays,
                onStartRevision = { onStartRecallSession(data.recommendedSession, null) }
            )
        }

        // 2. Compact Leitner 5-Dot Pipeline Strip
        if (leitnerBoxes.isNotEmpty()) {
            item {
                LeitnerPipelinePillStrip(
                    boxes = leitnerBoxes,
                    selectedBoxIndex = selectedBoxIndex,
                    onSelectBox = onSelectBox
                )
            }
        }

        // 3. Filter Chips Row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RevisionFilterChip(
                    label = "All ($totalItemsDue)",
                    isSelected = selectedFilter == RevisionFilter.ALL,
                    onClick = { onSelectFilter(RevisionFilter.ALL) }
                )
                RevisionFilterChip(
                    label = "Chapters (${data.dueChapters.size})",
                    isSelected = selectedFilter == RevisionFilter.CHAPTERS,
                    onClick = { onSelectFilter(RevisionFilter.CHAPTERS) }
                )
                RevisionFilterChip(
                    label = "Flashcards (${data.dueFlashcardsCount})",
                    isSelected = selectedFilter == RevisionFilter.FLASHCARDS,
                    onClick = { onSelectFilter(RevisionFilter.FLASHCARDS) }
                )
                RevisionFilterChip(
                    label = "Weak Topics (${data.unresolvedWeakTopicsCount})",
                    isSelected = selectedFilter == RevisionFilter.WEAK_TOPICS,
                    onClick = { onSelectFilter(RevisionFilter.WEAK_TOPICS) }
                )
            }
        }

        // 4. Filtered Stream of Due Items
        if (totalItemsDue == 0 && priorityQueue.isEmpty() && data.dueChapters.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shapes.card)
                        .background(colors.cardBackground.copy(alpha = 0.5f))
                        .border(0.5.dp, colors.border.copy(alpha = 0.2f), shapes.card)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Memory Retention on Track",
                            style = typography.body.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.primaryText
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "No spaced repetition reviews are due right now.",
                            style = typography.caption,
                            color = colors.secondaryText
                        )
                    }
                }
            }
        } else {
            // Render chapters if filter allows
            if (selectedFilter == RevisionFilter.ALL || selectedFilter == RevisionFilter.CHAPTERS) {
                items(data.dueChapters, key = { "ch_${it.chapterId}" }) { chMastery ->
                    MinimalDueChapterCard(
                        mastery = chMastery,
                        onRevise = { onStartRecallSession(ActiveRecallSessionType.DEEP_15, chMastery.chapterId) },
                        onOpen = { onOpenChapter(chMastery.chapterId) }
                    )
                }
            }

            // Render smart priority queue items if filter allows
            if (selectedFilter == RevisionFilter.ALL || selectedFilter == RevisionFilter.FLASHCARDS) {
                items(priorityQueue, key = { "q_${it.id}" }) { queueItem ->
                    MinimalPriorityQueueCard(
                        item = queueItem,
                        onRevise = { onStartRecallSession(ActiveRecallSessionType.DEEP_15, queueItem.chapterId) },
                        onOpen = { queueItem.chapterId?.let(onOpenChapter) }
                    )
                }
            }

            // Render weak topics if filter allows
            if (selectedFilter == RevisionFilter.ALL || selectedFilter == RevisionFilter.WEAK_TOPICS) {
                items(data.weakTopics, key = { "wt_${it.topic}" }) { weakTopic ->
                    MinimalWeakTopicCard(
                        weakTopic = weakTopic,
                        onRevise = { onStartRecallSession(ActiveRecallSessionType.FOCUSED_10, weakTopic.chapterId) }
                    )
                }
            }
        }
    }
}

/**
 * Tab 2 Content: Practice Lab (Feynman, Mistake Bank, Quick Sessions)
 */
@Composable
private fun PracticeLabTabContent(
    data: com.studyos.app.domain.model.DueRevisionDashboardData,
    onStartRecallSession: (ActiveRecallSessionType, String?) -> Unit,
    onOpenMistakes: () -> Unit,
    onOpenChapter: (String) -> Unit
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Feynman Blank-Page Mode Hero Card
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                padding = 18.dp,
                onClick = { onStartRecallSession(ActiveRecallSessionType.DEEP_15, null) }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.EditNote,
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Feynman Technique",
                                style = typography.caption.copy(fontWeight = FontWeight.Bold),
                                color = colors.accent
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Blank-Page Explanation Mode",
                            style = typography.body.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.primaryText
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Explain a concept simply in 3 minutes to expose hidden knowledge gaps.",
                            style = typography.caption,
                            color = colors.secondaryText
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(colors.accent.copy(alpha = 0.12f))
                            .padding(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // 2. Mistake Bank Shortcut Card
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                padding = 18.dp,
                onClick = onOpenMistakes
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.WarningAmber,
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Error Analysis",
                                style = typography.caption.copy(fontWeight = FontWeight.Bold),
                                color = colors.accent
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Mistake Bank & Weak Concepts",
                            style = typography.body.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.primaryText
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${data.unresolvedWeakTopicsCount} weak spots flagged from past quizzes and tests.",
                            style = typography.caption,
                            color = colors.secondaryText
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(colors.accent.copy(alpha = 0.12f))
                            .padding(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // 3. Quick Active Recall Sessions
        item {
            Text(
                text = "Rapid Recall Timers",
                style = typography.subsectionTitle,
                color = colors.primaryText
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                RapidRecallCard(
                    title = "5m Rapid",
                    subtitle = "5 quick questions",
                    modifier = Modifier.weight(1f),
                    onClick = { onStartRecallSession(ActiveRecallSessionType.QUICK_5, null) }
                )
                RapidRecallCard(
                    title = "10m Focus",
                    subtitle = "10 mixed cards",
                    modifier = Modifier.weight(1f),
                    onClick = { onStartRecallSession(ActiveRecallSessionType.FOCUSED_10, null) }
                )
                RapidRecallCard(
                    title = "15m Deep",
                    subtitle = "15 rigorous items",
                    modifier = Modifier.weight(1f),
                    onClick = { onStartRecallSession(ActiveRecallSessionType.DEEP_15, null) }
                )
            }
        }

        // 4. AI Recommendations (if any)
        if (data.recommendations.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Personalized Advice",
                    style = typography.subsectionTitle,
                    color = colors.primaryText
                )
            }

            items(data.recommendations, key = { it.id }) { rec ->
                MinimalRecommendationCard(
                    recommendation = rec,
                    onAction = { onStartRecallSession(ActiveRecallSessionType.DEEP_15, rec.chapterId) }
                )
            }
        }
    }
}

/**
 * Clean, high-contrast "One-Tap Revise" Hero Card
 */
@Composable
private fun OneTapReviseHeroCard(
    totalDue: Int,
    recommendedSession: ActiveRecallSessionType,
    streakDays: Int,
    onStartRevision: () -> Unit
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    val estMinutes = when (recommendedSession) {
        ActiveRecallSessionType.QUICK_5 -> 5
        ActiveRecallSessionType.FOCUSED_10 -> 10
        ActiveRecallSessionType.DEEP_15 -> 15
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        padding = 20.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Bolt,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "DAILY RECALL",
                        style = typography.caption.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = colors.accent
                    )
                }

                if (streakDays > 0) {
                    Text(
                        text = "🔥 $streakDays day streak",
                        style = typography.caption.copy(fontWeight = FontWeight.Medium),
                        color = colors.secondaryText
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (totalDue > 0) "$totalDue items due today" else "All caught up for today",
                style = typography.screenTitle.copy(fontSize = 22.sp),
                color = colors.primaryText
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (totalDue > 0) {
                    "Optimized cohort: ~$estMinutes min to maintain retention along your forgetting curve."
                } else {
                    "Your memory schedules are clear. Feel free to explore the Practice Lab."
                },
                style = typography.caption,
                color = colors.secondaryText
            )

            if (totalDue > 0) {
                Spacer(modifier = Modifier.height(16.dp))

                StudyOSButton(
                    text = "Start Daily Revision →",
                    onClick = onStartRevision,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Minimalist Leitner 5-box pipeline capsule strip
 */
@Composable
private fun LeitnerPipelinePillStrip(
    boxes: List<LeitnerBoxState>,
    selectedBoxIndex: Int?,
    onSelectBox: (Int?) -> Unit
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Memory Pipeline",
                style = typography.caption.copy(fontWeight = FontWeight.SemiBold),
                color = colors.secondaryText
            )
            Text(
                text = "Leitner 5-Box",
                style = typography.caption,
                color = colors.mutedText
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            boxes.forEach { box ->
                val isSelected = selectedBoxIndex == box.boxNumber
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(shapes.statusPill)
                        .background(
                            if (isSelected) colors.accent.copy(alpha = 0.2f)
                            else colors.cardBackground.copy(alpha = 0.7f)
                        )
                        .border(
                            width = 0.5.dp,
                            color = if (isSelected) colors.accent else colors.border.copy(alpha = 0.2f),
                            shape = shapes.statusPill
                        )
                        .clickable { onSelectBox(if (isSelected) null else box.boxNumber) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "B${box.boxNumber}",
                            style = typography.caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                            color = if (isSelected) colors.accent else colors.mutedText
                        )
                        Text(
                            text = "${box.cardCount}",
                            style = typography.caption.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                            color = colors.primaryText
                        )
                    }
                }
            }
        }
    }
}

/**
 * Filter Chip
 */
@Composable
private fun RevisionFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    Box(
        modifier = Modifier
            .clip(shapes.statusPill)
            .background(if (isSelected) colors.accent else colors.surface)
            .border(
                width = 0.5.dp,
                color = if (isSelected) colors.accent else colors.border.copy(alpha = 0.3f),
                shape = shapes.statusPill
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = typography.caption.copy(fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal),
            color = if (isSelected) colors.background else colors.primaryText
        )
    }
}

/**
 * Rapid Recall Session Card for Practice Lab
 */
@Composable
private fun RapidRecallCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    Box(
        modifier = modifier
            .clip(shapes.card)
            .background(colors.cardBackground.copy(alpha = 0.6f))
            .border(0.5.dp, colors.border.copy(alpha = 0.2f), shapes.card)
            .clickable(onClick = onClick)
            .padding(14.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                style = typography.body.copy(fontWeight = FontWeight.SemiBold),
                color = colors.primaryText
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = typography.caption.copy(fontSize = 11.sp),
                color = colors.secondaryText
            )
        }
    }
}

/**
 * Minimal Due Chapter Card with 0.5dp border
 */
@Composable
private fun MinimalDueChapterCard(
    mastery: ChapterMastery,
    onRevise: () -> Unit,
    onOpen: () -> Unit
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        padding = 14.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onOpen)
            ) {
                Text(
                    text = mastery.subjectName,
                    style = typography.caption.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                    color = colors.accent
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = mastery.chapterName,
                    style = typography.body.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.primaryText
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Mastery: ${mastery.masteryPercentage}% • ${mastery.flashcardsTotal - mastery.flashcardsMastered} cards to master",
                    style = typography.caption,
                    color = colors.secondaryText
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .clip(shapes.statusPill)
                    .background(colors.accent.copy(alpha = 0.12f))
                    .clickable(onClick = onRevise)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Revise",
                    style = typography.caption.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.accent
                )
            }
        }
    }
}

/**
 * Minimal Priority Queue Card
 */
@Composable
private fun MinimalPriorityQueueCard(
    item: PriorityQueueItem,
    onRevise: () -> Unit,
    onOpen: () -> Unit
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        padding = 14.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onOpen)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = item.subjectName,
                        style = typography.caption.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                        color = colors.accent
                    )
                    Text(
                        text = "•",
                        style = typography.caption,
                        color = colors.mutedText
                    )
                    Text(
                        text = item.reason,
                        style = typography.caption.copy(fontSize = 11.sp),
                        color = colors.secondaryText
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.title,
                    style = typography.body.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.primaryText
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .clip(shapes.statusPill)
                    .background(colors.accent.copy(alpha = 0.12f))
                    .clickable(onClick = onRevise)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Review",
                    style = typography.caption.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.accent
                )
            }
        }
    }
}

/**
 * Minimal Weak Topic Card
 */
@Composable
private fun MinimalWeakTopicCard(
    weakTopic: com.studyos.app.domain.model.WeakTopic,
    onRevise: () -> Unit
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        padding = 14.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "WEAK CONCEPT",
                    style = typography.caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                    color = colors.accent
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = weakTopic.topic,
                    style = typography.body.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.primaryText
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Accuracy: ${weakTopic.accuracyPercentage}% (${weakTopic.questionsAttempted} attempts)",
                    style = typography.caption,
                    color = colors.secondaryText
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .clip(shapes.statusPill)
                    .background(colors.accent.copy(alpha = 0.12f))
                    .clickable(onClick = onRevise)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Fix Gap",
                    style = typography.caption.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.accent
                )
            }
        }
    }
}

/**
 * Minimal Recommendation Card
 */
@Composable
private fun MinimalRecommendationCard(
    recommendation: com.studyos.app.domain.model.RevisionRecommendation,
    onAction: () -> Unit
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        padding = 14.dp,
        onClick = onAction
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = recommendation.chapterName,
                    style = typography.body.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.primaryText
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = recommendation.reason,
                    style = typography.caption,
                    color = colors.secondaryText
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
