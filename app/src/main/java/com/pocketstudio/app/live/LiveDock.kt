package com.pocketstudio.app.live

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketstudio.app.learn.spot
import com.pocketstudio.app.ui.RepeatingIconButton
import com.pocketstudio.model.Project
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.ui.MonoStyle
import com.pocketstudio.ui.Neon
import kotlin.math.roundToInt

/**
 * Lot 4d: « − 113 + » in the Live page's bar, whatever the tab (held, − and + repeat; the figure
 * opens the tempo sheet). Too narrow for the three: the figure alone.
 */
@Composable
internal fun LiveTempo(bpm: Double, actions: LiveActions) = BoxWithConstraints(Modifier.testTag("live_tempo_box")) {
    val steps = maxWidth >= TEMPO_STEPS_WIDTH
    Row(
        Modifier.clip(RoundedCornerShape(12.dp)).background(Neon.SurfaceHigh),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (steps) {
            RepeatingIconButton({ actions.nudgeTempo(-1) }, Modifier.size(48.dp).testTag("live_tempo_minus")) {
                Icon(Icons.Filled.Remove, contentDescription = tr("Tempo moins"), modifier = Modifier.size(20.dp))
            }
        }
        val said = tr("Tempo : %d BPM", bpm.roundToInt())
        Column(
            Modifier.widthIn(min = 48.dp).requiredHeight(48.dp).clip(RoundedCornerShape(10.dp))
                .clickable(role = Role.Button, onClickLabel = tr("Changer le tempo"), onClick = actions.openTempo)
                .semantics(mergeDescendants = true) { contentDescription = said }
                .padding(horizontal = 4.dp).testTag("live_tempo").spot("live_tempo", tr("Tempo")),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("${bpm.roundToInt()}", style = MonoStyle.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold, lineHeight = 20.sp), color = Neon.Amber, maxLines = 1)
            Text("BPM", style = MaterialTheme.typography.labelSmall.copy(lineHeight = 12.sp), color = Neon.TextSecondary, maxLines = 1)
        }
        if (steps) {
            RepeatingIconButton({ actions.nudgeTempo(+1) }, Modifier.size(48.dp).testTag("live_tempo_plus")) {
                Icon(Icons.Filled.Add, contentDescription = tr("Tempo plus"), modifier = Modifier.size(20.dp))
            }
        }
    }
}

/** Under this width the bar's tempo keeps its figure alone. */
private val TEMPO_STEPS_WIDTH = 150.dp

/**
 * « Lancer à » (lot 4d: over the grid, beside « Cases »): touching it goes 1 → 2 → 4 bars → a beat
 * → half a bar.
 */
@Composable
internal fun QuantumButton(p: Project, setQuantum: (Int) -> Unit, modifier: Modifier = Modifier) {
    val code = liveQuantumCode(p)
    val next = LIVE_QUANTUM_CYCLE[(LIVE_QUANTUM_CYCLE.indexOf(code) + 1).mod(LIVE_QUANTUM_CYCLE.size)]
    Row(
        modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(12.dp)).background(GridLook.Panel)
            .clickable { setQuantum(next) }.padding(horizontal = 10.dp).testTag("live_quantum")
            .spot("live_quantum", tr("Quand ça part")),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(tr("Lancer à"), style = MaterialTheme.typography.labelLarge, color = Neon.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
        Text(liveQuantumLabel(code), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Neon.TextPrimary, maxLines = 1)
    }
}
