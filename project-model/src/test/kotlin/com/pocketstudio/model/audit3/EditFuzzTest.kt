package com.pocketstudio.model.audit3

import com.pocketstudio.model.Note
import com.pocketstudio.model.PadLayer
import com.pocketstudio.model.PadSample
import com.pocketstudio.model.Project
import com.pocketstudio.model.ProjectJson
import com.pocketstudio.model.ProjectSession
import com.pocketstudio.model.ProjectStore
import com.pocketstudio.model.SongPart
import com.pocketstudio.model.addTrack
import com.pocketstudio.model.copySceneTo
import com.pocketstudio.model.duplicateSongPart
import com.pocketstudio.model.duplicateTrack
import com.pocketstudio.model.insertSongPart
import com.pocketstudio.model.moveScene
import com.pocketstudio.model.moveSongPart
import com.pocketstudio.model.removeSongPart
import com.pocketstudio.model.removeTrack
import com.pocketstudio.model.renameScene
import com.pocketstudio.model.sampleFiles
import com.pocketstudio.model.switchScene
import com.pocketstudio.model.track
import com.pocketstudio.model.withBeatsPerBar
import com.pocketstudio.model.withPartBars
import com.pocketstudio.model.withSongRepeats
import com.pocketstudio.model.withTrack
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.random.Random

