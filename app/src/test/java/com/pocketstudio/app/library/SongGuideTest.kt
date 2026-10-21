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

    private val pop = StarterWizard.style("pop")!!

    private fun popUi(mood: StarterWizard.Mood?, parts: Set<StarterWizard.Part>): WizardUi {
        val rows = pop.lines.associate { l ->
            val cats = soundCategories(l.role, pop.id, emptyList())
            l.role to WizardRow(true, cats.first().id, cats.first().options.first())
        }
        val partRows = StarterWizard.Part.entries.associateWith { PartRow(it in parts, StarterWizard.soundsFor(it, pop).first()) }
        return WizardUi(true, 0, pop.id, rows, mood, partRows)
    }

    @Test
    fun theAssistantSaysWhichSectionsEachLineAndPartPlaysIn() {
        // A song (pop): Intro, Couplet, Refrain, cumulative.
        val plan = popUi(StarterWizard.Mood.Happy, setOf(StarterWizard.Part.Bass, StarterWizard.Part.Pad)).plan
        assertEquals(3, plan.count)
        pop.lines.forEach {
            val expected = when (it.role) {
                StarterWizard.Role.Kick, StarterWizard.Role.Hat -> setOf(0, 1, 2)
                StarterWizard.Role.OpenHat, StarterWizard.Role.Perc -> setOf(2)
                else -> setOf(1, 2)
            }
            assertEquals(it.role.title, expected, plan.lines[it.role])
        }
        // Without a lead the chords make the Refrain: they do not open the Intro to vanish in the Couplet.
        assertEquals(mapOf(StarterWizard.Part.Bass to setOf(1, 2), StarterWizard.Part.Pad to setOf(2)), plan.parts)
        // Club music (house): Intro, Groove, Drop, and a Break with the chords, without kick nor bass.
        val club = ui(StarterWizard.Mood.Happy, setOf(StarterWizard.Part.Bass, StarterWizard.Part.Pad)).plan
        assertEquals(listOf("Intro", "Groove", "Drop", "Break"), (0 until club.count).map { club.title(it) })
        assertEquals(setOf(0, 1, 2), club.lines[StarterWizard.Role.Kick])
        assertEquals(mapOf(StarterWizard.Part.Bass to setOf(1, 2), StarterWizard.Part.Pad to setOf(2, 3)), club.parts)
        // A line switched off goes nowhere, and a part without a mood neither.
        val off = ui(null, setOf(StarterWizard.Part.Pad)).let { it.copy(rows = it.rows + (StarterWizard.Role.Kick to it.rows.getValue(StarterWizard.Role.Kick).copy(enabled = false))) }
        assertEquals(null, off.plan.lines[StarterWizard.Role.Kick])
        assertTrue(off.plan.parts.isEmpty())
        // The 303 left out on its step: the plan has none.
        val acidHouse = StarterWizard.style("acid_house")!!
        val acid = WizardUi(true, 0, acidHouse.id, mapOf(StarterWizard.Role.Kick to off.rows.getValue(StarterWizard.Role.Hat).copy(enabled = true)), StarterWizard.Mood.Dark, emptyMap())
        assertTrue(acid.plan.acid.isNotEmpty())
        assertTrue(acid.copy(acid = false).plan.acid.isEmpty())
    }

    @Test
    fun theGuideNamesTheSectionsAndHowToPlayTheSong() {
        val plan = popUi(StarterWizard.Mood.Happy, setOf(StarterWizard.Part.Bass, StarterWizard.Part.Pad, StarterWizard.Part.Lead)).plan
        val pages = songGuide("Pop", plan, inKey = true, song = songSummary(pop, plan))
        val first = pages.first()
        assertEquals("Ton morceau Pop est prêt", first.title)
        val intro = first.lines.single { it.startsWith("A · Intro") }
        assertTrue(intro, intro.contains("Kick") && intro.contains("Nappe") && !intro.contains("Basse") && !intro.contains("Lead"))
        assertTrue(first.lines.single { it.startsWith("B · Couplet") }.let { it.contains("Basse") && !it.contains("Lead") })
        assertTrue(first.lines.single { it.startsWith("C · Refrain") }.let { it.contains("Basse") && it.contains("Lead") && it.contains("Kick") })
        val all = pages.flatMap { it.lines }.joinToString("\n")
        // Two short pages: what the song holds, then how to hear it (all of it, or one section in a loop).
        assertEquals(2, pages.size)
        assertTrue(all.contains("tout le morceau") && all.contains("« Écouter » à côté de Jouer") && all.contains("« cette section »"))
        assertTrue(all.contains("Intro → Couplet → Refrain → Couplet → Refrain → Intro pour finir : environ "))
        // Nothing to save by hand.
        assertTrue(all.contains("Pas besoin de sauvegarder"))
        // The sections can be renamed, and copied for a bridge or an ending.
        assertTrue(all.contains("la renommer") && all.contains("Pont ou une Fin"))
        assertTrue(all.contains("aucune fausse note possible"))
        // The next thing to try: a voice on top, with headphones; no 303 here.
        assertTrue(all.contains("Audio : enregistre ta voix") && all.contains("écouteurs"))
        assertTrue(!all.contains("ligne 303"))
        // Club music: four sections with the Break, and the 303 when the style has one.
        val club = ui(StarterWizard.Mood.Happy, setOf(StarterWizard.Part.Bass, StarterWizard.Part.Pad)).plan
        val clubPages = songGuide("House", club, inKey = true, song = songSummary(house, club), acid = true)
        assertTrue(clubPages.first().lines.any { it.startsWith("D · Break") })
        assertTrue(clubPages.first().lines.first().contains("quatre sections"))
        // The 303 is named only when the plan plays it, with the sections it plays in.
        assertTrue(clubPages.flatMap { it.lines }.none { it.contains("ligne 303") })
        val withAcid = StarterWizard.planFor(house, house.lines.map { it.role }.toSet(), setOf(StarterWizard.Part.Bass, StarterWizard.Part.Pad), true)
        assertTrue(songGuide("House", withAcid, inKey = true, acid = true).flatMap { it.lines }.any { it.startsWith("Acide : ta ligne 303 joue dans Groove et Drop.") })
        assertTrue(songSummary(house, club).startsWith("Intro → Groove → Drop → Break → Drop → Intro pour finir"))
    }

    @Test
    fun theSongSummarySaysHowLongItLasts() {
        assertEquals("2 min 40", duration(160.0))
        assertEquals("2 min", duration(118.0))
        assertEquals("1 min 50", duration(113.0))
        assertEquals("50 s", duration(47.0))
        // Pop at 116 BPM, chords on four bars: 8 + 4 × 16 + 8 = 80 bars, about 2 min 45.
        val plan = popUi(StarterWizard.Mood.Happy, setOf(StarterWizard.Part.Bass, StarterWizard.Part.Pad)).plan
        val bars = StarterWizard.songOf(pop, plan).sumOf { it.repeats } * StarterWizard.loopBars(pop, plan)
        assertEquals(80, bars)
        assertTrue(songSummary(pop, plan).endsWith("environ ${duration(80 * 4 * 60 / pop.bpm)}."))
        // House at 122 BPM: Intro 16, Groove 16, Drop 16, Break 8, Drop 16, Intro 16 = 88 bars.
        val club = ui(StarterWizard.Mood.Happy, setOf(StarterWizard.Part.Bass, StarterWizard.Part.Pad)).plan
        assertEquals(88, StarterWizard.songOf(house, club).sumOf { it.repeats } * StarterWizard.loopBars(house, club))
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
