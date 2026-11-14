package com.pocketstudio.app.tuto

import android.os.Looper
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.guide.GuideListenPlayer
import com.pocketstudio.model.Project
import com.pocketstudio.model.tuto.TutoChecks
import com.pocketstudio.model.tuto.TutoCourse
import com.pocketstudio.model.tuto.TutoLevel
import com.pocketstudio.model.tuto.TutoProgress
import com.pocketstudio.model.tuto.TutoTarget
import kotlinx.coroutines.CoroutineDispatcher
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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext

/**
 * CI 43448175188 (8879 tests, one red): `TutoLot5ControllerTest#aWrongOrderIsSaid`, « what is left said: not in time ».
 * The first look at the project (at the start, every step the project already holds) was still working on a busy
 * machine when the song's parts changed; the next look began with the progress as it was before the first one
 * answered, so it found those steps again, and a look that finds something never said what the order lacks. Played
 * here in that order: the looks held off the main thread until the parts have changed, then answered in turn.
 */
@RunWith(RobolectricTestRunner::class)
class Tour6TutoOrderRaceTest {
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

    /** The looks' worker, held until [open]: a machine too busy to answer before the next edit. */
    private class Held : CoroutineDispatcher() {
        val open = CountDownLatch(1)
        private val pool = Executors.newSingleThreadExecutor()
        override fun dispatch(context: CoroutineContext, block: Runnable) {
            pool.execute { open.await(); block.run() }
        }
    }

    private val scope = MainScope()
    private val target = TutoTarget.of("jump_up")!!
    private val steps = TutoCourse.of("jump_up")!!.chapters.flatMap { it.steps }.associateBy { it.id }

    @After
    fun end() = scope.cancel()

    private fun idle(ms: Long = 50) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))

    private fun upTo(vararg ids: String): Project {
        var p = target.blank()
        for (id in ids) if (!TutoChecks.done(steps.getValue(id), p, target)) p = TutoChecks.show(steps.getValue(id), p, target)!!
        return p
    }

    @Test
    fun whatTheOrderLacksIsSaidWhenAnEarlierLookAnswersLate() {
        val parts = upTo("kick_do", "snare_do", "hat_do", "bass_do", "pad_do", "lead_do", "groove_name", "make_intro", "make_montee", "make_drop", "make_break", "make_drop2", "make_fin")
        val flow = MutableStateFlow(parts)
        val held = Held()
        val c = TutoController(
            scope, "p1", flow, edit = { f -> flow.value = flow.value.f() }, store = MemoryStore(),
            render = { FloatArray(8) }, player = Player(), worker = held, debounceMs = 10,
        )
        idle()
        c.start("jump_up", TutoLevel.Facile)
        idle()
        val chapters = c.ui.value!!.chapters
        val ch = chapters.indexOfFirst { x -> x.steps.any { it.id == "song_order" } }
        c.goTo(ch, chapters[ch].steps.indexOfFirst { it.id == "song_order" })
        idle()
        assertEquals("song_order", c.ui.value!!.current.id)
        // Intro, Montée, Drop, Break: the next look begins while the first ones still work.
        val full = TutoChecks.show(steps.getValue("song_order"), parts, target)!!
        flow.value = full.copy(song = full.song.take(4))
        idle(200)
        held.open.countDown()
        val end = System.currentTimeMillis() + 10_000
        while (c.ui.value!!.made?.startsWith("Encore") != true && System.currentTimeMillis() < end) { idle(); Thread.sleep(5) }
        val made = c.ui.value!!.made
        assertTrue("what the order lacks is said: « $made »", made?.startsWith("Encore") == true)
    }
}
