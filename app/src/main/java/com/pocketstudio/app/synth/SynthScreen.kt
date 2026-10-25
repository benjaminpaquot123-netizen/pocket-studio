@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.pocketstudio.app.synth

import com.pocketstudio.app.learn.spot
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.pocketstudio.model.i18n.tr

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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.pocketstudio.app.settings.SettingsStore
import com.pocketstudio.model.NoteNames
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
    /** Phase 37: « Automatiser » on knob (param) by a long press on its name (null: not offered). */
    val automate: ((Int) -> Unit)? = null,
)

private val EASY: List<Pair<Int, String>> get() = listOf(
    SynthPatch.CUTOFF to tr("Coupure"), SynthPatch.RESONANCE to tr("Résonance"), SynthPatch.FILTER_ENV to tr("Enveloppe du filtre"),
    SynthPatch.A_ATTACK to tr("Attaque"), SynthPatch.A_RELEASE to tr("Relâchement"), SynthPatch.OSC2_DETUNE to tr("Épaisseur (désaccord)"),
    SynthPatch.SUB to tr("Sous-basse"), SynthPatch.UNISON to tr("Unisson (supersaw)"), SynthPatch.UNISON_DETUNE to tr("Écart de l'unisson"),
    SynthPatch.LEVEL to tr("Volume"),
)

/** Every synth parameter a knob can move, with its name (MIDI learn). */
internal val SYNTH_LABELS: List<Pair<Int, String>> get() = EASY + ADVANCED

/**
 * The width and movement settings (phase 34, S4) the assistant sets and its curves move: named for
 * the automation lanes (no knob of their own yet).
 */
internal val S4_LABELS: List<Pair<Int, String>> get() = listOf(
    SynthPatch.WIDTH to tr("Largeur stéréo"), SynthPatch.OCTAVE_DOWN to tr("Doublure à l'octave basse"),
    SynthPatch.DRIVE to tr("Saturation du synthé"), SynthPatch.MONO_BELOW to tr("Graves en mono"),
    SynthPatch.LFO2_SHAPE to tr("Forme du balayage"), SynthPatch.CYCLE_BARS to tr("Cycle lent du filtre"),
    SynthPatch.CYCLE_DEPTH to tr("Profondeur du cycle"), SynthPatch.FORMANT to tr("Formant"), SynthPatch.VOWEL to tr("Voyelle"),
    SynthPatch.MID_LAYER to tr("Couche médium"), SynthPatch.OVERSAMPLE to tr("Suréchantillonnage"),
)

/** A synth knob's name (an automation lane's). */
internal fun synthKnobName(param: Int): String = (SYNTH_LABELS + S4_LABELS).firstOrNull { it.first == param }?.second ?: "?"

private val ADVANCED: List<Pair<Int, String>> get() = listOf(
    SynthPatch.OSC_MIX to tr("Mélange oscillateurs"), SynthPatch.NOISE to tr("Bruit"),
    SynthPatch.F_ATTACK to tr("Filtre : attaque"), SynthPatch.F_DECAY to tr("Filtre : déclin"),
    SynthPatch.F_SUSTAIN to tr("Filtre : maintien"), SynthPatch.F_RELEASE to tr("Filtre : relâchement"),
    SynthPatch.A_DECAY to tr("Volume : déclin"), SynthPatch.A_SUSTAIN to tr("Volume : maintien"),
    SynthPatch.LFO1_RATE to tr("Vibrato : vitesse"), SynthPatch.VIBRATO to tr("Vibrato : profondeur"),
    SynthPatch.LFO2_RATE to tr("Balayage : vitesse"), SynthPatch.SWEEP to tr("Balayage du filtre"),
    SynthPatch.LFO2_SYNC to tr("Balayage calé sur le tempo"), SynthPatch.GLIDE to tr("Glissé entre les notes"),
)

private val WAVES: List<Pair<Float, String>> get() = listOf(0f to tr("Scie"), 0.5f to tr("Carré"), 1f to tr("Triangle"))
private val FILTERS: List<Pair<Float, String>> get() = listOf(0f to "Ladder", 0.3f to tr("Passe-bas"), 0.6f to tr("Passe-haut"), 0.9f to tr("Passe-bande"))

