package com.pocketstudio.app.mixer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.pocketstudio.model.AutoTarget
import com.pocketstudio.model.AutomationLane
import com.pocketstudio.model.Channel
import com.pocketstudio.model.EffectType
import com.pocketstudio.model.Insert
import com.pocketstudio.model.Mixer
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Chantier 8 « Courbes » (retours de Benjamin, 2026-11-08, tel-14; defauts-caches B19, B20): a curve
 * just made is brought into view and lit, its setting stays in the picker (« déjà une courbe ») and
 * leads to it, and « Voir le réglage » opens the effects on the setting itself, framed.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h800dp-xxhdpi")
class CurvesFindTest {
    @get:Rule
    val compose = createComposeRule()

    /** The frame or lane brought into view once laid out (a short wait in the app, started a frame or two later). */
    private fun settle() = repeat(3) { compose.mainClock.advanceTimeBy(500); compose.waitForIdle() }

    private fun exists(tag: String) = compose.onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    private val drums = 0
    private val cutoff = AutoTarget(AutoTarget.INSERT_PARAM, drums, 0, 0)

    /** Four curves already there (on other tracks), the card in a screen's height: a new one lands out of sight. */
    private fun card(channels: () -> List<Channel>, lanes: () -> List<AutomationLane>, actions: AutomationActions) {
        compose.setContent {
            PocketStudioTheme {
                Column(Modifier.height(700.dp).verticalScroll(rememberScrollState())) {
                    AutomationCard(AutomationUi(lanes(), 16), channels(), emptyList(), actions)
                }
            }
        }
    }

    private val others = listOf(
        AutomationLane(AutoTarget(AutoTarget.VOLUME, 1)), AutomationLane(AutoTarget(AutoTarget.PAN, 1)),
        AutomationLane(AutoTarget(AutoTarget.VOLUME, 2)), AutomationLane(AutoTarget(AutoTarget.PAN, 2)),
    )

    @Test
    fun aNewCurveIsBroughtIntoViewAndLit() {
        var channels by mutableStateOf(List(16) { Channel() })
        var lanes by mutableStateOf(others)
        card({ channels }, { lanes }, AutomationActions(
            add = { t -> lanes = lanes + AutomationLane(t) },
            addFilter = { ch ->
                channels = channels.mapIndexed { i, c -> if (i == ch) c.copy(inserts = listOf(Insert.of(EffectType.FILTER)) + c.inserts.drop(1)) else c }
                lanes = lanes + AutomationLane(AutoTarget(AutoTarget.INSERT_PARAM, ch, 0, 0))
            },
        ))
        compose.onNodeWithTag("automation_add").performClick()
        compose.onNodeWithTag("pick_track_$drums").performClick()
        compose.onNodeWithTag("pick_filter_$drums").performClick()
        settle()
        assertEquals(5, lanes.size)
        // Benjamin: « la fenêtre se ferme sans rien dire » and the curve was further down, out of sight.
        compose.onNodeWithTag("auto_fresh_4", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("auto_lane_4").assertIsDisplayed()
    }

    @Test
    fun aSettingWithACurveStaysInThePickerAndLeadsToIt() {
        val filtered = List(16) { Channel() }.mapIndexed { i, c -> if (i == drums) c.copy(inserts = listOf(Insert.of(EffectType.FILTER)) + c.inserts.drop(1)) else c }
        var lanes by mutableStateOf(others + AutomationLane(cutoff))
        var added = 0
        card({ filtered }, { lanes }, AutomationActions(add = { t -> added++; lanes = lanes + AutomationLane(t) }))
        // The filter's curve is the last one, out of sight: the picker is where Benjamin looked for it again.
        compose.onNodeWithTag("automation_add").performClick()
        compose.onNodeWithTag("pick_track_$drums").performClick()
        val tag = "pick_${cutoff.pack()}"
        assertTrue("the filter is still offered, marked", exists(tag) && exists("${tag}_has_curve"))
        compose.onNodeWithTag(tag).performClick()
        settle()
        assertEquals("no second curve on the same setting", 0, added)
        compose.onNodeWithTag("auto_fresh_4", useUnmergedTree = true).assertIsDisplayed()
    }

    private fun box(spot: AutoTarget, channel: Channel) {
        compose.setContent {
            PocketStudioTheme {
                CompositionLocalProvider(LocalControlSpot provides spot) {
                    Column(Modifier.height(600.dp).verticalScroll(rememberScrollState())) {
                        EffectsBox(spot.ch, channel, MixerActions())
                    }
                }
            }
        }
        settle()
    }

    @Test
    fun seeTheSettingOpensTheEffectsOnTheSend() {
        // tel-14: « Envoi réverbe · Batterie », the sheet opened on its top (the ambiences), the send far below.
        box(AutoTarget(AutoTarget.SEND, drums, 0), Channel())
        compose.onNodeWithTag("spot_send_${drums}_0").assertIsDisplayed()
    }

    @Test
    fun seeTheSettingOpensTheEffectOnItsKnob() {
        // The filter's cutoff (second effect): its effect opened, the knob framed and in sight.
        val c = Channel(inserts = listOf(Insert.of(EffectType.CHORUS), Insert.of(EffectType.FILTER)) + List(Mixer.INSERTS - 2) { Insert() })
        box(AutoTarget(AutoTarget.INSERT_PARAM, drums, 1, 0), c)
        compose.onNodeWithTag("spot_fxp_${drums}_1_0").assertIsDisplayed()
    }

    @Test
    fun seeTheSettingOpensTheEffectsOnTheMacro() {
        box(AutoTarget(AutoTarget.MACRO, drums, 0, 2), Channel())
        compose.onNodeWithTag("spot_macro_${drums}_2").assertIsDisplayed()
    }
}
