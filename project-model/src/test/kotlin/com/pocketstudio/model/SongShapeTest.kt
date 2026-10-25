package com.pocketstudio.model

import com.pocketstudio.model.StarterWizard.Part
import com.pocketstudio.model.StarterWizard.Role
import com.pocketstudio.model.StarterWizard.SongForm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import com.pocketstudio.model.SongBars.drumSceneOfBars
import com.pocketstudio.model.SongBars.sceneOfBars
import kotlin.math.log10

/**
 * Chantier S2 (docs/PLAN-SON-PRO.md): an arrangement that drops. The song's shape per family, the
 * bars before each Drop (the kick and the bass out, a beat of silence), the Drop in two scenes of 8
 * (the hook doubled in the second), a new layer every 8 bars, the transitions (crash, impact, sub
 * drop, downlifter, rewind, fake drop) and the vocals, on the songs the assistant really makes.
 */
class SongShapeTest {
    private fun style(id: String) = StarterWizard.style(id)!!

    /** The song « Partir d'un style » makes: the style's advised lines and parts, its mood. */
    private fun song(s: StarterWizard.Style, parts: Set<Part> = StarterWizard.defaultParts(s)): Project {
        val lines = s.lines.filter { !it.off }.associate { it.role to null as PadSample? }
        return StarterWizard.build(s, lines, s.melody.mood, parts.associateWith { StarterWizard.soundsFor(it, s).first() })
    }

    private fun Project.notesOf(scene: Int, track: Int): List<Note> =
        allScenes[scene].let { if (track == 0) it.synthNotes else it.trackNotes[track] ?: NoteClip() }.notes

    private fun Project.sectionNames() = StarterWizard.songSections(this).map { allScenes[it.scene].label }

    private fun Project.seconds() = songBars * beatsPerBar * 60.0 / tempoBpm

    /** The scenes of [this] song right before a Drop (or a Refrain), in order of the song. */
    private fun Project.beforeDrops(): List<Int> = song.zipWithNext().filter { (a, b) ->
        // G0: a middle Drop may open on the last Drop's own scene (« Refrain 3 »).
        allScenes[b.scene].label.let { it == SongShape.FAKE_DROP || Regex("""^(Drop|Refrain)( \d+)?$""").matches(it) } &&
            allScenes[a.scene].label.let { it.startsWith("Montée") || it.startsWith(SongShape.PRE_CHORUS) || it.startsWith("Couplet ") || it == SongShape.FAKE_DROP }
    }.map { it.first.scene }

    @Test
    fun eachFamilyHasItsForm() {
        // Techno and bass music: 5 to 6 minutes (4 min 30 at their fastest tempos); house, the others: about 3.
        for (s in StarterWizard.STYLES) {
            val p = song(s)
            val long = s.family in SongShape.LONG_FAMILIES
            assertTrue("${s.id} ${p.seconds()}", p.seconds() in (if (long) 270.0..360.0 else 150.0..210.0))
            val expected = when (s.family) {
                "Hip-hop" -> SongForm.Rap
                "Autres" -> SongForm.Song
                else -> s.form
            }
            assertEquals(s.id, expected, s.form)
        }
        // A long club song: the DJ's intro of 32 bars, a Groove then its Montée, Drops of 32, a Break of 16 (and of 8).
        val techno = song(style("techno"))
        assertEquals(listOf("Intro", "Groove", "Montée", "Drop", "Break", "Montée", "Drop", "Intro"), techno.sectionNames())
        assertEquals(listOf(32, 24, 8, 32, 16, 8, 32, 24), StarterWizard.songSections(techno).map { it.bars })
        val hard = song(style("hard_techno"))
        assertEquals(listOf("Intro", "Groove", "Montée", "Drop", "Break", "Montée", "Drop", "Break", "Montée", "Drop", "Intro"), hard.sectionNames())
        // House stays near 3 minutes with the same steps.
        assertEquals(listOf("Intro", "Groove", "Montée", "Drop", "Break", "Montée", "Drop", "Intro"), song(style("house")).sectionNames())
        // A rap song: its Refrain right after the Intro, 16-bar Couplets, a Pont, the last Refrain.
        val trap = song(style("trap"))
        assertEquals(listOf("Intro", "Refrain", "Couplet", "Refrain", "Couplet", "Pont", "Refrain", "Intro"), trap.sectionNames())
        assertEquals(listOf(8, 16, 16, 16, 16, 8, 16, 8), StarterWizard.songSections(trap).map { it.bars })
        // A song: Pré-refrain, Post-refrain and Pont; the first Refrain within 45 seconds.
        for (id in listOf("pop", "synthwave", "afrobeats", "reggaeton", "uk_garage")) {
            val p = song(style(id))
            val names = p.sectionNames()
            assertTrue("$id $names", SongShape.PRE_CHORUS in names && "Pont" in names && names.count { it == "Refrain" } == 3)
            val first = StarterWizard.songSections(p).first { it.scene == 2 }
            assertTrue("$id ${first.start}", first.start * 4 * 60.0 / p.tempoBpm <= SongShape.FIRST_CHORUS_SECONDS)
        }
        assertTrue(SongShape.POST_CHORUS in song(style("pop")).sectionNames())
    }

