package com.pocketstudio.app.tuto

import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.guide.GuideListenPlayer
import com.pocketstudio.engine.OfflineExport
import com.pocketstudio.model.Project
import com.pocketstudio.model.tuto.TutoChecks
import com.pocketstudio.model.tuto.TutoCourse
import com.pocketstudio.model.tuto.TutoLevel
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
 * Tour 6, N20: a step ticked before the user touched anything (« Nächstes Kapitel » on the crash never touched, the
 * project held it already) read as a mistake. The panel says it: « Déjà fait ».
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w392dp-h852dp")
class Tour6TutoPanelAlreadyTest {
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
    private val style = "techno"
    private val target = TutoTarget.of(style)!!

    @After
    fun end() = scope.cancel()

    private fun idle(ms: Long = 50) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))

    @Test
    fun aStepTheProjectHeldAlreadyIsSaidAlreadyDone() {
        val tempo = TutoCourse.of(style)!!.chapters.flatMap { it.steps }.first { it.id == "tempo" }
        // The song's tempo already the style's: nothing to touch on « tempo ».
        val flow = MutableStateFlow<Project>(TutoChecks.show(tempo, target.blank(), target)!!)
        val c = TutoController(
            scope, "p1", flow, edit = { f -> flow.value = flow.value.f() }, store = MemoryStore(),
            render = { FloatArray(OfflineExport.SAMPLE_RATE * 2) { 0.1f } }, player = Player(), debounceMs = 10,
        )
        idle()
        c.start(style, TutoLevel.Facile)
        idle()
        c.goTo(0, c.ui.value!!.chapters[0].steps.indexOfFirst { it.id == "tempo" })
        val end = System.currentTimeMillis() + 20_000
        while ("tempo" !in c.ui.value!!.progress.done && System.currentTimeMillis() < end) { idle(); Thread.sleep(5) }
        idle()
        val ui = c.ui.value!!
        assertEquals("tempo", ui.current.id)
        assertTrue("tempo ticked by the project", "tempo" in ui.progress.done)
        assertTrue("the step reached done without a gesture is not said « Déjà fait »", ui.already)
    }

    /**
     * CI 43425565244 (8878 tests, one red: `aStepTheProjectHeldAlreadyIsSaidAlreadyDone`, line 84): « Déjà fait » was
     * lost when a look at the project begun on the step before ended after the step was reached. It ticked the step
     * (the project held it) but compared the step it had begun on, so nothing said « Déjà fait », and the look that
     * the step's arrival asked for came after, the step already ticked, and said nothing either. Played here in that
     * order: the first look's work done off the main thread before the step is reached, its answer read after.
     */
    @Test
    fun aLookBegunBeforeTheStepStillSaysAlreadyDone() {
        val tempo = TutoCourse.of(style)!!.chapters.flatMap { it.steps }.first { it.id == "tempo" }
        val flow = MutableStateFlow<Project>(TutoChecks.show(tempo, target.blank(), target)!!)
        val c = TutoController(
            scope, "p1", flow, edit = { f -> flow.value = flow.value.f() }, store = MemoryStore(),
            render = { FloatArray(8) }, player = Player(), debounceMs = 10,
        )
        idle()
        c.start(style, TutoLevel.Facile)
        idle()
        // The look begun on the first step finishes its work; its answer waits on the main thread.
        Thread.sleep(500)
        c.goTo(0, c.ui.value!!.chapters[0].steps.indexOfFirst { it.id == "tempo" })
        val end = System.currentTimeMillis() + 20_000
        while ("tempo" !in c.ui.value!!.progress.done && System.currentTimeMillis() < end) { idle(); Thread.sleep(5) }
        repeat(10) { idle(); Thread.sleep(5) }
        val ui = c.ui.value!!
        assertEquals("tempo", ui.current.id)
        assertTrue("tempo ticked by the project", "tempo" in ui.progress.done)
        assertTrue("a look begun before the step was reached lost « Déjà fait »", ui.already)
    }

    @Test
    fun thePanelSaysAlreadyDone() {
        val chapters = TutoCourse.of(style)!!.chaptersFor(TutoLevel.Facile)
        val at = chapters[0].steps.indexOfFirst { it.id == "tempo" }
        var bravoShown by androidx.compose.runtime.mutableStateOf(false)
        compose.setContent {
            val ui = TutoUi(TutoProgress(style, TutoLevel.Facile, done = setOf("tempo"), current = "tempo"), chapters, 0, at, bravo = bravoShown, already = true)
            PocketStudioTheme { TutoPanel(ui, canShow = false, actions = TutoActions(), reducedMotion = true) }
        }
        for (bravo in listOf(false, true)) {
            bravoShown = bravo
            compose.waitForIdle()
            assertTrue("bravo=$bravo: nothing says the step was done already", compose.onAllNodesWithTag("tuto_already", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty())
        }
    }
}
