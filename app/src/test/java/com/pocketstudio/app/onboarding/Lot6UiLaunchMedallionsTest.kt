package com.pocketstudio.app.onboarding

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs

/**
 * Lot 6, U10 (seat E a #47, English, phone at 130 %): on « Que veux-tu faire ? », square icons beside round ones, and the
 * « Challenges » text pushed 8 dp further than its neighbours (its stars 56 dp, the others 48). Every card's picture is
 * the same round 48 dp medallion, and every title starts on the same line (DESIGN principle 11).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h780dp-port-hdpi", fontScale = 1.3f)
class Lot6UiLaunchMedallionsTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun french() { I18n.lang = Lang.FR }

    private val cards = listOf("launch_projects" to "Mes projets", "launch_remix" to "Remix", "launch_padlive" to "Pad Live", "launch_challenges" to "Défis")

    private fun show() {
        I18n.lang = Lang.EN
        compose.setContent { PocketStudioTheme { LaunchScreen(LaunchUi(name = "My song", style = "Techno", length = "3:05"), LaunchActions(voice = {}, learn = {})) } }
        compose.waitForIdle()
    }

    private fun titleLeft(title: String): Float =
        compose.onAllNodesWithText(tr(title), useUnmergedTree = true).fetchSemanticsNodes().first().positionInRoot.x

    @Test
    fun everyTitleStartsOnTheSameLine() {
        show()
        val lefts = cards.associate { (tag, title) ->
            compose.onNodeWithTag(tag).performScrollTo()
            compose.waitForIdle()
            title to titleLeft(title)
        }
        val ref = lefts.getValue("Mes projets")
        lefts.forEach { (title, x) -> assertEquals("« $title » starts where « Mes projets » does ($lefts)", ref, x, 1f) }
    }

    /** Outside a round medallion, its square's corner shows the card; a square medallion fills it. */
    @Test
    fun everyMedallionIsRound() {
        show()
        val d = compose.density.density
        val squares = cards.filter { (tag, _) ->
            compose.onNodeWithTag(tag).performScrollTo()
            compose.waitForIdle()
            val img = compose.onNodeWithTag(tag).captureToImage().toPixelMap()
            val left = (16 * d).toInt()
            val top = (img.height / 2f - 24 * d).toInt()
            val corner = img[left + (5 * d).toInt(), top + (5 * d).toInt()]
            val card = img[(6 * d).toInt(), top + (5 * d).toInt()]
            !close(corner, card)
        }.map { it.second }
        assertEquals("medallions not round", emptyList<String>(), squares)
    }

    private fun close(a: Color, b: Color): Boolean =
        abs(a.red - b.red) < 0.06f && abs(a.green - b.green) < 0.06f && abs(a.blue - b.blue) < 0.06f
}
