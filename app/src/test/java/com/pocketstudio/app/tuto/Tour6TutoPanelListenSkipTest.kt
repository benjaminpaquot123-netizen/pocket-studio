package com.pocketstudio.app.tuto

import android.os.Looper
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.guide.GuideListenPlayer
import com.pocketstudio.engine.OfflineExport
import com.pocketstudio.model.Project
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
 * Tour 6, N19 (grave): the course's end « もう少しで完成！ » held five examples (chapters 1, 2, 4, 5, 7) left by « Suivant »,
 * never said; 24 gestures to find them and play them again. « Suivant » on an example counts it: the user chose to go on.
 */
@RunWith(RobolectricTestRunner::class)
class Tour6TutoPanelListenSkipTest {
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
    private val style = "techno"
    private val target = TutoTarget.of(style)!!

    @After
    fun end() = scope.cancel()

    private fun idle(ms: Long = 50) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))

    private fun controller(flow: MutableStateFlow<Project>) =
        TutoController(
            scope, "p1", flow, edit = { f -> flow.value = flow.value.f() }, store = MemoryStore(),
            render = { FloatArray(OfflineExport.SAMPLE_RATE * 2) { 0.1f } }, player = Player(), debounceMs = 10,
        ).also { idle() }

    @Test
    fun suivantOnEveryExampleLeavesNoneForTheEnd() {
        val c = controller(MutableStateFlow(target.blank()))
        c.start(style, TutoLevel.Facile)
        idle()
        val listening = c.ui.value!!.chapters.flatMap { it.steps }.filter { it.listening }
        assertTrue("the course has examples to hear", listening.size >= 5)
        // Each example reached and left by « Suivant », never played (the testers' way through the course).
        for (s in listening) {
            val ui = c.ui.value!!
            val (ch, i) = ui.placeOf(s)
            c.goTo(ch, i); idle()
            assertEquals(s.id, c.ui.value!!.current.id)
            c.next(); idle()
            // A chapter's « Bravo » stays a moment: still one touch on, never a trap.
            assertTrue("« Suivant » on « ${s.id} » left it out: the end will hold it", s.id in c.ui.value!!.progress.done)
        }
        val left = c.ui.value!!.left.filter { it.listening }.map { it.id }
        assertTrue("examples the end still asks for: $left", left.isEmpty())
        assertFalse("a step to make is not ticked by « Suivant »", "kick_do" in c.ui.value!!.progress.done)
    }
}
