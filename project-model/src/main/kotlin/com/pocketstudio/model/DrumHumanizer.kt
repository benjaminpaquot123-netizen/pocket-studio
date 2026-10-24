package com.pocketstudio.model

import com.pocketstudio.model.StarterWizard.Role
import com.pocketstudio.model.StarterWizard.Style
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * A drummer's hands on the assistant's beats (plan « son pro », chantier S3; docs/hits/):
 * the hats' strength in a 100/70/85/60 cycle, every hit a little different, the clap a few ms
 * ahead, the hats and the percussion a little behind, the snare laid back in lo-fi and boom bap,
 * ghost notes, the swing on the hats alone in drill, a fill at the end of each 4-bar phrase
 * with 3 or 4 different endings, a « B » version of the beat for the Drop's second scene.
 *
 * Everything comes from a fixed seed (the style and the mood, then the scene): the same choices
 * make the same song, note for note.
 */
object DrumHumanizer {
    /** How a style plays its beat ([feel]): timings in milliseconds (early < 0), strengths 0..1. */
    data class Feel(
        val hatLateMs: Float = 2f,
        val openHatLateMs: Float = 4f,
        /** The clap's lead: a hit between [clapEarlyMs] and [clapEarlyMs] + 2 (both early). */
        val clapEarlyMs: Float = -4f,
        val snareLateMs: Float = 0f,
        val percLateMs: Float = 3f,
        /** Ghost notes a bar on the snare (or the clap without a snare), on average. */
        val ghosts: Float = 0f,
        /** The swing on the hats alone (drill): the project plays straight, the hats' off 16ths come late. */
        val hatsOnlySwing: Boolean = false,
        /** The endings the 4-bar phrases pick from (3 or 4 of them). */
        val endings: List<Ending> = listOf(Ending.Roll, Ending.Stutter, Ending.Toms),
        /** How much each hit's strength varies (a fraction, each way) on the hats and the percussion. */
        val jitter: Float = 0.08f,
    )

    /** The end of a 4-bar phrase: what its last beat (its last two at the 8 and the 16) plays. */
    enum class Ending {
        /** The snare (or the clap) rolls crescendo, the last hits doubled. */
        Roll,
        /** Three hits that stop short: on the beat, on its « and », then a doubled last 16th. */
        Stutter,
        /** Down the toms, the last hit on the snare. */
        Toms,
        /** The kick drops out, the open hat breathes, a clap on the very last 16th. */
        Breath,
        /** The hats roll faster (a song's verse without snare, trap). */
        Hats,
        /** Claps on the 16ths of the last beat, rising (Energy Flash). */
        Claps,
    }

    private val CYCLE = floatArrayOf(1f, 0.7f, 0.85f, 0.6f)

