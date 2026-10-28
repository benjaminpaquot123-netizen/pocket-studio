package com.pocketstudio.model.tuto

import com.pocketstudio.model.AutoTarget
import com.pocketstudio.model.AutomationLane
import com.pocketstudio.model.EffectType
import com.pocketstudio.model.Hit
import com.pocketstudio.model.InstrumentTrack
import com.pocketstudio.model.LoudnessTarget
import com.pocketstudio.model.Mixer
import com.pocketstudio.model.Project
import com.pocketstudio.model.StarterWizard
import com.pocketstudio.model.allScenes
import com.pocketstudio.model.guide.GuideStatus
import com.pocketstudio.model.instrumentTracks
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
 * The second pass of the human testers (lot « tuto 4 »: Inès, Facile; Yanis, producer, Pro): a step
 * is ticked only once done, the repeated gestures are a button, the texts say what the screen shows,
 * the Facile words are explained, the producer's musical corrections, nothing done behind the back.
 */
class TutoLot4Test {
    private val course = TutoCourse.of("jump_up")!!
    private val target = TutoTarget.of("jump_up")!!
    private val steps = course.chapters.flatMap { it.steps }.associateBy { it.id }
    private fun step(id: String) = steps.getValue(id)

    /** The project after [ids], each by its « Montre-moi » (or as it is when already done). */
    private fun upTo(vararg ids: String, from: Project = target.blank()): Project {
        var p = from
        for (id in ids) if (!TutoChecks.done(step(id), p, target)) p = TutoChecks.show(step(id), p, target) ?: error("$id: no Montre-moi")
        return p
    }

    private val parts = arrayOf("kick_do", "snare_do", "hat_do", "bass_do", "pad_do", "lead_do", "groove_name", "make_intro", "make_montee", "make_drop", "make_break", "make_drop2", "make_fin")

    // ---- C1 · only what is done ----

    /** a/156, b/223: « Bravo » at 5 parts of 7; the 7 in order are needed, a Groove in between allowed. */
    @Test
    fun theSongNeedsItsSevenPartsInOrder() {
        val order = step("song_order")
        val full = TutoChecks.show(order, upTo(*parts), target)!!
        assertEquals(7, full.song.size)
        assertTrue(TutoChecks.done(order, full, target))
        assertFalse("5 of 7", TutoChecks.done(order, full.copy(song = full.song.take(5)), target))
        assertFalse("6 of 7", TutoChecks.done(order, full.copy(song = full.song.take(6)), target))
        assertFalse("Drop 2 and Fin swapped", TutoChecks.done(order, full.copy(song = full.song.take(5) + full.song[6] + full.song[5]), target))
        val groove = full.allScenes.indexOfFirst { it.label == "Groove" }
        val withGroove = full.copy(song = full.song.take(1) + full.song[0].copy(scene = groove) + full.song.drop(1))
        assertTrue("the Groove in between", TutoChecks.done(order, withGroove, target))
    }

    /** b/280: « Par partie » touched, no curve: ticked. A curve that rises over the Montée is needed. */
    @Test
    fun theRiserNeedsACurveThatRises() {
        val riser = step("riser")
        val p = TutoChecks.show(step("song_order"), upTo(*parts), target)!!
        val montee = p.allScenes.indexOfFirst { it.label == "Montée" }
        // Its main button first: the Montée shown (« Par partie » draws the part shown).
        assertEquals(TutoNeed.Scene("Montée"), TutoChecks.need(riser, p, target))
        val ch = InstrumentTrack.mixerChannel(TutoChecks.trackFor(p, "pad")!!)
        val withFilter = p.copy(mixer = p.mixer.copy(channels = p.mixer.channels.mapIndexed { i, c ->
            if (i == ch) c.copy(inserts = c.inserts.toMutableList().also { it[c.inserts.indexOfFirst { x -> x.effect == EffectType.NONE }] = com.pocketstudio.model.Insert(EffectType.FILTER.ordinal, EffectType.FILTER.defaults) }) else c
        }))
        val slot = withFilter.mixer.channels[ch].inserts.indexOfFirst { it.effect == EffectType.FILTER }
        val cutoff = AutoTarget(AutoTarget.INSERT_PARAM, ch, slot, 0)
        fun with(lane: AutomationLane) = withFilter.copy(automation = listOf(lane))
        val perPart = AutomationLane(cutoff, mode = AutomationLane.PER_SCENE)
        assertFalse("« Par partie », nothing drawn", TutoChecks.done(riser, with(perPart.copy(sceneValues = mapOf(montee to List(64) { null }))), target))
        assertFalse("a flat line", TutoChecks.done(riser, with(perPart.withPoint(0, 0.5f, montee).withPoint(63, 0.5f, montee)), target))
        assertFalse("going down", TutoChecks.done(riser, with(perPart.withPoint(0, 0.9f, montee).withPoint(63, 0.2f, montee)), target))
        assertTrue("rising in the Montée", TutoChecks.done(riser, with(perPart.withPoint(0, 0.2f, montee).withPoint(63, 0.9f, montee)), target))
        assertFalse("rising in another part", TutoChecks.done(riser, with(perPart.withPoint(0, 0.2f, 0).withPoint(63, 0.9f, 0)), target))
        val loop = AutomationLane(cutoff).withPoint(0, 0.2f).withPoint(31, 0.9f)
        assertFalse("« Boucle »: it would sweep in every part", TutoChecks.done(riser, with(loop), target))
        val volume = AutomationLane(AutoTarget(AutoTarget.VOLUME, ch), mode = AutomationLane.PER_SCENE).withPoint(0, 0.2f, montee).withPoint(63, 0.9f, montee)
        assertFalse("the volume, not the cutoff", TutoChecks.done(riser, with(volume), target))
        // Its Montre-moi (a song curve over each Montée) is done.
        assertTrue(TutoChecks.done(riser, TutoChecks.show(riser, p, target)!!, target))
    }

