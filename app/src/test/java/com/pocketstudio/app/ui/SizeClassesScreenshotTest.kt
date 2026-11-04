package com.pocketstudio.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.pocketstudio.app.live.LiveActions
import com.pocketstudio.app.live.LiveScreen
import com.pocketstudio.app.live.LiveScreenUi
import com.pocketstudio.app.padlive.PadLiveActions
import com.pocketstudio.app.padlive.PadLiveScreen
import com.pocketstudio.app.padlive.PadLiveUi
import com.pocketstudio.app.remix.BOARD_RATE
import com.pocketstudio.app.remix.CutModel
import com.pocketstudio.app.remix.CutScreen
import com.pocketstudio.app.remix.CutWithStructure
import com.pocketstudio.app.remix.NoStemBoardActions
import com.pocketstudio.app.remix.StemBoardActions
import com.pocketstudio.app.remix.StemBoardState
import com.pocketstudio.app.remix.StemBoardUi
import com.pocketstudio.app.remix.StemPeakSource
import com.pocketstudio.app.remix.StemPeaks
import com.pocketstudio.app.remix.rememberStemBoardView
import com.pocketstudio.app.session.LivePadsActions
import com.pocketstudio.app.session.LivePadsUi
import com.pocketstudio.app.session.SessionActions
import com.pocketstudio.app.session.SessionScreen
import com.pocketstudio.app.session.SessionScreenUi
import com.pocketstudio.model.AcidPattern
import com.pocketstudio.model.AcidStep
import com.pocketstudio.model.AudioClip
import com.pocketstudio.model.CutPiece
import com.pocketstudio.model.DrumPattern
import com.pocketstudio.model.LiveClip
import com.pocketstudio.model.LivePads
import com.pocketstudio.model.LiveSlots
import com.pocketstudio.model.LiveStarter
import com.pocketstudio.model.LiveState
import com.pocketstudio.model.Note
import com.pocketstudio.model.NoteClip
import com.pocketstudio.model.PadLive
import com.pocketstudio.model.PadMode
import com.pocketstudio.model.Project
import com.pocketstudio.model.addAudioTrack
import com.pocketstudio.model.addTrack
import com.pocketstudio.model.withAudioTrack
import com.pocketstudio.model.Remix
import com.pocketstudio.model.Scene
import com.pocketstudio.model.SongCuts
import com.pocketstudio.model.SongPart
import com.pocketstudio.model.StemCuts
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.PocketStudioTheme
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * Lot D: the screens lot D changed in Material's five window size classes (compact, medium,
 * expanded, large, extra-large; images in app/screenshots/sizes/): Pad Live (lying down: pads
 * on the left, controls on the right), the Live page (expanded and up: the bricks and the pads
 * side by side), « Découper la chanson » (expanded and up: the Structure beside it).
 */
abstract class SizeClassShots {
    @get:Rule
    val compose = createComposeRule()

    /** The size class's name in the images' names. */
    protected abstract val size: String

    private fun shoot(name: String, content: @Composable () -> Unit) {
        compose.setContent {
            PocketStudioTheme { Box(Modifier.fillMaxSize().background(Neon.Background)) { content() } }
        }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("screenshots/sizes/${size}_$name.png")
    }

    // --- Pad Live -----------------------------------------------------------------------------

    private val padLivePads: LivePads = PadLive.placed(
        LivePads(), 0,
        LiveStarter.PADS.mapIndexed { i, p -> LiveClip("s$i.wav", p.name, if (p.oneShot) 1 else p.beats, p.oneShot, p.gain) } +
            listOf(
                PadLive.withMode(LiveClip("m.wav", "Montée", 1, oneShot = true), PadMode.HOLD),
                LiveClip("i.wav", "Impact", 1, oneShot = true),
                LiveClip("c.wav", "Chute", 1, oneShot = true, group = 1),
                LiveClip("w.wav", "Sifflet", 1, oneShot = true, group = 1),
            ),
    )

    @Test
    fun padLive() = shoot("padlive") {
        val states = List(LivePads.ALL) { if (it == 0 || it == 2) 2 else if (it == 3) 1 else 0 }
        PadLiveScreen(
            PadLiveUi(padLivePads, states, page = 0, pack = "fete", playing = true, tempoBpm = 124.0, beatsPerBar = 4, canUndo = true),
            PadLiveActions(), clockOf = { 6 }, record = { com.pocketstudio.app.live.RecordButton(com.pocketstudio.app.live.TakeUi.Ready) {} }, still = true,
        )
    }

    // --- The Live page ------------------------------------------------------------------------

    /** The Live page's example of ScreenshotTest: four named scenes, drums, acid, a bass track, a voice and a remix. */
    private fun liveUi(): LiveScreenUi {
        val drums = DrumPattern().toggle(0, 0).toggle(0, 8).toggle(2, 4).toggle(2, 12)
        val acid = AcidPattern(steps = List(AcidPattern.MAX_STEPS) { i -> AcidStep(gate = i % 3 == 0) })
        val notes = NoteClip(16, listOf(Note(0, 48, 60), Note(96, 48, 63)))
        var p = Project(
            scenes = listOf(
                Scene("A", drums = drums, label = "Intro"),
                Scene("B", drums = drums, acid = acid, label = "Couplet"),
                Scene("C", drums = drums, acid = acid, synthNotes = notes, label = "Refrain"),
                Scene("D", acid = acid, synthNotes = notes, label = "Drop"),
            ),
            scene = 1, drums = drums, acid = acid,
            remix = Remix(setId = "x", name = "Chanson"),
        ).addTrack("Basse")!!.addAudioTrack("Voix")!!
        val bass = p.tracks.first().id
        p = p.withAudioTrack(p.audioTracks.first().copy(clip = AudioClip("v.wav", "Voix", scene = 2)))
            .copy(scenes = p.scenes.mapIndexed { i, sc -> if (i >= 2) sc.copy(trackNotes = mapOf(bass to notes)) else sc })
            .sanitized()
        var g = LiveState.on(1, 32)
        g = g.launched(LiveSlots.DRUMS, 2, 32).launched(LiveSlots.INST + bass, -1, 32).armed(LiveSlots.mask(LiveSlots.INST), 3)
        return LiveScreenUi(p, g, clock = 45, playing = true, muted = setOf(SongPart.audio(0)), acid = mapOf(0 to 0.62f, 1 to 0.8f, 7 to 0.55f))
    }

