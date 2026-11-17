package com.pocketstudio.model.tuto

import com.pocketstudio.model.Note
import com.pocketstudio.model.Project
import com.pocketstudio.model.guide.GuideStatus
import com.pocketstudio.model.Hit
import com.pocketstudio.model.StarterWizard
import com.pocketstudio.model.allScenes
import com.pocketstudio.model.tuto.TutoChecks.KEY_FRAMES
import com.pocketstudio.model.tuto.TutoChecks.LATER_SOFT
import com.pocketstudio.model.tuto.TutoChecks.LATER_TRIPLET
import com.pocketstudio.model.tuto.TutoChecks.SHOWN_OFFSET_MS
import com.pocketstudio.model.tuto.TutoChecks.SOFT_HITS_CHECKED
import com.pocketstudio.model.tuto.TutoChecks.asksSoft
import com.pocketstudio.model.tuto.TutoChecks.barsOf
import com.pocketstudio.model.tuto.TutoChecks.hasTriplet
import com.pocketstudio.model.tuto.TutoChecks.hatBars
import com.pocketstudio.model.tuto.TutoChecks.hatsSwing
import com.pocketstudio.model.tuto.TutoChecks.laneExtras
import com.pocketstudio.model.tuto.TutoChecks.laneMatches
import com.pocketstudio.model.tuto.TutoChecks.laneOf
import com.pocketstudio.model.tuto.TutoChecks.lanesOf
import com.pocketstudio.model.tuto.TutoChecks.leftToLater
import com.pocketstudio.model.tuto.TutoChecks.loopBefore
import com.pocketstudio.model.tuto.TutoChecks.namedScene
import com.pocketstudio.model.tuto.TutoChecks.offsetDone
import com.pocketstudio.model.tuto.TutoChecks.partOf
import com.pocketstudio.model.tuto.TutoChecks.scenesOf
import com.pocketstudio.model.tuto.TutoChecks.withSteps
import com.pocketstudio.model.tuto.TutoChecks.withTriplet
import com.pocketstudio.model.tuto.TutoChecks.SHARED_CARD
import com.pocketstudio.model.tuto.TutoChecks.SWING_POINTS
import com.pocketstudio.model.tuto.TutoChecks.sceneLabelled
import com.pocketstudio.model.tuto.TutoChecks.swingShown
import com.pocketstudio.model.NoteClip
import com.pocketstudio.model.tuto.TutoChecks.acidMatches
import com.pocketstudio.model.tuto.TutoChecks.acidNotes
import com.pocketstudio.model.tuto.TutoChecks.acidWanted
import com.pocketstudio.model.tuto.TutoChecks.copyNeed
import com.pocketstudio.model.tuto.TutoChecks.ensureTrack
import com.pocketstudio.model.tuto.TutoChecks.insertsLeft
import com.pocketstudio.model.tuto.TutoChecks.notesMatch
import com.pocketstudio.model.tuto.TutoChecks.notesOf
import com.pocketstudio.model.tuto.TutoChecks.onRoot
import com.pocketstudio.model.tuto.TutoChecks.partTrack
import com.pocketstudio.model.tuto.TutoChecks.roleOf
import com.pocketstudio.model.tuto.TutoChecks.subOf
import com.pocketstudio.model.tuto.TutoChecks.trackFor
import com.pocketstudio.model.tuto.TutoChecks.wantedLength
import com.pocketstudio.model.tuto.TutoChecks.wantedNotes
import com.pocketstudio.model.tuto.TutoChecks.withSub
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
import com.pocketstudio.model.Insert
import com.pocketstudio.model.InstrumentTrack
import com.pocketstudio.model.LoudnessTarget
import com.pocketstudio.model.Mixer
import com.pocketstudio.model.Scene
import com.pocketstudio.model.SongPart
import com.pocketstudio.model.DrumArrangement
import com.pocketstudio.model.partKeys
import com.pocketstudio.model.StarterWizard.Part
import com.pocketstudio.model.StudioDrums
import com.pocketstudio.model.SynthPatch
import com.pocketstudio.model.TrackKind
import com.pocketstudio.model.NewTracks
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
import kotlin.math.ceil
import kotlin.math.roundToInt
import com.pocketstudio.model.tuto.TutoChecks.KNOB_STEP_PCT
import com.pocketstudio.model.tuto.TutoChecks.OPEN_HAT_LANE
import com.pocketstudio.model.tuto.TutoChecks.RISE_SHAPE
import com.pocketstudio.model.tuto.TutoChecks.channelsToDo
import com.pocketstudio.model.tuto.TutoChecks.curve
import com.pocketstudio.model.tuto.TutoChecks.curveDrawn
import com.pocketstudio.model.tuto.TutoChecks.hasInsert
import com.pocketstudio.model.tuto.TutoChecks.inScene
import com.pocketstudio.model.tuto.TutoChecks.insertChannels
import com.pocketstudio.model.tuto.TutoChecks.knobPct
import com.pocketstudio.model.tuto.TutoChecks.knobValue
import com.pocketstudio.model.tuto.TutoChecks.knobsOf
import com.pocketstudio.model.tuto.TutoChecks.partsOf
import com.pocketstudio.model.tuto.TutoChecks.sceneAt
import com.pocketstudio.model.tuto.TutoChecks.sendChannels
import com.pocketstudio.model.tuto.TutoChecks.sends
import com.pocketstudio.model.tuto.TutoChecks.variationLanes
import com.pocketstudio.model.tuto.TutoChecks.volumeChannels
import com.pocketstudio.model.tuto.TutoChecks.withChannels
import com.pocketstudio.model.tuto.TutoChecks.withInsert
import com.pocketstudio.model.tuto.TutoChecks.withKnob
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

