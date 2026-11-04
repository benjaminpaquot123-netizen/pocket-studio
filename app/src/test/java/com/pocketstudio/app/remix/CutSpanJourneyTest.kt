package com.pocketstudio.app.remix

import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performCustomAccessibilityActionWithLabel
import androidx.compose.ui.test.performTouchInput
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.guide.GuideListenPlayer
import com.pocketstudio.model.AutoCut
import com.pocketstudio.model.DrumPattern
import com.pocketstudio.model.MySound
import com.pocketstudio.model.Project
import com.pocketstudio.model.ProjectJson
import com.pocketstudio.model.Remix
import com.pocketstudio.model.Scene
import com.pocketstudio.model.SongCuts
import com.pocketstudio.ui.PocketStudioTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.math.PI
import kotlin.math.sin

/**
 * « Un bout » end to end (Benjamin, 2026-11-04): « Découper la chanson » opens on the voice with two
 * bars around its first phrase, nothing else; « Fin » dragged by a finger onto a beat, « Début »
 * moved by TalkBack, the one piece put on the pads at once (nothing to tick), « Annuler », then the
 * project saved and opened again with its bars where they were.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class CutSpanJourneyTest {
    @get:Rule
    val compose = createComposeRule()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

    @After
    fun tearDown() = scope.cancel()

    /** 12 s at 120 BPM: a voice singing three phrases (1–2 s, 3.5–5.5 s, 7–8.5 s). */
    private class Song : StemSource {
        override val frames = 12L * BOARD_RATE
        override val songBpm = 120.0
        override val firstBeatFrame = 0L
        override val fittedBpm = 120.0
        val sung = listOf(1.0 to 2.0, 3.5 to 5.5, 7.0 to 8.5)
        override fun read(stem: Int, start: Long, count: Long, fitted: Boolean): FloatArray {
            val n = maxOf(0L, minOf(count, frames - start)).toInt()
            return FloatArray(n * 2) { i ->
                val t = (start + i / 2) / BOARD_RATE.toDouble()
                if (stem == VOCALS_STEM && sung.any { (a, b) -> t in a..b }) (0.5 * sin(2 * PI * 220 * t)).toFloat() else 0f
            }
        }
    }

    private class Host(var p: Project, val song: Song) : StemBoardHost {
        val pads = ArrayList<List<PadCut>>()
        override fun project() = p
        override suspend fun <T> withSong(setId: String, block: (StemSource) -> T): T? = if (setId == "song") block(song) else null
        override fun busy() = false
        override suspend fun putOnPads(cuts: List<PadCut>) {
            pads += cuts
            p = p.copy(drums = cuts.fold(p.drums) { d, c -> d.withSample(c.lane, com.pocketstudio.model.PadSample("${c.name}.wav", c.name)) })
        }
        override suspend fun putOnNewInstrument(pcm: Pcm, name: String): Int? = null
        override suspend fun putOnNewAudioTrack(pcm: Pcm, name: String, atStep: Int, bpm: Double): Int? = null
        override suspend fun putOnLivePad(pcm: Pcm, name: String, pad: Int, bpm: Double) = Unit
        override suspend fun keepInLibrary(pcm: Pcm, name: String, kind: String, bpm: Double, setId: String): MySound =
            MySound("id", name, kind, "id.wav", pcm.seconds)
        override suspend fun putInRemixCell(pcm: Pcm, name: String, scene: Int, bpm: Double, stem: Int, fromSeconds: Double) = Unit
        override fun songStepNow() = -1
        override fun pauseMusic() = Unit
        override fun saveCuts(cuts: SongCuts) {
            p = p.copy(songCuts = cuts)
        }
    }

    private object Silent : GuideListenPlayer {
        override fun play(pcm: Pcm, ended: () -> Unit) = true
        override fun stop() = Unit
    }

    private fun project() = Project(
        tempoBpm = 120.0, drums = DrumPattern(steps = 16).toggle(0, 0),
        scenes = listOf(Scene("A", DrumPattern(steps = 16).toggle(0, 0))), scene = 0,
        remix = Remix("song", "Boys", songBpm = 120.0),
    ).sanitized()

    private fun ui(song: Song) = StemBoardUi(
        "song", "Boys", song.frames, 120.0, 0.0,
        StemPeakSource { stem -> StemPeaks.of(song.frames, 2) { s, c -> song.read(stem, s, c.toLong(), false) } },
        peaksContext = EmptyCoroutineContext,
    )

    private val beat = 24_000L

    @OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)
    @Test
    fun opensOnTwoBarsAroundTheFirstPhraseAndSendsThatPieceOntoThePads() {
        val song = Song()
        val host = Host(project(), song)
        val router = StemBoardRouter(scope, host, StemEditor, Silent)
        var model: CutModel? = null
        var view: StemBoardView? = null
        var opened by mutableIntStateOf(0)
        compose.setContent {
            PocketStudioTheme {
                key(opened) {
                    val m = rememberCutModel(ui(song), router)
                    val v = rememberStemBoardView(ui(song))
                    model = m
                    view = v
                    CutScreen(ui(song), router, onClose = {}, model = m, view = v)
                }
            }
        }
        compose.waitForIdle()
        val m = model!!
        val v = view!!

        // « Un bout »: two bars around the first phrase (on the beats), brought into view; no numbered pieces.
        assertEquals(AutoCut.Selection, m.mode)
        val first = m.span!!
        assertEquals(0L, first.start % beat)
        assertTrue("$first", first.start <= 1L * BOARD_RATE && first.end >= 2L * BOARD_RATE - 2_400 && first.end <= 3L * BOARD_RATE)
        assertTrue(v.zoomedIn)
        compose.onNodeWithTag("cut_auto").assertTextEquals("Un bout", includeEditableText = false)
        compose.onAllNodesWithTag("cut_piece_0").assertCountEquals(0)
        compose.onAllNodesWithTag("cut_here").assertCountEquals(0)
        compose.onNodeWithTag("cut_chosen").assertTextEquals("Place Début et Fin, puis envoie ce bout.")
        compose.onNodeWithTag("cut_to_pads").assertIsEnabled()
        compose.onNodeWithContentDescription("Début du bout").assertExists()
        compose.onNodeWithContentDescription("Fin du bout").assertExists()
        assertTrue(m.cuts.of(VOCALS_STEM)!!.span == host.p.songCuts.of(VOCALS_STEM)!!.span)

        // « Fin » dragged by a finger on its handle: it sticks to the beat where the finger lifts (2.5 s).
        val end = first.end
        compose.onNodeWithTag("cut_wave").performTouchInput {
            down(Offset(v.x(end) - 6f, centerY))
            moveTo(Offset(v.x(Math.round(2.9 * BOARD_RATE)), centerY))
            moveTo(Offset(v.x(Math.round(2.55 * BOARD_RATE)), centerY))
            up()
        }
        compose.waitForIdle()
        assertEquals(first.start, m.span!!.start)
        assertEquals(Math.round(2.5 * BOARD_RATE), m.span!!.end)

        // « Début » one beat earlier, by TalkBack.
        compose.onNodeWithTag("cut_span_start").performCustomAccessibilityActionWithLabel("Reculer d'un temps")
        compose.waitForIdle()
        assertEquals(first.start - beat, m.span!!.start)
        assertEquals(2, m.undoCount)
        compose.onNodeWithTag("cut_span_length", useUnmergedTree = true).assertTextEquals(spanLength(m, m.span!!.frames))

        // On the pads at once, nothing to tick: one pad, the piece's length.
        val sent = m.span!!
        compose.onNodeWithTag("cut_to_pads").performClick()
        compose.waitForIdle()
        val pad = host.pads.single().single()
        assertEquals(sent.frames, pad.pcm.frames.toLong())
        assertTrue(pad.lane != 0)

        // « Annuler »: « Début » back where it was.
        compose.onNodeWithTag("cut_undo").performClick()
        compose.waitForIdle()
        assertEquals(first.start, m.span!!.start)
        val kept = m.span

        // Saved with the project, then opened again: the same bars, « Un bout » again, the phrases still there.
        host.p = ProjectJson.decode(ProjectJson.encode(host.p))
        opened++
        compose.waitForIdle()
        val again = model!!
        assertTrue(again !== m)
        assertEquals(kept, again.span)
        assertTrue(again.single)
        assertEquals(3, again.pieces.size)
    }
}
