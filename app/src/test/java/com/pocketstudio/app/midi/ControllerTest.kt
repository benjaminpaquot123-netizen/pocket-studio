package com.pocketstudio.app.midi

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.mixer.StripInfo
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.model.AutoTarget
import com.pocketstudio.model.InstrumentTrack
import com.pocketstudio.model.LiveSlots
import com.pocketstudio.model.Project
import com.pocketstudio.model.SongPart
import com.pocketstudio.model.midi.ControlButton
import com.pocketstudio.model.midi.Control
import com.pocketstudio.model.midi.Controllers
import com.pocketstudio.model.midi.KnobSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper
import java.io.File

/** The MiniLab lot: a controller's profile, its map on each screen, its faders, encoder and pads. */
@RunWith(RobolectricTestRunner::class)
class ControllerTest {
    private val app: Application = ApplicationProvider.getApplicationContext()
    private val minilab = "Minilab3 MIDI"

    /** The project and what the controller asked, as the real host would apply it. */
    private class Host(var p: Project = Project(), override var track: Int = 0) : KnobHost {
        val sent = ArrayList<Pair<KnobTarget, Float>>()
        val actions = ArrayList<ControllerAction>()
        override val project: Project get() = p
        override var live: LiveKnobs = LiveKnobs()
        override val strips: List<StripInfo> get() = com.pocketstudio.app.ui.mixerStrips(p)
        override fun apply(t: KnobTarget, value: Float) {
            sent += t to value
            val a = t.target
            p = when (a.kind) {
                AutoTarget.ACID -> if (t.live) p.also { live = live.copy(acid = live.acid + (a.param to value)) } else p.copy(acid = p.acid.copy(sound = p.acid.sound.with(a.param, value)))
                AutoTarget.VOLUME -> p.copy(mixer = p.mixer.copy(channels = p.mixer.channels.mapIndexed { i, c -> if (i == a.ch) c.copy(volume = value) else c }))
                else -> p
            }
        }
        override fun act(a: ControllerAction): String {
            actions += a
            return a.toString()
        }
    }

    private fun knobs(host: Host, screen: KnobScreen) = AutoKnobs(host).also { it.screen = screen; it.profile = Controllers.MINILAB3 }

    @Test
    fun eachScreenOfTheLivePageAndTheOthersHaveTheirKnobs() {
        val p = Project()
        val strips = com.pocketstudio.app.ui.mixerStrips(p)
        // Live with its 303: the six live knobs as its big sliders (heard there, not saved), then the returns.
        val acid = KnobPages.slots(KnobScreen.LiveAcide, p, 0, strips)
        assertEquals(listOf(0, 1, 2, 3, 4, 7), acid.take(6).map { it.target.target.param })
        assertTrue(acid.take(6).all { it.target.live && it.target.target.kind == AutoTarget.ACID })
        assertEquals(AutoTarget.live(AutoTarget.LIVE_REVERB_RETURN), acid[6].target.target)
        // Live with its effects: the pad XY's DJ filter on knobs 1 and 2.
        val fx = KnobPages.slots(KnobScreen.Live, p, 0, strips)
        assertEquals(AutoTarget.live(AutoTarget.LIVE_FILTER), fx[0].target.target)
        assertEquals(AutoTarget.live(AutoTarget.LIVE_RESONANCE), fx[1].target.target)
        // The other tabs: the selected track's strip (volume, pan, macros, sends).
        val piste = KnobPages.slots(KnobScreen.Piste, p, 0, strips)
        assertTrue(piste.all { it.target.target.ch == InstrumentTrack.mixerChannel(0) })
        assertEquals(8, piste.size)
        // Mixeur: with faders on the volumes, the knobs set pans and reverb sends.
        val mix = KnobPages.slots(KnobScreen.Mixeur, p, 0, strips, faders = 4)
        assertEquals(List(4) { AutoTarget.PAN } + List(4) { AutoTarget.SEND }, mix.map { it.target.target.kind })
        assertTrue(KnobPages.slots(KnobScreen.Mixeur, p, 0, strips).all { it.target.target.kind == AutoTarget.VOLUME })
        // Which screen the controller follows.
        assertEquals(KnobScreen.LiveAcide, KnobScreen.of("Live", live = true, liveTab = "Acide"))
        assertEquals(KnobScreen.LivePads, KnobScreen.of("Live", live = true, liveTab = "Pads"))
        assertEquals(KnobScreen.LivePads, KnobScreen.of("Morceau", live = false, livePads = true))
        assertEquals(KnobScreen.Live, KnobScreen.of("Live", live = true, liveTab = "Notes"))
        assertEquals(KnobScreen.Piste, KnobScreen.of("Audio", live = false))
        assertEquals(KnobScreen.Mixeur, KnobScreen.of("Mixeur", live = false))
    }