/** The frames a kind of step gives « Montre-moi » before its end; [whole]: they end the show themselves (the end is not appended). */
internal class TutoFrames(val frames: List<Project> = emptyList(), val sounds: List<TutoSound?> = emptyList(), val whole: Boolean = false)

/**
 * One kind of tutorial step (the word of [TutoStep.check]: "lane:hat", "notes:bass", "gap"…), defined ONCE: what
 * ticks it, what « Montre-moi » writes, what its button asks for first, what the screen dots or crosses out. A kind
 * is parsed once from its word ([of]) and holds what the word says (its role, its bus…); what the step itself says
 * (its part, its length, its song) is read from the [TutoStep] given to each function. [TutoChecks] stays the
 * façade: its public functions ask the step's kind and add what is common to every kind (the kick's comparison,
 * the parts a step names and the project lacks, the frames thinned, the answers kept).
 *
 * Why not the three functions of the audit's sketch (`requires`, `remaining`, `arm`, the rest derived): the tick
 * and the demo are not two readings of one list of edits here. [done] is tolerant on purpose (a line in any part,
 * a knob « at least 15 » higher, a curve of a shape) while [shown] writes one exact answer, and the tests pin both;
 * deriving one from the other would change what ticks. So the sketch's `remaining` is kept as its two ends ([done],
 * [shown]) plus what the screen draws of it ([ghosts], [dotted], [crossed], [toDo]); `requires` gives the one
 * [TutoNeed] the panel's button offers (not a list: the panel has one button); `arm` is kept as it was sketched.
 */
internal interface TutoKind {
    /** Whether the step is done in [p] ([armed]: what [arm] gave when the step began). */
    fun done(step: TutoStep, p: Project, target: TutoTarget, armed: Any?): Boolean

    /** What the kind watches for a change, read when the step begins; null for a kind that watches nothing. */
    fun arm(p: Project): Any? = null

    /** [p] with the step's target written in it, as « Montre-moi » leaves it; null when the step has none. */
    fun shown(step: TutoStep, p: Project, target: TutoTarget, level: TutoLevel?): Project? = null

    /** The frames of « Montre-moi » before [end] ([shown], cleaned); none by default (the end alone); null: no show. */
    fun frames(step: TutoStep, p: Project, target: TutoTarget, level: TutoLevel?, end: Project): TutoFrames? = NO_FRAMES

    /** Whether what « Montre-moi » leaves is its last frame (its frames lay less than [shown]: the dotted notes only). */
    val endsOnFrames: Boolean get() = false

    /** Whether « Montre-moi » can be played on [p], [shown] apart. */
    fun showable(p: Project, target: TutoTarget): Boolean = true

    /** What the step needs before the user can do it (the panel's button); by default the part it works in, shown. */
    fun requires(step: TutoStep, p: Project, target: TutoTarget): TutoNeed? = workPart(step, p)?.let { label ->
        sceneLabelled(p, label).takeIf { it >= 0 && it != p.scene }?.let { TutoNeed.Scene(label) }
    }

    /** The part the step works in, null when none. */
    fun workPart(step: TutoStep, p: Project): String? = null

    /** The parts the step names (made first when the project lacks one). */
    fun cited(step: TutoStep): List<String> = emptyList()

    /** The parts a step of this kind works in when it names none. */
    val defaultParts: List<String> get() = MONTEE

    /** The notes shown dotted on a track: (track, notes). */
    fun ghosts(step: TutoStep, p: Project, target: TutoTarget): Pair<Int, List<Note>>? = null

    /** The cells shown dotted on a pad: (pad, cells of a loop of [steps]). */
    fun dotted(step: TutoStep, target: TutoTarget, steps: Int): Pair<Int, Set<Int>>? = null

    /** The hits to take away in the part shown, by pad; null for a kind that takes nothing away. */
    fun crossed(step: TutoStep, p: Project, target: TutoTarget?): Map<Int, Set<Int>>? = null

    /** The strips still to do, in the order the step names them. */
    fun toDo(step: TutoStep, p: Project): List<Int> = emptyList()

