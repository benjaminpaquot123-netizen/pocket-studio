package com.pocketstudio.app.tuto

import android.os.Looper
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.guide.GuideListenPlayer
import com.pocketstudio.app.synth.dottedTouch
import com.pocketstudio.engine.OfflineExport
import com.pocketstudio.model.Note
import com.pocketstudio.model.NoteClip
import com.pocketstudio.model.Project
import com.pocketstudio.model.allScenes
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.time.Duration

/**
 * Lot « tuto 5 »: « Suivant » on a step left undone says what the song misses first; a wrong order
 * of parts is said; the Drop 2's button says its bass is the user's to write (not « sans Basse »);
 * the Montée's build is said; « Accords » on a dotted chord writes its 3 notes.
 */
@RunWith(RobolectricTestRunner::class)
class TutoLot5ControllerTest {
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

    private fun controller(flow: MutableStateFlow<Project>) =
        TutoController(
            scope, "p1", flow, edit = { f -> flow.value = flow.value.f() }, store = MemoryStore(),
            render = { FloatArray(OfflineExport.SAMPLE_RATE * 2) { 0.1f } }, player = Player(), debounceMs = 10,
        ).also { idle() }

    private fun goTo(c: TutoController, id: String) {
        val chapters = c.ui.value!!.chapters
        val ch = chapters.indexOfFirst { x -> x.steps.any { it.id == id } }
        c.goTo(ch, chapters[ch].steps.indexOfFirst { it.id == id })
        idle()
        assertEquals(id, c.ui.value!!.current.id)
    }

    private fun upTo(vararg ids: String, from: Project = target.blank()): Project {
        var p = from
        for (id in ids) if (!TutoChecks.done(steps.getValue(id), p, target)) p = TutoChecks.show(steps.getValue(id), p, target)!!
        return p
    }

    /** c/86, c/159: the tempo left at 120 and the melody skipped, not a word; the first « Suivant » says it, the second goes on. */
    @Test
    fun suivantOnAStepLeftUndoneSaysWhatItCostsFirst() {
        val flow = MutableStateFlow(target.blank())
        val c = controller(flow)
        c.start("jump_up", TutoLevel.Facile)
        idle()
        goTo(c, "tempo")
        c.next(); idle()
        assertEquals("still on the tempo", "tempo", c.ui.value!!.current.id)
        val said = c.ui.value!!.made
        assertNotNull(said)
        assertTrue(said!!, "120 BPM" in said && "« Suivant »" in said)
        assertTrue("the panel open to read it", c.ui.value!!.open)
        c.next(); idle()
        assertEquals("key", c.ui.value!!.current.id)
        assertNull("said no more on the next step", c.ui.value!!.made)
        // A step done, or one to listen to, goes on at once.
        c.goTo(0, 0); idle()
        c.next(); idle()
        assertEquals("tempo", c.ui.value!!.current.id)
        flow.value = flow.value.copy(tempoBpm = 174.0)
        await("tempo ticked") { "tempo" in c.ui.value!!.progress.done }
        c.next(); idle()
        assertEquals("key", c.ui.value!!.current.id)
    }

    /** c/202-224: seven parts in the order shown, no second Montée, not a word; what is wrong is said. */
    @Test
    fun aWrongOrderIsSaid() {
        val parts = upTo("kick_do", "snare_do", "hat_do", "bass_do", "pad_do", "lead_do", "groove_name", "make_intro", "make_montee", "make_drop", "make_break", "make_drop2", "make_fin")
        val flow = MutableStateFlow(parts)
        val c = controller(flow)
        c.start("jump_up", TutoLevel.Facile)
        idle()
        goTo(c, "song_order")
        val order = steps.getValue("song_order")
        val full = TutoChecks.show(order, parts, target)!!
        // Intro, Montée, Drop, Break: what is still to add is said.
        flow.value = full.copy(song = full.song.take(4))
        await("what is left said") { c.ui.value!!.made?.startsWith("Encore") == true }
        assertTrue(c.ui.value!!.made!!, "Montée" in c.ui.value!!.made!! && "Fin" in c.ui.value!!.made!!)
        // Then the Drop 2 right after the Break: the Montée missing, said.
        flow.value = full.copy(song = full.song.filterIndexed { i, _ -> i != 4 })
        await("the wrong place said") { c.ui.value!!.made?.startsWith("Pas dans l'ordre") == true }
        assertTrue(c.ui.value!!.made!!, "« Break »" in c.ui.value!!.made!! && "« Montée »" in c.ui.value!!.made!!)
        assertFalse("song_order" in c.ui.value!!.progress.done)
        // Right: ticked, the words gone.
        flow.value = full
        await("ticked") { "song_order" in c.ui.value!!.progress.done }
        assertNull(c.ui.value!!.made)
    }

    /** a/151, c/198, d/218: « Drop 2 créée : sans Basse » right before writing its bass; the button says it is the user's to write. */
    @Test
    fun theDropTwoSaysItsBassIsTheUsers() {
        val flow = MutableStateFlow(upTo("kick_do", "snare_do", "hat_do", "bass_do", "pad_do", "lead_do", "groove_name", "make_intro", "make_montee", "make_drop", "make_break"))
        val c = controller(flow)
        c.start("jump_up", TutoLevel.Facile)
        idle()
        goTo(c, "make_drop2")
        assertEquals(TutoNeed.Part("Drop 2"), c.ui.value!!.need)
        c.fulfilNeed(); idle(100)
        val made = c.ui.value!!.made!!
        assertFalse(made, "Basse" in made.substringBefore("."))
        assertTrue(made, "à toi d'écrire" in made)
        // Its bass written: « Bravo », the button's words gone.
        val step = steps.getValue("make_drop2")
        flow.value = TutoChecks.show(step, flow.value, target)!!
        await("ticked") { "make_drop2" in c.ui.value!!.progress.done }
        assertNull(c.ui.value!!.made)
    }

    /** b/247: the Montée's snare rewritten without a word; its button says it accelerates. */
    @Test
    fun theMonteeSaysItsBuild() {
        val flow = MutableStateFlow(upTo("kick_do", "snare_do", "hat_do", "bass_do", "pad_do", "lead_do", "groove_name", "make_intro"))
        val c = controller(flow)
        c.start("jump_up", TutoLevel.Pro)
        idle()
        goTo(c, "make_montee")
        c.fulfilNeed(); idle(100)
        assertTrue(c.ui.value!!.made!!, "accélère" in c.ui.value!!.made!!)
        assertTrue(flow.value.allScenes.any { it.label == "Montée" })
    }

    /** a/66-76, b/140: « Accords » on, the bottom dotted note wrote one note; it writes the dotted chord. */
    @Test
    fun theChordButtonWritesTheDottedChord() {
        val t = NoteClip.TICKS_PER_STEP
        val chord = listOf(Note(0, 16 * t, 65), Note(0, 16 * t, 68), Note(0, 16 * t, 72))
        val next = Note(16 * t, 16 * t, 65)
        val hints = chord + next
        val empty = NoteClip(steps = 64)
        assertEquals(chord.toSet(), dottedTouch(chord[0], hints, empty, chordMode = true).toSet())
        assertEquals(listOf(chord[0]), dottedTouch(chord[0], hints, empty, chordMode = false))
        // A note of it already there: the two others.
        assertEquals(chord.drop(1).toSet(), dottedTouch(chord[1], hints, empty.copy(notes = listOf(chord[0])), chordMode = true).toSet())
        // A lone dotted note (the lead's) with « Accords » left on: that note only.
        assertEquals(listOf(next), dottedTouch(next, hints, empty, chordMode = true))
    }
}
