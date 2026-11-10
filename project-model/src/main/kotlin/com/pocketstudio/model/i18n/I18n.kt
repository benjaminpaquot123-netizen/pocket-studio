package com.pocketstudio.model.i18n

import kotlinx.serialization.json.Json
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * The languages of the interface; French is the app's own language (its texts are the keys).
 *
 * Adding a language: one entry here (its BCP 47 [code], its name in itself, its [flag] emoji, [rtl] for a language
 * written right to left), its place in [Lang.PICKER], its catalog folder `i18n/<code>/` (the
 * [I18n.AREAS] files) and, when its plural is not one/other, its rule in [pluralOf]. The picker lists every entry, the phone's language picks it at the first launch, and a
 * right-to-left one turns the text screens around while the musical ones stay left to right.
 */
enum class Lang(val code: String, val nativeName: String, val flag: String, val rtl: Boolean = false) {
    // The first three keep their place: when two languages give the same word, the first one wins (DefaultNames).
    FR("fr", "Français", "🇫🇷"),
    EN("en", "English", "🇬🇧"),
    NL("nl", "Nederlands", "🇳🇱"),
    ES("es", "Español", "🇪🇸"),
    PT_BR("pt-BR", "Português (Brasil)", "🇧🇷"),
    DE("de", "Deutsch", "🇩🇪"),
    IT("it", "Italiano", "🇮🇹"),
    PL("pl", "Polski", "🇵🇱"),
    TR("tr", "Türkçe", "🇹🇷"),
    ID("id", "Bahasa Indonesia", "🇮🇩"),
    HI("hi", "हिन्दी", "🇮🇳"),
    AR("ar", "العربية", "🇸🇦", rtl = true),
    JA("ja", "日本語", "🇯🇵"),
    KO("ko", "한국어", "🇰🇷"),
    ZH_CN("zh-CN", "简体中文", "🇨🇳");

    /**
     * The language's locale for formatting and casing, always with Western digits (0-9): the texts
     * write them (« 120 BPM »), and Arabic would otherwise format numbers as ١٢٠.
     */
    val locale: Locale by lazy { Locale.Builder().setLanguageTag(code).setUnicodeLocaleKeyword("nu", "latn").build() }

    /**
     * This language's name written in [ui] (« Allemand » for German in a French interface), from
     * the platform's CLDR names, capitalised the way [ui] does; the bare language (« Portugais »,
     * « Chinois »): the native name shown above it already says which one.
     */
    fun nameIn(ui: Lang): String =
        Locale.forLanguageTag(code).getDisplayLanguage(ui.locale).ifBlank { nativeName }.replaceFirstChar { it.titlecase(ui.locale) }

    /**
     * Whether musicians of this language name notes Do Ré Mi (French, Spanish, Portuguese, Italian,
     * Turkish, Arabic) rather than C D E: the notation chosen with the language, and the one its texts write.
     */
    val solfege: Boolean get() = this in SOLFEGE

    companion object {
        // Arabic too (round 3, n° 38): دو ري مي, as taught in the Arab world.
        private val SOLFEGE = setOf(FR, ES, PT_BR, IT, TR, AR)

        fun of(code: String?): Lang = entries.firstOrNull { it.code == code } ?: FR

        /**
         * The language list as the picker shows it: the Latin-script ones by their own name, then
         * the other scripts (each name is written in itself, so anyone finds theirs).
         */
        val PICKER: List<Lang> get() = listOf(ID, DE, EN, ES, FR, IT, NL, PL, PT_BR, TR, AR, HI, JA, KO, ZH_CN)

        /**
         * The interface language for a phone set to [locale]: the same language and region first
         * (pt-BR), then the same language (fr-BE, nl-BE, en-GB; pt-PT gets Brazilian Portuguese,
         * zh-TW and zh-HK get simplified Chinese: closer than English), else English, the most
         * widely read of the others (French stays the app's default only for French phones).
         */
        fun forLocale(locale: Locale): Lang {
            // Android still says "in" for Indonesian (and "iw" for Hebrew, "ji" for Yiddish).
            val language = when (val l = locale.language.lowercase(Locale.ROOT)) { "in" -> "id"; "iw" -> "he"; "ji" -> "yi"; else -> l }
            fun languageOf(l: Lang) = Locale.forLanguageTag(l.code).language.lowercase(Locale.ROOT).let { if (it == "in") "id" else it }
            fun countryOf(l: Lang) = Locale.forLanguageTag(l.code).country
            return entries.firstOrNull { languageOf(it) == language && countryOf(it).isNotEmpty() && countryOf(it).equals(locale.country, ignoreCase = true) }
                ?: entries.firstOrNull { languageOf(it) == language && countryOf(it).isEmpty() }
                ?: entries.firstOrNull { languageOf(it) == language }
                ?: EN
        }
    }
}

