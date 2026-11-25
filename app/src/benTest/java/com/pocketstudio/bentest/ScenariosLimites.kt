package com.pocketstudio.bentest

import com.pocketstudio.app.journey.Journey
import com.pocketstudio.model.ProjectCatalog
import com.pocketstudio.bentest.Scenarios.Scenario
import com.pocketstudio.bentest.ToutTester.Companion.fmt
import com.pocketstudio.model.Project
import com.pocketstudio.model.SongPart
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Tour 9 catalogue, « Styles, structure et limites » (ids `lim_…`): each style crossed with a tempo change while it plays
 * and with an export of its own, the structure's gestures crossed with « Annuler » then « Rétablir » (still, while playing,
 * several in a row), and the app's limits (the lowest and highest tempos typed on the keypad, tempos out of range, a very
 * long or accented project name, a song 16 parts longer). Every scenario starts from the open song and leaves it as it found
 * it; its oracle reads the project in memory, the project on the disk, or the sound.
 */
internal class ScenariosLimites(private val o: Outils) {
    /** What [Scenarios] lends: its driver and readers, and its own gestures ([sous]: one of its table lines, played as this step). */
    class Outils(
        val j: Journey,
        val appli: Appli,
        val carnet: Carnet,
        val t: ToutTester,
        val e: EssaiEclair,
        val sauvegardeMs: Long,
        val courtMs: Long,
        val saisir: (String, String) -> Boolean,
        val auStudio: () -> Unit,
        val fermerFeuilles: () -> Unit,
        val ouvrirTempo: () -> Unit,
        val versProjets: () -> Unit,
        val versStudio: () -> Unit,
        val menu: (String, List<String>) -> Unit,
        val sous: (String) -> String,
    )

    private val j get() = o.j

    /** The work of the scenario [step] for its gesture's words after « lim » ([a]). */
    fun travail(step: String, a: List<String>): String = when (a.getOrNull(0)) {
        "style_tempo" -> o.sous("style:${a[1]}") + " ; " + o.sous("tempo:haut_lecture")
        "refaire" -> refaire(step, a[1], a[2].toInt(), enJouant = a.getOrNull(3) == LECTURE)
        "pile" -> pile(step, a[1].toInt())
        "borne" -> when (a[1]) {
            "hors" -> horsBornes(step)
            else -> borne(step, haut = a[1] == "max", suite = a.getOrNull(2))
        }
        "nom" -> nom(step, if (a[1] == "long") NOM_LONG else NOM_ACCENTS)
        "enorme" -> enorme(step, a[1].toInt())
        else -> error("geste inconnu : lim:${a.joinToString(":")}")
    }

    // ---- structure × « Annuler » / « Rétablir » ----

    /** What [tag] changes: the build-up curve's points for a « Montée » ([delta] 0), else the parts. */
    private fun compte(p: ProjetVu, delta: Int) = if (delta == 0) p.montee else p.parties
    private fun compte(d: ProjetVu.Disque, delta: Int) = if (delta == 0) d.montee else d.parties
    private fun quoi(delta: Int) = if (delta == 0) "points de montée" else "parties"

    /** Structure › the part's « ⋯ » › [tag] (« Insérer » asks which part: the first one offered). False: no such item. */
    private fun geste(tag: String): Boolean {
        j.tap("tab_Structure")
        try {
            sansBandeau()
            j.openPartSheet(Scenarios.PARTIE)
            if (!attendre(2_000) { j.exists(tag) }) return false
            j.tap(tag)
            if (tag.startsWith("part_insert") && attendre(3_000) { j.tagsMatching(PICK).isNotEmpty() }) j.tap(j.tagsMatching(PICK).first())
            return true
        } finally {
            o.fermerFeuilles()
        }
    }

