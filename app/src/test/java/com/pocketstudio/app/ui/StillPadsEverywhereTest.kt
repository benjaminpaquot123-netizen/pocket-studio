package com.pocketstudio.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.pocketstudio.app.padlive.PadLiveActions
import com.pocketstudio.app.padlive.PadLiveScreen
import com.pocketstudio.app.padlive.PadLiveUi
import com.pocketstudio.model.LiveClip
import com.pocketstudio.model.LivePads
import com.pocketstudio.model.PadLive
import com.pocketstudio.ui.PocketStudioTheme
import com.pocketstudio.ui.padLight
import com.pocketstudio.ui.rememberPadLight
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Benjamin's design rule (n° 25), règle « partout »: nothing moves on a pad or a cell that does
 * not play, on every screen with pads or cells (Rythme's pads and steps, the Live page's bricks,
 * the live pads, their full screen, Pad Live); each of them wears the one shared light
 * ([padLight]: inner light, the loop's ring, the finger's 200 ms ripple, the held effect's veil),
 * never a look of its own (a halo that breathes, a sheen, a blinking edge, a bouncing press).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class StillPadsEverywhereTest {
    @get:Rule
    val compose = createComposeRule()

    private val sources = File("src/main/java").walkTopDown().filter { it.extension == "kt" }.associateBy { it.name }

    /** The body of `fun [name](` in [file] (to the next top-level declaration). */
    private fun body(file: String, name: String): String {
        val text = sources.getValue(file).readText()
        val start = Regex("""fun (Modifier\.)?$name\(""").find(text)?.range?.first ?: error("$file: no fun $name")
        val end = Regex("""\n(private |internal )?(fun|class|object|val|@Composable)""").find(text, start + 10)?.range?.first ?: text.length
        return text.substring(start, end)
    }

    /** Every composable that draws a pad or a cell, by screen. */
    private val padsAndCells = listOf(
        "PadLiveScreen.kt" to "PadLivePad",
        // Lot B3: « Fête »'s big pads.
        "PartyMode.kt" to "PartyPad",
        "LiveScreen.kt" to "LivePadCell",
        "LivePadsFullScreen.kt" to "StagePad",
        "LivePadsCard.kt" to "LivePad",
        "RhythmScreen.kt" to "DrumPad",
        "RhythmScreen.kt" to "StepGrid",
        "RhythmScreen.kt" to "AllPadsGrid",
    )

    /** What would move on a pad that does not play: a beat's breath, a sheen, a blink, an endless animation, a bouncing press. */
    private val moving = Regex(
        """rememberInfiniteTransition|withInfiniteAnimationFrameNanos|\.pulse\b|blink\.value|sheen|\.scale\(press|liveGlow\(|\bwave\(""",
    )

    @Test
    fun everyPadAndCellWearsTheSharedLight() {
        padsAndCells.forEach { (file, fn) ->
            val b = body(file, fn)
            assertTrue("$file › $fn does not use the shared light (padLight)", ".padLight(" in b)
            val found = moving.findAll(b).map { it.value }.toList()
            assertTrue("$file › $fn moves on its own: $found", found.isEmpty())
        }
        // The light itself draws nothing for a pad that neither plays, nor waits, nor is touched.
        val light = File("../ui-components/src/main/java/com/pocketstudio/ui/PadLight.kt").readText()
        assertTrue("PadLight.kt must not loop on its own", !Regex("""rememberInfiniteTransition|withInfiniteAnimationFrameNanos|infiniteRepeatable""").containsMatchIn(light))
    }

    @Test
    fun noScreenDrawsAWaveOnItsEmptyDots() {
        val look = sources.getValue("LiveGridLook.kt").readText()
        val dot = look.substringAfter("internal fun DrawScope.emptyDot()").substringBefore("\n}")
        assertTrue("the empty dot reads a clock: $dot", !Regex("""clock|pulse|steps|beat""").containsMatchIn(dot))
    }

    /** The screen as drawn now (its pixels). */
    private fun frame(): IntArray {
        val image = compose.onRoot().captureToImage()
        val out = IntArray(image.width * image.height)
        image.readPixels(out)
        return out
    }

    @Test
    fun anInactivePadNeverChangesByItselfAPlayingOneDoes() {
        compose.mainClock.autoAdvance = false
        var playing by mutableStateOf(false)
        val loop = mutableFloatStateOf(0.1f)
        compose.setContent {
            PocketStudioTheme {
                Row {
                    repeat(4) { i ->
                        val light = rememberPadLight()
                        androidx.compose.foundation.layout.Box(
                            Modifier.size(64.dp).background(Color.DarkGray)
                                .padLight(light, Color.Cyan, playing = playing && i == 0, progress = { loop.floatValue })
                                .testTag("pad_$i"),
                        )
                    }
                }
            }
        }
        compose.mainClock.advanceTimeBy(500)
        val still = frame()
        compose.mainClock.advanceTimeBy(3_333)
        assertTrue("inactive pads changed by themselves", still.contentEquals(frame()))
        // A touch: the 200 ms ripple, then still again, as before.
        compose.onNodeWithTag("pad_2").performTouchInput { down(center); up() }
        compose.mainClock.advanceTimeBy(80)
        assertTrue("the ripple shows", !still.contentEquals(frame()))
        compose.mainClock.advanceTimeBy(400)
        assertTrue("the ripple is gone after 200 ms", still.contentEquals(frame()))
        // Playing: its inner light and its loop's ring, which follows the loop.
        playing = true
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
        val lit = frame()
        assertTrue("the playing pad lights (${still.indices.count { still[it] != lit[it] }} pixels changed)", !still.contentEquals(lit))
        compose.runOnIdle { loop.floatValue = 0.6f }
        compose.mainClock.advanceTimeBy(32)
        assertTrue("the playing pad's ring goes round", !lit.contentEquals(frame()))
    }

    @Test
    fun padLiveWithNothingPlayingNeverMoves() {
        compose.mainClock.autoAdvance = false
        val pads = PadLive.placed(LivePads(), 0, List(6) { LiveClip("s$it.wav", "Son $it", 4, oneShot = it % 2 == 0) })
        compose.setContent {
            PocketStudioTheme {
                PadLiveScreen(
                    PadLiveUi(pads, List(LivePads.ALL) { 0 }, page = 0, pack = "fete", playing = false, tempoBpm = 124.0, beatsPerBar = 4),
                    PadLiveActions(), clockOf = { -1 },
                )
            }
        }
        compose.mainClock.advanceTimeBy(1_000)
        val still = frame()
        compose.mainClock.advanceTimeBy(4_321)
        assertTrue("Pad Live's pads moved with nothing playing", still.contentEquals(frame()))
    }

    /** Lot B3: « Fête » with nothing playing (its room light follows the music): nothing moves either. */
    @Test
    fun partyWithNothingPlayingNeverMoves() {
        compose.mainClock.autoAdvance = false
        val pads = PadLive.placed(LivePads(), 0, List(6) { LiveClip("s$it.wav", "Son $it", 4, oneShot = it % 2 == 0) })
        compose.setContent {
            PocketStudioTheme {
                PadLiveScreen(
                    PadLiveUi(pads, List(LivePads.ALL) { 0 }, page = 0, pack = "fete", playing = false, tempoBpm = 124.0, beatsPerBar = 4),
                    PadLiveActions(), clockOf = { -1 }, readLevels = { it.fill(0.5f) }, partyAtStart = true,
                )
            }
        }
        compose.mainClock.advanceTimeBy(1_000)
        val still = frame()
        compose.mainClock.advanceTimeBy(4_321)
        assertTrue("« Fête » moved with nothing playing", still.contentEquals(frame()))
    }
}