    /** The feel of [style] (its family, then its own habits). */
    fun feel(style: Style): Feel {
        val base = when (style.family) {
            "House" -> Feel(hatLateMs = 3f, openHatLateMs = 6f, clapEarlyMs = -5f, percLateMs = 3f, ghosts = 0.5f,
                endings = listOf(Ending.Roll, Ending.Toms, Ending.Breath, Ending.Stutter))
            "Techno" -> Feel(hatLateMs = 2f, openHatLateMs = 4f, clapEarlyMs = -4f, percLateMs = 2f, ghosts = 0.5f, jitter = 0.06f,
                endings = listOf(Ending.Claps, Ending.Roll, Ending.Breath, Ending.Stutter))
            "Bass music" -> Feel(hatLateMs = 2f, openHatLateMs = 3f, clapEarlyMs = -4f, percLateMs = 2f, ghosts = 1.5f,
                endings = listOf(Ending.Roll, Ending.Stutter, Ending.Toms))
            "Hip-hop" -> Feel(hatLateMs = 3f, openHatLateMs = 5f, clapEarlyMs = -4f, percLateMs = 3f, ghosts = 0f,
                endings = listOf(Ending.Hats, Ending.Stutter, Ending.Breath))
            else -> Feel(hatLateMs = 2f, openHatLateMs = 4f, clapEarlyMs = -4f, percLateMs = 3f, ghosts = 0.5f,
                endings = listOf(Ending.Roll, Ending.Toms, Ending.Stutter))
        }
        return when (style.id) {
            // Laid back: the snare well behind the beat, the hats lazy, ghosts around the backbeat.
            "boom_bap" -> base.copy(snareLateMs = 10f, hatLateMs = 4f, ghosts = 1.5f, endings = listOf(Ending.Stutter, Ending.Toms, Ending.Breath))
            "lofi" -> base.copy(snareLateMs = 15f, hatLateMs = 6f, ghosts = 1.5f, jitter = 0.1f, endings = listOf(Ending.Stutter, Ending.Breath, Ending.Toms))
            "drill" -> base.copy(hatsOnlySwing = true, hatLateMs = 1f)
            "trap", "phonk" -> base.copy(hatLateMs = 1f)
            "uk_garage", "speed_garage" -> base.copy(hatLateMs = 4f, openHatLateMs = 7f, ghosts = 1f)
            "afrobeats", "reggaeton" -> base.copy(ghosts = 1f, endings = listOf(Ending.Toms, Ending.Stutter, Ending.Roll))
            "jungle", "dnb", "liquid_dnb" -> base.copy(ghosts = 2f)
            "gabber", "acidcore", "hard_techno", "industrial_techno" -> base.copy(ghosts = 0f, jitter = 0.05f, endings = listOf(Ending.Roll, Ending.Claps, Ending.Breath))
            else -> base
        }
    }

    /** The seed of [style]'s songs in [mood]: the same choices, the same beat. */
    fun seed(style: Style, mood: StarterWizard.Mood?): Long = ("${style.id}:${mood?.id ?: "-"}").hashCode().toLong() * GOLDEN

    /** 2^64 / φ, the usual multiplier that spreads seeds apart. */
    private const val GOLDEN = -0x61c8864680b583ebL

    /** A small deterministic random source (SplitMix64): the same seed, the same numbers on every phone. */
    class Dice(seed: Long) {
        private var state = seed
        fun next(): Long {
            state += GOLDEN
            var z = state
            z = (z xor (z ushr 30)) * -0x40a7b892e31b1a47L
            z = (z xor (z ushr 27)) * -0x6b2fb644ecceee15L
            return z xor (z ushr 31)
        }
        /** Uniform in [0, 1). */
        fun unit(): Float = ((next() ushr 40).toFloat() / (1L shl 24).toFloat())
        /** Uniform in [-1, 1). */
        fun signed(): Float = unit() * 2f - 1f
        fun pick(n: Int): Int = ((next() ushr 33) % n.coerceAtLeast(1)).toInt()
    }

    /** The ending scene number [scene] of a song takes: they turn through the style's endings. */
    fun endingOf(feel: Feel, seed: Long, scene: Int): Ending {
        val list = feel.endings.ifEmpty { listOf(Ending.Roll) }
        return list[Math.floorMod((Dice(seed).pick(list.size) + scene), list.size)]
    }

