package com.pocketstudio.app.tuto

import android.os.Looper
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.guide.GuideListenPlayer
import com.pocketstudio.engine.OfflineExport
import com.pocketstudio.model.Project
import com.pocketstudio.model.allScenes
import com.pocketstudio.model.tuto.TutoLevel
import com.pocketstudio.model.tuto.TutoNeed
import com.pocketstudio.model.tuto.TutoProgress
import com.pocketstudio.model.tuto.TutoTarget
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.time.Duration

/**
 * Tour 9, F036 (seat t9-72, trance ch. 8 « Montée »): right after « Montre-moi », touching what the demonstration
 * showed did nothing and the part was not made. The rule: « Créer la partie « X » » touched during the end of the
 * demonstration (its result still on screen) makes part X at once, and it stays once the demonstration is over.
 */
@RunWith(RobolectricTestRunner::class)
class T9L17PartAfterDemoTest {
    private class MemoryStore : TutoStore {
        val saved = HashMap<String, TutoProgress>()
        override fun load(projectId: String) = saved[projectId]
        override fun save(projectId: String, progress: TutoProgress?) {
            if (progress == null) saved.remove(projectId) else saved[projectId] = progress
        }
    }

    private object Quiet : GuideListenPlayer {
        override fun play(pcm: Pcm, ended: () -> Unit): Boolean = true
        override fun stop() {}
        override fun heard(): Long = 0L
    }

    private val scope = MainScope()

    @After
    fun end() = scope.cancel()

    private fun idle(ms: Long = 20) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))

    private fun await(what: String, ok: () -> Boolean) {
        val end = System.currentTimeMillis() + 10_000
        while (!ok()) {
            idle(10)
            check(System.currentTimeMillis() < end) { "$what: not in time" }
        }
    }

    private fun montee(p: Project) = p.allScenes.any { s -> s.label == "Montée" }

    @Test
    fun theNeedTouchedWhileTheDemoHoldsMakesThePartAndKeepsIt() {
        val flow = MutableStateFlow(TutoTarget.of("trance")!!.blank())
        var before: Project? = null
        val c = TutoController(
            scope, "p1", flow, edit = { f -> flow.value = flow.value.f() }, store = MemoryStore(),
            render = { FloatArray(OfflineExport.SAMPLE_RATE) { 0.1f } }, player = Quiet, debounceMs = 10,
            demoShow = { frame -> if (frame != null) { if (before == null) before = flow.value; flow.value = frame } else { before?.let { flow.value = it }; before = null } },
            frameMs = 20, holdMs = 400, fadeMs = 50,
        )
        idle()
        c.start("trance", TutoLevel.Pro)
        val ch = c.ui.value!!.chapters.indexOfFirst { it.steps.any { s -> s.id == "make_montee" } }
        c.goTo(ch, c.ui.value!!.chapters[ch].steps.indexOfFirst { it.id == "make_montee" })
        idle(100)
        assertEquals("make_montee", c.ui.value!!.current.id)
        await("the need") { c.ui.value!!.need != null }
        assertEquals(TutoNeed.Part("Montée"), c.ui.value!!.need)
        assertTrue(c.showMe())
        await("the result held") { c.ui.value!!.demo == TutoDemoPhase.HOLDING }
        c.fulfilNeed()
        await("the demonstration over") { c.ui.value!!.demo == null }
        idle(200)
        assertTrue("part « Montée » made and kept", montee(flow.value))
        assertTrue("the step done", "make_montee" in c.ui.value!!.progress.done)
    }
}
