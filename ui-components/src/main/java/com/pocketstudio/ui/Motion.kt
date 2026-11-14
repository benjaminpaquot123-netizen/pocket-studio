package com.pocketstudio.ui

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * The house motion (docs/DESIGN.md: short and useful, 150 to 250 ms): one spring for whatever
 * moves or changes size, one for colours and fades, one press. The springs are Material's
 * standard ones, so its own motion (sheets, menus, chips, switches) moves exactly as the rest.
 */
object StudioMotion {
    /** How far a touched control sinks: drawn smaller only, nothing around it moves. */
    const val PRESSED_SCALE = 0.97f

    /** The sink, quick enough that even a tap shows it. */
    const val PRESS_IN_MS = 90

    /** A finger that has not moved for this long is a press, not the start of a scroll. */
    const val PRESS_WAIT_MS = 60L

    /** A list's rows come in one after another, this far apart (its first opening only). */
    const val STAGGER_MS = 24

    /** Rows past this one come in with it: a long list does not keep arriving. */
    const val STAGGER_ROWS = 10

    /** How long a row takes to come in. */
    const val ENTER_MS = 220

    /** Movement and sizes (a sheet, a card folding, a tab settling, a figure rolling): about 200 ms, no visible bounce. */
    fun <T> spatial(): SpringSpec<T> = spring(dampingRatio = 0.9f, stiffness = 700f)
    fun <T> fastSpatial(): SpringSpec<T> = spring(dampingRatio = 0.9f, stiffness = 1400f)
    fun <T> slowSpatial(): SpringSpec<T> = spring(dampingRatio = 0.9f, stiffness = 300f)

    /** Colours and fades: never past the mark. */
    fun <T> effects(): SpringSpec<T> = spring(dampingRatio = 1f, stiffness = 1600f)
    fun <T> fastEffects(): SpringSpec<T> = spring(dampingRatio = 1f, stiffness = 3800f)
    fun <T> slowEffects(): SpringSpec<T> = spring(dampingRatio = 1f, stiffness = 800f)

    internal fun <T> pressIn(): TweenSpec<T> = tween(PRESS_IN_MS, easing = FastOutSlowInEasing)

    /** Back up after the touch: a spring with a hint of give, as a real key (under 0.5 % past rest). */
    internal fun <T> pressOut(): SpringSpec<T> = spring(dampingRatio = 0.55f, stiffness = 900f)

    internal fun <T> enter(delayMs: Int): TweenSpec<T> = tween(ENTER_MS, delayMillis = delayMs, easing = LinearOutSlowInEasing)
}

/** True when the phone's « Supprimer les animations » is on: the touch effects then stay still. */
val LocalReducedMotion = staticCompositionLocalOf { false }

/** The phone's « Supprimer les animations » (animator duration scale 0). */
fun systemAnimationsOff(context: Context): Boolean =
    runCatching { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }.getOrDefault(false)

/**
 * The control sinks a little while it is pressed ([StudioMotion.PRESSED_SCALE], 90 ms) and comes
 * back with a soft spring; a quick tap still shows the whole dip. Read from the control's own
 * [interactionSource] (its click decides what a press is). Drawn only: nothing is measured or
 * placed again, nothing is composed again, each frame only changes a layer's scale.
 */
fun Modifier.pressScale(interactionSource: InteractionSource, enabled: Boolean = true): Modifier =
    this then PressScaleElement(interactionSource, enabled)

/**
 * The same for a control whose touches cannot be read (a Material button, a pad drawn by hand):
 * the finger itself is followed, without taking anything from the control. A finger that moves
 * away (a scroll) leaves it still.
 */
fun Modifier.pressScale(enabled: Boolean = true): Modifier = this then PressScaleElement(null, enabled)

private class PressScaleElement(val source: InteractionSource?, val enabled: Boolean) : ModifierNodeElement<PressScaleNode>() {
    override fun create() = PressScaleNode(source, enabled)
    override fun update(node: PressScaleNode) = node.update(source, enabled)
    override fun equals(other: Any?) = other is PressScaleElement && other.source == source && other.enabled == enabled
    override fun hashCode() = 31 * (source?.hashCode() ?: 0) + enabled.hashCode()
    override fun InspectorInfo.inspectableProperties() {
        name = "pressScale"
        properties["enabled"] = enabled
    }
}

