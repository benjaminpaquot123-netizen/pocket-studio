package com.pocketstudio.model.tuto

import com.pocketstudio.model.AcidPattern
import com.pocketstudio.model.AcidStep
import com.pocketstudio.model.AutoTarget
import com.pocketstudio.model.AutomationLane
import com.pocketstudio.model.Channel
import com.pocketstudio.model.DrumLane
import com.pocketstudio.model.DrumPattern
import com.pocketstudio.model.EffectType
import com.pocketstudio.model.FxPresets
import com.pocketstudio.model.Harmony
import com.pocketstudio.model.Hit
import com.pocketstudio.model.Insert
import com.pocketstudio.model.InstrumentTrack
import com.pocketstudio.model.LoudnessTarget
import com.pocketstudio.model.Mixer
import com.pocketstudio.model.Note
import com.pocketstudio.model.guide.GuideStatus
import com.pocketstudio.model.NoteClip
import com.pocketstudio.model.Project
import com.pocketstudio.model.Scene
import com.pocketstudio.model.SongPart
import com.pocketstudio.model.DrumArrangement
import com.pocketstudio.model.partKeys
import com.pocketstudio.model.StarterWizard
import com.pocketstudio.model.StarterWizard.Part
import com.pocketstudio.model.StudioDrums
import com.pocketstudio.model.SynthPatch
import com.pocketstudio.model.TrackKind
import com.pocketstudio.model.NewTracks
import com.pocketstudio.model.allScenes
import com.pocketstudio.model.copySceneTo
import com.pocketstudio.model.currentSceneContent
import com.pocketstudio.model.firstEmptyScene
import com.pocketstudio.model.instrumentTracks
import com.pocketstudio.model.partSteps
import com.pocketstudio.model.renameScene
import com.pocketstudio.model.switchScene
import com.pocketstudio.model.track
import com.pocketstudio.model.withFx
import com.pocketstudio.model.withTrack
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.ceil

/**
 * What the course aims at in a style: the recipe's lines, notes and whole song, as the assistant
 * makes them (the targets of the steps, never written into the user's project but by « Montre-moi »).
 */
class TutoTarget private constructor(base: StarterWizard.Style) {
    /** The feeling the course picks ([TutoCourse.mood]), else the assistant's for the style. */
    val mood: StarterWizard.Mood = TutoCourse.of(base.id)?.mood?.let { m -> StarterWizard.Mood.entries.firstOrNull { it.id == m } } ?: base.melody.mood

    /**
     * The style as the course plays it: the assistant's recipe, in the course's key and on its
     * chords when it names them ([TutoCourse.key], [TutoCourse.chords]; the recipe itself unchanged).
     */
    val style: StarterWizard.Style = TutoCourse.of(base.id)?.let { c ->
        val key = TutoCourse.keyOf(c.key)
        val degrees = TutoCourse.degreesOf(c.chords)?.takeIf { it.isNotEmpty() }
        val lines = c.drums[TutoCourse.ALL_PARTS]?.let { mergedLines(base, it) }
        if (key == null && degrees == null && lines == null) return@let base
        val (scale, theirs) = StarterWizard.progressionOf(base, mood)
        base.copy(
            melody = if (key == null && degrees == null) base.melody else base.melody.copy(
                root = key?.first ?: base.melody.root,
                progressions = base.melody.progressions + (mood to ((key?.second ?: scale) to (degrees ?: theirs))),
            ),
            // The course's own drums in place of the recipe's lines it names (the others stay).
            lines = lines ?: base.lines,
        )
    } ?: base

    val harmony = StarterWizard.harmonyOf(style, mood)

    /**
     * The drum line of [role] (a [StarterWizard.Role] id: "kick", "open_hat", "rumble"…): in the
     * part [part] when the course gives it its own drums, else the course's or the recipe's line
     * for every part, else the first part of the course that has one; null when none.
     */
    fun line(role: String, part: String? = null): StarterWizard.Line? {
        part?.let { partLines(it) }?.let { lines -> return lines.firstOrNull { it.role.id == role } }
        style.lines.firstOrNull { it.role.id == role }?.let { return it }
        return TutoCourse.of(style.id)?.drums.orEmpty().keys.filter { it != TutoCourse.ALL_PARTS }
            .firstNotNullOfOrNull { k -> partLines(k)?.firstOrNull { it.role.id == role } }
    }

    /** The course's own drums of the part [label] (its key, or the part it is a second of: « Drop 2 » → « Drop »), null when none. */
    fun partLines(label: String): List<StarterWizard.Line>? {
        val drums = TutoCourse.of(style.id)?.drums ?: return null
        val key = drums.keys.firstOrNull { it == label.trim() } ?: STEM.matchEntire(label.trim())?.groupValues?.get(1)?.takeIf { it in drums } ?: return null
        return courseLines(drums[key], style)
    }

    /** The course's crash in the part [label] (played once at its start), null when it has none. */
    fun crashIn(label: String): Boolean {
        val drums = TutoCourse.of(style.id)?.drums ?: return false
        val key = drums.keys.firstOrNull { it == label.trim() } ?: STEM.matchEntire(label.trim())?.groupValues?.get(1)?.takeIf { it in drums } ?: return false
        return drums[key]?.keys?.any { drumRole(it, style) == TutoCourse.CRASH } == true
    }

    /** The drums of the part [label] the course gives it, over [steps] (null: none of its own). */
    fun partDrums(label: String, steps: Int? = null): DrumPattern? {
        val lines = partLines(label) ?: return null
        val n = (steps ?: lines.maxOfOrNull { it.pattern.length } ?: 16).coerceIn(DrumPattern.MIN_STEPS, DrumPattern.MAX_STEPS)
        val lanes = MutableList(DrumPattern.PAD_COUNT) { com.pocketstudio.model.DrumLane() }
        for (l in lines) lanes[l.role.lane] = lanes[l.role.lane].copy(hits = StarterWizard.hits(l, n))
        return DrumPattern(steps = n, lanes = lanes)
    }

    /** The bass is the 808 ([StudioDrums.BASS_808]: trap, drill, phonk, jersey club, reggaeton): the tutorial's is an 808 track. */
    val bass808: Boolean get() = style.id in StudioDrums.BASS_808

    /** The kind of sound the assistant gives [part] (its sound and level). */
    fun kind(part: Part) = when (part) {
        Part.Bass -> TrackKind.Bass
        Part.Pad -> TrackKind.Chords
        else -> TrackKind.Melody
    }

    /**
     * The kind of track « Créer la piste » makes for [part]: [kind], but the 808 styles' bass on an
     * 808 track (« + Piste › Basse 808 »: the studio 808, gliding where two notes overlap) — the
     * assistant plays it on a pad, where its notes cannot be written in the piano roll.
     */
    fun trackKind(part: Part) = if (part == Part.Bass && bass808) TrackKind.Bass808 else kind(part)

    private val notesCache = ConcurrentHashMap<Part, NoteClip>()

    /** The course written for this style (its own lines, its parts), null when none. */
    private val course: TutoCourse? get() = TutoCourse.of(style.id)

    /** The chord of each bar, as degrees of the key (the style's progression in its mood), bar after bar. */
    val chordDegrees: List<Int> get() = StarterWizard.progressionOf(style, mood).second.ifEmpty { listOf(0) }

    /**
     * The notes of [part] in the style (the assistant's first notes, in the course's key), as a
     * phrase of its first half played twice (testers, lot « tuto 4 »: 28 bass notes, 24 of chords
     * touched one by one; the user writes the first 4 bars, « Copier sur les mesures suivantes »
     * repeats them). The jump up bass's one change in its 7th bar goes; the chords and the lead
     * already repeat. A course's own line ([TutoCourse.lines]) replaces it.
     */
    fun notes(part: Part): NoteClip = notesCache.getOrPut(part) {
        val c = StarterWizard.startingNotes(part, style, mood, harmony, NewTracks.sound(kind(part), style))
        // The course's own line (lot « tuto 5 »: one or two bars by hand, the others copied), over the same clip.
        course?.let { lineClip(it, part.id, c) }?.let { return@getOrPut it }
        val half = c.ticks / 2
        if (c.steps < 2 * MOTIF_MIN_STEPS) c
        else c.copy(notes = c.notes.filter { it.start < half }.let { first -> first + first.map { it.copy(start = it.start + half) } }).sanitized()
    }

    /** [course]'s line of [role] over [c] (made long enough for one phrase), null when it has none. */
    private fun lineClip(course: TutoCourse, role: String, c: NoteClip): NoteClip? {
        val line = course.lines[role] ?: return null
        val phrase = parseLine(line, c.notes.firstOrNull()?.velocity ?: 0.85f)
        val steps = phraseSteps(course, role, phrase)
        val clip = if (c.steps < steps) c.withSteps(steps) else c
        return fitted(clip, phrase, steps, if (role in course.follow) chordDegrees else null, harmony)
    }

    /**
     * The 303 line the course aims at: the course's own ([TutoCourse.lines] "acid"), on the
     * assistant's 303 (its sound, its root), else the assistant's own line; null when the style has
     * no 303 and the course writes none.
     */
    val acid: AcidPattern? by lazy {
        val theirs = assistantSong.allScenes.map { it.acid }.firstOrNull { a -> a.steps.take(a.length).any { it.gate } }
        val c = course ?: return@lazy theirs
        val line = c.lines[ACID] ?: return@lazy theirs
        val base = theirs ?: AcidPattern(root = (36 + harmony.root).coerceIn(24, 60))
        val phrase = parseAcid(line, base.root)
        val steps = c.lineBars[ACID]?.let { it * 16 } ?: phraseBarsOf(phrase.keys.maxOrNull() ?: 0) * 16
        val follow = ACID in c.follow
        val out = MutableList(AcidPattern.MAX_STEPS) { AcidStep() }
        val length = maxOf(steps, theirs?.length ?: 0).coerceIn(16, AcidPattern.MAX_STEPS)
        for (at in 0 until length) {
            val k = at / steps
            val st = phrase[at % steps] ?: continue
            val shift = if (follow) nearest(chordDegrees[(at / 16) % chordDegrees.size] - chordDegrees[((at % steps) / 16) % chordDegrees.size], harmony) else 0
            out[at] = if (shift == 0 || k == 0) st else acidStep(base.root, byDegrees(acidPitch(base.root, st), shift, harmony), st.accent, st.slide)
        }
        base.copy(steps = out, length = length).sanitized()
    }

    /**
     * The style's whole song, as the assistant makes it with its advised sounds (the examples' parts
     * and the map), with what the course has the user make instead: its own lines (and the Drop 2's
     * bass, moved), the Montée's snare that accelerates, its last roll and its silent last beat (lot
     * « tuto 5 »: the examples are what the user writes).
     */
    val song: Project by lazy { TutoChecks.courseSong(assistantSong, this).sanitized() }

    /**
     * The parts the song has: the style's own, and those the course writes (a course may give a
     * style a lead the assistant leaves off).
     */
    val parts: Set<Part> get() = StarterWizard.defaultParts(style) + Part.entries.filter { p ->
        course?.let { c -> p.id in c.lines || c.chapters.any { ch -> ch.steps.any { it.check == "notes:${p.id}" } } } == true
    }

    /**
     * The notes of a track role ([TRACK_ROLES]): the assistant's parts' ([notes]), a second pad's
     * and a counter-melody's (the course's lines only, over 8 bars); null when the role has none.
     */
    fun roleNotes(role: String): NoteClip? {
        Part.entries.firstOrNull { it.id == role }?.let { return notes(it) }
        if (role !in EXTRA_ROLES) return null
        val c = course ?: return null
        return lineClip(c, role, NoteClip(steps = StarterWizard.PHRASE_STEPS))
    }

    /** The part whose kind of track and sound a role takes: the second pad the chords', the counter-melody the lead's. */
    fun partOf(role: String): Part? = when (role) {
        "sub" -> Part.Bass
        PAD2 -> Part.Pad
        COUNTER -> Part.Lead
        else -> Part.entries.firstOrNull { it.id == role }
    }

    /** The style's whole song as the assistant makes it (the drums a part takes from it: the Montée's before its silence). */
    val assistantSong: Project by lazy {
        val parts = parts.associateWith { NewTracks.sound(kind(it), style) }
        StarterWizard.build(style, style.lines.associate { it.role to null }, mood, parts).sanitized()
    }

    /**
     * The track of [assistantSong] that plays [part], null when none (the 808's bass is on its pad):
     * the assistant gives the parts their tracks in [Part] order.
     */
    fun songTrack(part: Part): Int? {
        val played = Part.entries.filter { it in parts && !(it == Part.Bass && bass808) }
        val i = played.indexOf(part).takeIf { it >= 0 } ?: return null
        return assistantSong.instrumentTracks.getOrNull(i)?.id
    }

    /** The step whose part has its own bass ([TutoStep.transpose]: the Drop 2's), null when none. */
    val movedStep: TutoStep? get() = course?.chapters?.flatMap { it.steps }?.firstOrNull { it.transpose != 0 && it.check.startsWith("part:") }

    /** The step that makes a part with an accelerating pad ([TutoStep.build]: the Montée's snare), null when none. */
    val buildStep: TutoStep? get() = course?.chapters?.flatMap { it.steps }?.firstOrNull { it.build.isNotEmpty() && it.check.startsWith("part:") }

    /**
     * The scene of [song] named [label], -1 when none: that label, else the part it is a second
     * of (« Refrain 2 » plays the Refrain, « Drop 2 » the Drop), else the Intro for a « Fin ».
     */
    fun sceneOf(label: String): Int {
        val all = song.allScenes
        all.indexOfFirst { it.label == label }.takeIf { it >= 0 }?.let { return it }
        val stem = STEM.matchEntire(label.trim())?.groupValues?.get(1)
        if (stem != null) all.indexOfFirst { it.label == stem }.takeIf { it >= 0 }?.let { return it }
        if (label.trim() in ENDINGS) return all.indexOfFirst { it.label == "Intro" }
        return -1
    }

    /**
     * The tutorial's project at its start: EMPTY (no hit, no note, no part, the first track left
     * blank and hidden until a step makes it a track of the song), but already with the style's
     * sounds as the assistant sets them: the kit, its voices (the tuned kick, the rumble, the 808),
     * their levels and their effects (the punch on the kick and the snare, not their reverb, a
     * step's), the 303's sound when the style has one (its notes the user's), and the built-in kit
     * standing for the style's drum pack ([KITS]: jump up's drum & bass kit; owner, 2026-10-28:
     * « ça doit sonner jump up dès le chapitre 2 »). The tracks a step makes take the style's sounds
     * then ([TutoChecks.partTrack]). Built-in sounds only (a downloaded pack is not taken: the
     * examples, made of the same sounds, sound like what the user builds). Tempo, key and swing stay
     * the new project's, steps set them.
     */
    fun blank(): Project {
        val s = song
        val fresh = Project(name = "")
        val drumChannels = setOf(0) + (0 until DrumPattern.PAD_COUNT).map { Mixer.padChannel(it) }
        val made = fresh.copy(
            kitVersion = s.kitVersion, soundVersion = s.soundVersion, mixVersion = s.mixVersion, padVoices = s.padVoices, drumSeed = s.drumSeed,
            drums = s.drums.copy(steps = fresh.drums.steps, lanes = s.drums.lanes.map { it.copy(hits = emptyMap()) }),
            mixer = fresh.mixer.copy(channels = fresh.mixer.channels.mapIndexed { i, c ->
                val theirs = s.mixer.channels.getOrNull(i)
                if ((i in drumChannels || (acid != null && i == AutoTarget.ACID_CHANNEL)) && theirs != null) c.copy(volume = theirs.volume, pan = theirs.pan, inserts = theirs.inserts) else c
            }),
            // The style's 303, silent: its sound and its root, no note.
            acid = acid?.let { a -> fresh.acid.copy(root = a.root, sound = a.sound) } ?: fresh.acid,
        )
        val kit = kitOf(style)?.let { KITS[it] }
        return (if (kit != null) kit(made.copy(harmony = harmony)).copy(harmony = made.harmony) else made).sanitized()
    }

