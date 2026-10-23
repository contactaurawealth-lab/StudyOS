package com.studyos.app.domain.usecase

import android.content.Context
import com.studyos.app.core.paper.PdfPreExamPrimerGenerator
import com.studyos.app.domain.model.RecallItem
import com.studyos.app.domain.repository.ChapterRepository
import com.studyos.app.domain.repository.LostMarksRepository
import com.studyos.app.domain.repository.MistakeRepository
import com.studyos.app.domain.repository.RecallRepository
import com.studyos.app.domain.repository.SubjectRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

class GeneratePreExamPrimerUseCase(
    private val subjectRepository: SubjectRepository,
    private val chapterRepository: ChapterRepository,
    private val recallRepository: RecallRepository,
    private val lostMarksRepository: LostMarksRepository,
    private val mistakeRepository: MistakeRepository,
    private val pdfGenerator: PdfPreExamPrimerGenerator = PdfPreExamPrimerGenerator()
) {
    suspend operator fun invoke(
        context: Context,
        subjectId: String,
        examTitle: String? = null
    ): File = withContext(Dispatchers.IO) {
        val subject = subjectRepository.getSubjectByIdOnce(subjectId)
            ?: throw IllegalArgumentException("Subject with ID $subjectId not found")

        val chapters = chapterRepository.getChaptersForSubjectOnce(subjectId)
        val recallItems = mutableListOf<RecallItem>()
        for (ch in chapters) {
            recallItems.addAll(recallRepository.getRecallItemsForChapter(ch.id))
        }

        val allLostMarks = lostMarksRepository.getAllLostMarks().first()
        val subjectMistakes = mistakeRepository.observeMistakesForSubject(subjectId).first()

        pdfGenerator.generatePrimerPdf(
            context = context,
            subjectName = subject.name,
            recallItems = recallItems.take(20),
            lostMarks = allLostMarks,
            mistakes = subjectMistakes.take(10),
            examTitle = examTitle
        )
    }
}
