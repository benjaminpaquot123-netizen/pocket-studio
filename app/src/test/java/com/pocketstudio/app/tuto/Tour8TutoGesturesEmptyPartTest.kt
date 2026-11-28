package com.pocketstudio.app.tuto

import android.os.Looper
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.guide.GuideListenPlayer
import com.pocketstudio.engine.OfflineExport
import com.pocketstudio.model.Project
import com.pocketstudio.model.Scene
import com.pocketstudio.model.allScenes
import com.pocketstudio.model.tuto.TutoCourse
import com.pocketstudio.model.tuto.TutoLevel
import com.pocketstudio.model.tuto.TutoNeed
import com.pocketstudio.model.tuto.TutoPlay
import com.pocketstudio.model.tuto.TutoProgress
import com.pocketstudio.model.tuto.TutoTarget
import com.pocketstudio.ui.PocketStudioTheme
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

/**
 * Tour 8, lot T0b (seat d, V2, chapter 12 « transitions »): the « Montée » was there but emptied, and the panel said
 * « Il n'y a pas de partie « Montée » » over its button. A part that exists, empty, is not said missing; the button
 * that fills it stays.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp")
class Tour8TutoGesturesEmptyPartTest {
    @get:Rule
    val compose = createComposeRule()

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

    private fun settle(c: TutoController, until: (TutoUi) -> Boolean) {
        for (i in 0 until 200) { idle(50); if (until(c.ui.value!!)) return; Thread.sleep(20) }
    }

    private fun why(): String {
        compose.waitForIdle()
        return compose.onAllNodes(hasTestTag("tuto_need_why"), useUnmergedTree = true).fetchSemanticsNodes()
            .flatMap { it.config.getOrNull(SemanticsProperties.Text).orEmpty() }.joinToString(" ") { it.text }
    }

    @Test
    fun anEmptyPartIsNotSaidMissing() {
        val style = "techno"
        val flow = MutableStateFlow(played(style, TutoLevel.Pro, "transitions"))
        val c = TutoController(
            scope, "p1", flow, edit = { f -> flow.value = flow.value.f() }, store = MemoryStore(),
            render = { FloatArray(OfflineExport.SAMPLE_RATE * 2) { 0.1f } }, player = Player(), debounceMs = 10,
        )
        idle()
        c.start(style, TutoLevel.Pro)
        settle(c) { u -> listOf("fill", "gap", "crash").all { it in u.progress.done } }
        val ch = c.ui.value!!.chapters.indexOfFirst { it.id == "transitions" }
        val all = flow.value.allScenes
        val montee = all.indexOfFirst { it.label == "Montée" }
        assertTrue("the Montée is the part shown: emptying it here would not hold", montee >= 0 && montee != flow.value.scene)
        // The Montée stays, its name kept, all its content gone.
        flow.value = flow.value.copy(scenes = all.mapIndexed { i, s -> if (i == montee) Scene(name = s.name, label = s.label) else s })
        idle(100)
        var seen = 0
        for (i in c.ui.value!!.chapters[ch].steps.indices) {
            val id = c.ui.value!!.chapters[ch].steps[i].id
            c.goTo(ch, i)
            settle(c) { it.current.id == id && (it.need != null || id == "crash") }
            val ui = c.ui.value!!
            if (ui.need != TutoNeed.Part("Montée")) continue
            seen++
            compose.setContent { PocketStudioTheme { TutoPanel(ui.copy(open = true), canShow = true, actions = TutoActions(), reducedMotion = true) } }
            val why = why()
            assertTrue("step $id: the Montée is there, empty, and the panel says it is missing: « $why »", "« Montée »" !in why)
            break
        }
        assertEquals("no step offered its button for the empty Montée: the test checks nothing", 1, seen)
    }
}
