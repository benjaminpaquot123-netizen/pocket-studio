package com.pocketstudio.app.ui

import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import com.pocketstudio.model.DefaultNames
import com.pocketstudio.model.Project
import com.pocketstudio.model.instrumentTracks
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tour 7, lot 3, D7 (U27): the Mixer said « Drums, Acid 303, Synth, Remix » and the Sons said « Drums, Acid 303,
 * Vocals, Remix »: the instrument tracks came after the voice and the remix in Sons, before them in the Mixer. Every
 * strip of [mixerStrips] is a chip of the Sons, with the same words, in the same order, left to right.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w1280dp-h800dp-land-hdpi")
class SonsMixerNamesTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun mixerStripsReadLikeTheSonsChips() {
        val p = Project()
        compose.setContent {
            PocketStudioTheme {
                SonsHeader(AppTab.Rythme, com.pocketstudio.app.synth.TracksUi(p.instrumentTracks, selected = 0), go = {}, audioTracks = 0)
            }
        }
        compose.waitForIdle()
        var lastX = -1f
        var lastName = "(start)"
        for (strip in mixerStrips(p)) {
            val name = DefaultNames.shown(strip.name)
            val nodes = compose.onAllNodesWithText(name, useUnmergedTree = true).fetchSemanticsNodes()
            assertTrue("« $name » of the Mixer is not a chip of the Sons", nodes.isNotEmpty())
            val x = compose.onAllNodesWithText(name, useUnmergedTree = true)[0].getBoundsInRoot().left.value
            assertTrue("« $name » (x=$x) comes before « $lastName » (x=$lastX) in the Sons, after it in the Mixer", x > lastX)
            lastX = x
            lastName = name
        }
    }
}
