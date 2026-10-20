package com.pocketstudio.app.session

import com.pocketstudio.app.help.Glossary
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.pillColors
import com.pocketstudio.ui.NeonCard
import com.pocketstudio.ui.SectionLabel

/** Parameter numbers of the engine's PerformanceFx. */
object Perf {
    const val FILTER = 0
    const val RESONANCE = 1
    const val REPEAT = 2
    const val DIVISION = 3
    const val TAPE_STOP = 4
    const val CHOP = 5
}

/**
 * Live effects on the whole mix: an XY pad (left = low-pass, right = high-pass, up = resonance,
 * springs back to neutral) and hold-to-play buttons. Nothing here is saved in the project.
 */
@Composable
fun LivePad(set: (param: Int, value: Float) -> Unit) {
    var touch by remember { mutableStateOf<Offset?>(null) }
    var division by rememberSaveable { mutableIntStateOf(2) }
    NeonCard(accent = Neon.Acid, modifier = Modifier.testTag("live")) {
        SectionLabel("Effets live", color = Neon.Acid, help = Glossary.PERFORMANCE)
        Text(
            "Glisse sur le pad : à gauche ça coupe les aigus, à droite les graves, en haut ça résonne. Lâche : tout revient.",
            style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary,
        )
        val padColor = Neon.Acid
        val grid = Neon.Outline
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1.6f)
                .clip(RoundedCornerShape(12.dp))
                .background(Neon.Surface)
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.22f), Color.Black.copy(alpha = 0.4f))))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                .testTag("xy_pad")
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        fun send(o: Offset) {
                            val x = (o.x / size.width).coerceIn(0f, 1f)
                            val y = (1f - o.y / size.height).coerceIn(0f, 1f)
                            touch = Offset(x, y)
                            set(Perf.RESONANCE, y)
                            set(Perf.FILTER, x)
                        }
                        send(down.position)
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            if (change.positionChange() != Offset.Zero) send(change.position)
                            change.consume()
                        }
                        touch = null
                        set(Perf.FILTER, 0.5f)
                    }
                },
        ) {
            Canvas(Modifier.matchParentSize()) {
                // A screen with a fine grid of dots and the two centre lines.
                for (gx in 1 until 8) for (gy in 1 until 5) {
                    drawCircle(grid.copy(alpha = 0.6f), radius = 1.5f, center = Offset(size.width * gx / 8, size.height * gy / 5))
                }
                drawLine(grid, Offset(size.width / 2, 0f), Offset(size.width / 2, size.height), strokeWidth = 2f)
                drawLine(grid, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), strokeWidth = 1f)
                touch?.let { t ->
                    val c = Offset(t.x * size.width, (1f - t.y) * size.height)
                    // Crosshair, then a glow around the finger.
                    drawLine(padColor.copy(alpha = 0.35f), Offset(c.x, 0f), Offset(c.x, size.height), strokeWidth = 1.5f)
                    drawLine(padColor.copy(alpha = 0.35f), Offset(0f, c.y), Offset(size.width, c.y), strokeWidth = 1.5f)
                    drawCircle(Brush.radialGradient(listOf(padColor.copy(alpha = 0.55f), Color.Transparent), center = c, radius = 140f), radius = 140f, center = c)
                    drawCircle(padColor, radius = 22f, center = c)
                    drawCircle(Color.White.copy(alpha = 0.8f), radius = 8f, center = c)
                }
            }
            Text("Passe-bas", Modifier.align(Alignment.BottomStart).padding(8.dp), style = MaterialTheme.typography.labelSmall, color = Neon.TextSecondary)
            Text("Passe-haut", Modifier.align(Alignment.BottomEnd).padding(8.dp), style = MaterialTheme.typography.labelSmall, color = Neon.TextSecondary)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Répéter :", style = MaterialTheme.typography.bodyMedium)
            listOf("1/4", "1/8", "1/16", "1/32").forEachIndexed { i, label ->
                FilterChip(
                    selected = division == i,
                    onClick = { division = i; set(Perf.DIVISION, (i + 0.5f) / 4f) },
                    label = { Text(label) },
                    colors = pillColors(Neon.Acid),
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HoldButton("Répéter", Modifier.weight(1f), "hold_repeat") { on ->
                if (on) set(Perf.DIVISION, (division + 0.5f) / 4f)
                set(Perf.REPEAT, if (on) 1f else 0f)
            }
            HoldButton("Hacher", Modifier.weight(1f), "hold_chop") { on -> set(Perf.CHOP, if (on) 1f else 0f) }
            HoldButton("Stop bande", Modifier.weight(1f), "hold_stop") { on -> set(Perf.TAPE_STOP, if (on) 1f else 0f) }
        }
    }
}

/** Active only while the finger stays on it. */
@Composable
private fun HoldButton(label: String, modifier: Modifier, tag: String, hold: (Boolean) -> Unit) {
    var held by remember { mutableStateOf(false) }
    Box(
        modifier
            .height(64.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (held) Neon.Acid else Neon.SurfaceHigh)
            .testTag(tag)
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    held = true
                    hold(true)
                    tryAwaitRelease()
                    held = false
                    hold(false)
                })
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (held) Neon.Background else Neon.TextPrimary, style = MaterialTheme.typography.titleSmall)
    }
}
