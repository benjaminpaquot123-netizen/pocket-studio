package com.pocketstudio.app.guide

import android.os.Looper
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.engine.OfflineExport
import com.pocketstudio.model.EffectType
import com.pocketstudio.model.InstrumentTrack
import com.pocketstudio.model.PadSample
import com.pocketstudio.model.Project
import com.pocketstudio.model.StarterWizard
import com.pocketstudio.model.guide.GuideActions
import com.pocketstudio.model.instrumentTracks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.time.Duration

/** « Écouter avant d'appliquer » (docs/PLAN-GUIDE.md §4.3): what is rendered, in what order, and what the panel is told. */
@RunWith(RobolectricTestRunner::class)
class GuideListenTest {
    private fun jumpUp(): Project {
        val style = StarterWizard.style("jump_up")!!
        val p = StarterWizard.build(style, style.lines.associate { it.role to null as PadSample? }, style.melody.mood,
            StarterWizard.defaultParts(style).associateWith { StarterWizard.soundsFor(it, style).first() })
        return GuideActions.withBaseline(p)
    }

    private class FakePlayer : GuideListenPlayer {
        val played = ArrayList<Pcm>()
        var stops = 0
        var ended: (() -> Unit)? = null
        var works = true
        override fun play(pcm: Pcm, ended: () -> Unit): Boolean {
            if (!works) return false
            played += pcm
            this.ended = ended
            return true
        }
        override fun stop() { stops++ }
    }

    private fun idle(ms: Long = 50) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))

    private fun await(what: String, ok: () -> Boolean) {
        val end = System.currentTimeMillis() + 30_000
        while (!ok()) {
            idle(20)
            check(System.currentTimeMillis() < end) { "$what: not in time" }
            Thread.sleep(5)
        }
    }

    private fun Project.subSaturated() = mixer.channels[InstrumentTrack.mixerChannel(instrumentTracks.first { it.name == "Sub" }.id)]
        .inserts.any { it.effect == EffectType.SATURATION }

    /** One second of a constant level, stereo. */
    private fun tone(level: Float) = FloatArray(OfflineExport.SAMPLE_RATE * 2) { level }

    @Test
    fun anOptionIsHeardWithoutThenWithItAndTheSongDoesNotChange() {
        val scope = MainScope()
        val p = jumpUp()
        val rendered = ArrayList<Pair<Project, Project>>()
        val player = FakePlayer()
        var paused = 0
        val listen = GuideListen(
            scope, project = { p },
            render = { with, without -> rendered += with to without; tone(0.5f) to tone(0.25f) },
            player = player, pauseMusic = { paused++ }, clock = { 0L },
        )
        listen.toggle("fx.sub", "light")
        assertEquals(GuideListenUi("fx.sub", "light"), listen.state.value)
        await("playing") { listen.state.value?.preparing == false }
        // « Avec » is the song with the Sub's saturation, « Sans » the song as it is; nothing changed in it.
        val (with, without) = rendered.single()
        assertTrue(with.subSaturated())
        assertTrue(without === p)
        assertFalse(p.subSaturated())
        assertEquals(1, paused)
        // « Sans » first, a gap, then « Avec ».
        val pcm = player.played.single()
        assertEquals(2, pcm.channels)
        assertEquals((OfflineExport.SAMPLE_RATE * 2 + GuideListen.GAP_FRAMES) * 2, pcm.data.size)
        assertEquals(0.25f, pcm.data[0])
        assertEquals(0.5f, pcm.data.last())
        assertEquals(0, listen.state.value!!.take)
        // The second take lights once it is heard.
        await("avec") { listen.state.value?.take == 1 }
        // Played to its end: the panel is told.
        player.ended!!.invoke()
        await("ended") { listen.state.value == null }
        // Heard again: the takes kept are played at once.
        listen.toggle("fx.sub", "light")
        await("again") { player.played.size == 2 }
        assertEquals(1, listen.renders)
        // A touch on ▶ while it plays stops it.
        listen.toggle("fx.sub", "light")
        assertNull(listen.state.value)
        assertTrue(player.stops > 0)
        scope.cancel()
    }

    @Test
    fun anOptionThatChangesNothingIsNotRenderedAndAFailureIsSaid() {
        val scope = MainScope()
        val p = jumpUp()
        var renders = 0
        val player = FakePlayer()
        var engine = true
        val listen = GuideListen(
            scope, project = { p },
            render = { _, _ -> renders++; if (engine) tone(0.1f) to tone(0.1f) else null },
            player = player, clock = { 0L },
        )
        // « Garder » changes nothing in the song (only the Guide's own state): nothing to compare.
        listen.toggle("fx.sub", "keep")
        await("nothing") { listen.state.value == null }
        idle(200)
        assertEquals(0, renders)
        assertTrue(player.played.isEmpty())
        // No engine here: said, never stuck on « preparing ».
        engine = false
        listen.toggle("fx.sub", "light")
        await("failed") { listen.state.value?.failed == true }
        assertTrue(player.played.isEmpty())
        // A player that cannot start: said too.
        engine = true
        player.works = false
        listen.toggle("fx.sub", "light")
        await("failed again") { listen.state.value?.let { it.failed && renders == 2 } == true }
        scope.cancel()
    }
}
