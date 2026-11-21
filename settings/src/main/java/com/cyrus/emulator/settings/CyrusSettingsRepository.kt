package com.cyrus.emulator.settings

import android.content.Context
import com.cyrus.emulator.api.AspectRatioMode
import com.cyrus.emulator.api.RendererBackend
import com.cyrus.emulator.api.VideoFilterMode
import com.cyrus.emulator.api.VideoSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppLanguage(val label: String) {
    ENGLISH("English"),
    PERSIAN("فارسی / Bilingual")
}

data class CyrusSettings(
    val language: AppLanguage = AppLanguage.ENGLISH,
    val autoRotateInGame: Boolean = false,
    val autoPlayMusic: Boolean = true,
    val splashAnimationEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val accentColorHex: Long = 0xFF91EF38,
    val panelOpacity: Float = 0.86f,
    val compactCards: Boolean = false,
    val visualEffectsEnabled: Boolean = true,
    val musicVolume: Float = 0.45f,
    val sfxVolume: Float = 0.65f,
    val gameVolume: Float = 0.90f,
    val muteMusicDuringGame: Boolean = true,
    val selectedTrackId: Int = 0,
    val shuffleMusic: Boolean = false,
    val videoSettings: VideoSettings = VideoSettings(),
    val showPerformanceHud: Boolean = true,
    val lowMemoryMode: Boolean = false,
    val externalDisplayAspect: AspectRatioMode = AspectRatioMode.FIT,
    val externalDisplayFullscreen: Boolean = true,
    val dualScreenControllerMode: Boolean = true
)

class CyrusSettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("cyrus_settings_prefs", Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<CyrusSettings> = _settings.asStateFlow()

    fun update(transform: (CyrusSettings) -> CyrusSettings) {
        val next = transform(_settings.value)
        _settings.value = next
        save(next)
    }

    private fun load(): CyrusSettings {
        return CyrusSettings(
            language = AppLanguage.entries.getOrElse(prefs.getInt("language", 0)) { AppLanguage.ENGLISH },
            autoRotateInGame = prefs.getBoolean("autoRotateInGame", false),
            autoPlayMusic = prefs.getBoolean("autoPlayMusic", true),
            splashAnimationEnabled = prefs.getBoolean("splashAnimationEnabled", true),
            notificationsEnabled = prefs.getBoolean("notificationsEnabled", true),
            accentColorHex = prefs.getLong("accentColorHex", 0xFF91EF38),
            panelOpacity = prefs.getFloat("panelOpacity", 0.86f),
            compactCards = prefs.getBoolean("compactCards", false),
            visualEffectsEnabled = prefs.getBoolean("visualEffectsEnabled", true),
            musicVolume = prefs.getFloat("musicVolume", 0.45f),
            sfxVolume = prefs.getFloat("sfxVolume", 0.65f),
            gameVolume = prefs.getFloat("gameVolume", 0.90f),
            muteMusicDuringGame = prefs.getBoolean("muteMusicDuringGame", true),
            selectedTrackId = prefs.getInt("selectedTrackId", 0),
            shuffleMusic = prefs.getBoolean("shuffleMusic", false),
            videoSettings = VideoSettings(
                aspectRatio = AspectRatioMode.entries.getOrElse(prefs.getInt("aspectRatio", 0)) { AspectRatioMode.FIT },
                resolutionScale = prefs.getInt("resolutionScale", 1).coerceIn(1, 4),
                vsyncEnabled = prefs.getBoolean("vsyncEnabled", true),
                filterMode = VideoFilterMode.entries.getOrElse(prefs.getInt("filterMode", 0)) { VideoFilterMode.NEAREST },
                rendererBackend = RendererBackend.entries.getOrElse(prefs.getInt("rendererBackend", 0)) { RendererBackend.OPENGL_ES },
                showFps = prefs.getBoolean("showFps", true)
            ),
            showPerformanceHud = prefs.getBoolean("showPerformanceHud", true),
            lowMemoryMode = prefs.getBoolean("lowMemoryMode", false),
            externalDisplayAspect = AspectRatioMode.entries.getOrElse(prefs.getInt("extAspect", 0)) { AspectRatioMode.FIT },
            externalDisplayFullscreen = prefs.getBoolean("extFullscreen", true),
            dualScreenControllerMode = prefs.getBoolean("dualScreen", true)
        )
    }

    private fun save(s: CyrusSettings) {
        prefs.edit()
            .putInt("language", s.language.ordinal)
            .putBoolean("autoRotateInGame", s.autoRotateInGame)
            .putBoolean("autoPlayMusic", s.autoPlayMusic)
            .putBoolean("splashAnimationEnabled", s.splashAnimationEnabled)
            .putBoolean("notificationsEnabled", s.notificationsEnabled)
            .putLong("accentColorHex", s.accentColorHex)
            .putFloat("panelOpacity", s.panelOpacity)
            .putBoolean("compactCards", s.compactCards)
            .putBoolean("visualEffectsEnabled", s.visualEffectsEnabled)
            .putFloat("musicVolume", s.musicVolume)
            .putFloat("sfxVolume", s.sfxVolume)
            .putFloat("gameVolume", s.gameVolume)
            .putBoolean("muteMusicDuringGame", s.muteMusicDuringGame)
            .putInt("selectedTrackId", s.selectedTrackId)
            .putBoolean("shuffleMusic", s.shuffleMusic)
            .putInt("aspectRatio", s.videoSettings.aspectRatio.ordinal)
            .putInt("resolutionScale", s.videoSettings.resolutionScale)
            .putBoolean("vsyncEnabled", s.videoSettings.vsyncEnabled)
            .putInt("filterMode", s.videoSettings.filterMode.ordinal)
            .putInt("rendererBackend", s.videoSettings.rendererBackend.ordinal)
            .putBoolean("showFps", s.videoSettings.showFps)
            .putBoolean("showPerformanceHud", s.showPerformanceHud)
            .putBoolean("lowMemoryMode", s.lowMemoryMode)
            .putInt("extAspect", s.externalDisplayAspect.ordinal)
            .putBoolean("extFullscreen", s.externalDisplayFullscreen)
            .putBoolean("dualScreen", s.dualScreenControllerMode)
            .apply()
    }
}
