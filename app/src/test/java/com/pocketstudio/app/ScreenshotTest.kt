package com.pocketstudio.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.pocketstudio.model.withFx
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
class ScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private val stream = StreamInfo(
        sampleRate = 48000, framesPerBurst = 96, bufferSize = 192, bufferCapacity = 3072,
        exclusive = true, aaudio = true, mmap = true, lowLatency = true, xruns = 0, outputLatencyMs = 9.6,
        running = true, callbacks = 1000, peak = 0.42f,
    )

    private fun shoot(name: String, palette: Palette = Palettes.Studio, content: @Composable () -> Unit) {
        compose.setContent {
            PocketStudioTheme(palette) {
                Box(Modifier.fillMaxSize().background(Neon.Background)) { content() }
            }
        }
        compose.onRoot().captureRoboImage("screenshots/$name.png")
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
        com.pocketstudio.app.synth.SynthScreen(
            patch = p.synth,
            octave = 4,
            actions = com.pocketstudio.app.synth.SynthActions(),
            tracks = com.pocketstudio.app.synth.TracksUi(p.instrumentTracks, selected = 2),
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
            ),
            com.pocketstudio.app.audiotracks.AudioTracksActions(),
        )
    }

    @Test
    fun mixerTracks() = shoot("mixer_tracks") {
        val p = withTracks()
        com.pocketstudio.app.mixer.MixerScreen(
            channels = p.mixer.channels,
            meters = FloatArray(17) { if (it in listOf(0, 2, 5, 6, 4, 12)) 0.5f + it * 0.02f else 0f }.also { it[16] = 1f },
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
            meters = FloatArray(17).also { m -> listOf(0.62f, 0.81f, 0f, 0.4f, 0.88f).forEachIndexed { i, v -> m[i] = v }; m[16] = 0.8f },
            actions = com.pocketstudio.app.mixer.MixerActions(),
            export = com.pocketstudio.app.rhythm.ExportUi(bars = 4, files = listOf(java.io.File("Mon-premier-rythme-20261018-221500.wav")), savedInMusic = true),
        )
    }

    @Test
    fun mixerExporting() = shoot("mixer_export") {
        androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.padding(androidx.compose.ui.unit.Dp(12f))) {
            com.pocketstudio.app.mixer.ExportCard(
                com.pocketstudio.app.rhythm.ExportUi(bars = 8, what = com.pocketstudio.app.rhythm.ExportWhat.Stems, running = true, progress = 0.4f, stage = "Acide (2/4)"),
                com.pocketstudio.app.mixer.MixerActions(),
            )
            com.pocketstudio.app.mixer.ExportCard(
                com.pocketstudio.app.rhythm.ExportUi(
                    bars = 4, what = com.pocketstudio.app.rhythm.ExportWhat.Song, format = com.pocketstudio.app.rhythm.ExportFormat.M4a,
                    files = listOf(java.io.File("Da-Funk-morceau-20261020-101500.m4a")), savedInMusic = true,
                ),
                com.pocketstudio.app.mixer.MixerActions(),
            )
        }
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

    @Test
    fun quickFx() = shoot("quick_fx") {
        androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.padding(androidx.compose.ui.unit.Dp(12f))) {
            val c = com.pocketstudio.model.Channel().let { it.withFx(com.pocketstudio.model.FxPresets.byId("underwater")!!) }
            com.pocketstudio.app.mixer.QuickFxPanel(2, c, com.pocketstudio.app.mixer.QuickFxActions())
        }
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
        )
    }

    @Test
    @Config(qualifiers = "w400dp-h1300dp-xxhdpi")
    fun mixerEffects() = shoot("mixer_effects") {
        val inserts = listOf(
            com.pocketstudio.model.Insert.of(com.pocketstudio.model.EffectType.COMPRESSOR),
            com.pocketstudio.model.Insert.of(com.pocketstudio.model.EffectType.EQ).copy(params = listOf(0.75f, 0.45f, 0.6f, 0f)),
            com.pocketstudio.model.Insert(), com.pocketstudio.model.Insert(),
        )
        val actions = com.pocketstudio.app.mixer.MixerActions()
        androidx.compose.foundation.layout.Column(
            androidx.compose.ui.Modifier.padding(androidx.compose.ui.unit.Dp(12f)),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(androidx.compose.ui.unit.Dp(10f)),
        ) {
            com.pocketstudio.ui.NeonCard {
                com.pocketstudio.ui.SectionLabel("Batterie · effets")
                com.pocketstudio.app.mixer.InsertRack(0, inserts, actions)
                com.pocketstudio.app.mixer.SendSliders(0, listOf(0.35f, 0.1f), actions)
            }
            com.pocketstudio.app.mixer.BusesCard(com.pocketstudio.model.Buses.DEFAULTS, actions)
        }
    }

    @Test
    fun parametricEq() = shoot("param_eq") {
        val inserts = listOf(
            com.pocketstudio.model.Insert(com.pocketstudio.model.EffectType.PARAM_EQ.ordinal, listOf(0.62f, 0.75f, 0.45f, 0.3f)),
            com.pocketstudio.model.Insert.of(com.pocketstudio.model.EffectType.PHASER),
            com.pocketstudio.model.Insert.of(com.pocketstudio.model.EffectType.BITCRUSHER), com.pocketstudio.model.Insert(),
        )
        androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.padding(androidx.compose.ui.unit.Dp(12f))) {
            com.pocketstudio.ui.NeonCard {
                com.pocketstudio.ui.SectionLabel("Voix · effets")
                com.pocketstudio.app.mixer.InsertRack(12, inserts, com.pocketstudio.app.mixer.MixerActions())
            }
        }
    }

    @Test
    fun analyserAndTuner() = shoot("tuner") {
        androidx.compose.foundation.layout.Column {
            com.pocketstudio.ui.NeonCard(androidx.compose.ui.Modifier.padding(androidx.compose.ui.unit.Dp(12f))) {
                com.pocketstudio.ui.SectionLabel("Analyseur de spectre")
                com.pocketstudio.app.analysis.SpectrumView(FloatArray(40) { i -> -8f - i * 1.2f - if (i % 7 == 3) 0f else 6f })
            }
            com.pocketstudio.app.analysis.TunerView(com.pocketstudio.app.analysis.TunerReading(111.2, 45, -9.0), micOk = true)
        }
    }

    @Test
    fun automation() = shoot("automation") {
        val lane = com.pocketstudio.model.AutomationLane(com.pocketstudio.model.AutoTarget(com.pocketstudio.model.AutoTarget.VOLUME, 2))
            .let { l -> (0 until 16).fold(l) { acc, s -> if (s % 2 == 0) acc.withPoint(s, 0.2f + 0.05f * s) else acc } }
        val send = com.pocketstudio.model.AutomationLane(com.pocketstudio.model.AutoTarget(com.pocketstudio.model.AutoTarget.SEND, 0, 0))
            .withPoint(0, 0f).withPoint(12, 0.9f)
        val filter = com.pocketstudio.model.AutomationLane(com.pocketstudio.model.AutoTarget(com.pocketstudio.model.AutoTarget.BUS_PARAM, 0, 0, 0),
            mode = com.pocketstudio.model.AutomationLane.SONG).withSongPoint(0, 0.1f).withSongPoint(8, 0.9f).withSongPoint(16, 0.5f).withSongPoint(23, 0.95f)
        androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.padding(androidx.compose.ui.unit.Dp(12f))) {
            com.pocketstudio.app.mixer.AutomationCard(
                com.pocketstudio.app.mixer.AutomationUi(listOf(lane, send, filter), 16, 5, recording = true, songBars = 24, songBar = 10),
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
                com.pocketstudio.app.ui.TopBarUi("Da Funk (remix)", 111.0, playing = true, canUndo = true, canRedo = false),
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
                ),
                currentId = "a",
                starters = listOf(com.pocketstudio.app.projects.ProjectStarter("Projet vide", "Tout est vierge.", "empty")),
                actions = com.pocketstudio.app.projects.ProjectsActions(),
                now = now,
            )
        }
    }

    @Test
    @Config(qualifiers = "w400dp-h1300dp-xxhdpi")
    fun settings() = shoot("settings") {
        com.pocketstudio.app.settings.SettingsScreen(
            com.pocketstudio.app.settings.AppSettings(uiScale = 1.15f, bufferBursts = 4, guideSeen = true),
            "0.5.70 (preview)",
            com.pocketstudio.app.settings.SettingsActions(),
        )
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
                connected = "midi-1", last = "Note Do 4", clockBpm = 124.0, sendClock = true,
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
            com.pocketstudio.app.acid.Panel303(com.pocketstudio.model.AcidSound(cutoff = 0.62f, waveform = 1f).values()) { _, _ -> }
        }
    }

    @Test
    fun session() = shoot("session") {
        val drums = com.pocketstudio.model.DrumPattern().toggle(0, 0).toggle(2, 4)
        val a = com.pocketstudio.model.Scene("A", drums)
        val b = com.pocketstudio.model.Scene("B", drums.copy(steps = 32))
        com.pocketstudio.app.session.SessionScreen(
            com.pocketstudio.app.session.SessionScreenUi(
                scenes = listOf(a, b, com.pocketstudio.model.Scene("C")) + List(6) { null } + com.pocketstudio.model.Scene("J", drums) + List(14) { null },
                current = 0, pending = 1,
                song = listOf(com.pocketstudio.model.SongPart(0, 4), com.pocketstudio.model.SongPart(1, 2), com.pocketstudio.model.SongPart(0, 2)),
                songIndex = 0, playing = true, tempoBpm = 124.0,
            ),
            com.pocketstudio.app.session.SessionActions(),
        )
    }

    @Test
    @Config(qualifiers = "w400dp-h2200dp-xxhdpi")
    fun timeline() = shoot("timeline") {
        val drums = com.pocketstudio.model.DrumPattern().toggle(0, 0).toggle(2, 4)
        val a = com.pocketstudio.model.Scene("A", drums)
        val b = com.pocketstudio.model.Scene("B", drums.copy(steps = 32))
        val p = withTracks()
        com.pocketstudio.app.session.SessionScreen(
            com.pocketstudio.app.session.SessionScreenUi(
                scenes = listOf(a, b) + List(22) { null },
                current = 1, pending = null,
                song = listOf(
                    com.pocketstudio.model.SongPart(0, 2, muted = setOf("acid", "inst:1", "inst:2", "inst:3", "audio:0")),
                    com.pocketstudio.model.SongPart(1, 2, muted = setOf("audio:0")),
                    com.pocketstudio.model.SongPart(0, 4),
                    com.pocketstudio.model.SongPart(1, 1, muted = setOf("drums", "acid")),
                ),
                songIndex = 1, playing = true, tempoBpm = 124.0, beatsPerBar = 4,
                rows = com.pocketstudio.app.ui.timelineRows(p),
            ),
            com.pocketstudio.app.session.SessionActions(),
        )
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
