package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import com.pocketstudio.model.SongBars.isDrumBar
import kotlin.random.Random

/** The assistant's 303 editor: touches on the steps, the generator on the scale, the line reaching the song. */
class WizardAcidTest {
    private val acidHouse = StarterWizard.style("acid_house")!!
    private val mood = StarterWizard.Mood.Dark
    private val harmony = StarterWizard.harmonyOf(acidHouse, mood)  // A minor

    @Test
    fun aTouchGoesUpTheScaleThenRestsThenStartsOnTheRoot() {
        var step = AcidStep()
        val heard = ArrayList<Int?>()
        repeat(10) {
            step = WizardAcid.nextNote(step, harmony)
            heard += if (step.gate) step.note + 12 * step.octave else null
        }
        // Rest → root, the minor scale up to the octave, then a rest, and round again.
        assertEquals(listOf(0, 2, 3, 5, 7, 8, 10, 12, null, 0), heard)
        // A note off the scale (from the Acide tab) goes to the next one of the scale.
        assertEquals(2, WizardAcid.nextNote(AcidStep(note = 1, gate = true), harmony).note)
    }

    @Test
    fun aLongTouchCyclesAccentSlideBothAndNeither() {
        val plain = AcidStep(note = 3, gate = true)
        val a = WizardAcid.nextMarks(plain)
        val b = WizardAcid.nextMarks(a)
        val c = WizardAcid.nextMarks(b)
        val d = WizardAcid.nextMarks(c)
        assertEquals(listOf(true to false, false to true, true to true, false to false), listOf(a, b, c, d).map { it.accent to it.slide })
        listOf(a, b, c, d).forEach { assertEquals(3, it.note) }
        // A rest becomes an accented root.
        assertEquals(AcidStep(gate = true, accent = true), WizardAcid.nextMarks(AcidStep()))
    }

    @Test
    fun theGeneratorStaysOnTheSongsScaleAtTheLengthAndDensityAsked() {
        val line = WizardAcid.withBars(WizardAcid.recipe(acidHouse, mood)!!, 2)
        repeat(20) { seed ->
            val made = WizardAcid.generate(line.copy(density = 1f), harmony, Random(seed))
            assertEquals(32, made.length)
            val notes = made.steps.take(32).filter { it.gate }
            assertEquals("density 100 % fills every step", 32, notes.size)
            notes.forEach { s -> assertTrue("seed $seed: ${s.note}", harmony.inScale(harmony.root + s.note)) }
        }
        val sparse = WizardAcid.generate(line.copy(density = 0f), harmony, Random(1))
        assertTrue(sparse.steps.take(32).count { it.gate } <= 2)
    }

    @Test
    fun aSecondBarStartsAsACopyOfTheFirst() {
        val one = WizardAcid.recipe(acidHouse, mood)!!
        val two = WizardAcid.withBars(one, 2)
        assertEquals(32, two.length)
        assertEquals(one.steps.take(16), two.steps.subList(16, 32))
        assertEquals(16, WizardAcid.withBars(two, 1).length)
    }

    @Test
    fun theQuickSoundsAreTheLibrarys() {
        WizardAcid.SOUNDS.forEach { (label, _) -> assertNotNull(label, WizardAcid.sound(label)) }
        val keep = AcidSound(tuning = 0.6f, level = 0.5f)
        val sat = WizardAcid.sound("Saturée", keep)!!
        assertEquals(0.6f, sat.tuning)
        assertEquals(SoundLibrary.ACID.first { it.name == "Carré saturé" }.sound.level, sat.level)  // its calibrated level (phase 30)
        assertEquals(0.9f, sat.drive)
    }

