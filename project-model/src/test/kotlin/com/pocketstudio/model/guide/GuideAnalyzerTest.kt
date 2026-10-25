package com.pocketstudio.model.guide

import com.pocketstudio.model.InstrumentTrack
import com.pocketstudio.model.Mixer
import com.pocketstudio.model.PadSample
import com.pocketstudio.model.Project
import com.pocketstudio.model.ProjectJson
import com.pocketstudio.model.StarterWizard
import com.pocketstudio.model.StarterWizard.Mood
import com.pocketstudio.model.instrumentTracks
import com.pocketstudio.model.withBeatsPerBar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The Guide's brain (docs/PLAN-GUIDE.md §4.2, §4.5): what it reads in the assistant's songs and in Benjamin's. */
class GuideAnalyzerTest {
    private fun build(id: String, mood: Mood? = null): Project {
        val style = StarterWizard.style(id)!!
        return StarterWizard.build(style, style.lines.associate { it.role to null as PadSample? }, mood ?: style.melody.mood,
            StarterWizard.defaultParts(style).associateWith { StarterWizard.soundsFor(it, style).first() })
    }

    private val benjamin: Project by lazy {
        ProjectJson.decode(GuideAnalyzerTest::class.java.classLoader.getResourceAsStream("guide/benjamin-jumpup.json")!!.bufferedReader().readText())
    }

    /**
     * Every style in every mood, untouched as the assistant makes it: about half done since G0 (the
     * frame, the sounds and the layers, most of the arrangement and the movement; its mix choices stay
     * to hear), 12 to 20 cards to decide,
     * no alert, and the first card « voilà ce que j'ai mis » of stage 3. The same song twice: the same report.
     */
    @Test
    fun anUntouchedAssistantSongIsAboutFortyPercentDoneAndStartsWithWhatTheAssistantPut() {
        val percents = ArrayList<Int>()
        for (style in StarterWizard.STYLES) for (mood in Mood.entries) {
            val p = build(style.id, mood)
            val r = GuideAnalyzer.analyze(p)
            val what = "${style.id}/${mood.id}"
            assertEquals(what, style.id, r.parcours.style)
            assertTrue("$what: alerts ${r.alerts.map { it.card.id }} ${r.facts.filter { it.severity == GuideSeverity.ALERT }.map { it.text }}", r.alerts.isEmpty())
            assertEquals("$what: ${r.cards.filter { it.applicable && !it.settled }.map { it.card.id }}", "fx.review", r.next?.card?.id)
            assertTrue("$what: ${r.percent} %", r.percent in 35..60)
            assertTrue("$what: ${r.toDecide} to decide", r.toDecide in 12..20)
            // Stages 0 to 2 are done by the assistant; nothing of stage 7 is.
            assertTrue(what, r.cards.filter { it.applicable && it.card.stage <= 2 }.all { it.status == GuideStatus.DONE_ELSEWHERE })
            assertTrue(what, r.cards.filter { it.card.stage == 7 }.none { it.settled })
            percents += r.percent
            if (mood == Mood.Dark) assertEquals(what, r, GuideAnalyzer.analyze(p))
        }
        val mean = percents.average()
        // About half since G0 (its Drops differ, the chords leave room, every track has its low cut and its place):
        // what the assistant now does well is done; its mix choices stay « à écouter ».
        assertTrue("mean $mean", mean in 40.0..55.0)
    }

    @Test
    fun theModeSimpleShowsItsStagesOnlyInEverydayWords() {
        val p = build("jump_up")
        val r = GuideAnalyzer.analyze(p, simple = true)
        assertTrue(r.cards.all { it.card.stage in GuideStages.SIMPLE })
        // Its stage 1 is the assistant's: the first card to decide is in the arrangement.
        assertEquals(4, r.next!!.card.stage)
        assertEquals("Le kick (le « boum »)", r.cards.first().card.titleFor(true))
    }

    /** docs/guide/diagnostic-jump-up.md §1.4: what the analysis must see in Benjamin's song. */
    @Test
    fun benjaminsSongShowsWhatTheDiagnosisFound() {
        val r = GuideAnalyzer.analyze(benjamin)
        fun fact(id: String) = assertNotNull("no $id in ${r.facts.map { it.id }}", r.fact(id)).let { r.fact(id)!! }
        val names = { f: GuideFact -> f.tracks.mapNotNull { k -> r.sheets.firstOrNull { it.track.key == k }?.track?.name } }
        // The pad plays through every Drop; the three Drops play the same notes (the bass changes sound, the lead waits in the first).
        assertEquals(listOf("Nappe"), names(fact("pad.everywhere")))
        val drops = fact("peaks.identical")
        assertEquals(listOf(3f, 3f), drops.values)
        assertTrue("mix" in drops.words)
        // Nothing is panned, every fader at 0 dB.
        fact("mix.no_pans")
        fact("mix.faders_flat")
        // The Sub, the Voix and the Sub drop have no effect.
        assertEquals(setOf("Sub", "Voix", "Sub drop"), names(fact("track.no_inserts")).toSet())
        // The bass loops the same bars through each Drop.
        assertTrue(fact("bass.repeats").values[1] >= 4f)
        // No low cut on the pad, the lead, the voice.
        assertTrue(names(fact("track.no_lowcut")).containsAll(listOf("Nappe", "Lead", "Voix")))
        // The kick's pad sits hard left (G0's lane shift): an alert, first in line.
        assertEquals("alert.low.off_center", r.next!!.card.id)
        assertEquals(GuideRoles.KICK, r.next!!.card.target)
        // Its melody and chords are read.
        fact("melody.lead")
        fact("chords.guess")
        // The summary: three lines, the header with its name, length and tracks.
        assertEquals(3, r.summary.size)
        assertTrue(r.header, r.header.startsWith("${benjamin.name} · "))
    }

