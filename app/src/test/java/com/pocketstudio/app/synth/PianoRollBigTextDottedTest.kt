package com.pocketstudio.app.synth

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import com.pocketstudio.app.nav.AppHarness
import com.pocketstudio.app.tuto.PrefsTutoStore
import com.pocketstudio.app.tuto.TutoHints
import com.pocketstudio.model.Note
import com.pocketstudio.model.NoteClip
import com.pocketstudio.model.track
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
 * Tour 9, D06 (seats 62, 29; 56, 87; the « plus haut ↑ » counter of 19, 39, 40, 60, 66): at big letters (200 %) in the
 * Mode simple, the piano roll's dotted notes vanished: a touch on one wrote nothing, the view jumped to another row and
 * no dotted note was left anywhere, the step never done. Acceptance, seen by the user: « En Simple comme en Pro, à
 * 130 % comme à 200 %, chaque note en pointillé que je touche se pose, le compteur « encore N notes » baisse d'autant,
 * et il reste toujours une note en pointillé en vue (ou une touche sur la ligne en montre une) jusqu'à la fin de l'étape. »
 * The touches are real touches on the grid, where the note is drawn, above the tutorial's panel.
 */
abstract class PianoRollBigTextDotted {
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
    private fun text(tag: String) = if (has(tag)) node(tag).config[SemanticsProperties.Text].joinToString() else "-"
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
        val (track, dotted) = TutoHints.notes ?: return emptyList()
        val written = h.vm.project.value.track(track).notes.notes
        return dotted.filter { d -> written.none { it.pitch == d.pitch && it.start == d.start } }.sortedWith(compareBy({ it.start }, { -it.pitch }))
    }

    /** Where [n] is drawn in the window (its cell's centre), or null when it is not whole in sight above the panel. */
    private fun place(n: Note): Offset? {
        if (!has("piano_roll") || !has("roll_view")) return null
        val track = TutoHints.notes!!.first
        val canvas = node("piano_roll")
        val view = node("roll_view").boundsInRoot
        val panel = if (has("tuto_panel")) node("tuto_panel").boundsInRoot.top else Float.MAX_VALUE
        val bottom = minOf(panel, view.bottom, compose.onRoot().fetchSemanticsNode().size.height.toFloat())
        val rowPx = canvas.size.height / (108 - 24 + 1f)
        val stepPx = canvas.size.width / h.vm.project.value.track(track).notes.steps.toFloat()
        val y = canvas.positionInRoot.y + (108 - n.pitch) * rowPx
        val x = canvas.positionInRoot.x + n.start / NoteClip.TICKS_PER_STEP.toFloat() * stepPx
        val grid = canvas.boundsInRoot
        val whole = y >= view.top - 1f && y + rowPx <= bottom + 1f && x >= grid.left - 1f && x + stepPx <= grid.right + 1f
        return if (whole) Offset(x + stepPx / 2, y + rowPx / 2) else null
    }

    private fun state(): String {
        val v = if (has("roll_view")) node("roll_view").boundsInRoot.toString() else "-"
        val p = if (has("tuto_panel")) node("tuto_panel").boundsInRoot.toString() else "-"
        return "step ${TutoHints.step}, line « ${text("tuto_notes_left_text")} », roll $v, panel $p, left ${leftNotes().map { "${it.pitch}@${it.start}" }}"
    }

    /** Every dotted note of [stepId] touched where it is drawn, until the step's notes are all written. */
    protected fun touchEveryDottedNote(style: String, stepId: String, simple: Boolean, askAgain: Boolean = false) {
        val target = TutoTarget.of(style)!!
        h.start(simple = simple, build = {
            session.edit { target.blank() }
            PrefsTutoStore(h.app).save(projectId, TutoProgress(style, if (simple) TutoLevel.Facile else TutoLevel.Pro, current = stepId))
        })
        await("the panel") { has("tuto_need") }
        compose.onNodeWithTag("tuto_need").performClick()
        await("the dotted notes") { has("piano_roll") && TutoHints.notes?.second?.isNotEmpty() == true }
        settle()
        val total = leftNotes().size
        var touches = 0
        while (leftNotes().isNotEmpty() && TutoHints.step == stepId && touches < 40) {
            var asks = 0
            while (leftNotes().none { place(it) != null }) {
                assertTrue("no dotted note in sight after $asks touches on the line: ${state()}", asks < 2)
                assertTrue("no dotted note in sight and no line to touch: ${state()}", has("tuto_notes_left"))
                compose.onNodeWithTag("tuto_notes_left", useUnmergedTree = true).performClick()
                asks++
                settle()
            }
            val before = leftNotes()
            val n = before.first { place(it) != null }
            val at = place(n)!!
            // A touch's place is read from the part of the node in sight (its bounds in the root), not its whole canvas.
            val origin = node("piano_roll").boundsInRoot.topLeft
            compose.onNodeWithTag("piano_roll", useUnmergedTree = true).performTouchInput { click(at - origin) }
            touches++
            settle()
            // Seat 62 (« bass_2 »): the panel's button touched again between notes (it asks the roll for the next dotted
            // note): one dotted note was left in sight, then none.
            if (askAgain && has("tuto_need") && leftNotes().isNotEmpty()) {
                compose.onNodeWithTag("tuto_need", useUnmergedTree = true).performClick()
                settle()
            }
            val after = leftNotes()
            assertTrue("touch $touches on ${n.pitch}@${n.start} wrote nothing (${before.size} then ${after.size} left): ${state()}",
                after.size < before.size || TutoHints.step != stepId)
        }
        assertTrue("$style/$stepId: $total dotted notes, ${leftNotes().size} left after $touches touches: ${state()}",
            leftNotes().isEmpty() || TutoHints.step != stepId)
    }

    /**
     * Tour 9, S17 (seat 17, pt-BR, Pro, 200 %): a finger drawn along a long dotted note scrolled the roll, no note
     * written. Acceptance: « Un glissé qui part d'une note en pointillé la pose entière, comme un toucher ; le rouleau ne
     * défile pas. »
     */
    protected fun dragAlongDottedNote(style: String, stepId: String, simple: Boolean, beside: Boolean = false) {
        val target = TutoTarget.of(style)!!
        h.start(simple = simple, build = {
            session.edit { target.blank() }
            PrefsTutoStore(h.app).save(projectId, TutoProgress(style, if (simple) TutoLevel.Facile else TutoLevel.Pro, current = stepId))
        })
        await("the panel") { has("tuto_need") }
        compose.onNodeWithTag("tuto_need").performClick()
        await("the dotted notes") { has("piano_roll") && TutoHints.notes?.second?.isNotEmpty() == true }
        settle()
        if (leftNotes().none { place(it) != null }) {
            compose.onNodeWithTag("tuto_notes_left", useUnmergedTree = true).performClick()
            settle()
        }
        val before = leftNotes()
        val canvas = node("piano_roll")
        val stepPx = canvas.size.width / h.vm.project.value.track(TutoHints.notes!!.first).notes.steps.toFloat()
        // [beside] (tour 9 replay of S17): the finger lands a quarter of a step before the drawn note, where a touch
        // still writes it (tutoTapTarget): the drag must write it too, « comme un toucher ».
        val shift = if (beside) 0.75f * stepPx else 0f
        val tickBefore: (Note) -> Int = { it.start - NoteClip.TICKS_PER_STEP / 4 }
        val n = before.first { place(it) != null && (!beside || (tickBefore(it) >= 0 && tutoTapTarget(tickBefore(it), it.pitch, before) == it)) }
        val at = place(n)!!.let { Offset(it.x - shift, it.y) }
        val origin = canvas.boundsInRoot.topLeft
        val roll = node("roll_view").boundsInRoot
        // Along the note, to its end and a little further (held a moment: a finger drawing, not a flick).
        val to = Offset(minOf(at.x + maxOf(n.length / NoteClip.TICKS_PER_STEP.toFloat(), 3f) * stepPx, roll.right - 2f), at.y)
        compose.onNodeWithTag("piano_roll", useUnmergedTree = true).performTouchInput { swipe(at - origin, to - origin, 500) }
        settle()
        val after = leftNotes()
        assertTrue("a drag along ${n.pitch}@${n.start} wrote nothing (${before.size} then ${after.size} left): ${state()}",
            after.size < before.size || TutoHints.step != stepId)
        assertTrue("the dotted note dragged along is written whole", n !in after)
    }

    @Test
    fun dragAlongDottedBassPro() = dragAlongDottedNote("speed_garage", "bass_do", simple = false)

    @Test
    fun dragAlongDottedBassSimple() = dragAlongDottedNote("speed_garage", "bass_do", simple = true)

    @Test
    fun dragFromBesideDottedBassPro() = dragAlongDottedNote("speed_garage", "bass_do", simple = false, beside = true)

    @Test
    fun dragFromBesideDottedBassSimple() = dragAlongDottedNote("speed_garage", "bass_do", simple = true, beside = true)

    @Test
    fun speedGarageBassSimple() = touchEveryDottedNote("speed_garage", "bass_do", simple = true)

    @Test
    fun speedGarageChordsSimple() = touchEveryDottedNote("speed_garage", "pad_do", simple = true)

    @Test
    fun grimeChordsSimple() = touchEveryDottedNote("grime", "pad_do", simple = true)

    @Test
    fun speedGarageBassAskedAgainSimple() = touchEveryDottedNote("speed_garage", "bass_do", simple = true, askAgain = true)

    @Test
    fun speedGarageBassPro() = touchEveryDottedNote("speed_garage", "bass_do", simple = false)
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp-port-hdpi", fontScale = 2.0f)
class PianoRollBigTextDottedTest : PianoRollBigTextDotted()

/** Seat 62: a foldable held open (1035 × 1243 px), big letters. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w690dp-h829dp-port-hdpi", fontScale = 2.0f)
class PianoRollBigTextDottedFoldableTest : PianoRollBigTextDotted()

/** The letters most testers use (130 %), on the same 360 dp phone. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp-port-hdpi", fontScale = 1.3f)
class PianoRollMidTextDottedTest : PianoRollBigTextDotted()
