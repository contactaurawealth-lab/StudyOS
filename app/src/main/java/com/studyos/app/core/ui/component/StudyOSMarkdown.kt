package com.studyos.app.core.ui.component

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.PriorityHigh
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studyos.app.theme.StudyOSTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class AlertType(val title: String, val color: Color, val icon: ImageVector) {
    NOTE("NOTE", Color(0xFF3B82F6), Icons.Outlined.Info),
    TIP("TIP", Color(0xFF10B981), Icons.Outlined.Lightbulb),
    IMPORTANT("IMPORTANT", Color(0xFF8B5CF6), Icons.Outlined.PriorityHigh),
    WARNING("WARNING", Color(0xFFF59E0B), Icons.Outlined.Warning),
    CAUTION("CAUTION", Color(0xFFEF4444), Icons.Outlined.Warning)
}

data class ChecklistItem(val isChecked: Boolean, val text: String)
data class MarkdownListItem(val text: String, val level: Int = 0)

private sealed class MarkdownBlock {
    data class Header(val level: Int, val text: String) : MarkdownBlock()
    data class CodeBlock(val language: String, val code: String) : MarkdownBlock()
    data class EquationBlock(val equation: String) : MarkdownBlock()
    data class AlertCallout(val type: AlertType, val content: String) : MarkdownBlock()
    data class TableBlock(val headers: List<String>, val rows: List<List<String>>) : MarkdownBlock()
    data class BulletList(val items: List<MarkdownListItem>) : MarkdownBlock()
    data class NumberedList(val items: List<MarkdownListItem>) : MarkdownBlock()
    data class Checklist(val items: List<ChecklistItem>) : MarkdownBlock()
    data class BlockQuote(val text: String) : MarkdownBlock()
    object HorizontalRule : MarkdownBlock()
    data class Paragraph(val text: String) : MarkdownBlock()
}

@Composable
fun StudyOSMarkdown(
    content: String,
    modifier: Modifier = Modifier,
    onWikiLinkClick: (String) -> Unit = {}
) {
    val blocks = remember(content) { parseMarkdown(content) }

    SelectionContainer(modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth()) {
            blocks.forEachIndexed { index, block ->
                when (block) {
                    is MarkdownBlock.Header -> MarkdownHeader(block)
                    is MarkdownBlock.CodeBlock -> MarkdownCodeBlock(block)
                    is MarkdownBlock.EquationBlock -> MarkdownEquation(block)
                    is MarkdownBlock.AlertCallout -> MarkdownAlertCallout(block, onWikiLinkClick)
                    is MarkdownBlock.TableBlock -> MarkdownTable(block, onWikiLinkClick)
                    is MarkdownBlock.BulletList -> MarkdownBulletList(block, onWikiLinkClick)
                    is MarkdownBlock.NumberedList -> MarkdownNumberedList(block, onWikiLinkClick)
                    is MarkdownBlock.Checklist -> MarkdownChecklist(block, onWikiLinkClick)
                    is MarkdownBlock.BlockQuote -> MarkdownBlockQuote(block, onWikiLinkClick)
                    is MarkdownBlock.HorizontalRule -> MarkdownHorizontalRule()
                    is MarkdownBlock.Paragraph -> MarkdownParagraph(block, onWikiLinkClick)
                }

                if (index < blocks.lastIndex) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun MarkdownHeader(header: MarkdownBlock.Header) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography

    val (style, topPad, bottomPad) = when (header.level) {
        1 -> Triple(typography.sectionTitle.copy(fontWeight = FontWeight.Bold, fontSize = 22.sp), 12.dp, 4.dp)
        2 -> Triple(typography.subsectionTitle.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp), 10.dp, 4.dp)
        3 -> Triple(typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp), 8.dp, 2.dp)
        4 -> Triple(typography.body.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp), 6.dp, 2.dp)
        5 -> Triple(typography.body.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp), 4.dp, 2.dp)
        else -> Triple(typography.caption.copy(fontWeight = FontWeight.Bold, fontSize = 12.5.sp), 4.dp, 2.dp)
    }

    Column(modifier = Modifier.padding(top = topPad, bottom = bottomPad)) {
        Text(
            text = header.text,
            style = style,
            color = colors.primaryText
        )
        if (header.level <= 2) {
            Spacer(modifier = Modifier.height(4.dp))
            HorizontalDivider(
                thickness = 1.dp,
                color = colors.border.copy(alpha = if (header.level == 1) 0.5f else 0.25f)
            )
        }
    }
}

