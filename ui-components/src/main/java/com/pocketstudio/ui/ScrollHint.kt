package com.pocketstudio.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.model.i18n.trCount
import kotlinx.coroutines.launch

/**
 * Rule 17 of docs/DESIGN.md, « Tout défilement se voit » (hands-on audit G9; Benjamin, 2026-11-06: « la carte suivante est
 * coupée en bas mais rien ne le signale »): a list taller than the screen fades out at its bottom edge and carries a small
 * floating chip « ↓ 4 autres » while it is at its top and something is below. A touch on the chip brings the first of
 * them up; once the list is scrolled the chip is gone and the fade stays until the end of the list. The things counted are the ones marked [scrollHintItem] (the cards of a home screen);
 * none marked below but more to see: « ↓ La suite ».
 */
@Stable
class ScrollHintState(val scroll: ScrollState) {
    /** How many marked things are not wholly in view, below. */
    var hidden by mutableIntStateOf(0)
        private set

    // Layout objects, read and written on the main thread in the layout callbacks only (no state: nothing recomposes on them).
    internal var viewport: LayoutCoordinates? = null
    internal val items = LinkedHashMap<Any, LayoutCoordinates>()

    /** Where the first thing below starts, in the list's own pixels (null: none marked). */
    private var firstBelow: Float? = null

    internal fun recount() {
        val v = viewport?.takeIf { it.isAttached } ?: return
        val height = v.size.height
        var n = 0
        var first: Float? = null
        for (c in items.values) {
            if (!c.isAttached) continue
            val top = v.localPositionOf(c, Offset.Zero).y
            if (top + c.size.height > height + 1f) {
                n++
                val inList = top + scroll.value
                if (first == null || inList < first) first = inList
            }
        }
        firstBelow = first
        if (hidden != n) hidden = n
    }

    /** Brings the next thing below up to the top of the list (a screen's worth further when none is marked). */
    internal suspend fun next(still: Boolean, margin: Float) {
        val page = (viewport?.size?.height ?: 0).toFloat()
        val target = (firstBelow?.minus(margin)?.takeIf { it > scroll.value + 1f } ?: (scroll.value + page * 0.8f))
            .toInt().coerceIn(0, scroll.maxValue)
        if (still) scroll.scrollTo(target) else scroll.animateScrollTo(target)
    }
}

@Composable
fun rememberScrollHint(scroll: ScrollState = rememberScrollState()): ScrollHintState = remember(scroll) { ScrollHintState(scroll) }

/** One of the things the chip counts (a card), wherever it is inside the list. */
@Composable
fun Modifier.scrollHintItem(state: ScrollHintState): Modifier {
    val key = remember { Any() }
    DisposableEffect(state, key) { onDispose { state.items.remove(key) } }
    return this.onGloballyPositioned { state.items[key] = it; state.recount() }
}

/**
 * The list's frame: [content] is the column that scrolls with [state]'s [ScrollHintState.scroll] (as big as this box); the
 * fade and the chip lie over its bottom edge. [bottom]: how far above the frame's bottom the chip floats.
 */
@Composable
fun ScrollHintBox(state: ScrollHintState, modifier: Modifier = Modifier, bottom: Dp = 12.dp, content: @Composable BoxScope.() -> Unit) {
    val more = state.scroll.canScrollForward
    Box(modifier.onGloballyPositioned { state.viewport = it; state.recount() }) {
        Box(Modifier.fadingVerticalEdges(state.scroll, SCROLL_FADE), content = content)
        if (more) {
            // The fade's band, for the screen scans: what lies under it is meant to be faded (LayoutScan).
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(SCROLL_FADE).testTag("scroll_hint_fade"))
            // The chip only while the list is at its top: once scrolled, the list has said it scrolls, and a card brought
            // up to the bottom edge is never under the chip (a touch meant for it would scroll instead). The fade stays.
            // In the bottom-end corner, as a chat's « ↓ »: a touch at the middle of a card is never taken by the chip.
            if (state.scroll.value == 0) ScrollHintChip(state, Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = bottom))
        }
    }
}

/** The fade at the bottom edge of a list that goes on. */
val SCROLL_FADE: Dp = 24.dp

@Composable
private fun ScrollHintChip(state: ScrollHintState, modifier: Modifier) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val still = LocalReducedMotion.current || remember { systemAnimationsOff(context) }
    val margin = with(LocalDensity.current) { 12.dp.toPx() }
    val n = state.hidden
    val words = if (n > 0) trCount(n, "1 autre", "%d autres") else tr("La suite")
    val shape = RoundedCornerShape(50)
    Row(
        modifier.heightIn(min = 48.dp).shadow(6.dp, shape).clip(shape).background(Neon.SurfaceHigh).border(1.dp, Neon.Outline, shape)
            .clickable(role = Role.Button, onClickLabel = tr("Faire défiler")) { scope.launch { state.next(still, margin) } }
            .padding(horizontal = 16.dp)
            .testTag("scroll_hint"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text("↓ $words", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = Neon.TextPrimary, maxLines = 1,
            modifier = Modifier.testTag("scroll_hint_words"))
    }
}
