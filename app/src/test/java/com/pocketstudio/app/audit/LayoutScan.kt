package com.pocketstudio.app.audit

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.text.TextLayoutResult
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Audit stage 1 (docs/audits/AUDIT.md §4): what a screen shows wrong, read from Compose's own layout
 * instead of by eye, so that only the flagged captures need a look.
 *
 * - [Kind.CUT]: a text ellipsized or overflowing its own box (the words did not fit), or drawn past a
 *   clipping parent (a button, a card, the screen) along an axis that does not scroll;
 * - [Kind.OVERLAP]: two texts whose letters are drawn over each other;
 * - [Kind.TARGET]: something to touch smaller than 48 x 48 dp (its touch area, as TalkBack is given it);
 * - [Kind.LABEL]: something to touch that TalkBack would read as nothing (an icon without a description);
 * - [Kind.CONTRAST]: a text under WCAG AA against what is behind it (4.5:1, 3:1 from 18 sp or 14 sp bold),
 *   read on the rendered pixels.
 */
object LayoutScan {
    enum class Kind { CUT, OVERLAP, TARGET, LABEL, CONTRAST }

    data class Finding(val kind: Kind, val what: String, val detail: String) {
        override fun toString() = "$kind « $what » $detail"
    }

    private val ANY = SemanticsMatcher("any node") { true }

    /** Everything wrong on the screen [rule] shows now ([image]: the root's pixels, for the contrast). */
    fun scan(rule: ComposeTestRule, density: Float, checks: Set<Kind> = Kind.entries.toSet()): List<Finding> {
        rule.waitForIdle()
        val all = rule.onAllNodes(ANY, useUnmergedTree = true).fetchSemanticsNodes(atLeastOneRootRequired = false)
        val out = mutableListOf<Finding>()
        val texts = all.mapNotNull { n -> textOf(n)?.let { Txt(n, it, layout(n)) } }
        if (Kind.CUT in checks) texts.forEach { cut(it, density)?.let(out::add) }
        if (Kind.OVERLAP in checks) {
            out += overlaps(texts, density)
            out += overButtons(texts, all, density)
        }
        if (Kind.CONTRAST in checks) {
            // Each window (the screen, a dialog, a menu) on its own pixels.
            val roots = rule.onAllNodes(androidx.compose.ui.test.isRoot(), useUnmergedTree = true)
            val rootNodes = roots.fetchSemanticsNodes(atLeastOneRootRequired = false)
            val pixels = HashMap<Int, androidx.compose.ui.graphics.PixelMap>()
            rootNodes.forEachIndexed { i, r -> if (r.size.width > 0 && r.size.height > 0) pixels[r.id] = roots[i].captureToImage().toPixelMap() }
            texts.forEach { t -> pixels[top(t.node).id]?.let { px -> contrast(t, px, density)?.let(out::add) } }
        }
        if (Kind.TARGET in checks || Kind.LABEL in checks) {
            // Audit ultra §1: LABEL reads every control TalkBack can act on (a touch, a long press, a slider, a switch);
            // TARGET stays on what is touched.
            val merged = rule.onAllNodes(CONTROL, useUnmergedTree = false).fetchSemanticsNodes(atLeastOneRootRequired = false)
            for (n in merged) {
                if (hidden(n)) continue
                val name = nameOf(n)
                // A slider's or a switch's value (« 0.0 », « on ») is not its name: TalkBack reads « 0.0, slider » (round 1, item 30b).
                // Nor is an action's label (« Réglages du pad ») or a state: TalkBack says « bouton » and the label only later,
                // as a hint (round 3, n° 7 and 37: 16 pad corners a page and the drag handles read « bouton » on the bench).
                val unnamed = controlName(n).isBlank()
                if (Kind.LABEL in checks && unnamed) out += Finding(Kind.LABEL, tagOf(n), "${controlKind(n)} without text nor description")
                if (Kind.TARGET in checks && n.config.contains(SemanticsActions.OnClick)) {
                    // Its touch area, as TalkBack is given it (Material's minimumInteractiveComponentSize
                    // widens a 32 dp chip's to 48 dp; a size forced on the button itself does not).
                    // Two rules: the touch area 48 dp (Android), and what is seen at least 24 x 24 dp (WCAG 2.2
                    // AA 2.5.8): Compose widens a small target's touch to 48 dp where no neighbour is, so a tiny
                    // one in a row of others is still missed.
                    val touch = n.touchBoundsInRoot
                    val w = n.size.width / density
                    val h = n.size.height / density
                    val tw = maxOf(n.size.width.toFloat(), touch.width) / density
                    val th = maxOf(n.size.height.toFloat(), touch.height) / density
                    // A target cut by the edge of a scrolled list is only partly on screen: not judged.
                    if (w > 0f && h > 0f && (w < 23.5f || h < 23.5f || tw < 47.5f || th < 47.5f) && !clippedByScroll(n)) {
                        out += Finding(Kind.TARGET, name.ifBlank { tagOf(n) }, "%.0f x %.0f dp (touch %.0f x %.0f)".format(w, h, tw, th))
                    }
                }
            }
        }
        return out.distinctBy { it.toString() }
    }