@Composable
private fun MarkdownAlertCallout(
    alert: MarkdownBlock.AlertCallout,
    onWikiLinkClick: (String) -> Unit = {}
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.surface)
            .background(alert.type.color.copy(alpha = 0.08f))
            .border(1.dp, alert.type.color.copy(alpha = 0.35f), shapes.surface)
            .padding(14.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = alert.type.icon,
                    contentDescription = null,
                    tint = alert.type.color,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = alert.type.title,
                    style = typography.caption.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = alert.type.color
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = formatInlineMarkdown(alert.content, onWikiLinkClick),
                style = typography.body.copy(lineHeight = 20.sp),
                color = colors.primaryText
            )
        }
    }
}

@Composable
private fun MarkdownTable(
    table: MarkdownBlock.TableBlock,
    onWikiLinkClick: (String) -> Unit = {}
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.surface)
            .background(colors.surface)
            .border(1.dp, colors.border, shapes.surface)
            .horizontalScroll(rememberScrollState())
    ) {
        Column {
            // Header Row
            if (table.headers.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .background(colors.accent.copy(alpha = 0.12f))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    table.headers.forEachIndexed { i, header ->
                        Box(
                            modifier = Modifier
                                .width(140.dp)
                                .padding(end = 8.dp)
                        ) {
                            Text(
                                text = formatInlineMarkdown(header, onWikiLinkClick),
                                style = typography.secondary.copy(fontWeight = FontWeight.Bold),
                                color = colors.accent
                            )
                        }
                    }
                }
                HorizontalDivider(thickness = 1.dp, color = colors.border)
            }

            // Data Rows
            table.rows.forEachIndexed { rowIndex, row ->
                val bg = if (rowIndex % 2 == 1) colors.border.copy(alpha = 0.1f) else Color.Transparent
                Row(
                    modifier = Modifier
                        .background(bg)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    row.forEachIndexed { colIndex, cell ->
                        Box(
                            modifier = Modifier
                                .width(140.dp)
                                .padding(end = 8.dp)
                        ) {
                            Text(
                                text = formatInlineMarkdown(cell, onWikiLinkClick),
                                style = typography.secondary,
                                color = colors.primaryText
                            )
                        }
                    }
                }
                if (rowIndex < table.rows.lastIndex) {
                    HorizontalDivider(thickness = 0.5.dp, color = colors.border.copy(alpha = 0.3f))
                }
            }
        }
    }
}

@Composable
private fun MarkdownChecklist(
    checklist: MarkdownBlock.Checklist,
    onWikiLinkClick: (String) -> Unit = {}
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        checklist.items.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = if (item.isChecked) Icons.Outlined.CheckBox else Icons.Outlined.CheckBoxOutlineBlank,
                    contentDescription = null,
                    tint = if (item.isChecked) colors.accent else colors.mutedText,
                    modifier = Modifier
                        .size(18.dp)
                        .padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formatInlineMarkdown(item.text, onWikiLinkClick),
                    style = typography.body.copy(
                        color = if (item.isChecked) colors.mutedText else colors.primaryText,
                        textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None
                    )
                )
            }
        }
    }
}

