package com.pocketstudio.app

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.rhythm.RhythmViewModel
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * Tour 5, moyens M2: during a take (●) on the song, « ▶ cette partie » looped the part instead of stopping; only
 * « ▶ le morceau » stopped. For a beginner both ▶ stop a take, as the ■ they show says.
 */
@RunWith(RobolectricTestRunner::class)
class PartPlayDuringTakeTest {
    private val app: Application = ApplicationProvider.getApplicationContext()

    /** Two scenes and the song A B, the song playing (not a part looped). */
    private fun songPlaying(): RhythmViewModel {
        File(app.filesDir, "projects").deleteRecursively()
        val vm = RhythmViewModel(app)
        vm.toggleHit(0, 0)
        vm.duplicateScene()
        vm.selectScene(0)
        listOf(0, 1).forEach(vm::addSongPart)
        vm.toggleWholeSong()
        assertTrue("the song plays", vm.transport.value.playing)
        assertFalse("not a part looped", vm.loopPart.value)
        return vm
    }

    @Test
    fun duringATakeOnTheSongThePartsPlayStops() {
        val vm = songPlaying()
        vm.toggleRecord()
        assertTrue("a take", vm.recording.value)
        vm.togglePart()
        assertFalse("« ▶ cette partie » stops the take's playback", vm.transport.value.playing)
        assertFalse("the part is not looped", vm.loopPart.value)
    }

    @Test
    fun withoutATakeThePartsPlayStillLoopsThePart() {
        val vm = songPlaying()
        vm.togglePart()
        assertTrue(vm.transport.value.playing)
        assertTrue("2B kept: the part open takes over, looped", vm.loopPart.value)
    }
}
