package com.studyos.app.domain

import com.studyos.app.domain.model.Chapter
import com.studyos.app.domain.model.ChapterStatus
import com.studyos.app.domain.model.progressForStatus
import com.studyos.app.domain.model.statusForProgress
import com.studyos.app.domain.repository.ChapterRepository
import com.studyos.app.domain.usecase.AddChapterUseCase
import com.studyos.app.domain.usecase.ChapterActionResult
import com.studyos.app.domain.usecase.DeleteChapterUseCase
import com.studyos.app.domain.usecase.GetChaptersForSubjectUseCase
import com.studyos.app.domain.usecase.MoveChapterUseCase
import com.studyos.app.domain.usecase.ReorderChaptersUseCase
import com.studyos.app.domain.usecase.UpdateChapterProgressUseCase
import com.studyos.app.domain.usecase.UpdateChapterStatusUseCase
import com.studyos.app.domain.usecase.UpdateChapterUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeChapterRepository : ChapterRepository {
    private val chaptersFlow = MutableStateFlow<List<Chapter>>(emptyList())
    val items = mutableListOf<Chapter>()

    private fun sync() {
        chaptersFlow.value = items.toList()
    }

    override fun observeChaptersForSubject(subjectId: String): Flow<List<Chapter>> =
        chaptersFlow.map { list -> list.filter { it.subjectId == subjectId }.sortedBy { it.orderIndex } }

    override suspend fun getChaptersForSubjectOnce(subjectId: String): List<Chapter> =
        items.filter { it.subjectId == subjectId }.sortedBy { it.orderIndex }

    override fun observeAllChapters(): Flow<List<Chapter>> =
        chaptersFlow.map { list -> list.sortedBy { it.orderIndex } }

    override suspend fun getAllChaptersOnce(): List<Chapter> =
        items.sortedBy { it.orderIndex }

    override fun getChapter(id: String): Flow<Chapter?> =
        chaptersFlow.map { list -> list.find { it.id == id } }

    override suspend fun getChapterById(id: String): Chapter? =
        items.find { it.id == id }

    override suspend fun addChapter(subjectId: String, name: String, description: String?): Chapter {
        val nextOrder = (items.filter { it.subjectId == subjectId }.maxOfOrNull { it.orderIndex } ?: -1) + 1
        val chapter = Chapter(
            subjectId = subjectId,
            name = name.trim(),
            description = description?.trim()?.ifEmpty { null },
            orderIndex = nextOrder,
            status = ChapterStatus.NOT_STARTED,
            progress = 0
        )
        items.add(chapter)
        sync()
        return chapter
    }

    override suspend fun updateChapter(chapter: Chapter) {
        items.removeAll { it.id == chapter.id }
        items.add(chapter)
        sync()
    }

    override suspend fun updateChapterProgress(id: String, progress: Int) {
        val chapter = items.find { it.id == id } ?: return
        val clamped = progress.coerceIn(0, 100)
        val updated = chapter.copy(progress = clamped, status = statusForProgress(clamped))
        items.removeAll { it.id == id }
        items.add(updated)
        sync()
    }

    override suspend fun updateChapterStatus(id: String, status: ChapterStatus) {
        val chapter = items.find { it.id == id } ?: return
        val newProgress = progressForStatus(status, chapter.progress)
        val updated = chapter.copy(status = status, progress = newProgress)
        items.removeAll { it.id == id }
        items.add(updated)
        sync()
    }

    override suspend fun deleteChapter(id: String) {
        items.removeAll { it.id == id }
        sync()
    }

    override suspend fun reorderChapters(subjectId: String, chapters: List<Chapter>) {
        items.removeAll { it.subjectId == subjectId }
        val updated = chapters.mapIndexed { index, chapter -> chapter.copy(orderIndex = index) }
        items.addAll(updated)
        sync()
    }

    override suspend fun moveChapter(subjectId: String, chapterId: String, moveUp: Boolean) {
        val current = items.filter { it.subjectId == subjectId }.sortedBy { it.orderIndex }.toMutableList()
        val index = current.indexOfFirst { it.id == chapterId }
        if (index == -1) return
        val targetIndex = if (moveUp) index - 1 else index + 1
        if (targetIndex !in current.indices) return
        val item = current.removeAt(index)
        current.add(targetIndex, item)
        reorderChapters(subjectId, current)
    }
}

class ChapterUseCasesTest {

    private lateinit var repository: FakeChapterRepository
    private lateinit var addChapterUseCase: AddChapterUseCase
    private lateinit var updateChapterUseCase: UpdateChapterUseCase
    private lateinit var updateChapterProgressUseCase: UpdateChapterProgressUseCase
    private lateinit var updateChapterStatusUseCase: UpdateChapterStatusUseCase
    private lateinit var deleteChapterUseCase: DeleteChapterUseCase
    private lateinit var moveChapterUseCase: MoveChapterUseCase
    private lateinit var getChaptersForSubjectUseCase: GetChaptersForSubjectUseCase

