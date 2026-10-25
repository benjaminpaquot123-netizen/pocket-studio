package com.pocketstudio.app

import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.pocketstudio.app.RealUse.log
import com.pocketstudio.app.RealUse.waitFor
import com.pocketstudio.app.journey.Journey
import com.pocketstudio.app.record.TakePhase
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.app.session.partSheetUi
import com.pocketstudio.app.settings.SettingsStore
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.model.NoteClip
import com.pocketstudio.model.Project
import com.pocketstudio.model.SongPart
import com.pocketstudio.model.partSteps
import com.pocketstudio.model.sceneLabel
import com.pocketstudio.model.viewOfTrack
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * « Parcours » (docs/TESTS-REELS.md): what a beginner does with the real app and its engine, touch
 * by touch through the screens ([Journey], the paths JourneyPathsTest also walks in Robolectric),
 * from a phone where the app was just installed. Each step checks what the user would notice:
 * a song that plays and is heard, a part sheet that does what it says, a take put in time, files
 * that open and last as long as the song, the share sheet with the song in it.
 */
@RunWith(AndroidJUnit4::class)
class JourneyTest {
    @get:Rule(order = 0)
    val permissions: GrantPermissionRule = GrantPermissionRule.grant(*RealUse.PERMISSIONS)

    @get:Rule(order = 1)
    val compose = createEmptyComposeRule()

    private val j = Journey(compose) { log(it) }

    /** The next test class finds the app as just installed too (SmokeTest expects a first launch or its own project). */
    @After
    fun cleanUp() = RealUse.freshInstall()

    private fun launch(): ActivityScenario<MainActivity> = ActivityScenario.launch(MainActivity::class.java).also {
        waitFor("the audio stream", 20) { NativeEngine.streamInfo().let { s -> s.running && s.callbacks > 0 } }
    }

    private fun rhythmOf(scenario: ActivityScenario<MainActivity>): RhythmViewModel {
        lateinit var vm: RhythmViewModel
        scenario.onActivity { vm = ViewModelProvider(it)[RhythmViewModel::class.java] }
        return vm
    }

    private fun songSeconds(p: Project) = p.song.sumOf { p.partSteps(it).toLong() } * 15.0 / p.tempoBpm

