package com.studyos.app.data.repository

import com.studyos.app.core.database.dao.TopicDao
import com.studyos.app.core.database.entity.toDomain
import com.studyos.app.core.database.entity.toEntity
import com.studyos.app.domain.model.Topic
import com.studyos.app.domain.model.TopicMasteryState
import com.studyos.app.domain.repository.TopicRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OfflineTopicRepository(
    private val topicDao: TopicDao
) : TopicRepository {

    override fun getTopicsForChapter(chapterId: String): Flow<List<Topic>> =
        topicDao.getTopicsForChapter(chapterId).map { list -> list.map { it.toDomain() } }

    override suspend fun getTopicsForChapterOnce(chapterId: String): List<Topic> =
        topicDao.getTopicsForChapterOnce(chapterId).map { it.toDomain() }

    override suspend fun getTopicById(id: String): Topic? =
        topicDao.getTopicById(id)?.toDomain()

    override fun observeTopicById(id: String): Flow<Topic?> =
        topicDao.observeTopicById(id).map { it?.toDomain() }

    override fun getTopicsByMasteryState(state: TopicMasteryState): Flow<List<Topic>> =
        topicDao.getTopicsByMasteryState(state.name).map { list -> list.map { it.toDomain() } }

    override fun getWeakestTopics(limit: Int): Flow<List<Topic>> =
        topicDao.getWeakestTopics(limit).map { list -> list.map { it.toDomain() } }

    override suspend fun saveTopic(topic: Topic) {
        topicDao.insertTopic(topic.toEntity())
    }

    override suspend fun saveTopics(topics: List<Topic>) {
        topicDao.insertTopics(topics.map { it.toEntity() })
    }

    override suspend fun updateMasteryState(id: String, state: TopicMasteryState) {
        topicDao.updateMasteryState(id, state.name)
    }

    override suspend fun updateWeaknessScore(id: String, score: Float) {
        topicDao.updateWeaknessScore(id, score)
    }

    override suspend fun deleteTopic(topic: Topic) {
        topicDao.deleteTopic(topic.toEntity())
    }

    override suspend fun deleteTopicById(id: String) {
        topicDao.deleteTopicById(id)
    }
}
