package com.pocketstudio.app.remix

import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.model.AutoCut
import com.pocketstudio.model.CutMagnet
import com.pocketstudio.model.CutPiece
import com.pocketstudio.model.Project
import com.pocketstudio.model.Remix
import com.pocketstudio.model.SongCuts
import com.pocketstudio.model.StemCuts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

/** Lot C, n° 24: what « Découper la chanson » does to the pieces, its undo, what it saves; the auto cuts on a synthetic stem. */
class CutModelTest {
    private val frames = 30L * BOARD_RATE
    private val beat = 24_000L  // 120 BPM

    private fun model(saved: SongCuts = SongCuts("song"), into: MutableList<SongCuts> = mutableListOf()) =
        CutModel("song", frames, 120.0, 0.0, saved, onChange = { into += it })

    @Test
    fun aStemNeverCutIsOnePieceAndCutHereSnapsToTheBeat() {
        val saved = mutableListOf<SongCuts>()
        val m = model(into = saved)
        assertFalse(m.cutYet)
        assertEquals(listOf(CutPiece(0, frames)), m.pieces)
        assertEquals(CutMagnet.Beat, m.magnet)
        m.cursor = 100_000
        assertTrue(m.cutHere())
        assertEquals(listOf(CutPiece(0, 96_000), CutPiece(96_000, frames)), m.pieces)
        assertTrue(m.cutYet)
        assertEquals(m.cuts, saved.last())
        // Free magnet: exactly where asked.
        m.magnet = CutMagnet.Free
        m.cursor = 200_001
        m.cutHere()
        assertEquals(200_001L, m.pieces[1].end)
    }

    @Test
    fun startEndRemoveAndLoopActAtThePlayHeadAndUndoGoesBackOneStepAtATime() {
        val m = model()
        m.cursor = 4 * beat; m.cutHere()
        m.cursor = 12 * beat; m.cutHere()
        assertEquals(3, m.pieces.size)
        m.cursor = 5 * beat + 100
        assertTrue(m.setStart())
        assertEquals(5 * beat, m.pieces[1].start)
        m.cursor = 10 * beat
        assertTrue(m.setEnd())
        assertEquals(10 * beat, m.pieces[1].end)
        m.cursor = 6 * beat
        val looped = m.loop()
        assertEquals(1, looped)
        assertTrue(m.pieces[1].loop)
        assertEquals(4 * beat, m.pieces[1].frames)  // 5 beats → the nearest whole loop (4)
        assertTrue(m.remove())
        assertEquals(2, m.pieces.size)
        // Undo: loop back, then end, start, the two cuts, then nothing more.
        m.undo(); assertEquals(3, m.pieces.size); assertTrue(m.pieces[1].loop)
        m.undo(); assertFalse(m.pieces[1].loop)
        m.undo(); assertEquals(12 * beat, m.pieces[1].end)
        m.undo(); assertEquals(4 * beat, m.pieces[1].start)
        m.undo(); m.undo()
        assertEquals(listOf(CutPiece(0, frames)), m.pieces)
        assertFalse(m.canUndo)
    }

    @Test
    fun aMovedCutFollowsTheFingerOnTheBeatsInOneUndoStep() {
        val saved = mutableListOf<SongCuts>()
        val m = model(into = saved)
        m.cursor = 8 * beat; m.cutHere()
        val savedBefore = saved.size
        assertTrue(m.beginMove(8 * beat + 500, slop = 2_000))
        m.moveTo(9 * beat + 3_000)
        assertEquals(9 * beat, m.movedTo)
        m.moveTo(10 * beat - 2_000)
        assertEquals(10 * beat, m.pieces[0].end)
        assertEquals(10 * beat, m.pieces[1].start)
        assertEquals(savedBefore, saved.size)  // saved once, when the finger lifts
        m.endMove()
        assertNull(m.movedTo)
        assertEquals(savedBefore + 1, saved.size)
        m.undo()
        assertEquals(8 * beat, m.pieces[0].end)
        // Nothing near: no move.
        assertFalse(m.beginMove(20 * beat, slop = 2_000))
    }

