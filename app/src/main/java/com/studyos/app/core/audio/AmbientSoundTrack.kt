package com.studyos.app.core.audio

enum class AmbientPresetType {
    RAIN,
    CAFE,
    FOREST,
    LOFI,
    DRONE,
    CUSTOM
}

data class AmbientSoundTrack(
    val id: String,
    val name: String,
    val emoji: String,
    val category: String,
    val isBuiltIn: Boolean = true,
    val fileUri: String? = null,
    val presetType: AmbientPresetType = AmbientPresetType.CUSTOM,
    val description: String = ""
) {
    companion object {
        val BUILT_IN_TRACKS = listOf(
            AmbientSoundTrack(
                id = "ambient_cafe",
                name = "Cozy Corner Cafe",
                emoji = "☕",
                category = "Atmospheric",
                presetType = AmbientPresetType.CAFE,
                description = "Soft background murmur, gentle warmth, porcelain clinks"
            ),
            AmbientSoundTrack(
                id = "ambient_rain",
                name = "Gentle Rain on Leaves",
                emoji = "🌧️",
                category = "Nature",
                presetType = AmbientPresetType.RAIN,
                description = "Natural acoustic brown noise and soothing rainfall"
            ),
            AmbientSoundTrack(
                id = "ambient_forest",
                name = "Deep Forest Creek",
                emoji = "🌲",
                category = "Nature",
                presetType = AmbientPresetType.FOREST,
                description = "Tranquil mountain stream, soft breeze, and pine rustle"
            ),
            AmbientSoundTrack(
                id = "ambient_lofi",
                name = "Lo-Fi Evening Rhodes",
                emoji = "🎹",
                category = "Musical",
                presetType = AmbientPresetType.LOFI,
                description = "Warm electric piano chords, 60 BPM alpha wave rhythm"
            ),
            AmbientSoundTrack(
                id = "ambient_drone",
                name = "432Hz Meditative Drone",
                emoji = "🌌",
                category = "Binaural",
                presetType = AmbientPresetType.DRONE,
                description = "Deep harmonic frequency for unbroken cognitive immersion"
            )
        )
    }
}
