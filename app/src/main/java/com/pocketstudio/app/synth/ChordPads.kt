package com.pocketstudio.app.synth

import com.pocketstudio.model.i18n.tr

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pocketstudio.model.ChordPad
import com.pocketstudio.model.Harmony
import com.pocketstudio.model.chordPads
import com.pocketstudio.ui.Neon

/**
 * Chords on pads: eight pads, the scale's chords from I to VII and I an octave up. One finger,
 * one full chord that fits the key (with the arpeggiator on, its notes play one after another).
 */
@Composable
fun ChordPads(
    harmony: Harmony,
    octaveRoot: Int,
    onNoteOn: (Int, Float) -> Unit,
    onNoteOff: (Int) -> Unit,
    height: Dp = 150.dp,
    english: Boolean = com.pocketstudio.app.settings.englishNotes(),
) {
    val pads = harmony.chordPads(octaveRoot)
    // Pads share notes (I and iii share two): a note sounds while any pad holding it is down.
    val held = remember { HashMap<Int, Int>() }
    val on: (Int, Float) -> Unit = { n, v ->
        val count = held[n] ?: 0
        held[n] = count + 1
        if (count == 0) onNoteOn(n, v)
    }
    val off: (Int) -> Unit = { n ->
        val count = held[n] ?: 0
        if (count <= 1) {
            held.remove(n)
            if (count == 1) onNoteOff(n)
        } else {
            held[n] = count - 1
        }
    }
    Column(Modifier.fillMaxWidth().testTag("chord_pads"), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // The chords of the scale the pads play: the minor's for the minor pentatonic and the blues, the major's for « Libre ».
        val key = harmony.chordKey
        Text(tr("Accords de %s %s", key.keyName(english), tr(key.scale.label).lowercase()), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary,
            modifier = Modifier.testTag("chord_pads_title"))
        for (row in 0 until 2) {
            Row(Modifier.fillMaxWidth().height((height - 24.dp) / 2), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                pads.drop(row * 4).take(4).forEach { pad -> ChordPadKey(pad, english, Modifier.weight(1f).fillMaxHeight(), on, off) }
            }
        }
    }
}

@Composable
private fun ChordPadKey(pad: ChordPad, english: Boolean, modifier: Modifier, onNoteOn: (Int, Float) -> Unit, onNoteOff: (Int) -> Unit) {
    var down by remember { mutableStateOf(false) }
    // Tonic chords (I) stand out: they sound like "home".
    val home = pad.degree % 7 == 0
    Box(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (down) Neon.Cyan else if (home) Neon.SurfaceHigh else Neon.Surface)
            .border(1.dp, if (home) Neon.Cyan else Neon.Outline, RoundedCornerShape(10.dp))
            .pointerInput(pad) {
                detectTapGestures(onPress = {
                    down = true
                    pad.notes.forEach { onNoteOn(it, 0.8f) }
                    try {
                        tryAwaitRelease()
                    } finally {
                        down = false
                        pad.notes.forEach(onNoteOff)
                    }
                })
            }
            .testTag("chord_pad_${pad.degree}"),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(pad.roman, style = MaterialTheme.typography.titleMedium, color = if (down) Neon.Background else Neon.TextPrimary)
            Text(pad.name(english), style = MaterialTheme.typography.bodySmall, color = if (down) Neon.Background else Neon.TextSecondary)
        }
    }
}
