package com.pocketstudio.app.human

import android.app.Application
import android.media.MediaFormat
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.projects.CurrentProject
import com.pocketstudio.app.rhythm.ExportFormat
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.model.Project
import com.pocketstudio.model.ProjectStore
import com.pocketstudio.model.SongPart
import com.pocketstudio.model.partSteps
import com.pocketstudio.model.sceneSteps
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowMediaCodec
import java.io.File
import java.time.Duration

/**
 * Lot « tuto 4 », É0: the « testeur humain » bench's stand-ins for the phone's engine. « Partager »
 * gives a file to share (no « le moteur audio n'est pas prêt »), and the play head moves with time.
 */
@RunWith(RobolectricTestRunner::class)
class BenchEngineTest {
    /** PLAN-RESTE 4.5: the state inherited from the test before must be pristine (else: red, naming that test). */
    @get:Rule
    val pristine = BenchPristineRule()
    private val app: Application get() = ApplicationProvider.getApplicationContext()

    @After
    fun clear() {
        NativeEngine.sink = null
        ShadowMediaCodec.clearCodecs()
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(20))

    private fun open(p: Project): RhythmViewModel {
        val id = "bench-${System.nanoTime()}"
        ProjectStore(File(app.filesDir, "projects")).save(id, p)
        CurrentProject.set(app, id)
        val vm = RhythmViewModel(app)
        val until = System.currentTimeMillis() + 10_000
        while (vm.project.value.song.size != p.song.size) {
            assertTrue("the project never opened", System.currentTimeMillis() < until)
            idle(); Thread.sleep(5)
        }
        return vm
    }

    private fun share(format: ExportFormat): Pair<RhythmViewModel, List<File>> {
        val passThrough = ShadowMediaCodec.CodecConfig.Codec { input, output ->
            val n = minOf(input.remaining(), output.remaining())
            output.put(input.duplicate().also { it.limit(it.position() + n) })
            input.position(input.position() + n)
        }
        ShadowMediaCodec.addEncoder(MediaFormat.MIMETYPE_AUDIO_AAC, ShadowMediaCodec.CodecConfig(16 * 1024, 16 * 1024, passThrough))
        val vm = open(Project(song = listOf(SongPart(0), SongPart(0))))
        val engine = BenchEngine()
        NativeEngine.sink = engine
        val shared = ArrayList<List<File>>()
        val scope = CoroutineScope(Dispatchers.Main.immediate)
        scope.launch { vm.shareRequests.collect { shared += it } }
        vm.shareSong(format)
        val until = System.currentTimeMillis() + 60_000
        while (shared.isEmpty() && vm.export.value.error == null) {
            assertTrue("the share never ended (${vm.export.value})", System.currentTimeMillis() < until)
            idle(); Thread.sleep(10)
        }
        scope.cancel()
        assertNull("no « moteur audio » error", vm.export.value.error)
        assertTrue(engine.renders >= 1)
        return vm to shared.single()
    }

    @Test
    fun anM4aIsMadeAndShared() {
        val (vm, files) = share(ExportFormat.M4a)
        assertEquals("m4a", files.single().extension)
        assertTrue(files.single().length() > 1000)
        assertEquals(false, vm.export.value.sharing)  // the progress window is gone, Android's menu opens
    }

    @Test
    fun aWavIsMadeAndShared() {
        val (vm, files) = share(ExportFormat.Wav)
        val f = files.single()
        assertEquals("wav", f.extension)
        // Audit ultra round 3 (lot A d, « Length: 3 s » for any song): the song's own length, float stereo at 48 kHz
        // (its tail cut once the sound died away: at most 0,5 s more).
        val p = vm.project.value
        val song = p.song.sumOf { p.partSteps(it) } * 15.0 / p.tempoBpm
        val seconds = (f.length() - 44) / 8.0 / 48_000
        assertTrue("$seconds s for a song of $song s", song > 3.5 && seconds in song - 0.01..song + 0.51)
    }