    companion object {
        /**
         * The kits built in that stand for a drum pack (« Kit ▾ »'s, nothing to download), by the
         * pack the style takes its kick from ([StarterWizard.Style.kit]): the tutorial starts on it.
         * A style whose pack has none starts on the assistant's own kit (its studio voices: the
         * tuned kick of house and techno, the hard kick, the rumble, the 808). The drum & bass kit
         * also stands for the packs of drum & bass's other styles (neurofunk, liquid, jungle: the same
         * short kick and cracking snare at 165-174 BPM; bass family, 2026-10-29).
         */
        val KITS: Map<String, (Project) -> Project> = listOf("dnb", "neurofunk", "liquid_dnb", "jungle").associateWith { { p: Project -> StudioDrums.withDnbKit(p) } }

        /**
         * The pack [style]'s kick comes from (its recipe's source, else the style's own pack, as the
         * assistant takes it), when a kit of [KITS] stands for it.
         */
        fun kitOf(style: StarterWizard.Style): String? = (style.kit[StarterWizard.Role.Kick] ?: style.id).takeIf { it in KITS }

        /** The 303's role in a course's lines. */
        const val ACID = "acid"
        /** A second pad (« Nappe ») beside the chords, and a counter-melody (« Contre-chant ») beside the lead. */
        const val PAD2 = "pad2"
        const val COUNTER = "counter"
        val EXTRA_ROLES = setOf(PAD2, COUNTER)
        /** The roles a track of the tutorial plays. */
        val TRACK_ROLES = listOf("bass", "pad", "lead", PAD2, COUNTER)
        /** The names of the extra roles' tracks (saved in French, translated at display). */
        val EXTRA_NAMES = mapOf(PAD2 to "Nappe", COUNTER to StarterWizard.COUNTER_TRACK)
        /** A line this long (4 bars of 16ths) or longer is a phrase written once, then copied. */
        const val MOTIF_MIN_STEPS = 64
        /** A course's line: a phrase of at most this many steps (4 bars) when its length is not said. */
        const val PHRASE_STEPS = 64

        /** A part's label that is a second one of another (« Drop 2 », « Refrain 3 »). */
        val STEM = Regex("""^(.+) \d+$""")
        /** The ends of a song, played as its Intro when the target has none of its own. */
        val ENDINGS = setOf("Fin", "Outro")

        /** The kit voice of the shaker (the pad « Shaker »). */
        private const val SHAKER_VOICE = 13

        /**
         * The drum role a course's [name] is in [style] ([TutoCourse.DRUM_NAMES]; [TutoCourse.CRASH]),
         * null when none: « shaker » is the recipe's line played by the shaker (afrobeats' hats), else
         * the second percussion's.
         */
        fun drumRole(name: String, style: StarterWizard.Style): String? {
            val id = TutoCourse.DRUM_NAMES[name.trim()] ?: return null
            if (name.trim() == "shaker") style.lines.firstOrNull { it.voice == SHAKER_VOICE }?.let { return it.role.id }
            return id
        }

        /**
         * A course's drums of one part as lines (the crash left out), each keeping the recipe line's
         * kit voice (the afro shaker, the congas), null when it gives none.
         */
        fun courseLines(drums: Map<String, String>?, style: StarterWizard.Style): List<StarterWizard.Line>? = drums?.mapNotNull { (name, pattern) ->
            val id = drumRole(name, style)?.takeIf { it != TutoCourse.CRASH } ?: return@mapNotNull null
            StarterWizard.Role.entries.firstOrNull { it.id == id }?.let { r ->
                StarterWizard.Line(r, pattern.trim(), style.lines.firstOrNull { it.role == r }?.tip.orEmpty(), voice = style.lines.firstOrNull { it.role == r }?.voice)
            }
        }?.takeIf { it.isNotEmpty() }

        /**
         * The recipe's lines with the course's « * » drums: the roles it names replaced, the others
         * kept, its new ones added; its crash, with a ride on the kit's one cymbal, played as strong
         * hits of the ride.
         */
        fun mergedLines(base: StarterWizard.Style, drums: Map<String, String>): List<StarterWizard.Line> {
            val mine = courseLines(drums, base).orEmpty().associateBy { it.role }
            var out = base.lines.map { l -> mine[l.role] ?: l } + mine.values.filter { m -> base.lines.none { it.role == m.role } }
            val crash = drums.entries.firstOrNull { drumRole(it.key, base) == TutoCourse.CRASH }?.value?.trim()
            if (crash != null) out = out.map { l ->
                if (l.role != StarterWizard.Role.Ride) l
                else l.copy(pattern = (0 until maxOf(l.pattern.length, crash.length)).map { i ->
                    if (crash.getOrNull(i) in setOf('X', 'x')) 'X' else l.pattern.getOrElse(i % l.pattern.length) { '.' }
                }.joinToString(""))
            }
            return out
        }

        private val NOTE = Regex("""^([A-G])([b#]?)(-?\d)$""")
        private val LETTERS = mapOf('C' to 0, 'D' to 2, 'E' to 4, 'F' to 5, 'G' to 7, 'A' to 9, 'B' to 11)

        /** « F2 » 41, « Gb2 » 42, « C3 » 48 (C-1 is 0). */
        fun pitchOf(name: String): Int {
            val m = NOTE.matchEntire(name.trim()) ?: error("not a note: $name")
            val acc = when (m.groupValues[2]) { "b" -> -1; "#" -> 1; else -> 0 }
            return 12 * (m.groupValues[3].toInt() + 1) + LETTERS.getValue(m.groupValues[1][0]) + acc
        }

        /** One entry of a line: its step, its notes, its length (steps), its strength (null: the clip's) and its 303 flags. */
        class Entry(val step: Float, val pitches: List<Int>, val length: Float, val velocity: Float?, val accent: Boolean, val slide: Boolean)

        /**
         * A course's line ("0:F2:2 3:F2:1 …": step, note, length in steps) as its entries. A chord:
         * "0:F3+Ab3+C4:16". A 4th field: the strength (0..1, or 1..127), and/or "a" (accent), "s"
         * (slide) for the 303: "4:C3:1:as", "8:F2:2:0.7".
         */
        fun entries(line: String): List<Entry> = line.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.map { e ->
            val f = e.split(':').also { require(it.size in 3..4) { "not step:Note:length[:strength]: $e" } }
            val step = f[0].toFloatOrNull()?.takeIf { it >= 0f } ?: error("not a step: $e")
            val length = f[2].toFloatOrNull()?.takeIf { it > 0f } ?: error("not a length: $e")
            val extra = f.getOrNull(3).orEmpty()
            val number = extra.filter { it.isDigit() || it == '.' }.takeIf { it.isNotEmpty() }?.toFloatOrNull()
            val flags = extra.filter { it.isLetter() }
            require(flags.all { it == 'a' || it == 's' }) { "not a/s: $e" }
            val velocity = number?.let { if (it > 1f) it / 127f else it }?.also { require(it in 0.05f..1f) { "strength out of range: $e" } }
            Entry(step, f[1].split('+').map(::pitchOf), length, velocity, 'a' in flags, 's' in flags)
        }

        /** A course's line as notes, at [velocity] where it says none. */
        fun parseLine(line: String, velocity: Float): List<Note> = entries(line).flatMap { e ->
            val v = e.velocity ?: if (e.accent) 1f else velocity
            e.pitches.map { Note(Math.round(e.step * NoteClip.TICKS_PER_STEP), Math.round(e.length * NoteClip.TICKS_PER_STEP), it, v) }
        }

        /** A 303 line ("0:A2:1 2:A3:1:as …") as its steps, by index, from the 303's [root] (its notes up to 3 octaves above). */
        fun parseAcid(line: String, root: Int): Map<Int, AcidStep> = entries(line).associate { e ->
            e.step.toInt() to acidStep(root, e.pitches.first(), e.accent, e.slide)
        }

        /** The 303 step that plays [pitch] from [root]. */
        fun acidStep(root: Int, pitch: Int, accent: Boolean, slide: Boolean): AcidStep {
            val d = pitch - root
            val octave = Math.floorDiv(d, 12).coerceIn(-1, 2)
            return AcidStep(note = (d - 12 * octave).coerceIn(0, 12), octave = octave, accent = accent, slide = slide, gate = true)
        }

        /** The pitch a 303 step plays from [root]. */
        fun acidPitch(root: Int, s: AcidStep): Int = root + s.note + 12 * s.octave

        /** The bars of a phrase whose last note starts at [lastStep]: 1, 2, 4 or 8. */
        fun phraseBarsOf(lastStep: Int): Int = listOf(1, 2, 4, 8).firstOrNull { lastStep < it * 16 } ?: 8

        /** The steps of [course]'s phrase of [role]: its [TutoCourse.lineBars], else the fewest bars holding [phrase]. */
        fun phraseSteps(course: TutoCourse, role: String, phrase: List<Note>): Int =
            16 * (course.lineBars[role] ?: phraseBarsOf((phrase.maxOfOrNull { it.start } ?: 0) / NoteClip.TICKS_PER_STEP))

        /**
         * [phrase] ([steps] long) played over [c]'s whole length, from its start; with [chords] (a
         * degree per bar), each bar moved along the key's scale from the chord it was written on to
         * the chord of the bar it lands on (« Copier » that follows the chords).
         */
        fun fitted(c: NoteClip, phrase: List<Note>, steps: Int = PHRASE_STEPS, chords: List<Int>? = null, key: Harmony? = null): NoteClip {
            val each = steps * NoteClip.TICKS_PER_STEP
            val bar = 16 * NoteClip.TICKS_PER_STEP
            val times = (c.ticks + each - 1) / each
            val notes = (0 until times).flatMap { k ->
                phrase.map { n ->
                    val at = n.start + k * each
                    val shift = if (chords == null || key == null || k == 0) 0 else nearest(chords[(at / bar) % chords.size] - chords[(n.start / bar) % chords.size], key)
                    n.copy(start = at, pitch = if (shift == 0) n.pitch else byDegrees(n.pitch, shift, key!!).coerceIn(0, 127))
                }
            }
            return c.copy(notes = notes.filter { it.start < c.ticks }).sanitized()
        }

        /**
         * A move of [degrees] along [key]'s scale towards the nearest note (a fifth up is a fourth
         * down: synthwave's bass goes down to G1, F1, D1 rather than up), within half the scale.
         */
        fun nearest(degrees: Int, key: Harmony): Int {
            val n = key.scale.intervals.size.takeIf { key.scale != com.pocketstudio.model.Scale.Off && it > 0 } ?: 12
            val d = Math.floorMod(degrees, n)
            return if (d > n / 2) d - n else d
        }

        /**
         * [pitch] moved [degrees] steps along [key]'s scale (2: a third up), a note out of the scale
         * by as many semitones as its root's degree moves; no scale: by semitones.
         */
        fun byDegrees(pitch: Int, degrees: Int, key: Harmony): Int {
            val ints = key.scale.intervals
            if (degrees == 0) return pitch
            if (key.scale == com.pocketstudio.model.Scale.Off || ints.isEmpty()) return pitch + degrees
            val n = ints.size
            val at = ints.indexOf(Math.floorMod(pitch - key.root, 12))
            val octave = Math.floorDiv(pitch - key.root, 12)
            fun degreePitch(o: Int, d: Int) = key.root + 12 * (o + Math.floorDiv(d, n)) + ints[Math.floorMod(d, n)]
            if (at < 0) return pitch + degreePitch(0, degrees) - degreePitch(0, 0)
            return degreePitch(octave, at + degrees)
        }

        private val cache = ConcurrentHashMap<String, TutoTarget>()
        fun of(style: String): TutoTarget? = StarterWizard.style(style)?.let { s -> cache.getOrPut(style) { TutoTarget(s) } }

        /** Tests: [style]'s target made again (its course changed). */
        internal fun forget(style: String) { cache.remove(style) }
    }
}

/** « Écouter l'exemple »: [after] heard, after [before] when the step compares two parts. */
class TutoExample(val before: Project?, val after: Project)

/**
 * A block of the song's map: a part's [label], its [bars] and its [energy] (0..1: how many of its
 * bricks play) at its start, [energyEnd] at its end (the Montée climbs to its Drop).
 */
data class TutoMapBlock(val label: String, val bars: Int, val energy: Float, val energyEnd: Float = energy)

/** What a step needs before the user can do it ([TutoChecks.need]); the panel's main button meets it. */
sealed class TutoNeed {
    /** The drum loop at least [bars] bars long (« Passer à 2 mesures »). */
    data class Bars(val bars: Int) : TutoNeed()
    /** A track for [role] ("bass", "pad", "lead", "sub"): « Créer la piste Basse ». */
    data class Track(val role: String) : TutoNeed()
    /** The part [label], made from the Groove as the step says: « Créer la partie « Intro » ». */
    data class Part(val label: String) : TutoNeed()
    /** The main loop named [label]. */
    data class Name(val label: String) : TutoNeed()
    /** Each part of the song its advised length. */
    data object Lengths : TutoNeed()
    /** The part [label] shown (the screens edit it). */
    data class Scene(val label: String) : TutoNeed()
    /**
     * « Copier sur les mesures suivantes »: what the user wrote of [role]'s line (its first 4 bars,
     * the hats' first bar) repeated over the next bars (testers, lot « tuto 4 »: the creative
     * gesture is the user's, the copying a button).
     */
    data class Copy(val role: String) : TutoNeed()
}

/** What a frame of « Montre-moi » sounds: pad [lane], or [pitches] on track [track]. */
data class TutoSound(val lane: Int? = null, val track: Int? = null, val pitches: List<Int> = emptyList())

/** « Montre-moi »: [frames] shown one after the other, each with its [sounds] entry (null: silent). */
class TutoDemo(val frames: List<Project>, val sounds: List<TutoSound?>)

/**
 * The detectors of the steps ([done], tolerant: a pattern close enough passes), « Montre-moi »
 * ([show]: the step's target written into the project, null when the step has none) and the
 * examples ([example]: sandboxes made from the target, the user's project untouched).
 */
object TutoChecks {
    /** The pads of each drum role ([StarterWizard.Role]'s lanes; the open hat counts as a hat). */
    private val ROLE_LANES = mapOf("kick" to setOf(0, 1), "snare" to setOf(2, 3), "clap" to setOf(4), "hat" to setOf(5, 6))
    /** Every drum role a course can name ([StarterWizard.Role] ids: "kick", "open_hat", "ride", "rumble", "808"…). */
    val DRUM_ROLES: List<String> = StarterWizard.Role.entries.map { it.id }
    const val CRASH_LANE = 12
    private const val LOW = 52
    /**
     * The Montée's last cells left silent before the Drop (the gap step's): its last beat (Théo, lot
     * « tuto 5 »: two 16ths, 86 ms, is no silence; in jump up a beat is cut, or a bar).
     */
    const val GAP_CELLS = 4
    /** The last roll before the silence: this many hits in its cell or more (the build's own rolls are ×2). */
    private const val FILL_REPEATS = 3
    /** How much a drawn curve must climb to « monter ». */
    private const val RISE = 0.2f
    /** The Guide's card the app marks done once the song is exported and its share sheet opened. */
    const val SHARED_CARD = "share.ready"
    /** The open hat's pad (« Charley ouvert »). */
    private const val OPEN_HAT_LANE = 6

    /** What a check is about: "lane:hat:rolls" → "hat", "notes:bass" → "bass", "send:0" → "0". */
    fun argOf(check: String): String = check.substringAfter(':', "").let { if (check.startsWith("lane:")) it.substringBefore(':') else it }

    fun laneOf(role: String): Int = StarterWizard.Role.entries.firstOrNull { it.id == role }?.lane ?: 0

    fun roleOfLane(lane: Int): String = ROLE_LANES.entries.firstOrNull { lane in it.value }?.key ?: "perc"

    /**
     * The pads of drum role [role] in [target]'s style: its own pad, and the pads that stand in for
     * it (the second kick, the open hat for the hats) when no other line of the style and no studio
     * voice plays there (techno's open hat has its own line: the hats are its pad alone; trap's
     * second kick is the 808; techno's rumble is on the second snare's).
     */
    fun lanesOf(role: String, target: TutoTarget?): Set<Int> {
        val base = lanesOf(role)
        if (target == null) return base
        val own = laneOf(role)
        val taken = target.style.lines.filter { it.role.id != role }.map { it.role.lane }.toSet() + target.assistantSong.padVoices.keys.filter { it != own }
        return base.filter { it == own || it !in taken }.toSet().ifEmpty { setOf(own) }
    }

    /** The drum role pad [lane] plays in [target]'s style: the line's on its pad, else the role it stands in for, else "perc". */
    fun roleOfLane(lane: Int, target: TutoTarget?): String {
        if (target == null) return roleOfLane(lane)
        target.style.lines.firstOrNull { it.role.lane == lane }?.let { return it.role.id }
        return ROLE_LANES.keys.firstOrNull { lane in lanesOf(it, target) } ?: "perc"
    }

    /** A signature of what a "changed:…" step watches (its value when the step began is the baseline). */
    fun signature(what: String, p: Project): Any? = when {
        what == "volumes" || what.startsWith("volumes:") -> p.mixer.channels.map { it.volume }
        what == "kit" -> p.drums.lanes.getOrNull(0)?.sample to p.padVoices[0]
        what.startsWith("synth:") -> trackFor(p, what.removePrefix("synth:").substringBefore(':'))?.let { id -> p.track(id).let { listOf(it.synth, it.plaits, it.engine) } }
        // The hits of every pad in the part named so (« change un détail » in the Drop 2).
        what.startsWith("drums:") -> sceneLabelled(p, what.removePrefix("drums:")).takeIf { it >= 0 }?.let { i -> sceneAt(p, i).drums.lanes.map { it.hits } }
        else -> null
    }