/** The piano roll's data, when the screen shows it (null: keyboard only, e.g. in tests). */
class NotesUi(
    val clip: NoteClip,
    val ghosts: List<Note>,
    val playStep: Int,
    val actions: PianoRollActions,
    val harmony: Harmony = Harmony(),
    /** Phase 37: the notes copied in any track or part (null: none). */
    val clipboard: com.pocketstudio.model.NoteClipboard? = null,
    /** Phase 37: the track is frozen (its notes wait for « Dégeler »). */
    val frozen: Boolean = false,
    /** Phase 37: « Accords du morceau » (null: not offered here). */
    val songChords: (() -> Unit)? = null,
    /** Sixteenths in a bar of the time signature: the bars a copy, a paste and the ruler count in. */
    val stepsPerBar: Int = 16,
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
    /** The selected track's effects, in a card under its sound (null: none shown). */
    effects: com.pocketstudio.app.mixer.EffectsUi? = null,
    /** Raised (a new value) to show the Notes view: the timeline's « Ouvrir dans Notes ». */
    notesRequest: Int = 0,
    /**
     * « ● Enregistrer » right above the keys (or the notes): the record bar, given by the app;
     * its argument is true lying down, where a line counts.
     */
    record: (@Composable (compact: Boolean) -> Unit)? = null,
) {
    var advanced by rememberSaveable { mutableStateOf(false) }
    var showNotes by rememberSaveable { mutableStateOf(startWithNotes) }
    // Each request once (coming back to the tab later keeps the view chosen since).
    var notesSeen by rememberSaveable { mutableStateOf(0) }
    LaunchedEffect(notesRequest) {
        if (notesRequest > notesSeen) {
            showNotes = true
            notesSeen = notesRequest
        }
    }
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
    // A tablet by the device (its smaller side at least 600 dp), whatever the rounding of the width given.
    val tablet = isTablet()
    // « Gamme et accords » can send the user down to the track's sound (it is not set note by note).
    val soundRequester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    BoxWithConstraints(modifier.fillMaxSize().testTag("synth")) {
        val keyboardHeight = (maxHeight * 0.45f).coerceIn(110.dp, 220.dp)
        val landscape = maxWidth > maxHeight
        // A tablet held upright (an 8.7" one is 600 dp wide): taller keys, and in Notes mode a
        // keyboard under the piano roll to hear and record notes while they are seen.
        val tabletUpright = !landscape && (tablet || maxWidth >= TABLET_WIDTH)
        val rollHeight = when {
            landscape -> (maxHeight - 140.dp).coerceAtLeast(160.dp)
            // Room left for the keyboard under it (once the tools above are scrolled away).
            tabletUpright -> (maxHeight - 480.dp).coerceIn(300.dp, 640.dp)
            else -> 380.dp
        }
        // A wide tablet held upright: a wider keyboard (three octaves, like lying down).
        val wide = maxWidth >= com.pocketstudio.ui.TWO_PANES_FROM
        val uprightOctaves = if (wide) 3 else 2
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
                tracks?.takeIf { it.showBar }?.let { TrackBar(it) }
                ModeSwitch(showNotes, tracks) { showNotes = it }
                record?.invoke(false)
                NeonCard {
                    PianoRoll(
                        notes.clip, notes.ghosts, notes.playStep, notes.actions,
                        gridHeight = rollHeight,
                        harmony = notes.harmony,
                        keyOn = keyOn, keyOff = keyOff,
                        clipboard = notes.clipboard, frozen = notes.frozen,
                        stepsPerBar = notes.stepsPerBar,
                    )
                }
                if (tabletUpright) {
                    // A pianist plays what she sees placed (and records it with ●) without leaving the notes.
                    NeonCard(modifier = Modifier.testTag("notes_keyboard")) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SectionLabel(if (chordPads) tr("Pads d'accords") else tr("Clavier"), foldKey = "Clavier des notes")
                            Box(Modifier.weight(1f))
                            OctaveControl(octave, actions)
                        }
                        if (chordPads) ChordPads(harmony, 12 * (octave + 1), actions.noteOn, actions.noteOff, height = 120.dp)
                        else PlayKeys(keyLayout, octave, uprightOctaves, harmony, english, keyOn, keyOff, 120.dp, tag = "notes_keyboard_keys", keyTag = "notes_key")
                    }
                }
                transcribe?.let { TranscribeCard(it) }
                NeonCard {
                    SectionLabel(tr("Gamme et accords"), help = Glossary.HARMONY, foldKey = "Gamme et accords")
                    notes.songChords?.let { open ->
                        // Phase 37: the song's chords, for every part and track at once.
                        androidx.compose.material3.OutlinedButton(onClick = open, modifier = Modifier.fillMaxWidth().testTag("synth_song_chords")) {
                            Text(tr("Accords du morceau (toutes les parties)"))
                        }
                    }
                    HarmonyPanel(notes.harmony, notes.clip, notes.actions, toSound = { scope.launch { soundRequester.bringIntoView() } })
                }
                // The same sound sections, in the same order, as in Clavier mode.
                SoundArea(trackSounds.bringIntoViewRequester(soundRequester)) {
                    SoundSections(patch, actions, instrument, browser, advanced, effects) { advanced = it }
                }
                Spacer(Modifier.height(floatRoom))
            }
        } else if (landscape) {
            // Landscape: one compact line (mode, octave), the sound sections scroll (the same ones,
            // in the same order, as upright), and a wide three-octave keyboard stays under the
            // thumbs. A tablet lying down also has the room for the keyboard's layouts.
            val tall = maxHeight >= TALL_LANDSCAPE
            // Lot 2 (audit 35, the Tab A11 lying down): the record bar and the take's card stand
            // beside the keyboard, never above it: after a take they pushed the keys down to 60 dp,
            // off the screen at 130 %. The keyboard keeps its height; two octaves when it is narrower.
            val beside = record != null
            val recordWidth = (maxWidth * 0.32f).coerceIn(240.dp, 340.dp)
            val keysWidth = maxWidth - 16.dp - if (beside) recordWidth + 8.dp else 0.dp
            val landOctaves = if (keysWidth >= LANDSCAPE_THREE_OCTAVES) 3 else 2
            Column(Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                tracks?.takeIf { it.showBar }?.let { TrackBar(it) }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Lot 2 review: the switch at its own width, the free room after it (filling the whole
                    // row, it squeezed the others to nothing; weighted to what they left, on a small phone
                    // lying down that was nothing, and « Notes » could not be touched).
                    if (notes != null) ModeSwitch(showNotes, tracks, Modifier, fill = false) { showNotes = it }
                    if (tall && !chordPads) KeyLayoutChips(keyLayout, setKeyLayout, wideOctaves = landOctaves)
                    Box(Modifier.weight(1f))
                    ChordChip(chordKeys) { chordKeys = it }
                    ChordPadsChip(chordPads) { chordPads = it }
                    OctaveControl(octave, actions)
                }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(if (tall) 10.dp else 8.dp)) {
                    play?.let { PlayPanel(it, playActions) }
                    SoundArea(Modifier) { SoundSections(patch, actions, instrument, browser, advanced, effects) { advanced = it } }
                }
                val keys: @Composable () -> Unit = {
                    if (chordPads) {
                        ChordPads(harmony, 12 * (octave + 1), actions.noteOn, actions.noteOff, height = keyboardHeight)
                    } else {
                        PlayKeys(keyLayout, octave, landOctaves, harmony, english, keyOn, keyOff, keyboardHeight)
                    }
                }
                if (record != null) {
                    // The keys' height for the row: the take's card scrolls beside them rather than push them off a phone lying down.
                    Row(Modifier.fillMaxWidth().height(keyboardHeight).testTag("synth_keys_row"), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) { keys() }
                        Column(
                            Modifier.width(recordWidth).heightIn(max = keyboardHeight).verticalScroll(rememberScrollState()).testTag("synth_record_side"),
                            verticalArrangement = Arrangement.Center,
                        ) { record(true) }
                    }
                } else {
                    keys()
                }
            }
        } else {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                tracks?.takeIf { it.showBar }?.let { TrackBar(it) }
                if (notes != null) ModeSwitch(showNotes, tracks) { showNotes = it }
                record?.invoke(false)
                KeyboardCard(octave, octaves = uprightOctaves, height = if (tabletUpright) 190.dp else 150.dp, actions = actions, harmony = harmony, chordKeys = chordKeys,
                    onChordKeys = { chordKeys = it }, keyOn = keyOn, keyOff = keyOff, play = play, playActions = playActions,
                    chordPads = chordPads, onChordPads = { chordPads = it }, modifier = trackKeyboard,
                    keyLayout = keyLayout, onKeyLayout = setKeyLayout, english = english)
                // The same sound sections, in the same order, as in Notes mode.
                SoundArea(trackSounds) {
                    SoundSections(patch, actions, instrument, browser, advanced, effects) { advanced = it }
                }
                Spacer(Modifier.height(floatRoom))
            }
        }
        if (follows) {
            // In Notes mode on an upright tablet the keyboard under the roll is there already.
            val wanted = soundsInView && (showNotes && notes != null && !tabletUpright || !keyboardInView)
            // Folding holds until the real keyboard is back: the next time it is out of reach, the floating one returns.
            LaunchedEffect(wanted) { if (!wanted) floatFolded = false }
            FloatingKeyboard(
                visible = wanted, folded = floatFolded, onFold = { floatFolded = it },
                octave = octave, octaves = if (landscape || wide) 3 else 2, keysHeight = floatKeys, actions = actions, harmony = harmony,
                chordPads = chordPads, keyOn = keyOn, keyOff = keyOff, modifier = Modifier.align(Alignment.BottomCenter),
                keyLayout = keyLayout, english = english,
            )
        }
    }
}

