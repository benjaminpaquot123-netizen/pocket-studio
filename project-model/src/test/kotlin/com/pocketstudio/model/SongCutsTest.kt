package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.exp

/** Lot C, n° 24: the cut screen's pure rules (« Couper ici », Début, Fin, Enlever, Boucle, the magnet, the auto cuts). */
class SongCutsTest {
    private val rate = 48_000
    private val frames = 10L * rate
    private val beat = Cuts.beatFrames(120.0, rate)  // 24 000 frames

    @Test
    fun cutHereSplitsThePieceUnderThePlayHeadAndOnlyThere() {
        val whole = Cuts.whole(frames)
        val two = Cuts.split(whole, 100_000)
        assertEquals(listOf(CutPiece(0, 100_000), CutPiece(100_000, frames)), two)
        // Too near an edge, in a gap, or outside: nothing changes.
        assertEquals(two, Cuts.split(two, 100_000 + Cuts.MIN_FRAMES - 1))
        assertEquals(two, Cuts.split(two, frames + 5))
        val gapped = Cuts.remove(two, listOf(0))
        assertEquals(gapped, Cuts.split(gapped, 50_000))
    }

    @Test
    fun startEndAndRemoveTrimWithoutTouchingTheNeighbours() {
        val p = Cuts.split(Cuts.split(Cuts.whole(frames), 100_000), 200_000)
        val started = Cuts.setStart(p, 1, 150_000)
        assertEquals(CutPiece(150_000, 200_000), started[1])
        assertEquals(p[0], started[0])
        // Never before the piece on its left, never shorter than a hit.
        assertEquals(100_000L, Cuts.setStart(p, 1, 10)[1].start)
        assertEquals(200_000L - Cuts.MIN_FRAMES, Cuts.setStart(p, 1, 199_999)[1].start)
        val ended = Cuts.setEnd(p, 1, 180_000, frames)
        assertEquals(CutPiece(100_000, 180_000), ended[1])
        assertEquals(200_000L, Cuts.setEnd(p, 1, 999_999, frames)[1].end)
        val removed = Cuts.remove(p, listOf(1))
        assertEquals(listOf(p[0], p[2]), removed)
    }

    @Test
    fun aSharedCutMovesBothEdgesAndStopsBeforeEitherPieceVanishes() {
        val p = Cuts.split(Cuts.whole(frames), 100_000)
        val lines = Cuts.lines(p)
        assertEquals(listOf(Cuts.Line(0, -1, 0), Cuts.Line(100_000, 0, 1), Cuts.Line(frames, 1, -1)), lines)
        val moved = Cuts.moveLine(p, lines[1], 130_000, frames)
        assertEquals(130_000L, moved[0].end)
        assertEquals(130_000L, moved[1].start)
        assertEquals(130_000L, Cuts.movedLine(moved, lines[1]).frame)
        val far = Cuts.moveLine(p, lines[1], frames * 2, frames)
        assertEquals(frames - Cuts.MIN_FRAMES, far[0].end)
        assertEquals(Cuts.Line(100_000, 0, 1), Cuts.lineNear(p, 100_500, 1_000))
        assertEquals(null, Cuts.lineNear(p, 150_000, 1_000))
    }

    @Test
    fun theMagnetSticksToBeatsOrBarsAndLetsGoWhenFree() {
        assertEquals(24_000L, Cuts.magnet(30_000, CutMagnet.Beat, beat, 0))
        assertEquals(96_000L, Cuts.magnet(80_000, CutMagnet.Bar, beat, 0))
        assertEquals(30_000L, Cuts.magnet(30_000, CutMagnet.Free, beat, 0))
        // The beats start at the song's first beat; no tempo, no magnet.
        assertEquals(29_000L, Cuts.magnet(30_000, CutMagnet.Beat, beat, 5_000))
        assertEquals(30_000L, Cuts.magnet(30_000, CutMagnet.Beat, 0.0, 0))
    }

