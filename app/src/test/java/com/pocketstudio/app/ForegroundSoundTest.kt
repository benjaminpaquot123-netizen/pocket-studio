package com.pocketstudio.app

import android.app.Application
import android.os.Looper
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.onboarding.LaunchRule
import com.pocketstudio.app.report.AppLog
import com.pocketstudio.app.settings.SettingsStore
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File
import java.time.Duration

/**
 * The CI emulator (2026-11-04): on a phone just set up, the app never opened its sound (no
 * « Premier plan » in its log), so ▶ played nothing. The screens open once the project is read in
 * the background (ProjectGate): the app root must ask for the sound then, even though the activity
 * was already in the foreground when it appeared. Checked through the app's log: here there is no
 * native engine, so it says « son NON ouvert (moteur absent) », but it must say it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h890dp")
class ForegroundSoundTest {
    // Compose's own clock and idling (its endless animations would keep a bare ActivityScenario waiting).
    @get:Rule
    val compose = createEmptyComposeRule()

    private val app: Application get() = ApplicationProvider.getApplicationContext()
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun freshInstall() {
        for (dir in listOf("projects", "projects-trash", "stems")) File(app.filesDir, dir).deleteRecursively()
        File(app.filesDir, "../shared_prefs").listFiles()?.forEach { app.getSharedPreferences(it.name.removeSuffix(".xml"), 0).edit().clear().commit() }
        app.getSharedPreferences("settings", 0).edit().putString("language", "fr").commit()
        SettingsStore.forgetForTests()
        LaunchRule.askAtStartForTest = true
        shadowOf(app).grantPermissions(android.Manifest.permission.RECORD_AUDIO)
        ViewModelProvider.AndroidViewModelFactory::class.java.getDeclaredField("_instance").apply { isAccessible = true }.set(null, null)
        AppLog.clear()
    }

    @After
    fun end() {
        runCatching { scenario?.close() }
        com.pocketstudio.app.rhythm.ClosingSaves.awaitIdleBlocking(15_000)
        LaunchRule.askAtStartForTest = true
        SettingsStore.forgetForTests()
    }

    /** Lets the app run (Compose's clock and the main looper, as the journeys do) until its log says [what]. */
    private fun awaitLog(what: String, seconds: Int = 60): Boolean =
        runCatching { compose.waitUntil(seconds * 1000L) { AppLog.dump(200).contains(what) } }.isSuccess

    @Test
    fun aFirstLaunchAsksForTheSound() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        assertTrue("no « Premier plan » in the app's log:\n${AppLog.dump(40)}", awaitLog("Premier plan"))
    }

    @Test
    fun theAppOpenedAgainAsksForTheSoundAgain() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        assertTrue("first launch:\n${AppLog.dump(40)}", awaitLog("Premier plan"))
        scenario!!.close()
        com.pocketstudio.app.rhythm.ClosingSaves.awaitIdleBlocking(15_000)
        AppLog.clear()
        scenario = ActivityScenario.launch(MainActivity::class.java)
        assertTrue("second launch:\n${AppLog.dump(40)}", awaitLog("Premier plan"))
    }
}
