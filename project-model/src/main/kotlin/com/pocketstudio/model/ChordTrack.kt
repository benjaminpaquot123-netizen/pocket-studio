package com.pocketstudio.model

/**
 * Phase 37, « Accords du morceau »: the song's chords, one per bar of each scene ([Scene.chords]),
 * as degrees of the project's key (Logic's chord track). Changing them moves the notes of the
 * tracks that follow ([follows]: the assistant's by default, the user's own only when asked) to
 * the new chords, each bar by the steps of the scale from its old chord's root to the new one's
 * (the shape of a line, a bass's root and fifth, a chord's voicing are kept). The old chords are
 * the ones written, else the ones the notes play ([guess]). One edit of the project: one undo step.
 */
object ChordTrack {
    /** Chords a scene keeps (bars of the longest loop and more). */
    const val MAX_CHORDS = 64

    /** [chords] as a file may hold them: degrees 0..6, at most [MAX_CHORDS]; itself when already so. */
    fun clean(chords: List<Int>): List<Int> =
        if (chords.size <= MAX_CHORDS && chords.all { it in 0..6 }) chords else chords.take(MAX_CHORDS).map { Math.floorMod(it, 7) }

    /** Bars scene [scene] lasts (at least one). */
    fun bars(p: Project, scene: Int): Int {
        val s = p.allScenes.getOrNull(scene) ?: return 1
        return (s.stepsFor(p.stepsPerBar) / p.stepsPerBar).coerceAtLeast(1)
    }

    /** The scenes the song plays, in order of first appearance (the scene shown when there is no song). */
    fun songScenes(p: Project): List<Int> =
        if (p.song.isEmpty()) listOf(p.scene) else p.song.map { it.scene }.distinct().filter { it in p.allScenes.indices }

    /** True when scene [scene] has no chords written ([chordsOf] then guesses them from its notes). */
    fun isGuessed(p: Project, scene: Int): Boolean = p.allScenes.getOrNull(scene)?.chords.isNullOrEmpty()

    /** Scene [scene]'s chords, one per bar: the ones written (repeated over its bars), else [guess]ed. */
    fun chordsOf(p: Project, scene: Int): List<Int> {
        val bars = bars(p, scene)
        val written = p.allScenes.getOrNull(scene)?.chords.orEmpty()
        return if (written.isEmpty()) guess(p, scene) else List(bars) { written[it % written.size] }
    }

    /** A chord's name in the key: "La m", or "Am" in English notation. */
    fun name(p: Project, degree: Int, english: Boolean): String =
        p.harmony.chordPads(48)[Math.floorMod(degree, 7)].name(english)

    /** The roman numeral of a degree in the key ("vi"). */
    fun roman(p: Project, degree: Int): String = p.harmony.chordPads(48)[Math.floorMod(degree, 7)].roman

    /** The degrees a ready-made progression plays in the key (a diminished one replaced, [progressionDegree]). */
    fun degreesOf(p: Project, progression: Progression): List<Int> =
        progression.degrees.map { Math.floorMod(p.harmony.progressionDegree(it), 7) }

    /** Whether track [t]'s notes follow the chords: its own choice, else the assistant's tracks do. */
    fun follows(p: Project, t: InstrumentTrack): Boolean =
        t.followChords ?: (p.style != null && !TrackRoles.isRiserName(t.name))

    /** The 303's id among the [followers] (instrument tracks are 0..7). */
    const val ACID = -1

    /** Whether the 303's steps follow the chords: the project's choice, else the assistant's songs do. */
    fun acidFollows(p: Project): Boolean = p.acidFollowChords ?: (p.style != null)

    /** The ids of the tracks that follow the chords ([ACID]: the 303). */
    fun followers(p: Project): Set<Int> =
        p.instrumentTracks.filter { follows(p, it) }.map { it.id }.toSet() + (if (acidFollows(p)) setOf(ACID) else emptySet())

    /** Track [id] ([ACID]: the 303) follows the chords ([on]) or keeps its notes as written. */
    fun withFollow(p: Project, id: Int, on: Boolean): Project =
        if (id == ACID) p.copy(acidFollowChords = on) else p.withTrack(p.track(id).copy(followChords = on))

