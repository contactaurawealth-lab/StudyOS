package com.studyos.app.core.workspace

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceStateManagerTest {

    @Test
    fun testSvgCircleOffsetCalculation() {
        // At start (0 seconds elapsed), offset should be 0.00
        val offsetStart = WorkspaceStateManager.calculateCircleOffset(remainingSeconds = 1500, totalSeconds = 1500)
        assertEquals("0.00", offsetStart)

        // At halfway (750 seconds remaining out of 1500), offset should be ~141.37
        val offsetHalf = WorkspaceStateManager.calculateCircleOffset(remainingSeconds = 750, totalSeconds = 1500)
        val halfDouble = offsetHalf.toDouble()
        assertTrue("Halfway offset should be approximately 141.37, got $halfDouble", halfDouble in 141.0..142.0)

        // At finish (0 seconds remaining), offset should be full circumference ~282.74
        val offsetFinish = WorkspaceStateManager.calculateCircleOffset(remainingSeconds = 0, totalSeconds = 1500)
        val finishDouble = offsetFinish.toDouble()
        assertTrue("Finish offset should be approximately 282.74, got $finishDouble", finishDouble in 282.0..283.0)
    }

    @Test
    fun testDocumentRoutingTable() {
        val pdfRoute = WorkspaceStateManager.routeDocument("lecture_notes.pdf")
        assertEquals("pdf_embed_pane", pdfRoute.targetViewer)
        assertTrue(pdfRoute.availableActions.contains("extract_summary"))
        assertTrue(pdfRoute.availableActions.contains("generate_qa_cards"))

        val mdRoute = WorkspaceStateManager.routeDocument("readme.md")
        assertEquals("monaco_markdown_editor", mdRoute.targetViewer)
        assertTrue(mdRoute.availableActions.contains("format_to_outline"))

        val docxRoute = WorkspaceStateManager.routeDocument("financial_model.xlsx")
        assertEquals("office_grid_embed_viewer", docxRoute.targetViewer)
        assertTrue(docxRoute.availableActions.contains("parse_table_data"))

        val imgRoute = WorkspaceStateManager.routeDocument("whiteboard_scan.png")
        assertEquals("lightbox_ocr_viewer", imgRoute.targetViewer)
        assertTrue(imgRoute.availableActions.contains("run_ocr_to_markdown"))

        val codeRoute = WorkspaceStateManager.routeDocument("algorithm.py")
        assertEquals("monaco_code_viewer", codeRoute.targetViewer)
        assertTrue(codeRoute.availableActions.contains("find_bugs"))
    }

    @Test
    fun testPresetLayoutSynthesis() {
        val academic = WorkspaceStateManager.createPresetPayload(WorkspacePreset.ACADEMIC_STUDY)
        assertEquals("focus", academic.systemState.activeMode)
        assertEquals(5, academic.activeWidgets.size)
        val academicWidgetIds = academic.activeWidgets.map { it.id }
        assertTrue(academicWidgetIds.contains("widget_focus_timer"))
        assertTrue(academicWidgetIds.contains("widget_doc_viewer"))
        assertTrue(academicWidgetIds.contains("widget_metric_counter"))
        assertTrue(academicWidgetIds.contains("widget_flashcards"))
        assertTrue(academicWidgetIds.contains("widget_concept_graph"))

        val deepWork = WorkspaceStateManager.createPresetPayload(WorkspacePreset.DEEP_WORK)
        val deepWorkWidgetIds = deepWork.activeWidgets.map { it.id }
        assertTrue(deepWorkWidgetIds.contains("widget_focus_timer"))
        assertTrue(deepWorkWidgetIds.contains("widget_task_list"))
        assertTrue(deepWorkWidgetIds.contains("widget_scratchpad"))
        assertTrue(deepWorkWidgetIds.contains("widget_doc_viewer"))
        assertTrue(deepWorkWidgetIds.contains("widget_sound_player"))
    }

    @Test
    fun testJsonSerializationAndDeserializationCycle() {
        val original = WorkspaceStateManager.createPresetPayload(WorkspacePreset.ACADEMIC_STUDY)
        val jsonString = WorkspaceStateManager.toJson(original)

        assertNotNull(jsonString)
        assertTrue(jsonString.contains("\"system_state\""))
        assertTrue(jsonString.contains("\"timer\""))
        assertTrue(jsonString.contains("\"active_widgets\""))
        assertTrue(jsonString.contains("\"document_routing\""))

        val deserialized = WorkspaceStateManager.fromJson(jsonString)
        assertEquals(original.systemState.activeMode, deserialized.systemState.activeMode)
        assertEquals(original.timer.title, deserialized.timer.title)
        assertEquals(original.timer.durationSeconds, deserialized.timer.durationSeconds)
        assertEquals(original.activeWidgets.size, deserialized.activeWidgets.size)
        assertEquals(original.documentRouting.targetViewer, deserialized.documentRouting.targetViewer)
    }
}
