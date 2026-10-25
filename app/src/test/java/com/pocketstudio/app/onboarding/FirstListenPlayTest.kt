package com.pocketstudio.app.onboarding

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.model.FirstListen
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * Decision 2A (S6): « Écouter » on « Ton morceau est prêt ! » plays the song from about 8 bars
 * before its first Drop, on a part's start; « Depuis le début » from its first bar.
 */
@RunWith(RobolectricTestRunner::class)
class FirstListenPlayTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()

    /** Twelve one-bar parts of A, then B (« Montée »), then C (« Drop »): the Drop 13 bars in. */
    private fun vmWithLateDrop(): RhythmViewModel {
        File(app.filesDir, "projects").deleteRecursively()
        val vm = RhythmViewModel(app)
        vm.toggleHit(0, 0)
        vm.duplicateScene()
        vm.toggleHit(0, 4)
        vm.duplicateScene()
        vm.toggleHit(0, 8)
        vm.renameScene(1, "Montée")
        vm.renameScene(2, "Drop")
        vm.selectScene(0)
        repeat(12) { vm.addSongPart(0) }
        vm.addSongPart(1)
        vm.addSongPart(2)
        return vm
    }

    @Test
    fun listenStartsEightBarsBeforeTheDropAndFromTheStartIsOneTouchAway() {
        val vm = vmWithLateDrop()
        val p = vm.project.value
        assertEquals(13, FirstListen.firstMain(p))
        assertEquals(5, FirstListen.startPart(p))
        vm.playStartsForTest = mutableListOf()
        vm.listenFromDrop()
        // Without the engine nothing plays here: the start point is where « Écouter » asked.
        assertEquals(5, vm.playFrom.value)
        vm.listenFromStart()
        assertEquals(0, vm.playFrom.value)
        assertEquals(listOf("listen 5", "song 0"), vm.playStartsForTest)
    }

    @Test
    fun aSongWithoutDropListensFromItsStart() {
        File(app.filesDir, "projects").deleteRecursively()
        val vm = RhythmViewModel(app)
        vm.toggleHit(0, 0)
        repeat(3) { vm.addSongPart(0) }
        vm.playStartsForTest = mutableListOf()
        vm.listenFromDrop()
        assertEquals(0, vm.playFrom.value)
        assertEquals(listOf("listen 0", "song 0"), vm.playStartsForTest)
    }
}
