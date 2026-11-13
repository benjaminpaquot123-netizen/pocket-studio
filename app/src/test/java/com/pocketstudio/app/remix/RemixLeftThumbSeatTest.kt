package com.pocketstudio.app.remix

import android.app.Application
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.human.BenchWindows
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
import java.io.File

/**
 * Seat tour-6 B a (es, phone, font 100, Mode simple, left hand): the Remix cut screen, walked whole by the bench.
 * The acceptance phrases A2..A8 of that seat, each proven in its conditions. Red before the fixes, green after.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = HumanTester.PHONE)
class RemixLeftThumbSeatTest : HumanTester("remix2") {
    override val remixTimeScale = 0.25
    private val app: Application get() = ApplicationProvider.getApplicationContext()
    private val out = File("build/remix2").apply { mkdirs() }
    private val LEFT = SeatOptions.Hand.LEFT

    private fun node(tag: String) = compose.onAllNodesWithTag(tag).fetchSemanticsNodes().firstOrNull()

    /** The first node tagged [tag] is drawn whole (not clipped, not off an edge): its words are not cut. */
    private fun whole(tag: String): Boolean = node(tag)?.let { shownWhole(it) } == true

    /** The left thumb can touch the node tagged [tag] where it is now (no scroll, no grip change). */
    private fun reachableNow(tag: String): Boolean {
        val n = node(tag) ?: return false
        val m = app.resources.displayMetrics
        val p = BenchWindows.drawnPosition(n, m)
        val b = BenchWindows.drawnBounds(n, m)
        val mid = p.x + n.size.width / 2f to p.y + n.size.height / 2f
        return SeatOptions.nearestReachable(
            LEFT, SeatOptions.Box(p.x, p.y, p.x + n.size.width, p.y + n.size.height),
            SeatOptions.Box(b.left, b.top, b.right, b.bottom), mid, m.widthPixels, m.heightPixels, m.density,
        ) != null
    }

    private fun countText(text: String): Int =
        compose.onAllNodes(hasText(text, substring = true)).fetchSemanticsNodes().size

    private fun tap(tag: String): String {
        val at = reachTag(tag) ?: error("no $tag on:\n" + screenLines().joinToString("\n"))
        return act("touche ${at.first} ${at.second}")
    }

    private fun seat(): SeatOptions =
        SeatOptions.parse(mapOf("lang" to "es", "device" to "phone", "font" to "100", "mode" to "simple", "main" to "gauche")).first

    /** Reaches the Remix with a song separated; returns its set id. */
    private fun separate(): String {
        begin(seat())
        amenee("remix", File(out, "journal.txt"))
        tap("remix_download_thumb")
        runCatching { tap("remix_model_ask_ok") }
        repeat(80) { if (SeparationJob.state.value is SeparationState.Done) return@repeat; act("attends 1") }
        runCatching { tap("remix_pick_thumb") }
        repeat(80) { if (SeparationJob.state.value is SeparationState.Done) return@repeat; act("attends 1") }
        return StemStore.list(app).first().id
    }

    private fun openCut(id: String) {
        tap("cut_song_$id")
        repeat(20) { act("attends 1") }
    }


    @Test
    fun a2_stemChipsReachableWithLeftThumb() {
        assumeTrue(System.getProperty("ps.human.dir").isNullOrBlank())
        openCut(separate())
        // Voz, Batería, Bajo, Resto: each touchable by the left thumb where it is (no scroll, no grip change).
        for (stem in listOf(3, 0, 1, 2)) assertTrue("cut_stem_$stem is out of the left thumb", reachableNow("cut_stem_$stem"))
    }

    @Test
    fun a5_spanToolsWholeOnArrival() {
        assumeTrue(System.getProperty("ps.human.dir").isNullOrBlank())
        openCut(separate())
        // On arrival (« Un trozo »): Inicio aquí, Fin aquí, Escuchar el trozo whole and touchable, no scroll.
        assertTrue("cut_start cut off", whole("cut_start"))
        assertTrue("cut_end cut off", whole("cut_end"))
        assertTrue("cut_span_play cut off", whole("cut_span_play"))
    }

    @Test
    fun a3_cutHintWholeNotUnderThumb() {
        assumeTrue(System.getProperty("ps.human.dir").isNullOrBlank())
        val id = separate()
        // After separation, before cutting: « Cortar lo que quiero » and its help line whole, nothing under the thumb bar.
        assertTrue("cut_song_$id cut off", whole("cut_song_$id"))
        assertTrue("cut_song_hint_$id cut off (under the thumb bar)", whole("cut_song_hint_$id"))
    }

    @Test
    fun a8_oneCutButtonInProject() {
        assumeTrue(System.getProperty("ps.human.dir").isNullOrBlank())
        val id = separate()
        // « Cortar lo que quiero » puts the song in the project (its board loads) and opens the cut screen; close it,
        // and the Remix page shows the project panel (seat screen 41).
        openCut(id)
        repeat(30) { if (node("remix_project") != null) return@repeat; runCatching { tap("cut_close") }; act("attends 1") }
        // One « Cortar la canción » on the panel, not two.
        assertTrue("the project panel never came:\n" + screenLines().joinToString("\n"), node("remix_project") != null)
        assertEquals("two « Découper la chanson » buttons", 1, countText("Cortar la canción"))
    }
}
