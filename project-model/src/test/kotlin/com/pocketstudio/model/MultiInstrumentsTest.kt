package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Phase 38: the multizone instruments' manifest, their tracks, the assistant's use of them, and the 808 track. */
class MultiInstrumentsTest {
    /** A pack.json as scripts/packs/instrument_pack.py writes it (two zones, a round robin, a loop, a release sample). */
    private val packJson = """
        {"id": "instrument_piano_salamander", "name": "Piano à queue", "family": "Instruments", "bpm": 120, "version": 1,
         "sounds": [{"file": "apercu.flac", "kind": "keys", "name": "Piano à queue", "tags": ["piano"]}],
         "instrument": {"category": "piano", "release": 0.6, "attack": 0.001, "velTrack": 0.45, "gainDb": -3.8, "releaseDecay": 2.0,
           "fallback": "Piano électrique", "memoryBytes": 57600000, "attribution": "Salamander Grand Piano V3, Alexander Holm, CC BY 3.0",
           "zones": [
             {"file": "z001.flac", "lokey": 21, "hikey": 22, "root": 21.0, "lovel": 1, "hivel": 43, "seq": 1, "seqLength": 2, "release": false, "loopStart": 0, "loopEnd": 0},
             {"file": "z002.flac", "lokey": 21, "hikey": 22, "root": 21.0, "lovel": 1, "hivel": 43, "seq": 2, "seqLength": 2, "release": false, "loopStart": 48000, "loopEnd": 96000},
             {"file": "../evil.flac", "lokey": 0, "hikey": 127},
             {"file": "rel1.flac", "lokey": 21, "hikey": 21, "root": 21, "release": true, "hivel": 300}
           ]},
         "credits": [{"title": "Salamander Grand Piano V3", "licence": "CC-BY-3.0"}]}
    """.trimIndent()

    @Test
    fun theManifestKeepsItsZonesAndOnlySafeFiles() {
        val m = SoundPacks.parseManifest(packJson)
        val inst = assertNotNull(m.instrument).let { m.instrument!! }
        assertEquals(listOf("z001.flac", "z002.flac", "rel1.flac"), inst.zones.map { it.file })
        assertEquals(setOf("apercu.flac", "z001.flac", "z002.flac", "rel1.flac"), m.files)
        // Out-of-range values come back in range (a velocity of 300).
        assertEquals(127, inst.zones[2].hivel)
        assertTrue(inst.zones[2].release)
        assertEquals("Salamander Grand Piano V3, Alexander Holm, CC BY 3.0", inst.attribution)
        // A pack of sounds has no instrument (and keeps only its sounds).
        val plain = SoundPacks.parseManifest("""{"id": "techno", "name": "Techno", "sounds": [{"file": "kick_a_01.flac", "kind": "kick", "name": "Kick 1"}]}""")
        assertNull(plain.instrument)
        assertEquals(setOf("kick_a_01.flac"), plain.files)
        // An instrument without a zone is no instrument.
        val empty = SoundPacks.parseManifest(packJson.replace(Regex(""""zones": \[[\s\S]*?\]}"""), """"zones": []}"""))
        assertNull(empty.instrument)
    }

    @Test
    fun theEngineGetsItsSettingsInMultiSamplerOrder() {
        val inst = SoundPacks.parseManifest(packJson).instrument!!
        // MultiSampler::Param: Count, Release, Attack, VelTrack, Gain (linear), ReleaseDecay.
        val p = inst.engineParams()
        assertEquals(6, p.size)
        assertEquals(3f, p[0])
        assertEquals(0.6f, p[1], 1e-6f)
        assertEquals(0.45f, p[3], 1e-6f)
        assertEquals(Math.pow(10.0, -3.8 / 20).toFloat(), p[4], 1e-4f)
        assertEquals(2f, p[5])
        // MultiSampler::ZoneParam: LoKey, HiKey, LoVel, HiVel, Root, ZoneGain, SeqPosition, SeqLength, Trigger, LoopStart, LoopEnd.
        assertEquals(listOf(21f, 22f, 1f, 43f, 21f, 0f, 2f, 2f, 0f, 48000f, 96000f), inst.zones[1].engineValues())
        assertEquals(1f, inst.zones[2].engineValues()[8])
    }

