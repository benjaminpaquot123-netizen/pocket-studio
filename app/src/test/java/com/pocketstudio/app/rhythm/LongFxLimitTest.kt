package com.pocketstudio.app.rhythm

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.model.AudioTrack
import com.pocketstudio.model.EffectType
import com.pocketstudio.model.Insert
import com.pocketstudio.model.InstrumentTrack
import com.pocketstudio.model.Mixer
import com.pocketstudio.model.addTrack
import com.pocketstudio.model.longFxCount
import com.pocketstudio.model.longFxLimitNotice
import com.pocketstudio.model.removeTrack
import com.pocketstudio.engine.NativeEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * The long effects (echoes, reverbs, tape stops) borrow memory the engine holds for a few of them:
 * wherever one becomes active, past the limit it is refused with the warning, never left silent.
 */
@RunWith(RobolectricTestRunner::class)
class LongFxLimitTest {
    private val app: Application = ApplicationProvider.getApplicationContext()
    private val limit = EffectType.longLimit(EffectType.ECHO)

    @Before
    fun fresh() {
        File(app.filesDir, "projects").deleteRecursively()
    }

    /** A VM whose project has [limit] echoes: channels 0 (all 8 slots) and 1 (the rest). */
    private fun fullOfEchoes(): RhythmViewModel {
        val vm = RhythmViewModel(app)
        for (i in 0 until limit) vm.setInsertType(i / Mixer.INSERTS, i % Mixer.INSERTS, EffectType.ECHO)
        assertEquals(limit, vm.project.value.mixer.longFxCount(EffectType.ECHO))
        assertNull(vm.notice.value)
        return vm
    }

    private fun RhythmViewModel.echoes() = project.value.mixer.longFxCount(EffectType.ECHO)

    @Test
    fun aThirteenthEchoIsRefusedWithTheWarning() {
        val vm = fullOfEchoes()
        vm.setInsertType(2, 0, EffectType.PING_PONG)  // the same pool
        assertEquals(EffectType.NONE, vm.project.value.mixer.channels[2].inserts[0].effect)
        assertEquals(longFxLimitNotice(EffectType.ECHO), vm.notice.value)
        // Another pool is still free; replacing an echo by a reverb frees one.
        vm.dismissNotice()
        vm.setInsertType(0, 0, EffectType.REVERB)
        assertNull(vm.notice.value)
        assertEquals(limit - 1, vm.echoes())
        vm.setInsertType(2, 0, EffectType.ECHO)
        assertEquals(limit, vm.echoes())
    }

    @Test
    fun switchingAnEchoBackOnPastTheLimitIsRefused() {
        val vm = fullOfEchoes()
        vm.setInsertBypass(0, 0, true)
        vm.setInsertType(2, 0, EffectType.ECHO)  // its place taken while it is off
        assertEquals(limit, vm.echoes())
        assertNull(vm.notice.value)
        vm.setInsertBypass(0, 0, false)
        assertTrue(vm.project.value.mixer.channels[0].inserts[0].bypass)
        assertEquals(longFxLimitNotice(EffectType.ECHO), vm.notice.value)
        // Switching another one off first: then it comes back on.
        vm.setInsertBypass(1, 0, true)
        vm.setInsertBypass(0, 0, false)
        assertFalse(vm.project.value.mixer.channels[0].inserts[0].bypass)
    }

    @Test
    fun aVibeWithAnEchoIsRefusedWhenTheEchoesAreAllTaken() {
        val vm = fullOfEchoes()
        val before = vm.project.value.mixer.channels[2]
        vm.applyFx(2, "dub_deep")
        assertEquals(before, vm.project.value.mixer.channels[2])
        assertEquals(longFxLimitNotice(EffectType.ECHO), vm.notice.value)
        // A vibe without long effects still goes on.
        vm.dismissNotice()
        vm.applyFx(2, "radio")
        assertNull(vm.notice.value)
        assertTrue(vm.project.value.mixer.channels[2].inserts.any { it.effect != EffectType.NONE })
    }

