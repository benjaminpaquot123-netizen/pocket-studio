package com.pocketstudio.app.tuto

import android.os.Looper
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.guide.GuideListenPlayer
import com.pocketstudio.model.Project
import com.pocketstudio.model.tuto.TutoLevel
import com.pocketstudio.model.tuto.TutoProgress
import com.pocketstudio.model.tuto.TutoTarget
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.time.Duration

/**
 * Tour 6, review of lot « tuto-flèches » (TutoController.noteFor): the tempo step says « Un morceau neuf démarre à
 * %d BPM » with the tempo the project has NOW. A user who stopped half-way (110 on the way from 100 to 122, House)
 * read « démarre à 110 BPM », which is false: a new song starts at 100. The note names the tempo a new song starts at.
 */
@RunWith(RobolectricTestRunner::class)
class Tour6TutoTempoNoteTest {
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

    private fun idle() = repeat(10) { shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(50)); Thread.sleep(5) }

    @Test
    fun theTempoNoteNamesTheStartNotATempoHalfWay() {
        val style = "house"
        val target = TutoTarget.of(style)!!
        val flow = MutableStateFlow<Project>(target.blank())
        val c = TutoController(
            scope, "p1", flow, edit = { f -> flow.value = flow.value.f() }, store = MemoryStore(),
            render = { FloatArray(8) }, player = Player(), debounceMs = 10,
        )
        idle()
        c.start(style, TutoLevel.Facile)
        idle()
        c.goTo(0, c.ui.value!!.chapters[0].steps.indexOfFirst { it.id == "tempo" })
        idle()
        val start = Math.round(flow.value.tempoBpm).toInt()
        assertTrue("a new House song starts away from 122 ($start)", start != 122)
        assertTrue("the note names the start: « ${c.ui.value!!.note} »", c.ui.value!!.note.orEmpty().contains("$start BPM"))
        // Half-way to the style's tempo, the step not done yet.
        flow.value = flow.value.copy(tempoBpm = 110.0)
        idle()
        val note = c.ui.value!!.note.orEmpty()
        assertTrue("the step is not done at 110", "tempo" !in c.ui.value!!.progress.done)
        assertTrue("half-way, the note still names the start ($start), not 110: « $note »", note.contains("$start BPM") && !note.contains("110"))
    }
}
