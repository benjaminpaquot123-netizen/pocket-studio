package com.pocketstudio.model

/**
 * A downloaded pack as a whole drum kit (the Rythme tab's « Kit ▾ »): a sound for each of the
 * 16 pads, of the kind the pad plays in the built-in kit (kick, snare, hats, toms…). A second
 * pad of the same kind takes the pack's next sound (« Kick 2 »). A pad whose kind the pack does
 * not have keeps the built-in voice.
 */
object PackKits {
    /** The kinds each pad takes, best first ([SoundPacks.KIND_LABELS] ids), in DrumKit voice order. */
    val LANE_KINDS: List<List<String>> = listOf(
        listOf("kick"), listOf("kick"), listOf("snare"), listOf("snare", "clap"),
        listOf("clap", "snare"), listOf("hat"), listOf("open_hat", "hat"), listOf("tom", "perc"),
        listOf("tom", "perc"), listOf("tom", "perc"), listOf("perc", "snare"), listOf("perc"),
        listOf("ride", "open_hat"), listOf("perc", "hat"), listOf("perc"), listOf("fx", "perc"),
    )

    /** The kind pad [lane] takes from [m], or null when the pack has none of its kinds. */
    private fun kindOf(m: PackManifest, lane: Int): String? = LANE_KINDS[lane].firstOrNull { m.ofKind(it).isNotEmpty() }

    /** The pack's sound for each pad that has one (pad, sound). */
    fun sounds(m: PackManifest): List<Pair<Int, PackSound>> = LANE_KINDS.indices.mapNotNull { lane ->
        val kind = kindOf(m, lane) ?: return@mapNotNull null
        val list = m.ofKind(kind)
        val nth = (0 until lane).count { kindOf(m, it) == kind }
        lane to list[nth % list.size]
    }

    /** A pack that can make a kit: it has kicks or snares. */
    fun hasDrums(m: PackManifest): Boolean = m.ofKind("kick").isNotEmpty() || m.ofKind("snare").isNotEmpty()
}
