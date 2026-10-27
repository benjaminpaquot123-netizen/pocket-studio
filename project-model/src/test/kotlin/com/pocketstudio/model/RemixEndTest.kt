package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Test

/** Audit 02 G1: a loop export (and « Partager » without a song) runs to the end of the remixed song. */
class RemixEndTest {
    private val song = Remix("abc", "Chanson", songBpm = 120.0, seconds = 210.0)

    @Test
    fun theRemixEndsWhereItsSongDoes() {
        // 3:30 at 120 BPM in 4/4: 105 bars.
        val p = Project(tempoBpm = 120.0, remix = song)
        assertEquals(105, p.remixEndBars())
        assertEquals(105, p.exportEndBars())
        // Started on its bar 5: four bars less.
        assertEquals(101, p.copy(remix = song.copy(startBar = 4)).remixEndBars())
        // A project at 100 BPM playing it at its own tempo: the same 210 s, fewer bars of the project.
        assertEquals(88, p.copy(tempoBpm = 100.0).remixEndBars())  // 210 s = 87.5 bars of 2.4 s
        // Following the project's tempo: stretched to 252 s, still 105 of the song's bars.
        assertEquals(105, p.copy(tempoBpm = 100.0, remix = song.copy(followTempo = true)).remixEndBars())
        // Its length not known yet (an older project): given by the stems loaded.
        val old = p.copy(remix = song.copy(seconds = 0.0))
        assertEquals(0, old.remixEndBars())
        assertEquals(105, old.exportEndBars(210.0))
    }

    @Test
    fun aRemixNotHeardAddsNothing() {
        val p = Project(tempoBpm = 120.0, remix = song)
        assertEquals(0, Project(tempoBpm = 120.0).remixEndBars())
        val strip = p.mixer.channels.mapIndexed { i, c -> if (i == Remix.STRIP) c.copy(mute = true) else c }
        assertEquals(0, p.copy(mixer = p.mixer.copy(channels = strip)).remixEndBars())
        assertEquals(0, p.copy(remix = song.copy(stems = List(Remix.STEMS) { StemMix(mute = true) })).remixEndBars())
        // Started past its end.
        assertEquals(0, p.copy(remix = song.copy(startBar = 200)).remixEndBars())
        // No audio clip: the export ends with the remix.
        assertEquals(105, p.exportEndBars())
    }

    @Test
    fun theSongsLengthIsSavedAndOlderProjectsReadWithout() {
        val p = Project(remix = song)
        assertEquals(210.0, ProjectJson.decode(ProjectJson.encode(p)).remix.seconds, 0.0)
        val older = ProjectJson.decode(ProjectJson.encode(p).replace(Regex(",\\s*\"seconds\":\\s*210\\.0"), ""))
        assertEquals(0.0, older.remix.seconds, 0.0)
        assertEquals("abc", older.remix.setId)
        assertEquals(0.0, Remix(seconds = Double.NaN).sanitized().seconds, 0.0)
        assertEquals(Remix.MAX_SECONDS, Remix(seconds = 1e9).sanitized().seconds, 0.0)
    }
}
