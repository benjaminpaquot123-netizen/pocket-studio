package com.pocketstudio.app.ui

import androidx.compose.ui.test.onAllNodesWithTag
import com.pocketstudio.app.human.HumanTester
import com.pocketstudio.app.human.SeatOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Tour 9, F131 (seats t9-09: fr, phone 360, 100 %, Mode simple, tuto afrobeats; t9-55: pl, tablet, 130 %, Pro, tuto pop):
 * the tutorial's panel folded (« Je le fais » or its « ⌄ »), the grid under it, Android's Back left the project for the
 * welcome screen at once. Acceptance: Back closes first the sheet or editor open; it leaves the project only last.
 * Here the folded panel is what is open over the grid: Back unfolds it and stays on the same tab; a second Back leaves.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h780dp-xhdpi")
class T9F131BackUnfoldsTutoTest : HumanTester("t9f131") {
    private fun screen() = screenLines().joinToString("\n")
    private fun has(tag: String) = compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
    private fun route() = screenText().read().route
    /** The panel folded: its one line under the header (the step's words, « Suivant » or the need) instead of the step. */
    private fun folded() = listOf("tuto_folded_text", "tuto_need_folded", "tuto_next_folded").any(::has)

    @Test
    fun backUnfoldsTheFoldedTutoBeforeLeavingTheProject() {
        assumeTrue(System.getProperty("ps.human.dir").isNullOrBlank())
        val o = SeatOptions.parse(mapOf("lang" to "fr", "device" to "phone360", "font" to "100", "mode" to "simple", "amenee" to "tuto:afrobeats:1")).first
        begin(o)
        amenee(o.amenee!!, java.io.File.createTempFile("amenee", ".txt").apply { deleteOnExit() })
        act("attends 1")
        val fold = reachTag("tuto_fold") ?: error("no « ⌄ » on the tutorial's panel:\n" + screen())
        act("touche ${fold.first.toInt()} ${fold.second.toInt()}")
        act("attends 1")
        assertTrue("the panel not folded:\n" + screen(), folded())
        val where = route()
        act("retour")
        act("attends 1")
        assertTrue("Back left the project (route ${route()}) with the tutorial folded:\n" + screen(), route() != "launch" && !has("launch_learn"))
        assertEquals("Back changed the tab:\n" + screen(), where, route())
        assertTrue("Back did not unfold the tutorial:\n" + screen(), !folded())
        // Unfolded, nothing else open: Back leaves the project last, as before.
        act("retour")
        act("attends 1")
        assertEquals("the second Back did not leave the project:\n" + screen(), "launch", route())
    }
}
