package com.studyos.app.domain.usecase

import com.studyos.app.domain.engine.LeverageMatrixEngine
import com.studyos.app.domain.model.LeverageMatrixReport
import com.studyos.app.domain.repository.ChapterRepository
import com.studyos.app.domain.repository.SubjectRepository
import com.studyos.app.domain.repository.TopicRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class CalculateLeverageMatrixUseCase(
    private val topicRepository: TopicRepository,
    private val chapterRepository: ChapterRepository,
    private val subjectRepository: SubjectRepository
) {
    operator fun invoke(): Flow<LeverageMatrixReport> {
        return combine(
            topicRepository.getAllTopics(),
            chapterRepository.observeAllChapters(),
            subjectRepository.getAllSubjects()
        ) { topics: List<com.studyos.app.domain.model.Topic>, chapters: List<com.studyos.app.domain.model.Chapter>, subjects: List<com.studyos.app.domain.model.Subject> ->
            LeverageMatrixEngine.buildMatrix(
                topics = topics,
                chapters = chapters,
                subjects = subjects
            )
        }
    }
}
