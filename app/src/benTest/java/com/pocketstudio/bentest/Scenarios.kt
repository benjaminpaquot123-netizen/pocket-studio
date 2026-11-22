package com.pocketstudio.bentest

import com.pocketstudio.app.journey.Journey
import com.pocketstudio.app.report.JsonText
import java.io.File
import java.util.Locale

/**
 * « Scénarios » : a few minutes of real use, by level (beginner, intermediate, pro), each scenario
 * with an oracle that reads one precise result: the engine's meters, the project in memory, the
 * project read back from the disk, the exported file. It stands in for a good part of a bench round:
 * the same gestures as a tester's seat, on the phone itself, with the true engine.
 *
 * Every scenario is a step of the run ([etapes]); the gestures are public, one by one, so that a
 * test can play them on the real screens.
 */
class Scenarios(
    private val j: Journey,
    private val sonde: Sonde,
    private val appli: Appli,
    private val carnet: Carnet,
    private val captures: File,
    private val ecran: () -> List<String> = { j.tagsMatching(Regex(".+")) },
    /** How long the beginner's new song is listened to, then the long listening for dropouts. */
    private val ecouteMs: Long = 20_000,
    private val decrochagesMs: Long = 30_000,
    /** How long the app may take to write a change on the disk (its saving is not immediate). */
    private val sauvegardeMs: Long = 20_000,
    /** How long the Live's ▶ may take to start the engine, or its second touch to stop it. */
    private val liveMs: Long = 10_000,
    private val liveEcouteMs: Long = 4_000,
    /** How long the song is listened to with its new effect. */
    private val effetEcouteMs: Long = 6_000,
) {
    private val e = EssaiEclair(j, sonde, appli, carnet, captures, ecouteMs, ecran)
    private val t = ToutTester(j, sonde, appli, carnet, captures, ecouteMs, ecran)

    fun etapes(): List<Etape> = LISTE.map { s ->
        etape(s.id, s.label, s.maxMs) {
            when (s.id) {
                DEBUTANT -> debutant()
                ECRANS -> e.ecrans()
                PROJET -> projet()
                STRUCTURE -> structure()
                EFFETS -> effets()
                LIVE -> live()
                EXPORT -> t.export("m4a")
                LATENCE -> t.latence()
                else -> decrochages()
            }
        }
    }

    /**
     * A step that starts from the studio whatever the one before left ([remiseAZero]), and whose failure
     * leaves what the robot saw in the logbook.
     */
    private fun etape(name: String, label: String, maxMs: Long, work: () -> String) = Etape(name, label, maxMs) { deadline ->
        val text = try {
            son(name)
            remiseAZero(name)
            work()
        } catch (t: Throwable) {
            e.noterEcran(name)
            indices(name)
            throw t
        }
        // A failure by a measure (a red threshold) or by the time: the same clues as when the robot gets lost.
        if (rouge(name) || System.currentTimeMillis() > deadline) indices(name)
        text
    }

    /** The app died in [step] (noted as a crash by [Passage]): its clues. */
    fun apresPlantage(step: String) = Unit

    /** [step] has a red light since it last began. */
    private fun rouge(step: String): Boolean {
        val lines = carnet.lines()
        val from = lines.indexOfLast { it.step == step && it.kind == Carnet.DEBUT }.coerceAtLeast(0)
        return lines.drop(from).any { it.step == step && it.kind == Carnet.FEU && Feu.of(it.text.substringBefore(';')) == Feu.ROUGE }
    }

    /**
     * The phone's sound as the scenario begins (« Son : coupé / allumé » at the head of resume.md). A run with the
     * sound cut is a normal one: only the measures through the speaker are « non mesuré (son coupé) ».
     */
    private fun son(step: String) {
        val v = runCatching { appli.volume() }.getOrNull() ?: return
        carnet.add(Carnet.NOTE, step, "$SON;" + (if (v.coupe()) SON_COUPE else SON_ALLUME) + ";" + v.text())
    }

    /**
     * The clues of a failure, in the report's zip: a picture of the screen and every tag on it (the known
     * markers first), with where the robot was by Android ([Appli.studioDevant]); their paths in a note.
     */
    private fun indices(step: String) {
        val picture = File(captures, "echec-$step.jpg")
        val list = File(captures, "echec-$step-tags.txt")
        val shot = runCatching { captures.mkdirs(); appli.capture(picture) && picture.isFile }.getOrDefault(false)
        val written = runCatching {
            val all = ecran().distinct()
            val known = all.filter { it in EssaiEclair.REPERES }
            val devant = when (appli.studioDevant()) { true -> "oui"; false -> "non"; null -> "inconnu" }
            list.writeText(
                buildString {
                    appendLine("studio devant : $devant")
                    appendLine("repères : " + known.ifEmpty { listOf("aucun") }.joinToString(", "))
                    appendLine("tags à l'écran (${all.size}) :")
                    (known + (all - known.toSet())).forEach { appendLine(it) }
                },
            )
        }.isSuccess
        carnet.add(
            Carnet.NOTE, step,
            "$INDICE;capture=" + (if (shot) "captures/${picture.name}" else "") + ";tags=" + (if (written) "captures/${list.name}" else ""),
        )
    }

    private fun feu(step: String, f: Pair<Feu, String>) = carnet.add(Carnet.FEU, step, f.first.mot + ";" + f.second)

    /** A listening's light, and the red thresholds it passed (value read against threshold) for the clues. */
    private fun feuEcoute(step: String, b: Ecoute.Bilan) {
        feu(step, Feux.style(b))
        Feux.depasses(b).forEach { carnet.add(Carnet.NOTE, step, "${ToutTester.SEUIL};$it") }
    }

    /** The numbers a scenario leaves for scenarios.json (« bilan;k=v;… »). */
    private fun bilan(step: String, vararg fields: Pair<String, Any?>) =
        carnet.add(Carnet.MESURE, step, "bilan;" + fields.joinToString(";") { (k, v) -> "$k=${v ?: ""}" })

    private fun ecoute(step: String, b: Ecoute.Bilan) = bilan(
        step, "decrochages" to b.xruns, "pire_bloc" to fmt(b.cpuWorst), "plus_long_ecart" to fmt(b.gapWorst), "charge_moyenne" to fmt(b.cpuMean),
        "crete" to fmt(b.peakMax), "secondes" to b.reads / Ecoute.READS_PER_SECOND, "alertes" to b.alertes.size,
    )

    private fun attendre(ms: Long, ok: () -> Boolean): Boolean {
        val end = System.currentTimeMillis() + ms
        while (true) {
            if (ok()) return true
            if (System.currentTimeMillis() >= end) return false
            j.idle(250)
        }
    }

    /**
     * A sheet a failed scenario left open (the part's, the strip's) is closed before the next one:
     * the robot does not touch under another window, and one red must not make three.
     */
    private fun fermerFeuilles() {
        for (sheet in listOf("part_sheet", "strip_sheet")) {
            if (j.exists(sheet)) runCatching { j.tap("${sheet}_close"); j.awaitGone(sheet, 5_000) }
        }
    }

    /**
     * The studio's main screen in front before each scenario, whatever the one before left. The robot knows
     * where it is by the [Tracker] (the studio's activity resumed or not: [Appli.studioDevant]) and by the
     * screen's markers: BenStudio Test's own screen in front (a run started there, the phone's first run of
     * 2026-11-22: 9 scenarios waiting for a tab_Structure that was never opened), a sheet or the assistant
     * over the song, a full-screen view hiding the bar (Android's « retour »).
     */
    private fun remiseAZero(step: String) {
        repeat(ESSAIS_REMISE) { essai ->
            val tags = ecran()
            val devant = appli.studioDevant() ?: tags.any { !it.startsWith("bentest_") }
            if (!devant) {
                carnet.add(Carnet.NOTE, step, "remise à zéro : le studio n'est pas devant, il est rouvert")
                appli.ouvrirStudio()
                runCatching { j.awaitAny("intro", "welcome", "launch", "projects", "wizard", *EssaiEclair.STUDIO, timeoutMs = 30_000) }
                runCatching { j.awaitGone("intro", 60_000) }
            }
            runCatching { fermerFeuilles() }
            runCatching { e.fermerAssistant() }
            if (PRET.any { j.exists(it) }) return
            if (essai < ESSAIS_REMISE - 1) {
                carnet.add(Carnet.NOTE, step, "remise à zéro : ni barre ni accueil à l'écran, « retour »")
                appli.retour()
                j.idle(800)
            }
        }
        error("remise à zéro : l'écran principal ne revient pas après $ESSAIS_REMISE essais")
    }

    /** The open song with the Pro tabs, no sheet over it. */
    private fun auStudio() {
        fermerFeuilles()
        e.auStudioPro()
    }

    private fun lu(): ProjetVu = appli.projet() ?: error("le projet ouvert ne se lit pas")

    /** Waits until the project on the disk says [ok]; the last one read (null: no file). */
    private fun surDisque(ok: (ProjetVu.Disque) -> Boolean): ProjetVu.Disque? {
        var d: ProjetVu.Disque? = null
        attendre(sauvegardeMs) { d = appli.projet()?.disque; d?.let(ok) == true }
        return d
    }

    /** Beginner: a song by the assistant, ▶, and sound comes out (no silence, no NaN, it keeps playing). */
    fun debutant(): String {
        val text = t.style(DEBUTANT, STYLE)
        return "morceau $STYLE par l'assistant ; $text"
    }

    /**
     * Intermediate: the song is kept without a « save » button. Oracle: the project read back from
     * the disk has the open one's name and parts, « Mes projets » lists it, and « Continuer » comes
     * back to the same project.
     */
    fun projet(): String {
        auStudio()
        val p = lu()
        val d = surDisque { it.nom == p.nom && it.parties == p.parties }
        check(d != null) { "aucun fichier de projet sur le disque pour « ${p.nom} » après ${sauvegardeMs / 1000} s" }
        check(d.nom == p.nom && d.parties == p.parties) { "le disque n'a pas le morceau ouvert : « ${d.nom} », ${d.parties} parties (ouvert : « ${p.nom} », ${p.parties} parties)" }
        if (!j.exists("top_projects")) j.tap("tab_Structure")
        j.tap("top_projects")
        j.await("projects")
        val listed = runCatching { j.await("project_${p.id}", 5_000) }.isSuccess
        // The list is lazy: with more projects than the screen shows, a row out of sight has no node,
        // so its absence proves nothing. Red only when the whole list is on screen.
        val rows = j.tagsMatching(Regex("project_.+")).size
        val hidden = !listed && rows >= LISTE_PLEINE
        j.tap("projects_back")
        j.tap("launch_continue")
        j.awaitAny(*EssaiEclair.STUDIO, timeoutMs = 30_000)
        check(listed || hidden) { "« ${p.nom} » n'est pas dans Mes projets" }
        val back = lu()
        check(back.id == p.id && back.parties == p.parties) { "« Continuer » ne rouvre pas le même morceau : « ${back.nom} », ${back.parties} parties" }
        bilan(PROJET, "parties" to p.parties, "sur_disque" to true)
        val list = if (listed) "dans Mes projets" else "pas vu dans Mes projets (liste plus longue que l'écran, non vérifié)"
        return "« ${p.nom} » relu sur le disque (${d.parties} parties), $list, rouvert par « Continuer »"
    }

    /** Intermediate: Structure › a part's « ⋯ » › « Break 4 mesures ». Oracle: one part more in the song, and on the disk. */
    fun structure(): String {
        auStudio()
        val before = lu().parties
        j.tap("tab_Structure")
        try {
            j.partIntent(PARTIE, "part_break_4")
        } finally {
            fermerFeuilles()
        }
        check(attendre(5_000) { lu().parties == before + 1 }) { "le break ne s'ajoute pas au morceau : ${lu().parties} parties (avant : $before)" }
        val d = surDisque { it.parties == before + 1 }
        check(d?.parties == before + 1) { "le break n'est pas enregistré : ${d?.parties ?: "aucun fichier"} parties sur le disque (attendu : ${before + 1})" }
        bilan(STRUCTURE, "parties_avant" to before, "parties_apres" to before + 1)
        return "break ajouté après la partie ${PARTIE + 1} : $before parties, puis ${before + 1}, enregistré"
    }

    /**
     * Pro: Mix › a strip's « ⋯ » › « Ajouter un effet » › Égaliseur. Oracle: one equaliser more on that
     * strip in the project, the same on the disk, and the song still sounds (no silence, no NaN).
     */
    fun effets(): String {
        auStudio()
        val before = lu().effets
        // The first strip with room for one more effect (the assistant's songs come with some).
        val ch = (0 until minOf(before.size, PISTES)).firstOrNull { before[it].size < ProjetVu.EFFETS_MAX } ?: 0
        fun n(p: List<List<String>>) = p.getOrNull(ch).orEmpty().count { it == EFFET }
        j.tap("tab_Mix")
        j.await("mixer")
        val chips = Regex("fx_chip_${ch}_\\d+")
        val added: Boolean
        val chip: Boolean
        try {
            j.tap("strip_more_$ch")
            j.await("strip_sheet")
            // The strip already shows the effects it came with: one chip more, not « a chip ».
            val shown = j.tagsMatching(chips).size
            j.tap("fx_add_$ch")
            j.await("fx_picker_$ch")
            j.tap("fx_pick_${ch}_$EFFET")
            added = attendre(5_000) { n(lu().effets) == n(before) + 1 }
            chip = attendre(5_000) { j.tagsMatching(chips).size == shown + 1 }
            // The list closed, then the sheet: the studio as a user leaves it.
            runCatching { if (j.exists("fx_picker_$ch")) j.tap("fx_add_$ch") }
        } finally {
            fermerFeuilles()
        }
        check(added) { "l'effet ne s'ajoute pas à la piste ${ch + 1} : ${lu().effets.getOrNull(ch).orEmpty().ifEmpty { listOf("aucun") }.joinToString(", ")}" }
        check(chip) { "l'effet est dans le projet mais la piste ${ch + 1} ne le montre pas" }
        val d = surDisque { n(it.effets) == n(before) + 1 }
        check(d != null && n(d.effets) == n(before) + 1) { "l'effet n'est pas enregistré sur le disque (piste ${ch + 1})" }
        val by = e.lancerLecture()
        val b = t.ecouter(EFFETS, effetEcouteMs)
        e.arreter()
        ecoute(EFFETS, b)
        feuEcoute(EFFETS, b)
        check(!b.nonFinite) { "valeur invalide (NaN) dans le son avec l'effet" }
        check(b.secondes.any { it.peakDb > Ecoute.SILENCE_DB }) { "silence avec l'effet : ${b.text()}" }
        return "égaliseur ajouté à la piste ${ch + 1}, enregistré ; lecture par $by : ${b.text()}"
    }

    /** Pro: the Live, its ▶, then its ▶ again. Oracle: the engine plays and sound comes out, then it stops. */
    fun live(): String {
        auStudio()
        e.arreter()
        j.tap("tab_Live")
        j.await("live_screen")
        j.tap("live_play")
        val started = attendre(liveMs) { sonde.lire().playing }
        var peak = 0f
        val heard = started && attendre(liveMs) { peak = maxOf(peak, sonde.lire().peak); peak > EssaiEclair.NIVEAU_MIN }
        val b = if (heard) t.ecouter(LIVE, liveEcouteMs) else null
        if (sonde.lire().playing) runCatching { j.tap("live_play") }
        val stopped = attendre(liveMs) { !sonde.lire().playing }
        // Out of the Live whatever happened: the next scenario starts from the song.
        runCatching { j.tap("tab_Structure") }
        check(started) { "le Live ne démarre pas : ▶ touché, le moteur ne joue pas" }
        check(heard) { "le Live joue mais aucun son ne sort : crête ${fmt(peak)}" }
        checkNotNull(b)
        ecoute(LIVE, b)
        feuEcoute(LIVE, b)
        check(stopped) { "le Live ne s'arrête pas : ▶ touché une seconde fois, le moteur joue encore" }
        check(!b.nonFinite) { "valeur invalide (NaN) dans le son du Live" }
        return "le Live joue (${b.text()}), puis s'arrête"
    }

    /** The song played [decrochagesMs]: no dropout, the worst block and the longest gap inside the thresholds. */
    fun decrochages(): String {
        auStudio()
        val by = e.lancerLecture()
        val b = t.ecouter(DECROCHAGES, decrochagesMs)
        e.arreter()
        ecoute(DECROCHAGES, b)
        feuEcoute(DECROCHAGES, b)
        check(b.reads > 0) { "aucune mesure" }
        check(b.playing * 2 >= b.reads) { "la lecture s'est arrêtée toute seule : ${b.text()}" }
        return "lecture par $by : ${b.text()}"
    }

    /** One scenario of the run: its step's name, what the report calls it, whose use it is. */
    data class Scenario(val id: String, val label: String, val niveau: String, val maxMs: Long)

    companion object {
        const val DEBUTANT = "debutant"
        const val ECRANS = "ecrans"
        const val PROJET = "projet"
        const val STRUCTURE = "structure"
        const val EFFETS = "effets"
        const val LIVE = "live"

        /** The names « Tout tester » writes its export's and its latency's lights under. */
        const val EXPORT = "export_m4a"
        const val LATENCE = "latence"
        const val DECROCHAGES = "decrochages"

        const val NIVEAU_DEBUTANT = "débutant"
        const val NIVEAU_MOYEN = "moyen"
        const val NIVEAU_PRO = "pro"
        const val NIVEAU_MESURE = "mesure"

        /** The beginner's song (a common one: the heavy styles are « Tout tester »'s). */
        const val STYLE = "house"

        /** The part the break goes after (the second: its sheet has every intention). */
        const val PARTIE = 1

        /** The effect added (short, never refused for being « long »), and the strips tried for a free slot. */
        const val EFFET = "EQ"
        const val PISTES = 4

        /** Rows of « Mes projets » from which the list may run past the screen. */
        const val LISTE_PLEINE = 5

        val LISTE = listOf(
            Scenario(DEBUTANT, "Débutant : un morceau par l'assistant, et il joue", NIVEAU_DEBUTANT, 420_000),
            Scenario(ECRANS, "Débutant : les cinq écrans s'ouvrent", NIVEAU_DEBUTANT, 120_000),
            Scenario(PROJET, "Moyen : le morceau est enregistré et se rouvre", NIVEAU_MOYEN, 120_000),
            Scenario(STRUCTURE, "Moyen : ajouter un break à la structure", NIVEAU_MOYEN, 120_000),
            Scenario(EFFETS, "Pro : un effet sur une piste du mixeur", NIVEAU_PRO, 180_000),
            Scenario(LIVE, "Pro : jouer puis arrêter le Live", NIVEAU_PRO, 120_000),
            Scenario(EXPORT, "Export M4A relu", NIVEAU_MOYEN, 600_000),
            Scenario(LATENCE, "Latence aller-retour", NIVEAU_MESURE, 120_000),
            Scenario(DECROCHAGES, "Décrochages audio (30 s de lecture)", NIVEAU_MESURE, 180_000),
        )

        /** [LISTE] as steps without work (to read a logbook). */
        /** The screens a scenario starts from: the studio (its bar, or its rail lying down), the welcome, the projects. */
        private val PRET = listOf(*EssaiEclair.STUDIO, "launch", "projects", "welcome")
        private const val ESSAIS_REMISE = 4
        private const val INDICE = "indice"
        private const val SON = "son"
        const val SON_COUPE = "coupé"
        const val SON_ALLUME = "allumé"

        private val NOMS = LISTE.map { s -> Etape(s.id, s.label, s.maxMs) { "" } }

        const val PASSE = "passé"
        const val ECHOUE = "échoué"
        const val NON_CONCLUANT = "non concluant"
        const val NON_MESURE = "non mesuré"
        const val PAS_JOUE = "pas joué"

        /** One scenario as the logbook has it. */
        data class Resultat(
            val scenario: Scenario, val feu: Feu?, val resultat: String, val detail: String, val secondes: Long?, val mesures: List<Pair<String, String>>,
            /** The failure's clues in the zip: the picture and the tag list (empty: not taken). */
            val indices: List<Pair<String, String>> = emptyList(),
            /** The thresholds a failure by measure passed, each as the value read against its threshold. */
            val seuils: List<String> = emptyList(),
        )

        /** Passed: green, or orange (it works, a number is to be watched); failed: red (the step failed, crashed, or a number is out). */
        fun resultats(lines: List<Carnet.Ligne>): List<Resultat> {
            val feux = Rapport.feux(lines, NOMS)
            return LISTE.map { s ->
                val from = lines.indexOfLast { it.step == s.id && it.kind == Carnet.DEBUT }.coerceAtLeast(0)
                val mine = lines.drop(from).filter { it.step == s.id }
                val begin = mine.firstOrNull { it.kind == Carnet.DEBUT }
                val end = mine.lastOrNull { it.kind == Carnet.REUSSI || it.kind == Carnet.ECHEC || it.kind == Carnet.PLANTAGE }
                val feu = feux[s.id]
                val why = mine.filter { it.kind == Carnet.FEU }.mapNotNull { l -> l.text.split(';', limit = 2).takeIf { Feu.of(it[0]) == feu }?.getOrNull(1)?.takeIf { it.isNotBlank() } }
                val mesures = mine.filter { it.kind == Carnet.MESURE }.flatMap { l -> champs(s.id, l.text) }
                // Nothing could be measured (the sound cut, too many failed measures): neither passed nor failed.
                val nonMesure = mine.lastOrNull { it.kind == Carnet.NOTE && it.text.startsWith("${ToutTester.NON_MESURE};") }
                    ?.text?.substringAfter(';')?.takeIf { feu == Feu.GRIS && end?.kind == Carnet.REUSSI }
                Resultat(
                    s, feu,
                    when (feu) {
                        null -> PAS_JOUE
                        Feu.VERT, Feu.ORANGE -> PASSE
                        Feu.GRIS -> if (nonMesure != null) NON_MESURE else NON_CONCLUANT
                        Feu.ROUGE -> ECHOUE
                    },
                    nonMesure ?: listOfNotNull(end?.text?.takeIf { it.isNotBlank() }, why.joinToString(", ").takeIf { it.isNotEmpty() && end?.kind == Carnet.REUSSI && feu != Feu.VERT }).joinToString(" ; "),
                    if (begin != null && end != null) (end.atMs - begin.atMs) / 1000 else null,
                    mesures + if (nonMesure != null) emptyList() else mediane(s.id, mine),
                    mine.filter { it.kind == Carnet.NOTE && it.text.startsWith("$INDICE;") }.lastOrNull()?.let { l -> l.text.removePrefix("$INDICE;").split(';').mapNotNull { f -> f.split('=', limit = 2).takeIf { it.size == 2 }?.let { it[0] to it[1] } } }.orEmpty(),
                    if (feu != Feu.ROUGE) emptyList() else mine.filter { it.kind == Carnet.NOTE && it.text.startsWith("${ToutTester.SEUIL};") }.map { it.text.substringAfter(';') }.distinct()
                        // A red without a threshold of its own (« fichier illisible »): its light's reason.
                        .ifEmpty { if (end?.kind == Carnet.REUSSI) why else emptyList() },
                )
            }
        }

        /** A scenario's own numbers: its « bilan » lines, and the export's reading of its file. */
        private fun champs(step: String, text: String): List<Pair<String, String>> {
            val keep = text.startsWith("bilan;") || (step == EXPORT && text.startsWith("octets="))
            if (!keep) return emptyList()
            return text.removePrefix("bilan;").split(';').mapNotNull { f -> f.split('=', limit = 2).takeIf { it.size == 2 && it[1].isNotEmpty() }?.let { it[0] to it[1] } }
        }

        /** The latency's median (the step writes each measure as « ms=… »). */
        private fun mediane(step: String, mine: List<Carnet.Ligne>): List<Pair<String, String>> {
            if (step != LATENCE) return emptyList()
            val ms = mine.filter { it.kind == Carnet.MESURE }.mapNotNull { it.text.removePrefix("ms=").takeIf { v -> v != it.text }?.toDoubleOrNull() }.sorted()
            return if (ms.isEmpty()) emptyList() else listOf("latence_ms" to fmt(ms[ms.size / 2]), "mesures" to ms.size.toString())
        }

        /** « 7 passés, 1 échoué, 1 pas joué ». */
        fun verdict(lines: List<Carnet.Ligne>): String {
            val r = resultats(lines)
            fun n(what: String) = r.count { it.resultat == what }
            return buildList {
                add("${n(PASSE)} passé" + if (n(PASSE) > 1) "s" else "")
                add("${n(ECHOUE)} échoué" + if (n(ECHOUE) > 1) "s" else "")
                if (n(NON_CONCLUANT) > 0) add("${n(NON_CONCLUANT)} non concluant" + if (n(NON_CONCLUANT) > 1) "s" else "")
                if (n(NON_MESURE) > 0) add("${n(NON_MESURE)} non mesuré" + if (n(NON_MESURE) > 1) "s" else "")
                if (n(PAS_JOUE) > 0) add("${n(PAS_JOUE)} pas joué" + if (n(PAS_JOUE) > 1) "s" else "")
            }.joinToString(", ")
        }

        /** scenarios.json: the verdict, the run's length, and each scenario (level, result, light, numbers). */
        fun json(lines: List<Carnet.Ligne>, about: List<Pair<String, String>>): String {
            val r = resultats(lines)
            val items = r.map { x ->
                val numbers = x.mesures.map { (k, v) -> k to (v.toLongOrNull()?.toString() ?: v.toFloatOrNull()?.let { JsonText.number(it) } ?: JsonText.string(v)) }
                JsonText.obj(
                    "id" to JsonText.string(x.scenario.id), "nom" to JsonText.string(x.scenario.label), "niveau" to JsonText.string(x.scenario.niveau),
                    "resultat" to JsonText.string(x.resultat), "feu" to x.feu?.let { JsonText.string(it.mot) }, "secondes" to x.secondes?.toString(),
                    "detail" to JsonText.string(x.detail), "mesures" to JsonText.obj(*numbers.toTypedArray()),
                )
            }
            val levels = niveaux(r).map { n ->
                JsonText.obj("niveau" to JsonText.string(n.niveau), "passes" to n.passes.toString(), "non_mesures" to n.nonMesures.toString(), "scenarios" to n.scenarios.toString())
            }
            return JsonText.obj(
                *about.map { (k, v) -> k to JsonText.string(v) }.toTypedArray(),
                "passage" to JsonText.string("scenarios"), "verdict" to JsonText.string(verdict(lines)), "tout_passe" to r.all { it.resultat == PASSE }.toString(),
                "secondes" to duree(lines)?.toString(), "niveaux" to JsonText.array(levels), "scenarios" to JsonText.array(items),
            )
        }

        /** resume.md: the same in plain French, short enough to be read aloud. */
        fun resume(lines: List<Carnet.Ligne>, about: List<Pair<String, String>>): String = buildString {
            val r = resultats(lines)
            appendLine("# BenStudio Test : scénarios")
            appendLine()
            appendLine("Son : ${son(lines) ?: "inconnu"}")
            appendLine()
            about.forEach { (k, v) -> appendLine("- $k : $v") }
            appendLine()
            appendLine("**Verdict : ${verdict(lines)}**" + (duree(lines)?.let { " (passage de ${it / 60} min ${it % 60} s)" } ?: ""))
            appendLine()
            niveaux(r).forEach { n ->
                appendLine("- Niveau ${n.niveau} : ${n.passes} sur ${n.scenarios}" + if (n.nonMesures > 0) " (${n.nonMesures} non mesuré" + (if (n.nonMesures > 1) "s)" else ")") else "")
            }
            appendLine()
            appendLine("| Scénario | Niveau | Résultat | Durée | Ce qu'il a lu |")
            appendLine("|---|---|---|---|---|")
            for (x in r) {
                val mark = when (x.resultat) {
                    PASSE -> if (x.feu == Feu.ORANGE) "🟠 passé, à surveiller" else "✅ passé"
                    ECHOUE -> "❌ échoué"
                    NON_CONCLUANT -> "⚪ non concluant"
                    NON_MESURE -> if (x.detail.startsWith(ToutTester.SON_COUPE)) "⚪ non mesuré (son coupé)" else "⚪ non mesuré"
                    else -> "pas joué"
                }
                appendLine("| ${x.scenario.label} | ${x.scenario.niveau} | $mark | ${x.secondes?.let { "$it s" } ?: ""} | ${x.detail.replace("|", "/").replace("\n", " ")} |")
            }
            val numbers = r.filter { it.mesures.isNotEmpty() }
            if (numbers.isNotEmpty()) {
                appendLine()
                appendLine("## Mesures")
                numbers.forEach { x -> appendLine("- ${x.scenario.label} : " + x.mesures.joinToString(", ") { (k, v) -> "${k.replace('_', ' ')} $v" }) }
            }
            val clues = r.filter { it.indices.isNotEmpty() || it.seuils.isNotEmpty() }
            if (clues.isNotEmpty()) {
                appendLine()
                appendLine("## Indices")
                clues.forEach { x ->
                    val m = x.indices.toMap()
                    val seuil = if (x.seuils.isEmpty()) "" else "seuil dépassé : ${x.seuils.joinToString(" ; ")} ; "
                    appendLine("- ${x.scenario.label} : $seuil" + "capture ${m["capture"].orEmpty().ifEmpty { "pas prise" }}, tags ${m["tags"].orEmpty().ifEmpty { "pas écrits" }}")
                }
            }
        }

        /** A level's scenarios: passed, not measured (counted apart: neither passed nor failed), all. */
        private data class Niveau(val niveau: String, val passes: Int, val nonMesures: Int, val scenarios: Int)

        private fun niveaux(r: List<Resultat>): List<Niveau> =
            r.groupBy { it.scenario.niveau }.map { (level, of) -> Niveau(level, of.count { it.resultat == PASSE }, of.count { it.resultat == NON_MESURE }, of.size) }

        /** The phone's sound the last scenario began with: « coupé » or « allumé »; null: unknown. */
        fun son(lines: List<Carnet.Ligne>): String? {
            val ids = LISTE.map { it.id }.toSet()
            return lines.lastOrNull { it.step in ids && it.kind == Carnet.NOTE && it.text.startsWith("$SON;") }?.text?.split(';')?.getOrNull(1)
        }

        /** From the first scenario's beginning to the last line of one. */
        private fun duree(lines: List<Carnet.Ligne>): Long? {
            val ids = LISTE.map { it.id }.toSet()
            val mine = lines.filter { it.step in ids }
            val begin = mine.firstOrNull { it.kind == Carnet.DEBUT } ?: return null
            return (mine.last().atMs - begin.atMs) / 1000
        }

        fun fmt(v: Float) = if (v.isFinite()) String.format(Locale.ROOT, "%.2f", v) else ""
        fun fmt(v: Double) = String.format(Locale.ROOT, "%.2f", v)
    }
}

/**
 * The open project as a scenario's oracle reads it: in memory (what the screens show) and read back
 * from its file ([disque], null without a file). [effets]: each strip's effects, by name.
 */
data class ProjetVu(val id: String, val nom: String, val parties: Int, val effets: List<List<String>>, val disque: Disque?) {
    data class Disque(val nom: String, val parties: Int, val effets: List<List<String>>)

    companion object {
        /** A strip's effects at most (the model's own limit). */
        const val EFFETS_MAX = com.pocketstudio.model.Layers.MAX_INSERTS
    }
}
