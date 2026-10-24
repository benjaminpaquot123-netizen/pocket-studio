package com.pocketstudio.model

import com.pocketstudio.model.StarterWizard.Mood
import com.pocketstudio.model.StarterWizard.Part
import com.pocketstudio.model.StarterWizard.Role
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** Phase 28's music audit: chords of every scale, spelling, the Acide tab's key, ranges, the song plans. */
class MusicTheoryTest {
    private fun style(id: String) = StarterWizard.style(id)!!

    /** Triad kinds: true for a major or minor chord (a perfect fifth), false for a diminished or augmented one. */
    private fun plain(chord: List<Int>) = chord[2] - chord[0] == 7

    @Test
    fun progressionsPlayTheScalesOwnChordsInEveryScale() {
        // « Libre » (the default) plays the root's major scale: I–V–vi–IV has La mineur, not La majeur.
        val free = NoteClip().withProgression(Progression.Pop, Harmony(0, Scale.Off))
        assertEquals(listOf(57, 60, 64), free.notes.filter { it.start == 2 * 16 * NoteClip.TICKS_PER_STEP }.map { it.pitch })
        // The minor pentatonic and the blues are minor: the natural minor's chords (La-Do-Mi, not La-Do♯-Mi).
        for (scale in listOf(Scale.MinorPentatonic, Scale.Blues)) {
            val h = Harmony(9, scale)
            assertEquals(listOf(57, 60, 64), h.chord(57))
            val epic = NoteClip().withProgression(Progression.Epic, h)
            assertTrue(epic.notes.all { Harmony(9, Scale.Minor).inScale(it.pitch) })
            assertTrue(h.chordPads(48).all { pad -> pad.notes.all { Harmony(9, Scale.Minor).inScale(it) } })
        }
        // Every progression, scale and root: notes of the seven-note scale only, never a diminished or augmented chord.
        for (scale in Scale.entries) for (root in 0..11) for (pr in Progression.entries) {
            val h = Harmony(root, scale)
            val clip = NoteClip().withProgression(pr, h)
            assertTrue("$pr $scale $root", clip.notes.all { Harmony(root, scale.parent).inScale(it.pitch) })
            clip.notes.groupBy { it.start }.values.forEach { chord -> assertTrue("$pr $scale $root", plain(chord.map { it.pitch }.sorted())) }
            val bass = NoteClip().withBassLine(pr.degrees, h)
            assertTrue(bass.notes.all { Harmony(root, scale.parent).inScale(it.pitch) })
        }
    }

    @Test
    fun progressionLabelsFollowTheScale() {
        assertEquals("I–V–vi–IV", Progression.Pop.labelIn(Harmony(0, Scale.Off)))
        assertEquals("i–v–VI–iv", Progression.Pop.labelIn(Harmony(9, Scale.Minor)))
        assertEquals("i–VI–III–VII", Progression.Epic.labelIn(Harmony(9, Scale.MinorPentatonic)))
        // A diminished chord is replaced by its closest major or minor one: vii° → V, ii° → iv.
        assertEquals("I–vi–iii–V", Progression.Epic.labelIn(Harmony(0, Scale.Major)))
        assertEquals("iv–v–i–i", Progression.Jazz.labelIn(Harmony(9, Scale.Minor)))
        assertEquals("I–IV–V–I", Progression.House.labelIn(Harmony(0, Scale.Major)))
        // The label says what plays.
        val h = Harmony(2, Scale.Dorian)
        Progression.entries.forEach { pr ->
            val pads = h.chordPads(48)
            val played = NoteClip().withProgression(pr, h).notes.groupBy { it.start }.toSortedMap().values.map { c -> Math.floorMod(c.minOf { it.pitch }, 12) }
            assertEquals(pr.labelIn(h), played.joinToString("–") { pc -> pads.first { it.root == pc }.roman })
        }
    }

    @Test
    fun theChordKeyboardPlaysTheScalesChords() {
        // « Libre »: Ré gives Ré mineur (the major scale of Do), a black key a borrowed major chord.
        assertEquals(listOf(62, 65, 69), Harmony(0, Scale.Off).chord(62))
        assertEquals(listOf(61, 65, 68), Harmony(0, Scale.Off).chord(61))
        // Penta mineure in La: La mineur, Ré mineur, Mi mineur; nothing out of La mineur.
        val penta = Harmony(9, Scale.MinorPentatonic)
        assertEquals(listOf(62, 65, 69), penta.chord(62))
        assertTrue((48..72).all { p -> penta.chord(p, 4).all { Harmony(9, Scale.Minor).inScale(it) } })
    }

