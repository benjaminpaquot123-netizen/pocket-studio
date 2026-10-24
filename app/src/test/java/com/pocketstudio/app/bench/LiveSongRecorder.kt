package com.pocketstudio.app.bench

import android.app.Application
import android.os.Looper
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.library.SoundLibraryViewModel
import com.pocketstudio.app.projects.CurrentProject
import com.pocketstudio.app.projects.ProjectsViewModel
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.model.Project
import com.pocketstudio.model.StarterWizard
import com.pocketstudio.model.partSteps
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.io.File
import java.time.Duration

/**
 * The live bench's recorder (scripts/live-bench.sh): for each style, the assistant's one-touch
 * song opened as the app opens it, then ▶ pressed as « Ton morceau est prêt ! » does
 * ([RhythmViewModel.toggleSong]), the next parts staged as the app stages them after each scene
 * swap of the engine. `<style>.pslive` holds every command the LIVE engine got, with the app's
 * pauses (`WAIT ms`) and the swaps it waited for (`WAIT swaps`): `ps_live` plays it through the
 * engine as the phone's stream does.
 *
 * Off by default; `-Pbench.live=<dir>` turns it on (`-Pbench.render=<ps_render>`, `-Pbench.styles`).
 */
@RunWith(RobolectricTestRunner::class)
class LiveSongRecorder {
    private val app = ApplicationProvider.getApplicationContext<Application>()

    private fun waitFor(what: String, seconds: Int = 60, condition: () -> Boolean) {
        val until = System.currentTimeMillis() + seconds * 1000L
        while (!condition()) {
            check(System.currentTimeMillis() < until) { "waited too long for $what" }
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(5)
        }
    }

    @Test
    fun recordsTheLivePlaybackOfEveryStyle() {
        val outDir = System.getProperty("ps.bench.live")?.takeIf { it.isNotBlank() }
        assumeTrue("live bench: run with -Pbench.live=<dir> (scripts/live-bench.sh)", outDir != null)
        val out = File(outDir!!).apply { mkdirs() }
        val renderer = System.getProperty("ps.bench.render")?.takeIf { it.isNotBlank() }?.let(::File)
        val only = System.getProperty("ps.bench.styles")?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }?.toSet().orEmpty()
        val styles = StarterWizard.STYLES.filter { only.isEmpty() || it.id in only }
        // How long ps_live will play each song: the parts staged up to there.
        val seconds = System.getProperty("ps.bench.seconds")?.toIntOrNull() ?: 90
        assumeTrue("no style matches ${only.joinToString()}", styles.isNotEmpty())

        val sink = RecordingSink(File(out, "samples"), renderer)
        NativeEngine.sink = sink
        try {
            val library = SoundLibraryViewModel(app)
            val projects = ProjectsViewModel(app)
            for (style in styles) {
                var made: Pair<Project, Map<String, Pcm>>? = null
                library.quickCreate(style.id) { project, sounds, _ -> made = project to sounds }
                waitFor("the song of ${style.id}", 120) { made != null || library.quick.value.error != null }
                val (song, sounds) = checkNotNull(made) { "${style.id}: ${library.quick.value.error}" }
                library.quickDone()
                var id: String? = null
                projects.create(song, sounds) { id = it }
                waitFor("the project of ${style.id}") { id != null }
                CurrentProject.set(app, id!!)

                // From here, what the live engine hears: the project opened, its sounds loaded.
                val live = StringBuilder()
                sink.live = live
                sink.swaps = 0
                sink.mark("PSCMD 1\nRATE 48000\nMETA style ${style.id}")
                // In a store of its own: cleared after its song (its coroutines and its sounds let go).
                val store = ViewModelStore()
                val vm = ViewModelProvider(store, ViewModelProvider.AndroidViewModelFactory.getInstance(app))[RhythmViewModel::class.java]
                var loaded = false
                CoroutineScope(Dispatchers.Main).launch { vm.awaitSoundsLoadedForTest(); loaded = true }
                waitFor("the sounds of ${style.id}") { loaded }
                val p = vm.project.value
                sink.mark("SONG bpm=${p.tempoBpm} parts=${p.song.size}")
                // The sheet shows, a moment passes, then ▶.
                sink.mark("WAIT ms 1000")
                vm.toggleSong()
                // The app's own pause before it stages the second part.
                sink.mark("WAIT ms 150")
                shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(150))
                // Each swap the engine makes: the 40 ms watch sees it and stages the part after.
                val sixteenth = 15.0 / p.tempoBpm
                var steps = 0L
                for ((i, part) in p.song.withIndex()) {
                    steps += p.partSteps(part)
                    if (steps * sixteenth > seconds + 5 || i == p.song.size - 1) break
                    sink.mark("WAIT swaps ${i + 1}")
                    sink.mark("WAIT ms 40")
                    sink.swaps = i + 1
                    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(40))
                }
                sink.live = null
                File(out, "${style.id}.pslive").writeText(live.toString())
                if (vm.transport.value.playing) vm.toggleSong()
                shadowOf(Looper.getMainLooper()).idle()
                store.clear()
                println("live bench: ${style.id} recorded (${live.length / 1024} KB)")
            }
        } finally {
            NativeEngine.sink = null
        }
    }
}
