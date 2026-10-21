package com.pocketstudio.model

import com.pocketstudio.model.i18n.tr
import kotlin.random.Random

/**
 * Templates by genre (Mes projets › Nouveau projet): tempo, groove, key, sounds and a first loop
 * ready to play, so a song starts from something that already sounds like the style.
 */
object Genres {
    /** Title, one-line description, and how to build it. */
    val ALL: List<Triple<String, String, () -> Project>> = listOf(
        Triple("House", "124 BPM, grosse caisse à chaque temps, charley entre les temps, accords en La mineur.", ::house),
        Triple("Techno", "130 BPM, rythme sec et hypnotique, ligne acide et percussions en Ré mineur.", ::techno),
        Triple("Drum & bass", "174 BPM, rythme cassé rapide et basse reese profonde en Fa mineur.", ::dnb),
        Triple("Trap", "140 BPM (rythme à demi-temps), charleys en roulements et basse 808 en Do mineur.", ::trap),
        Triple("Lo-fi", "84 BPM, groove chaloupé et piano électrique mélancolique en Mi bémol majeur.", ::lofi),
        Triple("Synthwave", "100 BPM, batterie années 80, basse en croches et nappe en La mineur.", ::synthwave),
    )

    private fun drums(steps: Int = 16, vararg lanes: Pair<Int, List<Int>>, velocity: (Int, Int) -> Float = { _, _ -> 1f }): DrumPattern {
        var d = DrumPattern().withSteps(steps)
        lanes.forEach { (lane, at) -> at.filter { it < steps }.forEach { d = d.toggle(lane, it, velocity(lane, it)) } }
        return d
    }

    private fun synth(name: String) = SoundLibrary.SYNTH.first { it.name == name }.sound
    private fun acidSound(name: String) = SoundLibrary.ACID.first { it.name == name }.sound

    /** A bass track playing [notes] (pitch, start step, length in steps) with [sound]. */
    private fun Project.withBass(name: String, sound: SynthPatch, notes: List<Triple<Int, Int, Int>>, steps: Int = 16): Project {
        val added = addTrack(name) ?: return this
        val id = added.tracks.last().id
        val clip = NoteClip(steps = steps, notes = notes.map { (p, s, l) -> Note(s * NoteClip.TICKS_PER_STEP, l * NoteClip.TICKS_PER_STEP, p, 0.9f) })
        return added.withTrack(added.track(id).copy(synth = sound, notes = clip.sanitized()))
    }

    private fun house(): Project {
        val beat = drums(16, 0 to listOf(0, 4, 8, 12), 4 to listOf(4, 12), 6 to listOf(2, 6, 10, 14), 5 to (0 until 16 step 2).toList()) { lane, step ->
            if (lane == 5) 0.45f else if (lane == 6) 0.8f else 1f
        }
        val harmony = Harmony(9, Scale.Minor)  // A minor
        val chords = NoteClip(steps = 64).withProgression(Progression.House, harmony, octaveRoot = 57)
        return Project(
            name = "House", tempoBpm = 124.0, drums = beat, harmony = harmony, synth = synth("Nappe"), synthNotes = chords,
            acid = AcidPattern(),
        ).withBass(tr("Basse"), synth("Basse ronde"), listOf(Triple(33, 2, 1), Triple(33, 6, 1), Triple(33, 10, 1), Triple(36, 14, 1))).sanitized()
    }

    private fun techno(): Project {
        val beat = drums(16, 0 to listOf(0, 4, 8, 12), 5 to (0 until 16).toList(), 10 to listOf(3, 11), 4 to listOf(4, 12), 13 to listOf(2, 6, 10, 14)) { lane, step ->
            when (lane) { 5 -> if (step % 4 == 2) 0.7f else 0.3f; 13 -> 0.5f; 4 -> 0.6f; else -> 1f }
        }
        val acid = AcidGenerator(Random(130)).generate(AcidPattern(root = 38, density = 0.7f), AcidGenerator.NOTE_SETS[0])
        return Project(name = "Techno", tempoBpm = 130.0, drums = beat, harmony = Harmony(2, Scale.Minor), acid = acid.copy(sound = acidSound("Squelchy")))
            .sanitized()
    }

