package com.studyos.app.domain

import com.studyos.app.domain.model.Chapter
import com.studyos.app.domain.model.ChapterStatus
import com.studyos.app.domain.model.calculateOverallProgress
import com.studyos.app.domain.model.calculateSubjectProgress
import com.studyos.app.domain.model.progressForStatus
import com.studyos.app.domain.model.statusForProgress
import org.junit.Assert.assertEquals
import org.junit.Test

class ProgressCalculationTest {

    @Test
    fun testSubjectProgress_emptyChapters_returnsZero() {
        val progress = calculateSubjectProgress(emptyList())
        assertEquals(0, progress)
    }

    @Test
    fun testSubjectProgress_singleChapter_exactValues() {
        val ch0 = Chapter.create(subjectId = "s1", name = "Ch0", progress = 0)
        assertEquals(0, calculateSubjectProgress(listOf(ch0)))

        val ch25 = Chapter.create(subjectId = "s1", name = "Ch25", progress = 25)
        assertEquals(25, calculateSubjectProgress(listOf(ch25)))

        val ch50 = Chapter.create(subjectId = "s1", name = "Ch50", progress = 50)
        assertEquals(50, calculateSubjectProgress(listOf(ch50)))

        val ch100 = Chapter.create(subjectId = "s1", name = "Ch100", progress = 100)
        assertEquals(100, calculateSubjectProgress(listOf(ch100)))
    }

    @Test
    fun testSubjectProgress_multiChapters_average() {
        // As specified in prompt section 5:
        // Chapter 1 -> 100%, Chapter 2 -> 50%, Chapter 3 -> 0% => 50%
        val ch1 = Chapter.create(subjectId = "s1", name = "Ch1", progress = 100)
        val ch2 = Chapter.create(subjectId = "s1", name = "Ch2", progress = 50)
        val ch3 = Chapter.create(subjectId = "s1", name = "Ch3", progress = 0)

        val subjectProgress = calculateSubjectProgress(listOf(ch1, ch2, ch3))
        assertEquals(50, subjectProgress)
    }

    @Test
    fun testOverallProgress_emptyChapters_returnsZero() {
        val progress = calculateOverallProgress(emptyList())
        assertEquals(0, progress)
    }

    @Test
    fun testOverallProgress_multiChapters() {
        val ch1 = Chapter.create(subjectId = "s1", name = "Ch1", progress = 100)
        val ch2 = Chapter.create(subjectId = "s1", name = "Ch2", progress = 60)
        val ch3 = Chapter.create(subjectId = "s2", name = "Ch3", progress = 40)
        val ch4 = Chapter.create(subjectId = "s2", name = "Ch4", progress = 0)

        // (100 + 60 + 40 + 0) / 4 = 200 / 4 = 50%
        val overall = calculateOverallProgress(listOf(ch1, ch2, ch3, ch4))
        assertEquals(50, overall)
    }

    @Test
    fun testStatusForProgress_automaticTransitions() {
        // 0 -> NOT_STARTED
        assertEquals(ChapterStatus.NOT_STARTED, statusForProgress(0))

        // 1..99 -> IN_PROGRESS
        assertEquals(ChapterStatus.IN_PROGRESS, statusForProgress(1))
        assertEquals(ChapterStatus.IN_PROGRESS, statusForProgress(25))
        assertEquals(ChapterStatus.IN_PROGRESS, statusForProgress(50))
        assertEquals(ChapterStatus.IN_PROGRESS, statusForProgress(99))

        // 100 -> COMPLETED
        assertEquals(ChapterStatus.COMPLETED, statusForProgress(100))

        // Clamping check
        assertEquals(ChapterStatus.NOT_STARTED, statusForProgress(-10))
        assertEquals(ChapterStatus.COMPLETED, statusForProgress(150))
    }

    @Test
    fun testProgressForStatus_mapping() {
        // COMPLETED -> 100
        assertEquals(100, progressForStatus(ChapterStatus.COMPLETED, currentProgress = 30))

        // NOT_STARTED -> 0
        assertEquals(0, progressForStatus(ChapterStatus.NOT_STARTED, currentProgress = 50))

        // IN_PROGRESS retains current if in 1..99, or defaults to 50
        assertEquals(40, progressForStatus(ChapterStatus.IN_PROGRESS, currentProgress = 40))
        assertEquals(50, progressForStatus(ChapterStatus.IN_PROGRESS, currentProgress = 0))
        assertEquals(50, progressForStatus(ChapterStatus.IN_PROGRESS, currentProgress = 100))
    }
}
