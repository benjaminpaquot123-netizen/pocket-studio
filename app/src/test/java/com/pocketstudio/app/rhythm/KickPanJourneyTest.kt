package com.pocketstudio.app.rhythm

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.library.SoundLibraryViewModel
import com.pocketstudio.app.nav.AppHarness
import com.pocketstudio.model.Project
import com.pocketstudio.model.StarterWizard
import com.pocketstudio.model.allScenes
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Benjamin's « Jump up DnB » (APK 181, the assistant « pas à pas », 11 steps): the kick ended
 * hard left (drums.lanes[0].pan == -1) though he never touched its pan. The only writers of a
 * pad's pan in the app are the two « Pan du pad » sliders: RhythmScreen's PadSoundCard (on the
 * selected pad, pad 0 = the kick unless another pad was chosen) and the long-press pad sheet.
 * Both are full-width Material sliders starting a few dp from the screen's left edge, inside the
 * system's back-gesture zone, with no system gesture exclusion: a back swipe from the left edge
 * at their height reaches the slider first (down, a few moves), the slider jumps to the finger
 * (its left end: -1) and the system then takes the gesture (cancel) and goes back. The pan stays
 * at -1 and nothing says so.
 *
 * The journey: the wizard's 11 steps on jump_up, the song opened, « Écouter » (the song's
 * follow, simulated by showing each part's scene in song order, as the follow does), the back
 * gesture while the Intro (scene A) plays, then the rest of the song up to B where it stopped.
 * Before the safe pan slider (lot 2), the per-scene copies of the kick's pan then matched his
 * file exactly (A-D, G, H at -1; E, F, I at 0) and the kick was hard left. Now the pan sliders
 * take a drag only from their thumb, leave the system's edge zone to the system and put back the
 * value a cancelled gesture had moved: the kick stays centred in every scene.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp-xxhdpi")
class KickPanJourneyTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val h = AppHarness(compose)

    @After
    fun end() = h.finish()

    /** The kick's pan in each of the first nine scenes (A..I), as saved. */
    private fun Project.kickPans(): List<Float> = allScenes.take(9).map { it.drums.lanes[0].pan }

    private fun kick(): Float = h.vm.project.value.drums.lanes[0].pan

    private fun assertCentred(after: String) {
        assertEquals("kick pan after $after", 0f, kick(), 0f)
        assertEquals("kick pan of every scene after $after", List(9) { 0f }, h.vm.project.value.kickPans())
    }

    /** The assistant on jump_up, through its 11 steps, then « Créer le morceau ». */
    private fun wizardJumpUp(): Project {
        val lib = SoundLibraryViewModel(compose.activity.application)
        for (v in 0 until 64) lib.kitCache[v] = Pcm(FloatArray(960), 2, 48000)
        lib.openWizard()
        lib.chooseStyle("jump_up")
        lib.stopPreview()
        assertEquals(11, lib.wizard.value.steps.size)
        while (lib.wizard.value.step < lib.wizard.value.lastStep) lib.next()
        var made: Project? = null
        lib.create { p, _, _ -> made = p }
        val end = System.currentTimeMillis() + 30_000
        while (made == null) {
            shadowOf(Looper.getMainLooper()).idle()
            check(System.currentTimeMillis() < end) { "the assistant made nothing" }
            Thread.sleep(2)
        }
        lib.closeWizard()
        return made!!
    }

    /** The song opened as AppRoot opens it, then « Écouter »: the screens follow E, F, then A plays. */
    private fun openAndListenToTheIntro() {
        val song = wizardJumpUp()
        assertEquals("jump_up", song.style)
        assertEquals(0f, song.drums.lanes[0].pan, 0f)
        h.start { session.edit { song } }
        assertCentred("opening the song")
        // The song's first parts: E (Intro 2), F (Intro 3), then A (Intro).
        assertEquals(listOf(4, 5, 0), h.vm.project.value.song.take(3).map { it.scene })
        for (scene in listOf(4, 5, 0)) {
            h.vm.selectScene(scene)
            compose.waitForIdle()
            assertCentred("the song reaching scene $scene")
        }
        // The Sons space: the drums, the pads, the selected pad's card (pad 0, the kick).
        compose.onNodeWithTag("tab_Sons").performClick()
        compose.waitForIdle()
        assertCentred("opening Sons")
        assertEquals(0, h.vm.transport.value.selectedPad)
    }

    /** The rest of the song as it plays on, stopped on B (Benjamin's file is on scene 1). */
    private fun playOnToB() {
        for (scene in listOf(1, 6, 2, 7, 3, 1)) {
            h.vm.selectScene(scene)
            compose.waitForIdle()
        }
        assertEquals(1, h.vm.project.value.scene)
    }

    /**
     * A back swipe from the screen's left edge, the finger landing [edgeDp] from it at the height
     * of [node]: the app gets the touch and a few moves (10 dp each), then the system takes the
     * gesture (the pointers cancelled) and goes back.
     */
    private fun backSwipeFromLeftEdge(
        node: androidx.compose.ui.test.SemanticsNodeInteraction,
        edgeDp: Float,
        back: () -> Unit = { compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() } },
    ) {
        val left = node.fetchSemanticsNode().boundsInRoot.left
        val px = compose.density.density
        node.performTouchInput {
            down(Offset(edgeDp * px - left, height / 2f))
            repeat(2) { moveBy(Offset(10f * px, 0.5f * px), delayMillis = 8) }
            cancel()
        }
        back()
        compose.mainClock.advanceTimeBy(1000)
        compose.waitForIdle()
    }

    /** A quick vertical flick (10 frames of [dx], [dy] px) from 30 % of [node]'s width. */
    private fun flick(node: androidx.compose.ui.test.SemanticsNodeInteraction, dx: Float, dy: Float) {
        node.performTouchInput {
            down(Offset(width * 0.3f, height / 2f))
            repeat(10) { moveBy(Offset(dx, dy), delayMillis = 16) }
            up()
        }
        compose.waitForIdle()
    }

    private fun padSoundPan() = compose.onNode(
        SemanticsMatcher("a -1..1 slider") { it.config.getOrNull(SemanticsProperties.ProgressBarRangeInfo)?.range == -1f..1f }
            and hasAnyAncestor(hasTestTag("pad_sound")),
    )

    @Test
    fun aBackSwipeOnTheSonsPageLeavesTheKickCentred() {
        openAndListenToTheIntro()
        // The selected pad's card, under the steps: its « Pan du pad » slider (-1..1), the kick's.
        val pan = padSoundPan()
        pan.performScrollTo()
        compose.waitForIdle()
        assertCentred("bringing the pad's card into view")
        // Back to where he came from: a swipe from the left edge, the thumb at the slider's height.
        backSwipeFromLeftEdge(pan, edgeDp = 28f)
        playOnToB()
        // Benjamin's file had A-D, G, H at -1 here: a back gesture is not a pan.
        assertCentred("a back swipe on Sons, the song played on to B")
    }

    @Test
    fun closingTheKicksPadSheetWithTheBackGestureLeavesItCentred() {
        openAndListenToTheIntro()
        // « Appui long : son, volume. »: the kick's sheet.
        compose.onNodeWithTag("pad_0").performTouchInput { longClick() }
        compose.waitForIdle()
        compose.onNodeWithTag("pad_sheet").assertExists()
        assertCentred("holding the kick's pad")
        // The sheet closed with the back gesture, the thumb at the height of its last slider (the
        // sheet's own window takes the back: closed here as its dismissal does, « Fermer »).
        backSwipeFromLeftEdge(compose.onNodeWithTag("pad_pan"), edgeDp = 20f) { compose.onNodeWithTag("pad_sheet_close").performClick() }
        compose.onNodeWithTag("pad_sheet").assertDoesNotExist()
        playOnToB()
        // Closing the sheet is not a pan.
        assertCentred("closing the pad sheet with back, the song played on to B")
    }

    /** A real pan still works: a drag from the thumb, one undo step, one journal line, a badge on the pad. */
    @Test
    fun aDragFromTheThumbPansTheKickWithOneJournalLine() {
        h.start()
        compose.onNodeWithTag("tab_Sons").performClick()
        compose.waitForIdle()
        com.pocketstudio.app.report.Journal.clear()
        val pan = padSoundPan()
        pan.performScrollTo()
        compose.waitForIdle()
        pan.performTouchInput {
            down(center)
            repeat(10) { moveBy(Offset(width * 0.03f, 0f), delayMillis = 16) }
            up()
        }
        compose.waitForIdle()
        assertTrue("the kick went right: ${kick()}", kick() >= 0.3f)
        val lines = com.pocketstudio.app.report.Journal.events.snapshot().map { it.text }.filter { it.startsWith("Pan du pad 1") }
        assertEquals("one line for the gesture: $lines", 1, lines.size)
        assertTrue(lines.single(), lines.single().contains("centre → D"))
        compose.onNodeWithTag("pad_pan_badge", useUnmergedTree = true).assertExists()
        h.vm.undo()
        compose.waitForIdle()
        assertEquals("one undo step", 0f, kick(), 0f)
        compose.onNodeWithTag("pad_pan_badge", useUnmergedTree = true).assertDoesNotExist()
    }

    /** Control: the same journey without the stray gesture keeps the kick centred everywhere. */
    @Test
    fun withoutTheStrayGestureTheKickStaysCentredThroughTheJourney() {
        openAndListenToTheIntro()
        compose.onNodeWithTag("pad_0").performTouchInput { longClick() }
        compose.waitForIdle()
        compose.onNodeWithTag("pad_sheet_close").performClick()
        compose.waitForIdle()
        assertCentred("the pad sheet opened and closed")
        // Scrolling the page over the pad's sliders, even drifting 45° sideways, scrolls: Compose
        // gives a gesture that is not mostly horizontal to the page, not to the slider.
        padSoundPan().performScrollTo()
        compose.waitForIdle()
        flick(padSoundPan(), dx = -30f, dy = -30f)
        flick(padSoundPan(), dx = -10f, dy = 35f)
        assertCentred("scrolling the Sons page over « Pan du pad »")
        for (tab in listOf("tab_Mix", "tab_Structure", "tab_Sons")) {
            compose.onNodeWithTag(tab).performClick()
            compose.waitForIdle()
            assertCentred(tab)
        }
        playOnToB()
        assertCentred("the song played on to B")
        assertEquals(StarterWizard.style("jump_up")!!.id, h.vm.project.value.style)
    }
}
