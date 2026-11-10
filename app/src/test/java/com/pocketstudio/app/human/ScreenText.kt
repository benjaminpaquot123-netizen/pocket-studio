package com.pocketstudio.app.human

import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.TextView
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutInfo
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.text.TextLayoutResult
import java.security.MessageDigest

/**
 * The screen as text (Audit ultra §3.1, « l'écran en texte »): what a person sees, window by window (the
 * top one first, down to the first that takes every touch), in reading order. Each line: what it looks
 * like, its icons and words as drawn, the state, and the middle in the screen's pixels (where a « touche »
 * lands). Audit « banc crédible » 2, R1: never what only TalkBack says (an icon's name, a state's words):
 * an icon is its glyph or its shape ([IconShapes]: « ⋮ », « (micro) »), and « bouton » is only what is drawn
 * with a fill or an outline; else an icon one can touch is « icône » (its label after it), and a sentence one can
 * touch, with neither frame nor icon, is « texte », as a person sees it.
 * TalkBack's names stay in the journal (« DOIGT », [under]), for a TalkBack seat (P4, not here).
 * With [reads] false (`lit=non`), the texts under 18 sp are « ▭▭▭ »: the tester has the icons, the big
 * words and the layout.
 *
 * Also, for the journal: the route (the dialog on top, or the page and the tab), the node under a
 * finger, and the texts to grey out on the image.
 */
internal class ScreenText(private val compose: ComposeTestRule, private val screen: DisplayMetrics, private val reads: Boolean, private val rtl: Boolean) {
    class Snapshot(val lines: List<String>, val route: String, val small: List<Rect>, val texts: Set<String>) {
        val fingerprint: String get() = fingerprintOf(lines)
    }

    private val density get() = screen.density

    fun read(): Snapshot {
        val wins = shownWindows()
        val roots = composeRoots()
        val lines = mutableListOf<String>()
        val small = mutableListOf<Rect>()
        val smallTexts = HashSet<String>()
        mark(wins, roots)
        val texts = unmergedTexts().filter { it.window == null || seen(it.node) }
        wins.forEachIndexed { i, w ->
            val head = when {
                wins.size == 1 -> null
                i == 0 -> "— au-dessus : ${kindOf(w)} —"
                else -> "— dessous —"
            }
            head?.let(lines::add)
            val mine = roots.filter { BenchWindows.windowOf(it)?.view === w.view }
            if (mine.isEmpty()) viewLines(w, lines, small)
            texts.filter { (it.window === w.view) && it.sp < 18f }.forEach { small += it.rect; smallTexts += it.text }
            for (r in mine) {
                walk(r, lines, smallTexts)
            }
        }
        return Snapshot(lines, route(wins, roots), small, texts.mapTo(HashSet()) { it.text })
    }

    /** The windows a person sees and can reach: the top one down to the first that takes every touch. */
    private fun shownWindows(): List<BenchWindows.Win> {
        val all = runCatching { BenchWindows.windows() }.getOrDefault(emptyList()).filter { it.view.visibility == View.VISIBLE && it.view.width > 0 }
        val out = mutableListOf<BenchWindows.Win>()
        for (w in all) { out += w; if (w.touchModal) break }
        return out
    }

    private fun kindOf(w: BenchWindows.Win): String =
        if (w.params.type == android.view.WindowManager.LayoutParams.TYPE_BASE_APPLICATION) "l'écran" else "dialogue"

    private fun composeRoots(): List<SemanticsNode> = runCatching {
        compose.onAllNodes(isRoot(), useUnmergedTree = false).fetchSemanticsNodes(atLeastOneRootRequired = false)
    }.getOrDefault(emptyList())

    private fun drawn(n: SemanticsNode): Rect = BenchWindows.drawnBounds(n, screen)

    private fun onScreen(r: Rect) = r.width > 0.5f && r.height > 0.5f && r.right > 0f && r.bottom > 0f && r.left < screen.widthPixels && r.top < screen.heightPixels

