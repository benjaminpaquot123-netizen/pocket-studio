package com.pocketstudio.app.tuto

import android.os.Looper
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.guide.GuideListenPlayer
import com.pocketstudio.engine.OfflineExport
import com.pocketstudio.model.Project
import com.pocketstudio.model.tuto.TutoLevel
import com.pocketstudio.model.tuto.TutoPlay
import com.pocketstudio.model.tuto.TutoProgress
import com.pocketstudio.model.tuto.TutoTarget
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
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
 * Tour 9, F033 (t9-04, t9-14, t9-23, t9-24, t9-33, t9-37): « Je le fais » set the effect or drew the curve itself
 * and the step ticked with no gesture of the user. Acceptance: « Je le fais » m'emmène au bon endroit mais me
 * laisse faire le geste ; rien n'est coché avant que je le fasse.
 */
@RunWith(RobolectricTestRunner::class)
class Tour9TutoDoItLeavesTheGestureTest {
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

    private fun await(what: String, ok: () -> Boolean) {
        val end = System.currentTimeMillis() + 20_000
        while (!ok()) {
            idle(50)
            check(System.currentTimeMillis() < end) { "$what: not in time" }
            Thread.sleep(5)
        }
    }

    private fun controller(flow: MutableStateFlow<Project>, goes: MutableList<TutoGo>) =
        TutoController(
            scope, "p1", flow, edit = { f -> flow.value = flow.value.f() }, store = MemoryStore(),
            render = { FloatArray(OfflineExport.SAMPLE_RATE * 2) { 0.1f } }, player = Player(), debounceMs = 10,
        ).also { c -> scope.launch { c.goes.collect { goes += it } }; idle() }

    /** The whole example song with no send and no curve: what a user has before the effects chapter. */
    private fun bare(target: TutoTarget): Project {
        val s = target.song
        return s.copy(automation = emptyList(), mixer = s.mixer.copy(channels = s.mixer.channels.map { ch -> ch.copy(sends = ch.sends.map { 0f }) }))
    }

    private fun stepOf(c: TutoController, id: String) {
        val chapters = c.ui.value!!.chapters
        val ch = chapters.indexOfFirst { ch -> ch.steps.any { it.id == id } }
        c.goTo(ch, chapters[ch].steps.indexOfFirst { it.id == id })
        idle()
        assertEquals(id, c.ui.value!!.current.id)
    }

    @Test
    fun doItGoesThereAndLeavesTheGestureToTheUser() {
        val checked = ArrayList<String>()
        for ((style, id) in listOf("techno" to "reverb", "jump_up" to "reverb", "techno" to "filter", "acid_techno" to "filter")) {
            val target = TutoTarget.of(style)!!
            val flow = MutableStateFlow(bare(target))
            val goes = ArrayList<TutoGo>()
            val c = controller(flow, goes)
            c.start(style, TutoLevel.Pro)
            idle()
            stepOf(c, id)
            val before = flow.value
            val n = goes.size
            c.doIt()
            await("$style/$id: went") { goes.size > n }
            idle(300)
            val go = goes.last()
            assertTrue("$style/$id: « Je le fais » shows where to work", go.effects != null || go.open != null)
            assertEquals("$style/$id: « Je le fais » made the gesture itself", before, flow.value)
            assertFalse("$style/$id: ticked before the user made the gesture", id in c.ui.value!!.progress.done)
            // The user makes the gesture there: then it ticks.
            val step = c.ui.value!!.current
            flow.value = TutoPlay.doHere(step, flow.value, target, go.effects, TutoLevel.Pro)!!
            await("$style/$id: ticked once the user made it") { id in c.ui.value!!.progress.done }
            checked += "$style/$id"
        }
        assertEquals(4, checked.size)
    }
}
