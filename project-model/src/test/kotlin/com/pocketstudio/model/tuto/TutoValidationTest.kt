package com.pocketstudio.model.tuto

import com.pocketstudio.model.EffectType
import com.pocketstudio.model.FxPresets
import com.pocketstudio.model.Hit
import com.pocketstudio.model.InstrumentTrack
import com.pocketstudio.model.Project
import com.pocketstudio.model.StarterWizard
import com.pocketstudio.model.StudioDrums
import com.pocketstudio.model.SynthPatch
import com.pocketstudio.model.allScenes
import com.pocketstudio.model.switchScene
import com.pocketstudio.model.track
import com.pocketstudio.model.withTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The testers (2026-10-28, lot « tuto 3 »): a step is ticked only once it is really done, and every
 * step can be done with what its screen shows. Each case is what Léa (Facile) or Karim (Pro) did.
 */
class TutoValidationTest {
    private val course = TutoCourse.of("jump_up")!!
    private val target = TutoTarget.of("jump_up")!!
    private val steps = course.chapters.flatMap { it.steps }.associateBy { it.id }
    private fun step(id: String) = steps.getValue(id)

    private fun withHits(p: Project, lane: Int, at: Collection<Int>, velocity: (Int) -> Float = { 1f }) =
        p.copy(drums = p.drums.copy(lanes = p.drums.lanes.mapIndexed { i, l -> if (i == lane) l.copy(hits = at.associateWith { Hit(velocity(it)) }) else l }))

    private fun bars(p: Project, n: Int) = p.copy(drums = p.drums.withSteps(n * p.stepsPerBar))

    /** a/41, b/68: the hats ticked at 7 cells of 9; every dotted cell is needed, on both bars when the loop has two. */
    @Test
    fun theHatsNeedEveryDottedCell() {
        val hat = step("hat_do")
        val lane = TutoChecks.laneOf("hat")
        val one = target.blank()
        val cells = TutoChecks.hint(hat, target, 16)!!.second.sorted()
        assertEquals(9, cells.size)
        assertFalse("7 of 9", TutoChecks.done(hat, withHits(one, lane, cells.take(7)), target))
        assertTrue("9 of 9", TutoChecks.done(hat, withHits(one, lane, cells), target))
        // Two bars (« Passer à 2 mesures » at the snare): the dotted cells are 18, the first bar is not enough.
        val two = bars(one, 2)
        val all = TutoChecks.hint(hat, target, 32)!!.second
        assertEquals(18, all.size)
        assertFalse("first bar only", TutoChecks.done(hat, withHits(two, lane, cells), target))
        assertTrue(TutoChecks.done(hat, withHits(two, lane, all), target))
        // « Remplir 1/2 » then the last cell of each bar: done (the hint's way).
        assertTrue(TutoChecks.done(hat, withHits(two, lane, (0 until 32 step 2) + listOf(15, 31)), target))
    }

    /** a/30 (Facile ticked on one bar), b/44-55 (Pro never ticked): the same rule in both, the two bars needed. */
    @Test
    fun theSnareNeedsItsTwoBarsInBothLevels() {
        val snare = step("snare_do")
        val lane = TutoChecks.laneOf("snare")
        val one = target.blank()
        // Léa: the two dotted cells of the only bar, without « Passer à 2 mesures »: not done, the button still offered.
        val first = withHits(one, lane, listOf(4, 12))
        assertFalse(TutoChecks.done(snare, first, target))
        assertEquals(TutoNeed.Bars(2), TutoChecks.need(snare, first, target))
        // Karim: two bars, the first bar's cells only: not done; the second bar's too: done.
        val two = TutoChecks.fulfil(TutoNeed.Bars(2), snare, first, target)!!.first
        val dotted = TutoChecks.hint(snare, target, 32)!!.second
        assertTrue(dotted.any { it >= 16 })
        assertFalse(TutoChecks.done(snare, withHits(two, lane, listOf(4, 12)), target))
        assertTrue(TutoChecks.done(snare, withHits(two, lane, dotted), target))
        // The rim, its two bars too (b/72-81).
        val perc = step("perc_do")
        val rims = TutoChecks.hint(perc, target, 32)!!.second
        assertFalse(TutoChecks.done(perc, withHits(two, TutoChecks.laneOf("perc"), rims.filter { it < 16 }), target))
        assertTrue(TutoChecks.done(perc, withHits(two, TutoChecks.laneOf("perc"), rims), target))
    }

