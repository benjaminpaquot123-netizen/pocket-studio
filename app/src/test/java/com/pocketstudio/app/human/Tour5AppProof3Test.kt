package com.pocketstudio.app.human

import android.app.Application
import android.net.Uri
import android.os.Looper
import android.os.SystemClock
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.text.AnnotatedString
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.export.MyExport
import com.pocketstudio.app.export.MyExports
import com.pocketstudio.app.projects.CurrentProject
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.app.ui.playPosition
import com.pocketstudio.engine.CommandSink
import com.pocketstudio.engine.ExportResult
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.model.Project
import com.pocketstudio.model.ProjectStore
import com.pocketstudio.model.SongPart
import com.pocketstudio.model.allScenes
import com.pocketstudio.model.instrumentTracks
import com.pocketstudio.model.sceneView
import com.pocketstudio.model.switchScene
import com.pocketstudio.model.track
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File
import java.time.Duration

/**
 * Lot « verdicts » (PLAN-RESTE 6.3): the proofs WITHOUT the bench (no HumanTester, no BenchEngine) of the lines of
 * verdicts.tsv judged « banc ». Each test runs the app's own code (its view model, its composables) against an engine
 * scripted here that only says what a phone's says (Engine.cpp: the song's position, the swap of parts on a bar line,
 * the song's end), and checks what the person saw go wrong on the bench does not happen:
 * - T4-17, T5-Fc-8: the bar counter stood still while the song played;
 * - T4-16, T5-Ab-14: « Jouer » played one loop, the clock stuck at 0:12 / 0:13 past the end;
 * - T5-Fb-73: a part picked while the song plays did not take over;
 * - T4-19, T5-Dc-4, T5-Ac-61, T5-Db-16: ✕ « Effacer » froze the rename window, or lost the field's cursor;
 * - T5-Cc-41b: « 1 KB » for an export (the size « Mes exports » says is the file's own);
 * - T5-TEL10: the keys of a take survive leaving the project while it records (recette tel-10, Benjamin ✅).
 * And T4-01 (« ■ Tout » of the Live view: the sound stopped, the bars went on): it stops the transport too (4A).
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = HumanTester.PHONE)
class Tour5AppProof3Test {
    /** PLAN-RESTE 4.5: the state inherited from the test before must be pristine (else: red, naming that test). */
    @get:Rule
    val pristine = BenchPristineRule()
    @get:Rule
    val knownDefects: TestRule = KnownDefectRule()
    @get:Rule
    val compose = androidx.compose.ui.test.junit4.createComposeRule()

    private val app: Application get() = ApplicationProvider.getApplicationContext()

    /**
     * A phone's engine as far as the view model reads it (Tour4AppProofTest's, with the song's own position): the play
     * position from ▶ (count-in first), the scene swap the app arms, made on the next bar line of the scene playing and
     * counted (Engine.cpp's onStep), the song's position set by ▶ and by a swap that restarts the song there, and the
     * song's end (the transport stops on that bar line, counted as a swap).
     */
    internal class SongEngine : CommandSink {
        private var next = 1L
        val posts = ArrayList<List<Number>>()
        private var since = -1L
        private var countIn = 0
        private var tempo = 120.0
        private var beats = 4
        private var songAt = -1  // the song's step at transport 0 (-1: outside the song)
        private var sceneStart = 0
        private var swapAt = -1
        private var swapFlags = 0
        private var swapSongStep = 0
        private var armedAt = 0
        var swaps = 0
            private set
        var stoppedBySongEnd = false
            private set

        private fun transport(): Double =
            if (since < 0) Double.NaN else (SystemClock.uptimeMillis() - since) * tempo / 15_000.0 - countIn * beats * 4.0

        private fun follow() {
            val t = transport()
            if (t.isNaN() || t < 0 || swapAt < 0) return
            val bar = (beats * 4).coerceAtLeast(1)
            val local = armedAt - sceneStart
            val at = maxOf(if (swapAt == 0) 1 else swapAt, local + 1).let { (it + bar - 1) / bar * bar }
            if (sceneStart + at > t.toInt()) return
            sceneStart += at
            swapAt = -1
            swaps++
            when (swapFlags) {
                2 -> { since = -1L; stoppedBySongEnd = true }
                1 -> songAt = swapSongStep - sceneStart
                3 -> songAt = -1
            }
        }

        @Synchronized override fun post(target: Long, type: Int, value: Float, a: Int, b: Int) {
            if (target != 0L) return
            follow()
            posts += listOf(type, value, a, b)
            when (type) {
                CMD_SET_TRANSPORT -> {
                    since = if (value > 0f) SystemClock.uptimeMillis() else -1L
                    countIn = if (value > 0f) a else 0
                    songAt = if (value > 0f) b else -1
                    sceneStart = 0
                    swapAt = -1
                }
                CMD_SET_TEMPO -> if (value > 0f) tempo = value.toDouble()
                CMD_BEATS_PER_BAR -> if (a > 0) beats = a
                CMD_ARM_SCENE_SWAP -> {
                    swapAt = if (a < 0) -1 else a
                    swapFlags = b
                    swapSongStep = value.toInt()
                    armedAt = transport().let { if (it.isNaN() || it < 0) 0 else it.toInt() }
                    if (a < 0 && b == 3) songAt = -1
                }
                // A stop armed for slots (Engine::launchSlots, « ■ Tout »): on the next multiple of its quantum, each slot
                // reported stopped (-1) to drainLaunchEvents. Only stops: what « ■ Tout » arms.
                CMD_ARM_LAUNCH -> if (value < 0f) {
                    val q = (b and 0x1FFFF).coerceAtLeast(1)
                    val now = transport().let { if (it.isNaN() || it < 0) 0 else it.toInt() }
                    stopMask = stopMask or a
                    stopAt = (now / q + 1) * q
                }
                CMD_CANCEL_LAUNCH -> stopMask = stopMask and a.inv()
            }
        }
        private var stopMask = 0
        private var stopAt = 0
        @Synchronized override fun drainLaunchEvents(out: IntArray): Int {
            follow()
            val t = transport()
            if (stopMask == 0 || t.isNaN() || t < stopAt) return 0
            var n = 0
            for (k in 0 until 31) if (stopMask and (1 shl k) != 0 && (n + 1) * 3 <= out.size) {
                out[n * 3] = k; out[n * 3 + 1] = -1; out[n * 3 + 2] = stopAt
                n++
            }
            stopMask = 0
            return n
        }
        @Synchronized override fun playPosition(): Double {
            follow()
            val t = transport()
            return if (t.isNaN()) -1.0 else if (t < 0) t else t - sceneStart
        }
        @Synchronized override fun transportPosition(): Double { follow(); return transport().let { if (it.isNaN() || it < 0) -1.0 else it } }
        override fun transportStep(): Int = transportPosition().let { if (it < 0) -1 else it.toInt() }
        @Synchronized override fun absoluteStep(): Int { follow(); return transport().let { if (it.isNaN() || it < 0) -1 else it.toInt() - sceneStart } }
        @Synchronized override fun songStep(): Int {
            follow()
            val t = transport()
            return if (t.isNaN() || t < 0 || songAt < 0) -1 else songAt + t.toInt()
        }
        @Synchronized override fun sceneSwaps(): Int { follow(); return swaps }
        override fun scheduleFrom(target: Long, frame: Long) = Unit
        override fun install(target: Long, kind: CommandSink.Install, index: Int, sample: Long) = Unit
        @Synchronized override fun sampleCreate(data: FloatArray, channels: Int, sampleRate: Int): Long = next++
        override fun sampleFrames(handle: Long): Long = 48_000L
        override fun sampleRelease(handle: Long) = Unit
        @Synchronized override fun exportCreate(): Long = next++
        override fun exportRender(target: Long, path: String, frames: Long, tailFrames: Long, fadeOutFrames: Long, autoTail: Boolean, floatWav: Boolean, sampleRate: Int, bits: Int) =
            ExportResult.Cancelled
        override fun exportRenderFloat(target: Long, frames: Long, tailFrames: Long): FloatArray = FloatArray(0)
        override fun exportDestroy(target: Long) = Unit
        @Synchronized fun of(type: Int): List<List<Number>> = posts.filter { it[0] == type }
    }

    private val opened = ArrayList<RhythmViewModel>()
    private val stores = ArrayList<androidx.lifecycle.ViewModelStore>()

    @After
    fun clear() {
        stores.forEach { it.clear() }
        com.pocketstudio.app.rhythm.ClosingSaves.awaitIdleBlocking(15_000)
        NativeEngine.sink = null
    }

    private fun idle(ms: Long = 20) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))

    /** The app's view model on [p], made and cleared as a screen's (its store cleared when the screen goes). */
    private fun open(
        p: Project, id: String = "verdicts-${System.nanoTime()}", save: Boolean = true,
        ready: (Project) -> Boolean = { it.song.size == p.song.size && it.instrumentTracks.size == p.instrumentTracks.size },
    ): RhythmViewModel {
        if (save) ProjectStore(File(app.filesDir, "projects")).save(id, p)
        CurrentProject.set(app, id)
        val store = androidx.lifecycle.ViewModelStore().also { stores += it }
        val factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T = RhythmViewModel(app) as T
        }
        val vm = androidx.lifecycle.ViewModelProvider(store, factory)[RhythmViewModel::class.java]
        val until = System.currentTimeMillis() + 10_000
        while (!ready(vm.project.value)) {
            assertTrue("the project never opened: song ${vm.project.value.song.size}/${p.song.size}, tracks ${vm.project.value.instrumentTracks.size}", System.currentTimeMillis() < until)
            idle(); Thread.sleep(5)
        }
        opened += vm
        return vm
    }

    /** A song of two parts of one bar each, on two scenes (a kick on each: an empty part is dropped as it is read). */
    private fun twoParts(): Project = Project().let { it.copy(drums = it.drums.toggle(0, 0)) }.switchScene(1)
        .let { it.copy(drums = it.drums.toggle(0, 4)) }.switchScene(0)
        .copy(song = listOf(SongPart(0), SongPart(1)), tempoBpm = 120.0)

    private fun startPlaying(vm: RhythmViewModel) {
        vm.togglePlay()
        val until = System.currentTimeMillis() + 10_000
        while (!vm.transport.value.playing && System.currentTimeMillis() < until) { idle(); Thread.sleep(5) }
        assertTrue("▶ plays", vm.transport.value.playing)
    }

    /**
     * T4-17, T5-Fc-8 (the counter « mesure N/T » stood still while the song played) and T4-16, T5-Ab-14 (« Jouer » played
     * one loop; « 0:12 / 0:13 » past the end): ▶ plays the song, its first part then its second, and stops at its end;
     * the top bar's counter, read as the screen reads it (playPosition of the app's playBar), says bar 1/2, then 2/2,
     * never past 2. The bench's own engine had no song position (fixed in 3.2 and R.1, its guards BenchPlayThenTabsTest
     * and BenchRecetteTest); the app reads the engine's, as here.
     */
    @Test
    fun theSongPlaysToItsEndAndItsCounterFollowsTheEngine() {
        val engine = SongEngine()
        NativeEngine.sink = engine
        val vm = open(twoParts())
        val poll = vm.viewModelScope.launch { vm.pollPosition() }
        try {
            startPlaying(vm)
            val bars = ArrayList<String>()
            val parts = ArrayList<Int>()
            var waited = 0L
            while (vm.transport.value.playing && waited < 12_000) {
                idle(50); waited += 50
                // Stopped, the strip says where ▶ starts again (bar 1): only the counter while it plays is read.
                if (!vm.transport.value.playing) break
                val p = vm.project.value
                playPosition(p, vm.transport.value.playing, vm.sessionUi.value.songIndex, vm.playBar.value, vm.playFrom.value, vm.loopPart.value)
                    ?.let { pos -> "${pos.bar}/${pos.bars}".takeIf { bars.lastOrNull() != it }?.let(bars::add) }
                vm.sessionUi.value.songIndex.takeIf { it >= 0 && parts.lastOrNull() != it }?.let(parts::add)
            }
            val facts = "counter $bars, parts $parts, ${waited} ms, swaps ${engine.swaps}"
            assertEquals("the song's two parts played, one after the other: $facts", listOf(0, 1), parts)
            assertEquals("the counter went from bar 1 to bar 2 of 2, never past it: $facts", listOf("1/2", "2/2"), bars)
            assertFalse("the song stopped at its end (one bar each at 120 BPM: 4 s): $facts", vm.transport.value.playing)
            assertTrue("the engine stopped it on the song's last bar line (4 s of song, a count-in at most before): $facts", engine.stoppedBySongEnd && waited in 3_500..8_500)
        } finally {
            poll.cancel()
        }
    }

    /**
     * T5-Fb-73 (tel-13, Benjamin ✅ on a phone and a tablet): the song playing, the top bar's part menu › B: the app arms
     * B on the engine, which swaps it in on the next bar line; the screens follow (B shown and heard). The bench's engine
     * never swapped a scene (fixed, its guard BenchRecetteTest#proPartPickedWhilePlayingTakesOver).
     */
    @Test
    fun aPartPickedWhileTheSongPlaysTakesOverOnTheNextBar() {
        val engine = SongEngine()
        NativeEngine.sink = engine
        val vm = open(twoParts().copy(song = listOf(SongPart(0), SongPart(0), SongPart(1))))
        startPlaying(vm)
        idle(500)
        assertEquals("A plays", 0, vm.project.value.scene)
        val before = engine.swaps
        vm.selectScene(1)  // « scene_pick_1 » of the top bar's menu (TopBar: actions.selectScene)
        val armed = engine.of(CMD_ARM_SCENE_SWAP).lastOrNull()
        assertTrue("B armed on the engine: $armed", armed != null && armed[2].toInt() >= 0)
        var waited = 0L
        while ((engine.swaps == before || vm.project.value.scene != 1) && waited < 3_000) { idle(50); waited += 50 }
        assertTrue("the engine swapped B in on the bar line (${waited} ms, a bar is 2 s)", engine.swaps > before && waited <= 2_100)
        assertEquals("B is the part shown", 1, vm.project.value.scene)
        assertTrue("still playing", vm.transport.value.playing)
    }

    /**
     * T4-19 (round 4, « Renommer » › ✕ « Effacer »: 240 s, the rename window frozen on the bench), T5-Dc-4, T5-Ac-61,
     * T5-Db-16 (the field lost its cursor after ✕; tel-01, Benjamin ✅: the letters go in). The app's own name field in
     * the app's rename window (ScaledAlertDialog), without the bench and without waiting for Compose's rest (Robolectric
     * never lets a dialog with a field rest, Zone3DialogDiagTest): the field touched, ✕ empties it at once, the field keeps
     * the cursor, the name typed goes in. The same ✕ in the field of a kept sound (SelectAllTextField, « Nom du son »).
     */
    @Test
    fun theClearButtonEmptiesTheFieldAndKeepsItsCursor() {
        val open = androidx.compose.runtime.mutableStateOf(false)
        val which = androidx.compose.runtime.mutableStateOf(0)
        compose.setContent {
            com.pocketstudio.ui.PocketStudioTheme {
                var name by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("Mon premier rythme") }
                if (open.value) com.pocketstudio.ui.ScaledAlertDialog(
                    onDismissRequest = {},
                    title = { androidx.compose.material3.Text("Renommer") },
                    text = {
                        if (which.value == 0) com.pocketstudio.app.ui.NameField(name, { name = it }, max = 40, label = "Nom du projet",
                            modifier = androidx.compose.ui.Modifier.testTag("field"))
                        else com.pocketstudio.app.ui.SelectAllTextField(name, { name = it }, modifier = androidx.compose.ui.Modifier.testTag("field"))
                    },
                    confirmButton = { androidx.compose.material3.Text("[$name]") },
                )
            }
        }
        compose.runWithoutImplicitWait {
            fun frames() = repeat(6) { compose.mainClock.advanceTimeByFrame(); shadowOf(Looper.getMainLooper()).idle() }
            fun node(tag: String) = compose.onAllNodesWithTag(tag).fetchSemanticsNodes(atLeastOneRootRequired = false).firstOrNull() ?: error("$tag absent")
            fun text() = node("field").config.getOrNull(SemanticsProperties.EditableText)?.text
            fun focused() = node("field").config.getOrNull(SemanticsProperties.Focused) == true
            for (kind in listOf(0, 1)) {
                compose.runOnUiThread { which.value = kind; open.value = true }
                frames()
                compose.runOnUiThread { node("field").config[SemanticsActions.RequestFocus].action!!() }
                frames()
                assertTrue("field $kind touched: it holds the cursor", focused())
                val at = System.currentTimeMillis()
                compose.runOnUiThread { node("name_clear").config[SemanticsActions.OnClick].action!!() }
                frames()
                assertTrue("✕ answered in ${System.currentTimeMillis() - at} ms (the bench waited 240 s)", System.currentTimeMillis() - at < 5_000)
                assertEquals("field $kind emptied by ✕", "", text())
                assertTrue("field $kind keeps the cursor after ✕ (tel-01)", focused())
                compose.runOnUiThread { node("field").config[SemanticsActions.InsertTextAtCursor].action!!(AnnotatedString("Morceau de Max v3")) }
                frames()
                assertEquals("field $kind takes the name typed after ✕", "Morceau de Max v3", text())
                compose.runOnUiThread { open.value = false }
                frames()
            }
        }
    }

    /**
     * T5-Cc-41b (round 5, « 1 KB » for an export; tel-04, Benjamin ✅: 42,05 Mo for 2 min 26 in 24 bits stereo at
     * 48 kHz): « Mes exports » says the size of the file it lists, the bytes the system gives, nothing of its own. A WAV of
     * 2 min 26 (146 s × 48 000 frames × 6 bytes + its 44 bytes of header) is said 40,1 Mo; 1 000 bytes, 1 Ko. So the
     * « 1 KB » a seat read is the size of the file the bench's stand-in encoder wrote (BenchEngineTest#exportProfile).
     */
    @Test
    fun myExportsSaysTheFilesOwnSize() {
        val lang = com.pocketstudio.model.i18n.I18n.lang
        com.pocketstudio.model.i18n.I18n.lang = com.pocketstudio.model.i18n.Lang.FR
        try {
        fun said(bytes: Long) = MyExports.detail(MyExport("Mon morceau.wav", Uri.parse("content://x/1"), bytes, 1_762_000_000L, "audio/wav"))
        val phone = 146L * 48_000 * 6 + 44
        assertTrue("2 min 26 of WAV 24 bits: « ${said(phone)} »", said(phone).endsWith("40,1 Mo"))
        assertTrue("1 000 bytes: « ${said(1_000)} »", said(1_000).endsWith("1 Ko"))
        assertTrue("768 000 bytes (the bench's M4A of 4 s): « ${said(768_000)} »", said(768_000).endsWith("750 Ko"))
        } finally {
            com.pocketstudio.model.i18n.I18n.lang = lang
        }
    }

    /**
     * T5-TEL10 (recette tel-10, Benjamin ✅: « elles sont toujours toutes présentes dans notes »): « Enregistrer ma mélodie »,
     * three keys while it records, then the project's screen left while the take still runs (Retour, then Pad Live: the
     * project's view model cleared), the project opened again: the three notes are there.
     */
    @Test
    fun theKeysOfATakeLeftRunningAreThereWhenTheProjectOpensAgain() {
        NativeEngine.sink = SongEngine()
        val id = "verdicts-tel10-${System.nanoTime()}"
        val vm = open(Project(), id)
        vm.recordMelodyTake()
        val until = SystemClock.uptimeMillis() + 10_000
        while (NativeEngine.playPosition() < 0.5) { assertTrue("the count-in never ended", SystemClock.uptimeMillis() < until); idle(50) }
        idle(100)
        for (key in listOf(62, 65, 69)) {
            vm.synthNoteOn(key); idle(150); vm.synthNoteOff(key); idle(250)
        }
        assertTrue("still recording when the screen goes", vm.recording.value)
        val taken = notes(vm.project.value)
        assertTrue("the take wrote its notes as they were played: $taken", taken >= 3)
        // Retour: the activity stops (AppRoot's ON_STOP: rhythmVm.flush()), the take still running; the main thread goes on
        // (the save is written off it, the looper pumped meanwhile). Then Pad Live and back: the project read again from
        // the disk, here by a new view model (its first one cleared, as when the activity goes).
        vm.flush()
        val store = ProjectStore(File(app.filesDir, "projects"))
        var onDisk = 0
        var waitedSave = 0L
        while (waitedSave < 20_000) {
            idle(50); Thread.sleep(10); waitedSave += 60
            onDisk = store.load(id)?.let(::notes) ?: 0
            if (onDisk >= taken) break
        }
        assertEquals("the take's notes written on the disk when the screen went (${waitedSave} ms)", taken, onDisk)
        stores.removeAt(stores.size - 1).clear()
        repeat(50) { idle(20); Thread.sleep(5) }  // the save of onCleared (ClosingSaves, off the main thread) let start
        com.pocketstudio.app.rhythm.ClosingSaves.awaitIdleBlocking(15_000)
        NativeEngine.sink = SongEngine()
        // Opened again: read from the disk (« Ma mélodie » there), the notes waited for 10 s at most.
        val again = open(Project(), id, save = false, ready = { it.instrumentTracks.size > 1 && notes(it) >= taken })
        assertEquals("the take's notes are still in the project opened again", taken, notes(again.project.value))
    }

    /** The notes of « Ma mélodie » in every part. */
    private fun notes(p: Project): Int {
        val id = com.pocketstudio.model.SimpleMelody.trackOf(p) ?: return 0
        return p.allScenes.indices.sumOf { s -> p.sceneView(s).track(id).notes.notes.size }
    }

    /**
     * T4-01 (recette tel-12, Benjamin ✅ « défaut partiel »: « ■ Tout coupe le son mais mesures et temps continuent ») and
     * T5-Ed-11 for Pad Live (tel-08 ❌, the same: « Tout couper coupe l'audio, le transport continue »): the owner's trials
     * say « ■ Tout » should stop the song as well. The app keeps the transport on (stopLiveAll: every block armed off, the
     * clock running; Tour4AppProofTest#liveStopAllAndATracksStopGoToTheEngineAndTheGridFollows). Plan of the audits, step 4,
     * n° 7 (Benjamin's 4A): the blocks stop on their launch line, and the transport with them.
     */
    @Test
    fun liveStopAllStopsTheSong() {
        val engine = SongEngine()
        NativeEngine.sink = engine
        val vm = open(twoParts())
        startPlaying(vm)
        idle(300)
        vm.stopLiveAll()
        idle(2_500)  // past the launch line: every block stopped
        assertFalse("« ■ Tout » of the Live view leaves the transport running (the bars and the time go on)", vm.transport.value.playing)
    }

    internal companion object {
        const val CMD_SET_TRANSPORT = 5
        const val CMD_SET_TEMPO = 6
        const val CMD_ARM_SCENE_SWAP = 40
        const val CMD_BEATS_PER_BAR = 57
        const val CMD_ARM_LAUNCH = 71
        const val CMD_CANCEL_LAUNCH = 72
    }
}
