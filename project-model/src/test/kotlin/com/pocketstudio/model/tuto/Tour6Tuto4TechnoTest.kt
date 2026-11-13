package com.pocketstudio.model.tuto

import com.pocketstudio.model.allScenes
import com.pocketstudio.model.switchScene
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 6, lot « tuto4 » (seats D a: Japanese, phone, Mode simple, tuto Facile; B b: German, tablet, 130 %, Mode pro,
 * tuto Pro), the techno course: a big beginner, or the musician of the Pro tutorial, reads the step and knows what to do
 * without guessing. Each test reads the course in French and in the languages concerned, from the catalogs.
 */
class Tour6Tuto4TechnoTest {
    private val course = TutoCourse.of("techno")!!
    private val langs = Lang.entries - Lang.FR
    private val steps = course.chapters.flatMap { it.steps }

    private fun step(id: String): TutoStep = steps.first { it.id == id }
    private fun chapter(id: String): TutoChapter = course.chapters.first { it.id == id }

    /** A step's words at both levels (the Pro's own when it has some). */
    private fun texts(s: TutoStep): List<String> = listOfNotNull(s.text, s.textPro)

    /** [fr] in [lang], never the French shown in its place. */
    private fun tr(lang: Lang, fr: String): String = I18n.table(lang)[fr] ?: "(no ${lang.code} translation of « $fr »)"

    /** [text] names [name] between quotes, in any language's quotes, as a pad or a button is named. */
    private fun quotes(text: String, name: String): Boolean =
        Regex("[$OPEN][\\s  ]?${Regex.escape(name.trim())}[\\s  ]?[$CLOSE]").containsMatchIn(text)

    private fun report(what: String, bad: List<String>) = assertTrue("${bad.size} $what:\n" + bad.joinToString("\n"), bad.isEmpty())

    /**
     * M1 (seats D a, B b): ch. 2 to 4 name the pad by the name written on it, in every language and at both levels
     * (« pose la ride » while the pad says « Becken »; « le « toc » » while no pad has that name).
     */
    @Test
    fun padsNamedAsWritten() {
        val pads = mapOf("kick" to "Kick", "rumble" to "Rumble", "clap" to "Clap", "open_hat" to "Charley ouvert", "hat" to "Charley", "perc" to "Rim", "ride" to "Cymbale")
        val bad = mutableListOf<String>()
        for (s in steps) {
            val role = when {
                s.check.startsWith("lane:") -> s.check.removePrefix("lane:")
                s.check == "crash" -> "ride"
                else -> continue
            }
            val name = pads.getValue(role)
            for (fr in texts(s)) {
                if (!quotes(fr, name)) bad += "fr ${s.id}: no « $name »: $fr"
                for (l in langs) tr(l, fr).let { t -> if (!quotes(t, tr(l, name))) bad += "${l.code} ${s.id}: no « ${tr(l, name)} »: $t" }
            }
        }
        // « toc » may say how the « Rim » sounds (« un petit « toc » »), never stand for a pad (« le « toc » »).
        steps.flatMap(::texts).filter { Regex("""\b(le|du|au) « toc »""").containsMatchIn(it) }.forEach { bad += "fr: « toc », no pad's name: $it" }
        report("drum steps that do not name their pad as written", bad)
    }

