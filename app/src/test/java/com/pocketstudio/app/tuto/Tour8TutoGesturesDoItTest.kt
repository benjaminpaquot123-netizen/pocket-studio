package com.pocketstudio.app.tuto

import android.os.Looper
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.guide.GuideListenPlayer
import com.pocketstudio.engine.OfflineExport
import com.pocketstudio.model.AutomationLane
import com.pocketstudio.model.Project
import com.pocketstudio.model.allScenes
import com.pocketstudio.model.switchScene
import com.pocketstudio.model.tuto.TutoChecks
import com.pocketstudio.model.tuto.TutoLevel
import com.pocketstudio.model.tuto.TutoPlay
import com.pocketstudio.model.tuto.TutoProgress
import com.pocketstudio.model.tuto.TutoTarget
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
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
 * Tour 8, lot T0b (seat e, ch.13): « Je le fais » on an effect step only opened the effect sheet; the send stayed at
 * 0 % and the curve was not drawn. Acceptance: « je touche « Ich mach's » et l'envoi passe à la valeur demandée, la
 * courbe est tracée, l'étape se coche ».
 */
@RunWith(RobolectricTestRunner::class)
class Tour8TutoGesturesDoItTest {
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

    private fun controller(flow: MutableStateFlow<Project>, goes: MutableList<TutoGo>) =
        TutoController(
            scope, "p1", flow, edit = { f -> flow.value = flow.value.f() }, store = MemoryStore(),
            render = { FloatArray(OfflineExport.SAMPLE_RATE * 2) { 0.1f } }, player = Player(), debounceMs = 10,
        ).also { c -> scope.launch { c.goes.collect { goes += it } }; idle() }

    /** The whole example song with no send and no curve: what a user has before the effects chapter. */
    private fun bare(target: TutoTarget): Project {
        val s = target.song
        return s.copy(automation = emptyList(), mixer = s.mixer.copy(channels = s.mixer.channels.map { ch -> ch.copy(sends = ch.sends.map { 0f }) }))
    }

    private fun stepOf(c: TutoController, id: String) {
        val chapters = c.ui.value!!.chapters
        val ch = chapters.indexOfFirst { ch -> ch.steps.any { it.id == id } }
        c.goTo(ch, chapters[ch].steps.indexOfFirst { it.id == id })
        idle()
        assertEquals(id, c.ui.value!!.current.id)
    }

    private fun doItUntilTicked(c: TutoController, target: TutoTarget, id: String, flow: MutableStateFlow<Project>, goes: List<TutoGo>) {
        repeat(4) {
            if (id in c.ui.value!!.progress.done) return
            val before = flow.value
            val n = goes.size
            c.doIt()
            await("$id: went") { goes.size > n }
            // The tick comes from a look made after the edit: waited for, not read at once.
            await("$id: « Je le fais » changed nothing in the project") { flow.value != before || id in c.ui.value!!.progress.done }
            val step = c.ui.value!!.current
            if (TutoPlay.done(step, flow.value, target, null)) await("$id: ticked once made") { id in c.ui.value!!.progress.done }
        }
        assertTrue("$id: ticked", id in c.ui.value!!.progress.done)
    }

    @Test
    fun theReverbsDoItSetsTheSendOfTheStripItGoesTo() {
        for (style in listOf("techno", "hard_techno", "retro_techno", "industrial_techno", "jump_up")) {
            val target = TutoTarget.of(style)!!
            val flow = MutableStateFlow(bare(target))
            val goes = ArrayList<TutoGo>()
            val c = controller(flow, goes)
            c.start(style, TutoLevel.Pro)
            idle()
            stepOf(c, "reverb")
            val n = goes.size
            c.doIt()
            await("$style: went") { goes.size > n }
            idle(300)
            val strip = goes.last().effects!!
            assertEquals("$style: the send it shows", 0, goes.last().send)
            assertTrue("$style: the send of strip $strip is set", flow.value.mixer.channels[strip].sends[0] >= 0.25f)
            doItUntilTicked(c, target, "reverb", flow, goes)
        }
    }

