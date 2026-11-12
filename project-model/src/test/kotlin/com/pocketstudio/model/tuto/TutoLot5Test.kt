package com.pocketstudio.model.tuto

import com.pocketstudio.model.Hit
import com.pocketstudio.model.Project
import com.pocketstudio.model.StarterWizard
import com.pocketstudio.model.allScenes
import com.pocketstudio.model.instrumentTracks
import com.pocketstudio.model.switchScene
import com.pocketstudio.model.track
import com.pocketstudio.model.withTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The third pass of the human testers (lot « tuto 5 »: Clara and Lucas in Facile, Théo the producer
 * and Nadia in Pro): a bar or two written by hand, the rest copied; the producer's music (a real
 * build, a beat of silence, a Drop 2 bass in the key, a still Sub, no crash looping every 2 bars);
 * what is ready said; a step left undone, an order gone wrong, said.
 */
class TutoLot5Test {
    private val course = TutoCourse.of("jump_up")!!
    private val target = TutoTarget.of("jump_up")!!
    private val steps = course.chapters.flatMap { it.steps }.associateBy { it.id }
    private fun step(id: String) = steps.getValue(id)

    private fun upTo(vararg ids: String, from: Project = target.blank()): Project {
        var p = from
        for (id in ids) if (!TutoChecks.done(step(id), p, target)) p = TutoChecks.show(step(id), p, target) ?: error("$id: no Montre-moi")
        return p
    }

    private val parts = arrayOf("kick_do", "snare_do", "hat_do", "bass_do", "pad_do", "lead_do", "groove_name", "make_intro", "make_montee", "make_drop", "make_break", "make_drop2", "make_fin")

    private fun scene(p: Project, label: String) = p.allScenes.indexOfFirst { it.label == label }.also { assertTrue(label, it >= 0) }

    // ---- C5 · a bar or two by hand, the rest by « Copier » ----

    /** c §16, a/62, d/95: the same 14 notes four times; each line is a bar or two by hand, the counter at 0 when « Copier » is the button. */
    @Test
    fun eachLineIsABarOrTwoByHandThenCopied() {
        for (id in listOf("bass_do", "pad_do", "lead_do", "make_drop2")) assertTrue("$id: ${TutoChecks.handBars(step(id), target)} bars by hand", TutoChecks.handBars(step(id), target) in 1..2)
        val bass = step("bass_do")
        val (q, track) = TutoChecks.fulfil(TutoChecks.need(bass, target.blank(), target)!!, bass, target.blank(), target)!!
        val first = TutoChecks.ghostNotes(bass, q, target)!!.second
        assertTrue("bar 1 only", first.all { it.start < 384 })
        assertNull("nothing to copy yet", TutoChecks.need(bass, q, target))
        // Bar 1 written: « Copier » (bars 2 and 3), and nothing more dotted to touch (« Motif posé »).
        val bar1 = q.withTrack(q.track(track!!).copy(notes = q.track(track).notes.copy(notes = first)))
        assertEquals(TutoNeed.Copy("bass"), TutoChecks.need(bass, bar1, target))
        assertEquals(2..3, TutoChecks.copyBars(bass, bar1, target))
        val dotted = TutoChecks.ghostNotes(bass, bar1, target)!!.second
        assertTrue("every dotted note written", dotted.all { g -> first.any { it.pitch == g.pitch && it.start == g.start } })
        // Copied: bar 4 is dotted, by hand; then « Copier » writes bars 5 to 8.
        val copied = TutoChecks.fulfil(TutoNeed.Copy("bass"), bass, bar1, target)!!.first
        assertEquals(3, copied.track(track).notes.notes.map { it.start / 384 }.distinct().size)
        val next = TutoChecks.ghostNotes(bass, copied, target)!!.second.filter { g -> copied.track(track).notes.notes.none { it.pitch == g.pitch && it.start == g.start } }
        assertTrue("bar 4: ${next.map { it.start / 384 }}", next.isNotEmpty() && next.all { it.start / 384 == 3 })
        val (done, taps) = writeLine(bass, q, track, target)
        assertTrue(TutoChecks.done(bass, done, target))
        assertTrue("$taps taps", taps <= 7)
        // « Montre-moi » shows the dotted notes of now, not the whole line (c/144: other bars than the dotted ones).
        val demo = TutoChecks.demo(bass, q, target)!!
        assertTrue(demo.frames.last().track(track).notes.notes.all { it.start < 384 })
    }

