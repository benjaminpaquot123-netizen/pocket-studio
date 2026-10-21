package com.pocketstudio.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import kotlin.math.roundToInt
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Width from which a screen shows two panes side by side (a tablet, or a big phone lying down). */
val TWO_PANES_FROM: Dp = 760.dp

/**
 * A screen of cards: one scrolling column on a phone; on a wide screen (tablet) two columns side
 * by side, each scrolling on its own, [first] on the left and [second] on the right, so nothing
 * is stretched across the whole width.
 */
@Composable
fun AdaptivePanes(
    modifier: Modifier = Modifier,
    spacing: Dp = 10.dp,
    padding: Dp = 12.dp,
    first: @Composable ColumnScope.() -> Unit,
    second: @Composable ColumnScope.() -> Unit,
    /** The phone's single column when its order differs from [first] then [second]. */
    phone: (@Composable ColumnScope.() -> Unit)? = null,
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        if (maxWidth >= TWO_PANES_FROM) {
            Row(Modifier.fillMaxSize().padding(horizontal = padding / 2), horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                Column(
                    Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(padding / 2, padding),
                    verticalArrangement = Arrangement.spacedBy(spacing), content = first,
                )
                Column(
                    Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(padding / 2, padding),
                    verticalArrangement = Arrangement.spacedBy(spacing), content = second,
                )
            }
        } else {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(padding),
                verticalArrangement = Arrangement.spacedBy(spacing),
            ) {
                if (phone != null) {
                    phone()
                } else {
                    first()
                    second()
                }
            }
        }
    }
}

/**
 * A screen meant to be read from top to bottom (lists, steps, settings): as wide as the screen on
 * a phone, a comfortable reading width centred on a tablet.
 */
@Composable
fun ReadingWidth(modifier: Modifier = Modifier, maxWidth: Dp = 720.dp, content: @Composable () -> Unit) {
    BoxWithConstraints(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        androidx.compose.foundation.layout.Box(Modifier.widthIn(max = maxWidth).fillMaxSize()) { content() }
    }
}

/**
 * [items] in as many columns as fit (each at least [minCellWidth] wide, at most [maxColumns]):
 * one under the other on a phone, side by side in a wide card or pane. Inside a column spaced by
 * [spacing], a single column looks exactly as if the items were written there directly.
 * With [masonry], each column stacks its own items (cards of different heights, no gaps);
 * otherwise items line up in rows (sliders, settings).
 */
@Composable
fun AdaptiveColumns(
    items: List<@Composable () -> Unit>,
    modifier: Modifier = Modifier,
    minCellWidth: Dp = 280.dp,
    maxColumns: Int = 2,
    spacing: Dp = 8.dp,
    columnSpacing: Dp = 16.dp,
    masonry: Boolean = false,
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val columns = ((maxWidth + columnSpacing) / (minCellWidth + columnSpacing)).toInt().coerceIn(1, maxColumns.coerceAtLeast(1))
        when {
            columns == 1 || items.size < 2 -> Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing)) {
                items.forEach { it() }
            }
            masonry -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(columnSpacing)) {
                for (c in 0 until columns) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing)) {
                        items.forEachIndexed { i, item -> if (i % columns == c) item() }
                    }
                }
            }
            else -> Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing)) {
                items.chunked(columns).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(columnSpacing)) {
                        row.forEach { item -> Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing)) { item() } }
                        repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

/**
 * Like aspectRatio([ratio]) on the width given, but never taller than [maxHeight]: the cells of a
 * grid (scenes, steps, pads) keep their phone shape, and stay compact on a wide tablet pane.
 */
fun Modifier.aspectRatioAtMost(ratio: Float, maxHeight: Dp): Modifier = layout { measurable, constraints ->
    val width = if (constraints.hasBoundedWidth) constraints.maxWidth else constraints.minWidth
    val height = minOf((width / ratio).roundToInt(), maxHeight.roundToPx())
        .coerceIn(constraints.minHeight, if (constraints.hasBoundedHeight) constraints.maxHeight else Int.MAX_VALUE)
    val placeable = measurable.measure(Constraints.fixed(width, height))
    layout(width, height) { placeable.place(0, 0) }
}