    /** b/61: the hats' forces ticked on bar 1 alone; every bar, the second by « Copier sur les mesures suivantes ». */
    @Test
    fun theHatsSwingOnEveryBarWithTheCopyButton() {
        val vel = step("hat_vel")
        val p = upTo("snare_do", "hat_do")
        assertEquals(32, p.drums.steps)
        val lane = TutoChecks.laneOf("hat")
        val firstBar = p.copy(drums = p.drums.copy(lanes = p.drums.lanes.mapIndexed { i, l ->
            if (i != lane) l else l.copy(hits = l.hits.mapValues { (s, h) -> if (s < 16 && s % 4 == 0) h.copy(velocity = 0.2f) else h.copy(velocity = 1f) })
        }))
        assertFalse("bar 1 only", TutoChecks.done(vel, firstBar, target))
        assertEquals(TutoNeed.Copy("hat"), TutoChecks.need(vel, firstBar, target))
        val copied = TutoChecks.fulfil(TutoNeed.Copy("hat"), vel, firstBar, target)!!.first
        assertTrue(TutoChecks.done(vel, copied, target))
        assertEquals(firstBar.drums.lanes[lane].hits.keys, copied.drums.lanes[lane].hits.keys)
        // Nothing to copy before the first bar swings.
        assertNull(TutoChecks.need(vel, p, target))
    }

    /** a/202, b/252-306: « C'est fait » without a check. What the project or the app can see is checked; reading steps say « J'ai compris ». */
    @Test
    fun noStepIsTickedOnTheUsersWordWhenItCanBeChecked() {
        // Every step ticked by hand is a reading one (no screen to go to: « J'ai compris », never « C'est fait »).
        course.chapters.flatMap { it.steps }.filter { it.manual }.forEach { assertTrue("${it.id}: « C'est fait » on ${it.spot}", it.spot.isEmpty()) }
        // The export: shared, as the app marks it once the share sheet opened.
        val share = step("share")
        assertFalse(share.manual)
        val p = target.blank()
        assertFalse(TutoChecks.done(share, p, target))
        assertNull("no Montre-moi can share", TutoChecks.show(share, p, target))
        val shared = p.copy(guide = p.guide.withStatus(TutoChecks.SHARED_CARD, GuideStatus.DONE, 0L, "keep"))
        assertTrue(TutoChecks.done(share, shared, target))
        // The volume aimed at: « Club », the master's limiter is there already.
        val loud = step("loudness")
        assertFalse(TutoChecks.done(loud, p, target))
        assertFalse(TutoChecks.done(loud, p.copy(mixer = p.mixer.copy(target = LoudnessTarget.STREAMING)), target))
        assertTrue(TutoChecks.done(loud, p.copy(mixer = p.mixer.copy(target = LoudnessTarget.CLUB)), target))
        assertTrue(TutoChecks.done(loud, TutoChecks.show(loud, p, target)!!, target))
        // The variation: a change in the Drop 2's drums since the step began, there.
        val variation = step("variation")
        val made = upTo(*parts)
        val base = TutoChecks.signature(variation.check.removePrefix("changed:"), made)
        assertNotNull(base)
        assertFalse(TutoChecks.done(variation, made, target, base))
        assertEquals(TutoNeed.Scene("Drop 2"), TutoChecks.need(variation, made, target, base))
        assertTrue(TutoChecks.done(variation, TutoChecks.show(variation, made, target)!!, target, base))
    }