    /**
     * [tag], « Annuler », « Rétablir », each checked in the project and on the disk; then « Annuler » again so the song is
     * left as it was. [enJouant]: all of it while the song plays, and the sound heard after.
     */
    private fun refaire(step: String, tag: String, delta: Int, enJouant: Boolean): String {
        o.auStudio()
        val p0 = lu()
        val before = compte(p0, delta)
        val by = if (enJouant) o.e.lancerLecture() else null
        var undone = 0
        try {
            if (!geste(tag)) return nonMesure(step, "pas de « $tag » dans la feuille de la partie ${Scenarios.PARTIE + 1}")
            undone = 1
            check(attendre(5_000) { compte(lu(), delta) != before }) { "rien ne change après « $tag » : ${compte(lu(), delta)} ${quoi(delta)}" }
            val after = compte(lu(), delta)
            if (delta != 0) check(after == before + delta) { "$after parties après « $tag » (avant : $before, attendu : ${before + delta})" }
            disque(step, delta, after, "après « $tag »")
            if (!j.exists("top_undo")) return nonMesure(step, "pas de bouton « Annuler » dans la barre (réglage de l'appli)")
            j.tap("top_undo")
            undone = 0
            check(attendre(5_000) { compte(lu(), delta) == before }) { "« Annuler » ne défait pas « $tag » : ${compte(lu(), delta)} ${quoi(delta)} (attendu : $before)" }
            disque(step, delta, before, "après « Annuler »")
            val redo = retablir() ?: error("pas de « Rétablir » après « Annuler » (à l'écran : ${vu()})")
            j.tap(redo)
            undone = 1
            check(attendre(5_000) { compte(lu(), delta) == after }) { "« Rétablir » ne refait pas « $tag » : ${compte(lu(), delta)} ${quoi(delta)} (attendu : $after)" }
            disque(step, delta, after, "après « Rétablir »")
            val b = if (enJouant) o.t.ecouter(step, o.courtMs) else null
            j.tap("top_undo")
            undone = 0
            check(attendre(5_000) { compte(lu(), delta) == before }) { "le second « Annuler » ne remet pas le morceau : ${compte(lu(), delta)} ${quoi(delta)} (attendu : $before)" }
            check(lu().id == p0.id) { "un autre projet est ouvert après « Annuler »" }
            bilan(step, "avant" to before, "apres" to after, "quoi" to quoi(delta))
            if (b != null) {
                ecoute(step, b)
                feuEcoute(step, b)
                entendu(b, "pendant « Annuler » et « Rétablir »")
            }
            val sound = if (b != null) " ; lecture par $by : ${b.text()}" else ""
            return "« $tag » : $before puis $after ${quoi(delta)}, annulé ($before), rétabli ($after), enregistré à chaque fois, puis remis$sound"
        } finally {
            if (undone > 0) runCatching { j.tap("top_undo") }
            o.e.arreter()
        }
    }

    /**
     * Waits (up to 8 s) for « Fait : … Annuler » to go: on a narrow phone its « Annuler » lies over the « ⋯ » of a part
     * at the foot of the list, and a touch meant for the « ⋯ » would undo the gesture just done.
     */
    private fun sansBandeau() {
        if (!j.exists("done_bar")) return
        // A touch on the page lets it go (tour 9, F111): the list's own « Parties » switch, already on.
        if (j.exists("structure_parts")) runCatching { j.tap("structure_parts") }
        attendre(8_000) { !j.exists("done_bar") }
    }

    /** The « Rétablir » button on screen (the top bar's, or the thumb bar's on a narrow phone), waited for up to 5 s. */
    private fun retablir(): String? =
        RETABLIR.takeIf { attendre(5_000) { RETABLIR.any { j.exists(it) } } }?.firstOrNull { j.exists(it) }

