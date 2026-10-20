@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.pocketstudio.app.synth

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import com.pocketstudio.ui.HelpDot
import com.pocketstudio.ui.Help
import com.pocketstudio.app.help.Glossary
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import com.pocketstudio.ui.ProSlider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.pocketstudio.app.settings.SettingsStore
import com.pocketstudio.app.settings.KeyLayout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pocketstudio.model.SynthPatch
import com.pocketstudio.app.rhythm.PlayUi
import com.pocketstudio.app.sounds.SoundBrowser
import com.pocketstudio.app.sounds.SoundBrowserUi
import com.pocketstudio.ui.MonoStyle
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.pillColors
import com.pocketstudio.ui.NeonCard
import com.pocketstudio.ui.SectionLabel
import kotlin.math.roundToInt

data class SynthActions(
    val noteOn: (Int, Float) -> Unit = { _, _ -> },
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
    SynthPatch.SUB to "Sous-basse", SynthPatch.UNISON to "Unisson (supersaw)", SynthPatch.UNISON_DETUNE to "Écart de l'unisson",
    SynthPatch.LEVEL to "Volume",
)

/** Every synth parameter a knob can move, with its name (MIDI learn). */
internal val SYNTH_LABELS: List<Pair<Int, String>> get() = EASY + ADVANCED

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
    tracks: TracksUi? = null,
    play: PlayUi? = null,
    playActions: PlayActions = PlayActions(),
) {
    var advanced by rememberSaveable { mutableStateOf(false) }
    var showNotes by rememberSaveable { mutableStateOf(startWithNotes) }
    var chordKeys by rememberSaveable { mutableStateOf(false) }
    var chordPads by rememberSaveable { mutableStateOf(false) }
    // The keyboard layout and the note names are the user's, for every project.
    val context = LocalContext.current
    val settings by SettingsStore.state.collectAsState()
    val keyLayout = settings.keyLayout
    val setKeyLayout: (KeyLayout) -> Unit = { l -> SettingsStore.update(context) { it.copy(keyLayout = l) } }
    val english = settings.englishNotes
    val harmony = notes?.harmony ?: Harmony()
    // One finger, one chord of the scale (keyboard "Accords" switch).
    val keyOn: (Int, Float) -> Unit = if (chordKeys) { n, _ -> actions.chordOn(n) } else actions.noteOn
    val keyOff: (Int) -> Unit = if (chordKeys) actions.chordOff else actions.noteOff
    // The keyboard that follows: shown while sounds are being chosen and the real keyboard is out of reach.
    var keyboardInView by remember(showNotes) { mutableStateOf(true) }
    var soundsInView by remember(showNotes) { mutableStateOf(false) }
    var floatFolded by rememberSaveable { mutableStateOf(false) }
    val minSoundsPx = with(LocalDensity.current) { 64.dp.toPx() }
    val trackKeyboard = Modifier.onGloballyPositioned { keyboardInView = it.boundsInWindow().height >= it.size.height * 0.5f }
    val trackSounds = Modifier.onGloballyPositioned { soundsInView = it.boundsInWindow().height >= minOf(minSoundsPx, it.size.height * 0.5f) && it.size.height > 0 }
    BoxWithConstraints(modifier.fillMaxSize().testTag("synth")) {
        val keyboardHeight = (maxHeight * 0.45f).coerceIn(110.dp, 220.dp)
        val landscape = maxWidth > maxHeight
        val rollHeight = if (landscape) (maxHeight - 140.dp).coerceAtLeast(160.dp) else 380.dp
        val hasSounds = instrument != null || browser != null
        val follows = hasSounds && ((notes != null && showNotes) || !landscape)
        val floatKeys = if (landscape) 96.dp else 120.dp
        // Room under the last settings so the floating keyboard never hides them.
        val floatRoom = if (follows) floatKeys + 72.dp else 0.dp
        if (notes != null && showNotes) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(if (landscape) 8.dp else 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                tracks?.let { TrackBar(it) }
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
                    SectionLabel("Gamme et accords", help = Glossary.HARMONY)
                    HarmonyPanel(notes.harmony, notes.clip.performance, notes.actions, notes.clip.notes)
                }
                SoundArea(trackSounds) {
                    instrument?.let { InstrumentPanel(it) }
                    if (instrument?.current == null) {
                        PresetsCard(patch, actions, browser)
                        if (instrument?.synthEngine != 1) SoundCard(patch, actions, advanced) { advanced = it }
                    }
                }
                Spacer(Modifier.height(floatRoom))
            }
        } else if (landscape) {
            // Landscape: one compact line (presets, octave), the settings scroll, and a wide
            // three-octave keyboard stays under the thumbs.
            Column(Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                tracks?.let { TrackBar(it) }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (notes != null) ModeSwitch(showNotes) { showNotes = it }
                    PresetChips(patch, actions)
                    Box(Modifier.weight(1f))
                    ChordChip(chordKeys) { chordKeys = it }
                    ChordPadsChip(chordPads) { chordPads = it }
                    OctaveControl(octave, actions)
                }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    play?.let { PlayPanel(it, playActions) }
                    instrument?.let { InstrumentPanel(it) }
                    if (instrument?.current == null && instrument?.synthEngine != 1) SoundCard(patch, actions, advanced) { advanced = it }
                }
                if (chordPads) {
                    ChordPads(harmony, 12 * (octave + 1), actions.noteOn, actions.noteOff, height = keyboardHeight)
                } else {
                    PlayKeys(keyLayout, octave, 3, harmony, english, keyOn, keyOff, keyboardHeight)
                }
            }
        } else {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                tracks?.let { TrackBar(it) }
                if (notes != null) ModeSwitch(showNotes) { showNotes = it }
                KeyboardCard(octave, octaves = 2, height = 150.dp, actions = actions, harmony = harmony, chordKeys = chordKeys,
                    onChordKeys = { chordKeys = it }, keyOn = keyOn, keyOff = keyOff, play = play, playActions = playActions,
                    chordPads = chordPads, onChordPads = { chordPads = it }, modifier = trackKeyboard,
                    keyLayout = keyLayout, onKeyLayout = setKeyLayout, english = english)
                SoundArea(trackSounds) {
                    instrument?.let { InstrumentPanel(it) }
                    if (instrument?.current == null) {
                        PresetsCard(patch, actions, browser)
                        if (instrument?.synthEngine != 1) SoundCard(patch, actions, advanced) { advanced = it }
                    }
                }
                Spacer(Modifier.height(floatRoom))
            }
        }
        if (follows) {
            val wanted = soundsInView && (showNotes && notes != null || !keyboardInView)
            // Folding holds until the real keyboard is back: the next time it is out of reach, the floating one returns.
            LaunchedEffect(wanted) { if (!wanted) floatFolded = false }
            FloatingKeyboard(
                visible = wanted, folded = floatFolded, onFold = { floatFolded = it },
                octave = octave, octaves = if (landscape) 3 else 2, keysHeight = floatKeys, actions = actions, harmony = harmony,
                chordPads = chordPads, keyOn = keyOn, keyOff = keyOff, modifier = Modifier.align(Alignment.BottomCenter),
                keyLayout = keyLayout, english = english,
            )
        }
    }
}

