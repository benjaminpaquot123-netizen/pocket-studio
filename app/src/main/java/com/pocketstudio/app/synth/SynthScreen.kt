package com.pocketstudio.app.synth

import com.pocketstudio.app.instruments.InstrumentPanel
import com.pocketstudio.app.instruments.InstrumentUi
import com.pocketstudio.model.Scale
import com.pocketstudio.model.Harmony
import com.pocketstudio.model.NoteClip
import com.pocketstudio.model.Note
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pocketstudio.model.SynthPatch
import com.pocketstudio.app.sounds.SoundBrowser
import com.pocketstudio.app.sounds.SoundBrowserUi
import com.pocketstudio.ui.MonoStyle
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.NeonCard
import com.pocketstudio.ui.SectionLabel
import kotlin.math.roundToInt

data class SynthActions(
    val noteOn: (Int) -> Unit = {},
    val noteOff: (Int) -> Unit = {},
    val shiftOctave: (Int) -> Unit = {},
    val setParam: (Int, Float) -> Unit = { _, _ -> },
    val applyPreset: (SynthPatch) -> Unit = {},
    val chordOn: (Int) -> Unit = {},
    val chordOff: (Int) -> Unit = {},
)

private val EASY = listOf(
    SynthPatch.CUTOFF to "Coupure", SynthPatch.RESONANCE to "Résonance", SynthPatch.FILTER_ENV to "Enveloppe du filtre",
    SynthPatch.A_ATTACK to "Attaque", SynthPatch.A_RELEASE to "Relâchement", SynthPatch.OSC2_DETUNE to "Épaisseur (désaccord)",
    SynthPatch.SUB to "Sous-basse", SynthPatch.LEVEL to "Volume",
)

private val ADVANCED = listOf(
    SynthPatch.OSC_MIX to "Mélange oscillateurs", SynthPatch.NOISE to "Bruit",
    SynthPatch.F_ATTACK to "Filtre : attaque", SynthPatch.F_DECAY to "Filtre : déclin",
    SynthPatch.F_SUSTAIN to "Filtre : maintien", SynthPatch.F_RELEASE to "Filtre : relâchement",
    SynthPatch.A_DECAY to "Volume : déclin", SynthPatch.A_SUSTAIN to "Volume : maintien",
    SynthPatch.LFO1_RATE to "Vibrato : vitesse", SynthPatch.VIBRATO to "Vibrato : profondeur",
    SynthPatch.LFO2_RATE to "Balayage : vitesse", SynthPatch.SWEEP to "Balayage du filtre",
)

private val WAVES = listOf(0f to "Scie", 0.5f to "Carré", 1f to "Triangle")
private val FILTERS = listOf(0f to "Ladder", 0.3f to "Passe-bas", 0.6f to "Passe-haut", 0.9f to "Passe-bande")

/** The piano roll's data, when the screen shows it (null: keyboard only, e.g. in tests). */
class NotesUi(
    val clip: NoteClip,
    val ghosts: List<Note>,
    val playStep: Int,
    val actions: PianoRollActions,
    val harmony: Harmony = Harmony(),
)

@Composable
fun SynthScreen(
    patch: SynthPatch,
    octave: Int,
    actions: SynthActions,
    modifier: Modifier = Modifier,
    notes: NotesUi? = null,
    startWithNotes: Boolean = false,
    instrument: InstrumentUi? = null,
    browser: SoundBrowserUi? = null,
    transcribe: TranscribeUi? = null,
) {
    var advanced by rememberSaveable { mutableStateOf(false) }
    var showNotes by rememberSaveable { mutableStateOf(startWithNotes) }
    var chordKeys by rememberSaveable { mutableStateOf(false) }
    val harmony = notes?.harmony ?: Harmony()
    // One finger, one chord of the scale (keyboard "Accords" switch).
    val keyOn: (Int) -> Unit = if (chordKeys) actions.chordOn else actions.noteOn
    val keyOff: (Int) -> Unit = if (chordKeys) actions.chordOff else actions.noteOff
    BoxWithConstraints(modifier.fillMaxSize().testTag("synth")) {
        val keyboardHeight = (maxHeight * 0.45f).coerceIn(110.dp, 220.dp)
        val landscape = maxWidth > maxHeight
        val rollHeight = if (landscape) (maxHeight - 140.dp).coerceAtLeast(160.dp) else 380.dp
        if (notes != null && showNotes) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(if (landscape) 8.dp else 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ModeSwitch(showNotes) { showNotes = it }
                NeonCard {
                    PianoRoll(
                        notes.clip, notes.ghosts, notes.playStep, notes.actions,
                        gridHeight = rollHeight,
                        harmony = notes.harmony,
                    )
                }
                transcribe?.let { TranscribeCard(it) }
                NeonCard {
                    SectionLabel("Harmonie")
                    HarmonyPanel(notes.harmony, notes.clip.performance, notes.actions)
                }
                instrument?.let { InstrumentPanel(it) }
                if (instrument?.current == null) {
                    PresetsCard(patch, actions, browser)
                    if (instrument?.synthEngine != 1) SoundCard(patch, actions, advanced) { advanced = it }
                }
            }
        } else if (landscape) {
            // Landscape: one compact line (presets, octave), the settings scroll, and a wide
            // three-octave keyboard stays under the thumbs.
            Column(Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (notes != null) ModeSwitch(showNotes) { showNotes = it }
                    PresetChips(patch, actions)
                    Box(Modifier.weight(1f))
                    ChordChip(chordKeys) { chordKeys = it }
                    OctaveControl(octave, actions)
                }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    instrument?.let { InstrumentPanel(it) }
                    if (instrument?.current == null && instrument?.synthEngine != 1) SoundCard(patch, actions, advanced) { advanced = it }
                }
                Keyboard(
                    firstNote = 12 * (octave + 1), octaves = 3,
                    onNoteOn = keyOn, onNoteOff = keyOff, height = keyboardHeight, harmony = harmony,
                )
            }
        } else {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (notes != null) ModeSwitch(showNotes) { showNotes = it }
                KeyboardCard(octave, octaves = 2, height = 150.dp, actions = actions, harmony = harmony, chordKeys = chordKeys,
                    onChordKeys = { chordKeys = it }, keyOn = keyOn, keyOff = keyOff)
                instrument?.let { InstrumentPanel(it) }
                if (instrument?.current == null) {
                    PresetsCard(patch, actions, browser)
                    if (instrument?.synthEngine != 1) SoundCard(patch, actions, advanced) { advanced = it }
                }
            }
        }
    }
}