    private val livePads = run {
        val clip = { n: String, b: Int -> LiveClip("x.wav", n, b) }
        LivePads(12).with(0, clip("Kick 909", 1)).with(1, clip("Charley", 2)).with(2, clip("Basse Da Funk", 8))
            .with(3, clip("Voix refrain", 16)).with(5, clip("Clap", 4).copy(oneShot = true)).with(6, clip("Nappe", 16))
    }

    @Test
    fun live() = shoot("live") {
        LiveScreen(
            liveUi(), LiveActions(),
            pads = LivePadsUi(livePads, listOf(2, 2, 1) + List(13) { 0 }, editing = null),
            padsActions = LivePadsActions(fullScreen = {}),
        )
    }

    // --- « Découper la chanson » and the Structure ----------------------------------------------

    private val frames = 145L * BOARD_RATE
    private val bpm = 113.0
    private val bar = 4 * 60.0 / bpm * BOARD_RATE

    private fun level(stem: Int, f: Long): Float {
        val b = f / bar
        return when (stem) {
            3 -> if (b < 4 || (b % 3) >= 2.2) 0.01f else (0.3 + 0.6 * abs(sin(b * PI * 3.1))).toFloat()
            0 -> (0.3 + 0.7 * (1 - ((b * 4) % 1.0)).let { it * it * it }).toFloat()
            else -> (0.3 + 0.3 * abs(sin(b * PI / 4))).toFloat()
        }
    }

    private val peaks = StemPeakSource { stem ->
        StemPeaks.of(frames, 1) { start, count -> FloatArray(count) { k -> val f = start + k; if (f % 480 == 0L) level(stem, f) else 0f } }
    }

    private val cuts = run {
        fun at(b: Double) = Math.round(b * bar)
        val pieces = listOf(4.0 to 6.2, 7.0 to 9.2, 12.4 to 13.6, 13.6 to 14.7, 14.7 to 15.7, 15.7 to 17.0).map { (a, z) -> CutPiece(at(a), at(z)) }
        SongCuts("boys", listOf(StemCuts(3, pieces, chosen = listOf(2, 3), auto = "Phrases")))
    }

    private class Shown(private val cuts: SongCuts) : StemBoardActions by NoStemBoardActions {
        override val state = MutableStateFlow(StemBoardState())
        override fun savedCuts(setId: String, frames: Long) = cuts
    }

    @Composable
    private fun Structure() {
        val drums = DrumPattern().toggle(0, 0).toggle(2, 4)
        SessionScreen(
            SessionScreenUi(
                scenes = listOf(Scene("A", drums, label = "Intro"), Scene("B", drums, label = "Couplet"), Scene("C", drums, label = "Refrain")) + List(Scene.MAX - 3) { null },
                current = 0, pending = null,
                song = listOf(SongPart(0, 2), SongPart(1, 2), SongPart(2, 2), SongPart(1), SongPart(2, 4)),
                songIndex = 0, playing = false, tempoBpm = 113.0, upcoming = -1,
                songStep = { -1 },
            ),
            SessionActions(),
            arrangeOnly = true,
        )
    }

    @Test
    fun cutAndStructure() = shoot("cut_structure") {
        val ui = StemBoardUi("boys", "ADÉLA - Boys", frames, bpm, 0.0, peaks, peaksContext = EmptyCoroutineContext)
        val actions = remember { Shown(cuts) }
        val model = remember { CutModel("boys", frames, bpm, 0.0, cuts, mode = com.pocketstudio.model.AutoCut.Phrases) }
        val view = rememberStemBoardView(ui)
        CutWithStructure(structure = { Structure() }) { CutScreen(ui, actions, onClose = {}, model = model, view = view) }
        LaunchedEffect(view.width) {
            if (view.width > 0f && !view.zoomedIn) {
                view.zoomBy((frames / (7.2 * bar)).toFloat(), 0f)
                view.scrollBy(-view.x(Math.round(3.6 * bar)))
            }
        }
    }
}

/** Compact: a phone upright (360 dp wide). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h780dp-xxhdpi")
class CompactSizeScreenshotTest : SizeClassShots() { override val size = "compact" }

/** Medium: a foldable opened, a small tablet upright (700 dp). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w700dp-h1000dp-port-hdpi")
class MediumSizeScreenshotTest : SizeClassShots() { override val size = "medium" }

/** Expanded: a tablet lying down, a foldable lying down (1000 dp). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w1000dp-h640dp-land-hdpi")
class ExpandedSizeScreenshotTest : SizeClassShots() { override val size = "expanded" }

/** Large: an 11-inch tablet lying down (1280 dp). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w1280dp-h800dp-land-mdpi")
class LargeSizeScreenshotTest : SizeClassShots() { override val size = "large" }

/** Extra-large: a big tablet or a computer's window (1700 dp). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w1700dp-h1000dp-land-mdpi")
class ExtraLargeSizeScreenshotTest : SizeClassShots() { override val size = "xlarge" }