    @Test
    fun theKickAndTheBassStopTwoBarsBeforeEachDropThenABeatOfSilence() {
        for (s in StarterWizard.STYLES) {
            val p = song(s)
            val before = p.beforeDrops()
            if (StarterWizard.defaultParts(s).isEmpty() && s.melody.acid == null) continue
            assertTrue(s.id, before.isNotEmpty())
            val bass = p.instrumentTracks.filter { it.name in setOf("Basse", StarterWizard.SUB_TRACK, StarterWizard.BASS_SWITCH_TRACK) }.map { it.id }
            for (scene in before.distinct()) {
                val sc = p.allScenes[scene]
                val steps = sc.stepsFor(16)
                assertEquals("${s.id} ${sc.label}", 64, steps)
                // The kick, the 808 and the rumble out of the last 2 bars (a song's last bar; a club Montée's whole
                // second half since S5b); the bass's notes too. A fake drop keeps its kicks (without the bass) up to its beat of silence.
                val cut = when {
                    sc.label == SongShape.FAKE_DROP -> 4
                    s.form == SongForm.Song -> 16
                    s.form == SongForm.Rap -> 32
                    else -> 16 * sc.drums.lanes.withIndex().none { (i, l) -> l.hits.isNotEmpty() && SongShape.BASS_LINES.none { it.lane == i } }.let { kickOnly -> if (kickOnly) 2 else SongShape.CLUB_CUT_BARS }
                }
                for (r in SongShape.BASS_LINES) assertTrue("${s.id} ${sc.label} $r", sc.drums.lanes[r.lane].hits.keys.none { it >= steps - cut })
                for (id in bass) assertTrue("${s.id} ${sc.label}", p.notesOf(scene, id).none { it.end > (steps - cut) * NoteClip.TICKS_PER_STEP })
                // A beat of silence: no hit, no note, no 303 step on the last beat.
                assertTrue("${s.id} ${sc.label}", sc.drums.lanes.all { l -> l.hits.keys.none { it >= steps - 4 } })
                assertTrue("${s.id} ${sc.label}", sc.trackNotes.values.all { c -> c.notes.none { it.end > (steps - 4) * NoteClip.TICKS_PER_STEP } })
                assertTrue("${s.id} ${sc.label}", sc.synthNotes.notes.none { it.end > (steps - 4) * NoteClip.TICKS_PER_STEP })
                assertTrue("${s.id} ${sc.label}", sc.acid.steps.take(sc.acid.length).withIndex().none { (i, st) -> st.gate && i >= sc.acid.length - 4 })
            }
        }
        // The beat of silence is total: the whole mix's volume (song only) at 0 on it, reverb and echo tails included.
        for (id in listOf("techno", "dnb", "pop", "trap")) {
            val p = song(style(id))
            val lane = p.automation.single { it.target == AutoTarget.masterVolume() }
            assertTrue(id, lane.songOnly && lane.songPerBeat)
            val curve = lane.songCurve(p.songBars * 4)
            val drops = StarterWizard.songSections(p).filter { it.scene == 2 }.map { it.start * 4 }.filter { it > 0 }
            val silent = curve.indices.filter { curve[it] == 0f }
            assertTrue("$id $silent", silent.isNotEmpty() && silent.all { it + 1 in drops || p.allScenes.any { sc -> sc.label == SongShape.FAKE_DROP } })
            drops.forEach { assertEquals(id, 1f, curve[it], 0f) }
            // The beat before the silence: a Montée's (a Pré-refrain's) top, under the Drop, lower in bass music (S5b);
            // a rap Intro's full level.
            val top = when (id) {
                "trap" -> 1f
                "pop" -> WizardMix.fader(WizardMix.MONTEE_TOP_DB)
                else -> WizardMix.fader(WizardMix.monteeTopDb(style(id)))
            }
            assertEquals(id, top, curve[drops.first() - 2], 1e-6f)
        }
        // A roll of several hits rises inside its step (the engine's velocity ramp).
        assertTrue((StarterWizard.rollHalf(0) + StarterWizard.rollHalf(1)).values.all { (it.repeats > 1) == (it.ramp == 1) })
        // A rap Couplet: its kick and 808 stop 2 bars before each Refrain, its hats go on.
        val trap = song(style("trap"))
        val end = trap.allScenes.indexOfFirst { it.label == "Couplet 3" }
        assertTrue(trap.allScenes[end].drums.lanes[Role.Hat.lane].hits.keys.any { it in 32 until 60 })
        assertTrue(trap.allScenes[end].drums.lanes[Role.Kick.lane].hits.keys.none { it >= 32 })
        // The Couplet leaves room for the voice: no chords there.
        val pad = trap.instrumentTracks.first { it.name in StarterWizard.CHORD_TRACK_NAMES }.id
        assertTrue(trap.notesOf(1, pad).isEmpty() && trap.notesOf(2, pad).isNotEmpty())
    }

