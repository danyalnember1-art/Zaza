package com.cyrus.emulator.bios

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.cyrus.emulator.core.model.ConsoleType
import com.cyrus.emulator.core.model.CyrusResult
import com.cyrus.emulator.database.BiosEntryEntity
import com.cyrus.emulator.database.CyrusDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

data class BiosSpec(
    val console: ConsoleType,
    val expectedFileName: String,
    val description: String,
    val minBytes: Long,
    val maxBytes: Long,
    val required: Boolean
)

data class BiosStatusItem(
    val spec: BiosSpec,
    val installed: Boolean,
    val installedFileName: String? = null,
    val sha1: String? = null,
    val verified: Boolean = false
)

class BiosRepository(
    private val context: Context,
    database: CyrusDatabase
) {
    private val biosDao = database.biosDao()
    private val biosDir = File(context.filesDir, "bios").apply { mkdirs() }

    val specs = listOf(
        BiosSpec(
            console = ConsoleType.PS1,
            expectedFileName = "scph5501.bin",
            description = "PlayStation 1 BIOS (512 KB). Optional with PCSX-ReARMed HLE, recommended for full compatibility.",
            minBytes = 512 * 1024L,
            maxBytes = 524288L,
            required = false
        ),
        BiosSpec(
            console = ConsoleType.GBA,
            expectedFileName = "gba_bios.bin",
            description = "Game Boy Advance BIOS (16 KB). Optional; mGBA has built-in HLE BIOS.",
            minBytes = 16 * 1024L,
            maxBytes = 16 * 1024L,
            required = false
        ),
        BiosSpec(
            console = ConsoleType.GENESIS,
            expectedFileName = "bios_CD_U.bin",
            description = "Sega Mega-CD BIOS (128 KB). Only needed for Sega CD images.",
            minBytes = 128 * 1024L,
            maxBytes = 131072L,
            required = false
        ),
        BiosSpec(
            console = ConsoleType.PS2,
            expectedFileName = "ps2_bios.bin",
            description = "PlayStation 2 BIOS (4 MB). Required for full Play! BIOS mode when experimental PS2 engine is active.",
            minBytes = 4 * 1024 * 1024L,
            maxBytes = 4 * 1024 * 1024L,
            required = true
        )
    )

    fun observeBiosStatuses(): Flow<List<BiosStatusItem>> = biosDao.observeAll().map { entries ->
        val map = entries.associateBy { it.consoleId }
        specs.map { spec ->
            val entry = map[spec.console.id]
            val fileExists = entry != null && File(entry.localPath).exists()
            BiosStatusItem(
                spec = spec,
                installed = fileExists,
                installedFileName = if (fileExists) entry?.fileName else null,
                sha1 = if (fileExists) entry?.sha1 else null,
                verified = fileExists && (entry?.verified == true)
            )
        }
    }

    suspend fun importBios(console: ConsoleType, uri: Uri): CyrusResult<BiosStatusItem> =
        withContext(Dispatchers.IO) {
            val spec = specs.firstOrNull { it.console == console }
                ?: return@withContext CyrusResult.Error("No BIOS specification for ${console.displayName}")

            val sourceName = resolveName(uri)
            val destFile = File(biosDir, spec.expectedFileName)
            val digest = MessageDigest.getInstance("SHA-1")
            var totalBytes = 0L

            val copied = runCatching {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        val buf = ByteArray(16384)
                        while (true) {
                            val r = input.read(buf)
                            if (r <= 0) break
                            output.write(buf, 0, r)
                            digest.update(buf, 0, r)
                            totalBytes += r
                        }
                    }
                } != null
            }.getOrDefault(false)

            if (!copied || totalBytes == 0L) {
                destFile.delete()
                return@withContext CyrusResult.Error("Failed to read selected BIOS file.")
            }

            val sizeValid = totalBytes in spec.minBytes..spec.maxBytes
            val sha1Hex = digest.digest().joinToString("") { "%02x".format(it) }
            if (!sizeValid) {
                destFile.delete()
                return@withContext CyrusResult.Error(
                    "Invalid BIOS size ($totalBytes bytes). Expected ${spec.minBytes} bytes for ${spec.expectedFileName}."
                )
            }

            val entity = BiosEntryEntity(
                consoleId = console.id,
                fileName = sourceName,
                localPath = destFile.absolutePath,
                sha1 = sha1Hex,
                sizeBytes = totalBytes,
                verified = true
            )
            biosDao.upsert(entity)
            CyrusResult.Success(
                BiosStatusItem(
                    spec = spec,
                    installed = true,
                    installedFileName = sourceName,
                    sha1 = sha1Hex,
                    verified = true
                )
            )
        }

    suspend fun removeBios(console: ConsoleType) = withContext(Dispatchers.IO) {
        val existing = biosDao.getForConsole(console.id)
        if (existing != null) {
            File(existing.localPath).delete()
            biosDao.deleteForConsole(console.id)
        }
    }

    private fun resolveName(uri: Uri): String {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (i >= 0) return c.getString(i) ?: "bios.bin"
            }
        }
        return "bios.bin"
    }
}
