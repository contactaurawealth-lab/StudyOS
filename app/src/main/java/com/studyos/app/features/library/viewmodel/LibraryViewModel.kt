package com.studyos.app.features.library.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyos.app.core.database.dao.ChapterDao
import com.studyos.app.core.database.dao.FlashcardDao
import com.studyos.app.core.database.dao.NoteDao
import com.studyos.app.core.database.dao.ResourceDao
import com.studyos.app.core.database.dao.SubjectDao
import com.studyos.app.core.database.entity.ChapterEntity
import com.studyos.app.core.database.entity.FlashcardEntity
import com.studyos.app.core.database.entity.NoteEntity
import com.studyos.app.core.database.entity.ResourceEntity
import com.studyos.app.core.database.entity.SubjectEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class LibraryTab {
    NOTES,
    FLASHCARDS,
    FORMULAS,
    RESOURCES
}

data class LibraryNoteItem(
    val note: NoteEntity,
    val subjectName: String,
    val chapterName: String
)

data class LibraryDeckItem(
    val chapterId: String,
    val chapterName: String,
    val subjectId: String,
    val subjectName: String,
    val totalCards: Int,
    val dueCards: Int,
    val masteryPercentage: Int
)

data class LibraryFormulaItem(
    val id: String,
    val noteId: String,
    val chapterId: String?,
    val subjectId: String?,
    val subjectName: String,
    val chapterName: String,
    val title: String,
    val formulaContent: String
)

data class LibraryResourceItem(
    val resource: ResourceEntity,
    val subjectName: String?
)

data class LibraryUiState(
    val currentTab: LibraryTab = LibraryTab.NOTES,
    val searchQuery: String = "",
    val selectedSubjectId: String? = null,
    val subjects: List<SubjectEntity> = emptyList(),
    val notes: List<LibraryNoteItem> = emptyList(),
    val decks: List<LibraryDeckItem> = emptyList(),
    val formulas: List<LibraryFormulaItem> = emptyList(),
    val resources: List<LibraryResourceItem> = emptyList(),
    val isLoading: Boolean = true,
    val isAddResourceDialogOpen: Boolean = false
)

