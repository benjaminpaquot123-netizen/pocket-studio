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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import com.pocketstudio.ui.PocketStudioTheme
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
 * Tour 7, control of « fable-repar » (seat d, V1, chapter 12 « transitions »): the « Montée » renamed « Pont », the
 * step showed its button « Créer la partie « Montée » » and not a word of why. The panel says the part is missing.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp")
class Tour7TutoMissingPartSaidTest {
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

    private fun why(): String {
        compose.waitForIdle()
        return compose.onAllNodes(hasTestTag("tuto_need_why"), useUnmergedTree = true).fetchSemanticsNodes()
            .flatMap { it.config.getOrNull(SemanticsProperties.Text).orEmpty() }.joinToString(" ") { it.text }
    }

    /**
     * Everywhere a step offers « Créer la partie » for a part it works in (four styles, both levels, each part of the
     * song renamed in turn): the sentence names the part; the step that makes a part says no such thing.
     */
    @Test
    fun everyStepThatLacksItsPartSaysSo() {
        var shown by mutableStateOf<TutoUi?>(null)
        compose.setContent { PocketStudioTheme { shown?.let { key(it) { TutoPanel(it, canShow = true, actions = TutoActions(), reducedMotion = true) } } } }
        val bad = ArrayList<String>()
        var lacking = 0
        var making = 0
        for (style in TutoCourse.STYLES.filter { TutoTarget.of(it) != null }.take(4)) for (level in TutoLevel.entries) {
            val target = TutoTarget.of(style)!!
            val chapters = TutoCourse.of(style)!!.chaptersFor(level)
            val p = played(style, level, chapters.last().id)
            val labels = p.song.map { p.allScenes[it.scene].label }.distinct()
            for ((c, chapter) in chapters.withIndex()) for ((i, s) in chapter.steps.withIndex()) {
                if (s.manual || s.listening) continue
                val own = s.check.startsWith("part:")
                // The step that makes a part, on the song before it; any other, each part of the song renamed in turn.
                val cases = if (own) listOf(target.blank()) else labels.map { l -> p.renameScene(p.allScenes.indexOfFirst { it.label == l }, "Mon passage") }
                val need = cases.firstNotNullOfOrNull { q -> TutoPlay.button(s, q, target, TutoPlay.arrive(s, q), level) as? TutoNeed.Part } ?: continue
                shown = TutoUi(TutoProgress(style, level, current = s.id), chapters, c, i, open = true, need = need)
                val why = why()
                val name = com.pocketstudio.model.Scene.shownLabel(need.label)
                if (own) { making++; if (why.isNotEmpty()) bad += "$style ${level.name} ${s.id}: the step that makes « $name » says it is missing" }
                else { lacking++; if ("« $name »" !in why) bad += "$style ${level.name} ${s.id}: nothing says « $name » is missing: « $why »" }
            }
        }
        assertTrue("only $lacking steps lacking their part and $making making one", lacking >= 12 && making >= 12)
        assertTrue("${bad.size}:\n" + bad.joinToString("\n"), bad.isEmpty())
    }

    @Test
    fun aMissingPartIsSaidOverItsButton() {
        val style = "techno"
        val flow = MutableStateFlow(played(style, TutoLevel.Pro, "transitions"))
        val c = controller(flow)
        idle()
        c.start(style, TutoLevel.Pro)
        settle(c) { u -> listOf("fill", "gap", "crash").all { it in u.progress.done } }
        val ch = c.ui.value!!.chapters.indexOfFirst { it.id == "transitions" }
        val montee = flow.value.allScenes.indexOfFirst { it.label == "Montée" }
        flow.value = flow.value.renameScene(montee, "Pont")
        idle(100)
        var said = 0
        for (i in c.ui.value!!.chapters[ch].steps.indices) {
            val id = c.ui.value!!.chapters[ch].steps[i].id
            c.goTo(ch, i)
            settle(c) { it.current.id == id && (it.need != null || id == "crash") }
            val ui = c.ui.value!!
            if (ui.need != TutoNeed.Part("Montée")) continue
            said++
            compose.setContent { PocketStudioTheme { TutoPanel(ui.copy(open = true), canShow = true, actions = TutoActions(), reducedMotion = true) } }
            val why = why()
            assertTrue("step $id: only the button « Créer la partie « Montée » », no sentence says the part is missing: « $why »", "« Montée »" in why)
            break
        }
        assertEquals("no step offered « Créer la partie « Montée » »: the test checks nothing", 1, said)
    }
}
