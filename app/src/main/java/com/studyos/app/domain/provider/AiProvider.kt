package com.studyos.app.domain.provider

import com.studyos.app.domain.model.AiConfig
import com.studyos.app.domain.model.AiMessage
import com.studyos.app.domain.model.AiStreamChunk
import com.studyos.app.domain.model.StudyContext
import kotlinx.coroutines.flow.Flow

interface AiProvider {
    fun generateStream(
        messages: List<AiMessage>,
        context: StudyContext?,
        config: AiConfig
    ): Flow<AiStreamChunk>
}
