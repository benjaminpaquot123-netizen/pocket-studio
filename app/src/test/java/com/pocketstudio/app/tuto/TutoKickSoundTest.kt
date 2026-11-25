package com.pocketstudio.app.tuto

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.lifecycle.ViewModelProvider
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.library.IndexState
import com.pocketstudio.app.library.PacksUi
import com.pocketstudio.app.guide.GuideListenPlayer
import com.pocketstudio.app.library.SoundLibraryViewModel
import com.pocketstudio.app.nav.AppHarness
import com.pocketstudio.app.rhythm.kitName
import com.pocketstudio.engine.OfflineExport
import com.pocketstudio.model.PackEntry
import com.pocketstudio.model.PackIndex
import com.pocketstudio.model.Project
import com.pocketstudio.model.StudioDrums
import com.pocketstudio.model.tuto.TutoChecks
import com.pocketstudio.model.tuto.TutoLevel
import com.pocketstudio.model.tuto.TutoProgress
import com.pocketstudio.model.tuto.TutoStep
import com.pocketstudio.model.tuto.TutoTarget
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Before
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
 * Karim (tester, Pro, 2026-10-28, screens b/26-43): « Choisis un kick court et sec » never ticked.
 * His gestures, done again in the app: a long press on the « Kick » pad, « Kick 2 » on its sheet,
 * « Kick » again, « Kit ▾ ». Why it never ticked: (1) the step watched only the sound ON pad 1 (its
 * own sample or studio voice), and the one gesture that puts one there renders a built-in voice
 * through the engine, which his simulated phone has not (« Ce son n'a pas pu être lu ») ; (2) on a
 * phone it would have ticked for « Kick », the very same sound, and never for a kit chosen in
 * « Kit ▾ » (not watched); (3) the course's own journey only ticked it by writing a pad voice no
 * screen can set. Now the tutorial starts on the drum & bass kit (its short dry kick), the step
 * says so, compares a long kick with it in « Montre-moi » and is ticked by « J'ai compris ».
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp")
class TutoKickSoundTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()
    private val h = AppHarness(compose)
    private val target = TutoTarget.of("jump_up")!!

    private object Silent : GuideListenPlayer {
        override fun play(pcm: Pcm, ended: () -> Unit) = true
        override fun stop() = Unit
        override fun heard(): Long = 0L
    }

    /**
     * Tour 9, lot « tests-fragiles »: the network is cut for these tests (every URL the app opens goes to a closed port)
     * and each address it asks for is kept, so a step that leaves the catalogue to the network is seen, whatever the
     * machine's network does.
     */
    private val asked = java.util.concurrent.CopyOnWriteArrayList<java.net.URI>()
    private val before: java.net.ProxySelector? = java.net.ProxySelector.getDefault()

    @Before
    fun cutTheNetwork() {
        java.net.ProxySelector.setDefault(object : java.net.ProxySelector() {
            override fun select(uri: java.net.URI): List<java.net.Proxy> {
                asked += uri
                return listOf(java.net.Proxy(java.net.Proxy.Type.HTTP, java.net.InetSocketAddress("127.0.0.1", 9)))
            }
            override fun connectFailed(uri: java.net.URI?, sa: java.net.SocketAddress?, ioe: java.io.IOException?) = Unit
        })
    }

    @After
    fun end() {
        TutoTestHooks.player = null
        TutoTestHooks.packs = null
        h.finish()
        java.net.ProxySelector.setDefault(before)
    }

    private fun idle(ms: Long = 50) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))

    // The real state is waited for; the cap only stops a hang (audit stage 1: 20 s of wall clock ran out under a loaded CI).
    private fun await(what: String, timeoutMs: Long = 120_000, ok: () -> Boolean) {
        val end = System.currentTimeMillis() + timeoutMs
        while (!ok()) {
            idle(50)
            compose.waitForIdle()
            Thread.sleep(5)
            check(System.currentTimeMillis() < end) { "$what: not in time" }
        }
    }

    private fun has(tag: String) = compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    private fun click(tag: String) {
        // Round 3, n° 28: the kit's offer is at the end of the step's words: read down to it, as a person does.
        if (tag.startsWith("tuto_kit")) readToTheEnd()
        val node = compose.onNodeWithTag(tag)
        runCatching { node.performScrollTo() }
        node.performClick()
        compose.waitForIdle()
    }
    private fun p(): Project = h.vm.project.value
    private fun progress(): TutoProgress = PrefsTutoStore(h.app).load(h.vm.projectId)!!

    /** The Pro tutorial on its kick's sound step, the kick written, the Sons space shown (Karim's screen 26). */
    private fun atTheKickStep(oldKit: Boolean = false) {
        TutoTestHooks.player = Silent
        h.start(simple = false, build = {
            // [oldKit]: the tutorial's project as the testers had it (the studio kit's own kick).
            session.edit { target.blank().let { if (oldKit) it.copy(padVoices = it.padVoices - 0) else it }.let { b -> b.copy(drums = b.drums.copy(lanes = b.drums.lanes.mapIndexed { i, l -> if (i == 0) l.copy(hits = TutoChecks.hint(TutoStep("k", "", check = "lane:kick"), target, 16)!!.second.associateWith { com.pocketstudio.model.Hit(1f) }) else l })) } }
            PrefsTutoStore(h.app).save(projectId, TutoProgress("jump_up", TutoLevel.Pro, setOf("ref", "tempo", "key", "kick_hear", "kick_do"), "kick_sound"))
        })
        await("panel") { has("tuto_panel") }
        click("tab_Sons")
    }

    /**
     * Karim's gesture: the kick's sheet (since 2026-11-04 the pad card's « Réglages du pad », a long
     * press now opens « Changer le son »), then a sound of its sheet.
     */
    private fun pickOnTheKickPad(tile: String) {
        h.vm.selectPad(0)
        compose.waitForIdle()
        click("pad_settings")
        await("the kick's sheet") { has("pad_sheet") }
        click(tile)
    }

    /**
     * Tour 9, lot « kick-ci »: the packs' catalogue the tutorial reads. Left to the network, Karim's test went red on
     * GitHub's runners only (CI 45508694147, twice): there the real catalogue loads, the style's kit card takes the panel's
     * place and the pad card's « Réglages du pad » has no room left on the 360 dp phone (bounds 0 × 0), so its tap
     * opened nothing; without the network (a laptop's run) no card came and the test was green. Both are played here.
     */
    private fun catalogue(withTheStylesPacks: Boolean) {
        val s = target.style
        val ids = (s.lines.filter { it.voice == null }.mapNotNull { s.kit[it.role] } + s.id).distinct()
        val index = if (withTheStylesPacks) IndexState.Ready(PackIndex(ids.map { PackEntry(it, "Pack $it", "$it.zip", "a".repeat(64), 1_500_000, 12) })) else IndexState.Unavailable
        TutoTestHooks.packs = MutableStateFlow(PacksUi(index, emptyList()))
    }

    @Test
    fun karimsGesturesOnTheOldStep() {
        catalogue(withTheStylesPacks = false)
        karimsGestures(kitCard = false)
    }

    /** The same gestures with the style's packs in the catalogue (GitHub's runners, a phone online): its kit card comes first. */
    @Test
    fun karimsGesturesWithTheKitCardOffered() {
        catalogue(withTheStylesPacks = true)
        karimsGestures(kitCard = true)
    }

    private fun karimsGestures(kitCard: Boolean) {
        atTheKickStep(oldKit = true)
        // The kit card asks first (« Télécharger » / « Plus tard »): put away, as a person does before the pads.
        assertEquals("the kit card offered", kitCard, putTheKitCardAway())
        if (kitCard) {
            // The open panel keeps the height it took with the card while its step stays (lot « tuto 3 »: it never changes
            // height by itself): on the 360 × 780 phone the pad card has no room left under it (« Réglages du pad » 0 × 0).
            // Karim folds the panel (« Replier »), as a person does to reach the pads.
            click("tuto_fold")
            await("the panel folded") { !has("tuto_mark") }
        }
        val old = TutoStep("kick_sound", "", check = "changed:kit")
        val base = TutoChecks.signature("kit", p())
        val lib = ViewModelProvider(compose.activity)[SoundLibraryViewModel::class.java]
        // His phone had no engine to render « Kick 2 »: nothing lands on the pad, the step cannot tick.
        pickOnTheKickPad("pad_sound_1")
        await("the library says it") { lib.packs.value.message == "Ce son n'a pas pu être lu." }
        idle(300)
        assertNull("no sound on the pad", p().drums.lanes[0].sample)
        assertFalse(TutoChecks.done(old, p(), target, base))
        // « Kit ▾ » › Classique: another kit, not watched by the old step either.
        h.vm.chooseKit("builtin:1")
        compose.waitForIdle()
        assertFalse("a kit chosen never ticked it", TutoChecks.done(old, p(), target, base))
        h.vm.undo()
        // On a phone the built-in voice renders and lands on the pad as its sound: « Kick », the very
        // same kick as before, would have ticked it.
        val same = p().copy(drums = p().drums.withSample(0, com.pocketstudio.model.PadSample("kick.wav", "Kick")))
        assertTrue("the old step ticked for the very same kick", TutoChecks.done(old, same, target, base))
    }

    @Test
    fun theKickStepNowSaysShowsAndTicks() {
        // The catalogue is the test's, never the network's: no style pack in it, so no kit card (the card's cases are below).
        catalogue(withTheStylesPacks = false)
        kickStepSaysShowsAndTicks()
    }

    /**
     * Tour 9, lot « tests-fragiles »: the same step with the style's packs in the catalogue from the start, as on a runner
     * or a phone online (the network answers before the panel is drawn): the kit card takes the panel's place first.
     */
    @Test
    fun theKickStepNowSaysShowsAndTicksWithTheKitCardOffered() {
        catalogue(withTheStylesPacks = true)
        kickStepSaysShowsAndTicks()
    }

    /**
     * Tour 9, lot « tests-fragiles »: the catalogue answers while the step is under way, as the network did on a laptop
     * online (the test left the catalogue to the network): [at] says when (« demo », « mark »: right before « J'ai compris »).
     */
    @Test
    fun theKickStepNowSaysShowsAndTicksWhenTheCatalogueComesDuringTheDemo() = catalogueLate("demo")

    @Test
    fun theKickStepNowSaysShowsAndTicksWhenTheCatalogueComesBeforeTheTick() = catalogueLate("mark")

    private fun catalogueLate(at: String) {
        val late = MutableStateFlow(PacksUi(IndexState.Loading, emptyList()))
        TutoTestHooks.packs = late
        catalogue(withTheStylesPacks = true)
        val ready = TutoTestHooks.packs!!.value
        TutoTestHooks.packs = late
        kickStepSaysShowsAndTicks { moment -> if (moment == at) { late.value = ready; idle(200); compose.waitForIdle() } }
    }

    private fun kickStepSaysShowsAndTicks(meanwhile: (String) -> Unit = {}) {
        atTheKickStep()
        // The tutorial starts on the drum & bass kit: its short dry kick, said on « Kit ▾ ».
        assertTrue(StudioDrums.isDnbKick(p().padVoices[0]))
        assertEquals("Drum & bass", kitName(p()))
        compose.onNodeWithTag("kit_chip").assertTextContains("Drum & bass", substring = true)
        await("kick_sound shown") { progress().current == "kick_sound" }
        assertFalse("not ticked by itself", "kick_sound" in progress().done)
        // « Montre-moi »: a long kick, then the short one, heard; the project as it was after.
        val before = p()
        assertTrue(has("tuto_show"))
        click("tuto_show")
        await("demo shown") { has("tuto_demo_caption") }
        meanwhile("demo")
        await("demo over", 60_000) { !has("tuto_demo_caption") }
        idle(400)
        assertEquals(before.padVoices, p().padVoices)
        // Round 1 (n° 34): the style's kit card takes the panel's place (no longer stacked on it): « Plus tard ».
        putTheKitCardAway()
        meanwhile("mark")
        // « J'ai compris »: ticked, on to the next step.
        click("tuto_mark")
        await("ticked") { "kick_sound" in progress().done }
        // Offline and the same on every machine: the step never asked the network for the catalogue.
        assertEquals(emptyList<java.net.URI>(), asked.toList())
    }

    /** « Kit ▾ » offers the drum & bass kit built in, in one undo step; « Studio » takes it back. */
    @Test
    fun theDrumAndBassKitIsBuiltIn() {
        atTheKickStep()
        h.vm.chooseKit("builtin:3")
        compose.waitForIdle()
        assertFalse(StudioDrums.isDnbKick(p().padVoices[0]))
        assertEquals("Studio", kitName(p()))
        assertTrue(h.vm.kitChoices(emptyList(), com.pocketstudio.app.rhythm.LibraryState.Missing).any { it.id == "kitdnb:jump_up" && it.name == "Drum & bass" })
        h.vm.chooseKit("kitdnb:jump_up")
        compose.waitForIdle()
        assertTrue(StudioDrums.isDnbKick(p().padVoices[0]))
        assertEquals(Project.KIT_V3, p().kitVersion)
        assertEquals("Drum & bass", kitName(p()))
        h.vm.undo()
        assertFalse(StudioDrums.isDnbKick(p().padVoices[0]))
    }

    /** The style's kit card, when it shows, put away by its « Plus tard »: true when it showed. */
    private fun putTheKitCardAway(): Boolean {
        if (!has("tuto_kit")) return false
        // Round 2 (n° 22): a card that came by itself takes no touch for a moment.
        compose.mainClock.advanceTimeBy(1_100L)
        click("tuto_kit_later")
        await("the panel back") { has("tuto_mark") }
        return true
    }

    /** « Lire la suite » until the step's words are read to their end. */
    private fun readToTheEnd() {
        repeat(12) {
            if (compose.onAllNodesWithTag("tuto_text_more", useUnmergedTree = true).fetchSemanticsNodes().isEmpty()) return
            compose.onNodeWithTag("tuto_text_more").performClick()
            compose.mainClock.advanceTimeBy(600)
            compose.waitForIdle()
        }
    }
}
