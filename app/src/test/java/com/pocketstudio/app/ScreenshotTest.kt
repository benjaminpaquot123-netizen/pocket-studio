package com.pocketstudio.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.click
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.onNodeWithTag
import com.github.takahirom.roborazzi.captureRoboImage
import com.pocketstudio.model.copySceneTo
import com.pocketstudio.model.renameScene
import com.pocketstudio.model.withFx
import com.pocketstudio.model.withAudioTrack
import com.pocketstudio.app.latency.LatencyScreen
import com.pocketstudio.app.latency.LatencyUiState
import com.pocketstudio.app.recette.CheckResult
import com.pocketstudio.app.recette.CheckStatus
import com.pocketstudio.app.recette.ChecklistParser
import com.pocketstudio.app.recette.RecetteScreen
import com.pocketstudio.app.recette.RecetteUiState
import com.pocketstudio.app.rhythm.DrumKitInfo
import com.pocketstudio.app.rhythm.LiveUi
import com.pocketstudio.app.rhythm.RhythmActions
import com.pocketstudio.app.rhythm.RhythmScreen
import com.pocketstudio.app.rhythm.TransportUi
import com.pocketstudio.app.studio.StudioScreen
import com.pocketstudio.model.DrumPattern
import com.pocketstudio.model.Project
import com.pocketstudio.app.studio.StudioUiState
import com.pocketstudio.engine.StreamInfo
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.Palette
import com.pocketstudio.ui.Palettes
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Rule
import org.junit.Test
import com.pocketstudio.model.addTrack
import com.pocketstudio.model.addAudioTrack
import com.pocketstudio.model.instrumentTracks
import com.pocketstudio.model.track
import com.pocketstudio.model.withTrack
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Renders every screen with representative data; images land in app/screenshots/. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h890dp-xxhdpi")
open class ScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private val stream = StreamInfo(
        sampleRate = 48000, framesPerBurst = 96, bufferSize = 192, bufferCapacity = 3072,
        exclusive = true, aaudio = true, mmap = true, lowLatency = true, xruns = 0, outputLatencyMs = 9.6,
        running = true, callbacks = 1000, peak = 0.42f,
    )

    /** Folder prefix of the images ("" for the phone; the tablet subclass shoots a chosen few again). */
    protected open val prefix: String = ""
    protected open fun wanted(name: String): Boolean = true

    /**
     * Room taken around a screen by the app's own bars on the device simulated (status bar, top bar,
     * tab bar or rail, system navigation): the screen is drawn inside, so it gets the size it really
     * has. Zero (the whole window) unless a subclass simulates a given device.
     */
    protected open val chrome = androidx.compose.foundation.layout.PaddingValues(0.dp)

    /** The same for a page that hides the app's bars (the Live page): only the system's are left. */
    protected open val fullScreenChrome = androidx.compose.foundation.layout.PaddingValues(0.dp)

    /** Réglages › Taille de l'interface, applied as the app does (density and the width screens see). */
    protected open val uiScale = 1f

    /** [prepare]: touches made before the image (a switch that only the screen itself holds). */
    protected fun shoot(
        name: String, palette: Palette = Palettes.Studio, prepare: () -> Unit = {}, fullScreen: Boolean = false, content: @Composable () -> Unit,
    ) {
        org.junit.Assume.assumeTrue(wanted(name))
        compose.setContent {
            com.pocketstudio.app.ui.UiScaled(uiScale) {
                PocketStudioTheme(palette) {
                    Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black).padding(if (fullScreen) fullScreenChrome else chrome).background(Neon.Background)) { content() }
                }
            }
        }
        prepare()
        compose.onRoot().captureRoboImage("screenshots/$prefix$name.png")
    }

    @Test
    fun studioPlaying() = shoot("studio_playing") {
        StudioScreen(StudioUiState(playing = true, frequencyHz = 440f, volume = 0.6f), stream, true, {}, {}, {})
    }

    @Test
    fun studioStopped() = shoot("studio_stopped") {
        StudioScreen(StudioUiState(), StreamInfo(), true, {}, {}, {})
    }

    @Test
    fun latencyMeasured() = shoot("latency_measured") {
        LatencyScreen(LatencyUiState.Measured(17.3, listOf(17.1, 17.3, 18.0), ""), stream, 17.3, true, {})
    }

    @Test
    fun latencyFailed() = shoot("latency_failed") {
        LatencyScreen(
            LatencyUiState.Failed(
                "Le micro n'entend que du silence absolu : l'accès au micro est sans doute coupé dans les réglages rapides (icône Micro).",
                "Micro brut, exclusif : SilentInput, niveau 0,0000, confiance 0,0, 60000 éch.",
            ),
            stream, null, true, {},
        )
    }

    @Test
    fun latencyRunning() = shoot("latency_running") {
        LatencyScreen(LatencyUiState.Running(2, 3), stream, null, true, {})
    }

    @Test
    fun recette() {
        val checklist = ChecklistParser.parse(File("src/main/assets/recette.json").readText())
        val results = mapOf(
            checklist.items[0].id to CheckResult(CheckStatus.Pass),
            checklist.items[1].id to CheckResult(CheckStatus.Fail, "Petit clic au démarrage"),
        )
        val changelog = File("src/main/assets/changelog.txt").readText().trim()
        shoot("recette") {
            RecetteScreen(RecetteUiState(checklist, results, changelog), stream, 17.3, { _, _ -> }, {}, {})
        }
    }

    /** « Force »: a touch low on a hit makes it soft, the slider shows its strength. */
    @Test
    @Config(qualifiers = "w400dp-h1300dp-xxhdpi")
    fun rhythmVelocity() {
        org.junit.Assume.assumeTrue(wanted("rhythm_velocity"))
        var drums = DrumPattern()
        DrumKitInfo.starterHits.forEach { (lane, steps) -> steps.forEach { drums = drums.toggle(lane, it) } }
        drums = drums.withHitVelocity(0, 8, 0.65f).withHitVelocity(0, 12, 0.3f)
        var project by androidx.compose.runtime.mutableStateOf(Project(tempoBpm = 124.0, drums = drums))
        compose.setContent {
            PocketStudioTheme {
                Box(Modifier.fillMaxSize().background(Neon.Background)) {
                    RhythmScreen(
                        project = project,
                        transport = TransportUi(selectedPad = 0),
                        live = LiveUi(),
                        canUndo = true,
                        canRedo = false,
                        actions = RhythmActions(setHitVelocity = { step, v -> project = project.copy(drums = project.drums.withHitVelocity(0, step, v)) }),
                    )
                }
            }
        }
        compose.onNodeWithTag("velocity_tool").performScrollTo().performClick()
        // Three quarters down the cell: a soft hit (about a quarter).
        compose.onNodeWithTag("step_4").performTouchInput { click(androidx.compose.ui.geometry.Offset(width / 2f, height * 0.75f)) }
        compose.waitForIdle()
        org.junit.Assert.assertEquals(0.25f, project.drums.lanes[0].hits[4]!!.velocity, 0.05f)
        // An empty cell gets no hit from this tool.
        compose.onNodeWithTag("step_1").performTouchInput { click(center) }
        org.junit.Assert.assertNull(project.drums.lanes[0].hits[1])
        compose.onRoot().captureRoboImage("screenshots/${prefix}rhythm_velocity.png")
    }

    @Test
    fun rhythm() = shoot("rhythm") {
        var drums = DrumPattern()
        DrumKitInfo.starterHits.forEach { (lane, steps) -> steps.forEach { drums = drums.toggle(lane, it) } }
        drums = drums.toggle(0, 10, 0.5f).toggle(0, 14, 0.2f)
            .withSample(0, com.pocketstudio.model.PadSample("k.wav", "Kick de ma voix", start = 0.05f, end = 0.8f, pitch = -3f, loop = true))
        val wave = FloatArray(24000) { i -> (kotlin.math.sin(i * 0.02) * kotlin.math.exp(-i / 6000.0)).toFloat() }
        val info = com.pocketstudio.app.rhythm.SampleInfo(
            0.5, com.pocketstudio.app.rhythm.PadSamples.peaks(com.pocketstudio.app.audio.Pcm(wave, 1, 48000)),
        )
        RhythmScreen(
            project = Project(tempoBpm = 124.0, swing = 0.2, drums = drums),
            transport = TransportUi(playing = true, metronome = true, selectedPad = 0),
            live = LiveUi(currentStep = 5, litPads = setOf(5)),
            canUndo = true,
            canRedo = false,
            actions = RhythmActions(),
            padSound = com.pocketstudio.app.rhythm.PadSoundUi(mapOf("k.wav" to info)),
        )
    }

    @Test
    fun acid() = shoot("acid") {
        val pattern = com.pocketstudio.model.AcidGenerator(kotlin.random.Random(3))
            .generate(com.pocketstudio.model.AcidPattern(length = 32, locked = setOf(2)))
        com.pocketstudio.app.acid.AcidScreen(
            acid = pattern,
            playing = true,
            currentStep = 6,
            selected = 2,
            actions = com.pocketstudio.app.acid.AcidActions(),
        )
    }

    @Test
    fun synth() = shoot("synth") {
        com.pocketstudio.app.synth.SynthScreen(
            patch = com.pocketstudio.model.SynthPatch.PRESETS[1].second,
            octave = 4,
            actions = com.pocketstudio.app.synth.SynthActions(),
        )
    }

    private fun withTracks(): com.pocketstudio.model.Project {
        var p = com.pocketstudio.model.Project()
        p = p.addTrack("Basse")!!.addTrack("Cordes")!!.addTrack("Lead")!!
        p = p.withTrack(p.track(1).copy(engine = 1))
        p = p.withTrack(p.track(2).copy(instrument = com.pocketstudio.model.InstrumentChoice(0, 48, "Strings")))
        return p.addAudioTrack("Voix")!!
    }

    @Test
    fun tracks() = shoot("tracks") {
        val p = withTracks()
        val clip = com.pocketstudio.model.NoteClip(16, listOf(com.pocketstudio.model.Note(start = 0, length = 48, pitch = 60, velocity = 1f)))
        com.pocketstudio.app.synth.SynthScreen(
            patch = p.synth,
            octave = 4,
            actions = com.pocketstudio.app.synth.SynthActions(),
            // The selected track plays notes in scene B: « Vider » next to Clavier / Notes empties it there.
            notes = com.pocketstudio.app.synth.NotesUi(clip, emptyList(), playStep = -1, actions = com.pocketstudio.app.synth.PianoRollActions()),
            tracks = com.pocketstudio.app.synth.TracksUi(
                p.instrumentTracks.map { if (it.id == 2) it.copy(notes = clip) else it }, selected = 2, clear = {}, scene = "B",
            ),
        )
    }

    @Test
    fun audioTracks() = shoot("audio_tracks") {
        val peaks = FloatArray(com.pocketstudio.app.rhythm.PadSamples.PEAK_BUCKETS * 2) { i ->
            val x = i / 2 / 2048f
            val v = (kotlin.math.sin(x * 60f) * 0.5f + 0.3f * kotlin.math.sin(x * 7f)) * (1f - x * 0.6f)
            if (i % 2 == 0) -kotlin.math.abs(v) else kotlin.math.abs(v)
        }
        val tracks = listOf(
            com.pocketstudio.model.AudioTrack(0, "Voix", 1, com.pocketstudio.model.AudioClip("v.wav", "Prise 21:14", 7.6, loopSteps = 64, fadeOut = 0.2f)),
            com.pocketstudio.model.AudioTrack(1, "Guitare", 3),
        )
        com.pocketstudio.app.audiotracks.AudioTracksScreen(
            com.pocketstudio.app.audiotracks.AudioTracksUi(
                tracks, recording = com.pocketstudio.app.rhythm.AudioRecordingUi(1, waiting = false, seconds = 3.4f, level = 0.6f),
                info = mapOf("v.wav" to com.pocketstudio.app.rhythm.SampleInfo(7.6, peaks)),
                sceneTitle = "B · Couplet",
                listening = com.pocketstudio.app.audio.Listening.Speaker,
            ),
            com.pocketstudio.app.audiotracks.AudioTracksActions(),
        )
    }

    @Test
    fun audioTracksOnTheSong() = shoot("audio_tracks_song") {
        // Before a take: « Enregistrer sur » the whole song from part 2; a song clip recorded at
        // another tempo in part 2, nudged 15 ms earlier, with a previous take to bring back; a
        // scene take of the couplet; Bluetooth headphones.
        val tracks = listOf(
            com.pocketstudio.model.AudioTrack(
                0, "Voix", 1,
                com.pocketstudio.model.AudioClip("v.wav", "Prise 21:14", 54.0, start = 256, song = true, offsetMs = -15f, bpm = 138.0),
                previous = com.pocketstudio.model.AudioClip("w.wav", "Prise 21:02", 50.0, start = 256, song = true),
            ),
            com.pocketstudio.model.AudioTrack(1, "Guitare", 3, com.pocketstudio.model.AudioClip("g.wav", "Prise 21:20", 6.2, once = true, scene = 1)),
        )
        com.pocketstudio.app.audiotracks.AudioTracksScreen(
            com.pocketstudio.app.audiotracks.AudioTracksUi(
                tracks,
                sceneTitle = "B · Couplet",
                songParts = listOf("A · Intro", "B · Couplet", "C · Refrain", "B · Couplet"),
                songBars = 64,
                tempoBpm = 140.0,
                target = com.pocketstudio.app.rhythm.RecordTarget(song = true, fromPart = 1),
                scene = 1,
                sceneTitles = listOf("A · Intro", "B · Couplet", "C · Refrain"),
                partStarts = listOf(0, 256, 512, 768),
                listening = com.pocketstudio.app.audio.Listening.Bluetooth,
                latencyMeasured = true,
            ),
            com.pocketstudio.app.audiotracks.AudioTracksActions(),
        )
    }

    @Test
    fun mixerTracks() = shoot("mixer_tracks") {
        val p = withTracks()
        com.pocketstudio.app.mixer.MixerScreen(
            channels = p.mixer.channels,
            meters = { FloatArray(com.pocketstudio.engine.NativeEngine.METERS) { if (it in listOf(0, 2, 5, 6, 4, 12)) 0.5f + it * 0.02f else 0f }.also { it[com.pocketstudio.engine.NativeEngine.METER_LIMITER] = 1f; it[com.pocketstudio.engine.NativeEngine.METER_MASTER_IN] = 0.58f } },
            actions = com.pocketstudio.app.mixer.MixerActions(),
            strips = com.pocketstudio.app.ui.mixerStrips(p),
        )
    }

    @Test
    fun mixer() = shoot("mixer") {
        val channels = com.pocketstudio.model.Project().mixer.channels.toMutableList()
        channels[1] = channels[1].copy(solo = true, pan = -0.4f)
        channels[2] = channels[2].copy(mute = true, volume = 0.5f)
        com.pocketstudio.app.mixer.MixerScreen(
            channels = channels,
            meters = { FloatArray(com.pocketstudio.engine.NativeEngine.METERS).also { m -> listOf(0.62f, 0.81f, 0f, 0.4f, 0.88f).forEachIndexed { i, v -> m[i] = v }; m[com.pocketstudio.engine.NativeEngine.METER_LIMITER] = 0.8f; m[com.pocketstudio.engine.NativeEngine.METER_MASTER_IN] = 1.1f } },
            actions = com.pocketstudio.app.mixer.MixerActions(),
            export = com.pocketstudio.app.rhythm.ExportUi(bars = 4, files = listOf(java.io.File("Mon-premier-rythme-20261018-221500.wav")), savedInMusic = true, loudness = -13.24,
                truePeakDb = -1.21, seconds = 192.4)
                .forProject(sceneBars = 4, hasSong = true),
            hasSong = true,
            // The master's loudness while it plays, its target following the style (house: −8 LUFS).
            loudness = { floatArrayOf(-7.9f, -8.1f, -8.4f, -1.2f, -1.3f, 3.2f) },
            target = com.pocketstudio.model.LoudnessTarget.AUTO,
            styleLufs = -8f,
        )
    }

    /**
     * Phase 33, the master section scrolled into view on a phone: loudness and true peak at a
     * glance, the meter, the readings, « Volume visé » (the style's, a club's, streaming's or none)
     * and the gain added to get there.
     */
    @Test
    fun mixerMaster() = shoot("mixer_master", prepare = { compose.onNodeWithTag("master_effects").performScrollTo(); compose.waitForIdle() }) {
        com.pocketstudio.app.mixer.MixerScreen(
            channels = com.pocketstudio.model.Project().mixer.channels,
            meters = { FloatArray(com.pocketstudio.engine.NativeEngine.METERS).also { m -> listOf(0.62f, 0.81f, 0f, 0.4f, 0.88f).forEachIndexed { i, v -> m[i] = v }; m[com.pocketstudio.engine.NativeEngine.METER_LIMITER] = 0.8f; m[com.pocketstudio.engine.NativeEngine.METER_MASTER_IN] = 1.1f } },
            actions = com.pocketstudio.app.mixer.MixerActions(),
            export = com.pocketstudio.app.rhythm.ExportUi(bars = 4).forProject(sceneBars = 4, hasSong = true),
            hasSong = true,
            loudness = { floatArrayOf(-7.9f, -8.1f, -8.4f, -1.2f, -1.3f, 3.2f) },
            target = com.pocketstudio.model.LoudnessTarget.AUTO,
            styleLufs = -8f,
        )
    }

    @Test
    @Config(qualifiers = "w400dp-h1650dp-xxhdpi")
    fun mixerExporting() = shoot("mixer_export") {
        androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.padding(androidx.compose.ui.unit.Dp(12f))) {
            com.pocketstudio.app.mixer.ExportCard(
                com.pocketstudio.app.rhythm.ExportUi(bars = 8, what = com.pocketstudio.app.rhythm.ExportWhat.Stems, running = true, progress = 0.4f, stage = "Acide (2/4)"),
                com.pocketstudio.app.mixer.MixerActions(),
            )
            com.pocketstudio.app.mixer.ExportCard(
                com.pocketstudio.app.rhythm.ExportUi(
                    bars = 4, what = com.pocketstudio.app.rhythm.ExportWhat.Song, format = com.pocketstudio.app.rhythm.ExportFormat.Flac,
                    files = listOf(java.io.File("Da-Funk-morceau-20261020-101500.flac")), savedInMusic = true, loudness = -8.43,
                    truePeakDb = -1.18, seconds = 214.0,
                ).forProject(sceneBars = 4, hasSong = true),
                com.pocketstudio.app.mixer.MixerActions(),
            )
        }
    }

    @Test
    fun topBar() = shoot("top_bar") {
        // The scene chip at a small phone's, a usual phone's and a big phone's width, built the app's way
        // (section names saved in French, shown in the interface's language).
        val labels = listOf("Intro", "Couplet", "Refrain", "Fin")
        val project = com.pocketstudio.model.Project(
            scenes = labels.mapIndexed { i, l -> com.pocketstudio.model.Scene(com.pocketstudio.model.Scene.NAMES[i], com.pocketstudio.model.DrumPattern().toggle(0, 0), label = l) },
            scene = 2,
        )
        val scenes = com.pocketstudio.app.ui.sceneChip(project)
        androidx.compose.foundation.layout.Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)) {
            listOf(300, 360, 400).forEach { w ->
                Box(Modifier.width(w.dp)) {
                    com.pocketstudio.app.ui.TopBar(
                        com.pocketstudio.app.ui.TopBarUi("Da Funk (remix)", playing = false, canUndo = true, canRedo = true, scene = scenes, saved = true),
                        com.pocketstudio.app.ui.TopBarActions(),
                    )
                }
            }
        }
    }

    /**
     * Under the top bar on every tab (phase 33): the tempo, where the song is (or where ▶ starts),
     * « ⟳ Partie » under ▶; the solo banner; renaming the project in place.
     */
    @Test
    fun playStrip() = shoot("play_strip") {
        val labels = listOf("Intro", "Couplet", "Refrain", "Fin")
        val project = com.pocketstudio.model.Project(
            scenes = labels.mapIndexed { i, l -> com.pocketstudio.model.Scene(com.pocketstudio.model.Scene.NAMES[i], com.pocketstudio.model.DrumPattern().toggle(0, 0), label = l) },
            scene = 2,
        )
        androidx.compose.foundation.layout.Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)) {
            // A small phone and a usual one, stopped: where ▶ starts the song.
            listOf(360, 400).forEach { w ->
                Box(Modifier.width(w.dp)) {
                    androidx.compose.foundation.layout.Column {
                        com.pocketstudio.app.ui.TopBar(
                            com.pocketstudio.app.ui.TopBarUi("Da Funk (remix)", playing = false, canUndo = true, canRedo = true, scene = com.pocketstudio.app.ui.sceneChip(project), saved = true),
                            com.pocketstudio.app.ui.TopBarActions(),
                        )
                        StripShot(124.0, song = true, loop = false, playing = false, com.pocketstudio.app.ui.PlayPosition("Couplet", 9, 60))
                    }
                }
            }
            // Playing the song; looping the part; no song yet; at 360 dp a long part name.
            StripShot(124.0, song = true, loop = false, playing = true, com.pocketstudio.app.ui.PlayPosition("Refrain", 14, 60))
            StripShot(124.0, song = true, loop = true, playing = true, com.pocketstudio.app.ui.PlayPosition("Refrain", 2, 4, loop = true))
            StripShot(120.0, song = false, loop = false, playing = false, null)
            Box(Modifier.width(360.dp)) {
                StripShot(174.0, song = true, loop = true, playing = false, com.pocketstudio.app.ui.PlayPosition(com.pocketstudio.model.Scene.shownLabel("Pré-refrain 2"), 1, 8, loop = true))
            }
            // Lying down, the solo banner folds into a chip of the strip.
            StripShot(124.0, song = true, loop = false, playing = true, com.pocketstudio.app.ui.PlayPosition("Refrain", 14, 60), soloed = listOf("Batterie"))
            com.pocketstudio.app.ui.SoloBanner(listOf("Batterie", "Basse acide"), {})
            com.pocketstudio.app.ui.SoloBanner(listOf("Batterie", "Basse acide", "Lead", "Voix"), {})
            com.pocketstudio.app.ui.RenameProjectBar("Da Funk (remix)", {}, {})
        }
    }

    @Composable
    private fun StripShot(bpm: Double, song: Boolean, loop: Boolean, playing: Boolean, pos: com.pocketstudio.app.ui.PlayPosition?, soloed: List<String> = emptyList()) =
        com.pocketstudio.app.ui.PlayStrip(
            com.pocketstudio.app.ui.PlayStripUi(
                bpm, hasSong = song, loopPart = loop, playing = playing, position = { pos }, parts = if (song) listOf("Intro", "Couplet") else emptyList(), soloed = soloed,
            ),
            com.pocketstudio.app.ui.PlayStripActions(),
        )

    /**
     * A phone lying down, with everything over the content: the top bar, the play strip with its
     * « Solo » chip (no banner, no « Prochaine étape » strip there), then Rythme.
     */
    @Test
    @Config(qualifiers = "w860dp-h380dp-land-xxhdpi")
    fun rhythmLandscapeWithStrips() = shoot("rhythm_landscape") {
        var drums = DrumPattern()
        DrumKitInfo.starterHits.forEach { (lane, steps) -> steps.forEach { drums = drums.toggle(lane, it) } }
        val labels = listOf("Intro", "Couplet", "Refrain", "Fin")
        val project = Project(
            tempoBpm = 124.0, drums = drums, scene = 2,
            scenes = labels.mapIndexed { i, l -> com.pocketstudio.model.Scene(com.pocketstudio.model.Scene.NAMES[i], drums, label = l) },
        )
        androidx.compose.foundation.layout.Column {
            com.pocketstudio.app.ui.TopBar(
                com.pocketstudio.app.ui.TopBarUi("Da Funk (remix)", playing = true, canUndo = true, canRedo = true, scene = com.pocketstudio.app.ui.sceneChip(project), saved = true),
                com.pocketstudio.app.ui.TopBarActions(),
            )
            StripShot(124.0, song = true, loop = false, playing = true, com.pocketstudio.app.ui.PlayPosition("Refrain", 14, 60), soloed = listOf("Batterie"))
            RhythmScreen(
                project = project,
                transport = TransportUi(playing = true, selectedPad = 0),
                live = LiveUi(currentStep = 5, litPads = setOf(5)),
                canUndo = true, canRedo = true,
                actions = RhythmActions(),
            )
        }
    }

    /** The tempo sheet's content: what is typed, the ready-made tempos, ½ ×2, the keypad, tap tempo. */
    @Test
    fun tempoSheet() = shoot("tempo_sheet") {
        Box(Modifier.background(Neon.Surface).padding(top = 12.dp)) {
            com.pocketstudio.app.ui.TempoPad(124.0, com.pocketstudio.app.ui.TempoActions(), {})
        }
    }

    @Test
    fun helpPage() = shoot("help_page") {
        com.pocketstudio.app.settings.HelpPage(com.pocketstudio.app.settings.HelpActions())
    }

    @Test
    fun background() = shoot("background") {
        com.pocketstudio.app.playback.BackgroundScreen(
            com.pocketstudio.app.playback.BackgroundState(notifications = true, batteryFree = false, honor = true),
            {}, {}, {},
        )
    }

    @Test
    @Config(qualifiers = "w860dp-h380dp-land-xxhdpi")
    fun synthLandscape() = shoot("synth_landscape") {
        com.pocketstudio.app.synth.SynthScreen(
            patch = com.pocketstudio.model.SynthPatch.PRESETS[2].second,
            octave = 3,
            actions = com.pocketstudio.app.synth.SynthActions(),
        )
    }


    /** The Live page's example: four named scenes, drums, acid, two instrument tracks, a voice and a remix. */
    private fun liveUi(acidStopped: Boolean = false, spectrum: Boolean = false, fx: com.pocketstudio.app.live.LiveFxUi = com.pocketstudio.app.live.LiveFxUi()): com.pocketstudio.app.live.LiveScreenUi {
        val drums = DrumPattern().toggle(0, 0).toggle(0, 8).toggle(2, 4).toggle(2, 12)
        val acid = com.pocketstudio.model.AcidPattern(steps = List(com.pocketstudio.model.AcidPattern.MAX_STEPS) { i -> com.pocketstudio.model.AcidStep(gate = i % 3 == 0) })
        val notes = com.pocketstudio.model.NoteClip(16, listOf(com.pocketstudio.model.Note(0, 48, 60), com.pocketstudio.model.Note(96, 48, 63)))
        var p = Project(
            scenes = listOf(
                com.pocketstudio.model.Scene("A", drums = drums, label = "Intro"),
                com.pocketstudio.model.Scene("B", drums = drums, acid = acid, label = "Couplet"),
                com.pocketstudio.model.Scene("C", drums = drums, acid = acid, synthNotes = notes, label = "Refrain"),
                com.pocketstudio.model.Scene("D", acid = acid, synthNotes = notes, label = "Drop"),
            ),
            scene = 1, drums = drums, acid = acid,
            remix = com.pocketstudio.model.Remix(setId = "x", name = "Chanson"),
        ).addTrack("Basse")!!.addAudioTrack("Voix")!!
        val bass = p.tracks.first().id
        p = p.withAudioTrack(p.audioTracks.first().copy(clip = com.pocketstudio.model.AudioClip("v.wav", "Voix", scene = 2)))
            .copy(scenes = p.scenes.mapIndexed { i, sc -> if (i >= 2) sc.copy(trackNotes = mapOf(bass to notes)) else sc })
            .sanitized()
        var g = com.pocketstudio.model.LiveState.on(1, 32)
        g = g.launched(com.pocketstudio.model.LiveSlots.DRUMS, 2, 32)
            .launched(com.pocketstudio.model.LiveSlots.INST + bass, -1, 32)
            .armed(com.pocketstudio.model.LiveSlots.mask(com.pocketstudio.model.LiveSlots.INST), 3)
        if (acidStopped) g = g.launched(com.pocketstudio.model.LiveSlots.ACID, -1, 32)
        return com.pocketstudio.app.live.LiveScreenUi(
            p, g, clock = 45, playing = true, muted = setOf(com.pocketstudio.model.SongPart.audio(0)),
            acid = mapOf(0 to 0.62f, 1 to 0.8f, 7 to 0.55f),
            // A kick and a bass under some hats: what the pads show faintly behind them.
            spectrum = if (!spectrum) ({ null }) else ({ FloatArray(40) { b -> -14f - b * 1.1f - if (b in 18..24) 10f else 0f } }),
            fx = fx,
        )
    }

    @Test
    fun liveGrid() = shoot("live_grid", fullScreen = true) {
        com.pocketstudio.app.live.LiveScreen(liveUi(), com.pocketstudio.app.live.LiveActions())
    }

    @Test
    fun liveFx() = shoot("live_fx", fullScreen = true, prepare = {
        compose.onNodeWithTag("live_fx_inst:0").performScrollTo().performClick()
    }) {
        com.pocketstudio.app.live.LiveScreen(liveUi(), com.pocketstudio.app.live.LiveActions())
    }

    /** « Tenus » with the lock on: « Freinage » kept (its padlock), the bass cut while held. */
    @Test
    fun liveFxLock() = shoot("live_fx_lock", fullScreen = true, prepare = {
        compose.onNodeWithTag("live_lock").performClick()
        compose.onNodeWithTag("live_page_HOLDS").performClick()
        compose.waitForIdle()
    }) {
        com.pocketstudio.app.live.LiveScreen(
            liveUi(
                spectrum = true,
                fx = com.pocketstudio.app.live.LiveFxUi(
                    values = mapOf(com.pocketstudio.model.AutoTarget.LIVE_TAPE_STOP to 1f, com.pocketstudio.model.AutoTarget.LIVE_KILL_LOW to 1f),
                    locks = setOf(com.pocketstudio.model.AutoTarget.LIVE_TAPE_STOP),
                ),
            ),
            com.pocketstudio.app.live.LiveActions(),
        )
    }

    /** « Montée » half way, the Drop armed for the next bar. */
    @Test
    fun liveFxBuild() = shoot("live_fx_build", fullScreen = true, prepare = {
        compose.onNodeWithTag("live_page_BUILD").performClick()
        compose.waitForIdle()
    }) {
        com.pocketstudio.app.live.LiveScreen(
            liveUi(fx = com.pocketstudio.app.live.LiveFxUi(values = mapOf(com.pocketstudio.model.AutoTarget.LIVE_BUILD_UP to 0.6f), dropArmed = true)),
            com.pocketstudio.app.live.LiveActions(),
        )
    }

    /** The roll pads, 1/16 rolling. */
    @Test
    fun liveFxRolls() = shoot("live_fx_rolls", fullScreen = true, prepare = {
        compose.onNodeWithTag("live_page_ROLLS").performClick()
        compose.waitForIdle()
    }) {
        com.pocketstudio.app.live.LiveScreen(
            liveUi(
                fx = com.pocketstudio.app.live.LiveFxUi(
                    values = mapOf(
                        com.pocketstudio.model.AutoTarget.LIVE_REPEAT to 1f,
                        com.pocketstudio.model.AutoTarget.LIVE_DIVISION to com.pocketstudio.model.LiveDivision.SIXTEENTH.engineValue,
                    ),
                ),
            ),
            com.pocketstudio.app.live.LiveActions(),
        )
    }

    /** The effects' settings (⚙). */
    @Test
    fun liveFxSettings() = shoot("live_fx_settings", fullScreen = true, prepare = {
        compose.onNodeWithTag("live_fx_settings").performClick()
        compose.waitForIdle()
    }) {
        com.pocketstudio.app.live.LiveScreen(liveUi(), com.pocketstudio.app.live.LiveActions())
    }

    @Test
    fun liveAcid() = shoot("live_acid", fullScreen = true) {
        com.pocketstudio.app.live.LiveScreen(liveUi(acidStopped = true), com.pocketstudio.app.live.LiveActions(), initialTab = com.pocketstudio.app.live.LivePanelTab.Acide)
    }

    /** « Enregistrer la performance »: a take on its way, after a song of two parts. */
    @Test
    fun liveRec() = shoot("live_rec", fullScreen = true, prepare = {
        compose.onNodeWithTag("live_fx_acid").performScrollTo().performClick()
    }) {
        val ui = liveUi()
        val song = ui.project.copy(song = listOf(com.pocketstudio.model.SongPart(0, 2), com.pocketstudio.model.SongPart(1))).sanitized()
        com.pocketstudio.app.live.LiveScreen(
            com.pocketstudio.app.live.LiveScreenUi(
                song, ui.grid, 45, ui.playing, ui.muted, ui.acid, listOf(0.7f, 0.5f),
                take = com.pocketstudio.app.live.TakeUi.Recording(32, 42, 21), takeAppend = true, chops = setOf(1),
            ),
            com.pocketstudio.app.live.LiveActions(),
        )
    }

    /** The take written: what it wrote, Annuler and Écouter. */
    @Test
    fun liveRecDone() = shoot("live_rec_done", fullScreen = true) {
        val ui = liveUi()
        com.pocketstudio.app.live.LiveScreen(
            com.pocketstudio.app.live.LiveScreenUi(
                ui.project, ui.grid, 45, ui.playing, ui.muted, ui.acid,
                take = com.pocketstudio.app.live.TakeUi.Done(parts = 7, settings = 3, firstPart = 0),
            ),
            com.pocketstudio.app.live.LiveActions(),
            initialTab = com.pocketstudio.app.live.LivePanelTab.Acide,
        )
    }

    @Test
    @Config(qualifiers = "w860dp-h400dp-land-xxhdpi")
    fun liveLandscape() = shoot("live_landscape", fullScreen = true) {
        com.pocketstudio.app.live.LiveScreen(liveUi(), com.pocketstudio.app.live.LiveActions())
    }

    @Test
    fun padSound() = shoot("pad_sound") {
        val wave = FloatArray(24000) { i -> (kotlin.math.sin(i * 0.02) * kotlin.math.exp(-i / 6000.0)).toFloat() }
        val info = com.pocketstudio.app.rhythm.SampleInfo(
            0.5, com.pocketstudio.app.rhythm.PadSamples.peaks(com.pocketstudio.app.audio.Pcm(wave, 1, 48000)),
        )
        val drums = DrumPattern().withSample(
            2, com.pocketstudio.model.PadSample("k.wav", "Clap de cuisine", start = 0.05f, end = 0.7f, pitch = 5f, reverse = true, choke = 1),
        )
        androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.padding(androidx.compose.ui.unit.Dp(12f))) {
            com.pocketstudio.app.rhythm.PadSoundCard(
                Project(drums = drums), 2, com.pocketstudio.app.rhythm.PadSoundUi(mapOf("k.wav" to info)), RhythmActions(),
            )
        }
    }

    @Test
    fun slicer() = shoot("slicer") {
        val rnd = java.util.Random(3)
        val wave = FloatArray(96000) { i -> ((rnd.nextFloat() - 0.5f) * 1.6f * kotlin.math.exp(-(i % 24000) / 3000.0)).toFloat() }
        val info = com.pocketstudio.app.rhythm.SampleInfo(
            2.0, com.pocketstudio.app.rhythm.PadSamples.peaks(com.pocketstudio.app.audio.Pcm(wave, 1, 48000)),
        )
        val drums = DrumPattern().withSample(0, com.pocketstudio.model.PadSample("b.wav", "Breakbeat"))
        androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.padding(androidx.compose.ui.unit.Dp(12f))) {
            com.pocketstudio.app.rhythm.PadSoundCard(
                Project(drums = drums), 0,
                com.pocketstudio.app.rhythm.PadSoundUi(
                    mapOf("b.wav" to info),
                    slicer = com.pocketstudio.app.rhythm.SlicerUi("b.wav", markers = listOf(0f, 0.25f, 0.5f, 0.75f)),
                ),
                RhythmActions(),
            )
        }
    }

    @Test
    fun pianoRoll() = shoot("piano_roll") {
        val chords = listOf(60, 64, 67, 57, 60, 64, 65, 69, 72, 67, 71, 74)
        val notes = chords.mapIndexed { i, p ->
            com.pocketstudio.model.Note(start = (i / 3) * 4 * 24 * 1, length = 3 * 24, pitch = p, velocity = if (i % 3 == 0) 1f else 0.5f)
        } + com.pocketstudio.model.Note(start = 13 * 24 + 7, length = 30, pitch = 76, velocity = 0.2f)
        val acid = com.pocketstudio.model.AcidGenerator(kotlin.random.Random(3)).generate(com.pocketstudio.model.AcidPattern(root = 48))
        com.pocketstudio.app.synth.SynthScreen(
            patch = com.pocketstudio.model.SynthPatch.PRESETS[2].second,
            octave = 4,
            actions = com.pocketstudio.app.synth.SynthActions(),
            notes = com.pocketstudio.app.synth.NotesUi(
                com.pocketstudio.model.NoteClip(16, notes),
                com.pocketstudio.model.NoteClip.ghostsOf(acid, 16),
                playStep = 6,
                actions = com.pocketstudio.app.synth.PianoRollActions(),
                harmony = com.pocketstudio.model.Harmony(0, com.pocketstudio.model.Scale.Major),
            ),
            startWithNotes = true,
        )
    }

    @Test
    fun allPads() = shoot("all_pads") {
        val p = com.pocketstudio.model.Demos.ALL[1].third()
        androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.padding(androidx.compose.ui.unit.Dp(12f))) {
            com.pocketstudio.app.rhythm.AllPadsGrid(p, 0, 6, RhythmActions())
        }
    }

    @Test
    fun pianoRollSelection() = shoot("piano_roll_select") {
        val notes = listOf(60, 64, 67, 72).mapIndexed { i, p -> com.pocketstudio.model.Note(i * 48, 40, p, 0.3f + 0.2f * i) } +
            listOf(55, 59, 62).map { com.pocketstudio.model.Note(240, 96, it, 0.8f) }
        androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.padding(androidx.compose.ui.unit.Dp(12f))) {
            com.pocketstudio.app.synth.PianoRoll(
                com.pocketstudio.model.NoteClip(16, notes), emptyList(), playStep = -1,
                actions = com.pocketstudio.app.synth.PianoRollActions(),
                gridHeight = androidx.compose.ui.unit.Dp(300f),
                initialSelection = notes.drop(4).toSet(),
            )
        }
    }

    @Test
    fun instruments() = shoot("instruments") {
        val names = listOf("Grand Piano", "Bright Piano", "Electric Grand", "Honky-Tonk", "Tine Electric Piano", "FM Electric Piano", "Harpsichord", "Clavinet")
        val presets = names.mapIndexed { i, n -> com.pocketstudio.engine.Preset(i, 0, i, n) }
        com.pocketstudio.app.synth.SynthScreen(
            patch = com.pocketstudio.model.SynthPatch(),
            octave = 4,
            actions = com.pocketstudio.app.synth.SynthActions(),
            instrument = com.pocketstudio.app.instruments.InstrumentUi(
                com.pocketstudio.app.rhythm.LibraryState.Ready(presets),
                com.pocketstudio.model.InstrumentChoice(0, 4, "Tine Electric Piano"),
            ),
        )
    }

    @Test
    fun sampledInstrument() = shoot("sampled_instrument") {
        androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.padding(androidx.compose.ui.unit.Dp(12f))) {
            com.pocketstudio.app.instruments.InstrumentPanel(
                com.pocketstudio.app.instruments.InstrumentUi(
                    com.pocketstudio.app.rhythm.LibraryState.Missing, null, synthEngine = 2,
                    sampled = com.pocketstudio.model.SampledPatch(file = "x.wav", name = "808 3 · Trap", root = 24.2f, glide = 0.08f, release = 0.08f),
                ),
            )
        }
    }

    @Test
    fun helpDialog() = shoot("help_dialog") {
        com.pocketstudio.ui.HelpDialog(com.pocketstudio.app.help.Glossary.SCENE) {}
    }

    @Test
    fun instrumentsDownload() = shoot("instruments_download") {
        androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.padding(androidx.compose.ui.unit.Dp(12f))) {
            com.pocketstudio.app.instruments.InstrumentPanel(
                com.pocketstudio.app.instruments.InstrumentUi(com.pocketstudio.app.rhythm.LibraryState.Downloading(0.42f), null),
            )
        }
    }

    @Test
    fun remix() = shoot("remix") {
        val set = com.pocketstudio.app.remix.StemSet(
            File("/tmp/stems/abc"), "Ma chanson", 214.0, 0L, com.pocketstudio.app.remix.SongInfo(124.0, 0.25, 9, true),
        )
        com.pocketstudio.app.remix.RemixScreen(
            com.pocketstudio.app.remix.RemixUi(true, com.pocketstudio.app.remix.SeparationState.Separating("Autre chanson", 0.37f), listOf(set),
                com.pocketstudio.app.remix.StemPreview(set.id, listOf(true, true, false, true), playing = true, position = 48000L * 75, frames = 48000L * 200)),
            com.pocketstudio.app.remix.RemixActions(),
        )
    }

    private fun wizardUi(step: Int, mood: com.pocketstudio.model.StarterWizard.Mood? = com.pocketstudio.model.StarterWizard.Mood.Happy): com.pocketstudio.app.library.WizardUi {
        val style = com.pocketstudio.model.StarterWizard.style("house")!!
        val rows = style.lines.associate { l ->
            val cats = com.pocketstudio.app.library.soundCategories(l.role, style.id, emptyList())
            l.role to com.pocketstudio.app.library.WizardRow(true, cats.first().id, cats.first().options.first())
        }
        val parts = com.pocketstudio.model.StarterWizard.Part.entries.associateWith {
            com.pocketstudio.app.library.PartRow(it.byDefault, com.pocketstudio.model.StarterWizard.soundsFor(it, style).first())
        }
        return com.pocketstudio.app.library.WizardUi(true, step, style.id, rows, mood, parts)
    }

    @Test
    fun wizardStyle() = shoot("wizard_style") {
        com.pocketstudio.app.library.WizardScreen(com.pocketstudio.app.library.WizardUi(open = true),
            com.pocketstudio.app.library.PacksUi(com.pocketstudio.app.library.IndexState.Unavailable), null, com.pocketstudio.app.library.WizardActions())
    }

    @Test
    fun wizardStyleOpen() = shoot("wizard_style_open") {
        // A card touched: its whole description, every reference, ▶ and "Choisir ce style".
        com.pocketstudio.app.library.WizardScreen(com.pocketstudio.app.library.WizardUi(open = true, styleId = "trap"),
            com.pocketstudio.app.library.PacksUi(com.pocketstudio.app.library.IndexState.Unavailable), null, com.pocketstudio.app.library.WizardActions())
    }

    @Test
    fun wizardAcid() = shoot("wizard_acid") {
        val style = com.pocketstudio.model.StarterWizard.style("acid_house")!!
        val rows = style.lines.associate { l ->
            val cats = com.pocketstudio.app.library.soundCategories(l.role, style.id, emptyList())
            l.role to com.pocketstudio.app.library.WizardRow(true, cats.first().id, cats.first().options.first())
        }
        val parts = com.pocketstudio.model.StarterWizard.Part.entries.associateWith {
            com.pocketstudio.app.library.PartRow(it in com.pocketstudio.model.StarterWizard.defaultParts(style), com.pocketstudio.model.StarterWizard.soundsFor(it, style).first())
        }
        val ui = com.pocketstudio.app.library.WizardUi(true, 0, style.id, rows, com.pocketstudio.model.StarterWizard.Mood.Dark, parts)
        com.pocketstudio.app.library.WizardScreen(ui.copy(step = ui.steps.indexOf(com.pocketstudio.model.StarterWizard.Step.Acid)),
            com.pocketstudio.app.library.PacksUi(com.pocketstudio.app.library.IndexState.Unavailable), null, com.pocketstudio.app.library.WizardActions())
    }

    /** The 303 step scrolled down: the generator, « Celle du style » and the 303's sound (a quick choice made, two bars). */
    @Test
    fun wizardAcidSound() = shoot("wizard_acid_sound", prepare = {
        compose.onNodeWithTag("wizard_acid_sound").performScrollTo()
    }) {
        val style = com.pocketstudio.model.StarterWizard.style("acid_house")!!
        val rows = style.lines.associate { l ->
            val cats = com.pocketstudio.app.library.soundCategories(l.role, style.id, emptyList())
            l.role to com.pocketstudio.app.library.WizardRow(true, cats.first().id, cats.first().options.first())
        }
        val ui = com.pocketstudio.app.library.WizardUi(true, 0, style.id, rows, com.pocketstudio.model.StarterWizard.Mood.Dark, emptyMap())
        val line = com.pocketstudio.model.WizardAcid.withBars(ui.acidPattern!!, 2)
        com.pocketstudio.app.library.WizardScreen(
            ui.copy(step = ui.steps.indexOf(com.pocketstudio.model.StarterWizard.Step.Acid), acidNotes = line,
                acidSound = com.pocketstudio.model.WizardAcid.sound("Saturée"), acidSoundName = "Saturée"),
            com.pocketstudio.app.library.PacksUi(com.pocketstudio.app.library.IndexState.Unavailable), null, com.pocketstudio.app.library.WizardActions(),
        )
    }

    /** « + Piste »: what to add, a melody written for me first. */
    @Test
    fun trackAdd() = shoot("track_add") {
        androidx.compose.foundation.layout.Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            androidx.compose.material3.Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp), color = Neon.Surface,
                modifier = Modifier.padding(16.dp).widthIn(max = 520.dp)) {
                com.pocketstudio.app.synth.TrackAddChoices(onPick = { _, _ -> }, onDismiss = {})
            }
        }
    }

    @Test
    fun wizardLead() = shoot("wizard_lead") {
        // A lead the recipe leaves out, playing a written riff (no arpeggiator).
        val ui = wizardUi(2 + com.pocketstudio.model.StarterWizard.style("house")!!.lines.size + 2)
        com.pocketstudio.app.library.WizardScreen(ui.copy(parts = ui.parts + (com.pocketstudio.model.StarterWizard.Part.Lead to ui.parts.getValue(com.pocketstudio.model.StarterWizard.Part.Lead).copy(enabled = false))),
            com.pocketstudio.app.library.PacksUi(com.pocketstudio.app.library.IndexState.Unavailable), null, com.pocketstudio.app.library.WizardActions())
    }

    @Test
    fun wizardMood() = shoot("wizard_mood") {
        com.pocketstudio.app.library.WizardScreen(wizardUi(1), com.pocketstudio.app.library.PacksUi(com.pocketstudio.app.library.IndexState.Unavailable),
            null, com.pocketstudio.app.library.WizardActions())
    }

    @Test
    fun wizardKick() = shoot("wizard_kick") {
        com.pocketstudio.app.library.WizardScreen(wizardUi(2), com.pocketstudio.app.library.PacksUi(com.pocketstudio.app.library.IndexState.Unavailable),
            com.pocketstudio.app.library.SoundRef.Kit(0), com.pocketstudio.app.library.WizardActions())
    }

    @Test
    fun wizardPad() = shoot("wizard_pad") {
        val ui = wizardUi(2 + com.pocketstudio.model.StarterWizard.style("house")!!.lines.size + 1)
        com.pocketstudio.app.library.WizardScreen(ui, com.pocketstudio.app.library.PacksUi(com.pocketstudio.app.library.IndexState.Unavailable),
            null, com.pocketstudio.app.library.WizardActions())
    }

    @Test
    fun wizardSummary() = shoot("wizard_summary") {
        val ui = wizardUi(99)
        com.pocketstudio.app.library.WizardScreen(ui.copy(step = ui.lastStep), com.pocketstudio.app.library.PacksUi(com.pocketstudio.app.library.IndexState.Unavailable),
            null, com.pocketstudio.app.library.WizardActions())
    }

    @Test
    fun songGuide() = shoot("song_guide") {
        val plan = wizardUi(99).plan
        val song = com.pocketstudio.app.library.songSummary(com.pocketstudio.model.StarterWizard.style("house")!!, plan)
        com.pocketstudio.app.settings.GuideDialog(onClose = {}, pages = com.pocketstudio.app.library.songGuide("House", plan, inKey = true, song = song), tag = "song_guide")
    }

    @Test
    fun library() = shoot("library") {
        val sounds = (1..5).map { com.pocketstudio.model.PackSound("kick_0$it.flac", "kick", "Kick $it") }
        val installed = listOf(com.pocketstudio.model.PackManifest("retro_techno", "Rétro techno", "Techno", 132.0, sounds))
        val index = com.pocketstudio.model.PackIndex(listOf(
            com.pocketstudio.model.PackEntry("retro_techno", "Rétro techno", "retro_techno.zip", "a".repeat(64), 1_400_000, 78),
            com.pocketstudio.model.PackEntry("detroit", "Techno de Detroit", "detroit.zip", "b".repeat(64), 1_300_000, 74),
            com.pocketstudio.model.PackEntry("uk_garage", "UK Garage", "uk_garage.zip", "c".repeat(64), 1_500_000, 80),
        ))
        com.pocketstudio.app.library.LibraryScreen(
            com.pocketstudio.app.library.LibraryUi(
                com.pocketstudio.app.library.PacksUi(com.pocketstudio.app.library.IndexState.Ready(index), installed, mapOf("uk_garage" to 0.4f)),
                target = "le pad 1 (Kick)", sizes = mapOf("retro_techno" to 1_380_000L),
            ),
            com.pocketstudio.app.library.LibraryActions(),
        )
    }

    /** « Changer le son » on a song made by the assistant, its style's pack not downloaded yet. */
    @Test
    fun libraryStyleOffer() = shoot("library_style_offer") {
        val style = com.pocketstudio.model.StarterWizard.style("trap") ?: com.pocketstudio.model.StarterWizard.STYLES.first()
        val index = com.pocketstudio.model.PackIndex(style.packs.mapIndexed { i, id ->
            com.pocketstudio.model.PackEntry(id, if (id == style.id) style.name else id.uppercase(), "$id.zip", "a".repeat(64), 1_300_000L + i * 200_000L, 64)
        })
        com.pocketstudio.app.library.LibraryScreen(
            com.pocketstudio.app.library.LibraryUi(
                com.pocketstudio.app.library.PacksUi(com.pocketstudio.app.library.IndexState.Ready(index)),
                target = "le pad 1 (Kick)", styleId = style.id,
            ),
            com.pocketstudio.app.library.LibraryActions(),
        )
    }

    /** The packs of the « Tout télécharger » screenshots: a style pack with a richer version, new free-library packs. */
    private fun allPacksUi(query: Boolean = false): com.pocketstudio.app.library.PacksUi {
        val e = { id: String, name: String, size: Long, sounds: Int, v: Int ->
            com.pocketstudio.model.PackEntry(id, name, "$id.zip", "a".repeat(64), size, sounds, v, "sons-packs-5")
        }
        val index = com.pocketstudio.model.PackIndex(listOf(
            e("retro_techno", "Rétro techno", 2_800_000, 136, 2),
            e("bruitages_maison", "Bruitages : maison et objets", 8_600_000, 203, 1),
            e("bruitages_corps", "Bruitages : corps, voix et pas", 5_200_000, 123, 1),
            e("percussions_acoustiques", "Percussions acoustiques", 28_500_000, 267, 1),
            e("detroit", "Techno de Detroit", 2_600_000, 148, 2),
        ))
        val foley = com.pocketstudio.model.PackManifest("bruitages_maison", "Bruitages : maison et objets", "Bruitages", 120.0, listOf(
            com.pocketstudio.model.PackSound("clap_doigts_01.flac", "clap", "Claquement de doigts 1", listOf("doigts", "snap")),
            com.pocketstudio.model.PackSound("clap_doigts_02.flac", "clap", "Claquement de doigts 2", listOf("doigts", "snap")),
            com.pocketstudio.model.PackSound("perc_pingpong_01.flac", "perc", "Balle de ping-pong 1", listOf("ping-pong", "balle")),
        ))
        val retro = com.pocketstudio.model.PackManifest("retro_techno", "Rétro techno", "Techno", 132.0,
            (1..5).map { com.pocketstudio.model.PackSound("kick_0$it.flac", "kick", "Kick $it") })
        val installed = if (query) listOf(retro, foley) else listOf(retro)
        return com.pocketstudio.app.library.PacksUi(
            com.pocketstudio.app.library.IndexState.Ready(index), installed, seen = setOf("retro_techno", "detroit", "bruitages_corps"),
        )
    }

    /** New packs and a richer version published: the badge and « Tout compléter » at the top of the library. */
    @Test
    fun libraryNews() = shoot("library_news") {
        com.pocketstudio.app.library.LibraryScreen(
            com.pocketstudio.app.library.LibraryUi(allPacksUi(), target = "le pad 1 (Kick)"),
            com.pocketstudio.app.library.LibraryActions(),
        )
    }

    /** The library's search (« doigts ») and the pack list with « Tout télécharger » and an update. */
    @Config(qualifiers = "w400dp-h1500dp-xxhdpi")
    @Test
    open fun librarySearch() = shoot("library_search", prepare = {
        compose.onNode(androidx.compose.ui.test.hasTestTag("library_search")).performTextInput("doigts")
    }) {
        com.pocketstudio.app.library.LibraryScreen(
            com.pocketstudio.app.library.LibraryUi(allPacksUi(query = true), target = "le pad 1 (Kick)"),
            com.pocketstudio.app.library.LibraryActions(),
        )
    }

    /** « Tout télécharger » running: which pack, how far, Stop. */
    @Test
    fun libraryQueue() = shoot("library_queue") {
        com.pocketstudio.app.library.LibraryScreen(
            com.pocketstudio.app.library.LibraryUi(
                allPacksUi().copy(queue = com.pocketstudio.app.library.QueueUi(2, 5, "Percussions acoustiques", 0.46f)), target = "le pad 1 (Kick)",
            ),
            com.pocketstudio.app.library.LibraryActions(),
        )
    }

    @Test
    fun quickFx() = shoot("quick_fx") {
        androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.padding(androidx.compose.ui.unit.Dp(12f))) {
            val c = com.pocketstudio.model.Channel().let { it.withFx(com.pocketstudio.model.FxPresets.byId("underwater")!!) }
            com.pocketstudio.app.mixer.QuickFxPanel(2, c, com.pocketstudio.app.mixer.QuickFxActions())
        }
    }

    @Test
    fun synthChordPads() = shoot("synth_chord_pads", prepare = {
        compose.onNode(androidx.compose.ui.test.hasTestTag("chord_pads_switch")).performClick()
    }) {
        // The synth with its chord pads on (a guitarist's way in), in A minor.
        com.pocketstudio.app.synth.SynthScreen(
            patch = com.pocketstudio.model.SynthPatch.PRESETS[1].second,
            octave = 4,
            actions = com.pocketstudio.app.synth.SynthActions(),
            notes = com.pocketstudio.app.synth.NotesUi(
                com.pocketstudio.model.NoteClip(), emptyList(), -1, com.pocketstudio.app.synth.PianoRollActions(),
                harmony = com.pocketstudio.model.Harmony(9, com.pocketstudio.model.Scale.Minor, lock = true),
            ),
        )
    }

    @Test
    fun chordPads() = shoot("chord_pads") {
        androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.padding(androidx.compose.ui.unit.Dp(12f))) {
            com.pocketstudio.app.synth.ChordPads(com.pocketstudio.model.Harmony(9, com.pocketstudio.model.Scale.Minor, lock = true), 48, { _, _ -> }, {})
            com.pocketstudio.app.synth.Keyboard(48, 2, { _, _ -> }, {}, harmony = com.pocketstudio.model.Harmony(9, com.pocketstudio.model.Scale.Minor, lock = true))
        }
    }

    @Test
    fun generateFirstTime() = shoot("generate_first") {
        com.pocketstudio.app.generate.GenerateScreen(
            com.pocketstudio.app.generate.GenerateUi(false, com.pocketstudio.app.generate.ModelDownload.Running(0.31f),
                com.pocketstudio.app.generate.GenerateState.Idle, "", 4f),
            com.pocketstudio.app.generate.GenerateActions(),
        )
    }

    @Test
    fun generateResult() = shoot("generate_result") {
        com.pocketstudio.app.generate.GenerateScreen(
            com.pocketstudio.app.generate.GenerateUi(
                true, com.pocketstudio.app.generate.ModelDownload.Idle, com.pocketstudio.app.generate.GenerateState.Running(0.46f),
                "124 BPM house drum loop with hi-hats", 8f,
                result = "124 BPM house drum loop with hi-h", resultSeconds = 8.0, playing = true, projectBpm = 124, selectedPad = 2,
            ),
            com.pocketstudio.app.generate.GenerateActions(),
        )
    }

    @Test
    fun remixFirstTime() = shoot("remix_first") {
        com.pocketstudio.app.remix.RemixScreen(
            com.pocketstudio.app.remix.RemixUi(false, com.pocketstudio.app.remix.SeparationState.Idle, emptyList()),
            com.pocketstudio.app.remix.RemixActions(),
        )
    }

    @Test
    fun remixInProject() = shoot("remix_project") {
        val set = com.pocketstudio.app.remix.StemSet(
            File("/tmp/stems/abc"), "Ma chanson", 214.0, 0L, com.pocketstudio.app.remix.SongInfo(124.0, 0.25, 9, true),
        )
        val remix = com.pocketstudio.model.Remix("abc", "Ma chanson", 124.0, 0.25, startBar = 8)
            .withStem(2) { it.copy(mute = true) }.withStem(3) { it.copy(solo = true, volume = 0.6f) }
        com.pocketstudio.app.remix.RemixScreen(
            com.pocketstudio.app.remix.RemixUi(
                true, com.pocketstudio.app.remix.SeparationState.Idle, listOf(set),
                remix = remix, transportPlaying = true, projectBpm = 124.0,
                overview = com.pocketstudio.app.rhythm.StemOverview(
                    List(4) { st -> FloatArray(400) { i -> (if (st == 3 && i < 45) 0.02f else 0.3f + 0.5f * kotlin.math.abs(kotlin.math.sin(i * (0.11f + st * 0.05f)))) } },
                    48_000L * 214,
                ),
                position = 48_000L * 80,
            ),
            com.pocketstudio.app.remix.RemixActions(),
        )
    }

    @Test
    @Config(qualifiers = "w400dp-h1500dp-xxhdpi")
    fun acidEditor() = shoot("acid_editor") {
        val pattern = com.pocketstudio.model.AcidGenerator(kotlin.random.Random(3))
            .generate(com.pocketstudio.model.AcidPattern(length = 32, locked = setOf(2)))
        com.pocketstudio.app.acid.AcidScreen(
            acid = pattern,
            playing = false,
            currentStep = -1,
            selected = 2,
            actions = com.pocketstudio.app.acid.AcidActions(),
            // The song's curves move the cutoff and the resonance: a line under the knobs says so.
            automated = setOf(0, 1),
        )
    }

    @Test
    @Config(qualifiers = "w400dp-h1300dp-xxhdpi")
    fun mixerEffects() = shoot("mixer_effects", prepare = { tap("fx_chip_0_1") }) {
        val inserts = com.pocketstudio.model.Mixer.sanitizeInserts(listOf(
            com.pocketstudio.model.Insert.of(com.pocketstudio.model.EffectType.COMPRESSOR),
            com.pocketstudio.model.Insert.of(com.pocketstudio.model.EffectType.EQ).copy(params = listOf(0.75f, 0.45f, 0.6f, 0f)),
        ))
        val actions = com.pocketstudio.app.mixer.MixerActions()
        androidx.compose.foundation.layout.Column(
            androidx.compose.ui.Modifier.padding(androidx.compose.ui.unit.Dp(12f)),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(androidx.compose.ui.unit.Dp(10f)),
        ) {
            com.pocketstudio.ui.NeonCard {
                com.pocketstudio.ui.SectionLabel("Batterie · effets")
                com.pocketstudio.app.mixer.InsertChain(0, inserts, actions)
                com.pocketstudio.app.mixer.SendSliders(0, listOf(0.35f, 0.1f), actions)
            }
            com.pocketstudio.app.mixer.BusesCard(com.pocketstudio.model.Buses.DEFAULTS, actions)
        }
    }

    /**
     * Phase 33: the compressor open, its new settings (gain, « Gain auto », detector, bass filter,
     * mix) and its reduction meter (here 6.5 dB, as the engine's atomics report it).
     */
    @Test
    @Config(qualifiers = "w400dp-h1300dp-xxhdpi")
    fun compressorEditor() = shoot("compressor_editor", prepare = { tap("fx_chip_0_0") }) {
        // The assistant's drum bus glue (WizardMix.BUS_GLUE).
        val inserts = com.pocketstudio.model.Mixer.sanitizeInserts(listOf(
            com.pocketstudio.model.Insert(com.pocketstudio.model.EffectType.COMPRESSOR.ordinal, listOf(0.6f, 0.25f, 0.45f, 0.3f, 0f, 0f, 0f, 0.5f, 1f)),
            com.pocketstudio.model.Insert(com.pocketstudio.model.EffectType.SATURATION.ordinal, listOf(0.15f, 1f, 1f, 0.7f, 1f)),
        ))
        val reductions = FloatArray(com.pocketstudio.engine.NativeEngine.INSERT_METERS).also { it[0] = -6.5f }
        val actions = com.pocketstudio.app.mixer.MixerActions(reductions = kotlinx.coroutines.flow.MutableStateFlow(reductions))
        androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.padding(androidx.compose.ui.unit.Dp(12f))) {
            com.pocketstudio.ui.NeonCard {
                com.pocketstudio.ui.SectionLabel("Batterie · effets")
                com.pocketstudio.app.mixer.InsertChain(0, inserts, actions)
            }
        }
    }

    private fun tap(tag: String) {
        compose.onNode(androidx.compose.ui.test.hasTestTag(tag)).performClick()
        compose.waitForIdle()
    }

    /** A lead with three effects in its chain (the echo switched off), as in the effects box of the mixer or a track. */
    private fun leadFx(): com.pocketstudio.model.Channel = com.pocketstudio.model.Channel(
        inserts = com.pocketstudio.model.Mixer.sanitizeInserts(listOf(
            com.pocketstudio.model.Insert.of(com.pocketstudio.model.EffectType.DISTORTION),
            com.pocketstudio.model.Insert.of(com.pocketstudio.model.EffectType.ECHO).copy(bypass = true),
            com.pocketstudio.model.Insert.of(com.pocketstudio.model.EffectType.OTT),
        )),
        sends = listOf(0.2f, 0f),
    )

    /** The sound going through strip 2 (its effects light up), one vibe starred. */
    private fun fxActions(kind: com.pocketstudio.model.SoundKind) = com.pocketstudio.app.mixer.MixerActions(
        kindOf = { kind },
        meters = kotlinx.coroutines.flow.MutableStateFlow(FloatArray(com.pocketstudio.model.Mixer.CHANNELS + 1) { if (it == 2) 0.35f else 0f }),
        quick = com.pocketstudio.app.mixer.QuickFxActions(favourites = kotlinx.coroutines.flow.MutableStateFlow(setOf("scream_lead"))),
    )

    /** The vibes by music style (Dubstep), one starred, the one in use with its star. */
    @Test
    @Config(qualifiers = "w400dp-h1100dp-xxhdpi")
    fun effectsStyles() = shoot("effects_styles", prepare = { tap("fx_genre_dubstep") }) {
        androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.padding(androidx.compose.ui.unit.Dp(12f))) {
            com.pocketstudio.app.mixer.EffectsCard(
                com.pocketstudio.app.mixer.EffectsUi(2, com.pocketstudio.model.Channel().withFx(com.pocketstudio.model.FxPresets.byId("scream_lead")!!), fxActions(com.pocketstudio.model.SoundKind.LEAD)) {},
            )
        }
    }

    /** The whole effects box (Effets of a track, a pad, the mixer): the chain, one effect open, the vibes, pad XY and sends. */
    @Test
    @Config(qualifiers = "w400dp-h1500dp-xxhdpi")
    fun effectsBox() = shoot("effects_box", prepare = { tap("fx_chip_2_0") }) {
        androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.padding(androidx.compose.ui.unit.Dp(12f))) {
            com.pocketstudio.app.mixer.EffectsBox(2, leadFx(), fxActions(com.pocketstudio.model.SoundKind.LEAD))
        }
    }

    /**
     * « Joue dans »: with four scenes, the distortion plays in « Groove » and « Refrain » only, the
     * chorus everywhere; under the chain, what else is set on the sound (shared sends, Space).
     */
    @Test
    @Config(qualifiers = "w400dp-h1700dp-xxhdpi")
    open fun effectsScenes() = shoot("effects_scenes") {
        val channel = com.pocketstudio.model.Channel(
            inserts = com.pocketstudio.model.Mixer.sanitizeInserts(listOf(
                com.pocketstudio.model.Insert.of(com.pocketstudio.model.EffectType.DISTORTION).copy(scenes = setOf(1, 3)),
                com.pocketstudio.model.Insert.of(com.pocketstudio.model.EffectType.CHORUS),
                // Its only scene was replaced by another one: it says it plays nowhere.
                com.pocketstudio.model.Insert.of(com.pocketstudio.model.EffectType.WAH).copy(scenes = emptySet()),
            )),
            sends = listOf(0.3f, 0.2f),
            macros = listOf(0.5f, 0f, 0f, 0.25f),
        )
        val actions = com.pocketstudio.app.mixer.MixerActions(
            kindOf = { com.pocketstudio.model.SoundKind.LEAD },
            scenes = kotlinx.coroutines.flow.MutableStateFlow(listOf("Intro", "Groove", "", "Refrain")),
        )
        // Scrollable, as in the app: the tablet's shorter screen shows its top.
        androidx.compose.foundation.layout.Column(
            androidx.compose.ui.Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(androidx.compose.ui.unit.Dp(12f)),
        ) {
            com.pocketstudio.app.mixer.EffectsBox(2, channel, actions)
        }
    }

    /** « Petite pièce »: no effect in place, the card says what the ambience sets (Space), as the strip's line names it. */
    @Test
    @Config(qualifiers = "w400dp-h1000dp-xxhdpi")
    open fun effectsVibe() = shoot("effects_vibe") {
        val actions = com.pocketstudio.app.mixer.MixerActions(
            kindOf = { com.pocketstudio.model.SoundKind.PAD },
            scenes = kotlinx.coroutines.flow.MutableStateFlow(listOf("", "Refrain")),
        )
        androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.padding(androidx.compose.ui.unit.Dp(12f))) {
            com.pocketstudio.app.mixer.EffectsCard(
                com.pocketstudio.app.mixer.EffectsUi(2, com.pocketstudio.model.Channel().withFx(com.pocketstudio.model.FxPresets.byId("small_room")!!), actions) {},
            )
        }
    }

    /** « + Ajouter un effet »: every effect by family, the list open to add several in a row. */
    @Test
    @Config(qualifiers = "w400dp-h1300dp-xxhdpi")
    fun effectsPicker() = shoot("effects_picker", prepare = { tap("fx_add_2") }) {
        androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.padding(androidx.compose.ui.unit.Dp(12f))) {
            com.pocketstudio.app.mixer.EffectsCard(com.pocketstudio.app.mixer.EffectsUi(2, leadFx(), fxActions(com.pocketstudio.model.SoundKind.LEAD)) {})
        }
    }

    /**
     * Eight effects stacked on one sound: « 8 sur 8 », the list open says why no more can be added,
     * and the phone is struggling (the warning above the chain).
     */
    @Test
    @Config(qualifiers = "w400dp-h1800dp-xxhdpi")
    fun effectsFull() = shoot("effects_full", prepare = { tap("fx_add_2") }) {
        val eight = listOf(
            com.pocketstudio.model.EffectType.EQ, com.pocketstudio.model.EffectType.COMPRESSOR, com.pocketstudio.model.EffectType.DISTORTION,
            com.pocketstudio.model.EffectType.CHORUS, com.pocketstudio.model.EffectType.PHASER, com.pocketstudio.model.EffectType.ECHO,
            com.pocketstudio.model.EffectType.REVERB, com.pocketstudio.model.EffectType.OTT,
        ).map { com.pocketstudio.model.Insert.of(it) }
        val actions = com.pocketstudio.app.mixer.MixerActions(
            kindOf = { com.pocketstudio.model.SoundKind.LEAD },
            cpuBusy = kotlinx.coroutines.flow.MutableStateFlow(true),
        )
        androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.padding(androidx.compose.ui.unit.Dp(12f))) {
            com.pocketstudio.app.mixer.EffectsCard(com.pocketstudio.app.mixer.EffectsUi(2, com.pocketstudio.model.Channel(inserts = eight), actions) {})
        }
    }

    /** Synthé: the « Effets » card right under the sound (scrolled to it). */
    @Test
    fun synthEffects() = shoot("synth_effects", prepare = {
        compose.onNode(androidx.compose.ui.test.hasTestTag("effects_card")).performScrollTo()
        compose.waitForIdle()
    }) {
        com.pocketstudio.app.synth.SynthScreen(
            patch = com.pocketstudio.model.SynthPatch.PRESETS[1].second,
            octave = 4,
            actions = com.pocketstudio.app.synth.SynthActions(),
            effects = com.pocketstudio.app.mixer.EffectsUi(
                2, com.pocketstudio.model.Channel().withFx(com.pocketstudio.model.FxPresets.byId("fat_bass")!!), fxActions(com.pocketstudio.model.SoundKind.BASS),
            ) {},
        )
    }

    /** Acide: the same card under the 303's sound. */
    @Test
    fun acidEffects() = shoot("acid_effects", prepare = {
        compose.onNode(androidx.compose.ui.test.hasTestTag("effects_card")).performScrollTo()
        compose.waitForIdle()
    }) {
        val pattern = com.pocketstudio.model.AcidGenerator(kotlin.random.Random(3))
            .generate(com.pocketstudio.model.AcidPattern(length = 32, locked = setOf(2)))
        com.pocketstudio.app.acid.AcidScreen(
            acid = pattern, playing = false, currentStep = -1, selected = 2,
            actions = com.pocketstudio.app.acid.AcidActions(),
            effects = com.pocketstudio.app.mixer.EffectsUi(1, com.pocketstudio.model.Channel(), fxActions(com.pocketstudio.model.SoundKind.ACID)) {},
        )
    }

    @Test
    fun crazyEffects() = shoot("crazy_effects", prepare = { tap("fx_chip_4_0") }) {
        val inserts = com.pocketstudio.model.Mixer.sanitizeInserts(listOf(
            com.pocketstudio.model.Insert.of(com.pocketstudio.model.EffectType.CHOPPER).copy(params = listOf(0.3f, 1f, 0.2f, 0f)),
            com.pocketstudio.model.Insert.of(com.pocketstudio.model.EffectType.VINYL),
        ))
        androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.padding(androidx.compose.ui.unit.Dp(12f))) {
            com.pocketstudio.ui.NeonCard {
                com.pocketstudio.ui.SectionLabel("Nappe · effets")
                com.pocketstudio.app.mixer.InsertChain(4, inserts, com.pocketstudio.app.mixer.MixerActions())
            }
        }
    }

    @Test
    fun parametricEq() = shoot("param_eq", prepare = { tap("fx_chip_12_0") }) {
        val inserts = com.pocketstudio.model.Mixer.sanitizeInserts(listOf(
            com.pocketstudio.model.Insert(com.pocketstudio.model.EffectType.PARAM_EQ.ordinal, listOf(0.62f, 0.75f, 0.45f, 0.3f)),
            com.pocketstudio.model.Insert.of(com.pocketstudio.model.EffectType.PHASER),
            com.pocketstudio.model.Insert.of(com.pocketstudio.model.EffectType.BITCRUSHER),
        ))
        androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.padding(androidx.compose.ui.unit.Dp(12f))) {
            com.pocketstudio.ui.NeonCard {
                com.pocketstudio.ui.SectionLabel("Voix · effets")
                com.pocketstudio.app.mixer.InsertChain(12, inserts, com.pocketstudio.app.mixer.MixerActions())
            }
        }
    }

    @Test
    fun analyserAndTuner() = shoot("tuner") {
        androidx.compose.foundation.layout.Column {
            com.pocketstudio.ui.NeonCard(androidx.compose.ui.Modifier.padding(androidx.compose.ui.unit.Dp(12f))) {
                // As the app shows it: the title translated, the note names the settings ask for (C D E in English).
                com.pocketstudio.ui.SectionLabel(com.pocketstudio.model.i18n.tr("Analyseur de spectre"))
                com.pocketstudio.app.analysis.SpectrumView(FloatArray(40) { i -> -8f - i * 1.2f - if (i % 7 == 3) 0f else 6f })
            }
            com.pocketstudio.app.analysis.TunerView(
                com.pocketstudio.app.analysis.TunerReading(111.2, 45, -9.0), micOk = true,
                englishNotes = com.pocketstudio.app.settings.englishNotes(),
            )
        }
    }

    @Test
    @Config(qualifiers = "w400dp-h1700dp-xxhdpi")
    fun automation() = shoot("automation") {
        val lane = com.pocketstudio.model.AutomationLane(com.pocketstudio.model.AutoTarget(com.pocketstudio.model.AutoTarget.VOLUME, 2))
            .let { l -> (0 until 16).fold(l) { acc, s -> if (s % 2 == 0) acc.withPoint(s, 0.2f + 0.05f * s) else acc } }
        val send = com.pocketstudio.model.AutomationLane(com.pocketstudio.model.AutoTarget(com.pocketstudio.model.AutoTarget.SEND, 0, 0))
            .withPoint(0, 0f).withPoint(12, 0.9f)
        val filter = com.pocketstudio.model.AutomationLane(com.pocketstudio.model.AutoTarget(com.pocketstudio.model.AutoTarget.BUS_PARAM, 0, 0, 0),
            mode = com.pocketstudio.model.AutomationLane.SONG).withSongPoint(0, 0.1f).withSongPoint(8, 0.9f).withSongPoint(16, 0.5f).withSongPoint(23, 0.95f)
        androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.padding(androidx.compose.ui.unit.Dp(12f))) {
            com.pocketstudio.app.mixer.AutomationCard(
                com.pocketstudio.app.mixer.AutomationUi(listOf(lane, send, filter), 16, recording = true, songBars = 24, playStep = { 5 }, playSongStep = { 10 * 16 + 6 }, focusLane = 2),
                List(16) { com.pocketstudio.model.Channel() }, List(4) { com.pocketstudio.model.Insert() },
                com.pocketstudio.app.mixer.AutomationActions(),
            )
        }
    }

    @Test
    fun soundBrowser() = shoot("sound_browser") {
        androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.padding(androidx.compose.ui.unit.Dp(12f))) {
            com.pocketstudio.ui.NeonCard {
                com.pocketstudio.app.sounds.SoundBrowser(
                    com.pocketstudio.app.sounds.SoundBrowserUi(
                        com.pocketstudio.model.SoundCategories.SYNTH, com.pocketstudio.model.SoundLibrary.SYNTH,
                        listOf(com.pocketstudio.model.NamedSound("Mon son 1", "Favoris", com.pocketstudio.model.SynthPatch())),
                        current = "Basse reese",
                    ),
                )
            }
            com.pocketstudio.ui.NeonCard {
                com.pocketstudio.app.sounds.SoundBrowser(
                    com.pocketstudio.app.sounds.SoundBrowserUi(
                        com.pocketstudio.model.SoundCategories.ACID, com.pocketstudio.model.SoundLibrary.ACID, emptyList(), current = null, tag = "acid",
                    ),
                )
            }
        }
    }

    @Test
    fun projects() = shoot("projects") {
        val now = 1_800_000_000_000L
        val p = { id: String, name: String, ago: Long, bpm: Double, bars: Int, scenes: Int, remix: Boolean ->
            com.pocketstudio.model.ProjectInfo(id, name, now - ago, bpm, bars, scenes, remix)
        }
        androidx.compose.foundation.layout.Column {
            com.pocketstudio.app.ui.TopBar(
                com.pocketstudio.app.ui.TopBarUi("Da Funk (remix)", playing = true, canUndo = true, canRedo = false),
                com.pocketstudio.app.ui.TopBarActions(),
            )
            com.pocketstudio.app.projects.ProjectsScreen(
                com.pocketstudio.app.projects.ProjectsUi(
                    listOf(
                        p("a", "Da Funk (remix)", 120_000, 111.0, 8, 3, true),
                        p("b", "Acide du dimanche", 3 * 3_600_000, 128.0, 4, 1, false),
                        p("c", "Mon premier rythme", 30 * 3_600_000, 120.0, 1, 1, false),
                        p("d", "Until I Found You", 9 * 86_400_000L, 120.0, 2, 2, true),
                    ),
                    loading = false, deleted = "x@1" to "Essai",
                    trash = listOf(com.pocketstudio.model.TrashedProject("x@1", "Essai", now - 60_000)),
                ),
                currentId = "a",
                starters = listOf(com.pocketstudio.app.projects.ProjectStarter("Projet vide", "Tout est vierge.", "empty")),
                actions = com.pocketstudio.app.projects.ProjectsActions(),
                now = now,
            )
        }
    }

    /** « Nouveau projet »: the guided song, then the assistant's styles, each made whole in one touch. */
    @Test
    fun projectsNew() = shoot("projects_new", prepare = { tap("project_new") }) {
        com.pocketstudio.app.projects.ProjectsScreen(
            com.pocketstudio.app.projects.ProjectsUi(loading = false), currentId = "a",
            starters = com.pocketstudio.app.ui.PROJECT_STARTERS.map { it.first },
            actions = com.pocketstudio.app.projects.ProjectsActions(),
        )
    }

    /** A style's song being prepared after its touch: a wheel in its card, the other choices waiting. */
    @Test
    fun projectsNewPreparing() = shoot("projects_new_preparing", prepare = { tap("project_new") }) {
        com.pocketstudio.app.projects.ProjectsScreen(
            com.pocketstudio.app.projects.ProjectsUi(loading = false), currentId = "a",
            starters = com.pocketstudio.app.ui.PROJECT_STARTERS.map { it.first },
            actions = com.pocketstudio.app.projects.ProjectsActions(),
            quick = com.pocketstudio.app.library.QuickUi(creating = "trap"),
        )
    }

    /** The trash unfolded: restore, delete for good, empty the bin. */
    @Test
    fun projectsTrash() = shoot("projects_trash", prepare = {
        compose.onNodeWithTag("project_trash").performScrollTo().performClick()
        compose.waitForIdle()
    }) {
        val now = 1_800_000_000_000L
        com.pocketstudio.app.projects.ProjectsScreen(
            com.pocketstudio.app.projects.ProjectsUi(
                listOf(com.pocketstudio.model.ProjectInfo("a", "Da Funk (remix)", now - 120_000, 111.0, 8, 3, true)),
                loading = false,
                trash = listOf(
                    com.pocketstudio.model.TrashedProject("x@2", "Essai", now - 60_000),
                    com.pocketstudio.model.TrashedProject("y@1", "Prise voix (5 min)", now - 3 * 86_400_000L),
                ),
            ),
            currentId = "a",
            starters = emptyList(),
            actions = com.pocketstudio.app.projects.ProjectsActions(),
            now = now,
        )
    }

    @Test
    @Config(qualifiers = "w400dp-h1300dp-xxhdpi")
    open fun settings() = shoot("settings") {
        com.pocketstudio.app.settings.SettingsScreen(
            com.pocketstudio.app.settings.AppSettings(uiScale = 1.15f, bufferBursts = 4, guideSeen = true),
            "0.5.70 (preview)",
            com.pocketstudio.app.settings.SettingsActions(),
        )
    }

    @Test
    fun welcome() = shoot("welcome") {
        com.pocketstudio.app.onboarding.WelcomeScreen(com.pocketstudio.app.onboarding.WelcomeActions())
    }

    @Test
    fun nextSteps() = shoot("next_steps") {
        val project = com.pocketstudio.app.rhythm.RhythmViewModel.starterProject()
        RhythmScreen(
            project = project,
            transport = TransportUi(selectedPad = 0),
            live = LiveUi(),
            canUndo = false,
            canRedo = false,
            actions = RhythmActions(),
            header = { com.pocketstudio.app.onboarding.NextStepCard(com.pocketstudio.app.onboarding.nextSteps(project)) },
        )
    }

    /**
     * Right after the assistant on a small phone (360 x 800 dp): the top bar with ⤴, the next step
     * on one thin line under it (as on every creation tab), the first cells on screen.
     */
    @Test
    @Config(qualifiers = "w360dp-h616dp-xxhdpi")
    fun rhythmAfterWizard() = shoot("rhythm_after_wizard") {
        val project = com.pocketstudio.app.rhythm.RhythmViewModel.starterProject()
        val steps = com.pocketstudio.app.onboarding.nextSteps(project)
            .map { it.copy(done = it.goal < com.pocketstudio.app.onboarding.StepGoal.Volumes) }
        androidx.compose.foundation.layout.Column {
            com.pocketstudio.app.ui.TopBar(
                com.pocketstudio.app.ui.TopBarUi("House du soir", playing = false, canUndo = true, canRedo = false, scene = com.pocketstudio.app.ui.sceneChip(project), saved = true),
                com.pocketstudio.app.ui.TopBarActions(),
            )
            com.pocketstudio.app.onboarding.NextStepStrip(steps, com.pocketstudio.app.ui.AppTab.Rythme)
            Box(Modifier.weight(1f)) {
                RhythmScreen(
                    project = project,
                    transport = TransportUi(selectedPad = 0),
                    live = LiveUi(),
                    canUndo = false,
                    canRedo = false,
                    actions = RhythmActions(),
                )
            }
        }
    }

    /** ⤴ « Prêt à partager ? »: a solo left on, a muted track that plays, the limiter working hard, each with its fix. */
    @Test
    fun shareSheet() = shoot("share_sheet") {
        com.pocketstudio.app.export.ShareSheetContent(
            com.pocketstudio.app.export.ShareSheetUi(
                "Nuit sur la ville", seconds = 178, parts = 6,
                checks = listOf(
                    com.pocketstudio.app.export.ShareCheck.Solo(listOf("Basse acide")),
                    com.pocketstudio.app.export.ShareCheck.Muted(listOf("Voix 1")),
                    com.pocketstudio.app.export.ShareCheck.Saturation,
                ),
                playing = true,
            ),
            com.pocketstudio.app.export.ShareSheetActions(),
            Modifier.background(Neon.Surface).padding(top = 16.dp),
        )
    }

    /** Nothing to fix: « Tout est prêt », WAV chosen (kept for next time), one big « Partager ». */
    @Test
    fun shareSheetAllClear() = shoot("share_sheet_ok") {
        com.pocketstudio.app.export.ShareSheetContent(
            com.pocketstudio.app.export.ShareSheetUi("Nuit sur la ville", seconds = 178, parts = 6, checks = emptyList(), format = com.pocketstudio.app.rhythm.ExportFormat.Wav),
            com.pocketstudio.app.export.ShareSheetActions(),
            Modifier.background(Neon.Surface).padding(top = 16.dp),
        )
    }

    /** « Ton morceau est prêt », once after the assistant: four big choices over the Morceau tab. */
    @Test
    fun songReady() = shoot("song_ready") {
        Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.55f)).padding(12.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
            com.pocketstudio.app.onboarding.SongReadyCard(
                com.pocketstudio.app.onboarding.SongReadyUi("House du soir", seconds = 160, parts = 6),
                com.pocketstudio.app.onboarding.SongReadyActions(guide = {}),
            )
        }
    }

    @Test
    fun guide() = shoot("guide") {
        com.pocketstudio.app.settings.GuideDialog(onClose = {})
    }

    @Test
    fun midi() = shoot("midi") {
        com.pocketstudio.app.midi.MidiScreen(
            com.pocketstudio.app.midi.MidiUi(
                sources = listOf(
                    com.pocketstudio.app.midi.MidiSource("midi-1", "Arturia MiniLab 3", "USB"),
                    com.pocketstudio.app.midi.MidiSource("ble-2", "Clavier Bluetooth", "Bluetooth"),
                ),
                connected = "midi-1", last = "Note Do4", clockBpm = 124.0, sendClock = true,
            ),
            com.pocketstudio.app.midi.MidiActions(),
            com.pocketstudio.app.midi.LearnUi(
                mappings = listOf(74 to "Synthé : Coupure (piste choisie)", 71 to "Volume · Batterie"),
                waiting = "Effet live : filtre",
            ),
        )
    }

    @Test
    fun playPanel() = shoot("play_panel") {
        com.pocketstudio.app.synth.SynthScreen(
            patch = com.pocketstudio.model.SynthPatch.PRESETS[1].second,
            octave = 4,
            actions = com.pocketstudio.app.synth.SynthActions(),
            play = com.pocketstudio.app.rhythm.PlayUi(bend = 0.4f, mod = 0.3f, sustain = true,
                arp = com.pocketstudio.app.rhythm.ArpUi(on = true, steps = 2f, mode = 2, octaves = 2)),
        )
    }

    @Test
    fun acidPanel() = shoot("acid_panel") {
        androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.padding(androidx.compose.ui.unit.Dp(12f))) {
            com.pocketstudio.app.acid.Panel303(com.pocketstudio.model.AcidSound(cutoff = 0.62f, waveform = 1f).values(), { _, _ -> })
        }
    }

    @Test
    @Config(qualifiers = "w400dp-h1700dp-xxhdpi")
    open fun session() = shoot("session") {
        // The song first (part 1 plays, part 2 comes next), then the named scenes, then the live tools.
        val drums = com.pocketstudio.model.DrumPattern().toggle(0, 0).toggle(2, 4)
        val a = com.pocketstudio.model.Scene("A", drums, label = "Intro")
        val b = com.pocketstudio.model.Scene("B", drums.copy(steps = 32), label = "Couplet")
        val c = com.pocketstudio.model.Scene("C", drums, label = "Refrain")
        com.pocketstudio.app.session.SessionScreen(
            com.pocketstudio.app.session.SessionScreenUi(
                scenes = listOf(a, b, c, com.pocketstudio.model.Scene("D")) + List(5) { null } + com.pocketstudio.model.Scene("J", drums) + List(14) { null },
                current = 0, pending = 1,
                song = listOf(
                    com.pocketstudio.model.SongPart(0, 2), com.pocketstudio.model.SongPart(1, 2),
                    com.pocketstudio.model.SongPart(2, 2), com.pocketstudio.model.SongPart(1), com.pocketstudio.model.SongPart(2, 4),
                ),
                songIndex = 0, playing = true, tempoBpm = 124.0, upcoming = 1,
                // A bar and a half into the song: the clock, the bar and the line say where it is.
                songStep = { 16 + 9 },
            ),
            com.pocketstudio.app.session.SessionActions(),
        )
    }

    @Test
    @Config(qualifiers = "w400dp-h2600dp-xxhdpi")
    open fun whatPlays() = shoot("what_plays") {
        // « Ce qui joue » in the Break of the assistant's Jump up DnB: each track, its effects, sends and curves.
        val p = WhatPlaysTest.wizard("jump_up")
        val strips = com.pocketstudio.app.ui.mixerStrips(p)
        val brk = p.allScenesList().indexOfFirst { it.label == com.pocketstudio.model.StarterWizard.SongForm.BREAK_LABEL }.takeIf { it >= 0 } ?: 3
        val scenes = p.song.map { it.scene }.distinct().sorted().map { it to com.pocketstudio.model.Scene.NAMES[it] + " · " + com.pocketstudio.model.Scene.shownLabel(p.allScenesList()[it].label) }
        androidx.compose.foundation.layout.Column(Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(12.dp)) {
            com.pocketstudio.ui.NeonCard {
                androidx.compose.material3.Text(com.pocketstudio.model.i18n.tr("Ce qui joue"), style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
                com.pocketstudio.app.mixer.WhatPlaysContent(com.pocketstudio.app.mixer.whatPlaysGroups(p, brk, strips), brk, null, scenes, {}, {}, {})
            }
        }
    }

    @Test
    @Config(qualifiers = "w400dp-h1800dp-xxhdpi")
    open fun automationPicker() = shoot("automation_picker") {
        // « Que veux-tu faire bouger ? »: by track, the filter first (or one to add), then volume and reverb.
        val p = withTracks().let { p ->
            val ch = p.mixer.channels.toMutableList()
            val lead = com.pocketstudio.model.InstrumentTrack.mixerChannel(p.instrumentTracks.last().id)
            ch[lead] = ch[lead].copy(inserts = listOf(com.pocketstudio.model.Insert.of(com.pocketstudio.model.EffectType.FILTER)) + ch[lead].inserts.drop(1))
            p.copy(mixer = p.mixer.copy(channels = ch))
        }
        androidx.compose.foundation.layout.Column(Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(12.dp)) {
            com.pocketstudio.ui.NeonCard {
                androidx.compose.material3.Text(com.pocketstudio.model.i18n.tr("Que veux-tu faire bouger ?"), style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
                com.pocketstudio.app.mixer.TargetPickerContent(emptySet(), p.mixer.channels, p.mixer.master, com.pocketstudio.app.ui.mixerStrips(p), {}, {})
            }
        }
    }

    @Test
    @Config(qualifiers = "w400dp-h1500dp-xxhdpi")
    open fun mixerMoves() = shoot("mixer_moves") {
        // The assistant's Rétro techno in the mixer: « Automation et effets » on top, a badge on each track that moves.
        val p = WhatPlaysTest.wizard("retro_techno")
        com.pocketstudio.app.mixer.MixerScreen(
            channels = p.mixer.channels,
            meters = { FloatArray(com.pocketstudio.engine.NativeEngine.METERS) { 0.4f }.also { it[com.pocketstudio.engine.NativeEngine.METER_LIMITER] = 1f } },
            actions = com.pocketstudio.app.mixer.MixerActions(),
            strips = com.pocketstudio.app.ui.mixerStrips(p),
            master = p.mixer.master,
            automation = com.pocketstudio.app.mixer.AutomationUi(p.automation, 16, songBars = p.songBarsCount(), stepsPerBar = 16, playSongStep = { 16 * 20 + 8 }),
            hasSong = true,
        )
    }

    @Test
    fun harmonyPanel() = shoot("harmony_panel") {
        // A melody without a chord (one note out of Do mineur): the chord playing is off and says
        // where the sound is set; the scale says it keeps the notes, and offers to fit them.
        val melody = listOf(60, 63, 66, 67).mapIndexed { i, p -> com.pocketstudio.model.Note(i * 48, 40, p) }
        androidx.compose.foundation.layout.Column(
            Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(12.dp),
        ) {
            com.pocketstudio.ui.NeonCard {
                com.pocketstudio.ui.SectionLabel(com.pocketstudio.model.i18n.tr("Gamme et accords"))
                com.pocketstudio.app.synth.HarmonyPanel(
                    com.pocketstudio.model.Harmony(0, com.pocketstudio.model.Scale.Minor),
                    com.pocketstudio.model.NoteClip(16, melody, performance = com.pocketstudio.model.Performance(arp = com.pocketstudio.model.ArpMode.Up)),
                    com.pocketstudio.app.synth.PianoRollActions(), toSound = {},
                )
            }
        }
    }

    @Test
    fun sceneDrag() = shoot("scene_drag", prepare = {
        // Scene B held and carried towards F: it follows the finger, the slot under it lights up.
        compose.onNodeWithTag("scene_5").performScrollTo()
        val a = compose.onNodeWithTag("scene_1").fetchSemanticsNode().boundsInRoot
        val b = compose.onNodeWithTag("scene_5").fetchSemanticsNode().boundsInRoot
        compose.onNodeWithTag("scene_1").performTouchInput {
            down(center)
            advanceEventTime(viewConfiguration.longPressTimeoutMillis + 200)
            repeat(10) { moveBy((b.center - a.center) / 10f, delayMillis = 16) }
        }
        compose.waitForIdle()
    }) {
        val drums = com.pocketstudio.model.DrumPattern().toggle(0, 0).toggle(2, 4)
        com.pocketstudio.app.session.SessionScreen(
            com.pocketstudio.app.session.SessionScreenUi(
                scenes = listOf(
                    com.pocketstudio.model.Scene("A", drums, label = "Intro"), com.pocketstudio.model.Scene("B", drums, label = "Couplet"),
                    com.pocketstudio.model.Scene("C", drums, label = "Refrain"),
                ) + List(21) { null },
                current = 0, pending = null, song = emptyList(), songIndex = -1, playing = false, tempoBpm = 124.0,
            ),
            com.pocketstudio.app.session.SessionActions(),
        )
    }

    @Test
    fun keyLayouts() = shoot("key_layouts") {
        // One octave with every note named, then only the notes of La mineur.
        androidx.compose.foundation.layout.Column(Modifier.padding(12.dp), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)) {
            val harmony = com.pocketstudio.model.Harmony(9, com.pocketstudio.model.Scale.Minor)
            com.pocketstudio.app.synth.PlayKeys(com.pocketstudio.app.settings.KeyLayout.OneOctave, 4, 2, harmony, false, { _, _ -> }, {}, 170.dp)
            com.pocketstudio.app.synth.PlayKeys(com.pocketstudio.app.settings.KeyLayout.Scale, 4, 2, harmony, false, { _, _ -> }, {}, 170.dp)
            com.pocketstudio.app.synth.PlayKeys(com.pocketstudio.app.settings.KeyLayout.TwoOctaves, 4, 2, harmony, false, { _, _ -> }, {}, 150.dp)
        }
    }

    @Test
    fun foldedCards() = shoot("folded_cards") {
        // "Scènes" folded: only its title stays, with the small chevron turned.
        androidx.compose.runtime.CompositionLocalProvider(
            com.pocketstudio.ui.LocalFoldStore provides com.pocketstudio.ui.MemoryFoldStore(mapOf("Scènes" to true)),
        ) {
            val drums = com.pocketstudio.model.DrumPattern().toggle(0, 0).toggle(2, 4)
            com.pocketstudio.app.session.SessionScreen(
                com.pocketstudio.app.session.SessionScreenUi(
                    scenes = listOf(com.pocketstudio.model.Scene("A", drums)) + List(15) { null },
                    current = 0, pending = null, song = emptyList(), songIndex = -1, playing = false, tempoBpm = 124.0,
                ),
                com.pocketstudio.app.session.SessionActions(),
            )
        }
    }

    @Test
    fun floatingKeyboard() = shoot("floating_keyboard") {
        Box(Modifier.fillMaxSize()) {
            androidx.compose.foundation.layout.Column(Modifier.padding(12.dp)) {
                com.pocketstudio.ui.NeonCard {
                    com.pocketstudio.ui.SectionLabel("Sons prêts à jouer")
                    com.pocketstudio.app.sounds.SoundBrowser(
                        com.pocketstudio.app.sounds.SoundBrowserUi(
                            com.pocketstudio.model.SoundCategories.SYNTH, com.pocketstudio.model.SoundLibrary.SYNTH, emptyList(),
                            current = com.pocketstudio.model.SoundLibrary.SYNTH[1].name,
                            describe = { (it.sound as? com.pocketstudio.model.SynthPatch)?.let(com.pocketstudio.app.sounds.SoundLooks::describe) },
                        ),
                    )
                }
            }
            com.pocketstudio.app.synth.FloatingKeyboard(
                visible = true, folded = false, onFold = {}, octave = 4, octaves = 2, keysHeight = 120.dp,
                actions = com.pocketstudio.app.synth.SynthActions(), harmony = com.pocketstudio.model.Harmony(), chordPads = false,
                keyOn = { _, _ -> }, keyOff = {}, modifier = Modifier.align(androidx.compose.ui.Alignment.BottomCenter),
            )
        }
    }

    @Test
    @Config(qualifiers = "w400dp-h2200dp-xxhdpi")
    open fun timeline() = shoot("timeline") {
        val drums = com.pocketstudio.model.DrumPattern().toggle(0, 0).toggle(2, 4)
        val a = com.pocketstudio.model.Scene("A", drums)
        val b = com.pocketstudio.model.Scene("B", drums.copy(steps = 32))
        val song = listOf(
            com.pocketstudio.model.SongPart(0, 2, muted = setOf("acid", "inst:1", "inst:2", "inst:3", "audio:0")),
            com.pocketstudio.model.SongPart(1, 2, muted = setOf("audio:0")),
            com.pocketstudio.model.SongPart(0, 4),
            com.pocketstudio.model.SongPart(1, 1, muted = setOf("drums", "acid")),
            // Written from the live grid: the acid on B, the drums carrying on, two bars of its own.
            com.pocketstudio.model.SongPart(0, 1, play = mapOf("acid" to 1), from = mapOf("drums" to 1), bars = 2),
        )
        // The voice: a song take from part 3, heard into the start of part 4.
        var p = withTracks().copy(drums = drums, scenes = listOf(a, b), song = song).sanitized()
        p = p.withAudioTrack(p.audioTracks[0].copy(clip = com.pocketstudio.model.AudioClip("v.wav", "Prise", 9.0, start = 96, song = true)))
        com.pocketstudio.app.session.SessionScreen(
            com.pocketstudio.app.session.SessionScreenUi(
                scenes = listOf(a, b) + List(22) { null },
                current = 1, pending = null,
                song = song,
                songIndex = 1, playing = true, tempoBpm = 124.0, beatsPerBar = 4,
                rows = com.pocketstudio.app.ui.timelineRows(p),
            ),
            com.pocketstudio.app.session.SessionActions(),
        )
    }

    /** A song of four sections over every kind of track, a filter curve on the 303, playing in the Drop. */
    private fun timelineUi(): com.pocketstudio.app.session.TimelineUi {
        var drums = com.pocketstudio.model.DrumPattern()
        listOf(0, 4, 8, 12).forEach { drums = drums.toggle(0, it) }
        listOf(4, 12).forEach { drums = drums.toggle(2, it) }
        (0 until 16 step 2).forEach { drums = drums.toggle(5, it, 0.6f) }
        val acid = com.pocketstudio.model.AcidPattern().let { a ->
            a.copy(steps = a.steps.mapIndexed { i, st -> if (i % 3 != 1) st.copy(gate = true, note = (i * 5) % 12, accent = i % 4 == 0, slide = i % 7 == 0) else st })
        }
        val chords = com.pocketstudio.model.NoteClip(32, listOf(60, 63, 67, 58, 62, 65).mapIndexed { i, n -> com.pocketstudio.model.Note(if (i < 3) 0 else 16, 16, n) })
        val lead = com.pocketstudio.model.NoteClip(16, listOf(72, 75, 79, 77, 75, 72).mapIndexed { i, n -> com.pocketstudio.model.Note(i * 2 + (i / 3) * 4, 2, n) })
        var p = withTracks().copy(drums = drums, acid = acid, synthNotes = chords).sanitized()
        val leadId = p.tracks.last().id
        p = p.withTrack(p.track(leadId).copy(notes = lead)).sanitized()
        p = p.renameScene(0, "Intro").copySceneTo(1).copySceneTo(2).renameScene(1, "Montée").renameScene(2, "Drop")
        p = p.copy(
            song = listOf(
                com.pocketstudio.model.SongPart(0, 2, muted = setOf("acid", "inst:$leadId", "audio:0")),
                com.pocketstudio.model.SongPart(1, 2, muted = setOf("drums")),
                com.pocketstudio.model.SongPart(2, 4),
                com.pocketstudio.model.SongPart(0, 1, play = mapOf("acid" to 2), from = mapOf("drums" to 1), bars = 2),
                com.pocketstudio.model.SongPart(2, 2, muted = setOf("inst:0")),
            ),
            automation = listOf(
                com.pocketstudio.model.AutomationLane(
                    com.pocketstudio.model.AutoTarget(com.pocketstudio.model.AutoTarget.ACID, ch = com.pocketstudio.model.AutoTarget.ACID_CHANNEL),
                    mode = com.pocketstudio.model.AutomationLane.SONG, songValues = listOf(0.1f, null, null, 0.6f, null, 0.3f, null, null, 0.9f, null, null, 0.4f),
                ),
            ),
        ).sanitized()
        p = p.withAudioTrack(p.audioTracks[0].copy(clip = com.pocketstudio.model.AudioClip("v.wav", "Prise", 9.0, start = 64, song = true)))
        val rows = com.pocketstudio.app.ui.timelineRows(p)
        return com.pocketstudio.app.session.TimelineUi(
            p, rows, listOf(com.pocketstudio.app.session.TimelineLane(0, "acid", "303 Coupure · Basse acide")),
            playing = true, songIndex = 2, upcoming = 3, songStep = { 100 }, loop = 2..3, canUndo = true,
        )
    }

    @Test
    @Config(qualifiers = "w400dp-h860dp-xxhdpi")
    fun timelineScreen() = shoot("timeline_screen", fullScreen = true) {
        com.pocketstudio.app.session.TimelineScreen(timelineUi(), com.pocketstudio.app.session.TimelineActions(), barDp = androidx.compose.ui.unit.Dp(22f))
    }

    @Test
    @Config(qualifiers = "w860dp-h400dp-xxhdpi")
    fun timelineLandscape() = shoot("timeline_landscape", fullScreen = true) {
        com.pocketstudio.app.session.TimelineScreen(timelineUi(), com.pocketstudio.app.session.TimelineActions(), barDp = androidx.compose.ui.unit.Dp(36f))
    }

    @Test
    @Config(qualifiers = "w400dp-h1500dp-xxhdpi")
    fun livePads() = shoot("live_pads") {
        val clip = { n: String, b: Int -> com.pocketstudio.model.LiveClip("x.wav", n, b) }
        val pads = com.pocketstudio.model.LivePads(8).with(0, clip("Kick 909", 1)).with(1, clip("Charley", 2))
            .with(2, clip("Basse Da Funk", 8)).with(3, clip("Voix refrain", 16)).with(5, clip("Clap", 4).copy(oneShot = true))
        androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.padding(androidx.compose.ui.unit.Dp(12f))) {
            com.pocketstudio.app.session.LivePadsCard(
                com.pocketstudio.app.session.LivePadsUi(pads, listOf(2, 2, 1, 0, 0, 0, 0, 0), editing = null),
                com.pocketstudio.app.session.LivePadsActions(),
            )
            com.pocketstudio.app.session.LivePadsCard(
                com.pocketstudio.app.session.LivePadsUi(pads, List(8) { 0 }, editing = 2, drumSounds = listOf(0 to "Kick importé", 3 to "Snare")),
                com.pocketstudio.app.session.LivePadsActions(),
            )
        }
    }

    /** The live pads on the whole screen, as the assistant fills them (Jump up DnB): two playing, one queued. */
    @Composable
    private fun LivePadsStage() {
        val clip = { n: String, b: Int -> com.pocketstudio.model.LiveClip("x.wav", n, b) }
        val pads = com.pocketstudio.model.LivePads(8).with(0, clip("Batterie", 8)).with(1, clip("Basse", 16)).with(2, clip("Nappe", 16))
            .with(3, clip("Sub", 8)).with(4, clip("Impact", 4).copy(oneShot = true)).with(5, clip("Morceau 4", 32))
        val ui = liveUi()
        val columns = com.pocketstudio.app.live.liveColumnsOf(ui.project)
        com.pocketstudio.app.session.LivePadsFullScreen(
            com.pocketstudio.app.session.LivePadsUi(pads, listOf(2, 2, 1, 0, 0, 0, 0, 0), editing = null),
            com.pocketstudio.app.session.LivePadsActions(), close = {},
            tempoBpm = 174.0, beatsPerBar = 4, playing = true, clockOf = { 45 }, togglePlay = {},
            effects = { m -> com.pocketstudio.app.live.LiveFx(ui, columns, com.pocketstudio.app.live.LiveActions(), m) },
        )
    }

    @Test
    fun livePadsFull() = shoot("live_pads_full", fullScreen = true, prepare = {
        // A moment into the loops: the beat lit, the rings going round.
        compose.mainClock.advanceTimeBy(700)
    }) { LivePadsStage() }

    @Test
    fun livePadsFullFx() = shoot("live_pads_full_fx", fullScreen = true, prepare = {
        compose.onNodeWithTag("live_pads_fx_toggle").performClick()
    }) { LivePadsStage() }

    @Test
    @Config(qualifiers = "w860dp-h400dp-land-xxhdpi")
    fun livePadsFullLandscape() = shoot("live_pads_full_landscape", fullScreen = true, prepare = {
        compose.onNodeWithTag("live_pads_fx_toggle").performClick()
    }) { LivePadsStage() }

    @Test
    @Config(qualifiers = "w400dp-h1500dp-xxhdpi")
    fun plaits() = shoot("plaits") {
        androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.padding(androidx.compose.ui.unit.Dp(12f))) {
            com.pocketstudio.app.instruments.InstrumentPanel(
                com.pocketstudio.app.instruments.InstrumentUi(
                    com.pocketstudio.app.rhythm.LibraryState.Missing, null,
                    synthEngine = 1, plaits = com.pocketstudio.model.PlaitsPatch(model = 2, timbre = 0.7f),
                ),
            )
        }
    }
}
