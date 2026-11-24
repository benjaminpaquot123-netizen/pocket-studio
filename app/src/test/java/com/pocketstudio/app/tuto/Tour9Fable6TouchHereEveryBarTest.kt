package com.pocketstudio.app.tuto

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import com.pocketstudio.app.nav.AppHarness
import com.pocketstudio.model.Note
import com.pocketstudio.model.NoteClip
import com.pocketstudio.model.track
import com.pocketstudio.model.tuto.TutoCourse
import com.pocketstudio.model.tuto.TutoLevel
import com.pocketstudio.model.tuto.TutoPlay
import com.pocketstudio.model.tuto.TutoProgress
import com.pocketstudio.model.tuto.TutoTarget
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

/**
 * Tour 9, lot « fable6 » (the « a-V1 » alert of the tutorial: tour 7, control p8 and p8b, seat a V1, seat b in ja:
 * « Mesures 1 et 4 : le premier toucher ne montre que 2 notes sur 6, la flèche passe à ↓ et il faut toucher une deuxième
 * fois »). Acceptance, seen by the user: « À chaque mesure d'une ligne de notes du tuto, la note demandée est en vue
 * au-dessus du panneau ; si elle ne l'est pas, UN toucher sur « Touche ici » montre toutes les notes en pointillé de la
 * mesure, sans flèche ; et chaque note en pointillé se pose d'un toucher, jusqu'à la fin de l'étape. »
 * Every line of notes of the course, the course played up to it, on the seats' phone (360 dp), by real touches.
 */
