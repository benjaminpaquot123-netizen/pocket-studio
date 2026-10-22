package com.pocketstudio.ui

import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.text.modifiers.TextAutoSizeLayoutScope
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp

/**
 * A label that never cuts a word in two (« Importe / ren », « Cymbal / e »): on a narrow screen
 * (a small phone, a big interface size, a long Dutch word) its font shrinks, down to [minScale] of
 * its size, until every line ends between words and the text fits its [maxLines].
 */
@Composable
fun WholeWordsText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    maxLines: Int = 2,
    minScale: Float = 0.72f,
) {
    val resolved = LocalTextStyle.current.merge(style)
    Text(
        text, modifier, color = color, fontWeight = fontWeight, textAlign = textAlign, style = resolved,
        maxLines = maxLines, overflow = TextOverflow.Ellipsis,
        autoSize = if (resolved.fontSize.isSpecified) WholeWords(resolved.fontSize, minScale) else null,
    )
}

/** The [TextAutoSize] behind [WholeWordsText]: the largest size, by half points, that cuts no word. */
class WholeWords(private val maxFontSize: TextUnit, private val minScale: Float) : TextAutoSize {
    override fun TextAutoSizeLayoutScope.getFontSize(constraints: Constraints, text: AnnotatedString): TextUnit {
        val max = maxFontSize.value
        val min = max * minScale
        var size = max
        while (size > min) {
            val layout = performLayout(constraints, text, size.sp)
            if (!layout.hasVisualOverflow && !splitsAWord(layout, text.text)) return size.sp
            size -= 0.5f
        }
        return min.sp
    }

    override fun equals(other: Any?) = other is WholeWords && other.maxFontSize == maxFontSize && other.minScale == minScale
    override fun hashCode() = maxFontSize.hashCode() * 31 + minScale.hashCode()
}

/** A line of [layout] ends inside a word (a letter on each side of the break). */
fun splitsAWord(layout: TextLayoutResult, text: String): Boolean {
    for (line in 0 until layout.lineCount - 1) {
        val end = layout.getLineEnd(line)
        if (end <= 0 || end >= text.length) continue
        if (text[end - 1].isLetterOrDigit() && text[end].isLetterOrDigit()) return true
    }
    return false
}
