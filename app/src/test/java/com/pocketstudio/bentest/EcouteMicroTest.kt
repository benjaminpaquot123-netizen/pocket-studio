package com.pocketstudio.bentest

import com.pocketstudio.bentest.SonsFabriques.Biquad
import com.pocketstudio.bentest.SonsFabriques.Chaine
import com.pocketstudio.bentest.SonsFabriques.RATE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.math.abs

/**
 * « Écoute au micro » (T1b) from end to end with a made-up phone: its speaker is a known filter, each style a known
 * filter on pink noise, the room a little white noise. The run must find the speaker's curve and each style's
 * fingerprint, write them in mesures.json and rapport.md, put the volume back, and keep no sound.
 */
class EcouteMicroTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val log = ArrayList<String>()
    private val speaker = Chaine(Biquad.highPass(400.0), Biquad.peak(3000.0, 6.0))
    private val styleFilters = mapOf(
        "jump_up" to Chaine(Biquad.highPass(60.0), Biquad.peak(150.0, 6.0)),
        "house" to Chaine(Biquad.highPass(40.0)),
        "trap" to Chaine(Biquad.lowPass(5000.0)),
        "techno" to Chaine(Biquad.peak(1000.0, -6.0)),
    )
    private var sweep: FloatArray? = null
    private var sweepAt = 0
    private var song: String? = null

    private val prep = object : Preparateur {
        override fun ouverture() { log += "ouverture" }
        override fun morceau(style: String) { log += "morceau:$style"; song = style }
        override fun rejouer() = Unit
        override fun arreter() { log += "arreter"; song = null }
        override fun ouvrirPads() = Unit
        override fun ouvrirBatterie() = "pad_0"
        override fun batterieOuverte() = true
    }

    private val hp = object : HautParleur {
        override fun jouer(pcm: FloatArray) { log += "jouer:${pcm.size}"; sweep = speaker.filtrer(pcm); sweepAt = 0 }
        override fun arreter() { log += "hp_arreter"; sweep = null }
        override fun reglerVolume() { log += "volume" }
        override fun remettreVolume() { log += "volume_remis" }
    }

    private fun micro(granted: Boolean) = object : Micro {
        override val rate = RATE
        override fun autorise() = granted
        override fun ecouter(ms: Long, recevoir: (FloatArray, Int) -> Unit) {
            log += "ecoute:$ms"
            val n = (ms * RATE / 1000).toInt()
            val room = SonsFabriques.blanc(n, 0.0005, seed = ms)
            val music = song?.let { speaker.filtrer(styleFilters.getValue(it).filtrer(SonsFabriques.rose(n, 0.1, seed = it.hashCode().toLong()))) }
            val x = FloatArray(n) { i ->
                val s = sweep?.let { sw -> sw.getOrElse(sweepAt + i) { 0f } } ?: 0f
                room[i] + s + (music?.get(i) ?: 0f)
            }
            sweepAt += n
            var i = 0
            while (i < n) {
                val k = minOf(960, n - i)
                recevoir(x.copyOfRange(i, i + k), k)
                i += k
            }
        }
    }

    private fun run(granted: Boolean): Pair<Carnet, List<Etape>> {
        val carnet = Carnet(File(tmp.root, "carnet.tsv"))
        val e = EcouteMicro(prep, micro(granted), hp, carnet, File(tmp.root, "sons"), ecouteMs = 6_000, bruitMs = 3_000, balayageS = 6.0).etapes()
        Passage(carnet, e).run()
        return carnet to e
    }

    @Test
    fun theRunFindsTheSpeakersCurveAndEachSongsFingerprint() {
        val (carnet, etapes) = run(granted = true)
        val lines = carnet.lines()
        assertEquals(
            listOf("micro_ouverture", "micro_bruit", "micro_courbe", "micro_jump_up", "micro_house", "micro_trap", "micro_techno"),
            etapes.map { it.name },
        )
        assertEquals("every step done", etapes.map { it.name }, lines.filter { it.kind == Carnet.REUSSI }.map { it.step })
        assertTrue("the sweep the robot makes, 6 s at 48 kHz: $log", "jouer:${6 * RATE}" in log)
        assertEquals("the volume put back each time", log.count { it == "volume" }, log.count { it == "volume_remis" })
        assertTrue(log.count { it == "volume" } >= 5)

        val curve = EcouteMicro.courbe(lines)
        val want = SonsFabriques.courbeAttendue(speaker::power)
        assertEquals(Tiers.NOMS.size, curve.size)
        for (k in curve.indices) {
            if (Tiers.CENTRES[k] < 125 || Tiers.CENTRES[k] > 12_000) continue
            assertNotNull(Tiers.NOMS[k], curve[k])
            assertTrue("${Tiers.NOMS[k]} Hz: ${curve[k]} against ${want[k]}", abs(curve[k]!! - want[k]) <= 1.0)
        }

        val prints = EcouteMicro.empreintes(lines)
        assertEquals(EcouteMicro.STYLES.toSet(), prints.keys)
        for ((style, filter) in styleFilters) {
            val expected = SonsFabriques.octavesAttendues { f -> filter.power(f) * speaker.power(f) }
            val got = prints.getValue(style)
            for (k in 2 until got.size) assertTrue("$style ${Empreinte.OCTAVES[k]} Hz: ${got[k]} against ${expected[k]}", got[k] != null && abs(got[k]!! - expected[k]) <= 1.5)
        }
        assertEquals("each step's light", listOf("micro_courbe", "micro_jump_up", "micro_house", "micro_trap", "micro_techno"),
            lines.filter { it.kind == Carnet.FEU }.map { it.step }.distinct())

        val about = listOf("Version" to "test")
        val json = Rapport.json(lines, about)
        assertTrue(json, json.contains("\"micro\":{"))
        assertTrue(json, json.contains("\"courbe_db\":["))
        assertTrue(json, Regex("\"empreintes\":\\{\"jump_up\":\\{").containsMatchIn(json))
        val md = Rapport.markdown(lines, etapes, about, tout = true)
        assertTrue(md, md.contains("## Écoute au micro"))
        assertTrue(md, md.contains("Courbe du haut-parleur"))
        assertFalse("no sound kept without an anomaly", File(tmp.root, "sons").list()?.isNotEmpty() == true)
    }

    @Test
    fun aRefusedMicrophoneSkipsEveryStep() {
        val (carnet, etapes) = run(granted = false)
        val lines = carnet.lines()
        assertEquals("the same 7 steps, each skipped", 7, etapes.size)
        assertEquals(etapes.size, lines.count { it.kind == Carnet.REUSSI && it.text == EcouteMicro.REFUSE })
        assertTrue("nothing played, nothing listened to: $log", log.isEmpty())
        val feux = Rapport.feux(lines, etapes)
        assertTrue("not a green light for a step not done: $feux", feux.values.all { it == Feu.GRIS })
    }

    @Test
    fun theMicrophoneIsAskedForWhenItIsNeeded() {
        val calls = ArrayList<String>()
        EcouteMicro.demarrer(autorise = true, demander = { calls += "demander" }, lancer = { calls += "lancer" })
        assertEquals(listOf("lancer"), calls)
        calls.clear()
        EcouteMicro.demarrer(autorise = false, demander = { calls += "demander" }, lancer = { calls += "lancer" })
        assertEquals("asked first; the run starts on the answer, whatever it is", listOf("demander"), calls)
    }
}
