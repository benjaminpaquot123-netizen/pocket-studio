package com.pocketstudio.model.tuto

import com.pocketstudio.model.Hit
import com.pocketstudio.model.Project
import com.pocketstudio.model.allScenes
import com.pocketstudio.model.switchScene
import com.pocketstudio.model.tuto.TutoPlay.End
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * « Montre-moi partout » (tour 7, audit « Fable », § 3.1): every step of every course, at both levels, played as the
 * app plays it ([TutoPlay.play], the functions `TutoController` calls) from the seven ways a user lands on it.
 *
 * Four things hold, wherever the step is opened from:
 * 1. its buttons then « Montre-moi » tick it;
 * 2. it is never ticked on arrival nor by its buttons alone when its words ask for a gesture, and the right gesture
 *    made in another part does not tick it;
 * 3. no dead end: there is always a button that changes the project, or « Montre-moi »;
 * 4. the dotted cells and the crosses are what « Montre-moi » touches, in the step's own part.
 */
class TutoShowMeEverywhereTest {
    /** O: the course in order, from the tutorial's empty project. */
    @Test
    fun inOrder() = everyCourse("O") { r -> r.steps.forEach { r.p = r.played(it, r.p) } }

    /** S: the step before skipped (« Suivant » twice): each step from what the one before the last left. */
    @Test
    fun withThePreviousStepSkipped() = everyCourse("S") { r ->
        val before = ArrayList<Project>()
        var p = r.p
        for (s in r.steps) { before += p; p = TutoPlay.play(s, p, r.target, r.level).project }
        r.steps.forEachIndexed { i, s -> if (i >= 1) r.played(s, before[i - 1]) }
    }

    /** V: a chapter opened from the list on the empty project. */
    @Test
    fun aChapterOpenedOnTheEmptyProject() = everyCourse("V") { r -> r.chapters { r.p } }

    /** K: a chapter opened from the list with one kick played. */
    @Test
    fun aChapterOpenedOnOneKick() = everyCourse("K") { r -> val k = oneKick(r.p, r.target); r.chapters { k } }

    /** B: a chapter opened from the list, the loop made, no part yet. */
    @Test
    fun aChapterOpenedOnTheLoop() = everyCourse("B") { r -> val b = r.loop() ?: return@everyCourse; r.chapters(fromParts = true) { b } }

    /** k: a step opened alone (not its chapter's first) with one kick played. */
    @Test
    fun aStepOpenedOnOneKick() = everyCourse("k") { r -> val k = oneKick(r.p, r.target); r.alone { k } }

    /** b: a step opened alone (not its chapter's first), the loop made, no part yet. */
    @Test
    fun aStepOpenedOnTheLoop() = everyCourse("b") { r -> val b = r.loop() ?: return@everyCourse; r.alone(fromParts = true) { b } }

    /** One course at one level: its steps « Montre-moi » can do, the project it runs on, the faults found. */
    private class Run(val style: String, val level: TutoLevel, val course: TutoCourse, val target: TutoTarget, val faults: MutableList<String>) {
        var p: Project = target.blank()
        var played = 0
        val chapters: List<TutoChapter> = course.chaptersFor(level)
        val steps: List<TutoStep> = chapters.flatMap { it.steps }.filter(::inReach)

        /** [step] played from [from]: its faults noted, the project it left. */
        fun played(step: TutoStep, from: Project): Project {
            played++
            fun fault(what: String) { faults += "$style $level ${step.id} [${step.check}]: $what" }
            var hints: String? = null
            val o = TutoPlay.play(step, from, target, level) { q, need -> if (hints == null) hints = hintFault(step, q, need) }
            when (o.end) {
                End.Done -> Unit
                End.Already -> if (TutoPlay.asksAGesture(step)) fault("ticked on arrival")
                End.DoneWithoutGesture -> fault("ticked by its buttons alone")
                End.NeverDone -> fault("never ticked")
                End.DeadEnd -> fault("dead end (${o.why})")
            }
            hints?.let(::fault)
            if (o.end == End.Done) elsewhere(step, o)?.let(::fault)
            return o.project
        }

        /** Check 4, on the state [q] a touch is made on: the dotted cells and the crosses against « Montre-moi ». */
        private fun hintFault(step: TutoStep, q: Project, need: TutoNeed?): String? {
            // R-D, worked out without [TutoPlay.inItsPart] (the app's own rule cannot judge itself): each other part
            // shown instead, cells are dotted or crossed there only when « Montre-moi » changes that part's drums, or
            // when touching them there ticks the step (the crash of « Drop » is as good in « Drop 2 »).
            if (need == null && named(step)) for (i in q.allScenes.indices) {
                if (i == q.scene || q.allScenes[i].isEmpty) continue
                val there = q.switchScene(i).sanitized()
                val dotted = TutoPlay.hint(step, there, target, there.drums.steps, null, ticked = false)
                val crossed = TutoPlay.toErase(step, there, target, null).orEmpty().filterValues { it.isNotEmpty() }
                if (dotted == null && crossed.isEmpty()) continue
                val shown = TutoPlay.showMe(step, there, target, level)?.frames?.lastOrNull()?.switchScene(i)?.sanitized()
                if (shown != null && shown.drums != there.drums) continue
                val touched = there.copy(drums = there.drums.copy(lanes = there.drums.lanes.mapIndexed { k, l ->
                    l.copy(hits = l.hits + (dotted?.takeIf { it.first == k }?.second.orEmpty().associateWith { Hit(1f) }) - crossed[k].orEmpty())
                })).sanitized()
                if (!TutoPlay.done(step, touched, target, TutoPlay.arrive(step, q))) {
                    return "dotted cells or crosses in « ${q.allScenes[i].label} », where « Montre-moi » changes nothing and touching them does not tick the step"
                }
            }
            val hint = TutoPlay.hint(step, q, target, q.drums.steps, need, ticked = false)
            val erase = TutoPlay.toErase(step, q, target, need).orEmpty().filterValues { it.isNotEmpty() }
            if (hint == null && erase.isEmpty()) return null
            if (need != null) return null
            val after = TutoPlay.showMe(step, q, target, level)?.frames?.lastOrNull()?.sanitized() ?: return null
            if (after.scene != q.scene) return "dotted cells in the part shown, « Montre-moi » plays in another"
            if (hint != null) {
                val (lane, cells) = hint
                val had = q.drums.lanes[lane].hits.keys
                val added = after.drums.lanes[lane].hits.keys - had
                if (cells - had != added) return "dotted cells ${(cells - had).sorted()} on line $lane, « Montre-moi » adds ${added.sorted()}"
            }
            for ((lane, cells) in erase) {
                val removed = q.drums.lanes[lane].hits.keys - after.drums.lanes[lane].hits.keys
                if (cells != removed) return "crosses ${cells.sorted()} on line $lane, « Montre-moi » takes ${removed.sorted()}"
            }
            return null
        }

