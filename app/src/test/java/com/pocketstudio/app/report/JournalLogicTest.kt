package com.pocketstudio.app.report

import com.pocketstudio.engine.StreamInfo
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** The bug report's memory: bounded rings in order, the journal, the health readings and their summary. */
class JournalLogicTest {
    @Before
    fun fresh() {
        Journal.clear()
        HealthMonitor.reset()
    }

    @After
    fun back() {
        Journal.clock = System::currentTimeMillis
        Journal.clear()
        HealthMonitor.reset()
    }

    @Test
    fun theLiveCopyFollowsTheSoundEvenWithoutAGesture() {
        val clock = LiveCopyClock(intervalMs = 10_000)
        assertTrue("the first copy", clock.due(0))
        assertFalse("nothing moved", clock.due(20_000))
        // Stopped and silent: nothing new to keep.
        HealthMonitor.add(HealthSample(1, peak = 0f, playing = false))
        assertFalse(clock.due(30_000))
        // Music playing, no touch at all: the copy follows the sound…
        HealthMonitor.add(HealthSample(2, peak = 0.3f, playing = true))
        assertTrue(clock.due(31_000))
        // …at most every 10 s.
        HealthMonitor.add(HealthSample(3, peak = 0.3f, playing = true))
        assertFalse(clock.due(35_000))
        assertTrue(clock.due(41_000))
        // A gesture moves it too.
        Journal.action("■ Arrêt")
        assertTrue(clock.due(52_000))
    }

    @Test
    fun pathsDocumentsAndAddressesNeverLeaveThePhone() {
        val line = "Son illisible /storage/emulated/0/Music/Nuit d'été v2.wav: open failed (ENOENT), " +
            "copie /data/user/0/com.pocketstudio/files/projects/abc.json; content://com.android.providers.media/document/audio%3A12 " +
            "par benjamin@example.com depuis https://x.org/a?token=123"
        val clean = LogTail.redact(line)
        listOf("emulated", "Nuit", "com.pocketstudio/files", "audio%3A12", "benjamin", "token").forEach {
            assertFalse("« $it » left in: $clean", clean.contains(it))
        }
        assertTrue(clean.contains("Son illisible <chemin>: open failed (ENOENT)"))
        assertTrue(clean.contains("<document>"))
        assertTrue(clean.contains("<email>"))
        // The app's own libraries stay readable in a native crash's trace.
        val trace = "#01 pc 0004 /data/app/~~a==/com.pocketstudio-b==/lib/arm64/libpocketstudio.so (ps::Engine::process+124)"
        assertEquals(trace, LogTail.redact(trace))
        // An error is cleaned and cut like any other line.
        Journal.error("import", "pas lu : /sdcard/Download/Mes sons/kick.wav; " + "x".repeat(2000))
        val e = Journal.errors.lastOrNull()!!
        assertFalse(e.text.contains("Mes sons"))
        assertTrue(e.text.startsWith("import : pas lu : <chemin>"))
        assertEquals(Journal.MAX_TEXT, e.text.length)
    }

    @Test
    fun ringKeepsTheNewestInOrderAndNeverGrows() {
        val r = Ring<Int>(5)
        assertEquals(emptyList<Int>(), r.snapshot())
        assertNull(r.lastOrNull())
        (1..3).forEach(r::add)
        assertEquals(listOf(1, 2, 3), r.snapshot())
        (4..12).forEach(r::add)
        assertEquals(5, r.size)
        assertEquals(12L, r.total)
        assertEquals(listOf(8, 9, 10, 11, 12), r.snapshot())
        assertEquals(listOf(11, 12), r.last(2))
        assertEquals(listOf(8, 9, 10, 11, 12), r.last(50))
        assertEquals(12, r.lastOrNull())
        r.clear()
        assertEquals(0, r.size)
        r.add(99)
        assertEquals(listOf(99), r.snapshot())
    }

    @Test
    fun ringIsSafeFromSeveralThreads() {
        val r = Ring<Int>(100)
        val threads = List(4) { t -> Thread { repeat(10_000) { r.add(t) } } }
        threads.forEach(Thread::start)
        threads.forEach(Thread::join)
        assertEquals(100, r.size)
        assertEquals(40_000L, r.total)
    }

    @Test
    fun journalIsBoundedAndInOrder() {
        var t = 1_000L
        Journal.clock = { t++ }
        repeat(Journal.EVENTS + 40) { Journal.action("geste $it") }
        val events = Journal.events.snapshot()
        assertEquals(Journal.EVENTS, events.size)
        assertEquals("geste 40", events.first().text)
        assertEquals("geste ${Journal.EVENTS + 39}", events.last().text)
        assertTrue(events.zipWithNext().all { (a, b) -> a.atMs < b.atMs })
    }

