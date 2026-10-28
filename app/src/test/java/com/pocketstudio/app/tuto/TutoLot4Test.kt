package com.pocketstudio.app.tuto

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.verticalScroll
import com.pocketstudio.model.addTrack
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.guide.GuideListenPlayer
import com.pocketstudio.engine.OfflineExport
import com.pocketstudio.model.History
import com.pocketstudio.model.InstrumentTrack
import com.pocketstudio.model.Mixer
import com.pocketstudio.model.Project
import com.pocketstudio.model.SynthPatch
import com.pocketstudio.model.allScenes
import com.pocketstudio.model.instrumentTracks
import com.pocketstudio.model.switchScene
import com.pocketstudio.model.tuto.TutoChecks
import com.pocketstudio.model.tuto.TutoCourse
import com.pocketstudio.model.tuto.TutoLevel
import com.pocketstudio.model.tuto.TutoProgress
import com.pocketstudio.model.tuto.TutoTarget
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
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

/**
 * Lot « tuto 4 » (2nd pass of the human testers), the screen's side: « Je le fais » on the right
 * strip, the panel's words never under its buttons, an end that says what is left, the part made
 * said in words, the effects box and the picker that do not jump under the finger.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w392dp-h852dp")
class TutoLot4Test {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private class MemoryStore : TutoStore {
        val saved = HashMap<String, TutoProgress>()
        override fun load(projectId: String) = saved[projectId]
        override fun save(projectId: String, progress: TutoProgress?) {
            if (progress == null) saved.remove(projectId) else saved[projectId] = progress
        }
    }

    private object Quiet : GuideListenPlayer {
        override fun play(pcm: Pcm, ended: () -> Unit) = true
        override fun stop() {}
        override fun heard() = 0L
    }

    private val scope = MainScope()

    @After
    fun end() {
        scope.cancel()
        TutoHints.banner = null
        TutoHints.knobs = emptySet()
    }

    private fun idle(ms: Long = 50) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))

    private fun await(what: String, ok: () -> Boolean) {
        val end = System.currentTimeMillis() + 20_000
        while (!ok()) {
            idle(50)
            check(System.currentTimeMillis() < end) { "$what: not in time" }
            Thread.sleep(5)
        }
    }

    private val target = TutoTarget.of("jump_up")!!

    private fun controller(flow: MutableStateFlow<Project>, store: TutoStore = MemoryStore(), history: History<Project> = History(flow.value)): TutoController {
        var before: Project? = null
        return TutoController(
            scope, "p1", flow, edit = { f -> history.push(history.current.f()); flow.value = history.current }, store = store,
            render = { FloatArray(OfflineExport.SAMPLE_RATE) { 0.1f } }, player = Quiet, debounceMs = 10,
            demoShow = { frame -> if (frame != null) { if (before == null) before = flow.value; flow.value = frame } else { before?.let { flow.value = it }; before = null } },
            frameMs = 20, holdMs = 50, fadeMs = 50,
        )
    }

    private fun goes(c: TutoController): MutableList<TutoGo> = ArrayList<TutoGo>().also { list -> scope.launch { c.goes.collect { list += it } }; idle() }

    private fun stepIndex(c: TutoController, id: String): Pair<Int, Int> {
        val u = c.ui.value!!
        val ch = u.chapters.indexOfFirst { ch -> ch.steps.any { it.id == id } }
        return ch to u.chapters[ch].steps.indexOfFirst { it.id == id }
    }

    private fun goTo(c: TutoController, id: String) {
        val (ch, s) = stepIndex(c, id)
        c.goTo(ch, s)
        idle()
        assertEquals(id, c.ui.value!!.current.id)
    }

    private fun sends(p: Project, ch: Int, bus: Int, v: Float) =
        p.copy(mixer = p.mixer.copy(channels = p.mixer.channels.mapIndexed { i, c -> if (i == ch) c.copy(sends = c.sends.mapIndexed { b, s -> if (b == bus) v else s }) else c }))

    @Test
    fun theReverbsSecondHalfIsTheSnaresPadAndItIsSaid() {
        // The whole song, nothing sent yet: « Je le fais » opens the « Accords » box first.
        val song = (0 until Mixer.CHANNELS).fold(target.song) { p, ch -> sends(p, ch, 0, 0f) }
        val flow = MutableStateFlow(song)
        val c = controller(flow)
        c.start("jump_up", TutoLevel.Pro)
        idle()
        goTo(c, "reverb")
        val list = goes(c)
        val pad = InstrumentTrack.mixerChannel(TutoChecks.trackFor(song, "pad")!!)
        val snare = Mixer.padChannel(TutoChecks.laneOf("snare"))
        c.doIt(); idle()
        assertEquals(pad, list.last().effects)
        assertEquals(0, list.last().send)
        assertTrue(c.sendNote()!!.contains("à régler ici"))
        // The « Accords » half done: the snare's pad, and the note says the first half is done.
        flow.value = sends(flow.value, pad, 0, 0.31f)
        idle(100)
        c.doIt(); idle()
        assertEquals(snare, list.last().effects)
        val note = c.sendNote()!!
        assertTrue(note, note.contains("✓"))
        assertTrue(note, note.contains("à régler ici"))
    }

    @Test
    fun theFaderOfTheChordsIsTheChordsStripNotTheFirst() {
        val flow = MutableStateFlow(target.song)
        val c = controller(flow)
        c.start("jump_up", TutoLevel.Pro)
        idle()
        goTo(c, "levels")
        val list = goes(c)
        c.doIt(); idle()
        val pad = InstrumentTrack.mixerChannel(TutoChecks.trackFor(target.song, "pad")!!)
        assertEquals(pad, list.last().strip)
        assertTrue(list.last().strip != 0)
    }

    @Test
    fun theGlideIsRingedAmongTheAdvancedKnobs() {
        val flow = MutableStateFlow(target.song)
        val c = controller(flow)
        c.start("jump_up", TutoLevel.Pro)
        idle()
        goTo(c, "lead_knobs")
        val list = goes(c)
        c.doIt(); idle()
        assertEquals(listOf(SynthPatch.OSC2_DETUNE, SynthPatch.GLIDE), list.last().knobs)
        assertNotNull(list.last().settings)
    }

    @Test
    fun theCrashIsDottedOnTheDropsFirstCellOnly() {
        val song = target.song
        val drop = song.allScenes.indexOfFirst { it.label == "Drop" }
        val montee = song.allScenes.indexOfFirst { it.label == "Montée" }
        // In the Montée: « Aller dans la partie Drop » first, nothing dotted.
        val flow = MutableStateFlow(song.switchSceneForTest(montee).withoutCrash())
        val c = controller(flow)
        c.start("jump_up", TutoLevel.Pro)
        idle()
        goTo(c, "crash")
        assertNotNull(c.ui.value!!.need)
        assertNull(c.hint(flow.value.drums.steps))
        c.fulfilNeed(); idle(100)
        assertEquals(drop, flow.value.scene)
        await("the need gone") { c.ui.value!!.need == null }
        assertEquals(TutoChecks.CRASH_LANE to setOf(0), c.hint(flow.value.drums.steps))
    }

    @Test
    fun theHitsToTakeAwayAreDrawnOnlyInTheMontee() {
        val song = target.song
        val drop = song.allScenes.indexOfFirst { it.label == "Drop" }
        val flow = MutableStateFlow(song.switchSceneForTest(drop))
        val c = controller(flow)
        c.start("jump_up", TutoLevel.Pro)
        idle()
        goTo(c, "gap")
        // Still in the Drop (« Aller dans la partie Montée » first): no cross drawn there.
        assertNotNull(c.ui.value!!.need)
        assertNull(c.toErase(flow.value))
    }

    @Test
    fun suivantOnTheLastStepSaysWhatIsLeftAndGoesThere() {
        val flow = MutableStateFlow(Project(name = "").sanitized())
        val c = controller(flow)
        c.start("jump_up", TutoLevel.Facile)
        idle()
        val u = c.ui.value!!
        val last = u.chapters.last().steps.last()
        goTo(c, last.id)
        c.next(); idle()
        assertTrue(c.ui.value!!.atEnd)
        assertEquals(last.id, c.ui.value!!.current.id)
        assertFalse(c.ui.value!!.finished)
        // One touch on the first step left: there, the end closed.
        val first = c.ui.value!!.left.first()
        val (ch, st) = c.ui.value!!.placeOf(first)
        c.goTo(ch, st); idle()
        assertFalse(c.ui.value!!.atEnd)
        assertEquals(first.id, c.ui.value!!.current.id)
    }

    @Test
    fun inProTheExamplesNotHeardDoNotHoldTheEndBack() {
        val flow = MutableStateFlow(Project(name = "").sanitized())
        val store = MemoryStore()
        val c = controller(flow, store)
        c.start("jump_up", TutoLevel.Pro)
        idle()
        val all = c.ui.value!!.chapters.flatMap { it.steps }
        val doable = all.filter { !it.listening }.map { it.id }.toSet()
        store.saved["p1"] = store.saved["p1"]!!.copy(done = doable)
        c.resume(); idle()
        assertTrue(c.ui.value!!.finished)
        assertTrue(c.ui.value!!.left.isEmpty())
        // In Facile they are steps like the others.
        c.setLevel(TutoLevel.Facile); idle()
        val facile = c.ui.value!!
        assertEquals(facile.chapters.flatMap { it.steps }.filter { it.id !in doable }, facile.left)
    }

    @Test
    fun createThePartSaysWhatItMade() {
        // The Groove written (the target's first scene), the Intro to make.
        val song = target.song
        val groove = song.keepOnlyFirstSceneForTest()
        assertTrue(TutoChecks.bricks(groove.allScenes[0]).size > 4)
        val flow = MutableStateFlow(groove)
        val c = controller(flow)
        c.start("jump_up", TutoLevel.Facile)
        idle()
        goTo(c, "make_intro")
        await("the need") { c.ui.value!!.need != null }
        c.fulfilNeed(); idle(100)
        val made = c.ui.value!!.made
        assertNotNull(made)
        assertTrue(made!!, made.contains("Intro"))
        assertTrue(made, made.contains("sans"))
    }

    @Test
    fun showMeOnAStepWhoseTrackIsToMakeEndsOnItsButton() {
        val song = target.song
        val lead = TutoChecks.trackFor(song, "lead")!!
        val noLead = song.copy(tracks = song.tracks.filter { it.id != lead })
        val flow = MutableStateFlow(noLead)
        val c = controller(flow)
        c.start("jump_up", TutoLevel.Facile)
        idle()
        goTo(c, "lead_do")
        await("the need") { c.ui.value!!.need != null }
        assertTrue(c.showMe())
        assertFalse(c.ui.value!!.open)
        await("the demonstration over") { c.ui.value!!.demo == null }
        // All it showed taken back (owner, 1B), the panel open on « Créer la piste Mélodie ».
        assertNull(TutoChecks.trackFor(flow.value, "lead"))
        assertTrue(c.ui.value!!.open)
        assertNotNull(c.ui.value!!.need)
    }

    @Test
    fun theNotesOfMontreMoiGoSlowerThanTheCells() {
        assertEquals(TutoController.FRAME_MS, TutoController.demoFrameMs(false, 6))
        // Six notes: 0.7 s each (they went by at 0.2 s); 28: about 8 s in all, never under the cells' pace.
        assertEquals(700L, TutoController.demoFrameMs(true, 6))
        assertEquals(8000L / 24, TutoController.demoFrameMs(true, 24))
        assertTrue(TutoController.demoFrameMs(true, 200) >= TutoController.FRAME_MS)
    }

    @Test
    fun voirUnAutreReglageShowsIt() {
        val p = Project().addTrack("Basse")!!.addTrack("Accords")!!
        val strips = com.pocketstudio.app.ui.mixerStrips(p)
        val ch = strips.first().ch
        compose.setContent {
            PocketStudioTheme {
                androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())) {
                    com.pocketstudio.app.mixer.TargetPickerContent(emptySet(), p.mixer.channels, p.mixer.master, strips, {}, {}, preferred = ch)
                }
            }
        }
        compose.waitForIdle()
        val more = compose.onAllNodesWithTag("pick_group_${ch}_more").fetchSemanticsNodes()
        assertTrue("no « Voir … autre(s) réglage(s) » here", more.isNotEmpty())
        compose.onNodeWithTag("pick_group_${ch}_more").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Moins de réglages").assertIsDisplayed()
    }

    // ---- the panel ----

    private fun ui(level: TutoLevel, id: String, done: Set<String> = emptySet(), atEnd: Boolean = false): TutoUi {
        val chapters = TutoCourse.of("jump_up")!!.chaptersFor(level)
        val ch = chapters.indexOfFirst { c -> c.steps.any { it.id == id } }
        return TutoUi(TutoProgress("jump_up", level, current = id, done = done), chapters, ch, chapters[ch].steps.indexOfFirst { it.id == id }, atEnd = atEnd)
    }

    @Test
    fun aStepDoneKeepsItsWordsAboveItsButtons() {
        // A listening step done: « Étape suivante », then « Précédent » and « Écouter l'exemple » on one row.
        var u by androidx.compose.runtime.mutableStateOf(ui(TutoLevel.Facile, "ref"))
        compose.setContent { PocketStudioTheme { TutoPanel(u, canShow = false, actions = TutoActions(), reducedMotion = true) } }
        compose.waitForIdle()
        val panelBefore = compose.onNodeWithTag("tuto_panel").getBoundsInRoot()
        u = u.copy(progress = u.progress.copy(done = setOf("ref")))
        compose.waitForIdle()
        val panel = compose.onNodeWithTag("tuto_panel").getBoundsInRoot()
        assertEquals(panelBefore.bottom - panelBefore.top, panel.bottom - panel.top)
        val text = compose.onNodeWithTag("tuto_text").getBoundsInRoot()
        val next = compose.onNodeWithTag("tuto_next_big").getBoundsInRoot()
        val listen = compose.onNodeWithTag("tuto_listen").getBoundsInRoot()
        val prev = compose.onNodeWithTag("tuto_prev").getBoundsInRoot()
        // The words end above the green button (they were cut under it), the example beside « Précédent ».
        assertTrue("text ${text.bottom} under next ${next.top}", text.bottom <= next.top + 1.dp)
        assertTrue(kotlin.math.abs((listen.top - prev.top).value) < 12f)
    }

    @Test
    fun theEndListsWhatIsLeftThenSaysBravo() {
        val chapters = TutoCourse.of("jump_up")!!.chaptersFor(TutoLevel.Facile)
        val all = chapters.flatMap { it.steps }
        val last = all.last()
        val left = all.take(2).map { it.id }.toSet()
        var u by androidx.compose.runtime.mutableStateOf(ui(TutoLevel.Facile, last.id, done = all.map { it.id }.toSet() - left, atEnd = true))
        val went = ArrayList<Pair<Int, Int>>()
        compose.setContent { PocketStudioTheme { TutoPanel(u, canShow = false, actions = TutoActions(goTo = { c, s -> went += c to s }), reducedMotion = true) } }
        compose.waitForIdle()
        compose.onNodeWithTag("tuto_left_count").assertIsDisplayed()
        assertEquals(0, compose.onAllNodesWithTag("tuto_do").fetchSemanticsNodes().size)
        assertEquals(0, compose.onAllNodesWithTag("tuto_mark").fetchSemanticsNodes().size)
        compose.onNodeWithTag("tuto_left_${all[1].id}").performClick()
        assertEquals(listOf(u.placeOf(all[1])), went)
        // All done: « Bravo », and no « Je le fais / C'est fait / Suivant » after the end.
        u = ui(TutoLevel.Facile, last.id, done = all.map { it.id }.toSet())
        compose.waitForIdle()
        compose.onNodeWithTag("tuto_finished").assertIsDisplayed()
        for (tag in listOf("tuto_do", "tuto_mark", "tuto_next", "tuto_next_big")) assertEquals(tag, 0, compose.onAllNodesWithTag(tag).fetchSemanticsNodes().size)
    }

    @Test
    fun theFoldedDemoCaptionIsWhole() {
        val u = ui(TutoLevel.Facile, "lead_do").copy(open = false, demo = TutoDemoPhase.HOLDING)
        compose.setContent { PocketStudioTheme { TutoPanel(u, canShow = true, actions = TutoActions(), reducedMotion = true) } }
        compose.onNodeWithText("Voilà ! À toi maintenant.").assertIsDisplayed()
    }

    @Test
    fun theBannerSaysTheBarTheGridTurnedTo() {
        assertEquals("Mesure 2 →", tutoBarBanner(1, forward = true))
        assertEquals("← Mesure 1", tutoBarBanner(0, forward = false))
        TutoHints.banner = tutoBarBanner(1, true)
        compose.setContent { PocketStudioTheme { TutoBanner() } }
        compose.onNodeWithTag("tuto_banner").assertIsDisplayed()
        // It goes by itself.
        compose.mainClock.advanceTimeBy(TUTO_BANNER_MS + 200)
        compose.waitForIdle()
        assertNull(TutoHints.banner)
    }
}

/** [this] showing scene [i] (as « Aller dans la partie »). */
private fun Project.switchSceneForTest(i: Int): Project = switchScene(i)

/** The Drop's crash taken away (the step still to do). */
private fun Project.withoutCrash(): Project {
    val drop = allScenes.indexOfFirst { it.label == "Drop" }
    return switchSceneForTest(drop).let { p ->
        p.copy(drums = p.drums.copy(lanes = p.drums.lanes.mapIndexed { i, l -> if (i == TutoChecks.CRASH_LANE) l.copy(hits = emptyMap()) else l }))
    }.switchSceneForTest(scene)
}

/** Only the target's first scene (the Groove), no song yet. */
private fun Project.keepOnlyFirstSceneForTest(): Project {
    val full = allScenes.indices.maxBy { TutoChecks.bricks(allScenes[it]).size }
    return switchSceneForTest(full).let { p -> p.copy(scenes = emptyList(), song = emptyList(), scene = 0) }
}
