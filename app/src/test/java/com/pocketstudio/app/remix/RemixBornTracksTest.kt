package com.pocketstudio.app.remix

import com.pocketstudio.app.ui.simpleTracks
import com.pocketstudio.model.Project
import com.pocketstudio.model.Remix
import com.pocketstudio.model.SongPart
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Benjamin's n° 23: a project born from a Remix shows no empty « Batterie » or « Synthé » in « Mes pistes ». */
class RemixBornTracksTest {
    @Test
    fun noEmptyTrackInAProjectBornFromARemix() {
        val p = Project(name = "Remix", remix = Remix("set", "Boys"))
        val keys = simpleTracks(p).map { it.key }
        assertTrue("empty drums shown: $keys", SongPart.DRUMS !in keys)
        assertTrue("empty Synthé shown: $keys", SongPart.inst(0) !in keys)
        // Once the drums play, they are there.
        val withHit = p.copy(drums = p.drums.copy(lanes = p.drums.lanes.mapIndexed { i, l -> if (i == 0) l.copy(hits = mapOf(0 to com.pocketstudio.model.Hit())) else l }))
        assertTrue(SongPart.DRUMS in simpleTracks(withHit).map { it.key })
        // A project without a song keeps its tracks as before.
        assertEquals(listOf(SongPart.DRUMS, SongPart.inst(0)), simpleTracks(Project()).map { it.key }.take(2))
    }
}
