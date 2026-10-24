package com.pocketstudio.app.bench

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.library.SoundLibraryViewModel
import com.pocketstudio.app.projects.CurrentProject
import com.pocketstudio.app.projects.ProjectsViewModel
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.engine.ExportResult
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.engine.OfflineExport
import com.pocketstudio.model.Project
import com.pocketstudio.model.InstrumentTrack
import com.pocketstudio.model.StarterWizard
import com.pocketstudio.model.leadTrackId
import com.pocketstudio.model.allScenes
import com.pocketstudio.model.partSteps
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.io.File

/**
 * The song bench's recorder (docs/plan/son.md § 2, scripts/song-bench.sh): for each style of the
 * assistant, the song « Partir d'un style » makes in one touch (its advised choices, the built-in
 * sounds: no pack installed), opened as the app opens it, then exported as « Exporter › Morceau »
 * does, with a [RecordingSink] in place of the native engine: `<style>.pscmd` holds the engine
 * commands of that export, which `ps_render` replays on Linux.
 *
 * Off by default; `-Pbench.out=<dir>` turns it on (`-Pbench.render=<ps_render>` renders the kit
 * voices the assistant turns into sounds; `-Pbench.styles=a,b` keeps a few styles).
 */
@RunWith(RobolectricTestRunner::class)
class SongBenchExport {
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
    fun recordsTheExportOfEveryStyle() {
        val outDir = System.getProperty("ps.bench.out")?.takeIf { it.isNotBlank() }
        assumeTrue("song bench: run with -Pbench.out=<dir> (scripts/song-bench.sh)", outDir != null)
        val out = File(outDir!!).apply { mkdirs() }
        val renderer = System.getProperty("ps.bench.render")?.takeIf { it.isNotBlank() }?.let(::File)
        val only = System.getProperty("ps.bench.styles")?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }?.toSet().orEmpty()
        val styles = StarterWizard.STYLES.filter { only.isEmpty() || it.id in only }
        assumeTrue("no style matches ${only.joinToString()}", styles.isNotEmpty())

        val sink = RecordingSink(File(out, "samples"), renderer)
        NativeEngine.sink = sink
        try {
            val library = SoundLibraryViewModel(app)
            val projects = ProjectsViewModel(app)
            for (style in styles) {
                // The one-touch song, as the app makes it (kit voices rendered by ps_render).
                var made: Pair<Project, Map<String, Pcm>>? = null
                library.quickCreate(style.id) { project, sounds, _ -> made = project to sounds }
                waitFor("the song of ${style.id}", 120) { made != null || library.quick.value.error != null }
                val (song, sounds) = checkNotNull(made) { "${style.id}: ${library.quick.value.error}" }
                library.quickDone()
                // Its live pads are not prepared: they never play in a song's export.
                var id: String? = null
                projects.create(song.copy(livePadsPending = false), sounds) { id = it }
                waitFor("the project of ${style.id}") { id != null }
                CurrentProject.set(app, id!!)

                // Opened as the app opens it, then exported as « Exporter › Morceau » does.
                val vm = RhythmViewModel(app)
                val p = vm.project.value
                assertEquals(style.name, p.name.substringBefore(" ("))
                sink.nextHeader = header(style, p)
                val job = OfflineExport()
                var result: ExportResult? = null
                CoroutineScope(Dispatchers.Main).launch { result = vm.renderSongForBench(job, File(out, "${style.id}.pscmd")) }
                waitFor("the export of ${style.id}") { result != null }
                job.close()
                assertEquals(ExportResult.Ok, result)
                println("bench: ${style.id} recorded (${File(out, "${style.id}.pscmd").length() / 1024} KB)")
            }
        } finally {
            NativeEngine.sink = null
        }
    }

    /**
     * What ps_measure needs: the style, the song's length and tempo, each part's first frame and its
     * section (a variant measured with its section: "Drop 2" with the Drop, "Montée 2" with the Montée).
     */
    private fun header(style: StarterWizard.Style, p: Project): List<String> {
        val sixteenth = 15.0 / p.tempoBpm * OfflineExport.SAMPLE_RATE
        var steps = 0L
        val numbered = Regex("""^(.+) \d+$""")
        val sections = p.song.mapIndexed { i, part ->
            val start = Math.round(steps * sixteenth)
            steps += p.partSteps(part)
            val label = p.allScenes.getOrNull(part.scene)?.label?.ifBlank { null }?.let { numbered.matchEntire(it)?.groupValues?.get(1) ?: it }
            "SECTION $i $start ${label ?: "Partie ${part.scene + 1}"}"
        }
        return listOf(
            "META style ${style.id}",
            "META name ${style.name}",
            "META family ${style.family}",
            "SONG frames=${Math.round(steps * sixteenth)} bpm=${p.tempoBpm}",
        ) + listOfNotNull(
            // S4: the lead's mixer channel, rendered alone by the bench (how its sound moves along the song).
            p.leadTrackId()?.let { "META lead ${InstrumentTrack.mixerChannel(it)}" },
        ) + sections
    }
}