    /** b/87: one hat made « Doux » ticked the groove; the 4 on the beats softer than those between them. */
    @Test
    fun theHatsSwingNeedsTheFourBeats() {
        val vel = step("hat_vel")
        val lane = TutoChecks.laneOf("hat")
        val cells = TutoChecks.hint(step("hat_do"), target, 16)!!.second
        val p = target.blank()
        assertFalse("one soft hit", TutoChecks.done(vel, withHits(p, lane, cells) { if (it == 0) 0.2f else 1f }, target))
        assertFalse("three", TutoChecks.done(vel, withHits(p, lane, cells) { if (it in setOf(0, 4, 8)) 0.2f else 1f }, target))
        assertTrue("the four beats", TutoChecks.done(vel, withHits(p, lane, cells) { if (it % 4 == 0) 0.2f else 1f }, target))
    }

    /** a/54-69: the bass and the chords ticked with the screen's first notes; every dotted note is needed. */
    @Test
    fun theNotesNeedEveryDottedNote() {
        var p = target.blank()
        for (id in listOf("bass_do", "pad_do")) {
            val s = step(id)
            val (q, track) = TutoChecks.fulfil(TutoChecks.need(s, p, target)!!, s, p, target)!!
            val ghosts = TutoChecks.ghostNotes(s, q, target)!!.second
            assertTrue("$id: ${ghosts.size} notes", ghosts.size >= 6)
            fun with(n: Int) = q.withTrack(q.track(track!!).copy(notes = q.track(track).notes.copy(notes = ghosts.take(n))))
            assertFalse("$id: the first three", TutoChecks.done(s, with(3), target))
            assertFalse("$id: all but one", TutoChecks.done(s, with(ghosts.size - 1), target))
            assertTrue(id, TutoChecks.done(s, with(ghosts.size), target))
            p = with(ghosts.size)
        }
    }

    /** a/158: the mix ticked with one track of two lowered. */
    @Test
    fun theMixNeedsBothTracksLowered() {
        val levels = step("levels")
        var p = target.blank()
        for (id in listOf("bass_do", "pad_do", "lead_do")) p = TutoChecks.show(step(id), p, target)!!
        val base = TutoChecks.signature(levels.check.substringAfter(':'), p)
        fun lower(q: Project, role: String): Project {
            val ch = InstrumentTrack.mixerChannel(TutoChecks.trackFor(q, role)!!)
            return q.copy(mixer = q.mixer.copy(channels = q.mixer.channels.mapIndexed { i, c -> if (i == ch) c.copy(volume = c.volume * 0.7f) else c }))
        }
        assertFalse(TutoChecks.done(levels, p, target, base))
        assertFalse("the melody only", TutoChecks.done(levels, lower(p, "lead"), target, base))
        assertTrue(TutoChecks.done(levels, lower(lower(p, "lead"), "pad"), target, base))
        assertTrue("its Montre-moi does it", TutoChecks.done(levels, TutoChecks.show(levels, p, target)!!, target, base))
    }

    /** b/136: the bass's sound ticked without what it asked; it asks for the knobs on the screen, by their names. */
    @Test
    fun theBassSoundNeedsItsTwoKnobs() {
        val knobs = step("bass_knobs")
        assertTrue(knobs.text, "« Coupure »" in knobs.text && "« Résonance »" in knobs.text && "cutoff" !in knobs.text)
        val p = TutoChecks.show(step("bass_do"), target.blank(), target)!!
        val base = TutoChecks.signature(knobs.check.substringAfter(':'), p)
        val id = TutoChecks.trackFor(p, "bass")!!
        fun turn(q: Project, k: Int) = q.withTrack(q.track(id).copy(synth = q.track(id).synth.with(k, q.track(id).synth.get(k) + 0.2f)))
        assertFalse(TutoChecks.done(knobs, p, target, base))
        assertFalse("the resonance only", TutoChecks.done(knobs, turn(p, SynthPatch.RESONANCE), target, base))
        assertFalse("another knob", TutoChecks.done(knobs, turn(p, SynthPatch.A_RELEASE), target, base))
        assertTrue(TutoChecks.done(knobs, turn(turn(p, SynthPatch.RESONANCE), SynthPatch.CUTOFF), target, base))
        assertTrue("its Montre-moi does it", TutoChecks.done(knobs, TutoChecks.show(knobs, p, target)!!, target, base))
        // The bass sounds jump up from the start: the style's « Growl » effect on its strip (its distortion).
        val strip = p.mixer.channels[InstrumentTrack.mixerChannel(id)].inserts.map { it.effect }
        assertEquals(FxPresets.byId("growl")!!.inserts.map { it.effect }, strip.filter { it != EffectType.NONE })
    }

