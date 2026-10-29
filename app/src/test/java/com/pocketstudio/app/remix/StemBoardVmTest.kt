package com.pocketstudio.app.remix

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.engine.CommandSink
import com.pocketstudio.engine.ExportResult
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.model.DrumPattern
import com.pocketstudio.model.Project
import com.pocketstudio.model.ProjectStore
import com.pocketstudio.model.Scene
import com.pocketstudio.model.SongPart
import kotlinx.coroutines.runBlocking
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
 * The stem board's actions (B3) through the drum machine's ViewModel, with an engine stand-in that
 * holds a separated song (a voice singing three phrases between silences, 120 BPM, A minor): each
 * send or chop is one undo step, never covers the kick nor a pad the beat plays, never changes the
 * project's tempo or key, and lands where it says (pads, a new instrument or audio track, a live pad).
 */
@RunWith(RobolectricTestRunner::class)
class StemBoardVmTest {
    private val app: Application = ApplicationProvider.getApplicationContext()
    private val store = ProjectStore(File(app.filesDir, "projects"))

    /** The song: 20 s at 48 kHz; the voice (stem 3) sings from 2 to 4.5 s, 8 to 11 s and 14 to 16 s. */
    private class Sink : CommandSink {
        private var next = 100L
        val frames = 48_000L * 20
        val phrases = listOf(2.0 to 4.5, 8.0 to 11.0, 14.0 to 16.0)
        /** (handle, start, count) of each read. */
        val extracted = ArrayList<Triple<Long, Long, Long>>()
        val loaded = ArrayList<Long>()
        val released = ArrayList<Long>()

        fun voice(frame: Long): Float {
            val t = frame / 48_000.0
            return if (phrases.any { (a, b) -> t >= a && t < b }) (0.5 * sin(2 * Math.PI * 220 * t)).toFloat() else 0f
        }

        override fun post(target: Long, type: Int, value: Float, a: Int, b: Int) = Unit
        override fun scheduleFrom(target: Long, frame: Long) = Unit
        override fun install(target: Long, kind: CommandSink.Install, index: Int, sample: Long) = Unit
        @Synchronized override fun sampleCreate(data: FloatArray, channels: Int, sampleRate: Int): Long = next++
        override fun sampleFrames(handle: Long): Long = 4800L
        override fun sampleRelease(handle: Long) = Unit
        @Synchronized override fun exportCreate(): Long = next++
        override fun exportRender(target: Long, path: String, frames: Long, tailFrames: Long, fadeOutFrames: Long, autoTail: Boolean, floatWav: Boolean, sampleRate: Int, bits: Int): ExportResult =
            ExportResult.Cancelled
        override fun exportRenderFloat(target: Long, frames: Long, tailFrames: Long): FloatArray = FloatArray(0)
        override fun exportDestroy(target: Long) = Unit

        @Synchronized override fun stemsLoad(paths: Array<String>): Long = (next++).also { loaded += it }
        override fun stemsFrames(handle: Long): Long = frames
        @Synchronized override fun stemsExtract(handle: Long, stem: Int, start: Long, count: Long): FloatArray {
            extracted += Triple(handle, start, count)
            val n = maxOf(0L, minOf(count, frames - start)).toInt()
            return FloatArray(n * 2) { i -> if (stem == 3) voice(start + i / 2) else 0.1f }
        }
        override fun stemsPeaks(handle: Long, stem: Int, buckets: Int): FloatArray = FloatArray(buckets) { 0.5f }
        @Synchronized override fun stemsStretched(handle: Long, ratio: Double): Long = 9_000L
        @Synchronized override fun stemsRelease(handle: Long): Boolean {
            released += handle
            return true
        }
        override fun stemPosition(): Long = -1L
    }

    private val sink = Sink()

    @After
    fun tearDown() {
        NativeEngine.sink = null
        com.pocketstudio.app.projects.CurrentProject.set(app, RhythmViewModel.PROJECT_ID)
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(50))

    private fun waitUntil(what: String, check: () -> Boolean) {
        val until = System.currentTimeMillis() + 60_000
        while (!check()) {
            assertTrue(what, System.currentTimeMillis() < until)
            idle()
            Thread.sleep(5)
        }
    }

    private fun stemSet(id: String = "song-b3"): StemSet {
        val dir = StemStore.dirFor(app, id)
        dir.mkdirs()
        listOf("drums", "bass", "other", "vocals").forEach { File(dir, "$it.wav").writeBytes(ByteArray(44)) }
        return StemStore.commit(dir, "Ma chanson", 20.0, SongInfo(bpm = 120.0, firstBeat = 0.0, root = 9, minor = true))
    }

