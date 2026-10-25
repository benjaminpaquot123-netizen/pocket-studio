package com.pocketstudio.model.guide

import com.pocketstudio.model.EffectType
import com.pocketstudio.model.Insert
import com.pocketstudio.model.InstrumentTrack
import com.pocketstudio.model.Mixer
import com.pocketstudio.model.PadSample
import com.pocketstudio.model.Project
import com.pocketstudio.model.SongPart
import com.pocketstudio.model.StarterWizard
import com.pocketstudio.model.allScenes
import com.pocketstudio.model.instrumentTracks
import com.pocketstudio.model.isFillPart
import com.pocketstudio.model.songBars
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * G4 (docs/PLAN-GUIDE.md §3.5, stages 3 to 6): each card family's detectors fire on the songs they
 * are about, its actions change what they say they change, and « Annuler mon choix » / « Ramener »
 * bring the song back as it was.
 */
class GuideStagesTest {
    private fun build(id: String): Project {
        val style = StarterWizard.style(id)!!
        return GuideActions.withBaseline(StarterWizard.build(style, style.lines.associate { it.role to null as PadSample? }, style.melody.mood,
            StarterWizard.defaultParts(style).associateWith { StarterWizard.soundsFor(it, style).first() }))
    }

    private val jump: Project by lazy { build("jump_up") }

    private fun Project.trackKey(name: String) = GuideTrack.inst(instrumentTracks.first { it.name == name }.id)

    private fun Project.strip(name: String) = mixer.channels[InstrumentTrack.mixerChannel(instrumentTracks.first { it.name == name }.id)]

    private fun facts(p: Project) = GuideFacts.of(p)

    // --- stage 3: « Voilà ce que j'ai mis », effect by effect -------------------------------------------

    @Test
    fun anEffectOfWhatTheAssistantPutIsRetiredAndPutBackWithoutDecidingTheCard() {
        val bass = jump.trackKey("Basse")
        val slot = jump.strip("Basse").inserts.indexOfFirst { it.effect != EffectType.NONE && !it.bypass }
        assertTrue(slot >= 0)
        val effect = jump.strip("Basse").inserts[slot]
        // Any card of the effects takes it (the review card, the bass's own).
        val off = GuideActions.tryApply(jump, "fx.review", GuideActions.effectOff(bass, slot), 1000L)
        assertNull(off.problem)
        val p = off.project
        assertTrue(p.strip("Basse").inserts[slot].bypass)
        assertEquals(effect.params, p.strip("Basse").inserts[slot].params)  // its settings kept
        // One effect does not decide the whole card; the history keeps it, on the bass.
        assertEquals(GuideStatus.TODO, p.guide.statusOf("fx.review"))
        val change = p.guide.history.single()
        assertEquals(bass, change.track)
        assertEquals("fx.review", change.card)
        // « Le remettre »: heard again, as it was.
        val on = GuideActions.apply(p, "fx.bass", GuideActions.effectOn(bass, slot), 2000L)
        assertEquals(jump.strip("Basse"), on.strip("Basse"))
        // « Ramener » the first change: the same.
        assertEquals(jump.strip("Basse"), GuideActions.revert(p, change.id, 2000L).strip("Basse"))
        // An effect slot that is empty, a track that is not there, a card not about effects: nothing.
        val empty = jump.strip("Basse").inserts.indexOfFirst { it.effect == EffectType.NONE }
        assertEquals(jump, GuideActions.tryApply(jump, "fx.review", GuideActions.effectOff(bass, empty), 1000L).project)
        assertEquals(jump, GuideActions.apply(jump, "fx.review", GuideActions.effectOff("inst:99", 0), 1000L))
        assertEquals(jump, GuideActions.apply(jump, "arr.cut", GuideActions.effectOff(bass, slot), 1000L))
    }