    @Test
    fun fadersMoveTheVolumesOnceTheyReachThem() {
        val host = Host()
        val k = knobs(host, KnobScreen.Acide)
        val drums = host.p.mixer.channels[0].volume  // 0.8
        // Fader 1 (CC 82) at the bottom: nothing, the hint says to push it up.
        assertTrue(k.control(82, 0, minilab))
        assertTrue(host.sent.isEmpty())
        assertEquals(1, k.last?.turn)
        assertTrue(k.last!!.faders)
        // Through the volume: it picks up and follows.
        k.control(82, 120, minilab)
        assertEquals(AutoTarget(AutoTarget.VOLUME, 0), host.sent.single().first.target)
        assertEquals(120 / 127f, host.p.mixer.channels[0].volume, 1e-6f)
        assertTrue(120 / 127f > drums)
        // The DAW program's fader 1 (CC 14) is the same fader.
        k.control(14, 100, minilab)
        assertEquals(100 / 127f, host.p.mixer.channels[0].volume, 1e-6f)
    }

    @Test
    fun relativeKnobsStepWithoutJumps() {
        val host = Host()
        val k = knobs(host, KnobScreen.Acide)
        val cutoff = host.p.acid.sound.cutoff
        // DAW program: knob 1 is CC 86, relative around 64: two clicks up, one down.
        k.control(86, 66, minilab)
        assertEquals(cutoff + 2 * AutoKnobs.STEP, host.p.acid.sound.cutoff, 1e-6f)
        k.control(86, 63, minilab)
        assertEquals(cutoff + AutoKnobs.STEP, host.p.acid.sound.cutoff, 1e-6f)
        // A controller the app does not know whose knobs turn out endless (two's complement).
        val other = Host()
        val g = AutoKnobs(other).also { it.screen = KnobScreen.Acide }
        repeat(6) { g.control(21, 1, "Boîte noire") }
        assertEquals("RELATIVE_TWOS", g.settings.modes["Boîte noire"])
        val before = other.p.acid.sound.cutoff
        g.control(21, 127, "Boîte noire")
        assertEquals(before - AutoKnobs.STEP, other.p.acid.sound.cutoff, 1e-6f)
    }