    private fun hidden(n: SemanticsNode): Boolean =
        n.config.contains(SemanticsProperties.HideFromAccessibility) || n.config.contains(SemanticsProperties.InvisibleToUser)

    private fun sorted(children: List<SemanticsNode>): List<SemanticsNode> {
        val row = 12f * density
        return children.sortedWith(compareBy<SemanticsNode>({ (drawn(it).top / row).toInt() }, { if (rtl) -drawn(it).right else drawn(it).left }))
    }

    /** The merged nodes walked (seen), and where the walk stopped (hidden from a person). */
    private val walked = HashSet<Int>()
    private val stopped = HashSet<Int>()

    /** [u] (any tree) is seen: its nearest node the walk met was walked, not stopped. */
    private fun seen(u: SemanticsNode): Boolean {
        var p: SemanticsNode? = u
        while (p != null) {
            if (p.id in walked) return true
            if (p.id in stopped) return false
            p = p.parent
        }
        return false
    }

    /** The screen's lines in reading order ([out] null: only marks what is seen). */
    /** The nodes under a page drawn over them on the whole screen (Pad Live over the project): never seen. */
    private val covered = HashSet<Int>()
    /** The tag of the page drawn over the others on the whole screen, if any (the route's page). */
    private var cover: String? = null

    /**
     * Marks what a person can see: the windows shown, without what they hide. In a window, the last
     * tagged page over half the screen or more (in the tree's order, the order of drawing) hides what was
     * drawn before it inside its bounds (its parents aside), as Pad Live hides the project under it.
     */
    private fun mark(wins: List<BenchWindows.Win>, roots: List<SemanticsNode>) {
        walked.clear()
        stopped.clear()
        covered.clear()
        lineIds.clear()
        cover = null
        val area = screen.widthPixels.toFloat() * screen.heightPixels
        val whole = Rect(0f, 0f, screen.widthPixels.toFloat(), screen.heightPixels.toFloat())
        for (w in wins) {
            val mine = roots.filter { BenchWindows.windowOf(it)?.view === w.view }
            val order = mutableListOf<SemanticsNode>()
            fun pre(n: SemanticsNode) { lineIds += n.id; if (hidden(n)) return; order += n; n.children.forEach(::pre) }
            mine.forEach(::pre)
            val last = order.indexOfLast { n ->
                val r = drawn(n).intersect(whole)
                n.config.contains(SemanticsProperties.TestTag) && n.parent != null && r.width * r.height >= 0.5f * area
            }
            if (last > 0) {
                val top = order[last]
                val box = drawn(top)
                val above = generateSequence(top.parent) { it.parent }.mapTo(HashSet()) { it.id }
                for (i in 0 until last) if (order[i].id !in above && box.contains(drawn(order[i]).center)) covered += order[i].id
                if (covered.isNotEmpty() && w === wins.first()) cover = top.config.getOrNull(SemanticsProperties.TestTag)
            }
            mine.forEach { walk(it, null, emptySet()) }
        }
    }

    private fun walk(n: SemanticsNode, out: MutableList<String>?, smallTexts: Set<String>) {
        if (hidden(n) || n.id in covered) { stopped += n.id; return }
        walked += n.id
        val r = drawn(n)
        if (out == null) { n.children.forEach { walk(it, null, smallTexts) }; return }
        line(n, r, smallTexts)?.let { if (onScreen(r)) out += it }
        // PLAN-RESTE R.8: no « ↓ la suite plus bas » any more, an arrow the app does not draw (hidden defect B09). What a
        // person sees of a list going on is a line cut by an edge: [line] says « coupé ».
        sorted(n.children).forEach { walk(it, out, smallTexts) }
    }

    private fun actionable(n: SemanticsNode) = n.config.contains(SemanticsActions.OnClick) || n.config.contains(SemanticsActions.OnLongClick) ||
        n.config.contains(SemanticsActions.SetProgress) || n.config.contains(SemanticsProperties.EditableText)

