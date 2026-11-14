package com.pocketstudio.ui

import androidx.compose.animation.core.animate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import kotlin.math.abs
import kotlin.math.sign

/**
 * A readout whose figures roll to their new value, as on a studio display, instead of jumping:
 * only the figures that changed move (up when the number grows, down when it drops), in about
 * 200 ms. At rest it is a plain [Text] (same look, same semantics); while it rolls, TalkBack and
 * tests still read [text] whole. For values that change now and then (a tempo, a bar, a level
 * read once), not for one that follows a finger.
 */
@Composable
fun RollingText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = LocalTextStyle.current,
    textAlign: TextAlign? = null,
    maxLines: Int = 1,
    softWrap: Boolean = false,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    val reduced = LocalReducedMotion.current
    val roll = remember { Roll(text) }
    val progress = remember { mutableFloatStateOf(1f) }
    var rolling by remember { mutableStateOf(false) }
    if (text != roll.to) {
        roll.from = roll.to
        roll.to = text
        roll.up = rollsUp(roll.from, text)
        // Set while composing: the frame that shows the new value already starts the roll.
        if (!reduced && roll.from.isNotEmpty()) {
            progress.floatValue = 0f
            rolling = true
        }
    }
    LaunchedEffect(text) {
        if (progress.floatValue < 1f) {
            animate(0f, 1f, animationSpec = StudioMotion.spatial()) { v, _ -> progress.floatValue = v }
        }
        rolling = false
    }
    if (!rolling) {
        Text(text, modifier, color = color, style = style, textAlign = textAlign, maxLines = maxLines, softWrap = softWrap, overflow = overflow)
        return
    }
    val from = roll.from
    val to = roll.to
    val sign = if (roll.up) 1f else -1f
    // Kept whole on both sides of what changed (as few pieces of text as possible).
    val same = from.length == to.length
    val head = if (same) from.commonPrefixWith(to).length else 0
    val tail = if (same) minOf(from.commonSuffixWith(to).length, to.length - head) else 0
    val arrangement = when (textAlign) {
        TextAlign.End, TextAlign.Right -> Arrangement.End
        TextAlign.Center -> Arrangement.Center
        else -> Arrangement.Start
    }
    Row(
        modifier.clearAndSetSemantics { this.text = AnnotatedString(text) },
        horizontalArrangement = arrangement,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (head > 0) Text(to.substring(0, head), color = color, style = style, maxLines = 1, softWrap = false)
        // In between, the figures that changed roll and the others (a comma) stay put.
        var i = head
        val end = to.length - tail
        while (i < end) {
            val changed = !same || from[i] != to[i]
            var j = i + 1
            while (j < end && (!same || from[j] != to[j]) == changed) j++
            if (changed) RollingSlot(from.substring(i, if (same) j else from.length), to.substring(i, j), color, style) { progress.floatValue * sign }
            else Text(to.substring(i, j), color = color, style = style, maxLines = 1, softWrap = false)
            i = j
        }
        if (tail > 0) Text(to.substring(to.length - tail), color = color, style = style, maxLines = 1, softWrap = false)
    }
}

/** [from] leaving and [to] coming in; [move] is the progress, negative when the number drops. */
@Composable
private fun RollingSlot(from: String, to: String, color: Color, style: TextStyle, move: () -> Float) {
    Box(Modifier.clipToBounds()) {
        // The new figures come in from below (above when the number drops), the old ones leave.
        Text(
            to, color = color, style = style, maxLines = 1, softWrap = false,
            modifier = Modifier.graphicsLayer {
                val m = move()
                val p = abs(m)
                translationY = (1f - p) * size.height * sign(m)
                alpha = p
                compositingStrategy = CompositingStrategy.ModulateAlpha
            },
        )
        Text(
            from, color = color, style = style, maxLines = 1, softWrap = false,
            modifier = Modifier.matchParentSize().graphicsLayer {
                val m = move()
                val p = abs(m)
                translationY = -p * size.height * sign(m)
                alpha = 1f - p
                compositingStrategy = CompositingStrategy.ModulateAlpha
            },
        )
    }
}

/** What a [RollingText] rolls from and to (not state: a new [RollingText] text recomposes it anyway). */
private class Roll(text: String) {
    var from = text
    var to = text
    var up = true
}

private val NUMBER = Regex("""-?\d+(?:[.,]\d+)?""")

/** Up when the first number of [to] is bigger than that of [from] (or when there is none to compare). */
internal fun rollsUp(from: String, to: String): Boolean {
    fun first(s: String) = NUMBER.find(s.replace('−', '-'))?.value?.replace(',', '.')?.toDoubleOrNull()
    val a = first(from) ?: return true
    val b = first(to) ?: return true
    return b >= a
}