abstract class Tour9Fable6TouchHereEveryBar(private val style: String, private val stepId: String, private val simple: Boolean) {
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
            check(System.currentTimeMillis() < end) { "$what: not in time (${state()})" }
        }
    }

    private fun leftNotes(): List<Note> {
        val (track, dotted) = TutoHints.notes ?: return emptyList()
        val written = h.vm.project.value.track(track).notes.notes
        return dotted.filter { d -> written.none { it.pitch == d.pitch && it.start == d.start } }.sortedWith(compareBy({ it.start }, { -it.pitch }))
    }

    /** Where [n] is drawn: whether its time is in the roll's window, whether its row is whole above the panel, its cell's centre. */
    private class Place(val inTime: Boolean, val whole: Boolean, val at: Offset)

    private fun place(n: Note): Place? {
        if (!has("piano_roll") || !has("roll_view")) return null
        val track = TutoHints.notes!!.first
        val canvas = node("piano_roll")
        val view = node("roll_view").boundsInRoot
        val panel = if (has("tuto_panel")) node("tuto_panel").boundsInRoot.top else Float.MAX_VALUE
        val bottom = minOf(panel, view.bottom)
        val rowPx = canvas.size.height / (108 - 24 + 1f)
        val stepPx = canvas.size.width / h.vm.project.value.track(track).notes.steps.toFloat()
        val y = canvas.positionInRoot.y + (108 - n.pitch) * rowPx
        val x = canvas.positionInRoot.x + n.start / NoteClip.TICKS_PER_STEP.toFloat() * stepPx
        val grid = canvas.boundsInRoot
        val inTime = x >= maxOf(grid.left, view.left) - 1f && x + stepPx <= minOf(grid.right, view.right) + 1f
        return Place(inTime, y >= view.top - 1f && y + rowPx <= bottom + 1f, Offset(x + stepPx / 2, y + rowPx / 2))
    }

    private fun touchable(n: Note) = place(n)?.takeIf { it.inTime && it.whole }

    private fun state(): String {
        val v = if (has("roll_view")) node("roll_view").boundsInRoot.toString() else "-"
        val p = if (has("tuto_panel")) node("tuto_panel").boundsInRoot.toString() else "-"
        return "$style/$stepId, step ${TutoHints.step}, line « ${text("tuto_notes_left_text")} », roll $v, panel $p, left ${leftNotes().map { "${it.pitch}@${it.start}" }}"
    }

    @Test
    fun everyDottedNoteIsOneTouchAway() {
        val level = if (simple) TutoLevel.Facile else TutoLevel.Pro
        val target = TutoTarget.of(style)!!
        val steps = TutoCourse.of(style)!!.chaptersFor(level).flatMap { it.steps }
        val step = steps.first { it.id == stepId }
        var played = target.blank()
        val before = steps.takeWhile { it != step }
        for (s in before) if (!(s.manual || s.listening || s.check == "shared")) played = TutoPlay.play(s, played, target, level).project
        h.start(simple = simple, build = {
            session.edit { played }
            PrefsTutoStore(h.app).save(projectId, TutoProgress(style, level, current = stepId, done = before.map { it.id }.toSet()))
        })
        await("the panel") { has("tuto_need") || has("tuto_do") || TutoHints.notes?.second?.isNotEmpty() == true }
        // The step's buttons (« Reprendre ici », « Je le fais », « Copier »…), until its notes are dotted in the roll.
        var buttons = 0
        while (!(has("piano_roll") && leftNotes().isNotEmpty()) && buttons < 4) {
            val tag = listOf("tuto_need", "tuto_do").firstOrNull { has(it) } ?: break
            compose.onNodeWithTag(tag, useUnmergedTree = true).performClick()
            buttons++
            settle()
        }
        await("the dotted notes") { has("piano_roll") && TutoHints.notes?.second?.isNotEmpty() == true }
        settle()
        // A line the course's earlier steps already wrote whole (a bar copied): nothing is left to touch on arrival.
        if (leftNotes().isEmpty()) {
            // Skipped only when the step is ticked: a line whole and a step never done would be a dead end.
            assertTrue("whole on arrival and not done: ${state()}", TutoPlay.done(step, h.vm.project.value, target, null))
            println("FABLE6 $style/$stepId ${level.name}: whole on arrival, done")
        }
        assumeTrue("$style/$stepId ${level.name}: nothing left to lay on arrival", leftNotes().isNotEmpty())
        settle()
        val total = leftNotes().size
        var touches = 0
        var asks = 0
        while (leftNotes().isNotEmpty() && TutoHints.step == stepId && touches < 80) {
            if (leftNotes().none { touchable(it) != null }) {
                assertTrue("no dotted note in sight and no line to touch: ${state()}", has("tuto_notes_left"))
                compose.onNodeWithTag("tuto_notes_left", useUnmergedTree = true).performClick()
                asks++
                settle()
                // ONE touch on the line: every dotted note of the time in sight is whole above the panel, no arrow left.
                val inTime = leftNotes().filter { place(it)?.inTime == true }
                val cut = inTime.filterNot { place(it)!!.whole }
                assertTrue("after one touch on the line (note ${total - leftNotes().size + 1} of $total), no dotted note in sight: ${state()}", inTime.isNotEmpty())
                assertTrue("after one touch on the line (note ${total - leftNotes().size + 1} of $total), ${cut.size} of ${inTime.size} dotted notes of the bar out of sight ${cut.map { "${it.pitch}@${it.start}" }}: ${state()}", cut.isEmpty())
                val said = text("tuto_notes_left_text")
                assertTrue("after one touch the line still points (« $said »): ${state()}", !said.contains("↓") && !said.contains("↑"))
            }
            val left = leftNotes()
            val n = left.first { touchable(it) != null }
            val at = touchable(n)!!.at
            val origin = node("piano_roll").boundsInRoot.topLeft
            compose.onNodeWithTag("piano_roll", useUnmergedTree = true).performTouchInput { click(at - origin) }
            touches++
            settle()
            val after = leftNotes()
            assertTrue("touch $touches on ${n.pitch}@${n.start} wrote nothing (${left.size} then ${after.size} left): ${state()}", after.size < left.size || TutoHints.step != stepId)
        }
        assertTrue("$total dotted notes, ${leftNotes().size} left after $touches touches: ${state()}", leftNotes().isEmpty() || TutoHints.step != stepId)
        println("FABLE6 $style/$stepId ${level.name}: $total notes, $touches touches, $asks touches on the line")
    }

    companion object {
        /** Every line of notes of [styles], at both levels. */
        fun lines(styles: List<String>): List<Array<Any>> = styles.flatMap { style ->
            TutoLevel.entries.flatMap { level ->
                TutoCourse.of(style)!!.chaptersFor(level).flatMap { it.steps }.filter { it.check.startsWith("notes:") }
                    .map { arrayOf<Any>(style, it.id, level == TutoLevel.Facile) }
            }
        }
    }
}

/** Every style, both levels, on the seats' phone (house is theirs: its bass has two « La » 13 rows apart). */
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp-port-hdpi", fontScale = 1.0f)
class Tour9Fable6TouchHereEveryBarTest(style: String, stepId: String, simple: Boolean) : Tour9Fable6TouchHereEveryBar(style, stepId, simple) {
    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0} {1} simple={2}")
        fun cases() = lines(if (System.getenv("PS_FABLE6_STYLES") == "all") TutoCourse.STYLES.toList() else SPREAD)

        /** The seats' style, and three more of other shapes; `PS_FABLE6_STYLES=all` plays every style (more than 20 minutes). */
        private val SPREAD = listOf("house", "techno", "pop", "trap").filter { it in TutoCourse.STYLES }
    }
}
