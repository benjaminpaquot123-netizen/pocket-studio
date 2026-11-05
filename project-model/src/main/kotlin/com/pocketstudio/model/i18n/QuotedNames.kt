package com.pocketstudio.model.i18n

/**
 * Audit ultra round 3, n° 29 (third time: the tutorial said « Grosse caisse 1 » / « Basdrum 1 » where the
 * pad said « Grosse caisse studio » / « Studiobasdrum »): a text that names something between quotes, in any
 * language's quotes, says the name the screen shows. [names]: each name as the text says it → as shown.
 * One pass, longest first (« Kick 2 » is not « Kick » followed by « 2 »); in Korean the particle after a
 * renamed word follows it.
 */
object QuotedNames {
    private const val OPEN = "«“„「『\"‘"
    private const val CLOSE = "»”“」』\"’"
    private const val SPACE = "[\\s  ]?"

    fun rename(text: String, names: Map<String, String>, lang: Lang = I18n.lang): String {
        val changed = names.filter { (a, b) -> a != b && a.isNotBlank() }
        if (changed.isEmpty() || text.none { it in OPEN }) return text
        val alternatives = changed.keys.sortedByDescending { it.length }.joinToString("|") { Regex.escape(it) }
        val regex = Regex("(?<=[$OPEN]$SPACE)(?:$alternatives)(?=$SPACE[$CLOSE])")
        val out = regex.replace(text) { m -> changed.getValue(m.value) }
        return if (lang == Lang.KO && out != text) KoreanParticles.follow(out, changed.values.toSet()) else out
    }
}
