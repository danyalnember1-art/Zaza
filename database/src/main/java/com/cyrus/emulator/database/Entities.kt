package com.cyrus.emulator.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "games",
    indices = [
        Index(value = ["uriString"], unique = true),
        Index(value = ["crc32", "consoleId"])
    ]
)
data class GameEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val consoleId: String,
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

@Entity(tableName = "watched_folders")
data class WatchedFolderEntity(
    @PrimaryKey val uriString: String,
    val displayName: String,
    val lastScannedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "bios_entries")
data class BiosEntryEntity(
    @PrimaryKey val consoleId: String,
    val fileName: String,
    val localPath: String,
    val sha1: String,
    val sizeBytes: Long,
    val verified: Boolean,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "controller_profiles")
data class ControllerProfileEntity(
    @PrimaryKey val consoleId: String,
    val mappingJson: String,
    val analogSensitivity: Float = 1.0f,
    val deadzone: Float = 0.15f
)
