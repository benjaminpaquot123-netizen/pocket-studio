package com.pocketstudio.app.tuto

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.semantics.SemanticsProperties
import com.pocketstudio.app.nav.AppHarness
import com.pocketstudio.app.synth.PianoRoll
import com.pocketstudio.app.synth.PianoRollActions
import com.pocketstudio.model.Note
import com.pocketstudio.model.NoteClip
import com.pocketstudio.model.tuto.TutoCourse
import com.pocketstudio.model.tuto.TutoLevel
import com.pocketstudio.model.tuto.TutoProgress
import com.pocketstudio.model.tuto.TutoTarget
import com.pocketstudio.ui.PocketStudioTheme
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
 * Testers (lot « tuto 3 »): what the screen does while a step is worked on. The panel never changes
 * height by itself (« Bravo » took the grid's last cells under the finger), the folded line is the
 * same height whatever it says, the song's map lights the part a step is about, the piano roll goes
 * to dotted notes out of sight, the step grid shows the bar of the dotted cells left.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w392dp-h852dp")
class TutoScreenTest {
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
    private fun bounds(tag: String) = compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot

    private fun await(what: String, ok: () -> Boolean) {
        val end = System.currentTimeMillis() + 20_000
        while (!ok()) {
            idle(50)
            // The screens' own delays (a page turned a moment after the last touch) run on the test's clock.
            compose.mainClock.advanceTimeBy(50)
            compose.waitForIdle()
            check(System.currentTimeMillis() < end) { "$what: not in time" }
        }
    }

    private fun uiAt(id: String, level: TutoLevel = TutoLevel.Facile): TutoUi {
        val chapters = TutoCourse.of("jump_up")!!.chaptersFor(level)
        val c = chapters.indexOfFirst { ch -> ch.steps.any { it.id == id } }
        return TutoUi(TutoProgress("jump_up", level, current = id), chapters, c, chapters[c].steps.indexOfFirst { it.id == id })
    }

    @Test
    fun thePanelNeverChangesHeightWhenAStepIsDone() {
        val start = uiAt("hat_do")
        var ui by mutableStateOf(start)
        var mine by mutableStateOf(false)
        compose.setContent {
            PocketStudioTheme {
                Column(Modifier.fillMaxSize()) {
                    Spacer(Modifier.weight(1f))
                    TutoPanel(ui, canShow = true, actions = TutoActions(), mine = mine, reducedMotion = true)
                }
            }
        }
        compose.waitForIdle()
        val open = bounds("tuto_panel")
        // Ticked while the user touches the cells: « Bravo » takes the text's place, « Écouter ton résultat » comes.
        ui = start.copy(progress = start.progress.copy(done = setOf("hat_do")), bravo = true)
        mine = true
        compose.waitForIdle()
        assertTrue(has("tuto_bravo"))
        assertTrue(has("tuto_play_mine"))
        assertEquals("the panel grew or shrank under the finger", open.top, bounds("tuto_panel").top, 0.5f)
        // Folded: one line, the same height for the step, « ✓ Réussi · Étape suivante → » and « Montre-moi ».
        ui = start.copy(open = false)
        compose.waitForIdle()
        val folded = bounds("tuto_panel").top
        assertTrue(has("tuto_folded_text"))
        ui = start.copy(open = false, progress = start.progress.copy(done = setOf("hat_do")), bravo = true)
        compose.waitForIdle()
        assertTrue(has("tuto_next_folded"))
        assertEquals(folded, bounds("tuto_panel").top, 0.5f)
        ui = start.copy(open = false, demo = TutoDemoPhase.SHOWING)
        compose.waitForIdle()
        assertTrue(has("tuto_demo_caption"))
        assertEquals(folded, bounds("tuto_panel").top, 0.5f)
        assertTrue("folded is smaller", folded > open.top)
    }

    @Test
    fun theFoldedSuccessGoesToTheNextStepAndTheChaptersTitleIsWhole() {
        val start = uiAt("ref")
        var next = 0
        compose.setContent {
            PocketStudioTheme {
                TutoPanel(start.copy(open = false, progress = start.progress.copy(done = setOf("ref"))), canShow = false, actions = TutoActions(next = { next++ }), reducedMotion = true)
            }
        }
        compose.onNodeWithTag("tuto_next_folded").performClick()
        compose.waitForIdle()
        assertEquals(1, next)
        val title = compose.onNodeWithTag("tuto_chapter_title", useUnmergedTree = true).fetchSemanticsNode().config[SemanticsProperties.Text].joinToString()
        assertEquals("Le tempo et la tonalité", title)
    }

    @Test
    fun theMapLightsThePartTheStepIsAbout() {
        val blocks = tutoMap("jump_up")
        compose.setContent { PocketStudioTheme { SongMap(blocks, focus = mapFocus("part:Montée")) } }
        val lit = blocks.indices.filter { has("tuto_map_lit_$it") }
        assertTrue(lit.isNotEmpty())
        assertEquals(blocks.indices.filter { blocks[it].label == "Montée" }, lit)
        assertEquals("Drop 2", mapFocus("part:Drop 2"))
        assertEquals("Intro", mapFocus("vs:Drop:Intro"))
        assertEquals(null, mapFocus("parts:bass"))
    }

    /** The bass line goes on past the screen: the roll goes there by itself, and says how many are left. */
    @Test
    fun thePianoRollGoesToTheDottedNotesOutOfSight() {
        val t = NoteClip.TICKS_PER_STEP
        val far = listOf(Note(52 * t, 2 * t, 41), Note(56 * t, 2 * t, 41))
        compose.setContent { PocketStudioTheme { PianoRoll(NoteClip(steps = 64), emptyList(), -1, PianoRollActions(), hints = far) } }
        // Out of sight it would say « plus loin → »; the roll went there: they are in sight.
        await("the roll went to them") {
            compose.onNodeWithTag("tuto_notes_left").fetchSemanticsNode().config[SemanticsProperties.Text].joinToString() == "Encore 2 notes en pointillé à toucher"
        }
    }

    /** Karim's snare on 2 bars: the second bar, out of sight, is shown once the first bar's dotted cells are touched. */
    @Test
    fun theGridShowsTheBarOfTheDottedCellsLeft() {
        val target = TutoTarget.of("jump_up")!!
        h.start(simple = false, build = {
            session.edit { target.blank() }
            PrefsTutoStore(h.app).save(projectId, TutoProgress("jump_up", TutoLevel.Pro, current = "snare_do"))
        })
        await("panel") { has("tuto_need") }
        compose.onNodeWithTag("tuto_need").performClick()
        await("2 bars, the grid") { h.vm.project.value.drums.steps == 32 && has("step_0") }
        val (lane, cells) = TutoHints.cells!!
        assertTrue("dotted cells on both bars: $cells", cells.any { it < 16 } && cells.any { it >= 16 })
        assertTrue(has("drum_pages"))
        cells.filter { it < 16 }.forEach { h.vm.toggleStep(it) }
        assertEquals(lane, h.vm.transport.value.selectedPad)
        await("the second bar shown") { has("step_${cells.first { it >= 16 }}") }
    }
}
