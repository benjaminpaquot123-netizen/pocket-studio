package com.pocketstudio.app

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.audio.FloatWav
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.export.ExportRun
import com.pocketstudio.app.export.video.VideoExportViewModel
import com.pocketstudio.app.instruments.MultiBank
import com.pocketstudio.app.learn.LearnViewModel
import com.pocketstudio.app.rhythm.PadSamples
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.engine.CommandSink
import com.pocketstudio.engine.ExportResult
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.engine.NativeSample
import com.pocketstudio.model.DrumPattern
import com.pocketstudio.model.Harmony
import com.pocketstudio.model.Hit
import com.pocketstudio.model.InstrumentTrack
import com.pocketstudio.model.MultiInstrument
import com.pocketstudio.model.MultiPatch
import com.pocketstudio.model.MultiZone
import com.pocketstudio.model.Note
import com.pocketstudio.model.NoteClip
import com.pocketstudio.model.PackManifest
import com.pocketstudio.model.PadSample
import com.pocketstudio.model.Progression
import com.pocketstudio.model.Project
import com.pocketstudio.model.ProjectStore
import com.pocketstudio.model.Scale
import com.pocketstudio.model.Scene
import com.pocketstudio.model.SongPart
import com.pocketstudio.model.SongVersion
import com.pocketstudio.model.addTrack
import com.pocketstudio.model.learn.SoundGlossary
import com.pocketstudio.model.track
import com.pocketstudio.model.withTrack
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.io.File
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/** The review of phases 35 to 39 (app): the sounds of the versions, the chords tried, the instruments, the video. */
@RunWith(RobolectricTestRunner::class)
class Review3539AppTest {
    private val app: Application = ApplicationProvider.getApplicationContext()
    private val store = ProjectStore(File(app.filesDir, "projects"))

    @get:Rule
    val tmp = TemporaryFolder()

    /** The engine's stand-in: counts the samples let go and the private engines; a render can be held. */
    private class Sink : CommandSink {
        private var next = 1L
        val released = AtomicInteger()
        val created = AtomicInteger()
        val destroyed = AtomicInteger()
        @Volatile var rendering = false
        @Volatile var holdRender: CountDownLatch? = null
        override fun post(target: Long, type: Int, value: Float, a: Int, b: Int) = Unit
        override fun scheduleFrom(target: Long, frame: Long) = Unit
        override fun install(target: Long, kind: CommandSink.Install, index: Int, sample: Long) = Unit
        @Synchronized override fun sampleCreate(data: FloatArray, channels: Int, sampleRate: Int): Long = next++
        override fun sampleFrames(handle: Long): Long = 4800L
        override fun sampleRelease(handle: Long) {
            released.incrementAndGet()
        }
        @Synchronized override fun exportCreate(): Long {
            created.incrementAndGet()
            return next++
        }
        override fun exportRender(target: Long, path: String, frames: Long, tailFrames: Long, fadeOutFrames: Long, autoTail: Boolean, floatWav: Boolean, sampleRate: Int, bits: Int): ExportResult {
            val hold = holdRender ?: return ExportResult.Ok
            rendering = true
            hold.await(30, TimeUnit.SECONDS)
            return ExportResult.Cancelled
        }
        override fun exportRenderFloat(target: Long, frames: Long, tailFrames: Long): FloatArray {
            holdRender?.let {
                rendering = true
                it.await(30, TimeUnit.SECONDS)
            }
            return FloatArray(2 * 4800) { 0.1f * kotlin.math.sin(it * 0.05f) }
        }
        override fun exportDestroy(target: Long) {
            destroyed.incrementAndGet()
        }
    }

    private val sink = Sink()

