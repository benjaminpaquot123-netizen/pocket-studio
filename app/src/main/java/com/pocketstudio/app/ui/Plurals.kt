package com.pocketstudio.app.ui

import com.pocketstudio.model.i18n.tr
import com.pocketstudio.model.i18n.trCount

/** "4 mes." / "1 mes.": a number of bars, short, singular for one (English says "1 bar"). */
fun barsShort(bars: Int): String = trCount(bars, "1 mes.", "%d mes.")

/** "3 voix" / "1 voix" (English "1 voice", Dutch "1 stem"). */
fun voicesLabel(voices: Int): String = trCount(voices, "1 voix", "%d voix")

/**
 * Audit ultra round 1, n° 23: a project's length in the same words on the home and in « Mes projets »
 * (« 6 mes. · 0:36 · 1 partie », or « Boucle de 4 mes. » before there is a song), from [com.pocketstudio.model.SongLength].
 */
fun lengthWords(length: com.pocketstudio.model.SongLength): String? = when {
    !length.loop -> listOf(barsShort(length.bars), "%d:%02d".format(length.seconds / 60, length.seconds % 60), trCount(length.parts, "1 partie", "%d parties")).joinToString(" · ")
    length.bars > 0 -> tr("Boucle de %s", barsShort(length.bars))
    else -> null
}