    /**
     * M2 (seats D a, B b): each musician's word is explained in simple words where it first shows (kick, bass, Drop,
     * bar, stab, Groove, Break, mix, transition, BPM; the hit menu's « Force », « Roulement », « Triolet »), in Japanese too.
     */
    @Test
    fun tradeWordsExplainedWhereTheyFirstShow() {
        val bad = mutableListOf<String>()
        fun need(where: String, text: String, vararg parts: String) = parts.filter { it !in text }.forEach { bad += "$where: no « $it »: $text" }
        need("ref", step("ref").text, "kick (la grosse caisse)", "basse (les notes graves)", "Drop (le moment où tout joue)")
        need("kick chapter", chapter("kick").why, "font une mesure")
        need("clap_do", step("clap_do").text, "« Force » (", "« Roulement » (", "« Triolet » (")
        need("pad_do", step("pad_do").text, "stabs (")
        need("groove_hear", step("groove_hear").text, "« Groove » (")
        need("gap", step("gap").text, "transition (")
        need("map_break", step("map_break").text, "Break (la pause)")
        need("levels", step("levels").text, "le mix, c'est")
        need("tempo", step("tempo").text, "battements par minute")
        // In the Facile course, the first step that says the word (with its chapter's title) is the one that explains it.
        val firsts = mapOf("kick" to "ref", "basse" to "ref", "Drop" to "ref", "stab" to "pad_do", "Groove" to "groove_hear", "Break" to "map_break", "transition" to "gap", "mix" to "levels", "BPM" to "tempo")
        val facile = course.chaptersFor(TutoLevel.Facile).flatMap { c -> c.steps.mapIndexed { i, s -> s.id to ((if (i == 0) c.title + "\n" else "") + s.text) } }
        for ((word, id) in firsts) {
            val first = facile.firstOrNull { (_, t) -> Regex("(?i)\\b${word}s?\\b").containsMatchIn(t) }?.first
            if (first != id) bad += "« $word » first shows in $first, explained in $id"
        }
        // Japanese: ベース, the menu's words, スタブ, グルーヴ, トランジション explained there; German: the bar (Takt).
        val ja = Lang.JA
        need("ja ref", tr(ja, step("ref").text), "ベース（")
        need("ja clap_do", tr(ja, step("clap_do").text), *listOf("Force", "Roulement", "Triolet").map { tr(ja, it) + "」（" }.toTypedArray())
        need("ja pad_do", tr(ja, step("pad_do").text), "スタブ（")
        need("ja groove_hear", tr(ja, step("groove_hear").text), "グルーヴ」（")
        need("ja gap", tr(ja, step("gap").text), "トランジション（")
        need("de kick chapter", tr(Lang.DE, chapter("kick").why), "Takt")
        // Every language keeps the explanations: as many brackets as the French.
        for (id in listOf("ref", "clap_do", "pad_do", "groove_hear", "gap")) {
            val fr = step(id).text
            val n = fr.count { it == '(' }
            for (l in langs) tr(l, fr).let { t -> if (t.count { it == '(' || it == '（' } < n) bad += "${l.code} $id: an explanation lost: $t" }
        }
        report("words of the trade not explained where they first show", bad)
    }

