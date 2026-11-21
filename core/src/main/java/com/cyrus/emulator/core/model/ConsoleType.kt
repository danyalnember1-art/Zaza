package com.cyrus.emulator.core.model

enum class ConsoleFamily {
    SEGA,
    NINTENDO,
    PLAYSTATION
}

enum class ConsoleType(
    val id: String,
    val displayName: String,
    val shortName: String,
    val family: ConsoleFamily,
    val extensions: List<String>,
    val accentHex: Long,
    val requiresBios: Boolean,
    val isExperimental: Boolean = false
) {
    GENESIS(
        id = "genesis",
        displayName = "Sega Genesis / Mega Drive",
        shortName = "GENESIS",
        family = ConsoleFamily.SEGA,
        extensions = listOf("bin", "gen", "md", "smd"),
        accentHex = 0xFF38BDF8,
        requiresBios = false
    ),
    NES(
        id = "nes",
        displayName = "Nintendo Entertainment System",
        shortName = "NES",
        family = ConsoleFamily.NINTENDO,
        extensions = listOf("nes", "unf", "unif"),
        accentHex = 0xFFEF4444,
        requiresBios = false
    ),
    SNES(
        id = "snes",
        displayName = "Super Nintendo",
        shortName = "SNES",
        family = ConsoleFamily.NINTENDO,
        extensions = listOf("sfc", "smc", "fig"),
        accentHex = 0xFFA855F7,
        requiresBios = false
    ),
    GB_GBC(
        id = "gb_gbc",
        displayName = "Game Boy / Color",
        shortName = "GB / GBC",
        family = ConsoleFamily.NINTENDO,
        extensions = listOf("gb", "gbc"),
        accentHex = 0xFF22C55E,
        requiresBios = false
    ),
    GBA(
        id = "gba",
        displayName = "Game Boy Advance",
        shortName = "GBA",
        family = ConsoleFamily.NINTENDO,
        extensions = listOf("gba"),
        accentHex = 0xFF6366F1,
        requiresBios = false
    ),
    PS1(
        id = "ps1",
        displayName = "PlayStation 1",
        shortName = "PS1",
        family = ConsoleFamily.PLAYSTATION,
        extensions = listOf("cue", "chd", "iso", "pbp"),
        accentHex = 0xFF94A3B8,
        requiresBios = false // PCSX-ReARMed provides HLE fallback; real BIOS improves compatibility
    ),
    PSP(
        id = "psp",
        displayName = "PlayStation Portable",
        shortName = "PSP",
        family = ConsoleFamily.PLAYSTATION,
        extensions = listOf("iso", "cso", "pbp"),
        accentHex = 0xFF0EA5E9,
        requiresBios = false
    ),
    PS2(
        id = "ps2",
        displayName = "PlayStation 2 (Experimental)",
        shortName = "PS2",
        family = ConsoleFamily.PLAYSTATION,
        extensions = listOf("iso", "chd", "cso"),
        accentHex = 0xFF3B82F6,
        requiresBios = true,
        isExperimental = true
    );

    companion object {
        fun fromId(id: String): ConsoleType? = entries.find { it.id == id }

        fun detectFromExtension(ext: String, fileSizeBytes: Long = 0L): ConsoleType? {
            val normalized = ext.lowercase().removePrefix(".")
            if (normalized == "bin" && fileSizeBytes > 16L * 1024L * 1024L) {
                return PS1
            }
            if (normalized == "iso" || normalized == "cso") {
                return if (fileSizeBytes > 2_000_000_000L) PS2 else PSP
            }
            return entries.firstOrNull { normalized in it.extensions }
        }
    }
}
