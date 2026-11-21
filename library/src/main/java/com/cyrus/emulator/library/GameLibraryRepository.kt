package com.cyrus.emulator.library

import android.net.Uri
import com.cyrus.emulator.core.model.ConsoleType
import com.cyrus.emulator.core.model.GameItem
import com.cyrus.emulator.core.model.LibraryStats
import com.cyrus.emulator.core.model.ScannedRomCandidate
import com.cyrus.emulator.database.CyrusDatabase
import com.cyrus.emulator.database.GameEntity
import com.cyrus.emulator.database.WatchedFolderEntity
import com.cyrus.emulator.filemanager.RomFileScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

enum class LibrarySortMode(val label: String) {
    TITLE_ASC("Title (A-Z)"),
    LAST_PLAYED("Recently Played"),
    SIZE_DESC("File Size")
}

class GameLibraryRepository(
    private val database: CyrusDatabase,
    private val scanner: RomFileScanner
) {
    private val gameDao = database.gameDao()
    private val folderDao = database.watchedFolderDao()

    val gamesFlow: Flow<List<GameItem>> = gameDao.observeAllGames().map { entities ->
        entities.mapNotNull { it.toDomain() }
    }

    val statsFlow: Flow<LibraryStats> = gamesFlow.map { games ->
        LibraryStats(
            totalGames = games.size,
            activeConsolesCount = games.map { it.console }.distinct().size,
            favoriteCount = games.count { it.isFavorite },
            playedGamesCount = games.count { it.playCount > 0 },
            lastPlayedGame = games.filter { it.lastPlayedAt != null }
                .maxByOrNull { it.lastPlayedAt ?: 0L }
        )
    }

    val watchedFoldersFlow = folderDao.observeAll()

    suspend fun inspectFile(uri: Uri): ScannedRomCandidate = scanner.inspectSingleFile(uri)

    suspend fun scanFolderCandidates(treeUri: Uri): Pair<String, List<ScannedRomCandidate>> =
        scanner.scanFolder(treeUri)

    suspend fun addCandidate(
        candidate: ScannedRomCandidate,
        overrideConsole: ConsoleType? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val console = overrideConsole ?: candidate.detectedConsole ?: return@withContext false
        if (gameDao.findByUri(candidate.uriString) != null) return@withContext false
        if (candidate.crc32 != "00000000" &&
            gameDao.findByCrcAndConsole(candidate.crc32, console.id) != null
        ) {
            return@withContext false
        }

        val insertedId = gameDao.insertGame(
            GameEntity(
                title = candidate.title,
                consoleId = console.id,
                uriString = candidate.uriString,
                fileName = candidate.fileName,
                fileExtension = candidate.extension,
                fileSizeBytes = candidate.sizeBytes,
                crc32 = candidate.crc32,
                isMissing = false
            )
        )
        insertedId > 0L
    }

    suspend fun addFolderAndGames(
        treeUri: Uri,
        folderName: String,
        candidates: List<ScannedRomCandidate>
    ): Int = withContext(Dispatchers.IO) {
        folderDao.upsert(
            WatchedFolderEntity(
                uriString = treeUri.toString(),
                displayName = folderName,
                lastScannedAt = System.currentTimeMillis()
            )
        )
        var added = 0
        candidates.forEach { candidate ->
            if (addCandidate(candidate)) added++
        }
        added
    }

    suspend fun rescanAllAndVerifyFiles(): Pair<Int, Int> = withContext(Dispatchers.IO) {
        var missingCount = 0
        gameDao.getAllGamesOnce().forEach { game ->
            val ok = scanner.isUriAccessible(game.uriString)
            if (!ok) missingCount++
            if (game.isMissing != !ok) {
                gameDao.setMissing(game.id, !ok)
            }
        }

        var newlyAdded = 0
        folderDao.getAllOnce().forEach { folder ->
            val (_, scanned) = scanner.scanFolder(Uri.parse(folder.uriString))
            scanned.forEach { c ->
                if (addCandidate(c)) newlyAdded++
            }
            folderDao.upsert(folder.copy(lastScannedAt = System.currentTimeMillis()))
        }
        newlyAdded to missingCount
    }

    suspend fun toggleFavorite(gameId: Long, current: Boolean) {
        gameDao.setFavorite(gameId, !current)
    }

    suspend fun removeGame(gameId: Long) {
        gameDao.deleteGame(gameId)
    }

    suspend fun recordSession(gameId: Long, durationSeconds: Long) {
        gameDao.recordPlaySession(gameId, System.currentTimeMillis(), durationSeconds)
    }

    private fun GameEntity.toDomain(): GameItem? {
        val console = ConsoleType.fromId(consoleId) ?: return null
        return GameItem(
            id = id,
            title = title,
            console = console,
            uriString = uriString,
            fileName = fileName,
            fileExtension = fileExtension,
            fileSizeBytes = fileSizeBytes,
            crc32 = crc32,
            isFavorite = isFavorite,
            isMissing = isMissing,
            lastPlayedAt = lastPlayedAt,
            playCount = playCount,
            totalPlayTimeSeconds = totalPlayTimeSeconds,
            addedAt = addedAt
        )
    }
}
