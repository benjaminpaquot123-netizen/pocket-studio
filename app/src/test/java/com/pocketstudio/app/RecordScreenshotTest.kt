package com.pocketstudio.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.record.NoteTakeUi
import com.pocketstudio.app.record.RecordBar
import com.pocketstudio.app.record.RecordBarActions
import com.pocketstudio.app.record.RecordBarUi
import com.pocketstudio.app.record.TakePhase
import com.pocketstudio.app.record.TakeSource
import com.pocketstudio.app.settings.SettingsStore
import com.pocketstudio.model.AudioClip
import com.pocketstudio.model.AudioTrack
import com.pocketstudio.model.SongPart
import com.pocketstudio.model.SongVersion
import com.pocketstudio.model.i18n.Lang
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Phase 35 « Enregistrer sans peur »: the record bar and its states, the card after a take, the
 * take picker and punch-in, the Live page's versions and notes, ● on the full-screen pads
 * (app/screenshots/; the 130 % and tablet subclasses shoot them again).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h890dp-xxhdpi")
open class RecordScreenshotTest : ScreenshotTest() {
    override fun wanted(name: String) = name in SHOTS

    private fun bar(take: NoteTakeUi?, keep: Boolean = false, counter: String? = null) =
        RecordBarUi(take, com.pocketstudio.model.i18n.tr("Enregistrer ma mélodie"), counter = { counter }, canKeepPlayed = keep)

    /** The Clavier with « ● Enregistrer ma mélodie » and « Garder ce que je viens de jouer » above the keys. */
    @Test
    fun recordBar() = shoot("record_bar") {
        com.pocketstudio.app.synth.SynthScreen(
            patch = com.pocketstudio.model.SynthPatch.PRESETS[1].second,
            octave = 4,
            actions = com.pocketstudio.app.synth.SynthActions(),
            record = { compact -> RecordBar(bar(null, keep = true), RecordBarActions(), compact = compact) },
        )
    }

