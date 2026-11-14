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
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.time.Duration

/**
 * Tour 6, N10: the Facile course's chapter 9 ends on a step ticked by hand (« C'est fait », map_phrases): it went
 * straight on to chapter 10, never a « Bravo, chapitre fini ! ». A chapter's last step ticked by hand has its « Bravo ».
 */
@RunWith(RobolectricTestRunner::class)
class Tour6TutoPanelManualEndTest {
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
    fun aChaptersLastStepTickedByHandHasItsBravo() {
        val c = controller(MutableStateFlow(target.blank()))
        c.start(style, TutoLevel.Facile)
        idle()
        val chapters = c.ui.value!!.chapters
        // Chapter 9 of the Facile course (the structure): its last step is « C'est fait » (map_phrases).
        val ch = chapters.indexOfFirst { it.steps.any { s -> s.id == "map_phrases" } }
        assertEquals("chapter 9", 8, ch)
        val last = chapters[ch].steps.lastIndex
        assertEquals("map_phrases", chapters[ch].steps[last].id)
        assertTrue(chapters[ch].steps[last].manual)
        c.goTo(ch, last); idle()
        c.markDone(); idle()
        val ui = c.ui.value!!
        assertTrue("map_phrases ticked", "map_phrases" in ui.progress.done)
        assertEquals("the chapter's end stays shown", "map_phrases", ui.current.id)
        assertTrue("« Bravo » for the chapter's end", ui.bravo)
        // Then « Chapitre suivant »: the next chapter.
        c.next(); idle()
        assertEquals(ch + 1, c.ui.value!!.chapter)
    }
}
