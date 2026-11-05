package com.pocketstudio.app

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.model.allScenes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * Relecture tour 1, M5: « + Copie » of the song (newSongPart(copy)) took any empty scene but the
 * one shown: one the song already played (an empty part made with « + Vide ») was overwritten,
 * the song then played the copy twice. Now a copy, like a new scene, goes on a scene really free.
 */
@RunWith(RobolectricTestRunner::class)
class NewSongPartSceneTest {
    private val app: Application = ApplicationProvider.getApplicationContext()

    @Test
    fun aCopiedPartNeverTakesAnEmptySceneTheSongPlays() {
        File(app.filesDir, "projects").deleteRecursively()
        val vm = RhythmViewModel(app)
        vm.toggleHit(0, 0)
        vm.newSongPart(copy = false)
        val empty = vm.project.value.scene
        assertEquals(listOf(empty), vm.project.value.song.map { it.scene })
        vm.selectScene(0)
        vm.newSongPart(copy = true)
        val p = vm.project.value
        assertEquals(2, p.song.size)
        val copy = p.song.last().scene
        assertTrue("the copy took scene $copy, the song's empty part is $empty", copy != empty)
        assertTrue("the empty part stays empty", p.allScenes[empty].isEmpty)
        assertEquals("the copy is scene A's", p.allScenes[0].drums.lanes[0].hits.keys, p.allScenes[copy].drums.lanes[0].hits.keys)
    }
}
