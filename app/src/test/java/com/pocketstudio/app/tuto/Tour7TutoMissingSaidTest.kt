package com.pocketstudio.app.tuto

import android.os.Looper
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.guide.GuideListenPlayer
import com.pocketstudio.app.rhythm.padName
import com.pocketstudio.engine.OfflineExport
import com.pocketstudio.model.allScenes
import com.pocketstudio.model.tuto.TutoChecks
import com.pocketstudio.model.tuto.TutoCourse
import com.pocketstudio.model.tuto.TutoLevel
import com.pocketstudio.model.tuto.TutoNeed
import com.pocketstudio.model.tuto.TutoPlay
import com.pocketstudio.model.tuto.TutoProgress
import com.pocketstudio.model.tuto.TutoTarget
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.time.Duration

/**
 * Tour 7, audit « Fable », fresh review (M2): « Poser ce qui manque » changed the song and said nothing (« Créer la
 * partie » says what it made). It says what it laid, « Posé : … », by the names of the lines it really changed.
 */
@RunWith(RobolectricTestRunner::class)
class Tour7TutoMissingSaidTest {
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
    fun whatWasLaidIsSaidByName() {
        val style = "techno"
        val target = TutoTarget.of(style)!!
        val steps = TutoCourse.of(style)!!.chapters.flatMap { it.steps }.associateBy { it.id }
        // A Groove with a kick and nothing else (the user went on without the rest).
        var p = target.blank()
        if (!TutoChecks.done(steps.getValue("kick_do"), p, target)) p = TutoChecks.show(steps.getValue("kick_do"), p, target)!!
        val flow = MutableStateFlow(p)
        val c = TutoController(
            scope, "p1", flow, edit = { f -> flow.value = flow.value.f() }, store = MemoryStore(),
            render = { FloatArray(OfflineExport.SAMPLE_RATE * 2) { 0.1f } }, player = Player(), debounceMs = 10,
        )
        idle()
        c.start(style, TutoLevel.Facile)
        idle()
        // The first step of the course whose button, on that Groove, is « Poser ce qui manque ».
        val chapters = c.ui.value!!.chapters
        val at = chapters.indices.flatMap { ch -> chapters[ch].steps.indices.map { ch to it } }.firstOrNull { (ch, i) ->
            val s = chapters[ch].steps[i]
            TutoPlay.button(s, flow.value, target, TutoPlay.arrive(s, flow.value), TutoLevel.Facile) is TutoNeed.Course
        }
        assertTrue("no step offers « Poser ce qui manque »: the test checks nothing", at != null)
        c.goTo(at!!.first, at.second); idle()
        // The panel reads the project off the main thread: wait for its button.
        for (i in 0 until 100) { if (c.ui.value!!.need is TutoNeed.Course) break; Thread.sleep(20); idle(50) }
        assertTrue("the button is ${c.ui.value!!.need}: the test checks nothing", c.ui.value!!.need is TutoNeed.Course)
        val before = flow.value
        c.fulfilNeed(); idle(100)
        val after = flow.value
        assertNotEquals("the button changed nothing: the test checks nothing", before, after)
        val said = c.ui.value!!.made.orEmpty()
        assertTrue("« Poser ce qui manque » says nothing of what it laid: « $said »", said.startsWith("Posé : "))
        // The pads it gave hits to, in any part, are named; one it left as it was is not.
        val pads = after.drums.lanes.indices
        fun hits(q: com.pocketstudio.model.Project, pad: Int) = q.allScenes.map { it.drums.lanes[pad].hits.keys }
        val changed = pads.filter { hits(before, it) != hits(after, it).take(before.allScenes.size) }
        assertTrue("no pad changed: the test checks nothing", changed.isNotEmpty())
        for (pad in changed) assertTrue("« $said » does not name « ${padName(after, pad)} »", padName(after, pad) in said)
        val kept = pads.filter { it !in changed && hits(after, it).any { h -> h.isNotEmpty() } }
        assertTrue("no pad kept as it was: the test checks nothing", kept.isNotEmpty())
        for (pad in kept) assertFalse("« $said » names « ${padName(after, pad)} », left as it was", Regex("""(^|[ ,:])${Regex.escape(padName(after, pad))}([,.]|$)""").containsMatchIn(said))
    }
}