    /** The whole line of notes the step asks for, null for a kind with none. */
    fun line(step: TutoStep, target: TutoTarget): List<Note>? = null

    /** The notes the user has where the step asks for its [line], null when there is no track. */
    fun mine(step: TutoStep, p: Project, target: TutoTarget): List<Note>? = null

    /** The role whose [line] the step writes. */
    fun lineRole(step: TutoStep): String = step.check.removePrefix("notes:")

    /** A line of the whole loop: held in another part, it asks for nothing in the part shown. */
    val wholeLoop: Boolean get() = false

    /** Nothing the app can do or show for it (read, listened to, shared): never « Poser ce qui manque ». */
    val byHand: Boolean get() = false

    /** Ticked by a change since the step began ([arm]): not one of the steps laid before a chapter. */
    val watches: Boolean get() = false

    /** Whether the step only makes a part: its « Créer la partie » is offered when its touch ticks it. */
    fun onlyMakesPart(step: TutoStep): Boolean = false

    /** The step lays the song out itself: a part made for it is not put in the song for the user. */
    val laysSong: Boolean get() = false

    companion object {
        val NO_FRAMES = TutoFrames()
        val MONTEE = listOf("Montée")
        private val kinds = ConcurrentHashMap<String, Any>()
        private val UNPARSED = Any()

        /** The kind [check] names, parsed once; null while its kind still lives in [TutoChecks]' own branches. */
        fun of(check: String): TutoKind? = kinds.getOrPut(check) { parse(check) ?: UNPARSED } as? TutoKind

        private fun parse(c: String): TutoKind? = when {
            c == "manual" || c == "listen" -> ByHand
            c == "shared" -> Shared
            c == "tempo" -> Tempo
            c == "key" -> Key
            c == "swing" -> Swing
            c.startsWith("lane:") -> Lane(c.substringAfter(':').substringBefore(':'), c.endsWith(":rolls"))
            c == "velocity:hat" -> VelocityHat
            c.startsWith("offset:") -> c.substringAfter(':').let { Offset(it.substringBefore(':'), it.substringAfter(':', "late") == "early") }
            c.startsWith("triplet:") -> Triplet(c.substringAfter(':'))
            c == "notes:acid" -> AcidNotes
            c.startsWith("notes:") -> Notes(c.removePrefix("notes:"))
            c == "track:sub" -> SubTrack
            c.startsWith("insert:") -> PutInsert(c.removePrefix("insert:"))
            c.startsWith("send:") -> SendTo(c.removePrefix("send:").toIntOrNull())
            c == "automation" || c.startsWith("automation:") -> Curve(c.substringAfter(':', "").ifEmpty { RISE_SHAPE })
            c == "master" -> MasterFx
            c.startsWith("loudness:") -> Loudness(c.removePrefix("loudness:"))
            c == "changed:volumes" -> Volumes(null)
            c.startsWith("changed:volumes:") -> Volumes(c)
            c.startsWith("changed:synth:") -> knobsOf(c).let { (role, knobs) -> SynthKnobs(role, knobs) }
            c.startsWith("changed:drums:") -> DrumsChanged(c.removePrefix("changed:drums:"))
            c == "changed:kit" -> KitChanged
            c.startsWith("changed:") -> NothingWatched
            else -> null
        }
    }
}

/** "manual", "listen": read or listened to, ticked by the user's own button. */
internal object ByHand : TutoKind {
    override fun done(step: TutoStep, p: Project, target: TutoTarget, armed: Any?) = false
    override val byHand get() = true
}

/** "shared": the song exported and its share sheet opened (the app marks the Guide's « Prêt à partager ? » then), not « C'est fait ». */
internal object Shared : TutoKind {
    override fun done(step: TutoStep, p: Project, target: TutoTarget, armed: Any?) = p.guide.statusOf(SHARED_CARD) == GuideStatus.DONE
    override val byHand get() = true
}

/** "tempo": the style's tempo, give or take [TutoTarget.TEMPO_SLACK]. */
internal object Tempo : TutoKind {
    override fun done(step: TutoStep, p: Project, target: TutoTarget, armed: Any?) = abs(p.tempoBpm - target.style.bpm) <= TutoTarget.TEMPO_SLACK
    override fun shown(step: TutoStep, p: Project, target: TutoTarget, level: TutoLevel?) = p.copy(tempoBpm = target.style.bpm)
}

/** "key": the style's root and scale. */
internal object Key : TutoKind {
    override fun done(step: TutoStep, p: Project, target: TutoTarget, armed: Any?) =
        p.harmony.root == target.harmony.root && p.harmony.scale == target.harmony.scale
    override fun shown(step: TutoStep, p: Project, target: TutoTarget, level: TutoLevel?) = p.copy(harmony = target.harmony)

