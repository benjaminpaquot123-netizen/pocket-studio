package com.pocketstudio.app.human

/**
 * Tour 9 (banc-b, « drapeaux »): every gesture the bench does not play leaves one line in the seat's journal that says
 * WHERE it failed and WHY, on a single line one can grep (`grep '^BENCH-FLAG' journal.txt`):
 *
 * `BENCH-FLAG where=gesture:<action> n=<turn> target="…" found=<oui|non> covered_by="…" bounds=<l,t,r,b> why=<reason> …`
 *
 * The line does not start with « #n »: the journal's readers (seat-metrics, rejeu-defauts, fuse-reports) skip it, so
 * no count and no oracle changes. A key with nothing to say is « - ». The reasons are the bench's refusals, one each:
 * [TAG_MISSING], [NO_POINT], [MALFORMED], [OUT_OF_REACH], [COVERED].
 */
internal object BenchFlag {
    const val MARK = "BENCH-FLAG"

    /** « touche <étiquette> »: nothing on the screen carries that test tag. `screen=` says what was there instead. */
    const val TAG_MISSING = "etiquette-introuvable"

    /** The tag is on the screen, but no point of it is shown, within the thumb's reach and under the finger. */
    const val NO_POINT = "aucun-point-touchable"

    /** The gesture's words are not a gesture (« tape 10 20 »). */
    const val MALFORMED = "geste-mal-ecrit"

    /** A gesture in pixels beyond the thumb's reach, or off the screen. */
    const val OUT_OF_REACH = "hors-de-portee"

    /** An element drawn over the target takes the touch. */
    const val COVERED = "doigt-couvert"

    val REASONS = listOf(TAG_MISSING, NO_POINT, MALFORMED, OUT_OF_REACH, COVERED)

    /**
     * The flag of gesture [n] ([action]: its first word). [target]: the tag aimed at, or what is under the finger.
     * [found]: the target is on the screen. [bounds]: where it is drawn (« l,t,r,b », the screen's pixels). [at]: the
     * finger's point. [screen]: what the screen shows instead of a target that is not there. [more]: the hand, …
     */
    fun line(
        n: Int, action: String, target: String, found: Boolean, why: String,
        coveredBy: String? = null, bounds: String? = null, at: Pair<Float, Float>? = null, screen: String? = null, more: String? = null,
    ): String = buildString {
        append(MARK).append(" where=gesture:").append(word(action)).append(" n=").append(n)
        append(" target=").append(quoted(target))
        append(" found=").append(if (found) "oui" else "non")
        append(" covered_by=").append(coveredBy?.let(::quoted) ?: "-")
        append(" bounds=").append(bounds ?: "-")
        append(" why=").append(why)
        at?.let { (x, y) -> append(" at=").append(x.toInt()).append(',').append(y.toInt()) }
        more?.let { append(" more=").append(quoted(it)) }
        screen?.let { append(" screen=").append(quoted(it)) }
    }

    /** « l,t,r,b » in whole pixels. */
    fun bounds(left: Float, top: Float, right: Float, bottom: Float): String = "${left.toInt()},${top.toInt()},${right.toInt()},${bottom.toInt()}"

    /** The cover's name in [ScreenText.coverAt]'s sentence (« « cover » couvre « target » »). */
    fun coverName(sentence: String?): String? = sentence?.let { Regex("^« (.*?) » couvre « ").find(it)?.groupValues?.get(1) }

    /** The value of [key] in a flag [line] (the bench's own tests, and whoever reads a journal). */
    fun value(line: String, key: String): String? =
        Regex("(?:^| )" + Regex.escape(key) + "=(?:\"([^\"]*)\"|(\\S+))").find(line)?.let { m -> m.groupValues[1].ifEmpty { m.groupValues[2] } }

    /** The flags of a journal, in order. */
    fun of(journal: String): List<String> = journal.lineSequence().filter { it.startsWith("$MARK ") }.toList()

    private fun word(s: String) = s.trim().lowercase().replace(Regex("\\s+"), "-").ifEmpty { "-" }

    /** One line, no double quote inside: the value reads back whole. */
    private fun quoted(s: String) = "\"" + s.replace('"', '\'').replace(Regex("\\s+"), " ").trim() + "\""
}
