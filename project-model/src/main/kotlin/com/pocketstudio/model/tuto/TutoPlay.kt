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
    fun button(step: TutoStep, p: Project, target: TutoTarget, base: Any?, level: TutoLevel? = null): TutoNeed? = TutoChecks.need(step, p, target, base, level)

    /** [p] once the main button [need] is touched, and the track it made; null when it can do nothing ([TutoChecks.fulfil]). */
    fun press(need: TutoNeed, step: TutoStep, p: Project, target: TutoTarget, level: TutoLevel? = null): Pair<Project, Int?>? = TutoChecks.fulfil(need, step, p, target, level)

    /**
     * « Je le fais » on an effect step (tour 8, seat e, ch.13: it only opened the effect sheet, the send stayed at
     * 0 % and no curve was drawn): [p] with the step's gesture made on [channel] alone, the strip the app goes to
     * (a send, an insert: one strip per touch, the next touch the next one), or its curve drawn; null for a step of
     * another kind, or when there is nothing left to make there.
     */
    fun doHere(step: TutoStep, p: Project, target: TutoTarget, channel: Int?, level: TutoLevel? = null): Project? {
        val c = step.check
        return when {
            c.startsWith("send:") || c.startsWith("insert:") -> {
                val ch = channel ?: return null
                val made = TutoChecks.show(step, p, target, level)?.mixer?.channels?.getOrNull(ch) ?: return null
                p.copy(mixer = p.mixer.copy(channels = p.mixer.channels.mapIndexed { i, x -> if (i == ch) made else x })).takeIf { it != p }
            }
            // Only the curves and the strips they work on (the filter it adds there), never the rest of the project
            // the show hands back (review of T0b: the user's work came from the show, not from [p]).
            c.startsWith("automation") -> {
                val made = TutoChecks.show(step, p, target, level) ?: return null
                val strips = made.automation.filter { it !in p.automation }.map { it.target.ch }.toSet()
                p.copy(
                    automation = made.automation,
                    mixer = p.mixer.copy(channels = p.mixer.channels.mapIndexed { i, x ->
                        if (i in strips) made.mixer.channels.getOrNull(i)?.let { x.copy(inserts = it.inserts) } ?: x else x
                    }),
                ).takeIf { it != p }
            }
            else -> null
        }
    }

    /** « Montre-moi » of [step] on [p]: its frames, the last one being what the user is to reach; null when it has none.
     *  [level]: the course's level, so the show leaves to a later step of that level what it teaches. */
    fun showMe(step: TutoStep, p: Project, target: TutoTarget, level: TutoLevel? = null): TutoDemo? = TutoChecks.demo(step, p, target, level)

    /** Whether [step] has a « Montre-moi » on [p]: [showMe] is not null (the panel's button and the step's main button read this one answer). */
    fun canShow(step: TutoStep, p: Project, target: TutoTarget, level: TutoLevel? = null): Boolean = TutoChecks.canShow(step, p, target, level)

    /**
     * Whether [need] asks for a part [p] holds, with nothing in it (tour 8, seat d, V2: the « Montée » emptied, the
     * panel said there was no such part): its button fills it, no word says it is missing.
     */
    fun emptyPart(p: Project, need: TutoNeed?): Boolean =
        need is TutoNeed.Part && p.allScenes.any { TutoChecks.isLabel(it.label, need.label) && it.isEmpty }

    /** Whether [step] is ticked on [p] ([base]: what it watched when it began). */
    fun done(step: TutoStep, p: Project, target: TutoTarget, base: Any?): Boolean = TutoChecks.done(step, p, target, base)

    /**
     * Whether [step], ticked earlier, is to do again on [p] (tour 7, seat d: the « Montée » renamed, an empty part
     * given its name, the roll's step read « Déjà fait »): a transition, read in its part as it is now; a step that
     * makes a part or works in one, when no part of that name holds anything. Any other step keeps its tick.
     */
    fun undone(step: TutoStep, p: Project, target: TutoTarget): Boolean {
        val c = step.check
        return when {
            step.manual || step.listening || c.startsWith("changed:") -> false
            c == "fill" || c == "gap" || c.startsWith("crash") -> !done(step, p, target, null)
            c.startsWith("part:") -> TutoChecks.partContent(p, c.removePrefix("part:").substringBeforeLast(':')) == null
            step.part.isNotBlank() -> TutoChecks.partsOf(step).all { TutoChecks.partContent(p, it) == null }
            else -> false
        }
    }

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
     * Null in a part that is not the step's own ([inItsPart]), or when its line is whole in another part
     * ([TutoChecks.heldElsewhere]).
     */
    fun hint(step: TutoStep, p: Project, target: TutoTarget, steps: Int, need: TutoNeed?, ticked: Boolean): Pair<Int, Set<Int>>? {
        // R-D: nothing of the step out of its part, the panel's button ([need]) read again or not.
        if (!inItsPart(step, p) || TutoChecks.heldElsewhere(step, p, target)) return null
        if (step.check == "crash" && need == null && !ticked) return TutoChecks.CRASH_LANE to setOf(0)
        return TutoChecks.hint(step, target, steps)
    }

    /**
     * The dotted cell the « Touche ici » ring goes round (tour 8, seat a, ch.3: no ring on the Clap's cells): the
     * first of [cells] in the bar shown ([from] until [until]) not yet [filled]; null when the bar holds none to touch.
     */
    fun ringed(cells: Set<Int>, filled: Set<Int>, from: Int, until: Int): Int? =
        cells.filter { it in from until until && it !in filled }.minOrNull()

    /** The hits [step] crosses out in the part shown, null when it takes none, while a button is to touch first, or in a part that is not the step's own. */
    fun toErase(step: TutoStep, p: Project, target: TutoTarget?, need: TutoNeed?): Map<Int, Set<Int>>? =
        if (need != null || !inItsPart(step, p) || (target != null && TutoChecks.heldElsewhere(step, p, target))) null else TutoChecks.toErase(step, p, target)

    /** The dotted notes of [step] on its track in the part shown ([TutoChecks.ghostNotes]); null when it has none, or when its line is whole in another part ([TutoChecks.heldElsewhere]). */
    fun notes(step: TutoStep, p: Project, target: TutoTarget): Pair<Int, List<com.pocketstudio.model.Note>>? =
        if (TutoChecks.heldElsewhere(step, p, target)) null else TutoChecks.ghostNotes(step, p, target)

    /** The dotted 303 steps of [step] in the part shown ([TutoChecks.acidWanted]); null when it has none, or when its line is whole in another part. */
    fun acid(step: TutoStep, p: Project, target: TutoTarget): Map<Int, com.pocketstudio.model.AcidStep>? =
        if (TutoChecks.heldElsewhere(step, p, target)) null else TutoChecks.acidWanted(step, p, target)

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
    fun play(step: TutoStep, from: Project, target: TutoTarget, level: TutoLevel? = null, watch: (Project, TutoNeed?) -> Unit = { _, _ -> }): Outcome {
        var p = from
        var held = arrive(step, p)
        if (done(step, p, target, held)) return Outcome(End.Already, p, base = held)
        var before: Project? = null
        var beforeBase: Any? = null
        // « Copier » is the step's own gesture (its words ask for it), not a button that prepares it.
        var copied = false
        repeat(MAX_TOUCHES) {
            held = base(step, held, p)
            val need = button(step, p, target, held, level)
            watch(p, need)
            val next = if (need != null) {
                if (need is TutoNeed.Copy) copied = true
                press(need, step, p, target, level)?.first?.sanitized()?.takeIf { it != p }
                    ?: return Outcome(End.DeadEnd, p, before, held, "button $need does nothing")
            } else {
                if (before == null) { before = p; beforeBase = held }
                showMe(step, p, target, level)?.frames?.lastOrNull()?.sanitized() ?: return Outcome(End.DeadEnd, p, null, held, "no button, no Montre-moi")
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
