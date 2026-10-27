package com.pocketstudio.model.guide

import com.pocketstudio.model.AutoTarget
import com.pocketstudio.model.AutomationLane
import com.pocketstudio.model.EffectType
import com.pocketstudio.model.Mixer
import com.pocketstudio.model.PartIntents
import com.pocketstudio.model.Project
import com.pocketstudio.model.SongPart
import com.pocketstudio.model.breakAfter
import com.pocketstudio.model.fillAtEnd
import com.pocketstudio.model.isFillPart
import com.pocketstudio.model.withSongLane
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.pow

/**
 * G4: each track's place in the mix (docs/PLAN-GUIDE.md stage 6), read from the app's measures
 * ([GuideMeasures.tracks]: each track heard alone in the first peak, the master's chain and target
 * left out): where its role sits against the kick, how far it is from there, and the checks' limits.
 * Starting points, then listening: never laws.
 */
object GuideLevels {
    /** Within this of its place (LU), a track is where it should be. */
    const val TOLERANCE_DB = 3f

    /** « Équilibrer pour moi » moves a fader by this at most, each time. */
    const val MAX_MOVE_DB = 6f

    /** Past this loss folded to mono (LU), something vanishes (a wide sound out of phase). */
    const val MONO_MAX_DB = 2f

    /** Under this (dB over 300 Hz against its own energy, docs/son/BANC.md B2), a phone's speaker loses the bass. */
    const val PHONE_MIN_DB = -16f

    /** Where each role sits against the kick, heard alone (LU): the bass just under it, the chords far behind. */
    val PLACE: Map<String, Float> = mapOf(
        GuideRoles.BASS to -1f, GuideRoles.SUB to -3f, GuideRoles.BASS2 to -2f, GuideRoles.SUB_DROP to -4f, GuideRoles.ACID to -3f,
        GuideRoles.PAD to -9f, GuideRoles.LEAD to -4f, GuideRoles.COUNTER to -7f, GuideRoles.VOCAL to -2f, GuideRoles.RISER to -10f,
        GuideRoles.AUDIO to -3f,
    )

    /**
     * How far each measured track is from its place (dB, + too loud), the kick as the reference; empty
     * without the kick's measure. Only the tracks a fader moves alone: instruments, the 303, audio tracks.
     */
    fun offsets(facts: GuideFacts, m: GuideMeasures): Map<String, Float> {
        val kick = facts.playing(GuideRoles.KICK).firstNotNullOfOrNull { m.tracks[it.key] } ?: return emptyMap()
        return facts.tracks.filter { it.key.startsWith("inst:") || it.key == GuideTrack.ACID || it.key.startsWith("audio:") }
            .mapNotNull { t -> val place = PLACE[t.role] ?: return@mapNotNull null; m.tracks[t.key]?.let { t.key to (it - kick - place) } }
            .toMap()
    }

    /** A fader's position as dB (0.8 = 0 dB, the gain its square). */
    fun faderDb(volume: Float): Float = 40f * log10((volume / 0.8f).coerceAtLeast(1e-4f))

    fun faderOf(db: Float): Float = (0.8f * 10f.pow(db / 40f)).coerceIn(0f, 1f)
}

/** G4: what the cards of the stages 4 to 6 do to a song (the arrangement, the movement, the levels). Pure. */
internal object GuideMoves {
    // --- stage 4: the arrangement by subtraction --------------------------------------------------------

    /** The roles « on enlève » first from a lighter part: the basses and the melody. */
    private val TAKEN_OUT = setOf(GuideRoles.BASS, GuideRoles.SUB, GuideRoles.BASS2, GuideRoles.SUB_DROP, GuideRoles.ACID, GuideRoles.LEAD, GuideRoles.COUNTER)

    /**
     * [name] "break": a Break made from the first peak (its scene without the kicks, the basses out,
     * [PartIntents.LONG_BARS] bars) right after it. "lighten": the intro, the Montées and the breaks
     * that play as much as the peak lose its basses and its melody (those parts only).
     */
    fun subtract(p: Project, name: String): Project {
        val song = GuideSong(p)
        val peaks = song.sectionsOf(GuideSection.Kind.PEAK)
        val first = peaks.firstOrNull() ?: return p
        return when (name) {
            "break" -> p.breakAfter(first.parts.last(), PartIntents.LONG_BARS)
            "lighten" -> {
                val full = peaks.maxOf { song.fullness(it) }
                val lighter = song.sections.filter { it.kind in setOf(GuideSection.Kind.INTRO, GuideSection.Kind.BUILD, GuideSection.Kind.BREAK) }
                val heavy = lighter.filter { song.fullness(it) >= full }.ifEmpty { lighter.filter { it.kind == GuideSection.Kind.INTRO } }
                val keys = song.tracks.filter { it.role in TAKEN_OUT }.mapNotNull { GuideTrack.partKey(it.key) }.toSet()
                val parts = heavy.flatMap { it.parts }.toSet()
                if (keys.isEmpty() || parts.isEmpty()) p
                else p.copy(song = p.song.mapIndexed { i, part -> if (i in parts) part.copy(muted = part.muted + keys) else part })
            }
            else -> p
        }
    }

