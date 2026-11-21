package com.cyrus.emulator.nativebridge

class NativeLibretroBridge {
    companion object {
        private var hostLoaded = false

        init {
            runCatching {
                System.loadLibrary("cyrus_libretro_host")
                hostLoaded = true
            }
        }

        fun isHostLoaded(): Boolean = hostLoaded
    }

    external fun nativeIsLibraryAvailable(libName: String): Boolean
    external fun nativeInitialize(libName: String, systemDir: String, saveDir: String): Long
    external fun nativeLoadGame(handlePtr: Long, romPath: String, sramPath: String): Boolean
    external fun nativeRunFrame(handlePtr: Long, outRgba: ByteArray): IntArray
    external fun nativePollAudio(handlePtr: Long, outSamples: ShortArray): Int
    external fun nativeSetInput(
        handlePtr: Long,
        port: Int,
        joypadMask: Int,
        lx: Int,
        ly: Int,
        rx: Int,
        ry: Int
    )
    external fun nativeSaveState(handlePtr: Long, path: String): Boolean
    external fun nativeLoadState(handlePtr: Long, path: String): Boolean
    external fun nativeGetSampleRate(handlePtr: Long): Double
    external fun nativeDispose(handlePtr: Long)
}