    @Test
    fun theMainEncoderAndItsClickFollowTheScreen() {
        val host = Host()
        val k = knobs(host, KnobScreen.Synthe)
        k.control(28, 65, minilab)  // a click clockwise
        assertEquals(ControllerAction.Track(1), host.actions.last())
        k.control(29, 62, minilab)  // with Shift, two down
        assertEquals(ControllerAction.Tempo(-2), host.actions.last())
        k.control(118, 127, minilab)
        assertEquals(ControllerAction.TogglePlay(live = false), host.actions.last())
        k.control(118, 0, minilab)  // let go: nothing more
        assertEquals(3, host.actions.size)
        k.control(119, 127, minilab)
        assertEquals(ControllerAction.Stop, host.actions.last())
        // On the Live page: the encoder sets the tempo, the click plays the scene shown.
        k.screen = KnobScreen.Live
        k.control(28, 65, minilab)
        assertEquals(ControllerAction.Tempo(1), host.actions.last())
        k.control(118, 127, minilab)
        assertEquals(ControllerAction.TogglePlay(live = true), host.actions.last())
        assertEquals(host.actions.last().toString(), k.flash)
        // A transport of another controller (nanoKONTROL2): Record only on the Live page.
        val nano = AutoKnobs(host).also { it.screen = KnobScreen.Live; it.profile = Controllers.detect("nanoKONTROL2") }
        nano.control(45, 127, "nanoKONTROL2")
        assertEquals(ControllerAction.Record, host.actions.last())
        nano.control(42, 127, "nanoKONTROL2")
        assertEquals(ControllerAction.Stop, host.actions.last())
    }

    @Test
    fun padsLaunchRowsMuteColumnsOrPlayLivePads() {
        val host = Host()
        val k = knobs(host, KnobScreen.Live)
        assertEquals(ControllerAction.LaunchRow(1), k.pad(1))
        assertEquals(ControllerAction.Mute(0), k.pad(8))
        k.screen = KnobScreen.LiveAcide
        assertEquals(ControllerAction.LaunchCell(SongPart.ACID, 2), k.pad(2))
        k.screen = KnobScreen.LivePads
        assertEquals(ControllerAction.LivePad(9), k.pad(9))
        // Elsewhere the drums play them (from the MIDI thread): nothing for the app.
        k.screen = KnobScreen.Rythme
        assertNull(k.pad(0))
    }

    @Test
    fun learningAFaderOrAButtonOnAGenericController() {
        val host = Host()
        val k = AutoKnobs(host).also { it.screen = KnobScreen.Rythme }
        k.learningControl = Control.Fader(0)
        assertTrue(k.control(40, 10, "Clavier X"))
        assertEquals(40, k.settings.faders["Clavier X"]?.get(0))
        k.control(40, 127, "Clavier X")
        k.control(40, 102, "Clavier X")
        assertEquals(102 / 127f, host.p.mixer.channels[0].volume, 1e-6f)
        k.learningControl = Control.Button(ControlButton.CLICK)
        k.control(41, 0, "Clavier X")
        k.control(41, 127, "Clavier X")
        assertEquals(ControllerAction.TogglePlay(false), host.actions.last())
        // The legend says what the learnt faders do.
        val rows = ControllerLegend.rows(KnobScreen.Rythme, k.slots(), k.faderSlots(), null, host.p)
        assertTrue(rows.any { it.tag == "pads_a" })
        assertFalse(rows.any { it.tag == "encoder" })
    }

    @Test
    fun theMiniLabsLegendFollowsTheScreen() {
        val p = Project()
        val strips = com.pocketstudio.app.ui.mixerStrips(p)
        val live = ControllerLegend.rows(KnobScreen.Live, KnobPages.slots(KnobScreen.Live, p, 0, strips), KnobPages.faders(strips).take(4), Controllers.MINILAB3, p)
        assertTrue(live.single { it.tag == "pads_a" }.does.contains("A"))
        assertTrue(live.any { it.tag == "faders" })
        assertTrue(live.any { it.tag == "encoder" })
        val rythme = ControllerLegend.rows(KnobScreen.Rythme, KnobPages.slots(KnobScreen.Rythme, p, 0, strips), KnobPages.faders(strips).take(4), Controllers.MINILAB3, p)
        assertTrue(rythme.single { it.tag == "pads_a" }.does.contains("batterie"))
    }

    // --- Through the real ViewModel ------------------------------------------------------------

    private fun twoScenes(): RhythmViewModel {
        File(app.filesDir, "projects").deleteRecursively()
        val vm = RhythmViewModel(app)
        vm.toggleHit(0, 0)
        vm.duplicateScene()
        vm.toggleHit(2, 4)
        vm.selectScene(0)
        return vm
    }

