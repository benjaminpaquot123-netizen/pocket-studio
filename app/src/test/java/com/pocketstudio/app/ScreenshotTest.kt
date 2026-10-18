package com.pocketstudio.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
        drums = drums.toggle(0, 10, 0.6f)
        RhythmScreen(
            project = Project(tempoBpm = 124.0, swing = 0.2, drums = drums),
            transport = TransportUi(playing = true, metronome = true, selectedPad = 0),
            live = LiveUi(currentStep = 5, litPads = setOf(5)),
            canUndo = true,
            canRedo = false,
            actions = RhythmActions(),
        )
    }
}
