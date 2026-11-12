package com.pocketstudio.model

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

private typealias TrialRole = StarterWizard.Role

/**
 * Tour 5 « sons-essai » (docs/sons/PLAN-SONS-MODERNES.md § 6): the same songs with other sounds, behind a switch only
 * the bench turns on (the JVM property ps.sons.essai=1). Off, every style is its very recipe and every song is the same
 * to the byte; on, jump up, tech house and techno take their trial drum packs, the bundled modern sounds and the OTT
 * on their pads and leads, nothing else changes.
 */
class SoundTrialTest {
    @After
    fun off() {
        System.clearProperty(SWITCH)
    }

    private fun on() {
        System.setProperty(SWITCH, "1")
    }

    private fun recipe(id: String) = StarterWizard.STYLES.first { it.id == id }

    /** The song the assistant makes of [s] with its built-in sounds and its advised parts. */
    private fun songHash(s: StarterWizard.Style): String {
        val p = StarterWizard.build(
            s, s.lines.associate { it.role to null }, s.melody.mood,
            StarterWizard.defaultParts(s).associateWith { StarterWizard.soundsFor(it, s).first() },
        )
        val sha = java.security.MessageDigest.getInstance("SHA-256").digest(ProjectJson.encode(p).toByteArray())
        return sha.joinToString("") { "%02x".format(it) }.take(16)
    }

    @Test
    fun offEveryStyleIsItsVeryRecipe() {
        for (s in StarterWizard.STYLES) assertSame(s.id, s, StarterWizard.style(s.id))
    }

    /** Off, the song of every style is the one the base made (hashes taken on 2cfb8a3a, before the trial). */
    @Test
    fun offEverySongIsTheSameToTheByte() {
        assertEquals(BASE_HASHES.keys, StarterWizard.STYLES.map { it.id }.toSet())
        for (s in StarterWizard.STYLES) assertEquals(s.id, BASE_HASHES.getValue(s.id), songHash(StarterWizard.style(s.id)!!))
    }

    @Test
    fun onTheThreeStylesTakeTheirTrialDrumPacks() {
        on()
        val expected = mapOf(
            "jump_up" to setOf(TrialRole.Kick, TrialRole.Snare, TrialRole.Clap, TrialRole.Hat, TrialRole.Perc),
            "tech_house" to setOf(TrialRole.Kick, TrialRole.Clap, TrialRole.Hat, TrialRole.OpenHat, TrialRole.Perc, TrialRole.Ride),
            "techno" to setOf(TrialRole.Kick, TrialRole.OpenHat, TrialRole.Clap, TrialRole.Hat, TrialRole.Perc, TrialRole.Ride),
        )
        for ((id, roles) in expected) {
            val s = StarterWizard.style(id)!!
            assertEquals(id, roles.associateWith { "essai_$id" }, s.kit)
            // A line naming its own kit voice would keep the kit even with the pack there: none does, but tech house's
            // shaker (its second percussion, the pack's percussions being one kind for both lines).
            for (line in s.lines) if (line.role in roles) assertNull("$id ${line.role}", line.voice)
        }
        // Techno keeps its studio rumble (its tuned voice, no pack).
        assertNull(StarterWizard.style("techno")!!.kit[TrialRole.Rumble])
        assertEquals(13, StarterWizard.style("tech_house")!!.lines.first { it.role == TrialRole.Perc2 }.voice)
    }

    @Test
    fun onPadsAndLeadsGetTheOtt() {
        on()
        for (id in TRIAL_STYLES) {
            val fx = StarterWizard.style(id)!!.effects
            for (target in listOf("pad", "lead")) assertTrue("$id $target", fx.any { it.target == target && it.insert == "OTT" })
            // Everything the recipe had is still there.
            assertTrue(id, fx.containsAll(recipe(id).effects))
        }
    }