    /** A factory for this test's application (AndroidViewModelFactory keeps the first test's). */
    private fun factory(make: () -> androidx.lifecycle.ViewModel) = object : androidx.lifecycle.ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T = make() as T
    }

    @After
    fun tearDown() {
        sink.holdRender?.countDown()
        NativeEngine.sink = null
        com.pocketstudio.app.projects.CurrentProject.set(app, RhythmViewModel.PROJECT_ID)
    }

    private fun waitUntil(what: String, seconds: Int = 60, check: () -> Boolean) {
        val until = System.currentTimeMillis() + seconds * 1000L
        while (!check()) {
            assertTrue(what, System.currentTimeMillis() < until)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(50))
            Thread.sleep(5)
        }
    }

    private fun idle(ms: Long) {
        repeat((ms / 50).toInt()) {
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(50))
            Thread.sleep(10)
        }
    }

    /** [p] saved as a project of its own and opened (the engine is the [sink]). */
    private fun open(p: Project, engine: Boolean = true, prepare: (String) -> Unit = {}): Pair<RhythmViewModel, String> {
        val id = "r3539-${System.nanoTime()}"
        store.save(id, p.copy(name = id))
        prepare(id)
        com.pocketstudio.app.projects.CurrentProject.set(app, id)
        if (engine) NativeEngine.sink = sink
        val vm = RhythmViewModel(app)
        waitUntil("the project never opened") { vm.project.value.name == id }
        waitUntil("the project's instruments were never looked for") { vm.multiLoading.value.isEmpty() }
        return vm to id
    }

    // --- G1: the sounds of the song versions -----------------------------------------------------

    @Test
    fun aVersionsPadSoundStaysOnDisk() {
        val lanes = DrumPattern().lanes.toMutableList().also { it[0] = it[0].copy(sample = PadSample("voixv.wav", "Voix"), hits = mapOf(0 to Hit())) }
        val versionScene = Scene("A", drums = DrumPattern(lanes = lanes))
        val p = Project(song = listOf(SongPart(0)), songVersions = listOf(SongVersion(song = listOf(SongPart(0, repeats = 2)), scenes = listOf(versionScene))))
        val (vm, id) = open(p) { pid ->
            val samples = File(store.folder(pid), "samples").apply { mkdirs() }
            FloatWav.write(File(samples, "voixv.wav"), Pcm(FloatArray(4800) { 0.1f }, 1, 48_000))
        }
        idle(1500)
        // Not used by the song now, but by a version kept aside: never cleaned away.
        assertTrue(File(store.folder(id), "samples/voixv.wav").isFile)
        // Brought back: the pads keep the project's sounds (shared by every scene); the sound the
        // version's scenes name is still there, so a .zip of the project misses none.
        vm.restoreSongVersion(0)
        runBlocking { vm.flushNow() }
        assertEquals(listOf(SongPart(0, repeats = 2)), vm.project.value.song)
        assertEquals(0, com.pocketstudio.app.projects.projectCatalog(app).exportZip(id, java.io.ByteArrayOutputStream()))
    }

    @Test
    fun aSoundNoLongerUsedWaitsAWeekAsideAndComesBackWhenNamedAgain() {
        val dir = tmp.newFolder("p", "samples")
        val now = System.currentTimeMillis()
        File(dir, "old.wav").writeBytes(ByteArray(8))
        PadSamples(dir).loadAll(emptySet(), cleanUp = true, now = now)
        val aside = File(dir.parentFile, PadSamples.RETIRED_DIR)
        assertFalse(File(dir, "old.wav").exists())
        assertTrue(File(aside, "old.wav").isFile)
        // Named again (a version brought back, a take chosen): back in samples/.
        PadSamples(dir).loadAll(setOf("old.wav"), cleanUp = false, now = now)
        assertTrue(File(dir, "old.wav").isFile)
        // A week later, a sound still unused goes for good.
        PadSamples(dir).loadAll(emptySet(), cleanUp = true, now = now)
        PadSamples(dir).loadAll(emptySet(), cleanUp = true, now = now + PadSamples.RETIRED_MS + 1)
        assertFalse(File(aside, "old.wav").exists())
    }

    // --- G2: the chords tried are never saved on their own ---------------------------------------

    @Test
    fun theChordsTriedReachTheDiskOnlyWithAppliquer() {
        // Do major held in each of the 4 bars: a progression moves the bars after the first.
        val chord = (0 until 4).flatMap { bar -> listOf(60, 64, 67).map { Note(bar * 384, 96, it) } }
        val p = Project(harmony = Harmony(0, Scale.Major), synthNotes = NoteClip(64, chord), synthFollowChords = true, song = listOf(SongPart(0)))
        val (vm, id) = open(p, engine = false)
        val before = vm.project.value.synthNotes
        vm.tryProgression(Progression.entries.first { pr -> pr.degrees.distinct().size > 1 })
        assertNotEquals("the chords tried are heard", before, vm.project.value.synthNotes)
        runBlocking { vm.flushNow() }
        // Saved (the app going to the background, then killed): the song as it was before the tries.
        assertEquals(before, store.load(id)!!.synthNotes)
        vm.applyChords()
        runBlocking { vm.flushNow() }
        assertEquals(vm.project.value.synthNotes, store.load(id)!!.synthNotes)
        assertNotEquals(before, store.load(id)!!.synthNotes)
    }

    // --- M1, M2: the multizone instruments' memory ------------------------------------------------

    private fun manifest(pack: String) = PackManifest(pack, pack, instrument = MultiInstrument(memoryBytes = 120_000_000L, zones = listOf(MultiZone("z.wav"))))

    private fun twoInstruments(): Project {
        val p = Project(synthEngine = InstrumentTrack.ENGINE_MULTI, multi = MultiPatch("pack-a", "A")).addTrack()!!
        return p.withTrack(p.track(1).copy(engine = InstrumentTrack.ENGINE_MULTI, multi = MultiPatch("pack-b", "B")))
    }

    private fun RhythmViewModel.instrumentsFrom(gate: CountDownLatch, calls: AtomicInteger) {
        multiManifest = { _, pack -> manifest(pack) }
        multiLoader = { _, pack ->
            calls.incrementAndGet()
            gate.await(30, TimeUnit.SECONDS)
            MultiBank(pack, 1, manifest(pack).instrument!!, listOf(NativeSample.createCompact(FloatArray(96), 1, 48_000)!!))
        }
        instrumentsChanged()
    }

    @Test
    fun twoInstrumentsOpenedTogetherStillKeepToTheBudget() {
        val (vm, _) = open(twoInstruments())
        val gate = CountDownLatch(1)
        val calls = AtomicInteger()
        vm.instrumentsFrom(gate, calls)
        waitUntil("no instrument was read") { calls.get() >= 1 }
        idle(800)
        // 120 MB each, 200 MB in all: the second waits for nothing, it plays its synth sound.
        assertEquals(1, calls.get())
        assertTrue(vm.notice.value.orEmpty(), vm.notice.value.orEmpty().contains("Trop d'instruments"))
        gate.countDown()
        waitUntil("the instrument never finished loading") { vm.multiLoading.value.isEmpty() }
    }

    @Test
    fun anInstrumentReadWhileTheScreenClosesIsFreed() {
        val id = "r3539-${System.nanoTime()}"
        val p = Project(name = id, synthEngine = InstrumentTrack.ENGINE_MULTI, multi = MultiPatch("pack-a", "A"))
        store.save(id, p)
        com.pocketstudio.app.projects.CurrentProject.set(app, id)
        NativeEngine.sink = sink
        val owner = androidx.lifecycle.ViewModelStore()
        val vm = androidx.lifecycle.ViewModelProvider(owner, factory { RhythmViewModel(app) })[RhythmViewModel::class.java]
        waitUntil("the project never opened") { vm.project.value.name == id && vm.multiLoading.value.isEmpty() }
        val gate = CountDownLatch(1)
        val calls = AtomicInteger()
        vm.instrumentsFrom(gate, calls)
        waitUntil("the instrument was never read") { calls.get() >= 1 }
        val releasedBefore = sink.released.get()
        owner.clear()
        gate.countDown()
        waitUntil("the instrument read meanwhile stays in memory", seconds = 15) { sink.released.get() > releasedBefore }
    }

    // --- M4: the video and the exports, one at a time ---------------------------------------------

    @Test
    fun theVideoKeepsExportsWaitingRunsInTheForegroundAndLeavesNothingWhenCancelled() {
        val lanes = DrumPattern().lanes.toMutableList().also { it[0] = it[0].copy(hits = mapOf(0 to Hit(), 8 to Hit())) }
        val (vm, _) = open(Project(drums = DrumPattern(lanes = lanes), song = listOf(SongPart(0, repeats = 16))))
        val exports = File(app.cacheDir, "exports")
        sink.holdRender = CountDownLatch(1)
        val video = VideoExportViewModel(app)
        video.start(vm)
        waitUntil("the video never rendered its sound") { sink.rendering }
        assertTrue(vm.exportBusy)
        assertNotNull("the video runs in the foreground", ExportRun.state.value)
        vm.exportWav()
        waitUntil("an export started during the video") { vm.export.value.error?.contains("vidéo") == true }
        video.cancel()
        sink.holdRender!!.countDown()
        waitUntil("the video never stopped") { !video.state.value.running }
        idle(300)
        assertFalse(vm.exportBusy)
        assertNull(ExportRun.state.value)
        assertTrue(exports.listFiles().orEmpty().none { it.name.contains("-video") })
        assertEquals(sink.created.get(), sink.destroyed.get())
    }

    // --- Minor points -------------------------------------------------------------------------------

    @Test
    fun refaireGivesBackTheNotesTheTakeReplaced() {
        File(app.filesDir, "projects").deleteRecursively()
        val vm = RhythmViewModel(app).also { it.playingForTest = true }
        val mine = Note(NoteClip.TICKS_PER_STEP, NoteClip.TICKS_PER_STEP, 60)
        vm.session.edit { it.copy(synthNotes = NoteClip(16, listOf(mine))) }
        vm.recordTake(com.pocketstudio.app.record.TakeSource.KEYS)
        // The same key played on it: the take's note replaces it.
        vm.recordPositionForTest = 1.0
        vm.playedClockForTest = 1.0
        vm.synthNoteOn(60, 0.6f)
        vm.recordPositionForTest = 2.0
        vm.playedClockForTest = 2.0
        vm.synthNoteOff(60)
        vm.recordTake(com.pocketstudio.app.record.TakeSource.KEYS)
        // (The loop grew to 4 bars for the take: the first bar is the one played on.)
        fun firstBar() = vm.project.value.synthNotes.notes.filter { it.start < 16 * NoteClip.TICKS_PER_STEP }
        assertEquals(listOf(0.6f), firstBar().map { it.velocity })
        // « Refaire »: the take goes, the note it replaced comes back (and the new take records nothing).
        vm.redoTake()
        assertEquals(listOf(mine), firstBar())
        vm.recordTake(com.pocketstudio.app.record.TakeSource.KEYS)
        assertEquals(listOf(mine), firstBar())
    }

    @Test
    fun aSheetLeftOpenOnADeletedTrackNeverChangesTheFirstOne() {
        File(app.filesDir, "projects").deleteRecursively()
        val vm = RhythmViewModel(app)
        vm.addTrack()
        val id = vm.project.value.tracks.single().id
        vm.removeTrack(id)
        val first = vm.project.value.track(0)
        vm.renameTrack(id, "Pas moi")
        vm.useMultiInstrument("piano-test", "Piano", "Piano", id = id)
        vm.use808(id = id)
        assertEquals(first, vm.project.value.track(0))
    }

    // --- The glossary's examples -------------------------------------------------------------------

    @Test
    fun aGlossaryExampleStoppedOrLeftFreesItsEngine() {
        NativeEngine.sink = sink
        sink.holdRender = CountDownLatch(1)
        val owner = androidx.lifecycle.ViewModelStore()
        val learn = androidx.lifecycle.ViewModelProvider(owner, factory { LearnViewModel(app) })[LearnViewModel::class.java]
        learn.sync = {}
        learn.play(SoundGlossary.ALL.first { it.key == "kick" })
        waitUntil("the example was never rendered") { sink.rendering }
        assertEquals("kick", learn.playing.value?.key)
        // Another touch (stop), then the screen left while it renders: the private engine goes.
        learn.stop()
        assertNull(learn.playing.value)
        owner.clear()
        sink.holdRender!!.countDown()
        waitUntil("the example's engine was never closed", seconds = 15) { sink.destroyed.get() == sink.created.get() }
        assertNull(learn.playing.value)
    }
}
