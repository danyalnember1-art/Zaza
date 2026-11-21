package com.cyrus.emulator.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Gamepad
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyrus.emulator.audio.MusicUiState
import com.cyrus.emulator.controller.ConnectedControllerInfo
import com.cyrus.emulator.core.model.ConsoleType
import com.cyrus.emulator.core.model.GameItem
import com.cyrus.emulator.core.model.LibraryStats
import com.cyrus.emulator.core.ui.components.CyrusActionButton
import com.cyrus.emulator.core.ui.components.CyrusBrandHeader
import com.cyrus.emulator.core.ui.components.GlassPanel
import com.cyrus.emulator.core.ui.theme.CyrusColors
import com.cyrus.emulator.ui.CyrusRoute
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeDashboardScreen(
    accentColor: Color,
    games: List<GameItem>,
    stats: LibraryStats,
    musicState: MusicUiState,
    connectedControllers: List<ConnectedControllerInfo>,
    externalDisplayCount: Int,
    selectedConsoleIndex: Int,
    onSelectConsole: (Int) -> Unit,
    onOpenConsole: (ConsoleType) -> Unit,
    onPlayGame: (GameItem) -> Unit,
    onNavigate: (CyrusRoute) -> Unit,
    onToggleMusic: () -> Unit,
    onPrevTrack: () -> Unit,
    onNextTrack: () -> Unit,
    onMusicVolume: (Float) -> Unit
) {
    var clockText by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
        while (true) {
            clockText = fmt.format(Date())
            delay(10000)
        }
    }

    val lastGame = stats.lastPlayedGame ?: games.firstOrNull()
    val consoles = ConsoleType.entries
    val recentlyPlayed = remember(games) {
        games.filter { it.lastPlayedAt != null }.sortedByDescending { it.lastPlayedAt }.take(4)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // TOP STATUS BAR
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            CyrusBrandHeader(accentColor = accentColor)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatusChip(
                    icon = Icons.Rounded.Gamepad,
                    label = if (connectedControllers.isEmpty()) "Touch Only" else "${connectedControllers.size} Pad Connected",
                    highlighted = connectedControllers.isNotEmpty(),
                    accentColor = accentColor,
                    onClick = { onNavigate(CyrusRoute.CONTROLLERS) }
                )
                StatusChip(
                    icon = Icons.Rounded.Cast,
                    label = if (externalDisplayCount > 0) "$externalDisplayCount Display" else "Phone Screen",
                    highlighted = externalDisplayCount > 0,
                    accentColor = accentColor,
                    onClick = { onNavigate(CyrusRoute.EXTERNAL_DISPLAY) }
                )
                StatusChip(
                    icon = Icons.Rounded.MusicNote,
                    label = if (musicState.isPlaying) musicState.currentTrack.title else "BGM Paused",
                    highlighted = musicState.isPlaying,
                    accentColor = accentColor,
                    onClick = { onNavigate(CyrusRoute.MUSIC) }
                )
                Text(
                    text = clockText,
                    color = CyrusColors.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { onNavigate(CyrusRoute.SETTINGS) }) {
                    Icon(Icons.Rounded.Settings, contentDescription = "Settings", tint = CyrusColors.TextPrimary)
                }
            }
        }

        // HERO BANNER + MINI MUSIC PANEL
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(175.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Central Hero Card
            Box(
                modifier = Modifier
                    .weight(1.75f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(18.dp))
                    .background(CyrusColors.Panel)
                    .border(1.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF101914),
                                accentColor.copy(alpha = 0.14f),
                                Color(0xFF0A0E0C)
                            )
                        )
                    )
                    drawCircle(
                        color = accentColor.copy(alpha = 0.10f),
                        radius = size.height * 0.85f,
                        center = Offset(size.width * 0.85f, size.height * 0.35f)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (lastGame != null) "READY TO RESUME" else "CYRUS CONSOLE DECK",
                            color = accentColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = lastGame?.title ?: "YOUR CLASSIC GAMES. ONE EXPERIENCE.",
                            color = CyrusColors.TextPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (lastGame != null) {
                                "${lastGame.console.displayName} • ${formatLastPlayed(lastGame.lastPlayedAt)}"
                            } else {
                                "Add ROM files from your phone storage via Android SAF to launch native engines."
                            },
                            color = CyrusColors.TextSecondary,
                            fontSize = 12.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (lastGame != null) {
                                CyrusActionButton(
                                    highlighted = true,
                                    accentColor = accentColor,
                                    onClick = { onPlayGame(lastGame) }
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.PlayArrow, null, tint = Color(0xFF061004), modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("PLAY", color = Color(0xFF061004), fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                                    }
                                }
                                CyrusActionButton(
                                    highlighted = false,
                                    onClick = { onPlayGame(lastGame) }
                                ) {
                                    Text("CONTINUE", color = CyrusColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            } else {
                                CyrusActionButton(
                                    highlighted = true,
                                    accentColor = accentColor,
                                    onClick = { onNavigate(CyrusRoute.ADD_GAME) }
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.AddCircle, null, tint = Color(0xFF061004), modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("ADD GAME", color = Color(0xFF061004), fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Mini Music Controller Panel
            GlassPanel(
                modifier = Modifier
                    .weight(0.95f)
                    .fillMaxHeight(),
                accentColor = accentColor
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("SYSTEM AUDIO", color = accentColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                            Text(
                                text = musicState.currentTrack.title,
                                color = CyrusColors.TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = musicState.currentTrack.subtitle,
                                color = CyrusColors.TextMuted,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }
                        Icon(Icons.Rounded.MusicNote, null, tint = accentColor)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onPrevTrack) {
                            Icon(Icons.Rounded.SkipPrevious, "Previous", tint = CyrusColors.TextPrimary)
                        }
                        CyrusActionButton(
                            highlighted = true,
                            accentColor = accentColor,
                            onClick = onToggleMusic
                        ) {
                            Icon(
                                imageVector = if (musicState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = Color(0xFF061004),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(onClick = onNextTrack) {
                            Icon(Icons.Rounded.SkipNext, "Next", tint = CyrusColors.TextPrimary)
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.VolumeUp, null, tint = CyrusColors.TextSecondary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Slider(
                            value = musicState.volume,
                            onValueChange = onMusicVolume,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = accentColor,
                                activeTrackColor = accentColor
                            )
                        )
                    }
                }
            }
        }

        // HORIZONTAL CONSOLES CAROUSEL
        Text(
            text = "CONSOLES",
            color = CyrusColors.TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            itemsIndexed(consoles) { idx, console ->
                val selected = idx == selectedConsoleIndex
                val count = games.count { it.console == console }
                val consoleColor = Color(console.accentHex)

                GlassPanel(
                    modifier = Modifier
                        .width(if (selected) 196.dp else 172.dp)
                        .height(112.dp),
                    selected = selected,
                    accentColor = accentColor,
                    onClick = {
                        if (selected) onOpenConsole(console) else onSelectConsole(idx)
                    }
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = console.shortName,
                                color = if (selected) accentColor else consoleColor,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "$count ROMs",
                                color = CyrusColors.TextMuted,
                                fontSize = 11.sp
                            )
                        }
                        Text(
                            text = console.displayName,
                            color = CyrusColors.TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (console.isExperimental) "Experimental • Tap to inspect" else console.extensions.joinToString(", ") { it.uppercase() },
                            color = if (console.isExperimental) CyrusColors.Warning else CyrusColors.TextSecondary,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // BOTTOM 3 PANELS: RECENTLY PLAYED | QUICK ACCESS | REAL GAME STATISTICS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Recently Played
            GlassPanel(modifier = Modifier.weight(1f).height(145.dp), accentColor = accentColor) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Text("RECENTLY PLAYED", color = accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    Spacer(Modifier.height(8.dp))
                    if (recentlyPlayed.isEmpty()) {
                        Text("No games played yet.", color = CyrusColors.TextMuted, fontSize = 12.sp)
                    } else {
                        recentlyPlayed.take(3).forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(item.title, color = CyrusColors.TextPrimary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                Text("${item.playCount}x", color = CyrusColors.TextSecondary, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // 2. Quick Access
            GlassPanel(modifier = Modifier.weight(1f).height(145.dp), accentColor = accentColor) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                    Text("QUICK ACCESS", color = accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CyrusActionButton(onClick = { onNavigate(CyrusRoute.ADD_GAME) }, modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.FolderOpen, null, tint = accentColor, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Add ROM", color = CyrusColors.TextPrimary, fontSize = 11.sp)
                            }
                        }
                        CyrusActionButton(onClick = { onNavigate(CyrusRoute.CONTROLLERS) }, modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Gamepad, null, tint = accentColor, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Controllers", color = CyrusColors.TextPrimary, fontSize = 11.sp)
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CyrusActionButton(onClick = { onNavigate(CyrusRoute.SAVE_MANAGER) }, modifier = Modifier.weight(1f)) {
                            Text("Save Manager", color = CyrusColors.TextPrimary, fontSize = 11.sp)
                        }
                        CyrusActionButton(onClick = { onNavigate(CyrusRoute.EXTERNAL_DISPLAY) }, modifier = Modifier.weight(1f)) {
                            Text("TV / Display", color = CyrusColors.TextPrimary, fontSize = 11.sp)
                        }
                    }
                }
            }

            // 3. Game Statistics (Real DB numbers)
            GlassPanel(modifier = Modifier.weight(1f).height(145.dp), accentColor = accentColor) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                    Text("GAME STATISTICS", color = accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    StatLine("Total Games", "${stats.totalGames}")
                    StatLine("Active Consoles", "${stats.activeConsolesCount}")
                    StatLine("Favorites", "${stats.favoriteCount}")
                    StatLine("Played Titles", "${stats.playedGamesCount}")
                }
            }
        }
    }
}

@Composable
private fun StatLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = CyrusColors.TextSecondary, fontSize = 12.sp)
        Text(value, color = CyrusColors.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StatusChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    highlighted: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    CyrusActionButton(
        highlighted = false,
        accentColor = accentColor,
        onClick = onClick
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = if (highlighted) accentColor else CyrusColors.TextSecondary, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(5.dp))
            Text(label, color = CyrusColors.TextPrimary, fontSize = 11.sp, maxLines = 1)
        }
    }
}

private fun formatLastPlayed(ts: Long?): String {
    if (ts == null) return "Never played"
    return SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(ts))
}
