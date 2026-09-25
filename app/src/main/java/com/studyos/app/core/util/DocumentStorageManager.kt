package com.studyos.app.core.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.UUID

data class StoredDocument(
    val file: File,
    val originalFileName: String,
    val cleanTitle: String,
    val extension: String,
    val sizeBytes: Long,
    val mimeType: String,
    val contentUri: Uri,
    val persistentPath: String
) {
    val formattedSize: String
        get() = DocumentStorageManager.formatFileSize(sizeBytes)
}

object DocumentStorageManager {

    private const val DOCUMENTS_DIR = "documents"

    /**
     * Copies any Uri (content://, file://, etc.) to internal storage under filesDir/documents/.
     * This provides 100% offline persistence, survives restarts, and prevents Android SecurityException.
     */
    suspend fun saveDocumentLocally(context: Context, sourceUri: Uri): StoredDocument = withContext(Dispatchers.IO) {
        val originalName = resolveFileName(context, sourceUri) ?: "document_${System.currentTimeMillis()}"
        val extension = originalName.substringAfterLast('.', "").lowercase()
        val cleanTitle = originalName.substringBeforeLast('.').ifBlank { "Document" }

        val docsDir = File(context.filesDir, DOCUMENTS_DIR).apply {
            if (!exists()) mkdirs()
        }

        // Sanitize filename to avoid filesystem issues
        val sanitizedBase = cleanTitle.replace("[^a-zA-Z0-9._-]".toRegex(), "_").take(40)
        val uniqueSuffix = UUID.randomUUID().toString().take(8)
        val targetFileName = if (extension.isNotEmpty()) "${sanitizedBase}_${uniqueSuffix}.$extension" else "${sanitizedBase}_${uniqueSuffix}"
        val targetFile = File(docsDir, targetFileName)

        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            FileOutputStream(targetFile).use { output ->
                input.copyTo(output)
            }
        } ?: throw IllegalStateException("Could not open input stream for: $sourceUri")

        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            targetFile
        )

        val mimeType = DocumentOpener.resolveMimeType(context, sourceUri)

        StoredDocument(
            file = targetFile,
            originalFileName = originalName,
            cleanTitle = cleanTitle,
            extension = extension,
            sizeBytes = targetFile.length(),
            mimeType = mimeType,
            contentUri = contentUri,
            persistentPath = targetFile.absolutePath
        )
    }

    /**
     * Formats bytes into human-readable string (e.g., 2.4 MB, 180 KB).
     */
    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        return when {
            mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
            kb >= 1.0 -> String.format(Locale.US, "%.1f KB", kb)
            else -> "$bytes B"
        }
    }

    fun getShareableUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    private fun resolveFileName(context: Context, uri: Uri): String? {
        if (uri.scheme == "file") {
            return uri.lastPathSegment
        }
        return try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) cursor.getString(index) else null
                } else null
            }
        } catch (_: Exception) {
            uri.lastPathSegment
        }
    }
}