    @Test
    fun barsCutTheWindowOnTheGrid() {
        val bars = Cuts.bars(0, frames, beat, 0)
        assertEquals(5, bars.size)
        assertEquals(0L until 96_000L, bars[0])
        assertTrue(bars.zipWithNext().all { (a, b) -> a.last + 1 == b.first })
        // A window in the middle: only its bars, clipped to it.
        val mid = Cuts.bars(100_000, 300_000, beat, 0)
        assertEquals(100_000L, mid.first().first)
        assertEquals(300_000L, mid.last().last + 1)
        assertTrue(Cuts.bars(0, frames, 0.0, 0).isEmpty())
    }

    @Test
    fun attacksFindEveryHitOfASyntheticBeat() {
        // Four hits a second (each decaying), as root mean square every 10 ms.
        val win = 480
        val env = FloatArray((frames / win).toInt()) { w ->
            val t = w * win / rate.toDouble()
            val since = t - Math.floor(t * 4) / 4
            (0.6 * exp(-since * 30) + 0.001).toFloat()
        }
        val found = Cuts.attacks(env, win, 0, rate)
        assertEquals(40, found.size)
        found.forEachIndexed { i, r ->
            val expected = i * rate / 4L
            assertTrue("hit $i at ${r.first}", abs(r.first - expected) <= 2 * win)
        }
        // A silent stem has no attacks.
        assertTrue(Cuts.attacks(FloatArray(500), win, 0, rate).isEmpty())
    }

    @Test
    fun autoCutsReplaceOnlyTheWindow() {
        val p = Cuts.split(Cuts.split(Cuts.whole(frames), 100_000), 300_000)
        val out = Cuts.replaceIn(p, 150_000, 250_000, listOf(160_000L until 200_000L, 200_000L until 400_000L))
        assertEquals(
            listOf(CutPiece(0, 100_000), CutPiece(100_000, 150_000), CutPiece(160_000, 200_000), CutPiece(200_000, 250_000), CutPiece(250_000, 300_000), CutPiece(300_000, frames)),
            out,
        )
    }

    @Test
    fun loopMakesAWholeNumberOfBeatsOnTheBeatAndUndoesIt() {
        val p = listOf(CutPiece(10_000, 10_000 + 4 * 24_000 + 3_000))
        val looped = Cuts.loopPiece(p, 0, beat, 0, frames)[0]
        assertTrue(looped.loop)
        assertEquals(0L, looped.start % 24_000)
        assertEquals(4 * 24_000L, looped.frames)
        assertFalse(Cuts.loopPiece(listOf(looped), 0, beat, 0, frames)[0].loop)
        // Never over its neighbour.
        val two = listOf(CutPiece(0, 30_000), CutPiece(30_000, 60_000))
        val l = Cuts.loopPiece(two, 1, beat, 0, frames)[1]
        assertTrue(l.start >= 30_000)
    }

    @Test
    fun savedCutsComeBackSaneAndOnlyForTheirSong() {
        val cuts = SongCuts("song", listOf(StemCuts(3, listOf(CutPiece(0, 100_000), CutPiece(50_000, 200_000), CutPiece(300_000, 300_010)), chosen = listOf(0, 1, 7))))
        val p = Project().copy(songCuts = cuts)
        val back = ProjectJson.decode(ProjectJson.encode(p))
        assertEquals(cuts, back.songCuts)
        val sane = back.songCuts.sanitized("song", frames).of(3)!!
        assertEquals(listOf(CutPiece(0, 100_000), CutPiece(100_000, 200_000)), sane.pieces)
        assertEquals(listOf(0, 1), sane.chosen)
        assertTrue(back.songCuts.sanitized("other", frames).stems.isEmpty())
        // A project without cuts writes nothing for them.
        assertFalse(ProjectJson.encode(Project()).contains("songCuts"))
    }
}
