package com.pocketstudio.model

/**
 * « Variation entre Drops » (plan « Le Guide », chantier G0): how much the Drops (the Refrains) of
 * a song differ from one another, bar by bar. Three Drops that replay the same 8-bar loop sound
 * like one Drop played three times (Benjamin's « Jump up DnB »: 96 of its 216 bars).
 *
 * A bar's print ([barPrints]) is what sounds in it: each track that plays (not muted in its part,
 * its song fader open), the notes it starts there (pitch, step, length in steps) and the drum hits
 * (pad, step, roll). Velocities and the drummer's few milliseconds are left out: a Drop played
 * again with other dice is the same Drop. Two Drops are compared bar against bar from their
 * starts ([difference]: the share of bars that differ over the shorter one); the song's variation
 * ([of]) is the smallest difference over every pair of Drops: 0 when two Drops are identical, 1
 * when no bar is the same. The assistant's songs keep it at [MIN] or more.
 */
object DropVariation {
    /** The smallest variation an assistant's song keeps between any two of its Drops (2 bars of each 8). */
    const val MIN = 0.25f

    /** The main sections, as the assistant names them (saved in French). */
    val LABELS = setOf("Drop", "Refrain")

    /** A song fader under this (0..1) is shut: its track is not heard (the second bass before its Drop). */
    private const val SHUT = 0.02f

    /** The Drops (Refrains) of [p]'s song, in order (a variant counted with its section). */
    fun drops(p: Project): List<StarterWizard.Section> {
        val labels = p.allScenes.map { it.label }
        return StarterWizard.songSections(p).filter { labels.getOrNull(it.scene) in LABELS }
    }

    /** What sounds in each bar of [p]'s song (see the class). */
    fun barPrints(p: Project): List<String> {
        val spb = p.stepsPerBar.coerceAtLeast(1)
        val ticksPerBar = spb * NoteClip.TICKS_PER_STEP
        val scenes = p.allScenes
        val keys = p.partKeys
        val curves = p.automation.filter { it.mode == AutomationLane.SONG && it.target.kind == AutoTarget.VOLUME && it.hasPoints }
            .associateBy { it.target.ch }
            .mapValues { (_, lane) -> lane to lane.songCurve(p.songBars * lane.pointsPerBar(p.beatsPerBar) + 1) }
        fun open(ch: Int, bar: Int): Boolean {
            val (lane, curve) = curves[ch] ?: return true
            return (curve.getOrNull(bar * lane.pointsPerBar(p.beatsPerBar)) ?: 1f) >= SHUT
        }
        val out = ArrayList<String>()
        for (part in p.song) {
            val bars = p.partSteps(part) / spb
            for (k in 0 until bars) {
                val bar = out.size
                val heard = ArrayList<String>()
                for (key in keys) {
                    if (!part.plays(key)) continue
                    val scene = scenes.getOrNull(part.sceneFor(key)) ?: continue
                    val from = part.fromBars(key) + k
                    when {
                        key == SongPart.DRUMS -> {
                            if (!open(0, bar)) continue
                            val loop = (scene.drums.steps / spb).coerceAtLeast(1)
                            val b = from % loop
                            val hits = scene.drums.lanes.flatMapIndexed { lane, l ->
                                l.hits.filterKeys { it in b * spb until (b + 1) * spb }.map { (st, h) -> "$lane:${st - b * spb}:${h.repeats}:${h.span}" }
                            }
                            if (hits.isNotEmpty()) heard += "D[" + hits.sorted().joinToString(",") + "]"
                        }
                        key == SongPart.ACID -> {
                            if (!open(AutoTarget.ACID_CHANNEL, bar)) continue
                            val a = scene.acid
                            val len = a.length.coerceAtLeast(1)
                            val steps = (0 until spb).mapNotNull { i ->
                                val s = a.steps.getOrNull((from * spb + i) % len)?.takeIf { it.gate } ?: return@mapNotNull null
                                "$i:${s.note + 12 * s.octave}"
                            }
                            if (steps.isNotEmpty()) heard += "A[" + steps.joinToString(",") + "]"
                        }
                        key.startsWith("inst:") -> {
                            val id = key.removePrefix("inst:").toIntOrNull() ?: continue
                            if (!open(InstrumentTrack.mixerChannel(id), bar)) continue
                            val clip = (if (id == 0) scene.synthNotes else scene.trackNotes[id]) ?: continue
                            val loop = (clip.steps / spb).coerceAtLeast(1)
                            val b = from % loop
                            val notes = clip.notes.filter { it.start in b * ticksPerBar until (b + 1) * ticksPerBar }
                                .map { "${it.pitch}@${(it.start - b * ticksPerBar) / NoteClip.TICKS_PER_STEP}/${(it.length + NoteClip.TICKS_PER_STEP / 2) / NoteClip.TICKS_PER_STEP}" }
                            if (notes.isNotEmpty()) heard += "$key[" + notes.sorted().joinToString(",") + "]"
                        }
                        else -> heard += key
                    }
                }
                out += heard.joinToString(" ")
            }
        }
        return out
    }

    /** The share of bars (0..1) that differ between Drops [a] and [b], bar against bar over the shorter one. */
    fun difference(prints: List<String>, a: StarterWizard.Section, b: StarterWizard.Section): Float {
        val n = minOf(a.bars, b.bars)
        if (n <= 0) return 1f
        val differ = (0 until n).count { prints.getOrNull(a.start + it) != prints.getOrNull(b.start + it) }
        return differ.toFloat() / n
    }

    /** Each pair of Drops (their order in the song, from 0) and how much they differ. */
    fun pairs(p: Project): List<Triple<Int, Int, Float>> {
        val drops = drops(p)
        if (drops.size < 2) return emptyList()
        val prints = barPrints(p)
        return drops.indices.flatMap { i -> (i + 1 until drops.size).map { j -> Triple(i, j, difference(prints, drops[i], drops[j])) } }
    }

    /** The song's variation between Drops (the smallest pairwise difference), null with fewer than two Drops. */
    fun of(p: Project): Float? = pairs(p).minOfOrNull { it.third }
}
