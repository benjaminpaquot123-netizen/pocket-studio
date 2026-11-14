package com.pocketstudio.app.human

import android.os.Looper
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.test.junit4.createComposeRule
import com.pocketstudio.app.tuto.TutoHints
import org.junit.Assert.assertTrue
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/**
 * Tour 6, lot « stable »: the cause under the windows that « never came » (CI 43566247865: 12 reds, « field absent »,
 * « nothing to touch », « nothing tagged … after 15000 ms »; the shuffled suites' Coquille2SeatTest).
 *
 * Compose tells a state's readers it was written through one coroutine of the whole process, run on the main looper
 * (its « a run is posted » flags are the process's too). Robolectric empties the main looper between tests. A state
 * written after a test's last looper run (the bench putting back what a test left, a worker's last word) had its run
 * posted, then dropped with the queue: the flags stayed up, nothing was ever posted again, and from then on, in that
 * JVM, a state written outside a composition's own frame reached no reader.
 *
 * The first test leaves a state the bench puts back once it is over; the second, the test after it, writes a state as
 * a touch does and lets the main looper run: its reader must be told.
 */
@RunWith(RobolectricTestRunner::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class BenchSnapshotPumpTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun a_aTestLeavesAStateTheBenchPutsBackOnceItIsOver() {
        compose.setContent { Text("x") }
        compose.waitForIdle()
        TutoHints.active = true  // left: put back after this test (BenchPristine.restore), the looper not run again
    }

    @Test
    fun b_aStateWrittenInTheTestAfterItReachesItsReaders() {
        compose.setContent { Text("x") }
        compose.waitForIdle()
        var told = false
        val reader = Snapshot.registerApplyObserver { changed, _ -> if (written in changed) told = true }
        try {
            written.intValue++
            shadowOf(Looper.getMainLooper()).idle()
            assertTrue("a state written on the main thread, the main looper run: its readers are told (Compose's pump is alive)", told)
        } finally {
            reader.dispose()
        }
    }

    private companion object {
        val written = mutableIntStateOf(0)
    }
}