    /** a/66-76: the chords' dotted triads, the bar's 3 notes in one touch (the app's « Accords » writes the dotted chord). */
    @Test
    fun theChordsAreTwoTouches() {
        val pad = step("pad_do")
        val p = upTo("bass_do")
        val (q, track) = TutoChecks.fulfil(TutoChecks.need(pad, p, target)!!, pad, p, target)!!
        val (done, taps) = writeLine(pad, q, track!!, target)
        assertTrue(TutoChecks.done(pad, done, target))
        assertEquals("Fa mineur, Sol♭ majeur", 2, taps)
    }

    // ---- C2 · the producer's music ----

    /** b/224: the Drop 2's bass a fourth up in semitones made a Si♮; along the scale, every note is in Fa phrygien. */
    @Test
    fun theDropTwoBassStaysInTheKey() {
        val key = target.harmony
        assertEquals(TutoTarget.pitchOf("C3"), TutoChecks.diatonic(TutoTarget.pitchOf("Gb2"), 5, key))
        assertEquals(TutoTarget.pitchOf("Bb2"), TutoChecks.diatonic(TutoTarget.pitchOf("F2"), 5, key))
        val p = upTo(*parts)
        val d2 = scene(p, "Drop 2")
        val bass = TutoChecks.trackFor(p, "bass")!!
        val notes = p.switchScene(d2).track(bass).notes.notes
        assertTrue(notes.isNotEmpty())
        notes.forEach { assertTrue("${it.pitch} out of the key", key.inScale(it.pitch)) }
        // Every line of the course too.
        for (part in StarterWizard.Part.entries) target.notes(part).notes.forEach { assertTrue("$part ${it.pitch}", key.inScale(it.pitch)) }
    }

    /** b: the Sub jumped octaves with the bass; it stays in one octave. */
    @Test
    fun theSubStaysStill() {
        val bass = target.notes(StarterWizard.Part.Bass).notes
        assertTrue("the bass jumps", bass.maxOf { it.pitch } - bass.minOf { it.pitch } >= 12)
        val sub = TutoChecks.subOf(bass)
        assertTrue(sub.maxOf { it.pitch } - sub.minOf { it.pitch } <= 11)
        assertTrue(sub.minOf { it.pitch } == bass.minOf { it.pitch } - 12)
        val p = TutoChecks.fulfil(TutoNeed.Track("sub"), step("bass_sub"), upTo("bass_do"), target)!!.first
        assertTrue(TutoChecks.done(step("bass_sub"), p, target))
    }

    /** b: one roll in the last bar is a fill; the Montée's snare accelerates bar after bar, its last beat silent. */
    @Test
    fun theMonteeAccelerates() {
        val p = upTo(*parts)
        val m = p.switchScene(scene(p, "Montée"))
        val n = m.drums.steps
        assertEquals(64, n)
        val snare = m.drums.lanes[TutoChecks.laneOf("snare")].hits
        val perBar = (0 until 4).map { b -> snare.keys.count { it / 16 == b } }
        assertEquals(listOf(4, 8, 16, 12), perBar)
        assertTrue("32nds in the last bar", snare.filterKeys { it >= 48 }.values.all { it.repeats == 2 })
        assertTrue("louder and louder", snare.getValue(0).velocity < snare.getValue(48).velocity)
        // Its notes stop on the last beat; the hats are still there to erase (the gap step's).
        val stop = (n - TutoChecks.GAP_CELLS) * 24
        m.instrumentTracks.forEach { t -> assertTrue(t.notes.notes.all { it.end <= stop }) }
        assertFalse("its last roll is the user's", TutoChecks.done(step("fill"), p, target))
        assertEquals(setOf(TutoChecks.laneOf("hat")), TutoChecks.toErase(step("gap"), m)!!.keys)
        // The example's Montée: the same build, the last roll, the beat of silence.
        val ex = target.song.switchScene(target.sceneOf("Montée"))
        val exSnare = ex.drums.lanes[TutoChecks.laneOf("snare")].hits
        assertEquals(4, exSnare.getValue(n - TutoChecks.GAP_CELLS - 1).repeats)
        assertTrue(ex.drums.lanes.all { l -> l.hits.keys.none { it >= n - TutoChecks.GAP_CELLS } })
    }

    /** b: 2 16ths (86 ms) of silence; a beat. */
    @Test
    fun theSilenceIsABeat() {
        assertEquals(4, TutoChecks.GAP_CELLS)
        val p = TutoChecks.show(step("fill"), upTo(*parts), target)!!
        assertTrue(TutoChecks.done(step("fill"), p, target))
        val gapped = TutoChecks.show(step("gap"), p, target)!!
        assertTrue(TutoChecks.done(step("gap"), gapped, target))
        assertTrue("the roll stays", TutoChecks.done(step("fill"), gapped, target))
    }

