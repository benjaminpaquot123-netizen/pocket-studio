package com.pocketstudio.model.guide

import com.pocketstudio.model.EffectType
import com.pocketstudio.model.Insert
import com.pocketstudio.model.InstrumentTrack
import com.pocketstudio.model.LoudnessTarget
import com.pocketstudio.model.PadSample
import com.pocketstudio.model.Project
import com.pocketstudio.model.StarterWizard
import com.pocketstudio.model.instrumentTracks
import com.pocketstudio.model.longFxCount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** What the Guide's buttons do: each change written with its value before, and brought back in a gesture. */
class GuideActionsTest {
    private val song: Project by lazy {
        val style = StarterWizard.style("jump_up")!!
        GuideActions.withBaseline(StarterWizard.build(style, style.lines.associate { it.role to null as PadSample? }, style.melody.mood,
            StarterWizard.defaultParts(style).associateWith { StarterWizard.soundsFor(it, style).first() }))
    }

    /** The song without its low cuts (since G0 the assistant cuts the lows of every track, RoleMix): their slots left empty. */
    private val noCuts: Project by lazy {
        GuideActions.withBaseline(song.copy(guide = GuideState.EMPTY).let { p ->
            p.copy(mixer = p.mixer.copy(channels = p.mixer.channels.map { c -> c.copy(inserts = c.inserts.map { i -> if (GuideFacts.isLowCut(i)) Insert() else i }) }))
        })
    }

    private fun Project.strip(name: String) = mixer.channels[InstrumentTrack.mixerChannel(instrumentTracks.first { it.name == name }.id)]

    @Test
    fun anOptionChangesTheTrackAndTheHistoryKeepsTheValueBefore() {
        assertTrue(song.strip("Sub").inserts.all { it.effect == EffectType.NONE })
        val p = GuideActions.apply(song, "fx.sub", "light", 1000L)
        assertEquals(EffectType.SATURATION, p.strip("Sub").inserts.first().effect)
        assertEquals(GuideStatus.DONE, p.guide.statusOf("fx.sub"))
        assertEquals("light", p.guide.cards.getValue("fx.sub").option)
        val change = p.guide.history.single()
        assertEquals("fx.sub", change.card)
        assertEquals(GuideChange.INSERT, change.kind)
        assertEquals(GuideChange.GUIDE, change.origin)
        assertEquals("Sub", p.instrumentTracks.first { GuideTrack.inst(it.id) == change.track }.name)
        // « Annuler mon choix »: the sub as it was, the card to decide again, the change kept and crossed out.
        val back = GuideActions.undoChoice(p, "fx.sub", 2000L)
        assertEquals(song.strip("Sub"), back.strip("Sub"))
        assertEquals(GuideStatus.TODO, back.guide.statusOf("fx.sub"))
        assertTrue(back.guide.history.single().reverted)
        // « Ramener » one change: the same.
        assertEquals(song.strip("Sub"), GuideActions.revert(p, change.id, 2000L).strip("Sub"))
    }

    @Test
    fun keepingAndOpeningChangeNothingButTheCard() {
        val kept = GuideActions.apply(song, "fx.review", "keep", 1000L)
        assertEquals(song.mixer, kept.mixer)
        assertTrue(kept.guide.history.isEmpty())
        assertEquals("fx.review", kept.guide.step)
        assertEquals(song, GuideActions.apply(song, "fx.review", "all", 1000L))  // « Voir tout ce qui joue » only opens a screen
        assertEquals(song, GuideActions.apply(song, "nope", "keep", 1000L))
    }

