package com.pocketstudio.app.human

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.projects.CurrentProject
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.model.Project
import com.pocketstudio.model.ProjectStore
import com.pocketstudio.model.Remix
import com.pocketstudio.model.SongPart
import com.pocketstudio.model.switchScene
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File
import java.time.Duration

/**
 * Chantier 10, fresh review (grave): « Allonger le morceau » touched while the song's last part plays. The engine
 * already holds the song's end for that part's bar line; the parts added must play on, not stop at the old end.
 * The app's view model and a scripted engine (Tour5AppProof3Test.SongEngine), no bench.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = HumanTester.PHONE)
class RemixLengthenWhilePlayingTest {
    @get:Rule
    val pristine = BenchPristineRule()

    private val app: Application get() = ApplicationProvider.getApplicationContext()
    private val stores = ArrayList<androidx.lifecycle.ViewModelStore>()

    @After
    fun clear() {
        stores.forEach { it.clear() }
        com.pocketstudio.app.rhythm.ClosingSaves.awaitIdleBlocking(15_000)
        NativeEngine.sink = null
        CurrentProject.set(app, RhythmViewModel.PROJECT_ID)
    }

    private fun idle(ms: Long = 20) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))

    private fun open(p: Project): RhythmViewModel {
        val id = "remix10-${System.nanoTime()}"
        ProjectStore(File(app.filesDir, "projects")).save(id, p)
        CurrentProject.set(app, id)
        val store = androidx.lifecycle.ViewModelStore().also { stores += it }
        val factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T = RhythmViewModel(app) as T
        }
        val vm = androidx.lifecycle.ViewModelProvider(store, factory)[RhythmViewModel::class.java]
        val until = System.currentTimeMillis() + 10_000
        while (vm.project.value.song.size != p.song.size) {
            assertTrue("the project never opened", System.currentTimeMillis() < until)
            idle(); Thread.sleep(5)
        }
        return vm
    }

    @Test
    fun partsAddedWhileTheLastPartPlaysPlayOn() {
        val engine = Tour5AppProof3Test.SongEngine()
        NativeEngine.sink = engine
        // Two parts of one bar (2 s each at 120 BPM); a song of 10 s from the start: 5 bars, 3 missing.
        val p = Project().let { it.copy(drums = it.drums.toggle(0, 0)) }.switchScene(1)
            .let { it.copy(drums = it.drums.toggle(0, 4)) }.switchScene(0)
            .copy(
                song = listOf(SongPart(0), SongPart(1)), tempoBpm = 120.0,
                remix = Remix(setId = "absent", name = "Boys", songBpm = 120.0, seconds = 10.0, placed = true),
            )
        val vm = open(p)
        val poll = vm.viewModelScope.launch { vm.pollPosition() }
        try { check(vm, engine) } finally { poll.cancel() }
    }

    private fun check(vm: RhythmViewModel, engine: Tour5AppProof3Test.SongEngine) {
        vm.togglePlay()
        var waited = 0L
        while (vm.sessionUi.value.songIndex != 1 && waited < 10_000) { idle(50); waited += 50 }
        assertEquals("the last part plays ($waited ms)", 1, vm.sessionUi.value.songIndex)
        vm.lengthenSongForRemix()
        idle(50)
        assertEquals("the song was lengthened: ${vm.project.value.song}", 3, vm.project.value.song.size)
        waited = 0L
        while (vm.transport.value.playing && vm.sessionUi.value.songIndex < 2 && waited < 6_000) { idle(50); waited += 50 }
        assertFalse("the engine stopped the song at its old end ($waited ms after « Allonger »)", engine.stoppedBySongEnd)
        assertEquals("the part added plays after the last one", 2, vm.sessionUi.value.songIndex)
    }
}