    /** The kick on pad 1 and the snare on pad 3 in both scenes, a song of the two. */
    private fun beat(): Project {
        val drums = DrumPattern(steps = 16).toggle(0, 0).toggle(0, 8).toggle(2, 4).toggle(2, 12)
        return Project(
            tempoBpm = 120.0, drums = drums,
            scenes = listOf(Scene("A", drums, label = "Intro"), Scene("B", drums, label = "Drop")), scene = 0,
            song = listOf(SongPart(0, repeats = 8), SongPart(1, repeats = 8)),
        ).sanitized()
    }

    private fun open(p: Project = beat()): RhythmViewModel {
        val id = "stem-board-${System.nanoTime()}"
        store.save(id, p.copy(name = id))
        com.pocketstudio.app.projects.CurrentProject.set(app, id)
        NativeEngine.sink = sink
        val vm = RhythmViewModel(app)
        waitUntil("the project never opened") { vm.project.value.name == id }
        return vm
    }

    private fun withSong(vm: RhythmViewModel): StemSet {
        val set = stemSet()
        vm.useSong(set)
        waitUntil("the stems never loaded") { vm.stemOverview.value != null }
        return set
    }

    /** Waits for the board's work to end; what it said. */
    private fun said(vm: RhythmViewModel): String {
        waitUntil("the board never finished") { vm.stemBoard.state.value.let { it.busy == null && it.message != null } }
        return vm.stemBoard.state.value.message!!
    }

    @Test
    fun theVoiceCutIntoItsPhrasesKeepsTheBeatInOneUndoStep() {
        val vm = open()
        val set = withSong(vm)
        val before = vm.project.value
        val depth = vm.session.undoDepth
        vm.stemBoard.chop(set.id, 3)
        assertEquals("3 phrases sur les pads 2, 4 et 5.", said(vm))
        val p = vm.project.value
        // The kick and the snare as they were.
        assertNull(p.drums.lanes[0].sample)
        assertNull(p.drums.lanes[2].sample)
        assertEquals(before.drums.lanes[0].hits, p.drums.lanes[0].hits)
        assertEquals(listOf("Voix 1", "Voix 2", "Voix 3"), listOf(1, 3, 4).map { p.drums.lanes[it].sample?.name })
        // Each phrase as sung: 2.5 s, 3 s, 2 s.
        val seconds = listOf(1, 3, 4).map { vm.padSamples.info.value[p.drums.lanes[it].sample!!.file]!!.seconds }
        listOf(2.5, 3.0, 2.0).zip(seconds).forEach { (want, got) -> assertEquals(want, got, 0.05) }
        // The project's tempo and key never move; one undo step takes all three away.
        assertEquals(before.tempoBpm, p.tempoBpm, 0.0)
        assertEquals(before.harmony, p.harmony)
        assertEquals(depth + 1, vm.session.undoDepth)
        vm.undo()
        assertTrue(vm.project.value.drums.lanes.all { it.sample == null })
    }

    /** Owner (2026-10-29): « Garder dans ma bibliothèque »: the stretch at the song's own tempo, with its key; the project untouched. */
    @Test
    fun aStretchIsKeptInMyLibraryWithTheSongsTempoAndKey() {
        File(app.filesDir, "mysounds").deleteRecursively()
        val vm = open()
        val set = withSong(vm)
        val before = vm.project.value
        val depth = vm.session.undoDepth
        vm.stemBoard.send(StemSelection(set.id, 3, 96_000, 192_000), StemDestination.Mine)
        assertEquals(
            "« Voix · Ma chanson » est dans ta bibliothèque (Voix) : tu le retrouves dans « Changer le son », pour tous tes projets.",
            said(vm),
        )
        val kept = com.pocketstudio.app.library.MySoundLibrary.sounds.value.single()
        assertEquals("vocal", kept.kind)
        assertEquals(2.0, kept.seconds, 0.01)
        assertEquals(120.0, kept.bpm, 0.0)
        assertEquals(9, kept.root)
        assertTrue(kept.minor)
        // Read at the song's own speed, never the project's fitted copy.
        assertTrue(sink.extracted.any { it.second == 96_000L && it.third == 96_000L })
        assertEquals(before, vm.project.value)
        assertEquals(depth, vm.session.undoDepth)
        File(app.filesDir, "mysounds").deleteRecursively()
        runBlocking { com.pocketstudio.app.library.MySoundLibrary.refresh(app) }
    }

