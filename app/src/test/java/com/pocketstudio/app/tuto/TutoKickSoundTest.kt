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
import com.pocketstudio.app.guide.GuideListenPlayer
import com.pocketstudio.app.library.SoundLibraryViewModel
import com.pocketstudio.app.nav.AppHarness
import com.pocketstudio.app.rhythm.kitName
import com.pocketstudio.engine.OfflineExport
import com.pocketstudio.model.Project
import com.pocketstudio.model.StudioDrums
import com.pocketstudio.model.tuto.TutoChecks
import com.pocketstudio.model.tuto.TutoLevel
import com.pocketstudio.model.tuto.TutoProgress
import com.pocketstudio.model.tuto.TutoStep
import com.pocketstudio.model.tuto.TutoTarget
import org.junit.After
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

    @After
    fun end() {
        TutoTestHooks.player = null
        h.finish()
    }

    private fun idle(ms: Long = 50) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))

    private fun await(what: String, timeoutMs: Long = 20_000, ok: () -> Boolean) {
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

    /** Karim's gesture: a long press on the kick's pad, then a sound of its sheet. */
    private fun pickOnTheKickPad(tile: String) {
        compose.onNodeWithTag("pad_0").performTouchInput { longClick() }
        await("the kick's sheet") { has("pad_sheet") }
        click(tile)
    }

    @Test
    fun karimsGesturesOnTheOldStep() {
        atTheKickStep(oldKit = true)
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
        await("demo over", 60_000) { !has("tuto_demo_caption") }
        idle(400)
        assertEquals(before.padVoices, p().padVoices)
        // « J'ai compris »: ticked, on to the next step.
        click("tuto_mark")
        await("ticked") { "kick_sound" in progress().done }
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
}