    /** [n] breaks in a row, [n] « Annuler » one by one, [n] « Rétablir », then all undone: each step's parts, and the disk at both ends. */
    private fun pile(step: String, n: Int): String {
        o.auStudio()
        val before = lu().parties
        if (before + n > SongPart.MAX_PARTS) return nonMesure(step, "le morceau a déjà $before parties (au plus ${SongPart.MAX_PARTS})")
        var extra = 0
        try {
            j.tap("tab_Structure")
            repeat(n) { k ->
                try {
                    sansBandeau()
                    j.partIntent(Scenarios.PARTIE, "part_break_4")
                } finally {
                    o.fermerFeuilles()
                }
                check(attendre(5_000) { lu().parties == before + k + 1 }) { "break ${k + 1} : ${lu().parties} parties (attendu : ${before + k + 1} ; à l'écran : ${vu()})" }
                extra = k + 1
            }
            disque(step, 1, before + n, "après $n breaks")
            if (!j.exists("top_undo")) return nonMesure(step, "pas de bouton « Annuler » dans la barre (réglage de l'appli)")
            for (k in n - 1 downTo 0) {
                j.tap("top_undo")
                check(attendre(5_000) { lu().parties == before + k }) { "« Annuler » n° ${n - k} : ${lu().parties} parties (attendu : ${before + k})" }
                extra = k
            }
            disque(step, 1, before, "après $n « Annuler »")
            for (k in 1..n) {
                val redo = retablir() ?: error("plus de « Rétablir » après ${k - 1} rétablis sur $n (à l'écran : ${vu()})")
                j.tap(redo)
                check(attendre(5_000) { lu().parties == before + k }) { "« Rétablir » n° $k : ${lu().parties} parties (attendu : ${before + k})" }
                extra = k
            }
            disque(step, 1, before + n, "après $n « Rétablir »")
            while (extra > 0) {
                j.tap("top_undo")
                val want = before + extra - 1
                check(attendre(5_000) { lu().parties == want }) { "en remettant le morceau : ${lu().parties} parties (attendu : $want)" }
                extra--
            }
            disque(step, 1, before, "une fois remis")
            bilan(step, "parties_avant" to before, "gestes" to n)
            return "$n breaks ($before puis ${before + n} parties), $n « Annuler » un à un, $n « Rétablir », enregistré à chaque bout, puis remis ($before)"
        } finally {
            repeat(extra) { runCatching { j.tap("top_undo"); j.idle(300) } }
        }
    }

    /** The song [delta] says: on the disk within the app's saving time, [want] of them. */
    private fun disque(step: String, delta: Int, want: Int, quand: String) {
        val d = surDisque { compte(it, delta) == want }
        check(d?.let { compte(it, delta) } == want) { "$quand : ${d?.let { compte(it, delta) } ?: "aucun fichier"} ${quoi(delta)} sur le disque (attendu : $want)" }
        carnet(step, "disque;$quand;$want")
    }

    // ---- the tempo's limits ----

    /** [bpm] typed on the keypad (the figures erased first), then OK. */
    private fun taper(bpm: Int) {
        // The erase key stays on screen once the figures are gone: at most the keypad's four figures, never a loop.
        repeat(4) { if (j.exists("tempo_key_erase")) runCatching { j.tap("tempo_key_erase", 500) } }
        bpm.toString().forEach { j.tap("tempo_key_$it") }
        runCatching { j.tap("tempo_key_ok", 2_000) }
    }

    /** The tempo put back to [before]: « Annuler » (the whole sheet is one step), else typed again. */
    private fun remettre(before: Double) {
        o.fermerFeuilles()
        if (abs(lu().tempo - before) >= 0.01) {
            val undone = j.exists("top_undo") && run {
                j.tap("top_undo")
                attendre(3_000) { abs(lu().tempo - before) < 0.01 }
            }
            if (!undone) {
                o.ouvrirTempo()
                taper(before.roundToInt())
                o.fermerFeuilles()
            }
        }
        // Put back on the disk too, so the next scenario opens the song as it was.
        surDisque { abs(it.tempo - before) < 0.01 }
    }

