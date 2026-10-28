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
import kotlinx.coroutines.launch
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

    /** Says what went out as [out] says (all of it by default). */
    private class FakePlayer(var out: (Pcm) -> Long = { it.frames.toLong() }) : GuideListenPlayer {
        val played = ArrayList<Pcm>()
        override fun play(pcm: Pcm, ended: () -> Unit): Boolean { played += pcm; return true }
        override fun stop() {}
        override fun heard(): Long = played.lastOrNull()?.let(out) ?: 0L
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
    private fun controller(
        flow: MutableStateFlow<Project>, history: History<Project>, store: TutoStore, rendered: MutableList<TutoExample> = ArrayList(), player: FakePlayer = FakePlayer(),
        level: Float = 0.1f, shown: MutableList<Project?> = ArrayList(),
    ): TutoController {
        // « Montre-moi »'s frames shown as the app shows them: on screen, never in the history.
        var before: Project? = null
        return TutoController(
            scope, "p1", flow, edit = { f -> history.push(history.current.f()); flow.value = history.current }, store = store,
            render = { ex -> rendered += ex; FloatArray(OfflineExport.SAMPLE_RATE * 2) { level } }, player = player, debounceMs = 10,
            demoShow = { frame ->
                shown += frame
                if (frame != null) { if (before == null) before = flow.value; flow.value = frame } else { before?.let { flow.value = it }; before = null }
            },
            frameMs = 20, holdMs = 100, fadeMs = 100,
        )
    }

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
        // Owner (2026-10-28): never on by itself; the step stays with « Bravo » until « Étape suivante ».
        idle(500)
        assertEquals("tempo", c.ui.value!!.current.id)
        assertTrue(c.ui.value.toString(), c.ui.value!!.bravo)
        c.toggleOpen()
        c.toggleOpen()
        assertTrue("« Bravo » kept while the step stays", c.ui.value!!.bravo)
        c.next()
        assertEquals("key", c.ui.value!!.current.id)
        assertFalse(c.ui.value!!.bravo)
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

    /**
     * Owner (2026-10-28): « Écouter l'exemple » ticks a listening step only once the example really
     * went out (not when it starts, never when it is silent); « Montre-moi » shows the kick cell by
     * cell, keeps it a moment with its caption, then takes it all back: the project as it was,
     * nothing in « Annuler », the step still to do, its dotted cells still there.
     */
    @Test
    fun listeningTicksOnceHeardAndShowMeShowsThenTakesItBack() {
        val empty = com.pocketstudio.model.tuto.TutoTarget.of("jump_up")!!.blank()
        val flow = MutableStateFlow(empty)
        val history = History(empty)
        val rendered = ArrayList<TutoExample>()
        var out = 0L
        val player = FakePlayer { out }
        val shown = ArrayList<Project?>()
        val c = controller(flow, history, MemoryStore(), rendered, player, shown = shown)
        c.start("jump_up", TutoLevel.Pro)
        c.listen()
        await("example played") { player.played.isNotEmpty() }
        assertTrue(c.ui.value!!.playing)
        idle(500)
        assertFalse("ticked before anything went out", "ref" in c.ui.value!!.progress.done)
        out = OfflineExport.SAMPLE_RATE.toLong()  // 1 s of the 2 went out: half of it
        await("listening ticked once heard") { "ref" in c.ui.value!!.progress.done }
        assertSame(empty, flow.value)
        assertEquals(0, history.undoDepth)
        // The reference: the Montée then the Drop of the finished song.
        assertEquals(174.0, rendered.single().after.tempoBpm, 0.0)
        assertNotNull(rendered.single().before)
        // « Montre-moi » on the kick.
        // Lot « tuto 5 »: « Déjà prêt pour toi » first, then the kick's sound, then its cells.
        c.goTo(1, 2)
        assertEquals("kick_do", c.ui.value!!.current.id)
        assertTrue(c.canShow())
        assertTrue(c.showMe())
        await("demo ends") { c.ui.value!!.demo == null }
        val frames = shown.filterNotNull()
        assertTrue("shown cell by cell: ${frames.size}", frames.size >= 3)
        assertEquals(frames.map { it.drums.lanes[0].hits.size }.sorted(), frames.map { it.drums.lanes[0].hits.size })
        assertEquals(setOf(0, 10), frames.last().drums.lanes[0].hits.keys.filter { it < 16 }.toSet())
        assertNull("taken back", shown.last())
        assertEquals(empty, flow.value)
        assertEquals(0, history.undoDepth)
        idle(200)
        assertFalse("Montre-moi does not tick", "kick_do" in c.ui.value!!.progress.done)
        assertEquals(0 to setOf(0, 10), c.hint(16))
        // A silent example is not played: it says so.
        val quiet = FakePlayer()
        val q = controller(MutableStateFlow(empty), History(empty), MemoryStore(), player = quiet, level = 0f)
        q.start("jump_up", TutoLevel.Facile)
        q.listen()
        await("silence said") { q.ui.value!!.failed }
        assertTrue(quiet.played.isEmpty())
    }

    /** The snare needs 2 bars: the main button says so and makes only that; a new step opens the panel again. */
    @Test
    fun theMainButtonMeetsTheStepsNeedAndANewStepOpensThePanel() {
        val empty = com.pocketstudio.model.tuto.TutoTarget.of("jump_up")!!.blank()
        val flow = MutableStateFlow(empty)
        val history = History(empty)
        val c = controller(flow, history, MemoryStore())
        c.start("jump_up", TutoLevel.Facile)
        c.goTo(2, 0)
        assertEquals("snare_do", c.ui.value!!.current.id)
        assertEquals(com.pocketstudio.model.tuto.TutoNeed.Bars(2), c.ui.value!!.need)
        c.toggleOpen()
        assertFalse(c.ui.value!!.open)
        c.fulfilNeed()
        assertEquals(32, flow.value.drums.steps)
        assertEquals(1, history.undoDepth)
        assertTrue(flow.value.drums.lanes.all { it.hits.isEmpty() })
        await("need met") { c.ui.value!!.need == null }
        assertFalse("folded within the step", c.ui.value!!.open)
        c.next()
        assertTrue("a new step opens it", c.ui.value!!.open)
    }

    /**
     * Testers (lot « tuto 3 »): « Je le fais » and « Montre-moi » fold the panel out of the way, and
     * « Je le fais » goes to the very control: the song's « Ajouter : », the reverb's send of the
     * Accords' track (not the Mélodie's volume), the bass's sound settings (not its ready-made sounds).
     */
    @Test
    fun jeLeFaisFoldsThePanelAndGoesToTheControlTheStepAsksFor() {
        val target = com.pocketstudio.model.tuto.TutoTarget.of("jump_up")!!
        val blank = target.blank()
        val (withPad, pad) = com.pocketstudio.model.tuto.TutoChecks.partTrack(blank, "pad", target)!!
        val (song, bass) = com.pocketstudio.model.tuto.TutoChecks.partTrack(withPad, "bass", target)!!
        val flow = MutableStateFlow(song)
        val c = controller(flow, History(song), MemoryStore())
        val goes = ArrayList<TutoGo>()
        val job = scope.launch { c.goes.collect { goes += it } }
        idle(100)
        c.start("jump_up", TutoLevel.Pro)
        fun at(id: String) {
            val chapters = c.ui.value!!.chapters
            val ch = chapters.indexOfFirst { x -> x.steps.any { it.id == id } }
            c.goTo(ch, chapters[ch].steps.indexOfFirst { it.id == id })
            assertEquals(id, c.ui.value!!.current.id)
        }
        fun doIt(): TutoGo {
            val before = goes.size
            assertTrue(c.ui.value!!.open)
            c.doIt()
            await("gone") { goes.size > before }
            assertFalse("« Je le fais » folds the panel", c.ui.value!!.open)
            return goes.last()
        }
        at("reverb")
        val reverb = doIt()
        assertEquals(com.pocketstudio.model.InstrumentTrack.mixerChannel(pad), reverb.effects)
        assertEquals(0, reverb.send)
        at("bass_knobs")
        assertEquals(bass, doIt().settings)
        at("song_order")
        assertEquals(TutoController.SONG_ADD_SPOT, doIt().open?.spot)
        at("levels")
        assertEquals(com.pocketstudio.model.InstrumentTrack.mixerChannel(pad), doIt().strip)
        // « Montre-moi »: folded while it shows, and after (« à toi maintenant »).
        at("hat_do")
        assertTrue(c.ui.value!!.open)
        assertTrue(c.showMe())
        assertFalse(c.ui.value!!.open)
        await("demo ends") { c.ui.value!!.demo == null }
        assertFalse(c.ui.value!!.open)
        job.cancel()
    }

    /**
     * Testers (lot « tuto 3 »): « Écoute toute ta batterie » plays the user's own drums (and ticks
     * once they were heard); the examples of what comes next stay examples only.
     */
    @Test
    fun aListeningStepAboutTheUsersMusicIsTickedByHearingIt() {
        val course = com.pocketstudio.model.tuto.TutoCourse.of("jump_up")!!.chaptersFor(TutoLevel.Facile).flatMap { it.steps }
        fun step(id: String) = course.first { it.id == id }
        val empty = com.pocketstudio.model.tuto.TutoTarget.of("jump_up")!!.blank()
        fun hits(p: Project, vararg lanes: Int) = p.copy(drums = p.drums.copy(lanes = p.drums.lanes.mapIndexed { i, l -> if (i in lanes) l.copy(hits = mapOf(0 to com.pocketstudio.model.Hit(0.9f))) else l }))
        val drums = hits(empty, 0, 2, 4, 5)
        assertFalse(TutoController.mineFor(step("drums_hear"), empty))
        assertTrue("kick, snare, clap, hats: « ta batterie »", TutoController.mineFor(step("drums_hear"), drums))
        assertFalse("the kick's example comes before the kick", TutoController.mineFor(step("kick_hear"), empty))
        assertFalse("no bass yet: the bass's example", TutoController.mineFor(step("bass_hear"), drums))
        assertFalse("not a listening step", TutoController.mineFor(step("hat_do"), drums))
        val flow = MutableStateFlow(drums)
        val c = controller(flow, History(drums), MemoryStore())
        c.start("jump_up", TutoLevel.Facile)
        val chapters = c.ui.value!!.chapters
        val ch = chapters.indexOfFirst { x -> x.steps.any { it.id == "drums_hear" } }
        c.goTo(ch, chapters[ch].steps.indexOfFirst { it.id == "drums_hear" })
        assertTrue(c.mineToHear())
        c.heardMine()
        // Lot « tuto 4 »: « ta batterie, puis celle de l'exemple »: the example too.
        idle(300)
        assertFalse("the user's drums alone", "drums_hear" in c.ui.value!!.progress.done)
        c.listen()
        await("ticked") { "drums_hear" in c.ui.value!!.progress.done }
        assertEquals("never on by itself", "drums_hear", c.ui.value!!.current.id)
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
}
