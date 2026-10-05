package com.studyos.app.domain.repository

import com.studyos.app.domain.model.Topic
import com.studyos.app.domain.model.TopicMasteryState
import kotlinx.coroutines.flow.Flow

interface TopicRepository {
    fun getTopicsForChapter(chapterId: String): Flow<List<Topic>>
    suspend fun getTopicsForChapterOnce(chapterId: String): List<Topic>
    suspend fun getTopicById(id: String): Topic?
    fun observeTopicById(id: String): Flow<Topic?>
    fun getTopicsByMasteryState(state: TopicMasteryState): Flow<List<Topic>>
    fun getWeakestTopics(limit: Int): Flow<List<Topic>>
    suspend fun saveTopic(topic: Topic)
    suspend fun saveTopics(topics: List<Topic>)
    suspend fun updateMasteryState(id: String, state: TopicMasteryState)
    suspend fun updateWeaknessScore(id: String, score: Float)
    suspend fun deleteTopic(topic: Topic)
    suspend fun deleteTopicById(id: String)
}
