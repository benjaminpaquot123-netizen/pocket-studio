package com.pocketstudio.app.tuto

import android.os.Looper
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.guide.GuideListenPlayer
import com.pocketstudio.engine.OfflineExport
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
import com.pocketstudio.model.DrumPattern
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.time.Duration

/**
 * Tour 7, L10 / N26 (seat 1d #45): « Der ganze Groove spielt. » over a Groove with nothing in it. What the step and
 * « Partie … créée » say is what the part made holds: never « tout le Groove joue » when the Groove is empty; the
 * part that keeps everything names what is in it.
 */
@RunWith(RobolectricTestRunner::class)
class Tour7TutoPartMadeTest {
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

    @Test
    fun aPartThatKeepsEverythingSaysWhatPlaysInIt() {
        val style = "techno"
        val target = TutoTarget.of(style)!!
        val steps = TutoCourse.of(style)!!.chapters.flatMap { it.steps }.associateBy { it.id }
        // A Groove with a kick and nothing else (the user went on without the rest).
        var p = target.blank()
        for (id in listOf("kick_do", "make_intro", "make_intro2")) if (!TutoChecks.done(steps.getValue(id), p, target)) p = TutoChecks.show(steps.getValue(id), p, target)!!
        val flow = MutableStateFlow(p)
        val c = TutoController(
            scope, "p1", flow, edit = { f -> flow.value = flow.value.f() }, store = MemoryStore(),
            render = { FloatArray(OfflineExport.SAMPLE_RATE * 2) { 0.1f } }, player = Player(), debounceMs = 10,
        )
        idle()
        c.start(style, TutoLevel.Facile)
        idle()
        val chapters = c.ui.value!!.chapters
        val ch = chapters.indexOfFirst { x -> x.steps.any { it.id == "make_drop" } }
        c.goTo(ch, chapters[ch].steps.indexOfFirst { it.id == "make_drop" }); idle()
        // Tour 7 (R-C): a Groove that poor is first offered what the chapters before put down; the part comes after.
        repeat(3) { if (c.ui.value!!.need is TutoNeed.Course) { c.fulfilNeed(); idle(100) } }
        assertEquals(TutoNeed.Part("Drop"), c.ui.value!!.need)
        c.fulfilNeed(); idle(100)
        val made = c.ui.value!!.made.orEmpty()
        assertTrue("« $made »", "Drop" in made)
        assertTrue("« $made » does not say the Drop holds the kick", "Kick" in made)
    }

    @Test
    fun noConsigneSaysTheWholeGroovePlays() {
        // The step can't know what the Groove holds: it says what the part is made from (a copy), not what plays.
        val says = TutoCourse.STYLES.flatMap { s -> TutoLevel.entries.flatMap { l -> TutoCourse.of(s)!!.chaptersFor(l).flatMap { it.steps }.map { "$s/${it.id}" to it.textFor(l) } } }
            .filter { Regex("""tout le Groove (?:y )?joue""").containsMatchIn(it.second) }.distinct()
        assertTrue("$says", says.isEmpty())
    }

    @Test
    fun anEmptyGrooveMakesNoEmptyList() {
        // Review of the lot: a Groove with nothing to name must never read « Dedans : . » (the part is built from the recipe).
        val style = "techno"
        val target = TutoTarget.of(style)!!
        val steps = TutoCourse.of(style)!!.chapters.flatMap { it.steps }.associateBy { it.id }
        var p = target.blank()
        for (id in listOf("kick_do", "make_intro", "make_intro2")) if (!TutoChecks.done(steps.getValue(id), p, target)) p = TutoChecks.show(steps.getValue(id), p, target)!!
        // Then every hit is wiped and only a chord grid is left (no note, no pad): the Groove is not empty, yet
        // nothing in it has a name to list.
        fun DrumPattern.wiped() = copy(lanes = lanes.map { it.copy(hits = emptyMap()) })
        p = p.copy(drums = p.drums.wiped(), scenes = p.scenes.map { it.copy(drums = it.drums.wiped(), chords = listOf(0, 5, 3, 4)) })
        assertTrue(p.scenes.all { s -> TutoChecks.bricks(s).isEmpty() && !s.isEmpty })
        val flow = MutableStateFlow(p)
        val c = TutoController(
            scope, "p1", flow, edit = { f -> flow.value = flow.value.f() }, store = MemoryStore(),
            render = { FloatArray(OfflineExport.SAMPLE_RATE * 2) { 0.1f } }, player = Player(), debounceMs = 10,
        )
        idle()
        c.start(style, TutoLevel.Facile)
        idle()
        val chapters = c.ui.value!!.chapters
        val ch = chapters.indexOfFirst { x -> x.steps.any { it.id == "make_drop" } }
        c.goTo(ch, chapters[ch].steps.indexOfFirst { it.id == "make_drop" }); idle()
        assertEquals(TutoNeed.Part("Drop"), c.ui.value!!.need)
        c.fulfilNeed(); idle(100)
        val made = c.ui.value!!.made.orEmpty()
        assertTrue("« $made »", "Drop" in made)
        assertFalse("« $made » lists nothing", "Dedans : ." in made || "Dedans :  " in made)
    }
}