private class PressScaleNode(private var source: InteractionSource?, private var enabled: Boolean) :
    DelegatingNode(), LayoutModifierNode, CompositionLocalConsumerModifierNode {
    private var depth = Animatable(1f)
    private var motion: Job? = null
    private var watching: Job? = null

    // Read in the layer only: a frame of the press touches this layer's scale and nothing else.
    private val layer: GraphicsLayerScope.() -> Unit = {
        val s = depth.value
        scaleX = s
        scaleY = s
    }

    init {
        delegate(SuspendingPointerInputModifierNode { followFinger() })
    }

    fun update(source: InteractionSource?, enabled: Boolean) {
        this.enabled = enabled
        if (source != this.source) {
            this.source = source
            if (isAttached) watch()
        }
    }

    override fun onAttach() = watch()

    override fun onDetach() {
        motion = null
        watching = null
        // Shown again later (a list row coming back): at rest.
        depth = Animatable(1f)
    }

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) { placeable.placeWithLayer(0, 0, layerBlock = layer) }
    }

    private fun watch() {
        watching?.cancel()
        val s = source ?: return
        watching = coroutineScope.launch {
            var held = 0
            s.interactions.collect { i ->
                when (i) {
                    is PressInteraction.Press -> {
                        held++
                        sink()
                    }
                    is PressInteraction.Release, is PressInteraction.Cancel -> {
                        held = (held - 1).coerceAtLeast(0)
                        if (held == 0) rise()
                    }
                }
            }
        }
    }

    private fun sink() {
        if (!isAttached || !enabled || currentValueOf(LocalReducedMotion)) return
        motion?.cancel()
        val d = depth
        motion = coroutineScope.launch { d.animateTo(StudioMotion.PRESSED_SCALE, StudioMotion.pressIn()) }
    }

    private fun rise() {
        if (!isAttached) return
        val dip = motion
        val d = depth
        if (dip == null && d.value == 1f) return
        motion = coroutineScope.launch {
            // The dip ends first: a tap shorter than it still shows the control going down.
            dip?.join()
            d.animateTo(1f, StudioMotion.pressOut())
        }
    }

    private suspend fun PointerInputScope.followFinger() = awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        if (source != null || !enabled) return@awaitEachGesture
        val slop = viewConfiguration.touchSlop
        when (withTimeoutOrNull(StudioMotion.PRESS_WAIT_MS) { lifted(down, slop) }) {
            false -> Unit  // moved away: a scroll, not a press
            true -> {  // a quick tap: the whole dip, then back
                sink()
                rise()
            }
            null -> {
                sink()
                try {
                    lifted(down, slop)
                } finally {
                    rise()
                }
            }
        }
    }

    /** True once the finger is lifted, false as soon as it moves away. */
    private suspend fun AwaitPointerEventScope.lifted(down: PointerInputChange, slop: Float): Boolean {
        while (true) {
            val change = awaitPointerEvent(PointerEventPass.Final).changes.firstOrNull { it.id == down.id } ?: return true
            if (!change.pressed) return true
            if ((change.position - down.position).getDistanceSquared() > slop * slop) return false
        }
    }
}

/** How far new content rises as it settles in ([settleIn]). */
private val SETTLE_RISE = 6.dp

/**
 * New content under [key] (another tab, another page) settles in: it rises 6 dp and fades the
 * rest of the way in, on the house spring. Never on its very first showing (the screen opening)
 * and never with the phone's animations off. Drawn only: nothing is measured or placed again.
 */
fun Modifier.settleIn(key: Any?): Modifier = composed {
    val reduced = LocalReducedMotion.current
    val progress = remember { mutableFloatStateOf(1f) }
    val shown = remember { arrayOf(key) }
    if (shown[0] != key) {
        shown[0] = key
        // Set while composing: the new content's first frame is already drawn at the start.
        if (!reduced) progress.floatValue = 0f
    }
    LaunchedEffect(key) {
        if (progress.floatValue < 1f) animate(progress.floatValue, 1f, animationSpec = StudioMotion.spatial()) { v, _ -> progress.floatValue = v }
    }
    val rise = with(LocalDensity.current) { SETTLE_RISE.toPx() }
    graphicsLayer {
        val p = progress.floatValue
        if (p < 1f) {
            alpha = 0.4f + 0.6f * p
            translationY = (1f - p) * rise
        }
    }
}

/**
 * A list's first opening: its rows come in one after another, [StudioMotion.STAGGER_MS] apart
 * ([staggerIn]). Once played it stays played (kept with the screen's state), so a scroll, a
 * filter or coming back to the screen just shows the rows.
 */
@Stable
class Stagger internal constructor(private val played: BooleanArray) {
    /** Rows first composed in the opening frame come in; any later one just shows. */
    internal var opening = !played[0]
        private set

    internal fun close() {
        opening = false
        played[0] = true
    }
}

@Composable
fun rememberStagger(): Stagger {
    val reduced = LocalReducedMotion.current
    val played = rememberSaveable { booleanArrayOf(reduced) }
    val stagger = remember { Stagger(played) }
    LaunchedEffect(stagger) {
        // The next frame: every row of the opening is composed by then.
        androidx.compose.runtime.withFrameNanos { }
        stagger.close()
    }
    return stagger
}

/** How far a row rises as it comes in ([staggerIn]). */
private val ENTER_RISE = 8.dp

/** Row [index] of a list opened with [stagger]: it fades in and rises 8 dp, after the rows above it. */
fun Modifier.staggerIn(stagger: Stagger, index: Int): Modifier = composed {
    val progress = remember { mutableFloatStateOf(if (stagger.opening) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (progress.floatValue < 1f) {
            val delayMs = index.coerceIn(0, StudioMotion.STAGGER_ROWS) * StudioMotion.STAGGER_MS
            // A delay inside the animation (not a coroutine delay): driven by frames from the start.
            animate(0f, 1f, animationSpec = StudioMotion.enter(delayMs)) { v, _ -> progress.floatValue = v }
        }
    }
    val rise = with(LocalDensity.current) { ENTER_RISE.toPx() }
    graphicsLayer {
        val p = progress.floatValue
        if (p < 1f) {
            alpha = p
            translationY = (1f - p) * rise
        }
    }
}