    @Test
    fun theMixOptionsDoWhatTheySay() {
        // The pans: the low end in the centre, the others apart.
        val panned = GuideActions.apply(song, "mix.pans", "place", 1000L)
        assertEquals(0f, panned.strip("Basse").pan, 0f)
        assertEquals(0f, panned.strip("Sub").pan, 0f)
        assertNotEquals(0f, panned.strip("Lead").pan)
        assertTrue(GuideFacts.of(panned).holds("pans", GuideRoles.SONG))
        // The low cuts: on every track without one, never on the bass nor the sub; then the card is ticked by the analysis.
        val cut = GuideActions.apply(noCuts, "fx.lowcut", "all", 1000L)
        val facts = GuideFacts.of(cut)
        assertTrue(facts.holds("lowcut", GuideRoles.SONG))
        assertEquals(EffectType.PARAM_EQ, cut.strip("Voix").inserts.first().effect)
        assertEquals(noCuts.strip("Sub"), cut.strip("Sub"))
        // The curves of the pad's effects follow its chain moved one slot down.
        val padCh = InstrumentTrack.mixerChannel(song.instrumentTracks.first { it.name == "Nappe" }.id)
        val lane = noCuts.automation.first { it.target.ch == padCh && it.target.kind == com.pocketstudio.model.AutoTarget.INSERT_PARAM }
        val moved = cut.automation.first { it.target.ch == padCh && it.target.kind == com.pocketstudio.model.AutoTarget.INSERT_PARAM }
        assertEquals(noCuts.strip("Nappe").inserts[lane.target.slot].effect, cut.strip("Nappe").inserts[moved.target.slot].effect)
        // The ducking and the target.
        val ducked = GuideActions.apply(song, "fx.sidechain", "pump", 1000L)
        assertEquals(0.8f, ducked.mixer.musicBus!!.lowDepth, 1e-6f)
        assertEquals(LoudnessTarget.STREAMING, GuideActions.apply(song, "frame.destination", "streaming", 1000L).mixer.target)
        // The second bass gets the bass's chain.
        val copied = GuideActions.apply(GuideActions.apply(song, "fx.bass", "cleaner", 1000L), "fx.bass", "copy_bass2", 2000L)
        assertEquals(copied.strip("Basse").inserts, copied.strip("Basse 2").inserts)
    }

    @Test
    fun thePadWaitsForTheSecondHalfOfTheFirstDrop() {
        val p = GuideActions.apply(song, "arr.pad", "wait", 1000L)
        val s = GuideSong(p)
        val drop = s.sectionsOf(GuideSection.Kind.PEAK).first()
        val pad = p.guideTracks().first { it.role == GuideRoles.PAD }.key
        assertFalse(s.sounds(pad, drop.start))
        assertTrue(s.sounds(pad, drop.end - 1))
        assertTrue(GuideFacts.of(p).holds("pad_room", GuideRoles.PAD))
        assertTrue(p.guide.history.isNotEmpty() && p.guide.history.all { it.kind == GuideChange.PART && it.field.startsWith(GuideActions.MUTED) })
        assertEquals(1, p.guide.history.map { it.id }.distinct().size)
        // Undone, the song is as it was.
        assertEquals(song.song, GuideActions.undoChoice(p, "arr.pad", 2000L).song)
    }

    @Test
    fun aRefusalIsNeverOfferedAgain() {
        var p = GuideActions.skip(song, "fx.review", 1000L, forever = true)
        val r = GuideAnalyzer.analyze(p)
        assertEquals(GuideStatus.SKIPPED, r.status("fx.review")!!.status)
        assertNotEquals("fx.review", r.next!!.card.id)
        p = GuideActions.refuseOption(p, "fx.lead", "dry", 2000L)
        assertTrue(p.guide.refused("fx.lead", "dry") && !p.guide.refused("fx.lead"))
        // The choice undone forgets the refusal.
        assertFalse(GuideActions.undoChoice(p, "fx.review", 3000L).guide.refused("fx.review"))
        // A note stays.
        assertEquals("Pas de réverbe ici", GuideActions.note(p, "Pas de réverbe ici", 4000L, "inst:0").guide.notes.single().text)
    }