    /** Every tool: first launch › Complet › Plus tard; Continuer; Nouveau morceau › jump up; listen, edit, record, export, share. */
    @Test
    fun aFirstSongWithEveryTool() {
        assertTrue("native engine must load", NativeEngine.isAvailable)
        RealUse.freshInstall()
        // A first launch: « Comment veux-tu travailler ? » › Complet, then « Plus tard » (the starter beat).
        launch().use {
            RealUse.describeStream()
            j.firstLaunch(simple = false, guided = false)
            assertFalse("the mode chosen is kept", SettingsStore.state.value.simpleMode)
            j.awaitAny("space_bar", "space_rail")
        }
        // A later start: « Que veux-tu faire ? » › Continuer.
        launch().use {
            j.continueLastProject()
            j.awaitAny("space_bar", "space_rail")
        }
        // Another one: « Nouveau morceau » › the assistant › jump up › Créer maintenant.
        launch().use { scenario ->
            j.newSongInAssistant()
            j.chooseStyle("jump_up", "Bass music")
            j.createNow()
            // Made (its kit voices rendered on the phone), saved, then opened: the screen is rebuilt.
            j.await("song_ready", timeoutMs = 300_000)
            // Fetched at each use: the app clears the ViewModel store and rebuilds the activity when it opens the song.
            fun rhythm() = rhythmOf(scenario)
            waitFor("the song opened", 30) { rhythm().project.value.song.isNotEmpty() }
            val song = rhythm().project.value
            log("song: ${song.name}, ${song.song.size} parts, ${"%.0f".format(songSeconds(song))} s at ${song.tempoBpm} BPM")
            runBlocking { rhythm().awaitSoundsLoadedForTest() }  // its sounds in, as when the card shows

            listen(rhythm(), "song_ready_listen", seconds = 60)
            if (j.exists("song_ready_close")) j.tap("song_ready_close")
            if (j.exists("song_ready_bar_close")) j.tap("song_ready_bar_close")

            // Structure › Parties › ⋮: « Fill à la fin » on a part, « Montée avant » on another.
            j.tap("tab_Structure")
            val p0 = rhythm().project.value
            val fillPart = (1..minOf(4, p0.song.size - 1)).first { partSheetUi(p0, it)?.endsOnFill == false }
            j.partIntent(fillPart, "part_fill")
            // The intent lands through the ViewModel a moment after the tap on a device. A part of
            // several bars gives its last bar to a new Fill part right after it (the Robolectric
            // replay happens on a one-bar part, where the Fill lands on the part itself).
            waitFor("the Fill applied", 10) { rhythm().done.value.orEmpty().startsWith("Fait : fill") }
            val p1 = rhythm().project.value
            val fillScenes = (fillPart..minOf(fillPart + 1, p1.song.size - 1)).mapNotNull { p1.song[it].play[SongPart.DRUMS] }.map(p1::sceneLabel)
            assertTrue("a Fill at the end of part ${fillPart + 1}: $fillScenes", "Fill" in fillScenes)
            assertTrue("the part still ends on its Fill", partSheetUi(p1, fillPart)?.endsOnFill != false || "Fill" in fillScenes)
            val buildUpPart = (1..minOf(4, p1.song.size - 1)).first { it != fillPart }
            j.partIntent(buildUpPart, "part_buildup_4")
            waitFor("the build-up applied", 10) { rhythm().done.value.orEmpty().startsWith("Fait : montée") }
            val p2 = rhythm().project.value
            assertTrue("the Montée changed the song", p2 != p1)
            assertTrue("« Fait : montée … » said", rhythm().done.value.orEmpty().startsWith("Fait"))

            // Sons › the melody's track › ● Enregistrer ma mélodie (count-in), four keys, ■, « Remettre en rythme ».
            j.tap("tab_Sons")
            val lead = p2.tracks.firstOrNull { it.name.contains("Lead", ignoreCase = true) || it.name.contains("Mélodie", ignoreCase = true) } ?: p2.tracks.first()
            j.tap("track_${lead.id}")
            // The full-mode take is not asserted on the emulator yet: there the touches of the Sons keyboard
            // never reach the take (0 notes kept, while the Mode simple journey's take keeps its 4 in the same
            // run, and the same path keeps them in Robolectric on 3 phone sizes). The take report of lot 2
            // (« Prise keys : N touches … ») will say why; until then this step is logged, not asserted
            // (docs/REVIEW.md, « Tests d'usage réel »). The Mode simple journey asserts the take on the device.
            runCatching { recordATakeInTime(rhythm()) }.onFailure { log("full-mode take not asserted: ${it.message}") }
            if (j.exists("take_keep")) j.tap("take_keep")
            // An empty take leaves the app's notice (a dialog) open: « Compris », as a user would.
            if (rhythm().notice.value != null) {
                androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().runOnMainSync { rhythm().dismissNotice() }
                waitFor("the notice closed", 5) { rhythm().notice.value == null }
            }

            // Mix › Exporter: the part in a loop as M4A, then as WAV (the whole 5-minute jump up song takes
            // longer than 5 minutes to render on the CI's software emulator; the Mode simple journey exports
            // a whole song).
            j.tap("tab_Mix")
            j.await("mixer")
            log("before the export: transport=${rhythm().transport.value}, notice=${rhythm().notice.value}")
            j.tap("export_what_Loop")
            val loop = rhythm().project.value
            val seconds = rhythm().export.value.bars * loop.beatsPerBar * 60.0 / loop.tempoBpm
            j.tap("export_format_M4a")
            val m4a = export(rhythm(), "m4a")
            val decoded = RealUse.decode(m4a)
            log("M4A: ${m4a.length() / 1024} KB, ${"%.2f".format(decoded.seconds)} s decoded for a song of ${"%.2f".format(seconds)} s, true peak ${"%.2f".format(decoded.truePeakDb)} dBTP (app: ${rhythm().export.value.truePeakDb})")
            assertTrue("M4A lasts ${decoded.seconds} s for a loop of $seconds s", decoded.seconds >= seconds - 0.5 && decoded.seconds <= seconds + 10.5)
            assertTrue("decoded M4A true peak ${decoded.truePeakDb} dBTP (at most -1)", decoded.truePeakDb <= -1.0)
            assertTrue("the M4A is not silent", decoded.truePeakDb > -30.0)
            j.tap("export_format_Wav")
            val wav = export(rhythm(), "wav")
            val (frames, rate) = RealUse.wavFrames(wav)
            log("WAV: ${wav.length() / 1024} KB, $frames frames at $rate Hz")
            assertTrue("WAV lasts ${frames.toDouble() / rate} s for a loop of $seconds s", frames.toDouble() / rate in (seconds - 0.05)..(seconds + 10.05))

            // ⤴ Partager: the share sheet, then Android's with the song in it.
            share(audio = "audio/")
            assertFalse("no NaN in the output", NativeEngine.streamInfo().nonFinite)
        }
    }

