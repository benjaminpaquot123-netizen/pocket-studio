package com.pocketstudio.app.tuto

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.guide.GuideListenPlayer
import com.pocketstudio.app.nav.AppHarness
import com.pocketstudio.app.projects.ProjectStarter
import com.pocketstudio.app.projects.ProjectsActions
import com.pocketstudio.app.projects.ProjectsScreen
import com.pocketstudio.app.projects.ProjectsUi
import com.pocketstudio.app.projects.StarterGroup
import com.pocketstudio.engine.OfflineExport
import com.pocketstudio.model.History
import com.pocketstudio.model.Project
import com.pocketstudio.model.tuto.TutoExample
import com.pocketstudio.model.tuto.TutoLevel
import com.pocketstudio.model.tuto.TutoProgress
import com.pocketstudio.ui.PocketStudioTheme
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

/** « Tuto de A à Z »: its controller (detectors, Montre-moi, examples, resume), « Nouveau projet »'s three choices, a journey in the app. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp")
class TutoTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private class MemoryStore : TutoStore {
        val saved = HashMap<String, TutoProgress>()
        override fun load(projectId: String) = saved[projectId]
        override fun save(projectId: String, progress: TutoProgress?) {
            if (progress == null) saved.remove(projectId) else saved[projectId] = progress
        }
    }

    private class FakePlayer : GuideListenPlayer {
        val played = ArrayList<Pcm>()
        override fun play(pcm: Pcm, ended: () -> Unit): Boolean { played += pcm; return true }
        override fun stop() {}
    }

    private fun idle(ms: Long = 50) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))

    private fun await(what: String, ok: () -> Boolean) {
        val end = System.currentTimeMillis() + 20_000
        while (!ok()) {
            idle(50)
            compose.waitForIdle()
            check(System.currentTimeMillis() < end) { "$what: not in time" }
            Thread.sleep(5)
        }
    }

    private val scope = MainScope()
    private val h = AppHarness(compose)

    @After
    fun end() {
        scope.cancel()
        h.finish()
    }

    /** A controller on [flow] whose edits go through [history] (one step each), as the session does. */
    private fun controller(flow: MutableStateFlow<Project>, history: History<Project>, store: TutoStore, rendered: MutableList<TutoExample> = ArrayList(), player: FakePlayer = FakePlayer()) =
        TutoController(
            scope, "p1", flow, edit = { f -> history.push(history.current.f()); flow.value = history.current }, store = store,
            render = { ex -> rendered += ex; FloatArray(OfflineExport.SAMPLE_RATE * 2) { 0.1f } }, player = player, debounceMs = 10,
        )

    @Test
    fun aStepIsTickedByItsDetectorAndTheTutorialResumesAfterARestart() {
        val empty = Project(name = "").sanitized()
        val flow = MutableStateFlow(empty)
        val history = History(empty)
        val store = MemoryStore()
        val c = controller(flow, history, store)
        c.start("jump_up", TutoLevel.Facile)
        assertEquals("ref", c.ui.value!!.current.id)
        // The user sets the tempo himself (not through the tutorial): it is seen and ticked.
        c.next()
        assertEquals("tempo", c.ui.value!!.current.id)
        flow.value = empty.copy(tempoBpm = 174.0)
        await("tempo ticked") { "tempo" in c.ui.value!!.progress.done }
        assertEquals("key", c.ui.value!!.current.id)
        assertTrue(c.ui.value.toString(), c.ui.value!!.bravo)
        // A restart: a new controller on the same project finds the same place.
        val again = controller(flow, history, store)
        assertEquals("key", again.ui.value!!.current.id)
        assertEquals(setOf("tempo"), again.ui.value!!.progress.done)
        // Quit, then « Reprendre le tuto ».
        again.quit()
        assertNull(again.ui.value)
        assertTrue(again.resumable)
        again.resume()
        assertEquals("key", again.ui.value!!.current.id)
    }

    @Test
    fun showMeIsOneUndoStepAndListeningNeverTouchesTheProject() {
        val empty = Project(name = "").sanitized()
        val flow = MutableStateFlow(empty)
        val history = History(empty)
        val rendered = ArrayList<TutoExample>()
        val player = FakePlayer()
        val c = controller(flow, history, MemoryStore(), rendered, player)
        c.start("jump_up", TutoLevel.Pro)
        // « Écouter l'exemple » of the first step (a listening step): heard, ticked, the project untouched.
        c.listen()
        await("example heard") { player.played.isNotEmpty() }
        assertSame(empty, flow.value)
        assertEquals(0, history.undoDepth)
        assertEquals(174.0, rendered.single().after.tempoBpm, 0.0)
        await("listening ticked") { "ref" in c.ui.value!!.progress.done }
        // « Montre-moi » on the kick: one undo step, which takes it back.
        c.goTo(1, 1)
        assertEquals("kick_do", c.ui.value!!.current.id)
        assertTrue(c.canShow())
        assertTrue(c.showMe())
        assertEquals(1, history.undoDepth)
        assertEquals(setOf(0, 10), flow.value.drums.lanes[0].hits.keys.filter { it < 16 }.toSet())
        await("kick ticked") { "kick_do" in c.ui.value!!.progress.done }
        assertEquals(empty, history.undo())
        // The dotted cells of a drum step: the recipe's kick.
        c.goTo(1, 1)
        assertEquals(0 to setOf(0, 10), c.hint(16))
    }

    @Test
    fun newProjectOffersThreeChoicesAndLearningAsksTheLevelAndStyle() {
        val learned = ArrayList<Pair<String, TutoLevel>>()
        var wizard = 0
        val created = ArrayList<Int>()
        val starters = listOf(
            ProjectStarter("Rythme de départ", "Un rythme", "starter", StarterGroup.Style),
            ProjectStarter("Projet vide", "Vierge", "empty", StarterGroup.Empty),
        )
        compose.setContent {
            PocketStudioTheme {
                ProjectsScreen(ProjectsUi(loading = false), "p-0", starters,
                    ProjectsActions(learn = { s, l -> learned += s to l }, wizard = { wizard++ }, create = { created += it }), newOnly = true)
            }
        }
        compose.onNodeWithTag("starter_wizard").performClick()
        assertEquals(1, wizard)
        compose.onNodeWithTag("start_empty").performClick()
        assertEquals(listOf(1), created)
        compose.onNodeWithTag("start_learn").performClick()
        compose.onNodeWithTag("tuto_chooser").assertExists()
        compose.onNodeWithTag("tuto_soon").performScrollTo().assertExists()
        // Only jump up has its course; the others are coming.
        assertEquals(1, compose.onAllNodesWithTag("tuto_style_jump_up").fetchSemanticsNodes().size)
        compose.onNodeWithTag("tuto_style_techno").assertDoesNotExist()
        compose.onNodeWithTag("tuto_level_pro").performClick()
        compose.onNodeWithTag("tuto_style_jump_up").performScrollTo().performClick()
        assertEquals(listOf("jump_up" to TutoLevel.Pro), learned)
    }

    /** In the app: a new empty project with the jump up course, its first steps done the way a user does them. */
    @Test
    fun theFirstJumpUpStepsInTheApp() {
        h.start(simple = true, build = {
            // « J'apprends » makes an empty project.
            session.edit { Project(name = "") }
            PrefsTutoStore(h.app).save(projectId, TutoProgress("jump_up", TutoLevel.Facile))
        })
        await("panel") { compose.onAllNodesWithTag("tuto_panel").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Chapitre 1/", substring = true).assertExists()
        // 1. Listen to the reference (skipped here: no engine), 2. the tempo with « Montre-moi ».
        compose.onNodeWithTag("tuto_next").performClick()
        compose.onNodeWithTag("tuto_show").performClick()
        await("tempo") { h.vm.project.value.tempoBpm == 174.0 }
        await("tempo ticked") { compose.onAllNodesWithTag("tuto_bravo").fetchSemanticsNodes().isNotEmpty() }
        // « Montre-moi » was one step: undone, the tempo goes back (the step stays done).
        h.vm.undo()
        assertEquals(120.0, h.vm.project.value.tempoBpm, 0.0)
        // 3. The key, by hand: the user sets F phrygian (the song's chords sheet does it); seen and ticked.
        h.vm.session.edit { it.copy(harmony = com.pocketstudio.model.Harmony(5, com.pocketstudio.model.Scale.Phrygian, lock = true)) }
        await("key ticked") { compose.onAllNodesWithText("Chapitre 2/", substring = true).fetchSemanticsNodes().isNotEmpty() }
        // 4. The kick: heard (skipped), then « Je le fais »: the Rythme tab, the kick pad, its cells dotted.
        compose.onNodeWithTag("tuto_next").performClick()
        compose.onNodeWithTag("tuto_do").performClick()
        await("dotted cells") { compose.onAllNodesWithTag("tuto_hint", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("step_0").performClick()
        compose.onNodeWithTag("step_10").performClick()
        await("kick ticked") { compose.onAllNodesWithText("Chapitre 3/", substring = true).fetchSemanticsNodes().isNotEmpty() }
        assertEquals(setOf(0, 10), h.vm.project.value.drums.lanes[0].hits.keys.filter { it < 16 }.toSet())
        // Kept for the next start of the app.
        assertTrue(PrefsTutoStore(h.app).load(h.vm.projectId)!!.done.containsAll(setOf("tempo", "key", "kick_do")))
    }
}
