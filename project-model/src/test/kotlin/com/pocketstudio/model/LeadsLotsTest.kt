package com.pocketstudio.model

import com.pocketstudio.model.StarterWizard.Part
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The leads chantier, lots B to D (docs/hits/leads-pros.md § 8): the library's sounds with the
 * engine's new touch (key tracking, velocity, click, delayed vibrato), unison (JP-8000) and clean
 * ladder; the assistant's lead and pad layers; the chorus's stereo input. Projects saved before keep
 * their sound: their patches and inserts read as they were written.
 */
class LeadsLotsTest {
    private fun song(style: StarterWizard.Style): Project {
        val lines = style.lines.filter { !it.off }.associate { it.role to null as PadSample? }
        val chosen = Part.entries.associateWith { StarterWizard.soundsFor(it, style).first() }
        return StarterWizard.build(style, lines, style.melody.mood, chosen)
    }

    private fun lib(name: String) = SoundLibrary.SYNTH.first { it.name == name }.sound

    @Test
    fun theLibrarysVoicesHaveTheirTouch() {
        with(SynthPatch) {
            for (name in listOf("Supersaw", "Lead trance", "Nappe", "Nappe chaude", "Cordes")) assertEquals(name, 1f, lib(name).get(SUPERSAW))
            assertEquals(1f, lib("Pluck").get(KEY_TRACK))
            assertTrue(lib("Pluck").get(CLICK) > 0f && lib("Pluck").get(VELOCITY_ENV) > 0f && lib("Pluck").get(PITCH_ENV) > 0f)
            assertTrue(lib("Lead").get(VIBRATO_DELAY) > 0f && lib("Lead").get(VIBRATO) > 0f)
            assertTrue(lib("Hoover").get(PWM) > 0f)
            // Every lead, pad, pluck or key still on the ladder is the clean one (the acid lead keeps its grit).
            for (s in SoundLibrary.SYNTH.filter { it.category in setOf("Leads", "Nappes", "Pluck", "Claviers") && it.name != "Lead acide" }) {
                if (s.sound.get(FILTER_TYPE) < 0.25f) assertEquals(s.name, 1f, s.sound.get(CLEAN_LADDER))
            }
            // The basses and effects are as they were.
            for (s in SoundLibrary.SYNTH.filter { it.category in setOf("Basses", "Effets") }) {
                for (i in LEAD_PARAMS) assertEquals(s.name, DEFAULTS[i], s.sound.get(i))
            }
        }
    }

    @Test
    fun soundsChosenBeforeKeepTheirNames() {
        // A lead, pluck or pad as the library had it between lot 1 and these lots (a project saved then).
        for (name in listOf("Supersaw", "Pluck", "Lead", "Hoover", "Nappe", "Piano électrique")) {
            val lot1 = ProVoices.synth(SoundLibrary.SYNTH_V2.first { it.name == name }).sound
            assertEquals(name, SoundLibrary.synthNameOf(lot1))
            assertEquals(name, SoundLibrary.synthNameOf(lib(name)))
        }
    }

    @Test
    fun aPatchSavedBeforeTheseLotsPlaysAsBefore() {
        val old = SynthPatch(SynthPatch.DEFAULTS.take(SynthPatch.KEY_TRACK))
        assertEquals(SynthPatch.DEFAULTS, old.sanitized().values)
        for (i in SynthPatch.LEAD_PARAMS) assertEquals(SynthPatch.DEFAULTS[i], old.get(i))
        // A frozen track's signature reads it as written before them.
        assertEquals(old.toString(), SynthPatch.beforeLeadParams(old.sanitized()).toString())
        val touched = SynthPatch().with(SynthPatch.CLICK, 0.3f)
        assertEquals(touched, SynthPatch.beforeLeadParams(touched))
        assertEquals(0.125f, SynthPatch.fmRatioValue(1f), 1e-6f)
        assertEquals(1f, SynthPatch.fmRatioValue(7f), 1e-6f)
    }

    @Test
    fun aLeadSoundPlayingABassHasNoLeadTouch() {
        // Grime's bass is the « Lead carré »: its cut-off must not follow the note down from C4.
        val p = song(StarterWizard.style("grime")!!)
        val basses = p.bassTrackIds().map { p.track(it) }.filter { it.engine == 0 && it.instrument == null }
        assertTrue(basses.isNotEmpty())
        for (t in basses) {
            assertEquals(0f, t.synth.get(SynthPatch.KEY_TRACK))
            assertEquals(0f, t.synth.get(SynthPatch.CLICK))
            assertEquals(0f, t.synth.get(SynthPatch.VELOCITY_ENV))
        }
        val lead = lib("Lead carré")
        assertEquals(ProVoices.synth(SoundLibrary.SYNTH_V2.first { it.name == "Lead carré" }).sound, ProVoices.untouched(lead, "Lead carré"))
    }

    @Test
    fun aChorusSavedBeforeStaysMonoAndANewOneIsStereo() {
        val saved = Insert(EffectType.CHORUS.ordinal, listOf(0.3f, 0.4f, 0.4f, 0.4f)).sanitized()
        assertEquals(listOf(0.3f, 0.4f, 0.4f, 0.4f, EffectType.CHORUS_MONO), saved.params)
        assertEquals(1f, Insert.of(EffectType.CHORUS).sanitized().params[EffectType.CHORUS_INPUT])
        assertEquals(1f, WideMix.LEAD_CHORUS.sanitized().params[EffectType.CHORUS_INPUT])
        // Read again, a stereo chorus stays stereo.
        assertEquals(1f, Insert(EffectType.CHORUS.ordinal, listOf(0.3f, 0.4f, 0.4f, 0.4f, 1f)).sanitized().sanitized().params[EffectType.CHORUS_INPUT])
    }

    @Test
    fun theAssistantsLeadsAndPadsGetTheirLayers() {
        var bites = 0
        var pluckPads = 0
        var airs = 0
        for (style in StarterWizard.STYLES) {
            val p = song(style)
            for (t in p.instrumentTracks) {
                for (l in t.layers) {
                    when (l.label) {
                        "Lead carré" -> {
                            bites++
                            assertEquals(12, l.transpose)
                            assertEquals(WideMix.BITE_DB, l.gainDb, 0f)
                            assertEquals("Supersaw", SoundLibrary.synthNameOf(t.synth))
                        }
                        "Nappe" -> if (l.role == LayerRole.BODY) {
                            pluckPads++
                            assertEquals(WideMix.PLUCK_PAD_DB, l.gainDb, 0f)
                        }
                        "Air" -> {
                            airs++
                            assertEquals(1, t.engine)
                            assertEquals(WideMix.AIR_DB, l.gainDb, 0f)
                        }
                    }
                }
                assertTrue(t.layers.size <= Layers.MAX)
            }
            // The engine's key tracking replaces the recipe's: a library sound with it keeps its cut-off.
            val lead = p.leadTrackId()?.let { p.track(it) }
            if (lead != null && lead.engine == 0 && lead.instrument == null) {
                val name = SoundLibrary.synthNameOf(lead.synth)
                val libSound = SoundLibrary.SYNTH.firstOrNull { it.name == name }?.sound
                if (libSound != null && libSound.get(SynthPatch.KEY_TRACK) > 0f) {
                    assertEquals("${style.id}: $name", libSound.get(SynthPatch.CUTOFF), lead.synth.get(SynthPatch.CUTOFF), 1e-6f)
                }
            }
        }
        assertTrue("no supersaw lead with its bite", bites > 0)
        assertTrue("no pluck lead with its pad", pluckPads > 0)
        assertTrue("no FM keys with their air", airs > 0)
    }
}
