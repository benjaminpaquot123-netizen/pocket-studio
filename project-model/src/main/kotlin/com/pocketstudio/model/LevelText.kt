package com.pocketstudio.model

import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.ltrNumber
import com.pocketstudio.model.i18n.tr

/**
 * Tour 5, « un mot par chose » (Volume): the Mode simple shows no « dB ». A shown text with levels in it
 * (« Seuil -16 dB », « +3,0 dB vers 120 Hz ») says them as the Mix simple does: a whole signed number
 * (« +3 », « −16 ») and « Normal » for nothing; the figures of the interface's language are read as they are written.
 * Pro keeps its text as it is.
 */
fun withoutDecibels(text: String): String = DECIBELS.replace(text) { m ->
    val sign = m.groupValues[1]
    val digits = m.groupValues[2].map { c -> if (c.isDigit()) Character.digit(c, 10).digitToChar() else '.' }.joinToString("")
    val whole = Math.round(digits.toDoubleOrNull() ?: return@replace m.value)
    if (whole == 0L) tr("Normal")
    else ltrNumber((if (sign == "+" || sign.isEmpty()) "+" else "−") + String.format(I18n.lang.locale, "%d", whole))
}

private val DECIBELS = Regex("""([+\-−]?)\s?(\p{Nd}+(?:[.,٫]\p{Nd}+)?)\s?dB(?![\p{L}])""")
