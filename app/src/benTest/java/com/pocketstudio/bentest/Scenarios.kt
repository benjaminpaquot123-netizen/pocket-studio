package com.pocketstudio.bentest

import com.pocketstudio.app.journey.Journey
import com.pocketstudio.app.report.JsonText
import com.pocketstudio.model.allScenes
import com.pocketstudio.model.cellHasContent
import com.pocketstudio.model.liveColumns
import com.pocketstudio.model.EffectType
import java.io.File
import java.util.Locale
import kotlin.math.abs

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
    /** How long the Live plays untouched (tour 9, C2: 30 s, it must not stop by itself). */
    private val liveEcouteMs: Long = 30_000,
    /** How long the song is listened to with its new effect. */
    private val effetEcouteMs: Long = 6_000,
    /** How long each style's new song is listened to, then the long playback, then the music with the studio behind another screen. */
    private val styleEcouteMs: Long = 10_000,
    private val longueMs: Long = 120_000,
    private val fondMs: Long = 8_000,
    /** Types [text] in the field tagged [tag] ([InAppDriver.saisir]); false: no such field (or not on the phone). */
    private val saisir: (String, String) -> Boolean = { _, _ -> false },
    /** Sets the slider tagged [tag] to a value ([InAppDriver.regler]); false: no such slider (or not on the phone). */
    private val regler: (String, Float) -> Boolean = { _, _ -> false },
    /** The pack played: [RAPIDE] (the first 31) or [COMPLET] (all of them). */
    private val paquet: String = COMPLET,
    /** The ids the last run did not pass (the [RATES] pack plays them again; Robot's rates.txt). */
    private val rates: Set<String> = emptySet(),
    /** The short listening of the full pack's scenarios (an effect, a tempo, a screen switched while playing). */
    private val courtMs: Long = 3_000,
    /** Scrolls the screen's lists back to their top ([InAppDriver.enHaut]): the Mix left scrolled down on the export settings. */
    private val enHaut: () -> Unit = {},
    /** The « seul » pack's long listenings: the endurance, the audio focus taken away, the big buffer. */
    private val enduranceMs: Long = 600_000,
    private val focusMs: Long = 3_000,
    private val tamponMs: Long = 15_000,
) {
    private val e = EssaiEclair(j, sonde, appli, carnet, captures, ecouteMs, ecran)
    private val t = ToutTester(j, sonde, appli, carnet, captures, ecouteMs, ecran)
    private val ts = ToutTester(j, sonde, appli, carnet, captures, styleEcouteMs, ecran)

    /** Each scenario's work (its gestures and its oracle), by its id. */
    private val travaux: Map<String, () -> String> = mapOf(
        DEBUTANT to { debutant() },
        ECRANS to { e.ecrans() },
        TEMPO to { tempo() },
        MUET to { muet() },
        ANNULER to { annulerRetablir() },
        PADS to { pads() },
        PROJET to { projet() },
        STRUCTURE to { structure() },
        DUPLIQUER to { dupliquer() },
        RENOMMER to { renommer() },
        SUPPRIMER to { supprimer() },
        SOLO to { solo() },
        REPRISE to { reprise() },
        ARRIERE_PLAN to { arrierePlan() },
        EFFETS to { effets() },
        EFFETS_PLUSIEURS to { effetsPlusieurs() },
        ANNULER_EFFET to { annulerEffet() },
        TEMPO_LECTURE to { tempoLecture() },
        ROTATION_LECTURE to { rotationLecture() },
        LIVE to { live() },
        EXPORT to { t.export("m4a") },
        EXPORT_WAV to { t.export("wav") },
        EXPORT_FLAC to { t.export("flac") },
        LATENCE to { t.latence() },
        DECROCHAGES to { decrochages() },
        LECTURE_LONGUE to { lectureLongue() },
        IMAGES to { t.images() },
        STYLE_TRANCE to { ts.style(STYLE_TRANCE, "trance") },
        STYLE_DUBSTEP to { ts.style(STYLE_DUBSTEP, "dubstep") },
        STYLE_DNB to { ts.style(STYLE_DNB, "dnb") },
        GUIDE to { t.guide("house") },
        ENDURANCE to { endurance() },
        FUITE to { fuite() },
        DEMARRAGE to { demarrage() },
        ARRET to { arret() },
        ARRET_RELU to { arretRelu() },
        FOCUS to { focus() },
        TAMPON to { tampon() },
        VIDEO_SON to { t.export("video", VIDEO_SON) },
    )

    /** The scenarios of [LISTE] without a work of their own (none: a test checks it). */
    internal fun sansTravail(): List<String> = TOUS.filter { travail(it) == null }.map { it.id }

    fun etapes(): List<Etape> = liste(paquet, rates).map { s ->
        etape(s.id, s.label, s.maxMs, rangeLeProjet(s)) { (travail(s) ?: error("scénario sans travail : ${s.id}"))() }
    }

    /** A scenario's work: its own ([travaux]), else the one its table line describes ([Scenario.geste]). */
    private fun travail(s: Scenario): (() -> String)? {
        travaux[s.id]?.let { return it }
        val g = s.geste?.split(':') ?: return null
        val a = g.getOrNull(1).orEmpty()
        val id = s.id
        return when (g[0]) {
            "style" -> { { ts.style(id, a) } }
            "muet" -> a.toIntOrNull()?.let { ch -> { piste(id, ch) { bascule(id, "Muet", "sheet_mute", ch, { it.muets }, { it.muets }) } } }
            "solo" -> a.toIntOrNull()?.let { ch -> { piste(id, ch) { bascule(id, "Solo", "sheet_solo", ch, { it.solos }, { it.solos }) } } }
            "volume" -> a.toIntOrNull()?.let { ch -> { curseur(id, "Volume", "sheet_volume", ch, { it.volumes }, { it.volumes }) { v -> if (v > 0.6f) 0.4f else 0.9f } } }
            "pan" -> a.toIntOrNull()?.let { ch -> { curseur(id, "Panoramique", "sheet_pan", ch, { it.pans }, { it.pans }) { v -> if (v > 0f) -0.5f else 0.5f } } }
            "effet" -> { { effet(id, a) } }
            "tempo" -> { { tempoExtreme(id, haut = a != "bas", enJouant = a == "haut_lecture") } }
            "export" -> g.getOrNull(2)?.let { format -> { ts.style(id, a) + " ; " + t.export(format, id) } }
            "montee" -> { { montee(id, a) } }
            "partie" -> g.getOrNull(2)?.toIntOrNull()?.let { delta -> { partie(id, a, delta) } }
            "pad" -> a.toIntOrNull()?.let { i -> { toucherPad(id, i) } }
            "ecrans" -> ECRANS_LECTURE[a]?.let { tabs -> { lectureEcrans(id, tabs) } }
            "repetition" -> when (a) { "jouer" -> { { jouerArreter(id) } }; "fond" -> { { fondReprise(id) } }; else -> null }
            "guide" -> { { t.guide(a) } }
            "style_tempo" -> { { ts.style(id, a) + " ; " + tempoExtreme(id, haut = true, enJouant = true) } }
            "effet_retrait" -> { { effetRetrait(id, a) } }
            "effet_reglage" -> g.getOrNull(2)?.takeIf { it == "min" || it == "max" }?.let { end -> { effetReglage(id, a, end == "max") } }
            "onglet" -> g.getOrNull(2)?.takeIf { it in QUOI_ONGLET }?.let { how -> { onglet(id, a, how) } }
            "partie_redo" -> g.getOrNull(2)?.toIntOrNull()?.let { delta -> { partieRedo(id, a, delta) } }
            "limite" -> when (a) { "min" -> { { tempoLimite(id, false) } }; "max" -> { { tempoLimite(id, true) } }; else -> null }
            else -> null
        }
    }

    /**
     * A step that starts from the studio whatever the one before left ([remiseAZero]), and whose failure
     * leaves what the robot saw in the logbook.
     */
    private fun etape(name: String, label: String, maxMs: Long, range: Boolean = false, work: () -> String) = Etape(name, label, maxMs) { deadline ->
        val text = try {
            carnet.add(Carnet.NOTE, name, "$PAQUET;$paquet")
            son(name)
            remiseAZero(name)
            val avant = if (range) appli.projet() else null
            work().also { if (avant != null) commeAvant(avant) }
        } catch (t: Throwable) {
            e.noterEcran(name)
            indices(name)
            throw t
        }
        // A failure by a measure (a red threshold) or by the time: the same clues as when the robot gets lost.
        if (rouge(name) || System.currentTimeMillis() > deadline) indices(name)
        text
    }

    /**
     * The app died in [step] (noted as a crash by [Passage], the run picked up): its clues. Those the robot
     * took as the app died ([Robot]'s crash guard writes them under the same names) are kept, else the screen
     * as it is now.
     */
    fun apresPlantage(step: String) {
        val picture = File(captures, "echec-$step.jpg")
        val list = File(captures, "echec-$step-tags.txt")
        if (list.isFile) carnet.add(Carnet.NOTE, step, "$INDICE;capture=" + (if (picture.isFile) "captures/${picture.name}" else "") + ";tags=captures/${list.name}")
        else indices(step)
    }

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
        // « Insérer après la partie N » left open covers the whole studio (2026-11-22: 15 scenarios failed under it).
        if (j.tagsMatching(PICK).isNotEmpty()) runCatching { j.tap("texte:Annuler"); attendre(3_000) { j.tagsMatching(PICK).isEmpty() } }
        for (sheet in listOf("part_sheet", "strip_sheet")) {
            if (j.exists(sheet)) runCatching { j.tap("${sheet}_close"); j.awaitGone(sheet, 5_000) }
        }
        if (j.exists("tempo_sheet")) runCatching { j.tap("tempo_close"); j.awaitGone("tempo_sheet", 5_000) }
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

    /**
     * Audit E: a scenario leaves the song as it found it, in memory and on the disk, else the next one starts
     * from a dirty project and a fault hides behind another. Not checked when the scenario opened another song.
     */
    private fun commeAvant(avant: ProjetVu) {
        val now = appli.projet() ?: return
        if (now.id != avant.id) return
        val ecarts = ArrayList<String>()
        check(attendre(5_000) { ecarts(avant.trace(), appli.projet()?.trace()).isEmpty() }) {
            "le scénario ne remet pas le morceau comme il l'a trouvé : " + ecarts(avant.trace(), appli.projet()?.trace()).joinToString(", ")
        }
        // The disk ends as the song was in memory, or as the file was (a save still to come is not the scenario's).
        val fichier = avant.disque?.trace() ?: return
        val d = surDisque { ecarts(avant.trace(), it.trace()).isEmpty() || ecarts(fichier, it.trace()).isEmpty() }
        if (d != null && ecarts(fichier, d.trace()).isEmpty()) return
        ecarts += ecarts(avant.trace(), d?.trace())
        check(ecarts.isEmpty()) { "le disque ne revient pas comme avant le scénario : " + ecarts.joinToString(", ") }
    }

    /** The open song with the Pro tabs, no sheet over it. */
    private fun auStudio() {
        fermerFeuilles()
        e.auStudioPro()
    }

    private fun lu(): ProjetVu = appli.projet() ?: error("le projet ouvert ne se lit pas")

    /** « Annuler » in the bar; none is red (audit E): [what] would stay in the project the next scenario finds. */
    private fun annuler(what: String) {
        check(j.exists("top_undo")) { "pas de bouton « Annuler » dans la barre : $what reste dans le projet" }
        j.tap("top_undo")
    }

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
        annuler("le break")
        check(attendre(5_000) { lu().parties == before }) { "« Annuler » ne retire pas le break : ${lu().parties} parties (attendu : $before)" }
        val back = surDisque { it.parties == before }
        check(back?.parties == before) { "le break annulé reste sur le disque : ${back?.parties ?: "aucun fichier"} parties (attendu : $before)" }
        bilan(STRUCTURE, "parties_avant" to before, "parties_apres" to before + 1)
        return "break ajouté après la partie ${PARTIE + 1} : $before parties, puis ${before + 1}, enregistré, puis $before après « Annuler »"
    }

    /**
     * Pro: Mix › a strip's « ⋯ » › « Ajouter un effet » › Égaliseur. Oracle: one equaliser more on that
     * strip in the project, the same on the disk, and the song still sounds (no silence, no NaN).
     */
    fun effets(): String {
        auStudio()
        val before = lu().effets
        // The first strip with room for one more effect (the assistant's songs come with some).
        val ch = (0 until minOf(before.size, PISTES)).firstOrNull { before[it].size < ProjetVu.EFFETS_MAX }
            ?: return nonMesure(EFFETS, "aucune des $PISTES premières pistes n'a de place libre")
        fun n(p: List<List<String>>) = p.getOrNull(ch).orEmpty().count { it == EFFET }
        j.tap("tab_Mix")
        j.await("mixer")
        val chips = Regex("fx_chip_${ch}_\\d+")
        val added: Boolean
        val chip: Boolean
        try {
            ouvrirBandeau(ch)
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
        val undone = retirer(ch, before[ch].sorted(), 1)
        check(undone) { "l'effet reste sur la piste ${ch + 1} : « Annuler » ne l'a pas retiré (${lu().effets[ch].joinToString(", ")})" }
        check(!b.nonFinite) { "valeur invalide (NaN) dans le son avec l'effet" }
        check(b.secondes.any { it.peakDb > Ecoute.SILENCE_DB }) { "silence avec l'effet : ${b.text()}" }
        return "égaliseur ajouté à la piste ${ch + 1}, enregistré ; lecture par $by : ${b.text()} ; retiré par « Annuler »"
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
        // Still playing when the robot comes back: only then is the stop the robot's own.
        val still = sonde.lire().playing
        if (still) runCatching { j.tap("live_play") }
        val stopped = attendre(liveMs) { !sonde.lire().playing }
        // Out of the Live whatever happened: the next scenario starts from the song.
        runCatching { j.tap("tab_Structure") }
        check(started) { "le Live ne démarre pas : ▶ touché, le moteur ne joue pas" }
        check(heard) { "le Live joue mais aucun son ne sort : crête ${fmt(peak)}" }
        checkNotNull(b)
        ecoute(LIVE, b)
        feuEcoute(LIVE, b)
        check(still && b.playing == b.reads) {
            "le Live s'arrête tout seul après ${b.playing / Ecoute.READS_PER_SECOND} s sur ${liveEcouteMs / 1000} : le moteur a cessé de jouer avant que le robot touche ▶ (${b.text()})"
        }
        check(stopped) { "le Live ne s'arrête pas : ▶ touché une seconde fois, le moteur joue encore" }
        check(!b.nonFinite) { "valeur invalide (NaN) dans le son du Live" }
        return "le Live joue ${liveEcouteMs / 1000} s sans s'arrêter (${b.text()}), puis le robot l'arrête"
    }

    /** The song played [decrochagesMs]: no dropout, the worst block and the longest gap inside the thresholds. */
    fun decrochages(): String = lecture(DECROCHAGES, decrochagesMs)

    /** The song played [longueMs] (2 min) without a stop: the same oracle as [decrochages], over a long time. */
    fun lectureLongue(): String = lecture(LECTURE_LONGUE, longueMs)

    private fun lecture(step: String, ms: Long): String {
        auStudio()
        val by = e.lancerLecture()
        val b = t.ecouter(step, ms)
        e.arreter()
        ecoute(step, b)
        feuEcoute(step, b)
        check(b.reads > 0) { "aucune mesure" }
        check(b.joueTout) { "la lecture s'est arrêtée toute seule : ${b.text()}" }
        check(!b.nonFinite) { "valeur invalide (NaN) dans le son" }
        return "lecture par $by : ${b.text()}"
    }

    // --- The scenarios added on 2026-11-22 -------------------------------------------------------------

    /** Neither passed nor failed: the phone cannot play this scenario ([why]); a grey light and the note the report reads. */
    private fun nonMesure(step: String, why: String): String {
        feu(step, Feu.GRIS to why)
        carnet.add(Carnet.NOTE, step, "${ToutTester.NON_MESURE};$why")
        return "non mesuré : $why"
    }

    private fun ouvrirTempo() {
        if (!j.exists("top_tempo")) j.tap("tab_Structure")
        j.tap("top_tempo")
        j.await("tempo_sheet")
    }

    /** Beginner: the tempo's « + », then « − ». Oracle: the project's tempo up, the same on the disk, then back down. */
    fun tempo(): String {
        auStudio()
        val before = lu().tempo
        var after = before
        val up: Boolean
        var d: ProjetVu.Disque? = null
        val down: Boolean
        try {
            ouvrirTempo()
            j.tap("tempo_plus")
            up = attendre(5_000) { lu().tempo > before }
            after = lu().tempo
            if (up) d = surDisque { it.tempo == after }
            j.tap("tempo_minus")
            down = attendre(5_000) { abs(lu().tempo - before) < 0.01 }
        } finally {
            fermerFeuilles()
        }
        check(up) { "le tempo ne change pas : ${fmt(before)} bpm après « + »" }
        check(d?.tempo == after) { "le tempo n'est pas enregistré : ${d?.tempo?.let(::fmt) ?: "aucun fichier"} bpm sur le disque (attendu : ${fmt(after)})" }
        check(down) { "« − » ne remet pas le tempo à ${fmt(before)} : ${fmt(lu().tempo)} bpm" }
        bilan(TEMPO, "tempo_avant" to fmt(before), "tempo_apres" to fmt(after))
        return "tempo ${fmt(before)} puis ${fmt(after)} bpm, enregistré, puis redescendu à ${fmt(lu().tempo)}"
    }

    /** Pro: the tempo raised by 5 while the song plays. Oracle: the project's tempo changed, the music never stops, no dropout. */
    fun tempoLecture(): String {
        auStudio()
        val before = lu().tempo
        val by = e.lancerLecture()
        val b: Ecoute.Bilan
        val changed: Boolean
        try {
            ouvrirTempo()
            repeat(PAS_TEMPO) { j.tap("tempo_plus") }
            changed = attendre(5_000) { lu().tempo >= before + PAS_TEMPO - 0.01 }
            b = t.ecouter(TEMPO_LECTURE, effetEcouteMs)
            repeat(PAS_TEMPO) { runCatching { j.tap("tempo_minus") } }
        } finally {
            fermerFeuilles()
            e.arreter()
        }
        ecoute(TEMPO_LECTURE, b)
        feuEcoute(TEMPO_LECTURE, b)
        check(changed) { "le tempo ne monte pas pendant la lecture : ${fmt(lu().tempo)} bpm (avant : ${fmt(before)})" }
        check(b.joueTout) { "la lecture s'arrête quand le tempo change : ${b.text()}" }
        check(!b.nonFinite) { "valeur invalide (NaN) dans le son" }
        return "tempo ${fmt(before)} puis ${fmt(before + PAS_TEMPO)} bpm en jouant (lecture par $by) : ${b.text()}"
    }

    /** A strip's sheet in the mixer. */
    private fun bandeau(ch: Int) {
        j.tap("tab_Mix")
        j.await("mixer")
        ouvrirBandeau(ch)
    }

    /** The strip's « ⋯ », its list back at the top first, three tries (2026-11-22: 14 sheets never opened, the Mix scrolled down). */
    private fun ouvrirBandeau(ch: Int) {
        repeat(3) { essai ->
            runCatching { enHaut() }
            j.idle(300)
            runCatching { j.tap("strip_more_$ch") }
            if (attendre(3_000) { j.exists("strip_sheet") }) return
            carnet.add(Carnet.NOTE, "", "bandeau $ch : la feuille ne s'ouvre pas, essai ${essai + 2}")
        }
        j.tap("strip_more_$ch")
        j.await("strip_sheet")
    }

    /** A strip's « Muet » or « Solo » ([tag]) on, then off. Oracle: the project's flag, the same on the disk, then back. */
    private fun bascule(step: String, what: String, tag: String, ch: Int, of: (ProjetVu) -> List<Boolean>, ofDisk: (ProjetVu.Disque) -> List<Boolean>): String {
        auStudio()
        val before = of(lu()).getOrNull(ch) ?: error("pas de piste ${ch + 1} dans le projet")
        val on: Boolean
        var d: ProjetVu.Disque? = null
        val off: Boolean
        try {
            bandeau(ch)
            j.tap("${tag}_$ch")
            on = attendre(5_000) { of(lu()).getOrNull(ch) == !before }
            if (on) d = surDisque { ofDisk(it).getOrNull(ch) == !before }
            j.tap("${tag}_$ch")
            off = attendre(5_000) { of(lu()).getOrNull(ch) == before }
        } finally {
            fermerFeuilles()
        }
        check(on) { "« $what » touché, la piste ${ch + 1} ne change pas dans le projet" }
        check(d != null && ofDisk(d).getOrNull(ch) == !before) { "« $what » n'est pas enregistré sur le disque (piste ${ch + 1})" }
        check(off) { "« $what » touché une seconde fois, la piste ${ch + 1} ne revient pas" }
        bilan(step, "piste" to ch + 1)
        return "« $what » sur la piste ${ch + 1} : mis, enregistré, puis enlevé"
    }

    /** Beginner: the first strip muted, then not. */
    fun muet(): String = bascule(MUET, "Muet", "sheet_mute", 0, { it.muets }, { it.muets })

    /** Intermediate: the second strip soloed, then not. */
    fun solo(): String = bascule(SOLO, "Solo", "sheet_solo", 1, { it.solos }, { it.solos })

    /** Beginner: a break added, « Annuler », « Rétablir ». Oracle: the parts one more, back, one more again, on the disk. */
    fun annulerRetablir(): String {
        auStudio()
        val before = lu().parties
        j.tap("tab_Structure")
        try {
            j.partIntent(PARTIE, "part_break_4")
        } finally {
            fermerFeuilles()
        }
        check(attendre(5_000) { lu().parties == before + 1 }) { "le break ne s'ajoute pas au morceau : ${lu().parties} parties (avant : $before)" }
        annuler("le break")
        check(attendre(5_000) { lu().parties == before }) { "« Annuler » ne retire pas le break : ${lu().parties} parties (attendu : $before)" }
        // A narrow phone has no room for « Rétablir » in the bar: it is in the thumb bar.
        val redo = REDO.firstOrNull { attendre(5_000) { j.exists(it) } }
        check(redo != null) { "pas de « Rétablir » après « Annuler »" }
        j.tap(redo)
        check(attendre(5_000) { lu().parties == before + 1 }) { "« Rétablir » ne remet pas le break : ${lu().parties} parties (attendu : ${before + 1})" }
        val d = surDisque { it.parties == before + 1 }
        check(d?.parties == before + 1) { "après « Rétablir », ${d?.parties ?: "aucun fichier"} parties sur le disque (attendu : ${before + 1})" }
        // Tidying: « Annuler » once more, the song as the scenario found it, on the disk too.
        annuler("le break rétabli")
        check(attendre(5_000) { lu().parties == before }) { "le second « Annuler » ne retire pas le break : ${lu().parties} parties (attendu : $before)" }
        val back = surDisque { it.parties == before }
        check(back?.parties == before) { "le break annulé reste sur le disque : ${back?.parties ?: "aucun fichier"} parties (attendu : $before)" }
        bilan(ANNULER, "parties" to before + 1)
        return "break ajouté ($before puis ${before + 1} parties), annulé ($before), rétabli (${before + 1}), enregistré, puis annulé ($before)"
    }

    /** [types] added one by one to strip [ch] by its sheet's list: each one in the project before the next. */
    private fun ajouter(ch: Int, types: List<String>) {
        bandeau(ch)
        try {
            for (type in types) {
                val n = lu().effets.getOrNull(ch).orEmpty().size
                if (!j.exists("fx_picker_$ch")) {
                    j.tap("fx_add_$ch")
                    j.await("fx_picker_$ch")
                }
                j.tap("fx_pick_${ch}_$type")
                check(attendre(5_000) { lu().effets.getOrNull(ch).orEmpty().size == n + 1 }) {
                    "$type ne s'ajoute pas à la piste ${ch + 1} : ${lu().effets.getOrNull(ch).orEmpty().ifEmpty { listOf("aucun") }.joinToString(", ")}"
                }
            }
            runCatching { if (j.exists("fx_picker_$ch")) j.tap("fx_add_$ch") }
        } finally {
            fermerFeuilles()
        }
    }

    /**
     * Tidying after an effect scenario: « Annuler » touched up to [fois] times until strip [ch] has its effects
     * [was] again, in the project and on the disk. False when it does not: the strip is left dirty, and the next
     * scenarios would find no room (C10), so the scenario says it rather than pass in silence. No « Annuler » in the bar
     * is red too (audit E: a gesture the robot cannot take back leaves the project dirty).
     */
    private fun retirer(ch: Int, was: List<String>, fois: Int): Boolean {
        fun clean(fx: List<List<String>>) = fx.getOrNull(ch).orEmpty().sorted() == was
        repeat(fois) {
            if (clean(lu().effets)) return@repeat
            val n = lu().effets.getOrNull(ch).orEmpty().size
            annuler("l'effet de la piste ${ch + 1}")
            attendre(5_000) { lu().effets.getOrNull(ch).orEmpty().size < n }
        }
        return attendre(5_000) { clean(lu().effets) } && surDisque { clean(it.effets) }?.let { clean(it.effets) } == true
    }

    /** The first strip with room for [n] effects more (null: none among the first ones). */
    private fun place(n: Int): Int? {
        val fx = lu().effets
        return (0 until minOf(fx.size, PISTES_MAX)).firstOrNull { fx[it].size + n <= ProjetVu.EFFETS_MAX }
    }

    /** Pro: three effects on one strip. Oracle: the three in the project and on the disk, and the song still sounds (no NaN). */
    fun effetsPlusieurs(): String {
        auStudio()
        val ch = place(TROIS.size) ?: return nonMesure(EFFETS_PLUSIEURS, "aucune des $PISTES_MAX premières pistes n'a ${TROIS.size} places libres")
        val was = lu().effets[ch].sorted()
        val want = (was + TROIS).sorted()
        ajouter(ch, TROIS)
        check(lu().effets[ch].sorted() == want) { "la piste ${ch + 1} a ${lu().effets[ch].joinToString(", ")} (attendu : ${want.joinToString(", ")})" }
        val d = surDisque { it.effets.getOrNull(ch).orEmpty().sorted() == want }
        check(d != null && d.effets.getOrNull(ch).orEmpty().sorted() == want) { "les trois effets ne sont pas enregistrés sur le disque (piste ${ch + 1})" }
        val by = e.lancerLecture()
        val b = t.ecouter(EFFETS_PLUSIEURS, effetEcouteMs)
        e.arreter()
        ecoute(EFFETS_PLUSIEURS, b)
        feuEcoute(EFFETS_PLUSIEURS, b)
        val undone = retirer(ch, was, TROIS.size)
        // A strip left dirty first: the next scenarios find it so, whatever was heard.
        check(undone) { "les effets restent sur la piste ${ch + 1} : « Annuler » ne les a pas retirés (${lu().effets[ch].joinToString(", ")})" }
        check(!b.nonFinite) { "valeur invalide (NaN) dans le son avec les trois effets" }
        check(b.secondes.any { it.peakDb > Ecoute.SILENCE_DB }) { "silence avec les trois effets : ${b.text()}" }
        return "${TROIS.joinToString(", ")} sur la piste ${ch + 1}, enregistrés ; lecture par $by : ${b.text()} ; retirés par « Annuler »"
    }

    /** Pro: an effect added, then « Annuler ». Oracle: the strip's effects back as they were, in the project and on the disk. */
    fun annulerEffet(): String {
        auStudio()
        val ch = place(1) ?: return nonMesure(ANNULER_EFFET, "aucune des $PISTES_MAX premières pistes n'a de place libre")
        val before = lu().effets[ch].sorted()
        ajouter(ch, listOf(EFFET_ANNULE))
        annuler("l'effet $EFFET_ANNULE")
        check(attendre(5_000) { lu().effets[ch].sorted() == before }) { "« Annuler » ne retire pas l'effet : ${lu().effets[ch].joinToString(", ")} sur la piste ${ch + 1}" }
        val d = surDisque { it.effets.getOrNull(ch).orEmpty().sorted() == before }
        check(d != null && d.effets.getOrNull(ch).orEmpty().sorted() == before) { "l'effet annulé reste sur le disque (piste ${ch + 1})" }
        return "$EFFET_ANNULE ajouté à la piste ${ch + 1} puis annulé, le disque aussi"
    }

    /** Beginner: a pad of the Live touched. Oracle: the engine plays and sound comes out. */
    fun pads(): String = toucherPad(PADS, null)

    /**
     * The Live's pad number [rang] (0: the first; null: the first one shown) among the cells that have something
     * to play, by the grid's own tags (`live_pad_<track>_<row>`), touched.
     */
    private fun toucherPad(step: String, rang: Int?): String {
        auStudio()
        e.arreter()
        val full = lu().pads
        j.tap("tab_Live")
        j.await("live_screen")
        attendre(2_000) { full.any { j.exists(it) } }
        val shown = full.filter { j.exists(it) }
        val pad = if (rang == null) shown.firstOrNull() else full.getOrNull(rang)?.takeIf { it in shown }
        if (pad == null) {
            runCatching { j.tap("tab_Structure") }
            val why = when {
                full.isEmpty() -> "aucune case du Live n'a de son dans ce morceau"
                rang == null || rang < full.size -> "les ${full.size} cases pleines du Live ne sont pas à l'écran"
                else -> "pas de pad ${rang + 1} : ce morceau a ${full.size} cases pleines dans le Live"
            }
            return nonMesure(step, why)
        }
        j.tap(pad)
        val started = attendre(liveMs) { sonde.lire().playing }
        var peak = 0f
        val heard = started && attendre(liveMs) { peak = maxOf(peak, sonde.lire().peak); peak > EssaiEclair.NIVEAU_MIN }
        runCatching { if (j.exists("live_stop_all")) j.tap("live_stop_all") }
        runCatching { j.tap("tab_Structure") }
        e.arreter()
        check(started) { "pad $pad touché, le moteur ne joue pas" }
        check(heard) { "pad $pad touché, le moteur joue mais aucun son ne sort : crête ${fmt(peak)}" }
        bilan(step, "crete" to fmt(peak))
        return "pad $pad : le son sort (crête ${fmt(peak)})"
    }

    private fun versProjets() {
        if (!j.exists("top_projects")) j.tap("tab_Structure")
        j.tap("top_projects")
        j.await("projects")
    }

    private fun versStudio() {
        runCatching { j.awaitGone("project_name_field", 2_000) }
        if (j.exists("projects")) j.tap("projects_back")
        if (runCatching { j.await("launch", 5_000) }.isSuccess) j.tap("launch_continue")
        j.awaitAny(*EssaiEclair.STUDIO, timeoutMs = 30_000)
    }

    /** A project's « ⋯ », then its item [names] (an item without a tag: found by its words, French or English). */
    private fun menu(id: String, names: List<String>) {
        j.tap("project_menu_$id")
        var item: String? = null
        attendre(3_000) { item = names.map { InAppDriver.TEXTE + it }.firstOrNull { j.exists(it) }; item != null }
        j.tap(item ?: error("« ${names.first()} » n'est pas dans le menu du projet"))
    }

    /** The copy this run made (the only project besides the open one that the robot may rename or delete). */
    private var copie: String? = null

    /** Intermediate: « Mes projets » › the open song's « ⋯ » › « Dupliquer ». Oracle: one project more on the disk, with the same parts. */
    fun dupliquer(): String {
        auStudio()
        val p = lu()
        val before = appli.projets() ?: return nonMesure(DUPLIQUER, "la liste des projets ne se lit pas ici")
        versProjets()
        try {
            menu(p.id, listOf("Dupliquer", "Duplicate"))
            var added = emptyMap<String, ProjetVu.Disque>()
            attendre(sauvegardeMs) { added = appli.projets().orEmpty() - before.keys; added.isNotEmpty() }
            check(added.size == 1) { if (added.isEmpty()) "aucune copie sur le disque après ${sauvegardeMs / 1000} s" else "${added.size} projets nouveaux au lieu d'un" }
            val (id, d) = added.entries.first()
            copie = id
            check(d.parties == p.parties) { "la copie « ${d.nom} » a ${d.parties} parties (l'original : ${p.parties})" }
            bilan(DUPLIQUER, "parties" to d.parties)
            return "copie « ${d.nom} » de « ${p.nom} » sur le disque (${d.parties} parties)"
        } finally {
            versStudio()
        }
    }

    /** Intermediate: the robot's copy renamed. Oracle: its file on the disk has the new name. Never one of the user's projects. */
    fun renommer(): String {
        auStudio()
        val id = copie?.takeIf { it != lu().id } ?: return nonMesure(RENOMMER, "pas de copie faite par le robot à renommer (« Dupliquer » n'a pas abouti)")
        versProjets()
        try {
            menu(id, listOf("Renommer", "Rename"))
            j.await("project_name_field", 5_000)
            check(saisir("project_name_field", NOM_ROBOT)) { "le champ du nom ne prend pas le texte" }
            j.tap("project_rename_ok")
            var d: ProjetVu.Disque? = null
            check(attendre(sauvegardeMs) { d = appli.projets()?.get(id); d?.nom == NOM_ROBOT }) { "le nouveau nom n'est pas enregistré : « ${d?.nom ?: "?"} » sur le disque (attendu : « $NOM_ROBOT »)" }
            return "copie renommée « $NOM_ROBOT », relue sur le disque"
        } finally {
            versStudio()
        }
    }

    /**
     * Intermediate: the robot's copy deleted. Oracle: it leaves the disk's list. Only a project the robot made (this run's
     * copy, else one under its name): never one of the user's.
     */
    fun supprimer(): String {
        auStudio()
        val open = lu().id
        val all = appli.projets() ?: return nonMesure(SUPPRIMER, "la liste des projets ne se lit pas ici")
        val id = (copie?.takeIf { it in all } ?: all.entries.firstOrNull { it.value.nom == NOM_ROBOT }?.key)?.takeIf { it != open }
            ?: return nonMesure(SUPPRIMER, "aucun projet fait par le robot à supprimer (« Dupliquer » n'a pas abouti)")
        versProjets()
        try {
            j.tap("project_menu_$id")
            j.tap("project_delete_$id")
            check(attendre(sauvegardeMs) { appli.projets()?.containsKey(id) == false }) { "le projet supprimé est encore sur le disque après ${sauvegardeMs / 1000} s" }
            copie = null
            val undo = j.exists("project_deleted")
            return "copie du robot supprimée : plus dans Mes projets" + if (undo) ", « Annuler » proposé" else ""
        } finally {
            versStudio()
        }
    }

    /** Intermediate: the studio closed, then opened again. Oracle: « Continuer » brings back the same song, with its parts. */
    fun reprise(): String {
        auStudio()
        e.arreter()
        val p = lu()
        surDisque { it.parties == p.parties }
        if (!appli.relancerStudio()) return nonMesure(REPRISE, "le studio ne se ferme et ne se rouvre pas d'ici")
        // The studio is opened again by [Appli.relancerStudio]: its opening, then « Que veux-tu faire ? » or the song.
        j.awaitAny("intro", "welcome", "launch", *EssaiEclair.STUDIO, timeoutMs = 60_000)
        j.awaitGone("intro", 60_000)
        val how = if (j.exists("launch")) "« Continuer »" else "rouvert sur le morceau"
        e.auStudioPro()
        val back = lu()
        check(back.id == p.id && back.parties == p.parties) { "rouvert sur « ${back.nom} » (${back.parties} parties) au lieu de « ${p.nom} » (${p.parties} parties)" }
        return "studio fermé puis rouvert ($how) : « ${p.nom} », ${p.parties} parties"
    }

    /** Intermediate: the song plays, the studio goes behind another screen. Oracle: the music goes on in the background, then the studio comes back. */
    fun arrierePlan(): String {
        auStudio()
        val by = e.lancerLecture()
        var b: Ecoute.Bilan? = null
        val done = try {
            appli.arrierePlan { b = t.ecouter(ARRIERE_PLAN, fondMs) }
        } finally {
            if (appli.studioDevant() == false) attendre(15_000) { appli.studioDevant() != false }
        }
        val back = appli.studioDevant() != false
        e.arreter()
        if (!done) return nonMesure(ARRIERE_PLAN, "le studio ne passe pas derrière un autre écran ici")
        val heard = checkNotNull(b)
        ecoute(ARRIERE_PLAN, heard)
        feuEcoute(ARRIERE_PLAN, heard)
        check(heard.joueTout) { "la musique s'arrête quand l'appli passe au fond : ${heard.text()}" }
        check(heard.secondes.any { it.peakDb > Ecoute.SILENCE_DB }) { "silence quand l'appli passe au fond : ${heard.text()}" }
        check(back) { "le studio ne revient pas devant" }
        return "au fond ${fondMs / 1000} s (lecture par $by) : ${heard.text()} ; studio revenu"
    }

    /** Pro: the phone turned while the song plays. Oracle: the studio comes back each time and the music never stops. */
    fun rotationLecture(): String {
        auStudio()
        val by = e.lancerLecture()
        val lying: Boolean
        val standing: Boolean
        val b: Ecoute.Bilan
        try {
            appli.orienter(true)
            j.idle(1_500)
            j.awaitAny(*EssaiEclair.STUDIO, timeoutMs = 30_000)
            lying = sonde.lire().playing
            appli.orienter(false)
            j.idle(1_500)
            j.awaitAny(*EssaiEclair.STUDIO, timeoutMs = 30_000)
            standing = sonde.lire().playing
            b = t.ecouter(ROTATION_LECTURE, liveEcouteMs)
        } finally {
            appli.orienter(null)
            e.arreter()
        }
        ecoute(ROTATION_LECTURE, b)
        feuEcoute(ROTATION_LECTURE, b)
        check(lying) { "la musique s'arrête quand le téléphone se couche" }
        check(standing && b.joueTout) { "la musique s'arrête quand le téléphone se redresse : ${b.text()}" }
        return "couché puis debout en jouant (lecture par $by) : ${b.text()}"
    }

    // --- The full pack's tables (« Scénarios complets », 2026-11-22) ---------------------------------------

    /** A strip's scenario: « non mesuré » when the song has no strip [ch]. */
    private fun piste(step: String, ch: Int, work: () -> String): String {
        auStudio()
        if (lu().muets.getOrNull(ch) == null) return nonMesure(step, "le morceau n'a pas de piste ${ch + 1}")
        return work()
    }

    /**
     * A strip's slider ([tag]: volume or pan) moved to [cible], then put back. Oracle: the project's value moved
     * that way, the disk has the same value.
     */
    private fun curseur(step: String, what: String, tag: String, ch: Int, of: (ProjetVu) -> List<Float>, ofDisk: (ProjetVu.Disque) -> List<Float>, cible: (Float) -> Float): String {
        auStudio()
        val before = of(lu()).getOrNull(ch) ?: return nonMesure(step, "le morceau n'a pas de piste ${ch + 1}")
        val target = cible(before)
        val up = target > before
        var after = before
        var d: ProjetVu.Disque? = null
        val moved: Boolean
        try {
            bandeau(ch)
            if (!regler("${tag}_$ch", target)) return nonMesure(step, "le curseur « $what » de la piste ${ch + 1} ne se règle pas d'ici")
            moved = attendre(5_000) { of(lu()).getOrNull(ch)?.let { if (up) it > before + PAS_CURSEUR else it < before - PAS_CURSEUR } == true }
            after = of(lu()).getOrNull(ch) ?: before
            if (moved) d = surDisque { ofDisk(it).getOrNull(ch)?.let { v -> abs(v - after) < 0.001f } == true }
            regler("${tag}_$ch", before)
        } finally {
            fermerFeuilles()
        }
        check(moved) { "« $what » réglé à ${fmt(target)}, la piste ${ch + 1} reste à ${fmt(of(lu()).getOrNull(ch) ?: before)} dans le projet" }
        check(d?.let { ofDisk(it).getOrNull(ch) }?.let { abs(it - after) < 0.001f } == true) { "« $what » à ${fmt(after)} n'est pas enregistré sur le disque (piste ${ch + 1})" }
        bilan(step, "piste" to ch + 1, "avant" to fmt(before), "apres" to fmt(after))
        return "« $what » de la piste ${ch + 1} : ${fmt(before)} puis ${fmt(after)}, enregistré, puis remis"
    }

    /** Pro: one effect of each type on a strip. Oracle: the effect in the project and on the disk; the song still sounds (no NaN). */
    private fun effet(step: String, type: String): String {
        auStudio()
        val et = EffectType.entries.firstOrNull { it.name == type } ?: return nonMesure(step, "l'appli n'a pas d'effet $type")
        if (et.long) {
            val n = lu().effets.flatten().count { name -> EffectType.entries.firstOrNull { it.name == name }?.let { EffectType.samePool(it, et) } == true }
            if (n >= EffectType.longLimit(et)) return nonMesure(step, "déjà $n effets de ce genre dans le morceau (l'appli en permet ${EffectType.longLimit(et)})")
        }
        val ch = place(1) ?: return nonMesure(step, "aucune des $PISTES_MAX premières pistes n'a de place libre")
        val before = lu().effets[ch].sorted()
        val want = (before + type).sorted()
        ajouter(ch, listOf(type))
        check(lu().effets[ch].sorted() == want) { "la piste ${ch + 1} a ${lu().effets[ch].joinToString(", ")} (attendu : ${want.joinToString(", ")})" }
        val d = surDisque { it.effets.getOrNull(ch).orEmpty().sorted() == want }
        check(d != null && d.effets.getOrNull(ch).orEmpty().sorted() == want) { "$type n'est pas enregistré sur le disque (piste ${ch + 1})" }
        val by = e.lancerLecture()
        val b = try { t.ecouter(step, courtMs) } finally { e.arreter() }
        ecoute(step, b)
        feuEcoute(step, b)
        val undone = retirer(ch, before, 1)
        check(undone) { "l'effet reste sur la piste ${ch + 1} : « Annuler » ne l'a pas retiré (${lu().effets[ch].joinToString(", ")})" }
        check(!b.nonFinite) { "valeur invalide (NaN) dans le son avec $type" }
        check(b.secondes.any { it.peakDb > Ecoute.SILENCE_DB }) { "silence avec $type : ${b.text()}" }
        return "$type sur la piste ${ch + 1}, enregistré ; lecture par $by : ${b.text()} ; retiré par « Annuler »"
    }

    /**
     * Pro: the tempo halved ([haut] false) or doubled [FOIS_TEMPO] times, the song playing or not. Oracle: the tempo moved
     * that way, on the disk, and the song plays with sound, no NaN. Then put back (tidying).
     */
    private fun tempoExtreme(step: String, haut: Boolean, enJouant: Boolean): String {
        auStudio()
        val tag = if (haut) "tempo_double" else "tempo_half"
        val back = if (haut) "tempo_half" else "tempo_double"
        val before = lu().tempo
        var by: String? = if (enJouant) e.lancerLecture() else null
        var after = before
        var d: ProjetVu.Disque? = null
        var effective = 0
        val moved: Boolean
        val b: Ecoute.Bilan
        try {
            ouvrirTempo()
            if (!j.exists(tag)) return nonMesure(step, "pas de bouton « ${if (haut) "×2" else "÷2"} » dans la feuille du tempo")
            // The touches that changed it (×2 is greyed out past the app's range): as many back to put it back.
            repeat(FOIS_TEMPO) {
                val t0 = lu().tempo
                runCatching { j.tap(tag) }
                if (attendre(2_000) { abs(lu().tempo - t0) > 0.01 }) effective++
            }
            moved = attendre(5_000) { if (haut) lu().tempo > before else lu().tempo < before }
            after = lu().tempo
            if (moved) d = surDisque { abs(it.tempo - after) < 0.01 }
            fermerFeuilles()
            if (by == null) by = e.lancerLecture()
            b = t.ecouter(step, courtMs)
        } finally {
            fermerFeuilles()
            e.arreter()
            runCatching {
                ouvrirTempo()
                repeat(effective) { if (abs(lu().tempo - before) > 0.01) { val t0 = lu().tempo; j.tap(back); attendre(2_000) { abs(lu().tempo - t0) > 0.01 } } }
            }
            runCatching { fermerFeuilles() }
        }
        ecoute(step, b)
        feuEcoute(step, b)
        check(moved) { "le tempo ne ${if (haut) "monte" else "descend"} pas : ${fmt(lu().tempo)} bpm (avant : ${fmt(before)})" }
        check(d?.let { abs(it.tempo - after) < 0.01 } == true) { "le tempo ${fmt(after)} bpm n'est pas enregistré sur le disque" }
        check(b.joueTout) { "la lecture s'arrête à ${fmt(after)} bpm : ${b.text()}" }
        check(!b.nonFinite) { "valeur invalide (NaN) dans le son à ${fmt(after)} bpm" }
        check(b.secondes.any { it.peakDb > Ecoute.SILENCE_DB }) { "silence à ${fmt(after)} bpm : ${b.text()}" }
        bilan(step, "tempo_avant" to fmt(before), "tempo_apres" to fmt(after))
        return "tempo ${fmt(before)} puis ${fmt(after)} bpm, enregistré ; lecture par $by : ${b.text()}"
    }

    /**
     * Structure › a part's « ⋯ » › [tag] ([delta]: the parts it adds or takes), then « Annuler ». Oracle: the parts
     * [delta] more in the project and on the disk, then as before.
     */
    private fun partie(step: String, tag: String, delta: Int): String {
        auStudio()
        val before = lu().parties
        j.tap("tab_Structure")
        val there: Boolean
        try {
            j.openPartSheet(PARTIE)
            there = attendre(2_000) { j.exists(tag) }
            if (there) j.tap(tag)
            // « Insérer » asks which part to insert: the first one offered.
            if (there && tag.startsWith("part_insert") && attendre(3_000) { j.tagsMatching(PICK).isNotEmpty() }) j.tap(j.tagsMatching(PICK).first())
        } finally {
            fermerFeuilles()
        }
        if (!there) return nonMesure(step, "pas de « $tag » dans la feuille de la partie ${PARTIE + 1}")
        check(attendre(5_000) { lu().parties == before + delta }) { "${lu().parties} parties après le geste (avant : $before, attendu : ${before + delta})" }
        val d = surDisque { it.parties == before + delta }
        check(d?.parties == before + delta) { "${d?.parties ?: "aucun fichier"} parties sur le disque (attendu : ${before + delta})" }
        annuler("le geste « $tag »")
        check(attendre(5_000) { lu().parties == before }) { "« Annuler » ne remet pas le morceau : ${lu().parties} parties (attendu : $before)" }
        val back = surDisque { it.parties == before }
        check(back?.parties == before) { "après « Annuler », ${back?.parties ?: "aucun fichier"} parties sur le disque (attendu : $before)" }
        bilan(step, "parties_avant" to before, "parties_apres" to before + delta)
        return "$before parties, puis ${before + delta}, enregistré, puis $before après « Annuler »"
    }

    /**
     * Structure › a part's « ⋯ » › [tag] (a « Montée »), then « Annuler ». Oracle: the song's build-up curve gets its
     * points in the project and on the disk while the parts stay as they are (a Montée draws a curve before the part,
     * it adds none: `buildUpBefore`), then « Annuler » takes the curve back.
     */
    private fun montee(step: String, tag: String): String {
        auStudio()
        val before = lu()
        j.tap("tab_Structure")
        val there: Boolean
        try {
            j.openPartSheet(PARTIE)
            there = attendre(2_000) { j.exists(tag) }
            if (there) j.tap(tag)
        } finally {
            fermerFeuilles()
        }
        if (!there) return nonMesure(step, "pas de « $tag » dans la feuille de la partie ${PARTIE + 1}")
        check(attendre(5_000) { lu().montee != before.montee }) { "pas de courbe de montée après le geste : ${lu().montee} points (avant : ${before.montee})" }
        val after = lu()
        check(after.parties == before.parties) { "${after.parties} parties après la montée (avant : ${before.parties}) : une montée ajoute une courbe, pas une partie" }
        val d = surDisque { it.montee == after.montee }
        check(d?.montee == after.montee) { "${d?.montee ?: "aucun fichier"} points de montée sur le disque (attendu : ${after.montee})" }
        annuler("la montée")
        check(attendre(5_000) { lu().montee == before.montee }) { "« Annuler » ne retire pas la montée : ${lu().montee} points (attendu : ${before.montee})" }
        val back = surDisque { it.montee == before.montee }
        check(back?.montee == before.montee) { "la montée annulée reste sur le disque : ${back?.montee ?: "aucun fichier"} points (attendu : ${before.montee})" }
        bilan(step, "montee_avant" to before.montee, "montee_apres" to after.montee, "parties" to before.parties)
        return "courbe de montée : ${before.montee} points, puis ${after.montee}, enregistrée, ${before.parties} parties avant et après, puis ${before.montee} après « Annuler »"
    }

    // --- The catalogue (tour 9, lot A3): one crossing each, one fault no other scenario finds ------------

    /** The slots of strip [ch]'s effect chips on screen (its sheet open). */
    private fun puces(ch: Int): Set<Int> =
        j.tagsMatching(Regex("fx_chip_${ch}_(\\d+)")).mapNotNull { it.substringAfterLast('_').toIntOrNull() }.toSet()

    /**
     * [type] added to strip [ch] by its sheet's list, the sheet left open: the new chip's slot (null: no new chip
     * on screen). The caller closes the sheet.
     */
    private fun ajouterUn(ch: Int, type: String): Int? {
        bandeau(ch)
        val had = puces(ch)
        val n = lu().effets.getOrNull(ch).orEmpty().size
        if (!j.exists("fx_picker_$ch")) {
            j.tap("fx_add_$ch")
            j.await("fx_picker_$ch")
        }
        j.tap("fx_pick_${ch}_$type")
        check(attendre(5_000) { lu().effets.getOrNull(ch).orEmpty().size == n + 1 }) {
            "$type ne s'ajoute pas à la piste ${ch + 1} : ${lu().effets.getOrNull(ch).orEmpty().ifEmpty { listOf("aucun") }.joinToString(", ")}"
        }
        runCatching { if (j.exists("fx_picker_$ch")) j.tap("fx_add_$ch") }
        attendre(3_000) { (puces(ch) - had).isNotEmpty() }
        return (puces(ch) - had).singleOrNull()
    }

    /** Why a long effect (a reverb, a delay) cannot go in the song any more (null: it can). */
    private fun tropDeLongs(et: EffectType): String? {
        if (!et.long) return null
        val n = lu().effets.flatten().count { name -> EffectType.entries.firstOrNull { it.name == name }?.let { EffectType.samePool(it, et) } == true }
        return if (n >= EffectType.longLimit(et)) "déjà $n effets de ce genre dans le morceau (l'appli en permet ${EffectType.longLimit(et)})" else null
    }

    /** The strip [ch] for one effect [type] more (an error text when there is none, or the type is unknown). */
    private fun placePour(type: String): Pair<Int?, String?> {
        val et = EffectType.entries.firstOrNull { it.name == type } ?: return null to "l'appli n'a pas d'effet $type"
        tropDeLongs(et)?.let { return null to it }
        val ch = place(1) ?: return null to "aucune des $PISTES_MAX premières pistes n'a de place libre"
        return ch to null
    }

    /**
     * Pro: an effect [type] added, then taken off by its « ✕ » (not by « Annuler »), then « Annuler » brings it back.
     * Oracle: gone from the project and the disk after ✕, back after « Annuler », then the strip as it was.
     */
    private fun effetRetrait(step: String, type: String): String {
        auStudio()
        val (ch, why) = placePour(type)
        if (ch == null) return nonMesure(step, why.orEmpty())
        val before = lu().effets[ch].sorted()
        val want = (before + type).sorted()
        try {
            val slot = try { ajouterUn(ch, type) } catch (t: Throwable) { fermerFeuilles(); throw t }
            try {
                check(slot != null) { "$type ajouté à la piste ${ch + 1}, mais aucune nouvelle puce d'effet à l'écran" }
                check(j.exists("fx_remove_${ch}_$slot")) { "pas de « ✕ Retirer l'effet » pour $type (piste ${ch + 1})" }
                j.tap("fx_remove_${ch}_$slot")
            } finally {
                fermerFeuilles()
            }
            check(attendre(5_000) { lu().effets[ch].sorted() == before }) { "« ✕ » ne retire pas $type : ${lu().effets[ch].joinToString(", ")}" }
            val d = surDisque { it.effets.getOrNull(ch).orEmpty().sorted() == before }
            check(d?.effets?.getOrNull(ch).orEmpty().sorted() == before && d != null) { "$type retiré reste sur le disque (piste ${ch + 1})" }
            annuler("le retrait de $type")
            check(attendre(5_000) { lu().effets[ch].sorted() == want }) { "« Annuler » ne remet pas $type sur la piste ${ch + 1} : ${lu().effets[ch].joinToString(", ")}" }
            val back = surDisque { it.effets.getOrNull(ch).orEmpty().sorted() == want }
            check(back?.effets?.getOrNull(ch).orEmpty().sorted() == want && back != null) { "$type remis par « Annuler » n'est pas sur le disque (piste ${ch + 1})" }
            check(retirer(ch, before, 2)) { "la piste ${ch + 1} ne revient pas comme avant : ${lu().effets[ch].joinToString(", ")}" }
        } finally {
            runCatching { retirer(ch, before, 3) }
        }
        bilan(step, "piste" to ch + 1)
        return "$type sur la piste ${ch + 1}, retiré par « ✕ » (projet et disque), remis par « Annuler », puis la piste comme avant"
    }

    /**
     * Pro: an effect [type] added, its first knob turned to its [haut] end (1) or its low end (0). Oracle: the knob's
     * value in the project and on the disk; after « Annuler » the old value or no effect; the song still sounds.
     */
    private fun effetReglage(step: String, type: String, haut: Boolean): String {
        auStudio()
        val et = EffectType.entries.firstOrNull { it.name == type } ?: return nonMesure(step, "l'appli n'a pas d'effet $type")
        if (et.params.isEmpty()) return nonMesure(step, "$type n'a pas de réglage")
        val (ch, why) = placePour(type)
        if (ch == null) return nonMesure(step, why.orEmpty())
        val before = lu().effets[ch]
        val cible = if (haut) 1f else 0f
        var lu0 = Float.NaN
        var vieux = Float.NaN
        var idx = -1
        try {
            val set: Boolean
            try {
                val slot = ajouterUn(ch, type)
                check(slot != null) { "$type ajouté à la piste ${ch + 1}, mais aucune nouvelle puce d'effet à l'écran" }
                val now = lu().effets[ch]
                idx = now.indices.firstOrNull { it >= before.size || now[it] != before[it] } ?: before.size
                vieux = lu().reglages.getOrNull(ch)?.getOrNull(idx)?.getOrNull(0) ?: Float.NaN
                // A knob already at the end aimed at would pass with a dead slider: not measured, not green.
                if (abs(vieux - cible) < PAS_REGLAGE) return nonMesure(step, "${et.params[0].label} de $type est déjà à ${fmt(cible)}")
                if (!j.exists("fx_editor_${ch}_$slot")) {
                    j.tap("fx_chip_${ch}_$slot")
                    attendre(3_000) { j.exists("fx_editor_${ch}_$slot") }
                }
                set = regler("fxp_${ch}_${slot}_0", cible)
            } finally {
                fermerFeuilles()
            }
            if (!set) return nonMesure(step, "le premier réglage de $type ne se règle pas d'ici")
            fun valeur(p: ProjetVu) = p.reglages.getOrNull(ch)?.getOrNull(idx)?.getOrNull(0)
            check(attendre(5_000) { valeur(lu())?.let { abs(it - cible) < PAS_REGLAGE } == true }) {
                "${et.params[0].label} de $type réglé à ${fmt(cible)}, le projet a ${valeur(lu())?.let(::fmt) ?: "rien"}"
            }
            lu0 = valeur(lu()) ?: Float.NaN
            val d = surDisque { r -> r.reglages.getOrNull(ch)?.getOrNull(idx)?.getOrNull(0)?.let { abs(it - cible) < PAS_REGLAGE } == true }
            check(d?.reglages?.getOrNull(ch)?.getOrNull(idx)?.getOrNull(0)?.let { abs(it - cible) < PAS_REGLAGE } == true) {
                "${et.params[0].label} de $type à ${fmt(cible)} n'est pas enregistré sur le disque (piste ${ch + 1})"
            }
            val by = e.lancerLecture()
            val b = try { t.ecouter(step, courtMs) } finally { e.arreter() }
            ecoute(step, b)
            feuEcoute(step, b)
            check(!b.nonFinite) { "valeur invalide (NaN) dans le son avec $type à ${fmt(cible)}" }
            check(b.secondes.any { it.peakDb > Ecoute.SILENCE_DB }) { "silence avec $type à ${fmt(cible)} : ${b.text()}" }
            // « Annuler »: the knob back, or (the knob's step merged with the add) the effect gone.
            annuler("le réglage de $type")
            check(attendre(5_000) { lu().effets[ch] == before || valeur(lu())?.let { abs(it - vieux) < PAS_REGLAGE } == true }) {
                "« Annuler » ne remet pas ${et.params[0].label} de $type : ${valeur(lu())?.let(::fmt) ?: "rien"} (avant : ${fmt(vieux)})"
            }
            check(retirer(ch, before.sorted(), 3)) { "la piste ${ch + 1} ne revient pas comme avant : ${lu().effets[ch].joinToString(", ")}" }
            bilan(step, "piste" to ch + 1, "avant" to fmt(vieux), "apres" to fmt(lu0))
            return "${et.params[0].label} de $type (piste ${ch + 1}) : ${fmt(vieux)} puis ${fmt(lu0)}, enregistré ; lecture par $by : ${b.text()} ; annulé"
        } finally {
            runCatching { retirer(ch, before.sorted(), 3) }
        }
    }

    /** Waits for the screen [mark] (ending in _: a prefix) of a tab. */
    private fun ecranLa(mark: String, ms: Long = 15_000): Boolean =
        if (mark.endsWith("_")) attendre(ms) { j.tagsMatching(Regex(Regex.escape(mark) + ".+")).isNotEmpty() } else attendre(ms) { j.exists(mark) }

    /** [open] touched (from Structure when the top bar is not there), then its [mark] on screen. */
    private fun ouvrirOnglet(open: String, mark: String) {
        if (!j.exists(open)) j.tap("tab_Structure")
        j.tap(open)
        check(ecranLa(mark)) { "l'écran « $open » ne s'ouvre pas" }
    }

    /** Back to the song from a screen (Mes projets: « ← » then « Continuer »). */
    private fun quitterOnglet() {
        if (j.exists("projects") && !j.exists("launch")) runCatching { j.tap("projects_back"); j.await("launch") }
        if (j.exists("launch")) runCatching { j.tap("launch_continue") }
        runCatching { j.awaitAny(*EssaiEclair.STUDIO, timeoutMs = 30_000) }
        runCatching { if (j.exists("tab_Structure")) j.tap("tab_Structure") }
    }

    /**
     * Intermediate: the screen [name] open, then the phone turned ([how] « rotation »), the app sent behind another
     * screen (« fond ») or the studio closed and opened again (« relance »). Oracle: the same screen comes back (the
     * same song after a relaunch, untouched).
     */
    private fun onglet(step: String, name: String, how: String): String {
        val (_, open, mark) = EssaiEclair.SCREENS.firstOrNull { it.first == name } ?: error("pas d'écran « $name »")
        auStudio()
        val before = lu()
        try {
            ouvrirOnglet(open, mark)
            when (how) {
                "rotation" -> {
                    try {
                        appli.orienter(true)
                        j.idle(1_500)
                        check(ecranLa(mark, 30_000)) { "« $name » perdu quand le téléphone se couche" }
                        appli.orienter(false)
                        j.idle(1_500)
                        check(ecranLa(mark, 30_000)) { "« $name » perdu quand le téléphone se redresse" }
                    } finally {
                        appli.orienter(null)
                    }
                }
                "fond" -> {
                    val done = try {
                        appli.arrierePlan { j.idle(1_000) }
                    } finally {
                        if (appli.studioDevant() == false) attendre(15_000) { appli.studioDevant() != false }
                    }
                    if (!done) return nonMesure(step, "le studio ne passe pas derrière un autre écran ici")
                    check(appli.studioDevant() != false) { "le studio ne revient pas devant" }
                    check(ecranLa(mark, 30_000)) { "« $name » perdu après un passage au fond" }
                }
                "relance" -> {
                    surDisque { ecarts(before.trace(), it.trace()).isEmpty() }
                    if (!appli.relancerStudio()) return nonMesure(step, "le studio ne se ferme et ne se rouvre pas d'ici")
                    j.awaitAny("intro", "welcome", "launch", *EssaiEclair.STUDIO, timeoutMs = 60_000)
                    j.awaitGone("intro", 60_000)
                    e.auStudioPro()
                    val back = lu()
                    check(back.id == before.id) { "rouvert sur « ${back.nom} » au lieu de « ${before.nom} »" }
                    val diff = ecarts(before.trace(), back.trace())
                    check(diff.isEmpty()) { "le morceau rouvert n'est plus le même : ${diff.joinToString(", ")}" }
                    ouvrirOnglet(open, mark)
                }
                else -> error("geste « $how » inconnu")
            }
        } finally {
            runCatching { quitterOnglet() }
        }
        return "« $name » ouvert, ${QUOI_ONGLET[how] ?: how}, et le même écran revient"
    }

    /**
     * Intermediate: a part's gesture [tag] ([delta] parts), « Annuler », then « Rétablir ». Oracle: the parts [delta] more
     * again after « Rétablir », in the project and on the disk; then « Annuler » once more (tidying).
     */
    private fun partieRedo(step: String, tag: String, delta: Int): String {
        auStudio()
        val before = lu().parties
        j.tap("tab_Structure")
        val there: Boolean
        try {
            j.openPartSheet(PARTIE)
            there = attendre(2_000) { j.exists(tag) }
            if (there) j.tap(tag)
            if (there && tag.startsWith("part_insert") && attendre(3_000) { j.tagsMatching(PICK).isNotEmpty() }) j.tap(j.tagsMatching(PICK).first())
        } finally {
            fermerFeuilles()
        }
        if (!there) return nonMesure(step, "pas de « $tag » dans la feuille de la partie ${PARTIE + 1}")
        try {
            return partieRedoSuite(step, tag, delta, before)
        } finally {
            // A failure half-way leaves the song as found for the next scenario.
            runCatching { repeat(3) { if (lu().parties != before && j.exists("top_undo")) { j.tap("top_undo"); attendre(3_000) { lu().parties == before } } } }
        }
    }

    private fun partieRedoSuite(step: String, tag: String, delta: Int, before: Int): String {
        check(attendre(5_000) { lu().parties == before + delta }) { "${lu().parties} parties après le geste (avant : $before, attendu : ${before + delta})" }
        annuler("le geste « $tag »")
        check(attendre(5_000) { lu().parties == before }) { "« Annuler » ne remet pas le morceau : ${lu().parties} parties (attendu : $before)" }
        // A narrow phone has no room for « Rétablir » in the bar: it is in the thumb bar.
        val redo = REDO.firstOrNull { attendre(5_000) { j.exists(it) } }
        check(redo != null) { "pas de « Rétablir » après « Annuler »" }
        j.tap(redo)
        check(attendre(5_000) { lu().parties == before + delta }) { "« Rétablir » ne refait pas « $tag » : ${lu().parties} parties (attendu : ${before + delta})" }
        val d = surDisque { it.parties == before + delta }
        check(d?.parties == before + delta) { "après « Rétablir », ${d?.parties ?: "aucun fichier"} parties sur le disque (attendu : ${before + delta})" }
        annuler("le geste « $tag » rétabli")
        check(attendre(5_000) { lu().parties == before }) { "le second « Annuler » ne remet pas le morceau : ${lu().parties} parties (attendu : $before)" }
        val back = surDisque { it.parties == before }
        check(back?.parties == before) { "${back?.parties ?: "aucun fichier"} parties sur le disque après le second « Annuler » (attendu : $before)" }
        bilan(step, "parties_avant" to before, "parties_apres" to before + delta)
        return "« $tag » : $before parties, ${before + delta}, annulé ($before), rétabli (${before + delta}), enregistré, puis annulé"
    }

    /** [n] typed on the tempo sheet's keypad (the sheet open). */
    private fun taper(n: Int) {
        repeat(3) { if (j.exists("tempo_key_erase")) runCatching { j.tap("tempo_key_erase") } }
        n.toString().forEach { j.tap("tempo_key_$it") }
    }

    /**
     * Pro: the tempo typed at the app's lowest ([haut] false) or highest, then « − » or « + » past it, then one bpm
     * outside typed. Oracle: the limit in the project and on the disk, « − »/« + » stays there, the bpm outside is
     * refused (OK does nothing), the song plays there with sound, no NaN; then « Annuler » puts the tempo back.
     */
    private fun tempoLimite(step: String, haut: Boolean): String {
        auStudio()
        val before = lu().tempo
        val limit = if (haut) TEMPO_MAX else TEMPO_MIN
        val past = if (haut) limit + 1 else limit - 1
        var by: String? = null
        val b: Ecoute.Bilan
        try {
            ouvrirTempo()
            if (!j.exists("tempo_key_ok")) return nonMesure(step, "pas de pavé de chiffres dans la feuille du tempo")
            taper(limit)
            j.tap("tempo_key_ok")
            check(attendre(5_000) { abs(lu().tempo - limit) < 0.01 }) { "$limit tapé puis OK, le tempo est à ${fmt(lu().tempo)} bpm" }
            val d = surDisque { abs(it.tempo - limit) < 0.01 }
            check(d?.let { abs(it.tempo - limit) < 0.01 } == true) { "le tempo $limit bpm n'est pas enregistré sur le disque" }
            fermerFeuilles()
            ouvrirTempo()
            runCatching { j.tap(if (haut) "tempo_plus" else "tempo_minus") }
            j.idle(500)
            check(abs(lu().tempo - limit) < 0.01) { "« ${if (haut) "+" else "−"} » fait sortir le tempo de la limite : ${fmt(lu().tempo)} bpm" }
            taper(past)
            runCatching { j.tap("tempo_key_ok") }
            j.idle(500)
            check(abs(lu().tempo - limit) < 0.01) { "$past bpm tapé est accepté : ${fmt(lu().tempo)} bpm (l'appli joue de $TEMPO_MIN à $TEMPO_MAX)" }
            fermerFeuilles()
            by = e.lancerLecture()
            b = t.ecouter(step, courtMs)
        } finally {
            fermerFeuilles()
            e.arreter()
            // The whole sheet is one undo step: « Annuler » until the tempo is back.
            runCatching { repeat(3) { if (abs(lu().tempo - before) > 0.01 && j.exists("top_undo")) { j.tap("top_undo"); attendre(3_000) { abs(lu().tempo - before) < 0.01 } } } }
        }
        ecoute(step, b)
        feuEcoute(step, b)
        check(b.joueTout) { "la lecture s'arrête à $limit bpm : ${b.text()}" }
        check(!b.nonFinite) { "valeur invalide (NaN) dans le son à $limit bpm" }
        check(b.secondes.any { it.peakDb > Ecoute.SILENCE_DB }) { "silence à $limit bpm : ${b.text()}" }
        check(attendre(5_000) { abs(lu().tempo - before) < 0.01 }) { "« Annuler » ne remet pas le tempo à ${fmt(before)} : ${fmt(lu().tempo)} bpm" }
        bilan(step, "tempo_avant" to fmt(before), "tempo_limite" to limit)
        return "tempo $limit bpm tapé, enregistré, « ${if (haut) "+" else "−"} » et $past refusés ; lecture par $by : ${b.text()} ; remis à ${fmt(before)}"
    }

    /** Intermediate: the screens [tabs] (tab, its marker) opened while the song plays. Oracle: the transport plays at each one, and sound comes out. */
    private fun lectureEcrans(step: String, tabs: List<Pair<String, String>>): String {
        auStudio()
        val by = e.lancerLecture()
        val stops = ArrayList<String>()
        val b: Ecoute.Bilan
        try {
            for ((tab, mark) in tabs) {
                j.tap(tab)
                j.await(mark)
                j.idle(500)
                if (!sonde.lire().playing) stops += tab.removePrefix("tab_")
            }
            b = t.ecouter(step, courtMs)
        } finally {
            runCatching { j.tap("tab_Structure") }
            e.arreter()
        }
        ecoute(step, b)
        feuEcoute(step, b)
        check(stops.isEmpty()) { "la musique s'arrête en ouvrant ${stops.distinct().joinToString(", ")}" }
        check(b.joueTout) { "la lecture s'arrête après les changements d'écran : ${b.text()}" }
        check(!b.nonFinite) { "valeur invalide (NaN) dans le son" }
        check(b.secondes.any { it.peakDb > Ecoute.SILENCE_DB }) { "silence après les changements d'écran : ${b.text()}" }
        return "${tabs.size} écran(s) ouverts en jouant (lecture par $by) : ${b.text()}"
    }

    /** Measure: ▶ then ■ [REPETITIONS] times. Oracle: each ▶ plays with sound, each ■ stops; then the song still sounds, no NaN. */
    private fun jouerArreter(step: String): String {
        auStudio()
        e.arreter()
        var departs = 0
        var arrets = 0
        repeat(REPETITIONS) {
            if (runCatching { e.lancerLecture() }.isFailure || !attendre(5_000) { sonde.lire().playing }) departs++
            e.arreter()
            if (!attendre(5_000) { !sonde.lire().playing }) arrets++
        }
        val by = e.lancerLecture()
        val b = try { t.ecouter(step, courtMs) } finally { e.arreter() }
        ecoute(step, b)
        feuEcoute(step, b)
        bilan(step, "departs_rates" to departs, "arrets_rates" to arrets)
        check(departs == 0) { "$departs ▶ sur $REPETITIONS ne lancent pas le son" }
        check(arrets == 0) { "$arrets ■ sur $REPETITIONS n'arrêtent pas la lecture" }
        check(!b.nonFinite) { "valeur invalide (NaN) dans le son après $REPETITIONS départs" }
        return "$REPETITIONS fois ▶ puis ■ ; ensuite lecture par $by : ${b.text()}"
    }

    /** Measure: the studio behind another screen, then back, [FONDS] times while the song plays. Oracle: the music each time, the studio back each time. */
    private fun fondReprise(step: String): String {
        auStudio()
        val by = e.lancerLecture()
        var muettes = 0
        var absent = 0
        try {
            repeat(FONDS) {
                var b: Ecoute.Bilan? = null
                if (!appli.arrierePlan { b = t.ecouter(step, courtMs) }) return nonMesure(step, "le studio ne passe pas derrière un autre écran ici")
                if (appli.studioDevant() == false && !attendre(15_000) { appli.studioDevant() != false }) absent++
                val x = b
                if (x == null || !x.joueTout || x.secondes.none { it.peakDb > Ecoute.SILENCE_DB }) muettes++
                if (x != null) feuEcoute(step, x)
            }
        } finally {
            if (appli.studioDevant() == false) attendre(15_000) { appli.studioDevant() != false }
            e.arreter()
        }
        bilan(step, "fois" to FONDS, "sans_musique" to muettes, "studio_absent" to absent)
        check(muettes == 0) { "la musique s'arrête au fond $muettes fois sur $FONDS" }
        check(absent == 0) { "le studio ne revient pas devant $absent fois sur $FONDS" }
        return "$FONDS fois au fond puis devant (lecture par $by) : la musique joue, le studio revient"
    }

    // --- The « seul » pack (tour 9, A2: what Benjamin tested on his phone, played alone) ---------------

    /** Endurance: the song played [enduranceMs] (10 min). Oracle: no stop, no NaN; the memory before and after. */
    fun endurance(): String {
        auStudio()
        val avant = appli.memoireMo()
        val text = lecture(ENDURANCE, enduranceMs)
        val apres = appli.memoireMo()
        bilan(ENDURANCE, "memoire_avant_mo" to avant, "memoire_apres_mo" to apres)
        if (avant != null && apres != null) feu(ENDURANCE, memoireFeu(avant, apres))
        return text + " ; mémoire ${avant ?: "?"} → ${apres ?: "?"} Mo"
    }

    /** « Mes projets » opened and closed [OUVERTURES] times. Oracle: the memory after the last against after the first, the same song open. */
    fun fuite(): String {
        auStudio()
        e.arreter()
        if (appli.memoireMo() == null) return nonMesure(FUITE, "la mémoire ne se lit pas sur ce téléphone")
        val p = lu()
        var premiere = 0
        for (i in 1..OUVERTURES) {
            if (!j.exists("top_projects")) j.tap("tab_Structure")
            j.tap("top_projects")
            j.await("projects")
            j.tap("projects_back")
            j.awaitAny("launch", *EssaiEclair.STUDIO, timeoutMs = 15_000)
            if (j.exists("launch")) j.tap("launch_continue")
            j.awaitAny(*EssaiEclair.STUDIO, timeoutMs = 15_000)
            if (i == 1 || i == OUVERTURES) {
                Runtime.getRuntime().gc()
                j.idle(1_500)
                val mo = appli.memoireMo() ?: return nonMesure(FUITE, "la mémoire ne se lit plus")
                if (i == 1) premiere = mo else {
                    val f = memoireFeu(premiere, mo)
                    bilan(FUITE, "ouvertures" to OUVERTURES, "memoire_premiere_mo" to premiere, "memoire_derniere_mo" to mo)
                    feu(FUITE, f)
                    check(lu().id == p.id) { "après $OUVERTURES ouvertures, le morceau ouvert n'est plus « ${p.nom} »" }
                    check(f.first != Feu.ROUGE) { "fuite de mémoire : ${f.second}" }
                    return "$OUVERTURES fois « Mes projets » ouvert et fermé : ${f.second}"
                }
            }
        }
        error("inatteignable")
    }

    /** The studio's last cold start, as Android timed it. Oracle: launch to first image under 2 s (green), 5 s (orange). */
    fun demarrage(): String {
        val d = appli.demarrage() ?: return nonMesure(DEMARRAGE, "Android 15 ou plus, après un démarrage à froid")
        bilan(DEMARRAGE, "ms" to d.ms, "par" to d.par)
        val f = demarrageFeu(d.ms)
        feu(DEMARRAGE, f)
        check(f.first != Feu.ROUGE) { "démarrage à froid trop lent : ${f.second}" }
        return "démarrage à froid (${d.par}) : ${f.second}"
    }

    /**
     * The tempo changed, saved, then the app killed as a phone does it (no closing). The next step
     * ([arretRelu]) reads the song back after the relaunch.
     */
    fun arret(): String {
        auStudio()
        e.arreter()
        val p = lu()
        val before = p.tempo
        ouvrirTempo()
        j.tap("tempo_plus")
        check(attendre(5_000) { lu().tempo > before }) { "le « + » du tempo ne change rien (${fmt(before)})" }
        val after = lu().tempo
        runCatching { j.tap("tempo_close"); j.awaitGone("tempo_sheet", 5_000) }
        val d = surDisque { it.tempo == after }
        check(d?.tempo == after) { "le tempo ${fmt(after)} n'est pas sur le disque après ${sauvegardeMs / 1000} s (${d?.tempo?.let(::fmt) ?: "pas de fichier"})" }
        carnet.add(Carnet.NOTE, ARRET, "${Carnet.ARRET_VOULU};${p.id};$after")
        if (!appli.tuer()) {
            runCatching { ouvrirTempo(); j.tap("tempo_minus"); j.tap("tempo_close") }
            return nonMesure(ARRET, "l'appli ne se tue pas d'ici")
        }
        error("l'appli tuée vit encore")
    }

    /** After [arret]: the same song, with the tempo saved just before the kill. */
    fun arretRelu(): String {
        val lines = carnet.lines()
        val at = lines.indexOfLast { it.step == ARRET && it.kind == Carnet.NOTE && it.text.startsWith("${Carnet.ARRET_VOULU};") }
        if (at < 0) return nonMesure(ARRET_RELU, "pas d'arrêt brutal avant")
        // The kill that did not happen (the tempo put back): nothing to read back.
        if (lines.drop(at).any { it.step == ARRET && it.kind == Carnet.NOTE && it.text.startsWith("${ToutTester.NON_MESURE};") }) {
            return nonMesure(ARRET_RELU, "l'arrêt brutal n'a pas eu lieu")
        }
        val note = lines[at]
        val (id, tempo) = note.text.split(';').let { it[1] to it[2].toDouble() }
        auStudio()
        val back = lu()
        check(back.id == id) { "après l'arrêt brutal, le studio rouvre « ${back.nom} » au lieu du morceau tué" }
        check(back.tempo == tempo) { "après l'arrêt brutal, le tempo est ${fmt(back.tempo)} au lieu de ${fmt(tempo)}" }
        check(back.disque?.tempo == tempo) { "après l'arrêt brutal, le disque a le tempo ${back.disque?.tempo?.let(::fmt)} au lieu de ${fmt(tempo)}" }
        runCatching { ouvrirTempo(); j.tap("tempo_minus"); attendre(5_000) { lu().tempo < tempo }; j.tap("tempo_close") }
        return "après l'arrêt brutal : « ${back.nom} », tempo ${fmt(tempo)} gardé"
    }

    /** Another app takes the sound [focusMs] (a call, a voice message). Oracle: the music plays again after, the studio in front. */
    fun focus(): String {
        auStudio()
        val by = e.lancerLecture()
        if (!appli.prendreFocus(focusMs)) {
            e.arreter()
            return nonMesure(FOCUS, "le son ne se prend pas d'ici")
        }
        j.idle(1_000)
        val relance = !sonde.lire().playing
        if (relance) e.lancerLecture()
        val b = try { t.ecouter(FOCUS, 5_000) } finally { e.arreter() }
        ecoute(FOCUS, b)
        bilan(FOCUS, "relance" to if (relance) "oui" else "non")
        feuEcoute(FOCUS, b)
        if (relance) feu(FOCUS, Feu.ORANGE to "la musique ne repart pas seule après la perte du son")
        check(b.reads > 0) { "aucune mesure" }
        check(b.joueTout) { "après la perte du son, la musique ne repart pas : ${b.text()}" }
        check(!b.nonFinite) { "valeur invalide (NaN) dans le son" }
        check(appli.studioDevant() != false) { "après la perte du son, le studio n'est plus devant" }
        return "son pris ${focusMs / 1000} s puis rendu (lecture par $by${if (relance) ", relancée" else ""}) : ${b.text()}"
    }

    /** The engine's biggest buffer ([TAMPON_GROS] blocks). Oracle: it plays, no NaN, no more dropouts than the usual buffer. */
    fun tampon(): String {
        auStudio()
        val by = e.lancerLecture()
        val normal = t.ecouter("${TAMPON}_normal", tamponMs)
        val avant = sonde.lire().buffer
        if (!appli.tampon(TAMPON_GROS)) {
            e.arreter()
            return nonMesure(TAMPON, "le tampon du moteur ne se règle pas d'ici")
        }
        val gros: Ecoute.Bilan
        val pendant: Int
        try {
            j.idle(500)
            pendant = sonde.lire().buffer
            gros = t.ecouter(TAMPON, tamponMs)
        } finally {
            appli.tampon(null)
            e.arreter()
        }
        ecoute(TAMPON, gros)
        bilan(TAMPON, "tampon_avant" to avant, "tampon_pendant" to pendant, "decrochages_normal" to normal.xruns)
        feuEcoute(TAMPON, gros)
        if (avant > 0 && pendant <= avant) feu(TAMPON, Feu.ORANGE to "le tampon lu n'a pas grandi ($avant → $pendant)")
        check(gros.reads > 0) { "aucune mesure" }
        check(gros.joueTout) { "avec le gros tampon, la lecture s'arrête : ${gros.text()}" }
        check(!gros.nonFinite) { "valeur invalide (NaN) dans le son" }
        check(gros.xruns <= normal.xruns) { "plus de décrochages avec le gros tampon (${gros.xruns}) qu'avec le tampon habituel (${normal.xruns})" }
        return "tampon de $TAMPON_GROS blocs (lecture par $by) : ${gros.text()}"
    }

    /** One scenario of the run: its step's name, what the report calls it, whose use it is. */
    data class Scenario(
        val id: String, val label: String, val niveau: String, val maxMs: Long,
        /** Its usual length on a phone (the run's estimate, under 20 min in all), and the precise result its oracle reads. */
        val estimeS: Int = 0, val oracle: String = "",
        /** A table line of the full pack (« style:techno », « muet:1 »…); null: one of the quick pack's own. */
        val geste: String? = null,
    )

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

        // Added on 2026-11-22 (30 scenarios).
        const val TEMPO = "tempo"
        const val MUET = "muet"
        const val ANNULER = "annuler_retablir"
        const val PADS = "pads_live"
        const val DUPLIQUER = "dupliquer"
        const val RENOMMER = "renommer"
        const val SUPPRIMER = "supprimer"
        const val SOLO = "solo"
        const val REPRISE = "reprise_fermeture"
        const val ARRIERE_PLAN = "arriere_plan"
        const val EFFETS_PLUSIEURS = "effets_plusieurs"

        const val ANNULER_EFFET = "annuler_effet"
        const val TEMPO_LECTURE = "tempo_lecture"
        const val ROTATION_LECTURE = "rotation_lecture"
        const val LECTURE_LONGUE = "lecture_longue"

        /** The names « Tout tester » writes these lights under. */
        const val EXPORT_WAV = "export_wav"
        const val EXPORT_FLAC = "export_flac"
        const val IMAGES = "images"
        const val STYLE_TRANCE = "style_trance"
        const val STYLE_DUBSTEP = "style_dubstep"
        const val STYLE_DNB = "style_dnb"
        const val GUIDE = "guide_house"

        /** The name the robot gives its copy: the only project, with this run's copy, it may delete. */
        const val NOM_ROBOT = "BenStudio Test (copie du robot)"

        /** The effects put together on one strip, and the one added then undone. */
        val TROIS = listOf("EQ", "COMPRESSOR", "FILTER")
        const val EFFET_ANNULE = "COMPRESSOR"
        const val PISTES_MAX = 8
        const val PAS_TEMPO = 5

        /** The whole run's estimated length at most (Benjamin, 2026-11-22: « le passage entier reste sous 20 min »). */
        const val DUREE_MAX_S = 20 * 60

        /**
         * The packs (Benjamin, 2026-11-22: 100 scenarios more, the whole run under 45 min, else a choice): the quick one
         * (the first 31, under [DUREE_MAX_S]) and the full one (all of them, under [DUREE_COMPLET_MAX_S] by estimate:
         * the estimates are about twice the phone's real time, 7 min 27 s for the quick pack's 19).
         */
        const val RAPIDE = "rapide"
        const val COMPLET = "complet"
        /** « Refaire les ratés »: the scenarios failed or not measured in the 2026-11-22 run (sound cut, Android Auto). */
        const val RATES = "rates"
        const val GUIDES = "guides"
        /** Every pack the robot can play: the first ones, then each theme's chunks ([THEMES]). */
        val PAQUETS by lazy { listOf(RAPIDE, COMPLET, RATES, SEUL, GUIDES) + CHUNKS.keys }
        const val DUREE_COMPLET_MAX_S = 65 * 60
        /** « Tests longs »: what the phone tested alone (tour 9, A2), played apart from the other packs. */
        const val SEUL = "seul"
        const val DUREE_SEUL_MAX_S = 30 * 60
        const val ENDURANCE = "endurance_10min"
        const val FUITE = "fuite_memoire"
        const val DEMARRAGE = "demarrage_froid"
        const val ARRET = "arret_brutal"
        const val ARRET_RELU = "arret_brutal_relu"
        const val FOCUS = "focus_audio"
        const val TAMPON = "tampon_8"
        const val VIDEO_SON = "export_video_son"
        const val OUVERTURES = 20
        const val TAMPON_GROS = 8
        /** The memory's rise that is a leak: over 20 % and at least 20 MB (red); 20 MB alone is to be watched (orange). */
        const val FUITE_MAX = 0.20
        const val FUITE_MIN_MO = 20
        /** A cold start's launch to first image (Android vitals: 5 s is « excessive »). */
        const val DEMARRAGE_VERT_MS = 2_000L
        const val DEMARRAGE_MAX_MS = 5_000L

        fun memoireFeu(avant: Int, apres: Int): Pair<Feu, String> {
            val hausse = apres - avant
            val text = "mémoire $avant → $apres Mo"
            return when {
                hausse >= FUITE_MIN_MO && hausse > avant * FUITE_MAX -> Feu.ROUGE to "$text (+$hausse Mo, plus de ${(FUITE_MAX * 100).toInt()} %)"
                hausse >= FUITE_MIN_MO -> Feu.ORANGE to "$text (+$hausse Mo)"
                else -> Feu.VERT to text
            }
        }

        fun demarrageFeu(ms: Long): Pair<Feu, String> = when {
            ms <= DEMARRAGE_VERT_MS -> Feu.VERT to "$ms ms"
            ms <= DEMARRAGE_MAX_MS -> Feu.ORANGE to "$ms ms (plus de ${DEMARRAGE_VERT_MS / 1000} s)"
            else -> Feu.ROUGE to "$ms ms (plus de ${DEMARRAGE_MAX_MS / 1000} s)"
        }

        val SEULS = listOf(
            Scenario(ENDURANCE, "Mesure : 10 minutes de lecture", NIVEAU_MESURE, 720_000, 640, "l'arrêt et les décrochages sur 10 min ; la mémoire avant et après"),
            Scenario(FUITE, "Mesure : 20 fois « Mes projets » ouvert et fermé", NIVEAU_MESURE, 600_000, 300, "la mémoire après la 20e fois contre la 1re"),
            Scenario(FOCUS, "Moyen : une autre appli prend le son", NIVEAU_MOYEN, 120_000, 40, "la lecture repart après, le studio devant"),
            Scenario(TAMPON, "Pro : le plus gros tampon du moteur (8 blocs)", NIVEAU_PRO, 180_000, 60, "la lecture, ni NaN ni plus de décrochages qu'avant"),
            Scenario(VIDEO_SON, "Moyen : la vidéo exportée a du son", NIVEAU_MOYEN, 600_000, 120, "la vidéo relue : image, durée, crête du son"),
            Scenario(ARRET, "Moyen : l'appli tuée d'un coup", NIVEAU_MOYEN, 120_000, 40, "le tempo changé est sur le disque avant l'arrêt"),
            Scenario(ARRET_RELU, "Moyen : le morceau après l'arrêt brutal", NIVEAU_MOYEN, 120_000, 30, "le même morceau et son tempo, projet et disque"),
            Scenario(DEMARRAGE, "Mesure : le démarrage à froid", NIVEAU_MESURE, 180_000, 20, "le temps d'Android de la relance après l'arrêt brutal à la première image"),
        )
        private const val PAQUET = "paquet"

        /** The ids to redo after a run: the earlier ones that did not pass this time either (or were not reached), and this run's failures. */
        fun ratesApres(avant: Set<String>, run: List<Resultat>): Set<String> {
            val passes = run.filter { it.resultat == PASSE }.map { it.scenario.id }.toSet()
            return TOUS.map { it.id }.filter { (it in avant && it !in passes) || run.any { r -> r.scenario.id == it && r.resultat != PASSE } }.toSet()
        }

        /** A pack's scenarios. */
        fun liste(paquet: String, rates: Set<String> = emptySet()): List<Scenario> = when (paquet) {
            RAPIDE -> LISTE.filter { it.geste == null }
            // The brutal stop's sequels need the stop just before them: it is played again with them.
            RATES -> {
                val ids = if (rates.any { it == ARRET_RELU || it == DEMARRAGE }) rates + ARRET else rates
                TOUS.filter { it.id in ids }
            }
            SEUL -> SEULS
            GUIDES -> TOUS.filter { it.geste?.startsWith("guide:") == true }
            else -> CHUNKS[paquet] ?: LISTE
        }

        /** The theme of a scenario ([THEMES]); null: the quick pack's own (no table line). */
        fun theme(s: Scenario): String? {
            if (s in SEULS) return "endurance"
            val kind = s.geste?.substringBefore(':') ?: return null
            return THEMES.entries.firstOrNull { kind in it.value }?.key
        }

        /** The themes, by the kind of their gesture. */
        val THEMES = linkedMapOf(
            "son" to setOf("style", "style_tempo", "tempo", "muet", "solo", "volume", "pan"),
            "effets" to setOf("effet", "effet_retrait", "effet_reglage"),
            "ecrans" to setOf("ecrans", "onglet"),
            "structure" to setOf("partie", "montee", "partie_redo"),
            "live" to setOf("pad"),
            "exports" to setOf("export"),
            "limites" to setOf("limite"),
            "endurance" to setOf("repetition"),
        )

        /** A pack's name for Benjamin (« Son 1 », « Guidés »). */
        fun nomPaquet(name: String): String {
            if (name == GUIDES) return "Guidés"
            val th = name.substringBeforeLast('_')
            val word = mapOf(
                "son" to "Son", "effets" to "Effets", "ecrans" to "Écrans", "structure" to "Structure", "live" to "Live",
                "exports" to "Exports", "limites" to "Limites", "endurance" to "Endurance",
            )[th] ?: th
            return "$word ${name.substringAfterLast('_')}"
        }

        /** A pack's longest estimate: half an hour. */
        const val DUREE_PAQUET_MAX_S = 30 * 60

        /** Each theme cut into packs of [DUREE_PAQUET_MAX_S] at most, by estimate (« son_1 », « son_2 »…), in the list's order. */
        val CHUNKS: Map<String, List<Scenario>> by lazy {
            val out = LinkedHashMap<String, List<Scenario>>()
            for (th in THEMES.keys) {
                var n = 0
                var cur = ArrayList<Scenario>()
                var sum = 0
                for (s in TOUS.filter { theme(it) == th }) {
                    if (cur.isNotEmpty() && sum + s.estimeS > DUREE_PAQUET_MAX_S) { out["${th}_${++n}"] = cur; cur = ArrayList(); sum = 0 }
                    cur += s
                    sum += s.estimeS
                }
                if (cur.isNotEmpty()) out["${th}_${++n}"] = cur
            }
            out
        }

        /** The pack the logbook's last scenarios were played in (none noted: the full list). */
        fun paquet(lines: List<Carnet.Ligne>): String {
            val ids = TOUS.map { it.id }.toSet()
            return lines.lastOrNull { it.step in ids && it.kind == Carnet.NOTE && it.text.startsWith("$PAQUET;") }?.text?.substringAfter(';')?.takeIf { it in PAQUETS } ?: COMPLET
        }

        /** The slider's smallest move the oracle takes, the tempo's ×2 or ÷2 touches, the repetitions. */
        const val PAS_CURSEUR = 0.05f
        const val FOIS_TEMPO = 3
        const val REPETITIONS = 10
        const val FONDS = 5

        /** The styles the quick pack has no scenario of (the assistant's, by its order), and its favourites among them. */
        val STYLES_NEUFS = listOf(
            "techno", "detroit", "acid_techno", "hard_techno", "industrial_techno", "retro_techno", "acidcore",
            "deep_house", "tech_house", "acid_house", "uk_garage", "jump_up",
            "minimal", "gabber", "afro_house", "speed_garage", "amapiano", "disco", "liquid_dnb", "neurofunk", "jungle", "riddim",
            "trap", "drill", "phonk", "boom_bap", "grime", "jersey_club", "lofi", "reggaeton", "afrobeats", "synthwave", "pop",
        )
        private const val FAVORIS_NEUFS = 12

        /** The exports of other styles: a new song of the style, then its file read back. */
        val EXPORTS_STYLES = listOf("techno" to "m4a", "lofi" to "wav", "trap" to "flac")

        /** The screens opened while the song plays: the tab and its marker. */
        val ECRANS_LECTURE = mapOf(
            "mix" to listOf("tab_Mix" to "mixer"),
            "live" to listOf("tab_Live" to "live_screen"),
            "va_et_vient" to List(5) { listOf("tab_Mix" to "mixer", "tab_Structure" to "structure_parts") }.flatten(),
        )

        private fun qui(niveau: String) = when (niveau) {
            NIVEAU_DEBUTANT -> "Débutant"
            NIVEAU_MOYEN -> "Moyen"
            NIVEAU_PRO -> "Pro"
            else -> "Mesure"
        }

        /** The full pack's 100 scenarios, by tables. */
        private fun tables(): List<Scenario> = buildList {
            STYLES_NEUFS.forEachIndexed { i, st ->
                val niveau = when {
                    st in ToutTester.LOURDS -> NIVEAU_MESURE
                    i < FAVORIS_NEUFS -> NIVEAU_DEBUTANT
                    (i - FAVORIS_NEUFS) % 2 == 0 -> NIVEAU_MOYEN
                    else -> NIVEAU_MESURE
                }
                add(Scenario("style_$st", "${qui(niveau)} : un morceau ${st.replace('_', ' ')}", niveau, 360_000, 40, "le moteur joue, le son sort, les décrochages", "style:$st"))
            }
            for (ch in 1..3) add(Scenario("piste${ch + 1}_muet", "Débutant : couper la piste ${ch + 1} (Muet)", NIVEAU_DEBUTANT, 90_000, 10, "le « muet » de la piste dans le projet et sur le disque", "muet:$ch"))
            for (ch in listOf(0, 2, 3)) add(Scenario("piste${ch + 1}_solo", "Moyen : la piste ${ch + 1} en solo", NIVEAU_MOYEN, 90_000, 10, "le « solo » de la piste dans le projet et sur le disque", "solo:$ch"))
            for (ch in 0 until PISTES) add(Scenario("piste${ch + 1}_volume", "Débutant : le volume de la piste ${ch + 1}", NIVEAU_DEBUTANT, 90_000, 10, "le volume de la piste dans le projet et sur le disque", "volume:$ch"))
            for (ch in 0 until PISTES) add(Scenario("piste${ch + 1}_pan", "Moyen : le panoramique de la piste ${ch + 1}", NIVEAU_MOYEN, 90_000, 10, "le panoramique de la piste dans le projet et sur le disque", "pan:$ch"))
            for (type in EffectType.entries.filter { it != EffectType.NONE && it.name != EFFET }) {
                add(Scenario("effet_${type.name.lowercase(Locale.ROOT)}", "Pro : l'effet ${type.name.lowercase(Locale.ROOT).replace('_', ' ')} sur une piste", NIVEAU_PRO, 120_000, 14, "l'effet dans le projet et sur le disque ; le son sans NaN", "effet:${type.name}"))
            }
            add(Scenario("tempo_bas", "Pro : le tempo divisé par 2, trois fois", NIVEAU_PRO, 90_000, 15, "le tempo descendu, sur le disque ; la lecture avec du son", "tempo:bas"))
            add(Scenario("tempo_haut", "Pro : le tempo doublé, trois fois", NIVEAU_PRO, 90_000, 15, "le tempo monté, sur le disque ; la lecture avec du son", "tempo:haut"))
            add(Scenario("tempo_haut_lecture", "Pro : le tempo doublé en jouant", NIVEAU_PRO, 90_000, 15, "le tempo monté, sur le disque ; la lecture sans arrêt", "tempo:haut_lecture"))
            for ((st, format) in EXPORTS_STYLES) {
                add(Scenario("export_style_${st}_$format", "Moyen : un morceau $st exporté en ${format.uppercase(Locale.ROOT)}", NIVEAU_MOYEN, 600_000, 75, "le fichier relu : durée, crête, temps pris", "export:$st:$format"))
            }
            add(Scenario("partie_inserer", "Moyen : insérer une partie puis annuler", NIVEAU_MOYEN, 90_000, 12, "une partie de plus (projet, disque), puis comme avant", "partie:part_insert_$PARTIE:1"))
            add(Scenario("partie_retirer", "Moyen : retirer une partie puis annuler", NIVEAU_MOYEN, 90_000, 12, "une partie de moins (projet, disque), puis comme avant", "partie:part_remove_$PARTIE:-1"))
            add(Scenario("partie_dupliquer", "Moyen : dupliquer une partie puis annuler", NIVEAU_MOYEN, 90_000, 12, "une partie de plus (projet, disque), puis comme avant", "partie:part_duplicate_$PARTIE:1"))
            add(Scenario("partie_montee", "Pro : une montée avant une partie, puis annuler", NIVEAU_PRO, 90_000, 12, "une courbe de montée (projet, disque), autant de parties, puis comme avant", "montee:part_buildup_4"))
            add(Scenario("partie_fin", "Pro : ajouter une fin, puis annuler", NIVEAU_PRO, 90_000, 12, "une partie de plus (projet, disque), puis comme avant", "partie:part_ending:1"))
            for (i in 1..7) add(Scenario("pad_${i + 1}", "Débutant : le pad ${i + 1} du Live", NIVEAU_DEBUTANT, 60_000, 8, "le moteur joue et la crête du son", "pad:$i"))
            add(Scenario("lecture_mix", "Moyen : ouvrir le Mix en jouant", NIVEAU_MOYEN, 90_000, 12, "le transport joue sur le Mix, le son sort", "ecrans:mix"))
            add(Scenario("lecture_live", "Moyen : ouvrir le Live en jouant", NIVEAU_MOYEN, 90_000, 12, "le transport joue sur le Live, le son sort", "ecrans:live"))
            add(Scenario("lecture_va_et_vient", "Moyen : passer 5 fois du Mix à la Structure en jouant", NIVEAU_MOYEN, 90_000, 20, "le transport joue à chaque écran, le son sort", "ecrans:va_et_vient"))
            add(Scenario("jouer_arreter_10", "Mesure : 10 fois jouer puis arrêter", NIVEAU_MESURE, 180_000, 40, "chaque départ avec du son, chaque arrêt ; ni NaN", "repetition:jouer"))
            add(Scenario("fond_reprise_5", "Mesure : 5 fois au fond puis devant, en jouant", NIVEAU_MESURE, 180_000, 45, "la musique au fond et le studio revenu, chaque fois", "repetition:fond"))
            for (st in ToutTester.GUIDE.drop(1)) add(Scenario("guide_$st", "Débutant : le Guide sur un morceau $st", NIVEAU_DEBUTANT, 480_000, 120, "« Tout est fait » à l'écran du Guide", "guide:$st"))
        }

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
            Scenario(DEBUTANT, "Débutant : un morceau par l'assistant, et il joue", NIVEAU_DEBUTANT, 420_000, 90, "le moteur joue et le son sort (ni silence ni NaN)"),
            Scenario(ECRANS, "Débutant : les cinq écrans s'ouvrent", NIVEAU_DEBUTANT, 120_000, 30, "le repère de chaque écran à l'écran"),
            Scenario(TEMPO, "Débutant : changer le tempo", NIVEAU_DEBUTANT, 90_000, 15, "le tempo du projet et du disque"),
            Scenario(MUET, "Débutant : couper une piste (Muet)", NIVEAU_DEBUTANT, 90_000, 12, "le « muet » de la piste dans le projet et sur le disque"),
            Scenario(ANNULER, "Débutant : annuler puis rétablir", NIVEAU_DEBUTANT, 90_000, 15, "le nombre de parties après chaque geste, puis sur le disque"),
            Scenario(PADS, "Débutant : toucher un pad du Live", NIVEAU_DEBUTANT, 90_000, 15, "le moteur joue et la crête du son"),
            Scenario(PROJET, "Moyen : le morceau est enregistré et se rouvre", NIVEAU_MOYEN, 120_000, 20, "le projet relu sur le disque, rouvert par « Continuer »"),
            Scenario(STRUCTURE, "Moyen : ajouter un break à la structure", NIVEAU_MOYEN, 120_000, 15, "le nombre de parties du projet et du disque"),
            Scenario(DUPLIQUER, "Moyen : dupliquer le morceau", NIVEAU_MOYEN, 90_000, 15, "un projet de plus sur le disque, avec les mêmes parties"),
            Scenario(RENOMMER, "Moyen : renommer la copie", NIVEAU_MOYEN, 90_000, 15, "le nom de la copie sur le disque"),
            Scenario(SUPPRIMER, "Moyen : supprimer la copie", NIVEAU_MOYEN, 90_000, 12, "la copie absente de la liste du disque"),
            Scenario(SOLO, "Moyen : une piste en solo", NIVEAU_MOYEN, 90_000, 12, "le « solo » de la piste dans le projet et sur le disque"),
            Scenario(REPRISE, "Moyen : fermer le studio et le rouvrir", NIVEAU_MOYEN, 150_000, 20, "le même projet (id et parties) après la réouverture"),
            Scenario(ARRIERE_PLAN, "Moyen : la musique continue au fond", NIVEAU_MOYEN, 90_000, 20, "le transport joue et le son sort, studio derrière"),
            Scenario(EFFETS, "Pro : un effet sur une piste du mixeur", NIVEAU_PRO, 180_000, 25, "l'égaliseur dans le projet, sur le disque et sur la piste ; le son"),
            Scenario(EFFETS_PLUSIEURS, "Pro : trois effets sur une piste", NIVEAU_PRO, 180_000, 25, "les trois effets dans le projet et sur le disque ; le son"),
            Scenario(ANNULER_EFFET, "Pro : annuler un effet ajouté", NIVEAU_PRO, 120_000, 15, "les effets de la piste revenus, projet et disque"),
            Scenario(TEMPO_LECTURE, "Pro : changer le tempo en jouant", NIVEAU_PRO, 90_000, 15, "le tempo du projet ; la lecture sans arrêt ni décrochage"),
            Scenario(ROTATION_LECTURE, "Pro : tourner le téléphone en jouant", NIVEAU_PRO, 120_000, 15, "le transport joue couché puis debout"),
            Scenario(LIVE, "Pro : jouer puis arrêter le Live", NIVEAU_PRO, 120_000, 20, "le moteur joue, le son sort, puis il s'arrête"),
            Scenario(EXPORT, "Export M4A relu", NIVEAU_MOYEN, 600_000, 60, "le fichier relu : durée, crête, temps pris"),
            Scenario(EXPORT_WAV, "Export WAV relu", NIVEAU_MOYEN, 600_000, 45, "le fichier relu : durée, crête, temps pris"),
            Scenario(EXPORT_FLAC, "Export FLAC relu", NIVEAU_PRO, 600_000, 45, "le fichier relu : durée, crête, temps pris"),
            Scenario(LATENCE, "Latence aller-retour", NIVEAU_MESURE, 120_000, 30, "la latence mesurée au micro (médiane)"),
            Scenario(DECROCHAGES, "Décrochages audio (30 s de lecture)", NIVEAU_MESURE, 180_000, 40, "les décrochages, le pire bloc, le plus long écart"),
            Scenario(LECTURE_LONGUE, "Pro : une lecture de 2 minutes", NIVEAU_PRO, 240_000, 125, "les décrochages et l'arrêt sur 2 min"),
            Scenario(IMAGES, "Mémoire et images lentes", NIVEAU_MESURE, 240_000, 50, "la part d'images lentes par écran, la mémoire"),
            Scenario(STYLE_TRANCE, "Débutant : un morceau trance", NIVEAU_DEBUTANT, 360_000, 60, "le moteur joue, le son sort, les décrochages"),
            Scenario(STYLE_DUBSTEP, "Moyen : un morceau dubstep", NIVEAU_MOYEN, 360_000, 60, "le moteur joue, le son sort, les décrochages"),
            Scenario(STYLE_DNB, "Style lourd : drum and bass, décrochages", NIVEAU_MESURE, 360_000, 60, "les décrochages et le pire bloc d'un style lourd"),
            Scenario(GUIDE, "Débutant : le Guide jusqu'au bout", NIVEAU_DEBUTANT, 480_000, 150, "« Tout est fait » à l'écran du Guide"),
        ) + tables()

        /** The great catalogue (tour 9, lot A3): one crossing each, played by the theme packs. */
        val CATALOGUE: List<Scenario> by lazy { catalogue() }

        /** Every scenario: the full pack, the catalogue, the « seul » pack. */
        val TOUS: List<Scenario> by lazy { LISTE + CATALOGUE + SEULS }

        /** The tempo's ends, as the app keeps them. */
        /** « Rétablir »: the bar's, or the thumb bar's on a narrow phone. */
        val REDO = listOf("top_redo", "thumb_redo")
        val TEMPO_MIN = com.pocketstudio.model.Project.MIN_TEMPO.toInt()
        val TEMPO_MAX = com.pocketstudio.model.Project.MAX_TEMPO.toInt()

        /** An effect knob's step the oracle forgives (the slider's own rounding). */
        const val PAS_REGLAGE = 0.02f

        /** The screens' crossings: what was done to the screen. */
        val QUOI_ONGLET = mapOf(
            "rotation" to "le téléphone couché puis redressé",
            "fond" to "le studio passé au fond puis revenu",
            "relance" to "le studio fermé puis rouvert",
        )

        /** The part gestures « Rétablir » is tried on: the tag and the parts it adds. */
        val GESTES_PARTIE = listOf("part_insert_$PARTIE" to 1, "part_remove_$PARTIE" to -1, "part_duplicate_$PARTIE" to 1, "part_ending" to 1)

        private fun nom(t: EffectType) = t.name.lowercase(Locale.ROOT)

        private fun catalogue(): List<Scenario> = buildList {
            for (st in STYLES_NEUFS) {
                add(Scenario("style_tempo_$st", "Pro : un morceau ${st.replace('_', ' ')}, le tempo doublé en jouant", NIVEAU_PRO, 420_000, 70, "le style joue, puis le tempo monté (projet, disque), la lecture sans arrêt ni NaN", "style_tempo:$st"))
            }
            val exported = EXPORTS_STYLES.map { it.first }.toSet()
            for (st in STYLES_NEUFS.filter { it !in exported }) {
                add(Scenario("export_style_${st}_wav", "Moyen : un morceau ${st.replace('_', ' ')} exporté en WAV", NIVEAU_MOYEN, 600_000, 75, "le fichier relu : durée, crête, temps pris", "export:$st:wav"))
            }
            val types = EffectType.entries.filter { it != EffectType.NONE }
            for (t in types) {
                add(Scenario("effet_retrait_${nom(t)}", "Pro : l'effet ${nom(t).replace('_', ' ')} retiré par ✕, puis « Annuler »", NIVEAU_PRO, 120_000, 18, "retiré (projet, disque) par ✕, remis par « Annuler », puis la piste comme avant", "effet_retrait:${t.name}"))
            }
            for (t in types.filter { it.params.isNotEmpty() }) for (end in listOf("min", "max")) {
                add(Scenario("effet_reglage_${nom(t)}_$end", "Pro : ${nom(t).replace('_', ' ')}, son premier réglage au ${if (end == "min") "minimum" else "maximum"}", NIVEAU_PRO, 120_000, 20, "la valeur du réglage (projet, disque), le son sans NaN, puis « Annuler »", "effet_reglage:${t.name}:$end"))
            }
            for ((name, _, _) in EssaiEclair.SCREENS) for ((how, what) in QUOI_ONGLET) {
                add(Scenario("onglet_${name}_$how", "Moyen : l'écran $name, $what", NIVEAU_MOYEN, 150_000, if (how == "relance") 40 else 20, "le même écran revient${if (how == "relance") ", le même morceau" else ""}", "onglet:$name:$how"))
            }
            for ((tag, delta) in GESTES_PARTIE) {
                add(Scenario("partie_redo_${tag.removePrefix("part_").substringBefore('_')}", "Moyen : « ${tag.removePrefix("part_").substringBefore('_')} », annuler puis rétablir", NIVEAU_MOYEN, 90_000, 15, "les parties après « Rétablir » (projet, disque), puis comme avant", "partie_redo:$tag:$delta"))
            }
            add(Scenario("limite_tempo_min", "Pro : le tempo au plus bas ($TEMPO_MIN bpm) tapé au pavé", NIVEAU_PRO, 120_000, 20, "$TEMPO_MIN bpm (projet, disque), « − » et ${TEMPO_MIN - 1} refusés, la lecture avec du son", "limite:min"))
            add(Scenario("limite_tempo_max", "Pro : le tempo au plus haut ($TEMPO_MAX bpm) tapé au pavé", NIVEAU_PRO, 120_000, 20, "$TEMPO_MAX bpm (projet, disque), « + » et ${TEMPO_MAX + 1} refusés, la lecture avec du son", "limite:max"))
        }

        /** [LISTE] as steps without work (to read a logbook). */
        /** The screens a scenario starts from: the studio (its bar, or its rail lying down), the welcome, the projects. */
        /** « Insérer après la partie N »'s choices. */
        private val PICK = Regex("pick_scene_\\d+")
        private val PRET = listOf(*EssaiEclair.STUDIO, "launch", "projects", "welcome")
        private const val ESSAIS_REMISE = 4
        private const val INDICE = "indice"
        private const val SON = "son"
        const val SON_COUPE = "coupé"
        const val SON_ALLUME = "allumé"

        private fun noms(of: List<Scenario>) = of.map { s -> Etape(s.id, s.label, s.maxMs) { "" } }

        /** A scenario the app died in, as the report says it, and when neither its trace nor Android's exit record could be read. */
        const val PLANTAGE = "plantage de l'appli"
        const val PLANTAGE_SANS_CAUSE = "cause non lue (ni trace ni rapport d'Android)"

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
            val pack = paquet(lines)
            // The « ratés » pack: the ids this logbook began (the list it was given is the robot's, not the logbook's).
            val list = if (pack == RATES) TOUS.filter { s -> lines.any { it.step == s.id && it.kind == Carnet.DEBUT } } else liste(pack)
            val feux = Rapport.feux(lines, noms(list))
            return list.map { s ->
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
                    nonMesure ?: if (end?.kind == Carnet.PLANTAGE) "$PLANTAGE : " + end.text.ifBlank { PLANTAGE_SANS_CAUSE } else listOfNotNull(end?.text?.takeIf { it.isNotBlank() }, why.joinToString(", ").takeIf { it.isNotEmpty() && end?.kind == Carnet.REUSSI && feu != Feu.VERT }).joinToString(" ; "),
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
            val keep = text.startsWith("bilan;") || (step.startsWith("export_") && text.startsWith("octets="))
            if (!keep) return emptyList()
            return text.removePrefix("bilan;").split(';').mapNotNull { f -> f.split('=', limit = 2).takeIf { it.size == 2 && it[1].isNotEmpty() }?.let { it[0] to it[1] } }
        }

        /** The latency's median (the step writes each measure as « ms=… »). */
        private fun mediane(step: String, mine: List<Carnet.Ligne>): List<Pair<String, String>> {
            if (step != LATENCE) return emptyList()
            val ms = mine.filter { it.kind == Carnet.MESURE }.mapNotNull { it.text.removePrefix("ms=").takeIf { v -> v != it.text }?.toDoubleOrNull() }.sorted()
            return if (ms.isEmpty()) emptyList() else listOf("latence_ms" to fmt(ms[ms.size / 2]), "mesures" to ms.size.toString())
        }

        /** More than [NON_MESURES_MAX] of the run not measured: the run says little, it fails (audit E). */
        fun tropDeNonMesures(r: List<Resultat>): Boolean = r.isNotEmpty() && r.count { it.resultat == NON_MESURE } > r.size * NON_MESURES_MAX
        const val NON_MESURES_MAX = 0.05

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
                if (tropDeNonMesures(r)) add("trop de non mesurés : passage à refaire")
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
            val ids = TOUS.map { it.id }.toSet()
            return lines.lastOrNull { it.step in ids && it.kind == Carnet.NOTE && it.text.startsWith("$SON;") }?.text?.split(';')?.getOrNull(1)
        }

        /** From the first scenario's beginning to the last line of one. */
        private fun duree(lines: List<Carnet.Ligne>): Long? {
            val ids = TOUS.map { it.id }.toSet()
            val mine = lines.filter { it.step in ids }
            val begin = mine.firstOrNull { it.kind == Carnet.DEBUT } ?: return null
            return (mine.last().atMs - begin.atMs) / 1000
        }

        /** Scenarios that open another song (a style, a guide, an export of a style) are not held to [commeAvant]. */
        fun rangeLeProjet(s: Scenario): Boolean = s.geste?.substringBefore(':') !in setOf("style", "style_tempo", "guide", "export") && s.id !in OUVRENT_UN_MORCEAU

        /** Ids that open, make or reopen a song by themselves (their work is their own). */
        val OUVRENT_UN_MORCEAU by lazy { setOf(DEBUTANT, PROJET, DUPLIQUER, SUPPRIMER, STYLE_TRANCE, STYLE_DUBSTEP, STYLE_DNB, GUIDE) }

        /** What differs between two [ProjetVu.Trace]s, by name (empty: the same song). */
        fun ecarts(a: ProjetVu.Trace, b: ProjetVu.Trace?): List<String> {
            if (b == null) return listOf("projet illisible")
            fun close(x: List<Float>, y: List<Float>) = x.size == y.size && x.zip(y).all { (u, v) -> abs(u - v) < 0.01f }
            return buildList {
                if (abs(a.tempo - b.tempo) >= 0.01) add("tempo ${fmt(a.tempo)} → ${fmt(b.tempo)}")
                if (a.parties != b.parties) add("parties ${a.parties} → ${b.parties}")
                if (a.montee != b.montee) add("montée ${a.montee} → ${b.montee} points")
                if (a.effets != b.effets) add("effets ${a.effets} → ${b.effets}")
                if (a.muets != b.muets) add("muets")
                if (a.solos != b.solos) add("solos")
                if (!close(a.volumes, b.volumes)) add("volumes")
                if (!close(a.pans, b.pans)) add("panoramiques")
                // Settings: only when both sides read them (an older reader leaves them empty).
                if (a.reglages.isNotEmpty() && b.reglages.isNotEmpty()) {
                    val same = a.reglages.size == b.reglages.size && a.reglages.zip(b.reglages).all { (x, y) -> x.size == y.size && x.zip(y).all { (u, v) -> close(u, v) } }
                    if (!same) add("réglages d'effets")
                }
            }
        }

        fun fmt(v: Float) = if (v.isFinite()) String.format(Locale.ROOT, "%.2f", v) else ""
        fun fmt(v: Double) = String.format(Locale.ROOT, "%.2f", v)
    }
}

