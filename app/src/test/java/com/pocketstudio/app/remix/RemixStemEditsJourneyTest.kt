package com.pocketstudio.app.remix

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.engine.CommandSink
import com.pocketstudio.engine.ExportResult
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.model.Project
import com.pocketstudio.model.ProjectStore
import com.pocketstudio.model.Remix
import com.pocketstudio.model.StemClip
import com.pocketstudio.model.allScenes
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.io.File
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

/**
 * Step 5b: the song put in a project, its voice cut, a piece moved a bar later and faded in; each
 * an undo step; saved and reopened as it was; and the export engine gets the same edits as the
 * phone's engine (the stems' files never touched). The engine's rendering of the edits is checked
 * on Linux (remix5b_stem_pieces_cut_moved_and_faded).
 */
@RunWith(RobolectricTestRunner::class)
class RemixStemEditsJourneyTest {
    private val app: Application = ApplicationProvider.getApplicationContext()
    private val store = ProjectStore(File(app.filesDir, "projects"))

    /** The engine's stand-in: each engine's (target's) last committed edits per stem. */
    private class Sink : CommandSink {
        private var next = 1L
        private val staged = ConcurrentHashMap<Triple<Long, Int, Int>, Long>()
        val committed: MutableMap<Pair<Long, Int>, List<Long>?> = java.util.Collections.synchronizedMap(HashMap())
        @Volatile var rendered = false
        override fun post(target: Long, type: Int, value: Float, a: Int, b: Int) {
            when (type) {
                144 -> staged[Triple(target, a shr 16, a and 0xffff)] = b.toLong()
                145 -> committed[target to a] = if (b < 0) null else (0 until b * 5).map { staged[Triple(target, a, ((it / 5) shl 8) or (it % 5))] ?: -1L }
            }
        }
        override fun scheduleFrom(target: Long, frame: Long) = Unit
        override fun install(target: Long, kind: CommandSink.Install, index: Int, sample: Long) = Unit
        @Synchronized override fun sampleCreate(data: FloatArray, channels: Int, sampleRate: Int): Long = next++
        override fun sampleFrames(handle: Long): Long = 4800L
        override fun sampleRelease(handle: Long) = Unit
        @Synchronized override fun exportCreate(): Long = 1000L + next++
        override fun exportRender(target: Long, path: String, frames: Long, tailFrames: Long, fadeOutFrames: Long, autoTail: Boolean, floatWav: Boolean, sampleRate: Int, bits: Int): ExportResult {
            rendered = true
            return ExportResult.Cancelled
        }
        override fun exportRenderFloat(target: Long, frames: Long, tailFrames: Long): FloatArray = FloatArray(0)
        override fun exportDestroy(target: Long) = Unit
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
            assertTrue(what, System.currentTimeMillis() < until)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(50))
            Thread.sleep(5)
        }
    }

    private fun open(id: String): RhythmViewModel {
        com.pocketstudio.app.projects.CurrentProject.set(app, id)
        NativeEngine.sink = sink
        val vm = RhythmViewModel(app)
        waitUntil("the project never opened") { vm.project.value.name == id }
        return vm
    }

    @Test
    fun cutMovedFadedSavedReopenedAndExportedTheSame() {
        val id = "remix-edits-${System.nanoTime()}"
        // « Boys » put in an empty project: 64 s at 120 BPM, one part of 32 bars on brique A.
        store.save(id, Project(name = id, tempoBpm = 120.0, remix = Remix("boys", "Boys", songBpm = 120.0, seconds = 64.0)))
        val vm = open(id)
        assertEquals(1, vm.project.value.song.size)
        val voice = 3
        // Bar 5 (step 64, the song's 8th second): cut there, the second piece a bar later, faded in.
        vm.cutStemAt(voice, 64)
        vm.moveStemPieceAt(voice, 64, 1)
        vm.toggleStemFadeAt(voice, 80, atStart = true)
        val edited = listOf(StemClip(0.0, 0.0, 8.0), StemClip(10.0, 8.0, 56.0, fadeIn = 2.0))
        assertEquals(edited, vm.project.value.remix.stems[voice].clips)
        // Each one an undo step.
        vm.undo()
        assertEquals(0.0, vm.project.value.remix.stems[voice].clips!![1].fadeIn, 0.0)
        vm.redo()
        assertEquals(edited, vm.project.value.remix.stems[voice].clips)
        // The phone's engine plays it: in frames of the song as separated (48 kHz).
        waitUntil("the engine never got the edits") { sink.committed[0L to voice] != null }
        assertEquals(listOf(0L, 0L, 384_000L, 0L, 0L, 480_000L, 384_000L, 2_688_000L, 96_000L, 0L), sink.committed[0L to voice])
        assertNull(sink.committed[0L to 0])
        // Saved, reopened: the same edits.
        kotlinx.coroutines.runBlocking { vm.flushNow() }
        assertEquals(edited, store.load(id)!!.remix.stems[voice].clips)
        val again = open(id)
        assertEquals(edited, again.project.value.remix.stems[voice].clips)
        // Exported: the export engine gets the same pieces.
        again.exportWav()
        waitUntil("nothing was rendered") { sink.rendered }
        val exportTarget = synchronized(sink.committed) { sink.committed.keys.toList() }.first { it.first != 0L && it.second == voice }.first
        assertEquals(sink.committed[0L to voice], sink.committed[exportTarget to voice])
    }

    /**
     * Step 5c: « Mettre Boys dans mon projet » with its choices: the project takes the song's tempo
     * and key, only the voice heard, its parts made bricks; one « Annuler » takes it all back.
     */
    @Test
    fun theImportScreensChoicesGoInAsOneUndoStep() {
        val id = "remix-import-${System.nanoTime()}"
        store.save(id, Project(name = id, tempoBpm = 100.0))
        val vm = open(id)
        val before = vm.project.value
        val set = StemSet(File(app.filesDir, "boys"), "Boys", 80.0, 0L, SongInfo(bpm = 120.0, firstBeat = 0.0, root = 9, minor = true))
        val parts = listOf(
            com.pocketstudio.model.SongSection("Intro", 0, 4), com.pocketstudio.model.SongSection("Refrain", 4, 8),
            com.pocketstudio.model.SongSection("Couplet", 12, 8), com.pocketstudio.model.SongSection("Refrain", 20, 8),
        )
        vm.useSong(set, null, com.pocketstudio.model.SongImport(takeTempo = true, setKey = true, place = com.pocketstudio.model.SongPlace.VoiceOnly, bricks = true), parts)
        val p = vm.project.value
        assertEquals(120.0, p.tempoBpm, 0.0)
        assertEquals(9, p.harmony.root)
        assertEquals(listOf(true, true, true, false), p.remix.stems.map { it.mute })
        assertEquals(listOf("Intro", "Refrain", "Couplet"), p.allScenes.take(3).map { it.label })
        assertEquals(listOf(0, 1, 2, 1), p.song.map { it.scene })
        // A passage: the stems heard from bar 9 for 8 bars only.
        vm.undo()
        assertEquals(before.copy(name = id).tempoBpm, vm.project.value.tempoBpm, 0.0)
        assertNull(vm.project.value.remix.setId)
        assertTrue(vm.project.value.song.isEmpty())
        vm.useSong(set, null, com.pocketstudio.model.SongImport(takeTempo = false, setKey = false, place = com.pocketstudio.model.SongPlace.Passage, fromBar = 8, bars = 8))
        val q = vm.project.value
        assertEquals(100.0, q.tempoBpm, 0.0)
        assertEquals(8, q.remix.startBar)
        assertEquals(listOf(com.pocketstudio.model.StemClip(16.0, 16.0, 16.0)), q.remix.stems[0].clips)
        // Following the project's 100 BPM, the song's 8 bars are 8 of the project's.
        assertEquals(8, q.song.single().bars)
    }
}
