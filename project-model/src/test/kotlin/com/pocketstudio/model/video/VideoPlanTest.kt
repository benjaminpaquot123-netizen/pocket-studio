package com.pocketstudio.model.video

import com.pocketstudio.model.DrumPattern
import com.pocketstudio.model.NoteClip
import com.pocketstudio.model.Note
import com.pocketstudio.model.Project
import com.pocketstudio.model.Scene
import com.pocketstudio.model.SongPart
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class VideoPlanTest {
    /** One bar per scene (2 s at 120 BPM); [lanes] drum lanes with a hit, [notes] a synth note. */
    private fun scene(letter: Int, label: String, lanes: Int = 1, notes: Boolean = false): Scene {
        var d = DrumPattern()
        repeat(lanes) { d = d.toggle(it, 0) }
        return Scene(name = Scene.NAMES[letter], drums = d, label = label,
            synthNotes = if (notes) NoteClip(notes = listOf(Note(0, 24, 60))) else NoteClip())
    }

    private fun song(scenes: List<Scene>, parts: List<SongPart>, bpm: Double = 120.0): Project {
        val first = scenes[0]
        return Project(tempoBpm = bpm, drums = first.drums, synthNotes = first.synthNotes, scenes = scenes, song = parts)
    }

    private val intro = scene(0, "Intro")
    private val verse = scene(1, "Couplet", lanes = 3)
    private val drop = scene(2, "Drop", lanes = 2)

    @Test
    fun theFirstDropIsChosenAndTheWindowStaysInsideTheSong() {
        // Intro 8 s, Couplet 8 s, Drop 16 s, Couplet 8 s: 40 s in all.
        val p = song(listOf(intro, verse, drop), listOf(SongPart(0, 4), SongPart(1, 4), SongPart(2, 8), SongPart(1, 4)))
        assertEquals(2, VideoPlanner.pickPart(p))
        val plan = VideoPlanner.plan(p, "Mon tube")
        assertEquals(30.0, plan.seconds, 1e-9)
        // From 16 s the song would end first: 10 s, on a bar line (2 s).
        assertEquals(10L * 48_000, plan.startFrame)
        assertEquals(40L * 48_000, plan.renderFrames)
        assertEquals(listOf("Couplet", "Drop", "Couplet"), plan.sections.map { it.label })
        assertEquals(6.0, plan.sections[1].start, 1e-9)
        assertEquals(22.0, plan.sections[1].end, 1e-9)
        assertEquals(30.0, plan.sections.last().end, 1e-9)
        assertEquals(1, plan.sectionAt(6.0))
        assertEquals(0, plan.sectionAt(5.99))
        assertEquals(2, plan.sectionAt(31.0))
    }

    @Test
    fun aDropFarFromTheEndStartsTheVideo() {
        val p = song(listOf(intro, verse, drop), listOf(SongPart(0, 4), SongPart(2, 16), SongPart(1, 8)))
        val plan = VideoPlanner.plan(p, "x")
        assertEquals(8L * 48_000, plan.startFrame)
        assertEquals("Drop", plan.sections.first().label)
        assertEquals(0.0, plan.sections.first().start, 1e-9)
        assertEquals(1, plan.sections.size)
    }

    @Test
    fun aRefrainIsTakenWhenThereIsNoDropAndCopiesCountAsTheirSection() {
        val chorus2 = scene(2, "Refrain 2", lanes = 1)
        val p = song(listOf(intro, verse, chorus2), listOf(SongPart(0, 2), SongPart(1, 2), SongPart(2, 20)))
        assertEquals("Refrain", VideoPlanner.baseSection("Refrain 2"))
        assertEquals("Drop", VideoPlanner.baseSection("Drop 2 +"))
        assertEquals(2, VideoPlanner.pickPart(p))
    }

    @Test
    fun withoutNamesTheFullestPartIsChosen() {
        val a = scene(0, "", lanes = 1)
        val b = scene(1, "", lanes = 2, notes = true)
        val c = scene(2, "", lanes = 4)
        val p = song(listOf(a, b, c), listOf(SongPart(0, 4), SongPart(1, 4), SongPart(2, 4), SongPart(1, 4)))
        // b: 2 lanes + a synth track (2) = 4, c: 4 lanes = 4: the first of the fullest.
        assertEquals(1, VideoPlanner.pickPart(p))
        // A part whose tracks are muted counts what is left.
        val muted = p.copy(song = p.song.mapIndexed { i, s -> if (i == 1 || i == 3) s.copy(muted = setOf(SongPart.inst(0))) else s })
        assertEquals(2, VideoPlanner.pickPart(muted))
        val plan = VideoPlanner.plan(muted, "x")
        assertEquals(2, plan.part)
        // 32 s of song: the window goes back to 2 s, and the parts without a name show their letter.
        assertEquals(listOf("Partie A", "Partie B", "Partie C", "Partie B"), plan.sections.map { it.label })
    }

    @Test
    fun aShortSongIsTakenWholeAndALoopFor30Seconds() {
        val p = song(listOf(intro, drop), listOf(SongPart(0, 2), SongPart(1, 6)))  // 16 s
        val plan = VideoPlanner.plan(p, "x")
        assertEquals(0L, plan.startFrame)
        assertEquals(16L * 48_000, plan.frames)
        assertEquals(listOf("Intro", "Drop"), plan.sections.map { it.label })

        val loop = VideoPlanner.plan(Project(drums = DrumPattern().toggle(0, 0)), "x")
        assertEquals(-1, loop.part)
        assertEquals(30L * 48_000, loop.frames)
        assertEquals(1, loop.sections.size)
    }

    @Test
    fun imagesAreEvenlyTimedAndCoverTheSound() {
        val plan = VideoPlan("x", startFrame = 48_000, frames = 30L * 48_000, sections = emptyList())
        assertEquals(900, plan.videoFrames)
        assertEquals(0L, plan.ptsUs(0))
        assertEquals(33_333L, plan.ptsUs(1))
        assertEquals(1_000_000L, plan.ptsUs(30))
        assertTrue(plan.ptsUs(899) < 30_000_000L)
        (1 until plan.videoFrames).forEach { assertTrue(plan.ptsUs(it) > plan.ptsUs(it - 1)) }
        assertEquals(1_600L, plan.audioFrameOf(1))
        assertEquals(plan.frames, plan.audioFrameOf(plan.videoFrames))
        // A length that is not a whole number of images still gets its last one.
        assertEquals(31, plan.copy(frames = 48_000 + 100).videoFrames)
    }

    @Test
    fun theSoundFadesInWhereItStartsAndOutAtTheEnd() {
        val plan = VideoPlan("x", startFrame = 96_000, frames = 30L * 48_000, sections = emptyList())
        assertEquals(0f, plan.gainAt(0), 0f)
        assertEquals(0.5f, plan.gainAt(1_200), 1e-3f)
        assertEquals(1f, plan.gainAt(48_000), 0f)
        assertEquals(1f, plan.gainAt(plan.frames - 72_001), 1e-4f)
        assertEquals(0.5f, plan.gainAt(plan.frames - 36_000), 1e-3f)
        assertEquals(0f, plan.gainAt(plan.frames), 0f)
        // From the song's very start: no fade in.
        assertEquals(1f, plan.copy(startFrame = 0).gainAt(0), 0f)
    }

    @Test
    fun levelsFollowTheSound() {
        val plan = VideoPlan("x", startFrame = 0, frames = 48_000, sections = emptyList())
        val pcm = FloatArray(2 * 48_000) { i ->
            val f = i / 2
            if (f < 24_000) 0f else (0.5 * sin(2 * PI * 440 * f / 48_000)).toFloat()
        }
        val levels = VideoLevels.of(plan, pcm)
        assertEquals(30, levels.rms.size)
        assertEquals(0f, levels.rms[0], 0f)
        assertEquals(0.3536f, levels.rms[20], 0.01f)
        assertEquals(0.5f, levels.peak[20], 0.01f)
        assertEquals(0f, VideoLevels.shown(0f), 0f)
        assertEquals(1f, VideoLevels.shown(1f), 1e-6f)
        assertEquals(0.5f, VideoLevels.shown(Math.pow(10.0, -24.0 / 20).toFloat()), 1e-4f)
    }
}