    @Test
    fun theMonteeRollsAndStaysUnderTheDrop() {
        // Club music: the Groove's 8 bars become a Montée, the snare (or the clap) rolling from quarters to rolls of 4.
        for (id in listOf("techno", "house", "trance", "jungle")) {
            val p = song(style(id))
            val a = p.allScenes.indexOfFirst { it.label == "Montée" }
            val b = p.allScenes.indexOfFirst { it.label == "Montée 2" }
            assertTrue(id, a >= 4 && b > a)
            val roll = listOf(Role.Snare, Role.Clap).first { r -> p.allScenes[b].drums.lanes[r.lane].hits.isNotEmpty() }
            // Its top follows the style's backbeat as the Drop plays it (S5b).
            val s = style(id)
            val steps = StarterWizard.drumSteps(s)
            val top = StarterWizard.rollTop(StarterWizard.hits(s.lines.first { it.role == roll }, steps), steps)
            assertEquals(id, StarterWizard.rollHalf(0, top), p.allScenes[a].drums.lanes[roll.lane].hits)
            assertEquals(id, StarterWizard.rollHalf(1, top), p.allScenes[b].drums.lanes[roll.lane].hits)
            // The chords softer than in the Drop (held under it).
            val chords = p.instrumentTracks.firstOrNull { it.name in StarterWizard.CHORD_TRACK_NAMES }?.id
            if (chords != null && p.notesOf(a, chords).isNotEmpty()) {
                assertTrue(id, p.notesOf(a, chords).maxOf { it.velocity } < p.notesOf(2, chords).maxOf { it.velocity })
            }
        }
        // The riser climbs under the impact: its top in a Montée is under the Drop's first beat.
        assertTrue(StarterWizard.RISER_TOP < 1f && StarterWizard.ROLL_TOP < 1f)
    }

