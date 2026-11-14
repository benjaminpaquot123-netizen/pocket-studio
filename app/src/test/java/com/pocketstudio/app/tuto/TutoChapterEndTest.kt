package com.pocketstudio.app.tuto

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import com.pocketstudio.app.nav.AppHarness
import com.pocketstudio.model.tuto.TutoCourse
import com.pocketstudio.model.tuto.TutoLevel
import com.pocketstudio.model.tuto.TutoProgress
import com.pocketstudio.model.tuto.TutoTarget
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

/**
 * Audit ultra round 3, n° 30 (seats A c and E b, the Facile course): « Bravo, chapitre fini ! » while the chapters'
 * list said 2/3 (the example left by « Suivant » before it was heard); the course's end without « Écouter mon morceau »
 * (A c, screen 36).
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp")
class TutoChapterEndTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()
    private val h = AppHarness(compose)
    private val style = "retro_techno"
    private val target = TutoTarget.of(style)!!
    private val flat = TutoCourse.of(style)!!.chaptersFor(TutoLevel.Facile).flatMap { it.steps }

    @After
    fun end() {
        org.robolectric.RuntimeEnvironment.setFontScale(1f)
        TutoTestHooks.kitOffer = true
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
    private fun bounds(tag: String) = compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().first().boundsInRoot
    private fun click(tag: String) {
        val node = compose.onNodeWithTag(tag)
        runCatching { node.performScrollTo() }
        node.performClick()
        compose.waitForIdle()
    }

    private fun open(done: Set<String>, current: String) {
        TutoTestHooks.kitOffer = false
        h.start(simple = true, build = {
            session.edit { target.blank() }
            PrefsTutoStore(h.app).save(projectId, TutoProgress(style, TutoLevel.Facile, done = done, current = current))
        })
        await("panel") { has("tuto_panel") }
    }

    @Test
    fun theChaptersLastStepDoneWithTheExampleUnheardIsNotAFinishedChapter() {
        // Seat A c: « Suivant » touched while the example played (step 1 not ticked), the tempo set, then the key.
        open(done = setOf("tempo"), current = "key")
        h.vm.session.edit { it.copy(harmony = target.harmony) }
        await("« Bravo »") { has("tuto_bravo") }
        assertFalse("« Bravo, chapitre fini ! » with step 1 still to do", has("tuto_chapter_done"))
        assertTrue("what is left of the chapter, said", has("tuto_chapter_left"))
    }

    @Test
    fun theCoursesEndOffersToHearTheSong() {
        // Seat A c, screen 36: the last chapter, steps left, « Presque fini ! » and no way to hear the song.
        val last = flat.last()
        open(done = flat.drop(3).dropLast(1).map { it.id }.toSet(), current = last.id)
        h.vm.session.edit { p -> p.copy(drums = p.drums.fill(0, 4)) }
        await("the step") { has("tuto_next") || has("tuto_next_big") }
        if (has("tuto_next_big")) click("tuto_next_big") else { click("tuto_next"); if (!has("tuto_end")) click("tuto_next") }
        await("the course's end") { has("tuto_end") }
        assertTrue("« Écouter mon morceau » at the course's end", has("tuto_end_play"))
    }
}