    /** The synth knobs a "changed:synth:<role>:<KNOB>+<KNOB>" step turns up, by their [SynthPatch] names. */
    private val KNOBS = mapOf(
        "CUTOFF" to SynthPatch.CUTOFF, "RESONANCE" to SynthPatch.RESONANCE, "OSC2_DETUNE" to SynthPatch.OSC2_DETUNE,
        "GLIDE" to SynthPatch.GLIDE, "A_ATTACK" to SynthPatch.A_ATTACK, "UNISON_DETUNE" to SynthPatch.UNISON_DETUNE,
    )

    /**
     * How much a knob must go up to count (or be near its top already): « au moins 15 » on its 0-100
     * scale (Théo, lot « tuto 5 »: ticked at the first touch, no value said).
     */
    private const val KNOB_STEP = 0.15f

    /** The knobs of a "changed:synth:…" check (empty: any change counts) and its role. */
    private fun knobsOf(check: String): Pair<String, List<Int>> {
        val spec = check.removePrefix("changed:synth:")
        return spec.substringBefore(':') to spec.substringAfter(':', "").split('+').mapNotNull { KNOBS[it.trim()] }
    }

    /** The tracks' strips a "changed:volumes:pad,lead" step lowers (the roles that have a track). */
    private fun volumeChannels(p: Project, check: String): List<Int> =
        check.removePrefix("changed:volumes").removePrefix(":").split(',').filter { it.isNotBlank() }
            .mapNotNull { stripOf(p, it.trim()) }

    /**
     * The strips a "send:<bus>" step sends: the roles of its [TutoStep.target] when it names several
     * ("pad,clap": a track's or a pad's strip each), else the reverb's the « Accords » track and the
     * snare's pad, the echo's the lead.
     */
    private fun sendChannels(p: Project, bus: Int, step: TutoStep? = null): List<Int> {
        val roles = step?.target?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty()
        // Any target but the jump up's own reverb (« pad »: its chords and its snare) names the strips (the echo on the bass alone).
        if (roles.isNotEmpty() && !(bus == 0 && roles == listOf("pad"))) return roles.mapNotNull { r -> stripOf(p, r) }
        return if (bus == 0) listOfNotNull(trackFor(p, "pad")?.let(InstrumentTrack::mixerChannel), Mixer.FIRST_PAD + laneOf("snare"))
        else listOfNotNull(trackFor(p, "lead")?.let(InstrumentTrack::mixerChannel))
    }

    /** The strip of [role]: its track's ("bass", "pad", "lead", "sub"), the 303's ("acid") or its pad's ("snare"…); null when none. */
    fun stripOf(p: Project, role: String): Int? = when {
        role == TutoTarget.ACID -> AutoTarget.ACID_CHANNEL
        role in DRUM_ROLES -> Mixer.padChannel(laneOf(role))
        else -> trackFor(p, role)?.let(InstrumentTrack::mixerChannel)
    }

    /** The tracks' strips an "insert:<EFFECT>" step puts its effect on: each role of [TutoStep.target] ("sub,bass") that has a track. */
    private fun insertChannels(p: Project, step: TutoStep): List<Int> =
        step.target.ifEmpty { "bass" }.split(',').mapNotNull { stripOf(p, it.trim()) }

    private fun hasInsert(p: Project, ch: Int, effect: String): Boolean =
        p.mixer.channels.getOrNull(ch)?.inserts?.any { !it.bypass && it.effect.name == effect } == true

    private fun sends(p: Project, ch: Int, bus: Int): Boolean = (p.mixer.channels.getOrNull(ch)?.sends?.getOrNull(bus) ?: 0f) > 0.05f

    /**
     * The strips a send or an effect step still has to do, in the order its text names them (testers,
     * lot « tuto 4 »: the reverb's « Accords » done, « Je le fais » went back there, never to the
     * snare); empty when done or for another kind of step. « Je le fais » goes to the first.
     */
    fun channelsToDo(step: TutoStep, p: Project): List<Int> {
        val c = step.check
        val arg = argOf(c)
        return when {
            c.startsWith("send:") -> arg.toIntOrNull()?.let { bus -> sendChannels(p, bus, step).filterNot { sends(p, it, bus) } }.orEmpty()
            c.startsWith("insert:") -> insertChannels(p, step).filterNot { hasInsert(p, it, arg) }
            else -> emptyList()
        }
    }

    // ---- what the user's project holds ----

    /** Scene [i] as it is now (the one shown: its live content). */
    private fun sceneAt(p: Project, i: Int): Scene = if (i == p.scene) p.currentSceneContent() else p.allScenes[i]

    /** Track [id]'s notes in scene [i] only. */
    private fun sceneNotes(p: Project, i: Int, id: Int): List<Note> =
        if (i == p.scene) p.track(id).notes.notes else p.allScenes.getOrNull(i)?.let { s -> (if (id == 0) s.synthNotes else s.trackNotes[id])?.notes }.orEmpty()

    private fun notesOf(p: Project, id: Int): List<Note> =
        p.allScenes.flatMap { s -> (if (id == 0) s.synthNotes else s.trackNotes[id])?.notes.orEmpty() }
            .ifEmpty { p.track(id).notes.notes }

    private fun median(notes: List<Note>): Int = notes.map { it.pitch }.sorted().let { it[it.size / 2] }

    /** What track [id] plays: "bass", "sub", "pad", "lead", "pad2" (« Nappe »), "counter" (« Contre-chant ») by its name, else by its notes; null with neither. */
    fun roleOf(p: Project, id: Int): String? {
        namedRole(p, id)?.let { return it }
        val notes = notesOf(p, id).ifEmpty { return null }
        val chords = notes.groupBy { it.start }.values.any { it.size >= 2 }
        return when {
            median(notes) < LOW -> "bass"
            chords -> "pad"
            else -> "lead"
        }
    }

    /** What track [id] plays by its name alone ("bass", "sub", "pad", "lead"), null when its name does not say. */
    private fun namedRole(p: Project, id: Int): String? {
        val name = p.track(id).name.lowercase()
        return when {
            "sub" in name -> "sub"
            // The tutorial's second pad and counter-melody (their own tracks, beside the chords and the lead).
            "contre" in name -> TutoTarget.COUNTER
            name.trim() == "nappe" -> TutoTarget.PAD2
            listOf("bass", "basse", "808").any { it in name } -> "bass"
            listOf("accord", "nappe", "pad", "chord").any { it in name } -> "pad"
            listOf("mélodie", "melod", "lead", "hoover").any { it in name } -> "lead"
            else -> null
        }
    }

    /** The track playing [role] (one with notes first), null when none does. */
    fun trackFor(p: Project, role: String): Int? {
        val ids = p.instrumentTracks.map { it.id }.filter { roleOf(p, it) == role }
        return ids.firstOrNull { notesOf(p, it).isNotEmpty() } ?: ids.firstOrNull()
    }

    /** What plays in scene [s]: its pads with hits, its tracks with notes, the 303. */
    fun bricks(s: Scene): Set<String> = buildSet {
        s.drums.lanes.forEachIndexed { i, l -> if (l.hits.keys.any { it < s.drums.steps }) add("pad:$i") }
        if (s.synthNotes.notes.isNotEmpty()) add("inst:0")
        s.trackNotes.forEach { (id, c) -> if (c.notes.isNotEmpty()) add("inst:$id") }
        if (s.acid.steps.take(s.acid.length.coerceAtLeast(0)).any { it.gate }) add("acid")
    }

    private fun labelIs(label: String, wanted: String): Boolean {
        val l = label.trim()
        return l.equals(wanted, ignoreCase = true) || l.equals(Scene.shownLabel(wanted), ignoreCase = true) ||
            (wanted == "Fin" && l.equals("Outro", ignoreCase = true))
    }

    /** The scene named [label], empty or not (a part whose pads are all to write), -1 when none. */
    private fun namedScene(p: Project, label: String): Int = sceneLabelled(p, label).takeIf { it >= 0 } ?: p.allScenes.indexOfFirst { labelIs(it.label, label) }

    private fun sceneLabelled(p: Project, label: String): Int = p.allScenes.indexOfFirst { labelIs(it.label, label) && !it.isEmpty }

    /** The user's main loop: the scene named Groove, else the one where the most plays. */
    private fun grooveOf(p: Project): Int {
        val all = p.allScenes
        return sceneLabelled(p, "Groove").takeIf { it >= 0 } ?: all.indices.maxByOrNull { bricks(all[it]).size } ?: 0
    }

    /**
     * The hits of a drum role in [d]: EVERY dotted cell (the target's line over the whole loop,
     * as « Je le fais » shows it) holds a hit, a few more tolerated (testers, 2026-10-28: the hats
     * ticked at 7 cells of 9, the last two never made). The loop must be as long as the line first
     * (« Passer à 2 mesures »), in Facile as in Pro: its second bar's cells are cells to fill too.
     */
    private fun laneMatches(d: DrumPattern, lanes: Set<Int>, line: StarterWizard.Line, perBar: Int, rolls: Boolean = false): Boolean {
        if (d.steps < line.pattern.length.coerceAtMost(4 * perBar.coerceAtLeast(1))) return false
        val wanted = StarterWizard.hits(line, d.steps).ifEmpty { return false }
        val hits = HashMap<Int, Hit>()
        lanes.forEach { l -> d.lanes.getOrNull(l)?.hits?.forEach { (s, h) -> if (s < d.steps) hits[s] = h } }
        val mine = hits.keys
        if (!(mine.containsAll(wanted.keys) && (mine - wanted.keys).size <= maxOf(1, wanted.size / 2))) return false
        // "lane:hat:rolls": each roll of the line (trap's hats, a « 3 »: three hits in one cell) rolled there too.
        return !rolls || wanted.all { (s, h) -> (h.repeats < 2 || (hits[s]?.repeats ?: 1) == h.repeats) && (hits[s]?.span ?: 1) == h.span }
    }

    /** Whether one of [lanes] holds a triplet ([Hit.isTriplet]) inside the loop. */
    private fun hasTriplet(d: DrumPattern, lanes: Set<Int>): Boolean =
        lanes.any { l -> d.lanes.getOrNull(l)?.hits?.any { (s, h) -> s < d.steps && h.isTriplet } == true }

    /**
     * « Montre-moi » of "triplet:<role>": the triplets of the style's line on that pad where they fit
     * (trap's hats: the last beat of the first bar), else one on the first bar's last beat: 3 hits over
     * its first 2 cells (a hit added there when there is none). Null when the loop has no room.
     */
    private fun withTriplet(p: Project, role: String, target: TutoTarget): Project? {
        val lane = laneOf(role)
        val steps = p.drums.steps
        val fromLine = target.line(role)?.let { StarterWizard.hits(it, steps) }.orEmpty().filter { (s, h) -> h.isTriplet && s + h.span <= steps }
        val wanted = fromLine.ifEmpty {
            val at = (p.stepsPerBar - 4).coerceAtLeast(0)
            mapOf(at to Hit(p.drums.lanes[lane].hits[at]?.velocity ?: 0.6f, repeats = Hit.TRIPLET.first, span = Hit.TRIPLET.second))
        }
        var d = p.drums
        for ((s, h) in wanted.entries.sortedBy { it.key }) {
            d = d.setHit(lane, s, true, h.velocity).withSpread(lane, s, h.repeats, h.span)
        }
        return p.copy(drums = d).takeIf { hasTriplet(d, setOf(lane)) }
    }

    /** The notes [mine] hold every one of [wanted] (its pitch, its start): the dotted notes all written. */
    private fun notesMatch(mine: List<Note>, wanted: List<Note>): Boolean {
        if (wanted.isEmpty()) return false
        val have = mine.map { it.pitch to it.start }.toSet()
        return wanted.all { (it.pitch to it.start) in have }
    }

    /**
     * Each bar of a pattern whose 4 hats on the beats are all softer (by 0.15) than the 4 between
     * them: true, false (a bar of hats that does not swing), in bar order.
     */
    private fun hatBars(d: DrumPattern, perBar: Int, target: TutoTarget? = null): List<Boolean> {
        val beat = (perBar / 4).coerceAtLeast(2)
        val v = HashMap<Int, Float>()
        lanesOf("hat", target).forEach { l -> d.lanes.getOrNull(l)?.hits?.forEach { (s, h) -> if (s < d.steps) v[s] = maxOf(v[s] ?: 0f, h.velocity) } }
        return (0 until d.steps step (4 * beat)).map { bar ->
            val on = (0 until 4).map { bar + it * beat }
            val off = on.map { it + beat / 2 }
            on.all { it in v } && off.all { it in v } && on.maxOf { v.getValue(it) } + 0.15f <= off.minOf { v.getValue(it) }
        }
    }

    /** Every bar of the pattern swings (testers, lot « tuto 4 »: the first bar alone was ticked, the second kept its loud beats). */
    private fun hatsSwing(d: DrumPattern, perBar: Int, target: TutoTarget?): Boolean = hatBars(d, perBar, target).let { it.isNotEmpty() && it.all { b -> b } }

    private fun lanesOf(role: String) = ROLE_LANES[role] ?: setOf(laneOf(role))

