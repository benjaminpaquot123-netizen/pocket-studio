package com.pocketstudio.app

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.mixer.alsoSet
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.model.Channel
import com.pocketstudio.model.EffectType
import com.pocketstudio.model.FxPresets
import com.pocketstudio.model.Insert
import com.pocketstudio.model.Mixer
import com.pocketstudio.model.withFx
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/** « Joue dans » from the effects box: an effect kept to some scenes (song sections). */
@RunWith(RobolectricTestRunner::class)
class InsertScenesVmTest {
    @get:Rule
    val compose = androidx.compose.ui.test.junit4.createComposeRule()
    private val app: Application = ApplicationProvider.getApplicationContext()

    private fun freshVm(): RhythmViewModel {
        File(app.filesDir, "projects").deleteRecursively()
        return RhythmViewModel(app)
    }

    @Test
    fun keepingAnEffectToSomeScenesIsOneUndoStepAndFollowsTheScenes() {
        val vm = freshVm()
        vm.toggleHit(0, 0)
        vm.duplicateScene()  // B, shown
        vm.renameScene(1, "Groove")
        assertEquals(listOf("", "Groove"), vm.sceneLabels.value)
        vm.setInsertType(0, 0, EffectType.DISTORTION)
        vm.setInsertScenes(0, 0, setOf(1))
        assertEquals(setOf(1), vm.project.value.mixer.channels[0].inserts[0].scenes)
        vm.undo()
        assertNull(vm.project.value.mixer.channels[0].inserts[0].scenes)  // back to « Tout »
        vm.redo()
        assertEquals(setOf(1), vm.project.value.mixer.channels[0].inserts[0].scenes)
        // B dragged onto C: its effect goes with it; a copy of it (shown) plays it too.
        vm.moveScene(1, 2)
        assertEquals(setOf(2), vm.project.value.mixer.channels[0].inserts[0].scenes)
        vm.duplicateScene()
        assertEquals(setOf(1, 2), vm.project.value.mixer.channels[0].inserts[0].scenes)
        // Moved up the chain, the choice goes with the effect.
        vm.setInsertType(0, 1, EffectType.CHORUS)
        vm.moveInsert(0, 0, 1)
        assertEquals(setOf(1, 2), vm.project.value.mixer.channels[0].inserts[1].scenes)
        assertNull(vm.project.value.mixer.channels[0].inserts[0].scenes)
        // The master's effects too.
        vm.setInsertType(Mixer.MASTER, 0, EffectType.COMPRESSOR)
        vm.setInsertScenes(Mixer.MASTER, 0, setOf(0))
        assertEquals(setOf(0), vm.project.value.mixer.master[0].scenes)
    }

    @Test
    fun theEngineHearsEachEffectInTheScenesItPlaysIn() {
        val inserts = listOf(
            Insert.of(EffectType.CHORUS).copy(scenes = setOf(1)),
            Insert.of(EffectType.FILTER),
            Insert(),  // empty: nothing sent
            Insert.of(EffectType.EQ).copy(bypass = true, scenes = setOf(0)),  // switched off: nothing sent
            Insert.of(EffectType.WAH).copy(scenes = emptySet()),  // its scene was replaced: nowhere
        )
        fun sent(scene: Int) = buildList { RhythmViewModel.insertOnCommands(inserts, scene) { slot, on -> add(slot to on) } }
        assertEquals(listOf(0 to false, 1 to true, 4 to false), sent(0))
        assertEquals(listOf(0 to true, 1 to true, 4 to false), sent(1))
    }

    @Test
    fun anAmbienceThatSetsNoEffectInPlaceStillShowsWhatItSets() {
        // « Petite pièce »: only the Space quick control, nothing in place.
        val room = Channel().withFx(FxPresets.byId("small_room")!!)
        assertTrue(room.inserts.none { it.effect != EffectType.NONE })
        assertTrue(room.alsoSet())
        assertTrue(Channel(sends = listOf(0.3f, 0f)).alsoSet())
        assertFalse(Channel().alsoSet())
        assertFalse(Channel().withFx(FxPresets.NONE).alsoSet())
    }

    @Test
    fun anEffectWhoseSceneWasReplacedSaysItPlaysNowhere() {
        // In B only, then A dropped onto B (replace): B is now A, where it did not play.
        val vm = freshVm()
        vm.toggleHit(0, 0)
        vm.duplicateScene()
        vm.setInsertType(0, 0, EffectType.DISTORTION)
        vm.setInsertScenes(0, 0, setOf(1))
        vm.moveScene(0, 1, replace = true)
        val ins = vm.project.value.mixer.channels[0].inserts[0]
        assertEquals(emptySet<Int>(), ins.scenes)
        var picked: Set<Int>? = setOf(-1)
        val actions = com.pocketstudio.app.mixer.MixerActions(
            kindOf = { com.pocketstudio.model.SoundKind.DRUMS },
            scenes = kotlinx.coroutines.flow.MutableStateFlow(listOf("", "")),
            setInsertScenes = { _, _, s -> picked = s },
        )
        compose.setContent {
            com.pocketstudio.ui.PocketStudioTheme {
                com.pocketstudio.app.mixer.EffectsBox(0, Channel(inserts = Mixer.sanitizeInserts(listOf(ins))), actions)
            }
        }
        compose.onNodeWithTag("fx_scenes_none_0_0").assertExists()
        // No chip says otherwise: neither « Tout » nor a letter is on.
        compose.onNodeWithTag("fx_scene_0_0_all").assertIsNotSelected()
        compose.onNodeWithTag("fx_scene_0_0_0").assertIsNotSelected()
        compose.onNodeWithTag("fx_scene_0_0_1").assertIsNotSelected()
        compose.onNodeWithTag("fx_scene_0_0_all").performClick()
        assertNull(picked)
    }

    @Test
    fun anEffectPlayingSomewhereHasNoNowhereLine() {
        val actions = com.pocketstudio.app.mixer.MixerActions(
            kindOf = { com.pocketstudio.model.SoundKind.DRUMS },
            scenes = kotlinx.coroutines.flow.MutableStateFlow(listOf("", "")),
        )
        compose.setContent {
            com.pocketstudio.ui.PocketStudioTheme {
                com.pocketstudio.app.mixer.EffectsBox(0, Channel(inserts = Mixer.sanitizeInserts(listOf(Insert.of(EffectType.CHORUS).copy(scenes = setOf(1))))), actions)
            }
        }
        compose.onNodeWithTag("fx_scenes_none_0_0").assertDoesNotExist()
        compose.onNodeWithTag("fx_scene_0_0_1").assertIsSelected()
    }
}