    /**
     * The lowest (40) or highest (300) tempo typed on the keypad. Oracle: the project's tempo and the disk's at the limit;
     * then « − » or « + » at the limit leaves it there; the song plays with sound at that tempo ([suite] « lecture »:
     * already playing when it is typed), or its export ([suite] « export ») is read back. Then the tempo is put back.
     */
    private fun borne(step: String, haut: Boolean, suite: String?): String {
        o.auStudio()
        val before = lu().tempo
        val limit = if (haut) Project.MAX_TEMPO else Project.MIN_TEMPO
        val past = if (haut) "tempo_plus" else "tempo_minus"
        var by = if (suite == LECTURE) o.e.lancerLecture() else null
        try {
            o.ouvrirTempo()
            taper(limit.roundToInt())
            o.fermerFeuilles()
            check(attendre(5_000) { abs(lu().tempo - limit) < 0.01 }) { "le tempo tapé ${fmt(limit)} n'est pas pris : ${fmt(lu().tempo)} bpm (avant : ${fmt(before)})" }
            val d = surDisque { abs(it.tempo - limit) < 0.01 }
            check(d?.let { abs(it.tempo - limit) < 0.01 } == true) { "le tempo ${fmt(limit)} n'est pas enregistré : ${d?.tempo?.let(::fmt) ?: "aucun fichier"} sur le disque" }
            o.ouvrirTempo()
            runCatching { j.tap(past, 2_000) }
            j.idle(500)
            val held = lu().tempo
            o.fermerFeuilles()
            check(abs(held - limit) < 0.01) { "« ${if (haut) "+" else "−"} » à ${fmt(limit)} bpm passe la limite : ${fmt(held)} bpm" }
            val text = if (suite == "export") {
                o.t.export("wav", step)
            } else {
                if (by == null) by = o.e.lancerLecture()
                val b = o.t.ecouter(step, o.courtMs)
                ecoute(step, b)
                feuEcoute(step, b)
                entendu(b, "à ${fmt(limit)} bpm")
                "lecture par $by : ${b.text()}"
            }
            bilan(step, "tempo_avant" to fmt(before), "tempo_limite" to fmt(limit))
            return "tempo ${fmt(limit)} bpm tapé, enregistré, « ${if (haut) "+" else "−"} » ne le passe pas ; $text"
        } finally {
            o.e.arreter()
            runCatching { remettre(before) }
        }
    }

    /** 39, 301 and 999 typed: OK does not take them, « Entre 40 et 300 » is said, the tempo does not move. */
    private fun horsBornes(step: String): String {
        o.auStudio()
        val before = lu().tempo
        val said = ArrayList<String>()
        try {
            for (bpm in HORS_BORNES) {
                o.ouvrirTempo()
                taper(bpm)
                j.idle(500)
                val now = lu().tempo
                check(abs(now - before) < 0.01) { "$bpm bpm tapé (hors de 40 à 300) change le tempo : ${fmt(now)} bpm (avant : ${fmt(before)})" }
                if (j.exists("tempo_range")) said += "$bpm"
                o.fermerFeuilles()
            }
            j.idle(500)
            check(abs(lu().tempo - before) < 0.01) { "le tempo a bougé : ${fmt(lu().tempo)} bpm (avant : ${fmt(before)})" }
            check(said.size == HORS_BORNES.size) { "« Entre 40 et 300 » absent de la feuille pour ${HORS_BORNES.map { "$it" } - said.toSet()}" }
            val d = surDisque { abs(it.tempo - before) < 0.01 }
            check(d?.let { abs(it.tempo - before) < 0.01 } == true) { "sur le disque : ${d?.tempo?.let(::fmt) ?: "aucun fichier"} bpm (attendu : ${fmt(before)})" }
            bilan(step, "tempo" to fmt(before), "refuses" to HORS_BORNES.joinToString("/"))
            return "${HORS_BORNES.joinToString(", ")} bpm refusés, « Entre 40 et 300 » affiché, tempo resté à ${fmt(before)} bpm"
        } finally {
            runCatching { remettre(before) }
        }
    }

