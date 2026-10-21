package com.pocketstudio.app

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.app.rhythm.SessionUi
import com.pocketstudio.app.session.SessionActions
import com.pocketstudio.app.session.SessionScreen
import com.pocketstudio.app.session.SessionScreenUi
import com.pocketstudio.app.session.RenameSceneDialog
import androidx.compose.ui.test.onNodeWithText
import com.pocketstudio.model.DrumPattern
import com.pocketstudio.model.Scene
import com.pocketstudio.model.SongPart
import com.pocketstudio.model.sceneTitle
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h890dp")
class SongArrangementTest {
    private val app: Application = ApplicationProvider.getApplicationContext()

    @get:Rule
    val compose = createComposeRule()

    /** A fresh project with three filled scenes A, B, C and the song A B C. */
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

    private fun RhythmViewModel.songScenes() = project.value.song.map { it.scene }

    @Test
    fun theSongIsReorderedInsertedAndDuplicatedOneUndoStepEach() {
        val vm = vmWithSong()
        assertEquals(listOf(0, 1, 2), vm.songScenes())
        vm.moveSongPart(2, 0)
        assertEquals(listOf(2, 0, 1), vm.songScenes())
        vm.insertSongPart(1, 1)
        assertEquals(listOf(2, 1, 0, 1), vm.songScenes())
        vm.setSongRepeats(0, 3)
        vm.duplicateSongPart(0)
        assertEquals(listOf(2, 2, 1, 0, 1), vm.songScenes())
        assertEquals(3, vm.project.value.song[1].repeats)
        vm.removeSongPart(3)
        assertEquals(listOf(2, 2, 1, 1), vm.songScenes())
        // Each edit undoes on its own, in reverse.
        vm.undo(); assertEquals(listOf(2, 2, 1, 0, 1), vm.songScenes())
        vm.undo(); assertEquals(listOf(2, 1, 0, 1), vm.songScenes()); assertEquals(3, vm.project.value.song[0].repeats)
        vm.undo(); assertEquals(1, vm.project.value.song[0].repeats)
        vm.undo(); assertEquals(listOf(2, 0, 1), vm.songScenes())
        vm.undo(); assertEquals(listOf(0, 1, 2), vm.songScenes())
        vm.redo(); assertEquals(listOf(2, 0, 1), vm.songScenes())
    }

    @Test
    fun aFullSongSaysSo() {
        val vm = vmWithSong()
        repeat(SongPart.MAX_PARTS) { vm.addSongPart(0) }
        assertEquals(SongPart.MAX_PARTS, vm.project.value.song.size)
        vm.dismissNotice()
        vm.duplicateSongPart(0)
        assertNotNull(vm.notice.value)
        assertEquals(SongPart.MAX_PARTS, vm.project.value.song.size)
    }

    @Test
    fun scenesAreNamedAndTheNameUndoes() {
        val vm = vmWithSong()
        vm.renameScene(1, "Refrain")
        assertEquals("B · Refrain", vm.project.value.sceneTitle(1))
        vm.renameScene(1, "")
        assertEquals("B", vm.project.value.sceneTitle(1))
        vm.undo()
        assertEquals("B · Refrain", vm.project.value.sceneTitle(1))
        // Renaming does not change the scene shown.
        assertEquals(0, vm.project.value.scene)
    }

    @Test
    fun theSongStopsAtTheEndByDefaultAndTheSettingUndoes() {
        val vm = vmWithSong()
        assertFalse(vm.project.value.songLoops)
        vm.setSongLoops(true)
        assertTrue(vm.project.value.songLoops)
        vm.undo()
        assertFalse(vm.project.value.songLoops)
    }

    @Test
    fun playGoesToTheSongOnceThereIsOne() {
        File(app.filesDir, "projects").deleteRecursively()
        val vm = RhythmViewModel(app)
        assertFalse(vm.listensToSong)  // no song yet: ▶ plays the scene
        vm.addSongPart(0)
        // Until chosen: the section being edited on the editing tabs, the song on the Morceau tab.
        assertFalse(vm.listensToSong)
        vm.onSongTab = true
        assertTrue(vm.listensToSong)
        vm.setListenSong(false)
        assertFalse(vm.listensToSong)
        vm.setListenSong(true)
        assertTrue(vm.listensToSong)
    }