    private fun line(n: SemanticsNode, r: Rect, smallTexts: Set<String>): String? {
        val c = n.config
        val texts = c.getOrNull(SemanticsProperties.Text)?.map { it.text }?.filter { it.isNotBlank() }.orEmpty()
        val edit = c.getOrNull(SemanticsProperties.EditableText)?.text
        val desc = c.getOrNull(SemanticsProperties.ContentDescription)?.joinToString(" ")?.takeIf { it.isNotBlank() }
        val act = actionable(n)
        if (texts.isEmpty() && desc == null && edit == null && !act) return null
        val role = c.getOrNull(SemanticsProperties.Role)
        val look = lookOf(n)
        val icons = look.before + look.after
        // R1: the word says how it is drawn, never a role only TalkBack hears (« bouton » for a plain sentence).
        val word = when {
            edit != null || c.contains(SemanticsActions.SetText) -> "champ"
            role == Role.Checkbox -> "case"
            role == Role.Switch -> "interrupteur"
            role == Role.RadioButton -> "choix"
            role == Role.Tab -> "onglet"
            role == Role.DropdownList -> "liste"
            role == Role.ValuePicker -> "sélecteur"
            c.contains(SemanticsActions.SetProgress) -> "curseur"
            act && look.framed -> "bouton"
            act && icons.isNotEmpty() -> "icône"
            texts.isEmpty() && icons.isNotEmpty() -> "image"
            texts.isEmpty() -> "dessin"
            c.contains(SemanticsProperties.Heading) -> "titre"
            else -> "texte"
        }
        fun shown(t: String) = if (!reads && t in smallTexts) "▭▭▭" else t
        val shownTexts = texts.map(::shown)
        val name = listOfNotNull(
            look.before.joinToString(" ").ifEmpty { null },
            shownTexts.takeIf { it.isNotEmpty() }?.joinToString(" ", "« ", " »"),
            edit?.let { "contenu « ${if (reads) it else "▭▭▭"} »" },
            look.after.joinToString(" ").ifEmpty { null },
        ).joinToString(" ")
        val states = buildList {
            if (c.contains(SemanticsProperties.Disabled)) add("désactivé")
            // R.8: drawn in part only (a scrolling parent or the screen's edge cuts it), as a person sees a cut card.
            if (n.size.height > 0 && n.size.width > 0 && (r.height < 0.9f * n.size.height || r.width < 0.9f * n.size.width)) add("coupé")
            when (c.getOrNull(SemanticsProperties.ToggleableState)) {
                ToggleableState.On -> add("coché"); ToggleableState.Off -> add("non coché"); ToggleableState.Indeterminate -> add("en partie"); null -> Unit
            }
            // Fresh review G1: a pad's light as drawn (Pad Live, the Live page, Rythme's pads): lit, or its faint waiting ring.
            look.light?.let { add(it) }
            if (c.getOrNull(SemanticsProperties.Selected) == true) add("sélectionné")
            // A state's words are TalkBack's, but what they say is drawn (a step lit or empty, « en cours »): kept.
            c.getOrNull(SemanticsProperties.StateDescription)?.takeIf { it.isNotBlank() && reads }?.let { add("état « $it »") }
            c.getOrNull(SemanticsProperties.ProgressBarRangeInfo)?.let { p ->
                val span = p.range.endInclusive - p.range.start
                if (span > 0f && p.current.isFinite()) add("à %d %%".format(((p.current - p.range.start) / span * 100).toInt()))
            }
        }
        val at = "@ ${r.center.x.toInt()},${r.center.y.toInt()}"
        return listOf(word, name, states.takeIf { it.isNotEmpty() }?.joinToString(", ", "[", "]").orEmpty(), at).filter { it.isNotEmpty() }.joinToString(" ")
    }

    /** The ids of the merged tree's nodes (each its own line): a node's drawing stops where another line's starts. */
    private val lineIds = HashSet<Int>()