    /** Review G2: « Annuler mon choix » never erases what was done after on the same tracks; a setting left alone comes back. */
    @Test
    fun bringingAChoiceBackNeverErasesWhatCameAfter() {
        val song = noCuts
        assertFalse(GuideFacts.of(song).holds("lowcut", GuideRoles.SONG))
        val cut = GuideActions.apply(song, "fx.lowcut", "all", 1000L)
        val voice = GuideActions.apply(cut, "fx.vocal", "clear", 2000L)
        val chain = voice.strip("Voix").inserts
        assertEquals(listOf(EffectType.PARAM_EQ, EffectType.COMPRESSOR), chain.filter { it.effect != EffectType.NONE }.map { it.effect })
        // The voice's low cut was changed since (« Voix claire » set its own): undoing the low cuts is refused, nothing moves.
        val refused = GuideActions.tryUndoChoice(voice, "fx.lowcut", 3000L)
        assertTrue(refused.problem != null)
        assertEquals(voice, refused.project)
        assertEquals(GuideStatus.DONE, refused.project.guide.statusOf("fx.lowcut"))
        // The voice's own choice undone: its settings come back, the others' low cuts stay.
        val back = GuideActions.undoChoice(voice, "fx.vocal", 4000L)
        assertEquals(cut.strip("Voix"), back.strip("Voix"))
        assertEquals(cut.strip("Nappe"), back.strip("Nappe"))
        assertEquals(GuideStatus.DONE, back.guide.statusOf("fx.lowcut"))
        // Then the low cuts can go: every chain as the assistant left it, its curves on their slots again (review M4).
        val none = GuideActions.undoChoice(back, "fx.lowcut", 5000L)
        assertEquals(song.mixer.channels, none.mixer.channels)
        assertEquals(song.automation, none.automation)
    }

    /** Review M10: « Ramener » after another edit of the same strip: the other setting stays; the same setting changed since: refused. */
    @Test
    fun bringingBackAfterALaterEditOfTheSameStrip() {
        val p = GuideActions.apply(song, "fx.sub", "light", 1000L)
        val id = p.guide.history.single().id
        val ch = InstrumentTrack.mixerChannel(p.instrumentTracks.first { it.name == "Sub" }.id)
        val sent = p.copy(mixer = p.mixer.copy(channels = p.mixer.channels.mapIndexed { i, c -> if (i == ch) c.copy(sends = listOf(0.3f, 0f)) else c }))
        val back = GuideActions.revert(sent, id, 2000L)
        assertEquals(song.strip("Sub").inserts, back.strip("Sub").inserts)
        assertEquals(listOf(0.3f, 0f), back.strip("Sub").sends)
        val edited = sent.copy(mixer = sent.mixer.copy(channels = sent.mixer.channels.mapIndexed { i, c ->
            if (i == ch) c.copy(inserts = c.inserts.toMutableList().also { it[0] = it[0].copy(params = listOf(0.9f) + it[0].params.drop(1)) }) else c
        }))
        val refused = GuideActions.tryRevert(edited, id, 2000L)
        assertTrue(refused.problem != null)
        assertEquals(edited, refused.project)
    }

    /** Review M3: the Guide's own edits on other tracks are not « done elsewhere ». */
    @Test
    fun theGuidesOwnEditsDoNotTickOtherCards() {
        val p = GuideActions.apply(noCuts, "fx.lowcut", "all", 1000L)
        assertEquals(GuideStatus.DONE, p.guide.statusOf("fx.lowcut"))
        val r = GuideAnalyzer.analyze(p)
        for (id in listOf("fx.pad", "fx.lead", "move.throw")) assertEquals(id, GuideStatus.TODO, r.status(id)!!.status)
        // A chain changed in the mixer still is.
        val ch = InstrumentTrack.mixerChannel(p.instrumentTracks.first { it.name == "Nappe" }.id)
        val q = p.copy(mixer = p.mixer.copy(channels = p.mixer.channels.mapIndexed { i, c -> if (i == ch) c.copy(sends = listOf(0.6f, 0f)) else c }))
        assertEquals(GuideStatus.DONE_ELSEWHERE, GuideAnalyzer.analyze(q).status("fx.pad")!!.status)
    }

