package com.pocketstudio.model

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

/** Step 5b: the song's stems cut, moved and faded on the bars, never touching their files. */
class StemEditsTest {
    /** « Boys », 120 BPM like the project, 64 s: a bar = 2 s, a sixteenth = 0.125 s. */
    private val p = Project(tempoBpm = 120.0, remix = Remix("set-boys", "Boys", songBpm = 120.0, seconds = 64.0)).withRemixPlaced()
    private val voice = 3

    @Test
    fun aStemCutInTwoPlaysTheSameAndKeepsBothPieces() {
        val cut = p.remix.cutStem(voice, 8.0)
        assertEquals(listOf(StemClip(0.0, 0.0, 8.0), StemClip(8.0, 8.0, 56.0)), cut.stems[voice].clips)
        // The other stems untouched, the file never changed (the pieces only point into it).
        assertNull(cut.stems[0].clips)
        // Cutting on an edge or nowhere changes nothing.
        assertSame(cut, cut.cutStem(voice, 8.0 - 1e-4))
        assertSame(cut, cut.cutStem(voice, 70.0))
    }

    @Test
    fun aPieceMovedByABarFadedAndTakenAway() {
        val bar = 16 * p.remixSecondsPerStep()
        assertEquals(2.0, bar, 1e-9)
        val moved = p.remix.cutStem(voice, 8.0).moveStemPiece(voice, 10.0, bar)
        // Moved on top, its own seconds kept: a bar of silence after the first piece.
        assertEquals(listOf(StemClip(0.0, 0.0, 8.0), StemClip(10.0, 8.0, 56.0)), moved.stems[voice].clips)
        assertEquals(-1, moved.pieceAt(voice, 9.0))
        // The song now lasts a bar longer.
        assertEquals(66.0, moved.heardEnd(), 1e-9)
        assertEquals(33 * 16, p.copy(remix = moved).remixSteps())
        val faded = moved.fadeStemPiece(voice, 1.0, fadeOut = bar).fadeStemPiece(voice, 12.0, fadeIn = bar)
        assertEquals(2.0, faded.stems[voice].clips!![0].fadeOut, 1e-9)
        assertEquals(2.0, faded.stems[voice].clips!![1].fadeIn, 1e-9)
        val gone = faded.removeStemPiece(voice, 1.0)
        assertEquals(listOf(10.0), gone.stems[voice].clips!!.map { it.at })
        assertNull(gone.resetStem(voice).stems[voice].clips)
        // Not before the timeline's start.
        assertEquals(0.0, p.remix.moveStemPiece(voice, 1.0, -10.0).stems[voice].clips!!.single().at, 1e-9)
    }

    @Test
    fun theEngineGetsFramesOfTheSongAsSeparated() {
        val r = p.remix.cutStem(voice, 8.0).fadeStemPiece(voice, 9.0, fadeIn = 0.5)
        assertNull(r.stemClipFrames(0, 48_000))
        assertArrayEquals(
            longArrayOf(0, 0, 384_000, 0, 0, 384_000, 384_000, 2_688_000, 24_000, 0),
            r.stemClipFrames(voice, 48_000),
        )
    }

    @Test
    fun theBarsFollowTheSongPlacedLaterAndAtAnotherTempo() {
        // Brought in on bar 3 (step 32): the song's second 0 is heard there.
        val later = p.copy(remix = p.remix.copy(atStep = 32))
        assertEquals(0.0, later.remixTimeAtStep(32), 1e-9)
        assertEquals(2.0, later.remixTimeAtStep(48), 1e-9)
        // Following a 60 BPM project: a project bar is still one bar of the song (2 s of it).
        val slow = p.copy(tempoBpm = 60.0, remix = p.remix.copy(followTempo = true))
        assertEquals(2.0, 16 * slow.remixSecondsPerStep(), 1e-9)
        assertEquals(48.0, slow.remixStepAtTime(6.0), 1e-9)
    }

    @Test
    fun aStemLineShowsItsPiecesAndTheEditsAreSavedAndReopened() {
        val edited = p.copy(remix = p.remix.cutStem(voice, 8.0).moveStemPiece(voice, 10.0, 2.0))
        // One part of 32 bars (64 s): the voice heard 0-8 s, then 10 s on (to the end of the part).
        assertEquals(listOf(listOf(0f..0.125f, 0.15625f..1f)), edited.remixStemSpans(voice))
        assertEquals(edited.remixSpans(), edited.remixStemSpans(0))
        val back = ProjectJson.decode(ProjectJson.encode(edited))
        assertEquals(edited.remix.stems, back.remix.stems)
        // Projects without edits write nothing new.
        fun clipsSaved(q: Project) = kotlinx.serialization.json.Json.parseToJsonElement(ProjectJson.encode(q))
            .let { it as kotlinx.serialization.json.JsonObject }["remix"].let { it as kotlinx.serialization.json.JsonObject }["stems"]
            .let { it as kotlinx.serialization.json.JsonArray }.count { "clips" in (it as kotlinx.serialization.json.JsonObject) }
        assertEquals(0, clipsSaved(p))
        assertEquals(1, clipsSaved(edited))
    }
}