    @Test
    fun theSongGetsExactlyTheLineAndSoundChosenInEveryScenePlayingTheAcid() {
        val mine = WizardAcid.withBars(WizardAcid.recipe(acidHouse, mood)!!, 2)
            .withStep(5, AcidStep(note = 7, octave = 1, accent = true, slide = true, gate = true))
            .copy(sound = WizardAcid.sound("Criarde")!!)
        val sounds = acidHouse.lines.associate { it.role to null as PadSample? }
        val project = StarterWizard.build(acidHouse, sounds, mood, emptyMap(), acid = true, chosenAcid = mine)
        val plan = StarterWizard.planFor(acidHouse, sounds.keys, emptySet(), true)
        assertEquals(setOf(1, 2, 3), plan.acid)
        project.allScenes.take(plan.count).forEachIndexed { i, scene ->
            assertEquals(32, scene.acid.length)
            if (i in plan.acid) assertEquals("scene $i", mine.steps, scene.acid.steps)
            else assertTrue("scene $i silent", scene.acid.steps.none { it.gate })
        }
        // The Montée's two halves play it written out over their 4 bars (its last bars cut, its last beat silent).
        for (scene in project.allScenes.drop(plan.count).filter { !it.isDrumBar() }) {
            val a = scene.acid
            assertTrue(scene.label, a.length == 32 || a.length == 64)
            a.steps.take(a.length).forEachIndexed { j, st -> if (st.gate) assertEquals(scene.label, mine.steps[j % 32], st) }
        }
        assertEquals(mine.sound, project.acid.sound)
        // Each scene's loop is the 303's two bars.
        assertEquals(32, project.allScenes[1].stepsFor(project.stepsPerBar))
    }

    @Test
    fun aLineWithoutANoteIsLeftOut() {
        val silent = WizardAcid.recipe(acidHouse, mood)!!.let { p -> p.copy(steps = p.steps.map { it.copy(gate = false) }) }
        assertFalse(WizardAcid.plays(silent))
        val sounds = acidHouse.lines.associate { it.role to null as PadSample? }
        val project = StarterWizard.build(acidHouse, sounds, mood, emptyMap(), acid = true, chosenAcid = silent)
        assertTrue(project.allScenes.all { s -> s.acid.steps.none { it.gate } })
        // Without a chosen line, the style's own as before.
        val recipe = StarterWizard.build(acidHouse, sounds, mood, emptyMap())
        assertEquals(WizardAcid.recipe(acidHouse, mood)!!.steps, recipe.allScenes[1].acid.steps)
    }

    @Test
    fun aOneBarLineStaysOneBarWithATwoBarFeel() {
        val techno = StarterWizard.style("acid_techno")!!
        val h = StarterWizard.harmonyOf(techno, mood)
        val one = WizardAcid.withBars(WizardAcid.recipe(techno, mood)!!, 1)
        repeat(10) { seed ->
            assertEquals(16, WizardAcid.generate(one, h, Random(seed), AcidGenerator.Feel.ACID_TECHNO).length)
        }
        // Two bars chosen: the two-bar phrase as before.
        assertEquals(32, WizardAcid.generate(WizardAcid.withBars(one, 2), h, Random(1), AcidGenerator.Feel.ACID_TECHNO).length)
    }

    @Test
    fun aWrittenSecondBarComesBackAfterTwoOneTwo() {
        val two = WizardAcid.withBars(WizardAcid.recipe(acidHouse, mood)!!, 2)
            .withStep(20, AcidStep(note = 7, gate = true, accent = true))
        val back = WizardAcid.withBars(WizardAcid.withBars(two, 1), 2)
        assertEquals(two.steps.take(32), back.steps.take(32))
        // An empty second bar still starts as a copy of the first.
        val oneBar = WizardAcid.recipe(acidHouse, mood)!!.let { it.copy(length = 16, steps = it.steps.take(16) + List(AcidPattern.MAX_STEPS - 16) { AcidStep() }) }
        val fresh = WizardAcid.withBars(oneBar, 2)
        assertEquals(fresh.steps.take(16), fresh.steps.subList(16, 32))
    }

    @Test
    fun aLineMovedToAnotherScaleKeepsItsMarksAndOctaves() {
        val major = harmony.copy(scale = Scale.Major)
        // The minor third (3) is not in A major: it goes to the nearest note of it (ties down: 2).
        val line = AcidPattern(length = 16)
            .withStep(0, AcidStep(note = 3, octave = 1, accent = true, slide = true, gate = true))
            .withStep(1, AcidStep(note = 10, gate = true))
            .withStep(2, AcidStep(note = 3, gate = false))
        val moved = WizardAcid.inScale(line, major)
        assertEquals(AcidStep(note = 2, octave = 1, accent = true, slide = true, gate = true), moved.steps[0])
        assertTrue(major.inScale(major.root + moved.steps[1].note))
        // A rest is left alone.
        assertEquals(line.steps[2], moved.steps[2])
        moved.steps.filter { it.gate }.forEach { assertTrue(major.inScale(major.root + it.note + 12 * it.octave)) }
    }
}
