package com.studyos.app.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studyos.app.theme.StudyOSTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private sealed class MarkdownBlock {
    data class Header(val level: Int, val text: String) : MarkdownBlock()
    data class CodeBlock(val language: String, val code: String) : MarkdownBlock()
    data class EquationBlock(val equation: String) : MarkdownBlock()
    data class BulletList(val items: List<String>) : MarkdownBlock()
    data class NumberedList(val items: List<String>) : MarkdownBlock()
    data class BlockQuote(val text: String) : MarkdownBlock()
    data class Paragraph(val text: String) : MarkdownBlock()
}

@Composable
fun StudyOSMarkdown(
    content: String,
    modifier: Modifier = Modifier
) {
    val blocks = remember(content) { parseMarkdown(content) }

    SelectionContainer(modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth()) {
            blocks.forEachIndexed { index, block ->
                when (block) {
                    is MarkdownBlock.Header -> MarkdownHeader(block)
                    is MarkdownBlock.CodeBlock -> MarkdownCodeBlock(block)
                    is MarkdownBlock.EquationBlock -> MarkdownEquation(block)
                    is MarkdownBlock.BulletList -> MarkdownBulletList(block)
                    is MarkdownBlock.NumberedList -> MarkdownNumberedList(block)
                    is MarkdownBlock.BlockQuote -> MarkdownBlockQuote(block)
                    is MarkdownBlock.Paragraph -> MarkdownParagraph(block)
                }

                if (index < blocks.lastIndex) {
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun MarkdownHeader(header: MarkdownBlock.Header) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography

    val style = when (header.level) {
        1 -> typography.subsectionTitle.copy(fontWeight = FontWeight.Bold)
        2 -> typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 17.sp)
        else -> typography.body.copy(fontWeight = FontWeight.SemiBold)
    }

    Text(
        text = header.text,
        style = style,
        color = colors.primaryText,
        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
    )
}

@Composable
private fun MarkdownCodeBlock(codeBlock: MarkdownBlock.CodeBlock) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    var isCopied by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.surface)
            .background(colors.surface)
            .border(1.dp, colors.border, shapes.surface)
    ) {
        Column {
            // Header: Language name + Copy button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.border.copy(alpha = 0.35f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = codeBlock.language.ifBlank { "code" },
                    style = typography.caption.copy(fontFamily = FontFamily.Monospace),
                    color = colors.secondaryText
                )

                Row(
                    modifier = Modifier.clickable {
                        clipboardManager.setText(AnnotatedString(codeBlock.code))
                        isCopied = true
                        coroutineScope.launch {
                            delay(2000)
                            isCopied = false
                        }
                    },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isCopied) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                        contentDescription = "Copy code",
                        tint = if (isCopied) colors.accent else colors.mutedText,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isCopied) "Copied" else "Copy",
                        style = typography.caption,
                        color = if (isCopied) colors.accent else colors.mutedText
                    )
                }
            }

            // Code Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(12.dp)
            ) {
                Text(
                    text = codeBlock.code,
                    style = typography.body.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    ),
                    color = colors.primaryText
                )
            }
        }
    }
}

@Composable
private fun MarkdownEquation(equation: MarkdownBlock.EquationBlock) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.surface)
            .background(colors.surface)
            .border(1.dp, colors.border, shapes.surface)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = equation.equation,
            style = typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontStyle = FontStyle.Italic,
                letterSpacing = 0.5.sp
            ),
            color = colors.primaryText
        )
    }
}

@Composable
private fun MarkdownBulletList(list: MarkdownBlock.BulletList) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography

    Column(modifier = Modifier.fillMaxWidth()) {
        list.items.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "•",
                    style = typography.bodyMedium,
                    color = colors.accent,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = formatInlineMarkdown(item),
                    style = typography.body,
                    color = colors.primaryText
                )
            }
        }
    }
}

@Composable
private fun MarkdownNumberedList(list: MarkdownBlock.NumberedList) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography

    Column(modifier = Modifier.fillMaxWidth()) {
        list.items.forEachIndexed { index, item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "${index + 1}.",
                    style = typography.caption.copy(fontWeight = FontWeight.Bold),
                    color = colors.secondaryText,
                    modifier = Modifier.padding(end = 8.dp, top = 2.dp)
                )
                Text(
                    text = formatInlineMarkdown(item),
                    style = typography.body,
                    color = colors.primaryText
                )
            }
        }
    }
}

@Composable
private fun MarkdownBlockQuote(quote: MarkdownBlock.BlockQuote) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(20.dp)
                .background(colors.accent)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = formatInlineMarkdown(quote.text),
            style = typography.body.copy(fontStyle = FontStyle.Italic),
            color = colors.secondaryText
        )
    }
}

@Composable
private fun MarkdownParagraph(paragraph: MarkdownBlock.Paragraph) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography

    Text(
        text = formatInlineMarkdown(paragraph.text),
        style = typography.body.copy(lineHeight = 22.sp),
        color = colors.primaryText
    )
}

