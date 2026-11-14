package com.pocketstudio.app.rhythm

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.WhatPlaysTest
import com.pocketstudio.app.remix.SongInfo
import com.pocketstudio.app.remix.StemSet
import com.pocketstudio.model.Project
import com.pocketstudio.model.ProjectStore
import com.pocketstudio.model.remixOverrunSeconds
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.io.File
import java.time.Duration

/** Lot 6, R4 (seats B a #38, G a #12): a song longer than the project is said when it is chosen, not only in « Partager ». */
@RunWith(RobolectricTestRunner::class)
class Lot6VmSongOverrunTest {
    private val app: Application = ApplicationProvider.getApplicationContext()
    private val store = ProjectStore(File(app.filesDir, "projects"))

    @After
    fun tearDown() {
        com.pocketstudio.app.projects.CurrentProject.set(app, RhythmViewModel.PROJECT_ID)
    }

    private fun open(p: Project): RhythmViewModel {
        val id = "lot6-overrun-${System.nanoTime()}"
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

    @Test
    fun aSongLongerThanTheProjectSaysItsEndWillBeCut() {
        val vm = open(WhatPlaysTest.wizard("jump_up"))
        val song = StemSet(File(app.filesDir, "stems/longue"), "Chanson longue", 600.0, 1L, SongInfo(bpm = 120.0, firstBeat = 0.1))
        vm.useSong(song)
        assertTrue("the song does overrun the project", vm.project.value.remixOverrunSeconds() > 0)
        val said = vm.notice.value
        assertTrue("nothing said the song's end will be cut: $said", said != null && "sa fin sera coupée" in said)
    }
}
