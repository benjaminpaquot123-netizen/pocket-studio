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
import com.pocketstudio.model.tuto.TutoNeed
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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The mids of the bass music courses (review « Tuto tous styles », MOYEN 5 and its rest, audit stage 1):
 * the song a Pro course makes with « Montre-moi » alone, its first Drop rendered by the app's own code
 * through the engine core (`ps_render`), against the assistant's song of the style: the share of the mix
 * in 500-2000 Hz (dB). Fails when the course's Drop is more than [MAX_GAP_DB] under the assistant's.
 *
 * Needs the engine: off unless `-Pbench.render=<ps_render>`. Report: `<ps_render's folder>/tuto-mids/report.txt`.
 */
@RunWith(RobolectricTestRunner::class)
class TutoMidsBench {
    private val app: Application = ApplicationProvider.getApplicationContext()

    @After
    fun tearDown() {
        NativeEngine.sink = null
        com.pocketstudio.app.projects.CurrentProject.set(app, RhythmViewModel.PROJECT_ID)
    }

    private fun <T> await(block: suspend () -> T): T {
        var done = false
        var result: Result<T>? = null
        CoroutineScope(Dispatchers.Main).launch {
            result = runCatching { block() }
            done = true
        }
        val until = System.currentTimeMillis() + 300_000
        while (!done) {
            check(System.currentTimeMillis() < until) { "render too long" }
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(2)
        }
        return result!!.getOrThrow()
    }

    /** The song [style]'s Pro course makes with « Montre-moi » alone (as TutoAllCoursesTest plays it). */
    private fun proSong(style: String, target: TutoTarget): Project {
        val steps = TutoCourse.of(style)!!.chaptersFor(TutoLevel.Pro).flatMap { it.steps }
        var p = target.blank()
        for (step in steps) {
            if (step.manual || step.listening || step.check == "shared") continue
            val baseline = if (step.check.startsWith("changed:")) TutoChecks.signature(step.check.substringAfter(':'), p) else null
            var guard = 0
            while (!TutoChecks.done(step, p, target, baseline) && guard++ < 12) {
                val need = TutoChecks.need(step, p, target)
                val next = when {
                    need is TutoNeed.Bars || need is TutoNeed.Track || need is TutoNeed.Part || need is TutoNeed.Scene -> TutoChecks.fulfil(need, step, p, target)?.first
                    else -> TutoChecks.show(step, p, target) ?: need?.let { TutoChecks.fulfil(it, step, p, target)?.first }
                } ?: break
                p = next.sanitized()
            }
        }
        return p
    }

    @Test
    fun theCoursesDropHasTheAssistantsMids() {
        val renderer = System.getProperty("ps.bench.render")?.takeIf { it.isNotBlank() }?.let(::File)
        assumeTrue("run with -Pbench.render=<ps_render>", renderer != null)
        val dir = File(renderer!!.parentFile, "tuto-mids").apply { deleteRecursively(); mkdirs() }
        NativeEngine.sink = RecordingSink(File(dir, "samples"), renderer)
        val id = "tuto-mids-${System.nanoTime()}"
        ProjectStore(File(app.filesDir, "projects")).save(id, Project())
        com.pocketstudio.app.projects.CurrentProject.set(app, id)
        val vm = RhythmViewModel(app)
        shadowOf(Looper.getMainLooper()).idle()
        val report = StringBuilder()
        val gaps = mutableMapOf<String, Double>()
        for (style in STYLES) {
            val target = TutoTarget.of(style) ?: continue
            val course = midShareDb(await { vm.renderExample(proSong(style, target), maxBars = 8, minBars = 8) } ?: error("$style: no render"))
            val theirs = midShareDb(await { vm.renderExample(target.assistantSong, maxBars = 8, minBars = 8) } ?: error("$style: no render"))
            gaps[style] = course - theirs
            report.appendLine("%-12s course %6.1f dB  assistant %6.1f dB  gap %+5.1f dB".format(style, course, theirs, course - theirs))
        }
        File(dir, "report.txt").writeText(report.toString())
        println(report)
        gaps.forEach { (style, gap) -> assertTrue("$style: the course's Drop is ${"%.1f".format(-gap)} dB under the assistant's in 500-2000 Hz", gap >= -MAX_GAP_DB) }
    }

    /** The 500-2000 Hz band's share of the mix (dB): mono, two 2nd-order high-passes at 500 Hz, two low-passes at 2 kHz. */
    private fun midShareDb(pcm: FloatArray): Double {
        val n = pcm.size / 2
        val mono = DoubleArray(n) { (pcm[2 * it] + pcm[2 * it + 1]) / 2.0 }
        var band = mono
        repeat(2) { band = biquad(biquad(band, 500.0, high = true), 2000.0, high = false) }
        val all = mono.sumOf { it * it }
        return 10 * log10(band.sumOf { it * it } / all.coerceAtLeast(1e-12))
    }

    /** An RBJ 2nd-order Butterworth high- or low-pass at [hz] (48 kHz). */
    private fun biquad(x: DoubleArray, hz: Double, high: Boolean): DoubleArray {
        val w = 2 * PI * hz / 48_000.0
        val alpha = sin(w) / (2 * sqrt(0.5))
        val c = cos(w)
        val b0 = if (high) (1 + c) / 2 else (1 - c) / 2
        val b1 = if (high) -(1 + c) else 1 - c
        val a0 = 1 + alpha
        val a1 = -2 * c
        val a2 = 1 - alpha
        val y = DoubleArray(x.size)
        var x1 = 0.0; var x2 = 0.0; var y1 = 0.0; var y2 = 0.0
        for (i in x.indices) {
            val v = (b0 * x[i] + b1 * x1 + b0 * x2 - a1 * y1 - a2 * y2) / a0
            x2 = x1; x1 = x[i]; y2 = y1; y1 = v; y[i] = v
        }
        return y
    }

    companion object {
        /** The bass music courses (and techno, as a check of the measure). */
        val STYLES = listOf("jungle", "dnb", "liquid_dnb", "neurofunk", "jump_up", "techno")
        /** How far under the assistant's Drop a course's Drop may be in 500-2000 Hz. */
        const val MAX_GAP_DB = 3.0
    }
}