    @Test
    fun onTheBundledModernSoundsPlay() {
        on()
        val jump = StarterWizard.style("jump_up")!!.melody
        assertEquals("Accords future bass", jump.pad)
        assertEquals("Supersaw", jump.lead)
        assertEquals("Basse growl", jump.bass)
        // Techno keeps its square pluck: the future pluck measured darker on its pad (−40 dB over 10 kHz against −39 for the
        // pluck, docs/audits/ultra/tour-5-sons-essai-rapport.md); the OTT is what changes there.
        assertEquals(recipe("techno").melody.pad, StarterWizard.style("techno")!!.melody.pad)
        // Each sound named is one of the library (the assistant would fall back to another one otherwise).
        for (id in TRIAL_STYLES) {
            val m = StarterWizard.style(id)!!.melody
            for (name in listOf(m.bass, m.pad, m.lead)) assertTrue("$id $name", SoundLibrary.SYNTH.any { it.name == name } || SoundLibrary.PLAITS.any { it.name == name })
        }
    }

    /** The song the trial makes of [id] (the switch on), with its built-in sounds and its advised parts. */
    private fun trialSong(id: String): Project {
        on()
        val s = StarterWizard.style(id)!!
        return StarterWizard.build(
            s, s.lines.associate { it.role to null }, s.melody.mood,
            StarterWizard.defaultParts(s).associateWith { StarterWizard.soundsFor(it, s).first() },
        )
    }

    private fun Project.strip(channel: Int) = mixer.channels[channel]

    /** Trial n° 2 (Benjamin: « tu rajoutes juste des cymbales »): the bass gets harmonics a phone plays, its sub kept clean. */
    @Test
    fun onTheBassGetsParallelSaturation() {
        for (id in TRIAL_STYLES) {
            val p = trialSong(id)
            val saturated = p.bassChannels().map { p.strip(it) }.flatMap { it.inserts }.filter { it.type == EffectType.SATURATION.ordinal }
            assertTrue("$id: a saturation on its bass", saturated.isNotEmpty())
            // Parallel (the dry sub stays under it) and its loudness held.
            assertTrue("$id: one of them parallel, its loudness held", saturated.any { it.params[2] < 1f && it.params[4] >= 0.5f })
            // Driven hard enough to bite at the bass's level (a drive of 0.55 moved its mid by less than 1 dB on the bench).
            // Driven hard and mostly wet (a drive of 0.55 at 60 % moved its mid by less than 1 dB on the bench, 0.85 at 60 %
            // by 0.4 to 2.9 dB: the « Basse audible » exciter and the held loudness take part of it back).
            assertTrue("$id: driven", saturated.any { it.params[0] >= 0.9f && it.params[2] in 0.75f..0.95f })
            // Its harmonics lifted where a phone plays them: a peak of +10 dB or more between 400 Hz and 1.2 kHz.
            val mids = p.bassChannels().map { p.strip(it) }.flatMap { it.inserts }.filter {
                it.type == EffectType.PARAM_EQ.ordinal && it.params[1] >= 0.5f + 10f / 36f && 20f * Math.pow(1000.0, it.params[0].toDouble()) in 400.0..1200.0
            }
            assertTrue("$id: a mid lift on its bass", mids.isNotEmpty())
        }
    }

    /** Trial n° 2: leads and pads wider, with a modern space (the plate reverb and the ping-pong delay). */
    @Test
    fun onLeadsAndPadsGetWidthAndSpace() {
        for (id in TRIAL_STYLES) {
            val p = trialSong(id)
            val lead = p.strip(InstrumentTrack.mixerChannel(p.leadTrackId()!!))
            assertTrue("$id lead widened", lead.inserts.any { it.type == EffectType.WIDENER.ordinal })
            assertTrue("$id lead reverb ${lead.sends}", lead.sends[0] >= 0.2f)
            assertTrue("$id lead delay ${lead.sends}", lead.sends[1] >= 0.2f)
            val pad = p.strip(InstrumentTrack.mixerChannel(p.padTrackIds().first()))
            assertTrue("$id pad widened", pad.inserts.any { it.type == EffectType.WIDENER.ordinal })
            assertTrue("$id pad reverb ${pad.sends}", pad.sends[0] >= 0.3f)
        }
    }

