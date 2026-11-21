package com.cyrus.emulator.api

import com.cyrus.emulator.core.model.ConsoleType
import com.cyrus.emulator.core.model.CyrusResult

enum class AspectRatioMode(val label: String) {
    FIT("Fit (Original)"),
    RATIO_4_3("4:3 Classic"),
    RATIO_16_9("16:9 Widescreen"),
    STRETCH("Fill Screen")
}

enum class VideoFilterMode(val label: String) {
    NEAREST("Sharp Pixels (Nearest)"),
    LINEAR("Smooth (Bilinear)"),
    CRT_SCANLINE("CRT Scanlines")
}

enum class RendererBackend(val label: String) {
    OPENGL_ES("OpenGL ES 3.0"),
    VULKAN("Vulkan (When Supported)")
}

data class VideoSettings(
    val aspectRatio: AspectRatioMode = AspectRatioMode.FIT,
    val resolutionScale: Int = 1,
    val vsyncEnabled: Boolean = true,
    val filterMode: VideoFilterMode = VideoFilterMode.NEAREST,
    val rendererBackend: RendererBackend = RendererBackend.OPENGL_ES,
    val showFps: Boolean = true
)

data class ControllerInputState(
    val port: Int = 0,
    val up: Boolean = false,
    val down: Boolean = false,
    val left: Boolean = false,
    val right: Boolean = false,
    val a: Boolean = false,
    val b: Boolean = false,
    val x: Boolean = false,
    val y: Boolean = false,
    val start: Boolean = false,
    val select: Boolean = false,
    val l1: Boolean = false,
    val r1: Boolean = false,
    val l2: Boolean = false,
    val r2: Boolean = false,
    val leftStickX: Float = 0f,
    val leftStickY: Float = 0f,
    val rightStickX: Float = 0f,
    val rightStickY: Float = 0f
)

data class EngineCapabilities(
    val supportsSaveState: Boolean,
    val supportsMemoryCard: Boolean,
    val supportsInternalResolutionScale: Boolean,
    val supportsVulkan: Boolean,
    val supportsAnalogStick: Boolean
)

data class EmulationRuntimeStats(
    val fps: Float = 0f,
    val frameWidth: Int = 0,
    val frameHeight: Int = 0,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false
)

interface EmulatorEngine {
    val consoleType: ConsoleType
    val engineName: String
    val capabilities: EngineCapabilities

    suspend fun initialize(systemDir: String, saveDir: String): CyrusResult<Unit>
    suspend fun loadGame(romFilePath: String): CyrusResult<Unit>
    fun start()
    fun pause()
    fun resume()
    fun stop()
    suspend fun saveState(slotPath: String): CyrusResult<Unit>
    suspend fun loadState(slotPath: String): CyrusResult<Unit>
    fun setAudioVolume(volume: Float)
    fun setVideoSettings(settings: VideoSettings)
    fun setControllerInput(input: ControllerInputState)
    fun stepFrameAndGetRgba(outBuffer: ByteArray): FrameResult
    fun pollAudioSamples(outSamples: ShortArray): Int
    fun getRuntimeStats(): EmulationRuntimeStats
    fun dispose()
}

data class FrameResult(
    val width: Int,
    val height: Int,
    val updated: Boolean
)
