package com.pocketstudio.app.live

import androidx.lifecycle.viewModelScope
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.audio.finishOrRelease
import com.pocketstudio.app.remix.StemRouting
import com.pocketstudio.app.remix.StemSelection
import com.pocketstudio.app.report.AppLog
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.app.rhythm.TempoFits
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.engine.NativeSample
import com.pocketstudio.model.Project
import com.pocketstudio.model.RemixCell
import com.pocketstudio.model.Scene
import com.pocketstudio.model.i18n.tr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Lot D2 (« case Remix du Live »): a passage of the separated song in a cell of the Live grid's
 * remix column, one per brick. Chosen from the cell's menu (a stem or the whole song, a start bar,
 * a length) or sent from « Découper la chanson » ([StemDestination.Live][com.pocketstudio.app.remix.StemDestination.Live]):
 * cut on whole bars at the song's tempo, kept as a sound of the project (`samples/`, saved and
 * zipped with it), one undo step. The engine plays it on the remix slot (commands 170 and 171);
 * like the loops of step 4e, it follows the project's tempo through a copy stretched on a worker
 * thread (the engine keeps the old copy on the beat meanwhile). Held by [RhythmViewModel] in one field.
 */
class LiveRemixCells internal constructor(private val vm: RhythmViewModel) {
    private val fits = TempoFits()
    private var fitJob: Job? = null
    private var fitFor = Double.NaN
    /** The cells the live engine was given a passage (to empty them when they go). */
    private val sent = HashSet<Int>()

    private val _busy = MutableStateFlow<Int?>(null)
    /** The brick whose passage is being cut (its cell shows it), or null. */
    val busy: StateFlow<Int?> = _busy.asStateFlow()

    /** Every cell's passage to the engine (the live one, or an export's while capturing), then the copies at the tempo. */
    internal fun push(p: Project) {
        if (!NativeEngine.isAvailable) return
        val capturing = NativeEngine.isCapturing
        for (scene in 0 until minOf(Scene.MAX, NativeEngine.REMIX_CELLS)) {
            val cell = p.remix.cell(scene)?.takeIf { p.remix.setId != null }
            val stored = cell?.let { vm.padSamples.native(it.file) }
            if (cell == null || stored == null) {
                if (!capturing && sent.remove(scene)) {
                    fits.pick(slot(scene), null)
                    NativeEngine.setRemixCellSound(scene, null, 1)
                }
                continue
            }
            val want = TempoFits.want(slot(scene), cell.file, stored.frames, cell.bpm, p.tempoBpm)
            val pick = if (capturing) TempoFits.Pick(fits.ready(want), false) else fits.pick(slot(scene), want)
            NativeEngine.setRemixCellSound(scene, pick.sample ?: stored, cell.loopSteps(p.stepsPerBar), pick.refit)
            if (!capturing) sent += scene
        }
        if (!capturing) fitAll(p)
    }

    private fun slot(scene: Int) = "r$scene"

    private fun wants(p: Project): List<TempoFits.Want> = p.remix.cells.mapNotNull { c ->
        vm.padSamples.native(c.file)?.let { TempoFits.want(slot(c.scene), c.file, it.frames, c.bpm, p.tempoBpm) }
    }

    /** The copies at the project's tempo, made one by one on a worker once the tempo settles. */
    private fun fitAll(p: Project) {
        val wants = wants(p)
        val missing = fits.missing(wants)
        if (missing.isEmpty()) {
            if (fitJob?.isActive != true) fits.prune(wants)
            return
        }
        if (fitJob?.isActive == true && fitFor == p.tempoBpm) return
        fitJob?.cancel()
        fitFor = p.tempoBpm
        fitJob = vm.viewModelScope.launch {
            delay(250)  // tempo nudges and drags settle first
            for (w in missing) {
                val source = vm.padSamples.native(w.file) ?: continue
                val copy = finishOrRelease(Dispatchers.Default, NativeSample::close) { fits.compute(source, w.frames) }
                if (copy == null) AppLog.add("Case Remix : calage au tempo impossible (${w.slot})")
                fits.register(w.key, copy)
                if (!isActive) break
            }
            fitFor = Double.NaN
            push(vm.project.value)
        }
    }