/**
 * The track's sound under a short title saying it is the track's (the same for all its notes),
 * in one block whose visibility is followed.
 */
@Composable
private fun SoundArea(track: Modifier, content: @Composable () -> Unit) {
    Column(track, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            tr("Le son de la piste (le même pour toutes ses notes)"),
            style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary,
            modifier = Modifier.padding(start = 4.dp, top = 4.dp).testTag("sound_area_title"),
        )
        content()
    }
}

/**
 * The track's sound, the same sections in the same order in Clavier and Notes, upright and lying
 * down: the instrument, its ready-made sounds, then the settings (the synth's own).
 */
@Composable
private fun SoundSections(
    patch: SynthPatch,
    actions: SynthActions,
    instrument: InstrumentUi?,
    browser: SoundBrowserUi?,
    advanced: Boolean,
    effects: com.pocketstudio.app.mixer.EffectsUi?,
    setAdvanced: (Boolean) -> Unit,
) {
    instrument?.let { InstrumentPanel(it) }
    if (instrument?.current == null) {
        PresetsCard(patch, actions, browser)
        if (instrument?.synthEngine != 1) SoundCard(patch, actions, advanced, setAdvanced)
    }
    // The track's effects, right after its sound, in every mode.
    effects?.let { com.pocketstudio.app.mixer.EffectsCard(it) }
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
            SectionLabel(if (chordPads) tr("Pads d'accords") else tr("Clavier"), help = if (chordPads) Glossary.CHORD else null, foldKey = "Clavier")
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
                KeyLayoutChips(keyLayout, onKeyLayout, octaves)
            }
        }
        if (chordPads) ChordPads(harmony, 12 * (octave + 1), actions.noteOn, actions.noteOff, height = height)
        else PlayKeys(keyLayout, octave, octaves, harmony, english, keyOn, keyOff, height)
        play?.let { PlayPanel(it, playActions) }
    }
}

