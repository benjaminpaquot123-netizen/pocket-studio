package com.pocketstudio.ui

import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.text.modifiers.TextAutoSizeLayoutScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Lot D2 (« règle partout »: no word cut on a small phone, in any of the 14 languages): the text
 * gets smaller, step by step down to [min], until it fits in its lines with every word whole (a
 * word never goes over two lines, « Monté/e »); only below [min] is it cut with « … ».
 */
class WholeWordsAutoSize(private val min: TextUnit, private val max: TextUnit, private val step: TextUnit = 0.5.sp) : TextAutoSize {
    override fun TextAutoSizeLayoutScope.getFontSize(constraints: Constraints, text: AnnotatedString): TextUnit {
        val top = if (max.isSp) max.value else 14f
        val bottom = if (min.isSp) min.value.coerceAtMost(top) else top
        var size = top
        while (size > bottom) {
            if (fits(performLayout(constraints, text, size.sp), text.text)) return size.sp
            size -= if (step.isSp) step.value.coerceAtLeast(0.25f) else 0.5f
        }
        return bottom.sp
    }

    override fun equals(other: Any?): Boolean = other is WholeWordsAutoSize && other.min == min && other.max == max && other.step == step
    override fun hashCode(): Int = (min.hashCode() * 31 + max.hashCode()) * 31 + step.hashCode()

    companion object {
        /** [r] shows [text] whole: no line past the last, nothing wider than its box, no word split over two lines. */
        fun fits(r: TextLayoutResult, text: String): Boolean {
            if (r.didOverflowHeight || r.isLineEllipsized(r.lineCount - 1)) return false
            // A single line that does not wrap: its full width against the box (didOverflowWidth says
            // « overflow » for a short text laid out narrower than its box).
            if (r.lineCount == 1 && r.multiParagraph.maxIntrinsicWidth > r.size.width + 0.5f) return false
            for (line in 1 until r.lineCount) {
                val at = r.getLineStart(line)
                if (at in 1 until text.length && !text[at - 1].isWhitespace() && !text[at].isWhitespace() && text[at - 1] != '-' &&
                    !breaksAnywhere(text[at - 1]) && !breaksAnywhere(text[at])
                ) return false
            }
            return true
        }

        /**
         * Chinese and Japanese go to the next line between any two characters (no spaces between words);
         * so does Korean on Android (its line breaker splits between syllables: audit stage 1, Korean
         * texts were made smaller for a break Korean readers expect).
         */
        private fun breaksAnywhere(c: Char): Boolean = when (Character.UnicodeScript.of(c.code)) {
            Character.UnicodeScript.HAN, Character.UnicodeScript.HIRAGANA, Character.UnicodeScript.KATAKANA, Character.UnicodeScript.HANGUL -> true
            else -> c.code in 0x3000..0x30FF || c.code in 0xFF00..0xFFEF
        }
    }
}

/**
 * The one text that must fit on small screens (a pad's name, a button's word, a page's title):
 * [maxLines] lines at most, words kept whole, smaller down to [min] before anything is cut.
 */
@Composable
fun FitText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    maxLines: Int = 1,
    min: TextUnit = 9.sp,
    textAlign: TextAlign? = null,
) {
    // The line height follows the size (a fixed one would leave no room for two smaller lines).
    val lines = if (style.lineHeight.isSp && style.fontSize.isSp && style.fontSize.value > 0f) {
        style.copy(lineHeight = (style.lineHeight.value / style.fontSize.value).em)
    } else {
        style
    }
    Text(
        text, modifier, style = lines, color = color, fontWeight = fontWeight, maxLines = maxLines, softWrap = maxLines > 1,
        overflow = TextOverflow.Ellipsis, textAlign = textAlign, autoSize = WholeWordsAutoSize(min, style.fontSize),
    )
}

/**
 * Audit stage 1: content taller than the room it is given (a small tile with three lines, a big
 * font size) is drawn whole and smaller, centred, instead of losing its last line under the edge.
 * Content that fits is untouched.
 */
fun Modifier.shrinkToFit(): Modifier = this.then(
    Modifier.layout { measurable, constraints ->
        val placeable = measurable.measure(constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity))
        val room = if (constraints.hasBoundedHeight) constraints.maxHeight else placeable.height
        val scale = if (placeable.height > room && placeable.height > 0) room.toFloat() / placeable.height else 1f
        val height = minOf(placeable.height, room).coerceAtLeast(constraints.minHeight)
        layout(placeable.width, height) {
            if (scale == 1f) placeable.place(0, (height - placeable.height) / 2)
            else placeable.placeWithLayer(0, (height - placeable.height) / 2) { scaleX = scale; scaleY = scale }
        }
    },
)
