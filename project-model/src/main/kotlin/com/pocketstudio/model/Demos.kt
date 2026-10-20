package com.pocketstudio.model

import kotlin.random.Random

/** Demo projects to listen to and take apart (Mes projets › Nouveau projet). Same result every time. */
object Demos {
    /** Title, one-line description, and how to build it. */
    val ALL: List<Triple<String, String, () -> Project>> = listOf(
        Triple("Démo · Acid house", "126 BPM, deux scènes et un morceau : batterie 4/4 et ligne acide qui monte.", ::acidHouse),
        Triple("Démo · Funk en Sol mineur", "111 BPM, groove funky, accords au synthé et basse acide douce.", ::funk),
        Triple("Démo · Chill", "88 BPM, rythme posé et piano électrique sur une progression mélancolique.", ::chill),
    )

    private fun drums(vararg lanes: Pair<Int, List<Int>>, velocity: (Int, Int) -> Float = { _, _ -> 1f }): DrumPattern {
        var d = DrumPattern()
        lanes.forEach { (lane, steps) -> steps.forEach { d = d.toggle(lane, it, velocity(lane, it)) } }
        return d
    }

    private fun acidHouse(): Project {
        val beatA = drums(0 to listOf(0, 4, 8, 12), 4 to listOf(4, 12), 5 to listOf(2, 6, 10, 14)) { lane, step -> if (lane == 5 && step % 4 == 2) 0.5f else 1f }
        val beatB = drums(0 to listOf(0, 4, 8, 12), 4 to listOf(4, 12), 5 to listOf(2, 6, 10, 14), 6 to listOf(14), 12 to listOf(0)) { lane, step -> if (lane == 5) 0.5f else 1f }
        val acidA = AcidGenerator(Random(303)).generate(AcidPattern(root = 36), AcidGenerator.NOTE_SETS[0])
        val acidB = AcidGenerator(Random(909)).generate(AcidPattern(root = 36, density = 0.8f), AcidGenerator.NOTE_SETS[0])
        val sound = SoundLibrary.ACID.first { it.name == "Squelchy" }.sound
        val a = Project(name = "Démo · Acid house", tempoBpm = 126.0, drums = beatA, acid = acidA.copy(sound = sound))
        return a.copySceneTo(1).switchScene(1).let { it.copy(drums = beatB.copy(lanes = beatB.lanes), acid = it.acid.copy(steps = acidB.steps)) }
            .switchScene(0).copy(song = listOf(SongPart(0, 2), SongPart(1, 2), SongPart(0, 1), SongPart(1, 2))).sanitized()
    }

    private fun funk(): Project {
        val beat = drums(
            0 to listOf(0, 7, 10), 2 to listOf(4, 12), 5 to listOf(0, 2, 4, 6, 8, 10, 12, 14), 6 to listOf(15), 10 to listOf(11),
        ) { lane, step -> if (lane == 5 && step % 4 != 0) 0.6f else if (lane == 10) 0.5f else 1f }
        val harmony = Harmony(7, Scale.Minor)  // G minor
        val notes = NoteClip().withProgression(Progression.Sad, harmony, octaveRoot = 55)
        val acid = AcidGenerator(Random(111)).generate(AcidPattern(root = 31, density = 0.55f), AcidGenerator.NOTE_SETS[2])
        val soft = SoundLibrary.ACID.first { it.name == "Basse ronde" }.sound
        return Project(
            name = "Démo · Funk en Sol mineur", tempoBpm = 111.0, swing = 0.15, drums = beat, harmony = harmony,
            synth = SoundLibrary.SYNTH.first { it.name == "Orgue" }.sound, synthNotes = notes, acid = acid.copy(sound = soft),
        ).sanitized()
    }

    private fun chill(): Project {
        val beat = drums(0 to listOf(0, 10), 2 to listOf(4, 12), 13 to (0 until 16).toList()) { lane, step ->
            if (lane == 13) (if (step % 2 == 0) 0.5f else 0.2f) else 1f
        }
        val harmony = Harmony(0, Scale.Major)
        val notes = NoteClip().withProgression(Progression.Sad, harmony, octaveRoot = 60)
        return Project(
            name = "Démo · Chill", tempoBpm = 88.0, swing = 0.25, drums = beat, harmony = harmony,
            synth = SoundLibrary.SYNTH.first { it.name == "Piano électrique" }.sound, synthNotes = notes,
            acid = AcidPattern(),  // no acid line: drums and piano only
        ).sanitized()
    }
}
