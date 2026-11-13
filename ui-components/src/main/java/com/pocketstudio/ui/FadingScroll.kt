package com.pocketstudio.ui

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * Fades the side of a row where more content waits (off the start or the end), instead of a hard
 * cut through a word: it says "this scrolls" the way polished apps do. Put it before the scroll.
 * Tour 6, lot 5 (R19, « la ligne G du Live » in Arabic): a row starts on the right in a right-to-left
 * language, so what waits further on is on the LEFT there; the fade was drawn on the right, over the
 * row's first item, and the far side was cut bare.
 */
fun Modifier.fadingEdges(state: ScrollableState, fade: Dp = 28.dp): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val f = fade.toPx().coerceAtMost(size.width / 3)
        val rtl = layoutDirection == LayoutDirection.Rtl
        val left = if (rtl) state.canScrollForward else state.canScrollBackward
        val right = if (rtl) state.canScrollBackward else state.canScrollForward
        if (left) {
            drawRect(Brush.horizontalGradient(listOf(Color.Transparent, Color.Black), startX = 0f, endX = f), size = Size(f, size.height), blendMode = BlendMode.DstIn)
        }
        if (right) {
            drawRect(
                Brush.horizontalGradient(listOf(Color.Black, Color.Transparent), startX = size.width - f, endX = size.width),
                topLeft = Offset(size.width - f, 0f), size = Size(f, size.height), blendMode = BlendMode.DstIn,
            )
        }
    }

/** A horizontal scroll with [fadingEdges]. */
@Composable
fun Modifier.fadingHorizontalScroll(): Modifier {
    val state = rememberScrollState()
    return fadingEdges(state).horizontalScroll(state)
}

/**
 * The top of a scrolled column fades out (once it is scrolled), so what passes under a fixed bar
 * above it (the « Partie : A » line) melts away instead of showing cut in half against that bar.
 */
fun Modifier.fadingTopEdge(state: ScrollableState, fade: Dp = 28.dp): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        if (state.canScrollBackward) {
            val f = fade.toPx().coerceAtMost(size.height / 3)
            drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black), startY = 0f, endY = f), size = Size(size.width, f), blendMode = BlendMode.DstIn)
        }
    }

/** [fadingEdges] for a column: the top or the bottom fades where more waits (a cut row, cut knobs). */
fun Modifier.fadingVerticalEdges(state: ScrollableState, fade: Dp = 24.dp): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val f = fade.toPx().coerceAtMost(size.height / 3)
        if (state.canScrollBackward) {
            drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black), startY = 0f, endY = f), size = Size(size.width, f), blendMode = BlendMode.DstIn)
        }
        if (state.canScrollForward) {
            drawRect(
                Brush.verticalGradient(listOf(Color.Black, Color.Transparent), startY = size.height - f, endY = size.height),
                topLeft = Offset(0f, size.height - f), size = Size(size.width, f), blendMode = BlendMode.DstIn,
            )
        }
    }
