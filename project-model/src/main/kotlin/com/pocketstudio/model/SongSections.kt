package com.pocketstudio.model

import kotlin.math.sqrt

/** Step 5c: a part of an imported song found by its loudness ([SongSections.detect]): its name (saved in French), its bars. */
data class SongSection(val label: String, val fromBar: Int, val bars: Int, val group: Int = 0)

/**
 * Step 5c (« Faire des briques avec ses parties »): the parts of a separated song, from each
 * stem's loudness bar by bar (bars on the song's own beat grid). The song is cut in phrases of
 * four bars; phrases that sound alike in a row make a part (a change of energy is a new part); parts
 * that sound alike are one group (a chorus coming back), named alike. The loudest group with
 * the voice is « Refrain » (« Drop » without it), a quiet start « Intro », a quiet end « Fin », the
 * rest « Couplet » (with the voice), « Break » (no drums) or « Groove »; a second group taking a
 * name already given: « Pont », then numbered.
 */
object SongSections {
    private const val PHRASE = 4
    private const val SAME_PART = 0.22
    private const val SAME_GROUP = 0.2
    /** The bricks of a song at most (the Live's grid shows 4 to 16; some room is kept for the user's). */
    const val MAX = 8
    private const val DRUMS = 0
    private const val VOICE = 3

    /** [energy]: one array per stem (drums, bass, other, vocals), the loudness of each bar. */
    fun detect(energy: List<FloatArray>, maxParts: Int = MAX): List<SongSection> {
        val bars = energy.minOfOrNull { it.size } ?: 0
        if (energy.size != Remix.STEMS || bars <= 0) return emptyList()
        // Each stem against its own loudest bar: what changes, not how loud the mix is.
        val norm = energy.map { e -> val top = e.take(bars).maxOrNull()?.takeIf { it > 1e-6f } ?: 1f; FloatArray(bars) { e[it] / top } }
        // Phrases of four bars (a last short one joins the one before when under two bars).
        val starts = (0 until bars step PHRASE).toMutableList()
        if (starts.size > 1 && bars - starts.last() < PHRASE / 2) starts.removeAt(starts.lastIndex)
        val blocks = starts.mapIndexed { i, s -> s until (starts.getOrNull(i + 1) ?: bars) }
        fun mean(r: IntRange) = FloatArray(Remix.STEMS) { k -> r.sumOf { norm[k][it].toDouble() }.toFloat() / r.count() }
        // Neighbours alike: one part.
        val parts = mutableListOf<IntRange>()
        for (b in blocks) {
            val last = parts.lastOrNull()
            if (last != null && distance(mean(last), mean(b)) < SAME_PART) parts[parts.lastIndex] = last.first..b.last
            else parts.add(b)
        }
        // Too many: the two neighbours most alike join, until it fits.
        while (parts.size > maxParts.coerceAtLeast(1)) {
            val i = (0 until parts.lastIndex).minBy { distance(mean(parts[it]), mean(parts[it + 1])) }
            parts[i] = parts[i].first..parts[i + 1].last
            parts.removeAt(i + 1)
        }
        val features = parts.map(::mean)
        // Groups: a part alike an earlier one comes back (a chorus again).
        val group = IntArray(parts.size)
        var groups = 0
        features.forEachIndexed { i, f ->
            group[i] = (0 until i).firstOrNull { distance(features[it], f) < SAME_GROUP }?.let { group[it] } ?: groups++
        }
        fun energyOf(f: FloatArray) = f.sum()
        val groupEnergy = DoubleArray(groups) { g -> features.indices.filter { group[it] == g }.map { energyOf(features[it]).toDouble() }.average() }
        val loudest = groupEnergy.indices.maxBy { groupEnergy[it] }
        val top = groupEnergy[loudest]
        val names = arrayOfNulls<String>(groups)
        val used = mutableMapOf<String, Int>()
        fun name(base: String): String {
            val n = used.merge(base, 1, Int::plus)!!
            return when {
                n == 1 -> base
                n == 2 && base == "Couplet" -> "Pont".also { used.merge(it, 1, Int::plus) }
                else -> "$base $n"
            }
        }
        val labels = parts.indices.map { i ->
            val f = features[i]
            val g = group[i]
            val quiet = energyOf(f) < 0.5 * top && g != loudest
            when {
                parts.size > 1 && i == 0 && quiet -> "Intro"
                parts.size > 2 && i == parts.lastIndex && quiet -> "Fin"
                else -> names[g] ?: name(
                    when {
                        g == loudest -> if (f[VOICE] > 0.3f) "Refrain" else "Drop"
                        f[VOICE] > 0.3f -> "Couplet"
                        f[DRUMS] < 0.3f -> "Break"
                        else -> "Groove"
                    },
                ).also { names[g] = it }
            }
        }
        return parts.mapIndexed { i, r -> SongSection(labels[i], r.first, r.count(), group[i]) }
    }

