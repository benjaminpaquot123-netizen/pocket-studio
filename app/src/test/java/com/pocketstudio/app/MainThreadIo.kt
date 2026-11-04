package com.pocketstudio.app

import java.io.File
import java.nio.file.Files
import java.time.Duration

/**
 * Audit ultra §0 point 9, StrictMode in the tests: the disk and the network used on the main thread by
 * the app's own code, as Android's StrictMode (`detectDiskReads`, `detectDiskWrites`, `detectNetwork`)
 * would see them on a phone. Android's StrictMode itself sees nothing under Robolectric: the files and
 * sockets are the host JDK's, which never go through its BlockGuard. So the JDK's own flight recorder
 * (JFR: `jdk.FileRead`, `jdk.FileWrite`, `jdk.SocketRead`, `jdk.SocketWrite`, every one with its stack)
 * records each test; an event on the main thread whose nearest frame of ours is app code (not a test,
 * not a class or a resource being loaded) is a violation, named by that frame (« SettingsStore.load »).
 *
 * The violations accepted, each with its reason, are in `src/test/resources/audit/strictmode-acceptes.tsv`;
 * any other fails its test. Every violation seen is also written to `app/build/strictmode/<test>.tsv`.
 */
object MainThreadIo {
    private val OFF: Boolean get() = System.getProperty("ps.strict") == "off"
    /** The recording (a `jdk.jfr.Recording`, held through reflection: see [Jfr]). */
    private var recording: Any? = null

    /**
     * The JDK's flight recorder, reached through the platform class loader: Robolectric's sandbox would
     * otherwise define its own copy of `jdk.jfr`, whose native side then refuses to start.
     */
    private object Jfr {
        private val loader: ClassLoader = ClassLoader::class.java.getMethod("getPlatformClassLoader").invoke(null) as ClassLoader
        private fun cls(n: String) = Class.forName(n, true, loader)
        val recording = cls("jdk.jfr.Recording")
        val settings = cls("jdk.jfr.EventSettings")
        val file = cls("jdk.jfr.consumer.RecordingFile")
        fun start(events: Collection<String>): Any = recording.getConstructor().newInstance().also { r ->
            for (e in events) {
                val es = recording.getMethod("enable", String::class.java).invoke(r, e)
                settings.getMethod("withThreshold", Duration::class.java).invoke(es, Duration.ZERO)
                settings.getMethod("withStackTrace").invoke(es)
            }
            recording.getMethod("setToDisk", Boolean::class.javaPrimitiveType).invoke(r, false)
            recording.getMethod("start").invoke(r)
        }
        fun stop(r: Any) { recording.getMethod("stop").invoke(r) }
        fun dump(r: Any, to: java.nio.file.Path) { recording.getMethod("dump", java.nio.file.Path::class.java).invoke(r, to) }
        fun close(r: Any) { recording.getMethod("close").invoke(r) }
        @Suppress("UNCHECKED_CAST")
        fun events(from: java.nio.file.Path): List<Any> = file.getMethod("readAllEvents", java.nio.file.Path::class.java).invoke(null, from) as List<Any>
        fun call(o: Any?, m: String): Any? = o?.let { it.javaClass.getMethod(m).apply { isAccessible = true }.invoke(it) }
    }

    /** One event as read: its type, its thread, its frames (type, method), nearest first. */
    private class Event(val type: String, val thread: Long?, val frames: List<Pair<String, String>>)

    private fun eventOf(e: Any): Event {
        val type = Jfr.call(Jfr.call(e, "getEventType"), "getName") as String
        val thread = Jfr.call(Jfr.call(e, "getThread"), "getJavaThreadId") as Long?
        @Suppress("UNCHECKED_CAST")
        val frames = (Jfr.call(Jfr.call(e, "getStackTrace"), "getFrames") as List<Any>?).orEmpty().map { f ->
            val m = Jfr.call(f, "getMethod")
            (Jfr.call(Jfr.call(m, "getType"), "getName") as String) to (Jfr.call(m, "getName") as String)
        }
        return Event(type, thread, frames)
    }
    private var main: Long = -1

    /** Why the recorder could not start (null: it did, or it was not asked to). */
    var startError: Throwable? = null
        private set

    /** False: a test's straight call into the app counts too (the tests of this watch only). */
    @Volatile var excuseDirectCalls = true

    /** Whether the watch runs in this test. */
    val watching: Boolean get() = recording != null

    private val EVENTS = mapOf("jdk.FileRead" to "disk", "jdk.FileWrite" to "disk", "jdk.SocketRead" to "network", "jdk.SocketWrite" to "network")

    /** A frame that loads a class or a resource, or runs the test bench: the I/O is not the app's. */
    private val NOT_OURS = listOf(
        "java.lang.ClassLoader", "jdk.internal.loader.", "org.robolectric.", "android.content.res.", "java.util.ServiceLoader",
        "java.util.ResourceBundle", "sun.misc.", "jdk.internal.jimage", "org.junit.", "com.github.takahirom.roborazzi",
        "kotlin.reflect.", "java.util.zip.", "java.util.jar.", "androidx.test.", "androidx.compose.ui.test.",
    )

