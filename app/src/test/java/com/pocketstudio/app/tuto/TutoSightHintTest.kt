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
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

/**
 * Tour 6, repairs: the tutorial's line over the piano roll says « plus haut ↑ » / « plus bas ↓ » only when none
 * of the dotted notes of the bar in sight is drawn whole above the tutorial's panel. The capture `tuto_app_bass`
 * said « Encore 4 notes en pointillé, plus haut ↑ » with three of them in sight, the first to touch among them.
 * A phone of 360 dp, its text at 100 % and at 130 %, the panel open.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp-port-hdpi")
class TutoSightHintTest {
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

    /** The bass step of [style]'s tutorial (Facile, Mode simple), « Je le fais » touched: the roll and its dotted notes. */
    private fun openBass(style: String) {
        val target = TutoTarget.of(style)!!
        h.start(simple = true, build = {
            session.edit { target.blank() }
            PrefsTutoStore(h.app).save(projectId, TutoProgress(style, TutoLevel.Facile, current = "bass_do"))
        })
        await("the panel") { has("tuto_need") }
        compose.onNodeWithTag("tuto_need").performClick()
        await("the dotted notes") { has("piano_roll") && TutoHints.notes?.second?.isNotEmpty() == true }
        settle()
    }

    /** The dotted notes left, in the order the line brings them (PianoRoll's). */
    private fun leftNotes(): List<Note> {
        val (track, dotted) = TutoHints.notes!!
        val written = h.vm.project.value.track(track).notes.notes
        return dotted.filter { d -> written.none { it.pitch == d.pitch && it.start == d.start } }.sortedWith(compareBy({ it.start }, { -it.pitch }))
    }

    /** Where a dotted note is drawn (window y of its row) and whether its start is in the bar in sight. */
    private class Seen(val top: Float, val bottom: Float, val rows: Map<Note, ClosedFloatingPointRange<Float>>, val inTime: Set<Note>) {
        fun whole(n: Note) = rows.getValue(n).let { it.start >= top - 1f && it.endInclusive <= bottom + 1f }
    }

    private fun seen(): Seen {
        val track = TutoHints.notes!!.first
        val left = leftNotes()
        val canvas = node("piano_roll")
        val view = node("roll_view").boundsInRoot
        val panelTop = node("tuto_panel").boundsInRoot.top
        // The roll's rows: C1 (24) to C8 (108), PianoRoll's LOW and HIGH (no note of these tracks lies past them).
        val rowPx = canvas.size.height / (108 - 24 + 1f)
        val stepPx = canvas.size.width / h.vm.project.value.track(track).notes.steps.toFloat()
        val rows = left.associateWith { n -> (canvas.positionInRoot.y + (108 - n.pitch) * rowPx).let { it..it + rowPx } }
        val inTime = left.filter { n -> (canvas.positionInRoot.x + n.start / NoteClip.TICKS_PER_STEP.toFloat() * stepPx).let { it >= view.left && it < view.right } }.toSet()
        return Seen(view.top, minOf(panelTop, view.bottom), rows, inTime)
    }

    /**
     * The notes of the bar in sight written one after the other, as a beginner does (those he sees first): at each
     * of them, the line points up or down exactly when none of the bar's dotted notes left is drawn whole above the
     * panel. The roll opens with notes to touch in sight; both cases must have been met.
     */
    private fun theArrowIsForNotesOutOfSight(style: String) {
        openBass(style)
        assertTrue("the panel is open", has("tuto_panel"))
        var inSight = 0
        var hidden = 0
        val log = mutableListOf<String>()
        repeat(leftNotes().size) {
            val s = seen()
            val here = leftNotes().filter { it in s.inTime }
            if (here.isEmpty()) return@repeat
            val said = text("tuto_notes_left_text")
            val next = here.firstOrNull { s.whole(it) } ?: here.first()
            val row = s.rows.getValue(next)
            val what = "note ${next.pitch}@${next.start / NoteClip.TICKS_PER_STEP} drawn at $row, rows in sight ${s.top}..${s.bottom}, the line says « $said »"
            log += what
            if (s.whole(next)) {
                inSight++
                assertTrue("a dotted note is in sight, no arrow expected: $what", !said.contains("↑") && !said.contains("↓"))
            } else {
                hidden++
                val arrow = if (row.start < s.top) "↑" else "↓"
                assertTrue("no dotted note in sight, « $arrow » expected: $what", said.contains(arrow))
            }
            if (log.size == 1) {
                // The note « Je le fais » asks for (the first one left, the one lit): itself whole in sight, not just another one.
                val asked = leftNotes().first()
                assertTrue("the roll opens with the note asked for (${asked.pitch}@${asked.start / NoteClip.TICKS_PER_STEP}, drawn at ${s.rows[asked]}) whole in sight: $what", asked in s.inTime && s.whole(asked))
            }
            h.vm.addNote(next)
            settle()
        }
        assertTrue("both cases met (in sight $inSight, hidden $hidden): $log", inSight > 0 && hidden > 0)
    }

    @Test
    fun jumpUpBassTheArrowIsOnlyForNotesOutOfSight() = theArrowIsForNotesOutOfSight("jump_up")

    @Test
    @Config(fontScale = 1.3f)
    fun jumpUpBassTheArrowIsOnlyForNotesOutOfSightAt130() = theArrowIsForNotesOutOfSight("jump_up")

    @Test
    fun houseBassTheArrowIsOnlyForNotesOutOfSight() = theArrowIsForNotesOutOfSight("house")

    @Test
    @Config(fontScale = 1.3f)
    fun houseBassTheArrowIsOnlyForNotesOutOfSightAt130() = theArrowIsForNotesOutOfSight("house")
}
