package com.pocketstudio.app.human

import com.pocketstudio.app.rhythm.LiveUi
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.model.sceneSteps
import com.pocketstudio.model.songBars
import com.pocketstudio.model.songStepOf
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * The play head as a phone's engine moves it (lot « tuto 4 », É0): without the native engine the
 * app never hears where the music is, so « mes. 1/8 » and the grid's head stood still while the
 * tester heard the loop turn. Each [tick] puts the bar and the step the tempo has reached since ▶
 * into the ViewModel's own flows (what its engine poll would write). Bench only.
 */
internal class BenchPlayhead {
    private var since = -1L

    fun tick(vm: RhythmViewModel, nowMs: Long) {
        if (!vm.transport.value.playing) {
            since = -1L
            vm.takeClockForTest = -1
            return
        }
        if (since < 0) since = nowMs
        val p = vm.project.value
        val spb = p.stepsPerBar.coerceAtLeast(1)
        val engine = NativeEngine.sink
        // Audit « banc crédible » A.9 (F4): one clock. With the bench's engine there (always, unless « ps.human.moteur=liste »
        // lets it go), every number comes from it, as the app's own polls read them on a phone: the count-in (no step yet), a
        // tempo change (the steps played are kept). The head's own count below is only for the app with no engine at all.
        val steps = engine?.transportStep() ?: ((nowMs - since) * p.tempoBpm / 15_000.0).toInt()  // a sixteenth = 15/bpm s
        // Audit ultra tour 0 (seat c): « Enregistrer la performance » reads the transport's steps since ▶ to start
        // on its launch line; without them it stayed « Prêt » for good.
        vm.takeClockForTest = steps
        val songIndex = vm.sessionUi.value.songIndex
        if (engine != null) {
            // PLAN-RESTE R.1 (tel-09; T4-16, T5-Ab-14) then A.15 (F5): nothing is counted again here. What the app's pollLive
            // writes on a phone, from the same reads: the drum pattern's step, the position in the part playing, the song's
            // position; and the bar as its pollPosition says it (the song's, else the part's loop).
            val songStep = engine.songStep()
            val at = engine.absoluteStep()
            val bar = if (songIndex >= 0) (if (songStep < 0) -1 else songStep / spb) else if (at < 0) -1 else at % p.sceneSteps(p.scene).coerceAtLeast(1) / spb
            set(vm, "_playBar", bar)
            set(vm, "_live", LiveUi(currentStep = engine.currentStep(), absoluteStep = at, songStep = songStep))
        } else if (songIndex >= 0 && songIndex < p.song.size) {
            val songStep = (p.songStepOf(songIndex) + steps).coerceAtMost(p.songBars.coerceAtLeast(1) * spb - 1)
            set(vm, "_playBar", songStep / spb)
            set(vm, "_live", LiveUi(currentStep = songStep - p.songStepOf(songIndex), absoluteStep = songStep, songStep = songStep))
        } else {
            val loop = p.sceneSteps(p.scene).coerceAtLeast(1)
            set(vm, "_playBar", steps % loop / spb)
            set(vm, "_live", LiveUi(currentStep = steps % loop, absoluteStep = steps))
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun set(vm: RhythmViewModel, field: String, value: Any) {
        val f = RhythmViewModel::class.java.getDeclaredField(field).apply { isAccessible = true }
        (f.get(vm) as MutableStateFlow<Any>).value = value
    }
}