    // Yanis (lot « tuto 4 »): the key was set at once; now its two buttons one after the other, the scale, then the root.
    override fun frames(step: TutoStep, p: Project, target: TutoTarget, level: TutoLevel?, end: Project): TutoFrames {
        val scaled = p.copy(harmony = p.harmony.copy(scale = target.harmony.scale))
        val frames = listOf(p) + List(KEY_FRAMES) { scaled } + List(KEY_FRAMES) { end }
        return TutoFrames(frames, frames.map { null })
    }
}

/**
 * "swing": the style's swing (house, garage, boom bap, lo-fi) as the slider shows it (swing / MAX_SWING, in %), give or
 * take [SWING_POINTS] (tour 6, T5: the raw value was compared, « 25 » on the slider was 0.1875, never 0.25).
 */
internal object Swing : TutoKind {
    override fun done(step: TutoStep, p: Project, target: TutoTarget, armed: Any?) =
        target.style.swing > 0.0 && p.swing > 0.0 && abs(swingShown(p.swing) - swingShown(target.style.swing)) <= SWING_POINTS
    override fun shown(step: TutoStep, p: Project, target: TutoTarget, level: TutoLevel?) = p.copy(swing = target.style.swing)
}

/**
 * "lane:<role>[:rolls]": the style's line of a drum role, every dotted cell hit and no other ([laneMatches]); with a
 * [TutoStep.part], the course's drums of that part, in that part. [rolls]: each roll of the line rolled there too.
 */
internal class Lane(val role: String, val rolls: Boolean) : TutoKind {
    override val wholeLoop get() = true

    override fun done(step: TutoStep, p: Project, target: TutoTarget, armed: Any?) = matches(step, p, target, SOFT_HITS_CHECKED && asksSoft(step))

    /** The line laid in a part the step looks at; [soft]: its soft cells played soft too. */
    fun matches(step: TutoStep, p: Project, target: TutoTarget, soft: Boolean): Boolean = target.line(role, partOf(step))?.let { line ->
        scenesOf(p, partOf(step)).any { laneMatches(it.drums, lanesOf(role, target), line, p.stepsPerBar, rolls = rolls, soft = soft) }
    } ?: false

    override fun shown(step: TutoStep, p: Project, target: TutoTarget, level: TutoLevel?): Project? {
        val line = target.line(role, partOf(step)) ?: return null
        val wide = withSteps(p, line.pattern.length)
        val lane = laneOf(role)
        // Relecture tour 1, M4: the check asks for no hit more on any pad of the role (Kick 2,
        // the open hat…): their hits inside the loop go too, or « Montre-moi » never ticks.
        val others = lanesOf(role, target) - lane
        val steps = wide.drums.steps
        val later = leftToLater(step, target, level)
        val hits = StarterWizard.hits(line, steps).mapValues { (_, h) ->
            val loud = if (LATER_SOFT in later) h.copy(velocity = 1f) else h
            if (LATER_TRIPLET in later && loud.isTriplet) loud.copy(repeats = 1, span = 1) else loud
        }
        return wide.copy(drums = wide.drums.copy(lanes = wide.drums.lanes.mapIndexed { i, l ->
            when (i) {
                lane -> l.copy(hits = hits)
                in others -> l.copy(hits = l.hits.filterKeys { it >= steps })
                else -> l
            }
        }))
    }

    /** A cell more each time, from what the user has of the line (its bars made first). */
    override fun frames(step: TutoStep, p: Project, target: TutoTarget, level: TutoLevel?, end: Project): TutoFrames {
        val lane = laneOf(role)
        val wanted = end.drums.lanes[lane].hits
        val start = TutoChecks.ownNeed(step, p, target)?.let { n -> TutoChecks.fulfil(n, step, p, target, level)?.first } ?: p
        var hits = start.drums.lanes[lane].hits.filterKeys { it in wanted }
        fun put(h: Map<Int, Hit>) = start.copy(drums = start.drums.copy(lanes = start.drums.lanes.mapIndexed { i, l -> if (i == lane) l.copy(hits = h) else l }))
        val frames = arrayListOf(put(hits))
        val sounds = arrayListOf<TutoSound?>(null)
        for ((at, hit) in wanted.entries.sortedBy { it.key }) {
            if (at in hits) continue
            hits = hits + (at to hit)
            frames += put(hits); sounds += TutoSound(lane = lane)
        }
        return TutoFrames(frames, sounds)
    }

    // Its part shown first; then its bars: its line's, or the loop the course had already made when its words count cells over it.
    override fun requires(step: TutoStep, p: Project, target: TutoTarget): TutoNeed? =
        partOf(step)?.let { l -> namedScene(p, l).takeIf { it >= 0 && it != p.scene }?.let { TutoNeed.Scene(l) } } ?: target.line(role, partOf(step))?.let { line ->
            maxOf(barsOf(p, line), loopBefore(step, p, target)).takeIf { p.drums.steps < it * p.stepsPerBar }?.let { TutoNeed.Bars(it) }
        }

