package com.pocketstudio.app.tuto

import android.os.Looper
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.guide.GuideListenPlayer
import com.pocketstudio.engine.OfflineExport
import com.pocketstudio.model.allScenes
import com.pocketstudio.model.switchScene
import com.pocketstudio.model.tuto.TutoCourse
import com.pocketstudio.model.tuto.TutoLevel
import com.pocketstudio.model.tuto.TutoPlay
import com.pocketstudio.model.tuto.TutoProgress
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
 * Tour 7, audit « Fable », rule R-D: the dotted cell and the crosses are never drawn out of the step's part. The
 * crash of the Drop is dotted in the Drop; the moment another part is shown (the user touched its tab), the grid
 * shows nothing of the step, before the panel has read the project again (the seats saw the Drop's crash dotted in
 * the Montée, and the Montée's crosses in the Drop).
 */
@RunWith(RobolectricTestRunner::class)
class Tour7TutoDottedInItsPartTest {
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

    @After
    fun end() = scope.cancel()

    private fun idle(ms: Long = 50) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))

    /** The controller on [check]'s step of techno, the course played in order up to it, its buttons touched. */
    private fun at(check: String): Pair<TutoController, MutableStateFlow<com.pocketstudio.model.Project>> {
        val style = "techno"
        val level = TutoLevel.Facile
        val target = TutoTarget.of(style)!!
        val steps = TutoCourse.of(style)!!.chaptersFor(level).flatMap { it.steps }
        val step = steps.first { it.check == check }
        var p = target.blank()
        for (s in steps.takeWhile { it != step }) if (!(s.manual || s.listening || s.check == "shared")) p = TutoPlay.play(s, p, target, level).project
        val flow = MutableStateFlow(p)
        val c = TutoController(
            scope, "p1", flow, edit = { f -> flow.value = flow.value.f() }, store = MemoryStore(),
            render = { FloatArray(OfflineExport.SAMPLE_RATE * 2) { 0.1f } }, player = Player(), debounceMs = 10,
        )
        idle()
        c.start(style, level)
        idle()
        val chapters = c.ui.value!!.chapters
        val ch = chapters.indexOfFirst { x -> x.steps.any { it.id == step.id } }
        c.goTo(ch, chapters[ch].steps.indexOfFirst { it.id == step.id }); idle()
        repeat(4) {
            if (c.ui.value!!.need != null) {
                c.fulfilNeed()
                for (i in 0 until 100) { idle(50); if (c.ui.value!!.need == null) break; Thread.sleep(20) }
            }
        }
        assertEquals(step.id, c.ui.value!!.current.id)
        assertNull("a button is still to touch: ${c.ui.value!!.need}", c.ui.value!!.need)
        return c to flow
    }

    /** [flow] showing a part that is not one of the current step's, the panel not yet told. */
    private fun elsewhere(c: TutoController, flow: MutableStateFlow<com.pocketstudio.model.Project>) {
        val p = flow.value
        val step = c.ui.value!!.current
        val other = p.allScenes.indices.first { i -> i != p.scene && !p.allScenes[i].isEmpty && !TutoPlay.inItsPart(step, p.switchScene(i)) }
        flow.value = p.switchScene(other)
    }

    @Test
    fun theCrashIsNotDottedInAnotherPart() {
        val (c, flow) = at("crash")
        assertNotNull("the crash is not dotted in its own part: the test checks nothing", c.hint(flow.value.drums.steps))
        elsewhere(c, flow)
        assertNull("the crash is dotted in « ${flow.value.allScenes[flow.value.scene].label} »", c.hint(flow.value.drums.steps))
    }

    @Test
    fun theSilenceIsNotCrossedOutInAnotherPart() {
        val (c, flow) = at("gap")
        assertTrue("nothing crossed out in its own part: the test checks nothing", c.toErase(flow.value).orEmpty().any { it.value.isNotEmpty() })
        elsewhere(c, flow)
        val crossed = c.toErase(flow.value).orEmpty().filterValues { it.isNotEmpty() }
        assertTrue("crosses $crossed in « ${flow.value.allScenes[flow.value.scene].label} »", crossed.isEmpty())
    }
}
