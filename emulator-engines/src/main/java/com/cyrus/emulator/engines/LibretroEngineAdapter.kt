package com.cyrus.emulator.engines

import com.cyrus.emulator.api.ControllerInputState
import com.cyrus.emulator.api.EmulationRuntimeStats
import com.cyrus.emulator.api.EmulatorEngine
import com.cyrus.emulator.api.EngineCapabilities
import com.cyrus.emulator.api.FrameResult
import com.cyrus.emulator.api.VideoSettings
import com.cyrus.emulator.core.model.ConsoleType
import com.cyrus.emulator.core.model.CyrusResult
import com.cyrus.emulator.nativebridge.NativeLibretroBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class LibretroEngineAdapter(
    override val consoleType: ConsoleType,
    override val engineName: String,
    private val sharedObjectName: String,
    override val capabilities: EngineCapabilities
) : EmulatorEngine {

    private val bridge = NativeLibretroBridge()
    private var handlePtr: Long = 0L
    private var saveDirPath: String = ""
    private var running = false
    private var paused = false
    private var lastWidth = 0
    private var lastHeight = 0
    private var fpsEstimate = 60f
    private var lastFrameNs = System.nanoTime()

    override suspend fun initialize(systemDir: String, saveDir: String): CyrusResult<Unit> =
        withContext(Dispatchers.IO) {
            if (!NativeLibretroBridge.isHostLoaded()) {
                return@withContext CyrusResult.Error("Native bridge cyrus_libretro_host failed to load.")
            }
            if (!bridge.nativeIsLibraryAvailable(sharedObjectName)) {
                return@withContext CyrusResult.Error(
                    "Core library '$sharedObjectName' is not bundled in this APK for ABI arm64-v8a."
                )
            }
            saveDirPath = saveDir
            File(systemDir).mkdirs()
            File(saveDir).mkdirs()

            val ptr = bridge.nativeInitialize(sharedObjectName, systemDir, saveDir)
            if (ptr == 0L) {
                CyrusResult.Error("Failed to initialize Libretro core: $sharedObjectName")
            } else {
                handlePtr = ptr
                CyrusResult.Success(Unit)
            }
        }

    override suspend fun loadGame(romFilePath: String): CyrusResult<Unit> =
        withContext(Dispatchers.IO) {
            if (handlePtr == 0L) return@withContext CyrusResult.Error("Engine not initialized")
            val romName = File(romFilePath).nameWithoutExtension
            val sramPath = File(saveDirPath, "$romName.srm").absolutePath
            val ok = bridge.nativeLoadGame(handlePtr, romFilePath, sramPath)
            if (ok) CyrusResult.Success(Unit)
            else CyrusResult.Error("Core '$engineName' rejected ROM file: ${File(romFilePath).name}")
        }

    override fun start() {
        running = true
        paused = false
        lastFrameNs = System.nanoTime()
    }

    override fun pause() {
        paused = true
    }

    override fun resume() {
        paused = false
        lastFrameNs = System.nanoTime()
    }

    override fun stop() {
        running = false
        paused = false
    }

    override suspend fun saveState(slotPath: String): CyrusResult<Unit> =
        withContext(Dispatchers.IO) {
            if (handlePtr == 0L) return@withContext CyrusResult.Error("Engine not running")
            if (bridge.nativeSaveState(handlePtr, slotPath)) CyrusResult.Success(Unit)
            else CyrusResult.Error("Save state failed for $engineName")
        }

    override suspend fun loadState(slotPath: String): CyrusResult<Unit> =
        withContext(Dispatchers.IO) {
            if (handlePtr == 0L) return@withContext CyrusResult.Error("Engine not running")
            if (bridge.nativeLoadState(handlePtr, slotPath)) CyrusResult.Success(Unit)
            else CyrusResult.Error("Load state failed for $engineName")
        }

    override fun setAudioVolume(volume: Float) {
        // Applied by AudioTrack stream mixer in EmulatorManager
    }

    override fun setVideoSettings(settings: VideoSettings) {
        // Applied by GL renderer in EmulatorPlayScreen & ExternalPresentation
    }

    override fun setControllerInput(input: ControllerInputState) {
        if (handlePtr == 0L) return
        var mask = 0
        if (input.b) mask = mask or (1 shl 0)
        if (input.y) mask = mask or (1 shl 1)
        if (input.select) mask = mask or (1 shl 2)
        if (input.start) mask = mask or (1 shl 3)
        if (input.up) mask = mask or (1 shl 4)
        if (input.down) mask = mask or (1 shl 5)
        if (input.left) mask = mask or (1 shl 6)
        if (input.right) mask = mask or (1 shl 7)
        if (input.a) mask = mask or (1 shl 8)
        if (input.x) mask = mask or (1 shl 9)
        if (input.l1) mask = mask or (1 shl 10)
        if (input.r1) mask = mask or (1 shl 11)
        if (input.l2) mask = mask or (1 shl 12)
        if (input.r2) mask = mask or (1 shl 13)

        val lx = (input.leftStickX.coerceIn(-1f, 1f) * 32767f).toInt()
        val ly = (input.leftStickY.coerceIn(-1f, 1f) * 32767f).toInt()
        val rx = (input.rightStickX.coerceIn(-1f, 1f) * 32767f).toInt()
        val ry = (input.rightStickY.coerceIn(-1f, 1f) * 32767f).toInt()

        bridge.nativeSetInput(handlePtr, input.port, mask, lx, ly, rx, ry)
    }

    override fun stepFrameAndGetRgba(outBuffer: ByteArray): FrameResult {
        if (handlePtr == 0L || !running || paused) {
            return FrameResult(lastWidth, lastHeight, false)
        }
        val meta = bridge.nativeRunFrame(handlePtr, outBuffer)
        val now = System.nanoTime()
        val dtSec = (now - lastFrameNs) / 1_000_000_000f
        if (dtSec > 0.0001f) {
            val instant = 1f / dtSec
            fpsEstimate = fpsEstimate * 0.9f + instant.coerceIn(1f, 120f) * 0.1f
        }
        lastFrameNs = now
        if (meta[2] == 1) {
            lastWidth = meta[0]
            lastHeight = meta[1]
            return FrameResult(meta[0], meta[1], true)
        }
        return FrameResult(lastWidth, lastHeight, false)
    }

    override fun pollAudioSamples(outSamples: ShortArray): Int {
        if (handlePtr == 0L || !running || paused) return 0
        return bridge.nativePollAudio(handlePtr, outSamples)
    }

    fun getCoreSampleRate(): Int {
        return if (handlePtr != 0L) bridge.nativeGetSampleRate(handlePtr).toInt().coerceIn(8000, 96000)
        else 44100
    }

    override fun getRuntimeStats(): EmulationRuntimeStats = EmulationRuntimeStats(
        fps = fpsEstimate,
        frameWidth = lastWidth,
        frameHeight = lastHeight,
        isRunning = running,
        isPaused = paused
    )

    override fun dispose() {
        running = false
        paused = false
        if (handlePtr != 0L) {
            bridge.nativeDispose(handlePtr)
            handlePtr = 0L
        }
    }
}
