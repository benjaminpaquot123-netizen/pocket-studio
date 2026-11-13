package com.pocketstudio.app.tuto

import android.os.Looper
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.guide.GuideListenPlayer
import com.pocketstudio.engine.OfflineExport
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
import kotlin.math.roundToInt

/**
 * Lot « tuto-flèches », control seats D a (« テクノ 132 BPM » chosen, the project at 120) and H a (« House 122 BPM »,
 * « 100 Tempo », « puis le tuto me fait régler 122 sans dire pourquoi c'était 100 »): the tempo step says the tempo
 * the project has, so that the number on the screen is no surprise; nothing more once the tempo is the style's.
 */
@RunWith(RobolectricTestRunner::class)
class Tour6TutoPanelTempoNoteTest {
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

    private fun theTempoStepSaysTheProjectsTempo(style: String) {
        val target = TutoTarget.of(style)!!
        val flow = MutableStateFlow(target.blank())
        val c = TutoController(
            scope, "p1", flow, edit = { f -> flow.value = flow.value.f() }, store = MemoryStore(),
            render = { FloatArray(OfflineExport.SAMPLE_RATE * 2) { 0.1f } }, player = Player(), debounceMs = 10,
        )
        idle()
        c.start(style, TutoLevel.Facile)
        idle()
        val chapters = c.ui.value!!.chapters
        val ch = chapters.indexOfFirst { x -> x.steps.any { it.check == "tempo" } }
        c.goTo(ch, chapters[ch].steps.indexOfFirst { it.check == "tempo" }); idle(100)
        val now = flow.value.tempoBpm.roundToInt()
        assertTrue("$style: a new project is not at the style's tempo (${target.style.bpm})", now != target.style.bpm.toInt())
        val note = c.ui.value!!.note.orEmpty()
        assertTrue("$style: the tempo step says the project's tempo ($now): « $note »", now.toString() in note)
        // The tempo set: nothing more to say.
        flow.value = flow.value.copy(tempoBpm = target.style.bpm)
        // The check runs off the main thread, after its debounce.
        val end = System.currentTimeMillis() + 10_000
        while (c.ui.value!!.note != null && System.currentTimeMillis() < end) { Thread.sleep(20); idle(20) }
        assertEquals("$style: the tempo is the style's", null, c.ui.value!!.note)
    }

    @Test
    fun houseTheTempoStepSaysTheProjectsTempo() = theTempoStepSaysTheProjectsTempo("house")

    @Test
    fun technoTheTempoStepSaysTheProjectsTempo() = theTempoStepSaysTheProjectsTempo("techno")
}
