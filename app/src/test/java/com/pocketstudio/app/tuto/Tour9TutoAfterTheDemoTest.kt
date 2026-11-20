package com.pocketstudio.app.tuto

import android.os.Looper
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.guide.GuideListenPlayer
import com.pocketstudio.engine.OfflineExport
import com.pocketstudio.model.Project
import com.pocketstudio.model.tuto.TutoChecks
import com.pocketstudio.model.tuto.TutoCourse
import com.pocketstudio.model.tuto.TutoLevel
import com.pocketstudio.model.tuto.TutoProgress
import com.pocketstudio.model.tuto.TutoTarget
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.time.Duration


/**
 * Tour 9, F035 (t9-07, t9-29, t9-41, t9-49, t9-59): after « Montre-moi », « Je le fais » was gone, the demonstration
 * did not give the hand back, or the step passed with nothing done. Acceptance: après une démo, mon réglage revient
 * comme avant, l'étape reste à faire et je garde « Je le fais ».
 */
@RunWith(RobolectricTestRunner::class)
class Tour9TutoAfterTheDemoTest {
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

    /** Whether [ok] holds within [ms] (the looks answered). */
    private fun within(ms: Long, ok: () -> Boolean): Boolean {
        val end = System.currentTimeMillis() + ms
        while (!ok() && System.currentTimeMillis() < end) { idle(50); Thread.sleep(5) }
        return ok()
    }

    /** The project the steps before [id] make. */
    private fun before(style: String, id: String): Project {
        val target = TutoTarget.of(style)!!
        var p = target.blank()
        for (s in TutoCourse.of(style)!!.chapters.flatMap { it.steps }) {
            if (s.id == id) return p
            if (!TutoChecks.done(s, p, target)) p = TutoChecks.show(s, p, target) ?: p
        }
        error("$style: no step $id")
    }

    @Test
    fun theDemoLeavesTheStepAsItWas() {
        for ((style, id, level) in listOf(
            Triple("afro_house", "lead_delay", TutoLevel.Pro),
            Triple("grime", "pad_end", TutoLevel.Facile),
            Triple("jungle", "pad_end", TutoLevel.Facile),
        )) {
            val flow = MutableStateFlow(before(style, id))
            val shown = ArrayList<Project?>()
            val c = TutoController(
                scope, "p-$style", flow, edit = { f -> flow.value = flow.value.f() }, store = MemoryStore(),
                render = { FloatArray(OfflineExport.SAMPLE_RATE * 2) { 0.1f } }, player = Player(), debounceMs = 10,
                demoShow = { shown += it }, frameMs = 5, holdMs = 5, fadeMs = 5,
            )
            idle()
            c.start(style, level)
            idle()
            val chapters = c.ui.value!!.chapters
            val ch = chapters.indexOfFirst { x -> x.steps.any { it.id == id } }
            c.goTo(ch, chapters[ch].steps.indexOfFirst { it.id == id }); idle(300)
            val u = c.ui.value!!
            assertEquals(id, u.current.id)
            assertFalse("$style/$id: done before anything", id in u.progress.done)
            val mine = flow.value
            assertTrue("$style/$id: no « Montre-moi »", c.canShow())
            assertTrue("$style/$id: « Montre-moi » did not start", c.showMe())
            assertTrue("$style/$id: the demonstration never gave the hand back", within(10_000) { c.ui.value!!.demo == null })
            idle(300)
            val after = c.ui.value!!
            assertTrue("$style/$id: nothing shown", shown.any { it != null })
            assertEquals("$style/$id: the screen not given back to my project", null, shown.last())
            assertEquals("$style/$id: my project changed by the demonstration", mine, flow.value)
            assertFalse("$style/$id: ticked by the demonstration", id in after.progress.done)
            assertEquals("$style/$id: the step's button changed by the demonstration", u.need, after.need)
            assertTrue("$style/$id: the panel's buttons not back", after.open || u.current.spot.isNotEmpty())
            assertTrue("$style/$id: « Montre-moi » gone after it", c.canShow())
        }
    }
}