    /**
     * The cell's menu: brick [scene]'s cell plays [bars] bars of stem [stem] ([RemixCell.WHOLE]: the
     * whole song) from the song's bar [fromBar] (counted from its first beat).
     */
    fun choose(scene: Int, stem: Int, fromBar: Int, bars: Int) {
        val p = vm.project.value
        val setId = p.remix.setId ?: return
        val bpm = p.remix.songBpm
        if (_busy.value != null || scene !in 0 until Scene.MAX) return
        if (bpm <= 0.0) {
            vm.liveRemixSays(tr("Le tempo de la chanson n'est pas connu : choisis le passage dans « Découper la chanson »."))
            return
        }
        val n = bars.coerceIn(1, RemixCell.MAX_BARS)
        val barFrames = RemixCell.framesFor(1, bpm, p.beatsPerBar, StemSelection.SAMPLE_RATE)
        val start = Math.round(p.remix.firstBeat * StemSelection.SAMPLE_RATE) + fromBar.coerceAtLeast(0) * barFrames
        _busy.value = scene
        vm.viewModelScope.launch {
            try {
                val pcm = vm.stemBoardHost.withSong(setId) { src ->
                    if (start >= src.frames) return@withSong null
                    val count = n * barFrames
                    val data = if (stem == RemixCell.WHOLE) {
                        (0 until com.pocketstudio.model.Remix.STEMS).map { src.read(it, start, count, fitted = false) }
                            .reduce { a, b -> FloatArray(maxOf(a.size, b.size)) { i -> a.getOrElse(i) { 0f } + b.getOrElse(i) { 0f } } }
                    } else {
                        src.read(stem, start, count, fitted = false)
                    }
                    Pcm(data, 2, StemSelection.SAMPLE_RATE)
                }
                if (pcm == null || pcm.frames == 0) {
                    vm.liveRemixSays(tr("Ce passage n'a pas pu être lu."))
                    return@launch
                }
                place(scene, pcm, nameOf(stem, p), bpm, stem, start.toDouble() / StemSelection.SAMPLE_RATE)
            } finally {
                _busy.value = null
            }
        }
    }

    /** What a cell of stem [stem] is called (French, shown translated): « Voix », the song's name for the whole song. */
    private fun nameOf(stem: Int, p: Project): String =
        if (stem == RemixCell.WHOLE) p.remix.name.ifBlank { "Chanson" } else StemRouting.stemFrench(stem)

    /**
     * [pcm] (a passage of the song cut at [bpm], the song's tempo; 0: not known) in brick [scene]'s
     * remix cell: brought to whole bars (silence after a short one, the end of a long one cut), with
     * short fades so its loop never clicks; one undo step. Also the « Découper la chanson » route.
     */
    suspend fun place(scene: Int, pcm: Pcm, name: String, bpm: Double, stem: Int, fromSeconds: Double, say: Boolean = true) {
        val p = vm.project.value
        // An unknown tempo: as many whole bars at the project's tempo (it then keeps its speed).
        val tempo = bpm.takeIf { it > 0.0 } ?: p.tempoBpm
        val bars = RemixCell.barsFor(pcm.seconds, tempo, p.beatsPerBar)
        val frames = RemixCell.framesFor(bars, tempo, p.beatsPerBar, pcm.sampleRate).toInt()
        val loop = withContext(Dispatchers.Default) { wholeBars(pcm, frames) }
        val (sample, prepared) = withContext(Dispatchers.IO) { vm.padSamples.add(loop, name) }
        vm.padSamples.register(prepared)
        vm.session.edit { q ->
            val cell = RemixCell(scene, sample.file, sample.name, bars, bpm.takeIf { it > 0.0 } ?: 0.0, stem, fromSeconds)
            q.copy(remix = q.remix.withCell(scene, cell))
        }
        push(vm.project.value)
        vm.liveRemixPushed()
        AppLog.add("Case Remix : « $name », $bars mesure(s), brique ${Scene.NAMES.getOrElse(scene) { "?" }}")
        if (say) vm.liveRemixSays(tr("%1\$s dans la case Remix de la brique %2\$s.", com.pocketstudio.model.DefaultNames.shownSample(name), Scene.NAMES.getOrElse(scene) { "?" }))
    }

    /** Brick [scene]'s cell plays the whole song again (one undo step). */
    fun clear(scene: Int) {
        if (vm.project.value.remix.cell(scene) == null) return
        vm.session.edit { q -> q.copy(remix = q.remix.withCell(scene, null)) }
        push(vm.project.value)
        vm.liveRemixPushed()
    }

    fun close() {
        fitJob?.cancel()
        fits.close()
    }

    companion object {
        /** Fades at the passage's ends, so its loop goes round without a click (ms). */
        const val FADE_IN_MS = 3
        const val FADE_OUT_MS = 8

        /** [pcm] made exactly [frames] long (silence after, or cut), with short fades at both ends. */
        fun wholeBars(pcm: Pcm, frames: Int): Pcm {
            val ch = pcm.channels
            val out = FloatArray(frames.coerceAtLeast(1) * ch)
            System.arraycopy(pcm.data, 0, out, 0, minOf(pcm.data.size, out.size))
            val heard = minOf(pcm.frames, frames)
            val fadeIn = (pcm.sampleRate * FADE_IN_MS / 1000).coerceAtMost(heard / 2)
            val fadeOut = (pcm.sampleRate * FADE_OUT_MS / 1000).coerceAtMost(heard / 2)
            for (i in 0 until fadeIn) for (c in 0 until ch) out[i * ch + c] *= i.toFloat() / fadeIn
            for (i in 0 until fadeOut) for (c in 0 until ch) out[(heard - 1 - i) * ch + c] *= i.toFloat() / fadeOut
            return Pcm(out, ch, pcm.sampleRate)
        }
    }
}
