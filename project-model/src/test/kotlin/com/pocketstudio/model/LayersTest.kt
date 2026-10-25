package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** G1b: « une piste = une pile de couches » (Track.layers), the model and its API for the Guide and G3. */
class LayersTest {
    private val sub = LayerSound.bass808()
    private val air = LayerSound.synth(SynthPatch())

    private fun withTrack(): Project = Project().addTrack("Basse")!!

    @Test
    fun aProjectWithoutLayersIsSavedAsBefore() {
        val p = withTrack()
        val text = ProjectJson.encode(p)
        assertFalse(text, "layers" in text || "synthLayers" in text)
        assertEquals(p, ProjectJson.decode(text))
        // A file of before (no such key) loads with no layer.
        val old = ProjectJson.decode("""{"name":"Ancien","tracks":[{"id":1,"name":"Basse"}]}""")
        assertTrue(old.track(1).layers.isEmpty() && old.track(0).layers.isEmpty())
    }

    @Test
    fun layersRoundTripOnEveryTrackTheFirstIncluded() {
        var p = withTrack()
        p = p.addLayer(1, sub, LayerRole.SUB)!!
        p = p.addLayer(1, air, LayerRole.AIR, label = "Brillance")!!
        p = p.addLayer(0, LayerSound.plaits(PlaitsPatch()), LayerRole.ATTACK)!!
        p = p.setLayer(1, 0, gainDb = -3.5f, pan = -0.2f, low = 24, high = 59, transpose = -12, fine = 7f, mute = true)
        p = p.editLayer(1, 1) { it.copy(inserts = listOf(Insert.of(EffectType.SATURATION)), sends = listOf(0.3f, 0.1f)) }
        val back = ProjectJson.decode(ProjectJson.encode(p))
        assertEquals(p, back)
        assertEquals(2, back.layersOf(1).size)
        assertEquals(1, back.synthLayers.size)
        assertEquals(back.synthLayers, back.track(0).layers)
        val l = back.layersOf(1)[0]
        assertEquals(listOf(-3.5f, -0.2f, -12f, 7f), listOf(l.gainDb, l.pan, l.transpose.toFloat(), l.fine))
        assertEquals(24..59, l.low..l.high)
        assertTrue(l.mute && !l.throughTrackFx && l.role == LayerRole.SUB)
        assertEquals("Brillance", back.layersOf(1)[1].title)
        assertEquals(EffectType.SATURATION, back.layersOf(1)[1].inserts.single().effect)
        // The view of a track (screens made for the first one) carries its layers.
        assertEquals(back.layersOf(1), back.viewOfTrack(1).synthLayers)
    }

    @Test
    fun aTrackTakesThreeLayersOverItsOwnSound() {
        var p = withTrack()
        repeat(Layers.MAX) { p = p.addLayer(1, air)!! }
        assertNull(p.addLayer(1, air))
        assertNull(p.addLayer(5, air))  // no such track
        assertEquals(Layers.MAX, p.layersOf(1).size)
        // Removed: the next ones move up; out of range: nothing changes.
        val labelled = p.editLayer(1, 2) { it.copy(label = "Dernière") }
        val removed = labelled.removeLayer(1, 0)
        assertEquals(2, removed.layersOf(1).size)
        assertEquals("Dernière", removed.layersOf(1)[1].label)
        assertEquals(removed, removed.removeLayer(1, 7))
        assertEquals("Dernière", labelled.moveLayer(1, 2, 0).layersOf(1)[0].label)
        // A duplicated track keeps its layers; a removed one takes them along.
        val dup = p.duplicateTrack(1)!!
        assertEquals(p.layersOf(1), dup.layersOf(dup.tracks.last().id))
        assertTrue(p.removeTrack(1).layersOf(1).isEmpty())
    }

