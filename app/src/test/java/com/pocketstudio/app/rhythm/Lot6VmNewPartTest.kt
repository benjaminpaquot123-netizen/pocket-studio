package com.pocketstudio.app.rhythm

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/** Lot 6, U28 (seat F a #8): « + Nouvelle partie » on a project whose part A loops alone keeps A before the new part. */
@RunWith(RobolectricTestRunner::class)
class Lot6VmNewPartTest {
    private val app: Application = ApplicationProvider.getApplicationContext()

    @Test
    fun aNewPartOnAnEmptySongKeepsTheLoopingPartFirst() {
        File(app.filesDir, "projects").deleteRecursively()
        val vm = RhythmViewModel(app)
        vm.toggleHit(0, 0)
        assertEquals(emptyList<Int>(), vm.project.value.song.map { it.scene })
        vm.newSongPart(copy = true)
        val made = vm.project.value.scene
        assertEquals("A, then the new part", listOf(0, made), vm.project.value.song.map { it.scene })
        // One undo step takes both back.
        vm.undo()
        assertEquals(emptyList<Int>(), vm.project.value.song.map { it.scene })
    }
}
