package com.pocketstudio.model.i18n

import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

/**
 * Tour 5, lot « langues » (promises t5-Aa-4, t5-Bd-1, t5-Fc-ecran-1): the Arabic, Hindi and Chinese catalogs write the
 * trade's words in English, in Latin letters (« Snare », « Loop », « Bridge »…), in the middle of their own script. In the
 * Mode simple ([I18n.everyday]) each such word is said in the person's script: `i18n/<lang>/mots_simples.json` (a word as
 * the catalogs write it → the everyday word), applied after the translation, whole words only, the longest first. The
 * Mode pro keeps the trade's words. Names of the app's features (« Pad Live ») are never touched.
 */
internal object EverydayWords {
    private val json = Json { ignoreUnknownKeys = true }
    private val rules = ConcurrentHashMap<Lang, Rules>()
    // Feature and button names, and what one types in the sound search (« dark techno kick », «kick»): kept as written.
    private val PROTECTED = listOf("Drum & bass", "Pad Live", "Pad المباشر", "→ Pad", "drum loop", "warm pad", "Think break", "dark techno kick", "«kick»", "«echo»", "«reverb»")

    private class Rules(val regex: Regex?, val words: Map<String, String>)

    private fun rulesOf(lang: Lang): Rules = rules.getOrPut(lang) {
        val stream = EverydayWords::class.java.classLoader?.getResourceAsStream("i18n/${lang.code}/mots_simples.json")
        val words: Map<String, String> = stream?.bufferedReader(Charsets.UTF_8)?.use { json.decodeFromString<Map<String, String>>(it.readText()) }.orEmpty()
        if (words.isEmpty()) Rules(null, emptyMap())
        else {
            val alternatives = words.keys.sortedByDescending { it.length }.joinToString("|") { Regex.escape(it) }
            // Arabic: « الـBridge » (the article on a Latin word) becomes « الجسر ».
            val article = if (lang == Lang.AR) "(الـ)?" else "()"
            Rules(Regex("$article(?<![A-Za-z])($alternatives)(?![A-Za-z])", RegexOption.IGNORE_CASE), words.mapKeys { it.key.lowercase() })
        }
    }

    /** [text] with the trade's English words in [lang]'s everyday ones (as it is when the language has none). */
    fun apply(lang: Lang, text: String): String {
        val r = rulesOf(lang)
        val regex = r.regex ?: return text
        if (text.none { it in 'A'..'Z' || it in 'a'..'z' }) return text
        var masked = text
        PROTECTED.forEachIndexed { i, p -> if (p in masked) masked = masked.replace(p, "\u0000$i\u0000") }
        val replaced = regex.replace(masked) { m ->
            val word = r.words[m.groupValues[2].lowercase()] ?: return@replace m.value
            if (m.groupValues[1].isEmpty()) word else if (word.startsWith("ال")) word else "ال$word"
        }
        return PROTECTED.foldIndexed(replaced) { i, acc, p -> acc.replace("\u0000$i\u0000", p) }
    }
}
