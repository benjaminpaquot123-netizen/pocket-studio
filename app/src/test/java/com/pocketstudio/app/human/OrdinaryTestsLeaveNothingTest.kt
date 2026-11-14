package com.pocketstudio.app.human

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.onboarding.LaunchRule
import com.pocketstudio.app.rhythm.ClosingSaves
import com.pocketstudio.app.settings.SettingsStore
import com.pocketstudio.model.i18n.I18n
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import org.robolectric.RobolectricTestRunner

/**
 * The full suite (CI, 2026-11-10: three bench tests red at BenchPristine.kt:54, each the first of the bench run after the
 * app's ordinary tests). A test that does not carry [BenchPristineRule] leaves what the app's tests leave (59 classes of
 * them, seen in a full run): the ViewModel factory that keeps its Application (any screen opened), a save still closing
 * (a project's screen closed), settings changed, the launch rule off. The test after it must start on a pristine process.
 *
 * On purpose without the rule: this class is the ordinary test. Its tests run in the order of their names.
 */
@RunWith(RobolectricTestRunner::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class OrdinaryTestsLeaveNothingTest {
    private val app: Application get() = ApplicationProvider.getApplicationContext()

    @Test
    fun a_anOrdinaryTestLeavesWhatTheAppsTestsLeave() {
        ViewModelProvider.AndroidViewModelFactory.getInstance(app)  // as any screen with a ViewModel (603 times in a full run)
        ClosingSaves.launch { delay(400) }  // as a project's screen closed with edits to write (GuideAloneTest, SimpleMixTest...)
        SettingsStore.update(app) { it.copy(simpleMode = false, guideSeen = true) }  // as ShellFixesTest, OnboardingUiTest
        I18n.everyday = true  // as OnboardingUiTest
        LaunchRule.askAtStartForTest = false  // as SpacesRestoreTest
        val left = BenchPristine.leaks().joinToString("; ")
        for (what in listOf("AndroidViewModelFactory._instance", "ClosingSaves busy", "SettingsStore.state", "I18n.everyday", "LaunchRule.askAtStartForTest")) {
            assertTrue("this test must leave $what for the next one to be a proof: $left", what in left)
        }
    }

    @Test
    fun b_theTestAfterAnOrdinaryTestStartsPristine() {
        assertEquals("left by the ordinary test before", emptyList<String>(), BenchPristine.leaks())
    }

    /**
     * Tour 6, lot « stable » (the conductor's request b): the one thing restore() cannot put back is the app's work still
     * running on the coroutine workers; the next class inherits it, whatever it is. An ordinary test that leaves such work
     * past the time it is given is red itself, by name (StrictTestApplication.afterTest, every Robolectric test).
     */
    @Test
    fun c_anOrdinaryTestThatLeavesTheAppsWorkRunningIsRedItself() {
        val stop = java.util.concurrent.atomic.AtomicBoolean(false)
        // A worker kept in this package's code (com.pocketstudio…) far past the time given, as a render or a fetch left on.
        @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.Default) { while (!stop.get()) Thread.sleep(20) }
        try {
            val m = javaClass.getMethod("c_anOrdinaryTestThatLeavesTheAppsWorkRunningIsRedItself")
            val e = runCatching { (app as com.pocketstudio.app.StrictTestApplication).afterTest(m) }.exceptionOrNull()
            assertTrue("red itself, by name: $e", e is AssertionError && "c_anOrdinaryTestThatLeavesTheAppsWorkRunningIsRedItself" in e.message.orEmpty())
            assertTrue("naming the work: ${e?.message}", "OrdinaryTestsLeaveNothingTest" in e?.message.orEmpty() && "worker" in e?.message.orEmpty())
        } finally {
            stop.set(true)
        }
    }

    /**
     * Tour 6, lot « stable », follow-up (CI 43531021739: LibraryRound2Zone1Test red with UncaughtExceptionsBeforeTest, an
     * UnsatisfiedLinkError thrown by a RhythmViewModel's live-gain estimate that a class run before had left waiting on the
     * main looper). Work of the app left suspended on the main thread (a delay, a dispatch) holds no coroutine worker: the
     * check did not see it. The test that leaves it is red itself, by name, and the work is stopped.
     */
    @Test
    fun d_anOrdinaryTestThatLeavesTheAppsWorkOnTheMainLooperIsRedItself() {
        // A coroutine of this package (com.pocketstudio…) waiting on the main looper far past the time given, as an estimate
        // whose sounds never come.
        @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
        val job = kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.Main) { while (true) delay(50) }
        try {
            val m = javaClass.getMethod("d_anOrdinaryTestThatLeavesTheAppsWorkOnTheMainLooperIsRedItself")
            val e = runCatching { (app as com.pocketstudio.app.StrictTestApplication).afterTest(m) }.exceptionOrNull()
            assertTrue("red itself, by name: $e", e is AssertionError && "d_anOrdinaryTestThatLeavesTheAppsWorkOnTheMainLooperIsRedItself" in e.message.orEmpty())
            assertTrue("naming the work on the main looper: ${e?.message}", "main" in e?.message.orEmpty() && "OrdinaryTestsLeaveNothingTest" in e?.message.orEmpty())
            assertTrue("the work left is stopped (the next class does not inherit it)", job.isCancelled)
        } finally {
            job.cancel()
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        }
    }

    /**
     * Same CI: the exception of work an ordinary test left was reported by the next test that runs coroutines' test scope
     * (« There were uncaught exceptions before the test started »), a neighbour. An uncaught exception of the app's
     * coroutines makes red the test during which it is thrown, by name.
     */
    @Test
    fun e_anUncaughtExceptionOfTheAppsWorkMakesItsOwnTestRed() {
        @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
        val job = kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.Default) { throw IllegalStateException("left by e_") }
        try {
            kotlinx.coroutines.runBlocking { job.join() }
            val m = javaClass.getMethod("e_anUncaughtExceptionOfTheAppsWorkMakesItsOwnTestRed")
            val e = runCatching { (app as com.pocketstudio.app.StrictTestApplication).afterTest(m) }.exceptionOrNull()
            assertTrue("red itself, by name: $e", e is AssertionError && "e_anUncaughtExceptionOfTheAppsWorkMakesItsOwnTestRed" in e.message.orEmpty())
            assertTrue("naming the exception: ${e?.message}", "IllegalStateException" in e?.message.orEmpty() && "left by e_" in e?.message.orEmpty())
        } finally {
            // Before the repair the exception waits for the next coroutines' test scope (a neighbour): not left to it.
            BenchPristine.forgetUncaughtForTests()
        }
    }
}