    override fun dotted(step: TutoStep, target: TutoTarget, steps: Int): Pair<Int, Set<Int>>? {
        val line = target.line(role, partOf(step)) ?: return null
        return laneOf(role) to StarterWizard.hits(line, steps).keys
    }

    override fun crossed(step: TutoStep, p: Project, target: TutoTarget?): Map<Int, Set<Int>>? = target?.let { laneExtras(step, p, it) }
}

/** "velocity:hat": the 4 hats on the beats softer than those between them, in every bar (one hit made « Doux » is not the groove). */
internal object VelocityHat : TutoKind {
    override fun done(step: TutoStep, p: Project, target: TutoTarget, armed: Any?) = p.allScenes.any { hatsSwing(it.drums, p.stepsPerBar, target) }

    override fun shown(step: TutoStep, p: Project, target: TutoTarget, level: TutoLevel?): Project? {
        val line = target.line("hat") ?: return null
        val v = StarterWizard.hits(line.pattern)
        val lane = laneOf("hat")
        val hits = p.drums.lanes[lane].hits.ifEmpty { StarterWizard.hits(line, p.drums.steps) }
        return p.copy(drums = p.drums.copy(lanes = p.drums.lanes.mapIndexed { i, l ->
            if (i != lane) l else l.copy(hits = hits.mapValues { (s, h) -> h.copy(velocity = v[s % line.pattern.length]?.velocity ?: 0.35f) })
        }))
    }

    // The loop as long as the line; then the first bar swinging, the others not yet: « Copier ».
    override fun requires(step: TutoStep, p: Project, target: TutoTarget): TutoNeed? = target.line("hat")?.let { line ->
        barsOf(p, line).takeIf { p.drums.steps < line.pattern.length.coerceAtMost(4 * p.stepsPerBar) }?.let { TutoNeed.Bars(it) }
    } ?: hatBars(p.drums, p.stepsPerBar, target).let { b -> TutoNeed.Copy("hat").takeIf { b.firstOrNull() == true && b.any { !it } } }
}

/** "offset:<role>:<late|early>": at least half the role's hits played late (early), by [TutoChecks.OFFSET_MS] or more (the lo-fi's lazy snare). */
internal class Offset(val role: String, val early: Boolean) : TutoKind {
    override fun done(step: TutoStep, p: Project, target: TutoTarget, armed: Any?) = p.allScenes.any { sc -> offsetDone(sc.drums, lanesOf(role, target), early) }

    override fun shown(step: TutoStep, p: Project, target: TutoTarget, level: TutoLevel?): Project {
        val lanes = lanesOf(role, target)
        return p.copy(drums = p.drums.copy(lanes = p.drums.lanes.mapIndexed { i, l ->
            if (i !in lanes) l else l.copy(hits = l.hits.mapValues { (_, h) -> h.copy(offset = if (early) -SHOWN_OFFSET_MS else SHOWN_OFFSET_MS) })
        }))
    }
}

/** "triplet:<role>": at least one triplet (3 hits over 2 cells, or 6 over a beat) on the role's pad, in any part. */
internal class Triplet(val role: String) : TutoKind {
    override fun done(step: TutoStep, p: Project, target: TutoTarget, armed: Any?) = p.allScenes.any { sc -> hasTriplet(sc.drums, lanesOf(role, target)) }
    override fun shown(step: TutoStep, p: Project, target: TutoTarget, level: TutoLevel?): Project? = withTriplet(p, role, target)
}

/** "notes:acid": the 303's line, every step of it, its note, its accent and its slide. */
internal object AcidNotes : TutoKind {
    override fun done(step: TutoStep, p: Project, target: TutoTarget, armed: Any?) =
        target.acid?.let { a -> p.allScenes.any { acidMatches(it.acid, a) } } ?: false

    override fun shown(step: TutoStep, p: Project, target: TutoTarget, level: TutoLevel?): Project? {
        val a = target.acid ?: return null
        val wanted = acidWanted(step, p, target) ?: return null
        val mine = onRoot(p.acid, a.root)
        return p.copy(acid = mine.copy(length = maxOf(mine.length, wantedLength(wanted, a)), steps = mine.steps.mapIndexed { i, st -> wanted[i] ?: st }))
    }

    // Its first 4 bars written: « Copier sur les mesures suivantes ».
    override fun requires(step: TutoStep, p: Project, target: TutoTarget) = copyNeed(step, p, target, TutoTarget.ACID)
    override fun line(step: TutoStep, target: TutoTarget) = target.acid?.let(::acidNotes)

    // The 303's steps written right: the note, and its accent and slide as the line has them.
    override fun mine(step: TutoStep, p: Project, target: TutoTarget) = target.acid?.let { a ->
        val want = acidNotes(a).associateBy { it.start }
        acidNotes(p.acid).filter { n -> want[n.start]?.let { it.length == n.length } != false }
    }
    override val wholeLoop get() = true
}