    class Accepted(val site: Regex, val kind: String, @Suppress("unused") val why: String)

    val ACCEPTED: List<Accepted> by lazy {
        val f = File("src/test/resources/audit/strictmode-acceptes.tsv")
        if (!f.exists()) emptyList() else f.readLines().filter { it.isNotBlank() && !it.startsWith("#") }.map { it.split('\t') }
            .map { Accepted(Regex(it[0]), it[1], it.getOrElse(2) { "" }) }
    }

    fun start() {
        if (OFF || recording != null) return
        main = android.os.Looper.getMainLooper().thread.id
        recording = runCatching { Jfr.start(EVENTS.keys) }.onFailure { startError = it }.getOrNull()
    }

    /** One violation: where (our nearest frame), what (disk, network), and the frames above it to read. */
    data class Violation(val site: String, val kind: String, val stack: String)

    fun stopAndCheck(test: String) {
        val r = recording ?: return
        recording = null
        val found = runCatching { read(r) }.getOrElse { emptyList() }
        if (found.isEmpty()) return
        val out = File("build/strictmode").apply { mkdirs() }
        File(out, "${test.replace(Regex("[^A-Za-z0-9_.-]"), "_")}.tsv").writeText(found.joinToString("") { "${it.site}\t${it.kind}\t$test\t${it.stack}\n" })
        val left = found.filter { v -> ACCEPTED.none { it.kind == v.kind && it.site.matches(v.site) } }
        if (left.isNotEmpty()) throw AssertionError(
            "StrictMode: ${left.size} use(s) of the ${left.map { it.kind }.distinct().joinToString()} on the main thread in $test (move it off the main " +
                "thread, or accept it with its reason in src/test/resources/audit/strictmode-acceptes.tsv):\n" +
                left.joinToString("\n") { "${it.kind} ${it.site} ← ${it.stack}" },
        )
    }

    /** Tests of this watch itself: what it has seen in the test so far, then a fresh start (nothing left to fail on). */
    fun seenAndForget(): List<Violation> {
        val r = recording ?: return emptyList()
        recording = null
        val found = read(r, keepAll = true)
        start()
        return found
    }

    /** Every main-thread event read (tests of the watch: what it sees before it filters). */
    var lastRaw: List<String> = emptyList()
        private set

    private fun read(r: Any, keepAll: Boolean = false): List<Violation> {
        val tmp = Files.createTempFile("strict", ".jfr")
        try {
            // An in-memory recording is dumped while it runs (stopped, it holds nothing to copy).
            Jfr.dump(r, tmp)
            Jfr.stop(r)
            val events = Jfr.events(tmp).map(::eventOf)
            if (keepAll) lastRaw = events.map { e -> "${e.type} t=${e.thread}/$main " + e.frames.take(6).joinToString(" < ") { "${it.first}.${it.second}" } }
            return events.mapNotNull(::violation).distinctBy { it.site to it.kind }
        } finally {
            Jfr.close(r)
            Files.deleteIfExists(tmp)
        }
    }

    private val testClass = HashMap<String, Boolean>()

    /** Whether [type] is test code (compiled with the unit tests), not the app's. */
    private fun isTest(type: String): Boolean = synchronized(testClass) {
        testClass.getOrPut(type) {
            val url = MainThreadIo::class.java.classLoader?.getResource(type.replace('.', '/') + ".class")?.toString().orEmpty()
            url.contains("UnitTest") || url.contains("/test/") || url.contains("test-classes")
        }
    }

    private fun violation(e: Event): Violation? {
        if (e.thread != main) return null
        val kind = EVENTS[e.type] ?: return null
        return classify(e.frames, kind)
    }

    /** [frames] (type, method; the I/O first) judged: the app's violation, or null (not the app's). */
    internal fun classify(frames: List<Pair<String, String>>, kind: String): Violation? {
        for ((i, f) in frames.withIndex()) {
            val type = f.first
            if (type == "java.lang.Class" || NOT_OURS.any { type.startsWith(it) }) return null
            // The generated decoders of the model's classes: the place is whoever asked for the decoding.
            if (!type.startsWith("com.pocketstudio.") || type.contains("\$\$serializer")) continue
            if (isTest(type)) return null
            // Called by a test straight (no Android or Compose frame between them, as a click, a frame or a
            // ViewModel made by its provider would put): the test's own use of the app's code, not the app's.
            val below = frames.drop(i + 1)
            val driver = below.indexOfFirst { (t, _) -> t.startsWith("android.") || t.startsWith("androidx.") || isTest(t) }
            if (excuseDirectCalls && driver >= 0 && isTest(below[driver].first)) return null
            val site = type.substringAfterLast('.').substringBefore('$') + "." + f.second
            val above = frames.take(i).takeLast(2).joinToString(" ← ") { it.first.substringAfterLast('.') + "." + it.second }
            return Violation(site, kind, above)
        }
        return null
    }
}
