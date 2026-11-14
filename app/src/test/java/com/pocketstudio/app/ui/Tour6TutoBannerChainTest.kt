package com.pocketstudio.app.ui

import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.human.HumanTester
import com.pocketstudio.app.human.SeatOptions
import com.pocketstudio.model.allScenes
import com.pocketstudio.model.switchScene
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Tour 6, seat D a-8 (ja, phone, 100 %, Mode simple, the techno tutorial « Facile » played from chapter to chapter):
 * in chapter 12 the line over the grid kept « → スタジオのバスドラム » over the cymbal's grid (screens 155, 157). The
 * course is chained as the seat played it: chapter 11 left by « Suivant », the gap step turns the grid to the kick and
 * says so, then « Suivant » brings the cymbal step, which puts the cymbal on the grid. The line names the pad shown.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = HumanTester.PHONE)
class Tour6TutoBannerChainTest : HumanTester("tuto4") {
    private fun tap(tag: String): String {
        val at = reachTag(tag) ?: error("no $tag on:\n" + screenLines().joinToString("\n"))
        return act("touche ${at.first} ${at.second}")
    }

    private fun screen() = screenLines().joinToString("\n")

    private fun current(): String? =
        com.pocketstudio.app.tuto.PrefsTutoStore(ApplicationProvider.getApplicationContext()).load(rhythm()!!.projectId)?.current

    /** The tutorial's « Suivant », as shown (unfolded, folded or the big one), touched without letting time go by. */
    private fun next() {
        val nexts = listOf("tuto_next_big", "tuto_next", "tuto_next_folded")
        // The panel folded over the grid: unfolded first (its « ⌃ »), as a finger would.
        if (nexts.none { compose.onAllNodesWithTag(it).fetchSemanticsNodes().isNotEmpty() }) {
            listOf("tuto_toggle", "tuto_fold").firstOrNull { compose.onAllNodesWithTag(it).fetchSemanticsNodes().isNotEmpty() }
                ?.let { compose.onAllNodesWithTag(it)[0].performClick(); compose.waitForIdle() }
        }
        val tag = listOf("tuto_next_big", "tuto_next", "tuto_next_folded").firstOrNull { compose.onAllNodesWithTag(it).fetchSemanticsNodes().isNotEmpty() }
            ?: error("no « Suivant » at ${current()}:\n" + screen())
        compose.onAllNodesWithTag(tag)[0].performClick()
        compose.waitForIdle()
    }

    @Test
    fun theLineNamesThePadShownWhenTheCourseGoesOn() {
        assumeTrue(System.getProperty("ps.human.dir").isNullOrBlank())
        begin(SeatOptions.parse(mapOf("lang" to "ja", "device" to "phone", "font" to "100", "mode" to "simple")).first)
        tap("welcome_learn"); runCatching { tap("mode_card_simple") }; tap("tuto_style_techno")
        repeat(3) { act("attends 1") }
        // Chapter 11 (« Range tes parties »), then on by « Suivant » as the seat went: chapter 12's gap step.
        tap("tuto_chapters")
        fun inList(tag: String) = compose.onAllNodesWithTag(tag)[0].performScrollTo().performClick().also { compose.waitForIdle() }
        // The seat's level: « Facile » (the list offers « Facile » back when the course is in Pro).
        if (compose.onAllNodesWithTag("tuto_to_easy").fetchSemanticsNodes().isNotEmpty()) inList("tuto_to_easy")
        if (compose.onAllNodesWithTag("tuto_chapter_10").fetchSemanticsNodes().isEmpty()) tap("tuto_chapters")
        inList("tuto_chapter_10")
        act("attends 1")
        repeat(8) { if (current() != "gap") { next(); act("attends 1") } }
        assertTrue("not at the gap step (${current()}):\n" + screen(), current() == "gap")
        val v = rhythm()!!
        // The song as the course made it, chapter after chapter, up to here (the chapters' list jumps without making the
        // parts): each Facile step before the gap done as the course does it, then shown on its first part (« パート：A »).
        val target = com.pocketstudio.model.tuto.TutoTarget.of("techno")!!
        var made = v.project.value
        for (s in com.pocketstudio.model.tuto.TutoCourse.of("techno")!!.chaptersFor(com.pocketstudio.model.tuto.TutoLevel.Facile).flatMap { it.steps }) {
            if (s.id == "gap") break
            if (!com.pocketstudio.model.tuto.TutoChecks.done(s, made, target)) made = com.pocketstudio.model.tuto.TutoChecks.show(s, made, target) ?: made
        }
        v.session.edit { made.switchScene(0) }
        act("attends 1")
        // The gap is the Montée's: the user goes there (as the panel says, « Aller dans la partie »), where its crosses are.
        val montee = v.project.value.allScenes.indexOfFirst { it.label == "Montée" && !it.isEmpty }
        assertTrue("no Montée in the song (${v.project.value.allScenes.map { it.label }}):\n" + screen(), montee >= 0)
        if (v.project.value.scene != montee) v.selectScene(montee)
        act("attends 1")
        // What the tutorial's line over the grid says it just did (« → <pad> »), as the line reads it (TutoGridLine's status):
        // read at its source, the line itself may be under the panel's success card at that moment.
        fun said() = listOfNotNull(com.pocketstudio.app.tuto.TutoHints.banner?.takeIf { it.startsWith("→ ") }?.removePrefix("→ "))
        // A hit on the kick's last beat while another pad is shown: the grid turns to the kick, the line says so.
        v.selectPad(1)
        v.toggleHit(0, v.project.value.drums.steps - 1)
        compose.mainClock.advanceTimeBy(com.pocketstudio.app.tuto.TUTO_TURN_MS + 100)
        compose.waitForIdle()
        assertTrue("the grid not turned to the kick:\n" + screen(), v.transport.value.selectedPad == 0)
        assertTrue("no « → » said for the kick:\n" + screen(), said().isNotEmpty())
        // « Suivant » (twice when the step is said undone): the cymbal step puts the cymbal on the grid.
        repeat(2) { if (current() == "gap") next() }
        compose.mainClock.advanceTimeBy(100)
        compose.waitForIdle()
        assertTrue("not at the cymbal step (${current()}):\n" + screen(), current() == "crash")
        val shown = com.pocketstudio.app.rhythm.padName(v.project.value, v.transport.value.selectedPad)
        assertTrue("« → ${said().joinToString()} » over the grid of « $shown »:\n" + screen(), said().all { it == shown })
    }
}