    /**
     * [lanes] ([steps] steps, a phrase of whole bars) played by hand: strengths in their cycle and
     * varied, each line at its distance from the grid, ghost notes (none from step [until] on: a
     * silent last beat stays silent). [keep]: lanes left as they are (a Montée's roll). [bpm]: for
     * the swing of the hats alone, [swing] the style's.
     */
    fun humanize(lanes: List<DrumLane>, steps: Int, feel: Feel, seed: Long, bpm: Double, swing: Double, keep: Set<Int> = emptySet(), until: Int = steps): List<DrumLane> {
        val dice = Dice(seed)
        val out = lanes.toMutableList()
        val stepMs = 60_000.0 / bpm.coerceAtLeast(20.0) / 4.0
        fun edit(lane: Int, f: (Int, Hit) -> Hit) {
            if (lane in keep || lane !in out.indices) return
            out[lane] = out[lane].copy(hits = out[lane].hits.mapValues { (step, h) -> f(step, h).sanitized() })
        }
        fun vary(v: Float, amount: Float) = (v * (1f + amount * dice.signed())).coerceIn(DrumPattern.MIN_VELOCITY, 1f)
        fun ms(x: Float) = (Math.round(x * 10f) / 10f).coerceIn(-Hit.MAX_OFFSET_MS, Hit.MAX_OFFSET_MS)

        // Ghost notes first (they get their timing with the line).
        val ghostLane = listOf(Role.Snare, Role.Clap).firstOrNull { out[it.lane].hits.isNotEmpty() }?.lane
        if (feel.ghosts > 0f && ghostLane != null && ghostLane !in keep) {
            val taken = { st: Int -> listOf(Role.Snare, Role.Clap).any { st in out[it.lane].hits } }
            val hits = out[ghostLane].hits.toMutableMap()
            for (bar in 0 until steps / 16) {
                val candidates = GHOST_STEPS.map { bar * 16 + it }.filter { it < until && !taken(it) }
                var n = feel.ghosts.toInt() + if (dice.unit() < feel.ghosts - feel.ghosts.toInt()) 1 else 0
                val free = candidates.toMutableList()
                while (n-- > 0 && free.isNotEmpty()) {
                    val st = free.removeAt(dice.pick(free.size))
                    hits[st] = Hit(GHOST_LOW + (GHOST_HIGH - GHOST_LOW) * dice.unit())
                }
            }
            out[ghostLane] = out[ghostLane].copy(hits = hits)
        }

        // The closed hats: the 100/70/85/60 cycle on their order in the bar (a pattern that has its own
        // accents keeps them, the cycle softened over it), each hit varied, a little late.
        if (Role.Hat.lane !in keep) {
            val hat = out[Role.Hat.lane]
            val byBar = hat.hits.keys.sorted().groupBy { it / 16 }
            val hits = HashMap<Int, Hit>()
            for ((_, inBar) in byBar) {
                val uniform = inBar.map { hat.hits.getValue(it).velocity }.distinct().size == 1
                inBar.forEachIndexed { ord, st ->
                    val h = hat.hits.getValue(st)
                    val c = CYCLE[ord % CYCLE.size].let { if (uniform) it else sqrt(it) }
                    val swingMs = if (feel.hatsOnlySwing && st % 2 == 1) swing * stepMs else 0.0
                    hits[st] = h.copy(
                        velocity = vary(h.velocity * c, feel.jitter * 0.6f),
                        offset = ms(feel.hatLateMs + 1.5f * dice.signed() + swingMs.toFloat()),
                    ).sanitized()
                }
            }
            out[Role.Hat.lane] = hat.copy(hits = hits)
        }
        edit(Role.OpenHat.lane) { _, h -> h.copy(velocity = vary(h.velocity, feel.jitter), offset = ms(feel.openHatLateMs + 1.5f * dice.signed())) }
        // The clap a few ms ahead (the « jack »), a little varied; the snare on the beat or laid back.
        edit(Role.Clap.lane) { _, h -> h.copy(velocity = vary(h.velocity, 0.04f), offset = ms(feel.clapEarlyMs + 2f * dice.unit())) }
        edit(Role.Snare.lane) { _, h ->
            val ghost = h.velocity < GHOST_HIGH + 0.01f
            h.copy(velocity = vary(h.velocity, if (ghost) 0.15f else 0.04f), offset = ms(feel.snareLateMs + (if (feel.snareLateMs > 0f) 2f else 1f) * dice.signed()))
        }
        for (r in listOf(Role.Perc, Role.Perc2)) edit(r.lane) { _, h -> h.copy(velocity = vary(h.velocity, feel.jitter), offset = ms(feel.percLateMs + 2f * dice.signed())) }
        edit(Role.Ride.lane) { _, h -> h.copy(velocity = vary(h.velocity, feel.jitter * 0.6f), offset = ms(2f + dice.signed())) }
        for (lane in TOM_LANES) edit(lane) { _, h -> h.copy(velocity = vary(h.velocity, 0.05f), offset = ms(1f + dice.signed())) }
        // The kick, the 808 and the rumble stay on the grid: the first kick of each bar at its strength, the others barely varied.
        edit(Role.Kick.lane) { st, h -> if (st % 16 == 0) h else h.copy(velocity = vary(h.velocity, 0.03f)) }
        return out
    }

