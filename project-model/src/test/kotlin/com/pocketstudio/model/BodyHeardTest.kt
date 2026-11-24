package com.pocketstudio.model

import com.pocketstudio.model.StarterWizard.Role
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Lot gr-l1b: what a format v2 style asks for is heard in the app's song: the body's shaker, the
 * layers on their free pads, the loop on its audio track, the vocal chop, the transition's hit on
 * the crash, the chords' pump kept; the song's shape knows the new lines; the drummer's toms leave
 * the Snap's pad alone.
 */
class BodyHeardTest {
    private fun trap() = PlainStyles.plain("trap")

    private fun build(style: StarterWizard.Style, extras: StarterWizard.Extras = StarterWizard.Extras()): Project {
        val parts = StarterWizard.defaultParts(style).associateWith { StarterWizard.soundsFor(it, style).first() }
        val sounds = StarterWizard.withBody(style).lines.associate { it.role to PadSample("${it.role.id}.wav", it.role.id) }
        return StarterWizard.build(style, sounds, style.melody.mood, parts, extras = extras)
    }

    private fun Project.allDrums() = listOf(drums) + scenes.map { it.drums }
    private fun Project.plays(lane: Int) = allDrums().any { it.lanes[lane].hits.isNotEmpty() }

    @Test
    fun theBodyKeepsTheSongsShape() {
        // The body adds layers, never bars or sections: the same length, the same scenes (Crash and Fill included), and
        // every beat of silence before a Drop stays silent (gr-hiphop: reggaeton « Dark » with the bass alone went up to
        // 230 s, its counter-melody playing 2 rounds of chord stabs; drill's Couplet 3 lost its silent beat to the texture).
        val bad = mutableListOf<String>()
        // The scenes that lead into a Drop: their last beat is the silence before it.
        fun beforeDrops(p: Project): Set<Int> = p.song.zipWithNext().filter { (a, b) ->
            p.allScenes[b.scene].label.let { it == SongShape.FAKE_DROP || Regex("""^(Drop|Refrain)( \d+)?$""").matches(it) } &&
                p.allScenes[a.scene].label.let { it.startsWith("Montée") || it.startsWith(SongShape.PRE_CHORUS) || it.startsWith("Couplet ") || it == SongShape.FAKE_DROP }
        }.map { it.first.scene }.toSet()
        val silent = { p: Project -> p.allScenes.map { sc ->
            val steps = sc.stepsFor(16)
            sc.drums.lanes.all { l -> l.hits.keys.none { it >= steps - 4 } } &&
                sc.trackNotes.values.all { c -> c.notes.none { it.end > (steps - 4) * NoteClip.TICKS_PER_STEP } }
        } }
        for (plain in StarterWizard.STYLES.map { PlainStyles.plain(it) }) {
            val sounds = StarterWizard.Part.entries.associateWith { StarterWizard.soundsFor(it, plain).first() }
            val all = StarterWizard.withBody(plain.copy(body = true)).lines.associate { it.role to PadSample("${it.role.id}.wav", it.role.id) }
            for (mood in StarterWizard.Mood.entries) for (mask in 1 until 8) for (acid in listOf(true, false)) {
                if (!acid && plain.melody.acid == null) continue
                val parts = sounds.filterKeys { StarterWizard.Part.entries.indexOf(it).let { i -> mask and (1 shl i) != 0 } }
                val without = StarterWizard.build(plain, all.filterKeys { r -> plain.lines.any { it.role == r } }, mood, parts, acid = acid)
                val with = StarterWizard.build(plain.copy(body = true), all, mood, parts, acid = acid)
                val what = "${plain.id} $mood ${parts.keys} acid=$acid"
                if (without.songBars != with.songBars) bad += "$what bars ${without.songBars} -> ${with.songBars}"
                if (without.allScenes.map { it.label } != with.allScenes.map { it.label }) bad += "$what scenes ${without.allScenes.map { it.label }} -> ${with.allScenes.map { it.label }}"
                else silent(without).zip(silent(with)).withIndex().filter { (i, s) -> i in beforeDrops(with) && s.first && !s.second }
                    .forEach { (i, _) -> bad += "$what ${with.allScenes[i].label} lost its beat of silence" }
            }
        }
        assertTrue(bad.sortedBy { "lost" in it }.take(20).joinToString("\n") + "\n(${bad.size})", bad.isEmpty())
    }