    @Test
    fun aStretchOnAPadNeverTheKick() {
        val vm = open()
        val set = withSong(vm)
        vm.selectPad(0)  // the kick, selected: the stretch still never goes there
        val depth = vm.session.undoDepth
        vm.stemBoard.send(StemSelection(set.id, 3, 96_000, 144_000), StemDestination.Pad())
        assertEquals("« Voix 1 » est sur le pad 2.", said(vm))
        val p = vm.project.value
        assertNull(p.drums.lanes[0].sample)
        assertEquals("Voix 1", p.drums.lanes[1].sample?.name)
        assertEquals(1.0, vm.padSamples.info.value[p.drums.lanes[1].sample!!.file]!!.seconds, 0.01)
        assertEquals(depth + 1, vm.session.undoDepth)
        // Read from the song the project plays (its own copy, no second load).
        assertEquals(1, sink.loaded.size)
        assertTrue(sink.extracted.any { it.second == 96_000L && it.third == 48_000L })
    }

    @Test
    fun aStretchBecomesAnInstrumentTrackPlayedOnTheKeys() {
        val vm = open()
        val set = withSong(vm)
        val before = vm.project.value
        val depth = vm.session.undoDepth
        vm.stemBoard.send(StemSelection(set.id, 3, 96_000, 192_000), StemDestination.Instrument)
        assertEquals("« Voix 1 » est une nouvelle piste d'instrument : joue-la au clavier.", said(vm))
        val p = vm.project.value
        val t = p.tracks.single { it.name == "Voix 1" }
        assertEquals(2, t.engine)
        assertNotNull(t.sampled.file)
        // The voice sings a 220 Hz A: the key of A3 plays it as it is.
        assertEquals(57f, t.sampled.root, 0.5f)
        assertEquals(t.id, vm.selectedTrack.value)
        assertEquals(before.tempoBpm, p.tempoBpm, 0.0)
        assertEquals(before.harmony, p.harmony)
        assertEquals(depth + 1, vm.session.undoDepth)
        vm.undo()
        assertTrue(vm.project.value.tracks.none { it.name == "Voix 1" })
    }

    @Test
    fun aStretchBecomesAnAudioTrackOfTheSongWhereAsked() {
        val vm = open()
        val set = withSong(vm)
        val depth = vm.session.undoDepth
        vm.stemBoard.send(StemSelection(set.id, 3, 384_000, 528_000), StemDestination.Track(atStep = 8 * 16))
        assertEquals("« Voix 1 » est sur une nouvelle piste audio, à la mesure 9.", said(vm))
        val t = vm.project.value.audioTracks.single { it.name == "Voix 1" }
        val clip = t.clip!!
        // A song clip: once, from the drop's first step.
        assertTrue(clip.song)
        assertEquals(8 * 16, clip.start)
        assertEquals(3.0, clip.seconds, 0.01)
        assertEquals(120.0, clip.bpm, 0.0)
        assertEquals(depth + 1, vm.session.undoDepth)
        vm.undo()
        assertTrue(vm.project.value.audioTracks.none { it.name == "Voix 1" })
    }

    @Test
    fun aStretchOnAFreeLivePad() {
        val vm = open()
        val set = withSong(vm)
        val depth = vm.session.undoDepth
        vm.stemBoard.send(StemSelection(set.id, 1, 0, 96_000), StemDestination.Live)
        assertEquals("« Basse 1 » est sur le pad live 1.", said(vm))
        val clip = vm.project.value.live.clips[0]!!
        assertEquals("Basse 1", clip.name)
        assertEquals(4, clip.beats)  // 2 s at 120 BPM
        assertEquals(depth + 1, vm.session.undoDepth)
    }

    @Test
    fun anotherSongIsReadOnceThenFreedAndTheFittedCopyIsRead() {
        val vm = open()
        withSong(vm)
        val other = stemSet("song-b3-other")
        val frames = runBlocking { vm.stemBoardHost.withSong(other.id) { it.frames } }
        assertEquals(sink.frames, frames)
        assertEquals(2, sink.loaded.size)
        assertTrue(sink.loaded.last() in sink.released)
        // Gone from the phone: null, nothing loaded.
        assertNull(runBlocking { vm.stemBoardHost.withSong("nowhere") { it.frames } })
        assertEquals(2, sink.loaded.size)
        // The project at 100 BPM, the song following it: a fitted read comes from the copy, 1.2 times as long.
        vm.setTempo(100.0)
        vm.setRemixFollowTempo(true)
        waitUntil("the copy never came") { !vm.remixStretching.value }
        idle()
        val bpm = runBlocking {
            vm.stemBoardHost.withSong("song-b3") { src ->
                src.read(3, 48_000, 4_800, fitted = true)
                src.read(3, 48_000, 4_800, fitted = false)
                src.fittedBpm
            }
        }
        assertEquals(100.0, bpm!!, 0.0)
        assertEquals(Triple(9_000L, 57_600L, 5_760L), sink.extracted[sink.extracted.size - 2])
        assertEquals(48_000L, sink.extracted.last().second)
    }
}