@Composable
private fun MarkdownHorizontalRule() {
    val colors = StudyOSTheme.colors
    HorizontalDivider(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        thickness = 1.dp,
        color = colors.border.copy(alpha = 0.5f)
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
                    text = codeBlock.language.ifBlank { "code" }.uppercase(),
                    style = typography.caption.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold),
                    color = colors.accent
                )

                Row(
                    modifier = Modifier
                        .clip(shapes.small)
                        .clickable {
                            clipboardManager.setText(AnnotatedString(codeBlock.code))
                            isCopied = true
                            coroutineScope.launch {
                                delay(2000)
                                isCopied = false
                            }
                        }
                        .padding(horizontal = 6.dp, vertical = 2.dp),
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

fun formatLatexMath(raw: String): String {
    var s = raw
    val greek = mapOf(
        "\\alpha" to "α", "\\beta" to "β", "\\gamma" to "γ", "\\delta" to "δ",
        "\\epsilon" to "ε", "\\zeta" to "ζ", "\\eta" to "η", "\\theta" to "θ",
        "\\iota" to "ι", "\\kappa" to "κ", "\\lambda" to "λ", "\\mu" to "μ",
        "\\nu" to "ν", "\\xi" to "ξ", "\\pi" to "π", "\\rho" to "ρ",
        "\\sigma" to "σ", "\\tau" to "τ", "\\upsilon" to "υ", "\\phi" to "φ",
        "\\chi" to "χ", "\\psi" to "ψ", "\\omega" to "ω",
        "\\Gamma" to "Γ", "\\Delta" to "Δ", "\\Theta" to "Θ", "\\Lambda" to "Λ",
        "\\Sigma" to "Σ", "\\Phi" to "Φ", "\\Psi" to "Ψ", "\\Omega" to "Ω"
    )
    greek.forEach { (k, v) -> s = s.replace(k, v) }
    val symbols = mapOf(
        "\\times" to "×", "\\div" to "÷", "\\pm" to "±", "\\mp" to "∓",
        "\\leq" to "≤", "\\geq" to "≥", "\\neq" to "≠", "\\approx" to "≈",
        "\\equiv" to "≡", "\\infty" to "∞", "\\int" to "∫", "\\sum" to "∑",
        "\\prod" to "∏", "\\sqrt" to "√", "\\rightarrow" to "→", "\\leftarrow" to "←",
        "\\leftrightarrow" to "↔", "\\Rightarrow" to "⇒", "\\Leftarrow" to "⇐",
        "\\partial" to "∂", "\\nabla" to "∇", "\\in" to "∈", "\\notin" to "∉",
        "\\subset" to "⊂", "\\subseteq" to "⊆", "\\cap" to "∩", "\\cup" to "∪",
        "\\emptyset" to "∅", "\\forall" to "∀", "\\exists" to "∃", "\\cdot" to "·",
        "\\degree" to "°"
    )
    symbols.forEach { (k, v) -> s = s.replace(k, v) }
    s = Regex("\\\\frac\\{([^}]+)\\}\\{([^}]+)\\}").replace(s) { m -> "(${m.groupValues[1]} / ${m.groupValues[2]})" }
    s = Regex("\\\\sqrt\\{([^}]+)\\}").replace(s) { m -> "√(${m.groupValues[1]})" }
    val supers = mapOf('0' to '⁰', '1' to '¹', '2' to '²', '3' to '³', '4' to '⁴', '5' to '⁵', '6' to '⁶', '7' to '⁷', '8' to '⁸', '9' to '⁹', '+' to '⁺', '-' to '⁻', '=' to '⁼', '(' to '⁽', ')' to '⁾', 'n' to 'ⁿ', 'x' to 'ˣ', 't' to 'ᵗ', 'y' to 'ʸ')
    s = Regex("\\^([0-9+\\-()nxty])").replace(s) { m -> supers[m.groupValues[1][0]]?.toString() ?: m.value }
    val subs = mapOf('0' to '₀', '1' to '₁', '2' to '₂', '3' to '₃', '4' to '₄', '5' to '₅', '6' to '₆', '7' to '₇', '8' to '₈', '9' to '₉', '+' to '₊', '-' to '₋', '=' to '₌', '(' to '₍', ')' to '₎', 'i' to 'ᵢ', 'j' to 'ⱼ', 'k' to 'ₖ', 'n' to 'ₙ', 'x' to 'ₓ', 'y' to 'ᵧ')
    s = Regex("_([0-9+\\-()ijknxy])").replace(s) { m -> subs[m.groupValues[1][0]]?.toString() ?: m.value }
    return s
}

@Composable
private fun MarkdownEquation(equation: MarkdownBlock.EquationBlock) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes
    val formatted = remember(equation.equation) { formatLatexMath(equation.equation) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.surface)
            .background(colors.surface)
            .border(1.dp, colors.accent.copy(alpha = 0.35f), shapes.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = formatted,
            style = typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontStyle = FontStyle.Italic,
                letterSpacing = 0.8.sp,
                fontSize = 15.sp
            ),
            color = colors.primaryText
        )
    }
}

