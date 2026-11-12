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
        assumeTrue(System.getProperty("ps.strict") == "on")
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

    @Test
    fun theAppsCodeReachedThroughComposeIsTheAppsAndTheTestsStraightCallIsNot() {
        val io = "java.io.FileOutputStream" to "write"
        val save = "com.pocketstudio.model.ProjectStore" to "save"
        val vm = "com.pocketstudio.app.rhythm.RhythmViewModel" to "rename"
        val compose = "androidx.compose.foundation.ClickableNode" to "onClick"
        val test = "com.pocketstudio.app.MainThreadIoTest" to "run"
        assertTrue(MainThreadIo.classify(listOf(io, save, vm, compose, test), "disk")?.site == "ProjectStore.save")
        assertTrue(MainThreadIo.classify(listOf(io, save, vm, test, compose), "disk") == null)
        assertTrue(MainThreadIo.classify(listOf(io, "org.robolectric.res.Fs" to "read", save, compose), "disk") == null)
        assertTrue(MainThreadIo.classify(listOf(io, "com.pocketstudio.model.Hit\$\$serializer" to "deserialize", save, compose), "disk")?.site == "ProjectStore.save")
        // A stack cut before any test frame: the app's.
        assertTrue(MainThreadIo.classify(listOf(io, save, vm), "disk")?.site == "ProjectStore.save")
    }
}
