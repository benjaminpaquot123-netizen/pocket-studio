package com.pocketstudio.bentest

import androidx.compose.ui.geometry.Rect
import com.pocketstudio.app.journey.Journey
import com.pocketstudio.app.journey.JourneyDriver
import com.pocketstudio.model.StarterWizard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * « Tout tester » (docs/PLAN-TEST-APK.md, T1): the live listening's seconds and alerts on made-up
 * readings, the lights and the end screen's verdict, an export read back from a made-up WAV, the
 * run's steps in their order, the latency's light.
 */
class ToutTesterTest {
    @get:Rule
    val tmp = TemporaryFolder()

    /** [seconds] of readings, 4 a second; [at] gives each reading from its index. */
    private fun reads(seconds: Int, at: (Int) -> Ecoute.Lu): List<Ecoute.Lu> = List(seconds * 4) { at(it) }

    private fun steady(i: Int, peak: Float = 0.5f) = Ecoute.Lu(ms = i * 250L, xruns = 3, cpu = 0.3f, peak = peak, lufs = -10f, drive = 5f)

    // --- The live listening -----------------------------------------------------------------------

    @Test
    fun aSteadySongIsHeardSecondBySecondWithoutAlert() {
        val r = reads(10) { i -> steady(i).copy(cpu = if (i == 17) 0.62f else 0.3f, xruns = if (i >= 30) 4 else 3) }
        val b = Ecoute.bilan(r, xrunsBefore = 3)
        assertEquals(10, b.secondes.size)
        assertTrue(b.niveaux(), b.secondes.all { abs(it.peakDb - -6.02f) < 0.05f })
        assertEquals(1, b.xruns)
        assertEquals(0.62f, b.cpuMax, 1e-6f)
        assertEquals(40, b.playing)
        assertTrue(b.text(), b.alertes.isEmpty() && b.text().endsWith("alertes : aucune"))
    }

    @Test
    fun twoSilentSecondsWhilePlayingAreASilenceUnlessTheSongPlansThem() {
        val hole = reads(12) { i -> steady(i, peak = if (i / 4 in 5..7) 0.001f else 0.5f) }
        val a = Ecoute.alertes(hole)
        assertEquals(listOf("SILENCE" to 5), a.map { it.kind to it.second })
        // One silent second: not yet.
        assertTrue(Ecoute.alertes(reads(12) { i -> steady(i, peak = if (i / 4 == 5) 0.001f else 0.5f) }).isEmpty())
        // The song's curve takes the master down there (a planned silence).
        val planned = hole.map { if (it.peak < 0.01f) it.copy(lane = 0.02f) else it.copy(lane = 1f) }
        assertTrue(Ecoute.alertes(planned).isEmpty())
        // Stopped: no silence to flag.
        assertTrue(Ecoute.alertes(hole.map { if (it.peak < 0.01f) it.copy(playing = false) else it }).isEmpty())
    }

    @Test
    fun aFadeIsTheLevelFallingWithTheLiveGain() {
        // From 15 s the short-term level falls 8 dB: with the live gain 5 dB lower, a fade; at a steady gain, the music.
        val fade = reads(25) { i -> steady(i).let { if (i / 4 >= 15) it.copy(lufs = -18f, drive = 0f) else it } }
        assertEquals(listOf("FONDU" to 15), Ecoute.alertes(fade).map { it.kind to it.second })
        val quieter = reads(25) { i -> steady(i).let { if (i / 4 >= 15) it.copy(lufs = -18f) else it } }
        assertTrue(Ecoute.alertes(quieter).isEmpty())
    }

    @Test
    fun anIsolatedPopIsAJump() {
        val pop = reads(6) { i -> steady(i, peak = if (i == 10) 0.9f else 0.1f) }
        assertEquals(listOf("SAUT" to 2), Ecoute.alertes(pop).map { it.kind to it.second })
        // A loud part that goes on (a drop coming in) is not a jump.
        assertTrue(Ecoute.alertes(reads(6) { i -> steady(i, peak = if (i >= 10) 0.9f else 0.1f) }).isEmpty())
    }

    // --- The lights ---------------------------------------------------------------------------------

