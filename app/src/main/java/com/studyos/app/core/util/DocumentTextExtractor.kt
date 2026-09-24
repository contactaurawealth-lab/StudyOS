package com.studyos.app.core.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.zip.InflaterInputStream
import java.util.zip.ZipInputStream

data class ExtractedDocument(
    val title: String,
    val content: String,
    val fileType: String,
    val wordCount: Int,
    val characterCount: Int
)

object DocumentTextExtractor {

    fun extract(context: Context, uri: Uri, fallbackTitle: String = "Uploaded Note"): ExtractedDocument {
        val fileName = resolveFileName(context, uri) ?: fallbackTitle
        val extension = fileName.substringAfterLast('.', "").lowercase()

        val rawText = try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                when (extension) {
                    "pdf" -> extractFromPdf(inputStream, fileName)
                    "docx" -> extractFromDocx(inputStream)
                    "doc" -> extractFromDoc(inputStream)
                    "txt", "md", "markdown", "text", "csv", "tsv", "json", "xml", "html", "htm", "rtf", "log" ->
                        extractFromPlainText(inputStream)
                    else -> extractWithBestEffort(inputStream)
                }
            } ?: ""
        } catch (e: Exception) {
            "Failed to extract document: ${e.localizedMessage ?: "Unknown error"}"
        }

        val cleanedText = cleanExtractedText(rawText)
        val words = if (cleanedText.isBlank()) 0 else cleanedText.split("\\s+".toRegex()).count { it.isNotBlank() }

        return ExtractedDocument(
            title = fileName.substringBeforeLast('.').ifBlank { fallbackTitle },
            content = cleanedText,
            fileType = extension.uppercase().ifBlank { "TEXT" },
            wordCount = words,
            characterCount = cleanedText.length
        )
    }

    private fun resolveFileName(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    cursor.getString(nameIndex)
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun extractFromPlainText(inputStream: InputStream): String {
        return BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
            reader.readText()
        }
    }

    /**
     * Extracts text from Microsoft Word .docx files.
     * .docx is an open packaging convention (ZIP file) containing `word/document.xml`.
     */
    private fun extractFromDocx(inputStream: InputStream): String {
        val textBuilder = StringBuilder()
        ZipInputStream(inputStream).use { zipStream ->
            var entry = zipStream.nextEntry
            while (entry != null) {
                if (entry.name == "word/document.xml") {
                    val xmlContent = zipStream.bufferedReader(Charsets.UTF_8).readText()
                    // Extract text between <w:t> tags and respect paragraph breaks <w:p>
                    val paragraphs = xmlContent.split("<w:p[ >]".toRegex())
                    for (p in paragraphs) {
                        val textTokens = Regex("<w:t[^>]*>(.*?)</w:t>").findAll(p)
                        val paragraphText = textTokens.joinToString("") { it.groupValues[1] }
                        if (paragraphText.isNotBlank()) {
                            textBuilder.append(paragraphText.trim()).append("\n\n")
                        }
                    }
                    break
                }
                entry = zipStream.nextEntry
            }
        }
        return textBuilder.toString()
    }

    /**
     * Legacy .doc binary format fallback
     */
    private fun extractFromDoc(inputStream: InputStream): String {
        val bytes = inputStream.readBytes()
        val textBuilder = StringBuilder()
        var consecutivePrintable = 0
        val currentWord = StringBuilder()

        for (b in bytes) {
            val c = b.toInt().toChar()
            if (c in ' '..'~' || c == '\n' || c == '\r' || c == '\t') {
                currentWord.append(c)
                consecutivePrintable++
            } else {
                if (consecutivePrintable >= 4) {
                    textBuilder.append(currentWord).append(" ")
                }
                currentWord.clear()
                consecutivePrintable = 0
            }
        }
        if (consecutivePrintable >= 4) {
            textBuilder.append(currentWord)
        }
        return textBuilder.toString()
    }

    /**
     * Pure Kotlin stream text extractor for PDF documents.
     * Decompresses Flate streams and decodes text operands (Tj, TJ, ET, BT).
     */
    private fun extractFromPdf(inputStream: InputStream, fileName: String): String {
        val bytes = inputStream.readBytes()
        val textBuilder = StringBuilder()

        // 1. Scan for stream ... endstream blocks in PDF
        val content = String(bytes, Charsets.ISO_8859_1)
        val streamPattern = Regex("stream[\\r\\n]+([\\s\\S]*?)[\\r\\n]+endstream")
        val matches = streamPattern.findAll(content)

        for (match in matches) {
            val streamData = match.groupValues[1]
            val streamBytes = streamData.toByteArray(Charsets.ISO_8859_1)

            // Attempt to decompress Flate stream
            val decodedBytes = try {
                InflaterInputStream(streamBytes.inputStream()).use { it.readBytes() }
            } catch (e: Exception) {
                streamBytes
            }

            val textStream = String(decodedBytes, Charsets.ISO_8859_1)
            extractPdfTextFromStream(textStream, textBuilder)
        }

        val result = textBuilder.toString().trim()
        return if (result.isNotBlank()) {
            result
        } else {
            // Fallback for non-standard PDF streams or scanned documents
            val printableWords = extractPrintableTokens(bytes)
            if (printableWords.isNotBlank() && printableWords.length > 50) {
                printableWords
            } else {
                "### Document: $fileName\n\n(PDF text stream is compressed or contains scanned images. Upload text notes or markdown alongside for best AI tutoring.)"
            }
        }
    }

    private fun extractPdfTextFromStream(streamText: String, builder: StringBuilder) {
        // Matches (Text) Tj or (Text) '
        val tjRegex = Regex("\\((.*?)\\)\\s*(?:Tj|')")
        for (match in tjRegex.findAll(streamText)) {
            val raw = unescapePdfString(match.groupValues[1])
            if (raw.isNotBlank()) {
                builder.append(raw).append(" ")
            }
        }

        // Matches [(Text) 12 (Text)] TJ
        val arrayTjRegex = Regex("\\[([^\\]]*)\\]\\s*TJ")
        for (match in arrayTjRegex.findAll(streamText)) {
            val inside = match.groupValues[1]
            val strMatches = Regex("\\((.*?)\\)").findAll(inside)
            for (sm in strMatches) {
                val raw = unescapePdfString(sm.groupValues[1])
                builder.append(raw)
            }
            builder.append(" ")
        }
        builder.append("\n")
    }

    private fun unescapePdfString(str: String): String {
        return str.replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .replace("\\(", "(")
            .replace("\\)", ")")
            .replace("\\\\", "\\")
    }

    private fun extractPrintableTokens(bytes: ByteArray): String {
        val sb = StringBuilder()
        var currentToken = StringBuilder()
        for (b in bytes) {
            val c = b.toInt().toChar()
            if (c in 'a'..'z' || c in 'A'..'Z' || c in '0'..'9' || c in " .,;:!?-\n\r") {
                currentToken.append(c)
            } else {
                if (currentToken.length >= 4) {
                    sb.append(currentToken).append(" ")
                }
                currentToken.clear()
            }
        }
        return sb.toString()
    }

    private fun extractWithBestEffort(inputStream: InputStream): String {
        val text = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        // Filter out binary garbage
        val printableRatio = text.count { it in ' '..'~' || it == '\n' || it == '\r' || it == '\t' }.toFloat() / text.length.coerceAtLeast(1)
        return if (printableRatio > 0.7f) {
            text
        } else {
            "Unsupported binary format."
        }
    }

    private fun cleanExtractedText(text: String): String {
        return text
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .replace(Regex("\n{3,}"), "\n\n")
            .replace(Regex(" {2,}"), " ")
            .trim()
    }
}
