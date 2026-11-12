package com.pocketstudio.model.i18n

/**
 * Tour 5, lot « langues » (promise t5-Bd-2): a right-to-left sentence reads from the right, so the arrow between the
 * steps of a sequence (« Intro → Couplet → Refrain ») points left there. An arrow that shows the music (the grids stay
 * left to right) keeps « → ».
 */
fun sequenceArrow(): String = if (I18n.lang.rtl) "←" else "→"

/** The comma between the items of a list: « ، » in Arabic, « , » elsewhere. */
fun listComma(): String = if (I18n.lang == Lang.AR) "، " else ", "
