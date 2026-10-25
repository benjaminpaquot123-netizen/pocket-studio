package com.pocketstudio.app.rhythm

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.model.PadSample
import com.pocketstudio.model.PadVoice
import com.pocketstudio.model.Project
import com.pocketstudio.model.StudioDrums
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * Lot 2 (audit 35, amateur 2): « Kit ▾ » offered only Studio and Classique without a pack: no
 * trap or drill kit. The engine's 808 (phase 38) now comes as « Trap (808) » and « Drill (808) ».
 */
@RunWith(RobolectricTestRunner::class)
class Kit808Test {
    private val app: Application = ApplicationProvider.getApplicationContext()

    private fun vm(): RhythmViewModel {
        File(app.filesDir, "projects").deleteRecursively()
        return RhythmViewModel(app)
    }

    @Test
    fun theKitSheetOffersTrapAndDrillBuiltIn() {
        val vm = vm()
        val builtIn = vm.kitChoices(emptyList(), com.pocketstudio.app.rhythm.LibraryState.Missing).filter { it.group == KitChoice.BUILT_IN }.map { it.id }
        assertTrue(builtIn.toString(), "kit808:trap" in builtIn && "kit808:drill" in builtIn)
    }

    @Test
    fun trapPutsThe808OnItsPadInOneUndoStep() {
        val vm = vm()
        vm.session.edit { p -> p.copy(kitVersion = Project.KIT_V1, drums = p.drums.withSample(3, PadSample("clap.wav", "Clap"))) }
        val before = vm.project.value
        vm.chooseKit("kit808:trap")
        val p = vm.project.value
        assertEquals(Project.KIT_V3, p.kitVersion)
        assertEquals(PadVoice.BASS_808, p.padVoices[StudioDrums.LANE_808]?.kind)
        assertTrue("the pads' own sounds are off", p.drums.lanes.all { it.sample == null })
        assertEquals("Studio + 808", kitName(p))
        vm.undo()
        assertEquals(before.kitVersion, vm.project.value.kitVersion)
        assertNull(vm.project.value.padVoices[StudioDrums.LANE_808])
        assertEquals("Clap", vm.project.value.drums.lanes[3].sample?.name)
    }

    @Test
    fun drillsIsLongerThanTraps() {
        val vm = vm()
        vm.chooseKit("kit808:trap")
        val trap = vm.project.value.padVoices.getValue(StudioDrums.LANE_808).param(PadVoice.DECAY)!!
        vm.chooseKit("kit808:drill")
        val drill = vm.project.value.padVoices.getValue(StudioDrums.LANE_808).param(PadVoice.DECAY)!!
        assertTrue("drill $drill, trap $trap", drill > trap)
    }

    /** Review: a song that already has its « Basse 808 » track is told the kit brings a second 808. */
    @Test
    fun aSongWithAn808TrackIsTold() {
        val vm = vm()
        vm.chooseKit("kit808:trap")
        assertTrue(vm.done.value.orEmpty(), "Ta piste" !in vm.done.value.orEmpty())
        vm.addTrackOf(com.pocketstudio.model.TrackKind.Bass808)
        assertTrue(vm.project.value.tracks.any { it.engine == com.pocketstudio.model.InstrumentTrack.ENGINE_808 })
        vm.chooseKit("kit808:drill")
        assertTrue(vm.done.value.orEmpty(), vm.done.value.orEmpty().contains("Ta piste Basse 808 joue aussi"))
    }
}