    @Test
    fun chordNamesAreSpeltAsTheKeyWritesThem() {
        assertEquals(listOf("Mi", "Fa♯ m", "Sol♯ m", "La", "Si", "Do♯ m", "Ré♯ dim", "Mi"), Harmony(4, Scale.Major).chordPads(48).map { it.name(false) })
        assertEquals(listOf("E", "F♯m", "G♯m", "A", "B", "C♯m", "D♯dim", "E"), Harmony(4, Scale.Major).chordPads(48).map { it.name(true) })
        assertEquals("A♯dim", Harmony(11, Scale.Major).chordPads(48)[6].name(true))
        assertEquals("Mi♯ dim", Harmony(6, Scale.Major).chordPads(48)[6].name(false))
        assertEquals("B♭", Harmony(5, Scale.Major).chordPads(48)[3].name(true))
        // The harmonic minor's III is augmented: III+, Do+.
        val harmonic = Harmony(9, Scale.HarmonicMinor).chordPads(48)
        assertEquals("III+", harmonic[2].roman)
        assertEquals("Do+", harmonic[2].name(false))
        assertEquals("C+", harmonic[2].name(true))
        assertEquals("Sol♯ dim", harmonic[6].name(false))
        // Keys: Mi♭ mineur writes Sol♭ (the « Gamme » keyboard and the title agree), Ré♭ majeur, Sol♯ mineur.
        val eFlatMinor = Harmony(3, Scale.Minor)
        assertEquals("Mi♭", eFlatMinor.keyName(false))
        assertEquals(listOf("Mi♭", "Fa", "Sol♭", "La♭", "Si♭", "Do♭", "Ré♭"), Scale.Minor.intervals.map { eFlatMinor.noteName(3 + it, false) })
        assertEquals("D♭", Harmony(1, Scale.Major).keyName(true))
        assertEquals("Sol♯", Harmony(8, Scale.Minor).keyName(false))
        // A note out of the key takes the key's accidentals: the blue note of La is Mi♭.
        assertEquals("Mi♭", Harmony(9, Scale.Blues).noteName(63, false))
        assertEquals("Ré♯", Harmony(4, Scale.Major).noteName(63, false))
        // Every key names every note, and seven letters for seven degrees.
        for (scale in Scale.entries) for (root in 0..11) {
            val h = Harmony(root, scale)
            assertEquals(7, h.letters().distinct().size)
            (0..11).forEach { assertTrue(h.noteName(it, true).isNotEmpty()) }
        }
    }

    @Test
    fun theAcidTabGeneratesInTheSongsKey() {
        for (scale in listOf(Scale.Minor, Scale.Dorian, Scale.Phrygian, Scale.MinorPentatonic)) {
            val harmony = Harmony(9, scale)
            // The 303 transposed a tone away from the key's root still plays the key's notes.
            for (acidRoot in listOf(45, 47)) {
                val locked = AcidStep(note = 1, gate = true)
                val project = Project(name = "Acid house", harmony = harmony, acid = AcidPattern(root = acidRoot, locked = setOf(5)).withStep(5, locked))
                val random = Random(7)
                repeat(200) {
                    val line = WizardAcid.generateFor(project, random)
                    assertEquals(locked, line.steps[5])
                    line.steps.take(line.length).filterIndexed { i, s -> s.gate && i != 5 }.forEach { s ->
                        assertTrue("$scale $acidRoot ${s.note}", harmony.inScale(line.root + s.note))
                    }
                }
            }
        }
        // Without a key: the generator as it was (any note), at the line's length.
        val free = WizardAcid.generateFor(Project(acid = AcidPattern(length = 32)), Random(1))
        assertEquals(32, free.length)
    }

