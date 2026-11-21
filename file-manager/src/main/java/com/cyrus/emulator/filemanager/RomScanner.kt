package com.cyrus.emulator.filemanager

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import com.cyrus.emulator.core.model.ConsoleType
import com.cyrus.emulator.core.model.ScannedRomCandidate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.CRC32
import java.util.zip.ZipInputStream

class RomFileScanner(private val context: Context) {

    fun persistReadPermission(uri: Uri) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
    }

    suspend fun inspectSingleFile(uri: Uri): ScannedRomCandidate = withContext(Dispatchers.IO) {
        persistReadPermission(uri)
        val fileName = resolveFileName(uri)
        val sizeBytes = resolveFileSize(uri)
        val ext = fileName.substringAfterLast('.', "").lowercase()

        if (ext == "zip") {
            inspectZipUri(uri, fileName, sizeBytes)
        } else {
            val console = ConsoleType.detectFromExtension(ext, sizeBytes)
            val crc = computeQuickCrc32(uri)
            val title = cleanGameTitle(fileName)
            ScannedRomCandidate(
                uriString = uri.toString(),
                fileName = fileName,
                title = title,
                extension = ext,
                sizeBytes = sizeBytes,
                crc32 = crc,
                detectedConsole = console,
                isZipArchive = false,
                isSupported = console != null,
                reason = if (console == null) "Unsupported file format .$ext" else null
            )
        }
    }

    suspend fun scanFolder(treeUri: Uri): Pair<String, List<ScannedRomCandidate>> = withContext(Dispatchers.IO) {
        persistReadPermission(treeUri)
        val root = DocumentFile.fromTreeUri(context, treeUri)
        val folderName = root?.name ?: "Selected Folder"
        val results = mutableListOf<ScannedRomCandidate>()

        fun walk(dir: DocumentFile, depth: Int) {
            if (depth > 3) return
            dir.listFiles().forEach { entry ->
                if (entry.isDirectory) {
                    walk(entry, depth + 1)
                } else if (entry.isFile) {
                    val name = entry.name ?: return@forEach
                    val ext = name.substringAfterLast('.', "").lowercase()
                    val size = entry.length()
                    val candidate = if (ext == "zip") {
                        inspectZipUri(entry.uri, name, size)
                    } else {
                        val console = ConsoleType.detectFromExtension(ext, size)
                        if (console != null) {
                            ScannedRomCandidate(
                                uriString = entry.uri.toString(),
                                fileName = name,
                                title = cleanGameTitle(name),
                                extension = ext,
                                sizeBytes = size,
                                crc32 = computeQuickCrc32(entry.uri),
                                detectedConsole = console,
                                isZipArchive = false,
                                isSupported = true
                            )
                        } else null
                    }
                    if (candidate != null && candidate.isSupported) {
                        results.add(candidate)
                    }
                }
            }
        }

        if (root != null && root.isDirectory) {
            walk(root, 0)
        }
        folderName to results
    }

    fun isUriAccessible(uriString: String): Boolean {
        return runCatching {
            val uri = Uri.parse(uriString)
            val doc = DocumentFile.fromSingleUri(context, uri)
            if (doc != null && doc.exists()) return true
            context.contentResolver.openInputStream(uri)?.use { true } ?: false
        }.getOrDefault(false)
    }

    /**
     * Stages a read-only working copy into app internal cache for native C/C++ cores
     * that require a POSIX filesystem path, without modifying or moving the user's original ROM.
     */
    suspend fun stageRomForNativeExecution(
        uriString: String,
        preferredFileName: String
    ): File? = withContext(Dispatchers.IO) {
        runCatching {
            val uri = Uri.parse(uriString)
            val cacheDir = File(context.cacheDir, "staged_roms").apply { mkdirs() }
            // Clear old staged ROMs except current to protect storage
            cacheDir.listFiles()?.forEach { old ->
                if (old.name != preferredFileName) old.delete()
            }

            val ext = preferredFileName.substringAfterLast('.', "").lowercase()
            if (ext == "zip") {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    ZipInputStream(input).use { zis ->
                        var entry = zis.nextEntry
                        while (entry != null) {
                            if (!entry.isDirectory) {
                                val innerExt = entry.name.substringAfterLast('.', "").lowercase()
                                if (ConsoleType.detectFromExtension(innerExt) != null) {
                                    val safeName = File(entry.name).name
                                    val outFile = File(cacheDir, safeName)
                                    FileOutputStream(outFile).use { fos ->
                                        zis.copyTo(fos)
                                    }
                                    return@withContext outFile
                                }
                            }
                            entry = zis.nextEntry
                        }
                    }
                }
            }

            val outFile = File(cacheDir, preferredFileName)
            if (!outFile.exists() || outFile.length() == 0L) {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(outFile).use { output ->
                        input.copyTo(output)
                    }
                } ?: return@withContext null
            }
            outFile
        }.getOrNull()
    }

    private fun inspectZipUri(uri: Uri, zipName: String, sizeBytes: Long): ScannedRomCandidate {
        var matchedConsole: ConsoleType? = null
        var innerName: String? = null
        var innerExt = "zip"
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                ZipInputStream(input).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory) {
                            val candidateExt = entry.name.substringAfterLast('.', "").lowercase()
                            val detected = ConsoleType.detectFromExtension(candidateExt, entry.size.coerceAtLeast(0L))
                            if (detected != null) {
                                matchedConsole = detected
                                innerName = File(entry.name).name
                                innerExt = candidateExt
                                break
                            }
                        }
                        entry = zis.nextEntry
                    }
                }
            }
        }
        return ScannedRomCandidate(
            uriString = uri.toString(),
            fileName = zipName,
            title = cleanGameTitle(innerName ?: zipName),
            extension = innerExt,
            sizeBytes = sizeBytes,
            crc32 = computeQuickCrc32(uri),
            detectedConsole = matchedConsole,
            isZipArchive = true,
            innerRomName = innerName,
            isSupported = matchedConsole != null,
            reason = if (matchedConsole == null) "ZIP contains no supported ROM" else null
        )
    }

    private fun resolveFileName(uri: Uri): String {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) return cursor.getString(idx) ?: "unknown_rom"
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/') ?: "unknown_rom"
    }

    private fun resolveFileSize(uri: Uri): Long {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (idx >= 0) return cursor.getLong(idx)
            }
        }
        return 0L
    }

    private fun computeQuickCrc32(uri: Uri): String {
        return runCatching {
            val crc = CRC32()
            val buf = ByteArray(16384)
            var totalRead = 0L
            context.contentResolver.openInputStream(uri)?.use { input ->
                while (totalRead < 2L * 1024L * 1024L) {
                    val read = input.read(buf)
                    if (read <= 0) break
                    crc.update(buf, 0, read)
                    totalRead += read
                }
            }
            crc.value.toString(16).uppercase().padStart(8, '0')
        }.getOrDefault("00000000")
    }

    private fun cleanGameTitle(fileName: String): String {
        return fileName
            .substringBeforeLast('.')
            .replace(Regex("\\([^)]*\\)"), "")
            .replace(Regex("\\[[^]]*\\]"), "")
            .replace('_', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()
            .ifBlank { fileName }
    }
}
