package com.pocketstudio.model.tuto

import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 6, lot « tuto4 », seat H a (house, French, phone, Mode simple, tuto Facile): a beginner reads each step of the
 * house tutorial and knows what to do without guessing, in French and in every language (no text falls back to French).
 */
class Tour6Tuto4HouseTest {
    private val course = TutoCourse.of("house")!!
    private val target = TutoTarget.of("house")!!
    private val translated = Lang.entries - Lang.FR

    private fun chapter(id: String) = course.chapters.first { it.id == id }
    private fun step(id: String) = course.chapters.flatMap { it.steps }.first { it.id == id }

    /** Every French text of the house course (titles, « Pourquoi ? », consignes Facile and Pro, « Suivant »'s warning). */
    private fun allTexts(): List<String> = course.chapters.flatMap { c ->
        listOfNotNull(c.title, c.titlePro, c.why) + c.steps.flatMap { s -> listOfNotNull(s.text, s.textPro, s.why, s.skip) }
    }.filter { it.isNotEmpty() }.distinct()

    /** What a Facile reader sees, in order: each chapter's title, then each of its steps' consigne. */
    private fun facileReading(): List<String> = course.chaptersFor(TutoLevel.Facile).flatMap { c -> listOf(c.title) + c.steps.map { it.text } }

    /** [fr] in [lang], null when that language has none (it would show the French). */
    private fun tr(lang: Lang, fr: String): String? = I18n.table(lang)[fr]?.takeIf { it.isNotBlank() }

    private fun word(lang: Lang, fr: String): String = I18n.table(lang)[fr] ?: fr

    /** [texts] said in every language (a missing one is shown in French). */
    private fun everyLanguage(texts: List<String>, bad: MutableList<String>) {
        for (fr in texts) for (lang in translated) if (tr(lang, fr) == null) bad += "${lang.code}: no translation of « $fr »"
    }

    private fun report(what: String, bad: List<String>) = assertTrue("${bad.size} $what:\n" + bad.take(40).joinToString("\n"), bad.isEmpty())

    /** M1: chapters 2 and 4 name the pad by what is written on it (« Rim », shown « Bord de caisse »), never « tambourin ». */
    @Test
    fun thePadIsNamedAsWrittenOnIt() {
        val bad = mutableListOf<String>()
        allTexts().filter { "tambourin" in it.lowercase() }.forEach { bad += "fr: « $it »" }
        val rim = allTexts().filter { "« Rim »" in it }
        listOf(step("perc_do").text, step("ready").text).forEach { if ("« Rim »" !in it) bad += "fr: the pad not named « Rim »: $it" }
        if ("tambourin" in chapter("hats").title.lowercase()) bad += "fr: title ${chapter("hats").title}"
        for (lang in translated) {
            val name = word(lang, "Rim")
            for (fr in rim) tr(lang, fr)?.let { v -> if (name !in v) bad += "${lang.code}: no « $name »: $v" }
            val all = allTexts().mapNotNull { tr(lang, it) }
            all.filter { v -> TAMBOURINE.any { it.containsMatchIn(v) } }.forEach { bad += "${lang.code}: tambourine in « $it »" }
        }
        everyLanguage(allTexts(), bad)
        report("texts that do not name the pad as written", bad)
    }

    /** M2: Groove, kick, relances, Break, Mix, solo, Tonique, BPM: each said in simple words where a Facile reader first meets it. */
    @Test
    fun musicianWordsAreExplainedAtFirstSight() {
        val bad = mutableListOf<String>()
        val reading = facileReading()
        for ((w, explained) in FIRST_SIGHT) {
            val first = reading.firstOrNull { w.containsMatchIn(it) }
            if (first == null) { bad += "fr: « ${w.pattern} » never said"; continue }
            if (!explained.containsMatchIn(first)) bad += "fr: « ${w.pattern} » not explained where first seen: $first"
            everyLanguage(listOf(first), bad)
        }
        // The key sheet's own word, as the sheet shows it in each language.
        val key = step("key").text
        for (lang in translated) tr(lang, key)?.let { v -> if (word(lang, "Tonique") !in v) bad += "${lang.code}: no « ${word(lang, "Tonique")} »: $v" }
        report("musician words met unexplained", bad)
    }

    /** M3: the numbers a step says are the step's own: the cells of the « Rim » pad; the parts' length before « Durées ». */
    @Test
    fun theNumbersAreTheStepsOwn() {
        val bad = mutableListOf<String>()
        var p = target.blank()
        var checked = 0
        for (s in course.chaptersFor(TutoLevel.Facile).flatMap { it.steps }) {
            if (s.id == "perc_do") {
                val (_, cells) = TutoChecks.hint(s, target, p.drums.steps)!!
                val said = Regex("""(\d+) cases""").find(s.text)?.groupValues?.get(1)?.toInt()
                if (said != cells.size) bad += "fr: says $said cells, ${cells.size} dotted: ${s.text}"
                for (lang in translated) tr(lang, s.text)?.let { v -> if ("${cells.size}" !in v) bad += "${lang.code}: no ${cells.size}: $v" }
                checked++
            }
            if (!TutoChecks.done(s, p, target)) p = TutoChecks.show(s, p, target) ?: p
        }
        // « Ajouter » brings a part at its loop's length: the order step says the lengths come next (the map said 16).
        val order = step("song_order").text
        if ("durée" !in order) bad += "fr: the order step does not say the parts' length comes next: $order"
        everyLanguage(listOf(order), bad)
        assertTrue("perc_do not met", checked == 1)
        report("numbers that are not the step's", bad)
    }