    @Test
    fun anInstrumentsPreviewMustBeSafe() {
        val ok = PackEntry("instrument_harpe", "Harpe", "instrument_harpe.zip", "a".repeat(64), 3_800_000, 1, 1, "sons-packs-6", "Instruments",
            preview = "instrument_harpe-apercu.flac", previewSha256 = "b".repeat(64), previewBytes = 400_000)
        assertTrue(ok.isSafe)
        assertTrue(MultiInstruments.isInstrument(ok))
        assertFalse(ok.copy(preview = "../x.flac").isSafe)
        assertFalse(ok.copy(previewSha256 = "nope").isSafe)
        assertFalse(ok.copy(previewBytes = 50_000_000).isSafe)
        assertTrue(ok.copy(preview = "", previewSha256 = "", previewBytes = 0).isSafe)
        // The index keeps it (and an unsafe one is left out).
        val index = SoundPacks.parseIndex("""{"packs": [
            {"id": "instrument_harpe", "name": "Harpe", "file": "instrument_harpe.zip", "sha256": "${"a".repeat(64)}", "sizeBytes": 3800000, "family": "Instruments",
             "preview": "instrument_harpe-apercu.flac", "previewSha256": "${"b".repeat(64)}", "previewBytes": 400000},
            {"id": "x", "name": "X", "file": "x.zip", "sha256": "${"a".repeat(64)}", "sizeBytes": 10, "preview": "../../etc", "previewSha256": "${"b".repeat(64)}", "previewBytes": 1}
        ]}""")
        assertEquals(listOf("instrument_harpe"), index.packs.map { it.id })
    }

    @Test
    fun aTrackKeepsItsInstrumentAndOldFilesLoadUnchanged() {
        val p = Project().addTrack("Piano")!!
        val id = p.tracks.last().id
        val withPiano = p.withTrack(p.track(id).copy(engine = InstrumentTrack.ENGINE_MULTI, multi = MultiPatch(MultiInstruments.PIANO_SALAMANDER, "Piano à queue")))
        val back = ProjectJson.decode(ProjectJson.encode(withPiano))
        assertEquals(InstrumentTrack.ENGINE_MULTI, back.track(id).engine)
        assertEquals(MultiInstruments.PIANO_SALAMANDER, back.track(id).multi.pack)
        assertEquals(setOf(MultiInstruments.PIANO_SALAMANDER), back.multiPacks)
        assertEquals("Piano à queue", back.track(id).soundLabel)
        // The first track too (its sound lives in the project's own fields).
        val first = Project().withTrack(Project().track(0).copy(engine = InstrumentTrack.ENGINE_MULTI, multi = MultiPatch(MultiInstruments.STRINGS, "Cordes")))
        assertEquals(MultiInstruments.STRINGS, ProjectJson.decode(ProjectJson.encode(first)).track(0).multi.pack)
        // A project without any writes no new field: files saved before and after look the same.
        val plain = ProjectJson.encode(Project().addTrack()!!)
        assertFalse(plain.contains("\"multi\""))
        assertFalse(plain.contains("\"voice808\""))
        // An unsafe pack id is dropped (the track then plays its synth).
        val evil = withPiano.withTrack(withPiano.track(id).copy(multi = MultiPatch("../../x", "x"))).sanitized()
        assertNull(evil.track(id).multi.pack)
        assertTrue(evil.multiPacks.isEmpty())
    }

    private fun installedPiano(id: String = MultiInstruments.PIANO_SALAMANDER) =
        SoundPacks.parseManifest(packJson.replace("instrument_piano_salamander", id))

