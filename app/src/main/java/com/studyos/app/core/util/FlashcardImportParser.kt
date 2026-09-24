package com.studyos.app.core.util

import android.content.Context
import android.net.Uri

data class ParsedCard(
    val question: String,
    val answer: String
)

object FlashcardImportParser {

    /**
     * Parses bulk flashcard data from raw text.
     * Supports:
     * - Anki TSV (tab-separated)
     * - Standard CSV (comma-separated with quote support)
     * - Semicolon-separated text
     * - Ignores empty lines and comments (#)
     */
    fun parseText(rawText: String): List<ParsedCard> {
        val lines = rawText.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("#") }

        if (lines.isEmpty()) return emptyList()

        // Delimiter detection
        val tabCount = lines.sumOf { line -> line.count { it == '\t' } }
        val semiCount = lines.sumOf { line -> line.count { it == ';' } }
        val commaCount = lines.sumOf { line -> line.count { it == ',' } }

        val delimiter = when {
            tabCount >= lines.size * 0.4 -> '\t'
            semiCount >= lines.size * 0.4 -> ';'
            commaCount >= lines.size * 0.4 -> ','
            else -> '\t'
        }

        return lines.mapNotNull { line ->
            parseLine(line, delimiter)
        }
    }

    private fun parseLine(line: String, delimiter: Char): ParsedCard? {
        val parts = if (delimiter == ',') {
            parseCsvLine(line)
        } else {
            line.split(delimiter).map { it.trim() }
        }

        if (parts.size >= 2) {
            val q = parts[0].trim().removeSurrounding("\"").replace("\\n", "\n")
            val a = parts[1].trim().removeSurrounding("\"").replace("\\n", "\n")
            if (q.isNotBlank() && a.isNotBlank()) {
                return ParsedCard(question = q, answer = a)
            }
        }
        return null
    }

    private fun parseCsvLine(line: String): List<String> {
        val tokens = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '\"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '\"') {
                        sb.append('\"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                c == ',' && !inQuotes -> {
                    tokens.add(sb.toString().trim())
                    sb.clear()
                }
                else -> sb.append(c)
            }
            i++
        }
        tokens.add(sb.toString().trim())
        return tokens
    }

    fun parseFromUri(context: Context, uri: Uri): List<ParsedCard> {
        return try {
            val content = context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader(Charsets.UTF_8).readText()
            } ?: ""
            parseText(content)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
