package com.pocketstudio.app.ui

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.model.DefaultNames
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * The gesture budgets of docs/plan/clics.md (column « Cible »), counted on the top of the app as
 * every tab has it (the top bar and what is right under it), on a small phone. A path that gets
 * longer fails here. Later phases add their actions with [withinGestures].
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp")
class GestureBudgetTest {
    private val app: Application = ApplicationProvider.getApplicationContext()

    @get:Rule
    val compose = createComposeRule()

    /** A fresh project with three filled scenes A, B, C and the song A B C (the scene shown: A). */
    private fun vmWithSong(): RhythmViewModel {
        File(app.filesDir, "projects").deleteRecursively()
        val vm = RhythmViewModel(app)
        vm.toggleHit(0, 0)
        vm.duplicateScene()
        vm.toggleHit(0, 4)
        vm.duplicateScene()
        vm.toggleHit(0, 8)
        vm.selectScene(0)
        listOf(0, 1, 2).forEach(vm::addSongPart)
        return vm
    }

    /** The top bar and what is under it, wired to the ViewModel as AppRoot wires them. */
    @Composable
    private fun Top(vm: RhythmViewModel) {
        var renaming by remember { mutableStateOf(false) }
        var tempo by remember { mutableStateOf(false) }
        val project by vm.project.collectAsState()
        val transport by vm.transport.collectAsState()
        Column {
            TopBar(
                TopBarUi(DefaultNames.shown(project.name), transport.playing, canUndo = true, canRedo = false, scene = sceneChip(project)),
                TopBarActions(rename = { renaming = true }, togglePlay = vm::togglePlay, selectScene = vm::selectScene),
            )
            UnderTopBar(vm, tempoOpen = tempo, setTempoOpen = { tempo = it }, renaming = renaming, closeRename = { renaming = false })
        }
    }

    private fun show(vm: RhythmViewModel) = compose.setContent { PocketStudioTheme { Top(vm) } }

    @Test
    fun listeningToTheWholeSongIsOneTouch() {
        val vm = vmWithSong()
        vm.selectScene(1)  // editing B, as on the Rythme tab: ▶ still plays the song (from B's part)
        vm.playStartsForTest = mutableListOf()
        show(vm)
        compose.onNodeWithTag("top_position").assertTextContains("mes. 2/3", substring = true)
        compose.withinGestures("Écouter tout le morceau", 1) { tap("top_play") }
        assertEquals(listOf("song 1"), vm.playStartsForTest)
        assertTrue(vm.transport.value.playing)
    }

    @Test
    fun changingTheTempoIsTwoTouchesAndOneUndoStep() {
        val vm = vmWithSong()
        show(vm)
        compose.withinGestures("Changer le tempo", 2) {
            tap("top_tempo")
            tap("tempo_preset_140")
        }
        assertEquals(140.0, vm.project.value.tempoBpm, 0.0)
        compose.onNodeWithTag("tempo_sheet").assertDoesNotExist()
        compose.onNodeWithTag("top_tempo").assertTextContains("140", substring = true)
        vm.undo()
        assertEquals(120.0, vm.project.value.tempoBpm, 0.0)
    }

    @Test
    fun aTypedTempoAndHalfOrDoubleAreOneUndoStep() {
        val vm = vmWithSong()
        show(vm)
        compose.withinGestures("Taper un tempo", 7) {
            tap("top_tempo")
            tap("tempo_double")  // 240, heard at once
            tap("tempo_half")  // 120
            tap("tempo_half")  // 60
            tap("tempo_key_1")
            tap("tempo_key_2")
            tap("tempo_key_8")
        }
        assertEquals(60.0, vm.project.value.tempoBpm, 0.0)
        compose.onNodeWithTag("tempo_display").assertTextContains("128")
        compose.onNodeWithTag("tempo_key_ok").assertIsDisplayed()
        compose.withinGestures("Valider le tempo", 1) { tap("tempo_key_ok") }
        assertEquals(128.0, vm.project.value.tempoBpm, 0.0)
        compose.onNodeWithTag("tempo_sheet").assertDoesNotExist()
        // The whole sheet is one step: back to 120 at once (not to 60, 120 or 240).
        vm.undo()
        assertEquals(120.0, vm.project.value.tempoBpm, 0.0)
        vm.redo()
        assertEquals(128.0, vm.project.value.tempoBpm, 0.0)
    }

    @Test
    fun renamingIsTwoTouchesAndTheNameTyped() {
        val vm = vmWithSong()
        val before = vm.project.value.name
        show(vm)
        val g = compose.withinGestures("Renommer le projet", 2) {
            longPress("top_projects")
            type("rename_project_field", "Mon tube")
            tap("rename_project_ok")
        }
        assertEquals(1, g.typed)
        assertEquals("Mon tube", vm.project.value.name)
        compose.onNodeWithTag("rename_project_field").assertDoesNotExist()
        vm.undo()
        assertEquals(before, vm.project.value.name)
    }

    @Test
    fun turningEverySoloOffIsOneTouch() {
        val vm = vmWithSong()
        vm.toggleSolo(0)
        vm.toggleSolo(1)
        show(vm)
        compose.onNodeWithTag("solo_banner").assertIsDisplayed().assertTextContains("Batterie", substring = true)
        compose.withinGestures("Désactiver le solo", 1) { tap("solo_banner") }
        assertTrue(vm.project.value.mixer.channels.none { it.solo })
        compose.onNodeWithTag("solo_banner").assertDoesNotExist()
        // One undo step brings both back.
        vm.undo()
        assertEquals(2, vm.project.value.mixer.channels.count { it.solo })
    }

    @Test
    fun loopingThePartIsOneTouchAndShows() {
        val vm = vmWithSong()
        vm.playStartsForTest = mutableListOf()
        show(vm)
        compose.withinGestures("Boucler la partie", 1) { tap("top_loop_part") }
        assertTrue(vm.loopPart.value)
        compose.onNodeWithTag("top_position_loop", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("top_play").performClick()
        compose.waitForIdle()
        assertEquals(listOf("part 0"), vm.playStartsForTest)
    }

    @Test
    fun playingFromAPartIsTwoTouchesFromAnyTab() {
        val vm = vmWithSong()
        show(vm)
        compose.withinGestures("Jouer à partir d'une partie", 2) {
            tap("top_position")
            tap("play_from_2")
        }
        assertEquals(2, vm.playFrom.value)
        compose.onNodeWithTag("top_position").assertTextContains("mes. 3/3", substring = true)
    }

    @Test
    fun aLongerPathFailsTheBudget() {
        val vm = vmWithSong()
        show(vm)
        val e = assertThrows(AssertionError::class.java) {
            compose.withinGestures("Changer le tempo", 1) {
                tap("top_tempo")
                tap("tempo_preset_90")
            }
        }
        assertTrue(e.message, e.message!!.contains("top_tempo › tempo_preset_90"))
    }
}
