package com.pocketstudio.app.tuto

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.bench.RecordingSink
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.model.Project
import com.pocketstudio.model.ProjectStore
import com.pocketstudio.model.tuto.TutoChecks
import com.pocketstudio.model.tuto.TutoCourse
import com.pocketstudio.model.tuto.TutoLevel
import com.pocketstudio.model.tuto.TutoTarget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.io.File
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * « Écouter l'exemple » (owner, APK 226: the button said « Arrêter l'écoute », nothing was heard):
 * every example of the jump up course (song, drums:…, parts:…, part:…, vs:…) rendered by the app's
 * own code from an EMPTY tutorial project, through the engine core (`ps_render`), is loud enough
 * to hear: no NaN, peak over -20 dBFS, RMS over -45 dBFS.
 *
 * Needs the engine: off unless `-Pbench.render=<ps_render>`.
 */
@RunWith(RobolectricTestRunner::class)
class TutoExampleSoundBench {
    private val app: Application = ApplicationProvider.getApplicationContext()

    @After
    fun tearDown() {
        NativeEngine.sink = null
        com.pocketstudio.app.projects.CurrentProject.set(app, RhythmViewModel.PROJECT_ID)
    }

    private fun <T> await(what: String, block: suspend () -> T): T {
        var done = false
        var result: Result<T>? = null
        CoroutineScope(Dispatchers.Main).launch {
            result = runCatching { block() }
            done = true
        }
        val until = System.currentTimeMillis() + 300_000
        while (!done) {
            check(System.currentTimeMillis() < until) { "waited too long for $what" }
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(2)
        }
        return result!!.getOrThrow()
    }

    @Test
    fun everyExampleIsAudible() {
        val renderer = System.getProperty("ps.bench.render")?.takeIf { it.isNotBlank() }?.let(::File)
        assumeTrue("run with -Pbench.render=<ps_render>", renderer != null)
        val dir = File(renderer!!.parentFile, "tuto-examples").apply { deleteRecursively(); mkdirs() }
        NativeEngine.sink = RecordingSink(File(dir, "samples"), renderer)
        val id = "tuto-ex-${System.nanoTime()}"
        ProjectStore(File(app.filesDir, "projects")).save(id, Project())
        com.pocketstudio.app.projects.CurrentProject.set(app, id)
        val vm = RhythmViewModel(app)
        shadowOf(Looper.getMainLooper()).idle()
        val target = TutoTarget.of("jump_up")!!
        val course = TutoCourse.of("jump_up")!!
        val listens = (course.chaptersFor(TutoLevel.Facile) + course.chaptersFor(TutoLevel.Pro)).flatMap { it.steps }.filter { it.listen.isNotEmpty() }
            .distinctBy { it.listen }
        val report = StringBuilder()
        val quiet = ArrayList<String>()
        for (s in listens) {
            val ex = TutoChecks.example(s, target)
            assertTrue("${s.id}: no example for ${s.listen}", ex != null)
            for ((name, p) in listOfNotNull(ex!!.before?.let { "before" to it }, "after" to ex.after)) {
                val pcm = await("${s.listen} $name") { vm.renderExample(p) }
                val ok = pcm != null && pcm.none { it.isNaN() }
                val peak = pcm?.maxOfOrNull { abs(it) } ?: 0f
                val rms = pcm?.let { a -> sqrt(a.fold(0.0) { acc, v -> acc + v * v } / a.size.coerceAtLeast(1)) } ?: 0.0
                val line = "%-40s %-6s frames=%7d peak=%6.1f dBFS rms=%6.1f dBFS".format(s.listen, name, (pcm?.size ?: 0) / 2, 20 * log10(peak.toDouble().coerceAtLeast(1e-9)), 20 * log10(rms.coerceAtLeast(1e-9)))
                report.appendLine(line)
                if (!ok || peak < 0.1f || rms < 0.0056) quiet += line
            }
        }
        File(dir, "report.txt").writeText(report.toString())
        assertTrue("silent or too quiet examples:\n${quiet.joinToString("\n")}\n\nall:\n$report", quiet.isEmpty())
    }
}
