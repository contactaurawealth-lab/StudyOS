package com.studyos.app.features.practice.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studyos.app.core.ui.component.GlassTopBar
import com.studyos.app.core.ui.component.StudyOSEmptyState
import com.studyos.app.core.ui.component.StudyOSIconButton
import com.studyos.app.core.ui.component.StudyOSLoadingState
import com.studyos.app.features.practice.viewmodel.KnowledgeGraphViewModel
import com.studyos.app.theme.StudyOSTheme
import kotlin.math.sqrt

@Composable
fun KnowledgeGraphScreen(
    viewModel: KnowledgeGraphViewModel,
    onBack: () -> Unit,
    onOpenNote: (noteId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            GlassTopBar(
                title = "Knowledge Graph",
                subtitle = if (uiState.nodes.isNotEmpty()) {
                    "${uiState.nodes.size} notes • ${uiState.totalConnections} connections"
                } else null,
                navigationIcon = {
                    StudyOSIconButton(
                        onClick = onBack,
                        contentDescription = "Back"
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = null,
                            tint = colors.primaryText
                        )
                    }
                },
                actions = {
                    StudyOSIconButton(
                        onClick = { viewModel.loadGraph() },
                        contentDescription = "Recalculate Graph Layout"
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = null,
                            tint = colors.primaryText
                        )
                    }
                }
            )

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    StudyOSLoadingState()
                }
            } else if (uiState.nodes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    StudyOSEmptyState(
                        title = "No linked notes found",
                        description = "Create notes with [[WikiLinks]] like [[Photosynthesis]] to link your knowledge into an interconnected graph."
                    )
                }
            } else {
                val nodes = uiState.nodes
                val edges = uiState.edges
                val nodeMap = remember(nodes) { nodes.associateBy { it.id } }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(0.4f, 3.5f)
                                offset += pan
                            }
                        }
                        .pointerInput(nodes, scale, offset) {
                            detectTapGestures { tapOffset ->
                                // Inverse transform to find tapped node in canvas coordinates
                                val canvasX = (tapOffset.x - offset.x) / scale
                                val canvasY = (tapOffset.y - offset.y) / scale

                                for (node in nodes) {
                                    val dx = node.x - canvasX
                                    val dy = node.y - canvasY
                                    val r = 24f + (node.connectionCount * 4f).coerceAtMost(20f)
                                    if (sqrt(dx * dx + dy * dy) <= r * 1.5f) {
                                        onOpenNote(node.id)
                                        break
                                    }
                                }
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f

                        // Draw Edges (Connecting Lines)
                        for (edge in edges) {
                            val src = nodeMap[edge.sourceId] ?: continue
                            val dst = nodeMap[edge.targetId] ?: continue

                            val startX = (src.x * scale) + offset.x + (centerX * (1f - scale))
                            val startY = (src.y * scale) + offset.y + (centerY * (1f - scale))
                            val endX = (dst.x * scale) + offset.x + (centerX * (1f - scale))
                            val endY = (dst.y * scale) + offset.y + (centerY * (1f - scale))

                            drawLine(
                                color = colors.accent.copy(alpha = 0.35f),
                                start = Offset(startX, startY),
                                end = Offset(endX, endY),
                                strokeWidth = 2.dp.toPx() * scale.coerceAtLeast(0.6f)
                            )
                        }

                        // Draw Nodes
                        val textPaint = android.graphics.Paint().apply {
                            color = android.graphics.Color.argb(220, 240, 240, 240)
                            textSize = 12.sp.toPx() * scale.coerceIn(0.8f, 1.4f)
                            textAlign = android.graphics.Paint.Align.CENTER
                            isAntiAlias = true
                        }

                        for (node in nodes) {
                            val screenX = (node.x * scale) + offset.x + (centerX * (1f - scale))
                            val screenY = (node.y * scale) + offset.y + (centerY * (1f - scale))
                            val radius = (18f + (node.connectionCount * 3f).coerceAtMost(16f)) * scale

                            // Outer glow ring
                            drawCircle(
                                color = colors.accent.copy(alpha = 0.2f),
                                radius = radius + 6.dp.toPx(),
                                center = Offset(screenX, screenY)
                            )

                            // Node Body
                            drawCircle(
                                color = colors.surface,
                                radius = radius,
                                center = Offset(screenX, screenY)
                            )

                            // Node Border
                            drawCircle(
                                color = colors.accent,
                                radius = radius,
                                center = Offset(screenX, screenY),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                            )

                            // Title Label
                            val label = if (node.title.length > 18) node.title.take(16) + "…" else node.title
                            drawContext.canvas.nativeCanvas.drawText(
                                label,
                                screenX,
                                screenY + radius + 16.dp.toPx(),
                                textPaint
                            )
                        }
                    }
                }
            }
        }
    }
}
