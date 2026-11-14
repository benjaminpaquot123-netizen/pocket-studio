package com.pocketstudio.app.rhythm

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.model.Project
import com.pocketstudio.model.ProjectStore
import com.pocketstudio.model.Remix
import com.pocketstudio.model.StemMix
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.io.File

/**
 * Benjamin (2026-11-04, a remix in a project): « Muet » and « Seule » lit together on every stem.
 * Muted and heard alone at once means nothing: on a track or a stem, lighting one puts the other out,
 * on every screen (they all go through these toggles, MuteSoloEverywhereTest).
 */
@RunWith(RobolectricTestRunner::class)
class MuteSoloExclusiveTest {
    private val app: Application get() = ApplicationProvider.getApplicationContext()

    private fun open(): RhythmViewModel {
        val id = "mutesolo-${System.nanoTime()}"
        ProjectStore(File(app.filesDir, "projects")).save(id, Project(remix = Remix(setId = "s", stems = List(4) { StemMix() })))
        com.pocketstudio.app.projects.CurrentProject.set(app, id)
        val vm = RhythmViewModel(app)
        val end = System.currentTimeMillis() + 30_000
        while (vm.project.value.remix.setId != "s") {
            shadowOf(Looper.getMainLooper()).idle()
            check(System.currentTimeMillis() < end) { "the project never opened" }
            Thread.sleep(10)
        }
        return vm
    }

    private fun RhythmViewModel.channel(ch: Int) = project.value.mixer.channels[ch]
    private fun RhythmViewModel.stem(i: Int) = project.value.remix.stems[i]

    @Test
    fun aTrackIsNeverMutedAndAloneAtOnce() {
        val vm = open()
        vm.toggleMute(0)
        assertTrue(vm.channel(0).mute)
        vm.toggleSolo(0)
        assertTrue(vm.channel(0).solo)
        assertFalse("solo left the mute on", vm.channel(0).mute)
        vm.toggleMute(0)
        assertTrue(vm.channel(0).mute)
        assertFalse("mute left the solo on", vm.channel(0).solo)
        vm.toggleMute(0)
        assertFalse(vm.channel(0).mute || vm.channel(0).solo)
    }

    @Test
    fun aStemIsNeverMutedAndAloneAtOnce() {
        val vm = open()
        vm.toggleStemMute(3)
        vm.toggleStemSolo(3)
        assertTrue(vm.stem(3).solo)
        assertFalse("solo left the mute on", vm.stem(3).mute)
        vm.toggleStemMute(3)
        assertTrue(vm.stem(3).mute)
        assertFalse("mute left the solo on", vm.stem(3).solo)
        // The other stems are left alone.
        assertFalse(vm.stem(0).mute || vm.stem(0).solo)
    }
}