/**
 * A landscape screen at least this high keeps the whole sounds card and the keyboard's layouts: a
 * tablet lying down (an 8.7" Galaxy Tab A11 leaves about 470 dp under the bars), not a phone (about 330).
 */
internal val TALL_LANDSCAPE = 440.dp

/** From this width a screen held upright is a tablet's (an 8.7" one is 600 dp wide). */
internal val TABLET_WIDTH = 600.dp

/** Lying down, a keyboard at least this wide shows three octaves (narrower: two, keys still wide enough for a finger). */
private val LANDSCAPE_THREE_OCTAVES = 600.dp

/** 2 octaves / 1 octave / Gamme: the keyboard's layouts; the full piano says how many octaves it shows. */
@Composable
private fun KeyLayoutChips(keyLayout: KeyLayout, onKeyLayout: (KeyLayout) -> Unit, wideOctaves: Int) {
    KeyLayout.entries.forEach { l ->
        FilterChip(selected = keyLayout == l, onClick = { onKeyLayout(l) }, label = { Text(l.label(wideOctaves)) }, colors = chipColors(),
            modifier = Modifier.testTag("key_layout_${l.name}"))
    }
}

@Composable
private fun ChordPadsChip(on: Boolean, onChange: (Boolean) -> Unit) {
    FilterChip(selected = on, onClick = { onChange(!on) }, label = { Text(tr("Pads")) }, colors = chipColors(),
        modifier = Modifier.testTag("chord_pads_switch"))
}