    @Test
    fun theMiniLabsKnobsMoveTheLive303AsItsSlidersShowAndItsPadsLaunch() {
        val vm = twoScenes()
        val k = AutoKnobs(rhythmKnobHost(vm), KnobSettings()).also { it.screen = KnobScreen.LiveAcide; it.profile = Controllers.MINILAB3 }
        val saved = vm.project.value.acid.sound.cutoff
        // Knob 1 (CC 74) swept from the bottom through the cutoff: the Live page's slider follows,
        // the saved sound does not change (the Live 303 is heard, not saved).
        (0..127 step 8).forEach { k.control(74, it, minilab) }
        assertEquals(120 / 127f, vm.liveAcid.value[0]!!, 1e-6f)
        assertEquals(saved, vm.project.value.acid.sound.cutoff, 1e-6f)
        // On the Acide screen the same knob moves the saved sound (what its knob shows).
        k.screen = KnobScreen.Acide
        (127 downTo 31 step 8).forEach { k.control(74, it, minilab) }
        assertEquals(31 / 127f, vm.project.value.acid.sound.cutoff, 1e-6f)
        // Pad 2 on the Live page: row B, from the next launch line once playing.
        k.screen = KnobScreen.Live
        vm.togglePlay()
        k.pad(1)
        assertEquals(1, vm.liveGrid.value!!.pendingRow)
        assertEquals("Ligne B", k.flash)
        // Pad 9 (bank B): the first column (the drums) muted, then back.
        k.pad(8)
        assertTrue(SongPart.DRUMS in vm.liveMuted.value)
        k.pad(8)
        assertFalse(SongPart.DRUMS in vm.liveMuted.value)
        // With the 303 panel: pad 1 launches the 303 of row A alone.
        k.screen = KnobScreen.LiveAcide
        k.pad(0)
        assertEquals(0, vm.liveGrid.value!!.slot(LiveSlots.ACID).pending)
    }

    @Test
    fun fakeMidiInputSendsPadsToTheDrumsOrToTheLivePage() {
        val midi = MidiViewModel(app)
        val got = ArrayList<MidiCommand>()
        val scope = CoroutineScope(Dispatchers.Unconfined)
        scope.launch { midi.commands.collect { got += it } }
        // Pad 1 of the MiniLab (channel 10, note 36): the drums, as a pad note for a take.
        midi.receiveForTest(byteArrayOf(0x99.toByte(), 36, 100))
        assertEquals(MidiCommand.Note(9, 36, 100 / 127f, true), got.last())
        // On the Live page the app takes it: pad 1.
        midi.padsToDrums = false
        midi.receiveForTest(byteArrayOf(0x99.toByte(), 44, 90))
        assertEquals(MidiCommand.Pad(8, 90 / 127f), got.last())
        // A knob: a control change for the app, and a line in the monitor.
        midi.receiveForTest(byteArrayOf(0xB0.toByte(), 74, 33))
        assertEquals(MidiCommand.Control(0, 74, 33), got.last())
        ShadowLooper.idleMainLooper()
        assertTrue(midi.state.value.monitor.first().contains("74"))
        assertEquals("minilab3", midi.state.value.controller?.profile?.id)
        // « Apprendre mes pads »: notes are heard, not played.
        midi.learnPads(true)
        midi.receiveForTest(byteArrayOf(0x90.toByte(), 60, 90))
        assertEquals(MidiCommand.PadLearnt(Controllers.packPad(0, 60)), got.last())
        midi.learnPads(false)
        // Taught pads on channel 1: note 60 is pad 1 now, even on the Live page.
        midi.pads = PadMap(null, listOf(Controllers.packPad(0, 60)))
        midi.receiveForTest(byteArrayOf(0x90.toByte(), 60, 90))
        assertEquals(MidiCommand.Pad(0, 90 / 127f), got.last())
        scope.cancel()
    }
}
