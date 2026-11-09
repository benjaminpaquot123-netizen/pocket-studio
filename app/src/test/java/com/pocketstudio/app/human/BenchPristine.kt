package com.pocketstudio.app.human

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.onboarding.LaunchRule
import com.pocketstudio.app.projects.CurrentProject
import com.pocketstudio.app.remix.SeparationJob
import com.pocketstudio.app.remix.SeparationState
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.app.settings.AppSettings
import com.pocketstudio.app.settings.SettingsStore
import com.pocketstudio.app.tuto.TutoTestHooks
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.io.File

/**
 * PLAN-RESTE 4.5: the leak detector, instead of a bisection. Before every test of the bench ([HumanTester]) and of the
 * `Tour*Test` classes, the process-wide state a test inherits from the one before (the app's objects, the bench's
 * stand-ins, the files of the install) must be pristine: the app's objects are statics of Robolectric's sandbox, which
 * the tests of one Gradle fork share, so what a test forgets to put back is what the next one starts on.
 *
 * Not pristine: the test fails at once, naming what leaked and the test that ran before ([lastTest], the culprit, or
 * the last of several). The state is then put back, so one leak is one red test and not the rest of the run.
 */
class BenchPristineRule : TestRule {
    override fun apply(base: Statement, description: Description): Statement = object : Statement() {
        override fun evaluate() {
            val leaks = BenchPristine.leaks()
            val before = lastTest
            lastTest = "${description.testClass?.simpleName ?: description.className}#${description.methodName}"
            if (leaks.isNotEmpty()) {
                BenchPristine.restore()
                throw AssertionError(
                    "inherited state not pristine before $lastTest (left by the test before: ${before ?: "none in this JVM"}): " +
                        leaks.joinToString("; "),
                )
            }
            base.evaluate()
        }
    }

    companion object {
        /** The last test this rule saw start (the class and method a leak is attributed to). */
        @Volatile var lastTest: String? = null
            private set
    }
}

internal object BenchPristine {
    private val app: Application get() = ApplicationProvider.getApplicationContext()

    /** What is not as a just-started process has it, one line each (empty: pristine). */
    fun leaks(): List<String> {
        val out = ArrayList<String>()
        fun check(what: String, ok: Boolean, value: () -> Any? = { "" }) {
            if (!ok) out += "$what = ${runCatching { value() }.getOrElse { "?" }}"
        }
        check("LaunchRule.askAtStartForTest", LaunchRule.askAtStartForTest) { false }
        // The Application's onCreate loaded the settings of this test's (fresh) install before this check: loaded is normal,
        // the state must be the defaults, and Retours follows it (on by default in this build).
        check("SettingsStore.state", SettingsStore.state.value == AppSettings()) { SettingsStore.state.value }
        check("SettingsStore.favouriteFx", SettingsStore.favouriteFx.value.isEmpty()) { SettingsStore.favouriteFx.value }
        check("SeparationJob.state", SeparationJob.state.value is SeparationState.Idle) { SeparationJob.state.value }
        check("I18n.lang", I18n.lang == Lang.FR) { I18n.lang }
        check("I18n.everyday", !I18n.everyday) { true }
        check("I18n.pseudo", I18n.pseudo == null) { I18n.pseudo }
        check("TutoTestHooks.player", TutoTestHooks.player == null) { TutoTestHooks.player }
        check("TutoTestHooks.kitOffer", TutoTestHooks.kitOffer) { false }
        check("TutoTestHooks.packs", TutoTestHooks.packs == null) { TutoTestHooks.packs }
        check("NativeEngine.sink", NativeEngine.sink == null) { NativeEngine.sink }
        check("Retours.active", com.pocketstudio.app.retours.Retours.active == SettingsStore.state.value.retoursMode) { com.pocketstudio.app.retours.Retours.active }
        check("Intro.playing", !com.pocketstudio.app.onboarding.Intro.playing) { true }
        check("TutoHints.active", !com.pocketstudio.app.tuto.TutoHints.active) { true }
        check("SpotChecks.log", com.pocketstudio.app.learn.SpotChecks.log == null) { com.pocketstudio.app.learn.SpotChecks.log }
        check("ProjectOpening.prepared", staticField(com.pocketstudio.app.rhythm.ProjectOpening::class.java, "prepared") == null) {
            staticField(com.pocketstudio.app.rhythm.ProjectOpening::class.java, "prepared")
        }
        check("ClosingSaves busy", ((staticField(com.pocketstudio.app.rhythm.ClosingSaves::class.java, "busy") as? java.util.concurrent.atomic.AtomicInteger)?.get() ?: 0) == 0) {
            staticField(com.pocketstudio.app.rhythm.ClosingSaves::class.java, "busy")
        }
        check("AndroidViewModelFactory._instance", staticField(ViewModelProvider.AndroidViewModelFactory::class.java, "_instance") == null) {
            staticField(ViewModelProvider.AndroidViewModelFactory::class.java, "_instance")
        }
        check("BenchMic.opened", BenchMic.opened == 0) { BenchMic.opened }
        // A touch guard armed by the test before (its uptime): a touch of this one at that place could be dropped as a double.
        val guard = com.pocketstudio.app.ui.DoubleTouchGuard::class.java
        check(
            "DoubleTouchGuard",
            staticField(guard, "downAt") == Long.MIN_VALUE && staticField(guard, "armedUntil") == Long.MIN_VALUE && staticField(guard, "dropping") == false,
        ) { "downAt=${staticField(guard, "downAt")} armedUntil=${staticField(guard, "armedUntil")} dropping=${staticField(guard, "dropping")}" }
        // A take of the test before still being written: the next opening waits for it (ProjectOpening.prepare, 10 s).
        check("TakeVault.app busy", takeVaultBusy() <= 0) { takeVaultBusy() }
        // Work of the test before still on the coroutine workers (given 2 s to park), running or blocked in the app's code: the
        // next test's clock and its waits are not its own any more, and enough of them starve Dispatchers.IO (run A of
        // banc45: the projects list of Tour3RenameIdleTest never finished loading).
        check("coroutine workers", workersParked()) { appThreads().joinToString(" | ") + " (${workerCount()} workers live)" }
        // The install's files (Robolectric gives each test a data folder of its own: a leak here is a bench's shortcut).
        val projects = File(app.filesDir, "projects")
        check("projects/", !projects.exists() || projects.listFiles().isNullOrEmpty()) { projects.listFiles()?.map { it.name } }
        check("CurrentProject", CurrentProject.get(app) == RhythmViewModel.PROJECT_ID) { CurrentProject.get(app) }
        return out
    }

