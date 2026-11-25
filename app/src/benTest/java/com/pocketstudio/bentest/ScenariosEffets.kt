package com.pocketstudio.bentest

import com.pocketstudio.app.journey.Journey
import com.pocketstudio.model.EffectType
import java.io.File

/**
 * The full pack's effect scenarios (tour 9, catalogue « Effets »): each effect crossed with its settings at both ends,
 * its removal while the song plays (then « Annuler », « Rétablir »), and the heaviest ones in pairs and in fours on one
 * strip while the song plays, dropouts counted. Each scenario starts from the studio with the strips as the one before
 * left them, finds a strip with room, and leaves it as it found it.
 *
 * A table line (`fx:<kind>:<TYPE>[:<TYPE>…]`, [Scenarios.Scenario.geste]) names the work; [Scenarios] plays it here.
 */
class ScenariosEffets(
    private val j: Journey,
    sonde: Sonde,
    private val appli: Appli,
    private val carnet: Carnet,
    captures: File,
    ecran: () -> List<String>,
    ecouteMs: Long,
    /** How long the app may take to write a change on the disk. */
    private val sauvegardeMs: Long,
    /** The short listening (each end of the settings, the removal). */
    private val courtMs: Long,
    /** The listening of the heavy pairs and fours (dropouts). */
    private val effetEcouteMs: Long,
    /** Sets the slider tagged [tag] to a value ([InAppDriver.regler]); false: no such slider. */
    private val regler: (String, Float) -> Boolean,
    /** Scrolls the screen's lists back to their top ([InAppDriver.enHaut]). */
    private val enHaut: () -> Unit,
) {
    private val e = EssaiEclair(j, sonde, appli, carnet, captures, ecouteMs, ecran)
    private val t = ToutTester(j, sonde, appli, carnet, captures, ecouteMs, ecran)

    /** The work [g] (the table line split on « : ») names, for the scenario [id]. */
    fun jouer(id: String, g: List<String>): String = when (g[1]) {
        BOUTS -> bouts(id, g[2])
        RETRAIT -> retrait(id, g[2])
        else -> lourds(id, g.drop(2))
    }

    // --- The scenarios -------------------------------------------------------------------------------

    /**
     * Pro: one effect, every knob at 0 then at 1, the song played at each end. Oracle: each knob's slider accepts both
     * ends (the value itself is not read back), the effect stays on the strip (project and disk), the song keeps playing with sound and no NaN at both ends. Then
     * « Retirer l'effet »: the strip as it was, in its order, on the disk too.
     */
    private fun bouts(step: String, type: String): String {
        auStudio()
        val et = typeDe(type) ?: return nonMesure(step, "l'appli n'a pas d'effet $type")
        manque(listOf(et))?.let { return nonMesure(step, it) }
        val ch = place(1) ?: return nonMesure(step, "aucune des ${Scenarios.PISTES_MAX} premières pistes n'a de place libre")
        val before = lu().effets[ch]
        val slot = ajouterOuvert(step, ch, type)
        var range = false
        try {
        val withIt = lu().effets[ch]
        val sons = ArrayList<Ecoute.Bilan>()
        var nonRegles = 0
        try {
            val d = surDisque { it.effets.getOrNull(ch) == withIt }
            check(d?.effets?.getOrNull(ch) == withIt) { "$type n'est pas enregistré sur le disque (piste ${ch + 1})" }
            for (v in BOUTS_VALEURS) {
                bandeau(ch)
                if (!j.exists("fx_editor_${ch}_$slot")) j.tap("fx_chip_${ch}_$slot")
                check(attendre(5_000) { j.exists("fx_editor_${ch}_$slot") }) { "$type touché, ses réglages ne s'ouvrent pas (piste ${ch + 1})" }
                val rates = et.params.indices.filterNot { k -> regler("fxp_${ch}_${slot}_$k", v) }
                if (rates.size == et.params.size && et.params.isNotEmpty()) nonRegles++
                else check(rates.isEmpty()) {
                    "$type : ${rates.joinToString(", ") { et.params[it].label }} ne se règle pas à ${Scenarios.fmt(v)} (piste ${ch + 1})"
                }
                fermerFeuilles()
                check(lu().effets.getOrNull(ch) == withIt) { "$type réglé à ${Scenarios.fmt(v)}, la piste ${ch + 1} a ${lu().effets.getOrNull(ch)} (attendu : $withIt)" }
                if (nonRegles > 0) break
                // A knob that silences the strip already fails at the start of play: said as this scenario's silence.
                try {
                    e.lancerLecture()
                } catch (x: IllegalStateException) {
                    if (x.message.orEmpty().contains("aucun son")) error("silence avec $type réglé à ${Scenarios.fmt(v)} : ${x.message}") else throw x
                }
                val b = try { t.ecouter(step, courtMs) } finally { e.arreter() }
                ecoute(step, b)
                sons += b
                check(!b.nonFinite) { "valeur invalide (NaN) dans le son avec $type réglé à ${Scenarios.fmt(v)}" }
                check(b.secondes.any { it.peakDb > Ecoute.SILENCE_DB }) { "silence avec $type réglé à ${Scenarios.fmt(v)} : ${b.text()}" }
                check(b.playing * 2 >= b.reads) { "la lecture s'arrête avec $type réglé à ${Scenarios.fmt(v)} : ${b.text()}" }
            }
        } finally {
            fermerFeuilles()
        }
        sons.maxByOrNull { it.xruns }?.let { feuEcoute(step, it) }
        val gone = retirerParCroix(ch, slot, before)
        range = true
        check(gone) { "« Retirer l'effet » touché, la piste ${ch + 1} a ${lu().effets.getOrNull(ch)} (attendu : $before)" }
        check(surDisque { it.effets.getOrNull(ch) == before }?.effets?.getOrNull(ch) == before) { "$type retiré, la piste ${ch + 1} n'est pas enregistrée sans lui sur le disque" }
        if (nonRegles > 0) return nonMesure(step, "aucun réglage de $type ne se règle par le robot (pas de curseur accessible)")
        bilan(step, "piste" to ch + 1, "reglages" to et.params.size)
        return "$type sur la piste ${ch + 1} : ${et.params.size} curseurs poussés à 0 puis à 1 et acceptés, joué aux deux bouts (${sons.joinToString(" ; ") { it.text() }}), retiré"
        } finally {
            if (!range) remettre(ch, slot, before)
        }
    }

    /** After a failure: the effect in [slot] taken off when strip [ch] is not [before], so the next scenario starts clean. */
    private fun remettre(ch: Int, slot: Int, before: List<String>) {
        runCatching {
            fermerFeuilles()
            if (lu().effets.getOrNull(ch) != before) retirerParCroix(ch, slot, before)
        }
    }

    /**
     * Pro: one effect taken off by its « × » while the song plays, then « Annuler » and « Rétablir ». Oracle: the song
     * keeps playing with sound and no NaN; the strip as it was (project and disk); « Annuler » brings the effect back
     * in its place, « Rétablir » takes it off again, each one on the disk.
     */
    private fun retrait(step: String, type: String): String {
        auStudio()
        val et = typeDe(type) ?: return nonMesure(step, "l'appli n'a pas d'effet $type")
        manque(listOf(et))?.let { return nonMesure(step, it) }
        val ch = place(1) ?: return nonMesure(step, "aucune des ${Scenarios.PISTES_MAX} premières pistes n'a de place libre")
        val before = lu().effets[ch]
        val slot = ajouterOuvert(step, ch, type)
        try {
        fermerFeuilles()
        val withIt = lu().effets[ch]
        val d = surDisque { it.effets.getOrNull(ch) == withIt }
        check(d?.effets?.getOrNull(ch) == withIt) { "$type n'est pas enregistré sur le disque (piste ${ch + 1})" }
        val by = e.lancerLecture()
        var gone = false
        val b = try {
            bandeau(ch)
            j.tap("fx_remove_${ch}_$slot")
            gone = attendre(5_000) { lu().effets.getOrNull(ch) == before }
            fermerFeuilles()
            t.ecouter(step, courtMs)
        } finally {
            fermerFeuilles()
            e.arreter()
        }
        ecoute(step, b)
        feuEcoute(step, b)
        check(gone) { "« Retirer l'effet » touché en jouant, la piste ${ch + 1} a ${lu().effets.getOrNull(ch)} (attendu : $before)" }
        check(!b.nonFinite) { "valeur invalide (NaN) dans le son après le retrait de $type" }
        check(b.playing * 2 >= b.reads) { "la lecture s'arrête quand $type est retiré : ${b.text()}" }
        check(b.secondes.any { it.peakDb > Ecoute.SILENCE_DB }) { "silence après le retrait de $type : ${b.text()}" }
        check(surDisque { it.effets.getOrNull(ch) == before }?.effets?.getOrNull(ch) == before) { "$type retiré, la piste ${ch + 1} n'est pas enregistrée sans lui sur le disque" }
        val undo = premier(ANNULER) ?: return nonMesure(step, "pas de bouton « Annuler » à l'écran (réglage de l'appli)")
        j.tap(undo)
        check(attendre(5_000) { lu().effets.getOrNull(ch) == withIt }) { "« Annuler » ne remet pas $type : la piste ${ch + 1} a ${lu().effets.getOrNull(ch)} (attendu : $withIt)" }
        check(surDisque { it.effets.getOrNull(ch) == withIt }?.effets?.getOrNull(ch) == withIt) { "après « Annuler », $type n'est pas sur le disque (piste ${ch + 1})" }
        val redo = attendre(5_000) { premier(RETABLIR) != null }.let { premier(RETABLIR) }
            ?: return nonMesure(step, "pas de bouton « Rétablir » à l'écran après « Annuler » (réglage de l'appli)")
        j.tap(redo)
        check(attendre(5_000) { lu().effets.getOrNull(ch) == before }) { "« Rétablir » ne retire pas $type : la piste ${ch + 1} a ${lu().effets.getOrNull(ch)} (attendu : $before)" }
        check(surDisque { it.effets.getOrNull(ch) == before }?.effets?.getOrNull(ch) == before) { "après « Rétablir », la piste ${ch + 1} n'est pas enregistrée sans $type sur le disque" }
        bilan(step, "piste" to ch + 1)
        return "$type retiré en jouant (lecture par $by : ${b.text()}), remis par « Annuler », retiré par « Rétablir », enregistré à chaque fois"
        } finally {
            remettre(ch, slot, before)
        }
    }

    /**
     * Measure: the heaviest effects together on one strip while the song plays. Oracle: all of them in the project and
     * on the disk, no dropout, no NaN, sound, the song keeps playing. Then « Annuler » takes them off.
     */
    private fun lourds(step: String, types: List<String>): String {
        auStudio()
        val ets = types.map { typeDe(it) ?: return nonMesure(step, "l'appli n'a pas d'effet $it") }
        manque(ets)?.let { return nonMesure(step, it) }
        val ch = place(types.size) ?: return nonMesure(step, "aucune des ${Scenarios.PISTES_MAX} premières pistes n'a ${types.size} places libres")
        val before = lu().effets[ch].sorted()
        val want = (before + types).sorted()
        val nom = types.joinToString(" + ")
        ajouter(ch, types)
        check(lu().effets[ch].sorted() == want) { "la piste ${ch + 1} a ${lu().effets[ch].joinToString(", ")} (attendu : ${want.joinToString(", ")})" }
        val d = surDisque { it.effets.getOrNull(ch).orEmpty().sorted() == want }
        check(d != null && d.effets.getOrNull(ch).orEmpty().sorted() == want) { "$nom ne sont pas enregistrés sur le disque (piste ${ch + 1})" }
        val by = e.lancerLecture()
        val b = try { t.ecouter(step, effetEcouteMs) } finally { e.arreter() }
        ecoute(step, b)
        feuEcoute(step, b)
        val undone = retirer(ch, before, types.size)
        check(!b.nonFinite) { "valeur invalide (NaN) dans le son avec $nom" }
        check(b.secondes.any { it.peakDb > Ecoute.SILENCE_DB }) { "silence avec $nom : ${b.text()}" }
        check(b.playing * 2 >= b.reads) { "la lecture s'arrête avec $nom : ${b.text()}" }
        check(b.xruns == 0) { "${b.xruns} décrochages avec $nom sur une piste : ${b.text()}" }
        if (undone == null) return nonMesure(step, Scenarios.LAISSE)
        check(undone) { "les effets restent sur la piste ${ch + 1} : « Annuler » ne les a pas retirés (${lu().effets[ch].joinToString(", ")})" }
        return "$nom sur la piste ${ch + 1}, enregistrés ; lecture par $by sans décrochage : ${b.text()} ; retirés par « Annuler »"
    }

    // --- The gestures --------------------------------------------------------------------------------

    private fun typeDe(name: String) = EffectType.entries.firstOrNull { it.name == name && it != EffectType.NONE }

    /** Why the song cannot take [ets] more (the app's limit on long effects); null: it can. */
    private fun manque(ets: List<EffectType>): String? {
        val present = lu().effets.flatten().mapNotNull { name -> EffectType.entries.firstOrNull { it.name == name } }
        for (et in ets.filter { it.long }) {
            val n = present.count { EffectType.samePool(it, et) } + ets.count { EffectType.samePool(it, et) }
            if (n > EffectType.longLimit(et)) return "pas de place pour ${et.name} : l'appli permet ${EffectType.longLimit(et)} effets de ce genre par morceau"
        }
        return null
    }

    /** [type] added to strip [ch] by its sheet's list, the sheet left open; its slot (the chip that appeared). */
    private fun ajouterOuvert(step: String, ch: Int, type: String): Int {
        bandeau(ch)
        val chips = Regex("fx_chip_${ch}_\\d+")
        val avant = j.tagsMatching(chips).toSet()
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
        attendre(3_000) { (j.tagsMatching(chips) - avant).isNotEmpty() }
        val neuf = (j.tagsMatching(chips) - avant).singleOrNull()
            ?: run { fermerFeuilles(); error("$type ajouté à la piste ${ch + 1}, la fiche ne le montre pas") }
        carnet.add(Carnet.NOTE, step, "$type à la place ${neuf.substringAfterLast('_')} de la piste ${ch + 1}")
        return neuf.substringAfterLast('_').toInt()
    }

    /** The effect in [slot] taken off by its « × »; true when strip [ch] is [before] again in the project. */
    private fun retirerParCroix(ch: Int, slot: Int, before: List<String>): Boolean = try {
        bandeau(ch)
        j.tap("fx_remove_${ch}_$slot")
        attendre(5_000) { lu().effets.getOrNull(ch) == before }
    } finally {
        fermerFeuilles()
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

    /** « Annuler » up to [fois] times until strip [ch] has [was] again (project and disk); null: no « Annuler ». */
    private fun retirer(ch: Int, was: List<String>, fois: Int): Boolean? {
        fun clean(fx: List<List<String>>) = fx.getOrNull(ch).orEmpty().sorted() == was
        repeat(fois) {
            if (clean(lu().effets)) return@repeat
            val undo = premier(ANNULER) ?: return null
            val n = lu().effets.getOrNull(ch).orEmpty().size
            j.tap(undo)
            attendre(5_000) { lu().effets.getOrNull(ch).orEmpty().size < n }
        }
        return attendre(5_000) { clean(lu().effets) } && surDisque { clean(it.effets) }?.let { clean(it.effets) } == true
    }

    /** The first of [tags] on the screen (the bar's button or the thumb bar's one, whichever the layout shows). */
    private fun premier(tags: List<String>): String? = tags.firstOrNull { j.exists(it) }

    /** The first strip with room for [n] effects more (null: none among the first ones). */
    private fun place(n: Int): Int? {
        val fx = lu().effets
        return (0 until minOf(fx.size, Scenarios.PISTES_MAX)).firstOrNull { fx[it].size + n <= ProjetVu.EFFETS_MAX }
    }

    private fun bandeau(ch: Int) {
        if (j.exists("strip_sheet")) return
        j.tap("tab_Mix")
        j.await("mixer")
        repeat(3) {
            runCatching { enHaut() }
            j.idle(300)
            runCatching { j.tap("strip_more_$ch") }
            if (attendre(3_000) { j.exists("strip_sheet") }) return
        }
        j.tap("strip_more_$ch")
        j.await("strip_sheet")
    }

    private fun fermerFeuilles() {
        if (j.tagsMatching(PICK).isNotEmpty()) runCatching { j.tap("texte:Annuler"); attendre(3_000) { j.tagsMatching(PICK).isEmpty() } }
        for (sheet in listOf("part_sheet", "strip_sheet")) {
            if (j.exists(sheet)) runCatching { j.tap("${sheet}_close"); j.awaitGone(sheet, 5_000) }
        }
        if (j.exists("tempo_sheet")) runCatching { j.tap("tempo_close"); j.awaitGone("tempo_sheet", 5_000) }
    }

    private fun auStudio() {
        fermerFeuilles()
        e.auStudioPro()
    }

    private fun lu(): ProjetVu = appli.projet() ?: error("le projet ouvert ne se lit pas")

    private fun surDisque(ok: (ProjetVu.Disque) -> Boolean): ProjetVu.Disque? {
        var d: ProjetVu.Disque? = null
        attendre(sauvegardeMs) { d = appli.projet()?.disque; d?.let(ok) == true }
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

    private fun nonMesure(step: String, why: String): String {
        feu(step, Feu.GRIS to why)
        carnet.add(Carnet.NOTE, step, "${ToutTester.NON_MESURE};$why")
        return "non mesuré : $why"
    }

    private fun feu(step: String, f: Pair<Feu, String>) = carnet.add(Carnet.FEU, step, f.first.mot + ";" + f.second)

    private fun feuEcoute(step: String, b: Ecoute.Bilan) {
        feu(step, Feux.style(b))
        Feux.depasses(b).forEach { carnet.add(Carnet.NOTE, step, "${ToutTester.SEUIL};$it") }
    }

    private fun bilan(step: String, vararg fields: Pair<String, Any?>) =
        carnet.add(Carnet.MESURE, step, "bilan;" + fields.joinToString(";") { (k, v) -> "$k=${v ?: ""}" })

    private fun ecoute(step: String, b: Ecoute.Bilan) = bilan(
        step, "decrochages" to b.xruns, "pire_bloc" to Scenarios.fmt(b.cpuWorst), "plus_long_ecart" to Scenarios.fmt(b.gapWorst),
        "charge_moyenne" to Scenarios.fmt(b.cpuMean), "crete" to Scenarios.fmt(b.peakMax), "secondes" to b.reads / Ecoute.READS_PER_SECOND,
        "alertes" to b.alertes.size,
    )

    companion object {
        const val PREFIXE = "fx_"
        private val ANNULER = listOf("top_undo", "thumb_undo")
        private val RETABLIR = listOf("top_redo", "thumb_redo")
        private const val GESTE = "fx"
        private const val BOUTS = "bouts"
        private const val RETRAIT = "retrait"
        private const val PAIRE = "paire"
        private const val CHAINE = "chaine"
        private val BOUTS_VALEURS = listOf(0f, 1f)
        private val PICK = Regex("pick_scene_\\d+")

        /** The effects' scenarios estimate in all stays under half an hour on a phone. */
        const val DUREE_MAX_S = 30 * 60

        /** The heaviest effects for the processor (long tails, many voices, analysis). */
        val LOURDS = listOf("REVERB", "OTT", "PING_PONG", "ECHO", "FREQ_SHIFT", "PHASER", "PARAM_EQ", "ROBOT")

        /** Four heavy effects on one strip. */
        val CHAINES = listOf(
            listOf("REVERB", "OTT", "FREQ_SHIFT", "PHASER"),
            listOf("PING_PONG", "ECHO", "PARAM_EQ", "ROBOT"),
            listOf("REVERB", "PING_PONG", "OTT", "ROBOT"),
        )

        private val TYPES = EffectType.entries.filter { it != EffectType.NONE }

        /** True when the table line [g] (split on « : ») names one of these works. */
        fun connu(g: List<String>): Boolean = g.firstOrNull() == GESTE && when (g.getOrNull(1)) {
            BOUTS, RETRAIT -> g.size == 3
            PAIRE -> g.size == 4
            CHAINE -> g.size in 4..2 + ProjetVu.EFFETS_MAX
            else -> false
        }

        val LISTE: List<Scenarios.Scenario> = buildList {
            for (et in TYPES) add(
                Scenarios.Scenario(
                    "${PREFIXE}bouts_${et.name.lowercase()}", "Pro : ${et.label}, réglages à fond dans les deux sens", Scenarios.NIVEAU_PRO, 150_000, 24,
                    "chaque réglage pris à 0 puis à 1, le son (ni NaN ni silence) aux deux bouts, la piste comme avant sur le disque",
                    "$GESTE:$BOUTS:${et.name}",
                ),
            )
            for (et in TYPES) add(
                Scenarios.Scenario(
                    "${PREFIXE}retrait_${et.name.lowercase()}", "Pro : ${et.label} retiré en jouant, annulé, rétabli", Scenarios.NIVEAU_PRO, 120_000, 20,
                    "la lecture continue après le retrait ; la piste sur le disque après retrait, « Annuler » et « Rétablir »",
                    "$GESTE:$RETRAIT:${et.name}",
                ),
            )
            for (i in LOURDS.indices) for (k in i + 1 until LOURDS.size) add(
                Scenarios.Scenario(
                    "${PREFIXE}paire_${LOURDS[i].lowercase()}_${LOURDS[k].lowercase()}", "Effets lourds en paire : ${LOURDS[i]} + ${LOURDS[k]}",
                    Scenarios.NIVEAU_MESURE, 120_000, 15, "les décrochages (0 attendu), ni NaN ni silence, les deux effets sur le disque",
                    "$GESTE:$PAIRE:${LOURDS[i]}:${LOURDS[k]}",
                ),
            )
            CHAINES.forEachIndexed { n, c ->
                add(
                    Scenarios.Scenario(
                        "${PREFIXE}chaine_${n + 1}", "Quatre effets lourds sur une piste : ${c.joinToString(" + ")}", Scenarios.NIVEAU_MESURE, 150_000, 20,
                        "les décrochages (0 attendu), ni NaN ni silence, les quatre effets sur le disque",
                        "$GESTE:$CHAINE:${c.joinToString(":")}",
                    ),
                )
            }
        }
    }
}
