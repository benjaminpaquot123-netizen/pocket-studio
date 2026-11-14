package com.pocketstudio.app.padlive

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.pocketstudio.model.LiveClip
import com.pocketstudio.model.LivePads
import com.pocketstudio.model.LiveStarter
import com.pocketstudio.model.PadLive
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Plan of the audits, step 4, n° 7 (AUDIT-PRISE-EN-MAIN G14; T5-Ed-40, recette tel-11: Benjamin's tablet lying down, a
 * glide that began on an effect lit « Montée · 4 mesures »). Pad Live lying down: a finger that glides up or down from
 * an effect of the scrolling column scrolls it; « Montée » never starts, and a held effect (Chute, Bégaie, Écho) lets
 * go as soon as the finger glides. A touch, or a finger held still, plays them as before.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w800dp-h360dp-land-hdpi")
class PadLiveGlideTest {
    @get:Rule
    val compose = createComposeRule()

    private val done = ArrayList<String>()

    private val pads: LivePads = PadLive.placed(LivePads(), 0, LiveStarter.PADS.mapIndexed { i, p ->
        LiveClip("s$i.wav", p.name, if (p.oneShot) 1 else p.beats, p.oneShot, p.gain)
    })

    private fun show() {
        compose.setContent {
            PocketStudioTheme {
                PadLiveScreen(
                    PadLiveUi(pads, List(LivePads.ALL) { 0 }, page = 0, pack = "fete", playing = true, tempoBpm = 124.0, beatsPerBar = 4, riserBars = 4),
                    PadLiveActions(
                        riser = { done += "riser" },
                        brake = { done += "brake $it" },
                        stutter = { done += "stutter $it" },
                        echo = { done += "echo $it" },
                    ),
                    clockOf = { 0 }, still = true,
                )
            }
        }
        compose.onNodeWithTag("padlive_more").performClick()
        compose.waitForIdle()
    }

    private fun top(tag: String) = compose.onNodeWithTag(tag).fetchSemanticsNode().positionInRoot.y

    /** A finger down on [tag]'s middle, then 60 dp up in small steps (as a finger scrolls); [lift]: then up. */
    private fun glide(tag: String, lift: Boolean = true) = compose.onNodeWithTag(tag).performTouchInput {
        down(center)
        val step = 6.dp.toPx()
        repeat(10) { moveBy(Offset(0f, -step)) }
        if (lift) up()
    }

    @Test
    fun aGlideFromMonteeScrollsTheColumnAndStartsNothing() {
        show()
        val before = top("padlive_fx_riser")
        glide("padlive_fx_riser")
        compose.waitForIdle()
        assertTrue("a glide from « Montée » never starts it: $done", "riser" !in done)
        assertTrue("the column scrolled under the finger (${top("padlive_fx_riser")} vs $before)", top("padlive_fx_riser") < before - 10f)
    }

    @Test
    fun aHeldEffectLetsGoAsSoonAsTheFingerGlides() {
        show()
        for (fx in listOf("brake", "stutter", "echo")) {
            done.clear()
            glide("padlive_fx_$fx", lift = false)
            compose.waitForIdle()
            assertTrue("$fx: a glide leaves it off while the finger is still down: $done", done.isEmpty() || done.last() == "$fx false")
            compose.onNodeWithTag("padlive_side").performTouchInput { up() }
            compose.waitForIdle()
        }
    }

    @Test
    fun aTouchOnMonteeStartsItAndAStillFingerHoldsAnEffect() {
        show()
        compose.onNodeWithTag("padlive_fx_riser").performTouchInput { down(center); up() }
        compose.waitForIdle()
        assertEquals(listOf("riser"), done)
        done.clear()
        compose.onNodeWithTag("padlive_fx_brake").performTouchInput { down(center) }
        compose.waitForIdle()
        assertEquals("held still: on", listOf("brake true"), done)
        compose.onNodeWithTag("padlive_fx_brake").performTouchInput { up() }
        compose.waitForIdle()
        assertEquals(listOf("brake true", "brake false"), done)
    }
}
