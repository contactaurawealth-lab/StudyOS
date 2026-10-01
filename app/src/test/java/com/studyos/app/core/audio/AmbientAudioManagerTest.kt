package com.studyos.app.core.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AmbientAudioManagerTest {

    @Test
    fun testBuiltInTracksList() {
        val tracks = AmbientSoundTrack.BUILT_IN_TRACKS
        assertEquals(5, tracks.size)

        val cafe = tracks.find { it.presetType == AmbientPresetType.CAFE }
        assertNotNull(cafe)
        assertEquals("Cozy Corner Cafe", cafe?.name)
        assertEquals("☕", cafe?.emoji)

        val rain = tracks.find { it.presetType == AmbientPresetType.RAIN }
        assertNotNull(rain)
        assertEquals("Gentle Rain on Leaves", rain?.name)
        assertEquals("🌧️", rain?.emoji)

        val forest = tracks.find { it.presetType == AmbientPresetType.FOREST }
        assertNotNull(forest)

        val lofi = tracks.find { it.presetType == AmbientPresetType.LOFI }
        assertNotNull(lofi)

        val drone = tracks.find { it.presetType == AmbientPresetType.DRONE }
        assertNotNull(drone)
    }

    @Test
    fun testVolumeClamping() {
        AmbientAudioManager.setVolume(1.5f)
        assertEquals(1.0f, AmbientAudioManager.volume.value, 0.001f)

        AmbientAudioManager.setVolume(-0.2f)
        assertEquals(0.0f, AmbientAudioManager.volume.value, 0.001f)

        AmbientAudioManager.setVolume(0.75f)
        assertEquals(0.75f, AmbientAudioManager.volume.value, 0.001f)
    }

    @Test
    fun testCustomTrackManagement() {
        val custom = AmbientSoundTrack(
            id = "custom_test_1",
            name = "My Piano Meditation",
            emoji = "🎵",
            category = "Custom",
            isBuiltIn = false,
            fileUri = "content://media/external/audio/123",
            presetType = AmbientPresetType.CUSTOM
        )

        AmbientAudioManager.addCustomTrack(custom)
        assertTrue(AmbientAudioManager.customTracks.value.any { it.id == "custom_test_1" })

        AmbientAudioManager.removeCustomTrack("custom_test_1")
        assertFalse(AmbientAudioManager.customTracks.value.any { it.id == "custom_test_1" })
    }
}
