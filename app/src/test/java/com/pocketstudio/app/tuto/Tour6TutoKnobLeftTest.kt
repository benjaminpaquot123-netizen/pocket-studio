package com.pocketstudio.app.tuto

import android.os.Looper
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.guide.GuideListenPlayer
import com.pocketstudio.model.Project
import com.pocketstudio.model.SynthPatch
import com.pocketstudio.model.track
import com.pocketstudio.model.tuto.TutoChecks
import com.pocketstudio.model.tuto.TutoCourse
import com.pocketstudio.model.tuto.TutoLevel
import com.pocketstudio.model.tuto.TutoProgress
import com.pocketstudio.model.tuto.TutoTarget
import com.pocketstudio.model.withTrack
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.time.Duration

/**
 * Tour 6, control seat Bb-3 (de, Pro, chapter 7, techno « lead_knobs », on 4cb88f55): at +7 on the Cutoff nothing
 * said what was missing. While a knob the step names is not up enough, the step says by how much: « Coupure : encore +8 ».
 */
@RunWith(RobolectricTestRunner::class)
class Tour6TutoKnobLeftTest {
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
    fun aKnobNotUpEnoughSaysHowMuchIsLeft() {
        val style = "techno"
        val target = TutoTarget.of(style)!!
        val steps = TutoCourse.of(style)!!.chaptersFor(TutoLevel.Pro).flatMap { it.steps }
        var p = target.blank()
        for (s in steps) {
            if (s.id == "lead_knobs") break
            if (!TutoChecks.done(s, p, target)) p = TutoChecks.show(s, p, target) ?: p
        }
        val lead = TutoChecks.trackFor(p, "lead")!!
        // A plain synth track, the knobs at a round « 40 » when the step begins.
        p = p.withTrack(p.track(lead).copy(engine = 0, instrument = null, synth = SynthPatch().with(SynthPatch.CUTOFF, 0.40f).with(SynthPatch.RESONANCE, 0.40f)))
        val flow = MutableStateFlow<Project>(p)
        val c = TutoController(
            scope, "p1", flow, edit = { f -> flow.value = flow.value.f() }, store = MemoryStore(),
            render = { FloatArray(8) }, player = Player(), debounceMs = 10,
        )
        idle()
        c.start(style, TutoLevel.Pro)
        idle()
        val chapter = c.ui.value!!.chapters.indexOfFirst { ch -> ch.steps.any { it.id == "lead_knobs" } }
        c.goTo(chapter, c.ui.value!!.chapters[chapter].steps.indexOfFirst { it.id == "lead_knobs" })
        idle()
        // The Cutoff up by 7 (« 47 »), the Resonance up by 20 (« 60 »).
        flow.value = flow.value.withTrack(flow.value.track(lead).let { t -> t.copy(synth = t.synth.with(SynthPatch.CUTOFF, 0.47f).with(SynthPatch.RESONANCE, 0.60f)) })
        idle()
        val ui = c.ui.value!!
        assertFalse("not done at +7", "lead_knobs" in ui.progress.done)
        val note = ui.note.orEmpty()
        assertTrue("the step says what is left on the Cutoff: « $note »", note.contains("Coupure") && note.contains("+8"))
        assertFalse("the Resonance, up enough, is not named: « $note »", note.contains("Résonance"))
    }
}