    @Test
    fun leadsStayInAPlayableRange() {
        for (style in StarterWizard.STYLES) for (mood in Mood.entries) {
            val notes = StarterWizard.startingNotes(Part.Lead, style, mood, StarterWizard.harmonyOf(style, mood)).notes.map { it.pitch }.sorted()
            assertTrue("${style.id} $mood ${notes.last()}", notes.last() <= StarterWizard.LEAD_TOP)
            assertTrue("${style.id} $mood ${notes[notes.size / 2]}", notes[notes.size / 2] <= StarterWizard.LEAD_MIDDLE)
        }
        // The jungle's hoover is a mid-range rave lead, no longer a whistle at Sol6.
        val jungle = StarterWizard.startingNotes(Part.Lead, style("jungle"), Mood.Happy, StarterWizard.harmonyOf(style("jungle"), Mood.Happy)).notes
        assertTrue(jungle.maxOf { it.pitch } <= 76)
    }

    @Test
    fun aLeadRecordedAsAChordIsNotArpeggiated() {
        val stab = StarterWizard.PartSound("Stab 8 · Detroit", sampled = SampledPatch(file = "pack:detroit/stab8.wav"), chord = true, family = "stab")
        for (id in listOf("riddim", "acid_house")) {
            val s = style(id)
            assertTrue(s.melody.leadPattern.isBlank())
            val clip = StarterWizard.startingNotes(Part.Lead, s, s.melody.mood, StarterWizard.harmonyOf(s, s.melody.mood), stab)
            assertEquals(ArpMode.Off, clip.performance.arp)
            // One stab per hit: a chord of recorded chords never plays.
            assertTrue(id, clip.notes.groupBy { it.start }.values.all { it.size == 1 })
            assertTrue(id, clip.notes.size > 4)
        }
        // A written hook on a chord sample: one stab per note, in the second Refrain too (no octave doubling).
        for (id in listOf("house", "synthwave", "detroit")) {
            val s = style(id)
            for (v in StarterWizard.Version.entries) {
                val clip = StarterWizard.startingNotes(Part.Lead, s, s.melody.mood, StarterWizard.harmonyOf(s, s.melody.mood), stab, v)
                assertTrue("$id $v", clip.notes.groupBy { it.start }.values.all { it.size == 1 })
            }
        }
    }

    @Test
    fun aSus2NeverGrindsASemitone() {
        // The phrygian i: Fa♯-Sol-Do♯ was a minor second; it takes the fourth.
        assertEquals(listOf(54, 59, 61), Harmony(6, Scale.Phrygian).voiced(0, 48, Voicing.Sus2))
        assertEquals(listOf(50, 52, 57), Harmony(2, Scale.Minor).voiced(0, 48, Voicing.Sus2))
        for (style in StarterWizard.STYLES) for (mood in Mood.entries) {
            val pad = StarterWizard.startingNotes(Part.Pad, style, mood, StarterWizard.harmonyOf(style, mood)).notes
            pad.groupBy { it.start }.values.forEach { chord ->
                val p = chord.map { it.pitch }.sorted()
                assertFalse("${style.id} $mood $p", p.zipWithNext().any { (a, b) -> b - a == 1 })
            }
        }
    }

    @Test
    fun theHouseBassStaysUnderTheChords() {
        for (id in listOf("house", "uk_garage")) for (mood in Mood.entries) {
            val s = style(id)
            val h = StarterWizard.harmonyOf(s, mood)
            val bass = StarterWizard.startingNotes(Part.Bass, s, mood, h).notes
            val pad = StarterWizard.startingNotes(Part.Pad, s, mood, h).notes
            val bar = 16 * NoteClip.TICKS_PER_STEP
            for (b in 0 until 4) {
                val top = bass.filter { it.start / bar == b }.maxOfOrNull { it.pitch } ?: continue
                val low = pad.filter { it.start / bar == b }.minOfOrNull { it.pitch } ?: continue
                assertTrue("$id $mood bar $b: bass $top, chords from $low", top <= low)
            }
        }
    }

    @Test
    fun theAcidStylesKeepTheir303InTheBreak() {
        for (id in listOf("acid_house", "acid_techno")) {
            val s = style(id)
            assertTrue(StarterWizard.acidBreaks(s))
            val plan = StarterWizard.planFor(s, s.lines.map { it.role }.toSet(), setOf(Part.Pad, Part.Lead), acid = true)
            assertTrue(id, plan.hasBreak && 3 in plan.acid && 3 in plan.parts.getValue(Part.Pad))
            // Its filter sweep in the Break is written: the 303's curve moves there.
            val p = StarterWizard.build(s, s.lines.associate { it.role to null }, s.melody.mood,
                listOf(Part.Pad, Part.Lead).associateWith { StarterWizard.soundsFor(it, s).first() })
            val brk = StarterWizard.songSections(p).first { it.scene == 3 }
            assertTrue(p.allScenes[3].acid.steps.any { it.gate })
            val lane = p.automation.first { it.target.kind == AutoTarget.ACID && s.melody.acid!!.moves.any { m -> 3 in m.sections && AutoTarget.acid(m.param) == it.target } }
            val curve = lane.songCurve(p.songBars)
            assertTrue(id, curve[brk.start + brk.bars - 1] > curve[brk.start])
        }
    }