    // ---- C2 · the copying is a button ----

    /** a/45-99, b/67-172: 28 + 28 + 24 notes one by one. The first 4 bars by hand, then one touch. */
    @Test
    fun theLinesAreWrittenOnceThenCopied() {
        var p = target.blank()
        var touches = 0
        for (id in listOf("bass_do", "pad_do", "lead_do")) {
            val s = step(id)
            val (q, track) = TutoChecks.fulfil(TutoChecks.need(s, p, target)!!, s, p, target)!!
            // Lot « tuto 5 »: a bar or two by hand, the others by « Copier » (TutoLot5Test has the details).
            val (written, taps) = TutoLot5Test.writeLine(s, q, track!!, target)
            touches += taps
            p = written
            assertTrue(id, TutoChecks.done(s, p, target))
        }
        // With « Accords » on, a chord is one touch: 7 bass taps, 2 chords, 3 lead notes (was 14 + 4 + 3, and 28 + 24 + 6 before).
        assertTrue("$touches touches", touches <= 7 + 2 + 3)
        // The chord button lays the dotted triad from its lowest note (the key of the course).
        val harmony = target.harmony
        val chords = target.notes(StarterWizard.Part.Pad).notes.groupBy { it.start }.values
        chords.forEach { c -> assertEquals(c.map { it.pitch }.sorted(), harmony.chord(c.minOf { it.pitch }).sorted()) }
    }

    /** b/109-137: the 28 notes of the Sub typed again. « Copier la basse une octave plus bas »: one touch. */
    @Test
    fun theSubIsOneTouch() {
        val sub = step("bass_sub")
        val p = upTo("bass_do")
        val need = TutoChecks.need(sub, p, target)
        assertEquals(TutoNeed.Track("sub"), need)
        val (q, id) = TutoChecks.fulfil(need!!, sub, p, target)!!
        assertNotNull(id)
        assertEquals("Sub", q.track(id!!).name)
        assertTrue(TutoChecks.done(sub, q, target))
        assertNull(TutoChecks.need(sub, q, target))
    }

    // ---- C3 · the texts say what the screen shows ----

    @Test
    fun theTextsSayWhatTheScreenShows() {
        val all = course.chapters.flatMap { c -> c.steps.flatMap { listOfNotNull(it.text, it.textPro) } }
        all.forEach { t ->
            assertFalse("« 4 mesures » for an 8 bars line: $t", "sur 4 mesures" in t)
            assertFalse("« la 4e » of an 8 bars Montée: $t", "(la 4e)" in t)
            assertFalse("« Mon morceau » for « Le morceau »: $t", "Mon morceau" in t)
        }
        assertTrue("« Le morceau »" in step("song_order").text)
        // Facile's mix: the « Mix » tab's sliders and their dB, not a « Volume » slider.
        val levels = step("levels").textFor(TutoLevel.Facile)
        assertTrue("« Mix »" in levels && "dB" in levels && "curseur « Volume »" !in levels)
        // The parts come at 8 bars: the text says the next step gives their length.
        assertTrue("8 mesures" in step("song_order").text)
        // The roll is not on the cells the gap erases (a/167, b/240): the last hit before the silence.
        assertTrue("dernier coup" in step("fill").text)
    }

    /** a/167, b/240: the roll made, then erased two steps later. The fill is before the last two cells, which the gap empties. */
    @Test
    fun theRollAndTheSilenceDoNotUndoEachOther() {
        val p = upTo(*parts)
        val fill = step("fill")
        val montee = p.allScenes.indexOfFirst { it.label == "Montée" }
        val inMontee = p.switchScene(montee)
        val n = inMontee.drums.steps
        val lane = TutoChecks.laneOf("snare")
        // Lot « tuto 5 »: the silence is the last beat, the roll ×3 or more just before it (the build rolls ×2 already).
        fun roll(at: Int, times: Int = 4) = inMontee.copy(drums = inMontee.drums.copy(lanes = inMontee.drums.lanes.mapIndexed { i, l -> if (i == lane) l.copy(hits = l.hits + (at to Hit(1f, repeats = times))) else l }))
        assertFalse("on a cell the silence takes away", TutoChecks.done(fill, roll(n - 2), target))
        assertFalse(TutoChecks.done(fill, roll(n - 4), target))
        assertFalse("×2 is the build's", TutoChecks.done(fill, roll(n - 5, 2), target))
        val rolled = roll(n - 5)
        assertTrue(TutoChecks.done(fill, rolled, target))
        val gapped = TutoChecks.show(step("gap"), rolled, target)!!
        assertTrue(TutoChecks.done(step("gap"), gapped, target))
        assertTrue("the roll is still there", TutoChecks.done(fill, gapped, target))
    }