    /** What a node draws besides its words: its icons before and after them, and whether it has a face (a fill or an outline). */
    private class Look(
        val before: List<String>, val after: List<String>, val framed: Boolean,
        /** Fresh review G1: « allumé » or « anneau pâle » when a pad light ([com.pocketstudio.ui.padLight]) is drawn on it. */
        val light: String? = null,
        /** Fresh review M1: « case », « choix » or « interrupteur » when Material draws a check box, a radio button or a switch. */
        val widget: String? = null,
        /** Every mark with where it is drawn (root coordinates), for a node that is no line of its own ([drawnAlone]). */
        val marks: List<Pair<String, Rect?>> = emptyList(),
        val box: Rect = Rect.Zero,
    )

    /**
     * R1: what [n] looks like, read in its layout (not in what it says to TalkBack): the icons painted in it (an
     * ImageVector's name → [IconShapes]; another picture: « (image) »; a drawn mark with a tag of [DRAWN_TAGS]), on the
     * start or end side of its middle, down to the nodes that are lines of their own; framed when the node itself, or a
     * part of it covering a quarter of its area or more (a chip drawn inside its 48 dp touch target), has a visible background or an outline.
     */
    private fun lookOf(n: SemanticsNode): Look {
        val root = runCatching { n.layoutInfo }.getOrNull() ?: return Look(emptyList(), emptyList(), false)
        val box = runCatching { root.coordinates.boundsInRoot() }.getOrNull() ?: return Look(emptyList(), emptyList(), false)
        val before = mutableListOf<String>()
        val after = mutableListOf<String>()
        val marks = mutableListOf<Pair<String, Rect?>>()
        var framed = false
        var light: String? = null
        var widget: String? = null
        fun place(shape: String, at: Rect?) {
            marks += shape to at
            // Before the words: on their start side, or above them (an icon over its label).
            val dx = at?.let { (it.center.x - box.center.x) * (if (rtl) -1 else 1) } ?: 0f
            val start = at == null || dx < -1f || (dx <= 1f && at.center.y < box.center.y)
            if (start) before += shape else after += shape
        }
        fun visit(info: LayoutInfo, top: Boolean) {
            if (!top && info.semanticsId in lineIds) return
            if (!info.isPlaced) return
            val at = runCatching { info.coordinates.boundsInRoot() }.getOrNull()
            val face = top || (at != null && box.width * box.height > 0f && at.width * at.height >= 0.25f * box.width * box.height)
            val mods = runCatching { info.getModifierInfo() }.getOrDefault(emptyList()).map { it.modifier }
            val turn = mods.firstOrNull { it.javaClass.name == "androidx.compose.ui.graphics.GraphicsLayerElement" }?.let { field(it, "rotationZ") as? Float } ?: 0f
            val dp = (at?.width ?: 0f) / density
            for (e in mods) {
                when (e.javaClass.name) {
                    "androidx.compose.ui.draw.PainterElement" -> painted(e)?.let { place(it, at) }
                    "androidx.compose.foundation.BackgroundElement" -> if (face && visibleBackground(e)) framed = true
                    "androidx.compose.foundation.BorderModifierNodeElement" -> if (face && visibleBorder(e)) framed = true
                    "androidx.compose.ui.draw.DrawWithCacheElement" -> if (face) padLightOf(field(e, "onBuildDrawCache"))?.let { light = it }
                }
            }

            if (!top) (call(info, "getSemanticsConfiguration") as? androidx.compose.ui.semantics.SemanticsConfiguration)
                ?.getOrNull(SemanticsProperties.TestTag)?.let { DRAWN_TAGS[it] }?.let { place(it, at) }
            (call(info, "getChildrenInfo") as? List<*>)?.forEach { (it as? LayoutInfo)?.let { c -> visit(c, false) } }
        }
        visit(root, true)
        return Look(before, after, framed, light, widget, marks, box)
    }