    /** The 303 plays something in the song's scenes (the chords' sheet then offers it). */
    fun acidPlays(p: Project): Boolean = songScenes(p).any { s ->
        p.allScenes.getOrNull(s)?.acid?.let { a -> a.steps.take(a.length.coerceAtLeast(0)).any { it.gate } } == true
    }

    /** The clip of track [id] in [scene] (the scene's own; the first track's in [Scene.synthNotes]). */
    private fun clipOf(s: Scene, id: Int): NoteClip? = if (id == 0) s.synthNotes else s.trackNotes[id]

    /**
     * The chords the notes of scene [scene] play, one per bar: for each bar, the chord of the key
     * whose notes sound longest there (the lowest note's pitch class counts more: the bass says the
     * root). A bar with nothing to hear keeps the chord before it (the key's I at the start).
     */
    fun guess(p: Project, scene: Int): List<Int> {
        val s = p.allScenes.getOrNull(scene) ?: return listOf(0)
        val bars = bars(p, scene)
        val barTicks = p.stepsPerBar * NoteClip.TICKS_PER_STEP
        // The chords and pads say the chord most, the bass its root; a melody passes through other notes.
        val pads = p.padTrackIds().toSet()
        val basses = p.bassTrackIds().toSet()
        val clips = p.instrumentTracks.filter { !TrackRoles.isRiserName(it.name) }.mapNotNull { t ->
            clipOf(s, t.id)?.takeIf { it.notes.isNotEmpty() && it.ticks > 0 }?.let { Triple(it, if (t.id in pads) 2.0 else if (t.id in basses) 1.0 else 0.5, t.id in basses) }
        }
        // The root is the bass's lowest note (any track's lowest when there is no bass).
        val anyBass = clips.any { it.third }
        // Each chord with its seventh: the assistant's stabs leave the root to the bass (3-5-7 voicings).
        val triads = (0..6).map { d -> p.harmony.chord(p.harmony.chordKey.degreeRoot(d, 48), size = 4).map { Math.floorMod(it, 12) } }
        // Only the key's major and minor chords (a diminished one is played as its neighbour, as the progressions do).
        val candidates = (0..6).filter { p.harmony.progressionDegree(it) == it }
        val out = ArrayList<Int>(bars)
        for (bar in 0 until bars) {
            val from = bar * barTicks
            val to = from + barTicks
            val weight = DoubleArray(12)
            val bass = DoubleArray(12)
            for ((clip, w, isBass) in clips) {
                // The clip loops inside the scene: its notes at each pass that touches this bar.
                var at = (from / clip.ticks) * clip.ticks
                while (at < to) {
                    val sounding = ArrayList<Pair<Note, Int>>()
                    for (n in clip.notes) {
                        val a = maxOf(from, at + n.start)
                        val b = minOf(to, at + n.end)
                        if (b > a) {
                            weight[Math.floorMod(n.pitch, 12)] += w * (b - a)
                            sounding += n to (b - a)
                        }
                    }
                    if (isBass || !anyBass) sounding.minByOrNull { it.first.pitch }?.let { (n, len) -> bass[Math.floorMod(n.pitch, 12)] += 2.0 * len }
                    at += clip.ticks
                }
            }
            if (weight.all { it == 0.0 }) {
                out += out.lastOrNull() ?: 0
                continue
            }
            val previous = out.lastOrNull()
            val best = candidates.maxWith(
                compareBy<Int> { d -> triads[d].sumOf { weight[it] } + 3.0 * bass[triads[d][0]] + 0.25 * weight[triads[d][0]] - 0.5 * outside(weight, triads[d]) }
                    .thenBy { d -> if (d == previous) 1 else 0 }
                    .thenBy { d -> -d },
            )
            out += best
        }
        return out
    }

    private fun outside(weight: DoubleArray, triad: List<Int>): Double = (0 until 12).filter { it !in triad }.sumOf { weight[it] }

    /** The steps of the scale (-3..3, the nearest way) from chord [from]'s root to chord [to]'s. */
    fun stepsBetween(from: Int, to: Int): Int {
        val d = Math.floorMod(to - from, 7)
        return if (d > 3) d - 7 else d
    }