    // --- stage 5: the movement ----------------------------------------------------------------------------

    /** Where the whole mix's filter sweeps from, and how far a « breath » closes it (the filter rests at [AutoTarget.liveRest]). */
    private const val SWEEP_FROM = 0.25f
    private const val BREATH = 0.42f

    /**
     * [name] "sweep": the whole mix's filter opens over each Montée (dark on its first bar, open on its
     * last, back at rest when the peak lands). "throw": the [role]'s track thrown into the reverb on the
     * last bar it plays in each 8-bar phrase. "wave": the filter breathes (a little darker, then open
     * again) over the last bars of each section where nothing moves. Null when the 16 curves are taken.
     */
    fun curve(p: Project, name: String, role: String): Project? {
        val song = GuideSong(p)
        if (song.bars == 0) return p
        val filter = AutoTarget.live(AutoTarget.LIVE_FILTER)
        val rest = AutoTarget.liveRest(AutoTarget.LIVE_FILTER)
        return when (name) {
            "sweep" -> {
                val builds = song.sectionsOf(GuideSection.Kind.BUILD).filter { it.end < AutomationLane.MAX_SONG_BARS }
                if (builds.isEmpty()) return p
                p.withSongLane(filter, songOnly = true) { lane ->
                    builds.fold(lane) { l, s ->
                        var out = l.cleared(p, s.start, s.end)
                        if (s.start > 0 && out.barValue(p, s.start - 1) == null) out = out.bar(p, s.start - 1, rest)
                        out.bar(p, s.start, SWEEP_FROM).jump(p, s.start).bar(p, s.end - 1, rest).bar(p, s.end, rest).jump(p, s.end)
                    }
                }
            }
            "throw" -> {
                val t = listOf(role, GuideRoles.LEAD, GuideRoles.VOCAL, GuideRoles.PAD).asSequence()
                    .flatMap { r -> song.tracks.filter { it.role == r && song.plays(it.key) && it.channel != Mixer.MASTER && !Mixer.isPad(it.channel) }.asSequence() }
                    .firstOrNull() ?: return p
                // The last bar it sounds in, in each 8-bar phrase of each section: its last note flies off.
                val throws = song.sections.flatMap { s ->
                    (s.start until s.end step 8).mapNotNull { from -> (minOf(from + 8, s.end) - 1 downTo from).firstOrNull { song.sounds(t.key, it) } }
                }.filter { it + 1 < AutomationLane.MAX_SONG_BARS }
                if (throws.isEmpty()) return p
                p.withSongLane(AutoTarget.macro(t.channel, AutoTarget.MACRO_THROW_REVERB), songOnly = true) { lane ->
                    var out = lane.copy(songValues = emptyList(), songJumps = emptyList())
                    // Off right before and right after each one: a curve slides between its points.
                    for (b in throws) {
                        if (b > 0 && b - 1 !in throws) out = out.bar(p, b - 1, 0f)
                        out = out.bar(p, b, 1f)
                        if (b + 1 !in throws) out = out.bar(p, b + 1, 0f)
                    }
                    out.bar(p, 0, 0f)
                }
            }
            "wave" -> {
                val lanes = p.automation.filter { it.target.kind != AutoTarget.BUS_PARAM && !it.target.isMasterVolume }
                val still = song.sections.filter { s -> s.bars >= 4 && s.end < AutomationLane.MAX_SONG_BARS && lanes.none { moves(p, song, it, s) } }
                if (still.isEmpty()) return p
                p.withSongLane(filter, songOnly = true) { lane ->
                    still.fold(lane) { l, s ->
                        val from = s.end - 4
                        // Its own curve is left alone where it already has points.
                        if ((from..s.end).any { l.barValue(p, it) != null }) l
                        else l.bar(p, from, rest).bar(p, s.end - 2, BREATH).bar(p, s.end - 1, rest).bar(p, s.end, rest)
                    }
                }
            }
            else -> p
        }
    }

    /** Lane [l] moves in section [s] (the analysis' own test). */
    private fun moves(p: Project, song: GuideSong, l: AutomationLane, s: GuideSection): Boolean {
        if (l.mode != AutomationLane.SONG) return l.values.filterNotNull().let { it.isNotEmpty() && it.max() - it.min() > GuideFacts.MOVE }
        val per = l.pointsPerBar(p.beatsPerBar)
        val curve = l.songCurve(song.bars * per)
        val vs = (s.start * per until (s.end * per).coerceAtMost(curve.size)).map { curve[it] }
        return vs.isNotEmpty() && vs.max() - vs.min() > GuideFacts.MOVE
    }

