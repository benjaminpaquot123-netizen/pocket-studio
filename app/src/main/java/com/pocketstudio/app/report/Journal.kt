package com.pocketstudio.app.report

import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

/** One line of the [Journal]: when ([atMs], wall clock), what kind, and what. */
data class JournalEvent(val atMs: Long, val kind: Kind, val text: String) {
    enum class Kind(val label: String) {
        /** A space, a screen, a sheet opened. */
        NAV("écran"),
        /** What the user did, coarsely (play, stop, record, undo, an intention…). */
        ACTION("geste"),
        /** The app's own journal ([AppLog]): sound opened, saved, exported… */
        APP("appli"),
        /** An error caught, or a crash. */
        ERROR("erreur"),
    }
}

/**
 * What happened lately, for « Signaler un bug »: the last [EVENTS] events and the last [ERRORS]
 * errors, in memory only (bounded rings). Nothing leaves the phone unless the user shares a
 * report. Snapshots go to disk only off the hot paths ([JournalFiles]): every few seconds while
 * something changes, and once in the crash handler.
 */
object Journal {
    const val EVENTS = 300
    const val ERRORS = 40

    val events = Ring<JournalEvent>(EVENTS)
    val errors = Ring<JournalEvent>(ERRORS)

    /** Changes with every event: a saver writes only when it moved. */
    val version: Long get() = counter.get()
    private val counter = AtomicLong()

    /** Wall clock, replaced by tests. */
    @Volatile var clock: () -> Long = System::currentTimeMillis

    @Volatile private var lastScreen: String? = null

    /** A screen, a space or a sheet shown; the same one again in a row is not repeated. */
    fun screen(where: String) {
        if (where == lastScreen) return
        lastScreen = where
        add(JournalEvent.Kind.NAV, where)
    }

    fun action(what: String) = add(JournalEvent.Kind.ACTION, what)

    /** A line of [AppLog]; its failures (« impossible », « échec »…) also count as errors. */
    fun app(line: String) {
        add(JournalEvent.Kind.APP, line)
        if (looksLikeError(line)) errors.add(JournalEvent(clock(), JournalEvent.Kind.ERROR, LogTail.redact(line).take(MAX_TEXT)))
    }

    fun error(where: String, message: String) {
        val e = JournalEvent(clock(), JournalEvent.Kind.ERROR, LogTail.redact("$where : $message").take(MAX_TEXT))
        events.add(e)
        errors.add(e)
        counter.incrementAndGet()
    }

    /** An exception caught: its type, message and the first lines of its stack. */
    fun error(where: String, t: Throwable) = error(where, describe(t))

    private fun add(kind: JournalEvent.Kind, text: String) {
        events.add(JournalEvent(clock(), kind, LogTail.redact(text).take(MAX_TEXT)))
        counter.incrementAndGet()
    }

    internal fun clear() {
        events.clear()
        errors.clear()
        lastScreen = null
        counter.incrementAndGet()
    }

    private val failure = Regex("impossible|échec|échoué|illisible|refusé|erreur|plantage|exception|introuvable|NaN", RegexOption.IGNORE_CASE)
    fun looksLikeError(line: String): Boolean = failure.containsMatchIn(line)

    fun describe(t: Throwable, lines: Int = 8): String {
        val trace = StringWriter().also { t.printStackTrace(PrintWriter(it)) }.toString()
        return LogTail.redact(trace.lineSequence().take(lines).joinToString("\n").trim())
    }

    const val MAX_TEXT = 600
}

/** A tiny JSON writer: the journal files need no library (strings, numbers, arrays, objects). */
object JsonText {
    fun string(s: String): String = buildString(s.length + 2) {
        append('"')
        for (c in s) when (c) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> if (c < ' ') append(String.format(Locale.ROOT, "\\u%04x", c.code)) else append(c)
        }
        append('"')
    }

    fun number(v: Float): String = if (v.isFinite()) String.format(Locale.ROOT, "%.4g", v) else "null"

    fun obj(vararg fields: Pair<String, String?>): String =
        fields.filter { it.second != null }.joinToString(",", "{", "}") { (k, v) -> string(k) + ":" + v }

    fun array(items: List<String>): String = items.joinToString(",", "[", "]")
}

/**
 * The journal as journal.json: the events and errors (time, kind, text) and the health readings of
 * the last minute; [extra] fields first (the app, the device, the crash).
 */