    @Test
    fun theLightsFollowThePlansThresholds() {
        assertEquals(listOf(Feu.VERT, Feu.ORANGE, Feu.ORANGE, Feu.ROUGE), listOf(0, 1, 2, 3).map(Feux::decrochages))
        assertEquals(listOf(Feu.VERT, Feu.ORANGE, Feu.ORANGE, Feu.ROUGE), listOf(0.49f, 0.5f, 0.8f, 0.81f).map(Feux::pireBloc))
        assertEquals(listOf(Feu.VERT, Feu.ORANGE, Feu.ROUGE), listOf(44.5, 70.0, 70.1).map(Feux::latence))
        assertEquals(listOf(Feu.VERT, Feu.ORANGE, Feu.ORANGE, Feu.ROUGE), listOf(0.049, 0.05, 0.15, 0.16).map(Feux::imagesLentes))
        assertEquals(Feu.VERT, Feux.export(true, 0.05, -1.2, 7.0, 9.0).first)
        assertEquals(Feu.ORANGE to "19.0 s, plus de 2 fois les 9.0 s prévues", Feux.export(true, 0.05, -1.2, 19.0, 9.0))
        assertEquals(Feu.ORANGE, Feux.export(true, 0.3, -1.2, 7.0, 9.0).first)
        assertEquals(Feu.ORANGE, Feux.export(true, 0.05, -0.5, 7.0, 9.0).first)
        assertEquals(Feu.ROUGE, Feux.export(false, null, null, 7.0, 9.0).first)
        val ok = Ecoute.bilan(reads(5) { steady(it) }, 3)
        assertEquals(Feu.VERT to "", Feux.style(ok))
        val hot = Ecoute.bilan(reads(5) { i -> steady(i).copy(cpu = if (i == 3) 0.85f else 0.3f, xruns = if (i > 8) 4 else 3) }, 3)
        assertEquals(Feu.ROUGE to "1 décrochage(s), pire bloc 85 %", Feux.style(hot))
        val nan = Ecoute.bilan(reads(5) { i -> steady(i).copy(nonFinite = i == 2) }, 3)
        assertEquals(Feu.ROUGE, Feux.style(nan).first)
        val popped = Ecoute.bilan(reads(6) { i -> steady(i, peak = if (i == 10) 0.9f else 0.1f) }, 3)
        assertEquals(Feu.ORANGE, Feux.style(popped).first)
    }

    @Test
    fun theEndScreenSaysTheLights() {
        val c = Carnet(File(tmp.root, "feux.tsv"))
        val steps = listOf("a", "b", "c", "d", "e", "f").map { Etape(it, it.uppercase(), 1_000) { "" } }
        fun run(step: String, end: String, vararg lights: String) {
            c.add(Carnet.DEBUT, step)
            lights.forEach { c.add(Carnet.FEU, step, it) }
            c.add(end, step, "fait")
        }
        run("a", Carnet.REUSSI, "vert;")
        run("b", Carnet.REUSSI)
        run("c", Carnet.REUSSI, "vert;", "orange;pire bloc 62 %")
        run("d", Carnet.ECHEC, "vert;")
        run("e", Carnet.REUSSI, "non concluant;l'écran ne s'est pas éteint")
        val feux = Rapport.feux(c.lines(), steps)
        assertEquals(mapOf("a" to Feu.VERT, "b" to Feu.VERT, "c" to Feu.ORANGE, "d" to Feu.ROUGE, "e" to Feu.GRIS, "f" to null), feux)
        assertEquals("2 verts, 1 orange, 1 rouge, 1 non concluante, 1 pas faite", Rapport.bilanFeux(c.lines(), steps).verdict())
        val md = Rapport.markdown(c.lines(), steps, listOf("Version" to "x"), tout = true)
        assertTrue(md, md.contains("**Feux : 2 verts, 1 orange, 1 rouge, 1 non concluante, 1 pas faite**"))
        assertTrue(md, md.contains("| C | 🟠 orange (pire bloc 62 %) | ✅ réussie |"))
        // A run picked up: only the lights since the step last began.
        run("d", Carnet.REUSSI, "vert;")
        assertEquals(Feu.VERT, Rapport.feux(c.lines(), steps)["d"])
    }

    // --- An export read back ----------------------------------------------------------------------------

