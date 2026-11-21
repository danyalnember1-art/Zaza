package com.cyrus.emulator.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.cyrus.emulator.core.ui.components.CyrusActionButton
import com.cyrus.emulator.core.ui.components.GlassPanel
import com.cyrus.emulator.core.ui.theme.CyrusColors
import com.cyrus.emulator.savemanager.SaveFileEntry
import com.cyrus.emulator.savemanager.SaveRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SaveManagerScreen(
    accentColor: Color,
    saveRepository: SaveRepository
) {
    val scope = rememberCoroutineScope()
    var saves by remember { mutableStateOf<List<SaveFileEntry>>(emptyList()) }
    var statusMsg by remember { mutableStateOf<String?>(null) }
    var pendingDelete by remember { mutableStateOf<SaveFileEntry?>(null) }

    fun reload() {
        scope.launch { saves = saveRepository.listAllSaves() }
    }

    LaunchedEffect(Unit) { reload() }

    val exportBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) {
            scope.launch {
                val count = saveRepository.exportBackupZip(uri)
                statusMsg = "Exported $count save file(s) to ZIP backup."
            }
        }
    }

    val importBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                val count = saveRepository.importBackupZip(uri)
                reload()
                statusMsg = "Restored $count save file(s) from backup."
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("SAVE MANAGER", color = CyrusColors.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CyrusActionButton(
                    highlighted = true,
                    accentColor = accentColor,
                    onClick = { exportBackup.launch("cyrus_saves_backup.zip") }
                ) {
                    Text("BACKUP ZIP", color = Color(0xFF061004), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                CyrusActionButton(onClick = { importBackup.launch(arrayOf("application/zip", "*/*")) }) {
                    Text("RESTORE ZIP", color = CyrusColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        statusMsg?.let {
            GlassPanel(modifier = Modifier.fillMaxWidth(), accentColor = accentColor) {
                Text(it, color = accentColor, fontSize = 12.sp)
            }
        }

        if (saves.isEmpty()) {
            GlassPanel(modifier = Modifier.fillMaxWidth(), accentColor = accentColor) {
                Text("No Save States, Save Data, or Memory Cards stored yet.", color = CyrusColors.TextSecondary, fontSize = 13.sp)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                items(saves, key = { it.absolutePath }) { entry ->
                    GlassPanel(modifier = Modifier.fillMaxWidth(), accentColor = accentColor) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(entry.fileName, color = CyrusColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                val timeStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(entry.modifiedAt))
                                Text("${entry.category.label} • ${formatBytes(entry.sizeBytes)} • $timeStr", color = CyrusColors.TextSecondary, fontSize = 11.sp)
                            }
                            IconButton(onClick = { pendingDelete = entry }) {
                                Icon(Icons.Rounded.Delete, "Delete Save", tint = CyrusColors.Danger)
                            }
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            containerColor = CyrusColors.Panel,
            title = { Text("Delete Save File?", color = CyrusColors.TextPrimary) },
            text = { Text("Delete '${target.fileName}' permanently?", color = CyrusColors.TextSecondary) },
            confirmButton = {
                CyrusActionButton(onClick = {
                    scope.launch {
                        saveRepository.deleteSave(target.absolutePath)
                        pendingDelete = null
                        reload()
                    }
                }) {
                    Text("DELETE", color = CyrusColors.Danger, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                CyrusActionButton(onClick = { pendingDelete = null }) {
                    Text("Cancel", color = CyrusColors.TextPrimary)
                }
            }
        )
    }
}
