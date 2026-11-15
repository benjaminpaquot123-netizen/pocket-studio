package com.pocketstudio.app.tuto

import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.guide.GuideListenPlayer
import com.pocketstudio.engine.OfflineExport
import com.pocketstudio.model.Project
import com.pocketstudio.model.tuto.TutoLevel
import com.pocketstudio.model.tuto.TutoProgress
import com.pocketstudio.model.tuto.TutoTarget
import com.pocketstudio.ui.PocketStudioTheme
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

/**
 * Tour 7, lot 1 D7 (seats 1d #1-2 and #17, 1c #20, 1b #30): « Weiter » on an example never heard ticked it without a
 * word, and the chapter's end showed « 次の章 » and « 次へ » side by side (two ways on, one place). Every example of
 * every level, left by « Suivant »: a line says the example was skipped, and the panel then shows one way on only.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w392dp-h852dp")
class Tour7TutoListenSkipSaidTest {
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

    @After
    fun end() = scope.cancel()

    private fun idle(ms: Long = 50) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))

    @Test
    fun suivantOnAnExampleSaysItAndShowsOneWayOn() {
        val bad = ArrayList<String>()
        var shown by mutableStateOf<TutoUi?>(null)
        compose.setContent { shown?.let { PocketStudioTheme { TutoPanel(it, canShow = true, actions = TutoActions(), reducedMotion = true) } } }
        var ends = 0
        for (level in TutoLevel.entries) {
            val flow = MutableStateFlow<Project>(TutoTarget.of(style)!!.blank())
            val c = TutoController(
                scope, "p1", flow, edit = { f -> flow.value = flow.value.f() }, store = MemoryStore(),
                render = { FloatArray(OfflineExport.SAMPLE_RATE * 2) { 0.1f } }, player = Player(), debounceMs = 10,
            )
            idle()
            c.start(style, level)
            idle()
            for (s in c.ui.value!!.chapters.flatMap { it.steps }.filter { it.listening }) {
                val (ch, i) = c.ui.value!!.placeOf(s)
                c.goTo(ch, i); idle()
                c.next(); idle()
                val ui = c.ui.value!!
                if (ui.made.isNullOrBlank()) bad += "${level.name} ${s.id}: « Suivant » on the example said nothing"
                if (ui.bravo && ui.current.id == s.id) {
                    ends++
                    shown = ui
                    compose.waitForIdle()
                    val big = compose.onAllNodes(hasTestTag("tuto_next_big"), useUnmergedTree = true).fetchSemanticsNodes().size
                    val small = compose.onAllNodes(hasTestTag("tuto_next"), useUnmergedTree = true).fetchSemanticsNodes().size
                    if (big > 0 && small > 0) bad += "${level.name} ${s.id}: « Chapitre suivant » and « Suivant » side by side"
                }
            }
        }
        assertTrue("no example ended its chapter", ends > 0)
        assertTrue("${bad.size} examples:\n" + bad.take(30).joinToString("\n"), bad.isEmpty())
    }
}
