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
import com.pocketstudio.model.tuto.TutoCourse
import com.pocketstudio.model.tuto.TutoLevel
import com.pocketstudio.model.tuto.TutoProgress
import com.pocketstudio.model.tuto.TutoTarget
import com.pocketstudio.model.track
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

/**
 * Tour 6, lot 1 « tuto-grille » (docs/audits/ultra/sieges/tour-6/lots-correction.md): what the tutorial's
 * panel hides, on the seats' small phone (360 dp) with its text at 130 %, panel open, in Pro and in Simple.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp-port-hdpi", fontScale = 1.3f)
class TutoGrilleTest {
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

    /** The house tutorial opened on step [id] ([level]), its button « Je le fais » touched: the roll and its dotted notes. */
    private fun openNotesStep(id: String, level: TutoLevel, simple: Boolean) {
        val target = TutoTarget.of("house")!!
        assertTrue("$id in the $level course", TutoCourse.of("house")!!.chaptersFor(level).any { c -> c.steps.any { it.id == id } })
        h.start(simple = simple, build = {
            session.edit { target.blank() }
            PrefsTutoStore(h.app).save(projectId, TutoProgress("house", level, current = id))
        })
        await("$id: the panel") { has("tuto_need") }
        compose.onNodeWithTag("tuto_need").performClick()
        await("$id: its dotted notes") { has("piano_roll") && TutoHints.notes?.second?.isNotEmpty() == true }
        settle()
    }

    /** Where each dotted note left is drawn (window y of its top and bottom), with the room above the panel. */
    private class Seen(val panelTop: Float, val rollTop: Float, val rollBottom: Float, val rows: Map<Note, ClosedFloatingPointRange<Float>>, val inTime: Set<Note>) {
        fun whole(n: Note) = rows.getValue(n).let { it.start >= rollTop - 1f && it.endInclusive <= minOf(panelTop, rollBottom) + 1f }
    }

    private fun seen(): Seen {
        val track = TutoHints.notes!!.first
        val written = h.vm.project.value.track(track).notes.notes
        val left = TutoHints.notes!!.second.filter { d -> written.none { it.pitch == d.pitch && it.start == d.start } }
        val canvas = node("piano_roll")
        val view = node("roll_view").boundsInRoot
        val panelTop = node("tuto_panel").boundsInRoot.top
        // The roll's rows: C1 (24) to C8 (108), PianoRoll's LOW and HIGH (no note of these tracks lies past them).
        val rowPx = canvas.size.height / (108 - 24 + 1f)
        val stepPx = canvas.size.width / h.vm.project.value.track(track).notes.steps.toFloat()
        val top = canvas.positionInRoot.y
        val left0 = canvas.positionInRoot.x
        val rows = left.associateWith { n -> (top + (108 - n.pitch) * rowPx).let { it..it + rowPx } }
        val inTime = left.filter { n -> (left0 + n.start / NoteClip.TICKS_PER_STEP.toFloat() * stepPx).let { it >= view.left && it < view.right } }.toSet()
        return Seen(panelTop, view.top, view.bottom, rows, inTime)
    }

    /** T4 / N4: a dotted note of the bar in sight is drawn whole above the panel, or the line says ↑ / ↓. */
    private fun dottedNotesInSightOrPointedAt(id: String, level: TutoLevel, simple: Boolean) {
        openNotesStep(id, level, simple)
        val s = seen()
        val said = text("tuto_notes_left_text")
        val hidden = s.inTime.filterNot { s.whole(it) }
        assertTrue(
            "$id: notes under the panel (top ${s.panelTop}, roll ${s.rollTop}..${s.rollBottom}) and the line says « $said »: " +
                hidden.map { "${it.pitch}@${it.start / NoteClip.TICKS_PER_STEP} ${s.rows[it]}" },
            hidden.isEmpty() || said.contains("↑") || said.contains("↓"),
        )
        // A touch on the line brings the next one whole above the panel.
        compose.onNodeWithTag("tuto_notes_left").performClick()
        settle()
        val after = seen()
        val next = after.rows.keys.sortedWith(compareBy({ it.start }, { -it.pitch })).first()
        assertTrue("$id: the next note ${next.pitch} at ${after.rows[next]}, panel at ${after.panelTop}", after.whole(next))
    }

    @Test
    fun proBassNotesAreAboveThePanelOrPointedAt() = dottedNotesInSightOrPointedAt("bass_do", TutoLevel.Pro, simple = false)

    @Test
    fun simpleBassNotesAreAboveThePanelOrPointedAt() = dottedNotesInSightOrPointedAt("bass_do", TutoLevel.Facile, simple = true)

    /** The dotted notes left of the open step, in the order the line brings them. */
    private fun leftNotes(): List<Note> {
        val (track, dotted) = TutoHints.notes!!
        val written = h.vm.project.value.track(track).notes.notes
        return dotted.filter { d -> written.none { it.pitch == d.pitch && it.start == d.start } }.sortedWith(compareBy({ it.start }, { -it.pitch }))
    }

    /** N5: the lead's last note (bar 2): « Je le fais » shows it whole above the panel and the line says its bar. */
    private fun theLastNoteAndItsBar(level: TutoLevel, simple: Boolean) {
        openNotesStep("lead_do", level, simple)
        val all = leftNotes()
        all.dropLast(1).forEach { h.vm.addNote(it) }
        settle()
        val last = leftNotes().single()
        compose.onNodeWithTag("tuto_notes_left").performClick()
        settle()
        val s = seen()
        assertTrue("the last note ${last.pitch} at ${s.rows[last]}, roll ${s.rollTop}..${s.rollBottom}, panel at ${s.panelTop}", last in s.inTime && s.whole(last))
        val bar = last.start / NoteClip.TICKS_PER_STEP / 16 + 1
        val where = text("tuto_roll_where")
        assertTrue("the line says « $where », the note is in bar $bar", where.contains("mesure $bar/"))
    }

    @Test
    fun proTheLastLeadNoteIsShownWithItsBar() = theLastNoteAndItsBar(TutoLevel.Pro, simple = false)

    @Test
    fun simpleTheLastLeadNoteIsShownWithItsBar() = theLastNoteAndItsBar(TutoLevel.Facile, simple = true)

}