    /**
     * T5-Cc-41b (round 5, seat C c: « 1 KB » in « Mes exports »; recette tel-04 on the phone: 42 Mo for 2 min 26): the bench's
     * encoder is a shadow that copies what it is given and its engine a stand-in, so an export's size and the time it took are
     * not a phone's. The bench says so in its sound line (PLAN-RESTE 4.7), so that a tester never reports them as the app's.
     */
    @Test
    fun anExportSizeIsSaidToBeTheBenchs() {
        val (_, files) = share(ExportFormat.M4a)
        val f = files.single()
        val note = HumanTester.shareNote(listOf(f.name to f.length()), "WhatsApp")
        assertTrue(note, f.name in note && "WhatsApp" in note)
        assertTrue("the bench's note says the size is the bench's, not a phone's: $note", "banc" in note.lowercase() && "représentativ" in note)
    }

    /** A render of [frames] (and [tail]) by [engine], its project described by [describe] (round 3, lot A d). */
    private fun render(engine: BenchEngine, frames: Long, tail: Long = 0L, autoTail: Boolean = false, describe: (Long) -> Unit): File {
        val t = engine.exportCreate()
        describe(t)
        val f = File(app.cacheDir, "render-$t-${System.nanoTime()}.wav")
        assertEquals(com.pocketstudio.engine.ExportResult.Ok, engine.exportRender(t, f.path, frames, tail, 0L, autoTail, true, 48_000, 32))
        engine.exportDestroy(t)
        return f
    }

    private fun seconds(f: File) = (f.length() - 44) / 8.0 / 48_000

    /** A 1-bar loop: 4 kicks at 120 BPM. */
    private fun loop(e: BenchEngine, t: Long) {
        e.post(t, 6, 120f, 0, 0)
        for (s in 0 until 4) e.post(t, 10, 1f, 0, s * 4)
    }

    /** A 4-bar song: the drums busier, a bass, its channels a little lower. */
    private fun song(e: BenchEngine, t: Long) {
        e.post(t, 6, 120f, 0, 0)
        for (part in 0 until 4) for (s in 0 until 16) e.post(t, 10, 0.9f, s % 3, s)
        for (ch in 0 until 4) e.post(t, 20, 0.7f, ch, 0)
    }

    /**
     * Audit ultra round 3 (lot A d): every export said « 3 s » and the same LUFS, a loop of 1 bar as a song of 4.
     * The bench's render lasts what the app asks (its tail cut once the sound died away) and sounds like the project
     * described to it: two projects measure differently with the app's own meter; a target still brings them to it.
     */
    @Test
    fun aRenderLastsWhatIsAskedAndSoundsLikeItsProject() {
        val e = BenchEngine().apply { renderMs = 0L }
        val bar = 96_000L  // 2 s at 120 BPM
        val a = render(e, bar) { loop(e, it) }
        val b = render(e, 4 * bar) { song(e, it) }
        assertEquals(2.0, seconds(a), 0.01)
        assertEquals(8.0, seconds(b), 0.01)
        val la = com.pocketstudio.app.export.Loudness.integrated(a)!!
        val lb = com.pocketstudio.app.export.Loudness.integrated(b)!!
        assertTrue("the loop $la LUFS, the song $lb LUFS", kotlin.math.abs(la - lb) > 1.0)
        assertEquals("the same project, the same sound", la, com.pocketstudio.app.export.Loudness.integrated(render(e, bar) { loop(e, it) })!!, 0.05)
        // A song's tail: at most 0,5 s once the sound died away (autoTail), the whole tail asked without it.
        assertEquals(2.5, seconds(render(e, bar, tail = 10 * 48_000L, autoTail = true) { loop(e, it) }), 0.01)
        assertEquals(3.0, seconds(render(e, bar, tail = 48_000L) { loop(e, it) }), 0.01)
        // Round 2's targets, each project brought to them (the master's two passes): club −8, streaming −14.
        for (target in listOf(-8f, -14f)) for (describe in listOf<(Long) -> Unit>({ loop(e, it) }, { song(e, it) })) {
            val f = render(e, 4 * bar) { t -> describe(t); e.post(t, 67, target, 4, 0) }
            assertEquals("target $target", target.toDouble(), com.pocketstudio.app.export.Loudness.integrated(f)!!, 0.5)
        }
        // Rendered to memory (a frozen track, a pad's loop): the length asked too.
        val t = e.exportCreate()
        loop(e, t)
        assertEquals(2 * (bar + 4_800), e.exportRenderFloat(t, bar, 4_800L).size.toLong())
    }

