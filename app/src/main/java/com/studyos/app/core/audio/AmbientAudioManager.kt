package com.studyos.app.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Random
import kotlin.math.PI
import kotlin.math.sin

/**
 * Integrated Ambient Soundscape Engine for StudyOS.
 * Features procedural sound synthesis via AudioTrack (zero asset overhead, infinite loop)
 * and MediaPlayer for custom user-imported ambient audio.
 */
object AmbientAudioManager {

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentTrack = MutableStateFlow<AmbientSoundTrack?>(AmbientSoundTrack.BUILT_IN_TRACKS.first())
    val currentTrack: StateFlow<AmbientSoundTrack?> = _currentTrack.asStateFlow()

    private val _volume = MutableStateFlow(0.65f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private val _customTracks = MutableStateFlow<List<AmbientSoundTrack>>(emptyList())
    val customTracks: StateFlow<List<AmbientSoundTrack>> = _customTracks.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private var proceduralAudioTrack: AudioTrack? = null
    private var proceduralJob: Job? = null
    private var isDucked = false

    private val scope = CoroutineScope(Dispatchers.Default)

    fun initialize(context: Context) {
        // Ready for playback
    }

    fun playTrack(context: Context, track: AmbientSoundTrack) {
        stopCurrentAudio()
        _currentTrack.value = track

        if (track.isBuiltIn) {
            startProceduralSynthesis(track.presetType)
        } else if (track.fileUri != null) {
            startMediaPlayer(context, Uri.parse(track.fileUri))
        }
        _isPlaying.value = true
    }

    fun togglePlayPause(context: Context) {
        if (_isPlaying.value) {
            pause()
        } else {
            _currentTrack.value?.let { playTrack(context, it) }
        }
    }

    fun pause() {
        stopCurrentAudio()
        _isPlaying.value = false
    }

    fun setVolume(vol: Float) {
        val clamped = vol.coerceIn(0.0f, 1.0f)
        _volume.value = clamped
        applyVolume(clamped)
    }

    fun duckAudio() {
        isDucked = true
        applyVolume(_volume.value * 0.25f)
    }

    fun unduckAudio() {
        isDucked = false
        applyVolume(_volume.value)
    }

    fun addCustomTrack(track: AmbientSoundTrack) {
        val current = _customTracks.value.toMutableList()
        current.removeAll { it.id == track.id }
        current.add(track)
        _customTracks.value = current
    }

    fun removeCustomTrack(trackId: String) {
        _customTracks.value = _customTracks.value.filter { it.id != trackId }
        if (_currentTrack.value?.id == trackId) {
            pause()
            _currentTrack.value = AmbientSoundTrack.BUILT_IN_TRACKS.first()
        }
    }

    private fun applyVolume(effectiveVol: Float) {
        try {
            mediaPlayer?.setVolume(effectiveVol, effectiveVol)
            proceduralAudioTrack?.setVolume(effectiveVol)
        } catch (e: Exception) {
            // Safe volume trap
        }
    }

    private fun stopCurrentAudio() {
        proceduralJob?.cancel()
        proceduralJob = null

        try {
            proceduralAudioTrack?.pause()
            proceduralAudioTrack?.flush()
            proceduralAudioTrack?.release()
        } catch (e: Exception) {
            // Ignored
        } finally {
            proceduralAudioTrack = null
        }

        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            // Ignored
        } finally {
            mediaPlayer = null
        }
    }