    @Test
    fun aSendIsTakenOffAndPutBackAtItsLevel() {
        val lead = jump.trackKey("Lead")
        val bus = jump.strip("Lead").sends.indexOfFirst { it > 0.01f }
        assertTrue(bus >= 0)
        val level = jump.strip("Lead").sends[bus]
        // Nothing to put back before it was taken off.
        assertNull(GuideActions.dynamicOption(jump, GuideActions.sendOn(lead, bus)))
        val dry = GuideActions.apply(jump, "fx.review", GuideActions.sendOff(lead, bus), 1000L)
        assertEquals(0f, dry.strip("Lead").sends[bus], 0f)
        val wet = GuideActions.apply(dry, "fx.review", GuideActions.sendOn(lead, bus), 2000L)
        assertEquals(level, wet.strip("Lead").sends[bus], 1e-6f)
    }

    @Test
    fun aMasterEffectIsRetiredToo() {
        val slot = jump.mixer.master.indexOfFirst { it.effect != EffectType.NONE }
        assertTrue(slot >= 0)
        val p = GuideActions.apply(jump, "fx.review", GuideActions.effectOff(GuideTrack.MASTER, slot), 1000L)
        assertTrue(p.mixer.master[slot].bypass)
        assertEquals(jump.mixer.master, GuideActions.undoChoice(p, "fx.review", 2000L).mixer.master)
    }

    /** Review G1a: a lone low cut (« Coupe-bas partout ») does not tick the voice's card any more. */
    @Test
    fun aLowCutAloneDoesNotTickTheVoicesCard() {
        val ch = InstrumentTrack.mixerChannel(jump.instrumentTracks.first { it.name == "Voix" }.id)
        val cut = Insert(EffectType.PARAM_EQ.ordinal, listOf(0.5f, 0.5f, 0.4f, 0.6f)).sanitized()
        val p = jump.copy(mixer = jump.mixer.copy(channels = jump.mixer.channels.mapIndexed { i, c ->
            if (i == ch) c.copy(inserts = listOf(cut) + List(Mixer.INSERTS - 1) { Insert() }) else c
        }))
        assertTrue(facts(p).holds("fx:vocal", GuideRoles.SONG))
        assertFalse(facts(p).holds("colour:vocal", GuideRoles.SONG))
        assertNotEquals(GuideStatus.DONE_ELSEWHERE, GuideAnalyzer.analyze(p).status("fx.vocal")!!.status)
        val clear = GuideActions.apply(p, "fx.vocal", "clear", 1000L)
        assertTrue(facts(clear).holds("colour:vocal", GuideRoles.SONG))
    }

    // --- stage 4: the arrangement by subtraction ---------------------------------------------------------

    @Test
    fun aBreakMadeFromTheDropComesAndGoesWithItsSceneAndCurves() {
        val before = GuideSong(jump)
        val peak = before.sectionsOf(GuideSection.Kind.PEAK).first()
        val p = GuideActions.apply(jump, "arr.subtract", "break", 1000L)
        val after = GuideSong(p)
        assertEquals(jump.song.size + 1, p.song.size)
        assertEquals(jump.songBars + 8, p.songBars)
        // Right after the first Drop: the Drop's notes without the kick nor the basses.
        val added = p.song[peak.parts.last() + 1]
        assertEquals("Break", p.allScenes[added.scene].label)
        val basses = p.guideTracks().filter { it.role in setOf(GuideRoles.BASS, GuideRoles.SUB) }.mapNotNull { GuideTrack.partKey(it.key) }
        assertTrue(added.muted.containsAll(basses))
        assertEquals(before.sectionsOf(GuideSection.Kind.BREAK).sumOf { it.bars } + 8, after.sectionsOf(GuideSection.Kind.BREAK).sumOf { it.bars })
        // The curves after it moved with their parts (the second Drop's still starts where its curves do).
        val drop2 = after.sectionsOf(GuideSection.Kind.PEAK)[1]
        assertEquals(before.sectionsOf(GuideSection.Kind.PEAK)[1].start + 8, drop2.start)
        // « Annuler mon choix »: the song, its scenes and its curves as they were.
        val back = GuideActions.tryUndoChoice(p, "arr.subtract", 2000L)
        assertNull(back.problem)
        assertEquals(jump.song, back.project.song)
        assertEquals(jump.allScenes, back.project.allScenes)
        // Every curve plays as it did (the way back may leave a point that holds the same value).
        assertEquals(jump.automation.map { it.target }, back.project.automation.map { it.target })
        jump.automation.zip(back.project.automation).forEach { (a, b) ->
            assertEquals(a.songCurve(jump.songBars), b.songCurve(jump.songBars))
            assertEquals(a.songJumps, b.songJumps)
        }
        assertEquals(GuideStatus.TODO, back.project.guide.statusOf("arr.subtract"))
    }

