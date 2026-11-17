package com.pocketstudio.model.tuto

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 7, control of « fable-repar » (chapter 12, step 1): the words said « efface les 3 coups du pad « Charley » …
 * (touche les 3 croix rouges) » while the screen crossed out 4 hits of the pad « Clap » (the Facile course lays no
 * « Charley »). Every style, both levels, the course played up to its silence step, its part made: the pads its words
 * name are the pads crossed out, and the number of hits they give is the number of crosses.
 */
class Tour7TutoGapWordsMatchCrossesTest {
    private val pads = mapOf("« Charley »" to "hat", "« Clap »" to "clap", "kick" to "kick", "caisse" to "snare")

    @Test
    fun theSilenceStepNamesThePadsAndTheHitsItCrossesOut() {
        var met = 0
        var named = 0
        val faults = ArrayList<String>()
        for (style in TutoCourse.STYLES) for (level in TutoLevel.entries) {
            val target = TutoTarget.of(style) ?: continue
            var p = target.blank()
            for (s in TutoCourse.of(style)!!.chaptersFor(level).flatMap { it.steps }) {
                if (s.manual || s.listening || s.check == "shared") continue
                if (s.check == "gap") {
                    // The user's path: the step's buttons (« Créer la partie », « Ouvrir la partie ») until the crosses show.
                    var q = p
                    repeat(6) {
                        val need = TutoPlay.button(s, q, target, TutoPlay.arrive(s, q), level) ?: return@repeat
                        q = TutoPlay.press(need, s, q, target, level)?.first ?: return@repeat
                    }
                    val crossed = TutoPlay.toErase(s, q, target, TutoPlay.button(s, q, target, TutoPlay.arrive(s, q), level)).orEmpty()
                    val words = s.textFor(level)
                    met++
                    if (crossed.isEmpty()) faults += "$style $level: nothing crossed out"
                    val said = pads.filterKeys { it in words }.values
                    if (said.isNotEmpty()) {
                        named++
                        val lanes = said.flatMap { TutoChecks.lanesOf(it, target) }.toSet()
                        if (!lanes.containsAll(crossed.keys)) faults += "$style $level: the words name $said (pads $lanes), the crosses are on pads $crossed"
                    }
                    Regex("les (\\d+) (coups|croix)").findAll(words).forEach { m ->
                        val n = m.groupValues[1].toInt()
                        val hits = crossed.values.sumOf { it.size }
                        if (n != hits) faults += "$style $level: the words say « ${m.value} », $hits hits are crossed out: $crossed"
                    }
                }
                p = TutoPlay.play(s, p, target, level).project
            }
        }
        assertTrue("${faults.size} faults over $met silence steps ($named naming a pad):\n" + faults.joinToString("\n"), faults.isEmpty())
        assertTrue("only $met silence steps, $named naming a pad: the test checks too little", met >= 50 && named >= 6)
    }
}
