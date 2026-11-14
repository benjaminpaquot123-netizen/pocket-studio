package com.pocketstudio.app.audit3

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.human.BenchEngine
import com.pocketstudio.app.projects.CurrentProject
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.model.Project
import com.pocketstudio.model.ProjectJson
import com.pocketstudio.model.ProjectStore
import com.pocketstudio.model.switchScene
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.io.File
import java.time.Duration
import kotlin.random.Random

/**
 * Audit stage 3, « fuzzing et propriétés » through the screen's model: thousands of random calls
 * the screens make (cells, pads, hit details, tempo, parts, the 303, play and stop, undo, redo),
 * seeded and replayable (the failure names seed and step; PS_FUZZ_SEED and PS_FUZZ_STEPS replay
 * one). Invariants: no crash; the project saves without NaN and reads back the same; an undo lands
 * on a state the project had, and redo comes back; the file on disk is the project at the end.
 */
@RunWith(RobolectricTestRunner::class)
class ViewModelFuzzTest {
    private val app: Application = ApplicationProvider.getApplicationContext()

    @After
    fun end() {
        NativeEngine.sink = null
        CurrentProject.set(app, RhythmViewModel.PROJECT_ID)
    }

    private val wild = floatArrayOf(Float.NaN, Float.POSITIVE_INFINITY, -1e9f, 1e9f, -0.5f, 0f, 1f, 2f)

    private fun Random.f(lo: Float, hi: Float) = if (nextInt(6) == 0) wild[nextInt(wild.size)] else lo + nextFloat() * (hi - lo)
    private fun Random.i(lo: Int, hi: Int) = if (nextInt(8) == 0) listOf(-1, Int.MAX_VALUE, Int.MIN_VALUE, 9999)[nextInt(4)] else lo + nextInt(hi - lo + 1)

    private fun Random.call(vm: RhythmViewModel): String {
        val steps = vm.project.value.drums.steps
        val song = vm.project.value.song.size
        return when (nextInt(34)) {
            0, 1, 2, 3, 4 -> i(0, steps - 1).also { vm.toggleStep(it) }.let { "toggleStep $it" }
            5 -> i(0, steps - 1).also { vm.cycleVelocity(it) }.let { "cycleVelocity $it" }
            6, 7 -> i(0, 15).also { vm.selectPad(it) }.let { "selectPad $it" }
            8 -> f(0f, 1f).also { vm.setPadVolume(nextInt(16), it) }.let { "setPadVolume $it" }
            9 -> f(-1f, 1f).also { vm.setPadPanOf(nextInt(16), it) }.let { "setPadPanOf $it" }
            10 -> nextInt(16).also { vm.togglePadMute(it) }.let { "togglePadMute $it" }
            11 -> f(-1f, 1f).also { vm.setHitPan(i(0, steps - 1), it) }.let { "setHitPan $it" }
            12 -> f(0f, 1f).also { vm.setHitVelocity(i(0, steps - 1), it) }.let { "setHitVelocity $it" }
            13 -> f(-60f, 60f).also { vm.setHitOffset(i(0, steps - 1), it) }.let { "setHitOffset $it" }
            14 -> i(-24, 24).also { vm.setHitPitch(i(0, steps - 1), it) }.let { "setHitPitch $it" }
            15 -> (if (nextInt(5) == 0) wild[nextInt(wild.size)].toDouble() else 40.0 + nextDouble() * 260).also { vm.setTempo(it) }.let { "setTempo $it" }
            16 -> f(0f, 0.7f).toDouble().also { vm.setSwing(it) }.let { "setSwing $it" }
            17 -> i(0, steps - 1).also { vm.cycleRoll(it) }.let { "cycleRoll $it" }
            18 -> { val s = i(0, steps - 1); vm.paintStep(s, nextBoolean()); vm.paintStep(i(0, steps - 1), nextBoolean()); vm.paintEnd(); "paint $s" }
            19 -> { val s = i(0, steps - 1); vm.setRoll(s, i(1, 8), nextInt(-1, 2)); "setRoll $s" }
            20 -> { val s = i(0, steps - 1); vm.setSpread(s, i(1, 8), i(1, 4), nextInt(-1, 2)); "setSpread $s" }
            21 -> i(1, 8).also { vm.setDrumBars(it) }.let { "setDrumBars $it" }
            22 -> i(0, 7).also { vm.selectScene(it) }.let { "selectScene $it" }
            23 -> i(0, 7).also { vm.addSongPart(it) }.let { "addSongPart $it" }
            24 -> i(0, song).also { vm.duplicateSongPart(it) }.let { "duplicateSongPart $it" }
            25 -> i(0, song).also { vm.removeSongPart(it) }.let { "removeSongPart $it" }
            26 -> i(0, song).also { vm.setSongRepeats(it, i(1, 8)) }.let { "setSongRepeats $it" }
            27 -> i(0, song).also { vm.splitSongPart(it, i(1, 8)) }.let { "splitSongPart $it" }
            28 -> i(0, 7).also { vm.renameScene(it, listOf("Drop", "", "x".repeat(300))[nextInt(3)]) }.let { "renameScene $it" }
            29 -> i(1, 8).also { vm.setAcidBars(it) }.let { "setAcidBars $it" }
            30 -> { vm.togglePlay(); "togglePlay" }
            31 -> { vm.undo(); "undo" }
            32 -> { vm.redo(); "redo" }
            else -> i(0, 15).also { vm.hitPad(it) }.let { "hitPad $it" }
        }
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(5))