@Composable
private fun KeyboardCard(
    octave: Int,
    octaves: Int,
    height: Dp,
    actions: SynthActions,
    harmony: Harmony,
    chordKeys: Boolean,
    onChordKeys: (Boolean) -> Unit,
    keyOn: (Int) -> Unit,
    keyOff: (Int) -> Unit,
) {
    NeonCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("Clavier")
            Box(Modifier.weight(1f))
            ChordChip(chordKeys, onChordKeys)
            OctaveControl(octave, actions)
        }
        Keyboard(firstNote = 12 * (octave + 1), octaves = octaves, onNoteOn = keyOn, onNoteOff = keyOff, height = height, harmony = harmony)
    }
}

@Composable
private fun ChordChip(on: Boolean, onChange: (Boolean) -> Unit) {
    FilterChip(selected = on, onClick = { onChange(!on) }, label = { Text("Accords") }, colors = chipColors(),
        modifier = Modifier.testTag("chord_keys"))
}

@Composable
private fun PresetsCard(patch: SynthPatch, actions: SynthActions, browser: SoundBrowserUi?) {
    NeonCard {
        SectionLabel("Sons prêts à jouer")
        if (browser != null) SoundBrowser(browser)
        else Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { PresetChips(patch, actions) }
    }
}

@Composable
private fun SoundCard(patch: SynthPatch, actions: SynthActions, advanced: Boolean, setAdvanced: (Boolean) -> Unit) {
    NeonCard {
        SectionLabel("Son")
        Text("Forme d'onde", style = MaterialTheme.typography.bodyMedium)
        ChoiceRow(WAVES, patch.get(SynthPatch.OSC1_WAVE)) { v ->
            actions.setParam(SynthPatch.OSC1_WAVE, v)
            actions.setParam(SynthPatch.OSC2_WAVE, v)
        }
        EASY.forEach { (index, label) -> ParamSlider(label, patch.get(index)) { actions.setParam(index, it) } }
        TextButton(onClick = { setAdvanced(!advanced) }) {
            Icon(if (advanced) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null)
            Text(if (advanced) "Masquer les réglages avancés" else "Réglages avancés")
        }
        AnimatedVisibility(visible = advanced) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Oscillateur 2", style = MaterialTheme.typography.bodyMedium)
                ChoiceRow(WAVES, patch.get(SynthPatch.OSC2_WAVE)) { actions.setParam(SynthPatch.OSC2_WAVE, it) }
                ChoiceRow(listOf(0f to "Octave −1", 0.5f to "Même octave", 1f to "Octave +1"), patch.get(SynthPatch.OSC2_OCTAVE)) {
                    actions.setParam(SynthPatch.OSC2_OCTAVE, it)
                }
                Text("Type de filtre", style = MaterialTheme.typography.bodyMedium)
                ChoiceRow(FILTERS, patch.get(SynthPatch.FILTER_TYPE)) { actions.setParam(SynthPatch.FILTER_TYPE, it) }
                ADVANCED.forEach { (index, label) -> ParamSlider(label, patch.get(index)) { actions.setParam(index, it) } }
            }
        }
    }
}

