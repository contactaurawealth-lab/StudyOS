package com.studyos.app.core.util

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

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
     * Determines whether the given Uri or path represents a PDF document.
     */
    fun isPdf(context: Context, uri: Uri): Boolean {
        val fileName = getDisplayName(context, uri).lowercase()
        if (fileName.endsWith(".pdf")) return true
        val mime = resolveMimeType(context, uri).lowercase()
        return mime == "application/pdf"
    }

    fun isPdfPath(pathOrUri: String): Boolean {
        val clean = pathOrUri.substringBefore('?').substringBefore('#').lowercase()
        return clean.endsWith(".pdf")
    }

    /**
     * Resolves an accurate MIME type for a given Uri.
     */
    fun resolveMimeType(context: Context, uri: Uri): String {
        val fileName = getDisplayName(context, uri).lowercase()
        if (fileName.endsWith(".pdf")) return "application/pdf"
        if (fileName.endsWith(".md") || fileName.endsWith(".markdown")) return "text/markdown"

        val contentResolverType = try {
            context.contentResolver.getType(uri)
        } catch (_: Exception) {
            null
        }
        if (!contentResolverType.isNullOrBlank() && contentResolverType != "*/*") {
            return contentResolverType
        }

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
     * Converts any path (file path, file:// URI, or content:// URI) into a safe,
     * shareable FileProvider URI so that external apps receive full read permissions.
     */
    fun getShareableContentUri(context: Context, uriOrPath: String): Pair<Uri, String>? {
        if (uriOrPath.isBlank()) return null

        val isPdf = isPdfPath(uriOrPath)

        // 1. Direct file path or file:// URI
        if (!uriOrPath.startsWith("content://", ignoreCase = true)) {
            val filePath = if (uriOrPath.startsWith("file://", ignoreCase = true)) {
                uriOrPath.removePrefix("file://")
            } else {
                uriOrPath
            }
            val file = File(filePath)
            if (file.exists()) {
                val contentUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                val mime = if (isPdf) "application/pdf" else resolveMimeType(context, contentUri)
                return Pair(contentUri, mime)
            }
        }

        // 2. content:// URI
        val parsedUri = Uri.parse(uriOrPath)
        val authority = parsedUri.authority
        if (authority == "${context.packageName}.fileprovider") {
            val mime = if (isPdf) "application/pdf" else resolveMimeType(context, parsedUri)
            return Pair(parsedUri, mime)
        }

        // 3. Foreign content:// URI (e.g. SAF picker) -> copy to cache preview folder so external apps can read it
        return try {
            val displayName = getDisplayName(context, parsedUri).ifBlank { "document.pdf" }
            val ext = displayName.substringAfterLast('.', if (isPdf) "pdf" else "bin")
            val cacheFolder = File(context.cacheDir, "shared_preview").apply { if (!exists()) mkdirs() }
            val tempFile = File(cacheFolder, "preview_${System.currentTimeMillis()}.$ext")
            context.contentResolver.openInputStream(parsedUri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            if (tempFile.exists() && tempFile.length() > 0) {
                val contentUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    tempFile
                )
                val mime = if (isPdf || ext.equals("pdf", ignoreCase = true)) "application/pdf" else resolveMimeType(context, contentUri)
                Pair(contentUri, mime)
            } else {
                val mime = if (isPdf) "application/pdf" else resolveMimeType(context, parsedUri)
                Pair(parsedUri, mime)
            }
        } catch (_: Exception) {
            val mime = if (isPdf) "application/pdf" else resolveMimeType(context, parsedUri)
            Pair(parsedUri, mime)
        }
    }

    /**
     * Opens a PDF in an external application (Google Drive PDF Viewer, Adobe Acrobat, Samsung Notes, etc.)
     * primarily as application/pdf with ClipData and URI read grants.
     */
    fun openPdfInExternalApp(context: Context, file: File, title: String? = null): Boolean {
        return openPdfInExternalApp(context, file.absolutePath, title)
    }

    fun openPdfInExternalApp(context: Context, uriOrPath: String, title: String? = null): Boolean {
        return try {
            val shareable = getShareableContentUri(context, uriOrPath)
            val uri = shareable?.first ?: Uri.parse(uriOrPath)
            val mimeType = "application/pdf"

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                clipData = ClipData.newRawUri(title ?: "PDF Document", uri)
            }

            // Explicitly grant read permission to all matched activities
            val resolvedActivities = context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            for (res in resolvedActivities) {
                val packageName = res.activityInfo.packageName
                try {
                    context.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (_: Exception) {}
            }

            val chooser = Intent.createChooser(intent, "Open PDF with").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(chooser)
            true
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "No PDF reader app found on device", Toast.LENGTH_LONG).show()
            false
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open PDF: ${e.localizedMessage ?: "Unknown error"}", Toast.LENGTH_SHORT).show()
            false
        }
    }

    /**
     * Opens a document Uri in an external application (Google Drive, Docs, Word, Acrobat, etc.).
     * Returns true if successfully started, false otherwise.
     */
    fun openInExternalApp(context: Context, uri: Uri, customMime: String? = null): Boolean {
        if (isPdf(context, uri) || isPdfPath(uri.toString()) || customMime == "application/pdf") {
            return openPdfInExternalApp(context, uri.toString(), getDisplayName(context, uri))
        }

        return try {
            val shareable = getShareableContentUri(context, uri.toString())
            val targetUri = shareable?.first ?: uri
            val mimeType = customMime ?: shareable?.second ?: resolveMimeType(context, targetUri)

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(targetUri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                clipData = ClipData.newRawUri("Document", targetUri)
            }

            val resolvedActivities = context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            for (res in resolvedActivities) {
                val packageName = res.activityInfo.packageName
                try {
                    context.grantUriPermission(packageName, targetUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (_: Exception) {}
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
     * If PDF, opens in external PDF viewer app primarily as PDF!
     * If Markdown, keeps inside StudyOS.
     * Otherwise, opens in external viewer app.
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

        // 2. PDF document -> open in other app as PDF primarily!
        if (isPdfPath(uriOrPath) || type.equals("PDF", ignoreCase = true)) {
            return openPdfInExternalApp(context, uriOrPath)
        }

        // 3. Markdown file -> keep in StudyOS
        if (isMarkdownPath(uriOrPath) || type.equals("MARKDOWN", ignoreCase = true)) {
            onOpenMarkdownInApp()
            return true
        }

        // 4. Content URI or file path -> open in external app
        val shareable = getShareableContentUri(context, uriOrPath)
        if (shareable != null) {
            return openInExternalApp(context, shareable.first, shareable.second)
        }
        val uri = Uri.parse(uriOrPath)
        return openInExternalApp(context, uri)
    }

    /**
     * Smart opener for selected file Uris:
     * - PDF files open in external PDF apps (Drive, Acrobat, etc.) primarily as PDF!
     * - Markdown (.md, .markdown) files open inside StudyOS!
     * - Other documents open in external viewer apps.
     */
    fun openDocumentSmart(
        context: Context,
        uri: Uri,
        onOpenInApp: () -> Unit
    ) {
        if (isPdf(context, uri) || isPdfPath(uri.toString())) {
            // PDFs open in external PDF app primarily!
            val launched = openPdfInExternalApp(context, uri.toString(), getDisplayName(context, uri))
            if (!launched) {
                // Fallback to in-app visual viewer if no external app found
                onOpenInApp()
            }
        } else if (isMarkdown(context, uri)) {
            // Markdown opens in StudyOS!
            onOpenInApp()
        } else {
            // DOCs, PPTs, etc. open in external apps
            val launched = openInExternalApp(context, uri)
            if (!launched) {
                onOpenInApp()
            }
        }
    }
}
