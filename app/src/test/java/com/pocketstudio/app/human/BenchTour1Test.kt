package com.pocketstudio.app.human

import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.engine.NativeEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Audit ultra round 1, the bench's own faults (docs/audits/ultra/tour-1-banc.md): what a phone does and the bench did
 * not (« Exporter » of the Mix tab, the microphone, « Écouter » of a performance, the meters, « Montre-moi »'s sounds,
 * the video), and its « rien ne change » stop (only looking, a long work under way).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = HumanTester.PHONE)
class BenchTour1Test : HumanTester("tour-1") {
    @Before
    fun notOnTheBench() = assumeTrue("not while the bench runs seats", System.getProperty("ps.human.dir").isNullOrBlank())

    private fun journal() = File.createTempFile("banc", ".txt").apply { deleteOnExit() }

    /** An empty project in Pro, as the amenée « premier-morceau » gives it. */
    private fun emptyProjectInPro(): SeatOptions {
        val o = SeatOptions(simple = false, amenee = AMENEE_SONG)
        begin(o)
        amenee(AMENEE_SONG, journal())
        return o
    }

    private fun tap(tag: String): String {
        val at = reachTag(tag) ?: error("no « $tag » on:\n" + screenLines().joinToString("\n"))
        return act("touche ${at.first} ${at.second}")
    }

    private fun waitFor(what: String, seconds: Int = 60, state: () -> String = { "" }, done: () -> Boolean) {
        val until = System.currentTimeMillis() + seconds * 1000L
        while (!done()) {
            assertTrue("$what never came (${state()}):\n" + screenLines().joinToString("\n"), System.currentTimeMillis() < until)
            act("attends 0.5")
        }
    }

    /** Lot A c, E d, F c: « Exporter » of the Mix tab did nothing (the app renders only with an engine). */
    @Test
    fun theMixTabsExportRenders() {
        emptyProjectInPro()
        tap("tab_Mix")
        centerOf("export_what_Loop")?.let { tap("export_what_Loop") }
        tap("export")
        val vm = rhythm()!!
        waitFor("the export") { vm.export.value.running.not() && (vm.export.value.files.isNotEmpty() || vm.export.value.error != null) }
        assertEquals("no error", null, vm.export.value.error)
        assertTrue("a file written", vm.export.value.files.isNotEmpty())
        assertTrue("rendered by the bench's engine", benchEngine.renders > 0)
    }

    /** Lot A d, C c, E b: the microphone heard nothing (« Rien n'a été entendu »): a pad gets the voice. */
    @Test
    fun theMicrophoneHearsAVoice() {
        emptyProjectInPro()
        // The project opened rebuilds the activity a moment later: its view model is the one the take runs on.
        act("attends 1")
        val vm = rhythm()!!
        vm.startRecording()
        // The recorder starts off the main thread: the voice is heard from its first read (the bench's clock jumps).
        val until = System.currentTimeMillis() + 10_000
        while (BenchMic.opened == 0 && System.currentTimeMillis() < until) Thread.sleep(10)
        act("attends 2")
        vm.stopRecording()
        waitFor("the take on the pad", state = { "${vm.sampleTask.value}, ${BenchMic.delivered} frames heard" }) { vm.sampleTask.value !is com.pocketstudio.app.rhythm.SampleTask.Recording && vm.sampleTask.value !is com.pocketstudio.app.rhythm.SampleTask.Busy }
        assertTrue("the microphone was read for the 2 s: ${BenchMic.delivered}", BenchMic.delivered >= 48_000)
        val pad = vm.transport.value.selectedPad
        val sample = project()!!.drums.lanes[pad].sample
        assertTrue("« Micro … » on the pad: ${vm.sampleTask.value} ${sample?.name}", sample?.name?.startsWith("Micro") == true)
    }

    /** Lot A d, C c, E b: « Enregistrer » of an audio track: the count-in, then the bench's voice, kept on the track. */
    @Test
    fun anAudioTrackTakeRecords() {
        emptyProjectInPro()
        val vm = rhythm()!!
        vm.addAudioTrack()
        val id = project()!!.audioTracks.last().id
        NativeEngine.sink = benchEngine
        vm.recordAudio(id)
        waitFor("the take recording") { (vm.audioRecording.value?.seconds ?: 0f) > 1f }
        assertTrue("the input level shows", (vm.audioRecording.value?.level ?: 0f) > 0.1f)
        vm.stopAudioRecording()
        waitFor("the take on the track") { !vm.takeSaving.value && project()!!.audioTracks.first { it.id == id }.clip != null }
        assertEquals(1, benchEngine.micTakes)
    }

