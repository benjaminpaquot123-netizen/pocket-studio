package com.pocketstudio.app.tuto

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.pocketstudio.app.audit.LayoutScan
import com.pocketstudio.app.library.IndexState
import com.pocketstudio.app.library.PacksUi
import com.pocketstudio.app.nav.AppHarness
import com.pocketstudio.model.PackEntry
import com.pocketstudio.model.PackIndex
import com.pocketstudio.model.tuto.TutoCourse
import com.pocketstudio.model.tuto.TutoLevel
import com.pocketstudio.model.tuto.TutoProgress
import com.pocketstudio.model.tuto.TutoTarget
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

/**
 * Audit ultra round 1, n° 34 (seat A b: a senior at 200 % on a phone, the techno course, Facile): the folded line
 * says the step whole (it was « À toi : sur le pad « Clap », t… »); « Je le fais » brings the dotted cells
 * whole into sight under « Partie : A » (the first row was half under it, in its fade); a chapter's last step
 * done says « Bravo » with « Écouter mon morceau » before the next chapter; the kit's card never goes over
 * the top bar.
 */
@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h780dp")
class TutoBigFontTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()
    private val h = AppHarness(compose)
    private val style = "techno"
    private val target = TutoTarget.of(style)!!
    private val chapters = TutoCourse.of(style)!!.chaptersFor(TutoLevel.Facile)

    @Before
    fun bigFont() {
        org.robolectric.RuntimeEnvironment.setFontScale(2f)
    }

    @After
    fun end() {
        org.robolectric.RuntimeEnvironment.setFontScale(1f)
        TutoTestHooks.kitOffer = true
        TutoTestHooks.packs = null
        TutoTestHooks.player = null
        h.finish()
    }

    private fun idle(ms: Long = 50) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))

    private fun await(what: String, timeoutMs: Long = 30_000, ok: () -> Boolean) {
        val end = System.currentTimeMillis() + timeoutMs
        while (!ok()) {
            idle(50)
            compose.mainClock.advanceTimeBy(50)
            compose.waitForIdle()
            check(System.currentTimeMillis() < end) { "$what: not in time" }
        }
    }

    private fun has(tag: String) = compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    private fun click(tag: String) {
        val node = compose.onNodeWithTag(tag)
        runCatching { node.performScrollTo() }
        node.performClick()
        compose.waitForIdle()
    }
    private fun bounds(tag: String) = compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().first().boundsInRoot
    private fun progress(): TutoProgress = PrefsTutoStore(h.app).load(h.vm.projectId)!!

    /** The techno course opened on step [current], the steps before it done. */
    private fun open(current: String) {
        val flat = chapters.flatMap { it.steps }
        val done = flat.takeWhile { it.id != current }.map { it.id }.toSet()
        TutoTestHooks.kitOffer = false
        h.start(simple = true, build = {
            session.edit { target.blank() }
            PrefsTutoStore(h.app).save(projectId, TutoProgress(style, TutoLevel.Facile, done = done, current = current))
        })
        await("panel") { has("tuto_panel") }
        assertEquals("the font at 200 %", 2f, compose.density.fontScale, 0.01f)
    }

    @Test
    fun theFoldedLineSaysTheStepWholeAndTheDottedCellsComeWholeUnderThePartLine() {
        open("clap_do")
        if (has("tuto_need")) click("tuto_need")
        await("« Je le fais »") { has("tuto_do") }
        click("tuto_do")
        await("the dotted cells") { has("rhythm") && has("tuto_hint") }
        idle(600)
        compose.mainClock.advanceTimeBy(1_000)
        compose.waitForIdle()
        // The folded line: a long step's words on two lines at least (it said « À toi : sur le pad « Clap », t… »),
        // and « Lire » to unfold the rest.
        assertTrue(has("tuto_folded_text"))
        val density = h.app.resources.displayMetrics.density
        val words = compose.onAllNodes(androidx.compose.ui.test.hasAnyAncestor(androidx.compose.ui.test.hasTestTag("tuto_folded_text")) and
            androidx.compose.ui.test.hasText("À toi", substring = true), useUnmergedTree = true).fetchSemanticsNodes().first()
        val layout = mutableListOf<androidx.compose.ui.text.TextLayoutResult>().also { words.config[androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult].action!!(it) }.first()
        val problems = ArrayList<String>()
        if (layout.lineCount < 2) problems += "the folded step on ${layout.lineCount} line (${words.boundsInRoot}, ${layout.layoutInput.text.text.take(60)}, ${layout.size}, ellipsized ${layout.isLineEllipsized(0)})"
        if (!has("tuto_folded_more")) problems += "no « Lire » on the folded line"
        // n° 38 (seats A b, F a at 200 %): the mode's chip (« Sim… ») and where the song is (« mes. 1/8 » cut under the tracks).
        LayoutScan.scan(compose, density, setOf(LayoutScan.Kind.CUT))
            .filter { f -> listOf("Lire", "Simple", "mes.").any { it in f.toString() } }.forEach { problems += it.toString() }
        // Each dotted cell whole, below the part's line and its fade, above the panel.
        val line = bounds("part_header")
        val panelTop = bounds("tuto_panel").top
        val fade = 28f * density
        val cells = compose.onAllNodesWithTag("tuto_hint", useUnmergedTree = true).fetchSemanticsNodes()
        assertTrue(cells.isNotEmpty())
        val hidden = cells.mapNotNull { n ->
            val whole = androidx.compose.ui.geometry.Rect(n.positionInRoot, androidx.compose.ui.geometry.Size(n.size.width.toFloat(), n.size.height.toFloat()))
            when {
                whole.top < line.bottom + fade - 1f -> "under « Partie : A » or its fade ($whole, line at ${line.bottom})"
                whole.bottom > panelTop + 1f -> "under the panel ($whole, panel at $panelTop)"
                else -> null
            }
        }
        problems += hidden
        assertTrue("at 200 %: $problems", problems.isEmpty())
    }

    @Test
    fun aChaptersLastStepDoneSaysBravoAndOffersToListenBeforeTheNextChapter() {
        // « Le kick et le grondement »'s last step: the rumble.
        open("rumble_do")
        val rumble = chapters[1].steps.last()
        assertEquals("rumble_do", rumble.id)
        // The step done as the user does it (its cells), the panel folded by « Je le fais ».
        if (has("tuto_need")) click("tuto_need")
        await("« Je le fais »") { has("tuto_do") }
        click("tuto_do")
        await("the dotted cells") { TutoHints.cells != null }
        val (_, cells) = TutoHints.cells!!
        cells.sorted().forEach { h.vm.toggleStep(it) }
        await("ticked") { "rumble_do" in progress().done }
        // Folded: the line says the chapter is over, a touch shows « Bravo », never straight to the next chapter.
        await("the chapter's end, folded") { has("tuto_chapter_end_folded") }
        click("tuto_chapter_end_folded")
        assertEquals("went on to the next chapter by itself", "rumble_do", progress().current)
        await("« Bravo », the chapter done") { has("tuto_chapter_done") }
        assertTrue("« Écouter mon morceau »", has("tuto_play_mine"))
        assertTrue("« Chapitre suivant »", has("tuto_next_big"))
        click("tuto_next_big")
        await("the next chapter") { progress().current == chapters[2].steps.first().id }
    }

    @Test
    fun theKitsCardNeverGoesOverTheTopBar() {
        val packs = MutableStateFlow(PacksUi())
        TutoTestHooks.packs = packs
        val s = target.style
        val ids = (s.lines.filter { it.voice == null }.mapNotNull { s.kit[it.role] } + s.id).distinct()
        h.start(simple = true, build = {
            session.edit { target.blank() }
            PrefsTutoStore(h.app).save(projectId, TutoProgress(style, TutoLevel.Facile))
        })
        await("panel") { has("tuto_panel") }
        // The catalogue comes a moment later (as on the phone): the card then.
        packs.value = PacksUi(IndexState.Ready(PackIndex(ids.map { PackEntry(it, "Pack $it", "$it.zip", "a".repeat(64), 1_500_000, 12) })))
        await("the kit's card") { has("tuto_kit") }
        idle(300)
        compose.waitForIdle()
        val bar = bounds("top_bar")
        val card = bounds("tuto_kit")
        assertTrue("the kit's card over the top bar ($card, bar ends at ${bar.bottom})", card.top >= bar.bottom - 1f)
        assertTrue("its buttons", has("tuto_kit_download") && has("tuto_kit_later"))
        click("tuto_kit_later")
        await("the card gone") { !has("tuto_kit") }
        assertFalse(has("tuto_kit"))
        assertTrue("the course's panel back", has("tuto_panel"))
    }
}
