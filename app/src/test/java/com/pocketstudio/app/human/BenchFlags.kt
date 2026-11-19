package com.pocketstudio.app.human

/**
 * The bench's flags (tour 9, banc A): one line per wait that ran out, per refusal of an amenée and per oracle missed,
 * always of the same shape, so that a seat that fails says where and why in its own journal:
 *
 *     #3 BENCH-FLAG where=amenee.wait want="les chapitres" saw="tuto_chapters@439,438" waited=120034 screen=tuto_panel
 *     #3 BENCH-VERDICT fail where=amenee.wait why="les chapitres n'est jamais venu"
 *
 * `grep BENCH-FLAG` lists them, `grep BENCH-VERDICT` gives the seat's end. Pure text: tested without a screen.
 */
internal object BenchFlags {
    const val FLAG = "BENCH-FLAG"
    const val VERDICT = "BENCH-VERDICT"

    /** A value on one line, in quotes when it holds a space, so that `key=value` pairs stay readable by a script. */
    fun value(s: String?): String {
        val one = (s ?: "-").replace(Regex("\\s+"), " ").trim().take(200).ifEmpty { "-" }
        return if (one.any { it == ' ' || it == '"' || it == '=' }) "\"" + one.replace("\"", "'") + "\"" else one
    }

    /** [where] the bench was (function.step), what it [want]ed, what it [saw], how long it [waitedMs], on which [screen]. */
    fun flag(turn: Int, where: String, want: String, saw: String?, waitedMs: Long, screen: String?): String =
        "#$turn $FLAG where=${value(where)} want=${value(want)} saw=${value(saw)} waited=$waitedMs screen=${value(screen)}"

    /** The seat's last word: ok, or fail with the place and the reason. */
    fun verdict(turn: Int, ok: Boolean, where: String, why: String? = null): String =
        "#$turn $VERDICT ${if (ok) "ok" else "fail"} where=${value(where)}" + (why?.let { " why=${value(it)}" } ?: "")

    /** A seat's end as [HumanTester.seatLoop] words it, to its verdict: (ok, where). */
    fun ofEnd(why: String): Pair<Boolean, String> = when {
        why == "fin" -> true to "seat.fin"
        why == "rejeu : oracle tenu" || why == "rejeu : reproduit" -> true to "seat.rejeu"
        why.startsWith("rejeu : oracle non tenu") -> false to "seat.oracle"
        why.startsWith("rejeu") -> false to "seat.rejeu"
        why.startsWith("rien ne change") -> false to "seat.rien-ne-change"
        // The budget of gestures is the planned end of a short anchored seat, not a failure.
        why.startsWith("abandon : budget") -> true to "seat.budget"
        why.startsWith("abandon") -> false to "seat.abandon"
        why.startsWith("banc") -> false to "seat.banc"
        why == "plantage" -> false to "seat.crash"
        else -> false to "seat.end"
    }

    /** The fields of a flag or verdict line (null for any other line): read back by the tests and by scripts. */
    fun parse(line: String): Map<String, String>? {
        val body = line.substringAfter(' ', "")
        val kind = body.substringBefore(' ')
        if (kind != FLAG && kind != VERDICT) return null
        val out = linkedMapOf("kind" to kind)
        if (kind == VERDICT) out["result"] = body.split(' ').getOrElse(1) { "" }
        Regex("(\\w+)=(\"[^\"]*\"|\\S+)").findAll(body).forEach { out[it.groupValues[1]] = it.groupValues[2].removeSurrounding("\"") }
        return out
    }
}
