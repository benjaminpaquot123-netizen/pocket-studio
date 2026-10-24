package com.pocketstudio.app

import android.app.Application
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.library.SoundLibraryViewModel
import com.pocketstudio.app.projects.CurrentProject
import com.pocketstudio.app.projects.ProjectsViewModel
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.model.Project
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * « Écoute en direct » on a device (docs/son/BANC.md): the assistant's jump up song, made as « Partir
 * d'un style » makes it, opened, then played as « Ton morceau est prêt ! »'s ▶ plays it, for 60 s
 * through the real audio stream. Benjamin's report on the preview: the music faded out, then went
 * silent, about 15 s in (the live loudness gain fell without end). Each second's output peak must
 * stay well above silence while the transport runs.
 */
@RunWith(AndroidJUnit4::class)
class LivePlaybackTest {
    @get:Rule
    val mic: GrantPermissionRule = GrantPermissionRule.grant(android.Manifest.permission.RECORD_AUDIO)

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val app = instrumentation.targetContext.applicationContext as Application

    private fun waitFor(what: String, seconds: Int, condition: () -> Boolean) {
        val until = System.currentTimeMillis() + seconds * 1000L
        while (!condition()) {
            check(System.currentTimeMillis() < until) { "waited too long for $what" }
            Thread.sleep(50)
        }
    }

    /** Highest output sample since the last look (the app's own poll may have taken some: both are read). */
    private fun peakNow(): Float = maxOf(NativeEngine.streamInfo().peak, Metrics.streamInfo.value.peak)

    @Test
    fun anAssistantSongKeepsPlayingFor60Seconds() {
        assertTrue("native engine must load", NativeEngine.isAvailable)
        // The song, made and saved as the app makes it, then the current project.
        var made: Pair<Project, Map<String, Pcm>>? = null
        lateinit var library: SoundLibraryViewModel
        instrumentation.runOnMainSync {
            library = SoundLibraryViewModel(app)
            library.quickCreate("jump_up") { project, sounds, _ -> made = project to sounds }
        }
        waitFor("the assistant's song", 180) { made != null || library.quick.value.error != null }
        val (song, sounds) = checkNotNull(made) { "jump_up: ${library.quick.value.error}" }
        var id: String? = null
        instrumentation.runOnMainSync {
            library.quickDone()
            ProjectsViewModel(app).create(song, sounds) { id = it }
        }
        waitFor("the project", 60) { id != null }
        // The project open before stays the current one afterwards: the next test (SmokeTest opens
        // « Continuer ») must find its small starter beat, not a 3-minute song to export.
        val previous = CurrentProject.get(app)
        CurrentProject.set(app, id!!)
        try {
            playFor60Seconds(song)
        } finally {
            CurrentProject.set(app, previous)
        }
    }

    private fun playFor60Seconds(song: Project) {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            waitFor("the audio stream", 15) { NativeEngine.streamInfo().let { it.running && it.callbacks > 0 } }
            lateinit var rhythm: RhythmViewModel
            scenario.onActivity { rhythm = androidx.lifecycle.ViewModelProvider(it)[RhythmViewModel::class.java] }
            waitFor("the song opened", 30) { rhythm.project.value.song.size == song.song.size }
            Thread.sleep(3000)  // its sounds loading, as when the card shows
            // ▶ as the card plays it.
            instrumentation.runOnMainSync { rhythm.toggleSong() }
            waitFor("the transport", 10) { NativeEngine.currentStep() >= 0 }
            val peaks = ArrayList<Float>()
            peakNow()
            repeat(60) {
                var peak = 0f
                repeat(10) {
                    Thread.sleep(100)
                    peak = maxOf(peak, peakNow())
                }
                peaks += peak
            }
            val playing = NativeEngine.currentStep() >= 0
            instrumentation.runOnMainSync { if (rhythm.transport.value.playing) rhythm.toggleSong() }
            val log = peaks.mapIndexed { i, p -> "${i + 1}s:%.3f".format(p) }.joinToString(" ")
            android.util.Log.i("LivePlaybackTest", log)
            assertTrue("the song still plays after 60 s", playing)
            // Past its first seconds (an Intro may start soft), never near silence: -40 dBFS at least
            // every second (a silent beat lasts a third of a second at 174 BPM), and its last 10 s
            // as loud as a song is.
            val quiet = peaks.withIndex().filter { it.index >= 3 && it.value < 0.01f }
            assertTrue("near silence while playing at ${quiet.map { it.index + 1 }} s: $log", quiet.isEmpty())
            assertTrue("the last 10 s are loud: $log", peaks.takeLast(10).max() > 0.1f)
            assertTrue("no NaN in the output", !NativeEngine.streamInfo().nonFinite)
        }
    }
}
