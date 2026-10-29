package com.pocketstudio.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.app.settings.SettingsStore
import com.pocketstudio.model.AutoTarget
import com.pocketstudio.model.AutomationLane
import com.pocketstudio.model.ChordTrack
import com.pocketstudio.model.DrumPattern
import com.pocketstudio.model.Harmony
import com.pocketstudio.model.InstrumentTrack
import com.pocketstudio.model.Note
import com.pocketstudio.model.NoteClip
import com.pocketstudio.model.NoteClipboard
import com.pocketstudio.model.Progression
import com.pocketstudio.model.Project
import com.pocketstudio.model.Scale
import com.pocketstudio.model.Scene
import com.pocketstudio.model.SongPart
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.model.instrumentTracks
import com.pocketstudio.model.partMarks
import com.pocketstudio.model.songBars
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Phase 37 « Écrire comme un pro »: the song's chords (the strip in Morceau and its sheet), the
 * piano roll's bar and clipboard, a drum pad's own loop and its hits' chances, a knob's lane
 * opened by « Automatiser » (the parts under the curve), a frozen track (app/screenshots/; the
 * 130 % and tablet subclasses shoot them again).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h890dp-xxhdpi")
open class WriteScreenshotTest : ScreenshotTest() {
    override fun wanted(name: String) = name in SHOTS

    private val t = NoteClip.TICKS_PER_STEP
    private val bar = 16 * t

    /** A four-bar chord loop (C, Am, F, G) on a « Nappe », a bass under it, in Do majeur. */
    private fun chordClip() = NoteClip(
        steps = 64,
        notes = listOf(listOf(60, 64, 67), listOf(57, 60, 64), listOf(53, 57, 60), listOf(55, 59, 62))
            .flatMapIndexed { i, ps -> ps.map { Note(i * bar, bar, it, 0.75f) } },
    )

    private fun bassClip() = NoteClip(
        steps = 64,
        notes = listOf(36, 33, 29, 31).flatMapIndexed { i, p -> (0 until 4).map { k -> Note(i * bar + k * 4 * t, 3 * t, p, 0.9f) } },
    )

    /** The assistant's song in three parts: Intro, Couplet, Refrain. */
    private fun song(): Project {
        val pad = InstrumentTrack(1, "Nappe", color = 2, notes = chordClip())
        val scene = Scene(synthNotes = bassClip(), trackNotes = mapOf(1 to chordClip()), drums = DrumPattern().withSteps(64).fill(0, 4))
        val scenes = listOf(scene.copy(label = "Intro"), scene.copy(label = "Couplet"), scene.copy(label = "Refrain"))
        return Project(
            name = "Soleil", harmony = Harmony(0, Scale.Major), style = "house", synthTrackName = "Basse", synthNotes = bassClip(), tracks = listOf(pad),
            scenes = scenes, drums = scene.drums, song = listOf(SongPart(0), SongPart(1, repeats = 2), SongPart(2, repeats = 2)),
        ).sanitized()
    }

    /** Morceau: the song card with « Accords du morceau » under its clock. */
    @Test
    fun songChordsStrip() = shoot("song_chords_strip") {
        val p = song()
        com.pocketstudio.app.session.SessionScreen(
            com.pocketstudio.app.session.SessionScreenUi(
                scenes = List(Scene.MAX) { p.scenes.getOrNull(it) }, current = 0, pending = null, song = p.song, songIndex = -1, playing = false,
                tempoBpm = 122.0, chords = com.pocketstudio.app.session.songChordsStrip(p, SettingsStore.state.value.englishNotes),
            ),
            com.pocketstudio.app.session.SessionActions(),
        )
    }

    /** « Accords du morceau »: a progression being heard (« Appliquer » lit), each part's chords, the tracks that follow. */
    @Test
    fun songChordsSheet() = shoot("song_chords_sheet") {
        // With a 303 line in its parts: « Ligne acide (Acid 303) » follows too.
        val line = com.pocketstudio.model.AcidPattern(steps = List(com.pocketstudio.model.AcidPattern.MAX_STEPS) { com.pocketstudio.model.AcidStep(gate = it % 4 == 2) })
        val p = song().let { q -> q.copy(acid = line, scenes = q.scenes.map { it.copy(acid = line) }).sanitized() }
        val degrees = ChordTrack.degreesOf(p, Progression.Sad)
        val tried = RhythmViewModel.ChordTry(ChordTrack.songScenes(p).associateWith { degrees }, progression = Progression.Sad)
        com.pocketstudio.app.session.SongChordsContent(
            com.pocketstudio.app.session.songChordsUi(p, tried, SettingsStore.state.value.englishNotes),
            com.pocketstudio.app.session.SongChordsActions(),
        )
    }

    /** The piano roll: bar 1 chosen from its number, copied (the clipboard came from the bass), « Coller mes. 2 ». */
    @Test
    fun pianoRollClipboard() = shoot("piano_roll_clipboard") {
        val clip = chordClip().copy(steps = 32, notes = chordClip().notes.filter { it.start < 2 * bar })
        com.pocketstudio.app.synth.PianoRoll(
            clip, emptyList(), -1, com.pocketstudio.app.synth.PianoRollActions(),
            gridHeight = 300.dp, harmony = Harmony(0, Scale.Major),
            initialSelection = clip.notes.filter { it.start < bar }.toSet(),
            clipboard = NoteClipboard(clip.notes.filter { it.start < bar }.map { it.copy(start = it.start % bar) }, 1, 0, "Nappe · B · Couplet"),
        )
    }

