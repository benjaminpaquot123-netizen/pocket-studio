package com.pocketstudio.app.learn

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.ui.Neon
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Phase 36: where the main controls of the screen shown are, by name, for the « ? » that labels
 * the screen and for the missions that point at a control. Each control says it is there with
 * [spot]; the positions are plain fields (a scroll costs nothing) and become state only while an
 * overlay reads them ([watching]).
 */
class SpotRegistry {
    internal class Spot(val owner: Any, val rect: Rect, val label: String?)

    internal val spots = HashMap<String, Spot>()

    /** Bumped when a spot moves while [watching]: the overlays redraw. */
    internal val version: MutableIntState = mutableIntStateOf(0)

    var watching by mutableStateOf(false)

    internal fun put(id: String, owner: Any, rect: Rect, label: String?) {
        val old = spots[id]
        if (old != null && old.owner === owner && old.rect == rect && old.label == label) return
        spots[id] = Spot(owner, rect, label)
        if (watching) version.intValue++
    }

    internal fun remove(id: String, owner: Any) {
        if (spots[id]?.owner === owner) {
            spots.remove(id)
            if (watching) version.intValue++
        }
    }

    /** Where control [id] is in the window (null: not on the screen). A [id] ending with « * » matches a prefix. */
    fun find(id: String): Rect? {
        val match = if (id.endsWith("*")) spots.entries.firstOrNull { it.key.startsWith(id.dropLast(1)) }?.value else spots[id]
        return match?.rect?.takeIf { it.width > 1f && it.height > 1f }
    }

    /** The labelled controls on the screen now, with their words. */
    fun labelled(): List<Pair<Rect, String>> = labelledById().map { it.second to it.third }

    /** [labelled] with each control's id. */
    fun labelledById(): List<Triple<String, Rect, String>> =
        spots.entries.mapNotNull { (id, s) -> s.label?.let { l -> s.rect.takeIf { it.width > 1f && it.height > 1f }?.let { Triple(id, it, l) } } }
}

val LocalSpots = staticCompositionLocalOf<SpotRegistry?> { null }

/**
 * This control is [id] for the missions, and [label] (a few words, already translated) in the
 * « ? » labels of the screen. Does nothing outside the app's registry (tests, previews).
 */
fun Modifier.spot(id: String, label: String? = null): Modifier = this then SpotElement(id, label)

/** [spot] as a modifier node: it reads the registry where it is attached and leaves it when detached. */
private data class SpotElement(val id: String, val label: String?) : androidx.compose.ui.node.ModifierNodeElement<SpotNode>() {
    override fun create() = SpotNode(id, label)
    override fun update(node: SpotNode) {
        if (node.id != id) node.leave()
        node.id = id
        node.label = label
    }
}

private class SpotNode(var id: String, var label: String?) : Modifier.Node(),
    androidx.compose.ui.node.GlobalPositionAwareModifierNode,
    androidx.compose.ui.node.CompositionLocalConsumerModifierNode {
    private var registry: SpotRegistry? = null

    override fun onGloballyPositioned(coordinates: androidx.compose.ui.layout.LayoutCoordinates) {
        val r = registry ?: currentValueOf(LocalSpots)?.also { registry = it } ?: return
        r.put(id, this, coordinates.boundsInWindow(), label)
    }

    fun leave() {
        registry?.remove(id, this)
    }

    override fun onDetach() {
        leave()
        registry = null
    }
}

/**
 * « ? qui étiquette l'écran »: a label next to each main control of the screen shown, over a
 * light veil. Touches go through to the screen (the veil and the labels take none); « Masquer
 * les étiquettes » and the « ? » itself hide them.
 */