    /** Something TalkBack can act on: a touch, a long press, a slider, a switch or a box to tick. */
    val CONTROL = SemanticsMatcher("a control") { n ->
        n.config.contains(SemanticsActions.OnClick) || n.config.contains(SemanticsActions.OnLongClick) ||
            n.config.contains(SemanticsActions.SetProgress) || n.config.contains(SemanticsProperties.ToggleableState)
    }

    /** What kind of control [n] is, in the findings' words. */
    fun controlKind(n: SemanticsNode): String = when {
        n.config.contains(SemanticsActions.SetProgress) -> "slider"
        n.config.contains(SemanticsProperties.ToggleableState) -> "toggle"
        n.config.contains(SemanticsActions.OnClick) -> "touchable"
        else -> "long press"
    }

    private fun top(n: SemanticsNode): SemanticsNode {
        var p = n
        while (true) p = p.parent ?: return p
    }

    private class Txt(val node: SemanticsNode, val text: String, val layout: TextLayoutResult?)

    private fun textOf(n: SemanticsNode): String? =
        n.config.getOrNull(SemanticsProperties.Text)?.joinToString(" ") { it.text }?.takeIf { it.isNotBlank() }

    private fun layout(n: SemanticsNode): TextLayoutResult? {
        val get = n.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action ?: return null
        val list = mutableListOf<TextLayoutResult>()
        return if (get(list)) list.firstOrNull() else null
    }

    fun tagOf(n: SemanticsNode): String =
        n.config.getOrNull(SemanticsProperties.TestTag) ?: "#${n.id}"

    fun nameOf(n: SemanticsNode): String = listOfNotNull(
        n.config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString(" "),
        n.config.getOrNull(SemanticsProperties.Text)?.joinToString(" ") { it.text },
        n.config.getOrNull(SemanticsProperties.EditableText)?.text,
        n.config.getOrNull(SemanticsActions.OnClick)?.label,
        n.config.getOrNull(SemanticsProperties.StateDescription),
    ).joinToString(" ").trim()

    /** A control whose state TalkBack reads as a value (a slider, a switch, a box to tick): it needs a name besides. */
    fun valued(n: SemanticsNode): Boolean =
        n.config.contains(SemanticsActions.SetProgress) || n.config.contains(SemanticsProperties.ToggleableState)

    /** What TalkBack reads as a control's name: its words and its description, not its value nor its action's label. */
    fun controlName(n: SemanticsNode): String = listOfNotNull(
        n.config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString(" "),
        n.config.getOrNull(SemanticsProperties.Text)?.joinToString(" ") { it.text },
        n.config.getOrNull(SemanticsProperties.EditableText)?.text,
    ).joinToString(" ").trim()