    /**
     * Fresh review G1: the state a pad light draws, read in what its drawing holds (ui-components PadLight.kt: the light's
     * animation, then whether it waits): « allumé » (its inner light and ring), « anneau pâle » (waiting for its beat), or null.
     */
    private fun padLightOf(lambda: Any?): String? {
        if (lambda == null) return null
        val held = lambda.javaClass.declaredFields.sortedBy { it.name.filter(Char::isDigit).toIntOrNull() ?: 0 }
            .map { f -> runCatching { f.isAccessible = true; f.get(lambda) }.getOrNull() }
        if (held.none { it?.javaClass?.name == "com.pocketstudio.ui.PadLight" }) return null
        val lit = held.firstNotNullOfOrNull { v -> v?.takeIf { it.javaClass.simpleName == "AnimationState" }?.let { call(it, "getValue") as? Float } } ?: 0f
        val waiting = held.firstNotNullOfOrNull { it as? Boolean } ?: false
        return when {
            lit > 0.5f -> "allumé"
            waiting -> "anneau pâle"
            else -> null
        }
    }

    /** A layout node's own method (its children, its semantics: Compose's `SemanticsInfo` is internal to it). */
    private fun call(of: Any, name: String): Any? = runCatching { of.javaClass.getMethod(name).invoke(of) }.getOrNull()

    private fun field(of: Any, name: String): Any? = runCatching { of.javaClass.getDeclaredField(name).apply { isAccessible = true }.get(of) }.getOrNull()

    /** A painter's look: an icon's glyph or shape; a picture (a bitmap, a colour) otherwise. */
    private fun painted(element: Any): String? {
        val painter = field(element, "painter") ?: return null
        if (painter is androidx.compose.ui.graphics.painter.ColorPainter) return null
        val vector = painter.javaClass.methods.firstOrNull { it.name.startsWith("getName") && it.parameterCount == 0 }
        val name = vector?.let { runCatching { it.invoke(painter) as? String }.getOrNull() } ?: return "(image)"
        return IconShapes.of(name)
    }

    private fun visibleBackground(element: Any): Boolean {
        if (field(element, "brush") != null) return true
        val bits = (field(element, "color") as? Long) ?: return false
        val alpha = (field(element, "alpha") as? Float) ?: 1f
        return Color(bits.toULong()).alpha * alpha > 0.05f
    }

    private fun visibleBorder(element: Any): Boolean {
        val width = (field(element, "width") as? Float) ?: return false
        val brush = field(element, "brush") as? androidx.compose.ui.graphics.SolidColor
        return width > 0f && (brush == null || brush.value.alpha > 0.05f)
    }

    private class Txt(val text: String, val sp: Float, val rect: Rect, val window: View?, val node: SemanticsNode)

    /** Every text of the unmerged tree shown: its words, its size in sp, where it is drawn, its window. */
    private fun unmergedTexts(): List<Txt> {
        val out = mutableListOf<Txt>()
        val all = runCatching {
            compose.onAllNodes(androidx.compose.ui.test.SemanticsMatcher.keyIsDefined(SemanticsProperties.Text), useUnmergedTree = true)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
        }.getOrDefault(emptyList())
        for (n in all) {
            if (hidden(n)) continue
            val text = n.config.getOrNull(SemanticsProperties.Text)?.joinToString(" ") { it.text }?.takeIf { it.isNotBlank() } ?: continue
            val r = drawn(n)
            if (!onScreen(r)) continue
            val sp = spOf(n)
            val win = BenchWindows.windowOf(n)?.view
            out += Txt(text, sp, r, win, n)
            n.config.getOrNull(SemanticsProperties.Text)?.forEach { a -> if (a.text != text) out += Txt(a.text, sp, r, win, n) }
        }
        return out
    }

    private fun spOf(n: SemanticsNode): Float {
        val get = n.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action ?: return 14f
        val list = mutableListOf<TextLayoutResult>()
        if (!runCatching { get(list) }.getOrDefault(false)) return 14f
        val size = list.firstOrNull()?.layoutInput?.style?.fontSize ?: return 14f
        return if (size.isSp) size.value else 14f
    }