@Composable
private fun MarkdownBulletList(
    list: MarkdownBlock.BulletList,
    onWikiLinkClick: (String) -> Unit = {}
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography

    Column(modifier = Modifier.fillMaxWidth()) {
        list.items.forEach { item ->
            val bullet = when (item.level % 3) {
                0 -> "•"
                1 -> "◦"
                else -> "▪"
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = (item.level * 16).dp, top = 2.dp, bottom = 2.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = bullet,
                    style = typography.bodyMedium,
                    color = colors.accent,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = formatInlineMarkdown(item.text, onWikiLinkClick),
                    style = typography.body.copy(lineHeight = 22.sp),
                    color = colors.primaryText,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MarkdownNumberedList(
    list: MarkdownBlock.NumberedList,
    onWikiLinkClick: (String) -> Unit = {}
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography

    Column(modifier = Modifier.fillMaxWidth()) {
        list.items.forEachIndexed { index, item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = (item.level * 16).dp, top = 2.dp, bottom = 2.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "${index + 1}.",
                    style = typography.caption.copy(fontWeight = FontWeight.Bold),
                    color = colors.accent,
                    modifier = Modifier.padding(end = 8.dp, top = 2.dp)
                )
                Text(
                    text = formatInlineMarkdown(item.text, onWikiLinkClick),
                    style = typography.body.copy(lineHeight = 22.sp),
                    color = colors.primaryText,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MarkdownBlockQuote(
    quote: MarkdownBlock.BlockQuote,
    onWikiLinkClick: (String) -> Unit = {}
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.surface)
            .background(colors.surface.copy(alpha = 0.6f))
            .border(1.dp, colors.border.copy(alpha = 0.4f), shapes.surface)
            .padding(vertical = 8.dp, horizontal = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .height(28.dp)
                    .clip(CircleShape)
                    .background(colors.accent)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = formatInlineMarkdown(quote.text, onWikiLinkClick),
                style = typography.body.copy(fontStyle = FontStyle.Italic, lineHeight = 21.sp),
                color = colors.secondaryText
            )
        }
    }
}

@Composable
private fun MarkdownParagraph(
    paragraph: MarkdownBlock.Paragraph,
    onWikiLinkClick: (String) -> Unit = {}
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val annotated = formatInlineMarkdown(paragraph.text, onWikiLinkClick)

    Text(
        text = annotated,
        style = typography.body.copy(lineHeight = 22.sp, color = colors.primaryText)
    )
}

@Composable
private fun formatInlineMarkdown(
    text: String,
    onWikiLinkClick: ((String) -> Unit)? = null
): AnnotatedString {
    val colors = StudyOSTheme.colors
    val context = LocalContext.current

    return buildAnnotatedString {
        var cursor = 0
        val length = text.length

        while (cursor < length) {
            when {
                // WikiLinks [[Target]] or [[Target|Label]]
                text.startsWith("[[", cursor) -> {
                    val endIdx = text.indexOf("]]", cursor + 2)
                    if (endIdx != -1) {
                        val rawInside = text.substring(cursor + 2, endIdx)
                        val parts = rawInside.split("|")
                        val target = parts[0].trim()
                        val label = if (parts.size > 1) parts[1].trim() else target

                        val linkAnnotation = LinkAnnotation.Clickable(
                            tag = target,
                            styles = TextLinkStyles(
                                style = SpanStyle(
                                    color = colors.accent,
                                    fontWeight = FontWeight.SemiBold,
                                    textDecoration = TextDecoration.Underline
                                )
                            ),
                            linkInteractionListener = {
                                onWikiLinkClick?.invoke(target)
                            }
                        )
                        pushLink(linkAnnotation)
                        append(label)
                        pop()
                        cursor = endIdx + 2
                    } else {
                        append(text[cursor])
                        cursor++
                    }
                }
                // Standard Markdown Links [Label](url)
                text.startsWith("[", cursor) -> {
                    val labelEnd = text.indexOf("]", cursor + 1)
                    val isLink = labelEnd != -1 && labelEnd + 1 < length && text[labelEnd + 1] == '('
                    if (isLink) {
                        val urlEnd = text.indexOf(")", labelEnd + 2)
                        if (urlEnd != -1) {
                            val label = text.substring(cursor + 1, labelEnd)
                            val url = text.substring(labelEnd + 2, urlEnd).trim()

                            val linkAnnotation = LinkAnnotation.Clickable(
                                tag = url,
                                styles = TextLinkStyles(
                                    style = SpanStyle(
                                        color = colors.accent,
                                        fontWeight = FontWeight.Medium,
                                        textDecoration = TextDecoration.Underline
                                    )
                                ),
                                linkInteractionListener = {
                                    try {
                                        if (url.startsWith("http://", true) || url.startsWith("https://", true)) {
                                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                            context.startActivity(browserIntent)
                                        } else if (url.endsWith(".pdf", true)) {
                                            com.studyos.app.core.util.DocumentOpener.openPdfInExternalApp(context, url, label)
                                        } else {
                                            onWikiLinkClick?.invoke(url)
                                        }
                                    } catch (_: Exception) {}
                                }
                            )
                            pushLink(linkAnnotation)
                            append(label)
                            pop()
                            cursor = urlEnd + 1
                        } else {
                            append(text[cursor])
                            cursor++
                        }
                    } else {
                        append(text[cursor])
                        cursor++
                    }
                }
                // Inline code `code`
                text.startsWith("`", cursor) && !text.startsWith("```", cursor) -> {
                    val endIdx = text.indexOf("`", cursor + 1)
                    if (endIdx != -1) {
                        val codeText = text.substring(cursor + 1, endIdx)
                        pushStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                background = colors.border.copy(alpha = 0.4f),
                                color = colors.accent,
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
                // Inline math $formula$
                text.startsWith("$", cursor) && !text.startsWith("$$", cursor) -> {
                    val endIdx = text.indexOf("$", cursor + 1)
                    if (endIdx != -1 && endIdx > cursor + 1) {
                        val formulaText = text.substring(cursor + 1, endIdx)
                        val formatted = formatLatexMath(formulaText)
                        pushStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                fontStyle = FontStyle.Italic,
                                color = colors.accent,
                                fontWeight = FontWeight.Medium
                            )
                        )
                        append(" $formatted ")
                        pop()
                        cursor = endIdx + 1
                    } else {
                        append(text[cursor])
                        cursor++
                    }
                }
                // Strikethrough ~~text~~
                text.startsWith("~~", cursor) -> {
                    val endIdx = text.indexOf("~~", cursor + 2)
                    if (endIdx != -1) {
                        val strikeText = text.substring(cursor + 2, endIdx)
                        pushStyle(
                            SpanStyle(
                                textDecoration = TextDecoration.LineThrough,
                                color = colors.mutedText
                            )
                        )
                        append(strikeText)
                        pop()
                        cursor = endIdx + 2
                    } else {
                        append(text[cursor])
                        cursor++
                    }
                }
                // Bold italic ***text***
                text.startsWith("***", cursor) -> {
                    val endIdx = text.indexOf("***", cursor + 3)
                    if (endIdx != -1) {
                        val boldItalicText = text.substring(cursor + 3, endIdx)
                        pushStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic))
                        append(boldItalicText)
                        pop()
                        cursor = endIdx + 3
                    } else {
                        append(text[cursor])
                        cursor++
                    }
                }
                // Bold **text** or __text__
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
                text.startsWith("__", cursor) -> {
                    val endIdx = text.indexOf("__", cursor + 2)
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
                // Italic *text* or _text_
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
                text.startsWith("_", cursor) && (cursor == 0 || text[cursor - 1].isWhitespace()) -> {
                    val endIdx = text.indexOf("_", cursor + 1)
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
            // Horizontal Rule ---, ***, ___
            trimmed.matches(Regex("^(---+|\\*\\*\\*+|___+)$")) -> {
                blocks.add(MarkdownBlock.HorizontalRule)
                idx++
            }

            // Code block ```
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

            // GitHub Alert Callouts: > [!NOTE], > [!TIP], > [!IMPORTANT], > [!WARNING], > [!CAUTION]
            trimmed.startsWith("> [!") -> {
                val alertMatch = Regex("^>\\s*\\[!(NOTE|TIP|IMPORTANT|WARNING|CAUTION)\\]", RegexOption.IGNORE_CASE).find(trimmed)
                if (alertMatch != null) {
                    val typeStr = alertMatch.groupValues[1].uppercase()
                    val alertType = AlertType.valueOf(typeStr)
                    val alertLines = mutableListOf<String>()
                    val remainingOnFirstLine = trimmed.substring(alertMatch.range.last + 1).trim()
                    if (remainingOnFirstLine.isNotBlank()) {
                        alertLines.add(remainingOnFirstLine)
                    }
                    idx++
                    while (idx < lines.size && lines[idx].trim().startsWith(">")) {
                        alertLines.add(lines[idx].trim().removePrefix(">").trim())
                        idx++
                    }
                    blocks.add(MarkdownBlock.AlertCallout(alertType, alertLines.joinToString(" ")))
                } else {
                    blocks.add(MarkdownBlock.BlockQuote(trimmed.removePrefix(">").trim()))
                    idx++
                }
            }

            // Standard Blockquote >
            trimmed.startsWith("> ") -> {
                val quoteLines = mutableListOf<String>()
                while (idx < lines.size && lines[idx].trim().startsWith("> ")) {
                    quoteLines.add(lines[idx].trim().removePrefix("> ").trim())
                    idx++
                }
                blocks.add(MarkdownBlock.BlockQuote(quoteLines.joinToString(" ")))
            }

            // Headers H1 to H6
            trimmed.startsWith("###### ") -> {
                blocks.add(MarkdownBlock.Header(6, trimmed.removePrefix("###### ")))
                idx++
            }
            trimmed.startsWith("##### ") -> {
                blocks.add(MarkdownBlock.Header(5, trimmed.removePrefix("##### ")))
                idx++
            }
            trimmed.startsWith("#### ") -> {
                blocks.add(MarkdownBlock.Header(4, trimmed.removePrefix("#### ")))
                idx++
            }
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

            // GFM Table: | Header 1 | Header 2 |
            trimmed.startsWith("|") && trimmed.endsWith("|") && idx + 1 < lines.size && lines[idx + 1].trim().matches(Regex("^\\|\\s*[-:]+[-| :]*\\|$")) -> {
                val headerRow = trimmed.split("|").filter { it.isNotBlank() }.map { it.trim() }
                idx += 2 // skip header row and divider row (e.g. |---|---|)
                val rows = mutableListOf<List<String>>()
                while (idx < lines.size && lines[idx].trim().startsWith("|") && lines[idx].trim().endsWith("|")) {
                    val cells = lines[idx].trim().split("|").filter { it.isNotBlank() }.map { it.trim() }
                    rows.add(cells)
                    idx++
                }
                blocks.add(MarkdownBlock.TableBlock(headerRow, rows))
            }

            // Checklist - [ ] or - [x]
            trimmed.matches(Regex("^[*-]\\s+\\[[ xX]\\]\\s+.*")) -> {
                val checkItems = mutableListOf<ChecklistItem>()
                while (idx < lines.size) {
                    val itemLine = lines[idx].trim()
                    val match = Regex("^[*-]\\s+\\[([ xX])\\]\\s+(.*)").find(itemLine)
                    if (match != null) {
                        val isChecked = match.groupValues[1].equals("x", ignoreCase = true)
                        val text = match.groupValues[2].trim()
                        checkItems.add(ChecklistItem(isChecked, text))
                        idx++
                    } else {
                        break
                    }
                }
                blocks.add(MarkdownBlock.Checklist(checkItems))
            }

            // Bullet list
            trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                val listItems = mutableListOf<MarkdownListItem>()
                while (idx < lines.size) {
                    val rawLine = lines[idx]
                    val itemLine = rawLine.trim()
                    if (itemLine.matches(Regex("^[*-]\\s+\\[[ xX]\\].*"))) break
                    if (itemLine.startsWith("- ") || itemLine.startsWith("* ")) {
                        val indent = rawLine.takeWhile { it == ' ' || it == '\t' }.length
                        val level = (indent / 2).coerceIn(0, 4)
                        listItems.add(MarkdownListItem(itemLine.substring(2).trim(), level))
                        idx++
                    } else {
                        break
                    }
                }
                blocks.add(MarkdownBlock.BulletList(listItems))
            }

            // Numbered list
            trimmed.matches(Regex("^\\d+\\.\\s+.*")) -> {
                val listItems = mutableListOf<MarkdownListItem>()
                while (idx < lines.size) {
                    val rawLine = lines[idx]
                    val itemLine = rawLine.trim()
                    val match = Regex("^\\d+\\.\\s+(.*)").find(itemLine)
                    if (match != null) {
                        val indent = rawLine.takeWhile { it == ' ' || it == '\t' }.length
                        val level = (indent / 2).coerceIn(0, 4)
                        listItems.add(MarkdownListItem(match.groupValues[1], level))
                        idx++
                    } else {
                        break
                    }
                }
                blocks.add(MarkdownBlock.NumberedList(listItems))
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
                        pLine.matches(Regex("^\\d+\\.\\s+.*")) || pLine.startsWith(">") ||
                        pLine.matches(Regex("^(---+|\\*\\*\\*+|___+)$")) ||
                        (pLine.startsWith("|") && pLine.endsWith("|"))
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
