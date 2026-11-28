package com.pocketstudio.app.tuto

import android.os.Looper
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Duration

/**
 * Tour 9, F010 (seat t9-24, gestures 82-83 and 89-90): the panel folded, « Étape suivante → » touched on its folded line;
 * the next step opened the panel by itself and the touch meant for ⌃ landed on « Montre-moi »: a demonstration began
 * without being asked. A demonstration only begins when « Montre-moi » is touched: the place of ⌃ is still ⌃.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Tour9L13TutoNoDemoBySurpriseTest {
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
    private val target = TutoTarget.of("jump_up")!!

    @After
    fun end() = scope.cancel()

    private fun idle(ms: Long = 50) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))

    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun theTouchMeantForTheFoldNeverStartsADemo() {
        val flow = MutableStateFlow<Project>(target.blank())
        val c = TutoController(
            scope, "p1", flow, edit = { f -> flow.value = flow.value.f() }, store = MemoryStore(),
            render = { FloatArray(OfflineExport.SAMPLE_RATE * 2) { 0.1f } }, player = Player(), debounceMs = 10,
        )
        c.start("jump_up", TutoLevel.Facile)
        idle(200)
        // The step before the first one with « Montre-moi » (the kick), folded by the user.
        val flat = c.ui.value!!.chapters.flatMap { it.steps }
        val kick = flat.indexOfFirst { it.id == "kick_do" }
        val before = flat[kick - 1]
        val ch = c.ui.value!!.chapters.indexOfFirst { before in it.steps }
        c.goTo(ch, c.ui.value!!.chapters[ch].steps.indexOf(before))
        idle(200)
        c.toggleOpen()
        idle(200)
        compose.setContent {
            val ui by c.ui.collectAsState()
            PocketStudioTheme {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                    ui?.let { TutoPanel(it, canShow = c.canShow(), actions = TutoActions(showMe = { c.showMe() }, toggle = c::toggleOpen), reducedMotion = true) }
                }
            }
        }
        compose.waitForIdle()
        fun node(tag: String) = compose.onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().firstOrNull()
        val fold = node("tuto_fold")?.boundsInRoot
        assertNotNull("the folded panel's ⌃", fold)
        // « Étape suivante → » on the folded line (an example is counted first, a step to do is said once).
        repeat(3) { if (c.ui.value!!.current.id != "kick_do") { c.next(); idle(200) } }
        assertTrue("on the kick: ${c.ui.value!!.current.id}", c.ui.value!!.current.id == "kick_do")
        assertTrue("the kick has its « Montre-moi »", c.canShow())
        compose.waitForIdle()
        // The touch meant for ⌃, where ⌃ was.
        val at = fold!!.center
        compose.onRoot().performTouchInput { click(Offset(at.x, at.y)) }
        compose.waitForIdle()
        idle(200)
        assertNull("a demonstration began without « Montre-moi » being aimed at", c.ui.value!!.demo)
    }
}
