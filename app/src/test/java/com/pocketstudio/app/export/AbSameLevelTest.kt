package com.pocketstudio.app.export

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.mixer.MasterProController
import com.pocketstudio.engine.CommandSink
import com.pocketstudio.engine.ExportResult
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.model.EffectType
import com.pocketstudio.model.MasterPro
import com.pocketstudio.model.Mixer
import com.pocketstudio.model.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Lot E (« Master pro », §1 bis point 2): « A/B au même volume » as the app drives it. Listening to B
 * (without the master) or to the reference turns the matching on first (the engine measures both
 * sides and brings them to the quietest one's loudness: the host test
 * mp_ab_and_reference_at_the_same_loudness measures it on the output, within 0.5 LU); leaving the
 * panel puts the master back as always. The chain button places EQ, glue, width and saturation in the
 * master's free slots without touching the effects already there, and turns the bass mono.
 */
@RunWith(RobolectricTestRunner::class)
class AbSameLevelTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()

    private class Sink : CommandSink {
        val pro = ArrayList<Pair<Int, Float>>()
        override fun post(target: Long, type: Int, value: Float, a: Int, b: Int) {
            if (type == 190) pro += a to value
        }
        override fun scheduleFrom(target: Long, frame: Long) = Unit
        override fun install(target: Long, kind: CommandSink.Install, index: Int, sample: Long) = Unit
        override fun sampleCreate(data: FloatArray, channels: Int, sampleRate: Int): Long = 1
        override fun sampleFrames(handle: Long): Long = 0
        override fun sampleRelease(handle: Long) = Unit
        override fun exportCreate(): Long = 1
        override fun exportRender(target: Long, path: String, frames: Long, tailFrames: Long, fadeOutFrames: Long, autoTail: Boolean, floatWav: Boolean, sampleRate: Int, bits: Int) =
            ExportResult.Cancelled
        override fun exportRenderFloat(target: Long, frames: Long, tailFrames: Long): FloatArray = FloatArray(0)
        override fun exportDestroy(target: Long) = Unit
    }

    private val sink = Sink()
    private val project = MutableStateFlow(Project())
    private val inserts = ArrayList<Triple<Int, Int, EffectType>>()
    private var monoBass = 0f

    private fun controller() = MasterProController(
        CoroutineScope(Dispatchers.Unconfined), app, project,
        edit = { _, f -> project.value = project.value.copy(mixer = project.value.mixer.copy(pro = f(project.value.mixer.pro ?: MasterPro()))) },
        sync = {}, setInsertType = { ch, slot, t -> inserts += Triple(ch, slot, t) }, setMonoBass = { monoBass = it }, projectId = { "ab-test" },
    )

    @Before
    fun setUp() {
        NativeEngine.sink = sink
    }

    @After
    fun tearDown() {
        NativeEngine.sink = null
    }

    @Test
    fun listeningWithoutTheMasterMatchesTheLoudnessFirstAndLeavingPutsItBack() {
        val c = controller()
        c.setListen(NativeEngine.LISTEN_WITHOUT)
        // ProArmed (1) on before ProListen (0) moves away from the master.
        assertEquals(listOf(1 to 1f, 0 to 1f), sink.pro)
        assertTrue(c.live.value.compare)
        assertEquals(NativeEngine.LISTEN_WITHOUT, c.live.value.listen)
        sink.pro.clear()
        c.leave()
        assertEquals(listOf(1 to 0f, 0 to 0f), sink.pro)
        assertEquals(NativeEngine.LISTEN_MASTER, c.live.value.listen)
        // Nothing to undo after that: leaving again sends nothing.
        sink.pro.clear()
        c.leave()
        assertTrue(sink.pro.isEmpty())
    }

    @Test
    fun theProChainGoesInTheFreeSlotsInOrder() {
        // An effect already on the master (slot 0, a compressor: the glue is there).
        project.value = project.value.copy(mixer = project.value.mixer.copy(master = project.value.mixer.master.mapIndexed { i, ins ->
            if (i == 0) ins.copy(type = EffectType.COMPRESSOR.ordinal) else ins
        }))
        controller().putChain()
        assertEquals(listOf(Triple(Mixer.MASTER, 1, EffectType.PARAM_EQ), Triple(Mixer.MASTER, 2, EffectType.WIDENER), Triple(Mixer.MASTER, 3, EffectType.SATURATION)), inserts)
        assertEquals(MasterProController.DEFAULT_MONO_BASS, monoBass)
    }

    @Test
    fun theTargetAndCeilingAreKeptSanitized() {
        val c = controller()
        c.setTarget(-30f)
        assertEquals(MasterPro.MIN_LUFS, project.value.mixer.pro!!.targetLufs)
        c.setCeiling(-0.5f)
        assertEquals(-0.5f, project.value.mixer.pro!!.ceilingDb)
        assertEquals(-0.3f, MasterPro(ceilingDb = -0.2f).sanitized().ceilingDb)
        c.setTarget(null)
        assertEquals(null, project.value.mixer.pro!!.targetLufs)
    }
}
