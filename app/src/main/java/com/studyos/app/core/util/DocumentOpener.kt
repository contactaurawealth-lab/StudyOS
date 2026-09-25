package com.studyos.app.core.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

object DocumentOpener {

    /**
     * Resolves the human-readable display name of a Uri.
     */
    fun getDisplayName(context: Context, uri: Uri): String {
        if (uri.scheme == "file") {
            return uri.lastPathSegment ?: "document"
        }
        var name: String? = null
        try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) {
                        name = cursor.getString(index)
                    }
                }
            }
        } catch (_: Exception) {
            // Best effort resolution
        }
        return name ?: uri.lastPathSegment ?: "document"
    }

    /**
     * Determines whether the given Uri represents a Markdown document (.md, .markdown).
     */
    fun isMarkdown(context: Context, uri: Uri): Boolean {
        val fileName = getDisplayName(context, uri).lowercase()
        if (fileName.endsWith(".md") || fileName.endsWith(".markdown") || fileName.endsWith(".mdown")) {
            return true
        }
        val mime = resolveMimeType(context, uri).lowercase()
        return mime.contains("markdown")
    }

    /**
     * Determines whether the given path/string represents a Markdown document.
     */
    fun isMarkdownPath(pathOrUri: String): Boolean {
        val clean = pathOrUri.substringBefore('?').substringBefore('#').lowercase()
        return clean.endsWith(".md") || clean.endsWith(".markdown") || clean.endsWith(".mdown")
    }

    /**
     * Resolves an accurate MIME type for a given Uri.
     */
    fun resolveMimeType(context: Context, uri: Uri): String {
        val contentResolverType = try {
            context.contentResolver.getType(uri)
        } catch (_: Exception) {
            null
        }
        if (!contentResolverType.isNullOrBlank() && contentResolverType != "*/*") {
            return contentResolverType
        }

        val fileName = getDisplayName(context, uri)
        val ext = fileName.substringAfterLast('.', "").lowercase()
        if (ext.isNotEmpty()) {
            val fromMap = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
            if (!fromMap.isNullOrBlank()) {
                return fromMap
            }
            return when (ext) {
                "pdf" -> "application/pdf"
                "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                "doc" -> "application/msword"
                "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
                "ppt" -> "application/vnd.ms-powerpoint"
                "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                "xls" -> "application/vnd.ms-excel"
                "txt" -> "text/plain"
                "md", "markdown" -> "text/markdown"
                "html", "htm" -> "text/html"
                "epub" -> "application/epub+zip"
                else -> "*/*"
            }
        }
        return "*/*"
    }

    /**
     * Opens a document Uri in an external application (Google Drive, Docs, Word, Acrobat, etc.).
     * Returns true if successfully started, false otherwise.
     */
    fun openInExternalApp(context: Context, uri: Uri, customMime: String? = null): Boolean {
        return try {
            val mimeType = customMime ?: resolveMimeType(context, uri)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, "Open with").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(chooser)
            true
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "No app available on device to open this document", Toast.LENGTH_LONG).show()
            false
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open in external app: ${e.localizedMessage ?: "Unknown error"}", Toast.LENGTH_SHORT).show()
            false
        }
    }

    /**
     * Opens any resource string (http/https link, content:// URI, or file path).
     * If http/https, launches browser.
     * If content://, launches external app viewer.
     * If file path, creates FileProvider Uri and launches external viewer.
     */
    fun openResource(
        context: Context,
        uriOrPath: String,
        type: String? = null,
        onOpenMarkdownInApp: () -> Unit = {}
    ): Boolean {
        if (uriOrPath.isBlank()) return false

        // 1. Web link
        if (uriOrPath.startsWith("http://", ignoreCase = true) || uriOrPath.startsWith("https://", ignoreCase = true)) {
            return try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriOrPath)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                true
            } catch (e: Exception) {
                Toast.makeText(context, "Could not open link: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                false
            }
        }

        // 2. Markdown file -> keep in StudyOS
        if (isMarkdownPath(uriOrPath) || type.equals("MARKDOWN", ignoreCase = true)) {
            onOpenMarkdownInApp()
            return true
        }

        // 3. content:// URI
        if (uriOrPath.startsWith("content://", ignoreCase = true)) {
            val uri = Uri.parse(uriOrPath)
            if (isMarkdown(context, uri)) {
                onOpenMarkdownInApp()
                return true
            }
            return openInExternalApp(context, uri)
        }

        // 4. Local file path
        try {
            val file = File(if (uriOrPath.startsWith("file://")) uriOrPath.removePrefix("file://") else uriOrPath)
            if (file.exists()) {
                val contentUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                return openInExternalApp(context, contentUri)
            }
        } catch (e: Exception) {
            // Fall through to content URI parsing
        }

        // 5. Fallback try parsing as generic Uri
        return try {
            val uri = Uri.parse(uriOrPath)
            openInExternalApp(context, uri)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Smart opener for selected file Uris:
     * - Markdown (.md, .markdown) files open inside StudyOS!
     * - PDF, DOCX, DOC, PPT, XLS, etc. open in the other external app (Drive, Docs, Acrobat, etc.)
     */
    fun openDocumentSmart(
        context: Context,
        uri: Uri,
        onOpenInApp: () -> Unit
    ) {
        if (isMarkdown(context, uri)) {
            // Markdown opens in StudyOS!
            onOpenInApp()
        } else {
            // PDFs, DOCs, etc. open in the other external app
            val launched = openInExternalApp(context, uri)
            if (!launched) {
                // If no external viewer app was found on device, fallback to in-app viewer
                onOpenInApp()
            }
        }
    }
}