    /** Trial n° 2: tech house's voice (-57 dB over 10 kHz, -14 dB of presence on a phone) lifted around 2.5 kHz. */
    @Test
    fun onTheTechHouseVoiceComesForward() {
        val p = trialSong("tech_house")
        val lead = p.strip(InstrumentTrack.mixerChannel(p.leadTrackId()!!))
        val lift = lead.inserts.filter { it.type == EffectType.PARAM_EQ.ordinal && it.params[1] > 0.6f }
        assertTrue("a presence lift on the voice: ${lead.inserts}", lift.isNotEmpty())
    }

    @Test
    fun onOnlyTheThreeStylesChangeAndTheirNotesStay() {
        on()
        for (s in StarterWizard.STYLES) {
            val now = StarterWizard.style(s.id)!!
            if (s.id !in TRIAL_STYLES) {
                assertSame(s.id, s, now)
                continue
            }
            // Same lines (patterns), same melody but its three sounds, same form, tempo, sections.
            assertEquals(s.id, s.lines.map { it.role to it.pattern }, now.lines.map { it.role to it.pattern })
            assertEquals(s.id, s.melody.copy(bass = "", pad = "", lead = ""), now.melody.copy(bass = "", pad = "", lead = ""))
            assertEquals(s.id, s.copy(kit = emptyMap(), effects = emptyList(), lines = emptyList(), melody = StarterWizard.Melody()),
                now.copy(kit = emptyMap(), effects = emptyList(), lines = emptyList(), melody = StarterWizard.Melody()))
        }
    }

    private companion object {
        const val SWITCH = "ps.sons.essai"
        val TRIAL_STYLES = listOf("jump_up", "tech_house", "techno")
        val BASE_HASHES = mapOf(
            "acid_house" to "4cdca49f3bec05fd",
            "acid_techno" to "2c7aa030237d111c",
            "acidcore" to "664490c67a54e953",
            "afro_house" to "4eacee6e4459b799",
            "afrobeats" to "e298abdb819aefc7",
            "amapiano" to "c3da775f31f3f42f",
            "boom_bap" to "0fbb02ecc0a9ca85",
            "deep_house" to "8473f86ceba6acd3",
            "detroit" to "017fc15669cc4985",
            "disco" to "7213fe3cea017e57",
            "dnb" to "c984b1e7d9d9f7c5",
            "drill" to "991e8aa01391ce89",
            "dubstep" to "5f715cfafbb03142",
            "gabber" to "adbc9000857576b3",
            "grime" to "50d4aefd2c0d253f",
            "hard_techno" to "9a0839e2eaa609ff",
            "house" to "cd4d1e26c84167e5",
            "industrial_techno" to "da84e9b103ab0fc6",
            "jersey_club" to "e3626db86ef9e743",
            "jump_up" to "6cac6ed59098c282",
            "jungle" to "86ee813a2547dd4b",
            "liquid_dnb" to "db50d1b3593c1942",
            "lofi" to "48dad51ae495a4b7",
            "minimal" to "8dadcd89b6d6c74b",
            "neurofunk" to "de502c99acad4412",
            "phonk" to "52c945601fb78c33",
            "pop" to "04c87a115c9f444e",
            "reggaeton" to "c578d698bc1eab6f",
            "retro_techno" to "0596294e9d5c140c",
            "riddim" to "5f3898c50804ea66",
            "speed_garage" to "c49fc4cf47f2ff64",
            "synthwave" to "b5833f4b2d560f7c",
            "tech_house" to "0ff371786ea4b892",
            "techno" to "3c3b788b93444cce",
            "trance" to "eae2221303133270",
            "trap" to "fd78529cb412a7c6",
            "uk_garage" to "9e9844c396f49f5f",
        )
    }
}
