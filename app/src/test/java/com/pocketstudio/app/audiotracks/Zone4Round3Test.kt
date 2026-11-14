package com.pocketstudio.app.audiotracks

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.pocketstudio.app.nav.AppHarness
import com.pocketstudio.app.rhythm.AudioRecordingUi
import com.pocketstudio.app.ui.MuteToggle
import com.pocketstudio.engine.CommandSink
import com.pocketstudio.engine.ExportResult
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.model.AudioClip
import com.pocketstudio.model.AudioTrack
import com.pocketstudio.model.SimpleMelody
import com.pocketstudio.model.SoundLibrary
import com.pocketstudio.model.allScenes
import com.pocketstudio.model.instrumentTracks
import com.pocketstudio.model.sceneView
import com.pocketstudio.model.track
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Audit ultra round 3, zone 4 (recording), without the bench (no HumanTester, no BenchEngine):
 * - n° 24 (seat E c): the take's count-in, level and « Arrêter et garder » sit in the card of the track that records,
 *   in sight (they were at the top of the page, out of sight on the second track); with the top ●'s new « Voix »
 *   track (seat C a, n° 23) the take is seen where it goes;
 * - n° 25 (seats C a, E c): a take touched is heard alone (comparing takes); no « Prise précédente » that swaps back
 *   and forth; one duration per take;
 * - n° 26 (seat C c): the notifications are not asked in the middle of the first take; « Ma mélodie » takes the sound
 *   chosen on the melody track (it was renamed after its sound and lost as the melody); the keys' short taps are
 *   recorded with an engine that moves on (the bench's never did: banc);
 * - n° 27 (seat E c): a mute switch reads « Son de Voix, désactivé », never « Muet » + « désactivé ».
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp")
class Zone4Round3Test {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val h = AppHarness(compose)

    @After
    fun end() {
        NativeEngine.sink = null
        h.finish()
    }

    private fun clip(file: String, take: Int, seconds: Double) = AudioClip(file, "Prise 12:00", seconds = seconds, take = take)

    private fun screen(ui: AudioTracksUi, actions: AudioTracksActions = AudioTracksActions()) {
        compose.setContent { PocketStudioTheme { Box(Modifier.size(360.dp, 640.dp)) { AudioTracksScreen(ui, actions) } } }
        compose.waitForIdle()
    }

    /** n° 27: the word under the icon is not read as the switch's name (« Muet », « désactivé » = « not muted »). */
    @Test
    fun aMutedTracksSwitchReadsTrue() {
        compose.setContent { PocketStudioTheme { MuteToggle(muted = true, name = "Voix", tag = "m", caption = true) {} } }
        val node = compose.onNodeWithTag("m").fetchSemanticsNode().config
        val words = node.getOrNull(SemanticsProperties.Text)?.joinToString(" ") { it.text }
        assertTrue("read as its name and state only, not « $words »", words.isNullOrEmpty())
        assertEquals(listOf("Son de Voix"), node[SemanticsProperties.ContentDescription])
        assertEquals("désactivé", node[SemanticsProperties.StateDescription])
    }

    /** n° 24: on the third track, the take's state is in that track's card and in sight. */
    @Test
    fun theTakesStateSitsInTheTrackThatRecords() {
        val tracks = (0..2).map { AudioTrack(it, "Audio ${it + 1}", clip = clip("t$it.wav", 1, 8.0)) }
        screen(AudioTracksUi(tracks, recording = AudioRecordingUi(2, waiting = false, seconds = 3f, level = 0.5f)))
        compose.onNode(hasTestTag("audio_stop") and hasAnyAncestor(hasTestTag("audio_track_2"))).assertIsDisplayed()
    }

    /** n° 25: the take picker numbers every take; no « Prise précédente » that flips between two of them. */
    @Test
    fun noPreviousTakeThatSwapsBack() {
        val t = AudioTrack(0, "Voix", clip = clip("b.wav", 2, 8.46), previous = clip("a.wav", 1, 7.04))
        screen(AudioTracksUi(listOf(t)))
        compose.onNodeWithTag("audio_takes_0").assertExists()
        assertTrue(compose.onAllNodes(hasTestTag("audio_previous_0")).fetchSemanticsNodes().isEmpty())
    }

    /** n° 25 (seat C a: « Take 1 » touched, nothing heard): a take touched is heard on its track alone, to compare. */
    @Test
    fun aTakeTouchedIsHeardAlone() {
        val heard = mutableListOf<String>()
        val t = AudioTrack(0, "Voix", clip = clip("b.wav", 2, 8.46), previous = clip("a.wav", 1, 7.04))
        screen(AudioTracksUi(listOf(t)), AudioTracksActions(hearTake = { id, other -> heard += "$id $other" }, listenAlone = { heard += "alone $it" }))
        compose.onNodeWithTag("audio_take_0_1").performClick()
        compose.onNodeWithTag("audio_take_0_2").performClick()
        compose.onNodeWithTag("audio_alone_0").performClick()
        assertEquals(listOf("0 0", "0 null", "alone 0"), heard)
    }

    /** The view model's side: the take chosen plays and the track is heard alone, its loop going on through the next take. */
    @Test
    fun theViewModelPlaysTheTakeAlone() {
        h.start(simple = true) {
            addAudioTrack()
            session.edit { p -> p.copy(audioTracks = p.audioTracks.map { it.copy(clip = clip("b.wav", 2, 8.0), previous = clip("a.wav", 1, 7.0)) }) }
        }
        val vm = h.vm
        val id = vm.project.value.audioTracks.first().id
        compose.runOnIdle { vm.hearTake(id, 0) }
        assertEquals("a.wav", vm.project.value.audioTracks.first().clip?.file)
        val alone = com.pocketstudio.app.rhythm.AloneTarget.ofSongKey(com.pocketstudio.model.SongPart.audio(id))
        assertEquals(alone, vm.alone.value?.target)
        compose.runOnIdle { vm.hearTake(id, 0) }
        assertEquals("b.wav", vm.project.value.audioTracks.first().clip?.file)
        assertEquals("still heard alone", alone, vm.alone.value?.target)
    }

    /** n° 25 (seat E c: « Take 2 · 8 s » and « Take 2 · 8.5 s »): one length per take, wherever it is written. */
    @Test
    fun oneDurationPerTake() {
        val t = AudioTrack(0, "Voix", clip = clip("b.wav", 2, 8.46), previous = clip("a.wav", 1, 7.04))
        screen(AudioTracksUi(listOf(t)))
        val texts = compose.onAllNodes(hasText("Prise 2 ·", substring = true), useUnmergedTree = true).fetchSemanticsNodes()
            .flatMap { n -> n.config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text } }
            .filter { "Prise 2 ·" in it }.toSet()
        assertEquals("one length for take 2: $texts", 1, texts.size)
    }

    /** n° 26 (seat C c): the notifications are asked after the first take, never in its middle. */
    @Test
    fun theNotificationsWaitForTheTakesEnd() {
        h.start(simple = true)
        compose.runOnIdle { h.vm.recordMelodyTake() }
        compose.waitForIdle()
        assertTrue("a take records", h.vm.recording.value)
        assertFalse("asked in the middle of the take: ${requested()}", android.Manifest.permission.POST_NOTIFICATIONS in requested())
        compose.runOnIdle { h.vm.recordMelodyTake() }
        compose.waitForIdle()
        assertFalse(h.vm.recording.value)
        assertTrue("asked once the take is over: ${requested()}", android.Manifest.permission.POST_NOTIFICATIONS in requested())
    }

    private fun requested(): List<String> =
        org.robolectric.Shadows.shadowOf(compose.activity).lastRequestedPermission?.requestedPermissions?.toList().orEmpty()

    /** n° 26 (seat C c: FM piano chosen for « Mélodie », « Ma mélodie » came with FM bell): the melody's sound is kept. */
    @Test
    fun myMelodyTakesTheSoundChosenForTheMelody() {
        h.start(simple = true)
        val vm = h.vm
        // The song's melody track, « Lead » (as « Mélodie » in the seat's Trap song).
        val melody = vm.project.value.instrumentTracks.last { it.name == "Lead" }.id
        val chosen = SoundLibrary.SYNTH.last()
        compose.runOnIdle {
            vm.selectTrack(melody)
            vm.applySynthPreset(chosen.sound, name = chosen.name)
        }
        assertEquals("the melody's role stays its name", "Lead", vm.project.value.track(melody).name)
        compose.runOnIdle { vm.recordMelodyTake() }
        compose.waitForIdle()
        val mine = SimpleMelody.trackOf(vm.project.value)
        assertNotNull(mine)
        assertEquals("« Ma mélodie » plays the sound chosen", chosen.sound, vm.project.value.track(mine!!).synth)
    }

    /** n° 26 (seat C c, « nothing played during the take » after short taps): with an engine that moves on, taps are kept. */
    @Test
    fun theKeysShortTapsAreRecorded() {
        NativeEngine.sink = MovingEngine()
        h.start(simple = true)
        val vm = h.vm
        compose.runOnIdle { vm.recordMelodyTake() }
        compose.waitForIdle()
        listOf(60, 64, 67).forEach { k ->
            compose.runOnIdle { vm.synthNoteOn(k) }
            Thread.sleep(80)
            compose.runOnIdle { vm.synthNoteOff(k) }
        }
        compose.runOnIdle { vm.recordMelodyTake() }
        compose.waitForIdle()
        val mine = SimpleMelody.trackOf(vm.project.value)!!
        val notes = vm.project.value.allScenes.indices.flatMap { vm.project.value.sceneView(it).track(mine).notes.notes }
        assertEquals("three taps, three notes (${vm.lastTakeReport})", 3, notes.size)
    }

    /** A phone's engine as far as a take needs it: the transport a few sixteenths past the take's first step. */
    private class MovingEngine : CommandSink {
        private var next = 1L
        override fun post(target: Long, type: Int, value: Float, a: Int, b: Int) = Unit
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
        private val start = System.currentTimeMillis()
        override fun playPosition(): Double = 2.0 + (System.currentTimeMillis() - start) / 125.0 % 12.0
        override fun transportStep(): Int = playPosition().toInt()
        override fun transportPosition(): Double = playPosition()
    }
}
