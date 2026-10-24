package com.pocketstudio.app.bench

import android.app.Application
import android.os.Looper
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.projects.CurrentProject
import com.pocketstudio.app.projects.projectCatalog
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.engine.ExportResult
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.engine.OfflineExport
import com.pocketstudio.model.ProjectJson
import com.pocketstudio.model.partSteps
import com.pocketstudio.model.sampleFiles
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.Duration
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Benjamin's real projects (docs/TESTS-REELS.md, « Projets réels »): every project in
 * `src/test/resources/real-projects/` (a folder holding `project.json` and `samples/`, or the
 * `.pocketstudio.zip` the app shares), and in `-Preal.projects=<dir>` (more, such as the full
 * versions downloaded by `scripts/real-projects.sh --fetch`), goes the app's way:
 *
 * 1. imported as « Mes projets › Importer » imports a .zip ([com.pocketstudio.model.ProjectCatalog.importZip]):
 *    the sounds it lacks are exactly the ones its `absents.txt` lists (left out of the repository);
 * 2. opened as the app opens it ([RhythmViewModel], its sounds loaded), then ▶ on the song
 *    ([RhythmViewModel.toggleSong]) with each next part staged after the engine's swap, as the
 *    live bench records it ([LiveSongRecorder]): 60 s of what the phone's engine gets, in `<name>.pslive`;
 * 3. exported as « Exporter › Morceau » exports it: `<name>.pscmd`.
 *
 * With `-Preal.live=<ps_live>` the 60 s are played by the real engine as the phone's stream drives
 * it (no fade, silence, click, NaN, heap call in the callback: ps_live's flags); with
 * `-Preal.render=<ps_render>` the export is rendered and checked (no NaN, true peak at most
 * −1 dBTP, its length, no 4 s of silence inside the song). `scripts/real-projects.sh` gives both.
 * Without them (the plain unit tests) steps 1 to 3 still run: the project imports, opens, plays
 * and exports without a crash. An empty folder passes.
 */
@RunWith(RobolectricTestRunner::class)
class RealProjectsTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()

    private fun waitFor(what: String, seconds: Int = 60, condition: () -> Boolean) {
        val until = System.currentTimeMillis() + seconds * 1000L
        while (!condition()) {
            check(System.currentTimeMillis() < until) { "waited too long for $what" }
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(5)
        }
    }

    /** A real project: its name, its .zip as the app shares it, the sounds left out on purpose. */
    class RealProject(val name: String, val zip: () -> ByteArray, val absent: Set<String>, val present: Set<String>, val projectJson: () -> String)

    @Test
    fun everyRealProjectImportsPlaysLiveAndExports() {
        val dirs = listOfNotNull(fixtures(), System.getProperty("ps.real.projects")?.takeIf { it.isNotBlank() }?.let(::File))
        val projects = dirs.flatMap { realProjects(it) }
        println("real projects: ${projects.size} (${projects.joinToString { it.name }}) in ${dirs.joinToString()}")
        check(projects)
    }

    /** The folder with nothing in it (or none at all): nothing to check, nothing fails. */
    @Test
    fun anEmptyFolderPasses() {
        val empty = File(app.cacheDir, "no-real-projects").apply { deleteRecursively(); mkdirs() }
        assertTrue(realProjects(empty).isEmpty())
        assertTrue(realProjects(File(empty, "missing")).isEmpty())
        check(emptyList())
    }

    private fun check(projects: List<RealProject>) {
        val out = (System.getProperty("ps.real.out")?.takeIf { it.isNotBlank() }?.let(::File) ?: File(app.cacheDir, "real-projects")).apply { mkdirs() }
        val renderer = System.getProperty("ps.real.render")?.takeIf { it.isNotBlank() }?.let(::File)
        val player = System.getProperty("ps.real.live")?.takeIf { it.isNotBlank() }?.let(::File)
        if (projects.isEmpty()) return
        val sink = RecordingSink(File(out, "samples"), renderer)
        NativeEngine.sink = sink
        val problems = ArrayList<String>()
        try {
            for (real in projects) {
                val problem = runCatching { checkOne(real, sink, out, renderer, player) }.exceptionOrNull()
                if (problem != null) problems += "${real.name}: ${problem.message ?: problem}".also { problem.printStackTrace() }
            }
        } finally {
            NativeEngine.sink = null
        }
        assertTrue("real projects:\n" + problems.joinToString("\n"), problems.isEmpty())
    }

    private fun checkOne(real: RealProject, sink: RecordingSink, out: File, renderer: File?, player: File?) {
        // 1. Its sounds: the ones it lacks are the ones said (a sound lost by accident fails).
        val read = ProjectJson.decode(real.projectJson())
        val missing = read.sampleFiles - real.present
        assertEquals("sounds missing from ${real.name} (absents.txt lists the ones left out)", real.absent, missing)
        val imported = projectCatalog(app).importZip(real.zip().inputStream())
        assertEquals(missing.size, imported.missingSounds)
        CurrentProject.set(app, imported.id)

        // 2. Opened, its sounds loaded, ▶ on the song: what the live engine gets for 60 s.
        val seconds = 60
        val live = StringBuilder()
        sink.live = live
        sink.swaps = 0
        sink.mark("PSCMD 1\nRATE 48000\nMETA real ${real.name}")
        val store = ViewModelStore()
        try {
            // This test's application (AndroidViewModelFactory.getInstance keeps the first test's one, whose
            // preferences name another project when the whole suite runs).
            val factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T = RhythmViewModel(app) as T
            }
            val vm = ViewModelProvider(store, factory)[RhythmViewModel::class.java]
            var loaded = false
            CoroutineScope(Dispatchers.Main).launch { vm.awaitSoundsLoadedForTest(); loaded = true }
            waitFor("the sounds of ${real.name}") { loaded }
            val p = vm.project.value
            assertEquals("${real.name}: the project opened is the one imported", read.song.size, p.song.size)
            assertEquals(read.name, p.name)
            assertTrue("${real.name} has a song", p.song.isNotEmpty())
            sink.mark("SONG bpm=${p.tempoBpm} parts=${p.song.size}")
            sink.mark("WAIT ms 1000")
            vm.toggleSong()
            assertTrue("${real.name} plays", vm.transport.value.playing)
            sink.mark("WAIT ms 150")
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(150))
            val sixteenth = 15.0 / p.tempoBpm
            var steps = 0L
            val parts = if (p.songLoops) generateSequence(0) { it + 1 }.map { p.song[it % p.song.size] } else p.song.asSequence()
            for ((i, part) in parts.withIndex()) {
                steps += p.partSteps(part)
                if (steps * sixteenth > seconds + 5 || (!p.songLoops && i == p.song.size - 1)) break
                sink.mark("WAIT swaps ${i + 1}")
                sink.mark("WAIT ms 40")
                sink.swaps = i + 1
                shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(40))
            }
            sink.live = null
            val pslive = File(out, "${real.name}.pslive").apply { writeText(live.toString()) }
            if (vm.transport.value.playing) vm.toggleSong()
            shadowOf(Looper.getMainLooper()).idle()

            // 3. « Exporter › Morceau ».
            val pscmd = File(out, "${real.name}.pscmd")
            val job = OfflineExport()
            var result: ExportResult? = null
            CoroutineScope(Dispatchers.Main).launch { result = vm.renderSongForBench(job, pscmd) }
            waitFor("the export of ${real.name}", 120) { result != null }
            job.close()
            assertEquals(ExportResult.Ok, result)
            val songSeconds = p.song.sumOf { p.partSteps(it).toLong() } * sixteenth
            println("real projects: ${real.name} imported (${imported.missingSounds} absent), ${p.song.size} parts, ${"%.0f".format(songSeconds)} s, " +
                "live ${live.length / 1024} KB, export ${pscmd.length() / 1024} KB")

            if (player != null) playLive(real.name, player, pslive, seconds)
            if (renderer != null) renderExport(real.name, renderer, pscmd, File(out, "${real.name}.wav"), songSeconds)
        } finally {
            sink.live = null
            store.clear()
            shadowOf(Looper.getMainLooper()).idle()
        }
    }

    /** ps_live plays the 60 s as the phone's stream does: any flag fails (its log beside the recording). */
    private fun playLive(name: String, player: File, pslive: File, seconds: Int) {
        val log = File(pslive.parentFile, "$name.live.txt")
        val process = ProcessBuilder(player.absolutePath, "--seconds", "$seconds", "--csv", File(pslive.parentFile, "$name.csv").absolutePath, pslive.absolutePath)
            .redirectErrorStream(true).redirectOutput(log).start()
        val status = process.waitFor()
        val lines = log.readLines()
        val flags = lines.filter { it.startsWith("FLAG ") }
        println("real projects: $name live: " + lines.lastOrNull())
        assertTrue("$name: ps_live failed ($status): ${lines.takeLast(5)}", status == 0 || status == 4)
        assertTrue("$name: heard live, ${flags.size} flag(s): ${flags.take(5)}", flags.isEmpty() && status == 0)
        assertTrue("$name: the 60 s played", lines.any { it.startsWith("OK: 0 flags over $seconds s") })
    }

    /** ps_render renders the export: no NaN, true peak ≤ −1 dBTP, the song's length, no 4 s of silence inside it. */
    private fun renderExport(name: String, renderer: File, pscmd: File, wav: File, songSeconds: Double) {
        val report = File(wav.parentFile, "$name.report.json")
        val process = ProcessBuilder(renderer.absolutePath, "--report", report.absolutePath, pscmd.absolutePath, wav.absolutePath).redirectErrorStream(true).start()
        val log = process.inputStream.bufferedReader().readText()
        assertEquals("$name: ps_render failed: $log", 0, process.waitFor())
        val json = report.readText()
        fun field(key: String) = Regex("\"$key\": (-?[0-9.]+)").find(json)?.groupValues?.get(1)?.toDouble() ?: error("no $key in $json")
        val (channels, samples) = floatWav(wav)
        val frames = samples.size / channels
        val nonFinite = samples.count { !it.isFinite() }
        assertEquals("$name: NaN or infinite samples in the export", 0, nonFinite)
        val truePeak = field("true_peak_db")
        assertTrue("$name: export true peak $truePeak dBTP (≤ −1)", truePeak <= -1.0)
        val seconds = frames / 48_000.0
        // The song, plus at most its tail (a reverb, a delay ringing on: RhythmViewModel.MAX_TAIL_FRAMES, 10 s), never shorter.
        assertTrue("$name: export lasts $seconds s for a song of $songSeconds s", seconds >= songSeconds - 0.05 && seconds <= songSeconds + 10.05)
        // The music never cuts out inside the song: no 4 s in a row under −60 dBFS (its last 10 s aside: a fade, the tail).
        var quietRun = 0
        var worst = 0
        val second = 48_000
        val inside = ((songSeconds - 10) * second).toInt().coerceAtMost(frames)
        for (start in 0 until inside step second / 10) {
            var peak = 0f
            for (i in start until minOf(start + second / 10, frames)) for (c in 0 until channels) peak = maxOf(peak, kotlin.math.abs(samples[i * channels + c]))
            quietRun = if (peak < 0.001f) quietRun + 1 else 0
            worst = maxOf(worst, quietRun)
        }
        assertTrue("$name: the export goes silent for ${worst / 10.0} s inside the song", worst < 40)
        println("real projects: $name export %.1f s, %.1f LUFS, true peak %.2f dBTP".format(seconds, field("lufs"), truePeak))
    }

    /** A 32-bit float WAV (what ps_render writes): its channels and samples. */
    private fun floatWav(file: File): Pair<Int, FloatArray> {
        val b = ByteBuffer.wrap(file.readBytes()).order(ByteOrder.LITTLE_ENDIAN)
        check(String(ByteArray(4).also { b.get(it) }) == "RIFF") { "${file.name}: not a WAV" }
        b.position(12)
        var channels = 2
        while (b.remaining() >= 8) {
            val id = String(ByteArray(4).also { b.get(it) })
            val size = b.int
            if (id == "fmt ") {
                val at = b.position()
                b.short
                channels = b.short.toInt()
                b.position(at + size)
            } else if (id == "data") {
                val n = minOf(size, b.remaining()) / 4
                val out = FloatArray(n)
                b.asFloatBuffer().get(out)
                return channels to out
            } else {
                b.position(b.position() + size + (size and 1))
            }
        }
        error("${file.name}: no data")
    }

    companion object {
        /** The repository's folder (on the test classpath, or from the module when run elsewhere). */
        fun fixtures(): File? =
            RealProjectsTest::class.java.classLoader?.getResource("real-projects")?.takeIf { it.protocol == "file" }?.let { File(it.toURI()) }
                ?: File("src/test/resources/real-projects").takeIf { it.isDirectory }

        /** The projects in [dir]: each folder with a project.json, each .zip. */
        fun realProjects(dir: File): List<RealProject> {
            val entries = dir.listFiles()?.sortedBy { it.name } ?: return emptyList()
            return entries.mapNotNull { f ->
                when {
                    f.isDirectory && File(f, "project.json").isFile -> {
                        val absent = File(f, "absents.txt").takeIf { it.isFile }?.readLines().orEmpty()
                            .map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }.map { it.split(Regex("\\s+"))[0] }.toSet()
                        val samples = File(f, "samples").listFiles()?.filter { it.isFile }.orEmpty()
                        RealProject(f.name, { zipOf(f) }, absent, samples.map { it.name }.toSet(), { File(f, "project.json").readText() })
                    }
                    f.isFile && f.name.endsWith(".zip") -> {
                        val names = java.util.zip.ZipFile(f).use { z -> z.entries().toList().map { it.name } }
                        val present = names.filter { it.startsWith("samples/") }.map { it.removePrefix("samples/") }.toSet()
                        RealProject(f.name.removeSuffix(".zip").removeSuffix(".pocketstudio"), { f.readBytes() }, emptySet(), present, {
                            java.util.zip.ZipFile(f).use { z -> z.getInputStream(z.getEntry("project.json")).bufferedReader().readText() }
                        })
                    }
                    else -> null
                }
            }
        }

        /** A folder as the .zip the app shares: project.json and samples/. */
        private fun zipOf(dir: File): ByteArray {
            val bytes = ByteArrayOutputStream()
            ZipOutputStream(bytes).use { zip ->
                zip.putNextEntry(ZipEntry("project.json"))
                File(dir, "project.json").inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
                File(dir, "samples").listFiles()?.filter { it.isFile }?.sortedBy { it.name }?.forEach { s ->
                    zip.putNextEntry(ZipEntry("samples/${s.name}"))
                    s.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
            return bytes.toByteArray()
        }
    }
}