    @Test
    fun screensAreNotRepeatedAndErrorsAreKeptApart() {
        Journal.screen("Mix › Mixeur")
        Journal.screen("Mix › Mixeur")
        Journal.screen("Plus › Outils")
        Journal.app("Son ouvert")
        Journal.app("Export impossible : disque plein")
        Journal.error("rapport", IllegalStateException("boum"))
        val events = Journal.events.snapshot()
        assertEquals(listOf(JournalEvent.Kind.NAV, JournalEvent.Kind.NAV, JournalEvent.Kind.APP, JournalEvent.Kind.APP, JournalEvent.Kind.ERROR), events.map { it.kind })
        val errors = Journal.errors.snapshot().map { it.text }
        assertEquals(2, errors.size)
        assertTrue(errors[0].contains("Export impossible"))
        assertTrue(errors[1].contains("IllegalStateException: boum"))
        // Redacted like AppLog: no e-mail leaves in a report.
        Journal.action("écrit à quelqu.un@example.com")
        assertTrue(Journal.events.lastOrNull()!!.text.contains("<email>"))
    }

    @Test
    fun appLogFeedsTheJournal() {
        AppLog.add("Premier plan : son ouvert")
        assertEquals("Premier plan : son ouvert", Journal.events.lastOrNull()?.text)
        assertEquals(JournalEvent.Kind.APP, Journal.events.lastOrNull()?.kind)
    }

    private fun info(peak: Float, xruns: Int = 0, restarts: Int = 0, nan: Boolean = false) =
        StreamInfo(sampleRate = 48_000, running = true, peak = peak, xruns = xruns, restarts = restarts, nonFinite = nan)

    @Test
    fun healthHoldsThePeakOverAQuarterSecond() {
        val read: (FloatArray) -> Unit = { it[0] = -14f; it[1] = -12f; it[2] = 0.9f; it[3] = 0.3f }
        assertNull(HealthMonitor.feed(info(0.2f), 0, 10_000, read))
        assertNull(HealthMonitor.feed(info(0.7f), 100, 10_100, read))
        assertNull(HealthMonitor.feed(info(0.1f), 200, 10_200, read))
        val s = HealthMonitor.feed(info(0.3f), 300, 10_300, read)
        assertNotNull(s)
        assertEquals(0.7f, s!!.peak, 1e-6f)
        assertEquals(-12f, s.shortTermLufs, 0f)
        assertEquals(-14f, s.momentaryLufs, 0f)
        assertEquals(0.9f, s.limiterGain, 0f)
        assertEquals(0.3f, s.cpu, 0f)
        // The next window starts afresh.
        assertNull(HealthMonitor.feed(info(0.05f), 400, 10_400, read))
        assertEquals(0.05f, HealthMonitor.feed(info(0.05f), 560, 10_560, read)!!.peak, 1e-6f)
        assertEquals(2, HealthMonitor.samples.size)
    }

    @Test
    fun healthKeepsOneMinuteAndJournalsUnderruns() {
        repeat(HealthMonitor.KEEP + 30) { i ->
            HealthMonitor.add(HealthSample(atMs = i * 250L, peak = 0.5f, xruns = if (i >= 100) 3 else 0, restarts = if (i >= 200) 1 else 0))
        }
        assertEquals(HealthMonitor.KEEP, HealthMonitor.samples.size)
        val texts = Journal.events.snapshot().map { it.text }
        assertEquals(1, texts.count { it.startsWith("Décrochage audio : +3") })
        assertEquals(1, texts.count { it.startsWith("Son rouvert") })
        HealthMonitor.add(HealthSample(atMs = 99_999, peak = 0f, nonFinite = true))
        assertTrue(Journal.errors.snapshot().any { it.text.contains("NaN") })
    }

    @Test
    fun aMinuteOfSoundBecomesSixtySecondsAndASparkline() {
        val now = 100_000L
        val samples = (0 until 240).map { i ->
            val at = now - 60_000 + i * 250L + 1
            // Silence for 20 s, then louder and louder, an underrun at -10 s.
            val sec = i / 4
            HealthSample(atMs = at, peak = if (sec < 20) 0f else (sec - 19) / 40f, shortTermLufs = -20f, cpu = 0.25f, xruns = if (sec >= 50) 1 else 0, playing = sec >= 20)
        }
        val seconds = HealthSummary.perSecond(samples, now)
        assertEquals(60, seconds.size)
        assertTrue(seconds.all { it.peakDb != null })
        val line = HealthSummary.sparkline(seconds)
        assertEquals(60, line.length)
        assertTrue(line.startsWith("_".repeat(20)))
        assertEquals('@', line.last())
        val marks = HealthSummary.markers(seconds)
        assertEquals(60, marks.length)
        assertEquals(1, marks.count { it == 'x' })
        assertEquals('x', marks[50])
        assertEquals('.', marks[0])
        assertEquals('>', marks[30])
        // No reading at all: blanks, never a crash.
        val empty = HealthSummary.perSecond(emptyList(), now)
        assertEquals(" ".repeat(60), HealthSummary.sparkline(empty))
    }

    @Test
    fun journalJsonIsEscaped() {
        Journal.action("dit \"bonjour\"\net part\\")
        val json = JournalJson.current(listOf("app" to "0.22"))
        assertTrue(json.startsWith("{\"app\":\"0.22\",\"events\":["))
        assertTrue(json.contains("dit \\\"bonjour\\\"\\net part\\\\"))
        assertFalse(json.contains("\n"))
        assertEquals("null", JsonText.number(Float.NaN))
    }
}