    /** a/142: « donne 8, 16 ou 32 mesures » ticked as « Ajouter » made the parts (all at 8 bars). */
    @Test
    fun thePhrasesNeedTheAdvisedLengths() {
        var p = target.blank()
        for (id in listOf("kick_do", "snare_do", "hat_do", "bass_do", "pad_do", "groove_name", "make_intro", "make_montee", "make_drop", "make_break", "make_drop2", "make_fin")) {
            if (!TutoChecks.done(step(id), p, target)) p = TutoChecks.show(step(id), p, target)!!
        }
        val phrases = step("song_phrases")
        // The song as « Ajouter » lays it: every part at 8 bars.
        val ordered = TutoChecks.show(step("song_order"), p, target)!!
        val laid = ordered.copy(song = ordered.song.map { it.copy(repeats = 1, bars = 8) })
        assertEquals(7, laid.song.size)
        assertTrue(TutoChecks.done(step("song_order"), laid, target))
        assertFalse("all at 8 bars", TutoChecks.done(phrases, laid, target))
        val need = TutoChecks.need(phrases, laid, target)
        assertEquals(TutoNeed.Lengths, need)
        assertTrue(TutoChecks.done(phrases, TutoChecks.fulfil(need!!, phrases, laid, target)!!.first, target))
    }

    /** a/143: the fill ticked before it was reached (the Drop 2 copied from the example rolls); a/145-148: the gap's cells. */
    @Test
    fun theTransitionsAreDoneInTheMontee() {
        var p = target.blank()
        for (id in listOf("kick_do", "snare_do", "hat_do", "bass_do", "groove_name", "make_intro", "make_montee", "make_drop", "make_break", "make_drop2", "make_fin")) {
            if (!TutoChecks.done(step(id), p, target)) p = TutoChecks.show(step(id), p, target)!!
        }
        assertTrue("the Drop 2 rolls", p.allScenes.any { s -> s.drums.lanes.any { l -> l.hits.values.any { it.repeats > 1 } } })
        val fill = step("fill")
        assertFalse("not done before it is reached", TutoChecks.done(fill, p, target))
        // Done in the Montée's last bar (its Montre-moi).
        val filled = TutoChecks.show(fill, p, target)!!
        assertTrue(TutoChecks.done(fill, filled, target))
        // A roll in the Montée's first bar is not the fill before the Drop.
        val montee = p.allScenes.indexOfFirst { it.label == "Montée" }
        val inMontee = p.switchScene(montee)
        val lane = TutoChecks.laneOf("snare")
        val early = inMontee.copy(drums = inMontee.drums.copy(lanes = inMontee.drums.lanes.mapIndexed { i, l -> if (i == lane) l.copy(hits = l.hits + (0 to Hit(1f, repeats = 3))) else l }))
        assertFalse(TutoChecks.done(fill, early, target))
        // The gap: the hits to take away are said, in the Montée's 4th bar.
        val gap = step("gap")
        val erase = TutoChecks.toErase(gap, inMontee)!!
        assertTrue(erase.isNotEmpty())
        assertTrue(erase.values.flatten().all { it >= inMontee.drums.steps - 2 && it >= 48 })
        assertFalse(TutoChecks.done(gap, filled, target))
        assertTrue(TutoChecks.toErase(gap, TutoChecks.show(gap, filled, target)!!.switchScene(montee))!!.isEmpty())
        assertNull(TutoChecks.toErase(fill, inMontee))
    }