    @Test
    fun anIntroThatPlaysTheWholeDropLosesItsBassesAndMelody() {
        val song = GuideSong(jump)
        val peak = song.sectionsOf(GuideSection.Kind.PEAK).first()
        val intro = song.sections.first { it.kind == GuideSection.Kind.INTRO }
        val keys = jump.guideTracks().mapNotNull { GuideTrack.partKey(it.key) }.toSet()
        // The intro plays the Drop's own notes, every track: nothing taken away.
        val full = jump.copy(song = jump.song.mapIndexed { i, part ->
            if (i in intro.parts) part.copy(muted = emptySet(), play = keys.associateWith { peak.scene }) else part
        })
        assertFalse(facts(full).holds("subtractive", GuideRoles.SONG))
        val p = GuideActions.apply(full, "arr.subtract", "lighten", 1000L)
        assertTrue(facts(p).holds("subtractive", GuideRoles.SONG))
        val bass = GuideTrack.partKey(p.trackKey("Basse"))!!
        assertTrue(intro.parts.all { bass in p.song[it].muted })
        // Only the intro's parts changed.
        assertEquals(full.song.filterIndexed { i, _ -> i !in intro.parts }, p.song.filterIndexed { i, _ -> i !in intro.parts })
        assertEquals(full.song, GuideActions.undoChoice(p, "arr.subtract", 2000L).song)
    }

    // --- stage 5: the movement ---------------------------------------------------------------------------

    @Test
    fun theFilterOpensOverEachMonteeAndComesBack() {
        val noCurves = jump.copy(automation = emptyList())
        assertFalse(facts(noCurves).holds("build_moves", GuideRoles.SONG))
        val p = GuideActions.apply(noCurves, "move.build", "sweep", 1000L)
        assertTrue(facts(p).holds("build_moves", GuideRoles.SONG))
        assertEquals(1, p.automation.size)
        // At rest when the Drop lands.
        val lane = p.automation.single()
        val build = GuideSong(p).sectionsOf(GuideSection.Kind.BUILD).first()
        val curve = lane.songCurve(p.songBars)
        assertTrue(curve[build.start] < 0.3f)
        assertEquals(0.5f, curve[build.end], 1e-6f)
        assertEquals(noCurves.automation, GuideActions.undoChoice(p, "move.build", 2000L).automation)
    }

    @Test
    fun theReverbIsThrownAtTheEndOfEachPhraseOfTheLead() {
        val noCurves = jump.copy(automation = emptyList())
        assertFalse(facts(noCurves).holds("send_moves", GuideRoles.SONG))
        val p = GuideActions.apply(noCurves, "move.throw", "throw", 1000L)
        assertTrue(facts(p).holds("send_moves", GuideRoles.SONG))
        val lane = p.automation.single()
        assertEquals(p.strip("Lead"), p.mixer.channels[lane.target.ch])
        // Thrown on the last bar the lead plays in each phrase.
        val song = GuideSong(p)
        val curve = lane.songCurve(p.songBars)
        val thrown = curve.indices.filter { curve[it] > 0.5f }
        assertTrue(thrown.isNotEmpty())
        // At most once per 8-bar phrase, on a bar the lead plays.
        assertTrue(thrown.all { b -> song.sounds(p.trackKey("Lead"), b) })
        assertTrue(song.sections.all { s -> (s.start until s.end step 8).all { from -> thrown.count { it in from until minOf(from + 8, s.end) } <= 1 } })
        // Its own change is a curve: it cannot be heard in the first Drop's « Sans / Avec ».
        assertFalse(GuideLibrary.forProject(p).card("move.throw")!!.proposals.first { it.id == "throw" }.action.heard)
        // « Ramener »: the curve goes.
        assertTrue(GuideActions.revert(p, p.guide.history.single().id, 2000L).automation.isEmpty())
    }

