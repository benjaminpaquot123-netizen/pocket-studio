package com.pocketstudio.app.remix

import android.app.Application
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.human.BenchWindows
import com.pocketstudio.app.human.HumanTester
import com.pocketstudio.app.human.SeatOptions
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Tour 8, lot R (review of decoupe2, moyen): « en es, main gauche, phone à 100 %, Mode Simple : j'arrive en « Un trozo »
 * avec un message resté d'avant et je vois la consigne en haut, sans saut. » The message lives until « OK »: the column
 * must not follow it down on arrival. A message that comes while the screen is open still brings the four buttons into
 * view (tour 7, lot 2 D3).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = HumanTester.PHONE)
class CutStaleMessageTest : HumanTester("t8-r-decoupe") {
    override val remixTimeScale = 0.25
    private val app: Application get() = ApplicationProvider.getApplicationContext()
    private val out = File("build/t8-r-decoupe").apply { mkdirs() }

    private fun node(tag: String) = compose.onAllNodesWithTag(tag).fetchSemanticsNodes().firstOrNull()

    private fun tap(tag: String): String {
        val at = reachTag(tag) ?: error("no $tag on:\n" + screenLines().joinToString("\n"))
        return act("touche ${at.first} ${at.second}")
    }

    private fun drawnEntire(tag: String): Boolean {
        val n = node(tag) ?: return false
        val b = BenchWindows.drawnBounds(n, app.resources.displayMetrics)
        return shownWhole(n) && b.height >= n.size.height - 1f && b.width >= n.size.width - 1f
    }

    private fun hintWhole(): Boolean {
        val hint = compose.onAllNodes(hasText("Coloca Inicio y Fin", substring = true)).fetchSemanticsNodes().firstOrNull() ?: return false
        val b = BenchWindows.drawnBounds(hint, app.resources.displayMetrics)
        return shownWhole(hint) && b.height >= hint.size.height - 1f
    }

    private fun song() = StemStore.list(app).first().id

    private fun openCut() {
        begin(SeatOptions.parse(mapOf("lang" to "es", "device" to "phone", "font" to "100", "mode" to "simple", "main" to "gauche")).first)
        amenee("remix", File(out, "journal.txt"))
        tap("remix_download_thumb")
        runCatching { tap("remix_model_ask_ok") }
        repeat(80) { if (SeparationJob.state.value is SeparationState.Done) return@repeat; act("attends 1") }
        runCatching { tap("remix_pick_thumb") }
        repeat(80) { if (SeparationJob.state.value is SeparationState.Done) return@repeat; act("attends 1") }
        tap("cut_song_${song()}")
        repeat(20) { act("attends 1") }
    }

    private fun send() {
        val d = app.resources.displayMetrics.density
        act("glisse ${196 * d} ${600 * d} ${196 * d} ${300 * d}")
        act("attends 1")
        tap("cut_to_pads")
        repeat(10) { act("attends 1") }
    }

    @Test
    fun staleMessageKeepsTheInstructionOnTop() {
        assumeTrue(System.getProperty("ps.human.dir").isNullOrBlank())
        openCut()
        send()
        assertTrue("no message after « En los pads »:\n" + screenLines().joinToString("\n"), node("cut_message") != null)
        // Out of the cut and back in « Un trozo »: the message is still there (it lives until « OK »).
        tap("cut_close")
        repeat(5) { act("attends 1") }
        if (reachTag("cut_song_${song()}") != null) tap("cut_song_${song()}") else tap("remix_cut_thumb")
        repeat(20) { act("attends 1") }
        assertTrue("the message from before is gone (the case is not played):\n" + screenLines().joinToString("\n"), node("cut_message") != null)
        assertTrue("« Coloca Inicio y Fin » not whole on arrival with a message from before:\n" + screenLines().joinToString("\n"), hintWhole())
        // A new message, while the screen is open: the column follows it down (tour 7, lot 2 D3 kept).
        send()
        val ok = runCatching { tap("cut_message_ok") }.isSuccess
        assertTrue("no « OK » under the message", ok)
        repeat(3) { act("attends 1") }
        send()
        for (t in listOf("cut_message", "cut_to_pads", "cut_to_live", "cut_to_library", "cut_to_track"))
            assertTrue("$t cut after a new message:\n" + screenLines().joinToString("\n"), drawnEntire(t))
    }
}