/**
 * Interface texts. The code writes every visible text in French inside [tr]; the French text is
 * the key of its translation in `i18n/<lang>/<area>.json` (Java resources of this module, one
 * JSON object per area). A text with no translation shows in French.
 */
object I18n {
    /** One translation file per area and language (they are merged). */
    val AREAS = listOf(
        "common", "home", "rhythm", "song", "synth", "audio", "mixer", "tools", "model", "finish", "master", "parts", "record",
        "write", "instruments", "learn", "nav", "bugreport", "guide", "a11y", "brand", "plurals", "listen", "tuto", "mixsimple", "retours",
        "remixvoix",
        // Fin 8 (2026-11-02): mute / solo icons, « Piocher dans la chanson », the tuto demos.
        "fin8",
        "remixroute",
        // Remix testers (lot « remix 1 »): finding and understanding the Remix.
        "extract",
        "board",
        // Owner (2026-10-29): « Simple ou Pro ? » for each project, and the home screen's Remix card.
        "mode",
        // « Tuto de A à Z » of every style (2026-10-29): the courses' texts, one file per style family.
        "tuto_techno", "tuto_house", "tuto_bass", "tuto_hiphop", "tuto_autres",
        // Owner (2026-10-29): « Ma bibliothèque de sons » and the Remix from the home screen.
        "mysounds",
        // Owner (2026-10-30): the one sound browser (folders by meaning, Récents, Favoris), « Fête », the ready-to-play live pads.
        "browser",
        // Lot C (n° 24): « Découper la chanson », « Garder » filed by song, the rights line at sharing.
        "remixc",
        // Pad Live (n° 25, lot B1): the home screen's card, the pages, the pad modes, the packs.
        "padlive",
        // Lot E (« Master pro »): the master's « Avancé », the file's final form, the pro readings and report.
        "masterpro",
        // Lot D (« Restes »): the Live's remix cell (a passage of the song per brick), the small leftovers.
        "restes",
        // Owner (2026-11-04): « Un bout », the cut screen's two bars.
        "remixbout",
        // Audit ultra round 1, zones 2 and 4: one loudness target, the export's report, what the ● records.
        "ultra1_export",
        // Audit ultra round 1, zone 3: names' limits, the Guide's « C'est fait », a new part in the Mode simple.
        "tour1nav",
        // Audit ultra round 1, zones 5 and 6: the assistant's length, the tutorial's hints, the plain words, the controls' names.
        "audit1z56",
        // Audit ultra round 1, zones 5 and 6 (team 4b): the tutorial's chapter end, kept sounds named and renamed.
        "audit1z56b",
        // Audit ultra round 1, zone 1: the song's « Découper ce que je veux », the pad picker, the challenges' list.
        "ultra1z1",
        // Audit ultra round 2, zone 1: a sound that could not go on its pad said, « Défis » as stars, the pad's sound marked.
        "ultra2z1",
        // Audit ultra round 2, zone 3: « Créer et écouter », « Sauvegardé », a track's new sound or deletion said.
        "ultra2z3",
        // Audit ultra round 2, zone 4: takes numbered, « Audio 1 » named, the microphone's level, the voice from home.
        "ultra2z4",
        // Audit ultra round 2, zone 5: a note's menu in the piano roll, the synth's units, the track's sound settings, a filter said as set.
        "ultra2z5",
        // Audit ultra round 2, zone 6: the Mode simple's pad names and tempo in everyday words, the Remix model asked first.
        "ultra2z6",
        // Audit ultra round 3, zone 2: the target where one exports, the remix's stems, a stale export, sending the project, « Mes exports ».
        "ultra3z2",
        // Audit ultra round 3, zone 3: the tutorial under way taken up again, a name field's ✕.
        "ultra3z3",
        // Audit ultra round 3, zone 1: a Live column cut in Mix, the pad asked before a drum sound is covered, a piece touched.
        "ultra3z1",
        // Audit ultra round 3, zone 5: the sound search's examples found, the tutorial's words.
        "ultra3z5",
        // Audit ultra round 3, zone 6: counts said with their plural in every language, units and words put in.
        "ultra3z6",
        // Hands-on audit, chantier 1 (2026-11-10): « Partager » everywhere, « Mes fichiers », the project saved as a .zip.
        "partage",
        // Hands-on audit, chantier 2 (2026-11-10): what « Avancé » and « Avancé : master pro » hold, said on them closed.
        "composants",
        // Round 5's blockers (2026-11-10): the part picked while the music plays, said until its bar comes.
        "bloquants",
        // Round 5, chantier 3 (2026-11-10): one word per thing (« Fais-moi un morceau », « Visite rapide »), no dB in the Mode simple.
        "textes",
    )

