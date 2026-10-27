package com.pocketstudio.app.nav

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.pocketstudio.app.settings.SettingsStore
import com.pocketstudio.app.ui.withinGestures
import com.pocketstudio.model.SongPart
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Phase 36 in the whole app: every space opens what it holds, nothing the seven tabs had is
 * lost, the Mode simple shows the same project in four tabs and everyday words (switching never
 * changes it), « Mon premier morceau » is in the Guide (G5), and « ? »
 * labels the screen.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp")
class SpacesTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val h = AppHarness(compose)

    @org.junit.After
    fun everydayWordsOff() = h.finish()

    private fun tap(tag: String) {
        compose.onNodeWithTag(tag).performClick()
        compose.waitForIdle()
    }

    @Test
    fun eachSpaceOpensWhatItHolds() {
        h.start()
        // Structure: the timeline, the parts a switch away (with what the Morceau tab had).
        compose.onNodeWithTag("timeline_screen").assertIsDisplayed()
        tap("structure_parts")
        compose.onNodeWithTag("session").assertIsDisplayed()
        compose.onNodeWithTag("part_play_0").assertIsDisplayed()
        compose.onNodeWithTag("open_live").assertDoesNotExist()
        tap("structure_timeline")
        compose.onNodeWithTag("timeline_screen").assertIsDisplayed()
        // Sons: the drums at first, a chip per track; the editors behind each.
        tap("tab_Sons")
        compose.onNodeWithTag("rhythm").assertIsDisplayed()
        tap("sons_Acide")
        compose.onNodeWithTag("acid").assertIsDisplayed()
        tap("track_${h.trackId("Lead")}")
        compose.onNodeWithTag("synth").assertIsDisplayed()
        assertEquals(h.trackId("Lead"), h.vm.selectedTrack.value)
        tap("sons_Audio")
        compose.onNodeWithTag("audio_page_0").assertIsDisplayed()
        // Mix: the console, a sheet per strip.
        tap("tab_Mix")
        compose.onNodeWithTag("console").assertIsDisplayed()
        tap("strip_more_0")
        compose.onNodeWithTag("strip_sheet").assertExists()
        compose.onNodeWithTag("strip_moves_0").assertExists()
        tap("strip_sheet_close")
        // Live, then back where it was opened from.
        tap("tab_Live")
        compose.onNodeWithTag("live_screen").assertIsDisplayed()
        tap("live_back")
        compose.onNodeWithTag("console").assertIsDisplayed()
        // Plus: the list, a page, back.
        tap("tab_Plus")
        compose.onNodeWithTag("plus").assertIsDisplayed()
        tap("tool_Reglages")
        compose.onNodeWithTag("settings").assertIsDisplayed()
        tap("plus_back")
        listOf("plus_projects", "tool_Aide", "plus_missions", "plus_glossary", "plus_mode", "tool_Midi", "tool_Latence", "tool_Fond", "tool_Son", "tool_Recette")
            .forEach { compose.onNodeWithTag(it).assertExists() }
    }

    @Test
    fun sonsOpensAgainOnTheLastEditor() {
        h.start()
        tap("tab_Sons")
        tap("sons_Acide")
        tap("tab_Mix")
        tap("tab_Sons")
        compose.onNodeWithTag("acid").assertIsDisplayed()
        compose.onNodeWithTag("sons_Acide").assertIsSelected()
        // Kept on the phone too: the next start opens there.
        assertEquals(com.pocketstudio.app.ui.AppTab.Acide, com.pocketstudio.app.ui.NavPrefs(h.app).lastSons)
    }

    @Test
    fun theTimelinesTrackNamesOpenTheirSounds() {
        h.start()
        tap("timeline_name_${SongPart.ACID}")
        compose.onNodeWithTag("acid_sound_sheet").assertExists()
        tap("acid_sound_close")
        tap("timeline_name_${h.inst("Lead")}")
        compose.onNodeWithTag("sound_chooser").assertExists()
        compose.onNodeWithTag("sound_chooser_title").assertTextContains("Lead", substring = true)
    }

    @Test
    fun theSimpleModeHasFiveTabsInEverydayWords() {
        h.start(simple = true)
        listOf("Morceau", "Rythme", "Clavier", "Mix", "Envoyer").forEach { compose.onNodeWithTag("tab_simple_$it").assertIsDisplayed() }
        compose.onNodeWithTag("tab_Structure").assertDoesNotExist()
        // Mon morceau: the parts and « Mes pistes », the « Lead » named « Mélodie ».
        compose.onNodeWithTag("simple_tracks").assertExists()
        compose.onNodeWithTag("simple_track_${h.inst("Lead")}").assertTextContains("Mélodie", substring = true)
        // No ⤴ twice: « Envoyer » is a tab, the rest is in « ⋯ ».
        compose.onNodeWithTag("top_share").assertDoesNotExist()
        compose.onNodeWithTag("top_more").assertIsDisplayed()
        for (t in listOf("Morceau", "Rythme", "Clavier", "Mix", "Envoyer")) {
            tap("tab_simple_$t")
            for (jargon in listOf("Lead", "303", "Quantifier")) {
                val found = compose.onAllNodes(hasText(jargon, substring = true), useUnmergedTree = true).fetchSemanticsNodes()
                assertTrue("« $jargon » on $t: ${found.map { it.config }}", found.isEmpty())
            }
        }
        // Envoyer is the « Fini ? » page itself.
        compose.onNodeWithTag("share_sheet").assertIsDisplayed()
        compose.onNodeWithTag("share_go").assertExists()
        compose.onNodeWithTag("share_close").assertDoesNotExist()
    }

    @Test
    fun theSimpleModesKeyActionsAreShort() {
        h.start(simple = true)
        val lead = h.trackId("Lead")
        val lib = com.pocketstudio.model.SoundLibrary.SYNTH
        val before = h.vm.project.value.tracks.first { it.id == lead }.synth
        val current = com.pocketstudio.model.SoundLibrary.synthNameOf(before, lib)
        val family = lib.firstOrNull { it.name == current }?.category ?: com.pocketstudio.model.SoundCategories.SYNTH.first()
        val pick = lib.indexOfFirst { it.category == family && it.name != current }
        compose.withinGestures("Changer le son de la mélodie (Mode simple)", 2) {
            tap("simple_track_${h.inst("Lead")}")
            tap("synth_sounds_$pick")
        }
        compose.withinGestures("Enregistrer ma mélodie (Mode simple)", 2) {
            tap("tab_simple_Clavier")
            tap("record_start")
        }
        assertTrue(h.vm.recording.value)
        h.vm.recordTake(com.pocketstudio.app.record.TakeSource.KEYS)
        compose.waitForIdle()
        compose.withinGestures("Partager (Mode simple)", 2) {
            tap("tab_simple_Envoyer")
            tap("share_go")
        }
        assertTrue(h.vm.export.value.sharing)
    }

    @Test
    fun switchingModesNeverChangesTheProject() {
        h.start()
        val before = h.vm.project.value
        tap("tab_Plus")
        tap("plus_mode")
        assertTrue(SettingsStore.state.value.simpleMode)
        compose.onNodeWithTag("tab_simple_Morceau").assertIsDisplayed()
        assertEquals(before, h.vm.project.value)
        // Back with « ⋯ › Mode complet »: the same project, nothing to undo.
        tap("top_more")
        tap("more_full")
        assertFalse(SettingsStore.state.value.simpleMode)
        compose.onNodeWithTag("tab_Structure").assertIsDisplayed()
        assertEquals(before, h.vm.project.value)
        assertFalse(h.vm.session.canUndo.value && h.vm.project.value != before)
        // And from Réglages.
        tap("tab_Plus")
        tap("tool_Reglages")
        tap("mode_simple")
        assertTrue(SettingsStore.state.value.simpleMode)
        assertEquals(before, h.vm.project.value)
    }

    @Test
    fun theFirstLaunchAsksHowToWorkSimpleFirst() {
        h.start(firstLaunch = true)
        compose.onNodeWithTag("welcome").assertExists()
        compose.onNodeWithTag("welcome_mode_simple").assertIsSelected()
        assertTrue(SettingsStore.state.value.simpleMode)
        tap("welcome_mode_full")
        compose.onNodeWithTag("welcome_mode_full").assertIsSelected()
        assertFalse(SettingsStore.state.value.simpleMode)
        compose.onNodeWithTag("welcome_later").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("tab_Sons").assertIsDisplayed()
        assertTrue(SettingsStore.state.value.guideSeen)
    }

    @Test
    fun theQuestionMarkLabelsTheScreenOneTouchOnOneTouchOff() {
        h.start()
        compose.onNodeWithTag("help_labels").assertDoesNotExist()
        tap("help_labels_button")
        compose.onNodeWithTag("help_labels").assertExists()
        assertTrue(compose.onAllNodes(hasText("Ordre des parties"), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty())
        assertTrue(compose.onAllNodes(hasText("Jouer d'une partie"), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty())
        tap("help_labels_button")
        compose.onNodeWithTag("help_labels").assertDoesNotExist()
        // « Masquer les étiquettes » too.
        tap("help_labels_button")
        tap("help_labels_hide")
        assertEquals(0, compose.onAllNodesWithTag("help_labels").fetchSemanticsNodes().size)
    }
}

/** Phase 36: the space shown and the Structure view come back after the process is killed (saved instance state). */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp")
class SpacesRestoreTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @org.junit.After
    fun back() {
        SettingsStore.forgetForTests()
        com.pocketstudio.model.i18n.I18n.everyday = false
    }

    @Test
    fun theSpaceAndTheViewSurviveARestore() {
        val h = AppHarness(compose)
        val restore = androidx.compose.ui.test.junit4.StateRestorationTester(compose)
        h.start(restore = restore)
        compose.onNodeWithTag("structure_parts").performClick()
        compose.onNodeWithTag("tab_Mix").performClick()
        compose.waitForIdle()
        restore.emulateSavedInstanceStateRestore()
        compose.waitForIdle()
        compose.onNodeWithTag("console").assertIsDisplayed()
        compose.onNodeWithTag("tab_Structure").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("session").assertIsDisplayed()
    }
}