    /**
     * M3 (seats D a, B b): the numbers tell the truth: « Ajouter » makes each part as long as its loop (8 bars, the
     * Montée 4: Tour6TutoBuildBarsTest), the durations said are the step's, the stabs of the Pro's bar 1 are on steps 4 and 11 (one of them only on an offbeat).
     */
    @Test
    fun numbersTellTheTruth() {
        val bad = mutableListOf<String>()
        for (fr in texts(step("song_order"))) {
            if (!Regex("\\b8 mesures\\b").containsMatchIn(fr)) bad += "fr song_order: the 8 bars « Ajouter » gives not said: $fr"
            if (!Regex("Montée 4|4 pour la Montée").containsMatchIn(fr)) bad += "fr song_order: the Montée's 4 bars not said: $fr"
            for (l in langs) tr(l, fr).let { t ->
                listOf("8", "4").filter { n -> !t.contains(Regex("(?<!\\d)$n(?!\\d)")) }.forEach { bad += "${l.code} song_order: no $it: $t" }
            }
        }
        // The durations said are the step's own (« Break 12 »), in every language.
        val phrases = step("song_phrases")
        val numbers = Regex("\\d+").findAll(phrases.song).map { it.value }.sorted().toList()
        for (fr in texts(phrases)) for (t in listOf(fr) + langs.map { tr(it, fr) }) {
            if (Regex("\\d+").findAll(t).map { it.value }.sorted().toList() != numbers) bad += "song_phrases: not the durations $numbers: $t"
        }
        val lengths = phrases.song.split(',').associate { it.substringBeforeLast(':') to it.substringAfterLast(':').toInt() }
        val (b, m) = lengths.getValue("Break") to lengths.getValue("Montée")
        if ("$b + $m = ${b + m}" !in step("map_phrases").why.orEmpty()) bad += "map_phrases: not « $b + $m = ${b + m} »"
        // The stab's bar 1: as many chords as said, on the steps said; an offbeat is the 3rd sixteenth of a beat.
        val chords = course.lines.getValue("pad").split(' ').map { it.substringBefore(':').toInt() }.filter { it < 16 }
        if ("les ${chords.size} accords de la mesure 1" !in step("pad_do").text) bad += "pad_do: not ${chords.size} chords: ${step("pad_do").text}"
        val pro = step("pad_do").textPro!!
        if ("pas ${chords.joinToString(" et ") { "${it + 1}" }}" !in pro) bad += "pad_do Pro: not the steps $chords: $pro"
        if (chords.any { it % 4 != 2 }) {
            if ("contretemps" in pro) bad += "fr pad_do Pro: « contretemps » while steps ${chords.map { it + 1 }} are not all offbeats: $pro"
            tr(Lang.DE, pro).let { if ("Offbeat" in it) bad += "de pad_do Pro: « Offbeats »: $it" }
        }
        // The riff: as many notes per bar as said.
        val lead = course.lines.getValue("lead").split(' ').map { it.substringBefore(':').toInt() }
        val said = "${lead.count { it < 16 }} à la mesure 1, ${lead.count { it in 16 until 32 }} à la mesure 2"
        if (said !in step("lead_do").text) bad += "lead_do: not « $said »"
        report("numbers that do not match the course", bad)
    }

    /**
     * M4 (seats D a, B b): no contradiction: the Break keeps the hats and the « Rim »; the Montée « Créer la partie » makes
     * still plays its last beat (its silence is the transitions' step); ch. 10 says which button makes each part.
     */
    @Test
    fun noContradiction() {
        val bad = mutableListOf<String>()
        for (fr in texts(step("make_montee"))) if ("se tait" in fr) bad += "make_montee: says the end is silent, the gap step makes it: $fr"
        for (id in listOf("map_break", "make_break")) step(id).text.let { fr ->
            listOf("charleys", "« Rim »", "stab").filter { it !in fr }.forEach { bad += "$id: says nothing of the $it it keeps: $fr" }
        }
        for (s in chapter("parts").steps.filter { it.check.startsWith("part:") }) {
            val label = s.check.removePrefix("part:").substringBefore(':')
            for (fr in texts(s)) {
                if ("« Créer la partie « $label » »" !in fr) bad += "fr ${s.id}: not « Créer la partie « $label » »: $fr"
                for (l in langs) {
                    val button = tr(l, "Créer la partie « %s »")
                    val shown = Regex(Regex.escape(button.substringBefore("%s")) + ".+?" + Regex.escape(button.substringAfter("%s")))
                    tr(l, fr).let { t -> if (!shown.containsMatchIn(t)) bad += "${l.code} ${s.id}: not its button « $button »: $t" }
                }
            }
        }
        report("steps that contradict the screen", bad)
    }

