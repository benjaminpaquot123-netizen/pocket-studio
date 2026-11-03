package com.pocketstudio.app.padlive

import androidx.lifecycle.viewModelScope
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.report.AppLog
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.model.LiveClip
import com.pocketstudio.model.LivePads
import com.pocketstudio.model.PadLive
import com.pocketstudio.model.i18n.tr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.ceil

/** « Enregistrer ce que je joue », as the button shows it. */
sealed interface PlayedTakeUi {
    /** Nothing recorded: a touch arms it. */
    data object Ready : PlayedTakeUi
    /** Armed: it starts on the next bar line where the music plays. */
    data object Armed : PlayedTakeUi
    /** Recording, [bars] begun so far: a touch ends it on the next bar line. */
    data class Recording(val bars: Int) : PlayedTakeUi
    /** Asked to end: on the next bar line. */
    data object Ending : PlayedTakeUi
    /** Recorded: [bars] whole bars, waiting to be put on a pad or in a brick. */
    data class Done(val bars: Int) : PlayedTakeUi
}

/** Where the engine records the output from bar line to bar line (a test gives its own). */
interface ResampleSource {
    /** Armed for at most [seconds]; 0: refused (no memory). */
    fun start(seconds: Int): Long
    /** [state 0 armed, 1 recording, 2 finished; frames written]. */
    fun status(handle: Long): LongArray?
    /** Ends on the next bar line heard. */
    fun stop()
    /** What was recorded (stereo interleaved, [RATE]), freed; ends it at once when still running. */
    fun finish(handle: Long): FloatArray?

    companion object {
        const val RATE = 48_000
    }
}

private object EngineResample : ResampleSource {
    // Armed in silence it waits for the music (the first pad tapped starts the clock on a bar line).
    override fun start(seconds: Int): Long = if (NativeEngine.isNative) NativeEngine.resampleStart(seconds, waitForSound = true) else 0L
    override fun status(handle: Long): LongArray? = if (handle != 0L) NativeEngine.resampleStatus(handle) else null
    override fun stop() { if (NativeEngine.isNative) NativeEngine.resampleStop() }
    override fun finish(handle: Long): FloatArray? = if (handle != 0L) NativeEngine.resampleFinish(handle) else null
}

/**
 * « Enregistrer ce que je joue » (n° 25, lot B2): what Pad Live plays (the whole output, its
 * effects included) from a bar line to a bar line, whole bars, then put on an empty pad (a loop at
 * the project's tempo) or in a brick of the Live grid; each one undo step, saved with the project.
 * Held by [PadLiveController.played].
 */
class PlayedTake(private val vm: RhythmViewModel) {
    private val _ui = MutableStateFlow<PlayedTakeUi>(PlayedTakeUi.Ready)
    val ui: StateFlow<PlayedTakeUi> = _ui.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    /** A word for the player (too short, no room, placed), shown once. */
    val message: StateFlow<String?> = _message.asStateFlow()

    fun messageShown() {
        _message.value = null
    }

    internal var sourceForTest: ResampleSource? = null
    private val source: ResampleSource get() = sourceForTest ?: EngineResample

    private var handle = 0L
    private var job: Job? = null
    /** The take recorded, waiting for its place, and the tempo it was played at. */
    private var take: Pcm? = null
    private var takeBpm = 0.0

    /** The button: arms, ends on the bar line, or (armed, nothing heard yet) lets go. */
    fun press() {
        when (_ui.value) {
            PlayedTakeUi.Ready, is PlayedTakeUi.Done -> arm()
            PlayedTakeUi.Armed -> cancel()
            is PlayedTakeUi.Recording -> {
                source.stop()
                _ui.value = PlayedTakeUi.Ending
            }
            PlayedTakeUi.Ending -> Unit
        }
    }

    private fun framesPerBar(bpm: Double = vm.project.value.tempoBpm): Double =
        60.0 / bpm.coerceAtLeast(1.0) * vm.project.value.beatsPerBar * ResampleSource.RATE

    private fun arm() {
        take = null
        val seconds = ceil(minOf(MAX_SECONDS.toDouble(), MAX_BARS * framesPerBar() / ResampleSource.RATE)).toInt() + 1
        val h = source.start(seconds)
        if (h == 0L) {
            _message.value = tr("Pas assez de mémoire pour enregistrer : ferme d'autres applis et réessaie.")
            return
        }
        handle = h
        _ui.value = PlayedTakeUi.Armed
        AppLog.add("Pad Live : enregistrer ce que je joue (armé, $seconds s au plus)")
        job?.cancel()
        job = vm.viewModelScope.launch {
            while (true) {
                val status = source.status(h) ?: break
                when (status[0].toInt()) {
                    1 -> if (_ui.value !is PlayedTakeUi.Ending) {
                        _ui.value = PlayedTakeUi.Recording((status[1] / framesPerBar()).toInt() + 1)
                    }
                    2 -> break
                }
                delay(POLL_MS)
            }
            collect(h)
        }
    }

