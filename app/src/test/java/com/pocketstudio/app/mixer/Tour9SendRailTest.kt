package com.pocketstudio.app.mixer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import com.pocketstudio.ui.NeonCard
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Tour 9, lot L2, F028 « Un envoi à 0 % montre quand même sa piste de glisse, et je peux le tirer depuis le début. »
 * The send's slider (the same LabeledSlider as every mix setting) at 0 % on its card.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h640dp-port-hdpi")
class Tour9SendRailTest {
    @get:Rule
    val compose = createComposeRule()

    private val changes = mutableListOf<Float>()

    private fun send() {
        compose.setContent {
            PocketStudioTheme {
                NeonCard {
                    var v by remember { mutableFloatStateOf(0f) }
                    LabeledSlider("Envoi réverbe", "0 %", v, "send") { v = it; changes += it }
                }
            }
        }
        compose.waitForIdle()
    }

    private fun contrast(a: Color, b: Color): Float {
        val (hi, lo) = listOf(a.luminance(), b.luminance()).sortedDescending()
        return (hi + 0.05f) / (lo + 0.05f)
    }

    @Test
    fun f028_aSendAtZeroShowsItsTrack() {
        send()
        val img = compose.onNodeWithTag("send").captureToImage().toPixelMap()
        val px = compose.density.density
        val mid = img.height / 2
        // The card beside the track (14 dp above its middle, inside the slider's touch box).
        val card = img[img.width / 2, (mid - 14 * px).toInt()]
        // Along the empty track, from the start to the end: the best contrast across its thickness.
        for (f in listOf(0.25f, 0.5f, 0.75f)) {
            val x = (img.width * f).toInt()
            val best = ((mid - 6 * px).toInt()..(mid + 6 * px).toInt()).maxOf { contrast(img[x, it], card) }
            assertTrue("the empty track at ${(f * 100).toInt()} % is not seen on the card: contrast $best", best >= 3f)
        }
    }

    @Test
    fun f028_aSendAtZeroIsDraggedFromTheStart() {
        send()
        changes.clear()
        compose.onNodeWithTag("send").performTouchInput {
            swipe(Offset(width * 0.03f, height / 2f), Offset(width * 0.6f, height / 2f), durationMillis = 400)
        }
        compose.waitForIdle()
        assertTrue("the send did not follow the finger: $changes", changes.isNotEmpty() && changes.last() > 0.4f)
    }
}
