package com.cyrus.emulator.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyrus.emulator.audio.CyrusAudioEngine
import com.cyrus.emulator.audio.MusicUiState
import com.cyrus.emulator.core.ui.components.CyrusActionButton
import com.cyrus.emulator.core.ui.components.GlassPanel
import com.cyrus.emulator.core.ui.theme.CyrusColors

@Composable
fun MusicScreen(
    accentColor: Color,
    musicState: MusicUiState,
    onTogglePlay: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onSelectTrack: (Int) -> Unit,
    onToggleShuffle: (Boolean) -> Unit,
    onToggleMute: (Boolean) -> Unit,
    onMusicVolume: (Float) -> Unit,
    onSfxVolume: (Float) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("MUSIC & SOUND SYSTEM", color = CyrusColors.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)

        Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GlassPanel(modifier = Modifier.weight(1f), accentColor = accentColor) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("NOW PLAYING", color = accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(musicState.currentTrack.title, color = CyrusColors.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    Text("${musicState.currentTrack.subtitle} • ${musicState.currentTrack.bpm} BPM", color = CyrusColors.TextSecondary, fontSize = 12.sp)

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onPrev) { Icon(Icons.Rounded.SkipPrevious, null, tint = CyrusColors.TextPrimary) }
                        CyrusActionButton(highlighted = true, accentColor = accentColor, onClick = onTogglePlay) {
                            Icon(if (musicState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = Color(0xFF061004))
                        }
                        IconButton(onClick = onNext) { Icon(Icons.Rounded.SkipNext, null, tint = CyrusColors.TextPrimary) }
                        IconButton(onClick = { onToggleShuffle(!musicState.shuffleEnabled) }) {
                            Icon(Icons.Rounded.Shuffle, null, tint = if (musicState.shuffleEnabled) accentColor else CyrusColors.TextMuted)
                        }
                        IconButton(onClick = { onToggleMute(!musicState.isMuted) }) {
                            Icon(if (musicState.isMuted) Icons.Rounded.VolumeOff else Icons.Rounded.VolumeUp, null, tint = if (musicState.isMuted) CyrusColors.Danger else CyrusColors.TextPrimary)
                        }
                    }

                    Text("BGM Volume: ${(musicState.volume * 100).toInt()}%", color = CyrusColors.TextSecondary, fontSize = 12.sp)
                    Slider(
                        value = musicState.volume,
                        onValueChange = onMusicVolume,
                        colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor)
                    )

                    Text("UI Sound Effects Volume: ${(musicState.sfxVolume * 100).toInt()}%", color = CyrusColors.TextSecondary, fontSize = 12.sp)
                    Slider(
                        value = musicState.sfxVolume,
                        onValueChange = onSfxVolume,
                        colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor)
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Mute All Music", color = CyrusColors.TextPrimary, fontSize = 13.sp)
                        Switch(
                            checked = musicState.isMuted,
                            onCheckedChange = onToggleMute,
                            colors = SwitchDefaults.colors(checkedThumbColor = accentColor)
                        )
                    }
                }
            }

            GlassPanel(modifier = Modifier.weight(1f), accentColor = accentColor) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("CYRUS ORIGINAL SOUNDTRACKS", color = accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(CyrusAudioEngine.TRACKS) { track ->
                            val active = track.id == musicState.currentTrack.id
                            GlassPanel(
                                modifier = Modifier.fillMaxWidth(),
                                selected = active,
                                accentColor = accentColor,
                                onClick = { onSelectTrack(track.id) }
                            ) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.MusicNote, null, tint = if (active) accentColor else CyrusColors.TextMuted)
                                        Spacer(Modifier.width(10.dp))
                                        Column {
                                            Text(track.title, color = CyrusColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text(track.subtitle, color = CyrusColors.TextSecondary, fontSize = 11.sp)
                                        }
                                    }
                                    Text("${track.bpm} BPM", color = if (active) accentColor else CyrusColors.TextMuted, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