/** The frames that write [wanted] on the track [id] of [start], a chord at a time, in a clip as long as [end]'s (null: its own). */
private fun noteFrames(start: Project, id: Int, end: InstrumentTrack?, wanted: List<Note>): TutoFrames {
    val frames = ArrayList<Project>()
    val sounds = ArrayList<TutoSound?>()
    var clip = start.track(id).notes.withSteps(end?.notes?.steps ?: start.track(id).notes.steps).copy(notes = emptyList())
    frames += start.withTrack(start.track(id).copy(notes = clip)); sounds += null
    for ((_, chord) in wanted.groupBy { it.start }.entries.sortedBy { it.key }) {
        clip = clip.copy(notes = clip.notes + chord)
        frames += start.withTrack(start.track(id).copy(notes = clip)); sounds += TutoSound(track = id, pitches = chord.map { it.pitch })
    }
    return TutoFrames(frames, sounds, whole = true)
}

/**
 * "notes:<role>": every dotted note written on the role's track (not 3 of 12: the testers' bass and chords ticked
 * with the screen's first notes). A step split in two (« la mesure 1 », then « Copier » and the rest: tour 6, T8)
 * reads and writes the cells of its [TutoStep.length] only.
 */
internal class Notes(val role: String) : TutoKind {
    private fun own(step: TutoStep, notes: List<Note>) = notes.filter { step.length <= 0 || it.start < step.length * NoteClip.TICKS_PER_STEP }

    /** [p] with the role's track (made with its inserts when it was not there) and its id. */
    private fun onTrack(p: Project, target: TutoTarget, level: TutoLevel?): Pair<Project, Int>? =
        ensureTrack(p, role, target)?.let { (q, id) -> (if (trackFor(p, role) == null) insertsLeft(q, id, role, target, level) else q) to id }

    override fun done(step: TutoStep, p: Project, target: TutoTarget, armed: Any?) = target.roleNotes(role)?.let { wanted ->
        trackFor(p, role)?.let { id -> notesMatch(notesOf(p, id), own(step, wanted.notes)) }
    } ?: false

    override fun shown(step: TutoStep, p: Project, target: TutoTarget, level: TutoLevel?): Project? {
        val all = target.roleNotes(role) ?: return null
        // A step cut in two: the demo lays its own bars only, as its check reads them.
        val notes = if (step.length <= 0) all else all.copy(notes = own(step, all.notes))
        val (q, id) = onTrack(p, target, level) ?: return null
        return q.withTrack(q.track(id).copy(notes = notes))
    }

    // Testers (lot « tuto 5 »: the show wrote other bars than the dotted ones): the notes dotted now, no more.
    override fun frames(step: TutoStep, p: Project, target: TutoTarget, level: TutoLevel?, end: Project): TutoFrames? {
        val (start, id) = onTrack(p, target, level) ?: return null
        return noteFrames(start, id, trackFor(end, role)?.let { end.track(it) }, own(step, wantedNotes(step, p, target).orEmpty()))
    }
    override val endsOnFrames get() = true

    // Its track first; its first 4 bars written: « Copier sur les mesures suivantes ».
    override fun requires(step: TutoStep, p: Project, target: TutoTarget) =
        if (trackFor(p, role) == null) TutoNeed.Track(role) else copyNeed(step, p, target, role)

    // The first 4 bars while they are not all written, then the whole line (testers, lot « tuto 4 »).
    override fun ghosts(step: TutoStep, p: Project, target: TutoTarget): Pair<Int, List<Note>>? {
        val id = trackFor(p, role) ?: return null
        return id to (wantedNotes(step, p, target) ?: return null)
    }
    override fun line(step: TutoStep, target: TutoTarget) = target.roleNotes(role)?.notes
    override fun mine(step: TutoStep, p: Project, target: TutoTarget) = trackFor(p, role)?.let { notesOf(p, it) }
    override val wholeLoop get() = true
}

/** "track:sub": a Sub track that plays the bass an octave lower (« Copier la basse une octave plus bas »: the track and its notes in one touch). */
internal object SubTrack : TutoKind {
    private fun sub(p: Project) = p.instrumentTracks.firstOrNull { roleOf(p, it.id) == "sub" }?.id

    override fun done(step: TutoStep, p: Project, target: TutoTarget, armed: Any?) = trackFor(p, "bass")?.let { bass ->
        val wanted = subOf(notesOf(p, bass))
        p.instrumentTracks.any { t -> t.id != bass && roleOf(p, t.id) == "sub" && notesMatch(notesOf(p, t.id), wanted) }
    } ?: false

    override fun shown(step: TutoStep, p: Project, target: TutoTarget, level: TutoLevel?) = withSub(p, target)