    /** Back to a just-started process (what [HumanTester.end] does, for the state a test can leak). */
    fun restore() {
        LaunchRule.askAtStartForTest = true
        SettingsStore.forgetForTests()
        SeparationJob.set(SeparationState.Idle)
        I18n.lang = Lang.FR
        I18n.everyday = false
        I18n.pseudo = null
        TutoTestHooks.player = null
        TutoTestHooks.kitOffer = true
        TutoTestHooks.packs = null
        NativeEngine.sink = null
        com.pocketstudio.app.retours.Retours.active = SettingsStore.state.value.retoursMode
        com.pocketstudio.app.onboarding.Intro.playing = false
        com.pocketstudio.app.tuto.TutoHints.active = false
        com.pocketstudio.app.learn.SpotChecks.log = null
        runCatching { setStaticField(com.pocketstudio.app.rhythm.ProjectOpening::class.java, "prepared", null) }
        runCatching { setStaticField(ViewModelProvider.AndroidViewModelFactory::class.java, "_instance", null) }
        BenchMic.unplug()
        com.pocketstudio.app.ui.DoubleTouchGuard.reset()
        com.pocketstudio.app.rhythm.TakeVault.app.awaitIdleBlocking(15_000)
        File(app.filesDir, "projects").deleteRecursively()
        CurrentProject.set(app, RhythmViewModel.PROJECT_ID)
    }

    private fun takeVaultBusy(): Int =
        ((staticFieldOf(com.pocketstudio.app.rhythm.TakeVault.app, "busy") as? kotlinx.coroutines.flow.StateFlow<*>)?.value as? Int) ?: 0

    private fun staticFieldOf(obj: Any, name: String): Any? =
        runCatching { obj.javaClass.getDeclaredField(name).apply { isAccessible = true }.get(obj) }.getOrNull()

    /** The coroutine workers (Dispatchers.Default and IO) free of the app's work, waited for 2 s at most. */
    private fun workersParked(): Boolean {
        val until = System.currentTimeMillis() + 2_000
        while (System.currentTimeMillis() < until) {
            if (appThreads().isEmpty()) return true
            Thread.sleep(50)
        }
        return appThreads().isEmpty()
    }

    private fun liveThreads(): List<Thread> {
        var root = Thread.currentThread().threadGroup
        while (root.parent != null) root = root.parent
        val threads = arrayOfNulls<Thread>(root.activeCount() + 16)
        val n = root.enumerate(threads, true)
        return (0 until n).mapNotNull { threads[it] }
    }

    /** How many coroutine workers live (Dispatchers.IO stalls once 64 of them are held). */
    fun workerCount(): Int = liveThreads().count { it.name.startsWith("DefaultDispatcher-worker") }

    /**
     * The coroutine workers the app's code holds, running or blocked (a parked idle worker has none of its frames), each with
     * its state and the top of its stack: what the test before left on them. Also what a failing test prints to name them.
     */
    fun appThreads(): List<String> = liveThreads().mapNotNull { t ->
        if (!t.name.startsWith("DefaultDispatcher-worker") || t === Thread.currentThread()) return@mapNotNull null
        val frames = t.stackTrace.filter { "pocketstudio" in it.className }.take(4).joinToString(" < ") { "${it.className.substringAfterLast('.')}.${it.methodName}:${it.lineNumber}" }
        if (frames.isEmpty()) null else "${t.name} ${t.state}: $frames"
    }

    private fun staticField(cls: Class<*>, name: String): Any? =
        runCatching { cls.getDeclaredField(name).apply { isAccessible = true }.get(null) }.getOrNull()

    private fun setStaticField(cls: Class<*>, name: String, value: Any?) {
        cls.getDeclaredField(name).apply { isAccessible = true }.set(null, value)
    }
}