@Composable
fun HelpLabelsOverlay(registry: SpotRegistry, hide: () -> Unit, reportBug: (() -> Unit)? = null) {
    DisposableEffect(registry) {
        registry.watching = true
        onDispose { registry.watching = false }
    }
    var origin by remember { mutableStateOf(Offset.Zero) }
    // Read: the overlay follows the controls (a scroll, a rotation).
    registry.version.intValue
    val withIds = registry.labelledById().filter { (_, r, _) -> r.top >= origin.y - 1f }
    val items = withIds.map { it.second to it.third }
    val ids = withIds.associate { (id, r, l) -> (r to l) to id }
    val amber = Neon.Amber
    Box(Modifier.fillMaxSize().onGloballyPositioned { origin = it.boundsInWindow().topLeft }.testTag("help_labels")) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Color.Black.copy(alpha = 0.38f))
            items.forEach { (r, _) ->
                drawRoundRect(
                    amber, topLeft = Offset(r.left - origin.x, r.top - origin.y), size = Size(r.width, r.height),
                    cornerRadius = CornerRadius(10.dp.toPx()), style = Stroke(2.dp.toPx()),
                )
            }
        }
        // Lot 2 (audit 35): the labels never sit on another control (« Mes projets » was on the
        // tempo), never off the screen, and « Masquer » / « Signaler un bug » go to the bottom, just
        // above the bar of the spaces, rather than over the middle of the screen.
        // The bar of the spaces is a row: at least three « tab_ » on one line (the rail, lying down, is a column).
        val bottomBarTop = registry.spots.entries
            .filter { (id, s) -> id.startsWith("tab_") && s.rect.width > 1f }
            .groupBy { (it.value.rect.top - origin.y).roundToInt() / 4 }
            .filterValues { it.size >= 3 }
            .map { (_, row) -> row.minOf { it.value.rect.top } - origin.y }
        // Every control on the screen, labelled or not (the « ? » itself): a label stays off them.
        val allSpots = registry.spots.entries.filter { it.value.rect.width > 1f && it.value.rect.height > 1f }
            .map { it.key to it.value.rect.translate(-origin.x, -origin.y) }
        val sorted = remember(items) { items.sortedWith(compareBy({ it.first.top }, { it.first.left })) }
        Layout(
            content = {
                sorted.forEach { item ->
                    Text(
                        item.second, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.Black,
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 150.dp).background(Neon.Amber, RoundedCornerShape(8.dp)).padding(horizontal = 7.dp, vertical = 3.dp)
                            .testTag("help_label_" + (ids[item] ?: "")),
                    )
                }
                // The way out (and « Signaler un bug », where help is looked for), last.
                Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp), modifier = Modifier.testTag("help_labels_actions")) {
                    Text(
                        tr("Masquer les étiquettes"), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Neon.OnAccent,
                        maxLines = 2, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.widthIn(max = 200.dp).background(Neon.Cyan, RoundedCornerShape(20.dp)).clickable(onClick = hide)
                            .padding(horizontal = 14.dp, vertical = 10.dp).testTag("help_labels_hide"),
                    )
                    if (reportBug != null) {
                        Text(
                            "🐞 " + tr("Signaler un bug"), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Neon.TextPrimary,
                            maxLines = 2, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.widthIn(max = 200.dp).background(Neon.Surface, RoundedCornerShape(20.dp)).border(1.dp, Neon.Amber, RoundedCornerShape(20.dp))
                                .clickable(onClick = { hide(); reportBug() }).padding(horizontal = 14.dp, vertical = 10.dp).testTag("help_labels_bug"),
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxSize(),
        ) { measurables, constraints ->
            val loose = Constraints(maxWidth = constraints.maxWidth, maxHeight = constraints.maxHeight)
            val placeables = measurables.dropLast(1).map { it.measure(loose) }
            val chips = measurables.last().measure(loose)
            val width = constraints.maxWidth.toFloat()
            val height = constraints.maxHeight.toFloat()
            val margin = 8.dp.toPx()
            // Above the bottom bar when it is at the bottom (upright), else at the screen's bottom.
            val barTop = bottomBarTop.filter { it > height / 2f }.minOrNull()
            // A line of labels' room left between them and the bar: the bar's own labels go above it.
            val labelRoom = if (barTop != null) (placeables.maxOfOrNull { it.height } ?: 0) + 2 * margin else 0f
            val chipsBottom = ((barTop ?: height) - margin - labelRoom).coerceAtLeast(chips.height.toFloat())
            // Small controls (not a whole screen's grid) the chips should not cover; with no bar (lying
            // down), the bottom place that covers the least of them: middle, right or left.
            val small = allSpots.map { it.second }.filter { it.width * it.height < width * height * 0.2f }
            fun covered(c: Rect) = small.sumOf { o -> o.intersect(c).let { if (it.width > 0f && it.height > 0f) (it.width * it.height).toDouble() else 0.0 } }
            val chipsTop = chipsBottom - chips.height
            val chipsAt = listOf((width - chips.width) / 2f, width - margin - chips.width, margin)
                .map { x -> Rect(x, chipsTop, x + chips.width, chipsBottom) }
                .let { places -> if (barTop != null) places.first() else places.minBy(::covered) }
            val placed = ArrayList<Rect>()
            placed += chipsAt
            val controls = sorted.map { it.first.translate(-origin.x, -origin.y) }
            val ownIds = sorted.map { ids[it] }
            // The unlabelled controls too (the « ? »): kept clear like the others.
            val others = allSpots.filter { (id, _) -> id !in ownIds }.map { it.second }
            val gap = 2.dp.toPx()
            fun onScreen(c: Rect) = c.left >= 0f && c.top >= 0f && c.right <= width && c.bottom <= height
            val positions = placeables.mapIndexed { i, pl ->
                val r = controls[i]
                val w = pl.width.toFloat()
                val h = pl.height.toFloat()
                // Sideways a label may slide to stay on the screen, never up or down.
                fun at(x: Float, y: Float): Rect {
                    val left = x.coerceIn(0f, max(0f, width - w))
                    return Rect(left, y, left + w, y + h)
                }
                val around = listOf(
                    at(r.center.x - w / 2f, r.bottom + gap),
                    at(r.center.x - w / 2f, r.top - gap - h),
                    at(r.left, r.bottom + gap),
                    at(r.right - w, r.bottom + gap),
                    at(r.right + gap, r.center.y - h / 2f),
                    at(r.left - gap - w, r.center.y - h / 2f),
                )
                // On its own control (its lower edge, inside it).
                val inside = at(r.center.x - w / 2f, (r.bottom - h - 2.dp.toPx()).coerceAtLeast(r.top))
                fun clearOfLabels(c: Rect) = onScreen(c) && placed.none { it.overlaps(c) }
                // Free: over no other control at all.
                fun free(c: Rect) = clearOfLabels(c) && controls.indices.none { j -> j != i && controls[j].overlaps(c) } && others.none { it.overlaps(c) }
                // Else over a corner of another control at most: never on its middle, where its icon or word is.
                fun middle(o: Rect) = o.deflate(min(o.width, o.height) * 0.3f)
                fun light(c: Rect) = clearOfLabels(c) && controls.indices.none { j -> j != i && middle(controls[j]).overlaps(c) } && others.none { middle(it).overlaps(c) }
                val spot = (around + inside).firstOrNull(::free) ?: (around + inside).firstOrNull(::light)
                if (spot != null) placed += spot
                spot?.let { it.left.roundToInt() to it.top.roundToInt() }
            }
            layout(constraints.maxWidth, constraints.maxHeight) {
                placeables.forEachIndexed { i, pl -> positions[i]?.let { (x, y) -> pl.place(x, y) } }
                chips.place(chipsAt.left.roundToInt(), chipsAt.top.roundToInt())
            }
        }
        if (items.isEmpty()) {
            Text(
                tr("Rien à étiqueter sur cet écran."), color = Neon.TextPrimary,
                modifier = Modifier.align(androidx.compose.ui.Alignment.Center).background(Neon.Surface, RoundedCornerShape(12.dp)).padding(16.dp),
            )
        }
    }
}