    /** The reverb: the « Accords » track AND the snare, not any strip; in Facile, heard (the Mode simple has no send). */
    @Test
    fun theReverbIsOnTheTracksItNames() {
        var p = target.blank()
        for (id in listOf("snare_do", "pad_do")) p = TutoChecks.show(step(id), p, target)!!
        val reverb = step("reverb")
        val pad = InstrumentTrack.mixerChannel(TutoChecks.trackFor(p, "pad")!!)
        val onlyPad = p.copy(mixer = p.mixer.copy(channels = p.mixer.channels.mapIndexed { i, c -> if (i == pad) c.copy(sends = listOf(0.3f, 0f)) else c }))
        assertFalse(TutoChecks.done(reverb, onlyPad, target))
        assertTrue(TutoChecks.done(reverb, TutoChecks.show(reverb, p, target)!!, target))
        // Facile: the reverb and the riser heard, no step it cannot do.
        val facile = course.chaptersFor(TutoLevel.Facile).first { it.id == "fx" }.steps
        assertTrue(facile.all { it.listening })
        val ex = TutoChecks.example(step("reverb_hear"), target)!!
        assertNotNull(ex.before)
        assertTrue(ex.after.mixer.channels.count { c -> (c.sends.getOrNull(0) ?: 0f) > 0.05f } >= 2)
    }

    /** b/26-43: the kick's step says what the kit is, compares a long kick with it, and is ticked by hand. */
    @Test
    fun theKickStepComparesTwoKicks() {
        val kick = step("kick_sound")
        assertTrue(kick.manual)
        assertTrue(kick.why!!.isNotEmpty())
        assertTrue("« Drum & bass »" in kick.text)
        val p = target.blank()
        assertTrue(StudioDrums.isDnbKick(p.padVoices[0]))
        val demo = TutoChecks.demo(kick, p, target)!!
        val heard = demo.sounds.filterNotNull()
        assertEquals(2, heard.size)
        assertTrue(heard.all { it.lane == 0 })
        assertFalse("first a long kick", StudioDrums.isDnbKick(demo.frames.first().padVoices[0]))
        assertTrue("then the kit's", StudioDrums.isDnbKick(demo.frames.last().padVoices[0]))
        // Another kit chosen: « Montre-moi » shows the drum & bass one.
        val studio = p.copy(padVoices = p.padVoices - 0)
        assertTrue(StudioDrums.isDnbKick(TutoChecks.show(kick, studio, target)!!.padVoices[0]))
    }

    /** Léa, Karim: the Facile words, and the Pro facts. */
    @Test
    fun theWordsAreTheScreensAndRight() {
        val facile = course.chaptersFor(TutoLevel.Facile)
        val text = facile.flatMap { c -> listOf(c.why) + c.steps.flatMap { listOfNotNull(it.textFor(TutoLevel.Facile), it.why) } }.joinToString("\n")
        assertFalse("Fa phrygien in Facile", "phrygien" in text.lowercase().replace("phrygienne", ""))
        val blocks = facile.flatMap { c -> listOf(c.why) + c.steps.flatMap { listOfNotNull(it.textFor(TutoLevel.Facile), it.why) } }
        assertTrue("« nappe » without the screen's « Accords »", blocks.filter { "nappe" in it }.all { "« Accords »" in it })
        assertTrue("tempo explained", "tempo (la vitesse" in text)
        assertTrue("mesure explained", "une mesure = un tour de la boucle" in text)
        assertFalse("« plus de kick »", "plus de kick" in text)
        val pro = course.chaptersFor(TutoLevel.Pro).flatMap { c -> c.steps.map { it.textFor(TutoLevel.Pro) } }.joinToString("\n")
        assertFalse("the darkest mode is the locrian", "le mode le plus sombre" in pro)
        assertFalse("cutoff", "cutoff" in pro.lowercase())
        // The map: the parts the course lays out, each once, no « Drop 2 » twice.
        val map = TutoChecks.map(target)
        assertEquals(listOf("Intro", "Montée", "Drop", "Break", "Montée", "Drop 2", "Fin"), map.map { it.label })
        assertEquals(listOf(32, 8, 32, 16, 8, 32, 16), map.map { it.bars })
        assertTrue(StarterWizard.style("jump_up") != null)
    }
}