/** The instrument, its ready-made sounds and its settings, in one block whose visibility is followed. */
@Composable
private fun SoundArea(track: Modifier, content: @Composable () -> Unit) {
    Column(track, verticalArrangement = Arrangement.spacedBy(10.dp)) { content() }
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
    keyOn: (Int, Float) -> Unit,
    keyOff: (Int) -> Unit,
    play: PlayUi? = null,
    playActions: PlayActions = PlayActions(),
    chordPads: Boolean = false,
    onChordPads: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
    keyLayout: KeyLayout = KeyLayout.TwoOctaves,
    onKeyLayout: (KeyLayout) -> Unit = {},
    english: Boolean = false,
) {
    NeonCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel(if (chordPads) "Pads d'accords" else "Clavier", help = if (chordPads) Glossary.CHORD else null, foldKey = "Clavier")
            Box(Modifier.weight(1f))
            OctaveControl(octave, actions)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (!chordPads) ChordChip(chordKeys, onChordKeys)
            ChordPadsChip(chordPads, onChordPads)
        }
        if (!chordPads) {
            // Fewer, wider keys for the fingers and to learn: one octave named, or only the notes of the key.
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                KeyLayout.entries.forEach { l ->
                    FilterChip(selected = keyLayout == l, onClick = { onKeyLayout(l) }, label = { Text(l.label) }, colors = chipColors(),
                        modifier = Modifier.testTag("key_layout_${l.name}"))
                }
            }
        }
        if (chordPads) ChordPads(harmony, 12 * (octave + 1), actions.noteOn, actions.noteOff, height = height)
        else PlayKeys(keyLayout, octave, octaves, harmony, english, keyOn, keyOff, height)
        play?.let { PlayPanel(it, playActions) }
    }
}