    // ---- names and size ----

    /**
     * The open song duplicated, the robot's copy renamed [typed], then deleted. Oracle: the copy's name on the disk is
     * [typed] as the app keeps it (at most [ProjectCatalog.MAX_NAME] characters, trimmed), the open song's name unchanged.
     */
    private fun nom(step: String, typed: String): String {
        o.auStudio()
        val p = lu()
        val before = appli().keys
        var copy: String? = null
        o.versProjets()
        try {
            o.menu(p.id, listOf("Dupliquer", "Duplicate"))
            attendre(o.sauvegardeMs) { (appli().keys - before).isNotEmpty() }
            val added = appli().keys - before
            check(added.size == 1) { if (added.isEmpty()) "aucune copie sur le disque après ${o.sauvegardeMs / 1000} s" else "${added.size} projets nouveaux au lieu d'un" }
            copy = added.first()
            check(copy != p.id) { "la copie a l'id du projet ouvert" }
            o.menu(copy, listOf("Renommer", "Rename"))
            j.await("project_name_field", 5_000)
            if (!o.saisir("project_name_field", typed)) return nonMesure(step, "le champ du nom ne se remplit pas d'ici")
            j.tap("project_rename_ok")
            val want = typed.trim().take(ProjectCatalog.MAX_NAME).trim()
            var got: String? = null
            check(attendre(o.sauvegardeMs) { got = appli()[copy]?.nom; got == want }) { "nom de la copie sur le disque : « ${got ?: "?"} » (${got?.length ?: 0} caractères ; attendu : « $want », ${want.length})" }
            check(want.length <= ProjectCatalog.MAX_NAME) { "nom de ${want.length} caractères" }
            check(appli()[p.id]?.nom == p.nom) { "le projet ouvert a changé de nom : « ${appli()[p.id]?.nom} » (avant : « ${p.nom} »)" }
            check(j.exists("projects")) { "la liste des projets n'est plus à l'écran après le renommage" }
            bilan(step, "tape" to typed.length, "garde" to want.length)
            return "copie renommée : ${typed.length} caractères tapés, « $want » (${want.length}) relu sur le disque ; projet ouvert intact"
        } finally {
            copy?.let { id -> runCatching { supprimerCopie(id, p.id) } }
            // Never over the step's own failure: a wrong name is what the step tells, not a later screen.
            runCatching { o.versStudio() }
        }
    }

    /** The robot's copy [id] deleted from « Mes projets » (never [open], never one of the user's). */
    private fun supprimerCopie(id: String, open: String) {
        if (id == open || appli()[id] == null) return
        if (!j.exists("projects")) o.versProjets()
        j.tap("project_menu_$id")
        j.tap("project_delete_$id")
        check(attendre(o.sauvegardeMs) { !appli().containsKey(id) }) { "la copie du robot est encore sur le disque" }
    }

