package com.cyrus.emulator.engines

import com.cyrus.emulator.api.EmulatorEngine
import com.cyrus.emulator.api.EngineCapabilities
import com.cyrus.emulator.core.model.ConsoleType
import com.cyrus.emulator.core.model.EngineReport
import com.cyrus.emulator.nativebridge.NativeLibretroBridge

object EngineCatalog {

    fun createEngineForConsole(console: ConsoleType): EmulatorEngine {
        return when (console) {
            ConsoleType.GENESIS -> LibretroEngineAdapter(
                consoleType = ConsoleType.GENESIS,
                engineName = "Genesis Plus GX",
                sharedObjectName = "libretro_genesis_plus_gx.so",
                capabilities = EngineCapabilities(
                    supportsSaveState = true,
                    supportsMemoryCard = false,
                    supportsInternalResolutionScale = false,
                    supportsVulkan = false,
                    supportsAnalogStick = false
                )
            )
            ConsoleType.NES -> LibretroEngineAdapter(
                consoleType = ConsoleType.NES,
                engineName = "Nestopia UE",
                sharedObjectName = "libretro_nestopia.so",
                capabilities = EngineCapabilities(
                    supportsSaveState = true,
                    supportsMemoryCard = false,
                    supportsInternalResolutionScale = false,
                    supportsVulkan = false,
                    supportsAnalogStick = false
                )
            )
            ConsoleType.SNES -> LibretroEngineAdapter(
                consoleType = ConsoleType.SNES,
                engineName = "Snes9x",
                sharedObjectName = "libretro_snes9x.so",
                capabilities = EngineCapabilities(
                    supportsSaveState = true,
                    supportsMemoryCard = false,
                    supportsInternalResolutionScale = false,
                    supportsVulkan = false,
                    supportsAnalogStick = false
                )
            )
            ConsoleType.GB_GBC -> LibretroEngineAdapter(
                consoleType = ConsoleType.GB_GBC,
                engineName = "Gambatte",
                sharedObjectName = "libretro_gambatte.so",
                capabilities = EngineCapabilities(
                    supportsSaveState = true,
                    supportsMemoryCard = false,
                    supportsInternalResolutionScale = false,
                    supportsVulkan = false,
                    supportsAnalogStick = false
                )
            )
            ConsoleType.GBA -> LibretroEngineAdapter(
                consoleType = ConsoleType.GBA,
                engineName = "mGBA",
                sharedObjectName = "libretro_mgba.so",
                capabilities = EngineCapabilities(
                    supportsSaveState = true,
                    supportsMemoryCard = false,
                    supportsInternalResolutionScale = false,
                    supportsVulkan = false,
                    supportsAnalogStick = false
                )
            )
            ConsoleType.PS1 -> LibretroEngineAdapter(
                consoleType = ConsoleType.PS1,
                engineName = "PCSX-ReARMed",
                sharedObjectName = "libretro_pcsx_rearmed.so",
                capabilities = EngineCapabilities(
                    supportsSaveState = true,
                    supportsMemoryCard = true,
                    supportsInternalResolutionScale = true,
                    supportsVulkan = false,
                    supportsAnalogStick = true
                )
            )
            ConsoleType.PSP -> LibretroEngineAdapter(
                consoleType = ConsoleType.PSP,
                engineName = "PPSSPP (Native Core)",
                sharedObjectName = "libretro_ppsspp.so",
                capabilities = EngineCapabilities(
                    supportsSaveState = true,
                    supportsMemoryCard = true,
                    supportsInternalResolutionScale = true,
                    supportsVulkan = true,
                    supportsAnalogStick = true
                )
            )
            ConsoleType.PS2 -> LibretroEngineAdapter(
                consoleType = ConsoleType.PS2,
                engineName = "Play! (Experimental PS2)",
                sharedObjectName = "libplay_ps2.so",
                capabilities = EngineCapabilities(
                    supportsSaveState = false,
                    supportsMemoryCard = true,
                    supportsInternalResolutionScale = true,
                    supportsVulkan = true,
                    supportsAnalogStick = true
                )
            )
        }
    }