    private suspend fun collect(h: Long) {
        handle = 0L
        val bpm = vm.project.value.tempoBpm
        val data = withContext(Dispatchers.IO) { source.finish(h) } ?: FloatArray(0)
        val perBar = framesPerBar(bpm)
        val bars = Math.round(data.size / 2 / perBar).toInt().coerceAtMost(MAX_BARS)
        if (bars < 1) {
            _ui.value = PlayedTakeUi.Ready
            _message.value = tr("Trop court : joue au moins une mesure.")
            return
        }
        // Whole bars (ended by the buffer's size, it stops anywhere): cut on the last bar line.
        val frames = Math.round(bars * perBar).toInt().coerceAtMost(data.size / 2)
        take = Pcm(data.copyOf(frames * 2), 2, ResampleSource.RATE)
        takeBpm = bpm
        _ui.value = PlayedTakeUi.Done(bars)
        AppLog.add("Pad Live : prise de $bars mesures à ${"%.1f".format(bpm)} BPM")
    }

    /** Armed and nothing heard yet, or the take thrown away: back to ready. */
    fun cancel() {
        job?.cancel()
        job = null
        val h = handle
        handle = 0L
        if (h != 0L) vm.viewModelScope.launch(Dispatchers.IO) { source.finish(h) }
        take = null
        _ui.value = PlayedTakeUi.Ready
    }

    /** The first empty pad of the page shown (else of any page), or null: every pad is taken. */
    fun emptyPad(): Int? {
        val p = vm.project.value
        val page = p.padLive.shownPage
        val shown = (0 until PadLive.PAGE_SIZE).map { PadLive.pad(page, it) }
        return shown.firstOrNull { p.live.at(it) == null } ?: (0 until LivePads.ALL).firstOrNull { p.live.at(it) == null }
    }

    private fun takeName(): String {
        val names = vm.project.value.live.all.mapNotNull { it?.name }.toSet()
        return (1..999).map { tr("Ma prise %d", it) }.first { it !in names }
    }

    /**
     * The take on pad [pad] (null: [emptyPad]), a loop of its bars that follows the tempo from now
     * on: one undo step. False: no empty pad (the take waits).
     */
    fun placeOnPad(pad: Int? = null): Boolean {
        val pcm = take ?: return false
        val target = pad ?: emptyPad()
        if (target == null || vm.project.value.live.at(target) != null) {
            _message.value = tr("Aucun pad vide : vide un pad, ou mets la prise dans une brique.")
            return false
        }
        val bars = (_ui.value as? PlayedTakeUi.Done)?.bars ?: return false
        val name = takeName()
        val bpm = takeBpm
        take = null
        _ui.value = PlayedTakeUi.Ready
        vm.viewModelScope.launch {
            val (sample, prepared) = withContext(Dispatchers.IO) { vm.padSamples.add(pcm, name) }
            vm.padSamples.register(prepared)
            val beats = (bars * vm.project.value.beatsPerBar).coerceIn(1, LivePads.MAX_BEATS)
            vm.session.edit { q -> q.copy(live = q.live.withAt(target, LiveClip(sample.file, name, beats, oneShot = false, bpm = bpm).sanitized())) }
            vm.pushLive()
            _message.value = tr("Ta prise est sur le pad %1\$s%2\$d.", PadLive.PAGE_NAMES[PadLive.pageOf(target)], target % PadLive.PAGE_SIZE + 1)
            AppLog.add("Pad Live : prise sur le pad $target ($bars mesures)")
        }
        return true
    }

    /** The take in a brick of the Live grid (the scene shown, on a free audio track): one undo step. */
    fun placeInBrick(): Boolean {
        val pcm = take ?: return false
        val p = vm.project.value
        if (!vm.placeInCell(com.pocketstudio.model.SongPart.audio(-1), p.scene, pcm, takeName(), known = takeBpm)) return false
        take = null
        _ui.value = PlayedTakeUi.Ready
        _message.value = tr("Ta prise est dans une brique du Live (Plus › Briques).")
        return true
    }

    /** Tests: the take waiting for its place. */
    internal val takeForTest: Pcm? get() = take

    companion object {
        /** A take lasts at most this many bars, and this many seconds. */
        const val MAX_BARS = 16
        const val MAX_SECONDS = 60
        private const val POLL_MS = 40L
    }
}
