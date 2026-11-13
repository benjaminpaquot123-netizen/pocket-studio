package com.pocketstudio.ui

import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.text.modifiers.TextAutoSizeLayoutScope
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.em
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
    val merged = LocalTextStyle.current.merge(style)
    // Audit ultra round 1, n° 38 (200 %: « Char… » on a 56 dp pad): a line height in sp stayed as tall when the words
    // shrank, two lines never fitted; as a share of the size it shrinks with them (the same at full size).
    // Past 130 % only (below, the words shrink little and the screens stay as they were).
    val big = androidx.compose.ui.platform.LocalDensity.current.fontScale > BIG_FONT_FROM
    val resolved = if (big && merged.lineHeight.isSp && merged.fontSize.isSp && merged.fontSize.value > 0f) {
        merged.copy(lineHeight = (merged.lineHeight.value / merged.fontSize.value).em)
    } else merged
    Text(
        text, modifier, color = color, fontWeight = fontWeight, textAlign = textAlign, style = resolved,
        maxLines = maxLines, overflow = TextOverflow.Ellipsis,
        autoSize = if (resolved.fontSize.isSpecified) WholeWords(resolved.fontSize, minScale) else null,
    )
}

/**
 * The same, its colour read only when drawn: a label whose colour moves (a pad lighting up and fading out) is
 * drawn again, never composed again (tour 5 « jank »). The same pixels as [Text] in that colour.
 */
@Composable
fun WholeWordsText(
    text: String,
    color: ColorProducer,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    textAlign: TextAlign? = null,
    maxLines: Int = 2,
    minScale: Float = 0.72f,
) {
    val merged = LocalTextStyle.current.merge(style)
    val big = androidx.compose.ui.platform.LocalDensity.current.fontScale > BIG_FONT_FROM
    val resolved = if (big && merged.lineHeight.isSp && merged.fontSize.isSp && merged.fontSize.value > 0f) {
        merged.copy(lineHeight = (merged.lineHeight.value / merged.fontSize.value).em)
    } else merged
    // As Material's Text: the style's colour, else the content's (the producer's colour drawn over it).
    val base = resolved.color.takeOrElse { androidx.compose.material3.LocalContentColor.current }
    BasicText(
        text, modifier, resolved.merge(color = base, textAlign = textAlign ?: TextAlign.Unspecified),
        overflow = TextOverflow.Ellipsis, maxLines = maxLines, color = color,
        autoSize = if (resolved.fontSize.isSpecified) WholeWords(resolved.fontSize, minScale) else null,
    )
}

/** The [TextAutoSize] behind [WholeWordsText]: the largest size, by half points, that cuts no word. */
class WholeWords(private val maxFontSize: TextUnit, private val minScale: Float) : TextAutoSize {
    override fun TextAutoSizeLayoutScope.getFontSize(constraints: Constraints, text: AnnotatedString): TextUnit {
        val max = maxFontSize.value
        // Audit ultra round 1, n° 38 (200 %: « Char… », « Écouter l'exempl »): past 130 % the words may shrink as far as
        // they would at 130 %, never further (a size in sp grows with the phone's font, its floor with it).
        val min = max * minScale / (fontScale / BIG_FONT_FROM).coerceAtLeast(1f)
        // Tour 5, moyens2 (seat A d, 200 %: 789 ms at the app's opening): the same half points, searched by halves.
        return largestFitting(max, min, 0.5f) { size ->
            val layout = performLayout(constraints, text, size.sp)
            !layout.hasVisualOverflow && !splitsAWord(layout, text.text)
        }?.sp ?: min.sp
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

/** The phone's font size from which [WholeWords] and [WholeWordsAutoSize] let the words shrink further (130 %). */
internal const val BIG_FONT_FROM = 1.3f

/**
 * The largest of [top], [top] − [step], [top] − 2 [step]… still above [bottom] for which [fits] holds (null: none), as a
 * search from [top] down, one step after the other, finds it, in far fewer layouts (tour 5, moyens2: up to 15 layouts a
 * label at 200 %, 168 for the first screen of seat A d). [top] first (a label that fits costs one layout), then by halves:
 * a text that fits at a size fits at a smaller one, so the first size that fits splits the steps in two.
 */
internal fun largestFitting(top: Float, bottom: Float, step: Float, fits: (Float) -> Boolean): Float? {
    if (!(top > bottom)) return null
    if (fits(top)) return top
    // The sizes as the step-by-step search made them (by repeated subtraction: the same floats).
    var n = 0
    var s = top
    while (s > bottom) { n++; s -= step }
    fun sizeAt(k: Int): Float { var v = top; repeat(k) { v -= step }; return v }
    var lo = 1
    var hi = n
    while (lo < hi) {
        val mid = (lo + hi) ushr 1
        if (fits(sizeAt(mid))) hi = mid else lo = mid + 1
    }
    return if (lo < n) sizeAt(lo) else null
}
