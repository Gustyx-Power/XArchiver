package id.xms.xarchiver.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

sealed class MarkdownBlock {
    data class Header(val level: Int, val text: String) : MarkdownBlock()
    data class ListItem(val bullet: String, val text: String) : MarkdownBlock()
    data class Blockquote(val text: String) : MarkdownBlock()
    data class Paragraph(val text: String) : MarkdownBlock()
    data object Divider : MarkdownBlock()
    data object Spacing : MarkdownBlock()
}

@Composable
fun MarkdownContent(
    markdown: String,
    modifier: Modifier = Modifier,
    textColor: Color = Color.Unspecified,
    accentColor: Color = Color(0xFF0070F0),
    secondaryColor: Color = Color.Gray,
    dividerColor: Color = Color.LightGray.copy(alpha = 0.5f),
    codeBgColor: Color = Color.White.copy(alpha = 0.1f),
    codeTextColor: Color = Color(0xFF93C5FD)
) {
    val blocks = remember(markdown) { parseMarkdownBlocks(markdown) }

    Column(modifier = modifier.fillMaxWidth()) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Header -> {
                    Spacer(modifier = Modifier.height(if (block.level <= 2) 14.dp else 10.dp))
                    val (fontSize, fontWeight, color) = when (block.level) {
                        1 -> Triple(19.sp, FontWeight.Bold, textColor)
                        2 -> Triple(17.sp, FontWeight.Bold, textColor)
                        3 -> Triple(15.sp, FontWeight.SemiBold, accentColor)
                        else -> Triple(14.sp, FontWeight.SemiBold, textColor)
                    }
                    Text(
                        text = parseMarkdownSpans(block.text, codeBgColor, codeTextColor, accentColor),
                        fontSize = fontSize,
                        fontWeight = fontWeight,
                        color = color,
                        lineHeight = (fontSize.value + 6).sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                is MarkdownBlock.ListItem -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        if (block.bullet == "•") {
                            Box(
                                modifier = Modifier
                                    .padding(top = 8.dp, start = 4.dp, end = 10.dp)
                                    .size(5.dp)
                                    .background(accentColor, CircleShape)
                            )
                        } else {
                            Text(
                                text = block.bullet,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentColor,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                        Text(
                            text = parseMarkdownSpans(block.text, codeBgColor, codeTextColor, accentColor),
                            fontSize = 14.5.sp,
                            lineHeight = 22.sp,
                            color = textColor
                        )
                    }
                }
                is MarkdownBlock.Blockquote -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .fillMaxHeight()
                                .background(accentColor.copy(alpha = 0.6f), RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = parseMarkdownSpans(block.text, codeBgColor, codeTextColor, accentColor),
                            fontSize = 14.sp,
                            lineHeight = 21.sp,
                            fontStyle = FontStyle.Italic,
                            color = secondaryColor
                        )
                    }
                }
                is MarkdownBlock.Divider -> {
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = dividerColor,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                }
                is MarkdownBlock.Spacing -> {
                    Spacer(modifier = Modifier.height(6.dp))
                }
                is MarkdownBlock.Paragraph -> {
                    Text(
                        text = parseMarkdownSpans(block.text, codeBgColor, codeTextColor, accentColor),
                        fontSize = 14.5.sp,
                        lineHeight = 22.sp,
                        color = textColor,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}

private fun parseMarkdownBlocks(markdown: String): List<MarkdownBlock> {
    val lines = markdown.replace("\r\n", "\n").replace('\r', '\n').split('\n')
    val blocks = mutableListOf<MarkdownBlock>()
    var previousWasSpacing = false

    for (rawLine in lines) {
        val line = rawLine.trim()

        if (line.isEmpty()) {
            if (!previousWasSpacing && blocks.isNotEmpty()) {
                blocks.add(MarkdownBlock.Spacing)
                previousWasSpacing = true
            }
            continue
        }
        previousWasSpacing = false

        when {
            line.startsWith("#### ") -> blocks.add(MarkdownBlock.Header(4, line.removePrefix("#### ").trim()))
            line.startsWith("### ") -> blocks.add(MarkdownBlock.Header(3, line.removePrefix("### ").trim()))
            line.startsWith("## ") -> blocks.add(MarkdownBlock.Header(2, line.removePrefix("## ").trim()))
            line.startsWith("# ") -> blocks.add(MarkdownBlock.Header(1, line.removePrefix("# ").trim()))
            line == "---" || line == "***" || line == "___" -> blocks.add(MarkdownBlock.Divider)
            line.startsWith("- ") || line.startsWith("* ") || line.startsWith("+ ") -> {
                blocks.add(MarkdownBlock.ListItem("•", line.substring(2).trim()))
            }
            line.matches(Regex("""^\d+\.\s+.*""")) -> {
                val match = Regex("""^(\d+\.)\s+(.*)""").find(line)
                if (match != null) {
                    blocks.add(MarkdownBlock.ListItem(match.groupValues[1], match.groupValues[2]))
                } else {
                    blocks.add(MarkdownBlock.Paragraph(line))
                }
            }
            line.startsWith("> ") -> blocks.add(MarkdownBlock.Blockquote(line.removePrefix("> ").trim()))
            else -> blocks.add(MarkdownBlock.Paragraph(line))
        }
    }

    return blocks
}

fun parseMarkdownSpans(
    text: String,
    codeBgColor: Color,
    codeTextColor: Color,
    linkColor: Color
): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        val len = text.length

        while (i < len) {
            // 1. Inline code: `code`
            if (text[i] == '`') {
                val end = text.indexOf('`', i + 1)
                if (end != -1) {
                    val code = text.substring(i + 1, end).trim()
                    pushStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            background = codeBgColor,
                            color = codeTextColor,
                            fontSize = 13.sp
                        )
                    )
                    append(" ")
                    append(code)
                    append(" ")
                    pop()
                    i = end + 1
                    continue
                }
            }

            // 2. Strikethrough: ~~strikethrough~~
            if (i + 1 < len && text[i] == '~' && text[i + 1] == '~') {
                val end = text.indexOf("~~", i + 2)
                if (end != -1) {
                    val content = text.substring(i + 2, end)
                    pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
                    append(content)
                    pop()
                    i = end + 2
                    continue
                }
            }

            // 3. Bold: **bold** or __bold__
            if (i + 1 < len && ((text[i] == '*' && text[i + 1] == '*') || (text[i] == '_' && text[i + 1] == '_'))) {
                val delim = text.substring(i, i + 2)
                val end = text.indexOf(delim, i + 2)
                if (end != -1) {
                    val content = text.substring(i + 2, end)
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    // If content has inline code inside, parse spans
                    append(parseMarkdownSpans(content, codeBgColor, codeTextColor, linkColor))
                    pop()
                    i = end + 2
                    continue
                }
            }

            // 4. Italic: *italic* or _italic_ (not preceded or followed by word boundary for underscore if desirable, but simple check works)
            if (text[i] == '*' || text[i] == '_') {
                val char = text[i]
                val end = text.indexOf(char, i + 1)
                if (end != -1 && end > i + 1 && text.getOrNull(end + 1) != char) {
                    val content = text.substring(i + 1, end)
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    append(content)
                    pop()
                    i = end + 1
                    continue
                }
            }

            // 5. Links: [title](url)
            if (text[i] == '[') {
                val closeBracket = text.indexOf(']', i + 1)
                if (closeBracket != -1 && closeBracket + 1 < len && text[closeBracket + 1] == '(') {
                    val closeParen = text.indexOf(')', closeBracket + 2)
                    if (closeParen != -1) {
                        val title = text.substring(i + 1, closeBracket)
                        pushStyle(
                            SpanStyle(
                                color = linkColor,
                                fontWeight = FontWeight.SemiBold,
                                textDecoration = TextDecoration.Underline
                            )
                        )
                        append(title)
                        pop()
                        i = closeParen + 1
                        continue
                    }
                }
            }

            append(text[i])
            i++
        }
    }
}