    fun getTechnicalReports(): List<EngineReport> {
        val bridge = NativeLibretroBridge()
        val hostOk = NativeLibretroBridge.isHostLoaded()
        fun hasLib(name: String): Boolean = hostOk && bridge.nativeIsLibraryAvailable(name)

        return listOf(
            EngineReport(
                console = ConsoleType.GENESIS,
                engineName = "Genesis Plus GX",
                repositoryUrl = "https://github.com/libretro/Genesis-Plus-GX",
                language = "C",
                license = "Non-Commercial (Genesis Plus / MAME derived)",
                licenseNotes = "Cannot be sold commercially without clearing MAME YM2612/SN76489 license terms.",
                dependencies = "zlib (optional)",
                androidSupported = true,
                requiredAbis = listOf("arm64-v8a"),
                integrationMethod = "Libretro C++ Host (JNI + dlopen)",
                supportedFormats = listOf("BIN", "GEN", "MD", "SMD", "ZIP"),
                biosRequirement = "None required for Genesis/MD (Optional for Mega-CD)",
                nativeLibraryName = "libretro_genesis_plus_gx.so",
                isNativeLibraryPresent = hasLib("libretro_genesis_plus_gx.so"),
                statusNote = if (hasLib("libretro_genesis_plus_gx.so")) "Native core bundled & ready" else "Core .so not present in APK"
            ),
            EngineReport(
                console = ConsoleType.NES,
                engineName = "Nestopia UE",
                repositoryUrl = "https://github.com/libretro/nestopia",
                language = "C++",
                license = "GPLv2",
                licenseNotes = "Copyleft open-source license.",
                dependencies = "None",
                androidSupported = true,
                requiredAbis = listOf("arm64-v8a"),
                integrationMethod = "Libretro C++ Host (JNI + dlopen)",
                supportedFormats = listOf("NES", "UNF", "UNIF", "ZIP"),
                biosRequirement = "None (Optional disksys.rom for FDS)",
                nativeLibraryName = "libretro_nestopia.so",
                isNativeLibraryPresent = hasLib("libretro_nestopia.so"),
                statusNote = if (hasLib("libretro_nestopia.so")) "Native core bundled & ready" else "Core .so not present in APK"
            ),
            EngineReport(
                console = ConsoleType.SNES,
                engineName = "Snes9x",
                repositoryUrl = "https://github.com/libretro/snes9x",
                language = "C++",
                license = "Non-Commercial (Snes9x License)",
                licenseNotes = "Free for non-commercial use; bsnes (GPLv3) is an alternative for strict GPL compliance.",
                dependencies = "None",
                androidSupported = true,
                requiredAbis = listOf("arm64-v8a"),
                integrationMethod = "Libretro C++ Host (JNI + dlopen)",
                supportedFormats = listOf("SFC", "SMC", "FIG", "ZIP"),
                biosRequirement = "None",
                nativeLibraryName = "libretro_snes9x.so",
                isNativeLibraryPresent = hasLib("libretro_snes9x.so"),
                statusNote = if (hasLib("libretro_snes9x.so")) "Native core bundled & ready" else "Core .so not present in APK"
            ),
            EngineReport(
                console = ConsoleType.GB_GBC,
                engineName = "Gambatte",
                repositoryUrl = "https://github.com/libretro/gambatte-libretro",
                language = "C++",
                license = "GPLv2",
                licenseNotes = "Compatible with open-source distribution.",
                dependencies = "None",
                androidSupported = true,
                requiredAbis = listOf("arm64-v8a"),
                integrationMethod = "Libretro C++ Host (JNI + dlopen)",
                supportedFormats = listOf("GB", "GBC", "ZIP"),
                biosRequirement = "None (Optional boot ROM)",
                nativeLibraryName = "libretro_gambatte.so",
                isNativeLibraryPresent = hasLib("libretro_gambatte.so"),
                statusNote = if (hasLib("libretro_gambatte.so")) "Native core bundled & ready" else "Core .so not present in APK"
            ),
            EngineReport(
                console = ConsoleType.GBA,
                engineName = "mGBA",
                repositoryUrl = "https://github.com/mgba-emu/mgba",
                language = "C",
                license = "MPL-2.0",
                licenseNotes = "Permissive file-level copyleft; high accuracy & speed.",
                dependencies = "zlib (optional)",
                androidSupported = true,
                requiredAbis = listOf("arm64-v8a"),
                integrationMethod = "Libretro C++ Host (JNI + dlopen)",
                supportedFormats = listOf("GBA", "ZIP"),
                biosRequirement = "Built-in HLE BIOS (Optional real gba_bios.bin)",
                nativeLibraryName = "libretro_mgba.so",
                isNativeLibraryPresent = hasLib("libretro_mgba.so"),
                statusNote = if (hasLib("libretro_mgba.so")) "Native core bundled & ready" else "Core .so not present in APK"
            ),
            EngineReport(
                console = ConsoleType.PS1,
                engineName = "PCSX-ReARMed",
                repositoryUrl = "https://github.com/libretro/pcsx_rearmed",
                language = "C / ARM64",
                license = "GPLv2",
                licenseNotes = "Chosen over DuckStation (PolyForm Strict) for clean open-source distribution.",
                dependencies = "zlib, chdr (optional)",
                androidSupported = true,
                requiredAbis = listOf("arm64-v8a"),
                integrationMethod = "Libretro C++ Host (JNI + dlopen)",
                supportedFormats = listOf("CUE", "CHD", "ISO", "PBP"),
                biosRequirement = "Built-in HLE fallback; scph5501.bin / scph1001.bin recommended",
                nativeLibraryName = "libretro_pcsx_rearmed.so",
                isNativeLibraryPresent = hasLib("libretro_pcsx_rearmed.so"),
                statusNote = if (hasLib("libretro_pcsx_rearmed.so")) "Native core bundled & ready" else "Core .so not present in APK"
            ),
            EngineReport(
                console = ConsoleType.PSP,
                engineName = "PPSSPP",
                repositoryUrl = "https://github.com/hrydgard/ppsspp",
                language = "C++",
                license = "GPLv2+",
                licenseNotes = "Requires full submodule CMake build (FFmpeg, glslang, SPIRV-Cross) for Android.",
                dependencies = "OpenGL ES 3.0 / Vulkan, FFmpeg, zlib",
                androidSupported = true,
                requiredAbis = listOf("arm64-v8a"),
                integrationMethod = "Native CMake / Libretro Core Integration",
                supportedFormats = listOf("ISO", "CSO", "PBP"),
                biosRequirement = "None (HLE)",
                nativeLibraryName = "libretro_ppsspp.so",
                isNativeLibraryPresent = hasLib("libretro_ppsspp.so"),
                statusNote = if (hasLib("libretro_ppsspp.so")) "Native core bundled & ready" else "Requires heavy PPSSPP CMake pipeline build"
            ),
            EngineReport(
                console = ConsoleType.PS2,
                engineName = "Play!",
                repositoryUrl = "https://github.com/jpd002/Play-",
                language = "C++",
                license = "BSD / MIT-style",
                licenseNotes = "Experimental on mobile; many titles have low FPS or rendering issues.",
                dependencies = "OpenGL ES 3.2 / Vulkan, CodeGen, Framework, Nuanceur",
                androidSupported = true,
                requiredAbis = listOf("arm64-v8a"),
                integrationMethod = "Independent Native Integration (Disabled by default until device-verified)",
                supportedFormats = listOf("ISO", "CHD", "CSO"),
                biosRequirement = "HLE built-in (Real PS2 BIOS optional)",
                nativeLibraryName = "libplay_ps2.so",
                isNativeLibraryPresent = hasLib("libplay_ps2.so"),
                statusNote = "Experimental: Not activated until Play! native build & device benchmark pass"
            )
        )
    }
}