    /**
     * PLAN-RESTE 5.6 (control 7): a microphone take started by a touch on [startTag] (the audio card's « Enregistrer »,
     * the top ● in its microphone mode, or « Le micro » of the ●'s choice) and ended by one on [stopTag] lands on an audio
     * track as a new clip: the bench's microphone was heard (its level shows) and its take kept, as on a phone.
     */
    private fun micTake(what: String, startTag: String, stopTag: String) {
        val vm = rhythm()!!
        val takes = benchEngine.micTakes
        val clipsBefore = project()!!.audioTracks.mapNotNull { it.clip?.file }.toSet()
        val said = tap(startTag)
        waitFor("$what: the take recording", state = { "${vm.audioRecording.value}; said: $said" }) { (vm.audioRecording.value?.seconds ?: 0f) > 1f }
        assertTrue("$what: the input level shows", (vm.audioRecording.value?.level ?: 0f) > 0.1f)
        tap(stopTag)
        waitFor("$what: the take on its track", state = { "${vm.audioRecording.value}, saving ${vm.takeSaving.value}" }) {
            vm.audioRecording.value == null && !vm.takeSaving.value && project()!!.audioTracks.any { t -> t.clip?.file?.let { it !in clipsBefore } == true }
        }
        assertEquals("$what: one take recorded by the bench's microphone", takes + 1, benchEngine.micTakes)
    }

    /** Control 7, Pro: the microphone from the audio card's « Enregistrer », then from the top ● (the microphone's on that page). */
    @Test
    fun theMicrophoneRecordsFromTheCardAndTheTopDotInPro() {
        emptyProjectInPro()
        act("attends 1")
        tap("tab_Sons")
        tap("sons_Audio")
        waitFor("the audio tracks' page") { centerOf("audio_tracks") != null }
        if (project()!!.audioTracks.isEmpty()) tap("audio_add")
        val id = project()!!.audioTracks.first().id
        micTake("the card's « Enregistrer » (Pro)", "audio_record_$id", "audio_stop")
        micTake("the top ● (Pro)", "top_record", "top_record")
    }

    /**
     * Control 7, Mode simple: the top ● asks what to record, « Le micro » opens the microphone's page and starts the take
     * (the ● stops it); then the card's « Nouvelle prise » on that page.
     */
    @Test
    fun theMicrophoneRecordsFromTheTopDotAndTheCardInSimple() {
        begin(SeatOptions(simple = true, amenee = AMENEE_SONG))
        amenee(AMENEE_SONG, journal())
        act("attends 1")
        tap("top_record")
        waitFor("« Le micro » among the ●'s choices") { centerOf("record_choice_mic") != null }
        micTake("the top ● › « Le micro » (Simple)", "record_choice_mic", "top_record")
        val id = project()!!.audioTracks.first { it.clip != null }.id
        micTake("the card's « Nouvelle prise » (Simple)", "audio_record_$id", "audio_stop")
    }

    /**
     * The « rien ne change » stop: never on screens only looked at, never while a long work shows its progress, still
     * after 5 identical screens otherwise (« ecris » with no field: the same screen each time).
     */
    @Test
    fun nothingChangesIgnoresLooksAndLongWork() {
        val o = emptyProjectInPro()
        fun seat(vararg cmds: String): File {
            val dir = kotlin.io.path.createTempDirectory("siege").toFile()
            cmds.forEachIndexed { i, c -> File(dir, "cmd-${i + 1}.txt").writeText(c) }
            seatLoop(dir, o, File(dir, "journal.txt"))
            return dir
        }
        val looked = seat(*Array(6) { "regarde" }, "fin")
        assertEquals(File(looked, "journal.txt").readText(), "fin", File(looked, "fin.txt").readText())
        val stuck = seat(*Array(8) { "ecris a" }, "fin")
        assertTrue(File(stuck, "fin.txt").readText(), File(stuck, "fin.txt").readText().startsWith("rien ne change"))

        // An export of 60 s under way: the screen waits for it, the tester's touches do not end the seat.
        benchEngine.renderMs = 60_000
        NativeEngine.sink = benchEngine
        val vm = rhythm()!!
        vm.setExportWhat(com.pocketstudio.app.rhythm.ExportWhat.Loop)
        vm.exportWav()
        act("attends 0.5")
        assertTrue("the export runs", vm.export.value.running)
        val busy = seat(*Array(8) { "ecris a" }, "fin")
        assertTrue("still running", longWorkShown())
        assertEquals(File(busy, "journal.txt").readText(), "fin", File(busy, "fin.txt").readText())
        vm.cancelExport()
    }

