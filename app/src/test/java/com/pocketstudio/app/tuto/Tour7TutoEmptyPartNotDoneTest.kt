package com.pocketstudio.app.tuto

import android.os.Looper
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.guide.GuideListenPlayer
import com.pocketstudio.engine.OfflineExport
import com.pocketstudio.model.Project
import com.pocketstudio.model.allScenes
import com.pocketstudio.model.renameScene
import com.pocketstudio.model.switchScene
import com.pocketstudio.model.tuto.TutoCourse
import com.pocketstudio.model.tuto.TutoLevel
import com.pocketstudio.model.tuto.TutoPlay
import com.pocketstudio.model.tuto.TutoProgress
import com.pocketstudio.model.tuto.TutoTarget
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
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
 * Tour 7, control of « fable-repar » (seat d, V2, chapter 12 « transitions »): the chapter done, its « Montée » renamed
 * « Pont », a new EMPTY part named « Montée », the chapter opened again: step 1/3 read « Déjà fait ! ». A step that
 * works in a named part is done by what that part holds now, not by a tick kept from another part of that name.
 */
@RunWith(RobolectricTestRunner::class)
class Tour7TutoEmptyPartNotDoneTest {
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
    fun anEmptyPartNamedMonteeIsNotDejaFait() {
        val style = "techno"
        val flow = MutableStateFlow(played(style, TutoLevel.Pro, "transitions"))
        val c = controller(flow)
        idle()
        c.start(style, TutoLevel.Pro)
        settle(c) { u -> listOf("fill", "gap", "crash").all { it in u.progress.done } }
        val chapters = c.ui.value!!.chapters
        val ch = chapters.indexOfFirst { it.id == "transitions" }
        assertEquals(listOf("fill", "gap", "crash"), chapters[ch].steps.map { it.id })
        assertTrue("the chapter is not done: ${c.ui.value!!.progress.done}", chapters[ch].steps.all { it.id in c.ui.value!!.progress.done })

        // The tester's gestures: « Montée » renamed « Pont », a new empty part named « Montée ».
        val montee = flow.value.allScenes.indexOfFirst { it.label == "Montée" }
        assertTrue(montee >= 0)
        val free = flow.value.allScenes.size
        flow.value = flow.value.renameScene(montee, "Pont").switchScene(free).renameScene(free, "Montée")
        assertTrue(flow.value.allScenes[free].let { it.isEmpty && it.label == "Montée" })
        idle(100)

        // The chapter opened again from the chapters' list.
        c.goTo(ch, 0)
        settle(c) { it.current.id == "fill" && it.need != null }
        val ui = c.ui.value!!
        assertEquals("fill", ui.current.id)
        assertFalse("step 1/3 reads « Déjà fait » on an empty « Montée »", ui.already && ui.current.id in ui.progress.done)
        assertFalse("step 1/3 is counted done on an empty « Montée »", ui.current.id in ui.progress.done)
        for (i in 1..2) {
            c.goTo(ch, i); idle(100)
            val u = c.ui.value!!
            if (u.current.id == "gap") assertFalse("the silence is counted done in an empty « Montée »", u.current.id in u.progress.done)
        }
    }
}