    // ---- C4 · the Facile words ----

    @Test
    fun theFacileWordsAreExplained() {
        val facile = course.chaptersFor(TutoLevel.Facile)
        val text = facile.flatMap { c -> c.steps.map { it.textFor(TutoLevel.Facile) } }.joinToString("\n")
        for (w in listOf("kick (", "caisse claire (", "« Charley » (", "accord (", "c'est le groove", "transition (", "le mix, c'est", "« mes. 1/2 »", "« Suivre »", "« dB »", "« M4A »", "« WAV »")) {
            assertTrue("$w not explained", w in text)
        }
        assertEquals("La vitesse et la couleur du morceau", facile.first().title)
        assertEquals("Le tempo et la tonalité", course.chaptersFor(TutoLevel.Pro).first().title)
    }

    // ---- C5 · the producer's corrections ----

    @Test
    fun theProducersCorrections() {
        // The rim between the beats, never on one (b/43: step 13 of bar 2 was beat 4, with the snare).
        val rims = StarterWizard.hits(target.line("perc")!!, 32).keys
        assertTrue(rims.toString(), rims.none { it % 4 == 0 })
        // The energy climbs in the Montée; the Drop 2 has the Drop's (b/199, b/204).
        val map = TutoChecks.map(target)
        val intro = map.first { it.label == "Intro" }
        val montee = map.first { it.label == "Montée" }
        val drop = map.first { it.label == "Drop" }
        assertTrue(montee.energyEnd > montee.energy)
        assertTrue(montee.energyEnd > intro.energy && montee.energyEnd < drop.energy)
        assertEquals(drop.energy, map.first { it.label == "Drop 2" }.energy, 0.001f)
        // The pump on the Sub too (b/138), the Sub first.
        val pump = step("bass_pump")
        val p = TutoChecks.fulfil(TutoNeed.Track("sub"), step("bass_sub"), upTo("bass_do"), target)!!.first
        val subCh = InstrumentTrack.mixerChannel(p.instrumentTracks.first { TutoChecks.roleOf(p, it.id) == "sub" }.id)
        val bassCh = InstrumentTrack.mixerChannel(TutoChecks.trackFor(p, "bass")!!)
        assertEquals(listOf(subCh, bassCh), TutoChecks.channelsToDo(pump, p))
        fun pumpOn(q: Project, ch: Int) = q.copy(mixer = q.mixer.copy(channels = q.mixer.channels.mapIndexed { i, c ->
            if (i == ch) c.copy(inserts = c.inserts.toMutableList().also { it[c.inserts.indexOfFirst { x -> x.effect == EffectType.NONE }] = com.pocketstudio.model.Insert(EffectType.PUMP.ordinal, EffectType.PUMP.defaults) }) else c
        }))
        assertFalse("the Basse only", TutoChecks.done(pump, pumpOn(p, bassCh), target))
        assertEquals(listOf(bassCh), TutoChecks.channelsToDo(pump, pumpOn(p, subCh)))
        assertTrue(TutoChecks.done(pump, pumpOn(pumpOn(p, subCh), bassCh), target))
        assertTrue(TutoChecks.done(pump, TutoChecks.show(pump, p, target)!!, target))
        // The Drop empties the top: no « Accords » (b/210).
        val drop1 = step("make_drop")
        val groove = upTo("kick_do", "snare_do", "hat_do", "bass_do", "pad_do", "lead_do", "groove_name")
        val made = TutoChecks.show(drop1, groove, target)!!
        assertTrue(TutoChecks.done(drop1, made, target))
        val i = made.allScenes.indexOfFirst { it.label == "Drop" }
        val padId = TutoChecks.trackFor(made, "pad")!!
        assertTrue(made.allScenes[i].trackNotes[padId]?.notes.isNullOrEmpty())
        // The OTT: on the lead (the bass has one), never asked on the master (b/290, b/297).
        assertEquals("insert:OTT", step("lead_ott").check)
        assertEquals("lead", step("lead_ott").target)
        assertTrue(course.chapters.flatMap { it.steps }.none { it.check == "master" })
        // The reverb's « Je le fais »: the snare's strip once the « Accords » sends (b/256-265).
        val reverb = step("reverb")
        val q = upTo("snare_do", "pad_do")
        val padCh = InstrumentTrack.mixerChannel(TutoChecks.trackFor(q, "pad")!!)
        assertEquals(padCh, TutoChecks.channelsToDo(reverb, q).first())
        val sent = q.copy(mixer = q.mixer.copy(channels = q.mixer.channels.mapIndexed { k, c -> if (k == padCh) c.copy(sends = listOf(0.3f, 0f)) else c }))
        assertEquals(listOf(Mixer.padChannel(TutoChecks.laneOf("snare"))), TutoChecks.channelsToDo(reverb, sent))
    }

