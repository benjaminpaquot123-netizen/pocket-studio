package com.pocketstudio.app.library

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.model.Project
import com.pocketstudio.model.StarterWizard
import com.pocketstudio.model.StarterWizard.Role
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/** The assistant's ride (the kit cymbal made short) and its rumble (in the bass line's place). */
@RunWith(RobolectricTestRunner::class)
class WizardRideTest {
    private val app: Application = ApplicationProvider.getApplicationContext()

    private fun techno(): SoundLibraryViewModel {
        val vm = SoundLibraryViewModel(app)
        // Every kit voice as if heard before (no audio engine here).
        for (v in 0 until 16) vm.kitCache[v] = Pcm(FloatArray(960), 2, 48000)
        vm.openWizard()
        vm.chooseStyle("techno")
        vm.setMood(StarterWizard.Mood.Dark)
        vm.stopPreview()
        return vm
    }

    @Test
    fun theRideIsTheKitCymbalShortHighAndSoftOnItsPad() {
        val vm = techno()
        vm.setRowEnabled(Role.Ride, true)
        var made: Pair<Project, Map<String, Pcm>>? = null
        vm.create { p, files, _ -> made = p to files }
        shadowOf(Looper.getMainLooper()).idle()
        assertNotNull(made)
        val (project, files) = made!!
        val ride = project.drums.lanes[Role.Ride.lane].sample
        assertNotNull(ride)
        assertEquals(StarterWizard.RIDE_NAME, ride!!.name)
        assertTrue(ride.pitch > 0f && ride.sustain == 0f && ride.decay < 0.5f && ride.gain < 1f)
        assertTrue(ride.file in files)
        // The other kit voices already on their pad stay the kit's own (no file).
        assertEquals(null, project.drums.lanes[Role.Kick.lane].sample)
    }

    @Test
    fun tickingTheRumbleTakesTheBassLineOut() {
        val vm = techno()
        assertTrue(vm.wizard.value.parts.getValue(StarterWizard.Part.Bass).enabled)
        vm.setRowEnabled(Role.Rumble, true)
        assertFalse(vm.wizard.value.parts.getValue(StarterWizard.Part.Bass).enabled)
        // It can come back by hand; unticking the rumble leaves the bass as it is.
        vm.setPartEnabled(StarterWizard.Part.Bass, true)
        vm.setRowEnabled(Role.Rumble, false)
        assertTrue(vm.wizard.value.parts.getValue(StarterWizard.Part.Bass).enabled)
    }
}
