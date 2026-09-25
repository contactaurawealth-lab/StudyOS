package com.studyos.app.core.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentOpenerTest {

    @Test
    fun testIsMarkdownPathRecognizesMarkdownExtensions() {
        assertTrue(DocumentOpener.isMarkdownPath("notes.md"))
        assertTrue(DocumentOpener.isMarkdownPath("lecture_summary.markdown"))
        assertTrue(DocumentOpener.isMarkdownPath("DOCUMENT.MD"))
        assertTrue(DocumentOpener.isMarkdownPath("folder/subfolder/test.mdown"))
        assertTrue(DocumentOpener.isMarkdownPath("https://example.com/file.md?param=1#heading"))
    }

    @Test
    fun testIsMarkdownPathRejectsNonMarkdownFiles() {
        assertFalse(DocumentOpener.isMarkdownPath("lecture_slides.pdf"))
        assertFalse(DocumentOpener.isMarkdownPath("essay.docx"))
        assertFalse(DocumentOpener.isMarkdownPath("old_notes.doc"))
        assertFalse(DocumentOpener.isMarkdownPath("formula_sheet.txt"))
        assertFalse(DocumentOpener.isMarkdownPath("diagram.png"))
        assertFalse(DocumentOpener.isMarkdownPath("data.csv"))
        assertFalse(DocumentOpener.isMarkdownPath("https://google.com"))
    }
}
