package com.cyrus.emulator.manager

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.cyrus.emulator.api.ControllerInputState
import com.cyrus.emulator.api.EmulationRuntimeStats
import com.cyrus.emulator.api.EmulatorEngine
import com.cyrus.emulator.api.FrameResult
import com.cyrus.emulator.api.VideoSettings
import com.cyrus.emulator.core.model.CyrusResult
import com.cyrus.emulator.core.model.GameItem
import com.cyrus.emulator.engines.EngineCatalog
import com.cyrus.emulator.engines.LibretroEngineAdapter
import com.cyrus.emulator.filemanager.RomFileScanner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

data class ActiveSessionInfo(
    val game: GameItem? = null,
    val engineName: String = "",
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val errorMessage: String? = null,
    val stats: EmulationRuntimeStats = EmulationRuntimeStats()
)

class EmulatorManager(
    private val context: Context,
    private val romScanner: RomFileScanner
) {
    private var activeEngine: EmulatorEngine? = null
    private var audioTrack: AudioTrack? = null
    private var audioVolume: Float = 0.9f
    private val audioBuffer = ShortArray(4096)
    private var sessionStartMs: Long = 0L

    private val _sessionInfo = MutableStateFlow(ActiveSessionInfo())
    val sessionInfo: StateFlow<ActiveSessionInfo> = _sessionInfo.asStateFlow()

    val systemDir: File = File(context.filesDir, "bios").apply { mkdirs() }
    val saveDir: File = File(context.filesDir, "saves").apply { mkdirs() }
    val statesDir: File = File(context.filesDir, "states").apply { mkdirs() }

    suspend fun launchGame(game: GameItem, videoSettings: VideoSettings, volume: Float): CyrusResult<Unit> {
        stopAndDispose()
        audioVolume = volume.coerceIn(0f, 1f)

        val engine = EngineCatalog.createEngineForConsole(game.console)
        val initRes = engine.initialize(systemDir.absolutePath, saveDir.absolutePath)
        if (initRes is CyrusResult.Error) {
            _sessionInfo.value = ActiveSessionInfo(
                game = game,
                engineName = engine.engineName,
                isRunning = false,
                errorMessage = initRes.message
            )
            return initRes
        }

        val stagedRom = romScanner.stageRomForNativeExecution(game.uriString, game.fileName)
            ?: return CyrusResult.Error("Unable to read ROM file from storage. Check file permission or Missing File status.")
                .also {
                    engine.dispose()
                    _sessionInfo.value = ActiveSessionInfo(game = game, engineName = engine.engineName, errorMessage = it.message)
                }

        val loadRes = engine.loadGame(stagedRom.absolutePath)
        if (loadRes is CyrusResult.Error) {
            engine.dispose()
            _sessionInfo.value = ActiveSessionInfo(game = game, engineName = engine.engineName, errorMessage = loadRes.message)
            return loadRes
        }

        activeEngine = engine
        engine.setVideoSettings(videoSettings)
        engine.setAudioVolume(volume)
        setupAudioTrack((engine as? LibretroEngineAdapter)?.getCoreSampleRate() ?: 44100)
        engine.start()
        sessionStartMs = System.currentTimeMillis()
        _sessionInfo.value = ActiveSessionInfo(
            game = game,
            engineName = engine.engineName,
            isRunning = true,
            isPaused = false,
            errorMessage = null
        )
        return CyrusResult.Success(Unit)
    }

    fun stepFrame(rgbaOut: ByteArray): FrameResult {
        val engine = activeEngine ?: return FrameResult(0, 0, false)
        val result = engine.stepFrameAndGetRgba(rgbaOut)
        val samples = engine.pollAudioSamples(audioBuffer)
        if (samples > 0 && audioTrack != null) {
            if (audioVolume < 0.99f) {
                for (i in 0 until samples) {
                    audioBuffer[i] = (audioBuffer[i] * audioVolume).toInt().toShort()
                }
            }
            audioTrack?.write(audioBuffer, 0, samples)
        }
        val stats = engine.getRuntimeStats()
        _sessionInfo.value = _sessionInfo.value.copy(stats = stats)
        return result
    }

    fun updateInput(state: ControllerInputState) {
        activeEngine?.setControllerInput(state)
    }

    fun pauseGame() {
        activeEngine?.pause()
        audioTrack?.pause()
        _sessionInfo.value = _sessionInfo.value.copy(isPaused = true)
    }

    fun resumeGame() {
        activeEngine?.resume()
        audioTrack?.play()
        _sessionInfo.value = _sessionInfo.value.copy(isPaused = false)
    }

    fun setAudioVolume(vol: Float) {
        audioVolume = vol.coerceIn(0f, 1f)
        activeEngine?.setAudioVolume(audioVolume)
    }

    fun updateVideoSettings(settings: VideoSettings) {
        activeEngine?.setVideoSettings(settings)
    }

    suspend fun saveState(slotIndex: Int): CyrusResult<String> {
        val game = _sessionInfo.value.game ?: return CyrusResult.Error("No active game")
        val engine = activeEngine ?: return CyrusResult.Error("Engine inactive")
        val file = File(statesDir, "${game.console.id}_${game.crc32}_slot$slotIndex.state")
        return when (val r = engine.saveState(file.absolutePath)) {
            is CyrusResult.Success -> CyrusResult.Success(file.name)
            is CyrusResult.Error -> r
        }
    }

    suspend fun loadState(slotIndex: Int): CyrusResult<String> {
        val game = _sessionInfo.value.game ?: return CyrusResult.Error("No active game")
        val engine = activeEngine ?: return CyrusResult.Error("Engine inactive")
        val file = File(statesDir, "${game.console.id}_${game.crc32}_slot$slotIndex.state")
        if (!file.exists()) return CyrusResult.Error("Save state slot $slotIndex is empty")
        return when (val r = engine.loadState(file.absolutePath)) {
            is CyrusResult.Success -> CyrusResult.Success(file.name)
            is CyrusResult.Error -> r
        }
    }

    fun getElapsedSeconds(): Long {
        if (sessionStartMs == 0L) return 0L
        return ((System.currentTimeMillis() - sessionStartMs) / 1000L).coerceAtLeast(0L)
    }

    fun getActiveCapabilities() = activeEngine?.capabilities

    fun stopAndDispose() {
        runCatching {
            activeEngine?.stop()
            activeEngine?.dispose()
        }
        activeEngine = null
        runCatching {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.release()
        }
        audioTrack = null
        sessionStartMs = 0L
        _sessionInfo.value = ActiveSessionInfo()
    }

    private fun setupAudioTrack(sampleRate: Int) {
        val minBuf = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(4096)

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build()
            )
            .setBufferSizeInBytes(minBuf * 2)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
            .apply { play() }
    }
}
