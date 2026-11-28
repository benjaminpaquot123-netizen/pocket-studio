package com.pocketstudio.model.tuto

import com.pocketstudio.model.SongPart
import com.pocketstudio.model.allScenes
import com.pocketstudio.model.showing
import com.pocketstudio.model.syncScenes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 8, lot T0b (seat d): the Montée made by « Créer la partie » was laid after the Break, with the user's own
 * parts (« Pont », a fill) between it and the Drop 2. Acceptance: « je crée la Montée et elle se place juste avant
 * le Drop 2 ».
 */
class Tour8TutoGesturesMonteeTest {
    @Test
    fun theMonteeMadeLandsJustBeforeTheDropTwo() {
        var seen = 0
        val done = HashSet<String>()
        for (style in TutoCourse.STYLES) {
            val target = TutoTarget.of(style) ?: continue
            val spec = TutoCourse.of(target.style.id)?.chapters?.flatMap { it.steps }?.map { it.song }
                ?.firstOrNull { s -> TutoChecks.barsOfList(s).any { TutoChecks.isLabel("Montée", it.first) } } ?: continue
            val example = target.song.syncScenes()
            fun sceneOf(label: String) = example.allScenes.indexOfFirst { TutoChecks.isLabel(it.label, label) && !it.isEmpty }
            // The song as the course lays it out (seat d's: Intro, Intro 2, Drop, Break, Montée, Drop 2, Fin).
            val order = TutoChecks.barsOfList(spec).mapNotNull { (l, bars) -> sceneOf(l).takeIf { it >= 0 }?.let { SongPart(it, 1, bars = bars) } }
            val m = sceneOf("Montée")
            // The part that follows the Montée in the course's order (the Drop 2 in techno).
            val wanted = TutoChecks.barsOfList(spec)
            val d2 = wanted.indexOfFirst { TutoChecks.isLabel("Montée", it.first) }.let { wanted.getOrNull(it + 1) }?.let { sceneOf(it.first) } ?: -1
            if (m < 0 || d2 < 0 || order.none { it.scene == m } || order.none { it.scene == d2 }) continue
            val song = example.copy(song = order)
            // The user renamed the Montée « Pont » (it stays in the song, between the Break and the Drop 2), then the
            // tuto makes a new Montée (« Créer » fills it: a part with nothing in it is no part to place).
            val old = song.allScenes[m]
            val scenes = song.allScenes.mapIndexed { i, s -> if (i == m) s.copy(name = "Pont", label = "Pont") else s } + old.copy(name = "Montée", label = "Montée")
            val p = song.showing(scenes, song.scene)
            val made = p.allScenes.lastIndex
            val q = TutoChecks.placed(p, "Montée", target)
            val at = q.song.indexOfFirst { it.scene == made }
            assertTrue("$style: the Montée is in the song", at >= 0)
            assertEquals("$style: the part after the Montée (${p.allScenes[d2].label})", d2, q.song.getOrNull(at + 1)?.scene)
            seen++
            done += target.style.id
        }
        assertTrue("techno seen", "techno" in done)
        assertTrue("only $seen styles", seen >= 5)
    }
}
