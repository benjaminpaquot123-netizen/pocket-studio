package com.pocketstudio.app.live

import android.app.Application
import android.os.Looper
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.remix.SongInfo
import com.pocketstudio.app.remix.StemSet
import com.pocketstudio.app.remix.StemStore
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.engine.CommandSink
import com.pocketstudio.engine.ExportResult
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.model.DrumPattern
import com.pocketstudio.model.LiveSlots
import com.pocketstudio.model.Project
import com.pocketstudio.model.ProjectStore
import com.pocketstudio.model.RemixCell
import com.pocketstudio.model.Scene
import com.pocketstudio.model.SongPart
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.io.File
import java.time.Duration
import kotlin.math.sin

/**
 * Lot D2 (« case Remix du Live »), end to end through the ViewModel, the engine a stand-in that
 * holds a separated song (20 s at 120 BPM, the voice a 220 Hz tone): a passage chosen in a cell's
 * menu (the voice, from bar 2, two bars) → its sound cut on whole bars and given to the engine on
 * that cell → launched, it is staged with its slot and waits for the bar → a Live take writes it
 * into the song → undo takes each step back → saved and reopened, it is still there → the song's
 * export installs it in the export engine and plays it in the part the take wrote.
 */
@RunWith(RobolectricTestRunner::class)
class LiveRemixCellTest {
    private val app: Application = ApplicationProvider.getApplicationContext()
    private val store = ProjectStore(File(app.filesDir, "projects"))

    private class Sink : CommandSink {
        private var next = 100L
        val frames = 48_000L * 20
        /** Everything posted: (target, type, a, b, value). */
        val posts = ArrayList<List<Any>>()
        /** Remix cells installed: (target, index, sample). */
        val cells = ArrayList<Triple<Long, Int, Long>>()
        val created = HashMap<Long, Int>()
        val extracted = ArrayList<Triple<Int, Long, Long>>()
        @Volatile var rendered = false

        @Synchronized override fun post(target: Long, type: Int, value: Float, a: Int, b: Int) {
            posts += listOf(target, type, a, b, value)
        }
        override fun scheduleFrom(target: Long, frame: Long) = Unit
        @Synchronized override fun install(target: Long, kind: CommandSink.Install, index: Int, sample: Long) {
            if (kind == CommandSink.Install.RemixCell) cells += Triple(target, index, sample)
        }
        @Synchronized override fun sampleCreate(data: FloatArray, channels: Int, sampleRate: Int): Long = (next++).also { created[it] = data.size / channels }
        @Synchronized override fun sampleFrames(handle: Long): Long = (created[handle] ?: 4800).toLong()
        override fun sampleRelease(handle: Long) = Unit
        @Synchronized override fun exportCreate(): Long = next++
        override fun exportRender(target: Long, path: String, frames: Long, tailFrames: Long, fadeOutFrames: Long, autoTail: Boolean, floatWav: Boolean, sampleRate: Int, bits: Int): ExportResult {
            rendered = true
            return ExportResult.Cancelled
        }
        override fun exportRenderFloat(target: Long, frames: Long, tailFrames: Long): FloatArray = FloatArray(0)
        override fun exportDestroy(target: Long) = Unit
        override fun stemsLoad(paths: Array<String>): Long = next++
        override fun stemsFrames(handle: Long): Long = frames
        @Synchronized override fun stemsExtract(handle: Long, stem: Int, start: Long, count: Long): FloatArray {
            extracted += Triple(stem, start, count)
            val n = maxOf(0L, minOf(count, frames - start)).toInt()
            return FloatArray(n * 2) { i -> if (stem == 3) (0.5 * sin(2 * Math.PI * 220 * (start + i / 2) / 48_000.0)).toFloat() else 0.1f }
        }
        override fun stemsPeaks(handle: Long, stem: Int, buckets: Int): FloatArray = FloatArray(buckets) { 0.5f }
        override fun stemsStretched(handle: Long, ratio: Double): Long = 9_000L
        override fun stemsRelease(handle: Long): Boolean = true
        override fun stemPosition(): Long = -1L
        /** The transport's step and where the engine marked the take's start. */
        @Volatile var step = -1
        @Volatile var mark = -1
        override fun transportStep(): Int = step
        override fun transportPosition(): Double = step.toDouble()
        override fun songMarkStep(): Int = mark

        @Synchronized fun postsOf(target: (Long) -> Boolean): List<List<Any>> = posts.filter { target(it[0] as Long) }
        @Synchronized fun clear() = posts.clear()
    }

