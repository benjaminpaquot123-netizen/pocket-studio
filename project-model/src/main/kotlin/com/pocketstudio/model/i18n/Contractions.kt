package com.pocketstudio.model.i18n

/**
 * A preposition of the text and the article of a word put in later, joined as the language writes them
 * (audit ultra round 3, n° 38: « em a parte A » in Brazilian Portuguese is « na parte A »; Spanish
 * « de el estribillo » is « del estribillo »). Only where an argument starts with the article: a
 * catalogue's own words are left as written.
 */
internal object Contractions {
    private val PT = mapOf(
        "em o" to "no", "em a" to "na", "em os" to "nos", "em as" to "nas",
        "de o" to "do", "de a" to "da", "de os" to "dos", "de as" to "das",
        "por o" to "pelo", "por a" to "pela", "por os" to "pelos", "por as" to "pelas",
        "a o" to "ao", "a os" to "aos", "a a" to "à", "a as" to "às",
    )
    private val ES = mapOf("de el" to "del", "a el" to "al")

    fun join(lang: Lang, text: String, args: Array<out Any?>): String {
        val table = when (lang) { Lang.PT_BR -> PT; Lang.ES -> ES; else -> return text }
        var out = text
        for (a in args) {
            val arg = (a as? CharSequence)?.toString() ?: continue
            val article = arg.substringBefore(' ', "").takeIf { it.isNotEmpty() } ?: continue
            for ((pair, joined) in table) {
                val (prep, art) = pair.split(' ')
                if (!article.equals(art, ignoreCase = true)) continue
                val rest = arg.substring(article.length)
                for (p in listOf(prep, prep.replaceFirstChar { it.uppercaseChar() })) {
                    val j = if (p[0].isUpperCase()) joined.replaceFirstChar { it.uppercaseChar() } else joined
                    out = out.replace(Regex("(?<![\\p{L}])" + Regex.escape("$p $arg")), Regex.escapeReplacement(j + rest))
                }
            }
        }
        return out
    }
}
