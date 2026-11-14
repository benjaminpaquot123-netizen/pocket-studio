package com.pocketstudio.app.human

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.net.ServerSocket
import java.net.SocketException
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

/**
 * Round 6, lot ci2 (CI 43279184052, the one red test of 8547: Zone3JourneysTest#redoAfterUndoInTheModeSimple, « leaves to the test
 * after it: coroutine workers = … PackStore.fetchCatalogue »): the assistant asks the packs' catalogue from GitHub when it opens, on a
 * coroutine worker. The bench's end let a worker « waiting for the network » run on (its rest ignores sockets), so a short test whose
 * fetch was still on the network when it ended was red, or not, by the CI's network speed.
 *
 * Here the network is slow on purpose: a local proxy holds the catalogue's connection (github.com) [HOLD_MS] and refuses every other
 * one at once (the update check of a preview or stable build, the app's fallbacks). The fetch is still waiting when the test ends: the bench's end must wait for it, as it waits for the
 * closing saves, before the state is put back. Without that wait the app's worker is still in PackStore when end() returns.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = HumanTester.PHONE)
class BenchNetworkEndTest : HumanTester("ci2net") {
    @Before
    fun notOnTheBench() = assumeTrue("not while the bench runs seats", System.getProperty("ps.human.dir").isNullOrBlank())

    private fun tap(tag: String) {
        val at = centerOf(tag)
        assertNotNull("nothing to touch: $tag\n" + screenLines().joinToString("\n"), at)
        act("touche ${at!!.first} ${at.second}")
    }

    @Test
    fun theEndWaitsForTheAppsNetworkWork() = endWhileTheCatalogueIsHeld(gapMs = null)

    /**
     * Tour 6, lot « stable » (CI 43498223553: « after 9800 ms … VerifiedDownload.fetch:39 < PackStore.fetchIndex:74 »): the
     * catalogue's fetch failed when the proxy let go, and the same job went on to its fallbacks (the saved copy on disk, then
     * the pinned index, on the network again). For a moment it was off the network: the bench's end, sampling every 20 ms,
     * took that moment for « other work » and returned, the job still running. On a loaded runner the moment is longer, so
     * the end lost the race about one run in two. Here that moment is made long on purpose: the saved copy is a pipe the
     * test writes [GAP_MS] after the hold. The end must wait for the whole job, through its step off the network.
     */
    @Test
    fun theEndWaitsThroughAStepOffTheNetworkBetweenTwoCalls() = endWhileTheCatalogueIsHeld(gapMs = GAP_MS)

    private fun endWhileTheCatalogueIsHeld(gapMs: Long?) {
        val held = AtomicInteger()
        val server = ServerSocket(0, 50, java.net.InetAddress.getLoopbackAddress())
        val keys = listOf("https.proxyHost", "https.proxyPort", "http.proxyHost", "http.proxyPort")
        val before = keys.associateWith { System.getProperty(it) }
        // The saved catalogue (PackStore.fetchIndex's first fallback) as a named pipe: reading it waits for the test's writer.
        val pipe = java.util.concurrent.atomic.AtomicReference<java.io.File?>()
        val writer = java.util.concurrent.atomic.AtomicReference<Thread?>()
        val proxy = thread(isDaemon = true, name = "ci2-slow-proxy") {
            try {
                while (true) {
                    val s = server.accept()
                    thread(isDaemon = true) {
                        runCatching {
                            // « CONNECT github.com:443 HTTP/1.1 »: only the packs' catalogue (github.com) is held, once; every other
                            // connection (the update check's api.github.com on a preview or stable build, the fallbacks) is refused.
                            val line = s.getInputStream().bufferedReader().readLine().orEmpty()
                            val first = line.startsWith("CONNECT github.com:") && held.getAndIncrement() == 0
                            if (first) Thread.sleep(HOLD_MS)
                            s.close()
                            val p = pipe.get()
                            if (first && p != null && gapMs != null) {
                                writer.set(thread(isDaemon = true, name = "ci2-saved-catalogue") {
                                    Thread.sleep(gapMs)
                                    runCatching { java.io.FileOutputStream(p).use { it.write("{}".toByteArray()) } }
                                })
                            }
                        }
                    }
                }
            } catch (_: SocketException) {
                // the server closed: the test is over
            }
        }
        try {
            for (scheme in listOf("https", "http")) {
                System.setProperty("$scheme.proxyHost", "127.0.0.1")
                System.setProperty("$scheme.proxyPort", server.localPort.toString())
            }
            begin(SeatOptions(simple = true))
            if (gapMs != null) {
                val app = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.app.Application>()
                val p = java.io.File(com.pocketstudio.app.library.PackStore.dir(app), "catalogue.json")
                p.parentFile!!.mkdirs()
                assertEquals("mkfifo", 0, ProcessBuilder("mkfifo", p.path).start().waitFor())
                pipe.set(p)
            }
            tap("welcome_guided")
            if (centerOf("mode_card_simple") != null) tap("mode_card_simple")
            // The assistant is open: its packs' catalogue is asked for (and held by the slow proxy).
            val until = System.currentTimeMillis() + 5_000
            while (held.get() == 0 && System.currentTimeMillis() < until) act("attends 0.2")
            assertTrue("the assistant asked GitHub for its packs' catalogue", held.get() > 0)
            val t0 = System.currentTimeMillis()
            end()
            // Without the bench's wait, end() is back long before the proxy lets go (HOLD_MS): the fetch is still running.
            assertEquals(
                "the bench's end returned while the app's work still ran (the next test would inherit it), after ${System.currentTimeMillis() - t0} ms",
                emptyList<String>(),
                BenchPristine.appThreads(),
            )
            if (gapMs != null) {
                // Review M7: the writer's last steps (closing the pipe) may still run after the job has read it; its end is
                // waited for (bounded: a writer that no reader ever opened stays blocked, and that is the red).
                writer.get()?.join(WRITER_END_CAP_MS)
                assertTrue("the job read its saved copy (the step off the network happened)", writer.get()?.isAlive == false)
            }
        } finally {
            before.forEach { (k, v) -> if (v == null) System.clearProperty(k) else System.setProperty(k, v) }
            server.close()
            proxy.join(1_000)
            // A writer still waiting for a reader (the job never came to its saved copy): let it go.
            if (writer.get()?.isAlive == true) pipe.get()?.let { p -> runCatching { java.io.FileInputStream(p).close() } }
        }
    }

    private companion object {
        const val HOLD_MS = 10_000L
        /** How long the job stays off the network between the catalogue and its fallbacks (the saved copy, a pipe). */
        const val GAP_MS = 3_000L
        /** How long a writer whose pipe was read is given to close it. */
        const val WRITER_END_CAP_MS = 5_000L
    }
}
