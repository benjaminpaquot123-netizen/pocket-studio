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
    /** How long each style's new song is listened to, then the long playback, then the music with the studio behind another screen. */
    private val styleEcouteMs: Long = 10_000,
    private val longueMs: Long = 120_000,
    private val fondMs: Long = 8_000,
    /** Types [text] in the field tagged [tag] ([InAppDriver.saisir]); false: no such field (or not on the phone). */
    private val saisir: (String, String) -> Boolean = { _, _ -> false },
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
    )

    /** The scenarios of [LISTE] without a work of their own (none: a test checks it). */
    internal fun sansTravail(): List<String> = LISTE.map { it.id }.filter { it !in travaux }

    fun etapes(): List<Etape> = LISTE.map { s ->
        etape(s.id, s.label, s.maxMs) { (travaux[s.id] ?: error("scénario sans travail : ${s.id}"))() }
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
        check(b.playing * 2 >= b.reads) { "la lecture s'est arrêtée toute seule : ${b.text()}" }
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
            down = attendre(5_000) { lu().tempo < after }
        } finally {
            fermerFeuilles()
        }
        check(up) { "le tempo ne change pas : ${fmt(before)} bpm après « + »" }
        check(d?.tempo == after) { "le tempo n'est pas enregistré : ${d?.tempo?.let(::fmt) ?: "aucun fichier"} bpm sur le disque (attendu : ${fmt(after)})" }
        check(down) { "« − » ne fait pas redescendre le tempo : ${fmt(lu().tempo)} bpm" }
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
        check(b.playing * 2 >= b.reads) { "la lecture s'arrête quand le tempo change : ${b.text()}" }
        check(!b.nonFinite) { "valeur invalide (NaN) dans le son" }
        return "tempo ${fmt(before)} puis ${fmt(before + PAS_TEMPO)} bpm en jouant (lecture par $by) : ${b.text()}"
    }

    /** A strip's sheet in the mixer. */
    private fun bandeau(ch: Int) {
        j.tap("tab_Mix")
        j.await("mixer")
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
        if (!j.exists("top_undo")) return nonMesure(ANNULER, "pas de bouton « Annuler » dans la barre (réglage de l'appli)")
        j.tap("top_undo")
        check(attendre(5_000) { lu().parties == before }) { "« Annuler » ne retire pas le break : ${lu().parties} parties (attendu : $before)" }
        check(runCatching { j.await("top_redo", 5_000) }.isSuccess) { "pas de « Rétablir » après « Annuler »" }
        j.tap("top_redo")
        check(attendre(5_000) { lu().parties == before + 1 }) { "« Rétablir » ne remet pas le break : ${lu().parties} parties (attendu : ${before + 1})" }
        val d = surDisque { it.parties == before + 1 }
        check(d?.parties == before + 1) { "après « Rétablir », ${d?.parties ?: "aucun fichier"} parties sur le disque (attendu : ${before + 1})" }
        bilan(ANNULER, "parties" to before + 1)
        return "break ajouté ($before puis ${before + 1} parties), annulé ($before), rétabli (${before + 1}), enregistré"
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

    /** The first strip with room for [n] effects more (null: none among the first ones). */
    private fun place(n: Int): Int? {
        val fx = lu().effets
        return (0 until minOf(fx.size, PISTES_MAX)).firstOrNull { fx[it].size + n <= ProjetVu.EFFETS_MAX }
    }

    /** Pro: three effects on one strip. Oracle: the three in the project and on the disk, and the song still sounds (no NaN). */
    fun effetsPlusieurs(): String {
        auStudio()
        val ch = place(TROIS.size) ?: return nonMesure(EFFETS_PLUSIEURS, "aucune des $PISTES_MAX premières pistes n'a ${TROIS.size} places libres")
        val want = (lu().effets[ch] + TROIS).sorted()
        ajouter(ch, TROIS)
        check(lu().effets[ch].sorted() == want) { "la piste ${ch + 1} a ${lu().effets[ch].joinToString(", ")} (attendu : ${want.joinToString(", ")})" }
        val d = surDisque { it.effets.getOrNull(ch).orEmpty().sorted() == want }
        check(d != null && d.effets.getOrNull(ch).orEmpty().sorted() == want) { "les trois effets ne sont pas enregistrés sur le disque (piste ${ch + 1})" }
        val by = e.lancerLecture()
        val b = t.ecouter(EFFETS_PLUSIEURS, effetEcouteMs)
        e.arreter()
        ecoute(EFFETS_PLUSIEURS, b)
        feuEcoute(EFFETS_PLUSIEURS, b)
        check(!b.nonFinite) { "valeur invalide (NaN) dans le son avec les trois effets" }
        check(b.secondes.any { it.peakDb > Ecoute.SILENCE_DB }) { "silence avec les trois effets : ${b.text()}" }
        return "${TROIS.joinToString(", ")} sur la piste ${ch + 1}, enregistrés ; lecture par $by : ${b.text()}"
    }

    /** Pro: an effect added, then « Annuler ». Oracle: the strip's effects back as they were, in the project and on the disk. */
    fun annulerEffet(): String {
        auStudio()
        val ch = place(1) ?: return nonMesure(ANNULER_EFFET, "aucune des $PISTES_MAX premières pistes n'a de place libre")
        val before = lu().effets[ch].sorted()
        ajouter(ch, listOf(EFFET_ANNULE))
        if (!j.exists("top_undo")) return nonMesure(ANNULER_EFFET, "pas de bouton « Annuler » dans la barre (réglage de l'appli)")
        j.tap("top_undo")
        check(attendre(5_000) { lu().effets[ch].sorted() == before }) { "« Annuler » ne retire pas l'effet : ${lu().effets[ch].joinToString(", ")} sur la piste ${ch + 1}" }
        val d = surDisque { it.effets.getOrNull(ch).orEmpty().sorted() == before }
        check(d != null && d.effets.getOrNull(ch).orEmpty().sorted() == before) { "l'effet annulé reste sur le disque (piste ${ch + 1})" }
        return "$EFFET_ANNULE ajouté à la piste ${ch + 1} puis annulé, le disque aussi"
    }

    /** Beginner: a pad of the Live touched. Oracle: the engine plays and sound comes out. */
    fun pads(): String {
        auStudio()
        e.arreter()
        j.tap("tab_Live")
        j.await("live_screen")
        val pad = j.tagsMatching(Regex("live_pad_.+")).firstOrNull()
        if (pad == null) {
            runCatching { j.tap("tab_Structure") }
            return nonMesure(PADS, "aucun pad à l'écran du Live pour ce morceau")
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
        bilan(PADS, "crete" to fmt(peak))
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
        check(heard.playing * 2 >= heard.reads) { "la musique s'arrête quand l'appli passe au fond : ${heard.text()}" }
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
        check(standing && b.playing * 2 >= b.reads) { "la musique s'arrête quand le téléphone se redresse : ${b.text()}" }
        return "couché puis debout en jouant (lecture par $by) : ${b.text()}"
    }

    /** One scenario of the run: its step's name, what the report calls it, whose use it is. */
    data class Scenario(
        val id: String, val label: String, val niveau: String, val maxMs: Long,
        /** Its usual length on a phone (the run's estimate, under 20 min in all), and the precise result its oracle reads. */
        val estimeS: Int = 0, val oracle: String = "",
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
data class ProjetVu(
    val id: String, val nom: String, val parties: Int, val effets: List<List<String>>, val disque: Disque?,
    /** The tempo (bpm), and each strip muted or soloed. */
    val tempo: Double = 0.0, val muets: List<Boolean> = emptyList(), val solos: List<Boolean> = emptyList(),
) {
    data class Disque(
        val nom: String, val parties: Int, val effets: List<List<String>>,
        val tempo: Double = 0.0, val muets: List<Boolean> = emptyList(), val solos: List<Boolean> = emptyList(),
    )

    companion object {
        /** A strip's effects at most (the model's own limit). */
        const val EFFETS_MAX = com.pocketstudio.model.Layers.MAX_INSERTS
    }
}
