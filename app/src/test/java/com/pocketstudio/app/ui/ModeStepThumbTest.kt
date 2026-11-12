package com.pocketstudio.app.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import com.pocketstudio.app.human.SeatOptions
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tour 5, F d #2 (t5-04, a left-handed seat on one hand): « Simple ou Pro ? » had its two cards at the top of the
 * screen, out of the thumb's reach; the seat could only go back. Both cards' centres are within the reach of either
 * thumb (the bench's own rule, [SeatOptions.reachable]), at 100 % and 130 % text, in the dialog's wording and the tutorial's.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h914dp-port-hdpi")
class ModeStepThumbTest {
    @get:Rule
    val compose = createComposeRule()

    private fun cardsWithinReach(fontScale: Float, tuto: Boolean) {
        compose.setContent {
            val d = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(d.density, fontScale)) {
                PocketStudioTheme { ModeStep(choose = {}, back = {}, tuto = tuto) }
            }
        }
        compose.waitForIdle()
        val root = compose.onRoot().getBoundsInRoot()
        val density = compose.density.density
        val w = ((root.right - root.left).value * density).toInt()
        val h = ((root.bottom - root.top).value * density).toInt()
        for (card in listOf("mode_card_simple", "mode_card_pro")) {
            val b = compose.onNodeWithTag(card).getBoundsInRoot()
            val x = (b.left.value + b.right.value) / 2 * density
            val y = (b.top.value + b.bottom.value) / 2 * density
            for (hand in listOf(SeatOptions.Hand.LEFT, SeatOptions.Hand.RIGHT)) {
                assertTrue(
                    "$card (${fontScale * 100} %, tuto=$tuto) at ${x.toInt()},${y.toInt()} on ${w}x$h: out of the ${hand.word} thumb",
                    SeatOptions.reachable(hand, x, y, w, h, density),
                )
            }
        }
    }

    @Test
    fun bothCardsWithinEitherThumb() = cardsWithinReach(1f, tuto = false)

    @Test
    fun bothCardsWithinEitherThumbAtBigText() = cardsWithinReach(1.3f, tuto = false)

    @Test
    fun bothTutorialCardsWithinEitherThumbAtBigText() = cardsWithinReach(1.3f, tuto = true)
}
