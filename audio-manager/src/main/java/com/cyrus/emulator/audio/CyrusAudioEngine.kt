package com.cyrus.emulator.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

enum class UiSfx {
    BOOT_LOGO,
    MOVE,
    SELECT,
    BACK,
    OPEN_PAGE,
    ADD_GAME,
    LAUNCH_GAME,
    PAUSE_GAME,
    ENTER_CONSOLE,
    ERROR
}

data class MusicTrack(
    val id: Int,
    val title: String,
    val subtitle: String,
    val bpm: Int,
    val rootHz: Double,
    val intervals: List<Double>
)

data class MusicUiState(
    val currentTrack: MusicTrack,
    val isPlaying: Boolean = false,
    val shuffleEnabled: Boolean = false,
    val volume: Float = 0.45f,
    val sfxVolume: Float = 0.65f,
    val isMuted: Boolean = false,
    val duckedForGame: Boolean = false
)

class CyrusAudioEngine {
    companion object {
        private const val SAMPLE_RATE = 22050
        val TRACKS = listOf(
            MusicTrack(0, "Neon Horizon", "CYRUS Ambient Synth", 88, 110.0, listOf(1.0, 1.189, 1.498, 1.782, 2.0)),
            MusicTrack(1, "Achaemenid Pulse", "CYRUS Sci-Fi Vector", 96, 98.0, listOf(1.0, 1.26, 1.498, 1.888, 2.245)),
            MusicTrack(2, "Cyber Arcade 84", "CYRUS Nostalgic Grid", 104, 130.81, listOf(1.0, 1.189, 1.335, 1.498, 1.782)),
            MusicTrack(3, "Midnight Silicon", "CYRUS Deep Console", 82, 87.31, listOf(1.0, 1.122, 1.498, 1.682, 2.0))
        )
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var bgmJob: Job? = null
    private var audioTrack: AudioTrack? = null

    private val _state = MutableStateFlow(MusicUiState(currentTrack = TRACKS.first()))
    val state: StateFlow<MusicUiState> = _state.asStateFlow()

    fun startMusic() {
        if (_state.value.isPlaying && bgmJob?.isActive == true) return
        _state.value = _state.value.copy(isPlaying = true)
        launchSynthLoop()
    }

    fun pauseMusic() {
        _state.value = _state.value.copy(isPlaying = false)
        bgmJob?.cancel()
        bgmJob = null
        releaseTrack()
    }

    fun togglePlayPause() {
        if (_state.value.isPlaying) pauseMusic() else startMusic()
    }

    fun nextTrack() {
        val list = TRACKS
        val currentIdx = list.indexOfFirst { it.id == _state.value.currentTrack.id }.coerceAtLeast(0)
        val next = if (_state.value.shuffleEnabled) {
            list.filter { it.id != _state.value.currentTrack.id }.randomOrNull() ?: list.first()
        } else {
            list[(currentIdx + 1) % list.size]
        }
        _state.value = _state.value.copy(currentTrack = next)
    }

    fun previousTrack() {
        val list = TRACKS
        val currentIdx = list.indexOfFirst { it.id == _state.value.currentTrack.id }.coerceAtLeast(0)
        val prev = list[(currentIdx - 1 + list.size) % list.size]
        _state.value = _state.value.copy(currentTrack = prev)
    }

    fun selectTrack(trackId: Int) {
        val track = TRACKS.firstOrNull { it.id == trackId } ?: return
        _state.value = _state.value.copy(currentTrack = track)
    }

    fun setShuffle(enabled: Boolean) {
        _state.value = _state.value.copy(shuffleEnabled = enabled)
    }

    fun setMusicVolume(volume: Float) {
        _state.value = _state.value.copy(volume = volume.coerceIn(0f, 1f))
    }

    fun setSfxVolume(volume: Float) {
        _state.value = _state.value.copy(sfxVolume = volume.coerceIn(0f, 1f))
    }

    fun setMuted(muted: Boolean) {
        _state.value = _state.value.copy(isMuted = muted)
    }

    fun setGameActiveDucking(active: Boolean, muteCompletely: Boolean) {
        _state.value = _state.value.copy(duckedForGame = active)
        if (active && muteCompletely && _state.value.isPlaying) {
            pauseMusic()
        }
    }

    fun playSfx(sfx: UiSfx) {
        val sfxVol = _state.value.sfxVolume
        if (sfxVol <= 0.01f) return
        scope.launch {
            runCatching { playSynthesizedSfx(sfx, sfxVol) }
        }
    }

    private fun launchSynthLoop() {
        bgmJob?.cancel()
        bgmJob = scope.launch {
            val minBuf = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(2048)

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(minBuf * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack = track
            track.play()

            val buffer = ShortArray(1024)
            var sampleIndex = 0L
            var padPhase1 = 0.0
            var padPhase2 = 0.0
            var arpPhase = 0.0

            while (isActive && _state.value.isPlaying) {
                val cur = _state.value
                val activeTrack = cur.currentTrack
                val effectiveVol = when {
                    cur.isMuted -> 0f
                    cur.duckedForGame -> cur.volume * 0.15f
                    else -> cur.volume
                }

                val stepSamples = (SAMPLE_RATE * 60) / (activeTrack.bpm * 2)
                for (i in buffer.indices) {
                    val step = ((sampleIndex / stepSamples) % activeTrack.intervals.size).toInt()
                    val chordStep = ((sampleIndex / (stepSamples * 8)) % 4).toInt()
                    val chordMult = when (chordStep) {
                        0 -> 1.0
                        1 -> 1.189
                        2 -> 0.891
                        else -> 1.335
                    }
                    val padFreq = activeTrack.rootHz * chordMult
                    val arpFreq = activeTrack.rootHz * 2.0 * activeTrack.intervals[step]

                    padPhase1 += 2.0 * PI * padFreq / SAMPLE_RATE
                    padPhase2 += 2.0 * PI * (padFreq * 1.498) / SAMPLE_RATE
                    arpPhase += 2.0 * PI * arpFreq / SAMPLE_RATE

                    val stepLocal = (sampleIndex % stepSamples).toDouble() / stepSamples.toDouble()
                    val arpEnv = exp(-stepLocal * 5.2)

                    val padWave = (sin(padPhase1) * 0.42 + sin(padPhase2) * 0.25)
                    val arpWave = sin(arpPhase) * 0.22 * arpEnv
                    val mixed = (padWave + arpWave) * effectiveVol * 0.45

                    buffer[i] = (mixed.coerceIn(-0.95, 0.95) * Short.MAX_VALUE).toInt().toShort()
                    sampleIndex++
                }
                track.write(buffer, 0, buffer.size)
            }
        }
    }

    private fun playSynthesizedSfx(sfx: UiSfx, volume: Float) {
        val (freqStart, freqEnd, durationMs) = when (sfx) {
            UiSfx.BOOT_LOGO -> Triple(220.0, 660.0, 520)
            UiSfx.MOVE -> Triple(520.0, 620.0, 42)
            UiSfx.SELECT -> Triple(640.0, 960.0, 85)
            UiSfx.BACK -> Triple(540.0, 340.0, 75)
            UiSfx.OPEN_PAGE -> Triple(440.0, 740.0, 110)
            UiSfx.ADD_GAME -> Triple(523.25, 1046.5, 160)
            UiSfx.LAUNCH_GAME -> Triple(330.0, 880.0, 240)
            UiSfx.PAUSE_GAME -> Triple(480.0, 280.0, 120)
            UiSfx.ENTER_CONSOLE -> Triple(392.0, 784.0, 140)
            UiSfx.ERROR -> Triple(210.0, 140.0, 190)
        }
        val totalSamples = (SAMPLE_RATE * durationMs) / 1000
        val pcm = ShortArray(totalSamples)
        var phase = 0.0
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / totalSamples.toDouble()
            val f = freqStart + (freqEnd - freqStart) * t
            phase += 2.0 * PI * f / SAMPLE_RATE
            val env = (1.0 - t) * (if (t < 0.08) t / 0.08 else 1.0)
            val s = sin(phase) * env * volume * 0.45
            pcm[i] = (s.coerceIn(-0.95, 0.95) * Short.MAX_VALUE).toInt().toShort()
        }
        val minBuf = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(pcm.size * 2)

        val player = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(minBuf)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        player.write(pcm, 0, pcm.size)
        player.play()
        Thread.sleep(durationMs.toLong() + 30L)
        runCatching {
            player.stop()
            player.release()
        }
    }

    private fun releaseTrack() {
        runCatching {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.release()
        }
        audioTrack = null
    }

    fun release() {
        pauseMusic()
    }
}