/**
 * A control pointed at: the screen dimmed around it (a hole where it is, so it stays plain to
 * see and to touch), a ring, and [card] (the mission's words) where it does not hide it.
 */
@Composable
fun SpotlightOverlay(registry: SpotRegistry, targets: List<String>, card: @Composable (Modifier, Boolean) -> Unit) {
    DisposableEffect(registry) {
        registry.watching = true
        onDispose { registry.watching = false }
    }
    var origin by remember { mutableStateOf(Offset.Zero) }
    registry.version.intValue
    val target = targets.firstNotNullOfOrNull { registry.find(it) }?.translate(-origin.x, -origin.y)
    val amber = Neon.Amber
    Box(Modifier.fillMaxSize().onGloballyPositioned { origin = it.boundsInWindow().topLeft }.testTag("mission_overlay")) {
        if (target != null) {
            Canvas(Modifier.fillMaxSize().graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
                val pad = 6.dp.toPx()
                val hole = Rect(target.left - pad, target.top - pad, target.right + pad, target.bottom + pad)
                drawRect(Color.Black.copy(alpha = 0.45f))
                drawRoundRect(Color.Transparent, hole.topLeft, hole.size, CornerRadius(14.dp.toPx()), blendMode = BlendMode.Clear)
                drawRoundRect(amber, hole.topLeft, hole.size, CornerRadius(14.dp.toPx()), style = Stroke(3.dp.toPx()))
            }
        }
        // The card on the half of the screen the control is not in.
        BoxLayoutHalf(target) { top -> card(Modifier.align(if (top) androidx.compose.ui.Alignment.TopCenter else androidx.compose.ui.Alignment.BottomCenter), top) }
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.BoxLayoutHalf(target: Rect?, content: @Composable androidx.compose.foundation.layout.BoxScope.(Boolean) -> Unit) {
    var height by remember { mutableStateOf(0f) }
    Box(Modifier.fillMaxSize().onGloballyPositioned { height = it.size.height.toFloat() }) {
        val top = target != null && height > 0f && target.center.y > height / 2f
        content(top)
    }
}