    @Volatile
    var lang: Lang = Lang.FR

    private val tables = ConcurrentHashMap<Lang, Map<String, String>>()
    private val json = Json { ignoreUnknownKeys = true }

    /** Every translation of [lang], by French text. */
    fun table(lang: Lang): Map<String, String> =
        if (lang == Lang.FR) emptyMap() else tables.getOrPut(lang) { AREAS.flatMap { area(lang, it).entries }.associate { it.key to it.value } }

    /** The translations of one area (empty when the file is missing). */
    fun area(lang: Lang, area: String): Map<String, String> {
        val stream = I18n::class.java.classLoader?.getResourceAsStream("i18n/${lang.code}/$area.json") ?: return emptyMap()
        return stream.bufferedReader(Charsets.UTF_8).use { json.decodeFromString<Map<String, String>>(it.readText()) }
    }

    /**
     * Phase 36, Mode simple: the app's jargon (« Lead », « Pluck », « Quantifier »…) said in
     * everyday words, in every language, while it is on (the settings set it, as [lang]). Only what
     * is shown changes: names are saved as they are.
     */
    @Volatile
    var everyday: Boolean = false

    /** The French texts [everyday] replaces, with their everyday French (each one has its translations). */
    val EVERYDAY: Map<String, String> = mapOf(
        "Lead" to "Mélodie", "Leads" to "Mélodies", "Lead carré" to "Mélodie carrée", "Lead acide" to "Mélodie acide",
        "Lead trance" to "Mélodie trance", "Mélodie (lead)" to "Mélodie", "Pluck" to "Son court", "Pluck future" to "Son court futur",
        "Pluck / percussif" to "Son court / percussif", "Stabs" to "Accords courts", "Accords (nappe)" to "Accords",
        "Sub" to "Basse profonde", "Sub drop" to "Chute grave", "Quantifier" to "Remettre en rythme",
        "Lead qui crie" to "Mélodie qui crie", "Lead chiptune" to "Mélodie chiptune", "Lead filtré" to "Mélodie filtrée",
        "Lead hyperpop" to "Mélodie hyperpop", "Lead / mélodie" to "Mélodie", "Lead 2 (sawtooth)" to "Mélodie 2 (dents de scie)",
        // Audit ultra round 2, n° 28 (seats A a, B b): the pads, the tempo and the takes' tools said in the person's words.
        "Kick" to "Grosse caisse 1", "Kick 2" to "Grosse caisse 2", "Kick studio" to "Grosse caisse studio", "Kick dur" to "Grosse caisse sèche",
        "Rumble" to "Grondement grave", "Clap" to "Claquement de mains", "Rim" to "Bord de caisse", "Cowbell" to "Cloche",
        "Shaker" to "Maracas", "Zap" to "Laser", "Kicks" to "Grosses caisses", "Claps" to "Claquements de mains",
        "BPM" to "Tempo", "Tempo : %d BPM" to "Tempo : %d battements par minute",
        "Normaliser" to "Volume au max", "Inverser" to "À l'envers",
    )