    /** Whether [step] is done in [p] ([baseline]: the [signature] when it began, for "changed:…"). */
    fun done(step: TutoStep, p: Project, target: TutoTarget, baseline: Any? = null): Boolean {
        val c = step.check
        val arg = argOf(c)
        return when {
            step.manual || step.listening -> false
            c == "tempo" -> kotlin.math.abs(p.tempoBpm - target.style.bpm) <= 2.0
            c == "key" -> p.harmony.root == target.harmony.root && p.harmony.scale == target.harmony.scale
            // The style's swing (house, garage, boom bap, lo-fi), within 5 points of it.
            c == "swing" -> target.style.swing > 0.0 && kotlin.math.abs(p.swing - target.style.swing) <= minOf(SWING_TOLERANCE, target.style.swing / 2)
            // With a [TutoStep.part]: the course's drums of that part, in that part.
            c.startsWith("lane:") -> target.line(arg, partOf(step))?.let { line ->
                scenesOf(p, partOf(step)).any { laneMatches(it.drums, lanesOf(arg, target), line, p.stepsPerBar, rolls = c.endsWith(":rolls")) }
            } ?: false
            // The 4 hats on the beats softer than those between them (one hit made « Doux » is not the groove).
            c == "velocity:hat" -> p.allScenes.any { hatsSwing(it.drums, p.stepsPerBar, target) }
            // "offset:snare:late": at least half the role's hits played late (early), by [OFFSET_MS] or more (the lo-fi's lazy snare).
            c.startsWith("offset:") -> p.allScenes.any { sc -> offsetDone(sc.drums, lanesOf(arg.substringBefore(':'), target), arg.substringAfter(':', "late") == "early") }
            // "triplet:hat": at least one triplet (3 hits over 2 cells, or 6 over a beat) on the role's pad, in any part.
            c.startsWith("triplet:") -> p.allScenes.any { sc -> hasTriplet(sc.drums, lanesOf(arg, target)) }
            // The 303's line: every step of it, its note, its accent and its slide.
            c == "notes:acid" -> target.acid?.let { a -> p.allScenes.any { acidMatches(it.acid, a) } } ?: false
            // Every dotted note written (not 3 of 12: the testers' bass and chords ticked with the screen's first notes).
            c.startsWith("notes:") -> target.roleNotes(arg)?.let { wanted ->
                trackFor(p, arg)?.let { id -> notesMatch(notesOf(p, id), wanted.notes) }
            } ?: false
            c == "track:sub" -> trackFor(p, "bass")?.let { bass ->
                val wanted = subOf(notesOf(p, bass))
                p.instrumentTracks.any { t -> t.id != bass && roleOf(p, t.id) == "sub" && notesMatch(notesOf(p, t.id), wanted) }
            } ?: false
            // On each track the step names (the pump on the Sub and the Basse), not anywhere in the mix.
            c.startsWith("insert:") -> insertChannels(p, step).let { chans -> chans.isNotEmpty() && chans.all { hasInsert(p, it, arg) } }
            // Every strip the step names sends (the « Accords » track AND the snare), not any strip.
            c.startsWith("send:") -> arg.substringBefore(':').toIntOrNull()?.let { bus -> sendChannels(p, bus, step).let { chans -> chans.isNotEmpty() && chans.all { sends(p, it, bus) } } } ?: false
            // A curve drawn over the part (b/280: « Par partie » touched, no curve, ticked): rising, falling or up and down.
            c == "automation" || c.startsWith("automation:") -> curveDrawn(p, step.target.ifEmpty { "pad" }, partsOf(step).first(), arg.ifEmpty { RISE_SHAPE })
            c == "master" -> p.mixer.master.any { it.effect != EffectType.NONE && !it.bypass }
            // The master's « Volume visé » (its limiter, always last, brings the song there).
            c.startsWith("loudness:") -> p.mixer.target.name == arg
            // The song exported and its share sheet opened (the app marks the Guide's « Prêt à partager ? » then), not « C'est fait ».
            c == "shared" -> p.guide.statusOf(SHARED_CARD) == GuideStatus.DONE
            // Each track the step names a little lower than when the step began (the testers' mix ticked with one of two).
            c.startsWith("changed:volumes:") -> (baseline as? List<*>)?.let { base ->
                volumeChannels(p, c).let { chans ->
                    chans.isNotEmpty() && chans.all { ch -> (base.getOrNull(ch) as? Float)?.let { p.mixer.channels.getOrNull(ch)?.volume?.let { v -> v < it - 0.005f } } == true }
                }
            } ?: false
            // The knobs the step names turned up since it began (not the distortion the synth has none of).
            c.startsWith("changed:synth:") && knobsOf(c).second.isNotEmpty() -> {
                val (role, knobs) = knobsOf(c)
                val before = (baseline as? List<*>)?.firstOrNull() as? SynthPatch
                val id = trackFor(p, role)
                before != null && id != null && knobs.all { k -> p.track(id).synth.get(k).let { it >= before.get(k) + KNOB_STEP || it >= 0.95f } }
            }
            // Théo (lot « tuto 5 »): « percussion ou charley ouvert », not any hit (a closed hat ticked it): a new hit on one of those pads.
            c.startsWith("changed:drums:") -> (baseline as? List<*>)?.let { base ->
                (signature(arg, p) as? List<*>)?.withIndex()?.any { (i, now) ->
                    i in variationLanes() && ((now as? Map<*, *>)?.keys.orEmpty() - (base.getOrNull(i) as? Map<*, *>)?.keys.orEmpty()).isNotEmpty()
                }
            } == true
            c.startsWith("changed:") -> baseline != null && signature(arg, p) != baseline
            c.startsWith("label:") -> sceneLabelled(p, arg) >= 0
            c.startsWith("part:") -> if (writesBass(step)) newBassDone(p, step, target) else partDone(p, c.removePrefix("part:"), target)
            c == "song:order" -> songOrdered(p, step.song)
            // Each part its advised length (all at 8 bars, as « Ajouter » makes them, is not the step done).
            // Testers (lot « tuto 5 »: « Bravo » on the durations with the parts in a wrong order): the order too.
            c == "song:phrases" -> songOrdered(p, step.song) && phrasesDone(p, step.song)
            // A roll on the snare in the Montée's last bar (a Drop 2 copied from the example rolls too: it does not count),
            // before its last two cells: those are the silence of the next step (testers, lot « tuto 4 »: the roll made, then erased).
            // Lot « tuto 5 »: the build already rolls ×2 in its last bar; the user's roll is the last one, ×3 or more, in the beat before the silence.
            c == "fill" -> sceneLabelled(p, partsOf(step).first()).takeIf { it >= 0 }?.let { i ->
                val d = sceneAt(p, i).drums
                val beat = (p.stepsPerBar / 4).coerceAtLeast(1)
                val end = d.steps - fillEnd(step, target)
                val window = (end - beat) until end
                val hits = lanesOf(fillRole(step), target).flatMap { l -> d.lanes.getOrNull(l)?.hits?.filterKeys { it in window }?.values.orEmpty() }
                // A roll ×3 or more; a tom fill (a pad the part does not build up): 3 hits or more in that beat.
                hits.any { it.repeats >= FILL_REPEATS } || (!buildsIn(step, target) && hits.size >= FILL_REPEATS)
            } ?: false
            // The crash on the first beat of the part (or of one of the parts) the step names: a hit once, at its entry.
            c == "crash:once" -> partsOf(step).let { parts -> parts.any { sceneLabelled(p, it) >= 0 } && parts.filter { sceneLabelled(p, it) >= 0 }.all { crashOnceDone(p, it) } }
            c == "crash" -> p.allScenes.any { s -> partsOf(step).any { labelIs(s.label, it) } && s.drums.lanes.getOrNull(CRASH_LANE)?.hits?.containsKey(0) == true }
            c == "gap" -> sceneLabelled(p, partsOf(step).first()).takeIf { it >= 0 }?.let { i ->
                val d = sceneAt(p, i).drums
                val silent = gapLanes(step, target)
                d.lanes.withIndex().none { (k, l) -> k in silent && l.hits.keys.any { it in d.steps - gapCells(step, target) until d.steps } }
            } ?: false
            else -> false
        }
    }

    /** A hit counts as played early or late from this many ms off its cell. */
    const val OFFSET_MS = 5f
    /** The offset « Montre-moi » gives. */
    private const val SHOWN_OFFSET_MS = 15f

    private fun offsetDone(d: DrumPattern, lanes: Set<Int>, early: Boolean): Boolean {
        val hits = lanes.flatMap { l -> d.lanes.getOrNull(l)?.hits?.filterKeys { it < d.steps }?.values.orEmpty() }
        if (hits.isEmpty()) return false
        return hits.count { if (early) it.offset <= -OFFSET_MS else it.offset >= OFFSET_MS } * 2 >= hits.size
    }

    /** The swing that counts as the style's (its value, give or take this much). */
    const val SWING_TOLERANCE = 0.05

    /** A curve's shapes: "rise" (the Montée's), "fall", "wave" (up then down again in the part: the techno filter's cycle). */
    const val RISE_SHAPE = "rise"
    val CURVE_SHAPES = setOf(RISE_SHAPE, "fall", "wave")

    /**
     * The parts [step] works in ([TutoStep.part], comma separated): by default the Montée for the
     * fill, the gap and the curve, the Drop and the Drop 2 for the crash.
     */
    fun partsOf(step: TutoStep): List<String> = step.part.split(',').map { it.trim() }.filter { it.isNotEmpty() }.ifEmpty {
        if (step.check.startsWith("crash")) listOf("Drop", "Drop 2") else listOf("Montée")
    }

    /** The part a drum step works in ([TutoStep.part], its first), null for one of the whole loop. */
    private fun partOf(step: TutoStep): String? = step.part.split(',').map { it.trim() }.firstOrNull { it.isNotEmpty() }

    /** The scenes of [p] named [label] as they are now; every scene when [label] is null. */
    private fun scenesOf(p: Project, label: String?): List<Scene> =
        p.allScenes.indices.filter { label == null || labelIs(p.allScenes[it].label, label) }.map { sceneAt(p, it) }

    /** The pad a fill rolls on: its [TutoStep.target] when it is a drum role, else the snare. */
    fun fillRole(step: TutoStep): String = step.target.takeIf { it in DRUM_ROLES } ?: "snare"

    /**
     * The cells left silent before the Drop: a beat ([GAP_CELLS]) at the jump up's speed; a slow
     * style's beat is long, half of it there (a lo-fi at 84 BPM: two 16ths, 357 ms).
     */
    fun gapCells(step: TutoStep, target: TutoTarget): Int = step.length.takeIf { it > 0 } ?: if (target.style.bpm < SLOW_BPM) GAP_CELLS / 2 else GAP_CELLS

    /** The cells a fill ends before: its own [TutoStep.length], else its part's gap step's silence, else none. */
    fun fillEnd(step: TutoStep, target: TutoTarget): Int = step.length.takeIf { it > 0 } ?: gapStep(target, partsOf(step).first())?.let { gapCells(it, target) } ?: 0

    /** The course's gap step in the part [label], null when none. */
    fun gapStep(target: TutoTarget, label: String): TutoStep? =
        TutoCourse.of(target.style.id)?.chapters?.flatMap { it.steps }?.firstOrNull { it.check == "gap" && labelIs(partsOf(it).first(), label) }

    /** The pads a gap step silences: those of its [TutoStep.target]'s drum roles, every pad when it names none. */
    fun gapLanes(step: TutoStep, target: TutoTarget?): Set<Int> {
        val roles = step.target.split(',').map { it.trim() }.filter { it in DRUM_ROLES }
        return if (roles.isEmpty()) (0 until DrumPattern.PAD_COUNT).toSet() else roles.flatMap { lanesOf(it, target) }.toSet()
    }

    /** Whether the fill's pad is the one its part's build accelerates (the Montée's snare: a roll, not more hits, makes its fill). */
    private fun buildsIn(step: TutoStep, target: TutoTarget): Boolean =
        target.buildStep?.let { b -> b.build == fillRole(step) && labelIs(partLabel(b), partsOf(step).first()) } ?: (fillRole(step) == "snare")

    /** Under this tempo the silence before a part is half a beat. */
    private const val SLOW_BPM = 100.0

    /** The 303 line [mine] plays every step of [wanted] (its note, its accent, its slide), as long or longer. */
    private fun acidMatches(mine: AcidPattern, wanted: AcidPattern): Boolean {
        if (mine.length < wanted.length) return false
        val steps = wanted.steps.take(wanted.length).withIndex().filter { it.value.gate }.ifEmpty { return false }
        return steps.all { (i, w) ->
            val m = mine.steps.getOrNull(i) ?: return@all false
            m.gate && TutoTarget.acidPitch(mine.root, m) == TutoTarget.acidPitch(wanted.root, w) && m.accent == w.accent && m.slide == w.slide
        }
    }

    /**
     * "Intro:less", "Montée:nokick", "Drop:full", "Drop 2:differs", "Couplet:nopad"… several joined
     * by "+" ("Couplet:nopad+less"): each must hold. [PART_MODES] lists them.
     */
    private fun partDone(p: Project, spec: String, target: TutoTarget): Boolean {
        val label = spec.substringBeforeLast(':')
        val modes = spec.substringAfterLast(':').split('+')
        val i = sceneLabelled(p, label).takeIf { it >= 0 } ?: return false
        // The scene shown as it is now (not as last stored).
        val all = p.allScenes.indices.map { sceneAt(p, it) }
        val mine = bricks(all[i])
        val most = all.filterIndexed { j, _ -> j != i }.maxOfOrNull { bricks(it).size } ?: 0
        fun plays(role: String) = trackFor(p, role)?.let { "inst:$it" in mine } == true
        val kick = lanesOf("kick", target)
        return mine.isNotEmpty() && modes.all { mode ->
            when (mode) {
                "any" -> true
                "less" -> mine.size < most
                "nokick" -> kick.none { "pad:$it" in mine }
                "full" -> mine.size >= most && kick.any { "pad:$it" in mine }
                // Yanis (lot « tuto 4 »): a jump up Drop empties the top for the bass, the « Accords » silent.
                "drop" -> kick.any { "pad:$it" in mine } && plays("bass") && trackFor(p, "pad")?.let { "inst:$it" !in mine } != false
                "nobass" -> trackFor(p, "bass")?.let { "inst:$it" !in mine } != false && p.instrumentTracks.none { roleOf(p, it.id) == "sub" && "inst:${it.id}" in mine }
                "nopad" -> trackFor(p, "pad")?.let { "inst:$it" !in mine } != false
                "nolead" -> trackFor(p, "lead")?.let { "inst:$it" !in mine } != false
                "noacid" -> "acid" !in mine
                "bass", "pad", "lead" -> plays(mode)
                "acid" -> "acid" in mine
                // The part it is a second one of (« Drop 2 » of the Drop, « Refrain 2 » of the Refrain), not the same.
                "differs" -> sceneLabelled(p, TutoTarget.STEM.matchEntire(label.trim())?.groupValues?.get(1) ?: "Drop").let { d ->
                    d >= 0 && d != i && all[d].copy(name = "", label = "") != all[i].copy(name = "", label = "")
                }
                else -> false
            }
        }
    }

    /** What a "part:<label>:<mode>" step can ask, each joined to others by "+". */
    val PART_MODES = setOf("any", "less", "nokick", "full", "drop", "nobass", "nopad", "nolead", "noacid", "bass", "pad", "lead", "acid", "differs")

    /**
     * The Drop 2's own bass, written by the user (Yanis, lot « tuto 4 »: the jump up's first creative
     * gesture was the button's): in the part, every note of the course's bass moved by
     * [TutoStep.transpose], and the Sub an octave under it when there is one.
     */
    private fun newBassDone(p: Project, step: TutoStep, target: TutoTarget): Boolean {
        val i = sceneLabelled(p, partLabel(step)).takeIf { it >= 0 } ?: return false
        val wanted = movedBass(step, target)
        val bass = trackFor(p, "bass") ?: return false
        if (!notesMatch(sceneNotes(p, i, bass), wanted)) return false
        val sub = p.instrumentTracks.firstOrNull { it.id != bass && roleOf(p, it.id) == "sub" }?.id ?: return true
        return notesMatch(sceneNotes(p, i, sub), subOf(wanted))
    }

    /**
     * The course's bass moved by the step's [TutoStep.transpose] (the Drop 2's), along the key's scale
     * (Théo, lot « tuto 5 »: a fourth up in semitones made the Sol♭ a Si♮, out of Fa phrygien).
     */
    private fun movedBass(step: TutoStep, target: TutoTarget): List<Note> =
        target.notes(Part.Bass).notes.map { it.copy(pitch = diatonic(it.pitch, step.transpose, target.harmony).coerceIn(0, 127)) }

    /**
     * [pitch] moved by [semitones] counted from the key's root (5: a fourth), each note of the scale
     * kept in the scale (the same number of its degrees); a note out of it, or no scale: in semitones.
     */
    fun diatonic(pitch: Int, semitones: Int, key: com.pocketstudio.model.Harmony): Int {
        val ints = key.scale.intervals
        val degrees = ints.indexOf(Math.floorMod(semitones, 12))
        val at = ints.indexOf(Math.floorMod(pitch - key.root, 12))
        if (key.scale == com.pocketstudio.model.Scale.Off || degrees < 0 || at < 0) return pitch + semitones
        val octave = Math.floorDiv(pitch - key.root, 12) + Math.floorDiv(semitones, 12)
        val d = at + degrees
        return key.root + 12 * (octave + d / ints.size) + ints[d % ints.size]
    }

    /**
     * The Sub under [bass]: an octave down, its octave jumps folded into one octave (Théo, lot
     * « tuto 5 »: a sub stays still, it does not jump with the bass).
     */
    fun subOf(bass: List<Note>): List<Note> {
        val sub = StarterWizard.subNotes(bass)
        val low = sub.minOfOrNull { it.pitch } ?: return sub
        return sub.map { n -> n.copy(pitch = generateSequence(n.pitch) { it - 12 }.first { it <= low + 11 }) }
    }

    /** The pads a variation adds a hit to: the open hat and the percussions (not the crash). */
    private fun variationLanes(): Set<Int> = (0 until DrumPattern.PAD_COUNT).filter { it == OPEN_HAT_LANE || (roleOfLane(it) == "perc" && it != CRASH_LANE) }.toSet()

    private fun partLabel(step: TutoStep): String = step.check.removePrefix("part:").substringBeforeLast(':')

    /** A part step whose bass the user writes ([TutoStep.transpose]: the Drop 2's new bass). */
    private fun writesBass(step: TutoStep): Boolean = step.check.startsWith("part:") && step.transpose != 0

    /**
     * A curve on [role]'s filter cutoff (its track's filter, its synth's cutoff, the 303's cutoff)
     * drawn over the part [label] (its own curve, or the song's over its bars), of [shape]: "rise"
     * (the Montée's), "fall", "wave" (up then down again: a filter that opens and closes in a long part).
     */
    private fun curveDrawn(p: Project, role: String, label: String, shape: String): Boolean {
        val part = sceneLabelled(p, label)
        val spans = spansOf(p, label)
        fun ok(points: List<Float?>): Boolean = shaped(points.filterNotNull(), shape)
        return p.automation.any { l ->
            cutoffOf(p, l.target, role) && when (l.mode) {
                AutomationLane.PER_SCENE -> part >= 0 && ok(l.sceneValues[part].orEmpty())
                AutomationLane.SONG -> spans.any { (at, bars) -> ok(l.songValues.drop(at).take(bars + 1)) }
                else -> false
            }
        }
    }

    /** [v] has [shape]: "rise" ends a [RISE] over its start, "fall" under it, "wave" climbs that much and comes back down. */
    private fun shaped(v: List<Float>, shape: String): Boolean {
        if (v.size < 2) return false
        return when (shape) {
            "fall" -> v.last() <= v.first() - RISE
            "wave" -> v.size >= 3 && (1 until v.size - 1).any { i -> v[i] >= v.first() + RISE && v[i] >= v.last() + RISE }
            else -> v.last() >= v.first() + RISE
        }
    }

    /** [t] moves [role]'s filter cutoff: its strip's filter, its synth's cutoff, the 303's cutoff knob. */
    private fun cutoffOf(p: Project, t: AutoTarget, role: String): Boolean {
        if (role == TutoTarget.ACID) return t.kind == AutoTarget.ACID && t.param == 0
        val ch = trackFor(p, role)?.let(InstrumentTrack::mixerChannel) ?: return false
        return t.ch == ch && ((t.kind == AutoTarget.INSERT_PARAM && t.param == 0 &&
            p.mixer.channels.getOrNull(ch)?.inserts?.getOrNull(t.slot)?.effect == EffectType.FILTER) ||
            (t.kind == AutoTarget.SYNTH && t.param == SynthPatch.CUTOFF))
    }

