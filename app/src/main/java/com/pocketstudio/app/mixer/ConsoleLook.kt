package com.pocketstudio.app.mixer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.pocketstudio.ui.Neon

/*
 * The mixing desk's finish (lot « design pro »), shared by the Mode simple's Mix and the console:
 * strips lit from above with a fine light edge, fader caps machined in two tones with grip lines,
 * and slots dug into the strip. The drawing helpers take only floats and colours: they allocate
 * nothing, so a meter redrawn 20 times a second costs no garbage.
 */

/** A strip of the desk: its panel a little lighter at the top, a faint edge of light ([outline]: the outline's, for the master). */
@Composable
internal fun Modifier.consoleStrip(shape: Shape, alpha: Float, outline: Boolean = false): Modifier {
    val high = Neon.SurfaceHigh
    val edge = if (outline) {
        Brush.verticalGradient(listOf(lerp(Neon.Outline, Color.White, 0.3f), Neon.Outline))
    } else {
        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.11f), Color.White.copy(alpha = 0.02f)))
    }
    return clip(shape)
        .background(Brush.verticalGradient(listOf(high.copy(alpha = (alpha + 0.12f).coerceAtMost(1f)), high.copy(alpha = (alpha - 0.1f).coerceAtLeast(0f)))))
        .border(1.dp, edge, shape)
}

/** A fader's slot [w] wide at [x] (its middle), from [top], [h] high: a dark groove with a light lip on its far side. */
internal fun DrawScope.drawFaderSlot(x: Float, top: Float, w: Float, h: Float, groove: Color) {
    drawRoundRect(groove, Offset(x - w / 2f, top), Size(w, h), CornerRadius(w / 2f))
    val lip = x + w / 2f + 0.5f
    drawLine(SLOT_LIP, Offset(lip, top + w / 2f), Offset(lip, top + h - w / 2f), 1f)
}

/**
 * A fader's cap [w] × [h] around ([x], [y]): a soft shadow under it, its [body] with the lower half
 * a shade darker (lit from above), two fine grip lines, and its mark across the middle ([mark], [markW] thick).
 */
internal fun DrawScope.drawFaderCap(x: Float, y: Float, w: Float, h: Float, body: Color, mark: Color, markW: Float) {
    val r = CornerRadius(4.dp.toPx())
    val left = x - w / 2f
    val top = y - h / 2f
    val one = 1.dp.toPx()
    // The shadow: wide and faint, then close and darker.
    drawRoundRect(CAP_SHADOW_FAR, Offset(left - one, top + one), Size(w + 2 * one, h + 3 * one), CornerRadius(r.x + one))
    drawRoundRect(CAP_SHADOW_NEAR, Offset(left, top + 2 * one), Size(w, h), r)
    drawRoundRect(body, Offset(left, top), Size(w, h), r)
    clipRect(top = y) { drawRoundRect(CAP_SHADE, Offset(left, top), Size(w, h), r) }
    // The edge of light along its top.
    drawLine(CAP_RIM, Offset(left + r.x, top + 0.5f), Offset(left + w - r.x, top + 0.5f), 1f)
    val inset = 4.dp.toPx()
    val grip = 3.5.dp.toPx()
    drawLine(CAP_GRIP, Offset(left + inset + one, y - grip), Offset(left + w - inset - one, y - grip), one)
    drawLine(CAP_GRIP, Offset(left + inset + one, y + grip), Offset(left + w - inset - one, y + grip), one)
    drawLine(mark, Offset(left + inset, y), Offset(left + w - inset, y), markW)
}

private val SLOT_LIP = Color.White.copy(alpha = 0.07f)
private val CAP_SHADOW_FAR = Color.Black.copy(alpha = 0.16f)
private val CAP_SHADOW_NEAR = Color.Black.copy(alpha = 0.4f)
private val CAP_SHADE = Color.Black.copy(alpha = 0.1f)
private val CAP_RIM = Color.White.copy(alpha = 0.9f)
private val CAP_GRIP = Color.Black.copy(alpha = 0.14f)
