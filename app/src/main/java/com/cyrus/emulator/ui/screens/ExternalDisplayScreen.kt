package com.cyrus.emulator.ui.screens

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyrus.emulator.api.AspectRatioMode
import com.cyrus.emulator.core.ui.components.CyrusActionButton
import com.cyrus.emulator.core.ui.components.GlassPanel
import com.cyrus.emulator.core.ui.theme.CyrusColors
import com.cyrus.emulator.externaldisplay.ExternalDisplayManager
import com.cyrus.emulator.screencasting.ScreenCastingManager
import com.cyrus.emulator.settings.CyrusSettings
import com.cyrus.emulator.settings.CyrusSettingsRepository

@Composable
fun ExternalDisplayScreen(
    accentColor: Color,
    settings: CyrusSettings,
    settingsRepository: CyrusSettingsRepository,
    externalDisplayManager: ExternalDisplayManager,
    screenCastingManager: ScreenCastingManager
) {
    val context = LocalContext.current
    val displays by externalDisplayManager.availableDisplays.collectAsState()
    val selectedDisplayId by externalDisplayManager.selectedDisplayId.collectAsState()
    val routes by screenCastingManager.routes.collectAsState()
    val hardwareCaps = remember(displays, routes) { screenCastingManager.getHardwareCapabilities() }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("EXTERNAL DISPLAY & SCREEN CASTING", color = CyrusColors.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CyrusActionButton(onClick = {
                    externalDisplayManager.refreshDisplays()
                    screenCastingManager.refreshRoutes()
                }) {
                    Text("Refresh", color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                CyrusActionButton(onClick = { screenCastingManager.openSystemCastSettings() }) {
                    Text("Android Cast Settings", color = CyrusColors.TextPrimary, fontSize = 12.sp)
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // Wired / Presentation Displays
            GlassPanel(modifier = Modifier.weight(1f), accentColor = accentColor) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("WIRED / PRESENTATION DISPLAYS (${displays.size})", color = accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    if (displays.isEmpty()) {
                        Text(
                            "No secondary HDMI/USB-C or wireless presentation display currently connected.",
                            color = CyrusColors.TextSecondary,
                            fontSize = 12.sp
                        )
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                            items(displays) { d ->
                                val active = selectedDisplayId == d.displayId
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(d.name, color = CyrusColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("${d.width}x${d.height} @ ${d.refreshRate.toInt()}Hz", color = CyrusColors.TextSecondary, fontSize = 11.sp)
                                    }
                                    CyrusActionButton(
                                        highlighted = active,
                                        accentColor = accentColor,
                                        onClick = {
                                            if (active) {
                                                externalDisplayManager.stopPresentation()
                                            } else {
                                                val act = context as? Activity ?: context
                                                externalDisplayManager.startPresentation(act, d.displayId, settings.externalDisplayAspect)
                                            }
                                        }
                                    ) {
                                        Text(if (active) "Disconnect" else "Output Here", color = if (active) Color(0xFF061004) else CyrusColors.TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Dual-Screen (Controls on Phone, Game on TV)", color = CyrusColors.TextPrimary, fontSize = 12.sp)
                        Switch(
                            checked = settings.dualScreenControllerMode,
                            onCheckedChange = { v -> settingsRepository.update { it.copy(dualScreenControllerMode = v) } },
                            colors = SwitchDefaults.colors(checkedThumbColor = accentColor)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        AspectRatioMode.entries.forEach { mode ->
                            val sel = settings.externalDisplayAspect == mode
                            CyrusActionButton(
                                highlighted = sel,
                                accentColor = accentColor,
                                onClick = { settingsRepository.update { it.copy(externalDisplayAspect = mode) } }
                            ) {
                                Text(mode.label.substringBefore(" "), color = if (sel) Color(0xFF061004) else CyrusColors.TextPrimary, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }

            // Wireless Cast / Laptop Compatibility
            GlassPanel(modifier = Modifier.weight(1f), accentColor = accentColor) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("WIRELESS ROUTES & LAPTOP NOTES", color = accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    if (routes.isEmpty()) {
                        Text("No active MediaRouter wireless displays discovered. Use 'Android Cast Settings' for Miracast / Chromecast.", color = CyrusColors.TextSecondary, fontSize = 12.sp)
                    } else {
                        routes.forEach { r ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(r.name, color = CyrusColors.TextPrimary, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                CyrusActionButton(
                                    highlighted = r.isSelected,
                                    accentColor = accentColor,
                                    onClick = {
                                        if (r.isSelected) screenCastingManager.disconnectWirelessRoute()
                                        else screenCastingManager.selectRoute(r.id)
                                    }
                                ) {
                                    Text(if (r.isSelected) "Disconnect" else "Connect", color = if (r.isSelected) Color(0xFF061004) else CyrusColors.TextPrimary, fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.weight(1f))
                    Text("HARDWARE COMPATIBILITY & LIMITATIONS", color = CyrusColors.Warning, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    hardwareCaps.compatibilityNotes.forEach { note ->
                        Text("• $note", color = CyrusColors.TextSecondary, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