    /** Where ghost notes fall in a bar: the 16ths around the backbeat (the « e » and « a » that lead into it). */
    private val GHOST_STEPS = listOf(3, 7, 10, 11, 14, 15)
    const val GHOST_LOW = 0.15f
    const val GHOST_HIGH = 0.28f

    /** The toms of the kit (low, mid, high), on their own pads. */
    val TOM_LANES = listOf(7, 8, 9)

    /**
     * The last bar of [lanes] ([steps] steps) ending on [ending]: over its last beat ([beats] = 1,
     * the end of a 4-bar phrase) or its last two ([beats] = 2, the bigger fill at the 8 and the 16).
     * Only lines already playing in the bar get the fill, except the kit's toms, which a fill may
     * always call on; in club music the kick stops under it.
     */
    fun ending(lanes: List<DrumLane>, steps: Int, ending: Ending, beats: Int = 1, sung: Boolean = false): List<DrumLane> {
        if (steps < 16) return lanes
        val out = lanes.toMutableList()
        val len = (beats.coerceIn(1, 2)) * 4
        val from = steps - len
        fun has(role: Role) = out[role.lane].hits.isNotEmpty()
        fun clear(lane: Int) { out[lane] = out[lane].copy(hits = out[lane].hits.filterKeys { it < from }) }
        fun put(lane: Int, step: Int, v: Float, repeats: Int = 1, ramp: Int = 0) {
            if (step !in from until steps) return
            out[lane] = out[lane].copy(hits = out[lane].hits + (step to Hit(v.coerceIn(0.2f, 1f), repeats, ramp = ramp)))
        }
        val snare = listOf(Role.Snare, Role.Clap).firstOrNull(::has)
        // What the bar can play: the hats' roll needs hats, the claps a clap, the others a snare or a clap.
        val asked = when {
            ending == Ending.Hats && !has(Role.Hat) -> Ending.Stutter
            ending == Ending.Claps && !has(Role.Clap) -> Ending.Roll
            else -> ending
        }
        val kind = if (asked != Ending.Hats && asked != Ending.Toms && snare == null) (if (has(Role.Hat)) Ending.Hats else Ending.Toms) else asked
        // The rise of the fill: from soft to strong over its length (a bigger fill climbs from lower).
        fun rise(i: Int, low: Float, high: Float) = low + (high - low) * i / (len - 1).coerceAtLeast(1)
        val lane = (snare ?: Role.Snare).lane
        when (kind) {
            Ending.Roll -> {
                clear(lane)
                for (i in 0 until len) {
                    val last = i >= len - 2
                    if (len > 4 && i < len / 2 && i % 2 == 1) continue
                    put(lane, from + i, rise(i, if (len > 4) 0.35f else 0.45f, 0.9f), if (last) 2 else 1, if (last) 1 else 0)
                }
            }
            Ending.Stutter -> {
                clear(lane)
                if (len > 4) { put(lane, from, 0.6f); put(lane, from + 3, 0.5f) }
                put(lane, steps - 4, 0.75f)
                put(lane, steps - 2, 0.85f)
                put(lane, steps - 1, 0.95f, 2, 1)
            }
            Ending.Toms -> {
                // The high and the low tom of the kit; the mid one only where the style's tom line plays (its pad).
                val mid = if (has(Role.Tom)) Role.Tom.lane else 9
                val end = if (snare != null) lane else 7
                for (l in setOf(9, 7, mid)) clear(l)
                if (snare != null) clear(lane)
                // High, mid, low in eighths (16ths over the last beat), the snare (or the low tom) doubled last.
                val order = listOf(9, 9, mid, mid, 7, 7, end, end).let { o -> if (len == 4) listOf(9, mid, 7, end) else o }
                for (i in 0 until len) {
                    val l = order[i * order.size / len]
                    if (len > 4 && i < len / 2 && i % 2 == 1) continue
                    put(l, from + i, rise(i, 0.6f, 0.95f), if (i == len - 1) 2 else 1, if (i == len - 1) 1 else 0)
                }
            }
            Ending.Breath -> {
                clear(Role.Kick.lane)
                if (snare != null) { clear(lane); put(lane, steps - 1, 0.9f) }
                if (has(Role.OpenHat)) out[Role.OpenHat.lane] = out[Role.OpenHat.lane].copy(hits = out[Role.OpenHat.lane].hits.filterKeys { it < from } + (steps - 3 to Hit(0.7f)))
                else if (has(Role.Hat)) out[Role.Hat.lane] = out[Role.Hat.lane].copy(hits = out[Role.Hat.lane].hits.filterKeys { it < from } + (steps - 3 to Hit(0.7f)))
                if (len > 4 && snare != null) { put(lane, from, 0.7f); put(lane, from + 3, 0.6f) }
            }
            Ending.Hats -> {
                clear(Role.Hat.lane)
                for (i in 0 until len) put(Role.Hat.lane, from + i, rise(i, 0.45f, 0.8f), if (i >= len - 2) (if (len > 4) 4 else 3) else if (i >= len - 4 && len > 4) 2 else 1, 0)
                if (snare != null) { clear(lane); put(lane, steps - 1, 0.9f, 2, 1) }
            }
            Ending.Claps -> {
                val clap = Role.Clap.lane
                clear(clap)
                for (i in 0 until len) if (len == 4 || i % 2 == 0 || i >= len - 4) put(clap, from + i, rise(i, 0.4f, 0.85f))
            }
        }
        // The kick stops for the fill in club music (when it plays elsewhere in the bar): the bar breathes.
        if (!sung && kind != Ending.Breath && out[Role.Kick.lane].hits.keys.any { it < from && it >= steps - 16 }) {
            clear(Role.Kick.lane)
        }
        return out
    }

