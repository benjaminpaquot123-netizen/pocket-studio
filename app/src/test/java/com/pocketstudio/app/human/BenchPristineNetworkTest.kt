package com.pocketstudio.app.human

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.model.Statement
import org.robolectric.RobolectricTestRunner
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ProxySelector
import java.net.URI
import java.net.URL

/**
 * Tour 9, lot ci-fiab (CI 449, shard 5: Tour5BloquantsTest#theTutorialsSwingStepShowsTheSwingSetting « leaves to the test after it:
 * coroutine workers = … VerifiedDownload.fetch:39 < PackStore.fetchIndex:74 < SoundLibraryViewModel.refreshPacks », then the two
 * tests after it red on the inherited worker): a bench test that opens the assistant or a tutorial leaves the packs' catalogue to
 * the real network (GitHub). On a runner whose network hangs, that job waits up to 10 + 15 s (catalogue) then 15 + 30 s (pinned
 * index): longer than the 30 s the bench's end waits for the app's network work, so the worker outlives the test.
 *
 * A bench test has no business on a remote host: under the rule, every connection to one is refused at once (a closed local
 * port), so the app's network jobs end in milliseconds whatever the runner's network does. Spared: loopback hosts (the tests'
 * own local servers), a test that sets its own proxy while it runs ([BenchNetworkEndTest]), and a real seat of the bench.
 */
@RunWith(RobolectricTestRunner::class)
class BenchPristineNetworkTest {
    /** What [inside] returns, run as a test of [test] named [method] under [rule]. */
    private fun <T> under(rule: BenchPristineRule = BenchPristineRule(), test: Class<*> = BenchPristineNetworkTest::class.java, method: String = "aTest", inside: () -> T): T {
        var out: Result<T>? = null
        rule.apply(object : Statement() { override fun evaluate() { out = runCatching(inside) } }, Description.createTestDescription(test, method)).evaluate()
        return out!!.getOrThrow()
    }

    private fun isClosedLocalPort(proxies: List<Proxy>): Boolean {
        val at = proxies.singleOrNull()?.takeIf { it.type() == Proxy.Type.HTTP }?.address() as? InetSocketAddress ?: return false
        return (at.address?.isLoopbackAddress == true || at.hostString == "127.0.0.1") && at.port == 9
    }

    @Test
    fun aTestUnderTheRuleNeverReachesARemoteHost() {
        val before = ProxySelector.getDefault()
        for (remote in listOf("https://github.com/benjaminpaquot123-netizen/pocket-studio/releases/download/packs/catalogue.json", "http://example.org/", "https://api.github.com/")) {
            val route = under { ProxySelector.getDefault().select(URI(remote)) }
            assertTrue("$remote is refused at once under the rule (a closed local port), not left to the runner's network: $route", isClosedLocalPort(route))
        }
        // As the app asks (PackStore.fetchCatalogue): over at once, whatever the network does.
        val (error, ms) = under {
            val t0 = System.currentTimeMillis()
            val e = runCatching {
                (URL("http://pocket-studio.invalid/catalogue.json").openConnection() as HttpURLConnection).apply {
                    connectTimeout = 10_000
                    readTimeout = 15_000
                }.responseCode
            }.exceptionOrNull()
            e to System.currentTimeMillis() - t0
        }
        assertTrue("the app's fetch fails as offline: $error", error is java.net.ConnectException)
        assertTrue("and at once: $ms ms", ms < 3_000)
        assertSame("the routes of before are back after the test", before, ProxySelector.getDefault())
    }

    @Test
    fun theTestsOwnLocalServersAreStillReached() {
        val before = ProxySelector.getDefault()
        for (local in listOf("http://127.0.0.1:8123/model.bin", "http://localhost:8123/", "http://[::1]:8123/")) {
            assertEquals("$local keeps its route", before.select(URI(local)), under { ProxySelector.getDefault().select(URI(local)) })
        }
    }

    /** [BenchNetworkEndTest] holds the catalogue's connection in its own slow proxy, set while the test runs: it must still get it. */
    @Test
    fun aProxySetByTheTestItselfIsKept() {
        val keys = listOf("https.proxyHost", "https.proxyPort")
        val saved = keys.associateWith { System.getProperty(it) }
        try {
            val route = under {
                System.setProperty("https.proxyHost", "127.0.0.1")
                System.setProperty("https.proxyPort", "45678")
                ProxySelector.getDefault().select(URI("https://github.com/"))
            }
            val at = route.single().address() as InetSocketAddress
            assertEquals("the test's own proxy: $route", 45678, at.port)
        } finally {
            saved.forEach { (k, v) -> if (v == null) System.clearProperty(k) else System.setProperty(k, v) }
        }
    }

    /** A real seat plays the app as a phone has it, network included: nothing is cut for it. */
    @Test
    fun aRealSeatKeepsItsNetwork() {
        val lot = kotlin.io.path.createTempDirectory("lot").toFile()
        java.io.File(lot, "a").mkdirs()
        java.io.File(lot, "a/start.txt").writeText("lang=fr\n")
        val dir = System.getProperty("ps.human.dir")
        System.setProperty("ps.human.dir", lot.path)
        try {
            val before = ProxySelector.getDefault()
            val uri = URI("https://github.com/")
            assertEquals(before.select(uri), under(HumanTesterA().pristine, HumanTesterA::class.java, "drive") { ProxySelector.getDefault().select(uri) })
        } finally {
            if (dir == null) System.clearProperty("ps.human.dir") else System.setProperty("ps.human.dir", dir)
            lot.deleteRecursively()
        }
    }
}