    @Test
    fun whatComesNextInTheSong() {
        // Part 0 of 3 plays: part 1 comes next; a part chosen comes next instead.
        assertEquals(1, SessionUi(songIndex = 0).upcoming(3, loops = false))
        assertEquals(0, SessionUi(songIndex = 1, queued = 0).upcoming(3, loops = false))
        // The last part: the song ends, or starts over.
        assertEquals(-1, SessionUi(songIndex = 2).upcoming(3, loops = false))
        assertEquals(0, SessionUi(songIndex = 2).upcoming(3, loops = true))
        assertEquals(-1, SessionUi(songIndex = 2, ending = true).upcoming(3, loops = false))
        // A scene loop plays and the song starts from part 2 at the next bar.
        assertEquals(2, SessionUi(pending = 1, queued = 2).upcoming(3, loops = false))
        assertEquals(-1, SessionUi().upcoming(3, loops = false))
    }

    private fun showScreen(actions: SessionActions, song: List<SongPart> = listOf(SongPart(0, 2), SongPart(1), SongPart(0))) {
        val drums = DrumPattern().toggle(0, 0)
        compose.setContent {
            PocketStudioTheme {
                SessionScreen(
                    SessionScreenUi(
                        scenes = listOf(Scene("A", drums, label = "Intro"), Scene("B", drums)) + List(22) { null },
                        current = 1, pending = null, song = song, songIndex = -1, playing = false, tempoBpm = 120.0,
                    ),
                    actions,
                )
            }
        }
    }

    @Test
    fun theScreenPlaysFromAPartAndEditsTheSong() {
        val calls = mutableListOf<String>()
        showScreen(
            SessionActions(
                playPart = { calls += "play $it" },
                movePart = { from, to -> calls += "move $from $to" },
                duplicatePart = { calls += "dup $it" },
                insertPart = { at, scene -> calls += "insert $at $scene" },
                removePart = { calls += "remove $it" },
            ),
        )
        // The song comes first and shows the section names.
        compose.onNodeWithTag("part_play_0").assertTextContains("A · Intro", substring = true)
        compose.onNodeWithTag("part_play_1").performClick()
        compose.onNodeWithTag("part_menu_1").performClick()
        compose.onNodeWithTag("part_up_1").performClick()
        compose.onNodeWithTag("part_menu_0").performClick()
        compose.onNodeWithTag("part_down_0").performClick()
        compose.onNodeWithTag("part_menu_2").performClick()
        compose.onNodeWithTag("part_duplicate_2").performClick()
        compose.onNodeWithTag("part_menu_0").performClick()
        compose.onNodeWithTag("part_insert_0").performClick()
        compose.onNodeWithTag("pick_scene_1").performClick()
        compose.onNodeWithTag("part_menu_2").performClick()
        compose.onNodeWithTag("part_remove_2").performClick()
        assertEquals(listOf("play 1", "move 1 0", "move 0 1", "dup 2", "insert 1 1", "remove 2"), calls)
    }

    @Test
    fun theSongSettingsAreTwoPlainChoices() {
        val set = mutableListOf<String>()
        showScreen(
            SessionActions(
                setListenSong = { set += "listen $it" },
                setSongLoops = { set += "loops $it" },
                setFollowSong = { set += "follow $it" },
            ),
        )
        // The same words as the switch next to Play in the Rhythm tab: one setting, one label.
        compose.onNode(androidx.compose.ui.test.hasTestTag("listen_0") and androidx.compose.ui.test.hasText("cette section")).assertExists()
        compose.onNode(androidx.compose.ui.test.hasTestTag("listen_1") and androidx.compose.ui.test.hasText("tout le morceau")).assertExists()
        compose.onNodeWithTag("listen_0").performClick()
        compose.onNodeWithTag("song_end_1").performScrollTo().performClick()
        compose.onNodeWithTag("follow_1").performScrollTo().performClick()
        compose.onNodeWithTag("follow_0").performScrollTo().performClick()
        assertEquals(listOf("listen false", "loops true", "follow false", "follow true"), set)
    }
}