    @Test
    fun theDropComesInTwoScenesOfEightTheSecondFuller() {
        for (id in listOf("techno", "house", "dnb", "trap", "pop", "trance")) {
            val s = style(id)
            val parts = Part.entries.toSet()
            val p = song(s, parts)
            val label = p.allScenes[2].label
            val second = p.allScenes.indexOfFirst { it.label == "$label 2" }
            assertTrue(id, second > 0)
            // The Drop (Refrain) section: its scene, then the second one, by 8 bars.
            val drop = StarterWizard.songSections(p).first { it.scene == 2 }
            val order = (drop.start until drop.start + drop.bars step 8).map { p.sceneOfBars()[it] }
            assertEquals(id, 2, order.first())
            assertEquals(id, second, order[1])
            // The second scene plays the hook's fourth version (doubled), the first the hook itself.
            val lead = p.instrumentTracks.first { it.name == "Lead" }
            val sound = StarterWizard.soundsFor(Part.Lead, s).first()
            fun version(v: StarterWizard.Version) = StarterWizard.startingNotes(Part.Lead, s, s.melody.mood, p.harmony, sound, v).notes
            assertEquals(id, version(StarterWizard.Version.Chorus2), p.notesOf(second, lead.id))
            assertEquals(id, version(StarterWizard.Version.Chorus), p.notesOf(2, lead.id))
            assertTrue(id, p.notesOf(second, lead.id) != p.notesOf(2, lead.id))
        }
        // The second Drop switches the bass sound (jump up, neurofunk, riddim: « Basse 2 » from the second Drop on).
        for (id in listOf("jump_up", "neurofunk", "riddim")) {
            val p = song(style(id))
            val second = p.instrumentTracks.first { it.name == StarterWizard.BASS_SWITCH_TRACK }
            val lane = p.automation.first { it.target == AutoTarget(AutoTarget.VOLUME, InstrumentTrack.mixerChannel(second.id)) }
            val drops = StarterWizard.songSections(p).filter { it.scene == 2 }
            val curve = lane.songCurve(p.songBars)
            assertEquals(id, 0f, curve[drops[0].start], 0f)
            assertTrue(id, curve[drops[1].start] > 0.5f)
        }
    }

    @Test
    fun technoMutesALineForEightBarsAndLayersComeInEveryEightBars() {
        val p = song(style("techno"))
        val mute = p.allScenes.single { it.label == "Drop 3" }
        val drop = p.allScenes[2]
        // The lines a scene plays (a pad heard only in its phrase's fill, the kit's toms, is not one).
        val lanes = { sc: Scene -> sc.drums.lanes.indices.filter { l -> sc.drums.lanes[l].hits.keys.any { it < sc.drums.steps - 4 } }.toSet() }
        assertEquals(lanes(drop).size - 1, lanes(mute).size)
        assertTrue(lanes(drop).containsAll(lanes(mute)))
        // The intro's 32 bars: a layer more every 8 bars (the kick first), the same scenes backwards in the outro.
        val intro = StarterWizard.songSections(p).first()
        val scenes = (0 until intro.bars step 8).map { p.sceneOfBars()[it] }
        val counts = scenes.map { lanes(p.allScenes[it]).size }
        assertTrue("$counts", counts.zipWithNext().all { (a, b) -> b >= a } && counts.last() > counts.first())
        // The kick first, with its rumble (S3: on by default in techno).
        assertEquals(setOf(Role.Kick.lane, Role.Rumble.lane), lanes(p.allScenes[scenes.first()]))
        // House: no mute (G0: its « Drop 3 » is the last Drop's own scene, a layer more, never one less).
        val house = song(style("house"))
        assertTrue(house.allScenes.filter { it.label.startsWith("Drop") }.all { lanes(it).size >= lanes(house.allScenes[2]).size })
    }