    /** [n] duplications of a part: the song [n] parts longer in the project and on the disk, it plays with sound; then all undone. */
    private fun enorme(step: String, n: Int): String {
        o.auStudio()
        val before = lu().parties
        if (before + n > SongPart.MAX_PARTS) return nonMesure(step, "le morceau a déjà $before parties (au plus ${SongPart.MAX_PARTS})")
        var extra = 0
        try {
            repeat(n) { k ->
                if (!geste("part_duplicate_${Scenarios.PARTIE}")) return nonMesure(step, "pas de « Dupliquer » dans la feuille de la partie ${Scenarios.PARTIE + 1}")
                check(attendre(5_000) { lu().parties == before + k + 1 }) { "duplication ${k + 1} : ${lu().parties} parties (attendu : ${before + k + 1})" }
                extra = k + 1
            }
            disque(step, 1, before + n, "après $n duplications")
            val by = o.e.lancerLecture()
            val b = o.t.ecouter(step, o.courtMs)
            o.e.arreter()
            ecoute(step, b)
            feuEcoute(step, b)
            entendu(b, "avec ${before + n} parties")
            if (!j.exists("top_undo")) return nonMesure(step, "pas de bouton « Annuler » dans la barre (réglage de l'appli)")
            while (extra > 0) {
                j.tap("top_undo")
                val want = before + extra - 1
                check(attendre(5_000) { lu().parties == want }) { "« Annuler » : ${lu().parties} parties (attendu : $want)" }
                extra--
            }
            disque(step, 1, before, "après $n « Annuler »")
            bilan(step, "parties_avant" to before, "parties_max" to before + n)
            return "$before puis ${before + n} parties, enregistré, lecture par $by : ${b.text()} ; $n « Annuler », $before parties"
        } finally {
            o.e.arreter()
            repeat(extra) { runCatching { j.tap("top_undo"); j.idle(300) } }
        }
    }

    // ---- readers ----

    private fun appli(): Map<String, ProjetVu.Disque> = o.appli.projets() ?: error("la liste des projets ne se lit pas ici")

    private fun lu(): ProjetVu = o.appli.projet() ?: error("le projet ouvert ne se lit pas")

    private fun surDisque(ok: (ProjetVu.Disque) -> Boolean): ProjetVu.Disque? {
        var d: ProjetVu.Disque? = null
        attendre(o.sauvegardeMs) { d = o.appli.projet()?.disque; d?.let(ok) == true }
        return d
    }

    private fun attendre(ms: Long, ok: () -> Boolean): Boolean {
        val end = System.currentTimeMillis() + ms
        while (true) {
            if (ok()) return true
            if (System.currentTimeMillis() >= end) return false
            j.idle(250)
        }
    }

    /** The song kept playing, with sound, without NaN, [quand]. */
    private fun entendu(b: Ecoute.Bilan, quand: String) {
        check(b.playing * 2 >= b.reads) { "la lecture s'arrête $quand : ${b.text()}" }
        check(!b.nonFinite) { "valeur invalide (NaN) dans le son $quand" }
        check(b.secondes.any { it.peakDb > Ecoute.SILENCE_DB }) { "silence $quand : ${b.text()}" }
    }

    /** The bar's and the parts' buttons on screen, for a failure's words. */
    private fun vu(): String = runCatching { j.tagsMatching(VU).joinToString(", ") }.getOrDefault("?")

    private fun carnet(step: String, text: String) = o.carnet.add(Carnet.NOTE, step, text)

    private fun feu(step: String, f: Pair<Feu, String>) = o.carnet.add(Carnet.FEU, step, f.first.mot + ";" + f.second)

    private fun feuEcoute(step: String, b: Ecoute.Bilan) {
        feu(step, Feux.style(b))
        Feux.depasses(b).forEach { o.carnet.add(Carnet.NOTE, step, "${ToutTester.SEUIL};$it") }
    }

    private fun bilan(step: String, vararg fields: Pair<String, Any?>) =
        o.carnet.add(Carnet.MESURE, step, "bilan;" + fields.joinToString(";") { (k, v) -> "$k=${v ?: ""}" })

    private fun ecoute(step: String, b: Ecoute.Bilan) = bilan(
        step, "decrochages" to b.xruns, "pire_bloc" to fmt(b.cpuWorst), "plus_long_ecart" to fmt(b.gapWorst),
        "charge_moyenne" to fmt(b.cpuMean), "crete" to fmt(b.peakMax), "secondes" to b.reads / Ecoute.READS_PER_SECOND, "alertes" to b.alertes.size,
    )

    private fun nonMesure(step: String, why: String): String {
        feu(step, Feu.GRIS to why)
        o.carnet.add(Carnet.NOTE, step, "${ToutTester.NON_MESURE};$why")
        return "non mesuré : $why"
    }