    /** [ms] of the bench's clock go by (the main looper's, which the bench's engine reads). */
    private fun pass(ms: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))

    /** [p] opened with the bench's engine there before the app, as on a seat (and as a phone's). */
    private fun openWithEngine(p: Project): Pair<RhythmViewModel, BenchEngine> {
        val engine = BenchEngine()
        NativeEngine.sink = engine
        val vm = open(p)
        val until = System.currentTimeMillis() + 10_000
        while (vm.project.value.tempoBpm != p.tempoBpm) {
            assertTrue("the project never opened", System.currentTimeMillis() < until)
            idle(); Thread.sleep(5)
        }
        vm.engineStarted()  // as the app's screen says once the audio stream started (AppRoot): the project is sent to the engine
        return vm to engine
    }

    /**
     * Audit « banc crédible » A.9 (F4): one clock. With the bench's engine there, the play head shows what the engine says (as
     * the app's pollLive does on a phone) and the take's clock is the engine's transport step. The head had a clock of its own:
     * after a tempo change it counted the whole time since ▶ at the new tempo (16 steps at 120 BPM became 8 at 60 BPM) while
     * the engine kept its 16.
     */
    @Test
    fun thePlayHeadReadsTheEnginesClock() {
        val (vm, engine) = openWithEngine(Project().copy(tempoBpm = 121.0))
        vm.setTempo(120.0)
        val head = BenchPlayhead()
        vm.togglePlay()
        idle()
        assertTrue(vm.transport.value.playing)
        head.tick(vm, android.os.SystemClock.uptimeMillis())  // the bench ticks it every 50 ms from ▶ on
        pass(2_000)
        head.tick(vm, android.os.SystemClock.uptimeMillis())
        assertEquals("16 steps after 2 s at 120 BPM", 16, vm.takeClockForTest)
        vm.setTempo(60.0)
        head.tick(vm, android.os.SystemClock.uptimeMillis())
        assertEquals("the play head keeps the steps played at 60 BPM", 16, vm.takeClockForTest)
        assertEquals(16, vm.live.value.absoluteStep)
        assertEquals("the take's clock is the engine's transport step", 16, engine.transportStep())
        pass(1_000)  // 4 steps at 60 BPM
        head.tick(vm, android.os.SystemClock.uptimeMillis())
        assertEquals(20, vm.takeClockForTest)
        assertEquals(20, vm.live.value.absoluteStep)
    }

    /**
     * T5-Ab-27 (audit « banc crédible » A.7, F1): on the bench as on a phone, a take of the microphone on the whole song,
     * left running, is kept when the song ends by itself: the bench's engine stops on the last bar line and ends the take
     * there, the app writes it onto its track (TakeVault.Outcome.Saved), a clip as long as the song. The app's side
     * without the bench: TakeAtTheSongsEndTest.
     */
    @Test
    fun aMicTakeLeftRunningIsKeptWhenTheSongEnds() {
        val (vm, engine) = openWithEngine(Project(song = listOf(SongPart(0), SongPart(0))))
        val song = vm.project.value.let { p -> p.song.sumOf { p.partSteps(it) } * 15.0 / p.tempoBpm }
        vm.setRecordTarget(com.pocketstudio.app.rhythm.RecordTarget(song = true, fromPart = 0))
        val track = vm.micTakeTrack()!!
        vm.recordAudio(track)
        var recording = false
        val until = System.currentTimeMillis() + 90_000
        while (vm.project.value.audioTracks.firstOrNull { it.id == track }?.clip == null || vm.takeSaving.value) {
            assertTrue("the take never ended on its track (recording ${vm.audioRecording.value}, notice ${vm.notice.value})", System.currentTimeMillis() < until)
            recording = recording || vm.audioRecording.value?.waiting == false
            pass(50); Thread.sleep(3)
        }
        assertTrue("it recorded", recording)
        val clip = vm.project.value.audioTracks.first { it.id == track }.clip!!
        assertTrue("a clip of the song", clip.song)
        assertTrue("as long as the song ($song s): ${clip.seconds} s", clip.seconds in song - 0.6..song + 0.3)
        assertEquals("the app stopped with the song", false, vm.transport.value.playing)
        assertNull(vm.audioRecording.value)
        assertEquals(1, engine.micTakes)
    }

    /**
     * Audit « banc crédible » A.8 (F2): a setting moved while the music plays with ● Automation on is recorded at the step
     * the engine plays (RhythmViewModel.recordAutomation reads NativeEngine.currentStep()). The bench's engine had no
     * currentStep (-1 for good): nothing was ever recorded on the bench, a false « ● does nothing » for a seat and a real
     * defect of the recording hidden.
     */
    @Test
    fun aKnobMovedWithAutomationRecordWritesAPoint() {
        val (vm, engine) = openWithEngine(Project().copy(tempoBpm = 100.0))  // no song: a volume's curve is the loop's
        vm.togglePlay()
        idle()
        vm.toggleAutomationRecord()
        pass(800)  // 150 ms a step at 100 BPM: step 5 (820 ms with the 20 ms above)
        assertEquals("the engine says the step it plays", 5, engine.currentStep())
        vm.setChannelVolume(0, 0.3f)
        val lanes = vm.project.value.automation
        assertEquals("one curve, made by the gesture", 1, lanes.size)
        assertEquals(com.pocketstudio.model.AutoTarget(com.pocketstudio.model.AutoTarget.VOLUME, 0), lanes[0].target)
        assertEquals("a loop's curve", com.pocketstudio.model.AutomationLane.LOOP, lanes[0].mode)
        assertEquals("one point, on the step playing", listOf(5), lanes[0].values.indices.filter { lanes[0].values[it] != null })
        assertEquals(0.3f, lanes[0].values[5]!!, 0.001f)
        pass(1_650)  // 11 steps more: step 16 = step 0 of the next pass of the 16-step drum loop
        vm.setChannelVolume(0, 0.6f)
        assertEquals("the next pass writes on the pattern's own steps", listOf(0, 5), vm.project.value.automation[0].values.indices.filter { vm.project.value.automation[0].values[it] != null })
    }

    /** A.8 (F2), the song's lane: while the whole song plays, a volume moved is written on the song's bar playing. */
    @Test
    fun aKnobMovedWhileTheSongPlaysWritesOnTheSongsBar() {
        val (vm, engine) = openWithEngine(Project(song = listOf(SongPart(0), SongPart(0), SongPart(0))).copy(tempoBpm = 100.0))
        vm.toggleSong()
        idle()
        vm.toggleAutomationRecord()
        pass(3_000)  // step 20: bar 2 of the song, step 4 of the pattern
        assertEquals(20, engine.songStep())
        assertEquals(4, engine.currentStep())
        vm.setChannelVolume(0, 0.3f)
        val lanes = vm.project.value.automation
        assertEquals("one curve, made by the gesture (notice: ${vm.notice.value})", 1, lanes.size)
        assertEquals("a volume moves along the song", com.pocketstudio.model.AutomationLane.SONG, lanes[0].mode)
        assertEquals("one point, on the song's second bar", listOf(1), lanes[0].songValues.indices.filter { lanes[0].songValues[it] != null })
        assertEquals(0.3f, lanes[0].songValues[1]!!, 0.001f)
    }

    @Test
    fun thePlayHeadMovesWithTheTempo() {
        val vm = open(Project(song = listOf(SongPart(0))).copy(tempoBpm = 120.0))
        val loop = vm.project.value.let { it.sceneSteps(it.scene) }
        val spb = vm.project.value.stepsPerBar
        val head = BenchPlayhead()
        head.tick(vm, 0L)
        assertEquals(-1, vm.playBar.value)  // stopped: nothing said
        vm.togglePlay()
        idle()
        assertTrue(vm.transport.value.playing)
        head.tick(vm, 1_000L)
        assertEquals(0, vm.playBar.value)
        // At 120 BPM a bar lasts 2 s: half a bar later, the head is on step 8 of bar 1.
        head.tick(vm, 2_000L)
        assertEquals(8 % loop, vm.live.value.currentStep)
        head.tick(vm, 3_000L)
        assertEquals(16 % loop / spb, vm.playBar.value)
        assertEquals(16 % loop, vm.live.value.currentStep)
        assertEquals(16, vm.live.value.absoluteStep)
    }
}
