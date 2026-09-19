package com.studyos.app.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.studyos.app.theme.StudyOSTheme

@Composable
fun StudyOSEmptyState(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    actionButtonText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 40.dp)
            .semantics {
                this.contentDescription = "$title. ${description ?: ""}"
            },
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = typography.sectionTitle,
            color = colors.primaryText
        )

        if (!description.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = description,
                style = typography.body,
                color = colors.secondaryText
            )
        }

        if (actionButtonText != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(20.dp))
            StudyOSOutlinedButton(
                text = actionButtonText,
                onClick = onActionClick
            )
        }
    }
}

@Composable
fun StudyOSLoadingState(
    modifier: Modifier = Modifier,
    message: String = "Loading..."
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .semantics { this.contentDescription = message },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                strokeWidth = 2.5.dp,
                color = colors.accent
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = message,
                style = typography.secondary,
                color = colors.secondaryText
            )
        }
    }
}

@Composable
fun StudyOSErrorState(
    modifier: Modifier = Modifier,
    message: String = "Something went wrong.",
    retryText: String = "Try again",
    onRetry: (() -> Unit)? = null
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp)
            .semantics { this.contentDescription = message },
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            style = typography.sectionTitle,
            color = colors.primaryText
        )
        if (onRetry != null) {
            Spacer(modifier = Modifier.height(16.dp))
            StudyOSOutlinedButton(
                text = retryText,
                onClick = onRetry
            )
        }
    }
}

@Composable
fun StudyOSProgressBar(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val colors = StudyOSTheme.colors
    val shapes = StudyOSTheme.shapes

    LinearProgressIndicator(
        progress = { progress.coerceIn(0f, 1f) },
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(shapes.statusPill),
        color = colors.accent,
        trackColor = colors.border
    )
}