    /** b: the crash on the first cell of a 2-bar Drop came back every 2 bars; out until a hit can play once. */
    @Test
    fun noCrashLoopsInTheDrop() {
        assertTrue(course.chapters.flatMap { it.steps }.none { it.check == "crash" })
        assertFalse("cymbale" in course.chapters.first { it.id == "transitions" }.why)
    }

    /** b: an Intro without its snare limps; it keeps all the drums, as the Fin. */
    @Test
    fun theIntroKeepsItsDrums() {
        val p = upTo(*parts)
        for (label in listOf("Intro", "Fin")) {
            val s = p.allScenes[scene(p, label)]
            assertTrue("$label: its snare", s.drums.lanes[TutoChecks.laneOf("snare")].hits.isNotEmpty())
        }
        assertTrue(TutoChecks.done(step("make_intro"), p, target))
        assertTrue(TutoChecks.done(step("make_fin"), p, target))
    }

    /** b/260: « percussion ou charley ouvert », ticked with a closed hat; only those pads count. */
    @Test
    fun theVariationIsAPercussionOrAnOpenHat() {
        val v = step("variation")
        val p = upTo(*parts)
        val base = TutoChecks.signature(v.check.removePrefix("changed:"), p)
        val d2 = scene(p, "Drop 2")
        fun hit(lane: Int, at: Int) = p.switchScene(d2).let { q -> q.copy(drums = q.drums.copy(lanes = q.drums.lanes.mapIndexed { i, l -> if (i == lane) l.copy(hits = l.hits + (at to Hit(0.8f))) else l })) }
        val closed = TutoChecks.laneOf("hat")
        val free = (0 until 64).first { it !in p.allScenes[d2].drums.lanes[closed].hits }
        assertFalse("a closed hat", TutoChecks.done(v, hit(closed, free), target, base))
        assertTrue("the open hat", TutoChecks.done(v, hit(6, 13), target, base))
        assertTrue("a percussion", TutoChecks.done(v, hit(9, 13), target, base))
        assertTrue("its Montre-moi", TutoChecks.done(v, TutoChecks.show(v, p, target)!!, target, base))
    }

    /** c/224: « Bravo » on the durations, the order wrong (no second Montée); the order is needed too, and said. */
    @Test
    fun theDurationsNeedTheOrderAndTheOrderIsSaid() {
        val order = step("song_order")
        val phrases = step("song_phrases")
        val p = upTo(*parts)
        val full = TutoChecks.show(order, p, target)!!
        assertTrue("each part as « Ajouter » brings it", full.song.all { it.bars == 8 })
        assertTrue(TutoChecks.done(order, full, target))
        assertFalse(TutoChecks.done(phrases, full, target))
        val noSecondMontee = full.copy(song = full.song.filterIndexed { i, _ -> i != 4 })
        val sized = TutoChecks.fulfil(TutoNeed.Lengths, phrases, noSecondMontee, target)!!.first
        assertFalse("no second Montée", TutoChecks.done(phrases, sized, target))
        assertTrue(TutoChecks.done(phrases, TutoChecks.fulfil(TutoNeed.Lengths, phrases, full, target)!!.first, target))
        val (wanted, have) = TutoChecks.orderOf(order, noSecondMontee)
        assertEquals(listOf("Intro", "Montée", "Drop", "Break", "Montée", "Drop 2", "Fin"), wanted)
        assertEquals(listOf("Intro", "Montée", "Drop", "Break", "Drop 2", "Fin"), have)
        // The song: the Drops on a phrase (Intro 28 + Montée 4).
        assertEquals(128, TutoChecks.fulfil(TutoNeed.Lengths, phrases, full, target)!!.first.song.sumOf { it.bars ?: 0 })
    }

    /** a/181, c/237: « Rim couvre les autres » in a Facile song with no rim made; a pad the user never played stays silent. */
    @Test
    fun aPadNeverPlayedStaysSilent() {
        val facile = arrayOf("kick_do", "snare_do", "clap_do", "hat_do", "bass_do", "pad_do", "lead_do", "groove_name", "make_intro", "make_montee", "make_drop", "make_break", "make_drop2", "make_fin")
        val p = upTo(*facile)
        val rim = 10
        assertTrue(p.allScenes.all { s -> s.drums.lanes[rim].hits.isEmpty() })
    }

    /** The examples play what the user writes: the course's bass, the Drop 2's moved. */
    @Test
    fun theExamplesAreTheCourses() {
        val song = target.song
        val line = target.notes(StarterWizard.Part.Bass).notes.filter { it.start < 64 * 24 }.map { it.pitch to it.start }.toSet()
        val drop = song.switchScene(target.sceneOf("Drop"))
        val bass = drop.instrumentTracks.first { it.name.startsWith("Basse") && it.notes.notes.isNotEmpty() }
        assertEquals(line, bass.notes.notes.filter { it.start < 64 * 24 }.map { it.pitch to it.start }.toSet())
        val d2 = song.switchScene(target.sceneOf("Drop 2")).track(bass.id).notes.notes
        d2.forEach { assertTrue(target.harmony.inScale(it.pitch)) }
        assertTrue(d2.map { it.pitch }.toSet() != bass.notes.notes.map { it.pitch }.toSet())
    }