    @Test
    fun sanitizeKeepsEveryValueInRange() {
        val wild = TrackLayer(
            sound = LayerSound(engine = 42), label = "x".repeat(80), role = "r".repeat(40), gainDb = Float.NaN, pan = 5f, low = 90, high = 10,
            transpose = 99, fine = -300f,
            inserts = listOf(Insert.of(EffectType.COMPRESSOR), Insert.of(EffectType.REVERB), Insert.of(EffectType.ECHO), Insert(), Insert.of(EffectType.CHORUS),
                Insert.of(EffectType.FILTER).copy(scenes = setOf(1)), Insert.of(EffectType.SATURATION), Insert.of(EffectType.EQ)),
            sends = listOf(2f, Float.NaN, 0.5f),
        )
        val s = wild.sanitized()
        assertEquals(InstrumentTrack.ENGINE_808, s.sound.engine)
        assertEquals(InstrumentTrack.MAX_NAME, s.label.length)
        assertEquals(0f, s.gainDb, 0f)
        assertEquals(1f, s.pan, 0f)
        assertEquals(90, s.low)
        assertEquals(90, s.high)
        assertEquals(Layers.MAX_TRANSPOSE, s.transpose)
        assertEquals(-100f, s.fine, 0f)
        // Four effects at most, none of the long ones (the project's pool), no scenes of their own.
        assertEquals(listOf(EffectType.COMPRESSOR, EffectType.CHORUS, EffectType.FILTER, EffectType.SATURATION), s.inserts.map { it.effect })
        assertTrue(s.inserts.all { it.scenes == null })
        assertEquals(listOf(1f, 0f), s.sends)
        assertEquals(-40f, TrackLayer(gainDb = -500f).sanitized().gainDb, 0f)
        assertEquals(12f, TrackLayer(gainDb = 99f).sanitized().gainDb, 0f)
        // A project saved with too many layers keeps three, each sanitized.
        val saved = wild.copy(gainDb = 500f, sends = listOf(2f, -1f, 0.5f))
        val p = withTrack().let { it.withTrack(it.track(1).copy(layers = List(6) { saved })) }
        val back = ProjectJson.decode(ProjectJson.encode(p))
        assertEquals(Layers.MAX, back.layersOf(1).size)
        assertTrue(back.layersOf(1).all { it == saved.sanitized() })
        assertEquals(Layers.MAX_GAIN_DB, back.layersOf(1)[0].gainDb, 0f)
    }

    @Test
    fun gainsRangesAndListening() {
        assertEquals(1f, Layers.linear(0f), 1e-6f)
        assertEquals(0f, Layers.linear(Layers.MIN_GAIN_DB), 0f)
        assertEquals(4f, Layers.linear(12f), 0.02f)
        assertEquals(-6f, Layers.db(Layers.linear(-6f)), 1e-4f)
        val l = TrackLayer(low = 36, high = 60)
        assertTrue(l.plays(36) && l.plays(60) && !l.plays(61) && !l.plays(35))
        // Bit 0: the track's own sound, bit 1 + k: layer k.
        assertEquals(1, Layers.only(Layers.OWN))
        assertEquals(4, Layers.only(1))
        assertEquals(0xF and 4.inv(), Layers.without(1))
        assertEquals(0xE, Layers.without(Layers.OWN))
        // The engine's numbers: sounds after the 8 tracks' own, strips after the project's 36.
        assertEquals(8, Layers.soundIndex(0, 0))
        assertEquals(8 + 7 * 3 + 2, Layers.soundIndex(7, 2))
        assertEquals(36, Layers.channel(0, 0))
        assertEquals(59, Layers.channel(7, 2))
        assertEquals(Mixer.CHANNELS, Layers.FIRST_CHANNEL)
        // The roles' usual settings.
        assertFalse(LayerRole.layer(sub, LayerRole.SUB).throughTrackFx)
        assertEquals(12, LayerRole.layer(air, LayerRole.AIR).transpose)
        assertEquals("", LayerRole.layer(air, "inconnu").role)
    }

    @Test
    fun layerSoundsCountAsTheProjectsSounds() {
        var p = withTrack()
        p = p.addLayer(1, LayerSound.sample(SampledPatch(file = "couche.wav", name = "Couche")))!!
        p = p.addLayer(0, LayerSound.multi(MultiPatch(pack = "piano", name = "Piano")).copy(engine = InstrumentTrack.ENGINE_MULTI))!!
        assertTrue("couche.wav" in p.sampleFiles)
        assertTrue("piano" in p.multiPacks)
        // A sound missing from an imported .zip: the layer keeps its place, without its file.
        val cleaned = p.withoutSounds(setOf("couche.wav"))
        assertNull(cleaned.layersOf(1).single().sound.sampled.file)
    }

    @Test
    fun aFrozenTrackWithoutLayersStaysUpToDate() {
        val p = withTrack()
        val before = Freeze.signature(p, 1)
        // Its signature is the one it had before layers existed (no « [] » in it).
        assertEquals(before, Freeze.signature(p.copy(), 1))
        val layered = p.addLayer(1, air)!!
        assertTrue(Freeze.signature(layered, 1) != before)
        assertTrue(Freeze.signature(layered.setLayer(1, 0, gainDb = -2f), 1) != Freeze.signature(layered, 1))
    }