    companion object {
        const val PREFIXE = "lim_"
        private const val LECTURE = "lecture"
        private val RETABLIR = listOf("top_redo", "thumb_redo")
        private val PICK = Regex("pick_scene_\\d+")
        private val VU = Regex("(top|thumb)_.*|part_(menu|break|sheet|play).*|structure_.*|timeline_screen|.*_sheet")

        /** Tempos out of the app's range, typed on the keypad (999: three figures, the most it takes). */
        val HORS_BORNES = listOf(39, 301, 999)

        /** 121 characters without a space (none falls at the cut), then a name with accents, signs and an emoji. */
        val NOM_LONG = "RobotLimite" + "0123456789".repeat(11)
        const val NOM_ACCENTS = "Robot éèàçù ñ ß « 🎵 » limite"

        /** The styles crossed (the full pack's new ones, then the quick pack's three), and the export formats in turn. */
        val STYLES: List<String> get() = Scenarios.STYLES_NEUFS + listOf("trance", "dubstep", "dnb")
        /** Styles the full run already exports (its own exports, and the « save » theme's): not exported again here. */
        val DEJA_EXPORTES: Set<String> get() = (Scenarios.EXPORTS_STYLES + ScenariosSauve.EXPORTS).map { it.first }.toSet()
        private val FORMATS = listOf("m4a", "wav", "flac")

        /** Structure's gestures crossed with « Annuler » / « Rétablir »: tag, the parts it adds (0: a build-up curve), its name. */
        val GESTES = listOf(
            Triple("part_insert_${Scenarios.PARTIE}", 1, "insérer une partie"),
            Triple("part_remove_${Scenarios.PARTIE}", -1, "retirer une partie"),
            Triple("part_duplicate_${Scenarios.PARTIE}", 1, "dupliquer une partie"),
            Triple("part_ending", 1, "ajouter une fin"),
            Triple("part_break_4", 1, "ajouter un break"),
            Triple("part_buildup_4", 0, "une montée"),
        )
        private val GESTES_LECTURE = setOf("part_insert_${Scenarios.PARTIE}", "part_remove_${Scenarios.PARTIE}", "part_break_4")

        private fun court(tag: String) = tag.removePrefix("part_").substringBefore('_')
        private fun nomStyle(st: String) = st.replace('_', ' ')
        private fun niveau(st: String, sinon: String) = if (st in ToutTester.LOURDS) Scenarios.NIVEAU_MESURE else sinon

        /**
         * The catalogue's scenarios of this theme. Lazy, and nothing above reads [Scenarios]' companion when this class loads:
         * [Scenarios.LISTE] is built from this list, so whichever of the two is touched first, neither sees the other half made.
         */
        val LISTE: List<Scenario> by lazy {
            buildList {
                STYLES.filter { it !in DEJA_EXPORTES }.forEachIndexed { i, st ->
                    val f = FORMATS[i % FORMATS.size]
                    add(Scenario("${PREFIXE}export_${st}_$f", "Moyen : un morceau ${nomStyle(st)} exporté en ${f.uppercase(Locale.ROOT)}", niveau(st, Scenarios.NIVEAU_MOYEN), 600_000, 75, "le fichier relu : durée, crête, temps pris", "export:$st:$f"))
                }
                for (st in STYLES) {
                    add(Scenario("${PREFIXE}tempo_$st", "Pro : un morceau ${nomStyle(st)}, puis son tempo doublé en jouant", niveau(st, Scenarios.NIVEAU_PRO), 480_000, 55, "le son du style ; le tempo monté, sur le disque, la lecture sans arrêt", "lim:style_tempo:$st"))
                }
                for ((tag, delta, name) in GESTES) {
                    add(Scenario("${PREFIXE}refaire_${court(tag)}", "Moyen : $name, annuler, rétablir", Scenarios.NIVEAU_MOYEN, 120_000, 20, "les ${if (delta == 0) "points de montée" else "parties"} après chaque geste, dans le projet et sur le disque", "lim:refaire:$tag:$delta"))
                }
                for ((tag, delta, name) in GESTES.filter { it.first in GESTES_LECTURE }) {
                    add(Scenario("${PREFIXE}refaire_lecture_${court(tag)}", "Pro : $name, annuler, rétablir, en jouant", Scenarios.NIVEAU_PRO, 150_000, 30, "les parties après chaque geste (projet, disque) ; la lecture sans arrêt, avec du son", "lim:refaire:$tag:$delta:$LECTURE"))
                }
                for (n in listOf(3, 5, 10)) {
                    add(Scenario("${PREFIXE}annuler_pile_$n", "Pro : $n breaks, $n fois annuler, $n fois rétablir", Scenarios.NIVEAU_PRO, 120_000L + n * 30_000L, 15 + n * 6, "les parties après chaque « Annuler » et « Rétablir », le disque aux deux bouts", "lim:pile:$n"))
                }
                add(Scenario("${PREFIXE}tempo_min", "Pro : le tempo le plus lent (40), tapé", Scenarios.NIVEAU_PRO, 120_000, 20, "40 bpm dans le projet et sur le disque, « − » ne le passe pas, du son", "lim:borne:min"))
                add(Scenario("${PREFIXE}tempo_max", "Pro : le tempo le plus rapide (300), tapé", Scenarios.NIVEAU_PRO, 120_000, 20, "300 bpm dans le projet et sur le disque, « + » ne le passe pas, du son", "lim:borne:max"))
                add(Scenario("${PREFIXE}tempo_min_lecture", "Pro : 40 bpm tapé en jouant", Scenarios.NIVEAU_PRO, 120_000, 20, "40 bpm (projet, disque) ; la lecture sans arrêt, avec du son", "lim:borne:min:$LECTURE"))
                add(Scenario("${PREFIXE}tempo_max_lecture", "Pro : 300 bpm tapé en jouant", Scenarios.NIVEAU_PRO, 120_000, 20, "300 bpm (projet, disque) ; la lecture sans arrêt, avec du son", "lim:borne:max:$LECTURE"))
                add(Scenario("${PREFIXE}tempo_min_export", "Pro : exporter le morceau à 40 bpm", Scenarios.NIVEAU_PRO, 600_000, 90, "40 bpm (projet, disque) ; le fichier relu : durée, crête", "lim:borne:min:export"))
                add(Scenario("${PREFIXE}tempo_max_export", "Pro : exporter le morceau à 300 bpm", Scenarios.NIVEAU_PRO, 600_000, 60, "300 bpm (projet, disque) ; le fichier relu : durée, crête", "lim:borne:max:export"))
                add(Scenario("${PREFIXE}tempo_hors_bornes", "Moyen : taper 39, 301 et 999 bpm", Scenarios.NIVEAU_MOYEN, 120_000, 25, "OK sans effet, « Entre 40 et 300 » affiché, tempo inchangé (projet, disque)", "lim:borne:hors"))
                add(Scenario("${PREFIXE}nom_long", "Moyen : un nom de projet de 121 caractères", Scenarios.NIVEAU_MOYEN, 120_000, 25, "le nom gardé sur le disque (40 caractères au plus), le projet ouvert intact", "lim:nom:long"))
                add(Scenario("${PREFIXE}nom_accents", "Moyen : un nom avec accents, signes et emoji", Scenarios.NIVEAU_MOYEN, 120_000, 25, "le nom relu à l'identique sur le disque", "lim:nom:accents"))
                add(Scenario("${PREFIXE}projet_enorme", "Mesure : 16 parties de plus, joué, puis annulé", Scenarios.NIVEAU_MESURE, 300_000, 70, "16 parties de plus (projet, disque), le son, puis comme avant", "lim:enorme:16"))
            }
        }
    }
}
