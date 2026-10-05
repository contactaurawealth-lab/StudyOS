package com.studyos.app.domain.usecase

import com.studyos.app.domain.repository.TopicRepository

class DeleteTopicUseCase(
    private val topicRepository: TopicRepository
) {
    suspend operator fun invoke(topicId: String) {
        topicRepository.deleteTopicById(topicId)
    }
}