    @Test
    fun whatActsListsTheLayersAfterTheirTracksChain() {
        var p = withTrack()
        val ch = InstrumentTrack.mixerChannel(1)
        p = p.copy(mixer = p.mixer.copy(channels = p.mixer.channels.mapIndexed { i, c -> if (i == ch) c.copy(inserts = listOf(Insert.of(EffectType.CHORUS)) + c.inserts.drop(1)) else c }))
        p = p.addLayer(1, sub, LayerRole.SUB)!!.addLayer(1, air, LayerRole.AIR)!!
        val acts = p.whatActs().filter { it.ch == ch }
        assertTrue(acts.first() is Acting.Effect)
        val layers = acts.filterIsInstance<Acting.Layered>()
        assertEquals(listOf(0, 1), layers.map { it.index })
        assertTrue(layers.all { it.track == 1 })
        assertEquals(LayerRole.SUB, layers[0].layer.role)
        assertTrue(Project().whatActs().none { it is Acting.Layered })
    }

    @Test
    fun aLayersNameAndRoleLeaveAFrozenTrackUpToDate() {
        val p = withTrack().addLayer(1, air, LayerRole.AIR)!!
        assertEquals(Freeze.signature(p, 1), Freeze.signature(p.editLayer(1, 0) { it.copy(label = "Brillance", role = LayerRole.BODY) }, 1))
        assertTrue(Freeze.signature(p, 1) != Freeze.signature(p.setLayer(1, 0, transpose = 7), 1))
        assertEquals(6, Layers.MAX_VOICES)
    }

    @Test
    fun aLayersSampleSitsInTheOctaveOfTheNotesItPlays() {
        var p = withTrack()
        p = p.withTrack(p.track(1).copy(notes = NoteClip(notes = listOf(Note(0, 24, 40), Note(24, 24, 43), Note(48, 24, 45)))))
        // A sample recorded at C5 (72) on a layer transposed an octave up: its notes are 52..57.
        p = p.addLayer(1, LayerSound.sample(SampledPatch(file = "a.wav", name = "A", root = 72f)))!!.setLayer(1, 0, transpose = 12)
        val fitted = ProjectJson.decode(ProjectJson.encode(p)).layersOf(1).single().sound.sampled.root
        assertEquals(60f, fitted, 0f)  // an octave down, the semitone kept
    }

    // --- Bass music: the sub track as the bass's « Sub » layer ----------------------------------

    private fun bassMusic(id: String): Project {
        val style = StarterWizard.style(id)!!
        return StarterWizard.build(style, style.lines.associate { it.role to null as PadSample? }, style.melody.mood,
            StarterWizard.Part.entries.associateWith { StarterWizard.soundsFor(it, style).first() })
    }

