package com.pocketstudio.app.rhythm

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.export.SharePrefs
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.io.File

/** Lot 6, U5: the format chosen in the Mode simple's « Partager » (M4A / WAV) is the Mix's (Pro) too. */
@RunWith(RobolectricTestRunner::class)
class Lot6VmExportFormatTest {
    private val app: Application = ApplicationProvider.getApplicationContext()

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    @Test
    fun theFormatChosenInSimpleIsKeptInPro() {
        File(app.filesDir, "projects").deleteRecursively()
        app.getSharedPreferences(SharePrefs.PREFS, 0).edit().clear().commit()
        SharePrefs(app).choose(ExportFormat.M4a)
        val vm = RhythmViewModel(app)
        idle()
        assertEquals("chosen before the Mix opened", ExportFormat.M4a, vm.export.value.format)
        SharePrefs(app).choose(ExportFormat.Wav)
        idle()
        assertEquals(ExportFormat.Wav, vm.export.value.format)
        SharePrefs(app).choose(ExportFormat.M4a)
        idle()
        assertEquals(ExportFormat.M4a, vm.export.value.format)
        // The Mix's own choice (FLAC) stays until another one is made in Simple.
        vm.setExportFormat(ExportFormat.Flac)
        idle()
        assertEquals(ExportFormat.Flac, vm.export.value.format)
    }
}
