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
 * Tour 6, control seat Da-10 (Japanese, phone, techno « Facile », ch. 3): with its cells to touch on the grid the
 * panel keeps a low room for the words (T4), and « 続きを読む » (« Lire la suite ») only moved them by a line or two in
 * that 3-line window: no visible effect. One touch on « Lire la suite » shows the whole instruction.
 * (Native graphics: the legacy text measure gives the instruction one short line.)
 */
@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w392dp-h852dp-port-hdpi")
class Tour6TutoReadAllTest {
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
    fun readOnShowsTheWholeInstructionInOneTouch() {
        val target = TutoTarget.of("techno")!!
        h.start(simple = true, lang = Lang.JA, build = {
            session.edit { target.blank() }
            PrefsTutoStore(h.app).save(projectId, TutoProgress("techno", TutoLevel.Facile, current = "clap_do"))
        })
        await("clap: its dotted cells and « Lire la suite »") { TutoHints.cells?.second?.isNotEmpty() == true && has("tuto_text") && has("tuto_text_more") }
        settle()
        val before = node("tuto_text")
        assertTrue("the instruction already whole (${before.boundsInRoot.height} of ${before.size.height} px): no lower room here", before.boundsInRoot.height < before.size.height - 1f)
        compose.onNodeWithTag("tuto_text_more", useUnmergedTree = true).performClick()
        settle()
        val text = node("tuto_text")
        val seen = text.boundsInRoot
        assertTrue("after one « Lire la suite »: ${seen.height} px of the instruction's ${text.size.height} in sight", seen.height >= text.size.height - 1f)
    }

    /** Control seat Da-11: the whole instruction shown, « 続きを読む » stayed (the words' plain lines were still under it). */
    @Test
    fun nothingIsLeftToReadOnAfterThatTouch() {
        val target = TutoTarget.of("techno")!!
        h.start(simple = true, lang = Lang.JA, build = {
            session.edit { target.blank() }
            PrefsTutoStore(h.app).save(projectId, TutoProgress("techno", TutoLevel.Facile, current = "clap_do"))
        })
        await("clap: its dotted cells and « Lire la suite »") { TutoHints.cells?.second?.isNotEmpty() == true && has("tuto_text") && has("tuto_text_more") }
        settle()
        compose.onNodeWithTag("tuto_text_more", useUnmergedTree = true).performClick()
        settle()
        assertTrue("« Lire la suite » still offered after the touch that opens the words", !has("tuto_text_more"))
    }
}