@Composable
private fun ModeSwitch(showNotes: Boolean, onChange: (Boolean) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        FilterChip(selected = !showNotes, onClick = { onChange(false) }, label = { Text("Clavier") }, colors = chipColors(),
            modifier = Modifier.testTag("mode_keyboard"))
        FilterChip(selected = showNotes, onClick = { onChange(true) }, label = { Text("Notes") }, colors = chipColors(),
            modifier = Modifier.testTag("mode_notes"))
    }
}

@Composable
private fun OctaveControl(octave: Int, actions: SynthActions) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { actions.shiftOctave(-1) }) { Icon(Icons.Filled.Remove, contentDescription = "Octave plus bas") }
        Text("Octave $octave", style = MonoStyle, color = Neon.Cyan)
        IconButton(onClick = { actions.shiftOctave(1) }) { Icon(Icons.Filled.Add, contentDescription = "Octave plus haut") }
    }
}

@Composable
private fun PresetChips(patch: SynthPatch, actions: SynthActions) {
    SynthPatch.PRESETS.forEach { (name, preset) ->
        FilterChip(
            selected = patch.sanitized() == preset.sanitized(),
            onClick = { actions.applyPreset(preset) },
            label = { Text(name) },
            colors = chipColors(),
            modifier = Modifier.testTag("preset_$name"),
        )
    }
}

@Composable
private fun chipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = Neon.Cyan.copy(alpha = 0.25f),
    selectedLabelColor = Neon.Cyan,
)

/** Picks the option whose stored value is closest to [current]. */
@Composable
private fun ChoiceRow(options: List<Pair<Float, String>>, current: Float, onPick: (Float) -> Unit) {
    val selected = options.minBy { kotlin.math.abs(it.first - current) }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { option ->
            FilterChip(selected = option == selected, onClick = { onPick(option.first) }, label = { Text(option.second) }, colors = chipColors())
        }
    }
}

@Composable
private fun ParamSlider(label: String, value: Float, onChange: (Float) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text("${(value * 100).roundToInt()}", style = MonoStyle, color = Neon.Cyan)
    }
    Slider(value = value, onValueChange = onChange)
}

private val BLACK_KEY_AFTER_WHITE = setOf(0, 1, 3, 4, 5)  // C# D# F# G# A# sit after C D F G A
private val WHITE_OFFSETS = listOf(0, 2, 4, 5, 7, 9, 11)

/** Multi-touch piano keyboard: every key handles its own finger. */
@Composable
fun Keyboard(
    firstNote: Int,
    octaves: Int,
    onNoteOn: (Int) -> Unit,
    onNoteOff: (Int) -> Unit,
    height: Dp = 150.dp,
    harmony: Harmony = Harmony(),
) {
    val whiteCount = 7 * octaves
    BoxWithConstraints(Modifier.fillMaxWidth().height(height).testTag("keyboard")) {
        val whiteWidth = maxWidth / whiteCount
        Row(Modifier.fillMaxSize()) {
            for (w in 0 until whiteCount) {
                val note = firstNote + 12 * (w / 7) + WHITE_OFFSETS[w % 7]
                Key(note, isBlack = false, Modifier.width(whiteWidth).fillMaxHeight(), onNoteOn, onNoteOff, harmony)
            }
        }
        for (w in 0 until whiteCount) {
            if (w % 7 !in BLACK_KEY_AFTER_WHITE) continue
            val note = firstNote + 12 * (w / 7) + WHITE_OFFSETS[w % 7] + 1
            Key(
                note,
                isBlack = true,
                Modifier.offset(x = whiteWidth * (w + 1) - whiteWidth * 0.32f).width(whiteWidth * 0.64f).height(height * 0.61f),
                onNoteOn,
                onNoteOff,
                harmony,
            )
        }
    }
}

@Composable
private fun Key(
    note: Int,
    isBlack: Boolean,
    modifier: Modifier,
    onNoteOn: (Int) -> Unit,
    onNoteOff: (Int) -> Unit,
    harmony: Harmony = Harmony(),
) {
    var down by remember { mutableStateOf(false) }
    val outside = harmony.scale != Scale.Off && !harmony.inScale(note)
    val color = when {
        down -> Neon.Cyan
        isBlack && outside -> Color(0xFF15171A)
        isBlack -> Color(0xFF26292E)
        outside -> Color(0xFF9EA2AA)  // out of the scale: greyed, still playable
        Math.floorMod(note - harmony.root, 12) == 0 && harmony.scale != Scale.Off -> Color(0xFFDCEFF5)
        else -> Color(0xFFF2F3F5)
    }
    Box(
        modifier
            .padding(horizontal = 1.dp)
            .clip(RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp))
            .background(color)
            .border(1.dp, Neon.Outline, RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp))
            .pointerInput(note) {
                detectTapGestures(onPress = {
                    down = true
                    onNoteOn(note)
                    try {
                        tryAwaitRelease()
                    } finally {
                        // Also when the gesture is cancelled (octave change, tab switch): no stuck note.
                        down = false
                        onNoteOff(note)
                    }
                })
            }
            .testTag("key_$note"),
    )
}
