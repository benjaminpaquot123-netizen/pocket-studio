package com.pocketstudio.model.guide

import com.pocketstudio.model.PadSample
import com.pocketstudio.model.Project
import com.pocketstudio.model.ProjectJson
import com.pocketstudio.model.StarterWizard
import com.pocketstudio.model.allScenes
import com.pocketstudio.model.switchScene
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * G5: one guidance surface. « Ton morceau est prêt » is the first card of a song just made, the
 * Missions are the first cards of « Mon premier morceau », the « Prochaine étape » of a song made by
 * hand its first cards; a song from before G5 shows none of them, and a user mid-mission lands on the
 * card of the step reached.
 */
class GuideStartTest {
    private fun house(): Project {
        val style = StarterWizard.style("house")!!
        return StarterWizard.build(style, style.lines.associate { it.role to null as PadSample? }, style.melody.mood,
            StarterWizard.defaultParts(style).associateWith { StarterWizard.soundsFor(it, style).first() })
    }

    private fun applicable(p: Project, simple: Boolean = false) = GuideAnalyzer.analyze(p, simple).cards.filter { it.applicable }.map { it.card.id }

    @Test
    fun aNewSongStartsOnItsReadyCardAndAnOldOneDoesNot() {
        val old = house()
        assertFalse("start.ready" in applicable(old))
        val made = GuideStart.begin(old, GuideStart.SONG, listOf(GuidePageText("Ton morceau House est prêt", listOf("Intro, Groove, Drop."))))
        val r = GuideAnalyzer.analyze(made)
        assertEquals("start.ready", r.next!!.card.id)
        assertEquals(GuideStages.START, r.next!!.card.stage)
        // Only the ready card: the Missions are for a first song.
        assertEquals(listOf("start.ready"), r.cards.filter { it.applicable && it.card.stage == GuideStages.START }.map { it.card.id })
        // The Mode simple has it too, first.
        assertEquals("start.ready", GuideAnalyzer.analyze(made, simple = true).next!!.card.id)
        // « Écouter » decides it (nothing in the song changes), the parcours goes on.
        val heard = GuideActions.apply(made, "start.ready", "drop", 5L)
        assertEquals(GuideStatus.DONE, heard.guide.statusOf("start.ready"))
        assertEquals(made.copy(guide = heard.guide), heard)
        assertTrue(GuideAnalyzer.analyze(heard).next!!.card.id != "start.ready")
        // Kept with the song, its guide too.
        val back = ProjectJson.decode(ProjectJson.encode(heard))
        assertEquals(GuideStart.SONG, back.guide.start)
        assertEquals("Intro, Groove, Drop.", back.guide.songGuide.single().lines.single())
    }

    @Test
    fun aFirstSongFollowsTheMissionsAsCards() {
        val p = GuideStart.begin(house(), GuideStart.FIRST)
        val ids = applicable(p, simple = true)
        assertEquals(listOf("start.ready", "start.sound", "start.melody", "start.arrange"), ids.filter { it.startsWith("start.") })
        assertTrue("share.ready" in ids && "share.remix" in ids)
        // The Remix closes the plan, after « Prêt à partager ? ».
        assertEquals("share.remix", ids.last())
        // A sound changed: « Change un son » ticks itself; notes added: « Enregistre une mélodie ».
        val lead = p.tracks.last()
        val sound = p.copy(tracks = p.tracks.map { if (it.id == lead.id) it.copy(synth = it.synth.with(0, (it.synth.get(0) + 0.3f) % 1f)) else it })
        val r = GuideAnalyzer.analyze(sound)
        assertEquals(GuideStatus.DONE_ELSEWHERE, r.status("start.sound")!!.status)
        assertEquals(GuideStatus.TODO, r.status("start.melody")!!.status)
    }

    /** G5 review: the part shown changes as the song plays (following it): « Enregistre une mélodie » is not ticked by that. */
    @Test
    fun showingAnotherPartDoesNotTickTheMelody() {
        val p = GuideStart.begin(house(), GuideStart.FIRST)
        assertTrue(p.allScenes.size >= 2)
        for (i in p.allScenes.indices) {
            val shown = p.switchScene(i)
            assertFalse("scene $i", GuideStart.changed(shown, GuideStart.NOTES))
            assertEquals("scene $i", GuideStatus.TODO, GuideAnalyzer.analyze(shown).status("start.melody")!!.status)
        }
    }

    /** G5 review: « Reprendre Mon premier morceau » only while its cards are not all settled. */
    @Test
    fun aFirstSongIsUnderWayUntilItsCardsAreSettled() {
        val p = GuideStart.begin(house(), GuideStart.FIRST)
        assertTrue(GuideStart.firstSongUnderWay(p) { false })
        assertFalse(GuideStart.firstSongUnderWay(p) { true })
        assertFalse(GuideStart.firstSongUnderWay(GuideStart.begin(house(), GuideStart.SONG)) { false })
    }

    @Test
    fun aUserMidMissionLandsOnTheStepReached() {
        // Style, Listen done, Sound skipped: on to « Enregistre une mélodie ».
        val p = GuideStart.fromMissions(house(), step = 3, skipped = setOf(2), at = 9L)
        assertEquals(GuideStart.FIRST, p.guide.start)
        assertEquals(GuideStatus.DONE, p.guide.statusOf("start.ready"))
        assertEquals(GuideStatus.SKIPPED, p.guide.statusOf("start.sound"))
        assertEquals("start.melody", GuideAnalyzer.analyze(p, simple = true).next!!.card.id)
    }

    @Test
    fun aSongMadeByHandGetsTheOldNextSteps() {
        val empty = Project()
        val r = GuideAnalyzer.analyze(empty, simple = true)
        assertEquals("start.beat", r.next!!.card.id)
        assertEquals(listOf("start.beat", "start.bassline", "start.tune", "start.parts", "start.song"),
            r.cards.filter { it.applicable && it.card.stage == GuideStages.START }.map { it.card.id })
        val beat = empty.copy(drums = empty.drums.toggle(0, 0))
        assertEquals("start.bassline", GuideAnalyzer.analyze(beat, simple = true).next!!.card.id)
        // A first song started on an empty project: a style first.
        val first = GuideStart.begin(empty, GuideStart.FIRST)
        assertEquals("start.style", GuideAnalyzer.analyze(first, simple = true).next!!.card.id)
        // Every common card is found by its id from any song.
        listOf("start.ready", "start.beat", "share.remix").forEach { assertTrue(it, GuideActions.cardOf(empty, it) != null) }
    }

    @Test
    fun theCommonCardsAreInEveryParcoursOnce() {
        for (s in StarterWizard.STYLES.take(5)) {
            val p = GuideLibrary.forProject(Project(style = s.id))
            assertEquals(p.cards.size, p.cards.map { it.id }.toSet().size)
            assertEquals(p.cards.map { it.stage }.sorted(), p.cards.map { it.stage })
            assertTrue(p.card("start.ready") != null && p.card("share.remix") != null)
        }
    }
}
