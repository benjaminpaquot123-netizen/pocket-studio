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
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.time.Duration

/**
 * Tour 9, F001 (seats t9-04, t9-28: « le panneau du tuto se rouvre à chaque étape »): once the user folds the panel, it
 * stays folded when the step changes (« Suivant », « Précédent », the chapters' list, a tick that moves on); it opens
 * again only when the user unfolds it.
 */
@RunWith(RobolectricTestRunner::class)
class Tour9L13TutoFoldKeptTest {
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
    private val target = TutoTarget.of("jump_up")!!

    @After
    fun end() = scope.cancel()

    private fun idle(ms: Long = 50) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))

    private fun controller(flow: MutableStateFlow<Project>) =
        TutoController(
            scope, "p1", flow, edit = { f -> flow.value = flow.value.f() }, store = MemoryStore(),
            render = { FloatArray(OfflineExport.SAMPLE_RATE * 2) { 0.1f } }, player = Player(), debounceMs = 10,
        ).also { idle() }

    @Test
    fun foldedByTheUserStaysFoldedWhenTheStepChanges() {
        val c = controller(MutableStateFlow(target.blank()))
        c.start("jump_up", TutoLevel.Facile)
        idle(200)
        assertTrue("a new tutorial opens on its words", c.ui.value!!.open)
        c.toggleOpen()
        assertFalse(c.ui.value!!.open)
        // The chapters' list: another chapter.
        val first = c.ui.value!!.current.id
        c.goTo(1, 0)
        idle(200)
        assertNotEquals(first, c.ui.value!!.current.id)
        assertFalse("goTo: the panel the user folded opened again", c.ui.value!!.open)
        // « Précédent »: back a step.
        val at = c.ui.value!!.current.id
        c.previous()
        idle(200)
        assertNotEquals(at, c.ui.value!!.current.id)
        assertFalse("previous: the panel the user folded opened again", c.ui.value!!.open)
        // « Suivant » until the step changes (an example is counted first, a step to do is said once).
        val now = c.ui.value!!.current.id
        repeat(3) { if (c.ui.value!!.current.id == now) { c.next(); idle(200) } }
        assertNotEquals(now, c.ui.value!!.current.id)
        assertFalse("next: the panel the user folded opened again", c.ui.value!!.open)
        // The user unfolds it: it stays open on the steps after.
        c.toggleOpen()
        assertTrue(c.ui.value!!.open)
        val open = c.ui.value!!.current.id
        c.goTo(2, 0)
        idle(200)
        assertNotEquals(open, c.ui.value!!.current.id)
        assertTrue("unfolded by the user: open on the next step", c.ui.value!!.open)
        assertEquals(2, c.ui.value!!.chapter)
    }
}
