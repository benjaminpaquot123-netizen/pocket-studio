package com.pocketstudio.app.ui

import com.pocketstudio.model.i18n.tr

/** "4 mes." / "1 mes.": a number of bars, short, singular for one (English says "1 bar"). */
fun barsShort(bars: Int): String = if (bars == 1) tr("1 mes.") else tr("%d mes.", bars)

/** "3 voix" / "1 voix" (English "1 voice", Dutch "1 stem"). */
fun voicesLabel(voices: Int): String = if (voices == 1) tr("1 voix") else tr("%d voix", voices)
