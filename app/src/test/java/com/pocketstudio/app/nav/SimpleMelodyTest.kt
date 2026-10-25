package com.pocketstudio.app.nav

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.pocketstudio.model.track
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Lot 2 (audit 35, amateurs 1 and 4): the Mode simple's « ● Enregistrer ma mélodie » recorded
 * into the first track (« Synthé ») rather than « Mélodie », and the line about the loop growing
 * to four bars spoke of « boucle » and « ↶ ».
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h890dp")
class SimpleMelodyTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val h = AppHarness(compose)

    @After
    fun end() = h.finish()

    private fun tap(tag: String) {
        compose.onNodeWithTag(tag).performClick()
        compose.waitForIdle()
    }

    @Test
    fun theMelodyGoesIntoTheMelodyTrack() {
        h.start(simple = true)
        // The song's tracks: « Synthé » first, then « Basse » and « Lead » (« Mélodie » in the Mode simple).
        assertEquals(0, h.vm.selectedTrack.value)
        tap("tab_simple_Clavier")
        assertEquals("the Clavier shows the melody track", h.trackId("Lead"), h.vm.selectedTrack.value)
        h.vm.playingForTest = true
        tap("record_start")
        assertTrue(h.vm.recording.value)
        h.vm.recordPositionForTest = 1.0
        h.vm.playedClockForTest = 1.0
        h.vm.synthNoteOn(64, 0.8f)
        h.vm.recordPositionForTest = 2.0
        h.vm.playedClockForTest = 2.0
        h.vm.synthNoteOff(64)
        tap("record_stop")
        val p = h.vm.project.value
        assertEquals("one note on « Mélodie »", 1, p.tracks.first { it.name == "Lead" }.notes.notes.size)
        assertTrue("nothing on « Synthé »", p.track(0).notes.notes.isEmpty())
    }

    @Test
    fun aTrackChosenInTheClavierStays() {
        h.start(simple = true)
        tap("tab_simple_Clavier")
        h.vm.selectTrack(h.trackId("Basse"))
        compose.waitForIdle()
        assertEquals(h.trackId("Basse"), h.vm.selectedTrack.value)
        // Review: another visit of the Clavier keeps it (« Mélodie » is chosen once per project).
        tap("tab_simple_Rythme")
        tap("tab_simple_Clavier")
        assertEquals(h.trackId("Basse"), h.vm.selectedTrack.value)
    }

    @Test
    fun theLoopLineSaysItInPlainWords() {
        h.start(simple = true)
        tap("tab_simple_Clavier")
        h.vm.playingForTest = true
        tap("record_start")
        val line = androidx.compose.ui.test.hasAnyAncestor(androidx.compose.ui.test.hasTestTag("take_loop_grown"))
        compose.onNode(androidx.compose.ui.test.hasText("Tu as 4 mesures pour jouer ta mélodie", substring = true) and line).assertExists()
        val texts = compose.onAllNodes(line).fetchSemanticsNodes()
            .flatMap { it.config.getOrElse(androidx.compose.ui.semantics.SemanticsProperties.Text) { emptyList() } }.joinToString { it.text }
        assertFalse(texts, "boucle" in texts || "↶" in texts)
    }
}
