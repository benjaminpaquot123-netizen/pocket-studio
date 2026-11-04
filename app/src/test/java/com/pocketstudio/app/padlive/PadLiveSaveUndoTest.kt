package com.pocketstudio.app.padlive

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.projects.projectCatalog
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.engine.CommandSink
import com.pocketstudio.engine.ExportResult
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.model.LiveClip
import com.pocketstudio.model.PadLive
import com.pocketstudio.model.PadLiveSet
import com.pocketstudio.model.PadMode
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.io.File
import java.time.Duration

/**
 * Pad Live (n° 25, lot B1) through the ViewModel: a pack put on page B is one undo step, the pad
 * modes and groups too; touches follow the modes and the groups; everything is saved and read back
 * by a new ViewModel (the app reopened). No audio engine here: a sink takes its place.
 */
@RunWith(RobolectricTestRunner::class)
class PadLiveSaveUndoTest {
    private val app: Application = ApplicationProvider.getApplicationContext()

    /** In the engine's place: sounds get handles, the pads switched are kept. */
    private class PadSink : CommandSink {
        private var next = 1L
        override fun post(target: Long, type: Int, value: Float, a: Int, b: Int) = Unit
        override fun scheduleFrom(target: Long, frame: Long) = Unit
        override fun install(target: Long, kind: CommandSink.Install, index: Int, sample: Long) = Unit
        @Synchronized override fun sampleCreate(data: FloatArray, channels: Int, sampleRate: Int): Long = next++
        override fun sampleFrames(handle: Long): Long = 48_000L
        override fun sampleRelease(handle: Long) = Unit
        @Synchronized override fun exportCreate(): Long = next++
        override fun exportRender(target: Long, path: String, frames: Long, tailFrames: Long, fadeOutFrames: Long, autoTail: Boolean, floatWav: Boolean, sampleRate: Int, bits: Int) = ExportResult.Ok
        override fun exportRenderFloat(target: Long, frames: Long, tailFrames: Long): FloatArray = FloatArray(0)
        override fun exportDestroy(target: Long) = Unit
    }

    private fun tone(seconds: Double) = Pcm(FloatArray((seconds * 48_000).toInt() * 2) { 0.2f * kotlin.math.sin(it * 0.01f) }, 2, 48_000)

    /** A pack of two sounds: a loop on the first pad, a « tenir » hit on the third row. */
    private val maker = PackMaker { pack, bpm, _ ->
        listOf(
            PackSound(0, tone(2.0), "Batterie") { file -> LiveClip(file, "Batterie", 4, bpm = bpm) },
            PackSound(PadLive.HITS_FROM, tone(0.3), pack.hits.last().name) { file -> PadLive.hitClip(pack.hits.last(), file) },
        )
    }

    private val toggled = ArrayList<Int>()

    /** The open project made a « Live » one, as [PadLive.newProject] makes it. */
    private fun asLive(p: com.pocketstudio.model.Project) = PadLive.newProject().let { n -> p.copy(name = n.name, tempoBpm = n.tempoBpm, live = n.live, padLive = n.padLive) }

    private fun liveVm(): RhythmViewModel = RhythmViewModel(app).also { vm ->
        vm.livePadsSoundingForTest = emptySet()
        vm.padLive.makerForTest = maker
        vm.padLive.toggleForTest = { toggled += it }
    }

