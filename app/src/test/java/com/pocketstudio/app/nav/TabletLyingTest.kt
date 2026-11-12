package com.pocketstudio.app.nav

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.pocketstudio.app.record.TakePhase
import com.pocketstudio.app.record.TakeSource
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Lot 2 (audit 35, amateur 4 and the tablet, grave): Benjamin's Galaxy Tab A11 lying down
 * (1006 × 528 dp once its bars are taken), the keyboard screen (Sons and the Mode simple's
 * Clavier). After a take, its card and the loop's line pushed the keys down to 60 dp, and at
 * 130 % off the screen. The keys keep their height and the card stands beside them. And Sons ›
 * Batterie cut its fourth row of pads there (the Mode simple's just fitted).
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "sw600dp-w1006dp-h528dp-land-tvdpi")
class TabletLyingTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val h = AppHarness(compose)

    @After
    fun end() = h.finish()

    private fun tap(tag: String) {
        compose.onNodeWithTag(tag).performClick()
        compose.waitForIdle()
    }

    /** A take of two notes, stopped: its card (Garder, Remettre en rythme, Refaire) shows. */
    private fun takeTwoNotes() {
        val vm = h.vm
        vm.playingForTest = true
        vm.recordTake(TakeSource.KEYS)
        for ((pitch, from, to) in listOf(Triple(60, 1.2, 2.9), Triple(62, 3.8, 5.0))) {
            vm.recordPositionForTest = from
            vm.playedClockForTest = from
            vm.synthNoteOn(pitch, 0.8f)
            vm.recordPositionForTest = to
            vm.playedClockForTest = to
            vm.synthNoteOff(pitch)
        }
        vm.recordTake(TakeSource.KEYS)
        assertEquals(TakePhase.DONE, vm.noteTake.value!!.phase)
        compose.waitForIdle()
    }

    private fun assertKeysAndCardOnScreen(scale: Float, what: String) {
        val px = compose.activity.resources.displayMetrics.density * scale
        val screen = compose.onRoot().fetchSemanticsNode().boundsInRoot
        val keys = compose.onNodeWithTag("keyboard").fetchSemanticsNode().boundsInRoot
        val card = compose.onNodeWithTag("take_done").fetchSemanticsNode().let { androidx.compose.ui.geometry.Rect(it.positionInRoot, androidx.compose.ui.geometry.Size(it.size.width.toFloat(), it.size.height.toFloat())) }
        val keysDp = keys.height / px
        assertTrue("$what: the keys are $keysDp dp high", keysDp >= 100f)
        assertTrue("$what: the keys end at ${keys.bottom}, the screen at ${screen.bottom}", keys.bottom <= screen.bottom + 1f)
        assertTrue("$what: the take's card ends at ${card.bottom}, the screen at ${screen.bottom}", card.bottom <= screen.bottom + 1f)
        // Beside the keys, not above them.
        assertTrue("$what: the card (${card.left}) is right of the keys (${keys.right})", card.left >= keys.right - 1f)
        compose.onNodeWithTag("take_keep").assertExistsOnScreen(screen.bottom)
        // Review: no blank band over « Clavier | Notes » (the switch squeezed the octave's control, its row grew).
        val synthTop = compose.onNodeWithTag("synth").fetchSemanticsNode().boundsInRoot.top
        val switchTop = compose.onNodeWithTag("mode_keyboard").fetchSemanticsNode().boundsInRoot.top
        assertTrue("$what: « Clavier » starts ${(switchTop - synthTop) / px} dp under the screen's top", (switchTop - synthTop) / px <= 24f)
    }

    private fun androidx.compose.ui.test.SemanticsNodeInteraction.assertExistsOnScreen(bottom: Float) {
        val b = fetchSemanticsNode().boundsInRoot
        assertTrue("« Garder » ends at ${b.bottom} (screen $bottom)", b.bottom <= bottom + 1f && b.height > 0f)
    }

    @Test
    fun theModeSimplesClavierKeepsItsKeysAfterATake() {
        h.start(simple = true)
        tap("tab_simple_Clavier")
        takeTwoNotes()
        assertKeysAndCardOnScreen(1f, "Mode simple, 100 %")
    }

    @Test
    fun theModeSimplesClavierKeepsItsKeysAt130() {
        h.start(simple = true, scale = 1.3f)
        tap("tab_simple_Clavier")
        takeTwoNotes()
        assertKeysAndCardOnScreen(1.3f, "Mode simple, 130 %")
    }

    @Test
    fun theSonsKeyboardKeepsItsKeysAfterATake() {
        h.start(scale = 1.3f)
        tap("tab_Sons")
        tap("track_${h.trackId("Lead")}")
        takeTwoNotes()
        assertKeysAndCardOnScreen(1.3f, "Sons, 130 %")
    }

    /** Review: a phone lying down, Sons › a track: no blank band over « Clavier | Notes », the keys on screen. */
    @Test
    @Config(qualifiers = "w860dp-h400dp-land")
    fun aPhoneLyingDownKeepsItsKeysToo() {
        h.start()
        tap("tab_Sons")
        tap("track_${h.trackId("Lead")}")
        val px = compose.activity.resources.displayMetrics.density
        val synthTop = compose.onNodeWithTag("synth").fetchSemanticsNode().boundsInRoot.top
        val switchTop = compose.onNodeWithTag("mode_keyboard").fetchSemanticsNode().boundsInRoot.top
        assertTrue("« Clavier » starts ${(switchTop - synthTop) / px} dp under the screen's top", (switchTop - synthTop) / px <= 24f)
        // « Accords » and « Pads » keep their room on the line (the switch squeezed them to nothing).
        for (tag in listOf("chord_keys", "chord_pads_switch")) {
            val w = compose.onNodeWithTag(tag).fetchSemanticsNode().size.width / px
            assertTrue("$tag is $w dp wide", w >= 30f)
        }
        takeTwoNotes()
        val screen = compose.onRoot().fetchSemanticsNode().boundsInRoot
        val keys = compose.onNodeWithTag("keyboard").fetchSemanticsNode().let { it.positionInRoot.y + it.size.height }
        assertTrue("the keys end at $keys (screen ${screen.bottom})", keys <= screen.bottom + 1f)
    }

    @Test
    fun theFourRowsOfPadsFitLyingDown() = padsFit(1f)

    /** Review: at 130 % the A11 lying down gets one column; the pads still fit, « Kit ▾ » too. */
    @Test
    fun theFourRowsOfPadsFitLyingDownAt130() = padsFit(1.3f)

    private fun padsFit(scale: Float) {
        h.start(scale = scale)
        tap("tab_Sons")
        tap("sons_Rythme")
        val screen = compose.onRoot().fetchSemanticsNode().boundsInRoot
        // Unclipped: where the node really is, not what its scrolled parent lets show.
        fun bottomOf(tag: String) = compose.onNodeWithTag(tag).fetchSemanticsNode().let { it.positionInRoot.y + it.size.height }
        for (lane in 12..15) {
            val bottom = bottomOf("pad_$lane")
            assertTrue("pad $lane ends at $bottom (screen ${screen.bottom})", bottom <= screen.bottom + 1f)
            val high = compose.onNodeWithTag("pad_$lane").fetchSemanticsNode().size.height / compose.activity.resources.displayMetrics.density
            assertTrue("pad $lane is $high dp high on the glass", high >= 47.5f)
        }
        assertTrue("« Kit ▾ » ends at ${bottomOf("kit_chip")} (screen ${screen.bottom})", bottomOf("kit_chip") <= screen.bottom + 1f)
    }

    /** Unclipped: where the node really is, not what its scrolled parent lets show. */
    private fun bottomOf(tag: String) = compose.onNodeWithTag(tag).fetchSemanticsNode().let { it.positionInRoot.y + it.size.height }
    private fun topOf(tag: String) = compose.onNodeWithTag(tag).fetchSemanticsNode().positionInRoot.y

    /** Audit final (09): at 130 % the drums keep two columns lying down, the steps beside the pads. */
    @Test
    fun theStepsStandBesideThePadsAt130() {
        h.start(scale = 1.3f)
        tap("tab_Sons")
        tap("sons_Rythme")
        val pane = bottomOf("rhythm")
        assertTrue("the second row of steps ends at ${bottomOf("step_8")} (pane $pane)", bottomOf("step_8") <= pane + 1f)
        assertTrue("the steps (${compose.onNodeWithTag("step_0").fetchSemanticsNode().positionInRoot.x}) are right of the pads", compose.onNodeWithTag("step_0").fetchSemanticsNode().positionInRoot.x > compose.onNodeWithTag("pad_3").fetchSemanticsNode().positionInRoot.x)
    }

    /** Audit final (09): « Partager » whole on the screen, the sheet's content scrolling above it. */
    @Test
    fun theShareButtonIsOnScreenAt130() {
        h.start(scale = 1.3f)
        tap("top_share")
        // The sheet is a window of its own: the screen's is the first root.
        val screen = compose.onAllNodes(androidx.compose.ui.test.isRoot()).fetchSemanticsNodes().first().boundsInRoot
        val go = compose.onNodeWithTag("share_go").fetchSemanticsNode()
        assertTrue("« Partager » ends at ${go.positionInRoot.y + go.size.height} (screen ${screen.bottom})", go.positionInRoot.y + go.size.height <= screen.bottom + 1f)
        assertTrue("« Partager » starts at ${go.positionInRoot.y} (screen ${screen.top})", go.positionInRoot.y >= screen.top)
    }

    /** Audit final (09): Sustain and Arpège in sight over the keys, without scrolling. */
    @Test
    fun theKeyboardsSettingsShowLyingDownAt130() {
        h.start(scale = 1.3f)
        tap("tab_Sons")
        tap("track_${h.trackId("Lead")}")
        val keys = topOf("keyboard")
        for (tag in listOf("bend", "mod", "sustain", "arp")) {
            assertTrue("$tag ends at ${bottomOf(tag)}, the keys start at $keys", bottomOf(tag) <= keys + 1f)
        }
    }

    /** Audit final (09): the console's faders whole in the pane. */
    @Test
    fun theFadersFitLyingDownAt130() {
        h.start(scale = 1.3f)
        tap("tab_Mix")
        val pane = bottomOf("mixer")
        assertTrue("the first fader ends at ${bottomOf("volume_0")} (pane $pane)", bottomOf("volume_0") <= pane + 1f)
    }
}
