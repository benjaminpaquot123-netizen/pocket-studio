package com.pocketstudio.bentest

import com.pocketstudio.app.journey.Journey
import com.pocketstudio.bentest.ToutTester.Companion.fmt
import com.pocketstudio.bentest.ToutTester.Companion.pctD
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
    /** The long listening (« Endurance »), 10 minutes on the phone. */
    private val enduranceMs: Long = 600_000,
    /** Round trips Mes projets › the song for « Fuite mémoire ». */
    private val allers: Int = 20,
    /** Each screen's frames are counted this long (« Images lentes »). */
    private val imagesMs: Long = ToutTester.IMAGES_MS,
) {
    private val e = EssaiEclair(j, sonde, appli, carnet, captures, ecouteMs, ecran)
    private val t = ToutTester(j, sonde, appli, carnet, captures, ecouteMs, ecran)

    fun etapes(): List<Etape> = listOf(
        etape(PERTE, "Perte du son au profit d'une autre appli", 240_000) { perteSon() },
        etape(KIT, "Changer de kit pendant la lecture", 240_000) { changementKit() },
        etape(ENDURANCE, "Endurance : 10 minutes de lecture", enduranceMs + 300_000) { endurance() },
        etape(FUITE, "Fuite mémoire : ouvrir et fermer le projet $allers fois", 600_000) { fuiteMemoire() },
        etape(FROID, "Démarrage à froid", 240_000) { demarrageFroid() },
        etape(BRUTAL, "Sauvegarde après un arrêt brutal", 240_000) { arretBrutal() },
        etape(VIDEO, "Export vidéo avec son", 600_000) { videoSon() },
        etape(TAMPON, "Tampon large (« Très sûr ») contre normal", 300_000) { tamponLarge() },
        etape(IMAGES, "Images lentes par écran, avec un écran témoin", 300_000) { imagesTemoin() },
    )

    /**
     * The guided pack (« Tests guidés »): a gesture only a hand can do (an output changed, the battery saver), asked by the
     * banner. Never in « Tout tester » nor the night's run: nobody is there to answer.
     */
    fun guides(): List<Etape> = listOf(
        etape(SORTIE, "Changer de sortie : le son revient en moins de 2 s", 300_000) { sortieChange() },
        etape(ECO, "Économie d'énergie : la lecture tient", 300_000) { economie() },
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

    /** Sound heard right now: the song plays and its peak is over the silence. */
    private fun sonEntendu(): Boolean = sonde.lire().let { it.playing && Ecoute.db(if (it.peak.isFinite()) it.peak else 0f) >= Ecoute.SILENCE_DB }

    /**
     * Guided: Benjamin plugs or unplugs a headset (or a Bluetooth speaker) while the song plays. The sound must be heard
     * again within [RETOUR_MS] of his « Fait »; when the app stopped the song (an unplugged headset: Android's « noisy »),
     * within [RETOUR_MS] of ▶. The phone must see the output change, else « non mesuré ».
     */
    fun sortieChange(): String {
        val step = SORTIE
        propre(step)
        val avant = appli.materiel()
        e.lancerLecture()
        try {
            val r = appli.poser(Question(SORTIE, "Branche ou débranche ton casque (ou ton enceinte Bluetooth) maintenant, puis touche « Fait ».", GUIDE_MS, action = "Fait"), GUIDE_MS)
            if (r.verdict != Verdict.OUI) return nonMesure(step, "pas de sortie changée (${r.verdict.mot})")
            val apres = appli.materiel()
            if (apres == avant) return nonMesure(step, "le téléphone ne voit aucun changement de sortie")
            val arretee = !sonde.lire().playing
            if (arretee) e.lancerLecture()
            val t0 = System.currentTimeMillis()
            val revenu = attendre(RETOUR_MS) { sonEntendu() }
            val ms = System.currentTimeMillis() - t0
            val ensuite = t.ecouter(step, ecouteMs / 2)
            bilan(step, "arretee" to arretee, "revenu" to revenu, "retour_ms" to ms, "ensuite_coupures" to coupures(ensuite).size, "materiel" to "$avant>$apres")
            val rouge = ArrayList<String>()
            if (!revenu) rouge += "le son n'est pas revenu en ${RETOUR_MS / 1000} s" + if (arretee) " après ▶" else ""
            if (ensuite.playing < ensuite.reads) rouge += "la lecture s'arrête sur la nouvelle sortie"
            coupures(ensuite).forEach { rouge += "ensuite : $it" }
            feu(step, (if (rouge.isEmpty()) Feu.VERT else Feu.ROUGE) to rouge.joinToString(", "))
            check(rouge.isEmpty()) { rouge.joinToString(" ; ") }
            return (if (arretee) "la lecture s'est arrêtée, ▶ : " else "") + "son revenu en $ms ms ; ensuite ${ensuite.text()}"
        } finally {
            e.arreter()
        }
    }

    /**
     * Guided: Benjamin turns the battery saver on (the shade, without leaving the app); the robot reads that it is on,
     * then the song must play through without a hole. The saver is asked off again at the end.
     */
    fun economie(): String {
        val step = ECO
        propre(step)
        val r = appli.poser(Question(ECO, "Active l'économie d'énergie depuis le volet du haut (sans quitter l'appli), puis touche « Fait ».", GUIDE_MS, action = "Fait"), GUIDE_MS)
        if (r.verdict != Verdict.OUI) return nonMesure(step, "économie d'énergie pas activée (${r.verdict.mot})")
        try {
            val on = appli.economieEnergie() ?: return nonMesure(step, "le robot ne lit pas l'économie d'énergie ici")
            if (!on) return nonMesure(step, "l'économie d'énergie n'est pas allumée selon le téléphone")
            e.auStudioPro()
            e.lancerLecture()
            val b = try {
                t.ecouter(step, ecouteMs)
            } finally {
                e.arreter()
            }
            bilan(step, "economie" to on, "xruns" to b.xruns, "coupures" to coupures(b).size, "en_lecture" to b.playing, "lectures" to b.reads)
            val rouge = ArrayList<String>()
            if (b.playing < b.reads) rouge += "la lecture s'est arrêtée en économie d'énergie"
            coupures(b).forEach { rouge += it }
            val orange = if (b.xruns > 0) listOf("${b.xruns} décrochage(s) en économie d'énergie") else emptyList()
            feu(step, (if (rouge.isNotEmpty()) Feu.ROUGE else if (orange.isNotEmpty()) Feu.ORANGE else Feu.VERT) to (rouge + orange).joinToString(", "))
            check(rouge.isEmpty()) { rouge.joinToString(" ; ") }
            return "économie d'énergie allumée : ${b.text()}"
        } finally {
            appli.poser(Question("${ECO}_fin", "Tu peux couper l'économie d'énergie, puis touche « Fait ».", GUIDE_MS, action = "Fait"), GUIDE_MS)
        }
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

    /** Ten minutes of the song: it never stops, no hole, no jump, no NaN; the memory at the start and the end (MB). */
    fun endurance(): String {
        val step = ENDURANCE
        propre(step)
        e.lancerLecture()
        val avant = appli.memoireMo()
        val reads = ArrayList<Ecoute.Lu>()
        val b = t.ecouter(step, enduranceMs) { reads += it }
        val apres = appli.memoireMo()
        e.arreter()
        val trous = trous(reads)
        bilan(step, "secondes" to enduranceMs / 1000, "en_lecture" to b.playing, "lectures" to b.reads, "xruns" to b.xruns, "trous" to trous,
            "coupures" to coupures(b).size, "memoire_debut_mo" to avant, "memoire_fin_mo" to apres)
        val rouge = ArrayList<String>()
        if (b.playing < b.reads) rouge += "la lecture s'est arrêtée"
        if (trous > 0) rouge += "$trous quart(s) de seconde muet(s) en lecture"
        coupures(b).forEach { rouge += it }
        val orange = listOfNotNull(memoireMonte(avant, apres)?.let { "mémoire $it" })
        feu(step, (if (rouge.isNotEmpty()) Feu.ROUGE else if (orange.isNotEmpty()) Feu.ORANGE else Feu.VERT) to (rouge + orange).joinToString(", "))
        check(rouge.isEmpty()) { rouge.joinToString(" ; ") }
        return "${enduranceMs / 1000} s de lecture : ${b.text()} ; mémoire ${mo(avant)} puis ${mo(apres)}"
    }

    /** [allers] round trips Mes projets › « Continuer »: the same song each time, the memory at most 20 % over the start's. */
    fun fuiteMemoire(): String {
        val step = FUITE
        propre(step)
        e.arreter()
        val id = appli.projet()?.id
        val avant = appli.memoireMo() ?: return nonMesure(step, "mémoire de l'appli illisible ici")
        repeat(allers) {
            if (!j.exists("top_projects")) j.tap("tab_Structure")
            j.tap("top_projects")
            j.await("projects")
            j.tap("projects_back")
            j.await("launch")
            j.continueLastProject()
            j.awaitAny(*EssaiEclair.STUDIO, timeoutMs = 30_000)
        }
        val apres = appli.memoireMo()
        val back = appli.projet()?.id
        bilan(step, "allers" to allers, "memoire_debut_mo" to avant, "memoire_fin_mo" to apres, "meme_projet" to (back == id))
        val rouge = ArrayList<String>()
        if (id != null && back != id) rouge += "« Continuer » rouvre un autre projet"
        memoireMonte(avant, apres)?.let { rouge += "mémoire $it après $allers allers-retours" }
        feu(step, (if (rouge.isEmpty()) Feu.VERT else Feu.ROUGE) to rouge.joinToString(", "))
        check(rouge.isEmpty()) { rouge.joinToString(" ; ") }
        return "$allers allers-retours Mes projets › Continuer : mémoire ${mo(avant)} puis ${mo(apres)}"
    }

    /** The studio closed then opened again: its time to the song (« Continuer » included), the same song. */
    fun demarrageFroid(): String {
        val step = FROID
        propre(step)
        e.arreter()
        val id = appli.projet()?.id
        val t0 = System.currentTimeMillis()
        if (!appli.relancerStudio()) return nonMesure(step, "le studio ne se ferme et ne se rouvre pas d'ici")
        rouvrir()
        val s = (System.currentTimeMillis() - t0) / 1000.0
        val back = appli.projet()?.id
        bilan(step, "secondes" to fmt(s), "meme_projet" to (back == id))
        val rouge = ArrayList<String>()
        if (id != null && back != id) rouge += "rouvert sur un autre projet"
        val f = when {
            rouge.isNotEmpty() || s > FROID_ROUGE_S -> Feu.ROUGE
            s > FROID_ORANGE_S -> Feu.ORANGE
            else -> Feu.VERT
        }
        if (s > FROID_ORANGE_S) rouge += "${fmt(s)} s pour revenir au morceau"
        feu(step, f to rouge.joinToString(", "))
        check(f != Feu.ROUGE) { rouge.joinToString(" ; ") }
        return "studio rouvert sur le morceau en ${fmt(s)} s"
    }

    /** The tempo raised by one, the studio closed at once (no wait for a save): opened again, the tempo is kept. */
    fun arretBrutal(): String {
        val step = BRUTAL
        propre(step)
        e.arreter()
        val avant = appli.projet()?.tempo ?: return nonMesure(step, "projet illisible ici")
        ouvrirTempo()
        j.tap("tempo_plus")
        val monte = attendre(5_000) { (appli.projet()?.tempo ?: avant) > avant }
        val voulu = appli.projet()?.tempo ?: avant
        fermerTempo()
        if (!monte) error("le tempo ne monte pas avec « + » : ${fmt(avant)} bpm")
        if (!appli.relancerStudio()) {
            ouvrirTempo()
            j.tap("tempo_minus")
            fermerTempo()
            return nonMesure(step, "le studio ne se ferme et ne se rouvre pas d'ici")
        }
        rouvrir()
        val apres = appli.projet()?.tempo
        bilan(step, "tempo_avant" to fmt(avant), "tempo_voulu" to fmt(voulu), "tempo_rouvert" to apres?.let(::fmt))
        val ok = apres != null && kotlin.math.abs(apres - voulu) < 0.01
        feu(step, (if (ok) Feu.VERT else Feu.ROUGE) to if (ok) "" else "tempo rouvert ${apres?.let(::fmt) ?: "?"} bpm au lieu de ${fmt(voulu)}")
        check(ok) { "le tempo changé juste avant l'arrêt est perdu : ${apres?.let(::fmt) ?: "?"} bpm au lieu de ${fmt(voulu)}" }
        // Back as it was, for the trials after.
        ouvrirTempo()
        j.tap("tempo_minus")
        fermerTempo()
        return "tempo ${fmt(avant)} puis ${fmt(voulu)} bpm, studio fermé aussitôt : rouvert à ${fmt(apres!!)} bpm"
    }

    /** « Vidéo pour les réseaux »: the MP4 read back has sound (its peak over −50 dBFS), not only the picture. */
    fun videoSon(): String {
        val step = VIDEO
        propre(step)
        e.arreter()
        val text = t.export("video")
        val file = appli.video().file ?: error("vidéo : pas de fichier")
        val r = appli.relire(file) ?: error("vidéo illisible")
        bilan(step, "crete" to fmt(r.creteDbtp), "duree" to fmt(r.secondes))
        val muet = !sonVideo(r)
        feu(step, (if (muet) Feu.ROUGE else Feu.VERT) to if (muet) "vidéo sans son (crête ${fmt(r.creteDbtp)} dBTP)" else "")
        check(!muet) { "la vidéo n'a pas de son : crête ${fmt(r.creteDbtp)} dBTP" }
        return "$text ; son présent"
    }

    /** The same song listened with the normal buffer, then « Très sûr » (the largest): no hole, no jump; then the normal back. */
    fun tamponLarge(): String {
        val step = TAMPON
        propre(step)
        val normal = ecouteTampon(step)
        reglerTampon(TAMPON_LARGE)
        val large = try {
            ecouteTampon(step)
        } finally {
            reglerTampon(TAMPON_NORMAL)
        }
        bilan(step, "normal_xruns" to normal.xruns, "normal_coupures" to coupures(normal).size, "large_xruns" to large.xruns,
            "large_coupures" to coupures(large).size, "large_en_lecture" to large.playing, "large_lectures" to large.reads, "large_bloc_ms" to fmt(large.blocMs.toDouble()))
        val rouge = ArrayList<String>()
        if (large.playing < large.reads) rouge += "la lecture s'est arrêtée avec le tampon large"
        coupures(large).forEach { rouge += "tampon large : $it" }
        val orange = if (large.xruns > normal.xruns) listOf("plus de décrochages avec le tampon large (${large.xruns} contre ${normal.xruns})") else emptyList()
        feu(step, (if (rouge.isNotEmpty()) Feu.ROUGE else if (orange.isNotEmpty()) Feu.ORANGE else Feu.VERT) to (rouge + orange).joinToString(", "))
        check(rouge.isEmpty()) { rouge.joinToString(" ; ") }
        return "normal : ${normal.text()} ; très sûr : ${large.text()}"
    }

    private fun ecouteTampon(step: String): Ecoute.Bilan {
        e.auStudioPro()
        e.lancerLecture()
        return try {
            t.ecouter(step, ecouteMs)
        } finally {
            e.arreter()
        }
    }

    /** Plus › Réglages › « Mode du son »: [bursts] chosen, then back to the song. */
    private fun reglerTampon(bursts: Int) {
        e.auStudioPro()
        ouvrirReglages()
        j.tap("buffer_$bursts")
        j.idle(500)
        j.tap("tab_Structure")
        e.auStudioPro()
    }

    /** Plus › Réglages; the Plus page may come back still on Réglages (its page is remembered), then no tile is touched. */
    private fun ouvrirReglages() {
        if (!j.exists("tool_Reglages") && !j.exists("settings")) j.tap(if (j.exists("tab_Plus")) "tab_Plus" else "thumb_more")
        j.awaitAny("tool_Reglages", "settings")
        if (!j.exists("settings")) j.tap("tool_Reglages")
        j.await("settings")
    }

    /**
     * Each screen's slow frames with the song playing, next to a still screen's (Réglages, the témoin): a screen is red
     * only when it is red by itself AND 10 points over the témoin (the phone's own slowness is not the app's).
     */
    fun imagesTemoin(): String {
        val step = IMAGES
        propre(step)
        val hz = appli.rafraichissementHz()
        e.lancerLecture()
        val parts = LinkedHashMap<String, Double?>()
        try {
            ouvrirReglages()
            parts[TEMOIN] = compter(step, TEMOIN)
            for ((name, open, mark) in EssaiEclair.SCREENS) {
                if (!j.exists(open)) j.tap("tab_Structure")
                j.tap(open)
                if (mark.endsWith("_")) {
                    check(attendre(15_000) { j.tagsMatching(Regex(Regex.escape(mark) + ".+")).isNotEmpty() }) { "$name : écran pas reconnu" }
                } else {
                    j.await(mark)
                }
                parts[name] = compter(step, name)
            }
        } finally {
            if (j.exists("projects") && !j.exists("launch")) runCatching { j.tap("projects_back") }
            e.auStudioPro()
            e.arreter()
        }
        val temoin = parts[TEMOIN]
        bilan(step, "hz" to hz?.let { fmt(it.toDouble()) }, *parts.map { (k, v) -> k to v?.let(::pctD) }.toTypedArray())
        if (parts.values.all { it == null }) return nonMesure(step, "images non comptées ici")
        val rouge = parts.filterKeys { it != TEMOIN }.filterValues { it != null && imagesRouge(it, temoin) }.map { (k, v) -> "$k ${pctD(v!!)} (témoin ${temoin?.let(::pctD) ?: "?"})" }
        val orange = parts.filterKeys { it != TEMOIN }.filterValues { it != null && !imagesRouge(it, temoin) && Feux.imagesLentes(it) != Feu.VERT }.keys.map { "$it lent comme le témoin" }
        feu(step, (if (rouge.isNotEmpty()) Feu.ROUGE else if (orange.isNotEmpty()) Feu.ORANGE else Feu.VERT) to (rouge + orange).joinToString(", "))
        check(rouge.isEmpty()) { "écrans lents : ${rouge.joinToString(", ")}" }
        return "écran à ${hz?.let { fmt(it.toDouble()) } ?: "?"} Hz ; " + parts.entries.joinToString(", ") { (k, v) -> "$k ${v?.let(::pctD) ?: "?"}" }
    }

    /** The slow frames' share on the screen now ([imagesMs]); null: none counted. */
    private fun compter(step: String, name: String): Double? {
        j.idle(1_000)
        appli.imagesDebut()
        j.idle(imagesMs)
        val (n, slow) = appli.imagesFin() ?: return null
        carnet.add(Carnet.MESURE, step, "ecran=$name;images=$n;lentes=$slow")
        return if (n == 0) null else slow.toDouble() / n
    }

    private fun ouvrirTempo() {
        e.auStudioPro()
        if (!j.exists("top_tempo")) j.tap("tab_Structure")
        j.tap("top_tempo")
        j.await("tempo_sheet")
    }

    private fun fermerTempo() {
        if (j.exists("tempo_sheet")) runCatching { j.tap("tempo_close"); j.awaitGone("tempo_sheet", 5_000) }
    }

    /** After [Appli.relancerStudio]: its opening, then « Continuer » or the song, the Pro tabs. */
    private fun rouvrir() {
        j.awaitAny("intro", "welcome", "launch", *EssaiEclair.STUDIO, timeoutMs = 60_000)
        j.awaitGone("intro", 60_000)
        e.auStudioPro()
    }

    private fun mo(v: Int?) = v?.let { "$it Mo" } ?: "inconnue"

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
        const val ENDURANCE = "endurance"
        const val FUITE = "fuite_memoire"
        const val FROID = "demarrage_froid"
        const val BRUTAL = "arret_brutal"
        const val VIDEO = "video_son"
        const val TAMPON = "tampon_large"
        const val IMAGES = "images_temoin"
        const val TEMOIN = "reglages"

        /** « Mode du son »: the largest buffer the app offers (« Très sûr », no 8), and the normal (« Rapide »). */
        const val SORTIE = "sortie_change"
        const val ECO = "economie_energie"
        /** The sound back within this after the output changed (or after ▶ when the app stopped it). */
        const val RETOUR_MS = 2_000L
        /** How long a guided question waits for Benjamin. */
        const val GUIDE_MS = 120_000L
        const val TAMPON_LARGE = 6
        const val TAMPON_NORMAL = 2

        /** The studio back to the song within this (s): green; over the red: red. */
        const val FROID_ORANGE_S = 8.0
        const val FROID_ROUGE_S = 20.0

        /** The memory may grow this much (a fifth) before it says « leak ». */
        const val MEMOIRE_MARGE = 0.20

        /** A screen's slow frames over the témoin's by this many points (and red by itself): red. */
        const val ECART_TEMOIN = 0.10

        /** « +35 Mo (+25 %) » when [apres] is over [avant] by more than [MEMOIRE_MARGE]; null: fine or unknown. */
        fun memoireMonte(avant: Int?, apres: Int?): String? {
            if (avant == null || apres == null || avant <= 0) return null
            val r = (apres - avant).toDouble() / avant
            return if (r > MEMOIRE_MARGE) String.format(Locale.ROOT, "+%d Mo (+%.0f %%)", apres - avant, r * 100) else null
        }

        /** Red: red by itself ([Feux.imagesLentes]) and [ECART_TEMOIN] over the still screen (unknown témoin: by itself). */
        fun imagesRouge(part: Double, temoin: Double?): Boolean =
            Feux.imagesLentes(part) == Feu.ROUGE && part > (temoin ?: 0.0) + ECART_TEMOIN

        /** The video has sound: its peak read back over the silence. */
        fun sonVideo(r: Relecture.Relu): Boolean = r.creteDbtp.isFinite() && r.creteDbtp > Ecoute.SILENCE_DB

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