    /** Review M1 and M2: an alert comes back with its fact; « Remettre au centre » centres every track the fact names. */
    @Test
    fun anAlertComesBackAndCentresEveryTrackItNames() {
        val sub = InstrumentTrack.mixerChannel(song.instrumentTracks.first { it.name == "Sub" }.id)
        fun offCentre(p: Project) = p.copy(drums = p.drums.withPadPan(0, -1f), mixer = p.mixer.copy(channels = p.mixer.channels.mapIndexed { i, c -> if (i == sub) c.copy(pan = 0.8f) else c }))
        val wrong = offCentre(song)
        assertEquals("alert.low.off_center", GuideAnalyzer.analyze(wrong).next!!.card.id)
        val fixed = GuideActions.apply(wrong, "alert.low.off_center", "centre", 1000L)
        assertEquals(0f, fixed.drums.lanes[0].pan, 0f)
        assertEquals(0f, fixed.mixer.channels[sub].pan, 0f)
        assertTrue(GuideAnalyzer.analyze(fixed).alerts.isEmpty())
        // Next week, off centre again: the alert is there again.
        assertEquals("alert.low.off_center", GuideAnalyzer.analyze(offCentre(fixed)).next!!.card.id)
        // Only « C'est voulu » keeps it away.
        val wanted = GuideActions.apply(wrong, "alert.low.off_center", "refuse", 1000L)
        assertTrue(GuideAnalyzer.analyze(offCentre(wanted)).alerts.isEmpty())
    }

    /** Review M5: a chain copied past the engine's long effects is refused, said, and changes nothing. */
    @Test
    fun aCopyPastTheLongEffectsLimitIsRefused() {
        val style = StarterWizard.style("trap")!!
        var p = GuideActions.withBaseline(StarterWizard.build(style, style.lines.associate { it.role to null as PadSample? }, style.melody.mood,
            StarterWizard.defaultParts(style).associateWith { StarterWizard.soundsFor(it, style).first() }))
        var pad = 0
        while (p.mixer.longFxCount(EffectType.REVERB) < EffectType.longLimit(EffectType.REVERB)) {
            val ch = com.pocketstudio.model.Mixer.padChannel(pad++)
            p = p.copy(mixer = p.mixer.copy(channels = p.mixer.channels.mapIndexed { i, c -> if (i == ch) c.copy(inserts = listOf(com.pocketstudio.model.Insert.of(EffectType.REVERB)) + c.inserts.drop(1)) else c }))
        }
        val out = GuideActions.tryApply(p, "fx.lead", "copy_counter", 1000L)
        assertTrue(out.problem != null)
        assertEquals(p, out.project)
    }

    /** Review minor: an option with nothing to change does not tick the card. */
    @Test
    fun anOptionWithNothingToChangeIsNotADecision() {
        val style = StarterWizard.style("house")!!
        val p = StarterWizard.build(style, style.lines.associate { it.role to null as PadSample? }, style.melody.mood,
            StarterWizard.defaultParts(style).associateWith { StarterWizard.soundsFor(it, style).first() })
        // Options that change something are decisions…
        val out = GuideActions.tryApply(p, "fx.drums", "softer", 1000L).let { GuideActions.tryApply(it.project, "fx.lead", "dry", 1000L) }
        assertEquals(GuideStatus.DONE, out.project.guide.statusOf("fx.lead"))
        // …one that finds nothing to turn (no drums at all) is said, and the card stays to decide.
        val none = GuideActions.tryApply(Project(), "fx.drums", "softer", 1000L)
        assertTrue(none.problem != null)
        assertEquals(GuideStatus.TODO, none.project.guide.statusOf("fx.drums"))
    }
}
