package com.pocketstudio.model.tuto

import com.pocketstudio.model.Hit
import com.pocketstudio.model.Project
import com.pocketstudio.model.StarterWizard
import com.pocketstudio.model.allScenes
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TutoCourseTest {
    private val course = TutoCourse.of("jump_up")!!
    private val target = TutoTarget.of("jump_up")!!

    private val known = Regex("""^(listen|manual|tempo|key|lane:(kick|snare|clap|hat|perc)|velocity:hat|notes:(bass|pad|lead)|track:sub|insert:[A-Z_]+|send:[01]|automation|master|changed:(volumes|kit|synth:\w+)|label:\w+|part:[^:]+:(less|nokick|full|differs)|song:(order|phrases)|fill|crash|gap)$""")

    @Test
    fun theCourseHasItsChaptersAndStepsInBothLevels() {
        val facile = course.chaptersFor(TutoLevel.Facile)
        val pro = course.chaptersFor(TutoLevel.Pro)
        assertTrue("Facile: ${facile.size} chapters", facile.size >= 12)
        assertTrue("Pro: ${pro.size} chapters", pro.size > facile.size)
        assertTrue(facile.sumOf { it.steps.size } >= 30)
        assertTrue(pro.sumOf { it.steps.size } >= 50)
        // The structure block is in both, with its map.
        assertTrue(facile.any { it.map } && pro.any { it.map })
        val ids = course.chapters.flatMap { c -> c.steps.map { it.id } }
        assertEquals("step ids are unique", ids.size, ids.toSet().size)
        course.chapters.flatMap { it.steps }.forEach { s ->
            assertTrue("${s.id}: unknown check ${s.check}", known.matches(s.check))
            if (s.listen.isNotEmpty()) assertNotNull("${s.id}: no example for ${s.listen}", TutoChecks.example(s, target))
            // A step to tick by its example has one.
            if (s.listening) assertTrue(s.id, s.listen.isNotEmpty())
        }
    }

    /** From an empty project, « Montre-moi » on each step in turn ticks it: the whole jump up track is made. */
    @Test
    fun showMeTicksEveryStepFromAnEmptyProject() {
        val empty = Project(name = "").sanitized()
        val steps = course.chaptersFor(TutoLevel.Pro).flatMap { it.steps }
        steps.forEach { assertFalse("${it.id} is done in an empty project", TutoChecks.done(it, empty, target, TutoChecks.signature(it.check.substringAfter(':'), empty))) }
        var p = empty
        for (step in steps) {
            if (step.manual || step.listening || step.check.startsWith("changed:")) continue
            // A step an earlier Montre-moi already did (the hats' strengths come with the hats) is ticked as it is.
            if (TutoChecks.done(step, p, target)) continue
            p = TutoChecks.show(step, p, target) ?: error("${step.id}: no Montre-moi")
            assertTrue("${step.id} is not done after Montre-moi", TutoChecks.done(step, p, target))
        }
        // The track: seven parts, in order, in phrases of 8 bars.
        assertEquals(7, p.song.size)
        assertEquals(listOf("Intro", "Montée", "Drop", "Break", "Montée", "Drop 2", "Fin"), p.song.map { p.allScenes[it.scene].label })
    }

    @Test
    fun aKickCloseEnoughPassesAndAWrongOneDoesNot() {
        val step = course.chapters.flatMap { it.steps }.first { it.id == "kick_do" }
        fun with(vararg at: Int) = Project(name = "").let { p ->
            p.copy(drums = p.drums.copy(lanes = p.drums.lanes.mapIndexed { i, l -> if (i == 0) l.copy(hits = at.associate { it to Hit(1f) }) else l }))
        }
        assertTrue(TutoChecks.done(step, with(0, 10), target))
        // One extra hit is tolerated; half the hits wrong are not.
        assertTrue(TutoChecks.done(step, with(0, 10, 14), target))
        assertFalse(TutoChecks.done(step, with(0), target))
        assertFalse(TutoChecks.done(step, with(2, 6, 12), target))
        assertFalse(TutoChecks.done(step, with(), target))
        // The dotted cells are the recipe's.
        assertEquals(0 to setOf(0, 10, 16, 26), TutoChecks.hint(step, target, 32))
    }

    @Test
    fun aChangedStepWaitsForAChangeSinceItBegan() {
        val step = TutoStep("v", "", check = "changed:volumes")
        val p = Project(name = "")
        val base = TutoChecks.signature("volumes", p)
        assertFalse(TutoChecks.done(step, p, target, base))
        val louder = p.copy(mixer = p.mixer.copy(channels = p.mixer.channels.mapIndexed { i, c -> if (i == 2) c.copy(volume = 0.5f) else c }))
        assertTrue(TutoChecks.done(step, louder, target, base))
    }

    @Test
    fun examplesAreMadeFromTheTargetOnly() {
        val drums = TutoChecks.example(TutoStep("k", "", listen = "drums:kick"), target)!!
        assertNull(drums.before)
        assertEquals(174.0, drums.after.tempoBpm, 0.0)
        assertEquals(StarterWizard.hits("X.........X.....").keys, drums.after.drums.lanes[0].hits.keys)
        assertTrue(drums.after.drums.lanes.drop(1).all { it.hits.isEmpty() })
        val vs = TutoChecks.example(TutoStep("v", "", listen = "vs:Drop:Intro"), target)!!
        assertEquals("Drop", vs.before!!.allScenes[vs.before!!.scene].label)
        assertEquals("Intro", vs.after.allScenes[vs.after.scene].label)
        assertTrue(vs.after.song.isEmpty())
        val map = TutoChecks.map(target)
        assertEquals("Intro", map.first().label)
        assertEquals("Fin", map.last().label)
        assertTrue(map.first { it.label == "Drop" }.energy > map.first().energy)
        assertTrue(map.all { it.bars % 8 == 0 })
    }

    @Test
    fun progressIsSavedAndReadBack() {
        val p = TutoProgress("jump_up", TutoLevel.Pro, setOf("tempo", "key"), "kick_do")
        assertEquals(p, TutoProgress.decode(p.encode()))
        assertNull(TutoProgress.decode("{broken"))
        assertNull(TutoProgress.decode(null))
    }

    @Test
    fun everyTextHasItsTranslations() {
        val texts = course.chapters.flatMap { c -> listOf(c.title, c.why) + c.steps.flatMap { listOfNotNull(it.text, it.textPro, it.why) } }.filter { it.isNotEmpty() }
        val problems = (Lang.entries - Lang.FR).flatMap { lang ->
            val table = I18n.table(lang)
            texts.filter { table[it].isNullOrBlank() }.map { "${lang.code}: $it" }
        }
        assertTrue("${problems.size} missing:\n" + problems.take(40).joinToString("\n"), problems.isEmpty())
    }
}