    /**
     * M5 (seat D a): the fader's marks (+4, 0, −12, −∞, SimpleMix) and the export's « Niveau » and « Pas de saturation »
     * (ExportCheck) said in one simple sentence, in every language.
     */
    @Test
    fun marksAndExportWordsSaid() {
        val bad = mutableListOf<String>()
        val levels = step("levels").text
        for (t in listOf(levels) + langs.map { tr(it, levels) }) {
            listOf("+4", "−12", "−∞").filter { it !in t }.forEach { bad += "levels: no « $it »: $t" }
        }
        val share = step("share").text
        listOf("Niveau", "Pas de saturation").filter { !quotes(share, it) }.forEach { bad += "fr share: no « $it »: $share" }
        for (l in langs) {
            val t = tr(l, share)
            val level = tr(l, "Niveau : %s").split(':', '：').first().trim()
            val clean = tr(l, "Pas de saturation.").trimEnd('.', '。', '।')
            listOf(level, clean).filter { !quotes(t, it) }.forEach { bad += "${l.code} share: no « $it »: $t" }
        }
        report("marks or export words not said", bad)
    }

    /** M6 (seat B b): the Clap's reverb send is in the pad's effects, under its grid: the Mix has no strip per pad. */
    @Test
    fun reverbSendFromThePadsEffects() {
        val bad = mutableListOf<String>()
        for (fr in texts(step("reverb"))) {
            listOf("Clap", "Effets du pad", "Envoi réverbe").filter { !quotes(fr, it) }.forEach { bad += "fr reverb: no « $it »: $fr" }
            if (Regex("\\bMix\\b|tranche").containsMatchIn(fr)) bad += "fr reverb: sends to the Mix: $fr"
            for (l in langs) {
                val t = tr(l, fr)
                listOf("Clap", "  Effets du pad", "Envoi réverbe").map { tr(l, it).trim() }.filter { !quotes(t, it) }.forEach { bad += "${l.code} reverb: no « $it »: $t" }
                if (tr(l, "Mix") in t) bad += "${l.code} reverb: sends to the Mix: $t"
            }
        }
        report("reverb steps that do not lead to the pad's effects", bad)
    }

    /**
     * M7 (seats D a, B b): one word per thing: the kit (the pads may show the built-in sounds, « Kit : Studio »: no
     * « 909 kit », the card's « Plus tard »), a « partie » (not « scène »), the « Cymbale » (not « ride » in Facile),
     * « Partager » as the button says it (Japanese シェア, never 共有).
     */
    @Test
    fun oneWordPerThing() {
        val bad = mutableListOf<String>()
        for (fr in texts(step("ready"))) {
            if ("909" in fr) bad += "fr ready: promises a 909 kit the pads may not show: $fr"
            if (!quotes(fr, "Plus tard")) bad += "fr ready: no « Plus tard »: $fr"
            for (l in langs) tr(l, fr).let { t -> if (!quotes(t, tr(l, "Plus tard"))) bad += "${l.code} ready: no « ${tr(l, "Plus tard")} »: $t" }
        }
        steps.flatMap(::texts).filter { Regex("(?i)\\bscènes?\\b").containsMatchIn(it) }.forEach { bad += "fr: « scène » for a part: $it" }
        course.chaptersFor(TutoLevel.Facile).flatMap { it.steps }.flatMap { listOfNotNull(it.text, it.why) }
            .filter { Regex("(?i)\\bride\\b").containsMatchIn(it) }.forEach { bad += "fr Facile: « ride » for the « Cymbale » pad: $it" }
        for (fr in texts(step("share"))) {
            if (!quotes(fr, "Partager")) bad += "fr share: no « Partager »: $fr"
            for (l in langs) tr(l, fr).let { t -> if (!quotes(t, tr(l, "Partager"))) bad += "${l.code} share: no « ${tr(l, "Partager")} »: $t" }
        }
        val ja = I18n.area(Lang.JA, "tuto") + I18n.area(Lang.JA, "tuto_techno")
        (steps.flatMap { listOfNotNull(it.text, it.textPro, it.why) } + I18n.area(Lang.JA, "tuto").keys).mapNotNull { ja[it] }
            .filter { "共有" in it }.forEach { bad += "ja: 共有 where the button says シェア: $it" }
        report("things called by two names", bad)
    }

    companion object {
        private const val OPEN = "«“„「『\"‘"
        private const val CLOSE = "»”“」』\"’"
    }

