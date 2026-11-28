package com.pocketstudio.app.rhythm

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.DpRect
import com.pocketstudio.model.Project
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Tour 9, lot L4, F047: a pad's badges (« FX », « G 100 ») never cover its name, on a 360 dp phone at 100, 130 and
 * 200 %: the name's first line starts under the badges' row.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Tour9L4BadgesTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun tidy() = RuntimeEnvironment.setFontScale(1f)

    private fun DpRect.rect() = Rect(left.value, top.value, right.value, bottom.value)

    private fun check(scale: Float) {
        if (scale != 1f) RuntimeEnvironment.setFontScale(scale)
        val start = Project()
        val project = start.copy(drums = start.drums.copy(lanes = start.drums.lanes.map { it.copy(pan = -1f) }))
        compose.setContent {
            PocketStudioTheme {
                RhythmScreen(project = project, transport = TransportUi(selectedPad = 0), live = { LiveUi() }, canUndo = false, canRedo = false, actions = RhythmActions())
            }
        }
        compose.waitForIdle()
        val badges = compose.onAllNodesWithTag("pad_pan_badge", useUnmergedTree = true).fetchSemanticsNodes().indices
            .map { compose.onAllNodesWithTag("pad_pan_badge", useUnmergedTree = true)[it].getBoundsInRoot().rect() }
        val covered = (0 until 16).mapNotNull { lane ->
            val pad = compose.onNodeWithTag("pad_$lane").getBoundsInRoot().rect()
            val badge = badges.firstOrNull { pad.contains(it.center) } ?: return@mapNotNull "pad $lane: no badge"
            val name = padName(project, lane)
            val nodes = compose.onAllNodesWithText(name, useUnmergedTree = true)
            val text = nodes.fetchSemanticsNodes().indices.map { nodes[it].getBoundsInRoot().rect() }.firstOrNull { pad.contains(it.center) }
                ?: return@mapNotNull "pad $lane: no name"
            if (text.top < badge.bottom - 0.5f) "pad $lane « $name »: pad $pad name $text badge $badge" else null
        }
        assertTrue("at ${(scale * 100).toInt()} %: $covered", covered.isEmpty())
    }

    @Test fun at100() = check(1f)

    @Test fun at130() = check(1.3f)

    @Test fun at200() = check(2f)
}