    /** A window of Android views (a menu, the system's question): its texts, what is touched there. */
    private fun viewLines(w: BenchWindows.Win, out: MutableList<String>, small: MutableList<Rect>) {
        val origin = w.drawn(screen)
        fun visit(v: View, inList: Boolean) {
            if (v.visibility != View.VISIBLE || v.width <= 0 || v.height <= 0) return
            val loc = IntArray(2).also { v.getLocationInWindow(it) }
            val r = Rect(origin.left + loc[0].toFloat(), origin.top + loc[1].toFloat(), origin.left + loc[0] + v.width.toFloat(), origin.top + loc[1] + v.height.toFloat())
            if (v is TextView && !v.text.isNullOrBlank() && onScreen(r)) {
                val sp = v.textSize / screen.scaledDensity
                val t = v.text.toString()
                val tiny = sp < 18f
                if (tiny) small += r
                val word = if (v.isClickable || inList || v is android.widget.Button) "bouton" else "texte"
                out += "$word « ${if (!reads && tiny) "▭▭▭" else t} » @ ${r.center.x.toInt()},${r.center.y.toInt()}"
            }
            if (v is ViewGroup) for (i in 0 until v.childCount) visit(v.getChildAt(i), inList || v is AdapterView<*>)
        }
        visit(w.view, false)
    }

    /** The route: the dialog on top (its first tag or words), else the page (the smallest tagged node over half the screen) and the tab. */
    private fun route(wins: List<BenchWindows.Win>, roots: List<SemanticsNode>): String {
        val tagged = runCatching {
            compose.onAllNodes(androidx.compose.ui.test.SemanticsMatcher.keyIsDefined(SemanticsProperties.TestTag), useUnmergedTree = true)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
        }.getOrDefault(emptyList())
        val top = wins.firstOrNull() ?: return "?"
        val inTop = tagged.filter { BenchWindows.windowOf(it)?.view === top.view && onScreen(drawn(it)) && seen(it) }
        if (top.params.type != android.view.WindowManager.LayoutParams.TYPE_BASE_APPLICATION) {
            val first = inTop.minByOrNull { drawn(it).top }?.config?.getOrNull(SemanticsProperties.TestTag)
                ?: roots.firstOrNull { BenchWindows.windowOf(it)?.view === top.view }?.let { r -> firstText(r) }?.let { "« ${it.take(30)} »" }
                ?: viewText(top)?.let { "« ${it.take(30)} »" }
            return "dialogue:${first ?: "?"}"
        }
        val area = screen.widthPixels.toFloat() * screen.heightPixels
        val page = cover ?: inTop.filter { val r = drawn(it); r.width * r.height >= 0.5f * area }
            .minByOrNull { val r = drawn(it); r.width * r.height }?.config?.getOrNull(SemanticsProperties.TestTag)
        val tab = inTop.firstOrNull { (it.config.getOrNull(SemanticsProperties.TestTag) ?: "").startsWith("tab_") && it.config.getOrNull(SemanticsProperties.Selected) == true }
            ?.config?.getOrNull(SemanticsProperties.TestTag)
            ?: runCatching {
                // The tab's Selected is on its merged node (the tag may sit on the same node or its parent).
                compose.onAllNodes(androidx.compose.ui.test.SemanticsMatcher.expectValue(SemanticsProperties.Selected, true), useUnmergedTree = false)
                    .fetchSemanticsNodes(atLeastOneRootRequired = false).filter(::seen).firstNotNullOfOrNull { n -> n.config.getOrNull(SemanticsProperties.TestTag)?.takeIf { it.startsWith("tab_") } }
            }.getOrNull()
        return listOfNotNull(page, tab).joinToString("/").ifEmpty { "?" }
    }

    private fun firstText(n: SemanticsNode): String? =
        n.config.getOrNull(SemanticsProperties.Text)?.joinToString(" ") { it.text }?.takeIf { it.isNotBlank() } ?: n.children.firstNotNullOfOrNull(::firstText)

