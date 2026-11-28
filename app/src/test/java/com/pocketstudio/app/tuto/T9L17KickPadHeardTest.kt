package com.pocketstudio.app.tuto

import android.os.Looper
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.guide.GuideListenPlayer
import com.pocketstudio.engine.OfflineExport
import com.pocketstudio.model.Project
import com.pocketstudio.model.tuto.TutoChecks
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
 * Tour 9, F039 (seat t9-27, gabber ch. 2): « Écoute le kick seul » was ticked only by « Écouter l'exemple »;
 * the kick pad played by hand (Rythme's pads, [TutoPadEar]) did nothing.
 */
@RunWith(RobolectricTestRunner::class)
class T9L17KickPadHeardTest {
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
    fun end() { scope.cancel(); TutoPadEar.heard = null }

    private fun idle(ms: Long = 50) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))

    private fun onKickHear(): TutoController {
        val flow = MutableStateFlow<Project>(TutoTarget.of("gabber")!!.blank())
        val c = TutoController(
            scope, "p1", flow, edit = { f -> flow.value = flow.value.f() }, store = MemoryStore(),
            render = { FloatArray(OfflineExport.SAMPLE_RATE * 2) { 0.1f } }, player = Player(), debounceMs = 10,
        )
        idle()
        c.start("gabber", TutoLevel.Facile)
        val ch = c.ui.value!!.chapters.indexOfFirst { it.steps.any { s -> s.id == "kick_hear" } }
        c.goTo(ch, c.ui.value!!.chapters[ch].steps.indexOfFirst { it.id == "kick_hear" })
        idle(100)
        assertEquals("kick_hear", c.ui.value!!.current.id)
        return c
    }

    @Test
    fun playingTheKickPadTicksListenToTheKick() {
        val c = onKickHear()
        val ear = TutoPadEar.heard
        assertTrue("the tutorial listens to the pads", ear != null)
        ear!!(TutoChecks.laneOf("snare"))
        idle(100)
        assertFalse("another pad: not the kick", "kick_hear" in c.ui.value!!.progress.done)
        ear(TutoChecks.laneOf("kick"))
        idle(100)
        assertTrue("the kick pad played: the kick heard", "kick_hear" in c.ui.value!!.progress.done)
    }

    @Test
    fun theTutorialQuitHearsNoPad() {
        val c = onKickHear()
        c.quit()
        assertEquals(null, TutoPadEar.heard)
    }
}
