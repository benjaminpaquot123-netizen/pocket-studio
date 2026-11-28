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
import com.pocketstudio.model.tuto.TutoPlay
import com.pocketstudio.model.tuto.TutoTarget
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.time.Duration

/**
 * Tour 9, F043 (t9-43): the bass track added from « + Piste » during the step that asks for it, the screen stayed on
 * the drums: the user had to find the track. Acceptance: après la création d'une piste par le tuto, l'écran ouvre
 * cette piste.
 */
@RunWith(RobolectricTestRunner::class)
class Tour9TutoNewTrackShownTest {
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

    /** Whether [ok] holds within [ms] (the looks answered). */
    private fun within(ms: Long, ok: () -> Boolean): Boolean {
        val end = System.currentTimeMillis() + ms
        while (!ok() && System.currentTimeMillis() < end) { idle(50); Thread.sleep(5) }
        return ok()
    }

    /** The project the steps before [id] make, as « Montre-moi » would have them. */
    private fun before(style: String, id: String): Project {
        val target = TutoTarget.of(style)!!
        var p = target.blank()
        for (s in TutoCourse.of(style)!!.chapters.flatMap { it.steps }) {
            if (s.id == id) return p
            if (!TutoChecks.done(s, p, target)) p = TutoChecks.show(s, p, target) ?: p
        }
        error("$style: no step $id")
    }

    @Test
    fun aTrackMadeForTheStepIsShown() {
        val checked = ArrayList<String>()
        for (style in TutoCourse.STYLES) {
            val target = TutoTarget.of(style)!!
            val step = TutoCourse.of(style)!!.chapters.flatMap { it.steps }.firstOrNull { it.check == "notes:bass" } ?: continue
            val flow = MutableStateFlow(before(style, step.id))
            val goes = ArrayList<TutoGo>()
            val c = TutoController(
                scope, "p-$style", flow, edit = { f -> flow.value = flow.value.f() }, store = MemoryStore(),
                render = { FloatArray(OfflineExport.SAMPLE_RATE * 2) { 0.1f } }, player = Player(), debounceMs = 10,
            )
            scope.launch { c.goes.collect { goes += it } }
            idle()
            c.start(style, TutoLevel.Facile)
            idle()
            val chapters = c.ui.value!!.chapters
            val ch = chapters.indexOfFirst { x -> x.steps.any { it.id == step.id } }
            c.goTo(ch, chapters[ch].steps.indexOfFirst { it.id == step.id }); idle(300)
            val need = c.ui.value!!.need as? TutoNeed.Track ?: continue
            val n = goes.size
            // The track made by the user from « + Piste » (the app's sheet, not the step's button).
            val (q, track) = TutoPlay.press(need, step, flow.value, target, TutoLevel.Facile)!!
            flow.value = q
            val made = track!!
            assertTrue(
                "$style/${step.id}: the track $made made, the screen stays where it was (${goes.drop(n)})",
                within(5_000) { goes.drop(n).any { it.notes == made } },
            )
            checked += "$style/${step.id}"
        }
        assertTrue("checked: $checked", checked.size >= 5)
    }
}