    @Test
    fun theBodyShakerIsHeard() {
        val p = build(trap().copy(body = true))
        assertTrue("the shaker plays (pad 15)", p.plays(Role.Shaker.lane))
    }

    @Test
    fun theShapeKnowsTheNewLines() {
        for (role in listOf(Role.Shaker, Role.Perc2, Role.Conga, Role.Rim, Role.Snap, Role.Ride))
            assertTrue("$role enters", role in SongShape.ENTRY)
        for (role in listOf(Role.Shaker, Role.Perc2, Role.Conga, Role.Rim)) assertTrue("$role drops out", role in SongShape.MUTES)
    }

    @Test
    fun layersPlayWithTheirLine() {
        val trap = trap()
        val kick = trap.lines.first { it.role == Role.Kick }
        val style = trap.copy(lines = trap.lines.map { if (it === kick) it.copy(layers = listOf("kit:0")) else it })
        val (role, spec, lane) = StarterWizard.layerLanes(style).single()
        assertEquals(Role.Kick, role)
        assertEquals("kit:0", spec)
        assertTrue("a free pad", StarterWizard.withBody(style).lines.none { it.role.lane == lane })
        val sample = PadSample("layer.wav", "Kick +")
        val p = build(style, StarterWizard.Extras(layers = mapOf(lane to sample)))
        for (d in p.allDrums()) assertEquals("same hits as the kick", d.lanes[Role.Kick.lane].hits, d.lanes[lane].hits)
        assertTrue(p.allDrums().any { it.lanes[lane].sample == sample && it.lanes[lane].hits.isNotEmpty() })
        val under = p.mixer.channels[Mixer.padChannel(lane)].volume
        assertTrue("under the kick", under < p.mixer.channels[Mixer.padChannel(Role.Kick.lane)].volume)
    }

    @Test
    fun theLoopPlaysFromTheGroove() {
        val style = trap().copy(loop = StarterWizard.LoopSpec("p", "l.wav", bars = 2))
        val p = build(style, StarterWizard.Extras(loop = StarterWizard.LoopSound("loop.wav", "Loop", 3.0, 90.0)))
        val t = p.audioTracks.single { it.name == StarterWizard.LOOP_TRACK }
        val clip = assertNotNull_(t.clip, "a clip")
        assertEquals("loop.wav", clip.file)
        assertTrue("follows the tempo", clip.fit)
        assertEquals(2 * p.stepsPerBar, clip.loopSteps)
        val key = SongPart.audio(t.id)
        assertTrue("silent in the Intro", key in p.song.first().muted)
        assertTrue("heard afterwards", p.song.any { key !in it.muted })
    }

    @Test
    fun theVocalChopIsPlayed() {
        val chop = StarterWizard.PartSound("Chop", sampled = SampledPatch(file = "chop.wav", name = "Chop", root = 62f))
        val style = trap().copy(vocal = StarterWizard.Vocal.Stab)
        val p = build(style, StarterWizard.Extras(vocal = chop))
        val t = p.tracks.single { it.name == StarterWizard.VOCAL_TRACK }
        assertEquals("the sampler", 2, t.engine)
        assertEquals("chop.wav", t.sampled.file)
    }

    @Test
    fun theTransitionHitRidesTheCrash() {
        val style = trap().copy(body = true)
        val lane = assertNotNull_(StarterWizard.transitionLane(style), "a pad for the hit")
        val hit = PadSample("impact.wav", "Impact")
        val p = build(style, StarterWizard.Extras(transition = hit))
        assertTrue("the hit plays", p.allDrums().any { it.lanes[lane].sample == hit && it.lanes[lane].hits.isNotEmpty() })
    }

    @Test
    fun theChordsPumpIsKept() {
        val p = build(trap().copy(mix = StarterWizard.StyleMix(pump = setOf("chords"))))
        assertTrue("a pump on the chords", p.instrumentTracks.any { t ->
            p.mixer.channels[InstrumentTrack.mixerChannel(t.id)].inserts.any { it.type == EffectType.PUMP.ordinal }
        })
    }

    @Test
    fun theDrummersTomsLeaveTheSnap() {
        val p = build(trap().copy(body = true))
        val snapHits = trap().lines.any { it.role == Role.Snap }
        if (!snapHits) assertTrue("no tom on the Snap's pad", p.allDrums().all { it.lanes[Role.Snap.lane].hits.isEmpty() })
    }

    private fun <T> assertNotNull_(v: T?, why: String): T { assertNotNull(why, v); return v!! }
}
