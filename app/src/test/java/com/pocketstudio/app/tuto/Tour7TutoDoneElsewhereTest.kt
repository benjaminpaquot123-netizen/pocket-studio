package com.pocketstudio.app.tuto

import android.os.Looper
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.guide.GuideListenPlayer
import com.pocketstudio.engine.OfflineExport
import com.pocketstudio.model.Project
import com.pocketstudio.model.allScenes
import com.pocketstudio.model.switchScene
import com.pocketstudio.model.tuto.TutoCourse
import com.pocketstudio.model.tuto.TutoLevel
import com.pocketstudio.model.tuto.TutoPlay
import com.pocketstudio.model.tuto.TutoProgress
import com.pocketstudio.model.tuto.TutoStep
import com.pocketstudio.model.tuto.TutoTarget
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.time.Duration

/**
 * Tour 7, control of « fable-repar » (seat a, V4; fr, house, Facile): a step done in the part A (the charleys of
 * chapter 4, the bass line), then a new one-bar part B shown: the grid of B had the step's cells dotted again, the
 * piano roll said « Encore 24 notes en pointillé ». A step ticked whose line is whole in another part asks for
 * nothing in the part shown: no dotted cell, no dotted note there; back in its part, they are the step's again.
 */
@RunWith(RobolectricTestRunner::class)
class Tour7TutoDoneElsewhereTest {
    private class MemoryStore : TutoStore {
        val saved = HashMap<String, TutoProgress>()
        override fun load(projectId: String) = saved[projectId]
        override fun save(projectId: String, progress: TutoProgress?) {
            if (progress == null) saved.remove(projectId) else saved[projectId] = progress
        }
    }

    private class Player : GuideListenPlayer {
        override fun play(pcm: Pcm, ended: () -> Unit): Boolean = true
        override fun stop() {}
        override fun heard(): Long = 0L
    }

    private val scope = MainScope()
    private val style = "house"
    private val level = TutoLevel.Facile

    @After
    fun end() { scope.cancel(); TutoHints.reset() }

    private fun idle(ms: Long = 50) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))

    /** The controller on [step], the course played in order up to it and the step itself done, in the part A. */
    private fun doneAt(step: TutoStep): Pair<TutoController, MutableStateFlow<Project>> {
        val target = TutoTarget.of(style)!!
        val steps = TutoCourse.of(style)!!.chaptersFor(level).flatMap { it.steps }
        var p = target.blank()
        for (s in steps.take(steps.indexOf(step) + 1)) if (!(s.manual || s.listening || s.check == "shared")) p = TutoPlay.play(s, p, target, level).project
        val flow = MutableStateFlow(p)
        val c = TutoController(
            scope, "p1", flow, edit = { f -> flow.value = flow.value.f() }, store = MemoryStore(),
            render = { FloatArray(OfflineExport.SAMPLE_RATE * 2) { 0.1f } }, player = Player(), debounceMs = 10,
        )
        idle()
        c.start(style, level)
        for (i in 0 until 200) { idle(50); if (step.id in c.ui.value!!.progress.done) break; Thread.sleep(20) }
        val chapters = c.ui.value!!.chapters
        val ch = chapters.indexOfFirst { x -> x.steps.any { it.id == step.id } }
        c.goTo(ch, chapters[ch].steps.indexOfFirst { it.id == step.id }); idle(100)
        assertEquals(step.id, c.ui.value!!.current.id)
        assertTrue("${step.id} is not ticked: the test checks nothing", step.id in c.ui.value!!.progress.done)
        return c to flow
    }

    /** The tester's gesture: a new empty part shown (« B »), one bar. */
    private fun newPart(flow: MutableStateFlow<Project>) {
        val p = flow.value
        flow.value = p.switchScene(p.allScenes.size)
        assertTrue("the new part is not empty", flow.value.allScenes[flow.value.scene].isEmpty)
        idle(100)
    }

    @Test
    fun theCharleysDoneInAAreNotDottedInB() {
        val chapters = TutoCourse.of(style)!!.chaptersFor(level)
        val step = chapters[3].steps.first { it.check.startsWith("lane:") && it.part.isBlank() }
        val (c, flow) = doneAt(step)
        // The screen's own writing of the dotted cells when the step is reached (AppRoot).
        TutoHints.cells = c.hint(flow.value.drums.steps)
        assertNotNull("${step.id}: no cell of the step in its own part: the test checks nothing", TutoHints.cells)
        val a = flow.value.scene
        newPart(flow)
        assertNull("${step.id}, done in A: its cells dotted in the new part B: ${c.hint(flow.value.drums.steps)}", c.hint(flow.value.drums.steps))
        assertNull("${step.id}, done in A: the grid of B still has its cells dotted (the part shown changed, the cells were not read again)", TutoHints.cells)
        flow.value = flow.value.switchScene(a); idle(100)
        assertNotNull("${step.id}: back in A, its cells are the step's again", TutoHints.cells)
    }

    @Test
    fun theBassDoneInAIsNotDottedInB() {
        val step = TutoCourse.of(style)!!.chaptersFor(level).flatMap { it.steps }.last { it.check == "notes:bass" }
        val (c, flow) = doneAt(step)
        assertTrue("${step.id}: no note of the step in its own part: the test checks nothing", c.ghostNotes(flow.value)?.second.orEmpty().isNotEmpty())
        newPart(flow)
        val left = c.ghostNotes(flow.value)?.second.orEmpty()
        assertTrue("${step.id}, done in A: « Encore ${left.size} notes en pointillé » in the new part B", left.isEmpty())
    }
}
