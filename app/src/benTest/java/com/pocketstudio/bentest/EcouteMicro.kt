package com.pocketstudio.bentest

import com.pocketstudio.app.report.JsonText
import com.pocketstudio.model.StarterWizard
import java.io.File
import java.util.Locale

/** The phone's microphone (the real one in [MicroReel]). */
interface Micro {
    val rate: Int

    /** RECORD_AUDIO granted. */
    fun autorise(): Boolean

    /** Listens [ms], handing each block to [recevoir] (samples, count); the blocks are not kept. */
    fun ecouter(ms: Long, recevoir: (FloatArray, Int) -> Unit)
}

/** The engine playing a sound of the robot's (the sweep) and the phone's music volume. */
interface HautParleur {
    /** [pcm] (mono, 48 kHz) played by the engine; returns once it plays. */
    fun jouer(pcm: FloatArray)
    fun arreter()

    /** The music volume set for the measure (kept to be put back). */
    fun reglerVolume()
    fun remettreVolume()
}

/**
 * « Écoute au micro » (docs/PLAN-TEST-APK.md, T1b, about 5 minutes, the phone lying in a quiet room): the room's
 * noise, the speaker's curve (a sweep), then 4 songs (jump up, house, trap, techno) heard by the microphone, each
 * reduced to its energy per band. Nothing is recorded but 2 s around an anomaly ([Guet]). Microphone refused: every
 * step « passée ». The numbers go in the logbook ([Carnet.MICRO] lines), so a run picked up after a crash keeps them.
 */