    private fun AutomationLane.bar(p: Project, bar: Int, v: Float): AutomationLane = withSongPoint(bar * pointsPerBar(p.beatsPerBar), v)

    private fun AutomationLane.jump(p: Project, bar: Int): AutomationLane = withSongJump(bar * pointsPerBar(p.beatsPerBar), true)

    private fun AutomationLane.barValue(p: Project, bar: Int): Float? = songValues.getOrNull(bar * pointsPerBar(p.beatsPerBar))

    /** Without its points from bar [from] to bar [to] (both included). */
    private fun AutomationLane.cleared(p: Project, from: Int, to: Int): AutomationLane {
        val per = pointsPerBar(p.beatsPerBar)
        val range = from * per..(to * per + per - 1)
        return copy(songValues = songValues.mapIndexed { i, v -> if (i in range) null else v }.dropLastWhile { it == null }, songJumps = songJumps.filter { it !in range })
    }

    /** A drum fill on the last bar before each peak that has none (the style's fill: [PartIntents.fillOf]). Last first: the parts it cuts do not move the others. */
    fun fills(p: Project): Project {
        val song = GuideSong(p)
        val before = song.sectionsOf(GuideSection.Kind.PEAK).filter { it.start > 0 && !song.fillBars[it.start - 1] }.map { it.parts.first() - 1 }
        var q = p
        for (i in before.filter { it >= 0 }.sortedDescending()) {
            val part = q.song.getOrNull(i) ?: continue
            if (q.isFillPart(part) || part.muted.contains(SongPart.DRUMS)) continue
            q = q.fillAtEnd(i)
        }
        return q
    }

    // --- stage 6: levels and checks ---------------------------------------------------------------------

    /**
     * Each measured track's fader moved toward its place ([GuideLevels.offsets], [GuideLevels.MAX_MOVE_DB]
     * at most), and the tracks' measures moved with it: a fader changes the track's level by exactly that, so the
     * analysis sees the balance done; the song's own loudness is cleared ([GuideMeasures.partial]).
     */
    fun balance(p: Project): Project {
        val facts = GuideFacts.of(p)
        val m = facts.fresh ?: return p
        val off = GuideLevels.offsets(facts, m).filter { abs(it.value) > GuideLevels.TOLERANCE_DB }
        if (off.isEmpty()) return p
        var mixer = p.mixer
        val tracks = m.tracks.toMutableMap()
        for ((key, d) in off) {
            val ch = GuideTrack.channelOf(key) ?: continue
            val c = mixer.channels.getOrNull(ch) ?: continue
            val now = GuideLevels.faderDb(c.volume)
            val volume = GuideLevels.faderOf(now - d.coerceIn(-GuideLevels.MAX_MOVE_DB, GuideLevels.MAX_MOVE_DB))
            if (abs(volume - c.volume) < 1e-4f) continue
            tracks[key] = tracks.getValue(key) + GuideLevels.faderDb(volume) - now
            mixer = mixer.copy(channels = mixer.channels.mapIndexed { i, old -> if (i == ch) old.copy(volume = volume).sanitized() else old })
        }
        if (mixer == p.mixer) return p
        val q = p.copy(mixer = mixer)
        // Only the tracks' levels are known after it (G4 review): the song's loudness, its sections and
        // mono are left to measure again, so that the contrast and the headroom never read old numbers.
        val kept = m.copy(tracks = tracks, integratedLufs = null, truePeakDb = null, sections = emptyMap(), monoLossDb = null, partial = true)
        return q.copy(guide = q.guide.copy(measures = kept.copy(signature = GuideSignatures.sound(q))))
    }

    /** The lows in mono up to [MONO_HZ] and no widener past 100 % (« Resserrer »). */
    fun mono(p: Project): Project {
        val m = p.mixer
        val channels = m.channels.map { c ->
            c.copy(inserts = c.inserts.map { i -> if (i.effect == EffectType.WIDENER && !i.bypass && i.params.getOrElse(0) { 0f } > WIDTH_100) i.copy(params = i.params.mapIndexed { k, v -> if (k == 0) WIDTH_100 else v }) else i })
        }
        return p.copy(mixer = m.copy(channels = channels, monoBassHz = maxOf(m.monoBassHz, MONO_HZ)))
    }

    /** The widener's « Largeur » at 100 % (its knob shows ×200 %). */
    private const val WIDTH_100 = 0.5f
    const val MONO_HZ = 150f
}