@Composable
private fun ChordChip(on: Boolean, onChange: (Boolean) -> Unit) {
    FilterChip(selected = on, onClick = { onChange(!on) }, label = { Text(tr("Accords")) }, colors = chipColors(),
        modifier = Modifier.testTag("chord_keys"))
}

@Composable
private fun PresetsCard(patch: SynthPatch, actions: SynthActions, browser: SoundBrowserUi?) {
    NeonCard {
        SectionLabel(tr("Sons prêts à jouer"), foldKey = "Sons prêts à jouer")
        if (browser != null) SoundBrowser(browser)
        // Chips go to a second line rather than squeeze (« Pluck » cut in two on a narrow screen).
        else androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { PresetChips(patch, actions) }
    }
}

@Composable
internal fun SoundCard(patch: SynthPatch, actions: SynthActions, advanced: Boolean, setAdvanced: (Boolean) -> Unit) {
    NeonCard {
        SectionLabel(tr("Réglages du son"), foldKey = "Réglages du son")
        Text(tr("Forme d'onde"), style = MaterialTheme.typography.bodyMedium)
        ChoiceRow(WAVES, patch.get(SynthPatch.OSC1_WAVE)) { v ->
            actions.setParam(SynthPatch.OSC1_WAVE, v)
            actions.setParam(SynthPatch.OSC2_WAVE, v)
        }
        // Two or three columns of sliders when the card is wide (tablet), one under the other on a phone.
        com.pocketstudio.ui.AdaptiveColumns(EASY.map { (index, label) ->
            @Composable {
                if (index == SynthPatch.UNISON) {
                    ParamSlider(label, patch.get(index), Glossary.UNISON, com.pocketstudio.app.ui.voicesLabel(1 + (patch.get(index) * 6).roundToInt()), knob(actions, index)) { actions.setParam(index, it) }
                } else {
                    ParamSlider(label, patch.get(index), automate = knob(actions, index)) { actions.setParam(index, it) }
                }
            }
        }, minCellWidth = 260.dp, maxColumns = 3)
        TextButton(onClick = { setAdvanced(!advanced) }) {
            Icon(if (advanced) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null)
            Text(if (advanced) tr("Masquer les réglages avancés") else tr("Réglages avancés"))
        }
        AnimatedVisibility(visible = advanced) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(tr("Oscillateur 2"), style = MaterialTheme.typography.bodyMedium)
                ChoiceRow(WAVES, patch.get(SynthPatch.OSC2_WAVE)) { actions.setParam(SynthPatch.OSC2_WAVE, it) }
                ChoiceRow(listOf(0f to tr("Octave −1"), 0.5f to tr("Même octave"), 1f to tr("Octave +1")), patch.get(SynthPatch.OSC2_OCTAVE)) {
                    actions.setParam(SynthPatch.OSC2_OCTAVE, it)
                }
                Text(tr("Type de filtre"), style = MaterialTheme.typography.bodyMedium)
                ChoiceRow(FILTERS, patch.get(SynthPatch.FILTER_TYPE)) { actions.setParam(SynthPatch.FILTER_TYPE, it) }
                com.pocketstudio.ui.AdaptiveColumns(
                    ADVANCED.map { (index, label) -> @Composable { ParamSlider(label, patch.get(index), automate = knob(actions, index)) { actions.setParam(index, it) } } },
                    minCellWidth = 260.dp, maxColumns = 3, spacing = 4.dp,
                )
            }
        }
    }
}