    @Test
    fun duplicatingATrackWithEchoesPastTheLimitIsRefused() {
        val vm = RhythmViewModel(app)
        vm.addTrack()
        val id = vm.project.value.tracks.last().id
        val ch = InstrumentTrack.mixerChannel(id)
        for (slot in 0 until 6) vm.setInsertType(ch, slot, EffectType.ECHO)
        for (slot in 0 until 5) vm.setInsertType(0, slot, EffectType.ECHO)
        assertEquals(11, vm.echoes())
        val tracks = vm.project.value.tracks.size
        vm.duplicateTrack(id)
        assertEquals(tracks, vm.project.value.tracks.size)
        assertEquals(longFxLimitNotice(EffectType.ECHO), vm.notice.value)
    }

    @Test
    fun aProjectOverTheLimitSaysSoWhenItOpens() {
        // Saved by an older version, or edited by hand: one echo too many.
        val p = RhythmViewModel.starterProject()
        val echo = Insert.of(EffectType.ECHO)
        val over = p.copy(
            mixer = p.mixer.copy(
                channels = p.mixer.channels.mapIndexed { i, c ->
                    when (i) {
                        0 -> c.copy(inserts = List(Mixer.INSERTS) { echo })
                        1 -> c.copy(inserts = List(limit + 1 - Mixer.INSERTS) { echo } + List(2 * Mixer.INSERTS - limit - 1) { Insert() })
                        else -> c
                    }
                },
            ),
        )
        val id = com.pocketstudio.app.projects.CurrentProject.get(app)
        com.pocketstudio.model.ProjectStore(File(app.filesDir, "projects")).save(id, over)
        val vm = RhythmViewModel(app)
        assertEquals(limit + 1, vm.echoes())
        assertEquals(longFxLimitNotice(EffectType.ECHO), vm.notice.value)
    }

    @Test
    fun stripsNoLongerUsedAreEmptiedAndEveryOtherOneIsSent() {
        val p = RhythmViewModel.starterProject()
        val withTrack = p.addTrack()!!
        val id = withTrack.tracks.last().id
        val ch = InstrumentTrack.mixerChannel(id)
        assertTrue(ch in RhythmViewModel.usedStrips(withTrack))
        assertFalse(ch in RhythmViewModel.stripsToClear(withTrack))
        // The track deleted (or undone, or another project opened): its strip is emptied in the
        // engine, its echoes given back.
        val gone = withTrack.removeTrack(id)
        assertTrue(ch in RhythmViewModel.stripsToClear(gone))
        // Every strip is either sent or emptied; the master's index apart.
        val used = RhythmViewModel.usedStrips(gone)
        val cleared = RhythmViewModel.stripsToClear(gone)
        assertEquals((0 until NativeEngine.MIXER_CHANNELS).toSet() - RhythmViewModel.MASTER, used + cleared)
        assertTrue(used.intersect(cleared.toSet()).isEmpty())
        // Audio tracks and pads with effects of their own are in use; a pad without is emptied.
        assertTrue(AudioTrack.mixerChannel(0) in RhythmViewModel.stripsToClear(gone) == gone.audioTracks.none { it.id == 0 })
        assertTrue(Mixer.padChannel(2) in RhythmViewModel.stripsToClear(gone))
    }

    @Test
    fun onlyTheSettingsOfSlotsHeardAreSent() {
        val echo = Insert.of(EffectType.ECHO)
        val inserts = listOf(echo, Insert(), echo.copy(bypass = true), Insert.of(EffectType.EQ)) + List(Mixer.INSERTS - 4) { Insert() }
        val types = mutableListOf<Pair<Int, Int>>()
        val params = mutableListOf<Int>()
        RhythmViewModel.insertCommands(inserts, { slot, type -> types += slot to type }) { slot, _, _ -> params += slot }
        // Every slot gets its type (a switched-off effect: an empty slot)...
        assertEquals((0 until Mixer.INSERTS).map { it to (if (it == 0) EffectType.ECHO.ordinal else if (it == 3) EffectType.EQ.ordinal else 0) }, types)
        // ... and only the effects heard their settings (each its own: 4 here), not the 6 empty or switched-off slots.
        assertEquals(List(EffectType.ECHO.defaults.size) { 0 } + List(EffectType.EQ.defaults.size) { 3 }, params)
    }
}