    @Before
    fun setup() {
        repository = FakeChapterRepository()
        addChapterUseCase = AddChapterUseCase(repository)
        updateChapterUseCase = UpdateChapterUseCase(repository)
        updateChapterProgressUseCase = UpdateChapterProgressUseCase(repository)
        updateChapterStatusUseCase = UpdateChapterStatusUseCase(repository)
        deleteChapterUseCase = DeleteChapterUseCase(repository)
        moveChapterUseCase = MoveChapterUseCase(repository)
        getChaptersForSubjectUseCase = GetChaptersForSubjectUseCase(repository)
    }

    @Test
    fun testAddChapter_success() = runTest {
        val result = addChapterUseCase(
            subjectId = "sub1",
            name = "Algebra Basics",
            description = "Fundamental operations"
        )
        assertTrue(result is ChapterActionResult.Success)
        val chapter = (result as ChapterActionResult.Success).chapter
        assertEquals("Algebra Basics", chapter.name)
        assertEquals("Fundamental operations", chapter.description)
        assertEquals(0, chapter.orderIndex)
        assertEquals(0, chapter.progress)
        assertEquals(ChapterStatus.NOT_STARTED, chapter.status)
        assertEquals(1, repository.items.size)
    }

    @Test
    fun testAddChapter_emptyName_fails() = runTest {
        val result = addChapterUseCase(subjectId = "sub1", name = "   ")
        assertTrue(result is ChapterActionResult.EmptyName)
        assertEquals(0, repository.items.size)
    }

    @Test
    fun testAddChapter_duplicateNameInSameSubject_fails() = runTest {
        addChapterUseCase("sub1", "Geometry")
        val result = addChapterUseCase("sub1", "geometry")
        assertTrue(result is ChapterActionResult.DuplicateName)
        assertEquals(1, repository.items.size)
    }

    @Test
    fun testAddChapter_duplicateNameInDifferentSubject_allowed() = runTest {
        addChapterUseCase("sub1", "Introduction")
        val result = addChapterUseCase("sub2", "Introduction")
        assertTrue(result is ChapterActionResult.Success)
        assertEquals(2, repository.items.size)
    }

    @Test
    fun testUpdateProgress_updatesStatusAutomatically() = runTest {
        val res = addChapterUseCase("sub1", "Motion") as ChapterActionResult.Success
        val id = res.chapter.id

        // Set to 50%
        updateChapterProgressUseCase(id, 50)
        var updated = repository.getChapterById(id)!!
        assertEquals(50, updated.progress)
        assertEquals(ChapterStatus.IN_PROGRESS, updated.status)

        // Set to 100%
        updateChapterProgressUseCase(id, 100)
        updated = repository.getChapterById(id)!!
        assertEquals(100, updated.progress)
        assertEquals(ChapterStatus.COMPLETED, updated.status)

        // Set to 0%
        updateChapterProgressUseCase(id, 0)
        updated = repository.getChapterById(id)!!
        assertEquals(0, updated.progress)
        assertEquals(ChapterStatus.NOT_STARTED, updated.status)
    }

    @Test
    fun testUpdateStatus_updatesProgressAutomatically() = runTest {
        val res = addChapterUseCase("sub1", "Cells") as ChapterActionResult.Success
        val id = res.chapter.id

        // Mark COMPLETED -> progress becomes 100
        updateChapterStatusUseCase(id, ChapterStatus.COMPLETED)
        var updated = repository.getChapterById(id)!!
        assertEquals(ChapterStatus.COMPLETED, updated.status)
        assertEquals(100, updated.progress)

        // Mark NOT_STARTED -> progress becomes 0
        updateChapterStatusUseCase(id, ChapterStatus.NOT_STARTED)
        updated = repository.getChapterById(id)!!
        assertEquals(ChapterStatus.NOT_STARTED, updated.status)
        assertEquals(0, updated.progress)
    }

    @Test
    fun testMoveChapter_reordersSuccessfully() = runTest {
        val c1 = (addChapterUseCase("sub1", "Ch1") as ChapterActionResult.Success).chapter
        val c2 = (addChapterUseCase("sub1", "Ch2") as ChapterActionResult.Success).chapter
        val c3 = (addChapterUseCase("sub1", "Ch3") as ChapterActionResult.Success).chapter

        assertEquals(listOf("Ch1", "Ch2", "Ch3"), repository.getChaptersForSubjectOnce("sub1").map { it.name })

        // Move Ch3 up
        moveChapterUseCase("sub1", c3.id, moveUp = true)
        assertEquals(listOf("Ch1", "Ch3", "Ch2"), repository.getChaptersForSubjectOnce("sub1").map { it.name })

        // Move Ch1 down
        moveChapterUseCase("sub1", c1.id, moveUp = false)
        assertEquals(listOf("Ch3", "Ch1", "Ch2"), repository.getChaptersForSubjectOnce("sub1").map { it.name })
    }

    @Test
    fun testDeleteChapter_removesFromRepository() = runTest {
        val c1 = (addChapterUseCase("sub1", "Ch1") as ChapterActionResult.Success).chapter
        assertEquals(1, repository.items.size)

        deleteChapterUseCase(c1.id)
        assertEquals(0, repository.items.size)
        assertNull(repository.getChapterById(c1.id))
    }
}