    private fun startMediaPlayer(context: Context, uri: Uri) {
        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(context, uri)
                isLooping = true
                prepare()
                val vol = if (isDucked) _volume.value * 0.25f else _volume.value
                setVolume(vol, vol)
                start()
            }
        } catch (e: Exception) {
            android.util.Log.e("AmbientAudioManager", "Failed to start MediaPlayer for $uri", e)
            _isPlaying.value = false
        }
    }

    /**
     * Continuous procedural audio synthesis using AudioTrack.
     * Generates rich acoustic noise textures (rain brown noise, cafe murmur, 432Hz sine drone)
     * without requiring multi-megabyte sound files.
     */
    private fun startProceduralSynthesis(preset: AmbientPresetType) {
        val sampleRate = 22050
        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(sampleRate / 4)

        proceduralAudioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        val vol = if (isDucked) _volume.value * 0.25f else _volume.value
        proceduralAudioTrack?.setVolume(vol)
        proceduralAudioTrack?.play()

        proceduralJob = scope.launch(Dispatchers.Default) {
            val shortBuffer = ShortArray(bufferSize / 2)
            val random = Random()
            var phase = 0.0
            var brownVal = 0.0
            var cafeUndertone = 0.0

            while (_isPlaying.value && proceduralAudioTrack != null) {
                for (i in shortBuffer.indices) {
                    when (preset) {
                        AmbientPresetType.RAIN -> {
                            // Brown noise integration for soft rainfall
                            val white = random.nextDouble() * 2.0 - 1.0
                            brownVal = (brownVal + (0.02 * white)) / 1.02
                            shortBuffer[i] = (brownVal * 16000).toInt().coerceIn(-32767, 32767).toShort()
                        }
                        AmbientPresetType.DRONE -> {
                            // 432Hz deep meditative fundamental with 864Hz octave harmonic
                            val freq1 = 432.0
                            val freq2 = 864.0
                            val sample = 0.7 * sin(phase * freq1 * 2 * PI / sampleRate) +
                                    0.3 * sin(phase * freq2 * 2 * PI / sampleRate)
                            phase += 1.0
                            if (phase >= sampleRate) phase = 0.0
                            shortBuffer[i] = (sample * 14000).toInt().coerceIn(-32767, 32767).toShort()
                        }
                        AmbientPresetType.CAFE -> {
                            // Warm filtered acoustic murmur with gentle micro-cadence
                            val white = random.nextDouble() * 2.0 - 1.0
                            cafeUndertone = (cafeUndertone * 0.94) + (white * 0.06)
                            val clink = if (random.nextInt(sampleRate * 2) == 0) 0.4 * sin(phase * 1200 * 2 * PI / sampleRate) else 0.0
                            val sample = cafeUndertone * 0.8 + clink
                            phase += 1.0
                            shortBuffer[i] = (sample * 15000).toInt().coerceIn(-32767, 32767).toShort()
                        }
                        AmbientPresetType.FOREST -> {
                            // Gentle mountain creek oscillation
                            val stream = random.nextDouble() * 2.0 - 1.0
                            brownVal = (brownVal * 0.88) + (stream * 0.12)
                            val waterPulse = sin(phase * 0.3 * 2 * PI / sampleRate) * 0.2 + 0.8
                            phase += 1.0
                            shortBuffer[i] = (brownVal * waterPulse * 15000).toInt().coerceIn(-32767, 32767).toShort()
                        }
                        AmbientPresetType.LOFI -> {
                            // 60 BPM warm Rhodes electric chords (Fmaj7 / Cmaj7 alpha harmony)
                            val beatTime = (phase / sampleRate) % 4.0
                            val chordFreq = if (beatTime < 2.0) 261.63 else 349.23 // C4 / F4
                            val rhodes = 0.6 * sin(phase * chordFreq * 2 * PI / sampleRate) +
                                    0.3 * sin(phase * chordFreq * 1.5 * 2 * PI / sampleRate)
                            phase += 1.0
                            shortBuffer[i] = (rhodes * 12000).toInt().coerceIn(-32767, 32767).toShort()
                        }
                        else -> {
                            // Fallback gentle pink texture
                            val white = random.nextDouble() * 2.0 - 1.0
                            brownVal = (brownVal * 0.9) + (white * 0.1)
                            shortBuffer[i] = (brownVal * 14000).toInt().coerceIn(-32767, 32767).toShort()
                        }
                    }
                }
                proceduralAudioTrack?.write(shortBuffer, 0, shortBuffer.size)
            }
        }
    }
}