object JournalJson {
    /** One formatter for every line (thread-safe, unlike SimpleDateFormat). */
    private val stamp = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.ROOT).withZone(ZoneId.systemDefault())
    private fun time(ms: Long) = stamp.format(Instant.ofEpochMilli(ms))

    fun event(e: JournalEvent): String = JsonText.obj("t" to JsonText.string(time(e.atMs)), "kind" to JsonText.string(e.kind.name.lowercase()), "text" to JsonText.string(e.text))

    fun sample(s: HealthSample): String = JsonText.obj(
        "t" to JsonText.string(time(s.atMs)),
        "peakDb" to JsonText.number(HealthSummary.db(s.peak)),
        "shortTermLufs" to JsonText.number(s.shortTermLufs),
        "momentaryLufs" to JsonText.number(s.momentaryLufs),
        "limiterDb" to JsonText.number(HealthSummary.db(s.limiterGain)),
        "cpu" to JsonText.number(s.cpu),
        "xruns" to s.xruns.toString(),
        "restarts" to s.restarts.toString(),
        "running" to s.running.toString(),
        "nonFinite" to s.nonFinite.toString(),
        "playing" to s.playing.toString(),
        "masterLane" to s.masterLane?.let(JsonText::number),
    )

    fun build(events: List<JournalEvent>, errors: List<JournalEvent>, health: List<HealthSample>, extra: List<Pair<String, String>> = emptyList()): String {
        val fields = extra.map { (k, v) -> k to JsonText.string(v) } + listOf(
            "events" to JsonText.array(events.map(::event)),
            "errors" to JsonText.array(errors.map(::event)),
            "health" to JsonText.array(health.map(::sample)),
        )
        return JsonText.obj(*fields.toTypedArray())
    }

    fun current(extra: List<Pair<String, String>> = emptyList()): String =
        build(Journal.events.snapshot(), Journal.errors.snapshot(), HealthMonitor.samples.snapshot(), extra)
}

/**
 * The journal on disk, for a session that ends badly. [LIVE] is rewritten every few seconds while
 * the app runs (a native crash gives no chance to save); at the next launch it becomes [PREVIOUS].
 * A Kotlin crash writes [CRASH] in its handler, with the stack trace in the journal.
 */
object JournalFiles {
    const val LIVE = "journal-live.json"
    const val PREVIOUS = "journal-previous.json"
    const val CRASH = "journal-crash.json"

    /** At launch: the last session's journal is kept as [PREVIOUS]. */
    fun startSession(dir: File) {
        runCatching {
            val live = File(dir, LIVE)
            if (live.exists()) {
                File(dir, PREVIOUS).delete()
                live.renameTo(File(dir, PREVIOUS))
            }
        }
    }

    /** Writes [json] as [name] in [dir] through a temporary file (never half a file). */
    fun write(dir: File, name: String, json: String): Boolean = runCatching {
        dir.mkdirs()
        val tmp = File(dir, "$name.tmp")
        tmp.writeText(json)
        val target = File(dir, name)
        if (!tmp.renameTo(target)) {
            target.delete()
            check(tmp.renameTo(target))
        }
    }.isSuccess

    fun saveLive(dir: File) = write(dir, LIVE, JournalJson.current())

    /** How often at most the live copy is rewritten. */
    const val LIVE_INTERVAL_MS = 10_000L

    /** In the crash handler: the journal with the crash in it. */
    fun saveCrash(dir: File, thread: String, t: Throwable): Boolean {
        Journal.error("plantage (thread $thread)", Journal.describe(t, lines = 30))
        return write(dir, CRASH, JournalJson.current(listOf("crash" to Journal.describe(t, lines = 60))))
    }

    /** The journal of the session that crashed: written by the crash handler, else the last live copy. */
    fun crashJournal(dir: File): String? =
        listOf(File(dir, CRASH), File(dir, PREVIOUS)).firstOrNull { it.exists() }?.let { runCatching { it.readText() }.getOrNull() }

    /** Once the crash was reported or dismissed. */
    fun clearCrash(dir: File) {
        File(dir, CRASH).delete()
        File(dir, PREVIOUS).delete()
    }
}

/**
 * When the live copy ([JournalFiles.LIVE]) is due: at most every [intervalMs], and only when the
 * journal or the sound ([HealthMonitor.version]: readings with sound going out) moved since the
 * last one, so a crash during gesture-free playback still finds the last seconds of sound.
 */
class LiveCopyClock(private val intervalMs: Long = JournalFiles.LIVE_INTERVAL_MS) {
    private var saved = -1L
    private var savedAt = Long.MIN_VALUE / 2

    fun due(nowMs: Long): Boolean {
        val v = Journal.version + HealthMonitor.version
        if (nowMs - savedAt < intervalMs || v == saved) return false
        saved = v
        savedAt = nowMs
        return true
    }
}
