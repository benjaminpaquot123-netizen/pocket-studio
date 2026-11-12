package com.pocketstudio.app.remix

import com.pocketstudio.model.AutoCut
import com.pocketstudio.model.CutMagnet
import com.pocketstudio.model.CutPiece
import com.pocketstudio.model.Cuts
import com.pocketstudio.model.SongCuts
import com.pocketstudio.model.StemCuts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * « Un bout » (Benjamin, 2026-11-04: « propre et sans fioriture… deux barres seulement »): the cut
 * screen opens on two bars, « Début » and « Fin », around the first phrase; moved with the magnet,
 * never crossed, undone, saved; the one piece between them is what the send buttons take.
 */
class CutSpanModelTest {
    private val frames = 30L * BOARD_RATE
    private val beat = 24_000L  // 120 BPM

    private fun model(saved: SongCuts = SongCuts("song"), into: MutableList<SongCuts> = mutableListOf(), bpm: Double = 120.0) =
        CutModel("song", frames, bpm, 0.0, saved, onChange = { into += it })

    /** The voice's first auto cuts as the screen makes them: three phrases. */
    private val phrases = listOf(48_000L until 96_000L, 168_000L until 264_000L, 336_000L until 408_000L)

    @Test
    fun opensOnOnePieceAroundTheFirstPhraseWithoutAnUndoStep() {
        val saved = mutableListOf<SongCuts>()
        val m = model(into = saved)
        assertTrue(m.single)
        assertEquals(AutoCut.Selection, m.mode)
        assertNull(m.span)
        assertTrue(m.toSend().isEmpty())
        m.firstCuts(AutoCut.Phrases, 0, frames, phrases)
        assertEquals(CutPiece(48_000, 96_000), m.span)
        // The phrases are kept for « Coupes › Phrases »; the opening is no act to undo.
        assertEquals(3, m.pieces.size)
        assertFalse(m.canUndo)
        assertEquals(m.cuts, saved.last())
        // The one piece by itself is what is sent, ticked or not.
        assertEquals(listOf(StemSelection("song", VOCALS_STEM, 48_000, 96_000, BoardFade.Small.inMs, BoardFade.Small.outMs)), m.toSend())
    }

    @Test
    fun nothingFoundPlacesTheFirstTwoBarsOfThePartShown() {
        val m = model()
        m.firstCuts(AutoCut.Phrases, 0, frames, emptyList(), shownFrom = 100_000)
        // The next bar line (192 000), two bars long.
        assertEquals(CutPiece(192_000, 384_000), m.span)
        // No tempo: four seconds from there.
        val free = model(bpm = 0.0)
        free.firstCuts(AutoCut.Attacks, 0, frames, emptyList(), shownFrom = 100_000)
        assertEquals(CutPiece(100_000, 100_000 + 4L * BOARD_RATE), free.span)
    }

    @Test
    fun aBarDraggedSticksToTheBeatNeverCrossesTheOtherAndIsOneUndoStep() {
        val saved = mutableListOf<SongCuts>()
        val m = model(into = saved)
        m.firstCuts(AutoCut.Phrases, 0, frames, phrases)
        val writes = saved.size
        // A finger near « Fin » grabs it (not « Début »), dragged: on the beat nearest.
        assertTrue(m.beginMove(96_000 + 1_000, 4_000))
        m.moveTo(130_000)
        m.moveTo(150_000)
        assertEquals(144_000L, m.movedTo)
        assertEquals(writes, saved.size)  // saved when the finger lifts, once
        m.endMove()
        assertEquals(CutPiece(48_000, 144_000), m.span)
        assertEquals(writes + 1, saved.size)
        assertEquals(1, m.undoCount)
        // « Début » dragged past « Fin »: it stops the shortest piece before it.
        assertTrue(m.beginMove(48_000, 4_000))
        m.magnet = CutMagnet.Free
        m.moveTo(500_000)
        m.endMove()
        assertEquals(CutPiece(144_000 - Cuts.MIN_FRAMES, 144_000), m.span)
        assertTrue(m.span!!.frames >= Cuts.MIN_FRAMES)
        // Far from both bars: nothing grabbed (the touch places the play head).
        assertFalse(m.beginMove(600_000, 4_000))
        m.undo()
        assertEquals(CutPiece(48_000, 144_000), m.span)
        m.undo()
        assertEquals(CutPiece(48_000, 96_000), m.span)
        assertFalse(m.canUndo)
    }

    @Test
    fun startAndEndAtThePlayHeadAndTalkBackMovesABarOneBeatAtATime() {
        val m = model()
        m.firstCuts(AutoCut.Phrases, 0, frames, phrases)
        m.cursor = 30_500
        assertTrue(m.setStart())
        assertEquals(24_000L, m.span!!.start)
        m.magnet = CutMagnet.Bar
        m.cursor = 200_000
        assertTrue(m.setEnd())
        assertEquals(192_000L, m.span!!.end)
        // « Avancer / reculer d'un temps », whatever the magnet.
        assertTrue(m.nudgeSpan(end = true, beats = 1))
        assertEquals(192_000L + beat, m.span!!.end)
        assertTrue(m.nudgeSpan(end = false, beats = -1))
        assertEquals(0L, m.span!!.start)
        assertFalse(m.nudgeSpan(end = false, beats = -1))  // already at the song's start
        assertEquals(4, m.undoCount)
        // The pieces of the auto cuts never moved.
        assertEquals(phrases.map { CutPiece(it.first, it.last + 1) }, m.pieces)
    }

    @Test
    fun theOtherModesKeepThePiecesAndBackToOnePieceKeepsTheBars() {
        val m = model()
        m.firstCuts(AutoCut.Phrases, 0, frames, phrases)
        m.cursor = 10 * beat
        m.setEnd()
        val span = m.span
        m.mode = AutoCut.Phrases
        assertFalse(m.single)
        assertTrue(m.toSend().isEmpty())  // nothing ticked: the pieces' rules again
        m.toggleChosen(1)
        assertEquals(listOf(m.selection(1)), m.toSend())
        m.cursor = 3 * beat
        assertTrue(m.setStart())  // « Début » trims the piece under the play head again
        assertEquals(span, m.span)
        m.mode = AutoCut.Selection
        assertEquals(span, m.span)
        assertEquals(listOfNotNull(m.spanSelection()), m.toSend())
    }

    @Test
    fun aProjectOfBeforeKeepsItsCutsAndGetsItsBarsOnItsFirstPiece() {
        val pieces = listOf(CutPiece(96_000, 192_000), CutPiece(192_000, 288_000))
        val old = SongCuts("song", listOf(StemCuts(VOCALS_STEM, pieces, chosen = listOf(1), auto = "Phrases")))
        val m = model(saved = old)
        assertTrue(m.single)
        assertTrue(m.cutYet)
        m.ensureSpan()
        assertEquals(CutPiece(96_000, 192_000), m.span)
        assertEquals(pieces, m.pieces)
        assertEquals(listOf(1), m.chosen)
        assertEquals(AutoCut.Phrases, m.auto)
        assertFalse(m.canUndo)
    }
}
