package com.cyrus.emulator.core.model

data class GameItem(
    val id: Long = 0L,
    val title: String,
    val console: ConsoleType,
    val uriString: String,
    val fileName: String,
    val fileExtension: String,
    val fileSizeBytes: Long,
    val crc32: String,
    val isFavorite: Boolean = false,
    val isMissing: Boolean = false,
    val lastPlayedAt: Long? = null,
    val playCount: Int = 0,
    val totalPlayTimeSeconds: Long = 0L,
    val addedAt: Long = System.currentTimeMillis()
)

data class ScannedRomCandidate(
    val uriString: String,
    val fileName: String,
    val title: String,
    val extension: String,
    val sizeBytes: Long,
    val crc32: String,
    val detectedConsole: ConsoleType?,
    val isZipArchive: Boolean,
    val innerRomName: String? = null,
    val isSupported: Boolean,
    val reason: String? = null
)

data class LibraryStats(
    val totalGames: Int = 0,
    val activeConsolesCount: Int = 0,
    val favoriteCount: Int = 0,
    val playedGamesCount: Int = 0,
    val lastPlayedGame: GameItem? = null
)

data class EngineReport(
    val console: ConsoleType,
    val engineName: String,
    val repositoryUrl: String,
    val language: String,
    val license: String,
    val licenseNotes: String,
    val dependencies: String,
    val androidSupported: Boolean,
    val requiredAbis: List<String>,
    val integrationMethod: String,
    val supportedFormats: List<String>,
    val biosRequirement: String,
    val nativeLibraryName: String,
    val isNativeLibraryPresent: Boolean,
    val statusNote: String
)

sealed interface CyrusResult<out T> {
    data class Success<T>(val data: T) : CyrusResult<T>
    data class Error(val message: String, val cause: Throwable? = null) : CyrusResult<Nothing>
}