@Composable
private fun ChordPadsChip(on: Boolean, onChange: (Boolean) -> Unit) {
    FilterChip(selected = on, onClick = { onChange(!on) }, label = { Text("Pads") }, colors = chipColors(),
        modifier = Modifier.testTag("chord_pads_switch"))
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
        SectionLabel("Réglages du son")
        Text("Forme d'onde", style = MaterialTheme.typography.bodyMedium)
        ChoiceRow(WAVES, patch.get(SynthPatch.OSC1_WAVE)) { v ->
            actions.setParam(SynthPatch.OSC1_WAVE, v)
            actions.setParam(SynthPatch.OSC2_WAVE, v)
        }
        EASY.forEach { (index, label) ->
            if (index == SynthPatch.UNISON) {
                ParamSlider(label, patch.get(index), Glossary.UNISON, "${1 + (patch.get(index) * 6).roundToInt()} voix") { actions.setParam(index, it) }
            } else {
                ParamSlider(label, patch.get(index)) { actions.setParam(index, it) }
            }
        }
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
internal fun OctaveControl(octave: Int, actions: SynthActions) {
    // − Octave 4 + in one dark capsule, like a hardware selector.
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = 0.25f)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { actions.shiftOctave(-1) }, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Filled.Remove, contentDescription = "Octave plus bas", modifier = Modifier.size(18.dp))
        }
        Text("Octave $octave", style = MonoStyle, color = Neon.Cyan, maxLines = 1, softWrap = false)
        IconButton(onClick = { actions.shiftOctave(1) }, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Filled.Add, contentDescription = "Octave plus haut", modifier = Modifier.size(18.dp))
        }
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
private fun chipColors() = pillColors()

/** Picks the option whose stored value is closest to [current]. */
@Composable
private fun ChoiceRow(options: List<Pair<Float, String>>, current: Float, onPick: (Float) -> Unit) {
    val selected = options.minBy { kotlin.math.abs(it.first - current) }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { option ->
            FilterChip(selected = option == selected, onClick = { onPick(option.first) }, label = { Text(option.second, maxLines = 1, softWrap = false) }, colors = chipColors())
        }
    }
}

@Composable
private fun ParamSlider(label: String, value: Float, help: Help? = null, shown: String? = null, onChange: (Float) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        if (help != null) HelpDot(help)
        Box(Modifier.weight(1f))
        Text(shown ?: "${(value * 100).roundToInt()}", style = MonoStyle, color = Neon.Cyan)
    }
    ProSlider(value = value, onValueChange = onChange)
}

private val BLACK_KEY_AFTER_WHITE = setOf(0, 1, 3, 4, 5)  // C# D# F# G# A# sit after C D F G A
private val WHITE_OFFSETS = listOf(0, 2, 4, 5, 7, 9, 11)

