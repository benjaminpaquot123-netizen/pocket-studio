package com.pocketstudio.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pocketstudio.engine.NativeEngine
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.pocketstudio.app.latency.LatencyUiState
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.model.ProjectStore
import java.io.File
import com.pocketstudio.app.latency.LatencyViewModel
import com.pocketstudio.engine.LatencyStart
import com.pocketstudio.engine.LatencyState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Emulator smoke test run by CI: the app starts, plays, and every space opens without crashing. */
@RunWith(AndroidJUnit4::class)
class SmokeTest {
    /** A French phone (the emulator's is American: a first start would take English), before the app starts. */
    @get:Rule(order = 0)
    val french = org.junit.rules.TestRule { base, _ ->
        object : org.junit.runners.model.Statement() {
            override fun evaluate() {
                val app = InstrumentationRegistry.getInstrumentation().targetContext
                val prefs = app.getSharedPreferences("settings", 0)
                if (!prefs.contains("language")) prefs.edit().putString("language", "fr").commit()
                base.evaluate()
            }
        }
    }

    @get:Rule(order = 1)
    val mic: GrantPermissionRule = GrantPermissionRule.grant(android.Manifest.permission.RECORD_AUDIO)

    @get:Rule(order = 2)
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun appStartsPlaysAndNavigates() {
        assertTrue("native engine must load", NativeEngine.isAvailable)
        compose.waitUntil(10_000) { NativeEngine.streamInfo().let { it.running && it.callbacks > 0 } }
        // The opening (BrandIntro) first: it covers the app for about a second.
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("intro").fetchSemanticsNodes().isEmpty() }
        // First launch: the welcome choices open; "later" keeps the starter beat (then Pro, below).
        compose.waitForIdle()
        if (compose.onAllNodesWithTag("welcome_later").fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithTag("welcome_later").performScrollTo().performClick()
        }
        compose.waitForIdle()
        // Any later start: « Que veux-tu faire ? », where « Continuer » opens the last project (one touch).
        if (compose.onAllNodesWithTag("launch_continue").fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithTag("launch_continue").performScrollTo().performClick()
        }
        compose.waitForIdle()
        // The project's own mode (owner, 2026-10-29): Pro, through the top bar's chip.
        if (compose.onAllNodesWithTag("tab_simple_Morceau").fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithTag("top_mode").performClick()
            compose.onNodeWithTag("mode_card_pro").performClick()
            compose.waitForIdle()
        }
        assertTrue("no « Que veux-tu faire ? » left over the project", compose.onAllNodesWithTag("launch").fetchSemanticsNodes().isEmpty())

        // Rhythm screen: the starter groove plays and makes sound. « Continuer » reopens the last space
        // used (another test may have left Structure open): go to Sons › Rythme explicitly.
        if (compose.onAllNodesWithTag("rhythm").fetchSemanticsNodes().isEmpty()) {
            compose.onNodeWithTag("tab_Sons").performClick()
            compose.waitForIdle()
            if (compose.onAllNodesWithTag("rhythm").fetchSemanticsNodes().isEmpty()) {
                compose.onNodeWithTag("sons_Rythme").performClick()
                compose.waitForIdle()
            }
        }
        compose.onNodeWithTag("rhythm").assertIsDisplayed()
        compose.onNodeWithTag("top_play").performClick()
        compose.waitUntil(5_000) { NativeEngine.currentStep() >= 0 }
        var heard = false
        compose.waitUntil(5_000) {
            heard = heard || Metrics.streamInfo.value.peak > 0.1f
            heard
        }
        assertTrue("drums must sound", heard)
        compose.onNodeWithTag("top_play").performClick()
        compose.waitUntil(2_000) { NativeEngine.currentStep() == -1 }

        // A live pad fires its lane.
        val before = IntArray(NativeEngine.LANES).also { NativeEngine.laneTriggers(it) }
        compose.onNodeWithTag("pad_4").performScrollTo().performClick()
        compose.waitUntil(2_000) {
            IntArray(NativeEngine.LANES).also { NativeEngine.laneTriggers(it) }[4] > before[4]
        }

        // An edit survives the app going to the background (autosave flush on stop).
        compose.onNodeWithTag("step_3").performScrollTo().performClick()  // pad 4 (clap) is selected now
        compose.waitForIdle()
        compose.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
        val store = ProjectStore(File(InstrumentationRegistry.getInstrumentation().targetContext.filesDir, "projects"))
        // The flush is asynchronous and the emulator's disk is slow: whichever project is open (the
        // starter's id can differ from PROJECT_ID), its clap must carry the hit within 15 s.
        fun savedHit(): Boolean = (store.list() + RhythmViewModel.PROJECT_ID).distinct().any { id ->
            store.load(id)?.drums?.lanes?.get(4)?.hits?.containsKey(3) == true
        }
        var saved = false
        for (i in 0 until 150) {
            saved = savedHit()
            if (saved) break
            Thread.sleep(100)
        }
        assertTrue("edit must be saved when the app goes to the background (projects: ${store.list()})", saved)
        compose.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
        compose.waitUntil(10_000) { NativeEngine.streamInfo().let { it.running && it.callbacks > 0 } }