    @Test
    fun aSongMadeByHandFollowsTheGenericParcours() {
        val p = Project(name = "À la main").let { it.copy(drums = it.drums.toggle(0, 0).toggle(0, 8).toggle(2, 4)) }
        val r = GuideAnalyzer.analyze(p)
        assertEquals(GuideParcours.GENERIC, r.parcours.style)
        // No style: the frame is to decide; the cards about tracks not there do not apply.
        assertFalse(r.status("frame.style")!!.settled)
        assertFalse(r.status("sound.bass")!!.applicable)
        assertTrue(r.status("sound.kick")!!.settled)
        assertNull(r.fact("peaks.identical"))
        assertTrue(r.header.contains("À la main"))
    }

    @Test
    fun decisionsTickTheCardsAndThePlanMovesOn() {
        var p = GuideActions.withBaseline(build("jump_up"))
        val first = GuideAnalyzer.analyze(p)
        p = GuideActions.apply(p, "fx.review", "keep", 1000L)
        val second = GuideAnalyzer.analyze(p)
        assertEquals(GuideStatus.DONE, second.status("fx.review")!!.status)
        assertEquals(first.done + 1, second.done)
        assertEquals("fx.drums", second.next!!.card.id)
        // « Plus tard » sends it to the end; « Sauter » settles it.
        p = GuideActions.later(p, "fx.drums", 2000L)
        assertEquals("fx.bass", GuideAnalyzer.analyze(p).next!!.card.id)
        p = GuideActions.skip(p, "fx.bass", 3000L)
        assertEquals(GuideStatus.SKIPPED, GuideAnalyzer.analyze(p).status("fx.bass")!!.status)
        // A chain changed in the mixer is decided elsewhere.
        val ch = InstrumentTrack.mixerChannel(p.instrumentTracksNamed("Lead"))
        p = p.copy(mixer = p.mixer.copy(channels = p.mixer.channels.mapIndexed { i, c -> if (i == ch) c.copy(sends = listOf(0.5f, 0f)) else c }))
        assertEquals(GuideStatus.DONE_ELSEWHERE, GuideAnalyzer.analyze(p).status("fx.lead")!!.status)
        // The summary says where it was.
        val r = GuideAnalyzer.analyze(p, now = 3000L + 3 * GuideAnalyzer.DAY_MS)
        assertTrue(r.summary[0], r.summary[0].startsWith("Te revoilà"))
        assertTrue(r.summary[1], r.summary[1].contains("le cadre"))
    }

    @Test
    fun theSheetsHaveTheirFiveColumns() {
        val p = build("jump_up")
        val r = GuideAnalyzer.analyze(p)
        val bass = r.sheets.first { it.track.name == "Basse" }
        assertEquals(GuideRoles.BASS, bass.track.role)
        assertTrue(bass.sound.label.isNotEmpty())
        assertTrue(bass.plays.any { it.kind == GuideSection.Kind.PEAK && it.playedBars > 0 } && bass.plays.none { it.kind == GuideSection.Kind.BUILD && it.playedBars > 0 })
        assertEquals(p.song.size, bass.parts.size)
        assertTrue(bass.chain.isNotEmpty() && bass.chain.all { it.why.isNotEmpty() && it.value.isNotEmpty() })
        assertTrue(bass.often.onceForSong)
        assertTrue(bass.often.lanes.isNotEmpty())
        assertTrue(bass.elsewhere.any { it.action.type == GuideAction.COPY_CHAIN })
        assertNotNull(bass.advice)
        // Every track of the assistant has its sheet, every effect a why.
        for (s in r.sheets) assertTrue(s.track.key, s.chain.all { it.why.isNotEmpty() })
        assertTrue(r.sheets.map { it.track.role }.containsAll(listOf(GuideRoles.KICK, GuideRoles.SNARE, GuideRoles.DRUMS, GuideRoles.SUB, GuideRoles.PAD, GuideRoles.LEAD, GuideRoles.VOCAL)))
        assertEquals(Mixer.padChannel(0), r.sheets.first().track.channel)  // the kick first
    }

    private fun Project.instrumentTracksNamed(name: String): Int = instrumentTracks.first { it.name == name }.id

    /** Review G1: a signature is the same in every run of the app (no enum's identity hash in it). */
    @Test
    fun theSignaturesAreTheSameInEveryRun() {
        val p = Project()
        assertEquals(31 * (31 * p.mixer.master.hashCode() + p.mixer.target.ordinal) + p.mixer.masterDb.hashCode(), GuideSignatures.master(p))
        assertEquals(623212737, GuideSignatures.master(p))
    }

    /** Review M10: an empty project, a song in 3/4, a style the app does not know. */
    @Test
    fun theEdgesOfAProject() {
        val empty = GuideAnalyzer.analyze(Project())
        assertEquals(GuideParcours.GENERIC, empty.parcours.style)
        assertTrue(empty.alerts.isEmpty() && empty.summary.size == 3)
        assertTrue(empty.sheets.all { it.track.key.startsWith("inst:") })
        assertTrue(empty.header, empty.header.contains("0:02"))  // one bar of 4/4 at 120 BPM
        val waltz = build("house").withBeatsPerBar(3)
        val r = GuideAnalyzer.analyze(waltz)
        assertTrue(GuideSong(waltz).sections.isNotEmpty())
        assertTrue(r.next != null)
        val unknown = GuideAnalyzer.analyze(build("techno").copy(style = "polka"))
        assertEquals(GuideParcours.GENERIC, unknown.parcours.style)
        assertEquals(GuideParcours.GENERIC, GuideLibrary.forProject(Project(style = GuideLibrary.ALERTS)).style)
    }
}