/** Multi-touch piano keyboard: every key handles its own finger. */
@Composable
fun Keyboard(
    firstNote: Int,
    octaves: Int,
    /** Note and velocity: softer near the top of the key, louder at its tip. */
    onNoteOn: (Int, Float) -> Unit,
    onNoteOff: (Int) -> Unit,
    height: Dp = 150.dp,
    harmony: Harmony = Harmony(),
    /** Test tags: several keyboards can be on screen (the floating one). */
    tag: String = "keyboard",
    keyTag: String = "key",
    /** One more white key: the octave ends on its Do, as a scale is sung (Do to Do). */
    topC: Boolean = false,
    /** The name written at the bottom of a white key, if any. */
    label: (Int) -> String? = { null },
) {
    val whiteCount = 7 * octaves + if (topC) 1 else 0
    BoxWithConstraints(Modifier.fillMaxWidth().height(height).testTag(tag)) {
        val whiteWidth = maxWidth / whiteCount
        Row(Modifier.fillMaxSize()) {
            for (w in 0 until whiteCount) {
                val note = firstNote + 12 * (w / 7) + WHITE_OFFSETS[w % 7]
                Key(note, isBlack = false, Modifier.width(whiteWidth).fillMaxHeight(), onNoteOn, onNoteOff, harmony, keyTag, label(note))
            }
        }
        for (w in 0 until whiteCount) {
            if (w % 7 !in BLACK_KEY_AFTER_WHITE || w == whiteCount - 1) continue
            val note = firstNote + 12 * (w / 7) + WHITE_OFFSETS[w % 7] + 1
            Key(
                note,
                isBlack = true,
                Modifier.offset(x = whiteWidth * (w + 1) - whiteWidth * 0.32f).width(whiteWidth * 0.64f).height(height * 0.61f),
                onNoteOn,
                onNoteOff,
                harmony,
                keyTag,
            )
        }
    }
}

@Composable
private fun Key(
    note: Int,
    isBlack: Boolean,
    modifier: Modifier,
    onNoteOn: (Int, Float) -> Unit,
    onNoteOff: (Int) -> Unit,
    harmony: Harmony = Harmony(),
    keyTag: String = "key",
    label: String? = null,
) {
    var down by remember { mutableStateOf(false) }
    val outside = harmony.scale != Scale.Off && !harmony.inScale(note)
    // Scale locked: the keys out of the scale are dark and silent (no wrong note possible).
    val locked = outside && harmony.lock
    val color = when {
        down -> Neon.Cyan
        locked -> if (isBlack) Color(0xFF0E0F11) else Color(0xFF5A5E66)
        isBlack && outside -> Color(0xFF15171A)
        isBlack -> Color(0xFF1C1F24)
        outside -> Color(0xFF9EA2AA)  // out of the scale: greyed, still playable
        // The root in pale accent, as on the scale keys.
        Math.floorMod(note - harmony.root, 12) == 0 && harmony.scale != Scale.Off -> lerp(Neon.Cyan, Color.White, 0.55f)
        else -> Color(0xFFF7F7F3)
    }
    // Keys in relief: lit from above, a darker lip at the bottom; the pressed one glows.
    val shape = RoundedCornerShape(bottomStart = if (isBlack) 5.dp else 7.dp, bottomEnd = if (isBlack) 5.dp else 7.dp)
    val face = if (down) {
        Brush.verticalGradient(listOf(lerp(color, Color.White, 0.35f), color))
    } else if (isBlack) {
        Brush.verticalGradient(0f to lerp(color, Color.White, 0.12f), 0.85f to color, 1f to lerp(color, Color.White, 0.18f))
    } else {
        Brush.verticalGradient(0f to lerp(color, Color.Black, 0.06f), 0.9f to color, 1f to lerp(color, Color.Black, 0.18f))
    }
    Box(
        modifier
            .padding(horizontal = if (isBlack) 0.dp else 1.dp)
            .then(if (down) Modifier.shadow(12.dp, shape, ambientColor = Neon.Cyan, spotColor = Neon.Cyan) else if (isBlack) Modifier.shadow(4.dp, shape) else Modifier)
            .clip(shape)
            .background(face)
            .border(1.dp, if (isBlack) Color.Black.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.22f), shape)
            .pointerInput(note, locked) {
                if (locked) return@pointerInput
                detectTapGestures(onPress = { at ->
                    down = true
                    onNoteOn(note, (0.35f + 0.65f * at.y / size.height).coerceIn(0.2f, 1f))
                    try {
                        tryAwaitRelease()
                    } finally {
                        // Also when the gesture is cancelled (octave change, tab switch): no stuck note.
                        down = false
                        onNoteOff(note)
                    }
                })
            }
            .testTag("${keyTag}_$note"),
        contentAlignment = Alignment.BottomCenter,
    ) {
        if (label != null) {
            Text(
                label, style = MaterialTheme.typography.labelMedium, maxLines = 1, softWrap = false,
                color = if (down) Neon.OnAccent else Color(0xFF5B6068), modifier = Modifier.padding(bottom = 6.dp),
            )
        }
    }
}
