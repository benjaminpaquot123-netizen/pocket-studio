package com.pocketstudio.app.remix

import android.app.Application
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
 * Tour 7, lot 2 D2 (seat lotB c, #15 #20 #33): « en es, main gauche, phone à 100 %, Mode Simple, depuis la découpe, je
 * touche les puces « Frases » et la poignée « Fin » sans quitter la zone du pouce gauche ». Walked as the seat did.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = HumanTester.PHONE)
class CutLeftThumbD2Test : HumanTester("decoupe2") {
    override val remixTimeScale = 0.25
    private val app: Application get() = ApplicationProvider.getApplicationContext()
    private val out = File("build/decoupe2").apply { mkdirs() }
    private val LEFT = SeatOptions.Hand.LEFT

    private fun node(tag: String) = compose.onAllNodesWithTag(tag).fetchSemanticsNodes().firstOrNull()

    /** Where the left thumb touches the node tagged [tag] as it is now (no scroll, no grip change), or null. */
    private fun reachPoint(tag: String): Pair<Float, Float>? {
        val n = node(tag) ?: return null
        val m = app.resources.displayMetrics
        val p = BenchWindows.drawnPosition(n, m)
        val b = BenchWindows.drawnBounds(n, m)
        val mid = p.x + n.size.width / 2f to p.y + n.size.height / 2f
        return SeatOptions.nearestReachable(
            LEFT, SeatOptions.Box(p.x, p.y, p.x + n.size.width, p.y + n.size.height),
            SeatOptions.Box(b.left, b.top, b.right, b.bottom), mid, m.widthPixels, m.heightPixels, m.density,
        )
    }

    /** Where each tag is drawn, in dp, for the failure messages. */
    private fun where(vararg tags: String): String {
        val m = app.resources.displayMetrics
        return tags.joinToString("; ") { t ->
            val n = node(t) ?: return@joinToString "$t: none"
            val b = BenchWindows.drawnBounds(n, m)
            "$t: ${(b.left / m.density).toInt()},${(b.top / m.density).toInt()}-${(b.right / m.density).toInt()},${(b.bottom / m.density).toInt()}"
        }
    }

    private val all = arrayOf("cut_stem_3", "cut_stem_0", "cut_stem_1", "cut_stem_2", "cut_wave", "cut_span_end", "cut_auto", "cut_span_play", "cut_to_pads")

    private fun tap(tag: String): String {
        val at = reachTag(tag) ?: error("no $tag on:\n" + screenLines().joinToString("\n"))
        return act("touche ${at.first} ${at.second}")
    }

    private fun seat(): SeatOptions =
        SeatOptions.parse(mapOf("lang" to "es", "device" to "phone", "font" to "100", "mode" to "simple", "main" to "gauche")).first

    private fun openCut() {
        begin(seat())
        amenee("remix", File(out, "journal.txt"))
        tap("remix_download_thumb")
        runCatching { tap("remix_model_ask_ok") }
        repeat(80) { if (SeparationJob.state.value is SeparationState.Done) return@repeat; act("attends 1") }
        runCatching { tap("remix_pick_thumb") }
        repeat(80) { if (SeparationJob.state.value is SeparationState.Done) return@repeat; act("attends 1") }
        tap("cut_song_${StemStore.list(app).first().id}")
        repeat(20) { act("attends 1") }
    }

    /** #20: in « Frases », Voz / Batería / Bajo / Resto each within the left thumb where they are. */
    @Test
    fun d2a_phraseChipsWithinLeftThumb() {
        assumeTrue(System.getProperty("ps.human.dir").isNullOrBlank())
        openCut()
        tap("cut_auto")
        tap("cut_auto_1")
        repeat(10) { act("attends 1") }
        for (stem in listOf(3, 0, 1, 2)) assertTrue("cut_stem_$stem out of the left thumb in Frases (${where(*all)})", reachPoint("cut_stem_$stem") != null)
    }

    /** #33: in « Un trozo », the « Fin » bar grabbed within the left thumb and dragged moves. */
    @Test
    fun d2b_endBarGrabbedWithLeftThumb() {
        assumeTrue(System.getProperty("ps.human.dir").isNullOrBlank())
        openCut()
        val m = app.resources.displayMetrics
        val at = reachPoint("cut_span_end")
        assertTrue("the « Fin » bar is out of the left thumb (${where(*all)})", at != null)
        val before = BenchWindows.drawnBounds(node("cut_span_end")!!, m).left
        // To the left: the thumb's side, so the end stays in the curve whatever the span.
        act("glisse ${at!!.first} ${at.second} ${at.first - 80 * m.density} ${at.second}")
        repeat(3) { act("attends 1") }
        val after = BenchWindows.drawnBounds(node("cut_span_end")!!, m).left
        assertTrue("the « Fin » bar did not move ($before -> $after; ${where(*all)})", after < before - 20 * m.density)
    }
}
