package com.studyos.app.domain.usecase

import com.studyos.app.domain.model.Topic
import com.studyos.app.domain.repository.TopicRepository
import kotlinx.coroutines.flow.Flow

class GetTopicsForChapterUseCase(
    private val topicRepository: TopicRepository
) {
    operator fun invoke(chapterId: String): Flow<List<Topic>> =
        topicRepository.getTopicsForChapter(chapterId)

    suspend fun getOnce(chapterId: String): List<Topic> =
        topicRepository.getTopicsForChapterOnce(chapterId)
}