    @Test
    fun noSectionIsLeftSilent() {
        // Techno with the rumble alone, acidcore with the chords alone.
        val techno = style("techno")
        val rumble = StarterWizard.planFor(techno, setOf(Role.Rumble), emptySet(), acid = false)
        assertEquals((0 until rumble.count).toSet(), rumble.lines.getValue(Role.Rumble))
        val acidcore = style("acidcore")
        val chords = StarterWizard.planFor(acidcore, emptySet(), setOf(Part.Pad), acid = false)
        assertEquals((0 until chords.count).toSet(), chords.parts.getValue(Part.Pad))
        // Every style, every set of lines (one, all, all but the kick, the rumble alone) and parts.
        for (s in StarterWizard.STYLES) {
            val roles = s.lines.map { it.role }
            val lineSets = (roles.map { setOf(it) } + listOf(roles.toSet(), roles.toSet() - Role.Kick, emptySet())).distinct()
            for (lines in lineSets) for (mask in 0 until 8) for (acid in listOf(false, true)) {
                val parts = Part.entries.filterIndexed { i, _ -> mask and (1 shl i) != 0 }.toSet()
                val withAcid = acid && s.melody.acid != null
                if (lines.isEmpty() && parts.isEmpty() && !withAcid) continue
                val plan = StarterWizard.planFor(s, lines, parts, withAcid)
                for (i in 0 until plan.count) {
                    val plays = plan.lines.values.any { i in it } || plan.parts.values.any { i in it } || i in plan.acid
                    assertTrue("${s.id} $lines $parts $withAcid: section $i silent", plays)
                }
                assertTrue("${s.id} $lines $parts", StarterWizard.songSeconds(s, plan) <= StarterWizard.maxSongSeconds(s))
            }
        }
    }

    @Test
    fun theChordsTrackIsNamedAfterItsSound() {
        fun name(id: String) = style(id).let { s -> StarterWizard.trackName(Part.Pad, StarterWizard.soundsFor(Part.Pad, s).first(), s) }
        assertEquals("Piano", name("house"))
        assertEquals("Orgue", name("tech_house"))
        assertEquals("Piano", name("uk_garage"))
        assertEquals("Stabs", name("techno"))
        assertEquals("Nappe", name("dnb"))
        val pack = StarterWizard.PartSound("Pad 2 · Detroit", chord = true, family = "pad")
        assertEquals("Nappe", StarterWizard.trackName(Part.Pad, pack, style("house")))
        // Saved in French, translated at display.
        com.pocketstudio.model.i18n.I18n.lang = com.pocketstudio.model.i18n.Lang.EN
        try {
            StarterWizard.CHORD_TRACK_NAMES.forEach { assertTrue(it, DefaultNames.isDefault(it)) }
            assertEquals("Organ", DefaultNames.shown("Orgue"))
            assertEquals("Keyboard", DefaultNames.shown("Clavier"))
        } finally {
            com.pocketstudio.model.i18n.I18n.lang = com.pocketstudio.model.i18n.Lang.FR
        }
    }

