package com.pocketstudio.app.human

import org.junit.Assert.assertEquals
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
 * Here the network is slow on purpose: a local proxy holds the first connection [HOLD_MS] (later ones are refused at once, so the
 * app's fallbacks end fast). The fetch is still waiting when the test ends: the bench's end must wait for it, as it waits for the
 * closing saves, before the state is put back. Without that wait the app's worker is still in PackStore when end() returns.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = HumanTester.PHONE)
class BenchNetworkEndTest : HumanTester("ci2net") {
    @Before
    fun notOnTheBench() = assumeTrue("not while the bench runs seats", System.getProperty("ps.human.dir").isNullOrBlank())

    @Test
    fun theEndWaitsForTheAppsNetworkWork() {
        val held = AtomicInteger()
        val server = ServerSocket(0, 50, java.net.InetAddress.getLoopbackAddress())
        val keys = listOf("https.proxyHost", "https.proxyPort", "http.proxyHost", "http.proxyPort")
        val before = keys.associateWith { System.getProperty(it) }
        val proxy = thread(isDaemon = true, name = "ci2-slow-proxy") {
            try {
                while (true) {
                    val s = server.accept()
                    if (held.getAndIncrement() == 0) {
                        thread(isDaemon = true) { runCatching { Thread.sleep(HOLD_MS); s.close() } }
                    } else s.close()
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
            val start = centerOf("welcome_guided")
            if (start != null) act("touche ${start.first} ${start.second}")
            centerOf("mode_card_simple")?.let { act("touche ${it.first} ${it.second}") }
            // The assistant is open: its packs' catalogue is asked for (and held by the slow proxy).
            val until = System.currentTimeMillis() + 5_000
            while (held.get() == 0 && System.currentTimeMillis() < until) act("attends 0.2")
            assumeTrue("the app asked the network for nothing (no catalogue to wait for)", held.get() > 0)
            end()
            assertEquals(
                "the bench's end returned while the app's work still ran (the next test would inherit it)",
                emptyList<String>(),
                BenchPristine.appThreads().filter { "PackStore" in it || "SoundLibraryViewModel" in it },
            )
        } finally {
            before.forEach { (k, v) -> if (v == null) System.clearProperty(k) else System.setProperty(k, v) }
            server.close()
            proxy.join(1_000)
        }
    }

    private companion object {
        const val HOLD_MS = 4_000L
    }
}
