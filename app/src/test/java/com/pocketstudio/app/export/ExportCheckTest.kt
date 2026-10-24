package com.pocketstudio.app.export

import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.log10
import kotlin.math.sin

/** Phase 38: the true peak of what a player rebuilds, the M4A that never saturates once decoded, the plain-words export report. */
class ExportCheckTest {
    private fun peakOf(channels: Int, samples: FloatArray): Double {
        val m = TruePeakMeter(channels)
        samples.forEach(m::add)
        return m.peakDb
    }

    @Test
    fun theTruePeakReadsBetweenTheSamples() {
        // A sine at a quarter of the rate, 45 degrees off: every sample at 0.707, the wave at 1.
        val n = 4800
        val quarter = FloatArray(n) { i -> sin(PI / 2 * i + PI / 4).toFloat() }
        assertEquals(20 * log10(0.7071), 20 * log10(quarter.maxOf { kotlin.math.abs(it) }.toDouble()), 0.01)
        assertEquals(0.0, peakOf(1, quarter), 0.25)
        // A slow sine: its sample peak is its true peak.
        val slow = FloatArray(n) { i -> (0.5 * sin(2 * PI * 440 * i / 48000)).toFloat() }
        assertEquals(20 * log10(0.5), peakOf(1, slow), 0.05)
        // Stereo: the louder side counts, the sides never mix.
        val stereo = FloatArray(2 * n) { i -> if (i % 2 == 0) slow[i / 2] else slow[i / 2] * 0.25f }
        assertEquals(20 * log10(0.5), peakOf(2, stereo), 0.05)
        // From the raw floats the decoder writes.
        val raw = File.createTempFile("truepeak", ".raw")
        try {
            val bytes = ByteBuffer.allocate(quarter.size * 4).order(ByteOrder.LITTLE_ENDIAN)
            quarter.forEach { bytes.putFloat(it) }
            raw.writeBytes(bytes.array())
            assertEquals(peakOf(1, quarter), TruePeakMeter.ofRawFloats(raw, 1), 1e-9)
        } finally {
            raw.delete()
        }
        assertEquals(-120.0, peakOf(2, FloatArray(100)), 0.0)
    }

    @Test
    fun anM4aOverTheLimitOnceDecodedIsEncodedAgainLower() {
        // An encoder that rebuilds the wave 1.6 dB over its source (worse than the 1.1 dB measured).
        val sourcePeakDb = -2.0
        val gains = mutableListOf<Float>()
        var lastGain = 1f
        val r = M4aGuard.encodeWithin(1f, encode = { g -> gains += g; lastGain = g }, decodedPeak = { sourcePeakDb + 1.6 + 20 * log10(lastGain.toDouble()) })
        assertEquals(2, r.tries)
        assertTrue("${r.decodedPeakDb}", r.decodedPeakDb!! <= M4aGuard.MAX_DECODED_DBTP)
        assertEquals(-1.0 - M4aGuard.MARGIN_DB, r.decodedPeakDb!!, 1e-6)
        assertTrue(gains[1] < gains[0])
        // Under the limit the first time: encoded once, untouched.
        val once = M4aGuard.encodeWithin(0.9f, encode = { lastGain = it }, decodedPeak = { -1.4 })
        assertEquals(1, once.tries)
        assertEquals(0.9f, once.gain)
        // Unreadable: kept as it is (the source was already held at −2 dBTP).
        val unread = M4aGuard.encodeWithin(1f, encode = {}, decodedPeak = { null })
        assertNull(unread.decodedPeakDb)
        assertEquals(1, unread.tries)
        // Never more than MAX_TRIES encodings, whatever the encoder does.
        var n = 0
        M4aGuard.encodeWithin(1f, encode = { n++ }, decodedPeak = { 3.0 })
        assertEquals(M4aGuard.MAX_TRIES, n)
    }

