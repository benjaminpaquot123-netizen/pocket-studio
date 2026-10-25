package com.pocketstudio.app.nav

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.model.InstrumentTrack
import com.pocketstudio.model.LayerRole
import com.pocketstudio.model.PadSample
import com.pocketstudio.model.Project
import com.pocketstudio.model.SongPart
import com.pocketstudio.model.SoundLibrary
import com.pocketstudio.model.StarterWizard
import com.pocketstudio.model.guide.GuideChange
import com.pocketstudio.model.guide.GuideTrack
import com.pocketstudio.model.instrumentTracks
import com.pocketstudio.model.layersOf
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
import java.time.Duration

/**
 * G3 in the whole app: a track's sheet from its name in the track bar and from the Guide's card,
 * its layers editor (a layer added with its sound, set, heard alone, removed: each in the project,
 * in the engine and in the track's history), « Ramener » from the history, a chain copied, a part
 * switched from « Quand ». No engine here: what it is sent is checked in LayersVmTest.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h890dp")
class TrackSheetTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val h = AppHarness(compose)


    @After
    fun back() {
        h.finish()
    }

    private fun jumpUp(): Project {
        val style = StarterWizard.style("jump_up")!!
        return StarterWizard.build(style, style.lines.associate { it.role to null as PadSample? }, style.melody.mood,
            StarterWizard.defaultParts(style).associateWith { StarterWizard.soundsFor(it, style).first() })
    }

    private fun startOn(p: Project = jumpUp()) {
        h.start { session.edit { p } }
        await("first analysis") { !h.vm.guide.ui.value.loading && h.vm.guide.ui.value.next != null }
    }

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

    private fun bass() = h.vm.project.value.instrumentTracks.first { it.name == "Basse" }.id

    /** Sons › the bass held › « Fiche de la piste ». */
    private fun openBassSheet() {
        tap("tab_Sons")
        val bass = bass()
        compose.onNodeWithTag("track_$bass").performScrollTo().performTouchInput { longClick() }
        compose.waitForIdle()
        tap("track_menu_sheet")
        await("sheet") { shown("track_sheet_title") }
        compose.onNodeWithTag("track_sheet_title").assertTextContains("Basse", substring = true)
    }

    @Test
    fun aLayerAddedSetHeardAloneAndRemovedReachesTheProjectTheEngineAndTheHistory() {
        startOn()
        openBassSheet()
        val bass = bass()
        // The five columns and the history are there.
        for (tag in listOf("track_sheet_sound", "track_sheet_when", "track_sheet_chain", "track_sheet_often", "track_sheet_elsewhere", "track_sheet_history")) {
            compose.onNodeWithTag(tag).performScrollTo().assertIsDisplayed()
        }
        // « Couches » › « Air » › the first synth sound: the layer is in the song and in the engine.
        tap("track_sheet_layers")
        await("editor") { shown("layers_editor") }
        tap("layers_add_air")
        tap("layer_synth_sounds_0")
        await("added") { h.vm.project.value.layersOf(bass).size == 1 }
        val layer = h.vm.project.value.layersOf(bass).single()
        assertEquals(LayerRole.AIR, layer.role)
        assertEquals(SoundLibrary.SYNTH[0].name, layer.label)
        assertEquals(12, layer.transpose)
        compose.onNodeWithTag("layer_0").assertIsDisplayed()
        // An octave down: the layer plays its notes there, in the engine too.
        tap("layer_octave_down_0")
        assertEquals(0, h.vm.project.value.layersOf(bass).single().transpose)
        // Heard alone, live (saved nowhere: the song does not change), every sound again when the editor closes.
        val before = h.vm.project.value
        tap("layer_alone_0")
        assertEquals(com.pocketstudio.model.Layers.only(0), h.vm.listeningForTest(bass))
        assertEquals(before, h.vm.project.value)
        // Each gesture is a line of the bass's history, by hand.
        val history = h.vm.project.value.guide.historyOf(GuideTrack.inst(bass))
        assertEquals(2, history.map { it.id }.distinct().size)
        assertTrue(history.all { it.origin == GuideChange.USER && it.field == "layers" })
        // Removed: gone from the song and switched off in the engine.
        tap("layer_remove_0")
        assertTrue(h.vm.project.value.layersOf(bass).isEmpty())
        assertFalse(shown("layer_0"))
        tap("layers_close")
        assertEquals(NativeEngine.LISTEN_ALL, h.vm.listeningForTest(bass))
        // Back on the sheet: the removal is the newest line; « Ramener » brings the layer back, in the engine too.
        await("the sheet's history") { h.vm.guide.ui.value.sheets.first { it.track.key == GuideTrack.inst(bass) }.lines.size == 3 }
        val removal = h.vm.guide.ui.value.sheets.first { it.track.key == GuideTrack.inst(bass) }.lines.first()
        assertTrue(removal.text, removal.text.startsWith("Couche retirée"))
        tap("track_sheet_revert_${removal.id}")
        await("brought back") { h.vm.project.value.layersOf(bass).size == 1 }
        assertEquals(0, h.vm.project.value.layersOf(bass).single().transpose)
        await("marked") { h.vm.guide.ui.value.sheets.first { it.track.key == GuideTrack.inst(bass) }.lines.first().reverted }
        compose.onNodeWithTag("track_sheet_layer_names").performScrollTo().assertTextContains(SoundLibrary.SYNTH[0].name, substring = true)
    }

    @Test
    fun theGuidesLayersCardOpensTheLayersEditorAndItsCardOpensTheSheet() {
        startOn()
        tap("guide_bar")
        tap("guide_stage_2")
        tap("guide_item_layers.bass")
        compose.onNodeWithTag("guide_card_layers.bass").assertIsDisplayed()
        // « Voir les couches »: the layers editor (no more the sound chooser).
        tap("guide_open_layers")
        await("layers editor") { shown("layers_editor") }
        assertFalse(shown("sound_chooser"))
        compose.onNodeWithTag("layers_title").assertTextContains("Basse", substring = true)
        tap("layers_close")
        // « La fiche de la piste « Basse » » under « Voir le réglage ».
        tap("guide_sheet")
        await("sheet") { shown("track_sheet_title") }
        compose.onNodeWithTag("track_sheet_title").assertTextContains("Basse", substring = true)
    }

    @Test
    fun aChainIsCopiedAndAPartSwitchedFromTheSheet() {
        startOn()
        openBassSheet()
        val p0 = h.vm.project.value
        val bass = bass()
        val lead = p0.instrumentTracks.first { it.name == "Lead" }.id
        // « Quand »: a touch on a part's box silences the bass there (another brings it back).
        val key = SongPart.inst(bass)
        val part = h.vm.project.value.song.indexOfFirst { it.plays(key) }
        // (Scrolled to: a box half out of its line counts as shown, its middle is not.)
        compose.onNodeWithTag("track_sheet_part_$part").performScrollTo()
        compose.onNodeWithTag("track_sheet_part_$part").performClick()
        compose.waitForIdle()
        assertFalse(h.vm.project.value.song[part].plays(key))
        compose.onNodeWithTag("track_sheet_part_$part").performScrollTo().performClick()
        compose.waitForIdle()
        assertTrue(h.vm.project.value.song[part].plays(key))
        // « Copier cette chaîne sur… » › the lead: the bass's effects and sends on the lead, one undo step.
        tap("track_sheet_copy")
        tap("track_sheet_copy_inst:$lead")
        val strips = h.vm.project.value.mixer.channels
        assertEquals(strips[InstrumentTrack.mixerChannel(bass)].inserts, strips[InstrumentTrack.mixerChannel(lead)].inserts)
        assertTrue(h.vm.project.value.guide.historyOf(GuideTrack.inst(lead)).isNotEmpty())
        h.vm.undo()
        assertEquals(p0.mixer.channels[InstrumentTrack.mixerChannel(lead)], h.vm.project.value.mixer.channels[InstrumentTrack.mixerChannel(lead)])
        // The sheet is still there after the menu, no window over it, and its boxes still answer.
        compose.onNodeWithTag("track_sheet_title").performScrollTo().assertIsDisplayed()
        assertTrue(compose.onAllNodesWithText("Compris").fetchSemanticsNodes().isEmpty())
        compose.onNodeWithTag("track_sheet_part_$part").performScrollTo().performClick()
        compose.waitForIdle()
        assertFalse(h.vm.project.value.song[part].plays(key))
    }
}