    @Test
    fun aWavIsReadBackForItsLengthAndTruePeak() {
        val f = File(tmp.root, "sine.wav")
        writeWav(f, seconds = 2.0, amplitude = 0.5)
        val r = Relecture.wav(f)
        assertEquals(2.0, r.secondes, 0.001)
        assertEquals(-6.02, r.creteDbtp, 0.1)
        assertTrue(Relecture.Relu(30.0, -1.5, 1080, 1920).portrait916)
        assertTrue(!Relecture.Relu(30.0, -1.5, 1920, 1080).portrait916)
    }

    /** 16-bit stereo 48 kHz, a 997 Hz sine. */
    private fun writeWav(f: File, seconds: Double, amplitude: Double) {
        val frames = (seconds * 48_000).toInt()
        val data = frames * 4
        DataOutputStream(FileOutputStream(f).buffered()).use { o ->
            fun le32(v: Int) = o.writeInt(Integer.reverseBytes(v))
            fun le16(v: Int) = o.writeShort(java.lang.Short.reverseBytes(v.toShort()).toInt())
            o.writeBytes("RIFF"); le32(36 + data); o.writeBytes("WAVEfmt ")
            le32(16); le16(1); le16(2); le32(48_000); le32(48_000 * 4); le16(4); le16(16)
            o.writeBytes("data"); le32(data)
            for (i in 0 until frames) {
                val v = (amplitude * sin(2 * PI * 997 * i / 48_000) * 32767).toInt()
                le16(v); le16(v)
            }
        }
    }

    // --- The run ------------------------------------------------------------------------------------------

    private class Screen(val tags: Set<String>) : JourneyDriver {
        override fun count(tag: String, merged: Boolean) = if (tag in tags) 1 else 0
        override fun bounds(tag: String) = Rect(0f, 0f, 10f, 10f)
        override fun scrollTo(tag: String) = Unit
        override fun click(tag: String) = Unit
        override fun idle(ms: Long) = Thread.sleep(minOf(ms, 5))
        override fun tags(pattern: Regex) = tags.filter { pattern.matches(it) }
    }

    private object Still : Sonde {
        override fun lire() = Sonde.Lecture(0, 0f, 0f, false, true)
    }

    @Test
    fun theRunPlaysEveryStyleWithTheHeavyOnesColdAndHotAndEndsWithTheScreenOff() {
        val names = ToutTester(Journey(Screen(emptySet())), Still, object : Appli {
            override fun ouvrirStudio() = Unit
            override fun export() = Appli.ExportVu(false, emptyList(), null)
            override fun capture(file: File) = false
            override fun demander(question: String, maxMs: Long): Boolean? = null
        }, Carnet(File(tmp.root, "c.tsv")), tmp.root).etapes().map { it.name }
        assertEquals(listOf("ouverture", "style_jump_up_froid", "style_dnb_froid"), names.take(3))
        val styles = names.filter { it.startsWith("style_") }
        assertEquals(StarterWizard.STYLES.size + 2, styles.size)
        assertEquals(37, StarterWizard.STYLES.size)
        assertTrue(StarterWizard.STYLES.all { s -> styles.any { it == "style_${s.id}" || it == "style_${s.id}_froid" } })
        assertEquals(listOf("style_dnb_chaud", "style_jump_up_chaud", "export_m4a", "export_wav", "export_flac", "export_video", "latence"),
            names.subList(names.indexOf("style_dnb_chaud"), names.indexOf("latence") + 1))
        assertEquals(listOf("guide_house", "guide_trap", "guide_techno", "images", "rotation", "ecran_eteint"), names.takeLast(6))
        assertEquals(names.size, names.toSet().size)
    }

    @Test
    fun theLatencyIsThreeRoundTripsAndTheirMedian() {
        val c = Carnet(File(tmp.root, "l.tsv"))
        val ms = ArrayDeque(listOf(44.5, null, 61.0))
        val t = ToutTester(Journey(Screen(setOf("space_bar", "tab_Mix"))), Still, object : Appli {
            override fun ouvrirStudio() = Unit
            override fun export() = Appli.ExportVu(false, emptyList(), null)
            override fun capture(file: File) = false
            override fun demander(question: String, maxMs: Long): Boolean? = null
            override fun latence(): Double? = ms.removeFirst()
            override fun latenceEchec() = "SilentInput"
        }, c, tmp.root)
        assertEquals("61.00 ms (mesures : 44.50, 61.00, 1 ratée(s))", t.latence())
        assertEquals("orange;61.00 ms", c.lines().single { it.kind == Carnet.FEU }.text)
    }
}
