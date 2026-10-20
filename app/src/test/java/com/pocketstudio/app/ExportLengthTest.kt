package com.pocketstudio.app

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.model.currentSceneContent
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.io.File

@RunWith(RobolectricTestRunner::class)
class ExportLengthTest {
    private val app: Application = ApplicationProvider.getApplicationContext()

    @Test
    fun theSceneIsExportedOnItsOwnLengthUnlessALengthIsPicked() {
        File(app.filesDir, "projects").deleteRecursively()
        val vm = RhythmViewModel(app)
        val sceneBars = { vm.project.value.let { p -> p.currentSceneContent().stepsFor(p.stepsPerBar) / p.stepsPerBar } }
        assertEquals(sceneBars(), vm.export.value.bars)
        vm.setDrumBars(3)
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(3, sceneBars())
        assertEquals(3, vm.export.value.sceneBars)
        assertEquals(3, vm.export.value.bars)  // not the old fixed 4
        // A length picked by hand stays, whatever the scene.
        vm.setExportBars(2)
        shadowOf(Looper.getMainLooper()).idle()
        vm.setDrumBars(1)
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(2, vm.export.value.bars)
        assertEquals(sceneBars(), vm.export.value.sceneBars)
    }
}
