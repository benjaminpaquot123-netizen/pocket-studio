package com.pocketstudio.app.tuto

import android.os.Looper
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.guide.GuideListenPlayer
import com.pocketstudio.engine.OfflineExport
import com.pocketstudio.model.Mixer
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
 * Tour 9, F042 (seat t9-34): on the clap's effects box, the reverb step's note spoke of « Accords » and
 * « Caisse » (the jump up's strips) for a step on the clap and the chords. The note names the step's strips.
 */
@RunWith(RobolectricTestRunner::class)
class T9L17SendNoteNamesStripsTest {
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
    fun end() { scope.cancel() }

    private fun idle(ms: Long = 50) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))

    @Test
    fun theHouseReverbNoteNamesTheClapNotTheSnare() {
        val target = TutoTarget.of("house")!!
        val song = (0 until Mixer.CHANNELS).fold(target.song) { p, ch ->
            p.copy(mixer = p.mixer.copy(channels = p.mixer.channels.mapIndexed { i, c -> if (i == ch) c.copy(sends = c.sends.map { 0f }) else c }))
        }
        val flow = MutableStateFlow<Project>(song)
        val c = TutoController(
            scope, "p1", flow, edit = { f -> flow.value = flow.value.f() }, store = MemoryStore(),
            render = { FloatArray(OfflineExport.SAMPLE_RATE) { 0.1f } }, player = Player(), debounceMs = 10,
        )
        idle()
        c.start("house", TutoLevel.Pro)
        val ch = c.ui.value!!.chapters.indexOfFirst { it.steps.any { s -> s.id == "reverb" } }
        c.goTo(ch, c.ui.value!!.chapters[ch].steps.indexOfFirst { it.id == "reverb" })
        idle(100)
        assertEquals("reverb", c.ui.value!!.current.id)
        val clap = com.pocketstudio.app.rhythm.padName(flow.value, TutoChecks.laneOf("clap"))
        val snare = com.pocketstudio.app.rhythm.padName(flow.value, TutoChecks.laneOf("snare"))
        val note = c.sendNote()
        assertTrue("the note names the clap: $note", note != null && note.contains(clap))
        assertFalse("the note does not name the snare: $note", note!!.contains(snare))
    }
}
