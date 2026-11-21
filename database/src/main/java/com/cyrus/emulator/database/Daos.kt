package com.cyrus.emulator.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM games ORDER BY title COLLATE NOCASE ASC")
    fun observeAllGames(): Flow<List<GameEntity>>

    @Query("SELECT * FROM games")
    suspend fun getAllGamesOnce(): List<GameEntity>

    @Query("SELECT * FROM games WHERE id = :id")
    suspend fun getById(id: Long): GameEntity?

    @Query("SELECT * FROM games WHERE uriString = :uriString LIMIT 1")
    suspend fun findByUri(uriString: String): GameEntity?

    @Query("SELECT * FROM games WHERE crc32 = :crc32 AND consoleId = :consoleId LIMIT 1")
    suspend fun findByCrcAndConsole(crc32: String, consoleId: String): GameEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertGame(game: GameEntity): Long

    @Update
    suspend fun updateGame(game: GameEntity)

    @Query("DELETE FROM games WHERE id = :id")
    suspend fun deleteGame(id: Long)

    @Query("UPDATE games SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean)

    @Query("UPDATE games SET isMissing = :missing WHERE id = :id")
    suspend fun setMissing(id: Long, missing: Boolean)

    @Query("UPDATE games SET lastPlayedAt = :timestamp, playCount = playCount + 1, totalPlayTimeSeconds = totalPlayTimeSeconds + :addedSeconds WHERE id = :id")
    suspend fun recordPlaySession(id: Long, timestamp: Long, addedSeconds: Long)
}

@Dao
interface WatchedFolderDao {
    @Query("SELECT * FROM watched_folders ORDER BY lastScannedAt DESC")
    fun observeAll(): Flow<List<WatchedFolderEntity>>

    @Query("SELECT * FROM watched_folders")
    suspend fun getAllOnce(): List<WatchedFolderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(folder: WatchedFolderEntity)
}

@Dao
interface BiosDao {
    @Query("SELECT * FROM bios_entries")
    fun observeAll(): Flow<List<BiosEntryEntity>>

    @Query("SELECT * FROM bios_entries WHERE consoleId = :consoleId LIMIT 1")
    suspend fun getForConsole(consoleId: String): BiosEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: BiosEntryEntity)

    @Query("DELETE FROM bios_entries WHERE consoleId = :consoleId")
    suspend fun deleteForConsole(consoleId: String)
}

@Dao
interface ControllerProfileDao {
    @Query("SELECT * FROM controller_profiles WHERE consoleId = :consoleId LIMIT 1")
    suspend fun getProfile(consoleId: String): ControllerProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: ControllerProfileEntity)
}