    @Test
    fun stillSectionsBreatheAndTheirCurveIsLeftWhereItHasPoints() {
        val noCurves = jump.copy(automation = emptyList())
        val still = facts(noCurves).list.first { it.id == "automation.missing" }
        val p = GuideActions.apply(noCurves, "move.sections", "wave", 1000L)
        assertTrue(facts(p).holds("sections_move", GuideRoles.SONG) || facts(p).list.first { it.id == "automation.missing" }.words.size < still.words.size)
        // A second time: nothing more to do.
        val again = GuideActions.tryApply(p.copy(guide = p.guide.copy(cards = emptyMap())), "move.sections", "wave", 2000L)
        assertEquals(p.automation, again.project.automation)
    }

    @Test
    fun aFillComesBeforeEachDropThatHasNone() {
        // The assistant's song with its fills taken out.
        val bare = jump.copy(song = jump.song.map { part -> if (jump.isFillPart(part)) part.copy(play = part.play - SongPart.DRUMS) else part })
        val peaks = GuideSong(bare).sectionsOf(GuideSection.Kind.PEAK).filter { it.start > 0 }
        assertTrue(peaks.isNotEmpty())
        val missing = peaks.count { !GuideSong(bare).fillBars[it.start - 1] }
        assertTrue("every Drop already follows a fill", missing > 0)
        assertFalse(facts(bare).holds("fills", GuideRoles.SONG))
        val p = GuideActions.apply(bare, "move.fills", "fills", 1000L)
        assertTrue(facts(p).holds("fills", GuideRoles.SONG))
        // The song lasts as long, the Drops start where they did.
        assertEquals(bare.songBars, p.songBars)
        assertEquals(peaks.map { it.start }, GuideSong(p).sectionsOf(GuideSection.Kind.PEAK).filter { it.start > 0 }.map { it.start })
        val back = GuideActions.tryUndoChoice(p, "move.fills", 2000L)
        assertNull(back.problem)
        assertEquals(bare.song, back.project.song)
        assertEquals(bare.allScenes, back.project.allScenes)
    }

    // --- stage 6: levels and checks ----------------------------------------------------------------------

    private fun measured(p: Project, tracks: Map<String, Float>, mono: Float? = null, phone: Float? = null): Project =
        p.copy(guide = p.guide.copy(measures = GuideMeasures(at = 1L, tracks = tracks, monoLossDb = mono, phoneDb = phone, signature = GuideSignatures.sound(p))))

    @Test
    fun eachTrackGoesBackToItsPlaceFromTheMeasures() {
        val kick = jump.guideTracks().first { it.role == GuideRoles.KICK }.key
        val pad = jump.trackKey("Nappe")
        // Not measured: the card can only measure first.
        assertNotNull(GuideActions.tryApply(jump, "mix.balance", "balance", 1000L).problem)
        // The pad 7 dB over its place (−9 under the kick).
        val p = measured(jump, mapOf(kick to -10f, pad to -12f))
        val f = facts(p)
        assertFalse(f.holds("balanced", GuideRoles.SONG))
        assertEquals(listOf(pad), f.list.first { it.id == "mix.levels" }.tracks)
        val done = GuideActions.apply(p, "mix.balance", "balance", 1000L)
        // Down by 6 dB at most at once; the measures moved with it: now within 3 dB, balanced.
        assertEquals(GuideLevels.faderDb(jump.strip("Nappe").volume) - 6f, GuideLevels.faderDb(done.strip("Nappe").volume), 0.01f)
        assertTrue(facts(done).holds("balanced", GuideRoles.SONG))
        assertTrue(GuideAnalyzer.analyze(done).status("mix.balance")!!.status.done)
        // Brought back: the pad's fader as it was.
        assertEquals(jump.strip("Nappe").volume, GuideActions.undoChoice(done, "mix.balance", 2000L).strip("Nappe").volume, 0f)
        // Any change to the sound: the measures are out of date, left aside.
        assertFalse(facts(done.copy(tempoBpm = done.tempoBpm + 1)).holds("measured", GuideRoles.SONG))
        // The Guide's own state is not the sound.
        assertTrue(facts(done.copy(guide = done.guide.copy(step = "x"))).holds("measured", GuideRoles.SONG))
    }

