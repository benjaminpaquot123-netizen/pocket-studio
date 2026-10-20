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
        house.lines.forEach { assertEquals(0, plan.lines[it.role]) }
        assertEquals(mapOf(StarterWizard.Part.Bass to 0, StarterWizard.Part.Pad to 1), plan.parts)
        // A line switched off goes nowhere, and a part without a mood neither.
        val off = ui(null, setOf(StarterWizard.Part.Pad)).let { it.copy(rows = it.rows + (StarterWizard.Role.Kick to it.rows.getValue(StarterWizard.Role.Kick).copy(enabled = false))) }
        assertEquals(null, off.plan.lines[StarterWizard.Role.Kick])
        assertTrue(off.plan.parts.isEmpty())
    }

    @Test
    fun theGuideNamesBothScenesAndHowToPlayThem() {
        val plan = ui(StarterWizard.Mood.Happy, setOf(StarterWizard.Part.Bass, StarterWizard.Part.Pad, StarterWizard.Part.Lead)).plan
        val pages = songGuide("House", plan)
        val first = pages.first()
        assertEquals("Ton morceau House est prêt", first.title)
        assertTrue(first.lines.any { it.startsWith("Scène A · le rythme : Kick") && it.contains("Basse") })
        assertTrue(first.lines.any { it == "Scène B · la mélodie : Nappe, Lead." })
        val all = pages.flatMap { it.lines }.joinToString("\n")
        assertTrue(all.contains("Jouer le morceau"))
        assertTrue(all.contains("Morceau › Scènes"))
    }
}
