package com.pocketstudio.model.tuto

import com.pocketstudio.model.Project
import com.pocketstudio.model.allScenes
import com.pocketstudio.model.renameScene
import com.pocketstudio.model.switchScene
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 9, lot « fable6 » (the « montee-vide » alert of the tutorial: tour 7, seats tuto2 d and fable d V2), everywhere
 * it can come back. Acceptance, seen by the user: « Une partie vide, même nommée comme celle de l'étape (« Montée »,
 * « Drop », « Break »…), ne valide jamais l'étape : pas de coche, pas de « Déjà fait ! », pas de « Bravo » tant que je
 * n'y ai rien fait. » Every style, both levels, every step that works in a named part: its parts renamed « Pont », a
 * new EMPTY part given the name, when the step is reached (never done) and at the end of the course (ticked earlier).
 */
class Tour9Fable6EmptyPartNeverDoneTest {
    private fun inAPart(s: TutoStep): Boolean {
        val c = s.check
        return !s.manual && !s.listening && c != "shared" && !c.startsWith("changed:") && (c == "fill" || c == "gap" || c.startsWith("crash") || c.startsWith("part:") || s.part.isNotBlank())
    }

    private fun partsOf(s: TutoStep) = if (s.check.startsWith("part:")) listOf(s.check.removePrefix("part:").substringBeforeLast(':')) else TutoChecks.partsOf(s)

    /** The tester's gestures: each part of [labels] renamed « Pont », a new empty part named as the first of them, shown. */
    private fun renamedAndEmpty(p: Project, labels: List<String>): Project {
        var q = p
        q.allScenes.forEachIndexed { i, s -> if (labels.any { TutoChecks.isLabel(s.label, it) }) q = q.renameScene(i, "Pont ${i + 1}") }
        val free = q.allScenes.size
        q = q.switchScene(free).renameScene(free, labels.first())
        check(q.allScenes[free].let { it.isEmpty && TutoChecks.isLabel(it.label, labels.first()) })
        check(q.allScenes.count { s -> labels.any { TutoChecks.isLabel(s.label, it) } } == 1)
        return q
    }

    @Test
    fun anEmptyPartOfTheStepsNameNeverTicksIt() {
        val bad = ArrayList<String>()
        var seen = 0
        val kinds = HashSet<String>()
        for (style in TutoCourse.STYLES) for (level in TutoLevel.entries) {
            val target = TutoTarget.of(style) ?: continue
            val reached = ArrayList<Pair<TutoStep, Project>>()
            var p = target.blank()
            for (s in TutoCourse.of(style)!!.chaptersFor(level).flatMap { it.steps }) {
                if (s.manual || s.listening || s.check == "shared") continue
                if (inAPart(s)) reached += s to p
                p = TutoPlay.play(s, p, target, level).project
            }
            for ((s, before) in reached) {
                seen++
                kinds += s.check.substringBefore(':')
                val parts = partsOf(s)
                for ((moment, from) in listOf("when reached" to before, "course played" to p)) {
                    val q = renamedAndEmpty(from, parts)
                    val where = "$style ${level.name} ${s.id} [${s.check}] $moment, empty « ${parts.first()} »"
                    // The look the app makes on arrival, and the one that follows a gesture elsewhere.
                    if (TutoPlay.done(s, q, target, TutoPlay.arrive(s, q))) bad += "$where: ticked"
                    if (moment == "course played" && !TutoPlay.undone(s, q, target)) bad += "$where: its old tick is kept (« Déjà fait »)"
                    if (TutoPlay.play(s, q, target, level).end == TutoPlay.End.Already) bad += "$where: « Déjà fait » on arrival"
                }
            }
        }
        assertTrue("${bad.size} of $seen:\n" + bad.take(60).joinToString("\n"), bad.isEmpty())
        assertTrue("only $seen steps in a part, kinds $kinds: the test checks too little", seen > 300 && kinds.containsAll(listOf("fill", "gap", "crash", "part")))
    }
}