    /** Where the part [label] plays in [p]'s song: (first bar, bars) of each time. */
    private fun spansOf(p: Project, label: String): List<Pair<Int, Int>> {
        var bar = 0
        return runs(p).mapNotNull { r ->
            val at = bar
            bar += r.bars
            (at to r.bars).takeIf { labelIs(p.allScenes.getOrNull(r.scene)?.label.orEmpty(), label) }
        }
    }

    /** The bars [spec] ("Intro:32,Montée:8,…") gives each label. */
    private fun barsOf(spec: String): Map<String, Int> =
        spec.split(',').filter { ':' in it }.associate { e -> e.substringBeforeLast(':').trim() to (e.substringAfterLast(':').toIntOrNull() ?: 0) }

    /** Each part of the song as long as [spec] advises for its label, the others in phrases of 8 bars. */
    private fun phrasesDone(p: Project, spec: String): Boolean {
        if (p.song.size < 3) return false
        return runs(p).zip(specFor(p, spec)).all { (r, e) -> e?.let { r.bars == it.second } ?: (r.bars % 8 == 0) }
    }

    /**
     * [label] stands for the part [wanted] of a course's song: its name, or, for its « Fin », the
     * Intro played again when the user has no part of that name (a club track ends on its Intro).
     */
    private fun standsFor(p: Project, label: String, wanted: String): Boolean =
        labelIs(label, wanted) || (wanted in TutoTarget.ENDINGS && labelIs(label, "Intro") && TutoTarget.ENDINGS.none { sceneLabelled(p, it) >= 0 })

    /** For each part of [p]'s song ([runs]), the entry of [spec] it stands for, in order (null: a part the course does not name). */
    private fun specFor(p: Project, spec: String): List<Pair<String, Int>?> {
        val wanted = barsOfList(spec)
        var k = 0
        return runs(p).map { r ->
            val label = p.allScenes.getOrNull(r.scene)?.label.orEmpty()
            if (k < wanted.size && standsFor(p, label, wanted[k].first)) wanted[k++]
            else wanted.firstOrNull { labelIs(label, it.first) } ?: wanted.firstOrNull { standsFor(p, label, it.first) }
        }
    }

    /**
     * Every part [spec] names, in its order (testers, lot « tuto 4 »: ticked at 5 parts of 7, without
     * the Drop 2 nor the Fin); a part it does not name (the Groove) may sit in between.
     */
    private fun songOrdered(p: Project, spec: String): Boolean {
        val wanted = barsOfList(spec).map { it.first }
        if (wanted.size < 3) return false
        val have = specFor(p, spec).filterNotNull().map { it.first }
        return have == wanted
    }

    /** The parts [step]'s order asks for, and the user's song's parts it names, in order (the others left out): (wanted, have). */
    fun orderOf(step: TutoStep, p: Project): Pair<List<String>, List<String>> {
        val wanted = barsOfList(step.song).map { it.first }
        return wanted to specFor(p, step.song).filterNotNull().map { it.first }
    }

    /** The parts [spec] ("Intro:32,Montée:8,…") lays out, in order, with their bars. */
    private fun barsOfList(spec: String): List<Pair<String, Int>> =
        spec.split(',').filter { ':' in it }.map { e -> e.substringBeforeLast(':').trim() to (e.substringAfterLast(':').toIntOrNull() ?: 0) }

    // ---- « Montre-moi » ----

    /** [p] with [step]'s target in it (one undo step in the app), null when the step has none. */
    fun show(step: TutoStep, p: Project, target: TutoTarget): Project? {
        val c = step.check
        val arg = argOf(c)
        // « Montre-moi » of the kick's step: the other kick (a long one when the drum & bass kick is on, else that one).
        if (step.demo == "kick") {
            val other = if (StudioDrums.isDnbKick(p.padVoices[0])) p.copy(padVoices = p.padVoices - 0) else StudioDrums.withDnbKit(p)
            return other.sanitized().takeIf { it != p.sanitized() }
        }
        val shown: Project = when {
            c == "tempo" -> p.copy(tempoBpm = target.style.bpm)
            c == "key" -> p.copy(harmony = target.harmony)
            c == "swing" -> p.copy(swing = target.style.swing)
            c.startsWith("offset:") -> {
                val early = arg.substringAfter(':', "late") == "early"
                val lanes = lanesOf(arg.substringBefore(':'), target)
                p.copy(drums = p.drums.copy(lanes = p.drums.lanes.mapIndexed { i, l ->
                    if (i !in lanes) l else l.copy(hits = l.hits.mapValues { (_, h) -> h.copy(offset = if (early) -SHOWN_OFFSET_MS else SHOWN_OFFSET_MS) })
                }))
            }
            c.startsWith("lane:") -> {
                val line = target.line(arg, partOf(step)) ?: return null
                val wide = withSteps(p, line.pattern.length)
                val lane = laneOf(arg)
                wide.copy(drums = wide.drums.copy(lanes = wide.drums.lanes.mapIndexed { i, l -> if (i == lane) l.copy(hits = StarterWizard.hits(line, wide.drums.steps)) else l }))
            }
            c == "velocity:hat" -> {
                val line = target.line("hat") ?: return null
                val v = StarterWizard.hits(line.pattern)
                val lane = laneOf("hat")
                val hits = p.drums.lanes[lane].hits.ifEmpty { StarterWizard.hits(line, p.drums.steps) }
                p.copy(drums = p.drums.copy(lanes = p.drums.lanes.mapIndexed { i, l ->
                    if (i != lane) l else l.copy(hits = hits.mapValues { (s, h) -> h.copy(velocity = v[s % line.pattern.length]?.velocity ?: 0.35f) })
                }))
            }
            c.startsWith("triplet:") -> withTriplet(p, arg, target) ?: return null
            c == "notes:acid" -> {
                val a = target.acid ?: return null
                val wanted = acidWanted(step, p, target) ?: return null
                val mine = onRoot(p.acid, a.root)
                p.copy(acid = mine.copy(length = maxOf(mine.length, wantedLength(wanted, a)), steps = mine.steps.mapIndexed { i, st -> wanted[i] ?: st }))
            }
            c.startsWith("notes:") -> {
                val notes = target.roleNotes(arg) ?: return null
                val (q, id) = ensureTrack(p, arg, target) ?: return null
                q.withTrack(q.track(id).copy(notes = notes))
            }
            c == "track:sub" -> withSub(p, target) ?: return null
            c.startsWith("insert:") -> {
                val type = EffectType.entries.firstOrNull { it.name == arg } ?: return null
                channelsToDo(step, p).fold(p) { q, ch -> withInsert(q, ch, Insert(type.ordinal, type.defaults)) ?: q }
            }
            c.startsWith("loudness:") -> LoudnessTarget.entries.firstOrNull { it.name == arg }?.let { p.copy(mixer = p.mixer.copy(target = it)) } ?: return null
            c.startsWith("changed:drums:") -> inScene(p, c.removePrefix("changed:drums:")) { q ->
                // A variation: an open hat on an empty cell near the end (the second to last first), else a percussion's.
                val n = q.drums.steps
                val cells = (n - 2 downTo n / 2) + (n - 1 downTo n / 2)
                val free = (listOf(OPEN_HAT_LANE) + variationLanes().filter { it != OPEN_HAT_LANE }.sorted()).firstNotNullOfOrNull { l ->
                    cells.firstOrNull { it !in q.drums.lanes[l].hits }?.let { l to it }
                }
                if (free == null) q
                else q.copy(drums = q.drums.copy(lanes = q.drums.lanes.mapIndexed { i, l -> if (i == free.first) l.copy(hits = l.hits + (free.second to Hit(0.8f))) else l }))
            } ?: return null
            c.startsWith("send:") -> {
                val bus = arg.toIntOrNull() ?: return null
                val chans = sendChannels(p, bus, step)
                if (chans.isEmpty()) return null
                withChannels(p, chans) { it.copy(sends = it.sends.mapIndexed { b, v -> if (b == bus) maxOf(v, if (bus == 0) 0.3f else 0.25f) else v }) }
            }
            c == "automation" || c.startsWith("automation:") -> curve(p, step, target) ?: return null
            c == "master" -> p.copy(mixer = p.mixer.copy(master = target.song.mixer.master))
            c == "changed:volumes" -> withChannels(p, listOfNotNull(trackFor(p, "pad"), trackFor(p, "lead")).map(InstrumentTrack::mixerChannel)) { it.copy(volume = it.volume * 0.8f) }
            c.startsWith("changed:volumes:") -> volumeChannels(p, c).takeIf { it.isNotEmpty() }?.let { chans -> withChannels(p, chans) { it.copy(volume = it.volume * 0.8f) } } ?: return null
            // The knobs the step names, turned up a third.
            c.startsWith("changed:synth:") && knobsOf(c).second.isNotEmpty() -> {
                val (role, knobs) = knobsOf(c)
                val id = trackFor(p, role) ?: return null
                val t = p.track(id)
                p.withTrack(t.copy(synth = knobs.fold(t.synth) { s, k -> s.with(k, (s.get(k) + 0.3f).coerceAtMost(1f)) }))
            }
            c.startsWith("label:") -> p.renameScene(grooveOf(p), arg)
            c.startsWith("part:") -> makePart(p, step, target)?.let { q ->
                // The Drop 2's own bass (the user's to write), then its Sub, shown in the part.
                if (!writesBass(step)) q
                else trackFor(q, "bass")?.let { b -> q.withTrack(q.track(b).copy(notes = q.track(b).notes.copy(notes = movedBass(step, target)))) }?.let { withSub(it, target) ?: it } ?: q
            } ?: return null
            // The order alone: each part as « Ajouter » brings it, 8 bars (testers: it showed the next step's lengths too).
            c == "song:order" -> song(p, step.song, sized = false) ?: return null
            c.startsWith("song:") -> song(p, step.song) ?: return null
            // The last roll: ×4 on the snare's last hit before the silence.
            c == "fill" -> inScene(p, partsOf(step).first()) { q ->
                val lane = laneOf(fillRole(step))
                val end = q.drums.steps - fillEnd(step, target)
                val at = end - 1
                val beat = (q.stepsPerBar / 4).coerceAtLeast(1)
                q.copy(drums = q.drums.copy(lanes = q.drums.lanes.mapIndexed { i, l ->
                    when {
                        i != lane -> l
                        buildsIn(step, target) -> l.copy(hits = l.hits + (at to (l.hits[at] ?: Hit(1f)).copy(repeats = 4)))
                        // A tom fill: every 16th of the last beat, louder and louder.
                        else -> l.copy(hits = l.hits + (end - beat until end).associateWith { Hit(0.6f + 0.4f * (it - end + beat) / beat) })
                    }
                }))
            } ?: return null
            c == "crash:once" -> partsOf(step).fold(p) { q, l -> crashOnce(q, l) ?: q }
            c == "crash" -> inScene(p, partsOf(step).first { sceneLabelled(p, it) >= 0 || it == partsOf(step).last() }) { q ->
                q.copy(drums = q.drums.copy(lanes = q.drums.lanes.mapIndexed { i, l -> if (i == CRASH_LANE) l.copy(hits = l.hits + (0 to Hit(1f))) else l }))
            } ?: return null
            c == "gap" -> inScene(p, partsOf(step).first()) { q ->
                val n = q.drums.steps
                val cut = gapCells(step, target)
                val silent = gapLanes(step, target)
                q.copy(drums = q.drums.copy(lanes = q.drums.lanes.mapIndexed { k, l -> if (k !in silent) l else l.copy(hits = l.hits.filterKeys { it < n - cut }) }))
            } ?: return null
            else -> return null
        }
        return shown.sanitized().takeIf { it != p.sanitized() }
    }

    /**
     * The Sub an octave under the bass of the part shown (« Copier la basse une octave plus bas »):
     * the Sub track made by the step's button, else a new one; null without a bass to copy.
     */
    private fun withSub(p: Project, target: TutoTarget): Project? {
        val bass = trackFor(p, "bass") ?: return null
        val notes = p.track(bass).notes.takeIf { it.notes.isNotEmpty() } ?: return null
        val (q, id) = p.instrumentTracks.firstOrNull { it.id != bass && roleOf(p, it.id) == "sub" }?.let { p to it.id } ?: partTrack(p, "sub", target) ?: return null
        return q.withTrack(q.track(id).copy(name = "Sub", notes = q.track(id).notes.withSteps(notes.steps).copy(notes = subOf(notes.notes))))
    }

    /**
     * « Copier sur les mesures suivantes » of [role]: the bars of the line that repeat a bar already
     * written, from the first not written to the next bar to write by hand, each the user's own bar
     * copied (the notes there kept); or the hats' first bar's forces on every bar.
     */
    private fun copied(p: Project, role: String, step: TutoStep, target: TutoTarget): Project? {
        if (role == "hat") {
            val per = p.stepsPerBar.coerceAtLeast(1)
            val lanes = lanesOf("hat", target)
            return p.copy(drums = p.drums.copy(lanes = p.drums.lanes.mapIndexed { i, l ->
                if (i !in lanes) l else l.copy(hits = l.hits.mapValues { (s, h) -> l.hits[s % per]?.let { h.copy(velocity = it.velocity) } ?: h })
            })).takeIf { it != p }
        }
        val acid = role == TutoTarget.ACID
        val id = if (acid) null else trackFor(p, role) ?: return null
        val written = if (acid) acidNotes(p.acid) else p.track(id!!).notes.notes
        val ticks = if (acid) AcidPattern.MAX_STEPS * NoteClip.TICKS_PER_STEP else p.track(id!!).notes.ticks
        val bars = lineBars(lineOf(step, target) ?: return null, followKey(step, target))
        val have = written.map { it.pitch to it.start }.toSet()
        var b = firstUndone(bars, have).takeIf { it >= 0 && bars[it].from != null } ?: return null
        val copies = ArrayList<Note>()
        while (b < bars.size && (bars[b].notes.isEmpty() || bars[b].from != null)) {
            val bar = bars[b]
            bar.from?.let { from ->
                copies += written.filter { it.start / BAR_TICKS == from }.map {
                    it.copy(start = it.start + (b - from) * BAR_TICKS, pitch = TutoTarget.byDegrees(it.pitch, bar.shift, target.harmony).coerceIn(0, 127), played = null)
                }
            }
            b++
        }
        val fresh = copies.filter { (it.pitch to it.start) !in have && it.start < ticks }
        if (fresh.isEmpty()) return null
        if (acid) {
            // The 303: its bars copied, its line as long as the course's.
            val want = target.acid ?: return null
            val mine = onRoot(p.acid, want.root)
            val steps = mine.steps.toMutableList()
            for (n in fresh) steps[n.start / NoteClip.TICKS_PER_STEP] = acidOf(n, mine.root)
            return p.copy(acid = mine.copy(steps = steps, length = maxOf(mine.length, want.length)).sanitized())
        }
        val clip = p.track(id!!).notes
        return p.withTrack(p.track(id).copy(notes = clip.copy(notes = clip.notes + fresh).sanitized()))
    }

    /**
     * The 303's gated steps as notes (to write, dot and copy them as a track's): a 16th each, its
     * accent and its slide in its length's last ticks ([acidOf] reads them back).
     */
    fun acidNotes(a: AcidPattern): List<Note> = a.steps.take(a.length).withIndex().filter { it.value.gate }.map { (i, st) ->
        Note(i * NoteClip.TICKS_PER_STEP, NoteClip.TICKS_PER_STEP + (if (st.accent) ACCENT_TICK else 0) + (if (st.slide) SLIDE_TICK else 0), TutoTarget.acidPitch(a.root, st), if (st.accent) 1f else 0.7f)
    }

    /** The 303 step a note of [acidNotes] stands for, from [root]. */
    private fun acidOf(n: Note, root: Int): AcidStep {
        val flags = n.length - NoteClip.TICKS_PER_STEP
        return TutoTarget.acidStep(root, n.pitch, flags and ACCENT_TICK != 0, flags and SLIDE_TICK != 0)
    }

    private const val ACCENT_TICK = 1
    private const val SLIDE_TICK = 2

    /** [a] with its root at [root], each step keeping the pitch it plays (a step out of its reach left as it is). */
    private fun onRoot(a: AcidPattern, root: Int): AcidPattern {
        if (a.root == root) return a
        return a.copy(root = root, steps = a.steps.map { st -> if (!st.gate) st else TutoTarget.acidStep(root, TutoTarget.acidPitch(a.root, st), st.accent, st.slide) }).sanitized()
    }

    /** The 303 steps [step] shows dotted now (by index), null for a step that is not the 303's. */
    fun acidWanted(step: TutoStep, p: Project, target: TutoTarget): Map<Int, AcidStep>? {
        if (step.check != "notes:acid") return null
        val root = target.acid?.root ?: return null
        return wantedNotes(step, p, target)?.associate { it.start / NoteClip.TICKS_PER_STEP to acidOf(it, root) }
    }

    /** The length of the 303 line that holds [wanted]: whole bars, at least 1. */
    private fun wantedLength(wanted: Map<Int, AcidStep>, a: AcidPattern): Int =
        (((wanted.keys.maxOrNull() ?: 0) / 16 + 1) * 16).coerceIn(16, a.length.coerceAtLeast(16))

