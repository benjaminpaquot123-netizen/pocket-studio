package com.pocketstudio.app.padlive

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pocketstudio.model.LiveClip
import com.pocketstudio.model.LivePads
import com.pocketstudio.model.PadChallenge
import com.pocketstudio.model.PadLive
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Audit ultra round 1, n° 9 (a GarageBand player): a try worse than the best seemed to wipe it, no
 * list of the challenges, back to challenge 1 each time, the rhythm's dots had no name. The panel
 * lists every challenge with its best stars (one touch plays it), says the record a try did not reach,
 * and its dots are said as one.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h890dp")
class PadLiveChallengeListTest {
    @get:Rule
    val compose = createComposeRule()

    private val pads: LivePads = PadLive.newProject().live.withAt(8, LiveClip("h.wav", "Klaxon", 1, oneShot = true)).withAt(9, LiveClip("l.wav", "Laser", 1, oneShot = true))

    @Test
    fun everyChallengeWithItsBestAndTheRecordSaid() {
        val c = PadChallenge.make(1, PadChallenge.shotsOf(pads, 0), 100.0)!!
        val worse = PadChallenge.Result(right = 1, total = c.hits.size, extra = 0, stars = 0, matched = c.hits.indices.map { it == 0 }, tendency = 0)
        val picked = ArrayList<Int>()
        compose.setContent {
            PocketStudioTheme {
                ChallengePanel(ChallengeUi.Done(c, worse), pads, ChallengeActions(pick = { picked += it }), best = listOf(2, 1))
            }
        }
        compose.onNodeWithTag("padlive_challenge_record").assertIsDisplayed()
        compose.onNodeWithText("Ton record : 2 sur 3").assertIsDisplayed()
        compose.onNodeWithTag("padlive_challenge_levels").assertIsDisplayed()
        compose.onNodeWithContentDescription("Défi 2 · Ping-pong · étoiles : 1 sur 3").assertIsDisplayed()
        compose.onNodeWithTag("padlive_challenge_level_${PadChallenge.LEVELS.size}").assertIsDisplayed()
        compose.onNodeWithTag("padlive_challenge_level_3").performClick()
        assertEquals(listOf(3), picked)
        compose.onNodeWithContentDescription("Rythme · coups : ${c.hits.size} · réussis : 1").assertExists()
    }

    @Test
    fun litUntilJustBeforeTheNextHit() {
        val rate = 48_000
        val starts = listOf(0L, 24_000L, 48_000L)
        assertEquals(-1, ChallengeController.litHit(starts, -1, rate))
        assertEquals(0, ChallengeController.litHit(starts, 12_000, rate))  // 0.25 s: dark before (180 ms)
        assertEquals(-1, ChallengeController.litHit(starts, 22_000, rate))  // the gap before the next
        assertEquals(1, ChallengeController.litHit(starts, 24_000, rate))
        assertEquals(2, ChallengeController.litHit(starts, 48_000 + 8_000, rate))
        assertEquals(-1, ChallengeController.litHit(starts, 48_000 + 9_000, rate))  // past the last one's 180 ms
    }
}
