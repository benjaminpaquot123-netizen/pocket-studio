package com.pocketstudio.app.nav

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.pocketstudio.app.ui.withinGestures
import com.pocketstudio.model.PadSample
import com.pocketstudio.model.Project
import com.pocketstudio.model.ProjectStore
import com.pocketstudio.model.StarterWizard
import com.pocketstudio.model.guide.GuideStatus
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File
import java.time.Duration

/**
 * Le Guide's panel in the whole app (docs/PLAN-GUIDE.md G2): its bar says where the song is, a
 * touch opens the panel on the next card, a card is decided in two gestures, « Plus tard »,
 * « Sauter » and « Ne plus proposer » are kept in the project (saved with it), « Voir le réglage »
 * opens the control, the panel welcomes a return after days, the Mode simple folds the other
 * stages under « Aller plus loin », and ▶ says when it cannot be heard (no engine here).
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h890dp")
class GuidePanelTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val h = AppHarness(compose)

    @After
    fun back() = h.finish()

    private fun jumpUp(): Project {
        val style = StarterWizard.style("jump_up")!!
        return StarterWizard.build(style, style.lines.associate { it.role to null as PadSample? }, style.melody.mood,
            StarterWizard.defaultParts(style).associateWith { StarterWizard.soundsFor(it, style).first() })
    }

    private fun startOn(p: Project = jumpUp(), simple: Boolean = false) {
        h.start(simple = simple) { session.edit { p } }
        await("first analysis") { !h.vm.guide.ui.value.loading && h.vm.guide.ui.value.next != null }
    }

    /** Lets the main thread run (its clock too: the analysis waits for the edits to settle) until [ok]. */
    private fun await(what: String, ok: () -> Boolean) {
        val end = System.currentTimeMillis() + 60_000
        while (!ok()) {
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100))
            compose.waitForIdle()
            check(System.currentTimeMillis() < end) { "$what: not in time" }
            Thread.sleep(5)
        }
        compose.waitForIdle()
    }

    private fun shown(tag: String) = compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()

    private fun tap(tag: String) {
        val node = compose.onNodeWithTag(tag)
        if (!node.isDisplayed()) node.performScrollTo()
        node.performClick()
        compose.waitForIdle()
    }

    private fun status(card: String) = h.vm.project.value.guide.statusOf(card)

    @Test
    fun theBarSaysWhereTheSongIsAndACardIsDecidedInTwoGestures() {
        startOn()
        compose.onNodeWithTag("guide_bar").assertIsDisplayed()
        compose.onNodeWithTag("guide_bar_step", useUnmergedTree = true).assertTextContains("Étape", substring = true)
        compose.onNodeWithTag("guide_panel").assertDoesNotExist()
        // Open, then « Écouter piste par piste »: the card is decided, the next one comes.
        compose.withinGestures("décider une carte du Guide", 2) {
            tap("guide_bar")
            compose.onNodeWithTag("guide_card_fx.review").assertIsDisplayed()
            tap("guide_option_keep")
        }
        await("decided") { status("fx.review") == GuideStatus.DONE }
        await("next card") { shown("guide_card_fx.drums") }
        // The panel never covers the screen: the spaces stay under it.
        compose.onNodeWithTag("tab_Structure").assertIsDisplayed()
        // « Plus tard », « Sauter », « Ne plus proposer »: each kept in the project.
        tap("guide_later")
        await("later") { status("fx.drums") == GuideStatus.LATER }
        await("bass card") { shown("guide_card_fx.bass") }
        tap("guide_skip")
        await("skipped") { status("fx.bass") == GuideStatus.SKIPPED }
        await("sub card") { shown("guide_card_fx.sub") }
        tap("guide_refuse")
        await("refused") { h.vm.project.value.guide.refused("fx.sub") }
        // Saved with the song: the next opening finds them.
        val store = ProjectStore(File(h.app.filesDir, "projects"))
        var saved = store.load(h.vm.projectId)
        await("saved") { saved = store.load(h.vm.projectId); saved?.guide?.refused("fx.sub") == true }
        assertEquals(GuideStatus.LATER, saved!!.guide.statusOf("fx.drums"))
        assertEquals(GuideStatus.SKIPPED, saved!!.guide.statusOf("fx.bass"))
        // Folded with a touch on its bar.
        tap("guide_bar")
        compose.onNodeWithTag("guide_panel").assertDoesNotExist()
    }

    @Test
    fun voirLeReglageOpensTheControl() {
        startOn()
        tap("guide_bar")
        // A card picked in the plan: « Le cadre » › « Le style, le tempo et la tonalité ».
        tap("guide_stage_0")
        tap("guide_item_frame.style")
        compose.onNodeWithTag("guide_card_frame.style").assertIsDisplayed()
        val before = h.vm.project.value
        tap("guide_open_tempo")
        await("tempo sheet") { shown("tempo_sheet") }
        assertEquals(before, h.vm.project.value)
        // The card waits under the sheet.
        compose.onNodeWithTag("guide_card_frame.style").assertExists()
        // « Revenir à l'étape en cours ».
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
        tap("guide_back_to_next")
        compose.onNodeWithTag("guide_card_frame.style").assertDoesNotExist()
        // A screen: Structure's parts, the panel folded, the control lit (« Le cadre » is still open in the plan).
        tap("guide_item_frame.form")
        tap("guide_open_parts")
        await("spot") { shown("guide_spot") }
        compose.onNodeWithTag("guide_panel").assertDoesNotExist()
        compose.onNodeWithTag("session").assertIsDisplayed()
        tap("guide_spot_close")
        assertFalse(shown("guide_spot"))
    }

    @Test
    fun daysLaterThePanelSaysWhereItWas() {
        val p = jumpUp()
        val days = 3 * 24 * 3600 * 1000L
        startOn(p.copy(guide = p.guide.copy(step = "fx.sub", lastUsed = System.currentTimeMillis() - days)))
        tap("guide_bar")
        compose.onNodeWithTag("guide_summary").assertIsDisplayed()
        compose.onNodeWithText("Te revoilà", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Reste", substring = true).assertIsDisplayed()
    }

    @Test
    fun theModeSimpleFoldsTheOtherStagesAndListeningSaysWhenItCannot() {
        startOn(simple = true)
        await("simple") { h.vm.guide.ui.value.simple && h.vm.guide.ui.value.further.isNotEmpty() }
        tap("guide_bar")
        compose.onNodeWithTag("guide_plan").assertExists()
        assertTrue(h.vm.guide.ui.value.plan.all { it.stage in setOf(1, 4, 6, 7) })
        assertFalse(shown("guide_stage_3"))
        tap("guide_further_toggle")
        tap("guide_stage_3")
        // A card of « Aller plus loin »: the Sub's saturation, heard before it is applied.
        tap("guide_item_fx.sub")
        compose.onNodeWithTag("guide_card_fx.sub").assertIsDisplayed()
        val before = h.vm.project.value
        tap("guide_listen_light")
        await("said") { shown("guide_listen_failed") }
        assertEquals(before, h.vm.project.value)
        // Applied from there.
        tap("guide_option_light")
        await("applied") { status("fx.sub") == GuideStatus.DONE }
    }
}