class EcouteMicro(
    private val prep: Preparateur,
    private val micro: Micro,
    private val hp: HautParleur,
    private val carnet: Carnet,
    private val sons: File,
    private val ecouteMs: Long = 30_000,
    private val bruitMs: Long = 4_000,
    private val balayageS: Double = 12.0,
    private val styles: List<String> = STYLES,
    private val horloge: () -> Long = System::currentTimeMillis,
) {
    fun etapes(): List<Etape> = buildList {
        add(etape("micro_ouverture", "Ouverture du studio", 120_000) { prep.ouverture(); "studio ouvert" })
        add(etape("micro_bruit", "Bruit de la pièce", 60_000) { bruit() })
        add(etape("micro_courbe", "Courbe du haut-parleur", 120_000) { courbe() })
        for (s in styles) add(etape("micro_$s", "Empreinte au micro : ${StarterWizard.style(s)?.name ?: s}", 600_000) { style(s) })
    }

    private fun etape(name: String, label: String, maxMs: Long, work: () -> String) = Etape(name, label, maxMs) {
        if (!micro.autorise()) {
            carnet.add(Carnet.FEU, name, Feu.GRIS.mot + ";micro refusé")
            REFUSE
        } else work()
    }

    private fun ecouter(ms: Long, vararg to: (FloatArray, Int) -> Unit) = micro.ecouter(ms) { x, n -> to.forEach { it(x, n) } }

    private fun bruit(): String {
        hp.arreter()
        val b = Bandes(micro.rate)
        ecouter(bruitMs, b::ajouter)
        val p = b.puissances()
        carnet.add(Carnet.MICRO, "micro_bruit", "bruit;" + Tiers.csv(p.map { Tiers.db(it) }))
        return "bruit de la pièce mesuré (${b.trames} trames)"
    }

    /** The room's noise the logbook holds (mean power per frame), null when not measured. */
    private fun bruitLu(): DoubleArray? = lire(carnet.lines(), "bruit")?.let { v -> DoubleArray(v.size) { i -> v[i]?.let { Math.pow(10.0, it / 10) } ?: 0.0 } }

    private fun courbe(): String {
        val sweep = Courbe.balayage(micro.rate, balayageS)
        // Every sample of the sweep under two half windows, as the microphone hears it (it starts late).
        val ref = Bandes(micro.rate)
        ref.ajouter(FloatArray(Tiers.TAILLE / 2))
        ref.ajouter(sweep)
        ref.ajouter(FloatArray(Tiers.TAILLE))
        val mic = Bandes(micro.rate)
        hp.reglerVolume()
        try {
            hp.jouer(sweep)
            ecouter((balayageS * 1000).toLong() + QUEUE_MS, mic::ajouter)
        } finally {
            hp.arreter()
            hp.remettreVolume()
        }
        val r = Courbe.mesurer(ref.energies(), mic.energies(), mic.trames, bruitLu())
        carnet.add(Carnet.MICRO, "micro_courbe", "courbe;" + Tiers.csv(r.db))
        carnet.add(Carnet.MICRO, "micro_courbe", "rsb;" + Tiers.csv(r.rsb))
        val f = Courbe.feu(r)
        carnet.add(Carnet.FEU, "micro_courbe", f.first.mot + ";" + f.second)
        return "courbe : " + f.second.ifEmpty { f.first.mot }
    }

    private fun style(s: String): String {
        val name = "micro_$s"
        prep.morceau(s)
        val t0 = horloge()
        val mic = Bandes(micro.rate)
        val guet = Guet(micro.rate, sons, name)
        hp.reglerVolume()
        // From the song heard playing to the microphone listening (the Linux side takes its render from there, about a second).
        val debut = (horloge() - t0).coerceAtLeast(0) / 1000.0
        try {
            ecouter(ecouteMs, mic::ajouter, guet::ajouter)
        } finally {
            guet.finir()
            prep.arreter()
            hp.remettreVolume()
        }
        val noise = bruitLu()
        val oct = Empreinte.octaves(mic.energies(), mic.trames, noise)
        val tiers = Empreinte.tiers(mic.energies(), mic.trames, noise)
        carnet.add(Carnet.MICRO, name, "empreinte;$s;${fmt3(debut)};${fmt3(ecouteMs / 1000.0)};${Tiers.csv(oct)};${Tiers.csv(tiers)}")
        for (a in guet.anomalies) {
            val file = guet.extraits.firstOrNull { it.name.startsWith(String.format(Locale.ROOT, "%s-%.1fs", name, a.seconde)) }
            carnet.add(Carnet.MICRO, name, "anomalie;${a.kind};${fmt3(a.seconde)};${file?.let { "sons/${it.name}" } ?: ""};${a.detail}")
        }
        val f = Empreinte.feu(oct)
        val parts = listOfNotNull(f.second.takeIf { it.isNotEmpty() }, guet.anomalies.takeIf { it.isNotEmpty() }?.joinToString(", ") { "${it.kind} à ${Courbe.fmt(it.seconde)} s" })
        val feu = if (guet.anomalies.isNotEmpty()) Feu.pire(listOf(f.first, Feu.ORANGE)) else f.first
        carnet.add(Carnet.FEU, name, feu.mot + ";" + parts.joinToString(", "))
        return "empreinte : " + oct.mapIndexed { i, d -> "${Empreinte.OCTAVES[i]} Hz ${d?.let { Courbe.fmt(it) } ?: "—"}" }.joinToString(", ")
    }

    companion object {
        val STYLES = listOf("jump_up", "house", "trap", "techno")
        const val REFUSE = "passée : micro refusé"

        /** After the sweep, what the room still gives back (and the speaker's delay). */
        const val QUEUE_MS = 1_000L

        private fun fmt3(v: Double) = String.format(Locale.ROOT, "%.3f", v)

        /** The last « [champ];csv » line of the logbook's microphone lines, as dB values. */
        private fun lire(lines: List<Carnet.Ligne>, champ: String): List<Double?>? =
            lines.lastOrNull { it.kind == Carnet.MICRO && it.text.startsWith("$champ;") }?.let { Tiers.lireCsv(it.text.substringAfter(';')) }

        private data class Trace(val style: String, val debut: Double, val duree: Double, val octaves: List<Double?>, val tiers: List<Double?>)
        private data class Vu(val step: String, val kind: String, val seconde: Double, val extrait: String, val detail: String)

        private fun traces(lines: List<Carnet.Ligne>): Map<String, Trace> = lines.filter { it.kind == Carnet.MICRO && it.text.startsWith("empreinte;") }.mapNotNull { l ->
            val f = l.text.split(';')
            if (f.size < 6) null else Trace(f[1], f[2].toDoubleOrNull() ?: 0.0, f[3].toDoubleOrNull() ?: 0.0, Tiers.lireCsv(f[4]), Tiers.lireCsv(f[5]))
        }.associateBy { it.style }

        private fun vus(lines: List<Carnet.Ligne>): List<Vu> = lines.filter { it.kind == Carnet.MICRO && it.text.startsWith("anomalie;") }.mapNotNull { l ->
            val f = l.text.split(';', limit = 5)
            if (f.size < 5) null else Vu(l.step, f[1], f[2].toDoubleOrNull() ?: 0.0, f[3], f[4])
        }

        private fun feuDe(lines: List<Carnet.Ligne>, step: String): String? = lines.lastOrNull { it.kind == Carnet.FEU && it.step == step }?.text?.substringBefore(';')

        /** The speaker's curve the logbook holds (dB per third-octave, [Tiers.NOMS]); empty: none. */
        fun courbe(lines: List<Carnet.Ligne>): List<Double?> = lire(lines, "courbe").orEmpty()

        /** Each style's fingerprint the logbook holds (dB per octave, [Empreinte.OCTAVES]). */
        fun empreintes(lines: List<Carnet.Ligne>): Map<String, List<Double?>> = traces(lines).mapValues { it.value.octaves }

        /** The extracts the logbook names (« sons/… .wav »). */
        fun extraits(lines: List<Carnet.Ligne>): List<String> = vus(lines).map { it.extrait }.filter { it.isNotEmpty() }.distinct()

        private fun nums(v: List<Double?>) = JsonText.array(v.map { d -> d?.let { String.format(Locale.ROOT, "%.2f", it) } ?: "null" })

        /** mesures.json's « micro » object (null: no such step in the logbook); scripts/bentest/empreinte.py reads it. */
        fun json(lines: List<Carnet.Ligne>): String? {
            if (lines.none { it.kind == Carnet.MICRO }) return null
            val prints = traces(lines).values.map { t ->
                t.style to JsonText.obj(
                    "db" to nums(t.octaves), "tiers_db" to nums(t.tiers), "debut_s" to fmt3(t.debut), "duree_s" to fmt3(t.duree),
                    "feu" to (feuDe(lines, "micro_${t.style}")?.let { JsonText.string(it) }),
                )
            }
            val seen = vus(lines).map { v ->
                JsonText.obj("etape" to JsonText.string(v.step), "kind" to JsonText.string(v.kind), "seconde" to fmt3(v.seconde),
                    "extrait" to JsonText.string(v.extrait), "detail" to JsonText.string(v.detail))
            }
            return JsonText.obj(
                "rate" to "48000",
                "tiers" to JsonText.array(Tiers.NOMS.map { JsonText.string(it) }),
                "bruit_db" to lire(lines, "bruit")?.let { nums(it) },
                "courbe_db" to lire(lines, "courbe")?.let { nums(it) },
                "rsb_db" to lire(lines, "rsb")?.let { nums(it) },
                "courbe_feu" to feuDe(lines, "micro_courbe")?.let { JsonText.string(it) },
                "octaves" to JsonText.array(Empreinte.OCTAVES.map { it.toString() }),
                "empreintes" to JsonText.obj(*prints.toTypedArray()),
                "anomalies" to JsonText.array(seen),
            )
        }

        private fun cell(d: Double?) = d?.let { Courbe.fmt(it) } ?: "—"

        /** rapport.md's section. */
        fun section(lines: List<Carnet.Ligne>): String = buildString {
            if (lines.none { it.kind == Carnet.MICRO }) return ""
            appendLine("## Écoute au micro")
            appendLine()
            appendLine("Analysé sur le téléphone : seuls ces chiffres sortent, plus 2 s du micro autour d'une anomalie. Le micro a sa propre couleur : la courbe est celle du haut-parleur, du micro et de la pièce ensemble.")
            val c = courbe(lines)
            if (c.isNotEmpty()) {
                appendLine()
                appendLine("**Courbe du haut-parleur** (dB, 0 dB = moyenne de 500 Hz à 2 kHz ; « — » : sous le bruit de la pièce)")
                appendLine()
                appendLine("| " + Tiers.NOMS.joinToString(" | ") + " |")
                appendLine("|" + Tiers.NOMS.joinToString("") { "---|" })
                appendLine("| " + Tiers.NOMS.indices.joinToString(" | ") { cell(c.getOrNull(it)) } + " |")
            }
            val t = traces(lines)
            if (t.isNotEmpty()) {
                appendLine()
                appendLine("**Empreinte au micro** (dB par octave, 0 dB = moyenne de 500 Hz à 4 kHz)")
                appendLine()
                appendLine("| Style | " + Empreinte.OCTAVES.joinToString(" | ") { "$it Hz" } + " | Feu |")
                appendLine("|---|" + Empreinte.OCTAVES.joinToString("") { "---|" } + "---|")
                for (tr in t.values) appendLine("| ${tr.style} | " + tr.octaves.joinToString(" | ") { cell(it) } + " | ${feuDe(lines, "micro_${tr.style}") ?: ""} |")
            }
            val v = vus(lines)
            if (v.isNotEmpty()) {
                appendLine()
                appendLine("**Anomalies entendues**")
                appendLine()
                for (a in v) appendLine("- ${a.step} : ${a.kind} à ${Courbe.fmt(a.seconde)} s (${a.detail})" + if (a.extrait.isNotEmpty()) ", extrait de 2 s : ${a.extrait}" else "")
            }
        }

        /**
         * « Écoute au micro » touched, its sentence read, « Commencer »: the microphone asked for now if it is not
         * granted ([demander], whose answer then calls [lancer]), else the run started at once.
         */
        fun demarrer(autorise: Boolean, demander: () -> Unit, lancer: () -> Unit) {
            if (autorise) lancer() else demander()
        }

        /** What « Écoute au micro » says before the microphone is asked for. */
        const val PHRASE = "Le robot va écouter le haut-parleur avec le micro pendant environ 5 minutes : un balayage de sons, puis 4 morceaux. " +
            "Pose le téléphone sur une table, dans une pièce calme, et ne parle pas. Rien n'est enregistré : seuls des chiffres sortent, " +
            "plus 2 secondes de son autour d'un problème s'il y en a un (le rapport le dira avant l'envoi)."
    }
}
