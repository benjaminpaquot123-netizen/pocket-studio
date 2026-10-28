package com.pocketstudio.app.human

import com.pocketstudio.app.rhythm.LiveUi
import com.pocketstudio.app.rhythm.RhythmViewModel
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
            return
        }
        if (since < 0) since = nowMs
        val p = vm.project.value
        val spb = p.stepsPerBar.coerceAtLeast(1)
        val steps = ((nowMs - since) * p.tempoBpm / 15_000.0).toInt()  // a sixteenth = 15/bpm s
        val songIndex = vm.sessionUi.value.songIndex
        if (songIndex >= 0 && songIndex < p.song.size) {
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
