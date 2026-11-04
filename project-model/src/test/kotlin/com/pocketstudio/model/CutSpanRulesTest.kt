package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** « Un bout » (Benjamin, 2026-11-04): the pure rules of the cut screen's two bars, « Début » and « Fin ». */
class CutSpanRulesTest {
    private val rate = 48_000
    private val frames = 10L * rate
    private val beat = Cuts.beatFrames(120.0, rate)  // 24 000 frames, a bar of 96 000

    @Test
    fun aSpanStaysInsideTheStemAndAtLeastAHitLong() {
        assertEquals(CutPiece(1_000, 50_000), Cuts.span(1_000, 50_000, frames))
        // Reversed or too short: « Fin » goes to « Début » plus the shortest piece.
        assertEquals(CutPiece(50_000, 50_000 + Cuts.MIN_FRAMES), Cuts.span(50_000, 10, frames))
        // Past the end: brought back inside, still long enough.
        assertEquals(CutPiece(frames - Cuts.MIN_FRAMES, frames), Cuts.span(frames + 10, frames + 99, frames))
        assertNull(Cuts.span(0, 100, Cuts.MIN_FRAMES - 1))
    }

    @Test
    fun debutNeverPassesFinAndFinNeverPassesTheEnd() {
        val sp = CutPiece(96_000, 192_000)
        assertEquals(CutPiece(120_000, 192_000), Cuts.spanStart(sp, 120_000))
        assertEquals(192_000L - Cuts.MIN_FRAMES, Cuts.spanStart(sp, 300_000).start)
        assertEquals(0L, Cuts.spanStart(sp, -5).start)
        assertEquals(CutPiece(96_000, 150_000), Cuts.spanEnd(sp, 150_000, frames))
        assertEquals(96_000L + Cuts.MIN_FRAMES, Cuts.spanEnd(sp, 0, frames).end)
        assertEquals(frames, Cuts.spanEnd(sp, frames * 2, frames).end)
    }

    @Test
    fun firstPlacedAroundTheFirstPhraseElseTwoBarsOfThePartShown() {
        // The first phrase found long enough (a blip before it is no phrase).
        val found = listOf(10_000L until 11_000L, 48_000L until 120_000L, 200_000L until 260_000L)
        assertEquals(CutPiece(48_000, 120_000), Cuts.firstSpan(found, 0, frames, beat, 0))
        // None: the first two bars from the part shown, on a bar line.
        assertEquals(CutPiece(192_000, 384_000), Cuts.firstSpan(emptyList(), 100_000, frames, beat, 0))
        // The song's first beat late: the bars follow it.
        assertEquals(CutPiece(12_000, 204_000), Cuts.firstSpan(emptyList(), 0, frames, beat, 12_000))
        // Near the end: the two bars end with the song.
        assertEquals(CutPiece(frames - 192_000, frames), Cuts.firstSpan(emptyList(), frames - 1_000, frames, beat, 0))
        // No tempo: four seconds from the part shown.
        assertEquals(CutPiece(48_000, 48_000 + 4L * rate), Cuts.firstSpan(emptyList(), 48_000, frames, 0.0, 0))
    }

    @Test
    fun theSpanIsSavedWithTheCutsAndAnOldProjectHasNone() {
        val cuts = SongCuts("s", listOf(StemCuts(3, listOf(CutPiece(0, 96_000)), auto = "Phrases", span = CutPiece(10, 60_000))))
        val back = ProjectJson.decode(ProjectJson.encode(Project(songCuts = cuts))).songCuts
        assertEquals(cuts, back)
        // Sanitized: brought inside the song, dropped when the song is too short for one.
        assertEquals(CutPiece(10, 50_000), cuts.with(cuts.stems[0].copy(span = CutPiece(10, 90_000))).sanitized("s", 50_000).of(3)!!.span)
        assertNull(cuts.sanitized("s", 1_000).of(3)!!.span)
        // A project of before « Un bout »: its cuts as they were, no span.
        val old = """{"songCuts":{"setId":"s","stems":[{"stem":3,"pieces":[{"start":0,"end":96000}],"chosen":[0],"auto":"Phrases"}]}}"""
        val s = ProjectJson.decode(old).songCuts.of(3)!!
        assertEquals(listOf(CutPiece(0, 96_000)), s.pieces)
        assertEquals(listOf(0), s.chosen)
        assertNull(s.span)
    }
}
