package com.pocketstudio.app.synth

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
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pocketstudio.app.acid.noteName
import com.pocketstudio.app.settings.KeyLayout
import com.pocketstudio.model.Harmony
import com.pocketstudio.model.Scale
import com.pocketstudio.ui.Neon

/**
 * The keys to play, in the layout chosen above them: piano ([KeyLayout.TwoOctaves] wide, or one
 * octave from Do to Do with every note named) or the notes of the key only ([KeyLayout.Scale]).
 */
@Composable
internal fun PlayKeys(
    layout: KeyLayout,
    octave: Int,
    /** Octaves of the full piano (2 upright, 3 lying down). */
    wideOctaves: Int,
    harmony: Harmony,
    english: Boolean,
    onNoteOn: (Int, Float) -> Unit,
    onNoteOff: (Int) -> Unit,
    height: Dp,
    tag: String = "keyboard",
    keyTag: String = "key",
) {
    val first = 12 * (octave + 1)
    when (layout) {
        // The Do keys are named, to find one's way on the keyboard.
        KeyLayout.TwoOctaves -> Keyboard(
            firstNote = first, octaves = wideOctaves, onNoteOn = onNoteOn, onNoteOff = onNoteOff, height = height,
            harmony = harmony, tag = tag, keyTag = keyTag, label = { n -> if (n % 12 == 0) noteName(n, english) else null },
        )
        KeyLayout.OneOctave -> Keyboard(
            firstNote = first, octaves = 1, onNoteOn = onNoteOn, onNoteOff = onNoteOff, height = height,
            harmony = harmony, tag = tag, keyTag = keyTag, topC = true, label = { n -> noteName(n, english) },
        )
        KeyLayout.Scale -> ScaleKeys(first, harmony, english, onNoteOn, onNoteOff, height, tag, keyTag)
    }
}

/**
 * Only the notes of the song's key, from its root to the root an octave up, as wide bars that
 * carry their name (GarageBand's "Scale" keyboard, Push's "In key"): no wrong note possible.
 * Without a key, the white keys (Do major).
 */
@Composable
private fun ScaleKeys(
    octaveStart: Int,
    harmony: Harmony,
    english: Boolean,
    onNoteOn: (Int, Float) -> Unit,
    onNoteOff: (Int) -> Unit,
    height: Dp,
    tag: String,
    keyTag: String,
) {
    val scale = if (harmony.scale == Scale.Off) Scale.Major else harmony.scale
    val root = if (harmony.scale == Scale.Off) 0 else Math.floorMod(harmony.root, 12)
    val notes = scale.intervals.map { octaveStart + root + it } + (octaveStart + root + 12)
    Row(Modifier.fillMaxWidth().height(height).testTag(tag), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        notes.forEachIndexed { i, note ->
            ScaleKey(
                note = note, name = noteName(note, english), degree = i + 1, isRoot = i == 0 || i == notes.lastIndex,
                onNoteOn = onNoteOn, onNoteOff = onNoteOff, modifier = Modifier.weight(1f).fillMaxHeight().testTag("${keyTag}_$note"),
            )
        }
    }
}

@Composable
private fun ScaleKey(
    note: Int,
    name: String,
    degree: Int,
    isRoot: Boolean,
    onNoteOn: (Int, Float) -> Unit,
    onNoteOff: (Int) -> Unit,
    modifier: Modifier,
) {
    var down by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp)
    // The root (the note the song turns around) in the accent colour, the others ivory.
    val base = if (isRoot) lerp(Neon.Cyan, Color.White, 0.55f) else Color(0xFFF2F2EE)
    val face = if (down) Neon.Cyan else base
    Box(
        modifier
            .then(if (down) Modifier.shadow(12.dp, shape, ambientColor = Neon.Cyan, spotColor = Neon.Cyan) else Modifier)
            .clip(shape)
            .background(Brush.verticalGradient(0f to lerp(face, Color.Black, 0.05f), 0.9f to face, 1f to lerp(face, Color.Black, 0.2f)))
            .border(1.dp, Color.Black.copy(alpha = 0.22f), shape)
            .pointerInput(note) {
                detectTapGestures(onPress = { at ->
                    down = true
                    onNoteOn(note, (0.35f + 0.65f * at.y / size.height).coerceIn(0.2f, 1f))
                    try {
                        tryAwaitRelease()
                    } finally {
                        down = false
                        onNoteOff(note)
                    }
                })
            },
        contentAlignment = Alignment.BottomCenter,
    ) {
        val ink = if (down) Neon.OnAccent else Color(0xFF3C4047)
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(bottom = 8.dp)) {
            Text(name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = ink, maxLines = 1, softWrap = false)
            Text("$degree", style = MaterialTheme.typography.labelSmall, color = ink.copy(alpha = 0.6f))
        }
    }
}
