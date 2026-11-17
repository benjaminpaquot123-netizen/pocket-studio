package com.pocketstudio.app.tuto

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.pocketstudio.app.nav.AppHarness
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.model.tuto.TutoLevel
import com.pocketstudio.model.tuto.TutoProgress
import com.pocketstudio.model.tuto.TutoTarget
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

/**
 * Tour 7, control « tuto2 »: after « Lire la suite » the panel, unfolded, covered the grid's cells to touch, with no
 * way back but folding the whole panel. Unfolded, it shows « Réduire »: one touch gives the cells their room back
 * (the words in their low room again, « Lire la suite » under them).
 */
@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w392dp-h852dp-port-hdpi")
class Tour7TutoReduceTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()
    private val h = AppHarness(compose)

    @After
    fun end() {
        TutoHints.reset()
        h.finish()
    }

    private fun idle(ms: Long = 50) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))
    private fun has(tag: String) = compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    private fun node(tag: String) = compose.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode()
    private fun settle() = repeat(20) { idle(100); compose.mainClock.advanceTimeBy(100); compose.waitForIdle() }

    private fun await(what: String, ok: () -> Boolean) {
        val end = System.currentTimeMillis() + 30_000
        while (!ok()) {
            idle(50)
            compose.mainClock.advanceTimeBy(50)
            compose.waitForIdle()
            check(System.currentTimeMillis() < end) { "$what: not in time" }
        }
    }

    @Test
    fun reduceGivesTheCellsTheirRoomBack() {
        val target = TutoTarget.of("techno")!!
        h.start(simple = true, lang = Lang.JA, build = {
            session.edit { target.blank() }
            PrefsTutoStore(h.app).save(projectId, TutoProgress("techno", TutoLevel.Facile, current = "clap_do"))
        })
        await("clap: its dotted cells and « Lire la suite »") { TutoHints.cells?.second?.isNotEmpty() == true && has("tuto_text") && has("tuto_text_more") }
        settle()
        val low = node("tuto_panel").boundsInRoot.height
        compose.onNodeWithTag("tuto_text_more", useUnmergedTree = true).performClick()
        settle()
        val open = node("tuto_panel").boundsInRoot.height
        assertTrue("the test checks nothing: the panel did not grow ($low to $open px)", open > low + 1f)
        assertTrue("no « Réduire » on the unfolded panel", has("tuto_text_less"))
        compose.onNodeWithTag("tuto_text_less", useUnmergedTree = true).performClick()
        settle()
        val back = node("tuto_panel").boundsInRoot.height
        assertTrue("after « Réduire » the panel is $back px high, $low before « Lire la suite »", back <= low + 1f)
        assertTrue("no « Lire la suite » after « Réduire »", has("tuto_text_more"))
        assertTrue("« Réduire » still shown in the low room", !has("tuto_text_less"))
    }
}
