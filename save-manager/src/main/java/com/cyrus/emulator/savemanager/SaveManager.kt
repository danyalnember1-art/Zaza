package com.cyrus.emulator.savemanager

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

enum class SaveCategory(val label: String) {
    SAVE_STATE("Save State"),
    SAVE_DATA("Save Data / SRAM"),
    MEMORY_CARD("PS1 Memory Card")
}

data class SaveFileEntry(
    val fileName: String,
    val absolutePath: String,
    val category: SaveCategory,
    val sizeBytes: Long,
    val modifiedAt: Long
)

class SaveRepository(private val context: Context) {
    private val savesDir = File(context.filesDir, "saves").apply { mkdirs() }
    private val statesDir = File(context.filesDir, "states").apply { mkdirs() }

    suspend fun listAllSaves(): List<SaveFileEntry> = withContext(Dispatchers.IO) {
        val entries = mutableListOf<SaveFileEntry>()
        statesDir.listFiles()?.forEach { file ->
            if (file.isFile) {
                entries.add(
                    SaveFileEntry(
                        fileName = file.name,
                        absolutePath = file.absolutePath,
                        category = SaveCategory.SAVE_STATE,
                        sizeBytes = file.length(),
                        modifiedAt = file.lastModified()
                    )
                )
            }
        }
        savesDir.listFiles()?.forEach { file ->
            if (file.isFile) {
                val cat = if (file.extension.lowercase() in listOf("mcr", "mcd", "vmp")) {
                    SaveCategory.MEMORY_CARD
                } else {
                    SaveCategory.SAVE_DATA
                }
                entries.add(
                    SaveFileEntry(
                        fileName = file.name,
                        absolutePath = file.absolutePath,
                        category = cat,
                        sizeBytes = file.length(),
                        modifiedAt = file.lastModified()
                    )
                )
            }
        }
        entries.sortedByDescending { it.modifiedAt }
    }

    suspend fun deleteSave(absolutePath: String): Boolean = withContext(Dispatchers.IO) {
        runCatching { File(absolutePath).delete() }.getOrDefault(false)
    }

    suspend fun exportBackupZip(destinationUri: Uri): Int = withContext(Dispatchers.IO) {
        var count = 0
        context.contentResolver.openOutputStream(destinationUri)?.use { os ->
            ZipOutputStream(os).use { zos ->
                listOf("states" to statesDir, "saves" to savesDir).forEach { (prefix, dir) ->
                    dir.listFiles()?.forEach { file ->
                        if (file.isFile) {
                            zos.putNextEntry(ZipEntry("$prefix/${file.name}"))
                            FileInputStream(file).use { it.copyTo(zos) }
                            zos.closeEntry()
                            count++
                        }
                    }
                }
            }
        }
        count
    }

    suspend fun importBackupZip(sourceUri: Uri): Int = withContext(Dispatchers.IO) {
        var count = 0
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            ZipInputStream(input).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val safeName = File(entry.name).name
                        val targetDir = if (entry.name.startsWith("states/")) statesDir else savesDir
                        val outFile = File(targetDir, safeName)
                        FileOutputStream(outFile).use { fos -> zis.copyTo(fos) }
                        count++
                    }
                    entry = zis.nextEntry
                }
            }
        }
        count
    }
}