    /**
     * Tests only (Audit §3, « pseudo-traduction »): when set, every text is shown through it, from its
     * French (longer, accented, right to left with [lang] set to a right-to-left language), instead of
     * its translation. The app never sets it.
     */
    @Volatile
    var pseudo: ((String) -> String)? = null

    /**
     * Keys whose French is a word another key already is, shown in French as their value (audit ultra round 1,
     * n° 40: « Annuler » is both « Cancel » and « Undo »; the undo arrow said « Cancel » in English).
     */
    val FRENCH_SHOWN: Map<String, String> = mapOf("Annuler (défaire)" to "Annuler")

    fun t(fr: String): String {
        val text = if (everyday) EVERYDAY[fr] ?: fr else fr
        pseudo?.let { return it(FRENCH_SHOWN[text] ?: text) }
        val shown = shown(lang, text)
        return if (everyday) everydayQuoted(shown) else shown
    }

    private fun shown(lang: Lang, text: String): String =
        if (lang == Lang.FR) FRENCH_SHOWN[text] ?: text else table(lang)[text]?.takeIf { it.isNotEmpty() } ?: FRENCH_SHOWN[text] ?: text

    /** The quotes a name is written between, in every language (« », “ ”, „ “, 「 」…). */
    private const val OPEN = "«“„「『\"‘"
    private const val CLOSE = "»”“」』\"’"

    /** Per language: the app's word of each [EVERYDAY] entry between quotes → its everyday word, longest first. */
    private val quotedNames = ConcurrentHashMap<Lang, Pair<Regex, Map<String, String>>?>()
    private val quotedDone = ConcurrentHashMap<String, String>()

    /**
     * Audit ultra round 2, n° 28: a text that names a pad or a button between quotes (« sur le pad « Rumble » »,
     * « touche « Normaliser » ») says the name the Mode simple shows (« Grondement grave », « Volume au max »);
     * in Korean, the particle after it follows the new word (« “클랩”을 » → « “손뼉”을 »).
     */
    private fun everydayQuoted(text: String): String {
        if (text.none { it in OPEN }) return text
        val lang = lang
        val (regex, names) = quotedNames.computeIfAbsent(lang) { l ->
            val names = EVERYDAY.entries.associate { (pro, plain) -> shown(l, pro) to shown(l, plain) }.filter { (a, b) -> a != b && a.isNotBlank() }
            if (names.isEmpty()) null
            else {
                val space = "[\\s\u00A0\u202F]?"
                val alternatives = names.keys.sortedByDescending { it.length }.joinToString("|") { Regex.escape(it) }
                Regex("(?<=[$OPEN]$space)(?:$alternatives)(?=$space[$CLOSE])") to names
            }
        } ?: return text
        return quotedDone.getOrPut(lang.code + "\u0000" + text) {
            val replaced = regex.replace(text) { m -> names.getValue(m.value) }
            if (lang == Lang.KO && replaced != text) KoreanParticles.follow(replaced, names.values.toSet()) else replaced
        }
    }

    private val reverse = ConcurrentHashMap<Lang, Map<String, String>>()

    /** The app's own word of each everyday one (the first when several give it). */
    private val everydayReverse: Map<String, String> by lazy { EVERYDAY.entries.reversed().associate { it.value to it.key } }

    /**
     * The French text a shown text was translated from (itself when unknown): a key that stays the
     * same in every language, and with the Mode simple's words on (« Mélodie » is shown for « Lead »).
     */
    fun frenchOf(shown: String): String {
        val fr = if (lang == Lang.FR) shown else reverse.getOrPut(lang) { table(lang).entries.associate { it.value to it.key } }[shown] ?: shown
        return if (everyday) everydayReverse[fr] ?: fr else fr
    }
}

/** [fr] in the interface's language. */
fun tr(fr: String): String = I18n.t(fr)

/** [fr] in the interface's language, its `%s` / `%d` / `%1$s` filled with [args] (numbers in the language's format). */
fun tr(fr: String, vararg args: Any?): String {
    val lang = I18n.lang
    val text = String.format(lang.locale, I18n.t(fr), *args)
    // Round 3, n° 38: what a filled-in word changes around it (« “%s”을(를) », « em %s » with « a parte A »).
    return when (lang) {
        Lang.KO -> KoreanParticles.resolve(text)
        Lang.PT_BR, Lang.ES -> Contractions.join(lang, text, args)
        else -> text
    }
}

