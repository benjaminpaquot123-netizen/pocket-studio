package com.pocketstudio.app.mixer

import com.pocketstudio.app.ui.mixerStrips
import com.pocketstudio.model.InstrumentTrack
import com.pocketstudio.model.Project
import com.pocketstudio.model.firstTrackDeleted
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 8 (D7/U27): the Mixer names the same tracks as the Sons page. The Sons page leaves out the first track once it
 * was deleted and is still blank; the Mixer's strips (and every list built from them) leave it out too, in the same
 * order otherwise.
 */
class Tour8MixerNamesTest {
    private val first = InstrumentTrack.mixerChannel(0)

    @Test
    fun aNewProjectStillShowsItsFirstTrack() {
        assertTrue(mixerStrips(Project()).any { it.ch == first })
    }

    @Test
    fun theDeletedFirstTrackIsNotAStripEither() {
        val p = Project().copy(firstTrackHidden = true)
        assertTrue("the test's project should have its first track deleted", p.firstTrackDeleted)
        val strips = mixerStrips(p)
        assertTrue("the Mixer still shows the deleted first track", strips.none { it.ch == first })
        assertEquals("the other strips moved", mixerStrips(Project()).filter { it.ch != first }, strips)
    }

    /** In a tutorial that hides the blank first track (the Sons page's « tutoHidesFirst »), the Mixer hides it too. */
    @Test
    fun aTutorialHidingTheFirstTrackHidesItsStripToo() {
        val strips = mixerStrips(Project(), hideFirst = true)
        assertTrue("the Mixer shows the first track the tutorial hides", strips.none { it.ch == first })
        assertEquals("the other strips moved", mixerStrips(Project()).filter { it.ch != first }, strips)
    }
}
