package com.pocketstudio.app.bench

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.library.PreviewLevel
import com.pocketstudio.app.library.SoundLibraryViewModel
import com.pocketstudio.app.library.SoundRef
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.model.StarterWizard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.io.File
import kotlin.math.abs
import kotlin.math.log10

/**
 * Lot 1 (APK 181: « Nappe / accords » much louder than the other steps of the assistant): every
 * sound the assistant offers for each melodic part (basses, pads and chords, keys, leads, plucks:
 * the synth's and Plaits' sounds, the kit voices played across the keys) previewed exactly as ▶
 * plays it (the two bars in context, rendered through the engine core by `ps_render`), measured
 * with one metric ([PreviewLevel.loudness]: the loudest 3 s, K-weighted). Asserts that what is
 * played sits at [PreviewLevel.REFERENCE_LUFS] ± [PreviewLevel.TOLERANCE_LU]; writes the table,
 * before and after the preview level, to `previews.md` next to the samples.
 *
 * Needs the engine: off unless `-Pbench.render=<ps_render>` (scripts/live-bench.sh runs it);
 * `-Pbench.styles` narrows the styles.
 */
@RunWith(RobolectricTestRunner::class)
class PreviewLevelBench {
    private val app = ApplicationProvider.getApplicationContext<Application>()

    private fun <T> await(what: String, block: suspend () -> T): T {
        var done = false
        var result: Result<T>? = null
        CoroutineScope(Dispatchers.Main).launch {
            result = runCatching { block() }
            done = true
        }
        val until = System.currentTimeMillis() + 120_000
        while (!done) {
            check(System.currentTimeMillis() < until) { "waited too long for $what" }
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(2)
        }
        return result!!.getOrThrow()
    }

    private fun peakDb(pcm: Pcm) = 20 * log10(pcm.data.maxOf { abs(it) }.toDouble().coerceAtLeast(1e-9))

    @Test
    fun everyChoosableSoundIsPreviewedAtOneLevel() {
        val renderer = System.getProperty("ps.bench.render")?.takeIf { it.isNotBlank() }?.let(::File)
        assumeTrue("preview bench: run with -Pbench.render=<ps_render> (scripts/live-bench.sh)", renderer != null)
        val only = System.getProperty("ps.bench.styles")?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }?.toSet().orEmpty()
        // A style of each family and tempo (the assistant offers every library sound in each).
        val styles = (if (only.isEmpty()) listOf("jump_up", "techno", "deep_house", "lofi", "trap", "trance") else only.toList())
            .mapNotNull(StarterWizard::style)
        assumeTrue("no style matches ${only.joinToString()}", styles.isNotEmpty())
        val dir = File(renderer!!.parentFile, "previews").apply { deleteRecursively(); mkdirs() }
        val sink = RecordingSink(File(dir, "samples"), renderer)
        NativeEngine.sink = sink
        val rows = ArrayList<String>()
        val off = ArrayList<String>()
        try {
            val library = SoundLibraryViewModel(app)
            for (style in styles) {
                library.chooseStyle(style.id)
                for (part in StarterWizard.Part.entries) {
                    for (sound in StarterWizard.soundsFor(part, style)) {
                        val ref = SoundRef.Melodic(part, sound)
                        val raw = await("${style.id} ${sound.name}") { library.loadForTest(ref) } ?: continue
                        val heard = await("${style.id} ${sound.name}") { library.previewForTest(ref) } ?: continue
                        val before = PreviewLevel.loudness(raw) ?: continue
                        val after = PreviewLevel.loudness(heard) ?: continue
                        val peak = peakDb(heard)
                        rows += "| ${style.id} | ${part.id} | ${sound.name} | ${"%.1f".format(before)} | ${"%.1f".format(after)} | ${"%.1f".format(peak)} |"
                        if (abs(after - PreviewLevel.REFERENCE_LUFS) > PreviewLevel.TOLERANCE_LU) off += "${style.id} ${part.id} ${sound.name}: ${"%.1f".format(after)} LUFS"
                    }
                }
            }
        } finally {
            NativeEngine.sink = null
        }
        File(dir, "previews.md").writeText(
            "| Style | Partie | Son | Avant (LUFS, 3 s max) | Joué (LUFS) | Crête (dBFS) |\n|---|---|---|---|---|---|\n" + rows.joinToString("\n") + "\n",
        )
        println("preview bench: ${rows.size} previews, table in ${File(dir, "previews.md")}")
        assertTrue("no preview measured", rows.isNotEmpty())
        assertTrue("previews off the level (${PreviewLevel.REFERENCE_LUFS} ± ${PreviewLevel.TOLERANCE_LU}):\n" + off.joinToString("\n"), off.isEmpty())
    }
}