/**
 * Audit stage 3, « fuzzing et propriétés »: thousands of random edits (seeded, replayable: the
 * failure names its seed and step) on the project through its session, with the invariants
 *  - no crash, no NaN nor infinity written;
 *  - the project read back from its file is the same (save, reopen);
 *  - « Annuler » returns exactly to the step before, « Rétablir » to the step after;
 *  - nothing lost: a sound the project names stays named until an edit takes it away;
 *  - a full undo goes back to the start, and the last save on disk is the project.
 * Values are thrown in wild (NaN, infinities, negative, huge) as a crafted file or a bug would.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EditFuzzTest {
    @get:Rule val tmp = TemporaryFolder()

    private val wild = floatArrayOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, -1e9f, 1e9f, -0f, 0f, 0.5f, 1f, 2f, -3f)

    private fun Random.wildFloat(lo: Float, hi: Float) = if (nextInt(5) == 0) wild[nextInt(wild.size)] else lo + nextFloat() * (hi - lo)
    private fun Random.wildInt(lo: Int, hi: Int) = when (nextInt(8)) {
        0 -> Int.MIN_VALUE
        1 -> Int.MAX_VALUE
        2 -> -1
        else -> lo + nextInt(hi - lo + 1)
    }

    /** One random edit: its name (for the failure's message) and what it does. */
    private fun Random.edit(p: Project): Pair<String, (Project) -> Project> {
        val lane = nextInt(-1, 17).coerceAtLeast(0).coerceAtMost(p.drums.lanes.size - 1)
        val step = wildInt(0, p.drums.steps - 1).coerceIn(0, p.drums.steps - 1)
        return when (nextInt(30)) {
            0, 1, 2, 3 -> "toggle $lane $step" to { q -> q.copy(drums = q.drums.toggle(lane, step, wildFloat(0f, 1f))) }
            4 -> "velocity" to { q -> q.copy(drums = q.drums.withHitVelocity(lane, step, wildFloat(0f, 1f))) }
            5 -> "roll" to { q -> q.copy(drums = q.drums.withRoll(lane, step, wildInt(1, 8), nextInt(-2, 3), wildInt(1, 4))) }
            6 -> "spread" to { q -> q.copy(drums = q.drums.withSpread(lane, step, wildInt(1, 8), wildInt(1, 4))) }
            7 -> "chance" to { q -> q.copy(drums = q.drums.withHitChance(lane, step, wildInt(0, 100))) }
            8 -> "pad pan" to { q -> q.copy(drums = q.drums.withPadPan(lane, wildFloat(-1f, 1f))) }
            9 -> "hit detail" to { q -> q.copy(drums = q.drums.withHitDetail(lane, step, wildFloat(-1f, 1f), wildInt(-24, 24))) }
            10 -> "lane length" to { q -> q.copy(drums = q.drums.withLaneLength(lane, wildInt(0, 64))) }
            11 -> "steps" to { q -> q.copy(drums = q.drums.withSteps(listOf(16, 32, 64, 7, 0, -16, 4096)[nextInt(7)])) }
            12 -> "sample" to { q ->
                val name = listOf("kick.wav", "snare-2.wav", "../x.wav", "", ".hidden", "a".repeat(200))[nextInt(6)]
                val s = PadSample(name, "Son", start = wildFloat(0f, 1f), end = wildFloat(0f, 1f), pitch = wildFloat(-24f, 24f), gain = wildFloat(0f, 2f),
                    layers = List(nextInt(0, 5)) { PadLayer("l$it.wav", "L$it", wildInt(0, 127)) })
                q.copy(drums = q.drums.withSample(lane, if (nextInt(4) == 0) null else s))
            }
            13 -> "tempo" to { q -> q.copy(tempoBpm = if (nextInt(4) == 0) wild[nextInt(wild.size)].toDouble() else 40.0 + nextDouble() * 260) }
            14 -> "swing" to { q -> q.copy(swing = wildFloat(0f, 0.7f).toDouble()) }
            15 -> "name" to { q -> q.copy(name = listOf("", "Mon projet", "x".repeat(500), "\u0000\n\"'", "ب".repeat(3))[nextInt(5)]) }
            16 -> "mixer" to { q ->
                val ch = nextInt(q.mixer.channels.size)
                q.copy(mixer = q.mixer.copy(channels = q.mixer.channels.mapIndexed { i, c -> if (i == ch) c.copy(volume = wildFloat(0f, 1f), pan = wildFloat(-1f, 1f), mute = nextBoolean()) else c }))
            }
            17 -> "scene" to { q -> q.switchScene(nextInt(-1, 9).coerceAtLeast(0)) }
            18 -> "copy scene" to { q -> q.copySceneTo(nextInt(0, 8)) }
            19 -> "move scene" to { q -> q.moveScene(nextInt(0, 8), nextInt(0, 8), nextBoolean()) }
            20 -> "rename scene" to { q -> q.renameScene(nextInt(-1, 9), listOf("Drop", "", "x".repeat(300))[nextInt(3)]) }
            21 -> "song part" to { q -> q.insertSongPart(wildInt(0, q.song.size).coerceIn(0, q.song.size), nextInt(-2, 9)) }
            22 -> "song edit" to { q ->
                if (q.song.isEmpty()) q else when (nextInt(5)) {
                    0 -> q.removeSongPart(nextInt(q.song.size))
                    1 -> q.duplicateSongPart(nextInt(q.song.size))
                    2 -> q.moveSongPart(nextInt(q.song.size), nextInt(q.song.size))
                    3 -> q.withSongRepeats(nextInt(q.song.size), wildInt(1, 8))
                    else -> q.withPartBars(nextInt(q.song.size), wildInt(1, 16))
                }
            }
            23 -> "beats per bar" to { q -> q.withBeatsPerBar(wildInt(2, 7)) }
            24 -> "add track" to { q -> q.addTrack(listOf(null, "Basse", "x".repeat(100))[nextInt(3)], nextInt(0, 5)) ?: q }
            25 -> "track edit" to { q ->
                val id = q.tracks.randomOrNull(this)?.id ?: 0
                when (nextInt(3)) {
                    0 -> q.removeTrack(id)
                    1 -> q.duplicateTrack(id) ?: q
                    else -> {
                        val t = q.track(id)
                        val notes = List(nextInt(0, 6)) { Note(wildInt(0, 400), wildInt(1, 200), wildInt(0, 127), wildFloat(0f, 1f)) }
                        q.withTrack(t.copy(notes = t.notes.copy(notes = t.notes.notes + notes)))
                    }
                }
            }
            26 -> "song raw" to { q -> q.copy(song = q.song + SongPart(scene = wildInt(0, 8), repeats = wildInt(1, 8), bars = if (nextBoolean()) wildInt(1, 32) else null)) }
            27 -> "clear lane" to { q -> q.copy(drums = q.drums.copy(lanes = q.drums.lanes.mapIndexed { i, l -> if (i == lane) l.copy(hits = emptyMap()) else l })) }
            28 -> "steps random" to { q -> q.copy(drums = q.drums.withSteps(wildInt(1, 256))) }
            else -> "nothing" to { q -> q }
        }
    }

    private fun run(seed: Long, steps: Int) = runTest {
        val rnd = Random(seed)
        val store = ProjectStore(tmp.newFolder("s$seed"))
        val io = StandardTestDispatcher(testScheduler)
        val session = ProjectSession("p", Project(name = "Fuzz"), store, this, io, clock = { testScheduler.currentTime })
        val history = ArrayList<Project>().apply { add(session.project.value) }
        for (i in 0 until steps) {
            val where = "seed $seed, step $i"
            val before = session.project.value
            val (what, change) = rnd.edit(before)
            try {
                session.edit(tag = what, transform = change)
            } catch (e: Throwable) {
                throw AssertionError("$where: « $what » crashed", e)
            }
            val after = session.project.value
            if (after != before) history += after
            // No NaN, no infinity; read back from its file, the same project.
            val text = runCatching { ProjectJson.encode(after) }.getOrElse { throw AssertionError("$where: « $what » cannot be saved", it) }
            assertFalse("$where: « $what » wrote NaN", text.contains("NaN") || text.contains("Infinity"))
            val back = ProjectJson.decode(text)
            if (back != after) {
                val diff = Project::class.java.declaredFields.filter { f -> f.isAccessible = true; f.get(back) != f.get(after) }.map { it.name }
                throw AssertionError("$where: « $what » reads back otherwise ($diff)")
            }
            // Undo goes back exactly, redo forward again.
            if (after != before) {
                session.undo()
                assertEquals("$where: undo of « $what »", before, session.project.value)
                session.redo()
                assertEquals("$where: redo of « $what »", after, session.project.value)
            }
            // Nothing lost: sounds go only with an edit that touches pads or scenes.
            val lost = before.sampleFiles - after.sampleFiles
            if (lost.isNotEmpty()) assertTrue("$where: « $what » lost $lost", what in setOf("sample", "scene", "copy scene", "move scene", "steps", "clear lane"))
            if (i % 97 == 0) testScheduler.advanceTimeBy(5_000)
        }
        assertTrue("seed $seed: too few changes (${history.size})", history.size > steps / 4)
        // The whole way back, then the file.
        while (session.canUndo.value) session.undo()
        assertEquals("seed $seed: full undo", history.first(), session.project.value)
        while (session.canRedo.value) session.redo()
        assertEquals("seed $seed: full redo", history.last(), session.project.value)
        assertTrue(session.flush())
        advanceUntilIdle()
        assertEquals("seed $seed: saved", session.project.value, store.load("p"))
    }

    @Test
    fun thousandsOfRandomEditsKeepTheProjectWhole() {
        // Replay one: EditFuzzTest.replay with its seed (PS_FUZZ_SEED), any number of steps (PS_FUZZ_STEPS).
        val seeds = System.getenv("PS_FUZZ_SEED")?.toLongOrNull()?.let { listOf(it) } ?: listOf(1L, 2L, 3L, 20261103L)
        val steps = System.getenv("PS_FUZZ_STEPS")?.toIntOrNull() ?: 1500
        seeds.forEach { run(it, steps) }
    }
}