    /** M4: no sentence says what another denies: the Break keeps its hats; the Montée's silence comes later; one button for the Drop 2. */
    @Test
    fun theSentencesAgree() {
        val bad = mutableListOf<String>()
        val brk = step("map_break").text
        if ("batterie se tait" in brk) bad += "fr: $brk"
        if ("hat" in step("make_break").keep && "charleys" !in brk) bad += "fr: the Break's hats not said: $brk"
        val montee = step("make_montee").text
        val gap = course.chapters.first { c -> c.steps.any { it.check == "gap" } }
        if ("tout se tait" in montee) bad += "fr: the Montée said silent before its gap step: $montee"
        if ("« ${gap.title} »" !in montee) bad += "fr: the Montée does not say where its silence is made (« ${gap.title} »): $montee"
        for (lang in translated) tr(lang, montee)?.let { v -> if (word(lang, gap.title) !in v) bad += "${lang.code}: no « ${word(lang, gap.title)} »: $v" }
        val drop2 = step("make_drop2").text
        if ("Montre-moi" in drop2) bad += "fr: « Montre-moi » keeps nothing, the part is made by « Créer la partie »: $drop2"
        if ("« Créer la partie »" !in drop2) bad += "fr: the button to touch not named: $drop2"
        everyLanguage(listOf(brk, montee, drop2), bad)
        report("sentences that contradict", bad)
    }

    /** M5: « quelques coups … une couche de plus » says how many; the fader's and the export's numbers in one simple sentence. */
    @Test
    fun howManyAndWhatTheNumbersMean() {
        val bad = mutableListOf<String>()
        val variation = step("variation").text
        if ("quelques" in variation || "couche" in variation) bad += "fr: how many, what a « couche » is: $variation"
        if (!Regex("""\d+ (cases|coups)""").containsMatchIn(variation)) bad += "fr: no number of hits: $variation"
        val levels = step("levels").text
        val share = step("share").text
        for ((text, signs) in listOf(levels to listOf("+4", "−∞"), share to listOf("−14", "saturation"))) {
            signs.filter { it !in text }.forEach { bad += "fr: « $it » not said: $text" }
        }
        for (lang in translated) {
            tr(lang, levels)?.let { v -> listOf("+4", "−∞").filter { it !in v }.forEach { bad += "${lang.code}: no $it: $v" } }
            tr(lang, share)?.let { v -> if ("−14" !in v) bad += "${lang.code}: no −14: $v" }
        }
        everyLanguage(listOf(variation, levels, share), bad)
        report("numbers left unsaid", bad)
    }

    /** M6: the Clap's reverb send is in the pad's effects (« Effets du pad », from the grid), not in the Mix. */
    @Test
    fun theClapsReverbIsInThePadsEffects() {
        val bad = mutableListOf<String>()
        val reverb = step("reverb").text
        val button = "  Effets du pad".trim()
        if ("« $button »" !in reverb) bad += "fr: no « $button »: $reverb"
        if (Regex("""dans Mix, monte « Envoi réverbe » sur le pad""").containsMatchIn(reverb)) bad += "fr: the pad's send said in the Mix: $reverb"
        for (lang in translated) tr(lang, reverb)?.let { v ->
            val shown = word(lang, "  Effets du pad").trim()
            if (shown !in v) bad += "${lang.code}: no « $shown »: $v"
        }
        everyLanguage(listOf(reverb), bad)
        report("reverb steps that lead elsewhere", bad)
    }

    companion object {
        /** The word for a tambourine in each language's former texts. */
        val TAMBOURINE = listOf(
            "tambourine", "tamburin", "pandereta", "खंजरी", "tamborin", "tamburello", "タンバリン", "탬버린", "tamboerijn",
            "tamburyn", "pandeiro", "(?<!\\p{L})tef(?!\\p{L}{2})", "铃鼓", "الدف(?!\\p{L})",
        ).map { Regex(it, RegexOption.IGNORE_CASE) }

        /** Each word (as written) → what has to stand by it where a Facile reader first meets it. */
        val FIRST_SIGHT: List<Pair<Regex, Regex>> = listOf(
            Regex("""(?i)\bkick\b""") to Regex("""grosse caisse"""),
            Regex("""\bGroove\b""") to Regex("""boucle de base"""),
            Regex("""\brelances?\b""") to Regex("""petits coups"""),
            Regex("""\bBreak\b""") to Regex("""pause"""),
            Regex("""« Mix »""") to Regex("""volume de chaque son"""),
            Regex("""\bsolo\b""") to Regex("""toute seule"""),
            Regex("""\bTonique\b""") to Regex("""note de départ"""),
            Regex("""\bBPM\b""") to Regex("""battements par minute"""),
        )
    }
}