    fun hidden(n: SemanticsNode): Boolean {
        var p: SemanticsNode? = n
        while (p != null) {
            if (p.config.contains(SemanticsProperties.HideFromAccessibility) || p.config.contains(SemanticsProperties.InvisibleToUser)) return true
            // Cleared to nothing (`clearAndSetSemantics {}`): what a screen drawn over it covers (the studio under « Que veux-tu
            // faire ? », round 2 integration): neither seen nor read; its texts under the cover's pixels read as false contrasts.
            if (p.config.isClearingSemantics && p.config.none()) return true
            p = p.parent
        }
        return n.boundsInRoot.width <= 0f || n.boundsInRoot.height <= 0f
    }

    /** Axes along which an ancestor scrolls (0: none, 1: horizontal, 2: vertical, 3: both). */
    private fun scrollAxes(n: SemanticsNode): Int {
        var axes = 0
        var p = n.parent
        while (p != null) {
            if (p.config.contains(SemanticsProperties.HorizontalScrollAxisRange)) axes = axes or 1
            if (p.config.contains(SemanticsProperties.VerticalScrollAxisRange)) axes = axes or 2
            p = p.parent
        }
        return axes
    }

    private fun clippedByScroll(n: SemanticsNode): Boolean {
        val full = Rect(n.positionInRoot, androidx.compose.ui.geometry.Size(n.size.width.toFloat(), n.size.height.toFloat()))
        val shown = n.boundsInRoot
        return scrollAxes(n) != 0 && (shown.width < full.width - 1f || shown.height < full.height - 1f)
    }

    /** Where the letters are drawn (the text box can be wider than its words), in root pixels. */
    private fun ink(t: Txt): Rect {
        val l = t.layout ?: return toRoot(t.node, Rect(0f, 0f, t.node.size.width.toFloat(), t.node.size.height.toFloat()))
        if (l.lineCount == 0) return Rect.Zero
        var left = Float.MAX_VALUE
        var right = -Float.MAX_VALUE
        for (i in 0 until l.lineCount) {
            if (l.getLineEnd(i, visibleEnd = true) <= l.getLineStart(i)) continue
            left = min(left, l.getLineLeft(i))
            right = max(right, l.getLineRight(i))
        }
        if (left > right) return Rect.Zero
        // A right-to-left or centred line is placed in the width the paragraph was laid out in, which
        // can be wider than the box the text was given in the end: brought back into its box.
        val box = t.node.size.width.toFloat()
        if (right - left <= box + 1f) {
            // The words fit their box: where the paragraph placed them inside it (centred, end-aligned,
            // laid out wider first) is not read; the box itself is where they are.
            left = maxOf(left, 0f).coerceAtMost(box)
            right = minOf(right, box)
            if (right - left < 1f) { left = 0f; right = box }
        }
        val bottom = min(l.getLineBottom(l.lineCount - 1), l.size.height.toFloat().coerceAtLeast(l.getLineBottom(0)))
        return toRoot(t.node, Rect(left, l.getLineTop(0), right, bottom))
    }

    /**
     * [local] (in the node's own pixels) where it lands in the root, through every scale and move of the
     * layers above it (a tile shrunk to fit), without their clipping (that is what is compared).
     */
    private fun toRoot(n: SemanticsNode, local: Rect): Rect {
        val c = n.layoutInfo.coordinates
        val whole = c.findRootCoordinates().localBoundingBoxOf(c, clipBounds = false)
        val sx = if (n.size.width > 0) whole.width / n.size.width else 1f
        val sy = if (n.size.height > 0) whole.height / n.size.height else 1f
        return Rect(whole.left + local.left * sx, whole.top + local.top * sy, whole.left + local.right * sx, whole.top + local.bottom * sy)
    }