@Composable
private fun formatInlineMarkdown(text: String): AnnotatedString {
    val colors = StudyOSTheme.colors

    return buildAnnotatedString {
        var cursor = 0
        val length = text.length

        while (cursor < length) {
            when {
                // Inline code `code`
                text.startsWith("`", cursor) && !text.startsWith("```", cursor) -> {
                    val endIdx = text.indexOf("`", cursor + 1)
                    if (endIdx != -1) {
                        val codeText = text.substring(cursor + 1, endIdx)
                        pushStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                background = colors.border.copy(alpha = 0.4f),
                                fontSize = 13.sp
                            )
                        )
                        append(" $codeText ")
                        pop()
                        cursor = endIdx + 1
                    } else {
                        append(text[cursor])
                        cursor++
                    }
                }
                // Bold **text**
                text.startsWith("**", cursor) -> {
                    val endIdx = text.indexOf("**", cursor + 2)
                    if (endIdx != -1) {
                        val boldText = text.substring(cursor + 2, endIdx)
                        pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                        append(boldText)
                        pop()
                        cursor = endIdx + 2
                    } else {
                        append(text[cursor])
                        cursor++
                    }
                }
                // Italic *text*
                text.startsWith("*", cursor) -> {
                    val endIdx = text.indexOf("*", cursor + 1)
                    if (endIdx != -1) {
                        val italicText = text.substring(cursor + 1, endIdx)
                        pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                        append(italicText)
                        pop()
                        cursor = endIdx + 1
                    } else {
                        append(text[cursor])
                        cursor++
                    }
                }
                else -> {
                    append(text[cursor])
                    cursor++
                }
            }
        }
    }
}

private fun parseMarkdown(rawContent: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val lines = rawContent.lines()
    var idx = 0

    while (idx < lines.size) {
        val line = lines[idx]
        val trimmed = line.trim()

        when {
            // Code block
            trimmed.startsWith("```") -> {
                val language = trimmed.removePrefix("```").trim()
                val codeLines = mutableListOf<String>()
                idx++
                while (idx < lines.size && !lines[idx].trim().startsWith("```")) {
                    codeLines.add(lines[idx])
                    idx++
                }
                blocks.add(MarkdownBlock.CodeBlock(language, codeLines.joinToString("\n")))
                idx++ // skip closing ```
            }
            // Block equation $$ ... $$
            trimmed.startsWith("$$") -> {
                if (trimmed.endsWith("$$") && trimmed.length > 4) {
                    blocks.add(MarkdownBlock.EquationBlock(trimmed.removePrefix("$$").removeSuffix("$$").trim()))
                    idx++
                } else {
                    val eqLines = mutableListOf<String>()
                    val firstLine = trimmed.removePrefix("$$").trim()
                    if (firstLine.isNotBlank()) eqLines.add(firstLine)
                    idx++
                    while (idx < lines.size && !lines[idx].trim().endsWith("$$")) {
                        eqLines.add(lines[idx])
                        idx++
                    }
                    if (idx < lines.size) {
                        val lastLine = lines[idx].trim().removeSuffix("$$").trim()
                        if (lastLine.isNotBlank()) eqLines.add(lastLine)
                        idx++
                    }
                    blocks.add(MarkdownBlock.EquationBlock(eqLines.joinToString("\n")))
                }
            }
            // Headers
            trimmed.startsWith("### ") -> {
                blocks.add(MarkdownBlock.Header(3, trimmed.removePrefix("### ")))
                idx++
            }
            trimmed.startsWith("## ") -> {
                blocks.add(MarkdownBlock.Header(2, trimmed.removePrefix("## ")))
                idx++
            }
            trimmed.startsWith("# ") -> {
                blocks.add(MarkdownBlock.Header(1, trimmed.removePrefix("# ")))
                idx++
            }
            // Bullet list
            trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                val listItems = mutableListOf<String>()
                while (idx < lines.size) {
                    val itemLine = lines[idx].trim()
                    if (itemLine.startsWith("- ") || itemLine.startsWith("* ")) {
                        listItems.add(itemLine.substring(2).trim())
                        idx++
                    } else {
                        break
                    }
                }
                blocks.add(MarkdownBlock.BulletList(listItems))
            }
            // Numbered list
            trimmed.matches(Regex("^\\d+\\.\\s+.*")) -> {
                val listItems = mutableListOf<String>()
                while (idx < lines.size) {
                    val itemLine = lines[idx].trim()
                    val match = Regex("^\\d+\\.\\s+(.*)").find(itemLine)
                    if (match != null) {
                        listItems.add(match.groupValues[1])
                        idx++
                    } else {
                        break
                    }
                }
                blocks.add(MarkdownBlock.NumberedList(listItems))
            }
            // Blockquote
            trimmed.startsWith("> ") -> {
                blocks.add(MarkdownBlock.BlockQuote(trimmed.removePrefix("> ")))
                idx++
            }
            // Blank lines
            trimmed.isEmpty() -> {
                idx++
            }
            // Paragraph
            else -> {
                val pLines = mutableListOf<String>()
                while (idx < lines.size) {
                    val pLine = lines[idx].trim()
                    if (pLine.isEmpty() || pLine.startsWith("#") || pLine.startsWith("```") ||
                        pLine.startsWith("$$") || pLine.startsWith("- ") || pLine.startsWith("* ") ||
                        pLine.matches(Regex("^\\d+\\.\\s+.*")) || pLine.startsWith("> ")
                    ) {
                        break
                    }
                    pLines.add(lines[idx])
                    idx++
                }
                blocks.add(MarkdownBlock.Paragraph(pLines.joinToString(" ")))
            }
        }
    }

    return blocks
}