    private fun distance(a: FloatArray, b: FloatArray): Double =
        sqrt(a.indices.sumOf { ((a[it] - b[it]) * (a[it] - b[it])).toDouble() } / a.size)
}

/**
 * Step 5c: the song's parts made bricks: one scene per name (a chorus coming back plays the same
 * brick), named after it (« Intro », « Refrain »), and the song's structure made of them, as long
 * as each part, from brique A on. Only for a song not written yet (no part, or the one part
 * [withRemixPlaced] made): a song of the user's own is never rewritten (null then). Scenes already
 * holding music are left alone: the bricks take the free ones.
 */
fun Project.withSongSections(sections: List<SongSection>): Project? {
    if (sections.isEmpty()) return null
    val placedOnly = song.size == 1 && song[0].bars != null && song[0].scene == 0 && remix.placed
    if (song.isNotEmpty() && !placedOnly) return null
    val inSong = song.map { it.scene }.toSet() - (if (placedOnly) setOf(0) else emptySet())
    val scenes = allScenes.toMutableList()
    val free = (0 until Scene.MAX).filter { it !in inSong && (scenes.getOrNull(it)?.isEmpty ?: true) && (scenes.getOrNull(it)?.label.isNullOrEmpty()) }
    val labels = sections.map { it.label }.distinct()
    if (labels.size > free.size) return null
    val sceneOf = labels.zip(free).toMap()
    while (scenes.size <= (sceneOf.values.maxOrNull() ?: 0)) scenes.add(Scene(name = Scene.NAMES[scenes.size]))
    sceneOf.forEach { (label, i) -> scenes[i] = scenes[i].copy(label = label) }
    // The song's first bar on brique A's first bar (the song comes in at its first part).
    val parts = sections.map { SongPart(scene = sceneOf.getValue(it.label), bars = it.bars.coerceIn(1, SongPart.MAX_BARS)) }
    return copy(scenes = scenes, song = parts, remix = remix.copy(placed = true, atStep = 0, startBar = sections.first().fromBar)).sanitized()
}

/** Step 5c: where an imported song goes (« Mettre Boys dans mon projet »). */
enum class SongPlace { Whole, Passage, VoiceOnly }

/**
 * Step 5c: the choices of « Mettre Boys dans mon projet », made on one screen: the project takes
 * the song's tempo ([takeTempo]) or the song is stretched to the project's; the harmony panel set to
 * the song's key ([setKey]); the whole song, a passage ([fromBar], [bars], of the song's bars) or
 * its voice alone; its parts made bricks ([bricks]).
 */
data class SongImport(
    val takeTempo: Boolean,
    val setKey: Boolean,
    val place: SongPlace = SongPlace.Whole,
    val fromBar: Int = 0,
    val bars: Int = 8,
    val bricks: Boolean = false,
) {
    companion object {
        /**
         * The safe choices for [p] (step 4e's rule): a project with neither music nor a tempo set
         * by a loop takes the song's tempo and key; one with its own keeps them. Bricks when the
         * song's structure is still free.
         */
        fun defaults(p: Project): SongImport {
            val empty = !p.hasMusicOfItsOwn()
            return SongImport(takeTempo = empty && p.tempoLead == null, setKey = empty, bricks = p.song.isEmpty())
        }
    }
}

/** Step 5c: a passage of the song: every stem heard from bar [SongImport.fromBar] for [SongImport.bars] bars only (edits, the files kept whole). */
fun Remix.onlyPassage(fromBar: Int, bars: Int): Remix {
    val bar = if (songBpm > 0) 240.0 / songBpm else return this
    val t0 = firstBeat + fromBar.coerceAtLeast(0) * bar
    val len = bars.coerceAtLeast(1) * bar
    return copy(startBar = fromBar.coerceIn(0, Remix.MAX_START_BAR), stems = stems.map { it.copy(clips = listOf(StemClip(t0, t0, len))) })
}
