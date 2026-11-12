package com.pocketstudio.app.rhythm

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.live.TakeUi
import com.pocketstudio.model.LiveSlots
import com.pocketstudio.model.SongPart
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/** Lot 6, U11 (seat E a #38): « Fin » of a Live take stops the music once the song is written. */
@RunWith(RobolectricTestRunner::class)
class Lot6VmTakeEndTest {
    private val app: Application = ApplicationProvider.getApplicationContext()

    @Test
    fun endingATakeStopsThePlayback() {
        File(app.filesDir, "projects").deleteRecursively()
        val vm = RhythmViewModel(app)
        vm.toggleHit(0, 0)
        vm.duplicateScene()
        vm.toggleHit(2, 4)
        vm.selectScene(0)
        listOf(0, 1).forEach(vm::addSongPart)
        vm.togglePlay()
        assertTrue(vm.transport.value.playing)
        vm.takeClockForTest = 5
        vm.togglePerformance()
        vm.takeClockForTest = 16
        vm.tickPerformance()
        vm.launchPad(SongPart.DRUMS, 1)
        vm.liveLaunchedForTest(LiveSlots.DRUMS, 1, 32)
        vm.takeClockForTest = 40
        vm.togglePerformance()
        assertTrue(vm.take.value is TakeUi.Done)
        assertFalse("the music goes on after « Fin »", vm.transport.value.playing)
    }
}
