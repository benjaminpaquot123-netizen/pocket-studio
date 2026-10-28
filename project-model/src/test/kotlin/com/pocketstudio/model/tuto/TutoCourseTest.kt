package com.pocketstudio.model.tuto

import com.pocketstudio.model.Hit
import com.pocketstudio.model.Project
import com.pocketstudio.model.StarterWizard
import com.pocketstudio.model.allScenes
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.model.track
import com.pocketstudio.model.withTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TutoCourseTest {
    private val course = TutoCourse.of("jump_up")!!
    private val target = TutoTarget.of("jump_up")!!

    private val known = Regex("""^(listen|manual|tempo|key|lane:(kick|snare|clap|hat|perc)|velocity:hat|notes:(bass|pad|lead)|track:sub|insert:[A-Z_]+|send:[01]|automation|master|changed:(volumes(:[a-z,]+)?|kit|synth:\w+(:[A-Z_0-9+]+)?)|label:\w+|part:[^:]+:(less|nokick|full|differs)|song:(order|phrases)|fill|crash|gap)$""")

    @Test
    fun theCourseHasItsChaptersAndStepsInBothLevels() {
        val facile = course.chaptersFor(TutoLevel.Facile)
        val pro = course.chaptersFor(TutoLevel.Pro)
        assertTrue("Facile: ${facile.size} chapters", facile.size >= 12)
        assertTrue("Pro: ${pro.size} chapters", pro.size > facile.size)
        assertTrue(facile.sumOf { it.steps.size } >= 30)
        assertTrue(pro.sumOf { it.steps.size } >= 50)
        // The structure block is in both, with its map.
        assertTrue(facile.any { it.map } && pro.any { it.map })
        val ids = course.chapters.flatMap { c -> c.steps.map { it.id } }
        assertEquals("step ids are unique", ids.size, ids.toSet().size)
        course.chapters.flatMap { it.steps }.forEach { s ->
            assertTrue("${s.id}: unknown check ${s.check}", known.matches(s.check))
            if (s.listen.isNotEmpty()) assertNotNull("${s.id}: no example for ${s.listen}", TutoChecks.example(s, target))
            // A step to tick by its example has one.
            if (s.listening) assertTrue(s.id, s.listen.isNotEmpty())
        }
    }

    /** From an empty project, « Montre-moi » on each step in turn ticks it: the whole jump up track is made. */
    @Test
    fun showMeTicksEveryStepFromAnEmptyProject() {
        val empty = target.blank()
        val steps = course.chaptersFor(TutoLevel.Pro).flatMap { it.steps }
        steps.forEach { assertFalse("${it.id} is done in an empty project", TutoChecks.done(it, empty, target, TutoChecks.signature(it.check.substringAfter(':'), empty))) }
        var p = empty
        for (step in steps) {
            if (step.manual || step.listening || step.check.startsWith("changed:")) continue
            // A step an earlier Montre-moi already did (the hats' strengths come with the hats) is ticked as it is.
            if (TutoChecks.done(step, p, target)) continue
            p = TutoChecks.show(step, p, target) ?: error("${step.id}: no Montre-moi")
            assertTrue("${step.id} is not done after Montre-moi", TutoChecks.done(step, p, target))
        }
        // The track: seven parts, in order, in phrases of 8 bars.
        assertEquals(7, p.song.size)
        assertEquals(listOf("Intro", "Montée", "Drop", "Break", "Montée", "Drop 2", "Fin"), p.song.map { p.allScenes[it.scene].label })
    }

    @Test
    fun aKickCloseEnoughPassesAndAWrongOneDoesNot() {
        val step = course.chapters.flatMap { it.steps }.first { it.id == "kick_do" }
        fun with(vararg at: Int) = Project(name = "").let { p ->
            p.copy(drums = p.drums.copy(lanes = p.drums.lanes.mapIndexed { i, l -> if (i == 0) l.copy(hits = at.associate { it to Hit(1f) }) else l }))
        }
        assertTrue(TutoChecks.done(step, with(0, 10), target))
        // One extra hit is tolerated; half the hits wrong are not.
        assertTrue(TutoChecks.done(step, with(0, 10, 14), target))
        assertFalse(TutoChecks.done(step, with(0), target))
        assertFalse(TutoChecks.done(step, with(2, 6, 12), target))
        assertFalse(TutoChecks.done(step, with(), target))
        // The dotted cells are the recipe's.
        assertEquals(0 to setOf(0, 10, 16, 26), TutoChecks.hint(step, target, 32))
    }

    @Test
    fun aChangedStepWaitsForAChangeSinceItBegan() {
        val step = TutoStep("v", "", check = "changed:volumes")
        val p = Project(name = "")
        val base = TutoChecks.signature("volumes", p)
        assertFalse(TutoChecks.done(step, p, target, base))
        val louder = p.copy(mixer = p.mixer.copy(channels = p.mixer.channels.mapIndexed { i, c -> if (i == 2) c.copy(volume = 0.5f) else c }))
        assertTrue(TutoChecks.done(step, louder, target, base))
    }

    @Test
    fun examplesAreMadeFromTheTargetOnly() {
        val drums = TutoChecks.example(TutoStep("k", "", listen = "drums:kick"), target)!!
        assertNull(drums.before)
        assertEquals(174.0, drums.after.tempoBpm, 0.0)
        assertEquals(StarterWizard.hits("X.........X.....").keys, drums.after.drums.lanes[0].hits.keys)
        assertTrue(drums.after.drums.lanes.drop(1).all { it.hits.isEmpty() })
        val vs = TutoChecks.example(TutoStep("v", "", listen = "vs:Drop:Intro"), target)!!
        assertEquals("Drop", vs.before!!.allScenes[vs.before!!.scene].label)
        assertEquals("Intro", vs.after.allScenes[vs.after.scene].label)
        assertTrue(vs.after.song.isEmpty())
        val map = TutoChecks.map(target)
        assertEquals("Intro", map.first().label)
        assertEquals("Fin", map.last().label)
        assertTrue(map.first { it.label == "Drop" }.energy > map.first().energy)
        assertTrue(map.all { it.bars % 8 == 0 })
    }

    @Test
    fun progressIsSavedAndReadBack() {
        val p = TutoProgress("jump_up", TutoLevel.Pro, setOf("tempo", "key"), "kick_do")
        assertEquals(p, TutoProgress.decode(p.encode()))
        assertNull(TutoProgress.decode("{broken"))
        assertNull(TutoProgress.decode(null))
    }

    @Test
    fun everyTextHasItsTranslations() {
        // TEMPORARY (owner, 2026-10-28, tutorial debug): the course's texts are French only for now.
        if ("tuto" in com.pocketstudio.model.i18n.I18nCatalogTest.PENDING_TRANSLATION_AREAS) return
        val texts = course.chapters.flatMap { c -> listOf(c.title, c.why) + c.steps.flatMap { listOfNotNull(it.text, it.textPro, it.why) } }.filter { it.isNotEmpty() }
        val problems = (Lang.entries - Lang.FR).flatMap { lang ->
            val table = I18n.table(lang)
            texts.filter { table[it].isNullOrBlank() }.map { "${lang.code}: $it" }
        }
        assertTrue("${problems.size} missing:\n" + problems.take(40).joinToString("\n"), problems.isEmpty())
    }

    /** Owner (2026-10-28): the tutorial's project starts truly empty, with the style's drum sounds on its pads. */
    @Test
    fun theTutorialProjectIsEmptyWithTheStylesDrums() {
        val p = target.blank()
        assertTrue(p.drums.lanes.all { it.hits.isEmpty() })
        assertTrue(p.tracks.isEmpty())
        assertTrue(TutoChecks.blankFirst(p))
        assertTrue(p.song.isEmpty() && p.allScenes.all { it.isEmpty })
        assertEquals(target.song.kitVersion, p.kitVersion)
        // Jump up: the drum & bass kit built in, its short kick tuned to F on the kick's pad (« Kit : Drum & bass »).
        assertTrue(com.pocketstudio.model.StudioDrums.isDnbKick(p.padVoices[0]))
        assertEquals(com.pocketstudio.model.StudioDrums.kickHz(5), p.padVoices[0]!!.param(com.pocketstudio.model.PadVoice.TUNE)!!, 0.01f)
        assertEquals(target.song.padVoices, p.padVoices - 0)
        // The example's drum effects (the punch on the kick and the snare), not the snare's reverb (a step's).
        val kick = com.pocketstudio.model.Mixer.padChannel(0)
        val snare = com.pocketstudio.model.Mixer.padChannel(2)
        assertEquals(target.song.mixer.channels[kick].inserts, p.mixer.channels[kick].inserts)
        assertTrue(p.mixer.channels[snare].inserts.any { it.effect == com.pocketstudio.model.EffectType.COMPRESSOR })
        assertTrue(p.mixer.channels.all { c -> c.sends.all { it == 0f } })
        assertEquals(120.0, p.tempoBpm, 0.0)
        assertEquals(com.pocketstudio.model.Scale.Off, p.harmony.scale)
        // Nothing in it is done but nothing.
        course.chapters.flatMap { it.steps }.forEach { assertFalse(it.id, TutoChecks.done(it, p, target)) }
    }

    /** The snare's line lasts 2 bars: « Passer à 2 mesures » first; the bass: « Créer la piste Basse », with its notes dotted. */
    @Test
    fun eachStepSaysWhatItNeedsAndTheButtonMakesOnlyThat() {
        val steps = course.chapters.flatMap { it.steps }.associateBy { it.id }
        var p = target.blank()
        assertNull(TutoChecks.need(steps.getValue("kick_do"), p, target))
        val snare = steps.getValue("snare_do")
        assertEquals(TutoNeed.Bars(2), TutoChecks.need(snare, p, target))
        p = TutoChecks.fulfil(TutoNeed.Bars(2), snare, p, target)!!.first
        assertEquals(32, p.drums.steps)
        assertNull(TutoChecks.need(snare, p, target))
        assertTrue("no hit made", p.drums.lanes.all { it.hits.isEmpty() })
        val bass = steps.getValue("bass_do")
        assertEquals(TutoNeed.Track("bass"), TutoChecks.need(bass, p, target))
        assertNull(TutoChecks.ghostNotes(bass, p, target))
        val (q, id) = TutoChecks.fulfil(TutoNeed.Track("bass"), bass, p, target)!!
        // The blank first track became the Basse: the jump up bass sound, no note.
        assertEquals(0, id)
        assertEquals("Basse", q.track(0).name)
        // The style's bass sound (as « + Piste » › Basse gives it in jump up), not the blank synth.
        assertTrue(q.track(0).synth != Project().synth)
        assertTrue(q.track(0).notes.notes.isEmpty())
        assertFalse(TutoChecks.done(bass, q, target))
        val ghosts = TutoChecks.ghostNotes(bass, q, target)!!
        assertEquals(0, ghosts.first)
        assertTrue(ghosts.second.size >= 4)
        // The user taps the dotted notes: done.
        val written = q.withTrack(q.track(0).copy(notes = q.track(0).notes.copy(notes = ghosts.second)))
        assertTrue(TutoChecks.done(bass, written, target))
        // The pad's track is a new one (the first is the bass now).
        val pad = steps.getValue("pad_do")
        val (r, padId) = TutoChecks.fulfil(TutoChecks.need(pad, written, target)!!, pad, written, target)!!
        assertTrue(padId!! > 0)
        assertEquals("Accords", r.track(padId).name)
    }

    /** « Montre-moi » goes a cell or a note at a time and ends on the target; the key detector sees a key already right. */
    @Test
    fun showMeGoesStepByStepAndTheKeyIsSeenWhenAlreadyRight() {
        val steps = course.chapters.flatMap { it.steps }.associateBy { it.id }
        val p = target.blank()
        val kick = TutoChecks.demo(steps.getValue("kick_do"), p, target)!!
        assertTrue(kick.frames.size >= 3)
        assertEquals(kick.frames.size, kick.sounds.size)
        val hits = kick.frames.map { it.drums.lanes[0].hits.size }
        assertEquals(hits.sorted(), hits)
        assertTrue(TutoChecks.done(steps.getValue("kick_do"), kick.frames.last(), target))
        assertTrue(kick.sounds.filterNotNull().all { it.lane == 0 })
        val bass = TutoChecks.demo(steps.getValue("bass_do"), p, target)!!
        val notes = bass.frames.map { f -> f.track(0).notes.notes.size }
        assertEquals(0, notes.first())
        assertEquals(notes.sorted(), notes)
        assertTrue(TutoChecks.done(steps.getValue("bass_do"), bass.frames.last(), target))
        // The key: right already, it is done (the step ticks as soon as it is shown).
        val key = steps.getValue("key")
        assertTrue(TutoChecks.done(key, p.copy(harmony = com.pocketstudio.model.Harmony(5, com.pocketstudio.model.Scale.Phrygian)), target))
        assertEquals("tuto.key", key.spot)
        // The texts name the words on the screens.
        assertTrue(key.text, "« Phrygienne »" in key.text && "« Fa »" in key.text)
        assertTrue("« Caisse »" in steps.getValue("snare_do").text)
        // The reference example: the Montée, then the Drop.
        val ref = TutoChecks.example(steps.getValue("ref"), target)!!
        assertEquals("Montée", ref.before!!.allScenes[ref.before!!.scene].label)
        assertEquals("Drop", ref.after.allScenes[ref.after.scene].label)
    }
}
