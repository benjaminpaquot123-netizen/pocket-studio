package com.pocketstudio.app.tuto

import android.os.Looper
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.guide.GuideListenPlayer
import com.pocketstudio.model.Project
import com.pocketstudio.model.tuto.TutoChecks
import com.pocketstudio.model.tuto.TutoCourse
import com.pocketstudio.model.tuto.TutoLevel
import com.pocketstudio.model.tuto.TutoNeed
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
import kotlin.coroutines.CoroutineContext

/**
 * The work of the tutorial's looks at the project ([TutoController]'s worker), kept until the test lets it go, in the
 * order the test chooses and on the test's own thread: a phone too busy to answer before the next touch, played without
 * a clock (no thread, no pause: the same order at each run).
 */
internal class LooksGate : CoroutineDispatcher() {
    private val held = ArrayList<Runnable>()

    /** The most looks whose work waited here at the same time. */
    var most = 0
        private set

    val size: Int get() = held.size

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        held.add(block)
        most = maxOf(most, held.size)
    }

    /** The work of the look asked first ([newest]: last) done now; its answer waits on the main thread. */
    fun answer(newest: Boolean = false) {
        (if (newest) held.removeAt(held.lastIndex) else held.removeAt(0)).run()
    }
}

/**
 * CI 43425565244, 43448175188, 43449478539 (a tutorial test red at each run, never the same, on a machine slower than
 * ours): one cause. The tutorial looked at the project from several places at once (the edits once settled, a step
 * reached, its button touched, « Montre-moi » over), each look working off the main thread on the project and the
 * progress of the moment it began, each answer taken as it came, whatever had moved meanwhile and whatever the order.
 * On a slow phone an answer about a project that was no more took back what a newer one had said. Played here in the
 * orders that hurt, the looks' work held by [LooksGate].
 */
@RunWith(RobolectricTestRunner::class)
class Tour6TutoLooksTest {
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
    private val gate = LooksGate()

    @After
    fun end() = scope.cancel()

    private fun idle(ms: Long = 50) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))

    private fun controller(flow: MutableStateFlow<Project>) =
        TutoController(
            scope, "p1", flow, edit = { f -> flow.value = flow.value.f() }, store = MemoryStore(),
            render = { FloatArray(8) }, player = Player(), worker = gate, debounceMs = 10,
        ).also { idle() }

    /** Every look asked for is answered ([newest] first, or in the order asked) until the tutorial asks for no more. */
    private fun answerAll(newest: Boolean = false) {
        var quiet = 0
        var turns = 0
        while (quiet < 3) {
            idle()
            if (gate.size == 0) quiet++ else { quiet = 0; gate.answer(newest) }
            check(turns++ < 200) { "the tutorial never stops looking at the project" }
        }
    }

    private fun goTo(c: TutoController, id: String) {
        val chapters = c.ui.value!!.chapters
        val ch = chapters.indexOfFirst { x -> x.steps.any { it.id == id } }
        c.goTo(ch, chapters[ch].steps.indexOfFirst { it.id == id })
        assertEquals(id, c.ui.value!!.current.id)
    }

    /** House, the bass: its track made, the notes written a note at a time; the project one note short of « Copier », then with it. */
    private fun oneNoteShortOfTheCopy(): Triple<TutoTarget, Project, Project> {
        val target = TutoTarget.of("house")!!
        val step = TutoCourse.of("house")!!.chapters.flatMap { it.steps }.first { it.id == "bass_do" }
        var p = target.blank()
        TutoChecks.need(step, p, target)?.let { p = TutoChecks.fulfil(it, step, p, target)!!.first }
        val frames = listOf(p) + TutoChecks.demo(step, p, target)!!.frames
        val k = frames.indexOfFirst { TutoChecks.need(step, it, target) is TutoNeed.Copy }
        assertTrue("the bass written by hand reaches « Copier sur les mesures suivantes » (frame $k of ${frames.size})", k >= 1)
        return Triple(target, frames[k - 1], frames[k])
    }

    /**
     * CI 43449478539 (`Tour6TutoFlechesRollTest`, « Copier » never offered): the look begun when the step was reached
     * answers after the look at the motif just written. It said « nothing to copy » about the project of before, and
     * took the button back: no look came after it, the button stayed away until the next note.
     */
    @Test
    fun anAnswerAboutTheProjectOfBeforeDoesNotTakeTheCopyButtonBack() {
        val (_, before, written) = oneNoteShortOfTheCopy()
        val flow = MutableStateFlow(before)
        val c = controller(flow)
        c.start("house", TutoLevel.Facile)
        answerAll()
        // The step reached: a look begins. Its work is not done yet when the last note of the motif is written.
        goTo(c, "bass_do")
        idle()
        flow.value = written
        idle()
        answerAll(newest = true)
        assertEquals("the motif is written, the last look said so: « Copier sur les mesures suivantes »", TutoNeed.Copy("bass"), c.ui.value!!.need)
    }

    /**
     * CI 43448175188 (`TutoLot5ControllerTest#aWrongOrderIsSaid`, « what is left said: not in time »): the first look
     * (every step the project holds already) still works when the step is reached and the song's parts change. The
     * looks begun meanwhile found its steps again, and a look that finds something said nothing of the order.
     */
    @Test
    fun whatTheSongLacksIsSaidWhenTheFirstLookAnswersLate() {
        val target = TutoTarget.of("jump_up")!!
        val steps = TutoCourse.of("jump_up")!!.chapters.flatMap { it.steps }.associateBy { it.id }
        var parts = target.blank()
        for (id in listOf("kick_do", "snare_do", "hat_do", "bass_do", "pad_do", "lead_do", "groove_name", "make_intro", "make_montee", "make_drop", "make_break", "make_drop2", "make_fin")) {
            if (!TutoChecks.done(steps.getValue(id), parts, target)) parts = TutoChecks.show(steps.getValue(id), parts, target)!!
        }
        val flow = MutableStateFlow(parts)
        val c = controller(flow)
        c.start("jump_up", TutoLevel.Facile)
        idle()
        goTo(c, "song_order")
        idle()
        // Intro, Montée, Drop, Break.
        val full = TutoChecks.show(steps.getValue("song_order"), parts, target)!!
        flow.value = full.copy(song = full.song.take(4))
        idle()
        answerAll()
        val made = c.ui.value!!.made
        assertTrue("what the order lacks is said: « $made »", made?.startsWith("Encore") == true)
        assertTrue("the steps the project held are ticked", "make_fin" in c.ui.value!!.progress.done)
    }

    /** The rule itself: one look at a time (a second one asked meanwhile waits for the first one's answer). */
    @Test
    fun theLooksAreMadeOneAtATime() {
        val (_, before, written) = oneNoteShortOfTheCopy()
        val flow = MutableStateFlow(before)
        val c = controller(flow)
        c.start("house", TutoLevel.Facile)
        idle()
        goTo(c, "bass_do")
        idle()
        flow.value = written
        idle()
        c.fulfilNeed()
        idle()
        answerAll()
        assertEquals("looks working at the same time", 1, gate.most)
    }
}
