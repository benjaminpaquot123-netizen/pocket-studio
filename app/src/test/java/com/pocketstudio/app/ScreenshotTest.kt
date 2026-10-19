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
            .generate(com.pocketstudio.model.AcidPattern(locked = setOf(2)))
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

    @Test
    fun mixer() = shoot("mixer") {
        val channels = com.pocketstudio.model.Project().mixer.channels.toMutableList()
        channels[1] = channels[1].copy(solo = true, pan = -0.4f)
        channels[2] = channels[2].copy(mute = true, volume = 0.5f)
        com.pocketstudio.app.mixer.MixerScreen(
            channels = channels,
            meters = floatArrayOf(0.62f, 0.81f, 0f, 0.88f, 0.8f),
            actions = com.pocketstudio.app.mixer.MixerActions(),
            export = com.pocketstudio.app.rhythm.ExportUi(bars = 4, file = java.io.File("Mon-premier-rythme-20261018-221500.wav"), savedInMusic = true),
        )
    }

    @Test
    fun mixerExporting() = shoot("mixer_export") {
        androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.padding(androidx.compose.ui.unit.Dp(12f))) {
            com.pocketstudio.app.mixer.ExportCard(
                com.pocketstudio.app.rhythm.ExportUi(bars = 8, running = true, progress = 0.4f),
                com.pocketstudio.app.mixer.MixerActions(),
            )
            com.pocketstudio.app.mixer.ExportCard(
                com.pocketstudio.app.rhythm.ExportUi(bars = 4, file = java.io.File("Mon-premier-rythme-20261018-221500.wav"), savedInMusic = true),
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
            ),
            startWithNotes = true,
        )
    }
}