    /**
     * The auditor's probe as a test: every style × mood × set of parts × 303 or not, all its drum
     * lines: every note in the locked scale, sane ranges, every chosen part and line heard, no silent
     * section, no song over 3 min 30.
     */
    @Test
    fun everyCombinationOfTheAssistantSoundsRight() {
        for (s in StarterWizard.STYLES) {
            val lines = s.lines.associate { it.role to null as PadSample? }
            val sounds = Part.entries.associateWith { StarterWizard.soundsFor(it, s).first() }
            for (mood in Mood.entries) for (mask in 1 until 8) for (acid in listOf(true, false)) {
                if (!acid && s.melody.acid == null) continue
                val parts = sounds.filterKeys { Part.entries.indexOf(it).let { i -> mask and (1 shl i) != 0 } }
                val p = StarterWizard.build(s, lines, mood, parts, acid = acid)
                val what = "${s.id} $mood ${parts.keys} acid=$acid"
                val h = p.harmony
                for (t in p.instrumentTracks.filter { it.name != StarterWizard.RISER_TRACK }) {
                    val notes = p.allScenes.flatMap { sc -> (if (t.id == 0) sc.synthNotes else sc.trackNotes[t.id] ?: NoteClip()).notes }
                    assertTrue("$what ${t.name} never plays", notes.isNotEmpty())
                    assertTrue("$what ${t.name} out of the key", notes.all { h.inScale(it.pitch) })
                    val range = when (t.name) {
                        // The trap 808 goes down to Do♯1 (35 Hz), as the genre does.
                        "Basse", StarterWizard.BASS_SWITCH_TRACK -> 24..72
                        // The jump up's clean sub, an octave under its growl (Fa1, 44 Hz).
                        StarterWizard.SUB_TRACK -> 24..60
                        // The sub drop slides from two octaves over the key's root down to it (Do1 to Si1).
                        StarterWizard.SUB_DROP_TRACK -> 24..72
                        "Lead" -> 36..StarterWizard.LEAD_TOP
                        else -> 36..96
                    }
                    assertTrue("$what ${t.name} ${notes.minOf { it.pitch }}..${notes.maxOf { it.pitch }}", notes.all { it.pitch in range })
                }
                if (acid && s.melody.acid != null) {
                    val steps = p.allScenes.flatMap { sc -> sc.acid.steps.take(sc.acid.length).filter { it.gate } }
                    assertTrue("$what 303 silent", steps.isNotEmpty())
                    assertTrue("$what 303 out of the key", steps.all { h.inScale(p.acid.root + it.note) })
                }
                for (role in lines.keys) assertTrue("$what ${role.id} never plays",
                    p.allScenes.any { sc -> sc.drums.lanes[role.lane].hits.isNotEmpty() })
                p.allScenes.forEachIndexed { i, sc -> assertFalse("$what scene $i silent", sc.isEmpty) }
                val seconds = p.songBars * p.beatsPerBar * 60.0 / p.tempoBpm
                assertTrue("$what $seconds s", seconds <= StarterWizard.maxSongSeconds(s))
            }
            // Fewer drum lines (one alone, all but the kick, none), in the style's own mood: every chosen layer heard, no silent scene.
            val roles = s.lines.map { it.role }
            val lineSets = (roles.map { setOf(it) } + listOf(roles.toSet() - Role.Kick, emptySet())).distinct()
            for (chosen in lineSets) for (mask in 0 until 8) for (acid in listOf(true, false)) {
                if (!acid && s.melody.acid == null) continue
                val parts = sounds.filterKeys { Part.entries.indexOf(it).let { i -> mask and (1 shl i) != 0 } }
                if (chosen.isEmpty() && parts.isEmpty() && !(acid && s.melody.acid != null)) continue
                val p = StarterWizard.build(s, chosen.associateWith { null }, s.melody.mood, parts, acid = acid)
                val what = "${s.id} ${chosen.map { it.id }} ${parts.keys} acid=$acid"
                p.allScenes.forEachIndexed { i, sc -> assertFalse("$what scene $i silent", sc.isEmpty) }
                for (role in chosen) assertTrue("$what ${role.id} never plays", p.allScenes.any { sc -> sc.drums.lanes[role.lane].hits.isNotEmpty() })
                val partTracks = p.instrumentTracks.filter { it.name in listOf("Basse", "Lead") + StarterWizard.CHORD_TRACK_NAMES }
                // The 808 styles' bass plays on its pad (S3), heard there.
                val on808 = StudioDrums.bassOn808(s, parts[Part.Bass])
                assertEquals(what, parts.size - (if (on808) 1 else 0), partTracks.size)
                if (on808) assertTrue("$what 808 never plays", p.allScenes.any { sc -> sc.drums.lanes[StudioDrums.LANE_808].hits.isNotEmpty() })
                for (t in partTracks) assertTrue("$what ${t.name} never plays",
                    p.allScenes.any { sc -> (if (t.id == 0) sc.synthNotes else sc.trackNotes[t.id] ?: NoteClip()).notes.isNotEmpty() })
            }
        }
    }
}