    /**
     * Scene [scene] with [chords] (one per bar, repeated over its bars when shorter): written, and
     * the notes of the tracks in [followers] moved to them from the chords it had ([chordsOf]).
     */
    fun withChords(p: Project, scene: Int, chords: List<Int>, followers: Set<Int> = followers(p)): Project =
        withChordsOf(p, mapOf(scene to chords), followers)

    /** [withChords] on several scenes at once (scene index to chords): one edit. */
    fun withChordsOf(p: Project, chords: Map<Int, List<Int>>, followers: Set<Int> = followers(p)): Project {
        val synced = p.syncScenes()
        val list = synced.allScenes.toMutableList()
        for ((scene, wanted) in chords) {
            if (scene !in list.indices || wanted.isEmpty()) continue
            val bars = bars(synced, scene)
            val old = chordsOf(synced, scene)
            val new = List(bars) { Math.floorMod(wanted[it % wanted.size], 7) }
            val s = list[scene]
            val sceneSteps = s.stepsFor(synced.stepsPerBar)
            fun moved(clip: NoteClip?): NoteClip? = clip?.let { reharmonized(synced, it, sceneSteps, old, new) }
            list[scene] = s.copy(
                chords = clean(compact(new)),
                synthNotes = if (0 in followers) moved(s.synthNotes)!! else s.synthNotes,
                trackNotes = s.trackNotes.mapValues { (id, clip) -> if (id in followers) moved(clip)!! else clip },
                acid = if (ACID in followers) reharmonizedAcid(synced, s.acid, sceneSteps, old, new) else s.acid,
            )
        }
        return synced.showing(list, synced.scene)
    }

    /** The whole song (every scene it plays) on [progression], one chord a bar, in the key. */
    fun withProgression(p: Project, progression: Progression, followers: Set<Int> = followers(p)): Project {
        val degrees = degreesOf(p, progression)
        return withChordsOf(p, songScenes(p).associateWith { degrees }, followers)
    }

    /** The chords as written: the shortest cycle that repeats into them (I V vi IV, not twice over). */
    private fun compact(chords: List<Int>): List<Int> {
        for (n in 1..chords.size) {
            if (chords.size % n == 0 && chords.indices.all { chords[it] == chords[it % n] }) return chords.take(n)
        }
        return chords
    }

    /**
     * [clip] (played over [sceneSteps]) moved from chords [old] to [new] (one per bar): each note by
     * the scale steps from its bar's old chord to its new one. A clip shorter than the scene that
     * would need different moves on each of its passes is first written out over the scene (a
     * 1-bar loop under 4 chords becomes 4 bars), when the scene fits in a clip.
     */
    fun reharmonized(p: Project, clip: NoteClip, sceneSteps: Int, old: List<Int>, new: List<Int>): NoteClip {
        if (clip.notes.isEmpty() || clip.ticks <= 0 || old.isEmpty() || new.isEmpty()) return clip
        val barTicks = p.stepsPerBar * NoteClip.TICKS_PER_STEP
        val sceneTicks = sceneSteps * NoteClip.TICKS_PER_STEP
        fun stepsAt(tick: Int): Int {
            val bar = (tick / barTicks).coerceAtLeast(0)
            return stepsBetween(old[bar % old.size], new[bar % new.size])
        }
        val passes = ((sceneTicks + clip.ticks - 1) / clip.ticks).coerceAtLeast(1)
        val uniform = passes == 1 || clip.notes.all { n -> (1 until passes).all { r -> stepsAt(r * clip.ticks + n.start) == stepsAt(n.start) } }
        // Written out only when every copy fits (the notes past MAX_NOTES would be lost).
        val canWriteOut = sceneSteps % 16 == 0 && sceneSteps <= NoteClip.MAX_STEPS && clip.ticks * passes <= NoteClip.MAX_STEPS * NoteClip.TICKS_PER_STEP &&
            clip.notes.size * passes + clip.hidden.count { it.start >= sceneTicks } <= NoteClip.MAX_NOTES
        val base = if (uniform || !canWriteOut) clip else clip.copy(
            steps = sceneSteps,
            notes = (0 until passes).flatMap { r -> clip.notes.map { it.movedTo(it.start + r * clip.ticks, clip.ticks, sceneTicks) } }
                .filter { it.start < sceneTicks },
            // The notes hidden past the old end inside the new one would come back over the written-out
            // copies (and in the old key): only those past the new end stay hidden.
            hidden = clip.hidden.filter { it.start >= sceneTicks },
        )
        val h = p.harmony
        fun move(n: Note): Note {
            val bar = (n.start / barTicks).coerceAtLeast(0)
            val from = old[bar % old.size]
            val steps = stepsAt(n.start)
            return if (steps == 0) n else n.copy(pitch = h.shiftDiatonic(n.pitch, steps, from).coerceIn(0, 127))
        }
        // Hidden notes move too, so they come back in the new chords when the loop grows again.
        return base.copy(notes = base.notes.map(::move), hidden = base.hidden.map(::move)).sanitized()
    }
}

