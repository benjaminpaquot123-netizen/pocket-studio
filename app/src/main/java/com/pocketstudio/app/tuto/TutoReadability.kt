package com.pocketstudio.app.tuto

import com.pocketstudio.model.i18n.Lang
import kotlin.math.floor

/**
 * Tour 7, P6 (docs/audits/ultra/tour-7-plan-p6-p7.md § 1): the tuto panel's readability rule, out of Compose.
 *  1. The instruction always comes first.
 *  2. In every state of the panel at least one whole line of it is readable.
 *  3. Cut, it ends with « … » and « Lire la suite » is shown.
 *  4. The other blocks fold before the words are cut: the examples, then the kit card, then « Pourquoi ? »; cut
 *     anyway, the kit card and « Pourquoi ? » come back (only the examples stay folded).
 * [layout] is what the panel does with measured heights; [budget] and [fits] are its projection for a text alone
 * (what the content tests measure every course with).
 */
object TutoReadability {
    /** OPEN: the words' room is [OPEN_ROOM] dp; GRID: cells, crosses or buttons to touch are in sight, [GRID_ROOM] dp. */
    enum class PanelState(val roomDp: Int) { OPEN(OPEN_ROOM), GRID(GRID_ROOM) }

    /** How many lines of the instruction the panel shows whole, and how many characters a line holds. */
    data class Budget(val maxLines: Int, val charsPerLine: Int)

    /** What stays in sight: the words' lines, whether they are cut (then « Lire la suite »), and the blocks kept. */
    data class PanelLayout(
        val wordsLines: Int, val cut: Boolean, val showMore: Boolean,
        val kitCard: Boolean, val why: Boolean, val examples: Boolean,
    )

    const val OPEN_ROOM = 240
    const val GRID_ROOM = 120
    /** The column's side margins (14 dp each). */
    const val MARGINS = 28
    /** « ▸ Pourquoi ? » pinned under the words (screen at least 480 dp high). */
    const val WHY_DP = 36
    /** « Lire la suite » under the words. */
    const val MORE_DP = 32
    /** bodyLarge: 16 sp, 24 sp line height. */
    const val LINE_SP = 24f

    /**
     * How much taller a line is in the language's script than in Latin (the fallback fonts: Noto CJK, Devanagari,
     * Arabic). Estimated (plan § 1.3); [TutoPanel] measures the real height with [witness] on the phone.
     */
    fun lineFactor(lang: Lang): Float = when (lang) {
        Lang.JA, Lang.ZH_CN, Lang.KO, Lang.AR -> 1.1f
        Lang.HI -> 1.2f
        else -> 1f
    }

    /** A character of the language's own script: a line measured on it is as high as a real line (not « Ag »). */
    fun witness(lang: Lang): String = when (lang) {
        Lang.JA -> "あ"
        Lang.ZH_CN -> "中"
        Lang.KO -> "한"
        Lang.HI -> "अ"
        Lang.AR -> "ع"
        else -> "Ag"
    }

    /**
     * The mean width (dp, at 16 sp and 100 %) of a character of the language, calibrated on the counts measured on the
     * phone at 320 and 360 dp, 100 / 130 / 200 % (plan § 1.3: fr, de, ja, zh-CN, ko); the others are estimates.
     */
    fun charDp(lang: Lang): Float = when (lang) {
        Lang.FR, Lang.ES, Lang.IT, Lang.PT_BR, Lang.ID, Lang.TR -> 7.28f
        Lang.EN -> 7.1f
        Lang.DE, Lang.NL, Lang.PL -> 7.5f
        Lang.JA -> 15.2f
        Lang.ZH_CN -> 14.1f
        Lang.KO -> 11.09f
        Lang.HI -> 7.6f
        Lang.AR -> 6.8f
    }

    /** The scripts written without spaces: a line breaks between any two characters. */
    private fun unspaced(lang: Lang) = lang == Lang.JA || lang == Lang.ZH_CN

    fun budget(state: PanelState, lang: Lang, fontScale: Float, widthDp: Int): Budget {
        val line = LINE_SP * fontScale * lineFactor(lang)
        val lines = floor((state.roomDp - WHY_DP) / line).toInt().coerceAtLeast(1)
        val chars = floor((widthDp - MARGINS) / (charDp(lang) * fontScale)).toInt().coerceAtLeast(1)
        return Budget(lines, chars)
    }

    /** How many lines [text] takes at [charsPerLine]: word by word (character by character in ja and zh-CN). */
    fun lines(text: String, charsPerLine: Int, lang: Lang): Int = text.split('\n').sumOf { para ->
        if (unspaced(lang)) maxOf(1, (para.length + charsPerLine - 1) / charsPerLine)
        else {
            var count = 1
            var used = 0
            for (word in para.split(' ').filter { it.isNotEmpty() }) {
                var w = word.length
                val need = if (used == 0) w else used + 1 + w
                if (need <= charsPerLine) { used = need; continue }
                if (used > 0) { count++; used = 0 }
                // A word longer than a line breaks inside it.
                while (w > charsPerLine) { count++; w -= charsPerLine }
                used = w
            }
            count
        }
    }

    fun fits(text: String, state: PanelState, lang: Lang, fontScale: Float, widthDp: Int): Boolean {
        val b = budget(state, lang, fontScale, widthDp)
        return lines(text, b.charsPerLine, lang) <= b.maxLines
    }

    /**
     * The panel's words and blocks in [roomDp]: the words take [textLines] lines of [lineHeightDp]; the examples, the
     * kit card and « Pourquoi ? » (0: none) fold in that order before the words are cut. Words too long even with every
     * block folded are cut anyway: folding the kit card and « Pourquoi ? » spared nothing then, so they come back
     * (tour 7 P6/P7 bloc 0b: the kit stays in sight, « Pourquoi ? » stays touchable), the examples stay folded, and the
     * words keep at least one whole line with « Lire la suite » ([moreDp]) under them.
     */
    fun layout(roomDp: Int, lineHeightDp: Float, textLines: Int, kitDp: Int, whyDp: Int, examplesDp: Int, moreDp: Int = MORE_DP): PanelLayout {
        val words = textLines * lineHeightDp
        var examples = examplesDp > 0
        var kit = kitDp > 0
        var why = whyDp > 0
        fun blocks() = (if (examples) examplesDp else 0) + (if (kit) kitDp else 0) + (if (why) whyDp else 0)
        if (words + blocks() > roomDp && examples) examples = false
        if (words + blocks() > roomDp && kit) kit = false
        if (words + blocks() > roomDp && why) why = false
        // A one-line instruction has nothing to cut away: its line stays whole (rule 2), the panel holds it.
        if (words <= roomDp || textLines <= 1) return PanelLayout(textLines, cut = false, showMore = false, kitCard = kit, why = why, examples = examples)
        val kept = (if (kitDp > 0) kitDp else 0) + (if (whyDp > 0) whyDp else 0)
        val shown = floor((roomDp - kept - moreDp) / lineHeightDp).toInt().coerceIn(1, textLines - 1)
        return PanelLayout(shown, cut = true, showMore = true, kitCard = kitDp > 0, why = whyDp > 0, examples = false)
    }
}