    @Test
    fun theDropHitsWithACrashAnImpactASubDropAndTheBassLifted() {
        val p = song(style("techno"))
        // The crash: the kit's cymbal on its own pad, on the first beat of each Drop (Refrain), echoed; a style whose
        // ride takes that pad keeps the riser's noise impact there. Since S3 it is the song's: the Drop's first bar
        // plays a one-bar scene with the crash (never every loop of the Drop's scene).
        var crashes = 0
        for (s in StarterWizard.STYLES) {
            val q = song(s)
            if (s.lines.none { it.role == Role.Ride }) {
                val drums = q.drumSceneOfBars()
                for (d in StarterWizard.songSections(q).filter { it.scene == 2 }) {
                    val crash = q.allScenes[drums[d.start]]
                    assertEquals(s.id, DrumArrangement.CRASH_LABEL, crash.label)
                    assertEquals(s.id, setOf(0), crash.drums.lanes[Role.Ride.lane].hits.keys)
                }
                assertEquals(s.id, 0.25f, q.mixer.channels[Mixer.padChannel(Role.Ride.lane)].sends[1], 0f)
                // Only there: the scenes' own cymbal pad stays silent.
                assertTrue(s.id, q.allScenes.filter { it.label != DrumArrangement.CRASH_LABEL }.all { it.drums.lanes[Role.Ride.lane].hits.isEmpty() })
                crashes++
            } else if (s.riser) {
                val riser = q.instrumentTracks.single { it.name == StarterWizard.RISER_TRACK }
                assertEquals(s.id, 0, q.notesOf(2, riser.id).single().start)
            }
        }
        assertTrue("$crashes", crashes >= 15)
        // The sub drop: a sine sliding down two octaves on the Drop's first beat (its track, its slide).
        val sub = p.instrumentTracks.single { it.name == StarterWizard.SUB_DROP_TRACK }
        assertEquals(1f, sub.synth.get(SynthPatch.GLIDE))
        val (high, low) = p.notesOf(2, sub.id)
        assertEquals(24, high.pitch - low.pitch)
        assertTrue(low.start in 1..high.end)
        assertTrue(p.allScenes.withIndex().filter { it.value.label != "Drop" }.all { p.notesOf(it.index, sub.id).isEmpty() })
        // The bass: 1.5 dB under its fader outside the Drops, at it in them (song only).
        val bass = p.instrumentTracks.first { it.name == "Basse" }
        val ch = InstrumentTrack.mixerChannel(bass.id)
        val lane = p.automation.single { it.target == AutoTarget(AutoTarget.VOLUME, ch) }
        assertTrue(lane.songOnly)
        val curve = lane.songCurve(p.songBars)
        val level = p.mixer.channels[ch].volume
        val sections = StarterWizard.songSections(p)
        val drop = sections.first { it.scene == 2 }
        val groove = sections.first { it.scene == 1 }
        assertEquals(level, curve[drop.start], 1e-6f)
        // The engine's fader law: (v / 0.8)², so 1.5 dB is v · 10^(1.5 / 40).
        assertEquals(-StarterWizard.BASS_LIFT_DB, (40 * log10(curve[groove.start] / level)), 1e-3f)
        // Songs have no sub drop (their Refrain's impact is enough).
        assertTrue(song(style("pop")).instrumentTracks.none { it.name == StarterWizard.SUB_DROP_TRACK })
    }

    @Test
    fun transitionsFakeDropAndRewind() {
        // Riddim: a fake drop (the Drop's beat without its bass), then a beat of silence before the real one.
        val riddim = song(style("riddim"))
        val names = riddim.sectionNames()
        assertEquals(SongShape.FAKE_DROP, names[names.indexOf("Drop") - 1])
        val fake = riddim.allScenes.indexOfFirst { it.label == SongShape.FAKE_DROP }
        val bassIds = riddim.instrumentTracks.filter { it.name.startsWith("Basse") }.map { it.id }
        assertTrue(bassIds.all { riddim.notesOf(fake, it).isEmpty() })
        assertTrue(riddim.allScenes[fake].drums.lanes[Role.Kick.lane].hits.isNotEmpty())
        // Only once, before the first Drop.
        assertEquals(1, names.count { it == SongShape.FAKE_DROP })
        // Jungle: the record pulled back (a tape stop on the master) before the second Drop, and only there.
        val jungle = song(style("jungle"))
        val slot = jungle.mixer.master.indexOfFirst { it.effect == EffectType.TAPE_STOP }
        assertTrue(slot >= 0)
        val lane = jungle.automation.single { it.target == AutoTarget(AutoTarget.INSERT_PARAM, Mixer.MASTER, slot, 0) }
        assertTrue(lane.songOnly && lane.songPerBeat)
        val curve = lane.songCurve(jungle.songBars * 4)
        val drops = StarterWizard.songSections(jungle).filter { it.scene == 2 }
        assertEquals(1f, curve[drops[1].start * 4 - 2], 0f)
        assertEquals(0f, curve[drops[1].start * 4], 0f)
        assertEquals(0f, curve[drops[0].start * 4 - 2], 0f)
        assertEquals(0f, jungle.mixer.master[slot].params[0], 0f)
        assertTrue(song(style("dnb")).mixer.master.none { it.effect == EffectType.TAPE_STOP })
        // A Break starts with the noise falling (a downlifter): the riser held there, its filter falling.
        val techno = song(style("techno"))
        val riser = techno.instrumentTracks.single { it.name == StarterWizard.RISER_TRACK }
        assertTrue(techno.notesOf(3, riser.id).isNotEmpty())
    }

