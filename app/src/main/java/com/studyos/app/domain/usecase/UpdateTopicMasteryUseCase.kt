package com.studyos.app.domain.usecase

import com.studyos.app.domain.model.TopicMasteryState
import com.studyos.app.domain.repository.TopicRepository

class UpdateTopicMasteryUseCase(
    private val topicRepository: TopicRepository
) {
    suspend operator fun invoke(topicId: String, state: TopicMasteryState) {
        topicRepository.updateMasteryState(topicId, state)
    }
}