    override fun frames(step: TutoStep, p: Project, target: TutoTarget, level: TutoLevel?, end: Project): TutoFrames? {
        val (start, id) = partTrack(p, "sub", target) ?: return null
        val endTrack = sub(end)?.let { end.track(it) }
        return noteFrames(start, id, endTrack, endTrack?.notes?.notes.orEmpty())
    }
    override val endsOnFrames get() = true
    override fun showable(p: Project, target: TutoTarget) = partTrack(p, "sub", target) != null
    override fun requires(step: TutoStep, p: Project, target: TutoTarget) = TutoNeed.Track("sub").takeIf { trackFor(p, "bass") != null }

    override fun ghosts(step: TutoStep, p: Project, target: TutoTarget): Pair<Int, List<Note>>? {
        val bass = trackFor(p, "bass") ?: return null
        val id = sub(p) ?: return null
        return id to StarterWizard.subNotes(notesOf(p, bass))
    }
}

/** "insert:<EFFECT>": the effect on each track the step names ([TutoStep.target]: the pump on the Sub and the Basse), not anywhere in the mix. */
internal class PutInsert(val effect: String) : TutoKind {
    override fun done(step: TutoStep, p: Project, target: TutoTarget, armed: Any?) =
        insertChannels(p, step).let { chans -> chans.isNotEmpty() && chans.all { hasInsert(p, it, effect) } }

    override fun shown(step: TutoStep, p: Project, target: TutoTarget, level: TutoLevel?): Project? {
        val type = EffectType.entries.firstOrNull { it.name == effect } ?: return null
        return toDo(step, p).fold(p) { q, ch -> withInsert(q, ch, Insert(type.ordinal, type.defaults)) ?: q }
    }
    override fun toDo(step: TutoStep, p: Project) = insertChannels(p, step).filterNot { hasInsert(p, it, effect) }
}

/** "send:<bus>": every strip the step names sends to the bus (the « Accords » track AND the snare), not any strip. */
internal class SendTo(val bus: Int?) : TutoKind {
    override fun done(step: TutoStep, p: Project, target: TutoTarget, armed: Any?) =
        bus != null && sendChannels(p, bus, step).let { chans -> chans.isNotEmpty() && chans.all { sends(p, it, bus) } }

    override fun shown(step: TutoStep, p: Project, target: TutoTarget, level: TutoLevel?): Project? {
        val chans = sendChannels(p, bus ?: return null, step)
        if (chans.isEmpty()) return null
        return withChannels(p, chans) { it.copy(sends = it.sends.mapIndexed { b, v -> if (b == bus) maxOf(v, if (bus == 0) 0.3f else 0.25f) else v }) }
    }
    override fun toDo(step: TutoStep, p: Project) = bus?.let { b -> sendChannels(p, b, step).filterNot { sends(p, it, b) } }.orEmpty()
}

/** "automation[:<shape>]": a curve drawn over the part (b/280: « Par partie » touched, no curve, ticked): rising, falling or up and down. */
internal class Curve(val shape: String) : TutoKind {
    override fun done(step: TutoStep, p: Project, target: TutoTarget, armed: Any?) =
        curveDrawn(p, step.target.ifEmpty { "pad" }, partsOf(step).first(), shape)
    override fun shown(step: TutoStep, p: Project, target: TutoTarget, level: TutoLevel?) = curve(p, step, target)
    override fun workPart(step: TutoStep, p: Project) = partsOf(step).first()
    override fun cited(step: TutoStep) = partsOf(step)
}

/** "master": an effect on, on the master. */
internal object MasterFx : TutoKind {
    override fun done(step: TutoStep, p: Project, target: TutoTarget, armed: Any?) = p.mixer.master.any { it.effect != EffectType.NONE && !it.bypass }
    override fun shown(step: TutoStep, p: Project, target: TutoTarget, level: TutoLevel?) = p.copy(mixer = p.mixer.copy(master = target.song.mixer.master))
}

/** "loudness:<TARGET>": the master's « Volume visé » (its limiter, always last, brings the song there). */
internal class Loudness(val name: String) : TutoKind {
    override fun done(step: TutoStep, p: Project, target: TutoTarget, armed: Any?) = p.mixer.target.name == name
    override fun shown(step: TutoStep, p: Project, target: TutoTarget, level: TutoLevel?) =
        LoudnessTarget.entries.firstOrNull { it.name == name }?.let { p.copy(mixer = com.pocketstudio.model.LoudnessTargets.withTarget(p.mixer, it)) }
}

/** "changed:…": ticked by a change of what it watches ([arm]) since the step began; any change, unless the kind asks for more. */
internal abstract class Changed : TutoKind {
    override fun done(step: TutoStep, p: Project, target: TutoTarget, armed: Any?) = armed != null && arm(p) != armed
    override val watches get() = true
}

/** "changed:volumes[:<roles>]": each track the step names a little lower than when the step began (the testers' mix ticked with one of two); no role: any fader. */
internal class Volumes(val check: String?) : Changed() {
    override fun arm(p: Project) = p.mixer.channels.map { it.volume }