/**
 * "50 %" in French, German and Spanish (a space before the sign), "%50" in Turkish (the sign
 * first), "50%" in the others.
 */
fun percent(value: Int): String = when (I18n.lang) {
    Lang.FR, Lang.DE, Lang.ES -> "$value %"
    Lang.TR -> "%$value"
    else -> "$value%"
}

/** The CLDR plural categories (https://cldr.unicode.org/index/cldr-spec/plural-rules). */
enum class Plural(val key: String) { ZERO("zero"), ONE("one"), TWO("two"), FEW("few"), MANY("many"), OTHER("other") }

/**
 * The CLDR plural category of the whole number [n] in the language [code] (its first subtag):
 * one/other for most, none at all in Japanese, Korean, Chinese, Indonesian; one/few/many in
 * Polish, Russian, Ukrainian; zero/one/two/few/many/other in Arabic.
 */
fun pluralOf(code: String, n: Long): Plural {
    val i = kotlin.math.abs(n)
    val m10 = i % 10
    val m100 = i % 100
    return when (code.substringBefore('-').lowercase(Locale.ROOT)) {
        "ja", "ko", "zh", "id", "in", "th", "vi" -> Plural.OTHER
        "fr", "pt", "hi" -> if (i <= 1) Plural.ONE else Plural.OTHER
        "pl" -> when {
            i == 1L -> Plural.ONE
            m10 in 2..4 && m100 !in 12..14 -> Plural.FEW
            else -> Plural.MANY
        }
        "ru", "uk" -> when {
            m10 == 1L && m100 != 11L -> Plural.ONE
            m10 in 2..4 && m100 !in 12..14 -> Plural.FEW
            else -> Plural.MANY
        }
        "cs", "sk" -> when (i) {
            1L -> Plural.ONE
            in 2..4 -> Plural.FEW
            else -> Plural.OTHER
        }
        "ar" -> when {
            i == 0L -> Plural.ZERO
            i == 1L -> Plural.ONE
            i == 2L -> Plural.TWO
            m100 in 3..10 -> Plural.FEW
            m100 in 11..99 -> Plural.MANY
            else -> Plural.OTHER
        }
        else -> if (i == 1L) Plural.ONE else Plural.OTHER
    }
}

/**
 * A count in words: [one] (its French key, "1 mesure") for exactly 1, else [other] ("%d mesures"),
 * filled with [args] ([n] when none). A language with more forms (Polish « 2 takty » / « 5 taktów »,
 * Arabic's dual…) gives them as "<other>|few", "<other>|many", "<other>|two"… in its `plurals.json`;
 * a form it does not give falls back to [other]. French, English and Dutch show exactly what the two
 * keys say.
 */
fun trCount(n: Int, one: String, other: String, vararg args: Any?): String {
    val values: Array<out Any?> = if (args.isEmpty()) arrayOf(n) else args
    if (n == 1) return if ('%' in one) tr(one, *values) else tr(one)
    val lang = I18n.lang
    val category = pluralOf(lang.code, n.toLong())
    if (lang != Lang.FR && category != Plural.OTHER && I18n.pseudo == null) {
        I18n.table(lang)["$other|${category.key}"]?.takeIf { it.isNotEmpty() }?.let { return String.format(lang.locale, it, *values) }
    }
    return tr(other, *values)
}

/**
 * A signed number ("+0,0 dB", "−8,2") kept in its order in a right-to-left interface: isolated left
 * to right (U+2066…U+2069), else the sign would land after the figures (« 8.2− »). As it is otherwise.
 */
fun ltrNumber(s: String): String = if (I18n.lang.rtl) "\u2066$s\u2069" else s

/** A shown text in capitals the language's way (Turkish « i » becomes « İ », not « I »). */
fun String.upperShown(): String = uppercase(I18n.lang.locale)

/**
 * A shown word put inside a sentence: in small letters the language's way (Turkish « I » becomes
 * « ı »), but as it is in German, whose nouns keep their capital (« Send Hall »).
 */
fun String.inSentence(): String = if (I18n.lang == Lang.DE) this else lowercase(I18n.lang.locale)
