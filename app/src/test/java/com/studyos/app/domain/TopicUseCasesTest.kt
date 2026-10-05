package com.studyos.app.domain

import com.studyos.app.domain.model.ExamRelevance
import com.studyos.app.domain.model.Topic
import com.studyos.app.domain.model.TopicMasteryState
import com.studyos.app.domain.repository.TopicRepository
import com.studyos.app.domain.usecase.AddTopicResult
import com.studyos.app.domain.usecase.AddTopicUseCase
import com.studyos.app.domain.usecase.DeleteTopicUseCase
import com.studyos.app.domain.usecase.GetTopicsForChapterUseCase
import com.studyos.app.domain.usecase.UpdateTopicMasteryUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeTopicRepository : TopicRepository {
    private val topicsFlow = MutableStateFlow<List<Topic>>(emptyList())
    val items = mutableListOf<Topic>()

    private fun sync() {
        topicsFlow.value = items.toList()
    }

    override fun getTopicsForChapter(chapterId: String): Flow<List<Topic>> =
        topicsFlow.map { list -> list.filter { it.chapterId == chapterId }.sortedBy { it.orderIndex } }

    override suspend fun getTopicsForChapterOnce(chapterId: String): List<Topic> =
        items.filter { it.chapterId == chapterId }.sortedBy { it.orderIndex }

    override suspend fun getTopicById(id: String): Topic? =
        items.find { it.id == id }

    override fun observeTopicById(id: String): Flow<Topic?> =
        topicsFlow.map { list -> list.find { it.id == id } }

    override fun getTopicsByMasteryState(state: TopicMasteryState): Flow<List<Topic>> =
        topicsFlow.map { list -> list.filter { it.masteryState == state } }

    override fun getWeakestTopics(limit: Int): Flow<List<Topic>> =
        topicsFlow.map { list -> list.sortedByDescending { it.weaknessScore }.take(limit) }

    override suspend fun saveTopic(topic: Topic) {
        val index = items.indexOfFirst { it.id == topic.id }
        if (index >= 0) {
            items[index] = topic
        } else {
            items.add(topic)
        }
        sync()
    }

    override suspend fun saveTopics(topics: List<Topic>) {
        topics.forEach { saveTopic(it) }
    }

    override suspend fun updateMasteryState(id: String, state: TopicMasteryState) {
        val index = items.indexOfFirst { it.id == id }
        if (index >= 0) {
            items[index] = items[index].copy(masteryState = state, lastRevisedAt = System.currentTimeMillis())
            sync()
        }
    }

    override suspend fun updateWeaknessScore(id: String, score: Float) {
        val index = items.indexOfFirst { it.id == id }
        if (index >= 0) {
            items[index] = items[index].copy(weaknessScore = score)
            sync()
        }
    }

    override suspend fun deleteTopic(topic: Topic) {
        items.removeAll { it.id == topic.id }
        sync()
    }

    override suspend fun deleteTopicById(id: String) {
        items.removeAll { it.id == id }
        sync()
    }
}

class TopicUseCasesTest {

    private lateinit var topicRepo: FakeTopicRepository
    private lateinit var getTopicsUseCase: GetTopicsForChapterUseCase
    private lateinit var addTopicUseCase: AddTopicUseCase
    private lateinit var updateMasteryUseCase: UpdateTopicMasteryUseCase
    private lateinit var deleteTopicUseCase: DeleteTopicUseCase

    @Before
    fun setUp() {
        topicRepo = FakeTopicRepository()
        getTopicsUseCase = GetTopicsForChapterUseCase(topicRepo)
        addTopicUseCase = AddTopicUseCase(topicRepo)
        updateMasteryUseCase = UpdateTopicMasteryUseCase(topicRepo)
        deleteTopicUseCase = DeleteTopicUseCase(topicRepo)
    }

    @Test
    fun testAddTopic_success() = runTest {
        val result = addTopicUseCase("chap-1", "Newton's First Law", ExamRelevance.HIGH)
        assertTrue(result is AddTopicResult.Success)
        val topic = (result as AddTopicResult.Success).topic
        assertEquals("Newton's First Law", topic.name)
        assertEquals("chap-1", topic.chapterId)
        assertEquals(TopicMasteryState.NOT_STARTED, topic.masteryState)
        assertEquals(ExamRelevance.HIGH, topic.examRelevance)
        assertEquals(0, topic.orderIndex)

        val topics = getTopicsUseCase("chap-1").first()
        assertEquals(1, topics.size)
        assertEquals("Newton's First Law", topics[0].name)
    }

    @Test
    fun testAddTopic_emptyName_rejected() = runTest {
        val result = addTopicUseCase("chap-1", "   ")
        assertTrue(result is AddTopicResult.Error)
        assertEquals("Topic name cannot be empty.", (result as AddTopicResult.Error).message)
    }

    @Test
    fun testAddTopic_duplicateName_rejected() = runTest {
        addTopicUseCase("chap-1", "Thermodynamics")
        val duplicateResult = addTopicUseCase("chap-1", "thermodynamics")
        assertTrue(duplicateResult is AddTopicResult.Error)
        assertEquals("A topic with this name already exists in this chapter.", (duplicateResult as AddTopicResult.Error).message)
    }

    @Test
    fun testUpdateTopicMastery_advancesCorrectly() = runTest {
        val result = addTopicUseCase("chap-1", "Kinematics") as AddTopicResult.Success
        val topicId = result.topic.id

        updateMasteryUseCase(topicId, TopicMasteryState.LEARNING)
        assertEquals(TopicMasteryState.LEARNING, topicRepo.getTopicById(topicId)?.masteryState)

        updateMasteryUseCase(topicId, TopicMasteryState.REVISED)
        assertEquals(TopicMasteryState.REVISED, topicRepo.getTopicById(topicId)?.masteryState)

        updateMasteryUseCase(topicId, TopicMasteryState.MASTERED)
        assertEquals(TopicMasteryState.MASTERED, topicRepo.getTopicById(topicId)?.masteryState)
    }

    @Test
    fun testDeleteTopic_removesFromList() = runTest {
        val t1 = (addTopicUseCase("chap-1", "Optics") as AddTopicResult.Success).topic
        val t2 = (addTopicUseCase("chap-1", "Magnetism") as AddTopicResult.Success).topic

        assertEquals(2, getTopicsUseCase("chap-1").first().size)

        deleteTopicUseCase(t1.id)
        val remaining = getTopicsUseCase("chap-1").first()
        assertEquals(1, remaining.size)
        assertEquals("Magnetism", remaining[0].name)
    }
}