    private fun run(seed: Long, steps: Int) {
        val rnd = Random(seed)
        val id = "fuzz-$seed-" + System.nanoTime()
        val store = ProjectStore(File(app.filesDir, "projects"))
        store.save(id, Project(name = "Fuzz"))
        CurrentProject.set(app, id)
        val vm = RhythmViewModel(app)
        val end = System.currentTimeMillis() + 20_000
        while (vm.project.value.name != "Fuzz") { check(System.currentTimeMillis() < end); idle() }
        // « ⟳ Partie » and the Live page's settings are kept through undo by design (keepListenChoice).
        // The scene shown follows the song quietly (no undo step): compared as if scene 1 were shown.
        fun norm(p: Project) = Project().let { d -> p.switchScene(0).copy(loopPart = false, liveQuantum = d.liveQuantum, liveQuantumFine = d.liveQuantumFine, liveFx = d.liveFx) }
        val seen = HashSet<Project>().apply { add(norm(vm.project.value)) }
        var changes = 0
        for (i in 0 until steps) {
            val before = vm.project.value
            val what = try { rnd.call(vm) } catch (e: Throwable) { throw AssertionError("seed $seed, step $i crashed", e) }
            if (rnd.nextInt(10) == 0) idle()
            val where = "seed $seed, step $i, « $what »"
            val after = vm.project.value
            val text = ProjectJson.encode(after)
            assertFalse("$where: NaN written", text.contains("NaN") || text.contains("Infinity"))
            assertEquals("$where: reads back the same", after, ProjectJson.decode(text))
            val quiet = listOf("undo", "redo", "togglePlay", "selectScene", "selectPad", "hitPad").any { what.startsWith(it) }
            if (after != before && !quiet) {
                changes++
                vm.undo()
                val undone = vm.project.value
                if (norm(undone) !in seen) {
                    fun diff(a: Any, b: Any) = a.javaClass.declaredFields.filter { f -> f.isAccessible = true; f.get(a) != f.get(b) }
                        .joinToString { f -> "${f.name}: ${f.get(a)} vs ${f.get(b)}".take(400) }
                    throw AssertionError("$where: undo lands on a state the project never had; against before: ${diff(undone, before)}")
                }
                vm.redo()
                assertEquals("$where: redo comes back", after, vm.project.value)
            }
            seen += norm(vm.project.value)
        }
        assertTrue("seed $seed: too few changes ($changes)", changes > steps / 5)
        if (vm.transport.value.playing) vm.togglePlay()
        // Not runBlocking on this (main) thread: an autosave under way holds the save lock and resumes on the main
        // thread, which runBlocking would hold: the flush then waited for ever (round 2 integration, the whole suite hung).
        var flushed: Boolean? = null
        @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.Main) { flushed = vm.flushNow() }
        val until = System.currentTimeMillis() + 30_000
        while (flushed == null && System.currentTimeMillis() < until) shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(20))
        assertEquals("seed $seed: the last save", true, flushed)
        assertEquals("seed $seed: the file is the project", vm.project.value, store.load(id))
    }

    @Test
    fun aPadOutOfRangeIsIgnored() {
        // Found by the fuzzer (seed 7, step 172): hitPad(-1) became the selected pad, the next cell crashed.
        NativeEngine.sink = BenchEngine()
        val vm = RhythmViewModel(app)
        for (lane in listOf(-1, 16, Int.MAX_VALUE)) {
            vm.hitPad(lane)
            assertTrue(vm.transport.value.selectedPad in 0 until 16)
            vm.paintStep(3, true)
            vm.paintEnd()
            vm.toggleStep(5)
        }
    }

    @Test
    fun thousandsOfRandomCallsKeepTheProjectWhole() {
        NativeEngine.sink = BenchEngine()
        val seeds = System.getenv("PS_FUZZ_SEED")?.toLongOrNull()?.let { listOf(it) } ?: listOf(7L, 20261103L)
        val steps = System.getenv("PS_FUZZ_STEPS")?.toIntOrNull() ?: 1500
        seeds.forEach { run(it, steps) }
    }
}