    @Test
    fun theFiltersDoItDrawsTheCurve() {
        for (style in listOf("techno", "hard_techno", "acid_techno")) {
            val target = TutoTarget.of(style)!!
            val flow = MutableStateFlow(bare(target))
            val goes = ArrayList<TutoGo>()
            val c = controller(flow, goes)
            c.start(style, TutoLevel.Pro)
            idle()
            stepOf(c, "filter")
            doItUntilTicked(c, target, "filter", flow, goes)
            assertTrue("$style: a curve", flow.value.automation.isNotEmpty())
        }
    }
    /**
     * Review of T0b (made grave by the chef): « Je le fais » on a curve step took the whole project the show made,
     * so anything else in it, the user's work included, came from the show and not from the project. Only the curve
     * and the filter it needs on its strip may change.
     */
    @Test
    fun theCurvesDoItChangesOnlyTheCurveAndItsFilter() {
        for (style in listOf("techno", "hard_techno", "acid_techno")) {
            val target = TutoTarget.of(style)!!
            val step = com.pocketstudio.model.tuto.TutoCourse.of(style)!!.chaptersFor(TutoLevel.Pro)
                .flatMap { it.steps }.first { it.id == "filter" }
            // A field of the project the show's clean-up rewrites (a style name longer than it keeps): it stands
            // for anything the show would hand back other than the project it was given.
            val own = bare(target).copy(style = "x".repeat(80))
            val made = TutoPlay.doHere(step, own, target, null, TutoLevel.Pro)
            assertTrue("$style: a curve made", made != null && made.automation.isNotEmpty())
            made!!
            val ch = made.automation.map { it.target.ch }.toSet()
            // A loop curve (« Par partie », control e of ch.13) also shows the part it is drawn on, nothing else.
            val shown = if (step.check == "automation:loop") own.switchScene(own.allScenes.indexOfFirst { it.label == "Drop" }) else own
            val expected = shown.copy(
                automation = made.automation,
                mixer = shown.mixer.copy(channels = shown.mixer.channels.mapIndexed { i, x -> if (i in ch) x.copy(inserts = made.mixer.channels[i].inserts) else x }),
            )
            assertEquals("$style: « Je le fais » changed more than the curve and its filter", expected, made)
        }
    }

    /**
     * Control seat e of T0b (e-ch13, grave): at chapter 13 step 2 of the Pro course, « Je le fais » ticked the step
     * but left the lane in « Morceau » (song) mode with no loop curve to see, while the step asks for « Par partie »
     * and a curve drawn on the Drop's 1-bar loop that rises to the middle and falls back. Three things are asked:
     * the mode the step names, the curve drawn on the Drop's loop, and a tick only once that gesture is really made.
     */
    @Test
    fun theLoopCurveDoItDrawsItPerPartOnTheDropLoop() {
        for (style in listOf("techno", "acid_techno")) {
            val target = TutoTarget.of(style)!!
            val step = com.pocketstudio.model.tuto.TutoCourse.of(style)!!.chaptersFor(TutoLevel.Pro)
                .flatMap { it.steps }.first { it.id == "filter" }
            val own = bare(target)
            assertFalse("$style: ticked before anything is drawn", TutoPlay.done(step, own, target, null))
            val made = TutoPlay.doHere(step, own, target, null, TutoLevel.Pro)
            assertTrue("$style: a curve made", made != null)
            made!!
            val drop = made.allScenes.indexOfFirst { it.label == "Drop" }
            assertTrue("$style: a Drop", drop >= 0)
            val lane = made.automation.single { it !in own.automation }
            assertEquals("$style: the mode the step asks (« Par partie »)", AutomationLane.PER_SCENE, lane.mode)
            assertEquals("$style: the Drop shown, so the curve drawn is the one on screen", drop, made.scene)
            val loop = lane.sceneValues[drop].orEmpty()
            val bar = made.stepsPerBar
            val first = loop.take(bar)
            assertTrue("$style: the Drop's first bar starts low", (first.firstOrNull() ?: 1f) <= 0.3f)
            assertTrue("$style: it rises to the middle of the bar", (first.getOrNull(bar / 2) ?: 0f) >= 0.9f)
            assertTrue("$style: and falls back by the end of the bar", (first.getOrNull(bar - 1) ?: 1f) <= 0.3f)
            assertTrue("$style: ticked once drawn", TutoPlay.done(step, made, target, null))
            // The song-wide wave the old « Je le fais » drew: a curve in another mode than the step asks is no tick.
            val songWide = TutoPlay.doHere(step.copy(check = "automation:wave"), own, target, null, TutoLevel.Pro)!!
            assertTrue("$style: the song-wide one is in song mode", songWide.automation.any { it.mode == AutomationLane.SONG })
            assertFalse("$style: a song-wide curve ticks a « Par partie » loop step", TutoPlay.done(step, songWide, target, null))
        }
    }
}