    @Test
    fun theSubBecomesALayerOfItsBassWhenNothingTellsThemApart() {
        val styles = StarterWizard.STYLES.filter { it.melody.subBass }
        assertTrue(styles.isNotEmpty())
        var folded = 0
        for (style in styles) {
            val p = bassMusic(style.id)
            val sub = p.instrumentTracks.first { it.name == StarterWizard.SUB_TRACK }
            val bass = SubLayers.bassOf(p, sub.id)
            assertNotNull(style.id, bass)
            val one = SubLayers.apply(p)
            println("${style.id}: ${p.subLayerProblem(bass!!, sub.id) ?: "folded"}")
            if (one.instrumentTracks.any { it.name == StarterWizard.SUB_TRACK }) {
                // Kept as two tracks: the fallback, nothing changed.
                assertEquals(style.id, p, one)
                continue
            }
            folded++
            assertEquals(style.id, p.instrumentTracks.size - 1, one.instrumentTracks.size)
            val layer = one.layersOf(bass).single()
            assertEquals(LayerRole.SUB, layer.role)
            assertEquals(-12, layer.transpose)
            assertFalse(layer.throughTrackFx)
            assertEquals(LayerSound.of(sub), layer.sound)
            // Its effects (the pump) on the layer; its level the same against the bass's fader.
            val subCh = p.mixer.channels[InstrumentTrack.mixerChannel(sub.id)]
            assertEquals(subCh.inserts.filter { it.effect != EffectType.NONE }, layer.inserts)
            // (The same fader curve on both, as the assistant draws them: the layer at unity under the bass's.)
            val bassCh = InstrumentTrack.mixerChannel(bass)
            val subLanes = p.automation.filter { it.target.ch == InstrumentTrack.mixerChannel(sub.id) }
            if (subLanes.isNotEmpty()) {
                assertEquals(0f, layer.gainDb, 1e-4f)
                assertTrue(subLanes.all { l -> l.copy(target = l.target.copy(ch = bassCh)) in one.automation })
            } else {
                val bassFader = (p.mixer.channels[bassCh].volume / 0.8f).let { it * it }
                val subFader = (subCh.volume / 0.8f).let { it * it }
                assertEquals(style.id, subFader, Layers.linear(layer.gainDb) * bassFader, 0.01f * subFader)
            }
            assertTrue(one.automation.none { it.target.ch == InstrumentTrack.mixerChannel(sub.id) && it.target.kind != AutoTarget.BUS_PARAM })
            // The bass's notes are untouched, in every scene; the sub's strip back to neutral.
            assertEquals(p.allScenes.map { it.trackNotes[bass] }, one.allScenes.map { it.trackNotes[bass] })
            assertEquals(Channel(), one.mixer.channels[InstrumentTrack.mixerChannel(sub.id)])
            // Saved and loaded as it is.
            assertEquals(one, ProjectJson.decode(ProjectJson.encode(one)))
        }
        println("bass music: $folded of ${styles.size} subs folded into their bass")
        assertTrue("at least one style's sub folds into its bass", folded > 0)
    }

    @Test
    fun aSubThatPlaysOtherNotesStaysATrack() {
        val p = bassMusic("tech_house")
        val sub = p.instrumentTracks.first { it.name == StarterWizard.SUB_TRACK }
        val bass = SubLayers.bassOf(p, sub.id)!!
        assertNotNull(p.subAsLayer(bass, sub.id))
        // One note a semitone off, in one scene: not the bass an octave down any more.
        val scene = p.scenes.indexOfFirst { it.trackNotes[sub.id]?.notes?.isNotEmpty() == true }
        val moved = p.copy(scenes = p.scenes.mapIndexed { i, sc ->
            if (i != scene) sc else sc.copy(trackNotes = sc.trackNotes + (sub.id to sc.trackNotes.getValue(sub.id).let { c -> c.copy(notes = c.notes.mapIndexed { k, n -> if (k == 0) n.copy(pitch = n.pitch + 1) else n }) }))
        })
        assertEquals("other notes in a scene", moved.subLayerProblem(bass, sub.id))
        assertNull(moved.subAsLayer(bass, sub.id))
        assertNull(p.subAsLayer(bass, bass))
        assertNull(p.subAsLayer(bass, 0))
        // The bass's strip shaping the sum (its sends, its pan, its quick controls), or the sub's
        // quick controls moved: the two stay apart.
        val bassCh = InstrumentTrack.mixerChannel(bass)
        fun strip(ch: Int, edit: (Channel) -> Channel) = p.copy(mixer = p.mixer.copy(channels = p.mixer.channels.mapIndexed { i, c -> if (i == ch) edit(c) else c }))
        assertEquals("the bass sends to the reverb or the echo", strip(bassCh) { it.copy(sends = listOf(0.2f, 0f)) }.subLayerProblem(bass, sub.id))
        assertEquals("the bass is panned", strip(bassCh) { it.copy(pan = 0.3f) }.subLayerProblem(bass, sub.id))
        assertEquals("the bass's quick controls are moved", strip(bassCh) { it.copy(macros = listOf(0.5f, 0.4f, 0f, 0f)) }.subLayerProblem(bass, sub.id))
        assertEquals("the sub's quick controls are moved", strip(InstrumentTrack.mixerChannel(sub.id)) { it.copy(macros = listOf(0.7f, 0f, 0f, 0f)) }.subLayerProblem(bass, sub.id))
        // A sub with a long effect on its strip (no place on a layer): stays.
        val ch = InstrumentTrack.mixerChannel(sub.id)
        val echo = p.copy(mixer = p.mixer.copy(channels = p.mixer.channels.mapIndexed { i, c ->
            if (i == ch) c.copy(inserts = listOf(Insert.of(EffectType.ECHO)) + c.inserts.drop(1)) else c
        }))
        assertNull(echo.subAsLayer(bass, sub.id))
    }
}