        // The microphone path delivers samples to the latency meter (the emulator mic is silent,
        // so only the plumbing is checked here, not the measured value).
        assertEquals(LatencyStart.Ok, NativeEngine.startLatencyTest())
        compose.waitUntil(6_000) { NativeEngine.latencyState() == LatencyState.Done }
        val result = NativeEngine.latencyResult()
        assertTrue("microphone delivered ${result.inputFrames} frames (${NativeEngine.inputDescription()})", result.inputFrames > 0)

        // Same path as the Latence screen (ViewModel -> JNI -> engine). The emulator mic is silent,
        // so the run may fail, but never because the meter did not run (NoInput) or was cut.
        val vm = LatencyViewModel()
        InstrumentationRegistry.getInstrumentation().runOnMainSync { vm.measure(wanted = 1, maxAttempts = 2) }
        compose.waitUntil(30_000) { vm.state.value.let { it is LatencyUiState.Measured || it is LatencyUiState.Failed } }
        val diagnostic = when (val s = vm.state.value) {
            is LatencyUiState.Measured -> s.diagnostic
            is LatencyUiState.Failed -> s.diagnostic
            else -> ""
        }
        assertTrue("meter must run: $diagnostic", !diagnostic.contains("NoInput") && !diagnostic.contains("Interrupted"))

        // Plus (phase 36: a list, each page with « ‹ Plus »): test tone, then latency page; then the checklist.
        compose.onNodeWithTag("tab_Plus").performClick()
        compose.onNodeWithTag("tool_Son").performScrollTo().performClick()
        compose.onNodeWithTag("play").performClick()
        compose.onNodeWithText("Arrêter").assertIsDisplayed()
        compose.onNodeWithTag("play").performClick()
        compose.onNodeWithTag("plus_back").performClick()
        compose.onNodeWithTag("tool_Latence").performScrollTo().performClick()
        compose.onNodeWithTag("latency").assertIsDisplayed()
        compose.onNodeWithTag("plus_back").performClick()

        compose.onNodeWithTag("tool_Recette").performScrollTo().performClick()
        compose.onNodeWithTag("recette").assertIsDisplayed()

        compose.onNodeWithTag("tab_Mix").performClick()
        compose.onNodeWithTag("mixer").assertIsDisplayed()
        compose.onNodeWithTag("mute_0").performClick()
        compose.onNodeWithTag("mute_0").performClick()

        // Export: 4 bars rendered offline into a real 24-bit WAV with sound in it. The part in a
        // loop, chosen explicitly: a project with a song exports the whole song by default, and a
        // 3-minute render then takes longer than this test waits on the emulator.
        compose.onNodeWithTag("export_what_Loop").performScrollTo().performClick()
        compose.onNodeWithTag("export").performScrollTo().performClick()
        compose.waitUntil(60_000) { compose.onAllNodesWithTag("share").fetchSemanticsNodes().isNotEmpty() }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val wav = File(context.cacheDir, "exports").listFiles()!!.maxBy { it.lastModified() }
        val bytes = wav.readBytes()
        assertEquals("RIFF", String(bytes, 0, 4))
        assertEquals(24, bytes[34].toInt())
        val loudest = (44 until bytes.size - 2 step 3).maxOf { i ->
            val v = (bytes[i].toInt() and 0xFF) or ((bytes[i + 1].toInt() and 0xFF) shl 8) or (bytes[i + 2].toInt() shl 16)
            kotlin.math.abs(v)
        }
        assertTrue("export has sound: $loudest", loudest > 800_000)

        // The same loop as M4A (Android's AAC encoder in an MP4 container).
        compose.onNodeWithTag("export_format_M4a").performScrollTo().performClick()
        compose.onNodeWithTag("export").performScrollTo().performClick()
        compose.waitUntil(60_000) {
            File(context.cacheDir, "exports").listFiles()!!.any { it.extension == "m4a" && it.length() > 10_000 } &&
                compose.onAllNodesWithTag("share").fetchSemanticsNodes().isNotEmpty()
        }
        val m4a = File(context.cacheDir, "exports").listFiles()!!.filter { it.extension == "m4a" }.maxBy { it.lastModified() }
        assertEquals("ftyp", String(m4a.readBytes(), 4, 4))
        compose.onNodeWithTag("export_format_Wav").performScrollTo().performClick()

