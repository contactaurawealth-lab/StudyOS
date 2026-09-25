package com.studyos.app.domain.usecase

import com.studyos.app.domain.model.Chapter
import com.studyos.app.domain.model.ChapterStatus
import com.studyos.app.domain.model.Subject
import com.studyos.app.domain.repository.ChapterRepository
import com.studyos.app.domain.repository.SubjectRepository
import kotlinx.coroutines.flow.Flow

sealed class ChapterActionResult {
    data class Success(val chapter: Chapter) : ChapterActionResult()
    object EmptyName : ChapterActionResult()
    object DuplicateName : ChapterActionResult()
    data class Error(val message: String) : ChapterActionResult()
}

class GetChaptersForSubjectUseCase(
    private val chapterRepository: ChapterRepository
) {
    operator fun invoke(subjectId: String): Flow<List<Chapter>> =
        chapterRepository.observeChaptersForSubject(subjectId)

    suspend fun getOnce(subjectId: String): List<Chapter> =
        chapterRepository.getChaptersForSubjectOnce(subjectId)
}

class GetChapterUseCase(
    private val chapterRepository: ChapterRepository
) {
    operator fun invoke(id: String): Flow<Chapter?> = chapterRepository.getChapter(id)
    suspend fun getOnce(id: String): Chapter? = chapterRepository.getChapterById(id)
}

class AddChapterUseCase(
    private val chapterRepository: ChapterRepository
) {
    suspend operator fun invoke(
        subjectId: String,
        name: String,
        description: String? = null
    ): ChapterActionResult {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            return ChapterActionResult.EmptyName
        }

        val existingChapters = chapterRepository.getChaptersForSubjectOnce(subjectId)
        if (existingChapters.any { it.name.equals(trimmedName, ignoreCase = true) }) {
            return ChapterActionResult.DuplicateName
        }

        return try {
            val chapter = chapterRepository.addChapter(
                subjectId = subjectId,
                name = trimmedName,
                description = description?.trim()?.ifEmpty { null }
            )
            ChapterActionResult.Success(chapter)
        } catch (e: Exception) {
            ChapterActionResult.Error(e.message ?: "Couldn't save the chapter.")
        }
    }
}

class UpdateChapterUseCase(
    private val chapterRepository: ChapterRepository
) {
    suspend operator fun invoke(
        chapterId: String,
        name: String,
        description: String?,
        progress: Int
    ): ChapterActionResult {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            return ChapterActionResult.EmptyName
        }

        val existing = chapterRepository.getChapterById(chapterId)
            ?: return ChapterActionResult.Error("Chapter not found.")

        val existingChapters = chapterRepository.getChaptersForSubjectOnce(existing.subjectId)
        if (existingChapters.any { it.id != chapterId && it.name.equals(trimmedName, ignoreCase = true) }) {
            return ChapterActionResult.DuplicateName
        }

        return try {
            val updated = existing.copy(
                name = trimmedName,
                description = description?.trim()?.ifEmpty { null },
                progress = progress.coerceIn(0, 100)
            )
            chapterRepository.updateChapter(updated)
            ChapterActionResult.Success(updated)
        } catch (e: Exception) {
            ChapterActionResult.Error(e.message ?: "Couldn't save the chapter.")
        }
    }
}

class UpdateChapterProgressUseCase(
    private val chapterRepository: ChapterRepository
) {
    suspend operator fun invoke(chapterId: String, progress: Int) {
        chapterRepository.updateChapterProgress(chapterId, progress.coerceIn(0, 100))
    }
}

class UpdateChapterStatusUseCase(
    private val chapterRepository: ChapterRepository
) {
    suspend operator fun invoke(chapterId: String, status: ChapterStatus) {
        chapterRepository.updateChapterStatus(chapterId, status)
    }
}

class DeleteChapterUseCase(
    private val chapterRepository: ChapterRepository
) {
    suspend operator fun invoke(chapterId: String) {
        chapterRepository.deleteChapter(chapterId)
    }
}

class ReorderChaptersUseCase(
    private val chapterRepository: ChapterRepository
) {
    suspend operator fun invoke(subjectId: String, chapters: List<Chapter>) {
        chapterRepository.reorderChapters(subjectId, chapters)
    }
}

class MoveChapterUseCase(
    private val chapterRepository: ChapterRepository
) {
    suspend operator fun invoke(subjectId: String, chapterId: String, moveUp: Boolean) {
        chapterRepository.moveChapter(subjectId, chapterId, moveUp)
    }
}

class LoadSampleDataUseCase(
    private val subjectRepository: SubjectRepository,
    private val chapterRepository: ChapterRepository
) {
    suspend operator fun invoke() {
        val sampleSubjects = listOf(
            "Mathematics" to listOf(
                Triple("Algebra Basics", "Variables, terms, and basic equations", 75),
                Triple("Linear Equations", "Solving single and multi-step equations", 50),
                Triple("Geometry Fundamentals", "Angles, triangles, and area calculations", 0)
            ),
            "Science" to listOf(
                Triple("Matter & States", "Solids, liquids, gases, and phase changes", 100),
                Triple("Laws of Motion", "Newton's three laws of motion with practical examples", 25),
                Triple("Energy & Work", "Kinetic, potential energy, and work-energy theorem", 0)
            ),
            "History" to listOf(
                Triple("Early Civilizations", "Mesopotamia, Indus Valley, and Nile river valleys", 100),
                Triple("Medieval Period", "Feudal societies, trade networks, and cultural shifts", 66)
            )
        )

        for ((subjectName, chapters) in sampleSubjects) {
            val existing = subjectRepository.findByName(subjectName)
            val subject = existing ?: Subject(name = subjectName, isCustom = false).also {
                subjectRepository.saveSubject(it)
            }

            for ((chapterName, desc, progress) in chapters) {
                val existingChapters = chapterRepository.getChaptersForSubjectOnce(subject.id)
                if (existingChapters.none { it.name.equals(chapterName, ignoreCase = true) }) {
                    val added = chapterRepository.addChapter(subject.id, chapterName, desc)
                    if (progress > 0) {
                        chapterRepository.updateChapterProgress(added.id, progress)
                    }
                }
            }
        }
    }
}

class RecordChapterOpenedUseCase(
    private val chapterRepository: ChapterRepository
) {
    suspend operator fun invoke(chapterId: String) {
        chapterRepository.recordChapterOpened(chapterId)
    }
}
