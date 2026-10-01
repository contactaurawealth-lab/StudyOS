package com.studyos.app.core.assistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AssistantManagerTest {

    @Before
    fun setUp() {
        StudyOSAssistantManager.clearAssistContext()
        StudyOSAssistantManager.unregisterSessionFinisher()
    }

    @Test
    fun `initial assist context is null`() {
        assertNull(StudyOSAssistantManager.assistContext.value)
    }

    @Test
    fun `updateAssistContext stores screen context and clearAssistContext resets it`() {
        val testContext = AssistScreenContext(
            textContent = "Quantum mechanics wave particle duality",
            sourcePackage = "com.android.chrome",
            timestamp = 123456789L
        )

        StudyOSAssistantManager.updateAssistContext(testContext)
        val current = StudyOSAssistantManager.assistContext.value
        assertNotNull(current)
        assertEquals("Quantum mechanics wave particle duality", current?.textContent)
        assertEquals("com.android.chrome", current?.sourcePackage)
        assertEquals(123456789L, current?.timestamp)

        StudyOSAssistantManager.clearAssistContext()
        assertNull(StudyOSAssistantManager.assistContext.value)
    }

    @Test
    fun `notifyOverlayClosed invokes registered finisher and clears context`() {
        var finisherCalled = false
        StudyOSAssistantManager.registerSessionFinisher {
            finisherCalled = true
        }

        StudyOSAssistantManager.updateAssistContext(
            AssistScreenContext(textContent = "Study notes", sourcePackage = "com.test")
        )

        StudyOSAssistantManager.notifyOverlayClosed()

        assertTrue("Session finisher must be called on overlay close", finisherCalled)
        assertNull("Assist context must be cleared on overlay close", StudyOSAssistantManager.assistContext.value)
    }

    @Test
    fun `default assistant customization contains Gemini Aurora and Socratic Tutor`() {
        val defaultCustomization = AssistantCustomization()

        assertEquals(AssistantThemeGlow.GEMINI_AURORA, defaultCustomization.themeGlow)
        assertEquals(AssistantPersona.SOCRATIC_TUTOR, defaultCustomization.persona)
        assertTrue(defaultCustomization.autoListenOnLaunch)
        assertFalse(defaultCustomization.speakResponses)
        assertTrue(defaultCustomization.autoCaptureScreen)
        assertTrue(defaultCustomization.hapticFeedback)
    }

    @Test
    fun `all assistant theme glows have non-null titles and colors`() {
        for (glow in AssistantThemeGlow.entries) {
            assertTrue(glow.title.isNotBlank())
            assertTrue(glow.description.isNotBlank())
            assertNotNull(glow.startColor)
            assertNotNull(glow.endColor)
            assertNotNull(glow.accentColor)
        }
    }

    @Test
    fun `all assistant personas have non-null prompt instructions`() {
        for (persona in AssistantPersona.entries) {
            assertTrue(persona.title.isNotBlank())
            assertTrue(persona.promptInstruction.isNotBlank())
        }
    }
}
