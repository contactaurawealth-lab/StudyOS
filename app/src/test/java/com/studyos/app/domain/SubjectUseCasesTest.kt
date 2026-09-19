package com.studyos.app.domain

import com.studyos.app.domain.model.Subject
import com.studyos.app.domain.repository.SubjectRepository
import com.studyos.app.domain.usecase.AddSubjectResult
import com.studyos.app.domain.usecase.AddSubjectUseCase
import com.studyos.app.domain.usecase.DeleteSubjectUseCase
import com.studyos.app.domain.usecase.GetSubjectsUseCase
import com.studyos.app.domain.usecase.RenameSubjectUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeSubjectRepository : SubjectRepository {
    val items = mutableListOf<Subject>()

    override fun getAllSubjects(): Flow<List<Subject>> = flowOf(items.toList())
    override suspend fun getAllSubjectsOnce(): List<Subject> = items.toList()
    override fun getSubjectById(id: String): Flow<Subject?> = flowOf(items.find { it.id == id })
    override suspend fun findByName(name: String): Subject? =
        items.find { it.name.equals(name.trim(), ignoreCase = true) }
    override suspend fun saveSubject(subject: Subject) {
        items.removeAll { it.id == subject.id }
        items.add(subject)
    }
    override suspend fun saveSubjects(subjects: List<Subject>) {
        subjects.forEach { saveSubject(it) }
    }
    override suspend fun updateSubject(subject: Subject) {
        saveSubject(subject)
    }
    override suspend fun deleteSubject(subject: Subject) {
        items.removeAll { it.id == subject.id }
    }
    override suspend fun deleteSubjectById(id: String) {
        items.removeAll { it.id == id }
    }
}

class SubjectUseCasesTest {

    private lateinit var repository: FakeSubjectRepository
    private lateinit var addSubjectUseCase: AddSubjectUseCase
    private lateinit var renameSubjectUseCase: RenameSubjectUseCase
    private lateinit var deleteSubjectUseCase: DeleteSubjectUseCase
    private lateinit var getSubjectsUseCase: GetSubjectsUseCase

    @Before
    fun setup() {
        repository = FakeSubjectRepository()
        addSubjectUseCase = AddSubjectUseCase(repository)
        renameSubjectUseCase = RenameSubjectUseCase(repository)
        deleteSubjectUseCase = DeleteSubjectUseCase(repository)
        getSubjectsUseCase = GetSubjectsUseCase(repository)
    }

    @Test
    fun testAddSubject_success() = runTest {
        val result = addSubjectUseCase("Physics")
        assertTrue(result is AddSubjectResult.Success)
        val added = (result as AddSubjectResult.Success).subject
        assertEquals("Physics", added.name)
        assertTrue(added.isCustom)
        assertEquals(1, repository.items.size)
    }

    @Test
    fun testAddSubject_emptyName_fails() = runTest {
        val result = addSubjectUseCase("   ")
        assertTrue(result is AddSubjectResult.EmptyName)
        assertEquals(0, repository.items.size)
    }

    @Test
    fun testAddSubject_duplicateCaseInsensitive_fails() = runTest {
        addSubjectUseCase("Mathematics")
        val duplicateResult = addSubjectUseCase("mathematics")
        assertTrue(duplicateResult is AddSubjectResult.DuplicateName)
        assertEquals(1, repository.items.size)
    }

    @Test
    fun testRenameSubject_success() = runTest {
        val addResult = addSubjectUseCase("Math") as AddSubjectResult.Success
        val renameResult = renameSubjectUseCase(addResult.subject.id, "Mathematics")
        assertTrue(renameResult is AddSubjectResult.Success)
        assertEquals("Mathematics", repository.items.first().name)
    }

    @Test
    fun testRenameSubject_duplicate_fails() = runTest {
        val s1 = (addSubjectUseCase("Chemistry") as AddSubjectResult.Success).subject
        addSubjectUseCase("Biology")
        val renameResult = renameSubjectUseCase(s1.id, "biology")
        assertTrue(renameResult is AddSubjectResult.DuplicateName)
    }

    @Test
    fun testDeleteSubject_removesItem() = runTest {
        val s1 = (addSubjectUseCase("History") as AddSubjectResult.Success).subject
        assertEquals(1, repository.items.size)
        deleteSubjectUseCase(s1.id)
        assertEquals(0, repository.items.size)
    }
}
