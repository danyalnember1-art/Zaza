package com.cyrus.emulator.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyrus.emulator.core.model.ScannedRomCandidate
import com.cyrus.emulator.core.ui.components.CyrusActionButton
import com.cyrus.emulator.core.ui.components.GlassPanel
import com.cyrus.emulator.core.ui.theme.CyrusColors
import com.cyrus.emulator.library.GameLibraryRepository
import kotlinx.coroutines.launch

@Composable
fun AddGameScreen(
    accentColor: Color,
    libraryRepository: GameLibraryRepository,
    onAddedSfx: () -> Unit,
    onErrorSfx: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var singleCandidate by remember { mutableStateOf<ScannedRomCandidate?>(null) }
    var folderCandidates by remember { mutableStateOf<Pair<Uri, Pair<String, List<ScannedRomCandidate>>>?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                val inspected = libraryRepository.inspectFile(uri)
                singleCandidate = inspected
                folderCandidates = null
                if (!inspected.isSupported) onErrorSfx()
            }
        }
    }

    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { treeUri ->
        if (treeUri != null) {
            scope.launch {
                val scanned = libraryRepository.scanFolderCandidates(treeUri)
                folderCandidates = treeUri to scanned
                singleCandidate = null
                statusMessage = "Found ${scanned.second.size} supported ROM(s) in '${scanned.first}'."
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("ADD GAME", color = CyrusColors.TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        Text(
            "Select ROM files or a folder via Android Storage Access Framework. Original files remain untouched in your storage.",
            color = CyrusColors.TextSecondary,
            fontSize = 12.sp
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GlassPanel(
                modifier = Modifier.weight(1f),
                accentColor = accentColor,
                onClick = { filePicker.launch(arrayOf("*/*")) }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.InsertDriveFile, null, tint = accentColor, modifier = Modifier.size(30.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("SELECT GAME FILE", color = CyrusColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Inspect format, console, engine & confirm", color = CyrusColors.TextSecondary, fontSize = 11.sp)
                    }
                }
            }

            GlassPanel(
                modifier = Modifier.weight(1f),
                accentColor = accentColor,
                onClick = { folderPicker.launch(null) }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CreateNewFolder, null, tint = accentColor, modifier = Modifier.size(30.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("SELECT GAME FOLDER", color = CyrusColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Batch scan directory & persist folder access", color = CyrusColors.TextSecondary, fontSize = 11.sp)
                    }
                }
            }

            CyrusActionButton(
                onClick = {
                    scope.launch {
                        val (added, missing) = libraryRepository.rescanAllAndVerifyFiles()
                        statusMessage = "Rescan complete: $added new game(s) added, $missing missing file(s)."
                        onAddedSfx()
                    }
                }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Refresh, null, tint = accentColor, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("RESCAN", color = CyrusColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        statusMessage?.let { msg ->
            GlassPanel(modifier = Modifier.fillMaxWidth(), accentColor = accentColor) {
                Text(msg, color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }

        singleCandidate?.let { c ->
            GlassPanel(modifier = Modifier.fillMaxWidth(), selected = true, accentColor = accentColor) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("DETECTED ROM PREVIEW", color = accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("Title: ${c.title}", color = CyrusColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("File: ${c.fileName} (${formatBytes(c.sizeBytes)}) • Format: .${c.extension.uppercase()} • CRC32: ${c.crc32}", color = CyrusColors.TextSecondary, fontSize = 12.sp)
                    Text(
                        text = if (c.isSupported) "Console: ${c.detectedConsole?.displayName}" else "Error: ${c.reason}",
                        color = if (c.isSupported) CyrusColors.NeonGreen else CyrusColors.Danger,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (c.isSupported) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            CyrusActionButton(
                                highlighted = true,
                                accentColor = accentColor,
                                onClick = {
                                    scope.launch {
                                        val ok = libraryRepository.addCandidate(c)
                                        if (ok) {
                                            onAddedSfx()
                                            statusMessage = "Added '${c.title}' to library."
                                            singleCandidate = null
                                        } else {
                                            onErrorSfx()
                                            statusMessage = "Game already exists in library (duplicate skipped)."
                                        }
                                    }
                                }
                            ) {
                                Text("CONFIRM & ADD TO LIBRARY", color = Color(0xFF061004), fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                            }
                            CyrusActionButton(onClick = { singleCandidate = null }) {
                                Text("CANCEL", color = CyrusColors.TextPrimary, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        folderCandidates?.let { (treeUri, pair) ->
            val (folderName, list) = pair
            GlassPanel(modifier = Modifier.fillMaxWidth().weight(1f), accentColor = accentColor) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Folder: $folderName (${list.size} games)", color = CyrusColors.TextPrimary, fontWeight = FontWeight.Bold)
                        if (list.isNotEmpty()) {
                            CyrusActionButton(
                                highlighted = true,
                                accentColor = accentColor,
                                onClick = {
                                    scope.launch {
                                        val count = libraryRepository.addFolderAndGames(treeUri, folderName, list)
                                        onAddedSfx()
                                        statusMessage = "Added $count new game(s) from '$folderName'."
                                        folderCandidates = null
                                    }
                                }
                            ) {
                                Text("IMPORT ALL (${list.size})", color = Color(0xFF061004), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(list) { rom ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(rom.title, color = CyrusColors.TextPrimary, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                Text("${rom.detectedConsole?.shortName} • ${formatBytes(rom.sizeBytes)}", color = CyrusColors.TextSecondary, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return when {
        mb >= 1.0 -> String.format("%.1f MB", mb)
        kb >= 1.0 -> String.format("%.0f KB", kb)
        else -> "$bytes B"
    }
}