    private fun cut(t: Txt, density: Float): Finding? {
        if (hidden(t.node)) return null
        val l = t.layout
        if (l != null) {
            val ellipsized = (0 until l.lineCount).any { l.isLineEllipsized(it) }
            if (ellipsized) return Finding(Kind.CUT, t.text, "ellipsized (« … »)")
            // A word split over two lines (« Monté/e »), as FitText's own rule reads it.
            val full = l.layoutInput.text.text
            if (l.lineCount > 1 && !com.pocketstudio.ui.WholeWordsAutoSize.fits(l, full) && !l.didOverflowHeight) {
                return Finding(Kind.CUT, t.text, "a word split over two lines")
            }
            // (didOverflowWidth compares with the width the paragraph was laid out in, not the words: the lines themselves.)
            val widest = (0 until l.lineCount).maxOfOrNull { l.getLineRight(it) - l.getLineLeft(it) } ?: 0f
            // (In a row that scrolls sideways a text has all the width it wants: not judged there.)
            if (widest > l.size.width + 1f && scrollAxes(t.node) and 1 == 0) return Finding(Kind.CUT, t.text, "wider than its box (by %.1f dp)".format((widest - l.size.width) / density))
            // Lines taller than the box by more than a quarter of a line: letters cut (less is the font's
            // own room above and below the letters, which a tight box may clip unseen).
            val last = l.lineCount - 1
            if (l.lineCount > 0 && l.getLineBottom(last) - l.size.height > 0.25f * (l.getLineBottom(last) - l.getLineTop(last))) {
                return Finding(Kind.CUT, t.text, "lines cut (taller than its box)")
            }
            if (l.didOverflowHeight && l.lineCount > 0 && l.getLineEnd(l.lineCount - 1, visibleEnd = true) < l.layoutInput.text.length) {
                return Finding(Kind.CUT, t.text, "lines dropped (more lines than room)")
            }
        }
        // Drawn past a clipping parent: the shown part of the letters is smaller than the letters.
        val ink = ink(t)
        if (ink.isEmpty) return null
        val shown = t.node.boundsInRoot
        val axes = scrollAxes(t.node)
        val tol = 2f * density
        val cutX = axes and 1 == 0 && (ink.left < shown.left - tol || ink.right > shown.right + tol)
        val cutY = axes and 2 == 0 && (ink.top < shown.top - tol || ink.bottom > shown.bottom + tol)
        if (shown.isEmpty && axes != 0) return null
        if (cutX || cutY) {
            return Finding(Kind.CUT, t.text, "drawn past its parent's edge (%s, %.0f dp shown of %.0f)".format(
                if (cutX) "across" else "down", (if (cutX) shown.width else shown.height) / density, (if (cutX) ink.width else ink.height) / density))
        }
        return null
    }

    private fun ancestorOf(a: SemanticsNode, b: SemanticsNode): Boolean {
        var p = b.parent
        while (p != null) { if (p.id == a.id) return true; p = p.parent }
        return false
    }

    private fun overlaps(texts: List<Txt>, density: Float): List<Finding> {
        val shown = texts.filter { !hidden(it.node) }.map { it to ink(it).intersect(it.node.boundsInRoot) }.filter { !it.second.isEmpty }
        val out = mutableListOf<Finding>()
        val min = 2f * density
        for (i in shown.indices) for (j in i + 1 until shown.size) {
            val (a, ra) = shown[i]
            val (b, rb) = shown[j]
            if (a.node.root != b.node.root) continue
            val x = ra.intersect(rb)
            if (x.width < min || x.height < min) continue
            if (ancestorOf(a.node, b.node) || ancestorOf(b.node, a.node)) continue
            // The same words twice in one place (a text and its own label for TalkBack) are not two texts.
            if (a.text == b.text && ra == rb) continue
            out += Finding(Kind.OVERLAP, a.text, "over « ${b.text} » (%.0f x %.0f dp)".format(x.width / density, x.height / density))
        }
        return out
    }

