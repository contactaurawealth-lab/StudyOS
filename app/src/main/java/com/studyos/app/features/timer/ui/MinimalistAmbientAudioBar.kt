package com.studyos.app.features.timer.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studyos.app.core.audio.AmbientAudioManager
import com.studyos.app.core.audio.AmbientPresetType
import com.studyos.app.core.audio.AmbientSoundTrack

private val WarmAmberLatte = Color(0xFFD4A373)
private val MatteSlate = Color(0xFF161920)
private val FrostedSmoke = Color(0xFF1E222D)
private val SoftChamomile = Color(0xFFE9D8A6)

@Composable
fun MinimalistAmbientAudioBar(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isPlaying by AmbientAudioManager.isPlaying.collectAsState()
    val currentTrack by AmbientAudioManager.currentTrack.collectAsState()
    val volume by AmbientAudioManager.volume.collectAsState()
    val customTracks by AmbientAudioManager.customTracks.collectAsState()

    var trackMenuExpanded by remember { mutableStateOf(false) }
    var showVolumeSlider by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                // Ignore if not persistable
            }
            val fileName = uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':') ?: "Custom Audio"
            val track = AmbientSoundTrack(
                id = "custom_${System.currentTimeMillis()}",
                name = fileName,
                emoji = "🎵",
                category = "Custom",
                isBuiltIn = false,
                fileUri = uri.toString(),
                presetType = AmbientPresetType.CUSTOM,
                description = "User imported audio track"
            )
            AmbientAudioManager.addCustomTrack(track)
            AmbientAudioManager.playTrack(context, track)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MatteSlate.copy(alpha = 0.90f))
            .border(1.dp, WarmAmberLatte.copy(alpha = 0.20f), RoundedCornerShape(24.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Track selector chip
            Box {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(FrostedSmoke)
                        .border(1.dp, WarmAmberLatte.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                        .clickable { trackMenuExpanded = true }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = currentTrack?.emoji ?: "☕", fontSize = 15.sp)
                    Text(
                        text = currentTrack?.name ?: "Cozy Corner Cafe",
                        color = SoftChamomile,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(text = "▾", color = WarmAmberLatte, fontSize = 12.sp)
                }

                DropdownMenu(
                    expanded = trackMenuExpanded,
                    onDismissRequest = { trackMenuExpanded = false },
                    modifier = Modifier.background(MatteSlate)
                ) {
                    Text(
                        text = "AMBIENT SOUNDSCAPES",
                        color = WarmAmberLatte,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )

                    AmbientSoundTrack.BUILT_IN_TRACKS.forEach { track ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(text = track.emoji, fontSize = 15.sp)
                                    Column {
                                        Text(text = track.name, color = Color.White, fontSize = 13.sp)
                                        Text(text = track.description, color = Color(0xFF9BA3AF), fontSize = 10.sp)
                                    }
                                }
                            },
                            onClick = {
                                trackMenuExpanded = false
                                AmbientAudioManager.playTrack(context, track)
                            }
                        )
                    }

                    if (customTracks.isNotEmpty()) {
                        Text(
                            text = "CUSTOM AUDIO",
                            color = WarmAmberLatte,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                        customTracks.forEach { track ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(text = track.emoji, fontSize = 15.sp)
                                        Text(text = track.name, color = Color.White, fontSize = 13.sp)
                                    }
                                },
                                onClick = {
                                    trackMenuExpanded = false
                                    AmbientAudioManager.playTrack(context, track)
                                }
                            )
                        }
                    }

                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Outlined.Add, contentDescription = null, tint = WarmAmberLatte, modifier = Modifier.size(16.dp))
                                Text(text = "Import Audio File...", color = WarmAmberLatte, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        },
                        onClick = {
                            trackMenuExpanded = false
                            filePickerLauncher.launch(arrayOf("audio/*"))
                        }
                    )
                }
            }

            // Right side: Waveform indicator, Volume button, Play/Pause
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Animated 3-bar waveform
                MiniWaveformIndicator(isPlaying = isPlaying)

                // Volume slider toggle
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(FrostedSmoke)
                        .clickable { showVolumeSlider = !showVolumeSlider },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.VolumeUp,
                        contentDescription = "Volume",
                        tint = if (showVolumeSlider) WarmAmberLatte else Color(0xFF9BA3AF),
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Play / Pause Circle Button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isPlaying) WarmAmberLatte else FrostedSmoke)
                        .border(1.dp, WarmAmberLatte.copy(alpha = 0.4f), CircleShape)
                        .clickable { AmbientAudioManager.togglePlayPause(context) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = if (isPlaying) MatteSlate else SoftChamomile,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Expandable Volume Slider Row
        AnimatedVisibility(visible = showVolumeSlider) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, start = 8.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Ambient Volume",
                    color = Color(0xFF9BA3AF),
                    fontSize = 11.sp
                )
                Slider(
                    value = volume,
                    onValueChange = { AmbientAudioManager.setVolume(it) },
                    valueRange = 0f..1f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = WarmAmberLatte,
                        activeTrackColor = WarmAmberLatte,
                        inactiveTrackColor = FrostedSmoke
                    )
                )
                Text(
                    text = "${(volume * 100).toInt()}%",
                    color = SoftChamomile,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun MiniWaveformIndicator(isPlaying: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "MiniWaveform")
    val h1 by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = 14f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "h1"
    )
    val h2 by infiniteTransition.animateFloat(
        initialValue = 12f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "h2"
    )
    val h3 by infiniteTransition.animateFloat(
        initialValue = 6f,
        targetValue = 16f,
        animationSpec = infiniteRepeatable(
            animation = tween(350, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "h3"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
        modifier = Modifier.height(18.dp)
    ) {
        WaveformBar(height = if (isPlaying) h1.dp else 4.dp, isPlaying = isPlaying)
        WaveformBar(height = if (isPlaying) h2.dp else 6.dp, isPlaying = isPlaying)
        WaveformBar(height = if (isPlaying) h3.dp else 4.dp, isPlaying = isPlaying)
    }
}

@Composable
private fun WaveformBar(height: androidx.compose.ui.unit.Dp, isPlaying: Boolean) {
    Box(
        modifier = Modifier
            .width(3.dp)
            .height(height)
            .clip(RoundedCornerShape(1.5.dp))
            .background(if (isPlaying) WarmAmberLatte else Color(0xFF5E6573))
    )
}
