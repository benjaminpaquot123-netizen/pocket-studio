package com.pocketstudio.app.tuto

import android.os.Looper
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.guide.GuideListenPlayer
import com.pocketstudio.model.Project
import com.pocketstudio.model.track
import com.pocketstudio.model.tuto.TutoChecks
import com.pocketstudio.model.tuto.TutoCourse
import com.pocketstudio.model.tuto.TutoLevel
import com.pocketstudio.model.tuto.TutoNeed
import com.pocketstudio.model.tuto.TutoProgress
import com.pocketstudio.model.tuto.TutoTarget
import com.pocketstudio.model.withTrack
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext

/**
 * The common cause of three tutorial classes that turned red now and then on the CI (Tour6TutoPanelAlreadyTest at
 * noon, TutoLot5ControllerTest#aWrongOrderIsSaid at 4 pm, Tour6TutoFlechesRollTest on remix2's run 43449478539):
 * the controller's looks at the project run off the main thread, several at once (the edits' own, and the one each
 * button launches), and a slow one could answer AFTER a newer one, putting back what an older project needed. In the
 * roll test: « Copier » offered by the look at the written motif, then taken back by the late look launched when the
 * track was made, never offered again. Played here in that order: the button's look held until the newer one answered.
 */
@RunWith(RobolectricTestRunner::class)
class Tour6TutoLateAnswerTest {
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

    /** The looks' worker; a look dispatched while [holding] waits for [open] (a busy machine), the others run at once. */
    private class Late : CoroutineDispatcher() {
        @Volatile var holding = false
        val open = CountDownLatch(1)
        private val pool = Executors.newCachedThreadPool()
        override fun dispatch(context: CoroutineContext, block: Runnable) {
            val wait = holding
            pool.execute { if (wait) open.await(); block.run() }
        }
    }

    private val scope = MainScope()
    private val style = "jump_up"
    private val target = TutoTarget.of(style)!!
    private val bass = TutoCourse.of(style)!!.chapters.flatMap { it.steps }.first { it.id == "bass_do" }

    @After
    fun end() = scope.cancel()

    private fun idle(ms: Long = 50) = repeat(5) { shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms)); Thread.sleep(5) }

    @Test
    fun aLateLookNeverTakesBackWhatANewerOneOffered() {
        val flow = MutableStateFlow<Project>(target.blank())
        val late = Late()
        val c = TutoController(
            scope, "p1", flow, edit = { f -> flow.value = flow.value.f() }, store = MemoryStore(),
            render = { FloatArray(8) }, player = Player(), worker = late, debounceMs = 10,
        )
        idle()
        c.start(style, TutoLevel.Facile)
        idle()
        val chapter = c.ui.value!!.chapters.indexOfFirst { ch -> ch.steps.any { it.id == "bass_do" } }
        c.goTo(chapter, c.ui.value!!.chapters[chapter].steps.indexOfFirst { it.id == "bass_do" })
        idle()
        // « Créer la piste Basse »: its own look is slow to answer.
        late.holding = true
        c.fulfilNeed()
        shadowOf(Looper.getMainLooper()).idle()
        late.holding = false
        idle()
        // The motif written by hand: « Copier » is what the step needs now.
        val q = flow.value
        val (track, first) = TutoChecks.ghostNotes(bass, q, target)!!
        flow.value = q.withTrack(q.track(track).copy(notes = q.track(track).notes.copy(notes = first)))
        idle()
        assertEquals("setup: the look at the written motif offers « Copier »", TutoNeed.Copy("bass"), c.ui.value!!.need)
        // The look launched by « Créer la piste » answers now, about the project before the motif.
        late.open.countDown()
        idle()
        assertEquals("a late look took « Copier » back", TutoNeed.Copy("bass"), c.ui.value!!.need)
    }
}
