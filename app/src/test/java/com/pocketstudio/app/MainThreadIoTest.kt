package com.pocketstudio.app

import com.pocketstudio.model.Project
import com.pocketstudio.model.ProjectStore
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/** The StrictMode watch of the tests ([MainThreadIo]) sees the app's disk use on the main thread, and only that. */
@RunWith(RobolectricTestRunner::class)
class MainThreadIoTest {
    @Test
    fun theAppsOwnSaveOnTheMainThreadIsSeenTheTestsOwnFilesAreNot() {
        assumeTrue(System.getProperty("ps.strict") != "off")
        assertTrue("the watch runs (${MainThreadIo.startError})", MainThreadIo.watching)
        val dir = File(System.getProperty("java.io.tmpdir"), "strict-" + System.nanoTime())
        // The test's own file: not the app's.
        File(dir, "mine.txt").apply { parentFile!!.mkdirs() }.writeText("x")
        // The app's save, on the main thread (called straight by this test: counted only for this test of the watch).
        MainThreadIo.excuseDirectCalls = false
        val seen = try {
            ProjectStore(dir).save("p", Project())
            // Off the main thread: not counted.
            Thread { ProjectStore(dir).save("q", Project()) }.apply { start(); join() }
            MainThreadIo.seenAndForget()
        } finally {
            MainThreadIo.excuseDirectCalls = true
        }
        // A test's own straight call is not the app's.
        ProjectStore(dir).save("r", Project())
        val after = MainThreadIo.seenAndForget()
        assertTrue("a test's straight call: $after", after.isEmpty())
        dir.deleteRecursively()
        assertTrue("the save is seen: $seen; raw: ${MainThreadIo.lastRaw.take(12).joinToString("\n")}", seen.any { it.site == "ProjectStore.save" && it.kind == "disk" })
        assertTrue("only the app's: $seen", seen.all { it.site.startsWith("ProjectStore.") })
    }
}
