package com.cyrus.emulator.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyrus.emulator.core.model.ConsoleType
import com.cyrus.emulator.core.model.GameItem
import com.cyrus.emulator.core.ui.components.CyrusActionButton
import com.cyrus.emulator.core.ui.components.GlassPanel
import com.cyrus.emulator.core.ui.theme.CyrusColors
import com.cyrus.emulator.library.LibrarySortMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LibraryScreen(
    title: String,
    accentColor: Color,
    games: List<GameItem>,
    initialFilterConsole: ConsoleType? = null,
    favoritesOnly: Boolean = false,
    onPlayGame: (GameItem) -> Unit,
    onToggleFavorite: (GameItem) -> Unit,
    onRemoveGame: (GameItem) -> Unit
) {
    var search by remember { mutableStateOf("") }
    var isGrid by remember { mutableStateOf(true) }
    var selectedConsole by remember(initialFilterConsole) { mutableStateOf(initialFilterConsole) }
    var sortMode by remember { mutableStateOf(LibrarySortMode.TITLE_ASC) }
    var detailGame by remember { mutableStateOf<GameItem?>(null) }

    val filtered = remember(games, search, selectedConsole, sortMode, favoritesOnly) {
        games.asSequence()
            .filter { if (favoritesOnly) it.isFavorite else true }
            .filter { selectedConsole == null || it.console == selectedConsole }
            .filter { search.isBlank() || it.title.contains(search, ignoreCase = true) }
            .let { seq ->
                when (sortMode) {
                    LibrarySortMode.TITLE_ASC -> seq.sortedBy { it.title.lowercase() }
                    LibrarySortMode.LAST_PLAYED -> seq.sortedByDescending { it.lastPlayedAt ?: 0L }
                    LibrarySortMode.SIZE_DESC -> seq.sortedByDescending { it.fileSizeBytes }
                }
            }
            .toList()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, color = CyrusColors.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text("Search games...", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.width(220.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = CyrusColors.BorderSubtle,
                        focusedTextColor = CyrusColors.TextPrimary,
                        unfocusedTextColor = CyrusColors.TextPrimary
                    )
                )

                CyrusActionButton(
                    onClick = {
                        val all = LibrarySortMode.entries
                        sortMode = all[(sortMode.ordinal + 1) % all.size]
                    }
                ) {
                    Text(sortMode.label, color = CyrusColors.TextPrimary, fontSize = 11.sp)
                }

                IconButton(onClick = { isGrid = !isGrid }) {
                    Icon(
                        imageVector = if (isGrid) Icons.Rounded.ViewList else Icons.Rounded.GridView,
                        contentDescription = "Toggle View",
                        tint = accentColor
                    )
                }
            }
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                CyrusActionButton(
                    highlighted = selectedConsole == null,
                    accentColor = accentColor,
                    onClick = { selectedConsole = null }
                ) {
                    Text(
                        "ALL",
                        color = if (selectedConsole == null) Color(0xFF061004) else CyrusColors.TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            items(ConsoleType.entries) { c ->
                val sel = selectedConsole == c
                CyrusActionButton(
                    highlighted = sel,
                    accentColor = accentColor,
                    onClick = { selectedConsole = if (sel) null else c }
                ) {
                    Text(
                        c.shortName,
                        color = if (sel) Color(0xFF061004) else CyrusColors.TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            GlassPanel(modifier = Modifier.fillMaxWidth(), accentColor = accentColor) {
                Text(
                    if (favoritesOnly) "No favorite games yet. Mark games from your library."
                    else "No games found. Use ADD GAME to select ROM files from storage.",
                    color = CyrusColors.TextSecondary,
                    fontSize = 13.sp
                )
            }
        } else if (isGrid) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 210.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filtered, key = { it.id }) { game ->
                    GameCard(
                        game = game,
                        accentColor = accentColor,
                        onClick = { detailGame = game },
                        onPlay = { onPlayGame(game) },
                        onFav = { onToggleFavorite(game) }
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filtered, key = { it.id }) { game ->
                    GameRow(
                        game = game,
                        accentColor = accentColor,
                        onClick = { detailGame = game },
                        onPlay = { onPlayGame(game) },
                        onFav = { onToggleFavorite(game) }
                    )
                }
            }
        }
    }

    detailGame?.let { game ->
        AlertDialog(
            onDismissRequest = { detailGame = null },
            containerColor = CyrusColors.Panel,
            title = { Text(game.title, color = CyrusColors.TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Console: ${game.console.displayName}", color = accentColor, fontSize = 13.sp)
                    Text("File: ${game.fileName}", color = CyrusColors.TextSecondary, fontSize = 12.sp)
                    Text("Size: ${formatBytes(game.fileSizeBytes)} • CRC32: ${game.crc32}", color = CyrusColors.TextSecondary, fontSize = 12.sp)
                    Text("Status: ${if (game.isMissing) "Missing File" else "Available"}", color = if (game.isMissing) CyrusColors.Danger else CyrusColors.NeonGreen, fontSize = 12.sp)
                    Text("Play Count: ${game.playCount} • Total Time: ${game.totalPlayTimeSeconds / 60} min", color = CyrusColors.TextSecondary, fontSize = 12.sp)
                }
            },
            confirmButton = {
                CyrusActionButton(
                    highlighted = !game.isMissing,
                    accentColor = accentColor,
                    onClick = {
                        detailGame = null
                        if (!game.isMissing) onPlayGame(game)
                    }
                ) {
                    Text("PLAY", color = Color(0xFF061004), fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CyrusActionButton(
                        onClick = {
                            onRemoveGame(game)
                            detailGame = null
                        }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Delete, null, tint = CyrusColors.Danger, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Remove", color = CyrusColors.Danger, fontSize = 12.sp)
                        }
                    }
                    CyrusActionButton(onClick = { detailGame = null }) {
                        Text("Close", color = CyrusColors.TextPrimary, fontSize = 12.sp)
                    }
                }
            }
        )
    }
}

@Composable
private fun GameCard(
    game: GameItem,
    accentColor: Color,
    onClick: () -> Unit,
    onPlay: () -> Unit,
    onFav: () -> Unit
) {
    GlassPanel(modifier = Modifier.height(148.dp), accentColor = accentColor, onClick = onClick) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(game.console.shortName, color = Color(game.console.accentHex), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onFav, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = if (game.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (game.isFavorite) CyrusColors.NeonGreen else CyrusColors.TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Text(game.title, color = CyrusColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(formatBytes(game.fileSizeBytes), color = CyrusColors.TextMuted, fontSize = 10.sp)
                    Text(
                        if (game.isMissing) "Missing File" else formatDate(game.lastPlayedAt),
                        color = if (game.isMissing) CyrusColors.Danger else CyrusColors.TextSecondary,
                        fontSize = 10.sp
                    )
                }
                CyrusActionButton(highlighted = !game.isMissing, accentColor = accentColor, onClick = onPlay) {
                    Icon(Icons.Rounded.PlayArrow, "Play", tint = Color(0xFF061004), modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun GameRow(
    game: GameItem,
    accentColor: Color,
    onClick: () -> Unit,
    onPlay: () -> Unit,
    onFav: () -> Unit
) {
    GlassPanel(modifier = Modifier.fillMaxWidth(), accentColor = accentColor, onClick = onClick) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text(game.title, color = CyrusColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("${game.console.displayName} • ${formatBytes(game.fileSizeBytes)} • ${if (game.isMissing) "Missing File" else formatDate(game.lastPlayedAt)}", color = CyrusColors.TextSecondary, fontSize = 11.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onFav) {
                    Icon(
                        if (game.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        null,
                        tint = if (game.isFavorite) accentColor else CyrusColors.TextMuted
                    )
                }
                CyrusActionButton(highlighted = !game.isMissing, accentColor = accentColor, onClick = onPlay) {
                    Text("PLAY", color = Color(0xFF061004), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }
    }
}

private fun formatDate(ts: Long?): String {
    if (ts == null) return "Never played"
    return SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(ts))
}