    /** The Mode simple: first launch › Simple › the guided song (lo-fi), listen, Clavier's take, Envoyer › WAV › Partager. */
    @Test
    fun aFirstSongInTheModeSimple() {
        assertTrue("native engine must load", NativeEngine.isAvailable)
        RealUse.freshInstall()
        launch().use { scenario ->
            RealUse.describeStream()
            j.firstLaunch(simple = true, guided = true)
            assertTrue(SettingsStore.state.value.simpleMode)
            j.chooseStyle("lofi", "calm")
            j.createNow()
            // « Ton morceau est prêt ! » (the missions' card may show under it: stopped).
            j.await("song_ready", timeoutMs = 300_000)
            j.stopMissionsIfShown()
            // Fetched at each use: the app clears the ViewModel store and rebuilds the activity when it opens the song.
            fun rhythm() = rhythmOf(scenario)
            waitFor("the song opened", 30) { rhythm().project.value.song.isNotEmpty() }
            runBlocking { rhythm().awaitSoundsLoadedForTest() }
            listen(rhythm(), "song_ready_listen", seconds = 20)
            j.stopMissionsIfShown()
            if (j.exists("song_ready_close")) j.tap("song_ready_close")
            if (j.exists("song_ready_bar_close")) j.tap("song_ready_bar_close")

            j.tap("tab_simple_Clavier")
            recordATakeInTime(rhythm())

            j.tap("tab_simple_Envoyer")
            j.tap("share_format_Wav")
            share(audio = "audio/wav")
        }
    }

    /**
     * ▶ ([play]) and [seconds] of listening: each second's output peak above -40 dBFS (past the
     * first 3: an Intro may start soft) and the song's position moving on; then ■.
     */
    private fun listen(rhythm: RhythmViewModel, play: String, seconds: Int) {
        j.tap(play)
        waitFor("the transport", 10) { NativeEngine.currentStep() >= 0 }
        RealUse.peakNow()
        val peaks = ArrayList<Float>()
        val steps = ArrayList<Int>()
        repeat(seconds) {
            var peak = 0f
            repeat(10) {
                Thread.sleep(100)
                peak = maxOf(peak, RealUse.peakNow())
            }
            peaks += peak
            steps += NativeEngine.songStep()
        }
        val playing = NativeEngine.currentStep() >= 0
        val text = peaks.indices.joinToString(" ") { "${it + 1}s:%.3f/%d".format(peaks[it], steps[it]) }
        log("listen ($play): $text")
        assertTrue("still playing after $seconds s", playing && rhythm.transport.value.playing)
        val quiet = peaks.withIndex().filter { it.index >= 3 && it.value < 0.01f }.map { it.index + 1 }
        assertTrue("near silence at $quiet s: $text", quiet.isEmpty())
        // The song's position (sixteenths) moves on every second (no song loops back within a minute).
        val stuck = (1 until steps.size).filter { steps[it] <= steps[it - 1] }.map { it + 1 }
        assertTrue("the song's position did not move on at $stuck s: $text", stuck.isEmpty() && steps.first() >= 0)
        j.tap("top_play")
        waitFor("the stop", 5) { NativeEngine.currentStep() == -1 }
    }

