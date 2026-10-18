package com.pocketstudio.app.rhythm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.model.DrumPattern
import com.pocketstudio.model.Hit
import com.pocketstudio.model.Project
import com.pocketstudio.model.ProjectSession
import com.pocketstudio.model.ProjectStore
import com.pocketstudio.model.TapTempo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File

/** What the audio engine is doing right now, polled for the playhead and pad lights. */
data class LiveUi(val currentStep: Int = -1, val litPads: Set<Int> = emptySet())

data class TransportUi(
    val playing: Boolean = false,
    val metronome: Boolean = false,
    val countIn: Boolean = false,
    val selectedPad: Int = 0,
)

/**
 * The drum machine: owns the open project (autosaved) and mirrors every edit to the engine.
 * The engine never reads the project directly: it receives small commands.
 */
class RhythmViewModel(app: Application) : AndroidViewModel(app) {
    private val store = ProjectStore(File(app.filesDir, "projects"))
    val session: ProjectSession = ProjectSession(
        id = PROJECT_ID,
        initial = loadOrCreate(),
        store = store,
        scope = viewModelScope,
        io = Dispatchers.IO,
    )
    val project: StateFlow<Project> = session.project

    private val _transport = MutableStateFlow(TransportUi())
    val transport: StateFlow<TransportUi> = _transport.asStateFlow()

    private val tapTempo = TapTempo()

    private val _live = MutableStateFlow(LiveUi())
    val live: StateFlow<LiveUi> = _live.asStateFlow()
    private val lastTriggers = IntArray(NativeEngine.LANES)
    private val triggers = IntArray(NativeEngine.LANES)
    private val litUntil = LongArray(NativeEngine.LANES)

    /** Polls the engine about 30 times a second while the screen is visible. */
    suspend fun pollLive() {
        if (!NativeEngine.isAvailable) return
        NativeEngine.laneTriggers(lastTriggers)
        while (true) {
            NativeEngine.laneTriggers(triggers)
            val now = System.currentTimeMillis()
            for (i in triggers.indices) {
                if (triggers[i] != lastTriggers[i]) litUntil[i] = now + 120
                lastTriggers[i] = triggers[i]
            }
            val lit = (0 until NativeEngine.LANES).filter { litUntil[it] > now }.toSet()
            _live.value = LiveUi(NativeEngine.currentStep(), lit)
            kotlinx.coroutines.delay(33)
        }
    }

    private fun loadOrCreate(): Project {
        val loaded = runCatching { runBlocking(Dispatchers.IO) { store.load(PROJECT_ID) } }.getOrNull()
        if (loaded != null) return loaded
        var drums = DrumPattern()
        DrumKitInfo.starterHits.forEach { (lane, steps) -> steps.forEach { drums = drums.toggle(lane, it) } }
        return Project(name = "Mon premier rythme", drums = drums)
    }

    /** Sends the whole project to the engine, e.g. after the audio stream (re)started. */
    fun syncEngine() {
        if (!NativeEngine.isAvailable) return
        val p = project.value
        NativeEngine.setTempo(p.tempoBpm)
        NativeEngine.setSwing(p.swing)
        NativeEngine.setPatternSteps(p.drums.steps)
        p.drums.lanes.forEachIndexed { lane, l ->
            for (step in 0 until DrumPattern.MAX_STEPS) {
                NativeEngine.setDrumHit(lane, step, l.hits[step]?.velocity ?: 0f)
            }
        }
        NativeEngine.setMetronome(_transport.value.metronome)
    }

    fun togglePlay() {
        val t = _transport.value
        val playing = !t.playing
        _transport.value = t.copy(playing = playing)
        if (!NativeEngine.isAvailable) return
        if (playing) NativeEngine.play(if (t.countIn) 1 else 0) else NativeEngine.stopTransport()
    }

    fun toggleMetronome() {
        val on = !_transport.value.metronome
        _transport.value = _transport.value.copy(metronome = on)
        if (NativeEngine.isAvailable) NativeEngine.setMetronome(on)
    }

    fun toggleCountIn() {
        _transport.value = _transport.value.copy(countIn = !_transport.value.countIn)
    }

    /** Tapping a pad plays it and makes it the pad shown in the step grid. */
    fun hitPad(lane: Int) {
        _transport.value = _transport.value.copy(selectedPad = lane)
        if (NativeEngine.isAvailable) NativeEngine.triggerPad(lane, 0.9f)
    }

    fun toggleStep(step: Int) {
        val lane = _transport.value.selectedPad
        session.edit { it.copy(drums = it.drums.toggle(lane, step)) }
        pushLane(lane)
    }

    /** Long press: cycles the hit between strong, medium and soft. */
    fun cycleVelocity(step: Int) {
        val lane = _transport.value.selectedPad
        session.edit { p ->
            val current = p.drums.lanes[lane].hits[step]?.velocity
            val next = when {
                current == null -> 1f
                current > 0.8f -> 0.6f
                current > 0.45f -> 0.3f
                else -> 1f
            }
            val lanes = p.drums.lanes.toMutableList()
            lanes[lane] = lanes[lane].copy(hits = lanes[lane].hits + (step to Hit(next)))
            p.copy(drums = p.drums.copy(lanes = lanes))
        }
        pushLane(lane)
    }

    fun clearPad() {
        val lane = _transport.value.selectedPad
        session.edit { p ->
            val lanes = p.drums.lanes.toMutableList()
            lanes[lane] = lanes[lane].copy(hits = emptyMap())
            p.copy(drums = p.drums.copy(lanes = lanes))
        }
        pushLane(lane)
    }

    fun nudgeTempo(delta: Int) {
        session.edit("tempo") { it.copy(tempoBpm = Math.round(it.tempoBpm).toDouble() + delta) }
        if (NativeEngine.isAvailable) NativeEngine.setTempo(project.value.tempoBpm)
    }

    fun tapTempo() {
        val bpm = tapTempo.tap(System.currentTimeMillis()) ?: return
        session.edit("tempo") { it.copy(tempoBpm = Math.round(bpm).toDouble()) }
        if (NativeEngine.isAvailable) NativeEngine.setTempo(project.value.tempoBpm)
    }

    fun setSwing(swing: Double) {
        session.edit("swing") { it.copy(swing = swing) }
        if (NativeEngine.isAvailable) NativeEngine.setSwing(project.value.swing)
    }

    fun undo() {
        session.undo()
        syncEngine()
    }

    fun redo() {
        session.redo()
        syncEngine()
    }

    /** Writes pending changes now (the app is going to the background). */
    fun flush() {
        viewModelScope.launch { withContext(NonCancellable) { session.flush() } }
    }

    private fun pushLane(lane: Int) {
        if (!NativeEngine.isAvailable) return
        val hits = project.value.drums.lanes[lane].hits
        for (step in 0 until DrumPattern.MAX_STEPS) NativeEngine.setDrumHit(lane, step, hits[step]?.velocity ?: 0f)
    }

    override fun onCleared() {
        if (NativeEngine.isAvailable) NativeEngine.stopTransport()
    }

    companion object {
        const val PROJECT_ID = "projet-1"
    }
}
