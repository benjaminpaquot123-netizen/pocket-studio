package com.pocketstudio.model

/**
 * The live pads ready to play (Benjamin's list n° 12): opened empty, the grid fills with these
 * eight sounds, four loops that go together and four party hits. The loops are rendered from the
 * [STYLE] recipe at the project's tempo (its kick, its 303 line, its bass), the hits come from
 * [PartySounds] and the built-in kit.
 */
object LiveStarter {
    /** The style the loops are rendered from: four on the floor, a 303 line, a rolling bass. */
    const val STYLE = "acid_techno"

    enum class Source { KickLoop, Clap, AcidLoop, BassLoop, Party }

    /** One pad: its [name] (French, shown translated), what makes it, played [oneShot] or looped every [beats]. */
    data class Pad(val name: String, val source: Source, val oneShot: Boolean, val beats: Int, val party: String? = null, val gain: Float = 1f)

    val PADS: List<Pad> = listOf(
        Pad("Boucle de kick", Source.KickLoop, oneShot = false, beats = 4),
        Pad("Clap", Source.Clap, oneShot = true, beats = 1),
        Pad("Boucle Acid 303", Source.AcidLoop, oneShot = false, beats = 8),
        Pad("Boucle de basse", Source.BassLoop, oneShot = false, beats = 8),
        Pad("Klaxon", Source.Party, oneShot = true, beats = 1, party = "air_horn", gain = 0.8f),
        Pad("Laser", Source.Party, oneShot = true, beats = 1, party = "laser", gain = 0.8f),
        Pad("Sirène", Source.Party, oneShot = true, beats = 1, party = "siren_dub", gain = 0.7f),
        Pad("Cri « Go go go! »", Source.Party, oneShot = true, beats = 1, party = "voice_go_go_go"),
    )

    /**
     * Whether the starter set goes on [p]'s live pads: nothing on them yet, never offered to this
     * project before, and no song of the assistant about to fill them ([Project.livePadsPending]).
     */
    fun wanted(p: Project): Boolean = !p.live.starterOffered && p.live.used == 0 && !p.livePadsPending

    /** A pad's sound as rendered: its file in the project's samples, and how many beats its loop really lasts. */
    data class Made(val file: String, val beats: Int)

    /**
     * [pads] with the set's clips ([made]: pad index → its sound), marked as offered (whatever
     * could be made: a sound that failed leaves its pad empty, the set is not offered again).
     * A pad the user filled meanwhile keeps its sound.
     */
    fun applied(pads: LivePads, made: Map<Int, Made>): LivePads {
        var out = pads.copy(starterOffered = true, size = maxOf(pads.size, PADS.size))
        PADS.forEachIndexed { i, pad ->
            val m = made[i] ?: return@forEachIndexed
            if (out.clips.getOrNull(i) == null) out = out.with(i, LiveClip(m.file, pad.name, if (pad.oneShot) 1 else m.beats, pad.oneShot, pad.gain))
        }
        return out.sanitized()
    }
}
