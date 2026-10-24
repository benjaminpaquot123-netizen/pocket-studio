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
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import com.pocketstudio.model.instrumentTracks
import com.pocketstudio.model.viewOfTrack
import com.pocketstudio.model.track
import androidx.compose.ui.unit.dp
import com.pocketstudio.model.multiPacks
import com.pocketstudio.model.allScenes
import com.pocketstudio.model.sceneLabel
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
    fun continuingTheLastProjectAfterLaunchIsOneTouch() {
        // « Que veux-tu faire ? » as the app opens on it, wired as AppRoot wires it (the whole app: LaunchFlowTest).
        val vm = vmWithSong()
        val before = vm.project.value
        var open by mutableStateOf(true)
        compose.setContent {
            PocketStudioTheme {
                val project by vm.project.collectAsState()
                com.pocketstudio.app.onboarding.LaunchChoice(
                    open, com.pocketstudio.app.onboarding.launchUi(project, null, System.currentTimeMillis(), simple = false, missionsActive = false),
                    com.pocketstudio.app.onboarding.LaunchActions(resume = { open = false }),
                )
            }
        }
        compose.onNodeWithTag("launch").assertIsDisplayed()
        compose.withinGestures("Continuer après l'ouverture", 1) { tap("launch_continue") }
        compose.onNodeWithTag("launch").assertDoesNotExist()
        assertTrue(before === vm.project.value)
        assertTrue(!vm.transport.value.playing)
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

    /** From « Ton morceau est prêt »: « Changer un son » opens the tune's sounds, a tile changes it (2), one undo step. */
    @Test
    fun changingASoundFromTheReadyCardIsTwoTouches() {
        val vm = vmWithSong()
        val before = vm.project.value.synth
        val lib = com.pocketstudio.model.SoundLibrary.SYNTH
        val current = com.pocketstudio.model.SoundLibrary.synthNameOf(before, lib)
        // The sheet opens on the current sound's family: a tile of that family, not the current one.
        val family = lib.firstOrNull { it.name == current }?.category ?: com.pocketstudio.model.SoundCategories.SYNTH.first()
        val pick = lib.indexOfFirst { it.category == family && it.name != current }
        compose.setContent {
            PocketStudioTheme {
                var chooser by remember { mutableStateOf<Int?>(null) }
                Column {
                    com.pocketstudio.app.onboarding.SongReadyCard(
                        com.pocketstudio.app.onboarding.SongReadyUi("Mon tube", 160, 3),
                        com.pocketstudio.app.onboarding.SongReadyActions(changeSound = { vm.selectTrack(0); chooser = 0 }),
                    )
                    chooser?.let { SoundChooserSheet(vm, it, close = { chooser = null }) }
                }
            }
        }
        compose.withinGestures("Changer un son", 2) {
            tap("song_ready_sound")
            tap("synth_sounds_$pick")
        }
        assertEquals(lib[pick].name, com.pocketstudio.model.SoundLibrary.synthNameOf(vm.project.value.synth, lib))
        compose.onNodeWithTag("sound_chooser").assertDoesNotExist()
        vm.undo()
        assertEquals(before, vm.project.value.synth)
    }

    /** ⤴ in the top bar, then « Partager »: 2 (the name is already there). */
    @Test
    fun sharingIsTwoTouchesFromAnyTab() {
        var shared = 0
        compose.setContent {
            PocketStudioTheme {
                var open by remember { mutableStateOf(false) }
                Column {
                    TopBar(TopBarUi("Mon tube", playing = false, canUndo = true, canRedo = false), TopBarActions(share = { open = true }))
                    if (open) {
                        com.pocketstudio.app.export.ShareSheet(
                            com.pocketstudio.app.export.ShareSheetUi("Mon tube", seconds = 180, parts = 6, checks = emptyList()),
                            com.pocketstudio.app.export.ShareSheetActions(share = { shared++; open = false }, close = { open = false }),
                        )
                    }
                }
            }
        }
        compose.withinGestures("Partager", 2) {
            tap("top_share")
            tap("share_go")
        }
        assertEquals(1, shared)
    }

    // --- Phase 34 (b): one intention, one button -----------------------------------------------

    /** The Morceau tab's list of parts, wired to the ViewModel as AppRoot wires it. */
    @Composable
    private fun Song(vm: RhythmViewModel) {
        val p by vm.project.collectAsState()
        com.pocketstudio.app.session.SessionScreen(
            com.pocketstudio.app.session.SessionScreenUi(
                scenes = List(com.pocketstudio.model.Scene.MAX) { p.allScenes.getOrNull(it) }, current = p.scene, pending = null,
                song = p.song, songIndex = -1, playing = false, tempoBpm = p.tempoBpm,
                sheetOf = { com.pocketstudio.app.session.partSheetUi(vm.project.value, it) },
            ),
            com.pocketstudio.app.session.SessionActions(partIntent = vm::partIntent, removePart = vm::removeSongPart, movePart = vm::moveSongPart),
        )
    }

    /** [action] from the part sheet of part [part] (appui long, then its button): 2 gestures, one undo step, « Fait : … » said. */
    private fun partIntentIsTwoGestures(action: String, part: Int, tag: String, vm: RhythmViewModel = vmWithSong(), check: (com.pocketstudio.model.Project) -> Unit) {
        val before = vm.project.value
        compose.setContent { PocketStudioTheme { Song(vm) } }
        compose.withinGestures(action, 2) {
            longPress("part_play_$part")
            tap(tag)
        }
        check(vm.project.value)
        assertTrue(vm.done.value, vm.done.value!!.startsWith("Fait"))
        compose.onNodeWithTag("part_sheet").assertDoesNotExist()
        vm.undoDone()
        assertEquals(before.song, vm.project.value.song)
        assertEquals(before.automation, vm.project.value.automation)
        assertEquals(null, vm.done.value)
    }

    @Test
    fun aVariationIsTwoGestures() = partIntentIsTwoGestures("Variation", 1, "part_variation") { p ->
        assertTrue(p.song[1].scene !in 0..2)
        assertEquals(listOf(0, 2), listOf(p.song[0].scene, p.song[2].scene))
    }

    @Test
    fun aSharedPartSaysItIsPlayedSeveralTimes() {
        val vm = vmWithSong()
        vm.addSongPart(1)  // B twice: A B C B
        compose.setContent { PocketStudioTheme { Song(vm) } }
        compose.withinGestures("Variation (jouée 2 fois)", 2) {
            longPress("part_play_3")
            compose.onNodeWithTag("part_edit_all").assertTextContains("2", substring = true)
            tap("part_variation")
        }
        val p = vm.project.value
        assertEquals(1, p.song[1].scene)
        assertTrue(p.song[3].scene > 2)
    }

    @Test
    fun aFillIsTwoGestures() = partIntentIsTwoGestures("Fill en fin de partie", 1, "part_fill") { p ->
        val fill = p.song[1].play.getValue(com.pocketstudio.model.SongPart.DRUMS)
        assertEquals("Fill", p.sceneLabel(fill))
    }

    @Test
    fun aBreakIsTwoGestures() = partIntentIsTwoGestures("Break", 1, "part_break_4") { p ->
        assertEquals(4, p.song.size)
        assertEquals("Break", p.sceneLabel(p.song[2].scene))
        assertTrue(p.allScenes[p.song[2].scene].drums.lanes[0].hits.isEmpty())
    }

    @Test
    fun aBuildUpIsTwoGestures() = partIntentIsTwoGestures("Montée avant", 2, "part_buildup_4") { p ->
        assertTrue(p.automation.any { it.target == com.pocketstudio.model.AutoTarget.live(com.pocketstudio.model.AutoTarget.LIVE_BUILD_UP) })
    }

    @Test
    fun aFadeIsTwoGestures() = partIntentIsTwoGestures("Fondu d'entrée", 0, "part_fade_in") { p ->
        assertTrue(p.automation.single().target.isMasterVolume)
    }

    @Test
    fun anEndingIsTwoGestures() = partIntentIsTwoGestures("Fin", 2, "part_ending") { p ->
        assertEquals(4, p.song.size)
        assertEquals("Fin", p.sceneLabel(p.song[3].scene))
        assertTrue(!p.songLoops)
    }

    @Test
    fun energyIsTwoGestures() = partIntentIsTwoGestures("Énergie", 1, "part_energy_1") { p ->
        assertTrue(com.pocketstudio.model.SongPart.DRUMS in p.song[1].muted)
    }

    @Test
    fun lengtheningIsTwoGestures() = partIntentIsTwoGestures("Allonger", 0, "part_longer") { p -> assertEquals(2, p.song[0].repeats) }

    @Test
    fun theBarsCanBeTyped() {
        val vm = vmWithSong()
        compose.setContent { PocketStudioTheme { Song(vm) } }
        val g = compose.withinGestures("Longueur en mesures", 2) {
            longPress("part_play_0")
            type("part_bars_field", "6")
            tap("part_bars_ok")
        }
        assertEquals(1, g.typed)
        assertEquals(6, vm.project.value.song[0].repeats)
    }

    /** The Rythme tab, wired to the ViewModel as AppRoot wires it. */
    @Composable
    private fun Rhythm(vm: RhythmViewModel) {
        val p by vm.project.collectAsState()
        val t by vm.transport.collectAsState()
        com.pocketstudio.app.rhythm.RhythmScreen(
            p, t, com.pocketstudio.app.rhythm.LiveUi(), canUndo = true, canRedo = false,
            actions = com.pocketstudio.app.rhythm.RhythmActions(
                toggleStep = vm::toggleStep, cycleVelocity = vm::cycleVelocity, setHitVelocity = vm::setHitVelocity,
                selectPad = vm::selectPad, removeSampleOf = vm::removeSampleOf, setPadVolume = vm::setPadVolume, togglePadMute = vm::togglePadMute,
                openKits = vm::refreshPacks, chooseKit = vm::chooseKit,
                paintStep = vm::paintStep, paintEnd = vm::paintEnd, setRoll = vm::setRoll, burst = vm::burstAt,
            ),
            kits = vm.kitChoices(emptyList(), vm.library.value),
        )
    }

    private fun withPadSample(vm: RhythmViewModel) =
        vm.session.edit { it.copy(drums = it.drums.withSample(0, com.pocketstudio.model.PadSample("k.wav", "Kick de ma voix"))) }

    @Test
    fun changingTheWholeKitIsTwoGestures() {
        val vm = vmWithSong()
        withPadSample(vm)
        val before = vm.project.value
        compose.setContent { PocketStudioTheme { Rhythm(vm) } }
        compose.onNodeWithTag("kit_chip").assertTextContains("Perso", substring = true)
        compose.withinGestures("Changer le kit", 2) {
            tap("kit_chip")
            tap("kit_choice_builtin:3")
        }
        val p = vm.project.value
        assertEquals(com.pocketstudio.model.Project.KIT_V3, p.kitVersion)
        assertTrue(p.drums.lanes.all { it.sample == null })
        compose.onNodeWithTag("kit_chip").assertTextContains("Studio", substring = true)
        vm.undoDone()
        assertEquals(before.drums.lanes[0].sample, vm.project.value.drums.lanes[0].sample)
        assertEquals(before.kitVersion, vm.project.value.kitVersion)
    }

    @Test
    fun aPadsSoundIsTwoGesturesAndItsVolumeIsThere() {
        val vm = vmWithSong()
        withPadSample(vm)
        compose.setContent { PocketStudioTheme { Rhythm(vm) } }
        compose.withinGestures("Son d'un pad", 2) {
            longPress("pad_0")
            tap("pad_sound_origin")
        }
        assertEquals(null, vm.project.value.drums.lanes[0].sample)
        vm.undo()
        assertTrue(vm.project.value.drums.lanes[0].sample != null)
        // The same sheet: the pad's volume and « Couper ce pad ».
        compose.withinGestures("Couper un pad", 2) {
            longPress("pad_3")
            tap("pad_mute")
        }
        assertEquals(3, vm.transport.value.selectedPad)
        assertTrue(vm.project.value.mixer.channels[com.pocketstudio.model.Mixer.padChannel(3)].mute)
        compose.onNodeWithTag("pad_volume").assertIsDisplayed()
    }

    @Test
    fun aLineOfHitsIsPaintedInOneStrokeAndUndoneInOneStep() {
        val vm = vmWithSong()
        vm.selectPad(7)
        assertTrue(vm.project.value.drums.lanes[7].hits.isEmpty())
        compose.setContent { PocketStudioTheme { Rhythm(vm) } }
        compose.onNodeWithTag("step_cells").performScrollTo().performTouchInput {
            swipe(androidx.compose.ui.geometry.Offset(width / 16f, height / 4f), androidx.compose.ui.geometry.Offset(width - width / 16f, height / 4f), 400)
        }
        compose.waitForIdle()
        assertEquals((0..7).toSet(), vm.project.value.drums.lanes[7].hits.keys)
        // Started on a hit: the stroke erases.
        compose.onNodeWithTag("step_cells").performTouchInput {
            swipe(androidx.compose.ui.geometry.Offset(width / 16f, height / 4f), androidx.compose.ui.geometry.Offset(width / 2f - 4f, height / 4f), 300)
        }
        compose.waitForIdle()
        assertTrue(vm.project.value.drums.lanes[7].hits.keys.none { it < 4 })
        vm.undo()
        assertEquals((0..7).toSet(), vm.project.value.drums.lanes[7].hits.keys)
        vm.undo()
        assertTrue(vm.project.value.drums.lanes[7].hits.isEmpty())
    }

    /**
     * Review m9: a stroke cut short (the page turning with the playhead under the finger restarts the
     * gesture) is still one undo step of its own, not merged into the next stroke.
     */
    @Test
    fun aStrokeCutShortIsStillItsOwnUndoStep() {
        val vm = vmWithSong()
        vm.selectPad(7)
        vm.session.edit { it.copy(drums = it.drums.withSteps(32)) }
        var step by androidx.compose.runtime.mutableIntStateOf(0)
        compose.setContent {
            PocketStudioTheme {
                val p by vm.project.collectAsState()
                val t by vm.transport.collectAsState()
                com.pocketstudio.app.rhythm.RhythmScreen(
                    p, t.copy(playing = true), com.pocketstudio.app.rhythm.LiveUi(currentStep = step), canUndo = true, canRedo = false,
                    actions = com.pocketstudio.app.rhythm.RhythmActions(toggleStep = vm::toggleStep, paintStep = vm::paintStep, paintEnd = vm::paintEnd),
                )
            }
        }
        val depth = vm.session.undoDepth
        compose.onNodeWithTag("step_cells").performScrollTo().performTouchInput {
            down(androidx.compose.ui.geometry.Offset(width / 16f, height / 4f))
            repeat(6) { moveBy(androidx.compose.ui.geometry.Offset(width / 16f, 0f)) }
        }
        compose.waitForIdle()
        val painted = vm.project.value.drums.lanes[7].hits.keys
        assertTrue(painted.isNotEmpty())
        assertEquals("the stroke is still open", depth, vm.session.undoDepth)
        step = 20  // the playhead in bar 2: the page turns, the finger still down
        compose.waitForIdle()
        assertEquals(depth + 1, vm.session.undoDepth)
        // The next edit is a step of its own: undoing it leaves the stroke.
        vm.toggleStep(15)
        vm.undo()
        assertEquals(painted, vm.project.value.drums.lanes[7].hits.keys)
        vm.undo()
        assertTrue(vm.project.value.drums.lanes[7].hits.isEmpty())
    }

    @Test
    fun aRollIsTwoGesturesUpToEightAndRising() {
        val vm = vmWithSong()
        vm.selectPad(2)
        if (vm.project.value.drums.lanes[2].hits[4] == null) vm.toggleStep(4)
        compose.setContent { PocketStudioTheme { Rhythm(vm) } }
        compose.withinGestures("Roulement", 2) {
            longPress("step_4")
            tap("roll_8")
        }
        assertEquals(8, vm.project.value.drums.lanes[2].hits.getValue(4).repeats)
        compose.onNodeWithTag("roll_ramp_1").performClick()
        compose.waitForIdle()
        assertEquals(1, vm.project.value.drums.lanes[2].hits.getValue(4).ramp)
        vm.undo()
        assertEquals(0, vm.project.value.drums.lanes[2].hits.getValue(4).ramp)
    }

    /** « Changer le son » beside the chosen track: the sheet of its sounds, a tile (2), one undo step. */
    @Test
    fun changingATracksSoundFromItsNameIsTwoGestures() {
        val vm = vmWithSong()
        val before = vm.project.value.synth
        val lib = com.pocketstudio.model.SoundLibrary.SYNTH
        val current = com.pocketstudio.model.SoundLibrary.synthNameOf(before, lib)
        val family = lib.firstOrNull { it.name == current }?.category ?: com.pocketstudio.model.SoundCategories.SYNTH.first()
        val pick = lib.indexOfFirst { it.category == family && it.name != current }
        compose.setContent {
            PocketStudioTheme {
                var chooser by remember { mutableStateOf<Int?>(null) }
                val p by vm.project.collectAsState()
                Column {
                    com.pocketstudio.app.synth.TrackBar(
                        com.pocketstudio.app.synth.TracksUi(p.instrumentTracks, selected = 0, changeSound = { vm.selectTrack(it); chooser = it }),
                    )
                    chooser?.let { SoundChooserSheet(vm, it, close = { chooser = null }) }
                }
            }
        }
        compose.withinGestures("Changer le son d'une piste", 2) {
            tap("track_sound_0")
            tap("synth_sounds_$pick")
        }
        assertEquals(lib[pick].name, com.pocketstudio.model.SoundLibrary.synthNameOf(vm.project.value.synth, lib))
        vm.undo()
        assertEquals(before, vm.project.value.synth)
    }

    // --- Phase 35: record without fear ----------------------------------------------------------

    /** The Clavier's record bar under the top of the app, wired to the ViewModel as AppRoot wires it. */
    @Composable
    private fun Keys(vm: RhythmViewModel, source: com.pocketstudio.app.record.TakeSource = com.pocketstudio.app.record.TakeSource.KEYS) {
        Column {
            Top(vm)
            com.pocketstudio.app.record.RecordBar(
                com.pocketstudio.app.record.recordBarUi(vm, "Enregistrer ma mélodie", source),
                com.pocketstudio.app.record.recordBarActions(vm, source),
            )
        }
    }

    /** A key played at sixteenth [from], let go at [to] (the music, not a gesture of the path). */
    private fun RhythmViewModel.key(pitch: Int, from: Double, to: Double) {
        recordPositionForTest = from
        playedClockForTest = from
        synthNoteOn(pitch, 0.8f)
        recordPositionForTest = to
        playedClockForTest = to
        synthNoteOff(pitch)
    }

    /** ● Enregistrer › (the melody) › ■ Arrêter › Remettre en rythme: 3 gestures, nothing lost, an undo step each. */
    @Test
    fun recordingAMelodyAndPuttingItInTimeIsThreeGestures() {
        val vm = vmWithSong()
        vm.playingForTest = true
        val t = com.pocketstudio.model.NoteClip.TICKS_PER_STEP
        compose.setContent { PocketStudioTheme { Keys(vm) } }
        compose.withinGestures("Enregistrer une mélodie et la remettre en rythme", 3) {
            tap("record_start")
            vm.key(60, 1.2, 2.9)
            vm.key(64, 3.8, 5.0)
            tap("record_stop")
            compose.onNodeWithTag("take_summary").assertTextContains("2 notes", substring = true)
            tap("take_in_time")
        }
        assertEquals(listOf(t, 4 * t), vm.project.value.synthNotes.notes.map { it.start })
        // Nothing lost: « Comme joué » puts them back where they were played.
        compose.onNodeWithTag("take_as_played").performClick()
        compose.waitForIdle()
        assertEquals(listOf(28, 91), vm.project.value.synthNotes.notes.map { it.start })
        compose.onNodeWithTag("take_strength_100").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("take_keep").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("record_start").assertIsDisplayed()
        // One undo step per action: in time, then the take.
        vm.undo()
        assertEquals(listOf(28, 91), vm.project.value.synthNotes.notes.map { it.start })
        vm.undo()
        assertTrue(vm.project.value.synthNotes.notes.isEmpty())
    }

    /** « Garder ce que je viens de jouer » (1), then « Remettre en rythme » (2): played without recording, kept after. */
    @Test
    fun keepingWhatWasJustPlayedIsOneGesture() {
        val vm = vmWithSong()
        compose.setContent { PocketStudioTheme { Keys(vm) } }
        vm.key(60, 100.0, 102.0)
        vm.key(62, 104.3, 106.0)
        compose.waitForIdle()
        compose.withinGestures("Garder ce que je viens de jouer", 1) { tap("keep_played") }
        assertEquals(listOf(60, 62), vm.project.value.synthNotes.notes.map { it.pitch })
        compose.withinGestures("Le remettre en rythme", 1) { tap("take_in_time") }
        assertEquals(4 * com.pocketstudio.model.NoteClip.TICKS_PER_STEP, vm.project.value.synthNotes.notes[1].start)
        vm.undo()
        vm.undo()
        assertTrue(vm.project.value.synthNotes.notes.isEmpty())
    }

    /** « Refaire »: the take goes and a new one starts (1 gesture). */
    @Test
    fun redoingATakeIsOneGesture() {
        val vm = vmWithSong()
        vm.playingForTest = true
        compose.setContent { PocketStudioTheme { Keys(vm) } }
        compose.onNodeWithTag("record_start").performClick()
        vm.key(60, 1.0, 2.0)
        compose.onNodeWithTag("record_stop").performClick()
        compose.waitForIdle()
        compose.withinGestures("Refaire la prise", 1) { tap("take_redo") }
        assertTrue(vm.project.value.synthNotes.notes.isEmpty())
        assertTrue(vm.recording.value)
        compose.onNodeWithTag("record_running").assertIsDisplayed()
    }

    /** No Live take overwrites the song for good: « Versions du morceau » › « Remettre » (2), one undo step. */
    @Test
    fun bringingBackTheSongALiveTakeReplacedIsTwoGestures() {
        val vm = vmWithSong()
        val before = vm.project.value.song
        // A Live take (« Nouvelle version », the default) launching B's drums.
        vm.togglePlay()
        vm.takeClockForTest = 5
        vm.togglePerformance()
        vm.takeClockForTest = 16
        vm.tickPerformance()
        vm.launchPad(com.pocketstudio.model.SongPart.DRUMS, 1)
        vm.liveLaunchedForTest(com.pocketstudio.model.LiveSlots.DRUMS, 1, 32)
        vm.takeClockForTest = 40
        vm.togglePerformance()
        val taken = vm.project.value.song
        assertTrue(taken != before)
        compose.setContent {
            PocketStudioTheme {
                var open by remember { mutableStateOf(false) }
                val p by vm.project.collectAsState()
                Column {
                    com.pocketstudio.app.session.SessionScreen(
                        com.pocketstudio.app.session.SessionScreenUi(
                            scenes = List(com.pocketstudio.model.Scene.MAX) { p.allScenes.getOrNull(it) }, current = p.scene, pending = null,
                            song = p.song, songIndex = -1, playing = false, tempoBpm = p.tempoBpm, versions = p.songVersions.size,
                        ),
                        com.pocketstudio.app.session.SessionActions(openVersions = { open = true }),
                    )
                    if (open) {
                        com.pocketstudio.app.record.SongVersionsSheet(
                            p, com.pocketstudio.app.record.SongVersionsActions(restore = { open = false; vm.restoreSongVersion(it) }, close = { open = false }),
                        )
                    }
                }
            }
        }
        compose.withinGestures("Remettre le morceau d'avant la prise", 2) {
            tap("song_versions")
            tap("version_restore_0")
        }
        assertEquals(before, vm.project.value.song)
        // The take's song is kept in its place.
        assertEquals(taken, vm.project.value.songVersions.first().song)
        vm.undo()
        assertEquals(taken, vm.project.value.song)
    }

    /** Phase 38: « Ajouter une piste » › « 808 »: a gliding 808 track, nothing to download (2), one undo step. */
    @Test
    fun adding808TrackIsTwoGestures() {
        val vm = vmWithSong()
        val before = vm.project.value.tracks.size
        compose.setContent {
            PocketStudioTheme {
                val p by vm.project.collectAsState()
                com.pocketstudio.app.synth.TrackBar(
                    com.pocketstudio.app.synth.TracksUi(p.instrumentTracks, selected = 0, add = vm::addTrack, addKind = vm::addTrackOf),
                )
            }
        }
        compose.withinGestures("Ajouter une piste 808", 2) {
            tap("track_add")
            tap("track_add_808")
        }
        val added = vm.project.value.tracks.last()
        assertEquals(before + 1, vm.project.value.tracks.size)
        assertEquals(com.pocketstudio.model.InstrumentTrack.ENGINE_808, added.engine)
        assertEquals("808", added.name)
        assertTrue((added.voice808.param(com.pocketstudio.model.PadVoice.GLIDE) ?: 0f) > 0f)
        assertTrue(vm.project.value.multiPacks.isEmpty())
        compose.onNodeWithTag("track_add_sheet").assertDoesNotExist()
        vm.undo()
        assertEquals(before, vm.project.value.tracks.size)
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

    // --- Phase 37: write like a pro -------------------------------------------------------------

    /** [vmWithSong] whose first track plays a C chord in scenes A, B and C, made by the assistant (its tracks follow the chords). */
    private fun vmWithChords(): RhythmViewModel {
        val vm = vmWithSong()
        val c = com.pocketstudio.model.NoteClip(steps = 16, notes = listOf(60, 64, 67).map { com.pocketstudio.model.Note(0, 16 * com.pocketstudio.model.NoteClip.TICKS_PER_STEP, it) })
        vm.session.edit { p ->
            val scenes = p.allScenes.map { it.copy(synthNotes = c) }
            p.copy(scenes = scenes, synthNotes = c, style = "house", harmony = com.pocketstudio.model.Harmony(0, com.pocketstudio.model.Scale.Major))
        }
        return vm
    }

    /** The Morceau tab's song card with « Accords du morceau », wired as AppRoot wires them. */
    @Composable
    private fun SongWithChords(vm: RhythmViewModel) {
        var open by remember { mutableStateOf(false) }
        val p by vm.project.collectAsState()
        Column {
            com.pocketstudio.app.session.SessionScreen(
                com.pocketstudio.app.session.SessionScreenUi(
                    scenes = List(com.pocketstudio.model.Scene.MAX) { p.allScenes.getOrNull(it) }, current = p.scene, pending = null,
                    song = p.song, songIndex = -1, playing = false, tempoBpm = p.tempoBpm,
                    chords = com.pocketstudio.app.session.songChordsStrip(p, english = false),
                ),
                com.pocketstudio.app.session.SessionActions(openChords = { open = true }),
            )
            if (open) com.pocketstudio.app.session.SongChordsRoute(vm, close = { open = false })
        }
    }

    /** « Accords du morceau » › a progression (heard) › « Appliquer à tout le morceau »: 3 gestures, one undo step. */
    @Test
    fun theWholeSongsChordsAreThreeGestures() {
        val vm = vmWithChords()
        val before = vm.project.value
        compose.setContent { PocketStudioTheme { SongWithChords(vm) } }
        compose.withinGestures("Accords de tout le morceau", 3) {
            tap("song_chords")
            tap("song_progression_Sad")
            // Heard before it is applied: the notes already moved, no undo step yet.
            assertEquals(listOf(57, 60, 64), vm.project.value.synthNotes.notes.map { it.pitch }.sorted())
            tap("song_chords_apply")
        }
        val p = vm.project.value
        // Every scene of the song: its chord written (one bar: the progression's first, vi = La m), its notes on it.
        for (s in 0..2) {
            assertEquals(listOf(5), p.allScenes[s].chords)
            assertEquals(listOf(57, 60, 64), p.allScenes[s].synthNotes.notes.map { it.pitch }.sorted())
        }
        assertTrue(vm.done.value!!.startsWith("Fait"))
        // One step back: the chords and every note as before.
        vm.undoDone()
        assertEquals(before.allScenes.map { it.synthNotes }, vm.project.value.allScenes.map { it.synthNotes })
        assertTrue(vm.project.value.allScenes.all { it.chords.isEmpty() })
    }

    /** Closing the sheet (or « Annuler ») after listening: nothing changed, no undo step. */
    @Test
    fun chordsTriedAndNotAppliedLeaveTheSongAsItWas() {
        val vm = vmWithChords()
        val before = vm.project.value
        val depth = vm.session.undoDepth
        compose.setContent { PocketStudioTheme { SongWithChords(vm) } }
        compose.onNodeWithTag("song_chords").performClick()
        compose.onNodeWithTag("song_progression_Epic").performClick()
        compose.onNodeWithTag("chord_0_0").performClick()
        compose.onNodeWithTag("chord_pick_4").performClick()  // V on scene A's bar
        compose.waitForIdle()
        assertEquals(listOf(55, 59, 62), vm.project.value.synthNotes.notes.map { it.pitch }.sorted())
        compose.onNodeWithTag("song_chords_cancel").performClick()
        compose.waitForIdle()
        assertEquals(before.allScenes.map { it.synthNotes }, vm.project.value.allScenes.map { it.synthNotes })
        assertEquals(depth, vm.session.undoDepth)
        compose.onNodeWithTag("song_chords_sheet").assertDoesNotExist()
    }

    /** The first track's piano roll, wired to the ViewModel as AppRoot wires it (its clipboard is the app's). */
    @Composable
    private fun Roll(vm: RhythmViewModel) {
        val full by vm.project.collectAsState()
        val selected by vm.selectedTrack.collectAsState()
        val board by vm.noteClipboard.collectAsState()
        val p = full.viewOfTrack(selected)
        com.pocketstudio.app.synth.PianoRoll(
            p.synthNotes, emptyList(), -1,
            com.pocketstudio.app.synth.PianoRollActions(
                add = vm::addNote, remove = vm::removeNote, change = vm::changeNote, setSteps = vm::setNoteClipSteps,
                replaceClip = vm::replaceNoteClip, copyNotes = vm::copyNotes,
            ),
            gridHeight = 200.dp, clipboard = board,
        )
    }

    /** A bar copied and pasted on the next one: touch its number, Copier, Coller (3); in another track: Coller (1). */
    @Test
    fun aBarIsCopiedAndPastedInThreeGestures() {
        val vm = vmWithChords()
        compose.setContent { PocketStudioTheme { Roll(vm) } }
        compose.withinGestures("Copier-coller une mesure", 3) {
            tap("roll_bar_0")
            tap("sel_copy")
            tap("sel_paste")
        }
        val clip = vm.project.value.synthNotes
        assertEquals(32, clip.steps)  // the one-bar loop grew to hold the copy
        val bar = 16 * com.pocketstudio.model.NoteClip.TICKS_PER_STEP
        assertEquals(clip.notes.filter { it.start < bar }.map { it.pitch }.sorted(), clip.notes.filter { it.start >= bar }.map { it.pitch }.sorted())
        // Another track: the clipboard is the app's, one touch pastes there.
        vm.addTrack()
        compose.waitForIdle()
        compose.withinGestures("Coller dans une autre piste", 1) { tap("sel_paste") }
        val other = vm.project.value.track(vm.selectedTrack.value).notes
        assertEquals(listOf(60, 64, 67), other.notes.map { it.pitch }.sorted())
    }

    /** In the piano roll a touch chooses a note, a long press deletes it; a finger across the lane sets strengths. */
    @Test
    fun aTouchChoosesALongPressDeletesAndAStrokeSetsStrengths() {
        val vm = vmWithChords()
        compose.setContent { PocketStudioTheme { Roll(vm) } }
        // The C chord's notes sit on the first sixteenth: the lane is drawn down from 1 to 0.2.
        compose.onNodeWithTag("velocity_lane").performTouchInput {
            down(androidx.compose.ui.geometry.Offset(4f, height * 0.1f))
            moveTo(androidx.compose.ui.geometry.Offset(6f, height * 0.8f))
            up()
        }
        compose.waitForIdle()
        assertTrue(vm.project.value.synthNotes.notes.all { it.velocity < 0.5f })
        val count = vm.project.value.synthNotes.notes.size
        compose.onNodeWithTag("roll_bar_0").performClick()
        compose.onNodeWithTag("selection_count").assertTextContains("3", substring = true)
        compose.onNodeWithTag("sel_delete").performScrollTo().performClick()
        compose.waitForIdle()
        assertEquals(count - 3, vm.project.value.synthNotes.notes.size)
    }

    /** A synth knob's name held › « Automatiser »: 2 gestures, its lane made along the song a beat at a time, and opened. */
    @Test
    fun automatingAKnobIsTwoGestures() {
        val vm = vmWithSong()
        compose.setContent {
            PocketStudioTheme {
                val p by vm.project.collectAsState()
                val target by vm.automateTarget.collectAsState()
                Column {
                    com.pocketstudio.app.synth.SoundCard(
                        p.synth, com.pocketstudio.app.synth.SynthActions(automate = { vm.automateKnob(com.pocketstudio.model.AutoTarget.synth(0, it)) }),
                        advanced = false, setAdvanced = {},
                    )
                    target?.let { com.pocketstudio.app.mixer.AutomateLaneRoute(vm, it, close = vm::closeAutomateLane) }
                }
            }
        }
        val cutoff = com.pocketstudio.model.SynthPatch.CUTOFF
        compose.withinGestures("Automatiser un potard", 2) {
            longPress("knob_$cutoff")
            tap("knob_automate")
        }
        val lane = vm.project.value.automation.single()
        assertEquals(com.pocketstudio.model.AutoTarget.synth(0, cutoff), lane.target)
        assertEquals(com.pocketstudio.model.AutomationLane.SONG, lane.mode)
        assertTrue(lane.songPerBeat)  // a quarter of a bar in 4/4
        compose.onNodeWithTag("automate_sheet").assertExists()
        // The parts' names along the curve.
        compose.onNodeWithTag("auto_parts_0").assertExists()
        // Again: the same lane opens, no second one.
        vm.closeAutomateLane()
        vm.automateKnob(com.pocketstudio.model.AutoTarget.synth(0, cutoff))
        assertEquals(1, vm.project.value.automation.size)
        assertEquals(com.pocketstudio.model.AutoTarget.synth(0, cutoff), vm.automateTarget.value)
    }

    /** A hit held › its chance; the pad's own loop with − / +: a polyrhythm in a gesture or two. */
    @Test
    fun aHitsChanceIsTwoGesturesAndAPadLoopsOnItsOwn() {
        val vm = vmWithSong()
        vm.selectPad(2)
        if (vm.project.value.drums.lanes[2].hits[4] == null) vm.toggleStep(4)
        compose.setContent {
            PocketStudioTheme {
                val p by vm.project.collectAsState()
                val t by vm.transport.collectAsState()
                com.pocketstudio.app.rhythm.RhythmScreen(
                    p, t, com.pocketstudio.app.rhythm.LiveUi(), canUndo = true, canRedo = false,
                    actions = com.pocketstudio.app.rhythm.RhythmActions(
                        toggleStep = vm::toggleStep, selectPad = vm::selectPad, setHitVelocity = vm::setHitVelocity, setRoll = vm::setRoll,
                        setPadLength = vm::setPadLength, setHitChance = vm::setHitChance, rerollChances = vm::rerollChances,
                    ),
                )
            }
        }
        // The pad loops over 15 steps then 14 (taps in a row: one undo step), and back to the loop.
        compose.onNodeWithTag("pad_len_minus").performScrollTo().performClick()
        compose.onNodeWithTag("pad_len_minus").performClick()
        compose.waitForIdle()
        assertEquals(14, vm.project.value.drums.lanes[2].length)
        compose.onNodeWithTag("pad_len_value").assertTextContains("14", substring = true)
        vm.undo()
        assertEquals(0, vm.project.value.drums.lanes[2].length)
        compose.onNodeWithTag("step_4").performScrollTo()
        compose.withinGestures("Chance d'un coup", 2) {
            longPress("step_4")
            tap("roll_chance_50")
        }
        assertEquals(50, vm.project.value.drums.lanes[2].hits.getValue(4).chance)
        // « Autre tirage »: another seed, one undo step.
        val seed = vm.project.value.drumSeed
        compose.onNodeWithTag("roll_menu").assertExists()
        vm.rerollChances()
        assertTrue(vm.project.value.drumSeed != seed)
        vm.undo()
        assertEquals(seed, vm.project.value.drumSeed)
    }

    /**
     * A sink in the engine's place: renders give back a second of sound, and the channels soloed
     * for each render are kept.
     */
    private class FreezeSink : com.pocketstudio.engine.CommandSink {
        val soloed = ArrayList<Int>()
        private var next = 1L
        @Synchronized override fun post(target: Long, type: Int, value: Float, a: Int, b: Int) {
            if (target != 0L && type == 23 && value > 0.5f) soloed += a  // CMD_CHANNEL_SOLO on a render engine
        }
        override fun scheduleFrom(target: Long, frame: Long) = Unit
        override fun install(target: Long, kind: com.pocketstudio.engine.CommandSink.Install, index: Int, sample: Long) = Unit
        @Synchronized override fun sampleCreate(data: FloatArray, channels: Int, sampleRate: Int): Long = next++
        override fun sampleFrames(handle: Long): Long = 48_000L
        override fun sampleRelease(handle: Long) = Unit
        @Synchronized override fun exportCreate(): Long = next++
        override fun exportRender(target: Long, path: String, frames: Long, tailFrames: Long, fadeOutFrames: Long, autoTail: Boolean, floatWav: Boolean, sampleRate: Int, bits: Int) =
            com.pocketstudio.engine.ExportResult.Ok
        override fun exportRenderFloat(target: Long, frames: Long, tailFrames: Long): FloatArray = FloatArray(96_000) { 0.1f * kotlin.math.sin(it * 0.01f) }
        override fun exportDestroy(target: Long) = Unit
    }

    /** A track held › « Geler la piste »: 2 gestures; its sound on an audio track, its notes kept and silent in the song; « Dégeler » back. */
    @Test
    fun freezingATrackIsTwoGestures() {
        val vm = vmWithChords()
        val sink = FreezeSink()
        com.pocketstudio.engine.NativeEngine.sink = sink
        try {
            compose.setContent {
                PocketStudioTheme {
                    val p by vm.project.collectAsState()
                    val freezing by vm.freezing.collectAsState()
                    com.pocketstudio.app.synth.TrackBar(
                        com.pocketstudio.app.synth.TracksUi(
                            p.instrumentTracks, selected = 0, select = vm::selectTrack, freeze = vm::freezeTrack, unfreeze = vm::unfreezeTrack,
                            frozen = com.pocketstudio.model.Freeze.frozenIds(p), freezing = freezing,
                        ),
                    )
                }
            }
            compose.withinGestures("Geler la piste", 2) {
                longPress("track_0")
                tap("track_freeze")
            }
            val until = System.currentTimeMillis() + 30_000
            while (!com.pocketstudio.model.Freeze.isFrozen(vm.project.value, 0)) {
                assertTrue("the track was never frozen (${vm.sampleTask.value})", System.currentTimeMillis() < until)
                org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(50))
                Thread.sleep(5)
            }
            val p = vm.project.value
            val audio = com.pocketstudio.model.Freeze.audioOf(p, 0)!!
            assertTrue(audio.clip!!.song)
            assertEquals(listOf(com.pocketstudio.model.InstrumentTrack.mixerChannel(0)), sink.soloed.distinct())  // the track's strip alone
            assertTrue(p.synthNotes.notes.isNotEmpty())  // its notes kept, untouched
            assertTrue(com.pocketstudio.model.SongPart.inst(0) in com.pocketstudio.model.Freeze.mutedKeys(p))
            compose.waitForIdle()
            compose.onNodeWithTag("track_frozen_0", useUnmergedTree = true).assertExists()
            // « Dégeler »: 2 gestures, then the freeze is one undo step away again.
            compose.withinGestures("Dégeler la piste", 2) {
                longPress("track_0")
                tap("track_unfreeze")
            }
            assertTrue(vm.project.value.audioTracks.isEmpty())
            vm.undo()
            assertTrue(com.pocketstudio.model.Freeze.isFrozen(vm.project.value, 0))
            vm.undo()
            assertTrue(vm.project.value.audioTracks.isEmpty())
        } finally {
            com.pocketstudio.engine.NativeEngine.sink = null
        }
    }
}
