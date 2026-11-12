package com.pocketstudio.app.rhythm

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.model.switchScene
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/** Lot 6, U3 (seat A c, screens 14, 20, 24): the part shown following the song is no change of the song exported. */
@RunWith(RobolectricTestRunner::class)
class Lot6VmExportStaleTest {
    private val app: Application = ApplicationProvider.getApplicationContext()

    @Test
    fun theSceneShownAloneNeverMakesTheSongsExportStale() {
        File(app.filesDir, "projects").deleteRecursively()
        val vm = RhythmViewModel(app)
        vm.toggleHit(0, 0)
        vm.duplicateScene()
        vm.toggleHit(0, 4)
        vm.selectScene(0)
        val loop = vm.project.value
        vm.addSongPart(0)
        vm.addSongPart(1)
        val p = vm.project.value
        val made = ExportUi(files = listOf(File(app.filesDir, "song.m4a")), madeFrom = ExportUi.madeHash(p))
        assertFalse("« Le morceau a changé » while only the part shown changed", made.staleFor(p.switchScene(1)))
        val edited = p.switchScene(1).let { q -> q.copy(drums = q.drums.copy(lanes = q.drums.lanes.mapIndexed { i, l -> if (i == 0) l.copy(hits = l.hits - 4) else l })) }
        assertTrue("a beat changed in another part still is", made.staleFor(edited))
        assertTrue("a tempo changed still is", made.staleFor(p.copy(tempoBpm = 90.0)))
        // Without a song, the loop shared is the part shown: another one is another file.
        val loopMade = ExportUi(files = listOf(File(app.filesDir, "loop.m4a")), madeFrom = ExportUi.madeHash(loop))
        assertTrue(loopMade.staleFor(loop.switchScene(1)))
    }
}