    @Test
    fun ticksFollowThePiecesAndGiveTheSelectionsToSend() {
        val m = model()
        m.cursor = 4 * beat; m.cutHere()
        m.cursor = 8 * beat; m.cutHere()
        m.toggleChosen(0)
        m.toggleChosen(2)
        assertEquals(listOf(0, 2), m.chosen)
        // A cut in the first piece: both halves stay ticked, the last one is now the fourth.
        m.cursor = 2 * beat; m.cutHere()
        assertEquals(listOf(0, 1, 3), m.chosen)
        val sels = m.chosenSelections()
        assertEquals(listOf(0L, 2 * beat, 8 * beat), sels.map { it.startFrame })
        assertTrue(sels.all { it.setId == "song" && it.stem == VOCALS_STEM })
        assertEquals(4 * beat + (frames - 8 * beat), m.chosenFrames)
        m.chooseAll(); assertEquals(listOf(0, 1, 2, 3), m.chosen)
        m.chooseAll(); assertTrue(m.chosen.isEmpty())
    }

    @Test
    fun theCutsComeBackFromTheProjectForTheirSongOnly() {
        val cuts = SongCuts("song", listOf(StemCuts(3, listOf(CutPiece(0, 48_000), CutPiece(48_000, 96_000)), chosen = listOf(1))))
        val m = model(saved = cuts)
        assertEquals(2, m.pieces.size)
        assertEquals(listOf(1), m.chosen)
        val other = CutModel("other", frames, 120.0, 0.0, cuts)
        assertFalse(other.cutYet)
    }

    /** A voice of three sung phrases (1 s, 2 s, 1.5 s) with silences between, as the stem board reads a song. */
    private class Voice : StemSource {
        override val frames = 12L * BOARD_RATE
        override val songBpm = 120.0
        override val firstBeatFrame = 0L
        override val fittedBpm = 120.0
        val sung = listOf(1.0 to 2.0, 3.5 to 5.5, 7.0 to 8.5)
        override fun read(stem: Int, start: Long, count: Long, fitted: Boolean): FloatArray {
            val n = maxOf(0L, minOf(count, frames - start)).toInt()
            return FloatArray(n * 2) { i ->
                val t = (start + i / 2) / BOARD_RATE.toDouble()
                val on = sung.any { (a, b) -> t in a..b }
                if (stem == VOCALS_STEM && on) (0.5 * sin(2 * PI * 220 * t)).toFloat()
                else if (stem == 0 && (t * 2) % 1.0 < 0.03) 0.8f else 0f
            }
        }
    }

    @Test
    fun theAutoCutsFindThePhrasesTheHitsAndTheBars() {
        val src = Voice()
        val phrases = SongCutFinder.find(src, StemEditor, VOCALS_STEM, AutoCut.Phrases, 0, src.frames)
        assertEquals(3, phrases.size)
        phrases.zip(src.sung).forEach { (r, s) ->
            // On the beats (half a second), around the sung phrase.
            assertTrue("$r around $s", r.first <= s.first * BOARD_RATE + 2_400 && r.last + 1 >= s.second * BOARD_RATE - 2_400)
        }
        // Only the window asked for.
        val inWindow = SongCutFinder.find(src, StemEditor, VOCALS_STEM, AutoCut.Phrases, 3L * BOARD_RATE, 6L * BOARD_RATE)
        assertEquals(1, inWindow.size)
        val hits = SongCutFinder.find(src, StemEditor, 0, AutoCut.Attacks, 0, src.frames)
        assertEquals(24, hits.size)
        hits.forEachIndexed { i, r -> assertEquals(i * BOARD_RATE / 2.0, r.first.toDouble(), 960.0) }
        assertEquals(6, SongCutFinder.find(src, StemEditor, 0, AutoCut.Bars, 0, src.frames).size)
        // The loudness, read ten seconds at a time: one value every 8 frames, loud where it sings, silent elsewhere.
        val e: Pcm = SongCutFinder.energyIn(src, VOCALS_STEM, 0, src.frames)
        assertEquals((src.frames / SongCutFinder.FACTOR).toInt(), e.frames)
        val sung = (1.5 * BOARD_RATE / 8).toInt()
        assertTrue((sung until sung + 600).maxOf { e.data[it] } > 0.3f)
        assertEquals(0f, (10 * BOARD_RATE / 8 until 11 * BOARD_RATE / 8).maxOf { e.data[it] })
    }

    @Test
    fun theRightsLineOnlyForASongOfSomeoneElse() {
        assertNull(RightsNotice.line(Project()))
        val line = RightsNotice.line(Project(remix = Remix("song", "Boys")))!!
        assertTrue(line, line.contains("Boys") && line.contains("auteurs"))
    }
}
