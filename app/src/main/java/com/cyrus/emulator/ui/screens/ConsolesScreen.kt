package com.cyrus.emulator.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.cyrus.emulator.bios.BiosRepository
import com.cyrus.emulator.bios.BiosStatusItem
import com.cyrus.emulator.core.model.ConsoleType
import com.cyrus.emulator.core.model.CyrusResult
import com.cyrus.emulator.core.ui.components.CyrusActionButton
import com.cyrus.emulator.core.ui.components.GlassPanel
import com.cyrus.emulator.core.ui.theme.CyrusColors
import com.cyrus.emulator.engines.EngineCatalog
import kotlinx.coroutines.launch

@Composable
fun ConsolesScreen(
    accentColor: Color,
    biosStatuses: List<BiosStatusItem>,
    biosRepository: BiosRepository,
    onBrowseConsoleGames: (ConsoleType) -> Unit
) {
    val reports = remember { EngineCatalog.getTechnicalReports() }
    val scope = rememberCoroutineScope()
    var pendingBiosConsole by remember { mutableStateOf<ConsoleType?>(null) }
    var biosMessage by remember { mutableStateOf<String?>(null) }

    val biosPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val console = pendingBiosConsole
        if (uri != null && console != null) {
            scope.launch {
                when (val res = biosRepository.importBios(console, uri)) {
                    is CyrusResult.Success -> biosMessage = "Verified BIOS imported for ${console.shortName} (SHA1: ${res.data.sha1?.take(10)}...)"
                    is CyrusResult.Error -> biosMessage = res.message
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("CONSOLES, ENGINES & BIOS MANAGER", color = CyrusColors.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)

        biosMessage?.let { msg ->
            GlassPanel(modifier = Modifier.fillMaxWidth(), accentColor = accentColor) {
                Text(msg, color = accentColor, fontSize = 12.sp)
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
            items(reports) { r ->
                val bios = biosStatuses.firstOrNull { it.spec.console == r.console }
                GlassPanel(modifier = Modifier.fillMaxWidth(), accentColor = accentColor) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("${r.console.displayName} — ${r.engineName}", color = CyrusColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("${r.repositoryUrl} • ${r.language} • License: ${r.license}", color = CyrusColors.TextSecondary, fontSize = 11.sp)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (bios != null) {
                                    CyrusActionButton(
                                        onClick = {
                                            pendingBiosConsole = r.console
                                            biosPicker.launch(arrayOf("*/*"))
                                        }
                                    ) {
                                        Text(
                                            if (bios.installed) "BIOS OK" else "Select BIOS",
                                            color = if (bios.installed) CyrusColors.NeonGreen else CyrusColors.Warning,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                CyrusActionButton(
                                    highlighted = true,
                                    accentColor = accentColor,
                                    onClick = { onBrowseConsoleGames(r.console) }
                                ) {
                                    Text("View Games", color = Color(0xFF061004), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Text("Formats: ${r.supportedFormats.joinToString(", ")} • ABI: ${r.requiredAbis.joinToString()} • Method: ${r.integrationMethod}", color = CyrusColors.TextSecondary, fontSize = 11.sp)
                        Text("BIOS: ${r.biosRequirement}", color = CyrusColors.TextMuted, fontSize = 11.sp)
                        Text(
                            text = "Native Library (${r.nativeLibraryName}): ${r.statusNote}",
                            color = if (r.isNativeLibraryPresent) CyrusColors.NeonGreen else CyrusColors.Warning,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (r.licenseNotes.isNotBlank()) {
                            Text("License Note: ${r.licenseNotes}", color = CyrusColors.TextMuted, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}
