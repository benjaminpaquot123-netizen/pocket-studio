package com.pocketstudio.app

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.live.TakeUi
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.model.LiveSlots
import com.pocketstudio.model.SongPart
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/** « Annuler » of the « Morceau écrit » line undoes the take, and only the take. */
@RunWith(RobolectricTestRunner::class)
class LiveTakeUndoTest {
    private val app: Application = ApplicationProvider.getApplicationContext()

    /** Two scenes, then a take that launches B's drums: written into the song. */
    private fun written(): RhythmViewModel {
        File(app.filesDir, "projects").deleteRecursively()
        val vm = RhythmViewModel(app)
        vm.toggleHit(0, 0)
        vm.duplicateScene()
        vm.toggleHit(2, 4)
        vm.selectScene(0)
        vm.togglePlay()
        vm.takeClockForTest = 5
        vm.togglePerformance()
        vm.takeClockForTest = 16
        vm.tickPerformance()
        vm.launchPad(SongPart.DRUMS, 1)
        vm.liveLaunchedForTest(LiveSlots.DRUMS, 1, 32)
        vm.takeClockForTest = 40
        vm.togglePerformance()
        assertTrue(vm.take.value is TakeUi.Done)
        assertTrue(vm.project.value.song.isNotEmpty())
        return vm
    }

    @Test
    fun rightAfterTheTakeAnnulerUndoesIt() {
        val vm = written()
        assertTrue(vm.canUndoPerformance())
        vm.undoPerformance()
        assertTrue(vm.project.value.song.isEmpty())
        assertEquals(TakeUi.Ready, vm.take.value)
    }

    @Test
    fun afterAnotherChangeAnnulerNoLongerTakesItBack() {
        val vm = written()
        val song = vm.project.value.song
        // A macro moved on the Live page after the take: a step of its own.
        vm.setMacro(1, 0, 0.9f)
        val moved = vm.project.value
        assertFalse(vm.canUndoPerformance())
        vm.undoPerformance()
        // Nothing undone (neither the macro nor half of something else), the line gone.
        assertEquals(moved, vm.project.value)
        assertEquals(song, vm.project.value.song)
        assertEquals(TakeUi.Ready, vm.take.value)
    }
}
