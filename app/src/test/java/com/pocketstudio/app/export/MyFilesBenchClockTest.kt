package com.pocketstudio.app.export

import android.net.Uri
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import com.pocketstudio.ui.PocketStudioTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Lot « coquille3 », control seat Cq3 (screens 14-16, 60-62: « Chargement de la liste… » for 95 s right after a send):
 * the proof that this is the bench's clock, not the app. A read of the phone's files that takes real time (here 1,5 s
 * of the wall, off the main thread, as `MyExports.list` is) is still « Chargement » after 95 s of the test's clock (the
 * clock the bench's « attends » moves), and the list comes once real time has passed. On a phone, time is the wall's.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h780dp-xxhdpi")
class MyFilesBenchClockTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun aSlowReadIsStillLoadingAfterTheBenchsSecondsAndComesInRealTime() {
        val file = MyExport("Démo-Ac.m4a", Uri.parse("content://media/1"), 900_000, 1_794_046_502, null)
        compose.mainClock.autoAdvance = false
        compose.setContent { PocketStudioTheme { MyExportsDialog(read = { withContext(Dispatchers.IO) { Thread.sleep(1_500); listOf(file) } }) {} } }
        // The seat's « attends 30 » then « attends 60 », as the bench moves its clock.
        compose.mainClock.advanceTimeBy(95_000)
        assertTrue("after 95 s of the test's clock the slow read was already done (no proof)",
            compose.onAllNodesWithTag("my_exports_wait").fetchSemanticsNodes().isNotEmpty())
        // Real time given: the list comes, the sign of waiting goes.
        compose.mainClock.autoAdvance = true
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("my_export").fetchSemanticsNodes().isNotEmpty() }
        assertTrue(compose.onAllNodesWithTag("my_exports_wait").fetchSemanticsNodes().isEmpty())
    }
}
