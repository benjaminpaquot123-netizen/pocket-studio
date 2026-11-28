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
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.time.Duration

/**
 * Tour 9, F037 (t9-42, t9-55): « Créer la partie », « Bravo », then Annuler took the part away and the step stayed
 * ticked with its « Bravo »; « Suivant » went on. Acceptance: si j'annule la création d'une partie, l'étape redevient
 * à faire.
 */
@RunWith(RobolectricTestRunner::class)
class Tour9TutoUndoMakesItToDoAgainTest {
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

    private fun await(what: String, ok: () -> Boolean) {
        val end = System.currentTimeMillis() + 20_000
        while (!ok()) {
            idle(50)
            check(System.currentTimeMillis() < end) { "$what: not in time" }
            Thread.sleep(5)
        }
    }

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
    fun undoingThePartMakesTheStepToDoAgain() {
        for ((style, id) in listOf("techno" to "make_intro", "jungle" to "make_drop2")) {
            val flow = MutableStateFlow(before(style, id))
            val c = TutoController(
                scope, "p-$style", flow, edit = { f -> flow.value = flow.value.f() }, store = MemoryStore(),
                render = { FloatArray(OfflineExport.SAMPLE_RATE * 2) { 0.1f } }, player = Player(), debounceMs = 10,
            )
            idle()
            c.start(style, TutoLevel.Facile)
            idle()
            val chapters = c.ui.value!!.chapters
            val ch = chapters.indexOfFirst { x -> x.steps.any { it.id == id } }
            c.goTo(ch, chapters[ch].steps.indexOfFirst { it.id == id }); idle()
            assertEquals(id, c.ui.value!!.current.id)
            assertTrue("$style/$id: « Créer la partie » offered, not ${c.ui.value!!.need}", c.ui.value!!.need is TutoNeed.Part)
            val undo = flow.value
            c.fulfilNeed()
            await("$style/$id: Bravo once the part is made") { id in c.ui.value!!.progress.done && c.ui.value!!.bravo }
            // Annuler: the project as it was before the part.
            flow.value = undo
            idle(300)
            assertTrue(
                "$style/$id: still ticked with « Bravo » once the part is undone",
                within(5_000) { id !in c.ui.value!!.progress.done && !c.ui.value!!.bravo },
            )
            assertEquals("$style/$id: still the step shown", id, c.ui.value!!.current.id)
            assertTrue("$style/$id: « Créer la partie » offered again, not ${c.ui.value!!.need}", c.ui.value!!.need is TutoNeed.Part)
        }
    }
}