    @Test
    fun anM4aOverTheLimitIsRenderedAgainWithItsLimiterLower() = kotlinx.coroutines.runBlocking {
        // A codec that rebuilds the peaks 3.5 dB over the limiter's ceiling (the source sits at it).
        var source = AacEncoder.CEILING_DBTP.toDouble()
        var gain = 1f
        val ceilings = mutableListOf<Float>()
        val r = M4aGuard.run(
            source,
            render = { c -> ceilings += c; source = c.toDouble(); source },
            encode = { g -> gain = g },
            decodedPeak = { source + 20 * log10(gain.toDouble()) + 3.5 },
        )
        // One more render, the limiter lower by the excess (2.5 dB) and the margin: no loudness lost to a gain.
        assertEquals(2, r.renders)
        assertEquals(2, r.encodings)
        assertEquals(listOf(AacEncoder.CEILING_DBTP - 2.5f - M4aGuard.MARGIN_LIMIT_DB.toFloat()), ceilings)
        assertEquals(1f, r.gain)
        assertEquals(-1.0 - M4aGuard.MARGIN_LIMIT_DB, r.decodedPeakDb!!, 1e-4)
        // Under the limit at once: one render, one encoding.
        val once = M4aGuard.run(-2.0, render = { error("no render") }, encode = {}, decodedPeak = { -1.5 })
        assertEquals(1, once.renders)
        assertEquals(1, once.encodings)
        // A limiter that cannot take the peaks down (the new mix still peaks at −2 dBTP): its gain
        // brings the next encoding under; never more encodings than renders + MAX_TRIES.
        var g2 = 1f
        val stubborn = M4aGuard.run(-2.0, render = { -2.0 }, encode = { g -> g2 = g }, decodedPeak = { 1.0 + 20 * log10(g2.toDouble()) })
        assertTrue(stubborn.renders in 2..M4aGuard.MAX_RENDERS)
        assertTrue("${stubborn.decodedPeakDb}", stubborn.decodedPeakDb!! <= M4aGuard.MAX_DECODED_DBTP)
        assertTrue(stubborn.encodings <= M4aGuard.MAX_RENDERS + M4aGuard.MAX_TRIES)
        // Never asked under MIN_CEILING_DBTP: a huge overshoot goes to the gain at once.
        val wild = M4aGuard.run(-2.0, render = { error("no render") }, encode = { g -> g2 = g }, decodedPeak = { 9.0 + 20 * log10(g2.toDouble()) })
        assertEquals(1, wild.renders)
        assertTrue(wild.decodedPeakDb!! <= M4aGuard.MAX_DECODED_DBTP)
        // Unreadable: kept as encoded.
        assertNull(M4aGuard.run(-2.0, render = { error("no") }, encode = {}, decodedPeak = { null }).decodedPeakDb)
    }

    @Test
    fun theReportSaysItInPlainWords() {
        I18n.lang = Lang.FR
        val ok = ExportSummary(seconds = 192.4, lufs = -9.5, truePeakDb = -1.3, checkedAfterCompression = true, format = "M4A")
        assertTrue(ok.ready)
        assertEquals("Prêt à partager", ok.verdict())
        assertEquals("Durée : 3 min 12 s", ok.durationLine())
        assertEquals("Niveau : fort, comme la musique de club", ok.levelLine())
        assertEquals("Crête : sous la limite, vérifiée après la compression", ok.peakLine())
        assertEquals("M4A, 48 kHz", ok.formatLine())
        val cd = ExportSummary(48.0, -14.0, -1.1, sampleRate = 44100, bits = 16, format = "WAV")
        assertEquals("WAV, 44,1 kHz, 16 bits", cd.formatLine())
        assertEquals("Crête : sous la limite, pas de saturation", cd.peakLine())
        assertEquals("Durée : 48 s", cd.durationLine())
        val hot = ExportSummary(30.0, -7.0, -0.2, format = "WAV")
        assertFalse(hot.ready)
        assertTrue(hot.saturates)
        assertTrue(hot.verdict().startsWith("À revoir"))
        assertTrue(hot.peakLine()!!.contains("trop haute"))
        val quiet = ExportSummary(30.0, -30.0, -12.0)
        assertFalse(quiet.ready)
        assertTrue(quiet.verdict().contains("très doux"))
        // In English too.
        I18n.lang = Lang.EN
        try {
            assertEquals("Ready to share", ok.verdict())
        } finally {
            I18n.lang = Lang.FR
        }
    }

    /**
     * Lot 1: the check names each step for the bar (« Compression M4A », « Vérification »,
     * « Nouvel essai 2/3 »…) and renders the whole song again only while the export's budget
     * allows: past it, the mix is encoded a little lower instead (one more encoding, never three
     * renders and five encodings).
     */
    @Test
    fun theCheckNamesEachStepAndRendersAgainOnlyWithinItsBudget() = kotlinx.coroutines.runBlocking {
        I18n.lang = Lang.FR
        var g2 = 1f
        var source = -2.0
        // An encoder 1.5 dB over its source.
        val decoded = { source + 1.5 + 20 * log10(g2.toDouble()) }
        val steps = mutableListOf<String>()
        val late = M4aGuard.run(source, render = { error("no new render past the budget") }, encode = { g -> g2 = g }, decodedPeak = { decoded() },
            step = { steps += it.label() }, mayGoOn = { false })
        assertEquals(1, late.renders)
        assertEquals(2, late.encodings)
        assertTrue("${late.decodedPeakDb}", late.decodedPeakDb!! <= M4aGuard.MAX_DECODED_DBTP)
        assertEquals(listOf("Compression M4A", "Vérification", "Réglage du volume : compression", "Réglage du volume : vérification"), steps)
        // Within the budget: rendered again with the limiter lower, each step named.
        steps.clear()
        g2 = 1f
        val asked = mutableListOf<M4aGuard.Step>()
        val inTime = M4aGuard.run(source, render = { c -> source = c.toDouble(); source }, encode = { g -> g2 = g }, decodedPeak = { decoded() },
            step = { steps += it.label() }, mayGoOn = { asked += it; true })
        assertEquals(2, inTime.renders)
        assertEquals(listOf(M4aGuard.Step(M4aGuard.Step.Kind.Render, 2, 3)), asked)
        assertEquals(
            listOf("Compression M4A", "Vérification", "Nouvel essai 2/3 : fabrication", "Nouvel essai 2/3 : compression", "Nouvel essai 2/3 : vérification"),
            steps,
        )
        // A further lowering past the budget: not tried, the gain actually encoded is the one told.
        source = -2.0
        g2 = 1f
        var encodes = 0
        val stubborn = M4aGuard.run(source, render = { error("no") }, encode = { g -> encodes++; g2 = g }, decodedPeak = { if (encodes < 3) 3.0 else -2.0 },
            mayGoOn = { false })
        assertEquals(2, encodes)
        assertEquals(g2, stubborn.gain, 0f)
    }

