package com.pocketstudio.app.remix

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.WhatPlaysTest
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.model.Harmony
import com.pocketstudio.model.Project
import com.pocketstudio.model.ProjectStore
import com.pocketstudio.model.Scale
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.io.File
import java.time.Duration

/**
 * Remix lot 1 (Sarah e/77 e/89, Mehdi f §3): « Jouer avec le projet » turned a jump up at 174 BPM in
 * F minor into a song at 120 BPM in A minor, without a word. A project with music of its own keeps
 * its tempo and key: the song follows the project's tempo, its pitch kept. An empty one takes the song's.
 */
@RunWith(RobolectricTestRunner::class)
class RemixKeepsTheSongTest {
    private val app: Application = ApplicationProvider.getApplicationContext()
    private val store = ProjectStore(File(app.filesDir, "projects"))

    @After
    fun tearDown() {
        com.pocketstudio.app.projects.CurrentProject.set(app, RhythmViewModel.PROJECT_ID)
    }

    private fun open(p: Project): RhythmViewModel {
        val id = "remix-keeps-${System.nanoTime()}"
        store.save(id, p.copy(name = id))
        com.pocketstudio.app.projects.CurrentProject.set(app, id)
        val vm = RhythmViewModel(app)
        val until = System.currentTimeMillis() + 60_000
        while (vm.project.value.name != id) {
            assertTrue("the project never opened", System.currentTimeMillis() < until)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(50))
        }
        return vm
    }

    /** « Ma chanson préférée »: 120 BPM, A minor. */
    private val song = StemSet(File(app.filesDir, "stems/chanson"), "Ma chanson préférée", 40.0, 1L, SongInfo(bpm = 120.0, firstBeat = 0.1, root = 9, minor = true))

    @Test
    fun aSongAddedToAJumpUpFollowsItsTempoAndLeavesItsKey() {
        val made = WhatPlaysTest.wizard("jump_up")
        val vm = open(made)
        val tempo = vm.project.value.tempoBpm
        val key = vm.project.value.harmony
        assertEquals(174.0, tempo, 0.5)
        assertFalse("the project took the song's tempo", vm.useSong(song))
        val p = vm.project.value
        assertEquals(tempo, p.tempoBpm, 0.0)
        assertEquals(key, p.harmony)
        assertEquals(song.id, p.remix.setId)
        // The song is stretched to the project's tempo, its pitch kept (the switch the testers had to find).
        assertTrue(p.remix.followTempo)
        assertEquals(120.0 / tempo, p.remix.stretchFor(p.tempoBpm), 1e-9)
        // One undo step gives the project back as it was.
        vm.undo()
        assertEquals(null, vm.project.value.remix.setId)
    }

    @Test
    fun anEmptyProjectTakesTheSongsTempoAndKey() {
        val vm = open(Project())
        assertTrue(vm.useSong(song))
        val p = vm.project.value
        assertEquals(120.0, p.tempoBpm, 0.0)
        assertEquals(Harmony(9, Scale.Minor), p.harmony)
        // Step 4e: the song set the tempo (« Revenir au tempo de … ») and follows it from then on.
        assertTrue(p.remix.followTempo)
        assertEquals(1.0, p.remix.stretchFor(p.tempoBpm), 0.0)
        assertEquals(song.name, p.tempoLead?.name)
    }
}