/**
 * The open project as a scenario's oracle reads it: in memory (what the screens show) and read back
 * from its file ([disque], null without a file). [effets]: each strip's effects, by name.
 */
data class ProjetVu(
    val id: String, val nom: String, val parties: Int, val effets: List<List<String>>, val disque: Disque?,
    /** The tempo (bpm), and each strip muted or soloed. */
    val tempo: Double = 0.0, val muets: List<Boolean> = emptyList(), val solos: List<Boolean> = emptyList(),
    /** Each strip's volume and pan. */
    val volumes: List<Float> = emptyList(), val pans: List<Float> = emptyList(),
    /** The points of the song's build-up curve ([montee]): a « Montée » draws them, it adds no part. */
    val montee: Int = 0,
    /** The Live cells with something to play, as the grid tags them (`live_pad_<track>_<row>`), row by row. */
    val pads: List<String> = emptyList(),
    /** Each strip's effects' settings, effect by effect in slot order ([reglages]). */
    val reglages: List<List<List<Float>>> = emptyList(),
) {
    data class Disque(
        val nom: String, val parties: Int, val effets: List<List<String>>,
        val tempo: Double = 0.0, val muets: List<Boolean> = emptyList(), val solos: List<Boolean> = emptyList(),
        val volumes: List<Float> = emptyList(), val pans: List<Float> = emptyList(),
        val montee: Int = 0,
        val reglages: List<List<List<Float>>> = emptyList(),
    ) {
        fun trace() = Trace(parties, effets.map { it.sorted() }, tempo, muets, solos, volumes, pans, montee, reglages)
    }

    /** What [Scenarios.commeAvant] compares: the song's content, without its name. */
    data class Trace(
        val parties: Int, val effets: List<List<String>>, val tempo: Double, val muets: List<Boolean>, val solos: List<Boolean>,
        val volumes: List<Float>, val pans: List<Float>, val montee: Int,
        val reglages: List<List<List<Float>>> = emptyList(),
    )

    fun trace() = Trace(parties, effets.map { it.sorted() }, tempo, muets, solos, volumes, pans, montee, reglages)

    companion object {
        /** A strip's effects at most (the model's own limit). */
        const val EFFETS_MAX = com.pocketstudio.model.Layers.MAX_INSERTS

        /** [p]'s strips' effects' settings: per strip, per effect (NONE left out, slot order), its parameters. */
        fun reglages(p: com.pocketstudio.model.Project): List<List<List<Float>>> =
            p.mixer.channels.map { ch -> ch.inserts.filter { it.effect != EffectType.NONE }.map { it.params.toList() } }

        /** The points drawn on [p]'s song build-up curve (what `buildUpBefore` writes; 0: none). */
        fun montee(p: com.pocketstudio.model.Project): Int {
            val target = com.pocketstudio.model.AutoTarget.live(com.pocketstudio.model.AutoTarget.LIVE_BUILD_UP)
            return p.automation.firstOrNull { it.target == target }?.songValues?.count { it != null } ?: 0
        }

        /** [p]'s Live cells that have something to play, by the grid's tag, row by row then track by track. */
        fun pads(p: com.pocketstudio.model.Project): List<String> = with(com.pocketstudio.model.LiveSlots) {
            p.allScenes.indices.flatMap { scene ->
                p.liveColumns.filter { of(it) != null && p.cellHasContent(it, scene) }.map { "live_pad_${it}_$scene" }
            }
        }
    }
}
