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
 * Tour 6, lot « tuto-flèches », control seat H a (French beginner, House, chapter 5): after « Copier sur les mesures
 * suivantes » the roll stayed on « mesure 1/8 » and said « plus bas ↓ », then « plus loin → » three swipes long, then
 * « plus bas ↓ » again: 8 gestures lost. The copy is the tutorial's own button: the roll goes to the next dotted note
 * to write, whole in sight, and the line has no arrow. A phone of 360 dp, 100 % and 130 %.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp-port-hdpi")
class Tour6TutoFlechesRollTest {
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

    private fun leftNotes(): List<Note> {
        val (track, dotted) = TutoHints.notes!!
        val written = h.vm.project.value.track(track).notes.notes
        return dotted.filter { d -> written.none { it.pitch == d.pitch && it.start == d.start } }.sortedWith(compareBy({ it.start }, { -it.pitch }))
    }

    /** [n] drawn whole in the roll, above the tutorial's panel. */
    private fun whole(n: Note): Boolean {
        val track = TutoHints.notes!!.first
        val canvas = node("piano_roll")
        val view = node("roll_view").boundsInRoot
        val bottom = minOf(node("tuto_panel").boundsInRoot.top, view.bottom)
        val rowPx = canvas.size.height / (108 - 24 + 1f)
        val stepPx = canvas.size.width / h.vm.project.value.track(track).notes.steps.toFloat()
        val y = canvas.positionInRoot.y + (108 - n.pitch) * rowPx
        val x = canvas.positionInRoot.x + n.start / NoteClip.TICKS_PER_STEP.toFloat() * stepPx
        // What is drawn of the grid (the keys cover the roll's left edge).
        val grid = canvas.boundsInRoot
        return y >= view.top - 1f && y + rowPx <= bottom + 1f && x >= grid.left - 1f && x + stepPx <= grid.right + 1f
    }

    private fun afterTheCopyTheRollShowsTheNextNote(style: String) {
        val target = TutoTarget.of(style)!!
        h.start(simple = true, build = {
            session.edit { target.blank() }
            PrefsTutoStore(h.app).save(projectId, TutoProgress(style, TutoLevel.Facile, current = "bass_do"))
        })
        await("the panel") { has("tuto_need") }
        compose.onNodeWithTag("tuto_need").performClick()
        await("the dotted notes") { has("piano_roll") && TutoHints.notes?.second?.isNotEmpty() == true }
        settle()
        // The bar written by hand, a note at a time.
        var guard = 0
        while (!has("tuto_roll_copy") && guard++ < 40) {
            h.vm.addNote(leftNotes().first())
            settle()
        }
        assertTrue("« Copier » is offered on the line once the motif is written", has("tuto_roll_copy"))
        compose.onNodeWithTag("tuto_roll_copy", useUnmergedTree = true).performClick()
        await("the notes to write after the copy") { !has("tuto_roll_copy") && leftNotes().isNotEmpty() }
        settle()
        val next = leftNotes().first()
        val said = text("tuto_notes_left_text")
        val where = text("tuto_roll_where")
        val bar = next.start / NoteClip.TICKS_PER_STEP / h.vm.project.value.stepsPerBar + 1
        assertTrue("after the copy the next note to write (${next.pitch}, bar $bar) is whole in sight; the line says « $where · $said »", whole(next))
        assertTrue("no arrow over a dotted note in sight: « $said »", listOf("↑", "↓", "→", "←").none { it in said })
        assertTrue("the line names the bar to write ($bar): « $where »", where.contains("$bar/"))
    }

    @Test
    fun houseBassAfterTheCopyTheRollShowsTheNextNote() = afterTheCopyTheRollShowsTheNextNote("house")

    @Test
    @Config(fontScale = 1.3f)
    fun houseBassAfterTheCopyTheRollShowsTheNextNoteAt130() = afterTheCopyTheRollShowsTheNextNote("house")

    /**
     * Seat D a (techno, chapters 5 and 7): on the real roll, every dotted note of the step is reached by following the
     * line: no arrow when the note the line speaks of is whole in sight, else at most two touches on the line bring one
     * whole in sight, above the tutorial's panel. Each note written in turn, the roll left where the user's last
     * touch put it (nothing scrolls between two notes).
     */
    private fun everyNoteIsReachedByFollowingTheLine(style: String, stepId: String) {
        val target = TutoTarget.of(style)!!
        h.start(simple = true, build = {
            session.edit { target.blank() }
            PrefsTutoStore(h.app).save(projectId, TutoProgress(style, TutoLevel.Facile, current = stepId))
        })
        await("the panel") { has("tuto_need") }
        compose.onNodeWithTag("tuto_need").performClick()
        await("the dotted notes") { has("piano_roll") && TutoHints.notes?.second?.isNotEmpty() == true }
        settle()
        var written = 0
        var arrows = 0
        while (leftNotes().isNotEmpty() && !has("tuto_roll_copy") && written < 40) {
            var touches = 0
            while (leftNotes().none { whole(it) }) {
                val said = text("tuto_notes_left_text")
                assertTrue("no dotted note whole in sight and no arrow: « $said » (${leftNotes().size} left)", listOf("↑", "↓", "→", "←").any { it in said })
                assertTrue("more than two touches on the line « $said » and still no dotted note in sight", touches < 2)
                compose.onNodeWithTag("tuto_notes_left", useUnmergedTree = true).performClick()
                touches++
                arrows++
                settle()
            }
            val said = text("tuto_notes_left_text")
            assertTrue("a dotted note is whole in sight, no arrow expected: « $said »", listOf("↑", "↓", "→", "←").none { it in said })
            h.vm.addNote(leftNotes().first { whole(it) })
            written++
            settle()
        }
        assertTrue("$style/$stepId: notes written $written", written >= 4)
    }

    @Test
    fun technoBassEveryNoteIsReachedByFollowingTheLine() = everyNoteIsReachedByFollowingTheLine("techno", "bass_do")

    @Test
    @Config(fontScale = 1.3f)
    fun technoBassEveryNoteIsReachedByFollowingTheLineAt130() = everyNoteIsReachedByFollowingTheLine("techno", "bass_do")

    @Test
    fun technoRiffEveryNoteIsReachedByFollowingTheLine() = everyNoteIsReachedByFollowingTheLine("techno", "lead_do")

    @Test
    @Config(fontScale = 1.3f)
    fun trapBassEveryNoteIsReachedByFollowingTheLineAt130() = everyNoteIsReachedByFollowingTheLine("trap", "bass_do")
}
