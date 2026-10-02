package com.studyos.app.features.practice.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyos.app.core.database.dao.NoteDao
import com.studyos.app.core.database.entity.NoteEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

data class GraphNode(
    val id: String,
    val title: String,
    var x: Float,
    var y: Float,
    var vx: Float = 0f,
    var vy: Float = 0f,
    val connectionCount: Int = 0
)

data class GraphEdge(
    val sourceId: String,
    val targetId: String
)

data class KnowledgeGraphUiState(
    val isLoading: Boolean = true,
    val nodes: List<GraphNode> = emptyList(),
    val edges: List<GraphEdge> = emptyList(),
    val totalConnections: Int = 0
)

class KnowledgeGraphViewModel(
    private val noteDao: NoteDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(KnowledgeGraphUiState())
    val uiState: StateFlow<KnowledgeGraphUiState> = _uiState.asStateFlow()

    init {
        loadGraph()
    }

    fun loadGraph() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true) }
            val notes = noteDao.getAllNotesOnce()
            if (notes.isEmpty()) {
                _uiState.update { it.copy(isLoading = false, nodes = emptyList(), edges = emptyList()) }
                return@launch
            }

            val titleToIdMap = notes.associate { it.title.trim().lowercase() to it.id }
            val wikiRegex = Regex("\\[\\[([^\\]|]+)(?:\\|[^\\]]+)?\\]\\]")
            val edgeList = mutableListOf<GraphEdge>()
            val connectionCounts = mutableMapOf<String, Int>()

            for (note in notes) {
                val matches = wikiRegex.findAll(note.content)
                for (match in matches) {
                    val targetTitle = match.groupValues[1].trim().lowercase()
                    val targetId = titleToIdMap[targetTitle]
                    if (targetId != null && targetId != note.id) {
                        edgeList.add(GraphEdge(note.id, targetId))
                        connectionCounts[note.id] = (connectionCounts[note.id] ?: 0) + 1
                        connectionCounts[targetId] = (connectionCounts[targetId] ?: 0) + 1
                    }
                }
            }

            // Distribute initial positions in a circular layout
            val center = 500f
            val radius = 320f
            val count = notes.size
            val nodeList = notes.mapIndexed { index, note ->
                val angle = (2 * Math.PI * index / count.toDouble()).toFloat()
                val jitter = Random.nextFloat() * 40f - 20f
                GraphNode(
                    id = note.id,
                    title = note.title.ifBlank { "Untitled Note" },
                    x = center + (radius + jitter) * cos(angle),
                    y = center + (radius + jitter) * sin(angle),
                    connectionCount = connectionCounts[note.id] ?: 0
                )
            }

            // Run 50 iterations of force-directed layout simulation
            simulatePhysics(nodeList, edgeList, iterations = 60)

            _uiState.update {
                it.copy(
                    isLoading = false,
                    nodes = nodeList,
                    edges = edgeList,
                    totalConnections = edgeList.size
                )
            }
        }
    }

    private fun simulatePhysics(nodes: List<GraphNode>, edges: List<GraphEdge>, iterations: Int) {
        val nodeMap = nodes.associateBy { it.id }
        val kRepulsion = 45000f
        val kSpring = 0.04f
        val damping = 0.85f

        repeat(iterations) {
            // 1. Repulsion between all node pairs
            for (i in nodes.indices) {
                val n1 = nodes[i]
                for (j in i + 1 until nodes.size) {
                    val n2 = nodes[j]
                    val dx = n2.x - n1.x
                    val dy = n2.y - n1.y
                    val dist = sqrt(dx * dx + dy * dy).coerceAtLeast(20f)
                    val force = kRepulsion / (dist * dist)
                    val fx = (dx / dist) * force
                    val fy = (dy / dist) * force

                    n1.vx -= fx
                    n1.vy -= fy
                    n2.vx += fx
                    n2.vy += fy
                }
            }

            // 2. Spring attraction along edges
            for (edge in edges) {
                val n1 = nodeMap[edge.sourceId] ?: continue
                val n2 = nodeMap[edge.targetId] ?: continue
                val dx = n2.x - n1.x
                val dy = n2.y - n1.y
                val dist = sqrt(dx * dx + dy * dy)
                val force = (dist - 140f) * kSpring
                val fx = (dx / (dist.coerceAtLeast(1f))) * force
                val fy = (dy / (dist.coerceAtLeast(1f))) * force

                n1.vx += fx
                n1.vy += fy
                n2.vx -= fx
                n2.vy -= fy
            }

            // 3. Apply velocities and damping
            for (node in nodes) {
                node.x += node.vx * 0.5f
                node.y += node.vy * 0.5f
                node.vx *= damping
                node.vy *= damping
            }
        }
    }
}
