package com.pocketstudio.app.tuto

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.pocketstudio.app.nav.AppHarness
import com.pocketstudio.model.tuto.TutoChecks
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
import org.robolectric.annotation.Config
import java.time.Duration

/**
 * Tour 6, seats Bb-7 and Bb-8 (H5, H7, decided): the techno « Pro » steps on the track's sound (bass_cutoff,
 * lead_knobs) start while the notes page is scrolled to its grid; « Régler le son de la piste ↓ », at the top of
 * that page, was out of sight (five gestures without finding it). The step brings it into view by itself: whole,
 * on the screen and above the tutorial's panel, with no gesture.
 */
abstract class SoundButtonInSight {
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

    /** The course made up to [before] (its steps done as the course does them), the tutorial on it, « Je le fais », then on to [id]. */
    protected fun theButtonIsInSight(before: String, id: String, scale: Float) {
        val target = TutoTarget.of("techno")!!
        val steps = TutoCourse.of("techno")!!.chaptersFor(TutoLevel.Pro).flatMap { it.steps }
        h.start(simple = false, scale = scale, build = {
            var p = target.blank()
            for (s in steps) {
                if (s.id == before) break
                if (!TutoChecks.done(s, p, target)) p = TutoChecks.show(s, p, target) ?: p
            }
            session.edit { p }
            PrefsTutoStore(h.app).save(projectId, TutoProgress("techno", TutoLevel.Pro, current = before))
        })
        await("$before: the panel") { has("tuto_need") || has("tuto_do") }
        compose.onNodeWithTag(if (has("tuto_need")) "tuto_need" else "tuto_do").performClick()
        await("$before: the notes") { has("piano_roll") }
        settle()
        // On to the step on the track's sound (« Suivant », twice when the step is said undone).
        fun current() = PrefsTutoStore(h.app).load(h.vm.projectId)?.current
        val seen = ArrayList<String?>()
        repeat(4) {
            seen += current()
            if (current() != id) {
                val next = listOf("tuto_next_big", "tuto_next", "tuto_next_folded").firstOrNull { has(it) }
                    ?: listOf("tuto_fold", "tuto_toggle").firstOrNull { has(it) }
                next?.let { compose.onNodeWithTag(it, useUnmergedTree = true).performClick() }
                settle()
            }
        }
        assertTrue("not at $id: $seen", current() == id)
        val b = node("synth_to_sound").boundsInRoot
        val panel = if (has("tuto_panel")) node("tuto_panel").boundsInRoot.top else Float.MAX_VALUE
        val top = node("synth_to_sound").let { n -> generateSequence(n.parent) { it.parent }.map { it.boundsInRoot.top }.maxOrNull() } ?: 0f
        assertTrue("« Régler le son de la piste ↓ » at $b, its page from $top, the panel at $panel", b.height > 1f && b.top >= top - 1f && b.top >= 0f && b.bottom <= panel + 1f)
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w600dp-h1006dp-port-hdpi", fontScale = 1.3f)
class Tour6TutoSoundButtonInSightTabletTest : SoundButtonInSight() {
    @Test fun bassCutoff() = theButtonIsInSight("bass_end", "bass_cutoff", 1f)
    @Test fun leadKnobs() = theButtonIsInSight("lead_do", "lead_knobs", 1f)
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w392dp-h852dp-port-hdpi")
class Tour6TutoSoundButtonInSightPhoneTest : SoundButtonInSight() {
    @Test fun bassCutoff() = theButtonIsInSight("bass_end", "bass_cutoff", 1f)
    @Test fun leadKnobs() = theButtonIsInSight("lead_do", "lead_knobs", 1f)
}
