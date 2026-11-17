package com.pocketstudio.model.tuto

import com.pocketstudio.model.Note
import com.pocketstudio.model.Project
import com.pocketstudio.model.guide.GuideStatus
import com.pocketstudio.model.tuto.TutoChecks.KEY_FRAMES
import com.pocketstudio.model.tuto.TutoChecks.SHARED_CARD
import com.pocketstudio.model.tuto.TutoChecks.SWING_POINTS
import com.pocketstudio.model.tuto.TutoChecks.sceneLabelled
import com.pocketstudio.model.tuto.TutoChecks.swingShown
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
