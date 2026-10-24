package com.pocketstudio.model.i18n

import kotlinx.serialization.json.Json
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/** The languages of the interface; French is the app's own language and the default. */
enum class Lang(val code: String, val nativeName: String) {
    FR("fr", "Français"),
    EN("en", "English"),
    NL("nl", "Nederlands");

    val locale: Locale get() = Locale.forLanguageTag(code)

    companion object {
        fun of(code: String?): Lang = entries.firstOrNull { it.code == code } ?: FR
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
        "write", "instruments", "learn", "nav", "bugreport",
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

/** "50 %" in French (a space before the sign), "50%" in English and Dutch. */
fun percent(value: Int): String = if (I18n.lang == Lang.FR) "$value %" else "$value%"
