package com.pocketstudio.model

/**
 * The song bar by bar, for the tests: since chantier S3 a part can be cut around a one-bar fill or
 * crash (its drums from a scene of their own, [DrumArrangement]), so a section is read by its bars,
 * not by its parts.
 */
internal object SongBars {
    /** The scene each bar of the song plays (its part's scene). */
    fun Project.sceneOfBars(): List<Int> = song.flatMap { part -> List(partSteps(part) / stepsPerBar) { part.scene } }

    /** The scene each bar's drums play (a one-bar fill's or crash's own). */
    fun Project.drumSceneOfBars(): List<Int> = song.flatMap { part -> List(partSteps(part) / stepsPerBar) { part.sceneFor(SongPart.DRUMS) } }

    /** The drums each bar of the song plays: per bar, each lane's hits (step in the bar → hit). */
    fun Project.drumBars(): List<List<Map<Int, Hit>>> {
        val spb = stepsPerBar
        val out = ArrayList<List<Map<Int, Hit>>>()
        for (part in song) {
            val bars = partSteps(part) / spb
            val drums = allScenes.getOrNull(part.sceneFor(SongPart.DRUMS))?.drums
            val loop = ((drums?.steps ?: spb) / spb).coerceAtLeast(1)
            for (k in 0 until bars) {
                if (drums == null || !part.plays(SongPart.DRUMS)) {
                    out += List(DrumPattern.PAD_COUNT) { emptyMap() }
                    continue
                }
                val b = (part.fromBars(SongPart.DRUMS) + k) % loop
                out += drums.lanes.map { l -> l.hits.filterKeys { it in b * spb until (b + 1) * spb }.mapKeys { it.key - b * spb } }
            }
        }
        return out
    }

    /** A bar as heard: each hit's place, strength (to 0.05), roll, pitch and timing (to the ms). */
    fun key(bar: List<Map<Int, Hit>>): String = bar.mapIndexed { lane, hits ->
        hits.toSortedMap().entries.joinToString(",") { (st, h) -> "$lane:$st:${Math.round(h.velocity * 20)}:${h.repeats}:${h.pitch}:${Math.round(h.offset)}" }
    }.filter { it.isNotEmpty() }.joinToString("|")

    /** The one-bar drum scenes the assistant adds (fills and crashes), not sections of the song. */
    fun Scene.isDrumBar(): Boolean = label == PartIntents.FILL_LABEL || label == DrumArrangement.CRASH_LABEL
}
