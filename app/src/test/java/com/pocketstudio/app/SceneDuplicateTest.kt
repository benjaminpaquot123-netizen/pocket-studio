package com.pocketstudio.app

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.model.Scene
import com.pocketstudio.model.allScenes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class SceneDuplicateTest {
    private val app: Application = ApplicationProvider.getApplicationContext()

    private fun freshVm(): RhythmViewModel {
        File(app.filesDir, "projects").deleteRecursively()
        return RhythmViewModel(app)
    }

    @Test
    fun duplicatingASceneIsOneUndoStep() {
        val vm = freshVm()
        vm.toggleHit(0, 0)
        vm.duplicateScene()
        assertEquals(1, vm.project.value.scene)
        assertEquals(2, vm.project.value.allScenes.size)
        vm.undo()
        // One undo: back on A, and the empty copy's slot is gone.
        assertEquals(0, vm.project.value.scene)
        assertEquals(1, vm.project.value.allScenes.size)
    }

    @Test
    fun aFullGridSaysSo() {
        val vm = freshVm()
        vm.toggleHit(0, 0)
        repeat(Scene.MAX - 1) { vm.duplicateScene() }
        assertEquals(Scene.MAX, vm.project.value.allScenes.size)
        vm.dismissNotice()
        assertNull(vm.notice.value)
        vm.duplicateScene()
        assertNotNull(vm.notice.value)
        assertEquals(Scene.MAX, vm.project.value.allScenes.size)
    }
}
