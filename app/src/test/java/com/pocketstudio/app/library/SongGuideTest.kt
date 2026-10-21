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
    fun theAssistantSaysWhichSectionsEachLineAndPartPlaysIn() {
        val plan = ui(StarterWizard.Mood.Happy, setOf(StarterWizard.Part.Bass, StarterWizard.Part.Pad)).plan
        // Cumulative: the kick from the Intro on, the other drums and the bass from the Couplet on.
        house.lines.forEach {
            val expected = when (it.role) {
                StarterWizard.Role.Kick, StarterWizard.Role.Hat -> setOf(0, 1, 2)
                else -> setOf(1, 2)
            }
            assertEquals(it.role.title, expected, plan.lines[it.role])
        }
        // Without a lead, the chords open the Intro and come back in the Refrain.
        assertEquals(mapOf(StarterWizard.Part.Bass to setOf(1, 2), StarterWizard.Part.Pad to setOf(0, 2)), plan.parts)
        // A line switched off goes nowhere, and a part without a mood neither.
        val off = ui(null, setOf(StarterWizard.Part.Pad)).let { it.copy(rows = it.rows + (StarterWizard.Role.Kick to it.rows.getValue(StarterWizard.Role.Kick).copy(enabled = false))) }
        assertEquals(null, off.plan.lines[StarterWizard.Role.Kick])
        assertTrue(off.plan.parts.isEmpty())
    }

    @Test
    fun theGuideNamesTheSectionsAndHowToPlayTheSong() {
        val plan = ui(StarterWizard.Mood.Happy, setOf(StarterWizard.Part.Bass, StarterWizard.Part.Pad, StarterWizard.Part.Lead)).plan
        val pages = songGuide("House", plan, inKey = true, song = songSummary(house, plan))
        val first = pages.first()
        assertEquals("Ton morceau House est prêt", first.title)
        val intro = first.lines.single { it.startsWith("A · Intro") }
        assertTrue(intro, intro.contains("Kick") && intro.contains("Nappe") && !intro.contains("Basse") && !intro.contains("Lead"))
        assertTrue(first.lines.single { it.startsWith("B · Couplet") }.let { it.contains("Basse") && !it.contains("Lead") })
        assertTrue(first.lines.single { it.startsWith("C · Refrain") }.let { it.contains("Basse") && it.contains("Lead") && it.contains("Kick") })
        val all = pages.flatMap { it.lines }.joinToString("\n")
        assertTrue(all.contains("Jouer le morceau"))
        assertTrue(all.contains("Intro → Couplet → Refrain → Couplet → Refrain → Intro pour finir : environ "))
        assertTrue(all.contains("Morceau › Scènes"))
        // The sections can be renamed, and copied for a bridge or an ending.
        assertTrue(all.contains("se changent dans l'onglet Morceau") && all.contains("Pont ou une Fin"))
        assertTrue(all.contains("aucune fausse note possible"))
    }

    @Test
    fun theSongSummarySaysHowLongItLasts() {
        assertEquals("2 min 40", duration(160.0))
        assertEquals("2 min", duration(118.0))
        assertEquals("1 min 50", duration(113.0))
        assertEquals("50 s", duration(47.0))
        // House at 122 BPM, chords on four bars: 80 bars, about 2 min 40.
        val plan = ui(StarterWizard.Mood.Happy, setOf(StarterWizard.Part.Bass, StarterWizard.Part.Pad)).plan
        val bars = StarterWizard.songOf(house, plan).sumOf { it.repeats } * StarterWizard.loopBars(house, plan)
        assertEquals(80, bars)
        assertTrue(songSummary(house, plan).endsWith("environ ${duration(80 * 4 * 60 / house.bpm)}."))
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
        assertTrue(off.plan.isEmpty)
        assertTrue(songGuide("House", off.plan, inKey = false).isNotEmpty())
    }
}
