package com.pocketstudio.app.tuto

import android.os.Looper
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.guide.GuideListenPlayer
import com.pocketstudio.engine.OfflineExport
import com.pocketstudio.model.Project
import com.pocketstudio.model.tuto.TutoChecks
import com.pocketstudio.model.tuto.TutoCourse
import com.pocketstudio.model.tuto.TutoLevel
import com.pocketstudio.model.tuto.TutoNeed
import com.pocketstudio.model.tuto.TutoProgress
import com.pocketstudio.model.tuto.TutoTarget
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import com.pocketstudio.model.switchScene
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.time.Duration

/**
 * Tour 6, N26: « Créer la partie « Intro 2 » » said « tout le Groove y joue » while its step says « tout, sauf la ride
 * et le riff » (the Groove had neither yet): what the button says never contradicts the step.
 */
@RunWith(RobolectricTestRunner::class)
class Tour6TutoPanelPartMadeTest {
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
    private val steps = TutoCourse.of(style)!!.chapters.flatMap { it.steps }.associateBy { it.id }

    @After
    fun end() = scope.cancel()

    private fun idle(ms: Long = 50) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))

    private fun upTo(vararg ids: String): Project {
        var p = target.blank()
        for (id in ids) if (!TutoChecks.done(steps.getValue(id), p, target)) p = TutoChecks.show(steps.getValue(id), p, target)!!
        return p
    }

    @Test
    fun aPartThatLeavesBricksOutIsNotSaidTheWholeGroove() {
        // The Facile Groove: no ride, no riff written.
        val flow = MutableStateFlow(upTo("kick_do", "clap_do", "open_hat_do", "perc_do", "bass_do", "pad_do", "groove_name", "make_intro"))
        val c = TutoController(
            scope, "p1", flow, edit = { f -> flow.value = flow.value.f() }, store = MemoryStore(),
            render = { FloatArray(OfflineExport.SAMPLE_RATE * 2) { 0.1f } }, player = Player(), debounceMs = 10,
        )
        idle()
        c.start(style, TutoLevel.Facile)
        idle()
        val chapters = c.ui.value!!.chapters
        val ch = chapters.indexOfFirst { x -> x.steps.any { it.id == "make_intro2" } }
        c.goTo(ch, chapters[ch].steps.indexOfFirst { it.id == "make_intro2" }); idle()
        assertEquals(TutoNeed.Part("Intro 2"), c.ui.value!!.need)
        c.fulfilNeed(); idle(100)
        val made = c.ui.value!!.made.orEmpty()
        assertFalse("« $made » while the step leaves the ride and the riff out", "tout le Groove" in made)
    }

    /**
     * Lot « tuto-flèches », control seat D a (chapter 10, the song playing): « Partie « Intro 2 » créée … : sans Rumble,
     * Clap, Basse, Accords, Mélodie » under « La basse et le stab arrivent » — the bricks of the part made just before.
     * While the song plays, the screen stays on the part heard (the new one comes at the next bar): what was made is
     * read from the part made, not from the part shown.
     */
    @Test
    fun whatAPartLeavesOutIsReadFromThePartMadeNotFromTheOneShown() {
        val flow = MutableStateFlow(upTo("kick_do", "rumble_do", "clap_do", "open_hat_do", "perc_do", "bass_do", "bass_end", "pad_do", "pad_end", "lead_do", "groove_name", "make_intro"))
        // As the app does while the song plays (RhythmViewModel.tutoEdit): the part heard stays the one shown.
        val c = TutoController(
            scope, "p1", flow, edit = { f -> val before = flow.value; val after = before.f(); flow.value = if (after.scene != before.scene) after.switchScene(before.scene) else after },
            store = MemoryStore(), render = { FloatArray(OfflineExport.SAMPLE_RATE * 2) { 0.1f } }, player = Player(), debounceMs = 10,
        )
        idle()
        c.start(style, TutoLevel.Facile)
        idle()
        val chapters = c.ui.value!!.chapters
        val ch = chapters.indexOfFirst { x -> x.steps.any { it.id == "make_intro2" } }
        c.goTo(ch, chapters[ch].steps.indexOfFirst { it.id == "make_intro2" }); idle()
        assertEquals(TutoNeed.Part("Intro 2"), c.ui.value!!.need)
        val shown = flow.value.scene
        c.fulfilNeed(); idle(100)
        assertEquals("the part heard is still the one shown", shown, flow.value.scene)
        val made = c.ui.value!!.made.orEmpty()
        assertTrue("« $made »", "Intro 2" in made)
        // The step: « tout, sauf la ride et le riff. La basse et le stab arrivent. »
        for (kept in listOf("Basse", "Accords", "Clap")) assertFalse("« $made » says « $kept » left out of the Intro 2, which keeps it", kept in made.substringAfter("sans", ""))
    }
}