    /**
     * M7, fresh review (moyen): the house says « Partage ton morceau » and « touche « Partager » et envoie ton morceau »,
     * the techno still said « Exporte ton morceau » and « exporte ton morceau et partage-le »: one word, « Partager », in
     * the chapter's title and its steps, in French and in every language (the same translations as the house's).
     */
    @Test
    fun theShareChapterSaysShareLikeTheHouse() {
        val bad = ArrayList<String>()
        val export = chapter("export")
        (listOf(export.title) + export.steps.flatMap { texts(it) }).filter { Regex("(?i)export").containsMatchIn(it) }.forEach { bad += "fr: « $it »" }
        for (lang in Lang.entries - Lang.FR) {
            (listOf(export.title) + texts(step("share"))).forEach { fr -> if (I18n.table(lang)[fr] == null) bad += "${lang.code}: no translation of « $fr »" }
        }
        report("share texts", bad)
    }
    /**
     * Round trip 1 after Da-5, M1 (« 手拍子たち », screen 35): in the Mode simple a step names one pad by the word the pad
     * shows, never a plural (« Clap » and « Claps » give the same word, the plural's everyday word used to win), in every language.
     */
    @Test
    fun theSimpleModeNamesOnePadInTheSingular() {
        val bad = ArrayList<String>()
        try {
            I18n.everyday = true
            for (lang in Lang.entries) {
                I18n.lang = lang
                val clap = I18n.t("Clap")
                I18n.t(step("clap_do").text).let { t -> if (!quotes(t, clap)) bad += "${lang.code} clap_do: not the pad's « $clap »: $t" }
                // « Kick » shows « Grosse caisse 1 » on the pad: the step may leave out the number, never say a plural.
                val kick = I18n.t("Kick")
                I18n.t(step("kick_do").text).let { t -> if (!quotes(t, kick) && !quotes(t, kick.removeSuffix(" 1"))) bad += "${lang.code} kick_do: not the pad's « $kick »: $t" }
            }
        } finally {
            I18n.everyday = false
            I18n.lang = Lang.FR
        }
        report("steps naming one pad by a plural", bad)
    }

    /**
     * Round trip 1 after Da-5, M2: the « Rim » pad explained where it first shows (screen 177: « Rim » never said), and a
     * chapter's title explains its word of the trade, since the « Bravo » of the chapter before already shows it
     * (screen 92 « スタブ », explained only on screen 93; the same for « Groove » and « transition »), in every language.
     */
    @Test
    fun theRimAndTheTitlesWordsExplainedWhereTheyFirstShow() {
        val bad = ArrayList<String>()
        val firstRim = steps.first { s -> texts(s).any { "Rim" in it } }
        if (firstRim.id != "perc_do") bad += "« Rim » first shows in ${firstRim.id}"
        val rim = firstRim.text
        if (!Regex("« Rim » \\([^)]*bord").containsMatchIn(rim)) bad += "fr perc_do: the « Rim » not explained (the rim of a drum): $rim"
        for (l in langs) I18n.table(l)[rim].let { t -> if (t == null) bad += "${l.code}: no translation of « $rim »" else if (t.count { it == '(' || it == '（' } < 1) bad += "${l.code} perc_do: no explanation: $t" }
        val titles = mapOf("chords" to "accord court", "groove" to "boucle", "transitions" to "passage")
        val facile = course.chaptersFor(TutoLevel.Facile).associateBy { it.id }
        for ((id, plain) in titles) {
            val title = facile.getValue(id).title
            if (plain !in title) bad += "fr title of $id: no « $plain »: $title"
            for (l in langs) if (I18n.table(l)[title] == null) bad += "${l.code}: no translation of « $title »"
        }
        I18n.table(Lang.JA)[facile.getValue("chords").title]?.let { if ("スタブ（" !in it) bad += "ja chords title: スタブ not explained: $it" }
        report("words shown before they are explained", bad)
    }
}