@Composable
private fun ModeSwitch(
    showNotes: Boolean, tracks: TracksUi?, modifier: Modifier = Modifier.fillMaxWidth(),
    /** « Vider » pushed to the row's end (false: right after the chips, the row's other controls after it). */
    fill: Boolean = true,
    onChange: (Boolean) -> Unit,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        FilterChip(selected = !showNotes, onClick = { onChange(false) }, label = { Text(tr("Clavier")) }, colors = chipColors(),
            modifier = Modifier.testTag("mode_keyboard"))
        FilterChip(selected = showNotes, onClick = { onChange(true) }, label = { Text(tr("Notes")) }, colors = chipColors(),
            modifier = Modifier.spot("mode_notes", tr("Les notes")).testTag("mode_notes"))
        // Empties this track in the scene shown, in one touch (Annuler brings it back): to take the
        // synth out of a copied scene, or start again after trying sounds.
        val clear = tracks?.clear
        if (clear != null) {
            if (fill) androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
            TextButton(
                onClick = { clear(tracks.selected) }, enabled = tracks.hasNotes(tracks.selected),
                modifier = Modifier.testTag("track_clear_button")
                    .semantics { contentDescription = tr("Vider cette piste dans la partie %s", tracks.scene) },
            ) {
                Icon(Icons.Filled.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(" " + tr("Vider"))
            }
        }
    }
}

@Composable
internal fun OctaveControl(octave: Int, actions: SynthActions) {
    // − Octave 4 + in one dark capsule, like a hardware selector. The number follows Réglages ›
    // Notation: the keyboard starting on middle C is "Octave 3" in Do Ré Mi, "Octave 4" in C D E.
    val english = SettingsStore.state.collectAsState().value.englishNotes
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = 0.25f)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { actions.shiftOctave(-1) }, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Filled.Remove, contentDescription = tr("Octave plus bas"), modifier = Modifier.size(18.dp))
        }
        Text(tr("Octave %d", NoteNames.keyboardOctave(octave, english)), style = MonoStyle, color = Neon.Cyan, maxLines = 1, softWrap = false)
        IconButton(onClick = { actions.shiftOctave(1) }, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Filled.Add, contentDescription = tr("Octave plus haut"), modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun PresetChips(patch: SynthPatch, actions: SynthActions) {
    SynthPatch.PRESETS.forEach { (name, preset) ->
        FilterChip(
            selected = patch.sanitized() == preset.sanitized(),
            onClick = { actions.applyPreset(preset) },
            label = { Text(tr(name)) },
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

/** Phase 37: knob [index]'s « Automatiser », when the synth's knobs can be automated ([AutoTarget.SYNTH_PARAMS]). */
private fun knob(actions: SynthActions, index: Int): Pair<Int, () -> Unit>? =
    actions.automate?.takeIf { index in com.pocketstudio.model.AutoTarget.SYNTH_PARAMS }?.let { a -> index to { a(index) } }

@Composable
private fun ParamSlider(label: String, value: Float, help: Help? = null, shown: String? = null, automate: Pair<Int, () -> Unit>? = null, onChange: (Float) -> Unit) {
    // A long press on the name: « Automatiser » (phase 37).
    AutomatableKnob(automate?.second, "knob_${automate?.first ?: label}", Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            if (help != null) HelpDot(help)
            Box(Modifier.weight(1f))
            Text(shown ?: "${(value * 100).roundToInt()}", style = MonoStyle, color = Neon.Cyan)
        }
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
