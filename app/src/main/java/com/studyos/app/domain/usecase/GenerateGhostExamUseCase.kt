package com.studyos.app.domain.usecase

import com.studyos.app.domain.engine.GhostExamEngine
import com.studyos.app.domain.model.Paper
import com.studyos.app.domain.model.PaperQuestionItem
import com.studyos.app.domain.model.PaperStatus
import com.studyos.app.domain.repository.ChapterRepository
import com.studyos.app.domain.repository.LostMarksRepository
import com.studyos.app.domain.repository.MistakeRepository
import com.studyos.app.domain.repository.PaperRepository
import com.studyos.app.domain.repository.QuestionBankRepository
import com.studyos.app.domain.repository.SubjectRepository
import kotlinx.coroutines.flow.first
import java.util.UUID

class GenerateGhostExamUseCase(
    private val paperRepository: PaperRepository,
    private val lostMarksRepository: LostMarksRepository,
    private val mistakeRepository: MistakeRepository,
    private val questionBankRepository: QuestionBankRepository,
    private val subjectRepository: SubjectRepository,
    private val chapterRepository: ChapterRepository
) {
    suspend operator fun invoke(targetSubjectId: String? = null): Paper {
        val subjects = subjectRepository.getAllSubjects().first()
        if (subjects.isEmpty()) {
            throw IllegalStateException("No subjects available to generate Ghost Exam.")
        }

        val allLostMarks = lostMarksRepository.getAllLostMarks().first().filter { !it.isRemediated }
        val allMistakes = mistakeRepository.observeAllMistakes().first().filter { !it.isResolved }

        val subject = if (targetSubjectId != null) {
            subjects.firstOrNull { it.id == targetSubjectId } ?: subjects.first()
        } else {
            // Pick subject with most leakage or unresolved mistakes
            val subjectLossCounts = subjects.associateWith { sub ->
                allLostMarks.count { it.resultId.contains(sub.id) } + allMistakes.count { it.subjectId == sub.id }
            }
            subjectLossCounts.maxByOrNull { it.value }?.key ?: subjects.first()
        }

        val subjectChapters = chapterRepository.getChaptersForSubjectOnce(subject.id)
        val fallbackChapterId = subjectChapters.firstOrNull()?.id ?: run {
            val newChap = chapterRepository.addChapter(subject.id, "Remediation Core", null)
            newChap.id
        }

        val subjectLostMarks = allLostMarks.filter {
            it.topicId != null || targetSubjectId == null || it.resultId.contains(subject.id)
        }
        val subjectMistakes = allMistakes.filter { it.subjectId == subject.id }

        val existingQuestions = questionBankRepository.getQuestionsForSubject(subject.id).first()

        val plan = GhostExamEngine.generatePlan(
            subjectId = subject.id,
            subjectName = subject.name,
            fallbackChapterId = fallbackChapterId,
            unremediatedLostMarks = subjectLostMarks,
            unresolvedMistakes = subjectMistakes,
            existingQuestions = existingQuestions
        )

        // Save any generated questions
        val newQuestions = plan.questionItems.map { it.question }
        questionBankRepository.saveQuestions(newQuestions)

        val paperId = UUID.randomUUID().toString()
        val paper = Paper(
            id = paperId,
            subjectId = subject.id,
            title = plan.title,
            totalMarks = plan.totalMarks,
            durationMinutes = plan.durationMinutes,
            sectionsJson = plan.sectionsJson,
            pdfUri = null,
            status = PaperStatus.GENERATED,
            createdAt = System.currentTimeMillis()
        )

        val paperQuestionItems = plan.questionItems.map { item ->
            PaperQuestionItem(
                paperId = paperId,
                questionId = item.question.id,
                sectionName = item.sectionName,
                questionNumber = item.questionNumber,
                marksAllocated = item.marksAllocated
            )
        }

        paperRepository.savePaper(paper, paperQuestionItems)
        return paper
    }
}
