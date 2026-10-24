package com.pocketstudio.app.learn

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.audio.PcmPlayer
import com.pocketstudio.app.audio.finishOrRelease
import com.pocketstudio.app.report.AppLog
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.engine.OfflineExport
import com.pocketstudio.model.Project
import com.pocketstudio.model.learn.SoundExample
import com.pocketstudio.model.learn.SoundGlossary
import com.pocketstudio.model.learn.SoundWord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * What the glossary is playing: word [key], [preparing] while the engine renders it, then the
 * take heard ([take], an index in its example's labels; -1 for a single take).
 */
data class GlossaryPlay(val key: String, val preparing: Boolean = true, val take: Int = -1)

/**
 * The glossary's sound examples (phase 39): each word's tiny projects rendered by a private
 * engine (never the open project's, which keeps playing as it was), then played by Android's
 * player as the library's previews are. The last few are kept, a second touch is immediate.
 */
class LearnViewModel(app: Application) : AndroidViewModel(app) {
    /**
     * Describes a project to the engine under [NativeEngine.captureInto] (main thread): set by
     * the screen that owns the engine sync (as the library's demos), null in tests.
     */
    var sync: ((Project) -> Unit)? = null

    private val _playing = MutableStateFlow<GlossaryPlay?>(null)
    val playing: StateFlow<GlossaryPlay?> = _playing.asStateFlow()

    private val player = PcmPlayer { _playing.value = null; follow?.cancel() }
    private var job: Job? = null
    private var follow: Job? = null
    private val cache = object : LinkedHashMap<String, Rendered>(8, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Rendered>?) = size > CACHE
    }

    /** A word's example as heard: the sound, and the frame each take starts on. */
    class Rendered(val pcm: Pcm, val starts: List<Int>)

    /** ▶ on a word: plays its example (a second touch on the word playing stops it). */
    fun toggle(word: SoundWord) {
        if (_playing.value?.key == word.key) {
            stop()
            return
        }
        play(word)
    }

    /** ▶ on the word named [key] (a « ? » on screen). */
    fun play(key: String) {
        SoundGlossary.ALL.firstOrNull { it.key == key }?.let(::play)
    }

    fun play(word: SoundWord) {
        stop()
        val example = word.example()
        _playing.value = GlossaryPlay(word.key)
        job = viewModelScope.launch {
            val rendered = cache[word.key] ?: render(example)?.also { cache[word.key] = it }
            if (rendered == null) {
                _playing.value = null
                return@launch
            }
            if (!player.play(rendered.pcm)) {
                _playing.value = null
                return@launch
            }
            _playing.value = GlossaryPlay(word.key, preparing = false, take = if (example.takes.size > 1) 0 else -1)
            if (example.takes.size > 1) followTakes(word.key, rendered)
        }
    }

    /** Lights the take heard (« Sans », then « Avec ») as the sound goes. */
    private fun followTakes(key: String, rendered: Rendered) {
        follow?.cancel()
        follow = viewModelScope.launch {
            val started = System.nanoTime()
            rendered.starts.forEachIndexed { i, frame ->
                val at = frame * 1000L / rendered.pcm.sampleRate
                val wait = at - (System.nanoTime() - started) / 1_000_000
                if (wait > 0) delay(wait)
                if (_playing.value?.key != key) return@launch
                _playing.value = GlossaryPlay(key, preparing = false, take = i)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        follow?.cancel()
        player.stop()
        _playing.value = null
    }

    private suspend fun render(example: SoundExample): Rendered? {
        val sync = sync ?: return null
        if (!NativeEngine.isAvailable) return null
        val parts = ArrayList<FloatArray>()
        for (take in example.takes) {
            val job = finishOrRelease(Dispatchers.Default, OfflineExport::close) { OfflineExport() }!!
            val data = try {
                NativeEngine.captureInto(job) {
                    sync(take)
                    NativeEngine.setMetronome(false)
                }
                coroutineScope {
                    // Another word touched meanwhile: this render stops at once.
                    var rendered = false
                    val stopper = launch { try { awaitCancellation() } finally { if (!rendered) job.cancel() } }
                    withContext(Dispatchers.Default) {
                        job.renderToArray(OfflineExport.framesFor(example.bars, take.tempoBpm, take.beatsPerBar), TAIL_FRAMES)
                    }.also { rendered = true; stopper.cancel() }
                }
            } finally {
                withContext(NonCancellable + Dispatchers.Main) { job.close() }
            }
            if (data.isEmpty()) return null
            parts += data
        }
        val joined = join(parts, GAP_FRAMES)
        AppLog.add("Glossaire : exemple de ${joined.first.size / 2 / OfflineExport.SAMPLE_RATE.toFloat()} s")
        return Rendered(Pcm(joined.first, 2, OfflineExport.SAMPLE_RATE), joined.second)
    }

    override fun onCleared() {
        stop()
    }

    companion object {
        private const val CACHE = 6
        /** The tail of each take (a reverb, an echo, the last note) before the next one. */
        const val TAIL_FRAMES = OfflineExport.SAMPLE_RATE * 6L / 10
        /** Silence between « sans » and « avec ». */
        const val GAP_FRAMES = OfflineExport.SAMPLE_RATE / 4

        /** [parts] (interleaved stereo) one after the other with [gap] frames of silence between them, and the frame each starts on. */
        fun join(parts: List<FloatArray>, gap: Int): Pair<FloatArray, List<Int>> {
            val frames = parts.sumOf { it.size / 2 } + gap * (parts.size - 1).coerceAtLeast(0)
            val out = FloatArray(frames * 2)
            val starts = ArrayList<Int>()
            var at = 0
            parts.forEachIndexed { i, p ->
                if (i > 0) at += gap
                starts += at
                p.copyInto(out, at * 2)
                at += p.size / 2
            }
            return out to starts
        }
    }
}
