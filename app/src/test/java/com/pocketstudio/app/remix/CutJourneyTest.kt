package com.pocketstudio.app.remix

import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.guide.GuideListenPlayer
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
 * Lot C, n° 24, end to end on a synthetic song (never a real one in the repository): « Découper la
 * chanson » opens on the voice cut into its phrases, « Couper ici » at the play head, the new cut
 * moved by a finger onto the next beat (magnet), three pieces ticked and put on the pads in one
 * undo step, « Annuler », then the project saved and opened again with the same pieces.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h890dp-xxhdpi")
class CutJourneyTest {
    @get:Rule
    val compose = createComposeRule()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

    @After
    fun tearDown() = scope.cancel()

    /** 12 s at 120 BPM: a voice singing three phrases (1–2 s, 3.5–5.5 s, 7–8.5 s), drums on every beat. */
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
                when {
                    stem == VOCALS_STEM && sung.any { (a, b) -> t in a..b } -> (0.5 * sin(2 * PI * 220 * t)).toFloat()
                    stem == 0 && (t * 2) % 1.0 < 0.03 -> 0.8f
                    else -> 0f
                }
            }
        }
    }

    /** The app as the router sees it: the project (kept as saved), the pads, the library. */
    private class Host(var p: Project, val song: Song) : StemBoardHost {
        val pads = ArrayList<List<PadCut>>()
        val kept = ArrayList<Pair<Pcm, String>>()
        val live = ArrayList<Int>()
        val tracks = ArrayList<Pair<Pcm, Int>>()
        override fun project() = p
        override suspend fun <T> withSong(setId: String, block: (StemSource) -> T): T? = if (setId == "song") block(song) else null
        override fun busy() = false
        override suspend fun putOnPads(cuts: List<PadCut>) {
            pads += cuts
            p = p.copy(drums = cuts.fold(p.drums) { d, c -> d.withSample(c.lane, com.pocketstudio.model.PadSample("${c.name}.wav", c.name)) })
        }
        override suspend fun putOnNewInstrument(pcm: Pcm, name: String): Int? = null
        override suspend fun putOnNewAudioTrack(pcm: Pcm, name: String, atStep: Int, bpm: Double): Int? {
            tracks += pcm to atStep
            return 0
        }
        override suspend fun putOnLivePad(pcm: Pcm, name: String, pad: Int, bpm: Double) {
            live += pad
            p = p.copy(live = p.live.with(pad, com.pocketstudio.model.LiveClip("$name.wav", name, 4)))
        }
        override suspend fun keepInLibrary(pcm: Pcm, name: String, kind: String, bpm: Double, setId: String): MySound {
            kept += pcm to name
            return MySound("id${kept.size}", name, kind, "id${kept.size}.wav", pcm.seconds, origin = "Boys")
        }
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

    @Test
    fun cutTheVoiceMoveACutSendThreePiecesUndoSaveAndReopen() {
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

        // Opened on the voice, cut into its three phrases (each on the beats, around the singing).
        assertEquals(VOCALS_STEM, m.stem)
        assertEquals(3, m.pieces.size)
        m.pieces.zip(song.sung).forEach { (p, s) ->
            assertEquals(0L, p.start % beat)
            assertTrue(p.start <= s.first * BOARD_RATE && p.end >= s.second * BOARD_RATE - 2_400)
        }
        assertEquals(m.cuts, host.p.songCuts)

        // A touch inside the second phrase puts the play head there; « Couper ici » cuts it on the beat.
        compose.onNodeWithTag("cut_wave").performTouchInput { click(Offset(v.x(Math.round(4.45 * BOARD_RATE)), centerY)) }
        compose.waitForIdle()
        assertEquals(4.45 * BOARD_RATE, m.cursor.toDouble(), v.framesPerPx * 2)
        compose.onNodeWithTag("cut_here").performClick()
        compose.waitForIdle()
        assertEquals(4, m.pieces.size)
        assertEquals(Math.round(4.5 * BOARD_RATE), m.pieces[1].end)

        // That cut moved by a finger: it sticks to the beat nearest to where the finger lifts (5 s).
        compose.onNodeWithTag("cut_wave").performTouchInput {
            down(Offset(v.x(Math.round(4.5 * BOARD_RATE)), height * 0.8f))
            moveTo(Offset(v.x(Math.round(4.7 * BOARD_RATE)), height * 0.8f))
            moveTo(Offset(v.x(Math.round(4.9 * BOARD_RATE)), height * 0.8f))
            up()
        }
        compose.waitForIdle()
        assertEquals(5L * BOARD_RATE, m.pieces[1].end)
        assertEquals(5L * BOARD_RATE, m.pieces[2].start)

        // Three pieces ticked, then on the pads: one undo step, free pads only (never the kick's).
        listOf(0, 1, 2).forEach { compose.onNodeWithTag("cut_piece_$it").performClick() }
        compose.waitForIdle()
        assertEquals(listOf(0, 1, 2), m.chosen)
        compose.onNodeWithTag("cut_to_pads").performClick()
        compose.waitForIdle()
        assertEquals(1, host.pads.size)
        val cuts = host.pads.single()
        assertEquals(listOf("Voix 1", "Voix 2", "Voix 3"), cuts.map { it.name })
        assertTrue(cuts.none { it.lane == 0 })
        assertEquals(m.pieces.take(3).map { it.frames }, cuts.map { it.pcm.frames.toLong() })

        // « Annuler »: the moved cut goes back to 4.5 s.
        compose.onNodeWithTag("cut_undo").performClick()
        compose.waitForIdle()
        assertEquals(Math.round(4.5 * BOARD_RATE), m.pieces[1].end)
        val before = m.pieces
        val ticked = m.chosen

        // Saved with the project, then opened again: the same pieces, the same ticks.
        host.p = ProjectJson.decode(ProjectJson.encode(host.p))
        opened++
        compose.waitForIdle()
        val again = model!!
        assertTrue(again !== m)
        assertEquals(before, again.pieces)
        assertEquals(ticked, again.chosen)
    }

    @Test
    fun severalPiecesGoToTheLiveATrackAndTheLibraryInOneGo() {
        val song = Song()
        val host = Host(project(), song)
        val router = StemBoardRouter(scope, host, StemEditor, Silent)
        val sels = listOf(
            StemSelection("song", VOCALS_STEM, 48_000, 96_000),
            StemSelection("song", VOCALS_STEM, 168_000, 264_000),
        )
        router.sendPieces(sels, StemDestination.Live)
        assertEquals(2, host.live.size)
        assertEquals(2, router.state.value.message!!.lines().size)
        router.dismissMessage()

        router.sendPieces(sels, StemDestination.Track())
        val (pcm, step) = host.tracks.single()
        // One clip from the first piece's start to the last's end, each at its place, silence between.
        assertEquals(264_000 - 48_000, pcm.frames)
        assertEquals(8, step)  // 1 s at 120 BPM: the ninth sixteenth
        assertTrue(pcm.data.copyOfRange(2 * 60_000, 2 * 110_000).all { it == 0f })
        router.dismissMessage()

        router.sendPieces(sels, StemDestination.Mine)
        assertEquals(listOf("Voix · Boys", "Voix · Boys"), host.kept.map { it.second })
        assertTrue(router.state.value.message!!, router.state.value.message!!.contains("Boys"))
    }
}