    private val sink = Sink()

    @After
    fun tearDown() {
        NativeEngine.sink = null
        com.pocketstudio.app.projects.CurrentProject.set(app, RhythmViewModel.PROJECT_ID)
    }

    private fun waitUntil(what: String, check: () -> Boolean) {
        val until = System.currentTimeMillis() + 60_000
        while (!check()) {
            assertTrue("never: $what", System.currentTimeMillis() < until)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(50))
            Thread.sleep(5)
        }
    }

    private fun stemSet(): StemSet {
        val dir = StemStore.dirFor(app, "song-d2")
        dir.mkdirs()
        listOf("drums", "bass", "other", "vocals").forEach { File(dir, "$it.wav").writeBytes(ByteArray(44)) }
        return StemStore.commit(dir, "Boys", 20.0, SongInfo(bpm = 120.0, firstBeat = 0.0, root = 9, minor = true))
    }

    private fun open(id: String): RhythmViewModel {
        com.pocketstudio.app.projects.CurrentProject.set(app, id)
        NativeEngine.sink = sink
        val vm = RhythmViewModel(app)
        waitUntil("the project opened") { vm.project.value.name == id }
        return vm
    }

    private fun save(vm: RhythmViewModel) {
        var saved: Boolean? = null
        vm.viewModelScope.launch { saved = vm.flushNow() }
        waitUntil("saved") { saved != null }
        assertTrue(saved!!)
    }

    /** The posts of the remix cell command (171) to [target]: the cells it named. */
    private fun cellPosts(target: (Long) -> Boolean) = sink.postsOf(target).filter { it[1] == 171 }.map { it[2] as Int }

    @Test
    fun aPassageChosenPlaysOnTheBarIsTakenUndoneSavedAndExported() {
        val id = "remix-cell-${System.nanoTime()}"
        val drums = DrumPattern(steps = 16).toggle(0, 0).toggle(2, 4)
        store.save(id, Project(name = id, tempoBpm = 120.0, drums = drums, scenes = listOf(Scene("A", drums), Scene("B", drums)), scene = 0).sanitized())
        val vm = open(id)
        vm.useSong(stemSet())
        waitUntil("the stems loaded") { vm.stemOverview.value != null }
        assertEquals(120.0, vm.project.value.tempoBpm, 0.0)
        val depth = vm.session.undoDepth

        // 1. The cell's menu: the voice, from bar 2, two bars, in brick B.
        vm.liveRemix.choose(1, 3, 1, 2)
        waitUntil("the passage cut") { vm.project.value.remix.cell(1) != null && vm.liveRemix.busy.value == null }
        val cell = vm.project.value.remix.cell(1)!!
        assertEquals(RemixCell(1, cell.file, "Voix", bars = 2, bpm = 120.0, stem = 3, from = 2.0), cell)
        assertTrue(sink.extracted.contains(Triple(3, 96_000L, 192_000L)))  // bar 2 of the voice, two bars
        assertEquals(4.0, vm.padSamples.info.value[cell.file]!!.seconds, 1e-6)
        assertEquals(depth + 1, vm.session.undoDepth)
        assertEquals("Voix dans la case Remix de la brique B.", vm.notice.value)
        // The engine has it on cell 1, looping over 32 steps (two bars).
        val live = sink.cells.last { it.first == 0L && it.third != 0L }
        assertEquals(1, live.second and 0xFF)
        assertEquals(32, live.second ushr 12)
        assertEquals(192_000, sink.created[live.third])

        // 2. Launched while the music plays: staged with its slot, armed on the bar line.
        vm.togglePlay()
        vm.takeClockForTest = 5
        sink.step = 5
        vm.togglePerformance()
        vm.takeClockForTest = 16
        sink.step = 16
        sink.mark = 16
        vm.tickPerformance()
        assertTrue("recording: ${vm.take.value}", vm.take.value is TakeUi.Recording)
        sink.clear()
        vm.launchPad(SongPart.REMIX, 1)
        val sent = sink.postsOf { it == 0L }
        val stagingOn = sent.indexOfFirst { it[1] == 39 && (it[4] as Float) > 0.5f }
        val cellAt = sent.indexOfFirst { it[1] == 171 }
        val stagingOff = sent.indexOfLast { it[1] == 39 && (it[4] as Float) < 0.5f }
        val armAt = sent.indexOfFirst { it[1] == 71 }
        assertTrue("staged: $sent", stagingOn in 0 until cellAt && cellAt < stagingOff && stagingOff < armAt)
        assertEquals(1, sent[cellAt][2])
        assertEquals(1 shl LiveSlots.REMIX, sent[armAt][2])
        assertEquals(16, (sent[armAt][3] as Int) and 0x1FFFF)  // on the next bar
        // Brick A (no passage) gives the remix the whole song back.
        sink.clear()
        vm.launchPad(SongPart.REMIX, 0)
        assertEquals(listOf(-1), cellPosts { it == 0L })
        vm.launchPad(SongPart.REMIX, 1)

        // 3. The take: the remix on brick B's passage from bar 3 to the end (bar 5).
        vm.liveLaunchedForTest(LiveSlots.REMIX, 1, 32)
        vm.takeClockForTest = 60
        sink.step = 60
        vm.togglePerformance()
        assertTrue("written: ${vm.take.value}", vm.take.value is TakeUi.Done)
        val song = vm.project.value.song
        assertEquals(2, song.size)
        assertEquals(0, song[0].sceneFor(SongPart.REMIX))
        assertEquals(1, song[1].sceneFor(SongPart.REMIX))
        assertEquals(depth + 2, vm.session.undoDepth)

        // 4. Saved, then opened again: the passage and the take's part are there.
        save(vm)
        val again = open(id)
        assertEquals(cell, again.project.value.remix.cell(1))
        assertEquals(1, again.project.value.song[1].sceneFor(SongPart.REMIX))
        waitUntil("its sound loaded") { again.padSamples.native(cell.file) != null }

        // 5. The song's export: the passage installed in the export engine, played in its part.
        sink.clear()
        again.exportWav()
        waitUntil("the export rendered") { sink.rendered }
        val exported = sink.cells.filter { it.first != 0L && it.third != 0L }
        assertTrue("installed in the export: ${sink.cells}", exported.any { (it.second and 0xFF) == 1 && it.second ushr 12 == 32 })
        assertTrue("staged in the export: ${cellPosts { it != 0L }}", 1 in cellPosts { it != 0L })

        // 6. Undo: the take, then the passage (the engine's cell emptied, brick B plays the song).
        vm.undoPerformance()
        assertTrue(vm.project.value.song.none { it.sceneFor(SongPart.REMIX) == 1 })
        assertNotNull(vm.project.value.remix.cell(1))
        vm.undo()
        waitUntil("the passage undone") { vm.project.value.remix.cell(1) == null }
        assertNull(vm.project.value.remix.cell(1))
        waitUntil("the engine's cell emptied") { sink.cells.any { it.first == 0L && (it.second and 0xFF) == 1 && it.third == 0L } }
        vm.redo()
        assertEquals(cell, vm.project.value.remix.cell(1))
    }

    @Test
    fun theMenuSetsAPassageAndGivesTheSongBack() {
        val id = "remix-menu-${System.nanoTime()}"
        store.save(id, Project(name = id, tempoBpm = 120.0, scenes = listOf(Scene("A"), Scene("B")), scene = 0).sanitized())
        val vm = open(id)
        vm.useSong(stemSet())
        waitUntil("the stems loaded") { vm.stemOverview.value != null }
        // The whole song, from the start, one bar: the four stems together.
        vm.liveRemix.choose(0, RemixCell.WHOLE, 0, 1)
        waitUntil("the passage cut") { vm.project.value.remix.cell(0) != null && vm.liveRemix.busy.value == null }
        val whole = vm.project.value.remix.cell(0)!!
        assertEquals("Boys", whole.name)
        assertEquals((0..3).toSet(), sink.extracted.filter { it.second == 0L && it.third == 96_000L }.map { it.first }.toSet())
        // « Toute la chanson ici »: one undo step, the engine told at once.
        sink.clear()
        vm.liveRemix.clear(0)
        assertNull(vm.project.value.remix.cell(0))
        assertEquals(listOf(-1), cellPosts { it == 0L }.takeLast(1))
        vm.undo()
        assertEquals(whole, vm.project.value.remix.cell(0))
        // Past the song's end: said, nothing changed.
        vm.liveRemix.choose(1, 3, 40, 1)
        waitUntil("said") { vm.liveRemix.busy.value == null && vm.notice.value == "Ce passage n'a pas pu être lu." }
        assertNull(vm.project.value.remix.cell(1))
    }
}
