package com.cyrus.emulator.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyrus.emulator.api.AspectRatioMode
import com.cyrus.emulator.api.RendererBackend
import com.cyrus.emulator.api.VideoFilterMode
import com.cyrus.emulator.core.ui.components.CyrusActionButton
import com.cyrus.emulator.core.ui.components.GlassPanel
import com.cyrus.emulator.core.ui.theme.CyrusColors
import com.cyrus.emulator.library.GameLibraryRepository
import com.cyrus.emulator.settings.AppLanguage
import com.cyrus.emulator.settings.CyrusSettings
import com.cyrus.emulator.settings.CyrusSettingsRepository
import com.cyrus.emulator.ui.CyrusRoute
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    accentColor: Color,
    settings: CyrusSettings,
    settingsRepository: CyrusSettingsRepository,
    libraryRepository: GameLibraryRepository,
    onNavigate: (CyrusRoute) -> Unit
) {
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("SETTINGS", color = CyrusColors.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // GENERAL + APPEARANCE
            GlassPanel(modifier = Modifier.weight(1f), accentColor = accentColor) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("GENERAL & APPEARANCE", color = accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppLanguage.entries.forEach { lang ->
                            val sel = settings.language == lang
                            CyrusActionButton(highlighted = sel, accentColor = accentColor, onClick = {
                                settingsRepository.update { it.copy(language = lang) }
                            }) {
                                Text(lang.label, color = if (sel) Color(0xFF061004) else CyrusColors.TextPrimary, fontSize = 11.sp)
                            }
                        }
                    }
                    SettingToggle("Splash Intro Animation", settings.splashAnimationEnabled, accentColor) { v ->
                        settingsRepository.update { it.copy(splashAnimationEnabled = v) }
                    }
                    SettingToggle("Auto-Play System Music", settings.autoPlayMusic, accentColor) { v ->
                        settingsRepository.update { it.copy(autoPlayMusic = v) }
                    }
                    SettingToggle("Auto-Rotate Inside Game", settings.autoRotateInGame, accentColor) { v ->
                        settingsRepository.update { it.copy(autoRotateInGame = v) }
                    }
                    SettingToggle("Mute Music During Gameplay", settings.muteMusicDuringGame, accentColor) { v ->
                        settingsRepository.update { it.copy(muteMusicDuringGame = v) }
                    }
                    Text("Panel Opacity: ${(settings.panelOpacity * 100).toInt()}%", color = CyrusColors.TextSecondary, fontSize = 12.sp)
                    Slider(
                        value = settings.panelOpacity,
                        onValueChange = { v -> settingsRepository.update { it.copy(panelOpacity = v) } },
                        valueRange = 0.5f..1.0f,
                        colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor)
                    )
                }
            }

            // GRAPHICS + PERFORMANCE + STORAGE
            GlassPanel(modifier = Modifier.weight(1f), accentColor = accentColor) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("GRAPHICS & PERFORMANCE", color = accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        AspectRatioMode.entries.forEach { ar ->
                            val sel = settings.videoSettings.aspectRatio == ar
                            CyrusActionButton(highlighted = sel, accentColor = accentColor, onClick = {
                                settingsRepository.update { it.copy(videoSettings = it.videoSettings.copy(aspectRatio = ar)) }
                            }) {
                                Text(ar.label.substringBefore(" "), color = if (sel) Color(0xFF061004) else CyrusColors.TextPrimary, fontSize = 10.sp)
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        VideoFilterMode.entries.forEach { vf ->
                            val sel = settings.videoSettings.filterMode == vf
                            CyrusActionButton(highlighted = sel, accentColor = accentColor, onClick = {
                                settingsRepository.update { it.copy(videoSettings = it.videoSettings.copy(filterMode = vf)) }
                            }) {
                                Text(vf.label.substringBefore(" "), color = if (sel) Color(0xFF061004) else CyrusColors.TextPrimary, fontSize = 10.sp)
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        RendererBackend.entries.forEach { rb ->
                            val sel = settings.videoSettings.rendererBackend == rb
                            CyrusActionButton(highlighted = sel, accentColor = accentColor, onClick = {
                                settingsRepository.update { it.copy(videoSettings = it.videoSettings.copy(rendererBackend = rb)) }
                            }) {
                                Text(rb.label.substringBefore(" "), color = if (sel) Color(0xFF061004) else CyrusColors.TextPrimary, fontSize = 10.sp)
                            }
                        }
                    }
                    SettingToggle("Show FPS & Performance HUD", settings.videoSettings.showFps, accentColor) { v ->
                        settingsRepository.update {
                            it.copy(showPerformanceHud = v, videoSettings = it.videoSettings.copy(showFps = v))
                        }
                    }
                    SettingToggle("VSync", settings.videoSettings.vsyncEnabled, accentColor) { v ->
                        settingsRepository.update { it.copy(videoSettings = it.videoSettings.copy(vsyncEnabled = v)) }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CyrusActionButton(onClick = { scope.launch { libraryRepository.rescanAllAndVerifyFiles() } }) {
                            Text("Rescan Library", color = accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        CyrusActionButton(onClick = { onNavigate(CyrusRoute.CONSOLES) }) {
                            Text("BIOS Manager", color = CyrusColors.TextPrimary, fontSize = 11.sp)
                        }
                        CyrusActionButton(onClick = { onNavigate(CyrusRoute.EXTERNAL_DISPLAY) }) {
                            Text("External Display", color = CyrusColors.TextPrimary, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingToggle(
    label: String,
    checked: Boolean,
    accentColor: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = CyrusColors.TextPrimary, fontSize = 12.sp)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = accentColor)
        )
    }
}
