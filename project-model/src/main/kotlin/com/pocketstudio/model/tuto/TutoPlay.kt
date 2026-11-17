package com.pocketstudio.model.tuto

import com.pocketstudio.model.Project
import com.pocketstudio.model.allScenes

/**
 * One step of a course, played as the app plays it (tour 7, audit « Fable », § 3.1): what it watches taken when the
 * step begins, its main button while it offers one, else « Montre-moi ». The app (`TutoController`) and the test
 * « Montre-moi partout » both go through these functions, so the test cannot play a step another way than the app.
 */
object TutoPlay {
    /** How a step ended once played. */
    enum class End {
        /** Ticked when it began: nothing was asked. */
        Already,

        /** Ticked by the buttons that prepare it alone, though its words ask for a gesture (« Copier » is one). */
        DoneWithoutGesture,

        /** Ticked after « Montre-moi ». */
        Done,

        /** Buttons and « Montre-moi » over and over: never ticked. */
        NeverDone,

        /** Nothing to touch: a button that changes nothing, or no button and no « Montre-moi ». */
        DeadEnd,
    }

    /**
     * [end] and the [project] it left; [before], the project just before the first « Montre-moi » (null when none
     * was played), [base] what the step watched then; [why], the dead end in words.
     */
    data class Outcome(val end: End, val project: Project, val before: Project? = null, val base: Any? = null, val why: String = "")

    /** How many touches (buttons and « Montre-moi ») a step is given before it is said never done. */
    const val MAX_TOUCHES = 14

    /** What a "changed:…" step watches, as it is when the step begins; null for any other step, or while the thing is not there. */
    fun arrive(step: TutoStep, p: Project): Any? =
        step.check.takeIf { it.startsWith("changed:") }?.let { TutoChecks.signature(it.removePrefix("changed:"), p) }

    /**
     * What [step] compares [p] to: [held], taken when it began ([arrive]); a part the step changes that was made
     * after it began (« Créer la partie ») is taken as it was made (tour 7, seat d: a null one never ticked).
     */
    fun base(step: TutoStep, held: Any?, p: Project): Any? =
        if (held == null && step.check.startsWith("changed:")) arrive(step, p) else held

    /** The step's main button now, null when it has none ([TutoChecks.need]). */
    fun button(step: TutoStep, p: Project, target: TutoTarget, base: Any?): TutoNeed? = TutoChecks.need(step, p, target, base)

    /** [p] once the main button [need] is touched, and the track it made; null when it can do nothing ([TutoChecks.fulfil]). */
    fun press(need: TutoNeed, step: TutoStep, p: Project, target: TutoTarget): Pair<Project, Int?>? = TutoChecks.fulfil(need, step, p, target)

    /** « Montre-moi » of [step] on [p]: its frames, the last one being what the user is to reach; null when it has none. */
    fun showMe(step: TutoStep, p: Project, target: TutoTarget): TutoDemo? = TutoChecks.demo(step, p, target)

    /** Whether [step] is ticked on [p] ([base]: what it watched when it began). */
    fun done(step: TutoStep, p: Project, target: TutoTarget, base: Any?): Boolean = TutoChecks.done(step, p, target, base)

    /**
     * Whether the words of [step] ask for a gesture in the project beyond its main button: not a part to make
     * (« Créer la partie »), a part to name, the Sub to copy, the lengths to give, which their button does whole.
     */
    fun asksAGesture(step: TutoStep): Boolean {
        val c = step.check
        return !(c.startsWith("part:") || c.startsWith("label:") || c.startsWith("track:") || c == "song:phrases" || step.manual || step.listening || c == "shared")
    }

    /**
     * The dotted cells of [step] on a pad of [steps] cells, null when it shows none: the line of a "lane:" step, the
     * crash's first cell while the step is to do ([ticked]: false) and no button is left to touch ([need]: null).
     */
    fun hint(step: TutoStep, p: Project, target: TutoTarget, steps: Int, need: TutoNeed?, ticked: Boolean): Pair<Int, Set<Int>>? {
        if (step.check == "crash" && need == null && !ticked) return TutoChecks.CRASH_LANE to setOf(0)
        return TutoChecks.hint(step, target, steps)
    }

    /** The hits [step] crosses out in the part shown, null when it takes none or while a button is to touch first. */
    fun toErase(step: TutoStep, p: Project, target: TutoTarget?, need: TutoNeed?): Map<Int, Set<Int>>? =
        if (need != null) null else TutoChecks.toErase(step, p, target)

    /** Whether the part [p] shows is one of [step]'s own ([TutoChecks.partsOf]); true for a step of the whole loop. */
    fun inItsPart(step: TutoStep, p: Project): Boolean {
        val named = step.part.isNotBlank() || step.check == "crash" || step.check == "gap" || step.check == "fill"
        if (!named) return true
        val shown = p.allScenes.getOrNull(p.scene)?.label ?: return false
        return TutoChecks.partsOf(step).any { TutoChecks.isLabel(shown, it) }
    }

    /**
     * [step] played from [from]: what it watches taken on arrival, its main button while there is one, else the last
     * frame of « Montre-moi », until it is ticked. [watch] sees each state a touch is made on, with its button.
     */
    fun play(step: TutoStep, from: Project, target: TutoTarget, watch: (Project, TutoNeed?) -> Unit = { _, _ -> }): Outcome {
        var p = from
        var held = arrive(step, p)
        if (done(step, p, target, held)) return Outcome(End.Already, p, base = held)
        var before: Project? = null
        var beforeBase: Any? = null
        // « Copier » is the step's own gesture (its words ask for it), not a button that prepares it.
        var copied = false
        repeat(MAX_TOUCHES) {
            held = base(step, held, p)
            val need = button(step, p, target, held)
            watch(p, need)
            val next = if (need != null) {
                if (need is TutoNeed.Copy) copied = true
                press(need, step, p, target)?.first?.sanitized()?.takeIf { it != p }
                    ?: return Outcome(End.DeadEnd, p, before, held, "button $need does nothing")
            } else {
                if (before == null) { before = p; beforeBase = held }
                showMe(step, p, target)?.frames?.lastOrNull()?.sanitized() ?: return Outcome(End.DeadEnd, p, null, held, "no button, no Montre-moi")
            }
            p = next
            held = base(step, held, p)
            if (done(step, p, target, held)) {
                return Outcome(if (before == null && !copied && asksAGesture(step)) End.DoneWithoutGesture else End.Done, p, before, beforeBase ?: held)
            }
        }
        return Outcome(End.NeverDone, p, before, held)
    }
}