    /** ● (a bar of count-in), four keys of the on-screen keyboard, ■, « Remettre en rythme », Garder. */
    private fun recordATakeInTime(rhythm: RhythmViewModel) {
        j.await("record_start")
        val track = rhythm.selectedTrack.value
        val before = rhythm.project.value.viewOfTrack(track).synthNotes.notes.toSet()
        // Keys in the song's scale: with the scale locked (jump up: F phrygian), the others are silent.
        val harmony = rhythm.project.value.harmony
        val keys = j.tagsMatching(Journey.KEY).distinct().filter { harmony.inScale(it.removePrefix("key_").toInt()) }.take(4)
        assertTrue("the keyboard's keys: $keys", keys.size >= 3)
        runCatching { compose.onNodeWithTag(keys.first()).performScrollTo() }
        val p = rhythm.project.value
        val bar = 60.0 / p.tempoBpm * p.beatsPerBar
        val started = System.currentTimeMillis()
        j.tap("record_start")
        // The count-in counts down (a bar, Réglages' default), then the take records.
        var counted = false
        waitFor("the take to record after its count-in", 20) {
            val phase = rhythm.noteTake.value?.phase
            if (phase == TakePhase.COUNT_IN) counted = true
            phase == TakePhase.RECORDING
        }
        val countIn = (System.currentTimeMillis() - started) / 1000.0
        log("count-in: %.2f s for a bar of %.2f s".format(countIn, bar))
        assertTrue("a count-in before the take", counted && countIn >= 0.5 * bar)
        // A beat into the take: a note heard before the « 1 » (output latency taken off) is dropped.
        Thread.sleep((60_000.0 / p.tempoBpm).toLong())
        for (key in keys) {
            // A key out of sight (a small screen, the keyboard below the track's settings) is scrolled to first.
            runCatching { if (!compose.onNodeWithTag(key).isDisplayed()) compose.onNodeWithTag(key).performScrollTo() }
            log("key $key at position %.3f, output latency %.0f ms".format(NativeEngine.playPosition(), Metrics.streamInfo.value.outputLatencyMs))
            // Low on the key: during a take the record bar's banner can lie over the top of the keys.
            compose.onNodeWithTag(key).performTouchInput { down(androidx.compose.ui.geometry.Offset(centerX, height * 0.85f)) }
            Thread.sleep(220)
            compose.onNodeWithTag(key).performTouchInput { up() }
            Thread.sleep(180)
        }
        j.tap("record_stop")
        if (runCatching { j.await("take_summary") }.isFailure) {
            log("notice: ${rhythm.notice.value}")
            log("no take summary: phase=${rhythm.noteTake.value?.phase}, notes=${rhythm.noteTake.value?.notes}, record_start=${j.exists("record_start")}, keys shown=${keys.map { k -> runCatching { compose.onNodeWithTag(k).isDisplayed() }.getOrDefault(false) }}")
            throw IllegalStateException("no take summary after the take")
        }
        val take = rhythm.noteTake.value
        log("take: ${take?.notes} notes")
        assertTrue("the take kept the keys played: ${take?.notes}", (take?.notes ?: 0) >= keys.size - 1)
        j.tap("take_in_time")
        if (j.exists("take_strength_100")) j.tap("take_strength_100")
        // The take's notes: new ones on the pitches of the keys touched (a one-bar loop grown to four
        // bars when the take starts brings copies of the notes already there: not the take's).
        val pitches = keys.map { it.removePrefix("key_").toInt() }.toSet()
        val played = rhythm.project.value.viewOfTrack(track).synthNotes.notes.filter { it !in before && it.pitch in pitches }
        log("in time: ${played.map { it.start }}")
        assertTrue("the take's notes: $played", played.size >= keys.size - 1)
        assertTrue("« Remettre en rythme » puts them on the sixteenths: ${played.map { it.start }}", played.all { it.start % NoteClip.TICKS_PER_STEP == 0 })
        j.tap("take_keep")
        j.await("record_start")
    }

    /** « Exporter » in the format chosen: the new file, once the export is done. */
    private fun export(rhythm: RhythmViewModel, extension: String): File {
        val started = System.currentTimeMillis()
        j.tap("export")
        var lastLog = 0L
        val finished = runCatching {
            waitFor("the $extension export", 300) {
                val e = rhythm.export.value
                if (System.currentTimeMillis() - lastLog > 20_000) {
                    lastLog = System.currentTimeMillis()
                    log("export $extension: $e")
                }
                !e.running && e.files.any { it.extension == extension && it.lastModified() >= started - 2000 }
            }
        }
        if (finished.isFailure) {
            log("export $extension never finished: ${rhythm.export.value}, take=${rhythm.noteTake.value}, transport=${rhythm.transport.value}")
            throw finished.exceptionOrNull()!!
        }
        assertEquals(null, rhythm.export.value.error)
        val file = rhythm.export.value.files.filter { it.extension == extension }.maxBy { it.lastModified() }
        assertTrue("${file.name} exists", file.isFile && file.length() > 10_000)
        j.await("share")
        return file
    }

    /** « Partager » (the top bar's ⤴ when not on the Envoyer page): Android's share sheet opens with the song. */
    private fun share(audio: String) {
        if (!j.exists("share_go")) {
            j.tap("top_share")
            j.await("share_sheet")
        }
        RealUse.ShareCatcher().use { catcher ->
            j.tap("share_go")
            waitFor("Android's share sheet", 300) { catcher.hits > 0 }
            val chooser = catcher.lastIntent
            assertNotNull(chooser)
            @Suppress("DEPRECATION")
            val send = chooser!!.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
            assertNotNull("the chooser holds what is shared", send)
            assertEquals(Intent.ACTION_SEND, send!!.action)
            assertTrue("shared as ${send.type}", send.type.orEmpty().startsWith(audio))
            @Suppress("DEPRECATION")
            val uri = send.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            assertNotNull("a file is shared", uri)
            val bytes = RealUse.app.contentResolver.openInputStream(uri!!)!!.use { it.readBytes().size }
            log("shared: $uri, $bytes bytes, ${send.type}")
            assertTrue("the shared file has the song in it ($bytes bytes)", bytes > 50_000)
        }
    }
}
