package com.pocketstudio.app

import android.app.Application
import androidx.test.core.app.ApplicationProvider
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
}
