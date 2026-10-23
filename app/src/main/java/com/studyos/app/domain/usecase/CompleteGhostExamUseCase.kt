package com.studyos.app.domain.usecase

import com.studyos.app.domain.model.PaperStatus
import com.studyos.app.domain.repository.LostMarksRepository
import com.studyos.app.domain.repository.MistakeRepository
import com.studyos.app.domain.repository.PaperRepository

class CompleteGhostExamUseCase(
    private val paperRepository: PaperRepository,
    private val lostMarksRepository: LostMarksRepository,
    private val mistakeRepository: MistakeRepository
) {
    suspend operator fun invoke(
        paperId: String,
        remediatedLostMarkIds: List<String> = emptyList(),
        remediatedMistakeIds: List<String> = emptyList()
    ) {
        val paper = paperRepository.getPaperById(paperId)
        if (paper != null) {
            val updated = paper.copy(status = PaperStatus.COMPLETED)
            val questions = paperRepository.getQuestionsForPaper(paperId)
            paperRepository.savePaper(updated, questions)
        }

        remediatedLostMarkIds.forEach { id ->
            lostMarksRepository.updateRemediation(id, true)
        }

        remediatedMistakeIds.forEach { id ->
            mistakeRepository.resolveMistake(id)
        }
    }
}
