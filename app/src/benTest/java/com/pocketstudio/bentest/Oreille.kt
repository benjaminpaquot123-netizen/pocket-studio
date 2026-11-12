package com.pocketstudio.bentest

import com.pocketstudio.model.StarterWizard
import java.io.File
import java.util.Locale

/** What Benjamin answered to a question of the banner. */
enum class Verdict(val mot: String) {
    OUI("oui"), NON("non"), PASSEE("passée"), SANS_REPONSE("sans réponse");

    companion object {
        fun of(mot: String): Verdict? = entries.firstOrNull { it.mot == mot }
    }
}

/** An answer: [commentaire] is the optional one-line word. */
data class Reponse(val verdict: Verdict, val commentaire: String = "")

/** One question of the banner. [maxMs]: how long it waits for an answer (never forever). */
data class Question(val id: String, val texte: String, val maxMs: Long = QuestionsOreille.ATTENTE_MS)

/** What the phone has plugged in or built in, as far as the app can tell without a permission. */
data class Materiel(val casqueFilaire: Boolean, val bluetooth: Boolean)

/** What the robot does to put each question's scene in place (the real one drives the screens; the tests record calls). */
interface Preparateur {
    /** The studio open in front. */
    fun ouverture()

    /** A song of [style] made and playing (the engine says it plays and sound comes out). */
    fun morceau(style: String)

    /** The open song playing again (the studio first, whatever is on screen). */
    fun rejouer()

    fun arreter()

    /** The Live screen's pads open, nothing playing. */
    fun ouvrirPads()

    /** Sons › Batterie open; the tag of the kick's pad. */
    fun ouvrirBatterie(): String
}

/**
 * « Questions à l'oreille » (docs/PLAN-TEST-APK.md, niveau 3): the eight questions of Benjamin in one pass.
 * 1 to 4: « Ça tape ? » on the phone's speaker, the robot having made and started the song; 5 and 6: wired
 * headphones and Bluetooth (skipped when the phone has none, not asked in vain); 7: playing with the fingers;
 * 8: three swipes from the left edge on the kick's pad, read by the robot itself. A ❌ keeps a picture of the
 * app's windows, the last lines of the app's journal and the last measurements. Every wait has a timeout.
 */