    /** A text drawn into something to touch that is not its own (a title running under the button beside it). */
    private fun overButtons(texts: List<Txt>, all: List<SemanticsNode>, density: Float): List<Finding> {
        val buttons = all.filter { it.config.contains(SemanticsActions.OnClick) && !hidden(it) }
        val out = mutableListOf<Finding>()
        val min = 3f * density
        for (t in texts) {
            if (hidden(t.node)) continue
            val ink = ink(t).intersect(t.node.boundsInRoot)
            if (ink.isEmpty) continue
            for (b in buttons) {
                if (b.root != t.node.root || b.id == t.node.id || ancestorOf(b, t.node) || ancestorOf(t.node, b)) continue
                val x = ink.intersect(b.boundsInRoot)
                if (x.width >= min && x.height >= min) {
                    out += Finding(Kind.OVERLAP, t.text, "over the touchable « ${nameOf(b).take(40)} » (%.0f x %.0f dp)".format(x.width / density, x.height / density))
                }
            }
        }
        return out
    }

    /** A text in something switched off (WCAG asks no contrast of inactive controls). */
    private fun disabled(n: SemanticsNode): Boolean {
        var p: SemanticsNode? = n
        while (p != null) { if (p.config.contains(SemanticsProperties.Disabled)) return true; p = p.parent }
        return false
    }

    private fun lum(argb: androidx.compose.ui.graphics.Color): Double {
        fun ch(c: Float): Double = if (c <= 0.03928f) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        return 0.2126 * ch(argb.red) + 0.7152 * ch(argb.green) + 0.0722 * ch(argb.blue)
    }

    private fun ratio(a: Double, b: Double) = (max(a, b) + 0.05) / (min(a, b) + 0.05)

    /**
     * The text's contrast on the pixels: the background is the most common colour in the box of its
     * letters, the ink the 98th percentile of the pixels' contrast with it (anti-aliased edges below).
     */
    private fun contrast(t: Txt, px: androidx.compose.ui.graphics.PixelMap, density: Float): Finding? {
        if (hidden(t.node) || disabled(t.node)) return null
        val r = ink(t).intersect(t.node.boundsInRoot).intersect(Rect(0f, 0f, px.width.toFloat(), px.height.toFloat()))
        if (r.width < 2 || r.height < 2) return null
        val counts = HashMap<Int, Int>()
        val lums = ArrayList<Pair<Int, Double>>()
        for (y in r.top.toInt() until r.bottom.toInt()) for (x in r.left.toInt() until r.right.toInt()) {
            val c = px[x, y]
            val key = (c.red * 255).toInt() shl 16 or ((c.green * 255).toInt() shl 8) or (c.blue * 255).toInt()
            counts[key] = (counts[key] ?: 0) + 1
            lums += key to lum(c)
        }
        val bgKey = counts.maxBy { it.value }.key
        val bg = lums.first { it.first == bgKey }.second
        val far = lums.map { ratio(it.second, bg) }.sortedDescending()
        // The letters cover several percent of their box; the strongest 2 % of its pixels are the ink's core.
        val inkRatio = far[(far.size * 0.02).toInt().coerceAtMost(far.size - 1)]
        if (inkRatio < 1.2) return null // nothing drawn there (a text under something opaque)
        val sp = (t.layout?.layoutInput?.style?.fontSize?.takeIf { it.isSp }?.value ?: 14f)
        val bold = (t.layout?.layoutInput?.style?.fontWeight?.weight ?: 400) >= 700
        val large = sp >= 18f || (bold && sp >= 14f)
        val need = if (large) 3.0 else 4.5
        return if (inkRatio < need - 0.05) Finding(Kind.CONTRAST, t.text, "%.2f:1 (needs %.1f:1, %.0f sp)".format(inkRatio, need, sp)) else null
    }
}
