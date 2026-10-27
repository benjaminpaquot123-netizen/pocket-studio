package com.pocketstudio.app.ui

import com.pocketstudio.model.i18n.tr
import com.pocketstudio.model.i18n.trCount

/** "4 mes." / "1 mes.": a number of bars, short, singular for one (English says "1 bar"). */
fun barsShort(bars: Int): String = trCount(bars, "1 mes.", "%d mes.")

/** "3 voix" / "1 voix" (English "1 voice", Dutch "1 stem"). */
fun voicesLabel(voices: Int): String = trCount(voices, "1 voix", "%d voix")
