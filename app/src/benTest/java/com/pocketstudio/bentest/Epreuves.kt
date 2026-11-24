package com.pocketstudio.bentest

import com.pocketstudio.app.journey.Journey
import java.io.File
import java.util.Locale

/**
 * Lot A2 (docs/audits/ultra/PLAN-APRES-ROBOT.md §3): the robot's trials of what a phone does to the app
 * besides the screens: another app taking the sound, a kit changed while it plays. Each starts from a
 * clean song (the assistant's [STYLE], made anew) and leaves its light ([Feu]) and its numbers
 * (« bilan;… ») in the logbook. They run in « Tout tester » ([ToutTester.etapes]), not in the essai éclair.
 */
class Epreuves(
    private val j: Journey,
    private val sonde: Sonde,
    private val appli: Appli,
    private val carnet: Carnet,
    captures: File,
    private val ecouteMs: Long = 20_000,
    ecran: () -> List<String> = { j.tagsMatching(Regex(".+")) },
    /** How long the sound is held by « another app » (each of its two ways). */
    private val perteMs: Long = 5_000,
    /** A clean song before each trial: the assistant's [STYLE] made anew (the tests on the real screens start from a fresh project). */
    private val nouveauMorceau: ((String) -> Unit)? = null,
) {
    private val e = EssaiEclair(j, sonde, appli, carnet, captures, ecouteMs, ecran)
    private val t = ToutTester(j, sonde, appli, carnet, captures, ecouteMs, ecran)

    fun etapes(): List<Etape> = listOf(
        etape(PERTE, "Perte du son au profit d'une autre appli", 240_000) { perteSon() },
        etape(KIT, "Changer de kit pendant la lecture", 240_000) { changementKit() },
    )

    private fun etape(name: String, label: String, maxMs: Long, work: () -> String) = Etape(name, label, maxMs) {
        try {
            work()
        } catch (t: Throwable) {
            e.noterEcran(name)
            throw t
        }
    }

    private fun feu(step: String, f: Pair<Feu, String>) = carnet.add(Carnet.FEU, step, f.first.mot + ";" + f.second)

    private fun bilan(step: String, vararg fields: Pair<String, Any?>) =
        carnet.add(Carnet.MESURE, step, "bilan;" + fields.joinToString(";") { (k, v) -> "$k=${v ?: ""}" })

    private fun nonMesure(step: String, why: String): String {
        feu(step, Feu.GRIS to why)
        carnet.add(Carnet.NOTE, step, "${ToutTester.NON_MESURE};$why")
        return "non mesuré : $why"
    }

    /** A clean song, the studio with its Pro tabs, nothing playing. */
    private fun propre(step: String) {
        val make = nouveauMorceau
        if (make != null) {
            make(step)
        } else {
            e.versCreer(STYLE)
            j.createNow()
            j.await("guide_card_start.ready", 300_000)
        }
        e.auStudioPro()
    }

    private fun attendre(ms: Long, ok: () -> Boolean): Boolean {
        val end = System.currentTimeMillis() + ms
        while (true) {
            if (ok()) return true
            if (System.currentTimeMillis() >= end) return false
            j.idle(250)
        }
    }

    /**
     * Another app takes the sound twice: first a notification that only lowers it (« duck »: the song must play on,
     * heard), then a call that takes it (the song may stop, never play on silent). Once given back, either the song
     * still plays with sound, or ▶ starts it again with sound; the studio in front, no crash.
     */
    fun perteSon(): String {
        val step = PERTE
        propre(step)
        e.lancerLecture()
        var duck: Ecoute.Bilan? = null
        if (!appli.prendreSon(duck = true) { duck = t.ecouter(step, perteMs) }) {
            e.arreter()
            return nonMesure(step, "le robot ne peut pas prendre le son ici")
        }
        var pris: Ecoute.Bilan? = null
        val grantedFull = appli.prendreSon(duck = false) { pris = t.ecouter(step, perteMs) }
        j.idle(1_000)
        val stoppedBack = !sonde.lire().playing
        val relance = if (stoppedBack) e.lancerLecture() else null
        val apres = t.ecouter(step, ecouteMs / 2)
        e.arreter()
        val d = duck!!
        val p = pris
        val devant = appli.studioDevant()
        bilan(
            step, "baisse_en_lecture" to d.playing, "baisse_lectures" to d.reads, "baisse_coupures" to coupures(d).size,
            "pris_en_lecture" to (p?.playing ?: ""), "pris_lectures" to (p?.reads ?: ""), "pris_muet_en_lecture" to (p?.let(::muetEnLecture) ?: ""),
            "relance" to (relance != null), "apres_coupures" to coupures(apres).size, "devant" to devant,
        )
        val rouge = ArrayList<String>()
        if (d.playing < d.reads) rouge += "la lecture s'est arrêtée quand une notification a seulement baissé le son"
        coupures(d).forEach { rouge += "son baissé : $it" }
        if (p != null && muetEnLecture(p) > 0) rouge += "son pris : ${muetEnLecture(p)} s en lecture mais muet"
        if (apres.playing < apres.reads) rouge += "la lecture ne repart pas une fois le son rendu"
        coupures(apres).forEach { rouge += "après : $it" }
        if (devant == false) rouge += "le studio n'est plus devant"
        val orange = if (p != null && p.playing == p.reads) listOf("la lecture a continué pendant que l'autre appli avait le son") else emptyList()
        feu(step, (if (rouge.isNotEmpty()) Feu.ROUGE else if (orange.isNotEmpty()) Feu.ORANGE else Feu.VERT) to (rouge + orange).joinToString(", "))
        check(rouge.isEmpty()) { rouge.joinToString(" ; ") }
        val how = when {
            !grantedFull -> "l'autre appli n'a pas eu le son"
            relance != null -> "la lecture s'est arrêtée, ▶ l'a relancée"
            else -> "la lecture a continué"
        }
        return "son baissé : ${d.text()} ; son pris : $how ; ensuite ${apres.text()}"
    }

    /**
     * Two kits put on the drums while the song plays (« Changer le son » on the drums' name, a kit touched):
     * no quarter second silent while it plays, no jump, no NaN, the song plays on.
     */
    fun changementKit(): String {
        val step = KIT
        propre(step)
        e.lancerLecture()
        val reads = ArrayList<Ecoute.Lu>()
        val chosen = ArrayList<String>()
        var fewer = false
        var n = 0
        val b = t.ecouter(step, ecouteMs) { lu ->
            reads += lu
            n++
            if ((n == KIT_PREMIER || n == KIT_SECOND) && !fewer) {
                val k = choisirKit(if (n == KIT_PREMIER) 1 else 0)
                if (k == null) fewer = true else chosen += k
            }
        }
        e.arreter()
        if (fewer) return nonMesure(step, "moins de 2 kits proposés")
        val trous = trous(reads)
        bilan(step, "kits" to chosen.joinToString(","), "trous" to trous, "coupures" to coupures(b).size, "en_lecture" to b.playing, "lectures" to b.reads)
        val rouge = ArrayList<String>()
        if (chosen.size < 2) rouge += "${chosen.size} kit(s) changé(s) sur 2"
        if (b.playing < b.reads) rouge += "la lecture s'est arrêtée"
        if (trous > 0) rouge += "$trous quart(s) de seconde muet(s) en lecture (trou de plus de 200 ms)"
        coupures(b).forEach { rouge += it }
        feu(step, (if (rouge.isEmpty()) Feu.VERT else Feu.ROUGE) to rouge.joinToString(", "))
        check(rouge.isEmpty()) { rouge.joinToString(" ; ") }
        return "kits ${chosen.joinToString(" puis ")} pendant la lecture ; ${b.text()}"
    }

    /** The drums' « Changer le son », then the [index]th kit offered; its tag, null when there are not 2 kits. */
    private fun choisirKit(index: Int): String? {
        if (!j.exists(DRUMS_NAME)) {
            if (!j.exists("structure_timeline")) j.tap("tab_Structure")
            // The Structure shows its parts: its switch to the timeline, where each track has its name.
            if (!j.exists(DRUMS_NAME) && j.exists("structure_timeline")) j.tap("structure_timeline")
            j.await(DRUMS_NAME)
        }
        j.tap(DRUMS_NAME)
        j.await("kit_sheet")
        val choices = j.tagsMatching(Regex("kit_choice_.+"))
        if (choices.size < 2) {
            j.tap("kit_sheet_close")
            j.awaitGone("kit_sheet", 5_000)
            return null
        }
        val tag = choices[index]
        j.tap(tag)
        if (!attendre(5_000) { !j.exists("kit_sheet") }) {
            j.tap("kit_sheet_close")
            j.awaitGone("kit_sheet", 5_000)
        }
        return tag.removePrefix("kit_choice_")
    }

    companion object {
        const val PERTE = "perte_son"
        const val KIT = "changement_kit"

        /** A light, common style: the trials measure the phone, not the heaviest song. */
        const val STYLE = "house"

        /** The drums' name on the Structure's timeline (SongPart.DRUMS): « Changer le son ». */
        const val DRUMS_NAME = "timeline_name_drums"

        /** The readings at which a kit is put on (one second, then three seconds in). */
        const val KIT_PREMIER = 4
        const val KIT_SECOND = 12

        /** A hole: the stream's longest gap over this (ms), when it says its block. */
        const val TROU_MS = 200f

        /** The seconds that played heard as silence (the engine says it plays, nothing comes out). */
        fun muetEnLecture(b: Ecoute.Bilan): Int = b.secondes.count { it.playing && it.peakDb < Ecoute.SILENCE_DB }

        /** Quarter seconds that played silent (each is over 200 ms of nothing), outside a quiet part the song plans. */
        fun trous(reads: List<Ecoute.Lu>): Int = reads.count {
            it.playing && Ecoute.db(if (it.peak.isFinite()) it.peak else 0f) < Ecoute.SILENCE_DB && (it.lane == null || it.lane >= Ecoute.LANE_QUIET)
        }

        /** What breaks the sound in a listening: silence, a jump, NaN, a gap between callbacks of 200 ms or more. */
        fun coupures(b: Ecoute.Bilan): List<String> = buildList {
            b.alertes.filter { it.kind == "SILENCE" || it.kind == "SAUT" }.forEach { add(it.text()) }
            muetEnLecture(b).takeIf { it > 0 && b.alertes.none { a -> a.kind == "SILENCE" } }?.let { add("$it s muette(s) en lecture") }
            if (b.nonFinite) add("valeur invalide (NaN) dans le son")
            if (b.blocMs > 0f && b.gapWorst * b.blocMs >= TROU_MS) add(String.format(Locale.ROOT, "écart de %.0f ms entre deux blocs", b.gapWorst * b.blocMs))
        }
    }
}
