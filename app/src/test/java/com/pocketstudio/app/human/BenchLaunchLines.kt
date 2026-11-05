package com.pocketstudio.app.human

import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.model.LiveSlots
import com.pocketstudio.model.liveLaunchSteps

/**
 * Audit ultra round 4 (seats B c, C c: Live's « ■ Tout » and a column's ■ stopped nothing, a block touched while the music
 * played never started): the Live page arms a launch or a stop for the next launch line and the ENGINE makes it there,
 * then reports it (NativeEngine.drainLaunchEvents, Tour4AppProofTest). The bench's engine is mostly let go and never had
 * launch lines: every armed block waited for good. Each tick, a slot that waits is launched (or stopped) on the first line
 * of its quantum after the step it was first seen waiting, as the engine reports it. Bench only.
 */
internal class BenchLaunchLines {
    /** The transport step each waiting slot was first seen at, and what it waited for. */
    private val seen = HashMap<Int, Pair<Int, Int>>()

    fun tick(vm: RhythmViewModel) {
        val grid = vm.liveGrid.value
        val clock = vm.takeClockForTest
        if (grid == null || !vm.transport.value.playing || clock < 0) {
            seen.clear()
            return
        }
        for (slot in 0 until LiveSlots.COUNT) {
            val s = grid.slot(slot)
            val pending = s.pending
            if (pending == null) {
                seen.remove(slot)
                continue
            }
            val (from, what) = seen[slot]?.takeIf { it.second == pending } ?: (clock to pending).also { seen[slot] = it }
            val q = s.quantum.takeIf { it > 0 } ?: vm.project.value.liveLaunchSteps(false).coerceAtLeast(1)
            val line = (from / q + 1) * q
            if (clock >= line) {
                seen.remove(slot)
                vm.liveLaunchedForTest(slot, what, line)
            }
        }
    }
}
