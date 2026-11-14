package com.pocketstudio.app.nav

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.pocketstudio.app.human.KnownDefect
import com.pocketstudio.app.human.KnownDefectRule
import com.pocketstudio.app.mixer.targetShortName
import com.pocketstudio.app.ui.mixerStrips
import com.pocketstudio.model.AutoTarget
import com.pocketstudio.model.EffectType
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * PLAN-RESTE § 5, criterion 9 (defauts-caches.tsv): two defects Benjamin saw on his tablet on 2026-11-08 (recette tel-14)
 * that no seat of the bench went near (« hors mission »), played here on the whole app (AppRoot, its Mix space, its
 * sheets), the way he went, with no seat and no fake engine: the app's own view model and screens.
 * - B19: « Automatiser un réglage » › Batterie › « Filtre (balayage) »: the sheet closes, and opened again the option is
 *   gone (he thought nothing had happened; the curve was made, out of view);
 * - B20: « Voir le réglage » on a curve of « Envoi réverbe · Batterie » opens « Effets · Batterie » at its top, the send
 *   it moves far below, out of view.
 * Each is the app's, held by its @KnownDefect until the app is fixed (then the rule says « no longer fails »).
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp")
class HiddenDefectsGuardTest {
    private val compose = createAndroidComposeRule<ComponentActivity>()

    /** The known defect around the app's rule: a failure the guard expects is read once the app is torn down. */
    @get:Rule
    val rules: TestRule = RuleChain.outerRule(KnownDefectRule()).around(compose)

    private val h = AppHarness(compose)

    @After
    fun end() = h.finish()

    private fun exists(tag: String) = compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    private fun tap(tag: String) {
        h.awaitTag(tag)
        val node = compose.onNodeWithTag(tag)
        if (!node.isDisplayed()) node.performScrollTo()
        node.performClick()
        compose.waitForIdle()
    }

    /** The picker of « Automatiser un réglage » open on the drums (Batterie, channel 0), every setting of it shown. */
    private fun openPickerOnTheDrums() {
        tap("tab_Mix")
        tap("automation_add")
        h.awaitTag("automation_picker")
        tap("pick_track_0")
        if (exists("pick_group_0_more")) tap("pick_group_0_more")
    }

    /**
     * B19 (retours-benjamin.md, tel-14): « Automatiser un réglage » › Batterie › « Filtre (balayage) ». The app puts a
     * filter on the drums and makes its curve; the sheet closes. Opened again on Batterie, the filter's sweep is nowhere:
     * neither « Filtre (balayage) » nor the setting its curve moves (« Filtre (déjà une courbe) » is what Benjamin asked).
     */
    @Test
    fun theFilterSweepJustAddedIsStillNamedInThePicker() {
        h.start()
        openPickerOnTheDrums()
        assertTrue("« Filtre (balayage) » offered on Batterie before", exists("pick_filter_0"))
        tap("pick_filter_0")
        val p = h.vm.project.value
        val lane = p.automation.singleOrNull { it.target.kind == AutoTarget.INSERT_PARAM && it.target.ch == 0 }
        assertTrue("the filter's curve was made: ${p.automation.map { it.target }}", lane != null &&
            p.mixer.channels[0].inserts.getOrNull(lane.target.slot)?.effect == EffectType.FILTER)
        assertTrue("the picker closed after the choice", !exists("automation_picker"))
        openPickerOnTheDrums()
        val sweep = targetShortName(lane!!.target, p.mixer.channels, p.mixer.master, mixerStrips(p))
        fun shown(m: androidx.compose.ui.test.SemanticsMatcher) =
            compose.onAllNodes(m and androidx.compose.ui.test.hasAnyAncestor(hasTestTag("automation_picker")), useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        val named = exists("pick_filter_0") || shown(hasText(sweep, substring = true)) || shown(hasText("déjà", substring = true))
        assertTrue("the filter's sweep just added is gone from « Automatiser un réglage » (looked for « $sweep »)", named)
    }

    /**
     * B20 (retours-benjamin.md, tel-14 ❌ « figé », video read frame by frame): a curve of « Envoi réverbe · Batterie »,
     * « Voir le réglage »: « Effets · Batterie » opens at its top (its effects, its pad XY), the send far below, out of
     * view; Benjamin had to scroll down to « Réverbe et écho communs » to find it.
     */
    @Test
    @KnownDefect("CACHE-B20", "« Voir le réglage » leaves the reverb send out of view")
    fun showTheSettingBringsTheReverbSendIntoView() {
        h.start { songWithTracks(); addAutomation(AutoTarget(AutoTarget.SEND, 0, 0)) }
        val i = h.vm.project.value.automation.indexOfFirst { it.target == AutoTarget(AutoTarget.SEND, 0, 0) }
        assertTrue("the curve « Envoi réverbe · Batterie » is there", i >= 0)
        tap("tab_Mix")
        tap("auto_show_$i")
        h.awaitTag("send_0_0")
        assertTrue("the drums' effects opened", exists("effects_box_0"))
        assertTrue("« Voir le réglage » leaves the reverb send out of view", compose.onNodeWithTag("send_0_0").isDisplayed())
    }
}
