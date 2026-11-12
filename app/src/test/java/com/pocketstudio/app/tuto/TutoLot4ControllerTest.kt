package com.pocketstudio.app.tuto

import android.os.Looper
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.guide.GuideListenPlayer
import com.pocketstudio.engine.OfflineExport
import com.pocketstudio.model.InstrumentTrack
import com.pocketstudio.model.Mixer
import com.pocketstudio.model.Project
import com.pocketstudio.model.track
import com.pocketstudio.model.tuto.TutoChecks
import com.pocketstudio.model.tuto.TutoCourse
import com.pocketstudio.model.tuto.TutoLevel
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

/** Lot « tuto 4 »: « ta boucle, puis l'exemple » needs both heard; « Je le fais » goes to the half of a step still to do. */
@RunWith(RobolectricTestRunner::class)
class TutoLot4ControllerTest {
    private class MemoryStore : TutoStore {
        val saved = HashMap<String, TutoProgress>()
        override fun load(projectId: String) = saved[projectId]
        override fun save(projectId: String, progress: TutoProgress?) {
            if (progress == null) saved.remove(projectId) else saved[projectId] = progress
        }
    }

    private class Player : GuideListenPlayer {
        val played = ArrayList<Pcm>()
        override fun play(pcm: Pcm, ended: () -> Unit): Boolean { played += pcm; return true }
        override fun stop() {}
        override fun heard(): Long = played.lastOrNull()?.frames?.toLong() ?: 0L
    }

    private val scope = MainScope()
    private val target = TutoTarget.of("jump_up")!!
    private val steps = TutoCourse.of("jump_up")!!.chapters.flatMap { it.steps }.associateBy { it.id }

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

    private fun controller(flow: MutableStateFlow<Project>, player: Player = Player(), goes: MutableList<TutoGo> = ArrayList()) =
        TutoController(
            scope, "p1", flow, edit = { f -> flow.value = flow.value.f() }, store = MemoryStore(),
            render = { FloatArray(OfflineExport.SAMPLE_RATE * 2) { 0.1f } }, player = player, debounceMs = 10,
        ).also { c -> scope.launch { c.goes.collect { goes += it } }; idle() }

    /** The project with the drums' Montre-moi done (kick, snare, hats): « ta batterie » has something to hear. */
    private fun withDrums(): Project {
        var p = target.blank()
        for (id in listOf("kick_do", "snare_do", "hat_do")) p = TutoChecks.show(steps.getValue(id), p, target)!!
        return p
    }

    /** a/116: « Écoute ta boucle, puis l'exemple » ticked on the user's loop alone. */
    @Test
    fun theLoopThenTheExampleNeedsBoth() {
        val drumsHear = steps.getValue("drums_hear")
        assertTrue(drumsHear.hearMine)
        val flow = MutableStateFlow(withDrums())
        val c = controller(flow)
        c.start("jump_up", TutoLevel.Facile)
        c.goTo(c.ui.value!!.chapters.indexOfFirst { ch -> ch.steps.any { it.id == "drums_hear" } }, 1)
        assertEquals("drums_hear", c.ui.value!!.current.id)
        assertTrue(c.mineToHear())
        c.heardMine()
        idle(300)
        assertFalse("the loop alone", "drums_hear" in c.ui.value!!.progress.done)
        c.listen()
        await("then the example: ticked") { "drums_hear" in c.ui.value!!.progress.done }
        // The example first, then the loop: ticked too.
        val player = Player()
        val other = controller(MutableStateFlow(withDrums()), player)
        other.start("jump_up", TutoLevel.Facile)
        other.goTo(other.ui.value!!.chapters.indexOfFirst { ch -> ch.steps.any { it.id == "drums_hear" } }, 1)
        other.listen()
        await("example played") { player.played.isNotEmpty() }
        idle(500)
        assertFalse("the example alone", "drums_hear" in other.ui.value!!.progress.done)
        other.heardMine()
        await("then the loop: ticked") { "drums_hear" in other.ui.value!!.progress.done }
    }

    /** Nothing of the user's to hear yet: the example alone ticks it (as before). */
    @Test
    fun withNothingToHearTheExampleTicks() {
        val c = controller(MutableStateFlow(target.blank()))
        c.start("jump_up", TutoLevel.Facile)
        c.goTo(c.ui.value!!.chapters.indexOfFirst { ch -> ch.steps.any { it.id == "drums_hear" } }, 1)
        assertFalse(c.mineToHear())
        c.listen()
        await("ticked") { "drums_hear" in c.ui.value!!.progress.done }
    }

    /** b/256-265: the « Accords » send done, « Je le fais » went back there; now the snare's strip. */
    @Test
    fun doItGoesToTheHalfStillToDo() {
        var p = withDrums()
        p = TutoChecks.show(steps.getValue("pad_do"), p, target)!!
        val padCh = InstrumentTrack.mixerChannel(TutoChecks.trackFor(p, "pad")!!)
        val goes = ArrayList<TutoGo>()
        val flow = MutableStateFlow(p)
        val c = controller(flow, goes = goes)
        c.start("jump_up", TutoLevel.Pro)
        val fx = c.ui.value!!.chapters.indexOfFirst { it.id == "fx" }
        c.goTo(fx, 0)
        assertEquals("reverb", c.ui.value!!.current.id)
        c.doIt()
        await("went") { goes.isNotEmpty() }
        assertEquals(padCh, goes.last().effects)
        flow.value = p.copy(mixer = p.mixer.copy(channels = p.mixer.channels.mapIndexed { i, ch -> if (i == padCh) ch.copy(sends = listOf(0.3f, 0f)) else ch }))
        idle(300)
        c.doIt()
        await("went again") { goes.size >= 2 }
        assertEquals(Mixer.padChannel(TutoChecks.laneOf("snare")), goes.last().effects)
        assertEquals(0, goes.last().send)
    }

    /** b/196-217: the Drop 2's « Créer la partie » makes the part, then « Je le fais » goes to its bass's dotted notes. */
    @Test
    fun theDropTwoGoesToItsDottedBass() {
        var p = target.blank()
        for (id in listOf("kick_do", "snare_do", "hat_do", "bass_do", "pad_do", "lead_do", "groove_name", "make_intro", "make_montee", "make_drop", "make_break")) {
            if (!TutoChecks.done(steps.getValue(id), p, target)) p = TutoChecks.show(steps.getValue(id), p, target)!!
        }
        val goes = ArrayList<TutoGo>()
        val flow = MutableStateFlow(p)
        val c = controller(flow, goes = goes)
        c.start("jump_up", TutoLevel.Facile)
        val parts = c.ui.value!!.chapters.indexOfFirst { it.id == "parts" }
        c.goTo(parts, c.ui.value!!.chapters[parts].steps.indexOfFirst { it.id == "make_drop2" })
        await("its button") { c.ui.value!!.need != null }
        c.fulfilNeed()
        await("its notes") { goes.any { it.notes != null } }
        assertEquals(TutoChecks.trackFor(flow.value, "bass"), goes.last { it.notes != null }.notes)
        assertTrue("the bass is the user's to write", flow.value.track(TutoChecks.trackFor(flow.value, "bass")!!).notes.notes.isEmpty())
        assertFalse("make_drop2" in c.ui.value!!.progress.done)
    }
}