        // Import: a 44.1 kHz 16-bit WAV goes through Android's decoder and the resampler onto pad 9.
        val context2 = InstrumentationRegistry.getInstrumentation().targetContext
        val wavFile = File(context2.cacheDir, "test-hit.wav").apply { writeBytes(testWav()) }
        val rhythm = androidx.lifecycle.ViewModelProvider(compose.activity)[RhythmViewModel::class.java]
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            rhythm.hitPad(9)
            rhythm.importSample(android.net.Uri.fromFile(wavFile))
        }
        compose.waitUntil(20_000) { rhythm.project.value.drums.lanes[9].sample != null }
        assertEquals("test-hit", rhythm.project.value.drums.lanes[9].sample!!.name)
        var padPeak = 0f
        for (attempt in 0 until 20) {
            InstrumentationRegistry.getInstrumentation().runOnMainSync { NativeEngine.triggerPad(9, 1f) }
            Thread.sleep(150)
            padPeak = maxOf(padPeak, Metrics.streamInfo.value.peak, NativeEngine.streamInfo().peak)
            if (padPeak > 0.2f) break
        }
        assertTrue("imported sound plays on the pad (peak $padPeak)", padPeak > 0.2f)

        // Fit the imported hit to 1 beat (time-stretch), then resample one bar of the song onto pad 10.
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            rhythm.editPadSample(null) { it.copy(fitBeats = 1) }
            rhythm.hitPad(10)
            rhythm.resampleSong(1)
        }
        compose.waitUntil(30_000) { rhythm.project.value.drums.lanes[10].sample != null }
        assertEquals("Morceau 1 mesure", rhythm.project.value.drums.lanes[10].sample!!.name)
        assertEquals(1, rhythm.project.value.drums.lanes[9].sample!!.fitBeats)

        // Piano roll: the Notes view opens and a note added through the screen's path is kept.
        // Sons (phase 36): the synth track's chip opens its editor.
        compose.onNodeWithTag("tab_Sons").performClick()
        // On the CI emulator a tap after a while in the app sometimes does not land (docs/REVIEW.md,
        // « Tests d'usage réel »; the chip of the drums' editor shown is not selected, so a second
        // touch only opens the synth): up to three touches, then what the screen shows instead.
        val editorShown = { compose.onAllNodesWithTag("mode_notes").fetchSemanticsNodes().isNotEmpty() }
        var touches = 0
        while (!editorShown() && touches < 3) {
            if (touches > 0) android.util.Log.i("RealUse", "tap on track_0 did not land: again")
            // Lot 2 put « Remix » in the bar: on a narrow screen the track's chip is past its right edge.
            runCatching { compose.onNodeWithTag("track_0").performScrollTo() }
            compose.onNodeWithTag("track_0").performClick()
            touches++
            runCatching { compose.waitUntil(3_000) { editorShown() } }
        }
        if (!editorShown()) {
            // What the screen holds instead (every root: a window left open shows here too).
            val shown = compose.onAllNodes(androidx.compose.ui.test.SemanticsMatcher("any") { true }, useUnmergedTree = true)
                .fetchSemanticsNodes().mapNotNull { it.config.getOrNull(androidx.compose.ui.semantics.SemanticsProperties.TestTag) }
            throw AssertionError("no synth editor after $touches touches on track_0: selected=${rhythm.selectedTrack.value}, shown=${shown.joinToString(" ").take(3000)}")
        }
        runCatching { compose.onNodeWithTag("mode_notes").performScrollTo() }  // in a scrolling page or not, by layout
        compose.onNodeWithTag("mode_notes").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("piano_roll").fetchSemanticsNodes().isNotEmpty() }
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            rhythm.addNote(com.pocketstudio.model.Note(start = 0, length = 48, pitch = 60))
        }
        assertEquals(1, rhythm.project.value.synthNotes.notes.size)

        assertTrue("no NaN in the output", !NativeEngine.streamInfo().nonFinite)
    }

    /** 0.3 s of a loud 440 Hz tone, 44.1 kHz mono 16-bit PCM WAV. */
    private fun testWav(): ByteArray {
        val rate = 44100
        val n = rate * 3 / 10
        val b = java.nio.ByteBuffer.allocate(44 + n * 2).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        b.put("RIFF".toByteArray()).putInt(36 + n * 2).put("WAVE".toByteArray())
        b.put("fmt ".toByteArray()).putInt(16).putShort(1).putShort(1).putInt(rate).putInt(rate * 2).putShort(2).putShort(16)
        b.put("data".toByteArray()).putInt(n * 2)
        for (i in 0 until n) b.putShort((kotlin.math.sin(2 * Math.PI * 440 * i / rate) * 26000).toInt().toShort())
        return b.array()
    }
}