    @Test
    fun vocalsAnswerStabShoutAndCrush() {
        // House and jersey club: chops that answer the hook in its rests; trap: a stab every 4 bars; drum & bass: a shout;
        // phonk: a crushed hit. All sung by the built-in voice, on a « Voix » track.
        for (id in listOf("house", "deep_house", "tech_house", "jersey_club", "trap", "dnb", "jump_up", "phonk")) {
            val s = style(id)
            assertNotNull(id, s.vocal)
            val p = song(s, Part.entries.toSet())
            val voice = p.instrumentTracks.singleOrNull { it.name == StarterWizard.VOCAL_TRACK }
            assertNotNull(id, voice)
            assertEquals(id, 1, voice!!.engine)
            val notes = p.allScenes.indices.flatMap { p.notesOf(it, voice.id) }
            assertTrue(id, notes.isNotEmpty() && notes.all { p.harmony.inScale(it.pitch) })
            when (s.vocal) {
                StarterWizard.Vocal.Stab -> assertEquals(id, listOf(12, 76), p.notesOf(2, voice.id).map { it.start / NoteClip.TICKS_PER_STEP })
                StarterWizard.Vocal.Crush -> assertTrue(id, p.mixer.channels[InstrumentTrack.mixerChannel(voice.id)].inserts.any { it.effect == EffectType.BITCRUSHER })
                StarterWizard.Vocal.Shout -> {
                    // Only in the Drop's second scene.
                    val second = p.allScenes.indexOfFirst { it.label == "Drop 2" }
                    assertTrue(id, p.notesOf(second, voice.id).isNotEmpty() && p.notesOf(2, voice.id).isEmpty())
                }
                StarterWizard.Vocal.Answer -> {
                    // In the hook's rests: no chop starts while a note of the hook sounds.
                    val lead = p.instrumentTracks.first { it.name == "Lead" }
                    val hook = p.notesOf(2, lead.id)
                    assertTrue(id, p.notesOf(2, voice.id).none { v -> hook.any { v.start in it.start until it.end } })
                }
                null -> Unit
            }
        }
        // Without a melody, no voice; a style without a vocal gets none.
        val house = style("house")
        assertNull(StarterWizard.build(house, house.lines.associate { it.role to null }).instrumentTracks.firstOrNull { it.name == StarterWizard.VOCAL_TRACK })
        assertTrue(song(style("techno")).instrumentTracks.none { it.name == StarterWizard.VOCAL_TRACK })
    }

    @Test
    fun theSummaryNamesTheSections() {
        val s = style("house")
        val plan = StarterWizard.planFor(s, s.lines.filter { !it.off }.map { it.role }.toSet(), StarterWizard.defaultParts(s), false)
        assertEquals(listOf("Intro", "Groove", "Montée", "Drop", "Break", "Montée", "Drop", "Intro"), StarterWizard.songSectionTitles(s, plan))
        // The variants' names are shown translated, numbered as copies are ("Drop 2").
        val drop2 = plan.sceneLabels.indexOf("Drop 2")
        assertEquals("Drop 2", plan.title(drop2))
        assertFalse(plan.contents(drop2).isEmpty())
        assertEquals(plan.role(2), plan.role(drop2))
    }
}
