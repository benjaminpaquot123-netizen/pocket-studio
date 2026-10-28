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

    companion object {
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
        "remixroute",
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
        "Sub" to "Basse profonde", "Sub drop" to "Chute grave", "Quantifier" to "Remettre en rythme", "Acide" to "Basse acide",
        "Lead qui crie" to "Mélodie qui crie", "Lead chiptune" to "Mélodie chiptune", "Lead filtré" to "Mélodie filtrée",
        "Lead hyperpop" to "Mélodie hyperpop", "Lead / mélodie" to "Mélodie", "Lead 2 (sawtooth)" to "Mélodie 2 (dents de scie)",
    )

    fun t(fr: String): String {
        val text = if (everyday) EVERYDAY[fr] ?: fr else fr
        return if (lang == Lang.FR) text else table(lang)[text]?.takeIf { it.isNotEmpty() } ?: text
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
fun tr(fr: String, vararg args: Any?): String = String.format(I18n.lang.locale, I18n.t(fr), *args)

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
    if (lang != Lang.FR && category != Plural.OTHER) {
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
