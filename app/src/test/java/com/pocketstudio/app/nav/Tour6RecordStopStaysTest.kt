package com.pocketstudio.app.nav

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Audit ultra round 6, lot 3, the portal's refusal (replay t4-26-prise-clavier, gesture 32: « record_stop » refused,
 * no visible point): « Fait : Nouveau son … » still showing, the user starts a take on the Mode simple's keys; when
 * the line went away the page got its height back and moved, and « ■ Arrêter » left the screen: a take that could
 * not be stopped. The line had been given a place of its own in the bottom bar (N15), which took room from the page
 * and gave it back six seconds later. It floats over the page again (the page never changes height for it), and it
 * keeps away while a take runs: nothing comes over the keys' page, or moves it, while « ■ Arrêter » is the way out.
 * The page's own move in the replay (it scrolls when a key is played, differently in a page made shorter) is not
 * reproduced here: the replay itself is its proof (scripts/bench/rejeu-defauts.sh t4-26-prise-clavier).
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp-xhdpi")
class Tour6RecordStopStaysTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()
    private val h = AppHarness(compose)
    private val shell = Tour6Shell(compose)

    @After
    fun back() {
        org.robolectric.RuntimeEnvironment.setFontScale(1f)
        h.finish()
    }

    /** « ■ Arrêter » is on screen, whole, where a finger can touch it. */
    private fun stopInReach(moment: String) {
        val node = shell.nodes().firstOrNull { Tour6Shell.tagOf(it) == "record_stop" }
        assertTrue("$moment: no « ■ Arrêter » on the page", node != null)
        val seen = node!!.boundsInRoot
        val whole = Tour6Shell.whole(node)
        val screen = compose.onRoot().fetchSemanticsNode().boundsInRoot
        assertTrue(
            "$moment: « ■ Arrêter » is cut or off the screen: in view $seen of $whole",
            seen.height >= whole.height - 1f && seen.width >= whole.width - 1f && seen.top >= screen.top && seen.bottom <= screen.bottom,
        )
        // Nothing of the shell over it.
        listOf("done_bar", "guide_bar", "guide_panel", "thumb_bar").forEach { tag ->
            val over = shell.bounds(tag)
            assertTrue("$moment: $tag at $over is over « ■ Arrêter » at $seen", over == null || !over.overlaps(seen))
        }
    }

    private fun doneLine() {
        val card = h.vm.guide.ui.value.next ?: h.vm.guide.ui.value.cards.values.first { !it.status.settled }
        h.vm.guide.skip(card.id)
        assertTrue("« Fait : … » shows", shell.reached(5_000) { shell.has("done_bar") })
        shell.idle(300)
    }

    /** The replay's order: the line shows, then the take starts: the line goes at once, for good, « ■ Arrêter » in reach. */
    private fun theLineLeavingDuringATake(scale: Float, fontScale: Float = 1f) {
        if (fontScale != 1f) org.robolectric.RuntimeEnvironment.setFontScale(fontScale)
        h.start(simple = true, scale = scale)
        h.awaitGuide()
        assertTrue(shell.reached { !h.vm.guide.ui.value.loading })
        shell.tap("tab_simple_Clavier")
        doneLine()
        h.vm.playingForTest = true
        shell.tap("record_start")
        assertTrue("the take runs", shell.reached(3_000) { h.vm.recording.value && shell.has("record_stop") })
        shell.idle(300)
        stopInReach("the take just started")
        h.vm.recordPositionForTest = 1.0
        h.vm.playedClockForTest = 1.0
        h.vm.synthNoteOn(64, 0.8f)
        h.vm.recordPositionForTest = 2.0
        h.vm.playedClockForTest = 2.0
        h.vm.synthNoteOff(64)
        shell.idle(300)
        // The take running, the line is away and its message let go (nothing here dismisses it: the app does).
        assertTrue("« Fait : … » over the keys' page during the take", !shell.has("done_bar"))
        assertTrue("the message of before the take is kept for after it", shell.reached(3_000) { h.vm.done.value == null })
        stopInReach("« Fait » gone, a note played")
        shell.tap("record_stop")
        assertTrue("« ■ Arrêter » stops the take", shell.reached(3_000) { !h.vm.recording.value })
        // The old message does not come back after the take (its « Annuler » would undo the take instead).
        shell.idle(600)
        assertTrue("« Fait : … » of before the take is back after it: « ${shell.textOf("done_text")} »", !shell.textOf("done_text").contains(com.pocketstudio.model.i18n.tr("Sauté : le Guide passe à la suite.")))
    }

    @Test fun theDoneLineIsLetGoWhenATakeStarts() = theLineLeavingDuringATake(1f)

    @Test fun theDoneLineIsLetGoWhenATakeStartsWithBigText() = theLineLeavingDuringATake(1.3f)

    /** The replay's own seat: a 392 x 852 dp phone, the phone's font at 200 %. */
    @Test
    @Config(qualifiers = "w392dp-h852dp-xhdpi")
    fun theDoneLineIsLetGoWhenATakeStartsAtTwiceTheFont() = theLineLeavingDuringATake(1f, fontScale = 2f)

    /** The other order: the take runs, something is done meanwhile: no line comes over the page during the take. */
    @Test
    fun noDoneLineComesOverThePageDuringATake() {
        h.start(simple = true)
        h.awaitGuide()
        assertTrue(shell.reached { !h.vm.guide.ui.value.loading })
        shell.tap("tab_simple_Clavier")
        h.vm.playingForTest = true
        shell.tap("record_start")
        assertTrue("the take runs", shell.reached(3_000) { h.vm.recording.value && shell.has("record_stop") })
        shell.idle(300)
        stopInReach("the take just started")
        val card = h.vm.guide.ui.value.next ?: h.vm.guide.ui.value.cards.values.first { !it.status.settled }
        h.vm.guide.skip(card.id)
        assertTrue("the choice is made", shell.reached(5_000) { h.vm.guide.ui.value.cards[card.id]?.status?.settled == true })
        shell.idle(600)
        assertTrue("« Fait : … » came over the keys' page during the take", !shell.has("done_bar"))
        stopInReach("something was done during the take")
    }
}
