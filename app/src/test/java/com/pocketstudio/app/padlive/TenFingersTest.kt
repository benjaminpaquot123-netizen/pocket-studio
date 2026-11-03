package com.pocketstudio.app.padlive

import android.app.Application
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.model.LiveClip
import com.pocketstudio.model.LivePads
import com.pocketstudio.model.PadLive
import com.pocketstudio.model.PadLiveSet
import com.pocketstudio.model.PadMode
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Lot D (USAGE-TABLETTE-SMARTPHONE §5 point 2): ten fingers on Pad Live's pads at once, on a
 * tablet lying down. Each finger plays its own pad on its way down, none waits for another to
 * lift; a « tenir » pad held by one finger sounds until that finger lifts, whatever the nine
 * others do. Through the screen's own gestures into the app's Pad Live controller.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w1280dp-h800dp-land-mdpi")
class TenFingersTest {
    @get:Rule
    val compose = createComposeRule()
    private val app: Application get() = ApplicationProvider.getApplicationContext()

    private lateinit var vm: RhythmViewModel
    /** The pads sounding, as the engine would say (each toggle on or off). */
    private val sounding = HashSet<Int>()
    /** Every toggle the controller sent, in order. */
    private val toggled = ArrayList<Int>()

    @Before
    fun setUp() {
        File(app.filesDir, "projects").deleteRecursively()
        vm = RhythmViewModel(app)
        // Page A: pad 0 « tenir », the 15 others « coup » (one hit each touch).
        val clips = List(PadLive.PAGE_SIZE) { i ->
            val hit = LiveClip("s$i.wav", "Son $i", 1, oneShot = true)
            if (i == 0) PadLive.withMode(LiveClip("h.wav", "Nappe", 4), PadMode.HOLD) else hit
        }
        vm.session.edit { it.copy(padLive = PadLiveSet(home = true), live = PadLive.placed(LivePads(), 0, clips)) }
        vm.livePadsSoundingForTest = sounding
        vm.padLive.toggleForTest = { pad ->
            toggled += pad
            // A hit sounds and is done; a held pad sounds until it is toggled again.
            if (vm.project.value.live.at(pad)?.oneShot == false) { if (!sounding.remove(pad)) sounding += pad }
        }
        compose.setContent {
            PocketStudioTheme {
                PadLiveScreen(
                    PadLiveUi(vm.project.value.live, List(LivePads.ALL) { 0 }, page = 0, pack = "fete", playing = true, tempoBpm = 124.0, beatsPerBar = 4),
                    PadLiveActions(touch = { pad, down -> vm.padLive.touch(pad, down) }),
                    clockOf = { -1 }, still = true,
                )
            }
        }
        compose.waitForIdle()
    }

    private fun center(pad: Int): Offset = compose.onNodeWithTag("padlive_pad_$pad").fetchSemanticsNode().boundsInRoot.center

    @Test
    fun tenFingersDownAtOncePlayTenPads() {
        val pads = (1..10).toList()
        val at = pads.map(::center)
        compose.onRoot().performTouchInput {
            // All ten down one after the other, none lifted yet.
            pads.forEachIndexed { finger, _ -> down(finger, at[finger]) }
        }
        compose.waitForIdle()
        assertEquals("each finger its pad, on its way down", pads, toggled)
        compose.onRoot().performTouchInput { pads.indices.forEach { finger -> up(finger) } }
        compose.waitForIdle()
        assertEquals("a hit plays on the way down only", pads, toggled)
        // Again, all ten at the same moment: ten more hits.
        toggled.clear()
        compose.onRoot().performTouchInput {
            pads.forEachIndexed { finger, _ -> down(finger, at[finger]) }
        }
        compose.onRoot().performTouchInput { pads.indices.forEach { finger -> up(finger) } }
        compose.waitForIdle()
        assertEquals(pads.toSet(), toggled.toSet())
        assertEquals(10, toggled.size)
    }

    @Test
    fun aHeldPadSoundsUntilItsOwnFingerLiftsWhateverTheNineOthersDo() {
        val held = center(0)
        val others = (1..9).map(::center)
        compose.onRoot().performTouchInput { down(0, held) }
        compose.waitForIdle()
        assertEquals(listOf(0), toggled)
        assertEquals(setOf(0), sounding)
        // Nine fingers drum three times while the first one holds.
        repeat(3) {
            compose.onRoot().performTouchInput {
                others.forEachIndexed { k, p -> down(k + 1, p) }
                others.indices.forEach { k -> up(k + 1) }
            }
            compose.waitForIdle()
        }
        assertEquals("the held pad still sounds", setOf(0), sounding)
        assertEquals(1 + 3 * 9, toggled.size)
        assertEquals((1..9).toList(), toggled.subList(1, 10))
        compose.onRoot().performTouchInput { up(0) }
        compose.waitForIdle()
        assertEquals("let go with its finger", emptySet<Int>(), sounding)
        assertEquals(0, toggled.last())
    }
}