    private fun waitFor(what: String, done: () -> Boolean) {
        val until = System.currentTimeMillis() + 20_000
        while (!done()) {
            assertTrue("never: $what", System.currentTimeMillis() < until)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(50))
            Thread.sleep(5)
        }
    }

    @Before
    fun setUp() {
        File(app.filesDir, "projects").deleteRecursively()
        NativeEngine.sink = PadSink()
    }

    @After
    fun tearDown() {
        NativeEngine.sink = null
    }

    /**
     * Audit ultra round 1, n° 5 (four testers met pages of « + »): in the home's « Live » project a page
     * shown empty for the first time gets its pack on its own, not an undo step; a page the user filled,
     * or one that had its pack once, is left as it is; another project's pages are never filled.
     */
    @Test
    fun anEmptyPageShownFirstGetsItsPackOnItsOwn() {
        val vm = liveVm()
        vm.session.edit { asLive(it) }
        val undoable = vm.session.canUndo.value
        vm.padLive.setPage(2)
        vm.padLive.fillShownPage { emptyMap() }
        waitFor("page C's pack") { vm.project.value.live.at(32) != null && vm.padLive.busy.value == null }
        assertEquals(PadLive.FIRST_PACKS[2], vm.project.value.padLive.packOf(2))
        assertEquals("not an undo step", undoable, vm.session.canUndo.value)
        // Emptied by the user: shown again, not filled again.
        vm.clearLive(32)
        vm.clearLive(PadLive.pad(2, PadLive.HITS_FROM))
        vm.padLive.setPage(0)
        vm.padLive.setPage(2)
        vm.padLive.fillShownPage { emptyMap() }
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(200))
        assertNull(vm.project.value.live.at(32))
        // Page A is the ready-to-play set's; a project that is not the home's « Live » keeps its pages empty.
        vm.padLive.setPage(0)
        vm.padLive.fillShownPage { emptyMap() }
        assertNull(vm.padLive.busy.value)
        vm.session.edit { it.copy(padLive = it.padLive.copy(home = false)) }
        vm.padLive.setPage(3)
        vm.padLive.fillShownPage { emptyMap() }
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(200))
        assertNull(vm.project.value.live.at(48))
        assertNull(vm.padLive.busy.value)
    }

    @Test
    fun aPackOnPageBIsOneUndoStepAndEverythingIsSavedAndReadBack() {
        val vm = liveVm()
        vm.session.edit { asLive(it) }
        vm.padLive.setPage(1)
        vm.padLive.loadPack("trap") { emptyMap() }
        waitFor("the pack on page B") { vm.project.value.live.at(16) != null && vm.padLive.busy.value == null }
        val p = vm.project.value
        // Fix 2, n° 6: the pack is page B's, never page A's.
        assertEquals("trap", p.padLive.packOf(1))
        assertEquals("trap", shownPack(p))
        assertEquals("", p.padLive.packOf(0))
        assertTrue(shownPack(p.copy(padLive = p.padLive.copy(page = 0))) != "trap")
        assertEquals(1, p.padLive.page)
        assertNull("page A untouched", p.live.at(0))
        val hit = p.live.at(PadLive.pad(1, PadLive.HITS_FROM))!!
        assertEquals(PadMode.HOLD, PadLive.modeOf(hit))  // Trap's siren: « tenir »
        assertTrue(File(File(File(app.filesDir, "projects"), vm.projectId), "samples/${hit.file}").isFile)
        // A mode and a group: one step each.
        vm.padLive.setMode(16, PadMode.SHOT)
        vm.padLive.setGroup(16, 2)
        assertEquals(2, vm.project.value.live.at(16)?.group)
        vm.undo()
        assertEquals(0, vm.project.value.live.at(16)?.group)
        vm.undo()
        assertEquals(PadMode.LOOP, PadLive.modeOf(vm.project.value.live.at(16)!!))
        vm.undo()
        assertNull("the pack undone", vm.project.value.live.at(16))
        assertEquals("the page shown stays", 1, vm.project.value.padLive.page)
        vm.session.redo()
        assertEquals("Batterie", vm.project.value.live.at(16)?.name)
        vm.padLive.setGroup(16, 3)
        // Saved, then read back as the app reopened finds it.
        assertTrue(runBlocking { vm.flushNow() })
        val again = liveVm()
        val q = again.project.value
        assertTrue(q.padLive.home)
        assertEquals(1, q.padLive.page)
        assertEquals("trap", q.padLive.packOf(1))
        assertEquals(3, q.live.at(16)?.group)
        assertEquals(PadMode.HOLD, PadLive.modeOf(q.live.at(PadLive.pad(1, PadLive.HITS_FROM))!!))
        assertTrue("listed as the « Live » project", projectCatalog(app).info(again.projectId)!!.padLive)
    }

    @Test
    fun touchesFollowTheModesAndTheGroups() {
        val vm = liveVm()
        vm.session.edit {
            asLive(it).let { p ->
                p.copy(
                    live = p.live
                        .withAt(0, LiveClip("a.wav", "Charley", 1, group = 1))
                        .withAt(1, LiveClip("b.wav", "Charley ouvert", 1, group = 1))
                        .withAt(20, PadLive.withMode(LiveClip("c.wav", "Sirène", 4), PadMode.HOLD))
                        .withAt(21, LiveClip("d.wav", "Clap", 1, oneShot = true)),
                    padLive = PadLiveSet(home = true),
                )
            }
        }
        // A loop: on the touch only.
        vm.padLive.touch(0, true)
        vm.padLive.touch(0, false)
        assertEquals(listOf(0), toggled)
        // Pad 1 starts while pad 0 sounds: pad 0 stops first (same group).
        vm.livePadsSoundingForTest = setOf(0)
        toggled.clear()
        vm.padLive.touch(1, true)
        assertEquals(listOf(0, 1), toggled)
        // « Tenir » on page B: on with the finger, off when it leaves.
        vm.livePadsSoundingForTest = emptySet()
        toggled.clear()
        vm.padLive.touch(20, true)
        vm.livePadsSoundingForTest = setOf(20)
        vm.padLive.touch(20, false)
        assertEquals(listOf(20, 20), toggled)
        // A hit, again and again: each touch.
        vm.livePadsSoundingForTest = emptySet()
        toggled.clear()
        vm.padLive.touch(21, true)
        vm.padLive.touch(21, false)
        vm.padLive.touch(21, true)
        assertEquals(listOf(21, 21), toggled)
        // An empty pad: nothing.
        toggled.clear()
        vm.padLive.touch(40, true)
        assertTrue(toggled.isEmpty())
        // Opened from the home screen: the page's first hit sounds once (page A has none: page B's).
        vm.padLive.setPage(1)
        vm.padLive.welcome()
        waitFor("the welcome hit") { toggled.isNotEmpty() }
        assertEquals(listOf(21), toggled)
        assertFalse(vm.session.canUndo.value && vm.project.value.padLive.page != 1)
    }
}
