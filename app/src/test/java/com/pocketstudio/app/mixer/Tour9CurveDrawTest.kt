package com.pocketstudio.app.mixer

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import com.pocketstudio.model.AutoTarget
import com.pocketstudio.model.AutomationLane
import com.pocketstudio.model.Channel
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Tour 9, lot L2, F127 (grave; seats t9-59 gesture 64, t9-70, t9-45 r2): in the Mix's curves, a stroke started at the
 * canvas's bottom edge was taken by the page's scroll. « Dessiner une courbe trace la courbe, la page ne défile pas
 * pendant le trait. » A plain Compose finger, no bench.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h640dp-port-hdpi")
class Tour9CurveDrawTest {
    @get:Rule
    val compose = createComposeRule()

    private val volume = AutoTarget(AutoTarget.VOLUME, 1)
    private val points = mutableListOf<Pair<Int, Float?>>()
    private lateinit var scroll: ScrollState

    private fun card() {
        compose.setContent {
            scroll = rememberScrollState()
            PocketStudioTheme {
                Column(Modifier.height(640.dp).verticalScroll(scroll)) {
                    Spacer(Modifier.height(200.dp))
                    AutomationCard(
                        AutomationUi(listOf(AutomationLane(volume)), 16), List(16) { Channel() }, emptyList(),
                        AutomationActions(setPoint = { _, u, v -> points += u to v }),
                    )
                    Spacer(Modifier.height(900.dp))
                }
            }
        }
        compose.waitForIdle()
    }

    /** A stroke from [from] to [to] (fractions of the canvas, y below 1 past its edge), slow as a finger. */
    private fun stroke(from: Offset, to: Offset) {
        card()
        val curve = compose.onNodeWithTag("lane_${volume.pack()}").performScrollTo()
        // The curve in the middle of the screen, the row under it in sight too (as a finger finds it).
        compose.runOnIdle { kotlinx.coroutines.runBlocking { scroll.scrollBy(compose.density.run { 160.dp.toPx() }) } }
        compose.waitForIdle()
        val before = scroll.value
        points.clear()
        curve.performTouchInput {
            swipe(Offset(width * from.x, height * from.y), Offset(width * to.x, height * to.y), durationMillis = 500)
        }
        compose.waitForIdle()
        assertEquals("the page scrolled during the stroke (points drawn: $points)", before, scroll.value)
        assertTrue("the stroke drew nothing", points.size >= 2)
    }

    @Test
    fun f127_aStrokeFromTheBottomEdgeUpDrawsAndTheMixStaysPut() = stroke(Offset(0.1f, 0.98f), Offset(0.9f, 0.2f))

    /** The finger lands on the canvas's bottom line, a hair under it (seat t9-59: the stroke went to the page). */
    @Test
    fun f127_aStrokeLandingJustUnderTheBottomEdgeDrawsAndTheMixStaysPut() = stroke(Offset(0.2f, 1.06f), Offset(0.7f, 0.3f))

    @Test
    fun f127_aStrokeAlongTheBottomEdgeDrawsAndTheMixStaysPut() = stroke(Offset(0.1f, 1.04f), Offset(0.9f, 0.85f))

    @Test
    fun f127_aStrokeGoingDownPastTheBottomEdgeDrawsAndTheMixStaysPut() = stroke(Offset(0.1f, 0.6f), Offset(0.5f, 1.6f))

    @Test
    fun f127_anUpwardStrokeAlongOneStepDrawsAndTheMixStaysPut() = stroke(Offset(0.3f, 0.97f), Offset(0.32f, 0.05f))
}