    // ---- C6 · nothing behind the user's back ----

    /** b/196-217: the Drop 2's new bass was written by the button. The user writes it (dotted), the copy and the Sub are buttons. */
    @Test
    fun theDropTwoBassIsTheUsers() {
        val d2 = step("make_drop2")
        val before = TutoChecks.fulfil(TutoNeed.Track("sub"), step("bass_sub"), upTo("kick_do", "snare_do", "hat_do", "bass_do"), target)!!.first
        var p = upTo("pad_do", "lead_do", "groove_name", "make_intro", "make_montee", "make_drop", "make_break", from = before)
        assertEquals(TutoNeed.Part("Drop 2"), TutoChecks.need(d2, p, target))
        p = TutoChecks.fulfil(TutoNeed.Part("Drop 2"), d2, p, target)!!.first
        val i = p.allScenes.indexOfFirst { it.label == "Drop 2" }
        assertEquals(i, p.scene)
        val bass = TutoChecks.trackFor(p, "bass")!!
        assertTrue("no bass written by the button", p.track(bass).notes.notes.isEmpty())
        assertFalse(TutoChecks.done(d2, p, target))
        val (ghostTrack, ghosts) = TutoChecks.ghostNotes(d2, p, target)!!
        assertEquals(bass, ghostTrack)
        val groove = target.notes(StarterWizard.Part.Bass).notes
        assertTrue("its first bar only", ghosts.isNotEmpty() && ghosts.all { it.start < 384 })
        assertTrue("another bass", ghosts.map { it.pitch }.toSet() != groove.map { it.pitch }.toSet())
        p = TutoLot5Test.writeLine(d2, p, bass, target).first
        assertFalse("its Sub still the old one", TutoChecks.done(d2, p, target))
        assertEquals(TutoNeed.Track("sub"), TutoChecks.need(d2, p, target))
        p = TutoChecks.fulfil(TutoNeed.Track("sub"), d2, p, target)!!.first
        assertTrue(TutoChecks.done(d2, p, target))
        // The Groove's bass is untouched.
        val g = p.allScenes.indexOfFirst { it.label == "Groove" }
        assertEquals(groove.map { it.pitch to it.start }.toSet(), p.switchScene(g).track(bass).notes.notes.map { it.pitch to it.start }.toSet())
    }

    /** b/11: « Montre-moi » of the key set it at once; it shows its two buttons one after the other (and the app undoes it). */
    @Test
    fun theKeyIsShownButtonByButton() {
        val key = step("key")
        val p = target.blank()
        val demo = TutoChecks.demo(key, p, target)!!
        val scales = demo.frames.map { it.harmony.scale }
        val roots = demo.frames.map { it.harmony.root }
        assertEquals(p.harmony, demo.frames.first().harmony)
        assertTrue("the scale first, the root unchanged", demo.frames.any { it.harmony.scale == target.harmony.scale && it.harmony.root == p.harmony.root })
        assertEquals(target.harmony, demo.frames.last().harmony)
        assertTrue(scales.indexOfFirst { it == target.harmony.scale } < roots.indexOfFirst { it == target.harmony.root && it != p.harmony.root })
        // Pro: where the key is in the app, beyond the tutorial's sheet.
        assertTrue("« Gamme et accords »" in key.textFor(TutoLevel.Pro))
        // b/140: the OTT the bass gets with its « Growl » is said.
        assertTrue("OTT" in step("bass_knobs").text)
    }
}
