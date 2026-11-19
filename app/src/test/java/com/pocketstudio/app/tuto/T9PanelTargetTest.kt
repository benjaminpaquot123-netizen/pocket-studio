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
 * Tour 9, defects D02/D13/D14 (seats t9-44, t9-51, t9-65; no bench): at each tuto step that asks for a touch, the target is
 * in sight above the tuto's panel (a whole row of the grid, the piano roll's notes, every strip's ▶), with nothing folded
 * by hand. Big font (200 %) on a phone, any language; the solo ▶ on a tablet at 100 %, the panel unfolded to read.
 */
@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
class T9PanelTargetTest {
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
    private fun panelTop() = compose.onNodeWithTag("tuto_panel").fetchSemanticsNode().boundsInRoot.top

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
        TutoTestHooks.kitOffer = false
        h.start(simple = simple, lang = lang, build = {
            val built = TutoAnchor.built(target.blank(), style, level, chapter)!!
            session.edit { built }
            PrefsTutoStore(h.app).save(projectId, TutoProgress(style, level, done = done, current = id))
        })
        settle(2000)
    }

    /** The step's own button (« Aller à la partie… », « Je le fais »), as a user goes to do it. */
    private fun go() {
        for (t in listOf("tuto_need", "tuto_do")) if (has(t)) { compose.onNodeWithTag(t).performClick(); settle(3000) }
    }

    /** A whole row of the step grid above the panel: a cell wholly in sight and every cell on its line too. */
    private fun assertRowAbovePanel(what: String) {
        val top = panelTop()
        val cells = tagged(Regex("step_\\d+"))
        val seen = cells.filter { whole(it) && it.boundsInRoot.bottom <= top + 1f }
        val row = seen.firstOrNull()?.let { first -> cells.filter { kotlin.math.abs(it.positionInRoot.y - first.positionInRoot.y) < 2f } }
        assertTrue(
            "$what: no whole row of cells above the panel (panel at $top; cells ${cells.size}, " +
                "whole above it ${seen.size}; first ${cells.firstOrNull()?.boundsInRoot})",
            row != null && row.all { whole(it) && it.boundsInRoot.bottom <= top + 1f },
        )
    }

    @Test
    fun fillCellsAboveThePanelIn360At200Japanese() {
        // t9-65: synthwave Pro, chapter 12 « fill »: the rhythm grid kept 23 dp, no cell in sight.
        start("w360dp-h780dp-port-hdpi", 2f, Lang.JA, false, "synthwave", TutoLevel.Pro, 12, 0)
        go()
        assertRowAbovePanel("ch. 12 fill (ja, 360 dp, 200 %)")
    }

    @Test
    fun padGridAboveThePanelAt200Chinese() {
        // Pads: afro house Pro, chapter 8 « variation » (Drop 2's drums): the cells' top pixel only above the panel.
        start("w392dp-h852dp-port-hdpi", 2f, Lang.ZH_CN, false, "afro_house", TutoLevel.Pro, 8, 3)
        go()
        assertRowAbovePanel("ch. 8 variation (zh, 392 dp, 200 %)")
    }

    @Test
    fun melodyRollAboveThePanelAt200Dutch() {
        // t9-44: liquid DnB Pro, chapter 8 « lead_do »: the piano roll wholly under the panel, « Melodie · maat 1/8 » only.
        start("w392dp-h852dp-port-hdpi", 2f, Lang.NL, false, "liquid_dnb", TutoLevel.Pro, 8, 1)
        go()
        val top = panelTop()
        val roll = tagged(Regex("piano_roll")).firstOrNull()
        val seen = roll?.boundsInRoot?.let { minOf(it.bottom, top) - it.top } ?: 0f
        val least = with(compose.density) { 96f * density }
        assertTrue("lead_do (nl, 392 dp, 200 %): the piano roll shows ${seen}px above the panel at $top (roll ${roll?.boundsInRoot})", seen >= least)
    }

    @Test
    fun soloButtonsAboveThePanelAfterTheMixTab() {
        // t9-51: neurofunk Facile, chapter 14, tablet at 100 %, German: the panel unfolded (read), « Mix » touched by hand:
        // the strips' ▶ under the panel for the whole chapter.
        start("w600dp-h1006dp-port-hdpi", 1f, Lang.DE, true, "neurofunk", TutoLevel.Facile, 14, 0)
        assertTrue("the panel starts unfolded", has("tuto_do"))
        compose.onNodeWithTag("tab_simple_Mix").performClick()
        settle(2000)
        val top = panelTop()
        val solos = tagged(Regex("solo_\\d+|mix_alone_\\w+"))
        assertTrue("no ▶ on the Mix tab", solos.isNotEmpty())
        val under = solos.filter { !whole(it) || it.boundsInRoot.bottom > top + 1f }
        assertTrue("ch. 14 (de, tablet, 100 %): ▶ under the panel at $top: ${under.map { it.boundsInRoot }}", under.isEmpty())
    }
}
