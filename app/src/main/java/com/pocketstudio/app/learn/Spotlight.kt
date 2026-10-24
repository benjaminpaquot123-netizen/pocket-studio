package com.pocketstudio.app.learn

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
    fun labelled(): List<Pair<Rect, String>> =
        spots.values.mapNotNull { s -> s.label?.let { l -> s.rect.takeIf { it.width > 1f && it.height > 1f }?.let { it to l } } }
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
fun HelpLabelsOverlay(registry: SpotRegistry, hide: () -> Unit) {
    DisposableEffect(registry) {
        registry.watching = true
        onDispose { registry.watching = false }
    }
    var origin by remember { mutableStateOf(Offset.Zero) }
    // Read: the overlay follows the controls (a scroll, a rotation).
    registry.version.intValue
    val items = registry.labelled().filter { (r, _) -> r.top >= origin.y - 1f }
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
        // The hide chip in the middle of the screen, where no main control's label goes.
        val chipW = with(LocalDensity.current) { 220.dp.toPx() }
        val chipH = with(LocalDensity.current) { 56.dp.toPx() }
        // The labels, top to bottom: under their control, else above, beside it; a label with no
        // free place is left out rather than drawn over another one.
        val sorted = remember(items) { items.sortedWith(compareBy({ it.first.top }, { it.first.left })) }
        Layout(
            content = {
                sorted.forEach { (_, text) ->
                    Text(
                        text, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.Black,
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 150.dp).background(Neon.Amber, RoundedCornerShape(8.dp)).padding(horizontal = 7.dp, vertical = 3.dp),
                    )
                }
            },
            modifier = Modifier.fillMaxSize(),
        ) { measurables, constraints ->
            val placeables = measurables.map { it.measure(Constraints(maxWidth = constraints.maxWidth, maxHeight = constraints.maxHeight)) }
            val width = constraints.maxWidth.toFloat()
            val height = constraints.maxHeight.toFloat()
            val placed = ArrayList<Rect>()
            placed += Rect(width / 2f - chipW / 2f, height / 2f - chipH / 2f, width / 2f + chipW / 2f, height / 2f + chipH / 2f)
            val gap = 2.dp.toPx()
            val positions = placeables.mapIndexed { i, pl ->
                val r = sorted[i].first.translate(-origin.x, -origin.y)
                val w = pl.width.toFloat()
                val h = pl.height.toFloat()
                fun fit(x: Float, y: Float) = Rect(x.coerceIn(0f, max(0f, width - w)), y.coerceIn(0f, max(0f, height - h)), 0f, 0f).let {
                    Rect(it.left, it.top, it.left + w, it.top + h)
                }
                val candidates = listOf(
                    fit(r.center.x - w / 2f, r.bottom + gap),
                    fit(r.center.x - w / 2f, r.top - gap - h),
                    fit(r.right + gap, r.center.y - h / 2f),
                    fit(r.left - gap - w, r.center.y - h / 2f),
                    fit(r.left + 4.dp.toPx(), r.top + 4.dp.toPx()),
                )
                val spot = candidates.firstOrNull { c -> placed.none { it.overlaps(c) } }
                if (spot != null) placed += spot
                spot?.let { it.left.roundToInt() to it.top.roundToInt() }
            }
            layout(constraints.maxWidth, constraints.maxHeight) {
                placeables.forEachIndexed { i, pl -> positions[i]?.let { (x, y) -> pl.place(x, y) } }
            }
        }
        Text(
            tr("Masquer les étiquettes"), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Neon.OnAccent,
            modifier = Modifier.align(androidx.compose.ui.Alignment.Center)
                .background(Neon.Cyan, RoundedCornerShape(20.dp)).clickable(onClick = hide)
                .padding(horizontal = 16.dp, vertical = 12.dp).testTag("help_labels_hide"),
        )
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
