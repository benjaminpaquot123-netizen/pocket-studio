package com.pocketstudio.model

/**
 * Putting a separated song's voice into the project (remix lot 1, R2): which pads may take a cut of
 * it without covering a sound the beat uses, the names those cuts get, the sung phrases found
 * between the voice's silences, and the places of the song the voice can come in on.
 */
object RemixVoice {
    /**
     * Pad [lane] is free for a cut of the song: no sound of its own, no studio voice (an 808), and
     * no hit in any scene. A kit's sound nobody plays (a cowbell, a conga) may go.
     */
    fun padIsFree(p: Project, lane: Int): Boolean {
        val own = p.drums.lanes.getOrNull(lane) ?: return false
        if (own.sample != null || own.hits.isNotEmpty() || p.padVoices.containsKey(lane)) return false
        return p.scenes.withIndex().none { (i, s) -> i != p.scene && s.drums.lanes.getOrNull(lane)?.hits?.isNotEmpty() == true }
    }

    /** The free pads ([padIsFree]), in pad order. */
    fun freePads(p: Project): List<Int> = (0 until DrumPattern.PAD_COUNT).filter { padIsFree(p, it) }

    /**
     * The pad a cut goes to: the selected one when it is free, else the first free one; null when
     * every pad is used (the screen asks before covering one).
     */
    fun padFor(p: Project, selected: Int): Int? = selected.takeIf { padIsFree(p, it) } ?: freePads(p).firstOrNull()

    /**
     * The name of the next cut of the stem [stem] (its French label, « Voix »): « Voix 1 », « Voix 2 »…
     * one more than the highest the pads have. Saved in French, shown translated ([DefaultNames.shown]).
     */
    fun cutName(p: Project, stem: String): String = "$stem ${cutNumbers(p, stem).maxOrNull()?.plus(1) ?: 1}"

    /** The numbers of the cuts of [stem] on the pads (« Voix 3 » → 3). */
    private fun cutNumbers(p: Project, stem: String): List<Int> {
        val re = Regex("^${Regex.escape(stem)} (\\d{1,4})$")
        return p.drums.lanes.mapNotNull { l -> l.sample?.name?.let { re.matchEntire(it)?.groupValues?.get(1)?.toIntOrNull() } }
    }

    /** The parts of a separated song a pad's sound can be cut from (their French labels). */
    val STEMS = listOf("Batterie", "Basse", "Autres", "Voix")

    /** A pad's sound cut from a separated song: « Voix 2 » (R2), or « Voix · <song> » as older versions named it. */
    fun isCut(name: String): Boolean =
        STEMS.any { s -> name.startsWith("$s · ") || Regex("^${Regex.escape(s)} \\d{1,4}$").matches(name) }

    /** A short fade on each end of a cut (seconds): no click where the voice is cut. */
    const val CUT_ATTACK = 0.005f
    const val CUT_RELEASE = 0.03f

    /** A place the song can come in on: the start of song part [index], named [name], song step [step]. */
    data class Place(val index: Int, val name: String, val step: Int)

    /**
     * Where the song's voice can come in: the start of the song, then the first part of each name
     * that follows (« Montée », « Drop »…), in song order. [nameOf]: a part's name (its scene's).
     */
    fun places(p: Project, nameOf: (Int) -> String): List<Place> {
        val seen = HashSet<String>()
        val out = ArrayList<Place>()
        p.song.forEachIndexed { i, part ->
            val name = nameOf(part.scene)
            if (seen.add(name)) out += Place(i, name, p.songStepOf(i))
        }
        return out
    }

    /** The song part playing on song step [step] (its index), or -1 past the song's end. */
    fun partAt(p: Project, step: Int): Int {
        var at = 0
        p.song.forEachIndexed { i, part ->
            val steps = p.partSteps(part)
            if (step < at + steps) return i
            at += steps
        }
        return -1
    }

    /**
     * The sung phrases of a voice from its loudness: [peaks] the loudest sample of each slice of
     * [sliceFrames] frames. A phrase is where the voice is louder than a tenth of its loudest,
     * apart from the next one by at least [minGapSlices] of quiet; phrases shorter than
     * [minSlices] are breaths or bleed and go. Each is widened by [padBefore] / [padAfter] slices
     * (the consonant before, the tail after) without reaching into its neighbours. At most [max],
     * from the first one. Slices are returned as (first, end exclusive).
     */
    fun phrases(
        peaks: FloatArray,
        minGapSlices: Int,
        minSlices: Int,
        padBefore: Int = 0,
        padAfter: Int = 0,
        max: Int = 8,
    ): List<Pair<Int, Int>> {
        val loudest = peaks.maxOrNull() ?: return emptyList()
        if (!(loudest > 0f)) return emptyList()
        val threshold = maxOf(loudest * 0.1f, 1e-3f)
        val raw = ArrayList<IntArray>()
        var i = 0
        while (i < peaks.size) {
            if (peaks[i] < threshold) { i++; continue }
            val start = i
            var end = i + 1
            var quiet = 0
            var j = i + 1
            while (j < peaks.size) {
                if (peaks[j] >= threshold) { end = j + 1; quiet = 0 } else if (++quiet >= minGapSlices) break
                j++
            }
            raw += intArrayOf(start, end)
            i = maxOf(end, j)
        }
        val kept = raw.filter { it[1] - it[0] >= minSlices }.take(max)
        return kept.mapIndexed { k, r ->
            val low = if (k > 0) kept[k - 1][1] else 0
            val high = if (k + 1 < kept.size) kept[k + 1][0] else peaks.size
            maxOf(low, r[0] - padBefore) to minOf(high, r[1] + padAfter)
        }
    }
}