class LibraryViewModel(
    private val noteDao: NoteDao,
    private val flashcardDao: FlashcardDao,
    private val resourceDao: ResourceDao,
    private val subjectDao: SubjectDao,
    private val chapterDao: ChapterDao
) : ViewModel() {

    private val _currentTab = MutableStateFlow(LibraryTab.NOTES)
    private val _searchQuery = MutableStateFlow("")
    private val _selectedSubjectId = MutableStateFlow<String?>(null)
    private val _isAddResourceDialogOpen = MutableStateFlow(false)

    val uiState: StateFlow<LibraryUiState> = combine(
        combine(_currentTab, _searchQuery, _selectedSubjectId, _isAddResourceDialogOpen) { tab, search, subId, isAddOpen ->
            Quad(tab, search, subId, isAddOpen)
        },
        noteDao.observeAllNotes(),
        flashcardDao.observeAllFlashcards(),
        resourceDao.getAllResources(),
        combine(subjectDao.getAllSubjects(), chapterDao.observeAllChapters()) { subs, chaps ->
            Pair(subs, chaps)
        }
    ) { (tab, search, subId, isAddOpen), notesList, flashcardsList, resourcesList, (subjects, chapters) ->
        val subjectMap = subjects.associateBy { it.id }
        val chapterMap = chapters.associateBy { it.id }
        val now = System.currentTimeMillis()

        // 1. Enriched notes
        val enrichedNotes = notesList.map { note ->
            val subName = note.subjectId?.let { subjectMap[it]?.name } ?: "General"
            val chapName = note.chapterId?.let { chapterMap[it]?.name } ?: "Independent Note"
            LibraryNoteItem(note, subName, chapName)
        }.filter { item ->
            val matchesSubject = subId == null || item.note.subjectId == subId
            val matchesQuery = search.isBlank() ||
                    item.note.title.contains(search, ignoreCase = true) ||
                    item.note.content.contains(search, ignoreCase = true) ||
                    item.subjectName.contains(search, ignoreCase = true) ||
                    item.chapterName.contains(search, ignoreCase = true)
            matchesSubject && matchesQuery
        }

        // 2. Flashcard decks grouped by chapter
        val decksByChapter = flashcardsList.groupBy { it.chapterId }
        val enrichedDecks = decksByChapter.mapNotNull { (chapterId, cards) ->
            if (chapterId == null) return@mapNotNull null
            val chapter = chapterMap[chapterId] ?: return@mapNotNull null
            val subject = subjectMap[chapter.subjectId]
            val subName = subject?.name ?: "General"
            val due = cards.count { it.nextReview != null && it.nextReview <= now }
            val mastered = cards.count { it.reviewCount >= 3 }
            val masteryPct = if (cards.isNotEmpty()) (mastered * 100) / cards.size else 0

            LibraryDeckItem(
                chapterId = chapterId,
                chapterName = chapter.name,
                subjectId = chapter.subjectId,
                subjectName = subName,
                totalCards = cards.size,
                dueCards = due,
                masteryPercentage = masteryPct
            )
        }.filter { deck ->
            val matchesSubject = subId == null || deck.subjectId == subId
            val matchesQuery = search.isBlank() ||
                    deck.chapterName.contains(search, ignoreCase = true) ||
                    deck.subjectName.contains(search, ignoreCase = true)
            matchesSubject && matchesQuery
        }.sortedByDescending { it.dueCards }

        // 3. Extracted formulas & equations from notes
        val extractedFormulas = mutableListOf<LibraryFormulaItem>()
        notesList.forEach { note ->
            val subName = note.subjectId?.let { subjectMap[it]?.name } ?: "General"
            val chapName = note.chapterId?.let { chapterMap[it]?.name } ?: "Notes"

            // Look for blocks with $$ or lines with $ or key definitions
            val lines = note.content.lines()
            var inMathBlock = false
            val currentBlock = StringBuilder()

            lines.forEachIndexed { index, line ->
                val trimmed = line.trim()
                if (trimmed.startsWith("$$")) {
                    if (inMathBlock) {
                        currentBlock.append("\n").append(trimmed)
                        extractedFormulas.add(
                            LibraryFormulaItem(
                                id = "${note.id}_formula_$index",
                                noteId = note.id,
                                chapterId = note.chapterId,
                                subjectId = note.subjectId,
                                subjectName = subName,
                                chapterName = chapName,
                                title = note.title.ifBlank { "Formula" },
                                formulaContent = currentBlock.toString().trim()
                            )
                        )
                        currentBlock.clear()
                        inMathBlock = false
                    } else {
                        inMathBlock = true
                        currentBlock.append(trimmed)
                    }
                } else if (inMathBlock) {
                    currentBlock.append("\n").append(trimmed)
                } else if (trimmed.contains("$") && trimmed.length > 3) {
                    extractedFormulas.add(
                        LibraryFormulaItem(
                            id = "${note.id}_inline_$index",
                            noteId = note.id,
                            chapterId = note.chapterId,
                            subjectId = note.subjectId,
                            subjectName = subName,
                            chapterName = chapName,
                            title = note.title.ifBlank { "Definition" },
                            formulaContent = trimmed
                        )
                    )
                }
            }
        }

        val filteredFormulas = extractedFormulas.filter { formula ->
            val matchesSubject = subId == null || formula.subjectId == subId
            val matchesQuery = search.isBlank() ||
                    formula.title.contains(search, ignoreCase = true) ||
                    formula.formulaContent.contains(search, ignoreCase = true) ||
                    formula.chapterName.contains(search, ignoreCase = true) ||
                    formula.subjectName.contains(search, ignoreCase = true)
            matchesSubject && matchesQuery
        }

        // 4. Resources
        val enrichedResources = resourcesList.map { res ->
            val subName = res.subjectId?.let { subjectMap[it]?.name }
            LibraryResourceItem(res, subName)
        }.filter { item ->
            val matchesSubject = subId == null || item.resource.subjectId == subId
            val matchesQuery = search.isBlank() ||
                    item.resource.title.contains(search, ignoreCase = true) ||
                    (item.subjectName?.contains(search, ignoreCase = true) == true)
            matchesSubject && matchesQuery
        }

        LibraryUiState(
            currentTab = tab,
            searchQuery = search,
            selectedSubjectId = subId,
            subjects = subjects,
            notes = enrichedNotes,
            decks = enrichedDecks,
            formulas = filteredFormulas,
            resources = enrichedResources,
            isLoading = false,
            isAddResourceDialogOpen = isAddOpen
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LibraryUiState()
    )

    fun setTab(tab: LibraryTab) {
        _currentTab.value = tab
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectSubjectFilter(subjectId: String?) {
        _selectedSubjectId.value = subjectId
    }

    fun togglePinNote(noteId: String, currentPinned: Boolean) {
        viewModelScope.launch {
            noteDao.setPinned(noteId, !currentPinned)
        }
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch {
            noteDao.deleteById(noteId)
        }
    }

    fun openAddResourceDialog() {
        _isAddResourceDialogOpen.value = true
    }

    fun closeAddResourceDialog() {
        _isAddResourceDialogOpen.value = false
    }

    fun addResource(title: String, type: String, uriOrPath: String, subjectId: String?) {
        viewModelScope.launch {
            val resource = ResourceEntity(
                title = title.trim(),
                type = type.uppercase(),
                uriOrPath = uriOrPath.trim(),
                subjectId = subjectId
            )
            resourceDao.insert(resource)
            closeAddResourceDialog()
        }
    }

    fun deleteResource(resource: ResourceEntity) {
        viewModelScope.launch {
            resourceDao.delete(resource)
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