    /**
     * The « B » version of a beat ([lanes] over [steps], the Drop's second scene, the second Refrain):
     * the same lines with a few hits moved or added, so that every 8 bars the beat answers itself:
     * a soft hat pickup before the 3, the percussion a 16th later in bars 2 and 4, an open hat on the
     * last off-beat of bar 2.
     */
    fun variantB(lanes: List<DrumLane>, steps: Int, seed: Long): List<DrumLane> {
        val dice = Dice(seed xor 0x5DEECE66DL)
        val out = lanes.toMutableList()
        val hat = out[Role.Hat.lane]
        if (hat.hits.isNotEmpty()) {
            val added = (0 until steps / 16).mapNotNull { bar -> (bar * 16 + 7).takeIf { it !in hat.hits && it < steps - 4 } }.associateWith { Hit(0.3f + 0.1f * dice.unit()) }
            out[Role.Hat.lane] = hat.copy(hits = hat.hits + added)
        }
        for (r in listOf(Role.Perc, Role.Perc2)) {
            val l = out[r.lane]
            if (l.hits.isEmpty()) continue
            out[r.lane] = l.copy(hits = l.hits.entries.associate { (st, h) ->
                val bar = st / 16
                val moved = if (bar % 2 == 1 && st < steps - 4 && (st + 1) !in l.hits) st + 1 else st
                moved to h
            })
        }
        val open = out[Role.OpenHat.lane]
        if (open.hits.isNotEmpty() && steps >= 32) out[Role.OpenHat.lane] = open.copy(hits = open.hits + (16 + 14 to Hit(0.6f)))
        return out
    }

    /** The population standard deviation of [values] (0 for fewer than two). */
    fun sigma(values: List<Float>): Float {
        if (values.size < 2) return 0f
        val mean = values.average()
        return sqrt(values.sumOf { (it - mean) * (it - mean) } / values.size).toFloat()
    }

    /** Whether two hits sound alike (the same strength within 0.05, the same roll and timing within 1 ms). */
    fun same(a: Hit?, b: Hit?): Boolean = when {
        a == null || b == null -> a == b
        else -> abs(a.velocity - b.velocity) < 0.05f && a.repeats == b.repeats && abs(a.offset - b.offset) < 1f && a.pitch == b.pitch
    }
}
