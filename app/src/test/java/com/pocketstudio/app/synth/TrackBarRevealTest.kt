package com.pocketstudio.app.synth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import com.pocketstudio.model.InstrumentTrack
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * APK 228 leftover (audit stage 1): in Sons, a track opened from elsewhere (its sheet, « + Piste »,
 * after Remix) was lit past the screen's edge; its chip now scrolls into the row's view.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp-xxhdpi")
class TrackBarRevealTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun theTrackOpenedElsewhereIsScrolledIntoView() {
        val tracks = List(8) { InstrumentTrack(it, "Piste numéro $it") }
        var selected by mutableIntStateOf(0)
        compose.setContent { PocketStudioTheme { TrackBar(TracksUi(tracks, selected)) } }
        compose.waitForIdle()
        val width = compose.onRoot().fetchSemanticsNode().size.width
        fun shown(tag: String): Boolean {
            val n = compose.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode()
            // Clipped by the row's scroll: the part on screen is the whole chip only when it is in view.
            val b = n.boundsInRoot
            return b.left >= 0f && b.right <= width + 0.5f && b.width >= n.size.width - 1f
        }
        assertTrue(shown("track_0"))
        selected = 7
        compose.waitForIdle()
        assertTrue("the last track's chip is on screen", shown("track_7"))
        selected = 1
        compose.waitForIdle()
        assertTrue("back to the second one", shown("track_1"))
    }
}