    /** The ticks of a bar of a line (4 beats of 4 16ths). */
    private const val BAR_TICKS = 16 * NoteClip.TICKS_PER_STEP

    /**
     * A bar of a line: its [notes], and [from] the earlier bar it repeats (written by « Copier »),
     * null when it is written by hand; [shift]: the degrees of the key it is moved by there (a line
     * that follows the chords), 0 for the same notes.
     */
    private class LineBar(val notes: List<Note>, val from: Int?, val shift: Int = 0)

    /**
     * The bars of [notes]: each written by hand, or the copy of an earlier bar (the same notes, the
     * same places in it; or, in [key], the same moved along its scale by a few degrees: the bass that
     * follows the chords).
     */
    private fun lineBars(notes: List<Note>, key: Harmony? = null): List<LineBar> {
        val count = (notes.maxOfOrNull { it.start } ?: -1) / BAR_TICKS + 1
        val inBar = (0 until count).map { b -> notes.filter { it.start / BAR_TICKS == b } }
        val shape = inBar.mapIndexed { b, ns -> ns.map { Triple(it.pitch, it.start - b * BAR_TICKS, it.length) }.toSet() }
        fun moved(a: Int, d: Int) = shape[a].map { Triple(TutoTarget.byDegrees(it.first, d, key!!), it.second, it.third) }.toSet()
        return inBar.mapIndexed { b, ns ->
            if (ns.isEmpty()) return@mapIndexed LineBar(ns, null)
            (0 until b).firstOrNull { shape[it] == shape[b] }?.let { return@mapIndexed LineBar(ns, it) }
            if (key != null && key.scale != com.pocketstudio.model.Scale.Off) for (a in 0 until b) {
                if (shape[a].isEmpty()) continue
                for (d in (-6..6).filter { it != 0 }) if (moved(a, d) == shape[b]) return@mapIndexed LineBar(ns, a, d)
            }
            LineBar(ns, null)
        }
    }

    /** The key a step's line follows the chords in ([TutoCourse.follow]: « Copier » moves its bars), null when it repeats them as they are. */
    private fun followKey(step: TutoStep, target: TutoTarget): Harmony? {
        val role = if (writesBass(step)) "bass" else step.check.removePrefix("notes:")
        return target.harmony.takeIf { TutoCourse.of(target.style.id)?.follow?.contains(role) == true }
    }

    /** The first bar with a note still to write (-1: all written). */
    private fun firstUndone(bars: List<LineBar>, have: Set<Pair<Int, Int>>): Int = bars.indexOfFirst { b -> b.notes.any { (it.pitch to it.start) !in have } }

    /** The whole line [step] asks for (its track's, the Drop 2's moved bass), null for a step with none. */
    private fun lineOf(step: TutoStep, target: TutoTarget): List<Note>? = when {
        step.check == "notes:acid" -> target.acid?.let(::acidNotes)
        step.check.startsWith("notes:") -> target.roleNotes(step.check.removePrefix("notes:"))?.notes
        writesBass(step) -> movedBass(step, target)
        else -> null
    }

    /** [p] with its drum loop at least [steps] long, each pad's hits repeated over the new bars. */
    private fun withSteps(p: Project, steps: Int): Project {
        val d = p.drums
        if (d.steps >= steps) return p
        val n = d.steps.coerceAtLeast(1)
        return p.copy(drums = d.copy(steps = steps, lanes = d.lanes.map { l ->
            l.copy(hits = (0 until steps).mapNotNull { s -> l.hits[s % n]?.let { s to it } }.toMap())
        }))
    }

    /** [p] with track [part] (an existing one, else a new one: [partTrack]), and its id. */
    private fun ensureTrack(p: Project, role: String, target: TutoTarget): Pair<Project, Int>? =
        trackFor(p, role)?.let { p to it } ?: partTrack(p, role, target)

    /**
     * The first track still as a new project has it (its default name, no note anywhere): the
     * tutorial hides it until a step makes it the song's first instrument track.
     */
    fun blankFirst(p: Project): Boolean = p.track(0).name == Project().synthTrackName && notesOf(p, 0).isEmpty()

    /**
     * A new, empty track for [role] ("bass", "pad", "lead", "sub") with the style's sound and level
     * as the assistant gives them, its notes as long as the target's (the blank first track becomes
     * it when it is still blank); null when no track is free.
     */
    fun partTrack(p: Project, role: String, target: TutoTarget): Pair<Project, Int>? {
        val part = target.partOf(role) ?: return null
        val kind = if (role == "sub" || role in TutoTarget.EXTRA_ROLES) target.kind(part) else target.trackKind(part)
        val added = NewTracks.add(p, kind, target.style) ?: return null
        val new = added.tracks.first { n -> p.tracks.none { it.id == n.id } }
        val steps = if (role == "sub") trackFor(p, "bass")?.let { p.track(it).notes.steps } ?: 16 else (target.roleNotes(role) ?: target.notes(part)).steps
        // The 808 track: the style's 808 (its length, its dirt, its click, its slide), as the assistant's pad plays it.
        val voice = if (kind != TrackKind.Bass808) new.voice808 else {
            val theirs = StudioDrums.voice808(target.style, target.harmony)
            com.pocketstudio.model.PadVoice(com.pocketstudio.model.PadVoice.BASS_808, theirs.params.filterKeys { it in com.pocketstudio.model.PadVoice.TRACK_808_PARAMS })
                .let { v -> if ((v.param(com.pocketstudio.model.PadVoice.GLIDE) ?: 0f) > 0f) v else v.with(com.pocketstudio.model.PadVoice.GLIDE, NewTracks.GLIDE_808) }.sanitized()
        }
        // The recipe's way of playing the part (synthwave's and trance's chords in an arpeggio): the track's from its start.
        val performance = if (role == "sub" || role in TutoTarget.EXTRA_ROLES) new.notes.performance else target.notes(part).performance
        val named = new.copy(
            name = if (role == "sub") "Sub" else TutoTarget.EXTRA_NAMES[role] ?: new.name,
            notes = new.notes.withSteps(steps).copy(notes = emptyList(), performance = performance), voice808 = voice,
        )
        // The second pad and the counter-melody: the style's own sounds for them (liquid's pad layer, its counter), else a pad and the lead's.
        val made = extraSound(role, target)?.let { (synth, plaits) ->
            if (plaits != null) named.copy(engine = 1, plaits = plaits) else named.copy(engine = 0, synth = synth ?: named.synth)
        } ?: named
        // The bass's own effect, as the assistant's song has it (jump up: « Growl », its distortion and its
        // three-band squash; measured: without it the growl synth alone is dull, 0.9 % of its energy over 2 kHz, 6.9 % with it).
        val fx = target.style.effects.firstOrNull { it.target == role && it.preset.isNotEmpty() }?.let { FxPresets.byId(it.preset) }
        val withFx = if (fx == null) added else withChannels(added, listOf(InstrumentTrack.mixerChannel(made.id))) { it.withFx(fx).sanitized() }
        if (!blankFirst(p)) return withFx.withTrack(made) to made.id
        // The blank first track takes its place (same sound, same level), the slot of the new one stays free.
        val level = withFx.mixer.channels[InstrumentTrack.mixerChannel(made.id)]
        val first = p.withTrack(made.copy(id = 0, color = p.track(0).color))
        return withChannels(first, listOf(InstrumentTrack.mixerChannel(0))) { level } to 0
    }

    /** The library sound of an extra role's track (synth, or Plaits), null: the kind's own. */
    private fun extraSound(role: String, target: TutoTarget): Pair<SynthPatch?, com.pocketstudio.model.PlaitsPatch?>? {
        val name = when (role) {
            TutoTarget.PAD2 -> target.style.melody.padLayer ?: PAD2_SOUND
            TutoTarget.COUNTER -> target.style.melody.counter ?: return null
            else -> return null
        }
        com.pocketstudio.model.SoundLibrary.SYNTH.firstOrNull { it.name == name }?.let { return it.sound to null }
        com.pocketstudio.model.SoundLibrary.PLAITS.firstOrNull { it.name == name }?.let { return null to it.sound }
        return null
    }

    /** The second pad's sound when the style names none. */
    private const val PAD2_SOUND = "Nappe"

    private fun withChannels(p: Project, chans: List<Int>, f: (Channel) -> Channel): Project =
        p.copy(mixer = p.mixer.copy(channels = p.mixer.channels.mapIndexed { i, ch -> if (i in chans) f(ch) else ch }))

    /** [insert] in the first free slot of channel [ch] (null when it already has one of its kind or no slot is free). */
    private fun withInsert(p: Project, ch: Int, insert: Insert): Project? {
        val inserts = p.mixer.channels.getOrNull(ch)?.inserts ?: return null
        if (inserts.any { it.type == insert.type }) return null
        val slot = inserts.indexOfFirst { it.effect == EffectType.NONE }.takeIf { it >= 0 } ?: return null
        return withChannels(p, listOf(ch)) { it.copy(inserts = inserts.toMutableList().also { l -> l[slot] = insert }) }
    }

    /**
     * The curve [step] asks for ([curveDrawn]): a filter on its role's track (or the 303's cutoff)
     * drawn over each time its part plays in the song (its first 8 bars without a song): rising,
     * falling, or up and down.
     */
    private fun curve(p: Project, step: TutoStep, target: TutoTarget): Project? {
        val role = step.target.ifEmpty { "pad" }
        val shape = step.check.substringAfter(':', "").ifEmpty { RISE_SHAPE }
        val label = partsOf(step).first()
        val (q, t) = if (role == TutoTarget.ACID) p to AutoTarget(AutoTarget.ACID, AutoTarget.ACID_CHANNEL, 0, 0)
        else {
            val id = trackFor(p, role) ?: (if (step.target.isEmpty()) trackFor(p, "lead") else null) ?: return null
            val ch = InstrumentTrack.mixerChannel(id)
            val withFilter = p.mixer.channels.getOrNull(ch)?.inserts?.indexOfFirst { it.effect == EffectType.FILTER }?.takeIf { it >= 0 }?.let { p to it }
                ?: withInsert(p, ch, Insert(EffectType.FILTER.ordinal, listOf(1f, 0.25f, 0f, 1f)))?.let { q -> q to q.mixer.channels[ch].inserts.indexOfFirst { it.effect == EffectType.FILTER } }
                ?: return null
            withFilter.first to AutoTarget(AutoTarget.INSERT_PARAM, ch, withFilter.second, 0)
        }
        var lane = AutomationLane(t, mode = AutomationLane.SONG)
        val spans = spansOf(q, label).ifEmpty { listOf(0 to 8) }
        for ((at, bars) in spans) lane = when (shape) {
            "fall" -> lane.withSongPoint(at, 1f).withSongPoint(at + bars - 1, 0.15f)
            "wave" -> lane.withSongPoint(at, 0.15f).withSongPoint(at + bars / 2, 1f).withSongPoint(at + bars - 1, 0.15f)
            else -> lane.withSongPoint(at, 0.15f).withSongPoint(at + bars - 1, 1f)
        }
        val kept = q.automation.filterNot { it.target == lane.target }
        return q.copy(automation = (kept + lane).take(AutomationLane.MAX_LANES))
    }

    /** [edit] made in the scene named [label] (shown, then back on the scene shown before). */
    private fun inScene(p: Project, label: String, edit: (Project) -> Project): Project? {
        val i = sceneLabelled(p, label).takeIf { it >= 0 } ?: return null
        return edit(p.switchScene(i)).switchScene(p.scene)
    }

    /**
     * A part [step] makes: the Groove copied into the scene named after it (a new one, or its own
     * when it exists), only the bricks of [TutoStep.keep] kept ("*": all), the drums of the
     * target's part [TutoStep.lanesFrom] on the pads it keeps.
     */
    private fun makePart(p: Project, step: TutoStep, target: TutoTarget): Project? {
        val label = step.check.removePrefix("part:").substringBeforeLast(':')
        val groove = grooveOf(p)
        if (p.allScenes.getOrNull(groove)?.isEmpty != false) return null
        val dest = p.allScenes.indexOfFirst { labelIs(it.label, label) }.takeIf { it >= 0 && it != groove } ?: p.switchScene(groove).firstEmptyScene() ?: return null
        var q = p.switchScene(groove).copySceneTo(dest).renameScene(dest, label).switchScene(dest)
        val keep = step.keep.split(',').map { it.trim() }.toSet()
        val all = "*" in keep
        // The course's own drums of that part, else the assistant's (the Montée's hats still on its last beat: the gap step erases them).
        val fromLabel = step.lanesFrom.ifEmpty { label.takeIf { target.partLines(it) != null }.orEmpty() }
        val from = fromLabel.takeIf { it.isNotEmpty() }?.let { l ->
            target.partDrums(l) ?: target.sceneOf(l).takeIf { it >= 0 }?.let { target.assistantSong.allScenes[it].drums }
        }
        if (from != null) q = withSteps(q, from.steps)
        val steps = q.drums.steps
        q = q.copy(drums = q.drums.copy(lanes = q.drums.lanes.mapIndexed { i, l ->
            val kept = all || "drums" in keep || roleOfLane(i, target) in keep
            val theirs = from?.lanes?.getOrNull(i)?.hits?.filterKeys { it < steps }.orEmpty()
            when {
                !kept -> l.copy(hits = emptyMap())
                // Testers (lot « tuto 5 »: a « Rim » in the Facile Drop 2, never put there): the example's hits only on a pad the user plays.
                theirs.isNotEmpty() && l.hits.isNotEmpty() -> l.copy(hits = theirs)
                else -> l
            }
        }))
        if (step.build.isNotEmpty()) q = built(q, step.build, buildGap(target, label))
        // A brick not kept is silent in the part; the Drop 2's bass ([TutoStep.transpose]) is left for the user to write.
        q.instrumentTracks.forEach { t ->
            val role = roleOf(q, t.id) ?: "lead"
            val kept = all || role in keep || (role == "sub" && "bass" in keep)
            q = q.withTrack(t.copy(notes = if (kept) t.notes else t.notes.copy(notes = emptyList())))
        }
        if (!all && "acid" !in keep) q = q.copy(acid = q.acid.copy(steps = q.acid.steps.map { it.copy(gate = false) }))
        return q
    }

    /** The bars a part brings when « Ajouter » puts it in the song. */
    private const val ADDED_BARS = 8

    /**
     * The part shown with its pad [role] accelerating bar after bar (Théo, lot « tuto 5 »: one roll
     * in the last bar is a fill, not a build): quarters, eighths, 16ths, then 16ths rolled in two
     * (32nds), louder and louder, up to the silence of its last beat, where its notes stop too.
     */
    fun built(p: Project, role: String, gap: Int = GAP_CELLS): Project {
        val lane = laneOf(role)
        val n = p.drums.steps
        val per = p.stepsPerBar.coerceAtLeast(4)
        val beat = per / 4
        val end = n - gap
        // Each quarter of the loop (a bar of a 4-bar one) faster than the one before.
        val hits = (0 until end).mapNotNull { s ->
            val quarter = s * 4 / n
            val every = when (quarter) { 0 -> beat; 1 -> beat / 2; else -> 1 }.coerceAtLeast(1)
            if (s % every != 0) null
            else s to Hit((0.5f + 0.5f * s / end.coerceAtLeast(1)).coerceAtMost(1f), repeats = if (quarter >= 3) 2 else 1)
        }.toMap()
        var q = p.copy(drums = p.drums.copy(lanes = p.drums.lanes.mapIndexed { i, l -> if (i == lane) l.copy(hits = hits) else l }))
        // Its notes as long as its drums, and silent on that last beat.
        val stop = end * NoteClip.TICKS_PER_STEP
        q.instrumentTracks.forEach { t ->
            val clip = t.notes.withSteps(n)
            val notes = clip.notes.filter { it.start < stop }.map { if (it.end > stop) it.copy(length = stop - it.start) else it }
            q = q.withTrack(t.copy(notes = clip.copy(notes = notes)))
        }
        return q
    }

