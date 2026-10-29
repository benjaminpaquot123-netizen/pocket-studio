package com.pocketstudio.app.nav

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.library.MySoundLibrary
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.model.InstrumentTrack
import com.pocketstudio.model.track
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import kotlin.math.PI
import kotlin.math.sin

/**
 * « Ma bibliothèque de sons » (owner, 2026-10-29) in the whole app, Mode simple: a pad's own sound
 * kept in one touch, found again in « Changer le son » in its family with an « À moi » badge,
 * renamed and filed under « Voix », put on another pad and played on the keys, then deleted; the
 * project keeps its copy.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp")
class MySoundsJourneyTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val h = AppHarness(compose)

    private val libraryDir get() = File(h.app.filesDir, "mysounds")

    @After
    fun end() {
        h.finish()
        NativeEngine.sink = null
        libraryDir.deleteRecursively()
        kotlinx.coroutines.runBlocking { MySoundLibrary.refresh(h.app) }
    }

    private fun exists(tag: String) = compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()

    private fun tap(tag: String) {
        val node = compose.onNodeWithTag(tag)
        if (!node.isDisplayed()) node.performScrollTo()
        node.performClick()
        compose.waitForIdle()
    }

    private fun until(what: String, ok: () -> Boolean) {
        val end = System.currentTimeMillis() + 20_000
        while (!ok()) {
            check(System.currentTimeMillis() < end) { what }
            Thread.sleep(20)
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
            compose.waitForIdle()
        }
    }

    /**
     * [action] with the bench's engine standing in for the phone's (a sound is made for it), until
     * [done]; then without it again: the screens' own polls need the phone's library.
     */
    private fun withEngine(what: String, done: () -> Boolean, action: () -> Unit) {
        NativeEngine.sink = com.pocketstudio.app.human.BenchEngine()
        try {
            action()
            until(what, done)
            // The engine's last commands of the sound (its pad, its track, the note heard and its end) sent before it goes.
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(3))
            compose.waitForIdle()
        } finally {
            NativeEngine.sink = null
        }
    }

    /** A short A (440 Hz), stereo, 0.4 s. */
    private fun cry() = Pcm(FloatArray(48_000 * 2 * 4 / 10) { i -> (0.5 * sin(2 * PI * 440 * (i / 2) / 48_000.0)).toFloat() }, 2, 48_000)

    @Test
    fun keepRenameFindOnAPadAndOnTheKeysThenDelete() {
        libraryDir.deleteRecursively()
        h.appViewModelsOfThisTest()
        h.start(simple = true)
        h.vm.selectPad(1)
        withEngine("the sound never reached the pad", { h.vm.project.value.drums.lanes[1].sample != null }) { h.vm.placeOnPad(cry(), "Mon cri") }
        val first = h.vm.project.value.drums.lanes[1].sample!!
        tap("tab_simple_Rythme")

        // One touch: kept for every project, in the pad's family (pad 2 is a kick), said where to find it.
        tap("pad_keep_library")
        until("never kept") { MySoundLibrary.sounds.value.isNotEmpty() }
        val kept = MySoundLibrary.sounds.value.single()
        assertEquals("Mon cri", kept.name)
        assertEquals("kick", kept.kind)
        assertEquals(0.4, kept.seconds, 0.01)
        compose.onNodeWithText("« Mon cri » est dans ta bibliothèque", substring = true).assertExists()
        compose.onNodeWithText("Compris").performClick()
        compose.waitForIdle()

        // « Changer le son »: the library opens on the pad's family, « À moi » first, the same tiles with a badge.
        tap("open_library")
        h.awaitTag("library_mine_0")
        compose.onNodeWithTag("library_mine_0").assertTextContains("Mon cri", substring = true).assertTextContains("À moi", substring = true)

        // Renamed and filed under « Voix ».
        compose.onNodeWithTag("library_mine_edit_0").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("library_mine_name").performTextReplacement("Cri perso")
        compose.onNodeWithTag("library_mine_kind_vocal").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("library_mine_save").performClick()
        compose.waitForIdle()
        until("never renamed") { MySoundLibrary.sounds.value.single().let { it.name == "Cri perso" && it.kind == "vocal" } }
        assertFalse("not with the kicks any more", exists("library_mine_0"))

        // Found in « Voix » (its family, first in the row now), heard, then put on the pad.
        tap("library_kind_vocal")
        h.awaitTag("library_mine_0")
        withEngine("never placed", { h.vm.project.value.drums.lanes[1].sample?.name == "Cri perso" }) { tap("library_mine_use_0") }
        // A copy of its own in the project, never the library's file.
        val placed = h.vm.project.value.drums.lanes[1].sample!!
        assertNotEquals(first.file, placed.file)
        assertFalse(exists("library"))

        // On the keys (« Jouer au clavier »): the Clavier's « Changer le son » shows it too.
        tap("tab_simple_Clavier")
        tap("simple_change_sound")
        h.awaitTag("keys_mine_0")
        compose.onNodeWithTag("keys_mine_0").assertTextContains("À moi", substring = true)
        val track = h.vm.selectedTrack.value
        withEngine("never on the keys", { h.vm.project.value.track(track).let { it.engine == InstrumentTrack.ENGINE_SAMPLED && it.sampled.name == "Cri perso" } }) {
            tap("keys_mine_0")
        }

        // Deleted from the library: gone from the list and the phone, the project keeps its copy.
        val libraryFile = File(libraryDir, kept.file)
        assertTrue(libraryFile.isFile)
        tap("tab_simple_Rythme")
        tap("open_library")
        tap("library_kind_vocal")
        h.awaitTag("library_mine_edit_0")
        compose.onNodeWithTag("library_mine_edit_0").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("library_mine_delete").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("library_mine_delete_confirm").performClick()
        compose.waitForIdle()
        until("never deleted") { MySoundLibrary.sounds.value.isEmpty() }
        assertFalse(libraryFile.exists())
        assertEquals("Cri perso", h.vm.project.value.drums.lanes[1].sample?.name)
        assertTrue(h.vm.padSamples.info.value.containsKey(placed.file))
    }
}