    /**
     * Zone 3, n° 27 (seats D b, C a): the song playing turned into its first part's loop at a gesture that did not need
     * the engine (a tab, a sound changed): the bench stopped it and played ▶ again without its engine. The song goes on.
     */
    @Test
    fun theSongGoesOnAfterAnotherGesture() {
        begin()
        val at = centerOfText("Fais-moi un morceau", substring = true)!!
        act("touche ${at.first} ${at.second}")
        tap("mode_card_simple")
        tap("wizard_family_Techno")
        tap("wizard_style_techno")
        tap("wizard_style_choose_techno")
        tap("wizard_create_now")
        waitFor("the song's tabs") { screenRoute().endsWith("tab_simple_Morceau") }
        val heard = tap("thumb_play")
        assertTrue(heard, "le morceau joue (partie" in heard)
        val after = tap("tab_simple_Rythme")
        assertTrue("the song still plays, not a loop: $after", "le morceau joue (partie" in after && "en boucle" !in after)
        assertTrue(screenRoute(), screenRoute().endsWith("tab_simple_Rythme"))
        val later = act("attends 2")
        assertTrue(later, "le morceau joue (partie" in later)
        assertTrue("the screen's work never failed: $problems", problems.isEmpty())
    }

    /** Lot E d, F c: the live meters showed only dashes; while the music plays, the master reads −14 LUFS. */
    @Test
    fun theLiveMetersRead() {
        begin(SeatOptions(simple = false))
        tap("launch_continue")
        tap("thumb_play")
        val vm = rhythm()!!
        assertTrue(vm.transport.value.playing)
        assertEquals(-14.0f, vm.loudness.value[NativeEngine.LOUDNESS_SHORT_TERM], 0.01f)
        assertTrue("the drums move", vm.meters.value[0] > 0f)
        tap("thumb_play")
        assertEquals("stopped: no short-term reading", NativeEngine.LOUDNESS_NONE, vm.loudness.value[NativeEngine.LOUDNESS_SHORT_TERM], 0.01f)
        assertEquals("the integrated reading stays", -14.2f, vm.loudness.value[NativeEngine.LOUDNESS_INTEGRATED], 0.01f)
    }

    /** Lot B d: « Montre-moi » played its pads through the engine; the bench hears what the app sends it. */
    @Test
    fun padsPlayedThroughTheEngineAreHeard() {
        emptyProjectInPro()
        NativeEngine.sink = benchEngine
        NativeEngine.triggerPad(0, 0.9f)
        val h = heard()
        assertTrue(h, Regex("le pad « [^»]+ » sonne").containsMatchIn(h))
        NativeEngine.sink = null
    }

    /** Lot E a: « Écouter » of the performance kept played nothing (the song plays from its part with an engine). */
    @Test
    fun aKeptPerformanceIsHeard() {
        padsAsOnASeat = true
        begin(SeatOptions(amenee = AMENEE_PADLIVE))
        amenee(AMENEE_PADLIVE, journal())
        val vm = rhythm()!!
        val loop = (0 until 16).first { project()!!.live.at(it)?.oneShot == false }
        tap("live_record")
        tap("padlive_pad_$loop")
        waitFor("the take recording") { vm.take.value is com.pocketstudio.app.live.TakeUi.Recording }
        act("attends 2")
        tap("live_record")
        waitFor("the take written") { vm.take.value is com.pocketstudio.app.live.TakeUi.Done }
        if (vm.transport.value.playing) tap("padlive_play")
        val listen = listOf("padlive_perf_listen", "live_take_listen").firstOrNull { centerOf(it) != null }
        assertNotNull("« Écouter » of the take:\n" + screenLines().joinToString("\n"), listen)
        val heard = tap(listen!!)
        assertTrue(heard, "le morceau joue" in heard)
    }

    /** Lot C a: « Créer la vidéo » said « Le moteur audio n'est pas prêt » (the bench's engine renders its sound). */
    @Test
    fun theVideoIsMade() {
        begin(SeatOptions(simple = false))
        tap("launch_continue")
        val video = com.pocketstudio.app.export.video.VideoExportViewModel(ApplicationProvider.getApplicationContext())
        NativeEngine.sink = benchEngine
        video.start(rhythm()!!)
        // Its sound rendered by the bench's engine, then its pictures begun: enough. Robolectric's MediaCodec does not
        // compress (raw frames: a 30 s video grew past 3 GB and filled the disk, round 1 integration), so it stops there.
        waitFor("the video's pictures", seconds = 300) {
            video.state.value.stage == com.pocketstudio.app.export.video.VideoExportUi.Stage.Pictures && video.state.value.progress > 0f ||
                video.state.value.file != null || video.state.value.error != null
        }
        assertEquals("no error", null, video.state.value.error)
        video.cancel()
        waitFor("the video stopped", seconds = 60) { !video.state.value.running }
        assertEquals("no error once stopped", null, video.state.value.error)
        NativeEngine.sink = null
    }
}
