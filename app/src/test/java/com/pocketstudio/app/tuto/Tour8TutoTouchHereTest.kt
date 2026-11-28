package com.pocketstudio.app.tuto

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.pocketstudio.app.nav.AppHarness
import com.pocketstudio.model.Note
import com.pocketstudio.model.NoteClip
import com.pocketstudio.model.tuto.TutoLevel
import com.pocketstudio.model.tuto.TutoProgress
import com.pocketstudio.model.tuto.TutoTarget
import com.pocketstudio.model.track
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

/**
 * Tour 8, lot T0a (control p8b, seat a V1; seat b in ja): the house bass, its low and high « La » 13 rows apart, on the
 * seats' phone (360 dp), the tutorial's panel open. « Je touche une fois « Touche ici » et je vois les notes en pointillé
 * de la mesure, sans flèche ↓ ni second « Touche ici » »: one touch shows every dotted note of the bar whole above the
 * panel, the line has no arrow and says as many notes as are in sight, and touching again does not flip the roll back.
 */
abstract class Tour8TutoTouchHere {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()
    private val h = AppHarness(compose)

    @After
    fun end() {
        TutoHints.cells = null
        TutoHints.notes = null
        h.finish()
    }

    private fun idle(ms: Long = 50) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))
    private fun has(tag: String) = compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    private fun node(tag: String) = compose.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode()
    private fun text(tag: String) = node(tag).config[SemanticsProperties.Text].joinToString()
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

    private fun open(level: TutoLevel, simple: Boolean) {
        val target = TutoTarget.of("house")!!
        h.start(simple = simple, build = {
            session.edit { target.blank() }
            PrefsTutoStore(h.app).save(projectId, TutoProgress("house", level, current = "bass_do"))
        })
        await("the panel") { has("tuto_need") }
        compose.onNodeWithTag("tuto_need").performClick()
        await("the dotted notes") { has("piano_roll") && TutoHints.notes?.second?.isNotEmpty() == true }
        settle()
    }

    /** The dotted notes left of the bar in sight, and the window rows of each, with the room above the panel. */
    private class Seen(val top: Float, val bottom: Float, val rows: Map<Note, ClosedFloatingPointRange<Float>>) {
        fun whole(n: Note) = rows.getValue(n).let { it.start >= top - 1f && it.endInclusive <= bottom + 1f }
    }

    private fun seen(): Seen {
        val (track, dotted) = TutoHints.notes!!
        val written = h.vm.project.value.track(track).notes.notes
        val left = dotted.filter { d -> written.none { it.pitch == d.pitch && it.start == d.start } }
        val canvas = node("piano_roll")
        val view = node("roll_view").boundsInRoot
        val rowPx = canvas.size.height / (108 - 24 + 1f)
        val stepPx = canvas.size.width / h.vm.project.value.track(track).notes.steps.toFloat()
        val x0 = canvas.positionInRoot.x
        val y0 = canvas.positionInRoot.y
        val inTime = left.filter { n -> (x0 + n.start / NoteClip.TICKS_PER_STEP.toFloat() * stepPx).let { it >= view.left && it + stepPx <= view.right + 1f } }
        return Seen(view.top, minOf(view.bottom, node("tuto_panel").boundsInRoot.top), inTime.associateWith { n -> (y0 + (108 - n.pitch) * rowPx).let { it..it + rowPx } })
    }

    private fun touchHere(level: TutoLevel, simple: Boolean) {
        open(level, simple)
        val pitches = TutoHints.notes!!.second.map { it.pitch }
        assertTrue("the house bass has its two « La » an octave apart: $pitches", pitches.max() - pitches.min() >= 12)
        val views = ArrayList<List<ClosedFloatingPointRange<Float>>>()
        repeat(3) { n ->
            compose.onNodeWithTag("tuto_notes_left").performClick()
            settle()
            val s = seen()
            val said = text("tuto_notes_left_text")
            val hidden = s.rows.keys.filterNot { s.whole(it) }
            assertTrue("touch ${n + 1}: no dotted note of the bar in sight", s.rows.isNotEmpty())
            assertTrue(
                "touch ${n + 1}: dotted notes out of the rows above the panel (${s.top}..${s.bottom}): " + hidden.map { "${it.pitch} ${s.rows[it]}" },
                hidden.isEmpty(),
            )
            assertTrue("touch ${n + 1}: the line still points (« $said »)", !said.contains("↓") && !said.contains("↑"))
            val count = Regex("\\d+").find(said)?.value?.toInt()
            assertEquals("touch ${n + 1}: the line says « $said », ${s.rows.size} dotted notes in sight", s.rows.size, count)
            views += s.rows.values.sortedBy { it.start }
        }
        assertTrue("touching again moves the roll: $views", views[1] == views[0] && views[2] == views[1])
    }

    @Test
    fun simpleOneTouchShowsTheBar() = touchHere(TutoLevel.Facile, simple = true)

    @Test
    fun proOneTouchShowsTheBar() = touchHere(TutoLevel.Pro, simple = false)
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp-port-hdpi", fontScale = 1.0f)
class Tour8TutoTouchHereTest : Tour8TutoTouchHere()

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp-port-hdpi", fontScale = 1.3f)
class Tour8TutoTouchHereLargeTextTest : Tour8TutoTouchHere()
