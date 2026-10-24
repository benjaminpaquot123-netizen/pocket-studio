package com.pocketstudio.app.midi

import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.mixer.StripInfo
import com.pocketstudio.model.AutoTarget
import com.pocketstudio.model.InstrumentTrack
import com.pocketstudio.model.Project
import com.pocketstudio.model.SynthPatch
import com.pocketstudio.model.withTrack
import com.pocketstudio.model.track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AutoKnobsTest {
    /** The project and what the knobs sent, as the real host would apply it (the project changes). */
    private class Host(var p: Project = Project(), override var track: Int = 0) : KnobHost {
        val sent = ArrayList<Pair<KnobTarget, Float>>()
        override val project: Project get() = p
        override var live: LiveKnobs = LiveKnobs()
        override val strips: List<StripInfo> get() = com.pocketstudio.app.ui.mixerStrips(p)
        override fun apply(t: KnobTarget, value: Float) {
            sent += t to value
            val a = t.target
            p = when (a.kind) {
                AutoTarget.ACID -> p.copy(acid = p.acid.copy(sound = p.acid.sound.with(a.param, value)))
                AutoTarget.VOLUME -> p.copy(mixer = p.mixer.copy(channels = p.mixer.channels.mapIndexed { i, c -> if (i == a.ch) c.copy(volume = value) else c }))
                else -> p
            }
        }
    }

    private val minilab = "Arturia MiniLab 3"

    @Test
    fun eachScreenHasItsEightMostUsefulKnobs() {
        val p = Project()
        val strips = com.pocketstudio.app.ui.mixerStrips(p)
        val acid = KnobPages.slots(KnobScreen.Acide, p, 0, strips)
        assertEquals(listOf(0, 1, 2, 3, 4, 7, 5, 8), acid.map { it.target.target.param })
        assertTrue(acid.all { it.target.target.kind == AutoTarget.ACID && !it.target.live })
        val synth = KnobPages.slots(KnobScreen.Synthe, p, 0, strips)
        assertEquals(8, synth.size)
        assertEquals(AutoTarget.synth(0, SynthPatch.CUTOFF), synth[0].target.target)
        assertEquals(AutoTarget.synth(0, SynthPatch.RESONANCE), synth[1].target.target)
        assertTrue(synth.any { it.target.target == AutoTarget.synth(0, SynthPatch.A_ATTACK) })
        assertTrue(synth.any { it.target.target == AutoTarget.synth(0, SynthPatch.WIDTH) })
        // A Plaits track: its own knobs, then the track's volume.
        val plaits = p.withTrack(p.track(0).copy(engine = 1))
        val pk = KnobPages.slots(KnobScreen.Synthe, plaits, 0, strips)
        assertEquals(AutoTarget.PLAITS, pk[0].target.target.kind)
        assertEquals(AutoTarget(AutoTarget.VOLUME, InstrumentTrack.mixerChannel(0)), pk.last().target.target)
        // Mixeur: the faders of the strips, in their order, named after them.
        val mixer = KnobPages.slots(KnobScreen.Mixeur, p, 0, strips)
        assertEquals(strips.filter { it.ch != 4 }.take(8).map { it.ch }, mixer.map { it.target.target.ch })
        assertTrue(mixer.all { it.target.target.kind == AutoTarget.VOLUME })
        // Live: the big effects and the 303 as the Live page plays it.
        val live = KnobPages.slots(KnobScreen.Live, p, 0, strips)
        assertEquals(AutoTarget.live(AutoTarget.LIVE_FILTER), live[0].target.target)
        assertTrue(live.any { it.target.target == AutoTarget.live(AutoTarget.LIVE_REVERB_RETURN) })
        assertTrue(live.filter { it.target.target.kind == AutoTarget.ACID }.all { it.target.live })
        // Rythme: the drums' strip.
        assertTrue(KnobPages.slots(KnobScreen.Rythme, p, 0, strips).all { it.target.target.ch == 0 })
        // Every screen: eight knobs, each its own setting, each named.
        KnobScreen.entries.forEach { s ->
            val slots = KnobPages.slots(s, p, 0, strips)
            assertTrue("$s", slots.size == 8 || s == KnobScreen.Mixeur)
            assertEquals("$s", slots.size, slots.map { it.target }.toSet().size)
            assertTrue("$s", slots.all { it.label.isNotBlank() })
        }
        assertEquals(KnobScreen.Live, KnobScreen.of("Acide", live = true))
        assertEquals(KnobScreen.Mixeur, KnobScreen.of("Morceau", live = false))
    }

    @Test
    fun aKnobMovesItsSettingOnceItHasReachedIt() {
        val host = Host()
        val knobs = AutoKnobs(host)
        knobs.screen = KnobScreen.Acide
        val cutoff = host.p.acid.sound.cutoff  // 0.35
        // Knob 1 of the MiniLab (CC 74) far below: nothing moves, the hint says to turn up.
        assertTrue(knobs.control(74, 0, minilab))
        assertTrue(host.sent.isEmpty())
        assertEquals(1, knobs.last?.turn)
        assertEquals(0, knobs.last?.touched)
        // Past the setting: it picks up and follows.
        assertTrue(knobs.control(74, 60, minilab))
        assertEquals(AutoTarget.acid(0), host.sent.single().first.target)
        assertEquals(60 / 127f, host.p.acid.sound.cutoff, 1e-6f)
        assertTrue(60 / 127f > cutoff)
        knobs.control(74, 90, minilab)
        assertEquals(90 / 127f, host.p.acid.sound.cutoff, 1e-6f)
        // Knob 2 (CC 71) is the resonance.
        knobs.control(71, Math.round(host.p.acid.sound.resonance * 127), minilab)
        assertEquals(AutoTarget.acid(1), host.sent.last().first.target)
        // Not a knob: left to the rest of the app.
        assertFalse(knobs.control(1, 64, minilab))
        assertFalse(knobs.control(82, 64, minilab))
    }

    @Test
    fun anotherScreenGivesTheSameKnobAnotherSettingAndItWaitsAgain() {
        val host = Host()
        val knobs = AutoKnobs(host)
        knobs.screen = KnobScreen.Acide
        knobs.control(74, 44, minilab)
        knobs.control(74, 45, minilab)
        val acidMoves = host.sent.size
        knobs.screen = KnobScreen.Mixeur
        // The drums' fader is at 0.8: from 45 nothing moves until the knob gets there.
        knobs.control(74, 50, minilab)
        assertEquals(acidMoves, host.sent.size)
        knobs.control(74, 110, minilab)
        assertEquals(AutoTarget(AutoTarget.VOLUME, 0), host.sent.last().first.target)
        assertEquals(110 / 127f, host.p.mixer.channels[0].volume, 1e-6f)
    }

    @Test
    fun learnAndChangeAreKeptPerKnob() {
        val host = Host()
        val knobs = AutoKnobs(host)
        knobs.screen = KnobScreen.Acide
        // « Apprendre » on knob 3: the next CC turned becomes it.
        knobs.learning = 2
        assertTrue(knobs.control(99, 10, minilab))
        assertNull(knobs.learning)
        assertEquals(99, knobs.settings.learnt(minilab, 2))
        knobs.control(99, Math.round(host.p.acid.sound.envMod * 127), minilab)
        assertEquals(AutoTarget.acid(2), host.sent.last().first.target)
        // « Changer »: knob 1 moves the 303's volume on Acide from now on.
        knobs.override(0, KnobTarget(AutoTarget.acid(8)))
        assertEquals(AutoTarget.acid(8), knobs.slots()[0].target.target)
        assertEquals(knobs.slots()[7].label, knobs.slots()[0].label)
        // Only on Acide.
        knobs.screen = KnobScreen.Synthe
        assertEquals(AutoTarget.synth(0, SynthPatch.CUTOFF), knobs.slots()[0].target.target)
        knobs.screen = KnobScreen.Acide
        knobs.reset(minilab)
        assertEquals(AutoTarget.acid(0), knobs.slots()[0].target.target)
        assertNull(knobs.settings.learnt(minilab, 2))
        // Off: the knobs do nothing.
        knobs.setEnabled(false)
        assertFalse(knobs.control(74, 64, minilab))
    }

    @Test
    fun targetsPackAndUnpack() {
        listOf(
            KnobTarget(AutoTarget.acid(7), live = true), KnobTarget(AutoTarget.synth(3, SynthPatch.WIDTH)), KnobTarget(AutoTarget(AutoTarget.SEND, 9, 1)),
            KnobTarget(AutoTarget.live(AutoTarget.LIVE_BUILD_UP)), KnobTarget(AutoTarget.macro(2, 3)),
        ).forEach { assertEquals(it, KnobTarget.of(it.key)) }
    }

    @Test
    fun theSettingsSurviveARestart() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = KnobsViewModel(app)
        val host = Host()
        vm.attach(host)
        vm.screen = KnobScreen.Acide
        vm.open(minilab)
        vm.learn(4)
        assertTrue(vm.control(42, 0, minilab))
        vm.override(1, KnobTarget(AutoTarget.acid(8)))
        vm.setEnabled(true)
        // A new start of the app: read again from the preferences.
        val again = KnobsViewModel(app)
        again.attach(Host())
        again.screen = KnobScreen.Acide
        again.open(minilab)
        val hint = again.hint.value!!
        assertEquals(AutoTarget.acid(8), hint.slots[1].target.target)
        assertEquals(42, hint.ccs[4])
        assertEquals(74, hint.ccs[0])
        assertTrue(again.enabled.value)
    }
}