    private fun viewText(w: BenchWindows.Win): String? {
        fun find(v: View): String? = if (v is TextView && !v.text.isNullOrBlank()) v.text.toString()
        else (v as? ViewGroup)?.let { g -> (0 until g.childCount).firstNotNullOfOrNull { find(g.getChildAt(it)) } }
        return find(w.view)
    }

    /**
     * What is under a finger at ([x], [y]), for the journal: the smallest thing to touch there in the
     * window that gets the touch (« tag « words » », « désactivé tag … »), or « rien » (and the words there).
     */
    fun under(x: Float, y: Float): String {
        if (walked.isEmpty()) mark(shownWindows(), composeRoots())
        val win = runCatching { BenchWindows.windows().filter { it.touchable }.firstOrNull { w -> w.touchModal || w.drawn(screen).contains(x.toInt(), y.toInt()) } }.getOrNull()
            ?: return "rien"
        val nodes = runCatching {
            compose.onAllNodes(androidx.compose.ui.test.SemanticsMatcher("anything") { true }, useUnmergedTree = true).fetchSemanticsNodes(atLeastOneRootRequired = false)
        }.getOrDefault(emptyList()).filter { BenchWindows.windowOf(it)?.view === win.view && seen(it) && drawn(it).contains(androidx.compose.ui.geometry.Offset(x, y)) }
        val hit = nodes.filter(::actionable).minByOrNull { val r = drawn(it); r.width * r.height }
        if (hit == null) {
            if (nodes.isEmpty()) viewUnder(win, x, y)?.let { return it }
            val words = nodes.mapNotNull { it.config.getOrNull(SemanticsProperties.Text)?.joinToString(" ") { t -> t.text } }.firstOrNull()
            return "rien" + (words?.let { " (texte « ${it.take(40)} »)" } ?: "")
        }
        val tag = hit.config.getOrNull(SemanticsProperties.TestTag) ?: generateSequence(hit.parent) { it.parent }.firstNotNullOfOrNull { it.config.getOrNull(SemanticsProperties.TestTag) }?.let { "dans:$it" } ?: "#${hit.id}"
        val words = hit.config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString(" ")
            ?: hit.config.getOrNull(SemanticsProperties.Text)?.joinToString(" ") { it.text } ?: textBelow(hit)
        val off = generateSequence(hit) { it.parent }.any { it.config.contains(SemanticsProperties.Disabled) }
        return (if (off) "désactivé " else "") + tag + (words?.let { " « ${it.take(40)} »" } ?: "")
    }

    private fun textBelow(n: SemanticsNode): String? =
        n.children.firstNotNullOfOrNull { c -> c.config.getOrNull(SemanticsProperties.Text)?.joinToString(" ") { it.text } ?: textBelow(c) }

    private fun viewUnder(w: BenchWindows.Win, x: Float, y: Float): String? {
        val origin = w.drawn(screen)
        var best: String? = null
        fun visit(v: View) {
            if (v.visibility != View.VISIBLE) return
            val loc = IntArray(2).also { v.getLocationInWindow(it) }
            val l = origin.left + loc[0]; val t = origin.top + loc[1]
            if (x < l || y < t || x > l + v.width || y > t + v.height) return
            if (v is TextView && !v.text.isNullOrBlank()) best = "vue « ${v.text.toString().take(40)} »"
            if (v is ViewGroup) for (i in 0 until v.childCount) visit(v.getChildAt(i))
        }
        visit(w.view)
        return best
    }

    companion object {
        /** Marks the app draws itself (a Canvas, no icon), by their test tag: what they look like. */
        val DRAWN_TAGS = mapOf("link_chevron" to "›")

        /** The screen's text as a short hash: clock times and percentages left out (a song's position, a progress). */
        fun fingerprintOf(lines: List<String>): String {
            val norm = lines.joinToString("\n") { l -> l.replace(Regex("\\d+:\\d\\d"), "#:##").replace(Regex("\\d+ ?%"), "#%") }
            return MessageDigest.getInstance("SHA-1").digest(norm.toByteArray()).joinToString("") { "%02x".format(it) }.take(10)
        }
    }
}
