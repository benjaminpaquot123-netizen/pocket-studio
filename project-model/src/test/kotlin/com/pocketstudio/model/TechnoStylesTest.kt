package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow

private typealias TR = StarterWizard.Role
private typealias TP = StarterWizard.Part

/**
 * The Techno family's recipes (scripts/packs/recipes/techno*.json): each style its own beat and
 * sounds, the 303 alone in acid techno, a bass that stays a bass, the gabber's kick in the key,
 * the trance's break and pump.
 */
class TechnoStylesTest {
    private val techno = StarterWizard.STYLES.filter { it.family == "Techno" }

    private fun style(id: String) = StarterWizard.style(id)!!

    /** The song with the recipe's own choices: every drum line, the default parts, the default mood. */
    private fun defaultSong(style: StarterWizard.Style): Project {
        val lines = style.lines.associate { it.role to null as PadSample? }
        val parts = StarterWizard.defaultParts(style).associateWith { StarterWizard.soundsFor(it, style).first() }
        return StarterWizard.build(style, lines, style.melody.mood, parts)
    }

    @Test
    fun everyTechnoStyleHasItsOwnBeatAndSounds() {
        assertEquals(10, techno.size)
        val beats = techno.map { s -> s.lines.map { Triple(it.role, it.pattern, it.voice) }.toSet() to s.swing }
        assertEquals("two techno styles share their beat", beats.size, beats.toSet().size)
        val sounds = techno.map { s -> listOf(s.melody.bass, s.melody.pad, s.melody.lead) + s.melody.packSounds.values }
        assertEquals("two techno styles share their sounds", sounds.size, sounds.toSet().size)
        // Each one also has something of its own on top of the beat: effects, a signature or a 303.
        for (s in techno) assertTrue(s.name, s.effects.isNotEmpty() || s.melody.signature != null || s.melody.acid != null)
    }

    @Test
    fun theClosedHatNeverHitsUnderTheOpenHat() {
        for (s in techno) {
            val open = s.lines.firstOrNull { it.role == TR.OpenHat } ?: continue
            val closed = s.lines.firstOrNull { it.role == TR.Hat } ?: continue
            val steps = maxOf(open.pattern.length, closed.pattern.length)
            val both = StarterWizard.hits(open, steps).keys intersect StarterWizard.hits(closed, steps).keys
            assertTrue("${s.name}: closed and open hat together on $both", both.isEmpty())
        }
    }

    @Test
    fun theAcidTechnoIsThe303Alone() {
        val s = style("acid_techno")
        assertNotNull(s.melody.acid)
        assertEquals(emptySet<TP>(), StarterWizard.defaultParts(s))
        // No percussion either: the 303 is alone between the 909 hits, and it carries the Break.
        assertFalse(s.lines.any { it.role == TR.Perc })
        val plan = StarterWizard.planFor(s, s.lines.map { it.role }.toSet(), emptySet(), acid = true)
        assertEquals(setOf(1, 2, 3), plan.acid)
        // Its strip is treated (crunch and delay).
        val p = defaultSong(s)
        assertTrue(p.mixer.channels[1] != Channel().sanitized())
        // The acid line stays on the key's root: the bass under it never rubs a semitone against its notes.
        val acid = s.melody.acid!!
        for (mood in StarterWizard.Mood.entries) {
            val harmony = StarterWizard.harmonyOf(s, mood)
            val line = acidLine(harmony, acid.pattern, acid.accents, acid.slides)
            val acidNotes = line.steps.take(16).filter { it.gate }.map { Math.floorMod(harmony.root + it.note, 12) }.toSet()
            val bass = StarterWizard.startingNotes(TP.Bass, s, mood, harmony).notes.map { Math.floorMod(it.pitch, 12) }.toSet()
            for (b in bass) for (a in acidNotes) {
                val d = Math.floorMod(a - b, 12)
                assertTrue("${mood.id}: bass ${b} against the 303's $a", d != 1 && d != 11)
            }
        }
    }

    @Test
    fun theBassStaysABassInEveryTechnoStyle() {
        for (s in techno) for (mood in StarterWizard.Mood.entries) {
            val pitches = StarterWizard.startingNotes(TP.Bass, s, mood, StarterWizard.harmonyOf(s, mood)).notes.map { it.pitch }
            // From C1 (33 Hz, no inaudible sub) to E3 (165 Hz): above that it is no longer a bass.
            assertTrue("${s.name} ${mood.id}: $pitches", pitches.all { it in 24..52 })
        }
        // Detroit's octave jumps start an octave down: A1 (55 Hz) to A2.
        val detroit = style("detroit")
        val pitches = StarterWizard.startingNotes(TP.Bass, detroit, StarterWizard.Mood.Dark, StarterWizard.harmonyOf(detroit, StarterWizard.Mood.Dark)).notes.map { it.pitch }
        assertEquals(33, pitches.first())
        assertEquals(45, pitches.max())
    }

    @Test
    fun theGabberKickIsInTheKeyAndDistorted() {
        val s = style("gabber")
        // The built-in kick rings at 47 Hz (DrumKit.cpp): the key's root in octave 1 (F#1) is within half a semitone.
        val rootHz = 440.0 * 2.0.pow((24 + s.melody.root - 69) / 12.0)
        assertTrue("kick 47 Hz vs root $rootHz Hz", abs(12 * ln(47.0 / rootHz) / ln(2.0)) < 0.5)
        assertFalse(TP.Bass in StarterWizard.defaultParts(s))
        val p = defaultSong(s)
        assertEquals(FxPresets.byId("distortion")!!.macros, p.mixer.channels[Mixer.padChannel(TR.Kick.lane)].macros)
    }

    @Test
    fun theTranceHasABreakAndPumps() {
        val s = style("trance")
        val p = defaultSong(s)
        assertEquals(listOf("A · Intro", "B · Groove", "C · Drop", "D · Break"), (0..3).map { p.sceneTitle(it) })
        val breakScene = p.allScenes[3]
        assertTrue(breakScene.drums.lanes[TR.Kick.lane].hits.isEmpty())
        val (bass, pad, lead) = p.instrumentTracks
        // Its supersaw chords hit in short stabs: the track says so.
        assertEquals(listOf("Basse", "Stabs", "Lead"), listOf(bass.name, pad.name, lead.name))
        assertTrue(breakScene.synthNotes.notes.isEmpty())
        assertTrue(breakScene.trackNotes.getValue(pad.id).notes.isNotEmpty() && breakScene.trackNotes.getValue(lead.id).notes.isNotEmpty())
        // S5: the bass and the stabs pump once, together, in the Musique bus under the kick's sound (no pump of their own).
        val pump = EffectType.PUMP.ordinal
        val bus = p.mixer.musicBus!!
        for (t in listOf(bass, pad)) {
            val ch = InstrumentTrack.mixerChannel(t.id)
            assertTrue(t.name, ch in bus.members && p.mixer.channels[ch].inserts.none { it.type == pump })
        }
        assertTrue(bus.depth >= 0.2f && bus.lowDepth >= bus.depth)
    }
}
