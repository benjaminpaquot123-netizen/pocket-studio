package com.pocketstudio.app

import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang

/**
 * Audit §3, « pseudo-traduction »: a 16th test-only language that finds overflows before the real
 * ones do. Each French text is shown accented (« Ŕéçöŕđéŕ »), 40 % longer, between brackets (a cut
 * edge shows), and in [rtl] forced right to left; format specifiers (`%s`, `%1$d`, `%%`) and line
 * breaks stay as they are, so every `tr(fr, args)` still formats. Tests only: [I18n.pseudo].
 */
object PseudoLocale {
    private val SPEC = Regex("%(\\d+\\$)?[-#+ 0,(]*\\d*(\\.\\d+)?[a-zA-Z%]")
    private const val FROM = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private const val TO = "åƀçđéƒĝĥîĵķļɱñöþǫŕšţüṽŵẋýžÅƁÇĐÉƑĜĤÎĴĶĻṀÑÖÞǪŔŠŢÜṼŴẊÝŽ"

    /** Right-to-left embedding and its end. */
    const val RLE = '‫'
    const val PDF = '‬'

    /** [fr] as the pseudo-language shows it. */
    fun of(fr: String, rtl: Boolean = false): String {
        if (fr.isEmpty()) return fr
        val out = StringBuilder(fr.length * 2)
        var i = 0
        for (m in SPEC.findAll(fr)) {
            accent(fr, i, m.range.first, out)
            out.append(m.value)
            i = m.range.last + 1
        }
        accent(fr, i, fr.length, out)
        // 40 % longer (at least one more character), the way German or Dutch often are.
        val pad = maxOf(1, (fr.length * 2 + 4) / 5)
        val body = "[" + out + " " + "·".repeat(pad) + "]"
        return if (rtl) "$RLE$body$PDF" else body
    }

    private fun accent(s: String, from: Int, to: Int, out: StringBuilder) {
        for (k in from until to) {
            val c = s[k]
            val j = FROM.indexOf(c)
            out.append(if (j >= 0) TO[j] else c)
        }
    }

    /** Turns the pseudo-language on ([rtl]: with a right-to-left layout and formats, [Lang.AR]'s). */
    fun on(rtl: Boolean = false) {
        I18n.lang = if (rtl) Lang.AR else Lang.FR
        I18n.pseudo = { of(it, rtl) }
    }

    fun off() {
        I18n.pseudo = null
        I18n.lang = Lang.FR
    }
}