class QuestionsOreille(
    private val prep: Preparateur,
    private val sonde: Sonde,
    private val appli: Appli,
    private val carnet: Carnet,
    private val captures: File,
) {
    fun etapes(): List<Etape> = listOf(
        Etape("oreille_ouverture", "Ouverture du studio", 120_000) { prep.ouverture(); "studio ouvert" },
    ) + STYLES.mapIndexed { i, s ->
        val nom = StarterWizard.style(s)?.name ?: s
        etape("oreille_${i + 1}_$s", "Question ${i + 1} : ça tape ? ($nom)", 600_000) {
            val id = "oreille_${i + 1}_$s"
            prep.morceau(s)
            val r = poser(Question(id, "Ça tape ? ($nom, haut-parleur du téléphone)"))
            prep.arreter()
            conclure(id, r)
        }
    } + listOf(
        etape("oreille_5_casque", "Question 5 : casque filaire", 600_000) {
            saut(Cas.CASQUE)?.let { return@etape passer("oreille_5_casque", it) }
            prep.rejouer()
            val r = poser(Question("oreille_5_casque", "Casque filaire : la basse est là et le son est large ?"))
            prep.arreter()
            conclure("oreille_5_casque", r)
        },
        etape("oreille_6_bluetooth", "Question 6 : Bluetooth", 900_000) {
            saut(Cas.BLUETOOTH)?.let { return@etape passer("oreille_6_bluetooth", it) }
            prep.rejouer()
            val r = poser(Question("oreille_6_bluetooth", "Bluetooth : branche ton casque ou ton enceinte, puis coupe-le en pleine musique. Le son a suivi, sans plantage ?", BLUETOOTH_MS))
            // The robot's own half: the engine did not choke on the cut.
            val after = sonde.lire()
            check(!after.nonFinite) { "valeur invalide (NaN) après la coupure" }
            prep.arreter()
            conclure("oreille_6_bluetooth", r)
        },
        etape("oreille_7_doigts", "Question 7 : jouer avec les doigts", 600_000) {
            prep.arreter()
            prep.ouvrirPads()
            val r = poser(Question("oreille_7_doigts", "Joue avec les doigts : les pads à trois doigts, puis le clavier. Pas de retard gênant ?"))
            conclure("oreille_7_doigts", r)
        },
        etape("oreille_8_retour", "Question 8 : trois gestes retour (le robot lit seul)", 120_000) { retour() },
    )

    /** Which skip rule a question has. */
    enum class Cas { CASQUE, BLUETOOTH }

    /** Why [cas] is not asked (null: it is), from what the phone has. */
    fun saut(cas: Cas): String? {
        val m = appli.materiel()
        return when (cas) {
            Cas.CASQUE -> if (m.casqueFilaire) null else SANS_CASQUE
            Cas.BLUETOOTH -> if (m.bluetooth) null else SANS_BLUETOOTH
        }
    }

    private fun etape(name: String, label: String, maxMs: Long, work: () -> String) = Etape(name, label, maxMs, { work() })

    private fun passer(id: String, raison: String): String {
        carnet.add(Carnet.REPONSE, id, reponseLigne(Verdict.PASSEE, "", raison))
        return raison
    }

    /** The question asked in the banner, the answer written in the logbook; a ❌ keeps its proof. */
    private fun poser(q: Question): Reponse {
        mesure(q.id, "avant")
        val r = appli.poser(q, q.maxMs)
        mesure(q.id, "après")
        var capture = ""
        if (r.verdict == Verdict.NON) capture = preuve(q.id)
        carnet.add(Carnet.REPONSE, q.id, reponseLigne(r.verdict, capture, r.commentaire))
        return r
    }

    private fun conclure(id: String, r: Reponse): String {
        val mot = if (r.commentaire.isBlank()) "" else " : ${r.commentaire}"
        return when (r.verdict) {
            Verdict.OUI -> "✅ oui$mot"
            Verdict.PASSEE -> "passée$mot"
            Verdict.NON -> error("❌ non$mot")
            Verdict.SANS_REPONSE -> error("pas de réponse")
        }
    }

    /** Question 8: three swipes from the left edge on the kick's pad; the kick must still be at the centre of the stereo field. */
    private fun retour(): String {
        prep.arreter()
        val pad = prep.ouvrirBatterie()
        val avant = appli.kickAuCentre()
        repeat(3) { appli.glisserDuBordGauche(pad) }
        val apres = appli.kickAuCentre()
        val id = "oreille_8_retour"
        check(avant != null && apres != null) { "pan du kick illisible" }
        if (!apres) {
            val capture = preuve(id)
            carnet.add(Carnet.REPONSE, id, reponseLigne(Verdict.NON, capture, "le kick a quitté le centre"))
            error("❌ le kick a quitté le centre après trois gestes depuis le bord gauche")
        }
        carnet.add(Carnet.REPONSE, id, reponseLigne(Verdict.OUI, "", "lu par le robot : kick au centre"))
        return "✅ kick resté au centre (lu par le robot)"
    }

    /** One reading of the engine in the logbook (a ❌ shows what it was). */
    private fun mesure(id: String, quand: String) {
        runCatching {
            val r = sonde.lire()
            carnet.add(Carnet.MESURE, id, "quand=$quand;xruns=${r.xruns};cpu=${EssaiEclair.fmt(r.cpu.toDouble())};peak=${EssaiEclair.fmt(r.peak.toDouble())};running=${r.running};playing=${r.playing}")
        }
    }

    /** What a ❌ keeps: a picture of the app's windows, the last lines of the app's journal and the last measurements. Returns the picture's file name (empty: none). */
    private fun preuve(id: String): String {
        val shot = File(captures, "$id.jpg")
        val ok = runCatching { appli.capture(shot) }.getOrDefault(false)
        val journal = runCatching { appli.journalFin(JOURNAL_LIGNES) }.getOrDefault("").lines().filter { it.isNotBlank() }
        carnet.add(Carnet.NOTE, id, "journal : " + (if (journal.isEmpty()) "vide" else journal.joinToString(" | ").take(JOURNAL_MAX)))
        val mesures = carnet.lines().filter { it.kind == Carnet.MESURE && it.step == id }.takeLast(2).joinToString(" | ") { it.text }
        carnet.add(Carnet.NOTE, id, "mesures : " + mesures.ifEmpty { "aucune" })
        if (!ok) carnet.add(Carnet.NOTE, id, "pas de capture")
        return if (ok) shot.name else ""
    }

    companion object {
        /** « Ça tape ? »: jump up, house, trap (the 808s), techno. */
        val STYLES = listOf("jump_up", "house", "trap", "techno")

        const val SANS_CASQUE = "passée : pas de casque branché"
        const val SANS_BLUETOOTH = "passée : pas de Bluetooth"

        /** 30 s of listening and the time to answer. */
        const val ATTENTE_MS = 90_000L
        const val BLUETOOTH_MS = 240_000L
        const val JOURNAL_LIGNES = 25
        const val JOURNAL_MAX = 1_500

        /** A logbook line of an answer: « verdict;capture;comment » (the comment last: it may hold a « ; »). */
        fun reponseLigne(v: Verdict, capture: String, commentaire: String) = "${v.mot};$capture;${commentaire.replace('\n', ' ')}"

        /** The answers of the logbook, the last one of each question, in the order they were given. */
        data class Ligne(val id: String, val verdict: Verdict, val capture: String, val commentaire: String)

        fun reponses(lines: List<Carnet.Ligne>): List<Ligne> =
            lines.filter { it.kind == Carnet.REPONSE }.mapNotNull { l ->
                val f = l.text.split(';', limit = 3)
                Verdict.of(f[0])?.let { Ligne(l.step, it, f.getOrElse(1) { "" }, f.getOrElse(2) { "" }) }
            }.associateBy { it.id }.values.toList()

        /** « 5 oui, 1 non, 2 passées ». */
        fun verdict(lines: List<Carnet.Ligne>): String {
            val r = reponses(lines)
            fun n(v: Verdict) = r.count { it.verdict == v }
            return buildList {
                add("${n(Verdict.OUI)} oui")
                add("${n(Verdict.NON)} non")
                if (n(Verdict.PASSEE) > 0) add("${n(Verdict.PASSEE)} passée" + if (n(Verdict.PASSEE) > 1) "s" else "")
                if (n(Verdict.SANS_REPONSE) > 0) add("${n(Verdict.SANS_REPONSE)} sans réponse")
            }.joinToString(", ")
        }

        /** rapport.md's section. */
        fun section(lines: List<Carnet.Ligne>, etapes: List<Etape>): String = buildString {
            val r = reponses(lines).associateBy { it.id }
            if (r.isEmpty()) return ""
            appendLine("## Questions à l'oreille")
            appendLine()
            appendLine("| Question | Réponse | Un mot | Capture |")
            appendLine("|---|---|---|---|")
            for (e in etapes) {
                val a = r[e.name] ?: continue
                val icone = when (a.verdict) { Verdict.OUI -> "✅"; Verdict.NON -> "❌"; else -> "⏭" }
                appendLine("| ${e.label} | $icone ${a.verdict.mot} | ${a.commentaire.replace("|", "/")} | ${if (a.capture.isEmpty()) "" else "captures/${a.capture}"} |")
            }
        }

        fun nombre(v: Double) = String.format(Locale.ROOT, "%.2f", v)
    }
}
