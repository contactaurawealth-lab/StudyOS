package com.studyos.app.domain.usecase

import com.studyos.app.domain.model.ExamRelevance
import com.studyos.app.domain.model.Topic
import com.studyos.app.domain.model.TopicMasteryState
import com.studyos.app.domain.repository.TopicRepository
import java.util.UUID

sealed class AddTopicResult {
    data class Success(val topic: Topic) : AddTopicResult()
    data class Error(val message: String) : AddTopicResult()
}

class AddTopicUseCase(
    private val topicRepository: TopicRepository
) {
    suspend operator fun invoke(
        chapterId: String,
        name: String,
        examRelevance: ExamRelevance = ExamRelevance.MEDIUM
    ): AddTopicResult {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            return AddTopicResult.Error("Topic name cannot be empty.")
        }
        val existing = topicRepository.getTopicsForChapterOnce(chapterId)
        if (existing.any { it.name.equals(trimmed, ignoreCase = true) }) {
            return AddTopicResult.Error("A topic with this name already exists in this chapter.")
        }
        val topic = Topic(
            id = UUID.randomUUID().toString(),
            chapterId = chapterId,
            name = trimmed,
            masteryState = TopicMasteryState.NOT_STARTED,
            examRelevance = examRelevance,
            orderIndex = existing.size
        )
        topicRepository.saveTopic(topic)
        return AddTopicResult.Success(topic)
    }
}