    /** Audit final (lot notes): a waltz in 3/4, two bars of 12 steps: « 2 mes. », the bar lines on the ruler's bars. */
    @Test
    fun pianoRollWaltz() = shoot("piano_roll_waltz") {
        val w = 12 * t
        val clip = NoteClip(steps = 24, notes = listOf(Note(0, 4 * t, 72), Note(4 * t, 4 * t, 76), Note(8 * t, 4 * t, 79), Note(w, 4 * t, 74), Note(w + 4 * t, 8 * t, 77)))
        com.pocketstudio.app.synth.PianoRoll(
            clip, emptyList(), -1, com.pocketstudio.app.synth.PianoRollActions(),
            gridHeight = 300.dp, harmony = Harmony(0, Scale.Major), stepsPerBar = 12,
        )
    }

    /** Rythme: the hi-hat on a loop of its own (6 steps against 16), two hits at 50 %, « Autre tirage ». */
    @Test
    fun drumPolyChance() = shoot("drum_poly_chance") {
        var drums = DrumPattern().fill(0, 4)
        listOf(0, 2, 3, 5).forEach { drums = drums.toggle(5, it) }
        drums = drums.withLaneLength(5, 6).withHitChance(5, 3, 50).withHitChance(5, 5, 50)
        com.pocketstudio.app.rhythm.RhythmScreen(
            Project(drums = drums, tempoBpm = 124.0).sanitized(),
            com.pocketstudio.app.rhythm.TransportUi(selectedPad = 5),
            com.pocketstudio.app.rhythm.LiveUi(), canUndo = true, canRedo = false,
            actions = com.pocketstudio.app.rhythm.RhythmActions(),
        )
    }

    /** « Automatiser » on the synth's cutoff: its lane along the song, a point a beat, the parts named under the curve. */
    @Test
    fun automateLane() = shoot("automate_lane") {
        val p0 = song()
        val target = AutoTarget.synth(1, com.pocketstudio.model.SynthPatch.CUTOFF)
        val beats = p0.songBars * 4
        val points = List(beats) { b -> if (b % 2 == 0) (0.2f + 0.7f * b / beats).coerceAtMost(1f) else null }
        val p = p0.copy(automation = listOf(AutomationLane(target, mode = AutomationLane.SONG, songPerBeat = true, songValues = points)))
        val strips = com.pocketstudio.app.ui.mixerStrips(p)
        com.pocketstudio.app.mixer.AutomateLaneContent(
            0,
            com.pocketstudio.app.mixer.AutomationUi(p.automation, 16, -1, false, songBars = p.songBars, stepsPerBar = 16, parts = p.partMarks()),
            p.mixer.channels, p.mixer.master, strips,
            com.pocketstudio.app.mixer.AutomationActions(setResolution = { _, _ -> }),
            com.pocketstudio.app.mixer.automationLabel(target, p.mixer.channels, p.mixer.master, strips),
            close = {},
        )
    }

    /** Synthé › Notes on a frozen track: « ❄ Gelée » on its tab, the notes kept but locked (« Dégeler »). */
    @Test
    fun trackFrozen() = shoot("track_frozen") {
        val p = song()
        com.pocketstudio.app.synth.SynthScreen(
            patch = com.pocketstudio.model.SynthPatch.PRESETS[2].second,
            octave = 4,
            actions = com.pocketstudio.app.synth.SynthActions(),
            startWithNotes = true,
            notes = com.pocketstudio.app.synth.NotesUi(chordClip(), emptyList(), playStep = -1, actions = com.pocketstudio.app.synth.PianoRollActions(),
                harmony = p.harmony, frozen = true, songChords = {}),
            tracks = com.pocketstudio.app.synth.TracksUi(p.instrumentTracks, selected = 1, frozen = setOf(1), freeze = {}, unfreeze = {}),
        )
    }

    companion object {
        val SHOTS = setOf("song_chords_strip", "song_chords_sheet", "piano_roll_clipboard", "piano_roll_waltz", "drum_poly_chance", "automate_lane", "track_frozen")
    }
}

/** The same at 130 % on a 411 dp phone (316 dp of room), in French then in Dutch: nothing cut mid-word. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h914dp-xxhdpi")
open class WriteScale130ScreenshotTest : WriteScreenshotTest() {
    protected open val lang = Lang.FR
    override val uiScale = 1.3f
    override val prefix get() = "scale130/${lang.code}_"

    @Before
    fun language() {
        SettingsStore.update(ApplicationProvider.getApplicationContext()) { it.copy(language = lang, englishNotes = lang != Lang.FR) }
    }

    @After
    fun french() {
        SettingsStore.update(ApplicationProvider.getApplicationContext()) { it.copy(language = Lang.FR, englishNotes = false) }
    }
}

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h914dp-xxhdpi")
class WriteScale130DutchScreenshotTest : WriteScale130ScreenshotTest() {
    override val lang = Lang.NL
}

/** On an 11-inch tablet held upright (app/screenshots/tablet/). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w800dp-h1280dp-hdpi")
class WriteTabletScreenshotTest : WriteScreenshotTest() {
    override val prefix = "tablet/"
}
