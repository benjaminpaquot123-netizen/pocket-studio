package com.pocketstudio.app.tuto

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.pocketstudio.app.human.TutoAnchor
import com.pocketstudio.app.nav.AppHarness
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.model.tuto.TutoCourse
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
import java.time.Duration

/**
 * Tour 9, defect D14 (seats t9-44, t9-05, t9-68, t9-69; no bench): one touch on « Lire la suite » shows the step's whole
 * instruction (not a line or three more), the panel's buttons and the app's tabs still whole under it.
 */
@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
class T9ReadMoreTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()
    private val h = AppHarness(compose)

    @After
    fun end() {
        org.robolectric.RuntimeEnvironment.setFontScale(1f)
        TutoTestHooks.kitOffer = true
        h.finish()
    }

    private fun idle(ms: Long = 50) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))
    private fun settle(ms: Long = 1500) { var t = 0L; while (t < ms) { idle(50); compose.mainClock.advanceTimeBy(50); compose.waitForIdle(); t += 50 } }
    private fun has(tag: String) = compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    private fun all(): List<SemanticsNode> {
        val out = ArrayList<SemanticsNode>()
        fun walk(n: SemanticsNode) { out += n; n.children.forEach(::walk) }
        walk(compose.onRoot(useUnmergedTree = true).fetchSemanticsNode())
        return out
    }
    private fun tagged(re: Regex) = all().filter { n -> n.config.getOrNull(SemanticsProperties.TestTag)?.let(re::matches) == true }
    private fun whole(n: SemanticsNode) = n.boundsInRoot.height >= n.size.height - 1f && n.boundsInRoot.width >= n.size.width - 1f && n.size.height > 0

    /** Starts the course of [style] at [level] on its [chapter]'s step [index], the steps before it done. */
    private fun start(q: String, font: Float, lang: Lang, simple: Boolean, style: String, level: TutoLevel, chapter: Int, index: Int) {
        org.robolectric.RuntimeEnvironment.setQualifiers(q)
        org.robolectric.RuntimeEnvironment.setFontScale(font)
        val target = TutoTarget.of(style)!!
        val chapters = TutoCourse.of(style)!!.chaptersFor(level)
        val ch = chapters[chapter - 1]
        val id = ch.steps[index].id
        val done = chapters.take(chapter - 1).flatMap { it.steps }.map { it.id }.toSet() + ch.steps.take(index).map { it.id }
        h.start(simple = simple, lang = lang, build = {
            val built = TutoAnchor.built(target.blank(), style, level, chapter)!!
            session.edit { built }
            PrefsTutoStore(h.app).save(projectId, TutoProgress(style, level, done = done, current = id))
        })
        settle(2000)
    }

    /** The words cut, one touch on « Lire la suite »: the whole instruction in sight, the panel's buttons and a tab whole. */
    private fun assertOneTouchReadsAll(what: String) {
        assertTrue("$what: the words are not cut (no « Lire la suite »)", has("tuto_text_more"))
        compose.onNodeWithTag("tuto_text_more", useUnmergedTree = true).performClick()
        settle(2000)
        val text = tagged(Regex("tuto_text")).single()
        assertTrue("$what: after one « Lire la suite » the instruction shows ${text.boundsInRoot.height}px of ${text.size.height}px", whole(text))
        val next = tagged(Regex("tuto_next|tuto_next_big|tuto_do|tuto_mark"))
        assertTrue("$what: the panel's buttons are cut (${next.map { it.boundsInRoot }})", next.isNotEmpty() && next.all(::whole))
        val tabs = tagged(Regex("tab_.+"))
        assertTrue("$what: no whole tab left under the panel", tabs.any(::whole))
    }

    @Test
    fun koreanWelcomeAt200On360() {
        // t9-05: acidcore, Facile, chapter 1, step 1, the kit offered: « 계속 읽기 » touched twice, nothing opened.
        start("w360dp-h780dp-port-hdpi", 2f, Lang.KO, true, "acidcore", TutoLevel.Facile, 1, 0)
        assertOneTouchReadsAll("acidcore ch.1 (ko, 360 dp, 200 %)")
    }

    @Test
    fun dutchChapterSevenAt200() {
        // t9-44: liquid DnB, Pro, chapter 7: « Verder lezen » touched, the instruction stayed cut to a line.
        start("w392dp-h852dp-port-hdpi", 2f, Lang.NL, false, "liquid_dnb", TutoLevel.Pro, 7, 0)
        assertOneTouchReadsAll("liquid_dnb ch.7 (nl, 392 dp, 200 %)")
    }

    @Test
    fun chineseTechnoAt200() {
        // t9-68: techno, Pro, chapters 1 to 5 at 200 %: « 继续阅读 » moved the words three lines at a time.
        start("w392dp-h852dp-port-hdpi", 2f, Lang.ZH_CN, false, "techno", TutoLevel.Pro, 1, 0)
        assertOneTouchReadsAll("techno ch.1 (zh, 392 dp, 200 %)")
    }
}
