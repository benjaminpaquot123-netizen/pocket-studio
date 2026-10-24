package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

/**
 * Phase 33, honest gain staging (docs/plan/son.md step 1): the compressor's make-up is the user's
 * (or « Gain auto »), the saturation's tone opens at its top, the assistant's songs use kit v2;
 * the projects saved before keep their sound.
 */
class GainStagingTest {
    private val comp = EffectType.COMPRESSOR.ordinal
    private val sat = EffectType.SATURATION.ordinal

    @Test
    fun anOldCompressorKeepsItsMakeUpAsItsGain() {
        // Saved before phase 33: 4 settings, the engine added half the full-scale reduction.
        val old = Insert(comp, listOf(0.6f, 0.2f, 0.2f, 0.3f)).sanitized()
        assertEquals(9, old.params.size)
        val ratio = 20f.pow(0.2f)
        val makeupDb = 16f * (1f - 1f / ratio) * 0.5f  // threshold -16 dB
        assertEquals(3.61f, makeupDb, 0.01f)
        assertEquals(makeupDb / EffectType.COMP_GAIN_MAX_DB, old.params[EffectType.COMP_GAIN], 1e-6f)
        // Nothing new switched on: no auto make-up, the peaks, no side-chain filter, all compressed.
        assertEquals(listOf(0f, 0f, 0f, 1f), old.params.drop(EffectType.COMP_AUTO))
        // Once upgraded it stays as it is.
        assertEquals(old, old.sanitized())
        // The make-up shows as the compressor's gain.
        assertEquals("+3.6 dB", EffectType.COMPRESSOR.params[EffectType.COMP_GAIN].format(old.params[EffectType.COMP_GAIN]).replace(',', '.'))
        // A project file from before reads the same way.
        val p = ProjectJson.decode("""{"mixer":{"master":[{"type":$comp,"params":[0.0,1.0,0.0,0.5]}]}}""")
        val master = p.mixer.master.first()
        assertEquals(19f / 24f, master.params[EffectType.COMP_GAIN], 1e-5f)  // -40 dB, 20:1: 40 * 0.95 / 2 dB
    }

    @Test
    fun aNewCompressorAddsNothingUnderItsThreshold() {
        val fresh = Insert.of(EffectType.COMPRESSOR).sanitized()
        assertEquals(0f, fresh.params[EffectType.COMP_GAIN])
        assertEquals(1f, fresh.params[EffectType.COMP_AUTO])  // « Gain auto »: only what it takes away
        assertEquals(1f, fresh.params[EffectType.COMP_MIX])
        assertEquals(EffectType.COMPRESSOR.params.size, fresh.params.size)
    }

    @Test
    fun anOldSaturationKeepsItsToneAndLevel() {
        val old = Insert(sat, listOf(0.2f, 0.85f, 1f, 0.8f)).sanitized()
        assertEquals(listOf(0.2f, 0.85f, 1f, 0.8f, 0f), old.params)
        // Its tone at the top was an 18 kHz low-pass: kept just under the open top.
        val top = Insert(sat, listOf(0.3f, 1f, 1f, 0.7f)).sanitized()
        assertEquals(EffectType.LEGACY_TOP_TONE, top.params[EffectType.SAT_TONE])
        assertTrue(EffectType.SATURATION.params[EffectType.SAT_TONE].format(top.params[EffectType.SAT_TONE]).contains("kHz"))
        // A new one is open (no hidden low-pass), its level as before unless « Volume constant ».
        val fresh = Insert.of(EffectType.SATURATION).sanitized()
        assertEquals(EffectType.TONE_OPEN, fresh.params[EffectType.SAT_TONE])
        assertEquals(0f, fresh.params[EffectType.SAT_COMPENSATION])
        assertFalse(EffectType.SATURATION.params[EffectType.SAT_TONE].format(fresh.params[EffectType.SAT_TONE]).contains("Hz"))
        assertEquals(fresh, fresh.sanitized())
    }

    @Test
    fun otherEffectsAreUntouched() {
        val eq = Insert(EffectType.EQ.ordinal, listOf(0.5f, 0.6f, 0.7f, 0f)).sanitized()
        assertEquals(listOf(0.5f, 0.6f, 0.7f, 0f), eq.params)
        val pump = Insert(EffectType.PUMP.ordinal, listOf(0.45f, 0.35f, 0f)).sanitized()
        assertEquals(EffectType.PUMP.defaults.size, pump.params.size)
    }

    @Test
    fun theAssistantsGlueIsFlatUnderTheThreshold() {
        // The same numbers as the engine test glue_chains_flat_under_threshold.
        for (chain in listOf(WizardMix.BUS_GLUE, WizardMix.MASTER_GLUE)) {
            val c = chain.first { it.type == comp }.sanitized()
            assertEquals(0f, c.params[EffectType.COMP_GAIN])
            assertEquals(0f, c.params[EffectType.COMP_AUTO])
            assertEquals(0.5f, c.params[EffectType.COMP_HIGH_PASS])  // the kick does not drive it (~90 Hz)
            val s = chain.first { it.type == sat }.sanitized()
            assertEquals(EffectType.TONE_OPEN, s.params[EffectType.SAT_TONE])
            assertEquals(1f, s.params[EffectType.SAT_COMPENSATION])
            assertEquals(0.7f, s.params[3])  // 0 dB
        }
    }

    @Test
    fun theAssistantsSongsUseKitThreeAndOldProjectsKitOne() {
        assertEquals(Project.KIT_V1, ProjectJson.decode("""{"name":"Ancien"}""").kitVersion)
        assertFalse(ProjectJson.encode(Project()).contains("kitVersion"))
        for (style in StarterWizard.STYLES) {
            val lines = style.lines.filter { !it.off }.associate { it.role to null as PadSample? }
            val parts = StarterWizard.defaultParts(style).associateWith { StarterWizard.soundsFor(it, style).first() }
            val p = StarterWizard.build(style, lines, style.melody.mood, parts)
            // Kit v3 since chantier S3 (its voices at kit v2's levels for their roles), with its studio voices.
            assertEquals(style.id, Project.KIT_V3, p.kitVersion)
            val back = ProjectJson.decode(ProjectJson.encode(p))
            assertEquals(style.id, Project.KIT_V3, back.kitVersion)
            assertEquals(style.id, p.padVoices, back.padVoices)
            // No 8 kHz shelf on a pad to lift the hats, no glue compressor with a hidden boost.
            for (lane in 0 until DrumPattern.PAD_COUNT) {
                assertTrue(style.id, p.mixer.channels[Mixer.padChannel(lane)].inserts.none { it.type == EffectType.EQ.ordinal })
            }
            val glue = p.mixer.channels[0].inserts + p.mixer.master
            assertTrue(style.id, glue.filter { it.type == comp }.all { it.params[EffectType.COMP_GAIN] == 0f })
        }
    }
}