    /**
     * The assistant's [song] with what the course has the user make instead ([TutoTarget.song]):
     * its own bass in every part that plays one (the Drop 2's moved), the Sub under it, and the
     * Montée built as its step builds it, its last roll and its silent last beat.
     */
    internal fun courseSong(song: Project, target: TutoTarget): Project {
        var p = song
        val shown = p.scene
        val lines = TutoCourse.of(target.style.id)?.lines.orEmpty()
        val ownBass = Part.Bass.id in lines
        val moved = target.movedStep
        val build = target.buildStep
        // The course's chords and lead on the assistant's tracks that play them.
        val own = listOf(Part.Pad, Part.Lead).filter { it.id in lines }.mapNotNull { part -> target.songTrack(part)?.let { it to target.notes(part).notes } }.toMap() +
            // The second pad, the counter-melody: on the assistant's tracks of that name when it has them.
            TutoTarget.EXTRA_ROLES.filter { it in lines }.mapNotNull { role ->
                song.instrumentTracks.firstOrNull { namedRole(song, it.id) == role }?.let { t -> target.roleNotes(role)?.let { t.id to it.notes } }
            }.toMap()
        p.allScenes.forEachIndexed { i, sc ->
            if (sc.isEmpty) return@forEachIndexed
            var q = p.switchScene(i)
            if (ownBass) {
                val whole = target.notes(Part.Bass)
                val line = if (moved != null && labelIs(sc.label, partLabel(moved))) movedBass(moved, target) else whole.notes
                q.instrumentTracks.forEach { t ->
                    // By its name only: a low riser (« Souffle montant ») is no bass.
                    val role = namedRole(q, t.id)
                    if ((role == "bass" || role == "sub") && t.notes.notes.isNotEmpty()) {
                        val fit = TutoTarget.fitted(t.notes, line.filter { it.start < whole.ticks }, whole.steps)
                        q = q.withTrack(t.copy(notes = if (role == "sub") fit.copy(notes = subOf(fit.notes)) else fit))
                    }
                }
                // The 808 styles: the 808's pad plays the course's bass where it played the assistant's.
                if (target.bass808) {
                    val l808 = StudioDrums.LANE_808
                    val theirs = q.drums.lanes.getOrNull(l808)?.hits.orEmpty()
                    if (theirs.isNotEmpty()) {
                        val n = q.drums.steps
                        val clip = TutoTarget.fitted(NoteClip(steps = n), line.filter { it.start < whole.ticks }, whole.steps)
                        val hits = StudioDrums.hits808(clip, n, StudioDrums.tune808(target.harmony.root), StudioDrums.glideMs(target.style), theirs.values.maxOf { it.velocity }, target.style.bpm.toFloat())
                        q = q.copy(drums = q.drums.copy(lanes = q.drums.lanes.mapIndexed { k, l -> if (k == l808) l.copy(hits = hits) else l }))
                    }
                }
            }
            // The course's own drums of this part (and of its variants: « Drop 2 » plays the Drop's).
            target.partDrums(sc.label, q.drums.steps)?.let { d ->
                q = q.copy(drums = q.drums.copy(lanes = q.drums.lanes.mapIndexed { k, l -> l.copy(hits = d.lanes[k].hits) }))
            }
            own.forEach { (id, notes) ->
                val t = q.track(id)
                if (t.notes.notes.isNotEmpty()) {
                    val whole = notes.maxOfOrNull { it.end } ?: 0
                    val steps = ((whole + NoteClip.TICKS_PER_STEP * 16 - 1) / (NoteClip.TICKS_PER_STEP * 16)) * 16
                    q = q.withTrack(t.copy(notes = TutoTarget.fitted(t.notes, notes, steps.coerceAtLeast(16))))
                }
            }
            // The course's 303 line where the 303 plays.
            if (TutoTarget.ACID in lines) target.acid?.let { a ->
                if (q.acid.steps.take(q.acid.length).any { it.gate }) q = q.copy(acid = q.acid.copy(root = a.root, steps = a.steps, length = a.length))
            }
            if (build != null && labelIs(sc.label, partLabel(build))) {
                val gap = buildGap(target, partLabel(build))
                q = built(q, build.build, gap)
                val at = q.drums.steps - gap - 1
                val snare = laneOf(build.build)
                q = q.copy(drums = q.drums.copy(lanes = q.drums.lanes.mapIndexed { l, lane ->
                    val kept = lane.hits.filterKeys { it < q.drums.steps - gap }
                    if (l == snare) lane.copy(hits = kept + (at to (kept[at] ?: Hit(1f)).copy(repeats = 4))) else lane.copy(hits = kept)
                }))
            }
            p = q.switchScene(shown)
        }
        // A crash in every part's loop (« * »), when no ride holds the kit's one cymbal: on its pad in each scene that plays drums.
        TutoCourse.of(target.style.id)?.drums?.get(TutoCourse.ALL_PARTS)?.entries?.firstOrNull { TutoTarget.drumRole(it.key, target.style) == TutoCourse.CRASH }?.value?.trim()
            ?.takeIf { target.style.lines.none { it.role == StarterWizard.Role.Ride } }?.let { pattern ->
                val line = StarterWizard.Line(StarterWizard.Role.Ride, pattern, "")
                p = p.copy(scenes = p.allScenes.map { sc ->
                    if (sc.drums.lanes.none { it.hits.isNotEmpty() }) sc
                    else sc.copy(drums = sc.drums.copy(lanes = sc.drums.lanes.mapIndexed { k, l -> if (k == CRASH_LANE) l.copy(hits = l.hits + StarterWizard.hits(line, sc.drums.steps)) else l }))
                }).sanitized()
            }
        // The course's crashes: once at their part's start, never looped.
        p.allScenes.map { it.label }.filter { it.isNotBlank() && target.crashIn(it) }.distinct().forEach { label -> crashOnce(p, label)?.let { p = it } }
        return p
    }

    /**
     * [p] with a crash played once at each start of the part [label] in its song, never looped
     * (the assistant's way, [DrumArrangement]: the part's first bar plays a one-bar « Crash » scene,
     * its loop's first bar with the cymbal on its first cell, then the part goes on where it was);
     * null when the part is not in the song, or when nothing changes.
     */
    fun crashOnce(p: Project, label: String): Project? {
        val q = p.sanitized()
        val i = sceneLabelled(q, label).takeIf { it >= 0 } ?: return null
        if (q.song.none { it.scene == i }) return null
        val spb = q.stepsPerBar.coerceAtLeast(1)
        val loop = sceneAt(q, i).drums
        val lanes = loop.lanes.mapIndexed { k, l ->
            val bar = l.hits.filterKeys { it < spb }
            l.copy(hits = if (k == CRASH_LANE) bar + (0 to Hit(StarterWizard.CRASH_VELOCITY)) else bar, length = 0)
        }
        val crash = DrumPattern(steps = spb, lanes = lanes)
        val scenes = q.allScenes.toMutableList()
        val at = scenes.indexOfFirst { it.label == DrumArrangement.CRASH_LABEL && it.drums == crash }.takeIf { it >= 0 } ?: run {
            if (scenes.size >= Scene.MAX) return null
            scenes += Scene(drums = crash, label = DrumArrangement.CRASH_LABEL)
            scenes.size - 1
        }
        val keys = q.partKeys
        val song = ArrayList<SongPart>()
        q.song.forEachIndexed { k, part ->
            val starts = part.scene == i && !(k > 0 && q.song[k - 1].scene == i && part.from.isNotEmpty())
            if (!starts || SongPart.DRUMS in part.play) { song += part; return@forEachIndexed }
            val bars = q.partSteps(part) / spb
            song += part.copy(repeats = 1, bars = 1, play = part.play + (SongPart.DRUMS to at))
            if (bars > 1) song += part.copy(repeats = 1, bars = bars - 1, from = keys.associateWith { (part.fromBars(it) + 1).coerceAtMost(SongPart.MAX_FROM) })
        }
        if (song.size > SongPart.MAX_PARTS) return null
        return q.copy(scenes = scenes, song = song).sanitized().takeIf { it != q }
    }

    /** Each start of the part [label] in [p]'s song plays the crash once (a one-bar scene with the cymbal on its first cell). */
    private fun crashOnceDone(p: Project, label: String): Boolean {
        val i = sceneLabelled(p, label).takeIf { it >= 0 } ?: return false
        val starts = runs(p).filter { it.scene == i }.ifEmpty { return false }
        return starts.all { r ->
            p.song[r.first].play[SongPart.DRUMS]?.let { j -> p.allScenes.getOrNull(j)?.drums?.lanes?.getOrNull(CRASH_LANE)?.hits?.containsKey(0) } == true
        }
    }

    /**
     * A part of the song as the user laid it out: [scene] played from song part [first] for [bars]
     * bars, the parts that go on where the one before stopped (its crash bar, then the rest) one with it.
     */
    class Run(val scene: Int, val first: Int, val last: Int, val bars: Int)

    fun runs(p: Project): List<Run> {
        val per = p.stepsPerBar.coerceAtLeast(1)
        val out = ArrayList<Run>()
        p.song.forEachIndexed { k, part ->
            val bars = p.partSteps(part) / per
            val prev = out.lastOrNull()
            if (prev != null && prev.last == k - 1 && p.song[k - 1].scene == part.scene && part.from.isNotEmpty()) out[out.size - 1] = Run(prev.scene, prev.first, k, prev.bars + bars)
            else out += Run(part.scene, k, k, bars)
        }
        return out
    }

    /** The silence a built part ends on: its gap step's, else a beat. */
    private fun buildGap(target: TutoTarget, label: String): Int = gapStep(target, label)?.let { gapCells(it, target) } ?: GAP_CELLS

    /** The song laid out from [spec] ("Intro:32,Montée:8,…"): each part named so, its bars; null when none is found. */
    private fun song(p: Project, spec: String, sized: Boolean = true): Project? {
        val parts = spec.split(',').mapNotNull { e ->
            val label = e.substringBeforeLast(':').trim()
            val bars = e.substringAfterLast(':').toIntOrNull() ?: return@mapNotNull null
            val i = sceneLabelled(p, label).takeIf { it >= 0 } ?: (if (label in TutoTarget.ENDINGS) sceneLabelled(p, "Intro") else -1).takeIf { it >= 0 } ?: return@mapNotNull null
            SongPart(i, 1, bars = if (sized) bars else ADDED_BARS)
        }
        if (parts.size < 3) return null
        return p.copy(song = parts.take(SongPart.MAX_PARTS))
    }

    // ---- « Écouter l'exemple » ----

    /** What [step]'s example plays, made from [target] (never from the user's project). */
    fun example(step: TutoStep, target: TutoTarget): TutoExample? {
        val l = step.listen
        return when {
            l.isEmpty() -> null
            // An excerpt of the finished song: its Montée, then its Drop (the whole song's first bars are its Intro).
            // A song without a Drop: its Refrain after what comes before it (the course's order, else the Couplet).
            l == "song" -> peak(target)?.let { (before, top) -> part(target, top)?.let { TutoExample(before?.let { b -> part(target, b) }, it) } } ?: TutoExample(null, target.song)
            l.startsWith("drums:") || l.startsWith("parts:") -> TutoExample(null, loop(target, l.substringAfter(':').split(',').toSet()))
            // The snare and the « Accords » track without, then with the reverb (Facile: the Mode simple has no send to touch).
            l == "fx:reverb" -> loop(target, setOf("kick", "snare", "pad")).let { dry ->
                TutoExample(dry, show(TutoStep("x", "", check = "send:0"), dry, target) ?: return null)
            }
            l.startsWith("part:") -> part(target, l.removePrefix("part:"))?.let { TutoExample(null, it) }
            l.startsWith("vs:") -> {
                val (a, b) = l.removePrefix("vs:").split(':').let { (it.getOrNull(0) ?: "") to (it.getOrNull(1) ?: "") }
                val after = part(target, b) ?: return null
                TutoExample(part(target, a), after)
            }
            else -> null
        }
    }

    /**
     * The song's peak for « Écouter l'exemple » of the whole song: its first Drop (else Refrain) and
     * the part the course's order puts before it (the Montée, the Couplet); null when it has neither.
     */
    private fun peak(target: TutoTarget): Pair<String?, String>? {
        val top = listOf("Drop", "Refrain").firstOrNull { target.sceneOf(it) >= 0 } ?: return null
        val order = TutoCourse.of(target.style.id)?.chapters?.flatMap { it.steps }?.firstOrNull { it.song.isNotEmpty() }?.song.orEmpty()
            .split(',').filter { ':' in it }.map { it.substringBeforeLast(':').trim() }
        val before = order.indexOfFirst { labelIs(it, top) }.takeIf { it > 0 }?.let { order[it - 1] }
            ?: listOf("Montée", "Couplet", "Groove").firstOrNull { target.sceneOf(it) >= 0 }
        return before to top
    }

    /**
     * [example] played with [kit]'s drum sounds (the user's project: the style's pack once it is on
     * the phone, else the built-in kit), so that « Écouter l'exemple » sounds as what the user
     * builds: each pad's own sound, the studio voices, the kit, the pads' strips.
     */
    fun withKitOf(example: Project, kit: Project): Project {
        val strips = setOf(0) + (0 until DrumPattern.PAD_COUNT).map { Mixer.padChannel(it) }
        fun lanes(d: DrumPattern) = d.copy(lanes = d.lanes.mapIndexed { i, l -> l.copy(sample = kit.drums.lanes.getOrNull(i)?.sample) })
        return example.copy(
            kitVersion = kit.kitVersion, padVoices = kit.padVoices, drums = lanes(example.drums),
            scenes = example.scenes.map { it.copy(drums = lanes(it.drums)) },
            mixer = example.mixer.copy(channels = example.mixer.channels.mapIndexed { i, c ->
                val mine = kit.mixer.channels.getOrNull(i)
                if (i in strips && mine != null) c.copy(volume = mine.volume, pan = mine.pan, inserts = mine.inserts) else c
            }),
        ).sanitized()
    }

    /** The target's part named [label] alone, in a loop. */
    private fun part(target: TutoTarget, label: String): Project? {
        val i = target.sceneOf(label).takeIf { it >= 0 } ?: return null
        return target.song.copy(song = emptyList(), automation = emptyList()).switchScene(i).sanitized()
    }

    /**
     * A loop of the target's [bricks] alone: drum roles ([DRUM_ROLES]), parts ("bass", "pad",
     * "lead") and the 303 ("acid"), on the tutorial's own kit ([TutoTarget.blank]: the user hears
     * the sounds he has) at the style's tempo, key and swing.
     */
    fun loop(target: TutoTarget, bricks: Set<String>): Project {
        val steps = DRUM_ROLES.filter { it in bricks }.mapNotNull { target.line(it)?.pattern?.length }.maxOrNull() ?: 16
        val blank = target.blank()
        val lanes = blank.drums.lanes.map { it.copy(hits = emptyMap()) }.toMutableList()
        for (role in DRUM_ROLES) if (role in bricks) target.line(role)?.let { lanes[laneOf(role)] = lanes[laneOf(role)].copy(hits = StarterWizard.hits(it, steps)) }
        var p = blank.copy(tempoBpm = target.style.bpm, harmony = target.harmony, swing = target.song.swing, drums = DrumPattern(steps = steps, lanes = lanes))
        for (role in TutoTarget.TRACK_ROLES) if (role in bricks) p = show(TutoStep("x", "", check = "notes:$role"), p, target) ?: p
        if (TutoTarget.ACID in bricks) target.acid?.let { a -> p = p.copy(acid = a) }
        return p.sanitized()
    }

    /**
     * The map of the song: the parts the course has the user lay out (its "song:…" step: Intro 32,
     * Montée 8, Drop 32, Break 16, Montée 8, Drop 2 32, Fin 16), each with the energy of the target's
     * part of that name (testers: the map showed the example's ten parts, « Drop 2 » twice, a Fin of
     * 32 the course never asks for). Without such a step: the target's song, its parts in order.
     */
    fun map(target: TutoTarget): List<TutoMapBlock> {
        val song = target.song
        val all = song.allScenes
        val most = all.maxOfOrNull { bricks(it).size }?.coerceAtLeast(1) ?: 1
        val spec = TutoCourse.of(target.style.id)?.chapters?.flatMap { it.steps }?.firstOrNull { it.song.isNotEmpty() }?.song.orEmpty()
        val laid = spec.split(',').filter { ':' in it }.map { it.substringBeforeLast(':').trim() to (it.substringAfterLast(':').toIntOrNull() ?: 0) }
        if (laid.size >= 3) {
            val energy = laid.map { (label, _) ->
                target.sceneOf(label).takeIf { it >= 0 }?.let { bricks(all[it]).size / most.toFloat() } ?: 0f
            }
            // Yanis (lot « tuto 4 »): the curve fell in the Montée and the Drop 2 sat under the Drop. A part that
            // builds (the Montée, the Pré-refrain) climbs from the part before it towards the next; a second
            // one (« Drop 2 », « Refrain 2 ») has its first's energy, as its text says.
            val level = laid.mapIndexed { k, (label, _) ->
                val stem = TutoTarget.STEM.matchEntire(label.trim())?.groupValues?.get(1)
                val first = stem?.let { st -> laid.indices.filter { labelIs(laid[it].first, st) }.maxOfOrNull { energy[it] } }
                first ?: energy[k]
            }
            return laid.mapIndexed { k, (label, bars) ->
                if (BUILDS.any { labelIs(label, it) }) TutoMapBlock(label, bars, level.getOrNull(k - 1) ?: level[k], (level.getOrNull(k + 1) ?: 1f) * MONTEE_TOP)
                else TutoMapBlock(label, bars, level[k])
            }
        }
        val sections = StarterWizard.songSections(song)
        var drops = 0
        return sections.mapIndexed { k, s ->
            // The later Drops play the second bass (« Drop 2 »'s, through the song's parts): named so on the map.
            val label = all.getOrNull(s.scene)?.label.orEmpty().let { if (k > 0 && k == sections.lastIndex && s.scene == sections.first().scene) "Fin" else it }
                .let { if (it == "Drop" && drops++ > 0) "Drop 2" else it }
            TutoMapBlock(label, s.bars, bricks(all[s.scene]).size / most.toFloat())
        }
    }