    // ---- C3 · nothing behind the back; C4 · the words ----

    /** b §7: the kit, its effects and pans, the other tracks: said before the drums. */
    @Test
    fun whatIsReadyIsSaid() {
        for (level in TutoLevel.entries) {
            val kick = course.chaptersFor(level).first { it.id == "kick" }
            val ready = kick.steps.first()
            assertEquals("ready", ready.id)
            assertTrue(ready.manual && ready.spot.isEmpty())
            assertTrue("« FX »" in ready.textFor(level) && "« G 30 »" in ready.textFor(level))
        }
        val pro = step("ready").textFor(TutoLevel.Pro)
        for (w in listOf("Drum & bass", "compresseur", "Batterie", "Remix", "303")) assertTrue(w, w in pro)
        assertTrue("Growl" in step("bass_do").textFor(TutoLevel.Pro))
        assertTrue("accélère" in step("make_montee").textFor(TutoLevel.Pro))
    }

    /** c §5: « Suivant » leaves a step undone; what the song misses is said first for the steps it hurts most. */
    @Test
    fun aStepLeftUndoneSaysWhatItCosts() {
        for (id in listOf("tempo", "key", "bass_do", "pad_do", "lead_do", "song_order")) assertTrue(id, step(id).skip.isNotEmpty())
        assertTrue("120 BPM" in step("tempo").skip)
    }

    @Test
    fun theWordsSayWhatTheScreenShows() {
        // a/29, c/101: « Suivre » is above the grid; « mes. 1/2 » shows while it plays.
        val clap = step("clap_do").text
        assertTrue("au-dessus de la grille" in clap && "pendant la lecture" in clap)
        // b/10-20: where the key is set first.
        assertTrue("« Je le fais » ouvre la fiche Tonalité" in step("key").textPro!!)
        // c §13: Facile, the key's two buttons are just names to touch.
        assertTrue("Pas besoin de retenir" in step("key").text)
        // a/40: Facile only does the hats.
        assertEquals("Les charleys", course.chaptersFor(TutoLevel.Facile).first { it.id == "hats" }.title)
        assertEquals("Charleys et percussions", course.chaptersFor(TutoLevel.Pro).first { it.id == "hats" }.title)
        // a/107-109: the Groove is a part, a brick is one instrument.
        assertFalse("brique de départ" in course.chapters.first { it.id == "groove" }.why + step("groove_name").text)
        // d/283: the limiter is built in, not in « Effets ».
        assertTrue("il n'apparaît pas" in step("loudness").text)
        // d/279: a reading, not a step ticked without doing it.
        assertTrue(step("meters").text.startsWith("Repère"))
        // b/119: a value to reach.
        assertTrue("au moins 15" in step("bass_knobs").text && "au moins 15" in step("lead_knobs").text)
        // a/43, d/171: no rule nobody sees for the user's own loop.
        assertTrue("quelques secondes" in step("drums_hear").text)
        // a/66-76: where the chord button is.
        assertTrue("au-dessus de la grille" in step("pad_do").text)
    }

    companion object {
        /**
         * [line]'s notes written as the user does: the dotted notes touched (a chord one touch), then
         * « Copier », until the line is all there; the project and the touches.
         */
        fun writeLine(line: TutoStep, from: Project, track: Int, target: TutoTarget): Pair<Project, Int> {
            var p = from
            var taps = 0
            var rounds = 0
            while (!TutoChecks.done(line, p, target) || TutoChecks.need(line, p, target) is TutoNeed.Copy) {
                check(++rounds < 10) { "${line.id}: $rounds rounds" }
                val need = TutoChecks.need(line, p, target)
                if (need is TutoNeed.Copy) {
                    p = TutoChecks.fulfil(need, line, p, target)!!.first
                    continue
                }
                if (need != null) break
                val have = p.track(track).notes.notes
                val missing = TutoChecks.ghostNotes(line, p, target)!!.second.filter { g -> have.none { it.pitch == g.pitch && it.start == g.start } }
                if (missing.isEmpty()) break
                taps += missing.map { it.start }.distinct().size
                p = p.withTrack(p.track(track).copy(notes = p.track(track).notes.copy(notes = have + missing)))
            }
            return p to taps
        }
    }
}
