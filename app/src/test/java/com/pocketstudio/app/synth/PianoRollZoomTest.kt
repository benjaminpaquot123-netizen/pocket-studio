package com.pocketstudio.app.synth

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pinch
import com.github.takahirom.roborazzi.captureRoboImage
import com.pocketstudio.model.Harmony
import com.pocketstudio.model.Note
import com.pocketstudio.model.NoteClip
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
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
 * Owner (lot « tuto 5 », É8, Nadia and Benjamin): every note of a track in sight at once, as in FL
 * Studio Mobile: a pinch zooms the time and the notes, « Tout voir » frames them all, the zoom stays
 * the user's; nothing moves by itself (É1), the tutorial's line says the track and the bar, and
 * « Copier » is there once the motif is written (É2). Picture: app/screenshots/piano_roll_tout_voir.png.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w392dp-h852dp-xxhdpi")
class PianoRollZoomTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val t = NoteClip.TICKS_PER_STEP

    /** A bass and a lead of 8 bars: from Fa1 (29) to Do5 (72), a note every 2 beats. */
    private val wide = NoteClip(steps = 128, notes = (0 until 16).map { i -> Note(i * 8 * t, 4 * t, if (i % 2 == 0) 29 + i else 72 - i) })

    private fun idle() = repeat(6) { shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(50)); compose.waitForIdle() }
    /** Where [tag] is, whole (not cut by the scrolls it is in): its place in the window and its size. */
    private fun bounds(tag: String): Rect = compose.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode().let { n ->
        Rect(n.positionInRoot, androidx.compose.ui.geometry.Size(n.size.width.toFloat(), n.size.height.toFloat()))
    }
    private fun has(tag: String) = compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun toutVoirFramesEveryNote() {
        compose.setContent {
            PocketStudioTheme { androidx.compose.material3.Surface(color = com.pocketstudio.ui.Neon.Background) { PianoRoll(wide, emptyList(), -1, PianoRollActions()) } }
        }
        idle()
        val view = bounds("roll_view")
        // Before: a bar or so across, a few octaves up and down.
        assertTrue(bounds("piano_roll").width > 2 * view.width)
        compose.onNodeWithTag("roll_fit").performClick()
        idle()
        val grid = bounds("piano_roll")
        val keys = bounds("roll_keys")
        // Every bar across the screen.
        assertTrue("${grid.width} vs ${view.width - keys.width}", grid.width <= view.width - keys.width + 2f)
        assertEquals(view.right, grid.right, 2f)
        // The lowest and the highest notes' rows in sight.
        for (pitch in listOf(29, 72)) {
            val key = bounds("roll_key_$pitch")
            assertTrue("$pitch: $key in $view", key.top >= view.top - 1f && key.bottom <= view.bottom + 1f)
        }
        compose.onRoot().captureRoboImage("screenshots/piano_roll_tout_voir.png")
    }

    @Test
    fun aPinchZoomsAndWritesNoNote() {
        var added = 0
        compose.setContent { PocketStudioTheme { PianoRoll(NoteClip(steps = 64), emptyList(), -1, PianoRollActions(add = { added++ })) } }
        idle()
        val before = bounds("piano_roll")
        val keyBefore = bounds("roll_key_60").height
        // Two fingers spread apart, across and up and down.
        compose.onNodeWithTag("roll_view").performTouchInput {
            pinch(Offset(centerX - 40f, centerY - 40f), Offset(centerX - 200f, centerY - 160f), Offset(centerX + 40f, centerY + 40f), Offset(centerX + 200f, centerY + 160f))
        }
        idle()
        assertTrue("wider: ${before.width} → ${bounds("piano_roll").width}", bounds("piano_roll").width > before.width * 1.5f)
        assertTrue("taller rows", bounds("roll_key_60").height > keyBefore * 1.5f)
        assertEquals("a pinch writes no note", 0, added)
        // And back: pinched in, it zooms out.
        val zoomed = bounds("piano_roll").width
        compose.onNodeWithTag("roll_view").performTouchInput {
            pinch(Offset(centerX - 250f, centerY), Offset(centerX - 40f, centerY), Offset(centerX + 250f, centerY), Offset(centerX + 40f, centerY))
        }
        idle()
        assertTrue(bounds("piano_roll").width < zoomed)
        assertEquals(0, added)
    }

    /** É1: a note written (the tutorial's line follows it) never moves the roll, across or up and down. */
    @Test
    fun theRollNeverMovesByItself() {
        val hints = listOf(Note(0, 2 * t, 41), Note(4 * t, 2 * t, 41), Note(40 * t, 2 * t, 53))
        var clip by mutableStateOf(NoteClip(steps = 64))
        compose.setContent { PocketStudioTheme { PianoRoll(clip, emptyList(), -1, PianoRollActions(add = { clip = clip.copy(notes = clip.notes + it) }), hints = hints) } }
        idle()
        val at = bounds("piano_roll")
        val keyAt = bounds("roll_key_41")
        // The notes in sight written: the next one is far away (bar 3), out of sight.
        clip = clip.copy(notes = hints.take(2))
        repeat(30) { compose.mainClock.advanceTimeBy(100); idle() }
        assertEquals(at, bounds("piano_roll"))
        assertEquals(keyAt, bounds("roll_key_41"))
    }

    /** É2: the motif written, « Motif posé ! » and « Copier » right over the grid. */
    @Test
    fun copyIsOfferedOverTheGrid() {
        var copied = 0
        val hints = listOf(Note(0, 2 * t, 41))
        compose.setContent {
            PocketStudioTheme { PianoRoll(NoteClip(steps = 64, notes = hints), emptyList(), -1, PianoRollActions(), hints = hints, trackName = "Basse", copyMotif = { copied++ }) }
        }
        idle()
        assertTrue(has("tuto_roll_copy"))
        compose.onNodeWithTag("tuto_roll_copy").performClick()
        assertEquals(1, copied)
    }

    /** « Accords » on: a touch on a dotted chord's note writes its three notes at once (testers, lot « tuto 5 »). */
    @Test
    fun chordModeWritesTheDottedChord() {
        val chord = listOf(Note(0, 16 * t, 53), Note(0, 16 * t, 56), Note(0, 16 * t, 60))
        var clip by mutableStateOf(NoteClip(steps = 64))
        compose.setContent {
            PocketStudioTheme {
                PianoRoll(clip, emptyList(), -1, PianoRollActions(add = { clip = clip.copy(notes = clip.notes + it) }, replaceClip = { c, _ -> clip = c }),
                    harmony = Harmony(), hints = chord)
            }
        }
        idle()
        compose.onNodeWithTag("chord_mode").performClick()
        idle()
        // The lowest note's row, a little into the bar (a touch is placed from what is seen of the grid).
        fun seen(tag: String) = compose.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        compose.onNodeWithTag("piano_roll").performTouchInput { click(Offset(20f, seen("roll_key_53").center.y - seen("piano_roll").top)) }
        idle()
        assertEquals(chord.map { it.pitch }.toSet(), clip.notes.map { it.pitch }.toSet())
    }
}