    private fun dnb(): Project {
        // The two-step: kick on 1 and the "and" of 3, snare on 2 and 4, busy hats.
        val beat = drums(16, 0 to listOf(0, 10), 2 to listOf(4, 12), 5 to (0 until 16 step 2).toList(), 13 to listOf(3, 7, 11, 15), 6 to listOf(14)) { lane, _ ->
            if (lane == 5) 0.5f else if (lane == 13) 0.35f else 1f
        }
        val harmony = Harmony(5, Scale.Minor)  // F minor
        return Project(name = "Drum & bass", tempoBpm = 174.0, drums = beat, harmony = harmony, synth = synth("Nappe chaude"),
            synthNotes = NoteClip(steps = 64).withProgression(Progression.Epic, harmony, octaveRoot = 65), acid = AcidPattern())
            .withBass("Reese", synth("Basse reese"), listOf(Triple(29, 0, 6), Triple(29, 8, 2), Triple(32, 10, 6)))
            .sanitized()
    }

    private fun trap(): Project {
        // Half-time: kick and snare on beat 3; hats in rolls.
        var beat = drums(16, 0 to listOf(0, 7, 11), 4 to listOf(8), 2 to listOf(8), 5 to (0 until 16 step 2).toList(), 6 to listOf(15)) { lane, step ->
            if (lane == 5) (if (step % 4 == 0) 0.7f else 0.45f) else 1f
        }
        beat = beat.cycleRepeats(5, 6).cycleRepeats(5, 6).cycleRepeats(5, 14)  // ×3 and ×2 rolls
        val harmony = Harmony(0, Scale.Minor)  // C minor
        return Project(name = "Trap", tempoBpm = 140.0, drums = beat, harmony = harmony, synth = synth("Cloche"),
            synthNotes = NoteClip(notes = listOf(Note(0, 48, 75), Note(48, 24, 72), Note(96, 48, 70), Note(144, 24, 67))), acid = AcidPattern())
            .withBass("808", synth("Basse sub"), listOf(Triple(24, 0, 6), Triple(24, 7, 3), Triple(27, 11, 4)))
            .sanitized()
    }

    private fun lofi(): Project {
        val beat = drums(16, 0 to listOf(0, 7, 10), 2 to listOf(4, 12), 13 to (0 until 16).toList()) { lane, step ->
            if (lane == 13) (if (step % 2 == 0) 0.4f else 0.2f) else if (lane == 0 && step != 0) 0.75f else 1f
        }
        val harmony = Harmony(3, Scale.Major)  // E flat major
        return Project(name = "Lo-fi", tempoBpm = 84.0, swing = 0.3, drums = beat, harmony = harmony, synth = synth("Piano électrique"),
            synthNotes = NoteClip(steps = 64).withProgression(Progression.Jazz, harmony, octaveRoot = 63), acid = AcidPattern())
            .withBass(tr("Basse"), synth("Basse sub"), listOf(Triple(39, 0, 3), Triple(41, 6, 2), Triple(43, 10, 4)))
            .sanitized()
    }

    private fun synthwave(): Project {
        val beat = drums(16, 0 to listOf(0, 8), 2 to listOf(4, 12), 5 to (0 until 16 step 2).toList(), 7 to listOf(14), 8 to listOf(15)) { lane, _ ->
            if (lane == 5) 0.5f else 1f
        }
        val harmony = Harmony(9, Scale.Minor)
        // Eighth-note bass on the root, the way the 80s did it.
        val bass = (0 until 16 step 2).map { Triple(33, it, 1) }
        return Project(name = "Synthwave", tempoBpm = 100.0, drums = beat, harmony = harmony, synth = synth("Cordes"),
            synthNotes = NoteClip(steps = 64).withProgression(Progression.Epic, harmony, octaveRoot = 57), acid = AcidPattern())
            .withBass(tr("Basse"), synth("Basse ronde"), bass)
            .sanitized()
    }
}