    @Test
    fun theMonoCheckSaysWhatVanishesAndNarrows() {
        val wide = measured(jump, emptyMap(), mono = 4f)
        assertFalse(facts(wide).holds("mono_ok", GuideRoles.SONG))
        assertEquals(GuideSeverity.WARN, facts(wide).list.first { it.id == "mono.loss" }.severity)
        // Listening changes nothing.
        assertEquals(wide, GuideActions.tryApply(wide, "mix.mono", "listen", 1000L).project)
        val p = GuideActions.apply(wide, "mix.mono", "narrow", 1000L)
        assertEquals(150f, p.mixer.monoBassHz, 0f)
        assertTrue(p.mixer.channels.flatMap { it.inserts }.filter { it.effect == EffectType.WIDENER && !it.bypass }.all { it.params[0] <= 0.5f })
        assertEquals(wide.mixer, GuideActions.undoChoice(p, "mix.mono", 2000L).mixer)
        // A mix that keeps everything in mono: checked by the analysis.
        assertEquals(GuideStatus.DONE_ELSEWHERE, GuideAnalyzer.analyze(measured(jump, emptyMap(), mono = 0.8f)).status("mix.mono")!!.status)
    }

    @Test
    fun thePhoneCheckMakesTheBassAudible() {
        val lost = measured(jump, emptyMap(), phone = -22f)
        val fact = facts(lost).list.first { it.id == "phone.low" }
        assertEquals(GuideSeverity.WARN, fact.severity)
        assertTrue(jump.trackKey("Basse") in fact.tracks)
        val p = GuideActions.apply(lost, "mix.phone", "audible", 1000L)
        val exciter = { q: Project -> q.strip("Basse").inserts.first { it.effect == EffectType.BASS_EXCITER } }
        assertTrue(abs(exciter(p).params[0] - exciter(lost).params[0] - 0.15f) < 1e-4f || exciter(p).params[0] == 1f)
        assertEquals(lost.strip("Basse"), GuideActions.undoChoice(p, "mix.phone", 2000L).strip("Basse"))
        assertEquals(GuideStatus.DONE_ELSEWHERE, GuideAnalyzer.analyze(measured(jump, emptyMap(), phone = -9f)).status("mix.phone")!!.status)
    }

    @Test
    fun theReferenceIsHeardNotApplied() {
        val r = GuideAnalyzer.analyze(jump)
        val card = r.status("mix.reference")!!
        assertTrue(card.applicable)
        assertFalse(card.settled)
        val listen = card.card.proposals.first()
        assertEquals(GuideAction.LISTEN, listen.action.type)
        assertEquals("reference", listen.action.name)
        assertFalse(listen.action.edits)
        assertEquals(jump, GuideActions.apply(jump, "mix.reference", "listen", 1000L))
    }

    @Test
    fun everyStyleHasItsChecksAndTheirActionsAreKnown() {
        for (style in StarterWizard.STYLES) {
            val parcours = GuideLibrary.forStyle(style.id)!!
            listOf("mix.mono", "mix.phone", "mix.reference").forEach { assertNotNull("${style.id}: $it", parcours.card(it)) }
            assertEquals(style.id, GuideAction.SUBTRACT, parcours.card("arr.subtract")!!.proposals.first().action.type)
            // The master's target and the curves cannot be heard in the « Sans / Avec »: no ▶ for them.
            parcours.cards.flatMap { it.proposals }.filter { it.action.type in setOf(GuideAction.TARGET, GuideAction.CURVE) }.forEach { assertFalse(it.action.heard) }
        }
    }
}