/**
 * The 303's line [acid] (played over [sceneSteps]) moved from chords [old] to [new] (one per bar),
 * as [ChordTrack.reharmonized] moves notes: each step by the scale steps from its bar's old chord to
 * its new one, so the line keeps its shape (its octave jumps, the gaps of its riff); its accents,
 * slides, gates and locks stay where they are (the acid character). A line shorter than the scene
 * that would need different moves on each of its passes is first written out over the scene
 * (when the scene fits in the 303's 8 bars). The root stays: it is the 303's for every scene.
 */
fun ChordTrack.reharmonizedAcid(p: Project, acid: AcidPattern, sceneSteps: Int, old: List<Int>, new: List<Int>): AcidPattern {
    val length = acid.length.coerceIn(1, AcidPattern.MAX_STEPS)
    if (old.isEmpty() || new.isEmpty() || acid.steps.take(length).none { it.gate }) return acid
    val bar = p.stepsPerBar.coerceAtLeast(1)
    fun movesAt(step: Int): Int {
        val b = (step / bar).coerceAtLeast(0)
        return stepsBetween(old[b % old.size], new[b % new.size])
    }
    val passes = ((sceneSteps + length - 1) / length).coerceAtLeast(1)
    val uniform = passes == 1 || (0 until length).all { i -> (1 until passes).all { r -> movesAt(r * length + i) == movesAt(i) } }
    val canWriteOut = sceneSteps in 1..AcidPattern.MAX_STEPS
    val base = if (uniform || !canWriteOut) acid else acid.copy(
        length = sceneSteps,
        steps = List(AcidPattern.MAX_STEPS) { i -> if (i < sceneSteps) acid.steps[i % length] else AcidStep() },
        locked = (0 until sceneSteps).filter { (it % length) in acid.locked }.toSet(),
    )
    val n = base.length.coerceIn(1, AcidPattern.MAX_STEPS)
    val h = p.harmony
    val steps = base.steps.mapIndexed { i, st ->
        if (i >= n) return@mapIndexed st
        val moves = movesAt(i)
        if (moves == 0) return@mapIndexed st
        val b = (i / bar).coerceAtLeast(0)
        val pitch = base.root + st.note + 12 * st.octave
        // Kept inside the 303's range (an octave under the root to three above), by whole octaves.
        var rel = h.shiftDiatonic(pitch, moves, old[b % old.size]) - base.root
        while (rel > 36) rel -= 12
        while (rel < -12) rel += 12
        val octave = Math.floorDiv(rel, 12).coerceIn(-1, 2)
        st.copy(note = (rel - 12 * octave).coerceIn(0, 12), octave = octave)
    }
    return base.copy(steps = steps).sanitized()
}

/**
 * [pitch] moved [steps] notes along the key's seven-note scale ([Harmony.chordKey]): Mi → Sol is
 * 2 steps up in Do majeur. A note outside the scale moves by as many semitones as the root of the
 * chord [fromDegree] does (a passing note keeps its distance to the chord).
 */
fun Harmony.shiftDiatonic(pitch: Int, steps: Int, fromDegree: Int = 0): Int {
    if (steps == 0) return pitch
    val key = chordKey
    val ints = key.scale.intervals
    val rel = pitch - root
    val pc = Math.floorMod(rel, 12)
    val idx = ints.indexOf(pc)
    if (idx < 0) return pitch + (key.degreeRoot(fromDegree + steps, 0) - key.degreeRoot(fromDegree, 0))
    val octave = Math.floorDiv(rel, 12)
    val target = idx + steps
    return root + 12 * (octave + Math.floorDiv(target, ints.size)) + ints[Math.floorMod(target, ints.size)]
}
