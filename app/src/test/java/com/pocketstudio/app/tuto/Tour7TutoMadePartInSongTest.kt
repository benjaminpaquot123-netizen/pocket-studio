package com.pocketstudio.app.tuto

import android.os.Looper
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.guide.GuideListenPlayer
import com.pocketstudio.engine.OfflineExport
import com.pocketstudio.model.Project
import com.pocketstudio.model.allScenes
import com.pocketstudio.model.renameScene
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.time.Duration

/**
 * Tour 7, control of « fable-repar » (seat d, chapter 12 « transitions »): the song laid out, its « Montée » renamed
 * « Pont »; « Créer la partie « Montée » » made the part and left it out of the song: the chapter ended on « Bravo »
 * for a part that never plays. The part the button makes while a song is laid out goes in it, at its place.
 */
@RunWith(RobolectricTestRunner::class)
class Tour7TutoMadePartInSongTest {
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

    /** The course of [style] at [level] played up to the end of its chapter [last], each step as the app plays it. */
    private fun played(style: String, level: TutoLevel, last: String): Project {
        val target = TutoTarget.of(style)!!
        val chapters = TutoCourse.of(style)!!.chaptersFor(level)
        var p = target.blank()
        for (s in chapters.take(chapters.indexOfFirst { it.id == last } + 1).flatMap { it.steps }) {
            if (s.manual || s.listening) continue
            p = TutoPlay.play(s, p, target, level).project
        }
        return p
    }

    private fun controller(flow: MutableStateFlow<Project>) = TutoController(
        scope, "p1", flow, edit = { f -> flow.value = flow.value.f() }, store = MemoryStore(),
        render = { FloatArray(OfflineExport.SAMPLE_RATE * 2) { 0.1f } }, player = Player(), debounceMs = 10,
    )

    /** The looks at the project are made off the main thread: wait until [until] holds. */
    private fun settle(c: TutoController, until: (TutoUi) -> Boolean) {
        for (i in 0 until 200) { idle(50); if (until(c.ui.value!!)) return; Thread.sleep(20) }
    }

    @Test
    fun thePartTheButtonMakesIsPlacedInTheSong() {
        val style = "techno"
        val flow = MutableStateFlow(played(style, TutoLevel.Pro, "transitions"))
        val c = controller(flow)
        idle()
        c.start(style, TutoLevel.Pro)
        settle(c) { u -> listOf("fill", "gap", "crash").all { it in u.progress.done } }
        val ch = c.ui.value!!.chapters.indexOfFirst { it.id == "transitions" }
        val montee = flow.value.allScenes.indexOfFirst { it.label == "Montée" }
        assertTrue("the song does not play the Montée", flow.value.song.any { it.scene == montee })

        // The tester's gestures: « Montée » renamed « Pont », the chapter opened again, its button touched.
        flow.value = flow.value.renameScene(montee, "Pont")
        idle(100)
        c.goTo(ch, 0)
        settle(c) { it.current.id == "fill" && it.need != null }
        assertEquals(TutoNeed.Part("Montée"), c.ui.value!!.need)
        c.fulfilNeed()
        idle(100)
        val p = flow.value
        val made = p.allScenes.indexOfFirst { it.label == "Montée" }
        assertTrue("no « Montée » made", made >= 0 && made != montee)
        val at = p.song.indexOfFirst { it.scene == made }
        assertTrue("« Montée » is made and the song never plays it: ${p.song.map { p.allScenes[it.scene].label }}", at >= 0)
        // At its place in the course's order: before the « Drop 2 », after the « Break ».
        val labels = p.song.map { p.allScenes[it.scene].label }
        assertTrue("« Montée » is not before « Drop 2 »: $labels", at < labels.indexOf("Drop 2") && at > labels.indexOf("Break"))
        assertEquals(4, p.song[at].bars)
    }
}