        /** Check 2, second half: the drums « Montre-moi » left in the step's part, put in another part instead, do not tick it. */
        private fun elsewhere(step: TutoStep, o: TutoPlay.Outcome): String? {
            val before = o.before ?: return null
            if (!named(step)) return null
            val own = o.project.scene
            if (before.switchScene(own).drums == o.project.drums) return null
            val other = before.allScenes.indices.firstOrNull { i ->
                i != own && !before.allScenes[i].isEmpty && TutoChecks.partsOf(step).none { TutoChecks.isLabel(before.allScenes[i].label, it) }
            } ?: return null
            val q = before.switchScene(other).copy(drums = o.project.drums).switchScene(before.scene).sanitized()
            return if (TutoPlay.done(step, q, target, o.base)) "ticked by the gesture made in « ${before.allScenes[other].label} »" else null
        }

        /** Each chapter opened from the list on [from]: its steps in order, each on what the one before left. */
        fun chapters(fromParts: Boolean = false, from: () -> Project) {
            for (c in opened(fromParts)) { var q = from(); c.steps.filter(::inReach).forEach { q = played(it, q) } }
        }

        /** Each step that is not its chapter's first, opened alone on [from]. */
        fun alone(fromParts: Boolean = false, from: () -> Project) {
            for (c in opened(fromParts)) c.steps.drop(1).filter(::inReach).forEach { played(it, from()) }
        }

        /** The chapters opened: all of them, or ([fromParts]) from the first that makes a part (the loop's own are made already). */
        private fun opened(fromParts: Boolean): List<TutoChapter> =
            if (!fromParts) chapters else chapters.dropWhile { c -> c.steps.none { it.check.startsWith("part:") } }

        /** The course in order up to its first part: the loop made, no part yet; null for a course that makes no part. */
        fun loop(): Project? {
            if (steps.none { it.check.startsWith("part:") }) return null
            var q = p
            for (s in steps) { if (s.check.startsWith("part:")) break; q = TutoPlay.play(s, q, target, level).project }
            return q
        }
    }

    /** [body] on every course at both levels; its faults, counted by kind, fail the test. */
    private fun everyCourse(arrival: String, body: (Run) -> Unit) {
        assertTrue("no course to play", TutoCourse.STYLES.size >= MIN_COURSES)
        val faults = ArrayList<String>()
        var played = 0
        for (style in TutoCourse.STYLES) for (level in TutoLevel.entries) {
            val r = Run(style, level, TutoCourse.of(style)!!, TutoTarget.of(style)!!, faults)
            body(r)
            played += r.played
        }
        assertTrue("arrival $arrival: nothing played", played >= MIN_COURSES)
        val kinds = faults.groupingBy { it.substringAfter("]: ").substringBefore(" (").substringBefore(" «").substringBefore(" [") }.eachCount()
        // The whole list, for who repairs: PS_TUTO_FAULTS=<folder> writes one file per arrival.
        System.getenv("PS_TUTO_FAULTS")?.let { java.io.File(it, "$arrival.txt").writeText(faults.joinToString("\n")) }
        val head = "arrival $arrival: ${faults.size} faults on $played steps played $kinds"
        assertTrue(head + "\n" + faults.take(SHOWN).joinToString("\n") + if (faults.size > SHOWN) "\n… and ${faults.size - SHOWN} more" else "", faults.isEmpty())
    }

    private companion object {
        /** The courses of `tutos/` (37 on 2026-10-29): a test looping over none passes for nothing. */
        const val MIN_COURSES = 37

        /** The faults written out in a failure (the count by kind says the rest). */
        const val SHOWN = 60

        /** The steps « Montre-moi » can do: not « C'est fait », an example to hear, the share (nothing in the project to check). */
        fun inReach(s: TutoStep): Boolean = !(s.manual || s.listening || s.check == "shared")

        /** A step that names the part it is played in. */
        fun named(s: TutoStep): Boolean = s.part.isNotBlank() || s.check in setOf("crash", "gap", "fill") || s.check.startsWith("changed:drums:")

        /** [p] with one kick on the first cell: the user's first touch, then the list of chapters. */
        fun oneKick(p: Project, target: TutoTarget): Project {
            val lane = TutoChecks.lanesOf("kick", target).first()
            return p.copy(drums = p.drums.toggle(lane, 0)).sanitized()
        }
    }
}
