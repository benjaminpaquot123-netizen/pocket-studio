package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Chantier 10 (G16, T5-Ec-28): the remix's song past the song's end, said, and its two fixes. */
class RemixOverrunTest {
    /** 8 bars at 120 BPM (128 steps, 16 s); a song of 60 s coming in on bar 2 (step 16): it lasts 480 steps, up to step 496. */
    private fun overrun(song: List<SongPart> = listOf(SongPart(0, bars = 4), SongPart(1, bars = 4))) = Project(
        song = song,
        remix = Remix(setId = "s", name = "Boys", songBpm = 120.0, seconds = 60.0, atStep = 16, placed = true),
    )

    @Test
    fun theOverrunIsCountedFromWhereTheSongComesIn() {
        val p = overrun()
        assertEquals(496L - 128L, p.remixOverrunSteps())
        assertEquals(46, p.remixOverrunSeconds())  // 368 sixteenths of 0.125 s
    }

    @Test
    fun aSongThatEndsInTimeOrIsNotHeardSaysNothing() {
        assertEquals(0, overrun(listOf(SongPart(0, bars = 31))).remixOverrunSeconds())
        val muted = overrun().let { it.copy(remix = it.remix.copy(stems = List(Remix.STEMS) { StemMix(mute = true) })) }
        assertEquals(0, muted.remixOverrunSeconds())
        assertEquals(0, overrun(emptyList()).remixOverrunSeconds())  // no song yet: the loop export lets it ring
    }

    @Test
    fun lengtheningReplaysTheLastPartUntilTheSongEnds() {
        val p = overrun().withSongLengthenedForRemix()
        assertEquals(3, p.song.size)
        assertEquals(SongPart(1, bars = 23), p.song.last())  // 368 steps = 23 bars, the last part's brick
        assertEquals(0, p.remixOverrunSeconds())
        assertTrue(p.songBars >= p.remixEndBars())
        assertEquals(p, p.withSongLengthenedForRemix())  // nothing left to do
    }

    @Test
    fun cuttingEndsEveryStemWithTheSongFaded() {
        val p = overrun().withRemixCutAtSongEnd()
        assertEquals(0, p.remixOverrunSeconds())
        assertEquals(p.songBars, p.remixEndBars())
        // The song comes in on step 16: the song's end (step 128) is 112 sixteenths later, 14 s into it.
        p.remix.stems.forEach { s ->
            val c = s.clips!!.single()
            assertEquals(14.0, c.end, 1e-9)
            assertEquals(REMIX_CUT_FADE_SECONDS, c.fadeOut, 1e-9)
        }
        assertEquals(p, p.withRemixCutAtSongEnd())
    }

    @Test
    fun aPieceMovedPastTheEndIsTakenAwayAndItsStemIsSilent() {
        val base = overrun()
        // Stem 0 holds one piece only, moved to 20 s (after the cut at 14 s).
        val moved = base.copy(remix = base.remix.withStem(0) { it.copy(clips = listOf(StemClip(at = 20.0, from = 0.0, length = 5.0))) })
        val cut = moved.withRemixCutAtSongEnd()
        assertEquals(emptyList<StemClip>(), cut.remix.stems[0].clips)
        assertEquals(0, cut.remixOverrunSeconds())
        // Every stem emptied: nothing of the song is heard any more (not « as long as the song »).
        val none = base.copy(remix = base.remix.copy(stems = List(Remix.STEMS) { StemMix(clips = emptyList()) }))
        assertEquals(0.0, none.remix.heardEnd(), 0.0)
        assertEquals(0, none.remixSteps())
    }

    /** Fresh review: the last part had the song switched off (or its stems): the parts added let it be heard. */
    @Test
    fun theAddedPartsLetTheSongBeHeard() {
        val last = SongPart(1, bars = 4, muted = setOf(SongPart.REMIX, Remix.stemKey(3), "drums"))
        val p = overrun(listOf(SongPart(0, bars = 4), last)).withSongLengthenedForRemix()
        val added = p.song.last()
        assertTrue("the song heard in the part added: ${added.muted}", added.plays(SongPart.REMIX) && added.plays(Remix.stemKey(3)))
        assertTrue("the other tracks as in the last part", !added.plays("drums"))
    }
}
