package com.pocketstudio.app.audit

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.text.TextLayoutResult
import com.pocketstudio.ui.WholeWordsAutoSize
import kotlin.math.abs
import kotlin.math.max

/**
 * Tour 6, lot 5 (R1: « Batteri / e ▸ », « Snar / e », « Claquem / ent de … »): every text laid out under a tagged
 * screen, read on Compose's own layout: a word split over two lines, a line ellipsized or lost under the box.
 */
internal object CutWords {
    data class Cut(val text: String, val tags: List<String>, val lines: Int, val box: String = "") {
        override fun toString() = "« $text » ($lines lines$box, in ${tags.take(3).joinToString(" < ")})"
    }

    /** The texts under the node tagged [root] that are not whole ([WholeWordsAutoSize.fits]). */
    fun under(rule: ComposeTestRule, root: String): List<Cut> {
        rule.waitForIdle()
        val nodes = rule.onAllNodes(hasAnyAncestor(hasTestTag(root)) and SemanticsMatcher.keyIsDefined(SemanticsProperties.Text), useUnmergedTree = true)
            .fetchSemanticsNodes(atLeastOneRootRequired = false)
        return nodes.mapNotNull { n ->
            val text = n.config[SemanticsProperties.Text].joinToString(" ") { it.text }
            if (text.isBlank()) return@mapNotNull null
            val r = layout(n) ?: return@mapNotNull null
            if (WholeWordsAutoSize.fits(r, text)) null else Cut(text, tags(n), r.lineCount, box(r))
        }
    }

    private fun layout(n: SemanticsNode): TextLayoutResult? {
        val get = n.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action ?: return null
        val list = mutableListOf<TextLayoutResult>()
        return if (get(list)) list.firstOrNull() else null
    }

    /** Its box and size, to see why: « 40 x 19 dp at 8.0 sp ». */
    private fun box(r: TextLayoutResult): String {
        val d = r.layoutInput.density
        return ", %.0f x %.0f dp at %.1f sp".format(r.size.width / d.density, r.size.height / d.density, r.layoutInput.style.fontSize.value)
    }

    /** The test tags of [n] and its ancestors, nearest first. */
    fun tags(n: SemanticsNode): List<String> {
        val out = mutableListOf<String>()
        var p: SemanticsNode? = n
        while (p != null) {
            p.config.getOrNull(SemanticsProperties.TestTag)?.let(out::add)
            p = p.parent
        }
        return out
    }
}

/**
 * Tour 6, lot 5 (R19: a row cut at the screen's edge without a sign that it goes on): how far the pixels of column
 * [x] of [image], between [fromY] and [toY] (shares of its height), are from [ground] on average (0 = the ground
 * itself: faded away; content shows above some 0.02).
 */
internal fun edgeDistance(image: PixelMap, x: Int, ground: Color, fromY: Float = 0.3f, toY: Float = 0.7f): Float {
    val y0 = (image.height * fromY).toInt()
    val y1 = (image.height * toY).toInt().coerceAtLeast(y0 + 1)
    var sum = 0f
    for (y in y0 until y1) {
        val c = image[x, y]
        sum += max(abs(c.red - ground.red), max(abs(c.green - ground.green), abs(c.blue - ground.blue)))
    }
    return sum / (y1 - y0)
}