    /** Where the Montée's curve ends, under its Drop's energy. */
    private const val MONTEE_TOP = 0.85f

    /** The parts that climb towards the next on the map. */
    private val BUILDS = listOf("Montée", "Pré-refrain")

    // ---- what a step needs first, its demonstration, its ghost notes ----

    /** The bars a drum role's line lasts (the loop must be at least that long to hold it). */
    private fun barsOf(p: Project, line: StarterWizard.Line): Int = ceil(line.pattern.length / p.stepsPerBar.coerceAtLeast(1).toDouble()).toInt().coerceIn(1, 4)

    /** The part a step works in ("Montée" for the fill and the gap, "Drop" for the crash), null when none. */
    private fun sceneFor(step: TutoStep, p: Project): String? = when {
        step.check == "fill" || step.check == "gap" || step.check == "automation" || step.check.startsWith("automation:") -> partsOf(step).first()
        step.check == "crash" -> partsOf(step).firstOrNull { sceneLabelled(p, it) >= 0 } ?: partsOf(step).first()
        step.check.startsWith("changed:drums:") -> step.check.removePrefix("changed:drums:")
        writesBass(step) -> partLabel(step)
        else -> null
    }

    /**
     * The notes [step] shows dotted now, null for a step with none: the bars to write by hand up to
     * the next bars « Copier » writes, then (« Copier » to touch) only those already written, the
     * count of notes left at 0 (testers, lot « tuto 5 »: « Encore 14 notes » while the button was
     * « Copier »); the whole line once it is all written.
     */
    private fun wantedNotes(step: TutoStep, p: Project, target: TutoTarget): List<Note>? {
        val all = lineOf(step, target) ?: return null
        val mine = mineFor(step, p, target) ?: return all
        val bars = lineBars(all, followKey(step, target))
        val have = mine.map { it.pitch to it.start }.toSet()
        val first = firstUndone(bars, have).takeIf { it >= 0 } ?: return all
        val end = if (bars[first].from != null) first
            else (first + 1 until bars.size).firstOrNull { b -> bars[b].from != null && bars[b].notes.any { (it.pitch to it.start) !in have } } ?: bars.size
        return bars.take(end).flatMap { it.notes }
    }

    /** The notes the user has where [step] asks for them (its track; the Drop 2's bass in its part), null when no track. */
    private fun mineFor(step: TutoStep, p: Project, target: TutoTarget): List<Note>? = when {
        // The 303's steps written right: the note, and its accent and slide as the line has them.
        step.check == "notes:acid" -> target.acid?.let { a ->
            val want = acidNotes(a).associateBy { it.start }
            acidNotes(p.acid).filter { n -> want[n.start]?.let { it.length == n.length } != false }
        }
        step.check.startsWith("notes:") -> trackFor(p, step.check.removePrefix("notes:"))?.let { notesOf(p, it) }
        writesBass(step) -> sceneLabelled(p, partLabel(step)).takeIf { it >= 0 }?.let { i -> trackFor(p, "bass")?.let { sceneNotes(p, i, it) } }
        else -> null
    }

    /**
     * What [step] needs before the user can do it, null when nothing (or when it is done): the loop
     * long enough for its line, its track, its part, the part it works in. The panel offers it as
     * its main button ([fulfil] on a touch); nothing is made without that touch.
     */
    fun need(step: TutoStep, p: Project, target: TutoTarget, baseline: Any? = null): TutoNeed? {
        if (done(step, p, target, baseline)) return null
        val c = step.check
        val arg = argOf(c)
        return when {
            c.startsWith("lane:") -> partOf(step)?.let { l -> namedScene(p, l).takeIf { it >= 0 && it != p.scene }?.let { TutoNeed.Scene(l) } } ?: target.line(arg, partOf(step))?.let { line ->
                barsOf(p, line).takeIf { p.drums.steps < line.pattern.length.coerceAtMost(4 * p.stepsPerBar) }?.let { TutoNeed.Bars(it) }
            }
            // Its track first; its first 4 bars written: « Copier sur les mesures suivantes ».
            c == "notes:acid" -> copyNeed(step, p, target, TutoTarget.ACID)
            c.startsWith("notes:") -> if (trackFor(p, arg) == null) TutoNeed.Track(arg) else copyNeed(step, p, target, arg)
            // Velocities: the first bar swinging, the others not yet.
            c == "velocity:hat" -> target.line("hat")?.let { line ->
                barsOf(p, line).takeIf { p.drums.steps < line.pattern.length.coerceAtMost(4 * p.stepsPerBar) }?.let { TutoNeed.Bars(it) }
            } ?: hatBars(p.drums, p.stepsPerBar, target).let { b -> TutoNeed.Copy("hat").takeIf { b.firstOrNull() == true && b.any { !it } } }
            // « Copier la basse une octave plus bas »: the Sub track and its notes in one touch.
            c == "track:sub" -> TutoNeed.Track("sub").takeIf { trackFor(p, "bass") != null }
            // The Drop 2: its part made (without its bass), shown, its bass's first 4 bars written, copied, then its Sub.
            writesBass(step) -> {
                val label = partLabel(step)
                val i = sceneLabelled(p, label)
                when {
                    i < 0 -> TutoNeed.Part(label).takeIf { p.allScenes.getOrNull(grooveOf(p))?.isEmpty == false }
                    i != p.scene -> TutoNeed.Scene(label)
                    else -> copyNeed(step, p, target, "bass") ?: TutoNeed.Track("sub").takeIf {
                        mineFor(step, p, target)?.let { notesMatch(it, movedBass(step, target)) } == true && p.instrumentTracks.any { roleOf(p, it.id) == "sub" }
                    }
                }
            }
            c.startsWith("part:") -> TutoNeed.Part(c.removePrefix("part:").substringBeforeLast(':')).takeIf { p.allScenes.getOrNull(grooveOf(p))?.isEmpty == false }
            c.startsWith("label:") -> TutoNeed.Name(arg).takeIf { p.allScenes.any { !it.isEmpty } }
            // The lengths right, the order not: nothing a button can do (the order is said, TutoController).
            c == "song:phrases" -> TutoNeed.Lengths.takeIf { p.song.size >= 3 && !phrasesDone(p, step.song) }
            else -> sceneFor(step, p)?.let { label ->
                sceneLabelled(p, label).takeIf { it >= 0 && it != p.scene }?.let { TutoNeed.Scene(label) }
            }
        }
    }

    /** « Copier sur les mesures suivantes » when the bars written by hand are all there and the next one repeats one of them. */
    private fun copyNeed(step: TutoStep, p: Project, target: TutoTarget, role: String): TutoNeed? {
        val mine = mineFor(step, p, target) ?: return null
        val bars = lineBars(lineOf(step, target) ?: return null, followKey(step, target))
        val first = firstUndone(bars, mine.map { it.pitch to it.start }.toSet())
        return TutoNeed.Copy(role).takeIf { first >= 0 && bars[first].from != null }
    }

    /** What « Copier » writes next, in bars (first, last; 1 is the first bar), null when it is not the step's button. */
    fun copyBars(step: TutoStep, p: Project, target: TutoTarget): IntRange? {
        val mine = mineFor(step, p, target) ?: return null
        val bars = lineBars(lineOf(step, target) ?: return null, followKey(step, target))
        val first = firstUndone(bars, mine.map { it.pitch to it.start }.toSet()).takeIf { it >= 0 && bars[it].from != null } ?: return null
        var last = first
        while (last + 1 < bars.size && (bars[last + 1].notes.isEmpty() || bars[last + 1].from != null)) last++
        return first + 1..last + 1
    }

    /** How many bars of [step]'s line are written by hand (the others by « Copier »), 0 for a step with no line. */
    fun handBars(step: TutoStep, target: TutoTarget): Int = lineOf(step, target)?.let { l -> lineBars(l, followKey(step, target)).count { it.notes.isNotEmpty() && it.from == null } } ?: 0

    /** [p] with [need] met (one touch of the user), and the instrument track it made (null: none); null when it cannot. */
    fun fulfil(need: TutoNeed, step: TutoStep, p: Project, target: TutoTarget): Pair<Project, Int?>? = when (need) {
        is TutoNeed.Bars -> p.copy(drums = p.drums.withSteps(need.bars * p.stepsPerBar)) to null
        // The Sub: made (when it is not) and written an octave under the bass of the part shown.
        is TutoNeed.Track -> if (need.role == "sub") withSub(p, target)?.let { q -> q to q.instrumentTracks.firstOrNull { roleOf(q, it.id) == "sub" }?.id }
            else partTrack(p, need.role, target)?.let { (q, id) -> q to id }
        is TutoNeed.Copy -> copied(p, need.role, step, target)?.let { it to null }
        is TutoNeed.Part -> makePart(p, step, target)?.let { it to null }
        is TutoNeed.Name -> p.renameScene(grooveOf(p), need.label) to null
        is TutoNeed.Lengths -> lengths(p, step.song)?.let { it to null }
        is TutoNeed.Scene -> namedScene(p, need.label).takeIf { it >= 0 }?.let { p.switchScene(it) to null }
    }

    /** Each part of the song given the bars [spec] names for its label (the order kept); null when nothing changes. */
    private fun lengths(p: Project, spec: String): Project? {
        val song = runs(p).zip(specFor(p, spec)).flatMap { (r, e) ->
            val bars = e?.second ?: 8
            val parts = p.song.subList(r.first, r.last + 1)
            // A part split for its crash keeps its first bar, the rest takes the others.
            if (parts.size == 2 && bars > 1) listOf(parts[0].copy(repeats = 1, bars = 1), parts[1].copy(repeats = 1, bars = bars - 1))
            else listOf(parts[0].copy(repeats = 1, bars = bars))
        }
        return p.copy(song = song).takeIf { it.song != p.song }
    }

    /** The notes [step] asks for on its track, shown in dotted outline: (track, notes); null when none. */
    fun ghostNotes(step: TutoStep, p: Project, target: TutoTarget): Pair<Int, List<Note>>? {
        val c = step.check
        return when {
            // The first 4 bars while they are not all written, then the whole line (testers, lot « tuto 4 »).
            c == "notes:acid" -> null
            c.startsWith("notes:") -> {
                val id = trackFor(p, c.removePrefix("notes:")) ?: return null
                id to (wantedNotes(step, p, target) ?: return null)
            }
            // The Drop 2's new bass, in its part only.
            writesBass(step) -> {
                val i = sceneLabelled(p, partLabel(step)).takeIf { it >= 0 && it == p.scene } ?: return null
                (trackFor(p, "bass") ?: return null) to (wantedNotes(step, p, target) ?: return null)
            }
            c == "track:sub" -> {
                val bass = trackFor(p, "bass") ?: return null
                val sub = p.instrumentTracks.firstOrNull { roleOf(p, it.id) == "sub" }?.id ?: return null
                sub to StarterWizard.subNotes(notesOf(p, bass))
            }
            else -> null
        }
    }

    /**
     * « Montre-moi »: the step's target put in step by step ([TutoDemo.frames], from what the user
     * has, a cell or a note more each time, each with what it sounds: [TutoDemo.sounds]), ending on
     * [show]'s result. Null when the step has none.
     */
    fun demo(step: TutoStep, p: Project, target: TutoTarget): TutoDemo? {
        val end = show(step, p, target) ?: return null
        val c = step.check
        val arg = argOf(c)
        val frames = ArrayList<Project>()
        val sounds = ArrayList<TutoSound?>()
        var shownRange = false
        if (step.demo == "kick") {
            // A long kick, then the short dry one, each heard and left a moment (it ends on the drum & bass kick).
            val dnb = StudioDrums.isDnbKick(p.padVoices[0])
            val (long, short) = if (dnb) end to p else p to end
            for ((q, lane) in listOf(long to StarterWizard.Role.Kick.lane, short to StarterWizard.Role.Kick.lane)) {
                repeat(KICK_FRAMES) { k -> frames += q; sounds += if (k == 0) TutoSound(lane = lane) else null }
            }
            return TutoDemo(frames, sounds)
        }
        when {
            // Yanis (lot « tuto 4 »): the key was set at once; now its two buttons one after the other, the scale, then the root.
            c == "key" -> {
                frames += p; sounds += null
                val scaled = p.copy(harmony = p.harmony.copy(scale = target.harmony.scale))
                repeat(KEY_FRAMES) { frames += scaled; sounds += null }
                repeat(KEY_FRAMES) { frames += end; sounds += null }
            }
            c.startsWith("lane:") -> {
                val lane = laneOf(arg)
                val wanted = end.drums.lanes[lane].hits
                val start = need(step, p, target)?.let { n -> fulfil(n, step, p, target)?.first } ?: p
                var hits = start.drums.lanes[lane].hits.filterKeys { it in wanted }
                fun put(h: Map<Int, Hit>) = start.copy(drums = start.drums.copy(lanes = start.drums.lanes.mapIndexed { i, l -> if (i == lane) l.copy(hits = h) else l }))
                frames += put(hits); sounds += null
                for ((at, hit) in wanted.entries.sortedBy { it.key }) {
                    if (at in hits) continue
                    hits = hits + (at to hit)
                    frames += put(hits); sounds += TutoSound(lane = lane)
                }
            }
            (c.startsWith("notes:") && c != "notes:acid") || c == "track:sub" -> {
                val role = if (c == "track:sub") "sub" else arg
                val (start, id) = (if (c == "track:sub") partTrack(p, "sub", target) else ensureTrack(p, arg, target)) ?: return null
                val endTrack = (if (c == "track:sub") end.instrumentTracks.firstOrNull { roleOf(end, it.id) == "sub" }?.id else trackFor(end, role))?.let { end.track(it) }
                // Testers (lot « tuto 5 »: the show wrote other bars than the dotted ones): the notes dotted now, no more.
                val wanted = if (c == "track:sub") endTrack?.notes?.notes.orEmpty() else wantedNotes(step, p, target).orEmpty()
                shownRange = true
                var clip = start.track(id).notes.withSteps(endTrack?.notes?.steps ?: start.track(id).notes.steps).copy(notes = emptyList())
                frames += start.withTrack(start.track(id).copy(notes = clip)); sounds += null
                for ((_, chord) in wanted.groupBy { it.start }.entries.sortedBy { it.key }) {
                    clip = clip.copy(notes = clip.notes + chord)
                    frames += start.withTrack(start.track(id).copy(notes = clip)); sounds += TutoSound(track = id, pitches = chord.map { it.pitch })
                }
            }
        }
        if (!shownRange && (frames.isEmpty() || frames.last().sanitized() != end)) { frames += end; sounds += null }
        // A long line (two bars of hats) goes two by two: the show stays short.
        if (frames.size > MAX_FRAMES) {
            val every = ceil(frames.size / MAX_FRAMES.toDouble()).toInt()
            val keep = frames.indices.filter { it % every == 0 || it == frames.lastIndex }
            return TutoDemo(keep.map { frames[it] }, keep.map { sounds[it] })
        }
        return TutoDemo(frames, sounds)
    }

    const val MAX_FRAMES = 24

    /** The kick's comparison: each kick shown this many frames (about 0.8 s at the panel's pace). */
    private const val KICK_FRAMES = 4

    /** The key's two buttons, each held this many frames. */
    private const val KEY_FRAMES = 4

    /** The cells « Je le fais » shows dotted on a pad: (pad, steps of [steps]) of a "lane:…" step. */
    fun hint(step: TutoStep, target: TutoTarget, steps: Int): Pair<Int, Set<Int>>? {
        if (!step.check.startsWith("lane:")) return null
        val role = argOf(step.check)
        val line = target.line(role, partOf(step)) ?: return null
        return laneOf(role) to StarterWizard.hits(line, steps).keys
    }

    /**
     * The hits a step asks to take away, by pad, in the part shown ([p]'s current scene): the gap's
     * last two cells of the Montée (testers: « efface les coups des 2 dernières cases », but they
     * were in its 4th bar, out of sight). Empty when nothing is left to take away; null for a step
     * that takes nothing away. For the screen to mark them (and show their bar).
     */
    fun toErase(step: TutoStep, p: Project, target: TutoTarget? = null): Map<Int, Set<Int>>? {
        if (step.check != "gap") return null
        val d = p.drums
        val cut = target?.let { gapCells(step, it) } ?: step.length.takeIf { it > 0 } ?: GAP_CELLS
        val last = (d.steps - cut).coerceAtLeast(0) until d.steps
        val silent = gapLanes(step, target)
        return d.lanes.mapIndexedNotNull { i, l -> if (i !in silent) null else l.hits.keys.filter { it in last }.toSet().takeIf { it.isNotEmpty() }?.let { i to it } }.toMap()
    }
}
