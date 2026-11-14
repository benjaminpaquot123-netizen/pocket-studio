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
 * the last of several; a test without this rule run in between, in a full CI run, is named as the other suspect). The
 * state is then put back, so one leak is one red test and not the rest of the run.
 *
 * Review of banc45 (moyen 1): a real seat of the bench (`-Phuman.dir`, [HumanTester.drive]) is never made red by a leak
 * of the seat before it in the same fork (its journal would stay empty, the seat lost for the lot): [seatJournal] gives
 * its journal, the state is put back and the leak is written there as the bench's own incident, before « #0 SIEGE »
 * (`fuse-reports.py` lists that line in the round's incidents-banc.tsv).
 */
class BenchPristineRule(private val seatJournal: (Description) -> File? = { null }) : TestRule {
    override fun apply(base: Statement, description: Description): Statement = object : Statement() {
        override fun evaluate() {
            ran = true
            val leaks = BenchPristine.leaks()
            val before = lastTest
            lastTest = "${description.testClass?.simpleName ?: description.className}#${description.methodName}"
            if (leaks.isNotEmpty()) {
                BenchPristine.restore()
                // Two suspects (review of banc45, moyen 2): [lastTest] only knows the tests that carry this rule.
                val journal = seatJournal(description)
                if (journal != null) {
                    journal.parentFile?.mkdirs()
                    journal.appendText(
                        "#0 BANC : état hérité remis (laissé par le test d'avant, ${before ?: "aucun dans cette JVM"}, ou par un test sans la " +
                            "règle passé entre les deux) : ${leaks.joinToString("; ")}\n",
                    )
                } else {
                    throw AssertionError(
                        "inherited state not pristine before $lastTest (left by the test before (${before ?: "none in this JVM"}), " +
                            "or by a test without the rule run in between): " + leaks.joinToString("; "),
                    )
                }
            }
            base.evaluate()
        }
    }

    companion object {
        /** The last test this rule saw start (the class and method a leak is attributed to). */
        @Volatile var lastTest: String? = null
            private set

        /** Set when the rule runs around a test, lowered by [ranSinceLastAsked]. */
        @Volatile private var ran = false

        /**
         * Whether a test run since the last call carried this rule (`StrictTestApplication.afterTest` asks once a test is
         * over): the rule says it ran, whatever type the field holding it is declared with.
         */
        fun ranSinceLastAsked(): Boolean = ran.also { ran = false }
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

    /**
     * After a test that does not carry [BenchPristineRule] (`StrictTestApplication.afterTest`, every Robolectric test of the
     * app): its closing saves are waited for (as [HumanTester.end] does) and the state is put back. The first full run of
     * the suite with the detector (CI, 2026-11-10) had three bench tests red, each the first of the bench after ordinary
     * tests: 59 classes of the app's tests leave something (the ViewModel factory, a closing save, settings).
     */
    fun afterAnOrdinaryTest(test: String = "?") {
        closingSaves()
        restore()
        // Tour 6, lot « stable » (the conductor's request b): what restore() cannot put back is the app's work still running
        // on the coroutine workers; the next class inherits it, whatever it is. Given [WORK_END_CAP_MS] to end, the main looper
        // running; past that the test that leaves it is red, by name (one listener for every Robolectric test of the app:
        // this application's afterTest, robolectric.properties).
        if (!System.getProperty("ps.human.dir").isNullOrBlank()) {
            uncaughtError(test)?.let { println("BenchPristine: ${it.message}") }
            return
        }
        // Follow-up (CI 43531021739): the app's work left waiting on the main looper (a RhythmViewModel's live-gain estimate,
        // a delay loop) holds no worker; it is given the same time, then named and stopped. And an uncaught exception of the
        // app's coroutines is the red of the test it is thrown in, no more of the next one that opens a coroutines' test scope.
        if (!appWorkEnded(WORK_END_CAP_MS)) {
            val left = (appThreads() + appMainCoroutines()).joinToString(" | ")
            cancelAppMainCoroutines()
            throw AssertionError("$test leaves the app's work running to the test after it (${WORK_END_CAP_MS / 1000} s given; the main looper's stopped): $left")
        }
        uncaughtError(test)?.let { throw it }
    }

    /**
     * Compose keeps two things for the whole process on the main looper: the pump that tells a state's readers it was
     * written (GlobalSnapshotManager: one coroutine and its « sent » flag) and the dispatcher that pump runs on
     * (AndroidUiDispatcher.Main, with its « a run is posted » flags). Robolectric empties the main looper between tests.
     * A state written after a test's last looper run ([restore] itself, a worker's last word) had its run posted, then
     * dropped with the queue: the flags stayed up, nothing was posted again, and from then on, in that JVM, a state
     * written outside a composition's own frame reached no reader (CI 43566247865: windows that « never came », 12 reds).
     * `StrictTestApplication.beforeTest` calls this: the main queue is empty there, so a flag still up is a run that was
     * dropped; what it held is run now (in the order a frame runs it). True when something was revived.
     * [BenchSnapshotPumpTest] is the proof, and the alarm if these names change with Compose.
     */
    fun reviveComposePump(): Boolean = runCatching {
        val dispatcher = Class.forName("androidx.compose.ui.platform.AndroidUiDispatcher")
        val lazy = dispatcher.getDeclaredField("Main\$delegate").apply { isAccessible = true }.get(null) as Lazy<*>
        if (!lazy.isInitialized()) return false
        val main = (lazy.value as kotlin.coroutines.CoroutineContext)[kotlin.coroutines.ContinuationInterceptor] ?: return false
        fun up(flag: String) = dispatcher.getDeclaredField(flag).apply { isAccessible = true }.getBoolean(main)
        if (!up("scheduledTrampolineDispatch") && !up("scheduledFrameDispatch")) return false
        dispatcher.getDeclaredMethod("performTrampolineDispatch").apply { isAccessible = true }.invoke(main)
        dispatcher.getDeclaredMethod("performFrameDispatch", Long::class.javaPrimitiveType).apply { isAccessible = true }.invoke(main, System.nanoTime())
        true
    }.getOrElse { e ->
        println("BenchPristine: Compose's pump not revived (${(e as? java.lang.reflect.InvocationTargetException)?.targetException ?: e})")
        false
    }

    /** How long an ordinary test's leftover work is given to end before that test is red. */
    const val WORK_END_CAP_MS = 10_000L

    /**
     * The uncaught exceptions of coroutines (kotlinx-coroutines-test's collector), from now on kept here instead of waiting
     * for the next coroutines' test scope (« There were uncaught exceptions before the test started », a neighbour's red):
     * `StrictTestApplication.beforeTest` calls this; once per sandbox registers. Registering takes what was already waiting.
     */
    fun watchUncaught() {
        if (watching) return
        watching = true
        val collector = runCatching { Class.forName("kotlinx.coroutines.test.internal.ExceptionCollector") }.getOrNull() ?: return
        runCatching {
            val add = collector.getMethod("addOnExceptionCallback", Any::class.java, kotlin.jvm.functions.Function1::class.java)
            val record: (Throwable) -> Unit = { uncaught += it }
            add.invoke(staticField(collector, "INSTANCE"), UNCAUGHT_KEY, record)
        }.onFailure { e ->
            // What was already waiting comes back as one exception (UncaughtExceptionsBeforeTest, the others suppressed): kept.
            val cause = (e as? java.lang.reflect.InvocationTargetException)?.targetException ?: e
            if (cause.javaClass.simpleName == "UncaughtExceptionsBeforeTest") uncaught += cause else println("BenchPristine: uncaught exceptions not watched ($cause)")
        }
    }

    @Volatile private var watching = false
    private val UNCAUGHT_KEY = Any()
    private val uncaught = java.util.concurrent.ConcurrentLinkedQueue<Throwable>()

    /** The uncaught exceptions since the last call (during [test], or after the test before it ended), as [test]'s red. */
    fun uncaughtError(test: String): AssertionError? {
        val all = generateSequence { uncaught.poll() }.toList().flatMap { listOf(it) + it.suppressed }
        if (all.isEmpty()) return null
        val what = all.joinToString(" | ") { t ->
            val frames = t.stackTrace.filter { "pocketstudio" in it.className }.take(4)
                .joinToString(" < ") { "${it.className.substringAfterLast('.')}.${it.methodName}:${it.lineNumber}" }
            "${t.javaClass.name}: ${t.message} @ $frames"
        }
        return AssertionError("$test: uncaught exception(s) of coroutines during it (or after the test before it ended): $what")
            .also { e -> all.forEach { e.addSuppressed(it) } }
    }

    /**
     * The app's coroutines waiting on the main looper (a dispatch, a delay), each by its suspended frames in this package's
     * code: work left there holds no coroutine worker. Read from the main queue's messages (Robolectric's paused looper).
     */
    fun appMainCoroutines(): List<String> = mainCoroutineFrames().map { (_, frames) -> "main: $frames" }

    /** Stops the coroutines [appMainCoroutines] names (their jobs cancelled), the main looper run once. */
    private fun cancelAppMainCoroutines() {
        mainCoroutineFrames().forEach { (frame, _) -> (frame as? kotlin.coroutines.Continuation<*>)?.context?.get(kotlinx.coroutines.Job)?.cancel() }
        runCatching { org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle() }
    }

    /** Each coroutine of this package's code the main queue holds (its innermost frame), with those frames joined. */
    private fun mainCoroutineFrames(): List<Pair<kotlin.coroutines.jvm.internal.CoroutineStackFrame, String>> {
        val out = ArrayList<Pair<kotlin.coroutines.jvm.internal.CoroutineStackFrame, String>>()
        runCatching {
            val queue = android.os.Looper.getMainLooper().queue
            val messages = android.os.MessageQueue::class.java.getDeclaredField("mMessages").apply { isAccessible = true }
            val next = android.os.Message::class.java.getDeclaredField("next").apply { isAccessible = true }
            synchronized(queue) {
                var m = messages.get(queue) as android.os.Message?
                var n = 0
                while (m != null && n++ < 10_000) {
                    m.callback?.let { r -> coroutineIn(r, 2)?.let { f -> appFrames(f).takeIf { it.isNotEmpty() }?.let { out += f to it } } }
                    m = next.get(m) as android.os.Message?
                }
            }
        }
        return out
    }

    /** The coroutine a main-queue message resumes: the message's runnable itself (a dispatch) or one of its fields (a delay). */
    private fun coroutineIn(o: Any?, depth: Int): kotlin.coroutines.jvm.internal.CoroutineStackFrame? {
        if (o == null) return null
        if (o is kotlin.coroutines.jvm.internal.CoroutineStackFrame) return o
        if (depth == 0 || o.javaClass.name.startsWith("java.")) return null
        for (f in o.javaClass.declaredFields) {
            if (java.lang.reflect.Modifier.isStatic(f.modifiers) || f.type.isPrimitive) continue
            val v = runCatching { f.isAccessible = true; f.get(o) }.getOrNull()
            coroutineIn(v, depth - 1)?.let { return it }
        }
        return null
    }

    private fun appFrames(top: kotlin.coroutines.jvm.internal.CoroutineStackFrame): String {
        val frames = ArrayList<StackTraceElement>()
        var f: kotlin.coroutines.jvm.internal.CoroutineStackFrame? = top
        var n = 0
        while (f != null && n++ < 64) {
            f.getStackTraceElement()?.let { frames += it }
            f = f.callerFrame
        }
        return frames.filter { "pocketstudio" in it.className }.take(4).joinToString(" < ") { "${it.className.substringAfterLast('.')}.${it.methodName}:${it.lineNumber}" }
    }

    /** No coroutine worker in the app's code any more, waited for (the main looper running) [capMs] at most. */
    private fun appWorkEnded(capMs: Long): Boolean {
        val until = System.currentTimeMillis() + capMs
        while (appThreads().isNotEmpty() || appMainCoroutines().isNotEmpty()) {
            if (System.currentTimeMillis() >= until) return false
            runCatching { org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(10)) }
            Thread.sleep(10)
        }
        return true
    }

    /**
     * After a test that carries [BenchPristineRule] (`StrictTestApplication.afterTest`, its @After done): what it leaves is
     * its own red, the state put back (one leak, one red), instead of the next bench test's, which an ordinary test run in
     * between hid. Its install's files are its own (Robolectric gives the next test another data folder): not leaks. A real
     * seat (`-Phuman.dir`) is never made red: the next seat's rule puts it back and writes it in its journal.
     */
    fun afterATestWithTheRule(test: String) {
        if (!System.getProperty("ps.human.dir").isNullOrBlank()) {
            uncaughtError(test)?.let { println("BenchPristine: ${it.message}") }
            return
        }
        // Tour 6, lot « stable » (CI 43495469474): the compose rule closes the screen after the test's @After, and its closing
        // save may wait for the main looper (the save lock of an autosave that resumes there). Let it end, the looper running,
        // as after an ordinary test; a save still running after the cap is this test's leak, as before.
        closingSaves()
        val thrown = uncaughtError(test)
        val left = leaks().filterNot { it.startsWith("projects/") || it.startsWith("CurrentProject") }
        if (left.isEmpty()) {
            thrown?.let { throw it }
            return
        }
        restore()
        throw AssertionError("$test leaves to the test after it (put back): " + left.joinToString("; ")).also { e -> thrown?.let { e.addSuppressed(it) } }
    }

    /**
     * The closing saves of the test that ends, waited for while the main looper runs (they finish on it): review of the
     * « CI : fuite d'état » repair, mineur 3 (the wait blocked the main thread, each save gave up after 10 s). Said on the
     * test's output when they do not end within [capMs].
     */
    private fun closingSaves(capMs: Long = 15_000) {
        val until = System.currentTimeMillis() + capMs
        while (closingBusy() > 0 && System.currentTimeMillis() < until) {
            runCatching { org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(10)) }
            Thread.sleep(5)
        }
        if (closingBusy() > 0) println("BenchPristine: ${closingBusy()} closing save(s) still running after $capMs ms (put back all the same)")
    }

    private fun closingBusy(): Int =
        (staticField(com.pocketstudio.app.rhythm.ClosingSaves::class.java, "busy") as? java.util.concurrent.atomic.AtomicInteger)?.get() ?: 0

    /** Back to a just-started process (what [HumanTester.end] does, for the state a test can leak). */
    fun restore() {
        // Tour 6, lot « stable »: a closing save left busy was not put back (the 15 BenchSongClockTest reds after one leak).
        closingSaves()
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

    /** The uncaught exceptions kept for the next coroutines' test scope (kotlinx-coroutines-test), dropped: tests of the check. */
    fun forgetUncaughtForTests() {
        uncaught.clear()
        val collector = runCatching { Class.forName("kotlinx.coroutines.test.internal.ExceptionCollector") }.getOrNull() ?: return
        val lock = staticField(collector, "lock") ?: return
        synchronized(lock) { (staticField(collector, "unprocessedExceptions") as? MutableList<*>)?.clear() }
    }

    private fun staticField(cls: Class<*>, name: String): Any? =
        runCatching { cls.getDeclaredField(name).apply { isAccessible = true }.get(null) }.getOrNull()

    private fun setStaticField(cls: Class<*>, name: String, value: Any?) {
        cls.getDeclaredField(name).apply { isAccessible = true }.set(null, value)
    }
}
