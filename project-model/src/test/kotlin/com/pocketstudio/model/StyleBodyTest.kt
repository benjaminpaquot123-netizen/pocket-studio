package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The default body (grand remplacement v2, lot L1): a style asking for it ([StarterWizard.Style.body], or
 * its own chords doubling, texture and mix) gets a high layer on its chords in the Drop, a texture held on
 * the key, a counter-melody, a shaker, its drum bus in parallel and its pumps; a style without them is built
 * as before.
 */
class StyleBodyTest {
    private fun trap() = PlainStyles.plain("trap")

    private fun build(style: StarterWizard.Style): Project {
        val parts = StarterWizard.defaultParts(style).associateWith { StarterWizard.soundsFor(it, style).first() }
        val sounds = style.lines.associate { it.role to PadSample("${it.role.id}.wav", it.role.id) }
        return StarterWizard.build(style, sounds, style.melody.mood, parts)
    }

    private fun Project.trackNamed(name: String) = tracks.firstOrNull { it.name == name }

    @Test
    fun withoutTheNewFieldsTheStyleIsUntouched() {
        for (style in StarterWizard.STYLES.map { PlainStyles.plain(it) }) assertSame(style.id, style, StarterWizard.withBody(style))
        // A style's own body (its team converted it): the defaults go only where it said nothing.
        for (style in StarterWizard.STYLES.filter { it.body }) assertEquals(style.id, style.melody.counter ?: StarterWizard.BODY_COUNTER, StarterWizard.withBody(style).melody.counter)
        val p = build(trap())
        for (name in listOf(StarterWizard.DOUBLE_TRACK, StarterWizard.TEXTURE_TRACK)) assertEquals(name, null, p.trackNamed(name))
        assertTrue("no parallel glue", p.mixer.channels[0].inserts.none { it.type == EffectType.COMPRESSOR.ordinal && it.params.getOrNull(WizardMix.GLUE_MIX)?.let { v -> v < 1f } == true })
    }

    @Test
    fun theBodyFillsTheDefaults() {
        val s = StarterWizard.withBody(trap().copy(body = true))
        assertEquals("its own counter wins", trap().melody.counter ?: StarterWizard.BODY_COUNTER, s.melody.counter)
        assertEquals(StarterWizard.BODY_COUNTER, StarterWizard.withBody(trap().copy(body = true, melody = trap().melody.copy(counter = null))).melody.counter)
        assertEquals(StarterWizard.ChordDouble.Both, s.melody.chordDouble)
        assertEquals(StarterWizard.BODY_TEXTURE, s.melody.texture)
        assertEquals(StarterWizard.BODY_GLUE_BLEND, s.mix!!.glueBlend)
        assertTrue("a shaker", s.lines.any { it.role == StarterWizard.Role.Shaker })
        val own = StarterWizard.withBody(trap().copy(body = true, mix = StarterWizard.StyleMix(glueBlend = 0.5f)))
        assertEquals("a blend given wins", 0.5f, own.mix!!.glueBlend)
    }

    @Test
    fun chordsAreDoubled() {
        val clip = NoteClip(steps = 16, notes = listOf(Note(0, 96, 60, 1f), Note(0, 192, 64, 1f), Note(0, 96, 67, 1f)))
        assertEquals(listOf(72, 76, 79), StarterWizard.doubled(clip, StarterWizard.ChordDouble.Octave).notes.map { it.pitch })
        val top = StarterWizard.doubled(clip, StarterWizard.ChordDouble.Top).notes.single()
        assertEquals(91, top.pitch)
        assertEquals("held as the chord", 192, top.length)
        assertEquals(0.8f, top.velocity, 1e-6f)
        assertEquals(listOf(72, 76, 79, 91), StarterWizard.doubled(clip, StarterWizard.ChordDouble.Both).notes.map { it.pitch })
        val high = NoteClip(steps = 16, notes = listOf(Note(0, 96, 100, 1f)))
        assertTrue("nothing over the melodic top", StarterWizard.doubled(high, StarterWizard.ChordDouble.Both).notes.all { it.pitch <= StarterWizard.DOUBLE_TOP })
    }

    @Test
    fun theDoubleAndTheTexturePlayInTheSong() {
        val p = build(trap().copy(melody = trap().melody.copy(chordDouble = StarterWizard.ChordDouble.Both, texture = "Souffle")))
        val double = assertNotNull_(p.trackNamed(StarterWizard.DOUBLE_TRACK), "a double track")
        val texture = assertNotNull_(p.trackNamed(StarterWizard.TEXTURE_TRACK), "a texture track")
        val withDouble = p.scenes.filter { it.trackNotes[double.id]?.notes?.isNotEmpty() == true }
        assertTrue("the double plays somewhere", withDouble.isNotEmpty())
        assertTrue("not everywhere: ${p.scenes.map { it.label }}", withDouble.size < p.scenes.size)
        assertTrue("only in a drop: ${withDouble.map { it.label }}", withDouble.all { s -> DropVariation.LABELS.any { s.label.startsWith(it) } })
        val root = 48 + Math.floorMod(p.harmony.root, 12)
        val held = p.scenes.mapNotNull { it.trackNotes[texture.id] }.filter { it.notes.isNotEmpty() }
        assertTrue("the texture plays", held.isNotEmpty())
        for (c in held) assertEquals(listOf(root, root + 7), c.notes.map { it.pitch }.sorted())
    }

    @Test
    fun theStyleMixSetsTheBusAndThePumps() {
        val mix = StarterWizard.StyleMix(pump = setOf("808", "chords"), glueBlend = 0.6f, busDrive = 0.3f)
        val p = build(trap().copy(mix = mix))
        val bus = p.mixer.channels[0].inserts
        val glue = bus.first { it.type == EffectType.COMPRESSOR.ordinal }
        assertEquals(0.6f, glue.params[WizardMix.GLUE_MIX], 1e-6f)
        assertEquals(WizardMix.PARALLEL_THRESHOLD, glue.params[0], 1e-6f)
        bus.firstOrNull { it.type == EffectType.SATURATION.ordinal }?.let { assertEquals(0.3f, it.params[0], 1e-6f) }
        val lane808 = StudioDrums.LANE_808
        val plays808 = (listOf(p.drums) + p.scenes.map { it.drums }).any { it.lanes.getOrNull(lane808)?.hits?.isNotEmpty() == true }
        if (plays808) assertTrue("a pump on the 808", p.mixer.channels[Mixer.padChannel(lane808)].inserts.any { it.type == EffectType.PUMP.ordinal })
    }

    private fun <T> assertNotNull_(v: T?, why: String): T { assertNotNull(why, v); return v!! }
}