    override fun done(step: TutoStep, p: Project, target: TutoTarget, armed: Any?): Boolean {
        if (check == null) return super.done(step, p, target, armed)
        val base = armed as? List<*> ?: return false
        val chans = volumeChannels(p, check)
        return chans.isNotEmpty() && chans.all { ch -> (base.getOrNull(ch) as? Float)?.let { p.mixer.channels.getOrNull(ch)?.volume?.let { v -> v < it - 0.005f } } == true }
    }

    override fun shown(step: TutoStep, p: Project, target: TutoTarget, level: TutoLevel?): Project? {
        val chans = if (check == null) listOfNotNull(trackFor(p, "pad"), trackFor(p, "lead")).map(InstrumentTrack::mixerChannel)
            else volumeChannels(p, check).takeIf { it.isNotEmpty() } ?: return null
        return withChannels(p, chans) { it.copy(volume = it.volume * 0.8f) }
    }
}

/** "changed:synth:<role>[:<KNOB>+<KNOB>]": the knobs the step names turned up since it began (not the distortion the synth has none of); no knob: any change of the sound. */
internal class SynthKnobs(val role: String, val knobs: List<Int>) : Changed() {
    override fun arm(p: Project) = trackFor(p, role)?.let { id -> p.track(id).let { listOf(it.synth, it.plaits, it.engine) } }

    override fun done(step: TutoStep, p: Project, target: TutoTarget, armed: Any?): Boolean {
        if (knobs.isEmpty()) return super.done(step, p, target, armed)
        val base = armed as? List<*>
        val now = trackFor(p, role)?.let { p.track(it) }
        // The track as it was ([arm]: its synth, its Plaits, its engine), read the way it plays now.
        val before = (base?.getOrNull(0) as? SynthPatch)?.let { s -> now?.copy(synth = s, plaits = base.getOrNull(1) as? com.pocketstudio.model.PlaitsPatch ?: now.plaits) }
        return before != null && now != null && knobs.all { k ->
            val was = knobValue(before, k)
            knobValue(now, k)?.let { v -> was != null && (knobPct(v) - knobPct(was) >= KNOB_STEP_PCT || knobPct(v) >= 95) } == true
        }
    }

    // The knobs the step names, turned up a third.
    override fun shown(step: TutoStep, p: Project, target: TutoTarget, level: TutoLevel?): Project? {
        if (knobs.isEmpty()) return null
        val t = p.track(trackFor(p, role) ?: return null)
        return p.withTrack(knobs.fold(t) { u, k -> knobValue(u, k)?.let { withKnob(u, k, (it + 0.3f).coerceAtMost(1f)) } ?: u })
    }
}

/**
 * "changed:drums:<part>": a new hit on a variation's pad in the part named so (« change un détail » in the Drop 2).
 * Théo (lot « tuto 5 »): « percussion ou charley ouvert », not any hit (a closed hat ticked it).
 */
internal class DrumsChanged(val label: String) : Changed() {
    // The hits of every pad in the part.
    override fun arm(p: Project) = sceneLabelled(p, label).takeIf { it >= 0 }?.let { i -> sceneAt(p, i).drums.lanes.map { it.hits } }

    override fun done(step: TutoStep, p: Project, target: TutoTarget, armed: Any?) = (armed as? List<*>)?.let { base ->
        arm(p)?.withIndex()?.any { (i, now) -> i in variationLanes() && (now.keys - (base.getOrNull(i) as? Map<*, *>)?.keys.orEmpty()).isNotEmpty() }
    } == true

    override fun shown(step: TutoStep, p: Project, target: TutoTarget, level: TutoLevel?) = inScene(p, label) { q ->
        // A variation: an open hat on an empty cell near the end (the second to last first), else a percussion's.
        val n = q.drums.steps
        val cells = (n - 2 downTo n / 2) + (n - 1 downTo n / 2)
        val free = (listOf(OPEN_HAT_LANE) + variationLanes().filter { it != OPEN_HAT_LANE }.sorted()).firstNotNullOfOrNull { l ->
            cells.firstOrNull { it !in q.drums.lanes[l].hits }?.let { l to it }
        }
        if (free == null) q
        else q.copy(drums = q.drums.copy(lanes = q.drums.lanes.mapIndexed { i, l -> if (i == free.first) l.copy(hits = l.hits + (free.second to Hit(0.8f))) else l }))
    }
    override fun workPart(step: TutoStep, p: Project) = label
    override fun cited(step: TutoStep) = listOf(label)
}

/** "changed:kit": another kick (its sample or its voice). */
internal object KitChanged : Changed() {
    override fun arm(p: Project): Any? = p.drums.lanes.getOrNull(0)?.sample to p.padVoices[0]
}

/** Any other "changed:…": it watches nothing, so nothing ticks it. */
internal object NothingWatched : Changed()
