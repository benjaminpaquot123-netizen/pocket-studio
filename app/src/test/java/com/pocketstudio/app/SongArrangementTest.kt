package com.pocketstudio.app

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.app.rhythm.SessionUi
import com.pocketstudio.app.session.SessionActions
import com.pocketstudio.app.session.SessionScreen
import com.pocketstudio.app.session.SessionScreenUi
import com.pocketstudio.app.session.RenameSceneDialog
import androidx.compose.ui.test.onNodeWithText
import com.pocketstudio.model.copySceneTo
import com.pocketstudio.model.renameScene
import com.pocketstudio.model.DrumPattern
import com.pocketstudio.model.Scene
import com.pocketstudio.model.SongPart
import com.pocketstudio.model.sceneTitle
import com.pocketstudio.model.allScenes
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
    fun aSceneMovedByDragKeepsTheSongAndUndoesInOneStep() {
        val vm = vmWithSong()
        vm.renameScene(1, "Couplet")
        val before = vm.project.value
        val hits = { i: Int -> vm.project.value.allScenes[i].drums.lanes[0].hits.keys }
        // B to the empty slot F: the song plays the same scenes from their new places.
        vm.moveScene(1, 5)
        assertEquals(listOf(0, 5, 2), vm.songScenes())
        assertEquals(before.allScenes[1].drums.lanes[0].hits.keys, hits(5))
        assertEquals("F · Couplet", vm.project.value.sceneTitle(5))
        // A (shown) swapped with C: the screens stay on A, now in C's slot.
        vm.moveScene(0, 2)
        assertEquals(listOf(2, 5, 0), vm.songScenes())
        assertEquals(2, vm.project.value.scene)
        vm.undo()
        assertEquals(listOf(0, 5, 2), vm.songScenes())
        assertEquals(0, vm.project.value.scene)
        vm.undo()
        assertEquals(listOf(0, 1, 2), vm.songScenes())
        assertEquals(before.allScenes, vm.project.value.allScenes)
        // Replacing C with A: every part of A or C now plays A.
        vm.moveScene(0, 2, replace = true)
        assertEquals(listOf(2, 1, 2), vm.songScenes())
        assertEquals(before.allScenes[0].drums.lanes[0].hits.keys, hits(2))
    }

    @Test
    fun undoingASceneMoveWhilePlayingStaysOnTheSceneHeard() {
        val vm = vmWithSong()
        vm.playingForTest = true  // what follows the scene heard, as while playing
        val hits = { vm.project.value.drums.lanes[0].hits.keys }
        val a = hits()
        // A (shown and heard) swapped with C: A plays on from slot C.
        vm.moveScene(0, 2)
        assertEquals(2, vm.project.value.scene)
        assertEquals(a, hits())
        // Annuler: A back in its slot, still the scene heard and shown (not C, now in slot C).
        vm.undo()
        assertEquals(0, vm.project.value.scene)
        assertEquals(a, hits())
        vm.redo()
        assertEquals(2, vm.project.value.scene)
        assertEquals(a, hits())
        vm.undo()
        // Replacing C with A, then Annuler: A again, from slot A.
        vm.moveScene(0, 2, replace = true)
        assertEquals(a, hits())
        vm.undo()
        assertEquals(0, vm.project.value.scene)
        assertEquals(a, hits())
        assertEquals(listOf(0, 1, 2), vm.songScenes())
    }

    @Test
    fun aSceneIsDraggedOntoAnotherSlotAndAFullOneAsks() {
        val moves = mutableListOf<String>()
        val selected = mutableListOf<Int>()
        showScreen(SessionActions(select = { selected += it }, moveScene = { f, t, r -> moves += "$f>$t" + if (r) " replace" else "" }))
        // B onto the empty F: moved at once.
        drag(1, 5)
        assertEquals(listOf("1>5"), moves)
        // B onto A (not empty): a question first.
        drag(1, 0)
        compose.onNodeWithTag("scene_move_swap").performClick()
        drag(1, 0)
        compose.onNodeWithTag("scene_move_replace").assertTextContains("A", substring = true).performClick()
        drag(1, 0)
        compose.onNodeWithTag("scene_move_cancel").performClick()
        assertEquals(listOf("1>5", "1>0", "1>0 replace"), moves)
        // A drag is never also a touch; a touch still picks the scene.
        assertTrue(selected.isEmpty())
        compose.onNodeWithTag("scene_1").performClick()
        assertEquals(listOf(1), selected)
    }

    /** Holds scene [from], then slides the finger onto slot [to] and lets go. */
    private fun drag(from: Int, to: Int) {
        compose.onNodeWithTag("scene_$to").performScrollTo()
        compose.onNodeWithTag("scene_$from").performScrollTo()
        val a = compose.onNodeWithTag("scene_$from").fetchSemanticsNode().boundsInRoot
        val b = compose.onNodeWithTag("scene_$to").fetchSemanticsNode().boundsInRoot
        compose.onNodeWithTag("scene_$from").performTouchInput {
            down(center)
            advanceEventTime(viewConfiguration.longPressTimeoutMillis + 200)
            moveBy(androidx.compose.ui.geometry.Offset(0f, 1f))
            val steps = 10
            repeat(steps) { moveBy((b.center - a.center) / steps.toFloat(), delayMillis = 16) }
            up()
        }
        compose.waitForIdle()
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
    fun playGoesToTheSongOnceThereIsOneOnEveryTab() {
        File(app.filesDir, "projects").deleteRecursively()
        val vm = RhythmViewModel(app)
        vm.playStartsForTest = mutableListOf()
        assertFalse(vm.listensToSong)  // no song yet: ▶ plays the scene
        vm.togglePlay()
        vm.togglePlay()
        vm.addSongPart(0)
        // A song: ▶ plays it (the same ▶ on every tab), from its start.
        assertTrue(vm.listensToSong)
        vm.togglePlay()
        vm.togglePlay()
        // « ⟳ Partie »: the scene shown loops instead; off again, the song.
        vm.setLoopPart(true)
        assertFalse(vm.listensToSong)
        vm.togglePlay()
        vm.togglePlay()
        vm.setLoopPart(false)
        assertTrue(vm.listensToSong)
        assertEquals(listOf("part 0", "song 0", "part 0"), vm.playStartsForTest)
    }

    @Test
    fun playStartsWhereTheSongIsAndAPartAskedForTurnsTheLoopOff() {
        val vm = vmWithSong()  // A, B, C
        vm.playStartsForTest = mutableListOf()
        assertEquals(0, vm.playFrom.value)
        // Picking B to edit: ▶ plays the song from B's part.
        vm.selectScene(1)
        assertEquals(1, vm.playFrom.value)
        vm.togglePlay()
        vm.togglePlay()
        // Picking the scene of the start point again keeps it; A, played from its first part.
        vm.selectScene(1)
        assertEquals(1, vm.playFrom.value)
        vm.selectScene(0)
        assertEquals(0, vm.playFrom.value)
        // « Jouer depuis… » the third part: the start point, and « ⟳ Partie » off (the song was asked for).
        vm.setLoopPart(true)
        vm.playSongPart(2)
        assertEquals(2, vm.playFrom.value)
        assertFalse(vm.loopPart.value)
        vm.togglePlay()
        vm.togglePlay()
        // « Jouer le morceau » on Morceau: from the top (without the engine nothing plays here).
        vm.setLoopPart(true)
        vm.toggleSong()
        assertFalse(vm.loopPart.value)
        assertEquals(0, vm.playFrom.value)
        assertEquals(listOf("song 1", "song 2", "song 0"), vm.playStartsForTest)
        // Parts removed under the start point: from the start.
        vm.playSongPart(2)
        vm.removeSongPart(2)
        vm.removeSongPart(1)
        vm.playStartsForTest = mutableListOf()
        vm.togglePlay()
        assertEquals(listOf("song 0"), vm.playStartsForTest)
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
    fun aTrackOnAnotherSceneIsChosenByALongPressAndUndoesInOneStep() {
        val vm = vmWithSong()
        val depth = vm.session.undoDepth
        vm.setPartScene(0, SongPart.DRUMS, 2)
        assertEquals(mapOf(SongPart.DRUMS to 2), vm.project.value.song[0].play)
        assertTrue(vm.project.value.song[0].mixed)
        assertEquals(depth + 1, vm.session.undoDepth)
        vm.setPartScene(0, SongPart.DRUMS, 0)  // its own scene again: a plain part
        assertFalse(vm.project.value.song[0].mixed)
        vm.undo()
        assertEquals(mapOf(SongPart.DRUMS to 2), vm.project.value.song[0].play)
        vm.undo()
        assertFalse(vm.project.value.song[0].mixed)
    }

    @Test
    fun theTimelineTogglesAClipAndALongPressChoosesItsScene() {
        val chosen = mutableListOf<String>()
        val toggled = mutableListOf<String>()
        val played = mutableListOf<Int>()
        val muted = mutableListOf<Int>()
        var p = com.pocketstudio.model.Project(drums = DrumPattern().toggle(0, 0)).sanitized()
            .copySceneTo(1).copySceneTo(2)
        p = p.copy(
            song = listOf(
                SongPart(0, 2),
                SongPart(0, 1, play = mapOf(SongPart.ACID to 1), from = mapOf(SongPart.DRUMS to 1), bars = 3),
            ),
        ).sanitized()
        compose.setContent {
            PocketStudioTheme {
                com.pocketstudio.app.session.TimelineScreen(
                    com.pocketstudio.app.session.TimelineUi(
                        p,
                        rows = listOf(
                            com.pocketstudio.app.session.TimelineRow(SongPart.DRUMS, "Batterie", androidx.compose.ui.graphics.Color.Blue),
                            com.pocketstudio.app.session.TimelineRow(SongPart.ACID, "Acid 303", androidx.compose.ui.graphics.Color.Green),
                        ),
                    ),
                    com.pocketstudio.app.session.TimelineActions(
                        setPartScene = { i, key, scene -> chosen += "$i $key $scene" },
                        togglePartTrack = { i, key -> toggled += "$i $key" },
                        playPart = { played += it },
                        toggleMute = { muted += it },
                    ),
                    barDp = androidx.compose.ui.unit.Dp(32f),
                )
            }
        }
        // Part 1 (two bars) is 64 dp wide; the drums' row is the first, 64 dp high.
        val inDrums = with(compose.density) { androidx.compose.ui.geometry.Offset(16.dp.toPx(), 32.dp.toPx()) }
        compose.onNodeWithTag("timeline_grid").performTouchInput { click(inDrums) }
        // Phase 36: two touches on a clip open it, so one touch waits to know it is alone.
        compose.mainClock.advanceTimeBy(600)
        compose.waitForIdle()
        assertEquals(listOf("0 drums"), toggled)
        // A long press: the clip's actions, then « Changer de scène… ».
        compose.onNodeWithTag("timeline_grid").performTouchInput { longClick(inDrums) }
        compose.onNodeWithTag("tl_scene").performClick()
        compose.onNodeWithTag("tl_pick_2").performClick()
        assertEquals(listOf("0 drums 2"), chosen)
        assertEquals(listOf("0 drums"), toggled)
        // The ruler plays from the part touched (the second starts at 64 dp).
        val onPart2 = with(compose.density) { androidx.compose.ui.geometry.Offset(80.dp.toPx(), 34.dp.toPx()) }
        compose.onNodeWithTag("timeline_ruler").performTouchInput { click(onPart2) }
        assertEquals(listOf(1), played)
        // M mutes the drums' mixer channel.
        compose.onNodeWithTag("timeline_mute_drums").performClick()
        assertEquals(listOf(0), muted)
    }

    @Test
    fun theTimelineFollowsThePlayheadUntilAFingerScrollsAndPlayBringsItBack() {
        // Ten parts of four bars (40 bars), 40 dp a bar: about seven bars on the screen.
        val p = com.pocketstudio.model.Project(drums = DrumPattern().toggle(0, 0)).sanitized()
            .let { it.copy(song = List(10) { SongPart(0, 4) }).sanitized() }
        val playing = androidx.compose.runtime.mutableStateOf(true)
        val step = androidx.compose.runtime.mutableIntStateOf(0)
        val played = mutableListOf<Int>()
        compose.setContent {
            PocketStudioTheme {
                com.pocketstudio.app.session.TimelineScreen(
                    com.pocketstudio.app.session.TimelineUi(
                        p, rows = listOf(com.pocketstudio.app.session.TimelineRow(SongPart.DRUMS, "Batterie", androidx.compose.ui.graphics.Color.Blue)),
                        playing = playing.value, songIndex = if (playing.value) step.intValue / 64 else -1,
                        songStep = { if (playing.value) step.intValue else -1 },
                    ),
                    com.pocketstudio.app.session.TimelineActions(playPart = { played += it }),
                    barDp = androidx.compose.ui.unit.Dp(40f),
                )
            }
        }
        val nearLeft = with(compose.density) { androidx.compose.ui.geometry.Offset(60.dp.toPx(), 40.dp.toPx()) }
        compose.onNodeWithTag("timeline_follow").assertIsSelected()
        // The playhead reaches bar 29 (part 8): the page turns, part 8 is now at the screen's left.
        step.intValue = 28 * 16
        compose.waitForIdle()
        compose.onNodeWithTag("timeline_ruler").performTouchInput { click(nearLeft) }
        assertEquals(listOf(7), played)
        // A finger scrolls back: « Suivre » goes off and the follow no longer pulls the view.
        compose.onNodeWithTag("timeline_grid").performTouchInput { swipeRight() }
        compose.onNodeWithTag("timeline_follow").assertIsNotSelected()
        step.intValue = 36 * 16
        compose.waitForIdle()
        compose.onNodeWithTag("timeline_ruler").performTouchInput { click(nearLeft) }
        assertTrue(played.last() < 7)
        // Stop, then Lecture from the start: « Suivre » is back on and the view returns to bar 1.
        playing.value = false
        compose.waitForIdle()
        step.intValue = 0
        playing.value = true
        compose.waitForIdle()
        compose.onNodeWithTag("timeline_follow").assertIsSelected()
        compose.onNodeWithTag("timeline_ruler").performTouchInput { click(nearLeft) }
        assertEquals(0, played.last())
    }

    @Test
    fun theSongScreenOpensTheTimeline() {
        var opened = false
        val drums = DrumPattern().toggle(0, 0)
        compose.setContent {
            PocketStudioTheme {
                SessionScreen(
                    SessionScreenUi(
                        scenes = listOf(Scene("A", drums)) + List(23) { null },
                        current = 0, pending = null, songIndex = -1, playing = false, tempoBpm = 120.0,
                        song = listOf(SongPart(0, 2)),
                        rows = listOf(com.pocketstudio.app.session.TimelineRow(SongPart.DRUMS, "Batterie", androidx.compose.ui.graphics.Color.Blue)),
                    ),
                    SessionActions(openTimeline = { opened = true }),
                )
            }
        }
        compose.onNodeWithTag("open_timeline").performClick()
        assertTrue(opened)
    }

    @Test
    fun aPartIsCutInTwoAndTheLoopStaysInTheSong() {
        val vm = vmWithSong()
        vm.setSongRepeats(0, 3)
        vm.splitSongPart(0, 1)
        assertEquals(listOf(0, 0, 1, 2), vm.songScenes())
        assertEquals(listOf(1, 2), vm.project.value.song.take(2).map { it.repeats })
        vm.undo()
        assertEquals(listOf(0, 1, 2), vm.songScenes())
        assertEquals(3, vm.project.value.song[0].repeats)
        // A loop over parts 2 and 3; one past the song's end is no loop.
        vm.setSongLoop(1..2)
        assertEquals(1..2, vm.songLoop.value)
        assertEquals(1, SessionUi(songIndex = 2).upcoming(3, loops = false, loop = vm.songLoop.value))
        vm.setSongLoop(0..5)
        assertEquals(null, vm.songLoop.value)
        // « Ouvrir dans … », stopped: the scene is shown to be edited.
        vm.showSceneToEdit(2)
        assertEquals(2, vm.project.value.scene)
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
                setSongLoops = { set += "loops $it" },
                setFollowSong = { set += "follow $it" },
            ),
        )
        // What ▶ plays is no longer chosen here: « ⟳ Partie », under the top bar, is the one place.
        compose.onNodeWithTag("listen_0").assertDoesNotExist()
        compose.onNodeWithTag("song_end_1").performScrollTo().performClick()
        compose.onNodeWithTag("follow_1").performScrollTo().performClick()
        compose.onNodeWithTag("follow_0").performScrollTo().performClick()
        assertEquals(listOf("loops true", "follow false", "follow true"), set)
    }
}
