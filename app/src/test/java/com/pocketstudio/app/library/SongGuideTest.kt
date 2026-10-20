package com.pocketstudio.app.library

import com.pocketstudio.model.StarterWizard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SongGuideTest {
    private val house = StarterWizard.style("house")!!

    private fun ui(mood: StarterWizard.Mood?, parts: Set<StarterWizard.Part>): WizardUi {
        val rows = house.lines.associate { l ->
            val cats = soundCategories(l.role, house.id, emptyList())
            l.role to WizardRow(true, cats.first().id, cats.first().options.first())
        }
        val partRows = StarterWizard.Part.entries.associateWith { PartRow(it in parts, StarterWizard.soundsFor(it, house).first()) }
        return WizardUi(true, 0, house.id, rows, mood, partRows)
    }

    @Test
    fun theAssistantSaysWhereEachLineAndPartGoes() {
        val plan = ui(StarterWizard.Mood.Happy, setOf(StarterWizard.Part.Bass, StarterWizard.Part.Pad)).plan
        house.lines.forEach { assertEquals(if (it.role == StarterWizard.Role.Bass808) 1 else 0, plan.lines[it.role]) }
        assertEquals(mapOf(StarterWizard.Part.Bass to 1, StarterWizard.Part.Pad to 2), plan.parts)
        // A line switched off goes nowhere, and a part without a mood neither.
        val off = ui(null, setOf(StarterWizard.Part.Pad)).let { it.copy(rows = it.rows + (StarterWizard.Role.Kick to it.rows.getValue(StarterWizard.Role.Kick).copy(enabled = false))) }
        assertEquals(null, off.plan.lines[StarterWizard.Role.Kick])
        assertTrue(off.plan.parts.isEmpty())
    }

    @Test
    fun theGuideNamesBothScenesAndHowToPlayThem() {
        val plan = ui(StarterWizard.Mood.Happy, setOf(StarterWizard.Part.Bass, StarterWizard.Part.Pad, StarterWizard.Part.Lead)).plan
        val pages = songGuide("House", plan, inKey = true)
        val first = pages.first()
        assertEquals("Ton morceau House est prêt", first.title)
        assertTrue(first.lines.any { it.startsWith("Scène A · le rythme : Kick") && !it.contains("Basse") })
        assertTrue(first.lines.any { it.startsWith("Scène B · la basse :") && it.contains("Basse") })
        assertTrue(first.lines.any { it == "Scène C · la mélodie : Nappe, Lead." })
        val all = pages.flatMap { it.lines }.joinToString("\n")
        assertTrue(all.contains("Jouer le morceau"))
        assertTrue(all.contains("Morceau › Scènes"))
        // The assistant makes A, B and C: a copy lands in D.
        assertTrue(all.contains("une scène D") && all.contains("« + D »"))
        assertTrue(!all.contains("scène C pour une variante"))
        assertTrue(all.contains("aucune fausse note possible"))
    }

    @Test
    fun withoutAMoodTheGuidePromisesNoKey() {
        val w = ui(null, emptySet())
        val all = songGuide("House", w.plan, inKey = w.mood != null).flatMap { it.lines }.joinToString("\n")
        assertTrue(!all.contains("fausse note"))
        assertTrue(all.contains("Synthé : joue au clavier"))
    }

    @Test
    fun withoutAMoodTheMelodyStepsAreSkipped() {
        val melodic = ui(StarterWizard.Mood.Happy, StarterWizard.Part.entries.toSet())
        val beat = melodic.copy(mood = null)
        assertEquals(melodic.steps.size - StarterWizard.Part.entries.size, beat.steps.size)
        assertTrue(beat.steps.none { it is StarterWizard.Step.Melodic })
        // The mood is chosen on step 2: the steps before the melody stay where they were.
        val drumStep = melodic.copy(step = 2)
        assertEquals(drumStep.current, drumStep.copy(mood = null).current)
        assertEquals(StarterWizard.Step.Summary, beat.copy(step = beat.lastStep).current)
    }

    @Test
    fun aPlanWithEverythingOffDoesNotCrash() {
        val w = ui(null, emptySet())
        val off = w.copy(rows = w.rows.mapValues { it.value.copy(enabled = false) })
        assertTrue(off.plan.copies.isEmpty())
        assertTrue(songGuide("House", off.plan, inKey = false).isNotEmpty())
    }
}