    @Test
    fun theBudgetKeepsTheExportShort() = kotlinx.coroutines.runBlocking {
        var now = 0L
        val budget = ExportBudget(60_000L) { now }
        // A 5-minute song renders in about a minute on a phone: no second render after it.
        budget.took(M4aGuard.Step.Kind.Render) { now += 50_000 }
        budget.took(M4aGuard.Step.Kind.Encode) { now += 4_000 }
        budget.took(M4aGuard.Step.Kind.Check) { now += 3_000 }
        assertFalse(budget.allows(M4aGuard.Step(M4aGuard.Step.Kind.Render, 2, 3)))
        assertFalse(budget.allows(M4aGuard.Step(M4aGuard.Step.Kind.Encode, 3, 0)))
        // A short song: a second render still fits.
        now = 0L
        val short = ExportBudget(60_000L) { now }
        short.took(M4aGuard.Step.Kind.Render) { now += 12_000 }
        short.took(M4aGuard.Step.Kind.Encode) { now += 1_000 }
        short.took(M4aGuard.Step.Kind.Check) { now += 1_000 }
        assertTrue(short.allows(M4aGuard.Step(M4aGuard.Step.Kind.Render, 2, 3)))
    }

    @Test
    fun aRenderThatStopsMovingIsSeenAsStuck() {
        var now = 0L
        val watch = StallWatch(60_000L) { now }
        assertFalse(watch.stalled(0f))
        now += 59_000
        assertFalse(watch.stalled(0f))
        now += 2_000
        assertTrue(watch.stalled(0f))
        // Moving again: not stuck.
        assertFalse(watch.stalled(0.1f))
        now += 30_000
        assertFalse(watch.stalled(0.2f))
    }

    /**
     * Review M1: a verification that cannot read the M4A back (a decoder that stops answering
     * included) keeps the file as encoded; only a cancel stops the export.
     */
    @Test
    fun aStuckVerificationKeepsTheFileEncoded() = kotlinx.coroutines.runBlocking {
        val logged = mutableListOf<String>()
        assertNull(M4aGuard.checkOrKeep({ logged += it }) { throw CodecStalledException("le codec du téléphone ne répond plus") })
        assertEquals(listOf("le codec du téléphone ne répond plus"), logged)
        assertNull(M4aGuard.checkOrKeep({ logged += it }) { throw java.io.IOException("pas de piste audio") })
        assertEquals(-3.0, M4aGuard.checkOrKeep({ logged += it }) { -3.0 }!!, 0.0)
        // Through the guard: encoded once, the file kept, no further try.
        var encodes = 0
        val r = M4aGuard.run(-2.0, render = { error("no") }, encode = { encodes++ },
            decodedPeak = { M4aGuard.checkOrKeep({}) { throw CodecStalledException("bloqué") } })
        assertEquals(1, encodes)
        assertEquals(1, r.encodings)
        assertNull(r.decodedPeakDb)
        // A cancel is not swallowed.
        try {
            M4aGuard.checkOrKeep({}) { throw kotlinx.coroutines.CancellationException("annulé") }
            fail("cancel")
        } catch (e: kotlinx.coroutines.CancellationException) {
            // expected
        }
    }

    /** Review M2: the video's watch starts with the render, not with the wait for the sounds before it. */
    @Test
    fun aWatchNotYetArmedNeverFlagsTheWaitBeforeTheRender() {
        var now = 0L
        val watch = StallWatch(stallMs = 60_000L, now = { now }, armed = false)
        // Two minutes waiting for the sounds at a progress of 0: not a stuck render.
        now += 120_000
        assertFalse(watch.stalled(0f))
        // Armed when the render starts: the minute counts from there.
        watch.arm()
        now += 59_000
        assertFalse(watch.stalled(0f))
        now += 2_000
        assertTrue(watch.stalled(0f))
    }
}