    @Test
    fun theAssistantUsesAnInstrumentOnlyOnceDownloaded() {
        val lofi = StarterWizard.style("lofi")!!
        val pad = StarterWizard.Part.Pad
        // Not on the phone: the style's usual sound, the song is made as before.
        assertNull(StarterWizard.soundsFor(pad, lofi).first().multi)
        assertTrue(StarterWizard.soundsFor(pad, lofi).none { it.multi != null })
        // Downloaded: the lo-fi's chords on the grand piano, its fallback synth sound underneath.
        val first = StarterWizard.soundsFor(pad, lofi, listOf(installedPiano())).first()
        assertEquals(MultiInstruments.PIANO_SALAMANDER, first.multi?.pack)
        assertEquals(SoundLibrary.SYNTH.first { it.name == "Piano électrique" }.sound, first.synth)
        // The instrument's pack never offers its preview as a sound played across the keys.
        assertTrue(StarterWizard.soundsFor(pad, lofi, listOf(installedPiano())).none { it.packFile?.first == MultiInstruments.PIANO_SALAMANDER })
        // No orchestra under the bass; another style offers it after its own sounds.
        assertTrue(StarterWizard.soundsFor(StarterWizard.Part.Bass, lofi, listOf(installedPiano())).none { it.multi != null })
        val techno = StarterWizard.style("techno")!!
        val technoPad = StarterWizard.soundsFor(pad, techno, listOf(installedPiano()))
        assertNull(technoPad.first().multi)
        assertTrue(technoPad.any { it.multi != null })
        // The song: its chord track plays the piano (engine 3), its synth sound the fallback.
        val song = StarterWizard.build(lofi, emptyMap(), lofi.melody.mood, mapOf(pad to first))
        val track = song.instrumentTracks.first { it.multi.pack != null }
        assertEquals(InstrumentTrack.ENGINE_MULTI, track.engine)
        assertEquals(first.synth.with(SynthPatch.GAIN, 0f), track.synth.with(SynthPatch.GAIN, 0f))
        assertEquals(setOf(MultiInstruments.PIANO_SALAMANDER), song.multiPacks)
    }

    @Test
    fun everyKnownInstrumentHasAFallbackSynthSoundAndAFamily() {
        MultiInstruments.KNOWN.forEach { e ->
            assertTrue(e.fallback, SoundLibrary.SYNTH.any { it.name == e.fallback })
            assertTrue(e.category, e.category in MultiInstruments.CATEGORY_LABELS)
        }
        // The styles that use one name a known instrument and a part it suits.
        listOf("lofi", "boom_bap", "pop").forEach { style ->
            val pack = MultiInstruments.forStyle(style, StarterWizard.Part.Pad)
            val e = MultiInstruments.entry(pack)
            assertNotNull(style, e)
            assertTrue(MultiInstruments.suits(e!!.category, StarterWizard.Part.Pad))
        }
    }

    @Test
    fun the808TrackGlidesAndNeedsNothingToDownload() {
        val p = Project()
        val next = NewTracks.add(p, TrackKind.Bass808, StarterWizard.style("trap"))!!
        val t = next.tracks.first { n -> p.tracks.none { it.id == n.id } }
        assertEquals("808", t.name)
        assertEquals(InstrumentTrack.ENGINE_808, t.engine)
        assertEquals(NewTracks.GLIDE_808, t.voice808.param(PadVoice.GLIDE)!!, 1e-6f)
        assertEquals(PadVoice.BASS_808, t.voice808.kind)
        assertNull(t.sampled.file)
        assertTrue(next.multiPacks.isEmpty())
        assertEquals("808", t.soundLabel)
        // Saved and read back the same.
        assertEquals(t, ProjectJson.decode(ProjectJson.encode(next)).track(t.id))
        // A bass part: as loud as the assistant's basses.
        assertTrue(TrackRoles.isBassName(t.name))
    }
}
