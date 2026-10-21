package com.pocketstudio.app

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.mixer.cpuBusyNext
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.model.AutoTarget
import com.pocketstudio.model.EffectType
import com.pocketstudio.model.FxPresets
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class InsertLanesTest {
    private val app: Application = ApplicationProvider.getApplicationContext()

    @Test
    fun changingAnEffectDropsTheLanesThatDroveTheOldOne() {
        File(app.filesDir, "projects").deleteRecursively()
        val vm = RhythmViewModel(app)
        val cut = AutoTarget(AutoTarget.INSERT_PARAM, 0, 1, 0)
        val volume = AutoTarget(AutoTarget.VOLUME, 0)
        vm.setInsertType(0, 1, EffectType.FILTER)
        vm.addAutomation(cut)
        vm.addAutomation(volume)
        vm.setInsertParam(0, 1, 0, 0.3f)  // same effect: the lane stays
        assertEquals(listOf(cut, volume), vm.project.value.automation.map { it.target })
        vm.setInsertType(0, 1, EffectType.CHORUS)
        assertEquals(listOf(volume), vm.project.value.automation.map { it.target })

        // A ready-made effect that changes the slot's effect does the same.
        vm.addAutomation(cut)
        val preset = FxPresets.ALL.first { it.inserts.getOrNull(1)?.type.let { t -> t != null && t != EffectType.CHORUS.ordinal } }
        vm.applyFx(0, preset.id)
        assertEquals(listOf(volume), vm.project.value.automation.map { it.target })
    }

    @Test
    fun theEighthSlotTakesAnEffectALaneAndAMove() {
        File(app.filesDir, "projects").deleteRecursively()
        val vm = RhythmViewModel(app)
        vm.setInsertType(2, 7, EffectType.FILTER)
        vm.setInsertParam(2, 7, 0, 0.3f)
        assertEquals(EffectType.FILTER, vm.project.value.mixer.channels[2].inserts[7].effect)
        assertEquals(0.3f, vm.project.value.mixer.channels[2].inserts[7].params[0], 1e-6f)
        val cut = AutoTarget(AutoTarget.INSERT_PARAM, 2, 7, 0)
        vm.addAutomation(cut)
        assertEquals(listOf(cut), vm.project.value.automation.map { it.target })
        // The master's eighth slot as well.
        vm.setInsertType(com.pocketstudio.model.Mixer.MASTER, 7, EffectType.COMPRESSOR)
        assertEquals(EffectType.COMPRESSOR, vm.project.value.mixer.master[7].effect)
        // Moved up one: the lane follows it to slot 6.
        vm.moveInsert(2, 7, 6)
        assertEquals(EffectType.FILTER, vm.project.value.mixer.channels[2].inserts[6].effect)
        assertEquals(listOf(cut.copy(slot = 6)), vm.project.value.automation.map { it.target })
    }

    @Test
    fun theBusyWarningDoesNotBlink() {
        assertEquals(false, cpuBusyNext(false, 0.79f))
        assertEquals(true, cpuBusyNext(false, 0.8f))
        assertEquals(true, cpuBusyNext(true, 0.65f))  // stays on between 60 and 80 %
        assertEquals(false, cpuBusyNext(true, 0.6f))
        assertEquals(false, cpuBusyNext(false, 0.7f))
    }
}
