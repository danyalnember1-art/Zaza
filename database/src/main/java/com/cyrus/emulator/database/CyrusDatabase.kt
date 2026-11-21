package com.cyrus.emulator.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        GameEntity::class,
        WatchedFolderEntity::class,
        BiosEntryEntity::class,
        ControllerProfileEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class CyrusDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
    abstract fun watchedFolderDao(): WatchedFolderDao
    abstract fun biosDao(): BiosDao
    abstract fun controllerProfileDao(): ControllerProfileDao

    companion object {
        @Volatile
        private var instance: CyrusDatabase? = null

        fun getInstance(context: Context): CyrusDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    CyrusDatabase::class.java,
                    "cyrus_database.db"
                ).build().also { instance = it }
            }
        }
    }
}
