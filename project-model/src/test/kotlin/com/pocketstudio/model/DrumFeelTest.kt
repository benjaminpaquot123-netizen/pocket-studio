package com.pocketstudio.model

import com.pocketstudio.model.SongBars.drumBars
import com.pocketstudio.model.SongBars.drumSceneOfBars
import com.pocketstudio.model.SongBars.isDrumBar
import com.pocketstudio.model.SongBars.key
import com.pocketstudio.model.StarterWizard.Part
import com.pocketstudio.model.StarterWizard.Role
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Chantier S3 (docs/PLAN-SON-PRO.md), the generator's half: a living beat (the hats' strength in
 * a cycle, every hit its own timing, ghost notes, fills at the 4, the 8 and the 16 with several
 * endings, the crash once every 16, the Drop's second scene a « B » beat), kit v3 and the studio
 * voices (the 808 on the chords' roots, the tuned kicks, the rumble), on the songs the assistant makes.
 */
class DrumFeelTest {
    private fun style(id: String) = StarterWizard.style(id)!!

    /** The song « Partir d'un style » makes: the style's advised lines and parts, its mood. */
    private fun song(s: StarterWizard.Style, parts: Set<Part> = StarterWizard.defaultParts(s)): Project {
        val lines = s.lines.filter { !it.off }.associate { it.role to null as PadSample? }
        return StarterWizard.build(s, lines, s.melody.mood, parts.associateWith { StarterWizard.soundsFor(it, s).first() })
    }

    private val songs by lazy { StarterWizard.STYLES.associateWith { song(it) } }

    @Test
    fun theHatsBreatheAndNoTwoBarsAreAlike() {
        for ((s, p) in songs) {
            // The verification of the plan: the hats' strengths vary (σ ≥ 0.05), at least 6 different bars of drums.
            val hats = p.allScenes.filter { !it.isDrumBar() }.flatMap { it.drums.lanes[Role.Hat.lane].hits.values.map { h -> h.velocity } }
            assertTrue("${s.id} ${DrumHumanizer.sigma(hats)}", DrumHumanizer.sigma(hats) >= 0.05f)
            val bars = p.drumBars().filter { b -> b.any { it.isNotEmpty() } }
            assertTrue("${s.id} ${bars.map(SongBars::key).toSet().size}", bars.map(SongBars::key).toSet().size >= 6)
            // Not by timing alone: at least 6 bars that differ in what they play (place, strength to 0.1, roll).
            val played = bars.map { b -> b.mapIndexed { lane, hits -> hits.toSortedMap().map { (st, h) -> "$lane:$st:${Math.round(h.velocity * 10)}:${h.repeats}" } } }.toSet()
            assertTrue("${s.id} ${played.size}", played.size >= 6)
        }
        // The cycle itself: 100/70/85/60 on even hats.
        val even = List(DrumPattern.PAD_COUNT) { if (it == Role.Hat.lane) DrumLane((0 until 16).associateWith { Hit(1f) }) else DrumLane() }
        val cycled = DrumHumanizer.humanize(even, 16, DrumHumanizer.Feel(jitter = 0f), 1L, 120.0, 0.0)[Role.Hat.lane].hits
        assertEquals(listOf(1f, 0.7f, 0.85f, 0.6f), (0 until 4).map { cycled.getValue(it).velocity })
    }

    @Test
    fun eachLineSitsAtItsPlaceAroundTheGrid() {
        for ((s, p) in songs) {
            val drop = p.allScenes[2].drums.lanes
            val feel = DrumHumanizer.feel(s)
            // The clap a few ms ahead, the hats a little behind (drill: its swing on top), the kick, the 808 and the rumble on the grid.
            drop[Role.Clap.lane].hits.values.forEach { assertTrue("${s.id} clap ${it.offset}", it.offset in -6f..-1f) }
            drop[Role.Hat.lane].hits.values.forEach { assertTrue("${s.id} hat ${it.offset}", it.offset in -1f..30f) }
            for (r in listOf(Role.Kick, Role.Bass808, Role.Rumble)) assertTrue(s.id, drop[r.lane].hits.values.all { it.offset == 0f })
            if (feel.snareLateMs > 0f) {
                val backbeat = drop[Role.Snare.lane].hits.filterValues { it.velocity > DrumHumanizer.GHOST_HIGH }.values
                assertTrue(s.id, backbeat.isNotEmpty() && backbeat.all { it.offset >= feel.snareLateMs - 2.5f })
            }
        }
        // Laid back: the lo-fi and boom bap snare behind the beat; ghost notes around it.
        assertEquals(15f, DrumHumanizer.feel(style("lofi")).snareLateMs)
        assertEquals(10f, DrumHumanizer.feel(style("boom_bap")).snareLateMs)
        val ghosts = songs.getValue(style("boom_bap")).allScenes[2].drums.lanes[Role.Snare.lane].hits.values.filter { it.velocity <= DrumHumanizer.GHOST_HIGH }
        assertTrue(ghosts.size >= 2)
        // Drill swings its hats alone: the project plays straight, the hats' off 16ths come late by the style's swing.
        val drill = style("drill")
        val p = songs.getValue(drill)
        assertEquals(0.0, p.swing, 0.0)
        val hats = p.allScenes[2].drums.lanes[Role.Hat.lane].hits
        val sixteenth = 60_000.0 / drill.bpm / 4
        val odd = hats.filterKeys { it % 2 == 1 }.values.map { it.offset }
        val even = hats.filterKeys { it % 2 == 0 }.values.map { it.offset }
        assertTrue(odd.isNotEmpty() && odd.average() - even.average() > drill.swing * sixteenth * 0.8)
        // Elsewhere the style's own swing stays the project's.
        assertEquals(style("house").swing, songs.getValue(style("house")).swing, 0.0)
    }

    @Test
    fun fillsAtTheFourTheEightAndTheSixteenWithSeveralEndings() {
        for (id in listOf("techno", "house", "dnb", "trap", "pop", "acid_techno")) {
            val p = songs.getValue(style(id))
            val sections = StarterWizard.songSections(p)
            val drums = p.drumSceneOfBars()
            // The bigger fills: one-bar parts whose drums play a « Fill » scene, on the last bar of an 8 or a 16.
            val fills = drums.indices.filter { p.allScenes[drums[it]].label == PartIntents.FILL_LABEL }
            assertTrue("$id $fills", fills.size >= 3)
            // The endings differ along the song: at least 3 different second halves of a phrase's or a fill's last bar.
            val bars = p.drumBars()
            val ends = (sections.flatMap { sec -> (sec.start + 3 until sec.start + sec.bars step 4).toList() } + fills)
                .map { b -> key(bars[b].map { l -> l.filterKeys { it >= 8 }.mapValues { (_, h) -> h.copy(offset = 0f, velocity = Math.round(h.velocity * 5) / 5f) } }) }.toSet()
            assertTrue("$id ${ends.size}", ends.size >= 3)
            // Fill parts play the drums only: the others go on where they were.
            for (part in p.song.filter { p.allScenes.getOrNull(it.play[SongPart.DRUMS] ?: -1)?.isDrumBar() == true }) {
                assertEquals(1, part.bars)
                assertTrue(part.from.keys.none { it == SongPart.DRUMS })
            }
            // A few scenes stay free for the user's own variations.
            assertTrue(p.allScenes.size <= Scene.MAX - DrumArrangement.FREE_SLOTS)
        }
    }

    @Test
    fun theCrashOnTheOneOfEachSixteenNeverTwice() {
        for ((s, p) in songs) {
            if (s.lines.any { it.role == Role.Ride }) continue
            val bars = p.drumBars()
            val crashes = bars.indices.filter { 0 in bars[it][Role.Ride.lane] }
            assertTrue(s.id, crashes.isNotEmpty())
            // Never two close together (a fake drop's, then the real Drop's 4 bars later at the closest), never on every loop of a scene.
            assertTrue("${s.id} $crashes", crashes.zipWithNext().all { (a, b) -> b - a >= 8 || (b - a == 4 && s.fakeDrop) })
            // Each Drop (Refrain) starts with one.
            for (d in StarterWizard.songSections(p).filter { it.scene == 2 }) assertTrue("${s.id} ${d.start}", d.start in crashes)
        }
        // A 32-bar Drop (jump up, no ride): on its bars 1 and 17, not on 5 nor 9.
        val jump = songs.getValue(style("jump_up"))
        val crashes = jump.drumBars().let { b -> b.indices.filter { 0 in b[it][Role.Ride.lane] } }
        val drop = StarterWizard.songSections(jump).first { it.scene == 2 }
        assertTrue("$crashes ${drop.start}", drop.start + 16 in crashes && drop.start + 4 !in crashes && drop.start + 8 !in crashes)
    }

    @Test
    fun theSecondDropPlaysTheBeatsBVersion() {
        for (id in listOf("techno", "house", "dnb", "trap")) {
            val p = songs.getValue(style(id))
            val a = p.allScenes[2]
            val b = p.allScenes.first { it.label == "${a.label} 2" }
            val hits = { sc: Scene -> sc.drums.lanes.map { it.hits.keys } }
            assertTrue(id, hits(a) != hits(b))
        }
    }

    @Test
    fun theStudioVoicesOnTheirPads() {
        for ((s, p) in songs) {
            assertEquals(s.id, Project.KIT_V3, p.kitVersion)
            val kick = p.padVoices[Role.Kick.lane]
            when {
                s.id in StudioDrums.HARD_KICK -> assertEquals(s.id, PadVoice.KICK_HARD, kick?.kind)
                s.family in setOf("House", "Techno") -> assertEquals(s.id, PadVoice.KICK_STUDIO, kick?.kind)
                else -> assertNull(s.id, kick)
            }
            // Tuned to the key (its root, else its fifth) between 43 and 66 Hz.
            kick?.param(PadVoice.TUNE)?.let { hz ->
                assertTrue("${s.id} $hz", hz in 43f..66f)
                val pc = Math.floorMod(Math.round(12 * kotlin.math.ln(hz / 440.0) / kotlin.math.ln(2.0) + 69).toInt(), 12)
                assertTrue("${s.id} $pc", pc == Math.floorMod(p.harmony.root, 12) || pc == Math.floorMod(p.harmony.root + 7, 12))
            }
            // The rumble wherever a style has one, tuned with its kick.
            if (s.lines.any { it.role == Role.Rumble && !it.off }) assertEquals(s.id, PadVoice.RUMBLE, p.padVoices[Role.Rumble.lane]?.kind)
        }
        // The 808 plays the bass in trap, drill, phonk and jersey club: on the chords' roots, sliding where the line slides.
        for (id in StudioDrums.BASS_808) {
            val s = style(id)
            val p = songs.getValue(s)
            val voice = p.padVoices.getValue(StudioDrums.LANE_808)
            assertEquals(id, PadVoice.BASS_808, voice.kind)
            val tune = voice.param(PadVoice.TUNE)!!.toInt()
            assertEquals(id, Math.floorMod(p.harmony.root, 12), Math.floorMod(tune, 12))
            assertTrue(id, StudioDrums.hz(tune) in 30.0..65.0)
            val hits = p.allScenes[2].drums.lanes[StudioDrums.LANE_808].hits
            StarterWizard.progressionOf(s, s.melody.mood).second.forEachIndexed { bar, d ->
                val root = Math.floorMod(p.harmony.degreeRoot(d, 36), 12)
                assertTrue("$id bar $bar", hits.filterKeys { it / 16 == bar }.values.any { Math.floorMod(tune + it.pitch, 12) == root })
            }
            if (s.melody.bassGlide) assertTrue(id, hits.values.any { it.glide in 60..200 }) else assertTrue(id, hits.values.all { it.glide == 0 })
            // The Refrain at full, the 808 1.5 dB softer elsewhere (the bass's lift).
            val verse = p.allScenes[1].drums.lanes[StudioDrums.LANE_808].hits.values.maxOfOrNull { it.velocity } ?: 0f
            assertTrue(id, verse < hits.values.maxOf { it.velocity })
            // The style's treatment of its bass is on the 808's pad.
            if (s.effects.any { it.target == Part.Bass.id }) assertTrue(id, p.mixer.channels[Mixer.padChannel(StudioDrums.LANE_808)].hasOwnFx())
        }
        // « Des basses qui tapent »: with the style's pack on the phone the engine's 808 still comes first,
        // the pack's 808 second; chosen, the pack's 808 keeps its track (its sound), the studio 808 stays off.
        val trap = style("trap")
        val pack = PackManifest("trap", "Trap", sounds = listOf(PackSound("808_01.flac", "808", "808 1")))
        val offered = StarterWizard.soundsFor(Part.Bass, trap, listOf(pack))
        assertTrue(StudioDrums.bassOn808(trap, offered[0]))
        val packed = offered[1]
        assertTrue(packed.sampled != null)
        assertFalse(StudioDrums.bassOn808(trap, packed))
        // Elsewhere no 808 pad.
        for ((s, p) in songs) if (s.id !in StudioDrums.BASS_808) assertNull(s.id, p.padVoices[StudioDrums.LANE_808])
    }

    @Test
    fun oldProjectsLoadAndSoundAsTheyDid() {
        // A hit saved before S3: no timing, no slide; a project without studio voices; the kit it had.
        val old = ProjectJson.decode("""{"name":"Ancien","kitVersion":2,"drums":{"steps":16,"lanes":[{"hits":{"0":{"velocity":1.0}}}]}}""")
        assertEquals(Project.KIT_V2, old.kitVersion)
        assertTrue(old.padVoices.isEmpty())
        val hit = old.drums.lanes[0].hits.getValue(0)
        assertEquals(0f, hit.offset)
        assertEquals(0, hit.glide)
        // Saved again, nothing new in the file.
        val json = ProjectJson.encode(old)
        val hitJson = Regex("\"hits\":\\{\"0\":\\{[^}]*\\}").find(json)!!.value
        assertFalse(hitJson, hitJson.contains("offset") || hitJson.contains("glide"))
        assertFalse(json.contains("padVoices"))
        // The new fields survive a save, within the engine's ranges.
        val p = Project(
            kitVersion = Project.KIT_V3,
            drums = DrumPattern(lanes = List(DrumPattern.PAD_COUNT) { if (it == 1) DrumLane(mapOf(0 to Hit(1f, pitch = 7, offset = -3.5f, glide = 150))) else DrumLane() }),
            padVoices = mapOf(1 to PadVoice(PadVoice.BASS_808, mapOf(PadVoice.TUNE to 29f, PadVoice.DECAY to 1.3f))),
        )
        val back = ProjectJson.decode(ProjectJson.encode(p))
        assertEquals(p.kitVersion, back.kitVersion)
        assertEquals(p.padVoices, back.padVoices)
        assertEquals(p.drums.lanes[1].hits, back.drums.lanes[1].hits)
        // Out of range: clamped as the engine would (a kit that does not exist, a timing of 80 ms, a voice 9, pad 20).
        val wild = Project(
            kitVersion = 9,
            drums = DrumPattern(lanes = List(DrumPattern.PAD_COUNT) { if (it == 0) DrumLane(mapOf(0 to Hit(1f, offset = 80f, glide = 900))) else DrumLane() }),
            padVoices = mapOf(0 to PadVoice(9, mapOf(PadVoice.TUNE to 9000f)), 20 to PadVoice(PadVoice.RUMBLE)),
        ).sanitized()
        assertEquals(Project.KIT_V3, wild.kitVersion)
        assertEquals(Hit.MAX_OFFSET_MS, wild.drums.lanes[0].hits.getValue(0).offset)
        assertEquals(Hit.MAX_GLIDE_MS, wild.drums.lanes[0].hits.getValue(0).glide)
        assertTrue(wild.padVoices.isEmpty())
        val clamped = Project(padVoices = mapOf(0 to PadVoice(PadVoice.KICK_STUDIO, mapOf(PadVoice.TUNE to 9000f, PadVoice.CLICK to 3f)))).sanitized()
        assertEquals(400f, clamped.padVoices.getValue(0).param(PadVoice.TUNE))
        assertEquals(1f, clamped.padVoices.getValue(0).param(PadVoice.CLICK))
    }

    @Test
    fun theSameChoicesMakeTheSameBeat() {
        val s = style("house")
        assertEquals(song(s), song(s))
        // Another mood, another hand.
        val lines = s.lines.filter { !it.off }.associate { it.role to null as PadSample? }
        val dark = StarterWizard.build(s, lines, StarterWizard.Mood.Dark, emptyMap())
        val happy = StarterWizard.build(s, lines, StarterWizard.Mood.Happy, emptyMap())
        assertTrue(dark.allScenes[2].drums != happy.allScenes[2].drums)
    }
}