    /** The bar's states one under the other: idle, count-in, recording (« Couplet · mes. 5/16 »), done, put in time. */
    @Test
    fun recordStates() = shoot("record_states") {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            RecordBar(bar(null), RecordBarActions())
            RecordBar(bar(NoteTakeUi(TakeSource.KEYS, TakePhase.COUNT_IN, beatsLeft = 3)), RecordBarActions())
            RecordBar(bar(NoteTakeUi(TakeSource.KEYS, TakePhase.RECORDING), counter = "Couplet · mes. 5/16"), RecordBarActions())
            RecordBar(bar(NoteTakeUi(TakeSource.KEYS, TakePhase.DONE, notes = 14)), RecordBarActions())
            RecordBar(bar(NoteTakeUi(TakeSource.KEYS, TakePhase.DONE, notes = 14, strength = 0.75f, tuned = true)), RecordBarActions())
            RecordBar(bar(NoteTakeUi(TakeSource.CAPTURE, TakePhase.DONE, notes = 9, hits = 4)), RecordBarActions())
        }
    }

    /** After a take in Notes: the card right above the notes, put in time (its strengths and « Comme joué »). */
    @Test
    fun takeDone() = shoot("take_done") {
        val played = listOf(60, 62, 64, 65, 67, 65, 64, 62).mapIndexed { i, p ->
            val at = i * 2 * 24
            com.pocketstudio.model.Note(at, 40, p, 0.8f, played = at + listOf(5, -4, 7, -2, 3, -6, 4, 2)[i])
        }
        com.pocketstudio.app.synth.SynthScreen(
            patch = com.pocketstudio.model.SynthPatch.PRESETS[2].second,
            octave = 4,
            actions = com.pocketstudio.app.synth.SynthActions(),
            notes = com.pocketstudio.app.synth.NotesUi(
                com.pocketstudio.model.NoteClip(16, played), emptyList(), playStep = -1, actions = com.pocketstudio.app.synth.PianoRollActions(),
                harmony = com.pocketstudio.model.Harmony(0, com.pocketstudio.model.Scale.Major),
            ),
            startWithNotes = true,
            record = { compact ->
                RecordBar(bar(NoteTakeUi(TakeSource.KEYS, TakePhase.DONE, notes = 8, strength = 1f, tuned = true)), RecordBarActions(), compact = compact)
            },
        )
    }

    /** A voice take with its bar lines, three takes to pick from, a passage to sing again, recorded at another tempo. */
    @Test
    @Config(qualifiers = "w400dp-h1800dp-xxhdpi")
    open fun takePicker() = shoot("take_picker") {
        val peaks = FloatArray(com.pocketstudio.app.rhythm.PadSamples.PEAK_BUCKETS * 2) { i ->
            val x = i / 2 / 2048f
            val v = (kotlin.math.sin(x * 60f) * 0.5f + 0.3f * kotlin.math.sin(x * 7f)) * (1f - x * 0.6f)
            if (i % 2 == 0) -kotlin.math.abs(v) else kotlin.math.abs(v)
        }
        val take = { f: String, n: String, s: Double -> AudioClip(f, n, s, start = 256, song = true, bpm = 140.0) }
        val tracks = listOf(
            AudioTrack(
                0, "Voix", 1, take("v.wav", "Prise 21:14", 27.4).copy(bpm = 138.0),
                previous = take("w.wav", "Prise 21:09", 26.0), older = listOf(take("x.wav", "Prise 21:02", 28.1)),
            ),
        )
        com.pocketstudio.app.audiotracks.AudioTracksScreen(
            com.pocketstudio.app.audiotracks.AudioTracksUi(
                tracks,
                info = mapOf("v.wav" to com.pocketstudio.app.rhythm.SampleInfo(27.4, peaks)),
                sceneTitle = "B · Couplet",
                songParts = listOf("A · Intro", "B · Couplet", "C · Refrain", "B · Couplet"),
                songBars = 64,
                tempoBpm = 140.0,
                scene = 1,
                sceneTitles = listOf("A · Intro", "B · Couplet", "C · Refrain"),
                partStarts = listOf(0, 256, 512, 768),
                latencyMeasured = true,
            ),
            com.pocketstudio.app.audiotracks.AudioTracksActions(),
        )
    }

    /** « Versions du morceau »: what three Live takes and a restore put aside, each one touch from coming back. */
    @Test
    fun liveVersions() = shoot("live_versions") {
        val now = java.util.Calendar.getInstance().apply { set(2026, 9, 23, 14, 40) }.timeInMillis
        val ui = liveUi()
        var p = ui.project.copy(song = listOf(SongPart(0, 2), SongPart(1, 2), SongPart(2), SongPart(3, 2))).sanitized()
        val v = { kind: Int, minutes: Int, parts: List<SongPart> ->
            SongVersion(kind, now - minutes * 60_000L, parts, scenes = p.scenes)
        }
        p = p.copy(
            songVersions = listOf(
                v(SongVersion.BEFORE_TAKE, 2, listOf(SongPart(0, 2), SongPart(1, 2), SongPart(2))),
                v(SongVersion.RESTORE, 9, listOf(SongPart(0), SongPart(2, 2))),
                v(SongVersion.BEFORE_TAKE, 16, listOf(SongPart(0, 2), SongPart(1))),
            ),
        )
        com.pocketstudio.app.record.SongVersionsContent(p, com.pocketstudio.app.record.versionRows(p, now), com.pocketstudio.app.record.SongVersionsActions())
    }

    /** The Live page's « Notes »: an instrument track played on the notes of the key, during a take. */
    @Test
    fun liveNotes() = shoot("live_notes", fullScreen = true) {
        val ui = liveUi()
        com.pocketstudio.app.live.LiveScreen(
            com.pocketstudio.app.live.LiveScreenUi(
                ui.project, ui.grid, 45, ui.playing, ui.muted, ui.acid,
                take = com.pocketstudio.app.live.TakeUi.Recording(32, 12, 7),
            ),
            com.pocketstudio.app.live.LiveActions(),
            initialTab = com.pocketstudio.app.live.LivePanelTab.Notes,
        )
    }

    /** ● on the full-screen pads, a take on its way: the pads played are kept with it. */
    @Test
    fun livePadsFullRec() = shoot("live_pads_full_rec", fullScreen = true) {
        val clip = { n: String, b: Int -> com.pocketstudio.model.LiveClip("x.wav", n, b) }
        val pads = com.pocketstudio.model.LivePads(8).with(0, clip("Batterie", 8)).with(1, clip("Basse", 16)).with(2, clip("Nappe", 16))
            .with(3, clip("Sub", 8)).with(4, clip("Impact", 4).copy(oneShot = true)).with(5, clip("Morceau 4", 32))
        val ui = liveUi()
        val recording = com.pocketstudio.app.live.LiveScreenUi(
            ui.project.copy(song = listOf(SongPart(0, 2), SongPart(1))).sanitized(), ui.grid, 45, ui.playing, ui.muted, ui.acid,
            take = com.pocketstudio.app.live.TakeUi.Recording(32, 42, 21),
        )
        val columns = com.pocketstudio.app.live.liveColumnsOf(ui.project)
        com.pocketstudio.app.session.LivePadsFullScreen(
            com.pocketstudio.app.session.LivePadsUi(pads, listOf(2, 2, 1, 0, 0, 0, 0, 0), editing = null),
            com.pocketstudio.app.session.LivePadsActions(), close = {},
            tempoBpm = 174.0, beatsPerBar = 4, playing = true, clockOf = { 45 }, togglePlay = {},
            effects = { m -> com.pocketstudio.app.live.LiveFx(ui, columns, com.pocketstudio.app.live.LiveActions(), m) },
            record = { com.pocketstudio.app.live.RecordButton(recording.take) {} },
            banner = { com.pocketstudio.app.live.TakeBanner(recording, com.pocketstudio.app.live.LiveActions(), showReady = false) },
        )
    }

    companion object {
        val SHOTS = setOf("record_bar", "record_states", "take_done", "take_picker", "live_versions", "live_notes", "live_pads_full_rec")
    }
}

/** The same at 130 % on a 411 dp phone (316 dp of room), in French then in Dutch: nothing cut mid-word. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h914dp-xxhdpi")
open class RecordScale130ScreenshotTest : RecordScreenshotTest() {
    protected open val lang = Lang.FR

    @Test @Config(qualifiers = "w411dp-h2000dp-xxhdpi") override fun takePicker() = super.takePicker()
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
class RecordScale130DutchScreenshotTest : RecordScale130ScreenshotTest() {
    override val lang = Lang.NL
}

/** On an 11-inch tablet held upright (app/screenshots/tablet/). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w800dp-h1280dp-hdpi")
class RecordTabletScreenshotTest : RecordScreenshotTest() {
    override val prefix = "tablet/"

    @Test override fun takePicker() = super.takePicker()
}
